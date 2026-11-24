package com.example.extreme

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.example.data.SystemHardwareController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BackgroundPrepResult(
    val freedMb: Int,
    val clearedCacheKb: Long,
    val trimmedPackages: List<String>,
    val memoryPressure: MemoryPressureLevel,
    val ramUsedPercent: Int,
    val ramAvailableMb: Long,
    val ramTotalMb: Long,
    val turboWorkloadsThrottled: Boolean
)

class BackgroundOptimizer(
    private val context: Context,
    private val hardware: SystemHardwareController
) {
    companion object {
        private val ESSENTIAL_PACKAGES = setOf(
            "android",
            "com.android.phone",
            "com.android.server.telecom",
            "com.android.dialer",
            "com.google.android.dialer",
            "com.samsung.android.dialer",
            "com.android.incallui",
            "com.samsung.android.incallui",
            "com.android.mms",
            "com.android.mms.service",
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",
            "com.android.deskclock",
            "com.google.android.deskclock",
            "com.sec.android.app.clockpackage",
            "com.android.systemui",
            "com.android.settings",
            "com.android.emergency",
            "com.google.android.safetyhub",
            "com.google.android.gms",
            "com.android.bluetooth"
        )

        private val ESSENTIAL_KEYWORDS = listOf(
            "telecom", "dialer", "incallui", "telephony",
            "messaging", "mms", "sms", "deskclock", "clock",
            "alarm", "emergency", "safetyhub", "systemui"
        )

        fun isEssentialPackage(packageName: String?): Boolean {
            if (packageName.isNullOrBlank()) return true
            val lower = packageName.lowercase()
            if (lower in ESSENTIAL_PACKAGES) return true
            return ESSENTIAL_KEYWORDS.any { lower.contains(it) }
        }
    }

    private val _turboWorkloadsThrottled = MutableStateFlow(false)
    val turboWorkloadsThrottled: StateFlow<Boolean> = _turboWorkloadsThrottled.asStateFlow()

    fun isProtectedPackage(
        packageName: String,
        selectedGamePackage: String?,
        addedGames: Set<String>
    ): Boolean {
        if (packageName.isBlank()) return true
        if (packageName == context.packageName) return true
        if (packageName == selectedGamePackage || packageName in addedGames) return true
        if (isEssentialPackage(packageName)) return true
        return isSystemApplication(packageName)
    }

    fun isCallOrEmergencyPackage(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        val lower = packageName.lowercase()
        return lower.contains("dialer") ||
            lower.contains("telecom") ||
            lower.contains("incallui") ||
            lower.contains("telephony") ||
            lower.contains("emergency") ||
            lower.contains("safetyhub")
    }

    fun readMemorySnapshot(): Triple<Int, Long, Long> {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val info = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(info)
        val totalMb = (info.totalMem / (1024L * 1024L)).coerceAtLeast(1L)
        val availMb = (info.availMem / (1024L * 1024L)).coerceAtLeast(0L)
        val usedPct = (((totalMb - availMb).toDouble() / totalMb.toDouble()) * 100.0)
            .toInt()
            .coerceIn(1, 99)
        return Triple(usedPct, availMb, totalMb)
    }

    fun classifyMemoryPressure(usedPercent: Int): MemoryPressureLevel {
        return when {
            usedPercent >= 90 -> MemoryPressureLevel.CRITICAL
            usedPercent >= 78 -> MemoryPressureLevel.HIGH
            usedPercent >= 60 -> MemoryPressureLevel.MODERATE
            else -> MemoryPressureLevel.LOW
        }
    }

    fun selectSafeTrimCandidates(
        knownPackages: List<String>,
        selectedGamePackage: String?,
        addedGames: Set<String>
    ): List<String> {
        return knownPackages
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filter { !isProtectedPackage(it, selectedGamePackage, addedGames) }
            .distinct()
            .toList()
    }

    suspend fun prepareForGameLaunch(
        selectedGamePackage: String,
        knownNonEssentialPackages: List<String>,
        addedGames: Set<String>,
        aggressiveness: OptimizationAggressiveness
    ): BackgroundPrepResult {
        _turboWorkloadsThrottled.value = aggressiveness != OptimizationAggressiveness.STOPPED

        val before = readMemorySnapshot()
        val clearedKb = hardware.clearAppCaches(cacheLimitIndex = if (aggressiveness == OptimizationAggressiveness.FULL) 2 else 1)

        val candidates = if (aggressiveness == OptimizationAggressiveness.FULL) {
            selectSafeTrimCandidates(knownNonEssentialPackages, selectedGamePackage, addedGames)
        } else {
            emptyList()
        }

        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val trimmed = mutableListOf<String>()
        candidates.forEach { pkg ->
            if (!isProtectedPackage(pkg, selectedGamePackage, addedGames)) {
                runCatching {
                    am?.killBackgroundProcesses(pkg)
                    trimmed.add(pkg)
                }
            }
        }

        val after = readMemorySnapshot()
        val freedMb = (after.second - before.second).toInt().coerceAtLeast(0)
        return BackgroundPrepResult(
            freedMb = freedMb,
            clearedCacheKb = clearedKb,
            trimmedPackages = trimmed,
            memoryPressure = classifyMemoryPressure(after.first),
            ramUsedPercent = after.first,
            ramAvailableMb = after.second,
            ramTotalMb = after.third,
            turboWorkloadsThrottled = _turboWorkloadsThrottled.value
        )
    }

    fun throttleTurboWorkloads(throttled: Boolean) {
        _turboWorkloadsThrottled.value = throttled
    }

    fun restoreWorkloads() {
        _turboWorkloadsThrottled.value = false
    }

    private fun isSystemApplication(packageName: String): Boolean {
        return runCatching {
            val pm = context.packageManager
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        }.getOrDefault(false)
    }
}
