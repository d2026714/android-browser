package com.example.browser.util

import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Long.toFormattedDate(): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(this))

fun String.toSearchUrl(engine: String): String =
    if (startsWith("http://") || startsWith("https://")) this
    else "$engine${URLEncoder.encode(this, "UTF-8")}"

enum class SearchEngine(val displayName: String, val baseUrl: String) {
    GOOGLE("Google", "https://www.google.com/search?q="),
    BING("Bing", "https://www.bing.com/search?q="),
    BAIDU("百度", "https://www.baidu.com/s?wd="),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q="),
}
