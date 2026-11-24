package com.example.extreme

import android.os.Build

enum class ExtremeModeState(val label: String) {
    LOCKED("LOCKED"),
    UNLOCKED("UNLOCKED"),
    INACTIVE("INACTIVE"),
    ACTIVATING("ACTIVATING"),
    ACTIVE("ACTIVE"),
    THERMAL_PROTECTED("THERMAL PROTECTED")
}

enum class ThermalProtectionLevel(val label: String) {
    NORMAL("NORMAL"),
    REDUCED("MODERATE — REDUCED LOAD"),
    PROTECTED("CRITICAL — PROTECTION ENGAGED")
}

enum class MemoryPressureLevel(val label: String) {
    LOW("LOW"),
    MODERATE("MODERATE"),
    HIGH("HIGH"),
    CRITICAL("CRITICAL")
}

enum class OptimizationAggressiveness(val label: String) {
    STOPPED("STOPPED"),
    REDUCED("THERMAL REDUCED"),
    FULL("EXTREME FULL")
}

enum class ForegroundDecision {
    ACTIVATE_SELECTED,
    IGNORE_NON_SELECTED,
    EXIT_STOP,
    NO_SELECTED_GAME,
    PERMISSION_MISSING
}

data class SelectedGameInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val isInstalled: Boolean,
    val isSelected: Boolean
)

data class RewardedUnlockState(
    val ad1Watched: Boolean = false,
    val ad2Watched: Boolean = false,
    val isUnlocked: Boolean = false,
    val unlockedAtWallMs: Long = 0L,
    val lastAdMessage: String? = null
) {
    val watchedStepsCount: Int
        get() = (if (ad1Watched) 1 else 0) + (if (ad2Watched && ad1Watched) 1 else 0)

    val nextRequiredStep: Int
        get() = when {
            isUnlocked -> 2
            !ad1Watched -> 1
            else -> 2
        }
}

sealed interface UnlockStepResult {
    data class Step1Completed(val state: RewardedUnlockState) : UnlockStepResult
    data class Unlocked(val state: RewardedUnlockState) : UnlockStepResult
    data class Rejected(val reason: String, val state: RewardedUnlockState) : UnlockStepResult
}

data class ThermalSnapshot(
    val batteryTempCelsius: Float = 32.0f,
    val thermalStatusCode: Int = 0,
    val thermalLabel: String = "NORMAL",
    val batteryPercent: Int = 80,
    val isCharging: Boolean = false,
    val protectionLevel: ThermalProtectionLevel = ThermalProtectionLevel.NORMAL,
    val notice: String = "Thermals normal — full optimization allowed"
)

enum class CapabilityCategory(val title: String) {
    FULLY_WORKING("Fully Working (Standard Android APIs)"),
    ANDROID_RESTRICTED("Restricted by Android Security Sandbox"),
    OEM_DEPENDENT("OEM & Hardware Dependent"),
    PERMISSION_REQUIRED("Requires User Permission")
}

data class DeviceCapabilityItem(
    val feature: String,
    val category: CapabilityCategory,
    val detail: String
)

data class DeviceCapabilityReport(
    val androidApiLevel: Int = Build.VERSION.SDK_INT,
    val adpfSupported: Boolean = false,
    val gameManagerSupported: Boolean = false,
    val sustainedPerfSupported: Boolean = false,
    val maxRefreshRateHz: Int = 60,
    val usageStatsGranted: Boolean = false,
    val writeSettingsGranted: Boolean = false,
    val items: List<DeviceCapabilityItem> = emptyList()
)

data class LiveExtremeMetrics(
    val targetPackage: String? = null,
    val targetGameName: String? = null,
    val fps: Int = 60,
    val frameTimeMs: Float = 16.6f,
    val cpuFreqMHz: Int = 0,
    val cpuUtilizationPercent: Int = 0,
    val adpfSessionActive: Boolean = false,
    val gameStateActive: Boolean = false,
    val gpuPipelineLabel: String = "Hardware Accelerated",
    val refreshRateHz: Int = 60,
    val supportedRefreshRates: List<Int> = listOf(60),
    val ramUsedPercent: Int = 0,
    val ramAvailableMb: Long = 0L,
    val ramTotalMb: Long = 0L,
    val memoryPressure: MemoryPressureLevel = MemoryPressureLevel.LOW,
    val turboWorkloadsThrottled: Boolean = false,
    val clearedCacheKb: Long = 0L,
    val networkType: String = "Offline",
    val pingMs: Int? = null,
    val jitterMs: Int? = null,
    val networkStabilityLabel: String = "Unknown",
    val thermalSnapshot: ThermalSnapshot = ThermalSnapshot(),
    val wakeLockHeld: Boolean = false,
    val foregroundServiceRunning: Boolean = false,
    val aggressiveness: OptimizationAggressiveness = OptimizationAggressiveness.STOPPED
)

data class ExtremeUiState(
    val modeState: ExtremeModeState = ExtremeModeState.LOCKED,
    val unlockState: RewardedUnlockState = RewardedUnlockState(),
    val selectedGame: SelectedGameInfo? = null,
    val availableGames: List<SelectedGameInfo> = emptyList(),
    val warningAccepted: Boolean = false,
    val autoStartOnLaunch: Boolean = true,
    val usageAccessGranted: Boolean = false,
    val lastMonitorDecision: ForegroundDecision = ForegroundDecision.NO_SELECTED_GAME,
    val lastDetectedForegroundPkg: String? = null,
    val lastTransitionReason: String = "Ready",
    val metrics: LiveExtremeMetrics = LiveExtremeMetrics(),
    val capabilityReport: DeviceCapabilityReport = DeviceCapabilityReport()
) {
    val isActiveOrProtecting: Boolean
        get() = modeState == ExtremeModeState.ACTIVE ||
            modeState == ExtremeModeState.ACTIVATING ||
            modeState == ExtremeModeState.THERMAL_PROTECTED

    val canActivate: Boolean
        get() = unlockState.isUnlocked &&
            selectedGame != null &&
            selectedGame.isInstalled &&
            metrics.thermalSnapshot.protectionLevel != ThermalProtectionLevel.PROTECTED
}

object ExtremeWarningSpec {
    const val TITLE = "SUPER HIGH PERFORMANCE WARNING"
    const val INTRO = "You are about to activate Super High Performance Mode for your selected game."
    val EFFECTS = listOf(
        "1. Higher battery consumption",
        "2. Increased device heat / thermal load",
        "3. Higher CPU & GPU workload",
        "4. Frame pacing / refresh rate changes",
        "5. Background app & cache optimization effects",
        "6. Temporary audio / display / brightness adjustments",
        "7. Automatic thermal throttling if device gets hot"
    )
    const val IMPORTANT_NOTICE =
        "IMPORTANT: All essential phone functions must continue working normally, including calls, SMS, alarms, notifications and emergency services."
    const val BUTTON_CANCEL = "CANCEL"
    const val BUTTON_CONTINUE = "I UNDERSTAND & CONTINUE"
}
