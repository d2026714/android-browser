package com.example.browser.data.local.dao

import androidx.room.*
import com.example.browser.data.local.entity.TabGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TabGroupDao {
    @Query("SELECT * FROM tab_groups ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<TabGroupEntity>>

    @Query("SELECT * FROM tab_groups ORDER BY createdAt DESC")
    suspend fun getAll(): List<TabGroupEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(group: TabGroupEntity)

    @Query("DELETE FROM tab_groups WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM tab_groups")
    suspend fun deleteAll()
}
