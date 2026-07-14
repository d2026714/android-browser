package com.example.browser.privacy

import android.content.Context
import android.webkit.WebResourceRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Privacy report tracker - monitors and reports on trackers,
 * cookies, and privacy-related requests.
 */
class PrivacyReport(private val context: Context) {

    data class Report(
        val trackersBlocked: Int = 0,
        val trackersAllowed: Int = 0,
        val cookiesTotal: Int = 0,
        val cookiesBlocked: Int = 0,
        val httpsRequests: Int = 0,
        val httpRequests: Int = 0,
        val fingerprintAttempts: Int = 0,
        val thirdPartyRequests: Int = 0,
        val domains: Set<String> = emptySet(),
        val startTime: Long = System.currentTimeMillis()
    ) {
        val privacyScore: Int
            get() {
                var score = 100
                score -= trackersAllowed * 5
                score -= httpRequests * 2
                score -= fingerprintAttempts * 10
                score -= thirdPartyRequests / 2
                return score.coerceIn(0, 100)
            }

        val grade: String
            get() = when {
                privacyScore >= 90 -> "A+"
                privacyScore >= 80 -> "A"
                privacyScore >= 70 -> "B"
                privacyScore >= 60 -> "C"
                privacyScore >= 50 -> "D"
                else -> "F"
            }
    }

    private val _report = MutableStateFlow(Report())
    val report: StateFlow<Report> = _report.asStateFlow()

    private val trackerDomains = setOf(
        "doubleclick.net", "googleadservices.com", "googlesyndication.com",
        "google-analytics.com", "facebook.com", "connect.facebook.net",
        "analytics.twitter.com", "ads.linkedin.com", "ads.tiktok.com",
        "amazon-adsystem.com", "outbrain.com", "taboola.com",
        "criteo.net", "adnxs.com", "pubmatic.com", "rubiconproject.com",
        "appnexus.com", "adsrvr.org", "casalemedia.com", "openx.net"
    )

    private val fingerprintPatterns = setOf(
        "fingerprint", "canvas.toDataURL", "webgl", "AudioContext",
        "navigator.plugins", "navigator.languages", "screen.colorDepth",
        "getTimezoneOffset", "hardwareConcurrency", "deviceMemory"
    )

    fun onRequest(request: WebResourceRequest, blocked: Boolean) {
        val url = request.url.toString()
        val host = request.url.host ?: ""
        val isHttps = url.startsWith("https://")
        val isTracker = trackerDomains.any { host.contains(it) }
        val isThirdParty = !isFirstParty(host)

        _report.value = _report.value.let { r ->
            r.copy(
                trackersBlocked = if (isTracker && blocked) r.trackersBlocked + 1 else r.trackersBlocked,
                trackersAllowed = if (isTracker && !blocked) r.trackersAllowed + 1 else r.trackersAllowed,
                httpsRequests = if (isHttps) r.httpsRequests + 1 else r.httpsRequests,
                httpRequests = if (!isHttps) r.httpRequests + 1 else r.httpRequests,
                thirdPartyRequests = if (isThirdParty) r.thirdPartyRequests + 1 else r.thirdPartyRequests,
                domains = r.domains + host
            )
        }
    }

    fun onFingerprintAttempt() {
        _report.value = _report.value.copy(fingerprintAttempts = _report.value.fingerprintAttempts + 1)
    }

    fun reset() {
        _report.value = Report()
    }

    private fun isFirstParty(host: String): Boolean {
        // Simplified - in production, compare with the main page domain
        return host.contains("localhost") || host.contains("127.0.0.1")
    }

    fun getDetailedReport(): String {
        val r = _report.value
        return buildString {
            appendLine("🛡️ Privacy Report")
            appendLine("═══════════════════")
            appendLine("Grade: ${r.grade} (${r.privacyScore}/100)")
            appendLine()
            appendLine("🚫 Trackers:")
            appendLine("  Blocked: ${r.trackersBlocked}")
            appendLine("  Allowed: ${r.trackersAllowed}")
            appendLine()
            appendLine("🔒 Connections:")
            appendLine("  HTTPS: ${r.httpsRequests}")
            appendLine("  HTTP: ${r.httpRequests}")
            appendLine()
            appendLine("🍪 Cookies: ${r.cookiesTotal}")
            appendLine("🔍 Fingerprint attempts: ${r.fingerprintAttempts}")
            appendLine("📡 Third-party requests: ${r.thirdPartyRequests}")
            appendLine("🌐 Unique domains: ${r.domains.size}")
        }
    }

    companion object {
        @Volatile
        private var instance: PrivacyReport? = null

        fun getInstance(context: Context): PrivacyReport {
            return instance ?: synchronized(this) {
                instance ?: PrivacyReport(context.applicationContext).also { instance = it }
            }
        }
    }
}
