package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CpuMode
import com.example.data.CpuUnitFormat
import com.example.data.CrosshairStyle
import com.example.data.EqPreset
import com.example.data.GameTurboSettings
import com.example.data.HardwareTelemetry
import com.example.data.InstalledAppInfo
import com.example.data.PerformanceEngineStatus
import com.example.data.PermissionStatus
import com.example.data.ScreenColorMode
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

internal val XGameDeepBlack = Color(0xFF040508)
internal val XGameGraphite = Color(0xFF0E1017)
internal val XGameGraphiteElevated = Color(0xFF161923)
internal val XGameNeonRed = Color(0xFFFF1E38)
internal val XGameCrimsonDeep = Color(0xFF7D0A1B)
internal val XGameSteelBorder = Color(0xFF2B2F3D)

/**
 * REDMAGIC X-GAME SPACE backdrop with deep black obsidian core,
 * red illuminated side glow panels, and technical HUD telemetry grid.
 */
@Composable
internal fun XGameSpaceBackdropCanvas(
    pulseAlpha: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(XGameDeepBlack, Color(0xFF09070C), XGameDeepBlack)
            )
        )
        val step = 30.dp.toPx()
        var gx = step
        while (gx < size.width) {
            drawLine(XGameNeonRed.copy(alpha = 0.035f), Offset(gx, 0f), Offset(gx, size.height), strokeWidth = 1f)
            gx += step
        }
        var gy = step
        while (gy < size.height) {
            drawLine(ElectricCyan.copy(alpha = 0.03f), Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
            gy += step
        }
        val railW = 4.dp.toPx()
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, XGameNeonRed.copy(alpha = 0.68f * pulseAlpha), Color.Transparent)
            ),
            topLeft = Offset(0f, size.height * 0.14f),
            size = Size(railW, size.height * 0.72f)
        )
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, XGameNeonRed.copy(alpha = 0.68f * pulseAlpha), Color.Transparent)
            ),
            topLeft = Offset(size.width - railW, size.height * 0.14f),
            size = Size(railW, size.height * 0.72f)
        )
    }
}

@Composable
internal fun XGameTopStatusBar(
    clock: String,
    telemetry: HardwareTelemetry,
    remainingTurboSeconds: Long,
    permissions: PermissionStatus,
    compact: Boolean,
    onBack: () -> Unit,
    onAddTurboTime: () -> Unit,
    onNavigateAddApps: () -> Unit,
    onNavigatePermissions: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    val badgeShape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp, topEnd = 2.dp, bottomStart = 2.dp)
    val minsRemaining = (remainingTurboSeconds / 60L).coerceAtLeast(0L)
    val timeLabel = if (remainingTurboSeconds <= 0L) "BOOST TIME" else String.format(Locale.US, "%02d:%02d", minsRemaining / 60L, minsRemaining % 60L)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(badgeShape)
            .background(XGameGraphite.copy(alpha = 0.94f))
            .border(1.dp, XGameNeonRed.copy(alpha = 0.55f), badgeShape)
            .padding(horizontal = if (compact) 8.dp else 12.dp, vertical = if (compact) 4.dp else 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .testTag("xgame_back_btn")
                    .minimumInteractiveComponentSize()
                    .size(if (compact) 32.dp else 36.dp)
                    .clip(badgeShape)
                    .background(XGameGraphiteElevated)
                    .border(1.dp, XGameNeonRed.copy(alpha = 0.65f), badgeShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back to Launcher",
                    tint = TextPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Column {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = XGameNeonRed, fontWeight = FontWeight.Bold)) { append("X-GAME ") }
                        withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) { append("SPACE") }
                    },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = RajdhaniFontFamily,
                        fontSize = if (compact) 15.sp else 17.sp,
                        letterSpacing = 1.6.sp
                    ),
                    maxLines = 1
                )
                Text(
                    text = "TURBO X ENGINE HUD",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, letterSpacing = 1.2.sp),
                    color = TextMuted,
                    maxLines = 1
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp)
        ) {
            if (!compact) {
                Text(
                    text = clock,
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold),
                    color = TextSecondary
                )
            }
            XGameHudStatusPill(
                icon = if (telemetry.isCharging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryStd,
                text = "${telemetry.batteryPercent}%",
                tint = if (telemetry.batteryPercent <= 20) CyberAmber else EmeraldBright
            )
            if (!compact) {
                XGameHudStatusPill(
                    icon = Icons.Rounded.Thermostat,
                    text = String.format(Locale.US, "%.1f°C", telemetry.batteryTempCelsius),
                    tint = if (telemetry.batteryTempCelsius >= 41f) CyberAmber else ElectricCyan
                )
                XGameHudStatusPill(
                    icon = Icons.Rounded.NetworkCheck,
                    text = telemetry.pingMs?.let { "${it}ms" } ?: telemetry.networkType,
                    tint = ElectricCyan
                )
            }
            Row(
                modifier = Modifier
                    .clip(badgeShape)
                    .background(if (remainingTurboSeconds > 0L) XGameNeonRed.copy(alpha = 0.16f) else CyberAmber.copy(alpha = 0.14f))
                    .border(1.dp, if (remainingTurboSeconds > 0L) XGameNeonRed else CyberAmber, badgeShape)
                    .clickable { onAddTurboTime() }
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = "Add Turbo Time",
                    tint = if (remainingTurboSeconds > 0L) XGameNeonRed else CyberAmber,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    color = Color.White,
                    maxLines = 1
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            XGameHeaderIconBtn(Icons.Rounded.Add, "Add Games", TextPrimary, onNavigateAddApps)
            XGameHeaderIconBtn(
                Icons.Rounded.Security,
                "Permissions",
                if (permissions.allGranted) EmeraldBright else CyberAmber,
                onNavigatePermissions
            )
            XGameHeaderIconBtn(Icons.Rounded.Settings, "Settings", TextPrimary, onNavigateSettings)
        }
    }
}

