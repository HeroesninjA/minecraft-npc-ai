package ro.ainpc.progression;

import org.junit.jupiter.api.Test;
import ro.ainpc.engine.FeaturePackLoader;
import ro.ainpc.engine.ScenarioEngine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressionDefinitionTest {

    @Test
    void buildsGenericDefinitionFromScenarioDefinition() {
        FeaturePackLoader.ScenarioDefinition scenario = new FeaturePackLoader.ScenarioDefinition(
            "medieval",
            "C01",
            "Hartie pentru negustor",
            "Contract de livrare",
            ScenarioEngine.ScenarioType.TRADE_DEAL
        );
        scenario.setQuestCode("C01");
        scenario.setQuestCategory("side");
        scenario.setProgressionEnabled(true);
        scenario.setProgressionMechanicId("village_contracts");
        scenario.setProgressionKind("contract");
        scenario.setProgressionLabel("Contracte de sat");
        scenario.setProgressionSingularLabel("contract");
        scenario.setProgressionPluralLabel("contracte");
        scenario.setProgressionMaxActive(3);
        scenario.addObjective(new FeaturePackLoader.QuestEntryDefinition(
            "collect_item", "PAPER", 6, "Aduna hartie"
        ));

        ProgressionDefinition definition = ProgressionDefinition.fromScenarioDefinition(scenario);

        assertEquals("medieval:village_contracts:C01", definition.progressionId());
        assertEquals("medieval:C01", definition.templateId());
        assertEquals("village_contracts", definition.mechanicId());
        assertEquals("contract", definition.kind());
        assertEquals(1, definition.objectiveCount());
        assertEquals(3, definition.maxActive());
        assertTrue(definition.enabled());
    }
}
