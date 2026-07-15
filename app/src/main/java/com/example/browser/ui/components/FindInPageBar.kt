package com.example.browser.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
fun FindInPageBar(
    visible: Boolean,
    onSearch: (String) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onClose: () -> Unit,
) {
    AnimatedVisibility(visible, enter = slideInVertically { -it }, exit = slideOutVertically { -it }) {
        var query by remember { mutableStateOf("") }
        Surface(tonalElevation = 2.dp, shadowElevation = 4.dp) {
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(query, { query = it }, Modifier.weight(1f),
                    placeholder = { Text("在页面中查找") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch(query) }))
                IconButton(onClick = onPrevious) { Icon(Icons.Default.KeyboardArrowUp, "上一个") }
                IconButton(onClick = onNext) { Icon(Icons.Default.KeyboardArrowDown, "下一个") }
                IconButton(onClick = { query = ""; onClose() }) { Icon(Icons.Default.Close, "关闭") }
            }
        }
    }
}
