package com.example.aurix.commands

import com.example.aurix.data.local.dao.CustomCommandDao
import com.example.aurix.data.local.entity.CustomCommandEntity
import com.example.aurix.tools.RealityEngine
import com.example.aurix.tools.ToolRegistry
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class PersonalCommandManager(
    private val commandDao: CustomCommandDao,
    private val toolRegistry: ToolRegistry,
    private val realityEngine: RealityEngine
) {
    val allCommandsFlow: Flow<List<CustomCommandEntity>> = commandDao.getAllCommands()

    suspend fun createCommand(
        name: String,
        triggerPhrase: String,
        actions: List<CommandAction>
    ): CustomCommandEntity {
        val actionsArray = JSONArray()
        actions.forEach { act ->
            val obj = JSONObject()
            obj.put("toolName", act.toolName)
            obj.put("argumentsJson", act.argumentsJson)
            actionsArray.put(obj)
        }

        val entity = CustomCommandEntity(
            id = "cmd_${System.currentTimeMillis()}",
            name = name,
            triggerPhrase = triggerPhrase.lowercase(),
            actionsJson = actionsArray.toString(),
            isEnabled = true
        )
        commandDao.insertOrUpdate(entity)
        return entity
    }

    suspend fun executeCommand(command: CustomCommandEntity): List<String> {
        val reports = mutableListOf<String>()
        val actions = deserializeActions(command.actionsJson)

        for (action in actions) {
            val tool = toolRegistry.getTool(action.toolName)
            if (tool != null) {
                val args = parseArgs(action.argumentsJson)
                val result = realityEngine.auditAndVerify(tool, "call_cmd_${command.id}", args)
                reports.add("${tool.name}: ${result.output} (Verified: ${result.verificationVerified})")
            } else {
                reports.add("${action.toolName}: Tool not found")
            }
        }
        return reports
    }

    suspend fun findMatchingCommand(phrase: String): CustomCommandEntity? {
        val enabled = commandDao.getEnabledCommands()
        val lower = phrase.lowercase()
        return enabled.firstOrNull { lower.contains(it.triggerPhrase) }
    }

    suspend fun deleteCommand(command: CustomCommandEntity) {
        commandDao.delete(command)
    }

    fun deserializeActions(json: String): List<CommandAction> {
        val list = mutableListOf<CommandAction>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    CommandAction(
                        toolName = obj.getString("toolName"),
                        argumentsJson = obj.optString("argumentsJson", "{}")
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

    data class CommandAction(
        val toolName: String,
        val argumentsJson: String
    )
}
