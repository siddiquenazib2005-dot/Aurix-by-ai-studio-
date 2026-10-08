package com.example.aurix.mission

import com.example.aurix.data.local.dao.MissionDao
import com.example.aurix.data.local.entity.MissionEntity
import com.example.aurix.data.model.AutonomyMode
import com.example.aurix.data.model.MissionStatus
import com.example.aurix.data.model.MissionStep
import com.example.aurix.data.model.StepStatus
import com.example.aurix.tools.RealityEngine
import com.example.aurix.tools.ToolRegistry
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class MissionEngine(
    private val missionDao: MissionDao,
    private val toolRegistry: ToolRegistry,
    private val realityEngine: RealityEngine
) {
    val allMissionsFlow: Flow<List<MissionEntity>> = missionDao.getAllMissions()

    suspend fun createMission(
        conversationId: String,
        title: String,
        userGoal: String,
        steps: List<MissionStep>
    ): MissionEntity {
        val stepsJson = serializeSteps(steps)
        val entity = MissionEntity(
            id = "mission_${System.currentTimeMillis()}",
            conversationId = conversationId,
            title = title,
            userGoal = userGoal,
            status = MissionStatus.PENDING,
            stepsJson = stepsJson,
            currentStepIndex = 0
        )
        missionDao.insertOrUpdateMission(entity)
        return entity
    }

    suspend fun executeOrResumeMission(
        missionId: String,
        autonomyMode: AutonomyMode,
        onApprovalRequired: suspend (step: MissionStep) -> Boolean = { true }
    ): MissionEntity {
        var mission = missionDao.getMissionById(missionId)
            ?: throw IllegalArgumentException("Mission $missionId not found")

        val steps = deserializeSteps(mission.stepsJson).toMutableList()
        missionDao.updateMissionStatus(mission.id, MissionStatus.IN_PROGRESS)

        for (i in mission.currentStepIndex until steps.size) {
            val step = steps[i]

            // If already succeeded, skip to avoid repeating actions (Phase 8 Mission Resume)
            if (step.status == StepStatus.SUCCESS) continue

            step.status = StepStatus.EXECUTING
            saveProgress(mission, steps, i)

            val tool = toolRegistry.getTool(step.toolName)
            if (tool == null) {
                step.status = StepStatus.FAILED
                step.errorReason = "Tool ${step.toolName} not registered"
                mission = mission.copy(status = MissionStatus.FAILED, errorReason = step.errorReason)
                saveProgress(mission, steps, i)
                return mission
            }

            // Check sensitive approval
            if (tool.isSensitive && autonomyMode != AutonomyMode.SAFE_AUTO) {
                missionDao.updateMissionStatus(mission.id, MissionStatus.AWAITING_APPROVAL)
                val approved = onApprovalRequired(step)
                if (!approved) {
                    step.status = StepStatus.SKIPPED
                    step.errorReason = "User rejected execution"
                    mission = mission.copy(status = MissionStatus.CANCELLED)
                    saveProgress(mission, steps, i)
                    return mission
                }
            }

            // Parse arguments
            val args = parseArgs(step.argumentsJson)

            // Execute & Verify via Reality Engine
            step.status = StepStatus.VERIFYING
            saveProgress(mission, steps, i)

            var result = realityEngine.auditAndVerify(tool, "call_${step.stepIndex}", args)

            // Phase 9 Self-Healing Retry
            if (!result.isSuccess && step.retryCount < 2) {
                step.retryCount++
                result = realityEngine.auditAndVerify(tool, "call_retry_${step.stepIndex}", args)
            }

            if (result.isSuccess) {
                step.status = StepStatus.SUCCESS
                step.realityVerificationReport = result.deviceRealityReport
                step.executionResult = result.output
            } else {
                step.status = StepStatus.FAILED
                step.errorReason = result.deviceRealityReport
                mission = mission.copy(status = MissionStatus.FAILED, errorReason = result.deviceRealityReport)
                saveProgress(mission, steps, i)
                return mission
            }

            saveProgress(mission, steps, i + 1)
        }

        mission = mission.copy(
            status = MissionStatus.COMPLETED,
            stepsJson = serializeSteps(steps),
            currentStepIndex = steps.size
        )
        missionDao.insertOrUpdateMission(mission)
        return mission
    }

    private suspend fun saveProgress(mission: MissionEntity, steps: List<MissionStep>, nextIdx: Int) {
        val updated = mission.copy(
            stepsJson = serializeSteps(steps),
            currentStepIndex = nextIdx,
            updatedAt = System.currentTimeMillis()
        )
        missionDao.insertOrUpdateMission(updated)
    }

    fun serializeSteps(steps: List<MissionStep>): String {
        val arr = JSONArray()
        for (s in steps) {
            val obj = JSONObject()
            obj.put("stepIndex", s.stepIndex)
            obj.put("title", s.title)
            obj.put("toolName", s.toolName)
            obj.put("argumentsJson", s.argumentsJson)
            obj.put("status", s.status.name)
            obj.put("realityVerificationReport", s.realityVerificationReport ?: "")
            obj.put("errorReason", s.errorReason ?: "")
            obj.put("executionResult", s.executionResult ?: "")
            obj.put("retryCount", s.retryCount)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun deserializeSteps(json: String): List<MissionStep> {
        val list = mutableListOf<MissionStep>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    MissionStep(
                        stepIndex = obj.optInt("stepIndex", i),
                        title = obj.optString("title", "Step $i"),
                        toolName = obj.optString("toolName", ""),
                        argumentsJson = obj.optString("argumentsJson", "{}"),
                        status = try { StepStatus.valueOf(obj.optString("status", "PENDING")) } catch (_: Exception) { StepStatus.PENDING },
                        realityVerificationReport = obj.optString("realityVerificationReport").takeIf { it.isNotBlank() },
                        errorReason = obj.optString("errorReason").takeIf { it.isNotBlank() },
                        executionResult = obj.optString("executionResult").takeIf { it.isNotBlank() },
                        retryCount = obj.optInt("retryCount", 0)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun parseArgs(json: String): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        try {
            val obj = JSONObject(json)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = obj.get(k)
            }
        } catch (_: Exception) {}
        return map
    }
}
