package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BACKGROUND_LIMIT_LABELS
import com.example.data.CACHE_LIMIT_LABELS
import com.example.data.CpuMode
import com.example.data.CpuUnitFormat
import com.example.data.GameTurboSettings
import com.example.data.HardwareTelemetry
import com.example.data.PerformanceEngineStatus
import com.example.data.PermissionStatus
import com.example.data.WallpaperTheme
import com.example.security.SecurityAuditReport
import com.example.ui.components.HolographicGaugeRing
import com.example.ui.components.TurboTopLogoCorner
import com.example.ui.components.cpuModeColor
import com.example.ui.components.rememberTurboFlingBehavior
import kotlinx.coroutines.delay
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberElevated
import com.example.ui.theme.CyberSlate
import com.example.ui.theme.LocalTurboAccent
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

private enum class SettingsCategory(val title: String) {
    DISTURBANCE("Disturbance Settings"),
    PROTECTION("Setting up protection"),
    CPU("Setting CPU"),
    PERFORMANCE("Performance"),
    RAM("Setting Ram"),
    WALLPAPER("Wallpaper Settings"),
    CONTROL_PANEL("Control Panel Setting"),
    GAME_PANEL("Game panel settings"),
    OTHER("Other Options"),
    ABOUT("About this app")
}

