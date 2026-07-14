package com.example.browser.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quick_links")
data class QuickLinkEntity(
    @PrimaryKey val id: String,
    val title: String,
    val url: String,
    val icon: String = "link",
    val position: Int = 0
)
