package com.example.browser.offline

import android.content.Context
import android.webkit.WebView
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@Serializable
data class OfflinePage(
    val id: String,
    val title: String,
    val url: String,
    val savedAt: Long,
    val htmlFileName: String,
    val fileSize: Long = 0
)

class OfflinePageManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("offline_pages", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    fun getOfflineDir(): File {
        val dir = File(context.filesDir, "offline_pages")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun savePage(url: String, title: String, webView: WebView, callback: (Boolean) -> Unit) {
        val id = "page_${dateFormat.format(Date())}"
        val fileName = "${id}.html"
        val pageDir = File(getOfflineDir(), id)
        if (!pageDir.exists()) pageDir.mkdirs()

        // Get page HTML
        webView.evaluateJavascript(
            "(function() { return document.documentElement.outerHTML; })()"
        ) { html ->
            try {
                val cleanedHtml = html?.removeSurrounding("\"")
                    ?.replace("\\n", "\n")
                    ?.replace("\\\"", "\"")
                    ?.replace("\\t", "\t")
                    ?: ""

                // Create a complete HTML file with base URL
                val fullHtml = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <base href="$url">
                        <meta charset="UTF-8">
                        <title>$title</title>
                        <style>
                            body { font-family: sans-serif; padding: 16px; max-width: 800px; margin: 0 auto; }
                            img { max-width: 100%; height: auto; }
                        </style>
                    </head>
                    <body>$cleanedHtml</body>
                    </html>
                """.trimIndent()

                val file = File(pageDir, fileName)
                FileOutputStream(file).use { fos -> fos.write(fullHtml.toByteArray()) }

                val page = OfflinePage(
                    id = id,
                    title = title,
                    url = url,
                    savedAt = System.currentTimeMillis(),
                    htmlFileName = fileName,
                    fileSize = file.length()
                )
                savePageMetadata(page)
                callback(true)
            } catch (_: Exception) {
                callback(false)
            }
        }
    }

    fun getSavedPages(): List<OfflinePage> {
        val data = prefs.getString(KEY_PAGES, null) ?: return emptyList()
        return try { json.decodeFromString(data) } catch (_: Exception) { emptyList() }
    }

    fun deletePage(id: String) {
        val dir = File(getOfflineDir(), id)
        dir.deleteRecursively()
        val pages = getSavedPages().toMutableList()
        pages.removeAll { it.id == id }
        prefs.edit().putString(KEY_PAGES, json.encodeToString(pages)).apply()
    }

    fun getPageFile(id: String): File? {
        val page = getSavedPages().find { it.id == id } ?: return null
        val file = File(getOfflineDir(), "${id}/${page.htmlFileName}")
        return if (file.exists()) file else null
    }

    fun getTotalSize(): Long {
        return getOfflineDir().walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    fun clearAll() {
        getOfflineDir().deleteRecursively()
        prefs.edit().remove(KEY_PAGES).apply()
    }

    private fun savePageMetadata(page: OfflinePage) {
        val pages = getSavedPages().toMutableList()
        pages.removeAll { it.url == page.url }
        pages.add(0, page)
        prefs.edit().putString(KEY_PAGES, json.encodeToString(pages)).apply()
    }

    companion object {
        private const val KEY_PAGES = "offline_pages_list"

        @Volatile
        private var instance: OfflinePageManager? = null

        fun getInstance(context: Context): OfflinePageManager {
            return instance ?: synchronized(this) {
                instance ?: OfflinePageManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
