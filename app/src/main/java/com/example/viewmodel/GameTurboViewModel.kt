package com.example.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.TurboGameApplication
import com.example.ads.AdMobManager
import com.example.data.AD_REWARD_DURATION_MS
import com.example.data.BoostStepId
import com.example.data.BoostStepStatus
import com.example.data.ControlPanelLayout
import com.example.data.CpuMode
import com.example.data.CpuUnitFormat
import com.example.data.CrosshairStyle
import com.example.data.EqPreset
import com.example.data.GamePanelStyle
import com.example.data.GameSessionRecord
import com.example.data.GameTurboPreferencesRepository
import com.example.data.GameTurboSettings
import com.example.data.HardwareTelemetry
import com.example.data.InstalledAppInfo
import com.example.data.OneTapBoosterState
import com.example.data.OneTapStepResult
import com.example.data.PerformanceEngineStatus
import com.example.data.PermissionStatus
import com.example.data.ScreenColorMode
import com.example.data.SystemHardwareController
import com.example.data.WallpaperTheme
import com.example.data.ScreenshotHelper
import com.example.network.NetworkGateMonitor
import com.example.security.SecurityAuditReport
import com.example.extreme.ActivationAttemptResult
import com.example.extreme.ExtremePerformanceManager
import com.example.extreme.ExtremeUiState
import com.example.extreme.UnlockStepResult
import com.example.service.GameOverlayService
import com.example.service.PerformanceSessionService
import com.example.service.ScreenCaptureActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface RewardedAdStep {
    data object Hidden : RewardedAdStep
    data class TimeGatePrompt(
        val targetGameName: String?,
        val launchAfterReward: Boolean,
        val waitingForLoad: Boolean = false
    ) : RewardedAdStep
}

class GameTurboViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = GameTurboPreferencesRepository(application)
    val hardware = SystemHardwareController(application)

    val settings: StateFlow<GameTurboSettings> = repo.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GameTurboSettings()
    )

    private val _telemetry = MutableStateFlow(HardwareTelemetry())
    val telemetry: StateFlow<HardwareTelemetry> = _telemetry.asStateFlow()

    private val _permissions = MutableStateFlow(hardware.checkPermissions())
    val permissions: StateFlow<PermissionStatus> = _permissions.asStateFlow()

    val performanceStatus: StateFlow<PerformanceEngineStatus> = hardware.performanceEngine.status

    private val _securityAudit = MutableStateFlow(repo.securityGuard.runSecurityAudit())
    val securityAudit: StateFlow<SecurityAuditReport> = _securityAudit.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    private val _appsLoading = MutableStateFlow(true)
    val appsLoading: StateFlow<Boolean> = _appsLoading.asStateFlow()

    val addedGames: StateFlow<List<InstalledAppInfo>> = combine(
        _installedApps,
        settings
    ) { apps, s ->
        apps.filter { it.packageName in s.addedGamePackages }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _brightnessFraction = MutableStateFlow(hardware.getBrightnessFraction())
    val brightnessFraction: StateFlow<Float> = _brightnessFraction.asStateFlow()

    private val _volumeFraction = MutableStateFlow(hardware.getVolumeFraction())
    val volumeFraction: StateFlow<Float> = _volumeFraction.asStateFlow()

    private val _autoBrightness = MutableStateFlow(hardware.isAutoBrightnessEnabled())
    val autoBrightness: StateFlow<Boolean> = _autoBrightness.asStateFlow()

    private val _autoRotate = MutableStateFlow(hardware.isAutoRotateEnabled())
    val autoRotate: StateFlow<Boolean> = _autoRotate.asStateFlow()

    private val _isStopwatchRunning = MutableStateFlow(false)
    val isStopwatchRunning: StateFlow<Boolean> = _isStopwatchRunning.asStateFlow()

    private val _stopwatchSeconds = MutableStateFlow(0L)
    val stopwatchSeconds: StateFlow<Long> = _stopwatchSeconds.asStateFlow()

    private val _isInAppOverlayOpen = MutableStateFlow(false)
    val isInAppOverlayOpen: StateFlow<Boolean> = _isInAppOverlayOpen.asStateFlow()

    private val _launchingGameName = MutableStateFlow<String?>(null)
    val launchingGameName: StateFlow<String?> = _launchingGameName.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _sessionHistory = MutableStateFlow<List<GameSessionRecord>>(emptyList())
    val sessionHistory: StateFlow<List<GameSessionRecord>> = _sessionHistory.asStateFlow()

    private val _isOverlayServiceRunning = MutableStateFlow(GameOverlayService.isRunning)
    val isOverlayServiceRunning: StateFlow<Boolean> = _isOverlayServiceRunning.asStateFlow()

    private val adMobManager: AdMobManager? = (application as? TurboGameApplication)?.adMobManager

    val rewardedAdReady: StateFlow<Boolean> =
        adMobManager?.rewardedAdReady ?: MutableStateFlow(false).asStateFlow()

    val rewardedAdLoading: StateFlow<Boolean> =
        adMobManager?.rewardedAdLoading ?: MutableStateFlow(false).asStateFlow()

    val rewardedAdError: StateFlow<String?> =
        adMobManager?.rewardedAdError ?: MutableStateFlow<String?>(null).asStateFlow()

    val rewardedCooldownSeconds: StateFlow<Int> =
        adMobManager?.rewardedCooldownSeconds ?: MutableStateFlow(0).asStateFlow()

    private val _remainingTurboSeconds = MutableStateFlow(0L)
    val remainingTurboSeconds: StateFlow<Long> = _remainingTurboSeconds.asStateFlow()

    private val _adStep = MutableStateFlow<RewardedAdStep>(RewardedAdStep.Hidden)
    val adStep: StateFlow<RewardedAdStep> = _adStep.asStateFlow()

    private val _showSplash = MutableStateFlow(true)
    val showSplash: StateFlow<Boolean> = _showSplash.asStateFlow()

    private val _prefsLoaded = MutableStateFlow(false)
    val prefsLoaded: StateFlow<Boolean> = _prefsLoaded.asStateFlow()

    private val _oneTapBooster = MutableStateFlow(OneTapBoosterState())
    val oneTapBooster: StateFlow<OneTapBoosterState> = _oneTapBooster.asStateFlow()

    private var oneTapJob: Job? = null

    private val networkMonitor = NetworkGateMonitor(application)
    val isNetworkConnected: StateFlow<Boolean> = networkMonitor.isConnected

    val extremeManager = ExtremePerformanceManager(application, repo, hardware)
    val extremeUiState: StateFlow<ExtremeUiState> = extremeManager.uiState

    private val _showSuperPerfWarningDialog = MutableStateFlow(false)
    val showSuperPerfWarningDialog: StateFlow<Boolean> = _showSuperPerfWarningDialog.asStateFlow()

    private val _superPerfPendingActivateAfterWarning = MutableStateFlow(false)

    private var lastPingMs: Int? = null
    private var lastJitterMs: Int? = null
    private var lastFrameNanos: Long = 0L
    private var frameCountWindow: Int = 0
    private var windowStartNanos: Long = 0L
    @Volatile
    private var measuredFps: Int = 60
    @Volatile
    private var measuredFrameMs: Float = 16.6f
    private val _isUiActive = MutableStateFlow(true)

    init {
        viewModelScope.launch {
            repo.settingsFlow.first()
            _prefsLoaded.value = true
        }
        PerformanceSessionService.onExternalStopRequested = {
            deactivateSuperHighPerformance("Stopped via session notification")
        }
        refreshInstalledApps()
        startTelemetryLoop()
        startStopwatchLoop()
        startTurboTimeLoop()
        observeNetworkGate()
        observeExtremeStateSync()
    }

    private fun observeExtremeStateSync() {
        viewModelScope.launch {
            combine(settings, _installedApps, _telemetry, _permissions) { s, apps, t, perms ->
                listOf(s, apps, t, perms)
            }.collect {
                extremeManager.syncState(
                    settings = settings.value,
                    installedApps = _installedApps.value,
                    telemetry = _telemetry.value,
                    permissions = _permissions.value
                )
            }
        }
    }

    private fun observeNetworkGate() {
        var wasConnected = isNetworkConnected.value
        viewModelScope.launch {
            isNetworkConnected.collect { connected ->
                if (!connected) {
                    _isInAppOverlayOpen.value = false
                    _adStep.value = RewardedAdStep.Hidden
                    stopFloatingOverlayService()
                } else if (!wasConnected) {
                    adMobManager?.onNetworkConnected()
                }
                wasConnected = connected
            }
        }
    }

    fun refreshNetworkConnection(): Boolean {
        val online = networkMonitor.refreshNow()
        if (!online) {
            hardware.audioEnhancer.pulseHaptic(false)
        } else {
            adMobManager?.onNetworkConnected()
        }
        return online
    }

    fun openNetworkSettings() {
        adMobManager?.suppressAppOpenTemporarily()
        networkMonitor.openInternetSettings()
    }

    fun refreshSecurityAudit(): SecurityAuditReport {
        val report = repo.securityGuard.runSecurityAudit()
        _securityAudit.value = report
        return report
    }

    fun onChoreographerFrame(frameTimeNanos: Long) {
        if (lastFrameNanos > 0L) {
            val deltaNs = (frameTimeNanos - lastFrameNanos).coerceAtLeast(1_000_000L)
            measuredFrameMs = deltaNs / 1_000_000f
            hardware.performanceEngine.reportFrameDurationNanos(deltaNs, settings.value)
        }
        lastFrameNanos = frameTimeNanos
        if (windowStartNanos == 0L) windowStartNanos = frameTimeNanos
        frameCountWindow++
        val elapsedNs = frameTimeNanos - windowStartNanos
        if (elapsedNs >= 500_000_000L) {
            measuredFps = ((frameCountWindow * 1_000_000_000.0) / elapsedNs.toDouble())
                .toInt()
                .coerceIn(1, 144)
            frameCountWindow = 0
            windowStartNanos = frameTimeNanos
        }
    }

    fun syncWindowPerformance(activity: Activity) {
        val t = _telemetry.value
        hardware.performanceEngine.applyProfile(
            settings = settings.value,
            supportedRates = t.supportedRefreshRates,
            activity = activity,
            isInGameLaunch = false,
            batteryPercent = t.batteryPercent,
            isCharging = t.isCharging
        )
    }

    private var lastAutoDetectedPackage: String? = null

    fun refreshPermissionsAndHardware() {
        _permissions.value = hardware.checkPermissions()
        _brightnessFraction.value = hardware.getBrightnessFraction()
        _volumeFraction.value = hardware.getVolumeFraction()
        _autoBrightness.value = hardware.isAutoBrightnessEnabled()
        _autoRotate.value = hardware.isAutoRotateEnabled()
        _isOverlayServiceRunning.value = GameOverlayService.isRunning
    }

    fun checkForegroundGameAutoLaunch() {
        val current = settings.value
        val fgPkg = hardware.getForegroundPackageName()
        viewModelScope.launch {
            extremeManager.evaluateForegroundApp(
                foregroundPackage = fgPkg,
                settings = current,
                installedApps = _installedApps.value,
                telemetry = _telemetry.value,
                permissions = _permissions.value,
                knownNonEssentialPackages = _installedApps.value.map { it.packageName }
            )
        }
        if (fgPkg != null && fgPkg !in current.addedGamePackages && fgPkg != getApplication<Application>().packageName) {
            lastAutoDetectedPackage = null
        }
        val triggered = SystemHardwareController.evaluateForegroundGameTrigger(
            foregroundPkg = fgPkg,
            lastDetectedPkg = lastAutoDetectedPackage,
            settings = current,
            ownPackage = getApplication<Application>().packageName
        ) ?: return
        lastAutoDetectedPackage = triggered
        if (current.autoDndOnEnter || current.dndMode) {
            hardware.setDoNotDisturb(true)
        }
        if (current.muteNotificationsInGame || current.notificationMuteMode) {
            hardware.audioEnhancer.setNotificationMuted(true)
        }
        val t = _telemetry.value
        hardware.performanceEngine.applyProfile(
            settings = current,
            supportedRates = t.supportedRefreshRates,
            isInGameLaunch = true,
            batteryPercent = t.batteryPercent,
            isCharging = t.isCharging
        )
        if (hardware.checkPermissions().overlayGranted && !GameOverlayService.isRunning) {
            startFloatingOverlayService()
        }
        val appName = addedGames.value.find { it.packageName == triggered }?.appName ?: triggered
        showStatus("Auto-detected $appName: Turbo Boost Active")
    }

    fun completePermissionSetup() {
        viewModelScope.launch {
            repo.updateSettings { it.copy(hasCompletedPermissionSetup = true) }
        }
    }

    fun refreshInstalledApps() {
        viewModelScope.launch {
            _appsLoading.value = true
            _installedApps.value = hardware.loadLaunchableApps()
            _appsLoading.value = false
        }
    }

    fun setUiActive(active: Boolean) {
        if (!active) {
            lastFrameNanos = 0L
            frameCountWindow = 0
            windowStartNanos = 0L
        }
        _isUiActive.value = active
    }

    fun getAppIcon(packageName: String): ImageBitmap? = hardware.getAppIcon(packageName)

    fun peekAppIcon(packageName: String): ImageBitmap? = hardware.peekCachedAppIcon(packageName)

    suspend fun loadAppIconAsync(packageName: String): ImageBitmap? = hardware.loadAppIconAsync(packageName)

    private fun startTelemetryLoop() {
        viewModelScope.launch {
            while (true) {
                if (!_isUiActive.value) {
                    _isUiActive.first { it }
                }
                val mode = settings.value.cpuMode
                val ping = lastPingMs
                val jitter = lastJitterMs
                val fps = measuredFps
                val frameMs = measuredFrameMs
                val snapshot = withContext(Dispatchers.Default) {
                    hardware.readHardwareTelemetry(
                        cpuMode = mode,
                        lastPingMs = ping,
                        lastJitterMs = jitter,
                        actualFps = fps,
                        frameTimeMs = frameMs
                    )
                }
                _telemetry.value = snapshot
                _volumeFraction.value = hardware.getVolumeFraction()
                _isOverlayServiceRunning.value = GameOverlayService.isRunning

                val currentSettings = settings.value
                if (currentSettings.autoPerformanceOptimization) {
                    val decision = hardware.performanceEngine.evaluateAutoPerformance(
                        batteryPercent = snapshot.batteryPercent,
                        isCharging = snapshot.isCharging
                    )
                    if (currentSettings.cpuMode != decision.recommendedCpuMode ||
                        currentSettings.sustainedPerformanceMode != decision.recommendSustained
                    ) {
                        repo.updateSettings { s ->
                            if (!s.autoPerformanceOptimization) s
                            else s.copy(
                                cpuMode = decision.recommendedCpuMode,
                                sustainedPerformanceMode = decision.recommendSustained
                            )
                        }
                    }
                }
                if (extremeManager.isSessionActive || extremeManager.selectedGameMonitor.hasUsageAccess()) {
                    val fg = hardware.getForegroundPackageName()
                    if (fg != null && (extremeManager.isSessionActive || currentSettings.superPerfAutoStartOnLaunch)) {
                        extremeManager.evaluateForegroundApp(
                            foregroundPackage = fg,
                            settings = currentSettings,
                            installedApps = _installedApps.value,
                            telemetry = snapshot,
                            permissions = _permissions.value,
                            knownNonEssentialPackages = _installedApps.value.map { it.packageName }
                        )
                    }
                    extremeManager.evaluateThermalTick(
                        settings = currentSettings,
                        installedApps = _installedApps.value,
                        telemetry = snapshot,
                        permissions = _permissions.value
                    )
                } else {
                    extremeManager.syncState(
                        settings = currentSettings,
                        installedApps = _installedApps.value,
                        telemetry = snapshot,
                        permissions = _permissions.value
                    )
                }
                val intervalMs = if (extremeManager.backgroundOptimizer.turboWorkloadsThrottled.value) 4000L else 1200L
                delay(intervalMs)
            }
        }
    }

    private fun startStopwatchLoop() {
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                if (_isStopwatchRunning.value) {
                    _stopwatchSeconds.value += 1L
                }
            }
        }
    }

    private fun startTurboTimeLoop() {
        var maxObservedWallMs = System.currentTimeMillis()
        viewModelScope.launch {
            settings.collect { s ->
                val rem = computeRemainingSeconds(s.turboTimeExpiresAtMs)
                _remainingTurboSeconds.value = rem
            }
        }
        viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                if (now + 60_000L < maxObservedWallMs) {
                    // System clock jumped backward during active session: re-verify against sealed vault
                    _remainingTurboSeconds.value = repo.getRemainingTurboSecondsChecked()
                    refreshSecurityAudit()
                    showStatus("Clock tamper detected — using verified Turbo timer")
                } else {
                    if (now > maxObservedWallMs) maxObservedWallMs = now
                    val rem = computeRemainingSeconds(settings.value.turboTimeExpiresAtMs)
                    _remainingTurboSeconds.value = rem
                }
                delay(1000L)
            }
        }
    }

    private fun computeRemainingSeconds(expiresAtMs: Long): Long {
        if (repo.lastVaultTamperDetected) return 0L
        val diffMs = expiresAtMs - System.currentTimeMillis()
        return if (diffMs <= 0L) 0L else (diffMs + 999L) / 1000L
    }

    fun openWatchAdPrompt(targetGameName: String? = null, launchAfterReward: Boolean = false) {
        hardware.audioEnhancer.pulseHaptic(false)
        if (adMobManager != null &&
            !adMobManager.rewardedAdReady.value &&
            !adMobManager.rewardedAdLoading.value &&
            adMobManager.rewardedCooldownSeconds.value == 0
        ) {
            adMobManager.loadRewardedAd()
        }
        _adStep.value = RewardedAdStep.TimeGatePrompt(
            targetGameName = targetGameName,
            launchAfterReward = launchAfterReward,
            waitingForLoad = false
        )
    }

    fun retryLoadRewardedAd() {
        val manager = adMobManager ?: return
        if (manager.rewardedCooldownSeconds.value > 0) {
            showStatus("Ad warming up — auto-retrying in ${manager.rewardedCooldownSeconds.value}s")
            return
        }
        manager.loadRewardedAd()
    }

    fun watchRealRewardedAd(activity: Activity?) {
        val currentStep = _adStep.value as? RewardedAdStep.TimeGatePrompt ?: return
        if (activity == null || adMobManager == null) {
            showStatus("Ad service unavailable")
            return
        }
        hardware.audioEnhancer.pulseHaptic(false)

        if (adMobManager.rewardedAdReady.value) {
            showLoadedRewardedAd(activity, currentStep.launchAfterReward)
        } else if (adMobManager.rewardedCooldownSeconds.value > 0) {
            showStatus("Ad warming up — ready in ${adMobManager.rewardedCooldownSeconds.value}s")
        } else {
            _adStep.value = currentStep.copy(waitingForLoad = true)
            adMobManager.loadRewardedAd()
            viewModelScope.launch {
                withTimeoutOrNull(12_000L) {
                    adMobManager.rewardedAdLoading.first { !it }
                }
                val activeStep = _adStep.value as? RewardedAdStep.TimeGatePrompt
                if (activeStep != null && activeStep.waitingForLoad) {
                    if (adMobManager.rewardedAdReady.value && !activity.isFinishing && !activity.isDestroyed) {
                        showLoadedRewardedAd(activity, activeStep.launchAfterReward)
                    } else {
                        _adStep.value = activeStep.copy(waitingForLoad = false)
                    }
                }
            }
        }
    }

    private fun showLoadedRewardedAd(activity: Activity, launchAfterReward: Boolean) {
        val manager = adMobManager ?: return
        _adStep.value = RewardedAdStep.Hidden
        manager.showRewardedAd(
            activity = activity,
            onRewardEarned = {
                grantOneHourReward(launchAfterReward)
            },
            onClosedWithoutReward = {
                showStatus("Watch the full ad to earn +1h:00m Turbo Time")
            },
            onNotReady = {
                _adStep.value = RewardedAdStep.TimeGatePrompt(
                    targetGameName = null,
                    launchAfterReward = launchAfterReward,
                    waitingForLoad = false
                )
                showStatus("Loading AdMob Rewarded Ad, please try again")
            }
        )
    }

    private fun grantOneHourReward(launchAfterReward: Boolean) {
        val audit = refreshSecurityAudit()
        if (!audit.isSafeForGameplay) {
            showStatus("Security check failed: ${audit.reasons.firstOrNull() ?: "Tampered environment"}")
            return
        }
        hardware.audioEnhancer.playTacticalTone()
        hardware.audioEnhancer.pulseHaptic(true)
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            var newExpiresAt = now + AD_REWARD_DURATION_MS
            repo.updateSettings { current ->
                val base = maxOf(now, current.turboTimeExpiresAtMs)
                newExpiresAt = base + AD_REWARD_DURATION_MS
                current.copy(
                    turboTimeExpiresAtMs = newExpiresAt,
                    adsWatchedCount = current.adsWatchedCount + 1
                )
            }
            _remainingTurboSeconds.value = computeRemainingSeconds(newExpiresAt)
            showStatus("+1h:00m Turbo Time unlocked!")
            if (launchAfterReward) {
                val s = settings.value
                val pkg = s.selectedGamePackage ?: addedGames.value.firstOrNull()?.packageName
                if (pkg != null) {
                    launchGamePackage(pkg, s)
                }
            }
        }
    }

    fun maybeShowInterstitial(activity: Activity?, onComplete: () -> Unit = {}) {
        if (activity == null || adMobManager == null) {
            onComplete()
            return
        }
        adMobManager.showInterstitialIfReady(activity, onComplete)
    }

    fun suppressAppOpenForExternalFlow() {
        adMobManager?.suppressAppOpenTemporarily()
    }

    fun dismissAdDialog() {
        _adStep.value = RewardedAdStep.Hidden
    }

    fun markSplashComplete() {
        _showSplash.value = false
    }

    fun toggleStopwatch() {
        hardware.audioEnhancer.pulseHaptic(false)
        _isStopwatchRunning.value = !_isStopwatchRunning.value
    }

    fun resetStopwatch() {
        _isStopwatchRunning.value = false
        _stopwatchSeconds.value = 0L
    }

    fun updateSettings(transform: (GameTurboSettings) -> GameTurboSettings) {
        viewModelScope.launch {
            val prev = settings.value
            repo.updateSettings(transform)
            // Apply immediate side-effects when relevant settings change
            val next = transform(prev)
            if (next.rotationLock != prev.rotationLock) {
                hardware.setAutoRotate(!next.rotationLock)
                _autoRotate.value = !next.rotationLock
            }
            if (next.lockBrightness != prev.lockBrightness) {
                if (next.lockBrightness) {
                    hardware.setAutoBrightness(false)
                    hardware.setBrightnessFraction(_brightnessFraction.value)
                    _autoBrightness.value = false
                }
            }
            if (next.notificationMuteMode != prev.notificationMuteMode) {
                hardware.audioEnhancer.setNotificationMuted(next.notificationMuteMode)
            }
            if (next.dndMode != prev.dndMode) {
                hardware.setDoNotDisturb(next.dndMode)
            }
            if (next.xSpaceIntroEnabled && !prev.xSpaceIntroEnabled) {
                hasPlayedXSpaceIntroInSession = false
            }
            val t = _telemetry.value
            hardware.performanceEngine.applyProfile(
                settings = next,
                supportedRates = t.supportedRefreshRates,
                batteryPercent = t.batteryPercent,
                isCharging = t.isCharging
            )
        }
    }

    fun setFpsGameTarget(targetFps: Int) {
        hardware.audioEnhancer.pulseHaptic(false)
        updateSettings { it.copy(fpsGameTarget = targetFps) }
        val maxSupported = _telemetry.value.supportedRefreshRates.maxOrNull() ?: 60
        showStatus(
            if (targetFps == 0) "FPS Game Target: Auto"
            else "FPS Game Target: ${targetFps}Hz (Panel max ${maxSupported}Hz)"
        )
    }

    fun setFrameRateTarget(targetHz: Int) {
        hardware.audioEnhancer.pulseHaptic(false)
        updateSettings { it.copy(frameRateTarget = targetHz) }
        val maxSupported = _telemetry.value.supportedRefreshRates.maxOrNull() ?: 60
        val note = if (targetHz > maxSupported) " (Panel max ${maxSupported}Hz)" else ""
        showStatus("Display Frame Rate: ${targetHz}Hz$note")
    }

    fun setSlidingFrictionIndex(index: Int) {
        updateSettings { it.copy(slidingFrictionIndex = index) }
        val factor = com.example.data.SLIDING_FRICTION_FACTORS[index.coerceIn(0, com.example.data.SLIDING_FRICTION_FACTORS.lastIndex)]
        val label = com.example.data.SLIDING_FRICTION_LABELS[index.coerceIn(0, com.example.data.SLIDING_FRICTION_LABELS.lastIndex)]
        showStatus("Sliding Friction: $label (Glide x${String.format(java.util.Locale.US, "%.2f", factor)})")
    }

    fun setScrollingSensitivity(value: Int) {
        updateSettings { it.copy(scrollingSensitivity = value.coerceIn(-7, 7)) }
        val pointerOk = hardware.checkPermissions().writeSettingsGranted
        val sign = if (value > 0) "+$value" else "$value"
        showStatus(
            if (pointerOk) "Scrolling Sensitivity $sign applied to touch & pointer"
            else "Scrolling Sensitivity $sign (App & Overlay active)"
        )
    }

    fun setEnhanceTouchSensitivity(enabled: Boolean) {
        hardware.audioEnhancer.pulseHaptic(false)
        updateSettings { it.copy(enhanceTouchSensitivity = enabled) }
        showStatus(
            if (enabled) "Touch Sensitivity Enhanced (Unbuffered +18%)"
            else "Standard Touch Sensitivity"
        )
    }

    fun setReduceTouchLatency(enabled: Boolean) {
        hardware.audioEnhancer.pulseHaptic(false)
        updateSettings { it.copy(reduceTouchLatency = enabled) }
        showStatus(
            if (enabled) "Unbuffered Touch Dispatch & Display Thread Priority ON"
            else "Standard Touch Latency"
        )
    }

    fun setIncreaseCpuPerformance(enabled: Boolean) {
        hardware.audioEnhancer.pulseHaptic(enabled)
        updateSettings {
            it.copy(
                increaseCpuPerformance = enabled,
                cpuMode = if (enabled) CpuMode.ACCELERATION else it.cpuMode
            )
        }
        if (enabled) {
            viewModelScope.launch {
                val cur = settings.value
                val res = hardware.performRealMemoryBoost(
                    protectedPackages = cur.addedGamePackages,
                    allKnownPackages = _installedApps.value.map { it.packageName },
                    backgroundLimitIndex = cur.backgroundLimitIndex,
                    cacheLimitIndex = cur.cacheLimitIndex
                )
                showStatus("CPU Boost Active (ADPF) • Freed ${res.freedMb} MB")
            }
        } else {
            showStatus("CPU Performance Boost Off")
        }
    }

    fun setIncreaseGpuPerformance(enabled: Boolean) {
        hardware.audioEnhancer.pulseHaptic(enabled)
        updateSettings { it.copy(increaseGpuPerformance = enabled) }
        showStatus(
            if (enabled) "GPU Performance Boost + Hardware Color Pipeline Active"
            else "GPU Performance Boost Off"
        )
    }

    fun setForceGpuOpenGl(enabled: Boolean) {
        hardware.audioEnhancer.pulseHaptic(false)
        updateSettings {
            it.copy(
                forceGpuOpenGl = enabled,
                enableSkiaVulkan = if (enabled) false else it.enableSkiaVulkan
            )
        }
        showStatus(
            if (enabled) "UI Pipeline: Force GPU (OpenGL ES) enabled"
            else "UI Pipeline: System Default"
        )
    }

    fun setEnableSkiaVulkan(enabled: Boolean) {
        hardware.audioEnhancer.pulseHaptic(false)
        val gpuInfo = hardware.performanceEngine.queryGpuPipelineInfo()
        if (enabled && !gpuInfo.vulkanSupported) {
            updateSettings { it.copy(enableSkiaVulkan = false) }
            showStatus("Vulkan hardware not supported on this device")
            return
        }
        updateSettings {
            it.copy(
                enableSkiaVulkan = enabled,
                forceGpuOpenGl = if (enabled) false else it.forceGpuOpenGl
            )
        }
        showStatus(
            if (enabled) "UI Pipeline: Skia (${gpuInfo.vulkanVersionLabel}) prioritized"
            else "Skia Vulkan override Off"
        )
    }

    fun setHighPerfFpsControl(enabled: Boolean) {
        hardware.audioEnhancer.pulseHaptic(enabled)
        updateSettings { it.copy(highPerfFpsControl = enabled) }
        val maxPanel = _telemetry.value.supportedRefreshRates.maxOrNull() ?: 60
        showStatus(
            if (enabled) "High Perf FPS Control Locked (${maxOf(settings.value.frameRateTarget, settings.value.fpsGameTarget, maxPanel)}Hz target)"
            else "High Perf FPS Control Off"
        )
    }

    fun openDeveloperRenderingOptions() {
        adMobManager?.suppressAppOpenTemporarily()
        val openedDev = hardware.performanceEngine.openDeveloperRenderingOptions()
        if (!openedDev) {
            showStatus("Enable Developer Options in About Phone -> Tap Build Number 7x")
        }
    }

    fun setOptimizeFramePacing(enabled: Boolean) {
        hardware.audioEnhancer.pulseHaptic(false)
        updateSettings { it.copy(optimizeFramePacing = enabled) }
        showStatus(
            if (enabled) "Frame Pacing Optimization & Minimal Post-Processing ON"
            else "Frame Pacing Optimization Off"
        )
    }

    fun setEnableAdpfHints(enabled: Boolean) {
        hardware.audioEnhancer.pulseHaptic(false)
        updateSettings { it.copy(enableAdpfHints = enabled) }
        val adpfStatus = hardware.performanceEngine.status.value
        showStatus(
            when {
                !enabled -> "ADPF PerformanceHintManager Disabled"
                adpfStatus.adpfSupported -> "ADPF Performance Hints Enabled"
                else -> "ADPF Preference Saved (Requires Android 12+ PowerHAL)"
            }
        )
    }

    fun setDisablePowerSavingInGame(enabled: Boolean) {
        hardware.audioEnhancer.pulseHaptic(false)
        updateSettings { it.copy(disablePowerSavingInGame = enabled) }
        val saverOn = hardware.performanceEngine.isBatterySaverActive()
        showStatus(
            when {
                enabled && saverOn -> "Battery Saver is ON! Tap Power Settings below to disable for full FPS"
                enabled -> "Gaming Battery Saver Guard Armed"
                else -> "Battery Saver Guard Off"
            }
        )
    }

    fun openPowerSaverSettings() {
        adMobManager?.suppressAppOpenTemporarily()
        if (!hardware.performanceEngine.openPowerSaverSettings()) {
            showStatus("Open Device Settings -> Battery to disable Battery Saver")
        }
    }

    fun setSmartBackgroundControl(enabled: Boolean) {
        hardware.audioEnhancer.pulseHaptic(false)
        updateSettings { it.copy(smartBackgroundControl = enabled, backgroundControlEnabled = enabled) }
        showStatus(
            if (enabled) "Smart Background Activity Control Enabled"
            else "Smart Background Activity Control Paused"
        )
    }

    fun runSmartBackgroundTrim() {
        performBoost()
    }

    fun openAppManagementSettings() {
        adMobManager?.suppressAppOpenTemporarily()
        if (!hardware.performanceEngine.openAppManagementSettings()) {
            showStatus("Open Device Settings -> Apps to manage background apps")
        }
    }

    fun setDisplayResolutionOpt(enabled: Boolean) {
        hardware.audioEnhancer.pulseHaptic(false)
        updateSettings { it.copy(displayResolutionOpt = enabled) }
        val res = hardware.performanceEngine.status.value.currentResolutionLabel
        showStatus(
            if (enabled) "Display Resolution Optimizer Active ($res)"
            else "Display Resolution Optimizer Off"
        )
    }

    fun selectDisplayModeId(modeId: Int) {
        hardware.audioEnhancer.pulseHaptic(false)
        updateSettings { it.copy(displayResolutionOpt = true, preferredDisplayModeId = modeId) }
        val match = hardware.performanceEngine.status.value.supportedResolutionModes.firstOrNull { it.modeId == modeId }
        showStatus(
            if (match != null) "Display Mode: ${match.label}" else "Preferred Display Mode Updated"
        )
    }

    fun openDisplaySettings() {
        adMobManager?.suppressAppOpenTemporarily()
        if (!hardware.performanceEngine.openDisplaySettings()) {
            showStatus("Open Device Settings -> Display for system resolution options")
        }
    }

    fun setSustainedPerformanceMode(enabled: Boolean) {
        hardware.audioEnhancer.pulseHaptic(false)
        updateSettings { it.copy(sustainedPerformanceMode = enabled) }
        val supported = hardware.performanceEngine.status.value.sustainedPerformanceSupported
        showStatus(
            when {
                !enabled -> "Sustained Performance Mode Off"
                supported -> "Android Sustained Performance Mode Engaged"
                else -> "Sustained Mode Saved (Not reported supported on this device)"
            }
        )
    }

    fun setAutoPerformanceOptimization(enabled: Boolean) {
        hardware.audioEnhancer.pulseHaptic(enabled)
        if (!enabled) {
            updateSettings { it.copy(autoPerformanceOptimization = false) }
            showStatus("Auto Performance Optimization Off (Manual profile)")
            return
        }
        val t = _telemetry.value
        val decision = hardware.performanceEngine.evaluateAutoPerformance(
            batteryPercent = t.batteryPercent,
            isCharging = t.isCharging
        )
        updateSettings {
            it.copy(
                autoPerformanceOptimization = true,
                cpuMode = decision.recommendedCpuMode,
                sustainedPerformanceMode = decision.recommendSustained
            )
        }
        showStatus("Auto Optimize: ${decision.reasonLabel}")
    }

    fun toggleGameAdded(packageName: String) {
        hardware.audioEnhancer.pulseHaptic(false)
        if (packageName == settings.value.superPerfSelectedGamePackage && extremeManager.isSessionActive) {
            extremeManager.deactivateAndRestoreAll(
                reason = "Removed selected game from library",
                settings = settings.value,
                installedApps = _installedApps.value,
                telemetry = _telemetry.value,
                permissions = _permissions.value
            )
        }
        updateSettings { current ->
            val nextSet = if (packageName in current.addedGamePackages) {
                current.addedGamePackages - packageName
            } else {
                current.addedGamePackages + packageName
            }
            val nextSelected = when {
                current.selectedGamePackage in nextSet -> current.selectedGamePackage
                else -> nextSet.firstOrNull()
            }
            val nextSuperSelected = current.superPerfSelectedGamePackage?.takeIf { it in nextSet }
            current.copy(
                addedGamePackages = nextSet,
                selectedGamePackage = nextSelected,
                superPerfSelectedGamePackage = nextSuperSelected
            )
        }
    }

    fun selectGame(packageName: String) {
        hardware.audioEnhancer.pulseHaptic(false)
        viewModelScope.launch {
            if (extremeManager.isSessionActive && extremeManager.activePackageName != packageName) {
                extremeManager.deactivateAndRestoreAll(
                    reason = "Stopped previous session before switching games",
                    settings = settings.value,
                    installedApps = _installedApps.value,
                    telemetry = _telemetry.value,
                    permissions = _permissions.value
                )
            }
            updateSettings { it.copy(selectedGamePackage = packageName) }
        }
    }

    fun selectSuperPerfGame(packageName: String?, activity: Activity? = null) {
        hardware.audioEnhancer.pulseHaptic(false)
        viewModelScope.launch {
            extremeManager.selectGame(
                packageName = packageName,
                settings = settings.value,
                installedApps = _installedApps.value,
                telemetry = _telemetry.value,
                permissions = _permissions.value,
                activity = activity
            )
            if (packageName != null) {
                val appName = _installedApps.value.firstOrNull { it.packageName == packageName }?.appName ?: packageName
                showStatus("Selected $appName for Super High Performance")
            } else {
                showStatus("Selected game cleared")
            }
        }
    }

    fun toggleSuperPerfAutoStart(enabled: Boolean) {
        hardware.audioEnhancer.pulseHaptic(false)
        updateSettings { it.copy(superPerfAutoStartOnLaunch = enabled) }
    }

    fun openSuperPerfWarningDialog(activateOnConfirm: Boolean = false) {
        hardware.audioEnhancer.pulseHaptic(false)
        _superPerfPendingActivateAfterWarning.value = activateOnConfirm
        _showSuperPerfWarningDialog.value = true
    }

    fun dismissSuperPerfWarningDialog() {
        _superPerfPendingActivateAfterWarning.value = false
        _showSuperPerfWarningDialog.value = false
    }

    fun confirmSuperPerfWarningDialog(activity: Activity? = null) {
        val shouldActivate = _superPerfPendingActivateAfterWarning.value
        _superPerfPendingActivateAfterWarning.value = false
        _showSuperPerfWarningDialog.value = false
        viewModelScope.launch {
            extremeManager.acceptWarningDialog(
                settings = settings.value,
                installedApps = _installedApps.value,
                telemetry = _telemetry.value,
                permissions = _permissions.value
            )
            if (shouldActivate) {
                activateSuperHighPerformance(activity)
            }
        }
    }

    fun activateSuperHighPerformance(activity: Activity? = null) {
        hardware.audioEnhancer.pulseHaptic(true)
        viewModelScope.launch {
            val result = extremeManager.activateForSelectedGame(
                settings = settings.value,
                installedApps = _installedApps.value,
                telemetry = _telemetry.value,
                permissions = _permissions.value,
                knownNonEssentialPackages = _installedApps.value.map { it.packageName },
                activity = activity,
                manualActivation = true
            )
            when (result) {
                is ActivationAttemptResult.NeedsWarningConfirmation -> {
                    openSuperPerfWarningDialog(activateOnConfirm = true)
                }
                is ActivationAttemptResult.Blocked -> {
                    showStatus(result.reason)
                }
                is ActivationAttemptResult.Activated -> {
                    hardware.audioEnhancer.playTacticalTone()
                    showStatus("Super High Performance ACTIVE for ${result.game.appName}")
                }
            }
        }
    }

    fun deactivateSuperHighPerformance(
        reason: String = "Disabled by user — all temporary settings restored",
        activity: Activity? = null
    ) {
        hardware.audioEnhancer.pulseHaptic(false)
        extremeManager.deactivateAndRestoreAll(
            reason = reason,
            settings = settings.value,
            installedApps = _installedApps.value,
            telemetry = _telemetry.value,
            permissions = _permissions.value,
            activity = activity
        )
        showStatus("Super High Performance stopped & settings restored")
    }

    fun watchSuperPerfUnlockAd(activity: Activity?) {
        val unlockState = extremeUiState.value.unlockState
        if (unlockState.isUnlocked) {
            showStatus("Super High Performance is already unlocked")
            return
        }
        val step = unlockState.nextRequiredStep
        val token = extremeManager.unlockManager.beginAdAttempt(step) ?: return

        if (activity == null || adMobManager == null) {
            extremeManager.unlockManager.recordAdSkippedOrFailed("Ad service unavailable")
            showStatus("Ad service unavailable — try again on a connected device")
            return
        }
        hardware.audioEnhancer.pulseHaptic(false)

        if (adMobManager.rewardedAdReady.value) {
            showSuperPerfRewardedAd(activity, step, token)
        } else if (adMobManager.rewardedCooldownSeconds.value > 0) {
            extremeManager.unlockManager.recordAdSkippedOrFailed(
                "Ad warming up — retry in ${adMobManager.rewardedCooldownSeconds.value}s"
            )
            showStatus("Ad warming up — ready in ${adMobManager.rewardedCooldownSeconds.value}s")
        } else {
            adMobManager.loadRewardedAd()
            viewModelScope.launch {
                withTimeoutOrNull(12_000L) {
                    adMobManager.rewardedAdLoading.first { !it }
                }
                if (adMobManager.rewardedAdReady.value && !activity.isFinishing && !activity.isDestroyed) {
                    showSuperPerfRewardedAd(activity, step, token)
                } else {
                    val err = adMobManager.rewardedAdError.value ?: "Rewarded ad not ready yet. Please retry."
                    extremeManager.unlockManager.recordAdSkippedOrFailed(err)
                    showStatus(err)
                }
            }
        }
    }

    private fun showSuperPerfRewardedAd(activity: Activity, step: Int, sessionToken: String) {
        val manager = adMobManager ?: return
        manager.showRewardedAd(
            activity = activity,
            onRewardEarned = {
                viewModelScope.launch {
                    val res = extremeManager.unlockManager.verifyAdRewardEarned(step, sessionToken)
                    extremeManager.syncState(
                        settings = settings.value,
                        installedApps = _installedApps.value,
                        telemetry = _telemetry.value,
                        permissions = _permissions.value
                    )
                    when (res) {
                        is UnlockStepResult.Step1Completed -> {
                            hardware.audioEnhancer.pulseHaptic(true)
                            showStatus("Ad 1 of 2 complete! Watch 1 more ad to unlock.")
                        }
                        is UnlockStepResult.Unlocked -> {
                            hardware.audioEnhancer.playTacticalTone()
                            hardware.audioEnhancer.pulseHaptic(true)
                            showStatus("Super High Performance UNLOCKED!")
                        }
                        is UnlockStepResult.Rejected -> {
                            showStatus(res.reason)
                        }
                    }
                }
            },
            onClosedWithoutReward = {
                extremeManager.unlockManager.recordAdSkippedOrFailed(
                    "Ad $step skipped before reward — step not counted"
                )
                showStatus("Watch the full rewarded ad ($step/2) to earn unlock progress")
            },
            onNotReady = {
                extremeManager.unlockManager.recordAdSkippedOrFailed("Loading rewarded ad — please retry")
                showStatus("Loading rewarded ad, please try again")
            }
        )
    }

    fun setCpuMode(mode: CpuMode) {
        hardware.audioEnhancer.pulseHaptic(false)
        updateSettings { it.copy(cpuMode = mode) }
        showStatus("Switched to ${mode.label}")
    }

    fun setWallpaperTheme(theme: WallpaperTheme, customUri: String? = null) {
        updateSettings {
            it.copy(
                wallpaperTheme = theme,
                customWallpaperUri = if (theme == WallpaperTheme.CUSTOM) customUri else it.customWallpaperUri
            )
        }
    }

    fun cycleCrosshair() {
        hardware.audioEnhancer.pulseHaptic(false)
        val entries = CrosshairStyle.entries
        val next = entries[(settings.value.crosshairStyle.ordinal + 1) % entries.size]
        updateSettings { it.copy(crosshairStyle = next) }
        showStatus("Crosshair: ${next.label}")
    }

    fun cycleScreenColor() {
        hardware.audioEnhancer.pulseHaptic(false)
        val entries = ScreenColorMode.entries
        val next = entries[(settings.value.screenColorMode.ordinal + 1) % entries.size]
        updateSettings { it.copy(screenColorMode = next) }
        showStatus("Screen Color: ${next.label}")
    }

    fun toggleOrCycleScreenZoom() {
        hardware.audioEnhancer.pulseHaptic(false)
        val s = settings.value
        if (!s.screenZoomEnabled) {
            updateSettings { it.copy(screenZoomEnabled = true, zoomScale = 1.5f) }
            showStatus("Screen Zoom: 1.5x")
        } else if (s.zoomScale < 2.5f) {
            val nextScale = if (s.zoomScale < 1.8f) 2.0f else 3.0f
            updateSettings { it.copy(zoomScale = nextScale) }
            showStatus("Screen Zoom: ${nextScale}x")
        } else {
            updateSettings { it.copy(screenZoomEnabled = false, zoomScale = 2.0f) }
            showStatus("Screen Zoom Off")
        }
    }

    fun cycleEffectSound() {
        hardware.audioEnhancer.pulseHaptic(false)
        val entries = EqPreset.entries
        val current = settings.value
        val nextPreset = entries[(current.eqPreset.ordinal + 1) % entries.size]
        val enabled = nextPreset != EqPreset.OFF
        val applied = hardware.audioEnhancer.applyEqPreset(enabled, nextPreset)
        if (enabled) hardware.audioEnhancer.playTacticalTone()
        updateSettings { it.copy(effectSoundEnabled = enabled, eqPreset = nextPreset) }
        showStatus(
            if (enabled) {
                if (applied) "Audio EQ: ${nextPreset.label}" else "Profile: ${nextPreset.label} (System EQ active)"
            } else "Effect Sound: Standard"
        )
    }

    fun cycleFrameRate() {
        hardware.audioEnhancer.pulseHaptic(false)
        val supported = _telemetry.value.supportedRefreshRates
        val options = listOf(60, 90, 120, 144)
        val currentIdx = options.indexOf(settings.value.frameRateTarget).coerceAtLeast(0)
        val next = options[(currentIdx + 1) % options.size]
        updateSettings { it.copy(frameRateTarget = next) }
        val maxSupported = supported.maxOrNull() ?: 60
        val note = if (next > maxSupported) " (Panel max ${maxSupported}Hz)" else ""
        showStatus("Frame Rate Target: ${next}Hz$note")
    }

    fun runPingTest() {
        viewModelScope.launch {
            showStatus("Measuring latency & jitter...")
            val (ping, jitter) = hardware.measurePingWithJitter()
            lastPingMs = ping
            lastJitterMs = jitter
            val mode = settings.value.cpuMode
            val fps = measuredFps
            val frameMs = measuredFrameMs
            val updated = withContext(Dispatchers.Default) {
                hardware.readHardwareTelemetry(
                    cpuMode = mode,
                    lastPingMs = ping,
                    lastJitterMs = jitter,
                    actualFps = fps,
                    frameTimeMs = frameMs
                )
            }
            _telemetry.value = updated
            if (ping != null) {
                updateSettings { it.copy(reducePingEnabled = true) }
                showStatus("Ping: ${ping}ms • Jitter: ${jitter ?: 0}ms (${updated.networkType})")
            } else {
                showStatus("Network offline — connect Wi-Fi or Data")
            }
        }
    }

    fun dismissOneTapBooster() {
        oneTapJob?.cancel()
        oneTapJob = null
        _oneTapBooster.value = _oneTapBooster.value.copy(isVisible = false, isRunning = false)
    }

    fun runOneTapBooster(stepDelayMs: Long = 220L) {
        hardware.audioEnhancer.pulseHaptic(false)
        oneTapJob?.cancel()
        oneTapJob = viewModelScope.launch {
            val initialSteps = OneTapBoosterState.defaultSteps()
            _oneTapBooster.value = OneTapBoosterState(
                isVisible = true,
                isRunning = true,
                isCompleted = false,
                currentStepIndex = 0,
                progress = 0.06f,
                steps = updateStepList(
                    initialSteps,
                    BoostStepId.DEVICE_SCAN,
                    BoostStepStatus.RUNNING,
                    "Scanning"
                )
            )
            if (stepDelayMs > 0L) delay(stepDelayMs)

            // STEP 1: Device Performance Scan (CPU, RAM, Battery, Thermal)
            val currentMode = settings.value.cpuMode
            val scanTelemetry = withContext(Dispatchers.Default) {
                hardware.readHardwareTelemetry(
                    cpuMode = currentMode,
                    lastPingMs = lastPingMs,
                    lastJitterMs = lastJitterMs,
                    actualFps = measuredFps,
                    frameTimeMs = measuredFrameMs
                )
            }
            _telemetry.value = scanTelemetry
            val scanThermal = hardware.performanceEngine.readThermalStatusLabel()
            val scanDetail = "${scanTelemetry.cpuFreqMHz}MHz (${scanTelemetry.cpuCoreCount}C) • RAM ${scanTelemetry.ramUsedPercent}% • Bat ${scanTelemetry.batteryPercent}% • ${scanTelemetry.batteryTempCelsius}°C"

            var steps = updateStepList(
                _oneTapBooster.value.steps,
                BoostStepId.DEVICE_SCAN,
                BoostStepStatus.DONE,
                "${scanTelemetry.cpuFreqMHz}MHz",
                scanDetail
            )
            steps = updateStepList(steps, BoostStepId.MEMORY_OPTIMIZATION, BoostStepStatus.RUNNING, "Analyzing")
            _oneTapBooster.value = _oneTapBooster.value.copy(
                currentStepIndex = 1,
                progress = 0.22f,
                steps = steps,
                scanCpuMHz = scanTelemetry.cpuFreqMHz,
                scanCpuCores = scanTelemetry.cpuCoreCount,
                ramUsedBeforePercent = scanTelemetry.ramUsedPercent,
                thermalLabel = scanThermal,
                batteryTempC = scanTelemetry.batteryTempCelsius,
                batteryPercent = scanTelemetry.batteryPercent,
                isCharging = scanTelemetry.isCharging
            )
            if (stepDelayMs > 0L) delay(stepDelayMs)

            // STEP 2: Memory Optimization (Pressure Check + Advice + Real Memory Trim)
            val (pressureLabel, pressureAdvice) = hardware.performanceEngine.evaluateMemoryPressure(scanTelemetry.ramUsedPercent)
            val allPackages = _installedApps.value.map { it.packageName }
            val curBoostSettings = settings.value
            val boostRes = hardware.performRealMemoryBoost(
                protectedPackages = curBoostSettings.addedGamePackages,
                allKnownPackages = allPackages,
                backgroundLimitIndex = curBoostSettings.backgroundLimitIndex,
                cacheLimitIndex = curBoostSettings.cacheLimitIndex
            )
            val afterTelemetry = withContext(Dispatchers.Default) {
                hardware.readHardwareTelemetry(
                    cpuMode = currentMode,
                    lastPingMs = lastPingMs,
                    lastJitterMs = lastJitterMs,
                    actualFps = measuredFps,
                    frameTimeMs = measuredFrameMs
                )
            }
            _telemetry.value = afterTelemetry
            val memStatus = if (pressureLabel == "CRITICAL") BoostStepStatus.WARNING else BoostStepStatus.DONE
            val memDetail = "$pressureLabel pressure • Freed ${boostRes.freedMb}MB (${boostRes.trimmedPackages} apps, ${boostRes.clearedCacheKb}KB cache). $pressureAdvice"

            steps = updateStepList(
                steps,
                BoostStepId.MEMORY_OPTIMIZATION,
                memStatus,
                "+${boostRes.freedMb} MB",
                memDetail
            )
            steps = updateStepList(steps, BoostStepId.THERMAL_PROTECTION, BoostStepStatus.RUNNING, "Checking")
            _oneTapBooster.value = _oneTapBooster.value.copy(
                currentStepIndex = 2,
                progress = 0.42f,
                steps = steps,
                ramUsedAfterPercent = afterTelemetry.ramUsedPercent,
                memoryPressureLabel = pressureLabel,
                memoryAdvice = pressureAdvice,
                freedMb = boostRes.freedMb,
                trimmedApps = boostRes.trimmedPackages,
                clearedCacheKb = boostRes.clearedCacheKb
            )
            if (stepDelayMs > 0L) delay(stepDelayMs)

            // STEP 3: Thermal Protection
            val sustainedSupported = hardware.performanceEngine.isSustainedPerformanceSupported()
            val thermalHot = scanThermal == "HIGH" || scanThermal == "THROTTLED" || scanThermal == "MODERATE" || afterTelemetry.batteryTempCelsius >= 39.5f
            val thermalStatus = if (scanThermal == "HIGH" || scanThermal == "THROTTLED") BoostStepStatus.WARNING else BoostStepStatus.DONE
            val thermalAdvice = when {
                scanThermal == "HIGH" || scanThermal == "THROTTLED" ->
                    "High thermal load (${afterTelemetry.batteryTempCelsius}°C, $scanThermal) — Sustained Thermal Guard engaged."
                thermalHot ->
                    "Warm state (${afterTelemetry.batteryTempCelsius}°C, $scanThermal) — balancing sustained clocks to prevent throttling."
                else ->
                    "Optimal thermals (${afterTelemetry.batteryTempCelsius}°C, $scanThermal) — full thermal headroom unlocked."
            }
            steps = updateStepList(
                steps,
                BoostStepId.THERMAL_PROTECTION,
                thermalStatus,
                "${afterTelemetry.batteryTempCelsius}°C",
                thermalAdvice
            )
            steps = updateStepList(steps, BoostStepId.POWER_OPTIMIZATION, BoostStepStatus.RUNNING, "Inspecting")
            _oneTapBooster.value = _oneTapBooster.value.copy(
                currentStepIndex = 3,
                progress = 0.60f,
                steps = steps,
                thermalLabel = scanThermal,
                batteryTempC = afterTelemetry.batteryTempCelsius,
                thermalAdvice = thermalAdvice
            )
            if (stepDelayMs > 0L) delay(stepDelayMs)

            // STEP 4: Power Optimization (Battery Saver & Exemption Check)
            val (batterySaverOn, ignoringBatteryOpt) = hardware.performanceEngine.detectBatterySaverState()
            val powerStatus = if (batterySaverOn) BoostStepStatus.WARNING else BoostStepStatus.DONE
            val powerAdvice = when {
                batterySaverOn ->
                    "Battery Saver is ON (restricts CPU/FPS). Tap 'Open Power Settings' in Report to disable for peak speed."
                ignoringBatteryOpt ->
                    "Battery Saver OFF • Turbo X battery optimization exempted (${afterTelemetry.batteryPercent}%)."
                else ->
                    "Battery Saver OFF • Standard power active (${afterTelemetry.batteryPercent}%${if (afterTelemetry.isCharging) ", Charging" else ""})."
            }
            steps = updateStepList(
                steps,
                BoostStepId.POWER_OPTIMIZATION,
                powerStatus,
                if (batterySaverOn) "Saver ON" else "${afterTelemetry.batteryPercent}% OK",
                powerAdvice
            )
            steps = updateStepList(steps, BoostStepId.GAMING_PROFILE, BoostStepStatus.RUNNING, "Tuning")
            _oneTapBooster.value = _oneTapBooster.value.copy(
                currentStepIndex = 4,
                progress = 0.78f,
                steps = steps,
                batterySaverActive = batterySaverOn,
                ignoringBatteryOpt = ignoringBatteryOpt,
                powerAdvice = powerAdvice
            )
            if (stepDelayMs > 0L) delay(stepDelayMs)

            // STEP 5: Gaming Profile Selection & Application
            val autoDecision = hardware.performanceEngine.evaluateAutoPerformance(
                batteryPercent = afterTelemetry.batteryPercent,
                isCharging = afterTelemetry.isCharging
            )
            val useSustained = autoDecision.recommendSustained || (thermalHot && sustainedSupported)
            val nextSettingsSnapshot = settings.value.copy(
                cpuMode = autoDecision.recommendedCpuMode,
                sustainedPerformanceMode = useSustained,
                optimizeFramePacing = true,
                enableAdpfHints = true,
                disablePowerSavingInGame = true,
                increaseCpuPerformance = autoDecision.recommendedCpuMode == CpuMode.ACCELERATION,
                totalBoostCount = settings.value.totalBoostCount + 1,
                lastFreedMb = boostRes.freedMb
            )
            repo.updateSettings {
                it.copy(
                    cpuMode = nextSettingsSnapshot.cpuMode,
                    sustainedPerformanceMode = nextSettingsSnapshot.sustainedPerformanceMode,
                    optimizeFramePacing = true,
                    enableAdpfHints = true,
                    disablePowerSavingInGame = true,
                    increaseCpuPerformance = nextSettingsSnapshot.increaseCpuPerformance,
                    totalBoostCount = it.totalBoostCount + 1,
                    lastFreedMb = boostRes.freedMb
                )
            }
            hardware.performanceEngine.applyProfile(
                settings = nextSettingsSnapshot,
                supportedRates = afterTelemetry.supportedRefreshRates,
                batteryPercent = afterTelemetry.batteryPercent,
                isCharging = afterTelemetry.isCharging
            )
            val profileLabel = "${autoDecision.recommendedCpuMode.label} • ADPF + Frame Pacing"
            steps = updateStepList(
                steps,
                BoostStepId.GAMING_PROFILE,
                BoostStepStatus.DONE,
                autoDecision.recommendedCpuMode.label,
                "${autoDecision.reasonLabel} • Frame Pacing & ADPF active"
            )
            steps = updateStepList(steps, BoostStepId.FINAL_REPORT, BoostStepStatus.RUNNING, "Finalizing")
            _oneTapBooster.value = _oneTapBooster.value.copy(
                currentStepIndex = 5,
                progress = 0.92f,
                steps = steps,
                selectedCpuMode = autoDecision.recommendedCpuMode,
                sustainedEnabled = useSustained,
                appliedProfileLabel = profileLabel
            )
            if (stepDelayMs > 0L) delay(stepDelayMs)

            // STEP 6: Final Performance Report
            val score = computeBoosterScore(
                ramAfterPercent = afterTelemetry.ramUsedPercent,
                thermalLabel = scanThermal,
                batterySaverOn = batterySaverOn,
                freedMb = boostRes.freedMb
            )
            val verdict = when {
                batterySaverOn -> "POWER SAVER LIMIT"
                scanThermal == "HIGH" || scanThermal == "THROTTLED" -> "THERMAL GUARDED"
                score >= 85 -> "BATTLE READY"
                else -> "OPTIMIZED"
            }
            steps = updateStepList(
                steps,
                BoostStepId.FINAL_REPORT,
                if (batterySaverOn) BoostStepStatus.WARNING else BoostStepStatus.DONE,
                "$score/100",
                "Freed ${boostRes.freedMb} MB • $profileLabel • Verdict: $verdict"
            )
            hardware.audioEnhancer.playTacticalTone()
            hardware.audioEnhancer.pulseHaptic(true)
            _oneTapBooster.value = _oneTapBooster.value.copy(
                isRunning = false,
                isCompleted = true,
                currentStepIndex = 5,
                progress = 1f,
                steps = steps,
                finalScore = score,
                finalVerdict = verdict
            )
            showStatus("One Tap Complete • Freed ${boostRes.freedMb} MB • Score $score/100")
        }
    }

    private fun updateStepList(
        current: List<OneTapStepResult>,
        targetId: BoostStepId,
        status: BoostStepStatus,
        badge: String,
        detail: String? = null
    ): List<OneTapStepResult> = current.map { item ->
        if (item.id == targetId) {
            item.copy(
                status = status,
                metricBadge = badge,
                detail = detail ?: item.detail
            )
        } else {
            item
        }
    }

    private fun computeBoosterScore(
        ramAfterPercent: Int,
        thermalLabel: String,
        batterySaverOn: Boolean,
        freedMb: Int
    ): Int {
        var score = 100
        if (ramAfterPercent >= 85) score -= 18
        else if (ramAfterPercent >= 70) score -= 9
        else if (ramAfterPercent >= 55) score -= 4

        when (thermalLabel) {
            "THROTTLED", "HIGH" -> score -= 16
            "MODERATE" -> score -= 8
            "WARM" -> score -= 3
        }
        if (batterySaverOn) score -= 18
        if (freedMb >= 32) score += 2
        return score.coerceIn(45, 100)
    }

    fun performBoost() {
        hardware.audioEnhancer.pulseHaptic(true)
        viewModelScope.launch {
            val cur = settings.value
            val allPackages = _installedApps.value.map { it.packageName }
            val result = hardware.performRealMemoryBoost(
                protectedPackages = cur.addedGamePackages,
                allKnownPackages = allPackages,
                backgroundLimitIndex = cur.backgroundLimitIndex,
                cacheLimitIndex = cur.cacheLimitIndex
            )
            updateSettings {
                it.copy(
                    totalBoostCount = it.totalBoostCount + 1,
                    lastFreedMb = result.freedMb
                )
            }
            val mode = settings.value.cpuMode
            val ping = lastPingMs
            val jitter = lastJitterMs
            val fps = measuredFps
            val frameMs = measuredFrameMs
            _telemetry.value = withContext(Dispatchers.Default) {
                hardware.readHardwareTelemetry(
                    cpuMode = mode,
                    lastPingMs = ping,
                    lastJitterMs = jitter,
                    actualFps = fps,
                    frameTimeMs = frameMs
                )
            }
            showStatus("Boosted! Freed ${result.freedMb} MB • Trimmed ${result.trimmedPackages} apps")
        }
    }

    fun clearCache() {
        hardware.audioEnhancer.pulseHaptic(false)
        viewModelScope.launch {
            val freedKb = hardware.clearAppCaches(settings.value.cacheLimitIndex)
            showStatus("Cache cleared — ${freedKb} KB reclaimed")
        }
    }

    fun engageTurboX() {
        hardware.audioEnhancer.pulseHaptic(true)
        val next = !settings.value.turboXMode
        updateSettings { s ->
            s.copy(
                turboXMode = next,
                cpuMode = if (next) CpuMode.ACCELERATION else CpuMode.BALANCED,
                increaseCpuPerformance = next,
                increaseGpuPerformance = next,
                highPerfFpsControl = next,
                activeInfoHud = if (next) true else s.activeInfoHud
            )
        }
        if (next) {
            hardware.audioEnhancer.applyEqPreset(true, settings.value.eqPreset)
            performBoost()
        } else {
            showStatus("Turbo X Overdrive Disengaged")
        }
    }

    fun captureScreenshot(activity: Activity?) {
        hardware.audioEnhancer.pulseHaptic(false)
        if (activity != null) {
            ScreenshotHelper.captureActivityWindow(activity) { success, detail ->
                showStatus(if (success) "Screenshot saved: $detail" else detail)
            }
        } else {
            val ctx = getApplication<Application>()
            runCatching {
                ctx.startActivity(
                    Intent(ctx, ScreenCaptureActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }.onFailure {
                showStatus("Screen capture permission cannot be launched from here")
            }
        }
    }

    fun setBrightness(fraction: Float) {
        val clamped = fraction.coerceIn(0.05f, 1f)
        _brightnessFraction.value = clamped
        updateSettings { it.copy(lockedBrightnessValue = clamped) }
        if (hardware.setBrightnessFraction(clamped)) {
            _autoBrightness.value = false
        }
    }

    fun toggleAutoBrightness() {
        hardware.audioEnhancer.pulseHaptic(false)
        val result = hardware.toggleAutoBrightness()
        if (result == null) {
            showStatus("Grant 'Modifying system settings' in Permissions to toggle AutoLight")
        } else {
            _autoBrightness.value = result
            if (result) updateSettings { it.copy(lockBrightness = false) }
            showStatus(if (result) "Auto-Brightness Enabled" else "Auto-Brightness Manual")
        }
    }

    fun toggleAutoRotate() {
        hardware.audioEnhancer.pulseHaptic(false)
        val result = hardware.toggleAutoRotate()
        if (result == null) {
            showStatus("Grant 'Modifying system settings' in Permissions to toggle Auto-Rotate")
        } else {
            _autoRotate.value = result
            updateSettings { it.copy(rotationLock = !result) }
            showStatus(if (result) "Auto-Rotate Enabled" else "Rotation Locked")
        }
    }

    fun setVolume(fraction: Float) {
        val clamped = fraction.coerceIn(0f, 1f)
        _volumeFraction.value = clamped
        hardware.setVolumeFraction(clamped)
    }

    fun toggleDoNotDisturb() {
        hardware.audioEnhancer.pulseHaptic(false)
        val next = !settings.value.dndMode
        if (hardware.setDoNotDisturb(next)) {
            updateSettings { it.copy(dndMode = next) }
            showStatus(if (next) "Do Not Disturb Activated" else "Do Not Disturb Off")
        } else {
            showStatus("Grant 'Do Not Disturb' permission in Permissions first")
        }
    }

    fun toggleNoNotifications() {
        hardware.audioEnhancer.pulseHaptic(false)
        val next = !settings.value.notificationMuteMode
        hardware.audioEnhancer.setNotificationMuted(next)
        updateSettings { it.copy(notificationMuteMode = next) }
        showStatus(if (next) "Notification Sound Muted" else "Notification Sound Restored")
    }

    fun openInAppOverlay() {
        hardware.audioEnhancer.pulseHaptic(false)
        _isInAppOverlayOpen.value = true
    }

    fun closeInAppOverlay() {
        _isInAppOverlayOpen.value = false
    }

    fun startFloatingOverlayService() {
        if (!networkMonitor.refreshNow()) {
            showStatus("Internet connection required to use Turbo X")
            return
        }
        val ctx = getApplication<Application>()
        if (!hardware.checkPermissions().overlayGranted) {
            showStatus("Grant 'Allow floating windows' permission for system overlay")
            _isInAppOverlayOpen.value = true
            return
        }
        val intent = Intent(ctx, GameOverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.startForegroundService(intent)
        } else {
            ctx.startService(intent)
        }
        _isOverlayServiceRunning.value = true
        showStatus("Floating Game Turbo handle activated")
    }

    fun stopFloatingOverlayService() {
        val ctx = getApplication<Application>()
        ctx.stopService(Intent(ctx, GameOverlayService::class.java))
        _isOverlayServiceRunning.value = false
    }

    fun launchSelectedGame() {
        if (!networkMonitor.refreshNow()) {
            hardware.audioEnhancer.pulseHaptic(false)
            showStatus("Internet connection required to launch Turbo X")
            return
        }
        val audit = refreshSecurityAudit()
        if (!audit.isSafeForGameplay) {
            hardware.audioEnhancer.pulseHaptic(false)
            showStatus("Security lock: ${audit.reasons.firstOrNull() ?: "APK integrity violation"}")
            return
        }
        val s = settings.value
        val pkg = s.selectedGamePackage ?: addedGames.value.firstOrNull()?.packageName
        if (pkg == null) {
            showStatus("Tap '+' to add a game to your Launcher first")
            return
        }
        val gameName = addedGames.value.find { it.packageName == pkg }?.appName ?: pkg
        viewModelScope.launch {
            val remaining = repo.getRemainingTurboSecondsChecked()
            _remainingTurboSeconds.value = remaining
            if (repo.lastVaultTamperDetected) {
                refreshSecurityAudit()
                showStatus("Credit vault tamper detected — Turbo Time reset")
                openWatchAdPrompt(targetGameName = gameName, launchAfterReward = true)
                return@launch
            }
            if (remaining <= 0L) {
                hardware.audioEnhancer.pulseHaptic(false)
                showStatus("No Turbo Time left! Watch 1 Ad for +1h:00m")
                openWatchAdPrompt(targetGameName = gameName, launchAfterReward = true)
                return@launch
            }
            launchGamePackage(pkg, s)
        }
    }

    private suspend fun launchGamePackage(pkg: String, s: GameTurboSettings) {
        val gameName = addedGames.value.find { it.packageName == pkg }?.appName ?: pkg
        hardware.audioEnhancer.pulseHaptic(true)
        if (s.openingAnimation) {
            _launchingGameName.value = gameName
            delay(950L)
            _launchingGameName.value = null
        }
        if (s.autoDndOnEnter || s.dndMode) {
            hardware.setDoNotDisturb(true)
        }
        if (s.muteNotificationsInGame || s.notificationMuteMode) {
            hardware.audioEnhancer.setNotificationMuted(true)
        }
        if (s.lockBrightness) {
            hardware.setAutoBrightness(false)
            hardware.setBrightnessFraction(_brightnessFraction.value)
        }
        if (s.rotationLock) {
            hardware.setAutoRotate(false)
        }
        if (s.effectSoundEnabled && s.eqPreset != EqPreset.OFF) {
            hardware.audioEnhancer.applyEqPreset(true, s.eqPreset)
        }
        val t = _telemetry.value
        hardware.performanceEngine.applyProfile(
            settings = s,
            supportedRates = t.supportedRefreshRates,
            isInGameLaunch = true,
            batteryPercent = t.batteryPercent,
            isCharging = t.isCharging
        )
        if (s.disablePowerSavingInGame && hardware.performanceEngine.isBatterySaverActive()) {
            showStatus("Battery Saver detected — disable Power Saving for max FPS")
        }
        if (s.preventMemoryLeaks || s.smartBackgroundControl || s.increaseCpuPerformance || s.increaseGpuPerformance || s.highPerfFpsControl) {
            val allPkgs = _installedApps.value.map { it.packageName }
            hardware.performRealMemoryBoost(
                protectedPackages = s.addedGamePackages + pkg,
                allKnownPackages = allPkgs,
                backgroundLimitIndex = s.backgroundLimitIndex,
                cacheLimitIndex = s.cacheLimitIndex
            )
        }
        if (hardware.checkPermissions().overlayGranted) {
            startFloatingOverlayService()
        }
        _sessionHistory.value = listOf(
            GameSessionRecord(
                packageName = pkg,
                appName = gameName,
                timestampMillis = System.currentTimeMillis(),
                cpuMode = s.cpuMode
            )
        ) + _sessionHistory.value.take(19)
        updateSettings { it.copy(totalUsageMinutes = it.totalUsageMinutes + 1) }

        adMobManager?.suppressAppOpenTemporarily(180_000L)
        val launched = hardware.launchPackage(pkg)
        if (!launched) {
            showStatus("Could not launch $gameName — opening Game Panel")
            _isInAppOverlayOpen.value = true
        }
    }

    private var hasPlayedXSpaceIntroInSession: Boolean = false

    fun shouldPlayXSpaceIntro(): Boolean {
        return settings.value.xSpaceIntroEnabled && !hasPlayedXSpaceIntroInSession
    }

    fun markXSpaceIntroPlayed() {
        hasPlayedXSpaceIntroInSession = true
    }

    fun setXSpaceIntroEnabled(enabled: Boolean) {
        if (enabled) {
            hasPlayedXSpaceIntroInSession = false
        }
        updateSettings { it.copy(xSpaceIntroEnabled = enabled) }
    }

    fun setXSpaceIntroSoundEnabled(enabled: Boolean) {
        updateSettings { it.copy(xSpaceIntroSoundEnabled = enabled) }
    }

    fun showStatus(message: String) {
        _statusMessage.value = message
    }

    fun clearStatus() {
        _statusMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        PerformanceSessionService.onExternalStopRequested = null
        extremeManager.resetSessionWithoutPersistedChanges()
        networkMonitor.unregister()
        hardware.performanceEngine.closeSession()
        hardware.audioEnhancer.releaseEffects()
    }
}
