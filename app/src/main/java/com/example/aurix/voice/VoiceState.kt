package com.example.aurix.voice

enum class VoiceState {
    IDLE,
    LISTENING,
    THINKING,
    EXECUTING,
    SPEAKING,
    INTERRUPTED,
    ERROR,
    PERMISSION_REQUIRED
}

data class ElevenLabsVoice(
    val voiceId: String,
    val name: String,
    val category: String = "premade",
    val previewUrl: String? = null
)
