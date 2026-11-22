package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.data.PermissionStatus
import com.example.ui.components.TurboXTopBar
import com.example.ui.theme.CrimsonTurbo
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberElevated
import com.example.ui.theme.CyberSlate
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private enum class AutoPermStep {
    NOTIFICATIONS,
    OVERLAY,
    DND,
    WRITE_SETTINGS,
    USAGE_STATS
}

private data class PermissionUiItem(
    val step: AutoPermStep,
    val title: String,
    val badge: String,
    val description: String,
    val icon: ImageVector,
    val granted: Boolean,
    val testTag: String,
    val onRequest: () -> Unit
)

@Composable
fun PermissionsScreen(
    permissions: PermissionStatus,
    onRefreshPermissions: () -> Unit,
    onRequestDnd: () -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestWriteSettings: () -> Unit,
    onRequestUsageStats: () -> Unit = {},
    onSuppressAppOpen: () -> Unit = {},
    onBack: () -> Unit,
    isOnboardingMode: Boolean = false,
    onCompleteOnboarding: () -> Unit = onBack
) {
    var showHelpDialog by remember { mutableStateOf(false) }
    var autoChainActive by remember { mutableStateOf(false) }
    var pendingStep by remember { mutableStateOf<AutoPermStep?>(null) }
    var resumeTrigger by remember { mutableIntStateOf(0) }
    var visibleCards by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visibleCards = true
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        onRefreshPermissions()
        resumeTrigger += 1
    }

    fun launchNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onSuppressAppOpen()
            runCatching {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }.onFailure {
                autoChainActive = false
                pendingStep = null
            }
        }
    }

    fun launchStep(step: AutoPermStep) {
        pendingStep = step
        when (step) {
            AutoPermStep.NOTIFICATIONS -> launchNotificationPermission()
            AutoPermStep.OVERLAY -> onRequestOverlay()
            AutoPermStep.DND -> onRequestDnd()
            AutoPermStep.WRITE_SETTINGS -> onRequestWriteSettings()
            AutoPermStep.USAGE_STATS -> onRequestUsageStats()
        }
    }

    fun nextMissingSpecialStep(status: PermissionStatus): AutoPermStep? = when {
        !status.overlayGranted -> AutoPermStep.OVERLAY
        !status.dndGranted -> AutoPermStep.DND
        !status.writeSettingsGranted -> AutoPermStep.WRITE_SETTINGS
        !status.usageStatsGranted -> AutoPermStep.USAGE_STATS
        else -> null
    }

    fun nextMissingAnyStep(status: PermissionStatus): AutoPermStep? = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !status.notificationsGranted -> AutoPermStep.NOTIFICATIONS
        else -> nextMissingSpecialStep(status)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        onRefreshPermissions()
        resumeTrigger += 1
    }

    LaunchedEffect(resumeTrigger) {
        if (resumeTrigger == 0 || !autoChainActive) return@LaunchedEffect
        val expected = pendingStep
        if (expected == null) {
            autoChainActive = false
            return@LaunchedEffect
        }
        val wasGranted = when (expected) {
            AutoPermStep.NOTIFICATIONS -> true // Runtime prompt answered; proceed to special access steps if needed
            AutoPermStep.OVERLAY -> permissions.overlayGranted
            AutoPermStep.DND -> permissions.dndGranted
            AutoPermStep.WRITE_SETTINGS -> permissions.writeSettingsGranted
            AutoPermStep.USAGE_STATS -> permissions.usageStatsGranted
        }
        if (wasGranted) {
            val next = if (expected == AutoPermStep.NOTIFICATIONS) {
                nextMissingSpecialStep(permissions)
            } else {
                nextMissingAnyStep(permissions)
            }
            if (next != null) {
                launchStep(next)
            } else {
                autoChainActive = false
                pendingStep = null
            }
        } else {
            // User returned without toggling the system permission; stop auto-chain cleanly
            autoChainActive = false
            pendingStep = null
        }
    }

    val items = remember(permissions) {
        listOf(
            PermissionUiItem(
                step = AutoPermStep.OVERLAY,
                title = "Floating Overlay HUD",
                badge = "Required",
                description = "Display ROG, Xiaomi, RedMagic & Cyber control panels directly over games.",
                icon = Icons.Rounded.Layers,
                granted = permissions.overlayGranted,
                testTag = "perm_overlay_card",
                onRequest = {
                    autoChainActive = false
                    pendingStep = null
                    onRequestOverlay()
                }
            ),
            PermissionUiItem(
                step = AutoPermStep.DND,
                title = "Do Not Disturb Access",
                badge = "Focus",
                description = "Silence incoming notifications and calls automatically while gaming.",
                icon = Icons.Rounded.NotificationsOff,
                granted = permissions.dndGranted,
                testTag = "perm_dnd_card",
                onRequest = {
                    autoChainActive = false
                    pendingStep = null
                    onRequestDnd()
                }
            ),
            PermissionUiItem(
                step = AutoPermStep.WRITE_SETTINGS,
                title = "Modify System Settings",
                badge = "Display & Touch",
                description = "Unlock brightness lock, AutoLight, Auto-Rotate, and touch response tuning.",
                icon = Icons.Rounded.Tune,
                granted = permissions.writeSettingsGranted,
                testTag = "perm_write_settings_card",
                onRequest = {
                    autoChainActive = false
                    pendingStep = null
                    onRequestWriteSettings()
                }
            ),
            PermissionUiItem(
                step = AutoPermStep.USAGE_STATS,
                title = "Game Auto-Detection",
                badge = "Smart Launch",
                description = "Detect when installed games open to automatically boost FPS & memory.",
                icon = Icons.Rounded.QueryStats,
                granted = permissions.usageStatsGranted,
                testTag = "perm_usage_stats_card",
                onRequest = {
                    autoChainActive = false
                    pendingStep = null
                    onRequestUsageStats()
                }
            ),
            PermissionUiItem(
                step = AutoPermStep.NOTIFICATIONS,
                title = "Engine & Boost Alerts",
                badge = "Foreground",
                description = "Keep persistent HUD service active and show real-time RAM boost status.",
                icon = Icons.Rounded.NotificationsActive,
                granted = permissions.notificationsGranted,
                testTag = "perm_notifications_card",
                onRequest = {
                    autoChainActive = false
                    pendingStep = null
                    launchNotificationPermission()
                }
            )
        )
    }

    val progressFraction = (permissions.grantedCount.toFloat() / permissions.totalCount.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow),
        label = "perm_reactor_progress"
    )
    val heroAccent by animateColorAsState(
        targetValue = when {
            permissions.allGranted -> EmeraldBright
            permissions.grantedCount >= 3 -> ElectricCyan
            permissions.grantedCount >= 1 -> CyberAmber
            else -> CrimsonTurbo
        },
        animationSpec = tween(300),
        label = "perm_hero_accent"
    )

    Box(
        modifier = Modifier
            .testTag("permissions_screen_root")
            .fillMaxSize()
            .background(ObsidianVoid)
    ) {
        // Subtle ambient cyber lighting backdrop
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(heroAccent.copy(alpha = 0.16f), Color.Transparent),
                    center = Offset(size.width * 0.15f, size.height * 0.10f),
                    radius = size.maxDimension * 0.55f
                ),
                radius = size.maxDimension * 0.55f,
                center = Offset(size.width * 0.15f, size.height * 0.10f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ElectricCyan.copy(alpha = 0.11f), Color.Transparent),
                    center = Offset(size.width * 0.88f, size.height * 0.82f),
                    radius = size.maxDimension * 0.48f
                ),
                radius = size.maxDimension * 0.48f,
                center = Offset(size.width * 0.88f, size.height * 0.82f)
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            val isLandscape = maxWidth >= 600.dp && maxWidth > maxHeight
            val leftPaneWidth = (maxWidth * 0.38f).coerceIn(255.dp, 340.dp)
            val missingCount = permissions.totalCount - permissions.grantedCount
            val ctaBrush = if (permissions.allGranted) {
                Brush.horizontalGradient(listOf(EmeraldActive, EmeraldBright))
            } else {
                Brush.horizontalGradient(listOf(CrimsonTurbo, Color(0xFFFF5252)))
            }

            val topHeaderBar: @Composable () -> Unit = {
                if (!isOnboardingMode) {
                    TurboXTopBar(
                        title = "System Permissions",
                        subtitle = "Hardware & Overlay Access",
                        onBack = onBack,
                        backTestTag = "permissions_back_btn"
                    ) {
                        HelpBadgeButton(onClick = { showHelpDialog = true })
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = if (isLandscape) 8.dp else 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(heroAccent.copy(alpha = 0.16f))
                                    .border(1.dp, heroAccent.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Security, contentDescription = null, tint = heroAccent, modifier = Modifier.size(20.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "TURBO X AUTHORIZATION",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = RajdhaniFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Initial Setup • System Access",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        HelpBadgeButton(onClick = { showHelpDialog = true })
                    }
                }
            }

            val grantActionButtons: @Composable () -> Unit = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 560.dp)
                            .fillMaxWidth()
                            .height(if (isLandscape) 48.dp else 54.dp)
                            .testTag("grant_all_permissions_btn")
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(14.dp))
                            .background(ctaBrush)
                            .clickable {
                                if (permissions.allGranted) {
                                    onCompleteOnboarding()
                                } else {
                                    val first = nextMissingAnyStep(permissions)
                                    if (first != null) {
                                        autoChainActive = true
                                        launchStep(first)
                                    } else if (isOnboardingMode) {
                                        onCompleteOnboarding()
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = if (permissions.allGranted) Icons.Rounded.VerifiedUser else Icons.Rounded.Bolt,
                                contentDescription = null,
                                tint = if (permissions.allGranted) Color(0xFF04150E) else Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                            Text(
                                text = if (permissions.allGranted) {
                                    if (isOnboardingMode) "ALL ARMED • ENTER TURBO X" else "ALL PERMISSIONS GRANTED"
                                } else {
                                    "GRANT ALL PERMISSIONS ($missingCount LEFT)"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = RajdhaniFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    fontSize = if (isLandscape) 14.sp else 16.sp
                                ),
                                color = if (permissions.allGranted) Color(0xFF04150E) else Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (isOnboardingMode && !permissions.allGranted) {
                        Box(
                            modifier = Modifier
                                .testTag("skip_permissions_btn")
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onCompleteOnboarding() }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Continue to Cockpit (${permissions.grantedCount}/${permissions.totalCount} Ready)",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                topHeaderBar()

                if (isLandscape) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left Pane: Progress Hero + Pinned Authorization Actions
                        Column(
                            modifier = Modifier
                                .width(leftPaneWidth)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PermissionHeroCard(
                                grantedCount = permissions.grantedCount,
                                totalCount = permissions.totalCount,
                                allGranted = permissions.allGranted,
                                animatedProgress = animatedProgress,
                                accent = heroAccent,
                                items = items
                            )
                            grantActionButtons()
                        }

                        // Right Pane: Scrollable Permission Cards
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                                .padding(bottom = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items.forEachIndexed { index, item ->
                                AnimatedVisibility(
                                    visible = visibleCards,
                                    enter = fadeIn(tween(240, delayMillis = index * 55)) +
                                        slideInVertically(
                                            animationSpec = tween(260, delayMillis = index * 55, easing = FastOutSlowInEasing),
                                            initialOffsetY = { it / 4 }
                                        )
                                ) {
                                    PermissionCard(item = item)
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.widthIn(max = 580.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PermissionHeroCard(
                                grantedCount = permissions.grantedCount,
                                totalCount = permissions.totalCount,
                                allGranted = permissions.allGranted,
                                animatedProgress = animatedProgress,
                                accent = heroAccent,
                                items = items
                            )

                            items.forEachIndexed { index, item ->
                                AnimatedVisibility(
                                    visible = visibleCards,
                                    enter = fadeIn(tween(240, delayMillis = index * 55)) +
                                        slideInVertically(
                                            animationSpec = tween(260, delayMillis = index * 55, easing = FastOutSlowInEasing),
                                            initialOffsetY = { it / 4 }
                                        )
                                ) {
                                    PermissionCard(item = item)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }

                    // Bottom Sticky Authorization Dock (Portrait / Compact fallback)
                    Surface(
                        color = MidnightSurface.copy(alpha = 0.96f),
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CyberBorder.copy(alpha = 0.6f), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            grantActionButtons()
                        }
                    }
                }
            }
        }
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            containerColor = MidnightSurface,
            title = { Text("Permission Guide", color = TextPrimary) },
            text = {
                Text(
                    text = "• Floating Overlay HUD: Renders the ROG, Xiaomi, RedMagic, or Cyber HUD panel directly on top of games.\n\n" +
                        "• Do Not Disturb: Silences alerts automatically when entering Game Mode.\n\n" +
                        "• Modify System Settings: Enables brightness lock, AutoLight, Auto-Rotate, and touch speed tuning.\n\n" +
                        "• Game Auto-Detection: Detects when an added game starts to auto-apply your profile.\n\n" +
                        "• Engine & Boost Alerts: Shows live boost feedback and keeps the foreground HUD active.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("Got it")
                }
            }
        )
    }
}

@Composable
private fun HelpBadgeButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .testTag("permissions_help_btn")
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(10.dp))
            .background(CyberElevated)
            .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(Icons.AutoMirrored.Rounded.HelpOutline, contentDescription = "Permission Help", tint = ElectricCyan, modifier = Modifier.size(16.dp))
        Text("Guide", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
    }
}

@Composable
private fun PermissionHeroCard(
    grantedCount: Int,
    totalCount: Int,
    allGranted: Boolean,
    animatedProgress: Float,
    accent: Color,
    items: List<PermissionUiItem>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(MidnightSurface, Color(0xFF101728))
                )
            )
            .border(1.2.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(72.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeW = 6.dp.toPx()
                    val pad = strokeW / 2f
                    val arcSize = Size(size.width - strokeW, size.height - strokeW)
                    drawArc(
                        color = CyberBorder,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(pad, pad),
                        size = arcSize,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = accent,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        topLeft = Offset(pad, pad),
                        size = arcSize,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$grantedCount/$totalCount",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = accent
                    )
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (allGranted) "ALL SYSTEMS ARMED" else "PERMISSIONS REQUIRED",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = if (allGranted) EmeraldBright else TextPrimary
                )
                Text(
                    text = if (allGranted) {
                        "Every Turbo X overlay, performance, and anti-disturbance feature is unlocked."
                    } else {
                        "Tap \"Grant All Permissions\" below to enable full in-game HUD, DND, and boost controls."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        // 5-Step Illuminated Segment Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items.forEach { item ->
                val segColor by animateColorAsState(
                    targetValue = if (item.granted) EmeraldBright else CyberBorder,
                    animationSpec = tween(250),
                    label = "seg_${item.step.name}"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(segColor)
                )
            }
        }
    }
}

