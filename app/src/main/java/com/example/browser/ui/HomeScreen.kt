package com.example.browser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.browser.util.SearchEngine
import com.example.browser.util.toSearchUrl

data class QuickLink(
    val name: String,
    val url: String,
    val color: Color,
)

val quickLinks = listOf(
    QuickLink("百度", "https://www.baidu.com", Color(0xFF2932E1)),
    QuickLink("微博", "https://weibo.com", Color(0xFFE6162D)),
    QuickLink("知乎", "https://www.zhihu.com", Color(0xFF0066FF)),
    QuickLink("B站", "https://www.bilibili.com", Color(0xFFFB7299)),
    QuickLink("淘宝", "https://www.taobao.com", Color(0xFFFF5000)),
    QuickLink("抖音", "https://www.douyin.com", Color(0xFF000000)),
    QuickLink("GitHub", "https://github.com", Color(0xFF24292E)),
    QuickLink("Google", "https://www.google.com", Color(0xFF4285F4)),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: BrowserViewModel) {
    val searchEngine by viewModel.searchEngine.collectAsState()
    var searchText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // App title
        Text(
            text = "浏览器",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Search bar
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("搜索或输入网址") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(
                onGo = {
                    if (searchText.isNotBlank()) {
                        viewModel.loadUrl(searchText.toSearchUrl(searchEngine.baseUrl))
                    }
                }
            ),
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Search engine selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SearchEngine.entries.forEach { engine ->
                FilterChip(
                    selected = searchEngine == engine,
                    onClick = { viewModel.setSearchEngine(engine) },
                    label = { Text(engine.displayName, fontSize = 11.sp) },
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quick links
        Text(
            text = "快捷访问",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(quickLinks) { link ->
                QuickLinkItem(link = link) {
                    viewModel.loadUrl(link.url)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Bottom shortcuts
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            BottomShortcut(icon = Icons.Default.Bookmark, label = "书签") {
                viewModel.showBookmarksScreen()
            }
            BottomShortcut(icon = Icons.Default.History, label = "历史") {
                viewModel.showHistoryScreen()
            }
            BottomShortcut(icon = Icons.Default.Settings, label = "设置") {
                viewModel.showSettingsScreen()
            }
        }
    }
}

@Composable
private fun QuickLinkItem(link: QuickLink, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(link.color),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = link.name.first().toString(),
                color = Color.White,
                fontSize = 18.sp,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = link.name,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun BottomShortcut(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(24.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
