package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AkashUiState
import com.example.model.AssistantOrbState
import com.example.model.ChatMessage
import com.example.model.DeviceAction
import com.example.ui.components.AkashVoiceOrb
import com.example.ui.components.PhoneControlsSheet
import com.example.ui.components.VoiceSettingsSheet
import com.example.ui.theme.AkashCyan
import com.example.ui.theme.AuroraIndigo
import com.example.ui.theme.AuroraIndigoContainer
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.SkyNightBackground
import com.example.ui.theme.SkySurface
import com.example.ui.theme.SkySurfaceContainer
import com.example.ui.theme.SkySurfaceElevated
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusErrorContainer
import com.example.ui.theme.StatusSuccess
import com.example.viewmodel.AkashViewModel

@Composable
fun AkashHomeScreen(
    uiState: AkashUiState,
    viewModel: AkashViewModel,
    onRequestMicPermission: () -> Unit
) {
    var showPhoneControlsSheet by remember { mutableStateOf(false) }
    var showVoiceSettingsSheet by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    Scaffold(
        containerColor = SkyNightBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            SkyNightBackground,
                            Color(0xFF0A1433),
                            SkyNightBackground
                        )
                    )
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
                    .statusBarsPadding()
            ) {
                // Top Header Bar
                AkashHeaderBar(
                    uiState = uiState,
                    onToggleFlashlight = { viewModel.toggleFlashlightQuick() },
                    onToggleAutoSpeak = { viewModel.toggleAutoSpeak() },
                    onOpenSettings = { showVoiceSettingsSheet = true }
                )

                // Mic permission prompt banner (non-blocking)
                AnimatedVisibility(visible = !uiState.hasMicPermission) {
                    MicPermissionBanner(
                        onGrantClick = onRequestMicPermission,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                // Error banner
                AnimatedVisibility(visible = uiState.errorBanner != null) {
                    uiState.errorBanner?.let { err ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = StatusErrorContainer,
                            border = BorderStroke(1.dp, StatusError.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = err,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.clearErrorBanner() },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Dismiss error",
                                        tint = StatusError
                                    )
                                }
                            }
                        }
                    }
                }

                // Main Stage (Welcome or Conversation Feed)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (uiState.messages.isEmpty()) {
                        AkashWelcomeStage(
                            onPromptSelected = { prompt -> viewModel.submitPrompt(prompt) },
                            onOpenControls = { showPhoneControlsSheet = true },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 10.dp,
                                bottom = 16.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.messages, key = { it.id }) { msg ->
                                ChatMessageCard(
                                    message = msg,
                                    isSpeakingThis = uiState.speakingMessageId == msg.id,
                                    onReplayOrStop = { viewModel.replayMessage(msg.id) },
                                    onExecuteAction = { action -> viewModel.executeActionDirect(action) }
                                )
                            }
                        }
                    }
                }

                // Bottom Voice & Text Dock
                AkashBottomDock(
                    uiState = uiState,
                    onMicClick = {
                        if (!uiState.hasMicPermission) {
                            onRequestMicPermission()
                        } else {
                            viewModel.onMicTapped()
                        }
                    },
                    onStopSpeaking = { viewModel.stopSpeaking() },
                    onOpenPhoneControls = { showPhoneControlsSheet = true },
                    onInputChange = { viewModel.onInputTextChanged(it) },
                    onSendInput = { viewModel.submitTypedInput() }
                )
            }
        }
    }

    if (showPhoneControlsSheet) {
        PhoneControlsSheet(
            snapshot = uiState.deviceSnapshot,
            onDismiss = { showPhoneControlsSheet = false },
            onToggleFlashlight = { viewModel.toggleFlashlightQuick() },
            onAdjustVolume = { up -> viewModel.adjustVolumeQuick(up) },
            onOpenSettings = { target -> viewModel.openSettingsQuick(target) },
            onCheckBattery = { viewModel.checkBatteryAndSpeak() },
            onExecuteDirect = { action -> viewModel.executeActionDirect(action) },
            onRunVoiceExample = { sample -> viewModel.submitPrompt(sample) }
        )
    }

    if (showVoiceSettingsSheet) {
        VoiceSettingsSheet(
            languageMode = uiState.languageMode,
            autoSpeakEnabled = uiState.autoSpeakEnabled,
            speechRate = uiState.speechRate,
            onDismiss = { showVoiceSettingsSheet = false },
            onSelectLanguage = { viewModel.setLanguageMode(it) },
            onToggleAutoSpeak = { viewModel.toggleAutoSpeak() },
            onSelectSpeechRate = { viewModel.setSpeechRate(it) },
            onClearChat = { viewModel.clearConversation() }
        )
    }
}

