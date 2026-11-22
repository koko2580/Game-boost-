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
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Thermostat
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CpuMode
import com.example.data.GameTurboSettings
import com.example.data.HardwareTelemetry
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.theme.TextMuted

// =====================================================================
// REAL REDMAGIC GAMESPACE — HALF-SCREEN CYBER MECHA DOCK
// Dual-neon crimson/cyan HUD + Spinning cooling fan + Dual Arc Dials
// =====================================================================
@Composable
internal fun RedMagicCyberOverlayContent(
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
    val neonRed = Color(0xFFFF1E42)
    val neonCyan = Color(0xFF00F0FF)
    val shellShape = CutCornerShape(14.dp)
    val tileShape = CutCornerShape(7.dp)
    var activeTab by remember { mutableIntStateOf(0) }

    val arcGlow = if (!settings.lowRamOverlayMode) {
        val inf = rememberInfiniteTransition(label = "rm_arc_pulse")
        val p by inf.animateFloat(
            initialValue = 0.68f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "rm_glow_alpha"
        )
        p
    } else {
        0.85f
    }

    val cpuModeColor by animateColorAsState(
        targetValue = when (settings.cpuMode) {
            CpuMode.ENERGY_SAVING -> neonCyan
            CpuMode.BALANCED -> Color(0xFFFFB300)
            CpuMode.ACCELERATION -> neonRed
        },
        animationSpec = tween(250),
        label = "rm_cpu_mode_color"
    )

    EsportsHalfDockShell(
        isWideLandscape = isWideLandscape,
        controlPanelLayout = settings.controlPanelLayout,
        lowRamOverlayMode = settings.lowRamOverlayMode,
        rootTag = "overlay_redmagic_root",
        onDismissToHandle = callbacks.onMinimizeClick,
        maxLandscapeWidthFraction = 0.49f,
        maxLandscapeWidthDp = 434.dp,
        maxPortraitHeightFraction = 0.64f
    ) { dockEdge, _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shellShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xF40B0712), Color(0xF6080D17), Color(0xF406101B))
                    )
                )
                .border(
                    width = 1.6.dp,
                    brush = Brush.horizontalGradient(listOf(neonRed, Color(0xFF283148), neonCyan)),
                    shape = shellShape
                )
        ) {
            // Subtle cyber reticle grid background accent
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridStep = 28.dp.toPx()
                var gx = gridStep
                while (gx < size.width) {
                    drawLine(neonCyan.copy(alpha = 0.04f), Offset(gx, 0f), Offset(gx, size.height), strokeWidth = 1f)
                    gx += gridStep
                }
                var gy = gridStep
                while (gy < size.height) {
                    drawLine(neonRed.copy(alpha = 0.04f), Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
                    gy += gridStep
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 9.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // Top GAMESPACE Cyber Header Bar + Spinning Fan + Effortless HIDE button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SpinningCoolingFanGlyph(
                            cpuMode = settings.cpuMode,
                            animate = !settings.lowRamOverlayMode,
                            primaryColor = neonRed,
                            secondaryColor = neonCyan,
                            sizeDp = 24.dp
                        )
                        Text(
                            text = "GAMESPACE",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = RajdhaniFontFamily,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.4.sp
                            ),
                            color = neonCyan,
                            maxLines = 1
                        )
                    }
                    OverlayActionIconRow(
                        accentColor = neonRed,
                        containerColor = Color(0xFF141826),
                        shape = tileShape,
                        callbacks = callbacks,
                        iconSizeDp = 25.dp
                    )
                    EsportsCollapseChip(
                        label = "${telemetry.actualFps}FPS",
                        dockEdge = dockEdge,
                        accent = neonCyan,
                        shape = tileShape,
                        onClick = callbacks.onMinimizeClick
                    )
                }

                if (statusMessage != null) {
                    OverlayStatusPill(statusMessage, neonCyan, tileShape)
                }
                if (isStopwatchRunning || stopwatchSeconds > 0L) {
                    EsportsStopwatchBar(
                        stopwatchSeconds = stopwatchSeconds,
                        isRunning = isStopwatchRunning,
                        accent = neonCyan,
                        shape = tileShape,
                        onToggle = callbacks.onToggleStopwatch,
                        onReset = callbacks.onResetStopwatch
                    )
                }

                // Dual Glowing Arc Dials + Mode & Boost Controls (compact mecha core)
                val dialSize = if (isWideLandscape) 64.dp else 78.dp
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(tileShape)
                        .background(Color(0x990D121F))
                        .border(1.dp, neonCyan.copy(alpha = 0.25f), tileShape)
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left CPU Arc Dial + Mode Selector
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RedMagicArcDial(
                            primaryValue = cpuDisplayValue,
                            unitLabel = cpuDisplayUnit,
                            badgeLabel = "CPU",
                            progress = (telemetry.cpuFreqMHz / 3200f).coerceIn(0.15f, 1f),
                            arcColor = cpuModeColor,
                            glowAlpha = arcGlow,
                            compact = true,
                            modifier = Modifier.size(dialSize)
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .testTag("overlay_cpu_mode_pill")
                                    .fillMaxWidth()
                                    .clip(tileShape)
                                    .background(cpuModeColor.copy(alpha = 0.16f))
                                    .border(1.dp, cpuModeColor.copy(alpha = 0.85f), tileShape)
                                    .clickable {
                                        val next = CpuMode.entries[(settings.cpuMode.ordinal + 1) % CpuMode.entries.size]
                                        callbacks.onSelectCpuMode(next)
                                    }
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (settings.cpuMode) {
                                        CpuMode.ENERGY_SAVING -> "ECO SHIELD"
                                        CpuMode.BALANCED -> "RISE MODE"
                                        CpuMode.ACCELERATION -> "DIABLO TURBO"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = RajdhaniFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = cpuModeColor,
                                    maxLines = 1
                                )
                            }
                            MiniTelemetryTag(
                                text = "${telemetry.batteryTempCelsius}°C FAN",
                                accent = neonRed,
                                shape = tileShape,
                                modifier = Modifier.fillMaxWidth(),
                                icon = Icons.Rounded.Thermostat
                            )
                        }
                    }

                    // Right GPU/Hz Arc Dial + Turbo Boost CTA
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RedMagicArcDial(
                            primaryValue = "${telemetry.displayRefreshRateHz}",
                            unitLabel = "Hz•${telemetry.actualFps}",
                            badgeLabel = "GPU",
                            progress = (telemetry.displayRefreshRateHz / 144f).coerceIn(0.35f, 1f),
                            arcColor = neonCyan,
                            glowAlpha = arcGlow,
                            compact = true,
                            modifier = Modifier.size(dialSize)
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .testTag("overlay_ram_boost_pill")
                                    .fillMaxWidth()
                                    .clip(tileShape)
                                    .background(neonCyan.copy(alpha = 0.16f))
                                    .border(1.dp, neonCyan.copy(alpha = 0.85f), tileShape)
                                    .clickable { callbacks.onBoostRam() }
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Icon(Icons.Rounded.Bolt, contentDescription = null, tint = neonCyan, modifier = Modifier.size(12.dp))
                                    Text(
                                        text = "TURBO ${telemetry.ramUsedPercent}%",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = RajdhaniFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        color = neonCyan,
                                        maxLines = 1
                                    )
                                }
                            }
                            MiniTelemetryTag(
                                text = telemetry.pingMs?.let { "${it}ms NET" } ?: "${telemetry.ramAvailableMb}MB",
                                accent = neonCyan,
                                shape = tileShape,
                                modifier = Modifier.fillMaxWidth(),
                                icon = Icons.Rounded.NetworkCheck
                            )
                        }
                    }
                }

                // 3 Crisp Cyber Tabs (PLUGINS | MATRIX | ENERGY FADER)
                StyleTabStrip(
                    tabs = listOf("PLUGINS", "MATRIX", "ENERGY FADER"),
                    selectedIndex = activeTab,
                    accent = if (activeTab == 0) neonRed else neonCyan,
                    shape = tileShape,
                    onSelect = { activeTab = it },
                    selectedTextColor = if (activeTab == 0) Color.White else Color(0xFF04101A),
                    containerColor = Color(0xFF111625)
                )

                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
                    label = "redmagic_tab_switch",
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
                                        activeAccent = neonRed,
                                        inactiveContainer = Color(0xFF151929),
                                        shape = tileShape,
                                        cardHeight = if (isWideLandscape) 41.dp else 48.dp,
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
                                        activeAccent = neonCyan,
                                        inactiveContainer = Color(0xFF131A29),
                                        shape = tileShape,
                                        cardHeight = if (isWideLandscape) 41.dp else 48.dp,
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
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(104.dp)
                                        .clip(tileShape)
                                        .background(Color(0xFF0E1523))
                                        .border(1.dp, neonCyan.copy(alpha = 0.45f), tileShape)
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    EsportsVerticalNeonSlider(
                                        icon = Icons.Rounded.WbSunny,
                                        label = "Brightness",
                                        value = brightnessFraction,
                                        accentColor = neonRed,
                                        onValueChange = callbacks.onBrightnessChange,
                                        modifier = Modifier.weight(1f)
                                    )
                                    EsportsVerticalNeonSlider(
                                        icon = Icons.AutoMirrored.Rounded.VolumeUp,
                                        label = "Volume",
                                        value = volumeFraction,
                                        accentColor = neonCyan,
                                        onValueChange = callbacks.onVolumeChange,
                                        modifier = Modifier.weight(1f)
                                    )
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

