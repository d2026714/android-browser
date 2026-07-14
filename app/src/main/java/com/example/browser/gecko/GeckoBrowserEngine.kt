package com.example.browser.gecko

import android.content.Context
import android.util.Log
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.StorageController

private const val TAG = "GeckoBrowserEngine"

/**
 * Callbacks for browser events from a GeckoSession.
 */
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

/**
 * GeckoBrowserEngine manages the GeckoRuntime singleton and GeckoSession instances.
 * Each tab maps to one GeckoSession. The GeckoRuntime is shared across all sessions.
 */
class GeckoBrowserEngine private constructor(private val context: Context) {

    private var runtime: GeckoRuntime? = null

    // Map of tabId -> GeckoSession
    private val sessions = mutableMapOf<String, GeckoSession>()

    // Map of tabId -> callback
    private val callbacks = mutableMapOf<String, GeckoBrowserCallback>()

    // Track current URL per session (updated by onLocationChange)
    private val currentUrlMap = mutableMapOf<String, String>()
    // Track current title per session (updated by onTitleChange)
    private val currentTitleMap = mutableMapOf<String, String>()

    // Track navigation state per session
    private val canGoBackMap = mutableMapOf<String, Boolean>()
    private val canGoForwardMap = mutableMapOf<String, Boolean>()

    /**
     * Initialize the GeckoRuntime. Must be called once, typically in Application.onCreate().
     */
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

    /**
     * Get the GeckoRuntime singleton.
     */
    fun getRuntime(): GeckoRuntime {
        return runtime ?: throw IllegalStateException("GeckoRuntime not initialized. Call initialize() first.")
    }

    /**
     * Create or get a GeckoSession for a tab.
     */
    fun getOrCreateSession(tabId: String): GeckoSession {
        return sessions.getOrPut(tabId) {
            Log.d(TAG, "Creating new GeckoSession for tab $tabId")
            createSession(tabId)
        }
    }

    /**
     * Get an existing session for a tab.
     */
    fun getSession(tabId: String): GeckoSession? = sessions[tabId]

    /**
     * Set the callback for a tab's session.
     */
    fun setCallback(tabId: String, callback: GeckoBrowserCallback) {
        callbacks[tabId] = callback
    }

    /**
     * Load a URL in a tab's session.
     */
    fun loadUrl(tabId: String, url: String) {
        val session = sessions[tabId]
        if (session != null) {
            Log.d(TAG, "Loading URL in tab $tabId: $url")
            session.loadUri(url)
        } else {
            Log.w(TAG, "No session found for tab $tabId, creating one")
            val newSession = getOrCreateSession(tabId)
            newSession.loadUri(url)
        }
    }

    /**
     * Go back in a tab's session.
     */
    fun goBack(tabId: String) {
        sessions[tabId]?.let { session ->
            session.goBack()
        }
    }

    /**
     * Go forward in a tab's session.
     */
    fun goForward(tabId: String) {
        sessions[tabId]?.let { session ->
            session.goForward()
        }
    }

    /**
     * Reload a tab's session.
     */
    fun reload(tabId: String) {
        sessions[tabId]?.let { session ->
            session.reload()
        }
    }

    /**
     * Stop loading in a tab's session.
     */
    fun stopLoading(tabId: String) {
        sessions[tabId]?.let { session ->
            session.stop()
        }
    }

    /**
     * Check if a tab can go back.
     */
    fun canGoBack(tabId: String): Boolean = canGoBackMap[tabId] ?: false

    /**
     * Check if a tab can go forward.
     */
    fun canGoForward(tabId: String): Boolean = canGoForwardMap[tabId] ?: false

    /**
     * Set JavaScript enabled/disabled for a session.
     */
    fun setJavaScriptEnabled(tabId: String, enabled: Boolean) {
        sessions[tabId]?.let { session ->
            val settings = session.settings
            settings.javaScriptEnabled = enabled
        }
    }

    /**
     * Set desktop mode (user agent override) for a session.
     */
    fun setDesktopMode(tabId: String, enabled: Boolean) {
        sessions[tabId]?.let { session ->
            val settings = session.settings
            settings.userAgentOverride = if (enabled) DESKTOP_UA else null
            session.reload()
        }
    }

    /**
     * Set cookie behavior.
     */
    fun setCookieMode(mode: CookieMode) {
        val rt = runtime ?: return
        val controller = rt.storageController
        when (mode) {
            CookieMode.ACCEPT_ALL -> {
                controller.setCookieBehavior(
                    StorageController.COOKIE_BEHAVIOR_ACCEPT
                )
            }
            CookieMode.ACCEPT_FIRST_PARTY -> {
                controller.setCookieBehavior(
                    StorageController.COOKIE_BEHAVIOR_ACCEPT_FIRST_PARTY
                )
            }
            CookieMode.REJECT_ALL -> {
                controller.setCookieBehavior(
                    StorageController.COOKIE_BEHAVIOR_REJECT
                )
            }
            CookieMode.REJECT_THIRD_PARTY -> {
                controller.setCookieBehavior(
                    StorageController.COOKIE_BEHAVIOR_REJECT_THIRD_PARTY
                )
            }
        }
    }

    /**
     * Clear all cookies.
     */
    fun clearCookies() {
        runtime?.storageController?.clearData(StorageController.CLEAR_COOKIES)
    }

    /**
     * Clear all browsing data (cookies, cache, etc.).
     */
    fun clearBrowsingData() {
        runtime?.storageController?.clearData(StorageController.CLEAR_ALL)
    }

