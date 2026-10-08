package com.example.aurix.tools

import com.example.aurix.data.model.ToolResult

data class ToolParameter(
    val name: String,
    val type: String, // "string", "int", "boolean", "number"
    val description: String,
    val isRequired: Boolean = true
)

interface AurixTool {
    val name: String
    val description: String
    val parameters: List<ToolParameter>
    val requiredPermissions: List<String>
    val isSensitive: Boolean // True for calls, sms, file delete

    suspend fun execute(
        toolCallId: String,
        arguments: Map<String, Any>
    ): ToolResult

    suspend fun verify(
        executionResult: ToolResult
    ): ToolResult
}
