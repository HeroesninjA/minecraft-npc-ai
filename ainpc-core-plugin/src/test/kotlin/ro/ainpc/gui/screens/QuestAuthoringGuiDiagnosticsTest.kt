package ro.ainpc.gui.screens

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class QuestAuthoringGuiDiagnosticsTest {
    @Test
    fun questAuthoringGuiExposesAuthoringDiagnosticsAndDumpShortcut() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/QuestAuthoringGui.kt").readText()

        assertTrue(source.contains("DebugDumpAuthoringText.buildAuthoringText(plugin, player, preferredQuestSelector, preferredMechanicId)"))
        assertTrue(source.contains("ainpc authoring dump"))
        assertTrue(source.contains("Authoring diagnostics"))
        assertTrue(source.contains("QUEST_AUTHORING_SUMMARY"))
        assertTrue(source.contains("Quest Semantic"))
        assertTrue(source.contains("Quest Authoring Summary"))
        assertTrue(source.contains("Quest Generation"))
        assertTrue(source.contains("AI preset"))
        assertTrue(source.contains("Story Signals"))
        assertTrue(source.contains("Story Context"))
        assertTrue(source.contains("Story Mode"))
        assertTrue(source.contains("cycleAuthoringQuestSelector"))
        assertTrue(source.contains("cycleAuthoringMechanicId"))
        assertTrue(source.contains("clearAuthoringSelection"))
        assertTrue(source.contains("Quest Creator"))
        assertTrue(source.contains("Mapping Creator"))
        assertTrue(source.contains("deschide quest creator"))
        assertTrue(source.contains("deschide mapping creator"))
        assertTrue(source.contains("Quest Draft Preview"))
        assertTrue(source.contains("Quest Editor"))
        assertTrue(source.contains("Deschide editorul cu lista de definitii."))
        assertTrue(source.contains("MCP ruleaza in mod read-only."))
        assertTrue(source.contains("Authoring writes sunt blocate."))
        assertTrue(source.contains("/ainpc quest preview"))
    }
}
