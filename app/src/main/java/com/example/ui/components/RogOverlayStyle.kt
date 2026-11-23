package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CpuMode
import com.example.data.CrosshairStyle
import com.example.data.GameTurboSettings
import com.example.data.HardwareTelemetry
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

// =====================================================================
// TURBO X — ASUS ROG GAME GENIE-INSPIRED BOTTOM GAMING OVERLAY
// Bottom-anchored angular wing console • Top Performance HUD Crown
// Central X-MODE pod • Symmetrical Tool Wings • Bottom Control Bar
// =====================================================================
@Composable
internal fun RogArmouryOverlayContent(
    isWideLandscape: Boolean,
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
    callbacks: OverlayCallbacks,
    primaryAccent: Color = Color(0xFFFF1744),
    deepAccent: Color = Color(0xFF9E0B22),
    rootTag: String = "overlay_rog_root"
) {
    val navyGlassTop = Color(0xEB0A0E1B)
    val navyGlassBottom = Color(0xF504060D)
    val tileSurface = Color(0xCC111829)
    val chipShape = CutCornerShape(topStart = 7.dp, bottomEnd = 7.dp, topEnd = 2.dp, bottomStart = 2.dp)
    val wingDeckShape = remember(isWideLandscape) {
        GenericShape { size, _ ->
            addPath(rogWingDeckPath(size.width, size.height, isWideLandscape))
        }
    }

    var quickControlOpen by remember { mutableStateOf(false) }
    var leftWingPage by remember { mutableIntStateOf(0) }
    var rightWingPage by remember { mutableIntStateOf(0) }
    var portraitTab by remember { mutableIntStateOf(0) }

    val pulseAlpha = if (!settings.lowRamOverlayMode) {
        val inf = rememberInfiniteTransition(label = "rog_genie_pulse")
        val v by inf.animateFloat(
            initialValue = 0.58f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1700, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "rog_pulse_alpha"
        )
        v
    } else {
        0.82f
    }

    val toolById = remember(leftTools) { leftTools.associateBy { it.id } }
    val tileById = remember(rightTiles) { rightTiles.associateBy { it.id } }

    // Left Wing Page 1 & 2 (all focus/perf/system shortcuts cleanly mapped without losing a single handler)
    val leftPage1 = remember(toolById, tileById) {
        listOfNotNull(
            tileById["frame_rate"]?.toUnifiedTool(),
            tileById["no_notif"]?.toUnifiedTool(),
            tileById["dnd"]?.toUnifiedTool(),
            toolById["bg"]?.toUnifiedTool(),
            toolById["ping"]?.toUnifiedTool(),
            tileById["clear_trash"]?.toUnifiedTool()
        )
    }
    val leftPage2 = remember(toolById, tileById) {
        listOfNotNull(
            tileById["screenshot"]?.toUnifiedTool(),
            tileById["auto_rotate"]?.toUnifiedTool(),
            tileById["auto_light"]?.toUnifiedTool(),
            tileById["wifi"]?.toUnifiedTool(),
            tileById["booster"]?.toUnifiedTool(),
            toolById["stopwatch"]?.toUnifiedTool()
        )
    }

    // Right Wing Page 1 & 2 (tactical overlays + quick apps)
    val rightPage1 = remember(toolById) {
        listOfNotNull(
            toolById["keymap"]?.toUnifiedTool(),
            toolById["turbo_x"]?.toUnifiedTool(),
            toolById["data_panel"]?.toUnifiedTool(),
            toolById["active_info"]?.toUnifiedTool(),
            toolById["effect_sound"]?.toUnifiedTool(),
            toolById["zoom"]?.toUnifiedTool()
        )
    }
    val rightPage2 = remember(toolById, callbacks) {
        listOfNotNull(
            toolById["crosshair"]?.toUnifiedTool(),
            toolById["screen_color"]?.toUnifiedTool(),
            toolById["screen_scale"]?.toUnifiedTool(),
            UnifiedWingTile("app_browser", "Browser", Icons.Rounded.Public, null, false) {
                callbacks.onQuickApp(listOf("com.android.chrome"), "https://www.google.com")
            },
            UnifiedWingTile("app_chat", "Chat", Icons.Rounded.Forum, null, false) {
                callbacks.onQuickApp(listOf("org.telegram.messenger", "com.whatsapp"), "https://web.telegram.org")
            },
            UnifiedWingTile("app_video", "Video", Icons.Rounded.VideoLibrary, null, false) {
                callbacks.onQuickApp(listOf("com.google.android.youtube"), "https://m.youtube.com")
            }
        )
    }

    RogBottomCockpitShell(
        isWideLandscape = isWideLandscape,
        lowRamOverlayMode = settings.lowRamOverlayMode,
        rootTag = rootTag,
        onDismissToHandle = callbacks.onMinimizeClick
    ) { _, _ ->
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (isWideLandscape) 3.dp else 5.dp)
        ) {
            // Optional Transient Status Banner floating cleanly above HUD
            if (statusMessage != null) {
                OverlayStatusPill(statusMessage, primaryAccent, chipShape)
            }

            // 1. TOP PERFORMANCE HUD CROWN (FPS, RAM, Temp, Battery, Network, Refresh Rate + Hide Handle)
            RogTopPerformanceHudBar(
                isWideLandscape = isWideLandscape,
                telemetry = telemetry,
                cpuDisplayValue = cpuDisplayValue,
                cpuDisplayUnit = cpuDisplayUnit,
                accent = primaryAccent,
                deepAccent = deepAccent,
                pulseAlpha = pulseAlpha,
                onPingClick = callbacks.onPingTest,
                onCycleRefreshClick = callbacks.onCycleFrameRate,
                onCollapseClick = callbacks.onMinimizeClick
            )

            // 2. MAIN ANGULAR FUTURISTIC GAMING CHASSIS
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isWideLandscape) Modifier.height(148.dp) else Modifier.height(294.dp)
                    )
                    .clip(wingDeckShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(navyGlassTop, Color(0xF2080C17), navyGlassBottom)
                        )
                    )
            ) {
                // Custom Angular Glowing Border + Center Crimson Core Halo + Carbon Texture
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val deckPath = rogWingDeckPath(size.width, size.height, isWideLandscape)

                    // Center X-MODE crimson plasma aura
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primaryAccent.copy(alpha = 0.22f * pulseAlpha),
                                deepAccent.copy(alpha = 0.09f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.5f, size.height * 0.42f),
                            radius = size.width * 0.34f
                        ),
                        radius = size.width * 0.34f,
                        center = Offset(size.width * 0.5f, size.height * 0.42f)
                    )

                    // Subtle diagonal carbon-mesh gaming stripes on wings
                    val step = 16.dp.toPx()
                    var x = -size.height
                    while (x < size.width + size.height) {
                        drawLine(
                            color = primaryAccent.copy(alpha = 0.045f),
                            start = Offset(x, size.height),
                            end = Offset(x + size.height * 0.45f, 0f),
                            strokeWidth = 2.dp.toPx()
                        )
                        x += step
                    }

                    // Soft outer crimson glow stroke + sharp metallic edge stroke
                    drawPath(
                        path = deckPath,
                        color = primaryAccent.copy(alpha = 0.26f * pulseAlpha),
                        style = Stroke(width = 4.5.dp.toPx())
                    )
                    drawPath(
                        path = deckPath,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                deepAccent,
                                primaryAccent,
                                Color(0xFFFF6E7F),
                                primaryAccent,
                                deepAccent
                            )
                        ),
                        style = Stroke(width = 1.6.dp.toPx())
                    )

                    // Symmetrical wing divider chevrons flanking center X-MODE pod in landscape
                    if (isWideLandscape) {
                        val leftDivX = size.width * 0.315f
                        val rightDivX = size.width * 0.685f
                        drawLine(
                            color = primaryAccent.copy(alpha = 0.42f),
                            start = Offset(leftDivX, 12.dp.toPx()),
                            end = Offset(leftDivX + 8.dp.toPx(), size.height - 32.dp.toPx()),
                            strokeWidth = 1.2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = primaryAccent.copy(alpha = 0.42f),
                            start = Offset(rightDivX, 12.dp.toPx()),
                            end = Offset(rightDivX - 8.dp.toPx(), size.height - 32.dp.toPx()),
                            strokeWidth = 1.2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                if (isWideLandscape) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Upper Symmetrical Wings + Central X-MODE Pod
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // LEFT GAMING TOOLS WING
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(top = 8.dp)
                            ) {
                                AnimatedContent(
                                    targetState = quickControlOpen,
                                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
                                    label = "rog_left_wing_mode"
                                ) { showQuick ->
                                    if (showQuick) {
                                        RogQuickControlSlidersDeck(
                                            brightnessFraction = brightnessFraction,
                                            volumeFraction = volumeFraction,
                                            accent = primaryAccent,
                                            chipShape = chipShape,
                                            onBrightnessChange = callbacks.onBrightnessChange,
                                            onVolumeChange = callbacks.onVolumeChange
                                        )
                                    } else {
                                        RogWingToolsBlock(
                                            title = if (leftWingPage == 0) "CORE TOOLS" else "SYSTEM",
                                            pageLabel = "${leftWingPage + 1}/2",
                                            items = if (leftWingPage == 0) leftPage1 else leftPage2,
                                            accent = primaryAccent,
                                            inactiveSurface = tileSurface,
                                            onTogglePage = { leftWingPage = (leftWingPage + 1) % 2 }
                                        )
                                    }
                                }
                            }

                            // CENTER PROMINENT X-MODE CORE POD
                            RogCenterXModePod(
                                selectedMode = settings.cpuMode,
                                ramUsedPercent = telemetry.ramUsedPercent,
                                batteryPercent = telemetry.batteryPercent,
                                isCharging = telemetry.isCharging,
                                stopwatchSeconds = stopwatchSeconds,
                                isStopwatchRunning = isStopwatchRunning,
                                accent = primaryAccent,
                                deepAccent = deepAccent,
                                pulseAlpha = pulseAlpha,
                                chipShape = chipShape,
                                onSelectMode = callbacks.onSelectCpuMode,
                                onBoostRam = callbacks.onBoostRam,
                                onToggleStopwatch = callbacks.onToggleStopwatch,
                                onResetStopwatch = callbacks.onResetStopwatch,
                                modifier = Modifier
                                    .width(194.dp)
                                    .fillMaxHeight()
                            )

                            // RIGHT GAMING TOOLS WING
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(top = 8.dp)
                            ) {
                                AnimatedContent(
                                    targetState = quickControlOpen,
                                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
                                    label = "rog_right_wing_mode"
                                ) { showQuick ->
                                    if (showQuick) {
                                        RogQuickControlAppsAndTimerDeck(
                                            stopwatchSeconds = stopwatchSeconds,
                                            isStopwatchRunning = isStopwatchRunning,
                                            accent = primaryAccent,
                                            chipShape = chipShape,
                                            callbacks = callbacks
                                        )
                                    } else {
                                        RogWingToolsBlock(
                                            title = if (rightWingPage == 0) "TACTICAL" else "HUD & APPS",
                                            pageLabel = "${rightWingPage + 1}/2",
                                            items = if (rightWingPage == 0) rightPage1 else rightPage2,
                                            accent = primaryAccent,
                                            inactiveSurface = tileSurface,
                                            onTogglePage = { rightWingPage = (rightWingPage + 1) % 2 }
                                        )
                                    }
                                }
                            }
                        }

                        // BOTTOM CONTROL BAR
                        RogBottomControlBar(
                            isWideLandscape = true,
                            quickControlOpen = quickControlOpen,
                            crosshairStyle = settings.crosshairStyle,
                            frameRateTarget = settings.frameRateTarget,
                            gpuBoostActive = settings.increaseGpuPerformance || settings.turboXMode,
                            accent = primaryAccent,
                            chipShape = chipShape,
                            onToggleQuickControl = { quickControlOpen = !quickControlOpen },
                            onCycleCrosshair = callbacks.onCycleCrosshair,
                            onCycleGpuPerf = callbacks.onCycleFrameRate,
                            callbacks = callbacks
                        )
                    }
                } else {
                    // PORTRAIT ADAPTIVE DECK: Compact bottom-docked chassis preserving all tools & tests
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RogCenterXModePod(
                            selectedMode = settings.cpuMode,
                            ramUsedPercent = telemetry.ramUsedPercent,
                            batteryPercent = telemetry.batteryPercent,
                            isCharging = telemetry.isCharging,
                            stopwatchSeconds = stopwatchSeconds,
                            isStopwatchRunning = isStopwatchRunning,
                            accent = primaryAccent,
                            deepAccent = deepAccent,
                            pulseAlpha = pulseAlpha,
                            chipShape = chipShape,
                            onSelectMode = callbacks.onSelectCpuMode,
                            onBoostRam = callbacks.onBoostRam,
                            onToggleStopwatch = callbacks.onToggleStopwatch,
                            onResetStopwatch = callbacks.onResetStopwatch,
                            compactPortrait = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        StyleTabStrip(
                            tabs = listOf("GAMING TOOLS", "SYSTEM", "QUICK CONTROL"),
                            selectedIndex = if (quickControlOpen) 2 else portraitTab,
                            accent = primaryAccent,
                            shape = chipShape,
                            onSelect = { idx ->
                                if (idx == 2) {
                                    quickControlOpen = true
                                } else {
                                    quickControlOpen = false
                                    portraitTab = idx
                                }
                            },
                            containerColor = Color(0xFF0E1424)
                        )

                        AnimatedContent(
                            targetState = if (quickControlOpen) 2 else portraitTab,
                            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
                            label = "rog_portrait_tab",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) { tab ->
                            when (tab) {
                                0 -> {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(4),
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(bottom = 2.dp),
                                        verticalArrangement = Arrangement.spacedBy(5.dp),
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        items(leftTools, key = { it.id }) { tool ->
                                            RogCircularWingTile(
                                                item = tool.toUnifiedTool(),
                                                accent = primaryAccent,
                                                inactiveSurface = tileSurface
                                            )
                                        }
                                    }
                                }
                                1 -> {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(3),
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(bottom = 2.dp),
                                        verticalArrangement = Arrangement.spacedBy(5.dp),
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        items(rightTiles, key = { it.id }) { tile ->
                                            RogCircularWingTile(
                                                item = tile.toUnifiedTool(),
                                                accent = primaryAccent,
                                                inactiveSurface = tileSurface
                                            )
                                        }
                                    }
                                }
                                else -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        RogQuickControlSlidersDeck(
                                            brightnessFraction = brightnessFraction,
                                            volumeFraction = volumeFraction,
                                            accent = primaryAccent,
                                            chipShape = chipShape,
                                            onBrightnessChange = callbacks.onBrightnessChange,
                                            onVolumeChange = callbacks.onVolumeChange
                                        )
                                        EsportsQuickAppBar(shape = chipShape, callbacks = callbacks)
                                    }
                                }
                            }
                        }

                        RogBottomControlBar(
                            isWideLandscape = false,
                            quickControlOpen = quickControlOpen,
                            crosshairStyle = settings.crosshairStyle,
                            frameRateTarget = settings.frameRateTarget,
                            gpuBoostActive = settings.increaseGpuPerformance || settings.turboXMode,
                            accent = primaryAccent,
                            chipShape = chipShape,
                            onToggleQuickControl = { quickControlOpen = !quickControlOpen },
                            onCycleCrosshair = callbacks.onCycleCrosshair,
                            onCycleGpuPerf = callbacks.onCycleFrameRate,
                            callbacks = callbacks
                        )
                    }
                }
            }
        }
    }
}

