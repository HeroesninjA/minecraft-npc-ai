package ro.ainpc.engine

import java.util.UUID

class QuestProgressLookupService(
    private val getActiveQuests: (UUID) -> MutableMap<String, PlayerQuestProgress>?,
    private val getArchivedQuests: (UUID) -> MutableMap<String, PlayerQuestProgress>?,
) {
    fun getCurrentQuestProgress(playerId: UUID): List<PlayerQuestProgress> {
        val currentQuests = getActiveQuests(playerId) ?: return emptyList()
        return currentQuests.values
            .filter { it.isCurrent() }
            .sortedWith(compareByDescending<PlayerQuestProgress> { it.updatedAt() }.thenBy { it.templateId() ?: "" })
    }

    fun getCurrentQuestProgress(playerId: UUID?, templateId: String?): PlayerQuestProgress? {
        if (playerId == null || templateId.isNullOrBlank()) return null
        val currentQuests = getActiveQuests(playerId) ?: return null
        return currentQuests[templateId]
    }

    fun getArchivedQuestProgress(playerId: UUID): List<PlayerQuestProgress> {
        val archived = getArchivedQuests(playerId) ?: return emptyList()
        return archived.values.toList()
    }

    fun getArchivedQuestProgress(playerId: UUID, templateId: String): PlayerQuestProgress? {
        if (templateId.isBlank()) return null
        return getArchivedQuests(playerId)?.get(templateId)
    }
}
