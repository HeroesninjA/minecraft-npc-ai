package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.story.StoryContextSnapshot
import ro.ainpc.story.StoryEvent

class QuestScoringCooldownTest {
    private val now = 100_000L
    private val resolver = QuestScoringResolver { now }
    private val baseSignals = listOf(
        "place_id=village:forge",
        "structure_category=profession",
        "quest_generation_cooldown_category=profession",
        "quest_generation_cooldown_ms=22000",
    )

    @Test
    fun irrelevantSummaryDoesNotHideAnActiveProfessionalCooldown() {
        val signals = listOf(
            "place_id=village:forge",
            "structure_category=profession",
            "quest_generation_cooldown=structure_recent",
            "quest_generation_cooldown_place_id=village:forge",
            "quest_generation_cooldown_category=exterior",
            "quest_generation_cooldown_ms=30000",
        )
        val professional = event(10_000)
        val exterior = event(1_000, type = "structure_visit_exterior")
        for (events in listOf(listOf(exterior, professional), listOf(professional, exterior))) {
            assertTrue(resolver.hasQuestGenerationCooldown(context(events = events, signals = signals), emptyList()))
        }
        assertFalse(resolver.hasQuestGenerationCooldown(context(events = listOf(exterior), signals = signals), emptyList()))
    }

    @Test
    fun fallbackUsesTheEventsDurationInsteadOfTheSummaryDuration() {
        val demand = listOf("structure_category=leisure")
        val expiredSocial = context(events = listOf(event(7_000, type = "structure_node_social")))
        val activeSocial = context(events = listOf(event(5_000, type = "structure_node_social")))

        assertFalse(resolver.hasQuestGenerationCooldown(expiredSocial, demand))
        assertTrue(resolver.hasQuestGenerationCooldown(activeSocial, demand))
        assertFalse(resolver.hasQuestGenerationCooldown(activeSocial, emptyList()))
    }

    @Test
    fun recentRelevantEventCanBeCheckedWithoutCooldownSummary() {
        val snapshot = context(
            events = listOf(event(1_000)),
            signals = listOf("place_id=village:forge", "structure_category=profession"),
        )
        assertTrue(resolver.hasQuestGenerationCooldown(snapshot, emptyList()))
    }

    @Test
    fun explicitCooldownOnlyBlocksItsOwnPlace() {
        val marker = listOf("quest_generation_cooldown=structure_recent")
        val ownPlace = context(marker + "quest_generation_cooldown_place_id=village:forge")
        val otherPlace = context(marker + "quest_generation_cooldown_place_id=village:market")

        assertTrue(resolver.hasQuestGenerationCooldown(ownPlace, emptyList()))
        assertFalse(resolver.hasQuestGenerationCooldown(otherPlace, emptyList()))
    }

    @Test
    fun eventFallbackExpiresExactlyAtDurationAndIgnoresFutureTimestamps() {
        assertTrue(resolver.hasQuestGenerationCooldown(context(events = listOf(event(21_999))), emptyList()))
        assertFalse(resolver.hasQuestGenerationCooldown(context(events = listOf(event(22_000))), emptyList()))
        assertFalse(resolver.hasQuestGenerationCooldown(context(events = listOf(event(-1_000))), emptyList()))
    }

    @Test
    fun eventFallbackDoesNotUseAnotherPlacesEvent() {
        val otherPlace = context(events = listOf(event(1_000, "village:market")))
        assertFalse(resolver.hasQuestGenerationCooldown(otherPlace, emptyList()))
    }

    private fun context(
        extraSignals: List<String> = emptyList(),
        events: List<StoryEvent> = emptyList(),
        signals: List<String> = baseSignals,
    ) = StoryContextSnapshot(null, null, null, null, null, null, events, emptyList(), signals + extraSignals, emptyList())

    private fun event(ageMs: Long, placeId: String = "village:forge", type: String = "structure_visit_profession") = StoryEvent(
        idValue = 1,
        scopeType = "place",
        scopeId = placeId,
        regionId = "village",
        placeId = placeId,
        eventType = type,
        eventKey = "forge_order",
        title = "Forge visit",
        description = "",
        payload = emptyMap(),
        actorType = "player",
        actorId = "player_1",
        playerUuid = null,
        npcId = null,
        createdAtValue = now - ageMs,
    )
}
