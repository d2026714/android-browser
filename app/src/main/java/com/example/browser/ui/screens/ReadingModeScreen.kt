package com.example.browser.ui.screens

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

    LaunchedEffect(currentUrl) {
        isLoading = true
        extractedText = "Loading content from:\n$currentTitle\n\n$currentUrl\n\n" +
            "Reading mode extracts the main text content from the page " +
            "and displays it in a clean, distraction-free format with " +
            "adjustable font size for comfortable reading.\n\n" +
            "To use reading mode on any page:\n" +
            "1. Navigate to the page you want to read\n" +
            "2. Tap the menu (⋮) button\n" +
            "3. Select 'Reading Mode'\n\n" +
            "The page content will be reformatted for easy reading " +
            "with a serif font and optimized line spacing."
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
                    IconButton(onClick = { if (fontSize > 12) fontSize-- }) {
                        Icon(Icons.Default.Remove, "Decrease font")
                    }
                    Text(
                        text = "${fontSize}sp",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    IconButton(onClick = { if (fontSize < 32) fontSize++ }) {
                        Icon(Icons.Default.Add, "Increase font")
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

const val READER_MODE_JS = """
(function() {
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
    var content = document.querySelector('article')
        || document.querySelector('[role="main"]')
        || document.querySelector('main')
        || document.querySelector('.content')
        || document.querySelector('.post')
        || document.querySelector('.article')
        || document.body;
    if (content) {
        var text = content.innerText || content.textContent;
        text = text.replace(/\n{3,}/g, '\n\n').trim();
        return text;
    }
    return document.body.innerText || 'No readable content found.';
})()
"""
