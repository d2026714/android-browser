package com.example.browser.reader

import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

data class ReaderSettings(
    val fontSize: Int = 18,
    val lineSpacing: Float = 1.8f,
    val bgColor: Color = Color(0xFFF5F0E8),
    val textColor: Color = Color(0xFF2C2C2C),
    val fontFamily: FontFamily = FontFamily.Serif,
)

@Composable
fun ReaderScreen(
    title: String,
    chapters: List<TextExtractor.Chapter>,
    fullText: String,
    onBack: () -> Unit,
    onOpenInBrowser: (String) -> Unit,
    webViewUrl: String,
) {
    var settings by remember { mutableStateOf(ReaderSettings()) }
    var showSettings by remember { mutableStateOf(false) }
    var showChapterList by remember { mutableStateOf(false) }
    var currentChapterIndex by remember { mutableIntStateOf(0) }

    val displayText = if (chapters.isNotEmpty()) {
        chapters.getOrNull(currentChapterIndex)?.content ?: fullText
    } else {
        fullText
    }

    val displayTitle = if (chapters.isNotEmpty()) {
        chapters.getOrNull(currentChapterIndex)?.title ?: title
    } else {
        title
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(settings.bgColor)
    ) {
        // Main reading area
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            TopAppBar(
                title = {
                    Text(
                        displayTitle,
                        maxLines = 1,
                        style = MaterialTheme.typography.titleSmall,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (chapters.isNotEmpty()) {
                        IconButton(onClick = { showChapterList = true }) {
                            Icon(Icons.Default.List, contentDescription = "目录")
                        }
                    }
                    IconButton(onClick = { showSettings = !showSettings }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                    IconButton(onClick = { onOpenInBrowser(webViewUrl) }) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = "浏览器打开")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = settings.bgColor,
                ),
            )

            // Content
            if (displayText.isBlank()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "无法提取正文内容",
                        color = settings.textColor.copy(alpha = 0.5f),
                    )
                }
            } else {
                // Paginated reading
                ReaderContent(
                    text = displayText,
                    settings = settings,
                    onNextChapter = {
                        if (currentChapterIndex < chapters.size - 1) {
                            currentChapterIndex++
                        }
                    },
                    onPrevChapter = {
                        if (currentChapterIndex > 0) {
                            currentChapterIndex--
                        }
                    },
                    modifier = Modifier.weight(1f),
                )

                // Chapter indicator
                if (chapters.isNotEmpty()) {
                    Text(
                        text = "${currentChapterIndex + 1} / ${chapters.size}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = settings.textColor.copy(alpha = 0.5f),
                    )
                }
            }
        }

        // Settings panel
        if (showSettings) {
            ReaderSettingsPanel(
                settings = settings,
                onSettingsChange = { settings = it },
                onDismiss = { showSettings = false },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        // Chapter list
        if (showChapterList) {
            ChapterListDialog(
                chapters = chapters,
                currentIndex = currentChapterIndex,
                onSelect = { currentChapterIndex = it },
                onDismiss = { showChapterList = false },
            )
        }
    }
}

@Composable
private fun ReaderContent(
    text: String,
    settings: ReaderSettings,
    onNextChapter: () -> Unit,
    onPrevChapter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val threshold = with(density) { 100.dp.toPx() }
                        if (dragAccumulator < -threshold) {
                            onNextChapter()
                        } else if (dragAccumulator > threshold) {
                            onPrevChapter()
                        }
                        dragAccumulator = 0f
                    },
                ) { _, dragAmount ->
                    dragAccumulator += dragAmount
                }
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Text(
                text = text,
                fontSize = settings.fontSize.sp,
                lineHeight = (settings.fontSize * settings.lineSpacing).sp,
                color = settings.textColor,
                fontFamily = settings.fontFamily,
                fontWeight = FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun ReaderSettingsPanel(
    settings: ReaderSettings,
    onSettingsChange: (ReaderSettings) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColorOptions = listOf(
        "护眼" to Color(0xFFF5F0E8),
        "白色" to Color.White,
        "浅灰" to Color(0xFFF0F0F0),
        "深色" to Color(0xFF1E1E1E),
        "黑色" to Color(0xFF121212),
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Font size
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("字号", modifier = Modifier.width(48.dp))
                IconButton(onClick = {
                    if (settings.fontSize > 12) onSettingsChange(settings.copy(fontSize = settings.fontSize - 2))
                }) {
                    Icon(Icons.Default.Remove, contentDescription = "减小")
                }
                Text("${settings.fontSize}sp", modifier = Modifier.width(48.dp), textAlign = TextAlign.Center)
                IconButton(onClick = {
                    if (settings.fontSize < 32) onSettingsChange(settings.copy(fontSize = settings.fontSize + 2))
                }) {
                    Icon(Icons.Default.Add, contentDescription = "增大")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Line spacing
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("行距", modifier = Modifier.width(48.dp))
                Slider(
                    value = settings.lineSpacing,
                    onValueChange = { onSettingsChange(settings.copy(lineSpacing = it)) },
                    valueRange = 1.2f..3.0f,
                    steps = 8,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    String.format("%.1f", settings.lineSpacing),
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Background color
            Text("背景", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                bgColorOptions.forEach { (name, color) ->
                    val isSelected = settings.bgColor == color
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                val textColor = if (color == Color(0xFF1E1E1E) || color == Color(0xFF121212))
                                    Color(0xFFE0E0E0) else Color(0xFF2C2C2C)
                                onSettingsChange(settings.copy(bgColor = color, textColor = textColor))
                            },
                        shape = RoundedCornerShape(8.dp),
                        color = color,
                        border = if (isSelected) ButtonDefaults.outlinedButtonBorder else null,
                    ) {
                        Text(
                            name,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            color = if (color == Color(0xFF1E1E1E) || color == Color(0xFF121212))
                                Color(0xFFE0E0E0) else Color(0xFF2C2C2C),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Close button
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("收起")
            }
        }
    }
}

@Composable
private fun ChapterListDialog(
    chapters: List<TextExtractor.Chapter>,
    currentIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("目录 (${chapters.size}章)") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(chapters.size) { index ->
                    ListItem(
                        headlineContent = {
                            Text(
                                chapters[index].title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (index == currentIndex)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (index == currentIndex) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        modifier = Modifier.clickable {
                            onSelect(index)
                            onDismiss()
                        },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
    )
}
