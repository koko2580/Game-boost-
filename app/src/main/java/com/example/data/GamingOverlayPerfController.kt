package com.example.data

import android.os.PowerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

enum class CapabilityStatus(val label: String) {
    ACTIVE("Active"),
    SUPPORTED("Supported"),
    WARNING("Warning"),
    REQUIRES_SETTINGS("Requires Settings"),
    UNSUPPORTED("Unsupported"),
    APP_PACING("App Pacing")
}

enum class FrameDropRiskLevel(val label: String) {
    LOW("Low"),
    MODERATE("Moderate"),
    HIGH("High")
}

data class ThermalReadingSnapshot(
    val timestampMs: Long,
    val timeLabel: String,
    val batteryTempCelsius: Float,
    val isCharging: Boolean,
    val thermalStatusLabel: String,
    val headroomLabel: String,
    val headroomAvailable: Boolean
)

data class FrameDropRiskAssessment(
    val level: FrameDropRiskLevel = FrameDropRiskLevel.LOW,
    val reasons: List<String> = emptyList(),
    val bannerVisible: Boolean = false,
    val snoozedRemainingMinutes: Int = 0,
    val evaluatedTimeLabel: String = "--:--:--"
)

data class GamingOverlayPerfUiState(
    val xCoolingEngineActive: Boolean = false,
    val coolingReadings: List<ThermalReadingSnapshot> = emptyList(),
    val latestCoolingReading: ThermalReadingSnapshot? = null,
    val frameDropRisk: FrameDropRiskAssessment = FrameDropRiskAssessment(),
    val fps60Capability: CapabilityStatus = CapabilityStatus.SUPPORTED,
    val fps60Note: String = "",
    val highFpsStates: Map<Int, CapabilityStatus> = mapOf(
        60 to CapabilityStatus.SUPPORTED,
        90 to CapabilityStatus.UNSUPPORTED,
        120 to CapabilityStatus.UNSUPPORTED
    ),
    val fpsOptimizationStates: Map<Int, CapabilityStatus> = mapOf(
        30 to CapabilityStatus.APP_PACING,
        60 to CapabilityStatus.SUPPORTED,
        90 to CapabilityStatus.UNSUPPORTED,
        120 to CapabilityStatus.UNSUPPORTED
    ),
    val lastActionTimestampLabel: String = ""
)

/**
 * Unified non-root controller for the 7 Gaming Overlay Performance features.
 * Shared between [com.example.viewmodel.GameTurboViewModel] (in-app overlay)
 * and [com.example.service.GameOverlayService] (floating service overlay).
 */
