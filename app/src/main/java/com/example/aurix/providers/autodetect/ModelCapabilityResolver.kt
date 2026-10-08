package com.example.aurix.providers.autodetect

import com.example.aurix.data.model.AIModelInfo
import com.example.aurix.data.model.ProviderType
import java.util.Locale

object ModelCapabilityResolver {

    fun isCompatibleForAgent(modelId: String, provider: ProviderType): Boolean {
        val lower = modelId.lowercase(Locale.ROOT)

        // Reject non-chat and non-agent models
        val blockedKeywords = listOf(
            "embed", "moderation", "whisper", "tts", "dall-e", "audio", "realtime",
            "search", "similarity", "babbage", "davinci", "curie", "ada",
            "reward", "guard", "rerank", "edit", "instruct-deprecated"
        )
        if (blockedKeywords.any { lower.contains(it) }) return false

        return when (provider) {
            ProviderType.GEMINI -> lower.contains("gemini")
            ProviderType.GROQ -> lower.contains("llama") || lower.contains("mixtral") || lower.contains("gemma") || lower.contains("deepseek") || lower.contains("qwen")
            ProviderType.OPENROUTER -> !lower.contains("free") || lower.contains("claude") || lower.contains("gpt") || lower.contains("llama") || lower.contains("deepseek")
            ProviderType.OPENAI_COMPATIBLE -> lower.contains("gpt") || lower.contains("o1") || lower.contains("o3") || lower.contains("claude") || lower.contains("llama")
            ProviderType.ELEVENLABS -> lower.contains("eleven") || lower.contains("turbo") || lower.contains("multilingual")
        }
    }

    fun resolveCapabilities(modelId: String, provider: ProviderType): AIModelInfo {
        val lower = modelId.lowercase(Locale.ROOT)

        val supportsVision = when {
            lower.contains("vision") || lower.contains("flash") || lower.contains("4o") || lower.contains("sonnet") || lower.contains("gemini") -> true
            else -> false
        }

        val supportsToolCalling = when {
            lower.contains("versatile") || lower.contains("instruct") || lower.contains("gpt-4") ||
                    lower.contains("gpt-3.5") || lower.contains("gemini") || lower.contains("sonnet") ||
                    lower.contains("claude") || lower.contains("deepseek-chat") -> true
            else -> true
        }

        val contextWindow = when {
            lower.contains("1.5-pro") -> 2000000
            lower.contains("gemini") -> 1000000
            lower.contains("sonnet") -> 200000
            lower.contains("128k") || lower.contains("gpt-4o") || lower.contains("llama-3.3") || lower.contains("llama-3.1") -> 128000
            lower.contains("32k") || lower.contains("mixtral") -> 32768
            else -> 64000
        }

        val friendlyName = formatFriendlyName(modelId)

        return AIModelInfo(
            id = modelId,
            name = friendlyName,
            provider = provider,
            contextWindow = contextWindow,
            supportsVision = supportsVision,
            supportsFunctionCalling = supportsToolCalling
        )
    }

    private fun formatFriendlyName(id: String): String {
        val clean = id.substringAfterLast("/")
        return clean.replace("-", " ")
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }
}
