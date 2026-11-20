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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
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
    modifier: Modifier = Modifier
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
            .height(58.dp)
            .clip(shape)
            .background(bg)
            .border(if (active) 1.3.dp else 1.dp, border, shape)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 6.dp, vertical = 6.dp),
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = label, tint = accentColor, modifier = Modifier.size(18.dp))
        Slider(
            value = value.coerceIn(0.05f, 1f),
            onValueChange = onValueChange,
            valueRange = 0.05f..1f,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = accentColor,
                inactiveTrackColor = CyberSlate
            ),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${(value * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = RajdhaniFontFamily),
            color = TextSecondary,
            modifier = Modifier.width(34.dp),
            textAlign = TextAlign.End
        )
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
    val infiniteTransition = rememberInfiniteTransition(label = "redmagic_fan_spin")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = cycleMs, easing = LinearEasing)
        ),
        label = "fan_angle"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fan_glow_pulse"
    )
    val effectiveAngle = if (animate) angle else 18f

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
        rotate(degrees = effectiveAngle, pivot = Offset(cx, cy)) {
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
