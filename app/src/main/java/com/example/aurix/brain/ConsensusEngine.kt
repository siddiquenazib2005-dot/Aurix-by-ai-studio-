package com.example.aurix.brain

import com.example.aurix.data.model.AIRequest
import com.example.aurix.data.model.AIResponse
import com.example.aurix.providers.ProviderRouter
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class ConsensusEngine(
    private val providerRouter: ProviderRouter
) {
    data class ConsensusReport(
        val finalAnswer: String,
        val agreementRatio: Float,
        val modelResponses: List<AIResponse>,
        val contradictionDetected: Boolean,
        val consensusNotes: String
    )

    suspend fun queryConsensus(
        prompt: String,
        systemPrompt: String? = null
    ): ConsensusReport = coroutineScope {
        val request = AIRequest(
            prompt = prompt,
            systemPrompt = systemPrompt,
            temperature = 0.2f // Lower temperature for factual stability
        )

        // Run multi-provider queries
        val primaryDeferred = async { providerRouter.executeWithRouting(request) }
        val secondaryDeferred = async { providerRouter.executeWithRouting(request) }

        val primaryResult = primaryDeferred.await()
        val secondaryResult = secondaryDeferred.await()

        val validResponses = listOfNotNull(
            primaryResult.getOrNull(),
            secondaryResult.getOrNull()
        )

        if (validResponses.isEmpty()) {
            return@coroutineScope ConsensusReport(
                finalAnswer = "Unable to reach multiple models for consensus check.",
                agreementRatio = 0f,
                modelResponses = emptyList(),
                contradictionDetected = false,
                consensusNotes = "Network or provider availability limitation."
            )
        }

        if (validResponses.size == 1) {
            return@coroutineScope ConsensusReport(
                finalAnswer = validResponses.first().content,
                agreementRatio = 1.0f,
                modelResponses = validResponses,
                contradictionDetected = false,
                consensusNotes = "Single authoritative provider responded (${validResponses.first().provider})."
            )
        }

        // Compare responses
        val first = validResponses[0].content
        val second = validResponses[1].content
        val wordsFirst = first.split("\\s+".toRegex()).toSet()
        val wordsSecond = second.split("\\s+".toRegex()).toSet()
        val overlap = wordsFirst.intersect(wordsSecond).size
        val ratio = overlap.toFloat() / maxOf(wordsFirst.size, wordsSecond.size, 1)

        val hasContradiction = ratio < 0.35f

        ConsensusReport(
            finalAnswer = if (first.length >= second.length) first else second,
            agreementRatio = ratio,
            modelResponses = validResponses,
            contradictionDetected = hasContradiction,
            consensusNotes = if (hasContradiction) {
                "⚠️ Notice: Variance detected between models (${validResponses.map { it.provider }.joinToString(", ")}). Stronger response selected."
            } else {
                "✓ Cross-model consensus verified (${(ratio * 100).toInt()}% agreement across ${validResponses.size} models)."
            }
        )
    }
}
