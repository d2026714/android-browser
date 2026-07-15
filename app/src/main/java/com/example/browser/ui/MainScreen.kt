package com.example.browser.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.browser.reader.BookshelfScreen
import com.example.browser.reader.ReaderScreen
import com.example.browser.ui.navigation.Screen

@Composable
fun MainScreen(viewModel: BrowserViewModel = viewModel()) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Browser.route,
    ) {
        composable(Screen.Browser.route) {
            BrowserScreen(
                viewModel = viewModel,
                onNavigateToBookmarks = { navController.navigate(Screen.Bookmarks.route) },
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToBookshelf = { navController.navigate(Screen.Bookshelf.route) },
                onNavigateToReader = { navController.navigate("reader") },
            )
        }
        composable(Screen.Bookmarks.route) {
            BookmarksScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Screen.History.route) {
            HistoryScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }
        composable("reader") {
            val content by viewModel.readerContent.collectAsState()
            val tabs by viewModel.tabs.collectAsState()
            val activeTabIndex by viewModel.activeTabIndex.collectAsState()
            val activeUrl = tabs.getOrNull(activeTabIndex)?.url ?: ""

            ReaderScreen(
                title = content?.title ?: "",
                chapters = content?.chapters ?: emptyList(),
                fullText = content?.text ?: "",
                onBack = {
                    viewModel.clearReaderContent()
                    navController.popBackStack()
                },
                onOpenInBrowser = { url ->
                    viewModel.clearReaderContent()
                    navController.popBackStack()
                    if (url.isNotEmpty()) viewModel.loadUrl(url)
                },
                webViewUrl = activeUrl,
            )
        }
        composable(Screen.Bookshelf.route) {
            val bookshelf by viewModel.bookshelf.collectAsState()

            BookshelfScreen(
                books = bookshelf,
                onBookClick = { book ->
                    viewModel.loadUrl(book.url)
                    navController.popBackStack()
                },
                onDeleteBook = { viewModel.removeFromBookshelf(it) },
                onBack = { navController.popBackStack() },
                onAddByUrl = {
                    viewModel.goHome()
                    navController.popBackStack()
                },
            )
        }
    }
}
