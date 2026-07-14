package com.example.browser.manager

import android.util.Log
import com.example.browser.data.local.dao.BookmarkDao
import com.example.browser.data.local.dao.HistoryDao
import com.example.browser.data.local.dao.ReadingListDao
import com.example.browser.data.local.dao.TabGroupDao
import com.example.browser.data.local.entity.BookmarkEntity
import com.example.browser.data.local.entity.HistoryEntity
import com.example.browser.data.local.entity.ReadingItemEntity
import com.example.browser.data.local.entity.TabGroupEntity
import com.example.browser.data.model.Bookmark
import com.example.browser.data.model.HistoryItem
import com.example.browser.data.model.ReadingItem
import com.example.browser.data.model.TabGroup
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
    private val scope: CoroutineScope
) {
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

    // --- History ---

    val history: Flow<List<HistoryItem>> = historyDao.getAllFlow().map { list ->
        list.map { HistoryItem(url = it.url, title = it.title, visitedAt = it.visitedAt) }
    }

    fun addHistory(url: String, title: String) {
        scope.launch {
            try {
                // Remove duplicate then insert at top
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
            val tabIds = try { Json.decodeFromString<List<String>>(it.tabIdsJson) } catch (e: Exception) { Log.e(TAG, "Failed to parse tabIds JSON", e); emptyList() }
            TabGroup(id = it.id, name = it.name, tabIds = tabIds, color = it.color, createdAt = it.createdAt)
        }
    }

    fun addTabGroup(name: String, tabIds: List<String>) {
        scope.launch {
            try {
                val group = TabGroup(name = name, tabIds = tabIds)
                tabGroupDao.insert(TabGroupEntity(
                    id = group.id,
                    name = group.name,
                    tabIdsJson = Json.encodeToString(tabIds),
                    color = group.color,
                    createdAt = group.createdAt
                ))
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
}
