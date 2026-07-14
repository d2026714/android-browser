package com.example.browser.password

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

@Serializable
data class SavedPassword(
    val id: String,
    val domain: String,
    val username: String,
    val encryptedPassword: String,
    val iv: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsed: Long = System.currentTimeMillis()
)

class PasswordManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("passwords", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val keyAlias = "browser_password_key"

    init {
        createKeyIfNotExists()
    }

    private fun createKeyIfNotExists() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        if (!keyStore.containsAlias(keyAlias)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            keyGenerator.init(
                KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
            keyGenerator.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        return keyStore.getKey(keyAlias, null) as SecretKey
    }

    private fun encrypt(plaintext: String): Pair<String, String> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Pair(Base64.encodeToString(encrypted, Base64.DEFAULT), Base64.encodeToString(iv, Base64.DEFAULT))
    }

    private fun decrypt(encrypted: String, iv: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, Base64.decode(iv, Base64.DEFAULT))
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
        val decoded = cipher.doFinal(Base64.decode(encrypted, Base64.DEFAULT))
        return String(decoded, Charsets.UTF_8)
    }

    fun savePassword(domain: String, username: String, password: String): SavedPassword {
        val (enc, iv) = encrypt(password)
        val entry = SavedPassword(
            id = "${domain}_${username}_${System.currentTimeMillis()}",
            domain = domain,
            username = username,
            encryptedPassword = enc,
            iv = iv
        )
        val passwords = getAllPasswords().toMutableList()
        // Remove existing entry for same domain+username
        passwords.removeAll { it.domain == domain && it.username == username }
        passwords.add(entry)
        saveAll(passwords)
        return entry
    }

    fun getPassword(domain: String, username: String): String? {
        val entry = getAllPasswords().find { it.domain == domain && it.username == username }
        return entry?.let { decrypt(it.encryptedPassword, it.iv) }
    }

    fun getSavedCredentials(domain: String): List<SavedPassword> {
        return getAllPasswords().filter { it.domain == domain }
    }

    fun getAllPasswords(): List<SavedPassword> {
        val data = prefs.getString(KEY_PASSWORDS, null) ?: return emptyList()
        return try { json.decodeFromString(data) } catch (_: Exception) { emptyList() }
    }

    fun deletePassword(id: String) {
        val passwords = getAllPasswords().toMutableList()
        passwords.removeAll { it.id == id }
        saveAll(passwords)
    }

    fun deleteAllForDomain(domain: String) {
        val passwords = getAllPasswords().toMutableList()
        passwords.removeAll { it.domain == domain }
        saveAll(passwords)
    }

    fun clearAll() {
        prefs.edit().remove(KEY_PASSWORDS).apply()
    }

    private fun saveAll(passwords: List<SavedPassword>) {
        prefs.edit().putString(KEY_PASSWORDS, json.encodeToString(passwords)).apply()
    }

    companion object {
        private const val KEY_PASSWORDS = "saved_passwords"

        @Volatile
        private var instance: PasswordManager? = null

        fun getInstance(context: Context): PasswordManager {
            return instance ?: synchronized(this) {
                instance ?: PasswordManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
