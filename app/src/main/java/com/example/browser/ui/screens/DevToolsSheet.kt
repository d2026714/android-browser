package com.example.browser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class LogLevel { ERROR, WARN, INFO, DEBUG }

data class ConsoleLog(
    val level: LogLevel,
    val message: String,
    val timestamp: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevToolsSheet(
    onDismiss: () -> Unit
) {
    var logs by remember {
        mutableStateOf(
            listOf(
                ConsoleLog(LogLevel.ERROR, "Uncaught TypeError: Cannot read property 'x' of undefined", "12:01:03"),
                ConsoleLog(LogLevel.WARN, "Resource interpreted as stylesheet but transferred with MIME type text/html", "12:01:05"),
                ConsoleLog(LogLevel.INFO, "DOM fully loaded and parsed", "12:01:02"),
                ConsoleLog(LogLevel.DEBUG, "Service worker registered", "12:01:06"),
                ConsoleLog(LogLevel.INFO, "WebSocket connection established", "12:01:07"),
                ConsoleLog(LogLevel.ERROR, "Failed to load resource: net::ERR_CONNECTION_REFUSED", "12:01:10"),
                ConsoleLog(LogLevel.WARN, "Deprecated API usage detected", "12:01:12")
            )
        )
    }
    var showPageSource by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Developer Tools",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !showPageSource,
                    onClick = { showPageSource = false },
                    label = { Text("Console") }
                )
                FilterChip(
                    selected = showPageSource,
                    onClick = { showPageSource = true },
                    label = { Text("Page Source") }
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { logs = emptyList() }) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear logs")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (showPageSource) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    LazyColumn(modifier = Modifier.padding(12.dp)) {
                        item {
                            Text(
                                text = """<!DOCTYPE html>
<html>
<head>
  <title>Example Page</title>
  <meta charset="UTF-8">
</head>
<body>
  <h1>Hello World</h1>
  <p>Sample page source content</p>
</body>
</html>""",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    if (logs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No console logs",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.padding(8.dp)) {
                            items(logs) { log ->
                                ConsoleLogRow(log)
                                Divider(modifier = Modifier)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ConsoleLogRow(log: ConsoleLog) {
    val (color, icon) = when (log.level) {
        LogLevel.ERROR -> Color(0xFFD32F2F) to Icons.Default.Close
        LogLevel.WARN -> Color(0xFFFFA000) to Icons.Default.Warning
        LogLevel.INFO -> Color(0xFF1976D2) to Icons.Default.Info
        LogLevel.DEBUG -> Color(0xFF757575) to Icons.Default.Info
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = log.level.name,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = log.timestamp,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(56.dp)
        )
        Text(
            text = log.message,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = color,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
