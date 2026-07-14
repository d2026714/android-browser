package com.example.browser.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_history")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val fileName: String,
    val mimeType: String = "*/*",
    val totalBytes: Long = 0,
    val downloadedBytes: Long = 0,
    val status: Int = STATUS_PENDING, // 0=pending, 1=downloading, 2=paused, 3=completed, 4=failed, 5=cancelled
    val filePath: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val errorMessage: String? = null,
    val resumable: Boolean = true // Whether server supports Range header
) {
    companion object {
        const val STATUS_PENDING = 0
        const val STATUS_DOWNLOADING = 1
        const val STATUS_PAUSED = 2
        const val STATUS_COMPLETED = 3
        const val STATUS_FAILED = 4
        const val STATUS_CANCELLED = 5
    }
}
