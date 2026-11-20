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
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.VideoLibrary
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CpuMode
import com.example.data.GameTurboSettings
import com.example.data.HardwareTelemetry
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSlate
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.RajdhaniFontFamily
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
    val tileShape = CutCornerShape(topStart = 9.dp, bottomEnd = 9.dp, topEnd = 3.dp, bottomStart = 3.dp)
    var activeTab by remember { mutableIntStateOf(0) }

    val effectiveSweep = if (!settings.lowRamOverlayMode) {
        val infiniteTransition = rememberInfiniteTransition(label = "rog_sweep")
        val sweep by infiniteTransition.animateFloat(
            initialValue = -0.3f,
            targetValue = 1.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rog_beam"
        )
        sweep
    } else {
        0.5f
    }

    Box(
        modifier = Modifier
            .testTag("overlay_rog_root")
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = if (isWideLandscape) 14.dp else 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = if (isWideLandscape) 860.dp else 440.dp)
                .fillMaxWidth()
                .then(if (isWideLandscape) Modifier.fillMaxHeight(0.95f) else Modifier.fillMaxHeight(0.86f))
                .clip(rogShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xF60B0D14), Color(0xF6131622), Color(0xF608090E))
                    )
                )
                .border(
                    width = 1.6.dp,
                    brush = Brush.linearGradient(colors = listOf(rogCrimson, Color(0xFF2E3446), rogOrange)),
                    shape = rogShape
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ROG Header with Inline ECO / BALANCE / X-MODE in Landscape
            Box(modifier = Modifier.fillMaxWidth()) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val x = size.width * effectiveSweep
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, rogCrimson.copy(alpha = 0.65f), Color.Transparent),
                            startX = x - 90f,
                            endX = x + 90f
                        ),
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OverlayActionIconRow(
                        accentColor = rogCrimson,
                        containerColor = Color(0xFF171A26),
                        shape = tileShape,
                        callbacks = callbacks
                    )

                    if (isWideLandscape) {
                        RogModeSegmentedStrip(
                            selectedMode = settings.cpuMode,
                            rogCrimson = rogCrimson,
                            shape = tileShape,
                            onSelect = callbacks.onSelectCpuMode,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 10.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .testTag("overlay_g_handle")
                            .clip(tileShape)
                            .background(Brush.horizontalGradient(listOf(rogCrimson, Color(0xFFD50032))))
                            .clickable { callbacks.onMinimizeClick() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "ROG • ${telemetry.actualFps} FPS",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = RajdhaniFontFamily,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp
                            ),
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }
            }

            if (statusMessage != null) {
                OverlayStatusPill(statusMessage, rogCrimson, tileShape)
            }

            if (!isWideLandscape) {
                RogModeSegmentedStrip(
                    selectedMode = settings.cpuMode,
                    rogCrimson = rogCrimson,
                    shape = tileShape,
                    onSelect = callbacks.onSelectCpuMode,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (isWideLandscape) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Left Armory Wing (Zero-overlap vertical scroll safety)
                    Column(
                        modifier = Modifier
                            .weight(0.36f)
                            .fillMaxHeight()
                            .clip(tileShape)
                            .background(Color(0xFF10131D))
                            .border(1.dp, CyberBorder.copy(alpha = 0.6f), tileShape)
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EsportsMetricMeter(
                            label = "CPU CLOCK",
                            valueText = "$cpuDisplayValue $cpuDisplayUnit",
                            progress = telemetry.cpuFreqMHz / 3200f,
                            accentColor = rogCrimson
                        )
                        EsportsMetricMeter(
                            label = "MEMORY",
                            valueText = "${telemetry.ramUsedPercent}% (${telemetry.ramAvailableMb}MB)",
                            progress = telemetry.ramUsedPercent / 100f,
                            accentColor = rogOrange
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MiniTelemetryTag(
                                text = "${telemetry.batteryTempCelsius}°C",
                                accent = CyberAmber,
                                shape = tileShape,
                                modifier = Modifier.weight(1f)
                            )
                            MiniTelemetryTag(
                                text = telemetry.pingMs?.let { "${it} ms" } ?: "${telemetry.displayRefreshRateHz}Hz",
                                accent = ElectricCyan,
                                shape = tileShape,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .testTag("overlay_ram_boost_pill")
                                .fillMaxWidth()
                                .height(36.dp)
                                .clip(tileShape)
                                .background(Brush.horizontalGradient(listOf(rogCrimson, Color(0xFFB71C1C))))
                                .clickable { callbacks.onBoostRam() },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.RocketLaunch, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "PURGE & BOOST",
                                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                        EsportsHorizontalSliderRow(Icons.Rounded.WbSunny, "Brightness", brightnessFraction, rogCrimson, callbacks.onBrightnessChange)
                        EsportsHorizontalSliderRow(Icons.AutoMirrored.Rounded.VolumeUp, "Volume", volumeFraction, ElectricCyan, callbacks.onVolumeChange)
                        EsportsQuickAppBar(shape = tileShape, callbacks = callbacks)
                    }

                    // Right 4-Column Tactical Matrix (fits all 12 tools cleanly)
                    Column(
                        modifier = Modifier
                            .weight(0.64f)
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
                            columns = 4,
                            cardHeight = 52.dp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EsportsMetricMeter("CPU", "$cpuDisplayValue $cpuDisplayUnit", telemetry.cpuFreqMHz / 3200f, rogCrimson, modifier = Modifier.weight(1f))
                    EsportsMetricMeter("RAM", "${telemetry.ramUsedPercent}%", telemetry.ramUsedPercent / 100f, rogOrange, modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .testTag("overlay_ram_boost_pill")
                            .height(38.dp)
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
                    tabs = listOf("TACTICAL", "SYSTEM", "AUDIO & APPS"),
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
                        cardHeight = 54.dp,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
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
            .background(Color(0xFF131622))
            .border(1.dp, CyberBorder.copy(alpha = 0.7f), shape)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(
            CpuMode.ENERGY_SAVING to "ECO",
            CpuMode.BALANCED to "BALANCE",
            CpuMode.ACCELERATION to "X-MODE"
        ).forEach { (mode, label) ->
            val selected = selectedMode == mode
            val modeColor = when (mode) {
                CpuMode.ENERGY_SAVING -> ElectricCyan
                CpuMode.BALANCED -> CyberAmber
                CpuMode.ACCELERATION -> rogCrimson
            }
            val bg by animateColorAsState(
                targetValue = if (selected) modeColor.copy(alpha = 0.25f) else Color.Transparent,
                animationSpec = tween(220),
                label = "rog_mode_bg_$label"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(30.dp)
                    .clip(shape)
                    .background(bg)
                    .border(1.dp, if (selected) modeColor else Color.Transparent, shape)
                    .clickable { onSelect(mode) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = if (selected) modeColor else TextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

// =====================================================================
// 2. XIAOMI / HYPEROS GAME TURBO FLOATING SIDEBAR STYLE
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
        animationSpec = tween(260),
        label = "xiaomi_accent"
    )
    val deckShape = RoundedCornerShape(24.dp)
    val pillShape = RoundedCornerShape(14.dp)
    var activeTab by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .testTag("overlay_xiaomi_root")
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = if (isWideLandscape) 14.dp else 10.dp, vertical = 8.dp),
        // Authentic HyperOS Game Turbo anchors cleanly on the left side in Landscape, keeping game visible!
        contentAlignment = if (isWideLandscape) Alignment.CenterStart else Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = if (isWideLandscape) 440.dp else 420.dp)
                .fillMaxWidth()
                .then(if (isWideLandscape) Modifier.fillMaxHeight(0.95f) else Modifier.fillMaxHeight(0.86f))
                .clip(deckShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xEA0C1526), Color(0xEE080F1D))
                    )
                )
                .border(
                    width = 1.4.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(primaryAccent.copy(alpha = 0.85f), hyperMint.copy(alpha = 0.55f), CyberBorder)
                    ),
                    shape = deckShape
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top HyperOS Header + Floating App Bubbles
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

                // Iconic MIUI/HyperOS mini floating app pills
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    HyperFloatingAppBubble("Browser", Color(0xFF1A73E8), Icons.Rounded.Public) {
                        callbacks.onQuickApp(listOf("com.android.chrome"), "https://www.google.com")
                    }
                    HyperFloatingAppBubble("Chat", Color(0xFF0088CC), Icons.Rounded.Forum) {
                        callbacks.onQuickApp(listOf("org.telegram.messenger", "com.whatsapp"), "https://web.telegram.org")
                    }
                    HyperFloatingAppBubble("Video", Color(0xFFE53935), Icons.Rounded.VideoLibrary) {
                        callbacks.onQuickApp(listOf("com.google.android.youtube"), "https://m.youtube.com")
                    }
                    Box(
                        modifier = Modifier
                            .testTag("overlay_g_handle")
                            .clip(CircleShape)
                            .background(Color(0xFF182742))
                            .border(1.dp, hyperMint.copy(alpha = 0.7f), CircleShape)
                            .clickable { callbacks.onMinimizeClick() }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "${telemetry.actualFps} FPS",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold),
                            color = hyperMint
                        )
                    }
                }
            }

            if (statusMessage != null) {
                OverlayStatusPill(statusMessage, primaryAccent, pillShape)
            }

            // Iconic Xiaomi Hero Booster Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(pillShape)
                    .background(Color(0xFF132138))
                    .border(1.dp, primaryAccent.copy(alpha = 0.45f), pillShape)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "HYPER GAME TURBO",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = primaryAccent,
                        maxLines = 1
                    )
                    Text(
                        text = "$cpuDisplayValue$cpuDisplayUnit • RAM ${telemetry.ramUsedPercent}% • ${telemetry.batteryTempCelsius}°C",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .testTag("overlay_cpu_mode_pill")
                            .height(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1B2E4E))
                            .border(1.dp, hyperBlue.copy(alpha = 0.7f), CircleShape)
                            .clickable {
                                val next = CpuMode.entries[(settings.cpuMode.ordinal + 1) % CpuMode.entries.size]
                                callbacks.onSelectCpuMode(next)
                            }
                            .padding(horizontal = 10.dp),
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
                            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "CPU Mode", tint = hyperBlue, modifier = Modifier.size(15.dp))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .testTag("overlay_ram_boost_pill")
                            .height(34.dp)
                            .clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(primaryAccent, hyperMint)))
                            .clickable { callbacks.onBoostRam() }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Bolt, contentDescription = null, tint = Color(0xFF06101C), modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "Boost",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF06101C)
                            )
                        }
                    }
                }
            }

            StyleTabStrip(
                tabs = listOf("Game Tools", "Quick Controls"),
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
                cardHeight = 48.dp,
                modifier = Modifier.weight(1f)
            )

            // Compact Bottom Frosted Sliders (Zero clipping!)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(pillShape)
                    .background(Color(0xFF111D32))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                EsportsHorizontalSliderRow(Icons.Rounded.WbSunny, "Brightness", brightnessFraction, hyperGold, callbacks.onBrightnessChange)
                EsportsHorizontalSliderRow(Icons.AutoMirrored.Rounded.VolumeUp, "Volume", volumeFraction, hyperMint, callbacks.onVolumeChange)
            }
        }
    }
}

