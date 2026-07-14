package com.example.browser.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