private data class UnifiedWingTile(
    val id: String,
    val label: String,
    val icon: ImageVector?,
    val badgeText: String?,
    val isActive: Boolean,
    val onClick: () -> Unit
)

private fun WingToolItem.toUnifiedTool() = UnifiedWingTile(
    id = id,
    label = label,
    icon = icon,
    badgeText = null,
    isActive = isActive,
    onClick = onClick
)

private fun RightTileItem.toUnifiedTool() = UnifiedWingTile(
    id = id,
    label = label,
    icon = icon,
    badgeText = customBadgeText,
    isActive = isActive,
    onClick = onClick
)

@Composable
private fun RogTopPerformanceHudBar(
    isWideLandscape: Boolean,
    telemetry: HardwareTelemetry,
    cpuDisplayValue: String,
    cpuDisplayUnit: String,
    accent: Color,
    deepAccent: Color,
    pulseAlpha: Float,
    onPingClick: () -> Unit,
    onCycleRefreshClick: () -> Unit,
    onCollapseClick: () -> Unit
) {
    val crownShape = CutCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
    val tempColor = if (telemetry.batteryTempCelsius >= 41f) CyberAmber else EmeraldBright
    val netText = telemetry.pingMs?.let { "${it}ms" } ?: telemetry.networkType

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Floating Angular Telemetry Crown
        Row(
            modifier = Modifier
                .clip(crownShape)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xE80A0D18),
                            Color(0xF513182A),
                            Color(0xE80A0D18)
                        )
                    )
                )
                .border(
                    width = 1.2.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(deepAccent, accent.copy(alpha = 0.9f * pulseAlpha), deepAccent)
                    ),
                    shape = crownShape
                )
                .padding(horizontal = if (isWideLandscape) 10.dp else 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isWideLandscape) 9.dp else 6.dp)
        ) {
            RogHudStatChip(
                value = "${telemetry.actualFps}",
                unit = "FPS",
                valueColor = EmeraldBright,
                icon = Icons.Rounded.Speed
            )
            RogHudDivider()
            RogHudStatChip(
                value = String.format(Locale.US, "%.1f°", telemetry.batteryTempCelsius),
                unit = "C",
                valueColor = tempColor,
                icon = Icons.Rounded.Thermostat
            )
            RogHudDivider()
            RogHudStatChip(
                value = netText,
                unit = if (telemetry.pingMs != null) "NET" else "",
                valueColor = ElectricCyan,
                icon = Icons.Rounded.NetworkCheck,
                onClick = onPingClick
            )
            RogHudDivider()
            RogHudStatChip(
                value = cpuDisplayValue,
                unit = cpuDisplayUnit,
                valueColor = TextPrimary,
                icon = Icons.Rounded.Memory
            )
            if (isWideLandscape) {
                RogHudDivider()
                RogHudStatChip(
                    value = "${telemetry.ramUsedPercent}%",
                    unit = "RAM",
                    valueColor = accent,
                    icon = null
                )
                RogHudDivider()
                RogHudStatChip(
                    value = "${telemetry.batteryPercent}%",
                    unit = if (telemetry.isCharging) "CHG" else "BAT",
                    valueColor = if (telemetry.batteryPercent <= 20) CyberAmber else EmeraldBright,
                    icon = if (telemetry.isCharging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryStd
                )
                RogHudDivider()
                RogHudStatChip(
                    value = "${telemetry.displayRefreshRateHz}",
                    unit = "HZ",
                    valueColor = Color.White,
                    icon = null,
                    onClick = onCycleRefreshClick
                )
            }
            Spacer(modifier = Modifier.width(2.dp))
            // Collapse/Hide Handle matching ASUS Game Genie pull-down chevron tab
            Row(
                modifier = Modifier
                    .testTag("overlay_g_handle")
                    .clip(CutCornerShape(5.dp))
                    .background(accent.copy(alpha = 0.20f))
                    .border(1.dp, accent.copy(alpha = 0.85f), CutCornerShape(5.dp))
                    .clickable { onCollapseClick() }
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Hide game panel",
                    tint = accent,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "HIDE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.5.sp,
                        letterSpacing = 0.6.sp
                    ),
                    color = Color.White,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun RogHudDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(12.dp)
            .background(CyberBorder.copy(alpha = 0.75f))
    )
}

