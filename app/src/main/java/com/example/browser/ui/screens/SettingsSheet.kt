package com.example.browser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.browser.ui.viewmodel.BrowserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(viewModel: BrowserViewModel, onDismiss: () -> Unit) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isAmoledDark by viewModel.isAmoledDark.collectAsState()
    val isAdBlockEnabled by viewModel.isAdBlockEnabled.collectAsState()
    val isDesktopMode by viewModel.isDesktopMode.collectAsState()
    val isSearchSuggestions by viewModel.isSearchSuggestions.collectAsState()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text("Settings", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))

            // Appearance
            Text("Appearance", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
            ListItem(headlineContent = { Text("Dark Mode") }, supportingContent = { Text("Light/dark theme") },
                leadingContent = { Icon(if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode, null) },
                trailingContent = { Switch(checked = isDarkMode, onCheckedChange = { viewModel.toggleDarkMode() }) })
            ListItem(headlineContent = { Text("AMOLED Black") }, supportingContent = { Text("Pure black for OLED screens") },
                leadingContent = { Icon(Icons.Default.Contrast, null) },
                trailingContent = { Switch(checked = isAmoledDark, onCheckedChange = { viewModel.toggleAmoledDark() }) })
            ListItem(headlineContent = { Text("Desktop Mode") }, supportingContent = { Text("Request desktop sites") },
                leadingContent = { Icon(if (isDesktopMode) Icons.Default.Computer else Icons.Default.PhoneAndroid, null) },
                trailingContent = { Switch(checked = isDesktopMode, onCheckedChange = { viewModel.toggleDesktopMode() }) })

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Privacy
            Text("Privacy & Security", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
            ListItem(headlineContent = { Text("Ad Blocker") }, supportingContent = { Text("Block ads and trackers") },
                leadingContent = { Icon(Icons.Default.Shield, null, tint = if (isAdBlockEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) },
                trailingContent = { Switch(checked = isAdBlockEnabled, onCheckedChange = { viewModel.toggleAdBlock() }) })
            ListItem(headlineContent = { Text("Search Suggestions") }, supportingContent = { Text("Show suggestions while typing") },
                leadingContent = { Icon(Icons.Default.Lightbulb, null) },
                trailingContent = { Switch(checked = isSearchSuggestions, onCheckedChange = { viewModel.toggleSearchSuggestions() }) })

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Navigation
            Text("Navigation", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
            ListItem(headlineContent = { Text("Search Engine") }, supportingContent = { Text("Choose default search engine") },
                leadingContent = { Icon(Icons.Default.Search, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })
            ListItem(headlineContent = { Text("Quick Links") }, supportingContent = { Text("Edit home page shortcuts") },
                leadingContent = { Icon(Icons.Default.GridView, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })
            ListItem(headlineContent = { Text("Tab Groups") }, supportingContent = { Text("Organize tabs into groups") },
                leadingContent = { Icon(Icons.Default.FolderOpen, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Data
            Text("Data", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
            ListItem(headlineContent = { Text("Bookmarks") }, leadingContent = { Icon(Icons.Default.Bookmarks, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })
            ListItem(headlineContent = { Text("History") }, leadingContent = { Icon(Icons.Default.History, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })
            ListItem(headlineContent = { Text("Reading List") }, leadingContent = { Icon(Icons.Default.MenuBook, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })
            ListItem(headlineContent = { Text("Downloads") }, leadingContent = { Icon(Icons.Default.Download, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Actions
            ListItem(headlineContent = { Text("New Tab") }, leadingContent = { Icon(Icons.Default.Add, null) })
            ListItem(headlineContent = { Text("New Incognito Tab") }, leadingContent = { Icon(Icons.Default.VisibilityOff, null) })

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            ListItem(headlineContent = { Text("About") }, supportingContent = { Text("Android Browser v1.3.0") }, leadingContent = { Icon(Icons.Default.Info, null) })

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
