package ro.ainpc.gui.screens

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class AdminMcpGuiDiagnosticsTest {
    @Test
    fun adminMcpShowsReadOnlyBanner() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/AdminMcpGui.kt").readText()

        assertTrue(source.contains("Read-only activ"))
        assertTrue(source.contains("MCP ruleaza in mod read-only."))
        assertTrue(source.contains("Scrierea in mapping este blocata."))
        assertTrue(source.contains("Inspectia ramane disponibila: status / history / export."))
    }

    @Test
    fun adminMcpShowsQuestContextLabel() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/AdminMcpGui.kt").readText()

        assertTrue(source.contains("World Context"))
        assertTrue(source.contains("Story Context"))
        assertTrue(source.contains("Quest Context"))
        assertTrue(source.contains("Mapping Context"))
        assertTrue(source.contains("debugdump quest semantic context"))
    }
}
