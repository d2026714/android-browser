package com.example.browser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.browser.R
import com.example.browser.ui.viewmodel.BrowserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomCssSheet(
    viewModel: BrowserViewModel,
    onDismiss: () -> Unit
) {
    var cssText by remember { mutableStateOf(viewModel.getCustomCss()) }
    var isEnabled by remember { mutableStateOf(viewModel.isCustomCssEnabled()) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.custom_css), style = MaterialTheme.typography.titleLarge)
                Switch(checked = isEnabled, onCheckedChange = { isEnabled = it; viewModel.toggleCustomCss() })
            }
            Text("Inject custom styles into web pages", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.padding(bottom = 16.dp))

            OutlinedTextField(
                value = cssText,
                onValueChange = { cssText = it },
                modifier = Modifier.fillMaxWidth().height(200.dp),
                placeholder = { Text("body { font-size: 16px; }\n* { max-width: 100% !important; }", fontSize = 13.sp) },
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                minLines = 8
            )

            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    cssText = """
                        /* Dark reader */
                        html { filter: invert(1) hue-rotate(180deg) !important; }
                        img, video, iframe { filter: invert(1) hue-rotate(180deg) !important; }
                    """.trimIndent()
                }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.dark_reader)) }

                OutlinedButton(onClick = {
                    cssText = """
                        /* Readability */
                        body { font-family: Georgia, serif !important; line-height: 1.8 !important; max-width: 700px !important; margin: 0 auto !important; padding: 20px !important; }
                        * { font-size: 18px !important; }
                    """.trimIndent()
                }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.readable)) }
            }

            Spacer(Modifier.height(8.dp))

            Button(onClick = { viewModel.setCustomCss(cssText); onDismiss() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.apply_css))
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