@Composable
private fun AkashHeaderBar(
    uiState: AkashUiState,
    onToggleFlashlight: () -> Unit,
    onToggleAutoSpeak: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val statusText = when (uiState.orbState) {
        AssistantOrbState.IDLE -> stringResource(R.string.status_idle)
        AssistantOrbState.LISTENING -> stringResource(R.string.status_listening)
        AssistantOrbState.THINKING -> stringResource(R.string.status_thinking)
        AssistantOrbState.SPEAKING -> stringResource(R.string.status_speaking)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Brand Badge
        Image(
            painter = painterResource(id = R.drawable.ic_akash_foreground_1795184631351),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelMedium,
                color = when (uiState.orbState) {
                    AssistantOrbState.LISTENING -> AkashCyan
                    AssistantOrbState.SPEAKING -> SolarAmber
                    AssistantOrbState.THINKING -> AuroraIndigo
                    AssistantOrbState.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Quick Battery Pill
        if (uiState.deviceSnapshot.batteryPercent >= 0) {
            Surface(
                shape = CircleShape,
                color = SkySurfaceContainer,
                border = BorderStroke(1.dp, OutlineSubtle)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.BatteryChargingFull,
                        contentDescription = null,
                        tint = StatusSuccess,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${uiState.deviceSnapshot.batteryPercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        // Flashlight Quick Toggle
        IconButton(
            onClick = onToggleFlashlight,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("header_flashlight_button")
        ) {
            Icon(
                imageVector = if (uiState.deviceSnapshot.isFlashlightOn) {
                    Icons.Rounded.FlashlightOn
                } else {
                    Icons.Rounded.FlashlightOff
                },
                contentDescription = "Toggle flashlight",
                tint = if (uiState.deviceSnapshot.isFlashlightOn) SolarAmber else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Auto-speak toggle
        IconButton(
            onClick = onToggleAutoSpeak,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("header_speak_toggle")
        ) {
            Icon(
                imageVector = if (uiState.autoSpeakEnabled) {
                    Icons.AutoMirrored.Rounded.VolumeUp
                } else {
                    Icons.AutoMirrored.Rounded.VolumeOff
                },
                contentDescription = stringResource(R.string.cd_speak_toggle),
                tint = if (uiState.autoSpeakEnabled) AkashCyan else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Settings button
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("header_settings_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = stringResource(R.string.cd_settings),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MicPermissionBanner(
    onGrantClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = SkySurfaceElevated,
        border = BorderStroke(1.dp, AkashCyan.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Mic,
                contentDescription = null,
                tint = AkashCyan,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.mic_permission_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.mic_permission_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onGrantClick,
                colors = ButtonDefaults.buttonColors(containerColor = AkashCyan, contentColor = SkyNightBackground),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("grant_mic_permission_button")
            ) {
                Text(text = stringResource(R.string.grant_mic_permission))
            }
        }
    }
}

@Composable
private fun AkashWelcomeStage(
    onPromptSelected: (String) -> Unit,
    onOpenControls: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Atmospheric Hero Card with bundled horizon art
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SkySurface,
            border = BorderStroke(1.dp, OutlineSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(id = R.drawable.img_akash_horizon_1795184665393),
                    contentDescription = stringResource(R.string.cd_horizon_art),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    SkyNightBackground.copy(alpha = 0.25f),
                                    SkyNightBackground.copy(alpha = 0.88f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = AkashCyan.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, AkashCyan.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = AkashCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "HINDI • HINGLISH • ENGLISH",
                                style = MaterialTheme.typography.labelSmall,
                                color = AkashCyan
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.welcome_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.welcome_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Suggestion Chips / Starter Cards
        Text(
            text = "Try bolkar ya tap karke puchhein",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val starterItems = listOf(
            "Flashlight on karo" to "Phone Control",
            "5 minute ka timer lagao" to "Clock & Timer",
            "Black hole kya hota hai? Aasan bhasha mein samjhao" to "Voice Q&A",
            "Wi-Fi settings kholo" to "Quick Settings"
        )

        starterItems.forEachIndexed { idx, (prompt, badge) ->
            Surface(
                onClick = { onPromptSelected(prompt) },
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("starter_prompt_$idx"),
                shape = RoundedCornerShape(16.dp),
                color = SkySurfaceContainer,
                border = BorderStroke(1.dp, OutlineSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = if (idx < 2 || idx == 3) SolarAmber else AkashCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SkySurfaceElevated
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        TextButton(
            onClick = onOpenControls,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .minimumInteractiveComponentSize()
                .testTag("open_all_controls_link")
        ) {
            Icon(
                imageVector = Icons.Rounded.Widgets,
                contentDescription = null,
                tint = AkashCyan,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Saare Phone Controls dekhein",
                color = AkashCyan
            )
        }
    }
}

@Composable
private fun ChatMessageCard(
    message: ChatMessage,
    isSpeakingThis: Boolean,
    onReplayOrStop: () -> Unit,
    onExecuteAction: (DeviceAction) -> Unit
) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 560.dp),
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (message.isUser) 20.dp else 6.dp,
                bottomEnd = if (message.isUser) 6.dp else 20.dp
            ),
            color = when {
                message.isError -> StatusErrorContainer
                message.isUser -> AuroraIndigoContainer
                else -> SkySurfaceContainer
            },
            border = BorderStroke(
                width = 1.dp,
                color = when {
                    message.isError -> StatusError.copy(alpha = 0.5f)
                    isSpeakingThis -> SolarAmber
                    message.isUser -> AuroraIndigo.copy(alpha = 0.45f)
                    else -> OutlineSubtle
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!message.isUser) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = if (isSpeakingThis) SolarAmber else AkashCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSpeakingThis) "AI Akash • Bol raha hai" else "AI Akash",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSpeakingThis) SolarAmber else AkashCyan
                            )
                        }

                        if (!message.isError) {
                            Surface(
                                onClick = onReplayOrStop,
                                shape = CircleShape,
                                color = if (isSpeakingThis) SolarAmber.copy(alpha = 0.2f) else SkySurfaceElevated,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("replay_voice_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isSpeakingThis) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                        contentDescription = if (isSpeakingThis) {
                                            stringResource(R.string.cd_stop_voice)
                                        } else {
                                            stringResource(R.string.cd_replay_voice)
                                        },
                                        tint = if (isSpeakingThis) SolarAmber else AkashCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isSpeakingThis) "Stop" else "Suno",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSpeakingThis) SolarAmber else AkashCyan
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Phone Control Execution Pill
                message.action?.let { action ->
                    val result = message.actionResult
                    Surface(
                        onClick = { onExecuteAction(action) },
                        shape = RoundedCornerShape(12.dp),
                        color = SkySurfaceElevated,
                        border = BorderStroke(
                            1.dp,
                            if (result?.executed == true) StatusSuccess.copy(alpha = 0.45f) else OutlineSubtle
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Bolt,
                                    contentDescription = null,
                                    tint = if (result?.executed == true) StatusSuccess else SolarAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = action.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    result?.let {
                                        Text(
                                            text = it.statusText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (it.executed) StatusSuccess else SolarAmber
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Run again",
                                style = MaterialTheme.typography.labelSmall,
                                color = AkashCyan
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AkashBottomDock(
    uiState: AkashUiState,
    onMicClick: () -> Unit,
    onStopSpeaking: () -> Unit,
    onOpenPhoneControls: () -> Unit,
    onInputChange: (String) -> Unit,
    onSendInput: () -> Unit
) {
    Surface(
        color = SkySurface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, OutlineSubtle),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Live partial transcript pill while listening
            AnimatedVisibility(
                visible = uiState.orbState == AssistantOrbState.LISTENING && uiState.partialTranscript.isNotBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = AkashCyan.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, AkashCyan.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "\"${uiState.partialTranscript}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AkashCyan,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            // Text question bar for typing anytime
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.inputText,
                    onValueChange = onInputChange,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.type_hint),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSendInput() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SkySurfaceContainer,
                        unfocusedContainerColor = SkySurfaceContainer,
                        focusedBorderColor = AkashCyan,
                        unfocusedBorderColor = OutlineSubtle,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("question_input")
                )

                FilledTonalIconButton(
                    onClick = onSendInput,
                    enabled = uiState.inputText.isNotBlank() && uiState.orbState != AssistantOrbState.THINKING,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = AkashCyan,
                        contentColor = SkyNightBackground
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = stringResource(R.string.cd_send_text)
                    )
                }
            }

            // Tactile Voice Orb Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Phone Controls Trigger
                Surface(
                    onClick = onOpenPhoneControls,
                    shape = RoundedCornerShape(16.dp),
                    color = SkySurfaceContainer,
                    border = BorderStroke(1.dp, OutlineSubtle),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("phone_controls_button")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Widgets,
                            contentDescription = stringResource(R.string.cd_phone_controls),
                            tint = AkashCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Controls",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Living Voice Orb
                AkashVoiceOrb(
                    orbState = uiState.orbState,
                    rmsNormalized = uiState.rmsNormalized,
                    onClick = onMicClick
                )

                // Stop Speaking / Audio Status Button
                val isSpeaking = uiState.orbState == AssistantOrbState.SPEAKING
                Surface(
                    onClick = { if (isSpeaking) onStopSpeaking() else onMicClick() },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSpeaking) SolarAmber.copy(alpha = 0.16f) else SkySurfaceContainer,
                    border = BorderStroke(1.dp, if (isSpeaking) SolarAmber else OutlineSubtle),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("stop_speaking_button")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = if (isSpeaking) {
                                stringResource(R.string.cd_stop_voice)
                            } else {
                                "Voice Ready"
                            },
                            tint = if (isSpeaking) SolarAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isSpeaking) "Chup" else "Voice",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSpeaking) SolarAmber else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
