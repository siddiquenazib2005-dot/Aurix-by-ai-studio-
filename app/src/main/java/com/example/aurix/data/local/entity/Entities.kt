package com.example.aurix.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.aurix.data.model.MemoryCategory
import com.example.aurix.data.model.MissionStatus
import com.example.aurix.data.model.ProviderHealth
import com.example.aurix.data.model.ProviderType

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val summary: String? = null
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val sender: String, // "user", "aurix", "system", "tool"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val toolCallsJson: String? = null,
    val toolResultJson: String? = null,
    val missionId: String? = null,
    val isStreaming: Boolean = false
)

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey val id: String,
    val category: MemoryCategory,
    val content: String,
    val tags: String = "", // Comma-separated tags
    val importance: Int = 3, // 1 to 5
    val createdAt: Long = System.currentTimeMillis(),
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val accessCount: Int = 1,
    val metadata: String? = null
)

@Entity(tableName = "missions")
data class MissionEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val title: String,
    val userGoal: String,
    val status: MissionStatus,
    val stepsJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val errorReason: String? = null,
    val currentStepIndex: Int = 0
)

@Entity(tableName = "provider_keys")
data class ProviderKeyEntity(
    @PrimaryKey val id: String,
    val providerType: ProviderType,
    val label: String,
    val apiKeyMasked: String,
    val apiKeyEncrypted: String,
    val modelId: String,
    val isActive: Boolean = true,
    val healthStatus: ProviderHealth = ProviderHealth.HEALTHY,
    val latencyMs: Long = 0L,
    val lastUsedAt: Long = 0L,
    val failureCount: Int = 0,
    val cooldownUntilMs: Long = 0L,
    val lastError: String? = null
)

@Entity(tableName = "custom_commands")
data class CustomCommandEntity(
    @PrimaryKey val id: String,
    val name: String,
    val triggerPhrase: String,
    val actionsJson: String,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notification_audits")
data class NotificationAuditEntity(
    @PrimaryKey val id: String,
    val appPackage: String,
    val title: String,
    val text: String,
    val category: String, // URGENT, IMPORTANT, NORMAL, PROMOTIONAL
    val timestamp: Long = System.currentTimeMillis(),
    val isProcessed: Boolean = false
)

@Entity(tableName = "device_telemetry")
data class DeviceTelemetryEntity(
    @PrimaryKey val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val toolName: String,
    val parametersJson: String,
    val isSuccess: Boolean,
    val verificationReport: String,
    val message: String
)
