package com.example.browser.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_list")
data class ReadingItemEntity(
    @PrimaryKey val url: String,
    val title: String,
    val addedAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
