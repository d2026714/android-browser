package com.example.browser.data.model

import java.util.UUID

data class ReadingItem(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val title: String,
    val addedAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
