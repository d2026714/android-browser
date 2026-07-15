package com.example.browser.web

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebView
import android.widget.Toast

class DownloadHandler(private val context: Context) {
    private val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    fun onDownloadStart(url: String, userAgent: String, contentDisposition: String, mimeType: String) {
        try {
            val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
            val req = DownloadManager.Request(Uri.parse(url)).apply {
                setMimeType(mimeType)
                addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url) ?: "")
                addRequestHeader("User-Agent", userAgent)
                setTitle(fileName)
                setDescription("下载中...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                setAllowedOverMetered(true)
            }
            dm.enqueue(req)
            Toast.makeText(context, "开始下载: $fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "下载失败", Toast.LENGTH_SHORT).show()
        }
    }

    fun attachTo(webView: WebView) {
        webView.setDownloadListener { url, ua, cd, mime, _ ->
            onDownloadStart(url, ua, cd, mime)
        }
    }
}
