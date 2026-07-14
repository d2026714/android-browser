package com.example.browser.ui.screens

import android.content.Intent
import android.util.Log
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
import com.example.browser.ui.viewmodel.BrowserViewModel
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "BackupRestoreSheet"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreSheet(viewModel: BrowserViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }
    val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    // Collect current data from ViewModel's flows
    val bookmarks by viewModel.bookmarkManager.bookmarks.collectAsState(initial = emptyList())
    val history by viewModel.bookmarkManager.history.collectAsState(initial = emptyList())
    val readingList by viewModel.bookmarkManager.readingList.collectAsState(initial = emptyList())

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text(stringResource(R.string.backup_restore), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
            Text(stringResource(R.string.backup_restore_description),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 16.dp))

            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row { Icon(Icons.Default.Backup, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.backup), style = MaterialTheme.typography.titleMedium) }
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.save_all_data_json), style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        try {
                            val bk = bookmarks.map { mapOf("url" to it.url, "title" to it.title) }
                            val hi = history.take(100).map { mapOf("url" to it.url, "title" to it.title) }
                            val rl = readingList.map { mapOf("url" to it.url, "title" to it.title) }
                            val data = Json.encodeToString(mapOf("version" to 1, "timestamp" to System.currentTimeMillis(), "bookmarks" to bk, "history" to hi, "readingList" to rl))
                            val dir = File(context.cacheDir, "backups"); dir.mkdirs()
                            val file = File(dir, "browser_backup_${dateFormat.format(Date())}.json")
                            file.writeText(data)
                            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            val intent = Intent(Intent.ACTION_SEND).apply { type = "application/json"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                            context.startActivity(Intent.createChooser(intent, "Save Backup").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            message = stringResource(R.string.backup_created)
                            Log.d(TAG, "Backup created: ${file.absolutePath}")
                        } catch (e: Exception) {
                            Log.e(TAG, "Backup failed", e)
                            message = "Error: ${e.message}"
                        }
                    }) { Text(stringResource(R.string.create_backup)) }
                }
            }

            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row { Icon(Icons.Default.Restore, null, tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.restore), style = MaterialTheme.typography.titleMedium) }
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.import_from_backup), style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { message = stringResource(R.string.open_backup_to_restore) }) { Text(stringResource(R.string.select_backup_file)) }
                }
            }

            message?.let { Spacer(Modifier.height(8.dp)); Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
