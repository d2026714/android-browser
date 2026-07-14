package com.example.browser

import android.app.Application

class BrowserApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: BrowserApp
            private set
    }
}
