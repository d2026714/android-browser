package com.example.browser.data.model

import java.util.UUID

data class Bookmark(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val title: String,
    val favicon: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
