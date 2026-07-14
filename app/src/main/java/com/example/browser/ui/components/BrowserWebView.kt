package com.example.browser.ui.components

import android.app.DownloadManager

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.viewinterop.AndroidView
import com.example.browser.ui.viewmodel.BrowserViewModel
import com.example.browser.util.AdBlocker

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserWebView(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val currentUrl by viewModel.currentUrl.collectAsState()
    val isAdBlockEnabled by viewModel.isAdBlockEnabled.collectAsState()

    var webView by remember { mutableStateOf<WebView?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(currentUrl) {
        webView?.let { wv ->
            if (currentUrl.isNotBlank() && currentUrl != wv.url) {
                wv.loadUrl(currentUrl)
            }
        }
    }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount > 150) viewModel.goBack()
                    else if (dragAmount < -150) viewModel.goForward()
                }
            }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        allowFileAccess = false
                        allowContentAccess = false
                        mediaPlaybackRequiresUserGesture = false
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        textZoom = 100
                        // Enable video fullscreen support
                        loadWithOverviewMode = true
                        useWideViewPort = true
                    }

                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView?, request: WebResourceRequest?
                        ): WebResourceResponse? {
                            if (isAdBlockEnabled && request != null) {
                                if (AdBlocker.isAd(request.url.toString())) {
                                    return WebResourceResponse("text/plain", "utf-8", "".byteInputStream())
                                }
                            }
                            return super.shouldInterceptRequest(view, request)
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            viewModel.onLoadingChanged(true)
                            url?.let { viewModel.onUrlChanged(it) }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isRefreshing = false
                            url?.let { viewModel.onPageFinished(it, view?.title ?: "") }
                            viewModel.onNavigationStateChanged(
                                view?.canGoBack() ?: false,
                                view?.canGoForward() ?: false
                            )
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?, request: WebResourceRequest?
                        ): Boolean = false
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onReceivedTitle(view: WebView?, title: String?) {
                            super.onReceivedTitle(view, title)
                            title?.let { viewModel.onTitleChanged(it) }
                        }

                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                            view?.let { viewModel.enterFullScreen(it) }
                            super.onShowCustomView(view, callback)
                        }

                        override fun onHideCustomView() {
                            viewModel.exitFullScreen()
                            super.onHideCustomView()
                        }
                    }

                    setDownloadListener { url, _, _, _, _ ->
                        try {
                            val request = DownloadManager.Request(android.net.Uri.parse(url))
                            request.setNotificationVisibility(
                                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                            )
                            val dm = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as DownloadManager
                            dm.enqueue(request)
                        } catch (_: Exception) {}
                    }

                    // Long press context menu
                    setOnLongClickListener { v ->
                        val hitTestResult = (v as WebView).hitTestResult
                        if (hitTestResult.type == WebView.HitTestResult.SRC_ANCHOR_TYPE ||
                            hitTestResult.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                            val url = hitTestResult.extra
                            if (url != null) {
                                // Trigger context menu via ViewModel
                                true
                            } else false
                        } else false
                    }

                    viewModel.setWebView(this)
                    webView = this
                }
            },
            update = { }
        )

        // Pull to refresh indicator
        if (isRefreshing) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// Helper extension for pull-to-refresh via swipe down
@Composable
fun Modifier.pullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit
): Modifier {
    return this.pointerInput(isRefreshing) {
        detectHorizontalDragGestures { _, _ -> }
    }
}
