package com.example.aurix.providers.autodetect

import com.example.aurix.data.model.AIModelInfo
import com.example.aurix.data.model.ProviderType
import org.json.JSONArray
import org.json.JSONObject

class ModelDiscoveryService {

    fun parseDiscoveredModels(provider: ProviderType, rawJson: String?): List<AIModelInfo> {
        val discovered = mutableListOf<String>()

        if (!rawJson.isNullOrBlank()) {
            try {
                val root = JSONObject(rawJson)

                // Gemini format: { "models": [ { "name": "models/gemini-2.5-flash", ... } ] }
                if (provider == ProviderType.GEMINI && root.has("models")) {
                    val arr = root.getJSONArray("models")
                    for (i in 0 until arr.length()) {
                        val m = arr.getJSONObject(i)
                        val rawName = m.optString("name", "")
                        // e.g. "models/gemini-2.5-flash" -> "gemini-2.5-flash"
                        val modelId = if (rawName.startsWith("models/")) rawName.substring(7) else rawName
                        if (modelId.isNotBlank()) discovered.add(modelId)
                    }
                }

                // OpenAI / Groq / OpenRouter format: { "data": [ { "id": "llama-3.3-70b-versatile", ... } ] }
                if (root.has("data")) {
                    val arr = root.getJSONArray("data")
                    for (i in 0 until arr.length()) {
                        val m = arr.getJSONObject(i)
                        val id = m.optString("id", "")
                        if (id.isNotBlank()) discovered.add(id)
                    }
                }
            } catch (_: Exception) {}
        }

        // If no models were successfully extracted, fall back to safe curated provider catalog
        if (discovered.isEmpty()) {
            discovered.addAll(getFallbackCatalog(provider))
        }

        // Filter through capability resolver
        return discovered
            .filter { ModelCapabilityResolver.isCompatibleForAgent(it, provider) }
            .distinct()
            .map { ModelCapabilityResolver.resolveCapabilities(it, provider) }
    }

    private fun getFallbackCatalog(provider: ProviderType): List<String> {
        return when (provider) {
            ProviderType.GEMINI -> listOf(
                "gemini-2.5-flash",
                "gemini-1.5-flash",
                "gemini-1.5-pro"
            )
            ProviderType.GROQ -> listOf(
                "llama-3.3-70b-versatile",
                "llama-3.1-8b-instant",
                "mixtral-8x7b-32768"
            )
            ProviderType.OPENROUTER -> listOf(
                "anthropic/claude-3.5-sonnet",
                "openai/gpt-4o",
                "deepseek/deepseek-chat",
                "meta-llama/llama-3.3-70b-instruct"
            )
            ProviderType.OPENAI_COMPATIBLE -> listOf(
                "gpt-4o",
                "gpt-4o-mini",
                "gpt-4-turbo"
            )
            ProviderType.ELEVENLABS -> listOf(
                "eleven_turbo_v2_5",
                "eleven_multilingual_v2"
            )
        }
    }
}
