package ro.ainpc.story

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.engine.QuestScoringResolver
import ro.ainpc.world.PlaceType
import ro.ainpc.world.WorldPlaceInfo

class StoryStructureCooldownTest {
    @Test
    fun activityNodeCooldownsAreRelevantToQuestGeneration() {
        val scorer = QuestScoringResolver { now }
        val activities = listOf(
            Triple("work", "profession", 22_000L),
            Triple("social", "leisure", 6_000L),
            Triple("home", "residential", 10_000L),
        )
        for ((activity, category, duration) in activities) {
            val events = listOf(event("structure_node_$activity", 1_000))
            val cooldown = resolver.addStructureQuestCooldownSignals(place, events).toMap()
            assertEquals(category, cooldown["quest_generation_cooldown_category"], activity)
            assertEquals(duration.toString(), cooldown["quest_generation_cooldown_ms"], activity)
            val signals = cooldown.map { (key, value) -> "$key=$value" } +
                listOf("place_id=${place.id()}", "structure_category=$category")
            val context = StoryContextSnapshot(null, null, null, null, null, null, events, emptyList(), signals, emptyList())
            assertTrue(scorer.hasQuestGenerationCooldown(context, emptyList()), activity)

            val fallbackSignals = signals.filterNot {
                it.startsWith("quest_generation_cooldown=") || it.startsWith("quest_generation_cooldown_ms=")
            }
            val expiredContext = StoryContextSnapshot(
                null, null, null, null, null, null,
                listOf(event("structure_node_$activity", duration)), emptyList(), fallbackSignals, emptyList(),
            )
            assertFalse(scorer.hasQuestGenerationCooldown(expiredContext, emptyList()), activity)
        }
    }

    private val now = 100_000L
    private val resolver = StoryStructureSignalResolver { now }
    private val place = WorldPlaceInfo(
        "village:forge", "village", "Forge", "world", PlaceType.FORGE,
        0, 0, 0, 10, 10, 10, emptyList(), null, true, emptyMap(),
    )

    @Test
    fun questCooldownRemainsActiveBeyondThirtySeconds() {
        val signals = resolver.addStructureQuestCooldownSignals(place, listOf(event("structure_node_quest", 35_000))).toMap()

        assertEquals("structure_recent", signals["quest_generation_cooldown"])
        assertEquals("45000", signals["quest_generation_cooldown_ms"])
        assertEquals("10000", signals["quest_generation_cooldown_remaining_ms"])
    }

    @Test
    fun longestRemainingCooldownWinsRegardlessOfEventOrder() {
        val quest = event("structure_node_quest", 20_000)
        val expiredVisit = event("structure_visit_leisure", 7_000)
        val recentVisit = event("structure_visit_leisure", 1_000)
        for (events in listOf(listOf(expiredVisit, recentVisit, quest), listOf(quest, recentVisit, expiredVisit))) {
            val signals = resolver.addStructureQuestCooldownSignals(place, events).toMap()
            assertEquals("quest", signals["quest_generation_cooldown_category"])
            assertEquals("25000", signals["quest_generation_cooldown_remaining_ms"])
        }
    }

    @Test
    fun cooldownExpiresAtEachCategoryBoundary() {
        for ((category, duration) in mapOf("quest" to 45_000L, "profession" to 22_000L, "leisure" to 6_000L)) {
            val active = resolver.addStructureQuestCooldownSignals(place, listOf(event("structure_visit_$category", duration - 1))).toMap()
            val expired = resolver.addStructureQuestCooldownSignals(place, listOf(event("structure_visit_$category", duration))).toMap()
            assertEquals("1", active["quest_generation_cooldown_remaining_ms"], category)
            assertFalse(expired.containsKey("quest_generation_cooldown"), category)
        }
    }

    @Test
    fun unrelatedAndFutureEventsDoNotBlockCurrentPlace() {
        val events = listOf(
            event("structure_node_quest", 1_000, "village:other"),
            event("structure_node_quest", -1_000),
            event("quest_completed", 1_000),
        )
        val signals = resolver.addStructureQuestCooldownSignals(place, events).toMap()
        assertFalse(signals.containsKey("quest_generation_cooldown"))
    }

    private fun event(type: String, ageMs: Long, placeId: String = place.id()): StoryEvent = StoryEvent(
        idValue = 1,
        scopeType = "place",
        scopeId = placeId,
        regionId = "village",
        placeId = placeId,
        eventType = type,
        eventKey = type,
        title = "Test event",
        description = "",
        payload = emptyMap(),
        actorType = "player",
        actorId = "player_1",
        playerUuid = null,
        npcId = null,
        createdAtValue = now - ageMs,
    )
}
