package com.example.browser.download

import android.app.DownloadManager as SystemDownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class DownloadTask(
    val id: Long,
    val url: String,
    val title: String,
    val fileName: String,
    val mimeType: String,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val status: DownloadStatus,
    val filePath: String,
    val startTime: Long,
    val speed: Long = 0 // bytes per second
)

enum class DownloadStatus {
    PENDING, DOWNLOADING, PAUSED, COMPLETED, FAILED, CANCELLED
}

class BrowserDownloadManager(private val context: Context) {
    private val systemDl = context.getSystemService(Context.DOWNLOAD_SERVICE) as SystemDownloadManager
    private val _downloads = MutableStateFlow<List<DownloadTask>>(emptyList())
    val downloads: StateFlow<List<DownloadTask>> = _downloads.asStateFlow()

    private val activeDownloads = mutableMapOf<Long, Long>() // internalId -> systemId

    fun download(
        url: String,
        title: String = "Download",
        fileName: String = url.substringAfterLast("/").substringBefore("?").ifBlank { "download" },
        mimeType: String = "*/*",
        notificationVisibility: Int = SystemDownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
    ): Long {
        val request = SystemDownloadManager.Request(Uri.parse(url)).apply {
            setTitle(title)
            setDescription("Downloading $fileName")
            setNotificationVisibility(notificationVisibility)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
            if (mimeType != "*/*") setMimeType(mimeType)
        }
        val systemId = systemDl.enqueue(request)
        val task = DownloadTask(
            id = systemId,
            url = url,
            title = title,
            fileName = fileName,
            mimeType = mimeType,
            totalBytes = 0,
            downloadedBytes = 0,
            status = DownloadStatus.PENDING,
            filePath = "",
            startTime = System.currentTimeMillis()
        )
        _downloads.value = _downloads.value + task
        return systemId
    }

    fun pause(downloadId: Long) {
        // System DownloadManager doesn't support pause/resume directly
        // We can only cancel and re-download
    }

    fun cancel(downloadId: Long) {
        systemDl.remove(downloadId)
        _downloads.value = _downloads.value.map {
            if (it.id == downloadId) it.copy(status = DownloadStatus.CANCELLED) else it
        }
    }

    fun remove(downloadId: Long) {
        systemDl.remove(downloadId)
        _downloads.value = _downloads.value.filter { it.id != downloadId }
    }

    fun clearCompleted() {
        _downloads.value = _downloads.value.filter { it.status != DownloadStatus.COMPLETED }
    }

    fun refreshStatus() {
        val updated = _downloads.value.map { task ->
            val query = SystemDownloadManager.Query().setFilterById(task.id)
            val cursor = systemDl.query(query)
            cursor?.use {
                if (it.moveToFirst()) {
                    val statusIdx = it.getColumnIndex(SystemDownloadManager.COLUMN_STATUS)
                    val totalIdx = it.getColumnIndex(SystemDownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    val downloadedIdx = it.getColumnIndex(SystemDownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val pathIdx = it.getColumnIndex(SystemDownloadManager.COLUMN_LOCAL_URI)

                    val status = when (it.getInt(statusIdx)) {
                        SystemDownloadManager.STATUS_PENDING -> DownloadStatus.PENDING
                        SystemDownloadManager.STATUS_RUNNING -> DownloadStatus.DOWNLOADING
                        SystemDownloadManager.STATUS_SUCCESSFUL -> DownloadStatus.COMPLETED
                        SystemDownloadManager.STATUS_FAILED -> DownloadStatus.FAILED
                        SystemDownloadManager.STATUS_PAUSED -> DownloadStatus.PAUSED
                        else -> task.status
                    }
                    task.copy(
                        status = status,
                        totalBytes = it.getLong(totalIdx),
                        downloadedBytes = it.getLong(downloadedIdx),
                        filePath = it.getString(pathIdx) ?: ""
                    )
                } else task
            } ?: task
        }
        _downloads.value = updated
    }

    fun getDownloadFilePath(downloadId: Long): String? {
        val query = SystemDownloadManager.Query().setFilterById(downloadId)
        val cursor = systemDl.query(query)
        cursor?.use {
            if (it.moveToFirst()) {
                val idx = it.getColumnIndex(SystemDownloadManager.COLUMN_LOCAL_URI)
                return it.getString(idx)
            }
        }
        return null
    }

    fun getDownloadCount(): Int = _downloads.value.size
    fun getActiveDownloadCount(): Int = _downloads.value.count { it.status == DownloadStatus.DOWNLOADING }

    companion object {
        @Volatile
        private var instance: BrowserDownloadManager? = null

        fun getInstance(context: Context): BrowserDownloadManager {
            return instance ?: synchronized(this) {
                instance ?: BrowserDownloadManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
