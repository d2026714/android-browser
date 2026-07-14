package com.example.browser.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.browser.R
import com.example.browser.ui.viewmodel.BrowserViewModel

data class UserAgentOption(val name: String, val description: String, val ua: String?)

val userAgents = listOf(
    UserAgentOption("Default", "Standard Android browser", null),
    UserAgentOption("Desktop (Chrome)", "Windows Chrome 120", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"),
    UserAgentOption("Desktop (Firefox)", "Windows Firefox 121", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0"),
    UserAgentOption("Desktop (Safari)", "macOS Safari 17", "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_2) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Safari/605.1.15"),
    UserAgentOption("iPhone", "Safari on iPhone", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1"),
    UserAgentOption("iPad", "Safari on iPad", "Mozilla/5.0 (iPad; CPU OS 17_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1"),
    UserAgentOption("Googlebot", "Google crawler", "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)"),
    UserAgentOption("Custom", "Enter your own User-Agent", "CUSTOM"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserAgentSheet(
    viewModel: BrowserViewModel,
    onDismiss: () -> Unit
) {
    var selectedIndex by remember { mutableStateOf(0) }
    var customUA by remember { mutableStateOf("") }
    var showCustomInput by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text(stringResource(R.string.user_agent), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))

            userAgents.forEachIndexed { index, ua ->
                ListItem(
                    headlineContent = { Text(ua.name) },
                    supportingContent = { Text(ua.description, maxLines = 1) },
                    leadingContent = {
                        RadioButton(selected = selectedIndex == index, onClick = {
                            selectedIndex = index
                            if (ua.ua == "CUSTOM") { showCustomInput = true }
                            else { viewModel.setUserAgent(ua.ua); onDismiss() }
                        })
                    },
                    modifier = Modifier.clickable {
                        selectedIndex = index
                        if (ua.ua == "CUSTOM") { showCustomInput = true }
                        else { viewModel.setUserAgent(ua.ua); onDismiss() }
                    }
                )
            }

            if (showCustomInput) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = customUA,
                    onValueChange = { customUA = it },
                    label = { Text(stringResource(R.string.custom_user_agent)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    if (customUA.isNotBlank()) { viewModel.setUserAgent(customUA); onDismiss() }
                }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.apply)) }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
