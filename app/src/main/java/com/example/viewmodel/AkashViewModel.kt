package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AkashAiRepository
import com.example.device.PhoneController
import com.example.model.ActionExecutionResult
import com.example.model.AkashUiState
import com.example.model.AssistantOrbState
import com.example.model.ChatMessage
import com.example.model.DeviceAction
import com.example.model.DeviceActionType
import com.example.model.VoiceLanguageMode
import com.example.voice.AkashSpeechEngine
import com.example.voice.AkashTtsEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class AkashViewModel(application: Application) : AndroidViewModel(application) {

    private companion object {
        const val TAG = "AkashViewModel"
    }

    private val aiRepository = AkashAiRepository()
    private val phoneController = PhoneController(application)
    private val ttsEngine = AkashTtsEngine(application)
    private val speechEngine = AkashSpeechEngine(
        context = application,
        onFinalTranscript = { transcript ->
            submitPrompt(transcript)
        },
        onSpeechError = { errMsg ->
            _uiState.update {
                it.copy(
                    errorBanner = errMsg,
                    orbState = if (ttsEngine.isSpeaking.value) AssistantOrbState.SPEAKING else AssistantOrbState.IDLE
                )
            }
        }
    )

    private val _uiState: MutableStateFlow<AkashUiState> = MutableStateFlow(
        AkashUiState(
            deviceSnapshot = phoneController.readSnapshot()
        )
    )
    val uiState: StateFlow<AkashUiState> = _uiState.asStateFlow()

    private var isThinking = false

    init {
        _uiState.update { it.copy(speechAvailable = speechEngine.isRecognitionAvailable()) }

        viewModelScope.launch {
            val speechTriple = combine(
                speechEngine.isListening,
                speechEngine.rmsNormalized,
                speechEngine.partialTranscript
            ) { listening, rms, partial -> Triple(listening, rms, partial) }

            val ttsPair = combine(
                ttsEngine.isSpeaking,
                ttsEngine.speakingMessageId
            ) { speaking, speakingId -> speaking to speakingId }

            combine(
                speechTriple,
                ttsPair,
                phoneController.isFlashlightOn
            ) { (listening, rms, partial), (speaking, speakingId), flashOn ->
                val orb = when {
                    listening -> AssistantOrbState.LISTENING
                    isThinking -> AssistantOrbState.THINKING
                    speaking -> AssistantOrbState.SPEAKING
                    else -> AssistantOrbState.IDLE
                }
                _uiState.update { state ->
                    state.copy(
                        orbState = orb,
                        rmsNormalized = rms,
                        partialTranscript = partial,
                        speakingMessageId = speakingId,
                        deviceSnapshot = phoneController.readSnapshot().copy(isFlashlightOn = flashOn)
                    )
                }
            }.collect {}
        }
    }

    fun updateMicPermission(granted: Boolean) {
        _uiState.update { it.copy(hasMicPermission = granted) }
    }

    fun refreshSnapshot() {
        _uiState.update { it.copy(deviceSnapshot = phoneController.readSnapshot()) }
    }

    fun onInputTextChanged(newText: String) {
        _uiState.update { it.copy(inputText = newText) }
    }

    fun clearErrorBanner() {
        _uiState.update { it.copy(errorBanner = null) }
    }

    fun clearActionBanner() {
        _uiState.update { it.copy(lastActionBanner = null) }
    }

    fun onMicTapped() {
        clearErrorBanner()
        if (ttsEngine.isSpeaking.value) {
            ttsEngine.stop()
        }
        if (speechEngine.isListening.value) {
            speechEngine.stopListening()
        } else {
            speechEngine.startListening(_uiState.value.languageMode)
        }
    }

    fun submitTypedInput() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return
        _uiState.update { it.copy(inputText = "") }
        submitPrompt(text)
    }

    fun submitPrompt(promptText: String) {
        val trimmed = promptText.trim()
        if (trimmed.isEmpty()) return

        clearErrorBanner()
        if (ttsEngine.isSpeaking.value) {
            ttsEngine.stop()
        }
        if (speechEngine.isListening.value) {
            speechEngine.cancelListening()
        }

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            text = trimmed
        )

        isThinking = true
        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                orbState = AssistantOrbState.THINKING,
                deviceSnapshot = phoneController.readSnapshot()
            )
        }

        viewModelScope.launch {
            try {
                val snapshot = phoneController.readSnapshot()
                val reply = aiRepository.askAkash(
                    userQuery = trimmed,
                    deviceSnapshot = snapshot,
                    languageMode = _uiState.value.languageMode
                )

                val actionResult = reply.deviceAction?.let { action ->
                    phoneController.execute(action)
                }

                val aiMessageId = UUID.randomUUID().toString()
                val aiMessage = ChatMessage(
                    id = aiMessageId,
                    isUser = false,
                    text = reply.spokenReply,
                    action = reply.deviceAction,
                    actionResult = actionResult
                )

                isThinking = false
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages + aiMessage,
                        orbState = if (state.autoSpeakEnabled) AssistantOrbState.SPEAKING else AssistantOrbState.IDLE,
                        deviceSnapshot = phoneController.readSnapshot(),
                        lastActionBanner = actionResult?.statusText
                    )
                }

                if (_uiState.value.autoSpeakEnabled) {
                    ttsEngine.speak(
                        messageId = aiMessageId,
                        rawText = reply.spokenReply,
                        languageMode = _uiState.value.languageMode,
                        speechRate = _uiState.value.speechRate
                    )
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini request timed out", e)
                appendErrorMessage("Request timed out after 60 seconds. Kripya dubara koshish karein.")
            } catch (e: CancellationException) {
                isThinking = false
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Akash inference error", e)
                appendErrorMessage(
                    e.localizedMessage ?: "AI Akash se connect nahi ho paya. Kripya dubara try karein."
                )
            }
        }
    }

    private fun appendErrorMessage(message: String) {
        isThinking = false
        val errorMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = false,
            text = message,
            isError = true
        )
        _uiState.update {
            it.copy(
                messages = it.messages + errorMsg,
                orbState = AssistantOrbState.IDLE,
                errorBanner = message
            )
        }
    }

    fun replayMessage(messageId: String) {
        val msg = _uiState.value.messages.firstOrNull { it.id == messageId } ?: return
        if (_uiState.value.speakingMessageId == messageId) {
            ttsEngine.stop()
        } else {
            ttsEngine.speak(
                messageId = messageId,
                rawText = msg.text,
                languageMode = _uiState.value.languageMode,
                speechRate = _uiState.value.speechRate
            )
        }
    }

    fun stopSpeaking() {
        ttsEngine.stop()
    }

    fun toggleAutoSpeak() {
        val newValue = !_uiState.value.autoSpeakEnabled
        if (!newValue) {
            ttsEngine.stop()
        }
        _uiState.update { it.copy(autoSpeakEnabled = newValue) }
    }

    fun setLanguageMode(mode: VoiceLanguageMode) {
        _uiState.update { it.copy(languageMode = mode) }
    }

    fun setSpeechRate(rate: Float) {
        _uiState.update { it.copy(speechRate = rate.coerceIn(0.75f, 1.4f)) }
    }

    fun toggleFlashlightQuick(): ActionExecutionResult {
        val res = phoneController.toggleFlashlight()
        _uiState.update {
            it.copy(
                deviceSnapshot = phoneController.readSnapshot(),
                lastActionBanner = res.statusText
            )
        }
        return res
    }

    fun adjustVolumeQuick(up: Boolean): ActionExecutionResult {
        val res = phoneController.adjustVolumeStep(up)
        _uiState.update {
            it.copy(
                deviceSnapshot = phoneController.readSnapshot(),
                lastActionBanner = res.statusText
            )
        }
        return res
    }

    fun executeActionDirect(action: DeviceAction): ActionExecutionResult {
        val res = phoneController.execute(action)
        _uiState.update {
            it.copy(
                deviceSnapshot = phoneController.readSnapshot(),
                lastActionBanner = res.statusText
            )
        }
        return res
    }

    fun openSettingsQuick(target: String) {
        val res = phoneController.openSettings(target)
        _uiState.update { it.copy(lastActionBanner = res.statusText) }
    }

    fun checkBatteryAndSpeak() {
        val snap = phoneController.readSnapshot()
        val chargingLabel = if (snap.isCharging) "aur phone charge ho raha hai" else "aur abhi charging par nahi hai"
        val spoken = "Aapke phone ki battery ${snap.batteryPercent} percent hai, $chargingLabel."
        val msgId = UUID.randomUUID().toString()
        val msg = ChatMessage(
            id = msgId,
            isUser = false,
            text = spoken,
            action = DeviceAction(DeviceActionType.BATTERY_STATUS, "Battery ${snap.batteryPercent}%"),
            actionResult = ActionExecutionResult(true, "Battery ${snap.batteryPercent}%")
        )
        _uiState.update {
            it.copy(
                messages = it.messages + msg,
                deviceSnapshot = snap,
                lastActionBanner = "Battery ${snap.batteryPercent}%"
            )
        }
        if (_uiState.value.autoSpeakEnabled) {
            ttsEngine.speak(msgId, spoken, _uiState.value.languageMode, _uiState.value.speechRate)
        }
    }

    fun clearConversation() {
        ttsEngine.stop()
        speechEngine.cancelListening()
        isThinking = false
        aiRepository.resetConversation()
        _uiState.update {
            it.copy(
                messages = emptyList(),
                orbState = AssistantOrbState.IDLE,
                errorBanner = null,
                lastActionBanner = null
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechEngine.release()
        ttsEngine.release()
        phoneController.release()
    }
}
