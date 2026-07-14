package com.example.browser.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.browser.ui.viewmodel.BrowserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LongPressMenuSheet(viewModel: BrowserViewModel) {
    val longPressUrl by viewModel.longPressUrl.collectAsState()
    val url = longPressUrl ?: return

    ModalBottomSheet(onDismissRequest = { viewModel.dismissLongPressMenu() }) {
        ListItem(
            headlineContent = { Text(url, maxLines = 2) },
        )
        Divider()
        ListItem(
            headlineContent = { Text("Open in New Tab") },
            modifier = Modifier.clickable {
                viewModel.openInNewTab(url)
                viewModel.dismissLongPressMenu()
            }
        )
        ListItem(
            headlineContent = { Text("Open in Incognito") },
            modifier = Modifier.clickable {
                viewModel.openInIncognito(url)
                viewModel.dismissLongPressMenu()
            }
        )
        ListItem(
            headlineContent = { Text("Copy Link") },
            modifier = Modifier.clickable {
                viewModel.copyLink(url)
                viewModel.dismissLongPressMenu()
            }
        )
        ListItem(
            headlineContent = { Text("Share Link") },
            modifier = Modifier.clickable {
                viewModel.shareCurrentPage()
                viewModel.dismissLongPressMenu()
            }
        )
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextSelectionMenuSheet(viewModel: BrowserViewModel) {
    val selectedText by viewModel.selectedText.collectAsState()
    val text = selectedText ?: return

    ModalBottomSheet(onDismissRequest = { viewModel.dismissTextSelection() }) {
        ListItem(
            headlineContent = {
                Text(
                    text = if (text.length > 200) text.take(200) + "..." else text,
                    maxLines = 4
                )
            },
        )
        Divider()
        ListItem(
            headlineContent = { Text("Translate Selected Text") },
            leadingContent = { Icon(Icons.Default.Translate, null) },
            modifier = Modifier.clickable {
                viewModel.openTranslate(text)
                viewModel.dismissTextSelection()
            }
        )
        ListItem(
            headlineContent = { Text("Copy") },
            leadingContent = { Icon(Icons.Default.ContentCopy, null) },
            modifier = Modifier.clickable {
                viewModel.copyLink(text)
                viewModel.dismissTextSelection()
            }
        )
        ListItem(
            headlineContent = { Text("Share") },
            leadingContent = { Icon(Icons.Default.Share, null) },
            modifier = Modifier.clickable {
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT, text)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                viewModel.dismissTextSelection()
            }
        )
        Spacer(modifier = Modifier.height(32.dp))
    }
}
