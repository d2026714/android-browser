package com.example.browser.gecko

import android.content.Context
import android.util.Log
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings

private const val TAG = "GeckoBrowserEngine"

interface GeckoBrowserCallback {
    fun onPageStarted(url: String)
    fun onPageFinished(url: String, title: String)
    fun onProgressChanged(progress: Int)
    fun onTitleChanged(title: String)
    fun onUrlChanged(url: String)
    fun onCanGoBackChanged(canGoBack: Boolean)
    fun onCanGoForwardChanged(canGoForward: Boolean)
    fun onPageError(errorCode: Int, description: String, url: String?)
    fun onSecurityChange(isSecure: Boolean)
    fun onMediaUrlDetected(url: String, title: String)
}

class GeckoBrowserEngine private constructor(private val context: Context) {

    private var runtime: GeckoRuntime? = null
    private val sessions = mutableMapOf<String, GeckoSession>()
    private val callbacks = mutableMapOf<String, GeckoBrowserCallback>()
    private val currentUrlMap = mutableMapOf<String, String>()
    private val currentTitleMap = mutableMapOf<String, String>()
    private val canGoBackMap = mutableMapOf<String, Boolean>()
    private val canGoForwardMap = mutableMapOf<String, Boolean>()

    fun initialize() {
        if (runtime != null) {
            Log.d(TAG, "GeckoRuntime already initialized")
            return
        }
        val settings = GeckoRuntimeSettings.Builder()
            .consoleOutput(true)
            .remoteDebuggingEnabled(false)
            .build()
        runtime = GeckoRuntime.create(context, settings)
        Log.d(TAG, "GeckoRuntime initialized")
    }

    fun getRuntime(): GeckoRuntime {
        return runtime ?: throw IllegalStateException("GeckoRuntime not initialized.")
    }

    fun getOrCreateSession(tabId: String): GeckoSession {
        return sessions.getOrPut(tabId) {
            Log.d(TAG, "Creating new GeckoSession for tab $tabId")
            createSession(tabId)
        }
    }

    fun getSession(tabId: String): GeckoSession? = sessions[tabId]

    fun setCallback(tabId: String, callback: GeckoBrowserCallback) {
        callbacks[tabId] = callback
    }

    fun loadUrl(tabId: String, url: String) {
        val session = sessions[tabId] ?: getOrCreateSession(tabId)
        Log.d(TAG, "Loading URL in tab $tabId: $url")
        session.loadUri(url)
    }

    fun goBack(tabId: String) { sessions[tabId]?.goBack() }
    fun goForward(tabId: String) { sessions[tabId]?.goForward() }
    fun reload(tabId: String) { sessions[tabId]?.reload() }
    fun stopLoading(tabId: String) { sessions[tabId]?.stop() }
    fun canGoBack(tabId: String): Boolean = canGoBackMap[tabId] ?: false
    fun canGoForward(tabId: String): Boolean = canGoForwardMap[tabId] ?: false

    fun setJavaScriptEnabled(tabId: String, enabled: Boolean) {
        Log.d(TAG, "setJavaScriptEnabled($tabId, $enabled)")
        sessions[tabId]?.reload()
    }

    fun setDesktopMode(tabId: String, enabled: Boolean) {
        sessions[tabId]?.let { session ->
            val settings = session.settings
            settings.userAgentOverride = if (enabled) DESKTOP_UA else null
            session.reload()
        }
    }

    fun setCookieMode(mode: CookieMode) {
        Log.d(TAG, "setCookieMode: $mode")
    }

    fun clearCookies() { Log.d(TAG, "clearCookies") }
    fun clearBrowsingData() { Log.d(TAG, "clearBrowsingData") }

    fun evaluateJavaScript(tabId: String, script: String, callback: ((String?) -> Unit)? = null) {
        sessions[tabId]?.loadUri("javascript:$script")
        callback?.invoke(null)
    }

    fun findAll(tabId: String, query: String) { Log.d(TAG, "findAll: $query") }
    fun findNext(tabId: String) { Log.d(TAG, "findNext") }
    fun findPrevious(tabId: String) { Log.d(TAG, "findPrevious") }
    fun clearFind(tabId: String) { Log.d(TAG, "clearFind") }

    fun closeSession(tabId: String) {
        sessions.remove(tabId)?.let { session ->
            callbacks.remove(tabId)
            canGoBackMap.remove(tabId)
            canGoForwardMap.remove(tabId)
            currentUrlMap.remove(tabId)
            currentTitleMap.remove(tabId)
            session.close()
        }
    }

    fun pauseSession(tabId: String) { sessions[tabId]?.setActive(false) }
    fun resumeSession(tabId: String) { sessions[tabId]?.setActive(true) }

    fun pauseAllExcept(activeTabId: String) {
        sessions.forEach { (id, session) ->
            if (id != activeTabId) session.setActive(false)
        }
    }

    fun closeAllSessions() {
        sessions.keys.toList().forEach { closeSession(it) }
    }

    fun getSessionCount(): Int = sessions.size

    private fun createSession(tabId: String): GeckoSession {
        val settings = GeckoSessionSettings.Builder()
            .usePrivateMode(false)
            .build()

        val session = GeckoSession(settings)

        session.navigationDelegate = object : GeckoSession.NavigationDelegate {}
        session.contentDelegate = object : GeckoSession.ContentDelegate {}
        session.progressDelegate = object : GeckoSession.ProgressDelegate {}
        session.promptDelegate = object : GeckoSession.PromptDelegate {}

        session.open(getRuntime())
        Log.d(TAG, "GeckoSession created for tab $tabId")
        return session
    }

    companion object {
        private const val DESKTOP_UA =
            "Mozilla/5.0 (X11; Linux x86_64; rv:131.0) Gecko/20100101 Firefox/131.0"

        @Volatile
        private var instance: GeckoBrowserEngine? = null

        fun getInstance(context: Context): GeckoBrowserEngine {
            return instance ?: synchronized(this) {
                instance ?: GeckoBrowserEngine(context.applicationContext).also {
                    instance = it
                }
            }
        }

        private fun isMediaUrl(url: String): Boolean {
            val lower = url.lowercase()
            return lower.endsWith(".mp4") || lower.endsWith(".mp3") ||
                    lower.endsWith(".webm") || lower.endsWith(".ogg") ||
                    lower.contains("youtube.com/watch")
        }
    }

    enum class CookieMode { ACCEPT_ALL, ACCEPT_FIRST_PARTY, REJECT_THIRD_PARTY, REJECT_ALL }
}
