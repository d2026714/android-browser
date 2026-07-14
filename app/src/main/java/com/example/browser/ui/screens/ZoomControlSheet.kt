package com.example.browser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.browser.ui.viewmodel.BrowserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZoomControlSheet(
    viewModel: BrowserViewModel,
    onDismiss: () -> Unit
) {
    var zoomLevel by remember { mutableStateOf(100) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text("Zoom Control", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))

            // Zoom slider
            Text("Page Zoom: ${zoomLevel}%", style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (zoomLevel > 25) zoomLevel -= 25 }) {
                    Icon(Icons.Default.Remove, "Zoom out")
                }
                Slider(
                    value = zoomLevel.toFloat(),
                    onValueChange = { zoomLevel = it.toInt() },
                    valueRange = 25f..300f,
                    steps = 10,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { if (zoomLevel < 300) zoomLevel += 25 }) {
                    Icon(Icons.Default.Add, "Zoom in")
                }
            }

            // Preset buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf(50, 75, 100, 125, 150, 200).forEach { preset ->
                    FilterChip(
                        selected = zoomLevel == preset,
                        onClick = { zoomLevel = preset },
                        label = { Text("${preset}%") }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Apply button
            Button(
                onClick = { viewModel.setZoomLevel(zoomLevel); onDismiss() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Apply Zoom") }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
