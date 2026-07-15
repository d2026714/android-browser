package com.example.browser.util

import android.content.Context
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Long.toFormattedDate(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(this))
}

fun String.isValidUrl(): Boolean {
    return this.startsWith("http://") || this.startsWith("https://")
}

fun String.toSearchUrl(engine: String = "https://www.google.com/search?q="): String {
    return if (this.isValidUrl()) {
        this
    } else {
        "$engine${java.net.URLEncoder.encode(this, "UTF-8")}"
    }
}

enum class SearchEngine(val displayName: String, val baseUrl: String) {
    GOOGLE("Google", "https://www.google.com/search?q="),
    BING("Bing", "https://www.bing.com/search?q="),
    BAIDU("百度", "https://www.baidu.com/s?wd="),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q="),
}
