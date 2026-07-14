package com.example.browser.translator

import android.content.Context
import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "TranslationManager"

/**
 * Offline translation engine powered by Google ML Kit.
 * Manages language model downloads, deletions, and text translation.
 */
class TranslationManager(private val context: Context) {

    /** Represents a supported language with its ML Kit language code. */
    data class Language(val code: String, val displayName: String)

    /** Status of a downloaded language model. */
    enum class ModelStatus { NOT_DOWNLOADED, DOWNLOADING, DOWNLOADED, ERROR }

    /** Progress info for model downloads. */
    data class DownloadProgress(val languageCode: String, val progress: Float)

    /** Translation result with metadata. */
    data class TranslationResult(
        val originalText: String,
        val translatedText: String,
        val sourceLang: String,
        val targetLang: String
    )

    /** 10+ commonly used languages supported by ML Kit. */
    val supportedLanguages = listOf(
        Language(TranslateLanguage.CHINESE, "中文"),
        Language(TranslateLanguage.ENGLISH, "English"),
        Language(TranslateLanguage.JAPANESE, "日本語"),
        Language(TranslateLanguage.KOREAN, "한국어"),
        Language(TranslateLanguage.FRENCH, "Français"),
        Language(TranslateLanguage.GERMAN, "Deutsch"),
        Language(TranslateLanguage.SPANISH, "Español"),
        Language(TranslateLanguage.RUSSIAN, "Русский"),
        Language(TranslateLanguage.ARABIC, "العربية"),
        Language(TranslateLanguage.PORTUGUESE, "Português"),
        Language(TranslateLanguage.ITALIAN, "Italiano"),
        Language(TranslateLanguage.THAI, "ไทย"),
        Language(TranslateLanguage.VIETNAMESE, "Tiếng Việt"),
        Language(TranslateLanguage.HINDI, "हिन्दी"),
        Language(TranslateLanguage.DUTCH, "Nederlands"),
        Language(TranslateLanguage.POLISH, "Polski"),
        Language(TranslateLanguage.TURKISH, "Türkçe"),
    )

    private val modelManager = RemoteModelManager.getInstance()

    // Track model download statuses
    private val _modelStatuses = MutableStateFlow<Map<String, ModelStatus>>(emptyMap())
    val modelStatuses: StateFlow<Map<String, ModelStatus>> = _modelStatuses.asStateFlow()

    // Download progress
    private val _downloadProgress = MutableStateFlow<DownloadProgress?>(null)
    val downloadProgress: StateFlow<DownloadProgress?> = _downloadProgress.asStateFlow()

    // Currently translating
    private val _isTranslating = MutableStateFlow(false)
    val isTranslating: StateFlow<Boolean> = _isTranslating.asStateFlow()

    init {
        refreshModelStatuses()
    }

