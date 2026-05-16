package ro.ainpc.progression;

import org.junit.jupiter.api.Test;
import ro.ainpc.engine.ScenarioEngine;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressionSnapshotTest {

    @Test
    void statusSnapshotPreservesNormalizedSelectorAndMessages() {
        ScenarioEngine.QuestInteractionResult result = ScenarioEngine.QuestInteractionResult.handled(
            false,
            List.of(),
            List.of("&6=== Progression Status ===", "&eJucator: &fHero")
        );

        ProgressionStatusSnapshot snapshot = ProgressionStatusSnapshot.fromResult(
            "Hero",
            ProgressionSelector.parse("curent"),
            result
        );

        assertTrue(snapshot.handled());
        assertEquals("curent", snapshot.selector());
        assertEquals("tracked", snapshot.normalizedSelector());
        assertEquals(2, snapshot.systemMessages().size());
        assertTrue(snapshot.toQuestInteractionResult().isHandled());
    }

    @Test
    void statusSnapshotCanCarryStructuredEntryData() {
        ScenarioEngine.QuestGuiEntry entry = new ScenarioEngine.QuestGuiEntry(
            "village_contracts:C01",
            "medieval:C01",
            "C01",
            "Hartie pentru negustor",
            "Activ",
            "Secundar",
            "Contracte de sat",
            true,
            true,
            true,
            false,
            false,
            false,
            "RETURN",
            "Returnare",
            123L,
            "Negustor",
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of()
        );
        ProgressionDefinition definition = new ProgressionDefinition(
            "medieval:village_contracts:C01",
            "medieval",
            "village_contracts",
            "contract",
            "C01",
            "medieval:C01",
            "C01",
            "Hartie pentru negustor",
            "",
            "side",
            "fetch",
            "TRADE_DEAL",
            "Contracte de sat",
            "contract",
            "contracte",
            3,
            1,
            1,
            1,
            false,
            true
        );

        ProgressionStatusSnapshot snapshot = ProgressionStatusSnapshot.fromResult(
            "Hero",
            ProgressionSelector.parse("contract:C01"),
            ScenarioEngine.QuestInteractionResult.handled(false, List.of(), List.of("ok")),
            entry,
            definition
        );

        assertEquals("medieval:village_contracts:C01", snapshot.progressionId());
        assertEquals("medieval:C01", snapshot.templateId());
        assertEquals("C01", snapshot.code());
        assertEquals("RETURN", snapshot.currentStageId());
        assertTrue(snapshot.tracked());
    }

    @Test
    void progressSnapshotCanRepresentNotHandledResult() {
        ProgressionProgressSnapshot snapshot = ProgressionProgressSnapshot.fromResult(
            "Hero",
            ProgressionSelector.parse("missing"),
            ScenarioEngine.QuestInteractionResult.notHandled()
        );

        assertFalse(snapshot.handled());
        assertEquals("missing", snapshot.normalizedSelector());
        assertFalse(snapshot.toQuestInteractionResult().isHandled());
    }

    @Test
    void progressSnapshotCanCarryObjectiveSnapshots() {
        ScenarioEngine.QuestGuiObjective objective = new ScenarioEngine.QuestGuiObjective(
            "collect_paper",
            "collect_item",
            "Aduna hartie",
            "Aduna hartie pentru negustor",
            "COLLECT",
            "Colectare",
            "in_progress",
            "In progres",
            3,
            6,
            false,
            true
        );
        ScenarioEngine.QuestGuiEntry entry = new ScenarioEngine.QuestGuiEntry(
            "village_contracts:C01",
            "medieval:C01",
            "C01",
            "Hartie pentru negustor",
            "Activ",
            "Secundar",
            "Contracte de sat",
            false,
            true,
            true,
            false,
            false,
            false,
            "COLLECT",
            "Colectare",
            123L,
            "Negustor",
            List.of(),
            List.of(objective),
            List.of(),
            List.of(),
            List.of()
        );

        ProgressionProgressSnapshot snapshot = ProgressionProgressSnapshot.fromResult(
            "Hero",
            ProgressionSelector.parse("village_contracts:C01"),
            ScenarioEngine.QuestInteractionResult.handled(false, List.of(), List.of("ok")),
            entry,
            null
        );

        assertEquals("village_contracts:C01", snapshot.progressionId());
        assertEquals(1, snapshot.objectives().size());
        assertEquals("collect_paper", snapshot.objectives().get(0).key());
        assertEquals(0, snapshot.completedObjectiveCount());
    }

    @Test
    void guiSnapshotCanConvertQuestEntriesToProgressionEntries() {
        ScenarioEngine.QuestGuiObjective objective = new ScenarioEngine.QuestGuiObjective(
            "return_goods",
            "deliver_to_npc",
            "Returneaza marfa",
            "Returneaza marfa la negustor",
            "RETURN",
            "Returnare",
            "completed",
            "Completat",
            1,
            1,
            true,
            true
        );
        ScenarioEngine.QuestGuiStage stage = new ScenarioEngine.QuestGuiStage(
            "RETURN",
            "Returnare",
            "Duce obiectele inapoi",
            "all_objectives",
            "",
            true,
            true,
            List.of("return_goods")
        );
        ScenarioEngine.QuestGuiEntry entry = new ScenarioEngine.QuestGuiEntry(
            "village_contracts:C01",
            "medieval:C01",
            "C01",
            "Hartie pentru negustor",
            "Activ",
            "Secundar",
            "Contracte de sat",
            true,
            true,
            true,
            false,
            false,
            false,
            "RETURN",
            "Returnare",
            123L,
            "Negustor",
            List.of("status"),
            List.of(objective),
            List.of(stage),
            List.of("reward"),
            List.of("action")
        );
        ProgressionDefinition definition = new ProgressionDefinition(
            "medieval:village_contracts:C01",
            "medieval",
            "village_contracts",
            "contract",
            "C01",
            "medieval:C01",
            "C01",
            "Hartie pentru negustor",
            "",
            "side",
            "trade",
            "TRADE_DEAL",
            "Contracte de sat",
            "contract",
            "contracte",
            3,
            1,
            1,
            1,
            false,
            true
        );
        ScenarioEngine.QuestGuiSnapshot questSnapshot = new ScenarioEngine.QuestGuiSnapshot(
            true,
            "Hero",
            "Contracte",
            List.of("summary"),
            List.of(entry),
            List.of(),
            0
        );

        ProgressionGuiSnapshot snapshot = ProgressionGuiSnapshot.fromQuestGuiSnapshot(
            questSnapshot,
            ignored -> definition
        );

        assertTrue(snapshot.handled());
        assertEquals("Hero", snapshot.playerName());
        assertEquals(1, snapshot.allEntries().size());
        ProgressionGuiEntry progressionEntry = snapshot.currentEntries().get(0);
        assertEquals("medieval:village_contracts:C01", progressionEntry.progressionId());
        assertEquals("village_contracts", progressionEntry.mechanicId());
        assertEquals("contract", progressionEntry.kind());
        assertEquals("contract", progressionEntry.commandRoot());
        assertEquals("village_contracts:C01", progressionEntry.commandSelector());
        assertEquals("village_contracts:C01", progressionEntry.guiDetailSelector());
        assertEquals("contract", progressionEntry.guiFilter());
        assertEquals("ainpc contract status village_contracts:C01", progressionEntry.command("status"));
        assertEquals("ainpc contract progress village_contracts:C01", progressionEntry.command("progress"));
        assertEquals("ainpc contract debug village_contracts:C01", progressionEntry.command("debug"));
        assertEquals("ainpc contract track start village_contracts:C01", progressionEntry.trackStartCommand());
        assertEquals("ainpc contract track stop", progressionEntry.trackStopCommand());
        assertEquals(List.of("status"), progressionEntry.statusLines());
        assertEquals(List.of("action"), progressionEntry.actionLines());
        assertEquals(1, progressionEntry.objectives().size());
        assertEquals("return_goods", progressionEntry.objectives().get(0).key());
        assertEquals(1, progressionEntry.stages().size());
        assertEquals("RETURN", progressionEntry.stages().get(0).id());
    }
}
