package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CpuMode
import com.example.data.GameTurboSettings
import com.example.data.HardwareTelemetry
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSlate
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

// =====================================================================
// 1. ROG ARMOURY / GAME GENIE OVERLAY STYLE
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
    val rogOrange = Color(0xFFFF5252)
    val rogShape = CutCornerShape(topStart = 18.dp, bottomEnd = 18.dp, topEnd = 6.dp, bottomStart = 6.dp)
    val tileShape = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp, topEnd = 4.dp, bottomStart = 4.dp)
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Tactical, 1 = System & Display

    val infiniteTransition = rememberInfiniteTransition(label = "rog_sweep")
    val sweepFraction by infiniteTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rog_beam"
    )
    val effectiveSweep = if (!settings.lowRamOverlayMode) sweepFraction else 0.5f

    Box(
        modifier = Modifier
            .testTag("overlay_rog_root")
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = if (isWideLandscape) 18.dp else 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = if (isWideLandscape) 860.dp else 460.dp)
                .fillMaxWidth()
                .then(if (isWideLandscape) Modifier.fillMaxHeight(0.94f) else Modifier.fillMaxHeight(0.88f))
                .clip(rogShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xF50B0D14), Color(0xF512141E), Color(0xF508090E))
                    )
                )
                .border(
                    width = 1.6.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(rogCrimson, Color(0xFF2B2F3E), rogOrange)
                    ),
                    shape = rogShape
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ROG Animated Scanline Header
            Box(modifier = Modifier.fillMaxWidth()) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val x = size.width * effectiveSweep
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, rogCrimson.copy(alpha = 0.55f), Color.Transparent),
                            startX = x - 80f,
                            endX = x + 80f
                        ),
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OverlayActionIconRow(
                        accentColor = rogCrimson,
                        containerColor = Color(0xFF171A26),
                        shape = tileShape,
                        callbacks = callbacks
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .testTag("overlay_g_handle")
                                .clip(tileShape)
                                .background(
                                    Brush.horizontalGradient(listOf(rogCrimson, Color(0xFFD50032)))
                                )
                                .clickable { callbacks.onMinimizeClick() }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "ROG GENIE • ${telemetry.actualFps} FPS",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = RajdhaniFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.1.sp
                                ),
                                color = Color.White
                            )
                        }
                    }
                }
            }

            if (statusMessage != null) {
                OverlayStatusPill(statusMessage, rogCrimson, tileShape)
            }

            // ROG Mode Selector (Eco / Balance / X-Mode)
            Row(
                modifier = Modifier
                    .testTag("overlay_cpu_mode_pill")
                    .fillMaxWidth()
                    .clip(tileShape)
                    .background(Color(0xFF131622))
                    .border(1.dp, CyberBorder.copy(alpha = 0.7f), tileShape)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    CpuMode.ENERGY_SAVING to "ECO",
                    CpuMode.BALANCED to "BALANCE",
                    CpuMode.ACCELERATION to "X-MODE"
                ).forEach { (mode, label) ->
                    val selected = settings.cpuMode == mode
                    val modeColor = when (mode) {
                        CpuMode.ENERGY_SAVING -> ElectricCyan
                        CpuMode.BALANCED -> CyberAmber
                        CpuMode.ACCELERATION -> rogCrimson
                    }
                    val bg by animateColorAsState(
                        targetValue = if (selected) modeColor.copy(alpha = 0.25f) else Color.Transparent,
                        animationSpec = tween(220),
                        label = "rog_mode_bg"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(tileShape)
                            .background(bg)
                            .border(
                                1.dp,
                                if (selected) modeColor else Color.Transparent,
                                tileShape
                            )
                            .clickable { callbacks.onSelectCpuMode(mode) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = RajdhaniFontFamily,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = if (selected) modeColor else TextSecondary
                        )
                    }
                }
            }

            // Adaptive Main Body
            if (isWideLandscape) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left Hardware & Audio/Brightness Armory Column
                    Column(
                        modifier = Modifier
                            .weight(0.42f)
                            .fillMaxHeight()
                            .clip(tileShape)
                            .background(Color(0xFF10131D))
                            .border(1.dp, CyberBorder.copy(alpha = 0.6f), tileShape)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            EsportsMetricMeter(
                                label = "CPU FREQUENCY",
                                valueText = "$cpuDisplayValue $cpuDisplayUnit",
                                progress = telemetry.cpuFreqMHz / 3200f,
                                accentColor = rogCrimson
                            )
                            EsportsMetricMeter(
                                label = "MEMORY LOAD",
                                valueText = "${telemetry.ramUsedPercent}% (${telemetry.ramAvailableMb}MB)",
                                progress = telemetry.ramUsedPercent / 100f,
                                accentColor = rogOrange
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MiniTelemetryTag(
                                    text = "${telemetry.batteryTempCelsius}°C",
                                    accent = CyberAmber,
                                    shape = tileShape,
                                    modifier = Modifier.weight(1f)
                                )
                                MiniTelemetryTag(
                                    text = telemetry.pingMs?.let { "${it} ms" } ?: "Net OK",
                                    accent = ElectricCyan,
                                    shape = tileShape,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .testTag("overlay_ram_boost_pill")
                                .fillMaxWidth()
                                .height(42.dp)
                                .clip(tileShape)
                                .background(Brush.horizontalGradient(listOf(rogCrimson, Color(0xFFB71C1C))))
                                .clickable { callbacks.onBoostRam() },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.RocketLaunch, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("PURGE & BOOST RAM", style = MaterialTheme.typography.labelLarge.copy(fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold), color = Color.White)
                            }
                        }
                        Column {
                            EsportsHorizontalSliderRow(Icons.Rounded.WbSunny, "Brightness", brightnessFraction, rogCrimson, callbacks.onBrightnessChange)
                            EsportsHorizontalSliderRow(Icons.AutoMirrored.Rounded.VolumeUp, "Volume", volumeFraction, ElectricCyan, callbacks.onVolumeChange)
                        }
                        EsportsQuickAppBar(shape = tileShape, callbacks = callbacks)
                    }

                    // Right Tactical Tools & System Grid Column
                    Column(
                        modifier = Modifier
                            .weight(0.58f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StyleTabStrip(
                            tabs = listOf("TACTICAL TOOLS", "SYSTEM CONTROLS"),
                            selectedIndex = activeTab,
                            accent = rogCrimson,
                            shape = tileShape,
                            onSelect = { activeTab = it }
                        )
                        AnimatedStyleGrid(
                            tabIndex = activeTab,
                            leftTools = leftTools,
                            rightTiles = rightTiles,
                            accent = rogCrimson,
                            inactiveCard = Color(0xFF151926),
                            shape = tileShape,
                            columns = 3,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                // Portrait ROG Armoury Stack
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EsportsMetricMeter(
                        label = "CPU",
                        valueText = "$cpuDisplayValue $cpuDisplayUnit",
                        progress = telemetry.cpuFreqMHz / 3200f,
                        accentColor = rogCrimson,
                        modifier = Modifier.weight(1f)
                    )
                    EsportsMetricMeter(
                        label = "RAM",
                        valueText = "${telemetry.ramUsedPercent}%",
                        progress = telemetry.ramUsedPercent / 100f,
                        accentColor = rogOrange,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .testTag("overlay_ram_boost_pill")
                            .height(42.dp)
                            .clip(tileShape)
                            .background(rogCrimson)
                            .clickable { callbacks.onBoostRam() }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("BOOST", style = MaterialTheme.typography.labelLarge.copy(fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold), color = Color.White)
                    }
                }

                StyleTabStrip(
                    tabs = listOf("TACTICAL", "SYSTEM", "AUDIO & DISPLAY"),
                    selectedIndex = activeTab,
                    accent = rogCrimson,
                    shape = tileShape,
                    onSelect = { activeTab = it }
                )

                if (activeTab < 2) {
                    AnimatedStyleGrid(
                        tabIndex = activeTab,
                        leftTools = leftTools,
                        rightTiles = rightTiles,
                        accent = rogCrimson,
                        inactiveCard = Color(0xFF151926),
                        shape = tileShape,
                        columns = 3,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        EsportsHorizontalSliderRow(Icons.Rounded.WbSunny, "Brightness", brightnessFraction, rogCrimson, callbacks.onBrightnessChange)
                        EsportsHorizontalSliderRow(Icons.AutoMirrored.Rounded.VolumeUp, "Volume", volumeFraction, ElectricCyan, callbacks.onVolumeChange)
                        if (isStopwatchRunning || stopwatchSeconds > 0) {
                            Text("Stopwatch: ${stopwatchSeconds}s", style = MaterialTheme.typography.labelLarge, color = CyberAmber)
                        }
                        EsportsQuickAppBar(shape = tileShape, callbacks = callbacks)
                    }
                }
            }
        }
    }
}

