package ro.ainpc.engine;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestScenarioContractTest {

    @Test
    void infersFetchContractFromCollectObjectives() {
        QuestScenarioContract contract = QuestScenarioContract.fromQuestEntries(
            "",
            "",
            "",
            "",
            List.of("Village Support"),
            List.of(new FeaturePackLoader.QuestEntryDefinition(
                "collect_item",
                "WHEAT",
                8,
                "Adu grau."
            ))
        );

        assertEquals(QuestScenarioContract.Kind.FETCH, contract.kind());
        assertEquals(QuestScenarioContract.Category.SIDE, contract.category());
        assertEquals(QuestScenarioContract.AcceptanceMode.EXPLICIT, contract.acceptanceMode());
        assertEquals(QuestScenarioContract.CompletionMode.RETURN_TO_GIVER, contract.completionMode());
        assertEquals(QuestScenarioContract.TrackingMode.NEXT_OBJECTIVE, contract.trackingMode());
        assertEquals(List.of("village_support"), contract.tags());
        assertFalse(contract.autoAcceptOnOffer());
    }

    @Test
    void explicitMetadataOverridesInference() {
        QuestScenarioContract contract = QuestScenarioContract.fromQuestEntries(
            "main",
            "hunt",
            "auto_accept",
            "manual",
            "quest_giver",
            List.of("Road Safety"),
            List.of(new FeaturePackLoader.QuestEntryDefinition(
                "collect_item",
                "ARROW",
                8,
                "Adu sageti."
            )),
            false
        );

        assertEquals(QuestScenarioContract.Kind.HUNT, contract.kind());
        assertEquals(QuestScenarioContract.Category.MAIN, contract.category());
        assertEquals(QuestScenarioContract.AcceptanceMode.AUTO_ACCEPT, contract.acceptanceMode());
        assertEquals(QuestScenarioContract.CompletionMode.MANUAL, contract.completionMode());
        assertEquals(QuestScenarioContract.TrackingMode.QUEST_GIVER, contract.trackingMode());
        assertTrue(contract.autoAcceptOnOffer());
    }

    @Test
    void supportsInvestigationContractKind() {
        QuestScenarioContract explicitContract = QuestScenarioContract.fromQuestEntries(
            "side",
            "investigation",
            "explicit",
            "return_to_giver",
            "next_objective",
            List.of("mapping", "market"),
            List.of(new FeaturePackLoader.QuestEntryDefinition(
                "visit_place",
                "tag:market",
                1,
                "Mergi in piata."
            )),
            false
        );
        QuestScenarioContract inferredContract = QuestScenarioContract.fromQuestEntries(
            "",
            "",
            "",
            "",
            List.of("mapping"),
            List.of(new FeaturePackLoader.QuestEntryDefinition(
                "inspect_node",
                "quest_board",
                1,
                "Verifica avizierul."
            ))
        );

        assertEquals(QuestScenarioContract.Kind.INVESTIGATION, explicitContract.kind());
        assertEquals("investigatie", explicitContract.displayName());
        assertEquals(QuestScenarioContract.Kind.INVESTIGATION, inferredContract.kind());
    }

    @Test
    void supportsDutyScenarioKind() {
        QuestScenarioContract contract = QuestScenarioContract.fromQuestEntries(
            "repeatable",
            "duty",
            "explicit",
            "return_to_giver",
            "next_objective",
            List.of("guard", "patrol"),
            List.of(new FeaturePackLoader.QuestEntryDefinition(
                "visit_region",
                "type:settlement",
                1,
                "Verifica satul."
            )),
            true
        );

        assertEquals(QuestScenarioContract.Kind.DUTY, contract.kind());
        assertEquals("sarcina", contract.displayName());
        assertEquals(QuestScenarioContract.Category.REPEATABLE, contract.category());
    }

    @Test
    void supportsEventScenarioKind() {
        QuestScenarioContract contract = QuestScenarioContract.fromQuestEntries(
            "repeatable",
            "event",
            "explicit",
            "return_to_giver",
            "next_objective",
            List.of("market", "temporary"),
            List.of(new FeaturePackLoader.QuestEntryDefinition(
                "inspect_node",
                "quest_board",
                1,
                "Verifica avizierul."
            )),
            true
        );

        assertEquals(QuestScenarioContract.Kind.EVENT, contract.kind());
        assertEquals("eveniment", contract.displayName());
        assertEquals(QuestScenarioContract.Category.REPEATABLE, contract.category());
    }

    @Test
    void supportsTutorialScenarioKind() {
        QuestScenarioContract contract = QuestScenarioContract.fromQuestEntries(
            "side",
            "tutorial",
            "explicit",
            "return_to_giver",
            "next_objective",
            List.of("onboarding", "market"),
            List.of(new FeaturePackLoader.QuestEntryDefinition(
                "inspect_node",
                "quest_board",
                1,
                "Invata avizierul."
            )),
            false
        );

        assertEquals(QuestScenarioContract.Kind.TUTORIAL, contract.kind());
        assertEquals("tutorial", contract.displayName());
        assertEquals(QuestScenarioContract.Category.SIDE, contract.category());
    }

    @Test
    void supportsRitualScenarioKind() {
        QuestScenarioContract contract = QuestScenarioContract.fromQuestEntries(
            "repeatable",
            "ritual",
            "explicit",
            "return_to_giver",
            "next_objective",
            List.of("ritual", "altar"),
            List.of(new FeaturePackLoader.QuestEntryDefinition(
                "inspect_node",
                "ritual_circle",
                1,
                "Verifica cercul ritualic."
            )),
            true
        );

        assertEquals(QuestScenarioContract.Kind.RITUAL, contract.kind());
        assertEquals("ritual", contract.displayName());
        assertEquals(QuestScenarioContract.Category.REPEATABLE, contract.category());
    }

    @Test
    void questEntryDefinitionCarriesActionMetadata() {
        FeaturePackLoader.QuestEntryDefinition entry = new FeaturePackLoader.QuestEntryDefinition(
            "record_story_event",
            "story_event",
            1,
            "",
            Map.of("scope", "region", "event_type", "quest_completed"),
            Map.of("quest", "Q01"),
            Map.of("outcome", "forge_supplied")
        );

        assertEquals("region", entry.getMetadata().get("scope"));
        assertEquals("quest_completed", entry.getMetadata().get("event_type"));
        assertEquals("Q01", entry.getVariables().get("quest"));
        assertEquals("forge_supplied", entry.getPayload().get("outcome"));
    }
}
