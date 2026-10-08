package com.example.aurix.providers

import com.example.BuildConfig
import com.example.aurix.data.local.dao.ProviderKeyDao
import com.example.aurix.data.local.entity.ProviderKeyEntity
import com.example.aurix.data.model.AIRequest
import com.example.aurix.data.model.AIResponse
import com.example.aurix.data.model.ProviderHealth
import com.example.aurix.data.model.ProviderType
import com.example.aurix.providers.autodetect.ModelHealthManager
import com.example.aurix.security.SecureStorage
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.atomic.AtomicInteger

class ProviderRouter(
    private val keyDao: ProviderKeyDao
) {
    private val geminiProvider = GeminiProvider()
    private val groqProvider = GroqProvider()
    private val openRouterProvider = OpenRouterProvider()

    private val roundRobinIndex = AtomicInteger(0)

    val allKeysFlow: Flow<List<ProviderKeyEntity>> = keyDao.getAllProviderKeysFlow()

    fun getProvider(type: ProviderType): AIProvider {
        return when (type) {
            ProviderType.GEMINI -> geminiProvider
            ProviderType.GROQ -> groqProvider
            ProviderType.OPENROUTER -> openRouterProvider
            ProviderType.OPENAI_COMPATIBLE -> openRouterProvider
            ProviderType.ELEVENLABS -> geminiProvider // handled in Voice module
        }
    }

    suspend fun executeWithRouting(request: AIRequest): Result<AIResponse> {
        val configuredKeys = keyDao.getAllActiveKeys()
        val now = System.currentTimeMillis()

        // Filter out keys in provider-level cooldown
        val availableKeys = configuredKeys.filter { it.cooldownUntilMs <= now }
            .sortedWith(
                compareBy<ProviderKeyEntity> { it.failureCount }
                    .thenBy { if (it.healthStatus == ProviderHealth.HEALTHY) 0 else 1 }
                    .thenBy { it.lastUsedAt }
            )

        // Try available user-configured keys in prioritized order
        for (keyEntity in availableKeys) {
            val rawKey = SecureStorage.deobfuscateKey(keyEntity.apiKeyEncrypted)
            if (rawKey.isBlank()) continue

            val provider = getProvider(keyEntity.providerType)
            val providerName = keyEntity.providerType.name

            // Build candidate model list starting with the key's primary model, then fallbacks
            val candidateModels = mutableListOf<String>()
            candidateModels.add(keyEntity.modelId)
            when (keyEntity.providerType) {
                ProviderType.GROQ -> {
                    listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant", "mixtral-8x7b-32768").forEach {
                        if (!candidateModels.contains(it)) candidateModels.add(it)
                    }
                }
                ProviderType.GEMINI -> {
                    listOf("gemini-2.5-flash", "gemini-1.5-flash", "gemini-1.5-pro").forEach {
                        if (!candidateModels.contains(it)) candidateModels.add(it)
                    }
                }
                ProviderType.OPENROUTER -> {
                    listOf("anthropic/claude-3.5-sonnet", "openai/gpt-4o", "deepseek/deepseek-chat").forEach {
                        if (!candidateModels.contains(it)) candidateModels.add(it)
                    }
                }
                ProviderType.OPENAI_COMPATIBLE -> {
                    listOf("gpt-4o", "gpt-4o-mini").forEach {
                        if (!candidateModels.contains(it)) candidateModels.add(it)
                    }
                }
                else -> {}
            }

            // Model-Level Fallback (Phase 6 & 7): Model A -> Model B -> Model C
            var keyHasSucceeded = false
            for (modelId in candidateModels) {
                // Check if specific model is currently in cooldown
                if (!ModelHealthManager.isModelAvailable(providerName, modelId)) {
                    continue
                }

                val startTime = System.currentTimeMillis()
                val result = provider.generateResponse(rawKey, modelId, request)

                if (result.isSuccess) {
                    val latency = System.currentTimeMillis() - startTime
                    ModelHealthManager.recordModelSuccess(providerName, modelId, latency)
                    keyDao.recordSuccess(keyEntity.id, latency, System.currentTimeMillis())
                    keyHasSucceeded = true
                    return result
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                    ModelHealthManager.recordModelFailure(providerName, modelId, errorMsg)
                }
            }

            // If all models for this key failed
            if (!keyHasSucceeded) {
                val cooldown = now + 30_000L
                keyDao.recordFailure(keyEntity.id, cooldown, "All candidate models failed for key")
            }
        }

        // Fallback: Check built-in Gemini API key from BuildConfig if available
        val defaultGeminiKey = try {
            BuildConfig::class.java.getField("GEMINI_API_KEY").get(null) as? String ?: ""
        } catch (_: Exception) {
            ""
        }

        if (defaultGeminiKey.isNotBlank() && defaultGeminiKey != "MY_GEMINI_API_KEY") {
            val result = geminiProvider.generateResponse(defaultGeminiKey, "gemini-2.5-flash", request)
            if (result.isSuccess) {
                return result
            }
        }

        // If all providers fail or are unconfigured, return a structured, graceful local fallback
        return Result.failure(
            Exception("All configured AI providers and models are currently unavailable. Please verify your API key in Settings.")
        )
    }

    suspend fun testKey(keyEntity: ProviderKeyEntity): ProviderHealth {
        val rawKey = SecureStorage.deobfuscateKey(keyEntity.apiKeyEncrypted)
        if (rawKey.isBlank()) return ProviderHealth.UNCONFIGURED
        val provider = getProvider(keyEntity.providerType)
        return provider.checkHealth(rawKey, keyEntity.modelId)
    }
}
