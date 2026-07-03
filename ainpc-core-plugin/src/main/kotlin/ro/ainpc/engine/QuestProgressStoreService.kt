package ro.ainpc.engine

import java.util.UUID

class QuestProgressStoreService(
    private val getActiveQuests: (UUID) -> MutableMap<String, PlayerQuestProgress>?,
    private val getArchivedQuests: (UUID) -> MutableMap<String, PlayerQuestProgress>?,
) {
    fun putActiveQuestProgress(playerId: UUID, progress: PlayerQuestProgress) {
        val templateId = progress.templateId() ?: return
        val activeQuests = getActiveQuests(playerId) ?: return
        activeQuests[templateId] = progress
    }

    fun archiveQuestProgress(playerId: UUID, progress: PlayerQuestProgress) {
        val templateId = progress.templateId() ?: return
        val archivedQuests = getArchivedQuests(playerId) ?: return
        archivedQuests[templateId] = progress
    }

    fun removeActiveQuestProgress(playerId: UUID, templateId: String): Boolean {
        return getActiveQuests(playerId)?.remove(templateId) != null
    }

    fun removeArchivedQuestProgress(playerId: UUID, templateId: String): Boolean {
        return getArchivedQuests(playerId)?.remove(templateId) != null
    }
}
