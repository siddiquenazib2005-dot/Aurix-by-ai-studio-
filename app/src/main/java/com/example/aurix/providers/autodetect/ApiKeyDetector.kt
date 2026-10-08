package com.example.aurix.providers.autodetect

import com.example.aurix.data.model.ProviderType

object ApiKeyDetector {

    /**
     * Inspects visual format hints to decide which provider to probe first.
     * Note: Does NOT definitively decide; only provides an optimization hint for probe ordering.
     */
    fun detectProviderHint(rawKey: String): ProviderType? {
        val trimmed = rawKey.trim()
        return when {
            trimmed.startsWith("AIzaSy") || trimmed.startsWith("AIza") -> ProviderType.GEMINI
            trimmed.startsWith("gsk_") -> ProviderType.GROQ
            trimmed.startsWith("sk-or-") -> ProviderType.OPENROUTER
            trimmed.startsWith("sk-") && !trimmed.startsWith("sk-or-") -> ProviderType.OPENAI_COMPATIBLE
            trimmed.length == 32 && trimmed.all { it.isLetterOrDigit() } -> ProviderType.ELEVENLABS
            trimmed.startsWith("xi-") -> ProviderType.ELEVENLABS
            else -> null
        }
    }
}
