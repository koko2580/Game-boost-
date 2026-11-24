package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ControlPanelLayout
import com.example.data.CpuMode
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSlate
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale
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

internal enum class DockEdge {
    START, END, BOTTOM
}

internal fun resolveDockEdge(
    isWideLandscape: Boolean,
    controlPanelLayout: ControlPanelLayout
): DockEdge = when {
    !isWideLandscape -> DockEdge.BOTTOM
    controlPanelLayout == ControlPanelLayout.RIGHT_EDGE_MENU -> DockEdge.END
    else -> DockEdge.START
}

/**
 * Shared half-screen floating dock wrapper for all Turbo X overlay styles.
 * - Leaves half+ of the game screen completely unobstructed for FPS visibility
 * - Tap outside the docked HUD (on [overlay_backdrop_scrim]) collapses back to the handle
 * - Swipe toward the docked edge also collapses quickly
 */
@Composable
internal fun EsportsHalfDockShell(
    isWideLandscape: Boolean,
    controlPanelLayout: ControlPanelLayout,
    lowRamOverlayMode: Boolean,
    rootTag: String,
    onDismissToHandle: () -> Unit,
    modifier: Modifier = Modifier,
    maxLandscapeWidthFraction: Float = 0.46f,
    maxLandscapeWidthDp: Dp = 400.dp,
    maxPortraitHeightFraction: Float = 0.62f,
    panelContent: @Composable BoxScope.(dockEdge: DockEdge, dockWidthFraction: Dp) -> Unit
) {
    val dockEdge = resolveDockEdge(isWideLandscape, controlPanelLayout)
    val entrance = remember(rootTag, lowRamOverlayMode) {
        Animatable(if (lowRamOverlayMode) 1f else 0f)
    }
    LaunchedEffect(rootTag, lowRamOverlayMode) {
        if (lowRamOverlayMode) {
            entrance.snapTo(1f)
        } else {
            entrance.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
            )
        }
    }

    val scrimInteraction = remember { MutableInteractionSource() }
    var dragAccum by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        modifier = modifier
            .testTag(rootTag)
            .fillMaxSize()
    ) {
        // Lightweight tap-outside scrim that never blinds the FPS battlefield
        Box(
            modifier = Modifier
                .testTag("overlay_backdrop_scrim")
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.24f))
                .clickable(
                    interactionSource = scrimInteraction,
                    indication = null,
                    onClick = onDismissToHandle
                )
        )

        val alignment = when (dockEdge) {
            DockEdge.START -> Alignment.CenterStart
            DockEdge.END -> Alignment.CenterEnd
            DockEdge.BOTTOM -> Alignment.BottomCenter
        }

        val targetWidth = if (isWideLandscape) {
            (maxWidth * maxLandscapeWidthFraction).coerceIn(288.dp, maxLandscapeWidthDp)
        } else {
            (maxWidth - 16.dp).coerceAtMost(436.dp)
        }

        val targetHeight = if (isWideLandscape) {
            (maxHeight - 14.dp).coerceIn(260.dp, 388.dp)
        } else {
            (maxHeight * maxPortraitHeightFraction).coerceIn(320.dp, 490.dp)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(
                    horizontal = if (isWideLandscape) 10.dp else 8.dp,
                    vertical = if (isWideLandscape) 6.dp else 10.dp
                ),
            contentAlignment = alignment
        ) {
            Box(
                modifier = Modifier
                    .testTag("overlay_dock_panel")
                    .width(targetWidth)
                    .heightIn(max = targetHeight)
                    .fillMaxHeight(if (isWideLandscape) 0.96f else 1f)
                    .graphicsLayer {
                        val t = entrance.value.coerceIn(0f, 1f)
                        alpha = 0.35f + (0.65f * t)
                        when (dockEdge) {
                            DockEdge.START -> translationX = -110f * (1f - t)
                            DockEdge.END -> translationX = 110f * (1f - t)
                            DockEdge.BOTTOM -> translationY = 110f * (1f - t)
                        }
                    }
                    .pointerInput(dockEdge) {
                        detectHorizontalDragGestures(
                            onDragEnd = { dragAccum = 0f },
                            onDragCancel = { dragAccum = 0f }
                        ) { change, dragAmount ->
                            dragAccum += dragAmount
                            val dismissSwiped = when (dockEdge) {
                                DockEdge.START -> dragAccum < -95f
                                DockEdge.END -> dragAccum > 95f
                                DockEdge.BOTTOM -> false
                            }
                            if (dismissSwiped) {
                                change.consume()
                                dragAccum = 0f
                                onDismissToHandle()
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { /* shield panel surface from dismissing via scrim without merging child semantics */ }
                    }
            ) {
                panelContent(dockEdge, targetWidth)
            }
        }
    }
}