@Composable
private fun XGameHeaderIconBtn(icon: ImageVector, description: String, tint: Color, onClick: () -> Unit) {
    val shape = CutCornerShape(6.dp)
    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(30.dp)
            .clip(shape)
            .background(XGameGraphiteElevated)
            .border(1.dp, XGameSteelBorder, shape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(15.dp))
    }
}

@Composable
private fun XGameHudStatusPill(icon: ImageVector, text: String, tint: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(XGameGraphiteElevated)
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = RajdhaniFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.5.sp
            ),
            color = TextPrimary,
            maxLines = 1
        )
    }
}

@Composable
internal fun XGamePerformanceLeftPanel(
    settings: GameTurboSettings,
    telemetry: HardwareTelemetry,
    performanceStatus: PerformanceEngineStatus,
    pulseAlpha: Float,
    compactHeight: Boolean,
    onSelectCpuMode: (CpuMode) -> Unit,
    onRunPingTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val panelShape = CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp, topEnd = 4.dp, bottomStart = 4.dp)
    val chipShape = CutCornerShape(6.dp)
    val cpuFormatted = if (settings.cpuUnitFormat == CpuUnitFormat.GHZ) {
        String.format(Locale.US, "%.2f", telemetry.cpuFreqMHz / 1000f)
    } else {
        "${telemetry.cpuFreqMHz}"
    }
    val cpuUnit = if (settings.cpuUnitFormat == CpuUnitFormat.GHZ) "GHz" else "MHz"

    Column(
        modifier = modifier
            .clip(panelShape)
            .background(Brush.verticalGradient(listOf(XGameGraphite.copy(alpha = 0.95f), Color(0xEB08090F))))
            .border(
                width = 1.3.dp,
                brush = Brush.verticalGradient(
                    listOf(XGameNeonRed.copy(alpha = 0.85f * pulseAlpha), XGameCrimsonDeep, XGameSteelBorder)
                ),
                shape = panelShape
            )
            .padding(horizontal = 9.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TELEMETRY CORE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = RajdhaniFontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    fontSize = 10.sp
                ),
                color = XGameNeonRed
            )
            Text(
                text = "${telemetry.cpuCoreCount} CORES",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = RajdhaniFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.5.sp
                ),
                color = ElectricCyan
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            XGameArcDialGauge(
                valueText = cpuFormatted,
                unitText = cpuUnit,
                titleText = "CPU",
                progress = (telemetry.cpuFreqMHz / 3200f).coerceIn(0.18f, 1f),
                arcColor = XGameNeonRed,
                pulseAlpha = pulseAlpha,
                compact = compactHeight,
                modifier = Modifier.weight(1f)
            )
            XGameArcDialGauge(
                valueText = "${telemetry.actualFps}",
                unitText = "FPS",
                titleText = "RAM ${telemetry.ramUsedPercent}%",
                progress = (telemetry.ramUsedPercent / 100f).coerceIn(0.12f, 1f),
                arcColor = ElectricCyan,
                pulseAlpha = pulseAlpha,
                compact = compactHeight,
                modifier = Modifier.weight(1f)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                XGameMonitorMetricTile(
                    label = "THERMAL",
                    value = "${telemetry.batteryTempCelsius}°C",
                    valueColor = if (telemetry.batteryTempCelsius >= 41f) CyberAmber else EmeraldBright,
                    shape = chipShape,
                    modifier = Modifier.weight(1f)
                )
                XGameMonitorMetricTile(
                    label = "NETWORK",
                    value = telemetry.pingMs?.let { "${it}ms" } ?: "PING",
                    valueColor = ElectricCyan,
                    shape = chipShape,
                    onClick = onRunPingTest,
                    modifier = Modifier.weight(1f)
                )
            }
            XGameMonitorMetricTile(
                label = "ENGINE PIPELINE",
                value = "${performanceStatus.activeGpuPipelineLabel} • ${settings.frameRateTarget}Hz",
                valueColor = TextPrimary,
                shape = chipShape,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(chipShape)
                .background(XGameDeepBlack)
                .border(1.dp, XGameSteelBorder, chipShape)
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            listOf(
                CpuMode.ENERGY_SAVING to "ECO",
                CpuMode.BALANCED to "BAL",
                CpuMode.ACCELERATION to "TURBO"
            ).forEach { (mode, label) ->
                val selected = settings.cpuMode == mode
                val mColor = cpuModeColor(mode, XGameNeonRed)
                val bg by animateColorAsState(
                    targetValue = if (selected) mColor.copy(alpha = 0.28f) else Color.Transparent,
                    animationSpec = tween(200),
                    label = "xgame_mode_bg_$label"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(26.dp)
                        .clip(chipShape)
                        .background(bg)
                        .border(1.dp, if (selected) mColor else Color.Transparent, chipShape)
                        .clickable { onSelectCpuMode(mode) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = if (selected) Color.White else TextMuted,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun XGameArcDialGauge(
    valueText: String,
    unitText: String,
    titleText: String,
    progress: Float,
    arcColor: Color,
    pulseAlpha: Float,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val dialSize = if (compact) 64.dp else 88.dp
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(modifier = Modifier.size(dialSize), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeW = 5.dp.toPx()
                val pad = strokeW * 0.9f
                val arcSize = Size(size.width - pad * 2f, size.height - pad * 2f)
                val topLeft = Offset(pad, pad)

                drawArc(
                    color = XGameSteelBorder.copy(alpha = 0.7f),
                    startAngle = 140f,
                    sweepAngle = 260f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
                drawArc(
                    color = arcColor.copy(alpha = 0.25f * pulseAlpha),
                    startAngle = 140f,
                    sweepAngle = 260f * progress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeW * 1.9f, cap = StrokeCap.Round)
                )
                drawArc(
                    brush = Brush.sweepGradient(listOf(arcColor.copy(alpha = 0.75f), arcColor, Color.White)),
                    startAngle = 140f,
                    sweepAngle = 260f * progress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (compact) 14.sp else 18.sp
                    ),
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = unitText,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                    color = arcColor,
                    maxLines = 1
                )
            }
        }
        Text(
            text = titleText,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = RajdhaniFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 9.5.sp,
                letterSpacing = 0.7.sp
            ),
            color = TextSecondary,
            maxLines = 1
        )
    }
}

