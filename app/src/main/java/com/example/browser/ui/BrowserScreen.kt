package com.example.browser.ui

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.browser.ui.components.BottomTabBar
import com.example.browser.ui.components.ErrorPage
import com.example.browser.ui.components.FindInPageBar
import com.example.browser.ui.components.TopNavBar
import com.example.browser.ui.components.WebViewContent

@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    onNavigateToBookmarks: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val tabs by viewModel.tabs.collectAsState()
    val activeTabIndex by viewModel.activeTabIndex.collectAsState()
    val showHome by viewModel.showHome.collectAsState()
    val showFindBar by viewModel.showFindBar.collectAsState()
    val adBlockEnabled by viewModel.adBlockEnabled.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val context = LocalContext.current

    if (showHome) {
        HomeScreen(
            viewModel = viewModel,
            onNavigateToBookmarks = onNavigateToBookmarks,
            onNavigateToHistory = onNavigateToHistory,
            onNavigateToSettings = onNavigateToSettings,
        )
    } else {
        val activeTab = tabs.getOrNull(activeTabIndex)

        Column(modifier = Modifier.fillMaxSize()) {
            // Top navigation bar
            TopNavBar(
                url = activeTab?.url ?: "",
                isLoading = activeTab?.isLoading ?: false,
                progress = activeTab?.progress ?: 0,
                canGoBack = activeTab?.canGoBack ?: false,
                canGoForward = activeTab?.canGoForward ?: false,
                onBack = { viewModel.goBack() },
                onForward = { viewModel.goForward() },
                onReload = { viewModel.reload() },
                onStop = { viewModel.stopLoading() },
                onShare = {
                    val url = activeTab?.url ?: return@TopNavBar
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        putExtra(Intent.EXTRA_TEXT, url)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(intent, "分享页面"))
                },
                onBookmark = { viewModel.toggleBookmark() },
                onFind = { viewModel.showFind() },
                onMenu = { onNavigateToSettings() },
            )

            // WebView content or error page
            Box(modifier = Modifier.weight(1f)) {
                if (activeTab?.hasError == true) {
                    ErrorPage(
                        errorMessage = "无法加载页面",
                        onRetry = { viewModel.reload() },
                    )
                } else {
                    WebViewContent(
                        tabs = tabs,
                        activeTabIndex = activeTabIndex,
                        viewModel = viewModel,
                        adBlockEnabled = adBlockEnabled,
                        fontSize = fontSize,
                        onError = { _, _ -> },
                    )
                }
            }

            // Find in page bar
            FindInPageBar(
                visible = showFindBar,
                onSearch = { query -> viewModel.findAll(query) },
                onNext = { viewModel.findNext() },
                onPrevious = { viewModel.findPrevious() },
                onClose = {
                    viewModel.clearFindMatches()
                    viewModel.hideFind()
                },
            )

            // Bottom tab bar
            BottomTabBar(
                tabs = tabs,
                activeTabIndex = activeTabIndex,
                onTabClick = { viewModel.switchTab(it) },
                onAddTab = { viewModel.addTab() },
                onCloseTab = { viewModel.closeTab(it) },
                onHome = { viewModel.goHome() },
            )
        }
    }
}
