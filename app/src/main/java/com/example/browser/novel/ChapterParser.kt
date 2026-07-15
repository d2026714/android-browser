package com.example.browser.novel

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.util.regex.Pattern

/**
 * Parses HTML to extract chapter lists from novel sites.
 * Uses JavaScript injection for dynamic content extraction.
 */
object ChapterParser {

    private const val TAG = "ChapterParser"

    data class ChapterInfo(
        val index: Int,
        val title: String,
        val url: String
    )

    data class ParsedNovel(
        val title: String = "",
        val author: String = "",
        val coverUrl: String = "",
        val chapters: List<ChapterInfo> = emptyList()
    )

    /**
     * JavaScript to extract chapter list from a novel page.
     * This JS runs in the WebView/GeckoView context.
     */
    val EXTRACT_CHAPTERS_JS = """
    (function() {
        var result = { title: '', author: '', coverUrl: '', chapters: [] };
        
        // Extract title
        var titleEl = document.querySelector('h1, .bookname, .book-title, .bookname h1, #bookinfo h1, .info h1');
        if (titleEl) result.title = titleEl.textContent.trim();
        if (!result.title) {
            var ogTitle = document.querySelector('meta[property="og:title"]');
            if (ogTitle) result.title = ogTitle.getAttribute('content') || '';
        }
        if (!result.title && document.title) {
            result.title = document.title.split(/[-–—_|]/)[0].trim();
        }
        
        // Extract author
        var authorEl = document.querySelector('.author a, .bookinfo .author, [property="og:novel:author"]');
        if (authorEl) result.author = authorEl.textContent.trim();
        if (!result.author) {
            var body = document.body.innerText;
            var authorMatch = body.match(/作者[：:]\s*(\S+)/);
            if (authorMatch) result.author = authorMatch[1];
        }
        
        // Extract cover
        var coverEl = document.querySelector('.bookimg img, .book-cover img, .cover img, #bookimg img, .bookinfo img, [property="og:image"]');
        if (coverEl) {
            result.coverUrl = coverEl.getAttribute('src') || coverEl.getAttribute('content') || '';
        }
        
        // Extract chapter links - try multiple common selectors
        var linkSelectors = [
            '#list dd a',                    // 笔趣阁
            '.list-chapter a',               // 通用
            '#chapterList a',                // 番茄
            '.chapter-list a',               // 通用
            '.volume-list a',                // 纵横
            '.mulu a',                       // 目录
            '.dirlist a',                    // 目录列表
            'dd a',                          // 起点
            '.book-list a',                  // 书列表
            'a[href*="chapter"]',            // 链接含chapter
            'a[href*="/read/"]',             // 阅读链接
        ];
        
        var links = [];
        for (var i = 0; i < linkSelectors.length; i++) {
            var found = document.querySelectorAll(linkSelectors[i]);
            if (found.length >= 5) {
                links = found;
                break;
            }
        }
        
        // Fallback: find consecutive <a> tags that look like chapters
        if (links.length < 5) {
            var allLinks = document.querySelectorAll('a');
            var candidates = [];
            for (var j = 0; j < allLinks.length; j++) {
                var text = allLinks[j].textContent.trim();
                if (/第[\\d一二三四五六七八九十百千]+[章节卷]/.test(text) || 
                    /chapter\s*\d+/i.test(text)) {
                    candidates.push(allLinks[j]);
                }
            }
            if (candidates.length >= 5) links = candidates;
        }
        
        // Deduplicate and build chapter list
        var seen = {};
        for (var k = 0; k < links.length; k++) {
            var a = links[k];
            var href = a.getAttribute('href') || '';
            var chTitle = a.textContent.trim();
            if (!href || !chTitle || seen[href]) continue;
            if (chTitle.length > 100) continue;
            seen[href] = true;
            
            // Resolve relative URL
            try {
                if (href.startsWith('/')) {
                    href = window.location.origin + href;
                } else if (!href.startsWith('http')) {
                    href = new URL(href, window.location.href).href;
                }
            } catch(e) {}
            
            result.chapters.push({
                title: chTitle,
                url: href
            });
        }
        
        // Add indices
        for (var m = 0; m < result.chapters.length; m++) {
            result.chapters[m].index = m;
        }
        
        return JSON.stringify(result);
    })()
    """.trimIndent()

    /**
     * JavaScript to extract the main content of a single chapter page.
     */
    val EXTRACT_CHAPTER_CONTENT_JS = """
    (function() {
        // Try common content selectors
        var selectors = [
            '#content', '#chaptercontent', '.chapter-content', 
            '.readcontent', '.novelcontent', '.txt', '#booktext',
            '.content', 'article', '.text', '#text',
            '.chapter_text', '#chapter_text', '.read-content',
            '.articlecontent', '#articlecontent'
        ];
        
        var content = '';
        for (var i = 0; i < selectors.length; i++) {
            var el = document.querySelector(selectors[i]);
            if (el && el.innerText.trim().length > 100) {
                content = el.innerText.trim();
                break;
            }
        }
        
        // Fallback: get the largest text block
        if (!content || content.length < 100) {
            var allDivs = document.querySelectorAll('div, article, section');
            var maxLen = 0;
            for (var j = 0; j < allDivs.length; j++) {
                var text = allDivs[j].innerText.trim();
                if (text.length > maxLen && text.length > 200) {
                    maxLen = text.length;
                    content = text;
                }
            }
        }
        
        // Extract chapter title
        var title = '';
        var titleEl = document.querySelector('h1, .bookname, .chapter-title, .title');
        if (titleEl) title = titleEl.textContent.trim();
        
        return JSON.stringify({ title: title, content: content });
    })()
    """.trimIndent()

    /**
     * Parse the JSON result from EXTRACT_CHAPTERS_JS.
     */
    fun parseChapterList(json: String): ParsedNovel {
        return try {
            val obj = JSONObject(json)
            val title = obj.optString("title", "")
            val author = obj.optString("author", "")
            val coverUrl = obj.optString("coverUrl", "")
            val chaptersArray = obj.optJSONArray("chapters") ?: JSONArray()

            val chapters = (0 until chaptersArray.length()).mapNotNull { i ->
                val ch = chaptersArray.optJSONObject(i) ?: return@mapNotNull null
                ChapterInfo(
                    index = ch.optInt("index", i),
                    title = ch.optString("title", ""),
                    url = ch.optString("url", "")
                )
            }

            ParsedNovel(
                title = title,
                author = author,
                coverUrl = coverUrl,
                chapters = chapters
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse chapter list JSON", e)
            ParsedNovel()
        }
    }

    /**
     * Parse the JSON result from EXTRACT_CHAPTER_CONTENT_JS.
     */
    fun parseChapterContent(json: String): Pair<String, String> {
        return try {
            val obj = JSONObject(json)
            Pair(
                obj.optString("title", ""),
                obj.optString("content", "")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse chapter content JSON", e)
            Pair("", "")
        }
    }
}
