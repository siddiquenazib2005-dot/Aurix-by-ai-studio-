package com.example.aurix.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aurix.ui.components.FuturisticOrb
import com.example.ui.theme.AurixAmber
import com.example.ui.theme.AurixBorder
import com.example.ui.theme.AurixCardSurface
import com.example.ui.theme.AurixCrimson
import com.example.ui.theme.AurixCyan
import com.example.ui.theme.AurixDarkSurface
import com.example.ui.theme.AurixElectricBlue
import com.example.ui.theme.AurixEmerald
import com.example.ui.theme.AurixTextMuted
import com.example.ui.theme.AurixTextPrimary
import com.example.ui.theme.AurixTextSecondary
import com.example.ui.theme.AurixViolet
import com.example.ui.theme.AurixVoid
import com.example.aurix.voice.VoiceState

@Composable
fun VoiceScreen(
    voiceState: VoiceState,
    rmsLevel: Float,
    liveTranscription: String,
    lastAgentResponse: String,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onStopSpeaking: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AurixVoid)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Status Header
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "AURIX LIVE VOICE CORE",
                color = AurixCyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            val stateLabel = when (voiceState) {
                VoiceState.IDLE -> "READY • TAP TO SPEAK"
                VoiceState.LISTENING -> "LISTENING TO VOICE..."
                VoiceState.THINKING -> "NEURAL PLANNING..."
                VoiceState.EXECUTING -> "EXECUTING HARDWARE MISSION..."
                VoiceState.SPEAKING -> "SPEAKING • BARGE-IN ACTIVE"
                VoiceState.INTERRUPTED -> "INTERRUPTED • SWITCHING TO MIC"
                VoiceState.ERROR -> "SYSTEM ERROR"
                VoiceState.PERMISSION_REQUIRED -> "MICROPHONE PERMISSION NEEDED"
            }
            val stateColor = when (voiceState) {
                VoiceState.IDLE -> AurixTextMuted
                VoiceState.LISTENING -> AurixEmerald
                VoiceState.THINKING -> AurixViolet
                VoiceState.EXECUTING -> AurixAmber
                VoiceState.SPEAKING -> AurixCyan
                VoiceState.INTERRUPTED -> AurixEmerald
                VoiceState.ERROR -> AurixCrimson
                VoiceState.PERMISSION_REQUIRED -> AurixAmber
            }
            Text(
                text = stateLabel,
                color = stateColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
        }

        // Central Futuristic Orb (The Primary Interaction Point)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            FuturisticOrb(
                voiceState = voiceState,
                rmsLevel = rmsLevel,
                size = 220.dp,
                onClick = {
                    when (voiceState) {
                        VoiceState.LISTENING -> onStopListening()
                        VoiceState.SPEAKING -> {
                            onStopSpeaking()
                            onStartListening()
                        }
                        else -> onStartListening()
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Live Transcription & Barge-in Indicator
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AurixCardSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AurixBorder))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = when {
                            liveTranscription.isNotBlank() -> "\"$liveTranscription\""
                            lastAgentResponse.isNotBlank() -> lastAgentResponse.take(120)
                            voiceState == VoiceState.LISTENING -> "Listening... say 'Flashlight on', 'Mom ko call karo', or any request."
                            else -> "Tap the Orb or press the Mic below to start voice mode."
                        },
                        color = if (liveTranscription.isNotBlank()) AurixCyan else AurixTextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = AurixTextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Full Barge-In support: speak anytime to interrupt",
                            color = AurixTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Bottom Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (voiceState == VoiceState.SPEAKING) {
                Button(
                    onClick = onStopSpeaking,
                    colors = ButtonDefaults.buttonColors(containerColor = AurixCrimson, contentColor = Color.White),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Stop Voice")
                }
            } else {
                Button(
                    onClick = {
                        if (voiceState == VoiceState.LISTENING) onStopListening() else onStartListening()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (voiceState == VoiceState.LISTENING) AurixCrimson else AurixCyan,
                        contentColor = AurixVoid
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (voiceState == VoiceState.LISTENING) "Stop Listening" else "Activate Voice",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
