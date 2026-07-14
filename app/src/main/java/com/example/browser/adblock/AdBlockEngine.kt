package com.example.browser.adblock

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Advanced ad blocking engine with bundled filter lists.
 * Parses EasyList-style rules and blocks matching requests.
 */
class AdBlockEngine(private val context: Context) {
    private val domainRules = mutableSetOf<String>()
    private val urlPatterns = mutableListOf<String>()
    private val exceptionDomains = mutableSetOf<String>()
    private val exceptionPatterns = mutableListOf<String>()
    private var isLoaded = false

    fun loadFilters() {
        if (isLoaded) return
        try {
            // Load bundled EasyList
            loadFromAssets("adblock/easylist.txt")
            isLoaded = true
        } catch (_: Exception) {}
    }

    private fun loadFromAssets(fileName: String) {
        try {
            val input = context.assets.open(fileName)
            val reader = BufferedReader(InputStreamReader(input))
            reader.useLines { lines ->
                lines.forEach { line ->
                    parseLine(line.trim())
                }
            }
        } catch (_: Exception) {}
    }

    private fun parseLine(line: String) {
        if (line.isEmpty() || line.startsWith("!") || line.startsWith("[Adblock")) return

        // Exception rules (starts with @@)
        if (line.startsWith("@@")) {
            val rule = line.substring(2)
            if (rule.startsWith("||")) {
                exceptionDomains.add(rule.removeSurrounding("||", "^").removeSuffix("^"))
            } else {
                exceptionPatterns.add(rule)
            }
            return
        }

        // Domain rules (||domain^)
        if (line.startsWith("||") && line.endsWith("^")) {
            val domain = line.removeSurrounding("||", "^")
            domainRules.add(domain)
            return
        }

        // URL pattern rules
        if (line.startsWith("/") && line.endsWith("/")) {
            // Regex pattern - convert to simple contains check
            val pattern = line.removeSurrounding("/")
                .replace("\\", "")
                .replace("^", "")
                .replace("*", "")
                .replace("?", "")
            if (pattern.length > 3) urlPatterns.add(pattern)
            return
        }

        // Simple URL patterns
        if (line.contains("*") || line.contains("^") || line.startsWith("/") || line.contains("://")) {
            val cleaned = line.replace("*", "").replace("^", "")
            if (cleaned.length > 5) urlPatterns.add(cleaned)
        }
    }

    fun isAd(url: String): Boolean {
        if (!isLoaded) loadFilters()

        val lowerUrl = url.lowercase()

        // Check exceptions first
        for (domain in exceptionDomains) {
            if (lowerUrl.contains(domain)) return false
        }

        // Check domain rules
        for (domain in domainRules) {
            if (lowerUrl.contains(domain)) return true
        }

        // Check URL patterns
        for (pattern in urlPatterns) {
            if (lowerUrl.contains(pattern)) return true
        }

        return false
    }

    fun shouldIntercept(request: WebResourceRequest): WebResourceResponse? {
        if (isAd(request.url.toString())) {
            return WebResourceResponse("text/plain", "utf-8", "".byteInputStream())
        }
        return null
    }

    fun getStats(): AdBlockStats {
        return AdBlockStats(
            domainRules = domainRules.size,
            urlPatterns = urlPatterns.size,
            exceptionDomains = exceptionDomains.size,
            exceptionPatterns = exceptionPatterns.size,
            totalRules = domainRules.size + urlPatterns.size
        )
    }

    companion object {
        @Volatile
        private var instance: AdBlockEngine? = null

        fun getInstance(context: Context): AdBlockEngine {
            return instance ?: synchronized(this) {
                instance ?: AdBlockEngine(context.applicationContext).also { instance = it }
            }
        }
    }
}

data class AdBlockStats(
    val domainRules: Int,
    val urlPatterns: Int,
    val exceptionDomains: Int,
    val exceptionPatterns: Int,
    val totalRules: Int
)
