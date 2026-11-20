package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.DeviceAction
import com.example.model.DeviceActionType
import com.example.model.DeviceSnapshot
import com.example.model.VoiceLanguageMode
import com.example.ui.theme.AkashCyan
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.SkySurface
import com.example.ui.theme.SkySurfaceContainer
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.StatusError

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PhoneControlsSheet(
    snapshot: DeviceSnapshot,
    onDismiss: () -> Unit,
    onToggleFlashlight: () -> Unit,
    onAdjustVolume: (Boolean) -> Unit,
    onOpenSettings: (String) -> Unit,
    onCheckBattery: () -> Unit,
    onExecuteDirect: (DeviceAction) -> Unit,
    onRunVoiceExample: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SkySurface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.phone_controls_title),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Tap karein ya Akash ko bolkar ye commands dein",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Quick tactile grid (2 columns)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickControlTile(
                        icon = if (snapshot.isFlashlightOn) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff,
                        title = if (snapshot.isFlashlightOn) "Flashlight ON" else "Flashlight OFF",
                        highlighted = snapshot.isFlashlightOn,
                        onClick = onToggleFlashlight,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("control_flashlight")
                    )
                    QuickControlTile(
                        icon = Icons.Rounded.BatteryChargingFull,
                        title = "Battery ${snapshot.batteryPercent}%",
                        highlighted = false,
                        onClick = {
                            onCheckBattery()
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("control_battery")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickControlTile(
                        icon = Icons.AutoMirrored.Rounded.VolumeDown,
                        title = "Volume - (${snapshot.mediaVolumePercent}%)",
                        highlighted = false,
                        onClick = { onAdjustVolume(false) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("control_volume_down")
                    )
                    QuickControlTile(
                        icon = Icons.AutoMirrored.Rounded.VolumeUp,
                        title = "Volume +",
                        highlighted = false,
                        onClick = { onAdjustVolume(true) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("control_volume_up")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickControlTile(
                        icon = Icons.Rounded.Wifi,
                        title = "Wi-Fi Panel",
                        highlighted = false,
                        onClick = { onOpenSettings("wifi") },
                        modifier = Modifier.weight(1f)
                    )
                    QuickControlTile(
                        icon = Icons.Rounded.Bluetooth,
                        title = "Bluetooth",
                        highlighted = false,
                        onClick = { onOpenSettings("bluetooth") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickControlTile(
                        icon = Icons.Rounded.BrightnessHigh,
                        title = "Display",
                        highlighted = false,
                        onClick = { onOpenSettings("display") },
                        modifier = Modifier.weight(1f)
                    )
                    QuickControlTile(
                        icon = Icons.Rounded.CameraAlt,
                        title = "Camera",
                        highlighted = false,
                        onClick = {
                            onExecuteDirect(DeviceAction(DeviceActionType.OPEN_CAMERA, "Camera"))
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickControlTile(
                        icon = Icons.Rounded.Timer,
                        title = "5 Min Timer",
                        highlighted = false,
                        onClick = {
                            onExecuteDirect(
                                DeviceAction(
                                    type = DeviceActionType.SET_TIMER,
                                    label = "5 min timer",
                                    intValue = 300,
                                    textParam = "AI Akash Timer"
                                )
                            )
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    QuickControlTile(
                        icon = Icons.Rounded.Phone,
                        title = "Dialer",
                        highlighted = false,
                        onClick = {
                            onExecuteDirect(DeviceAction(DeviceActionType.DIAL_NUMBER, "Dialer"))
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            HorizontalDivider(color = OutlineSubtle)

            Text(
                text = "Voice se aise bolein:",
                style = MaterialTheme.typography.titleMedium
            )

            val examples = listOf(
                "Flashlight on karo",
                "Subah 6:30 ka alarm lagao",
                "Volume 80 percent karo",
                "YouTube par lo-fi music kholo",
                "Near me petrol pump dikhao"
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                examples.forEach { sample ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            onRunVoiceExample(sample)
                            onDismiss()
                        },
                        label = { Text(sample, style = MaterialTheme.typography.labelMedium) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Alarm,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = SkySurfaceContainer,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickControlTile(
    icon: ImageVector,
    title: String,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.minimumInteractiveComponentSize(),
        shape = RoundedCornerShape(16.dp),
        color = if (highlighted) AkashCyan.copy(alpha = 0.16f) else SkySurfaceContainer,
        border = BorderStroke(
            width = 1.dp,
            color = if (highlighted) AkashCyan else OutlineSubtle
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (highlighted) SolarAmber else AkashCyan,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VoiceSettingsSheet(
    languageMode: VoiceLanguageMode,
    autoSpeakEnabled: Boolean,
    speechRate: Float,
    onDismiss: () -> Unit,
    onSelectLanguage: (VoiceLanguageMode) -> Unit,
    onToggleAutoSpeak: () -> Unit,
    onSelectSpeechRate: (Float) -> Unit,
    onClearChat: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SkySurface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = null,
                    tint = AkashCyan
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.voice_settings_title),
                    style = MaterialTheme.typography.titleLarge
                )
            }

            // Auto-speak switch
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SkySurfaceContainer,
                border = BorderStroke(1.dp, OutlineSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = stringResource(R.string.auto_speak_label),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.auto_speak_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoSpeakEnabled,
                        onCheckedChange = { onToggleAutoSpeak() },
                        modifier = Modifier.testTag("auto_speak_switch")
                    )
                }
            }

            // Language selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Bhasha (Voice Input & Reply Language)",
                    style = MaterialTheme.typography.titleMedium
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VoiceLanguageMode.entries.forEach { mode ->
                        FilterChip(
                            selected = languageMode == mode,
                            onClick = { onSelectLanguage(mode) },
                            label = { Text(mode.displayName) }
                        )
                    }
                }
            }

            // Speaking speed
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Bolne ki Speed (TTS Rate)",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val speeds = listOf(0.85f to "0.85x Slow", 1.0f to "1.0x Normal", 1.2f to "1.2x Fast")
                    speeds.forEach { (rate, label) ->
                        FilterChip(
                            selected = kotlin.math.abs(speechRate - rate) < 0.05f,
                            onClick = { onSelectSpeechRate(rate) },
                            label = { Text(label) }
                        )
                    }
                }
            }

            HorizontalDivider(color = OutlineSubtle)

            OutlinedButton(
                onClick = {
                    onClearChat()
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("clear_chat_button"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                border = BorderStroke(1.dp, StatusError.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.clear_chat))
            }
        }
    }
}
