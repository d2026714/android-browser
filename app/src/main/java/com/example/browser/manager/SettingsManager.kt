package com.example.browser.manager

import android.util.Log
import com.example.browser.data.local.dao.QuickLinkDao
import com.example.browser.data.local.dao.SettingsDao
import com.example.browser.data.local.entity.QuickLinkEntity
import com.example.browser.data.local.entity.SettingsEntity
import com.example.browser.data.model.QuickLink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val TAG = "SettingsManager"

class SettingsManager(
    private val settingsDao: SettingsDao,
    private val quickLinkDao: QuickLinkDao,
    private val scope: CoroutineScope
) {
    companion object {
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_AMOLED_DARK = "amoled_dark"
        const val KEY_AD_BLOCK = "ad_block"
        const val KEY_SEARCH_ENGINE = "search_engine"
        const val KEY_SEARCH_SUGGESTIONS = "search_suggestions"
        const val DEFAULT_SEARCH_ENGINE = "https://www.google.com/search?q="

        private val DEFAULT_QUICK_LINKS = listOf(
            QuickLinkEntity(id = "g", title = "Google", url = "https://www.google.com", icon = "search", position = 0),
            QuickLinkEntity(id = "yt", title = "YouTube", url = "https://www.youtube.com", icon = "play", position = 1),
            QuickLinkEntity(id = "w", title = "Wikipedia", url = "https://www.wikipedia.org", icon = "book", position = 2),
            QuickLinkEntity(id = "gh", title = "GitHub", url = "https://github.com", icon = "code", position = 3),
            QuickLinkEntity(id = "r", title = "Reddit", url = "https://www.reddit.com", icon = "forum", position = 4),
            QuickLinkEntity(id = "t", title = "Twitter", url = "https://twitter.com", icon = "tag", position = 5),
            QuickLinkEntity(id = "a", title = "Amazon", url = "https://www.amazon.com", icon = "cart", position = 6),
            QuickLinkEntity(id = "n", title = "Netflix", url = "https://www.netflix.com", icon = "movie", position = 7),
        )
    }

    // Eagerly loaded settings as StateFlows
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _isAmoledDark = MutableStateFlow(false)
    val isAmoledDark: StateFlow<Boolean> = _isAmoledDark.asStateFlow()

    private val _isAdBlockEnabled = MutableStateFlow(true)
    val isAdBlockEnabled: StateFlow<Boolean> = _isAdBlockEnabled.asStateFlow()

    private val _isSearchSuggestions = MutableStateFlow(true)
    val isSearchSuggestions: StateFlow<Boolean> = _isSearchSuggestions.asStateFlow()

    private val _searchEngine = MutableStateFlow(DEFAULT_SEARCH_ENGINE)
    val searchEngine: StateFlow<String> = _searchEngine.asStateFlow()

    init {
        // Load settings from DB into StateFlows
        scope.launch {
            try {
                _isDarkMode.value = settingsDao.getValue(KEY_DARK_MODE)?.toBoolean() ?: false
                _isAmoledDark.value = settingsDao.getValue(KEY_AMOLED_DARK)?.toBoolean() ?: false
                _isAdBlockEnabled.value = settingsDao.getValue(KEY_AD_BLOCK)?.toBoolean() ?: true
                _isSearchSuggestions.value = settingsDao.getValue(KEY_SEARCH_SUGGESTIONS)?.toBoolean() ?: true
                _searchEngine.value = settingsDao.getValue(KEY_SEARCH_ENGINE) ?: DEFAULT_SEARCH_ENGINE
                Log.d(TAG, "Settings loaded from DB")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load settings", e)
            }
        }

        // Ensure default quick links exist
        scope.launch {
            try {
                val existing = quickLinkDao.getAll()
                if (existing.isEmpty()) {
                    quickLinkDao.insertAll(DEFAULT_QUICK_LINKS)
                    Log.d(TAG, "Inserted default quick links")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to init quick links", e)
            }
        }
    }

    val quickLinks: Flow<List<QuickLink>> = quickLinkDao.getAllFlow().map { list ->
        list.map { QuickLink(id = it.id, title = it.title, url = it.url, icon = it.icon, position = it.position) }
    }

    // --- Toggle/set methods ---

    fun toggleDarkMode() = toggleSetting(KEY_DARK_MODE, _isDarkMode)
    fun toggleAmoledDark() = toggleSetting(KEY_AMOLED_DARK, _isAmoledDark)
    fun toggleAdBlock() = toggleSetting(KEY_AD_BLOCK, _isAdBlockEnabled)
    fun toggleSearchSuggestions() = toggleSetting(KEY_SEARCH_SUGGESTIONS, _isSearchSuggestions)

    fun setSearchEngine(url: String) {
        _searchEngine.value = url
        scope.launch {
            try {
                settingsDao.setValue(SettingsEntity(KEY_SEARCH_ENGINE, url))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set search engine", e)
            }
        }
    }

    private fun toggleSetting(key: String, stateFlow: MutableStateFlow<Boolean>) {
        val newValue = !stateFlow.value
        stateFlow.value = newValue
        scope.launch {
            try {
                settingsDao.setValue(SettingsEntity(key, newValue.toString()))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save setting $key", e)
            }
        }
    }

    // --- Quick Links ---

    fun addQuickLink(link: QuickLink) {
        scope.launch {
            try {
                quickLinkDao.insert(QuickLinkEntity(link.id, link.title, link.url, link.icon, link.position))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add quick link", e)
            }
        }
    }

    fun removeQuickLink(id: String) {
        scope.launch {
            try {
                quickLinkDao.deleteById(id)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove quick link $id", e)
            }
        }
    }

    fun updateQuickLink(link: QuickLink) {
        scope.launch {
            try {
                quickLinkDao.insert(QuickLinkEntity(link.id, link.title, link.url, link.icon, link.position))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update quick link", e)
            }
        }
    }
}
