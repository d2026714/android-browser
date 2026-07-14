package com.example.browser.ui.screens

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
import com.example.browser.data.model.QuickLink
import com.example.browser.ui.viewmodel.BrowserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickLinksEditorSheet(
    viewModel: BrowserViewModel,
    onDismiss: () -> Unit
) {
    val quickLinks by viewModel.quickLinks.collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var editingLink by remember { mutableStateOf<QuickLink?>(null) }
    var newTitle by remember { mutableStateOf("") }
    var newUrl by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Quick Links", style = MaterialTheme.typography.titleLarge)
                FilledTonalButton(onClick = {
                    newTitle = ""; newUrl = ""; editingLink = null; showAddDialog = true
                }) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn {
                items(quickLinks) { link ->
                    ListItem(
                        headlineContent = {
                            Text(link.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        supportingContent = {
                            Text(link.url, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        },
                        leadingContent = {
                            Icon(Icons.Default.Language, null)
                        },
                        trailingContent = {
                            Row {
                                IconButton(onClick = {
                                    editingLink = link; newTitle = link.title; newUrl = link.url
                                    showAddDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { viewModel.removeQuickLink(link.id) }) {
                                    Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(if (editingLink != null) "Edit Quick Link" else "Add Quick Link") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newUrl,
                        onValueChange = { newUrl = it },
                        label = { Text("URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newTitle.isNotBlank() && newUrl.isNotBlank()) {
                        if (editingLink != null) {
                            viewModel.updateQuickLink(editingLink!!.copy(title = newTitle, url = newUrl))
                        } else {
                            viewModel.addQuickLink(QuickLink(title = newTitle, url = newUrl))
                        }
                        showAddDialog = false
                    }
                }) { Text(if (editingLink != null) "Save" else "Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}
