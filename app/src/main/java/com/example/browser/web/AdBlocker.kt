package com.example.browser.web

object AdBlocker {
    private val adDomains = setOf(
        "pagead2.googlesyndication.com", "adservice.google.com",
        "www.googleadservices.com", "googleads.g.doubleclick.net",
        "ads.youtube.com", "ads.facebook.com", "pixel.facebook.com",
        "analytics.facebook.com", "ads.linkedin.com", "ads-api.tiktok.com",
        "analytics.tiktok.com", "ads.yahoo.com", "adserver.yahoo.com",
        "amazon-adsystem.com", "google-analytics.com", "googlesyndication.com",
        "googletagmanager.com", "hotjar.com", "doubleclick.net",
        "outbrain.com", "taboola.com", "pubmatic.com", "adnxs.com",
        "adsrvr.org", "moatads.com", "scorecardresearch.com",
        "quantserve.com", "rubiconproject.com", "segment.com",
    )

    private val adPaths = listOf(
        "/ads/", "/ad/", "/adserver/", "/advert/", "/banner/",
        "/banners/", "/popup/", "/tracking/", "/pixel/",
    )

    fun isAd(url: String): Boolean {
        val lower = url.lowercase()
        return try {
            val host = java.net.URI(url).host?.lowercase() ?: return false
            adDomains.any { host == it || host.endsWith(".$it") } ||
                adPaths.any { lower.contains(it) }
        } catch (_: Exception) {
            false
        }
    }
}
