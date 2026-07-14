package com.example.browser.data.local.dao

import androidx.room.*
import com.example.browser.data.local.entity.QuickLinkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuickLinkDao {
    @Query("SELECT * FROM quick_links ORDER BY position ASC")
    fun getAllFlow(): Flow<List<QuickLinkEntity>>

    @Query("SELECT * FROM quick_links ORDER BY position ASC")
    suspend fun getAll(): List<QuickLinkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(link: QuickLinkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(links: List<QuickLinkEntity>)

    @Query("DELETE FROM quick_links WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM quick_links")
    suspend fun deleteAll()
}