internal fun rogWingDeckPath(width: Float, height: Float, isLandscape: Boolean): Path {
    val shoulderInsetX = if (isLandscape) width * 0.25f else width * 0.14f
    val topBevel = if (isLandscape) (height * 0.12f).coerceIn(8f, 18f) else (height * 0.04f).coerceIn(6f, 14f)
    val sideCut = if (isLandscape) (height * 0.17f).coerceIn(12f, 24f) else (height * 0.055f).coerceIn(8f, 16f)
    val bottomCut = (height * 0.10f).coerceIn(8f, 16f)
    return Path().apply {
        moveTo(sideCut, topBevel)
        lineTo((shoulderInsetX - sideCut * 0.65f).coerceAtLeast(sideCut), topBevel)
        lineTo(shoulderInsetX, 0f)
        lineTo(width - shoulderInsetX, 0f)
        lineTo((width - shoulderInsetX + sideCut * 0.65f).coerceAtMost(width - sideCut), topBevel)
        lineTo(width - sideCut, topBevel)
        lineTo(width, topBevel + sideCut)
        lineTo(width, height - bottomCut)
        lineTo(width - bottomCut, height)
        lineTo(bottomCut, height)
        lineTo(0f, height - bottomCut)
        lineTo(0f, topBevel + sideCut)
        close()
    }
}

/**
 * Bottom-docked ROG Game Genie-inspired cockpit wrapper.
 * Anchors cleanly at BottomCenter so the upper battlefield remains completely unobstructed,
 * with smooth spring slide-up + fade entrance, scrim tap-to-dismiss, and swipe-down collapse.
 */
@Composable
internal fun RogBottomCockpitShell(
    isWideLandscape: Boolean,
    lowRamOverlayMode: Boolean,
    rootTag: String,
    onDismissToHandle: () -> Unit,
    modifier: Modifier = Modifier,
    panelContent: @Composable BoxScope.(panelWidth: Dp, maxPanelHeight: Dp) -> Unit
) {
    val entrance = remember(rootTag, lowRamOverlayMode) {
        Animatable(if (lowRamOverlayMode) 1f else 0f)
    }
    LaunchedEffect(rootTag, lowRamOverlayMode) {
        if (lowRamOverlayMode) {
            entrance.snapTo(1f)
        } else {
            entrance.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessMediumLow)
            )
        }
    }

    val scrimInteraction = remember { MutableInteractionSource() }
    var dragAccumY by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        modifier = modifier
            .testTag(rootTag)
            .fillMaxSize()
    ) {
        // Lightweight scrim keeps the actual game visible behind the panel; tap outside to hide
        Box(
            modifier = Modifier
                .testTag("overlay_backdrop_scrim")
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.08f),
                            Color.Black.copy(alpha = 0.24f),
                            Color.Black.copy(alpha = 0.42f)
                        )
                    )
                )
                .clickable(
                    interactionSource = scrimInteraction,
                    indication = null,
                    onClick = onDismissToHandle
                )
        )

        val targetWidth = if (isWideLandscape) {
            (maxWidth * 0.91f).coerceIn(500.dp, 820.dp)
        } else {
            (maxWidth - 14.dp).coerceAtMost(450.dp)
        }
        val maxPanelHeight = if (isWideLandscape) {
            (maxHeight * 0.52f).coerceIn(168.dp, 214.dp)
        } else {
            (maxHeight * 0.54f).coerceIn(296.dp, 450.dp)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(
                    horizontal = if (isWideLandscape) 10.dp else 7.dp,
                    vertical = if (isWideLandscape) 4.dp else 8.dp
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .testTag("overlay_dock_panel")
                    .width(targetWidth)
                    .heightIn(max = maxPanelHeight)
                    .graphicsLayer {
                        val t = entrance.value.coerceIn(0f, 1f)
                        alpha = 0.25f + (0.75f * t)
                        translationY = 140f * (1f - t)
                    }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = { dragAccumY = 0f },
                            onDragCancel = { dragAccumY = 0f }
                        ) { change, dragAmount ->
                            dragAccumY += dragAmount
                            if (dragAccumY > 85f) {
                                change.consume()
                                dragAccumY = 0f
                                onDismissToHandle()
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { /* shield panel surface from dismissing via scrim without merging child semantics */ }
                    }
            ) {
                panelContent(targetWidth, maxPanelHeight)
            }
        }
    }
}

