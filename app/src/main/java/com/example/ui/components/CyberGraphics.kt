package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CpuMode
import com.example.data.CrosshairStyle
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberElevated
import com.example.ui.theme.CyberSlate
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.LocalTurboAccent
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val HexBadgeShape = GenericShape { size, _ ->
    val cut = size.height * 0.28f
    moveTo(cut, 0f)
    lineTo(size.width - cut, 0f)
    lineTo(size.width, size.height * 0.5f)
    lineTo(size.width - cut, size.height)
    lineTo(cut, size.height)
    lineTo(0f, size.height * 0.5f)
    close()
}

fun cpuModeColor(mode: CpuMode, defaultAccent: Color): Color = when (mode) {
    CpuMode.ENERGY_SAVING -> ElectricCyan
    CpuMode.BALANCED -> CyberAmber
    CpuMode.ACCELERATION -> defaultAccent
}

/**
 * Original Turbo X vector emblem — twin turbine chevrons inside a stealth hex-core.
 */
@Composable
fun TurboXLogoMark(
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        // Outer subtle hex-shield stroke
        val shield = Path().apply {
            moveTo(cx, h * 0.06f)
            lineTo(w * 0.90f, h * 0.28f)
            lineTo(w * 0.90f, h * 0.72f)
            lineTo(cx, h * 0.94f)
            lineTo(w * 0.10f, h * 0.72f)
            lineTo(w * 0.10f, h * 0.28f)
            close()
        }
        drawPath(
            path = shield,
            brush = Brush.linearGradient(
                colors = listOf(CyberElevated, MidnightSurface)
            )
        )
        drawPath(
            path = shield,
            color = accentColor.copy(alpha = 0.7f),
            style = Stroke(width = (w * 0.055f).coerceAtLeast(1.8f))
        )

        // Precision dual opposing blades (TX turbine mark)
        val bladeLeft = Path().apply {
            moveTo(w * 0.28f, h * 0.30f)
            lineTo(w * 0.48f, h * 0.50f)
            lineTo(w * 0.28f, h * 0.70f)
        }
        val bladeRight = Path().apply {
            moveTo(w * 0.72f, h * 0.30f)
            lineTo(w * 0.52f, h * 0.50f)
            lineTo(w * 0.72f, h * 0.70f)
        }
        val stroke = Stroke(width = (w * 0.10f).coerceAtLeast(2.4f), cap = StrokeCap.Round)
        drawPath(bladeLeft, color = Color.White, style = stroke)
        drawPath(bladeRight, color = accentColor, style = stroke)
    }
}

/**
 * Original sleek Material 3 top bar for subscreens (Settings, Permissions, Add Apps).
 */
