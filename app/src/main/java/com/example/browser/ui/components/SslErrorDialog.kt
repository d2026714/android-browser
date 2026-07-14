package com.example.browser.ui.components

import android.net.http.SslError
import android.net.http.SslErrorHandler
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.browser.ui.viewmodel.BrowserViewModel

@Composable
fun SslErrorDialog(viewModel: BrowserViewModel) {
    val sslError by viewModel.sslError.collectAsState()
    val error = sslError ?: return

    AlertDialog(
        onDismissRequest = { viewModel.dismissSslError() },
        icon = { },
        title = { Text("Security Warning") },
        text = {
            Text(
                "The SSL certificate for this site is not valid. " +
                    "Your connection may not be secure.\n\n" +
                    "URL: ${error.url ?: "unknown"}\n" +
                    "Error: ${
                        when (error.primaryError) {
                            SslError.SSL_DATE_INVALID -> "Certificate date is invalid"
                            SslError.SSL_EXPIRED -> "Certificate has expired"
                            SslError.SSL_IDMISMATCH -> "Certificate ID mismatch"
                            SslError.SSSL_NOTYETVALID -> "Certificate not yet valid"
                            SslError.SSL_UNTRUSTED -> "Certificate not trusted"
                            SslError.SSL_INVALID -> "Invalid certificate"
                            else -> "Unknown SSL error"
                        }
                    }"
            )
        },
        confirmButton = {
            TextButton(onClick = { viewModel.dismissSslError() }) {
                Text("OK")
            }
        }
    )
}
