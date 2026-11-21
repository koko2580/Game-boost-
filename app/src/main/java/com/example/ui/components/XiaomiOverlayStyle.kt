package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
// REAL XIAOMI / HYPEROS GAME TURBO SIDEBAR OVERLAY
// Left floating app rail + Charcoal GameTurbo deck + Amber FPS graph
// =====================================================================
@Composable
internal fun XiaomiHyperOverlayContent(
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
    val miGold = Color(0xFFFFB703)
    val miOrange = Color(0xFFFF8A00)
    val miBg = Color(0xEC16181E)
    val miCardBg = Color(0xFF1F222A)
    val miTileBg = Color(0xFF262A34)
    val miBorder = Color(0xFF393E4B)

    val isPerfActive = settings.cpuMode != CpuMode.ENERGY_SAVING || settings.turboXMode
    val accent by animateColorAsState(
        targetValue = when (settings.cpuMode) {
            CpuMode.ENERGY_SAVING -> Color(0xFF38BDF8)
            CpuMode.BALANCED -> miGold
            CpuMode.ACCELERATION -> miOrange
        },
        animationSpec = tween(260),
        label = "xiaomi_mode_accent"
    )

    var activeTab by remember { mutableIntStateOf(0) }

    // Smooth left-edge drawer slide-in animation
    val slideProgress = remember { Animatable(if (settings.lowRamOverlayMode) 1f else 0f) }
    LaunchedEffect(settings.lowRamOverlayMode) {
        if (settings.lowRamOverlayMode) {
            slideProgress.snapTo(1f)
        } else {
            slideProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
            )
        }
    }

    // Live FPS waveform history buffer
    val fpsSamples = remember { mutableStateListOf(56f, 59f, 58f, 60f, 57f, 60f, 59f, 60f, 58f, 60f) }
    LaunchedEffect(telemetry.actualFps) {
        fpsSamples.add(telemetry.actualFps.toFloat().coerceIn(5f, 144f))
        if (fpsSamples.size > 22) {
            fpsSamples.removeAt(0)
        }
    }

    val cpuLoadPct = ((telemetry.cpuFreqMHz.toFloat() / 3200f) * 100f).toInt().coerceIn(18, 99)

    Box(
        modifier = Modifier
            .testTag("overlay_xiaomi_root")
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = if (isWideLandscape) 12.dp else 10.dp, vertical = 8.dp),
        contentAlignment = if (isWideLandscape) Alignment.CenterStart else Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = if (isWideLandscape) 430.dp else 410.dp)
                .fillMaxWidth()
                .then(if (isWideLandscape) Modifier.fillMaxHeight(0.95f) else Modifier.fillMaxHeight(0.86f))
                .graphicsLayer {
                    val t = slideProgress.value
                    translationX = -96f * (1f - t)
                    alpha = t
                }
                .clip(RoundedCornerShape(22.dp))
                .background(miBg)
                .border(1.2.dp, miBorder, RoundedCornerShape(22.dp))
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Far-Left Vertical Quick App Rail (iconic Xiaomi sidebar strip)
            Column(
                modifier = Modifier
                    .width(44.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF101217))
                    .border(1.dp, Color(0xFF282C36), RoundedCornerShape(16.dp))
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    MiRailAppDot(Icons.Rounded.Public, "Browser", Color(0xFF2563EB)) {
                        callbacks.onQuickApp(listOf("com.android.chrome"), "https://www.google.com")
                    }
                    MiRailAppDot(Icons.Rounded.Forum, "Chat", Color(0xFF0EA5E9)) {
                        callbacks.onQuickApp(listOf("org.telegram.messenger", "com.whatsapp"), "https://web.telegram.org")
                    }
                    MiRailAppDot(Icons.Rounded.VideoLibrary, "Video", Color(0xFFEF4444)) {
                        callbacks.onQuickApp(listOf("com.google.android.youtube"), "https://m.youtube.com")
                    }
                    MiRailAppDot(Icons.Rounded.Gamepad, "Games", Color(0xFF8B5CF6)) {
                        callbacks.onGamesClick()
                    }
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    MiRailAppDot(Icons.Rounded.Home, "Home", Color(0xFF334155), tag = "overlay_home") {
                        callbacks.onHomeClick()
                    }
                    MiRailAppDot(Icons.Rounded.Settings, "Settings", Color(0xFF334155), tag = "overlay_settings") {
                        callbacks.onSettingsClick()
                    }
                    MiRailAppDot(Icons.Rounded.PowerSettingsNew, "Close", Color(0xFF991B1B), tag = "overlay_power") {
                        callbacks.onPowerExitClick()
                    }
                }
            }

            // Main Xiaomi Game Turbo Body
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                // Top Header: GAMETURBO + Minimize Handle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Brush.horizontalGradient(listOf(miOrange, miGold)))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "GT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = RajdhaniFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFF140D02)
                            )
                        }
                        Text(
                            text = "GAMETURBO",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = RajdhaniFontFamily,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))

                    Row(
                        modifier = Modifier
                            .testTag("overlay_g_handle")
                            .clip(CircleShape)
                            .background(miTileBg)
                            .border(1.dp, accent.copy(alpha = 0.55f), CircleShape)
                            .clickable { callbacks.onMinimizeClick() }
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Turbo X",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Icon(Icons.Rounded.KeyboardArrowRight, contentDescription = "Minimize", tint = accent, modifier = Modifier.size(15.dp))
                    }
                }

                if (statusMessage != null) {
                    OverlayStatusPill(statusMessage, accent, RoundedCornerShape(10.dp))
                }

                // Authentic Xiaomi FPS Graph & Performance Switch Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(miCardBg)
                        .border(1.dp, miBorder.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CPU $cpuLoadPct% • $cpuDisplayValue$cpuDisplayUnit • ${telemetry.batteryTempCelsius}°C",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))

                        // Iconic Performance Toggle Pill (cycles CpuMode cleanly!)
                        Row(
                            modifier = Modifier
                                .testTag("overlay_cpu_mode_pill")
                                .clip(CircleShape)
                                .clickable {
                                    val next = CpuMode.entries[(settings.cpuMode.ordinal + 1) % CpuMode.entries.size]
                                    callbacks.onSelectCpuMode(next)
                                }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = when (settings.cpuMode) {
                                    CpuMode.ENERGY_SAVING -> "Balanced"
                                    CpuMode.BALANCED -> "Performance"
                                    CpuMode.ACCELERATION -> "Wild Boost"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = accent
                            )
                            val thumbOffset by animateDpAsState(
                                targetValue = if (isPerfActive) 14.dp else 2.dp,
                                animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
                                label = "mi_perf_switch"
                            )
                            Box(
                                modifier = Modifier
                                    .size(width = 30.dp, height = 16.dp)
                                    .clip(CircleShape)
                                    .background(if (isPerfActive) accent else Color(0xFF3A3F4B))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .offset(x = thumbOffset, y = 2.dp)
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    }

                    // Live Golden FPS Line Graph
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isWideLandscape) 42.dp else 52.dp)
                    ) {
                        XiaomiFpsWaveformCanvas(
                            samples = fpsSamples,
                            lineColor = accent,
                            modifier = Modifier.fillMaxSize().padding(end = 18.dp)
                        )
                        Column(
                            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.End
                        ) {
                            Text("120", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = TextMuted)
                            Text("60", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = TextMuted)
                            Text("0", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = TextMuted)
                        }
                    }

                    // Bottom FPS + Gold-Bordered Boost CTA Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = "${telemetry.actualFps}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = RajdhaniFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                ),
                                color = accent
                            )
                            Text(
                                text = "FPS",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextSecondary,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .testTag("overlay_ram_boost_pill")
                                .weight(1f)
                                .height(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(accent.copy(alpha = 0.14f))
                                .border(1.2.dp, accent, RoundedCornerShape(8.dp))
                                .clickable { callbacks.onBoostRam() },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Rounded.Bolt, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
                                Text(
                                    text = "Boost • RAM ${telemetry.ramUsedPercent}%",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = RajdhaniFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = accent,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Clean Switch Tabs: Tools | System & Sliders
                StyleTabStrip(
                    tabs = listOf("Game Tools", "Quick Controls"),
                    selectedIndex = activeTab,
                    accent = accent,
                    shape = RoundedCornerShape(10.dp),
                    onSelect = { activeTab = it },
                    selectedTextColor = Color(0xFF151208),
                    containerColor = miCardBg
                )

                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(150)) },
                    label = "xiaomi_tab_content",
                    modifier = Modifier.weight(1f)
                ) { tab ->
                    if (tab == 0) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(leftTools, key = { it.id }) { tool ->
                                EsportsToolCard(
                                    label = tool.label,
                                    icon = tool.icon,
                                    badgeText = null,
                                    active = tool.isActive,
                                    activeAccent = accent,
                                    inactiveContainer = miTileBg,
                                    shape = RoundedCornerShape(11.dp),
                                    cardHeight = if (isWideLandscape) 46.dp else 54.dp,
                                    onClick = tool.onClick
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(4),
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(rightTiles, key = { it.id }) { tile ->
                                    EsportsToolCard(
                                        label = tile.label,
                                        icon = tile.icon,
                                        badgeText = tile.customBadgeText,
                                        active = tile.isActive,
                                        activeAccent = accent,
                                        inactiveContainer = miTileBg,
                                        shape = RoundedCornerShape(11.dp),
                                        cardHeight = if (isWideLandscape) 44.dp else 50.dp,
                                        onClick = tile.onClick
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(miCardBg)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                EsportsHorizontalSliderRow(Icons.Rounded.WbSunny, "Brightness", brightnessFraction, accent, callbacks.onBrightnessChange)
                                EsportsHorizontalSliderRow(Icons.AutoMirrored.Rounded.VolumeUp, "Volume", volumeFraction, Color(0xFF38BDF8), callbacks.onVolumeChange)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiRailAppDot(
    icon: ImageVector,
    contentDescription: String,
    tintBg: Color,
    tag: String? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .size(30.dp)
            .clip(CircleShape)
            .background(tintBg.copy(alpha = 0.85f))
            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = Color.White, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun XiaomiFpsWaveformCanvas(
    samples: List<Float>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
        for (i in 0..2) {
            val y = h * (i / 2f)
            drawLine(
                color = Color(0xFF383E4C),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = dash
            )
        }
        if (samples.size < 2) return@Canvas
        val path = Path()
        val fillPath = Path()
        val maxFps = 125f
        samples.forEachIndexed { index, fps ->
            val x = (index.toFloat() / (samples.size - 1).coerceAtLeast(1)) * w
            val y = h - ((fps / maxFps).coerceIn(0.05f, 0.98f) * h)
            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, h)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            if (index == samples.lastIndex) {
                fillPath.lineTo(x, h)
                fillPath.close()
            }
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.28f), Color.Transparent))
        )
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        val lastY = h - ((samples.last() / maxFps).coerceIn(0.05f, 0.98f) * h)
        drawCircle(color = Color.White, radius = 3.2.dp.toPx(), center = Offset(w, lastY))
        drawCircle(color = lineColor, radius = 5.5.dp.toPx(), center = Offset(w, lastY), style = Stroke(1.4.dp.toPx()))
    }
}
