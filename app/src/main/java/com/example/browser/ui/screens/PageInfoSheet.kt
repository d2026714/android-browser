package com.example.browser.ui.screens

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
            Text("Page Info", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))

            ListItem(
                headlineContent = { Text("Title") },
                supportingContent = { Text(currentTitle) },
                leadingContent = { Icon(Icons.Default.Title, null) }
            )
            ListItem(
                headlineContent = { Text("URL") },
                supportingContent = { Text(currentUrl, maxLines = 3) },
                leadingContent = { Icon(Icons.Default.Link, null) }
            )
            ListItem(
                headlineContent = { Text("Protocol") },
                supportingContent = {
                    Text(if (currentUrl.startsWith("https://")) "HTTPS (Secure)" else if (currentUrl.startsWith("http://")) "HTTP (Not Secure)" else "Other")
                },
                leadingContent = {
                    Icon(
                        if (currentUrl.startsWith("https://")) Icons.Default.Lock else Icons.Default.LockOpen,
                        null, tint = if (currentUrl.startsWith("https://")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            )
            ListItem(
                headlineContent = { Text("View Mode") },
                supportingContent = { Text(if (isDesktopMode) "Desktop" else "Mobile") },
                leadingContent = { Icon(if (isDesktopMode) Icons.Default.Computer else Icons.Default.PhoneAndroid, null) }
            )
            ListItem(
                headlineContent = { Text("Ad Blocker") },
                supportingContent = { Text(if (isAdBlockEnabled) "Active" else "Disabled") },
                leadingContent = { Icon(Icons.Default.Shield, null) }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                FilledTonalButton(onClick = { viewModel.copyLink(currentUrl) }) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy URL")
                }
                FilledTonalButton(onClick = { viewModel.shareCurrentPage() }) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
