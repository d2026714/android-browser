package com.example.browser.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.browser.R
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
            Text(stringResource(R.string.settings), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))

            // Appearance
            Text(stringResource(R.string.appearance), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
            ListItem(headlineContent = { Text(stringResource(R.string.wallpaper)) }, supportingContent = { Text(stringResource(R.string.customize_new_tab_background)) }, leadingContent = { Icon(Icons.Default.Wallpaper, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) }, modifier = Modifier.clickable { viewModel.toggleWallpaperPicker() })
            ListItem(headlineContent = { Text(stringResource(R.string.dark_mode)) }, leadingContent = { Icon(if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode, null) }, trailingContent = { Switch(checked = isDarkMode, onCheckedChange = { viewModel.toggleDarkMode() }) })
            ListItem(headlineContent = { Text(stringResource(R.string.amoled_black)) }, supportingContent = { Text(stringResource(R.string.pure_black_for_oled)) }, leadingContent = { Icon(Icons.Default.Contrast, null) }, trailingContent = { Switch(checked = isAmoledDark, onCheckedChange = { viewModel.toggleAmoledDark() }) })
            ListItem(headlineContent = { Text(stringResource(R.string.blue_light_filter)) }, supportingContent = { Text(stringResource(R.string.reduce_eye_strain)) }, leadingContent = { Icon(Icons.Default.NightsStay, null) }, trailingContent = { Switch(checked = isBlueLightFilter, onCheckedChange = { viewModel.toggleBlueLightFilter() }) })
            if (isBlueLightFilter) {
                Slider(value = blueLightIntensity, onValueChange = { viewModel.setBlueLightIntensity(it) }, valueRange = 0.1f..0.8f, modifier = Modifier.padding(horizontal = 16.dp))
            }
            ListItem(headlineContent = { Text(stringResource(R.string.desktop_mode)) }, leadingContent = { Icon(if (isDesktopMode) Icons.Default.Computer else Icons.Default.PhoneAndroid, null) }, trailingContent = { Switch(checked = isDesktopMode, onCheckedChange = { viewModel.toggleDesktopMode() }) })

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Privacy & Security
            Text(stringResource(R.string.privacy_security), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
            ListItem(headlineContent = { Text(stringResource(R.string.ad_blocker)) }, leadingContent = { Icon(Icons.Default.Shield, null, tint = if (isAdBlockEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) }, trailingContent = { Switch(checked = isAdBlockEnabled, onCheckedChange = { viewModel.toggleAdBlock() }) })
            ListItem(headlineContent = { Text(stringResource(R.string.javascript)) }, supportingContent = { Text(stringResource(R.string.enable_disable_js)) }, leadingContent = { Icon(Icons.Default.Code, null) }, trailingContent = { Switch(checked = isJavaScriptEnabled, onCheckedChange = { viewModel.toggleJavaScript() }) })
            ListItem(headlineContent = { Text(stringResource(R.string.data_saver)) }, supportingContent = { Text(stringResource(R.string.block_images_use_cache)) }, leadingContent = { Icon(Icons.Default.DataSaverOn, null) }, trailingContent = { Switch(checked = isDataSaver, onCheckedChange = { viewModel.toggleDataSaver() }) })
            ListItem(headlineContent = { Text(stringResource(R.string.dns_over_https)) }, supportingContent = { Text(stringResource(R.string.encrypt_dns_queries)) }, leadingContent = { Icon(Icons.Default.Dns, null) }, trailingContent = { Switch(checked = isDohEnabled, onCheckedChange = { viewModel.toggleDoh() }) })
            ListItem(headlineContent = { Text(stringResource(R.string.cookie_control)) }, supportingContent = { Text(
                when (cookieMode) {
                    "all" -> stringResource(R.string.accept_all_cookies)
                    "first_party" -> stringResource(R.string.first_party_only)
                    "none" -> stringResource(R.string.block_all_cookies)
                    else -> stringResource(R.string.accept_all_cookies)
                }
            ) }, leadingContent = { Icon(Icons.Default.Cookie, null) })
            // Cookie mode selector
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = cookieMode == "all", onClick = { viewModel.setCookieMode("all") }, label = { Text(stringResource(R.string.all)) })
                FilterChip(selected = cookieMode == "first_party", onClick = { viewModel.setCookieMode("first_party") }, label = { Text(stringResource(R.string.first_party)) })
                FilterChip(selected = cookieMode == "none", onClick = { viewModel.setCookieMode("none") }, label = { Text(stringResource(R.string.none)) })
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Content
            Text(stringResource(R.string.content), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
            ListItem(headlineContent = { Text(stringResource(R.string.search_suggestions)) }, leadingContent = { Icon(Icons.Default.Lightbulb, null) }, trailingContent = { Switch(checked = isSearchSuggestions, onCheckedChange = { viewModel.toggleSearchSuggestions() }) })

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Developer
            Text(stringResource(R.string.developer), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
            ListItem(headlineContent = { Text(stringResource(R.string.developer_tools)) }, leadingContent = { Icon(Icons.Default.Code, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })
            ListItem(headlineContent = { Text(stringResource(R.string.user_scripts)) }, leadingContent = { Icon(Icons.Default.Code, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })
            ListItem(headlineContent = { Text(stringResource(R.string.privacy_report)) }, leadingContent = { Icon(Icons.Default.Security, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })
            ListItem(headlineContent = { Text(stringResource(R.string.traffic_stats)) }, leadingContent = { Icon(Icons.Default.DataUsage, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })
            ListItem(headlineContent = { Text(stringResource(R.string.proxy_settings)) }, leadingContent = { Icon(Icons.Default.VpnKey, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Tools
            Text(stringResource(R.string.tools), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
            ListItem(headlineContent = { Text(stringResource(R.string.search_engine)) }, leadingContent = { Icon(Icons.Default.Search, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })
            ListItem(headlineContent = { Text(stringResource(R.string.quick_links)) }, supportingContent = { Text(stringResource(R.string.edit_home_page_shortcuts)) }, leadingContent = { Icon(Icons.Default.GridView, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })
            ListItem(headlineContent = { Text(stringResource(R.string.user_agent)) }, supportingContent = { Text(stringResource(R.string.change_browser_identity)) }, leadingContent = { Icon(Icons.Default.Person, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })
            ListItem(headlineContent = { Text(stringResource(R.string.custom_css)) }, supportingContent = { Text(stringResource(R.string.inject_custom_styles)) }, leadingContent = { Icon(Icons.Default.Palette, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })
            ListItem(headlineContent = { Text(stringResource(R.string.zoom_control)) }, leadingContent = { Icon(Icons.Default.ZoomIn, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })
            ListItem(headlineContent = { Text(stringResource(R.string.backup_restore)) }, leadingContent = { Icon(Icons.Default.Backup, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })
            ListItem(headlineContent = { Text(stringResource(R.string.offline_translation)) }, supportingContent = { Text(stringResource(R.string.download_language_models)) }, leadingContent = { Icon(Icons.Default.Translate, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) }, modifier = Modifier.clickable { viewModel.toggleTranslationSettings() })

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Data
            Text(stringResource(R.string.data), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
            ListItem(headlineContent = { Text(stringResource(R.string.bookmarks)) }, leadingContent = { Icon(Icons.Default.Bookmarks, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })
            ListItem(headlineContent = { Text(stringResource(R.string.bookmark_folders)) }, leadingContent = { Icon(Icons.Default.Folder, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })
            ListItem(headlineContent = { Text(stringResource(R.string.history)) }, leadingContent = { Icon(Icons.Default.History, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })
            ListItem(headlineContent = { Text(stringResource(R.string.reading_list)) }, leadingContent = { Icon(Icons.Default.MenuBook, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })
            ListItem(headlineContent = { Text(stringResource(R.string.downloads)) }, leadingContent = { Icon(Icons.Default.Download, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })
            ListItem(headlineContent = { Text(stringResource(R.string.tab_groups)) }, leadingContent = { Icon(Icons.Default.FolderOpen, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })

            Divider(modifier = Modifier.padding(vertical = 8.dp))
            ListItem(headlineContent = { Text(stringResource(R.string.new_tab)) }, leadingContent = { Icon(Icons.Default.Add, null) })
            ListItem(headlineContent = { Text(stringResource(R.string.new_incognito)) }, leadingContent = { Icon(Icons.Default.VisibilityOff, null) })
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            ListItem(headlineContent = { Text(stringResource(R.string.about)) }, supportingContent = { Text(stringResource(R.string.about_version)) }, leadingContent = { Icon(Icons.Default.Info, null) })
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
