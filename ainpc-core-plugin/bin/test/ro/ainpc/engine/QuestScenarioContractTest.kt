package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class QuestScenarioContractTest {
    @Test
    fun infersFetchContractFromCollectObjectives() {
        val contract = QuestScenarioContract.fromQuestEntries(
            "", "", "", "", listOf("Village Support"),
            listOf(FeaturePackLoader.QuestEntryDefinition("collect_item", "WHEAT", 8, "Adu grau."))
        )

        assertEquals(QuestScenarioContract.Kind.FETCH, contract.kind())
        assertEquals(QuestScenarioContract.Category.SIDE, contract.category())
        assertEquals(QuestScenarioContract.AcceptanceMode.EXPLICIT, contract.acceptanceMode())
        assertEquals(QuestScenarioContract.CompletionMode.RETURN_TO_GIVER, contract.completionMode())
        assertEquals(QuestScenarioContract.TrackingMode.NEXT_OBJECTIVE, contract.trackingMode())
        assertEquals(listOf("village_support"), contract.tags())
        assertFalse(contract.autoAcceptOnOffer())
    }

    @Test
    fun explicitMetadataOverridesInference() {
        val contract = QuestScenarioContract.fromQuestEntries(
            "main", "hunt", "auto_accept", "manual", "quest_giver",
            listOf("Road Safety"),
            listOf(FeaturePackLoader.QuestEntryDefinition("collect_item", "ARROW", 8, "Adu sageti.")),
            false
        )

        assertEquals(QuestScenarioContract.Kind.HUNT, contract.kind())
        assertEquals(QuestScenarioContract.Category.MAIN, contract.category())
        assertEquals(QuestScenarioContract.AcceptanceMode.AUTO_ACCEPT, contract.acceptanceMode())
        assertEquals(QuestScenarioContract.CompletionMode.MANUAL, contract.completionMode())
        assertEquals(QuestScenarioContract.TrackingMode.QUEST_GIVER, contract.trackingMode())
        assertTrue(contract.autoAcceptOnOffer())
    }

    @Test
    fun supportsInvestigationContractKind() {
        val explicitContract = QuestScenarioContract.fromQuestEntries(
            "side", "investigation", "explicit", "return_to_giver", "next_objective",
            listOf("mapping", "market"),
            listOf(FeaturePackLoader.QuestEntryDefinition("visit_place", "tag:market", 1, "Mergi in piata.")),
            false
        )
        val inferredContract = QuestScenarioContract.fromQuestEntries(
            "", "", "", "", listOf("mapping"),
            listOf(FeaturePackLoader.QuestEntryDefinition("inspect_node", "quest_board", 1, "Verifica avizierul."))
        )

        assertEquals(QuestScenarioContract.Kind.INVESTIGATION, explicitContract.kind())
        assertEquals("investigatie", explicitContract.displayName())
        assertEquals(QuestScenarioContract.Kind.INVESTIGATION, inferredContract.kind())
    }

    @Test
    fun supportsDutyScenarioKind() {
        val contract = QuestScenarioContract.fromQuestEntries(
            "repeatable", "duty", "explicit", "return_to_giver", "next_objective",
            listOf("guard", "patrol"),
            listOf(FeaturePackLoader.QuestEntryDefinition("visit_region", "type:settlement", 1, "Verifica satul.")),
            true
        )

        assertEquals(QuestScenarioContract.Kind.DUTY, contract.kind())
        assertEquals("sarcina", contract.displayName())
        assertEquals(QuestScenarioContract.Category.REPEATABLE, contract.category())
    }

    @Test
    fun supportsEventScenarioKind() {
        val contract = QuestScenarioContract.fromQuestEntries(
            "repeatable", "event", "explicit", "return_to_giver", "next_objective",
            listOf("market", "temporary"),
            listOf(FeaturePackLoader.QuestEntryDefinition("inspect_node", "quest_board", 1, "Verifica avizierul.")),
            true
        )

        assertEquals(QuestScenarioContract.Kind.EVENT, contract.kind())
        assertEquals("eveniment", contract.displayName())
        assertEquals(QuestScenarioContract.Category.REPEATABLE, contract.category())
    }

    @Test
    fun supportsTutorialScenarioKind() {
        val contract = QuestScenarioContract.fromQuestEntries(
            "side", "tutorial", "explicit", "return_to_giver", "next_objective",
            listOf("onboarding", "market"),
            listOf(FeaturePackLoader.QuestEntryDefinition("inspect_node", "quest_board", 1, "Invata avizierul.")),
            false
        )

        assertEquals(QuestScenarioContract.Kind.TUTORIAL, contract.kind())
        assertEquals("tutorial", contract.displayName())
        assertEquals(QuestScenarioContract.Category.SIDE, contract.category())
    }

    @Test
    fun supportsRitualScenarioKind() {
        val contract = QuestScenarioContract.fromQuestEntries(
            "repeatable", "ritual", "explicit", "return_to_giver", "next_objective",
            listOf("ritual", "altar"),
            listOf(FeaturePackLoader.QuestEntryDefinition("inspect_node", "ritual_circle", 1, "Verifica cercul ritualic.")),
            true
        )

        assertEquals(QuestScenarioContract.Kind.RITUAL, contract.kind())
        assertEquals("ritual", contract.displayName())
        assertEquals(QuestScenarioContract.Category.REPEATABLE, contract.category())
    }

    @Test
    fun questEntryDefinitionCarriesActionMetadata() {
        val entry = FeaturePackLoader.QuestEntryDefinition(
            "record_story_event",
            "story_event",
            1,
            "",
            mapOf("scope" to "region", "event_type" to "quest_completed"),
            mapOf("quest" to "Q01"),
            mapOf("outcome" to "forge_supplied")
        )

        assertEquals("region", entry.metadata["scope"])
        assertEquals("quest_completed", entry.metadata["event_type"])
        assertEquals("Q01", entry.variables["quest"])
        assertEquals("forge_supplied", entry.payload["outcome"])
    }
}
