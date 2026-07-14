package com.example.browser.data.local.dao

import androidx.room.*
import com.example.browser.data.local.entity.ReadingItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingListDao {
    @Query("SELECT * FROM reading_list ORDER BY addedAt DESC")
    fun getAllFlow(): Flow<List<ReadingItemEntity>>

    @Query("SELECT * FROM reading_list ORDER BY addedAt DESC")
    suspend fun getAll(): List<ReadingItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ReadingItemEntity)

    @Query("DELETE FROM reading_list WHERE url = :url")
    suspend fun deleteByUrl(url: String)

    @Query("UPDATE reading_list SET isRead = 1 WHERE url = :url")
    suspend fun markRead(url: String)
}
