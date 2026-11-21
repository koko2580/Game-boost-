package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DisplaySettings
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GameTurboSettings
import com.example.data.PerformanceEngineStatus
import com.example.ui.components.CyberToggleSwitch
import com.example.ui.theme.CrimsonTurbo
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.LocalTurboAccent
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun AdvancedPerformanceModulesSection(
    settings: GameTurboSettings,
    engineStatus: PerformanceEngineStatus,
    actions: PerformanceActions
) {
    val accent = LocalTurboAccent.current.accent

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "PRO ANDROID PERFORMANCE SUITE",
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = RajdhaniFontFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            ),
            color = accent
        )

        // 7. Auto Performance Optimization
        FeatureControlCard(
            icon = Icons.Rounded.AutoAwesome,
            title = "Auto Performance Optimization",
            subtitle = "Monitors thermal headroom, battery level & charging state to automatically select Turbo, Sustained, or Eco profile.",
            badgeText = if (settings.autoPerformanceOptimization) engineStatus.autoProfileRecommendation else "Manual",
            badgeColor = if (settings.autoPerformanceOptimization) EmeraldBright else TextMuted,
            checked = settings.autoPerformanceOptimization,
            onCheckedChange = actions.onSetAutoPerformanceOptimization,
            toggleTag = "perf_auto_optimize"
        )

        // 1. Optimize Frame Pacing
        FeatureControlCard(
            icon = Icons.Rounded.Speed,
            title = "Optimize Frame Pacing",
            subtitle = "Stabilizes frame delivery jitter across Turbo X rendering & requests low-latency display presentation.",
            badgeText = buildString {
                append(if (settings.optimizeFramePacing) "Jitter ${String.format(Locale.US, "%.2f", engineStatus.framePacingJitterMs)}ms" else "Disabled")
                if (engineStatus.minimalPostProcessingActive) append(" • Low-Latency Mode")
            },
            badgeColor = if (settings.optimizeFramePacing) ElectricCyan else TextMuted,
            checked = settings.optimizeFramePacing,
            onCheckedChange = actions.onSetOptimizeFramePacing,
            toggleTag = "perf_frame_pacing"
        )

        // 2. Enable ADPF Performance Hints
        FeatureControlCard(
            icon = Icons.Rounded.Memory,
            title = "Enable ADPF Performance Hints",
            subtitle = "Uses Android PerformanceHintManager to report frame budgets (${String.format(Locale.US, "%.1f", engineStatus.adpfTargetDurationMs)}ms) to PowerHAL.",
            badgeText = when {
                !settings.enableAdpfHints -> "ADPF Off"
                engineStatus.adpfHintActive -> "PowerHAL Session Active"
                engineStatus.adpfSupported -> "ADPF Ready"
                else -> "Android 12+ PowerHAL Needed"
            },
            badgeColor = when {
                !settings.enableAdpfHints -> TextMuted
                engineStatus.adpfHintActive || engineStatus.adpfSupported -> EmeraldBright
                else -> CyberAmber
            },
            checked = settings.enableAdpfHints,
            onCheckedChange = actions.onSetEnableAdpfHints,
            toggleTag = "perf_adpf_hints"
        )

        // 6. Sustained Performance Mode
        FeatureControlCard(
            icon = Icons.Rounded.Tune,
            title = "Sustained Performance Mode",
            subtitle = "Engages Android Window.setSustainedPerformanceMode to prevent sudden thermal throttling during long sessions.",
            badgeText = when {
                !engineStatus.sustainedPerformanceSupported -> "Not Supported on Device"
                settings.sustainedPerformanceMode && engineStatus.sustainedPerformanceActive -> "Sustained Lock Active"
                settings.sustainedPerformanceMode -> "Sustained Armed"
                else -> "Hardware Supported"
            },
            badgeColor = when {
                !engineStatus.sustainedPerformanceSupported -> CyberAmber
                settings.sustainedPerformanceMode -> EmeraldBright
                else -> TextSecondary
            },
            checked = settings.sustainedPerformanceMode,
            onCheckedChange = actions.onSetSustainedPerformanceMode,
            toggleTag = "perf_sustained_mode"
        )

        // 3. Disable Power Saving During Gaming
        FeatureControlCard(
            icon = Icons.Rounded.BatteryChargingFull,
            title = "Disable Power Saving During Gaming",
            subtitle = "Detects Android Battery Saver throttling before game launch and provides instant access to Power settings.",
            badgeText = when {
                engineStatus.batterySaverActive -> "WARNING: Battery Saver ON"
                engineStatus.ignoringBatteryOptimizations -> "Battery Unrestricted"
                else -> "Battery Saver OFF"
            },
            badgeColor = if (engineStatus.batterySaverActive) CrimsonTurbo else EmeraldBright,
            checked = settings.disablePowerSavingInGame,
            onCheckedChange = actions.onSetDisablePowerSavingInGame,
            toggleTag = "perf_disable_power_saving"
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            ActionChipButton(
                label = if (engineStatus.batterySaverActive) "DISABLE BATTERY SAVER IN SETTINGS" else "OPEN BATTERY & POWER SETTINGS",
                accentColor = if (engineStatus.batterySaverActive) CrimsonTurbo else accent,
                testTag = "btn_power_settings",
                onClick = actions.onOpenPowerSaverSettings
            )
        }

        // 4. Smart Background Activity Control
        FeatureControlCard(
            icon = Icons.Rounded.CleaningServices,
            title = "Smart Background Activity Control",
            subtitle = "Monitors active background app processes and recent usage activity while protecting your added games.",
            badgeText = if (settings.smartBackgroundControl) {
                "${engineStatus.backgroundProcessCount} background processes detected"
            } else {
                "Monitoring Paused"
            },
            badgeColor = if (settings.smartBackgroundControl && engineStatus.backgroundProcessCount > 0) CyberAmber else EmeraldBright,
            checked = settings.smartBackgroundControl,
            onCheckedChange = actions.onSetSmartBackgroundControl,
            toggleTag = "perf_smart_bg_control"
        ) {
            if (engineStatus.backgroundProcessSampleNames.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Active: ${engineStatus.backgroundProcessSampleNames.joinToString(", ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionChipButton(
                    label = "TRIM BACKGROUND",
                    accentColor = EmeraldBright,
                    testTag = "btn_trim_bg_apps",
                    onClick = actions.onRunSmartBackgroundTrim,
                    modifier = Modifier.weight(1f)
                )
                ActionChipButton(
                    label = "APP MANAGEMENT",
                    accentColor = accent,
                    testTag = "btn_manage_apps",
                    onClick = actions.onOpenAppManagementSettings,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 5. Display Resolution Optimizer
        FeatureControlCard(
            icon = Icons.Rounded.DisplaySettings,
            title = "Display Resolution Optimizer",
            subtitle = "Detects physical panel resolution (${engineStatus.densityDpi} DPI) and applies supported Display.Mode profiles.",
            badgeText = engineStatus.currentResolutionLabel,
            badgeColor = ElectricCyan,
            checked = settings.displayResolutionOpt,
            onCheckedChange = actions.onSetDisplayResolutionOpt,
            toggleTag = "perf_display_res_opt"
        ) {
            if (engineStatus.supportedResolutionModes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    engineStatus.supportedResolutionModes.chunked(2).forEach { rowModes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowModes.forEach { mode ->
                                val isSelected = settings.displayResolutionOpt &&
                                    (settings.preferredDisplayModeId == mode.modeId ||
                                        (settings.preferredDisplayModeId == 0 && engineStatus.appliedDisplayModeId == mode.modeId))
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("display_mode_${mode.modeId}")
                                        .minimumInteractiveComponentSize()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) ElectricCyan.copy(alpha = 0.18f) else CyberElevated)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) ElectricCyan else CyberBorder,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { actions.onSelectDisplayModeId(mode.modeId) }
                                        .padding(horizontal = 10.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = mode.label,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) ElectricCyan else TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (rowModes.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            ActionChipButton(
                label = "SYSTEM DISPLAY SETTINGS",
                accentColor = accent,
                testTag = "btn_display_settings",
                onClick = actions.onOpenDisplaySettings
            )
        }

        HorizontalDivider(color = CyberBorder.copy(alpha = 0.5f))
    }
}

@Composable
private fun FeatureControlCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeText: String,
    badgeColor: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    toggleTag: String,
    extraContent: (@Composable () -> Unit)? = null
) {
    val accent = LocalTurboAccent.current.accent

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MidnightSurface)
            .border(
                width = 1.dp,
                color = if (checked) accent.copy(alpha = 0.48f) else CyberBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = badgeColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            CyberToggleSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                testTag = toggleTag
            )
        }
        if (extraContent != null) {
            extraContent()
        }
    }
}

@Composable
private fun ActionChipButton(
    label: String,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(8.dp))
            .background(accentColor.copy(alpha = 0.12f))
            .border(1.dp, accentColor.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = RajdhaniFontFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.7.sp
            ),
            color = accentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
