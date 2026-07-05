package ro.ainpc.gui.screens

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class AdminQuestGuiDiagnosticsTest {
    @Test
    fun adminQuestShowsQuestOverview() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/AdminQuestGui.kt").readText()

        assertTrue(source.contains("Admin Quest"))
        assertTrue(source.contains("Definitii"))
        assertTrue(source.contains("Mecanici"))
        assertTrue(source.contains("Diagnostic"))
        assertTrue(source.contains("Status: curat") || source.contains("Status: necesita atentie"))
    }

    @Test
    fun adminQuestHasAdminActions() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/AdminQuestGui.kt").readText()

        assertTrue(source.contains("Quest Log"))
        assertTrue(source.contains("Authoring"))
        assertTrue(source.contains("Ancore"))
        assertTrue(source.contains("Reset progresie"))
        assertTrue(source.contains("Story Snapshot"))
    }

    @Test
    fun adminQuestHasMechanicList() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/AdminQuestGui.kt").readText()

        assertTrue(source.contains("Toate progresiile"))
        assertTrue(source.contains("Active"))
        assertTrue(source.contains("Admin Mapping"))
    }

    @Test
    fun adminQuestShowsReadOnlyBanner() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/AdminQuestGui.kt").readText()

        assertTrue(source.contains("Read-only activ"))
        assertTrue(source.contains("MCP ruleaza in mod read-only."))
        assertTrue(source.contains("Scrierea in questuri si progresii este blocata."))
        assertTrue(source.contains("Inspectia ramane disponibila."))
    }
}
