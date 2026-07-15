package com.example.browser.data.local.dao

import androidx.room.*
import com.example.browser.data.local.entity.NovelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NovelDao {
    @Query("SELECT * FROM novels ORDER BY lastReadTime DESC")
    fun getAllByLastRead(): Flow<List<NovelEntity>>

    @Query("SELECT * FROM novels ORDER BY lastUpdated DESC")
    fun getAllByLastUpdated(): Flow<List<NovelEntity>>

    @Query("SELECT * FROM novels WHERE id = :id")
    suspend fun getById(id: Long): NovelEntity?

    @Query("SELECT * FROM novels WHERE url = :url")
    suspend fun getByUrl(url: String): NovelEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM novels WHERE url = :url)")
    suspend fun existsByUrl(url: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(novel: NovelEntity): Long

    @Update
    suspend fun update(novel: NovelEntity)

    @Query("UPDATE novels SET lastReadChapterIndex = :chapterIndex, lastReadTime = :time WHERE id = :novelId")
    suspend fun updateReadProgress(novelId: Long, chapterIndex: Int, time: Long = System.currentTimeMillis())

    @Query("UPDATE novels SET totalChapters = :total, lastUpdated = :time WHERE id = :novelId")
    suspend fun updateChapterCount(novelId: Long, total: Int, time: Long = System.currentTimeMillis())

    @Query("DELETE FROM novels WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM novels WHERE url = :url")
    suspend fun deleteByUrl(url: String)
}
