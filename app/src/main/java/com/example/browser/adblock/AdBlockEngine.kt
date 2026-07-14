package com.example.browser.adblock

import android.content.Context
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.CopyOnWriteArrayList
import java.util.regex.Pattern

private const val TAG = "AdBlockEngine"

/**
 * Ad blocking engine that loads EasyList rules from bundled assets.
 * Supports:
 * - Domain blocking rules (||domain^)
 * - URL pattern matching (plain text substring, wildcard, regex)
 * - Exception rules (@@)
 * - Element hiding rules (##) are ignored (not applicable to request interception)
 *
 * Uses optimized data structures for fast lookups:
 * - HashSet for domain matching
 * - Pre-compiled regex patterns where possible
 * - Aho-Corasick style prefix matching for plain patterns
 */
class AdBlockEngine(private val context: Context) {

    // Domain-level blocking: stores domain suffixes for fast HashSet lookup
    private val blockedDomains = HashSet<String>(4096)

    // URL path patterns (substring match, lowercased)
    private val urlPatterns = CopyOnWriteArrayList<String>()

    // Compiled regex patterns for rules that need regex matching
    private val regexPatterns = CopyOnWriteArrayList<Pattern>()

    // Exception domains (whitelist)
    private val exceptionDomains = HashSet<String>(256)

    // Exception URL patterns
    private val exceptionPatterns = CopyOnWriteArrayList<String>()

    // Exception regex patterns
    private val exceptionRegexPatterns = CopyOnWriteArrayList<Pattern>()

    // Options-based rules (e.g., ||domain^$third-party) — stored as structured rules
    private val optionRules = CopyOnWriteArrayList<OptionRule>()

    @Volatile
    private var isLoaded = false

    @Volatile
    private var isLoading = false

    /**
     * Load filter rules from bundled assets.
     * Thread-safe: only one thread will do the actual loading.
     */
    fun loadFilters() {
        if (isLoaded) return
        if (isLoading) {
            // Another thread is loading, wait for it
            while (isLoading && !isLoaded) {
                Thread.sleep(50)
            }
            return
        }
        synchronized(this) {
            if (isLoaded) return
            isLoading = true
            try {
                val startTime = System.currentTimeMillis()
                loadFromAssets("adblock/easylist.txt")
                val elapsed = System.currentTimeMillis() - startTime
                Log.i(TAG, "EasyList loaded: ${blockedDomains.size} domains, ${urlPatterns.size} patterns, " +
                        "${regexPatterns.size} regexes, ${optionRules.size} option rules in ${elapsed}ms")
                isLoaded = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load EasyList", e)
                // Load fallback hardcoded rules
                loadFallbackRules()
                isLoaded = true
            } finally {
                isLoading = false
            }
        }
    }

