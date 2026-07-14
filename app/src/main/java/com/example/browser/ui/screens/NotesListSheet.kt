package com.example.browser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.browser.R
import com.example.browser.data.local.entity.NoteEntity
import com.example.browser.notes.HighlightColor
import com.example.browser.notes.NoteType
import com.example.browser.ui.viewmodel.BrowserViewModel
import java.text.SimpleDateFormat
import java.util.*

private enum class SortMode { BY_URL, BY_TIME }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListSheet(
    viewModel: BrowserViewModel,
    onDismiss: () -> Unit
) {
    val allNotes by viewModel.allNotes.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var searchedNotes by remember { mutableStateOf<List<NoteEntity>?>(null) }
    var sortMode by remember { mutableStateOf(SortMode.BY_TIME) }
    var showExportDialog by remember { mutableStateOf(false) }

    val displayNotes = searchedNotes ?: allNotes

    // Export dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text(stringResource(R.string.export_notes)) },
            text = { Text(stringResource(R.string.export_notes_confirm)) },
            confirmButton = {
                Button(onClick = {
                    viewModel.exportNotes()
                    showExportDialog = false
                }) { Text(stringResource(R.string.export)) }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.all_notes_count, allNotes.size),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(R.string.export))
                    }
                    IconButton(onClick = {
                        sortMode = if (sortMode == SortMode.BY_TIME) SortMode.BY_URL else SortMode.BY_TIME
                    }) {
                        Icon(
                            if (sortMode == SortMode.BY_TIME) Icons.Default.AccessTime else Icons.Default.Link,
                            contentDescription = stringResource(R.string.sort)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { query ->
                    searchQuery = query
                    if (query.isBlank()) {
                        searchedNotes = null
                    } else {
                        viewModel.searchNotes(query) { results ->
                            searchedNotes = results
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.search_notes)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            searchedNotes = null
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
                        }
                    }
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (displayNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) stringResource(R.string.no_notes_yet) else stringResource(R.string.no_matching_notes),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val grouped = when (sortMode) {
                    SortMode.BY_URL -> displayNotes.groupBy { it.url to it.pageTitle }
                    SortMode.BY_TIME -> {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        displayNotes.groupBy { sdf.format(Date(it.updatedAt)) to "" }
                    }
                }

                LazyColumn(
                    modifier = Modifier.heightIn(max = 500.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    grouped.forEach { (groupKey, notes) ->
                        item(key = "header_${groupKey.first}") {
                            val label = if (sortMode == SortMode.BY_URL) {
                                groupKey.second.ifBlank { groupKey.first }
                            } else {
                                groupKey.first
                            }
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            if (sortMode == SortMode.BY_URL) {
                                Text(
                                    text = groupKey.first,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        items(notes, key = { it.id }) { note ->
                            NoteListItem(
                                note = note,
                                onClick = {
                                    if (sortMode == SortMode.BY_URL) {
                                        viewModel.navigateTo(groupKey.first)
                                    } else {
                                        viewModel.navigateTo(note.url)
                                    }
                                    onDismiss()
                                },
                                onDelete = { viewModel.deleteNote(note.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteListItem(
    note: NoteEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (note.noteType == NoteType.HIGHLIGHT.name) {
                val color = HighlightColor.fromName(note.highlightColor ?: "YELLOW")
                Color(android.graphics.Color.parseColor(color.hex)).copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (note.noteType == NoteType.HIGHLIGHT.name && note.highlightColor != null) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(
                            HighlightColor.fromName(note.highlightColor).hex
                        )))
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row {
                    Text(
                        text = if (note.noteType == NoteType.HIGHLIGHT.name) "🖍️ Highlight" else "📝 Note",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(note.updatedAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
