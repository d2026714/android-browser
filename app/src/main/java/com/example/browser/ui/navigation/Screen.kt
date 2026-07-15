package com.example.browser.ui.navigation

sealed class Screen(val route: String) {
    data object Browser : Screen("browser")
    data object Bookmarks : Screen("bookmarks")
    data object History : Screen("history")
    data object Settings : Screen("settings")
    data object Reader : Screen("reader/{url}") {
        fun createRoute(url: String) = "reader/${java.net.URLEncoder.encode(url, "UTF-8")}"
    }
    data object Bookshelf : Screen("bookshelf")
}
