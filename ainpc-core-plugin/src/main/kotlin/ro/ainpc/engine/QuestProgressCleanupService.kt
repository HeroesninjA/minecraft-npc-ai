package ro.ainpc.engine

import java.util.UUID

class QuestProgressCleanupService(
    private val getActivePlayerQuests: () -> MutableMap<UUID, MutableMap<String, PlayerQuestProgress>>,
    private val getArchivedPlayerQuests: () -> MutableMap<UUID, MutableMap<String, PlayerQuestProgress>>,
    private val getQuestTemplates: () -> Map<String, ScenarioTemplate>,
    private val deleteQuestProgress: (UUID, String) -> Unit,
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
        getActivePlayerQuests().keys.removeIf { it !in onlinePlayerIds }
        getArchivedPlayerQuests().keys.removeIf { it !in onlinePlayerIds }
        sentCompletionMessages.removeIf { key ->
            key.split(":").firstOrNull()?.let { uid ->
                runCatching { UUID.fromString(uid) }.getOrNull()?.let { it !in onlinePlayerIds } ?: false
            } == true
        }
        trackedQuestPlayers.removeIf { it !in onlinePlayerIds }
        trackedQuestTemplates.keys.removeIf { it !in onlinePlayerIds }
        eventDebounceBuffer.keys.removeIf { key ->
            key.split(":").firstOrNull()?.let { uid ->
                runCatching { UUID.fromString(uid) }.getOrNull()?.let { it !in onlinePlayerIds } ?: false
            } == true
        }
        cleanupStaleTemplateProgress()
    }

    fun cleanupStaleTemplateProgress() {
        val knownTemplateIds = getQuestTemplates().keys.toSet()
        for ((playerId, quests) in getActivePlayerQuests()) {
            val staleKeys = quests.keys.filter { it !in knownTemplateIds }
            for (key in staleKeys) {
                quests.remove(key)
                deleteQuestProgress(playerId, key)
            }
        }
        for ((playerId, quests) in getArchivedPlayerQuests()) {
            val staleKeys = quests.keys.filter { it !in knownTemplateIds }
            for (key in staleKeys) {
                quests.remove(key)
                deleteQuestProgress(playerId, key)
            }
        }
    }
}
