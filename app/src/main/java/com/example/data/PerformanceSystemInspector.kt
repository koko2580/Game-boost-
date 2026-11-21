package com.example.data

import android.app.Activity
import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import android.view.Display
import android.view.WindowManager

data class DisplayModeOption(
    val modeId: Int,
    val widthPx: Int,
    val heightPx: Int,
    val refreshRateHz: Int
) {
    val label: String
        get() = "${widthPx}×${heightPx} @ ${refreshRateHz}Hz"
}

data class AutoPerfDecision(
    val recommendedCpuMode: CpuMode,
    val recommendSustained: Boolean,
    val reasonLabel: String
)

internal class PerformanceSystemInspector(private val context: Context) {

    fun detectBatterySaverState(): Pair<Boolean, Boolean> {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            ?: return false to false
        val powerSave = runCatching { pm.isPowerSaveMode }.getOrDefault(false)
        val ignoringOpt = runCatching {
            pm.isIgnoringBatteryOptimizations(context.packageName)
        }.getOrDefault(false)
        return powerSave to ignoringOpt
    }

    fun isSustainedPerformanceSupported(): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return runCatching { pm.isSustainedPerformanceModeSupported }.getOrDefault(false)
    }

    fun detectBackgroundActivitySummary(protectedPackages: Set<String> = emptySet()): Pair<Int, List<String>> {
        val pkgs = linkedSetOf<String>()
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        runCatching {
            am?.runningAppProcesses?.forEach { proc ->
                if (proc.uid >= Process.FIRST_APPLICATION_UID &&
                    proc.importance > ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                ) {
                    val pkg = proc.pkgList?.firstOrNull() ?: proc.processName
                    if (pkg.isNotBlank() && pkg != context.packageName && pkg !in protectedPackages) {
                        pkgs.add(pkg)
                    }
                }
            }
        }
        if (hasUsageStatsPermission()) {
            runCatching {
                val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                val end = System.currentTimeMillis()
                val start = end - 15 * 60_000L
                usm?.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
                    ?.asSequence()
                    ?.filter { it.lastTimeUsed >= start }
                    ?.sortedByDescending { it.lastTimeUsed }
                    ?.forEach { stat ->
                        val pkg = stat.packageName
                        if (pkg.isNotBlank() && pkg != context.packageName && pkg !in protectedPackages) {
                            pkgs.add(pkg)
                        }
                    }
            }
        }
        return pkgs.size to pkgs.take(4)
    }

    fun detectDisplayResolutionInfo(activity: Activity? = null): Triple<String, List<DisplayModeOption>, Int> {
        val dm = context.resources.displayMetrics
        val densityDpi = dm.densityDpi
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager

        val display: Display? = runCatching {
            when {
                activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> activity.display
                activity != null -> @Suppress("DEPRECATION") activity.windowManager.defaultDisplay
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> context.display
                else -> @Suppress("DEPRECATION") wm?.defaultDisplay
            }
        }.getOrNull()

        val supported = runCatching {
            display?.supportedModes
                ?.map { mode ->
                    DisplayModeOption(
                        modeId = mode.modeId,
                        widthPx = mode.physicalWidth,
                        heightPx = mode.physicalHeight,
                        refreshRateHz = mode.refreshRate.toInt().coerceAtLeast(60)
                    )
                }
                ?.distinctBy { Triple(it.widthPx, it.heightPx, it.refreshRateHz) }
                ?.sortedWith(
                    compareByDescending<DisplayModeOption> { it.widthPx * it.heightPx }
                        .thenByDescending { it.refreshRateHz }
                )
                .orEmpty()
        }.getOrDefault(emptyList())

        val activeMode = runCatching { display?.mode }.getOrNull()
        val currentLabel = if (activeMode != null && activeMode.physicalWidth > 0 && activeMode.physicalHeight > 0) {
            "${activeMode.physicalWidth}×${activeMode.physicalHeight} @ ${activeMode.refreshRate.toInt().coerceAtLeast(60)}Hz"
        } else {
            val fallbackRefresh = activeMode?.refreshRate?.toInt()?.takeIf { it > 0 } ?: 60
            "${dm.widthPixels}×${dm.heightPixels} @ ${fallbackRefresh}Hz"
        }

        val finalModes = if (supported.isNotEmpty()) {
            supported
        } else {
            listOf(
                DisplayModeOption(
                    modeId = activeMode?.modeId ?: 0,
                    widthPx = dm.widthPixels,
                    heightPx = dm.heightPixels,
                    refreshRateHz = activeMode?.refreshRate?.toInt()?.coerceAtLeast(60) ?: 60
                )
            )
        }

        return Triple(currentLabel, finalModes, densityDpi)
    }

    fun evaluateAutoPerformance(
        thermalLabel: String,
        batterySaverActive: Boolean,
        batteryPercent: Int,
        isCharging: Boolean,
        sustainedSupported: Boolean
    ): AutoPerfDecision {
        return when {
            batterySaverActive || (batteryPercent <= 20 && !isCharging) -> AutoPerfDecision(
                recommendedCpuMode = CpuMode.ENERGY_SAVING,
                recommendSustained = false,
                reasonLabel = if (batterySaverActive) {
                    "Battery Saver Active -> Eco Guard"
                } else {
                    "Low Battery ($batteryPercent%) -> Eco Guard"
                }
            )
            thermalLabel == "HIGH" || thermalLabel == "THROTTLED" || thermalLabel == "MODERATE" -> AutoPerfDecision(
                recommendedCpuMode = CpuMode.BALANCED,
                recommendSustained = sustainedSupported,
                reasonLabel = if (sustainedSupported) {
                    "Thermal $thermalLabel -> Sustained Balanced"
                } else {
                    "Thermal $thermalLabel -> Balanced Guard"
                }
            )
            else -> AutoPerfDecision(
                recommendedCpuMode = CpuMode.ACCELERATION,
                recommendSustained = false,
                reasonLabel = "Optimal Thermals & Power -> Turbo Peak"
            )
        }
    }

    fun evaluateMemoryPressure(usedPercent: Int): Pair<String, String> {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val info = ActivityManager.MemoryInfo()
        val lowMem = runCatching {
            am?.getMemoryInfo(info)
            info.lowMemory
        }.getOrDefault(false)

        return when {
            lowMem || usedPercent >= 88 -> "CRITICAL" to "Critical memory pressure ($usedPercent% used) — aggressive RAM trim & closing heavy background apps recommended."
            usedPercent >= 75 -> "HIGH" to "High memory pressure ($usedPercent% used) — trimming background apps & caches to prevent stutter."
            usedPercent >= 58 -> "MODERATE" to "Moderate memory load ($usedPercent% used) — light background trim advised for smoother frame times."
            else -> "LOW" to "Healthy RAM headroom ($usedPercent% used) — sufficient memory available for peak gaming."
        }
    }

    fun openPowerSaverSettings(): Boolean {
        val candidates = listOf(
            Settings.ACTION_BATTERY_SAVER_SETTINGS,
            Intent.ACTION_POWER_USAGE_SUMMARY,
            Settings.ACTION_SETTINGS
        )
        return launchFirstResolvableIntent(candidates)
    }

    fun openAppManagementSettings(): Boolean {
        val candidates = listOf(
            Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS,
            Settings.ACTION_APPLICATION_SETTINGS,
            Settings.ACTION_SETTINGS
        )
        return launchFirstResolvableIntent(candidates)
    }

    fun openDisplaySettings(): Boolean {
        val candidates = listOf(
            Settings.ACTION_DISPLAY_SETTINGS,
            Settings.ACTION_SETTINGS
        )
        return launchFirstResolvableIntent(candidates)
    }

    private fun launchFirstResolvableIntent(actions: List<String>): Boolean {
        for (action in actions) {
            val intent = Intent(action).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            val launched = runCatching {
                context.startActivity(intent)
                true
            }.getOrDefault(false)
            if (launched) return true
        }
        return false
    }

    private fun hasUsageStatsPermission(): Boolean {
        return runCatching {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
            }
            mode == AppOpsManager.MODE_ALLOWED
        }.getOrDefault(false)
    }
}