    /**
     * Refresh the download status of all supported language models.
     */
    fun refreshModelStatuses() {
        val statuses = mutableMapOf<String, ModelStatus>()
        for (lang in supportedLanguages) {
            val model = TranslateRemoteModel.Builder(lang.code).build()
            try {
                // Check if model is available locally via the downloaded models list
                modelManager.getDownloadedModels(TranslateRemoteModel::class.java)
                    .addOnSuccessListener { models ->
                        val downloaded = models.any { it.language == lang.code }
                        _modelStatuses.value = _modelStatuses.value.toMutableMap().apply {
                            put(lang.code, if (downloaded) ModelStatus.DOWNLOADED else ModelStatus.NOT_DOWNLOADED)
                        }
                    }
                    .addOnFailureListener {
                        _modelStatuses.value = _modelStatuses.value.toMutableMap().apply {
                            put(lang.code, ModelStatus.NOT_DOWNLOADED)
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking model status for ${lang.code}", e)
                statuses[lang.code] = ModelStatus.NOT_DOWNLOADED
            }
        }
        if (statuses.isNotEmpty()) {
            _modelStatuses.value = statuses
        }
    }

    /**
     * Download a language model for offline use.
     * Returns a Flow that emits progress updates (0.0 to 1.0) and completes when done.
     */
    fun downloadModel(languageCode: String): Flow<Float> = callbackFlow {
        val model = TranslateRemoteModel.Builder(languageCode).build()

        _modelStatuses.value = _modelStatuses.value.toMutableMap().apply {
            put(languageCode, ModelStatus.DOWNLOADING)
        }
        _downloadProgress.value = DownloadProgress(languageCode, 0f)

        val conditions = DownloadConditions.Builder()
            .build()

        modelManager.download(model, conditions)
            .addOnSuccessListener {
                Log.d(TAG, "Model downloaded: $languageCode")
                _modelStatuses.value = _modelStatuses.value.toMutableMap().apply {
                    put(languageCode, ModelStatus.DOWNLOADED)
                }
                _downloadProgress.value = DownloadProgress(languageCode, 1f)
                trySend(1f)
                close()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Model download failed: $languageCode", e)
                _modelStatuses.value = _modelStatuses.value.toMutableMap().apply {
                    put(languageCode, ModelStatus.ERROR)
                }
                _downloadProgress.value = null
                close(e)
            }

        // Simulate progress updates since ML Kit doesn't provide granular progress
        val progressThread = Thread {
            var progress = 0f
            while (progress < 0.9f && !isClosedForSend) {
                try {
                    Thread.sleep(500)
                    progress += 0.05f
                    _downloadProgress.value = DownloadProgress(languageCode, progress.coerceAtMost(0.9f))
                    trySend(progress.coerceAtMost(0.9f))
                } catch (_: Exception) {
                    break
                }
            }
        }
        progressThread.start()

        awaitClose {
            progressThread.interrupt()
            _downloadProgress.value = null
        }
    }

    /**
     * Delete a downloaded language model to free up storage.
     */
    suspend fun deleteModel(languageCode: String): Result<Unit> {
        return try {
            val model = TranslateRemoteModel.Builder(languageCode).build()
            suspendCancellableCoroutine { cont ->
                modelManager.deleteDownloadedModel(model)
                    .addOnSuccessListener {
                        Log.d(TAG, "Model deleted: $languageCode")
                        _modelStatuses.value = _modelStatuses.value.toMutableMap().apply {
                            put(languageCode, ModelStatus.NOT_DOWNLOADED)
                        }
                        cont.resume(Result.success(Unit))
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Model delete failed: $languageCode", e)
                        cont.resume(Result.failure(e))
                    }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Translate text from source language to target language using ML Kit offline.
     * Both source and target language models must be downloaded first.
     */
    suspend fun translateText(
        text: String,
        sourceLang: String,
        targetLang: String
    ): Result<TranslationResult> {
        if (text.isBlank()) return Result.failure(IllegalArgumentException("Text is empty"))

        _isTranslating.value = true
        return try {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLang)
                .setTargetLanguage(targetLang)
                .build()
            val translator = Translation.getClient(options)

            val result = suspendCancellableCoroutine<String> { cont ->
                translator.translate(text)
                    .addOnSuccessListener { translated ->
                        cont.resume(translated)
                    }
                    .addOnFailureListener { e ->
                        cont.resumeWithException(e)
                    }
            }

            translator.close()
            _isTranslating.value = false
            Result.success(TranslationResult(text, result, sourceLang, targetLang))
        } catch (e: Exception) {
            _isTranslating.value = false
            Log.e(TAG, "Translation failed", e)
            Result.failure(e)
        }
    }

    /**
     * Check if both source and target language models are downloaded.
     */
    fun areModelsReady(sourceLang: String, targetLang: String): Boolean {
        val statuses = _modelStatuses.value
        return statuses[sourceLang] == ModelStatus.DOWNLOADED &&
                statuses[targetLang] == ModelStatus.DOWNLOADED
    }

    /**
     * Download multiple language models at once.
     */
    fun downloadModels(languageCodes: List<String>): Flow<Pair<String, Float>> = callbackFlow {
        for (code in languageCodes) {
            val model = TranslateRemoteModel.Builder(code).build()
            _modelStatuses.value = _modelStatuses.value.toMutableMap().apply {
                put(code, ModelStatus.DOWNLOADING)
            }

            val conditions = DownloadConditions.Builder().build()
            suspendCancellableCoroutine<Unit> { cont ->
                modelManager.download(model, conditions)
                    .addOnSuccessListener {
                        _modelStatuses.value = _modelStatuses.value.toMutableMap().apply {
                            put(code, ModelStatus.DOWNLOADED)
                        }
                        trySend(Pair(code, 1f))
                        cont.resume(Unit)
                    }
                    .addOnFailureListener { e ->
                        _modelStatuses.value = _modelStatuses.value.toMutableMap().apply {
                            put(code, ModelStatus.ERROR)
                        }
                        cont.resume(Unit) // Continue with next language
                    }
            }
        }
        close()
        awaitClose {}
    }

    /**
     * Get the display name for a language code.
     */
    fun getLanguageName(code: String): String {
        return supportedLanguages.find { it.code == code }?.displayName ?: code
    }

    /**
     * Get total size of downloaded models (approximate).
     */
    fun getDownloadedModelCount(): Int {
        return _modelStatuses.value.values.count { it == ModelStatus.DOWNLOADED }
    }

    companion object {
        @Volatile
        private var instance: TranslationManager? = null

        fun getInstance(context: Context): TranslationManager {
            return instance ?: synchronized(this) {
                instance ?: TranslationManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
