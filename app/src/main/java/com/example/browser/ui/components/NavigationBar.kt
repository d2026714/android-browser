package com.example.browser.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.browser.ui.viewmodel.BrowserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationBar(
    viewModel: BrowserViewModel,
    onGoBack: () -> Unit,
    onGoForward: () -> Unit,
    onReload: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUrl by viewModel.currentUrl.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val canGoBack by viewModel.canGoBack.collectAsState()
    val canGoForward by viewModel.canGoForward.collectAsState()
    val isBookmarked by viewModel.isBookmarked.collectAsState()
    val tabs by viewModel.tabs.collectAsState()
    val isDesktopMode by viewModel.isDesktopMode.collectAsState()
    val searchSuggestions by viewModel.searchSuggestions.collectAsState()
    val isSearchSuggestionsEnabled by viewModel.isSearchSuggestions.collectAsState()

    var urlText by remember { mutableStateOf("") }
    var isUrlFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(currentUrl) {
        if (!isUrlFocused) {
            urlText = if (currentUrl == "about:blank") "" else currentUrl
        }
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentUrl.startsWith("https://")) {
                Icon(
                    Icons.Default.Lock, "Secure",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp).padding(end = 4.dp)
                )
            } else if (currentUrl.startsWith("http://")) {
                Icon(
                    Icons.Default.Lock, "Not secure",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp).padding(end = 4.dp)
                )
            }
            TextField(
                value = urlText,
                onValueChange = {
                    urlText = it
                    if (isSearchSuggestionsEnabled && isUrlFocused) {
                        viewModel.fetchSearchSuggestions(it)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp)),
                placeholder = { Text("Search or enter URL", fontSize = 15.sp) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions(onGo = {
                    viewModel.navigateTo(urlText)
                    focusManager.clearFocus()
                    viewModel.clearSearchSuggestions()
                })
            )
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(
                onClick = { viewModel.toggleBookmark() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    if (isBookmarked) Icons.Default.Star else Icons.Default.StarBorder,
                    "Bookmark",
                    tint = if (isBookmarked) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Search suggestions dropdown
        if (isUrlFocused && searchSuggestions.isNotEmpty() && isSearchSuggestionsEnabled) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column {
                    searchSuggestions.forEach { suggestion ->
                        ListItem(
                            headlineContent = { Text(suggestion, fontSize = 14.sp) },
                            leadingContent = {
                                Icon(
                                    Icons.Default.Search, null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            },
                            modifier = Modifier.clickable {
                                urlText = suggestion
                                viewModel.navigateTo(suggestion)
                                focusManager.clearFocus()
                                viewModel.clearSearchSuggestions()
                            }
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onGoBack, enabled = canGoBack, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Go back", modifier = Modifier.size(22.dp))
            }
            IconButton(onClick = onGoForward, enabled = canGoForward, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Go forward", modifier = Modifier.size(22.dp))
            }
            IconButton(onClick = { if (isLoading) onStop() else onReload() }, modifier = Modifier.size(40.dp)) {
                AnimatedVisibility(visible = isLoading, enter = fadeIn(), exit = fadeOut()) {
                    Icon(Icons.Default.Close, contentDescription = "Stop loading", modifier = Modifier.size(22.dp))
                }
                AnimatedVisibility(visible = !isLoading, enter = fadeIn(), exit = fadeOut()) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reload page", modifier = Modifier.size(22.dp))
                }
            }
            IconButton(onClick = { viewModel.navigateTo("about:blank") }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Home, contentDescription = "Home", modifier = Modifier.size(22.dp))
            }
            BadgedBox(badge = { if (tabs.size > 1) Badge { Text("${tabs.size}") } }) {
                IconButton(onClick = { viewModel.toggleTabs() }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Tab, contentDescription = "Tabs", modifier = Modifier.size(22.dp))
                }
            }
            Box {
                var expanded by remember { mutableStateOf(false) }
                IconButton(onClick = { expanded = true }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu", modifier = Modifier.size(22.dp))
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text("Bookmarks") }, onClick = { expanded = false; viewModel.toggleBookmarks() }, leadingIcon = { Icon(Icons.Default.Bookmarks, null) })
                    DropdownMenuItem(text = { Text("Bookmark Folders") }, onClick = { expanded = false; viewModel.toggleBookmarkFolders() }, leadingIcon = { Icon(Icons.Default.Folder, null) })
                    DropdownMenuItem(text = { Text("History") }, onClick = { expanded = false; viewModel.toggleHistory() }, leadingIcon = { Icon(Icons.Default.History, null) })
                    DropdownMenuItem(text = { Text("Reading List") }, onClick = { expanded = false; viewModel.toggleReadingList() }, leadingIcon = { Icon(Icons.Default.MenuBook, null) })
                    DropdownMenuItem(text = { Text("Downloads") }, onClick = { expanded = false; viewModel.toggleDownloads() }, leadingIcon = { Icon(Icons.Default.Download, null) })
                    Divider(modifier = Modifier)
                    DropdownMenuItem(text = { Text("Find in Page") }, onClick = { expanded = false; viewModel.toggleFindInPage() }, leadingIcon = { Icon(Icons.Default.Search, null) })
                    DropdownMenuItem(text = { Text("Reading Mode") }, onClick = { expanded = false; viewModel.toggleReadingMode() }, leadingIcon = { Icon(Icons.Default.AutoStories, null) })
                    DropdownMenuItem(text = { Text("View Source") }, onClick = { expanded = false; viewModel.viewPageSource() }, leadingIcon = { Icon(Icons.Default.Code, null) })
                    DropdownMenuItem(text = { Text("Translate") }, onClick = { expanded = false; viewModel.translatePage() }, leadingIcon = { Icon(Icons.Default.Translate, null) })
                    DropdownMenuItem(text = { Text("Translate (Offline)") }, onClick = { expanded = false; viewModel.translatePageOffline() }, leadingIcon = { Icon(Icons.Default.GTranslate, null) })
                    DropdownMenuItem(text = { Text("Translate Text") }, onClick = { expanded = false; viewModel.openTranslate() }, leadingIcon = { Icon(Icons.Default.Translate, null) })
                    DropdownMenuItem(text = { Text("Translation Settings") }, onClick = { expanded = false; viewModel.toggleTranslationSettings() }, leadingIcon = { Icon(Icons.Default.Settings, null) })
                    DropdownMenuItem(text = { Text("Page Info") }, onClick = { expanded = false; viewModel.togglePageInfo() }, leadingIcon = { Icon(Icons.Default.Info, null) })
                    Divider(modifier = Modifier)
                    DropdownMenuItem(text = { Text("Page Notes") }, onClick = { expanded = false; viewModel.toggleNoteEditor() }, leadingIcon = { Icon(Icons.Default.EditNote, null) })
                    DropdownMenuItem(text = { Text("All Notes") }, onClick = { expanded = false; viewModel.toggleNotesList() }, leadingIcon = { Icon(Icons.Default.StickyNote2, null) })
                    Divider(modifier = Modifier)
                    DropdownMenuItem(text = { Text(if (isDesktopMode) "Mobile Site" else "Desktop Site") }, onClick = { expanded = false; viewModel.toggleDesktopMode() }, leadingIcon = { Icon(if (isDesktopMode) Icons.Default.PhoneAndroid else Icons.Default.Computer, null) })
                    DropdownMenuItem(text = { Text("Zoom") }, onClick = { expanded = false; viewModel.toggleZoomControl() }, leadingIcon = { Icon(Icons.Default.ZoomIn, null) })
                    DropdownMenuItem(text = { Text("User Agent") }, onClick = { expanded = false; viewModel.toggleUserAgent() }, leadingIcon = { Icon(Icons.Default.Person, null) })
                    DropdownMenuItem(text = { Text("Custom CSS") }, onClick = { expanded = false; viewModel.toggleCustomCssSheet() }, leadingIcon = { Icon(Icons.Default.Palette, null) })
                    Divider(modifier = Modifier)
                    DropdownMenuItem(text = { Text("QR Code") }, onClick = { expanded = false; viewModel.toggleQrCode() }, leadingIcon = { Icon(Icons.Default.QrCode, null) })
                    DropdownMenuItem(text = { Text("Screenshot") }, onClick = { expanded = false; viewModel.takeScreenshot() }, leadingIcon = { Icon(Icons.Default.CameraAlt, null) })
                    DropdownMenuItem(text = { Text("Share") }, onClick = { expanded = false; viewModel.shareCurrentPage() }, leadingIcon = { Icon(Icons.Default.Share, null) })
                    DropdownMenuItem(text = { Text("Print") }, onClick = { expanded = false; viewModel.printPage() }, leadingIcon = { Icon(Icons.Default.Print, null) })
                    Divider(modifier = Modifier)
                    DropdownMenuItem(text = { Text("New Tab") }, onClick = { expanded = false; viewModel.addTab() }, leadingIcon = { Icon(Icons.Default.Add, null) })
                    DropdownMenuItem(text = { Text("New Incognito") }, onClick = { expanded = false; viewModel.addTab(incognito = true) }, leadingIcon = { Icon(Icons.Default.VisibilityOff, null) })
                    DropdownMenuItem(text = { Text("Tab Groups") }, onClick = { expanded = false; viewModel.toggleTabGroups() }, leadingIcon = { Icon(Icons.Default.FolderOpen, null) })
                    DropdownMenuItem(text = { Text("Save to Reading List") }, onClick = { expanded = false; viewModel.addToReadingList() }, leadingIcon = { Icon(Icons.Default.BookmarkAdd, null) })
                    DropdownMenuItem(text = { Text("Dev Tools") }, onClick = { expanded = false; viewModel.toggleDevTools() }, leadingIcon = { Icon(Icons.Default.Code, null) })
                    DropdownMenuItem(text = { Text("Passwords") }, onClick = { expanded = false; viewModel.togglePasswordSheet() }, leadingIcon = { Icon(Icons.Default.Lock, null) })
                    DropdownMenuItem(text = { Text("Proxy") }, onClick = { expanded = false; viewModel.toggleProxySheet() }, leadingIcon = { Icon(Icons.Default.VpnKey, null) })
                    DropdownMenuItem(text = { Text("Privacy Report") }, onClick = { expanded = false; viewModel.togglePrivacyReport() }, leadingIcon = { Icon(Icons.Default.Security, null) })
                    DropdownMenuItem(text = { Text("Traffic Stats") }, onClick = { expanded = false; viewModel.toggleTrafficStats() }, leadingIcon = { Icon(Icons.Default.DataUsage, null) })
                    DropdownMenuItem(text = { Text("User Scripts") }, onClick = { expanded = false; viewModel.toggleUserScripts() }, leadingIcon = { Icon(Icons.Default.Code, null) })
                    DropdownMenuItem(text = { Text("Offline Pages") }, onClick = { expanded = false; viewModel.toggleOfflinePages() }, leadingIcon = { Icon(Icons.Default.Save, null) })
                    Divider(modifier = Modifier)
                    DropdownMenuItem(text = { Text("Backup & Restore") }, onClick = { expanded = false; viewModel.toggleBackupRestore() }, leadingIcon = { Icon(Icons.Default.Backup, null) })
                    DropdownMenuItem(text = { Text("Settings") }, onClick = { expanded = false; viewModel.toggleSettings() }, leadingIcon = { Icon(Icons.Default.Settings, null) })
                }
            }
        }
    }
}
