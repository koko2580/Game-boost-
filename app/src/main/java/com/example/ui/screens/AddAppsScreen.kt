package com.example.ui.screens

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.InstalledAppInfo
import com.example.ui.components.CyberToggleSwitch
import com.example.ui.components.TurboXTopBar
import com.example.ui.components.rememberTurboFlingBehavior
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberElevated
import com.example.ui.theme.CyberSlate
import com.example.ui.theme.LocalTurboAccent
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AddAppsScreen(
    apps: List<InstalledAppInfo>,
    addedPackages: Set<String>,
    isLoading: Boolean,
    peekAppIcon: (String) -> ImageBitmap?,
    loadAppIconAsync: suspend (String) -> ImageBitmap?,
    onToggleApp: (String) -> Unit,
    onBack: () -> Unit
) {
    val accent = LocalTurboAccent.current.accent
    var searchQuery by remember { mutableStateOf("") }
    var showAddedOnly by remember { mutableStateOf(false) }

    val filteredApps = remember(apps, addedPackages, searchQuery, showAddedOnly) {
        apps.filter { app ->
            val matchesQuery = searchQuery.isBlank() ||
                app.appName.contains(searchQuery, ignoreCase = true) ||
                app.packageName.contains(searchQuery, ignoreCase = true)
            val matchesAdded = !showAddedOnly || app.packageName in addedPackages
            matchesQuery && matchesAdded
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid)
            .safeDrawingPadding()
    ) {
        val isWide = maxWidth >= 600.dp

        Column(modifier = Modifier.fillMaxSize()) {
            TurboXTopBar(
                title = "Game Library",
                subtitle = "${addedPackages.size} Selected for Turbo X",
                onBack = onBack,
                backTestTag = "add_apps_back_btn"
            ) {
                FilterChip(
                    selected = showAddedOnly,
                    onClick = { showAddedOnly = !showAddedOnly },
                    label = { Text("Added (${addedPackages.size})") }
                )
            }

            // Search Box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search installed apps or games...", color = TextMuted) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = TextSecondary) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CyberSlate,
                    unfocusedContainerColor = CyberSlate,
                    focusedBorderColor = accent,
                    unfocusedBorderColor = CyberBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("add_apps_search")
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accent)
                }
            } else if (filteredApps.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No matching apps found on device.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextMuted
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = if (isWide) GridCells.Adaptive(minSize = 310.dp) else GridCells.Fixed(1),
                    flingBehavior = rememberTurboFlingBehavior(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        val isAdded = app.packageName in addedPackages
                        val iconBitmap by produceState<ImageBitmap?>(
                            initialValue = peekAppIcon(app.packageName),
                            key1 = app.packageName
                        ) {
                            if (value == null) {
                                value = loadAppIconAsync(app.packageName)
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isAdded) accent.copy(alpha = 0.12f) else MidnightSurface.copy(alpha = 0.90f)
                                )
                                .border(
                                    1.dp,
                                    if (isAdded) accent.copy(alpha = 0.65f) else CyberBorder.copy(alpha = 0.55f),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { onToggleApp(app.packageName) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val resolvedIcon = iconBitmap
                            if (resolvedIcon != null) {
                                Image(
                                    bitmap = resolvedIcon,
                                    contentDescription = app.appName,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(11.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(CyberElevated),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.SportsEsports, contentDescription = null, tint = TextPrimary)
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Version ${app.versionName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = app.appName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = app.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            CyberToggleSwitch(
                                checked = isAdded,
                                onCheckedChange = { onToggleApp(app.packageName) },
                                testTag = "toggle_app_${app.packageName}"
                            )
                        }
                    }
                }
            }
        }
    }
}