@Composable
private fun RogHudStatChip(
    value: String,
    unit: String,
    valueColor: Color,
    icon: ImageVector?,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = valueColor.copy(alpha = 0.9f),
                modifier = Modifier.size(11.dp)
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = RajdhaniFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp
            ),
            color = valueColor,
            maxLines = 1
        )
        if (unit.isNotEmpty()) {
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = TextMuted,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RogCenterXModePod(
    selectedMode: CpuMode,
    ramUsedPercent: Int,
    batteryPercent: Int,
    isCharging: Boolean,
    stopwatchSeconds: Long,
    isStopwatchRunning: Boolean,
    accent: Color,
    deepAccent: Color,
    pulseAlpha: Float,
    chipShape: Shape,
    onSelectMode: (CpuMode) -> Unit,
    onBoostRam: () -> Unit,
    onToggleStopwatch: () -> Unit,
    onResetStopwatch: () -> Unit,
    compactPortrait: Boolean = false,
    modifier: Modifier = Modifier
) {
    val activeModeColor = when (selectedMode) {
        CpuMode.ENERGY_SAVING -> ElectricCyan
        CpuMode.BALANCED -> CyberAmber
        CpuMode.ACCELERATION -> accent
    }
    val podShape = CutCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
    val mins = stopwatchSeconds / 60L
    val secs = stopwatchSeconds % 60L
    val clockFormatted = String.format(Locale.US, "%02d:%02d", mins, secs)

    Column(
        modifier = modifier
            .clip(podShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        activeModeColor.copy(alpha = 0.14f * pulseAlpha),
                        Color(0xE80A0E1B),
                        Color(0xF5070912)
                    )
                )
            )
            .border(
                width = 1.4.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        activeModeColor.copy(alpha = 0.95f),
                        deepAccent.copy(alpha = 0.7f),
                        CyberBorder.copy(alpha = 0.6f)
                    )
                ),
                shape = podShape
            )
            .padding(horizontal = if (compactPortrait) 10.dp else 7.dp, vertical = if (compactPortrait) 6.dp else 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Prominent X-MODE Selector Strip (ECO | BAL | X-MODE)
        Row(
            modifier = Modifier
                .testTag("overlay_cpu_mode_pill")
                .fillMaxWidth()
                .clip(chipShape)
                .background(Color(0xFF070A13))
                .border(1.dp, activeModeColor.copy(alpha = 0.45f), chipShape)
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            listOf(
                CpuMode.ENERGY_SAVING to "ECO",
                CpuMode.BALANCED to "BAL",
                CpuMode.ACCELERATION to "X-MODE"
            ).forEach { (mode, label) ->
                val isSelected = selectedMode == mode
                val mColor = when (mode) {
                    CpuMode.ENERGY_SAVING -> ElectricCyan
                    CpuMode.BALANCED -> CyberAmber
                    CpuMode.ACCELERATION -> accent
                }
                val bg by animateColorAsState(
                    targetValue = if (isSelected) mColor.copy(alpha = 0.30f) else Color.Transparent,
                    animationSpec = tween(220),
                    label = "xmode_bg_$label"
                )
                val border by animateColorAsState(
                    targetValue = if (isSelected) mColor else Color.Transparent,
                    animationSpec = tween(220),
                    label = "xmode_border_$label"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(if (compactPortrait) 24.dp else 21.dp)
                        .clip(chipShape)
                        .background(bg)
                        .border(1.dp, border, chipShape)
                        .clickable { onSelectMode(mode) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = if (compactPortrait) 11.sp else 10.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = if (isSelected) Color.White else TextSecondary,
                        maxLines = 1
                    )
                }
            }
        }

        // Session Stopwatch + Battery Bar Strip (preserves overlay_stopwatch_pill when active)
        Row(
            modifier = Modifier
                .then(
                    if (isStopwatchRunning || stopwatchSeconds > 0L) {
                        Modifier.testTag("overlay_stopwatch_pill")
                    } else {
                        Modifier
                    }
                )
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.clickable { onToggleStopwatch() }
            ) {
                Icon(
                    imageVector = if (isStopwatchRunning) Icons.Rounded.Pause else Icons.Rounded.Timer,
                    contentDescription = "Session timer",
                    tint = if (isStopwatchRunning) accent else TextSecondary,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = clockFormatted,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp
                    ),
                    color = if (isStopwatchRunning) TextPrimary else TextSecondary
                )
                if (stopwatchSeconds > 0L) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Reset stopwatch",
                        tint = TextMuted,
                        modifier = Modifier
                            .testTag("overlay_stopwatch_reset")
                            .size(12.dp)
                            .clickable { onResetStopwatch() }
                    )
                }
            }

            // Segmented green/amber battery indicator matching ROG Center Pod
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                val segs = 5
                val lit = ((batteryPercent / 100f) * segs).toInt().coerceIn(1, segs)
                val batColor = if (batteryPercent <= 20) CyberAmber else EmeraldBright
                Row(horizontalArrangement = Arrangement.spacedBy(1.5.dp)) {
                    for (i in 0 until segs) {
                        Box(
                            modifier = Modifier
                                .width(5.dp)
                                .height(6.dp)
                                .background(
                                    color = if (i < lit) batColor else Color(0xFF1F273B),
                                    shape = RoundedCornerShape(1.dp)
                                )
                        )
                    }
                }
                Text(
                    text = "$batteryPercent%" + if (isCharging) "+" else "",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    color = batColor
                )
            }
        }

        // TURBO X Brand Wordmark + Instant RAM BOOST Trigger
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) {
                        append("TURBO ")
                    }
                    withStyle(SpanStyle(color = accent, fontWeight = FontWeight.Bold)) {
                        append("X")
                    }
                },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = RajdhaniFontFamily,
                    fontSize = if (compactPortrait) 17.sp else 15.sp,
                    letterSpacing = 1.6.sp
                ),
                maxLines = 1
            )

            Row(
                modifier = Modifier
                    .testTag("overlay_ram_boost_pill")
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(accent, deepAccent)))
                    .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape)
                    .clickable { onBoostRam() }
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = "Boost RAM",
                    tint = Color.White,
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = "BOOST $ramUsedPercent%",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.5.sp
                    ),
                    color = Color.White,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun RogWingToolsBlock(
    title: String,
    pageLabel: String,
    items: List<UnifiedWingTile>,
    accent: Color,
    inactiveSurface: Color,
    onTogglePage: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = RajdhaniFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    letterSpacing = 0.8.sp
                ),
                color = TextMuted,
                maxLines = 1
            )
            Text(
                text = "$pageLabel ›",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = RajdhaniFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                ),
                color = accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(accent.copy(alpha = 0.14f))
                    .border(0.8.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(4.dp))
                    .clickable { onTogglePage() }
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            )
        }

        // Symmetrical 2 rows x 3 columns grid
        val rows = remember(items) { items.chunked(3) }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            rows.take(2).forEach { rowItems ->
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    rowItems.forEach { item ->
                        RogCircularWingTile(
                            item = item,
                            accent = accent,
                            inactiveSurface = inactiveSurface,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                    repeat(3 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun RogCircularWingTile(
    item: UnifiedWingTile,
    accent: Color,
    inactiveSurface: Color,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMedium),
        label = "rog_tile_scale_${item.id}"
    )
    val ringColor by animateColorAsState(
        targetValue = if (item.isActive) accent else CyberBorder.copy(alpha = 0.72f),
        animationSpec = tween(200),
        label = "rog_tile_ring_${item.id}"
    )
    val circleBg by animateColorAsState(
        targetValue = if (item.isActive) accent.copy(alpha = 0.26f) else inactiveSurface,
        animationSpec = tween(200),
        label = "rog_tile_bg_${item.id}"
    )

    Column(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(7.dp))
            .clickable(interactionSource = interactionSource, indication = null) { item.onClick() }
            .padding(horizontal = 2.dp, vertical = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(25.dp)
                .clip(CircleShape)
                .background(circleBg)
                .border(if (item.isActive) 1.3.dp else 1.dp, ringColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (item.badgeText != null) {
                Text(
                    text = item.badgeText.replace("Hz", ""),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    ),
                    color = if (item.isActive) accent else TextPrimary,
                    maxLines = 1
                )
            } else if (item.icon != null) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    tint = if (item.isActive) Color.White else TextSecondary,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                fontWeight = if (item.isActive) FontWeight.SemiBold else FontWeight.Normal,
                lineHeight = 9.sp
            ),
            color = if (item.isActive) Color.White else TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RogQuickControlSlidersDeck(
    brightnessFraction: Float,
    volumeFraction: Float,
    accent: Color,
    chipShape: Shape,
    onBrightnessChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(chipShape)
            .background(Color(0xCC0E1424))
            .border(1.dp, accent.copy(alpha = 0.45f), chipShape)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "QUICK DISPLAY & AUDIO",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = RajdhaniFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                letterSpacing = 0.8.sp
            ),
            color = accent
        )
        EsportsHorizontalSliderRow(
            icon = Icons.Rounded.WbSunny,
            label = "Brightness",
            value = brightnessFraction,
            accentColor = accent,
            onValueChange = onBrightnessChange
        )
        EsportsHorizontalSliderRow(
            icon = Icons.AutoMirrored.Rounded.VolumeUp,
            label = "Volume",
            value = volumeFraction,
            accentColor = ElectricCyan,
            onValueChange = onVolumeChange
        )
    }
}

