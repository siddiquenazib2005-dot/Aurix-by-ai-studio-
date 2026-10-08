package com.example.aurix.data.model

enum class ProviderType {
    GEMINI,
    GROQ,
    OPENROUTER,
    OPENAI_COMPATIBLE,
    ELEVENLABS
}

enum class ProviderHealth {
    HEALTHY,
    DEGRADED,
    RATE_LIMITED,
    FAILED,
    UNCONFIGURED
}

data class AIModelInfo(
    val id: String,
    val name: String,
    val provider: ProviderType,
    val contextWindow: Int = 128000,
    val supportsVision: Boolean = false,
    val supportsFunctionCalling: Boolean = true
)

data class ToolCall(
    val id: String,
    val name: String,
    val arguments: Map<String, Any>
)

data class ToolResult(
    val toolCallId: String,
    val toolName: String,
    val isSuccess: Boolean,
    val verificationVerified: Boolean,
    val deviceRealityReport: String,
    val output: String,
    val data: Map<String, Any> = emptyMap()
)

data class AIRequest(
    val prompt: String,
    val systemPrompt: String? = null,
    val conversationHistory: List<ChatMessageItem> = emptyList(),
    val availableTools: List<String> = emptyList(),
    val temperature: Float = 0.4f,
    val maxTokens: Int = 2048,
    val imageBase64: String? = null
)

data class AIResponse(
    val content: String,
    val toolCalls: List<ToolCall> = emptyList(),
    val provider: ProviderType,
    val modelId: String,
    val latencyMs: Long,
    val rawTokensUsed: Int = 0
)

data class ChatMessageItem(
    val role: String, // "user", "assistant", "system", "tool"
    val content: String,
    val toolCallsJson: String? = null,
    val toolResultJson: String? = null
)
