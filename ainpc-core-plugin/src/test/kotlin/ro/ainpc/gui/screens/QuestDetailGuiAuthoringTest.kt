package ro.ainpc.gui.screens

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class QuestDetailGuiAuthoringTest {
    @Test
    fun questDetailGuiExposesAuthoringSummaryCard() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/QuestDetailGui.kt").readText()

        assertTrue(source.contains("findProgressionGuiEntry("))
        assertTrue(source.contains("renderSelectionDiagnostics(context, entry)"))
        assertTrue(source.contains("renderAuthoringCard(context, entry)"))
        assertTrue(source.contains("context.plugin().authoringService.analyze("))
        assertTrue(source.contains("Material.ENCHANTED_BOOK"))
        assertTrue(source.contains("click.service().openAuthoring(click.player(), entry.selector(), entry.mechanicId())"))
    }
}
