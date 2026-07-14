package com.example.browser.ui.screens

import android.webkit.WebView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.browser.ui.components.BrowserWebView
import com.example.browser.ui.components.NavigationBar
import com.example.browser.ui.viewmodel.BrowserViewModel

@Composable
fun MainScreen(
    viewModel: BrowserViewModel = viewModel()
) {
    val currentUrl by viewModel.currentUrl.collectAsState()
    val showBookmarks by viewModel.showBookmarks.collectAsState()
    val showHistory by viewModel.showHistory.collectAsState()
    val showTabs by viewModel.showTabs.collectAsState()
    val showSettings by viewModel.showSettings.collectAsState()

    // WebView reference for navigation
    var webView by remember { mutableStateOf<WebView?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // WebView takes most of the screen
        Box(modifier = Modifier.weight(1f)) {
            if (currentUrl.isBlank() || currentUrl == "about:blank") {
                HomeScreen(viewModel = viewModel)
            } else {
                BrowserWebView(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Navigation bar at the bottom
        NavigationBar(
            viewModel = viewModel,
            modifier = Modifier.fillMaxWidth()
        )
    }

    // Bottom sheets
    if (showBookmarks) {
        BookmarksSheet(
            viewModel = viewModel,
            onDismiss = { viewModel.toggleBookmarks() }
        )
    }

    if (showHistory) {
        HistorySheet(
            viewModel = viewModel,
            onDismiss = { viewModel.toggleHistory() }
        )
    }

    if (showTabs) {
        TabsSheet(
            viewModel = viewModel,
            onDismiss = { viewModel.toggleTabs() }
        )
    }

    if (showSettings) {
        SettingsSheet(
            viewModel = viewModel,
            onDismiss = { viewModel.toggleSettings() }
        )
    }
}
