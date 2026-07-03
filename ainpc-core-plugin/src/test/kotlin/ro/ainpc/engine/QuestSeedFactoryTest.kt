package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.progression.ProgressionDefinition
import ro.ainpc.story.StoryContextSnapshot

class QuestSeedFactoryTest {
    @Test
    fun createsStoryDrivenSeedFromDecisionAndProgressionDefinition() {
        val definition = ProgressionDefinition(
            "medieval:village_contracts:C02",
            "medieval",
            "village_contracts",
            "contract",
            "C02",
            "medieval:C02",
            "C02",
            "Avizierul Pietei",
            "Verificare a avizierului.",
            "side",
            "investigation",
            "TRADE_DEAL",
            "Contracte",
            "contract",
            "contracte",
            3,
            2,
            1,
            1,
            repeatableValue = false,
            enabledValue = true
        )

        val decision = QuestDirectorDecision.candidateFound(
            definition,
            listOf("recent_story_event_key=market_unrest", "quest_hook=investigation", "structure_type=market_square"),
            listOf("medieval:C02"),
            listOf("note")
        )

        val seed = QuestSeedFactory().create(
            decision,
            StoryContextSnapshot.empty(),
            definition,
            "demo_sat",
            "demo_sat:market"
        )

        assertEquals("demo_sat", seed.regionId())
        assertEquals("demo_sat:market", seed.placeId())
        assertEquals("village_contracts", seed.mechanicId())
        assertEquals("contract", seed.kind())
        assertEquals("Avizierul Pietei", seed.theme())
        assertEquals("story_driven", seed.storyMode())
        assertTrue(seed.storySignals().contains("recent_story_event_key=market_unrest"))
        assertTrue(seed.storySignals().contains("quest_hook=investigation"))
        assertTrue(seed.storySignals().contains("structure_type=market_square"))
        assertTrue(seed.limits().contains("read_only"))
        assertTrue(seed.limits().contains("candidate_templates=1"))
    }

    @Test
    fun fallsBackToNoActionSeedWhenDecisionHasNoMatch() {
        val decision = QuestDirectorDecision.noAction("no_story", listOf("context"))

        val seed = QuestSeedFactory().create(
            decision,
            StoryContextSnapshot.empty(),
            null,
            "demo_sat",
            ""
        )

        assertEquals("demo_sat", seed.regionId())
        assertEquals("quest", seed.mechanicId())
        assertEquals("quest", seed.kind())
        assertEquals("no_story", seed.storyMode())
        assertTrue(seed.limits().contains("decision_status=no_action"))
    }

    @Test
    fun seedFactoryAdvertisesRuntimeSupportedObjectives() {
        val seed = QuestSeedFactory().create(
            QuestDirectorDecision.noAction("no_story", emptyList()),
            StoryContextSnapshot.empty(),
            null,
            "demo_sat",
            ""
        )

        assertTrue(seed.allowedObjectiveTypes().contains("visit_region"))
        assertTrue(seed.allowedObjectiveTypes().contains("collect_item"))
        assertTrue(seed.allowedObjectiveTypes().contains("kill_mob"))
        assertTrue(seed.allowedObjectiveTypes().contains("place_block"))
        assertTrue(seed.allowedObjectiveTypes().contains("break_block"))
        assertTrue(seed.allowedObjectiveTypes().contains("craft_item"))
    }
}
