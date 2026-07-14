package com.example.browser.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.runtime.*
import androidx.compose.ui.viewinterop.AndroidView
import com.example.browser.ui.viewmodel.BrowserViewModel
import com.example.browser.util.AdBlocker

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserWebView(
    viewModel: BrowserViewModel,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    val currentUrl by viewModel.currentUrl.collectAsState()
    val isAdBlockEnabled by viewModel.isAdBlockEnabled.collectAsState()

    var webView by remember { mutableStateOf<WebView?>(null) }

    // Navigate when URL changes externally (from bookmarks, history, etc.)
    LaunchedEffect(currentUrl) {
        webView?.let { wv ->
            if (currentUrl.isNotBlank() && currentUrl != wv.url) {
                wv.loadUrl(currentUrl)
            }
        }
    }

    AndroidView(
        modifier = modifier,
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
                    ): Boolean {
                        // Let the WebView handle all http/https URLs
                        return false
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        super.onReceivedTitle(view, title)
                        title?.let { viewModel.onTitleChanged(it) }
                    }

                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        // Could expose progress bar here
                    }
                }

                setDownloadListener { url, _, _, mimeType, _ ->
                    // Handle downloads via system download manager
                    try {
                        val request = android.app.DownloadManager.Request(android.net.Uri.parse(url))
                        request.setNotificationVisibility(
                            android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                        )
                        val dm = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                        dm.enqueue(request)
                    } catch (_: Exception) {}
                }

                webView = this
            }
        },
        update = { wv ->
            // Updates handled via LaunchedEffect
        }
    )
}
