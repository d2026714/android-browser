package com.example.browser.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.browser.util.SearchEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: BrowserViewModel, onBack: () -> Unit) {
    val adBlock by vm.adBlock.collectAsState()
    val dark by vm.darkMode.collectAsState()
    val engine by vm.searchEngine.collectAsState()
    val fs by vm.fontSize.collectAsState()
    var showEngine by remember { mutableStateOf(false) }
    var showClear by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("设置") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") } })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Section("搜索引擎") { OutlinedCard(Modifier.fillMaxWidth().clickable { showEngine = true }) { ListItem({ Text(engine.displayName) }, supportingContent = { Text("点击切换") }, leadingContent = { Icon(Icons.Default.Search, null) }) } }
            Section("广告拦截") { Card(Modifier.fillMaxWidth()) { ListItem({ Text("广告拦截器") }, supportingContent = { Text("拦截页面广告") }, leadingContent = { Icon(Icons.Default.Block, null) }, trailingContent = { Switch(adBlock, { vm.setAdBlock(it) }) }) } }
            Section("外观") { Card(Modifier.fillMaxWidth()) { ListItem({ Text("深色模式") }, supportingContent = { Text("手动切换") }, leadingContent = { Icon(Icons.Default.DarkMode, null) }, trailingContent = { Switch(dark, { vm.setDarkMode(it) }) }) } }
            Section("字体大小") { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("当前: ${fs}%"); Slider(fs.toFloat(), { vm.setFontSize(it.toInt()) }, valueRange = 50f..200f, steps = 5); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("小", style = MaterialTheme.typography.labelSmall); Text("标准", style = MaterialTheme.typography.labelSmall); Text("大", style = MaterialTheme.typography.labelSmall) } } } }
            Section("数据") { OutlinedCard(Modifier.fillMaxWidth().clickable { showClear = true }) { ListItem({ Text("清除所有数据") }, supportingContent = { Text("删除书签和历史") }, leadingContent = { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) }) } }
            Section("关于") { Card(Modifier.fillMaxWidth()) { ListItem({ Text("浏览器") }, supportingContent = { Text("v1.0.0 · Kotlin + WebView + Compose") }, leadingContent = { Icon(Icons.Default.Info, null) }) } }
            Spacer(Modifier.height(32.dp))
        }
    }
    if (showEngine) AlertDialog(onDismissRequest = { showEngine = false }, title = { Text("搜索引擎") },
        text = { Column { SearchEngine.entries.forEach { e -> ListItem({ Text(e.displayName) }, leadingContent = { RadioButton(engine == e, { vm.setEngine(e); showEngine = false }) }, modifier = Modifier.clickable { vm.setEngine(e); showEngine = false }) } } },
        confirmButton = { TextButton(onClick = { showEngine = false }) { Text("取消") } })
    if (showClear) AlertDialog(onDismissRequest = { showClear = false }, title = { Text("清除数据") }, text = { Text("确定删除所有书签和历史？") },
        confirmButton = { TextButton(onClick = { vm.clearAllData(); showClear = false }) { Text("清除", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { showClear = false }) { Text("取消") } })
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)); content()
}
