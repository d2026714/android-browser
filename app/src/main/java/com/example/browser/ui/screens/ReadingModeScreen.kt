package com.example.browser.ui.screens

import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.browser.R
import com.example.browser.ui.viewmodel.BrowserViewModel
import kotlin.math.roundToInt

/** Background color presets for reading mode */
enum class ReadingBackground(val label: String, val bg: Color, val fg: Color) {
    WHITE("White", Color(0xFFFFFFFF), Color(0xFF1A1A1A)),
    SEPIA("Sepia", Color(0xFFF5ECD7), Color(0xFF3E2C1C)),
    DARK("Dark", Color(0xFF1A1A1A), Color(0xFFD4D4D4))
}

/** Scroll position memory: url -> scroll fraction (0..1) */
private val scrollMemory = mutableMapOf<String, Float>()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingModeScreen(
    viewModel: BrowserViewModel,
    onDismiss: () -> Unit
) {
    val currentUrl by viewModel.currentUrl.collectAsState()
    val currentTitle by viewModel.currentTitle.collectAsState()

    var extractedHtml by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var fontSize by remember { mutableFloatStateOf(18f) }
    var background by remember { mutableStateOf(ReadingBackground.SEPIA) }
    var showControls by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    // Word count & reading time
    val plainText = remember(extractedHtml) {
        extractedHtml.replace(Regex("<[^>]*>"), " ").replace(Regex("\\s+"), " ").trim()
    }
    val wordCount = remember(plainText) {
        if (plainText.isEmpty()) 0 else plainText.split(Regex("\\s+")).size
    }
    val readingTimeMin = remember(wordCount) { maxOf(1, (wordCount / 238f).roundToInt()) }

    // Scroll progress
    val scrollProgress = remember(scrollState.maxValue) {
        derivedStateOf {
            if (scrollState.maxValue == 0) 0f
            else scrollState.value.toFloat() / scrollState.maxValue
        }
    }

    // Extract content via JS when screen opens
    LaunchedEffect(currentUrl) {
        isLoading = true
        val webView = viewModel.getActiveWebView()
        if (webView != null) {
            extractArticleContent(webView) { html ->
                extractedHtml = if (html.isNullOrBlank() || html == "null") {
                    "<p>No readable content found on this page.</p>"
                } else {
                    html
                }
                isLoading = false
            }
        } else {
            // Fallback: show title and URL
            extractedHtml = "<h1>$currentTitle</h1><p>Content could not be extracted. " +
                "Navigate to a page and try again.</p>"
            isLoading = false
        }
    }

    // Restore scroll position
    LaunchedEffect(extractedHtml, isLoading) {
        if (!isLoading && extractedHtml.isNotEmpty()) {
            val savedFraction = scrollMemory[currentUrl] ?: 0f
            if (savedFraction > 0f && scrollState.maxValue > 0) {
                scrollState.scrollTo((savedFraction * scrollState.maxValue).toInt())
            }
        }
    }

    // Save scroll position on dispose
    DisposableEffect(currentUrl) {
        onDispose {
            if (scrollState.maxValue > 0) {
                scrollMemory[currentUrl] = scrollState.value.toFloat() / scrollState.maxValue
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentTitle,
                            maxLines = 1,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (wordCount > 0) {
                            Text(
                                text = "$wordCount words · ~${readingTimeMin} min read",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { showControls = !showControls }) {
                        Icon(Icons.Default.TextFormat, "Reading settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = background.bg,
                    titleContentColor = background.fg,
                    navigationIconContentColor = background.fg,
                    actionIconContentColor = background.fg
                )
            )
        },
        containerColor = background.bg
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = background.fg.copy(alpha = 0.6f))
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(background.bg)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    // Article title
                    Text(
                        text = currentTitle,
                        fontSize = (fontSize + 8).sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        lineHeight = ((fontSize + 8) * 1.3).sp,
                        color = background.fg,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Reading time badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = background.fg.copy(alpha = 0.08f),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = "~${readingTimeMin} min read · $wordCount words",
                            style = MaterialTheme.typography.bodySmall,
                            color = background.fg.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    Divider(
                        modifier = Modifier.padding(bottom = 20.dp),
                        color = background.fg.copy(alpha = 0.15f)
                    )

                    // Rendered HTML content
                    Text(
                        text = plainText,
                        fontSize = fontSize.sp,
                        fontFamily = FontFamily.Serif,
                        lineHeight = (fontSize * 1.7).sp,
                        color = background.fg,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(48.dp))
                }
            }

            // Progress indicator bar at top
            if (!isLoading && scrollProgress.value > 0f) {
                LinearProgressIndicator(
                    progress = { scrollProgress.value },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            }

            // Settings panel (slides up)
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                ReadingControlsPanel(
                    fontSize = fontSize,
                    onFontSizeChange = { fontSize = it },
                    background = background,
                    onBackgroundChange = { background = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            background.bg.copy(alpha = 0.95f),
                            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                        )
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ReadingControlsPanel(
    fontSize: Float,
    onFontSizeChange: (Float) -> Unit,
    background: ReadingBackground,
    onBackgroundChange: (ReadingBackground) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Font size control
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "A",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = background.fg.copy(alpha = 0.6f)
            )
            Slider(
                value = fontSize,
                onValueChange = onFontSizeChange,
                valueRange = 12f..28f,
                steps = 7,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
            Text(
                text = "A",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = background.fg.copy(alpha = 0.6f)
            )
            Text(
                text = " ${fontSize.roundToInt()}sp",
                fontSize = 12.sp,
                color = background.fg.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Background color selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            ReadingBackground.entries.forEach { bg ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onBackgroundChange(bg) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(bg.bg)
                            .then(
                                if (bg == background) {
                                    Modifier.padding(2.dp)
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (bg == background) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = bg.fg,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(bg.fg.copy(alpha = 0.2f))
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = bg.label,
                        fontSize = 11.sp,
                        color = background.fg.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

/**
 * JavaScript to extract article content from the page.
 * Uses a readability-style algorithm: finds the main content container
 * and extracts clean text.
 */
private const val EXTRACT_ARTICLE_JS = """
(function() {
    // Remove unwanted elements
    var removeSelectors = [
        'script', 'style', 'nav', 'footer', 'header',
        'iframe', 'noscript', '.ad', '.ads', '.advertisement',
        '.sidebar', '.menu', '.navigation', '.social',
        '.related', '.comments', '.comment', '.share',
        '.cookie', '.popup', '.modal', '.overlay',
        '[role="banner"]', '[role="navigation"]', '[role="complementary"]',
        '[role="contentinfo"]', '[aria-hidden="true"]'
    ];
    removeSelectors.forEach(function(sel) {
        try {
            document.querySelectorAll(sel).forEach(function(el) {
                el.remove();
            });
        } catch(e) {}
    });

    // Try to find main content
    var content = document.querySelector('article')
        || document.querySelector('[role="main"]')
        || document.querySelector('main')
        || document.querySelector('.article-content')
        || document.querySelector('.article-body')
        || document.querySelector('.post-content')
        || document.querySelector('.entry-content')
        || document.querySelector('.content')
        || document.querySelector('.post')
        || document.querySelector('.article')
        || document.querySelector('.story-body')
        || document.querySelector('.mw-parser-output')
        || document.body;

    if (content) {
        // Get text content, preserving paragraph structure
        var result = '';
        var elements = content.querySelectorAll('h1, h2, h3, h4, h5, h6, p, li, blockquote, pre');
        if (elements.length > 0) {
            elements.forEach(function(el) {
                var text = el.innerText.trim();
                if (text.length > 0) {
                    result += text + '\\n\\n';
                }
            });
        } else {
            result = content.innerText || content.textContent;
        }
        result = result.replace(/\\n{3,}/g, '\\n\\n').trim();
        return result;
    }
    return 'No readable content found.';
})()
"""

/**
 * Extract article content from the WebView using JavaScript.
 */
private fun extractArticleContent(webView: WebView, onResult: (String?) -> Unit) {
    try {
        webView.evaluateJavascript(EXTRACT_ARTICLE_JS) { result ->
            val cleaned = result
                ?.removeSurrounding("\"")
                ?.replace("\\n", "\n")
                ?.replace("\\\"", "\"")
                ?.replace("\\t", "\t")
                ?.replace("\\u003C", "<")
                ?.replace("\\/", "/")
            onResult(cleaned)
        }
    } catch (e: Exception) {
        onResult(null)
    }
}
