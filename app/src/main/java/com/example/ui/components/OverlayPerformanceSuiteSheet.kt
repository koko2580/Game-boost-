package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CapabilityStatus
import com.example.data.FrameDropRiskLevel
import com.example.data.GameTurboSettings
import com.example.data.GamingOverlayPerfUiState
import com.example.data.HardwareTelemetry
import com.example.data.PerformanceEngineStatus
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSlate
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class PerfSuiteTab(val label: String, val icon: ImageVector) {
    COOLING("Cooling", Icons.Rounded.AcUnit),
    ESPORTS("Esports", Icons.Rounded.SportsEsports),
    FPS60("60 FPS", Icons.Rounded.Speed),
    BOOST("Boost", Icons.Rounded.Bolt),
    FRAME_DROP("Drop Guard", Icons.Rounded.Shield),
    FPS_OPT("FPS Opt", Icons.Rounded.Tune),
    HIGH_FPS("High FPS", Icons.Rounded.GraphicEq)
}

@Composable
internal fun OverlayPerformanceSuiteSheet(
    visible: Boolean,
    initialTab: PerfSuiteTab,
    settings: GameTurboSettings,
    telemetry: HardwareTelemetry,
    perfStatus: PerformanceEngineStatus,
    perfState: GamingOverlayPerfUiState,
    accent: Color,
    callbacks: OverlayCallbacks,
    onDismiss: () -> Unit
) {
    if (!visible) return

    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }
    val animateVisuals = visible && !settings.lowRamOverlayMode
    val scrimInteraction = remember { MutableInteractionSource() }

    BoxWithConstraints(
        modifier = Modifier
            .testTag("perf_suite_sheet_root")
            .fillMaxSize()
    ) {
        val isWide = maxWidth >= 520.dp
        val drawerWidth = if (isWide) (maxWidth * 0.62f).coerceIn(360.dp, 560.dp) else (maxWidth - 16.dp).coerceAtMost(450.dp)
        val drawerMaxHeight = if (isWide) (maxHeight - 16.dp).coerceIn(280.dp, 420.dp) else (maxHeight * 0.76f).coerceIn(360.dp, 620.dp)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.56f))
                .clickable(interactionSource = scrimInteraction, indication = null, onClick = onDismiss)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width(drawerWidth)
                    .heightIn(max = drawerMaxHeight)
                    .clip(RoundedCornerShape(18.dp))
                    .background(ObsidianVoid.copy(alpha = 0.98f))
                    .border(1.5.dp, accent.copy(alpha = 0.8f), RoundedCornerShape(18.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PERFORMANCE SUITE • 7 MODES",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = RajdhaniFontFamily,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = accent,
                            maxLines = 1
                        )
                        Text(
                            text = "${telemetry.batteryTempCelsius}°C • ${perfStatus.thermalStatusLabel} • Panel ${telemetry.displayRefreshRateHz}Hz • RAM ${telemetry.ramUsedPercent}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .testTag("perf_suite_close_btn")
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close performance suite", tint = TextPrimary)
                    }
                }

                // Feature Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PerfSuiteTab.entries.forEach { tab ->
                        val selected = selectedTab == tab
                        val activeDot = when (tab) {
                            PerfSuiteTab.COOLING -> perfState.xCoolingEngineActive
                            PerfSuiteTab.ESPORTS -> settings.esportsModeEnabled
                            PerfSuiteTab.FPS60 -> settings.fps60StableEnabled
                            PerfSuiteTab.BOOST -> settings.performanceBoostActive
                            PerfSuiteTab.FRAME_DROP -> settings.frameDropProtectionEnabled
                            PerfSuiteTab.FPS_OPT -> settings.fpsOptimizationTarget > 0
                            PerfSuiteTab.HIGH_FPS -> settings.highFpsModeEnabled
                        }
                        Row(
                            modifier = Modifier
                                .testTag("perf_tab_${tab.name}")
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) accent.copy(alpha = 0.22f) else CyberSlate)
                                .border(1.dp, if (selected) accent else CyberBorder.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                                .clickable { selectedTab = tab }
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(tab.icon, contentDescription = null, tint = if (selected) accent else TextSecondary, modifier = Modifier.size(14.dp))
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (selected) TextPrimary else TextSecondary,
                                maxLines = 1
                            )
                            if (activeDot) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldBright)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = CyberBorder.copy(alpha = 0.55f))

                // Active Feature Panel
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(120)) },
                        label = "perf_suite_tab_content"
                    ) { tab ->
                        when (tab) {
                            PerfSuiteTab.COOLING -> XCoolingEngineCard(
                                animateVisuals = animateVisuals,
                                telemetry = telemetry,
                                perfStatus = perfStatus,
                                perfState = perfState,
                                callbacks = callbacks
                            )
                            PerfSuiteTab.ESPORTS -> GamingEsportsModeCard(
                                animateVisuals = animateVisuals,
                                settings = settings,
                                accent = accent,
                                callbacks = callbacks
                            )
                            PerfSuiteTab.FPS60 -> Fps60StableCard(
                                animateVisuals = animateVisuals,
                                settings = settings,
                                telemetry = telemetry,
                                perfState = perfState,
                                callbacks = callbacks
                            )
                            PerfSuiteTab.BOOST -> PerformanceBoostCard(
                                animateVisuals = animateVisuals,
                                settings = settings,
                                perfState = perfState,
                                accent = accent,
                                callbacks = callbacks
                            )
                            PerfSuiteTab.FRAME_DROP -> FrameDropProtectionCard(
                                settings = settings,
                                perfState = perfState,
                                callbacks = callbacks
                            )
                            PerfSuiteTab.FPS_OPT -> FpsOptimizationCard(
                                animateVisuals = animateVisuals,
                                settings = settings,
                                telemetry = telemetry,
                                perfState = perfState,
                                accent = accent,
                                callbacks = callbacks
                            )
                            PerfSuiteTab.HIGH_FPS -> HighFpsModeCard(
                                animateVisuals = animateVisuals,
                                settings = settings,
                                telemetry = telemetry,
                                perfState = perfState,
                                callbacks = callbacks
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun XCoolingEngineCard(
    animateVisuals: Boolean,
    telemetry: HardwareTelemetry,
    perfStatus: PerformanceEngineStatus,
    perfState: GamingOverlayPerfUiState,
    callbacks: OverlayCallbacks
) {
    var confirmActionVisible by remember { mutableStateOf(false) }
    val reading = perfState.latestCoolingReading
    val active = perfState.xCoolingEngineActive
    val tempC = reading?.batteryTempCelsius ?: telemetry.batteryTempCelsius
    val tempColor = when {
        tempC >= 42f -> Color(0xFFFF3D57)
        tempC >= 38.5f -> CyberAmber
        else -> ElectricCyan
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CyberSlate)
                .border(1.dp, if (active) ElectricCyan else CyberBorder, RoundedCornerShape(12.dp))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CoolingEngineFanCanvas(
                spinning = active && animateVisuals,
                bladeColor = if (active) ElectricCyan else TextMuted,
                sizeDp = 56.dp
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "X Cooling Engine",
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    CapabilityTagBadge(
                        text = if (active) "Active (Manual)" else "Standby",
                        color = if (active) ElectricCyan else TextMuted
                    )
                }
                Text(
                    text = "Battery: ${tempC}°C (${if (reading?.isCharging ?: telemetry.isCharging) "Charging" else "Discharging"})",
                    style = MaterialTheme.typography.bodySmall,
                    color = tempColor
                )
                Text(
                    text = "Thermal: ${reading?.thermalStatusLabel ?: perfStatus.thermalStatusLabel} • Headroom: ${reading?.headroomLabel ?: perfStatus.thermalHeadroomSeconds?.let { String.format(java.util.Locale.US, "%.2fx", it) } ?: "Unavailable"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                if (reading != null) {
                    Text(
                        text = "Last reading: ${reading.timeLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { callbacks.onToggleCoolingEngine(!active) },
                colors = ButtonDefaults.buttonColors(containerColor = if (active) Color(0xFF8E1B2B) else ElectricCyan),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("cooling_toggle_btn")
            ) {
                Text(
                    text = if (active) "Stop Cooling Monitor" else "Start X Cooling Engine",
                    color = if (active) Color.White else Color(0xFF04141D),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
            OutlinedButton(
                onClick = { confirmActionVisible = true },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("cooling_apply_action_btn")
            ) {
                Text("Cool Prep", color = TextPrimary)
            }
        }

        if (confirmActionVisible) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF162033))
                    .border(1.dp, ElectricCyan.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Confirm heat-reduction actions: disable Turbo X UI animations, engage Sustained Performance (if supported), and trim Turbo X caches?",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            confirmActionVisible = false
                            callbacks.onApplyCoolingPrep()
                        },
                        modifier = Modifier.testTag("cooling_confirm_action_btn")
                    ) {
                        Text("Confirm & Apply")
                    }
                    OutlinedButton(onClick = { confirmActionVisible = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = callbacks.onOpenDisplaySettings,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("cooling_display_settings_btn")
            ) {
                Text("Display / Brightness", fontSize = 11.sp, color = TextPrimary, maxLines = 1)
            }
            OutlinedButton(
                onClick = callbacks.onOpenBatterySaverSettings,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("cooling_battery_settings_btn")
            ) {
                Text("Battery Saver", fontSize = 11.sp, color = TextPrimary, maxLines = 1)
            }
        }

        Text(
            text = "Non-root thermal guide: software cannot add hardware fans. For cooler sustained play, unplug fast charging if >39°C, lower screen brightness by 15%, and cap in-game graphics/FPS if Thermal reaches HIGH.",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
    }
}

@Composable
private fun GamingEsportsModeCard(
    animateVisuals: Boolean,
    settings: GameTurboSettings,
    accent: Color,
    callbacks: OverlayCallbacks
) {
    val active = settings.esportsModeEnabled
    val glowAlpha = if (active && animateVisuals) {
        val inf = rememberInfiniteTransition(label = "esports_glow")
        val v by inf.animateFloat(
            initialValue = 0.45f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "esports_glow_alpha"
        )
        v
    } else if (active) {
        0.85f
    } else {
        0.35f
    }
    val borderColor = if (active) accent.copy(alpha = glowAlpha) else CyberBorder

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberSlate)
            .border(if (active) 2.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Gaming Esports Mode",
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    CapabilityTagBadge(
                        text = if (active) "ESPORTS ACTIVE" else "OFF",
                        color = if (active) EmeraldBright else TextMuted
                    )
                }
                Text(
                    text = "Applies Acceleration CPU hint, low-latency unbuffered touch, ADPF + frame pacing, reduces overlay animations, and trims background cache. Restores prior Turbo X settings on disable.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        Button(
            onClick = { callbacks.onToggleEsportsMode(!active) },
            colors = ButtonDefaults.buttonColors(containerColor = if (active) Color(0xFF283247) else accent),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("esports_mode_toggle_btn")
        ) {
            Text(
                text = if (active) "Disable & Restore Prior Settings" else "Activate Gaming Esports Mode",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(
                onClick = callbacks.onOpenDisplaySettings,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("esports_display_btn")
            ) {
                Text("Display", fontSize = 11.sp, color = TextPrimary)
            }
            OutlinedButton(
                onClick = callbacks.onOpenSoundSettings,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("esports_sound_btn")
            ) {
                Text("Sound", fontSize = 11.sp, color = TextPrimary)
            }
            OutlinedButton(
                onClick = callbacks.onOpenAppSettings,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("esports_apps_btn")
            ) {
                Text("Apps", fontSize = 11.sp, color = TextPrimary)
            }
        }
    }
}

@Composable
private fun Fps60StableCard(
    animateVisuals: Boolean,
    settings: GameTurboSettings,
    telemetry: HardwareTelemetry,
    perfState: GamingOverlayPerfUiState,
    callbacks: OverlayCallbacks
) {
    val status = perfState.fps60Capability
    val statusColor = capabilityColor(status)
    val enabled = settings.fps60StableEnabled

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CyberSlate)
                .border(1.dp, statusColor.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TargetArcMeterCanvas(
                targetLabel = "60",
                unitLabel = "FPS TARGET",
                progress = if (enabled) 0.78f else 0.42f,
                arcColor = statusColor,
                animate = animateVisuals && enabled,
                sizeDp = 64.dp
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "60 FPS Stable",
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    CapabilityTagBadge(text = status.label, color = statusColor)
                }
                Text(
                    text = perfState.fps60Note.ifBlank { "Panel modes: ${telemetry.supportedRefreshRates.joinToString("/")}Hz" },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        Button(
            onClick = { callbacks.onToggle60FpsStable(!enabled) },
            enabled = status != CapabilityStatus.UNSUPPORTED || enabled,
            colors = ButtonDefaults.buttonColors(containerColor = if (enabled) Color(0xFF283247) else EmeraldBright),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("fps60_stable_toggle_btn")
        ) {
            Text(
                text = if (enabled) "Disable & Restore Display Target" else "Enable 60 FPS Stable Preference",
                color = if (enabled) Color.White else Color(0xFF04140B),
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = "60 FPS Setup Guide: Set in-game Frame Rate = High/60 and Shadows = Low/Medium. This targets Turbo X's window refresh preference and pacing hints — it does NOT measure or cap 3rd-party game rendering FPS.",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
    }
}

@Composable
private fun PerformanceBoostCard(
    animateVisuals: Boolean,
    settings: GameTurboSettings,
    perfState: GamingOverlayPerfUiState,
    accent: Color,
    callbacks: OverlayCallbacks
) {
    val active = settings.performanceBoostActive
    val ringColor = if (active) EmeraldBright else accent

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CyberSlate)
                .border(1.dp, ringColor.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TargetArcMeterCanvas(
                targetLabel = if (active) "ON" else "IDLE",
                unitLabel = "TURBO X",
                progress = if (active) 0.92f else 0.30f,
                arcColor = ringColor,
                animate = animateVisuals && active,
                sizeDp = 62.dp
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Performance Boost",
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = "Optimizes Turbo X animations, enables frame pacing + ADPF, clears app temp cache, and requests Android background trim. Never uses fake root actions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        Button(
            onClick = { callbacks.onTogglePerformanceBoost(!active) },
            colors = ButtonDefaults.buttonColors(containerColor = if (active) Color(0xFF283247) else accent),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("perf_boost_toggle_btn")
        ) {
            Text(
                text = if (active) "Restore Prior Turbo X Settings" else "Run Performance Boost Now",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        if (settings.lastPerfBoostSummary.isNotBlank()) {
            Text(
                text = settings.lastPerfBoostSummary,
                style = MaterialTheme.typography.labelSmall,
                color = EmeraldBright,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0E1D19))
                    .padding(8.dp)
                    .testTag("perf_boost_summary_text")
            )
        } else if (perfState.lastActionTimestampLabel.isNotBlank()) {
            Text(
                text = "Last action at ${perfState.lastActionTimestampLabel}",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun FrameDropProtectionCard(
    settings: GameTurboSettings,
    perfState: GamingOverlayPerfUiState,
    callbacks: OverlayCallbacks
) {
    val risk = perfState.frameDropRisk
    val riskColor = when (risk.level) {
        FrameDropRiskLevel.LOW -> EmeraldBright
        FrameDropRiskLevel.MODERATE -> CyberAmber
        FrameDropRiskLevel.HIGH -> Color(0xFFFF3D57)
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CyberSlate)
                .border(1.dp, riskColor.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Frame Drop Protection",
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    CapabilityTagBadge(text = "Risk: ${risk.level.label}", color = riskColor)
                }
                Text(
                    text = "Evaluated at ${risk.evaluatedTimeLabel}" +
                        if (risk.snoozedRemainingMinutes > 0) " • Alerts snoozed (${risk.snoozedRemainingMinutes}m)" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
            Switch(
                checked = settings.frameDropProtectionEnabled,
                onCheckedChange = callbacks.onToggleFrameDropProtection,
                modifier = Modifier.testTag("frame_drop_protection_toggle")
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF131826))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            risk.reasons.forEach { reason ->
                Text("• $reason", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = callbacks.onApplyFrameDropMitigation,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("frame_drop_mitigate_btn")
            ) {
                Text("Apply Mitigation", maxLines = 1)
            }
            OutlinedButton(
                onClick = callbacks.onSnoozeFrameDropWarning,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("frame_drop_snooze_btn")
            ) {
                Text("Snooze 10m", color = TextPrimary)
            }
        }

        Text(
            text = "Honest scope: Flags thermal throttling risk and memory load using Android PowerManager & RAM telemetry. Does not read 3rd-party game framebuffers.",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
    }
}

@Composable
private fun FpsOptimizationCard(
    animateVisuals: Boolean,
    settings: GameTurboSettings,
    telemetry: HardwareTelemetry,
    perfState: GamingOverlayPerfUiState,
    accent: Color,
    callbacks: OverlayCallbacks
) {
    val tiers = listOf(30, 60, 90, 120)
    val selected = settings.fpsOptimizationTarget
    val tips = when (selected) {
        30 -> "30 FPS tip: Best thermal endurance on heavy RPGs. Lock 30 FPS inside the game's graphics menu."
        60 -> "60 FPS tip: Ideal competitive balance. Pair Medium graphics with Frame Rate = High."
        90 -> "90 FPS tip: Ensure Smooth Display >=90Hz in Android Display Settings and turn off Battery Saver."
        120 -> "120 FPS tip: Keep thermals under 39°C and select Ultra/120Hz inside supported games."
        else -> "Choose a tier below. Consent controls whether Turbo X applies display mode preferences."
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(CyberSlate)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Allow Turbo X display preference updates",
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = settings.fpsOptDisplayConsent,
                onCheckedChange = callbacks.onSetFpsOptDisplayConsent,
                modifier = Modifier.testTag("fps_opt_consent_toggle")
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            tiers.forEach { tier ->
                val cap = perfState.fpsOptimizationStates[tier] ?: CapabilityStatus.UNSUPPORTED
                val isSelected = selected == tier
                val capColor = capabilityColor(cap)
                val ringAlpha = if (isSelected && animateVisuals) {
                    val inf = rememberInfiniteTransition(label = "fps_opt_ring_$tier")
                    val v by inf.animateFloat(
                        initialValue = 0.55f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(tween(950), RepeatMode.Reverse),
                        label = "fps_opt_alpha_$tier"
                    )
                    v
                } else if (isSelected) {
                    1f
                } else {
                    0.5f
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("fps_opt_card_$tier")
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) accent.copy(alpha = 0.22f) else CyberSlate)
                        .border(if (isSelected) 2.dp else 1.dp, if (isSelected) accent.copy(alpha = ringAlpha) else CyberBorder, RoundedCornerShape(10.dp))
                        .clickable { callbacks.onSelectFpsOptTarget(tier) }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "$tier",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold),
                        color = if (isSelected) Color.White else TextPrimary
                    )
                    Text("FPS", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(
                        text = cap.label,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        color = capColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Text(
            text = tips,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )

        if (selected > 0) {
            OutlinedButton(
                onClick = { callbacks.onSelectFpsOptTarget(0) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("fps_opt_restore_btn")
            ) {
                Text("Clear & Restore Prior Display Target", color = TextPrimary)
            }
        }
        Text(
            text = "Detected panel rates: ${telemetry.supportedRefreshRates.joinToString("/")}Hz",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
    }
}

@Composable
private fun HighFpsModeCard(
    animateVisuals: Boolean,
    settings: GameTurboSettings,
    telemetry: HardwareTelemetry,
    perfState: GamingOverlayPerfUiState,
    callbacks: OverlayCallbacks
) {
    val rates = listOf(60, 90, 120)
    val selectedHz = settings.highFpsTargetHz
    val enabled = settings.highFpsModeEnabled
    val selectedState = perfState.highFpsStates[selectedHz] ?: CapabilityStatus.UNSUPPORTED
    val gaugeColor = capabilityColor(selectedState)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CyberSlate)
                .border(1.dp, gaugeColor.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TargetArcMeterCanvas(
                targetLabel = "$selectedHz",
                unitLabel = "Hz PANEL",
                progress = (selectedHz / 120f).coerceIn(0.45f, 1f),
                arcColor = gaugeColor,
                animate = animateVisuals && enabled,
                sizeDp = 64.dp
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "High FPS Mode",
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    CapabilityTagBadge(text = selectedState.label, color = gaugeColor)
                }
                Text(
                    text = "Active display: ${telemetry.displayRefreshRateHz}Hz • Supported: ${telemetry.supportedRefreshRates.joinToString("/")}Hz",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            rates.forEach { hz ->
                val state = perfState.highFpsStates[hz] ?: CapabilityStatus.UNSUPPORTED
                val isSelected = selectedHz == hz
                val c = capabilityColor(state)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("high_fps_chip_$hz")
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) c.copy(alpha = 0.22f) else CyberSlate)
                        .border(if (isSelected) 2.dp else 1.dp, if (isSelected) c else CyberBorder, RoundedCornerShape(10.dp))
                        .clickable { callbacks.onConfigureHighFps(true, hz) }
                        .padding(vertical = 8.dp, horizontal = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "${hz}Hz",
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = state.label,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        color = c,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Button(
            onClick = { callbacks.onConfigureHighFps(!enabled, selectedHz) },
            colors = ButtonDefaults.buttonColors(containerColor = if (enabled) Color(0xFF283247) else gaugeColor),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("high_fps_toggle_btn")
        ) {
            Text(
                text = if (enabled) "Disable & Restore Display Mode" else "Enable ${selectedHz}Hz Window Preference",
                color = if (enabled) Color.White else Color(0xFF04141D),
                fontWeight = FontWeight.Bold
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = callbacks.onOpenDisplaySettings,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("high_fps_display_btn")
            ) {
                Text("Display Settings", fontSize = 11.sp, color = TextPrimary, maxLines = 1)
            }
            OutlinedButton(
                onClick = callbacks.onOpenBatterySaverSettings,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("high_fps_battery_btn")
            ) {
                Text("Battery Saver", fontSize = 11.sp, color = TextPrimary, maxLines = 1)
            }
        }

        Text(
            text = "Panel refresh rate (Hz) is the display's hardware cadence; actual game render FPS depends on each game's engine limits and device Smooth Display settings.",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
    }
}

@Composable
private fun CoolingEngineFanCanvas(
    spinning: Boolean,
    bladeColor: Color,
    sizeDp: Dp
) {
    val angle = if (spinning) {
        val inf = rememberInfiniteTransition(label = "cooling_fan_spin")
        val a by inf.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(850, easing = LinearEasing), RepeatMode.Restart),
            label = "cooling_fan_angle"
        )
        a
    } else {
        0f
    }

    Canvas(modifier = Modifier.size(sizeDp)) {
        val r = size.minDimension / 2f
        val c = Offset(size.width / 2f, size.height / 2f)
        drawCircle(color = bladeColor.copy(alpha = 0.14f), radius = r - 2f, center = c)
        drawCircle(color = bladeColor.copy(alpha = 0.65f), radius = r - 2f, center = c, style = Stroke(width = 2.dp.toPx()))
        rotate(degrees = angle, pivot = c) {
            val blades = 7
            for (i in 0 until blades) {
                val base = (i * 2.0 * PI / blades)
                val tipAngle = base + 0.38
                val p = Path().apply {
                    moveTo(c.x, c.y)
                    lineTo(c.x + (r * 0.42f * cos(base)).toFloat(), c.y + (r * 0.42f * sin(base)).toFloat())
                    lineTo(c.x + (r * 0.78f * cos(tipAngle)).toFloat(), c.y + (r * 0.78f * sin(tipAngle)).toFloat())
                    close()
                }
                drawPath(path = p, color = bladeColor.copy(alpha = 0.88f))
            }
        }
        drawCircle(color = ObsidianVoid, radius = r * 0.22f, center = c)
        drawCircle(color = bladeColor, radius = r * 0.14f, center = c)
    }
}

@Composable
private fun TargetArcMeterCanvas(
    targetLabel: String,
    unitLabel: String,
    progress: Float,
    arcColor: Color,
    animate: Boolean,
    sizeDp: Dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = if (animate) tween(450, easing = FastOutSlowInEasing) else tween(0),
        label = "arc_meter_progress"
    )
    val sweepPulse = if (animate) {
        val inf = rememberInfiniteTransition(label = "arc_pulse")
        val p by inf.animateFloat(
            initialValue = 0.75f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "arc_pulse_alpha"
        )
        p
    } else {
        0.9f
    }

    Box(modifier = Modifier.size(sizeDp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 5.dp.toPx()
            val inset = stroke / 2f + 2f
            val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
            drawArc(
                color = CyberBorder.copy(alpha = 0.5f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = arcColor.copy(alpha = sweepPulse),
                startAngle = 135f,
                sweepAngle = 270f * animatedProgress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = targetLabel,
                style = MaterialTheme.typography.titleSmall.copy(fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold),
                color = TextPrimary,
                maxLines = 1
            )
            Text(
                text = unitLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp),
                color = TextSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CapabilityTagBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.16f))
            .border(1.dp, color.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
            color = color,
            maxLines = 1
        )
    }
}

@Composable
internal fun FrameDropAlertPill(
    riskLevel: FrameDropRiskLevel,
    onOpenGuard: () -> Unit,
    onSnooze: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (riskLevel == FrameDropRiskLevel.HIGH) Color(0xFFFF3D57) else CyberAmber
    Row(
        modifier = modifier
            .testTag("frame_drop_alert_banner")
            .clip(RoundedCornerShape(999.dp))
            .background(ObsidianVoid.copy(alpha = 0.94f))
            .border(1.2.dp, color, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Rounded.WarningAmber, contentDescription = "Frame drop risk", tint = color, modifier = Modifier.size(15.dp))
        Text(
            text = "Frame Drop Risk: ${riskLevel.label}",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary,
            modifier = Modifier
                .clickable { onOpenGuard() }
                .testTag("frame_drop_banner_open")
        )
        Text(
            text = "Snooze 10m",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = color,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable { onSnooze() }
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .testTag("frame_drop_banner_snooze")
        )
    }
}

private fun capabilityColor(status: CapabilityStatus): Color = when (status) {
    CapabilityStatus.ACTIVE -> EmeraldBright
    CapabilityStatus.SUPPORTED -> ElectricCyan
    CapabilityStatus.APP_PACING -> ElectricCyan
    CapabilityStatus.WARNING -> CyberAmber
    CapabilityStatus.REQUIRES_SETTINGS -> CyberAmber
    CapabilityStatus.UNSUPPORTED -> Color(0xFFFF5252)
}
