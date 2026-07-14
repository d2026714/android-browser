package com.example.browser.data.local.dao

import androidx.room.*
import com.example.browser.data.local.entity.BookmarkFolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkFolderDao {
    @Query("SELECT * FROM bookmark_folders ORDER BY name ASC")
    fun getAllFlow(): Flow<List<BookmarkFolderEntity>>

    @Query("SELECT * FROM bookmark_folders ORDER BY name ASC")
    suspend fun getAll(): List<BookmarkFolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: BookmarkFolderEntity)

    @Query("DELETE FROM bookmark_folders WHERE id = :id")
    suspend fun deleteById(id: String)
}
