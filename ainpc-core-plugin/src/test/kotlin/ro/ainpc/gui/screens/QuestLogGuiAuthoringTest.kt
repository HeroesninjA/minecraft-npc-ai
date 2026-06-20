package ro.ainpc.gui.screens

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class QuestLogGuiAuthoringTest {
    @Test
    fun questLogGuiExposesAuthoringSummaryCard() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/QuestLogGui.kt").readText()

        assertTrue(source.contains("renderAuthoringSummary(context, snapshot)"))
        assertTrue(source.contains("context.plugin().authoringService.analyze("))
        assertTrue(source.contains("Material.ENCHANTED_BOOK"))
        assertTrue(source.contains("click.service().openAuthoring(click.player(), selectedEntry?.selector(), selectedEntry?.mechanicId())"))
    }
}
