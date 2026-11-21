package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
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
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CpuMode
import com.example.data.GameTurboSettings
import com.example.data.HardwareTelemetry
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

// =====================================================================
// REAL ROG GAME GENIE OVERLAY STYLE
// Bottom-anchored angled crimson gunmetal console + X-MODE core
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
    callbacks: OverlayCallbacks
) {
    val rogCrimson = Color(0xFFFF1744)
    val rogDeepRed = Color(0xFFB71C1C)
    val rogGunmetal = Color(0xF612151E)
    val rogCardBg = Color(0xFF1A1F2B)
    val deckShape = CutCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
    val tileShape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp, topEnd = 3.dp, bottomStart = 3.dp)
    var activeTab by remember { mutableIntStateOf(0) }

    // Smooth bottom-up console dock entrance
    val slideUp = remember { Animatable(if (settings.lowRamOverlayMode) 1f else 0f) }
    LaunchedEffect(settings.lowRamOverlayMode) {
        if (settings.lowRamOverlayMode) {
            slideUp.snapTo(1f)
        } else {
            slideUp.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessMediumLow)
            )
        }
    }

    val beamSweep = if (!settings.lowRamOverlayMode) {
        val inf = rememberInfiniteTransition(label = "rog_genie_beam")
        val v by inf.animateFloat(
            initialValue = -0.25f,
            targetValue = 1.25f,
            animationSpec = infiniteRepeatable(
                animation = tween(2500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rog_beam_x"
        )
        v
    } else {
        0.5f
    }

    Box(
        modifier = Modifier
            .testTag("overlay_rog_root")
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = if (isWideLandscape) 16.dp else 10.dp, vertical = 8.dp),
        contentAlignment = if (isWideLandscape) Alignment.BottomCenter else Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = if (isWideLandscape) 860.dp else 440.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    val t = slideUp.value
                    translationY = 84f * (1f - t)
                    alpha = t
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Sloped Top Hardware Telemetry Bevel Pill (Real ROG top center bar)
            Row(
                modifier = Modifier
                    .clip(CutCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(Color(0xF4161A26))
                    .border(1.2.dp, Brush.horizontalGradient(listOf(rogCrimson, Color(0xFF475569), rogCrimson)), CutCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .testTag("overlay_ram_boost_pill")
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(rogCrimson, rogDeepRed)))
                        .clickable { callbacks.onBoostRam() }
                        .padding(horizontal = 9.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Rounded.Bolt, contentDescription = "Boost", tint = Color.White, modifier = Modifier.size(13.dp))
                        Text(
                            text = "BOOST",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }

                RogTelemetryChip("${telemetry.actualFps}", "FPS", rogCrimson)
                Text("/", color = Color(0xFF475569), fontWeight = FontWeight.Bold)
                RogTelemetryChip("${telemetry.batteryTempCelsius}°", "C", CyberAmber)
                Text("/", color = Color(0xFF475569), fontWeight = FontWeight.Bold)
                RogTelemetryChip("${telemetry.pingMs ?: 18}", "MS", ElectricCyan)
                Text("/", color = Color(0xFF475569), fontWeight = FontWeight.Bold)
                RogTelemetryChip(cpuDisplayValue, cpuDisplayUnit, TextPrimary)
            }

            // Main GAME GENIE Angled Wing Console
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (isWideLandscape) Modifier.height(238.dp) else Modifier.fillMaxHeight(0.76f))
                    .clip(deckShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(rogGunmetal, Color(0xF80B0D13))
                        )
                    )
                    .border(
                        width = 1.6.dp,
                        brush = Brush.horizontalGradient(listOf(rogCrimson, Color(0xFF334155), rogCrimson)),
                        shape = deckShape
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Animated ROG Red Energy Horizon Line
                Box(modifier = Modifier.fillMaxWidth()) {
                    Canvas(modifier = Modifier.fillMaxWidth().height(2.dp)) {
                        val x = size.width * beamSweep
                        drawLine(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, rogCrimson, Color(0xFFFF8A80), rogCrimson, Color.Transparent),
                                startX = x - 110f,
                                endX = x + 110f
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }

                if (statusMessage != null) {
                    OverlayStatusPill(statusMessage, rogCrimson, tileShape)
                }

                if (isWideLandscape) {
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Wing Matrix (Game Tools)
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.weight(0.35f).fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(leftTools, key = { it.id }) { tool ->
                                EsportsToolCard(
                                    label = tool.label,
                                    icon = tool.icon,
                                    badgeText = null,
                                    active = tool.isActive,
                                    activeAccent = rogCrimson,
                                    inactiveContainer = rogCardBg,
                                    shape = tileShape,
                                    cardHeight = 46.dp,
                                    onClick = tool.onClick
                                )
                            }
                        }

                        // Center Signature GAME GENIE Core (X-MODE + Battery + Sliders/Action)
                        Column(
                            modifier = Modifier
                                .weight(0.30f)
                                .fillMaxHeight()
                                .clip(tileShape)
                                .background(Color(0xFF141722))
                                .border(1.dp, rogCrimson.copy(alpha = 0.5f), tileShape)
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            RogModeSegmentedStrip(
                                selectedMode = settings.cpuMode,
                                rogCrimson = rogCrimson,
                                shape = tileShape,
                                onSelect = callbacks.onSelectCpuMode,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Battery + Clock + RAM Mini Readout
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (isStopwatchRunning || stopwatchSeconds > 0) "⏱ ${stopwatchSeconds}s" else "BAT ${telemetry.batteryPercent}%",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold),
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "RAM ${telemetry.ramUsedPercent}%",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold),
                                        color = ElectricCyan
                                    )
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    val filled = ((telemetry.batteryPercent / 100f) * 10).toInt().coerceIn(1, 10)
                                    for (s in 0 until 10) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(4.dp)
                                                .background(if (s < filled) rogCrimson else Color(0xFF282E3F))
                                        )
                                    }
                                }
                            }

                            // Iconic GAME GENIE Emblem + Minimize pill
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "/// GAME GENIE",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = RajdhaniFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = Color.White,
                                    maxLines = 1
                                )
                                Box(
                                    modifier = Modifier
                                        .testTag("overlay_g_handle")
                                        .clip(tileShape)
                                        .background(rogCrimson)
                                        .clickable { callbacks.onMinimizeClick() }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "HIDE",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                            }

                            OverlayActionIconRow(
                                accentColor = rogCrimson,
                                containerColor = Color(0xFF1D2230),
                                shape = tileShape,
                                callbacks = callbacks
                            )
                        }

                        // Right Wing Matrix (System Tiles / Quick Sliders Switch)
                        Column(
                            modifier = Modifier.weight(0.35f).fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            StyleTabStrip(
                                tabs = listOf("SYSTEM", "AUDIO & APPS"),
                                selectedIndex = activeTab.coerceIn(0, 1),
                                accent = rogCrimson,
                                shape = tileShape,
                                onSelect = { activeTab = it },
                                containerColor = rogCardBg
                            )
                            AnimatedContent(
                                targetState = activeTab.coerceIn(0, 1),
                                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(150)) },
                                label = "rog_right_switch",
                                modifier = Modifier.weight(1f)
                            ) { tab ->
                                if (tab == 0) {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(3),
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(rightTiles, key = { it.id }) { tile ->
                                            EsportsToolCard(
                                                label = tile.label,
                                                icon = tile.icon,
                                                badgeText = tile.customBadgeText,
                                                active = tile.isActive,
                                                activeAccent = rogCrimson,
                                                inactiveContainer = rogCardBg,
                                                shape = tileShape,
                                                cardHeight = 46.dp,
                                                onClick = tile.onClick
                                            )
                                        }
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        EsportsHorizontalSliderRow(Icons.Rounded.WbSunny, "Brightness", brightnessFraction, rogCrimson, callbacks.onBrightnessChange)
                                        EsportsHorizontalSliderRow(Icons.AutoMirrored.Rounded.VolumeUp, "Volume", volumeFraction, ElectricCyan, callbacks.onVolumeChange)
                                        EsportsQuickAppBar(shape = tileShape, callbacks = callbacks)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Portrait Real ROG Game Genie Console
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OverlayActionIconRow(rogCrimson, rogCardBg, tileShape, callbacks)
                        Box(
                            modifier = Modifier
                                .testTag("overlay_g_handle")
                                .clip(tileShape)
                                .background(rogCrimson)
                                .clickable { callbacks.onMinimizeClick() }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text("GAME GENIE", style = MaterialTheme.typography.labelLarge.copy(fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold), color = Color.White)
                        }
                    }
                    RogModeSegmentedStrip(
                        selectedMode = settings.cpuMode,
                        rogCrimson = rogCrimson,
                        shape = tileShape,
                        onSelect = callbacks.onSelectCpuMode,
                        modifier = Modifier.fillMaxWidth()
                    )
                    StyleTabStrip(
                        tabs = listOf("GENIE", "SYSTEM", "AUDIO & APPS"),
                        selectedIndex = activeTab,
                        accent = rogCrimson,
                        shape = tileShape,
                        onSelect = { activeTab = it },
                        containerColor = rogCardBg
                    )
                    if (activeTab < 2) {
                        AnimatedStyleGrid(
                            tabIndex = activeTab,
                            leftTools = leftTools,
                            rightTiles = rightTiles,
                            accent = rogCrimson,
                            inactiveCard = rogCardBg,
                            shape = tileShape,
                            columns = 3,
                            cardHeight = 54.dp,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Column(
                            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            EsportsHorizontalSliderRow(Icons.Rounded.WbSunny, "Brightness", brightnessFraction, rogCrimson, callbacks.onBrightnessChange)
                            EsportsHorizontalSliderRow(Icons.AutoMirrored.Rounded.VolumeUp, "Volume", volumeFraction, ElectricCyan, callbacks.onVolumeChange)
                            EsportsQuickAppBar(shape = tileShape, callbacks = callbacks)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RogTelemetryChip(
    value: String,
    unit: String,
    color: Color
) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold),
            color = color
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
            color = TextMuted,
            modifier = Modifier.padding(bottom = 1.dp)
        )
    }
}