class GamingOverlayPerfController(
    private val hardware: SystemHardwareController
) {
    // X Cooling Engine is manual-only and NEVER auto-starts on app/service boot.
    private val _uiState = MutableStateFlow(GamingOverlayPerfUiState())
    val uiState: StateFlow<GamingOverlayPerfUiState> = _uiState.asStateFlow()

    fun refreshDiagnostics(
        settings: GameTurboSettings,
        telemetry: HardwareTelemetry,
        perfStatus: PerformanceEngineStatus,
        nowMs: Long = System.currentTimeMillis()
    ) {
        val updatedReading = if (_uiState.value.xCoolingEngineActive) {
            buildThermalSnapshot(telemetry, perfStatus, nowMs)
        } else {
            _uiState.value.latestCoolingReading
        }
        val history = if (_uiState.value.xCoolingEngineActive && updatedReading != null) {
            val prev = _uiState.value.coolingReadings
            val shouldAppend = prev.isEmpty() || (nowMs - prev.first().timestampMs >= 2_500L)
            if (shouldAppend) (listOf(updatedReading) + prev).take(5) else prev
        } else {
            _uiState.value.coolingReadings
        }

        val (fps60Status, fps60Note) = evaluate60FpsCapability(settings, telemetry, perfStatus)
        val highFpsMap = listOf(60, 90, 120).associateWith { hz ->
            evaluateHighFpsCapability(hz, settings, telemetry, perfStatus)
        }
        val fpsOptMap = listOf(30, 60, 90, 120).associateWith { fps ->
            evaluateFpsOptTierCapability(fps, telemetry.supportedRefreshRates)
        }
        val risk = evaluateFrameDropRisk(telemetry, perfStatus, settings, nowMs)

        _uiState.value = _uiState.value.copy(
            coolingReadings = history,
            latestCoolingReading = updatedReading,
            frameDropRisk = risk,
            fps60Capability = fps60Status,
            fps60Note = fps60Note,
            highFpsStates = highFpsMap,
            fpsOptimizationStates = fpsOptMap
        )
    }

    fun toggleCoolingEngine(
        enable: Boolean,
        telemetry: HardwareTelemetry,
        perfStatus: PerformanceEngineStatus,
        nowMs: Long = System.currentTimeMillis()
    ): ThermalReadingSnapshot {
        val reading = buildThermalSnapshot(telemetry, perfStatus, nowMs)
        val prevHistory = _uiState.value.coolingReadings
        _uiState.value = _uiState.value.copy(
            xCoolingEngineActive = enable,
            latestCoolingReading = reading,
            coolingReadings = if (enable) (listOf(reading) + prevHistory).take(5) else prevHistory,
            lastActionTimestampLabel = reading.timeLabel
        )
        return reading
    }

    suspend fun applyManualCoolingAction(
        updateSettings: suspend ((GameTurboSettings) -> GameTurboSettings) -> Unit
    ): Long {
        val clearedKb = hardware.clearAppCaches(cacheLimitIndex = 1)
        updateSettings { cur ->
            cur.copy(
                lowRamOverlayMode = true,
                openingAnimation = false,
                homeAnimation = false,
                sustainedPerformanceMode = if (hardware.performanceEngine.isSustainedPerformanceSupported()) {
                    true
                } else {
                    cur.sustainedPerformanceMode
                }
            )
        }
        _uiState.value = _uiState.value.copy(lastActionTimestampLabel = formatTime())
        return clearedKb
    }

    suspend fun toggleEsportsMode(
        enable: Boolean,
        currentSettings: GameTurboSettings,
        knownPackages: List<String>,
        updateSettings: suspend ((GameTurboSettings) -> GameTurboSettings) -> Unit
    ): BoostResult? {
        hardware.audioEnhancer.pulseHaptic(enable)
        return if (enable) {
            val snapshot = if (currentSettings.esportsModeEnabled && currentSettings.esportsRestoreSnapshot.isNotBlank()) {
                currentSettings.esportsRestoreSnapshot
            } else {
                encodeEsportsSnapshot(currentSettings)
            }
            updateSettings { cur ->
                val resolvedSnapshot = if (cur.esportsModeEnabled && cur.esportsRestoreSnapshot.isNotBlank()) {
                    cur.esportsRestoreSnapshot
                } else {
                    encodeEsportsSnapshot(cur)
                }
                cur.copy(
                    esportsModeEnabled = true,
                    esportsRestoreSnapshot = resolvedSnapshot,
                    cpuMode = CpuMode.ACCELERATION,
                    lowRamOverlayMode = true,
                    reduceTouchLatency = true,
                    enhanceTouchSensitivity = true,
                    optimizeFramePacing = true,
                    enableAdpfHints = true,
                    smartBackgroundControl = true,
                    backgroundControlEnabled = true
                )
            }
            val boost = hardware.performRealMemoryBoost(
                protectedPackages = currentSettings.addedGamePackages,
                allKnownPackages = knownPackages,
                backgroundLimitIndex = currentSettings.backgroundLimitIndex,
                cacheLimitIndex = currentSettings.cacheLimitIndex
            )
            _uiState.value = _uiState.value.copy(lastActionTimestampLabel = formatTime())
            boost
        } else {
            updateSettings { cur -> restoreEsportsSnapshot(cur) }
            _uiState.value = _uiState.value.copy(lastActionTimestampLabel = formatTime())
            null
        }
    }

    suspend fun toggle60FpsStable(
        enable: Boolean,
        supportedModes: List<DisplayModeOption>,
        updateSettings: suspend ((GameTurboSettings) -> GameTurboSettings) -> Unit
    ) {
        hardware.audioEnhancer.pulseHaptic(false)
        if (enable) {
            val mode60 = supportedModes.firstOrNull { abs(it.refreshRateHz - 60) <= 3 }
            updateSettings { cur ->
                val baseline = cur.fpsRestoreSnapshot.ifBlank { encodeFpsSnapshot(cur) }
                cur.copy(
                    fps60StableEnabled = true,
                    highFpsModeEnabled = false,
                    fpsOptimizationTarget = 0,
                    fpsRestoreSnapshot = baseline,
                    frameRateTarget = 60,
                    fpsGameTarget = 0,
                    highPerfFpsControl = false,
                    optimizeFramePacing = true,
                    displayResolutionOpt = mode60 != null,
                    preferredDisplayModeId = mode60?.modeId ?: cur.preferredDisplayModeId
                )
            }
        } else {
            updateSettings { cur ->
                val base = restoreFpsSnapshot(cur)
                base.copy(fps60StableEnabled = false)
            }
        }
        _uiState.value = _uiState.value.copy(lastActionTimestampLabel = formatTime())
    }

    suspend fun togglePerformanceBoost(
        enable: Boolean,
        currentSettings: GameTurboSettings,
        knownPackages: List<String>,
        updateSettings: suspend ((GameTurboSettings) -> GameTurboSettings) -> Unit
    ): String {
        hardware.audioEnhancer.pulseHaptic(enable)
        return if (enable) {
            val clearedKb = hardware.clearAppCaches(currentSettings.cacheLimitIndex)
            val memRes = hardware.performRealMemoryBoost(
                protectedPackages = currentSettings.addedGamePackages,
                allKnownPackages = knownPackages,
                backgroundLimitIndex = currentSettings.backgroundLimitIndex,
                cacheLimitIndex = currentSettings.cacheLimitIndex
            )
            val timeStr = formatTime()
            val summary = "[$timeStr] UI animations reduced • Frame pacing + ADPF ON • Cleared $clearedKb KB cache • Trimmed ${memRes.trimmedPackages} bg apps (${memRes.freedMb} MB free delta)"
            updateSettings { cur ->
                val baseline = if (cur.performanceBoostActive && cur.perfBoostRestoreSnapshot.isNotBlank()) {
                    cur.perfBoostRestoreSnapshot
                } else {
                    encodePerfBoostSnapshot(cur)
                }
                cur.copy(
                    performanceBoostActive = true,
                    perfBoostRestoreSnapshot = baseline,
                    lowRamOverlayMode = true,
                    openingAnimation = false,
                    homeAnimation = false,
                    optimizeFramePacing = true,
                    enableAdpfHints = true,
                    smartBackgroundControl = true,
                    backgroundControlEnabled = true,
                    lastPerfBoostSummary = summary,
                    totalBoostCount = cur.totalBoostCount + 1,
                    lastFreedMb = memRes.freedMb
                )
            }
            _uiState.value = _uiState.value.copy(lastActionTimestampLabel = timeStr)
            summary
        } else {
            val timeStr = formatTime()
            updateSettings { cur ->
                restorePerfBoostSnapshot(cur).copy(
                    lastPerfBoostSummary = "[$timeStr] Restored prior Turbo X animation & pacing settings"
                )
            }
            _uiState.value = _uiState.value.copy(lastActionTimestampLabel = timeStr)
            "Restored prior Turbo X settings"
        }
    }

    suspend fun toggleFrameDropProtection(
        enabled: Boolean,
        updateSettings: suspend ((GameTurboSettings) -> GameTurboSettings) -> Unit
    ) {
        updateSettings { cur ->
            cur.copy(
                frameDropProtectionEnabled = enabled,
                frameDropAlertSnoozedUntilMs = if (enabled) 0L else cur.frameDropAlertSnoozedUntilMs
            )
        }
    }

    suspend fun snoozeFrameDropWarning(
        snoozeMinutes: Int = 10,
        nowMs: Long = System.currentTimeMillis(),
        updateSettings: suspend ((GameTurboSettings) -> GameTurboSettings) -> Unit
    ) {
        val until = nowMs + snoozeMinutes.coerceAtLeast(1) * 60_000L
        updateSettings { it.copy(frameDropAlertSnoozedUntilMs = until) }
        _uiState.value = _uiState.value.copy(
            frameDropRisk = _uiState.value.frameDropRisk.copy(
                bannerVisible = false,
                snoozedRemainingMinutes = snoozeMinutes
            )
        )
    }

    suspend fun applyFrameDropMitigation(
        updateSettings: suspend ((GameTurboSettings) -> GameTurboSettings) -> Unit
    ) {
        hardware.clearAppCaches(cacheLimitIndex = 1)
        updateSettings { cur ->
            cur.copy(
                lowRamOverlayMode = true,
                optimizeFramePacing = true,
                sustainedPerformanceMode = if (hardware.performanceEngine.isSustainedPerformanceSupported()) {
                    true
                } else {
                    cur.sustainedPerformanceMode
                }
            )
        }
        _uiState.value = _uiState.value.copy(lastActionTimestampLabel = formatTime())
    }

    suspend fun selectFpsOptimizationTarget(
        targetFps: Int,
        consentGranted: Boolean,
        supportedModes: List<DisplayModeOption>,
        updateSettings: suspend ((GameTurboSettings) -> GameTurboSettings) -> Unit
    ) {
        hardware.audioEnhancer.pulseHaptic(false)
        if (targetFps == 0) {
            updateSettings { cur ->
                restoreFpsSnapshot(cur).copy(fpsOptimizationTarget = 0)
            }
            _uiState.value = _uiState.value.copy(lastActionTimestampLabel = formatTime())
            return
        }
        val effectiveRate = if (targetFps == 30) 60 else targetFps
        val matchedMode = supportedModes.firstOrNull { abs(it.refreshRateHz - effectiveRate) <= 3 }
        updateSettings { cur ->
            val baseline = cur.fpsRestoreSnapshot.ifBlank { encodeFpsSnapshot(cur) }
            if (consentGranted) {
                cur.copy(
                    fpsOptimizationTarget = targetFps,
                    fpsOptDisplayConsent = true,
                    fps60StableEnabled = false,
                    highFpsModeEnabled = false,
                    fpsRestoreSnapshot = baseline,
                    frameRateTarget = effectiveRate,
                    fpsGameTarget = if (targetFps >= 90) targetFps else 0,
                    highPerfFpsControl = targetFps >= 90,
                    optimizeFramePacing = true,
                    displayResolutionOpt = matchedMode != null,
                    preferredDisplayModeId = matchedMode?.modeId ?: cur.preferredDisplayModeId
                )
            } else {
                // Save chosen guide tier without changing display preference when consent is not granted
                cur.copy(
                    fpsOptimizationTarget = targetFps,
                    fpsOptDisplayConsent = false
                )
            }
        }
        _uiState.value = _uiState.value.copy(lastActionTimestampLabel = formatTime())
    }

    suspend fun setFpsOptDisplayConsent(
        consentGranted: Boolean,
        supportedModes: List<DisplayModeOption>,
        updateSettings: suspend ((GameTurboSettings) -> GameTurboSettings) -> Unit
    ) {
        updateSettings { cur ->
            if (!consentGranted) {
                cur.copy(fpsOptDisplayConsent = false)
            } else if (cur.fpsOptimizationTarget > 0) {
                val effectiveRate = if (cur.fpsOptimizationTarget == 30) 60 else cur.fpsOptimizationTarget
                val matchedMode = supportedModes.firstOrNull { abs(it.refreshRateHz - effectiveRate) <= 3 }
                val baseline = cur.fpsRestoreSnapshot.ifBlank { encodeFpsSnapshot(cur) }
                cur.copy(
                    fpsOptDisplayConsent = true,
                    fps60StableEnabled = false,
                    highFpsModeEnabled = false,
                    fpsRestoreSnapshot = baseline,
                    frameRateTarget = effectiveRate,
                    fpsGameTarget = if (cur.fpsOptimizationTarget >= 90) cur.fpsOptimizationTarget else 0,
                    highPerfFpsControl = cur.fpsOptimizationTarget >= 90,
                    optimizeFramePacing = true,
                    displayResolutionOpt = matchedMode != null,
                    preferredDisplayModeId = matchedMode?.modeId ?: cur.preferredDisplayModeId
                )
            } else {
                cur.copy(fpsOptDisplayConsent = true)
            }
        }
    }

    suspend fun configureHighFpsMode(
        enable: Boolean,
        targetHz: Int,
        supportedModes: List<DisplayModeOption>,
        updateSettings: suspend ((GameTurboSettings) -> GameTurboSettings) -> Unit
    ) {
        val clampedHz = when (targetHz) {
            90, 120 -> targetHz
            else -> 60
        }
        hardware.audioEnhancer.pulseHaptic(enable)
        if (!enable) {
            updateSettings { cur ->
                restoreFpsSnapshot(cur).copy(highFpsModeEnabled = false)
            }
            _uiState.value = _uiState.value.copy(lastActionTimestampLabel = formatTime())
            return
        }
        val matchedMode = supportedModes.firstOrNull { abs(it.refreshRateHz - clampedHz) <= 3 }
        updateSettings { cur ->
            val baseline = cur.fpsRestoreSnapshot.ifBlank { encodeFpsSnapshot(cur) }
            cur.copy(
                highFpsModeEnabled = true,
                highFpsTargetHz = clampedHz,
                fps60StableEnabled = false,
                fpsOptimizationTarget = 0,
                fpsRestoreSnapshot = baseline,
                frameRateTarget = clampedHz,
                fpsGameTarget = if (clampedHz >= 90) clampedHz else 0,
                highPerfFpsControl = clampedHz >= 90,
                optimizeFramePacing = true,
                displayResolutionOpt = matchedMode != null,
                preferredDisplayModeId = matchedMode?.modeId ?: cur.preferredDisplayModeId
            )
        }
        _uiState.value = _uiState.value.copy(lastActionTimestampLabel = formatTime())
    }

    private fun buildThermalSnapshot(
        telemetry: HardwareTelemetry,
        perfStatus: PerformanceEngineStatus,
        nowMs: Long
    ): ThermalReadingSnapshot {
        val headroom = hardware.performanceEngine.readThermalHeadroomSeconds(10)
            ?: perfStatus.thermalHeadroomSeconds
        val headroomLabel = if (headroom != null) {
            String.format(Locale.US, "%.2fx load", headroom)
        } else {
            "Unavailable"
        }
        return ThermalReadingSnapshot(
            timestampMs = nowMs,
            timeLabel = formatTime(nowMs),
            batteryTempCelsius = telemetry.batteryTempCelsius,
            isCharging = telemetry.isCharging,
            thermalStatusLabel = perfStatus.thermalStatusLabel,
            headroomLabel = headroomLabel,
            headroomAvailable = headroom != null
        )
    }

    companion object {
        fun encodeEsportsSnapshot(s: GameTurboSettings): String = listOf(
            s.cpuMode.name,
            s.lowRamOverlayMode,
            s.reduceTouchLatency,
            s.enhanceTouchSensitivity,
            s.optimizeFramePacing,
            s.smartBackgroundControl,
            s.backgroundControlEnabled,
            s.enableAdpfHints
        ).joinToString("|")

        fun restoreEsportsSnapshot(s: GameTurboSettings): GameTurboSettings {
            val parts = s.esportsRestoreSnapshot.split("|")
            if (parts.size < 8) {
                return s.copy(
                    esportsModeEnabled = false,
                    esportsRestoreSnapshot = ""
                )
            }
            val cpu = runCatching { CpuMode.valueOf(parts[0]) }.getOrDefault(s.cpuMode)
            return s.copy(
                esportsModeEnabled = false,
                esportsRestoreSnapshot = "",
                cpuMode = cpu,
                lowRamOverlayMode = parts[1].toBooleanStrictOrNull() ?: s.lowRamOverlayMode,
                reduceTouchLatency = parts[2].toBooleanStrictOrNull() ?: s.reduceTouchLatency,
                enhanceTouchSensitivity = parts[3].toBooleanStrictOrNull() ?: s.enhanceTouchSensitivity,
                optimizeFramePacing = parts[4].toBooleanStrictOrNull() ?: s.optimizeFramePacing,
                smartBackgroundControl = parts[5].toBooleanStrictOrNull() ?: s.smartBackgroundControl,
                backgroundControlEnabled = parts[6].toBooleanStrictOrNull() ?: s.backgroundControlEnabled,
                enableAdpfHints = parts[7].toBooleanStrictOrNull() ?: s.enableAdpfHints
            )
        }

        fun encodeFpsSnapshot(s: GameTurboSettings): String = listOf(
            s.frameRateTarget,
            s.fpsGameTarget,
            s.highPerfFpsControl,
            s.displayResolutionOpt,
            s.preferredDisplayModeId,
            s.optimizeFramePacing
        ).joinToString("|")

        fun restoreFpsSnapshot(s: GameTurboSettings): GameTurboSettings {
            val parts = s.fpsRestoreSnapshot.split("|")
            if (parts.size < 6) {
                return s.copy(
                    fps60StableEnabled = false,
                    fpsOptimizationTarget = 0,
                    highFpsModeEnabled = false,
                    fpsRestoreSnapshot = ""
                )
            }
            return s.copy(
                fps60StableEnabled = false,
                fpsOptimizationTarget = 0,
                highFpsModeEnabled = false,
                fpsRestoreSnapshot = "",
                frameRateTarget = parts[0].toIntOrNull() ?: s.frameRateTarget,
                fpsGameTarget = parts[1].toIntOrNull() ?: s.fpsGameTarget,
                highPerfFpsControl = parts[2].toBooleanStrictOrNull() ?: s.highPerfFpsControl,
                displayResolutionOpt = parts[3].toBooleanStrictOrNull() ?: s.displayResolutionOpt,
                preferredDisplayModeId = parts[4].toIntOrNull() ?: s.preferredDisplayModeId,
                optimizeFramePacing = parts[5].toBooleanStrictOrNull() ?: s.optimizeFramePacing
            )
        }

        fun encodePerfBoostSnapshot(s: GameTurboSettings): String = listOf(
            s.lowRamOverlayMode,
            s.openingAnimation,
            s.homeAnimation,
            s.optimizeFramePacing,
            s.enableAdpfHints,
            s.smartBackgroundControl,
            s.backgroundControlEnabled
        ).joinToString("|")

        fun restorePerfBoostSnapshot(s: GameTurboSettings): GameTurboSettings {
            val parts = s.perfBoostRestoreSnapshot.split("|")
            if (parts.size < 7) {
                return s.copy(
                    performanceBoostActive = false,
                    perfBoostRestoreSnapshot = ""
                )
            }
            return s.copy(
                performanceBoostActive = false,
                perfBoostRestoreSnapshot = "",
                lowRamOverlayMode = parts[0].toBooleanStrictOrNull() ?: s.lowRamOverlayMode,
                openingAnimation = parts[1].toBooleanStrictOrNull() ?: s.openingAnimation,
                homeAnimation = parts[2].toBooleanStrictOrNull() ?: s.homeAnimation,
                optimizeFramePacing = parts[3].toBooleanStrictOrNull() ?: s.optimizeFramePacing,
                enableAdpfHints = parts[4].toBooleanStrictOrNull() ?: s.enableAdpfHints,
                smartBackgroundControl = parts[5].toBooleanStrictOrNull() ?: s.smartBackgroundControl,
                backgroundControlEnabled = parts[6].toBooleanStrictOrNull() ?: s.backgroundControlEnabled
            )
        }

        fun evaluate60FpsCapability(
            settings: GameTurboSettings,
            telemetry: HardwareTelemetry,
            perfStatus: PerformanceEngineStatus
        ): Pair<CapabilityStatus, String> {
            val has60Mode = telemetry.supportedRefreshRates.any { abs(it - 60) <= 3 }
            if (!has60Mode) {
                return CapabilityStatus.UNSUPPORTED to "Panel reports ${telemetry.supportedRefreshRates.joinToString("/")}Hz without a 60Hz Display.Mode."
            }
            val thermalWarning = perfStatus.thermalStatusCode >= PowerManager.THERMAL_STATUS_MODERATE ||
                telemetry.batteryTempCelsius >= 40.5f ||
                perfStatus.batterySaverActive
            if (thermalWarning) {
                val reason = if (perfStatus.batterySaverActive) {
                    "Battery Saver is ON (may throttle frames)."
                } else {
                    "Thermal ${perfStatus.thermalStatusLabel} (${telemetry.batteryTempCelsius}°C) may cause drops."
                }
                return CapabilityStatus.WARNING to reason
            }
            return if (settings.fps60StableEnabled) {
                CapabilityStatus.ACTIVE to "Turbo X window prefers 60Hz • Frame pacing ON (does not cap 3P games)."
            } else {
                CapabilityStatus.SUPPORTED to "60Hz Display.Mode available • Targets Turbo X window + pacing tips."
            }
        }

        fun evaluateFpsOptTierCapability(
            targetFps: Int,
            supportedRefreshRates: List<Int>
        ): CapabilityStatus {
            if (targetFps == 30) return CapabilityStatus.APP_PACING
            val hasMode = supportedRefreshRates.any { abs(it - targetFps) <= 3 }
            return if (hasMode) CapabilityStatus.SUPPORTED else CapabilityStatus.UNSUPPORTED
        }

        fun evaluateHighFpsCapability(
            hz: Int,
            settings: GameTurboSettings,
            telemetry: HardwareTelemetry,
            perfStatus: PerformanceEngineStatus
        ): CapabilityStatus {
            val supported = telemetry.supportedRefreshRates.any { it >= hz - 2 && abs(it - hz) <= 3 }
            if (!supported) return CapabilityStatus.UNSUPPORTED
            val blockedBySystem = perfStatus.batterySaverActive ||
                (hz > 60 && telemetry.displayRefreshRateHz < hz - 2 && settings.highFpsModeEnabled && settings.highFpsTargetHz == hz)
            return when {
                blockedBySystem -> CapabilityStatus.REQUIRES_SETTINGS
                settings.highFpsModeEnabled && settings.highFpsTargetHz == hz -> CapabilityStatus.ACTIVE
                perfStatus.thermalStatusCode >= PowerManager.THERMAL_STATUS_SEVERE -> CapabilityStatus.WARNING
                else -> CapabilityStatus.SUPPORTED
            }
        }

        fun evaluateFrameDropRisk(
            telemetry: HardwareTelemetry,
            perfStatus: PerformanceEngineStatus,
            settings: GameTurboSettings,
            nowMs: Long = System.currentTimeMillis()
        ): FrameDropRiskAssessment {
            val reasons = mutableListOf<String>()
            var score = 0

            when {
                perfStatus.thermalStatusCode >= PowerManager.THERMAL_STATUS_SEVERE || telemetry.batteryTempCelsius >= 42.5f -> {
                    score += 2
                    reasons += "Thermal stress (${perfStatus.thermalStatusLabel}, ${telemetry.batteryTempCelsius}°C)"
                }
                perfStatus.thermalStatusCode >= PowerManager.THERMAL_STATUS_MODERATE || telemetry.batteryTempCelsius >= 39.0f -> {
                    score += 1
                    reasons += "Rising thermal load (${perfStatus.thermalStatusLabel}, ${telemetry.batteryTempCelsius}°C)"
                }
            }

            val headroom = perfStatus.thermalHeadroomSeconds
            if (headroom != null) {
                if (headroom >= 0.95f) {
                    score += 2
                    reasons += "Low thermal headroom (${String.format(Locale.US, "%.2fx", headroom)})"
                } else if (headroom >= 0.80f) {
                    score += 1
                    reasons += "Warming thermal headroom (${String.format(Locale.US, "%.2fx", headroom)})"
                }
            }

            if (telemetry.ramUsedPercent >= 86) {
                score += 2
                reasons += "High RAM pressure (${telemetry.ramUsedPercent}% used)"
            } else if (telemetry.ramUsedPercent >= 74) {
                score += 1
                reasons += "Elevated RAM usage (${telemetry.ramUsedPercent}% used)"
            }

            if (perfStatus.batterySaverActive) {
                score += 1
                reasons += "Android Battery Saver is limiting background/CPU clocks"
            }

            if (perfStatus.framePacingJitterMs >= 8.0f) {
                score += 1
                reasons += "UI frame jitter ${String.format(Locale.US, "%.1f", perfStatus.framePacingJitterMs)}ms"
            }

            val level = when {
                score >= 3 -> FrameDropRiskLevel.HIGH
                score >= 1 -> FrameDropRiskLevel.MODERATE
                else -> FrameDropRiskLevel.LOW
            }
            if (reasons.isEmpty()) {
                reasons += "Thermals (${telemetry.batteryTempCelsius}°C) and RAM (${telemetry.ramUsedPercent}%) look steady."
            }

            val snoozedMs = (settings.frameDropAlertSnoozedUntilMs - nowMs).coerceAtLeast(0L)
            val snoozedMins = ((snoozedMs + 59_999L) / 60_000L).toInt()
            val showBanner = settings.frameDropProtectionEnabled &&
                level != FrameDropRiskLevel.LOW &&
                nowMs >= settings.frameDropAlertSnoozedUntilMs

            return FrameDropRiskAssessment(
                level = level,
                reasons = reasons,
                bannerVisible = showBanner,
                snoozedRemainingMinutes = snoozedMins,
                evaluatedTimeLabel = formatTime(nowMs)
            )
        }

        fun formatTime(timeMs: Long = System.currentTimeMillis()): String {
            return SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(timeMs))
        }
    }
}
