package com.example.browser.data.local.dao

import androidx.room.*
import com.example.browser.data.local.entity.DownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM download_history ORDER BY updatedAt DESC")
    fun getAllFlow(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM download_history WHERE status = :status ORDER BY updatedAt DESC")
    fun getByStatusFlow(status: Int): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM download_history WHERE id = :id")
    suspend fun getById(id: Long): DownloadEntity?

    @Query("SELECT * FROM download_history WHERE url = :url AND status != 5 ORDER BY createdAt DESC LIMIT 1")
    suspend fun getByUrl(url: String): DownloadEntity?

    @Insert
    suspend fun insert(entity: DownloadEntity): Long

    @Update
    suspend fun update(entity: DownloadEntity)

    @Query("UPDATE download_history SET status = :status, updatedAt = :time WHERE id = :id")
    suspend fun updateStatus(id: Long, status: Int, time: Long = System.currentTimeMillis())

    @Query("UPDATE download_history SET downloadedBytes = :bytes, updatedAt = :time WHERE id = :id")
    suspend fun updateProgress(id: Long, bytes: Long, time: Long = System.currentTimeMillis())

    @Query("UPDATE download_history SET totalBytes = :total, updatedAt = :time WHERE id = :id")
    suspend fun updateTotalBytes(id: Long, total: Long, time: Long = System.currentTimeMillis())

    @Query("UPDATE download_history SET filePath = :path, updatedAt = :time WHERE id = :id")
    suspend fun updateFilePath(id: Long, path: String, time: Long = System.currentTimeMillis())

    @Query("UPDATE download_history SET errorMessage = :msg, updatedAt = :time WHERE id = :id")
    suspend fun updateError(id: Long, msg: String?, time: Long = System.currentTimeMillis())

    @Query("UPDATE download_history SET resumable = :resumable WHERE id = :id")
    suspend fun updateResumable(id: Long, resumable: Boolean)

    @Delete
    suspend fun delete(entity: DownloadEntity)

    @Query("DELETE FROM download_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM download_history WHERE status = 3") // completed
    suspend fun deleteCompleted()

    @Query("DELETE FROM download_history WHERE status = 4 OR status = 5") // failed or cancelled
    suspend fun deleteFailedAndCancelled()

    @Query("SELECT COUNT(*) FROM download_history WHERE status IN (0, 1)") // pending or downloading
    suspend fun getActiveCount(): Int

    @Query("SELECT * FROM download_history WHERE status IN (0, 1) ORDER BY createdAt ASC")
    suspend fun getActiveDownloads(): List<DownloadEntity>
}
