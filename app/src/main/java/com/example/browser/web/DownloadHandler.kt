package com.example.browser.web

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebView
import android.widget.Toast

class DownloadHandler(private val context: Context) {

    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    fun onDownloadStart(url: String, userAgent: String, contentDisposition: String, mimeType: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
                setMimeType(mimeType)
                addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url) ?: "")
                addRequestHeader("User-Agent", userAgent)
                setDescription("下载中...")
                setTitle(fileName)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    fileName,
                )
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }
            downloadManager.enqueue(request)
            Toast.makeText(context, "开始下载", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "下载失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun registerDownloadListener(webView: WebView) {
        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            onDownloadStart(url, userAgent, contentDisposition, mimeType)
        }
    }
}
