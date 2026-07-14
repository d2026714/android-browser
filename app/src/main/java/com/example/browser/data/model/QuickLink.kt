package com.example.browser.data.model

import java.util.UUID

data class QuickLink(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val url: String,
    val icon: String = "link", // icon name
    val position: Int = 0
)
