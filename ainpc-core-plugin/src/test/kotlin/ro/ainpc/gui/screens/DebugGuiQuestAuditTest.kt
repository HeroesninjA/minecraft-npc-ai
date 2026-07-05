package ro.ainpc.gui.screens

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class DebugGuiQuestAuditTest {
    @Test
    fun debugGuiExposesQuestAuditShortcut() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/DebugGui.kt").readText()

        assertTrue(source.contains("ainpc audit quest"))
        assertTrue(source.contains("Quest Audit"))
        assertTrue(source.contains("Quest Authoring"))
    }

    @Test
    fun debugGuiShowsReadOnlyBanner() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/DebugGui.kt").readText()

        assertTrue(source.contains("Read-only activ"))
        assertTrue(source.contains("MCP ruleaza in mod read-only."))
        assertTrue(source.contains("Inspectia ramane disponibila: debug / status / export."))
        assertTrue(source.contains("Debugdump-ul este read-only."))
    }
}
