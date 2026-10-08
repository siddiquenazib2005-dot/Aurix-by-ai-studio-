package com.example.aurix.providers

import com.example.aurix.data.model.AIModelInfo
import com.example.aurix.data.model.ProviderType

class GroqProvider : OpenAICompatibleProvider(
    providerType = ProviderType.GROQ,
    baseUrl = "https://api.groq.com/openai/v1",
    supportedModels = listOf(
        AIModelInfo("llama-3.3-70b-versatile", "Groq LLaMA 3.3 70B (Ultra Fast)", ProviderType.GROQ, 128000, false, true),
        AIModelInfo("llama-3.1-8b-instant", "Groq LLaMA 3.1 8B Instant", ProviderType.GROQ, 128000, false, true),
        AIModelInfo("mixtral-8x7b-32768", "Groq Mixtral 8x7B", ProviderType.GROQ, 32768, false, true)
    )
)

class OpenRouterProvider : OpenAICompatibleProvider(
    providerType = ProviderType.OPENROUTER,
    baseUrl = "https://openrouter.ai/api/v1",
    supportedModels = listOf(
        AIModelInfo("anthropic/claude-3.5-sonnet", "Claude 3.5 Sonnet", ProviderType.OPENROUTER, 200000, true, true),
        AIModelInfo("openai/gpt-4o", "GPT-4o Omnimodal", ProviderType.OPENROUTER, 128000, true, true),
        AIModelInfo("deepseek/deepseek-chat", "DeepSeek V3", ProviderType.OPENROUTER, 64000, false, true),
        AIModelInfo("meta-llama/llama-3.3-70b-instruct", "LLaMA 3.3 70B", ProviderType.OPENROUTER, 128000, false, true)
    )
)
