package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
            WingToolItem("bg", "Background", Icons.Rounded.Apps, settings.backgroundControlEnabled, callbacks.onToggleBackgroundControl),
            WingToolItem("stopwatch", "Stopwatch", Icons.Rounded.Timer, isStopwatchRunning || stopwatchSeconds > 0, callbacks.onToggleStopwatch),
            WingToolItem("active_info", "Active info", Icons.Rounded.MonitorHeart, settings.activeInfoHud, callbacks.onToggleActiveInfo),
            WingToolItem("effect_sound", if (settings.eqPreset == EqPreset.OFF) "Audio EQ" else settings.eqPreset.label, Icons.Rounded.GraphicEq, settings.effectSoundEnabled && settings.eqPreset != EqPreset.OFF, callbacks.onToggleEffectSound),
            WingToolItem("screen_scale", "Scaling", Icons.Rounded.AspectRatio, settings.screenScalingWide, callbacks.onToggleScreenScaling),
            WingToolItem("keymap", "KeyMap L/R", Icons.Rounded.TouchApp, settings.keyMapTriggerVisible, callbacks.onToggleKeyMap),
            WingToolItem("turbo_x", "Turbo X", Icons.Rounded.Speed, settings.turboXMode, callbacks.onToggleTurboX),
            WingToolItem("data_panel", "Data Panel", Icons.Rounded.Memory, settings.dataPanelVisible, callbacks.onToggleDataPanel),
            WingToolItem("crosshair", "Aim: ${settings.crosshairStyle.label}", Icons.Rounded.MyLocation, settings.crosshairStyle.ordinal > 0, callbacks.onCycleCrosshair),
            WingToolItem("screen_color", "Color: ${settings.screenColorMode.label}", Icons.Rounded.ColorLens, settings.screenColorMode != ScreenColorMode.DEFAULT, callbacks.onCycleScreenColor),
            WingToolItem("zoom", if (settings.screenZoomEnabled) "Zoom ${settings.zoomScale}x" else "Scope Zoom", Icons.Rounded.ZoomIn, settings.screenZoomEnabled, callbacks.onToggleScreenZoom)
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
            RightTileItem("screenshot", "Capture", Icons.Rounded.Crop, null, false, callbacks.onScreenshotInfo),
            RightTileItem("auto_rotate", "Auto Rotate", Icons.Rounded.ScreenRotation, null, autoRotate, callbacks.onToggleAutoRotate),
            RightTileItem("no_notif", "Mute Notif", Icons.Rounded.NotificationsOff, null, settings.notificationMuteMode, callbacks.onToggleNoNotifications),
            RightTileItem("auto_light", "AutoLight", Icons.Rounded.BrightnessAuto, null, autoBrightness, callbacks.onToggleAutoBrightness),
            RightTileItem("frame_rate", "FrameRate", null, "${settings.frameRateTarget}Hz", true, callbacks.onCycleFrameRate),
            RightTileItem("wifi", telemetry.wifiLinkMbps?.let { "${it}Mbps" } ?: "Wi-Fi", Icons.Rounded.Wifi, null, telemetry.networkType != "Offline", callbacks.onOpenWifi),
            RightTileItem("booster", "RAM Boost", Icons.Rounded.RocketLaunch, null, settings.totalBoostCount > 0, callbacks.onBoostRam),
            RightTileItem("dnd", "Do Not Disturb", Icons.Rounded.DarkMode, null, settings.dndMode, callbacks.onToggleDnd),
            RightTileItem("clear_trash", "Clear Cache", Icons.Rounded.CleaningServices, null, false, callbacks.onClearCache)
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

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(screenTintColor)
    ) {
        val isWideLandscape = maxWidth >= 520.dp

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
                    callbacks = callbacks,
                    primaryAccent = Color(0xFFFF1744),
                    deepAccent = Color(0xFF9E0B22),
                    rootTag = "overlay_rog_root"
                )
            }
            GamePanelStyle.NEW -> {
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
                    callbacks = callbacks,
                    primaryAccent = palette.accent,
                    deepAccent = palette.accentDeep,
                    rootTag = "overlay_new_root"
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
                    stopwatchSeconds = stopwatchSeconds,
                    isStopwatchRunning = isStopwatchRunning,
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
                    stopwatchSeconds = stopwatchSeconds,
                    isStopwatchRunning = isStopwatchRunning,
                    statusMessage = statusMessage,
                    leftTools = leftTools,
                    rightTiles = rightTiles,
                    callbacks = callbacks
                )
            }
            GamePanelStyle.CLASSIC -> {
                CyberWingHalfOverlayContent(
                    isWideLandscape = isWideLandscape,
                    isClassic = isClassic,
                    panelAccent = panelAccent,
                    cpuRingColor = cpuRingColor,
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
        }

        if (settings.keyMapTriggerVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset { IntOffset(keyL1X.roundToInt(), keyL1Y.roundToInt()) }
                    .size(54.dp)
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
                    .size(54.dp)
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
    }
}

