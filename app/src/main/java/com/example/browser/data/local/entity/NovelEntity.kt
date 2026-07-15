package com.example.browser.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "novels")
data class NovelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val author: String = "",
    val url: String,
    val coverUrl: String = "",
    val lastReadChapterIndex: Int = 0,
    val totalChapters: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis(),
    val lastReadTime: Long = System.currentTimeMillis(),
    val addedTime: Long = System.currentTimeMillis()
)