@Composable
fun TurboXTopBar(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
    backTestTag: String,
    trailingContent: @Composable RowScope.() -> Unit = {}
) {
    val accent = LocalTurboAccent.current.accent
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MidnightSurface.copy(alpha = 0.96f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag(backTestTag)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                TurboXLogoMark(
                    accentColor = accent,
                    modifier = Modifier.size(30.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(end = 8.dp),
                content = trailingContent
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.45f),
                            CyberBorder.copy(alpha = 0.6f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

@Composable
fun CyberToggleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    val accent = LocalTurboAccent.current.accent
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 24.dp else 4.dp,
        animationSpec = spring(
            dampingRatio = 0.62f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "switch_thumb"
    )
    val trackColor by animateColorAsState(
        targetValue = if (checked) accent.copy(alpha = 0.22f) else CyberSlate,
        animationSpec = tween(durationMillis = 200),
        label = "switch_track"
    )
    val borderColor by animateColorAsState(
        targetValue = if (checked) accent.copy(alpha = 0.85f) else CyberBorder,
        animationSpec = tween(durationMillis = 200),
        label = "switch_border"
    )
    val thumbColor by animateColorAsState(
        targetValue = if (checked) accent else Color(0xFF8C95AD),
        animationSpec = tween(durationMillis = 200),
        label = "switch_dot"
    )
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Switch
            ) { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 52.dp, height = 30.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(trackColor)
                .border(
                    width = 1.2.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(15.dp)
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(thumbColor)
            )
        }
    }
}

/**
 * Original Turbo X Precision Telemetry Reactor Dial.
 * Clean 270° automotive/ROG-style tachometer arc with glowing sweep tip and uncluttered center readout.
 */
@Composable
fun TurboCockpitReactor(
    glowColor: Color,
    secondaryColor: Color = ElectricCyan,
    actualFps: Int = 60,
    cpuFreqMHz: Int = 1800,
    ramUsedPercent: Int = 45,
    modeLabel: String = "ACCELERATION",
    gameIcon: ImageBitmap? = null,
    animatePulse: Boolean = true,
    centerTitle: String? = null,
    modifier: Modifier = Modifier
) {
    val animatedGlow by animateColorAsState(
        targetValue = glowColor,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "reactor_glow"
    )
    val fpsRatio = (actualFps / 120f).coerceIn(0.18f, 1f)
    val cpuRatio = (cpuFreqMHz / 3200f).coerceIn(0.22f, 1f)
    val animatedMainSweep by animateFloatAsState(
        targetValue = if (centerTitle != null) 0.85f else ((fpsRatio * 0.55f) + (cpuRatio * 0.45f)).coerceIn(0.24f, 0.96f),
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessLow),
        label = "reactor_main_arc"
    )
    val animatedRamSweep by animateFloatAsState(
        targetValue = (ramUsedPercent / 100f).coerceIn(0.12f, 0.96f),
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessLow),
        label = "reactor_ram_arc"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "cockpit_reactor_loop")
    val outerOrbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing)
        ),
        label = "orbit_angle"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.70f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "reactor_pulse"
    )

    val effectiveAngle = if (animatePulse) outerOrbitAngle else 0f
    val effectivePulse = if (animatePulse) pulseAlpha else 0.85f

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val radius = minOf(w, h) * 0.46f

            // Subtle ambient radial halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedGlow.copy(alpha = 0.22f * effectivePulse),
                        secondaryColor.copy(alpha = 0.06f * effectivePulse),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = radius * 1.12f
                ),
                radius = radius * 1.12f,
                center = Offset(cx, cy)
            )

            // Outer precision tick ring (slowly rotating)
            rotate(degrees = effectiveAngle, pivot = Offset(cx, cy)) {
                val tickCount = 48
                val tickOuter = radius * 0.99f
                val tickInnerMajor = radius * 0.90f
                val tickInnerMinor = radius * 0.94f
                for (i in 0 until tickCount) {
                    val angleRad = (i * (360f / tickCount)) * (PI / 180.0)
                    val isMajor = i % 4 == 0
                    val innerR = if (isMajor) tickInnerMajor else tickInnerMinor
                    val start = Offset(
                        cx + (innerR * cos(angleRad)).toFloat(),
                        cy + (innerR * sin(angleRad)).toFloat()
                    )
                    val end = Offset(
                        cx + (tickOuter * cos(angleRad)).toFloat(),
                        cy + (tickOuter * sin(angleRad)).toFloat()
                    )
                    drawLine(
                        color = if (isMajor) animatedGlow.copy(alpha = 0.65f) else CyberBorder.copy(alpha = 0.50f),
                        start = start,
                        end = end,
                        strokeWidth = if (isMajor) 2.dp.toPx() else 1.1.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // 270-degree open-bottom main power arc
            val mainRadius = radius * 0.81f
            val mainTopLeft = Offset(cx - mainRadius, cy - mainRadius)
            val mainSize = Size(mainRadius * 2f, mainRadius * 2f)
            val startDeg = 135f
            val fullSweep = 270f
            val activeSweep = fullSweep * animatedMainSweep

            drawArc(
                color = CyberSlate.copy(alpha = 0.85f),
                startAngle = startDeg,
                sweepAngle = fullSweep,
                useCenter = false,
                topLeft = mainTopLeft,
                size = mainSize,
                style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(animatedGlow, secondaryColor, animatedGlow),
                    center = Offset(cx, cy)
                ),
                startAngle = startDeg,
                sweepAngle = activeSweep,
                useCenter = false,
                topLeft = mainTopLeft,
                size = mainSize,
                style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
            )

            // Glowing tip dot at end of main sweep
            val tipRad = (startDeg + activeSweep) * (PI / 180.0)
            val tipCenter = Offset(
                cx + (mainRadius * cos(tipRad)).toFloat(),
                cy + (mainRadius * sin(tipRad)).toFloat()
            )
            drawCircle(
                color = animatedGlow.copy(alpha = 0.40f),
                radius = 7.dp.toPx(),
                center = tipCenter
            )
            drawCircle(
                color = Color.White,
                radius = 3.2.dp.toPx(),
                center = tipCenter
            )

            // Subtle inner cyan RAM progress ring
            val ramRadius = radius * 0.68f
            val ramTopLeft = Offset(cx - ramRadius, cy - ramRadius)
            val ramSize = Size(ramRadius * 2f, ramRadius * 2f)
            drawArc(
                color = CyberBorder.copy(alpha = 0.30f),
                startAngle = 150f,
                sweepAngle = 240f,
                useCenter = false,
                topLeft = ramTopLeft,
                size = ramSize,
                style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = secondaryColor.copy(alpha = 0.85f),
                startAngle = 150f,
                sweepAngle = 240f * animatedRamSweep,
                useCenter = false,
                topLeft = ramTopLeft,
                size = ramSize,
                style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
            )

            // Clean inner obsidian core disc
            drawCircle(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF151C2F), Color(0xFF0A0E18))
                ),
                radius = radius * 0.60f,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = animatedGlow.copy(alpha = 0.32f),
                radius = radius * 0.60f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.2.dp.toPx())
            )
        }

        // Uncluttered, centered readout with generous padding
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (centerTitle != null) {
                TurboXLogoMark(
                    accentColor = animatedGlow,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = centerTitle,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.8.sp
                    ),
                    color = animatedGlow
                )
            } else {
                Text(
                    text = "$actualFps",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 38.sp,
                        lineHeight = 38.sp
                    ),
                    color = TextPrimary
                )
                Text(
                    text = "FPS",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.2.sp,
                        fontSize = 12.sp
                    ),
                    color = animatedGlow
                )
            }
        }
    }
}

