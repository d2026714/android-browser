package com.example.browser.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val url: String,
    val title: String,
    val favicon: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
