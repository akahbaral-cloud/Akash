package com.example.ai

import android.util.Log
import com.example.model.DeviceAction
import com.example.model.DeviceActionType
import com.example.model.DeviceSnapshot
import com.example.model.VoiceLanguageMode
import com.google.firebase.Firebase
import com.google.firebase.ai.Chat
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AkashAiReply(
    val spokenReply: String,
    val deviceAction: DeviceAction?
)

class AkashAiRepository {

    private companion object {
        const val TAG = "AkashAiRepository"
        const val MODEL_NAME = "gemini-3.8-flash"
        const val TIMEOUT_MS = 60_000L
    }

    private val actionSchema = Schema.obj(
        properties = mapOf(
            "type" to Schema.enumeration(
                values = DeviceActionType.entries.map { it.name },
                description = "Phone control action to execute, or NONE if this is a normal question."
            ),
            "label" to Schema.string(
                description = "Short 2-4 word label for the UI pill, e.g. 'Flashlight ON', 'Timer 5 min', 'Wi-Fi Settings'."
            ),
            "boolValue" to Schema.boolean(
                description = "For FLASHLIGHT: true=ON, false=OFF. For VOLUME: true=mute, false=unmute.",
                nullable = true
            ),
            "intValue" to Schema.integer(
                description = "For VOLUME: target volume percentage 0-100 (or negative/positive delta if textParam='DELTA'). For SET_ALARM: 24-hour (0-23). For SET_TIMER: duration in seconds.",
                nullable = true
            ),
            "intValue2" to Schema.integer(
                description = "For SET_ALARM: minute (0-59).",
                nullable = true
            ),
            "textParam" to Schema.string(
                description = "Parameter text: settings target ('wifi','bluetooth','display','sound','battery','location','general'), phone number for DIAL_NUMBER/SEND_SMS, search query for MAPS_SEARCH/WEB_SEARCH/YOUTUBE_SEARCH, app name for OPEN_APP, event title for CALENDAR_EVENT, or 'UP'/'DOWN'/'MUTE'/'UNMUTE' for VOLUME.",
                nullable = true
            )
        ),
        optionalProperties = listOf("boolValue", "intValue", "intValue2", "textParam")
    )

