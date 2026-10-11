package ro.ainpc.engine

import java.util.UUID

/**
 * Questprogresscleanupservice.
 */
class QuestProgressCleanupService(
    private val getActivePlayerQuests: () -> Map<UUID, Map<String, PlayerQuestProgress>>,
    private val getQuestTemplates: () -> Map<String, ScenarioTemplate>,
    private val trackedBlockLocations: MutableMap<UUID, MutableSet<String>>,
    private val trackedVisitedPlaces: MutableMap<UUID, MutableSet<String>>,
    private val npcConversationCooldowns: MutableMap<UUID, Long>,
    private val regionEntryCounts: MutableMap<UUID, MutableMap<String, Int>>,
    private val eventDebounceBuffer: MutableMap<String, Long>,
    private val sentCompletionMessages: MutableSet<String>,
    private val trackedQuestPlayers: MutableSet<UUID>,
    private val trackedQuestTemplates: MutableMap<UUID, String>,
    private val persistQuestTrackingPreferenceAsync: (UUID, String) -> Unit,
    private val onlinePlayerIdsProvider: () -> Set<UUID>,
) {
    fun clearQuestTrackingData(playerId: UUID) {
        trackedBlockLocations.remove(playerId)
        trackedVisitedPlaces.remove(playerId)
        npcConversationCooldowns.remove(playerId)
        regionEntryCounts.remove(playerId)
        eventDebounceBuffer.keys.removeIf { it.startsWith("$playerId:") }
    }

    fun clearQuestTrackingIfMatches(playerId: UUID?, templateId: String?) {
        if (playerId == null || templateId.isNullOrBlank()) return

        val trackedTemplateId = trackedQuestTemplates[playerId] ?: ""
        if (templateId != trackedTemplateId) return

        trackedQuestTemplates.remove(playerId)
        trackedQuestPlayers.remove(playerId)
        persistQuestTrackingPreferenceAsync(playerId, "")
    }

    fun cleanupOrphanedObjectives() {
        val onlinePlayerIds = onlinePlayerIdsProvider()
        val resumablePlayerIds = getActivePlayerQuests().asSequence()
            .filter { (_, quests) -> quests.values.any { it.isCurrent() || it.status() == QuestStatus.SUSPENDED } }
            .map { (playerId, _) -> playerId }
            .toSet()
        trackedBlockLocations.keys.removeIf { it !in resumablePlayerIds }
        trackedVisitedPlaces.keys.removeIf { it !in resumablePlayerIds }
        regionEntryCounts.keys.removeIf { it !in resumablePlayerIds }
        npcConversationCooldowns.keys.removeIf { it !in onlinePlayerIds }
        sentCompletionMessages.removeIf { isOfflinePlayerKey(it, onlinePlayerIds) }
        trackedQuestPlayers.removeIf { it !in onlinePlayerIds }
        trackedQuestTemplates.keys.removeIf { it !in onlinePlayerIds }
        eventDebounceBuffer.keys.removeIf { isOfflinePlayerKey(it, onlinePlayerIds) }
        cleanupStaleTemplateProgress()
    }

    fun cleanupStaleTemplateProgress() {
        val knownTemplateIds = getQuestTemplates().keys
        val activePlayerQuests = getActivePlayerQuests()
        trackedQuestTemplates.entries.removeIf { (playerId, templateId) ->
            templateId !in knownTemplateIds || activePlayerQuests[playerId]?.get(templateId)?.isCurrent() != true
        }
        trackedQuestPlayers.retainAll(trackedQuestTemplates.keys)
    }

    private fun isOfflinePlayerKey(key: String, onlinePlayerIds: Set<UUID>): Boolean {
        val playerId = runCatching { UUID.fromString(key.substringBefore(':')) }.getOrNull() ?: return false
        return playerId !in onlinePlayerIds
    }
}
