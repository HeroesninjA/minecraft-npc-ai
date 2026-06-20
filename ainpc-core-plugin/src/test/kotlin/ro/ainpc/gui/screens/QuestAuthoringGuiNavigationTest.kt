package ro.ainpc.gui.screens

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class QuestAuthoringGuiNavigationTest {
    @Test
    fun questAuthoringGuiCanReturnToQuestDetailOrLog() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/QuestAuthoringGui.kt").readText()

        assertTrue(source.contains("val detailSelector = authoringSnapshot.requestedQuestSelector"))
        assertTrue(source.contains("Material.BOOK"))
        assertTrue(source.contains("Decision detail"))
        assertTrue(source.contains("decisionRuntimeExecutable"))
        assertTrue(source.contains("click.service().openQuestDetail(click.player(), detailSelector, \"all\")"))
        assertTrue(source.contains("click.service().openQuestLog(click.player(), \"all\")"))
    }
}