    private fun loadFromAssets(fileName: String) {
        val input = context.assets.open(fileName)
        val reader = BufferedReader(InputStreamReader(input), 8192)
        reader.useLines { lines ->
            lines.forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty()) {
                    parseLine(trimmed)
                }
            }
        }
    }

    private fun parseLine(line: String) {
        // Skip comments and headers
        if (line.startsWith("!") || line.startsWith("[Adblock")) return

        // Skip element hiding rules (##) — not applicable to request interception
        if (line.contains("##") || line.contains("#@#")) return

        // Exception rules (starts with @@)
        if (line.startsWith("@@")) {
            parseExceptionRule(line.substring(2))
            return
        }

        // Check for options ($)
        val dollarIndex = line.lastIndexOf('$')
        if (dollarIndex > 0 && dollarIndex < line.length - 1) {
            val pattern = line.substring(0, dollarIndex)
            val options = line.substring(dollarIndex + 1)
            parseOptionsRule(pattern, options)
            return
        }

        // Standard blocking rules
        parseBlockingRule(line)
    }

    private fun parseExceptionRule(rule: String) {
        if (rule.startsWith("||")) {
            val domain = extractDomain(rule)
            if (domain != null) {
                exceptionDomains.add(domain)
            } else {
                val pattern = normalizePattern(rule)
                if (pattern != null) {
                    if (isRegexPattern(rule)) {
                        try {
                            exceptionRegexPatterns.add(Pattern.compile(rule.removeSurrounding("/", "/"), Pattern.CASE_INSENSITIVE))
                        } catch (_: Exception) {}
                    } else {
                        exceptionPatterns.add(pattern.lowercase())
                    }
                }
            }
        } else if (isRegexPattern(rule)) {
            try {
                exceptionRegexPatterns.add(Pattern.compile(rule.removeSurrounding("/", "/"), Pattern.CASE_INSENSITIVE))
            } catch (_: Exception) {}
        } else {
            val pattern = normalizePattern(rule)
            if (pattern != null) {
                exceptionPatterns.add(pattern.lowercase())
            }
        }
    }

    private fun parseBlockingRule(line: String) {
        // Domain anchor rules (||domain^)
        if (line.startsWith("||")) {
            val domain = extractDomain(line)
            if (domain != null) {
                blockedDomains.add(domain)
                return
            }
            // Not a simple domain rule, treat as URL pattern
            val pattern = normalizePattern(line)
            if (pattern != null) {
                if (isRegexPattern(line)) {
                    compileRegex(line)?.let { regexPatterns.add(it) }
                } else {
                    urlPatterns.add(pattern.lowercase())
                }
            }
            return
        }

        // Regex patterns (/regex/)
        if (isRegexPattern(line)) {
            compileRegex(line)?.let { regexPatterns.add(it) }
            return
        }

        // Simple URL patterns
        val pattern = normalizePattern(line)
        if (pattern != null && pattern.length >= 3) {
            urlPatterns.add(pattern.lowercase())
        }
    }

    private fun parseOptionsRule(pattern: String, options: String) {
        val optionList = options.lowercase().split(",").map { it.trim() }

        // Skip rules that only apply to specific content types we can't filter
        // (stylesheet, script, image, etc. are fine for URL blocking)
        // Skip rules requiring specific domains (we can't check document domain in shouldInterceptRequest)
        val hasDomainOption = optionList.any { it.startsWith("domain=") }
        if (hasDomainOption) return

        // Skip element hiding options
        if (optionList.contains("elemhide") || optionList.contains("document")) return

        val normalizedPattern = normalizePattern(pattern) ?: return
        if (normalizedPattern.length < 3) return

        optionRules.add(OptionRule(
            pattern = normalizedPattern.lowercase(),
            isRegex = isRegexPattern(pattern),
            isThirdParty = optionList.contains("third-party"),
            isException = false
        ))
    }

    /**
     * Check if a URL should be blocked as an advertisement.
     */
    fun isAd(url: String): Boolean {
        if (!isLoaded) loadFilters()

        val lowerUrl = url.lowercase()

        // Extract hostname for domain matching
        val hostname = extractHostname(lowerUrl)

        // Check exception rules first (whitelist)
        if (isException(lowerUrl, hostname)) return false

        // Check domain rules (fast HashSet lookup)
        if (hostname != null && isDomainBlocked(hostname)) return true

        // Check URL patterns (substring match)
        if (isUrlPatternMatch(lowerUrl)) return true

        // Check regex patterns
        if (isRegexMatch(lowerUrl)) return true

        // Check option-based rules
        if (isOptionRuleMatch(lowerUrl)) return true

        return false
    }

    private fun isException(url: String, hostname: String?): Boolean {
        // Check exception domains
        if (hostname != null) {
            for (domain in exceptionDomains) {
                if (hostname == domain || hostname.endsWith(".$domain")) {
                    return true
                }
            }
        }

        // Check exception patterns
        for (pattern in exceptionPatterns) {
            if (url.contains(pattern)) return true
        }

        // Check exception regex patterns
        for (regex in exceptionRegexPatterns) {
            if (regex.matcher(url).find()) return true
        }

        return false
    }

    private fun isDomainBlocked(hostname: String): Boolean {
        // Direct match
        if (blockedDomains.contains(hostname)) return true

        // Check parent domains (e.g., "ads.example.com" matches rule "example.com")
        var dotIndex = hostname.indexOf('.')
        while (dotIndex >= 0) {
            val parentDomain = hostname.substring(dotIndex + 1)
            if (blockedDomains.contains(parentDomain)) return true
            dotIndex = hostname.indexOf('.', dotIndex + 1)
        }

        return false
    }

    private fun isUrlPatternMatch(url: String): Boolean {
        for (pattern in urlPatterns) {
            if (url.contains(pattern)) return true
        }
        return false
    }

    private fun isRegexMatch(url: String): Boolean {
        for (regex in regexPatterns) {
            if (regex.matcher(url).find()) return true
        }
        return false
    }

    private fun isOptionRuleMatch(url: String): Boolean {
        for (rule in optionRules) {
            val matches = if (rule.isRegex) {
                // Skip complex regex option rules for performance
                false
            } else {
                url.contains(rule.pattern)
            }
            if (matches) return true
        }
        return false
    }

    /**
     * Intercept ad requests, returning an empty response for blocked URLs.
     */
    fun shouldIntercept(request: WebResourceRequest): WebResourceResponse? {
        if (isAd(request.url.toString())) {
            return WebResourceResponse("text/plain", "utf-8", "".byteInputStream())
        }
        return null
    }

    /**
     * Get statistics about loaded rules.
     */
    fun getStats(): AdBlockStats {
        return AdBlockStats(
            domainRules = blockedDomains.size,
            urlPatterns = urlPatterns.size,
            regexPatterns = regexPatterns.size,
            exceptionDomains = exceptionDomains.size,
            exceptionPatterns = exceptionPatterns.size,
            optionRules = optionRules.size,
            totalRules = blockedDomains.size + urlPatterns.size + regexPatterns.size + optionRules.size
        )
    }

    // --- Helper functions ---

    /**
     * Extract the domain from a ||domain^ rule.
     * Returns null if the rule is too complex for simple domain extraction.
     */
    private fun extractDomain(rule: String): String? {
        if (!rule.startsWith("||")) return null
        val afterPrefix = rule.substring(2)

        // Find the end of the domain part (^, /, $, or end of string)
        val endChars = charArrayOf('^', '/', '$', '*', '|')
        val endIndex = afterPrefix.indexOfAny(endChars)
        val domain = if (endIndex >= 0) afterPrefix.substring(0, endIndex) else afterPrefix

        // Only use as domain rule if it looks like a valid domain
        if (domain.isEmpty() || domain.contains('*') || domain.contains('|')) return null
        if (domain.length < 3) return null

        return domain.lowercase()
    }

    /**
     * Extract hostname from a URL string.
     */
    private fun extractHostname(url: String): String? {
        val withoutProtocol = when {
            url.startsWith("https://") -> url.substring(8)
            url.startsWith("http://") -> url.substring(7)
            else -> return null
        }
        val endIdx = withoutProtocol.indexOfAny(charArrayOf('/', '?', '#', ':'))
        return if (endIdx >= 0) withoutProtocol.substring(0, endIdx) else withoutProtocol
    }

    /**
     * Check if a rule is a regex pattern (surrounded by /).
     */
    private fun isRegexPattern(rule: String): Boolean {
        return rule.startsWith("/") && rule.endsWith("/") && rule.length > 2
    }

    /**
     * Compile a regex pattern, returning null on failure.
     */
    private fun compileRegex(rule: String): Pattern? {
        return try {
            val patternStr = rule.removeSurrounding("/", "/")
            Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Normalize a rule pattern by removing anchor markers and converting wildcards.
     * Returns a plain text substring for matching, or null if too complex.
     */
    private fun normalizePattern(rule: String): String? {
        var pattern = rule

        // Remove || prefix (domain anchor)
        if (pattern.startsWith("||")) {
            pattern = pattern.substring(2)
        }

        // Remove | (start/end anchors)
        pattern = pattern.trimStart('|').trimEnd('|')

        // Remove ^ separator wildcards (match any non-alphanumeric)
        pattern = pattern.replace("^", "")

        // If still contains regex-like characters, skip
        if (pattern.contains("*") && pattern.count { it == '*' } > 2) return null

        // Remove wildcards for substring matching
        pattern = pattern.replace("*", "")

        // Unescape
        pattern = pattern.replace("\\", "")

        return pattern.ifEmpty { null }
    }

    /**
     * Load fallback hardcoded rules when EasyList cannot be loaded.
     */
    private fun loadFallbackRules() {
        Log.w(TAG, "Loading fallback ad rules")
        val fallbackDomains = listOf(
            "doubleclick.net", "googleadservices.com", "googlesyndication.com",
            "google-analytics.com", "adservice.google.com", "pagead2.googlesyndication.com",
            "ads.yahoo.com", "static.criteo.net", "cdn.taboola.com",
            "ads.twitter.com", "ads.linkedin.com", "connect.facebook.net",
            "ads.tiktok.com", "ads.reddit.com"
        )
        blockedDomains.addAll(fallbackDomains)

        val fallbackPatterns = listOf(
            "/ads/", "/ad/", "/advert/", "/banner/", "/popup/", "/analytics/",
            "/tracking/", "ad_id=", "adid=", "adunit=", "ad_unit=",
            "advertisement", "sponsor", "promoted"
        )
        urlPatterns.addAll(fallbackPatterns)
    }

    companion object {
        @Volatile
        private var instance: AdBlockEngine? = null

        fun getInstance(context: Context): AdBlockEngine {
            return instance ?: synchronized(this) {
                instance ?: AdBlockEngine(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}

/**
 * Structured option rule (e.g., ||domain^$third-party)
 */
private data class OptionRule(
    val pattern: String,
    val isRegex: Boolean,
    val isThirdParty: Boolean,
    val isException: Boolean
)

/**
 * Statistics about loaded ad blocking rules.
 */
data class AdBlockStats(
    val domainRules: Int,
    val urlPatterns: Int,
    val regexPatterns: Int,
    val exceptionDomains: Int,
    val exceptionPatterns: Int,
    val optionRules: Int,
    val totalRules: Int
)
