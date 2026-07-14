package com.example.browser.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.browser.data.model.Bookmark
import com.example.browser.data.model.HistoryItem
import com.example.browser.data.model.Tab
import com.example.browser.data.repository.BrowserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BrowserViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = BrowserRepository.getInstance(application)

    // Tabs
    private val _tabs = MutableStateFlow(listOf(Tab(isActive = true)))
    val tabs: StateFlow<List<Tab>> = _tabs.asStateFlow()

    private val _activeTabIndex = MutableStateFlow(0)
    val activeTabIndex: StateFlow<Int> = _activeTabIndex.asStateFlow()

    // URL bar
    private val _currentUrl = MutableStateFlow("")
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()

    private val _currentTitle = MutableStateFlow("New Tab")
    val currentTitle: StateFlow<String> = _currentTitle.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _canGoBack = MutableStateFlow(false)
    val canGoBack: StateFlow<Boolean> = _canGoBack.asStateFlow()

    private val _canGoForward = MutableStateFlow(false)
    val canGoForward: StateFlow<Boolean> = _canGoForward.asStateFlow()

    // Bookmarks
    private val _bookmarks = MutableStateFlow(repository.getBookmarks())
    val bookmarks: StateFlow<List<Bookmark>> = _bookmarks.asStateFlow()

    private val _isBookmarked = MutableStateFlow(false)
    val isBookmarked: StateFlow<Boolean> = _isBookmarked.asStateFlow()

    // History
    private val _history = MutableStateFlow(repository.getHistory())
    val history: StateFlow<List<HistoryItem>> = _history.asStateFlow()

    // Settings
    private val _isDarkMode = MutableStateFlow(repository.isDarkMode())
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _isAdBlockEnabled = MutableStateFlow(repository.isAdBlockEnabled())
    val isAdBlockEnabled: StateFlow<Boolean> = _isAdBlockEnabled.asStateFlow()

    // Screen state
    private val _showBookmarks = MutableStateFlow(false)
    val showBookmarks: StateFlow<Boolean> = _showBookmarks.asStateFlow()

    private val _showHistory = MutableStateFlow(false)
    val showHistory: StateFlow<Boolean> = _showHistory.asStateFlow()

    private val _showTabs = MutableStateFlow(false)
    val showTabs: StateFlow<Boolean> = _showTabs.asStateFlow()

    private val _showSettings = MutableStateFlow(false)
    val showSettings: StateFlow<Boolean> = _showSettings.asStateFlow()

    fun navigateTo(url: String) {
        val finalUrl = if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("about:")) {
            url
        } else if (url.contains(".") && !url.contains(" ")) {
            "https://$url"
        } else {
            "${repository.getSearchEngine()}${url.replace(" ", "+")}"
        }
        _currentUrl.value = finalUrl
        updateTabUrl(finalUrl)
    }

    fun onUrlChanged(url: String) {
        _currentUrl.value = url
        _isBookmarked.value = repository.isBookmarked(url)
    }

    fun onTitleChanged(title: String) {
        _currentTitle.value = title
        updateTabTitle(title)
    }

    fun onLoadingChanged(loading: Boolean) {
        _isLoading.value = loading
    }

    fun onNavigationStateChanged(canGoBack: Boolean, canGoForward: Boolean) {
        _canGoBack.value = canGoBack
        _canGoForward.value = canGoForward
    }

    fun onPageFinished(url: String, title: String) {
        _currentUrl.value = url
        _currentTitle.value = title
        _isLoading.value = false
        _isBookmarked.value = repository.isBookmarked(url)
        updateTabUrl(url)
        updateTabTitle(title)
        if (url != "about:blank") {
            repository.addHistory(HistoryItem(url = url, title = title))
            _history.value = repository.getHistory()
        }
    }

    // Tab management
    fun addTab(incognito: Boolean = false) {
        val currentTabs = _tabs.value.toMutableList()
        currentTabs.forEachIndexed { i, tab ->
            currentTabs[i] = tab.copy(isActive = false)
        }
        currentTabs.add(Tab(isActive = true, isIncognito = incognito))
        _tabs.value = currentTabs
        _activeTabIndex.value = currentTabs.size - 1
        _currentUrl.value = ""
        _currentTitle.value = if (incognito) "Incognito" else "New Tab"
        _canGoBack.value = false
        _canGoForward.value = false
    }

    fun switchTab(index: Int) {
        val currentTabs = _tabs.value.toMutableList()
        currentTabs.forEachIndexed { i, tab ->
            currentTabs[i] = tab.copy(isActive = i == index)
        }
        _tabs.value = currentTabs
        _activeTabIndex.value = index
        val activeTab = currentTabs[index]
        _currentUrl.value = activeTab.url
        _currentTitle.value = activeTab.title
    }

    fun closeTab(index: Int) {
        val currentTabs = _tabs.value.toMutableList()
        if (currentTabs.size <= 1) {
            // Don't close the last tab, reset it instead
            currentTabs[0] = Tab(isActive = true)
            _tabs.value = currentTabs
            _activeTabIndex.value = 0
            _currentUrl.value = ""
            _currentTitle.value = "New Tab"
            return
        }
        currentTabs.removeAt(index)
        val newActiveIndex = if (index >= currentTabs.size) currentTabs.size - 1 else index
        currentTabs.forEachIndexed { i, tab ->
            currentTabs[i] = tab.copy(isActive = i == newActiveIndex)
        }
        _tabs.value = currentTabs
        _activeTabIndex.value = newActiveIndex
        _currentUrl.value = currentTabs[newActiveIndex].url
        _currentTitle.value = currentTabs[newActiveIndex].title
    }

    private fun updateTabUrl(url: String) {
        val currentTabs = _tabs.value.toMutableList()
        val activeIndex = _activeTabIndex.value
        if (activeIndex in currentTabs.indices) {
            currentTabs[activeIndex] = currentTabs[activeIndex].copy(url = url)
            _tabs.value = currentTabs
        }
    }

    private fun updateTabTitle(title: String) {
        val currentTabs = _tabs.value.toMutableList()
        val activeIndex = _activeTabIndex.value
        if (activeIndex in currentTabs.indices) {
            currentTabs[activeIndex] = currentTabs[activeIndex].copy(title = title)
            _tabs.value = currentTabs
        }
    }

    // Bookmark management
    fun toggleBookmark() {
        val url = _currentUrl.value
        if (url.isBlank() || url == "about:blank") return
        if (repository.isBookmarked(url)) {
            repository.removeBookmark(url)
        } else {
            repository.addBookmark(Bookmark(url = url, title = _currentTitle.value))
        }
        _bookmarks.value = repository.getBookmarks()
        _isBookmarked.value = repository.isBookmarked(url)
    }

    fun deleteBookmark(url: String) {
        repository.removeBookmark(url)
        _bookmarks.value = repository.getBookmarks()
        _isBookmarked.value = repository.isBookmarked(_currentUrl.value)
    }

    // History management
    fun clearHistory() {
        repository.clearHistory()
        _history.value = emptyList()
    }

    // Settings
    fun toggleDarkMode() {
        val newValue = !_isDarkMode.value
        _isDarkMode.value = newValue
        repository.setDarkMode(newValue)
    }

    fun toggleAdBlock() {
        val newValue = !_isAdBlockEnabled.value
        _isAdBlockEnabled.value = newValue
        repository.setAdBlockEnabled(newValue)
    }

    // UI toggles
    fun toggleBookmarks() { _showBookmarks.value = !_showBookmarks.value }
    fun toggleHistory() { _showHistory.value = !_showHistory.value }
    fun toggleTabs() { _showTabs.value = !_showTabs.value }
    fun toggleSettings() { _showSettings.value = !_showSettings.value }

    fun hideOverlays() {
        _showBookmarks.value = false
        _showHistory.value = false
        _showTabs.value = false
        _showSettings.value = false
    }
}
