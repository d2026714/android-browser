package com.example.browser.ui.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.browser.data.local.BrowserDatabase
import com.example.browser.data.model.*
import com.example.browser.manager.BookmarkManager
import com.example.browser.manager.SettingsManager
import com.example.browser.manager.TabManager
import com.example.browser.util.AdBlocker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

private const val TAG = "BrowserViewModel"

class BrowserViewModel(application: Application) : AndroidViewModel(application) {
    private val db = BrowserDatabase.getInstance(application)

    // --- Managers ---
    val tabManager = TabManager(application)
    val bookmarkManager = BookmarkManager(
        bookmarkDao = db.bookmarkDao(),
        historyDao = db.historyDao(),
        readingListDao = db.readingListDao(),
        tabGroupDao = db.tabGroupDao(),
        scope = viewModelScope
    )
    val settingsManager = SettingsManager(
        settingsDao = db.settingsDao(),
        quickLinkDao = db.quickLinkDao(),
        scope = viewModelScope
    )

    // --- Delegated flows (backward compat for UI) ---
    val tabs = tabManager.tabs
    val activeTabIndex = tabManager.activeTabIndex
    val bookmarks = bookmarkManager.bookmarks
    val history = bookmarkManager.history
    val readingList = bookmarkManager.readingList
    val tabGroups = bookmarkManager.tabGroups
    val quickLinks = settingsManager.quickLinks
    val isDarkMode = settingsManager.isDarkMode
    val isAmoledDark = settingsManager.isAmoledDark
    val isAdBlockEnabled = settingsManager.isAdBlockEnabled
    val isSearchSuggestions = settingsManager.isSearchSuggestions

    // --- URL bar state ---
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
    private val _isBookmarked = MutableStateFlow(false)
    val isBookmarked: StateFlow<Boolean> = _isBookmarked.asStateFlow()

    // --- Feature state ---
    private val _isDesktopMode = MutableStateFlow(false)
    val isDesktopMode: StateFlow<Boolean> = _isDesktopMode.asStateFlow()
    private val _isFindInPage = MutableStateFlow(false)
    val isFindInPage: StateFlow<Boolean> = _isFindInPage.asStateFlow()
    private val _isReadingMode = MutableStateFlow(false)
    val isReadingMode: StateFlow<Boolean> = _isReadingMode.asStateFlow()
    private val _pageSource = MutableStateFlow<String?>(null)
    val pageSource: StateFlow<String?> = _pageSource.asStateFlow()
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

    // --- Bottom sheet visibility ---
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

    // Active WebView reference (set by BrowserWebView composable)
    private var activeWebView: WebView? = null

    fun setActiveWebView(webView: WebView?) {
        activeWebView = webView
        Log.d(TAG, "Active WebView set: ${webView != null}")
    }

    // --- Navigation ---

    fun navigateTo(url: String) {
        val finalUrl = when {
            url.startsWith("http://") || url.startsWith("https://") || url.startsWith("about:") -> url
            url.contains(".") && !url.contains(" ") -> "https://$url"
            else -> "${settingsManager.searchEngine.value}${url.replace(" ", "+")}"
        }
        Log.d(TAG, "navigateTo: $finalUrl")
        _currentUrl.value = finalUrl
        tabManager.updateActiveTabUrl(finalUrl)
    }

    fun onUrlChanged(url: String) {
        _currentUrl.value = url
        viewModelScope.launch {
            _isBookmarked.value = bookmarkManager.isBookmarked(url)
        }
    }

    fun onTitleChanged(title: String) {
        _currentTitle.value = title
        tabManager.updateActiveTabTitle(title)
    }

    fun onLoadingChanged(loading: Boolean) { _isLoading.value = loading }

    fun onNavigationStateChanged(canGoBack: Boolean, canGoForward: Boolean) {
        _canGoBack.value = canGoBack
        _canGoForward.value = canGoForward
    }

    fun onPageFinished(url: String, title: String) {
        _currentUrl.value = url
        _currentTitle.value = title
        _isLoading.value = false
        tabManager.updateActiveTabUrl(url)
        tabManager.updateActiveTabTitle(title)

        viewModelScope.launch {
            _isBookmarked.value = bookmarkManager.isBookmarked(url)
        }

        if (url != "about:blank") {
            bookmarkManager.addHistory(url, title)
        }

        if (_isDesktopMode.value) applyDesktopMode()
        if (_isCustomCssEnabled.value && _customCss.value.isNotBlank()) {
            injectCustomCss(_customCss.value)
        }
    }

    fun goBack() { activeWebView?.goBack() }
    fun goForward() { activeWebView?.goForward() }
    fun reload() { activeWebView?.reload() }
    fun stopLoading() { activeWebView?.stopLoading() }