@Composable
private fun RogQuickControlAppsAndTimerDeck(
    stopwatchSeconds: Long,
    isStopwatchRunning: Boolean,
    accent: Color,
    chipShape: Shape,
    callbacks: OverlayCallbacks
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(chipShape)
            .background(Color(0xCC0E1424))
            .border(1.dp, accent.copy(alpha = 0.45f), chipShape)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = "QUICK APPS & TIMER",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = RajdhaniFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                letterSpacing = 0.8.sp
            ),
            color = accent
        )
        EsportsQuickAppBar(shape = chipShape, callbacks = callbacks)
        val mins = stopwatchSeconds / 60L
        val secs = stopwatchSeconds % 60L
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(chipShape)
                .background(Color(0xFF0A0E19))
                .border(1.dp, CyberBorder.copy(alpha = 0.65f), chipShape)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TIMER ${String.format(Locale.US, "%02d:%02d", mins, secs)}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = RajdhaniFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = if (isStopwatchRunning) accent else TextPrimary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = if (isStopwatchRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isStopwatchRunning) "Pause stopwatch" else "Resume stopwatch",
                    tint = Color.White,
                    modifier = Modifier
                        .testTag("overlay_stopwatch_toggle")
                        .size(16.dp)
                        .clickable { callbacks.onToggleStopwatch() }
                )
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Reset stopwatch",
                    tint = TextSecondary,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { callbacks.onResetStopwatch() }
                )
            }
        }
    }
}

