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

    @Test
    void preservesInvestigationScenarioKindForContracts() {
        FeaturePackLoader.ScenarioDefinition scenario = new FeaturePackLoader.ScenarioDefinition(
            "medieval",
            "C02",
            "Avizierul Pietei",
            "Contract de verificare",
            ScenarioEngine.ScenarioType.TRADE_DEAL
        );
        scenario.setQuestCode("C02");
        scenario.setQuestCategory("side");
        scenario.setQuestScenarioKind("investigation");
        scenario.setProgressionEnabled(true);
        scenario.setProgressionMechanicId("village_contracts");
        scenario.setProgressionKind("contract");
        scenario.addObjective(new FeaturePackLoader.QuestEntryDefinition(
            "inspect_node", "quest_board", 1, "Verifica avizierul."
        ));

        ProgressionDefinition definition = ProgressionDefinition.fromScenarioDefinition(scenario);

        assertEquals("contract", definition.kind());
        assertEquals("investigation", definition.scenarioKind());
    }

    @Test
    void preservesDutyProgressionMetadata() {
        FeaturePackLoader.ScenarioDefinition scenario = new FeaturePackLoader.ScenarioDefinition(
            "medieval",
            "D01",
            "Rondul Strajerului",
            "Sarcina de patrula",
            ScenarioEngine.ScenarioType.DUTY
        );
        scenario.setQuestCode("D01");
        scenario.setQuestCategory("repeatable");
        scenario.setQuestScenarioKind("duty");
        scenario.setQuestRepeatable(true);
        scenario.setProgressionEnabled(true);
        scenario.setProgressionMechanicId("npc_duties");
        scenario.setProgressionKind("duty");
        scenario.addObjective(new FeaturePackLoader.QuestEntryDefinition(
            "visit_region", "type:settlement", 1, "Confirma rondul."
        ));

        ProgressionDefinition definition = ProgressionDefinition.fromScenarioDefinition(scenario);

        assertEquals("medieval:npc_duties:D01", definition.progressionId());
        assertEquals("duty", definition.kind());
        assertEquals("duty", definition.scenarioKind());
        assertEquals("DUTY", definition.baseType());
        assertEquals("repeatable", definition.category());
        assertTrue(definition.repeatable());
    }

    @Test
    void preservesBountyProgressionMetadata() {
        FeaturePackLoader.ScenarioDefinition scenario = new FeaturePackLoader.ScenarioDefinition(
            "medieval",
            "B01",
            "Recompensa Drumului Vechi",
            "Bounty local",
            ScenarioEngine.ScenarioType.BOUNTY
        );
        scenario.setQuestCode("B01");
        scenario.setQuestCategory("repeatable");
        scenario.setQuestScenarioKind("hunt");
        scenario.setQuestRepeatable(true);
        scenario.setProgressionEnabled(true);
        scenario.setProgressionMechanicId("local_bounties");
        scenario.setProgressionKind("bounty");
        scenario.addObjective(new FeaturePackLoader.QuestEntryDefinition(
            "kill_mob", "SKELETON", 2, "Curata drumul."
        ));

        ProgressionDefinition definition = ProgressionDefinition.fromScenarioDefinition(scenario);

        assertEquals("medieval:local_bounties:B01", definition.progressionId());
        assertEquals("bounty", definition.kind());
        assertEquals("hunt", definition.scenarioKind());
        assertEquals("BOUNTY", definition.baseType());
        assertEquals("repeatable", definition.category());
        assertTrue(definition.repeatable());
    }

    @Test
    void preservesEventProgressionMetadata() {
        FeaturePackLoader.ScenarioDefinition scenario = new FeaturePackLoader.ScenarioDefinition(
            "medieval",
            "E01",
            "Alarma Fantanii din Piata",
            "Eveniment local",
            ScenarioEngine.ScenarioType.WORLD_EVENT
        );
        scenario.setQuestCode("E01");
        scenario.setQuestCategory("repeatable");
        scenario.setQuestScenarioKind("event");
        scenario.setQuestRepeatable(true);
        scenario.setProgressionEnabled(true);
        scenario.setProgressionMechanicId("village_events");
        scenario.setProgressionKind("event");
        scenario.addObjective(new FeaturePackLoader.QuestEntryDefinition(
            "inspect_node", "quest_board", 1, "Verifica avizierul."
        ));

        ProgressionDefinition definition = ProgressionDefinition.fromScenarioDefinition(scenario);

        assertEquals("medieval:village_events:E01", definition.progressionId());
        assertEquals("event", definition.kind());
        assertEquals("event", definition.scenarioKind());
        assertEquals("WORLD_EVENT", definition.baseType());
        assertEquals("repeatable", definition.category());
        assertTrue(definition.repeatable());
    }

    @Test
    void preservesTutorialProgressionMetadata() {
        FeaturePackLoader.ScenarioDefinition scenario = new FeaturePackLoader.ScenarioDefinition(
            "medieval",
            "T01",
            "Indrumarea Avizierului",
            "Tutorial de onboarding",
            ScenarioEngine.ScenarioType.TUTORIAL
        );
        scenario.setQuestCode("T01");
        scenario.setQuestCategory("side");
        scenario.setQuestScenarioKind("tutorial");
        scenario.setProgressionEnabled(true);
        scenario.setProgressionMechanicId("onboarding");
        scenario.setProgressionKind("tutorial");
        scenario.addObjective(new FeaturePackLoader.QuestEntryDefinition(
            "inspect_node", "quest_board", 1, "Inspecteaza avizierul."
        ));

        ProgressionDefinition definition = ProgressionDefinition.fromScenarioDefinition(scenario);

        assertEquals("medieval:onboarding:T01", definition.progressionId());
        assertEquals("tutorial", definition.kind());
        assertEquals("tutorial", definition.scenarioKind());
        assertEquals("TUTORIAL", definition.baseType());
        assertEquals("side", definition.category());
    }

    @Test
    void preservesRitualProgressionMetadata() {
        FeaturePackLoader.ScenarioDefinition scenario = new FeaturePackLoader.ScenarioDefinition(
            "medieval",
            "R01",
            "Luminile Vechiului Altar",
            "Ritual local",
            ScenarioEngine.ScenarioType.RITUAL
        );
        scenario.setQuestCode("R01");
        scenario.setQuestCategory("repeatable");
        scenario.setQuestScenarioKind("ritual");
        scenario.setQuestRepeatable(true);
        scenario.setProgressionEnabled(true);
        scenario.setProgressionMechanicId("village_rituals");
        scenario.setProgressionKind("ritual");
        scenario.addObjective(new FeaturePackLoader.QuestEntryDefinition(
            "inspect_node", "ritual_circle", 1, "Inspecteaza cercul ritualic."
        ));

        ProgressionDefinition definition = ProgressionDefinition.fromScenarioDefinition(scenario);

        assertEquals("medieval:village_rituals:R01", definition.progressionId());
        assertEquals("ritual", definition.kind());
        assertEquals("ritual", definition.scenarioKind());
        assertEquals("RITUAL", definition.baseType());
        assertEquals("repeatable", definition.category());
        assertTrue(definition.repeatable());
    }
}
