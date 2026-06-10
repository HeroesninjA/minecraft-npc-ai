package ro.ainpc.progression

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.engine.FeaturePackLoader
import ro.ainpc.engine.ScenarioEngine

class ProgressionDefinitionTest {
    @Test
    fun buildsGenericDefinitionFromScenarioDefinition() {
        val scenario = FeaturePackLoader.ScenarioDefinition(
            "medieval",
            "C01",
            "Hartie pentru negustor",
            "Contract de livrare",
            ScenarioEngine.ScenarioType.TRADE_DEAL
        )
        scenario.questCode = "C01"
        scenario.questCategory = "side"
        scenario.isProgressionEnabled = true
        scenario.progressionMechanicId = "village_contracts"
        scenario.progressionKind = "contract"
        scenario.progressionLabel = "Contracte de sat"
        scenario.progressionSingularLabel = "contract"
        scenario.progressionPluralLabel = "contracte"
        scenario.progressionMaxActive = 3
        scenario.addObjective(FeaturePackLoader.QuestEntryDefinition("collect_item", "PAPER", 6, "Aduna hartie"))

        val definition = ProgressionDefinition.fromScenarioDefinition(scenario)

        assertEquals("medieval:village_contracts:C01", definition.progressionId())
        assertEquals("medieval:C01", definition.templateId())
        assertEquals("village_contracts", definition.mechanicId())
        assertEquals("contract", definition.kind())
        assertEquals(1, definition.objectiveCount())
        assertEquals(3, definition.maxActive())
        assertTrue(definition.enabled())
    }

    @Test
    fun preservesInvestigationScenarioKindForContracts() {
        val scenario = FeaturePackLoader.ScenarioDefinition(
            "medieval",
            "C02",
            "Avizierul Pietei",
            "Contract de verificare",
            ScenarioEngine.ScenarioType.TRADE_DEAL
        )
        scenario.questCode = "C02"
        scenario.questCategory = "side"
        scenario.questScenarioKind = "investigation"
        scenario.isProgressionEnabled = true
        scenario.progressionMechanicId = "village_contracts"
        scenario.progressionKind = "contract"
        scenario.addObjective(FeaturePackLoader.QuestEntryDefinition("inspect_node", "quest_board", 1, "Verifica avizierul."))

        val definition = ProgressionDefinition.fromScenarioDefinition(scenario)

        assertEquals("contract", definition.kind())
        assertEquals("investigation", definition.scenarioKind())
    }

    @Test
    fun preservesDutyProgressionMetadata() {
        val scenario = FeaturePackLoader.ScenarioDefinition(
            "medieval",
            "D01",
            "Rondul Strajerului",
            "Sarcina de patrula",
            ScenarioEngine.ScenarioType.DUTY
        )
        scenario.questCode = "D01"
        scenario.questCategory = "repeatable"
        scenario.questScenarioKind = "duty"
        scenario.isQuestRepeatable = true
        scenario.isProgressionEnabled = true
        scenario.progressionMechanicId = "npc_duties"
        scenario.progressionKind = "duty"
        scenario.addObjective(FeaturePackLoader.QuestEntryDefinition("visit_region", "type:settlement", 1, "Confirma rondul."))

        val definition = ProgressionDefinition.fromScenarioDefinition(scenario)

        assertEquals("medieval:npc_duties:D01", definition.progressionId())
        assertEquals("duty", definition.kind())
        assertEquals("duty", definition.scenarioKind())
        assertEquals("DUTY", definition.baseType())
        assertEquals("repeatable", definition.category())
        assertTrue(definition.repeatable())
    }

