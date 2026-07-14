package com.example.browser.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
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

    var urlText by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(currentUrl) {
        if (!isEditing) {
            urlText = if (currentUrl == "about:blank") "" else currentUrl
        }
    }

    Column(modifier = modifier) {
        // URL bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentUrl.startsWith("https://")) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Secure",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp).padding(end = 4.dp)
                )
            } else if (currentUrl.startsWith("http://")) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Not secure",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp).padding(end = 4.dp)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                if (urlText.isEmpty() && !isEditing) {
                    Text(
                        text = "Search or enter URL",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontSize = 15.sp
                    )
                }
                BasicTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            viewModel.navigateTo(urlText)
                            focusManager.clearFocus()
                            isEditing = false
                        }
                    ),
                    onFocusChanged = { focusState ->
                        isEditing = focusState.isFocused
                    }
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = { viewModel.toggleBookmark() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Bookmark",
                    tint = if (isBookmarked) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Bottom toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back
            IconButton(
                onClick = onGoBack,
                enabled = canGoBack,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.ArrowBack, "Back", modifier = Modifier.size(22.dp))
            }

            // Forward
            IconButton(
                onClick = onGoForward,
                enabled = canGoForward,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.ArrowForward, "Forward", modifier = Modifier.size(22.dp))
            }

            // Reload/Stop
            IconButton(
                onClick = { if (isLoading) onStop() else onReload() },
                modifier = Modifier.size(40.dp)
            ) {
                AnimatedVisibility(visible = isLoading, enter = fadeIn(), exit = fadeOut()) {
                    Icon(Icons.Default.Close, "Stop", modifier = Modifier.size(22.dp))
                }
                AnimatedVisibility(visible = !isLoading, enter = fadeIn(), exit = fadeOut()) {
                    Icon(Icons.Default.Refresh, "Reload", modifier = Modifier.size(22.dp))
                }
            }

            // Home
            IconButton(
                onClick = { viewModel.navigateTo("about:blank") },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.Home, "Home", modifier = Modifier.size(22.dp))
            }

            // Tabs
            BadgedBox(
                badge = {
                    if (tabs.size > 1) {
                        Badge { Text("${tabs.size}") }
                    }
                }
            ) {
                IconButton(
                    onClick = { viewModel.toggleTabs() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Tab, "Tabs", modifier = Modifier.size(22.dp))
                }
            }

            // Menu (overflow)
            Box {
                var expanded by remember { mutableStateOf(false) }

                IconButton(
                    onClick = { expanded = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.MoreVert, "Menu", modifier = Modifier.size(22.dp))
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Bookmarks") },
                        onClick = { expanded = false; viewModel.toggleBookmarks() },
                        leadingIcon = { Icon(Icons.Default.Bookmarks, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("History") },
                        onClick = { expanded = false; viewModel.toggleHistory() },
                        leadingIcon = { Icon(Icons.Default.History, null) }
                    )
                    Divider()
                    DropdownMenuItem(
                        text = { Text("Find in Page") },
                        onClick = { expanded = false; viewModel.toggleFindInPage() },
                        leadingIcon = { Icon(Icons.Default.Search, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Reading Mode") },
                        onClick = { expanded = false; viewModel.toggleReadingMode() },
                        leadingIcon = { Icon(Icons.Default.MenuBook, null) }
                    )
                    Divider()
                    DropdownMenuItem(
                        text = {
                            Text(if (isDesktopMode) "Mobile Site" else "Desktop Site")
                        },
                        onClick = { expanded = false; viewModel.toggleDesktopMode() },
                        leadingIcon = {
                            Icon(
                                if (isDesktopMode) Icons.Default.PhoneAndroid else Icons.Default.Computer,
                                null
                            )
                        }
                    )
                    Divider()
                    DropdownMenuItem(
                        text = { Text("New Tab") },
                        onClick = { expanded = false; viewModel.addTab() },
                        leadingIcon = { Icon(Icons.Default.Add, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("New Incognito") },
                        onClick = { expanded = false; viewModel.addTab(incognito = true) },
                        leadingIcon = { Icon(Icons.Default.VisibilityOff, null) }
                    )
                    Divider()
                    DropdownMenuItem(
                        text = { Text("Settings") },
                        onClick = { expanded = false; viewModel.toggleSettings() },
                        leadingIcon = { Icon(Icons.Default.Settings, null) }
                    )
                }
            }
        }
    }
}
