package com.example.browser.util

object AdBlocker {
    private val adDomains = setOf(
        "doubleclick.net",
        "googleadservices.com",
        "googlesyndication.com",
        "google-analytics.com",
        "adservice.google.com",
        "pagead2.googlesyndication.com",
        "tpc.googlesyndication.com",
        "www.googleadservices.com",
        "ads.yahoo.com",
        "ad.doubleclick.net",
        "static.criteo.net",
        "cdn.taboola.com",
        "cdn.revjet.com",
        "ads-twitter.com",
        "ads.linkedin.com",
        "facebook.com/tr",
        "connect.facebook.net",
        "analytics.twitter.com",
        "ads.tiktok.com",
        "ads.reddit.com"
    )

    private val adUrlPatterns = listOf(
        "/ads/",
        "/ad/",
        "/advert/",
        "/banner/",
        "/popup/",
        "/analytics/",
        "/tracking/",
        "ad_id=",
        "adid=",
        "adunit=",
        "ad_unit=",
        "advertisement",
        "sponsor",
        "promoted"
    )

    fun isAd(url: String): Boolean {
        val lowerUrl = url.lowercase()
        // Check domain blocklist
        for (domain in adDomains) {
            if (lowerUrl.contains(domain)) return true
        }
        // Check URL patterns
        for (pattern in adUrlPatterns) {
            if (lowerUrl.contains(pattern)) return true
        }
        return false
    }
}
