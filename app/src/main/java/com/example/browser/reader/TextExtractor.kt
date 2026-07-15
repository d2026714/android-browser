package com.example.browser.reader

import android.webkit.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Extracts main text content from a web page using a readability-like algorithm.
 * Runs JS in WebView to get the page content, then scores text blocks.
 */
object TextExtractor {

    data class ExtractedContent(
        val title: String,
        val text: String,
        val chapters: List<Chapter>,
    )

    data class Chapter(
        val title: String,
        val content: String,
    )

    private const val EXTRACT_JS = """
    (function() {
        // Remove noise elements
        var remove = 'script,style,nav,header,footer,aside,iframe,noscript,.ad,.ads,.sidebar,.menu,.nav,.header,.footer,.comment';
        document.querySelectorAll(remove).forEach(function(el) { el.remove() });

        // Get all text blocks
        var blocks = [];
        var elements = document.querySelectorAll('p,div,li,td,th,h1,h2,h3,h4,h5,h6,article,section');

        for (var i = 0; i < elements.length; i++) {
            var el = elements[i];
            var text = el.innerText.trim();
            if (text.length < 20) continue;

            // Score based on text length and density
            var linkLen = 0;
            var links = el.querySelectorAll('a');
            for (var j = 0; j < links.length; j++) {
                linkLen += links[j].innerText.length;
            }
            var density = text.length > 0 ? (text.length - linkLen) / text.length : 0;
            if (density < 0.4) continue;

            blocks.push({
                text: text,
                score: text.length * density,
                tag: el.tagName.toLowerCase()
            });
        }

        // Sort by score and take top blocks
        blocks.sort(function(a, b) { return b.score - a.score; });
        var topBlocks = blocks.slice(0, Math.min(50, blocks.length));

        // Re-sort by DOM position for readability
        var result = topBlocks.map(function(b) { return b.text; }).join('\\n\\n');

        // Detect chapters
        var chapters = [];
        var lines = result.split('\\n');
        var current = { title: '', content: '' };

        for (var k = 0; k < lines.length; k++) {
            var line = lines[k].trim();
            if (!line) continue;

            var isChapter = /^(第[一二三四五六七八九十百千零〇0-9]+[章节回卷集部篇]|Chapter\\s+\\d+|CHAPTER\\s+\\d+)/i.test(line);
            if (isChapter) {
                if (current.title || current.content) {
                    chapters.push({ title: current.title || '未命名', content: current.content.trim() });
                }
                current = { title: line, content: '' };
            } else {
                current.content += line + '\\n';
            }
        }
        if (current.title || current.content) {
            chapters.push({ title: current.title || '未命名', content: current.content.trim() });
        }

        return JSON.stringify({
            title: document.title,
            text: result,
            chapters: chapters
        });
    })();
    """

    suspend fun extract(webView: WebView): ExtractedContent = withContext(Dispatchers.Main) {
        val json = webView.evaluateJavascriptAsync(EXTRACT_JS)
        parseResult(json, webView.title ?: "")
    }

    private fun parseResult(json: String, fallbackTitle: String): ExtractedContent {
        return try {
            val clean = json.removeSurrounding("\"").replace("\\\"", "\"").replace("\\n", "\n")
            val obj = org.json.JSONObject(clean)
            val title = obj.optString("title", fallbackTitle)
            val text = obj.optString("text", "")
            val chaptersArr = obj.optJSONArray("chapters")

            val chapters = mutableListOf<Chapter>()
            if (chaptersArr != null) {
                for (i in 0 until chaptersArr.length()) {
                    val ch = chaptersArr.getJSONObject(i)
                    chapters.add(Chapter(ch.getString("title"), ch.getString("content")))
                }
            }

            ExtractedContent(title, text, chapters)
        } catch (_: Exception) {
            ExtractedContent(fallbackTitle, "", emptyList())
        }
    }

    private suspend fun WebView.evaluateJavascriptAsync(script: String): String =
        withContext(Dispatchers.Main) {
            kotlinx.coroutines.suspendCancellableCoroutine { cont ->
                evaluateJavascript(script) { result ->
                    cont.resumeWith(Result.success(result ?: ""))
                }
            }
        }
}
