package com.example.browser.gecko

import android.content.Context
import android.util.Log
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.ContentBlocking

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
        val session = sessions[tabId]
        if (session != null) {
            Log.d(TAG, "Loading URL in tab $tabId: $url")
            session.loadUri(url)
        } else {
            val newSession = getOrCreateSession(tabId)
            newSession.loadUri(url)
        }
    }

    fun goBack(tabId: String) { sessions[tabId]?.goBack() }
    fun goForward(tabId: String) { sessions[tabId]?.goForward() }
    fun reload(tabId: String) { sessions[tabId]?.reload() }
    fun stopLoading(tabId: String) { sessions[tabId]?.stop() }
    fun canGoBack(tabId: String): Boolean = canGoBackMap[tabId] ?: false
    fun canGoForward(tabId: String): Boolean = canGoForwardMap[tabId] ?: false

    fun setJavaScriptEnabled(tabId: String, enabled: Boolean) {
        Log.d(TAG, "setJavaScriptEnabled($tabId, $enabled) — reloading session")
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
        // Cookie control via ContentBlocking settings on runtime
        Log.d(TAG, "setCookieMode: $mode (GeckoView 131 API — logged only)")
    }

    fun clearCookies() {
        Log.d(TAG, "clearCookies called")
    }

    fun clearBrowsingData() {
        Log.d(TAG, "clearBrowsingData called")
    }

    fun evaluateJavaScript(tabId: String, script: String, callback: ((String?) -> Unit)? = null) {
        sessions[tabId]?.let { session ->
            session.loadUri("javascript:$script")
            callback?.invoke(null)
        }
    }

    fun findAll(tabId: String, query: String) {
        Log.d(TAG, "findAll($tabId, $query)")
    }

    fun findNext(tabId: String) {
        Log.d(TAG, "findNext($tabId)")
    }

    fun findPrevious(tabId: String) {
        Log.d(TAG, "findPrevious($tabId)")
    }

    fun clearFind(tabId: String) {
        Log.d(TAG, "clearFind($tabId)")
    }

    fun closeSession(tabId: String) {
        sessions.remove(tabId)?.let { session ->
            Log.d(TAG, "Closing GeckoSession for tab $tabId")
            callbacks.remove(tabId)
            canGoBackMap.remove(tabId)
            canGoForwardMap.remove(tabId)
            currentUrlMap.remove(tabId)
            currentTitleMap.remove(tabId)
            session.close()
        }
    }

    fun pauseSession(tabId: String) {
        sessions[tabId]?.setActive(false)
    }

    fun resumeSession(tabId: String) {
        sessions[tabId]?.setActive(true)
    }

    fun pauseAllExcept(activeTabId: String) {
        sessions.forEach { (id, session) ->
            if (id != activeTabId) session.setActive(false)
        }
    }

    fun closeAllSessions() {
        val ids = sessions.keys.toList()
        ids.forEach { closeSession(it) }
        Log.d(TAG, "Closed all GeckoSessions (${ids.size})")
    }

    fun getSessionCount(): Int = sessions.size

    private fun createSession(tabId: String): GeckoSession {
        val settings = GeckoSessionSettings.Builder()
            .usePrivateMode(false)
            .build()

        val session = GeckoSession(settings)

        session.navigationDelegate = createNavigationDelegate(tabId)
        session.contentDelegate = createContentDelegate(tabId)
        session.progressDelegate = createProgressDelegate(tabId)
        session.promptDelegate = createPromptDelegate()

        val rt = getRuntime()
        session.open(rt)

        Log.d(TAG, "GeckoSession created and opened for tab $tabId")
        return session
    }

    private fun createNavigationDelegate(tabId: String): GeckoSession.NavigationDelegate {
        return object : GeckoSession.NavigationDelegate {
            override fun onLocationChange(session: GeckoSession, url: String?) {
                url?.let {
                    currentUrlMap[tabId] = it
                    callbacks[tabId]?.onUrlChanged(it)
                }
            }

            override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
                canGoBackMap[tabId] = canGoBack
                callbacks[tabId]?.onCanGoBackChanged(canGoBack)
            }

            override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
                canGoForwardMap[tabId] = canGoForward
                callbacks[tabId]?.onCanGoForwardChanged(canGoForward)
            }

            override fun onLoadRequest(
                session: GeckoSession,
                request: GeckoSession.NavigationDelegate.LoadRequest
            ): GeckoResult<AllowOrDeny>? {
                val url = request.uri
                if (isMediaUrl(url)) {
                    callbacks[tabId]?.onMediaUrlDetected(url, "")
                }
                return GeckoResult.fromValue(AllowOrDeny.ALLOW)
            }

            override fun onNewSession(
                session: GeckoSession,
                uri: String
            ): GeckoResult<GeckoSession>? {
                Log.d(TAG, "New session requested for: $uri")
                return null
            }
        }
    }

    private fun createContentDelegate(tabId: String): GeckoSession.ContentDelegate {
        return object : GeckoSession.ContentDelegate {
            override fun onTitleChange(session: GeckoSession, title: String?) {
                title?.let {
                    currentTitleMap[tabId] = it
                    callbacks[tabId]?.onTitleChanged(it)
                }
            }

            override fun onFocusRequest(session: GeckoSession) {}

            override fun onCloseRequest(session: GeckoSession) {
                Log.d(TAG, "Close request for tab $tabId")
            }

            override fun onFullScreen(session: GeckoSession, fullScreen: Boolean) {
                Log.d(TAG, "Fullscreen: $fullScreen for tab $tabId")
            }

            override fun onCrash(session: GeckoSession) {
                Log.e(TAG, "GeckoSession crashed for tab $tabId")
                callbacks[tabId]?.onPageError(-1, "Page crashed", null)
            }

            override fun onKill(session: GeckoSession) {
                Log.e(TAG, "GeckoSession killed for tab $tabId")
            }

            override fun onExternalResponse(
                session: GeckoSession,
                response: WebResponseInfo
            ) {
                Log.d(TAG, "External response: ${response.uri}")
            }
        }
    }

    private fun createProgressDelegate(tabId: String): GeckoSession.ProgressDelegate {
        return object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                callbacks[tabId]?.onPageStarted(url)
            }

            override fun onPageStop(session: GeckoSession, success: Boolean) {
                if (success) {
                    val url = currentUrlMap[tabId] ?: "about:blank"
                    val title = currentTitleMap[tabId] ?: ""
                    callbacks[tabId]?.onPageFinished(url, title)
                }
            }

            override fun onProgressChange(session: GeckoSession, progress: Int) {
                callbacks[tabId]?.onProgressChanged(progress)
            }

            override fun onSecurityChange(
                session: GeckoSession,
                securityInfo: SecurityInformation
            ) {
                val isSecure = securityInfo.securityMode ==
                    SecurityInformation.SECURITY_MODE_IDENTIFIED
                callbacks[tabId]?.onSecurityChange(isSecure)
            }
        }
    }

    private fun createPromptDelegate(): GeckoSession.PromptDelegate {
        return object : GeckoSession.PromptDelegate {}
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
                    lower.endsWith(".wav") || lower.endsWith(".m3u8") ||
                    lower.contains("youtube.com/watch") ||
                    lower.contains("youtu.be/") ||
                    lower.contains("vimeo.com/")
        }
    }

    enum class CookieMode {
        ACCEPT_ALL,
        ACCEPT_FIRST_PARTY,
        REJECT_THIRD_PARTY,
        REJECT_ALL
    }
}
