package com.example.browser.web

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object SearchSuggestions {

    suspend fun fetch(query: String): List<String> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = URL("https://suggestqueries.google.com/complete/search?client=firefox&q=$encoded")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            conn.connectTimeout = 3000
            conn.readTimeout = 3000

            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().readText()
                val arr = JSONArray(text)
                val suggestions = arr.getJSONArray(1)
                (0 until suggestions.length()).map { suggestions.getString(it) }
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
