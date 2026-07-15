package com.example.browser.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.browser.ui.components.*
import com.example.browser.gecko.GeckoBrowserView
import com.example.browser.player.PlayerScreen
import com.example.browser.ui.viewmodel.BrowserViewModel

@Composable
fun BlueLightFilterOverlay(
    intensity: Float,
    enabled: Boolean
) {
    if (enabled && intensity > 0f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFF8C00).copy(alpha = intensity * 0.3f))
                .zIndex(0.5f)
        )
    }
}

@Composable
fun MainScreen(viewModel: BrowserViewModel) {
    val currentUrl by viewModel.currentUrl.collectAsState()
    val showBookmarks by viewModel.showBookmarks.collectAsState()
    val showHistory by viewModel.showHistory.collectAsState()
    val showTabs by viewModel.showTabs.collectAsState()
    val showSettings by viewModel.showSettings.collectAsState()
    val isFindInPage by viewModel.isFindInPage.collectAsState()
    val isReadingMode by viewModel.isReadingMode.collectAsState()
    val showSearchEngineSheet by viewModel.showSearchEngineSheet.collectAsState()
    val showDownloads by viewModel.showDownloads.collectAsState()
    val showQuickLinksEditor by viewModel.showQuickLinksEditor.collectAsState()
    val showPageInfo by viewModel.showPageInfo.collectAsState()
    val showBackupRestore by viewModel.showBackupRestore.collectAsState()
    val showZoomControl by viewModel.showZoomControl.collectAsState()
    val isBlueLightFilter by viewModel.isBlueLightFilter.collectAsState()
    val blueLightIntensity by viewModel.blueLightIntensity.collectAsState()
    val pageError by viewModel.pageError.collectAsState()
    val longPressUrl by viewModel.longPressUrl.collectAsState()
    val showBookmarkFolders by viewModel.showBookmarkFolders.collectAsState()
    val showWallpaperPicker by viewModel.showWallpaperPicker.collectAsState()
    val showTranslateScreen by viewModel.showTranslateScreen.collectAsState()
    val showTranslationSettings by viewModel.showTranslationSettings.collectAsState()
    val selectedText by viewModel.selectedText.collectAsState()
    val showNoteEditor by viewModel.showNoteEditor.collectAsState()
    val showNotesList by viewModel.showNotesList.collectAsState()
    val showPlayer by viewModel.showPlayer.collectAsState()

    // Full-screen player takes over the entire screen
    if (showPlayer) {
        PlayerScreen(
            manager = viewModel.mediaPlaybackManager,
            onBack = { viewModel.closePlayer() }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
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

            Box(modifier = Modifier.weight(1f)) {
                if (currentUrl.isBlank() || currentUrl == "about:blank") {
                    HomeScreen(viewModel = viewModel)
                } else {
                    GeckoBrowserView(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                    if (pageError != null) {
                        ErrorPage(viewModel = viewModel)
                    }
                }
            }

            // Media prompt bar (shows when media URL detected)
            MediaPromptBar(viewModel = viewModel)

            NavigationBar(
                viewModel = viewModel,
                onGoBack = { viewModel.goBack() },
                onGoForward = { viewModel.goForward() },
                onReload = { viewModel.reload() },
                onStop = { viewModel.stopLoading() },
                modifier = Modifier.fillMaxWidth()
            )
        }

        BlueLightFilterOverlay(intensity = blueLightIntensity, enabled = isBlueLightFilter)
    }

    SslErrorDialog(viewModel = viewModel)

    if (longPressUrl != null) {
        LongPressMenuSheet(viewModel = viewModel)
    }

    if (selectedText != null) {
        TextSelectionMenuSheet(viewModel = viewModel)
    }

    if (showBookmarks) BookmarksSheet(viewModel = viewModel, onDismiss = { viewModel.toggleBookmarks() })
    if (showHistory) HistorySheet(viewModel = viewModel, onDismiss = { viewModel.toggleHistory() })
    if (showTabs) TabsSheet(viewModel = viewModel, onDismiss = { viewModel.toggleTabs() })
    if (showSettings) SettingsSheet(viewModel = viewModel, onDismiss = { viewModel.toggleSettings() })
    if (showSearchEngineSheet) SearchEngineSheet(viewModel = viewModel, onDismiss = { viewModel.toggleSearchEngineSheet() })
    if (showDownloads) DownloadManagerScreen(
        onDismiss = { viewModel.toggleDownloads() },
        downloadManager = viewModel.downloadManager
    )
    if (showQuickLinksEditor) QuickLinksEditorSheet(viewModel = viewModel, onDismiss = { viewModel.toggleQuickLinksEditor() })
    if (showPageInfo) PageInfoSheet(viewModel = viewModel, onDismiss = { viewModel.togglePageInfo() })
    if (showBackupRestore) BackupRestoreSheet(viewModel = viewModel, onDismiss = { viewModel.toggleBackupRestore() })
    if (showZoomControl) ZoomControlSheet(viewModel = viewModel, onDismiss = { viewModel.toggleZoomControl() })
    if (isReadingMode) ReadingModeScreen(viewModel = viewModel, onDismiss = { viewModel.toggleReadingMode() })
    if (showBookmarkFolders) BookmarkFoldersSheet(viewModel = viewModel, onDismiss = { viewModel.toggleBookmarkFolders() })
    if (showWallpaperPicker) WallpaperPickerSheet(viewModel = viewModel, onDismiss = { viewModel.toggleWallpaperPicker() })
    if (showTranslateScreen) TranslateScreen(viewModel = viewModel, onDismiss = { viewModel.toggleTranslateScreen() })
    if (showTranslationSettings) TranslationSettingsScreen(viewModel = viewModel, onDismiss = { viewModel.toggleTranslationSettings() })
    if (showNoteEditor) NoteEditorSheet(viewModel = viewModel, onDismiss = { viewModel.toggleNoteEditor() })
    if (showNotesList) NotesListSheet(viewModel = viewModel, onDismiss = { viewModel.toggleNotesList() })
}
