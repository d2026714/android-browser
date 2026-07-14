package com.example.browser.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.browser.manager.TabManager
import com.example.browser.ui.viewmodel.BrowserViewModel
import com.example.browser.adblock.AdBlockEngine
import com.example.browser.download.BrowserDownloadManager
import com.example.browser.download.DownloadService
import com.example.browser.player.MediaPlaybackManager

private const val TAG = "BrowserWebView"

/**
 * BrowserWebView v2: uses TabManager's WebView pool.
 * Each tab has its own WebView instance. Switching tabs swaps views
 * in the container instead of creating new ones.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserWebView(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeTabIndex by viewModel.activeTabIndex.collectAsState()
    val tabs by viewModel.tabs.collectAsState()
    val isAdBlockEnabled by viewModel.isAdBlockEnabled.collectAsState()
    val progress by viewModel.pageProgress.collectAsState()

    var container by remember { mutableStateOf<FrameLayout?>(null) }
    var previousTabIndex by remember { mutableIntStateOf(-1) }

    // Swap WebView when tab changes
    LaunchedEffect(activeTabIndex, tabs.size) {
        val tab = tabs.getOrNull(activeTabIndex) ?: return@LaunchedEffect
        val frame = container ?: return@LaunchedEffect

        // Remove old WebView from container
        if (frame.childCount > 0) {
            val oldWv = frame.getChildAt(0)
            frame.removeView(oldWv)
            // Pause the old WebView to save resources
            (oldWv as? WebView)?.onPause()
        }

        // Get or create WebView for new tab
        val wv = viewModel.tabManager.getOrCreateWebView(tab.id)

        // Configure WebView
        configureWebView(wv, viewModel, isAdBlockEnabled)

        // Attach to container
        if (wv.parent != null) {
            (wv.parent as? ViewGroup)?.removeView(wv)
        }
        frame.addView(wv)

        // Resume the new WebView
        wv.onResume()

        // Update ViewModel's active reference
        viewModel.setActiveWebView(wv)

        // Sync ViewModel state from tab
        viewModel.syncFromTab(tab)

        // Load URL if this is a fresh tab or switching tabs
        if (previousTabIndex != activeTabIndex) {
            if (tab.url.isNotBlank() && tab.url != "about:blank" && tab.url != wv.url) {
                wv.loadUrl(tab.url)
            }
            previousTabIndex = activeTabIndex
        }
    }

    // Handle URL changes from navigation bar (user types new URL)
    val currentUrl by viewModel.currentUrl.collectAsState()
    LaunchedEffect(currentUrl) {
        val wv = viewModel.getActiveWebView() ?: return@LaunchedEffect
        if (currentUrl.isNotBlank() && currentUrl != "about:blank" && currentUrl != wv.url) {
            wv.loadUrl(currentUrl)
        }
    }

    Box(
        modifier = modifier
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                FrameLayout(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    container = this
                }
            }
        )

        // Page loading progress bar
        if (progress in 1..99) {
            LinearProgressIndicator(
                progress = progress / 100f,
                modifier = Modifier.fillMaxWidth(),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

/**
 * Configure a WebView with standard settings and callbacks.
 * Called once per WebView when it's first attached.
 */
