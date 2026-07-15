package com.example.browser.ui.components

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.browser.ui.TabState
import com.example.browser.ui.BrowserViewModel
import com.example.browser.web.BrowserWebViewClient

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewContent(
    tabs: List<TabState>,
    activeTabIndex: Int,
    viewModel: BrowserViewModel,
    adBlockEnabled: Boolean,
    fontSize: Int,
    onError: (Int, String) -> Unit,
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
                    onError = { desc -> onError(index, desc) },
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
    onError: (String) -> Unit,
) {
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
                    textZoom = fontSize
                    // Performance
                    cacheMode = WebSettings.LOAD_DEFAULT
                    databaseEnabled = true
                }

                val client = BrowserWebViewClient(
                    onPageStarted = { url ->
                        viewModel.updateTabState(index, isLoading = true, url = url, hasError = false)
                    },
                    onPageFinished = { url ->
                        viewModel.updateTabState(index, isLoading = false)
                        val title = this.title ?: url ?: ""
                        viewModel.updateTabState(index, title = title)
                    },
                    onReceivedError = { desc ->
                        viewModel.updateTabState(index, hasError = true, isLoading = false)
                        onError(desc)
                    },
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
