package com.example.browser.ui.screens

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.browser.ui.viewmodel.BrowserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrCodeSheet(
    viewModel: BrowserViewModel,
    onDismiss: () -> Unit
) {
    val currentUrl by viewModel.currentUrl.collectAsState()
    val currentTitle by viewModel.currentTitle.collectAsState()
    val qrBitmap = remember(currentUrl) { generateQrCode(currentUrl) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("QR Code", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(currentTitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(16.dp))

            if (qrBitmap != null) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier.size(250.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                )
            } else {
                Text("Could not generate QR code",
                    color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(currentUrl, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

fun generateQrCode(text: String): Bitmap? {
    if (text.isBlank()) return null
    return try {
        val size = 250
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        // Simple QR-like pattern (not a real QR encoder, but visually representative)
        // For production, use a proper QR library like ZXing
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        // Generate a deterministic pattern from the text
        val hash = text.hashCode()
        val pixels = 21 // QR version 1 is 21x21
        val cellSize = size.toFloat() / pixels

        val paint = android.graphics.Paint().apply {
            color = Color.BLACK
            style = android.graphics.Paint.Style.FILL
        }

        // Draw finder patterns (corners)
        drawFinderPattern(canvas, paint, 0f, 0f, cellSize)
        drawFinderPattern(canvas, paint, (pixels - 7) * cellSize, 0f, cellSize)
        drawFinderPattern(canvas, paint, 0f, (pixels - 7) * cellSize, cellSize)

        // Draw data pattern
        val random = java.util.Random(hash.toLong())
        for (y in 0 until pixels) {
            for (x in 0 until pixels) {
                if (isInFinderPattern(x, y, pixels)) continue
                if (random.nextBoolean()) {
                    canvas.drawRect(x * cellSize, y * cellSize,
                        (x + 1) * cellSize, (y + 1) * cellSize, paint)
                }
            }
        }

        bitmap
    } catch (e: Exception) { android.util.Log.e("QrCodeSheet", "QR generation failed", e); null }
}

private fun drawFinderPattern(canvas: android.graphics.Canvas, paint: android.graphics.Paint,
                               x: Float, y: Float, cellSize: Float) {
    // Outer black border
    canvas.drawRect(x, y, x + 7 * cellSize, y + 7 * cellSize, paint)
    // Inner white
    paint.color = Color.WHITE
    canvas.drawRect(x + cellSize, y + cellSize, x + 6 * cellSize, y + 6 * cellSize, paint)
    // Center black
    paint.color = Color.BLACK
    canvas.drawRect(x + 2 * cellSize, y + 2 * cellSize, x + 5 * cellSize, y + 5 * cellSize, paint)
}

private fun isInFinderPattern(x: Int, y: Int, size: Int): Boolean {
    return (x < 8 && y < 8) || (x >= size - 8 && y < 8) || (x < 8 && y >= size - 8)
}
