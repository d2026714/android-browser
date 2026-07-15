package com.example.browser.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class TabInfo(val title: String, val url: String)

@Composable
fun BottomTabBar(
    tabs: List<TabInfo>,
    activeIndex: Int,
    onTabClick: (Int) -> Unit,
    onAddTab: () -> Unit,
    onCloseTab: (Int) -> Unit,
    onHome: () -> Unit,
) {
    Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onHome, Modifier.size(36.dp)) {
                Icon(Icons.Default.Home, "首页", Modifier.size(20.dp))
            }
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                tabs.forEachIndexed { i, tab ->
                    val active = i == activeIndex
                    Surface(
                        Modifier.weight(1f).clip(RoundedCornerShape(6.dp)).clickable { onTabClick(i) },
                        shape = RoundedCornerShape(6.dp),
                        color = if (active) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Row(Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(tab.title.take(12), Modifier.weight(1f),
                                style = MaterialTheme.typography.labelSmall, maxLines = 1,
                                overflow = TextOverflow.Ellipsis, fontSize = 10.sp,
                                color = if (active) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant)
                            Icon(Icons.Default.Close, "关闭",
                                Modifier.size(14.dp).clickable { onCloseTab(i) },
                                tint = if (active) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            IconButton(onClick = onAddTab, Modifier.size(36.dp)) {
                Icon(Icons.Default.Add, "新标签", Modifier.size(20.dp))
            }
        }
    }
}
