package com.example.model

enum class VoiceLanguageMode(
    val displayName: String,
    val sttLocaleTag: String,
    val ttsLocaleTag: String
) {
    AUTO("Auto (Hindi / Hinglish / EN)", "en-IN", "hi-IN"),
    HINDI("Hindi (hi-IN)", "hi-IN", "hi-IN"),
    HINGLISH("Hinglish / India (en-IN)", "en-IN", "en-IN"),
    ENGLISH_US("English (en-US)", "en-US", "en-US")
}

enum class AssistantOrbState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

enum class DeviceActionType {
    NONE,
    FLASHLIGHT,
    VOLUME,
    SET_ALARM,
    SET_TIMER,
    OPEN_SETTINGS,
    DIAL_NUMBER,
    SEND_SMS,
    OPEN_CAMERA,
    MAPS_SEARCH,
    WEB_SEARCH,
    YOUTUBE_SEARCH,
    BATTERY_STATUS,
    OPEN_APP,
    CALENDAR_EVENT;

    companion object {
        fun fromString(raw: String?): DeviceActionType {
            if (raw.isNullOrBlank()) return NONE
            return entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) } ?: NONE
        }
    }
}

data class DeviceAction(
    val type: DeviceActionType,
    val label: String,
    val boolValue: Boolean? = null,
    val intValue: Int? = null,
    val intValue2: Int? = null,
    val textParam: String? = null
)

data class ActionExecutionResult(
    val executed: Boolean,
    val statusText: String
)

data class ChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val action: DeviceAction? = null,
    val actionResult: ActionExecutionResult? = null,
    val isError: Boolean = false
)

data class DeviceSnapshot(
    val batteryPercent: Int = -1,
    val isCharging: Boolean = false,
    val isFlashlightOn: Boolean = false,
    val mediaVolumePercent: Int = 50
)

data class AkashUiState(
    val messages: List<ChatMessage> = emptyList(),
    val orbState: AssistantOrbState = AssistantOrbState.IDLE,
    val rmsNormalized: Float = 0f,
    val partialTranscript: String = "",
    val speakingMessageId: String? = null,
    val deviceSnapshot: DeviceSnapshot = DeviceSnapshot(),
    val autoSpeakEnabled: Boolean = true,
    val languageMode: VoiceLanguageMode = VoiceLanguageMode.AUTO,
    val speechRate: Float = 1.0f,
    val inputText: String = "",
    val hasMicPermission: Boolean = false,
    val speechAvailable: Boolean = true,
    val errorBanner: String? = null,
    val lastActionBanner: String? = null
)
