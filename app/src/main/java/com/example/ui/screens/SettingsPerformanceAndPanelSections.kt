package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ControlPanelLayout
import com.example.data.FPS_GAME_OPTIONS
import com.example.data.FRAME_RATE_OPTIONS
import com.example.data.GamePanelStyle
import com.example.data.GameTurboSettings
import com.example.data.HardwareTelemetry
import com.example.data.PerformanceEngineStatus
import com.example.data.PermissionStatus
import com.example.data.SCROLLING_SENSITIVITY_TICKS
import com.example.data.SLIDING_FRICTION_LABELS
import com.example.ui.components.CyberToggleSwitch
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
import java.util.Locale

data class PerformanceActions(
    val onSetFpsGameTarget: (Int) -> Unit,
    val onSetFrameRateTarget: (Int) -> Unit,
    val onSetSlidingFrictionIndex: (Int) -> Unit,
    val onSetScrollingSensitivity: (Int) -> Unit,
    val onSetEnhanceTouchSensitivity: (Boolean) -> Unit,
    val onSetReduceTouchLatency: (Boolean) -> Unit,
    val onSetIncreaseCpuPerformance: (Boolean) -> Unit,
    val onSetIncreaseGpuPerformance: (Boolean) -> Unit,
    val onSetForceGpuOpenGl: (Boolean) -> Unit,
    val onSetEnableSkiaVulkan: (Boolean) -> Unit,
    val onSetHighPerfFpsControl: (Boolean) -> Unit,
    val onOpenPermissions: () -> Unit,
    val onOpenDeveloperOptions: () -> Unit,
    val onSetOptimizeFramePacing: (Boolean) -> Unit = {},
    val onSetEnableAdpfHints: (Boolean) -> Unit = {},
    val onSetDisablePowerSavingInGame: (Boolean) -> Unit = {},
    val onOpenPowerSaverSettings: () -> Unit = {},
    val onSetSmartBackgroundControl: (Boolean) -> Unit = {},
    val onRunSmartBackgroundTrim: () -> Unit = {},
    val onOpenAppManagementSettings: () -> Unit = {},
    val onSetDisplayResolutionOpt: (Boolean) -> Unit = {},
    val onSelectDisplayModeId: (Int) -> Unit = {},
    val onOpenDisplaySettings: () -> Unit = {},
    val onSetSustainedPerformanceMode: (Boolean) -> Unit = {},
    val onSetAutoPerformanceOptimization: (Boolean) -> Unit = {}
)

@Composable
fun SettingToggleRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
            CyberToggleSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                testTag = testTag
            )
        }
        HorizontalDivider(color = CyberBorder.copy(alpha = 0.45f))
    }
}

