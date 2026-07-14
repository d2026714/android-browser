package com.example.browser.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tab_state")
data class TabStateEntity(
    @PrimaryKey val tabId: String,
    val url: String,
    val title: String,
    val isIncognito: Boolean = false,
    val isActive: Boolean = false
)
