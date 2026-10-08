package com.example.aurix.brain

import com.example.aurix.data.model.MissionStep
import com.example.aurix.data.model.StepStatus
import org.json.JSONObject
import java.util.Locale

object ConversationActionCompiler {

    data class CompiledPlan(
        val isMultiStepMission: Boolean,
        val missionTitle: String,
        val userGoal: String,
        val steps: List<MissionStep>
    )

    fun compile(input: String): CompiledPlan {
        val clean = input.trim()
        val lower = clean.lowercase(Locale.ROOT)
        val steps = mutableListOf<MissionStep>()

        // Check for Intent Fusion: Multiple intentions in single request (Phase 4)
        // e.g. "Mummy ko call karke bol de main late hoon aur 8 baje reminder laga dena"
        val hasCallIntent = lower.contains("call") || lower.contains("phone lagao") || lower.contains("dial")
        val hasMessageIntent = lower.contains("message") || lower.contains("bol de") || lower.contains("whatsapp") || lower.contains("sms")
        val hasReminderIntent = lower.contains("reminder") || lower.contains("yaad dila") || lower.contains("alarm")
        val hasFlashlightIntent = lower.contains("flashlight") || lower.contains("torch")
        val hasVolumeIntent = lower.contains("volume") || lower.contains("awaaz")
        val hasAppIntent = lower.contains("open") || lower.contains("kholo") || lower.contains("chalao")

        var stepIdx = 0

        // 1. Flashlight
        if (hasFlashlightIntent) {
            val turnOn = !lower.contains("off") && !lower.contains("band")
            steps.add(
                MissionStep(
                    stepIndex = stepIdx++,
                    title = if (turnOn) "Turn On Flashlight" else "Turn Off Flashlight",
                    toolName = "FLASHLIGHT",
                    argumentsJson = JSONObject().put("enabled", turnOn).toString(),
                    status = StepStatus.PENDING
                )
            )
        }

        // 2. Volume
        if (hasVolumeIntent) {
            val isMute = lower.contains("mute") || lower.contains("silent")
            val isUp = lower.contains("up") || lower.contains("badhao") || lower.contains("increase")
            val isDown = lower.contains("down") || lower.contains("kam") || lower.contains("decrease")
            val action = when {
                isMute -> "mute"
                isUp -> "up"
                isDown -> "down"
                else -> "set"
            }
            steps.add(
                MissionStep(
                    stepIndex = stepIdx++,
                    title = "Adjust Volume ($action)",
                    toolName = "VOLUME",
                    argumentsJson = JSONObject().put("action", action).put("level", 60).toString(),
                    status = StepStatus.PENDING
                )
            )
        }

        // 3. Call Contact
        if (hasCallIntent && !hasMessageIntent) {
            val contact = extractContact(lower)
            steps.add(
                MissionStep(
                    stepIndex = stepIdx++,
                    title = "Call Contact: $contact",
                    toolName = "CALL_CONTACT",
                    argumentsJson = JSONObject().put("contactName", contact).toString(),
                    status = StepStatus.PENDING
                )
            )
        }

        // 4. Send Message / WhatsApp
        if (hasMessageIntent) {
            val contact = extractContact(lower)
            val msgText = extractMessageContent(clean)
            val isWhatsApp = lower.contains("whatsapp") || !lower.contains("sms")
            steps.add(
                MissionStep(
                    stepIndex = stepIdx++,
                    title = "Send message to $contact",
                    toolName = if (isWhatsApp) "SEND_MESSAGE" else "SEND_SMS",
                    argumentsJson = JSONObject()
                        .put("recipient", contact)
                        .put("phoneNumber", contact)
                        .put("message", msgText)
                        .put("app", if (isWhatsApp) "whatsapp" else "sms")
                        .toString(),
                    status = StepStatus.PENDING
                )
            )
        }

        // 5. Reminder
        if (hasReminderIntent) {
            val time = extractTime(lower)
            val remTitle = if (hasMessageIntent) "Follow up on sent message" else "AURIX Reminder"
            steps.add(
                MissionStep(
                    stepIndex = stepIdx++,
                    title = "Create Reminder: $remTitle at $time",
                    toolName = "CREATE_REMINDER",
                    argumentsJson = JSONObject().put("title", remTitle).put("timeString", time).toString(),
                    status = StepStatus.PENDING
                )
            )
        }

        // 6. Open App
        if (hasAppIntent && steps.isEmpty()) {
            val app = extractAppName(lower)
            steps.add(
                MissionStep(
                    stepIndex = stepIdx++,
                    title = "Launch App: $app",
                    toolName = "OPEN_APP",
                    argumentsJson = JSONObject().put("appName", app).toString(),
                    status = StepStatus.PENDING
                )
            )
        }

        return CompiledPlan(
            isMultiStepMission = steps.size > 1,
            missionTitle = if (steps.size > 1) "Multi-Action Mission: ${steps.size} steps" else (steps.firstOrNull()?.title ?: "Conversational Query"),
            userGoal = input,
            steps = steps
        )
    }

    private fun extractContact(text: String): String {
        val targets = listOf("mummy", "mom", "papa", "dad", "rahul", "priya", "amit", "bhai", "sister", "boss", "rohan")
        for (t in targets) {
            if (text.contains(t)) return t.replaceFirstChar { it.uppercase() }
        }
        val match = Regex("(?:to|ko|call|bol de|message)\\s+([a-zA-Z]+)").find(text)
        return match?.groupValues?.getOrNull(1)?.replaceFirstChar { it.uppercase() } ?: "Contact"
    }

    private fun extractMessageContent(text: String): String {
        val lower = text.lowercase(Locale.ROOT)
        val markers = listOf("bol de", "tell", "say", "message ki", "message that")
        for (m in markers) {
            val idx = lower.indexOf(m)
            if (idx != -1) {
                var extracted = text.substring(idx + m.length).trim()
                // Trim trailing conjunctions
                val aurIdx = extracted.lowercase().indexOf(" aur ")
                if (aurIdx != -1) extracted = extracted.substring(0, aurIdx).trim()
                val andIdx = extracted.lowercase().indexOf(" and ")
                if (andIdx != -1) extracted = extracted.substring(0, andIdx).trim()
                return extracted.ifBlank { "I'm running late." }
            }
        }
        return "Hey, updating you from AURIX."
    }

    private fun extractTime(text: String): String {
        val timeRegex = Regex("([0-9]{1,2}(?::[0-9]{2})?\\s*(?:am|pm|baje)?)")
        val match = timeRegex.find(text)
        return match?.value ?: "8:00 PM"
    }

    private fun extractAppName(text: String): String {
        val apps = listOf("spotify", "youtube", "whatsapp", "instagram", "camera", "settings", "chrome", "maps", "gmail")
        for (a in apps) {
            if (text.contains(a)) return a
        }
        return "app"
    }
}