// =====================================================================
// 2. XIAOMI / HYPEROS GAME TURBO OVERLAY STYLE
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
    val hyperBlue = Color(0xFF00A8FF)
    val hyperMint = Color(0xFF00E5C3)
    val hyperGold = Color(0xFFFFB300)
    val isTurboActive = settings.cpuMode == CpuMode.ACCELERATION || settings.turboXMode
    val primaryAccent by animateColorAsState(
        targetValue = if (isTurboActive) hyperGold else hyperBlue,
        animationSpec = tween(280),
        label = "xiaomi_accent"
    )
    val deckShape = RoundedCornerShape(26.dp)
    val pillShape = RoundedCornerShape(16.dp)
    var activeTab by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .testTag("overlay_xiaomi_root")
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = if (isWideLandscape) 18.dp else 12.dp, vertical = 10.dp),
        contentAlignment = if (isWideLandscape) Alignment.CenterStart else Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = if (isWideLandscape) 720.dp else 440.dp)
                .fillMaxWidth()
                .then(if (isWideLandscape) Modifier.fillMaxHeight(0.94f) else Modifier.fillMaxHeight(0.88f))
                .clip(deckShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xF20D1729), Color(0xF50A1120))
                    )
                )
                .border(
                    width = 1.4.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(primaryAccent.copy(alpha = 0.85f), hyperMint.copy(alpha = 0.55f), CyberBorder)
                    ),
                    shape = deckShape
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // HyperOS Top Capsule Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OverlayActionIconRow(
                    accentColor = primaryAccent,
                    containerColor = Color(0xFF162238),
                    shape = CircleShape,
                    callbacks = callbacks
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .testTag("overlay_g_handle")
                            .clip(CircleShape)
                            .background(Color(0xFF182742))
                            .border(1.dp, hyperMint.copy(alpha = 0.65f), CircleShape)
                            .clickable { callbacks.onMinimizeClick() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${telemetry.actualFps} FPS • HyperOS",
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold),
                            color = hyperMint
                        )
                    }
                }
            }

            if (statusMessage != null) {
                OverlayStatusPill(statusMessage, primaryAccent, pillShape)
            }

            // Iconic Xiaomi Hero Booster Banner + Mode Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(pillShape)
                    .background(Color(0xFF132038))
                    .border(1.dp, primaryAccent.copy(alpha = 0.45f), pillShape)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "GAME TURBO ${if (isTurboActive) "BOOSTED" else "BALANCED"}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = primaryAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "CPU $cpuDisplayValue $cpuDisplayUnit • RAM ${telemetry.ramUsedPercent}% • ${telemetry.batteryTempCelsius}°C",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .testTag("overlay_cpu_mode_pill")
                            .height(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1B2E4E))
                            .border(1.dp, hyperBlue.copy(alpha = 0.7f), CircleShape)
                            .clickable {
                                val next = CpuMode.entries[(settings.cpuMode.ordinal + 1) % CpuMode.entries.size]
                                callbacks.onSelectCpuMode(next)
                            }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = when (settings.cpuMode) {
                                    CpuMode.ENERGY_SAVING -> "Eco"
                                    CpuMode.BALANCED -> "Balanced"
                                    CpuMode.ACCELERATION -> "Wild"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Mode", tint = hyperBlue, modifier = Modifier.size(16.dp))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .testTag("overlay_ram_boost_pill")
                            .height(40.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(listOf(primaryAccent, hyperMint))
                            )
                            .clickable { callbacks.onBoostRam() }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Bolt, contentDescription = null, tint = Color(0xFF06101C), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Boost",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF06101C)
                            )
                        }
                    }
                }
            }

            if (isWideLandscape) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.38f)
                            .fillMaxHeight()
                            .clip(pillShape)
                            .background(Color(0xFF111D32))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            EsportsMetricMeter("CPU CLOCK", "$cpuDisplayValue $cpuDisplayUnit", telemetry.cpuFreqMHz / 3200f, hyperBlue)
                            EsportsMetricMeter("MEMORY", "${telemetry.ramUsedPercent}%", telemetry.ramUsedPercent / 100f, hyperMint)
                        }
                        Column {
                            EsportsHorizontalSliderRow(Icons.Rounded.WbSunny, "Brightness", brightnessFraction, hyperGold, callbacks.onBrightnessChange)
                            EsportsHorizontalSliderRow(Icons.AutoMirrored.Rounded.VolumeUp, "Volume", volumeFraction, hyperMint, callbacks.onVolumeChange)
                        }
                        EsportsQuickAppBar(shape = CircleShape, callbacks = callbacks)
                    }
                    Column(
                        modifier = Modifier
                            .weight(0.62f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StyleTabStrip(
                            tabs = listOf("Utilities", "Quick Controls"),
                            selectedIndex = activeTab,
                            accent = primaryAccent,
                            shape = CircleShape,
                            onSelect = { activeTab = it }
                        )
                        AnimatedStyleGrid(
                            tabIndex = activeTab,
                            leftTools = leftTools,
                            rightTiles = rightTiles,
                            accent = primaryAccent,
                            inactiveCard = Color(0xFF15233B),
                            shape = pillShape,
                            columns = 3,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                StyleTabStrip(
                    tabs = listOf("Utilities", "Controls", "Sliders & Apps"),
                    selectedIndex = activeTab,
                    accent = primaryAccent,
                    shape = CircleShape,
                    onSelect = { activeTab = it }
                )
                if (activeTab < 2) {
                    AnimatedStyleGrid(
                        tabIndex = activeTab,
                        leftTools = leftTools,
                        rightTiles = rightTiles,
                        accent = primaryAccent,
                        inactiveCard = Color(0xFF15233B),
                        shape = pillShape,
                        columns = 3,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        EsportsHorizontalSliderRow(Icons.Rounded.WbSunny, "Brightness", brightnessFraction, hyperGold, callbacks.onBrightnessChange)
                        EsportsHorizontalSliderRow(Icons.AutoMirrored.Rounded.VolumeUp, "Volume", volumeFraction, hyperMint, callbacks.onVolumeChange)
                        EsportsQuickAppBar(shape = CircleShape, callbacks = callbacks)
                    }
                }
            }
        }
    }
}

