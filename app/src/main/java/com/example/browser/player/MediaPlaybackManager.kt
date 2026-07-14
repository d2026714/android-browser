package com.example.browser.player

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "MediaPlaybackManager"

/**
 * Manages ExoPlayer instance lifecycle and exposes playback state
 * for Compose UI consumption.
 */
@OptIn(UnstableApi::class)
class MediaPlaybackManager(private val context: Context) {

    private var player: ExoPlayer? = null

    // --- Playback state exposed to UI ---
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _volume = MutableStateFlow(1f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _isFullScreen = MutableStateFlow(false)
    val isFullScreen: StateFlow<Boolean> = _isFullScreen.asStateFlow()

    private val _playerError = MutableStateFlow<String?>(null)
    val playerError: StateFlow<String?> = _playerError.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _mediaTitle = MutableStateFlow("")
    val mediaTitle: StateFlow<String> = _mediaTitle.asStateFlow()

    private var progressUpdateRunnable: Runnable? = null
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    /**
     * Get or create the ExoPlayer instance.
     */
    fun getPlayer(): ExoPlayer {
        if (player == null) {
            player = ExoPlayer.Builder(context).build().apply {
                addListener(playerListener)
            }
            Log.d(TAG, "ExoPlayer created")
        }
        return player!!
    }

    /**
     * Load and play a media item.
     * @param url The media URL (video or audio)
     * @param title Optional title for the media
     * @param mimeType Optional MIME type hint (e.g. "video/mp4", "audio/mpeg")
     */
    fun load(url: String, title: String = "", mimeType: String? = null) {
        val p = getPlayer()
        val mediaItem = if (mimeType != null) {
            val contentType = when {
                mimeType.startsWith("video/") -> C.CONTENT_TYPE_MOVIE
                mimeType.startsWith("audio/") -> C.CONTENT_TYPE_MUSIC
                else -> C.CONTENT_TYPE_UNKNOWN
            }
            MediaItem.Builder()
                .setUri(url)
                .setMimeType(mimeType)
                .build()
        } else {
            MediaItem.fromUri(url)
        }
        p.setMediaItem(mediaItem)
        p.prepare()
        p.playWhenReady = true
        _mediaTitle.value = title
        _playerError.value = null
        startPositionUpdates()
        Log.d(TAG, "Loading media: $url (title=$title, mime=$mimeType)")
    }

    fun play() {
        getPlayer().playWhenReady = true
    }

    fun pause() {
        getPlayer().playWhenReady = false
    }

    fun togglePlayPause() {
        val p = getPlayer()
        p.playWhenReady = !p.playWhenReady
    }

    fun seekTo(positionMs: Long) {
        getPlayer().seekTo(positionMs)
    }

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        getPlayer().volume = clamped
        _volume.value = clamped
    }

    fun toggleFullScreen() {
        _isFullScreen.value = !_isFullScreen.value
    }

    fun setFullScreen(fullScreen: Boolean) {
        _isFullScreen.value = fullScreen
    }

    fun stop() {
        player?.stop()
        _isPlaying.value = false
        _currentPosition.value = 0
        _duration.value = 0
        stopPositionUpdates()
    }

    fun release() {
        stopPositionUpdates()
        player?.apply {
            removeListener(playerListener)
            release()
        }
        player = null
        Log.d(TAG, "ExoPlayer released")
    }

    fun clearError() {
        _playerError.value = null
    }

    // --- Position tracking ---

    private fun startPositionUpdates() {
        stopPositionUpdates()
        progressUpdateRunnable = object : Runnable {
            override fun run() {
                player?.let {
                    _currentPosition.value = it.currentPosition
                    _duration.value = if (it.duration != C.TIME_UNSET) it.duration else 0L
                }
                handler.postDelayed(this, 500)
            }
        }
        handler.post(progressUpdateRunnable!!)
    }

    private fun stopPositionUpdates() {
        progressUpdateRunnable?.let { handler.removeCallbacks(it) }
        progressUpdateRunnable = null
    }

    // --- Player listener ---

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            if (isPlaying) startPositionUpdates() else stopPositionUpdates()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> _isBuffering.value = true
                Player.STATE_READY -> {
                    _isBuffering.value = false
                    _duration.value = player?.let {
                        if (it.duration != C.TIME_UNSET) it.duration else 0L
                    } ?: 0L
                }
                Player.STATE_ENDED -> {
                    _isBuffering.value = false
                    _isPlaying.value = false
                    stopPositionUpdates()
                }
                Player.STATE_IDLE -> _isBuffering.value = false
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "Playback error: ${error.message}", error)
            _playerError.value = error.message ?: "Playback error"
            _isBuffering.value = false
            _isPlaying.value = false
        }
    }

    companion object {
        /** Check if a URL likely points to a video resource. */
        fun isVideoUrl(url: String): Boolean {
            val lower = url.lowercase()
            return lower.contains(".mp4") ||
                    lower.contains(".webm") ||
                    lower.contains(".mkv") ||
                    lower.contains(".avi") ||
                    lower.contains(".mov") ||
                    lower.contains(".m3u8") ||
                    lower.contains(".ts") ||
                    lower.contains("video/")
        }

        /** Check if a URL likely points to an audio resource. */
        fun isAudioUrl(url: String): Boolean {
            val lower = url.lowercase()
            return lower.contains(".mp3") ||
                    lower.contains(".ogg") ||
                    lower.contains(".wav") ||
                    lower.contains(".aac") ||
                    lower.contains(".flac") ||
                    lower.contains(".m4a") ||
                    lower.contains("audio/")
        }

        /** Check if a URL is a media resource (video or audio). */
        fun isMediaUrl(url: String): Boolean = isVideoUrl(url) || isAudioUrl(url)

        /** Guess MIME type from URL. */
        fun guessMimeType(url: String): String? {
            val lower = url.lowercase()
            return when {
                lower.contains(".mp4") -> "video/mp4"
                lower.contains(".webm") -> "video/webm"
                lower.contains(".mkv") -> "video/x-matroska"
                lower.contains(".m3u8") -> MimeTypes.APPLICATION_M3U8
                lower.contains(".mp3") -> "audio/mpeg"
                lower.contains(".ogg") -> "audio/ogg"
                lower.contains(".aac") -> "audio/aac"
                lower.contains(".flac") -> "audio/flac"
                lower.contains(".m4a") -> "audio/mp4"
                lower.contains(".wav") -> "audio/wav"
                else -> null
            }
        }
    }
}
