package com.example.aurix.providers

import com.example.aurix.data.model.AIModelInfo
import com.example.aurix.data.model.AIRequest
import com.example.aurix.data.model.AIResponse
import com.example.aurix.data.model.ChatMessageItem
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

class GeminiProvider(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
) : AIProvider {

    override val providerType = ProviderType.GEMINI

    override val supportedModels = listOf(
        AIModelInfo("gemini-2.5-flash", "Gemini 2.5 Flash (Ultra Fast)", ProviderType.GEMINI, 1000000, true, true),
        AIModelInfo("gemini-1.5-flash", "Gemini 1.5 Flash", ProviderType.GEMINI, 1000000, true, true),
        AIModelInfo("gemini-1.5-pro", "Gemini 1.5 Pro (Deep Reasoning)", ProviderType.GEMINI, 2000000, true, true)
    )

    override suspend fun generateResponse(
        apiKey: String,
        modelId: String,
        request: AIRequest
    ): Result<AIResponse> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$modelId:generateContent?key=$apiKey"

            val rootJson = JSONObject()

            // System instructions
            if (!request.systemPrompt.isNullOrBlank()) {
                val sysInstruction = JSONObject()
                val parts = JSONArray()
                parts.put(JSONObject().put("text", request.systemPrompt))
                sysInstruction.put("parts", parts)
                rootJson.put("systemInstruction", sysInstruction)
            }

            // Contents array
            val contentsArray = JSONArray()

            // Add previous history if any
            for (msg in request.conversationHistory) {
                val contentObj = JSONObject()
                contentObj.put("role", if (msg.role == "assistant") "model" else "user")
                val parts = JSONArray()
                parts.put(JSONObject().put("text", msg.content))
                contentObj.put("parts", parts)
                contentsArray.put(contentObj)
            }

            // Current prompt
            val currentContent = JSONObject()
            currentContent.put("role", "user")
            val currentParts = JSONArray()

            // If image is present
            if (!request.imageBase64.isNullOrBlank()) {
                val inlineData = JSONObject()
                inlineData.put("mimeType", "image/jpeg")
                inlineData.put("data", request.imageBase64)
                currentParts.put(JSONObject().put("inlineData", inlineData))
            }
            currentParts.put(JSONObject().put("text", request.prompt))
            currentContent.put("parts", currentParts)
            contentsArray.put(currentContent)

            rootJson.put("contents", contentsArray)

            // Generation config
            val genConfig = JSONObject()
            genConfig.put("temperature", request.temperature)
            genConfig.put("maxOutputTokens", request.maxTokens)
            rootJson.put("generationConfig", genConfig)

            val body = rootJson.toString().toRequestBody("application/json".toMediaType())
            val httpRequest = Request.Builder()
                .url(endpoint)
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
            val candidates = respJson.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext Result.failure(Exception("No candidates returned from Gemini"))
            }

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            val fullText = StringBuilder()
            val toolCalls = mutableListOf<ToolCall>()

            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    if (part.has("text")) {
                        fullText.append(part.getString("text"))
                    }
                    if (part.has("functionCall")) {
                        val fn = part.getJSONObject("functionCall")
                        val fnName = fn.getString("name")
                        val fnArgsObj = fn.optJSONObject("args")
                        val argsMap = mutableMapOf<String, Any>()
                        if (fnArgsObj != null) {
                            val keys = fnArgsObj.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                argsMap[key] = fnArgsObj.get(key)
                            }
                        }
                        toolCalls.add(
                            ToolCall(
                                id = "call_${System.currentTimeMillis()}_$i",
                                name = fnName,
                                arguments = argsMap
                            )
                        )
                    }
                }
            }

            Result.success(
                AIResponse(
                    content = fullText.toString(),
                    toolCalls = toolCalls,
                    provider = ProviderType.GEMINI,
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
