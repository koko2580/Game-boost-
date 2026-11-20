package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.data.PermissionStatus
import com.example.ui.components.TurboXTopBar
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberElevated
import com.example.ui.theme.CyberSlate
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PermissionsScreen(
    permissions: PermissionStatus,
    onRefreshPermissions: () -> Unit,
    onRequestDnd: () -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestWriteSettings: () -> Unit,
    onBack: () -> Unit
) {
    var showHelpDialog by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        onRefreshPermissions()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid)
            .systemBarsPadding()
    ) {
        TurboXTopBar(
            title = "System Permissions",
            subtitle = "Hardware & Overlay Access",
            onBack = onBack,
            backTestTag = "permissions_back_btn"
        ) {
            Box(
                modifier = Modifier
                    .testTag("permissions_help_btn")
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberElevated)
                    .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
                    .clickable { showHelpDialog = true }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text("Help", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(
                modifier = Modifier.widthIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                PermissionCard(
                    title = "Do Not Disturb",
                    description = "Allows Turbo X to silence incoming alerts automatically while gaming.",
                    granted = permissions.dndGranted,
                    onClick = onRequestDnd,
                    testTag = "perm_dnd_card"
                )

                PermissionCard(
                    title = "Floating Overlay HUD",
                    description = "Allows Turbo X to render the live performance HUD handle over games.",
                    granted = permissions.overlayGranted,
                    onClick = onRequestOverlay,
                    testTag = "perm_overlay_card"
                )

                PermissionCard(
                    title = "Modifying System Settings",
                    description = "Enables direct brightness lock, AutoLight, Auto-Rotate, and pointer speed tuning.",
                    granted = permissions.writeSettingsGranted,
                    onClick = onRequestWriteSettings,
                    testTag = "perm_write_settings_card"
                )
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
                    text = "• Do Not Disturb lets Turbo X silence alerts automatically while gaming.\n\n" +
                        "• Floating Overlay HUD enables the live performance HUD handle & side panel over any game.\n\n" +
                        "• Modifying System Settings allows direct control of brightness, AutoLight, Auto-Rotate, and pointer speed.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    granted: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val bannerBg by animateColorAsState(
        targetValue = if (granted) EmeraldActive else Color(0xFF0E1320),
        animationSpec = tween(250),
        label = "perm_banner_bg"
    )
    Column(
        modifier = Modifier
            .testTag(testTag)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CyberSlate)
            .border(1.dp, if (granted) EmeraldActive.copy(alpha = 0.65f) else CyberBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(bannerBg)
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.widthIn(min = 24.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Icon(
                imageVector = if (granted) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = if (granted) "Granted" else "Grant permission",
                tint = Color.White
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}
