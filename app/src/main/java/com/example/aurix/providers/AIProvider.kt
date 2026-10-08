package com.example.aurix.providers

import com.example.aurix.data.model.AIModelInfo
import com.example.aurix.data.model.AIRequest
import com.example.aurix.data.model.AIResponse
import com.example.aurix.data.model.ProviderHealth
import com.example.aurix.data.model.ProviderType

interface AIProvider {
    val providerType: ProviderType
    val supportedModels: List<AIModelInfo>

    suspend fun generateResponse(
        apiKey: String,
        modelId: String,
        request: AIRequest
    ): Result<AIResponse>

    suspend fun checkHealth(apiKey: String, modelId: String): ProviderHealth
}
