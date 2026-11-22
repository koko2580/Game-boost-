package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSlate
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.LocalTurboAccent
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val SplashNavyCore = Color(0xFF041022)

@Composable
fun TurboSplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalTurboAccent.current.accent
    val secondary = LocalTurboAccent.current.secondaryGlow

    val ringReveal = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.72f) }
    val logoAlpha = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleOffsetY = remember { Animatable(24f) }
    val progress = remember { Animatable(0f) }
    val pulseWave = remember { Animatable(0f) }
    var canSkip by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "splash_orbit")
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing)
        ),
        label = "splash_outer_rot"
    )
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6500, easing = LinearEasing)
        ),
        label = "splash_inner_rot"
    )
    val glowBreath by infiniteTransition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "splash_glow"
    )

    LaunchedEffect(Unit) {
        launch {
            ringReveal.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 620, easing = FastOutSlowInEasing)
            )
        }
        launch {
            delay(80L)
            launch {
                logoAlpha.animateTo(1f, animationSpec = tween(380, easing = FastOutSlowInEasing))
            }
            launch {
                logoScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.62f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            launch {
                pulseWave.animateTo(1f, animationSpec = tween(900, easing = FastOutSlowInEasing))
            }
        }
        launch {
            delay(300L)
            launch {
                titleAlpha.animateTo(1f, animationSpec = tween(420, easing = FastOutSlowInEasing))
            }
            launch {
                titleOffsetY.animateTo(0f, animationSpec = tween(460, easing = FastOutSlowInEasing))
            }
        }
        launch {
            delay(200L)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1480, easing = FastOutSlowInEasing)
            )
        }
        delay(550L)
        canSkip = true
        delay(1300L)
        onFinished()
    }

    val statusText = when {
        progress.value < 0.35f -> "Initializing Security Guard..."
        progress.value < 0.72f -> "Calibrating FPS & Telemetry Engine..."
        progress.value < 0.96f -> "Syncing Adaptive Hardware Profile..."
        else -> "Turbo X Engine Ready"
    }

    BoxWithConstraints(
        modifier = modifier
            .testTag("turbo_splash_screen")
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        SplashNavyCore,
                        Color(0xFF060B17),
                        ObsidianVoid
                    )
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = canSkip
            ) { onFinished() }
            .safeDrawingPadding()
    ) {
        val isLandscape = maxWidth > maxHeight
        val dialSize = if (isLandscape) {
            (maxHeight * 0.62f).coerceIn(148.dp, 236.dp)
        } else if (maxWidth >= 600.dp) {
            256.dp
        } else {
            224.dp
        }

        // Subtle ambient crimson + cyan backdrop glow
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = if (isLandscape) size.width * 0.34f else size.width / 2f
            val cy = if (isLandscape) size.height * 0.50f else size.height * 0.44f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.18f * glowBreath),
                        secondary.copy(alpha = 0.07f * glowBreath),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = minOf(size.width, size.height) * 0.56f
                ),
                radius = minOf(size.width, size.height) * 0.56f,
                center = Offset(cx, cy)
            )
        }

        val emblemDial: @Composable () -> Unit = {
            Box(
                modifier = Modifier.size(dialSize),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val cx = w / 2f
                    val cy = h / 2f
                    val radius = minOf(w, h) * 0.47f

                    if (pulseWave.value in 0.01f..0.99f) {
                        drawCircle(
                            color = accent.copy(alpha = (1f - pulseWave.value) * 0.45f),
                            radius = radius * (0.65f + 0.42f * pulseWave.value),
                            center = Offset(cx, cy),
                            style = Stroke(width = 2.5.dp.toPx())
                        )
                    }

                    rotate(degrees = outerRotation, pivot = Offset(cx, cy)) {
                        val tickCount = 48
                        for (i in 0 until tickCount) {
                            val rad = (i * (360f / tickCount)) * (PI / 180.0)
                            val major = i % 4 == 0
                            val rOuter = radius * 0.98f * ringReveal.value
                            val rInner = radius * (if (major) 0.90f else 0.94f) * ringReveal.value
                            drawLine(
                                color = if (major) accent.copy(alpha = 0.78f) else CyberBorder.copy(alpha = 0.45f),
                                start = Offset(cx + (rInner * cos(rad)).toFloat(), cy + (rInner * sin(rad)).toFloat()),
                                end = Offset(cx + (rOuter * cos(rad)).toFloat(), cy + (rOuter * sin(rad)).toFloat()),
                                strokeWidth = if (major) 2.dp.toPx() else 1.1.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    rotate(degrees = innerRotation, pivot = Offset(cx, cy)) {
                        val arcRadius = radius * 0.82f
                        val topLeft = Offset(cx - arcRadius, cy - arcRadius)
                        val arcSize = Size(arcRadius * 2f, arcRadius * 2f)
                        drawArc(
                            color = accent.copy(alpha = 0.85f * ringReveal.value),
                            startAngle = 18f,
                            sweepAngle = 110f * ringReveal.value,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = ElectricCyan.copy(alpha = 0.72f * ringReveal.value),
                            startAngle = 198f,
                            sweepAngle = 110f * ringReveal.value,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    drawCircle(
                        color = SplashNavyCore,
                        radius = radius * 0.72f,
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = accent.copy(alpha = 0.42f * glowBreath),
                        radius = radius * 0.72f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.4.dp.toPx())
                    )
                }

                Image(
                    painter = painterResource(id = R.drawable.ic_turbo_x_logo),
                    contentDescription = "Turbo X Emblem",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(dialSize * 0.60f)
                        .clip(CircleShape)
                        .scale(logoScale.value)
                        .alpha(logoAlpha.value)
                )
            }
        }

        val titleBlock: @Composable (Alignment.Horizontal) -> Unit = { align ->
            Column(
                horizontalAlignment = align,
                modifier = Modifier
                    .alpha(titleAlpha.value)
                    .padding(top = titleOffsetY.value.dp)
            ) {
                Text(
                    text = "PRECISION PERFORMANCE ENGINE",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.2.sp
                    ),
                    color = accent
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "FPS • TELEMETRY • OVERDRIVE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 2.sp
                    ),
                    color = TextSecondary
                )
            }
        }

        val progressBlock: @Composable (Alignment.Horizontal) -> Unit = { align ->
            Column(
                modifier = Modifier
                    .widthIn(max = 380.dp)
                    .fillMaxWidth()
                    .alpha(titleAlpha.value),
                horizontalAlignment = align,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "${(progress.value * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = TextPrimary
                    )
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                ) {
                    drawRoundRect(
                        color = CyberSlate,
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )
                    val fillWidth = (size.width * progress.value).coerceAtLeast(0f)
                    if (fillWidth > 0f) {
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(accent, ElectricCyan)
                            ),
                            size = Size(fillWidth, size.height),
                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberSlate.copy(alpha = 0.55f))
                        .border(1.dp, CyberBorder.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VerifiedUser,
                        contentDescription = null,
                        tint = EmeraldBright,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "SHIELDED RUNTIME • TURBO X",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 10.sp
                        ),
                        color = TextMuted
                    )
                }
            }
        }

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                emblemDial()
                Spacer(modifier = Modifier.width(36.dp))
                Column(
                    modifier = Modifier.widthIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    titleBlock(Alignment.Start)
                    progressBlock(Alignment.Start)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    emblemDial()
                    Spacer(modifier = Modifier.height(18.dp))
                    titleBlock(Alignment.CenterHorizontally)
                }
                progressBlock(Alignment.CenterHorizontally)
            }
        }
    }
}