    /**
     * Execute JavaScript in a tab's session.
     */
    fun evaluateJavaScript(tabId: String, script: String, callback: ((String?) -> Unit)? = null) {
        sessions[tabId]?.let { session ->
            session.evaluateJS(script).then { result ->
                callback?.invoke(result?.toString())
                GeckoResult.fromValue(result)
            }?.accept { /* completed */ }
        }
    }

    /**
     * Find text in page.
     */
    fun findAll(tabId: String, query: String) {
        sessions[tabId]?.let { session ->
            session.findAll(query)
        }
    }

    /**
     * Find next match.
     */
    fun findNext(tabId: String) {
        sessions[tabId]?.let { session ->
            session.findNext(GeckoSession.FIND_FIND_NEXT)
        }
    }

    /**
     * Find previous match.
     */
    fun findPrevious(tabId: String) {
        sessions[tabId]?.let { session ->
            session.findNext(GeckoSession.FIND_FIND_PREVIOUS)
        }
    }

    /**
     * Clear find matches.
     */
    fun clearFind(tabId: String) {
        sessions[tabId]?.let { session ->
            session.clearFind()
        }
    }

    /**
     * Close and release a session for a tab.
     */
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

    /**
     * Pause a session (e.g., when switching away from a tab).
     */
    fun pauseSession(tabId: String) {
        sessions[tabId]?.let { session ->
            session.setActive(false)
            Log.v(TAG, "Paused GeckoSession for tab $tabId")
        }
    }

    /**
     * Resume a session (e.g., when switching to a tab).
     */
    fun resumeSession(tabId: String) {
        sessions[tabId]?.let { session ->
            session.setActive(true)
            Log.v(TAG, "Resumed GeckoSession for tab $tabId")
        }
    }

    /**
     * Pause all sessions except the active one.
     */
    fun pauseAllExcept(activeTabId: String) {
        sessions.forEach { (id, session) ->
            if (id != activeTabId) {
                session.setActive(false)
            }
        }
    }

    /**
     * Close all sessions.
     */
    fun closeAllSessions() {
        val ids = sessions.keys.toList()
        ids.forEach { closeSession(it) }
        Log.d(TAG, "Closed all GeckoSessions (${ids.size})")
    }

    /**
     * Get the number of active sessions.
     */
    fun getSessionCount(): Int = sessions.size

    // --- Private helpers ---

    private fun createSession(tabId: String): GeckoSession {
        val settings = GeckoSessionSettings.Builder()
            .usePrivateMode(false)
            .suspendMediaWhenInactive(true)
            .build()

        val session = GeckoSession(settings)

        // Set up content blocking (ad blocking / tracking protection)
        session.contentBlockingDelegate = createContentBlockingDelegate()

        // Set up navigation delegate
        session.navigationDelegate = createNavigationDelegate(tabId)

        // Set up content delegate
        session.contentDelegate = createContentDelegate(tabId)

        // Set up progress delegate
        session.progressDelegate = createProgressDelegate(tabId)

        // Set up prompt delegate for permissions and alerts
        session.promptDelegate = createPromptDelegate()

        // Open session with runtime
        val rt = getRuntime()
        session.open(rt)

        Log.d(TAG, "GeckoSession created and opened for tab $tabId")
        return session
    }

    private fun createNavigationDelegate(tabId: String): GeckoSession.NavigationDelegate {
        return object : GeckoSession.NavigationDelegate {
            override fun onLocationChange(session: GeckoSession, url: String?) {
                url?.let {
                    Log.v(TAG, "Location changed for tab $tabId: $it")
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
                // Handle media URLs
                if (isMediaUrl(url)) {
                    callbacks[tabId]?.onMediaUrlDetected(url, "")
                }
                // Allow all navigations
                return GeckoResult.fromValue(AllowOrDeny.ALLOW)
            }

            override fun onNewSession(
                session: GeckoSession,
                uri: String
            ): GeckoResult<GeckoSession>? {
                // Open links in new tabs - return null to let the app handle it
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

            override fun onFocusRequest(session: GeckoSession) {
                // Handle focus request
            }

            override fun onCloseRequest(session: GeckoSession) {
                // Handle window.close()
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
                Log.d(TAG, "External response: ${response.uri} (${response.contentType})")
            }
        }
    }

    private fun createProgressDelegate(tabId: String): GeckoSession.ProgressDelegate {
        return object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                Log.v(TAG, "Page start for tab $tabId: $url")
                callbacks[tabId]?.onPageStarted(url)
            }

            override fun onPageStop(session: GeckoSession, success: Boolean) {
                Log.v(TAG, "Page stop for tab $tabId: success=$success")
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

    private fun createContentBlockingDelegate(): ContentBlocking.Delegate {
        return object : ContentBlocking.Delegate {
            override fun onContentBlocked(
                session: GeckoSession,
                event: ContentBlocking.BlockEvent
            ) {
                Log.v(TAG, "Content blocked: ${event.categories}")
            }
        }
    }

    private fun createPromptDelegate(): GeckoSession.PromptDelegate {
        return object : GeckoSession.PromptDelegate {
            // Default implementation - auto-dismiss prompts
            // In a real app, you'd show custom UI for these
        }
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

    /**
     * Cookie behavior modes.
     */
    enum class CookieMode {
        ACCEPT_ALL,
        ACCEPT_FIRST_PARTY,
        REJECT_THIRD_PARTY,
        REJECT_ALL
    }
}
