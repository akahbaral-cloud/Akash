package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AssistantOrbState
import com.example.ui.theme.AkashCyan
import com.example.ui.theme.AkashCyanDeep
import com.example.ui.theme.AuroraIndigo
import com.example.ui.theme.SkyNightBackground
import com.example.ui.theme.SolarAmber

@Composable
fun AkashVoiceOrb(
    orbState: AssistantOrbState,
    rmsNormalized: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "akash_orb")

    val breathPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath_phase"
    )

    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin_angle"
    )

    val speakPulse by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 520),
            repeatMode = RepeatMode.Reverse
        ),
        label = "speak_pulse"
    )

    val animatedRms by animateFloatAsState(
        targetValue = rmsNormalized.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = 500f),
        label = "animated_rms"
    )

    val primaryColor = when (orbState) {
        AssistantOrbState.IDLE -> AkashCyan
        AssistantOrbState.LISTENING -> AkashCyan
        AssistantOrbState.THINKING -> AuroraIndigo
        AssistantOrbState.SPEAKING -> SolarAmber
    }

    val secondaryColor = when (orbState) {
        AssistantOrbState.IDLE -> AuroraIndigo
        AssistantOrbState.LISTENING -> SolarAmber
        AssistantOrbState.THINKING -> AkashCyan
        AssistantOrbState.SPEAKING -> AkashCyan
    }

    val cdText = stringResource(R.string.cd_mic_orb)
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(132.dp)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.minDimension / 2f

            val dynamicEnergy = when (orbState) {
                AssistantOrbState.IDLE -> breathPhase * 0.22f
                AssistantOrbState.LISTENING -> (0.35f + animatedRms * 0.65f).coerceIn(0.2f, 1f)
                AssistantOrbState.THINKING -> 0.48f
                AssistantOrbState.SPEAKING -> speakPulse * 0.85f
            }

            // Outer ambient halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.26f + dynamicEnergy * 0.16f),
                        secondaryColor.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxRadius
                ),
                radius = maxRadius,
                center = center
            )

            // Expanding wave rings
            val ring1Radius = maxRadius * (0.64f + dynamicEnergy * 0.18f)
            drawCircle(
                color = primaryColor.copy(alpha = 0.35f + dynamicEnergy * 0.25f),
                radius = ring1Radius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            if (orbState == AssistantOrbState.LISTENING || orbState == AssistantOrbState.SPEAKING) {
                val ring2Radius = maxRadius * (0.76f + dynamicEnergy * 0.22f)
                drawCircle(
                    color = secondaryColor.copy(alpha = 0.24f + dynamicEnergy * 0.22f),
                    radius = ring2Radius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            if (orbState == AssistantOrbState.THINKING) {
                val arcRadius = maxRadius * 0.72f
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(Color.Transparent, AkashCyan, SolarAmber, Color.Transparent),
                        center = center
                    ),
                    startAngle = spinAngle,
                    sweepAngle = 220f,
                    useCenter = false,
                    topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                    size = Size(arcRadius * 2f, arcRadius * 2f),
                    style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(78.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    ambientColor = primaryColor,
                    spotColor = primaryColor
                )
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = when (orbState) {
                            AssistantOrbState.IDLE -> listOf(AkashCyan, AkashCyanDeep)
                            AssistantOrbState.LISTENING -> listOf(AkashCyan, AuroraIndigo)
                            AssistantOrbState.THINKING -> listOf(AuroraIndigo, AkashCyanDeep)
                            AssistantOrbState.SPEAKING -> listOf(SolarAmber, Color(0xFFD97706))
                        }
                    )
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true, color = Color.White),
                    role = Role.Button,
                    onClick = onClick
                )
                .semantics {
                    contentDescription = cdText
                }
                .testTag("mic_orb_button")
        ) {
            val icon = when (orbState) {
                AssistantOrbState.IDLE -> Icons.Rounded.Mic
                AssistantOrbState.LISTENING -> Icons.Rounded.Stop
                AssistantOrbState.THINKING -> Icons.Rounded.AutoAwesome
                AssistantOrbState.SPEAKING -> Icons.Rounded.GraphicEq
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SkyNightBackground,
                modifier = Modifier.size(34.dp)
            )
        }
    }
}
