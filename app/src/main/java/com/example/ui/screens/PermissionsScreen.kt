package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.ui.components.TurboTopLogoCorner
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
        // Header matching Screenshot 3
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
                    modifier = Modifier.testTag("permissions_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Text(
                    text = "Permission",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }

            Box(
                modifier = Modifier
                    .testTag("permissions_help_btn")
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberElevated)
                    .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
                    .clickable { showHelpDialog = true }
                    .padding(horizontal = 18.dp, vertical = 10.dp)
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
                    description = "Click to allow Do Not Disturb permission to access the Do Not Disturb feature.",
                    granted = permissions.dndGranted,
                    onClick = onRequestDnd,
                    testTag = "perm_dnd_card"
                )

                PermissionCard(
                    title = "Allow floating windows",
                    description = "Click to allow floating window permissions to access special GameTurbo features.",
                    granted = permissions.overlayGranted,
                    onClick = onRequestOverlay,
                    testTag = "perm_overlay_card"
                )

                PermissionCard(
                    title = "Modifying system settings",
                    description = "Click to allow editing of system settings to access additional functions of GameTurbo.",
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
                    text = "• Do Not Disturb lets Game Turbo silence alerts automatically while gaming.\n\n" +
                        "• Allow floating windows enables the live Game Turbo HUD handle & side panel over any game.\n\n" +
                        "• Modifying system settings allows quick control of screen brightness, AutoLight, and Auto-Rotate from the HUD.",
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
    Column(
        modifier = Modifier
            .testTag(testTag)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CyberSlate)
            .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (granted) EmeraldActive else Color(0xFF0E1019))
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
