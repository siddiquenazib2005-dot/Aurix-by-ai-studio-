package com.example.aurix.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.aurix.data.local.entity.ConversationEntity
import com.example.aurix.data.local.entity.CustomCommandEntity
import com.example.aurix.data.local.entity.DeviceTelemetryEntity
import com.example.aurix.data.local.entity.MemoryEntity
import com.example.aurix.data.local.entity.MessageEntity
import com.example.aurix.data.local.entity.MissionEntity
import com.example.aurix.data.local.entity.NotificationAuditEntity
import com.example.aurix.data.local.entity.ProviderKeyEntity
import com.example.aurix.data.model.MemoryCategory
import com.example.aurix.data.model.MissionStatus
import com.example.aurix.data.model.ProviderType
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getConversationById(id: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(conversation: ConversationEntity)

    @Query("UPDATE conversations SET title = :newTitle, updatedAt = :updatedAt WHERE id = :id")
    suspend fun renameConversation(id: String, newTitle: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversation(id: String)

    @Query("DELETE FROM messages WHERE conversationId = :id")
    suspend fun deleteMessagesForConversation(id: String)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    suspend fun getMessagesList(conversationId: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(conversationId: String, limit: Int = 10): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessage(id: String)
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories ORDER BY importance DESC, lastAccessedAt DESC")
    fun getAllMemories(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE category = :category ORDER BY importance DESC")
    suspend fun getMemoriesByCategory(category: MemoryCategory): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE content LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%'")
    suspend fun searchMemories(query: String): List<MemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateMemory(memory: MemoryEntity)

    @Query("UPDATE memories SET accessCount = accessCount + 1, lastAccessedAt = :now WHERE id = :id")
    suspend fun recordMemoryAccess(id: String, now: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteMemory(memory: MemoryEntity)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteMemoryById(id: String)
}

@Dao
interface MissionDao {
    @Query("SELECT * FROM missions ORDER BY updatedAt DESC")
    fun getAllMissions(): Flow<List<MissionEntity>>

    @Query("SELECT * FROM missions WHERE status IN ('PENDING', 'IN_PROGRESS', 'AWAITING_APPROVAL') ORDER BY updatedAt DESC")
    suspend fun getActiveMissions(): List<MissionEntity>

    @Query("SELECT * FROM missions WHERE id = :id LIMIT 1")
    suspend fun getMissionById(id: String): MissionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateMission(mission: MissionEntity)

    @Query("UPDATE missions SET status = :status, updatedAt = :now WHERE id = :id")
    suspend fun updateMissionStatus(id: String, status: MissionStatus, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM missions WHERE id = :id")
    suspend fun deleteMission(id: String)
}

@Dao
interface ProviderKeyDao {
    @Query("SELECT * FROM provider_keys ORDER BY lastUsedAt ASC")
    fun getAllProviderKeysFlow(): Flow<List<ProviderKeyEntity>>

    @Query("SELECT * FROM provider_keys WHERE providerType = :type AND isActive = 1")
    suspend fun getActiveKeysForProvider(type: ProviderType): List<ProviderKeyEntity>

    @Query("SELECT * FROM provider_keys WHERE isActive = 1")
    suspend fun getAllActiveKeys(): List<ProviderKeyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(key: ProviderKeyEntity)

    @Query("UPDATE provider_keys SET latencyMs = :latency, lastUsedAt = :now, failureCount = 0 WHERE id = :id")
    suspend fun recordSuccess(id: String, latency: Long, now: Long = System.currentTimeMillis())

    @Query("UPDATE provider_keys SET failureCount = failureCount + 1, cooldownUntilMs = :cooldownUntil, lastError = :error WHERE id = :id")
    suspend fun recordFailure(id: String, cooldownUntil: Long, error: String)

    @Delete
    suspend fun delete(key: ProviderKeyEntity)

    @Query("DELETE FROM provider_keys WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface CustomCommandDao {
    @Query("SELECT * FROM custom_commands ORDER BY createdAt DESC")
    fun getAllCommands(): Flow<List<CustomCommandEntity>>

    @Query("SELECT * FROM custom_commands WHERE isEnabled = 1")
    suspend fun getEnabledCommands(): List<CustomCommandEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(command: CustomCommandEntity)

    @Delete
    suspend fun delete(command: CustomCommandEntity)
}

@Dao
interface NotificationAuditDao {
    @Query("SELECT * FROM notification_audits ORDER BY timestamp DESC LIMIT 50")
    fun getRecentAudits(): Flow<List<NotificationAuditEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudit(audit: NotificationAuditEntity)
}

@Dao
interface TelemetryDao {
    @Query("SELECT * FROM device_telemetry ORDER BY timestamp DESC LIMIT 100")
    fun getRecentTelemetry(): Flow<List<DeviceTelemetryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTelemetry(item: DeviceTelemetryEntity)
}
