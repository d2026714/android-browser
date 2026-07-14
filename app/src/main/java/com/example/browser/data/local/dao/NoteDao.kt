package com.example.browser.data.local.dao

import androidx.room.*
import com.example.browser.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getAllFlow(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    suspend fun getAll(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE url = :url ORDER BY createdAt ASC")
    fun getByUrlFlow(url: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE url = :url ORDER BY createdAt ASC")
    suspend fun getByUrl(url: String): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE content LIKE '%' || :query || '%' OR pageTitle LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    suspend fun search(query: String): List<NoteEntity>

    @Query("SELECT COUNT(*) FROM notes WHERE url = :url")
    suspend fun countByUrl(url: String): Int

    @Query("SELECT COUNT(*) FROM notes")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM notes WHERE url = :url")
    suspend fun deleteByUrl(url: String)

    @Query("DELETE FROM notes")
    suspend fun deleteAll()
}
