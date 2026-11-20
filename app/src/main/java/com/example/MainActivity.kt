package com.example

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.Choreographer
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.ScreenColorMode
import com.example.service.GameOverlayService
import com.example.ui.components.CrosshairOverlay
import com.example.ui.components.GamePanelOverlayContent
import com.example.ui.components.LocalTurboTouchConfig
import com.example.ui.components.OverlayCallbacks
import com.example.ui.components.RewardedAdDialog
import com.example.ui.components.TurboCockpitReactor
import com.example.ui.components.TurboTouchConfig
import com.example.ui.components.cpuModeColor
import com.example.ui.screens.AddAppsScreen
import com.example.ui.screens.LauncherHomeScreen
import com.example.ui.screens.PerformanceActions
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.LocalTurboAccent
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TurboGameXTheme
import com.example.viewmodel.GameTurboViewModel
import kotlin.math.roundToInt

private const val ROUTE_LAUNCHER = "launcher"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_ADD_APPS = "add_apps"
private const val ROUTE_PERMISSIONS = "permissions"

class MainActivity : ComponentActivity() {

    private var pendingRoute: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingRoute = intent?.getStringExtra(GameOverlayService.EXTRA_START_ROUTE)
        window.decorView.filterTouchesWhenObscured = true
        enableEdgeToEdge()
        setContent {
            val viewModel: GameTurboViewModel = viewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
            val permissions by viewModel.permissions.collectAsStateWithLifecycle()
            val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
            val appsLoading by viewModel.appsLoading.collectAsStateWithLifecycle()
            val addedGames by viewModel.addedGames.collectAsStateWithLifecycle()
            val sessionHistory by viewModel.sessionHistory.collectAsStateWithLifecycle()
            val brightness by viewModel.brightnessFraction.collectAsStateWithLifecycle()
            val volume by viewModel.volumeFraction.collectAsStateWithLifecycle()
            val autoBrightness by viewModel.autoBrightness.collectAsStateWithLifecycle()
            val autoRotate by viewModel.autoRotate.collectAsStateWithLifecycle()
            val stopwatchRunning by viewModel.isStopwatchRunning.collectAsStateWithLifecycle()
            val stopwatchSeconds by viewModel.stopwatchSeconds.collectAsStateWithLifecycle()
            val isOverlayOpen by viewModel.isInAppOverlayOpen.collectAsStateWithLifecycle()
            val launchingGameName by viewModel.launchingGameName.collectAsStateWithLifecycle()
            val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
            val isOverlayServiceRunning by viewModel.isOverlayServiceRunning.collectAsStateWithLifecycle()
            val remainingTurboSeconds by viewModel.remainingTurboSeconds.collectAsStateWithLifecycle()
            val adStep by viewModel.adStep.collectAsStateWithLifecycle()
            val rewardedAdReady by viewModel.rewardedAdReady.collectAsStateWithLifecycle()
            val rewardedAdLoading by viewModel.rewardedAdLoading.collectAsStateWithLifecycle()
            val rewardedAdError by viewModel.rewardedAdError.collectAsStateWithLifecycle()
            val rewardedCooldownSeconds by viewModel.rewardedCooldownSeconds.collectAsStateWithLifecycle()
            val performanceStatus by viewModel.performanceStatus.collectAsStateWithLifecycle()
            val securityAudit by viewModel.securityAudit.collectAsStateWithLifecycle()

            // Real Choreographer FPS measurement + ADPF frame duration reporting
            DisposableEffect(Unit) {
                val callback = object : Choreographer.FrameCallback {
                    override fun doFrame(frameTimeNanos: Long) {
                        viewModel.onChoreographerFrame(frameTimeNanos)
                        Choreographer.getInstance().postFrameCallback(this)
                    }
                }
                Choreographer.getInstance().postFrameCallback(callback)
                onDispose {
                    Choreographer.getInstance().removeFrameCallback(callback)
                }
            }

            // Apply real Window refresh rate, ADPF hint, unbuffered touch, GPU pipeline, orientation, brightness & immersive scaling
            LaunchedEffect(
                settings.frameRateTarget,
                settings.fpsGameTarget,
                settings.highPerfFpsControl,
                settings.cpuMode,
                settings.increaseCpuPerformance,
                settings.increaseGpuPerformance,
                settings.forceGpuOpenGl,
                settings.enableSkiaVulkan,
                settings.enhanceTouchSensitivity,
                settings.reduceTouchLatency,
                settings.scrollingSensitivity,
                settings.slidingFrictionIndex,
                telemetry.supportedRefreshRates
            ) {
                viewModel.syncWindowPerformance(this@MainActivity)
            }
            LaunchedEffect(settings.rotationLock) {
                requestedOrientation = if (settings.rotationLock) {
                    ActivityInfo.SCREEN_ORIENTATION_LOCKED
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }
            LaunchedEffect(settings.lockBrightness, brightness) {
                val lp = window.attributes
                lp.screenBrightness = if (settings.lockBrightness) brightness else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = lp
            }
            LaunchedEffect(settings.screenScalingWide) {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                if (settings.screenScalingWide) {
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                } else {
                    controller.show(WindowInsetsCompat.Type.systemBars())
                }
            }

            val touchConfig = remember(performanceStatus) {
                TurboTouchConfig(
                    scrollMultiplier = performanceStatus.scrollMultiplier,
                    frictionFactor = performanceStatus.frictionFactor,
                    unbufferedTouch = performanceStatus.unbufferedTouchActive
                )
            }

            CompositionLocalProvider(LocalTurboTouchConfig provides touchConfig) {
                TurboGameXTheme(wallpaperTheme = settings.wallpaperTheme) {
                    val navController = rememberNavController()

                    LaunchedEffect(pendingRoute) {
                        val target = pendingRoute
                        if (target != null && target != ROUTE_LAUNCHER) {
                            navController.navigate(target) { launchSingleTop = true }
                            pendingRoute = null
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_LAUNCHER
                    ) {
                        composable(ROUTE_LAUNCHER) {
                            LauncherHomeScreen(
                                settings = settings,
                                telemetry = telemetry,
                                permissions = permissions,
                                addedGames = addedGames,
                                sessionHistory = sessionHistory,
                                remainingTurboSeconds = remainingTurboSeconds,
                                statusMessage = statusMessage,
                                isOverlayServiceRunning = isOverlayServiceRunning,
                                getAppIcon = viewModel::getAppIcon,
                                onSelectGame = viewModel::selectGame,
                                onStartPlay = viewModel::launchSelectedGame,
                                onAddTurboTimeClick = { viewModel.openWatchAdPrompt() },
                                onOpenOverlayPreview = viewModel::openInAppOverlay,
                                onToggleOverlayService = {
                                    if (isOverlayServiceRunning) {
                                        viewModel.stopFloatingOverlayService()
                                    } else {
                                        viewModel.startFloatingOverlayService()
                                    }
                                },
                                onNavigateAddApps = { navController.navigate(ROUTE_ADD_APPS) },
                                onNavigateSettings = { navController.navigate(ROUTE_SETTINGS) },
                                onNavigatePermissions = { navController.navigate(ROUTE_PERMISSIONS) },
                                onBoostRam = viewModel::performBoost,
                                onRunPingTest = viewModel::runPingTest,
                                onSelectWallpaperTheme = viewModel::setWallpaperTheme,
                                onDismissStatus = viewModel::clearStatus
                            )
                        }

                        composable(ROUTE_SETTINGS) {
                            val perfActions = remember(viewModel) {
                                PerformanceActions(
                                    onSetFpsGameTarget = viewModel::setFpsGameTarget,
                                    onSetFrameRateTarget = viewModel::setFrameRateTarget,
                                    onSetSlidingFrictionIndex = viewModel::setSlidingFrictionIndex,
                                    onSetScrollingSensitivity = viewModel::setScrollingSensitivity,
                                    onSetEnhanceTouchSensitivity = viewModel::setEnhanceTouchSensitivity,
                                    onSetReduceTouchLatency = viewModel::setReduceTouchLatency,
                                    onSetIncreaseCpuPerformance = viewModel::setIncreaseCpuPerformance,
                                    onSetIncreaseGpuPerformance = viewModel::setIncreaseGpuPerformance,
                                    onSetForceGpuOpenGl = viewModel::setForceGpuOpenGl,
                                    onSetEnableSkiaVulkan = viewModel::setEnableSkiaVulkan,
                                    onSetHighPerfFpsControl = viewModel::setHighPerfFpsControl,
                                    onOpenPermissions = { navController.navigate(ROUTE_PERMISSIONS) },
                                    onOpenDeveloperOptions = viewModel::openDeveloperRenderingOptions
                                )
                            }
                            SettingsScreen(
                                settings = settings,
                                telemetry = telemetry,
                                performanceStatus = performanceStatus,
                                permissions = permissions,
                                securityAudit = securityAudit,
                                statusMessage = statusMessage,
                                performanceActions = perfActions,
                                onUpdateSettings = viewModel::updateSettings,
                                onBoostRam = viewModel::performBoost,
                                onClearCache = viewModel::clearCache,
                                onSelectWallpaperTheme = viewModel::setWallpaperTheme,
                                onOpenPermissions = { navController.navigate(ROUTE_PERMISSIONS) },
                                onDismissStatus = viewModel::clearStatus,
                                onBack = {
                                    viewModel.maybeShowInterstitial(this@MainActivity) {
                                        navController.popBackStack()
                                    }
                                }
                            )
                        }

                        composable(ROUTE_ADD_APPS) {
                            AddAppsScreen(
                                apps = installedApps,
                                addedPackages = settings.addedGamePackages,
                                isLoading = appsLoading,
                                getAppIcon = viewModel::getAppIcon,
                                onToggleApp = viewModel::toggleGameAdded,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ROUTE_PERMISSIONS) {
                            PermissionsScreen(
                                permissions = permissions,
                                onRefreshPermissions = viewModel::refreshPermissionsAndHardware,
                                onRequestDnd = {
                                    viewModel.suppressAppOpenForExternalFlow()
                                    viewModel.hardware.openDndPermissionSettings()
                                },
                                onRequestOverlay = {
                                    viewModel.suppressAppOpenForExternalFlow()
                                    viewModel.hardware.openOverlayPermissionSettings()
                                },
                                onRequestWriteSettings = {
                                    viewModel.suppressAppOpenForExternalFlow()
                                    viewModel.hardware.openWriteSettingsPermission()
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }

                    // Persistent in-app ScreenColor, Crosshair, and Active Info HUD when panel minimized
                    if (!isOverlayOpen) {
                        val tint = when (settings.screenColorMode) {
                            ScreenColorMode.DEFAULT -> Color.Transparent
                            ScreenColorMode.VIVID -> Color(0x16FF2438)
                            ScreenColorMode.NIGHT_VISION -> Color(0x1C00E5FF)
                            ScreenColorMode.WARM_EYE -> Color(0x20FFB300)
                        }
                        if (tint != Color.Transparent) {
                            Box(modifier = Modifier.fillMaxSize().background(tint))
                        }
                        CrosshairOverlay(style = settings.crosshairStyle)
                        if (settings.activeInfoHud) {
                            var hudOffsetX by remember { mutableFloatStateOf(0f) }
                            var hudOffsetY by remember { mutableFloatStateOf(0f) }
                            Column(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .systemBarsPadding()
                                    .padding(end = 16.dp, top = 62.dp)
                                    .offset { IntOffset(hudOffsetX.roundToInt(), hudOffsetY.roundToInt()) }
                                    .pointerInput(Unit) {
                                        detectDragGestures { change, drag ->
                                            change.consume()
                                            hudOffsetX += drag.x
                                            hudOffsetY += drag.y
                                        }
                                    }
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ObsidianVoid.copy(alpha = 0.84f))
                                    .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${telemetry.actualFps} FPS • ${telemetry.cpuFreqMHz} MHz",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = EmeraldBright
                                )
                                Text(
                                    text = "RAM ${telemetry.ramUsedPercent}% • ${telemetry.batteryTempCelsius}°C",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    // Interactive In-App Dual-Wing Game Panel Overlay
                    AnimatedVisibility(
                        visible = isOverlayOpen,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        BackHandler(enabled = isOverlayOpen) {
                            viewModel.closeInAppOverlay()
                        }
                        GamePanelOverlayContent(
                            settings = settings,
                            telemetry = telemetry,
                            brightnessFraction = brightness,
                            volumeFraction = volume,
                            autoBrightness = autoBrightness,
                            autoRotate = autoRotate,
                            stopwatchSeconds = stopwatchSeconds,
                            isStopwatchRunning = stopwatchRunning,
                            statusMessage = statusMessage,
                            callbacks = OverlayCallbacks(
                                onHomeClick = {
                                    viewModel.closeInAppOverlay()
                                    navController.popBackStack(ROUTE_LAUNCHER, inclusive = false)
                                },
                                onSettingsClick = {
                                    viewModel.closeInAppOverlay()
                                    navController.navigate(ROUTE_SETTINGS)
                                },
                                onGamesClick = {
                                    viewModel.closeInAppOverlay()
                                    navController.navigate(ROUTE_ADD_APPS)
                                },
                                onPowerExitClick = {
                                    viewModel.closeInAppOverlay()
                                    viewModel.stopFloatingOverlayService()
                                },
                                onMinimizeClick = viewModel::closeInAppOverlay,
                                onSelectCpuMode = viewModel::setCpuMode,
                                onCycleFrameRate = viewModel::cycleFrameRate,
                                onBoostRam = viewModel::performBoost,
                                onClearCache = viewModel::clearCache,
                                onPingTest = viewModel::runPingTest,
                                onToggleStopwatch = viewModel::toggleStopwatch,
                                onResetStopwatch = viewModel::resetStopwatch,
                                onToggleActiveInfo = {
                                    viewModel.updateSettings { it.copy(activeInfoHud = !it.activeInfoHud) }
                                },
                                onToggleTurboX = viewModel::engageTurboX,
                                onToggleDataPanel = {
                                    viewModel.updateSettings { it.copy(dataPanelVisible = !it.dataPanelVisible) }
                                },
                                onCycleCrosshair = viewModel::cycleCrosshair,
                                onCycleScreenColor = viewModel::cycleScreenColor,
                                onToggleScreenZoom = viewModel::toggleOrCycleScreenZoom,
                                onToggleBackgroundControl = {
                                    val next = !settings.backgroundControlEnabled
                                    viewModel.updateSettings { it.copy(backgroundControlEnabled = next) }
                                    if (next) viewModel.performBoost()
                                },
                                onToggleEffectSound = viewModel::cycleEffectSound,
                                onToggleScreenScaling = {
                                    viewModel.updateSettings { it.copy(screenScalingWide = !it.screenScalingWide) }
                                },
                                onToggleKeyMap = {
                                    viewModel.updateSettings { it.copy(keyMapTriggerVisible = !it.keyMapTriggerVisible) }
                                },
                                onBrightnessChange = viewModel::setBrightness,
                                onVolumeChange = viewModel::setVolume,
                                onToggleAutoRotate = viewModel::toggleAutoRotate,
                                onToggleAutoBrightness = viewModel::toggleAutoBrightness,
                                onToggleNoNotifications = viewModel::toggleNoNotifications,
                                onToggleDnd = viewModel::toggleDoNotDisturb,
                                onOpenWifi = viewModel.hardware::openWifiSettingsPanel,
                                onScreenshotInfo = { viewModel.captureScreenshot(this@MainActivity) },
                                onQuickApp = viewModel.hardware::launchQuickShortcut
                            )
                        )
                    }

                    // GameMode Opening Animation Splash
                    AnimatedVisibility(
                        visible = launchingGameName != null,
                        enter = fadeIn(tween(220)) + scaleIn(initialScale = 0.92f, animationSpec = tween(260)),
                        exit = fadeOut(tween(200))
                    ) {
                        GameLaunchAnimationOverlay(
                            gameName = launchingGameName ?: "",
                            cpuModeLabel = settings.cpuMode.label,
                            glowColor = cpuModeColor(settings.cpuMode, LocalTurboAccent.current.accent)
                        )
                    }

                    // Turbo Time Gate & Real AdMob Rewarded Ad Dialog
                    RewardedAdDialog(
                        step = adStep,
                        remainingTurboSeconds = remainingTurboSeconds,
                        adReady = rewardedAdReady,
                        adLoading = rewardedAdLoading,
                        adError = rewardedAdError,
                        cooldownSeconds = rewardedCooldownSeconds,
                        onWatchAd = { viewModel.watchRealRewardedAd(this@MainActivity) },
                        onRetryLoad = viewModel::retryLoadRewardedAd,
                        onDismiss = viewModel::dismissAdDialog
                    )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingRoute = intent.getStringExtra(GameOverlayService.EXTRA_START_ROUTE)
    }
}

@Composable
private fun GameLaunchAnimationOverlay(
    gameName: String,
    cpuModeLabel: String,
    glowColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(glowColor.copy(alpha = 0.25f), ObsidianVoid)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            TurboCockpitReactor(
                glowColor = glowColor,
                animatePulse = true,
                centerTitle = "TURBO X",
                modifier = Modifier.size(196.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "TURBO X ENGINE ENGAGED",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = RajdhaniFontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                ),
                color = glowColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Launching $gameName • $cpuModeLabel",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Optimizing memory & touch latency...",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}
