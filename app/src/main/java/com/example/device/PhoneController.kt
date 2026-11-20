package com.example.device

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import com.example.model.ActionExecutionResult
import com.example.model.DeviceAction
import com.example.model.DeviceActionType
import com.example.model.DeviceSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PhoneController(context: Context) {

    private companion object {
        const val TAG = "PhoneController"
    }

    private val appContext = context.applicationContext
    private val cameraManager = appContext.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val batteryManager = appContext.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager

    private var torchCameraId: String? = null
    private var torchCallbackRegistered = false

    private val _isFlashlightOn = MutableStateFlow(false)
    val isFlashlightOn: StateFlow<Boolean> = _isFlashlightOn.asStateFlow()

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == torchCameraId) {
                _isFlashlightOn.value = enabled
            }
        }
    }

    init {
        try {
            val camIds = cameraManager?.cameraIdList.orEmpty()
            torchCameraId = camIds.firstOrNull { id ->
                val chars = cameraManager?.getCameraCharacteristics(id)
                chars?.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            if (torchCameraId != null) {
                cameraManager?.registerTorchCallback(torchCallback, Handler(Looper.getMainLooper()))
                torchCallbackRegistered = true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Torch setup unavailable on this device", e)
        }
    }

    fun readSnapshot(): DeviceSnapshot {
        val batteryIntent = try {
            appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        } catch (_: Exception) {
            null
        }
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val batteryPct = if (level >= 0 && scale > 0) {
            (level * 100) / scale
        } else {
            batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        }
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val mediaVolPct = readMediaVolumePercent()

        return DeviceSnapshot(
            batteryPercent = batteryPct,
            isCharging = isCharging,
            isFlashlightOn = _isFlashlightOn.value,
            mediaVolumePercent = mediaVolPct
        )
    }

    fun execute(action: DeviceAction): ActionExecutionResult {
        return when (action.type) {
            DeviceActionType.NONE -> ActionExecutionResult(false, "No action")
            DeviceActionType.FLASHLIGHT -> setFlashlight(action.boolValue ?: !_isFlashlightOn.value)
            DeviceActionType.VOLUME -> controlVolume(action)
            DeviceActionType.SET_ALARM -> setAlarm(action)
            DeviceActionType.SET_TIMER -> setTimer(action)
            DeviceActionType.OPEN_SETTINGS -> openSettings(action.textParam)
            DeviceActionType.DIAL_NUMBER -> dialNumber(action.textParam)
            DeviceActionType.SEND_SMS -> sendSms(action.textParam)
            DeviceActionType.OPEN_CAMERA -> openCamera()
            DeviceActionType.MAPS_SEARCH -> openMaps(action.textParam)
            DeviceActionType.YOUTUBE_SEARCH -> openYouTube(action.textParam)
            DeviceActionType.WEB_SEARCH -> openWebSearch(action.textParam)
            DeviceActionType.BATTERY_STATUS -> {
                val snap = readSnapshot()
                val chargingTxt = if (snap.isCharging) "Charging" else "Not charging"
                ActionExecutionResult(true, "Battery ${snap.batteryPercent}% ($chargingTxt)")
            }
            DeviceActionType.OPEN_APP -> openKnownApp(action.textParam)
            DeviceActionType.CALENDAR_EVENT -> createCalendarEvent(action.textParam)
        }
    }

    fun toggleFlashlight(): ActionExecutionResult {
        return setFlashlight(!_isFlashlightOn.value)
    }

    fun adjustVolumeStep(up: Boolean): ActionExecutionResult {
        val am = audioManager ?: return ActionExecutionResult(false, "Audio service unavailable")
        val direction = if (up) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
        val pct = readMediaVolumePercent()
        return ActionExecutionResult(true, "Media volume $pct%")
    }

    private fun setFlashlight(enable: Boolean): ActionExecutionResult {
        val camId = torchCameraId
            ?: return ActionExecutionResult(false, "Is device par flashlight hardware nahi mila")
        return try {
            cameraManager?.setTorchMode(camId, enable)
            _isFlashlightOn.value = enable
            ActionExecutionResult(true, if (enable) "Flashlight ON" else "Flashlight OFF")
        } catch (e: Exception) {
            Log.e(TAG, "setTorchMode failed", e)
            ActionExecutionResult(false, "Flashlight control failed: ${e.localizedMessage ?: "Error"}")
        }
    }

    private fun controlVolume(action: DeviceAction): ActionExecutionResult {
        val am = audioManager ?: return ActionExecutionResult(false, "Audio service unavailable")
        val mode = action.textParam?.uppercase()?.trim()
        return try {
            when {
                action.boolValue == true || mode == "MUTE" -> {
                    am.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_MUTE,
                        AudioManager.FLAG_SHOW_UI
                    )
                    ActionExecutionResult(true, "Media volume muted")
                }
                action.boolValue == false || mode == "UNMUTE" -> {
                    am.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_UNMUTE,
                        AudioManager.FLAG_SHOW_UI
                    )
                    ActionExecutionResult(true, "Media volume unmuted (${readMediaVolumePercent()}%)")
                }
                mode == "UP" -> adjustVolumeStep(up = true)
                mode == "DOWN" -> adjustVolumeStep(up = false)
                action.intValue != null -> {
                    val pct = action.intValue.coerceIn(0, 100)
                    val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    val target = ((pct / 100f) * maxVol).toInt().coerceIn(0, maxVol)
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
                    ActionExecutionResult(true, "Media volume $pct% set")
                }
                else -> adjustVolumeStep(up = true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Volume control failed", e)
            ActionExecutionResult(false, "Volume change nahi ho paya")
        }
    }

    private fun setAlarm(action: DeviceAction): ActionExecutionResult {
        val hour = (action.intValue ?: 7).coerceIn(0, 23)
        val minute = (action.intValue2 ?: 0).coerceIn(0, 59)
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, action.textParam ?: "AI Akash Alarm")
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val launched = launchSafe(intent)
        val timeFormatted = String.format("%02d:%02d", hour, minute)
        return if (launched) {
            ActionExecutionResult(true, "Alarm $timeFormatted set")
        } else {
            ActionExecutionResult(false, "Clock app nahi mila")
        }
    }

    private fun setTimer(action: DeviceAction): ActionExecutionResult {
        val seconds = (action.intValue ?: 60).coerceAtLeast(1)
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_MESSAGE, action.textParam ?: "AI Akash Timer")
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val launched = launchSafe(intent)
        val label = if (seconds >= 60) "${seconds / 60} min" else "$seconds sec"
        return if (launched) {
            ActionExecutionResult(true, "Timer $label started")
        } else {
            ActionExecutionResult(false, "Timer app nahi mila")
        }
    }

    fun openSettings(target: String?): ActionExecutionResult {
        val key = target?.lowercase()?.trim().orEmpty()
        val intentAction = when {
            key.contains("wifi") || key.contains("wi-fi") -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Settings.Panel.ACTION_WIFI
                } else {
                    Settings.ACTION_WIFI_SETTINGS
                }
            }
            key.contains("internet") || key.contains("data") || key.contains("network") -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Settings.Panel.ACTION_INTERNET_CONNECTIVITY
                } else {
                    Settings.ACTION_WIRELESS_SETTINGS
                }
            }
            key.contains("bluetooth") -> Settings.ACTION_BLUETOOTH_SETTINGS
            key.contains("display") || key.contains("bright") -> Settings.ACTION_DISPLAY_SETTINGS
            key.contains("sound") || key.contains("volume") -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Settings.Panel.ACTION_VOLUME
                } else {
                    Settings.ACTION_SOUND_SETTINGS
                }
            }
            key.contains("battery") -> Settings.ACTION_BATTERY_SAVER_SETTINGS
            key.contains("location") || key.contains("gps") -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }

        val intent = Intent(intentAction).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val ok = launchSafe(intent) || launchSafe(
            Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        return if (ok) {
            ActionExecutionResult(true, "Settings opened (${key.ifBlank { "general" }})")
        } else {
            ActionExecutionResult(false, "Settings open nahi ho payi")
        }
    }

    private fun dialNumber(rawNumber: String?): ActionExecutionResult {
        val cleaned = rawNumber?.filter { it.isDigit() || it == '+' }.orEmpty()
        val uri = if (cleaned.isNotEmpty()) Uri.parse("tel:$cleaned") else Uri.parse("tel:")
        val intent = Intent(Intent.ACTION_DIAL, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return if (launchSafe(intent)) {
            ActionExecutionResult(true, if (cleaned.isNotEmpty()) "Dialer: $cleaned" else "Dialer opened")
        } else {
            ActionExecutionResult(false, "Phone dialer app nahi mila")
        }
    }

    private fun sendSms(rawParam: String?): ActionExecutionResult {
        val parts = rawParam.orEmpty().split("|", limit = 2)
        val first = parts.getOrNull(0)?.trim().orEmpty()
        val second = parts.getOrNull(1)?.trim().orEmpty()
        val isPhone = first.isNotEmpty() && first.all { it.isDigit() || it == '+' || it == ' ' }
        val number = if (isPhone) first.filter { it.isDigit() || it == '+' } else ""
        val body = if (isPhone) second else rawParam.orEmpty()

        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number")).apply {
            if (body.isNotBlank()) putExtra("sms_body", body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (launchSafe(intent)) {
            ActionExecutionResult(true, "Messages opened")
        } else {
            ActionExecutionResult(false, "SMS app nahi mila")
        }
    }

    private fun openCamera(): ActionExecutionResult {
        val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return if (launchSafe(intent)) {
            ActionExecutionResult(true, "Camera opened")
        } else {
            ActionExecutionResult(false, "Camera app nahi mila")
        }
    }

    private fun openMaps(query: String?): ActionExecutionResult {
        val q = query?.trim().orEmpty()
        val uri = if (q.isNotEmpty()) {
            Uri.parse("geo:0,0?q=${Uri.encode(q)}")
        } else {
            Uri.parse("geo:0,0")
        }
        val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return if (launchSafe(intent)) {
            ActionExecutionResult(true, "Maps opened")
        } else {
            openWebSearch("maps $q")
        }
    }

    private fun openYouTube(query: String?): ActionExecutionResult {
        val q = query?.trim().orEmpty()
        val url = if (q.isNotEmpty()) {
            "https://www.youtube.com/results?search_query=${Uri.encode(q)}"
        } else {
            "https://www.youtube.com"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return if (launchSafe(intent)) {
            ActionExecutionResult(true, "YouTube opened")
        } else {
            ActionExecutionResult(false, "YouTube/Browser nahi mila")
        }
    }

    private fun openWebSearch(query: String?): ActionExecutionResult {
        val q = query?.trim().orEmpty()
        val url = "https://www.google.com/search?q=${Uri.encode(q)}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return if (launchSafe(intent)) {
            ActionExecutionResult(true, "Web search opened")
        } else {
            ActionExecutionResult(false, "Browser nahi mila")
        }
    }

    private fun createCalendarEvent(title: String?): ActionExecutionResult {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title ?: "AI Akash Reminder")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (launchSafe(intent)) {
            ActionExecutionResult(true, "Calendar event opened")
        } else {
            ActionExecutionResult(false, "Calendar app nahi mila")
        }
    }

    private fun openKnownApp(appName: String?): ActionExecutionResult {
        val key = appName?.lowercase()?.trim().orEmpty()
        val pkg = when {
            key.contains("youtube") -> "com.google.android.youtube"
            key.contains("whatsapp") -> "com.whatsapp"
            key.contains("chrome") || key.contains("browser") -> "com.android.chrome"
            key.contains("map") -> "com.google.android.apps.maps"
            key.contains("clock") || key.contains("alarm") -> "com.google.android.deskclock"
            key.contains("spotify") -> "com.spotify.music"
            key.contains("setting") -> return openSettings("general")
            key.contains("camera") -> return openCamera()
            key.contains("phone") || key.contains("dial") -> return dialNumber(null)
            key.contains("message") || key.contains("sms") -> return sendSms(null)
            else -> null
        }

        if (pkg != null) {
            val launchIntent = appContext.packageManager.getLaunchIntentForPackage(pkg)
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (launchIntent != null && launchSafe(launchIntent)) {
                return ActionExecutionResult(true, "${appName ?: "App"} opened")
            }
        }
        return ActionExecutionResult(false, "${appName ?: "App"} is device par install nahi mila")
    }

    private fun readMediaVolumePercent(): Int {
        val am = audioManager ?: return 50
        val current = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return ((current * 100f) / max).toInt().coerceIn(0, 100)
    }

    private fun launchSafe(intent: Intent): Boolean {
        return try {
            appContext.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "No activity found for intent: ${intent.action}", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed launching intent: ${intent.action}", e)
            false
        }
    }

    fun release() {
        if (torchCallbackRegistered) {
            try {
                cameraManager?.unregisterTorchCallback(torchCallback)
            } catch (_: Exception) {
            }
            torchCallbackRegistered = false
        }
    }
}
