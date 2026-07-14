package com.example.browser.devtools

import android.content.Context
import android.webkit.ConsoleMessage
import android.webkit.WebView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

data class ConsoleLog(
    val level: Level,
    val message: String,
    val source: String,
    val lineNumber: Int,
    val timestamp: Long = System.currentTimeMillis()
) {
    enum class Level { LOG, WARNING, ERROR, DEBUG, INFO }
}

data class NetworkRequest(
    val url: String,
    val method: String,
    val status: Int,
    val size: Long,
    val time: Long,
    val mimeType: String
)

data class PageMetrics(
    val domElements: Int = 0,
    val jsHeapUsed: Long = 0,
    val jsHeapTotal: Long = 0,
    val loadTime: Long = 0,
    val resourceCount: Int = 0,
    val pageSize: Long = 0
)

class DevTools(private val context: Context) {
    private val _consoleLogs = MutableStateFlow<List<ConsoleLog>>(emptyList())
    val consoleLogs: StateFlow<List<ConsoleLog>> = _consoleLogs.asStateFlow()

    private val _networkRequests = MutableStateFlow<List<NetworkRequest>>(emptyList())
    val networkRequests: StateFlow<List<NetworkRequest>> = _networkRequests.asStateFlow()

    private val _pageMetrics = MutableStateFlow(PageMetrics())
    val pageMetrics: StateFlow<PageMetrics> = _pageMetrics.asStateFlow()

    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
        val level = when (consoleMessage.messageLevel()) {
            ConsoleMessage.MessageLevel.ERROR -> ConsoleLog.Level.ERROR
            ConsoleMessage.MessageLevel.WARNING -> ConsoleLog.Level.WARNING
            ConsoleMessage.MessageLevel.DEBUG -> ConsoleLog.Level.DEBUG
            ConsoleMessage.MessageLevel.TIP -> ConsoleLog.Level.INFO
            else -> ConsoleLog.Level.LOG
        }
        val log = ConsoleLog(
            level = level,
            message = consoleMessage.message(),
            source = consoleMessage.sourceId(),
            lineNumber = consoleMessage.lineNumber()
        )
        _consoleLogs.value = _consoleLogs.value + log
        return false
    }

    fun clearConsole() { _consoleLogs.value = emptyList() }

    fun injectDevToolsScript(webView: WebView) {
        val script = """
            (function() {
                // Performance metrics
                var perf = window.performance;
                if (perf) {
                    var timing = perf.timing;
                    var loadTime = timing.loadEventEnd - timing.navigationStart;
                    var entries = perf.getEntriesByType('resource');
                    var totalSize = 0;
                    entries.forEach(function(e) { totalSize += e.transferSize || 0; });
                    
                    window.__devtools_metrics = {
                        loadTime: loadTime,
                        resourceCount: entries.length,
                        pageSize: totalSize,
                        domElements: document.querySelectorAll('*').length
                    };
                }
                
                // Console interceptor
                var origLog = console.log;
                var origWarn = console.warn;
                var origError = console.error;
                
                console.log = function() {
                    origLog.apply(console, arguments);
                    if (window.Android) window.Android.consoleLog('LOG', Array.from(arguments).join(' '));
                };
                console.warn = function() {
                    origWarn.apply(console, arguments);
                    if (window.Android) window.Android.consoleLog('WARN', Array.from(arguments).join(' '));
                };
                console.error = function() {
                    origError.apply(console, arguments);
                    if (window.Android) window.Android.consoleLog('ERROR', Array.from(arguments).join(' '));
                };
                
                // Error catcher
                window.onerror = function(msg, url, line) {
                    if (window.Android) window.Android.consoleLog('ERROR', msg + ' (' + url + ':' + line + ')');
                };
                
                return JSON.stringify(window.__devtools_metrics || {});
            })();
        """.trimIndent()

        webView.evaluateJavascript(script) { result ->
            try {
                // Parse metrics from result
            } catch (_: Exception) {}
        }
    }

    fun getPageSource(webView: WebView, callback: (String) -> Unit) {
        webView.evaluateJavascript(
            "(function() { return document.documentElement.outerHTML; })()"
        ) { html ->
            callback(html?.removeSurrounding("\"")
                ?.replace("\\n", "\n")
                ?.replace("\\\"", "\"")
                ?.replace("\\t", "\t") ?: "")
        }
    }

    fun getDocumentTree(webView: WebView, callback: (String) -> Unit) {
        val script = """
            (function() {
                function getTree(el, depth) {
                    if (depth > 5) return '';
                    var indent = '  '.repeat(depth);
                    var tag = el.tagName.toLowerCase();
                    var attrs = '';
                    for (var i = 0; i < el.attributes.length && i < 3; i++) {
                        attrs += ' ' + el.attributes[i].name + '="' + el.attributes[i].value.substring(0, 30) + '"';
                    }
                    var result = indent + '<' + tag + attrs + '>';
                    var children = el.children;
                    if (children.length > 0) {
                        result += ' (' + children.length + ' children)\n';
                        for (var i = 0; i < Math.min(children.length, 10); i++) {
                            result += getTree(children[i], depth + 1);
                        }
                        if (children.length > 10) result += indent + '  ... +' + (children.length - 10) + ' more\n';
                    } else {
                        var text = el.textContent.trim().substring(0, 50);
                        if (text) result += ' "' + text + '"';
                        result += '\n';
                    }
                    return result;
                }
                return getTree(document.body, 0);
            })();
        """.trimIndent()
        webView.evaluateJavascript(script) { result ->
            callback(result?.removeSurrounding("\"")?.replace("\\n", "\n")?.replace("\\\"", "\"") ?: "")
        }
    }

    fun clearNetworkRequests() { _networkRequests.value = emptyList() }

    fun formatTimestamp(time: Long): String = dateFormat.format(Date(time))

    companion object {
        @Volatile
        private var instance: DevTools? = null

        fun getInstance(context: Context): DevTools {
            return instance ?: synchronized(this) {
                instance ?: DevTools(context.applicationContext).also { instance = it }
            }
        }
    }
}
