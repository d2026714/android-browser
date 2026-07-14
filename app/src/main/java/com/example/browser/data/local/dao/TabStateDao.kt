package com.example.browser.data.local.dao

import androidx.room.*
import com.example.browser.data.local.entity.TabStateEntity

@Dao
interface TabStateDao {
    @Query("SELECT * FROM tab_state ORDER BY rowid ASC")
    suspend fun getAll(): List<TabStateEntity>

    @Query("SELECT * FROM tab_state WHERE isActive = 1 LIMIT 1")
    suspend fun getLastActive(): TabStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tabs: List<TabStateEntity>)

    @Query("DELETE FROM tab_state")
    suspend fun deleteAll()
}