@Composable
private fun RedMagicArcDial(
    primaryValue: String,
    unitLabel: String,
    badgeLabel: String,
    progress: Float,
    arcColor: Color,
    glowAlpha: Float,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0.1f, 1f),
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessLow),
        label = "rm_arc_$badgeLabel"
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize().padding(if (compact) 2.dp else 4.dp)) {
            val strokeW = (if (compact) 5.dp else 6.5.dp).toPx()
            val pad = strokeW / 2f
            val arcSize = Size(size.width - strokeW, size.height - strokeW)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(arcColor.copy(alpha = 0.14f * glowAlpha), Color.Transparent)
                ),
                radius = size.minDimension * 0.48f
            )
            drawArc(
                color = Color(0xFF1E263A),
                startAngle = 140f,
                sweepAngle = 260f,
                useCenter = false,
                topLeft = Offset(pad, pad),
                size = arcSize,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(arcColor.copy(alpha = 0.55f), arcColor, Color.White.copy(alpha = 0.9f))),
                startAngle = 140f,
                sweepAngle = 260f * animatedProgress,
                useCenter = false,
                topLeft = Offset(pad, pad),
                size = arcSize,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = primaryValue,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = RajdhaniFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (compact) 14.sp else 18.sp
                ),
                color = Color.White,
                maxLines = 1
            )
            Text(
                text = unitLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = if (compact) 7.5.sp else 9.sp, fontWeight = FontWeight.Bold),
                color = TextMuted,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(1.dp))
            Box(
                modifier = Modifier
                    .clip(CutCornerShape(4.dp))
                    .background(arcColor.copy(alpha = 0.22f))
                    .border(1.dp, arcColor.copy(alpha = 0.7f), CutCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = badgeLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold, fontSize = 7.5.sp),
                    color = arcColor
                )
            }
        }
    }
}
