package com.example.browser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.browser.ui.viewmodel.BrowserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    viewModel: BrowserViewModel,
    onDismiss: () -> Unit
) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isAdBlockEnabled by viewModel.isAdBlockEnabled.collectAsState()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Dark Mode
            ListItem(
                headlineContent = { Text("Dark Mode") },
                supportingContent = { Text("Switch between light and dark theme") },
                leadingContent = {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = null
                    )
                },
                trailingContent = {
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { viewModel.toggleDarkMode() }
                    )
                }
            )

            // Ad Blocker
            ListItem(
                headlineContent = { Text("Ad Blocker") },
                supportingContent = { Text("Block ads and trackers") },
                leadingContent = {
                    Icon(
                        imageVector = if (isAdBlockEnabled) Icons.Default.Shield else Icons.Default.ShieldOutlined,
                        contentDescription = null
                    )
                },
                trailingContent = {
                    Switch(
                        checked = isAdBlockEnabled,
                        onCheckedChange = { viewModel.toggleAdBlock() }
                    )
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Bookmarks
            ListItem(
                headlineContent = { Text("Bookmarks") },
                leadingContent = {
                    Icon(imageVector = Icons.Default.Bookmarks, contentDescription = null)
                },
                modifier = Modifier.let { mod ->
                    mod
                },
                trailingContent = {
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null)
                }
            )

            // History
            ListItem(
                headlineContent = { Text("History") },
                leadingContent = {
                    Icon(imageVector = Icons.Default.History, contentDescription = null)
                },
                trailingContent = {
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null)
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // New Tab
            ListItem(
                headlineContent = { Text("New Tab") },
                leadingContent = {
                    Icon(imageVector = Icons.Default.Tab, contentDescription = null)
                },
                modifier = Modifier.let { mod ->
                    mod
                }
            )

            // New Incognito Tab
            ListItem(
                headlineContent = { Text("New Incognito Tab") },
                leadingContent = {
                    Icon(imageVector = Icons.Default.VisibilityOff, contentDescription = null)
                },
                modifier = Modifier.let { mod ->
                    mod
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // About
            ListItem(
                headlineContent = { Text("About") },
                supportingContent = { Text("Android Browser v1.0.0") },
                leadingContent = {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null)
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
