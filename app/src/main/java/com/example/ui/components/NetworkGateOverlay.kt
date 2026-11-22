package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberElevated
import com.example.ui.theme.LocalTurboAccent
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun NetworkGateOverlay(
    onOpenNetworkSettings: () -> Unit,
    onRetryCheck: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalTurboAccent.current.accent

    // Block Android Back button bypass while offline
    BackHandler(enabled = true) { }

    val pulseTransition = rememberInfiniteTransition(label = "offline_gate_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offline_icon_scale"
    )
    val dotAlpha by pulseTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offline_dot_alpha"
    )

    BoxWithConstraints(
        modifier = modifier
            .testTag("network_required_gate")
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF041022),
                        ObsidianVoid
                    )
                )
            )
            // Intercept all underlying touches so app cannot be interacted with offline
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { }
            .safeDrawingPadding()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        val isLandscape = maxWidth >= 560.dp && maxWidth > maxHeight
        val cardGradient = Brush.verticalGradient(
            colors = listOf(MidnightSurface, Color(0xFF0A0E19))
        )
        val borderBrush = Brush.linearGradient(
            colors = listOf(
                accent.copy(alpha = 0.72f),
                CyberBorder,
                CyberAmber.copy(alpha = 0.55f)
            )
        )

        val wifiEmblem: @Composable (androidx.compose.ui.unit.Dp) -> Unit = { boxSize ->
            Box(
                modifier = Modifier
                    .scale(pulseScale)
                    .size(boxSize)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.14f))
                    .border(1.5.dp, accent.copy(alpha = 0.85f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.WifiOff,
                    contentDescription = "No Internet Connection",
                    tint = accent,
                    modifier = Modifier.size(boxSize * 0.48f)
                )
            }
        }

        val statusStrip: @Composable () -> Unit = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberElevated)
                    .border(1.dp, CyberBorder.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Box(
                    modifier = Modifier
                        .alpha(dotAlpha)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(CyberAmber)
                )
                Text(
                    text = "Waiting for active network connection...",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted
                )
            }
        }

        val actionButtons: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onOpenNetworkSettings,
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .testTag("open_network_settings_btn")
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Wifi,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Turn On Wi-Fi / Mobile Data",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                OutlinedButton(
                    onClick = onRetryCheck,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .testTag("retry_network_check_btn")
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Check Connection",
                        color = TextPrimary
                    )
                }
            }
        }

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .widthIn(max = 660.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(cardGradient)
                    .border(1.2.dp, borderBrush, RoundedCornerShape(24.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(
                    modifier = Modifier.weight(0.48f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    wifiEmblem(60.dp)
                    Text(
                        text = "TURBO X • OFFLINE LOCK",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        ),
                        color = CyberAmber
                    )
                    Text(
                        text = "Internet Connection Required",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Turbo X cannot be used offline. Please turn on Wi-Fi or Mobile Data to verify security and access performance features.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
                Column(
                    modifier = Modifier.weight(0.52f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    statusStrip()
                    actionButtons()
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(cardGradient)
                    .border(1.2.dp, borderBrush, RoundedCornerShape(24.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                wifiEmblem(76.dp)
                Text(
                    text = "TURBO X • OFFLINE LOCK",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = CyberAmber
                )
                Text(
                    text = "Internet Connection Required",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Turbo X cannot be used offline. Please turn on Wi-Fi or Mobile Data to verify security and access performance features.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                statusStrip()
                Spacer(modifier = Modifier.height(2.dp))
                actionButtons()
            }
        }
    }
}
