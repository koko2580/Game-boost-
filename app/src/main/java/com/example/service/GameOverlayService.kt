package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Choreographer
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ElectricCyan
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.R
import com.example.data.ControlPanelLayout
import com.example.data.CpuMode
import com.example.data.CrosshairStyle
import com.example.data.EqPreset
import com.example.data.GameTurboPreferencesRepository
import com.example.data.GameTurboSettings
import com.example.data.HardwareTelemetry
import com.example.data.ScreenColorMode
import com.example.data.SystemHardwareController
import com.example.network.NetworkGateMonitor
import com.example.ui.components.CrosshairOverlay
import com.example.ui.components.GamePanelOverlayContent
import com.example.ui.components.OverlayCallbacks
import com.example.ui.theme.CrimsonTurbo
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TurboGameXTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

class GameOverlayService : Service() {

    companion object {
        @Volatile
        var isRunning: Boolean = false
            private set

        const val EXTRA_START_ROUTE = "extra_start_route"
        private const val CHANNEL_ID = "turbo_game_x_overlay"
        private const val NOTIFICATION_ID = 7001
        private const val ACTION_STOP = "com.example.action.STOP_OVERLAY"
    }

    private lateinit var windowManager: WindowManager
    private lateinit var repo: GameTurboPreferencesRepository
    private lateinit var hardware: SystemHardwareController
    private var networkMonitor: NetworkGateMonitor? = null
    private var controlView: ComposeView? = null
    private var hudPassThroughView: ComposeView? = null
    private var lifecycleOwner: ServiceLifecycleOwner? = null
    private var controlLayoutParams: WindowManager.LayoutParams? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var telemetryJob: Job? = null

    private var isExpanded by mutableStateOf(false)
    private var isRightMenuCollapsed by mutableStateOf(false)
    private val telemetryFlow = MutableStateFlow(HardwareTelemetry())
    private val statusMsgFlow = MutableStateFlow<String?>(null)
    private val brightnessFlow = MutableStateFlow(0.5f)
    private val volumeFlow = MutableStateFlow(0.5f)
    private val stopwatchSecFlow = MutableStateFlow(0L)
    private val stopwatchRunningFlow = MutableStateFlow(false)

