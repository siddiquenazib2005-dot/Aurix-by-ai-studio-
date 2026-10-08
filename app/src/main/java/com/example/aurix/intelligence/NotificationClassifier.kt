package com.example.aurix.intelligence

import com.example.aurix.data.local.dao.NotificationAuditDao
import com.example.aurix.data.local.entity.NotificationAuditEntity
import java.util.Locale

object NotificationClassifier {

    enum class PriorityCategory {
        URGENT,
        IMPORTANT,
        NORMAL,
        PROMOTIONAL,
        IRRELEVANT
    }

    data class ClassifiedNotification(
        val appPackage: String,
        val title: String,
        val text: String,
        val category: PriorityCategory,
        val summary: String
    )

    fun classify(appPackage: String, title: String, text: String): ClassifiedNotification {
        val combined = "$title $text".lowercase(Locale.ROOT)

        val category = when {
            combined.contains("otp") || combined.contains("emergency") || combined.contains("urgent") || combined.contains("call missed") || combined.contains("alert") -> {
                PriorityCategory.URGENT
            }
            combined.contains("message") || combined.contains("whatsapp") || combined.contains("telegram") || combined.contains("meeting") || combined.contains("flight") -> {
                PriorityCategory.IMPORTANT
            }
            combined.contains("discount") || combined.contains("sale") || combined.contains("offer") || combined.contains("cashback") || combined.contains("promo") -> {
                PriorityCategory.PROMOTIONAL
            }
            combined.contains("cleaning") || combined.contains("battery saver") || combined.contains("syncing") -> {
                PriorityCategory.IRRELEVANT
            }
            else -> PriorityCategory.NORMAL
        }

        val summary = when (category) {
            PriorityCategory.URGENT -> "🚨 [Urgent] $title: ${text.take(60)}"
            PriorityCategory.IMPORTANT -> "⭐ [Important] $title: ${text.take(60)}"
            PriorityCategory.PROMOTIONAL -> "🏷️ [Promo] $title"
            PriorityCategory.NORMAL -> "📩 $title"
            PriorityCategory.IRRELEVANT -> "ℹ️ System notification"
        }

        return ClassifiedNotification(appPackage, title, text, category, summary)
    }

    fun generateAuditSummary(audits: List<NotificationAuditEntity>): String {
        if (audits.isEmpty()) return "No notifications captured."
        val urgent = audits.filter { it.category == PriorityCategory.URGENT.name }
        val important = audits.filter { it.category == PriorityCategory.IMPORTANT.name }

        return buildString {
            append("Notification Intelligence Summary:\n")
            if (urgent.isNotEmpty()) {
                append("• ${urgent.size} Urgent alerts (OTP/Missed calls)\n")
            }
            if (important.isNotEmpty()) {
                append("• ${important.size} Important communications\n")
            }
            append("• ${audits.size} Total notifications processed")
        }
    }
}
