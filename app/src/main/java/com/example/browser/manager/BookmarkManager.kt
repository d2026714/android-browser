package com.example.browser.manager

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.browser.data.local.dao.*
import com.example.browser.data.local.entity.*
import com.example.browser.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "BookmarkManager"

class BookmarkManager(
    private val bookmarkDao: BookmarkDao,
    private val historyDao: HistoryDao,
    private val readingListDao: ReadingListDao,
    private val tabGroupDao: TabGroupDao,
    private val bookmarkFolderDao: BookmarkFolderDao? = null,
    private val scope: CoroutineScope,
    private val context: Context? = null
) {
    init {
        // Migrate SharedPreferences data to Room on first run
        if (context != null) {
            scope.launch { migrateFromSharedPreferences(context) }
        }
    }

    // --- Bookmarks ---

    val bookmarks: Flow<List<Bookmark>> = bookmarkDao.getAllFlow().map { list ->
        list.map { Bookmark(url = it.url, title = it.title, favicon = it.favicon, createdAt = it.createdAt) }
    }

    suspend fun isBookmarked(url: String): Boolean = bookmarkDao.isBookmarked(url)

    fun addBookmark(url: String, title: String) {
        scope.launch {
            try {
                bookmarkDao.insert(BookmarkEntity(url = url, title = title))
                Log.d(TAG, "Bookmarked: $url")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to bookmark $url", e)
            }
        }
    }

    fun removeBookmark(url: String) {
        scope.launch {
            try {
                bookmarkDao.deleteByUrl(url)
                Log.d(TAG, "Removed bookmark: $url")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove bookmark $url", e)
            }
        }
    }

    fun toggleBookmark(url: String, title: String) {
        scope.launch {
            if (bookmarkDao.isBookmarked(url)) {
                bookmarkDao.deleteByUrl(url)
                Log.d(TAG, "Unbookmarked: $url")
            } else {
                bookmarkDao.insert(BookmarkEntity(url = url, title = title))
                Log.d(TAG, "Bookmarked: $url")
            }
        }
    }

    // --- Bookmark Folders ---

    val bookmarkFolders: Flow<List<BookmarkFolder>> =
        (bookmarkFolderDao?.getAllFlow() ?: kotlinx.coroutines.flow.flowOf(emptyList())).map { list ->
            list.map { BookmarkFolder(id = it.id, name = it.name, parentId = it.parentId, createdAt = it.createdAt) }
        }

    fun addBookmarkFolder(name: String, parentId: String? = null) {
        scope.launch {
            try {
                bookmarkFolderDao?.insert(
                    BookmarkFolderEntity(id = java.util.UUID.randomUUID().toString(), name = name, parentId = parentId)
                )
                Log.d(TAG, "Added bookmark folder: $name")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add bookmark folder", e)
            }
        }
    }

    fun removeBookmarkFolder(id: String) {
        scope.launch {
            try {
                bookmarkFolderDao?.deleteById(id)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove bookmark folder $id", e)
            }
        }
    }

    // --- History ---

    val history: Flow<List<HistoryItem>> = historyDao.getAllFlow().map { list ->
        list.map { HistoryItem(url = it.url, title = it.title, visitedAt = it.visitedAt) }
    }

    fun addHistory(url: String, title: String) {
        scope.launch {
            try {
                historyDao.deleteByUrl(url)
                historyDao.insert(HistoryEntity(url = url, title = title))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add history for $url", e)
            }
        }
    }

    fun clearHistory() {
        scope.launch {
            try {
                historyDao.deleteAll()
                Log.d(TAG, "History cleared")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear history", e)
            }
        }
    }

    suspend fun getTopSites(limit: Int = 10): List<TopSite> {
        return try {
            historyDao.getTopSites(limit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get top sites", e)
            emptyList()
        }
    }

    // --- History search ---

    fun searchHistory(query: String): Flow<List<HistoryItem>> {
        return historyDao.getAllFlow().map { list ->
            list.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.url.contains(query, ignoreCase = true)
            }.map { HistoryItem(url = it.url, title = it.title, visitedAt = it.visitedAt) }
        }
    }

    // --- Reading List ---

    val readingList: Flow<List<ReadingItem>> = readingListDao.getAllFlow().map { list ->
        list.map { ReadingItem(url = it.url, title = it.title, addedAt = it.addedAt, isRead = it.isRead) }
    }

    fun addToReadingList(url: String, title: String) {
        scope.launch {
            try {
                readingListDao.insert(ReadingItemEntity(url = url, title = title))
                Log.d(TAG, "Added to reading list: $url")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add to reading list: $url", e)
            }
        }
    }

    fun removeFromReadingList(url: String) {
        scope.launch {
            try {
                readingListDao.deleteByUrl(url)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove from reading list: $url", e)
            }
        }
    }

    fun markReadingItemRead(url: String) {
        scope.launch {
            try {
                readingListDao.markRead(url)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark reading item read: $url", e)
            }
        }
    }

    // --- Tab Groups ---

    val tabGroups: Flow<List<TabGroup>> = tabGroupDao.getAllFlow().map { list ->
        list.map {
            val tabIds = try {
                Json.decodeFromString<List<String>>(it.tabIdsJson)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse tabIds JSON", e)
                emptyList()
            }
            TabGroup(id = it.id, name = it.name, tabIds = tabIds, color = it.color, createdAt = it.createdAt)
        }
    }

    fun addTabGroup(name: String, tabIds: List<String>) {
        scope.launch {
            try {
                val group = TabGroup(name = name, tabIds = tabIds)
                tabGroupDao.insert(
                    TabGroupEntity(
                        id = group.id,
                        name = group.name,
                        tabIdsJson = Json.encodeToString(tabIds),
                        color = group.color,
                        createdAt = group.createdAt
                    )
                )
                Log.d(TAG, "Added tab group: $name with ${tabIds.size} tabs")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add tab group", e)
            }
        }
    }

    fun removeTabGroup(id: String) {
        scope.launch {
            try {
                tabGroupDao.deleteById(id)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove tab group $id", e)
            }
        }
    }

    // --- SP → Room migration ---

    private suspend fun migrateFromSharedPreferences(context: Context) {
        try {
            val prefs: SharedPreferences =
                context.getSharedPreferences("browser_prefs", Context.MODE_PRIVATE)

            // Check if migration needed
            if (!prefs.contains("bookmarks") && !prefs.contains("history")) {
                Log.d(TAG, "No SP data to migrate")
                return
            }

            // Check if Room already has data
            if (bookmarkDao.getAll().isNotEmpty()) {
                Log.d(TAG, "Room already has data, clearing SP")
                prefs.edit().clear().apply()
                return
            }

            Log.d(TAG, "Migrating SP data to Room...")

            // Migrate bookmarks
            val bookmarksJson = prefs.getString("bookmarks", null)
            if (bookmarksJson != null) {
                try {
                    val bookmarks = Json.decodeFromString<List<Bookmark>>(bookmarksJson)
                    bookmarks.forEach { b ->
                        bookmarkDao.insert(BookmarkEntity(url = b.url, title = b.title, createdAt = b.createdAt))
                    }
                    Log.d(TAG, "Migrated ${bookmarks.size} bookmarks")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to migrate bookmarks", e)
                }
            }

            // Migrate history
            val historyJson = prefs.getString("history", null)
            if (historyJson != null) {
                try {
                    val items = Json.decodeFromString<List<HistoryItem>>(historyJson)
                    items.forEach { item ->
                        historyDao.insert(HistoryEntity(url = item.url, title = item.title, visitedAt = item.visitedAt))
                    }
                    Log.d(TAG, "Migrated ${items.size} history items")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to migrate history", e)
                }
            }

            // Migrate reading list
            val readingJson = prefs.getString("reading_list", null)
            if (readingJson != null) {
                try {
                    val items = Json.decodeFromString<List<ReadingItem>>(readingJson)
                    items.forEach { item ->
                        readingListDao.insert(ReadingItemEntity(url = item.url, title = item.title, addedAt = item.addedAt, isRead = item.isRead))
                    }
                    Log.d(TAG, "Migrated ${items.size} reading list items")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to migrate reading list", e)
                }
            }

            // Migrate quick links
            val quickLinksJson = prefs.getString("quick_links", null)
            if (quickLinksJson != null) {
                try {
                    // Quick links migration handled by SettingsManager
                } catch (_: Exception) {}
            }

            // Clear SP after successful migration
            prefs.edit().clear().apply()
            Log.d(TAG, "SP migration complete, cleared old data")
        } catch (e: Exception) {
            Log.e(TAG, "SP migration failed", e)
        }
    }
}
