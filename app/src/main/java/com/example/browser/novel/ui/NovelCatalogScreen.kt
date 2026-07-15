package com.example.browser.novel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.browser.R
import com.example.browser.data.local.entity.ChapterEntity
import com.example.browser.ui.viewmodel.BrowserViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelCatalogScreen(
    viewModel: BrowserViewModel,
    novelId: Long,
    onDismiss: () -> Unit,
    onChapterClick: (Int) -> Unit
) {
    val chapters by viewModel.getChapters(novelId).collectAsState(initial = emptyList())
    val novel by viewModel.currentNovel.collectAsState()
    var isDownloadingAll by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = novel?.title ?: stringResource(R.string.novel_catalog),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (novel != null) {
                            Text(
                                text = stringResource(
                                    R.string.catalog_subtitle,
                                    chapters.size,
                                    chapters.count { it.isCached }
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    // Download all chapters
                    IconButton(
                        onClick = {
                            isDownloadingAll = true
                            viewModel.cacheAllChapters(novelId) { _, _ ->
                                isDownloadingAll = false
                            }
                        },
                        enabled = !isDownloadingAll
                    ) {
                        if (isDownloadingAll) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = stringResource(R.string.download_all_chapters)
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (chapters.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.List,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.no_chapters_loaded),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            val listState = rememberLazyListState()

            // Scroll to current reading position
            LaunchedEffect(novel, chapters) {
                val currentChapter = novel?.lastReadChapterIndex ?: 0
                if (currentChapter > 0 && currentChapter < chapters.size) {
                    listState.animateScrollToItem(currentChapter)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(chapters, key = { it.id }) { chapter ->
                    ChapterListItem(
                        chapter = chapter,
                        isCurrentChapter = chapter.chapterIndex == (novel?.lastReadChapterIndex ?: -1),
                        onClick = { onChapterClick(chapter.chapterIndex) }
                    )
                }
            }
        }
    }
}

@Composable
fun ChapterListItem(
    chapter: ChapterEntity,
    isCurrentChapter: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isCurrentChapter)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else
                    MaterialTheme.colorScheme.surface
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Chapter title
        Text(
            text = chapter.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isCurrentChapter) FontWeight.Bold else FontWeight.Normal,
            color = if (isCurrentChapter)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Cached status indicator
        if (chapter.isCached) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = stringResource(R.string.cached),
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
        } else {
            Icon(
                Icons.Default.CloudDownload,
                contentDescription = stringResource(R.string.not_cached),
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }

    Divider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    )
}