@Composable
internal fun EsportsCollapseChip(
    label: String,
    dockEdge: DockEdge,
    accent: Color,
    shape: Shape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val arrowIcon = when (dockEdge) {
        DockEdge.START -> Icons.AutoMirrored.Rounded.KeyboardArrowLeft
        DockEdge.END -> Icons.AutoMirrored.Rounded.KeyboardArrowRight
        DockEdge.BOTTOM -> Icons.Rounded.KeyboardArrowDown
    }
    Row(
        modifier = modifier
            .testTag("overlay_g_handle")
            .height(28.dp)
            .clip(shape)
            .background(accent.copy(alpha = 0.18f))
            .border(1.dp, accent.copy(alpha = 0.85f), shape)
            .clickable { onClick() }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = arrowIcon,
            contentDescription = "Hide game panel",
            tint = accent,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = RajdhaniFontFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp
            ),
            color = Color.White,
            maxLines = 1
        )
    }
}

@Composable
internal fun OverlayActionIconRow(
    accentColor: Color,
    containerColor: Color,
    shape: Shape = CircleShape,
    callbacks: OverlayCallbacks,
    iconSizeDp: Dp = 28.dp
) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        EsportsActionIconBtn(Icons.Rounded.Home, "Home", accentColor, containerColor, shape, "overlay_home", iconSizeDp, callbacks.onHomeClick)
        EsportsActionIconBtn(Icons.Rounded.Settings, "Settings", accentColor, containerColor, shape, "overlay_settings", iconSizeDp, callbacks.onSettingsClick)
        EsportsActionIconBtn(Icons.Rounded.Gamepad, "Games", accentColor, containerColor, shape, "overlay_games", iconSizeDp, callbacks.onGamesClick)
        EsportsActionIconBtn(Icons.Rounded.PowerSettingsNew, "Stop Overlay Service", Color(0xFFFF3D57), containerColor, shape, "overlay_power", iconSizeDp, callbacks.onPowerExitClick)
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
    sizeDp: Dp = 28.dp,
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
            .scale(scale)
            .testTag(tag)
            .size(sizeDp)
            .clip(shape)
            .background(containerColor)
            .border(1.dp, tint.copy(alpha = 0.45f), shape)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = TextPrimary, modifier = Modifier.size(15.dp))
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
    cardHeight: Dp = 44.dp
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

    Box(
        modifier = modifier
            .scale(scale)
            .height(cardHeight)
            .clip(shape)
            .background(bg)
            .border(if (active) 1.3.dp else 1.dp, border, shape)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
    ) {
        // Active neon LED status pip in top-right corner for immediate gaming feedback
        if (active) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 5.dp)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(activeAccent)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (badgeText != null) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    ),
                    color = if (active) activeAccent else TextPrimary,
                    maxLines = 1
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (active) activeAccent else TextPrimary,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = if (active) TextPrimary else TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
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
            .height(28.dp),
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
                val trackH = 7.dp.toPx()
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
                    brush = Brush.horizontalGradient(listOf(accentColor.copy(alpha = 0.72f), accentColor)),
                    topLeft = Offset(0f, topY),
                    size = Size(fillW, trackH),
                    cornerRadius = corner
                )
                val thumbX = fillW.coerceIn(6.5.dp.toPx(), (size.width - 6.5.dp.toPx()).coerceAtLeast(6.5.dp.toPx()))
                drawCircle(
                    color = Color.White,
                    radius = 6.dp.toPx(),
                    center = Offset(thumbX, size.height / 2f)
                )
                drawCircle(
                    color = accentColor,
                    radius = 2.8.dp.toPx(),
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
        verticalArrangement = Arrangement.spacedBy(3.dp)
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
                .width(36.dp)
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
                val gap = 2.5.dp.toPx()
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
                        cornerRadius = CornerRadius(2.dp.toPx())
                    )
                }
            }
        }
        Icon(icon, contentDescription = label, tint = accentColor, modifier = Modifier.size(15.dp))
    }
}

