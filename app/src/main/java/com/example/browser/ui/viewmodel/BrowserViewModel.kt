package com.example.browser.ui.viewmodel

import android.app.Application
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import com.example.browser.data.model.*
import com.example.browser.data.repository.BrowserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream

class BrowserViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = BrowserRepository.getInstance(application)

    // Tabs
    private val _tabs = MutableStateFlow(listOf(Tab(isActive = true)))
    val tabs: StateFlow<List<Tab>> = _tabs.asStateFlow()
    private val _activeTabIndex = MutableStateFlow(0)
    val activeTabIndex: StateFlow<Int> = _activeTabIndex.asStateFlow()
    private val _tabGroups = MutableStateFlow(repository.getTabGroups())
    val tabGroups: StateFlow<List<TabGroup>> = _tabGroups.asStateFlow()

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

    // Bookmarks & History
    private val _bookmarks = MutableStateFlow(repository.getBookmarks())
    val bookmarks: StateFlow<List<Bookmark>> = _bookmarks.asStateFlow()
    private val _isBookmarked = MutableStateFlow(false)
    val isBookmarked: StateFlow<Boolean> = _isBookmarked.asStateFlow()
    private val _history = MutableStateFlow(repository.getHistory())
    val history: StateFlow<List<HistoryItem>> = _history.asStateFlow()

    // Reading list & Quick links
    private val _readingList = MutableStateFlow(repository.getReadingList())
    val readingList: StateFlow<List<ReadingItem>> = _readingList.asStateFlow()
    private val _quickLinks = MutableStateFlow(repository.getQuickLinks())
    val quickLinks: StateFlow<List<QuickLink>> = _quickLinks.asStateFlow()

    // Settings
    private val _isDarkMode = MutableStateFlow(repository.isDarkMode())
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()
    private val _isAmoledDark = MutableStateFlow(repository.isAmoledDark())
    val isAmoledDark: StateFlow<Boolean> = _isAmoledDark.asStateFlow()
    private val _isAdBlockEnabled = MutableStateFlow(repository.isAdBlockEnabled())
    val isAdBlockEnabled: StateFlow<Boolean> = _isAdBlockEnabled.asStateFlow()
    private val _isSearchSuggestions = MutableStateFlow(repository.isSearchSuggestionsEnabled())
    val isSearchSuggestions: StateFlow<Boolean> = _isSearchSuggestions.asStateFlow()

    // Features state
    private val _isDesktopMode = MutableStateFlow(false)
    val isDesktopMode: StateFlow<Boolean> = _isDesktopMode.asStateFlow()
    private val _isFindInPage = MutableStateFlow(false)
    val isFindInPage: StateFlow<Boolean> = _isFindInPage.asStateFlow()
    private val _isReadingMode = MutableStateFlow(false)
    val isReadingMode: StateFlow<Boolean> = _isReadingMode.asStateFlow()
    private val _pageSource = MutableStateFlow<String?>(null)
    val pageSource: StateFlow<String?> = _pageSource.asStateFlow()

    // New features
    private val _isBlueLightFilter = MutableStateFlow(false)
    val isBlueLightFilter: StateFlow<Boolean> = _isBlueLightFilter.asStateFlow()
    private val _blueLightIntensity = MutableStateFlow(0.4f)
    val blueLightIntensity: StateFlow<Float> = _blueLightIntensity.asStateFlow()
    private val _isJavaScriptEnabled = MutableStateFlow(true)
    val isJavaScriptEnabled: StateFlow<Boolean> = _isJavaScriptEnabled.asStateFlow()
    private val _isDataSaver = MutableStateFlow(false)
    val isDataSaver: StateFlow<Boolean> = _isDataSaver.asStateFlow()
    private val _customCss = MutableStateFlow("")
    val customCss: StateFlow<String> = _customCss.asStateFlow()
    private val _isCustomCssEnabled = MutableStateFlow(false)
    val isCustomCssEnabled: StateFlow<Boolean> = _isCustomCssEnabled.asStateFlow()
    private val _userAgent = MutableStateFlow<String?>(null)
    val userAgent: StateFlow<String?> = _userAgent.asStateFlow()

    // Bottom sheets
    private val _showBookmarks = MutableStateFlow(false); val showBookmarks: StateFlow<Boolean> = _showBookmarks.asStateFlow()
    private val _showHistory = MutableStateFlow(false); val showHistory: StateFlow<Boolean> = _showHistory.asStateFlow()
    private val _showTabs = MutableStateFlow(false); val showTabs: StateFlow<Boolean> = _showTabs.asStateFlow()
    private val _showSettings = MutableStateFlow(false); val showSettings: StateFlow<Boolean> = _showSettings.asStateFlow()
    private val _showSearchEngineSheet = MutableStateFlow(false); val showSearchEngineSheet: StateFlow<Boolean> = _showSearchEngineSheet.asStateFlow()
    private val _showDownloads = MutableStateFlow(false); val showDownloads: StateFlow<Boolean> = _showDownloads.asStateFlow()
    private val _showViewSource = MutableStateFlow(false); val showViewSource: StateFlow<Boolean> = _showViewSource.asStateFlow()
    private val _showReadingList = MutableStateFlow(false); val showReadingList: StateFlow<Boolean> = _showReadingList.asStateFlow()
    private val _showTabGroups = MutableStateFlow(false); val showTabGroups: StateFlow<Boolean> = _showTabGroups.asStateFlow()
    private val _showQuickLinksEditor = MutableStateFlow(false); val showQuickLinksEditor: StateFlow<Boolean> = _showQuickLinksEditor.asStateFlow()
    private val _showQrCode = MutableStateFlow(false); val showQrCode: StateFlow<Boolean> = _showQrCode.asStateFlow()
    private val _showPageInfo = MutableStateFlow(false); val showPageInfo: StateFlow<Boolean> = _showPageInfo.asStateFlow()
    private val _showBackupRestore = MutableStateFlow(false); val showBackupRestore: StateFlow<Boolean> = _showBackupRestore.asStateFlow()
    private val _showUserAgent = MutableStateFlow(false); val showUserAgent: StateFlow<Boolean> = _showUserAgent.asStateFlow()
    private val _showZoomControl = MutableStateFlow(false); val showZoomControl: StateFlow<Boolean> = _showZoomControl.asStateFlow()
    private val _showCustomCss = MutableStateFlow(false); val showCustomCss: StateFlow<Boolean> = _showCustomCss.asStateFlow()

    private var webViewRef: WebView? = null

    fun setWebView(webView: WebView?) { webViewRef = webView }

    // Navigation
    fun navigateTo(url: String) {
        val finalUrl = when {
            url.startsWith("http://") || url.startsWith("https://") || url.startsWith("about:") -> url
            url.contains(".") && !url.contains(" ") -> "https://$url"
            else -> "${repository.getSearchEngine()}${url.replace(" ", "+")}"
        }
        _currentUrl.value = finalUrl; updateTabUrl(finalUrl)
    }

    fun onUrlChanged(url: String) { _currentUrl.value = url; _isBookmarked.value = repository.isBookmarked(url) }
    fun onTitleChanged(title: String) { _currentTitle.value = title; updateTabTitle(title) }
    fun onLoadingChanged(loading: Boolean) { _isLoading.value = loading }
    fun onNavigationStateChanged(canGoBack: Boolean, canGoForward: Boolean) { _canGoBack.value = canGoBack; _canGoForward.value = canGoForward }

    fun onPageFinished(url: String, title: String) {
        _currentUrl.value = url; _currentTitle.value = title; _isLoading.value = false
        _isBookmarked.value = repository.isBookmarked(url)
        updateTabUrl(url); updateTabTitle(title)
        if (url != "about:blank") { repository.addHistory(HistoryItem(url = url, title = title)); _history.value = repository.getHistory() }
        if (_isDesktopMode.value) applyDesktopMode()
        // Apply custom CSS
        if (_isCustomCssEnabled.value && _customCss.value.isNotBlank()) {
            val escaped = _customCss.value.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n")
            webViewRef?.evaluateJavascript("(function(){var s=document.createElement('style');s.textContent='$escaped';document.head.appendChild(s);})()", null)
        }
    }

    fun goBack() { webViewRef?.goBack() }
    fun goForward() { webViewRef?.goForward() }
    fun reload() { webViewRef?.reload() }
    fun stopLoading() { webViewRef?.stopLoading() }

    // Tab management
    fun addTab(incognito: Boolean = false) {
        val t = _tabs.value.toMutableList()
        t.forEachIndexed { i, tab -> t[i] = tab.copy(isActive = false) }
        t.add(Tab(isActive = true, isIncognito = incognito))
        _tabs.value = t; _activeTabIndex.value = t.size - 1
        _currentUrl.value = ""; _currentTitle.value = if (incognito) "Incognito" else "New Tab"
        _canGoBack.value = false; _canGoForward.value = false
    }
    fun switchTab(index: Int) {
        val t = _tabs.value.toMutableList(); t.forEachIndexed { i, tab -> t[i] = tab.copy(isActive = i == index) }
        _tabs.value = t; _activeTabIndex.value = index
        _currentUrl.value = t[index].url; _currentTitle.value = t[index].title
    }
    fun closeTab(index: Int) {
        val t = _tabs.value.toMutableList()
        if (t.size <= 1) { t[0] = Tab(isActive = true); _tabs.value = t; _activeTabIndex.value = 0; _currentUrl.value = ""; _currentTitle.value = "New Tab"; return }
        t.removeAt(index); val ni = if (index >= t.size) t.size - 1 else index
        t.forEachIndexed { i, tab -> t[i] = tab.copy(isActive = i == ni) }
        _tabs.value = t; _activeTabIndex.value = ni; _currentUrl.value = t[ni].url; _currentTitle.value = t[ni].title
    }
    private fun updateTabUrl(url: String) { val t = _tabs.value.toMutableList(); val i = _activeTabIndex.value; if (i in t.indices) { t[i] = t[i].copy(url = url); _tabs.value = t } }
    private fun updateTabTitle(title: String) { val t = _tabs.value.toMutableList(); val i = _activeTabIndex.value; if (i in t.indices) { t[i] = t[i].copy(title = title); _tabs.value = t } }

    // Tab groups
    fun addTabGroup(name: String) { repository.addTabGroup(TabGroup(name = name, tabIds = _tabs.value.filter { it.url != "about:blank" }.map { it.id })); _tabGroups.value = repository.getTabGroups() }
    fun removeTabGroup(id: String) { repository.removeTabGroup(id); _tabGroups.value = repository.getTabGroups() }

    // Bookmarks
    fun toggleBookmark() { val url = _currentUrl.value; if (url.isBlank() || url == "about:blank") return; if (repository.isBookmarked(url)) repository.removeBookmark(url) else repository.addBookmark(Bookmark(url = url, title = _currentTitle.value)); _bookmarks.value = repository.getBookmarks(); _isBookmarked.value = repository.isBookmarked(url) }
    fun deleteBookmark(url: String) { repository.removeBookmark(url); _bookmarks.value = repository.getBookmarks(); _isBookmarked.value = repository.isBookmarked(_currentUrl.value) }

    // History
    fun clearHistory() { repository.clearHistory(); _history.value = emptyList() }

    // Reading list
    fun addToReadingList() { val url = _currentUrl.value; if (url.isBlank() || url == "about:blank") return; repository.addToReadingList(ReadingItem(url = url, title = _currentTitle.value)); _readingList.value = repository.getReadingList() }
    fun removeFromReadingList(url: String) { repository.removeFromReadingList(url); _readingList.value = repository.getReadingList() }
    fun markReadingItemRead(url: String) { repository.markReadingItemRead(url); _readingList.value = repository.getReadingList() }

    // Quick links
    fun addQuickLink(link: QuickLink) { repository.addQuickLink(link); _quickLinks.value = repository.getQuickLinks() }
    fun removeQuickLink(id: String) { repository.removeQuickLink(id); _quickLinks.value = repository.getQuickLinks() }
    fun updateQuickLink(link: QuickLink) { repository.updateQuickLink(link); _quickLinks.value = repository.getQuickLinks() }

    // Settings
    fun toggleDarkMode() { val v = !_isDarkMode.value; _isDarkMode.value = v; repository.setDarkMode(v) }
    fun toggleAmoledDark() { val v = !_isAmoledDark.value; _isAmoledDark.value = v; repository.setAmoledDark(v) }
    fun toggleAdBlock() { val v = !_isAdBlockEnabled.value; _isAdBlockEnabled.value = v; repository.setAdBlockEnabled(v) }
    fun toggleSearchSuggestions() { val v = !_isSearchSuggestions.value; _isSearchSuggestions.value = v; repository.setSearchSuggestionsEnabled(v) }
    fun setSearchEngine(url: String) { repository.setSearchEngine(url) }
    fun getSearchEngine(): String = repository.getSearchEngine()

    // Desktop mode
    fun toggleDesktopMode() { _isDesktopMode.value = !_isDesktopMode.value; applyDesktopMode() }
    private fun applyDesktopMode() { webViewRef?.let { wv -> wv.settings.userAgentString = _userAgent.value ?: if (_isDesktopMode.value) DESKTOP_UA else null; wv.reload() } }

    // Find in page
    fun toggleFindInPage() { _isFindInPage.value = !_isFindInPage.value }
    fun findInPage(query: String) { webViewRef?.findAllAsync(query) }
    fun findNext() { webViewRef?.findNext(true) }
    fun findPrevious() { webViewRef?.findNext(false) }
    fun clearFindInPage() { webViewRef?.clearMatches(); _isFindInPage.value = false }

    // Reading mode
    fun toggleReadingMode() { _isReadingMode.value = !_isReadingMode.value }

    // Full-screen video
    fun enterFullScreen(view: View) {}
    fun exitFullScreen() {}

    // View page source
    fun viewPageSource() {
        webViewRef?.evaluateJavascript("(function(){return document.documentElement.outerHTML;})()") { html ->
            _pageSource.value = html?.removeSurrounding("\"")?.replace("\\n", "\n")?.replace("\\\"", "\"")?.replace("\\t", "\t")
            _showViewSource.value = true
        }
    }
    fun closeViewSource() { _showViewSource.value = false; _pageSource.value = null }

    // Screenshot
    fun takeScreenshot() {
        webViewRef?.let { wv ->
            try {
                val bitmap = Bitmap.createBitmap(wv.width, wv.height, Bitmap.Config.ARGB_8888)
                android.graphics.Canvas(bitmap).let { c -> wv.draw(c) }
                val dir = File(getApplication<Application>().cacheDir, "screenshots"); dir.mkdirs()
                val file = File(dir, "screenshot_${System.currentTimeMillis()}.png")
                FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 90, out) }
                val uri = FileProvider.getUriForFile(getApplication(), "${getApplication<Application>().packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply { type = "image/png"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                getApplication<Application>().startActivity(Intent.createChooser(intent, "Share Screenshot").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: Exception) {}
        }
    }

    fun copyLink(url: String) { (getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("URL", url)) }
    fun shareCurrentPage() { val url = _currentUrl.value; if (url.isBlank() || url == "about:blank") return; val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_SUBJECT, _currentTitle.value); putExtra(Intent.EXTRA_TEXT, url); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }; getApplication<Application>().startActivity(Intent.createChooser(intent, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    fun translatePage() { val url = _currentUrl.value; if (url.isBlank()) return; navigateTo("https://translate.google.com/translate?sl=auto&tl=en&u=$url") }
    fun clearSiteData() { CookieManager.getInstance().removeAllCookies(null); webViewRef?.clearCache(true); webViewRef?.reload() }
    fun openInNewTab(url: String) { addTab(); navigateTo(url) }
    fun openInIncognito(url: String) { addTab(incognito = true); navigateTo(url) }

    // Blue light filter
    fun toggleBlueLightFilter() { _isBlueLightFilter.value = !_isBlueLightFilter.value }
    fun setBlueLightIntensity(intensity: Float) { _blueLightIntensity.value = intensity }

    // JavaScript
    fun toggleJavaScript() { val v = !_isJavaScriptEnabled.value; _isJavaScriptEnabled.value = v; webViewRef?.settings?.javaScriptEnabled = v; webViewRef?.reload() }

    // Data saver
    fun toggleDataSaver() {
        val v = !_isDataSaver.value; _isDataSaver.value = v
        webViewRef?.let { wv ->
            wv.settings.blockNetworkImage = v
            if (v) wv.settings.cacheMode = android.webkit.WebSettings.LOAD_CACHE_ELSE_NETWORK
            else wv.settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
            wv.reload()
        }
    }

    // User agent
    fun setUserAgent(ua: String?) { _userAgent.value = ua; webViewRef?.settings?.userAgentString = ua; webViewRef?.reload() }

    // Zoom
    fun setZoomLevel(level: Int) { webViewRef?.settings?.textZoom = level }

    // Custom CSS
    fun getCustomCss(): String = _customCss.value
    fun isCustomCssEnabled(): Boolean = _isCustomCssEnabled.value
    fun setCustomCss(css: String) { _customCss.value = css; if (_isCustomCssEnabled.value) webViewRef?.reload() }
    fun toggleCustomCss() { _isCustomCssEnabled.value = !_isCustomCssEnabled.value; webViewRef?.reload() }

    // Print
    fun printPage() {
        webViewRef?.let { wv ->
            try {
                val printManager = getApplication<Application>().getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
                val printAdapter = wv.createPrintDocumentAdapter("Browser Page")
                printManager.print("Browser Page", printAdapter, android.print.PrintAttributes.Builder().build())
            } catch (_: Exception) {}
        }
    }

    // UI toggles
    fun toggleBookmarks() { _showBookmarks.value = !_showBookmarks.value }
    fun toggleHistory() { _showHistory.value = !_showHistory.value }
    fun toggleTabs() { _showTabs.value = !_showTabs.value }
    fun toggleSettings() { _showSettings.value = !_showSettings.value }
    fun toggleSearchEngineSheet() { _showSearchEngineSheet.value = !_showSearchEngineSheet.value }
    fun toggleDownloads() { _showDownloads.value = !_showDownloads.value }
    fun toggleReadingList() { _showReadingList.value = !_showReadingList.value }
    fun toggleTabGroups() { _showTabGroups.value = !_showTabGroups.value }
    fun toggleQuickLinksEditor() { _showQuickLinksEditor.value = !_showQuickLinksEditor.value }
    fun toggleQrCode() { _showQrCode.value = !_showQrCode.value }
    fun togglePageInfo() { _showPageInfo.value = !_showPageInfo.value }
    fun toggleBackupRestore() { _showBackupRestore.value = !_showBackupRestore.value }
    fun toggleUserAgent() { _showUserAgent.value = !_showUserAgent.value }
    fun toggleZoomControl() { _showZoomControl.value = !_showZoomControl.value }
    fun toggleCustomCssSheet() { _showCustomCss.value = !_showCustomCss.value }

    fun hideOverlays() {
        _showBookmarks.value = false; _showHistory.value = false; _showTabs.value = false; _showSettings.value = false
        _showSearchEngineSheet.value = false; _showDownloads.value = false; _showViewSource.value = false
        _showReadingList.value = false; _showTabGroups.value = false; _showQuickLinksEditor.value = false
        _showQrCode.value = false; _showPageInfo.value = false; _showBackupRestore.value = false
        _showUserAgent.value = false; _showZoomControl.value = false; _showCustomCss.value = false
    }

    companion object {
        private const val DESKTOP_UA = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
}
