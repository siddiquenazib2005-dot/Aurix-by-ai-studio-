package com.example.aurix.tools

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class ToolRegistry(context: Context) {
    private val toolsMap = mutableMapOf<String, AurixTool>()

    init {
        // Register default Android tools
        register(FlashlightTool(context))
        register(VolumeTool(context))
        register(DeviceStatusTool(context))
        register(CallContactTool(context))
        register(SendSmsTool(context))
        register(SendMessageTool(context))
        register(OpenAppTool(context))
        register(MediaControlTool(context))
        register(CalendarReminderTool(context))
        register(WebSearchTool(context))
        register(VisionTool(context))
        register(ContactSearchTool(context))
        register(LocationTool(context))
        register(CameraCaptureTool(context))
        register(FileSearchTool(context))
        register(AppAutomationTool(context))
        register(ScreenAnalysisTool(context))
    }

    fun register(tool: AurixTool) {
        toolsMap[tool.name.uppercase()] = tool
    }

    fun getTool(name: String): AurixTool? {
        return toolsMap[name.uppercase()]
    }

    fun getAllTools(): List<AurixTool> = toolsMap.values.toList()

    fun getToolDefinitionsPrompt(): String {
        val root = JSONArray()
        for (tool in toolsMap.values) {
            val toolObj = JSONObject()
            toolObj.put("name", tool.name)
            toolObj.put("description", tool.description)
            toolObj.put("isSensitive", tool.isSensitive)

            val paramsObj = JSONObject()
            for (p in tool.parameters) {
                val pObj = JSONObject()
                pObj.put("type", p.type)
                pObj.put("description", p.description)
                pObj.put("required", p.isRequired)
                paramsObj.put(p.name, pObj)
            }
            toolObj.put("parameters", paramsObj)
            root.put(toolObj)
        }
        return root.toString()
    }
}