@Composable
private fun HyperFloatingAppBubble(
    label: String,
    color: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.22f))
            .border(1.dp, color.copy(alpha = 0.7f), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(14.dp))
    }
}

// =====================================================================
// 3. REDMAGIC CYBER / 3-WING ENERGY DECK OVERLAY STYLE
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

    Box(
        modifier = Modifier
            .testTag("overlay_redmagic_root")
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = if (isWideLandscape) 14.dp else 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = if (isWideLandscape) 880.dp else 440.dp)
                .fillMaxWidth()
                .then(if (isWideLandscape) Modifier.fillMaxHeight(0.95f) else Modifier.fillMaxHeight(0.86f))
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
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // RedMagic Dual-Rail Top Bar with Live Cooling Fan Core
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
                        sizeDp = 32.dp
                    )
                    Box(
                        modifier = Modifier
                            .testTag("overlay_g_handle")
                            .clip(tileShape)
                            .background(Color(0xFF131826))
                            .border(1.2.dp, Brush.horizontalGradient(listOf(neonRed, neonCyan)), tileShape)
                            .clickable { callbacks.onMinimizeClick() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "REDMAGIC • ${telemetry.actualFps} FPS",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = RajdhaniFontFamily,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp
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
                // Iconic 3-Wing Mecha Reactor Layout (Left Power Wing + Center Matrix + Right Vertical Neon Faders)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Left Energy Wing
                    Column(
                        modifier = Modifier
                            .weight(0.28f)
                            .fillMaxHeight()
                            .clip(tileShape)
                            .background(Color(0xFF100C15))
                            .border(1.dp, neonRed.copy(alpha = 0.45f), tileShape)
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .testTag("overlay_cpu_mode_pill")
                                .fillMaxWidth()
                                .clip(tileShape)
                                .background(Color(0xFF161928))
                                .border(1.dp, neonRed.copy(alpha = 0.75f), tileShape)
                                .clickable {
                                    val next = CpuMode.entries[(settings.cpuMode.ordinal + 1) % CpuMode.entries.size]
                                    callbacks.onSelectCpuMode(next)
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = when (settings.cpuMode) {
                                    CpuMode.ENERGY_SAVING -> "ECO SHIELD"
                                    CpuMode.BALANCED -> "RISE MODE"
                                    CpuMode.ACCELERATION -> "DIABLO TURBO"
                                },
                                style = MaterialTheme.typography.labelLarge.copy(fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold),
                                color = neonRed,
                                maxLines = 1
                            )
                            Text(
                                text = "CPU $cpuDisplayValue $cpuDisplayUnit",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }

                        Box(
                            modifier = Modifier
                                .testTag("overlay_ram_boost_pill")
                                .fillMaxWidth()
                                .height(36.dp)
                                .clip(tileShape)
                                .background(Color(0xFF0E1D28))
                                .border(1.dp, neonCyan.copy(alpha = 0.85f), tileShape)
                                .clickable { callbacks.onBoostRam() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "TURBO ${telemetry.ramUsedPercent}%",
                                style = MaterialTheme.typography.labelLarge.copy(fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold),
                                color = neonCyan
                            )
                        }

                        MiniTelemetryTag("${telemetry.batteryTempCelsius}°C FAN", neonRed, tileShape, Modifier.fillMaxWidth(), Icons.Rounded.Thermostat)
                        MiniTelemetryTag(
                            telemetry.pingMs?.let { "${it}ms NET" } ?: "${telemetry.displayRefreshRateHz}Hz",
                            neonCyan,
                            tileShape,
                            Modifier.fillMaxWidth(),
                            Icons.Rounded.NetworkCheck
                        )
                        EsportsQuickAppBar(shape = tileShape, callbacks = callbacks)
                    }

                    // Center Tactical Matrix
                    Column(
                        modifier = Modifier
                            .weight(0.50f)
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
                            cardHeight = 50.dp,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Right Dual Vertical Faders + Energy Telemetry Wing
                    Column(
                        modifier = Modifier
                            .weight(0.22f)
                            .fillMaxHeight()
                            .clip(tileShape)
                            .background(Color(0xFF0B131E))
                            .border(1.dp, neonCyan.copy(alpha = 0.45f), tileShape)
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "ENERGY FADER",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            color = neonCyan,
                            maxLines = 1
                        )
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
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
                    }
                }
            } else {
                // Portrait RedMagic Energy Stack
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .testTag("overlay_cpu_mode_pill")
                            .weight(1.4f)
                            .height(38.dp)
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
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = RajdhaniFontFamily),
                            color = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .testTag("overlay_ram_boost_pill")
                            .weight(1f)
                            .height(38.dp)
                            .clip(tileShape)
                            .background(Color(0xFF0E1D28))
                            .border(1.dp, neonCyan.copy(alpha = 0.85f), tileShape)
                            .clickable { callbacks.onBoostRam() }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "TURBO ${telemetry.ramUsedPercent}%",
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold),
                            color = neonCyan
                        )
                    }
                }

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
                        cardHeight = 52.dp,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
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
private fun StyleTabStrip(
    tabs: List<String>,
    selectedIndex: Int,
    accent: Color,
    shape: Shape,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CyberSlate.copy(alpha = 0.85f))
            .padding(3.dp),
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
                    .padding(vertical = 6.dp),
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
    shape: Shape,
    columns: Int,
    cardHeight: Dp,
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
                        inactiveContainer = inactiveCard,
                        shape = shape,
                        cardHeight = cardHeight,
                        onClick = tool.onClick
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
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
                        activeAccent = accent,
                        inactiveContainer = inactiveCard,
                        shape = shape,
                        cardHeight = cardHeight,
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
    shape: Shape,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Row(
        modifier = modifier
            .clip(shape)
            .background(accent.copy(alpha = 0.14f))
            .border(1.dp, accent.copy(alpha = 0.45f), shape)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(13.dp))
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
    shape: Shape
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
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
