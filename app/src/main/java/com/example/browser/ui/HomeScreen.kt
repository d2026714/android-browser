package com.example.browser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
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
    val initial: String,
)

val defaultQuickLinks = listOf(
    QuickLink("百度", "https://www.baidu.com", Color(0xFF2932E1), "百"),
    QuickLink("微博", "https://weibo.com", Color(0xFFE6162D), "微"),
    QuickLink("知乎", "https://www.zhihu.com", Color(0xFF0066FF), "知"),
    QuickLink("B站", "https://www.bilibili.com", Color(0xFFFB7299), "B"),
    QuickLink("淘宝", "https://www.taobao.com", Color(0xFFFF5000), "淘"),
    QuickLink("抖音", "https://www.douyin.com", Color(0xFF000000), "抖"),
    QuickLink("GitHub", "https://github.com", Color(0xFF24292E), "G"),
    QuickLink("Google", "https://www.google.com", Color(0xFF4285F4), "G"),
)

@Composable
fun HomeScreen(
    viewModel: BrowserViewModel,
    onNavigateToBookmarks: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val searchEngine by viewModel.searchEngine.collectAsState()
    var searchText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        // App title
        Text(
            text = "浏览器",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Light,
            ),
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Search bar
        Surface(
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 2.dp,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("搜索或输入网址") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(
                    onGo = {
                        if (searchText.isNotBlank()) {
                            viewModel.loadUrl(searchText.toSearchUrl(searchEngine.baseUrl))
                        }
                    },
                ),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search engine chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SearchEngine.entries.forEach { engine ->
                val isSelected = searchEngine == engine
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setSearchEngine(engine) },
                    label = { Text(engine.displayName, fontSize = 11.sp) },
                    modifier = Modifier.padding(horizontal = 3.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Quick links
        Text(
            text = "快捷访问",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(defaultQuickLinks) { link ->
                QuickLinkItem(link = link) {
                    viewModel.loadUrl(link.url)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Bottom shortcuts
        Surface(
            tonalElevation = 1.dp,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                BottomShortcut(
                    icon = Icons.Default.Bookmark,
                    label = "书签",
                    onClick = onNavigateToBookmarks,
                )
                BottomShortcut(
                    icon = Icons.Default.History,
                    label = "历史",
                    onClick = onNavigateToHistory,
                )
                BottomShortcut(
                    icon = Icons.Default.Settings,
                    label = "设置",
                    onClick = onNavigateToSettings,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun QuickLinkItem(link: QuickLink, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(link.color),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = link.initial,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = link.name,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurface,
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
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        Icon(
            icon,
            contentDescription = label,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
