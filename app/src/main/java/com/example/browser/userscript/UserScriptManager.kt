package com.example.browser.userscript

import android.content.Context
import android.webkit.WebView
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class UserScript(
    val id: String,
    val name: String,
    val description: String = "",
    val matchPattern: String = "*", // URL match pattern (* = all sites)
    val script: String,
    val enabled: Boolean = true,
    val runAt: RunAt = RunAt.DOCUMENT_END,
    val createdAt: Long = System.currentTimeMillis()
) {
    enum class RunAt { DOCUMENT_START, DOCUMENT_END, DOCUMENT_IDLE }
}

class UserScriptManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("userscripts", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun getScripts(): List<UserScript> {
        val data = prefs.getString(KEY_SCRIPTS, null) ?: return defaultScripts()
        return try { json.decodeFromString(data) } catch (_: Exception) { defaultScripts() }
    }

    fun saveScript(script: UserScript) {
        val scripts = getScripts().toMutableList()
        scripts.removeAll { it.id == script.id }
        scripts.add(script)
        prefs.edit().putString(KEY_SCRIPTS, json.encodeToString(scripts)).apply()
    }

    fun deleteScript(id: String) {
        val scripts = getScripts().toMutableList()
        scripts.removeAll { it.id == id }
        prefs.edit().putString(KEY_SCRIPTS, json.encodeToString(scripts)).apply()
    }

    fun toggleScript(id: String) {
        val scripts = getScripts().toMutableList()
        val idx = scripts.indexOfFirst { it.id == id }
        if (idx >= 0) {
            scripts[idx] = scripts[idx].copy(enabled = !scripts[idx].enabled)
            prefs.edit().putString(KEY_SCRIPTS, json.encodeToString(scripts)).apply()
        }
    }

    fun injectScripts(webView: WebView, url: String) {
        val scripts = getScripts().filter { it.enabled && matchesUrl(url, it.matchPattern) }
        scripts.forEach { script ->
            val js = when (script.runAt) {
                UserScript.RunAt.DOCUMENT_START -> script.script
                UserScript.RunAt.DOCUMENT_END -> script.script
                UserScript.RunAt.DOCUMENT_IDLE -> """
                    if (document.readyState === 'complete' || document.readyState === 'interactive') {
                        ${script.script}
                    } else {
                        document.addEventListener('DOMContentLoaded', function() { ${script.script} });
                    }
                """.trimIndent()
            }
            webView.evaluateJavascript("(function(){${js}})()", null)
        }
    }

    private fun matchesUrl(url: String, pattern: String): Boolean {
        if (pattern == "*" || pattern.isBlank()) return true
        val regex = pattern
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", "\\?")
        return Regex(regex).containsMatchIn(url)
    }

    private fun defaultScripts(): List<UserScript> = listOf(
        UserScript(
            id = "anti_right_click",
            name = "Enable Right Click",
            description = "Re-enable right-click on sites that block it",
            matchPattern = "*",
            script = """
                document.addEventListener('contextmenu', function(e) { e.stopPropagation(); }, true);
                document.oncontextmenu = null;
                document.querySelectorAll('[oncontextmenu]').forEach(function(el) { el.removeAttribute('oncontextmenu'); });
            """.trimIndent(),
            enabled = false
        ),
        UserScript(
            id = "dark_mode",
            name = "Force Dark Mode",
            description = "Apply dark mode to any website",
            matchPattern = "*",
            script = """
                var style = document.createElement('style');
                style.textContent = 'html { filter: invert(1) hue-rotate(180deg) brightness(0.9) !important; } img, video, iframe, canvas, [style*="background-image"] { filter: invert(1) hue-rotate(180deg) !important; }';
                document.head.appendChild(style);
            """.trimIndent(),
            enabled = false
        ),
        UserScript(
            id = "remove_ads",
            name = "Aggressive Ad Remover",
            description = "Remove ad elements from page DOM",
            matchPattern = "*",
            script = """
                var selectors = ['[class*="ad-"]', '[class*="ads-"]', '[id*="ad-"]', '[id*="ads-"]', 'iframe[src*="ad"]', '.advertisement', '.sponsored'];
                selectors.forEach(function(s) { document.querySelectorAll(s).forEach(function(el) { el.remove(); }); });
            """.trimIndent(),
            enabled = false
        ),
        UserScript(
            id = "auto_expand",
            name = "Auto Expand Content",
            description = "Remove 'read more' and expand truncated content",
            matchPattern = "*",
            script = """
                document.querySelectorAll('[class*="expand"], [class*="show-more"], [class*="read-more"]').forEach(function(el) { el.click(); });
                document.querySelectorAll('[style*="max-height"]').forEach(function(el) { el.style.maxHeight = 'none'; });
                document.querySelectorAll('[style*="overflow: hidden"]').forEach(function(el) { el.style.overflow = 'visible'; });
            """.trimIndent(),
            enabled = false
        )
    )

    companion object {
        private const val KEY_SCRIPTS = "userscripts_data"

        @Volatile
        private var instance: UserScriptManager? = null

        fun getInstance(context: Context): UserScriptManager {
            return instance ?: synchronized(this) {
                instance ?: UserScriptManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
