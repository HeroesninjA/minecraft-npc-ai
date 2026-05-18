package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.progression.ProgressionDefinition
import ro.ainpc.story.StoryContextSnapshot
import ro.ainpc.world.WorldContextSnapshot

class QuestDirectorTest {
    private val director = QuestDirector()

    @Test
    fun emptyStoryContextDoesNotStartQuestSelection() {
        val decision = director.decide(
            QuestDirectorRequest(
                StoryContextSnapshot.empty(),
                listOf(definition("market_investigation", "main_quests", "Market Investigation", true)),
                "",
                true,
                listOf()
            )
        )

        assertEquals(QuestDirectorDecision.Status.NO_ACTION, decision.status())
        assertEquals("story_context_does_not_request_quest", decision.reason())
        assertFalse(decision.runtimeExecutable())
        assertTrue(decision.selectedTemplateId().isBlank())
    }

    @Test
    fun blockingReasonsWinBeforeTemplateSelection() {
        val decision = director.decide(
            QuestDirectorRequest(
                storyContext(listOf("persistent_place_state=market_unrest"), listOf()),
                listOf(definition("market_investigation", "main_quests", "Market Investigation", true)),
                "",
                true,
                listOf("missing quest_board anchor")
            )
        )

        assertEquals(QuestDirectorDecision.Status.BLOCKED, decision.status())
        assertEquals(listOf("missing quest_board anchor"), decision.blockedReasons())
        assertTrue(decision.selectedTemplateId().isBlank())
        assertFalse(decision.runtimeExecutable())
    }

    @Test
    fun storyConditionBlockExplainsMissingCondition() {
        val decision = director.decide(
            QuestDirectorRequest(
                storyContext(listOf("quest_hook=festival_preparation"), listOf()),
                listOf(definition("festival_preparation", "village_events", "Festival Preparation", true)),
                "village_events",
                true,
                listOf("story_condition_missing: region_state=festival_ready")
            )
        )

        assertEquals(QuestDirectorDecision.Status.BLOCKED, decision.status())
        assertEquals("request_blocked", decision.reason())
        assertEquals(listOf("story_condition_missing: region_state=festival_ready"), decision.blockedReasons())
        assertTrue(decision.selectedTemplateId().isBlank())
        assertFalse(decision.runtimeExecutable())
    }

    @Test
    fun storyDemandSelectsMatchingProgressionDefinitionReadOnly() {
        val market = definition("market_investigation", "main_quests", "Market Investigation", true)
        val farm = definition("farm_delivery", "village_contracts", "Farm Delivery", true)

        val decision = director.decide(
            QuestDirectorRequest(
                storyContext(listOf("persistent_place_state=market_unrest", "recent_story_event_type=vandalism"), listOf()),
                listOf(farm, market),
                "",
                false,
                listOf()
            )
        )

        assertEquals(QuestDirectorDecision.Status.CANDIDATE_FOUND, decision.status())
        assertEquals(market.templateId(), decision.selectedTemplateId())
        assertEquals(market.progressionId(), decision.selectedProgressionId())
        assertEquals("main_quests", decision.selectedMechanicId())
        assertTrue(decision.matchedSignals().contains("persistent_place_state=market_unrest"))
        assertTrue(decision.candidateTemplateIds().contains(market.templateId()))
        assertFalse(decision.runtimeExecutable())
    }

    @Test
    fun storyDemandWithPreferredMechanicSelectsReadOnlyCandidate() {
        val contract = definition("market_delivery", "village_contracts", "Market Delivery", true)
        val event = definition("market_alarm_response", "village_events", "Market Alarm Response", true)

        val decision = director.decide(
            QuestDirectorRequest(
                storyContext(listOf("recent_story_event_type=market_alarm"), listOf()),
                listOf(contract, event),
                "village_events",
                true,
                listOf()
            )
        )

        assertEquals(QuestDirectorDecision.Status.CANDIDATE_FOUND, decision.status())
        assertEquals(event.templateId(), decision.selectedTemplateId())
        assertEquals("village_events", decision.selectedMechanicId())
        assertTrue(decision.matchedSignals().contains("preferred_mechanic=village_events"))
        assertTrue(decision.matchedSignals().contains("recent_story_event_type=market_alarm"))
        assertFalse(decision.runtimeExecutable())
    }

    @Test
    fun storyDemandSuggestsSeedWhenNoTemplateMatchesAndSeedIsAllowed() {
        val decision = director.decide(
            QuestDirectorRequest(
                storyContext(listOf("place_danger=bandits"), listOf("mapping warning")),
                listOf(definition("market_investigation", "main_quests", "Market Investigation", true)),
                "",
                true,
                listOf()
            )
        )

        assertEquals(QuestDirectorDecision.Status.SEED_SUGGESTED, decision.status())
        assertEquals("no_matching_template_but_seed_allowed", decision.reason())
        assertEquals(listOf("mapping warning"), decision.warnings())
        assertTrue(decision.matchedSignals().contains("place_danger=bandits"))
        assertFalse(decision.runtimeExecutable())
    }

    @Test
    fun storyDemandBlocksWhenNoTemplateMatchesAndSeedIsNotAllowed() {
        val decision = director.decide(
            QuestDirectorRequest(
                storyContext(listOf("place_conflict=stolen_relic"), listOf()),
                listOf(definition("farm_delivery", "village_contracts", "Farm Delivery", true)),
                "",
                false,
                listOf()
            )
        )

        assertEquals(QuestDirectorDecision.Status.BLOCKED, decision.status())
        assertEquals("no_matching_progression_definition", decision.reason())
        assertEquals(listOf("Nu exista template/progresie potrivita pentru story context."), decision.blockedReasons())
        assertFalse(decision.runtimeExecutable())
    }

    @Test
    fun decisionIsImmutableAndNeverRuntimeExecutable() {
        val decision = QuestDirectorDecision(
            QuestDirectorDecision.Status.CANDIDATE_FOUND,
            " test ",
            " progression ",
            " template ",
            " mechanic ",
            " definition ",
            listOf(" signal "),
            listOf(" candidate "),
            listOf(" blocked "),
            listOf(" warning "),
            true
        )

        assertFalse(decision.runtimeExecutable())
        assertEquals("test", decision.reason())
        assertEquals(listOf("signal"), decision.matchedSignals())
        assertThrows(UnsupportedOperationException::class.java) { (decision.matchedSignals() as MutableList<String>).add("new") }
    }

    private fun storyContext(signals: List<String>, warnings: List<String>): StoryContextSnapshot {
        return StoryContextSnapshot(
            "Mara",
            "merchant",
            "Hero",
            WorldContextSnapshot.empty(),
            null,
            null,
            listOf(),
            listOf(),
            signals,
            warnings
        )
    }

    private fun definition(id: String, mechanicId: String, displayName: String, enabled: Boolean): ProgressionDefinition {
        return ProgressionDefinition(
            "medieval:$mechanicId:$id",
            "medieval",
            mechanicId,
            "quest",
            id,
            "medieval:$id",
            id.uppercase(),
            displayName,
            "$displayName description",
            "side",
            "investigation",
            "QUEST",
            mechanicId,
            "quest",
            "questuri",
            3,
            2,
            1,
            1,
            false,
            enabled
        )
    }
}
