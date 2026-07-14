package com.example.browser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.browser.R
import com.example.browser.ui.viewmodel.BrowserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageInfoSheet(
    viewModel: BrowserViewModel,
    onDismiss: () -> Unit
) {
    val currentUrl by viewModel.currentUrl.collectAsState()
    val currentTitle by viewModel.currentTitle.collectAsState()
    val isDesktopMode by viewModel.isDesktopMode.collectAsState()
    val isAdBlockEnabled by viewModel.isAdBlockEnabled.collectAsState()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text(stringResource(R.string.page_info), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))

            ListItem(
                headlineContent = { Text(stringResource(R.string.title_label)) },
                supportingContent = { Text(currentTitle) },
                leadingContent = { Icon(Icons.Default.Title, null) }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.url_label_simple)) },
                supportingContent = { Text(currentUrl, maxLines = 3) },
                leadingContent = { Icon(Icons.Default.Link, null) }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.protocol)) },
                supportingContent = {
                    Text(if (currentUrl.startsWith("https://")) stringResource(R.string.https_secure) else if (currentUrl.startsWith("http://")) stringResource(R.string.http_not_secure) else stringResource(R.string.other))
                },
                leadingContent = {
                    Icon(
                        if (currentUrl.startsWith("https://")) Icons.Default.Lock else Icons.Default.LockOpen,
                        null, tint = if (currentUrl.startsWith("https://")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.view_mode)) },
                supportingContent = { Text(if (isDesktopMode) stringResource(R.string.desktop) else stringResource(R.string.mobile)) },
                leadingContent = { Icon(if (isDesktopMode) Icons.Default.Computer else Icons.Default.PhoneAndroid, null) }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.ad_blocker)) },
                supportingContent = { Text(if (isAdBlockEnabled) stringResource(R.string.active) else stringResource(R.string.disabled)) },
                leadingContent = { Icon(Icons.Default.Shield, null) }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                FilledTonalButton(onClick = { viewModel.copyLink(currentUrl) }) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.copy_url))
                }
                FilledTonalButton(onClick = { viewModel.shareCurrentPage() }) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.share))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