// =====================================================================
// 3. REDMAGIC CYBER / ENERGY CUBE OVERLAY STYLE
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
    val tileShape = CutCornerShape(9.dp)
    var activeTab by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .testTag("overlay_redmagic_root")
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = if (isWideLandscape) 18.dp else 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = if (isWideLandscape) 880.dp else 460.dp)
                .fillMaxWidth()
                .then(if (isWideLandscape) Modifier.fillMaxHeight(0.94f) else Modifier.fillMaxHeight(0.88f))
                .clip(shellShape)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xF60A060C), Color(0xF6070A12), Color(0xF6060C14))
                    )
                )
                .border(
                    width = 1.6.dp,
                    brush = Brush.horizontalGradient(colors = listOf(neonRed, Color(0xFF282E42), neonCyan)),
                    shape = shellShape
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // RedMagic Cooling Fan + Dual Neon Rail Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OverlayActionIconRow(
                    accentColor = neonRed,
                    containerColor = Color(0xFF141824),
                    shape = tileShape,
                    callbacks = callbacks
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SpinningCoolingFanGlyph(
                        cpuMode = settings.cpuMode,
                        animate = !settings.lowRamOverlayMode,
                        primaryColor = neonRed,
                        secondaryColor = neonCyan,
                        sizeDp = 34.dp
                    )
                    Box(
                        modifier = Modifier
                            .testTag("overlay_g_handle")
                            .clip(tileShape)
                            .background(Color(0xFF131826))
                            .border(
                                1.2.dp,
                                Brush.horizontalGradient(listOf(neonRed, neonCyan)),
                                tileShape
                            )
                            .clickable { callbacks.onMinimizeClick() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "REDMAGIC • ${telemetry.actualFps} FPS",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = RajdhaniFontFamily,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            ),
                            color = Color.White
                        )
                    }
                }
            }

            if (statusMessage != null) {
                OverlayStatusPill(statusMessage, neonCyan, tileShape)
            }

            // Dual Neon Energy Strip (Rise/Diablo Mode + Fan/Temp + Boost)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .testTag("overlay_cpu_mode_pill")
                        .weight(1.4f)
                        .height(42.dp)
                        .clip(tileShape)
                        .background(Color(0xFF121521))
                        .border(1.dp, neonRed.copy(alpha = 0.7f), tileShape)
                        .clickable {
                            val next = CpuMode.entries[(settings.cpuMode.ordinal + 1) % CpuMode.entries.size]
                            callbacks.onSelectCpuMode(next)
                        }
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = when (settings.cpuMode) {
                            CpuMode.ENERGY_SAVING -> "ECO SHIELD"
                            CpuMode.BALANCED -> "RISE MODE"
                            CpuMode.ACCELERATION -> "DIABLO TURBO"
                        },
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold),
                        color = neonRed
                    )
                    Text(
                        text = "$cpuDisplayValue $cpuDisplayUnit",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = RajdhaniFontFamily),
                        color = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .testTag("overlay_ram_boost_pill")
                        .weight(1f)
                        .height(42.dp)
                        .clip(tileShape)
                        .background(Color(0xFF0E1D28))
                        .border(1.dp, neonCyan.copy(alpha = 0.85f), tileShape)
                        .clickable { callbacks.onBoostRam() }
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "TURBO ${telemetry.ramUsedPercent}%",
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold),
                        color = neonCyan
                    )
                }
            }

            // Live Esports Pill Telemetry Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiniTelemetryTag("${telemetry.batteryTempCelsius}°C FAN", neonRed, tileShape, Modifier.weight(1f), Icons.Rounded.Thermostat)
                MiniTelemetryTag(
                    telemetry.pingMs?.let { "${it}ms NET" } ?: "${telemetry.displayRefreshRateHz}Hz",
                    neonCyan,
                    tileShape,
                    Modifier.weight(1f),
                    Icons.Rounded.NetworkCheck
                )
            }

            if (isWideLandscape) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.58f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StyleTabStrip(
                            tabs = listOf("ESPORTS PLUGINS", "SYSTEM MATRIX"),
                            selectedIndex = activeTab,
                            accent = neonCyan,
                            shape = tileShape,
                            onSelect = { activeTab = it }
                        )
                        AnimatedStyleGrid(
                            tabIndex = activeTab,
                            leftTools = leftTools,
                            rightTiles = rightTiles,
                            accent = if (activeTab == 0) neonRed else neonCyan,
                            inactiveCard = Color(0xFF121623),
                            shape = tileShape,
                            columns = 3,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(0.42f)
                            .fillMaxHeight()
                            .clip(tileShape)
                            .background(Color(0xFF0F1420))
                            .border(1.dp, neonCyan.copy(alpha = 0.45f), tileShape)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            EsportsMetricMeter("CPU CORE", "$cpuDisplayValue $cpuDisplayUnit", telemetry.cpuFreqMHz / 3200f, neonRed)
                            EsportsMetricMeter("RAM ENERGY", "${telemetry.ramUsedPercent}%", telemetry.ramUsedPercent / 100f, neonCyan)
                        }
                        Column {
                            EsportsHorizontalSliderRow(Icons.Rounded.WbSunny, "Brightness", brightnessFraction, neonRed, callbacks.onBrightnessChange)
                            EsportsHorizontalSliderRow(Icons.AutoMirrored.Rounded.VolumeUp, "Volume", volumeFraction, neonCyan, callbacks.onVolumeChange)
                        }
                        EsportsQuickAppBar(shape = tileShape, callbacks = callbacks)
                    }
                }
            } else {
                StyleTabStrip(
                    tabs = listOf("PLUGINS", "MATRIX", "CONTROLS"),
                    selectedIndex = activeTab,
                    accent = neonCyan,
                    shape = tileShape,
                    onSelect = { activeTab = it }
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
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.SpaceEvenly
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
private fun StyleTabStrip(
    tabs: List<String>,
    selectedIndex: Int,
    accent: Color,
    shape: androidx.compose.ui.graphics.Shape,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CyberSlate.copy(alpha = 0.85f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tabs.forEachIndexed { idx, label ->
            val selected = selectedIndex == idx
            val bg by animateColorAsState(
                targetValue = if (selected) accent else Color.Transparent,
                animationSpec = tween(200),
                label = "style_tab_bg_$idx"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(shape)
                    .background(bg)
                    .clickable { onSelect(idx) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (selected) Color.White else TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AnimatedStyleGrid(
    tabIndex: Int,
    leftTools: List<WingToolItem>,
    rightTiles: List<RightTileItem>,
    accent: Color,
    inactiveCard: Color,
    shape: androidx.compose.ui.graphics.Shape,
    columns: Int,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = tabIndex,
        transitionSpec = {
            val dir = if (targetState > initialState) 1 else -1
            (slideInHorizontally(
                animationSpec = tween(220, easing = FastOutSlowInEasing),
                initialOffsetX = { dir * (it / 5) }
            ) + fadeIn(tween(180))) togetherWith
                (slideOutHorizontally(
                    animationSpec = tween(180),
                    targetOffsetX = { -dir * (it / 6) }
                ) + fadeOut(tween(140)))
        },
        label = "esports_grid_switch",
        modifier = modifier
    ) { tab ->
        if (tab == 0) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(leftTools, key = { it.id }) { tool ->
                    EsportsToolCard(
                        label = tool.label,
                        icon = tool.icon,
                        badgeText = null,
                        active = tool.isActive,
                        activeAccent = accent,
                        inactiveContainer = inactiveCard,
                        shape = shape,
                        onClick = tool.onClick
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(rightTiles, key = { it.id }) { tile ->
                    EsportsToolCard(
                        label = tile.label,
                        icon = tile.icon,
                        badgeText = tile.customBadgeText,
                        active = tile.isActive,
                        activeAccent = accent,
                        inactiveContainer = inactiveCard,
                        shape = shape,
                        onClick = tile.onClick
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniTelemetryTag(
    text: String,
    accent: Color,
    shape: androidx.compose.ui.graphics.Shape,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(
        modifier = modifier
            .clip(shape)
            .background(accent.copy(alpha = 0.14f))
            .border(1.dp, accent.copy(alpha = 0.45f), shape)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold),
            color = TextPrimary,
            maxLines = 1
        )
    }
}

@Composable
private fun OverlayStatusPill(
    message: String,
    accent: Color,
    shape: androidx.compose.ui.graphics.Shape
) {
    Surface(
        color = Color(0xFF131826),
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, accent.copy(alpha = 0.7f), shape)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