@SuppressLint("SetJavaScriptEnabled")
private fun configureWebView(
    wv: WebView,
    viewModel: BrowserViewModel,
    isAdBlockEnabled: Boolean
) {
    // Skip if already configured
    if (wv.tag == "configured") return
    wv.tag = "configured"

    wv.settings.apply {
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
    }

    // Initialize AdBlockEngine and preload rules in background
    val adBlockEngine = AdBlockEngine.getInstance(wv.context)
    Thread { adBlockEngine.loadFilters() }.start()

    wv.webViewClient = object : WebViewClient() {
        override fun shouldInterceptRequest(
            view: WebView?, request: WebResourceRequest?
        ): WebResourceResponse? {
            if (isAdBlockEnabled && request != null) {
                if (adBlockEngine.isAd(request.url.toString())) {
                    return WebResourceResponse("text/plain", "utf-8", "".byteInputStream())
                }
            }
            return super.shouldInterceptRequest(view, request)
        }

        override fun shouldOverrideUrlLoading(
            view: WebView?, request: WebResourceRequest?
        ): Boolean {
            val url = request?.url?.toString() ?: return false
            // Intercept media URLs and prompt user to open in built-in player
            if (MediaPlaybackManager.isMediaUrl(url)) {
                viewModel.onMediaUrlDetected(url, view?.title ?: "")
                return false // Let WebView still try to load it, but show the prompt
            }
            return false
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            viewModel.onLoadingChanged(true)
            url?.let { viewModel.onUrlChanged(it) }
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            url?.let { viewModel.onPageFinished(it, view?.title ?: "") }
            viewModel.onNavigationStateChanged(
                view?.canGoBack() ?: false,
                view?.canGoForward() ?: false
            )
        }

        override fun onReceivedError(
            view: WebView?, request: WebResourceRequest?, error: WebResourceError?
        ) {
            super.onReceivedError(view, request, error)
            // Only handle main frame errors
            if (request?.isForMainFrame == true) {
                val errorCode = error?.errorCode ?: 0
                val desc = error?.description?.toString() ?: "Unknown error"
                Log.e(TAG, "Page error: $errorCode - $desc for ${request.url}")
                viewModel.onPageError(errorCode, desc, request.url?.toString())
            }
        }

        override fun onReceivedSslError(
            view: WebView?, handler: SslErrorHandler?, error: SslError?
        ) {
            Log.e(TAG, "SSL error: ${error?.primaryError} for ${error?.url}")
            // Cancel by default - don't proceed with invalid SSL
            handler?.cancel()
            viewModel.onSslError(error)
        }
    }

    wv.webChromeClient = object : WebChromeClient() {
        override fun onReceivedTitle(view: WebView?, title: String?) {
            super.onReceivedTitle(view, title)
            title?.let { viewModel.onTitleChanged(it) }
        }

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            viewModel.onProgressChanged(newProgress)
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

    wv.setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
        try {
            val downloadManager = BrowserDownloadManager.getInstance(wv.context)
            val fileName = extractDownloadFileName(url, contentDisposition)
            val downloadId = downloadManager.download(
                url = url,
                fileName = fileName,
                mimeType = mimeType.ifBlank { "*/*" }
            )
            // Start foreground service to keep downloads alive
            DownloadService.start(wv.context)
            Log.d(TAG, "Download #$downloadId started: $fileName ($url)")
        } catch (e: Exception) {
            Log.e(TAG, "Download failed: $url", e)
        }
    }

    wv.setOnLongClickListener { v ->
        val hitTestResult = (v as WebView).hitTestResult
        when (hitTestResult.type) {
            WebView.HitTestResult.SRC_ANCHOR_TYPE,
            WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                val url = hitTestResult.extra
                if (url != null) {
                    viewModel.onLongPressUrl(url)
                    true
                } else false
            }
            WebView.HitTestResult.PHONE_TYPE,
            WebView.HitTestResult.EMAIL_TYPE,
            WebView.HitTestResult.GEO_TYPE,
            WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> false
            else -> {
                // For text selection: try to get selected text via JavaScript
                v.evaluateJavascript(
                    "(function(){var s=window.getSelection();return s?s.toString():'';})()"
                ) { text ->
                    val selected = text?.removeSurrounding("\"")?.replace("\\n", "\n")?.replace("\\\"", "\"")
                    if (!selected.isNullOrBlank() && selected != "null") {
                        viewModel.onTextSelected(selected)
                    }
                }
                false // Don't consume the event so text selection handles still work
            }
        }
    }
}

/**
 * Extract a meaningful filename from URL and Content-Disposition header.
 */
private fun extractDownloadFileName(url: String, contentDisposition: String?): String {
    // Try Content-Disposition first
    if (!contentDisposition.isNullOrBlank()) {
        val patterns = listOf(
            Regex("filename\*=UTF-8''(.+)", RegexOption.IGNORE_CASE),
            Regex("filename=\"([^\"]+)\"", RegexOption.IGNORE_CASE),
            Regex("filename=([^;\\s]+)", RegexOption.IGNORE_CASE)
        )
        for (pattern in patterns) {
            val match = pattern.find(contentDisposition)
            if (match != null) {
                val name = match.groupValues[1].trim()
                if (name.isNotBlank()) return java.net.URLDecoder.decode(name, "UTF-8")
            }
        }
    }
    // Fallback to URL path
    val path = url.substringAfterLast("/").substringBefore("?").substringBefore("#")
    val decoded = try { java.net.URLDecoder.decode(path, "UTF-8") } catch (e: Exception) { path }
    return decoded.ifBlank { "download_\${System.currentTimeMillis()}" }
}
