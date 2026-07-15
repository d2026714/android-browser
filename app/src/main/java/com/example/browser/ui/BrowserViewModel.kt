package com.example.browser.ui

import android.app.Application
import android.content.Context
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.browser.data.AppDatabase
import com.example.browser.data.entity.BookmarkEntity
import com.example.browser.data.entity.HistoryEntity
import com.example.browser.reader.TextExtractor
import com.example.browser.util.SearchEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TabState(
    val id: Long = System.currentTimeMillis(),
    val title: String = "新标签页",
    val url: String = "",
    val webView: WebView? = null,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val hasError: Boolean = false,
)

class BrowserViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.getInstance(app)
    private val prefs = app.getSharedPreferences("browser", Context.MODE_PRIVATE)

    // ── Tabs ──
    private val _tabs = MutableStateFlow(listOf(TabState()))
    val tabs: StateFlow<List<TabState>> = _tabs.asStateFlow()

    private val _activeIndex = MutableStateFlow(0)
    val activeIndex: StateFlow<Int> = _activeIndex.asStateFlow()

    private val _showHome = MutableStateFlow(true)
    val showHome: StateFlow<Boolean> = _showHome.asStateFlow()

    // ── UI ──
    private val _showFindBar = MutableStateFlow(false)
    val showFindBar: StateFlow<Boolean> = _showFindBar.asStateFlow()

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

    // ── Settings ──
    private val _adBlock = MutableStateFlow(prefs.getBoolean("ad_block", true))
    val adBlock: StateFlow<Boolean> = _adBlock.asStateFlow()

    private val _darkMode = MutableStateFlow(prefs.getBoolean("dark_mode", false))
    val darkMode: StateFlow<Boolean> = _darkMode.asStateFlow()

    private val _searchEngine = MutableStateFlow(
        SearchEngine.valueOf(prefs.getString("engine", "GOOGLE") ?: "GOOGLE"))
    val searchEngine: StateFlow<SearchEngine> = _searchEngine.asStateFlow()

    private val _fontSize = MutableStateFlow(prefs.getInt("font_size", 100))
    val fontSize: StateFlow<Int> = _fontSize.asStateFlow()

    // ── Reader ──
    private val _readerContent = MutableStateFlow<TextExtractor.ExtractedContent?>(null)
    val readerContent: StateFlow<TextExtractor.ExtractedContent?> = _readerContent.asStateFlow()

    private val _isExtracting = MutableStateFlow(false)
    val isExtracting: StateFlow<Boolean> = _isExtracting.asStateFlow()

    private val _bookshelf = MutableStateFlow(loadBookshelf())
    val bookshelf: StateFlow<List<BookItem>> = _bookshelf.asStateFlow()

    // ── Data ──
    val bookmarks = db.bookmarkDao().getAll().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val history = db.historyDao().getAll().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // ═══════════════════════════════════
    //  Tabs
    // ═══════════════════════════════════

    fun addTab(url: String = "") {
        _tabs.value = _tabs.value + TabState(title = if (url.isEmpty()) "新标签页" else url, url = url)
        _activeIndex.value = _tabs.value.size - 1
        _showHome.value = url.isEmpty()
    }

    fun closeTab(i: Int) {
        val list = _tabs.value.toMutableList()
        if (list.size <= 1) {
            list[0] = TabState(); _tabs.value = list; _showHome.value = true; return
        }
        list.removeAt(i); _tabs.value = list
        if (_activeIndex.value >= list.size) _activeIndex.value = list.size - 1
    }

    fun switchTab(i: Int) {
        if (i in _tabs.value.indices) { _activeIndex.value = i; _showHome.value = false }
    }

    fun updateTab(i: Int, title: String? = null, url: String? = null, isLoading: Boolean? = null,
                  progress: Int? = null, hasError: Boolean? = null, canGoBack: Boolean? = null,
                  canGoForward: Boolean? = null) {
        val list = _tabs.value.toMutableList()
        if (i !in list.indices) return
        val t = list[i]
        list[i] = t.copy(
            title = title ?: t.title, url = url ?: t.url,
            isLoading = isLoading ?: t.isLoading, progress = progress ?: t.progress,
            hasError = hasError ?: t.hasError, canGoBack = canGoBack ?: t.canGoBack,
            canGoForward = canGoForward ?: t.canGoForward,
        )
        _tabs.value = list
    }

    fun setWebView(i: Int, wv: WebView) {
        val list = _tabs.value.toMutableList()
        if (i in list.indices) { list[i] = list[i].copy(webView = wv); _tabs.value = list }
    }

    fun activeWebView(): WebView? = _tabs.value.getOrNull(_activeIndex.value)?.webView

    // ═══════════════════════════════════
    //  Navigation
    // ═══════════════════════════════════

    fun loadUrl(url: String) {
        if (url.isEmpty()) return
        val i = _activeIndex.value
        _showHome.value = false
        activeWebView()?.loadUrl(url)
        updateTab(i, url = url, hasError = false)
        viewModelScope.launch {
            db.historyDao().insert(HistoryEntity(title = _tabs.value.getOrNull(i)?.title ?: "", url = url))
        }
    }

    fun goBack() { activeWebView()?.goBack() }
    fun goForward() { activeWebView()?.goForward() }
    fun reload() { activeWebView()?.reload() }
    fun stopLoading() { activeWebView()?.stopLoading() }
    fun goHome() { _showHome.value = true }

    // ═══════════════════════════════════
    //  Find
    // ═══════════════════════════════════

    fun showFind() { _showFindBar.value = true }
    fun hideFind() { _showFindBar.value = false }
    fun findAll(q: String) { activeWebView()?.findAllAsync(q) }
    fun findNext() { activeWebView()?.findNext(true) }
    fun findPrev() { activeWebView()?.findNext(false) }
    fun clearFind() { activeWebView()?.clearMatches() }

    // ═══════════════════════════════════
    //  Suggestions
    // ═══════════════════════════════════

    fun updateSuggestions(q: String) {
        viewModelScope.launch { _suggestions.value = com.example.browser.web.SearchSuggestions.fetch(q) }
    }
    fun clearSuggestions() { _suggestions.value = emptyList() }

    // ═══════════════════════════════════
    //  Bookmarks
    // ═══════════════════════════════════

    fun toggleBookmark() {
        val tab = _tabs.value.getOrNull(_activeIndex.value) ?: return
        viewModelScope.launch {
            val exists = db.bookmarkDao().isBookmarked(tab.url).first()
            if (exists) db.bookmarkDao().deleteByUrl(tab.url)
            else db.bookmarkDao().insert(BookmarkEntity(title = tab.title, url = tab.url))
        }
    }
    fun deleteBookmark(b: BookmarkEntity) { viewModelScope.launch { db.bookmarkDao().delete(b) } }

    // ═══════════════════════════════════
    //  History
    // ═══════════════════════════════════

    fun deleteHistory(id: Long) { viewModelScope.launch { db.historyDao().deleteById(id) } }
    fun clearHistory() { viewModelScope.launch { db.historyDao().deleteAll() } }

    // ═══════════════════════════════════
    //  Settings
    // ═══════════════════════════════════

    fun setAdBlock(v: Boolean) { _adBlock.value = v; prefs.edit().putBoolean("ad_block", v).apply() }
    fun setDarkMode(v: Boolean) { _darkMode.value = v; prefs.edit().putBoolean("dark_mode", v).apply() }
    fun setEngine(e: SearchEngine) { _searchEngine.value = e; prefs.edit().putString("engine", e.name).apply() }
    fun setFontSize(s: Int) { _fontSize.value = s; prefs.edit().putInt("font_size", s).apply(); activeWebView()?.settings?.textZoom = s }
    fun clearAllData() { viewModelScope.launch { db.bookmarkDao().deleteAll(); db.historyDao().deleteAll() } }

    // ═══════════════════════════════════
    //  Reader
    // ═══════════════════════════════════

    fun extractContent() {
        val wv = activeWebView() ?: return
        _isExtracting.value = true
        viewModelScope.launch {
            try {
                val c = TextExtractor.extract(wv)
                _readerContent.value = c
                if (c.text.length > 100) addBookshelf(BookItem(c.title, wv.url ?: "", c.chapters.firstOrNull()?.title ?: ""))
            } catch (_: Exception) { _readerContent.value = TextExtractor.ExtractedContent("", "提取失败", emptyList()) }
            finally { _isExtracting.value = false }
        }
    }
    fun clearReader() { _readerContent.value = null }

    // ═══════════════════════════════════
    //  Bookshelf
    // ═══════════════════════════════════

    private fun loadBookshelf(): List<BookItem> {
        return prefs.getStringSet("bookshelf", emptySet())?.mapNotNull {
            val p = it.split("|||"); if (p.size >= 2) BookItem(p[0], p[1], p.getOrElse(2) { "" }) else null
        } ?: emptyList()
    }

    private fun saveBookshelf() {
        prefs.edit().putStringSet("bookshelf", _bookshelf.value.map { "${it.title}|||${it.url}|||${it.chapter}" }.toSet()).apply()
    }

    fun addBookshelf(b: BookItem) {
        _bookshelf.value = listOf(b) + _bookshelf.value.filter { it.url != b.url }; saveBookshelf()
    }
    fun removeBookshelf(b: BookItem) { _bookshelf.value = _bookshelf.value.filter { it.url != b.url }; saveBookshelf() }
}

data class BookItem(val title: String, val url: String, val chapter: String = "")
