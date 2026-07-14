package com.example.browser.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.browser.data.repository.BrowserRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }
    val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text("Backup & Restore", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
            Text("Export or import your bookmarks, history, and settings.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 16.dp))

            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row { Icon(Icons.Default.Backup, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(8.dp)); Text("Backup", style = MaterialTheme.typography.titleMedium) }
                    Spacer(Modifier.height(8.dp))
                    Text("Save all data to a JSON file.", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        try {
                            val repo = BrowserRepository.getInstance(context)
                            val bk = repo.getBookmarks().map { mapOf("url" to it.url, "title" to it.title) }
                            val hi = repo.getHistory().take(100).map { mapOf("url" to it.url, "title" to it.title) }
                            val rl = repo.getReadingList().map { mapOf("url" to it.url, "title" to it.title) }
                            val data = Json.encodeToString(mapOf("version" to 1, "timestamp" to System.currentTimeMillis(), "bookmarks" to bk, "history" to hi, "readingList" to rl))
                            val dir = File(context.cacheDir, "backups"); dir.mkdirs()
                            val file = File(dir, "browser_backup_${dateFormat.format(Date())}.json")
                            file.writeText(data)
                            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            val intent = Intent(Intent.ACTION_SEND).apply { type = "application/json"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                            context.startActivity(Intent.createChooser(intent, "Save Backup").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            message = "Backup created!"
                        } catch (e: Exception) { message = "Error: ${e.message}" }
                    }) { Text("Create Backup") }
                }
            }

            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row { Icon(Icons.Default.Restore, null, tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.width(8.dp)); Text("Restore", style = MaterialTheme.typography.titleMedium) }
                    Spacer(Modifier.height(8.dp))
                    Text("Import data from a backup file.", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { message = "Open a .json backup file with this browser to restore." }) { Text("Select Backup File") }
                }
            }

            message?.let { Spacer(Modifier.height(8.dp)); Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
