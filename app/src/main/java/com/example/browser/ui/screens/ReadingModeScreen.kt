package com.example.browser.ui.screens

import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.browser.ui.viewmodel.BrowserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingModeScreen(
    viewModel: BrowserViewModel,
    onDismiss: () -> Unit
) {
    val currentUrl by viewModel.currentUrl.collectAsState()
    val currentTitle by viewModel.currentTitle.collectAsState()
    var fontSize by remember { mutableStateOf(18) }
    var extractedText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current

    // Extract text content from the page
    LaunchedEffect(currentUrl) {
        isLoading = true
        // We'll extract text via JavaScript injection in the WebView
        // For now, show a placeholder with the title
        extractedText = "Loading content from:\n$currentTitle\n\n$url"
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reading Mode") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Font size controls
                    IconButton(onClick = { if (fontSize > 12) fontSize-- }) {
                        Icon(Icons.Default.TextDecrease, "Decrease font")
                    }
                    Text(
                        text = "${fontSize}sp",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    IconButton(onClick = { if (fontSize < 32) fontSize++ }) {
                        Icon(Icons.Default.TextIncrease, "Increase font")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // Title
                Text(
                    text = currentTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                HorizontalDivider(
                    modifier = Modifier.padding(bottom = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // Content
                Text(
                    text = extractedText,
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * 1.6).sp,
                    fontFamily = FontFamily.Serif,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

/**
 * JavaScript to extract readable text from a page.
 * Strips scripts, styles, nav, footer, ads, etc.
 */
const val READER_MODE_JS = """
(function() {
    // Remove non-content elements
    var removeSelectors = [
        'script', 'style', 'nav', 'footer', 'header',
        'iframe', 'noscript', '.ad', '.ads', '.advertisement',
        '.sidebar', '.menu', '.navigation', '.social',
        '[role="banner"]', '[role="navigation"]', '[role="complementary"]'
    ];
    removeSelectors.forEach(function(sel) {
        document.querySelectorAll(sel).forEach(function(el) {
            el.remove();
        });
    });

    // Try to find the main content
    var content = document.querySelector('article')
        || document.querySelector('[role="main"]')
        || document.querySelector('main')
        || document.querySelector('.content')
        || document.querySelector('.post')
        || document.querySelector('.article')
        || document.body;

    if (content) {
        // Clean up the content
        var text = content.innerText || content.textContent;
        // Remove excessive whitespace
        text = text.replace(/\n{3,}/g, '\n\n').trim();
        return text;
    }
    return document.body.innerText || 'No readable content found.';
})()
"""
