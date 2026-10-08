package com.example.aurix.security

import android.util.Base64
import java.nio.charset.StandardCharsets

object SecureStorage {
    // Obfuscate sensitive credentials for safe local storage
    fun obfuscateKey(rawKey: String): String {
        if (rawKey.isBlank()) return ""
        val bytes = rawKey.toByteArray(StandardCharsets.UTF_8)
        val inverted = ByteArray(bytes.size) { i -> (bytes[i].toInt() xor 0x5A).toByte() }
        return Base64.encodeToString(inverted, Base64.NO_WRAP)
    }

    fun deobfuscateKey(obfuscated: String): String {
        if (obfuscated.isBlank()) return ""
        return try {
            val bytes = Base64.decode(obfuscated, Base64.NO_WRAP)
            val restored = ByteArray(bytes.size) { i -> (bytes[i].toInt() xor 0x5A).toByte() }
            String(restored, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }

    // Mask for UI display: "sk-proj-12345678" -> "sk-pr...5678"
    fun maskKey(key: String): String {
        if (key.length <= 8) return "••••••••"
        val prefix = key.take(4)
        val suffix = key.takeLast(4)
        return "$prefix••••$suffix"
    }

    // Redact keys from logs/telemetry
    fun redactSecrets(text: String): String {
        val patterns = listOf(
            Regex("(?i)(key|secret|token|bearer|api_key)[\"':\\s=]+([a-zA-Z0-9_\\-]{16,})"),
            Regex("AIza[0-9A-Za-z-_]{35}"),
            Regex("sk-[a-zA-Z0-9]{20,}")
        )
        var sanitized = text
        for (pattern in patterns) {
            sanitized = pattern.replace(sanitized) { match ->
                val str = match.value
                val split = str.split("=")
                if (split.size > 1) "${split[0]}=[REDACTED]" else "[REDACTED]"
            }
        }
        return sanitized
    }
}
