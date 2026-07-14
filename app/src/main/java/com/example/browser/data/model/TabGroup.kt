package com.example.browser.data.model

import java.util.UUID

data class TabGroup(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val tabIds: List<String> = emptyList(),
    val color: Long = 0xFF2196F3, // Blue
    val createdAt: Long = System.currentTimeMillis()
)