@Composable
fun SettingsScreen(
    settings: GameTurboSettings,
    telemetry: HardwareTelemetry,
    performanceStatus: PerformanceEngineStatus,
    permissions: PermissionStatus,
    securityAudit: SecurityAuditReport,
    statusMessage: String?,
    performanceActions: PerformanceActions,
    onUpdateSettings: ((GameTurboSettings) -> GameTurboSettings) -> Unit,
    onBoostRam: () -> Unit,
    onClearCache: () -> Unit,
    onSelectWallpaperTheme: (WallpaperTheme, String?) -> Unit,
    onOpenPermissions: () -> Unit,
    onDismissStatus: () -> Unit,
    onBack: () -> Unit
) {
    val accent = LocalTurboAccent.current.accent
    val turboFling = rememberTurboFlingBehavior()
    var selectedCategory by remember { mutableStateOf(SettingsCategory.DISTURBANCE) }

    LaunchedEffect(statusMessage) {
        if (statusMessage != null) {
            delay(2800L)
            onDismissStatus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid)
            .systemBarsPadding()
    ) {
        // Header Bar matching Screenshots 1, 2, 4, 6..15
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MidnightSurface)
                .padding(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TurboTopLogoCorner()
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("settings_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Text(
                    text = "Setting",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }

            if (statusMessage != null) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    maxLines = 2,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val isWide = maxWidth >= 600.dp

            if (isWide) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left Category Rail
                    Column(
                        modifier = Modifier
                            .width(228.dp)
                            .fillMaxHeight()
                            .background(Color(0xFF151824))
                            .verticalScroll(rememberScrollState())
                    ) {
                        SettingsCategory.entries.forEach { category ->
                            val isSelected = category == selectedCategory
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_cat_${category.name}")
                                    .background(
                                        if (isSelected) {
                                            Brush.horizontalGradient(
                                                listOf(Color(0xFF282E42), accent.copy(alpha = 0.38f))
                                            )
                                        } else {
                                            Brush.horizontalGradient(
                                                listOf(Color.Transparent, Color.Transparent)
                                            )
                                        }
                                    )
                                    .clickable { selectedCategory = category }
                                    .height(54.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = category.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 16.dp)
                                )
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .fillMaxHeight()
                                            .background(accent)
                                    )
                                }
                            }
                        }
                    }

                    // Right Detail Pane
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(ObsidianVoid)
                            .verticalScroll(rememberScrollState(), flingBehavior = turboFling)
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        SettingsCategoryDetail(
                            category = selectedCategory,
                            settings = settings,
                            telemetry = telemetry,
                            performanceStatus = performanceStatus,
                            permissions = permissions,
                            securityAudit = securityAudit,
                            performanceActions = performanceActions,
                            onUpdateSettings = onUpdateSettings,
                            onBoostRam = onBoostRam,
                            onClearCache = onClearCache,
                            onSelectWallpaperTheme = onSelectWallpaperTheme,
                            onOpenPermissions = onOpenPermissions
                        )
                    }
                }
            } else {
                // Portrait Adaptive Layout: Horizontal category tabs + scrollable detail pane
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF151824))
                            .horizontalScroll(rememberScrollState(), flingBehavior = turboFling)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SettingsCategory.entries.forEach { category ->
                            val isSelected = category == selectedCategory
                            Box(
                                modifier = Modifier
                                    .testTag("settings_cat_${category.name}")
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) accent else CyberSlate)
                                    .clickable { selectedCategory = category }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = category.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState(), flingBehavior = turboFling)
                            .padding(16.dp)
                    ) {
                        SettingsCategoryDetail(
                            category = selectedCategory,
                            settings = settings,
                            telemetry = telemetry,
                            performanceStatus = performanceStatus,
                            permissions = permissions,
                            securityAudit = securityAudit,
                            performanceActions = performanceActions,
                            onUpdateSettings = onUpdateSettings,
                            onBoostRam = onBoostRam,
                            onClearCache = onClearCache,
                            onSelectWallpaperTheme = onSelectWallpaperTheme,
                            onOpenPermissions = onOpenPermissions
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCategoryDetail(
    category: SettingsCategory,
    settings: GameTurboSettings,
    telemetry: HardwareTelemetry,
    performanceStatus: PerformanceEngineStatus,
    permissions: PermissionStatus,
    securityAudit: SecurityAuditReport,
    performanceActions: PerformanceActions,
    onUpdateSettings: ((GameTurboSettings) -> GameTurboSettings) -> Unit,
    onBoostRam: () -> Unit,
    onClearCache: () -> Unit,
    onSelectWallpaperTheme: (WallpaperTheme, String?) -> Unit,
    onOpenPermissions: () -> Unit
) {
    when (category) {
        SettingsCategory.DISTURBANCE -> {
            Column {
                SettingToggleRow(
                    title = "Do Not Disturb mode will be enabled as soon as you enter Game Mode.",
                    subtitle = "Turn on Do Not Disturb mode when entering the game",
                    checked = settings.autoDndOnEnter,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(autoDndOnEnter = v) } },
                    testTag = "toggle_auto_dnd"
                )
                SettingToggleRow(
                    title = "Turn off notifications while playing games",
                    subtitle = "Notifications will be disabled while playing the game.",
                    checked = settings.muteNotificationsInGame,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(muteNotificationsInGame = v) } },
                    testTag = "toggle_mute_notif"
                )
            }
        }

        SettingsCategory.PROTECTION -> {
            Column {
                SettingToggleRow(
                    title = "Prevent memory leaks",
                    subtitle = "Prevent memory leaks when using GameMode and unnecessary background processes.",
                    checked = settings.preventMemoryLeaks,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(preventMemoryLeaks = v) } }
                )
                SettingToggleRow(
                    title = "Disable three-finger screenshot function",
                    subtitle = "Disable three-finger screenshot function for devices with three-finger screenshot gesture.",
                    checked = settings.disableThreeFingerScreenshot,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(disableThreeFingerScreenshot = v) } }
                )
            }
        }

        SettingsCategory.CPU -> {
            CpuSettingsSection(
                settings = settings,
                telemetry = telemetry,
                onUpdateSettings = onUpdateSettings
            )
        }

        SettingsCategory.PERFORMANCE -> {
            PerformanceSettingsSection(
                settings = settings,
                telemetry = telemetry,
                engineStatus = performanceStatus,
                permissions = permissions,
                actions = performanceActions
            )
        }

        SettingsCategory.RAM -> {
            RamSettingsSection(
                settings = settings,
                telemetry = telemetry,
                onUpdateSettings = onUpdateSettings,
                onBoostRam = onBoostRam,
                onClearCache = onClearCache
            )
        }

        SettingsCategory.WALLPAPER -> {
            WallpaperSettingsSection(
                settings = settings,
                onSelectWallpaperTheme = onSelectWallpaperTheme
            )
        }

        SettingsCategory.CONTROL_PANEL -> {
            ControlPanelSettingSection(
                selectedLayout = settings.controlPanelLayout,
                onSelectLayout = { layout -> onUpdateSettings { it.copy(controlPanelLayout = layout) } }
            )
        }

        SettingsCategory.GAME_PANEL -> {
            GamePanelStyleSection(
                selectedStyle = settings.gamePanelStyle,
                onSelectStyle = { style -> onUpdateSettings { it.copy(gamePanelStyle = style) } }
            )
        }

        SettingsCategory.OTHER -> {
            Column {
                SettingToggleRow(
                    title = "Notification mute mode",
                    checked = settings.notificationMuteMode,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(notificationMuteMode = v) } }
                )
                SettingToggleRow(
                    title = "Do Not Disturb Mode",
                    checked = settings.dndMode,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(dndMode = v) } }
                )
                SettingToggleRow(
                    title = "Lock brightness",
                    checked = settings.lockBrightness,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(lockBrightness = v) } }
                )
                SettingToggleRow(
                    title = "Rotation lock",
                    checked = settings.rotationLock,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(rotationLock = v) } }
                )
                SettingToggleRow(
                    title = "GameMode opening animation",
                    subtitle = "When enabled, the game mode's on/off animation will be enabled.",
                    checked = settings.openingAnimation,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(openingAnimation = v) } }
                )
                SettingToggleRow(
                    title = "GameMode Home Animation",
                    subtitle = "Open all animations in the app including the home page.",
                    checked = settings.homeAnimation,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(homeAnimation = v) } }
                )
                SettingToggleRow(
                    title = "RAM and CPU-saving mode for UI rendering: Game Mode",
                    subtitle = "Enabling this will limit certain animations in the Game Mode Overlay to reduce latency.",
                    checked = settings.lowRamOverlayMode,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(lowRamOverlayMode = v) } }
                )
            }
        }

        SettingsCategory.ABOUT -> {
            AboutAppSection(
                telemetry = telemetry,
                securityAudit = securityAudit,
                onOpenPermissions = onOpenPermissions
            )
        }
    }
}

