package com.example.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Tune
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import com.example.ads.AdMobBannerBar
import com.example.data.CpuMode
import com.example.data.GameSessionRecord
import com.example.data.GameTurboSettings
import com.example.data.HardwareTelemetry
import com.example.data.InstalledAppInfo
import com.example.data.PermissionStatus
import com.example.data.WallpaperTheme
import com.example.ui.components.TurboCockpitReactor
import com.example.ui.components.TurboXLogoMark
import com.example.ui.components.cpuModeColor
import com.example.ui.components.formatCompactTurboClock
import com.example.ui.components.rememberTurboFlingBehavior
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
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
    peekAppIcon: (String) -> ImageBitmap?,
    loadAppIconAsync: suspend (String) -> ImageBitmap?,
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
    onSelectCpuMode: (CpuMode) -> Unit,
    onSelectWallpaperTheme: (WallpaperTheme, String?) -> Unit,
    onDismissStatus: () -> Unit
) {
    val palette = LocalTurboAccent.current
    val modeGlowColor = cpuModeColor(settings.cpuMode, palette.accent)
    var selectedBottomTab by remember { mutableIntStateOf(0) } // 0 = Cockpit, 1 = Telemetry
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
    val selectedGameIcon by produceState<ImageBitmap?>(
        initialValue = selectedGame?.let { peekAppIcon(it.packageName) },
        key1 = selectedGame?.packageName
    ) {
        val pkg = selectedGame?.packageName
        value = if (pkg != null) {
            peekAppIcon(pkg) ?: loadAppIconAsync(pkg)
        } else {
            null
        }
    }

    val context = LocalContext.current
    val customWallpaperBitmap by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = settings.customWallpaperUri,
        key2 = settings.wallpaperTheme
    ) {
        value = if (settings.wallpaperTheme == WallpaperTheme.CUSTOM && settings.customWallpaperUri != null) {
            withContext(Dispatchers.IO) {
                decodeSampledWallpaper(context, Uri.parse(settings.customWallpaperUri), maxDimensionPx = 1280)
            }
        } else {
            null
        }
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
                    colors = listOf(
                        ObsidianVoid,
                        Color(0xFF0A0F1B),
                        ObsidianVoid
                    )
                )
            )
    ) {
        // Subtle top-left signature ambient glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            modeGlowColor.copy(alpha = 0.11f),
                            Color.Transparent
                        ),
                        radius = 850f
                    )
                )
        )

        customWallpaperBitmap?.let { wallpaper ->
            Image(
                bitmap = wallpaper,
                contentDescription = "Custom launcher background",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.30f)
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            val isWide = maxWidth >= 600.dp

            Column(modifier = Modifier.fillMaxSize()) {
                // ORIGINAL TURBO X HEADER BAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TurboXLogoMark(
                            accentColor = palette.accent,
                            modifier = Modifier.size(38.dp)
                        )
                        Column {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(color = TextPrimary, fontWeight = FontWeight.Bold)) {
                                        append("TURBO ")
                                    }
                                    withStyle(SpanStyle(color = palette.accent, fontWeight = FontWeight.Bold)) {
                                        append("X")
                                    }
                                },
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = RajdhaniFontFamily,
                                    fontSize = if (isWide) 25.sp else 22.sp,
                                    letterSpacing = 1.4.sp
                                )
                            )
                            Text(
                                text = "PRECISION ENGINE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = RajdhaniFontFamily,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.6.sp
                                ),
                                color = TextMuted
                            )
                        }

                        if (isWide) {
                            Spacer(modifier = Modifier.width(6.dp))
                            HeaderStatusCluster(
                                clock = currentClock,
                                telemetry = telemetry,
                                remainingTurboSeconds = remainingTurboSeconds,
                                accentColor = palette.accent,
                                onAddTimeClick = onAddTurboTimeClick
                            )
                        }
                    }

                    // Top Right Circular Glass Action Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
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

                // MAIN ANIMATED BODY AREA
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    AnimatedContent(
                        targetState = selectedBottomTab,
                        transitionSpec = {
                            val dir = if (targetState > initialState) 1 else -1
                            (slideInHorizontally(
                                animationSpec = tween(260, easing = FastOutSlowInEasing),
                                initialOffsetX = { dir * (it / 5) }
                            ) + fadeIn(tween(220))) togetherWith
                                (slideOutHorizontally(
                                    animationSpec = tween(200, easing = FastOutSlowInEasing),
                                    targetOffsetX = { -dir * (it / 6) }
                                ) + fadeOut(tween(180)))
                        },
                        label = "main_tab_transition"
                    ) { tabIndex ->
                        if (tabIndex == 0) {
                            // COCKPIT TAB
                            if (isWide) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Left Game Dock
                                    Column(
                                        modifier = Modifier
                                            .width(248.dp)
                                            .fillMaxHeight(),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        GameDockList(
                                            games = addedGames,
                                            selectedPackage = selectedGame?.packageName,
                                            accentColor = palette.accent,
                                            peekAppIcon = peekAppIcon,
                                            loadAppIconAsync = loadAppIconAsync,
                                            onSelectGame = onSelectGame,
                                            onAddGames = onNavigateAddApps,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        AddGameSquareButton(
                                            label = "Add Games",
                                            onClick = onNavigateAddApps,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    // Center Flagship Cockpit Hero Card + Right Action Deck
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        CockpitHeroCard(
                                            selectedGame = selectedGame,
                                            selectedGameIcon = selectedGameIcon,
                                            settings = settings,
                                            telemetry = telemetry,
                                            glowColor = modeGlowColor,
                                            secondaryColor = palette.secondaryGlow,
                                            onSelectCpuMode = onSelectCpuMode,
                                            onBoostRam = onBoostRam,
                                            onRunPingTest = onRunPingTest,
                                            onAddGames = onNavigateAddApps,
                                            modifier = Modifier.weight(1f)
                                        )

                                        StartPlayButtonCluster(
                                            accentColor = palette.accent,
                                            accentDeep = palette.accentDeep,
                                            remainingTurboSeconds = remainingTurboSeconds,
                                            onStartPlay = onStartPlay,
                                            onQuickPanel = onOpenOverlayPreview
                                        )
                                    }
                                }
                            } else {
                                // Portrait Adaptive Cockpit
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Center Pro Telemetry Hero Card (fills middle weight cleanly without empty void)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CockpitHeroCard(
                                            selectedGame = selectedGame,
                                            selectedGameIcon = selectedGameIcon,
                                            settings = settings,
                                            telemetry = telemetry,
                                            glowColor = modeGlowColor,
                                            secondaryColor = palette.secondaryGlow,
                                            onSelectCpuMode = onSelectCpuMode,
                                            onBoostRam = onBoostRam,
                                            onRunPingTest = onRunPingTest,
                                            onAddGames = onNavigateAddApps,
                                            modifier = Modifier
                                                .widthIn(max = 520.dp)
                                                .fillMaxWidth()
                                        )
                                    }

                                    StartPlayButtonCluster(
                                        accentColor = palette.accent,
                                        accentDeep = palette.accentDeep,
                                        remainingTurboSeconds = remainingTurboSeconds,
                                        onStartPlay = onStartPlay,
                                        onQuickPanel = onOpenOverlayPreview
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Horizontal Game Shelf
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        AddGameSquareButton(onClick = onNavigateAddApps)
                                        if (addedGames.isEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(48.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(CyberSlate.copy(alpha = 0.65f))
                                                    .border(1.dp, CyberBorder.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                                                    .clickable { onNavigateAddApps() }
                                                    .padding(horizontal = 14.dp),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                Text(
                                                    text = "Tap + to add games to Turbo X",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = TextSecondary
                                                )
                                            }
                                        } else {
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                flingBehavior = rememberTurboFlingBehavior(),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                items(addedGames, key = { it.packageName }) { game ->
                                                    val chipIcon by produceState<ImageBitmap?>(
                                                        initialValue = peekAppIcon(game.packageName),
                                                        key1 = game.packageName
                                                    ) {
                                                        if (value == null) {
                                                            value = loadAppIconAsync(game.packageName)
                                                        }
                                                    }
                                                    PortraitGameChip(
                                                        game = game,
                                                        selected = game.packageName == selectedGame?.packageName,
                                                        accentColor = palette.accent,
                                                        icon = chipIcon,
                                                        onClick = { onSelectGame(game.packageName) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                        } else {
                            // TELEMETRY TAB
                            TelemetryPageContent(
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
                    }

                    // Smooth Animated Status Banner
                    androidx.compose.animation.AnimatedVisibility(
                        visible = statusMessage != null,
                        enter = slideInVertically(
                            initialOffsetY = { -it },
                            animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)
                        ) + fadeIn(tween(180)),
                        exit = slideOutVertically(
                            targetOffsetY = { -it },
                            animationSpec = tween(160)
                        ) + fadeOut(tween(150)),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 6.dp, start = 16.dp, end = 16.dp)
                    ) {
                        if (statusMessage != null) {
                            Surface(
                                color = CyberElevated.copy(alpha = 0.96f),
                                shape = RoundedCornerShape(16.dp),
                                shadowElevation = 8.dp,
                                modifier = Modifier.border(1.dp, palette.accent.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                            ) {
                                Text(
                                    text = statusMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                                )
                            }
                        }
                    }
                }

                // ORIGINAL FLOATING PILL NAVIGATION BAR
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .widthIn(max = 420.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(MidnightSurface.copy(alpha = 0.94f))
                            .border(1.dp, CyberBorder.copy(alpha = 0.8f), RoundedCornerShape(24.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CockpitNavPill(
                            title = "Cockpit",
                            icon = Icons.Rounded.Dashboard,
                            selected = selectedBottomTab == 0,
                            accentColor = palette.accent,
                            onClick = { selectedBottomTab = 0 },
                            testTag = "tab_lobby",
                            modifier = Modifier.weight(1f)
                        )
                        CockpitNavPill(
                            title = "Telemetry",
                            icon = Icons.Rounded.Analytics,
                            selected = selectedBottomTab == 1,
                            accentColor = palette.accent,
                            onClick = { selectedBottomTab = 1 },
                            testTag = "tab_database",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // DEDICATED BOTTOM-ANCHORED BANNER AD DOCK (Clean separation prevents mis-taps)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AdMobBannerBar(
                        modifier = Modifier
                            .widthIn(max = 520.dp)
                            .fillMaxWidth()
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
private fun CockpitHeroCard(
    selectedGame: InstalledAppInfo?,
    selectedGameIcon: ImageBitmap?,
    settings: GameTurboSettings,
    telemetry: HardwareTelemetry,
    glowColor: Color,
    secondaryColor: Color,
    onSelectCpuMode: (CpuMode) -> Unit,
    onBoostRam: () -> Unit,
    onRunPingTest: () -> Unit,
    onAddGames: () -> Unit,
    modifier: Modifier = Modifier
) {
    val targetHz = maxOf(settings.frameRateTarget, settings.fpsGameTarget)
    val themeAccent = LocalTurboAccent.current.accent
    val animatedAccent by animateColorAsState(
        targetValue = glowColor,
        animationSpec = tween(320),
        label = "hero_accent"
    )
    val cpuFraction by animateFloatAsState(
        targetValue = (telemetry.cpuFreqMHz / 3200f).coerceIn(0.18f, 1f),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
        label = "hero_cpu_bar"
    )
    val ramFraction by animateFloatAsState(
        targetValue = (telemetry.ramUsedPercent / 100f).coerceIn(0.10f, 1f),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
        label = "hero_ram_bar"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MidnightSurface.copy(alpha = 0.96f),
                        Color(0xFF090D18)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        animatedAccent.copy(alpha = 0.55f),
                        CyberBorder.copy(alpha = 0.7f),
                        secondaryColor.copy(alpha = 0.35f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Selected Game Profile Header + Quick Boost Chip
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = selectedGame == null) { onAddGames() }
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberElevated)
                        .border(1.dp, animatedAccent.copy(alpha = 0.55f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedGameIcon != null) {
                        Image(
                            bitmap = selectedGameIcon,
                            contentDescription = selectedGame?.appName ?: "Selected Game",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.SportsEsports,
                            contentDescription = "Add Game",
                            tint = animatedAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedGame?.appName ?: "No Game Selected",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (selectedGame != null) {
                            "${targetHz}Hz Target • ${telemetry.cpuCoreCount} Cores Active"
                        } else {
                            "Tap + below to add a game"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Quick Boost Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .testTag("hero_quick_boost_btn")
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(animatedAccent.copy(alpha = 0.14f))
                    .border(1.dp, animatedAccent.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .clickable { onBoostRam() }
                    .padding(horizontal = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = "Quick RAM Boost",
                    tint = animatedAccent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "BOOST",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = TextPrimary
                )
            }
        }

        // Subtle divider line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(CyberBorder.copy(alpha = 0.45f))
        )

        // 2. Precision FPS Dial + Live Hardware Readout Stack
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TurboCockpitReactor(
                glowColor = animatedAccent,
                secondaryColor = secondaryColor,
                actualFps = telemetry.actualFps,
                cpuFreqMHz = telemetry.cpuFreqMHz,
                ramUsedPercent = telemetry.ramUsedPercent,
                animatePulse = settings.homeAnimation,
                modifier = Modifier.size(144.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                HeroTelemetryBarRow(
                    label = "CPU CLOCK",
                    valueText = "${telemetry.cpuFreqMHz} MHz",
                    progress = cpuFraction,
                    barColor = animatedAccent
                )
                HeroTelemetryBarRow(
                    label = "MEMORY LOAD",
                    valueText = "${telemetry.ramUsedPercent}% RAM",
                    progress = ramFraction,
                    barColor = secondaryColor
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HeroMiniBadge(
                        icon = Icons.Rounded.Thermostat,
                        label = "${telemetry.batteryTempCelsius}°C",
                        tint = if (telemetry.batteryTempCelsius >= 41f) CyberAmber else EmeraldBright,
                        modifier = Modifier.weight(1f)
                    )
                    HeroMiniBadge(
                        icon = Icons.Rounded.NetworkCheck,
                        label = telemetry.pingMs?.let { "${it} ms" } ?: "Ping",
                        tint = ElectricCyan,
                        onClick = onRunPingTest,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Interactive Engine Mode Selector (Eco | Balanced | Turbo)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(ObsidianVoid.copy(alpha = 0.78f))
                .border(1.dp, CyberBorder.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                CpuMode.ENERGY_SAVING to "Eco",
                CpuMode.BALANCED to "Balanced",
                CpuMode.ACCELERATION to "Turbo"
            ).forEach { (mode, shortLabel) ->
                val selected = settings.cpuMode == mode
                val pillColor = cpuModeColor(mode, themeAccent)
                val bg by animateColorAsState(
                    targetValue = if (selected) pillColor.copy(alpha = 0.22f) else Color.Transparent,
                    animationSpec = tween(220),
                    label = "mode_pill_bg"
                )
                val border by animateColorAsState(
                    targetValue = if (selected) pillColor.copy(alpha = 0.85f) else Color.Transparent,
                    animationSpec = tween(220),
                    label = "mode_pill_border"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bg)
                        .border(1.dp, border, RoundedCornerShape(10.dp))
                        .clickable { onSelectCpuMode(mode) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = shortLabel.uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.9.sp
                        ),
                        color = if (selected) pillColor else TextMuted,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroTelemetryBarRow(
    label: String,
    valueText: String,
    progress: Float,
    barColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = RajdhaniFontFamily,
                    letterSpacing = 0.8.sp,
                    fontSize = 10.sp
                ),
                color = TextMuted
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = RajdhaniFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = TextPrimary
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            color = barColor,
            trackColor = CyberSlate,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )
    }
}

@Composable
private fun HeroMiniBadge(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(CyberElevated.copy(alpha = 0.85f))
            .border(1.dp, CyberBorder.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = RajdhaniFontFamily,
                fontWeight = FontWeight.Bold
            ),
            color = TextPrimary,
            maxLines = 1
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
    val timeColor by animateColorAsState(
        targetValue = if (hasTime) EmeraldBright else CyberAmber,
        animationSpec = tween(250),
        label = "turbo_time_color"
    )
    val formattedRemaining = formatCompactTurboClock(remainingTurboSeconds)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = clock,
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = RajdhaniFontFamily),
            color = TextSecondary
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(CyberSlate.copy(alpha = 0.7f))
                .padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
            Icon(
                imageVector = if (telemetry.isCharging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryStd,
                contentDescription = "Battery level",
                tint = EmeraldActive,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "${telemetry.batteryPercent}%",
                style = MaterialTheme.typography.labelSmall,
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
                    if (hasTime) EmeraldActive.copy(alpha = 0.55f) else CyberAmber.copy(alpha = 0.55f),
                    RoundedCornerShape(10.dp)
                )
                .clickable { onAddTimeClick() }
                .padding(horizontal = 9.dp, vertical = 5.dp)
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
                    .size(30.dp)
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
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "header_btn_scale"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .testTag(testTag)
            .size(44.dp)
            .clip(CircleShape)
            .background(CyberSlate.copy(alpha = 0.9f))
            .border(1.dp, CyberBorder.copy(alpha = 0.9f), CircleShape)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = TextPrimary, modifier = Modifier.size(20.dp))
        if (badgeDotColor != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(7.dp)
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
    peekAppIcon: (String) -> ImageBitmap?,
    loadAppIconAsync: suspend (String) -> ImageBitmap?,
    onSelectGame: (String) -> Unit,
    onAddGames: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (games.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MidnightSurface.copy(alpha = 0.88f))
                .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
                .clickable { onAddGames() }
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.SportsEsports, contentDescription = null, tint = accentColor, modifier = Modifier.size(34.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("No games added", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap below to add installed games",
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
            flingBehavior = rememberTurboFlingBehavior(),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(games, key = { it.packageName }) { game ->
                val isSelected = game.packageName == selectedPackage
                val iconBitmap by produceState<ImageBitmap?>(
                    initialValue = peekAppIcon(game.packageName),
                    key1 = game.packageName
                ) {
                    if (value == null) {
                        value = loadAppIconAsync(game.packageName)
                    }
                }
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) accentColor.copy(alpha = 0.16f) else MidnightSurface.copy(alpha = 0.78f),
                    animationSpec = tween(200),
                    label = "dock_bg"
                )
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) accentColor else CyberBorder.copy(alpha = 0.55f),
                    animationSpec = tween(200),
                    label = "dock_border"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(bgColor)
                        .border(if (isSelected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
                        .clickable { onSelectGame(game.packageName) }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val resolvedDockIcon = iconBitmap
                    if (resolvedDockIcon != null) {
                        Image(
                            bitmap = resolvedDockIcon,
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
                            text = game.appName,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "v${game.versionName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
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
    val bgColor by animateColorAsState(
        targetValue = if (selected) accentColor.copy(alpha = 0.22f) else CyberSlate.copy(alpha = 0.9f),
        animationSpec = tween(200),
        label = "chip_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) accentColor else CyberBorder,
        animationSpec = tween(200),
        label = "chip_border"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.03f else 1f,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "chip_scale"
    )

    Row(
        modifier = Modifier
            .scale(scale)
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(if (selected) 1.4.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = game.appName,
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        }
        Text(
            text = game.appName,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = TextPrimary,
            maxLines = 1
        )
    }
}

@Composable
private fun AddGameSquareButton(
    label: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .testTag("add_games_button")
            .height(48.dp)
            .then(if (label == null) Modifier.width(48.dp) else Modifier)
            .clip(RoundedCornerShape(14.dp))
            .background(CyberElevated)
            .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = if (label == null) 0.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Rounded.Add, contentDescription = "Add Apps", tint = TextPrimary, modifier = Modifier.size(22.dp))
        if (label != null) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
        }
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
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "launch_btn_scale"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .scale(buttonScale)
                    .testTag("start_play_button")
                    .height(52.dp)
                    .width(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = if (hasTime) {
                                listOf(accentColor, accentDeep)
                            } else {
                                listOf(CyberElevated, CyberSlate)
                            }
                        )
                    )
                    .border(
                        1.2.dp,
                        if (hasTime) accentColor.copy(alpha = 0.9f) else CyberAmber.copy(alpha = 0.8f),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable(interactionSource = interactionSource, indication = null) { onStartPlay() },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (hasTime) Icons.Rounded.PlayArrow else Icons.Rounded.Bolt,
                        contentDescription = null,
                        tint = if (hasTime) Color.White else CyberAmber,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = if (hasTime) "LAUNCH TURBO" else "UNLOCK +1H PLAY",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            letterSpacing = 1.sp
                        ),
                        color = if (hasTime) Color.White else CyberAmber
                    )
                }
            }

            Box(
                modifier = Modifier
                    .testTag("quick_panel_arrow_btn")
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MidnightSurface.copy(alpha = 0.92f))
                    .border(1.2.dp, accentColor.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                    .clickable { onQuickPanel() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = "Open Game Panel",
                    tint = TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Text(
            text = if (hasTime) {
                "Turbo Active • ${formatCompactTurboClock(remainingTurboSeconds)} remaining"
            } else {
                "Timer 0:00:00 • Watch 1 short Ad for +1h:00m"
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (hasTime) EmeraldBright else CyberAmber
        )
    }
}

@Composable
private fun CockpitNavPill(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) accentColor.copy(alpha = 0.22f) else Color.Transparent,
        animationSpec = tween(220),
        label = "nav_pill_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) accentColor.copy(alpha = 0.75f) else Color.Transparent,
        animationSpec = tween(220),
        label = "nav_pill_border"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) TextPrimary else TextSecondary,
        animationSpec = tween(200),
        label = "nav_pill_text"
    )

    Row(
        modifier = modifier
            .testTag(testTag)
            .height(44.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable { onClick() },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) accentColor else TextSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = RajdhaniFontFamily,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 15.sp
            ),
            color = contentColor
        )
    }
}

@Composable
private fun TelemetryPageContent(
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
            .verticalScroll(rememberScrollState(), flingBehavior = rememberTurboFlingBehavior())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
                title = "Thermal & Net",
                value = "${telemetry.batteryTempCelsius}°C",
                subtitle = "${telemetry.displayRefreshRateHz}Hz • ${telemetry.networkType}" +
                    (telemetry.wifiLinkMbps?.let { " ${it}Mbps" } ?: ""),
                icon = Icons.Rounded.Thermostat,
                accent = CyberAmber,
                modifier = Modifier.weight(1f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MidnightSurface.copy(alpha = 0.92f))
                .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Text("CPU Core Frequencies", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            val maxFreq = (telemetry.cpuCoreFreqsMHz.maxOrNull() ?: 2400).coerceAtLeast(1800)
            telemetry.cpuCoreFreqsMHz.forEachIndexed { idx, freq ->
                val animatedRatio by animateFloatAsState(
                    targetValue = (freq.toFloat() / maxFreq.toFloat()).coerceIn(0.1f, 1f),
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
                    label = "core_bar_$idx"
                )
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
                        progress = { animatedRatio },
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MidnightSurface.copy(alpha = 0.92f))
                .border(
                    1.dp,
                    if (remainingTurboSeconds > 0L) EmeraldActive.copy(alpha = 0.6f) else CyberAmber.copy(alpha = 0.6f),
                    RoundedCornerShape(16.dp)
                )
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
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("+1h Ad")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onBoostRam,
                colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(Icons.Rounded.RocketLaunch, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Boost (${settings.totalBoostCount})")
            }
            Button(
                onClick = onRunPingTest,
                colors = ButtonDefaults.buttonColors(containerColor = CyberElevated),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(Icons.Rounded.NetworkCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(telemetry.pingMs?.let { "${it}ms" } ?: "Ping")
            }
            Button(
                onClick = onToggleOverlayService,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isOverlayRunning) EmeraldActive else CyberElevated
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text(if (isOverlayRunning) "HUD ON" else "HUD OFF")
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MidnightSurface.copy(alpha = 0.92f))
                .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Text("Recent Launch Sessions", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            if (sessionHistory.isEmpty()) {
                Text(
                    text = "No game sessions recorded yet. Select a game in Cockpit and tap LAUNCH TURBO.",
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
            .clip(RoundedCornerShape(16.dp))
            .background(MidnightSurface.copy(alpha = 0.92f))
            .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Text(title, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = RajdhaniFontFamily),
            color = TextPrimary,
            maxLines = 1
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
                .clip(RoundedCornerShape(20.dp))
                .background(MidnightSurface)
                .border(1.dp, CyberBorder, RoundedCornerShape(20.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Cockpit Theme & Wallpaper", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Close",
                    tint = TextSecondary,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { onDismiss() }
                )
            }
            WallpaperTheme.entries.forEach { theme ->
                val selected = theme == currentTheme
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) CyberElevated else CyberSlate)
                        .border(1.dp, if (selected) LocalTurboAccent.current.accent else CyberBorder, RoundedCornerShape(14.dp))
                        .clickable { onSelectTheme(theme) }
                        .padding(14.dp),
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

private fun decodeSampledWallpaper(
    context: android.content.Context,
    uri: Uri,
    maxDimensionPx: Int
): ImageBitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, bounds)
    }
    var sampleSize = 1
    val width = bounds.outWidth
    val height = bounds.outHeight
    if (width > maxDimensionPx || height > maxDimensionPx) {
        while ((width / sampleSize) > maxDimensionPx * 2 || (height / sampleSize) > maxDimensionPx * 2) {
            sampleSize *= 2
        }
    }
    val decodeOpts = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
    }
    context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, decodeOpts)?.asImageBitmap()
    }
}.getOrNull()

private fun formatClock(): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

private fun formatTime(millis: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
