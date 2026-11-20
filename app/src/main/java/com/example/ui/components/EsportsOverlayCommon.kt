package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CpuMode
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSlate
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal data class WingToolItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val isActive: Boolean,
    val onClick: () -> Unit
)

internal data class RightTileItem(
    val id: String,
    val label: String,
    val icon: ImageVector?,
    val customBadgeText: String? = null,
    val isActive: Boolean,
    val onClick: () -> Unit
)

@Composable
internal fun OverlayActionIconRow(
    accentColor: Color,
    containerColor: Color,
    shape: Shape = CircleShape,
    callbacks: OverlayCallbacks
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        EsportsActionIconBtn(Icons.Rounded.Home, "Home", accentColor, containerColor, shape, "overlay_home", callbacks.onHomeClick)
        EsportsActionIconBtn(Icons.Rounded.Settings, "Settings", accentColor, containerColor, shape, "overlay_settings", callbacks.onSettingsClick)
        EsportsActionIconBtn(Icons.Rounded.Gamepad, "Games", accentColor, containerColor, shape, "overlay_games", callbacks.onGamesClick)
        EsportsActionIconBtn(Icons.Rounded.PowerSettingsNew, "Close Overlay", Color(0xFFFF3D57), containerColor, shape, "overlay_power", callbacks.onPowerExitClick)
    }
}

@Composable
internal fun EsportsActionIconBtn(
    icon: ImageVector,
    description: String,
    tint: Color,
    containerColor: Color,
    shape: Shape,
    tag: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "esports_btn_scale"
    )
    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .scale(scale)
            .testTag(tag)
            .size(34.dp)
            .clip(shape)
            .background(containerColor)
            .border(1.dp, tint.copy(alpha = 0.45f), shape)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = TextPrimary, modifier = Modifier.size(17.dp))
    }
}

@Composable
internal fun EsportsToolCard(
    label: String,
    icon: ImageVector?,
    badgeText: String?,
    active: Boolean,
    activeAccent: Color,
    inactiveContainer: Color,
    shape: Shape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardHeight: Dp = 50.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium),
        label = "tool_press_scale"
    )
    val bg by animateColorAsState(
        targetValue = if (active) activeAccent.copy(alpha = 0.22f) else inactiveContainer,
        animationSpec = tween(200),
        label = "tool_bg"
    )
    val border by animateColorAsState(
        targetValue = if (active) activeAccent else CyberBorder.copy(alpha = 0.65f),
        animationSpec = tween(200),
        label = "tool_border"
    )

    Column(
        modifier = modifier
            .scale(scale)
            .height(cardHeight)
            .clip(shape)
            .background(bg)
            .border(if (active) 1.3.dp else 1.dp, border, shape)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 5.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (badgeText != null) {
            Text(
                text = badgeText,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = RajdhaniFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = if (active) activeAccent else TextPrimary,
                maxLines = 1
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) activeAccent else TextPrimary,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = if (active) TextPrimary else TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun EsportsMetricMeter(
    label: String,
    valueText: String,
    progress: Float,
    accentColor: Color,
    trackColor: Color = CyberSlate,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0.08f, 1f),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
        label = "esports_meter"
    )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = RajdhaniFontFamily,
                    letterSpacing = 0.9.sp,
                    fontSize = 10.sp
                ),
                color = TextMuted
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = RajdhaniFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = TextPrimary
            )
        }
        LinearProgressIndicator(
            progress = { animatedProgress },
            color = accentColor,
            trackColor = trackColor,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )
    }
}

@Composable
internal fun EsportsHorizontalSliderRow(
    icon: ImageVector,
    label: String,
    value: Float,
    accentColor: Color,
    onValueChange: (Float) -> Unit
) {
    val clamped = value.coerceIn(0.05f, 1f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = label, tint = accentColor, modifier = Modifier.size(16.dp))
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val frac = (offset.x / size.width.toFloat()).coerceIn(0.05f, 1f)
                        onValueChange(frac)
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        val frac = (change.position.x / size.width.toFloat()).coerceIn(0.05f, 1f)
                        onValueChange(frac)
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val trackH = 8.dp.toPx()
                val topY = (size.height - trackH) / 2f
                val corner = CornerRadius(trackH / 2f)
                drawRoundRect(
                    color = CyberSlate,
                    topLeft = Offset(0f, topY),
                    size = Size(size.width, trackH),
                    cornerRadius = corner
                )
                val fillW = (size.width * clamped).coerceAtLeast(trackH)
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(accentColor.copy(alpha = 0.78f), accentColor)),
                    topLeft = Offset(0f, topY),
                    size = Size(fillW, trackH),
                    cornerRadius = corner
                )
                val thumbX = fillW.coerceIn(7.dp.toPx(), (size.width - 7.dp.toPx()).coerceAtLeast(7.dp.toPx()))
                drawCircle(
                    color = Color.White,
                    radius = 6.5.dp.toPx(),
                    center = Offset(thumbX, size.height / 2f)
                )
                drawCircle(
                    color = accentColor,
                    radius = 3.dp.toPx(),
                    center = Offset(thumbX, size.height / 2f)
                )
            }
        }
        Text(
            text = "${(clamped * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold),
            color = TextSecondary,
            modifier = Modifier.width(34.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
internal fun EsportsVerticalNeonSlider(
    icon: ImageVector,
    label: String,
    value: Float,
    accentColor: Color,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val clamped = value.coerceIn(0.05f, 1f)
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "${(clamped * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp),
            color = accentColor,
            maxLines = 1
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .width(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF101522))
                .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val frac = (1f - (offset.y / size.height.toFloat())).coerceIn(0.05f, 1f)
                        onValueChange(frac)
                    }
                }
                .pointerInput(Unit) {
                    detectVerticalDragGestures { change, _ ->
                        change.consume()
                        val frac = (1f - (change.position.y / size.height.toFloat())).coerceIn(0.05f, 1f)
                        onValueChange(frac)
                    }
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                val segments = 8
                val gap = 3.dp.toPx()
                val segH = (size.height - gap * (segments - 1)) / segments
                val litCount = (clamped * segments).toInt().coerceIn(1, segments)
                for (i in 0 until segments) {
                    val idxFromBottom = segments - 1 - i
                    val top = i * (segH + gap)
                    val isLit = idxFromBottom < litCount
                    drawRoundRect(
                        color = if (isLit) accentColor else Color(0xFF1D2436),
                        topLeft = Offset(0f, top),
                        size = Size(size.width, segH),
                        cornerRadius = CornerRadius(2.5.dp.toPx())
                    )
                }
            }
        }
        Icon(icon, contentDescription = label, tint = accentColor, modifier = Modifier.size(16.dp))
    }
}

