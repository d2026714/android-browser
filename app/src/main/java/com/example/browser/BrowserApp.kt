package com.example.browser

import android.app.Application
import com.example.browser.gecko.GeckoBrowserEngine

class BrowserApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize GeckoView engine early
        try {
            GeckoBrowserEngine.getInstance(this).initialize()
        } catch (e: Throwable) {
            // GeckoView initialization failed; app can fall back to WebView
            android.util.Log.e("BrowserApp", "GeckoView init failed", e)
        }
    }

    companion object {
        lateinit var instance: BrowserApp
            private set
    }
}
