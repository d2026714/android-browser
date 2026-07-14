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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.browser.R

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
                headlineContent = { Text(stringResource(R.string.open_in_new_tab_lower)) },
                leadingContent = { Icon(Icons.Default.Tab, null) },
                modifier = Modifier.let { it }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.open_in_incognito_lower)) },
                leadingContent = { Icon(Icons.Default.VisibilityOff, null) },
                modifier = Modifier.let { it }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.copy_link_lower)) },
                leadingContent = { Icon(Icons.Default.ContentCopy, null) },
                modifier = Modifier.let { it }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.share_link_lower)) },
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
                text = stringResource(R.string.page_actions),
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
                headlineContent = { Text(stringResource(R.string.view_page_source)) },
                supportingContent = { Text(stringResource(R.string.see_html_code)) },
                leadingContent = { Icon(Icons.Default.Code, null) }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.find_in_page)) },
                supportingContent = { Text(stringResource(R.string.search_text_on_page)) },
                leadingContent = { Icon(Icons.Default.Search, null) }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.reading_mode)) },
                supportingContent = { Text(stringResource(R.string.clean_distraction_free)) },
                leadingContent = { Icon(Icons.Default.MenuBook, null) }
            )

            ListItem(
                headlineContent = { Text(if (isDesktopMode) stringResource(R.string.mobile_site) else stringResource(R.string.desktop_site)) },
                supportingContent = { Text(stringResource(R.string.switch_mobile_desktop)) },
                leadingContent = {
                    Icon(
                        if (isDesktopMode) Icons.Default.PhoneAndroid else Icons.Default.Computer,
                        null
                    )
                }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            ListItem(
                headlineContent = { Text(stringResource(R.string.screenshot)) },
                supportingContent = { Text(stringResource(R.string.capture_current_page)) },
                leadingContent = { Icon(Icons.Default.CameraAlt, null) }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.save_for_offline)) },
                supportingContent = { Text(stringResource(R.string.download_page_offline)) },
                leadingContent = { Icon(Icons.Default.SaveAlt, null) }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.share_page)) },
                supportingContent = { Text(stringResource(R.string.share_via_other_apps)) },
                leadingContent = { Icon(Icons.Default.Share, null) }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            ListItem(
                headlineContent = { Text(stringResource(R.string.clear_cookies_data)) },
                supportingContent = { Text(stringResource(R.string.remove_site_data)) },
                leadingContent = {
                    Icon(Icons.Default.DeleteSweep, null, tint = MaterialTheme.colorScheme.error)
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
