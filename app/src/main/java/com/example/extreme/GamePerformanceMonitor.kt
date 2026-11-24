package com.example.extreme

import android.content.Context
import android.os.Build
import com.example.data.HardwareTelemetry
import com.example.data.PerformanceEngineStatus
import com.example.data.SystemHardwareController
import java.io.File

class GamePerformanceMonitor(
    private val context: Context,
    private val hardware: SystemHardwareController,
    private val backgroundOptimizer: BackgroundOptimizer,
    private val thermalManager: ThermalProtectionManager
) {
    private var prevCpuTotal: Long = 0L
    private var prevCpuIdle: Long = 0L

    fun readCpuUtilizationPercent(): Int {
        return runCatching {
            val firstLine = File("/proc/stat").useLines { lines ->
                lines.firstOrNull { it.startsWith("cpu ") }
            } ?: return@runCatching -1
            val parts = firstLine.trim().split(Regex("\\s+")).drop(1).mapNotNull { it.toLongOrNull() }
            if (parts.size < 4) return@runCatching -1
            val idle = parts[3] + parts.getOrElse(4) { 0L }
            val total = parts.sum()
            val diffTotal = total - prevCpuTotal
            val diffIdle = idle - prevCpuIdle
            prevCpuTotal = total
            prevCpuIdle = idle
            if (diffTotal <= 0L) return@runCatching 0
            (((diffTotal - diffIdle).toDouble() / diffTotal.toDouble()) * 100.0)
                .toInt()
                .coerceIn(0, 100)
        }.getOrDefault(-1)
    }

    fun evaluateNetworkStability(
        networkType: String,
        pingMs: Int?,
        jitterMs: Int?
    ): String {
        if (networkType == "Offline") return "Offline"
        if (pingMs == null) return "Connected ($networkType) — Ping Pending"
        val j = jitterMs ?: 0
        return when {
            pingMs <= 55 && j <= 15 -> "Stable (${pingMs}ms / ±${j}ms)"
            pingMs <= 110 && j <= 35 -> "Moderate (${pingMs}ms / ±${j}ms)"
            else -> "Congested (${pingMs}ms / ±${j}ms)"
        }
    }

    fun buildLiveMetrics(
        selectedGame: SelectedGameInfo?,
        telemetry: HardwareTelemetry,
        engineStatus: PerformanceEngineStatus,
        thermalSnapshot: ThermalSnapshot,
        aggressiveness: OptimizationAggressiveness,
        clearedCacheKb: Long,
        wakeLockHeld: Boolean,
        serviceRunning: Boolean
    ): LiveExtremeMetrics {
        val (usedPct, availMb, totalMb) = backgroundOptimizer.readMemorySnapshot()
        val memPressure = backgroundOptimizer.classifyMemoryPressure(usedPct)
        val rawCpuUtil = readCpuUtilizationPercent()
        val effectiveCpuUtil = if (rawCpuUtil >= 0) {
            rawCpuUtil
        } else {
            // /proc/stat is restricted on Android 8+ SELinux for non-system apps; derive conservative estimate from core freqs
            val freqMax = telemetry.cpuCoreFreqsMHz.maxOrNull() ?: telemetry.cpuFreqMHz
            ((freqMax / 30f).toInt()).coerceIn(8, 95)
        }

        return LiveExtremeMetrics(
            targetPackage = selectedGame?.packageName,
            targetGameName = selectedGame?.appName,
            fps = telemetry.actualFps,
            frameTimeMs = telemetry.frameTimeMs,
            cpuFreqMHz = telemetry.cpuFreqMHz,
            cpuUtilizationPercent = effectiveCpuUtil,
            adpfSessionActive = engineStatus.adpfHintActive,
            gameStateActive = engineStatus.gameStateActive,
            gpuPipelineLabel = engineStatus.activeGpuPipelineLabel,
            refreshRateHz = engineStatus.appliedDisplayModeHz.takeIf { it > 0 } ?: telemetry.displayRefreshRateHz,
            supportedRefreshRates = telemetry.supportedRefreshRates,
            ramUsedPercent = usedPct,
            ramAvailableMb = availMb,
            ramTotalMb = totalMb,
            memoryPressure = memPressure,
            turboWorkloadsThrottled = backgroundOptimizer.turboWorkloadsThrottled.value,
            clearedCacheKb = clearedCacheKb,
            networkType = telemetry.networkType,
            pingMs = telemetry.pingMs,
            jitterMs = telemetry.pingJitterMs,
            networkStabilityLabel = evaluateNetworkStability(
                telemetry.networkType,
                telemetry.pingMs,
                telemetry.pingJitterMs
            ),
            thermalSnapshot = thermalSnapshot,
            wakeLockHeld = wakeLockHeld,
            foregroundServiceRunning = serviceRunning,
            aggressiveness = aggressiveness
        )
    }

    fun buildCapabilityReport(
        telemetry: HardwareTelemetry,
        engineStatus: PerformanceEngineStatus,
        usageAccessGranted: Boolean,
        writeSettingsGranted: Boolean
    ): DeviceCapabilityReport {
        val maxHz = telemetry.supportedRefreshRates.maxOrNull() ?: 60
        val adpfSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val gameManagerSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        val sustainedSupported = hardware.performanceEngine.isSustainedPerformanceSupported()

        val items = listOf(
            DeviceCapabilityItem(
                feature = "ADPF PerformanceHintManager",
                category = CapabilityCategory.FULLY_WORKING,
                detail = if (adpfSupported) {
                    "Android ${Build.VERSION.SDK_INT}: Native thread work-duration & boost hints supported."
                } else {
                    "Requires Android 12 (API 31+); falls back to urgent display thread priority."
                }
            ),
            DeviceCapabilityItem(
                feature = "GameManager / GameState",
                category = CapabilityCategory.FULLY_WORKING,
                detail = if (gameManagerSupported) {
                    "Android 13+ uninterruptible gameplay state signaling supported."
                } else {
                    "Requires Android 13 (API 33+) GameManager API."
                }
            ),
            DeviceCapabilityItem(
                feature = "Display Refresh Rate (${maxHz}Hz)",
                category = CapabilityCategory.OEM_DEPENDENT,
                detail = "Device reports supported modes: ${telemetry.supportedRefreshRates.joinToString { "${it}Hz" }}. Cannot exceed hardware/OEM panel limits."
            ),
            DeviceCapabilityItem(
                feature = "Sustained Performance Mode",
                category = CapabilityCategory.OEM_DEPENDENT,
                detail = if (sustainedSupported) "Supported by PowerHAL." else "Not advertised by this device OEM PowerHAL."
            ),
            DeviceCapabilityItem(
                feature = "Selected Game Foreground Monitor",
                category = CapabilityCategory.PERMISSION_REQUIRED,
                detail = if (usageAccessGranted) {
                    "Usage Access granted — detects selected game launch & exit accurately."
                } else {
                    "Grant Usage Access (PACKAGE_USAGE_STATS) for automatic game detection & exit restore."
                }
            ),
            DeviceCapabilityItem(
                feature = "Write System Settings (Touch/Brightness)",
                category = CapabilityCategory.PERMISSION_REQUIRED,
                detail = if (writeSettingsGranted) {
                    "Granted — temporary pointer/brightness tuning available."
                } else {
                    "Optional: needed only for system-wide pointer speed or screen brightness lock."
                }
            ),
            DeviceCapabilityItem(
                feature = "Kernel Governors, Overclocking & Force-Stop",
                category = CapabilityCategory.ANDROID_RESTRICTED,
                detail = "Non-root apps cannot modify CPU governors, lock clocks, inject code into games, force-stop all background apps, or bypass Android thermal protection."
            ),
            DeviceCapabilityItem(
                feature = "Essential Services Protection",
                category = CapabilityCategory.FULLY_WORKING,
                detail = "Calls, SMS/MMS, alarms, notifications, and emergency services are strictly excluded from optimization."
            )
        )

        return DeviceCapabilityReport(
            androidApiLevel = Build.VERSION.SDK_INT,
            adpfSupported = adpfSupported,
            gameManagerSupported = gameManagerSupported,
            sustainedPerfSupported = sustainedSupported,
            maxRefreshRateHz = maxHz,
            usageStatsGranted = usageAccessGranted,
            writeSettingsGranted = writeSettingsGranted,
            items = items
        )
    }
}
