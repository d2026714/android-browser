package com.example.browser.ui.components

import android.net.http.SslError
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.example.browser.R
import com.example.browser.ui.viewmodel.BrowserViewModel

@Composable
fun SslErrorDialog(viewModel: BrowserViewModel) {
    val sslError by viewModel.sslError.collectAsState()
    val error = sslError ?: return

    AlertDialog(
        onDismissRequest = { viewModel.dismissSslError() },
        icon = { },
        title = { Text(stringResource(R.string.security_warning)) },
        text = {
            val errorMsg = when (error.primaryError) {
                SslError.SSL_DATE_INVALID -> stringResource(R.string.ssl_date_invalid)
                SslError.SSL_EXPIRED -> stringResource(R.string.ssl_expired)
                SslError.SSL_IDMISMATCH -> stringResource(R.string.ssl_id_mismatch)
                SslError.SSL_NOTYETVALID -> stringResource(R.string.ssl_not_yet_valid)
                SslError.SSL_UNTRUSTED -> stringResource(R.string.ssl_untrusted)
                SslError.SSL_INVALID -> stringResource(R.string.ssl_invalid)
                else -> stringResource(R.string.ssl_unknown_error)
            }
            Text(
                stringResource(R.string.ssl_warning_message) + "\n\n" +
                    stringResource(R.string.url_label, error.url ?: "unknown") + "\n" +
                    stringResource(R.string.error_label, errorMsg)
            )
        },
        confirmButton = {
            TextButton(onClick = { viewModel.dismissSslError() }) {
                Text(stringResource(R.string.ok))
            }
        }
    )
}