@Composable
private fun XGameMonitorMetricTile(
    label: String,
    value: String,
    valueColor: Color,
    shape: CutCornerShape,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(XGameGraphiteElevated.copy(alpha = 0.88f))
            .border(1.dp, XGameSteelBorder, shape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 7.dp, vertical = 4.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp, letterSpacing = 0.7.sp), color = TextMuted, maxLines = 1)
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = RajdhaniFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp
            ),
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun XGameCenterGameplayFrame(
    selectedGame: InstalledAppInfo?,
    selectedGameIcon: ImageBitmap?,
    addedGames: List<InstalledAppInfo>,
    settings: GameTurboSettings,
    telemetry: HardwareTelemetry,
    pulseAlpha: Float,
    compactHeight: Boolean,
    peekAppIcon: (String) -> ImageBitmap?,
    loadAppIconAsync: suspend (String) -> ImageBitmap?,
    onSelectGame: (String) -> Unit,
    onStartPlay: () -> Unit,
    onNavigateAddApps: () -> Unit,
    modifier: Modifier = Modifier
) {
    val frameShape = CutCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
    val btnShape = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp, topEnd = 3.dp, bottomStart = 3.dp)
    val targetHz = maxOf(settings.frameRateTarget, settings.fpsGameTarget)

    Box(
        modifier = modifier
            .clip(frameShape)
            .background(Brush.radialGradient(listOf(Color(0xFF1A0912), Color(0xFF0A0C14), XGameDeepBlack)))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cut = 18.dp.toPx()
            val outerPath = Path().apply {
                moveTo(cut, 0f); lineTo(w - cut, 0f); lineTo(w, cut)
                lineTo(w, h - cut * 0.7f); lineTo(w - cut * 0.7f, h)
                lineTo(cut * 0.7f, h); lineTo(0f, h - cut * 0.7f); lineTo(0f, cut); close()
            }
            drawPath(outerPath, XGameNeonRed.copy(alpha = 0.25f * pulseAlpha), style = Stroke(width = 5.dp.toPx()))
            drawPath(
                path = outerPath,
                brush = Brush.horizontalGradient(listOf(XGameNeonRed, Color(0xFFFF6E7F), XGameNeonRed)),
                style = Stroke(width = 1.6.dp.toPx())
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(XGameNeonRed.copy(alpha = 0.20f * pulseAlpha), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.44f),
                    radius = minOf(w, h) * 0.46f
                ),
                radius = minOf(w, h) * 0.46f,
                center = Offset(w * 0.5f, h * 0.44f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = if (compactHeight) 8.dp else 11.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xB8090B12))
                    .border(1.dp, XGameNeonRed.copy(alpha = 0.38f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${telemetry.actualFps} FPS LIVE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = EmeraldBright
                )
                Text(
                    text = settings.cpuMode.label.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = XGameNeonRed
                )
                Text(
                    text = "${targetHz}Hz TARGET",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = ElectricCyan
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (compactHeight) 4.dp else 7.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Spacer(modifier = Modifier.weight(0.5f))
                Box(
                    modifier = Modifier
                        .size(if (compactHeight) 56.dp else 76.dp)
                        .clip(CutCornerShape(14.dp))
                        .background(XGameGraphiteElevated)
                        .border(2.dp, XGameNeonRed.copy(alpha = 0.9f), CutCornerShape(14.dp))
                        .clickable { if (selectedGame == null) onNavigateAddApps() else onStartPlay() },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedGameIcon != null) {
                        Image(
                            bitmap = selectedGameIcon,
                            contentDescription = selectedGame?.appName ?: "Selected Game",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.SportsEsports,
                            contentDescription = "Add Game",
                            tint = XGameNeonRed,
                            modifier = Modifier.size(if (compactHeight) 28.dp else 36.dp)
                        )
                    }
                }

                Text(
                    text = selectedGame?.appName ?: "NO GAME ARMED",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (compactHeight) 16.sp else 20.sp,
                        letterSpacing = 1.sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (selectedGame != null) {
                        "RAM ${telemetry.ramUsedPercent}% • FRAME ${String.format(Locale.US, "%.1f", telemetry.frameTimeMs)}ms"
                    } else {
                        "Tap + to arm games inside X-Game Space"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = TextSecondary,
                    maxLines = 1
                )

                Button(
                    onClick = { if (selectedGame == null) onNavigateAddApps() else onStartPlay() },
                    colors = ButtonDefaults.buttonColors(containerColor = XGameNeonRed, contentColor = Color.White),
                    shape = btnShape,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                    modifier = Modifier
                        .testTag("xgame_launch_btn")
                        .height(if (compactHeight) 34.dp else 42.dp)
                ) {
                    Icon(
                        imageVector = if (selectedGame == null) Icons.Rounded.Add else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (selectedGame == null) "ADD GAME" else "ENTER GAME",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    )
                }
                Spacer(modifier = Modifier.weight(0.5f))
            }

            if (addedGames.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(addedGames, key = { it.packageName }) { game ->
                        val active = game.packageName == selectedGame?.packageName
                        val iconBmp by produceState<ImageBitmap?>(
                            initialValue = peekAppIcon(game.packageName),
                            key1 = game.packageName
                        ) {
                            if (value == null) value = loadAppIconAsync(game.packageName)
                        }
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CutCornerShape(7.dp))
                                .background(if (active) XGameNeonRed.copy(alpha = 0.25f) else XGameGraphiteElevated)
                                .border(
                                    width = if (active) 1.6.dp else 1.dp,
                                    color = if (active) XGameNeonRed else XGameSteelBorder,
                                    shape = CutCornerShape(7.dp)
                                )
                                .clickable { onSelectGame(game.packageName) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (iconBmp != null) {
                                Image(
                                    bitmap = iconBmp!!,
                                    contentDescription = game.appName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().alpha(if (active) 1f else 0.72f)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.SportsEsports,
                                    contentDescription = game.appName,
                                    tint = if (active) XGameNeonRed else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun XGameControlsRightPanel(
    settings: GameTurboSettings,
    autoBrightness: Boolean,
    autoRotate: Boolean,
    pulseAlpha: Float,
    onCycleFrameRate: () -> Unit,
    onToggleTurboX: () -> Unit,
    onToggleGpuBoost: () -> Unit,
    onCycleCrosshair: () -> Unit,
    onToggleDnd: () -> Unit,
    onToggleNoNotif: () -> Unit,
    onCycleEqPreset: () -> Unit,
    onCycleScreenColor: () -> Unit,
    onToggleAutoRotate: () -> Unit,
    onToggleAutoBrightness: () -> Unit,
    onToggleActiveInfo: () -> Unit,
    onClearCache: () -> Unit,
    scrollableGrid: Boolean = true,
    modifier: Modifier = Modifier
) {
    val panelShape = CutCornerShape(topEnd = 14.dp, bottomStart = 14.dp, topStart = 4.dp, bottomEnd = 4.dp)
    val controls = remember(
        settings.frameRateTarget,
        settings.turboXMode,
        settings.increaseGpuPerformance,
        settings.crosshairStyle,
        settings.dndMode,
        settings.notificationMuteMode,
        settings.eqPreset,
        settings.screenColorMode,
        autoRotate,
        autoBrightness,
        settings.activeInfoHud
    ) {
        listOf(
            XGameControlSpec("hz", "${settings.frameRateTarget}Hz", Icons.Rounded.Speed, settings.frameRateTarget > 60, onCycleFrameRate),
            XGameControlSpec("turbox", "Turbo X", Icons.Rounded.RocketLaunch, settings.turboXMode, onToggleTurboX),
            XGameControlSpec("gpu", "GPU Boost", Icons.Rounded.Memory, settings.increaseGpuPerformance, onToggleGpuBoost),
            XGameControlSpec("aim", if (settings.crosshairStyle == CrosshairStyle.NONE) "Crosshair" else settings.crosshairStyle.label, Icons.Rounded.MyLocation, settings.crosshairStyle != CrosshairStyle.NONE, onCycleCrosshair),
            XGameControlSpec("dnd", "DND", Icons.Rounded.DarkMode, settings.dndMode, onToggleDnd),
            XGameControlSpec("mute", "Mute Notif", Icons.Rounded.NotificationsOff, settings.notificationMuteMode, onToggleNoNotif),
            XGameControlSpec("eq", if (settings.eqPreset == EqPreset.OFF) "Audio EQ" else settings.eqPreset.label, Icons.Rounded.GraphicEq, settings.eqPreset != EqPreset.OFF, onCycleEqPreset),
            XGameControlSpec("color", settings.screenColorMode.label, Icons.Rounded.ColorLens, settings.screenColorMode != ScreenColorMode.DEFAULT, onCycleScreenColor),
            XGameControlSpec("rot", "Rotate", Icons.Rounded.ScreenRotation, autoRotate, onToggleAutoRotate),
            XGameControlSpec("lit", "AutoLit", Icons.Rounded.BrightnessAuto, autoBrightness, onToggleAutoBrightness),
            XGameControlSpec("hud", "Active HUD", Icons.Rounded.MonitorHeart, settings.activeInfoHud, onToggleActiveInfo),
            XGameControlSpec("cache", "Clear", Icons.Rounded.CleaningServices, false, onClearCache)
        )
    }

    Column(
        modifier = modifier
            .clip(panelShape)
            .background(Brush.verticalGradient(listOf(XGameGraphite.copy(alpha = 0.95f), Color(0xEB08090F))))
            .border(
                width = 1.3.dp,
                brush = Brush.verticalGradient(
                    listOf(XGameNeonRed.copy(alpha = 0.85f * pulseAlpha), XGameCrimsonDeep, XGameSteelBorder)
                ),
                shape = panelShape
            )
            .padding(horizontal = 9.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "GAMING CONTROLS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = RajdhaniFontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    fontSize = 10.sp
                ),
                color = XGameNeonRed
            )
            Text(
                text = "${controls.count { it.active }} ACTIVE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = RajdhaniFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.5.sp
                ),
                color = EmeraldBright
            )
        }

        val rows = remember(controls) { controls.chunked(3) }
        val gridModifier = if (scrollableGrid) {
            Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
        } else {
            Modifier.fillMaxWidth()
        }
        Column(modifier = gridModifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
            rows.forEach { rowItems ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    rowItems.forEach { item -> XGameControlTile(spec = item, modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}

private data class XGameControlSpec(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val active: Boolean,
    val onClick: () -> Unit
)

@Composable
private fun XGameControlTile(spec: XGameControlSpec, modifier: Modifier = Modifier) {
    val tileShape = CutCornerShape(topStart = 7.dp, bottomEnd = 7.dp, topEnd = 2.dp, bottomStart = 2.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.91f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium),
        label = "xgame_tile_scale_${spec.id}"
    )
    val bg by animateColorAsState(
        targetValue = if (spec.active) XGameNeonRed.copy(alpha = 0.22f) else XGameGraphiteElevated,
        animationSpec = tween(180),
        label = "xgame_tile_bg_${spec.id}"
    )
    val border by animateColorAsState(
        targetValue = if (spec.active) XGameNeonRed else XGameSteelBorder,
        animationSpec = tween(180),
        label = "xgame_tile_border_${spec.id}"
    )

    Column(
        modifier = modifier
            .scale(scale)
            .minimumInteractiveComponentSize()
            .height(50.dp)
            .clip(tileShape)
            .background(bg)
            .border(if (spec.active) 1.3.dp else 1.dp, border, tileShape)
            .clickable(interactionSource = interactionSource, indication = null) { spec.onClick() }
            .padding(horizontal = 3.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = spec.icon,
            contentDescription = spec.label,
            tint = if (spec.active) XGameNeonRed else TextSecondary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = spec.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.5.sp,
                fontWeight = if (spec.active) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (spec.active) Color.White else TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun XGameBottomCommandBar(
    totalBoostCount: Int,
    brightnessFraction: Float,
    volumeFraction: Float,
    isOverlayServiceRunning: Boolean,
    compact: Boolean,
    onBrightnessChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onBoostRam: () -> Unit,
    onToggleOverlayService: () -> Unit,
    onOpenInAppOverlay: () -> Unit
) {
    val barShape = CutCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
    val pillShape = CutCornerShape(topStart = 7.dp, bottomEnd = 7.dp, topEnd = 2.dp, bottomStart = 2.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(barShape)
            .background(XGameGraphite.copy(alpha = 0.96f))
            .border(1.dp, XGameNeonRed.copy(alpha = 0.55f), barShape)
            .padding(horizontal = if (compact) 8.dp else 12.dp, vertical = if (compact) 4.dp else 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TurboXLogoMark(accentColor = XGameNeonRed, modifier = Modifier.size(if (compact) 20.dp else 24.dp))
            Column {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) { append("TURBO ") }
                        withStyle(SpanStyle(color = XGameNeonRed, fontWeight = FontWeight.Bold)) { append("X") }
                    },
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = RajdhaniFontFamily,
                        fontSize = if (compact) 13.sp else 15.sp,
                        letterSpacing = 1.4.sp
                    )
                )
                if (!compact) {
                    Text(
                        text = "$totalBoostCount BOOSTS",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, letterSpacing = 0.8.sp),
                        color = TextMuted
                    )
                }
            }
        }

        if (!compact) {
            Row(
                modifier = Modifier.weight(1f).padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                XGameCompactSlider(Icons.Rounded.WbSunny, brightnessFraction, XGameNeonRed, onBrightnessChange, Modifier.weight(1f))
                XGameCompactSlider(Icons.AutoMirrored.Rounded.VolumeUp, volumeFraction, ElectricCyan, onVolumeChange, Modifier.weight(1f))
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier
                    .testTag("xgame_onetap_btn")
                    .minimumInteractiveComponentSize()
                    .clip(pillShape)
                    .background(Brush.horizontalGradient(listOf(XGameNeonRed, XGameCrimsonDeep)))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), pillShape)
                    .clickable { onBoostRam() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Rounded.RocketLaunch, contentDescription = "One Tap Booster", tint = Color.White, modifier = Modifier.size(13.dp))
                Text(
                    text = "ONE TAP",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = Color.White
                )
            }

            Row(
                modifier = Modifier
                    .testTag("xgame_hud_toggle_btn")
                    .minimumInteractiveComponentSize()
                    .clip(pillShape)
                    .background(if (isOverlayServiceRunning) EmeraldBright.copy(alpha = 0.18f) else XGameGraphiteElevated)
                    .border(1.dp, if (isOverlayServiceRunning) EmeraldBright else XGameSteelBorder, pillShape)
                    .clickable { onToggleOverlayService() }
                    .padding(horizontal = 9.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Layers,
                    contentDescription = "Floating HUD Service",
                    tint = if (isOverlayServiceRunning) EmeraldBright else TextSecondary,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = if (isOverlayServiceRunning) "HUD ON" else "HUD OFF",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold),
                    color = if (isOverlayServiceRunning) EmeraldBright else TextPrimary
                )
            }

            if (!compact) {
                Row(
                    modifier = Modifier
                        .testTag("xgame_overlay_preview_btn")
                        .minimumInteractiveComponentSize()
                        .clip(pillShape)
                        .background(XGameGraphiteElevated)
                        .border(1.dp, XGameSteelBorder, pillShape)
                        .clickable { onOpenInAppOverlay() }
                        .padding(horizontal = 9.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Rounded.Tune, contentDescription = "Open Overlay Panel", tint = XGameNeonRed, modifier = Modifier.size(13.dp))
                    Text(
                        text = "PANEL",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun XGameCompactSlider(
    icon: ImageVector,
    value: Float,
    accent: Color,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent, inactiveTrackColor = XGameSteelBorder),
            modifier = Modifier.weight(1f).height(22.dp)
        )
    }
}