@Composable
fun PerformanceSettingsSection(
    settings: GameTurboSettings,
    telemetry: HardwareTelemetry,
    engineStatus: PerformanceEngineStatus,
    permissions: PermissionStatus,
    actions: PerformanceActions
) {
    val accent = LocalTurboAccent.current.accent
    var helpDialogTitle by remember { mutableStateOf<String?>(null) }
    var helpDialogBody by remember { mutableStateOf("") }
    val panelMaxHz = telemetry.supportedRefreshRates.maxOrNull() ?: 60

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Optimize performance",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Real-time ADPF hints, refresh-rate locking, unbuffered touch, and GPU tuning.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))

        // Live Performance Engine Diagnostics Card
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MidnightSurface)
                .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
            val useTwoColumns = maxWidth >= 380.dp
            val statLines = listOf(
                Triple(
                    "Display & FPS",
                    "${engineStatus.appliedDisplayModeHz}Hz mode • ${telemetry.actualFps} FPS (Max ${panelMaxHz}Hz)",
                    EmeraldBright
                ),
                Triple(
                    "CPU Boost",
                    buildString {
                        append(if (engineStatus.adpfHintActive) "ADPF (${String.format(Locale.US, "%.1f", engineStatus.adpfTargetDurationMs)}ms)" else "Thread Priority")
                        if (engineStatus.gameStateActive) append(" • GameState")
                    },
                    if (engineStatus.adpfHintActive || engineStatus.threadPriorityBoosted) ElectricCyan else TextSecondary
                ),
                Triple(
                    "GPU Pipeline",
                    "${engineStatus.activeGpuPipelineLabel} • ${engineStatus.gpuInfo.rendererName}",
                    TextPrimary
                ),
                Triple(
                    "Touch & Scroll",
                    "${if (engineStatus.unbufferedTouchActive) "Unbuffered ON" else "Standard"} • x${String.format(Locale.US, "%.2f", engineStatus.scrollMultiplier * engineStatus.frictionFactor)}",
                    if (engineStatus.unbufferedTouchActive) EmeraldBright else TextSecondary
                ),
                Triple(
                    "Frame Pacing",
                    "${if (engineStatus.framePacingActive) "ON (${String.format(Locale.US, "%.2f", engineStatus.framePacingJitterMs)}ms)" else "OFF"} • ${engineStatus.currentResolutionLabel}",
                    if (engineStatus.framePacingActive) ElectricCyan else TextSecondary
                ),
                Triple(
                    "Power & Mode",
                    buildString {
                        append(if (engineStatus.batterySaverActive) "Battery Saver ON!" else "Saver OFF")
                        if (engineStatus.sustainedPerformanceActive) append(" • Sustained")
                        if (engineStatus.autoPerfActive) append(" • Auto")
                    },
                    if (engineStatus.batterySaverActive) CyberAmber else EmeraldBright
                )
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(if (useTwoColumns) 4.dp else 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACTIVE ENGINE STATUS",
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = RajdhaniFontFamily),
                        color = accent
                    )
                    Text(
                        text = "Thermal: ${engineStatus.thermalStatusLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (engineStatus.thermalStatusLabel == "NORMAL") EmeraldBright else CyberAmber
                    )
                }

                if (useTwoColumns) {
                    statLines.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            pair.forEach { (lbl, valText, valColor) ->
                                EngineStatTile(
                                    label = lbl,
                                    value = valText,
                                    valueColor = valColor,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                } else {
                    statLines.forEach { (lbl, valText, valColor) ->
                        EngineStatLine(
                            label = lbl,
                            value = valText,
                            valueColor = valColor
                        )
                    }
                }

                if (!permissions.writeSettingsGranted) {
                    Text(
                        text = "Tip: Grant 'Modify System Settings' to sync system pointer speed (${engineStatus.appliedPointerSpeed}). Tap here ->",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberAmber,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { actions.onOpenPermissions() }
                            .padding(vertical = 3.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = CyberBorder.copy(alpha = 0.5f))

        // FPS Game Slider
        val fpsIdx = FPS_GAME_OPTIONS.indexOf(settings.fpsGameTarget).coerceAtLeast(0)
        DiscreteSliderRow(
            title = "FPS Game",
            labels = listOf("OFF", "90", "120", "144"),
            selectedIndex = fpsIdx,
            description = "Locks game refresh target & ADPF frame budget (90/120/144 FPS) across launcher & overlay.",
            accentColor = accent,
            onIndexChange = { idx -> actions.onSetFpsGameTarget(FPS_GAME_OPTIONS[idx]) },
            onHelpClick = {
                helpDialogTitle = "FPS Game Target"
                helpDialogBody = "Targets 90/120/144Hz on the display window & ADPF PowerHAL budget while Gaming Mode is active."
            }
        )

        // Frame Rate Slider
        val frameIdx = FRAME_RATE_OPTIONS.indexOf(settings.frameRateTarget).coerceAtLeast(0)
        DiscreteSliderRow(
            title = "Frame Rate",
            labels = listOf("60", "90", "120", "144"),
            selectedIndex = frameIdx,
            description = "Sets preferred Display.Mode refresh rate (panel supports up to ${panelMaxHz}Hz).",
            accentColor = accent,
            onIndexChange = { idx -> actions.onSetFrameRateTarget(FRAME_RATE_OPTIONS[idx]) },
            onHelpClick = {
                helpDialogTitle = "Display Frame Rate"
                helpDialogBody = "Applies WindowManager.LayoutParams preferredDisplayModeId & preferredRefreshRate on both app & overlay windows."
            }
        )

        // Sliding Friction Slider
        DiscreteSliderRow(
            title = "Sliding friction",
            labels = SLIDING_FRICTION_LABELS,
            selectedIndex = settings.slidingFrictionIndex.coerceIn(0, SLIDING_FRICTION_LABELS.lastIndex),
            description = "Lower friction increases fling glide & camera pan travel distance.",
            accentColor = accent,
            onIndexChange = { idx -> actions.onSetSlidingFrictionIndex(idx) },
            onHelpClick = {
                helpDialogTitle = "Sliding Friction"
                helpDialogBody = "Scales swipe deceleration and inertial fling glide across Launcher & HUD controls."
            }
        )

        // Scrolling Sensitivity Slider
        val scrollIdx = SCROLLING_SENSITIVITY_TICKS.indexOf(settings.scrollingSensitivity)
            .let { if (it >= 0) it else 4 }
        DiscreteSliderRow(
            title = "Scrolling sensitivity",
            labels = SCROLLING_SENSITIVITY_TICKS.map { it.toString() },
            selectedIndex = scrollIdx,
            description = "Adjusts pointer & swipe responsiveness (-7 to +7); syncs system pointer speed when permitted.",
            accentColor = accent,
            onIndexChange = { idx -> actions.onSetScrollingSensitivity(SCROLLING_SENSITIVITY_TICKS[idx]) },
            onHelpClick = {
                helpDialogTitle = "Scrolling Sensitivity"
                helpDialogBody = "Applies swipe velocity scaling in Turbo X and writes Android pointer_speed (-7..+7) when Write Settings is granted."
            }
        )

        SettingToggleRow(
            title = "Enhance touch sensitivity",
            subtitle = "Enables unbuffered touchscreen dispatch & boosts swipe responsiveness by +18%.",
            checked = settings.enhanceTouchSensitivity,
            onCheckedChange = actions.onSetEnhanceTouchSensitivity,
            testTag = "perf_enhance_touch"
        )
        SettingToggleRow(
            title = "Reduce the latency of touch response",
            subtitle = "Disables VSync input batching via requestUnbufferedDispatch & elevates UI thread to URGENT_DISPLAY.",
            checked = settings.reduceTouchLatency,
            onCheckedChange = actions.onSetReduceTouchLatency,
            testTag = "perf_reduce_latency"
        )
        SettingToggleRow(
            title = "Increase CPU performance",
            subtitle = "Engages ADPF PerformanceHintManager, Android GameState, Acceleration mode & trims background RAM.",
            checked = settings.increaseCpuPerformance,
            onCheckedChange = actions.onSetIncreaseCpuPerformance,
            testTag = "perf_cpu_boost"
        )
        SettingToggleRow(
            title = "Increase GPU performance",
            subtitle = "Forces hardware acceleration, enables Wide Color Gamut (when supported) & tightens GPU frame budget.",
            checked = settings.increaseGpuPerformance,
            onCheckedChange = actions.onSetIncreaseGpuPerformance,
            testTag = "perf_gpu_boost"
        )
        SettingToggleRow(
            title = "Force GPU to process the UI (OpenGL)",
            subtitle = "Uses OpenGL ES (${engineStatus.gpuInfo.glEsVersion}) hardware UI pipeline (mutually exclusive with Vulkan).",
            checked = settings.forceGpuOpenGl,
            onCheckedChange = actions.onSetForceGpuOpenGl,
            testTag = "perf_force_opengl"
        )
        SettingToggleRow(
            title = "Enable Skia (Vulkan) UI rendering",
            subtitle = "Prioritizes Skia Vulkan (${engineStatus.gpuInfo.vulkanVersionLabel}) when supported by device hardware.",
            checked = settings.enableSkiaVulkan,
            onCheckedChange = actions.onSetEnableSkiaVulkan,
            testTag = "perf_skia_vulkan"
        )
        SettingToggleRow(
            title = "High performance FPS control",
            subtitle = "Locks maximum panel refresh rate (${panelMaxHz}Hz) across app & floating overlay windows.",
            checked = settings.highPerfFpsControl,
            onCheckedChange = actions.onSetHighPerfFpsControl,
            testTag = "perf_high_fps"
        )

        AdvancedPerformanceModulesSection(
            settings = settings,
            engineStatus = engineStatus,
            actions = actions
        )

        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_open_dev_rendering")
                .clip(RoundedCornerShape(10.dp))
                .background(CyberElevated)
                .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
                .clickable { actions.onOpenDeveloperOptions() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "OPEN SYSTEM DEVELOPER RENDERING OPTIONS",
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }

    if (helpDialogTitle != null) {
        AlertDialog(
            onDismissRequest = { helpDialogTitle = null },
            containerColor = MidnightSurface,
            title = { Text(helpDialogTitle ?: "", color = TextPrimary) },
            text = { Text(helpDialogBody, color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { helpDialogTitle = null }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun EngineStatTile(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(7.dp))
            .background(CyberElevated.copy(alpha = 0.55f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
            color = TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EngineStatLine(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = valueColor,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun DiscreteSliderRow(
    title: String,
    labels: List<String>,
    selectedIndex: Int,
    description: String,
    accentColor: Color,
    onIndexChange: (Int) -> Unit,
    onHelpClick: () -> Unit,
    helpTestTag: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.width(142.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onHelpClick,
                    modifier = Modifier
                        .then(if (helpTestTag != null) Modifier.testTag(helpTestTag) else Modifier)
                        .size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                        contentDescription = "Help for $title",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    labels.forEachIndexed { i, lbl ->
                        Text(
                            text = lbl,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (i == selectedIndex) TextPrimary else TextSecondary,
                            fontWeight = if (i == selectedIndex) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
                Slider(
                    value = selectedIndex.toFloat(),
                    onValueChange = { onIndexChange(it.toInt().coerceIn(0, labels.lastIndex)) },
                    valueRange = 0f..labels.lastIndex.toFloat(),
                    steps = (labels.size - 2).coerceAtLeast(0),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFD5DBE8),
                        activeTrackColor = accentColor,
                        inactiveTrackColor = CyberBorder
                    )
                )
            }
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = CyberBorder.copy(alpha = 0.45f))
    }
}

@Composable
fun ControlPanelSettingSection(
    selectedLayout: ControlPanelLayout,
    onSelectLayout: (ControlPanelLayout) -> Unit
) {
    val accent = LocalTurboAccent.current.accent
    val layouts = ControlPanelLayout.entries

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Choose how the floating Game Mode handle appears during gameplay:",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        for (rowIdx in 0 until 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                for (colIdx in 0 until 2) {
                    val layout = layouts[rowIdx * 2 + colIdx]
                    val isSelected = layout == selectedLayout
                    ControlLayoutPreviewCard(
                        layout = layout,
                        selected = isSelected,
                        accentColor = accent,
                        onClick = { onSelectLayout(layout) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ControlLayoutPreviewCard(
    layout: ControlPanelLayout,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .testTag("control_layout_${layout.name}")
            .height(128.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF1D202C))
            .border(
                width = 2.dp,
                color = if (selected) accentColor else Color(0xFFB4BCD0),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        when (layout) {
            ControlPanelLayout.LEFT_EDGE_SWIPE,
            ControlPanelLayout.LEFT_EDGE_MINIMAL -> {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight(0.75f)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("----- Drag to Open Game Mode", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                }
            }
            ControlPanelLayout.RIGHT_EDGE_MENU -> {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Hide menu ----", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = TextPrimary)
                        Text("Open Game Mode ----", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = TextPrimary)
                        Text("Open settings ----", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = TextPrimary)
                        Text("Close All ----", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = TextPrimary)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White)
                            .padding(3.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Rounded.ExpandMore, null, tint = ObsidianVoid, modifier = Modifier.size(12.dp))
                        Icon(Icons.Rounded.Fullscreen, null, tint = ObsidianVoid, modifier = Modifier.size(12.dp))
                        Icon(Icons.Rounded.Settings, null, tint = ObsidianVoid, modifier = Modifier.size(12.dp))
                        Icon(Icons.Rounded.Close, null, tint = ObsidianVoid, modifier = Modifier.size(12.dp))
                        Icon(Icons.Rounded.SwapVert, null, tint = ObsidianVoid, modifier = Modifier.size(12.dp))
                    }
                }
            }
            ControlPanelLayout.TOP_G_HANDLE -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp, 22.dp)
                            .background(Color(0xFF701822), RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("G", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Open Game Mode", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                }
            }
        }
    }
}

@Composable
fun GamePanelStyleSection(
    selectedStyle: GamePanelStyle,
    onSelectStyle: (GamePanelStyle) -> Unit
) {
    val accent = LocalTurboAccent.current.accent
    val rows = remember { GamePanelStyle.entries.chunked(2) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = "Choose your in-game control panel aesthetic. Switch instantly between Cyber HUD and pro esports overlays:",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        rows.forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                pair.forEach { style ->
                    GamePanelStyleCard(
                        style = style,
                        selected = style == selectedStyle,
                        fallbackAccent = accent,
                        onClick = { onSelectStyle(style) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun GamePanelStyleCard(
    style: GamePanelStyle,
    selected: Boolean,
    fallbackAccent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val styleAccent = when (style) {
        GamePanelStyle.CLASSIC -> CyberAmber
        GamePanelStyle.NEW -> fallbackAccent
        GamePanelStyle.ROG_STYLE -> Color(0xFFFF1744)
        GamePanelStyle.XIAOMI_STYLE -> Color(0xFFFFB703)
        GamePanelStyle.REDMAGIC_STYLE -> Color(0xFF00F0FF)
    }
    val cardScale by animateFloatAsState(
        targetValue = if (selected) 1.02f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
        label = "style_card_scale_${style.name}"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) styleAccent else CyberBorder,
        animationSpec = tween(220),
        label = "style_card_border_${style.name}"
    )
    val cardShape = when (style) {
        GamePanelStyle.ROG_STYLE -> CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp, topEnd = 6.dp, bottomStart = 6.dp)
        GamePanelStyle.REDMAGIC_STYLE -> CutCornerShape(12.dp)
        GamePanelStyle.XIAOMI_STYLE -> RoundedCornerShape(18.dp)
        else -> RoundedCornerShape(12.dp)
    }

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .testTag("panel_style_${style.name}")
            .minimumInteractiveComponentSize()
            .clip(cardShape)
            .background(if (selected) styleAccent.copy(alpha = 0.08f) else Color(0xFF0C101B))
            .border(
                width = if (selected) 2.dp else 1.2.dp,
                color = borderColor,
                shape = cardShape
            )
            .clickable { onClick() }
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(98.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF070910))
                .padding(6.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                when (style) {
                    GamePanelStyle.CLASSIC -> {
                        drawRect(Color(0xFF1A1E2B), topLeft = Offset.Zero, size = Size(w * 0.34f, h))
                        drawRect(Color(0xFF1A1E2B), topLeft = Offset(w * 0.66f, 0f), size = Size(w * 0.34f, h))
                        drawCircle(CyberAmber, radius = 11.dp.toPx(), center = Offset(w * 0.22f, h * 0.45f), style = Stroke(2.dp.toPx()))
                        drawCircle(CyberAmber, radius = 11.dp.toPx(), center = Offset(w * 0.78f, h * 0.45f), style = Stroke(2.dp.toPx()))
                    }
                    GamePanelStyle.NEW -> {
                        val leftWing = Path().apply {
                            moveTo(0f, 0f); lineTo(w * 0.30f, 0f); lineTo(w * 0.40f, h * 0.45f); lineTo(w * 0.24f, h); lineTo(0f, h); close()
                        }
                        val rightWing = Path().apply {
                            moveTo(w, 0f); lineTo(w * 0.70f, 0f); lineTo(w * 0.60f, h * 0.45f); lineTo(w * 0.76f, h); lineTo(w, h); close()
                        }
                        drawPath(leftWing, Color(0xFF1A1E2B))
                        drawPath(rightWing, Color(0xFF1A1E2B))
                        drawPath(leftWing, fallbackAccent, style = Stroke(1.8.dp.toPx()))
                        drawPath(rightWing, fallbackAccent, style = Stroke(1.8.dp.toPx()))
                        drawCircle(fallbackAccent, radius = 10.dp.toPx(), center = Offset(w * 0.22f, h * 0.44f), style = Stroke(1.8.dp.toPx()))
                        drawCircle(fallbackAccent, radius = 10.dp.toPx(), center = Offset(w * 0.78f, h * 0.44f), style = Stroke(1.8.dp.toPx()))
                    }
                    GamePanelStyle.ROG_STYLE -> {
                        // Half-screen ROG Game Genie crimson tactical dock
                        val rogDock = Path().apply {
                            moveTo(w * 0.05f, h * 0.06f); lineTo(w * 0.56f, h * 0.06f)
                            lineTo(w * 0.60f, h * 0.16f); lineTo(w * 0.60f, h * 0.94f)
                            lineTo(w * 0.05f, h * 0.94f); close()
                        }
                        drawPath(rogDock, Color(0xFF131622))
                        drawPath(rogDock, Color(0xFFFF1744), style = Stroke(1.8.dp.toPx()))
                        drawRoundRect(Color(0xFFFF1744), Offset(w * 0.10f, h * 0.14f), Size(w * 0.43f, h * 0.10f), CornerRadius(3.dp.toPx()))
                        for (c in 0 until 3) {
                            drawRoundRect(
                                color = if (c == 2) Color(0xFFFF1744) else Color(0xFF232838),
                                topLeft = Offset(w * (0.10f + c * 0.15f), h * 0.30f),
                                size = Size(w * 0.13f, h * 0.14f),
                                cornerRadius = CornerRadius(3.dp.toPx())
                            )
                            drawRoundRect(
                                color = Color(0xFF1E2232),
                                topLeft = Offset(w * (0.10f + c * 0.15f), h * 0.52f),
                                size = Size(w * 0.13f, h * 0.32f),
                                cornerRadius = CornerRadius(3.dp.toPx())
                            )
                        }
                    }
                    GamePanelStyle.XIAOMI_STYLE -> {
                        // Left frosted app rail + GameTurbo sidebar + amber FPS waveform
                        drawRoundRect(
                            color = Color(0xFF111319),
                            topLeft = Offset(w * 0.04f, h * 0.04f),
                            size = Size(w * 0.12f, h * 0.92f),
                            cornerRadius = CornerRadius(7.dp.toPx())
                        )
                        drawRoundRect(
                            color = Color(0xFF1A1D24),
                            topLeft = Offset(w * 0.19f, h * 0.04f),
                            size = Size(w * 0.42f, h * 0.92f),
                            cornerRadius = CornerRadius(10.dp.toPx())
                        )
                        val wave = Path().apply {
                            moveTo(w * 0.23f, h * 0.36f)
                            lineTo(w * 0.31f, h * 0.24f)
                            lineTo(w * 0.39f, h * 0.34f)
                            lineTo(w * 0.48f, h * 0.20f)
                            lineTo(w * 0.57f, h * 0.22f)
                        }
                        drawPath(wave, Color(0xFFFFB703), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                        drawRoundRect(
                            color = Color(0xFFFFB703),
                            topLeft = Offset(w * 0.23f, h * 0.46f),
                            size = Size(w * 0.34f, h * 0.13f),
                            cornerRadius = CornerRadius(4.dp.toPx())
                        )
                        for (c in 0 until 3) {
                            drawRoundRect(
                                color = if (c == 0) Color(0xFFFFB703) else Color(0xFF282C37),
                                topLeft = Offset(w * (0.23f + c * 0.12f), h * 0.66f),
                                size = Size(w * 0.10f, h * 0.20f),
                                cornerRadius = CornerRadius(4.dp.toPx())
                            )
                        }
                    }
                    GamePanelStyle.REDMAGIC_STYLE -> {
                        // Half-screen RedMagic GameSpace cyber mecha dock
                        drawRoundRect(
                            color = Color(0xFF0C101B),
                            topLeft = Offset(w * 0.04f, h * 0.05f),
                            size = Size(w * 0.54f, h * 0.90f),
                            cornerRadius = CornerRadius(6.dp.toPx())
                        )
                        drawLine(Color(0xFFFF1E42), Offset(w * 0.04f, h * 0.05f), Offset(w * 0.04f, h * 0.95f), strokeWidth = 2.4.dp.toPx(), cap = StrokeCap.Round)
                        drawLine(Color(0xFF00F0FF), Offset(w * 0.58f, h * 0.05f), Offset(w * 0.58f, h * 0.95f), strokeWidth = 2.4.dp.toPx(), cap = StrokeCap.Round)
                        val r = 9.dp.toPx()
                        drawCircle(Color(0xFFFF1E42), radius = r, center = Offset(w * 0.20f, h * 0.32f), style = Stroke(2.dp.toPx()))
                        drawCircle(Color(0xFF00F0FF), radius = r, center = Offset(w * 0.42f, h * 0.32f), style = Stroke(2.dp.toPx()))
                        for (c in 0 until 3) {
                            drawRoundRect(
                                color = if (c == 0) Color(0xFF231320) else Color(0xFF0D232E),
                                topLeft = Offset(w * (0.09f + c * 0.155f), h * 0.56f),
                                size = Size(w * 0.135f, h * 0.30f),
                                cornerRadius = CornerRadius(3.dp.toPx())
                            )
                        }
                    }
                }
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(styleAccent)
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "ACTIVE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (style == GamePanelStyle.REDMAGIC_STYLE || style == GamePanelStyle.XIAOMI_STYLE) Color(0xFF04101A) else Color.White
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = style.label,
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold),
            color = if (selected) styleAccent else TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Text(
            text = style.tagline,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
