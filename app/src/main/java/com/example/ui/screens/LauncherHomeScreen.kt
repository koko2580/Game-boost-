package com.example.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.GameSessionRecord
import com.example.data.GameTurboSettings
import com.example.data.HardwareTelemetry
import com.example.data.InstalledAppInfo
import com.example.data.PermissionStatus
import com.example.data.WallpaperTheme
import com.example.ads.AdMobBannerBar
import com.example.ui.components.SlantedTabShape
import com.example.ui.components.TurboXEmblem
import com.example.ui.components.cpuModeColor
import com.example.ui.components.formatCompactTurboClock
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
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LauncherHomeScreen(
    settings: GameTurboSettings,
    telemetry: HardwareTelemetry,
    permissions: PermissionStatus,
    addedGames: List<InstalledAppInfo>,
    sessionHistory: List<GameSessionRecord>,
    remainingTurboSeconds: Long,
    statusMessage: String?,
    isOverlayServiceRunning: Boolean,
    getAppIcon: (String) -> ImageBitmap?,
    onSelectGame: (String) -> Unit,
    onStartPlay: () -> Unit,
    onAddTurboTimeClick: () -> Unit,
    onOpenOverlayPreview: () -> Unit,
    onToggleOverlayService: () -> Unit,
    onNavigateAddApps: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigatePermissions: () -> Unit,
    onBoostRam: () -> Unit,
    onRunPingTest: () -> Unit,
    onSelectWallpaperTheme: (WallpaperTheme, String?) -> Unit,
    onDismissStatus: () -> Unit
) {
    val palette = LocalTurboAccent.current
    val modeGlowColor = cpuModeColor(settings.cpuMode, palette.accent)
    var selectedBottomTab by remember { mutableIntStateOf(0) } // 0 = Lobby, 1 = DataBase Page
    var showWallpaperDialog by remember { mutableStateOf(false) }

    var currentClock by remember { mutableStateOf(formatClock()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentClock = formatClock()
            delay(15_000L)
        }
    }

    LaunchedEffect(statusMessage) {
        if (statusMessage != null) {
            delay(2800L)
            onDismissStatus()
        }
    }

    val selectedGame = addedGames.find { it.packageName == settings.selectedGamePackage }
        ?: addedGames.firstOrNull()

    val context = LocalContext.current
    val customWallpaperBitmap = remember(settings.customWallpaperUri, settings.wallpaperTheme) {
        if (settings.wallpaperTheme == WallpaperTheme.CUSTOM && settings.customWallpaperUri != null) {
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(settings.customWallpaperUri))?.use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            }.getOrNull()
        } else null
    }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ObsidianVoid, Color(0xFF0E121E), ObsidianVoid)
                )
            )
    ) {
        if (customWallpaperBitmap != null) {
            Image(
                bitmap = customWallpaperBitmap,
                contentDescription = "Custom launcher background",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.32f)
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            val isWide = maxWidth >= 600.dp

            Column(modifier = Modifier.fillMaxSize()) {
                // TOP HEADER BAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Brand + Status telemetry
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(color = TextPrimary, fontWeight = FontWeight.Bold)) {
                                    append("Launcher ")
                                }
                                withStyle(SpanStyle(color = palette.accent, fontWeight = FontWeight.Bold)) {
                                    append("Game")
                                }
                            },
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = RajdhaniFontFamily,
                                fontSize = if (isWide) 26.sp else 22.sp
                            )
                        )

                        if (isWide) {
                            HeaderStatusCluster(
                                clock = currentClock,
                                telemetry = telemetry,
                                remainingTurboSeconds = remainingTurboSeconds,
                                accentColor = palette.accent,
                                onAddTimeClick = onAddTurboTimeClick
                            )
                        }
                    }

                    // Top Right Circular Action Pills
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HeaderCircleButton(
                            icon = Icons.Rounded.SportsEsports,
                            description = "Open Game Control Panel",
                            badgeDotColor = if (isOverlayServiceRunning) EmeraldActive else null,
                            onClick = onOpenOverlayPreview,
                            testTag = "open_overlay_preview_btn"
                        )
                        val allPermsOk = permissions.dndGranted && permissions.overlayGranted && permissions.writeSettingsGranted
                        HeaderCircleButton(
                            icon = Icons.Rounded.Security,
                            description = "Permissions",
                            badgeDotColor = if (allPermsOk) EmeraldActive else CyberAmber,
                            onClick = onNavigatePermissions,
                            testTag = "permissions_button"
                        )
                        HeaderCircleButton(
                            icon = Icons.Rounded.Brush,
                            description = "Wallpaper and Theme",
                            onClick = { showWallpaperDialog = true },
                            testTag = "wallpaper_button"
                        )
                        HeaderCircleButton(
                            icon = Icons.Rounded.Settings,
                            description = "Settings",
                            onClick = onNavigateSettings,
                            testTag = "settings_button"
                        )
                    }
                }

                // Compact Status Strip for Portrait Phones
                if (!isWide) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HeaderStatusCluster(
                            clock = currentClock,
                            telemetry = telemetry,
                            remainingTurboSeconds = remainingTurboSeconds,
                            accentColor = palette.accent,
                            onAddTimeClick = onAddTurboTimeClick
                        )
                    }
                }

                // MAIN BODY AREA
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (selectedBottomTab == 0) {
                        // LOBBY TAB
                        if (isWide) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left Game Dock
                                Column(
                                    modifier = Modifier
                                        .width(235.dp)
                                        .fillMaxHeight(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    GameDockList(
                                        games = addedGames,
                                        selectedPackage = selectedGame?.packageName,
                                        accentColor = palette.accent,
                                        getAppIcon = getAppIcon,
                                        onSelectGame = onSelectGame,
                                        onAddGames = onNavigateAddApps,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    AddGameSquareButton(onClick = onNavigateAddApps)
                                }

                                // Center Metallic X Hero + Right Start Play
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    TurboXEmblem(
                                        glowColor = modeGlowColor,
                                        animatePulse = settings.homeAnimation,
                                        modifier = Modifier.size(240.dp)
                                    )

                                    Column(
                                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = selectedGame?.appName ?: "Tap + to Add Your Games",
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontFamily = RajdhaniFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 26.sp
                                            ),
                                            color = TextPrimary.copy(alpha = 0.88f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${settings.cpuMode.label}  •  ${telemetry.cpuFreqMHz} MHz  •  RAM ${telemetry.ramUsedPercent}%",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary
                                        )
                                    }

                                    StartPlayButtonCluster(
                                        accentColor = palette.accent,
                                        accentDeep = palette.accentDeep,
                                        remainingTurboSeconds = remainingTurboSeconds,
                                        onStartPlay = onStartPlay,
                                        onQuickPanel = onOpenOverlayPreview,
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .padding(end = 12.dp)
                                    )
                                }
                            }
                        } else {
                            // Portrait Adaptive Lobby
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Hero X + Selected Game
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    TurboXEmblem(
                                        glowColor = modeGlowColor,
                                        animatePulse = settings.homeAnimation,
                                        modifier = Modifier.size(210.dp)
                                    )
                                    Column(
                                        modifier = Modifier.align(Alignment.BottomCenter),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = selectedGame?.appName ?: "No Games Added Yet",
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontFamily = RajdhaniFontFamily,
                                                fontSize = 24.sp
                                            ),
                                            color = TextPrimary,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${settings.cpuMode.label} • ${telemetry.cpuFreqMHz} MHz • ${telemetry.ramUsedPercent}% RAM",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                StartPlayButtonCluster(
                                    accentColor = palette.accent,
                                    accentDeep = palette.accentDeep,
                                    remainingTurboSeconds = remainingTurboSeconds,
                                    onStartPlay = onStartPlay,
                                    onQuickPanel = onOpenOverlayPreview
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Horizontal Game Dock Strip
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    AddGameSquareButton(onClick = onNavigateAddApps)
                                    if (addedGames.isEmpty()) {
                                        Text(
                                            text = "Add games from your installed apps",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextMuted
                                        )
                                    } else {
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            items(addedGames, key = { it.packageName }) { game ->
                                                PortraitGameChip(
                                                    game = game,
                                                    selected = game.packageName == selectedGame?.packageName,
                                                    accentColor = palette.accent,
                                                    icon = getAppIcon(game.packageName),
                                                    onClick = { onSelectGame(game.packageName) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // DATABASE PAGE TAB — Live Hardware & Session Telemetry
                        DataBasePageContent(
                            settings = settings,
                            telemetry = telemetry,
                            sessionHistory = sessionHistory,
                            remainingTurboSeconds = remainingTurboSeconds,
                            isOverlayRunning = isOverlayServiceRunning,
                            onAddTurboTime = onAddTurboTimeClick,
                            onBoostRam = onBoostRam,
                            onRunPingTest = onRunPingTest,
                            onToggleOverlayService = onToggleOverlayService
                        )
                    }

                    // Floating Toast Status Banner
                    androidx.compose.animation.AnimatedVisibility(
                        visible = statusMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 6.dp)
                    ) {
                        if (statusMessage != null) {
                            Surface(
                                color = CyberElevated.copy(alpha = 0.96f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.border(1.dp, palette.accent, RoundedCornerShape(14.dp))
                            ) {
                                Text(
                                    text = statusMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                // REAL ADMOB BANNER AD
                AdMobBannerBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                )

                // BOTTOM SLANTED TABS: Lobby | DataBase Page
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    SlantedBottomTab(
                        title = "Lobby",
                        selected = selectedBottomTab == 0,
                        accentColor = palette.accent,
                        onClick = { selectedBottomTab = 0 },
                        testTag = "tab_lobby"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    SlantedBottomTab(
                        title = "DataBase Page",
                        selected = selectedBottomTab == 1,
                        accentColor = palette.accent,
                        onClick = { selectedBottomTab = 1 },
                        testTag = "tab_database"
                    )
                }
            }
        }
    }

    if (showWallpaperDialog) {
        WallpaperThemeDialog(
            currentTheme = settings.wallpaperTheme,
            onSelectTheme = { theme ->
                if (theme == WallpaperTheme.CUSTOM) {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                } else {
                    onSelectWallpaperTheme(theme, null)
                }
                showWallpaperDialog = false
            },
            onDismiss = { showWallpaperDialog = false }
        )
    }
}

@Composable
private fun HeaderStatusCluster(
    clock: String,
    telemetry: HardwareTelemetry,
    remainingTurboSeconds: Long,
    accentColor: Color,
    onAddTimeClick: () -> Unit
) {
    val hasTime = remainingTurboSeconds > 0L
    val timeColor = if (hasTime) EmeraldBright else CyberAmber
    val formattedRemaining = formatCompactTurboClock(remainingTurboSeconds)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = clock,
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = RajdhaniFontFamily),
            color = TextSecondary
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Icon(
                imageVector = if (telemetry.isCharging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryStd,
                contentDescription = "Battery level",
                tint = EmeraldActive,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "${telemetry.batteryPercent}%",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .testTag("turbo_time_badge")
                .clip(RoundedCornerShape(10.dp))
                .background(if (hasTime) EmeraldActive.copy(alpha = 0.14f) else accentColor.copy(alpha = 0.14f))
                .border(
                    1.dp,
                    if (hasTime) EmeraldActive.copy(alpha = 0.5f) else CyberAmber.copy(alpha = 0.5f),
                    RoundedCornerShape(10.dp)
                )
                .clickable { onAddTimeClick() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Bolt,
                contentDescription = "Remaining Turbo Time",
                tint = timeColor,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = formattedRemaining,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = RajdhaniFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = timeColor
            )
        }
        Box(
            modifier = Modifier
                .testTag("header_add_time_btn")
                .size(48.dp)
                .clip(CircleShape)
                .clickable { onAddTimeClick() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(CyberElevated)
                    .border(1.dp, if (hasTime) CyberBorder else CyberAmber, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = "Watch ad to add 1 hour Turbo Time",
                    tint = if (hasTime) TextPrimary else CyberAmber,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun HeaderCircleButton(
    icon: ImageVector,
    description: String,
    badgeDotColor: Color? = null,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .testTag(testTag)
            .size(40.dp)
            .clip(CircleShape)
            .background(CyberSlate)
            .border(1.dp, CyberBorder, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = TextPrimary, modifier = Modifier.size(20.dp))
        if (badgeDotColor != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(badgeDotColor)
            )
        }
    }
}

@Composable
private fun GameDockList(
    games: List<InstalledAppInfo>,
    selectedPackage: String?,
    accentColor: Color,
    getAppIcon: (String) -> ImageBitmap?,
    onSelectGame: (String) -> Unit,
    onAddGames: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (games.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MidnightSurface.copy(alpha = 0.85f))
                .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
                .clickable { onAddGames() }
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.SportsEsports, contentDescription = null, tint = accentColor, modifier = Modifier.size(34.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No games added",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap '+' below to add installed games",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(games, key = { it.packageName }) { game ->
                val isSelected = game.packageName == selectedPackage
                val iconBitmap = remember(game.packageName) { getAppIcon(game.packageName) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) accentColor.copy(alpha = 0.14f) else MidnightSurface.copy(alpha = 0.72f)
                        )
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) accentColor else CyberBorder.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectGame(game.packageName) }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (iconBitmap != null) {
                        Image(
                            bitmap = iconBitmap,
                            contentDescription = game.appName,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyberElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.SportsEsports, contentDescription = null, tint = TextPrimary)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Version ${game.versionName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = game.appName,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PortraitGameChip(
    game: InstalledAppInfo,
    selected: Boolean,
    accentColor: Color,
    icon: ImageBitmap?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) accentColor.copy(alpha = 0.2f) else CyberSlate)
            .border(1.dp, if (selected) accentColor else CyberBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = game.appName,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        }
        Text(
            text = game.appName,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            maxLines = 1
        )
    }
}

@Composable
private fun AddGameSquareButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .testTag("add_games_button")
            .size(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(CyberSlate)
            .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.Add, contentDescription = "Add Apps", tint = TextPrimary, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun StartPlayButtonCluster(
    accentColor: Color,
    accentDeep: Color,
    remainingTurboSeconds: Long,
    onStartPlay: () -> Unit,
    onQuickPanel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasTime = remainingTurboSeconds > 0L
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .testTag("start_play_button")
                    .height(48.dp)
                    .width(168.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = if (hasTime) {
                                listOf(accentColor, accentDeep)
                            } else {
                                listOf(CyberElevated, CyberSlate)
                            }
                        )
                    )
                    .border(
                        1.dp,
                        if (hasTime) accentColor.copy(alpha = 0.85f) else CyberAmber.copy(alpha = 0.75f),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onStartPlay() },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!hasTime) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = CyberAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = if (hasTime) "Start Play" else "Watch Ad to Play",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = if (hasTime) Color.White else CyberAmber
                    )
                }
            }

            Box(
                modifier = Modifier
                    .testTag("quick_panel_arrow_btn")
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ObsidianVoid.copy(alpha = 0.9f))
                    .border(1.5.dp, accentColor, RoundedCornerShape(10.dp))
                    .clickable { onQuickPanel() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "Open Game Panel",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Text(
            text = if (hasTime) {
                "Turbo Time Active • ${formatCompactTurboClock(remainingTurboSeconds)}"
            } else {
                "0:00:00 left • Watch 1 Ad for +1h:00m"
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (hasTime) EmeraldBright else CyberAmber
        )
    }
}

@Composable
private fun SlantedBottomTab(
    title: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .testTag(testTag)
            .width(172.dp)
            .height(38.dp)
            .clip(SlantedTabShape)
            .background(
                if (selected) {
                    Brush.verticalGradient(listOf(accentColor.copy(alpha = 0.35f), MidnightSurface))
                } else {
                    Brush.verticalGradient(listOf(CyberSlate.copy(alpha = 0.7f), MidnightSurface))
                }
            )
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) accentColor else CyberBorder,
                shape = SlantedTabShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = RajdhaniFontFamily,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (selected) TextPrimary else TextSecondary
        )
    }
}

