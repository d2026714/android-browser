package com.example.browser.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(vm: BrowserViewModel, onBack: () -> Unit) {
    val list by vm.bookmarks.collectAsState()
    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("书签") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") } })
        if (list.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("暂无书签", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        else LazyColumn(Modifier.fillMaxSize()) {
            items(list, key = { it.id }) { b ->
                ListItem(headlineContent = { Text(b.title, maxLines = 1) },
                    supportingContent = { Text(b.url, maxLines = 1, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingContent = { IconButton(onClick = { vm.deleteBookmark(b) }) { Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier.clickable { vm.loadUrl(b.url); onBack() })
            }
        }
    }
}