@Composable
internal fun EsportsQuickAppBar(
    shape: Shape,
    callbacks: OverlayCallbacks
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
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
            .height(34.dp)
            .clip(shape)
            .background(color.copy(alpha = 0.16f))
            .border(1.dp, color.copy(alpha = 0.55f), shape)
            .clickable { onClick() }
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
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
    sizeDp: Dp = 28.dp
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
            style = Stroke(width = 1.5.dp.toPx())
        )
        rotate(degrees = angle, pivot = Offset(cx, cy)) {
            val bladeCount = 6
            for (i in 0 until bladeCount) {
                val deg = i * (360f / bladeCount)
                val rad1 = deg * (PI / 180.0)
                val rad2 = (deg + 28f) * (PI / 180.0)
                val blade = Path().apply {
                    moveTo(cx, cy)
                    lineTo(cx + (r * 0.85f * cos(rad1)).toFloat(), cy + (r * 0.85f * sin(rad1)).toFloat())
                    lineTo(cx + (r * 0.72f * cos(rad2)).toFloat(), cy + (r * 0.72f * sin(rad2)).toFloat())
                    close()
                }
                drawPath(blade, color = if (i % 2 == 0) primaryColor else secondaryColor.copy(alpha = 0.9f))
            }
        }
        drawCircle(color = Color(0xFF0A0D16), radius = r * 0.28f, center = Offset(cx, cy))
        drawCircle(color = Color.White, radius = r * 0.13f, center = Offset(cx, cy))
    }
}

@Composable
internal fun StyleTabStrip(
    tabs: List<String>,
    selectedIndex: Int,
    accent: Color,
    shape: Shape,
    onSelect: (Int) -> Unit,
    selectedTextColor: Color = Color.White,
    containerColor: Color = CyberSlate.copy(alpha = 0.85f)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(containerColor)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
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
                    .padding(vertical = 5.dp, horizontal = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    color = if (selected) selectedTextColor else TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun AnimatedStyleGrid(
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
                contentPadding = PaddingValues(bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
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
                contentPadding = PaddingValues(bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
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
internal fun MiniTelemetryTag(
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
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(3.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp),
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun OverlayStatusPill(
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
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = accent,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun EsportsStopwatchBar(
    stopwatchSeconds: Long,
    isRunning: Boolean,
    accent: Color,
    shape: Shape,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mins = stopwatchSeconds / 60L
    val secs = stopwatchSeconds % 60L
    val formatted = String.format(Locale.US, "%02d:%02d", mins, secs)
    Row(
        modifier = modifier
            .testTag("overlay_stopwatch_pill")
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xFF131826))
            .border(1.dp, accent.copy(alpha = 0.7f), shape)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(Icons.Rounded.Timer, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
            Text(
                text = formatted,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = RajdhaniFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = TextPrimary
            )
            Text(
                text = if (isRunning) "RUN" else "PAUSE",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                color = accent
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onToggle,
                modifier = Modifier
                    .testTag("overlay_stopwatch_toggle")
                    .size(30.dp)
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isRunning) "Pause stopwatch" else "Resume stopwatch",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(
                onClick = onReset,
                modifier = Modifier
                    .testTag("overlay_stopwatch_reset")
                    .size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Reset stopwatch",
                    tint = TextSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}
