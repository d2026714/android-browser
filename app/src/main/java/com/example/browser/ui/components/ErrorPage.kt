package com.example.browser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.browser.R
import com.example.browser.ui.viewmodel.BrowserViewModel

@Composable
fun ErrorPage(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val pageError by viewModel.pageError.collectAsState()
    val error = pageError ?: return

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = when {
                    error.description.contains("net::", ignoreCase = true) -> Icons.Default.WifiOff
                    error.description.contains("timeout", ignoreCase = true) -> Icons.Default.Timer
                    error.description.contains("ssl", ignoreCase = true) -> Icons.Default.Lock
                    else -> Icons.Default.ErrorOutline
                },
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = when {
                    error.description.contains("net::ERR_NAME_NOT_RESOLVED") -> stringResource(R.string.cant_reach_site)
                    error.description.contains("net::ERR_INTERNET_DISCONNECTED") -> stringResource(R.string.no_internet)
                    error.description.contains("net::ERR_CONNECTION_TIMED_OUT") -> stringResource(R.string.connection_timed_out)
                    error.description.contains("net::ERR_CONNECTION_REFUSED") -> stringResource(R.string.connection_refused)
                    error.description.contains("ssl", ignoreCase = true) -> stringResource(R.string.ssl_certificate_error)
                    else -> stringResource(R.string.page_cant_be_loaded)
                },
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = error.url ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = error.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = { viewModel.reload() }) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.try_again))
            }
        }
    }
}
