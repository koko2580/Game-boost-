package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
// REAL ROG GAME GENIE — HALF-SCREEN TACTICAL HUD DOCK
// Compact side-docked crimson gunmetal console + X-MODE + zero-clipping grid
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
    val rogCardBg = Color(0xFF181C28)
    val deckShape = CutCornerShape(topStart = 18.dp, bottomEnd = 18.dp, topEnd = 8.dp, bottomStart = 8.dp)
    val tileShape = CutCornerShape(topStart = 7.dp, bottomEnd = 7.dp, topEnd = 2.dp, bottomStart = 2.dp)
    var activeTab by remember { mutableIntStateOf(0) }

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

    EsportsHalfDockShell(
        isWideLandscape = isWideLandscape,
        controlPanelLayout = settings.controlPanelLayout,
        lowRamOverlayMode = settings.lowRamOverlayMode,
        rootTag = "overlay_rog_root",
        onDismissToHandle = callbacks.onMinimizeClick,
        maxLandscapeWidthFraction = 0.49f,
        maxLandscapeWidthDp = 432.dp,
        maxPortraitHeightFraction = 0.62f
    ) { dockEdge, _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(deckShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xF6131722), Color(0xFA0A0C13))
                    )
                )
                .border(
                    width = 1.6.dp,
                    brush = Brush.linearGradient(listOf(rogCrimson, Color(0xFF334155), rogCrimson)),
                    shape = deckShape
                )
        ) {
            // Subtle tactical carbon slash lines behind header for ROG Armoury identity
            Canvas(modifier = Modifier.fillMaxWidth().height(54.dp)) {
                val step = 14.dp.toPx()
                var x = -20f
                while (x < size.width + 20f) {
                    drawLine(
                        color = rogCrimson.copy(alpha = 0.08f),
                        start = Offset(x, size.height),
                        end = Offset(x + 24.dp.toPx(), 0f),
                        strokeWidth = 3.dp.toPx()
                    )
                    x += step
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Header: Emblem + Action Icons + Effortless Collapse Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isWideLandscape) "/// GAME GENIE" else "GAME GENIE",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp
                        ),
                        color = Color.White,
                        maxLines = 1
                    )
                    OverlayActionIconRow(
                        accentColor = rogCrimson,
                        containerColor = Color(0xFF1C2130),
                        shape = tileShape,
                        callbacks = callbacks,
                        iconSizeDp = 26.dp
                    )
                    EsportsCollapseChip(
                        label = "HIDE",
                        dockEdge = dockEdge,
                        accent = rogCrimson,
                        shape = tileShape,
                        onClick = callbacks.onMinimizeClick
                    )
                }

                // Sweeping Crimson Plasma Beam
                Canvas(modifier = Modifier.fillMaxWidth().height(2.dp)) {
                    val x = size.width * beamSweep
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, rogCrimson, Color(0xFFFF8A80), rogCrimson, Color.Transparent),
                            startX = x - 90f,
                            endX = x + 90f
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 2.dp.toPx()
                    )
                }

                // Compact Telemetry HUD Strip + Instant BOOST CTA
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(tileShape)
                        .background(Color(0xFF141824))
                        .border(1.dp, rogCrimson.copy(alpha = 0.42f), tileShape)
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RogTelemetryChip("${telemetry.actualFps}", "FPS", rogCrimson)
                        RogTelemetryChip("${telemetry.batteryTempCelsius}°", "C", CyberAmber)
                        RogTelemetryChip("${telemetry.pingMs ?: 18}", "MS", ElectricCyan)
                        RogTelemetryChip(cpuDisplayValue, cpuDisplayUnit, TextPrimary)
                    }
                    Box(
                        modifier = Modifier
                            .testTag("overlay_ram_boost_pill")
                            .clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(rogCrimson, rogDeepRed)))
                            .clickable { callbacks.onBoostRam() }
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.Rounded.Bolt, contentDescription = "Boost RAM", tint = Color.White, modifier = Modifier.size(12.dp))
                            Text(
                                text = "BOOST ${telemetry.ramUsedPercent}%",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                color = Color.White
                            )
                        }
                    }
                }

                // Tactical Mode Selector (ECO | BAL | X-MODE)
                RogModeSegmentedStrip(
                    selectedMode = settings.cpuMode,
                    rogCrimson = rogCrimson,
                    shape = tileShape,
                    onSelect = callbacks.onSelectCpuMode,
                    modifier = Modifier.fillMaxWidth()
                )

                if (statusMessage != null) {
                    OverlayStatusPill(statusMessage, rogCrimson, tileShape)
                }
                if (isStopwatchRunning || stopwatchSeconds > 0L) {
                    EsportsStopwatchBar(
                        stopwatchSeconds = stopwatchSeconds,
                        isRunning = isStopwatchRunning,
                        accent = rogCrimson,
                        shape = tileShape,
                        onToggle = callbacks.onToggleStopwatch,
                        onReset = callbacks.onResetStopwatch
                    )
                }

                // 3 Crisp Tabs: Game Tools | System | Audio & Apps
                StyleTabStrip(
                    tabs = listOf("GENIE TOOLS", "SYSTEM", "AUDIO & APPS"),
                    selectedIndex = activeTab,
                    accent = rogCrimson,
                    shape = tileShape,
                    onSelect = { activeTab = it },
                    containerColor = rogCardBg
                )

                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
                    label = "rog_half_dock_tab",
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
                                        activeAccent = rogCrimson,
                                        inactiveContainer = rogCardBg,
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
                                        activeAccent = rogCrimson,
                                        inactiveContainer = rogCardBg,
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
                                        .background(rogCardBg)
                                        .border(1.dp, CyberBorder.copy(alpha = 0.5f), tileShape)
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    EsportsHorizontalSliderRow(Icons.Rounded.WbSunny, "Brightness", brightnessFraction, rogCrimson, callbacks.onBrightnessChange)
                                    EsportsHorizontalSliderRow(Icons.AutoMirrored.Rounded.VolumeUp, "Volume", volumeFraction, ElectricCyan, callbacks.onVolumeChange)
                                }
                                EsportsQuickAppBar(shape = tileShape, callbacks = callbacks)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("BAT ${telemetry.batteryPercent}%", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    Text("${telemetry.ramAvailableMb}MB FREE", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
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
private fun RogTelemetryChip(
    value: String,
    unit: String,
    color: Color
) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp),
            color = color
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold),
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
            .padding(2.dp),
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
                    .height(25.dp)
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
                        letterSpacing = 0.6.sp,
                        fontSize = 11.sp
                    ),
                    color = if (selected) modeColor else TextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}
