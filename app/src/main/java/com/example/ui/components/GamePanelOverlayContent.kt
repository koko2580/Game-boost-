package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CpuMode
import com.example.data.CpuUnitFormat
import com.example.data.EqPreset
import com.example.data.GamePanelStyle
import com.example.data.GameTurboSettings
import com.example.data.HardwareTelemetry
import com.example.data.ScreenColorMode
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSlate
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.LocalTurboAccent
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.roundToInt

data class OverlayCallbacks(
    val onHomeClick: () -> Unit,
    val onSettingsClick: () -> Unit,
    val onGamesClick: () -> Unit,
    val onPowerExitClick: () -> Unit,
    val onMinimizeClick: () -> Unit,
    val onSelectCpuMode: (CpuMode) -> Unit,
    val onCycleFrameRate: () -> Unit,
    val onBoostRam: () -> Unit,
    val onClearCache: () -> Unit,
    val onPingTest: () -> Unit,
    val onToggleStopwatch: () -> Unit,
    val onResetStopwatch: () -> Unit,
    val onToggleActiveInfo: () -> Unit,
    val onToggleTurboX: () -> Unit,
    val onToggleDataPanel: () -> Unit,
    val onCycleCrosshair: () -> Unit,
    val onCycleScreenColor: () -> Unit,
    val onToggleScreenZoom: () -> Unit,
    val onToggleBackgroundControl: () -> Unit,
    val onToggleEffectSound: () -> Unit,
    val onToggleScreenScaling: () -> Unit,
    val onToggleKeyMap: () -> Unit,
    val onBrightnessChange: (Float) -> Unit,
    val onVolumeChange: (Float) -> Unit,
    val onToggleAutoRotate: () -> Unit,
    val onToggleAutoBrightness: () -> Unit,
    val onToggleNoNotifications: () -> Unit,
    val onToggleDnd: () -> Unit,
    val onOpenWifi: () -> Unit,
    val onScreenshotInfo: () -> Unit,
    val onQuickApp: (List<String>, String) -> Unit
)

