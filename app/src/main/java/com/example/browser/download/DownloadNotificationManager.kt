package com.example.browser.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.browser.MainActivity
import com.example.browser.R

class DownloadNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "download_channel"
        const val CHANNEL_NAME = "Downloads"
        private const val GROUP_KEY = "com.example.browser.DOWNLOAD_GROUP"
        private const val SUMMARY_NOTIFICATION_ID = 99999
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows download progress"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showProgressNotification(
        downloadId: Long,
        fileName: String,
        progress: Int,
        totalBytes: Long,
        downloadedBytes: Long
    ) {
        val notificationId = downloadId.toInt()

        val contentIntent = PendingIntent.getActivity(
            context, notificationId,
            Intent(context, MainActivity::class.java).apply {
                action = "OPEN_DOWNLOADS"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Cancel action
        val cancelIntent = PendingIntent.getBroadcast(
            context, notificationId,
            Intent(context, DownloadActionReceiver::class.java).apply {
                action = "CANCEL_DOWNLOAD"
                putExtra("download_id", downloadId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Pause action
        val pauseIntent = PendingIntent.getBroadcast(
            context, notificationId + 100000,
            Intent(context, DownloadActionReceiver::class.java).apply {
                action = "PAUSE_DOWNLOAD"
                putExtra("download_id", downloadId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val sizeText = if (totalBytes > 0) {
            "${formatBytes(downloadedBytes)} / ${formatBytes(totalBytes)}"
        } else {
            formatBytes(downloadedBytes)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(fileName)
            .setContentText("$sizeText ($progress%)")
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .addAction(android.R.drawable.ic_media_pause, "Pause", pauseIntent)
            .addAction(android.R.drawable.ic_delete, "Cancel", cancelIntent)
            .setGroup(GROUP_KEY)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun showCompletedNotification(downloadId: Long, fileName: String, filePath: String) {
        val notificationId = downloadId.toInt()

        val openIntent = PendingIntent.getActivity(
            context, notificationId,
            Intent(context, MainActivity::class.java).apply {
                action = "OPEN_DOWNLOADS"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(fileName)
            .setContentText("Download complete")
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .setGroup(GROUP_KEY)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun showFailedNotification(downloadId: Long, fileName: String, error: String) {
        val notificationId = downloadId.toInt()

        // Retry action
        val retryIntent = PendingIntent.getBroadcast(
            context, notificationId + 200000,
            Intent(context, DownloadActionReceiver::class.java).apply {
                action = "RETRY_DOWNLOAD"
                putExtra("download_id", downloadId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(fileName)
            .setContentText("Download failed: $error")
            .setOngoing(false)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_media_play, "Retry", retryIntent)
            .setGroup(GROUP_KEY)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun cancelNotification(downloadId: Long) {
        notificationManager.cancel(downloadId.toInt())
    }

    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        if (bytes < 1024 * 1024) return "${"%.1f".format(bytes / 1024.0)} KB"
        if (bytes < 1024 * 1024 * 1024) return "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
        return "${"%.2f".format(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
    }
}
