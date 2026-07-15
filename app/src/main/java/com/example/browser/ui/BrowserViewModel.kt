package com.example.browser.ui

import android.app.Application
import android.content.Context
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.browser.data.AppDatabase
import com.example.browser.data.entity.BookmarkEntity
import com.example.browser.data.entity.HistoryEntity
import com.example.browser.util.SearchEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TabState(
    val id: Long = 0,
    val title: String = "",
    val url: String = "",
    val webView: WebView? = null,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isLoading: Boolean = false,
    val progress: Int = 0,
)

class BrowserViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val prefs = application.getSharedPreferences("browser_prefs", Context.MODE_PRIVATE)

    // Tabs
    private val _tabs = MutableStateFlow<List<TabState>>(emptyList())
    val tabs: StateFlow<List<TabState>> = _tabs.asStateFlow()

    private val _activeTabIndex = MutableStateFlow(0)
    val activeTabIndex: StateFlow<Int> = _activeTabIndex.asStateFlow()

    // UI State
    private val _showHome = MutableStateFlow(true)
    val showHome: StateFlow<Boolean> = _showHome.asStateFlow()

    private val _showBookmarks = MutableStateFlow(false)
    val showBookmarks: StateFlow<Boolean> = _showBookmarks.asStateFlow()

    private val _showHistory = MutableStateFlow(false)
    val showHistory: StateFlow<Boolean> = _showHistory.asStateFlow()

    private val _showSettings = MutableStateFlow(false)
    val showSettings: StateFlow<Boolean> = _showSettings.asStateFlow()

    private val _showFindBar = MutableStateFlow(false)
    val showFindBar: StateFlow<Boolean> = _showFindBar.asStateFlow()

    // Settings
    private val _adBlockEnabled = MutableStateFlow(prefs.getBoolean("ad_block", true))
    val adBlockEnabled: StateFlow<Boolean> = _adBlockEnabled.asStateFlow()

    private val _darkMode = MutableStateFlow(prefs.getBoolean("dark_mode", false))
    val darkMode: StateFlow<Boolean> = _darkMode.asStateFlow()

    private val _searchEngine = MutableStateFlow(
        SearchEngine.valueOf(prefs.getString("search_engine", "GOOGLE") ?: "GOOGLE")
    )
    val searchEngine: StateFlow<SearchEngine> = _searchEngine.asStateFlow()

    private val _fontSize = MutableStateFlow(prefs.getInt("font_size", 100))
    val fontSize: StateFlow<Int> = _fontSize.asStateFlow()

    // Data
    val bookmarks = db.bookmarkDao().getAll()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val history = db.historyDao().getAll()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        addTab()
    }

    // Tab Management
    fun addTab(url: String = "") {
        val newTab = TabState(
            id = System.currentTimeMillis(),
            title = if (url.isEmpty()) "新标签页" else url,
            url = url,
        )
        _tabs.value = _tabs.value + newTab
        _activeTabIndex.value = _tabs.value.size - 1
        _showHome.value = url.isEmpty()
    }

    fun closeTab(index: Int) {
        val currentTabs = _tabs.value.toMutableList()
        if (currentTabs.size <= 1) {
            // Don't close last tab, just go home
            _showHome.value = true
            currentTabs[0] = currentTabs[0].copy(title = "新标签页", url = "")
            _tabs.value = currentTabs
            return
        }
        currentTabs.removeAt(index)
        _tabs.value = currentTabs
        if (_activeTabIndex.value >= currentTabs.size) {
            _activeTabIndex.value = currentTabs.size - 1
        }
    }

    fun switchTab(index: Int) {
        if (index in _tabs.value.indices) {
            _activeTabIndex.value = index
            _showHome.value = false
        }
    }

    fun updateTabState(index: Int, title: String? = null, url: String? = null,
                       canGoBack: Boolean? = null, canGoForward: Boolean? = null,
                       isLoading: Boolean? = null, progress: Int? = null) {
        val currentTabs = _tabs.value.toMutableList()
        if (index !in currentTabs.indices) return
        val tab = currentTabs[index]
        currentTabs[index] = tab.copy(
            title = title ?: tab.title,
            url = url ?: tab.url,
            canGoBack = canGoBack ?: tab.canGoBack,
            canGoForward = canGoForward ?: tab.canGoForward,
            isLoading = isLoading ?: tab.isLoading,
            progress = progress ?: tab.progress,
        )
        _tabs.value = currentTabs
    }

    fun setWebView(index: Int, webView: WebView) {
        val currentTabs = _tabs.value.toMutableList()
        if (index !in currentTabs.indices) return
        currentTabs[index] = currentTabs[index].copy(webView = webView)
        _tabs.value = currentTabs
    }

    fun getActiveWebView(): WebView? {
        return _tabs.value.getOrNull(_activeTabIndex.value)?.webView
    }

    // Navigation
    fun loadUrl(url: String) {
        val activeIndex = _activeTabIndex.value
        if (url.isNotEmpty()) {
            _showHome.value = false
            getActiveWebView()?.loadUrl(url)
            updateTabState(activeIndex, url = url)
            addHistory(_tabs.value.getOrNull(activeIndex)?.title ?: "", url)
        }
    }

    fun goBack() {
        getActiveWebView()?.goBack()
    }

    fun goForward() {
        getActiveWebView()?.goForward()
    }

    fun reload() {
        getActiveWebView()?.reload()
    }

    fun goHome() {
        _showHome.value = true
    }

    // Find in page
    fun showFind() { _showFindBar.value = true }
    fun hideFind() { _showFindBar.value = false }
    fun findAll(query: String) { getActiveWebView()?.findAllAsync(query) }
    fun findNext() { getActiveWebView()?.findNext(true) }
    fun findPrevious() { getActiveWebView()?.findNext(false) }
    fun clearFindMatches() { getActiveWebView()?.clearMatches() }

    // Bookmarks
    fun toggleBookmark() {
        val tab = _tabs.value.getOrNull(_activeTabIndex.value) ?: return
        viewModelScope.launch {
            val isBookmarked = db.bookmarkDao().isBookmarked(tab.url).first()
            if (isBookmarked) {
                db.bookmarkDao().deleteByUrl(tab.url)
            } else {
                db.bookmarkDao().insert(
                    BookmarkEntity(title = tab.title, url = tab.url)
                )
            }
        }
    }

    fun isBookmarked(url: String) = db.bookmarkDao().isBookmarked(url)

    fun deleteBookmark(bookmark: BookmarkEntity) {
        viewModelScope.launch { db.bookmarkDao().delete(bookmark) }
    }

    // History
    private fun addHistory(title: String, url: String) {
        viewModelScope.launch {
            db.historyDao().insert(HistoryEntity(title = title, url = url))
        }
    }

    fun deleteHistory(id: Long) {
        viewModelScope.launch { db.historyDao().deleteById(id) }
    }

    fun clearHistory() {
        viewModelScope.launch { db.historyDao().deleteAll() }
    }

    // Settings
    fun setAdBlockEnabled(enabled: Boolean) {
        _adBlockEnabled.value = enabled
        prefs.edit().putBoolean("ad_block", enabled).apply()
    }

    fun setDarkMode(enabled: Boolean) {
        _darkMode.value = enabled
        prefs.edit().putBoolean("dark_mode", enabled).apply()
    }

    fun setSearchEngine(engine: SearchEngine) {
        _searchEngine.value = engine
        prefs.edit().putString("search_engine", engine.name).apply()
    }

    fun setFontSize(size: Int) {
        _fontSize.value = size
        prefs.edit().putInt("font_size", size).apply()
        getActiveWebView()?.settings?.textZoom = size
    }

    fun clearAllData() {
        viewModelScope.launch {
            db.bookmarkDao().deleteAll()
            db.historyDao().deleteAll()
        }
    }

    // Screen navigation
    fun showBookmarksScreen() { _showBookmarks.value = true }
    fun hideBookmarksScreen() { _showBookmarks.value = false }
    fun showHistoryScreen() { _showHistory.value = true }
    fun hideHistoryScreen() { _showHistory.value = false }
    fun showSettingsScreen() { _showSettings.value = true }
    fun hideSettingsScreen() { _showSettings.value = false }

    fun shareCurrentPage() {
        // Handled in UI via Intent
    }
}
