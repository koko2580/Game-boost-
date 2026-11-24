package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.extreme.CapabilityCategory
import com.example.extreme.DeviceCapabilityReport
import com.example.extreme.ExtremeModeState
import com.example.extreme.ExtremeUiState
import com.example.extreme.LiveExtremeMetrics
import com.example.extreme.SelectedGameInfo
import com.example.extreme.ThermalProtectionLevel
import com.example.ui.theme.CrimsonTurbo
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

@Composable
fun SuperHighPerformanceScreen(
    uiState: ExtremeUiState,
    adReady: Boolean,
    adLoading: Boolean,
    adError: String?,
    adCooldownSeconds: Int,
    statusMessage: String?,
    peekAppIcon: (String) -> ImageBitmap?,
    loadAppIconAsync: suspend (String) -> ImageBitmap?,
    onSelectGame: (String?) -> Unit,
    onWatchUnlockAd: () -> Unit,
    onActivate: () -> Unit,
    onDeactivate: () -> Unit,
    onToggleAutoStart: (Boolean) -> Unit,
    onViewWarningInfo: () -> Unit,
    onRequestUsageAccess: () -> Unit,
    onNavigateAddGames: () -> Unit,
    onRunPingTest: () -> Unit,
    onBack: () -> Unit
) {
    val palette = LocalTurboAccent.current
    var showCapabilitiesDetail by remember { mutableStateOf(true) }

    val stateColor = when (uiState.modeState) {
        ExtremeModeState.ACTIVE -> EmeraldBright
        ExtremeModeState.ACTIVATING -> ElectricCyan
        ExtremeModeState.THERMAL_PROTECTED -> CrimsonTurbo
        ExtremeModeState.UNLOCKED -> ElectricCyan
        ExtremeModeState.INACTIVE -> TextSecondary
        ExtremeModeState.LOCKED -> CyberAmber
    }

    Box(
        modifier = Modifier
            .testTag("super_perf_screen_root")
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ObsidianVoid, Color(0xFF0A0E1A), ObsidianVoid)
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWide = maxWidth >= 680.dp

            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .testTag("super_perf_back_btn")
                                .minimumInteractiveComponentSize()
                                .clip(CircleShape)
                                .background(CyberSlate.copy(alpha = 0.88f))
                                .border(1.dp, CyberBorder, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }
                        Column {
                            Text(
                                text = "SUPER HIGH PERFORMANCE",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = RajdhaniFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                ),
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "SELECTED GAME ONLY • EXTREME ENGINE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = RajdhaniFontFamily,
                                    letterSpacing = 1.1.sp
                                ),
                                color = palette.accent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onViewWarningInfo,
                        modifier = Modifier
                            .testTag("super_perf_view_warning_btn")
                            .minimumInteractiveComponentSize(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberAmber)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = "View Warning Info",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Warning Info",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = RajdhaniFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                    }
                }

                if (statusMessage != null) {
                    Surface(
                        color = CyberElevated.copy(alpha = 0.95f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .border(1.dp, palette.accent.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                    ) {
                        Text(
                            text = statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 1040.dp)
                            .fillMaxWidth()
                    ) {
                        if (isWide) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    EngineStatusAndControlsCard(
                                        uiState = uiState,
                                        stateColor = stateColor,
                                        adReady = adReady,
                                        adLoading = adLoading,
                                        adError = adError,
                                        adCooldownSeconds = adCooldownSeconds,
                                        onWatchUnlockAd = onWatchUnlockAd,
                                        onActivate = onActivate,
                                        onDeactivate = onDeactivate,
                                        onToggleAutoStart = onToggleAutoStart,
                                        onViewWarningInfo = onViewWarningInfo,
                                        onRequestUsageAccess = onRequestUsageAccess
                                    )
                                    SelectedGameOnlyCard(
                                        games = uiState.availableGames,
                                        selectedGame = uiState.selectedGame,
                                        isActive = uiState.isActiveOrProtecting,
                                        accentColor = palette.accent,
                                        peekAppIcon = peekAppIcon,
                                        loadAppIconAsync = loadAppIconAsync,
                                        onSelectGame = onSelectGame,
                                        onManageGames = onNavigateAddGames
                                    )
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    LiveOptimizationMonitoringCard(
                                        metrics = uiState.metrics,
                                        onRunPingTest = onRunPingTest
                                    )
                                    DeviceCapabilitiesCard(
                                        report = uiState.capabilityReport,
                                        expanded = showCapabilitiesDetail,
                                        onToggleExpand = { showCapabilitiesDetail = !showCapabilitiesDetail }
                                    )
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                EngineStatusAndControlsCard(
                                    uiState = uiState,
                                    stateColor = stateColor,
                                    adReady = adReady,
                                    adLoading = adLoading,
                                    adError = adError,
                                    adCooldownSeconds = adCooldownSeconds,
                                    onWatchUnlockAd = onWatchUnlockAd,
                                    onActivate = onActivate,
                                    onDeactivate = onDeactivate,
                                    onToggleAutoStart = onToggleAutoStart,
                                    onViewWarningInfo = onViewWarningInfo,
                                    onRequestUsageAccess = onRequestUsageAccess
                                )
                                SelectedGameOnlyCard(
                                    games = uiState.availableGames,
                                    selectedGame = uiState.selectedGame,
                                    isActive = uiState.isActiveOrProtecting,
                                    accentColor = palette.accent,
                                    peekAppIcon = peekAppIcon,
                                    loadAppIconAsync = loadAppIconAsync,
                                    onSelectGame = onSelectGame,
                                    onManageGames = onNavigateAddGames
                                )
                                LiveOptimizationMonitoringCard(
                                    metrics = uiState.metrics,
                                    onRunPingTest = onRunPingTest
                                )
                                DeviceCapabilitiesCard(
                                    report = uiState.capabilityReport,
                                    expanded = showCapabilitiesDetail,
                                    onToggleExpand = { showCapabilitiesDetail = !showCapabilitiesDetail }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun EngineStatusAndControlsCard(
    uiState: ExtremeUiState,
    stateColor: Color,
    adReady: Boolean,
    adLoading: Boolean,
    adError: String?,
    adCooldownSeconds: Int,
    onWatchUnlockAd: () -> Unit,
    onActivate: () -> Unit,
    onDeactivate: () -> Unit,
    onToggleAutoStart: (Boolean) -> Unit,
    onViewWarningInfo: () -> Unit,
    onRequestUsageAccess: () -> Unit
) {
    val cardShape = CutCornerShape(topStart = 16.dp, bottomEnd = 16.dp, topEnd = 6.dp, bottomStart = 6.dp)
    val unlock = uiState.unlockState

    Column(
        modifier = Modifier
            .testTag("super_perf_status_card")
            .fillMaxWidth()
            .clip(cardShape)
            .background(MidnightSurface.copy(alpha = 0.94f))
            .border(1.2.dp, stateColor.copy(alpha = 0.72f), cardShape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(stateColor.copy(alpha = 0.16f))
                        .border(1.dp, stateColor.copy(alpha = 0.7f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (unlock.isUnlocked) Icons.Rounded.Bolt else Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = stateColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "ENGINE STATUS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = RajdhaniFontFamily,
                            letterSpacing = 1.sp
                        ),
                        color = TextMuted
                    )
                    Text(
                        text = uiState.modeState.label,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = stateColor,
                        modifier = Modifier.testTag("super_perf_mode_state_label")
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (unlock.isUnlocked) EmeraldBright.copy(alpha = 0.14f) else CyberAmber.copy(alpha = 0.14f))
                    .border(
                        1.dp,
                        if (unlock.isUnlocked) EmeraldBright.copy(alpha = 0.6f) else CyberAmber.copy(alpha = 0.6f),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = if (unlock.isUnlocked) "UNLOCKED (2/2)" else "LOCKED (${unlock.watchedStepsCount}/2 ADS)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (unlock.isUnlocked) EmeraldBright else CyberAmber
                )
            }
        }

        Text(
            text = uiState.lastTransitionReason,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )

        // Two Rewarded Ads Unlock Gate
        if (!unlock.isUnlocked) {
            Column(
                modifier = Modifier
                    .testTag("super_perf_unlock_panel")
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberElevated.copy(alpha = 0.9f))
                    .border(1.dp, CyberAmber.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "UNLOCK VIA 2 REWARDED ADS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = CyberAmber
                    )
                    Text(
                        text = "Step ${unlock.watchedStepsCount}/2",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = TextPrimary
                    )
                }

                LinearProgressIndicator(
                    progress = { unlock.watchedStepsCount / 2f },
                    color = CyberAmber,
                    trackColor = CyberSlate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StepStatusChip(
                        label = "Ad 1",
                        done = unlock.ad1Watched,
                        modifier = Modifier.weight(1f)
                    )
                    StepStatusChip(
                        label = "Ad 2",
                        done = unlock.ad2Watched,
                        modifier = Modifier.weight(1f)
                    )
                }

                val helperMsg = unlock.lastAdMessage
                    ?: adError
                    ?: when {
                        adLoading -> "Loading Google rewarded ad..."
                        adCooldownSeconds > 0 -> "Ad warming up — ready in ${adCooldownSeconds}s"
                        adReady -> "Rewarded ad ready — complete full ad to advance."
                        else -> "Requires 2 full rewarded ad views (persisted across restarts)."
                    }
                Text(
                    text = helperMsg,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Button(
                    onClick = onWatchUnlockAd,
                    enabled = adCooldownSeconds == 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("super_perf_watch_ad_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberAmber,
                        contentColor = ObsidianVoid
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LockOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (!unlock.ad1Watched) "WATCH AD 1 OF 2 TO UNLOCK" else "WATCH AD 2 OF 2 TO UNLOCK",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        // Activation / Deactivation Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!uiState.isActiveOrProtecting) {
                Button(
                    onClick = onActivate,
                    enabled = uiState.canActivate,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("super_perf_activate_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CrimsonTurbo,
                        contentColor = Color.White,
                        disabledContainerColor = CyberSlate,
                        disabledContentColor = TextMuted
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ACTIVATE FOR SELECTED GAME",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Button(
                    onClick = onDeactivate,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("super_perf_deactivate_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CrimsonTurbo,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DEACTIVATE & RESTORE",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                }
            }
        }

        if (!uiState.canActivate && !uiState.isActiveOrProtecting) {
            val blockedReason = when {
                !unlock.isUnlocked -> "Locked: Complete 2 rewarded ads above to enable Super High Performance."
                uiState.selectedGame == null -> "Activation disabled: Select ONE game below first."
                !uiState.selectedGame.isInstalled -> "${uiState.selectedGame.appName} is not currently installed."
                uiState.metrics.thermalSnapshot.protectionLevel == ThermalProtectionLevel.PROTECTED ->
                    "Thermal protection active (${uiState.metrics.thermalSnapshot.batteryTempCelsius}°C). Allow device to cool before activating."
                else -> "Unavailable"
            }
            Text(
                text = blockedReason,
                style = MaterialTheme.typography.labelSmall,
                color = CyberAmber
            )
        }

        // Auto-start + Usage Access strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CyberElevated.copy(alpha = 0.72f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Auto-Activate Selected Game Only",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = TextPrimary
                )
                Text(
                    text = "Starts on launch & restores all settings on exit",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
            Switch(
                checked = uiState.autoStartOnLaunch,
                onCheckedChange = onToggleAutoStart,
                modifier = Modifier.testTag("super_perf_autostart_switch"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = ElectricCyan
                )
            )
        }

        if (!uiState.usageAccessGranted) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberAmber.copy(alpha = 0.12f))
                    .border(1.dp, CyberAmber.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Usage Access Required for Auto-Detect",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = CyberAmber
                    )
                    Text(
                        text = "Detects ONLY when your selected game enters or exits the foreground. No personal data is collected.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onRequestUsageAccess,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("super_perf_grant_usage_btn"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberAmber, contentColor = ObsidianVoid)
                ) {
                    Text(
                        text = "Grant",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        OutlinedButton(
            onClick = onViewWarningInfo,
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("super_perf_warning_info_btn"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "View Warning Info & Essential Safety Guarantees",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = RajdhaniFontFamily,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
private fun StepStatusChip(
    label: String,
    done: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (done) EmeraldBright.copy(alpha = 0.16f) else CyberSlate.copy(alpha = 0.8f))
            .border(
                1.dp,
                if (done) EmeraldBright.copy(alpha = 0.7f) else CyberBorder,
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = if (done) Icons.Rounded.CheckCircle else Icons.Rounded.Lock,
            contentDescription = null,
            tint = if (done) EmeraldBright else TextMuted,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = if (done) "$label Verified" else "$label Pending",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (done) EmeraldBright else TextSecondary
        )
    }
}

@Composable
private fun SelectedGameOnlyCard(
    games: List<SelectedGameInfo>,
    selectedGame: SelectedGameInfo?,
    isActive: Boolean,
    accentColor: Color,
    peekAppIcon: (String) -> ImageBitmap?,
    loadAppIconAsync: suspend (String) -> ImageBitmap?,
    onSelectGame: (String?) -> Unit,
    onManageGames: () -> Unit
) {
    Column(
        modifier = Modifier
            .testTag("super_perf_game_selector_card")
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MidnightSurface.copy(alpha = 0.94f))
            .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SELECTED GAME ONLY (SINGLE TARGET)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = accentColor
                )
                Text(
                    text = selectedGame?.let { "Target: ${it.appName}" } ?: "No game selected",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            }
            OutlinedButton(
                onClick = onManageGames,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("super_perf_manage_games_btn"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Add Games")
            }
        }

        Text(
            text = "Only ONE added game can be selected at a time. Other games stay unaffected, and switching stops any active session first.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )

        if (games.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberElevated)
                    .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
                    .clickable { onManageGames() }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No games in your Turbo X library yet. Tap 'Add Games' to add one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                games.forEach { game ->
                    val iconBitmap by produceState<ImageBitmap?>(
                        initialValue = peekAppIcon(game.packageName),
                        key1 = game.packageName
                    ) {
                        if (value == null) {
                            value = loadAppIconAsync(game.packageName)
                        }
                    }
                    Row(
                        modifier = Modifier
                            .testTag("super_perf_game_row_${game.packageName}")
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (game.isSelected) accentColor.copy(alpha = 0.14f)
                                else CyberElevated.copy(alpha = 0.78f)
                            )
                            .border(
                                width = if (game.isSelected) 1.4.dp else 1.dp,
                                color = if (game.isSelected) accentColor else CyberBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                onSelectGame(if (game.isSelected) null else game.packageName)
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (iconBitmap != null) {
                            Image(
                                bitmap = iconBitmap!!,
                                contentDescription = game.appName,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CyberSlate),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SportsEsports,
                                    contentDescription = game.appName,
                                    tint = accentColor
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = game.appName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = game.packageName,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = if (game.isInstalled) "Installed" else "Not Installed",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (game.isInstalled) EmeraldBright else CrimsonTurbo
                                )
                                Text(
                                    text = if (game.isSelected) {
                                        if (isActive) "• SELECTED & ACTIVE" else "• SELECTED"
                                    } else {
                                        "• Tap to select"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (game.isSelected) accentColor else TextMuted
                                )
                            }
                        }

                        Icon(
                            imageVector = if (game.isSelected) Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked,
                            contentDescription = if (game.isSelected) "Selected" else "Not Selected",
                            tint = if (game.isSelected) accentColor else TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveOptimizationMonitoringCard(
    metrics: LiveExtremeMetrics,
    onRunPingTest: () -> Unit
) {
    val thermalColor = when (metrics.thermalSnapshot.protectionLevel) {
        ThermalProtectionLevel.NORMAL -> EmeraldBright
        ThermalProtectionLevel.REDUCED -> CyberAmber
        ThermalProtectionLevel.PROTECTED -> CrimsonTurbo
    }

    Column(
        modifier = Modifier
            .testTag("super_perf_live_monitor_card")
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MidnightSurface.copy(alpha = 0.94f))
            .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "LIVE OPTIMIZATION MONITORING",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = ElectricCyan
                )
                Text(
                    text = metrics.targetGameName?.let {
                        "Target: $it • ${metrics.aggressiveness.label}"
                    } ?: "Idle — No active game session",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            IconButton(
                onClick = onRunPingTest,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("super_perf_ping_refresh_btn")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Measure real network latency",
                    tint = ElectricCyan
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MonitorMetricTile(
                icon = Icons.Rounded.Speed,
                title = "FPS / Pacing",
                value = "${metrics.fps} FPS",
                subtitle = "${String.format(Locale.US, "%.1f", metrics.frameTimeMs)}ms • ${metrics.refreshRateHz}Hz",
                accent = ElectricCyan,
                modifier = Modifier.weight(1f)
            )
            MonitorMetricTile(
                icon = Icons.Rounded.Memory,
                title = "CPU & ADPF",
                value = "${metrics.cpuFreqMHz} MHz",
                subtitle = "Load ${metrics.cpuUtilizationPercent}% • ADPF ${if (metrics.adpfSessionActive) "ON" else "OFF"}",
                accent = EmeraldBright,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MonitorMetricTile(
                icon = Icons.Rounded.Memory,
                title = "RAM Pressure",
                value = "${metrics.ramUsedPercent}% (${metrics.memoryPressure.name})",
                subtitle = "${metrics.ramAvailableMb} / ${metrics.ramTotalMb} MB Free",
                accent = ElectricCyan,
                modifier = Modifier.weight(1f)
            )
            MonitorMetricTile(
                icon = Icons.Rounded.NetworkCheck,
                title = "Network",
                value = metrics.networkType,
                subtitle = metrics.networkStabilityLabel,
                accent = CyberAmber,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MonitorMetricTile(
                icon = Icons.Rounded.Thermostat,
                title = "Thermal Guard",
                value = "${metrics.thermalSnapshot.batteryTempCelsius}°C • ${metrics.thermalSnapshot.thermalLabel}",
                subtitle = metrics.thermalSnapshot.protectionLevel.label,
                accent = thermalColor,
                modifier = Modifier.weight(1f)
            )
            MonitorMetricTile(
                icon = if (metrics.thermalSnapshot.isCharging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryStd,
                title = "Battery & Service",
                value = "${metrics.thermalSnapshot.batteryPercent}%",
                subtitle = "Svc: ${if (metrics.foregroundServiceRunning) "ON" else "OFF"} • Lock: ${if (metrics.wakeLockHeld) "HELD" else "FREE"}",
                accent = EmeraldBright,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(CyberElevated.copy(alpha = 0.85f))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "GPU: ${metrics.gpuPipelineLabel}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (metrics.turboWorkloadsThrottled) "Turbo BG Throttled" else "Normal Workloads",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (metrics.turboWorkloadsThrottled) ElectricCyan else TextMuted
            )
        }
    }
}

@Composable
private fun MonitorMetricTile(
    icon: ImageVector,
    title: String,
    value: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CyberElevated.copy(alpha = 0.85f))
            .border(1.dp, CyberBorder.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                maxLines = 1
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = RajdhaniFontFamily,
                fontWeight = FontWeight.Bold
            ),
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DeviceCapabilitiesCard(
    report: DeviceCapabilityReport,
    expanded: Boolean,
    onToggleExpand: () -> Unit
) {
    Column(
        modifier = Modifier
            .testTag("super_perf_capabilities_card")
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MidnightSurface.copy(alpha = 0.94f))
            .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .clickable { onToggleExpand() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.Security, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                Column {
                    Text(
                        text = "DEVICE CAPABILITIES & ANDROID LIMITS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = TextPrimary
                    )
                    Text(
                        text = "Android API ${report.androidApiLevel} • Max Panel ${report.maxRefreshRateHz}Hz",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = "Toggle capability details",
                tint = TextSecondary
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CapabilityCategory.entries.forEach { category ->
                    val matching = report.items.filter { it.category == category }
                    if (matching.isNotEmpty()) {
                        val badgeColor = when (category) {
                            CapabilityCategory.FULLY_WORKING -> EmeraldBright
                            CapabilityCategory.PERMISSION_REQUIRED -> ElectricCyan
                            CapabilityCategory.OEM_DEPENDENT -> CyberAmber
                            CapabilityCategory.ANDROID_RESTRICTED -> TextMuted
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyberElevated.copy(alpha = 0.72f))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = category.title.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = RajdhaniFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = badgeColor
                            )
                            matching.forEach { item ->
                                Text(
                                    text = "• ${item.feature}: ${item.detail}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
