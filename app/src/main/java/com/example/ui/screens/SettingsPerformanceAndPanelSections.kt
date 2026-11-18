package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    val onOpenDeveloperOptions: () -> Unit
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
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Real-time ADPF performance hinting, refresh-rate locking, unbuffered touch dispatch, and GPU pipeline tuning.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Live Performance Engine Diagnostics Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MidnightSurface)
                .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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
            EngineStatLine(
                label = "Display & FPS",
                value = "${engineStatus.appliedDisplayModeHz}Hz mode • ${telemetry.actualFps} FPS live (Max ${panelMaxHz}Hz)",
                valueColor = EmeraldBright
            )
            EngineStatLine(
                label = "CPU Boost Engine",
                value = buildString {
                    append(if (engineStatus.adpfHintActive) "ADPF Hint (${String.format(Locale.US, "%.1f", engineStatus.adpfTargetDurationMs)}ms)" else "Thread Priority")
                    if (engineStatus.gameStateActive) append(" • GameState")
                },
                valueColor = if (engineStatus.adpfHintActive || engineStatus.threadPriorityBoosted) ElectricCyan else TextSecondary
            )
            EngineStatLine(
                label = "GPU Pipeline",
                value = "${engineStatus.activeGpuPipelineLabel} • ${engineStatus.gpuInfo.rendererName}",
                valueColor = TextPrimary
            )
            EngineStatLine(
                label = "Touch & Scroll",
                value = "${if (engineStatus.unbufferedTouchActive) "Unbuffered Dispatch ON" else "Standard Dispatch"} • Glide x${String.format(Locale.US, "%.2f", engineStatus.scrollMultiplier * engineStatus.frictionFactor)}",
                valueColor = if (engineStatus.unbufferedTouchActive) EmeraldBright else TextSecondary
            )
            if (!permissions.writeSettingsGranted) {
                Text(
                    text = "Tip: Grant 'Modify System Settings' to sync system pointer speed (${engineStatus.appliedPointerSpeed}). Tap here ->",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberAmber,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { actions.onOpenPermissions() }
                        .padding(vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
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
    onHelpClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
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
                IconButton(onClick = onHelpClick, modifier = Modifier.size(30.dp)) {
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
            .height(146.dp)
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        GamePanelStyle.entries.forEach { style ->
            val selected = style == selectedStyle
            Column(
                modifier = Modifier
                    .weight(1f)
                    .testTag("panel_style_${style.name}")
                    .clickable { onSelectStyle(style) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF090B12))
                        .border(
                            width = 2.dp,
                            color = if (selected) accent else Color.White.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(8.dp)
                ) {
                    // Mini wireframe preview of Classic vs New angled wing HUD
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val wingColor = CyberAmber.copy(alpha = 0.85f)
                        if (style == GamePanelStyle.CLASSIC) {
                            drawRect(Color(0xFF1A1E2B), topLeft = Offset.Zero, size = androidx.compose.ui.geometry.Size(w * 0.32f, h))
                            drawRect(Color(0xFF1A1E2B), topLeft = Offset(w * 0.68f, 0f), size = androidx.compose.ui.geometry.Size(w * 0.32f, h))
                            drawCircle(wingColor, radius = 14.dp.toPx(), center = Offset(w * 0.22f, h * 0.42f), style = Stroke(2.dp.toPx()))
                            drawCircle(wingColor, radius = 14.dp.toPx(), center = Offset(w * 0.78f, h * 0.42f), style = Stroke(2.dp.toPx()))
                        } else {
                            val leftWing = Path().apply {
                                moveTo(0f, 0f)
                                lineTo(w * 0.30f, 0f)
                                lineTo(w * 0.40f, h * 0.45f)
                                lineTo(w * 0.25f, h)
                                lineTo(0f, h)
                                close()
                            }
                            val rightWing = Path().apply {
                                moveTo(w, 0f)
                                lineTo(w * 0.70f, 0f)
                                lineTo(w * 0.60f, h * 0.45f)
                                lineTo(w * 0.75f, h)
                                lineTo(w, h)
                                close()
                            }
                            drawPath(leftWing, Color(0xFF1A1E2B))
                            drawPath(rightWing, Color(0xFF1A1E2B))
                            drawPath(leftWing, wingColor, style = Stroke(2.dp.toPx()))
                            drawPath(rightWing, wingColor, style = Stroke(2.dp.toPx()))
                            drawCircle(wingColor, radius = 12.dp.toPx(), center = Offset(w * 0.24f, h * 0.42f), style = Stroke(2.dp.toPx()))
                            drawCircle(wingColor, radius = 12.dp.toPx(), center = Offset(w * 0.76f, h * 0.42f), style = Stroke(2.dp.toPx()))
                        }
                    }
                    Text(
                        text = if (style == GamePanelStyle.CLASSIC) "0.88 GHz  •  43%" else "Angled HUD",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = RajdhaniFontFamily),
                        color = CyberAmber,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = style.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) accent else TextPrimary
                )
            }
        }
    }
}
