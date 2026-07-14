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
import androidx.compose.ui.unit.dp
import com.example.browser.translator.TranslationManager
import com.example.browser.ui.viewmodel.BrowserViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationSettingsScreen(
    viewModel: BrowserViewModel,
    onDismiss: () -> Unit
) {
    val translationManager = viewModel.translationManager
    val modelStatuses by translationManager.modelStatuses.collectAsState()
    val downloadProgress by translationManager.downloadProgress.collectAsState()
    val scope = rememberCoroutineScope()

    var sourceLang by remember { mutableStateOf("zh") }
    var targetLang by remember { mutableStateOf("en") }
    var sourceExpanded by remember { mutableStateOf(false) }
    var targetExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Offline Translation",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                "Download language models for offline translation. Each model uses ~30MB of storage.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Source language selector
            Text(
                "Source Language",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            ExposedDropdownMenuBox(
                expanded = sourceExpanded,
                onExpandedChange = { sourceExpanded = it }
            ) {
                OutlinedTextField(
                    value = translationManager.getLanguageName(sourceLang),
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sourceExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = sourceExpanded,
                    onDismissRequest = { sourceExpanded = false }
                ) {
                    translationManager.supportedLanguages.forEach { lang ->
                        DropdownMenuItem(
                            text = { Text(lang.displayName) },
                            onClick = {
                                sourceLang = lang.code
                                sourceExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Target language selector
            Text(
                "Target Language",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            ExposedDropdownMenuBox(
                expanded = targetExpanded,
                onExpandedChange = { targetExpanded = it }
            ) {
                OutlinedTextField(
                    value = translationManager.getLanguageName(targetLang),
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = targetExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = targetExpanded,
                    onDismissRequest = { targetExpanded = false }
                ) {
                    translationManager.supportedLanguages.forEach { lang ->
                        DropdownMenuItem(
                            text = { Text(lang.displayName) },
                            onClick = {
                                targetLang = lang.code
                                targetExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Download buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            translationManager.downloadModel(sourceLang).collect {}
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = modelStatuses[sourceLang] != TranslationManager.ModelStatus.DOWNLOADING
                ) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Source", maxLines = 1)
                }
                Button(
                    onClick = {
                        scope.launch {
                            translationManager.downloadModel(targetLang).collect {}
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = modelStatuses[targetLang] != TranslationManager.ModelStatus.DOWNLOADING
                ) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Target", maxLines = 1)
                }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            translationManager.downloadModels(listOf(sourceLang, targetLang)).collect {}
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = modelStatuses[sourceLang] != TranslationManager.ModelStatus.DOWNLOADING &&
                            modelStatuses[targetLang] != TranslationManager.ModelStatus.DOWNLOADING
                ) {
                    Icon(Icons.Default.DownloadDone, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Both", maxLines = 1)
                }
            }

            // Download progress indicator
            downloadProgress?.let { progress ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Downloading ${translationManager.getLanguageName(progress.languageCode)}...",
                    style = MaterialTheme.typography.bodySmall
                )
                LinearProgressIndicator(
                    progress = { progress.progress },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                )
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // Downloaded models management
            Text(
                "Downloaded Models",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val downloadedLanguages = translationManager.supportedLanguages.filter {
                modelStatuses[it.code] == TranslationManager.ModelStatus.DOWNLOADED
            }

            if (downloadedLanguages.isEmpty()) {
                Text(
                    "No models downloaded yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                downloadedLanguages.forEach { lang ->
                    ListItem(
                        headlineContent = { Text(lang.displayName) },
                        supportingContent = { Text(lang.code) },
                        leadingContent = {
                            Icon(
                                Icons.Default.CheckCircle,
                                null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            IconButton(onClick = {
                                scope.launch {
                                    translationManager.deleteModel(lang.code)
                                }
                            }) {
                                Icon(
                                    Icons.Default.Delete,
                                    "Delete model",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // All models status
            Text(
                "All Languages",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            translationManager.supportedLanguages.forEach { lang ->
                val status = modelStatuses[lang.code]
                val isDownloading = status == TranslationManager.ModelStatus.DOWNLOADING
                ListItem(
                    headlineContent = { Text(lang.displayName) },
                    leadingContent = {
                        when (status) {
                            TranslationManager.ModelStatus.DOWNLOADED ->
                                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                            TranslationManager.ModelStatus.DOWNLOADING ->
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            TranslationManager.ModelStatus.ERROR ->
                                Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                            else ->
                                Icon(Icons.Default.CloudDownload, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        }
                    },
                    trailingContent = {
                        if (status != TranslationManager.ModelStatus.DOWNLOADED) {
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        translationManager.downloadModel(lang.code).collect {}
                                    }
                                },
                                enabled = !isDownloading
                            ) { Text("Download") }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
