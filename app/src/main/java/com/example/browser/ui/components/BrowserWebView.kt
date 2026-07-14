package com.example.browser.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.GestureDetector
import android.view.MotionEvent
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
    val isDesktopMode by viewModel.isDesktopMode.collectAsState()

    var webView by remember { mutableStateOf<WebView?>(null) }

    // Navigate when URL changes externally
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
                    if (dragAmount > 150) {
                        // Swipe right → go back
                        viewModel.goBack()
                    } else if (dragAmount < -150) {
                        // Swipe left → go forward
                        viewModel.goForward()
                    }
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
                        mediaPlaybackRequiresUserGesture = true
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

                        // Enable text reflow for reading mode
                        textZoom = 100
                    }

                    // Desktop mode user agent
                    if (isDesktopMode) {
                        settings.userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                    }

                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            if (isAdBlockEnabled && request != null) {
                                val url = request.url.toString()
                                if (AdBlocker.isAd(url)) {
                                    return WebResourceResponse(
                                        "text/plain",
                                        "utf-8",
                                        "".byteInputStream()
                                    )
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
                            url?.let {
                                viewModel.onPageFinished(it, view?.title ?: "")
                            }
                            viewModel.onNavigationStateChanged(
                                view?.canGoBack() ?: false,
                                view?.canGoForward() ?: false
                            )
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean = false
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onReceivedTitle(view: WebView?, title: String?) {
                            super.onReceivedTitle(view, title)
                            title?.let { viewModel.onTitleChanged(it) }
                        }
                    }

                    setDownloadListener { url, _, _, mimeType, _ ->
                        try {
                            val request = android.app.DownloadManager.Request(android.net.Uri.parse(url))
                            request.setNotificationVisibility(
                                android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                            )
                            val dm = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                            dm.enqueue(request)
                        } catch (_: Exception) {}
                    }

                    // Expose WebView to ViewModel for navigation and find-in-page
                    viewModel.setWebView(this)
                    webView = this
                }
            },
            update = { wv ->
                // Updates handled via LaunchedEffect and ViewModel
            }
        )
    }
}
