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
import com.example.browser.util.toFormattedDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(vm: BrowserViewModel, onBack: () -> Unit) {
    val list by vm.history.collectAsState()
    var showClear by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("历史记录") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") } },
            actions = { if (list.isNotEmpty()) IconButton(onClick = { showClear = true }) { Icon(Icons.Default.DeleteSweep, "清空") } })
        if (list.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("暂无记录", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        else LazyColumn(Modifier.fillMaxSize()) {
            items(list, key = { it.id }) { h ->
                ListItem(headlineContent = { Text(h.title, maxLines = 1) },
                    supportingContent = { Column { Text(h.url, maxLines = 1, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(h.visitedAt.toFormattedDate(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline) } },
                    trailingContent = { IconButton(onClick = { vm.deleteHistory(h.id) }) { Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier.clickable { vm.loadUrl(h.url); onBack() })
            }
        }
    }
    if (showClear) AlertDialog(onDismissRequest = { showClear = false }, title = { Text("清空历史") },
        text = { Text("确定清空所有历史记录？") },
        confirmButton = { TextButton(onClick = { vm.clearHistory(); showClear = false }) { Text("清空", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { showClear = false }) { Text("取消") } })
}
