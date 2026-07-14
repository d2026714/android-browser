package com.example.browser.traffic

import android.content.Context
import android.net.TrafficStats as AndroidTrafficStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

/**
 * Network traffic statistics tracker.
 * Monitors data usage per session and overall.
 */
class TrafficStats(private val context: Context) {

    data class Stats(
        val sessionRxBytes: Long = 0,      // Bytes received this session
        val sessionTxBytes: Long = 0,      // Bytes sent this session
        val sessionStartRx: Long = 0,      // Total rx at session start
        val sessionStartTx: Long = 0,      // Total tx at session start
        val totalRxBytes: Long = 0,        // Total bytes received ever
        val totalTxBytes: Long = 0,        // Total bytes sent ever
        val adsBlockedRx: Long = 0,        // Bytes saved by ad blocker
        val dataSaverRx: Long = 0,         // Bytes saved by data saver
        val requestsCount: Int = 0,        // Total requests this session
        val adsBlockedCount: Int = 0,      // Ads blocked this session
        val sessionStartTime: Long = System.currentTimeMillis()
    ) {
        val sessionTotalBytes: Long get() = sessionRxBytes + sessionTxBytes
        val totalBytes: Long get() = totalRxBytes + totalTxBytes
        val totalSavedBytes: Long get() = adsBlockedRx + dataSaverRx

        val sessionDuration: Long get() = System.currentTimeMillis() - sessionStartTime
        val averageSpeed: Long get() = if (sessionDuration > 0) sessionTotalBytes * 1000 / sessionDuration else 0
    }

    private val _stats = MutableStateFlow(Stats())
    val stats: StateFlow<Stats> = _stats.asStateFlow()

    private var initialRx: Long = 0
    private var initialTx: Long = 0

    fun startSession() {
        initialRx = AndroidTrafficStats.getTotalRxBytes()
        initialTx = AndroidTrafficStats.getTotalTxBytes()
        _stats.value = Stats(
            sessionStartRx = initialRx,
            sessionStartTx = initialTx,
            totalRxBytes = getStoredTotalRx(),
            totalTxBytes = getStoredTotalTx()
        )
    }

    fun update() {
        val currentRx = AndroidTrafficStats.getTotalRxBytes()
        val currentTx = AndroidTrafficStats.getTotalTxBytes()

        _stats.value = _stats.value.copy(
            sessionRxBytes = currentRx - initialRx,
            sessionTxBytes = currentTx - initialTx,
            totalRxBytes = getStoredTotalRx() + (currentRx - initialRx),
            totalTxBytes = getStoredTotalTx() + (currentTx - initialTx)
        )
    }

    fun onAdBlocked(bytesSaved: Long) {
        _stats.value = _stats.value.copy(
            adsBlockedRx = _stats.value.adsBlockedRx + bytesSaved,
            adsBlockedCount = _stats.value.adsBlockedCount + 1
        )
    }

    fun onDataSaved(bytesSaved: Long) {
        _stats.value = _stats.value.copy(dataSaverRx = _stats.value.dataSaverRx + bytesSaved)
    }

    fun onRequest() {
        _stats.value = _stats.value.copy(requestsCount = _stats.value.requestsCount + 1)
    }

    fun resetSession() {
        startSession()
    }

    private fun getStoredTotalRx(): Long {
        return context.getSharedPreferences("traffic", Context.MODE_PRIVATE).getLong("total_rx", 0)
    }

    private fun getStoredTotalTx(): Long {
        return context.getSharedPreferences("traffic", Context.MODE_PRIVATE).getLong("total_tx", 0)
    }

    fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        if (bytes < 1024 * 1024) return "${"%.1f".format(bytes / 1024.0)} KB"
        if (bytes < 1024 * 1024 * 1024) return "${"%.2f".format(bytes / (1024.0 * 1024.0))} MB"
        return "${"%.2f".format(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
    }

    fun formatSpeed(bytesPerSecond: Long): String {
        return "${formatBytes(bytesPerSecond)}/s"
    }

    fun formatDuration(ms: Long): String {
        val seconds = ms / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        return when {
            hours > 0 -> "${hours}h ${minutes % 60}m"
            minutes > 0 -> "${minutes}m ${seconds % 60}s"
            else -> "${seconds}s"
        }
    }

    companion object {
        @Volatile
        private var instance: TrafficStats? = null

        fun getInstance(context: Context): TrafficStats {
            return instance ?: synchronized(this) {
                instance ?: TrafficStats(context.applicationContext).also { instance = it }
            }
        }
    }
}