@Composable
private fun RogBottomControlBar(
    isWideLandscape: Boolean,
    quickControlOpen: Boolean,
    crosshairStyle: CrosshairStyle,
    frameRateTarget: Int,
    gpuBoostActive: Boolean,
    accent: Color,
    chipShape: Shape,
    onToggleQuickControl: () -> Unit,
    onCycleCrosshair: () -> Unit,
    onCycleGpuPerf: () -> Unit,
    callbacks: OverlayCallbacks
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(27.dp)
            .clip(CutCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
            .background(Color(0xEB070A13))
            .border(
                width = 0.9.dp,
                color = CyberBorder.copy(alpha = 0.65f),
                shape = CutCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
            )
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: TURBO X Brand Identity + Navigation Icons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isWideLandscape) 6.dp else 4.dp)
        ) {
            TurboXLogoMark(
                accentColor = accent,
                modifier = Modifier.size(if (isWideLandscape) 17.dp else 15.dp)
            )
            if (isWideLandscape) {
                Text(
                    text = "TURBO X",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    ),
                    color = TextPrimary
                )
            }
            OverlayActionIconRow(
                accentColor = accent,
                containerColor = Color(0xFF13192B),
                shape = chipShape,
                callbacks = callbacks,
                iconSizeDp = if (isWideLandscape) 21.dp else 19.dp
            )
        }

        // Right: Quick Control, Crosshair, GPU / Performance Chip (strictly real implemented features)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isWideLandscape) 5.dp else 4.dp)
        ) {
            RogBottomActionPill(
                label = if (isWideLandscape) "Quick Control" else "Quick",
                icon = Icons.Rounded.Tune,
                active = quickControlOpen,
                accent = accent,
                shape = chipShape,
                onClick = onToggleQuickControl
            )
            RogBottomActionPill(
                label = when {
                    crosshairStyle == CrosshairStyle.NONE -> if (isWideLandscape) "Crosshair" else "Aim"
                    isWideLandscape -> crosshairStyle.label
                    else -> "Aim:${crosshairStyle.label.take(4)}"
                },
                icon = Icons.Rounded.MyLocation,
                active = crosshairStyle != CrosshairStyle.NONE,
                accent = accent,
                shape = chipShape,
                onClick = onCycleCrosshair
            )
            RogBottomActionPill(
                label = if (isWideLandscape) "GPU ${frameRateTarget}Hz" else "${frameRateTarget}Hz",
                icon = Icons.Rounded.Speed,
                active = gpuBoostActive || frameRateTarget > 60,
                accent = accent,
                shape = chipShape,
                onClick = onCycleGpuPerf
            )
        }
    }
}

@Composable
private fun RogBottomActionPill(
    label: String,
    icon: ImageVector,
    active: Boolean,
    accent: Color,
    shape: Shape,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        targetValue = if (active) accent.copy(alpha = 0.24f) else Color(0xFF111728),
        animationSpec = tween(200),
        label = "bottom_pill_bg_$label"
    )
    val border by animateColorAsState(
        targetValue = if (active) accent else CyberBorder.copy(alpha = 0.7f),
        animationSpec = tween(200),
        label = "bottom_pill_border_$label"
    )
    Row(
        modifier = Modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) accent else TextSecondary,
            modifier = Modifier.size(11.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = RajdhaniFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 9.5.sp
            ),
            color = if (active) Color.White else TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
