package com.example.browser.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.browser.data.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BrowserRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("browser_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    // --- Bookmarks ---
    fun getBookmarks(): List<Bookmark> {
        val data = prefs.getString(KEY_BOOKMARKS, null) ?: return emptyList()
        return try { json.decodeFromString(data) } catch (_: Exception) { emptyList() }
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

    fun isBookmarked(url: String): Boolean = getBookmarks().any { it.url == url }

    // --- Bookmark Folders ---
    fun getBookmarkFolders(): List<BookmarkFolder> {
        val data = prefs.getString(KEY_BOOKMARK_FOLDERS, null) ?: return emptyList()
        return try { json.decodeFromString(data) } catch (_: Exception) { emptyList() }
    }

    fun addBookmarkFolder(folder: BookmarkFolder) {
        val folders = getBookmarkFolders().toMutableList()
        folders.add(folder)
        prefs.edit().putString(KEY_BOOKMARK_FOLDERS, json.encodeToString(folders)).apply()
    }

    fun removeBookmarkFolder(id: String) {
        val folders = getBookmarkFolders().toMutableList()
        folders.removeAll { it.id == id }
        prefs.edit().putString(KEY_BOOKMARK_FOLDERS, json.encodeToString(folders)).apply()
    }

    // --- History ---
    fun getHistory(): List<HistoryItem> {
        val data = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try { json.decodeFromString(data) } catch (_: Exception) { emptyList() }
    }

    fun addHistory(item: HistoryItem) {
        val history = getHistory().toMutableList()
        history.removeAll { it.url == item.url }
        history.add(0, item)
        if (history.size > 500) history.subList(500, history.size).clear()
        prefs.edit().putString(KEY_HISTORY, json.encodeToString(history)).apply()
    }

    fun clearHistory() { prefs.edit().remove(KEY_HISTORY).apply() }

    // --- Reading List ---
    fun getReadingList(): List<ReadingItem> {
        val data = prefs.getString(KEY_READING_LIST, null) ?: return emptyList()
        return try { json.decodeFromString(data) } catch (_: Exception) { emptyList() }
    }

    fun addToReadingList(item: ReadingItem) {
        val list = getReadingList().toMutableList()
        if (list.none { it.url == item.url }) {
            list.add(0, item)
            prefs.edit().putString(KEY_READING_LIST, json.encodeToString(list)).apply()
        }
    }

    fun removeFromReadingList(url: String) {
        val list = getReadingList().toMutableList()
        list.removeAll { it.url == url }
        prefs.edit().putString(KEY_READING_LIST, json.encodeToString(list)).apply()
    }

    fun markReadingItemRead(url: String) {
        val list = getReadingList().toMutableList()
        val idx = list.indexOfFirst { it.url == url }
        if (idx >= 0) {
            list[idx] = list[idx].copy(isRead = true)
            prefs.edit().putString(KEY_READING_LIST, json.encodeToString(list)).apply()
        }
    }

    // --- Quick Links ---
    fun getQuickLinks(): List<QuickLink> {
        val data = prefs.getString(KEY_QUICK_LINKS, null) ?: return defaultQuickLinks()
        return try { json.decodeFromString(data) } catch (_: Exception) { defaultQuickLinks() }
    }

    fun saveQuickLinks(links: List<QuickLink>) {
        prefs.edit().putString(KEY_QUICK_LINKS, json.encodeToString(links)).apply()
    }

    fun addQuickLink(link: QuickLink) {
        val links = getQuickLinks().toMutableList()
        links.add(link)
        saveQuickLinks(links)
    }

    fun removeQuickLink(id: String) {
        val links = getQuickLinks().toMutableList()
        links.removeAll { it.id == id }
        saveQuickLinks(links)
    }

    fun updateQuickLink(link: QuickLink) {
        val links = getQuickLinks().toMutableList()
        val idx = links.indexOfFirst { it.id == link.id }
        if (idx >= 0) {
            links[idx] = link
            saveQuickLinks(links)
        }
    }

    private fun defaultQuickLinks(): List<QuickLink> = listOf(
        QuickLink(title = "Google", url = "https://www.google.com", icon = "search", position = 0),
        QuickLink(title = "YouTube", url = "https://www.youtube.com", icon = "play", position = 1),
        QuickLink(title = "Wikipedia", url = "https://www.wikipedia.org", icon = "book", position = 2),
        QuickLink(title = "GitHub", url = "https://github.com", icon = "code", position = 3),
        QuickLink(title = "Reddit", url = "https://www.reddit.com", icon = "forum", position = 4),
        QuickLink(title = "Twitter", url = "https://twitter.com", icon = "tag", position = 5),
        QuickLink(title = "Amazon", url = "https://www.amazon.com", icon = "cart", position = 6),
        QuickLink(title = "Netflix", url = "https://www.netflix.com", icon = "movie", position = 7),
    )

    // --- Tab Groups ---
    fun getTabGroups(): List<TabGroup> {
        val data = prefs.getString(KEY_TAB_GROUPS, null) ?: return emptyList()
        return try { json.decodeFromString(data) } catch (_: Exception) { emptyList() }
    }

    fun saveTabGroups(groups: List<TabGroup>) {
        prefs.edit().putString(KEY_TAB_GROUPS, json.encodeToString(groups)).apply()
    }

    fun addTabGroup(group: TabGroup) {
        val groups = getTabGroups().toMutableList()
        groups.add(group)
        saveTabGroups(groups)
    }

    fun removeTabGroup(id: String) {
        val groups = getTabGroups().toMutableList()
        groups.removeAll { it.id == id }
        saveTabGroups(groups)
    }

    // --- Settings ---
    fun isDarkMode(): Boolean = prefs.getBoolean(KEY_DARK_MODE, false)
    fun setDarkMode(enabled: Boolean) = prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()

    fun isAmoledDark(): Boolean = prefs.getBoolean(KEY_AMOLED_DARK, false)
    fun setAmoledDark(enabled: Boolean) = prefs.edit().putBoolean(KEY_AMOLED_DARK, enabled).apply()

    fun isAdBlockEnabled(): Boolean = prefs.getBoolean(KEY_AD_BLOCK, true)
    fun setAdBlockEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_AD_BLOCK, enabled).apply()

    fun getSearchEngine(): String = prefs.getString(KEY_SEARCH_ENGINE, "https://www.google.com/search?q=")!!
    fun setSearchEngine(url: String) = prefs.edit().putString(KEY_SEARCH_ENGINE, url).apply()

    fun isSearchSuggestionsEnabled(): Boolean = prefs.getBoolean(KEY_SEARCH_SUGGESTIONS, true)
    fun setSearchSuggestionsEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_SEARCH_SUGGESTIONS, enabled).apply()

    companion object {
        private const val KEY_BOOKMARKS = "bookmarks"
        private const val KEY_BOOKMARK_FOLDERS = "bookmark_folders"
        private const val KEY_HISTORY = "history"
        private const val KEY_READING_LIST = "reading_list"
        private const val KEY_QUICK_LINKS = "quick_links"
        private const val KEY_TAB_GROUPS = "tab_groups"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_AMOLED_DARK = "amoled_dark"
        private const val KEY_AD_BLOCK = "ad_block"
        private const val KEY_SEARCH_ENGINE = "search_engine"
        private const val KEY_SEARCH_SUGGESTIONS = "search_suggestions"

        @Volatile
        private var instance: BrowserRepository? = null

        fun getInstance(context: Context): BrowserRepository {
            return instance ?: synchronized(this) {
                instance ?: BrowserRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
