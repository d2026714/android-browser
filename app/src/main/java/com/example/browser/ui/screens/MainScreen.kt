package com.example.browser.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.browser.ui.components.BlueLightFilterOverlay
import com.example.browser.ui.components.BrowserWebView
import com.example.browser.ui.components.FindInPageBar
import com.example.browser.ui.components.NavigationBar
import com.example.browser.ui.viewmodel.BrowserViewModel

@Composable
fun MainScreen(viewModel: BrowserViewModel = viewModel()) {
    val currentUrl by viewModel.currentUrl.collectAsState()
    val showBookmarks by viewModel.showBookmarks.collectAsState()
    val showHistory by viewModel.showHistory.collectAsState()
    val showTabs by viewModel.showTabs.collectAsState()
    val showSettings by viewModel.showSettings.collectAsState()
    val isFindInPage by viewModel.isFindInPage.collectAsState()
    val isReadingMode by viewModel.isReadingMode.collectAsState()
    val showSearchEngineSheet by viewModel.showSearchEngineSheet.collectAsState()
    val showDownloads by viewModel.showDownloads.collectAsState()
    val showViewSource by viewModel.showViewSource.collectAsState()
    val showReadingList by viewModel.showReadingList.collectAsState()
    val showTabGroups by viewModel.showTabGroups.collectAsState()
    val showQuickLinksEditor by viewModel.showQuickLinksEditor.collectAsState()
    val showQrCode by viewModel.showQrCode.collectAsState()
    val showPageInfo by viewModel.showPageInfo.collectAsState()
    val showBackupRestore by viewModel.showBackupRestore.collectAsState()
    val showUserAgent by viewModel.showUserAgent.collectAsState()
    val showZoomControl by viewModel.showZoomControl.collectAsState()
    val showCustomCss by viewModel.showCustomCss.collectAsState()
    val isBlueLightFilter by viewModel.isBlueLightFilter.collectAsState()
    val blueLightIntensity by viewModel.blueLightIntensity.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(visible = isFindInPage, enter = slideInVertically { -it }, exit = slideOutVertically { -it }) {
                FindInPageBar(onQueryChanged = { viewModel.findInPage(it) }, onFindNext = { viewModel.findNext() }, onFindPrevious = { viewModel.findPrevious() }, onClose = { viewModel.clearFindInPage() })
            }
            Box(modifier = Modifier.weight(1f)) {
                if (currentUrl.isBlank() || currentUrl == "about:blank") HomeScreen(viewModel = viewModel)
                else BrowserWebView(viewModel = viewModel, modifier = Modifier.fillMaxSize())
            }
            NavigationBar(viewModel = viewModel, onGoBack = { viewModel.goBack() }, onGoForward = { viewModel.goForward() }, onReload = { viewModel.reload() }, onStop = { viewModel.stopLoading() }, modifier = Modifier.fillMaxWidth())
        }

        // Blue light filter overlay
        BlueLightFilterOverlay(intensity = blueLightIntensity, enabled = isBlueLightFilter)
    }

    // All bottom sheets
    if (showBookmarks) BookmarksSheet(viewModel = viewModel, onDismiss = { viewModel.toggleBookmarks() })
    if (showHistory) HistorySheet(viewModel = viewModel, onDismiss = { viewModel.toggleHistory() })
    if (showTabs) TabsSheet(viewModel = viewModel, onDismiss = { viewModel.toggleTabs() })
    if (showSettings) SettingsSheet(viewModel = viewModel, onDismiss = { viewModel.toggleSettings() })
    if (showSearchEngineSheet) SearchEngineSheet(viewModel = viewModel, onDismiss = { viewModel.toggleSearchEngineSheet() })
    if (showDownloads) DownloadsSheet(onDismiss = { viewModel.toggleDownloads() })
    if (showReadingList) ReadingListSheet(viewModel = viewModel, onDismiss = { viewModel.toggleReadingList() })
    if (showTabGroups) TabGroupsSheet(viewModel = viewModel, onDismiss = { viewModel.toggleTabGroups() })
    if (showQuickLinksEditor) QuickLinksEditorSheet(viewModel = viewModel, onDismiss = { viewModel.toggleQuickLinksEditor() })
    if (showQrCode) QrCodeSheet(viewModel = viewModel, onDismiss = { viewModel.toggleQrCode() })
    if (showPageInfo) PageInfoSheet(viewModel = viewModel, onDismiss = { viewModel.togglePageInfo() })
    if (showBackupRestore) BackupRestoreSheet(onDismiss = { viewModel.toggleBackupRestore() })
    if (showUserAgent) UserAgentSheet(viewModel = viewModel, onDismiss = { viewModel.toggleUserAgent() })
    if (showZoomControl) ZoomControlSheet(viewModel = viewModel, onDismiss = { viewModel.toggleZoomControl() })
    if (showCustomCss) CustomCssSheet(viewModel = viewModel, onDismiss = { viewModel.toggleCustomCssSheet() })
    if (isReadingMode) ReadingModeScreen(viewModel = viewModel, onDismiss = { viewModel.toggleReadingMode() })
    if (showViewSource) ViewSourceScreen(viewModel = viewModel, onDismiss = { viewModel.closeViewSource() })
}
