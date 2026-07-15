package com.example.browser.novel

import android.util.Log
import com.example.browser.data.local.dao.ChapterDao
import com.example.browser.data.local.entity.ChapterEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Downloads and caches chapter content for offline reading.
 */
class ChapterCache(
    private val chapterDao: ChapterDao,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "ChapterCache"
        private const val TIMEOUT_SECONDS = 30L
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    /**
     * Cache a single chapter's content.
     * If the chapter already has content in the DB, skip downloading.
     */
    fun cacheChapter(chapter: ChapterEntity, onResult: (Boolean) -> Unit = {}) {
        scope.launch {
            try {
                if (chapter.isCached && chapter.content.isNotBlank()) {
                    onResult(true)
                    return@launch
                }

                if (chapter.url.isBlank()) {
                    Log.w(TAG, "Chapter has no URL: ${chapter.title}")
                    onResult(false)
                    return@launch
                }

                val content = downloadChapterContent(chapter.url)
                if (content.isNotBlank()) {
                    chapterDao.updateContent(chapter.id, content)
                    Log.d(TAG, "Cached chapter: ${chapter.title} (${content.length} chars)")
                    onResult(true)
                } else {
                    Log.w(TAG, "Empty content for chapter: ${chapter.title}")
                    onResult(false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to cache chapter: ${chapter.title}", e)
                onResult(false)
            }
        }
    }

    /**
     * Cache all chapters for a novel (batch download).
     * Reports progress via callback.
     */
    fun cacheAllChapters(
        novelId: Long,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> },
        onComplete: (cached: Int, failed: Int) -> Unit = { _, _ -> }
    ) {
        scope.launch {
            try {
                val chapters = chapterDao.getChaptersByNovelList(novelId)
                val uncached = chapters.filter { !it.isCached || it.content.isBlank() }

                if (uncached.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        onComplete(chapters.size, 0)
                    }
                    return@launch
                }

                var cached = 0
                var failed = 0

                for ((i, chapter) in uncached.withIndex()) {
                    withContext(Dispatchers.Main) {
                        onProgress(i + 1, uncached.size)
                    }

                    try {
                        if (chapter.url.isBlank()) {
                            failed++
                            continue
                        }
                        val content = downloadChapterContent(chapter.url)
                        if (content.isNotBlank()) {
                            chapterDao.updateContent(chapter.id, content)
                            cached++
                        } else {
                            failed++
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to cache: ${chapter.title}", e)
                        failed++
                    }
                }

                Log.d(TAG, "Batch cache complete: $cached cached, $failed failed out of ${uncached.size}")
                withContext(Dispatchers.Main) {
                    onComplete(cached, failed)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Batch cache failed", e)
                withContext(Dispatchers.Main) {
                    onComplete(0, 0)
                }
            }
        }
    }

    /**
     * Pre-cache the next N chapters from a given index.
     * Useful for auto-caching while reading.
     */
    fun preCacheNext(novelId: Long, fromIndex: Int, count: Int = 2) {
        scope.launch {
            try {
                for (i in fromIndex + 1..fromIndex + count) {
                    val chapter = chapterDao.getChapter(novelId, i) ?: continue
                    if (!chapter.isCached && chapter.url.isNotBlank()) {
                        val content = downloadChapterContent(chapter.url)
                        if (content.isNotBlank()) {
                            chapterDao.updateContent(chapter.id, content)
                            Log.d(TAG, "Pre-cached chapter: ${chapter.title}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Pre-cache failed", e)
            }
        }
    }

    /**
     * Download chapter content from URL.
     * Returns plain text content.
     */
    private suspend fun downloadChapterContent(url: String): String = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "HTTP ${response.code} for $url")
                return@withContext ""
            }

            val html = response.body?.string() ?: ""
            extractTextFromHtml(html)
        } catch (e: Exception) {
            Log.e(TAG, "Download failed for $url", e)
            ""
        }
    }

    /**
     * Extract readable text from HTML.
     * Removes scripts, styles, and other non-content elements.
     */
    private fun extractTextFromHtml(html: String): String {
        var text = html
        // Remove script and style blocks
        text = text.replace(Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("<style[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), "")
        // Remove HTML comments
        text = text.replace(Regex("<!--[\\s\\S]*?-->"), "")
        // Remove nav/header/footer
        text = text.replace(Regex("<(nav|header|footer|aside)[^>]*>[\\s\\S]*?</\\1>", RegexOption.IGNORE_CASE), "")
        // Replace <br> and <p> with newlines
        text = text.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        text = text.replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n\n")
        // Remove remaining HTML tags
        text = text.replace(Regex("<[^>]+>"), "")
        // Decode HTML entities
        text = text.replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
        // Clean up whitespace
        text = text.replace(Regex("[ \\t]+"), " ")
        text = text.replace(Regex("\\n{3,}"), "\n\n")
        text = text.trim()
        return text
    }
}
