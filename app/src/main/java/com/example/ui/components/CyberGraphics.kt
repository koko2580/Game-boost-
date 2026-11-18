package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CpuMode
import com.example.data.CrosshairStyle
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSlate
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.LocalTurboAccent
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

val SlantedTabShape = GenericShape { size, _ ->
    val slant = size.height * 0.65f
    moveTo(slant, 0f)
    lineTo(size.width - slant, 0f)
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    close()
}

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

@Composable
fun TurboTopLogoCorner(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(96.dp)
            .height(50.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF212638), Color(0xFF141824))
                ),
                shape = RoundedCornerShape(bottomEnd = 22.dp)
            )
            .border(
                width = 1.dp,
                color = Color(0xFF39415A),
                shape = RoundedCornerShape(bottomEnd = 22.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(34.dp, 20.dp)) {
            val w = size.width
            val h = size.height
            val path = Path().apply {
                moveTo(w * 0.15f, h * 0.15f)
                lineTo(w * 0.78f, h * 0.15f)
                lineTo(w * 0.92f, h * 0.50f)
                lineTo(w * 0.72f, h * 0.85f)
                lineTo(w * 0.08f, h * 0.85f)
                lineTo(w * 0.25f, h * 0.58f)
                lineTo(w * 0.62f, h * 0.58f)
                lineTo(w * 0.68f, h * 0.42f)
                lineTo(w * 0.28f, h * 0.42f)
                close()
            }
            drawPath(
                path = path,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFD6DCEB), Color(0xFF7D869E))
                )
            )
        }
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
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "switch_thumb"
    )
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .size(width = 52.dp, height = 30.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(if (checked) Color(0xFF1F2333) else Color(0xFF1E2230))
            .border(
                width = 1.dp,
                color = if (checked) accent.copy(alpha = 0.65f) else CyberBorder,
                shape = RoundedCornerShape(15.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Switch
            ) { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(22.dp)
                .clip(CircleShape)
                .background(if (checked) accent else Color(0xFF8C92A4))
        )
    }
}

@Composable
fun TurboXEmblem(
    glowColor: Color,
    animatePulse: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "x_emblem_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    val effectivePulse = if (animatePulse) pulse else 0.85f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val arm = minOf(w, h) * 0.42f

        // Radial energy flare behind the X
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    glowColor.copy(alpha = 0.55f * effectivePulse),
                    glowColor.copy(alpha = 0.16f * effectivePulse),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = arm * 1.45f
            ),
            radius = arm * 1.45f,
            center = Offset(cx, cy)
        )

        // Diagonal energy rays
        for (signX in listOf(-1f, 1f)) {
            for (signY in listOf(-1f, 1f)) {
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.9f * effectivePulse),
                            Color.Transparent
                        ),
                        start = Offset(cx, cy),
                        end = Offset(cx + signX * arm * 1.28f, cy + signY * arm * 1.28f)
                    ),
                    start = Offset(cx, cy),
                    end = Offset(cx + signX * arm * 1.28f, cy + signY * arm * 1.28f),
                    strokeWidth = arm * 0.11f,
                    cap = StrokeCap.Round
                )
            }
        }

        // Metallic X silhouette
        val xPath = Path().apply {
            val outer = arm * 0.96f
            val inner = arm * 0.25f
            val notch = arm * 0.42f
            moveTo(cx - outer, cy - outer)
            lineTo(cx - notch, cy - outer)
            lineTo(cx, cy - inner)
            lineTo(cx + notch, cy - outer)
            lineTo(cx + outer, cy - outer)
            lineTo(cx + inner, cy)
            lineTo(cx + outer, cy + outer)
            lineTo(cx + notch, cy + outer)
            lineTo(cx, cy + inner)
            lineTo(cx - notch, cy + outer)
            lineTo(cx - outer, cy + outer)
            lineTo(cx - inner, cy)
            close()
        }

        drawPath(
            path = xPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF474E63),
                    Color(0xFF1F2330),
                    Color(0xFF3A4154),
                    Color(0xFF151822)
                ),
                start = Offset(cx - arm, cy - arm),
                end = Offset(cx + arm, cy + arm)
            )
        )

        drawPath(
            path = xPath,
            color = Color(0xFF96A2BE).copy(alpha = 0.7f),
            style = Stroke(width = 3.2f)
        )

        // Glowing inner cyber circuit lines
        val strokeW = (arm * 0.045f).coerceAtLeast(3f)
        drawLine(
            color = glowColor.copy(alpha = 0.85f * effectivePulse),
            start = Offset(cx - arm * 0.78f, cy - arm * 0.78f),
            end = Offset(cx + arm * 0.78f, cy + arm * 0.78f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        drawLine(
            color = glowColor.copy(alpha = 0.85f * effectivePulse),
            start = Offset(cx + arm * 0.78f, cy - arm * 0.78f),
            end = Offset(cx - arm * 0.78f, cy + arm * 0.78f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.9f * effectivePulse),
            radius = arm * 0.06f,
            center = Offset(cx, cy)
        )
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

            // Ambient under-glow
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(ringColor.copy(alpha = 0.42f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = w * 0.5f
                ),
                topLeft = Offset(0f, 0f),
                size = size
            )

            // Outer glowing ring
            drawOval(
                color = ringColor,
                topLeft = Offset(w * 0.04f, h * 0.14f),
                size = Size(w * 0.92f, h * 0.72f),
                style = Stroke(width = 5.5f)
            )
            // Middle dashed/arc ring
            drawArc(
                color = ringColor.copy(alpha = 0.85f),
                startAngle = if (animate) spinPhase else 30f,
                sweepAngle = 210f,
                useCenter = false,
                topLeft = Offset(w * 0.15f, h * 0.24f),
                size = Size(w * 0.70f, h * 0.52f),
                style = Stroke(width = 3.2f, cap = StrokeCap.Round)
            )
            // Inner upper floating ring
            drawOval(
                color = ringColor.copy(alpha = 0.9f),
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
            color = ringColor
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
