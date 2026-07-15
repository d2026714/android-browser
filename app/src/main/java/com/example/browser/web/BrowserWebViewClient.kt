package com.example.browser.web

import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.*

class BrowserWebViewClient(
    private val onPageStarted: ((String?) -> Unit)? = null,
    private val onPageFinished: ((String?) -> Unit)? = null,
    private val onReceivedError: ((String) -> Unit)? = null,
    private var adBlockEnabled: Boolean = true,
) : WebViewClient() {

    override fun shouldInterceptRequest(
        view: WebView?, request: WebResourceRequest?,
    ): WebResourceResponse? {
        val url = request?.url?.toString() ?: return null
        if (adBlockEnabled && AdBlocker.isAd(url)) {
            return WebResourceResponse("text/plain", "utf-8", "".byteInputStream())
        }
        return super.shouldInterceptRequest(view, request)
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        onPageStarted?.invoke(url)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        onPageFinished?.invoke(url)
    }

    override fun onReceivedError(
        view: WebView?, request: WebResourceRequest?, error: WebResourceError?,
    ) {
        super.onReceivedError(view, request, error)
        if (request?.isForMainFrame == true) {
            onReceivedError?.invoke(error?.description?.toString() ?: "未知错误")
        }
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        handler?.cancel()
    }

    fun setAdBlockEnabled(enabled: Boolean) { adBlockEnabled = enabled }
}
