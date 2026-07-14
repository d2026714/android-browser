package com.example.browser.ui.screens

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

data class DownloadItem(
    val id: Long,
    val title: String,
    val uri: String,
    val status: Int,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val lastModified: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var downloads by remember { mutableStateOf(listOf<DownloadItem>()) }

    // Poll download status
    LaunchedEffect(Unit) {
        while (true) {
            downloads = getDownloads(context)
            delay(2000)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Downloads",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (downloads.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No downloads yet",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                LazyColumn {
                    items(downloads) { item ->
                        DownloadItemRow(item)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun DownloadItemRow(item: DownloadItem) {
    val statusText = when (item.status) {
        DownloadManager.STATUS_RUNNING -> "Downloading..."
        DownloadManager.STATUS_SUCCESSFUL -> "Complete"
        DownloadManager.STATUS_FAILED -> "Failed"
        DownloadManager.STATUS_PAUSED -> "Paused"
        DownloadManager.STATUS_PENDING -> "Pending"
        else -> "Unknown"
    }
    val statusIcon = when (item.status) {
        DownloadManager.STATUS_RUNNING -> Icons.Default.Downloading
        DownloadManager.STATUS_SUCCESSFUL -> Icons.Default.CheckCircle
        DownloadManager.STATUS_FAILED -> Icons.Default.Error
        else -> Icons.Default.Schedule
    }
    val statusColor = when (item.status) {
        DownloadManager.STATUS_SUCCESSFUL -> MaterialTheme.colorScheme.primary
        DownloadManager.STATUS_FAILED -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }

    val progress = if (item.totalBytes > 0) {
        item.downloadedBytes.toFloat() / item.totalBytes.toFloat()
    } else 0f

    ListItem(
        headlineContent = {
            Text(
                text = item.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Column {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor
                )
                if (item.status == DownloadManager.STATUS_RUNNING && item.totalBytes > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "${formatBytes(item.downloadedBytes)} / ${formatBytes(item.totalBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        },
        leadingContent = {
            Icon(statusIcon, null, tint = statusColor)
        }
    )
}

fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    if (bytes < 1024 * 1024) return "${bytes / 1024} KB"
    if (bytes < 1024 * 1024 * 1024) return "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
    return "${"%.2f".format(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
}

fun getDownloads(context: Context): List<DownloadItem> {
    val items = mutableListOf<DownloadItem>()
    try {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query()
        val cursor: Cursor? = dm.query(query)
        cursor?.use {
            val idIdx = it.getColumnIndex(DownloadManager.COLUMN_ID)
            val titleIdx = it.getColumnIndex(DownloadManager.COLUMN_TITLE)
            val uriIdx = it.getColumnIndex(DownloadManager.COLUMN_URI)
            val statusIdx = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val totalIdx = it.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            val downloadedIdx = it.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val modifiedIdx = it.getColumnIndex(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP)

            while (it.moveToNext()) {
                items.add(
                    DownloadItem(
                        id = it.getLong(idIdx),
                        title = it.getString(titleIdx) ?: "Unknown",
                        uri = it.getString(uriIdx) ?: "",
                        status = it.getInt(statusIdx),
                        totalBytes = it.getLong(totalIdx),
                        downloadedBytes = it.getLong(downloadedIdx),
                        lastModified = it.getLong(modifiedIdx)
                    )
                )
            }
        }
    } catch (_: Exception) {}
    return items.sortedByDescending { it.lastModified }
}
