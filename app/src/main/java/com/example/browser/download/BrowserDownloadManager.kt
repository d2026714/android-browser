package com.example.browser.download

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.browser.data.local.BrowserDatabase
import com.example.browser.data.local.dao.DownloadDao
import com.example.browser.data.local.entity.DownloadEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import okhttp3.*
import okio.BufferedSink
import okio.BufferedSource
import okio.buffer
import okio.sink
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Full-featured download manager with:
 * - Concurrent download queue with configurable limit
 * - Pause / Resume / Cancel / Retry
 * - Range header support for resumable downloads
 * - Progress callbacks via StateFlow
 * - Download history persisted in Room
 * - Notification integration
 */
class BrowserDownloadManager(private val context: Context) {

    companion object {
        private const val TAG = "BrowserDownloadManager"
        const val MAX_CONCURRENT_DOWNLOADS = 3

        @Volatile
        private var instance: BrowserDownloadManager? = null

        fun getInstance(context: Context): BrowserDownloadManager {
            return instance ?: synchronized(this) {
                instance ?: BrowserDownloadManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val db = BrowserDatabase.getInstance(context)
    private val downloadDao: DownloadDao = db.downloadDao()
    private val notificationManager = DownloadNotificationManager(context)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    // Active download jobs: downloadId -> coroutine Job
    private val activeJobs = ConcurrentHashMap<Long, Job>()
    // Active OkHttp calls: downloadId -> Call
    private val activeCalls = ConcurrentHashMap<Long, Call>()
    // Pause flags: downloadId -> true means paused
    private val pauseFlags = ConcurrentHashMap<Long, Boolean>()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Public observable downloads
    val allDownloads: Flow<List<DownloadEntity>> = downloadDao.getAllFlow()

    // Progress updates (downloadId -> downloadedBytes)
    private val _progressUpdates = MutableStateFlow<Map<Long, Long>>(emptyMap())
    val progressUpdates: StateFlow<Map<Long, Long>> = _progressUpdates.asStateFlow()

    /**
     * Start a new download. Returns the download ID.
     */
    fun download(
        url: String,
        fileName: String = extractFileName(url),
        mimeType: String = "*/*",
        downloadDir: File? = null
    ): Long {
        // Check for existing active download of the same URL
        val existing = runBlocking { downloadDao.getByUrl(url) }
        if (existing != null && (existing.status == DownloadEntity.STATUS_DOWNLOADING ||
                    existing.status == DownloadEntity.STATUS_PENDING)
        ) {
            Log.d(TAG, "Download already active for: $url")
            return existing.id
        }

        val targetDir = downloadDir ?: File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "Browser"
        )
        if (!targetDir.exists()) targetDir.mkdirs()

        val entity = DownloadEntity(
            url = url,
            fileName = fileName,
            mimeType = mimeType,
            filePath = File(targetDir, fileName).absolutePath,
            status = DownloadEntity.STATUS_PENDING
        )

        val id = runBlocking { downloadDao.insert(entity) }
        Log.d(TAG, "Enqueued download #$id: $fileName")

        processQueue()
        return id
    }

    /**
     * Pause a download. If the download supports Range headers, it can be resumed later.
     */
    fun pauseDownload(downloadId: Long) {
        Log.d(TAG, "Pausing download #$downloadId")
        pauseFlags[downloadId] = true

        // Cancel the OkHttp call (will trigger catch block which checks pause flag)
        activeCalls[downloadId]?.cancel()

        scope.launch {
            downloadDao.updateStatus(downloadId, DownloadEntity.STATUS_PAUSED)
            notificationManager.cancelNotification(downloadId)
        }
    }

    /**
     * Resume a paused download.
     */
    fun resumeDownload(downloadId: Long) {
        Log.d(TAG, "Resuming download #$downloadId")
        pauseFlags.remove(downloadId)

        scope.launch {
            downloadDao.updateStatus(downloadId, DownloadEntity.STATUS_PENDING)
            processQueue()
        }
    }

    /**
     * Cancel a download and optionally delete partial file.
     */
    fun cancelDownload(downloadId: Long) {
        Log.d(TAG, "Cancelling download #$downloadId")
        pauseFlags.remove(downloadId)
        activeCalls[downloadId]?.cancel()
        activeJobs[downloadId]?.cancel()
        activeJobs.remove(downloadId)
        activeCalls.remove(downloadId)

        scope.launch {
            downloadDao.updateStatus(downloadId, DownloadEntity.STATUS_CANCELLED)
            notificationManager.cancelNotification(downloadId)

            // Delete partial file
            val entity = downloadDao.getById(downloadId)
            entity?.let {
                val file = File(it.filePath)
                if (file.exists()) file.delete()
            }

            processQueue()
        }
    }

    /**
     * Retry a failed download from the beginning.
     */
    fun retryDownload(downloadId: Long) {
        Log.d(TAG, "Retrying download #$downloadId")
        scope.launch {
            val entity = downloadDao.getById(downloadId) ?: return@launch

            // Delete partial file
            val file = File(entity.filePath)
            if (file.exists()) file.delete()

            // Reset progress
            downloadDao.updateProgress(downloadId, 0)
            downloadDao.updateTotalBytes(downloadId, 0)
            downloadDao.updateStatus(downloadId, DownloadEntity.STATUS_PENDING)
            downloadDao.updateError(downloadId, null)

            processQueue()
        }
    }

    /**
     * Remove a download from history (only if not active).
     */
    fun removeDownload(downloadId: Long) {
        scope.launch {
            val entity = downloadDao.getById(downloadId) ?: return@launch
            if (entity.status == DownloadEntity.STATUS_DOWNLOADING ||
                entity.status == DownloadEntity.STATUS_PENDING
            ) {
                cancelDownload(downloadId)
            }
            downloadDao.deleteById(downloadId)
            notificationManager.cancelNotification(downloadId)
        }
    }

    /**
     * Clear all completed downloads from history.
     */
    fun clearCompleted() {
        scope.launch { downloadDao.deleteCompleted() }
    }

    /**
     * Clear all failed and cancelled downloads from history.
     */
    fun clearFailed() {
        scope.launch { downloadDao.deleteFailedAndCancelled() }
    }

    /**
     * Process the download queue: start pending downloads up to the concurrency limit.
     */
    private fun processQueue() {
        scope.launch {
            val activeCount = downloadDao.getActiveCount()
            val slotsAvailable = MAX_CONCURRENT_DOWNLOADS - activeCount

            if (slotsAvailable <= 0) return@launch

            // Get pending downloads ordered by creation time
            val pendingDownloads = downloadDao.getActiveDownloads()
                .filter { it.status == DownloadEntity.STATUS_PENDING }
                .take(slotsAvailable)

            for (entity in pendingDownloads) {
                if (activeJobs[entity.id]?.isActive == true) continue
                startDownload(entity)
            }
        }
    }

    /**
     * Start downloading a single entity.
     */
    private fun startDownload(entity: DownloadEntity) {
        val job = scope.launch {
            try {
                downloadDao.updateStatus(entity.id, DownloadEntity.STATUS_DOWNLOADING)
                pauseFlags.remove(entity.id)

                val file = File(entity.filePath)
                var downloadedBytes = entity.downloadedBytes

                // Build request with Range header for resume
                val requestBuilder = Request.Builder().url(entity.url)
                if (downloadedBytes > 0 && file.exists()) {
                    requestBuilder.addHeader("Range", "bytes=$downloadedBytes-")
                    Log.d(TAG, "Resuming from byte $downloadedBytes")
                }

                val request = requestBuilder.build()
                val call = httpClient.newCall(request)
                activeCalls[entity.id] = call

                val response = call.execute()

                if (!response.isSuccessful && response.code != 206) {
                    throw IOException("HTTP ${response.code}: ${response.message}")
                }

                val body = response.body ?: throw IOException("Empty response body")
                val contentLength = body.contentLength()

                // If server returned 206 (Partial Content), resume is supported
                val isPartialContent = response.code == 206
                if (isPartialContent) {
                    downloadDao.updateResumable(entity.id, true)
                } else if (downloadedBytes > 0 && response.code == 200) {
                    // Server doesn't support Range, restart from beginning
                    downloadedBytes = 0
                    downloadDao.updateProgress(entity.id, 0)
                    downloadDao.updateResumable(entity.id, false)
                }

                val totalBytes = if (contentLength > 0) {
                    if (isPartialContent) contentLength + downloadedBytes else contentLength
                } else {
                    -1
                }
                if (totalBytes > 0) {
                    downloadDao.updateTotalBytes(entity.id, totalBytes)
                }

                // Write to file
                if (isPartialContent && downloadedBytes > 0) {
                    // Append to existing file
                    RandomAccessFile(file, "rw").use { raf ->
                        raf.seek(downloadedBytes)
                        val source: BufferedSource = body.source()
                        val sink = okio.buffer(raf.channel.sink())
                        val buffer = okio.Buffer()
                        var lastUpdateTime = 0L

                        while (true) {
                            if (pauseFlags[entity.id] == true || !isActive) break

                            val bytesRead = source.read(buffer, 8192)
                            if (bytesRead == -1L) break

                            sink.write(buffer, bytesRead)
                            downloadedBytes += bytesRead

                            // Update progress (throttled to every 500ms)
                            val now = System.currentTimeMillis()
                            if (now - lastUpdateTime > 500) {
                                downloadDao.updateProgress(entity.id, downloadedBytes)
                                updateProgressFlow(entity.id, downloadedBytes)
                                val progress = if (totalBytes > 0) {
                                    ((downloadedBytes * 100) / totalBytes).toInt()
                                } else 0
                                notificationManager.showProgressNotification(
                                    entity.id, entity.fileName, progress, totalBytes, downloadedBytes
                                )
                                lastUpdateTime = now
                            }
                        }
                        sink.flush()
                        sink.close()
                        source.close()
                    }
                } else {
                    // Write from beginning
                    file.parentFile?.mkdirs()
                    val sink: BufferedSink = file.sink().buffer()
                    val source: BufferedSource = body.source()
                    val buffer = okio.Buffer()
                    var lastUpdateTime = 0L

                    while (true) {
                        if (pauseFlags[entity.id] == true || !isActive) break

                        val bytesRead = source.read(buffer, 8192)
                        if (bytesRead == -1L) break

                        sink.write(buffer, bytesRead)
                        downloadedBytes += bytesRead

                        // Update progress (throttled)
                        val now = System.currentTimeMillis()
                        if (now - lastUpdateTime > 500) {
                            downloadDao.updateProgress(entity.id, downloadedBytes)
                            updateProgressFlow(entity.id, downloadedBytes)
                            val progress = if (totalBytes > 0) {
                                ((downloadedBytes * 100) / totalBytes).toInt()
                            } else 0
                            notificationManager.showProgressNotification(
                                entity.id, entity.fileName, progress, totalBytes, downloadedBytes
                            )
                            lastUpdateTime = now
                        }
                    }
                    sink.flush()
                    sink.close()
                    source.close()
                }

                response.close()

                // Check if completed or paused
                if (pauseFlags[entity.id] == true) {
                    downloadDao.updateStatus(entity.id, DownloadEntity.STATUS_PAUSED)
                    notificationManager.cancelNotification(entity.id)
                    Log.d(TAG, "Download paused: #${entity.id}")
                } else if (isActive) {
                    downloadDao.updateProgress(entity.id, downloadedBytes)
                    downloadDao.updateStatus(entity.id, DownloadEntity.STATUS_COMPLETED)
                    notificationManager.showCompletedNotification(entity.id, entity.fileName, entity.filePath)
                    Log.d(TAG, "Download completed: #${entity.id}")
                }

            } catch (e: CancellationException) {
                // Job was cancelled
                if (pauseFlags[entity.id] == true) {
                    downloadDao.updateStatus(entity.id, DownloadEntity.STATUS_PAUSED)
                } else {
                    downloadDao.updateStatus(entity.id, DownloadEntity.STATUS_CANCELLED)
                }
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Download failed: #${entity.id}", e)
                val errorMsg = e.message ?: "Unknown error"
                downloadDao.updateStatus(entity.id, DownloadEntity.STATUS_FAILED)
                downloadDao.updateError(entity.id, errorMsg)
                notificationManager.showFailedNotification(entity.id, entity.fileName, errorMsg)
            } finally {
                activeCalls.remove(entity.id)
                activeJobs.remove(entity.id)
                pauseFlags.remove(entity.id)
            }

            // Process next in queue
            processQueue()
        }

        activeJobs[entity.id] = job
    }

    private fun updateProgressFlow(downloadId: Long, bytes: Long) {
        _progressUpdates.value = _progressUpdates.value.toMutableMap().apply {
            put(downloadId, bytes)
        }
    }

    /**
     * Check if a file exists and can be opened.
     */
    fun isFileAvailable(entity: DownloadEntity): Boolean {
        if (entity.status != DownloadEntity.STATUS_COMPLETED) return false
        val file = File(entity.filePath)
        return file.exists() && file.length() > 0
    }

    /**
     * Get the download directory.
     */
    fun getDownloadDir(): File {
        return File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "Browser"
        )
    }

    private fun extractFileName(url: String): String {
        val path = url.substringAfterLast("/").substringBefore("?").substringBefore("#")
        return path.ifBlank { "download_${System.currentTimeMillis()}" }
    }

    fun destroy() {
        scope.cancel()
        httpClient.dispatcher.executorService.shutdown()
    }
}
