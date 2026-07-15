package com.example.browser.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tabs")
data class TabEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val url: String = "",
    val position: Int = 0,
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
