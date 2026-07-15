package com.example.browser

import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.browser.ui.screens.MainScreen
import com.example.browser.ui.theme.BrowserTheme
import com.example.browser.ui.viewmodel.BrowserViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: BrowserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel = ViewModelProvider(this)[BrowserViewModel::class.java]

        // Handle intent URI (e.g., user opens a link from another app)
        intent?.data?.let { uri ->
            viewModel.handleIntentUri(uri)
        }

        // Observe full-screen mode changes
        lifecycleScope.launch {
            viewModel.isFullScreen.collectLatest { isFullScreen ->
                setFullScreenMode(isFullScreen)
            }
        }

        setContent {
            BrowserTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        intent.data?.let { uri ->
            viewModel.handleIntentUri(uri)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Volume key navigation: scroll page up/down
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                val webView = viewModel.getActiveWebView()
                if (webView != null) {
                    webView.scrollBy(0, -webView.height / 2)
                    return true
                }
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                val webView = viewModel.getActiveWebView()
                if (webView != null) {
                    webView.scrollBy(0, webView.height / 2)
                    return true
                }
            }
        }

        if (viewModel.handleKeyShortcut(keyCode)) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun setFullScreenMode(enabled: Boolean) {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (enabled) {
            // Hide status bar and navigation bar
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            // Show status bar and navigation bar
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