@Composable
fun HolographicGaugeRing(
    valueText: String,
    unitText: String,
    bottomLabel: String,
    ringColor: Color,
    animate: Boolean = true,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ring_spin")
    val spinPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing)
        ),
        label = "ring_angle"
    )
    val animatedRingColor by animateColorAsState(
        targetValue = ringColor,
        animationSpec = tween(300),
        label = "gauge_ring_color"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = valueText,
            style = MaterialTheme.typography.displayMedium.copy(
                fontFamily = RajdhaniFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            ),
            color = TextPrimary
        )
        if (unitText.isNotEmpty()) {
            Text(
                text = unitText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = RajdhaniFontFamily,
                    fontSize = 12.sp
                ),
                color = TextMuted
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Canvas(
            modifier = Modifier
                .width(116.dp)
                .height(48.dp)
        ) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h * 0.52f

            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(animatedRingColor.copy(alpha = 0.42f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = w * 0.5f
                ),
                topLeft = Offset(0f, 0f),
                size = size
            )
            drawOval(
                color = animatedRingColor,
                topLeft = Offset(w * 0.04f, h * 0.14f),
                size = Size(w * 0.92f, h * 0.72f),
                style = Stroke(width = 5.5f)
            )
            drawArc(
                color = animatedRingColor.copy(alpha = 0.85f),
                startAngle = if (animate) spinPhase else 30f,
                sweepAngle = 210f,
                useCenter = false,
                topLeft = Offset(w * 0.15f, h * 0.24f),
                size = Size(w * 0.70f, h * 0.52f),
                style = Stroke(width = 3.2f, cap = StrokeCap.Round)
            )
            drawOval(
                color = animatedRingColor.copy(alpha = 0.9f),
                topLeft = Offset(w * 0.26f, h * 0.06f),
                size = Size(w * 0.48f, h * 0.34f),
                style = Stroke(width = 3.2f)
            )
        }
        Text(
            text = bottomLabel,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = RajdhaniFontFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp
            ),
            color = animatedRingColor
        )
    }
}

