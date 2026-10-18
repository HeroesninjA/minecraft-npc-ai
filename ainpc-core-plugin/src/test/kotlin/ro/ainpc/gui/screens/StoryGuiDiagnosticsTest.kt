package ro.ainpc.gui.screens

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class StoryGuiDiagnosticsTest {
    @Test
    fun storyGuiExposesStoryDiagnosticsAndDebugDumpShortcut() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/StoryGui.kt").readText()

        assertTrue(source.contains("DebugDumpStoryText.buildStoryStatePreviewText(activeScenarioCount, states)"))
        assertTrue(source.contains("DebugDumpStoryStateJson.buildStoryStatesJson(plugin)"))
        assertTrue(source.contains("ainpc debugdump story"))
        assertTrue(source.contains("Afiseaza diagnosticul story complet in chat."))
        assertTrue(source.contains("Story Diagnostics"))
        assertTrue(source.contains("Progression Verification"))
        assertTrue(source.contains("Mapping Mode"))
        assertTrue(source.contains("Mapping State"))
        assertTrue(source.contains("Mapping Pool"))
        assertTrue(source.contains("Story Context read-only"))
        assertTrue(source.contains("Story Anchors"))
        assertTrue(source.contains("Story Warnings"))
        assertTrue(source.contains("Quest Events"))
        assertTrue(source.contains("Quest Code"))
        assertTrue(source.contains("Quest Status"))
        assertTrue(source.contains("getStoredProgressionCounts(playerId)"))
        assertTrue(source.contains("storyContextService.buildForPlayerAsync"))
        assertEquals(1, source.split("storyFuture.thenCombine(progressionFuture, ::progressionVerificationLore)").size - 1)
        assertEquals(1, source.split("getAnchorBindings(playerId, null, 200)").size - 1)
        assertTrue(source.contains("databaseManager.supplyAsync"))
        assertTrue(source.contains("updateIfCurrent(context.player(), sessionId, key(), update)"))
        assertEquals(1, source.split("if (player.hasPermission(\"ainpc.admin\")) loadDiagnosticsCard(context, sessionId)").size - 1)
        assertTrue(source.contains("if (!current.player().hasPermission(\"ainpc.admin\"))"))
        assertTrue(source.contains("Permisiune necesara: ainpc.admin"))
    }
}
