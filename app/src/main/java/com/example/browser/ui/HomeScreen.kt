package com.example.browser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.browser.util.SearchEngine
import com.example.browser.util.toSearchUrl

private data class QL(val name: String, val url: String, val color: Color, val ch: String)
private val links = listOf(
    QL("百度", "https://www.baidu.com", Color(0xFF2932E1), "百"),
    QL("微博", "https://weibo.com", Color(0xFFE6162D), "微"),
    QL("知乎", "https://www.zhihu.com", Color(0xFF0066FF), "知"),
    QL("B站", "https://www.bilibili.com", Color(0xFFFB7299), "B"),
    QL("起点", "https://www.qidian.com", Color(0xFFE4393C), "起"),
    QL("番茄", "https://fanqienovel.com", Color(0xFFFF6B6B), "番"),
    QL("GitHub", "https://github.com", Color(0xFF24292E), "G"),
    QL("Google", "https://www.google.com", Color(0xFF4285F4), "G"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: BrowserViewModel,
    onBookmarks: () -> Unit, onHistory: () -> Unit, onSettings: () -> Unit, onBookshelf: () -> Unit,
) {
    val engine by vm.searchEngine.collectAsState()
    val suggs by vm.suggestions.collectAsState()
    var text by remember { mutableStateOf("") }
    var showSugg by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {

        Spacer(Modifier.height(80.dp))
        Text("浏览器", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Light),
            color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(32.dp))

        // Search bar
        Column {
            Surface(RoundedCornerShape(28.dp), tonalElevation = 2.dp, shadowElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(text, {
                    text = it
                    if (it.isNotBlank()) { vm.updateSuggestions(it); showSugg = true } else showSugg = false
                }, Modifier.fillMaxWidth(), placeholder = { Text("搜索或输入网址") },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingIcon = { if (text.isNotEmpty()) IconButton(onClick = { text = ""; showSugg = false; vm.clearSuggestions() }) { Icon(Icons.Default.Close, "清除") } },
                    singleLine = true, shape = RoundedCornerShape(28.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { if (text.isNotBlank()) { vm.loadUrl(text.toSearchUrl(engine.baseUrl)); showSugg = false } }))
            }
            if (showSugg && suggs.isNotEmpty()) {
                Surface(RoundedCornerShape(12.dp), tonalElevation = 4.dp, shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
                    LazyColumn(Modifier.heightIn(max = 200.dp)) {
                        items(suggs.take(6)) { s ->
                            ListItem(headlineContent = { Text(s) }, leadingContent = {
                                Icon(Icons.Default.Search, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }, modifier = Modifier.clickable { text = s; vm.loadUrl(s.toSearchUrl(engine.baseUrl)); showSugg = false })
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            SearchEngine.entries.forEach { e ->
                FilterChip(selected = engine == e, onClick = { vm.setEngine(e) }, label = { Text(e.displayName, fontSize = 11.sp) },
                    modifier = Modifier.padding(horizontal = 3.dp))
            }
        }

        Spacer(Modifier.height(32.dp))
        Text("快捷访问", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))

        LazyVerticalGrid(GridCells.Fixed(4), Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(links) { l ->
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { vm.loadUrl(l.url) }.padding(4.dp)) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(l.color), contentAlignment = Alignment.Center) {
                        Text(l.ch, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(l.name, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 1)
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Surface(tonalElevation = 1.dp, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf(Icons.Default.Bookmark to "书签" to onBookmarks,
                    Icons.Default.History to "历史" to onHistory,
                    Icons.Default.MenuBook to "书架" to onBookshelf,
                    Icons.Default.Settings to "设置" to onSettings,
                ).forEach { (pair, action) ->
                    val (icon, label) = pair
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = action).padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Icon(icon, label, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(4.dp))
                        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
