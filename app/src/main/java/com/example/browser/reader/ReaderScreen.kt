package com.example.browser.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
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

/** Process text for comfortable reading:首行缩进 + 段落间距 */
private fun String.processForReading(): String {
    return lines().joinToString("\n") { line ->
        val trimmed = line.trim()
        if (trimmed.isEmpty()) ""
        else "\u00A0\u00A0\u00A0\u00A0$trimmed" // 4 non-breaking spaces for indent
    }.replace("\n\n\n+".toRegex(), "\n\n") // Normalize multiple blank lines
}

data class ReaderSettings(
    val fontSize: Int = 20,
    val lineSpacing: Float = 2.0f,
    val bgColor: Color = Color(0xFFF5F0E8),
    val textColor: Color = Color(0xFF2C2C2C),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    title: String,
    chapters: List<TextExtractor.Chapter>,
    fullText: String,
    onBack: () -> Unit,
    onOpenInBrowser: () -> Unit,
) {
    var settings by remember { mutableStateOf(ReaderSettings()) }
    var showSettings by remember { mutableStateOf(false) }
    var showChapters by remember { mutableStateOf(false) }
    var chapterIdx by remember { mutableIntStateOf(0) }

    val rawText = if (chapters.isNotEmpty()) chapters.getOrNull(chapterIdx)?.content ?: fullText else fullText
    val text = rawText.processForReading()
    val chTitle = if (chapters.isNotEmpty()) chapters.getOrNull(chapterIdx)?.title ?: title else title

    Box(Modifier.fillMaxSize().background(settings.bgColor)) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(chTitle, maxLines = 1, style = MaterialTheme.typography.titleSmall) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") } },
                actions = {
                    if (chapters.isNotEmpty()) IconButton(onClick = { showChapters = true }) { Icon(Icons.Default.List, "目录") }
                    IconButton(onClick = { showSettings = !showSettings }) { Icon(Icons.Default.Settings, "设置") }
                    IconButton(onClick = onOpenInBrowser) { Icon(Icons.Default.OpenInBrowser, "浏览器") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = settings.bgColor),
            )

            if (text.isBlank()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("无法提取正文", color = settings.textColor.copy(alpha = 0.5f))
                }
            } else {
                val density = LocalDensity.current
                var drag by remember { mutableFloatStateOf(0f) }
                Box(Modifier.weight(1f).pointerInput(Unit) {
                    detectHorizontalDragGestures(onDragEnd = {
                        val th = with(density) { 100.dp.toPx() }
                        if (drag < -th && chapterIdx < chapters.size - 1) chapterIdx++
                        else if (drag > th && chapterIdx > 0) chapterIdx--
                        drag = 0f
                    }) { _, amount -> drag += amount }
                }) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp)) {
                        Text(text, fontSize = settings.fontSize.sp,
                            lineHeight = (settings.fontSize * settings.lineSpacing).sp,
                            color = settings.textColor, fontFamily = FontFamily.Serif,
                            letterSpacing = 0.5.sp)
                    }
                }
                if (chapters.isNotEmpty()) {
                    Text("${chapterIdx + 1} / ${chapters.size}", Modifier.fillMaxWidth().padding(8.dp),
                        textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall,
                        color = settings.textColor.copy(alpha = 0.5f))
                }
            }
        }

        if (showSettings) SettingsPanel(settings, { settings = it }, { showSettings = false }, Modifier.align(Alignment.BottomCenter))

        if (showChapters) ChapterDialog(chapters, chapterIdx, { chapterIdx = it }, { showChapters = false })
    }
}

@Composable
private fun SettingsPanel(s: ReaderSettings, onChange: (ReaderSettings) -> Unit, onDismiss: () -> Unit, mod: Modifier) {
    val bgOpts = listOf("护眼" to Color(0xFFF5F0E8), "白色" to Color.White, "浅灰" to Color(0xFFF0F0F0),
        "深色" to Color(0xFF1E1E1E), "黑色" to Color(0xFF121212))
    Surface(mod.fillMaxWidth(), shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp), tonalElevation = 8.dp) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("字号", Modifier.width(48.dp))
                IconButton(onClick = { if (s.fontSize > 12) onChange(s.copy(fontSize = s.fontSize - 2)) }) { Icon(Icons.Default.Remove, "小") }
                Text("${s.fontSize}sp", Modifier.width(48.dp), textAlign = TextAlign.Center)
                IconButton(onClick = { if (s.fontSize < 32) onChange(s.copy(fontSize = s.fontSize + 2)) }) { Icon(Icons.Default.Add, "大") }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("行距", Modifier.width(48.dp))
                Slider(s.lineSpacing, { onChange(s.copy(lineSpacing = it)) }, Modifier.weight(1f), valueRange = 1.2f..3f, steps = 8)
                Text(String.format("%.1f", s.lineSpacing), Modifier.width(36.dp), textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(8.dp))
            Text("背景", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                bgOpts.forEach { (name, c) ->
                    val dark = c == Color(0xFF1E1E1E) || c == Color(0xFF121212)
                    Surface(Modifier.weight(1f).clickable {
                        onChange(s.copy(bgColor = c, textColor = if (dark) Color(0xFFE0E0E0) else Color(0xFF2C2C2C)))
                    }, shape = RoundedCornerShape(8.dp), color = c) {
                        Text(name, Modifier.padding(vertical = 8.dp), textAlign = TextAlign.Center, fontSize = 12.sp,
                            color = if (dark) Color(0xFFE0E0E0) else Color(0xFF2C2C2C))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDismiss, Modifier.fillMaxWidth()) { Text("收起") }
        }
    }
}

@Composable
private fun ChapterDialog(chs: List<TextExtractor.Chapter>, cur: Int, onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("目录 (${chs.size}章)") },
        text = { LazyColumn(Modifier.heightIn(max = 400.dp)) {
            items(chs.size) { i ->
                val active = i == cur
                Surface(
                    color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)
                ) {
                    ListItem(headlineContent = {
                        Text(chs[i].title,
                            color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                    }, modifier = Modifier.clickable { onSelect(i); onDismiss() })
                }
            }
        } }, confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } })
}