@Composable
private fun CyberWingHalfOverlayContent(
    isWideLandscape: Boolean,
    isClassic: Boolean,
    panelAccent: Color,
    cpuRingColor: Color,
    settings: GameTurboSettings,
    telemetry: HardwareTelemetry,
    cpuDisplayValue: String,
    cpuDisplayUnit: String,
    brightnessFraction: Float,
    volumeFraction: Float,
    stopwatchSeconds: Long,
    isStopwatchRunning: Boolean,
    statusMessage: String?,
    leftTools: List<WingToolItem>,
    rightTiles: List<RightTileItem>,
    callbacks: OverlayCallbacks
) {
    var activeTab by remember { mutableIntStateOf(0) }
    var cpuDropdownOpen by remember { mutableStateOf(false) }
    val shellShape = if (isClassic) RoundedCornerShape(16.dp) else CutCornerShape(topStart = 18.dp, bottomEnd = 18.dp, topEnd = 6.dp, bottomStart = 6.dp)
    val tileShape = RoundedCornerShape(9.dp)

    EsportsHalfDockShell(
        isWideLandscape = isWideLandscape,
        controlPanelLayout = settings.controlPanelLayout,
        lowRamOverlayMode = settings.lowRamOverlayMode,
        rootTag = if (isClassic) "overlay_classic_root" else "overlay_new_root",
        onDismissToHandle = callbacks.onMinimizeClick,
        maxLandscapeWidthFraction = 0.46f,
        maxLandscapeWidthDp = 400.dp,
        maxPortraitHeightFraction = 0.62f
    ) { dockEdge, _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shellShape)
                .background(ObsidianVoid.copy(alpha = 0.94f))
                .border(1.5.dp, panelAccent.copy(alpha = 0.72f), shellShape)
        ) {
            if (!isClassic) {
                // Tactical Neon Wing Corner Brackets for NEW Cyber HUD
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val bracketLen = 22.dp.toPx()
                    val stroke = 2.2.dp.toPx()
                    drawLine(panelAccent, Offset(0f, bracketLen), Offset(0f, 0f), stroke, StrokeCap.Round)
                    drawLine(panelAccent, Offset(0f, 0f), Offset(bracketLen, 0f), stroke, StrokeCap.Round)
                    drawLine(ElectricCyan, Offset(size.width - bracketLen, size.height), Offset(size.width, size.height), stroke, StrokeCap.Round)
                    drawLine(ElectricCyan, Offset(size.width, size.height - bracketLen), Offset(size.width, size.height), stroke, StrokeCap.Round)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // Header: Brand Title + Quick Actions + Effortless HIDE button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isClassic) "TURBO CLASSIC" else "TURBO X HUD",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = panelAccent,
                        maxLines = 1
                    )
                    OverlayActionIconRow(
                        accentColor = panelAccent,
                        containerColor = Color(0xFF1A1F2D),
                        shape = tileShape,
                        callbacks = callbacks,
                        iconSizeDp = 26.dp
                    )
                    EsportsCollapseChip(
                        label = "HIDE",
                        dockEdge = dockEdge,
                        accent = panelAccent,
                        shape = tileShape,
                        onClick = callbacks.onMinimizeClick
                    )
                }

                // Compact Live Telemetry + CPU Mode Selector + RAM Boost Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(tileShape)
                        .background(Color(0xFF141824))
                        .border(1.dp, CyberBorder.copy(alpha = 0.7f), tileShape)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "${telemetry.actualFps} FPS • $cpuDisplayValue $cpuDisplayUnit",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = RajdhaniFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = EmeraldBright
                        )
                        Text(
                            text = "${telemetry.batteryTempCelsius}°C • RAM ${telemetry.ramUsedPercent}%" +
                                (telemetry.pingMs?.let { " • ${it}ms" } ?: ""),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = TextSecondary
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            Row(
                                modifier = Modifier
                                    .testTag("overlay_cpu_mode_pill")
                                    .clip(HexBadgeShape)
                                    .background(cpuRingColor.copy(alpha = 0.22f))
                                    .border(1.dp, cpuRingColor, HexBadgeShape)
                                    .clickable { cpuDropdownOpen = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = settings.cpuMode.label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                    color = TextPrimary,
                                    maxLines = 1
                                )
                                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Select CPU mode", tint = TextPrimary, modifier = Modifier.size(13.dp))
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

                        Row(
                            modifier = Modifier
                                .testTag("overlay_ram_boost_pill")
                                .clip(HexBadgeShape)
                                .background(panelAccent.copy(alpha = 0.22f))
                                .border(1.dp, panelAccent, HexBadgeShape)
                                .clickable { callbacks.onBoostRam() }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Rounded.Bolt, contentDescription = null, tint = panelAccent, modifier = Modifier.size(12.dp))
                            Text(
                                text = "Boost",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                color = TextPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }

                if (statusMessage != null) {
                    OverlayStatusPill(statusMessage, panelAccent, tileShape)
                }
                if (isStopwatchRunning || stopwatchSeconds > 0L) {
                    EsportsStopwatchBar(
                        stopwatchSeconds = stopwatchSeconds,
                        isRunning = isStopwatchRunning,
                        accent = panelAccent,
                        shape = tileShape,
                        onToggle = callbacks.onToggleStopwatch,
                        onReset = callbacks.onResetStopwatch
                    )
                }

                StyleTabStrip(
                    tabs = listOf("FPS Tools", "Quick Tiles", "Audio & Apps"),
                    selectedIndex = activeTab,
                    accent = panelAccent,
                    shape = tileShape,
                    onSelect = { activeTab = it },
                    containerColor = Color(0xFF151926)
                )

                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
                    label = "classic_new_tab",
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) { tab ->
                    when (tab) {
                        0 -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp),
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                items(leftTools, key = { it.id }) { tool ->
                                    EsportsToolCard(
                                        label = tool.label,
                                        icon = tool.icon,
                                        badgeText = null,
                                        active = tool.isActive,
                                        activeAccent = panelAccent,
                                        inactiveContainer = Color(0xFF191E2D),
                                        shape = tileShape,
                                        cardHeight = if (isWideLandscape) 42.dp else 48.dp,
                                        onClick = tool.onClick
                                    )
                                }
                            }
                        }
                        1 -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp),
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                items(rightTiles, key = { it.id }) { tile ->
                                    EsportsToolCard(
                                        label = tile.label,
                                        icon = tile.icon,
                                        badgeText = tile.customBadgeText,
                                        active = tile.isActive,
                                        activeAccent = panelAccent,
                                        inactiveContainer = Color(0xFF191E2D),
                                        shape = tileShape,
                                        cardHeight = if (isWideLandscape) 42.dp else 48.dp,
                                        onClick = tile.onClick
                                    )
                                }
                            }
                        }
                        else -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(tileShape)
                                        .background(Color(0xFF151926))
                                        .border(1.dp, CyberBorder.copy(alpha = 0.5f), tileShape)
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    EsportsHorizontalSliderRow(Icons.Rounded.WbSunny, "Brightness", brightnessFraction, panelAccent, callbacks.onBrightnessChange)
                                    EsportsHorizontalSliderRow(Icons.AutoMirrored.Rounded.VolumeUp, "Volume", volumeFraction, ElectricCyan, callbacks.onVolumeChange)
                                }
                                EsportsQuickAppBar(shape = tileShape, callbacks = callbacks)
                            }
                        }
                    }
                }
            }
        }
    }
}
