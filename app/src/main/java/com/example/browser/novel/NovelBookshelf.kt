package com.example.browser.novel

import android.util.Log
import com.example.browser.data.local.dao.ChapterDao
import com.example.browser.data.local.dao.NovelDao
import com.example.browser.data.local.entity.ChapterEntity
import com.example.browser.data.local.entity.NovelEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Manages the novel bookshelf: adding, removing, tracking progress, checking updates.
 */
class NovelBookshelf(
    private val novelDao: NovelDao,
    private val chapterDao: ChapterDao,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "NovelBookshelf"
    }

    /** All novels sorted by last read time */
    val novelsByLastRead: Flow<List<NovelEntity>> = novelDao.getAllByLastRead()

    /** All novels sorted by last updated */
    val novelsByLastUpdated: Flow<List<NovelEntity>> = novelDao.getAllByLastUpdated()

    /** Add a novel to the bookshelf */
    fun addNovel(
        title: String,
        author: String = "",
        url: String,
        coverUrl: String = "",
        chapters: List<ChapterParser.ChapterInfo> = emptyList(),
        onAdded: ((Long) -> Unit)? = null
    ) {
        scope.launch {
            try {
                // Check if already exists
                val existing = novelDao.getByUrl(url)
                if (existing != null) {
                    // Update chapter count if we have more info
                    if (chapters.size > existing.totalChapters) {
                        novelDao.updateChapterCount(existing.id, chapters.size)
                        // Update chapter list
                        saveChapterList(existing.id, chapters)
                    }
                    onAdded?.invoke(existing.id)
                    return@launch
                }

                val novel = NovelEntity(
                    title = title,
                    author = author,
                    url = url,
                    coverUrl = coverUrl,
                    totalChapters = chapters.size
                )
                val novelId = novelDao.insert(novel)
                Log.d(TAG, "Added novel: $title (id=$novelId, chapters=${chapters.size})")

                // Save chapter list
                if (chapters.isNotEmpty()) {
                    saveChapterList(novelId, chapters)
                }
                onAdded?.invoke(novelId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add novel: $title", e)
            }
        }
    }

    /** Remove a novel and its chapters from the bookshelf */
    fun removeNovel(novelId: Long) {
        scope.launch {
            try {
                novelDao.deleteById(novelId)
                Log.d(TAG, "Removed novel: $novelId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove novel: $novelId", e)
            }
        }
    }

    /** Update reading progress for a novel */
    fun updateReadProgress(novelId: Long, chapterIndex: Int) {
        scope.launch {
            try {
                novelDao.updateReadProgress(novelId, chapterIndex)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update read progress", e)
            }
        }
    }

    /** Get a novel by ID */
    suspend fun getNovel(novelId: Long): NovelEntity? {
        return novelDao.getById(novelId)
    }

    /** Get chapters for a novel */
    fun getChapters(novelId: Long): Flow<List<ChapterEntity>> {
        return chapterDao.getChaptersByNovel(novelId)
    }

    /** Get chapters as a list (non-Flow) */
    suspend fun getChapterList(novelId: Long): List<ChapterEntity> {
        return chapterDao.getChaptersByNovelList(novelId)
    }

    /** Get a specific chapter */
    suspend fun getChapter(novelId: Long, chapterIndex: Int): ChapterEntity? {
        return chapterDao.getChapter(novelId, chapterIndex)
    }

    /** Get a cached chapter (for offline reading) */
    suspend fun getCachedChapter(novelId: Long, chapterIndex: Int): ChapterEntity? {
        return chapterDao.getCachedChapter(novelId, chapterIndex)
    }

    /**
     * Check for updates: compare stored chapter count with freshly scraped count.
     * Returns the number of new chapters, or 0 if no update.
     */
    suspend fun checkForUpdates(novelId: Long, freshChapterCount: Int): Int {
        return try {
            val novel = novelDao.getById(novelId) ?: return 0
            val diff = freshChapterCount - novel.totalChapters
            if (diff > 0) {
                novelDao.updateChapterCount(novelId, freshChapterCount)
                Log.d(TAG, "Novel ${novel.title} has $diff new chapters")
            }
            diff.coerceAtLeast(0)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check updates", e)
            0
        }
    }

    /** Update the full chapter list for a novel */
    fun updateChapterList(novelId: Long, chapters: List<ChapterParser.ChapterInfo>) {
        scope.launch {
            try {
                saveChapterList(novelId, chapters)
                novelDao.updateChapterCount(novelId, chapters.size)
                Log.d(TAG, "Updated chapter list for novel $novelId: ${chapters.size} chapters")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update chapter list", e)
            }
        }
    }

    /** Get cached chapter count */
    suspend fun getCachedCount(novelId: Long): Int {
        return chapterDao.getCachedChapterCount(novelId)
    }

    /** Clear cached content for a novel (keeps chapter metadata) */
    fun clearCache(novelId: Long) {
        scope.launch {
            try {
                chapterDao.clearCache(novelId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear cache", e)
            }
        }
    }

    /** Check if a URL is already on the bookshelf */
    suspend fun isOnBookshelf(url: String): Boolean {
        return novelDao.existsByUrl(url)
    }

    private suspend fun saveChapterList(novelId: Long, chapters: List<ChapterParser.ChapterInfo>) {
        val entities = chapters.map { ch ->
            ChapterEntity(
                novelId = novelId,
                chapterIndex = ch.index,
                title = ch.title,
                url = ch.url
            )
        }
        // Delete old chapters and insert new ones
        chapterDao.deleteByNovel(novelId)
        chapterDao.insertAll(entities)
    }
}
