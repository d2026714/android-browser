package com.example.browser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class ProxyType { HTTP, HTTPS, SOCKS5 }

data class ProxyConfig(
    val id: Int,
    val name: String,
    val host: String,
    val port: Int,
    val type: ProxyType,
    val isActive: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProxySheet(
    onDismiss: () -> Unit
) {
    var proxies by remember {
        mutableStateOf(
            listOf(
                ProxyConfig(1, "Work Proxy", "proxy.work.com", 8080, ProxyType.HTTP, true),
                ProxyConfig(2, "Home SOCKS", "127.0.0.1", 1080, ProxyType.SOCKS5, false)
            )
        )
    }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingProxy by remember { mutableStateOf<ProxyConfig?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Proxy Settings",
                    style = MaterialTheme.typography.titleLarge
                )
                FilledTonalIconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add proxy")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (proxies.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No proxy configurations",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(proxies, key = { it.id }) { proxy ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (proxy.isActive)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = proxy.name,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "${proxy.type.name} • ${proxy.host}:${proxy.port}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = proxy.isActive,
                                    onCheckedChange = { checked ->
                                        proxies = proxies.map {
                                            if (it.id == proxy.id) it.copy(isActive = checked)
                                            else it.copy(isActive = false)
                                        }
                                    }
                                )
                                IconButton(onClick = { editingProxy = proxy }) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        proxies = proxies.filter { it.id != proxy.id }
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                        Divider(modifier = Modifier)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showAddDialog) {
        ProxyDialog(
            title = "Add Proxy",
            onDismiss = { showAddDialog = false },
            onConfirm = { name, host, port, type ->
                val newId = (proxies.maxOfOrNull { it.id } ?: 0) + 1
                proxies = proxies + ProxyConfig(newId, name, host, port, type, false)
                showAddDialog = false
            }
        )
    }

    editingProxy?.let { proxy ->
        ProxyDialog(
            title = "Edit Proxy",
            initialName = proxy.name,
            initialHost = proxy.host,
            initialPort = proxy.port.toString(),
            initialType = proxy.type,
            onDismiss = { editingProxy = null },
            onConfirm = { name, host, port, type ->
                proxies = proxies.map {
                    if (it.id == proxy.id) it.copy(name = name, host = host, port = port, type = type)
                    else it
                }
                editingProxy = null
            }
        )
    }
}

@Composable
private fun ProxyDialog(
    title: String,
    initialName: String = "",
    initialHost: String = "",
    initialPort: String = "",
    initialType: ProxyType = ProxyType.HTTP,
    onDismiss: () -> Unit,
    onConfirm: (name: String, host: String, port: Int, type: ProxyType) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var host by remember { mutableStateOf(initialHost) }
    var port by remember { mutableStateOf(initialPort) }
    var selectedType by remember { mutableStateOf(initialType) }
    var typeExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("Host") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("Port") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedType.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        ProxyType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    selectedType = type
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val portInt = port.toIntOrNull() ?: 0
                    onConfirm(name, host, portInt, selectedType)
                },
                enabled = name.isNotBlank() && host.isNotBlank() && port.toIntOrNull() != null
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
