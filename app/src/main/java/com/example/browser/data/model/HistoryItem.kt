package com.example.browser.data.model

import java.util.UUID

data class HistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val title: String,
    val visitedAt: Long = System.currentTimeMillis()
)
