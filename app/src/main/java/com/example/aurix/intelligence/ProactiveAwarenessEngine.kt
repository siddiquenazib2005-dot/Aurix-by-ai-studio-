package com.example.aurix.intelligence

import android.content.Context
import android.os.BatteryManager
import com.example.aurix.data.local.dao.TelemetryDao
import com.example.aurix.data.model.MemoryCategory
import com.example.aurix.memory.SemanticMemoryEngine
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar

class ProactiveAwarenessEngine(
    private val context: Context,
    private val telemetryDao: TelemetryDao,
    private val memoryEngine: SemanticMemoryEngine
) {
    data class ProactiveInsight(
        val title: String,
        val description: String,
        val suggestedAction: String? = null,
        val priority: Int = 1 // 1: normal, 2: high
    )

    suspend fun evaluateProactiveInsights(): List<ProactiveInsight> {
        val insights = mutableListOf<ProactiveInsight>()

        // 1. Battery Awareness
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
        val isCharging = bm?.isCharging == true

        if (level in 1..20 && !isCharging) {
            insights.add(
                ProactiveInsight(
                    title = "Low Battery ($level%)",
                    description = "Battery is below 20% and discharging. Would you like me to enable battery saving mode?",
                    suggestedAction = "Enable Battery Saver",
                    priority = 2
                )
            )
        }

        // 2. Time-of-Day Context
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (hour in 6..10) {
            insights.add(
                ProactiveInsight(
                    title = "Morning Briefing Ready",
                    description = "Good morning! Ready to brief notifications, device status, and upcoming tasks.",
                    suggestedAction = "Run Morning Briefing",
                    priority = 1
                )
            )
        } else if (hour in 21..23) {
            insights.add(
                ProactiveInsight(
                    title = "Night Wind-Down",
                    description = "It's late evening. Set volume to silent and turn off flash?",
                    suggestedAction = "Activate Night Routine",
                    priority = 1
                )
            )
        }

        // 3. Learned Workflow Pattern Detection (Phase 18)
        val recentTelem = telemetryDao.getRecentTelemetry().firstOrNull() ?: emptyList()
        val appOpens = recentTelem.filter { it.toolName == "OPEN_APP" }
        if (appOpens.size >= 3) {
            insights.add(
                ProactiveInsight(
                    title = "Learned Workflow Suggestion",
                    description = "You frequently launch apps through AURIX. Would you like to create a quick custom macro command?",
                    suggestedAction = "Create Macro",
                    priority = 1
                )
            )
        }

        return insights
    }
}
