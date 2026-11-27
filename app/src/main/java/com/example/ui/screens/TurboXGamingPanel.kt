package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.mandatorySystemGestures
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.CpuMode
import com.example.data.GameTurboSettings
import com.example.data.HardwareTelemetry
import com.example.data.InstalledAppInfo
import com.example.data.PerformanceEngineStatus
import com.example.data.PermissionStatus
import com.example.ui.components.XGameBottomCommandBar
import com.example.ui.components.XGameCenterGameplayFrame
import com.example.ui.components.XGameControlsRightPanel
import com.example.ui.components.XGameDeepBlack
import com.example.ui.components.XGameGraphiteElevated
import com.example.ui.components.XGameNeonRed
import com.example.ui.components.XGamePerformanceLeftPanel
import com.example.ui.components.XGameSpaceBackdropCanvas
import com.example.ui.components.XGameTopStatusBar
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * TURBO X — X-GAME SPACE
 * Independent REDMAGIC Gaming Space-inspired three-column landscape dashboard
 * wired strictly to existing Turbo X telemetry, game library, and controls.
 */
@Composable
fun TurboXGamingPanel(
    settings: GameTurboSettings,
    telemetry: HardwareTelemetry,
    performanceStatus: PerformanceEngineStatus,
    permissions: PermissionStatus,
    addedGames: List<InstalledAppInfo>,
    remainingTurboSeconds: Long,
    brightnessFraction: Float,
    volumeFraction: Float,
    autoBrightness: Boolean,
    autoRotate: Boolean,
    isOverlayServiceRunning: Boolean,
    statusMessage: String?,
    peekAppIcon: (String) -> ImageBitmap?,
    loadAppIconAsync: suspend (String) -> ImageBitmap?,
    onBack: () -> Unit,
    onSelectGame: (String) -> Unit,
    onStartPlay: () -> Unit,
    onBoostRam: () -> Unit,
    onClearCache: () -> Unit,
    onRunPingTest: () -> Unit,
    onSelectCpuMode: (CpuMode) -> Unit,
    onToggleTurboX: () -> Unit,
    onToggleGpuBoost: () -> Unit,
    onCycleFrameRate: () -> Unit,
    onCycleCrosshair: () -> Unit,
    onCycleScreenColor: () -> Unit,
    onCycleEqPreset: () -> Unit,
    onToggleDnd: () -> Unit,
    onToggleNoNotif: () -> Unit,
    onToggleAutoRotate: () -> Unit,
    onToggleAutoBrightness: () -> Unit,
    onToggleActiveInfo: () -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleOverlayService: () -> Unit,
    onOpenInAppOverlay: () -> Unit,
    onAddTurboTime: () -> Unit,
    onNavigateAddApps: () -> Unit,
    onNavigatePermissions: () -> Unit,
    onNavigateSettings: () -> Unit,
    onDismissStatus: () -> Unit
) {
    var clockText by remember {
        mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()))
    }
    LaunchedEffect(Unit) {
        while (true) {
            clockText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            delay(15_000L)
        }
    }

    LaunchedEffect(statusMessage) {
        if (statusMessage != null) {
            delay(2600L)
            onDismissStatus()
        }
    }

    val selectedGame = addedGames.find { it.packageName == settings.selectedGamePackage }
        ?: addedGames.firstOrNull()
    val selectedGameIcon by produceState<ImageBitmap?>(
        initialValue = selectedGame?.let { peekAppIcon(it.packageName) },
        key1 = selectedGame?.packageName
    ) {
        val pkg = selectedGame?.packageName
        value = if (pkg != null) peekAppIcon(pkg) ?: loadAppIconAsync(pkg) else null
    }

    val pulseAlpha = if (settings.homeAnimation && !settings.lowRamOverlayMode) {
        val infinite = rememberInfiniteTransition(label = "xgame_space_pulse")
        val p by infinite.animateFloat(
            initialValue = 0.60f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "xgame_pulse_alpha"
        )
        p
    } else {
        0.86f
    }

    Box(
        modifier = Modifier
            .testTag("turbo_x_gaming_panel_root")
            .fillMaxSize()
            .background(XGameDeepBlack)
    ) {
        XGameSpaceBackdropCanvas(pulseAlpha = pulseAlpha)

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.union(
                        WindowInsets.mandatorySystemGestures.only(WindowInsetsSides.Bottom)
                    )
                )
                .padding(start = 10.dp, end = 14.dp, top = 6.dp, bottom = 6.dp)
        ) {
            val isLandscape = maxWidth >= 560.dp
            val compactHeight = maxHeight < 430.dp

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(if (compactHeight) 6.dp else 8.dp)
            ) {
                XGameTopStatusBar(
                    clock = clockText,
                    telemetry = telemetry,
                    remainingTurboSeconds = remainingTurboSeconds,
                    permissions = permissions,
                    compact = !isLandscape,
                    compactHeight = compactHeight,
                    onBack = onBack,
                    onAddTurboTime = onAddTurboTime,
                    onNavigateAddApps = onNavigateAddApps,
                    onNavigatePermissions = onNavigatePermissions,
                    onNavigateSettings = onNavigateSettings
                )

                if (isLandscape) {
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        XGamePerformanceLeftPanel(
                            settings = settings,
                            telemetry = telemetry,
                            performanceStatus = performanceStatus,
                            pulseAlpha = pulseAlpha,
                            compactHeight = compactHeight,
                            onSelectCpuMode = onSelectCpuMode,
                            onRunPingTest = onRunPingTest,
                            modifier = Modifier.weight(0.28f).fillMaxHeight()
                        )

                        XGameCenterGameplayFrame(
                            selectedGame = selectedGame,
                            selectedGameIcon = selectedGameIcon,
                            addedGames = addedGames,
                            settings = settings,
                            telemetry = telemetry,
                            pulseAlpha = pulseAlpha,
                            compactHeight = compactHeight,
                            peekAppIcon = peekAppIcon,
                            loadAppIconAsync = loadAppIconAsync,
                            onSelectGame = onSelectGame,
                            onStartPlay = onStartPlay,
                            onNavigateAddApps = onNavigateAddApps,
                            modifier = Modifier.weight(0.44f).fillMaxHeight()
                        )

                        XGameControlsRightPanel(
                            settings = settings,
                            autoBrightness = autoBrightness,
                            autoRotate = autoRotate,
                            pulseAlpha = pulseAlpha,
                            onCycleFrameRate = onCycleFrameRate,
                            onToggleTurboX = onToggleTurboX,
                            onToggleGpuBoost = onToggleGpuBoost,
                            onCycleCrosshair = onCycleCrosshair,
                            onToggleDnd = onToggleDnd,
                            onToggleNoNotif = onToggleNoNotif,
                            onCycleEqPreset = onCycleEqPreset,
                            onCycleScreenColor = onCycleScreenColor,
                            onToggleAutoRotate = onToggleAutoRotate,
                            onToggleAutoBrightness = onToggleAutoBrightness,
                            onToggleActiveInfo = onToggleActiveInfo,
                            onClearCache = onClearCache,
                            scrollableGrid = true,
                            modifier = Modifier.weight(0.28f).fillMaxHeight()
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        XGameCenterGameplayFrame(
                            selectedGame = selectedGame,
                            selectedGameIcon = selectedGameIcon,
                            addedGames = addedGames,
                            settings = settings,
                            telemetry = telemetry,
                            pulseAlpha = pulseAlpha,
                            compactHeight = false,
                            peekAppIcon = peekAppIcon,
                            loadAppIconAsync = loadAppIconAsync,
                            onSelectGame = onSelectGame,
                            onStartPlay = onStartPlay,
                            onNavigateAddApps = onNavigateAddApps,
                            modifier = Modifier.fillMaxWidth().height(290.dp)
                        )
                        XGamePerformanceLeftPanel(
                            settings = settings,
                            telemetry = telemetry,
                            performanceStatus = performanceStatus,
                            pulseAlpha = pulseAlpha,
                            compactHeight = false,
                            onSelectCpuMode = onSelectCpuMode,
                            onRunPingTest = onRunPingTest,
                            modifier = Modifier.fillMaxWidth()
                        )
                        XGameControlsRightPanel(
                            settings = settings,
                            autoBrightness = autoBrightness,
                            autoRotate = autoRotate,
                            pulseAlpha = pulseAlpha,
                            onCycleFrameRate = onCycleFrameRate,
                            onToggleTurboX = onToggleTurboX,
                            onToggleGpuBoost = onToggleGpuBoost,
                            onCycleCrosshair = onCycleCrosshair,
                            onToggleDnd = onToggleDnd,
                            onToggleNoNotif = onToggleNoNotif,
                            onCycleEqPreset = onCycleEqPreset,
                            onCycleScreenColor = onCycleScreenColor,
                            onToggleAutoRotate = onToggleAutoRotate,
                            onToggleAutoBrightness = onToggleAutoBrightness,
                            onToggleActiveInfo = onToggleActiveInfo,
                            onClearCache = onClearCache,
                            scrollableGrid = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                XGameBottomCommandBar(
                    totalBoostCount = settings.totalBoostCount,
                    brightnessFraction = brightnessFraction,
                    volumeFraction = volumeFraction,
                    isOverlayServiceRunning = isOverlayServiceRunning,
                    compact = !isLandscape,
                    compactHeight = compactHeight,
                    onBrightnessChange = onBrightnessChange,
                    onVolumeChange = onVolumeChange,
                    onBoostRam = onBoostRam,
                    onToggleOverlayService = onToggleOverlayService,
                    onOpenInAppOverlay = onOpenInAppOverlay
                )
            }

            AnimatedVisibility(
                visible = statusMessage != null,
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)
                ) + fadeIn(tween(180)),
                exit = slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(160)) + fadeOut(tween(150)),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 46.dp)
            ) {
                if (statusMessage != null) {
                    val toastShape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp)
                    Surface(
                        color = XGameGraphiteElevated.copy(alpha = 0.96f),
                        shape = toastShape,
                        shadowElevation = 8.dp,
                        modifier = Modifier.border(1.dp, XGameNeonRed, toastShape)
                    ) {
                        Text(
                            text = statusMessage,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = RajdhaniFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }
    }
}
