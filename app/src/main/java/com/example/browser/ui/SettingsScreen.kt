package com.example.browser.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.browser.util.SearchEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: BrowserViewModel,
    onBack: () -> Unit,
) {
    val adBlockEnabled by viewModel.adBlockEnabled.collectAsState()
    val darkMode by viewModel.darkMode.collectAsState()
    val searchEngine by viewModel.searchEngine.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }
    var showEngineDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("设置") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            // ── Search Engine ──
            SettingsSection(title = "搜索引擎") {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth().clickable { showEngineDialog = true },
                ) {
                    ListItem(
                        headlineContent = { Text(searchEngine.displayName) },
                        supportingContent = { Text("点击切换搜索引擎") },
                        leadingContent = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                    )
                }
            }

            // ── Ad Blocker ──
            SettingsSection(title = "广告拦截") {
                Card(modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text("广告拦截器") },
                        supportingContent = { Text("拦截页面中的广告内容") },
                        leadingContent = { Icon(Icons.Default.Block, contentDescription = null) },
                        trailingContent = {
                            Switch(
                                checked = adBlockEnabled,
                                onCheckedChange = { viewModel.setAdBlockEnabled(it) },
                            )
                        },
                    )
                }
            }

            // ── Appearance ──
            SettingsSection(title = "外观") {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        ListItem(
                            headlineContent = { Text("深色模式") },
                            supportingContent = { Text("手动切换深色/浅色主题") },
                            leadingContent = {
                                Icon(Icons.Default.DarkMode, contentDescription = null)
                            },
                            trailingContent = {
                                Switch(
                                    checked = darkMode,
                                    onCheckedChange = { viewModel.setDarkMode(it) },
                                )
                            },
                        )
                    }
                }
            }

            // ── Font Size ──
            SettingsSection(title = "字体大小") {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "当前大小: ${fontSize}%",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = fontSize.toFloat(),
                            onValueChange = { viewModel.setFontSize(it.toInt()) },
                            valueRange = 50f..200f,
                            steps = 5,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("小", style = MaterialTheme.typography.labelSmall)
                            Text("标准", style = MaterialTheme.typography.labelSmall)
                            Text("大", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // ── Data ──
            SettingsSection(title = "数据") {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth().clickable { showClearDialog = true },
                ) {
                    ListItem(
                        headlineContent = { Text("清除所有数据") },
                        supportingContent = { Text("删除所有书签和历史记录") },
                        leadingContent = {
                            Icon(
                                Icons.Default.DeleteForever,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        },
                    )
                }
            }

            // ── About ──
            SettingsSection(title = "关于") {
                Card(modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text("浏览器") },
                        supportingContent = { Text("v3.0.0 · Kotlin + WebView + Compose") },
                        leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Search engine dialog
    if (showEngineDialog) {
        AlertDialog(
            onDismissRequest = { showEngineDialog = false },
            title = { Text("选择搜索引擎") },
            text = {
                Column {
                    SearchEngine.entries.forEach { engine ->
                        ListItem(
                            headlineContent = { Text(engine.displayName) },
                            leadingContent = {
                                RadioButton(
                                    selected = searchEngine == engine,
                                    onClick = {
                                        viewModel.setSearchEngine(engine)
                                        showEngineDialog = false
                                    },
                                )
                            },
                            modifier = Modifier.clickable {
                                viewModel.setSearchEngine(engine)
                                showEngineDialog = false
                            },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEngineDialog = false }) {
                    Text("取消")
                }
            },
        )
    }

    // Clear data dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清除所有数据") },
            text = { Text("确定要删除所有书签和历史记录吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllData()
                    showClearDialog = false
                }) {
                    Text("清除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
    )
    content()
}
