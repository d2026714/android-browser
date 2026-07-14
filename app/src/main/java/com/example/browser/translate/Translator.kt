package com.example.browser.translate

import android.content.Context
import android.webkit.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Built-in page translator using Google Translate API (free tier).
 * Supports 100+ languages.
 */
class Translator(private val context: Context) {

    data class Language(val code: String, val name: String)

    val supportedLanguages = listOf(
        Language("auto", "Auto Detect"),
        Language("en", "English"),
        Language("zh", "Chinese"),
        Language("ja", "Japanese"),
        Language("ko", "Korean"),
        Language("es", "Spanish"),
        Language("fr", "French"),
        Language("de", "German"),
        Language("ru", "Russian"),
        Language("pt", "Portuguese"),
        Language("it", "Italian"),
        Language("ar", "Arabic"),
        Language("hi", "Hindi"),
        Language("th", "Thai"),
        Language("vi", "Vietnamese"),
        Language("nl", "Dutch"),
        Language("pl", "Polish"),
        Language("tr", "Turkish"),
        Language("sv", "Swedish"),
        Language("da", "Danish"),
        Language("fi", "Finnish"),
        Language("no", "Norwegian"),
        Language("cs", "Czech"),
        Language("el", "Greek"),
        Language("he", "Hebrew"),
        Language("hu", "Hungarian"),
        Language("ro", "Romanian"),
        Language("uk", "Ukrainian"),
        Language("id", "Indonesian"),
        Language("ms", "Malay"),
        Language("bn", "Bengali"),
        Language("ta", "Tamil"),
        Language("te", "Telugu"),
        Language("ml", "Malayalam"),
        Language("kn", "Kannada"),
        Language("mr", "Marathi"),
        Language("gu", "Gujarati"),
        Language("pa", "Punjabi"),
        Language("ur", "Urdu"),
        Language("fa", "Persian"),
        Language("sw", "Swahili"),
        Language("am", "Amharic"),
        Language("my", "Burmese"),
        Language("km", "Khmer"),
        Language("lo", "Lao"),
        Language("si", "Sinhala"),
        Language("ne", "Nepali"),
        Language("et", "Estonian"),
        Language("lv", "Latvian"),
        Language("lt", "Lithuanian"),
        Language("sk", "Slovak"),
        Language("sl", "Slovenian"),
        Language("bg", "Bulgarian"),
        Language("sr", "Serbian"),
        Language("hr", "Croatian"),
        Language("sq", "Albanian"),
        Language("mk", "Macedonian"),
        Language("bs", "Bosnian"),
        Language("is", "Icelandic"),
        Language("ga", "Irish"),
        Language("cy", "Welsh"),
        Language("af", "Afrikaans"),
        Language("zu", "Zulu"),
        Language("xh", "Xhosa"),
        Language("st", "Southern Sotho"),
        Language("yo", "Yoruba"),
        Language("ig", "Igbo"),
        Language("ha", "Hausa"),
        Language("so", "Somali"),
        Language("mg", "Malagasy"),
        Language("eo", "Esperanto"),
        Language("la", "Latin"),
        Language("mi", "Maori"),
        Language("haw", "Hawaiian"),
        Language("ceb", "Cebuano"),
        Language("hmn", "Hmong"),
        Language("jw", "Javanese"),
        Language("su", "Sundanese"),
        Language("tl", "Filipino"),
        Language("ht", "Haitian Creole"),
        Language("co", "Corsican"),
        Language("fy", "Frisian"),
        Language("lb", "Luxembourgish"),
        Language("gd", "Scots Gaelic"),
        Language("eu", "Basque"),
        Language("gl", "Galician"),
        Language("ca", "Catalan"),
    )

    fun translatePage(webView: WebView, sourceLang: String = "auto", targetLang: String = "en") {
        val url = webView.url ?: return
        val translateUrl = "https://translate.google.com/translate?sl=$sourceLang&tl=$targetLang&u=${URLEncoder.encode(url, "UTF-8")}"
        webView.loadUrl(translateUrl)
    }

    suspend fun translateText(text: String, sourceLang: String = "auto", targetLang: String = "en"): String {
        return withContext(Dispatchers.IO) {
            try {
                val encoded = URLEncoder.encode(text, "UTF-8")
                val url = URL("https://translate.googleapis.com/translate_a/single?client=gtx&sl=$sourceLang&tl=$targetLang&dt=t&q=$encoded")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                val response = conn.inputStream.bufferedReader().readText()
                // Parse Google Translate response
                val result = StringBuilder()
                val parts = response.split("[[\"")
                for (i in 1 until parts.size) {
                    val end = parts[i].indexOf("\"")
                    if (end > 0) {
                        result.append(parts[i].substring(0, end))
                    }
                }
                result.toString().ifBlank { text }
            } catch (_: Exception) { text }
        }
    }

    fun getLanguageName(code: String): String {
        return supportedLanguages.find { it.code == code }?.name ?: code
    }

    companion object {
        @Volatile
        private var instance: Translator? = null

        fun getInstance(context: Context): Translator {
            return instance ?: synchronized(this) {
                instance ?: Translator(context.applicationContext).also { instance = it }
            }
        }
    }
}
