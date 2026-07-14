package com.example.browser.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Built-in PDF viewer using Android's PdfRenderer.
 * Downloads PDF to temp file and renders pages as bitmaps.
 */
class PdfViewer(private val context: Context) {

    data class PdfDocument(
        val file: File,
        val pageCount: Int,
        val title: String
    )

    data class PdfPage(
        val index: Int,
        val bitmap: Bitmap,
        val width: Int,
        val height: Int
    )

    private var currentPdf: PdfDocument? = null
    private var renderer: PdfRenderer? = null

    suspend fun loadFromUrl(url: String): PdfDocument? {
        return withContext(Dispatchers.IO) {
            try {
                val file = downloadPdf(url)
                if (file != null) {
                    loadFromFile(file, url.substringAfterLast("/").substringBefore("?"))
                } else null
            } catch (_: Exception) { null }
        }
    }

    fun loadFromFile(file: File, title: String = file.name): PdfDocument? {
        return try {
            val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(fd)
            val doc = PdfDocument(
                file = file,
                pageCount = renderer?.pageCount ?: 0,
                title = title
            )
            currentPdf = doc
            doc
        } catch (_: Exception) { null }
    }

    fun renderPage(pageIndex: Int, width: Int = 1080): PdfPage? {
        val r = renderer ?: return null
        if (pageIndex < 0 || pageIndex >= r.pageCount) return null

        return try {
            val page = r.openPage(pageIndex)
            val ratio = page.height.toFloat() / page.width.toFloat()
            val height = (width * ratio).toInt()
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            PdfPage(pageIndex, bitmap, width, height)
        } catch (_: Exception) { null }
    }

    fun getPageCount(): Int = currentPdf?.pageCount ?: 0

    fun getTitle(): String = currentPdf?.title ?: "PDF Document"

    fun close() {
        renderer?.close()
        renderer = null
        currentPdf = null
    }

    private suspend fun downloadPdf(url: String): File? {
        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                connection.connectTimeout = 10000
                connection.readTimeout = 30000

                val dir = File(context.cacheDir, "pdfs")
                dir.mkdirs()
                val file = File(dir, "temp_${System.currentTimeMillis()}.pdf")

                connection.inputStream.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }

                if (file.length() > 0) file else null
            } catch (_: Exception) { null }
        }
    }

    fun isPdfUrl(url: String): Boolean {
        return url.lowercase().endsWith(".pdf") || url.contains("pdf", ignoreCase = true)
    }

    companion object {
        @Volatile
        private var instance: PdfViewer? = null

        fun getInstance(context: Context): PdfViewer {
            return instance ?: synchronized(this) {
                instance ?: PdfViewer(context.applicationContext).also { instance = it }
            }
        }
    }
}
