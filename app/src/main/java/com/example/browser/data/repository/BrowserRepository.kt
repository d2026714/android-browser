package com.example.browser.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.browser.data.model.Bookmark
import com.example.browser.data.model.HistoryItem
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BrowserRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("browser_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    // --- Bookmarks ---
    fun getBookmarks(): List<Bookmark> {
        val data = prefs.getString(KEY_BOOKMARKS, null) ?: return emptyList()
        return try {
            json.decodeFromString(data)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addBookmark(bookmark: Bookmark) {
        val bookmarks = getBookmarks().toMutableList()
        if (bookmarks.none { it.url == bookmark.url }) {
            bookmarks.add(0, bookmark)
            prefs.edit().putString(KEY_BOOKMARKS, json.encodeToString(bookmarks)).apply()
        }
    }

    fun removeBookmark(url: String) {
        val bookmarks = getBookmarks().toMutableList()
        bookmarks.removeAll { it.url == url }
        prefs.edit().putString(KEY_BOOKMARKS, json.encodeToString(bookmarks)).apply()
    }

    fun isBookmarked(url: String): Boolean {
        return getBookmarks().any { it.url == url }
    }

    // --- History ---
    fun getHistory(): List<HistoryItem> {
        val data = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            json.decodeFromString(data)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addHistory(item: HistoryItem) {
        val history = getHistory().toMutableList()
        history.removeAll { it.url == item.url }
        history.add(0, item)
        if (history.size > 500) history.subList(500, history.size).clear()
        prefs.edit().putString(KEY_HISTORY, json.encodeToString(history)).apply()
    }

    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    // --- Settings ---
    fun isDarkMode(): Boolean = prefs.getBoolean(KEY_DARK_MODE, false)
    fun setDarkMode(enabled: Boolean) = prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()

    fun isAdBlockEnabled(): Boolean = prefs.getBoolean(KEY_AD_BLOCK, true)
    fun setAdBlockEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_AD_BLOCK, enabled).apply()

    fun getSearchEngine(): String = prefs.getString(KEY_SEARCH_ENGINE, "https://www.google.com/search?q=")!!
    fun setSearchEngine(url: String) = prefs.edit().putString(KEY_SEARCH_ENGINE, url).apply()

    companion object {
        private const val KEY_BOOKMARKS = "bookmarks"
        private const val KEY_HISTORY = "history"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_AD_BLOCK = "ad_block"
        private const val KEY_SEARCH_ENGINE = "search_engine"

        @Volatile
        private var instance: BrowserRepository? = null

        fun getInstance(context: Context): BrowserRepository {
            return instance ?: synchronized(this) {
                instance ?: BrowserRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
