package com.example.browser.manager

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import com.example.browser.data.model.Tab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "TabManager"

class TabManager(private val context: Context) {

    private val _tabs = MutableStateFlow(listOf(Tab(isActive = true)))
    val tabs: StateFlow<List<Tab>> = _tabs.asStateFlow()

    private val _activeTabIndex = MutableStateFlow(0)
    val activeTabIndex: StateFlow<Int> = _activeTabIndex.asStateFlow()

    // WebView pool: one WebView per tab, keyed by tab ID
    private val webViewPool = mutableMapOf<String, WebView>()

    fun getActiveTab(): Tab? {
        val idx = _activeTabIndex.value
        return _tabs.value.getOrNull(idx)
    }

    fun getActiveTabId(): String? = getActiveTab()?.id

    fun addTab(incognito: Boolean = false): Tab {
        val current = _tabs.value.toMutableList()
        current.forEachIndexed { i, tab -> current[i] = tab.copy(isActive = false) }
        val newTab = Tab(isActive = true, isIncognito = incognito)
        current.add(newTab)
        _tabs.value = current
        _activeTabIndex.value = current.size - 1
        Log.d(TAG, "Added tab ${newTab.id} (incognito=$incognito), total=${current.size}")
        return newTab
    }

    fun switchTab(index: Int) {
        val current = _tabs.value
        if (index !in current.indices) {
            Log.w(TAG, "switchTab: index $index out of bounds (${current.size})")
            return
        }
        val updated = current.mapIndexed { i, tab -> tab.copy(isActive = i == index) }
        _tabs.value = updated
        _activeTabIndex.value = index
        Log.d(TAG, "Switched to tab $index (${updated[index].id})")
    }

    fun closeTab(index: Int) {
        val current = _tabs.value.toMutableList()
        if (index !in current.indices) return

        val closedTab = current.removeAt(index)
        destroyWebView(closedTab.id)
        Log.d(TAG, "Closed tab ${closedTab.id}")

        if (current.isEmpty()) {
            current.add(Tab(isActive = true))
        }
        val newIndex = if (index >= current.size) current.size - 1 else index
        current.forEachIndexed { i, tab -> current[i] = tab.copy(isActive = i == newIndex) }
        _tabs.value = current
        _activeTabIndex.value = newIndex
    }

    fun updateActiveTabUrl(url: String) {
        val t = _tabs.value.toMutableList()
        val i = _activeTabIndex.value
        if (i in t.indices) {
            t[i] = t[i].copy(url = url)
            _tabs.value = t
        }
    }

    fun updateActiveTabTitle(title: String) {
        val t = _tabs.value.toMutableList()
        val i = _activeTabIndex.value
        if (i in t.indices) {
            t[i] = t[i].copy(title = title)
            _tabs.value = t
        }
    }

    // --- WebView Pool ---

    fun getOrCreateWebView(tabId: String): WebView {
        return webViewPool.getOrPut(tabId) {
            Log.d(TAG, "Creating new WebView for tab $tabId")
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        }
    }

    fun getWebView(tabId: String): WebView? = webViewPool[tabId]

    fun destroyWebView(tabId: String) {
        webViewPool.remove(tabId)?.let { wv ->
            Log.d(TAG, "Destroying WebView for tab $tabId")
            try {
                wv.stopLoading()
                wv.loadUrl("about:blank")
                wv.clearHistory()
                wv.removeAllViews()
                wv.destroy()
            } catch (e: Exception) {
                Log.e(TAG, "Error destroying WebView for tab $tabId", e)
            }
        }
    }

    fun destroyAllWebViews() {
        val ids = webViewPool.keys.toList()
        ids.forEach { destroyWebView(it) }
        Log.d(TAG, "Destroyed all WebViews (${ids.size})")
    }

    fun getWebViewCount(): Int = webViewPool.size
}
