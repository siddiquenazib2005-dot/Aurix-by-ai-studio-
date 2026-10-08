package com.example.aurix.providers.autodetect

import com.example.aurix.data.model.AIModelInfo
import com.example.aurix.data.model.ProviderType
import java.util.Locale

class ModelSelectionEngine {

    data class SelectionResult(
        val bestModel: AIModelInfo,
        val fallbackModels: List<AIModelInfo>,
        val selectionRationale: String
    )

    fun selectBestModel(
        models: List<AIModelInfo>,
        provider: ProviderType,
        requiresVision: Boolean = false
    ): SelectionResult {
        if (models.isEmpty()) {
            val defaultFallback = ModelCapabilityResolver.resolveCapabilities(
                when (provider) {
                    ProviderType.GEMINI -> "gemini-2.5-flash"
                    ProviderType.GROQ -> "llama-3.3-70b-versatile"
                    ProviderType.OPENROUTER -> "anthropic/claude-3.5-sonnet"
                    ProviderType.OPENAI_COMPATIBLE -> "gpt-4o"
                    ProviderType.ELEVENLABS -> "eleven_turbo_v2_5"
                },
                provider
            )
            return SelectionResult(
                bestModel = defaultFallback,
                fallbackModels = emptyList(),
                selectionRationale = "Default flagship model selected (empty catalog fallback)."
            )
        }

        // Score each model
        val scored = models.map { model ->
            var score = 0
            val lower = model.id.lowercase(Locale.ROOT)

            // 1. Tool calling is critical for AURIX agent actions
            if (model.supportsFunctionCalling) score += 500

            // 2. Vision support if requested
            if (requiresVision && model.supportsVision) score += 400
            else if (model.supportsVision) score += 100

            // 3. Known flagship high-intelligence models
            when {
                lower.contains("llama-3.3-70b") -> score += 350
                lower.contains("gemini-2.5-flash") -> score += 380
                lower.contains("gemini-1.5-pro") -> score += 320
                lower.contains("gemini-1.5-flash") -> score += 340
                lower.contains("claude-3.5-sonnet") -> score += 390
                lower.contains("gpt-4o") && !lower.contains("mini") -> score += 370
                lower.contains("gpt-4o-mini") -> score += 280
                lower.contains("deepseek-chat") -> score += 310
                lower.contains("llama-3.1-8b") -> score += 250
                lower.contains("mixtral") -> score += 240
            }

            // 4. Large context window
            if (model.contextWindow >= 128000) score += 150
            else if (model.contextWindow >= 32000) score += 80

            // 5. Fast flash/turbo tier bonus for voice latency
            if (lower.contains("flash") || lower.contains("turbo") || lower.contains("versatile")) {
                score += 120
            }

            // Penalties for legacy / small test models
            if (lower.contains("preview") || lower.contains("experimental")) score -= 50
            if (lower.contains("small") || lower.contains("nano")) score -= 100

            Pair(model, score)
        }.sortedByDescending { it.second }

        val best = scored.first().first
        val fallbacks = scored.drop(1).take(3).map { it.first }

        val rationale = "Selected '${best.id}' based on highest agent capability score (Tool Calling: ${best.supportsFunctionCalling}, Context: ${best.contextWindow / 1000}k)."

        return SelectionResult(
            bestModel = best,
            fallbackModels = fallbacks,
            selectionRationale = rationale
        )
    }
}
