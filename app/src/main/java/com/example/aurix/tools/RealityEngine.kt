package com.example.aurix.tools

import com.example.aurix.data.local.dao.TelemetryDao
import com.example.aurix.data.local.entity.DeviceTelemetryEntity
import com.example.aurix.data.model.ToolResult
import org.json.JSONObject

class RealityEngine(
    private val telemetryDao: TelemetryDao
) {
    suspend fun auditAndVerify(
        tool: AurixTool,
        toolCallId: String,
        arguments: Map<String, Any>
    ): ToolResult {
        // Step 1: Execute
        val initialResult = try {
            tool.execute(toolCallId, arguments)
        } catch (e: Exception) {
            ToolResult(
                toolCallId = toolCallId,
                toolName = tool.name,
                isSuccess = false,
                verificationVerified = false,
                deviceRealityReport = "Execution failed: ${e.message}",
                output = "Error executing ${tool.name}: ${e.message}"
            )
        }

        // Step 2: Reality Verification Check
        val verifiedResult = try {
            tool.verify(initialResult)
        } catch (e: Exception) {
            initialResult.copy(
                verificationVerified = false,
                deviceRealityReport = "Verification error: ${e.message}"
            )
        }

        // Step 3: Telemetry audit
        try {
            telemetryDao.insertTelemetry(
                DeviceTelemetryEntity(
                    id = "telem_${System.currentTimeMillis()}",
                    toolName = tool.name,
                    parametersJson = JSONObject(arguments).toString(),
                    isSuccess = verifiedResult.isSuccess,
                    verificationReport = verifiedResult.deviceRealityReport,
                    message = verifiedResult.output
                )
            )
        } catch (_: Exception) {}

        return verifiedResult
    }
}
