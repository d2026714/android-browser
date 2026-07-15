package com.example.browser.ui.navigation

sealed class Screen(val route: String) {
    data object Browser : Screen("browser")
    data object Bookmarks : Screen("bookmarks")
    data object History : Screen("history")
    data object Settings : Screen("settings")
}
