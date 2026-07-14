package com.example.browser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.browser.translator.TranslationManager
import com.example.browser.ui.viewmodel.BrowserViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslateScreen(
    viewModel: BrowserViewModel,
    onDismiss: () -> Unit
) {
    val translationManager = viewModel.translationManager
    val modelStatuses by translationManager.modelStatuses.collectAsState()
    val isTranslating by translationManager.isTranslating.collectAsState()
    val translateResult by viewModel.translationResult.collectAsState()
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var inputText by remember { mutableStateOf("") }
    var sourceLang by remember { mutableStateOf("auto") }
    var targetLang by remember { mutableStateOf("en") }
    var sourceExpanded by remember { mutableStateOf(false) }
    var targetExpanded by remember { mutableStateOf(false) }

    // Pre-fill if there's a pending translation text
    LaunchedEffect(Unit) {
        val pending = viewModel.pendingTranslateText.value
        if (!pending.isNullOrBlank()) {
            inputText = pending
            viewModel.clearPendingTranslateText()
            // Auto-translate
            scope.launch {
                translationManager.translateText(pending, sourceLang, targetLang)
                    .onSuccess { result ->
                        viewModel.setTranslationResult(result)
                    }
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Translate",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Language selectors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Source language
                ExposedDropdownMenuBox(
                    expanded = sourceExpanded,
                    onExpandedChange = { sourceExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = if (sourceLang == "auto") "Auto Detect"
                        else translationManager.getLanguageName(sourceLang),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("From") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sourceExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = sourceExpanded,
                        onDismissRequest = { sourceExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Auto Detect") },
                            onClick = { sourceLang = "auto"; sourceExpanded = false }
                        )
                        translationManager.supportedLanguages.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(lang.displayName) },
                                onClick = { sourceLang = lang.code; sourceExpanded = false }
                            )
                        }
                    }
                }

                // Swap button
                IconButton(
                    onClick = {
                        if (sourceLang != "auto") {
                            val temp = sourceLang
                            sourceLang = targetLang
                            targetLang = temp
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Icon(Icons.Default.SwapHoriz, "Swap languages")
                }

                // Target language
                ExposedDropdownMenuBox(
                    expanded = targetExpanded,
                    onExpandedChange = { targetExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = translationManager.getLanguageName(targetLang),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("To") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = targetExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = targetExpanded,
                        onDismissRequest = { targetExpanded = false }
                    ) {
                        translationManager.supportedLanguages.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(lang.displayName) },
                                onClick = { targetLang = lang.code; targetExpanded = false }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Input text field
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 200.dp),
                placeholder = { Text("Enter text to translate...") },
                label = { Text("Input") },
                maxLines = 8
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Translate button
            Button(
                onClick = {
                    if (inputText.isNotBlank()) {
                        scope.launch {
                            val effectiveSource = if (sourceLang == "auto") {
                                // Auto-detect: try to detect by using "auto" which ML Kit doesn't support,
                                // so we default to English as source for offline
                                "en"
                            } else sourceLang
                            translationManager.translateText(inputText, effectiveSource, targetLang)
                                .onSuccess { result ->
                                    viewModel.setTranslationResult(result)
                                }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = inputText.isNotBlank() && !isTranslating
            ) {
                if (isTranslating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Translate")
            }

            // Model status warning
            if (sourceLang != "auto") {
                val sourceReady = modelStatuses[sourceLang] == TranslationManager.ModelStatus.DOWNLOADED
                val targetReady = modelStatuses[targetLang] == TranslationManager.ModelStatus.DOWNLOADED
                if (!sourceReady || !targetReady) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    "Language model not downloaded",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                val missing = mutableListOf<String>()
                                if (!sourceReady) missing.add(translationManager.getLanguageName(sourceLang))
                                if (!targetReady) missing.add(translationManager.getLanguageName(targetLang))
                                Text(
                                    "Missing: ${missing.joinToString(", ")}. Go to Translation Settings to download.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            // Translation result
            translateResult?.let { result ->
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Result",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row {
                        IconButton(onClick = {
                            clipboardManager.setText(AnnotatedString(result.translatedText))
                        }) {
                            Icon(Icons.Default.ContentCopy, "Copy translation")
                        }
                        IconButton(onClick = {
                            // Use as new input
                            inputText = result.translatedText
                            viewModel.clearTranslationResult()
                        }) {
                            Icon(Icons.Default.Input, "Use as input")
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = result.translatedText,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${translationManager.getLanguageName(result.sourceLang)} → ${translationManager.getLanguageName(result.targetLang)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
