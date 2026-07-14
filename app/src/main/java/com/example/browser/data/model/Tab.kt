package com.example.browser.data.model

import java.util.UUID

data class Tab(
    val id: String = UUID.randomUUID().toString(),
    val url: String = "about:blank",
    val title: String = "New Tab",
    val isActive: Boolean = false,
    val isIncognito: Boolean = false
)
