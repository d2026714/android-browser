package com.example.browser.ui.components

import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
fun BlueLightFilterOverlay(
    intensity: Float, // 0.0 to 0.8
    enabled: Boolean
) {
    if (enabled && intensity > 0f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFF8C00).copy(alpha = intensity * 0.3f))
                .zIndex(0.5f)
        )
    }
}
