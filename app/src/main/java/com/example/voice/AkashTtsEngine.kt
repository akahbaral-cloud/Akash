package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.model.VoiceLanguageMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

class AkashTtsEngine(context: Context) : TextToSpeech.OnInitListener {

    private companion object {
        const val TAG = "AkashTtsEngine"
    }

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _speakingMessageId = MutableStateFlow<String?>(null)
    val speakingMessageId: StateFlow<String?> = _speakingMessageId.asStateFlow()

    private var pendingSpeak: Triple<String, String, VoiceLanguageMode>? = null
    private var currentSpeechRate: Float = 1.0f

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    // Clear speaking indicators only for the final chunk
                    if (utteranceId?.endsWith("_last") == true) {
                        _isSpeaking.value = false
                        _speakingMessageId.value = null
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    _speakingMessageId.value = null
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    Log.w(TAG, "TTS utterance errorCode=$errorCode")
                    _isSpeaking.value = false
                    _speakingMessageId.value = null
                }
            })
            pendingSpeak?.let { (id, text, mode) ->
                pendingSpeak = null
                speak(id, text, mode, currentSpeechRate)
            }
        } else {
            Log.e(TAG, "TextToSpeech init failed with status=$status")
        }
    }

    fun speak(
        messageId: String,
        rawText: String,
        languageMode: VoiceLanguageMode,
        speechRate: Float = currentSpeechRate
    ) {
        currentSpeechRate = speechRate.coerceIn(0.6f, 1.6f)
        val cleanText = sanitizeForSpeech(rawText)
        if (cleanText.isBlank()) return

        if (!isInitialized) {
            pendingSpeak = Triple(messageId, cleanText, languageMode)
            return
        }

        val engine = tts ?: return
        try {
            val locale = resolveLocale(cleanText, languageMode)
            val langResult = engine.setLanguage(locale)
            if (langResult == TextToSpeech.LANG_MISSING_DATA ||
                langResult == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                engine.language = Locale.forLanguageTag("en-IN")
            }
            engine.setSpeechRate(currentSpeechRate)
            engine.setPitch(1.0f)

            _speakingMessageId.value = messageId
            _isSpeaking.value = true

            val maxLen = TextToSpeech.getMaxSpeechInputLength().coerceAtLeast(1000)
            val chunks = splitIntoChunks(cleanText, maxLen - 100)
            chunks.forEachIndexed { index, chunk ->
                val queueMode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
                val suffix = if (index == chunks.lastIndex) "_last" else "_$index"
                val utteranceId = "${UUID.randomUUID()}$suffix"
                engine.speak(chunk, queueMode, null, utteranceId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to speak text", e)
            _isSpeaking.value = false
            _speakingMessageId.value = null
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "TTS stop failed", e)
        } finally {
            _isSpeaking.value = false
            _speakingMessageId.value = null
        }
    }

    fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.w(TAG, "TTS shutdown failed", e)
        } finally {
            tts = null
            isInitialized = false
            _isSpeaking.value = false
            _speakingMessageId.value = null
        }
    }

    private fun resolveLocale(text: String, mode: VoiceLanguageMode): Locale {
        return when (mode) {
            VoiceLanguageMode.AUTO -> {
                val hasDevanagari = text.any { it in '\u0900'..'\u097F' }
                if (hasDevanagari) Locale.forLanguageTag("hi-IN") else Locale.forLanguageTag("en-IN")
            }
            else -> Locale.forLanguageTag(mode.ttsLocaleTag)
        }
    }

    private fun sanitizeForSpeech(text: String): String {
        return text
            .replace("**", "")
            .replace("*", "")
            .replace("`", "")
            .replace("#", "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun splitIntoChunks(text: String, maxChunkSize: Int): List<String> {
        if (text.length <= maxChunkSize) return listOf(text)
        val result = mutableListOf<String>()
        var start = 0
        while (start < text.length) {
            var end = (start + maxChunkSize).coerceAtMost(text.length)
            if (end < text.length) {
                val lastPeriod = text.lastIndexOf('.', end)
                val lastSpace = text.lastIndexOf(' ', end)
                end = when {
                    lastPeriod > start -> lastPeriod + 1
                    lastSpace > start -> lastSpace
                    else -> end
                }
            }
            result.add(text.substring(start, end).trim())
            start = end
        }
        return result.filter { it.isNotBlank() }
    }
}
