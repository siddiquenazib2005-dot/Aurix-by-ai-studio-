package com.example.aurix.memory

import com.example.aurix.data.local.dao.MemoryDao
import com.example.aurix.data.local.dao.MessageDao
import com.example.aurix.data.local.entity.MemoryEntity
import com.example.aurix.data.model.MemoryCategory
import kotlinx.coroutines.flow.Flow
import java.util.Locale

class SemanticMemoryEngine(
    private val memoryDao: MemoryDao,
    private val messageDao: MessageDao
) {
    val allMemoriesFlow: Flow<List<MemoryEntity>> = memoryDao.getAllMemories()

    suspend fun storeMemory(
        content: String,
        category: MemoryCategory = MemoryCategory.IMPORTANT_FACT,
        tags: List<String> = emptyList(),
        importance: Int = 3
    ) {
        val entity = MemoryEntity(
            id = "mem_${System.currentTimeMillis()}_${(100..999).random()}",
            category = category,
            content = content.trim(),
            tags = tags.joinToString(","),
            importance = importance.coerceIn(1, 5)
        )
        memoryDao.insertOrUpdateMemory(entity)
    }

    suspend fun retrieveRelevantMemories(userQuery: String, limit: Int = 4): List<MemoryEntity> {
        val queryTokens = tokenize(userQuery)
        if (queryTokens.isEmpty()) return emptyList()

        // Fetch candidate memories from DB
        val directMatches = mutableSetOf<MemoryEntity>()
        for (token in queryTokens) {
            if (token.length >= 3) {
                directMatches.addAll(memoryDao.searchMemories(token))
            }
        }

        // Add high importance user preferences
        directMatches.addAll(memoryDao.getMemoriesByCategory(MemoryCategory.USER_PREFERENCE))

        // Rank by TF-IDF / token match score + importance weight
        val ranked = directMatches.map { memory ->
            val memoryTokens = tokenize(memory.content + " " + memory.tags)
            val overlap = queryTokens.count { token -> memoryTokens.contains(token) }
            val score = (overlap * 2.5) + (memory.importance * 0.8) + (memory.accessCount * 0.1)
            Pair(memory, score)
        }.sortedByDescending { it.second }
            .map { it.first }
            .take(limit)

        // Record access for retrieved memories
        for (mem in ranked) {
            memoryDao.recordMemoryAccess(mem.id)
        }

        return ranked
    }

    suspend fun resolveTimelineQuery(conversationId: String, query: String): String? {
        val q = query.lowercase(Locale.ROOT)
        val isTimeline = q.contains("pehle") || q.contains("before") || q.contains("earlier") ||
                q.contains("kal") || q.contains("yesterday") || q.contains("last time") ||
                q.contains("last task") || q.contains("kya baat hui")

        if (!isTimeline) return null

        val recent = messageDao.getRecentMessages(conversationId, limit = 6)
        if (recent.isEmpty()) return "No previous messages recorded in this conversation."

        val historySummary = StringBuilder("Recent timeline context:\n")
        recent.reversed().forEach { msg ->
            historySummary.append("- [${msg.sender.uppercase()}]: ${msg.content.take(80)}\n")
        }
        return historySummary.toString()
    }

    suspend fun deleteMemory(id: String) {
        memoryDao.deleteMemoryById(id)
    }

    private fun tokenize(text: String): Set<String> {
        return text.lowercase(Locale.ROOT)
            .replace("[^a-z0-9 ]".toRegex(), " ")
            .split("\\s+".toRegex())
            .filter { it.isNotBlank() && it.length > 2 }
            .toSet()
    }
}
