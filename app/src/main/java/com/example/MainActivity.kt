package com.example

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.view.Choreographer
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import com.example.ui.components.NetworkGateOverlay
import com.example.ui.components.OneTapBoosterDialog
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
import com.example.ui.screens.TurboSplashScreen
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
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
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
            val showSplash by viewModel.showSplash.collectAsStateWithLifecycle()
            val prefsLoaded by viewModel.prefsLoaded.collectAsStateWithLifecycle()
            val isNetworkConnected by viewModel.isNetworkConnected.collectAsStateWithLifecycle()
            val oneTapState by viewModel.oneTapBooster.collectAsStateWithLifecycle()

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                var choreographerActive = false
                val frameCallback = object : Choreographer.FrameCallback {
                    override fun doFrame(frameTimeNanos: Long) {
                        if (!choreographerActive) return
                        viewModel.onChoreographerFrame(frameTimeNanos)
                        Choreographer.getInstance().postFrameCallback(this)
                    }
                }
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_START -> {
                            viewModel.setUiActive(true)
                            if (!choreographerActive) {
                                choreographerActive = true
                                Choreographer.getInstance().postFrameCallback(frameCallback)
                            }
                        }
                        Lifecycle.Event.ON_RESUME -> {
                            viewModel.refreshNetworkConnection()
                            viewModel.refreshPermissionsAndHardware()
                            viewModel.checkForegroundGameAutoLaunch()
                        }
                        Lifecycle.Event.ON_STOP -> {
                            viewModel.setUiActive(false)
                            choreographerActive = false
                            Choreographer.getInstance().removeFrameCallback(frameCallback)
                        }
                        else -> Unit
                    }
                }
                if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    viewModel.setUiActive(true)
                    choreographerActive = true
                    Choreographer.getInstance().postFrameCallback(frameCallback)
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    choreographerActive = false
                    Choreographer.getInstance().removeFrameCallback(frameCallback)
                    lifecycleOwner.lifecycle.removeObserver(observer)
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
                settings.optimizeFramePacing,
                settings.enableAdpfHints,
                settings.disablePowerSavingInGame,
                settings.smartBackgroundControl,
                settings.displayResolutionOpt,
                settings.preferredDisplayModeId,
                settings.sustainedPerformanceMode,
                settings.autoPerformanceOptimization,
                telemetry.supportedRefreshRates
            ) {
                viewModel.syncWindowPerformance(this@MainActivity)
            }
            LaunchedEffect(settings.rotationLock) {
                requestedOrientation = if (settings.rotationLock) {
                    ActivityInfo.SCREEN_ORIENTATION_LOCKED
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
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
                            viewModel.markSplashComplete()
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
                                peekAppIcon = viewModel::peekAppIcon,
                                loadAppIconAsync = viewModel::loadAppIconAsync,
                                onSelectGame = viewModel::selectGame,
                                onStartPlay = viewModel::launchSelectedGame,
                                onAddTurboTimeClick = { viewModel.openWatchAdPrompt() },
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
                                onBoostRam = { viewModel.runOneTapBooster() },
                                onRunPingTest = viewModel::runPingTest,
                                onSelectCpuMode = viewModel::setCpuMode,
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
                                    onOpenDeveloperOptions = viewModel::openDeveloperRenderingOptions,
                                    onSetOptimizeFramePacing = viewModel::setOptimizeFramePacing,
                                    onSetEnableAdpfHints = viewModel::setEnableAdpfHints,
                                    onSetDisablePowerSavingInGame = viewModel::setDisablePowerSavingInGame,
                                    onOpenPowerSaverSettings = viewModel::openPowerSaverSettings,
                                    onSetSmartBackgroundControl = viewModel::setSmartBackgroundControl,
                                    onRunSmartBackgroundTrim = viewModel::runSmartBackgroundTrim,
                                    onOpenAppManagementSettings = viewModel::openAppManagementSettings,
                                    onSetDisplayResolutionOpt = viewModel::setDisplayResolutionOpt,
                                    onSelectDisplayModeId = viewModel::selectDisplayModeId,
                                    onOpenDisplaySettings = viewModel::openDisplaySettings,
                                    onSetSustainedPerformanceMode = viewModel::setSustainedPerformanceMode,
                                    onSetAutoPerformanceOptimization = viewModel::setAutoPerformanceOptimization
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
                                peekAppIcon = viewModel::peekAppIcon,
                                loadAppIconAsync = viewModel::loadAppIconAsync,
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
                                onRequestUsageStats = {
                                    viewModel.suppressAppOpenForExternalFlow()
                                    viewModel.hardware.openUsageAccessSettings()
                                },
                                onSuppressAppOpen = viewModel::suppressAppOpenForExternalFlow,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }

                    // Screen Color subtle tint when enabled outside expanded overlay
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
                    }

                    // Interactive In-App Dual-Wing / Esports Game Panel Overlay
                    AnimatedVisibility(
                        visible = isOverlayOpen,
                        enter = fadeIn(tween(220)) + scaleIn(
                            initialScale = 0.94f,
                            animationSpec = spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessMediumLow)
                        ),
                        exit = fadeOut(tween(180)) + scaleOut(
                            targetScale = 0.96f,
                            animationSpec = tween(180)
                        )
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

                    // One Tap Booster 6-Stage Diagnostic & Final Report Dialog
                    OneTapBoosterDialog(
                        state = oneTapState,
                        onReBoost = { viewModel.runOneTapBooster() },
                        onOpenPowerSettings = viewModel::openPowerSaverSettings,
                        onDismiss = viewModel::dismissOneTapBooster
                    )

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

                    // First-Launch Required Permissions Onboarding Screen
                    AnimatedVisibility(
                        visible = prefsLoaded && !settings.hasCompletedPermissionSetup && pendingRoute == null,
                        enter = fadeIn(tween(220)),
                        exit = fadeOut(tween(260)) + scaleOut(targetScale = 1.03f, animationSpec = tween(240))
                    ) {
                        BackHandler(enabled = true) {
                            viewModel.completePermissionSetup()
                        }
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
                            onRequestUsageStats = {
                                viewModel.suppressAppOpenForExternalFlow()
                                viewModel.hardware.openUsageAccessSettings()
                            },
                            onSuppressAppOpen = viewModel::suppressAppOpenForExternalFlow,
                            onBack = viewModel::completePermissionSetup,
                            isOnboardingMode = true,
                            onCompleteOnboarding = viewModel::completePermissionSetup
                        )
                    }

                    // Mandatory Online Gate (blocks app usage completely when offline)
                    AnimatedVisibility(
                        visible = !isNetworkConnected && !showSplash,
                        enter = fadeIn(tween(220)),
                        exit = fadeOut(tween(220))
                    ) {
                        NetworkGateOverlay(
                            onOpenNetworkSettings = viewModel::openNetworkSettings,
                            onRetryCheck = { viewModel.refreshNetworkConnection() }
                        )
                    }

                    // Animated Turbo X Brand Splash Screen
                    AnimatedVisibility(
                        visible = showSplash && launchingGameName == null,
                        enter = fadeIn(tween(100)),
                        exit = fadeOut(tween(380)) + scaleOut(targetScale = 1.06f, animationSpec = tween(380))
                    ) {
                        TurboSplashScreen(onFinished = viewModel::markSplashComplete)
                    }
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
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(glowColor.copy(alpha = 0.25f), ObsidianVoid)
                )
            )
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        val isLandscape = maxWidth > maxHeight
        val reactorSize = (maxHeight * 0.56f).coerceIn(128.dp, 188.dp)
        val portraitReactorSize = (maxWidth * 0.48f).coerceIn(148.dp, 196.dp)
        if (isLandscape) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(24.dp)
            ) {
                TurboCockpitReactor(
                    glowColor = glowColor,
                    animatePulse = true,
                    centerTitle = "TURBO X",
                    modifier = Modifier.size(reactorSize)
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "TURBO X ENGINE ENGAGED",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        ),
                        color = glowColor
                    )
                    Text(
                        text = "Launching $gameName • $cpuModeLabel",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Optimizing memory & touch latency...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                TurboCockpitReactor(
                    glowColor = glowColor,
                    animatePulse = true,
                    centerTitle = "TURBO X",
                    modifier = Modifier.size(portraitReactorSize)
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
}
