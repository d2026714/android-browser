package com.example.browser.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.browser.reader.BookshelfScreen
import com.example.browser.reader.ReaderScreen
import com.example.browser.ui.navigation.Screen

@Composable
fun MainScreen(vm: BrowserViewModel = viewModel()) {
    val nav = rememberNavController()
    NavHost(nav, Screen.Browser.route) {
        composable(Screen.Browser.route) {
            BrowserScreen(vm,
                onBookmarks = { nav.navigate(Screen.Bookmarks.route) },
                onHistory = { nav.navigate(Screen.History.route) },
                onSettings = { nav.navigate(Screen.Settings.route) },
                onBookshelf = { nav.navigate(Screen.Bookshelf.route) },
                onReader = { nav.navigate(Screen.Reader.route) })
        }
        composable(Screen.Bookmarks.route) { BookmarksScreen(vm) { nav.popBackStack() } }
        composable(Screen.History.route) { HistoryScreen(vm) { nav.popBackStack() } }
        composable(Screen.Settings.route) { SettingsScreen(vm) { nav.popBackStack() } }
        composable(Screen.Reader.route) {
            val c by vm.readerContent.collectAsState()
            ReaderScreen(c?.title ?: "", c?.chapters ?: emptyList(), c?.text ?: "",
                onBack = { vm.clearReader(); nav.popBackStack() },
                onOpenInBrowser = { vm.clearReader(); nav.popBackStack() })
        }
        composable(Screen.Bookshelf.route) {
            val books by vm.bookshelf.collectAsState()
            BookshelfScreen(books, onBookClick = { vm.loadUrl(it.url); nav.popBackStack() },
                onDelete = { vm.removeBookshelf(it) }, onBack = { nav.popBackStack() })
        }
    }
}
