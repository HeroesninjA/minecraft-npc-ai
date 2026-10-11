package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class QuestProgressCleanupServiceTest {
    private val playerId = UUID.randomUUID()
    private val active = mutableMapOf<UUID, MutableMap<String, PlayerQuestProgress>>()
    private val archived = mutableMapOf<UUID, MutableMap<String, PlayerQuestProgress>>()
    private val templates = mutableMapOf<String, ScenarioTemplate>()
    private val onlinePlayers = mutableSetOf<UUID>()
    private val blocks = mutableMapOf<UUID, MutableSet<String>>()
    private val places = mutableMapOf<UUID, MutableSet<String>>()
    private val conversations = mutableMapOf<UUID, Long>()
    private val regionEntries = mutableMapOf<UUID, MutableMap<String, Int>>()
    private val debounce = mutableMapOf<String, Long>()
    private val completionMessages = mutableSetOf<String>()
    private val trackedPlayers = mutableSetOf<UUID>()
    private val trackedTemplates = mutableMapOf<UUID, String>()
    private val preferenceWrites = mutableListOf<Pair<UUID, String>>()
    private val cleanup = QuestProgressCleanupService(
        getActivePlayerQuests = { active },
        getQuestTemplates = { templates },
        trackedBlockLocations = blocks,
        trackedVisitedPlaces = places,
        npcConversationCooldowns = conversations,
        regionEntryCounts = regionEntries,
        eventDebounceBuffer = debounce,
        sentCompletionMessages = completionMessages,
        trackedQuestPlayers = trackedPlayers,
        trackedQuestTemplates = trackedTemplates,
        persistQuestTrackingPreferenceAsync = { targetPlayerId, templateId ->
            preferenceWrites.add(targetPlayerId to templateId)
        },
        onlinePlayerIdsProvider = { onlinePlayers.toSet() },
    )
    private val lookup = QuestProgressLookupService({ active[it] }, { archived[it] })

    @Test
    fun offlineCleanupPreservesActiveProgressForReconnect() {
        val progress = addActiveQuest()

        cleanup.cleanupOrphanedObjectives()
        onlinePlayers.add(playerId)
        cleanup.cleanupOrphanedObjectives()

        assertSame(progress, lookup.getCurrentQuestProgress(playerId, "quest"))
        assertEquals(7, lookup.getCurrentQuestProgress(playerId, "quest")?.objectiveProgress()?.get("objective"))
    }

    @Test
    fun offlineCleanupPreservesCompletedAndFailedHistory() {
        addTemplate("completed")
        addTemplate("failed")
        val completed = progress("completed", QuestStatus.COMPLETED)
        val failed = progress("failed", QuestStatus.FAILED)
        archived[playerId] = mutableMapOf("completed" to completed, "failed" to failed)

        cleanup.cleanupOrphanedObjectives()

        assertSame(completed, lookup.getArchivedQuestProgress(playerId, "completed"))
        assertSame(failed, lookup.getArchivedQuestProgress(playerId, "failed"))
    }

    @Test
    fun missingPackDoesNotDeleteActiveOrArchivedProgress() {
        onlinePlayers.add(playerId)
        val current = addActiveQuest()
        val completed = progress("completed", QuestStatus.COMPLETED)
        archived[playerId] = mutableMapOf("completed" to completed)
        templates.clear()

        cleanup.cleanupStaleTemplateProgress()
        cleanup.cleanupOrphanedObjectives()
        addTemplate("quest")
        addTemplate("completed")
        cleanup.cleanupStaleTemplateProgress()

        assertSame(current, lookup.getCurrentQuestProgress(playerId, "quest"))
        assertSame(completed, lookup.getArchivedQuestProgress(playerId, "completed"))
    }

    @Test
    fun transientOfflineTrackingIsRemovedWithoutChangingStoredPreference() {
        addActiveQuest()
        trackedPlayers.add(playerId)
        trackedTemplates[playerId] = "quest"

        cleanup.cleanupOrphanedObjectives()

        assertTrue(trackedPlayers.isEmpty())
        assertTrue(trackedTemplates.isEmpty())
        assertTrue(preferenceWrites.isEmpty())
    }

    @Test
    fun missingTemplateStopsOnlyItsRuntimeTracker() {
        onlinePlayers.add(playerId)
        addActiveQuest()
        trackedPlayers.add(playerId)
        trackedTemplates[playerId] = "quest"
        templates.clear()

        cleanup.cleanupStaleTemplateProgress()

        assertTrue(trackedPlayers.isEmpty())
        assertTrue(trackedTemplates.isEmpty())
        assertTrue(preferenceWrites.isEmpty())
        assertEquals(1, active[playerId]?.size)
    }

    @Test
    fun trackingWithoutCurrentProgressIsRemovedEvenWhenTemplateExists() {
        onlinePlayers.add(playerId)
        addTemplate("quest")
        trackedPlayers.add(playerId)
        trackedTemplates[playerId] = "quest"

        cleanup.cleanupStaleTemplateProgress()

        assertTrue(trackedPlayers.isEmpty())
        assertTrue(trackedTemplates.isEmpty())
    }

    @Test
    fun currentOnlineTrackerRemainsActive() {
        onlinePlayers.add(playerId)
        addActiveQuest()
        trackedPlayers.add(playerId)
        trackedTemplates[playerId] = "quest"

        cleanup.cleanupOrphanedObjectives()

        assertEquals(setOf(playerId), trackedPlayers)
        assertEquals(mapOf(playerId to "quest"), trackedTemplates)
        assertTrue(preferenceWrites.isEmpty())
    }

    @Test
    fun runtimeTrackerWithoutSelectionIsRemoved() {
        onlinePlayers.add(playerId)
        addActiveQuest()
        trackedPlayers.add(playerId)

        cleanup.cleanupOrphanedObjectives()

        assertTrue(trackedPlayers.isEmpty())
    }

    @Test
    fun offlineResumableQuestsKeepDuplicatePreventionState() {
        for (status in listOf(QuestStatus.ACTIVE, QuestStatus.OFFERED, QuestStatus.SUSPENDED)) {
            active[playerId] = mutableMapOf("quest" to progress(status = status))
            addTemplate("quest")
            seedObjectiveState(playerId)

            cleanup.cleanupOrphanedObjectives()

            assertEquals(setOf("world:1:2:3"), blocks[playerId])
            assertEquals(setOf("market"), places[playerId])
            assertEquals(mapOf("region" to 2), regionEntries[playerId])
            assertEquals(status, lookup.getCurrentQuestProgress(playerId, "quest")?.status())
        }
    }

    @Test
    fun objectiveStateWithoutResumableQuestIsReleased() {
        val currentPlayer = UUID.randomUUID()
        val completedPlayer = UUID.randomUUID()
        onlinePlayers.addAll(listOf(playerId, completedPlayer))
        active[currentPlayer] = mutableMapOf("quest" to progress())
        active[completedPlayer] = mutableMapOf("quest" to progress(status = QuestStatus.COMPLETED))
        addTemplate("quest")
        listOf(playerId, currentPlayer, completedPlayer).forEach(::seedObjectiveState)

        cleanup.cleanupOrphanedObjectives()

        assertEquals(setOf(currentPlayer), blocks.keys)
        assertEquals(setOf(currentPlayer), places.keys)
        assertEquals(setOf(currentPlayer), regionEntries.keys)
    }

    @Test
    fun offlineCooldownsAreRemovedWithoutAffectingOnlinePlayer() {
        val otherPlayer = UUID.randomUUID()
        onlinePlayers.add(otherPlayer)
        conversations[playerId] = 10L
        conversations[otherPlayer] = 20L
        debounce["$playerId:quest:objective"] = 10L
        debounce["$otherPlayer:quest:objective"] = 20L
        completionMessages.addAll(listOf("$playerId:objective", "$otherPlayer:objective"))

        cleanup.cleanupOrphanedObjectives()

        assertEquals(mapOf(otherPlayer to 20L), conversations)
        assertEquals(mapOf("$otherPlayer:quest:objective" to 20L), debounce)
        assertEquals(setOf("$otherPlayer:objective"), completionMessages)
    }

    @Test
    fun malformedCompoundKeysDoNotAbortCleanup() {
        debounce["not-a-uuid:objective"] = 10L
        debounce["$playerId:quest:objective"] = 20L
        completionMessages.addAll(listOf("invalid", "$playerId:objective"))

        cleanup.cleanupOrphanedObjectives()

        assertFalse(debounce.containsKey("$playerId:quest:objective"))
        assertFalse(completionMessages.contains("$playerId:objective"))
    }

    @Test
    fun explicitTrackingClearPersistsOnlyMatchingSelection() {
        trackedPlayers.add(playerId)
        trackedTemplates[playerId] = "quest"

        cleanup.clearQuestTrackingIfMatches(playerId, "different")
        assertTrue(preferenceWrites.isEmpty())
        assertTrue(playerId in trackedPlayers)
        cleanup.clearQuestTrackingIfMatches(playerId, "quest")
        cleanup.clearQuestTrackingIfMatches(playerId, "quest")

        assertEquals(listOf(playerId to ""), preferenceWrites)
        assertTrue(trackedPlayers.isEmpty())
        assertTrue(trackedTemplates.isEmpty())
    }

    private fun addActiveQuest(): PlayerQuestProgress {
        addTemplate("quest")
        return progress().also { active[playerId] = mutableMapOf("quest" to it) }
    }

    private fun addTemplate(templateId: String) {
        templates[templateId] = ScenarioTemplate(ScenarioType.QUEST).apply { this.templateId = templateId }
    }

    private fun progress(templateId: String = "quest", status: QuestStatus = QuestStatus.ACTIVE) =
        PlayerQuestProgress(templateId, "code", status, 1L, 0L, 2L, "EXECUTION",
            mapOf("objective" to 7), mapOf("anchor.objective.id" to "market"))

    private fun seedObjectiveState(targetPlayerId: UUID) {
        blocks[targetPlayerId] = mutableSetOf("world:1:2:3")
        places[targetPlayerId] = mutableSetOf("market")
        regionEntries[targetPlayerId] = mutableMapOf("region" to 2)
    }
}
