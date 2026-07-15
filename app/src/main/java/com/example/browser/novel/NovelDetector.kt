package com.example.browser.novel

import android.util.Log
import java.util.regex.Pattern

/**
 * Detects whether a web page is likely a novel/fiction reading site.
 * Uses pattern matching and heuristics to return a confidence score.
 */
object NovelDetector {

    private const val TAG = "NovelDetector"

    data class DetectionResult(
        val isNovelSite: Boolean,
        val confidence: Float,  // 0.0 to 1.0
        val title: String = "",
        val author: String = "",
        val chapterCount: Int = 0
    )

    // Common chapter title patterns in Chinese
    private val CHAPTER_PATTERNS = listOf(
        Pattern.compile("第[\\d一二三四五六七八九十百千零〇]+章"),
        Pattern.compile("第[\\d一二三四五六七八九十百千零〇]+节"),
        Pattern.compile("第[\\d一二三四五六七八九十百千零〇]+卷"),
        Pattern.compile("Chapter\\s+\\d+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("章节[\\d]+"),
    )

    // Known novel site domains
    private val NOVEL_DOMAINS = listOf(
        "qidian.com",       // 起点中文网
        "zongheng.com",     // 纵横中文网
        "17k.com",          // 17K小说网
        "jjwxc.net",        // 晋江文学城
        "ciweimao.com",     // 刺猬猫
        "biquge",           // 笔趣阁 (various mirrors)
        "biquge5200",
        "biquge5200",
        "xbiquge",
        "biqugeu",
        "biquge-la",
        "biqugexs",
        "fanqienovel",      // 番茄小说
        "changdunovel",     // 长图小说
        "readnovel",
        "69shu",
        "shuqi.com",        // 书旗小说
        "hongxiu.com",      // 红袖添香
        "xxsy.net",         // 潇湘书院
        "txt",              // Various .txt sites
        "quanben",
        "douban.com/read",  // 豆瓣阅读
        "yruan.com",
        "novel",
    )

    /**
     * Analyze HTML content to determine if it's a novel page.
     * Call this with the page's HTML source.
     */
    fun analyze(html: String, url: String): DetectionResult {
        var score = 0.0f
        val title = extractNovelTitle(html)
        val author = extractAuthor(html)

        // 1. Check URL domain
        val domainScore = checkDomain(url)
        score += domainScore

        // 2. Check for chapter-like links (table of contents pattern)
        val chapterLinks = countChapterLinks(html)
        val linkScore = when {
            chapterLinks >= 50 -> 0.35f
            chapterLinks >= 20 -> 0.30f
            chapterLinks >= 10 -> 0.25f
            chapterLinks >= 5 -> 0.15f
            else -> 0.0f
        }
        score += linkScore

        // 3. Check for chapter title patterns in the text
        val chapterTitleCount = countChapterPatterns(html)
        val titleScore = when {
            chapterTitleCount >= 20 -> 0.25f
            chapterTitleCount >= 10 -> 0.20f
            chapterTitleCount >= 5 -> 0.15f
            else -> 0.0f
        }
        score += titleScore

        // 4. Check for novel-specific HTML structures
        if (hasNovelStructure(html)) {
            score += 0.15f
        }

        // 5. If we found a title and author, boost confidence
        if (title.isNotBlank() && author.isNotBlank()) {
            score += 0.10f
        } else if (title.isNotBlank()) {
            score += 0.05f
        }

        val confidence = score.coerceIn(0.0f, 1.0f)
        val isNovelSite = confidence >= 0.4f

        Log.d(TAG, "Novel detection: url=$url, confidence=$confidence, chapters=$chapterLinks, title=$title")

        return DetectionResult(
            isNovelSite = isNovelSite,
            confidence = confidence,
            title = title,
            author = author,
            chapterCount = chapterLinks.coerceAtLeast(chapterTitleCount)
        )
    }

    /**
     * Quick check using just the URL (for pre-filtering).
     */
    fun isLikelyNovelUrl(url: String): Boolean {
        val lower = url.lowercase()
        return NOVEL_DOMAINS.any { lower.contains(it) }
    }

    private fun checkDomain(url: String): Float {
        val lower = url.lowercase()
        return if (NOVEL_DOMAINS.any { lower.contains(it) }) {
            0.25f
        } else {
            0.0f
        }
    }

    private fun countChapterLinks(html: String): Int {
        // Look for <a> tags whose text matches chapter patterns
        val linkPattern = Pattern.compile("<a[^>]*>([^<]*)</a>", Pattern.CASE_INSENSITIVE)
        val matcher = linkPattern.matcher(html)
        var count = 0
        while (matcher.find()) {
            val text = matcher.group(1)?.trim() ?: ""
            if (CHAPTER_PATTERNS.any { it.matcher(text).find() }) {
                count++
            }
        }
        return count
    }

    private fun countChapterPatterns(html: String): Int {
        // Strip HTML tags first
        val text = html.replace(Regex("<[^>]+>"), " ")
        var count = 0
        for (pattern in CHAPTER_PATTERNS) {
            val matcher = pattern.matcher(text)
            while (matcher.find()) {
                count++
            }
        }
        return count
    }

    private fun hasNovelStructure(html: String): Boolean {
        val indicators = listOf(
            "class=\"chapter-list\"",
            "class=\"list-chapter\"",
            "id=\"list-chapter\"",
            "class=\"volume-list\"",
            "class=\"catalog\"",
            "class=\"book-list\"",
            "class=\"chapterlist\"",
            "id=\"chapterList\"",
            "class=\"mulu\"",         // 目录
            "class=\"dirlist\"",
            "class=\"readcontent\"",  // 阅读内容
            "class=\"novelcontent\"",
            "class=\"chapter-content\"",
            "id=\"content\"",
            "id=\"chaptercontent\"",
            "class=\"txt\"",          // 常见正文class
        )
        val lower = html.lowercase()
        return indicators.any { lower.contains(it.lowercase()) }
    }

    private fun extractNovelTitle(html: String): String {
        // Try common patterns for novel title
        val patterns = listOf(
            Pattern.compile("<h1[^>]*>([^<]+)</h1>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("class=\"bookname\"[^>]*>([^<]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("class=\"book-title\"[^>]*>([^<]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("class=\"title\"[^>]*>([^<]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<title>([^<]+)</title>", Pattern.CASE_INSENSITIVE),
        )
        for (p in patterns) {
            val m = p.matcher(html)
            if (m.find()) {
                val title = m.group(1)?.trim() ?: ""
                if (title.isNotBlank() && title.length < 100) {
                    return title
                }
            }
        }
        return ""
    }

    private fun extractAuthor(html: String): String {
        val patterns = listOf(
            Pattern.compile("作者[：:]\\s*([^<\\n]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("class=\"author\"[^>]*>([^<]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("byline[^>]*>([^<]+)", Pattern.CASE_INSENSITIVE),
        )
        for (p in patterns) {
            val m = p.matcher(html)
            if (m.find()) {
                val author = m.group(1)?.trim()?.removePrefix("：")?.removePrefix(":")?.trim() ?: ""
                if (author.isNotBlank() && author.length < 50) {
                    return author
                }
            }
        }
        return ""
    }
}
