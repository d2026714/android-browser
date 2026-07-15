package com.example.browser.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.browser.web.BrowserWebViewClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: BrowserViewModel = viewModel()) {
    val tabs by viewModel.tabs.collectAsState()
    val activeTabIndex by viewModel.activeTabIndex.collectAsState()
    val showHome by viewModel.showHome.collectAsState()
    val showBookmarks by viewModel.showBookmarks.collectAsState()
    val showHistory by viewModel.showHistory.collectAsState()
    val showSettings by viewModel.showSettings.collectAsState()
    val showFindBar by viewModel.showFindBar.collectAsState()
    val adBlockEnabled by viewModel.adBlockEnabled.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()

    val context = LocalContext.current

    when {
        showBookmarks -> BookmarksScreen(viewModel = viewModel)
        showHistory -> HistoryScreen(viewModel = viewModel)
        showSettings -> SettingsScreen(viewModel = viewModel)
        showHome -> HomeScreen(viewModel = viewModel)
        else -> {
            val activeTab = tabs.getOrNull(activeTabIndex)

            Column(modifier = Modifier.fillMaxSize()) {
                // Top navigation bar
                TopNavBar(
                    title = activeTab?.title ?: "",
                    url = activeTab?.url ?: "",
                    isLoading = activeTab?.isLoading ?: false,
                    progress = activeTab?.progress ?: 0,
                    canGoBack = activeTab?.canGoBack ?: false,
                    canGoForward = activeTab?.canGoForward ?: false,
                    onBack = { viewModel.goBack() },
                    onForward = { viewModel.goForward() },
                    onReload = { viewModel.reload() },
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
                    onHome = { viewModel.goHome() },
                    onMenu = { viewModel.showSettingsScreen() },
                )

                // WebView content
                Box(modifier = Modifier.weight(1f)) {
                    WebViewContent(
                        tabs = tabs,
                        activeTabIndex = activeTabIndex,
                        viewModel = viewModel,
                        adBlockEnabled = adBlockEnabled,
                        fontSize = fontSize,
                    )
                }

                // Find in page bar
                AnimatedVisibility(
                    visible = showFindBar,
                    enter = slideInVertically(initialOffsetY = { -it }),
                    exit = slideOutVertically(targetOffsetY = { -it }),
                ) {
                    FindInPageBar(
                        onSearch = { query -> viewModel.findAll(query) },
                        onNext = { viewModel.findNext() },
                        onPrevious = { viewModel.findPrevious() },
                        onClose = {
                            viewModel.clearFindMatches()
                            viewModel.hideFind()
                        },
                    )
                }

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
}

@Composable
private fun TopNavBar(
    title: String,
    url: String,
    isLoading: Boolean,
    progress: Int,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onShare: () -> Unit,
    onBookmark: () -> Unit,
    onFind: () -> Unit,
    onHome: () -> Unit,
    onMenu: () -> Unit,
) {
    Column {
        // Navigation controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, enabled = canGoBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "后退")
            }
            IconButton(onClick = onForward, enabled = canGoForward) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "前进")
            }
            IconButton(onClick = if (isLoading) onReload else onReload) {
                Icon(
                    if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
                    contentDescription = if (isLoading) "停止" else "刷新",
                )
            }

            // URL display
            Text(
                text = url.removePrefix("https://").removePrefix("http://").take(50),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            IconButton(onClick = onShare) {
                Icon(Icons.Default.Share, contentDescription = "分享")
            }
            IconButton(onClick = onBookmark) {
                Icon(Icons.Default.BookmarkBorder, contentDescription = "书签")
            }
            IconButton(onClick = onFind) {
                Icon(Icons.Default.Search, contentDescription = "查找")
            }
            IconButton(onClick = onMenu) {
                Icon(Icons.Default.MoreVert, contentDescription = "菜单")
            }
        }

        // Progress bar
        if (isLoading) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth().height(2.dp),
            )
        }
    }
}

@Composable
private fun WebViewContent(
    tabs: List<TabState>,
    activeTabIndex: Int,
    viewModel: BrowserViewModel,
    adBlockEnabled: Boolean,
    fontSize: Int,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        tabs.forEachIndexed { index, tab ->
            if (index == activeTabIndex) {
                WebViewContainer(
                    tab = tab,
                    index = index,
                    viewModel = viewModel,
                    adBlockEnabled = adBlockEnabled,
                    fontSize = fontSize,
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebViewContainer(
    tab: TabState,
    index: Int,
    viewModel: BrowserViewModel,
    adBlockEnabled: Boolean,
    fontSize: Int,
) {
    val context = LocalContext.current

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    builtInZoomControls = true
                    displayZoomControls = false
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    textSize = fontSize
                }

                val client = BrowserWebViewClient(
                    onPageStarted = { url ->
                        viewModel.updateTabState(index, isLoading = true, url = url)
                    },
                    onPageFinished = { url ->
                        viewModel.updateTabState(index, isLoading = false)
                        // Update title
                        val title = this.title ?: url ?: ""
                        viewModel.updateTabState(index, title = title)
                    },
                    onReceivedError = { /* handled silently */ },
                    adBlockEnabled = adBlockEnabled,
                )
                webViewClient = client

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        viewModel.updateTabState(index, progress = newProgress)
                    }
                }

                viewModel.setWebView(index, this)

                if (tab.url.isNotEmpty()) {
                    loadUrl(tab.url)
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun FindInPageBar(
    onSearch: (String) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onClose: () -> Unit,
) {
    var query by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text("在页面中查找") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
        )
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "上一个")
        }
        IconButton(onClick = onNext) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "下一个")
        }
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = "关闭")
        }
    }
}

@Composable
private fun BottomTabBar(
    tabs: List<TabState>,
    activeTabIndex: Int,
    onTabClick: (Int) -> Unit,
    onAddTab: () -> Unit,
    onCloseTab: (Int) -> Unit,
    onHome: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Home button
        IconButton(onClick = onHome, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Home, contentDescription = "首页", modifier = Modifier.size(20.dp))
        }

        // Tabs
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            tabs.forEachIndexed { index, tab ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (index == activeTabIndex)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { onTabClick(index) }
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = tab.title.take(12),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 10.sp,
                        )
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭",
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { onCloseTab(index) },
                        )
                    }
                }
            }
        }

        // Add tab button
        IconButton(onClick = onAddTab, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Add, contentDescription = "新标签", modifier = Modifier.size(20.dp))
        }
    }
}
