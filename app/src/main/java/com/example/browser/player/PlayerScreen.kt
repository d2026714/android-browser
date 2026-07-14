package com.example.browser.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.browser.R
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Full-screen video player UI with:
 * - Playback controls (play/pause, seek, volume)
 * - Progress bar with time display
 * - Full-screen toggle
 * - Gesture controls (vertical swipe for volume, horizontal for seek)
 * - Auto-hide controls after 3 seconds
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    manager: MediaPlaybackManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    val isPlaying by manager.isPlaying.collectAsState()
    val currentPosition by manager.currentPosition.collectAsState()
    val duration by manager.duration.collectAsState()
    val volume by manager.volume.collectAsState()
    val isFullScreen by manager.isFullScreen.collectAsState()
    val isBuffering by manager.isBuffering.collectAsState()
    val playerError by manager.playerError.collectAsState()
    val mediaTitle by manager.mediaTitle.collectAsState()

    var controlsVisible by remember { mutableStateOf(true) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableStateOf(0L) }
    var brightnessSlider by remember { mutableStateOf(false) }
    var volumeSlider by remember { mutableStateOf(false) }
    var gestureVolume by remember { mutableStateOf(volume) }

    // Auto-hide controls after 3 seconds of inactivity
    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying) {
            delay(3000)
            controlsVisible = false
        }
    }

    // Immersive mode for full-screen
    LaunchedEffect(isFullScreen) {
        if (isFullScreen) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Clean up on dispose
    DisposableEffect(Unit) {
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // --- Video surface ---
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { controlsVisible = false },
                        onHorizontalDrag = { _, dragAmount ->
                            // Horizontal drag: seek forward/backward
                            val seekDelta = (dragAmount * 200).toLong()
                            val newPos = (currentPosition + seekDelta).coerceIn(0, duration)
                            manager.seekTo(newPos)
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            val screenWidth = size.width
                            if (offset.x < screenWidth / 2) {
                                brightnessSlider = true
                                volumeSlider = false
                            } else {
                                volumeSlider = true
                                brightnessSlider = false
                                gestureVolume = volume
                            }
                        },
                        onVerticalDrag = { _, dragAmount ->
                            if (volumeSlider) {
                                gestureVolume = (gestureVolume - dragAmount / 800f).coerceIn(0f, 1f)
                                manager.setVolume(gestureVolume)
                            } else if (brightnessSlider) {
                                val lp = activity?.window?.attributes ?: return@detectVerticalDragGestures
                                val newBrightness = (lp.screenBrightness - dragAmount / 800f).coerceIn(0.01f, 1f)
                                lp.screenBrightness = newBrightness
                                activity.window.attributes = lp
                            }
                        },
                        onDragEnd = {
                            brightnessSlider = false
                            volumeSlider = false
                        }
                    )
                },
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = manager.getPlayer()
                    useController = false  // We build our own controls
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                }
            },
            update = { playerView ->
                playerView.player = manager.getPlayer()
            }
        )

        // --- Buffering indicator ---
        if (isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp),
                color = Color.White,
                strokeWidth = 3.dp
            )
        }

        // --- Error overlay ---
        playerError?.let { error ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        manager.clearError()
                        onBack()
                    }) {
                        Text(stringResource(R.string.close))
                    }
                }
            }
        }

        // --- Controls overlay (tap to toggle) ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { _, _ -> }
                }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.any { it.pressed }) {
                                controlsVisible = !controlsVisible
                            }
                        }
                    }
                }
        )

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // --- Top bar (back, title, full-screen) ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = Color.White
                        )
                    }
                    Text(
                        text = mediaTitle.ifBlank { stringResource(R.string.player) },
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )
                    IconButton(onClick = { manager.toggleFullScreen() }) {
                        Icon(
                            if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = if (isFullScreen) stringResource(R.string.exit_fullscreen) else stringResource(R.string.fullscreen),
                            tint = Color.White
                        )
                    }
                }

                // --- Center play/pause ---
                IconButton(
                    onClick = { manager.togglePlayPause() },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) stringResource(R.string.pause_playback) else stringResource(R.string.play_playback),
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }

                // --- Bottom controls (progress bar, time, volume) ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    // Time display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(if (isSeeking) seekPosition else currentPosition),
                            color = Color.White,
                            fontSize = 12.sp
                        )
                        Text(
                            text = formatTime(duration),
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Progress slider
                    Slider(
                        value = if (duration > 0) {
                            (if (isSeeking) seekPosition else currentPosition).toFloat() / duration.toFloat()
                        } else 0f,
                        onValueChange = { fraction ->
                            isSeeking = true
                            seekPosition = (fraction * duration).toLong()
                        },
                        onValueChangeFinished = {
                            manager.seekTo(seekPosition)
                            isSeeking = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Volume control
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            when {
                                volume == 0f -> Icons.Default.VolumeOff
                                volume < 0.5f -> Icons.Default.VolumeDown
                                else -> Icons.Default.VolumeUp
                            },
                            contentDescription = stringResource(R.string.volume),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Slider(
                            value = volume,
                            onValueChange = { manager.setVolume(it) },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
