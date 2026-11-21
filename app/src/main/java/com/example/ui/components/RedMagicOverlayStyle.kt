package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CpuMode
import com.example.data.GameTurboSettings
import com.example.data.HardwareTelemetry
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

// =====================================================================
// REAL REDMAGIC GAMESPACE OVERLAY STYLE
// Left Plugins Dock + Center Dual Neon CPU/GPU Arc Dials + Right Energy Fader
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
    statusMessage: String?,
    leftTools: List<WingToolItem>,
    rightTiles: List<RightTileItem>,
    callbacks: OverlayCallbacks
) {
    val neonRed = Color(0xFFFF1E42)
    val neonCyan = Color(0xFF00F0FF)
    val shellShape = CutCornerShape(14.dp)
    val tileShape = CutCornerShape(8.dp)
    var activeTab by remember { mutableIntStateOf(0) }

    // Smooth dual-wing split cyber entrance
    val entrance = remember { Animatable(if (settings.lowRamOverlayMode) 1f else 0f) }
    LaunchedEffect(settings.lowRamOverlayMode) {
        if (settings.lowRamOverlayMode) {
            entrance.snapTo(1f)
        } else {
            entrance.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessMediumLow)
            )
        }
    }

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

    Box(
        modifier = Modifier
            .testTag("overlay_redmagic_root")
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = if (isWideLandscape) 12.dp else 10.dp, vertical = if (isWideLandscape) 4.dp else 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = if (isWideLandscape) 900.dp else 440.dp)
                .fillMaxWidth()
                .then(if (isWideLandscape) Modifier.fillMaxHeight(0.98f) else Modifier.fillMaxHeight(0.86f))
                .clip(shellShape)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xF00B0710), Color(0xDC080C15), Color(0xF0070E17))
                    )
                )
                .border(
                    width = 1.6.dp,
                    brush = Brush.horizontalGradient(listOf(neonRed, Color(0xFF283148), neonCyan)),
                    shape = shellShape
                )
                .padding(horizontal = 10.dp, vertical = if (isWideLandscape) 7.dp else 10.dp),
            verticalArrangement = Arrangement.spacedBy(if (isWideLandscape) 5.dp else 8.dp)
        ) {
            // Top GAMESPACE Cyber Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OverlayActionIconRow(
                    accentColor = neonRed,
                    containerColor = Color(0xFF141826),
                    shape = tileShape,
                    callbacks = callbacks
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "GAMESPACE",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.8.sp
                        ),
                        color = neonCyan,
                        maxLines = 1
                    )
                    SpinningCoolingFanGlyph(
                        cpuMode = settings.cpuMode,
                        animate = !settings.lowRamOverlayMode,
                        primaryColor = neonRed,
                        secondaryColor = neonCyan,
                        sizeDp = 30.dp
                    )
                    Box(
                        modifier = Modifier
                            .testTag("overlay_g_handle")
                            .clip(tileShape)
                            .background(Color(0xFF141B2B))
                            .border(1.2.dp, Brush.horizontalGradient(listOf(neonRed, neonCyan)), tileShape)
                            .clickable { callbacks.onMinimizeClick() }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "REDMAGIC • ${telemetry.actualFps} FPS",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = RajdhaniFontFamily,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.9.sp
                            ),
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }
            }

            if (statusMessage != null) {
                OverlayStatusPill(statusMessage, neonCyan, tileShape)
            }

            if (isWideLandscape) {
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Left Side Plugins Rail
                    Column(
                        modifier = Modifier
                            .weight(0.28f)
                            .fillMaxHeight()
                            .graphicsLayer {
                                val t = entrance.value
                                translationX = -56f * (1f - t)
                                alpha = t
                            }
                            .clip(tileShape)
                            .background(Color(0xFF110D18))
                            .border(1.dp, neonRed.copy(alpha = 0.5f), tileShape)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "ESPORTS PLUGINS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = RajdhaniFontFamily,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = neonRed
                        )
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(leftTools, key = { it.id }) { tool ->
                                EsportsToolCard(
                                    label = tool.label,
                                    icon = tool.icon,
                                    badgeText = null,
                                    active = tool.isActive,
                                    activeAccent = neonRed,
                                    inactiveContainer = Color(0xFF171A29),
                                    shape = tileShape,
                                    cardHeight = 46.dp,
                                    onClick = tool.onClick
                                )
                            }
                        }
                    }

                    // Center Dual-Arc Mecha Gauge Showcase
                    Column(
                        modifier = Modifier
                            .weight(0.42f)
                            .fillMaxHeight()
                            .graphicsLayer {
                                val t = entrance.value
                                val s = 0.92f + 0.08f * t
                                scaleX = s
                                scaleY = s
                                alpha = t
                            },
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left CPU Glowing Arc Dial + Mode Selector
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                RedMagicArcDial(
                                    primaryValue = cpuDisplayValue,
                                    unitLabel = cpuDisplayUnit,
                                    badgeLabel = "CPU",
                                    progress = (telemetry.cpuFreqMHz / 3200f).coerceIn(0.15f, 1f),
                                    arcColor = cpuModeColor,
                                    glowAlpha = arcGlow,
                                    compact = true,
                                    modifier = Modifier.size(80.dp)
                                )
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
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = when (settings.cpuMode) {
                                            CpuMode.ENERGY_SAVING -> "ECO SHIELD"
                                            CpuMode.BALANCED -> "RISE MODE"
                                            CpuMode.ACCELERATION -> "DIABLO TURBO"
                                        },
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = RajdhaniFontFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = cpuModeColor,
                                        maxLines = 1
                                    )
                                }
                            }

                            // Right GPU / RAM Glowing Arc Dial + Boost CTA
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                RedMagicArcDial(
                                    primaryValue = "${telemetry.displayRefreshRateHz}",
                                    unitLabel = "Hz • ${telemetry.actualFps}FPS",
                                    badgeLabel = "GPU/HZ",
                                    progress = (telemetry.displayRefreshRateHz / 144f).coerceIn(0.35f, 1f),
                                    arcColor = neonCyan,
                                    glowAlpha = arcGlow,
                                    compact = true,
                                    modifier = Modifier.size(80.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .testTag("overlay_ram_boost_pill")
                                        .fillMaxWidth()
                                        .clip(tileShape)
                                        .background(neonCyan.copy(alpha = 0.15f))
                                        .border(1.dp, neonCyan.copy(alpha = 0.85f), tileShape)
                                        .clickable { callbacks.onBoostRam() }
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Icon(Icons.Rounded.Bolt, contentDescription = null, tint = neonCyan, modifier = Modifier.size(14.dp))
                                        Text(
                                            text = "TURBO ${telemetry.ramUsedPercent}%",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontFamily = RajdhaniFontFamily,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = neonCyan,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MiniTelemetryTag("${telemetry.batteryTempCelsius}°C FAN", neonRed, tileShape, Modifier.weight(1f), Icons.Rounded.Thermostat)
                            MiniTelemetryTag(
                                telemetry.pingMs?.let { "${it}ms NET" } ?: "${telemetry.ramAvailableMb}MB FREE",
                                neonCyan,
                                tileShape,
                                Modifier.weight(1f),
                                Icons.Rounded.NetworkCheck
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        EsportsQuickAppBar(shape = tileShape, callbacks = callbacks)
                    }

                    // Right Side System Matrix + Vertical Energy Faders
                    Row(
                        modifier = Modifier
                            .weight(0.30f)
                            .fillMaxHeight()
                            .graphicsLayer {
                                val t = entrance.value
                                translationX = 56f * (1f - t)
                                alpha = t
                            }
                            .clip(tileShape)
                            .background(Color(0xFF0C1420))
                            .border(1.dp, neonCyan.copy(alpha = 0.5f), tileShape)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(0.62f).fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "MATRIX",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = RajdhaniFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = neonCyan
                            )
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(1),
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(rightTiles, key = { it.id }) { tile ->
                                    EsportsToolCard(
                                        label = tile.label,
                                        icon = tile.icon,
                                        badgeText = tile.customBadgeText,
                                        active = tile.isActive,
                                        activeAccent = neonCyan,
                                        inactiveContainer = Color(0xFF151C2B),
                                        shape = tileShape,
                                        cardHeight = 44.dp,
                                        onClick = tile.onClick
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.weight(0.38f).fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "ENERGY FADER",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = RajdhaniFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.sp
                                ),
                                color = neonCyan,
                                maxLines = 1
                            )
                            EsportsVerticalNeonSlider(
                                icon = Icons.Rounded.WbSunny,
                                label = "Brightness",
                                value = brightnessFraction,
                                accentColor = neonRed,
                                onValueChange = callbacks.onBrightnessChange,
                                modifier = Modifier.weight(1f).fillMaxWidth()
                            )
                            EsportsVerticalNeonSlider(
                                icon = Icons.AutoMirrored.Rounded.VolumeUp,
                                label = "Volume",
                                value = volumeFraction,
                                accentColor = neonCyan,
                                onValueChange = callbacks.onVolumeChange,
                                modifier = Modifier.weight(1f).fillMaxWidth()
                            )
                        }
                    }
                }
            } else {
                // Portrait RedMagic Dual-Arc + Tabs Layout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        RedMagicArcDial(
                            primaryValue = cpuDisplayValue,
                            unitLabel = cpuDisplayUnit,
                            badgeLabel = "CPU",
                            progress = (telemetry.cpuFreqMHz / 3200f).coerceIn(0.15f, 1f),
                            arcColor = cpuModeColor,
                            glowAlpha = arcGlow,
                            modifier = Modifier.size(92.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .testTag("overlay_cpu_mode_pill")
                                .fillMaxWidth()
                                .clip(tileShape)
                                .background(cpuModeColor.copy(alpha = 0.16f))
                                .border(1.dp, cpuModeColor, tileShape)
                                .clickable {
                                    val next = CpuMode.entries[(settings.cpuMode.ordinal + 1) % CpuMode.entries.size]
                                    callbacks.onSelectCpuMode(next)
                                }
                                .padding(vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (settings.cpuMode) {
                                    CpuMode.ENERGY_SAVING -> "ECO SHIELD"
                                    CpuMode.BALANCED -> "RISE MODE"
                                    CpuMode.ACCELERATION -> "DIABLO TURBO"
                                },
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold),
                                color = cpuModeColor
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        RedMagicArcDial(
                            primaryValue = "${telemetry.displayRefreshRateHz}",
                            unitLabel = "Hz • ${telemetry.actualFps}FPS",
                            badgeLabel = "GPU/HZ",
                            progress = (telemetry.displayRefreshRateHz / 144f).coerceIn(0.35f, 1f),
                            arcColor = neonCyan,
                            glowAlpha = arcGlow,
                            modifier = Modifier.size(92.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .testTag("overlay_ram_boost_pill")
                                .fillMaxWidth()
                                .clip(tileShape)
                                .background(neonCyan.copy(alpha = 0.15f))
                                .border(1.dp, neonCyan, tileShape)
                                .clickable { callbacks.onBoostRam() }
                                .padding(vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "TURBO ${telemetry.ramUsedPercent}%",
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold),
                                color = neonCyan
                            )
                        }
                    }
                }

                StyleTabStrip(
                    tabs = listOf("PLUGINS", "MATRIX", "ENERGY FADER"),
                    selectedIndex = activeTab,
                    accent = neonCyan,
                    shape = tileShape,
                    onSelect = { activeTab = it },
                    selectedTextColor = Color(0xFF04101A)
                )
                if (activeTab < 2) {
                    AnimatedStyleGrid(
                        tabIndex = activeTab,
                        leftTools = leftTools,
                        rightTiles = rightTiles,
                        accent = if (activeTab == 0) neonRed else neonCyan,
                        inactiveCard = Color(0xFF121623),
                        shape = tileShape,
                        columns = 3,
                        cardHeight = 52.dp,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        EsportsHorizontalSliderRow(Icons.Rounded.WbSunny, "Brightness", brightnessFraction, neonRed, callbacks.onBrightnessChange)
                        EsportsHorizontalSliderRow(Icons.AutoMirrored.Rounded.VolumeUp, "Volume", volumeFraction, neonCyan, callbacks.onVolumeChange)
                        EsportsQuickAppBar(shape = tileShape, callbacks = callbacks)
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
            val strokeW = (if (compact) 5.5.dp else 7.dp).toPx()
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
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = RajdhaniFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (compact) 17.sp else 20.sp
                ),
                color = Color.White,
                maxLines = 1
            )
            Text(
                text = unitLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = if (compact) 8.sp else 9.sp, fontWeight = FontWeight.Bold),
                color = TextMuted,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(if (compact) 1.dp else 2.dp))
            Box(
                modifier = Modifier
                    .clip(CutCornerShape(4.dp))
                    .background(arcColor.copy(alpha = 0.22f))
                    .border(1.dp, arcColor.copy(alpha = 0.7f), CutCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text(
                    text = badgeLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold, fontSize = if (compact) 8.sp else 9.sp),
                    color = arcColor
                )
            }
        }
    }
}
