package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.example.model.VoiceLanguageMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AkashSpeechEngine(
    private val context: Context,
    private val onFinalTranscript: (String) -> Unit,
    private val onSpeechError: (String) -> Unit
) {

    private companion object {
        const val TAG = "AkashSpeechEngine"
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _rmsNormalized = MutableStateFlow(0f)
    val rmsNormalized: StateFlow<Float> = _rmsNormalized.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    fun isRecognitionAvailable(): Boolean {
        return try {
            SpeechRecognizer.isRecognitionAvailable(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking speech recognition availability", e)
            false
        }
    }

    fun startListening(languageMode: VoiceLanguageMode) {
        mainHandler.post {
            if (!isRecognitionAvailable()) {
                _isListening.value = false
                onSpeechError("Is device par voice recognition service uplabdh nahi hai. Neeche type karke puchh sakte hain.")
                return@post
            }

            try {
                destroyRecognizerInternal()
                val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                speechRecognizer = recognizer

                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                        _partialTranscript.value = ""
                        _rmsNormalized.value = 0.1f
                    }

                    override fun onBeginningOfSpeech() {
                        _isListening.value = true
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                        _rmsNormalized.value = normalized
                    }

                    override fun onBufferReceived(buffer: ByteArray?) = Unit

                    override fun onEndOfSpeech() {
                        _rmsNormalized.value = 0f
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        _rmsNormalized.value = 0f
                        _partialTranscript.value = ""
                        val message = mapSpeechError(error)
                        Log.w(TAG, "SpeechRecognizer error=$error ($message)")
                        if (message != null) {
                            onSpeechError(message)
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        _rmsNormalized.value = 0f
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val transcript = matches?.firstOrNull()?.trim().orEmpty()
                        _partialTranscript.value = ""
                        if (transcript.isNotEmpty()) {
                            onFinalTranscript(transcript)
                        } else {
                            onSpeechError("Awaz samajh nahi aayi. Kripya dobara bolein.")
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val partial = partialResults
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            ?.trim()
                            .orEmpty()
                        if (partial.isNotEmpty()) {
                            _partialTranscript.value = partial
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) = Unit
                })

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageMode.sttLocaleTag)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
                }

                _isListening.value = true
                recognizer.startListening(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start listening", e)
                _isListening.value = false
                _rmsNormalized.value = 0f
                onSpeechError("Mic start nahi ho paya: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.w(TAG, "stopListening error", e)
            }
            _isListening.value = false
            _rmsNormalized.value = 0f
        }
    }

    fun cancelListening() {
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                Log.w(TAG, "cancelListening error", e)
            }
            _isListening.value = false
            _rmsNormalized.value = 0f
            _partialTranscript.value = ""
        }
    }

    fun release() {
        mainHandler.post {
            destroyRecognizerInternal()
        }
    }

    private fun destroyRecognizerInternal() {
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "destroy error", e)
        } finally {
            speechRecognizer = null
        }
    }

    private fun mapSpeechError(errorCode: Int): String? = when (errorCode) {
        SpeechRecognizer.ERROR_AUDIO -> "Audio record karne mein dikkat aayi."
        SpeechRecognizer.ERROR_CLIENT -> null
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission nahi mili."
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Voice recognition ke liye network connection check karein."
        SpeechRecognizer.ERROR_NO_MATCH -> "Awaz sunai nahi di, kripya mic dabakar phir bolein."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Mic abhi vyast hai, ek second baad try karein."
        SpeechRecognizer.ERROR_SERVER -> "Speech server error, kripya dobara try karein."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Koi awaz record nahi hui. Mic dabakar phir bolein."
        else -> "Voice input error ($errorCode)."
    }
}