    // --- Tab management (delegates to TabManager) ---

    fun addTab(incognito: Boolean = false) {
        tabManager.addTab(incognito)
        _currentUrl.value = ""
        _currentTitle.value = if (incognito) "Incognito" else "New Tab"
        _canGoBack.value = false
        _canGoForward.value = false
    }

    fun switchTab(index: Int) {
        tabManager.switchTab(index)
        val tab = tabManager.tabs.value[index]
        _currentUrl.value = tab.url
        _currentTitle.value = tab.title
    }

    fun closeTab(index: Int) {
        tabManager.closeTab(index)
        val tab = tabManager.getActiveTab()
        if (tab != null) {
            _currentUrl.value = tab.url
            _currentTitle.value = tab.title
        }
    }

    // --- Bookmarks (delegates to BookmarkManager) ---

    fun toggleBookmark() {
        val url = _currentUrl.value
        if (url.isBlank() || url == "about:blank") return
        bookmarkManager.toggleBookmark(url, _currentTitle.value)
        viewModelScope.launch {
            _isBookmarked.value = bookmarkManager.isBookmarked(url)
        }
    }

    fun deleteBookmark(url: String) {
        bookmarkManager.removeBookmark(url)
        viewModelScope.launch {
            _isBookmarked.value = bookmarkManager.isBookmarked(_currentUrl.value)
        }
    }

    fun clearHistory() = bookmarkManager.clearHistory()

    fun addToReadingList() {
        val url = _currentUrl.value
        if (url.isBlank() || url == "about:blank") return
        bookmarkManager.addToReadingList(url, _currentTitle.value)
    }

    fun removeFromReadingList(url: String) = bookmarkManager.removeFromReadingList(url)
    fun markReadingItemRead(url: String) = bookmarkManager.markReadingItemRead(url)

    fun addTabGroup(name: String) {
        val tabIds = tabManager.tabs.value.filter { it.url != "about:blank" }.map { it.id }
        bookmarkManager.addTabGroup(name, tabIds)
    }

    fun removeTabGroup(id: String) = bookmarkManager.removeTabGroup(id)

    // --- Settings (delegates to SettingsManager) ---

    fun toggleDarkMode() = settingsManager.toggleDarkMode()
    fun toggleAmoledDark() = settingsManager.toggleAmoledDark()
    fun toggleAdBlock() = settingsManager.toggleAdBlock()
    fun toggleSearchSuggestions() = settingsManager.toggleSearchSuggestions()
    fun setSearchEngine(url: String) = settingsManager.setSearchEngine(url)
    fun getSearchEngine(): String = settingsManager.searchEngine.value

    fun addQuickLink(link: QuickLink) = settingsManager.addQuickLink(link)
    fun removeQuickLink(id: String) = settingsManager.removeQuickLink(id)
    fun updateQuickLink(link: QuickLink) = settingsManager.updateQuickLink(link)

    // --- Desktop mode ---

    fun toggleDesktopMode() {
        _isDesktopMode.value = !_isDesktopMode.value
        applyDesktopMode()
    }

    private fun applyDesktopMode() {
        activeWebView?.let { wv ->
            val ua = _userAgent.value
            wv.settings.userAgentString = if (_isDesktopMode.value) {
                ua ?: DESKTOP_UA
            } else {
                ua
            }
            wv.reload()
        }
    }

    // --- Find in page ---

    fun toggleFindInPage() { _isFindInPage.value = !_isFindInPage.value }
    fun findInPage(query: String) { activeWebView?.findAllAsync(query) }
    fun findNext() { activeWebView?.findNext(true) }
    fun findPrevious() { activeWebView?.findNext(false) }
    fun clearFindInPage() { activeWebView?.clearMatches(); _isFindInPage.value = false }

    // --- Reading mode ---

    fun toggleReadingMode() { _isReadingMode.value = !_isReadingMode.value }

    // --- Full-screen video ---

    fun enterFullScreen(view: View) {}
    fun exitFullScreen() {}

    // --- View page source ---

    fun viewPageSource() {
        activeWebView?.evaluateJavascript("(function(){return document.documentElement.outerHTML;})()") { html ->
            _pageSource.value = html?.removeSurrounding("\"")
                ?.replace("\\n", "\n")
                ?.replace("\\\"", "\"")
                ?.replace("\\t", "\t")
            _showViewSource.value = true
        }
    }

    fun closeViewSource() { _showViewSource.value = false; _pageSource.value = null }

    // --- Screenshot ---