@Composable
private fun PermissionCard(item: PermissionUiItem) {
    val cardAccent by animateColorAsState(
        targetValue = if (item.granted) EmeraldBright else CrimsonTurbo,
        animationSpec = tween(250),
        label = "perm_accent_${item.step.name}"
    )
    val pillBg by animateColorAsState(
        targetValue = if (item.granted) EmeraldActive.copy(alpha = 0.22f) else cardAccent.copy(alpha = 0.16f),
        animationSpec = tween(250),
        label = "perm_pill_bg_${item.step.name}"
    )

    Column(
        modifier = Modifier
            .testTag(item.testTag)
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(CyberSlate, Color(0xFF131B2C))
                )
            )
            .border(
                width = if (item.granted) 1.2.dp else 1.dp,
                color = if (item.granted) EmeraldBright.copy(alpha = 0.55f) else CyberBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { item.onRequest() }
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(cardAccent.copy(alpha = 0.14f))
                    .border(1.dp, cardAccent.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = cardAccent,
                    modifier = Modifier.size(21.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF192338))
                        .border(1.dp, ElectricCyan.copy(alpha = 0.28f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.badge.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 0.7.sp
                        ),
                        color = ElectricCyan,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(pillBg)
                    .border(1.dp, cardAccent.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 11.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = if (item.granted) "DONE" else "ALLOW",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    ),
                    color = if (item.granted) EmeraldBright else Color.White,
                    maxLines = 1,
                    softWrap = false
                )
                Icon(
                    imageVector = if (item.granted) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = if (item.granted) "Granted" else "Grant ${item.title}",
                    tint = if (item.granted) EmeraldBright else Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Text(
            text = item.description,
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 17.sp),
            color = TextSecondary,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