@Composable
fun GamePanelOverlayContent(
    settings: GameTurboSettings,
    telemetry: HardwareTelemetry,
    brightnessFraction: Float,
    volumeFraction: Float,
    autoBrightness: Boolean,
    autoRotate: Boolean,
    stopwatchSeconds: Long,
    isStopwatchRunning: Boolean,
    statusMessage: String?,
    callbacks: OverlayCallbacks,
    modifier: Modifier = Modifier
) {
    val palette = LocalTurboAccent.current
    val haptics = LocalHapticFeedback.current
    val isClassic = settings.gamePanelStyle == GamePanelStyle.CLASSIC
    val panelAccent = if (isClassic) CyberAmber else palette.accent
    val cpuRingColor = cpuModeColor(settings.cpuMode, panelAccent)

    var leftToolsGridMode by remember { mutableStateOf(false) }
    var cpuDropdownOpen by remember { mutableStateOf(false) }
    var portraitTab by remember { mutableStateOf(0) }

    var keyL1X by remember { mutableFloatStateOf(-70f) }
    var keyL1Y by remember { mutableFloatStateOf(0f) }
    var keyR1X by remember { mutableFloatStateOf(70f) }
    var keyR1Y by remember { mutableFloatStateOf(0f) }

    val leftTools = remember(
        telemetry.pingMs,
        settings.reducePingEnabled,
        settings.backgroundControlEnabled,
        isStopwatchRunning,
        stopwatchSeconds,
        settings.activeInfoHud,
        settings.eqPreset,
        settings.effectSoundEnabled,
        settings.screenScalingWide,
        settings.keyMapTriggerVisible,
        settings.turboXMode,
        settings.dataPanelVisible,
        settings.crosshairStyle,
        settings.screenColorMode,
        settings.screenZoomEnabled,
        settings.zoomScale,
        callbacks
    ) {
        listOf(
            WingToolItem("ping", telemetry.pingMs?.let { "Ping ${it}ms" } ?: "Reduce ping", Icons.Rounded.NetworkCheck, settings.reducePingEnabled, callbacks.onPingTest),
            WingToolItem("bg", "Background control", Icons.Rounded.Apps, settings.backgroundControlEnabled, callbacks.onToggleBackgroundControl),
            WingToolItem("stopwatch", "Quick Stopwatch", Icons.Rounded.Timer, isStopwatchRunning || stopwatchSeconds > 0, callbacks.onToggleStopwatch),
            WingToolItem("active_info", "Active info", Icons.Rounded.MonitorHeart, settings.activeInfoHud, callbacks.onToggleActiveInfo),
            WingToolItem("effect_sound", if (settings.eqPreset == EqPreset.OFF) "Effect Sound" else settings.eqPreset.label, Icons.Rounded.GraphicEq, settings.effectSoundEnabled && settings.eqPreset != EqPreset.OFF, callbacks.onToggleEffectSound),
            WingToolItem("screen_scale", "Screen scaling", Icons.Rounded.AspectRatio, settings.screenScalingWide, callbacks.onToggleScreenScaling),
            WingToolItem("keymap", "KeyMap Trigger", Icons.Rounded.TouchApp, settings.keyMapTriggerVisible, callbacks.onToggleKeyMap),
            WingToolItem("turbo_x", "Turbo X mode", Icons.Rounded.Speed, settings.turboXMode, callbacks.onToggleTurboX),
            WingToolItem("data_panel", "Data Panel", Icons.Rounded.Memory, settings.dataPanelVisible, callbacks.onToggleDataPanel),
            WingToolItem("crosshair", "Crosshair: ${settings.crosshairStyle.label}", Icons.Rounded.MyLocation, settings.crosshairStyle.ordinal > 0, callbacks.onCycleCrosshair),
            WingToolItem("screen_color", "Color: ${settings.screenColorMode.label}", Icons.Rounded.ColorLens, settings.screenColorMode != ScreenColorMode.DEFAULT, callbacks.onCycleScreenColor),
            WingToolItem("zoom", if (settings.screenZoomEnabled) "Zoom ${settings.zoomScale}x" else "Screen Zoom Tool", Icons.Rounded.ZoomIn, settings.screenZoomEnabled, callbacks.onToggleScreenZoom)
        )
    }

    val rightTiles = remember(
        autoRotate,
        settings.notificationMuteMode,
        autoBrightness,
        settings.frameRateTarget,
        telemetry.wifiLinkMbps,
        telemetry.networkType,
        settings.totalBoostCount,
        settings.dndMode,
        callbacks
    ) {
        listOf(
            RightTileItem("screenshot", "Screenshot", Icons.Rounded.Crop, null, false, callbacks.onScreenshotInfo),
            RightTileItem("auto_rotate", "Auto Rotate", Icons.Rounded.ScreenRotation, null, autoRotate, callbacks.onToggleAutoRotate),
            RightTileItem("no_notif", "No notifications", Icons.Rounded.NotificationsOff, null, settings.notificationMuteMode, callbacks.onToggleNoNotifications),
            RightTileItem("auto_light", "AutoLight", Icons.Rounded.BrightnessAuto, null, autoBrightness, callbacks.onToggleAutoBrightness),
            RightTileItem("frame_rate", "FrameRate", null, "${settings.frameRateTarget}Hz", true, callbacks.onCycleFrameRate),
            RightTileItem("wifi", telemetry.wifiLinkMbps?.let { "${it}Mbps" } ?: "Wifi", Icons.Rounded.Wifi, null, telemetry.networkType != "Offline", callbacks.onOpenWifi),
            RightTileItem("booster", "Booster", Icons.Rounded.RocketLaunch, null, settings.totalBoostCount > 0, callbacks.onBoostRam),
            RightTileItem("dnd", "Do Not Disturb", Icons.Rounded.DarkMode, null, settings.dndMode, callbacks.onToggleDnd),
            RightTileItem("clear_trash", "Clear trash", Icons.Rounded.CleaningServices, null, false, callbacks.onClearCache)
        )
    }

    val screenTintColor = when (settings.screenColorMode) {
        ScreenColorMode.DEFAULT -> Color.Transparent
        ScreenColorMode.VIVID -> Color(0x18FF2438)
        ScreenColorMode.NIGHT_VISION -> Color(0x2200E5FF)
        ScreenColorMode.WARM_EYE -> Color(0x24FFB300)
    }

    val cpuDisplayValue = if (settings.cpuUnitFormat == CpuUnitFormat.GHZ) {
        String.format(Locale.US, "%.2f", telemetry.cpuFreqMHz / 1000f)
    } else {
        "${telemetry.cpuFreqMHz}"
    }
    val cpuDisplayUnit = if (settings.cpuUnitFormat == CpuUnitFormat.GHZ) "GHz" else "MHz"

    val isRedesignedEsportsStyle = settings.gamePanelStyle == GamePanelStyle.ROG_STYLE ||
        settings.gamePanelStyle == GamePanelStyle.XIAOMI_STYLE ||
        settings.gamePanelStyle == GamePanelStyle.REDMAGIC_STYLE

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (isRedesignedEsportsStyle) 0.50f else 0.72f))
            .background(screenTintColor)
    ) {
        val isWideLandscape = maxWidth >= 620.dp
        val entranceProgress = remember(settings.gamePanelStyle) {
            Animatable(if (settings.lowRamOverlayMode) 1f else 0f)
        }
        LaunchedEffect(settings.gamePanelStyle, settings.lowRamOverlayMode) {
            if (settings.lowRamOverlayMode) {
                entranceProgress.snapTo(1f)
            } else {
                entranceProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.80f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        }

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

        if (settings.keyMapTriggerVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset { IntOffset(keyL1X.roundToInt(), keyL1Y.roundToInt()) }
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(panelAccent.copy(alpha = 0.45f))
                    .border(2.dp, panelAccent, CircleShape)
                    .pointerInput(Unit) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            keyL1X += drag.x
                            keyL1Y += drag.y
                        }
                    }
                    .clickable { haptics.performHapticFeedback(HapticFeedbackType.LongPress) },
                contentAlignment = Alignment.Center
            ) {
                Text("L1", style = MaterialTheme.typography.labelLarge, color = Color.White, fontWeight = FontWeight.Bold)
            }

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset { IntOffset(keyR1X.roundToInt(), keyR1Y.roundToInt()) }
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(ElectricCyan.copy(alpha = 0.42f))
                    .border(2.dp, ElectricCyan, CircleShape)
                    .pointerInput(Unit) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            keyR1X += drag.x
                            keyR1Y += drag.y
                        }
                    }
                    .clickable { haptics.performHapticFeedback(HapticFeedbackType.LongPress) },
                contentAlignment = Alignment.Center
            ) {
                Text("R1", style = MaterialTheme.typography.labelLarge, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isRedesignedEsportsStyle) {
                        Modifier
                    } else {
                        Modifier.graphicsLayer {
                            val t = entranceProgress.value.coerceIn(0f, 1f)
                            val s = 0.94f + (0.06f * t)
                            scaleX = s
                            scaleY = s
                            alpha = 0.45f + (0.55f * t)
                        }
                    }
                )
        ) {
            when (settings.gamePanelStyle) {
                GamePanelStyle.ROG_STYLE -> {
                    RogArmouryOverlayContent(
                        isWideLandscape = isWideLandscape,
                        settings = settings,
                        telemetry = telemetry,
                        cpuDisplayValue = cpuDisplayValue,
                        cpuDisplayUnit = cpuDisplayUnit,
                        brightnessFraction = brightnessFraction,
                        volumeFraction = volumeFraction,
                        stopwatchSeconds = stopwatchSeconds,
                        isStopwatchRunning = isStopwatchRunning,
                        statusMessage = statusMessage,
                        leftTools = leftTools,
                        rightTiles = rightTiles,
                        callbacks = callbacks
                    )
                }
                GamePanelStyle.XIAOMI_STYLE -> {
                    XiaomiHyperOverlayContent(
                        isWideLandscape = isWideLandscape,
                        settings = settings,
                        telemetry = telemetry,
                        cpuDisplayValue = cpuDisplayValue,
                        cpuDisplayUnit = cpuDisplayUnit,
                        brightnessFraction = brightnessFraction,
                        volumeFraction = volumeFraction,
                        statusMessage = statusMessage,
                        leftTools = leftTools,
                        rightTiles = rightTiles,
                        callbacks = callbacks
                    )
                }
                GamePanelStyle.REDMAGIC_STYLE -> {
                    RedMagicCyberOverlayContent(
                        isWideLandscape = isWideLandscape,
                        settings = settings,
                        telemetry = telemetry,
                        cpuDisplayValue = cpuDisplayValue,
                        cpuDisplayUnit = cpuDisplayUnit,
                        brightnessFraction = brightnessFraction,
                        volumeFraction = volumeFraction,
                        statusMessage = statusMessage,
                        leftTools = leftTools,
                        rightTiles = rightTiles,
                        callbacks = callbacks
                    )
                }
                GamePanelStyle.CLASSIC, GamePanelStyle.NEW -> {
                    if (isWideLandscape) {
                        Box(modifier = Modifier.fillMaxSize()) {
                if (!isClassic) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val wingOuterTop = w * 0.25f
                        val wingMid = w * 0.37f
                        val wingOuterBot = w * 0.19f

                        val leftPath = Path().apply {
                            moveTo(wingOuterTop, 0f)
                            lineTo(wingMid, h * 0.45f)
                            lineTo(wingOuterBot, h)
                        }
                        drawPath(leftPath, color = panelAccent, style = Stroke(width = 10.dp.toPx()))

                        val rightPath = Path().apply {
                            moveTo(w - wingOuterTop, 0f)
                            lineTo(w - wingMid, h * 0.45f)
                            lineTo(w - wingOuterBot, h)
                        }
                        drawPath(rightPath, color = panelAccent, style = Stroke(width = 10.dp.toPx()))
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // LEFT WING + LEFT GAUGE
                    Row(
                        modifier = Modifier
                            .weight(0.39f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(16.dp))
                            .background(ObsidianVoid.copy(alpha = 0.90f))
                            .border(
                                1.dp,
                                if (isClassic) panelAccent.copy(alpha = 0.45f) else CyberBorder,
                                RoundedCornerShape(16.dp)
                            )
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(0.50f)
                                .fillMaxHeight()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OverlayCircleAction(Icons.Rounded.Home, "Home", callbacks.onHomeClick, "overlay_home")
                                OverlayCircleAction(Icons.Rounded.Settings, "Settings", callbacks.onSettingsClick, "overlay_settings")
                                OverlayCircleAction(Icons.Rounded.Gamepad, "Games", callbacks.onGamesClick, "overlay_games")
                                OverlayCircleAction(Icons.Rounded.PowerSettingsNew, "Exit Overlay", callbacks.onPowerExitClick, "overlay_power")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CyberSlate)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .background(if (!leftToolsGridMode) panelAccent else Color.Transparent)
                                        .clickable { leftToolsGridMode = false },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.List, "List view", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .background(if (leftToolsGridMode) panelAccent else Color.Transparent)
                                        .clickable { leftToolsGridMode = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.GridView, "Grid view", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            if (!leftToolsGridMode) {
                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(leftTools, key = { it.id }) { tool ->
                                        LeftToolListTile(tool = tool, accentColor = panelAccent)
                                    }
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(leftTools, key = { it.id }) { tool ->
                                        LeftToolGridTile(tool = tool, accentColor = panelAccent)
                                    }
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .weight(0.50f)
                                .fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                HolographicGaugeRing(
                                    valueText = cpuDisplayValue,
                                    unitText = cpuDisplayUnit,
                                    bottomLabel = "CPU",
                                    ringColor = cpuRingColor,
                                    animate = !settings.lowRamOverlayMode
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Box {
                                    Row(
                                        modifier = Modifier
                                            .testTag("overlay_cpu_mode_pill")
                                            .clip(HexBadgeShape)
                                            .background(cpuRingColor.copy(alpha = 0.22f))
                                            .border(1.dp, cpuRingColor, HexBadgeShape)
                                            .clickable { cpuDropdownOpen = true }
                                            .padding(horizontal = 12.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = settings.cpuMode.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Icon(
                                            Icons.Rounded.KeyboardArrowDown,
                                            contentDescription = "Select CPU mode",
                                            tint = TextPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = cpuDropdownOpen,
                                        onDismissRequest = { cpuDropdownOpen = false }
                                    ) {
                                        CpuMode.entries.forEach { mode ->
                                            DropdownMenuItem(
                                                text = { Text(mode.label) },
                                                onClick = {
                                                    cpuDropdownOpen = false
                                                    callbacks.onSelectCpuMode(mode)
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            SegmentedVerticalBar(
                                fraction = brightnessFraction,
                                onFractionChange = callbacks.onBrightnessChange,
                                icon = Icons.Rounded.WbSunny,
                                iconDescription = "Screen brightness",
                                activeColor = Color.White,
                                barHeight = 92.dp
                            )
                        }
                    }

                    // CENTER HUD ZONE
                    Column(
                        modifier = Modifier
                            .weight(0.22f)
                            .fillMaxHeight()
                            .padding(horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .testTag("overlay_g_handle")
                                .size(38.dp, 30.dp)
                                .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                                .background(panelAccent)
                                .clickable { callbacks.onMinimizeClick() },
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

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (statusMessage != null) {
                                Surface(
                                    color = ObsidianVoid.copy(alpha = 0.92f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.border(1.dp, panelAccent, RoundedCornerShape(10.dp))
                                ) {
                                    Text(
                                        text = statusMessage,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextPrimary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            AnimatedVisibility(visible = settings.activeInfoHud || settings.dataPanelVisible, enter = fadeIn(), exit = fadeOut()) {
                                Column(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ObsidianVoid.copy(alpha = 0.90f))
                                        .border(1.dp, panelAccent.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${telemetry.actualFps} FPS (${String.format(Locale.US, "%.1f", telemetry.frameTimeMs)}ms)",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = EmeraldBright
                                    )
                                    Text(
                                        text = "${telemetry.batteryTempCelsius}°C • ${telemetry.ramAvailableMb}MB Free",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "${telemetry.networkType}" +
                                            (telemetry.pingMs?.let { " • ${it}ms (J:${telemetry.pingJitterMs ?: 0})" } ?: ""),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ElectricCyan
                                    )
                                }
                            }

                            AnimatedVisibility(visible = isStopwatchRunning || stopwatchSeconds > 0, enter = fadeIn(), exit = fadeOut()) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ObsidianVoid.copy(alpha = 0.9f))
                                        .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = formatStopwatch(stopwatchSeconds),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = CyberAmber
                                    )
                                    IconButton(onClick = callbacks.onToggleStopwatch, modifier = Modifier.size(26.dp)) {
                                        Icon(
                                            if (isStopwatchRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                            contentDescription = "Pause or resume stopwatch",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(onClick = callbacks.onResetStopwatch, modifier = Modifier.size(26.dp)) {
                                        Icon(
                                            Icons.Rounded.Refresh,
                                            contentDescription = "Reset stopwatch",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            QuickSocialIcon("Browser", Color(0xFF1A73E8), Icons.Rounded.Public) {
                                callbacks.onQuickApp(listOf("com.android.chrome"), "https://www.google.com")
                            }
                            QuickSocialIcon("Chat", Color(0xFF0088CC), Icons.Rounded.Forum) {
                                callbacks.onQuickApp(listOf("org.telegram.messenger", "com.whatsapp"), "https://web.telegram.org")
                            }
                            QuickSocialIcon("Video", Color(0xFFFF0000), Icons.Rounded.VideoLibrary) {
                                callbacks.onQuickApp(listOf("com.google.android.youtube"), "https://m.youtube.com")
                            }
                        }
                    }

                    // RIGHT WING + RIGHT GAUGE
                    Row(
                        modifier = Modifier
                            .weight(0.39f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(16.dp))
                            .background(ObsidianVoid.copy(alpha = 0.90f))
                            .border(
                                1.dp,
                                if (isClassic) panelAccent.copy(alpha = 0.45f) else CyberBorder,
                                RoundedCornerShape(16.dp)
                            )
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(0.50f)
                                .fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                HolographicGaugeRing(
                                    valueText = "${telemetry.ramUsedPercent}%",
                                    unitText = "",
                                    bottomLabel = "RAM",
                                    ringColor = panelAccent,
                                    animate = !settings.lowRamOverlayMode
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .testTag("overlay_ram_boost_pill")
                                        .clip(HexBadgeShape)
                                        .background(panelAccent.copy(alpha = 0.22f))
                                        .border(1.dp, panelAccent, HexBadgeShape)
                                        .clickable { callbacks.onBoostRam() }
                                        .padding(horizontal = 12.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (settings.turboXMode) "Turbo Clear" else "Boost RAM",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextPrimary,
                                        maxLines = 1
                                    )
                                }
                            }

                            SegmentedVerticalBar(
                                fraction = volumeFraction,
                                onFractionChange = callbacks.onVolumeChange,
                                icon = Icons.AutoMirrored.Rounded.VolumeUp,
                                iconDescription = "Media volume",
                                activeColor = Color.White,
                                barHeight = 92.dp
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(0.50f)
                                .fillMaxHeight()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                QuickSocialIcon("Web", Color(0xFF2B6CB0), Icons.Rounded.Public) {
                                    callbacks.onQuickApp(listOf("com.android.chrome"), "https://www.google.com")
                                }
                                QuickSocialIcon("Chat", Color(0xFF1D9BF0), Icons.Rounded.Forum) {
                                    callbacks.onQuickApp(listOf("com.twitter.android", "com.discord"), "https://discord.com")
                                }
                                QuickSocialIcon("Games", Color(0xFF3949AB), Icons.Rounded.SportsEsports) {
                                    callbacks.onGamesClick()
                                }
                                QuickSocialIcon("Video", Color(0xFFE53935), Icons.Rounded.VideoLibrary) {
                                    callbacks.onQuickApp(listOf("com.google.android.youtube"), "https://m.youtube.com")
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(rightTiles, key = { it.id }) { tile ->
                                    RightQuickTile(tile = tile, accentColor = panelAccent)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Adaptive Portrait Cyber HUD
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(ObsidianVoid.copy(alpha = 0.94f))
                    .border(1.5.dp, panelAccent.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OverlayCircleAction(Icons.Rounded.Home, "Home", callbacks.onHomeClick, "overlay_home")
                        OverlayCircleAction(Icons.Rounded.Settings, "Settings", callbacks.onSettingsClick, "overlay_settings")
                        OverlayCircleAction(Icons.Rounded.Gamepad, "Games", callbacks.onGamesClick, "overlay_games")
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(panelAccent)
                            .clickable { callbacks.onMinimizeClick() }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("${telemetry.actualFps} FPS • TURBO X HUD", style = MaterialTheme.typography.labelLarge, color = Color.White)
                    }
                    OverlayCircleAction(Icons.Rounded.Close, "Close Overlay", callbacks.onPowerExitClick, "overlay_power")
                }

                if (statusMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberAmber,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberSlate)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Performance", "Tools", "Quick Toggles").forEachIndexed { idx, label ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (portraitTab == idx) panelAccent else Color.Transparent)
                                .clickable { portraitTab = idx }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (portraitTab == idx) Color.White else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (portraitTab) {
                    0 -> {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                HolographicGaugeRing(
                                    valueText = cpuDisplayValue,
                                    unitText = cpuDisplayUnit,
                                    bottomLabel = "CPU",
                                    ringColor = cpuRingColor,
                                    animate = !settings.lowRamOverlayMode
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(HexBadgeShape)
                                        .background(cpuRingColor.copy(alpha = 0.22f))
                                        .border(1.dp, cpuRingColor, HexBadgeShape)
                                        .clickable {
                                            val next = CpuMode.entries[(settings.cpuMode.ordinal + 1) % CpuMode.entries.size]
                                            callbacks.onSelectCpuMode(next)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(settings.cpuMode.label, style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                SegmentedVerticalBar(
                                    fraction = brightnessFraction,
                                    onFractionChange = callbacks.onBrightnessChange,
                                    icon = Icons.Rounded.WbSunny,
                                    iconDescription = "Brightness",
                                    barHeight = 120.dp
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                HolographicGaugeRing(
                                    valueText = "${telemetry.ramUsedPercent}%",
                                    unitText = "",
                                    bottomLabel = "RAM",
                                    ringColor = panelAccent,
                                    animate = !settings.lowRamOverlayMode
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(HexBadgeShape)
                                        .background(panelAccent.copy(alpha = 0.22f))
                                        .border(1.dp, panelAccent, HexBadgeShape)
                                        .clickable { callbacks.onBoostRam() }
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("Boost RAM", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                SegmentedVerticalBar(
                                    fraction = volumeFraction,
                                    onFractionChange = callbacks.onVolumeChange,
                                    icon = Icons.AutoMirrored.Rounded.VolumeUp,
                                    iconDescription = "Volume",
                                    barHeight = 120.dp
                                )
                            }
                        }
                    }
                    1 -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(leftTools, key = { it.id }) { tool ->
                                LeftToolListTile(tool = tool, accentColor = panelAccent)
                            }
                        }
                    }
                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(rightTiles, key = { it.id }) { tile ->
                                RightQuickTile(tile = tile, accentColor = panelAccent)
                            }
                        }
                    }
                }
            }
        }
                }
            }
        }
    }
}

@Composable
private fun OverlayCircleAction(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    tag: String
) {
    Box(
        modifier = Modifier
            .testTag(tag)
            .size(34.dp)
            .clip(CircleShape)
            .background(Color(0xFF1D2232))
            .border(1.dp, CyberBorder, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = TextPrimary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun QuickSocialIcon(
    label: String,
    bgColor: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun LeftToolListTile(
    tool: WingToolItem,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (tool.isActive) accentColor.copy(alpha = 0.85f) else CyberSlate)
            .border(
                1.dp,
                if (tool.isActive) accentColor else CyberBorder,
                RoundedCornerShape(10.dp)
            )
            .clickable { tool.onClick() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = tool.icon,
            contentDescription = tool.label,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = tool.label,
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LeftToolGridTile(
    tool: WingToolItem,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (tool.isActive) accentColor.copy(alpha = 0.85f) else CyberSlate)
            .clickable { tool.onClick() }
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(tool.icon, contentDescription = tool.label, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = tool.label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RightQuickTile(
    tile: RightTileItem,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (tile.isActive) accentColor else CyberSlate)
            .border(
                1.dp,
                if (tile.isActive) accentColor else CyberBorder,
                RoundedCornerShape(10.dp)
            )
            .clickable { tile.onClick() }
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (tile.customBadgeText != null) {
            Text(
                text = tile.customBadgeText,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = RajdhaniFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
        } else if (tile.icon != null) {
            Icon(
                imageVector = tile.icon,
                contentDescription = tile.label,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = tile.label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatStopwatch(totalSeconds: Long): String {
    val mins = totalSeconds / 60
    val secs = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}