@Composable
internal fun EsportsQuickAppBar(
    shape: Shape,
    callbacks: OverlayCallbacks
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        QuickShortcutChip("Browser", Icons.Rounded.Public, Color(0xFF1A73E8), shape, Modifier.weight(1f)) {
            callbacks.onQuickApp(listOf("com.android.chrome"), "https://www.google.com")
        }
        QuickShortcutChip("Chat", Icons.Rounded.Forum, Color(0xFF0088CC), shape, Modifier.weight(1f)) {
            callbacks.onQuickApp(listOf("org.telegram.messenger", "com.whatsapp"), "https://web.telegram.org")
        }
        QuickShortcutChip("Video", Icons.Rounded.VideoLibrary, Color(0xFFE53935), shape, Modifier.weight(1f)) {
            callbacks.onQuickApp(listOf("com.google.android.youtube"), "https://m.youtube.com")
        }
    }
}

@Composable
private fun QuickShortcutChip(
    label: String,
    icon: ImageVector,
    color: Color,
    shape: Shape,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .height(38.dp)
            .clip(shape)
            .background(color.copy(alpha = 0.16f))
            .border(1.dp, color.copy(alpha = 0.55f), shape)
            .clickable { onClick() }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimary,
            maxLines = 1
        )
    }
}

@Composable
internal fun SpinningCoolingFanGlyph(
    cpuMode: CpuMode,
    animate: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
    sizeDp: Dp = 36.dp
) {
    val cycleMs = when (cpuMode) {
        CpuMode.ENERGY_SAVING -> 2800
        CpuMode.BALANCED -> 1600
        CpuMode.ACCELERATION -> 850
    }
    val angle: Float
    val pulse: Float
    if (animate) {
        val infiniteTransition = rememberInfiniteTransition(label = "redmagic_fan_spin")
        val animatedAngle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = cycleMs, easing = LinearEasing)
            ),
            label = "fan_angle"
        )
        val animatedPulse by infiniteTransition.animateFloat(
            initialValue = 0.55f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "fan_glow_pulse"
        )
        angle = animatedAngle
        pulse = animatedPulse
    } else {
        angle = 18f
        pulse = 0.75f
    }

    Canvas(modifier = Modifier.size(sizeDp)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = minOf(size.width, size.height) * 0.45f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primaryColor.copy(alpha = 0.35f * pulse), Color.Transparent),
                center = Offset(cx, cy),
                radius = r * 1.25f
            ),
            radius = r * 1.25f,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = secondaryColor.copy(alpha = 0.85f),
            radius = r,
            center = Offset(cx, cy),
            style = Stroke(width = 1.6.dp.toPx())
        )
        rotate(degrees = angle, pivot = Offset(cx, cy)) {
            val bladeCount = 6
            for (i in 0 until bladeCount) {
                val deg = i * (360f / bladeCount)
                val rad1 = (deg) * (PI / 180.0)
                val rad2 = (deg + 28f) * (PI / 180.0)
                val blade = Path().apply {
                    moveTo(cx, cy)
                    lineTo(
                        cx + (r * 0.85f * cos(rad1)).toFloat(),
                        cy + (r * 0.85f * sin(rad1)).toFloat()
                    )
                    lineTo(
                        cx + (r * 0.72f * cos(rad2)).toFloat(),
                        cy + (r * 0.72f * sin(rad2)).toFloat()
                    )
                    close()
                }
                drawPath(blade, color = if (i % 2 == 0) primaryColor else secondaryColor.copy(alpha = 0.9f))
            }
        }
        drawCircle(
            color = Color(0xFF0A0D16),
            radius = r * 0.28f,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = Color.White,
            radius = r * 0.13f,
            center = Offset(cx, cy)
        )
    }
}