    private val responseSchema = Schema.obj(
        properties = mapOf(
            "spokenReply" to Schema.string(
                description = "Clear, warm, spoken-ready explanation or confirmation in the user's language (Hindi, Hinglish, or English). No markdown symbols like *, #, or backticks."
            ),
            "deviceAction" to actionSchema
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = this@AkashAiRepository.responseSchema
                temperature = 0.7f
            },
            systemInstruction = content {
                text(
                    """
                    You are AI Akash (एआई आकाश), a smart, warm, conversational Android voice assistant.
                    
                    Language rules:
                    - Automatically match the user's language and style.
                    - If the user asks in Hindi or Hinglish (e.g. "Mausam kaisa hai", "Gravity kya hoti hai", "Flashlight jalao"), respond naturally in clear conversational Hinglish or simple Hindi so Text-to-Speech sounds great.
                    - If the user asks in English, respond in clear, friendly English.
                    - Never use markdown formatting (*, **, #, backticks, bullet symbols) in spokenReply because spokenReply is read aloud by Text-to-Speech. Keep explanations crisp, structured in natural sentences, and easy to understand by listening.
                    
                    Phone Control rules:
                    - You can control the user's Android phone! Always examine whether the user wants a phone action:
                      * FLASHLIGHT: turn torch on/off (boolValue = true/false).
                      * VOLUME: adjust or set volume. If user says "awaaz badhao / volume up", set textParam = "UP"; if "kam karo / volume down", set textParam = "DOWN"; if "mute", boolValue = true; if specific %, set intValue = 0..100.
                      * SET_ALARM: set alarm at intValue (hour 0..23) and intValue2 (minute 0..59), optional textParam label.
                      * SET_TIMER: set countdown timer of intValue seconds (e.g. 5 minutes -> 300), optional textParam label.
                      * OPEN_SETTINGS: open phone settings panel. textParam one of: "wifi", "bluetooth", "display", "sound", "battery", "location", "internet", "general".
                      * DIAL_NUMBER: open phone dialer with number in textParam (digits/plus).
                      * SEND_SMS: open SMS composer; textParam can be "number|message" or just message/number.
                      * OPEN_CAMERA: open camera app.
                      * MAPS_SEARCH: search location or directions in Google Maps (query in textParam).
                      * YOUTUBE_SEARCH: search/play topic on YouTube (query in textParam).
                      * WEB_SEARCH: search Google on browser (query in textParam).
                      * BATTERY_STATUS: report current battery status from live device context.
                      * OPEN_APP: open an app by name in textParam (e.g. "WhatsApp", "Chrome", "YouTube", "Maps", "Settings", "Clock", "Spotify").
                      * CALENDAR_EVENT: create a calendar reminder/event (title in textParam).
                    - If no phone control is requested, set deviceAction.type to "NONE" and label to "Answer".
                    """.trimIndent()
                )
            }
        )
    }

    private var chatSession: Chat? = null

    private fun ensureChat(): Chat {
        return chatSession ?: model.startChat().also { chatSession = it }
    }

    fun resetConversation() {
        chatSession = model.startChat()
    }

    suspend fun askAkash(
        userQuery: String,
        deviceSnapshot: DeviceSnapshot,
        languageMode: VoiceLanguageMode
    ): AkashAiReply {
        val now = SimpleDateFormat("hh:mm a, EEEE dd MMM yyyy", Locale.getDefault()).format(Date())
        val languageHint = when (languageMode) {
            VoiceLanguageMode.HINDI -> "Preferred reply language: Hindi (Devanagari or simple Hindi)."
            VoiceLanguageMode.HINGLISH -> "Preferred reply language: Hinglish (conversational Hindi in Latin script)."
            VoiceLanguageMode.ENGLISH_US -> "Preferred reply language: English."
            VoiceLanguageMode.AUTO -> "Match the user's language (Hindi, Hinglish, or English)."
        }

        val enrichedPrompt = """
            [Device Context]
            Local Time: $now
            Battery: ${deviceSnapshot.batteryPercent}% (${if (deviceSnapshot.isCharging) "Charging" else "Not charging"})
            Flashlight currently: ${if (deviceSnapshot.isFlashlightOn) "ON" else "OFF"}
            Media Volume: ${deviceSnapshot.mediaVolumePercent}%
            $languageHint
            
            User said: $userQuery
        """.trimIndent()

        return try {
            val response = withTimeout(TIMEOUT_MS) {
                ensureChat().sendMessage(enrichedPrompt)
            }
            val rawJson = response.text
                ?: throw IllegalStateException("AI Akash returned an empty response.")

            parseReplyJson(rawJson)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "askAkash failed", e)
            throw e
        }
    }

    private fun parseReplyJson(jsonString: String): AkashAiReply {
        val root = JSONObject(jsonString)
        val spokenReply = root.optString("spokenReply").trim()
        if (spokenReply.isEmpty()) {
            throw IllegalStateException("AI Akash reply was empty.")
        }

        val actionObj = root.optJSONObject("deviceAction")
        val actionType = DeviceActionType.fromString(actionObj?.optString("type"))

        val action = if (actionObj != null && actionType != DeviceActionType.NONE) {
            DeviceAction(
                type = actionType,
                label = actionObj.optString("label").ifBlank { actionType.name },
                boolValue = if (actionObj.has("boolValue") && !actionObj.isNull("boolValue")) {
                    actionObj.getBoolean("boolValue")
                } else null,
                intValue = if (actionObj.has("intValue") && !actionObj.isNull("intValue")) {
                    actionObj.getInt("intValue")
                } else null,
                intValue2 = if (actionObj.has("intValue2") && !actionObj.isNull("intValue2")) {
                    actionObj.getInt("intValue2")
                } else null,
                textParam = if (actionObj.has("textParam") && !actionObj.isNull("textParam")) {
                    actionObj.optString("textParam").takeIf { it.isNotBlank() }
                } else null
            )
        } else {
            null
        }

        return AkashAiReply(
            spokenReply = spokenReply,
            deviceAction = action
        )
    }
}
