package com.example.browser.ui

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.browser.ui.components.*
import com.example.browser.web.DownloadHandler

@Composable
fun BrowserScreen(
    vm: BrowserViewModel,
    onBookmarks: () -> Unit, onHistory: () -> Unit, onSettings: () -> Unit,
    onBookshelf: () -> Unit, onReader: () -> Unit,
) {
    val tabs by vm.tabs.collectAsState()
    val idx by vm.activeIndex.collectAsState()
    val home by vm.showHome.collectAsState()
    val findBar by vm.showFindBar.collectAsState()
    val adBlock by vm.adBlock.collectAsState()
    val fs by vm.fontSize.collectAsState()
    val extracting by vm.isExtracting.collectAsState()
    val ctx = LocalContext.current
    val dl = remember { DownloadHandler(ctx) }

    if (home) {
        HomeScreen(vm, onBookmarks, onHistory, onSettings, onBookshelf)
    } else {
        val tab = tabs.getOrNull(idx)
        Column(Modifier.fillMaxSize()) {
            TopNavBar(
                url = tab?.url ?: "", isLoading = tab?.isLoading ?: false, progress = tab?.progress ?: 0,
                canGoBack = tab?.canGoBack ?: false, canGoForward = tab?.canGoForward ?: false,
                onBack = { vm.goBack() }, onForward = { vm.goForward() },
                onReload = { vm.reload() }, onStop = { vm.stopLoading() },
                onShare = {
                    val u = tab?.url ?: return@TopNavBar
                    ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { putExtra(Intent.EXTRA_TEXT, u); type = "text/plain" }, "分享"))
                }, onBookmark = { vm.toggleBookmark() }, onFind = { vm.showFind() }, onMenu = { onSettings() })

            Box(Modifier.weight(1f)) {
                if (tab?.hasError == true) ErrorPage("无法加载页面") { vm.reload() }
                else WebViewContent(
                    url = tab?.url ?: "", isLoading = tab?.isLoading ?: false,
                    adBlockEnabled = adBlock, fontSize = fs, downloadHandler = dl,
                    onPageStarted = { vm.updateTab(idx, isLoading = true, url = it, hasError = false) },
                    onPageFinished = { vm.updateTab(idx, isLoading = false, title = it ?: "") },
                    onProgressChanged = { vm.updateTab(idx, progress = it) },
                    onError = { vm.updateTab(idx, hasError = true, isLoading = false) },
                    onWebViewCreated = { vm.setWebView(idx, it) })
            }

            FindInPageBar(findBar, { vm.findAll(it) }, { vm.findNext() }, { vm.findPrev() }, { vm.clearFind(); vm.hideFind() })

            if (tab?.url?.isNotEmpty() == true) {
                SmallFloatingActionButton(onClick = { vm.extractContent(); onReader() },
                    Modifier.padding(8.dp), containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                    if (extracting) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Default.AutoStories, "阅读模式", Modifier.size(20.dp))
                }
            }

            BottomTabBar(tabs.map { TabInfo(it.title, it.url) }, idx,
                onTabClick = { vm.switchTab(it) }, onAddTab = { vm.addTab() },
                onCloseTab = { vm.closeTab(it) }, onHome = { vm.goHome() })
        }
    }
}
