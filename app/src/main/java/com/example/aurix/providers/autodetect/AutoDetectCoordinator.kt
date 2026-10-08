package com.example.aurix.providers.autodetect

import com.example.aurix.data.model.AIModelInfo
import com.example.aurix.data.model.ProviderType

class AutoDetectCoordinator(
    private val detector: ProviderDetector = ProviderDetector(),
    private val discoveryService: ModelDiscoveryService = ModelDiscoveryService(),
    private val selectionEngine: ModelSelectionEngine = ModelSelectionEngine()
) {
    sealed class AutoDetectState {
        object Idle : AutoDetectState()
        data class Detecting(val stepMessage: String) : AutoDetectState()
        data class Success(
            val provider: ProviderType,
            val rawKey: String,
            val bestModel: AIModelInfo,
            val discoveredModels: List<AIModelInfo>,
            val fallbackModels: List<AIModelInfo>,
            val latencyMs: Long,
            val statusSummary: String
        ) : AutoDetectState()
        data class Error(val message: String, val canFallbackToManual: Boolean = true) : AutoDetectState()
    }

    suspend fun runDetectionFlow(
        apiKey: String,
        onProgress: (String) -> Unit = {}
    ): AutoDetectState {
        val trimmed = apiKey.trim()
        if (trimmed.isBlank()) {
            return AutoDetectState.Error("Please enter an API key.")
        }

        // Step 1: Detect provider & validate key directly on official endpoint
        onProgress("Detecting provider & verifying API key...")
        val hint = ApiKeyDetector.detectProviderHint(trimmed)
        val detection = detector.detectAndValidate(trimmed)
        if (!detection.isSuccess || detection.provider == null || detection.validationResult == null) {
            // If network verification failed (e.g. offline sandbox or DNS failure), but format hint is unambiguous
            if (hint != null) {
                onProgress("Format matches ${hint.name}. Preparing model catalog...")
                val models = discoveryService.parseDiscoveredModels(hint, null)
                val selection = selectionEngine.selectBestModel(models, hint)
                val summary = "✓ Detected: ${hint.name} (Format Match)\n✓ ${models.size} Recommended Models\n✓ Best Agent Model: ${selection.bestModel.id}\n(Network probe unverified: ${detection.failureReason?.take(50)})"
                return AutoDetectState.Success(
                    provider = hint,
                    rawKey = trimmed,
                    bestModel = selection.bestModel,
                    discoveredModels = models,
                    fallbackModels = selection.fallbackModels,
                    latencyMs = 0L,
                    statusSummary = summary
                )
            }
            return AutoDetectState.Error(
                detection.failureReason ?: "Provider could not be detected automatically.",
                canFallbackToManual = true
            )
        }

        val provider = detection.provider
        val latency = detection.validationResult.latencyMs

        // Step 2: Discover models
        onProgress("Discovering available models for ${provider.name}...")
        val models = discoveryService.parseDiscoveredModels(
            provider,
            detection.validationResult.rawModelsResponse
        )

        // Step 3: Select best model
        onProgress("Evaluating capabilities & selecting best model...")
        val selection = selectionEngine.selectBestModel(models, provider)

        val summary = "✓ Detected: ${provider.name}\n✓ Key Verified (${latency}ms)\n✓ ${models.size} Models Discovered\n✓ Best Model: ${selection.bestModel.id}"

        return AutoDetectState.Success(
            provider = provider,
            rawKey = trimmed,
            bestModel = selection.bestModel,
            discoveredModels = models,
            fallbackModels = selection.fallbackModels,
            latencyMs = latency,
            statusSummary = summary
        )
    }
}