@Composable
private fun CpuSettingsSection(
    settings: GameTurboSettings,
    telemetry: HardwareTelemetry,
    onUpdateSettings: ((GameTurboSettings) -> GameTurboSettings) -> Unit
) {
    val accent = LocalTurboAccent.current.accent
    val ringColor = cpuModeColor(settings.cpuMode, accent)
    val ghzText = String.format(Locale.US, "%.2f", telemetry.cpuFreqMHz / 1000f)
    val mhzText = "${telemetry.cpuFreqMHz}"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Select Format", style = MaterialTheme.typography.titleMedium, color = TextPrimary)

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CpuFormatCard(
                valueText = ghzText,
                unitText = "GHz",
                ringColor = ringColor,
                selected = settings.cpuUnitFormat == CpuUnitFormat.GHZ,
                accentColor = accent,
                onClick = { onUpdateSettings { it.copy(cpuUnitFormat = CpuUnitFormat.GHZ) } },
                testTag = "cpu_format_ghz"
            )
            CpuFormatCard(
                valueText = mhzText,
                unitText = "MHz",
                ringColor = ringColor,
                selected = settings.cpuUnitFormat == CpuUnitFormat.MHZ,
                accentColor = accent,
                onClick = { onUpdateSettings { it.copy(cpuUnitFormat = CpuUnitFormat.MHZ) } },
                testTag = "cpu_format_mhz"
            )
        }

        Text(
            text = settings.cpuMode.description,
            style = MaterialTheme.typography.bodyLarge,
            color = ringColor,
            textAlign = TextAlign.Center
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            CpuMode.entries.forEach { mode ->
                val isSelected = settings.cpuMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("cpu_mode_${mode.name}")
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) accent else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) accent else CyberBorder,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onUpdateSettings { it.copy(cpuMode = mode) } }
                        .padding(horizontal = 8.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) Color.White else TextPrimary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun CpuFormatCard(
    valueText: String,
    unitText: String,
    ringColor: Color,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .testTag(testTag)
            .width(172.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) accentColor else Color.White.copy(alpha = 0.7f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        HolographicGaugeRing(
            valueText = valueText,
            unitText = unitText,
            bottomLabel = "CPU",
            ringColor = ringColor
        )
    }
}

@Composable
private fun RamSettingsSection(
    settings: GameTurboSettings,
    telemetry: HardwareTelemetry,
    onUpdateSettings: ((GameTurboSettings) -> GameTurboSettings) -> Unit,
    onBoostRam: () -> Unit,
    onClearCache: () -> Unit
) {
    val accent = LocalTurboAccent.current.accent

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Customization",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Text(
            text = "Live Memory: ${telemetry.ramAvailableMb} MB free of ${telemetry.ramTotalMb} MB (${telemetry.ramUsedPercent}% used)",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        HorizontalDivider(color = CyberBorder.copy(alpha = 0.5f))

        DiscreteSliderRow(
            title = "Background limit: ${BACKGROUND_LIMIT_LABELS[settings.backgroundLimitIndex.coerceIn(0, 4)]}",
            labels = listOf("Def", "0", "1", "2", "3"),
            selectedIndex = settings.backgroundLimitIndex.coerceIn(0, 4),
            description = "Target background process retention profile during Game Mode.",
            accentColor = accent,
            onIndexChange = { idx -> onUpdateSettings { it.copy(backgroundLimitIndex = idx) } },
            onHelpClick = {}
        )

        DiscreteSliderRow(
            title = "Cache limit: ${CACHE_LIMIT_LABELS[settings.cacheLimitIndex.coerceIn(0, 2)]}",
            labels = CACHE_LIMIT_LABELS,
            selectedIndex = settings.cacheLimitIndex.coerceIn(0, 2),
            description = "Controls how aggressively temporary app caches are trimmed.",
            accentColor = accent,
            onIndexChange = { idx -> onUpdateSettings { it.copy(cacheLimitIndex = idx) } },
            onHelpClick = {}
        )

        Button(
            onClick = onBoostRam,
            colors = ButtonDefaults.buttonColors(containerColor = CyberElevated),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("btn_force_memory_clear")
        ) {
            Text(
                text = "FORCE AUTOMATIC MEMORY CLEARING",
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary
            )
        }
        Text(
            text = "Clear phone memory. If your phone has 6-24 GB memory, you don't need to enable it continuously.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
        )

        Button(
            onClick = onClearCache,
            colors = ButtonDefaults.buttonColors(containerColor = CyberElevated),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("btn_clear_cache")
        ) {
            Text(
                text = "CLEAR CACHE OF ALL APPS",
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary
            )
        }
        Text(
            text = "Clear temporary app caches, which helps your phone run smoother.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
        )
    }
}

