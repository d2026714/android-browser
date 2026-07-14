package com.example.browser.ui.components

import android.content.Context
import android.webkit.WebView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

data class LinkContextMenu(
    val url: String,
    val title: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkContextMenuSheet(
    link: LinkContextMenu,
    onDismiss: () -> Unit,
    onOpenInNewTab: (String) -> Unit,
    onOpenInIncognito: (String) -> Unit,
    onCopyLink: (String) -> Unit,
    onShareLink: (String) -> Unit
) {
    val context = LocalContext.current

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = link.title.ifBlank { link.url },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            ListItem(
                headlineContent = { Text("Open in new tab") },
                leadingContent = { Icon(Icons.Default.Tab, null) },
                modifier = Modifier.let { it }
            )

            ListItem(
                headlineContent = { Text("Open in incognito") },
                leadingContent = { Icon(Icons.Default.VisibilityOff, null) },
                modifier = Modifier.let { it }
            )

            ListItem(
                headlineContent = { Text("Copy link") },
                leadingContent = { Icon(Icons.Default.ContentCopy, null) },
                modifier = Modifier.let { it }
            )

            ListItem(
                headlineContent = { Text("Share link") },
                leadingContent = { Icon(Icons.Default.Share, null) },
                modifier = Modifier.let { it }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageActionsSheet(
    url: String,
    title: String,
    onDismiss: () -> Unit,
    onViewSource: () -> Unit,
    onFindInPage: () -> Unit,
    onReadingMode: () -> Unit,
    onDesktopMode: () -> Unit,
    onShare: () -> Unit,
    onScreenshot: () -> Unit,
    onSaveOffline: () -> Unit,
    onClearData: () -> Unit,
    isDesktopMode: Boolean
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Page Actions",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            ListItem(
                headlineContent = { Text("View Page Source") },
                supportingContent = { Text("See the HTML code") },
                leadingContent = { Icon(Icons.Default.Code, null) }
            )

            ListItem(
                headlineContent = { Text("Find in Page") },
                supportingContent = { Text("Search text on this page") },
                leadingContent = { Icon(Icons.Default.Search, null) }
            )

            ListItem(
                headlineContent = { Text("Reading Mode") },
                supportingContent = { Text("Clean, distraction-free reading") },
                leadingContent = { Icon(Icons.Default.MenuBook, null) }
            )

            ListItem(
                headlineContent = { Text(if (isDesktopMode) "Mobile Site" else "Desktop Site") },
                supportingContent = { Text("Switch between mobile and desktop") },
                leadingContent = {
                    Icon(
                        if (isDesktopMode) Icons.Default.PhoneAndroid else Icons.Default.Computer,
                        null
                    )
                }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            ListItem(
                headlineContent = { Text("Screenshot") },
                supportingContent = { Text("Capture the current page") },
                leadingContent = { Icon(Icons.Default.CameraAlt, null) }
            )

            ListItem(
                headlineContent = { Text("Save for Offline") },
                supportingContent = { Text("Download page for offline reading") },
                leadingContent = { Icon(Icons.Default.SaveAlt, null) }
            )

            ListItem(
                headlineContent = { Text("Share Page") },
                supportingContent = { Text("Share via other apps") },
                leadingContent = { Icon(Icons.Default.Share, null) }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            ListItem(
                headlineContent = { Text("Clear Cookies & Data") },
                supportingContent = { Text("Remove site data for this page") },
                leadingContent = {
                    Icon(Icons.Default.DeleteSweep, null, tint = MaterialTheme.colorScheme.error)
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
