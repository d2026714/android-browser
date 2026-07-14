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

@Composable
fun NavigationBar(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val currentUrl by viewModel.currentUrl.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val canGoBack by viewModel.canGoBack.collectAsState()
    val canGoForward by viewModel.canGoForward.collectAsState()
    val isBookmarked by viewModel.isBookmarked.collectAsState()
    val tabs by viewModel.tabs.collectAsState()

    var urlText by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // Sync URL text with current URL when not editing
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
            // Security icon
            if (currentUrl.startsWith("https://")) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Secure",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp).padding(end = 4.dp)
                )
            }

            // URL input
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
                        if (focusState.isFocused) {
                            // Select all text when focused
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Bookmark button
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
                onClick = { /* Handled by WebView in MainScreen */ },
                enabled = canGoBack,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(22.dp)
                )
            }

            // Forward
            IconButton(
                onClick = { /* Handled by WebView in MainScreen */ },
                enabled = canGoForward,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Forward",
                    modifier = Modifier.size(22.dp)
                )
            }

            // Reload/Stop
            IconButton(
                onClick = { /* Handled by WebView in MainScreen */ },
                modifier = Modifier.size(40.dp)
            ) {
                AnimatedVisibility(visible = isLoading, enter = fadeIn(), exit = fadeOut()) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Stop",
                        modifier = Modifier.size(22.dp)
                    )
                }
                AnimatedVisibility(visible = !isLoading, enter = fadeIn(), exit = fadeOut()) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reload",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Home
            IconButton(
                onClick = { viewModel.navigateTo("about:blank") },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home",
                    modifier = Modifier.size(22.dp)
                )
            }

            // Tabs
            BadgedBox(
                badge = {
                    if (tabs.size > 1) {
                        Badge {
                            Text("${tabs.size}")
                        }
                    }
                }
            ) {
                IconButton(
                    onClick = { viewModel.toggleTabs() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tab,
                        contentDescription = "Tabs",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Menu
            IconButton(
                onClick = { viewModel.toggleSettings() },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Menu",
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
