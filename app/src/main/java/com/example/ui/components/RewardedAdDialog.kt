package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.PlayCircleFilled
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberElevated
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.LocalTurboAccent
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.RewardedAdStep
import java.util.Locale

@Composable
fun RewardedAdDialog(
    step: RewardedAdStep,
    remainingTurboSeconds: Long,
    adReady: Boolean,
    adLoading: Boolean,
    adError: String?,
    onWatchAd: () -> Unit,
    onRetryLoad: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalTurboAccent.current

    when (step) {
        RewardedAdStep.Hidden -> Unit

        is RewardedAdStep.TimeGatePrompt -> {
            val isLoadingNow = adLoading || step.waitingForLoad
            Dialog(
                onDismissRequest = onDismiss,
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.74f))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 420.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(MidnightSurface)
                            .border(1.5.dp, palette.accent.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(palette.accent.copy(alpha = 0.16f))
                                .border(1.dp, palette.accent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = "Turbo Time",
                                tint = palette.accent,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Text(
                            text = if (step.launchAfterReward && remainingTurboSeconds <= 0L) {
                                "Turbo Time Required"
                            } else {
                                "Add +1 Hour Turbo Time"
                            },
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = RajdhaniFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = if (step.launchAfterReward && step.targetGameName != null) {
                                "Time is 0:00:00. Watch 1 Rewarded Ad to unlock +1 hour (1:00:00) and launch ${step.targetGameName}."
                            } else {
                                "Balance: ${formatCountdownClock(remainingTurboSeconds)}. Watch 1 Rewarded Ad to add +1 hour (1:00:00) Turbo Time."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        // Reward Summary Card + Real AdMob Load State
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(CyberElevated)
                                .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Bolt,
                                        contentDescription = null,
                                        tint = EmeraldBright,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Rewarded Ad",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted
                                        )
                                        Text(
                                            text = "+1 Hour (1:00:00)",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontFamily = RajdhaniFontFamily,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = EmeraldBright
                                        )
                                    }
                                }
                                Text(
                                    text = when {
                                        adReady -> "Ad Ready"
                                        isLoadingNow -> "Loading..."
                                        else -> "Not Ready"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = when {
                                        adReady -> EmeraldBright
                                        isLoadingNow -> CyberAmber
                                        else -> CyberAmber
                                    }
                                )
                            }

                            if (!adError.isNullOrBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = adError,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyberAmber,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    OutlinedButton(
                                        onClick = onRetryLoad,
                                        modifier = Modifier
                                            .testTag("retry_ad_load_button")
                                            .height(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Refresh,
                                            contentDescription = "Retry ad load",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Retry")
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .testTag("cancel_ad_prompt_button")
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Later", color = TextSecondary)
                            }
                            Button(
                                onClick = onWatchAd,
                                enabled = !step.waitingForLoad,
                                colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .testTag("watch_ad_button")
                                    .weight(1.6f)
                                    .height(48.dp)
                            ) {
                                if (step.waitingForLoad) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Loading Ad...",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayCircleFilled,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (adReady) "Watch Ad (+1h)" else "Load & Watch Ad",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatCountdownClock(totalSeconds: Long): String {
    val safe = totalSeconds.coerceAtLeast(0L)
    val hours = safe / 3600L
    val mins = (safe % 3600L) / 60L
    val secs = safe % 60L
    return String.format(Locale.US, "%dh:%02dm:%02ds", hours, mins, secs)
}

fun formatCompactTurboClock(totalSeconds: Long): String {
    val safe = totalSeconds.coerceAtLeast(0L)
    val hours = safe / 3600L
    val mins = (safe % 3600L) / 60L
    val secs = safe % 60L
    return if (hours > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", hours, mins, secs)
    } else {
        String.format(Locale.US, "0:%02d:%02d", mins, secs)
    }
}
