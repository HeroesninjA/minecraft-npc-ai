package ro.ainpc.engine

import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class QuestDraftExporterTest {
    @Test
    fun exportsNpcAnchorEvenWithoutPlace() {
        val exporter = QuestDraftExporter()
        val json = exporter.exportDraft(
            QuestDraftExporter.QuestDraftParams(
                draftId = "Q100",
                title = "Quest Test",
                mechanicId = "side_quests",
                npcGiver = "Dagon",
                stages = listOf(
                    QuestDraftExporter.StageDef(
                        id = "RETURN",
                        name = "Return",
                        completionMode = "manual_turn_in"
                    )
                ),
                objectives = listOf(
                    QuestDraftExporter.ObjectiveDef(
                        type = "visit_place",
                        target = "place:castel",
                        count = 1
                    )
                )
            )
        )

        val root = JsonParser.parseString(json).asJsonObject
        assertEquals("Q100", root["draftId"].asString)
        assertTrue(root.has("questAnchor"))
        assertTrue(root.has("stages"))

        val anchor = root["questAnchor"].asJsonObject
        assertEquals("npc", anchor["type"].asString)
        assertEquals("Dagon", anchor["target"].asString)
        assertFalse(anchor.has("place"))

        val stage = root["stages"].asJsonArray[0].asJsonObject
        assertEquals("RETURN", stage["id"].asString)
        assertEquals("manual_turn_in", stage["completionMode"].asString)
    }
}
