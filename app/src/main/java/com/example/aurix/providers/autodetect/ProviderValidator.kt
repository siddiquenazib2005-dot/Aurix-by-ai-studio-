package com.example.aurix.providers.autodetect

import com.example.aurix.data.model.ProviderType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ProviderValidator(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()
) {
    data class ValidationResult(
        val isConfirmed: Boolean,
        val provider: ProviderType,
        val rawModelsResponse: String? = null,
        val latencyMs: Long = 0L,
        val errorMessage: String? = null
    )

    suspend fun validate(provider: ProviderType, apiKey: String): ValidationResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val key = apiKey.trim()
        if (key.isBlank()) {
            return@withContext ValidationResult(false, provider, errorMessage = "Empty API key")
        }

        try {
            val request = when (provider) {
                ProviderType.GEMINI -> {
                    Request.Builder()
                        .url("https://generativelanguage.googleapis.com/v1beta/models?key=$key")
                        .get()
                        .build()
                }
                ProviderType.GROQ -> {
                    Request.Builder()
                        .url("https://api.groq.com/openai/v1/models")
                        .addHeader("Authorization", "Bearer $key")
                        .get()
                        .build()
                }
                ProviderType.OPENROUTER -> {
                    Request.Builder()
                        .url("https://openrouter.ai/api/v1/models")
                        .addHeader("Authorization", "Bearer $key")
                        .get()
                        .build()
                }
                ProviderType.OPENAI_COMPATIBLE -> {
                    Request.Builder()
                        .url("https://api.openai.com/v1/models")
                        .addHeader("Authorization", "Bearer $key")
                        .get()
                        .build()
                }
                ProviderType.ELEVENLABS -> {
                    Request.Builder()
                        .url("https://api.elevenlabs.io/v1/user")
                        .addHeader("xi-api-key", key)
                        .get()
                        .build()
                }
            }

            val response = client.newCall(request).execute()
            val latency = System.currentTimeMillis() - start
            val body = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                ValidationResult(
                    isConfirmed = true,
                    provider = provider,
                    rawModelsResponse = body,
                    latencyMs = latency
                )
            } else {
                ValidationResult(
                    isConfirmed = false,
                    provider = provider,
                    latencyMs = latency,
                    errorMessage = "HTTP ${response.code}: ${response.message}"
                )
            }
        } catch (e: Exception) {
            ValidationResult(
                isConfirmed = false,
                provider = provider,
                latencyMs = System.currentTimeMillis() - start,
                errorMessage = e.message ?: "Connection error"
            )
        }
    }
}
