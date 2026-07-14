package com.example.browser.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.browser.R
import com.example.browser.ui.viewmodel.BrowserViewModel

data class SearchEngineOption(
    val name: String,
    val url: String,
    val icon: @Composable () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchEngineSheet(
    viewModel: BrowserViewModel,
    onDismiss: () -> Unit
) {
    val engines = listOf(
        SearchEngineOption("Google", "https://www.google.com/search?q=", {
            Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary)
        }),
        SearchEngineOption("Bing", "https://www.bing.com/search?q=", {
            Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.secondary)
        }),
        SearchEngineOption("DuckDuckGo", "https://duckduckgo.com/?q=", {
            Icon(Icons.Default.Shield, null, tint = MaterialTheme.colorScheme.tertiary)
        }),
        SearchEngineOption("Yahoo", "https://search.yahoo.com/search?p=", {
            Icon(Icons.Default.Language, null, tint = MaterialTheme.colorScheme.error)
        }),
        SearchEngineOption("Baidu", "https://www.baidu.com/s?wd=", {
            Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary)
        }),
        SearchEngineOption("Yandex", "https://yandex.com/search/?text=", {
            Icon(Icons.Default.Public, null, tint = MaterialTheme.colorScheme.secondary)
        })
    )

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.default_search_engine),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            engines.forEach { engine ->
                ListItem(
                    headlineContent = { Text(engine.name) },
                    leadingContent = { engine.icon() },
                    trailingContent = {
                        RadioButton(
                            selected = false, // Will be connected to ViewModel
                            onClick = {
                                viewModel.setSearchEngine(engine.url)
                                onDismiss()
                            }
                        )
                    },
                    modifier = Modifier.clickable {
                        viewModel.setSearchEngine(engine.url)
                        onDismiss()
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
