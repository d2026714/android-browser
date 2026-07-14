package com.example.browser.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.browser.R
import androidx.core.content.FileProvider
import com.example.browser.data.local.entity.DownloadEntity
import com.example.browser.data.local.entity.DownloadEntity.Companion.STATUS_CANCELLED
import com.example.browser.data.local.entity.DownloadEntity.Companion.STATUS_COMPLETED
import com.example.browser.data.local.entity.DownloadEntity.Companion.STATUS_DOWNLOADING
import com.example.browser.data.local.entity.DownloadEntity.Companion.STATUS_FAILED
import com.example.browser.data.local.entity.DownloadEntity.Companion.STATUS_PAUSED
import com.example.browser.data.local.entity.DownloadEntity.Companion.STATUS_PENDING
import com.example.browser.download.BrowserDownloadManager
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadManagerScreen(
    onDismiss: () -> Unit,
    downloadManager: BrowserDownloadManager
) {
    val context = LocalContext.current
    val allDownloads by downloadManager.allDownloads.collectAsState(initial = emptyList())
    val progressUpdates by downloadManager.progressUpdates.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.tab_all),
        stringResource(R.string.tab_active),
        stringResource(R.string.tab_completed),
        stringResource(R.string.tab_failed)
    )

    val filteredDownloads = when (selectedTab) {
        1 -> allDownloads.filter { it.status == STATUS_DOWNLOADING || it.status == STATUS_PENDING || it.status == STATUS_PAUSED }
        2 -> allDownloads.filter { it.status == STATUS_COMPLETED }
        3 -> allDownloads.filter { it.status == STATUS_FAILED || it.status == STATUS_CANCELLED }
        else -> allDownloads
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // Header with clear buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.downloads),
                    style = MaterialTheme.typography.titleLarge
                )
                Row {
                    if (allDownloads.any { it.status == STATUS_COMPLETED }) {
                        TextButton(onClick = { downloadManager.clearCompleted() }) {
                            Text(stringResource(R.string.clear_completed), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (allDownloads.any { it.status == STATUS_FAILED || it.status == STATUS_CANCELLED }) {
                        TextButton(onClick = { downloadManager.clearFailed() }) {
                            Text(stringResource(R.string.clear_failed), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tab row
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 0.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredDownloads.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
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
                            text = when (selectedTab) {
                                1 -> stringResource(R.string.no_active_downloads)
                                2 -> stringResource(R.string.no_completed_downloads)
                                3 -> stringResource(R.string.no_failed_downloads)
                                else -> stringResource(R.string.no_downloads_yet)
                            },
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredDownloads, key = { it.id }) { entity ->
                        DownloadItemCard(
                            entity = entity,
                            liveProgress = progressUpdates[entity.id],
                            downloadManager = downloadManager,
                            onOpenFile = {
                                val file = File(entity.filePath)
                                if (file.exists()) {
                                    try {
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, entity.mimeType)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // Fallback: open folder
                                        openFolder(context, entity.filePath)
                                    }
                                }
                            },
                            onOpenFolder = { openFolder(context, entity.filePath) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DownloadItemCard(
    entity: DownloadEntity,
    liveProgress: Long?,
    downloadManager: BrowserDownloadManager,
    onOpenFile: () -> Unit,
    onOpenFolder: () -> Unit
) {
    val status = entity.status
    val downloadedBytes = liveProgress ?: entity.downloadedBytes
    val totalBytes = entity.totalBytes
    val progress = if (totalBytes > 0) {
        (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val statusText = when (status) {
        STATUS_PENDING -> stringResource(R.string.status_waiting)
        STATUS_DOWNLOADING -> if (totalBytes > 0) {
            "${formatSize(downloadedBytes)} / ${formatSize(totalBytes)}"
        } else {
            formatSize(downloadedBytes)
        }
        STATUS_PAUSED -> stringResource(R.string.status_paused_with_size, formatSize(downloadedBytes))
        STATUS_COMPLETED -> stringResource(R.string.status_completed_with_size, formatSize(totalBytes))
        STATUS_FAILED -> stringResource(R.string.status_failed_with_message, entity.errorMessage ?: stringResource(R.string.unknown_error))
        STATUS_CANCELLED -> stringResource(R.string.status_cancelled)
        else -> stringResource(R.string.status_unknown)
    }

    val statusColor = when (status) {
        STATUS_COMPLETED -> MaterialTheme.colorScheme.primary
        STATUS_FAILED, STATUS_CANCELLED -> MaterialTheme.colorScheme.error
        STATUS_PAUSED -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurface
    }

    val statusIcon = when (status) {
        STATUS_PENDING -> Icons.Default.Schedule
        STATUS_DOWNLOADING -> Icons.Default.Downloading
        STATUS_PAUSED -> Icons.Default.PauseCircle
        STATUS_COMPLETED -> Icons.Default.CheckCircle
        STATUS_FAILED -> Icons.Default.Error
        STATUS_CANCELLED -> Icons.Default.Cancel
        else -> Icons.Default.InsertDriveFile
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // File name and status icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entity.fileName,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Progress bar for active downloads
            if (status == STATUS_DOWNLOADING || status == STATUS_PAUSED) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }

            // Action buttons
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (status) {
                    STATUS_DOWNLOADING -> {
                        IconButton(onClick = { downloadManager.pauseDownload(entity.id) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Pause, stringResource(R.string.pause), modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { downloadManager.cancelDownload(entity.id) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Close, stringResource(R.string.cancel), modifier = Modifier.size(20.dp))
                        }
                    }
                    STATUS_PAUSED, STATUS_PENDING -> {
                        IconButton(onClick = { downloadManager.resumeDownload(entity.id) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.PlayArrow, stringResource(R.string.resume), modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { downloadManager.cancelDownload(entity.id) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Close, stringResource(R.string.cancel), modifier = Modifier.size(20.dp))
                        }
                    }
                    STATUS_FAILED -> {
                        IconButton(onClick = { downloadManager.retryDownload(entity.id) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Refresh, stringResource(R.string.retry), modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { downloadManager.removeDownload(entity.id) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Delete, stringResource(R.string.delete), modifier = Modifier.size(20.dp))
                        }
                    }
                    STATUS_COMPLETED -> {
                        IconButton(onClick = onOpenFile, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.OpenInNew, stringResource(R.string.open), modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = onOpenFolder, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.FolderOpen, stringResource(R.string.folder), modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { downloadManager.removeDownload(entity.id) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Delete, stringResource(R.string.delete), modifier = Modifier.size(20.dp))
                        }
                    }
                    STATUS_CANCELLED -> {
                        IconButton(onClick = { downloadManager.removeDownload(entity.id) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Delete, stringResource(R.string.delete), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun openFolder(context: android.content.Context, filePath: String) {
    try {
        val file = File(filePath)
        val parent = file.parentFile ?: return
        val uri = Uri.parse("content://com.android.externalstorage.documents/document/primary:${parent.absolutePath.removePrefix("/storage/emulated/0/")}")
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "resource/folder")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Try with a file manager intent
            val fileManagerIntent = Intent(Intent.ACTION_VIEW).apply {
                val fileUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    parent
                )
                setDataAndType(fileUri, "resource/folder")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fileManagerIntent)
        }
    } catch (e: Exception) {
        android.util.Log.e("DownloadManagerScreen", "Failed to open folder", e)
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    if (bytes < 1024 * 1024) return "${"%.1f".format(bytes / 1024.0)} KB"
    if (bytes < 1024 * 1024 * 1024) return "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
    return "${"%.2f".format(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
}
