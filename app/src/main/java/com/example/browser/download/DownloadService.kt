package com.example.browser.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.example.browser.MainActivity
import kotlinx.coroutines.*

/**
 * Foreground service that keeps the download manager alive
 * even when the app is in the background.
 */
class DownloadService : Service() {

    companion object {
        private const val TAG = "DownloadService"
        private const val CHANNEL_ID = "download_service_channel"
        private const val NOTIFICATION_ID = 88888
        private const val ACTION_START = "com.example.browser.download.START"
        private const val ACTION_STOP = "com.example.browser.download.STOP"

        fun start(context: Context) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var monitoringJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, buildServiceNotification())
                startMonitoring()
                Log.d(TAG, "Download service started")
            }
            ACTION_STOP -> {
                stopMonitoring()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                Log.d(TAG, "Download service stopped")
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        scope.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Download Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps downloads running in the background"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildServiceNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                action = "OPEN_DOWNLOADS"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading...")
            .setContentText("Downloads are running in the background")
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .build()
    }

    private fun startMonitoring() {
        if (monitoringJob?.isActive == true) return
        monitoringJob = scope.launch {
            val downloadManager = BrowserDownloadManager.getInstance(this@DownloadService)
            while (isActive) {
                val activeCount = com.example.browser.data.local.BrowserDatabase
                    .getInstance(this@DownloadService)
                    .downloadDao()
                    .getActiveCount()

                if (activeCount == 0) {
                    // No active downloads, stop service after a short delay
                    delay(5000)
                    val recheckCount = com.example.browser.data.local.BrowserDatabase
                        .getInstance(this@DownloadService)
                        .downloadDao()
                        .getActiveCount()
                    if (recheckCount == 0) {
                        stopSelf()
                        break
                    }
                }

                // Update service notification with active count
                if (activeCount > 0) {
                    val nm = getSystemService(NotificationManager::class.java)
                    val notification = Notification.Builder(this@DownloadService, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.stat_sys_download)
                        .setContentTitle("$activeCount download(s) active")
                        .setContentText("Downloads are running in the background")
                        .setOngoing(true)
                        .setContentIntent(
                            PendingIntent.getActivity(
                                this@DownloadService, 0,
                                Intent(this@DownloadService, MainActivity::class.java).apply {
                                    action = "OPEN_DOWNLOADS"
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                },
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                        )
                        .build()
                    nm.notify(NOTIFICATION_ID, notification)
                }

                delay(3000)
            }
        }
    }

    private fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
    }
}
