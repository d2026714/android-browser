package com.example.browser.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.browser.data.model.ReadingItem
import com.example.browser.ui.viewmodel.BrowserViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingListSheet(
    viewModel: BrowserViewModel,
    onDismiss: () -> Unit
) {
    val readingList by viewModel.readingList.collectAsState()
    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Reading List", style = MaterialTheme.typography.titleLarge)
                if (readingList.isNotEmpty()) {
                    Text("${readingList.size} items", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (readingList.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.MenuBook, null, modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No saved articles", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
            } else {
                LazyColumn {
                    items(readingList) { item ->
                        ListItem(
                            headlineContent = {
                                Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    color = if (item.isRead) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    else MaterialTheme.colorScheme.onSurface)
                            },
                            supportingContent = {
                                Column {
                                    Text(item.url, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                                    Text(dateFormat.format(Date(item.addedAt)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                                }
                            },
                            leadingContent = {
                                Icon(
                                    if (item.isRead) Icons.Default.CheckCircle else Icons.Default.MenuBook,
                                    null, tint = if (item.isRead) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.secondary
                                )
                            },
                            trailingContent = {
                                IconButton(onClick = { viewModel.removeFromReadingList(item.url) }) {
                                    Icon(Icons.Default.Close, "Remove", modifier = Modifier.size(18.dp))
                                }
                            },
                            modifier = Modifier.clickable {
                                viewModel.navigateTo(item.url)
                                viewModel.markReadingItemRead(item.url)
                                onDismiss()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
