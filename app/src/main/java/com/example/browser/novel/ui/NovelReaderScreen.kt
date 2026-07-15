package com.example.browser.novel.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.browser.R
import com.example.browser.data.local.entity.ChapterEntity
import com.example.browser.ui.viewmodel.BrowserViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelReaderScreen(
    viewModel: BrowserViewModel,
    novelId: Long,
    initialChapterIndex: Int,
    onDismiss: () -> Unit
) {
    val novel by viewModel.currentNovel.collectAsState()
    val chapters by viewModel.getChapters(novelId).collectAsState(initial = emptyList())
    var currentChapterIndex by remember { mutableStateOf(initialChapterIndex) }
    var currentChapter by remember { mutableStateOf<ChapterEntity?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(false) }
    var showCatalog by remember { mutableStateOf(false) }
    var fontSize by remember { mutableStateOf(18f) }
    var bgColor by remember { mutableStateOf(ReaderBgColor.WHITE) }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    // Load chapter content
    LaunchedEffect(currentChapterIndex) {
        isLoading = true
        val chapter = viewModel.getChapterForReading(novelId, currentChapterIndex)
        currentChapter = chapter
        isLoading = false
        scrollState.scrollTo(0)
        // Update reading progress
        viewModel.updateNovelReadProgress(novelId, currentChapterIndex)
        // Pre-cache next chapter
        viewModel.preCacheNextChapter(novelId, currentChapterIndex)
    }

    val bgColorValue = when (bgColor) {
        ReaderBgColor.WHITE -> Color(0xFFFFFBF0)     // Warm white
        ReaderBgColor.GREEN -> Color(0xFFCCE8CF)      // Eye-care green
        ReaderBgColor.BEIGE -> Color(0xFFF5E6CC)      // Beige/parchment
        ReaderBgColor.DARK -> Color(0xFF1A1A2E)       // Dark mode
    }
    val textColor = if (bgColor == ReaderBgColor.DARK) Color(0xFFCCCCCC) else Color(0xFF333333)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColorValue)
            .clickable { showControls = !showControls }
    ) {
        if (isLoading) {
            // Loading state
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (currentChapter == null) {
            // Chapter not found
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = textColor.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.chapter_not_found),
                    color = textColor.copy(alpha = 0.7f)
                )
            }
        } else {
            // Chapter content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 48.dp)
            ) {
                // Chapter title
                Text(
                    text = currentChapter!!.title,
                    fontSize = (fontSize + 4).sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    textAlign = TextAlign.Center
                )

                Divider(
                    modifier = Modifier.padding(bottom = 16.dp),
                    thickness = 0.5.dp,
                    color = textColor.copy(alpha = 0.2f)
                )

                // Chapter content
                if (currentChapter!!.content.isNotBlank()) {
                    // Split content into paragraphs for better readability
                    val paragraphs = currentChapter!!.content
                        .split("\n")
                        .filter { it.isNotBlank() }

                    paragraphs.forEach { paragraph ->
                        Text(
                            text = "　　$paragraph",  // Full-width space indent
                            fontSize = fontSize.sp,
                            lineHeight = (fontSize * 1.8).sp,
                            color = textColor,
                            fontFamily = FontFamily.Serif,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.chapter_content_empty),
                        fontSize = fontSize.sp,
                        color = textColor.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Chapter navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (currentChapterIndex > 0) {
                        TextButton(
                            onClick = {
                                currentChapterIndex--
                                scope.launch { scrollState.scrollTo(0) }
                            }
                        ) {
                            Icon(Icons.Default.NavigateBefore, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.prev_chapter))
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (chapters.isNotEmpty() && currentChapterIndex < chapters.size - 1) {
                        TextButton(
                            onClick = {
                                currentChapterIndex++
                                scope.launch { scrollState.scrollTo(0) }
                            }
                        ) {
                            Text(stringResource(R.string.next_chapter))
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.NavigateNext, contentDescription = null)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(64.dp))
            }
        }

        // Top control bar (shows when tapped)
        AnimatedVisibility(
            visible = showControls,
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = novel?.title ?: "",
                        maxLines = 1,
                        style = MaterialTheme.typography.titleSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { showCatalog = true }) {
                        Icon(Icons.Default.List, contentDescription = stringResource(R.string.novel_catalog))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = bgColorValue.copy(alpha = 0.95f)
                )
            )
        }

        // Bottom control bar (shows when tapped)
        AnimatedVisibility(
            visible = showControls,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            BottomAppBar(
                containerColor = bgColorValue.copy(alpha = 0.95f),
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Font size controls
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { fontSize = (fontSize - 2).coerceAtLeast(12f) }) {
                            Icon(
                                Icons.Default.Remove,
                                contentDescription = stringResource(R.string.decrease_font)
                            )
                        }
                        Text(
                            text = "${fontSize.toInt()}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        IconButton(onClick = { fontSize = (fontSize + 2).coerceAtLeast(28f) }) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = stringResource(R.string.increase_font)
                            )
                        }
                    }

                    // Background color selector
                    Row {
                        ReaderBgColor.entries.forEach { bg ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .padding(2.dp)
                                    .background(
                                        when (bg) {
                                            ReaderBgColor.WHITE -> Color(0xFFFFFBF0)
                                            ReaderBgColor.GREEN -> Color(0xFFCCE8CF)
                                            ReaderBgColor.BEIGE -> Color(0xFFF5E6CC)
                                            ReaderBgColor.DARK -> Color(0xFF1A1A2E)
                                        },
                                        shape = MaterialTheme.shapes.small
                                    )
                                    .clickable { bgColor = bg }
                            )
                            if (bg != ReaderBgColor.entries.last()) {
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                        }
                    }

                    // Chapter info
                    Text(
                        text = "${currentChapterIndex + 1}/${chapters.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }

    // Catalog overlay
    if (showCatalog) {
        NovelCatalogScreen(
            viewModel = viewModel,
            novelId = novelId,
            onDismiss = { showCatalog = false },
            onChapterClick = { index ->
                currentChapterIndex = index
                showCatalog = false
                scope.launch { scrollState.scrollTo(0) }
            }
        )
    }
}

enum class ReaderBgColor {
    WHITE, GREEN, BEIGE, DARK
}