    @Test
    fun preservesBountyProgressionMetadata() {
        val scenario = FeaturePackLoader.ScenarioDefinition(
            "medieval",
            "B01",
            "Recompensa Drumului Vechi",
            "Bounty local",
            ScenarioEngine.ScenarioType.BOUNTY
        )
        scenario.questCode = "B01"
        scenario.questCategory = "repeatable"
        scenario.questScenarioKind = "hunt"
        scenario.isQuestRepeatable = true
        scenario.isProgressionEnabled = true
        scenario.progressionMechanicId = "local_bounties"
        scenario.progressionKind = "bounty"
        scenario.addObjective(FeaturePackLoader.QuestEntryDefinition("kill_mob", "SKELETON", 2, "Curata drumul."))

        val definition = ProgressionDefinition.fromScenarioDefinition(scenario)

        assertEquals("medieval:local_bounties:B01", definition.progressionId())
        assertEquals("bounty", definition.kind())
        assertEquals("hunt", definition.scenarioKind())
        assertEquals("BOUNTY", definition.baseType())
        assertEquals("repeatable", definition.category())
        assertTrue(definition.repeatable())
    }

    @Test
    fun preservesEventProgressionMetadata() {
        val scenario = FeaturePackLoader.ScenarioDefinition(
            "medieval",
            "E01",
            "Alarma Fantanii din Piata",
            "Eveniment local",
            ScenarioEngine.ScenarioType.WORLD_EVENT
        )
        scenario.questCode = "E01"
        scenario.questCategory = "repeatable"
        scenario.questScenarioKind = "event"
        scenario.isQuestRepeatable = true
        scenario.isProgressionEnabled = true
        scenario.progressionMechanicId = "village_events"
        scenario.progressionKind = "event"
        scenario.addObjective(FeaturePackLoader.QuestEntryDefinition("inspect_node", "quest_board", 1, "Verifica avizierul."))

        val definition = ProgressionDefinition.fromScenarioDefinition(scenario)

        assertEquals("medieval:village_events:E01", definition.progressionId())
        assertEquals("event", definition.kind())
        assertEquals("event", definition.scenarioKind())
        assertEquals("WORLD_EVENT", definition.baseType())
        assertEquals("repeatable", definition.category())
        assertTrue(definition.repeatable())
    }

    @Test
    fun preservesTutorialProgressionMetadata() {
        val scenario = FeaturePackLoader.ScenarioDefinition(
            "medieval",
            "T01",
            "Indrumarea Avizierului",
            "Tutorial de onboarding",
            ScenarioEngine.ScenarioType.TUTORIAL
        )
        scenario.questCode = "T01"
        scenario.questCategory = "side"
        scenario.questScenarioKind = "tutorial"
        scenario.isProgressionEnabled = true
        scenario.progressionMechanicId = "onboarding"
        scenario.progressionKind = "tutorial"
        scenario.addObjective(FeaturePackLoader.QuestEntryDefinition("inspect_node", "quest_board", 1, "Inspecteaza avizierul."))

        val definition = ProgressionDefinition.fromScenarioDefinition(scenario)

        assertEquals("medieval:onboarding:T01", definition.progressionId())
        assertEquals("tutorial", definition.kind())
        assertEquals("tutorial", definition.scenarioKind())
        assertEquals("TUTORIAL", definition.baseType())
        assertEquals("side", definition.category())
    }

    @Test
    fun preservesRitualProgressionMetadata() {
        val scenario = FeaturePackLoader.ScenarioDefinition(
            "medieval",
            "R01",
            "Luminile Vechiului Altar",
            "Ritual local",
            ScenarioEngine.ScenarioType.RITUAL
        )
        scenario.questCode = "R01"
        scenario.questCategory = "repeatable"
        scenario.questScenarioKind = "ritual"
        scenario.isQuestRepeatable = true
        scenario.isProgressionEnabled = true
        scenario.progressionMechanicId = "village_rituals"
        scenario.progressionKind = "ritual"
        scenario.addObjective(FeaturePackLoader.QuestEntryDefinition("inspect_node", "ritual_circle", 1, "Inspecteaza cercul ritualic."))

        val definition = ProgressionDefinition.fromScenarioDefinition(scenario)

        assertEquals("medieval:village_rituals:R01", definition.progressionId())
        assertEquals("ritual", definition.kind())
        assertEquals("ritual", definition.scenarioKind())
        assertEquals("RITUAL", definition.baseType())
        assertEquals("repeatable", definition.category())
        assertTrue(definition.repeatable())
    }
}
