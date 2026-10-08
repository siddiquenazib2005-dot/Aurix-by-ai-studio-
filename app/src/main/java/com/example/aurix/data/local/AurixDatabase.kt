package com.example.aurix.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.aurix.data.local.dao.ConversationDao
import com.example.aurix.data.local.dao.CustomCommandDao
import com.example.aurix.data.local.dao.MemoryDao
import com.example.aurix.data.local.dao.MessageDao
import com.example.aurix.data.local.dao.MissionDao
import com.example.aurix.data.local.dao.NotificationAuditDao
import com.example.aurix.data.local.dao.ProviderKeyDao
import com.example.aurix.data.local.dao.TelemetryDao
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
import com.example.aurix.data.model.ProviderHealth
import com.example.aurix.data.model.ProviderType

class Converters {
    @TypeConverter
    fun fromMemoryCategory(value: MemoryCategory): String = value.name

    @TypeConverter
    fun toMemoryCategory(value: String): MemoryCategory =
        try { MemoryCategory.valueOf(value) } catch (e: Exception) { MemoryCategory.WORKING_MEMORY }

    @TypeConverter
    fun fromMissionStatus(value: MissionStatus): String = value.name

    @TypeConverter
    fun toMissionStatus(value: String): MissionStatus =
        try { MissionStatus.valueOf(value) } catch (e: Exception) { MissionStatus.PENDING }

    @TypeConverter
    fun fromProviderType(value: ProviderType): String = value.name

    @TypeConverter
    fun toProviderType(value: String): ProviderType =
        try { ProviderType.valueOf(value) } catch (e: Exception) { ProviderType.GEMINI }

    @TypeConverter
    fun fromProviderHealth(value: ProviderHealth): String = value.name

    @TypeConverter
    fun toProviderHealth(value: String): ProviderHealth =
        try { ProviderHealth.valueOf(value) } catch (e: Exception) { ProviderHealth.HEALTHY }
}

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        MemoryEntity::class,
        MissionEntity::class,
        ProviderKeyEntity::class,
        CustomCommandEntity::class,
        NotificationAuditEntity::class,
        DeviceTelemetryEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AurixDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun memoryDao(): MemoryDao
    abstract fun missionDao(): MissionDao
    abstract fun providerKeyDao(): ProviderKeyDao
    abstract fun customCommandDao(): CustomCommandDao
    abstract fun notificationAuditDao(): NotificationAuditDao
    abstract fun telemetryDao(): TelemetryDao

    companion object {
        @Volatile
        private var INSTANCE: AurixDatabase? = null

        fun getInstance(context: Context): AurixDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AurixDatabase::class.java,
                    "aurix_brain.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
