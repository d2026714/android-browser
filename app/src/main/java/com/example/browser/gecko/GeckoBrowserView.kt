package com.example.browser.gecko

import android.annotation.SuppressLint
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.browser.ui.viewmodel.BrowserViewModel
import org.mozilla.geckoview.GeckoView

private const val TAG = "GeckoBrowserView"

/**
 * GeckoBrowserView: Compose wrapper for GeckoView.
 * Drop-in replacement for BrowserWebView, using GeckoView as the rendering engine.
 *
 * Each tab has its own GeckoSession managed by GeckoBrowserEngine.
 * This composable handles:
 * - Swapping GeckoViews when tabs change
 * - Connecting GeckoSession delegates to BrowserViewModel callbacks
 * - Handling URL loads from the navigation bar
 */
@Composable
fun GeckoBrowserView(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeTabIndex by viewModel.activeTabIndex.collectAsState()
    val tabs by viewModel.tabs.collectAsState()
    val progress by viewModel.pageProgress.collectAsState()

    val engine = remember { GeckoBrowserEngine.getInstance(context) }
    var container by remember { mutableStateOf<FrameLayout?>(null) }
    var previousTabIndex by remember { mutableIntStateOf(-1) }

    // Swap GeckoView when tab changes
    LaunchedEffect(activeTabIndex, tabs.size) {
        val tab = tabs.getOrNull(activeTabIndex) ?: return@LaunchedEffect
        val frame = container ?: return@LaunchedEffect

        // Remove old GeckoView from container
        if (frame.childCount > 0) {
            val oldView = frame.getChildAt(0)
            frame.removeView(oldView)
            // Pause the old session
            val oldTab = tabs.getOrNull(previousTabIndex)
            if (oldTab != null) {
                engine.pauseSession(oldTab.id)
            }
        }

        // Get or create session for this tab
        val session = engine.getOrCreateSession(tab.id)

        // Set up callback to bridge GeckoView events -> ViewModel
        engine.setCallback(tab.id, object : GeckoBrowserCallback {
            override fun onPageStarted(url: String) {
                viewModel.onLoadingChanged(true)
                viewModel.onUrlChanged(url)
            }

            override fun onPageFinished(url: String, title: String) {
                viewModel.onPageFinished(url, title.ifBlank { tab.title })
            }

            override fun onProgressChanged(progress: Int) {
                viewModel.onProgressChanged(progress)
            }

            override fun onTitleChanged(title: String) {
                viewModel.onTitleChanged(title)
            }

            override fun onUrlChanged(url: String) {
                viewModel.onUrlChanged(url)
            }

            override fun onCanGoBackChanged(canGoBack: Boolean) {
                viewModel.onNavigationStateChanged(canGoBack, engine.canGoForward(tab.id))
            }

            override fun onCanGoForwardChanged(canGoForward: Boolean) {
                viewModel.onNavigationStateChanged(engine.canGoBack(tab.id), canGoForward)
            }

            override fun onPageError(errorCode: Int, description: String, url: String?) {
                viewModel.onPageError(errorCode, description, url)
            }

            override fun onSecurityChange(isSecure: Boolean) {
                // Could be used for SSL indicator in URL bar
            }

            override fun onMediaUrlDetected(url: String, title: String) {
                viewModel.onMediaUrlDetected(url, title)
            }
        })

        // Get or create GeckoView for this tab
        val geckoView = getOrCreateGeckoView(context, tab.id, session, frame)

        // Attach to container
        if (geckoView.parent != null) {
            (geckoView.parent as? ViewGroup)?.removeView(geckoView)
        }
        frame.addView(geckoView)

        // Resume the session
        engine.resumeSession(tab.id)

        // Sync ViewModel state from tab
        viewModel.syncFromTab(tab)

        // Set the active session reference in ViewModel
        viewModel.setActiveGeckoSession(tab.id, engine)

        // Load URL if switching tabs or fresh tab
        if (previousTabIndex != activeTabIndex) {
            if (tab.url.isNotBlank() && tab.url != "about:blank") {
                engine.loadUrl(tab.id, tab.url)
            }
            previousTabIndex = activeTabIndex
        }
    }

    // Handle URL changes from navigation bar (user types new URL)
    val currentUrl by viewModel.currentUrl.collectAsState()
    LaunchedEffect(currentUrl) {
        val tab = tabs.getOrNull(activeTabIndex) ?: return@LaunchedEffect
        if (currentUrl.isNotBlank() && currentUrl != "about:blank") {
            engine.loadUrl(tab.id, currentUrl)
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                FrameLayout(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    container = this
                }
            }
        )

        // Page loading progress bar
        if (progress in 1..99) {
            LinearProgressIndicator(
                progress = progress / 100f,
                modifier = Modifier.fillMaxWidth(),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

/**
 * Cache of GeckoView instances per tab, so we can reuse them across recompositions.
 */
private val geckoViewCache = mutableMapOf<String, GeckoView>()

@SuppressLint("SetJavaScriptEnabled")
private fun getOrCreateGeckoView(
    context: android.content.Context,
    tabId: String,
    session: org.mozilla.geckoview.GeckoSession,
    parent: FrameLayout
): GeckoView {
    return geckoViewCache.getOrPut(tabId) {
        Log.d(TAG, "Creating new GeckoView for tab $tabId")
        GeckoView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setSession(session)
        }
    }
}

/**
 * Clean up GeckoView cache for a closed tab.
 */
fun removeGeckoViewFromCache(tabId: String) {
    geckoViewCache.remove(tabId)
}

/**
 * Clear the entire GeckoView cache.
 */
fun clearGeckoViewCache() {
    geckoViewCache.clear()
}
