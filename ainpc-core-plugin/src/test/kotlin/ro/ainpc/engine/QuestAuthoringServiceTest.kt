package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.progression.ProgressionDefinition
import ro.ainpc.story.StoryContextSnapshot

class QuestAuthoringServiceTest {
    @Test
    fun buildsAuthoringSnapshotForMatchingProgression() {
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
        val storyContext = StoryContextSnapshot(
            "",
            "",
            "Alex",
            null,
            null,
            null,
            emptyList(),
            emptyList(),
            listOf("quest_hook=investigation", "recent_story_event_key=market_unrest"),
            listOf("story warning")
        )

        val snapshot = QuestAuthoringService().analyze(
            storyContext,
            listOf(definition),
            "medieval:C02",
            "village_contracts",
            "demo_sat",
            "demo_sat:market",
            true,
            emptyList()
        )

        assertTrue(snapshot.handled)
        assertEquals("Alex", snapshot.playerName)
        assertEquals("medieval:C02", snapshot.requestedQuestSelector)
        assertEquals("village_contracts", snapshot.requestedMechanicId)
        assertEquals("candidate_found", snapshot.decisionStatus())
        assertEquals("village_contracts", snapshot.seedMechanicId())
        assertEquals("contract", snapshot.seedKind())
        assertEquals("story_driven", snapshot.seedStoryMode())
        assertTrue(snapshot.summaryLines.any { it.contains("requested_selector=medieval:C02") })
        assertTrue(snapshot.summaryLines.any { it.contains("requested_mechanic=village_contracts") })
        assertTrue(snapshot.summaryLines.any { it.contains("selected_selector=medieval:village_contracts:C02") })
        assertTrue(snapshot.summaryLines.any { it.contains("selected_template=medieval:C02") })
        assertTrue(snapshot.summaryLines.any { it.contains("seed_region=demo_sat") })
        assertTrue(snapshot.warnings.contains("story warning"))
        assertTrue(snapshot.warnings.contains("read_only"))
    }

    @Test
    fun keepsBlockedDecisionAndNoStorySeed() {
        val snapshot = QuestAuthoringService().analyze(
            StoryContextSnapshot.empty(),
            emptyList(),
            "",
            "",
            "demo_sat",
            "",
            false,
            listOf("mapping blocked")
        )

        assertEquals("blocked", snapshot.decisionStatus())
        assertEquals("no_story", snapshot.seedStoryMode())
        assertTrue(snapshot.warnings.contains("mapping blocked"))
        assertTrue(snapshot.warnings.contains("decision_status=blocked"))
    }
}
