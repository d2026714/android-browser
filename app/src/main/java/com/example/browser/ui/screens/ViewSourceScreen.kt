package com.example.browser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.browser.R
import com.example.browser.ui.viewmodel.BrowserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewSourceScreen(
    viewModel: BrowserViewModel,
    onDismiss: () -> Unit
) {
    val pageSource by viewModel.pageSource.collectAsState()
    val currentTitle by viewModel.currentTitle.collectAsState()
    val hScroll = rememberScrollState()
    val vScroll = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.page_source), maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // Copy source to clipboard
                        pageSource?.let { source ->
                            val clipboard = viewModel // Will use copyLink-like logic
                        }
                    }) {
                        Icon(Icons.Default.ContentCopy, "Copy")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Info bar
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = currentTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    maxLines = 1
                )
            }

            // Source code
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(hScroll)
                    .verticalScroll(vScroll)
                    .padding(12.dp)
            ) {
                Text(
                    text = pageSource ?: stringResource(R.string.loading),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