@Composable
private fun RogModeSegmentedStrip(
    selectedMode: CpuMode,
    rogCrimson: Color,
    shape: Shape,
    onSelect: (CpuMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .testTag("overlay_cpu_mode_pill")
            .clip(shape)
            .background(Color(0xFF0E1018))
            .border(1.dp, CyberBorder.copy(alpha = 0.75f), shape)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        listOf(
            CpuMode.ENERGY_SAVING to "ECO",
            CpuMode.BALANCED to "BAL",
            CpuMode.ACCELERATION to "X-MODE"
        ).forEach { (mode, label) ->
            val selected = selectedMode == mode
            val modeColor = when (mode) {
                CpuMode.ENERGY_SAVING -> ElectricCyan
                CpuMode.BALANCED -> CyberAmber
                CpuMode.ACCELERATION -> rogCrimson
            }
            val bg by animateColorAsState(
                targetValue = if (selected) modeColor.copy(alpha = 0.28f) else Color.Transparent,
                animationSpec = tween(220),
                label = "rog_mode_bg_$label"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp)
                    .clip(shape)
                    .background(bg)
                    .border(1.dp, if (selected) modeColor else Color.Transparent, shape)
                    .clickable { onSelect(mode) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    ),
                    color = if (selected) modeColor else TextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}
