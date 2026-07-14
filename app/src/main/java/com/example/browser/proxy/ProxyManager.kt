package com.example.browser.proxy

import android.content.Context
import android.webkit.WebView
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.InetSocketAddress
import java.net.Proxy

@Serializable
data class ProxyConfig(
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
    val type: ProxyType = ProxyType.HTTP,
    val username: String = "",
    val password: String = "",
    val enabled: Boolean = false
) {
    enum class ProxyType { HTTP, HTTPS, SOCKS4, SOCKS5 }
}

class ProxyManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("proxy_config", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun getConfigs(): List<ProxyConfig> {
        val data = prefs.getString(KEY_CONFIGS, null) ?: return emptyList()
        return try { json.decodeFromString(data) } catch (_: Exception) { emptyList() }
    }

    fun saveConfig(config: ProxyConfig) {
        val configs = getConfigs().toMutableList()
        configs.removeAll { it.id == config.id }
        configs.add(config)
        prefs.edit().putString(KEY_CONFIGS, json.encodeToString(configs)).apply()
    }

    fun deleteConfig(id: String) {
        val configs = getConfigs().toMutableList()
        configs.removeAll { it.id == id }
        prefs.edit().putString(KEY_CONFIGS, json.encodeToString(configs)).apply()
    }

    fun getActiveConfig(): ProxyConfig? {
        return getConfigs().find { it.enabled }
    }

    fun setActiveConfig(id: String?) {
        val configs = getConfigs().map { it.copy(enabled = it.id == id) }
        prefs.edit().putString(KEY_CONFIGS, json.encodeToString(configs)).apply()
    }

    fun disableAll() {
        val configs = getConfigs().map { it.copy(enabled = false) }
        prefs.edit().putString(KEY_CONFIGS, json.encodeToString(configs)).apply()
    }

    fun applyProxy(webView: WebView, config: ProxyConfig) {
        // Apply proxy settings via system properties
        // Note: WebView doesn't directly support proxy, this needs system-level configuration
        when (config.type) {
            ProxyConfig.ProxyType.HTTP, ProxyConfig.ProxyType.HTTPS -> {
                System.setProperty("http.proxyHost", config.host)
                System.setProperty("http.proxyPort", config.port.toString())
                System.setProperty("https.proxyHost", config.host)
                System.setProperty("https.proxyPort", config.port.toString())
            }
            ProxyConfig.ProxyType.SOCKS4, ProxyConfig.ProxyType.SOCKS5 -> {
                System.setProperty("socksProxyHost", config.host)
                System.setProperty("socksProxyPort", config.port.toString())
            }
        }
    }

    fun testConnection(config: ProxyConfig): Boolean {
        return try {
            val socket = java.net.Socket()
            socket.connect(InetSocketAddress(config.host, config.port), 5000)
            socket.close()
            true
        } catch (_: Exception) { false }
    }

    companion object {
        private const val KEY_CONFIGS = "proxy_configs"

        @Volatile
        private var instance: ProxyManager? = null

        fun getInstance(context: Context): ProxyManager {
            return instance ?: synchronized(this) {
                instance ?: ProxyManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