    fun takeScreenshot() {
        activeWebView?.let { wv ->
            try {
                val bitmap = Bitmap.createBitmap(wv.width, wv.height, Bitmap.Config.ARGB_8888)
                android.graphics.Canvas(bitmap).let { c -> wv.draw(c) }
                val dir = File(getApplication<Application>().cacheDir, "screenshots"); dir.mkdirs()
                val file = File(dir, "screenshot_${System.currentTimeMillis()}.png")
                FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 90, out) }
                val uri = FileProvider.getUriForFile(getApplication(), "${getApplication<Application>().packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                getApplication<Application>().startActivity(Intent.createChooser(intent, "Share Screenshot").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                Log.d(TAG, "Screenshot saved: ${file.absolutePath}")
            } catch (e: Exception) {
                Log.e(TAG, "Screenshot failed", e)
            }
        }
    }

    // --- Misc actions ---

    fun copyLink(url: String) {
        (getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText("URL", url))
    }

    fun shareCurrentPage() {
        val url = _currentUrl.value
        if (url.isBlank() || url == "about:blank") return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, _currentTitle.value)
            putExtra(Intent.EXTRA_TEXT, url)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        getApplication<Application>().startActivity(Intent.createChooser(intent, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun translatePage() {
        val url = _currentUrl.value
        if (url.isBlank()) return
        navigateTo("https://translate.google.com/translate?sl=auto&tl=en&u=$url")
    }

    fun clearSiteData() {
        CookieManager.getInstance().removeAllCookies(null)
        activeWebView?.clearCache(true)
        activeWebView?.reload()
        Log.d(TAG, "Site data cleared")
    }

    fun openInNewTab(url: String) { addTab(); navigateTo(url) }
    fun openInIncognito(url: String) { addTab(incognito = true); navigateTo(url) }

    // --- Blue light filter ---

    fun toggleBlueLightFilter() { _isBlueLightFilter.value = !_isBlueLightFilter.value }
    fun setBlueLightIntensity(intensity: Float) { _blueLightIntensity.value = intensity }

    // --- JavaScript ---

    fun toggleJavaScript() {
        val v = !_isJavaScriptEnabled.value
        _isJavaScriptEnabled.value = v
        activeWebView?.settings?.javaScriptEnabled = v
        activeWebView?.reload()
    }

    // --- Data saver ---

    fun toggleDataSaver() {
        val v = !_isDataSaver.value
        _isDataSaver.value = v
        activeWebView?.let { wv ->
            wv.settings.blockNetworkImage = v
            wv.settings.cacheMode = if (v) WebSettings.LOAD_CACHE_ELSE_NETWORK else WebSettings.LOAD_DEFAULT
            wv.reload()
        }
    }

    // --- User agent ---

    fun setUserAgent(ua: String?) {
        _userAgent.value = ua
        activeWebView?.settings?.userAgentString = ua
        activeWebView?.reload()
    }

    // --- Zoom ---

    fun setZoomLevel(level: Int) { activeWebView?.settings?.textZoom = level }

    // --- Custom CSS ---

    fun getCustomCss(): String = _customCss.value
    fun isCustomCssEnabled(): Boolean = _isCustomCssEnabled.value

    fun setCustomCss(css: String) {
        _customCss.value = css
        if (_isCustomCssEnabled.value) activeWebView?.reload()
    }

    fun toggleCustomCss() {
        _isCustomCssEnabled.value = !_isCustomCssEnabled.value
        activeWebView?.reload()
    }

    private fun injectCustomCss(css: String) {
        val escaped = css.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n")
        activeWebView?.evaluateJavascript(
            "(function(){var s=document.createElement('style');s.textContent='$escaped';document.head.appendChild(s);})()",
            null
        )
    }

    // --- Print ---

    fun printPage() {
        activeWebView?.let { wv ->
            try {
                val printManager = getApplication<Application>().getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
                val printAdapter = wv.createPrintDocumentAdapter("Browser Page")
                printManager.print("Browser Page", printAdapter, android.print.PrintAttributes.Builder().build())
                Log.d(TAG, "Print initiated")
            } catch (e: Exception) {
                Log.e(TAG, "Print failed", e)
            }
        }
    }

    // --- UI toggles ---

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
        _showBookmarks.value = false; _showHistory.value = false; _showTabs.value = false
        _showSettings.value = false; _showSearchEngineSheet.value = false; _showDownloads.value = false
        _showViewSource.value = false; _showReadingList.value = false; _showTabGroups.value = false
        _showQuickLinksEditor.value = false; _showQrCode.value = false; _showPageInfo.value = false
        _showBackupRestore.value = false; _showUserAgent.value = false; _showZoomControl.value = false
        _showCustomCss.value = false
    }

    override fun onCleared() {
        super.onCleared()
        tabManager.destroyAllWebViews()
        Log.d(TAG, "ViewModel cleared, all WebViews destroyed")
    }

    companion object {
        private const val DESKTOP_UA = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
}
