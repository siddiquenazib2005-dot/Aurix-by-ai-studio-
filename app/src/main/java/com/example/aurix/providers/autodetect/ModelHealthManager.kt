package com.example.aurix.providers.autodetect

import com.example.aurix.data.model.ProviderHealth
import java.util.concurrent.ConcurrentHashMap

object ModelHealthManager {

    data class ModelHealthState(
        val health: ProviderHealth = ProviderHealth.HEALTHY,
        val failureCount: Int = 0,
        val cooldownUntilMs: Long = 0L,
        val lastLatencyMs: Long = 0L,
        val lastErrorMessage: String? = null
    )

    // Key: "$provider:$modelId"
    private val modelHealthMap = ConcurrentHashMap<String, ModelHealthState>()

    fun getModelHealth(providerName: String, modelId: String): ModelHealthState {
        val key = "$providerName:$modelId"
        val state = modelHealthMap[key] ?: return ModelHealthState()
        val now = System.currentTimeMillis()
        if (state.cooldownUntilMs > 0 && state.cooldownUntilMs <= now) {
            // Cooldown expired; restore to healthy
            val restored = state.copy(health = ProviderHealth.HEALTHY, cooldownUntilMs = 0L)
            modelHealthMap[key] = restored
            return restored
        }
        return state
    }

    fun recordModelSuccess(providerName: String, modelId: String, latencyMs: Long) {
        val key = "$providerName:$modelId"
        modelHealthMap[key] = ModelHealthState(
            health = ProviderHealth.HEALTHY,
            failureCount = 0,
            cooldownUntilMs = 0L,
            lastLatencyMs = latencyMs
        )
    }

    fun recordModelFailure(providerName: String, modelId: String, error: String) {
        val key = "$providerName:$modelId"
        val existing = modelHealthMap[key] ?: ModelHealthState()
        val isRateLimit = error.contains("429") || error.contains("rate limit", ignoreCase = true)
        val now = System.currentTimeMillis()
        val cooldown = if (isRateLimit) now + 60_000L else now + 20_000L

        modelHealthMap[key] = existing.copy(
            health = if (isRateLimit) ProviderHealth.RATE_LIMITED else ProviderHealth.FAILED,
            failureCount = existing.failureCount + 1,
            cooldownUntilMs = cooldown,
            lastErrorMessage = error
        )
    }

    fun isModelAvailable(providerName: String, modelId: String): Boolean {
        val health = getModelHealth(providerName, modelId)
        return health.health == ProviderHealth.HEALTHY || health.cooldownUntilMs <= System.currentTimeMillis()
    }
}