@Composable
fun SegmentedVerticalBar(
    fraction: Float,
    onFractionChange: (Float) -> Unit,
    icon: ImageVector,
    iconDescription: String,
    activeColor: Color = Color.White,
    barHeight: Dp = 112.dp,
    modifier: Modifier = Modifier
) {
    val segments = 14
    val clamped = fraction.coerceIn(0f, 1f)
    val activeCount = (clamped * segments).toInt().coerceIn(1, segments)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(34.dp)
                .height(barHeight)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { change, _ ->
                        change.consume()
                        val newFraction = 1f - (change.position.y / size.height.toFloat())
                        onFractionChange(newFraction.coerceIn(0.05f, 1f))
                    }
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    val stepped = ((clamped * 4).toInt() + 1) / 4f
                    onFractionChange(if (clamped >= 0.95f) 0.25f else stepped.coerceIn(0.25f, 1f))
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val segHeight = (size.height - (segments - 1) * 3.dp.toPx()) / segments
                val segGap = 3.dp.toPx()
                for (i in 0 until segments) {
                    val indexFromBottom = segments - 1 - i
                    val isLit = indexFromBottom < activeCount
                    val top = i * (segHeight + segGap)
                    drawRoundRect(
                        color = if (isLit) activeColor else Color(0xFF2B3042),
                        topLeft = Offset(3.dp.toPx(), top),
                        size = Size(size.width - 6.dp.toPx(), segHeight),
                        cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Icon(
            imageVector = icon,
            contentDescription = iconDescription,
            tint = TextPrimary,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun CrosshairOverlay(
    style: CrosshairStyle,
    modifier: Modifier = Modifier
) {
    if (style == CrosshairStyle.NONE) return
    val accent = LocalTurboAccent.current.accentBright
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(48.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            when (style) {
                CrosshairStyle.DOT -> {
                    drawCircle(color = accent, radius = 4.dp.toPx(), center = Offset(cx, cy))
                    drawCircle(
                        color = Color.White,
                        radius = 6.dp.toPx(),
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
                CrosshairStyle.RETICLE -> {
                    drawCircle(
                        color = ElectricCyan,
                        radius = 12.dp.toPx(),
                        center = Offset(cx, cy),
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawCircle(color = accent, radius = 2.5.dp.toPx(), center = Offset(cx, cy))
                    val gap = 7.dp.toPx()
                    val len = 18.dp.toPx()
                    drawLine(ElectricCyan, Offset(cx - len, cy), Offset(cx - gap, cy), 2.dp.toPx())
                    drawLine(ElectricCyan, Offset(cx + gap, cy), Offset(cx + len, cy), 2.dp.toPx())
                    drawLine(ElectricCyan, Offset(cx, cy - len), Offset(cx, cy - gap), 2.dp.toPx())
                    drawLine(ElectricCyan, Offset(cx, cy + gap), Offset(cx, cy + len), 2.dp.toPx())
                }
                CrosshairStyle.SNIPER -> {
                    val r = 18.dp.toPx()
                    drawCircle(
                        color = EmeraldBright,
                        radius = r,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawLine(EmeraldBright, Offset(0f, cy), Offset(size.width, cy), 1.4.dp.toPx())
                    drawLine(EmeraldBright, Offset(cx, 0f), Offset(cx, size.height), 1.4.dp.toPx())
                    drawCircle(accent, radius = 2.dp.toPx(), center = Offset(cx, cy))
                }
                CrosshairStyle.NONE -> Unit
            }
        }
    }
}
