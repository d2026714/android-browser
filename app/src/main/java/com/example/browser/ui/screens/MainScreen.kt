package com.example.browser.ui.screens

import android.content.Intent
import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.browser.ui.components.BrowserWebView
import com.example.browser.ui.components.FindInPageBar
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
    val isFindInPage by viewModel.isFindInPage.collectAsState()
    val isReadingMode by viewModel.isReadingMode.collectAsState()
    val showSearchEngineSheet by viewModel.showSearchEngineSheet.collectAsState()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Find in page bar (animated)
        AnimatedVisibility(
            visible = isFindInPage,
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it }
        ) {
            FindInPageBar(
                onQueryChanged = { viewModel.findInPage(it) },
                onFindNext = { viewModel.findNext() },
                onFindPrevious = { viewModel.findPrevious() },
                onClose = { viewModel.clearFindInPage() }
            )
        }

        // WebView or Home Screen
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
            onGoBack = { viewModel.goBack() },
            onGoForward = { viewModel.goForward() },
            onReload = { viewModel.reload() },
            onStop = { viewModel.stopLoading() },
            modifier = Modifier.fillMaxWidth()
        )
    }

    // --- Bottom Sheets ---

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

    if (showSearchEngineSheet) {
        SearchEngineSheet(
            viewModel = viewModel,
            onDismiss = { viewModel.toggleSearchEngineSheet() }
        )
    }

    // Reading Mode (full screen overlay)
    if (isReadingMode) {
        ReadingModeScreen(
            viewModel = viewModel,
            onDismiss = { viewModel.toggleReadingMode() }
        )
    }
}
