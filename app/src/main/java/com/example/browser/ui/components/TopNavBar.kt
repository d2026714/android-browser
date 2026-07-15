package com.example.browser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun TopNavBar(
    url: String,
    isLoading: Boolean,
    progress: Int,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onStop: () -> Unit,
    onShare: () -> Unit,
    onBookmark: () -> Unit,
    onFind: () -> Unit,
    onMenu: () -> Unit,
) {
    Column {
        Row(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, enabled = canGoBack) {
                Icon(Icons.Default.ArrowBack, "后退")
            }
            IconButton(onClick = onForward, enabled = canGoForward) {
                Icon(Icons.Default.ArrowForward, "前进")
            }
            IconButton(onClick = { if (isLoading) onStop() else onReload() }) {
                Icon(if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
                    if (isLoading) "停止" else "刷新")
            }
            Surface(
                Modifier.weight(1f).padding(horizontal = 8.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 1.dp,
            ) {
                Text(
                    url.removePrefix("https://").removePrefix("http://").take(50),
                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onShare) { Icon(Icons.Default.Share, "分享") }
            IconButton(onClick = onBookmark) { Icon(Icons.Default.BookmarkBorder, "书签") }
            IconButton(onClick = onFind) { Icon(Icons.Default.Search, "查找") }
            IconButton(onClick = onMenu) { Icon(Icons.Default.MoreVert, "菜单") }
        }
        if (isLoading) {
            LinearProgressIndicator(
                progress = progress / 100f,
                Modifier.fillMaxWidth().height(2.dp),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}
