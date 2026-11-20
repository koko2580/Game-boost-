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
import com.example.data.PerformanceEngineStatus
import com.example.data.PermissionStatus
import com.example.data.ScreenColorMode
import com.example.data.SystemHardwareController
import com.example.data.WallpaperTheme
import com.example.data.ScreenshotHelper
import com.example.network.NetworkGateMonitor
import com.example.security.SecurityAuditReport
import com.example.service.GameOverlayService
import com.example.service.ScreenCaptureActivity
import kotlinx.coroutines.Dispatchers
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

    private val networkMonitor = NetworkGateMonitor(application)
    val isNetworkConnected: StateFlow<Boolean> = networkMonitor.isConnected

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
        refreshInstalledApps()
        startTelemetryLoop()
        startStopwatchLoop()
        startTurboTimeLoop()
        observeNetworkGate()
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
                    adMobManager?.loadRewardedAd()
                    adMobManager?.loadInterstitialAd()
                }
                wasConnected = connected
            }
        }
    }

    fun refreshNetworkConnection(): Boolean {
        val online = networkMonitor.refreshNow()
        if (!online) {
            hardware.audioEnhancer.pulseHaptic(false)
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
        hardware.performanceEngine.applyProfile(
            settings = settings.value,
            supportedRates = _telemetry.value.supportedRefreshRates,
            activity = activity,
            isInGameLaunch = false
        )
    }

    fun refreshPermissionsAndHardware() {
        _permissions.value = hardware.checkPermissions()
        _brightnessFraction.value = hardware.getBrightnessFraction()
        _volumeFraction.value = hardware.getVolumeFraction()
        _autoBrightness.value = hardware.isAutoBrightnessEnabled()
        _autoRotate.value = hardware.isAutoRotateEnabled()
        _isOverlayServiceRunning.value = GameOverlayService.isRunning
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
                delay(1200L)
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
            hardware.performanceEngine.applyProfile(
                settings = next,
                supportedRates = _telemetry.value.supportedRefreshRates
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
                val res = hardware.performRealMemoryBoost(
                    protectedPackages = settings.value.addedGamePackages,
                    allKnownPackages = _installedApps.value.map { it.packageName }
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

    fun toggleGameAdded(packageName: String) {
        hardware.audioEnhancer.pulseHaptic(false)
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
            current.copy(addedGamePackages = nextSet, selectedGamePackage = nextSelected)
        }
    }

    fun selectGame(packageName: String) {
        hardware.audioEnhancer.pulseHaptic(false)
        updateSettings { it.copy(selectedGamePackage = packageName) }
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

    fun performBoost() {
        hardware.audioEnhancer.pulseHaptic(true)
        viewModelScope.launch {
            val allPackages = _installedApps.value.map { it.packageName }
            val result = hardware.performRealMemoryBoost(
                protectedPackages = settings.value.addedGamePackages,
                allKnownPackages = allPackages
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
            val freedKb = hardware.clearAppCaches()
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
            ctx.startActivity(
                Intent(ctx, ScreenCaptureActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
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
        hardware.performanceEngine.applyProfile(
            settings = s,
            supportedRates = _telemetry.value.supportedRefreshRates,
            isInGameLaunch = true
        )
        if (s.preventMemoryLeaks || s.increaseCpuPerformance || s.increaseGpuPerformance || s.highPerfFpsControl) {
            val allPkgs = _installedApps.value.map { it.packageName }
            hardware.performRealMemoryBoost(
                protectedPackages = s.addedGamePackages + pkg,
                allKnownPackages = allPkgs
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

    fun showStatus(message: String) {
        _statusMessage.value = message
    }

    fun clearStatus() {
        _statusMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        networkMonitor.unregister()
        hardware.performanceEngine.closeSession()
        hardware.audioEnhancer.releaseEffects()
    }
}
