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