    private var lastFrameNs = 0L
    private var windowStartNs = 0L
    private var frameCount = 0
    @Volatile
    private var liveFps = 60
    @Volatile
    private var liveFrameMs = 16.6f
    private var knownPackages: List<String> = emptyList()
    private var appLabelsByPackage: Map<String, String> = emptyMap()
    private var lastAutoDetectedGamePkg: String? = null
    @Volatile
    private var latestSettings: GameTurboSettings = GameTurboSettings()
    private var hudPassThroughParams: WindowManager.LayoutParams? = null

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!isRunning) return
            if (lastFrameNs > 0L) {
                val deltaNs = (frameTimeNanos - lastFrameNs).coerceAtLeast(1_000_000L)
                liveFrameMs = deltaNs / 1_000_000f
                hardware.performanceEngine.reportFrameDurationNanos(deltaNs, latestSettings)
            }
            lastFrameNs = frameTimeNanos
            if (windowStartNs == 0L) windowStartNs = frameTimeNanos
            frameCount++
            val elapsed = frameTimeNanos - windowStartNs
            if (elapsed >= 500_000_000L) {
                liveFps = ((frameCount * 1_000_000_000.0) / elapsed).toInt().coerceIn(1, 144)
                frameCount = 0
                windowStartNs = frameTimeNanos
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        repo = GameTurboPreferencesRepository(applicationContext)
        hardware = SystemHardwareController(applicationContext)
        networkMonitor = NetworkGateMonitor(applicationContext)

        createNotificationChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        if (networkMonitor?.refreshNow() != true) {
            stopSelf()
            return
        }

        attachOverlayWindows()
        Choreographer.getInstance().postFrameCallback(frameCallback)
        startTelemetryUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP || networkMonitor?.refreshNow() != true) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun startTelemetryUpdates() {
        telemetryJob?.cancel()
        telemetryJob = serviceScope.launch {
            val apps = hardware.loadLaunchableApps()
            knownPackages = apps.map { it.packageName }
            appLabelsByPackage = apps.associate { it.packageName to it.appName }
            val (_, supportedRates) = hardware.readDisplayRefreshInfo()
            launch {
                networkMonitor?.isConnected?.collect { online ->
                    if (!online) stopSelf()
                }
            }
            launch {
                repo.settingsFlow.collect { s ->
                    latestSettings = s
                    val currentTelemetry = telemetryFlow.value
                    hardware.performanceEngine.applyProfile(
                        settings = s,
                        supportedRates = supportedRates,
                        isInGameLaunch = true,
                        batteryPercent = currentTelemetry.batteryPercent,
                        isCharging = currentTelemetry.isCharging
                    )
                    hudPassThroughParams?.let { hp ->
                        hardware.performanceEngine.applyToOverlayWindowParams(hp, s, supportedRates, hudPassThroughView)
                        hudPassThroughView?.let { hv -> runCatching { windowManager.updateViewLayout(hv, hp) } }
                    }
                    controlLayoutParams?.let { cp ->
                        hardware.performanceEngine.applyToOverlayWindowParams(cp, s, supportedRates, controlView)
                        controlView?.let { cv -> runCatching { windowManager.updateViewLayout(cv, cp) } }
                    }
                }
            }
            while (true) {
                val settings = latestSettings
                val prevPing = telemetryFlow.value.pingMs
                val prevJitter = telemetryFlow.value.pingJitterMs
                val fps = liveFps
                val frameMs = liveFrameMs
                val snapshot = withContext(Dispatchers.Default) {
                    hardware.readHardwareTelemetry(
                        cpuMode = settings.cpuMode,
                        lastPingMs = prevPing,
                        lastJitterMs = prevJitter,
                        actualFps = fps,
                        frameTimeMs = frameMs
                    )
                }
                telemetryFlow.value = snapshot
                brightnessFlow.value = hardware.getBrightnessFraction()
                volumeFlow.value = hardware.getVolumeFraction()
                if (stopwatchRunningFlow.value) {
                    stopwatchSecFlow.value += 1L
                }

                if (settings.autoPerformanceOptimization) {
                    val decision = hardware.performanceEngine.evaluateAutoPerformance(
                        batteryPercent = snapshot.batteryPercent,
                        isCharging = snapshot.isCharging
                    )
                    if (settings.cpuMode != decision.recommendedCpuMode ||
                        settings.sustainedPerformanceMode != decision.recommendSustained
                    ) {
                        repo.updateSettings { cur ->
                            if (!cur.autoPerformanceOptimization) cur
                            else cur.copy(
                                cpuMode = decision.recommendedCpuMode,
                                sustainedPerformanceMode = decision.recommendSustained
                            )
                        }
                    }
                }

                val fgPkg = hardware.getForegroundPackageName()
                if (fgPkg != null && fgPkg !in settings.addedGamePackages && fgPkg != packageName) {
                    lastAutoDetectedGamePkg = null
                }
                val triggeredGame = SystemHardwareController.evaluateForegroundGameTrigger(
                    foregroundPkg = fgPkg,
                    lastDetectedPkg = lastAutoDetectedGamePkg,
                    settings = settings,
                    ownPackage = packageName
                )
                if (triggeredGame != null) {
                    lastAutoDetectedGamePkg = triggeredGame
                    if (settings.autoDndOnEnter || settings.dndMode) {
                        hardware.setDoNotDisturb(true)
                    }
                    if (settings.muteNotificationsInGame || settings.notificationMuteMode) {
                        hardware.audioEnhancer.setNotificationMuted(true)
                    }
                    hardware.performanceEngine.applyProfile(
                        settings = settings,
                        supportedRates = supportedRates,
                        isInGameLaunch = true,
                        batteryPercent = snapshot.batteryPercent,
                        isCharging = snapshot.isCharging
                    )
                    if (settings.preventMemoryLeaks || settings.smartBackgroundControl) {
                        hardware.performRealMemoryBoost(
                            protectedPackages = settings.addedGamePackages + triggeredGame,
                            allKnownPackages = knownPackages,
                            backgroundLimitIndex = settings.backgroundLimitIndex,
                            cacheLimitIndex = settings.cacheLimitIndex
                        )
                    }
                    val gameLabel = appLabelsByPackage[triggeredGame] ?: triggeredGame
                    showTransientStatus("Auto-detected $gameLabel: Boost Active")
                }

                delay(1200L)
            }
        }
    }

    private fun attachOverlayWindows() {
        val owner = ServiceLifecycleOwner()
        owner.performRestore()
        owner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        owner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        lifecycleOwner = owner

        // 1. Non-touchable persistent HUD window (Crosshair, ScreenColor tint, Active Info, Stopwatch)
        val hudView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                val settings by repo.settingsFlow.collectAsState(initial = GameTurboSettings())
                val telemetry by telemetryFlow.collectAsState()
                val stopwatchSec by stopwatchSecFlow.collectAsState()
                val stopwatchRunning by stopwatchRunningFlow.collectAsState()

                TurboGameXTheme(wallpaperTheme = settings.wallpaperTheme) {
                    PersistentHudPassThroughLayer(
                        isExpanded = isExpanded,
                        settings = settings,
                        telemetry = telemetry,
                        stopwatchSeconds = stopwatchSec,
                        isStopwatchRunning = stopwatchRunning
                    )
                }
            }
        }
        hudPassThroughView = hudView
        val hudParams = createHudPassThroughParams()
        hudPassThroughParams = hudParams
        windowManager.addView(hudView, hudParams)

        // 2. Interactive Handle / Expandable Dual-Wing Control Panel
        val ctrlView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                val settings by repo.settingsFlow.collectAsState(initial = GameTurboSettings())
                val telemetry by telemetryFlow.collectAsState()
                val brightness by brightnessFlow.collectAsState()
                val volume by volumeFlow.collectAsState()
                val stopwatchSec by stopwatchSecFlow.collectAsState()
                val stopwatchRunning by stopwatchRunningFlow.collectAsState()
                val statusMsg by statusMsgFlow.collectAsState()

                TurboGameXTheme(wallpaperTheme = settings.wallpaperTheme) {
                    if (isExpanded) {
                        GamePanelOverlayContent(
                            settings = settings,
                            telemetry = telemetry,
                            brightnessFraction = brightness,
                            volumeFraction = volume,
                            autoBrightness = hardware.isAutoBrightnessEnabled(),
                            autoRotate = hardware.isAutoRotateEnabled(),
                            stopwatchSeconds = stopwatchSec,
                            isStopwatchRunning = stopwatchRunning,
                            statusMessage = statusMsg,
                            callbacks = createCallbacks(settings)
                        )
                    } else {
                        FloatingTriggerHandle(
                            layout = settings.controlPanelLayout,
                            isCollapsed = isRightMenuCollapsed,
                            onExpand = { toggleExpanded(true, settings.controlPanelLayout) },
                            onToggleCollapse = { isRightMenuCollapsed = !isRightMenuCollapsed },
                            onOpenSettings = { openMainActivity("settings") },
                            onClose = { stopSelf() },
                            onDrag = { dx, dy -> dragHandleBy(dx, dy) }
                        )
                    }
                }
            }
        }
        controlView = ctrlView
        val params = createControlParams(expanded = false, layout = latestSettings.controlPanelLayout)
        val (_, supportedRates) = hardware.readDisplayRefreshInfo()
        hardware.performanceEngine.applyToOverlayWindowParams(params, latestSettings, supportedRates, ctrlView)
        controlLayoutParams = params
        windowManager.addView(ctrlView, params)
    }

    private fun dragHandleBy(dx: Float, dy: Float) {
        val params = controlLayoutParams ?: return
        val view = controlView ?: return
        if (isExpanded) return
        val mult = hardware.performanceEngine.computeScrollMultiplier(latestSettings)
        params.x = (params.x + (dx * mult).roundToInt())
        params.y = (params.y + (dy * mult).roundToInt())
        runCatching { windowManager.updateViewLayout(view, params) }
    }

    private fun toggleExpanded(expand: Boolean, layout: ControlPanelLayout) {
        isExpanded = expand
        val view = controlView ?: return
        val params = createControlParams(expand, layout)
        val (_, supportedRates) = hardware.readDisplayRefreshInfo()
        hardware.performanceEngine.applyToOverlayWindowParams(params, latestSettings, supportedRates, view)
        controlLayoutParams = params
        runCatching { windowManager.updateViewLayout(view, params) }
    }

    private fun overlayWindowType(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
        @Suppress("DEPRECATION")
        WindowManager.LayoutParams.TYPE_PHONE
    }

    private fun createHudPassThroughParams(): WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        overlayWindowType(),
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    private fun createControlParams(expanded: Boolean, layout: ControlPanelLayout): WindowManager.LayoutParams {
        return if (expanded) {
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                overlayWindowType(),
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
        } else {
            val gravity = when (layout) {
                ControlPanelLayout.LEFT_EDGE_SWIPE,
                ControlPanelLayout.LEFT_EDGE_MINIMAL -> Gravity.START or Gravity.CENTER_VERTICAL
                ControlPanelLayout.RIGHT_EDGE_MENU -> Gravity.END or Gravity.CENTER_VERTICAL
                ControlPanelLayout.TOP_G_HANDLE -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
            }
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayWindowType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                this.gravity = gravity
            }
        }
    }

    private fun showTransientStatus(msg: String) {
        serviceScope.launch {
            statusMsgFlow.value = msg
            delay(2200L)
            if (statusMsgFlow.value == msg) statusMsgFlow.value = null
        }
    }

    private fun createCallbacks(settings: GameTurboSettings): OverlayCallbacks = OverlayCallbacks(
        onHomeClick = {
            toggleExpanded(false, settings.controlPanelLayout)
            openMainActivity("launcher")
        },
        onSettingsClick = {
            toggleExpanded(false, settings.controlPanelLayout)
            openMainActivity("settings")
        },
        onGamesClick = {
            toggleExpanded(false, settings.controlPanelLayout)
            openMainActivity("add_apps")
        },
        onPowerExitClick = { stopSelf() },
        onMinimizeClick = { toggleExpanded(false, settings.controlPanelLayout) },
        onSelectCpuMode = { mode ->
            hardware.audioEnhancer.pulseHaptic(false)
            serviceScope.launch { repo.updateSettings { it.copy(cpuMode = mode) } }
            showTransientStatus("CPU: ${mode.label}")
        },
        onCycleFrameRate = {
            hardware.audioEnhancer.pulseHaptic(false)
            val opts = listOf(60, 90, 120, 144)
            val next = opts[(opts.indexOf(settings.frameRateTarget).coerceAtLeast(0) + 1) % opts.size]
            serviceScope.launch { repo.updateSettings { it.copy(frameRateTarget = next) } }
            showTransientStatus("Target: ${next}Hz")
        },
        onBoostRam = {
            hardware.audioEnhancer.pulseHaptic(true)
            serviceScope.launch {
                val res = hardware.performRealMemoryBoost(
                    protectedPackages = settings.addedGamePackages,
                    allKnownPackages = knownPackages,
                    backgroundLimitIndex = settings.backgroundLimitIndex,
                    cacheLimitIndex = settings.cacheLimitIndex
                )
                repo.updateSettings {
                    it.copy(totalBoostCount = it.totalBoostCount + 1, lastFreedMb = res.freedMb)
                }
                showTransientStatus("Freed ${res.freedMb} MB • Trimmed ${res.trimmedPackages} apps")
            }
        },
        onClearCache = {
            hardware.audioEnhancer.pulseHaptic(false)
            serviceScope.launch {
                val freedKb = hardware.clearAppCaches(settings.cacheLimitIndex)
                showTransientStatus("Cleared $freedKb KB cache")
            }
        },
        onPingTest = {
            hardware.audioEnhancer.pulseHaptic(false)
            serviceScope.launch {
                showTransientStatus("Testing ping...")
                val (ping, jitter) = hardware.measurePingWithJitter()
                telemetryFlow.value = telemetryFlow.value.copy(pingMs = ping, pingJitterMs = jitter)
                repo.updateSettings { it.copy(reducePingEnabled = ping != null) }
                showTransientStatus(ping?.let { "Ping: ${it}ms (Jitter ${jitter ?: 0}ms)" } ?: "Offline")
            }
        },
        onToggleStopwatch = {
            hardware.audioEnhancer.pulseHaptic(false)
            stopwatchRunningFlow.value = !stopwatchRunningFlow.value
        },
        onResetStopwatch = {
            stopwatchRunningFlow.value = false
            stopwatchSecFlow.value = 0L
        },
        onToggleActiveInfo = {
            hardware.audioEnhancer.pulseHaptic(false)
            serviceScope.launch { repo.updateSettings { it.copy(activeInfoHud = !it.activeInfoHud) } }
        },
        onToggleTurboX = {
            hardware.audioEnhancer.pulseHaptic(true)
            val next = !settings.turboXMode
            serviceScope.launch {
                repo.updateSettings {
                    it.copy(
                        turboXMode = next,
                        cpuMode = if (next) CpuMode.ACCELERATION else CpuMode.BALANCED,
                        activeInfoHud = if (next) true else it.activeInfoHud
                    )
                }
                if (next) {
                    val res = hardware.performRealMemoryBoost(
                        protectedPackages = settings.addedGamePackages,
                        allKnownPackages = knownPackages,
                        backgroundLimitIndex = settings.backgroundLimitIndex,
                        cacheLimitIndex = settings.cacheLimitIndex
                    )
                    showTransientStatus("Turbo X Engaged • Freed ${res.freedMb} MB")
                } else {
                    showTransientStatus("Turbo X Off")
                }
            }
        },
        onToggleDataPanel = {
            hardware.audioEnhancer.pulseHaptic(false)
            serviceScope.launch { repo.updateSettings { it.copy(dataPanelVisible = !it.dataPanelVisible) } }
        },
        onCycleCrosshair = {
            hardware.audioEnhancer.pulseHaptic(false)
            val entries = CrosshairStyle.entries
            val next = entries[(settings.crosshairStyle.ordinal + 1) % entries.size]
            serviceScope.launch { repo.updateSettings { it.copy(crosshairStyle = next) } }
            showTransientStatus("Crosshair: ${next.label}")
        },
        onCycleScreenColor = {
            hardware.audioEnhancer.pulseHaptic(false)
            val entries = ScreenColorMode.entries
            val next = entries[(settings.screenColorMode.ordinal + 1) % entries.size]
            serviceScope.launch { repo.updateSettings { it.copy(screenColorMode = next) } }
            showTransientStatus("Color: ${next.label}")
        },
        onToggleScreenZoom = {
            hardware.audioEnhancer.pulseHaptic(false)
            serviceScope.launch {
                if (!settings.screenZoomEnabled) {
                    repo.updateSettings { it.copy(screenZoomEnabled = true, zoomScale = 1.5f) }
                    showTransientStatus("Scope Zoom: 1.5x")
                } else if (settings.zoomScale < 2.5f) {
                    val nextScale = if (settings.zoomScale < 1.8f) 2.0f else 3.0f
                    repo.updateSettings { it.copy(zoomScale = nextScale) }
                    showTransientStatus("Scope Zoom: ${nextScale}x")
                } else {
                    repo.updateSettings { it.copy(screenZoomEnabled = false, zoomScale = 2.0f) }
                    showTransientStatus("Scope Zoom Off")
                }
            }
        },
        onToggleBackgroundControl = {
            hardware.audioEnhancer.pulseHaptic(false)
            serviceScope.launch {
                val next = !settings.backgroundControlEnabled
                repo.updateSettings { it.copy(backgroundControlEnabled = next) }
                if (next) {
                    val res = hardware.performRealMemoryBoost(
                        protectedPackages = settings.addedGamePackages,
                        allKnownPackages = knownPackages,
                        backgroundLimitIndex = settings.backgroundLimitIndex,
                        cacheLimitIndex = settings.cacheLimitIndex
                    )
                    showTransientStatus("BG Control: Trimmed ${res.trimmedPackages} apps")
                } else {
                    showTransientStatus("Background Control Paused")
                }
            }
        },
        onToggleEffectSound = {
            hardware.audioEnhancer.pulseHaptic(false)
            val entries = EqPreset.entries
            val nextPreset = entries[(settings.eqPreset.ordinal + 1) % entries.size]
            val enabled = nextPreset != EqPreset.OFF
            hardware.audioEnhancer.applyEqPreset(enabled, nextPreset)
            if (enabled) hardware.audioEnhancer.playTacticalTone()
            serviceScope.launch {
                repo.updateSettings { it.copy(effectSoundEnabled = enabled, eqPreset = nextPreset) }
            }
            showTransientStatus("Audio EQ: ${nextPreset.label}")
        },
        onToggleScreenScaling = {
            hardware.audioEnhancer.pulseHaptic(false)
            serviceScope.launch {
                repo.updateSettings { it.copy(screenScalingWide = !it.screenScalingWide) }
                showTransientStatus(if (!settings.screenScalingWide) "Immersive Scaling On" else "Standard Scaling")
            }
        },
        onToggleKeyMap = {
            hardware.audioEnhancer.pulseHaptic(false)
            serviceScope.launch { repo.updateSettings { it.copy(keyMapTriggerVisible = !it.keyMapTriggerVisible) } }
        },
        onBrightnessChange = { f ->
            brightnessFlow.value = f
            if (!hardware.setBrightnessFraction(f)) {
                showTransientStatus("Grant Write Settings for system brightness")
            }
        },
        onVolumeChange = { f ->
            volumeFlow.value = f
            hardware.setVolumeFraction(f)
        },
        onToggleAutoRotate = {
            if (hardware.toggleAutoRotate() == null) {
                showTransientStatus("Grant Write Settings for Auto-Rotate")
            }
        },
        onToggleAutoBrightness = {
            if (hardware.toggleAutoBrightness() == null) {
                showTransientStatus("Grant Write Settings for AutoLight")
            }
        },
        onToggleNoNotifications = {
            val next = !settings.notificationMuteMode
            hardware.audioEnhancer.setNotificationMuted(next)
            serviceScope.launch { repo.updateSettings { it.copy(notificationMuteMode = next) } }
            showTransientStatus(if (next) "Notifications Muted" else "Notifications Active")
        },
        onToggleDnd = {
            val next = !settings.dndMode
            if (hardware.setDoNotDisturb(next)) {
                serviceScope.launch { repo.updateSettings { it.copy(dndMode = next) } }
                showTransientStatus(if (next) "DND On" else "DND Off")
            } else {
                showTransientStatus("Grant Do Not Disturb permission first")
            }
        },
        onOpenWifi = {
            toggleExpanded(false, settings.controlPanelLayout)
            hardware.openWifiSettingsPanel()
        },
        onScreenshotInfo = {
            toggleExpanded(false, settings.controlPanelLayout)
            startActivity(
                Intent(this, ScreenCaptureActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        },
        onQuickApp = { pkgs, url ->
            toggleExpanded(false, settings.controlPanelLayout)
            hardware.launchQuickShortcut(pkgs, url)
        }
    )

    private fun openMainActivity(route: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(EXTRA_START_ROUTE, route)
        }
        startActivity(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Turbo X Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active floating Game Turbo HUD service"
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, GameOverlayService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Turbo X Active")
            .setContentText("Tap floating handle to open Game Control Panel")
            .setContentIntent(openIntent)
            .addAction(0, "Stop Overlay", stopIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        networkMonitor?.unregister()
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        telemetryJob?.cancel()
        serviceScope.cancel()
        // Restore system states cleanly on exit
        hardware.performanceEngine.closeSession()
        hardware.audioEnhancer.setNotificationMuted(false)
        hardware.audioEnhancer.releaseEffects()
        hardware.setDoNotDisturb(false)
        controlView?.let { runCatching { windowManager.removeView(it) } }
        hudPassThroughView?.let { runCatching { windowManager.removeView(it) } }
        lifecycleOwner?.let {
            it.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            it.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            it.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            it.viewModelStore.clear()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

@Composable
private fun PersistentHudPassThroughLayer(
    isExpanded: Boolean,
    settings: GameTurboSettings,
    telemetry: HardwareTelemetry,
    stopwatchSeconds: Long,
    isStopwatchRunning: Boolean
) {
    // Only render passive pass-through HUD elements when control panel is minimized
    // (when expanded, GamePanelOverlayContent renders its own controls)
    if (isExpanded) return

    val tint = when (settings.screenColorMode) {
        ScreenColorMode.DEFAULT -> Color.Transparent
        ScreenColorMode.VIVID -> Color(0x16FF2438)
        ScreenColorMode.NIGHT_VISION -> Color(0x1C00E5FF)
        ScreenColorMode.WARM_EYE -> Color(0x20FFB300)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tint)
    ) {
        CrosshairOverlay(style = settings.crosshairStyle)

        if (settings.screenZoomEnabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(118.dp)
                    .graphicsLayer(
                        scaleX = 1f + (settings.zoomScale - 1f) * 0.14f,
                        scaleY = 1f + (settings.zoomScale - 1f) * 0.14f
                    )
                    .border(2.dp, ElectricCyan.copy(alpha = 0.85f), CircleShape)
                    .background(ElectricCyan.copy(alpha = 0.10f), CircleShape),
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    text = "${settings.zoomScale}x SCOPE",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        if (settings.activeInfoHud || settings.dataPanelVisible) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ObsidianVoid.copy(alpha = 0.78f))
                    .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${telemetry.actualFps} FPS • ${telemetry.cpuFreqMHz} MHz",
                    style = MaterialTheme.typography.labelLarge,
                    color = EmeraldBright
                )
                Text(
                    text = "RAM ${telemetry.ramUsedPercent}% • ${telemetry.batteryTempCelsius}°C" +
                        (telemetry.pingMs?.let { " • ${it}ms" } ?: ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }

        if (isStopwatchRunning || stopwatchSeconds > 0L) {
            val mins = stopwatchSeconds / 60
            val secs = stopwatchSeconds % 60
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp, top = 42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ObsidianVoid.copy(alpha = 0.8f))
                    .border(1.dp, CyberAmber, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = String.format(Locale.US, "%02d:%02d", mins, secs),
                    style = MaterialTheme.typography.labelLarge,
                    color = CyberAmber
                )
            }
        }
    }
}

@Composable
private fun FloatingTriggerHandle(
    layout: ControlPanelLayout,
    isCollapsed: Boolean,
    onExpand: () -> Unit,
    onToggleCollapse: () -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
    onDrag: (Float, Float) -> Unit
) {
    val dragModifier = Modifier.pointerInput(Unit) {
        detectDragGestures { change, dragAmount ->
            change.consume()
            onDrag(dragAmount.x, dragAmount.y)
        }
    }

    when (layout) {
        ControlPanelLayout.LEFT_EDGE_SWIPE,
        ControlPanelLayout.LEFT_EDGE_MINIMAL -> {
            Box(
                modifier = dragModifier
                    .padding(start = 2.dp)
                    .width(10.dp)
                    .height(96.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.White.copy(alpha = 0.88f))
                    .border(1.5.dp, CrimsonTurbo, RoundedCornerShape(5.dp))
                    .clickable { onExpand() }
            )
        }
        ControlPanelLayout.RIGHT_EDGE_MENU -> {
            if (isCollapsed) {
                Box(
                    modifier = dragModifier
                        .padding(end = 2.dp)
                        .size(28.dp, 54.dp)
                        .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                        .background(ObsidianVoid.copy(alpha = 0.92f))
                        .border(1.dp, CrimsonTurbo, RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                        .clickable { onToggleCollapse() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("G", color = CrimsonTurbo, fontWeight = FontWeight.Bold)
                }
            } else {
                Column(
                    modifier = Modifier
                        .padding(end = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianVoid.copy(alpha = 0.94f))
                        .border(1.dp, CrimsonTurbo, RoundedCornerShape(8.dp))
                        .padding(5.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Rounded.ExpandMore, "Hide", tint = Color.White, modifier = Modifier.size(20.dp).clickable { onToggleCollapse() })
                    Icon(Icons.Rounded.Fullscreen, "Open Game Mode", tint = Color.White, modifier = Modifier.size(20.dp).clickable { onExpand() })
                    Icon(Icons.Rounded.Settings, "Settings", tint = Color.White, modifier = Modifier.size(20.dp).clickable { onOpenSettings() })
                    Icon(Icons.Rounded.Close, "Close", tint = CrimsonTurbo, modifier = Modifier.size(20.dp).clickable { onClose() })
                    Icon(Icons.Rounded.SwapVert, "Drag", tint = TextSecondary, modifier = dragModifier.size(20.dp))
                }
            }
        }
        ControlPanelLayout.TOP_G_HANDLE -> {
            Box(
                modifier = dragModifier
                    .padding(top = 4.dp)
                    .size(42.dp, 30.dp)
                    .clip(RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
                    .background(CrimsonTurbo)
                    .clickable { onExpand() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "G",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
            }
        }
    }
}

private class ServiceLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    fun performRestore() {
        savedStateController.performRestore(null)
    }

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        lifecycleRegistry.handleLifecycleEvent(event)
    }
}
