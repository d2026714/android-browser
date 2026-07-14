package com.example.browser.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tab_groups")
data class TabGroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val tabIdsJson: String = "[]",
    val color: Long = 0xFF2196F3,
    val createdAt: Long = System.currentTimeMillis()
)
