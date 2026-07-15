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
            val q = URLEncoder.encode(query, "UTF-8")
            val conn = URL("https://suggestqueries.google.com/complete/search?client=firefox&q=$q")
                .openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            conn.connectTimeout = 2000
            conn.readTimeout = 2000
            if (conn.responseCode == 200) {
                val arr = JSONArray(conn.inputStream.bufferedReader().readText())
                val list = arr.getJSONArray(1)
                (0 until list.length()).map { list.getString(it) }
            } else emptyList()
        } catch (_: Exception) { emptyList() }
    }
}
