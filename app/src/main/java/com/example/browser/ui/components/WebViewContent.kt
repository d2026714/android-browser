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
import com.example.browser.web.BrowserWebViewClient
import com.example.browser.web.DownloadHandler

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewContent(
    url: String,
    isLoading: Boolean,
    adBlockEnabled: Boolean,
    fontSize: Int,
    downloadHandler: DownloadHandler,
    onPageStarted: (String?) -> Unit,
    onPageFinished: (String?) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onError: (String) -> Unit,
    onWebViewCreated: (WebView) -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
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
                        cacheMode = WebSettings.LOAD_DEFAULT
                        databaseEnabled = true
                    }
                    webViewClient = BrowserWebViewClient(
                        onPageStarted = onPageStarted,
                        onPageFinished = onPageFinished,
                        onReceivedError = onError,
                        adBlockEnabled = adBlockEnabled,
                    )
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            onProgressChanged(newProgress)
                        }
                    }
                    downloadHandler.attachTo(this)
                    onWebViewCreated(this)
                    if (url.isNotEmpty()) loadUrl(url)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
