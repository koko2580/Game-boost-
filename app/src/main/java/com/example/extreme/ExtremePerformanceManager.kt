package com.example.extreme

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.CpuMode
import com.example.data.GameTurboPreferencesRepository
import com.example.data.GameTurboSettings
import com.example.data.HardwareTelemetry
import com.example.data.InstalledAppInfo
import com.example.data.PermissionStatus
import com.example.data.SystemHardwareController
import com.example.service.PerformanceSessionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

interface ExtremeSessionServiceBridge {
    val isServiceRunning: Boolean
    val isWakeLockHeld: Boolean
    fun startSessionService(gameName: String, gamePackage: String, acquireWakeLock: Boolean)
    fun stopSessionService()
}

class AndroidExtremeSessionServiceBridge(
    private val context: Context
) : ExtremeSessionServiceBridge {

    override val isServiceRunning: Boolean
        get() = PerformanceSessionService.isRunningFlow.value

    override val isWakeLockHeld: Boolean
        get() = PerformanceSessionService.wakeLockHeldFlow.value

    override fun startSessionService(
        gameName: String,
        gamePackage: String,
        acquireWakeLock: Boolean
    ) {
        val intent = PerformanceSessionService.createStartIntent(
            context = context,
            gameName = gameName,
            gamePackage = gamePackage,
            acquireWakeLock = acquireWakeLock
        )
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun stopSessionService() {
        runCatching {
            context.stopService(Intent(context, PerformanceSessionService::class.java))
        }
        PerformanceSessionService.resetStateForCleanShutdown()
    }
}

sealed interface ActivationAttemptResult {
    data class Activated(val game: SelectedGameInfo) : ActivationAttemptResult
    data object NeedsWarningConfirmation : ActivationAttemptResult
    data class Blocked(val reason: String) : ActivationAttemptResult
}

class ExtremePerformanceManager(
    private val context: Context,
    private val repo: GameTurboPreferencesRepository,
    private val hardware: SystemHardwareController,
    val selectedGameManager: SelectedGameManager = SelectedGameManager(context, repo),
    val thermalManager: ThermalProtectionManager = ThermalProtectionManager(context),
    val backgroundOptimizer: BackgroundOptimizer = BackgroundOptimizer(context, hardware),
    val selectedGameMonitor: SelectedGameMonitor = SelectedGameMonitor(context),
    val performanceMonitor: GamePerformanceMonitor = GamePerformanceMonitor(
        context = context,
        hardware = hardware,
        backgroundOptimizer = backgroundOptimizer,
        thermalManager = thermalManager
    ),
    val unlockManager: RewardedAdUnlockManager = RewardedAdUnlockManager(repo),
    private val serviceBridge: ExtremeSessionServiceBridge = AndroidExtremeSessionServiceBridge(context)
) {
    private val _uiState = MutableStateFlow(ExtremeUiState())
    val uiState: StateFlow<ExtremeUiState> = _uiState.asStateFlow()

    @Volatile
    private var activeSessionGamePackage: String? = null

    @Volatile
    private var manualSessionActive: Boolean = false

    @Volatile
    private var hasActivatedAtLeastOnce: Boolean = false

    @Volatile
    private var thermalProtectionTripped: Boolean = false

    @Volatile
    private var lastClearedCacheKb: Long = 0L

    @Volatile
    private var currentAggressiveness: OptimizationAggressiveness = OptimizationAggressiveness.STOPPED

    val isSessionActive: Boolean
        get() = activeSessionGamePackage != null

    val activePackageName: String?
        get() = activeSessionGamePackage

    fun syncState(
        settings: GameTurboSettings,
        installedApps: List<InstalledAppInfo>,
        telemetry: HardwareTelemetry,
        permissions: PermissionStatus
    ) {
        val unlock = unlockManager.syncFromSettings(settings)
        val availableGames = selectedGameManager.buildAvailableGames(settings, installedApps)
        val selectedGame = selectedGameManager.resolveSelectedGame(settings, installedApps)
        val thermal = thermalManager.evaluate()
        val usageGranted = selectedGameMonitor.hasUsageAccess() || permissions.usageStatsGranted

        val resolvedModeState = computeModeState(
            unlockState = unlock,
            thermal = thermal
        )

        val engineStatus = hardware.performanceEngine.status.value
        val metrics = performanceMonitor.buildLiveMetrics(
            selectedGame = selectedGame,
            telemetry = telemetry,
            engineStatus = engineStatus,
            thermalSnapshot = thermal,
            aggressiveness = if (isSessionActive) currentAggressiveness else OptimizationAggressiveness.STOPPED,
            clearedCacheKb = lastClearedCacheKb,
            wakeLockHeld = serviceBridge.isWakeLockHeld,
            serviceRunning = serviceBridge.isServiceRunning
        )

        val capabilityReport = performanceMonitor.buildCapabilityReport(
            telemetry = telemetry,
            engineStatus = engineStatus,
            usageAccessGranted = usageGranted,
            writeSettingsGranted = permissions.writeSettingsGranted
        )

        _uiState.value = _uiState.value.copy(
            modeState = resolvedModeState,
            unlockState = unlock,
            selectedGame = selectedGame,
            availableGames = availableGames,
            warningAccepted = settings.superPerfWarningAccepted,
            autoStartOnLaunch = settings.superPerfAutoStartOnLaunch,
            usageAccessGranted = usageGranted,
            metrics = metrics,
            capabilityReport = capabilityReport
        )
    }

    suspend fun acceptWarningDialog(
        settings: GameTurboSettings,
        installedApps: List<InstalledAppInfo>,
        telemetry: HardwareTelemetry,
        permissions: PermissionStatus
    ) {
        repo.updateSettings { it.copy(superPerfWarningAccepted = true) }
        val updated = repo.settingsFlow.first()
        syncState(updated, installedApps, telemetry, permissions)
    }

    suspend fun selectGame(
        packageName: String?,
        settings: GameTurboSettings,
        installedApps: List<InstalledAppInfo>,
        telemetry: HardwareTelemetry,
        permissions: PermissionStatus,
        activity: Activity? = null
    ) {
        selectedGameManager.selectSingleGame(packageName) { prevPkg, newPkg ->
            if (isSessionActive && prevPkg != newPkg) {
                deactivateAndRestoreAll(
                    reason = "Stopped previous session for ${prevPkg ?: "game"} before switching",
                    settings = settings,
                    installedApps = installedApps,
                    telemetry = telemetry,
                    permissions = permissions,
                    activity = activity
                )
            }
        }
        val refreshed = repo.settingsFlow.first()
        syncState(refreshed, installedApps, telemetry, permissions)
    }

    suspend fun activateForSelectedGame(
        settings: GameTurboSettings,
        installedApps: List<InstalledAppInfo>,
        telemetry: HardwareTelemetry,
        permissions: PermissionStatus,
        knownNonEssentialPackages: List<String>,
        activity: Activity? = null,
        manualActivation: Boolean = true
    ): ActivationAttemptResult {
        val unlock = unlockManager.syncFromSettings(settings)
        if (!unlock.isUnlocked) {
            return ActivationAttemptResult.Blocked("Watch 2 rewarded ads to unlock Super High Performance Mode.")
        }

        val selected = selectedGameManager.resolveSelectedGame(settings, installedApps)
            ?: return ActivationAttemptResult.Blocked("Select one game from your library before activating.")

        if (!selected.isInstalled) {
            return ActivationAttemptResult.Blocked("${selected.appName} is not installed on this device.")
        }

        if (!settings.superPerfWarningAccepted) {
            return ActivationAttemptResult.NeedsWarningConfirmation
        }

        val thermal = thermalManager.evaluate()
        if (thermal.protectionLevel == ThermalProtectionLevel.PROTECTED) {
            thermalProtectionTripped = true
            deactivateAndRestoreAll(
                reason = thermal.notice,
                settings = settings,
                installedApps = installedApps,
                telemetry = telemetry,
                permissions = permissions,
                activity = activity,
                markThermalProtected = true
            )
            return ActivationAttemptResult.Blocked(thermal.notice)
        }

        // Stop any stale session targeting a different package before starting
        if (activeSessionGamePackage != null && activeSessionGamePackage != selected.packageName) {
            deactivateAndRestoreAll(
                reason = "Switching active target to ${selected.appName}",
                settings = settings,
                installedApps = installedApps,
                telemetry = telemetry,
                permissions = permissions,
                activity = activity
            )
        }

        _uiState.value = _uiState.value.copy(
            modeState = ExtremeModeState.ACTIVATING,
            lastTransitionReason = "Activating Super High Performance for ${selected.appName}..."
        )

        thermalProtectionTripped = false
        val targetAggressiveness = thermalManager.recommendedAggressiveness(thermal)
        currentAggressiveness = targetAggressiveness
        activeSessionGamePackage = selected.packageName
        manualSessionActive = manualActivation
        hasActivatedAtLeastOnce = true

        selectedGameMonitor.startMonitoring()

        val prep = backgroundOptimizer.prepareForGameLaunch(
            selectedGamePackage = selected.packageName,
            knownNonEssentialPackages = knownNonEssentialPackages,
            addedGames = settings.addedGamePackages,
            aggressiveness = targetAggressiveness
        )
        lastClearedCacheKb = prep.clearedCacheKb

        val maxSupportedHz = telemetry.supportedRefreshRates.maxOrNull() ?: 60
        val extremeSettings = settings.copy(
            cpuMode = if (targetAggressiveness == OptimizationAggressiveness.FULL) CpuMode.ACCELERATION else CpuMode.BALANCED,
            increaseCpuPerformance = true,
            increaseGpuPerformance = true,
            highPerfFpsControl = true,
            optimizeFramePacing = true,
            enableAdpfHints = true,
            reduceTouchLatency = true,
            enhanceTouchSensitivity = true,
            sustainedPerformanceMode = targetAggressiveness == OptimizationAggressiveness.REDUCED || settings.sustainedPerformanceMode,
            frameRateTarget = maxOf(settings.frameRateTarget, maxSupportedHz),
            smartBackgroundControl = true
        )

        hardware.performanceEngine.applyProfile(
            settings = extremeSettings,
            supportedRates = telemetry.supportedRefreshRates,
            activity = activity,
            isInGameLaunch = true,
            batteryPercent = thermal.batteryPercent,
            isCharging = thermal.isCharging
        )

        serviceBridge.startSessionService(
            gameName = selected.appName,
            gamePackage = selected.packageName,
            acquireWakeLock = targetAggressiveness == OptimizationAggressiveness.FULL
        )

        _uiState.value = _uiState.value.copy(
            lastTransitionReason = if (targetAggressiveness == OptimizationAggressiveness.FULL) {
                "Super High Performance ACTIVE for ${selected.appName}"
            } else {
                "Super High Performance ACTIVE (Thermal Reduced) for ${selected.appName}"
            }
        )
        syncState(settings, installedApps, telemetry, permissions)
        return ActivationAttemptResult.Activated(selected)
    }

    suspend fun evaluateForegroundApp(
        foregroundPackage: String?,
        settings: GameTurboSettings,
        installedApps: List<InstalledAppInfo>,
        telemetry: HardwareTelemetry,
        permissions: PermissionStatus,
        knownNonEssentialPackages: List<String>,
        activity: Activity? = null
    ): ForegroundEvaluationResult {
        val selectedPkg = settings.superPerfSelectedGamePackage
            ?.takeIf { it in settings.addedGamePackages }

        val result = selectedGameMonitor.evaluatePackageChange(
            foregroundPackage = foregroundPackage,
            selectedGamePackage = selectedPkg,
            isSessionCurrentlyActive = isSessionActive,
            allowTurboXForegroundWhileManual = manualSessionActive
        )

        _uiState.value = _uiState.value.copy(
            lastMonitorDecision = result.decision,
            lastDetectedForegroundPkg = result.foregroundPackage
        )

        when (result.decision) {
            ForegroundDecision.ACTIVATE_SELECTED -> {
                if (!isSessionActive && settings.superPerfAutoStartOnLaunch &&
                    settings.superPerfUnlocked && settings.superPerfWarningAccepted
                ) {
                    activateForSelectedGame(
                        settings = settings,
                        installedApps = installedApps,
                        telemetry = telemetry,
                        permissions = permissions,
                        knownNonEssentialPackages = knownNonEssentialPackages,
                        activity = activity,
                        manualActivation = false
                    )
                }
            }
            ForegroundDecision.EXIT_STOP -> {
                if (isSessionActive) {
                    val reason = if (backgroundOptimizer.isCallOrEmergencyPackage(foregroundPackage)) {
                        "Paused/stopped cleanly for incoming call/essential service ($foregroundPackage)"
                    } else {
                        "Selected game exited (foreground: ${foregroundPackage ?: "system"}) — auto-restored"
                    }
                    deactivateAndRestoreAll(
                        reason = reason,
                        settings = settings,
                        installedApps = installedApps,
                        telemetry = telemetry,
                        permissions = permissions,
                        activity = activity
                    )
                }
            }
            ForegroundDecision.IGNORE_NON_SELECTED -> {
                // Non-selected games/apps are NEVER optimized
                _uiState.value = _uiState.value.copy(
                    lastTransitionReason = if (foregroundPackage != null) {
                        "Ignored $foregroundPackage (not the selected game)"
                    } else {
                        _uiState.value.lastTransitionReason
                    }
                )
            }
            ForegroundDecision.NO_SELECTED_GAME,
            ForegroundDecision.PERMISSION_MISSING -> Unit
        }
        return result
    }

    fun evaluateThermalTick(
        settings: GameTurboSettings,
        installedApps: List<InstalledAppInfo>,
        telemetry: HardwareTelemetry,
        permissions: PermissionStatus,
        activity: Activity? = null
    ): ThermalSnapshot {
        val thermal = thermalManager.evaluate()
        if (isSessionActive) {
            when (thermal.protectionLevel) {
                ThermalProtectionLevel.PROTECTED -> {
                    thermalProtectionTripped = true
                    deactivateAndRestoreAll(
                        reason = thermal.notice,
                        settings = settings,
                        installedApps = installedApps,
                        telemetry = telemetry,
                        permissions = permissions,
                        activity = activity,
                        markThermalProtected = true
                    )
                }
                ThermalProtectionLevel.REDUCED -> {
                    if (currentAggressiveness != OptimizationAggressiveness.REDUCED) {
                        currentAggressiveness = OptimizationAggressiveness.REDUCED
                        backgroundOptimizer.throttleTurboWorkloads(true)
                        val reducedSettings = settings.copy(
                            cpuMode = CpuMode.BALANCED,
                            sustainedPerformanceMode = true,
                            enableAdpfHints = true
                        )
                        hardware.performanceEngine.applyProfile(
                            settings = reducedSettings,
                            supportedRates = telemetry.supportedRefreshRates,
                            activity = activity,
                            isInGameLaunch = false,
                            batteryPercent = thermal.batteryPercent,
                            isCharging = thermal.isCharging
                        )
                        activeSessionGamePackage?.let { pkg ->
                            val gameName = installedApps.firstOrNull { it.packageName == pkg }?.appName ?: pkg
                            serviceBridge.startSessionService(gameName, pkg, acquireWakeLock = false)
                        }
                        _uiState.value = _uiState.value.copy(
                            lastTransitionReason = thermal.notice
                        )
                    }
                }
                ThermalProtectionLevel.NORMAL -> {
                    thermalProtectionTripped = false
                }
            }
        } else if (thermal.protectionLevel == ThermalProtectionLevel.NORMAL && thermalProtectionTripped) {
            thermalProtectionTripped = false
        }
        syncState(settings, installedApps, telemetry, permissions)
        return thermal
    }

    fun deactivateAndRestoreAll(
        reason: String,
        settings: GameTurboSettings,
        installedApps: List<InstalledAppInfo>,
        telemetry: HardwareTelemetry,
        permissions: PermissionStatus,
        activity: Activity? = null,
        markThermalProtected: Boolean = false
    ) {
        activeSessionGamePackage = null
        manualSessionActive = false
        currentAggressiveness = OptimizationAggressiveness.STOPPED
        thermalProtectionTripped = markThermalProtected

        selectedGameMonitor.stopMonitoring()
        backgroundOptimizer.restoreWorkloads()
        hardware.performanceEngine.restoreAfterExtremeSession(activity)
        serviceBridge.stopSessionService()

        _uiState.value = _uiState.value.copy(
            lastTransitionReason = reason
        )
        syncState(settings, installedApps, telemetry, permissions)
    }

    fun resetSessionWithoutPersistedChanges() {
        activeSessionGamePackage = null
        manualSessionActive = false
        currentAggressiveness = OptimizationAggressiveness.STOPPED
        selectedGameMonitor.stopMonitoring()
        backgroundOptimizer.restoreWorkloads()
        hardware.performanceEngine.restoreAfterExtremeSession(null)
        serviceBridge.stopSessionService()
    }

    private fun computeModeState(
        unlockState: RewardedUnlockState,
        thermal: ThermalSnapshot
    ): ExtremeModeState {
        if (!unlockState.isUnlocked) return ExtremeModeState.LOCKED
        if (thermalProtectionTripped || thermal.protectionLevel == ThermalProtectionLevel.PROTECTED) {
            return ExtremeModeState.THERMAL_PROTECTED
        }
        if (isSessionActive) return ExtremeModeState.ACTIVE
        return if (hasActivatedAtLeastOnce) ExtremeModeState.INACTIVE else ExtremeModeState.UNLOCKED
    }
}
