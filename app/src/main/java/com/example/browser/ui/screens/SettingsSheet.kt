package com.example.browser.ui.screens

import androidx.compose.foundation.clickable
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
    val isBlueLightFilter by viewModel.isBlueLightFilter.collectAsState()
    val isJavaScriptEnabled by viewModel.isJavaScriptEnabled.collectAsState()
    val isDataSaver by viewModel.isDataSaver.collectAsState()
    val blueLightIntensity by viewModel.blueLightIntensity.collectAsState()
    val isDohEnabled by viewModel.isDohEnabled.collectAsState()
    val cookieMode by viewModel.cookieMode.collectAsState()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text("Settings", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))

            // Appearance
            Text("Appearance", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
            ListItem(headlineContent = { Text("Wallpaper") }, supportingContent = { Text("Customize new tab background") }, leadingContent = { Icon(Icons.Default.Wallpaper, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) }, modifier = Modifier.clickable { viewModel.toggleWallpaperPicker() })
            ListItem(headlineContent = { Text("Dark Mode") }, leadingContent = { Icon(if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode, null) }, trailingContent = { Switch(checked = isDarkMode, onCheckedChange = { viewModel.toggleDarkMode() }) })
            ListItem(headlineContent = { Text("AMOLED Black") }, supportingContent = { Text("Pure black for OLED") }, leadingContent = { Icon(Icons.Default.Contrast, null) }, trailingContent = { Switch(checked = isAmoledDark, onCheckedChange = { viewModel.toggleAmoledDark() }) })
            ListItem(headlineContent = { Text("Blue Light Filter") }, supportingContent = { Text("Reduce eye strain at night") }, leadingContent = { Icon(Icons.Default.NightsStay, null) }, trailingContent = { Switch(checked = isBlueLightFilter, onCheckedChange = { viewModel.toggleBlueLightFilter() }) })
            if (isBlueLightFilter) {
                Slider(value = blueLightIntensity, onValueChange = { viewModel.setBlueLightIntensity(it) }, valueRange = 0.1f..0.8f, modifier = Modifier.padding(horizontal = 16.dp))
            }
            ListItem(headlineContent = { Text("Desktop Mode") }, leadingContent = { Icon(if (isDesktopMode) Icons.Default.Computer else Icons.Default.PhoneAndroid, null) }, trailingContent = { Switch(checked = isDesktopMode, onCheckedChange = { viewModel.toggleDesktopMode() }) })

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Privacy & Security
            Text("Privacy & Security", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
            ListItem(headlineContent = { Text("Ad Blocker") }, leadingContent = { Icon(Icons.Default.Shield, null, tint = if (isAdBlockEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) }, trailingContent = { Switch(checked = isAdBlockEnabled, onCheckedChange = { viewModel.toggleAdBlock() }) })
            ListItem(headlineContent = { Text("JavaScript") }, supportingContent = { Text("Enable/disable JS globally") }, leadingContent = { Icon(Icons.Default.Code, null) }, trailingContent = { Switch(checked = isJavaScriptEnabled, onCheckedChange = { viewModel.toggleJavaScript() }) })
            ListItem(headlineContent = { Text("Data Saver") }, supportingContent = { Text("Block images, use cache") }, leadingContent = { Icon(Icons.Default.DataSaverOn, null) }, trailingContent = { Switch(checked = isDataSaver, onCheckedChange = { viewModel.toggleDataSaver() }) })
            ListItem(headlineContent = { Text("DNS over HTTPS") }, supportingContent = { Text("Encrypt DNS queries") }, leadingContent = { Icon(Icons.Default.Dns, null) }, trailingContent = { Switch(checked = isDohEnabled, onCheckedChange = { viewModel.toggleDoh() }) })
            ListItem(headlineContent = { Text("Cookie Control") }, supportingContent = { Text(
                when (cookieMode) {
                    "all" -> "Accept all cookies"
                    "first_party" -> "First-party only"
                    "none" -> "Block all cookies"
                    else -> "Accept all cookies"
                }
            ) }, leadingContent = { Icon(Icons.Default.Cookie, null) })
            // Cookie mode selector
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = cookieMode == "all", onClick = { viewModel.setCookieMode("all") }, label = { Text("All") })
                FilterChip(selected = cookieMode == "first_party", onClick = { viewModel.setCookieMode("first_party") }, label = { Text("1st Party") })
                FilterChip(selected = cookieMode == "none", onClick = { viewModel.setCookieMode("none") }, label = { Text("None") })
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Content
            Text("Content", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
            ListItem(headlineContent = { Text("Search Suggestions") }, leadingContent = { Icon(Icons.Default.Lightbulb, null) }, trailingContent = { Switch(checked = isSearchSuggestions, onCheckedChange = { viewModel.toggleSearchSuggestions() }) })

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Developer
            Text("Developer", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
            ListItem(headlineContent = { Text("Developer Tools") }, leadingContent = { Icon(Icons.Default.Code, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })
            ListItem(headlineContent = { Text("User Scripts") }, leadingContent = { Icon(Icons.Default.Code, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })
            ListItem(headlineContent = { Text("Privacy Report") }, leadingContent = { Icon(Icons.Default.Security, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })
            ListItem(headlineContent = { Text("Traffic Stats") }, leadingContent = { Icon(Icons.Default.DataUsage, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })
            ListItem(headlineContent = { Text("Proxy Settings") }, leadingContent = { Icon(Icons.Default.VpnKey, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Tools
            Text("Tools", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
            ListItem(headlineContent = { Text("Search Engine") }, leadingContent = { Icon(Icons.Default.Search, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })
            ListItem(headlineContent = { Text("Quick Links") }, supportingContent = { Text("Edit home page shortcuts") }, leadingContent = { Icon(Icons.Default.GridView, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })
            ListItem(headlineContent = { Text("User Agent") }, supportingContent = { Text("Change browser identity") }, leadingContent = { Icon(Icons.Default.Person, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })
            ListItem(headlineContent = { Text("Custom CSS") }, supportingContent = { Text("Inject custom styles") }, leadingContent = { Icon(Icons.Default.Palette, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })
            ListItem(headlineContent = { Text("Zoom Control") }, leadingContent = { Icon(Icons.Default.ZoomIn, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })
            ListItem(headlineContent = { Text("Backup & Restore") }, leadingContent = { Icon(Icons.Default.Backup, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Data
            Text("Data", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
            ListItem(headlineContent = { Text("Bookmarks") }, leadingContent = { Icon(Icons.Default.Bookmarks, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })
            ListItem(headlineContent = { Text("Bookmark Folders") }, leadingContent = { Icon(Icons.Default.Folder, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })
            ListItem(headlineContent = { Text("History") }, leadingContent = { Icon(Icons.Default.History, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })
            ListItem(headlineContent = { Text("Reading List") }, leadingContent = { Icon(Icons.Default.MenuBook, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })
            ListItem(headlineContent = { Text("Downloads") }, leadingContent = { Icon(Icons.Default.Download, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })
            ListItem(headlineContent = { Text("Tab Groups") }, leadingContent = { Icon(Icons.Default.FolderOpen, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })

            Divider(modifier = Modifier.padding(vertical = 8.dp))
            ListItem(headlineContent = { Text("New Tab") }, leadingContent = { Icon(Icons.Default.Add, null) })
            ListItem(headlineContent = { Text("New Incognito") }, leadingContent = { Icon(Icons.Default.VisibilityOff, null) })
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            ListItem(headlineContent = { Text("About") }, supportingContent = { Text("Android Browser v2.0.0") }, leadingContent = { Icon(Icons.Default.Info, null) })
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
