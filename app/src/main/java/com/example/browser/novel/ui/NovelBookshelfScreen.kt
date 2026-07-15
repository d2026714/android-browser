package com.example.browser.novel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import com.example.browser.R
import com.example.browser.data.local.entity.NovelEntity
import com.example.browser.ui.viewmodel.BrowserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelBookshelfScreen(
    viewModel: BrowserViewModel,
    onDismiss: () -> Unit,
    onNovelClick: (Long) -> Unit
) {
    val novels by viewModel.novelList.collectAsState(initial = emptyList())
    var sortByLastRead by remember { mutableStateOf(true) }
    var showDeleteConfirm by remember { mutableStateOf<NovelEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.novel_bookshelf)) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    // Sort toggle
                    IconButton(onClick = { sortByLastRead = !sortByLastRead }) {
                        Icon(
                            Icons.Default.Sort,
                            contentDescription = stringResource(R.string.sort),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (novels.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.LibraryBooks,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.no_novels_yet),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.no_novels_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        } else {
            val sortedNovels = if (sortByLastRead) {
                novels.sortedByDescending { it.lastReadTime }
            } else {
                novels.sortedByDescending { it.lastUpdated }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(sortedNovels, key = { it.id }) { novel ->
                    NovelGridItem(
                        novel = novel,
                        onClick = { onNovelClick(novel.id) },
                        onLongClick = { showDeleteConfirm = novel }
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    showDeleteConfirm?.let { novel ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text(stringResource(R.string.remove_novel)) },
            text = { Text(stringResource(R.string.remove_novel_confirm, novel.title)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeNovel(novel.id)
                    showDeleteConfirm = null
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun NovelGridItem(
    novel: NovelEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Cover image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            // Cover with title initial
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = novel.title.take(2),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (novel.author.isNotBlank()) {
                    Text(
                        text = novel.author.take(4),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Reading progress indicator
            if (novel.totalChapters > 0 && novel.lastReadChapterIndex > 0) {
                val progress = novel.lastReadChapterIndex.toFloat() / novel.totalChapters
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            }

            // Update badge (if new chapters since last read)
            if (novel.totalChapters > novel.lastReadChapterIndex + 1) {
                val newCount = novel.totalChapters - novel.lastReadChapterIndex - 1
                if (newCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .background(
                                MaterialTheme.colorScheme.error,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (newCount > 99) "99+" else "$newCount",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Novel title
        Text(
            text = novel.title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )

        // Chapter info
        if (novel.totalChapters > 0) {
            Text(
                text = stringResource(R.string.chapters_count, novel.totalChapters),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
