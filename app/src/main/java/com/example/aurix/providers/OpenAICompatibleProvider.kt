package com.example.aurix.providers

import com.example.aurix.data.model.AIModelInfo
import com.example.aurix.data.model.AIRequest
import com.example.aurix.data.model.AIResponse
import com.example.aurix.data.model.ProviderHealth
import com.example.aurix.data.model.ProviderType
import com.example.aurix.data.model.ToolCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

open class OpenAICompatibleProvider(
    override val providerType: ProviderType,
    protected val baseUrl: String,
    override val supportedModels: List<AIModelInfo>,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
) : AIProvider {

    override suspend fun generateResponse(
        apiKey: String,
        modelId: String,
        request: AIRequest
    ): Result<AIResponse> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val endpoint = if (baseUrl.endsWith("/chat/completions")) baseUrl else "$baseUrl/chat/completions"

            val rootJson = JSONObject()
            rootJson.put("model", modelId)
            rootJson.put("temperature", request.temperature)
            rootJson.put("max_tokens", request.maxTokens)

            val messagesArray = JSONArray()

            // System prompt
            if (!request.systemPrompt.isNullOrBlank()) {
                messagesArray.put(
                    JSONObject().put("role", "system").put("content", request.systemPrompt)
                )
            }

            // History
            for (msg in request.conversationHistory) {
                messagesArray.put(
                    JSONObject().put("role", msg.role).put("content", msg.content)
                )
            }

            // Current prompt
            messagesArray.put(
                JSONObject().put("role", "user").put("content", request.prompt)
            )

            rootJson.put("messages", messagesArray)

            val body = rootJson.toString().toRequestBody("application/json".toMediaType())
            val httpRequest = Request.Builder()
                .url(endpoint)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            val httpResponse = client.newCall(httpRequest).execute()
            val latency = System.currentTimeMillis() - startTime
            val responseString = httpResponse.body?.string().orEmpty()

            if (!httpResponse.isSuccessful) {
                val errorMsg = if (httpResponse.code == 429) {
                    "Rate limit reached (429)"
                } else {
                    "HTTP ${httpResponse.code}: $responseString"
                }
                return@withContext Result.failure(Exception(errorMsg))
            }

            val respJson = JSONObject(responseString)
            val choices = respJson.optJSONArray("choices")
            if (choices == null || choices.length() == 0) {
                return@withContext Result.failure(Exception("No choices returned from $providerType"))
            }

            val firstChoice = choices.getJSONObject(0)
            val message = firstChoice.optJSONObject("message")
            val content = message?.optString("content", "").orEmpty()

            val toolCalls = mutableListOf<ToolCall>()
            val toolCallsArr = message?.optJSONArray("tool_calls")
            if (toolCallsArr != null) {
                for (i in 0 until toolCallsArr.length()) {
                    val tc = toolCallsArr.getJSONObject(i)
                    val id = tc.optString("id", "call_$i")
                    val fn = tc.optJSONObject("function")
                    val name = fn?.optString("name", "").orEmpty()
                    val argsStr = fn?.optString("arguments", "{}").orEmpty()
                    val argsMap = mutableMapOf<String, Any>()
                    try {
                        val argsObj = JSONObject(argsStr)
                        val keys = argsObj.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            argsMap[k] = argsObj.get(k)
                        }
                    } catch (_: Exception) {}
                    toolCalls.add(ToolCall(id, name, argsMap))
                }
            }

            Result.success(
                AIResponse(
                    content = content,
                    toolCalls = toolCalls,
                    provider = providerType,
                    modelId = modelId,
                    latencyMs = latency
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun checkHealth(apiKey: String, modelId: String): ProviderHealth = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext ProviderHealth.UNCONFIGURED
        try {
            val testReq = AIRequest(prompt = "ping")
            val result = generateResponse(apiKey, modelId, testReq)
            if (result.isSuccess) ProviderHealth.HEALTHY else ProviderHealth.FAILED
        } catch (e: Exception) {
            ProviderHealth.FAILED
        }
    }
}
