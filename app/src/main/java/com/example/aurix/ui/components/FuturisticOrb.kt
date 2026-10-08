package com.example.aurix.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AurixAmber
import com.example.ui.theme.AurixCrimson
import com.example.ui.theme.AurixCyan
import com.example.ui.theme.AurixElectricBlue
import com.example.ui.theme.AurixEmerald
import com.example.ui.theme.AurixViolet
import com.example.ui.theme.AurixVoid
import com.example.aurix.voice.VoiceState
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FuturisticOrb(
    voiceState: VoiceState,
    rmsLevel: Float, // 0f..1f
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbPulse")

    // Breathing pulse
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    // Orbital ring rotation
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                when (voiceState) {
                    VoiceState.THINKING, VoiceState.EXECUTING -> 2000
                    VoiceState.SPEAKING -> 4000
                    else -> 8000
                },
                easing = LinearEasing
            )
        ),
        label = "OrbRotation"
    )

    val (primaryColor: Color, secondaryColor: Color) = when (voiceState) {
        VoiceState.IDLE -> Pair(AurixCyan, AurixElectricBlue)
        VoiceState.LISTENING -> Pair(AurixEmerald, AurixCyan)
        VoiceState.THINKING -> Pair(AurixViolet, AurixCyan)
        VoiceState.EXECUTING -> Pair(AurixAmber, AurixCyan)
        VoiceState.SPEAKING -> Pair(AurixViolet, AurixElectricBlue)
        VoiceState.INTERRUPTED -> Pair(AurixAmber, AurixEmerald)
        VoiceState.ERROR -> Pair(AurixCrimson, AurixAmber)
        VoiceState.PERMISSION_REQUIRED -> Pair(AurixAmber, AurixCrimson)
    }

    Box(
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val baseRadius = (this.size.minDimension / 2) * 0.72f
            val audioBoost = rmsLevel * 25f
            val dynamicRadius = (baseRadius * pulse) + audioBoost

            // Outer atmospheric energy glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.35f), Color.Transparent),
                    center = center,
                    radius = dynamicRadius * 1.5f
                ),
                radius = dynamicRadius * 1.5f,
                center = center
            )

            // Outer Orbit Ring 1
            rotate(rotation, pivot = center) {
                drawCircle(
                    color = primaryColor.copy(alpha = 0.5f),
                    radius = dynamicRadius * 1.15f,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
                // Orbital Energy Node
                val angleRad = Math.toRadians(45.0)
                val nodeX = center.x + (dynamicRadius * 1.15f * cos(angleRad)).toFloat()
                val nodeY = center.y + (dynamicRadius * 1.15f * sin(angleRad)).toFloat()
                drawCircle(color = primaryColor, radius = 4.dp.toPx(), center = Offset(nodeX, nodeY))
            }

            // Outer Orbit Ring 2 (counter-rotating)
            rotate(-rotation * 1.4f, pivot = center) {
                drawCircle(
                    color = secondaryColor.copy(alpha = 0.35f),
                    radius = dynamicRadius * 1.28f,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
                val angleRad2 = Math.toRadians(210.0)
                val nodeX2 = center.x + (dynamicRadius * 1.28f * cos(angleRad2)).toFloat()
                val nodeY2 = center.y + (dynamicRadius * 1.28f * sin(angleRad2)).toFloat()
                drawCircle(color = secondaryColor, radius = 3.5.dp.toPx(), center = Offset(nodeX2, nodeY2))
            }

            // Core Orb Body
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.95f),
                        secondaryColor.copy(alpha = 0.7f),
                        AurixVoid
                    ),
                    center = center,
                    radius = dynamicRadius
                ),
                radius = dynamicRadius,
                center = center
            )

            // Inner Iris Border
            drawCircle(
                color = primaryColor.copy(alpha = 0.85f),
                radius = dynamicRadius * 0.9f,
                center = center,
                style = Stroke(width = 2.5.dp.toPx())
            )
        }

        // Center Icon Overlay
        val icon = when (voiceState) {
            VoiceState.ERROR -> Icons.Default.Warning
            VoiceState.PERMISSION_REQUIRED -> Icons.Default.Warning
            VoiceState.LISTENING -> Icons.Default.Mic
            VoiceState.SPEAKING -> Icons.Default.Mic
            else -> Icons.Default.Mic
        }

        Icon(
            imageVector = icon,
            contentDescription = "AURIX Voice Orb",
            tint = if (voiceState == VoiceState.ERROR) AurixCrimson else Color.White,
            modifier = Modifier.size(size * 0.28f)
        )
    }
}
