package com.example.browser.data.local.dao

import androidx.room.*
import com.example.browser.data.local.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY visitedAt DESC LIMIT 500")
    fun getAllFlow(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history ORDER BY visitedAt DESC LIMIT 500")
    suspend fun getAll(): List<HistoryEntity>

    @Insert
    suspend fun insert(item: HistoryEntity)

    @Query("DELETE FROM history WHERE url = :url")
    suspend fun deleteByUrl(url: String)

    @Query("DELETE FROM history")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM history")
    suspend fun count(): Int

    @Query("SELECT url, title, COUNT(*) as visitCount FROM history GROUP BY url ORDER BY visitCount DESC LIMIT :limit")
    suspend fun getTopSites(limit: Int = 10): List<TopSite>
}

data class TopSite(
    val url: String,
    val title: String,
    val visitCount: Int
)
