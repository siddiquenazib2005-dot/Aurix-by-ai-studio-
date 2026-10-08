package com.example.aurix.brain

import com.example.aurix.data.model.AIRequest
import com.example.aurix.data.model.AutonomyMode
import com.example.aurix.data.model.ChatMessageItem
import com.example.aurix.data.model.MemoryCategory
import com.example.aurix.data.model.MissionStatus
import com.example.aurix.memory.SemanticMemoryEngine
import com.example.aurix.mission.MissionEngine
import com.example.aurix.providers.ProviderRouter
import com.example.aurix.tools.RealityEngine
import com.example.aurix.tools.ToolRegistry

class AgentBrain(
    private val providerRouter: ProviderRouter,
    private val toolRegistry: ToolRegistry,
    private val realityEngine: RealityEngine,
    private val missionEngine: MissionEngine,
    private val memoryEngine: SemanticMemoryEngine
) {
    data class BrainResult(
        val responseText: String,
        val missionId: String? = null,
        val telemetryReport: String? = null,
        val requiresApproval: Boolean = false,
        val pendingActionSummary: String? = null
    )

    suspend fun processUserRequest(
        conversationId: String,
        userInput: String,
        autonomyMode: AutonomyMode,
        history: List<ChatMessageItem> = emptyList()
    ): BrainResult {
        // 1. Context Timeline query check ("5 minute pehle kya baat hui thi?", "Kal wala message dikhao")
        val timelineContext = memoryEngine.resolveTimelineQuery(conversationId, userInput)
        if (timelineContext != null) {
            return BrainResult(responseText = timelineContext)
        }

        // 2. Intent Fusion & Action Compilation check
        val plan = ConversationActionCompiler.compile(userInput)

        if (plan.steps.isNotEmpty()) {
            if (plan.isMultiStepMission) {
                // Multi-step mission
                val mission = missionEngine.createMission(
                    conversationId = conversationId,
                    title = plan.missionTitle,
                    userGoal = plan.userGoal,
                    steps = plan.steps
                )

                // Execute mission
                val executed = missionEngine.executeOrResumeMission(mission.id, autonomyMode)

                val summary = buildString {
                    append("🚀 Multi-Step Mission Executed:\n")
                    val steps = missionEngine.deserializeSteps(executed.stepsJson)
                    for (s in steps) {
                        append("• ${s.title}: ${s.status} (${s.executionResult ?: s.errorReason ?: ""})\n")
                    }
                    if (executed.status == MissionStatus.COMPLETED) {
                        append("All actions verified on device ✓")
                    } else if (executed.status == MissionStatus.AWAITING_APPROVAL) {
                        append("Awaiting your confirmation for sensitive action.")
                    }
                }

                // Update memory
                memoryEngine.storeMemory(
                    content = "User goal '${plan.userGoal}' executed with ${stepsSummary(plan.steps)}",
                    category = MemoryCategory.TASK_HISTORY,
                    importance = 4
                )

                return BrainResult(
                    responseText = summary,
                    missionId = mission.id,
                    telemetryReport = "Mission ${mission.id} status: ${executed.status}"
                )
            } else {
                // Single step tool
                val singleStep = plan.steps.first()
                val tool = toolRegistry.getTool(singleStep.toolName)
                if (tool != null) {
                    if (tool.isSensitive && autonomyMode == AutonomyMode.FULL_MANUAL) {
                        return BrainResult(
                            responseText = "I am ready to ${singleStep.title}. Please confirm to proceed.",
                            requiresApproval = true,
                            pendingActionSummary = singleStep.title
                        )
                    }

                    val args = parseArgs(singleStep.argumentsJson)
                    val result = realityEngine.auditAndVerify(tool, "call_single", args)

                    val realityVerifiedText = if (result.isSuccess) {
                        "${result.output}\n(Device Reality: ${result.deviceRealityReport} ✓)"
                    } else {
                        "Action could not be completed: ${result.deviceRealityReport}"
                    }

                    // Learn preference or fact if relevant
                    if (singleStep.toolName == "VOLUME" || singleStep.toolName == "FLASHLIGHT") {
                        memoryEngine.storeMemory(
                            content = "Preference: ${singleStep.title}",
                            category = MemoryCategory.USER_PREFERENCE,
                            importance = 2
                        )
                    }

                    return BrainResult(
                        responseText = realityVerifiedText,
                        telemetryReport = result.deviceRealityReport
                    )
                }
            }
        }

        // 3. Conversational / Generative Path with Semantic Memory context
        val memories = memoryEngine.retrieveRelevantMemories(userInput, limit = 4)
        val memoryContext = if (memories.isNotEmpty()) {
            "RELEVANT USER MEMORIES:\n" + memories.joinToString("\n") { "- [${it.category}] ${it.content}" }
        } else ""

        val systemPrompt = buildString {
            append("You are AURIX: a futuristic, multimodal, voice-first Android AI agent.\n")
            append("Be authoritative, precise, and proactive. Never invent or hallucinate device outcomes.\n")
            if (memoryContext.isNotBlank()) {
                append("\n").append(memoryContext).append("\n")
            }
            append("\nAVAILABLE HARDWARE & SYSTEM TOOLS:\n")
            append(toolRegistry.getToolDefinitionsPrompt())
        }

        val aiRequest = AIRequest(
            prompt = userInput,
            systemPrompt = systemPrompt,
            conversationHistory = history.takeLast(6),
            temperature = 0.4f
        )

        val aiResult = providerRouter.executeWithRouting(aiRequest)

        return if (aiResult.isSuccess) {
            val response = aiResult.getOrThrow()
            // Check if model extracted user facts to remember
            if (userInput.lowercase().contains("my name is") || userInput.lowercase().contains("i like") || userInput.lowercase().contains("remember that")) {
                memoryEngine.storeMemory(userInput, MemoryCategory.IMPORTANT_FACT, importance = 4)
            }
            BrainResult(
                responseText = response.content,
                telemetryReport = "Routed via ${response.provider} (${response.modelId}) in ${response.latencyMs}ms"
            )
        } else {
            // Local Degraded / Offline Mode (Phase 36)
            BrainResult(
                responseText = "AURIX [Offline Mode]: I couldn't reach cloud AI providers right now (${aiResult.exceptionOrNull()?.message?.take(60)}). Local device automation tools (Flashlight, Volume, Apps, Alarms) are still fully active.",
                telemetryReport = "Degraded mode active"
            )
        }
    }

    private fun stepsSummary(steps: List<com.example.aurix.data.model.MissionStep>): String {
        return steps.joinToString(" -> ") { it.toolName }
    }

    private fun parseArgs(json: String): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        try {
            val obj = org.json.JSONObject(json)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = obj.get(k)
            }
        } catch (_: Exception) {}
        return map
    }
}