@Composable
private fun DataBasePageContent(
    settings: GameTurboSettings,
    telemetry: HardwareTelemetry,
    sessionHistory: List<GameSessionRecord>,
    remainingTurboSeconds: Long,
    isOverlayRunning: Boolean,
    onAddTurboTime: () -> Unit,
    onBoostRam: () -> Unit,
    onRunPingTest: () -> Unit,
    onToggleOverlayService: () -> Unit
) {
    val palette = LocalTurboAccent.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top 3 Hardware Telemetry Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TelemetrySummaryCard(
                title = "CPU & Live FPS",
                value = "${telemetry.cpuFreqMHz} MHz",
                subtitle = "${telemetry.actualFps} FPS (${String.format(Locale.US, "%.1f", telemetry.frameTimeMs)}ms) • ${telemetry.cpuCoreCount} Cores",
                icon = Icons.Rounded.Speed,
                accent = palette.accent,
                modifier = Modifier.weight(1f)
            )
            TelemetrySummaryCard(
                title = "Memory (RAM)",
                value = "${telemetry.ramUsedPercent}%",
                subtitle = "${telemetry.ramAvailableMb} / ${telemetry.ramTotalMb} MB Free",
                icon = Icons.Rounded.Memory,
                accent = ElectricCyan,
                modifier = Modifier.weight(1f)
            )
            TelemetrySummaryCard(
                title = "Thermal & Network",
                value = "${telemetry.batteryTempCelsius}°C",
                subtitle = "${telemetry.displayRefreshRateHz}Hz • ${telemetry.networkType}" +
                    (telemetry.wifiLinkMbps?.let { " ${it}Mbps" } ?: ""),
                icon = Icons.Rounded.Thermostat,
                accent = CyberAmber,
                modifier = Modifier.weight(1f)
            )
        }

        // Per-Core CPU Frequency Bars
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MidnightSurface)
                .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Text("CPU Core Frequencies", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            val maxFreq = (telemetry.cpuCoreFreqsMHz.maxOrNull() ?: 2400).coerceAtLeast(1800)
            telemetry.cpuCoreFreqsMHz.forEachIndexed { idx, freq ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "CPU $idx",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.width(46.dp)
                    )
                    LinearProgressIndicator(
                        progress = { (freq.toFloat() / maxFreq.toFloat()).coerceIn(0.1f, 1f) },
                        color = palette.accent,
                        trackColor = CyberElevated,
                        modifier = Modifier
                            .weight(1f)
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Text(
                        text = "$freq MHz",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary,
                        modifier = Modifier.width(68.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }

        // Turbo Time Credit Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MidnightSurface)
                .border(1.dp, if (remainingTurboSeconds > 0L) EmeraldActive.copy(alpha = 0.6f) else CyberAmber.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Turbo Play Time", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(
                    text = "${formatCompactTurboClock(remainingTurboSeconds)} left • ${settings.adsWatchedCount} ads watched",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (remainingTurboSeconds > 0L) EmeraldBright else CyberAmber
                )
            }
            Button(
                onClick = onAddTurboTime,
                colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("+1h Ad")
            }
        }

        // Quick Utility Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onBoostRam,
                colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.RocketLaunch, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Boost RAM (${settings.totalBoostCount})")
            }
            Button(
                onClick = onRunPingTest,
                colors = ButtonDefaults.buttonColors(containerColor = CyberElevated),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.NetworkCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(telemetry.pingMs?.let { "Ping: ${it}ms" } ?: "Test Ping")
            }
            Button(
                onClick = onToggleOverlayService,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isOverlayRunning) EmeraldActive else CyberElevated
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isOverlayRunning) "Overlay ON" else "Start Overlay")
            }
        }

        // Session History
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MidnightSurface)
                .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Text("Recent Launch Sessions", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            if (sessionHistory.isEmpty()) {
                Text(
                    text = "No game sessions recorded yet. Select a game in Lobby and tap Start Play.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            } else {
                sessionHistory.forEach { rec ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(rec.appName, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Text(
                            text = "${rec.cpuMode.label} • ${formatTime(rec.timestampMillis)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetrySummaryCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MidnightSurface)
            .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Text(title, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = RajdhaniFontFamily),
            color = TextPrimary
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun WallpaperThemeDialog(
    currentTheme: WallpaperTheme,
    onSelectTheme: (WallpaperTheme) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MidnightSurface)
                .border(1.dp, CyberBorder, RoundedCornerShape(18.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Launcher Theme & Wallpaper", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Close",
                    tint = TextSecondary,
                    modifier = Modifier.clickable { onDismiss() }
                )
            }
            WallpaperTheme.entries.forEach { theme ->
                val selected = theme == currentTheme
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) CyberElevated else CyberSlate)
                        .border(1.dp, if (selected) LocalTurboAccent.current.accent else CyberBorder, RoundedCornerShape(12.dp))
                        .clickable { onSelectTheme(theme) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(theme.label, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                    if (theme == WallpaperTheme.CUSTOM) {
                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = "Pick photo", tint = TextSecondary)
                    }
                }
            }
        }
    }
}

private fun formatClock(): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

private fun formatTime(millis: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
