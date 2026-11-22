package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.BoostStepId
import com.example.data.BoostStepStatus
import com.example.data.OneTapBoosterState
import com.example.data.OneTapStepResult
import com.example.ui.theme.CrimsonTurbo
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberElevated
import com.example.ui.theme.CyberSlate
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.LocalTurboAccent
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun OneTapBoosterDialog(
    state: OneTapBoosterState,
    onReBoost: () -> Unit,
    onOpenPowerSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!state.isVisible) return

    val accent = LocalTurboAccent.current.accent
    val animatedProgress by animateFloatAsState(
        targetValue = state.progress.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 280f),
        label = "one_tap_progress"
    )
    val scoreColor = when {
        state.batterySaverActive -> CyberAmber
        state.finalScore >= 85 -> EmeraldBright
        else -> ElectricCyan
    }

    Dialog(
        onDismissRequest = { if (!state.isRunning) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            val isLandscape = maxWidth >= 560.dp && maxWidth > maxHeight
            val dialogMaxHeight = (maxHeight * 0.94f).coerceAtLeast(280.dp)

            Column(
                modifier = Modifier
                    .widthIn(max = if (isLandscape) 720.dp else 520.dp)
                    .heightIn(max = dialogMaxHeight)
                    .fillMaxWidth()
                    .testTag("one_tap_booster_dialog")
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(MidnightSurface, ObsidianVoid)
                        )
                    )
                    .border(1.2.dp, accent.copy(alpha = 0.65f), RoundedCornerShape(22.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(accent.copy(alpha = 0.16f))
                                .border(1.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.RocketLaunch,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "ONE TAP BOOSTER",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = RajdhaniFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.1.sp
                                ),
                                color = TextPrimary
                            )
                            Text(
                                text = when {
                                    state.isRunning -> "Running 6-stage hardware optimization..."
                                    state.isCompleted -> "Final Performance Report • ${state.finalVerdict}"
                                    else -> "Ready to optimize"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (state.isCompleted) scoreColor else TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !state.isRunning,
                        modifier = Modifier.testTag("one_tap_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close One Tap Booster",
                            tint = if (state.isRunning) TextMuted.copy(alpha = 0.4f) else TextSecondary
                        )
                    }
                }

                // Overall Progress Bar
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (state.isCompleted) "OPTIMIZATION COMPLETE" else "STAGE ${state.currentStepIndex + 1} OF 6",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = RajdhaniFontFamily,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            ),
                            color = accent
                        )
                        Text(
                            text = "${(animatedProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = RajdhaniFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = TextPrimary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        color = if (state.isCompleted) scoreColor else accent,
                        trackColor = CyberSlate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }

                // Scrollable Body: Side-by-side in Landscape when completed, vertical otherwise
                if (isLandscape && state.isCompleted) {
                    Row(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(0.46f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            FinalPerformanceReportBanner(
                                state = state,
                                scoreColor = scoreColor,
                                onOpenPowerSettings = onOpenPowerSettings
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(0.54f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            state.steps.forEach { step ->
                                BoosterStepCard(step = step, accent = accent)
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AnimatedVisibility(
                            visible = state.isCompleted,
                            enter = fadeIn(tween(240)),
                            exit = fadeOut(tween(150))
                        ) {
                            FinalPerformanceReportBanner(
                                state = state,
                                scoreColor = scoreColor,
                                onOpenPowerSettings = onOpenPowerSettings
                            )
                        }

                        state.steps.forEach { step ->
                            BoosterStepCard(step = step, accent = accent)
                        }
                    }
                }

                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onReBoost,
                        enabled = !state.isRunning,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("one_tap_reboost_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("RE-BOOST", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onDismiss,
                        enabled = !state.isRunning,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.isCompleted) EmeraldActive else accent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("one_tap_done_btn")
                    ) {
                        Text(
                            text = if (state.isRunning) "OPTIMIZING..." else "DONE",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FinalPerformanceReportBanner(
    state: OneTapBoosterState,
    scoreColor: Color,
    onOpenPowerSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("one_tap_final_report_card")
            .clip(RoundedCornerShape(14.dp))
            .background(scoreColor.copy(alpha = 0.08f))
            .border(1.2.dp, scoreColor.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "FINAL PERFORMANCE REPORT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = scoreColor
                )
                Text(
                    text = state.finalVerdict,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = TextPrimary
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(scoreColor.copy(alpha = 0.16f))
                    .border(1.dp, scoreColor.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${state.finalScore}/100",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = scoreColor
                )
            }
        }

        // 3 Compact Summary Metric Tiles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReportMetricTile(
                label = "FREED RAM",
                value = "+${state.freedMb} MB",
                sub = "${state.trimmedApps} apps",
                color = EmeraldBright,
                modifier = Modifier.weight(1f)
            )
            ReportMetricTile(
                label = "MEM PRESSURE",
                value = state.memoryPressureLabel,
                sub = "${state.ramUsedBeforePercent}%→${state.ramUsedAfterPercent}%",
                color = ElectricCyan,
                modifier = Modifier.weight(1f)
            )
            ReportMetricTile(
                label = "THERMAL",
                value = "${state.batteryTempC}°C",
                sub = state.thermalLabel,
                color = if (state.thermalLabel == "NORMAL") EmeraldBright else CyberAmber,
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider(color = CyberBorder.copy(alpha = 0.5f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Gaming Profile",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            Text(
                text = state.appliedProfileLabel.ifBlank { state.selectedCpuMode.label },
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (state.batterySaverActive) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("one_tap_power_settings_btn")
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CrimsonTurbo.copy(alpha = 0.16f))
                    .border(1.dp, CrimsonTurbo.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                    .clickable { onOpenPowerSettings() }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "BATTERY SAVER ON — OPEN POWER SETTINGS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = CrimsonTurbo
                )
            }
        }
    }
}

@Composable
private fun ReportMetricTile(
    label: String,
    value: String,
    sub: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(CyberSlate.copy(alpha = 0.85f))
            .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 0.6.sp),
            color = TextMuted,
            maxLines = 1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = RajdhaniFontFamily,
                fontWeight = FontWeight.Bold
            ),
            color = color,
            maxLines = 1
        )
        Text(
            text = sub,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BoosterStepCard(
    step: OneTapStepResult,
    accent: Color
) {
    val statusColor by animateColorAsState(
        targetValue = when (step.status) {
            BoostStepStatus.DONE -> EmeraldBright
            BoostStepStatus.WARNING -> CyberAmber
            BoostStepStatus.RUNNING -> accent
            BoostStepStatus.PENDING -> TextMuted
        },
        animationSpec = tween(220),
        label = "step_color_${step.id.name}"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("one_tap_step_${step.id.name}")
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (step.status == BoostStepStatus.RUNNING) CyberElevated else MidnightSurface.copy(alpha = 0.88f)
            )
            .border(
                width = 1.dp,
                color = if (step.status == BoostStepStatus.RUNNING) accent.copy(alpha = 0.7f) else CyberBorder.copy(alpha = 0.75f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(statusColor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            when (step.status) {
                BoostStepStatus.RUNNING -> CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = accent,
                    modifier = Modifier.size(16.dp)
                )
                BoostStepStatus.DONE -> Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Done",
                    tint = EmeraldBright,
                    modifier = Modifier.size(18.dp)
                )
                BoostStepStatus.WARNING -> Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = "Warning",
                    tint = CyberAmber,
                    modifier = Modifier.size(18.dp)
                )
                BoostStepStatus.PENDING -> Icon(
                    imageVector = stepIcon(step.id),
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = step.id.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = step.metricBadge,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = statusColor,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = step.detail,
                style = MaterialTheme.typography.bodySmall,
                color = if (step.status == BoostStepStatus.PENDING) TextMuted else TextSecondary
            )
        }
    }
}

private fun stepIcon(id: BoostStepId): ImageVector = when (id) {
    BoostStepId.DEVICE_SCAN -> Icons.Rounded.Speed
    BoostStepId.MEMORY_OPTIMIZATION -> Icons.Rounded.Memory
    BoostStepId.THERMAL_PROTECTION -> Icons.Rounded.Thermostat
    BoostStepId.POWER_OPTIMIZATION -> Icons.Rounded.BatteryChargingFull
    BoostStepId.GAMING_PROFILE -> Icons.Rounded.SportsEsports
    BoostStepId.FINAL_REPORT -> Icons.Rounded.Bolt
}
