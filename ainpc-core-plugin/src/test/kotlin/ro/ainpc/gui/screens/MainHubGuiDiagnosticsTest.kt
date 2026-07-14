package ro.ainpc.gui.screens

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class MainHubGuiDiagnosticsTest {
    @Test
    fun mainHubShowsReadOnlyBanner() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/MainHubGui.kt").readText()

        assertTrue(source.contains("Read-only activ"))
        assertTrue(source.contains("MCP ruleaza in mod read-only."))
        assertTrue(source.contains("Operatiile de scriere in mapping sunt blocate."))
        assertTrue(source.contains("Inspectia ramane disponibila: status / history / export."))
    }

    @Test
    fun mainHubShowsQuestLabels() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/MainHubGui.kt").readText()

        assertTrue(source.contains("Quest Debug"))
        assertTrue(source.contains("Quest Anchors"))
        assertTrue(source.contains("World Mapping"))
        assertTrue(source.contains("Lista NPC Admin"))
        assertTrue(source.contains("Listeaza si mapeaza ancorele persistate"))
    }
}