@Composable
private fun WallpaperSettingsSection(
    settings: GameTurboSettings,
    onSelectWallpaperTheme: (WallpaperTheme, String?) -> Unit
) {
    val accent = LocalTurboAccent.current.accent
    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            onSelectWallpaperTheme(WallpaperTheme.CUSTOM, uri.toString())
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Select Launcher Theme & Background", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        WallpaperTheme.entries.forEach { theme ->
            val selected = settings.wallpaperTheme == theme
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) accent.copy(alpha = 0.18f) else CyberSlate)
                    .border(1.dp, if (selected) accent else CyberBorder, RoundedCornerShape(12.dp))
                    .clickable {
                        if (theme == WallpaperTheme.CUSTOM) {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        } else {
                            onSelectWallpaperTheme(theme, null)
                        }
                    }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(theme.label, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(
                    text = if (selected) "ACTIVE" else if (theme == WallpaperTheme.CUSTOM) "PICK PHOTO" else "SELECT",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) accent else TextSecondary
                )
            }
        }
    }
}

@Composable
private fun AboutAppSection(
    telemetry: HardwareTelemetry,
    securityAudit: SecurityAuditReport,
    onOpenPermissions: () -> Unit
) {
    val accent = LocalTurboAccent.current.accent
    val shortSig = securityAudit.signatureSha256.take(12).ifBlank { "Verified" }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Turbo X",
            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = RajdhaniFontFamily),
            color = accent
        )
        Text("Version 2.0 • Cyber HUD & Game Launcher", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
        HorizontalDivider(color = CyberBorder.copy(alpha = 0.5f))
        InfoRow("Device Model", telemetry.deviceModel)
        InfoRow("Android Version", "Android ${telemetry.androidVersion}")
        InfoRow("Processor Cores", "${telemetry.cpuCoreCount} Cores (${telemetry.cpuFreqMHz} MHz)")
        InfoRow("System Memory", "${telemetry.ramTotalMb} MB Total (${telemetry.ramAvailableMb} MB Free)")
        InfoRow("Display Refresh Rate", "${telemetry.displayRefreshRateHz} Hz")
        InfoRow("Graphics Pipeline", telemetry.gpuRenderer)

        Spacer(modifier = Modifier.height(6.dp))
        HorizontalDivider(color = CyberBorder.copy(alpha = 0.5f))
        Text(
            text = "SECURITY SHIELD & INTEGRITY",
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = RajdhaniFontFamily),
            color = if (securityAudit.isSafeForGameplay) Color(0xFF00E676) else accent
        )
        InfoRow(
            "APK & Signature Integrity",
            if (securityAudit.packageIntegrityOk && !securityAudit.repackagedDebuggable) "Verified ($shortSig…)" else "Tamper Alert"
        )
        InfoRow(
            "Anti-Debug & Anti-Hook",
            if (!securityAudit.debuggerDetected && !securityAudit.hookingDetected) "Protected (No Frida/Xposed)" else "Hook / Debug Detected"
        )
        InfoRow(
            "Credit Vault & Anti-Rollback",
            if (securityAudit.clockTamperDetected) "Clock Tamper Blocked" else if (securityAudit.vaultKeystoreBacked) "Keystore AES-256-GCM" else "AES-256-GCM Sealed"
        )
        InfoRow("Network & Backup Policy", "HTTPS-Only TLS • Backup Disabled")

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onOpenPermissions,
            colors = ButtonDefaults.buttonColors(containerColor = accent)
        ) {
            Text("Manage System Permissions")
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Medium)
    }
}
