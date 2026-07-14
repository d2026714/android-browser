package com.example.browser.download

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Handles notification action buttons: Cancel, Pause, Retry.
 */
class DownloadActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val downloadId = intent.getLongExtra("download_id", -1)
        if (downloadId == -1L) return

        val downloadManager = BrowserDownloadManager.getInstance(context)

        when (intent.action) {
            "CANCEL_DOWNLOAD" -> {
                Log.d("DownloadActionReceiver", "Cancel download: $downloadId")
                downloadManager.cancelDownload(downloadId)
            }
            "PAUSE_DOWNLOAD" -> {
                Log.d("DownloadActionReceiver", "Pause download: $downloadId")
                downloadManager.pauseDownload(downloadId)
            }
            "RETRY_DOWNLOAD" -> {
                Log.d("DownloadActionReceiver", "Retry download: $downloadId")
                downloadManager.retryDownload(downloadId)
            }
        }
    }
}
