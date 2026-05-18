package ro.ainpc.progression

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.engine.ScenarioEngine

class ProgressionSnapshotTest {
    @Test
    fun statusSnapshotPreservesNormalizedSelectorAndMessages() {
        val result = ScenarioEngine.QuestInteractionResult.handled(
            false,
            listOf(),
            listOf("&6=== Progression Status ===", "&eJucator: &fHero")
        )

        val snapshot = ProgressionStatusSnapshot.fromResult("Hero", ProgressionSelector.parse("curent"), result)

        assertTrue(snapshot.handled())
        assertEquals("curent", snapshot.selector())
        assertEquals("tracked", snapshot.normalizedSelector())
        assertEquals(2, snapshot.systemMessages().size)
        assertTrue(snapshot.toQuestInteractionResult().isHandled)
    }

    @Test
    fun statusSnapshotCanCarryStructuredEntryData() {
        val entry = ScenarioEngine.QuestGuiEntry(
            "village_contracts:C01", "medieval:C01", "C01", "Hartie pentru negustor", "Activ", "Secundar", "Contracte de sat",
            true, true, true, false, false, false, "RETURN", "Returnare", 123L, "Negustor",
            listOf(), listOf(), listOf(), listOf(), listOf()
        )
        val definition = ProgressionDefinition(
            "medieval:village_contracts:C01", "medieval", "village_contracts", "contract", "C01", "medieval:C01", "C01",
            "Hartie pentru negustor", "", "side", "fetch", "TRADE_DEAL", "Contracte de sat", "contract", "contracte",
            3, 1, 1, 1, false, true
        )

        val snapshot = ProgressionStatusSnapshot.fromResult(
            "Hero",
            ProgressionSelector.parse("contract:C01"),
            ScenarioEngine.QuestInteractionResult.handled(false, listOf(), listOf("ok")),
            entry,
            definition
        )

        assertEquals("medieval:village_contracts:C01", snapshot.progressionId())
        assertEquals("medieval:C01", snapshot.templateId())
        assertEquals("C01", snapshot.code())
        assertEquals("RETURN", snapshot.currentStageId())
        assertTrue(snapshot.tracked())
    }

    @Test
    fun progressSnapshotCanRepresentNotHandledResult() {
        val snapshot = ProgressionProgressSnapshot.fromResult(
            "Hero",
            ProgressionSelector.parse("missing"),
            ScenarioEngine.QuestInteractionResult.notHandled()
        )

        assertFalse(snapshot.handled())
        assertEquals("missing", snapshot.normalizedSelector())
        assertFalse(snapshot.toQuestInteractionResult().isHandled)
    }

    @Test
    fun progressSnapshotCanCarryObjectiveSnapshots() {
        val objective = ScenarioEngine.QuestGuiObjective(
            "collect_paper", "collect_item", "Aduna hartie", "Aduna hartie pentru negustor", "COLLECT", "Colectare",
            "in_progress", "In progres", 3, 6, false, true
        )
        val entry = ScenarioEngine.QuestGuiEntry(
            "village_contracts:C01", "medieval:C01", "C01", "Hartie pentru negustor", "Activ", "Secundar", "Contracte de sat",
            false, true, true, false, false, false, "COLLECT", "Colectare", 123L, "Negustor",
            listOf(), listOf(objective), listOf(), listOf(), listOf()
        )

        val snapshot = ProgressionProgressSnapshot.fromResult(
            "Hero",
            ProgressionSelector.parse("village_contracts:C01"),
            ScenarioEngine.QuestInteractionResult.handled(false, listOf(), listOf("ok")),
            entry,
            null
        )

        assertEquals("village_contracts:C01", snapshot.progressionId())
        assertEquals(1, snapshot.objectives().size)
        assertEquals("collect_paper", snapshot.objectives()[0].key())
        assertEquals(0, snapshot.completedObjectiveCount())
    }

    @Test
    fun guiSnapshotCanConvertQuestEntriesToProgressionEntries() {
        val objective = ScenarioEngine.QuestGuiObjective(
            "return_goods", "deliver_to_npc", "Returneaza marfa", "Returneaza marfa la negustor", "RETURN", "Returnare",
            "completed", "Completat", 1, 1, true, true
        )
        val stage = ScenarioEngine.QuestGuiStage(
            "RETURN", "Returnare", "Duce obiectele inapoi", "all_objectives", "", true, true, listOf("return_goods")
        )
        val entry = ScenarioEngine.QuestGuiEntry(
            "village_contracts:C01", "medieval:C01", "C01", "Hartie pentru negustor", "Activ", "Secundar", "Contracte de sat",
            true, true, true, false, false, false, "RETURN", "Returnare", 123L, "Negustor",
            listOf("status"), listOf(objective), listOf(stage), listOf("reward"), listOf("action")
        )
        val definition = ProgressionDefinition(
            "medieval:village_contracts:C01", "medieval", "village_contracts", "contract", "C01", "medieval:C01", "C01",
            "Hartie pentru negustor", "", "side", "trade", "TRADE_DEAL", "Contracte de sat", "contract", "contracte",
            3, 1, 1, 1, false, true
        )
        val questSnapshot = ScenarioEngine.QuestGuiSnapshot(
            true, "Hero", "Contracte", listOf("summary"), listOf(entry), listOf(), 0
        )

        val snapshot = ProgressionGuiSnapshot.fromQuestGuiSnapshot(questSnapshot) { definition }

        assertTrue(snapshot.handled())
        assertEquals("Hero", snapshot.playerName())
        assertEquals(1, snapshot.allEntries().size)
        val progressionEntry = snapshot.currentEntries()[0]
        assertEquals("medieval:village_contracts:C01", progressionEntry.progressionId())
        assertEquals("village_contracts", progressionEntry.mechanicId())
        assertEquals("contract", progressionEntry.kind())
        assertEquals("contract", progressionEntry.commandRoot())
        assertEquals("village_contracts:C01", progressionEntry.commandSelector())
        assertEquals("village_contracts:C01", progressionEntry.guiDetailSelector())
        assertEquals("contract", progressionEntry.guiFilter())
        assertEquals("ainpc contract status village_contracts:C01", progressionEntry.command("status"))
        assertEquals("ainpc contract progress village_contracts:C01", progressionEntry.command("progress"))
        assertEquals("ainpc contract debug village_contracts:C01", progressionEntry.command("debug"))
        assertEquals("ainpc contract track start village_contracts:C01", progressionEntry.trackStartCommand())
        assertEquals("ainpc contract track stop", progressionEntry.trackStopCommand())
        assertEquals(listOf("status"), progressionEntry.statusLines())
        assertEquals(listOf("action"), progressionEntry.actionLines())
        assertEquals(1, progressionEntry.objectives().size)
        assertEquals("return_goods", progressionEntry.objectives()[0].key())
        assertEquals(1, progressionEntry.stages().size)
        assertEquals("RETURN", progressionEntry.stages()[0].id())
    }
}
