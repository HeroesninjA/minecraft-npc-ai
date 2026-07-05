package ro.ainpc.gui.screens

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class NpcManagerGuiDiagnosticsTest {
    @Test
    fun npcManagerGuiExposesNpcDiagnosticsAndDebugDumpShortcut() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/NpcManagerGui.kt").readText()

        assertTrue(source.contains("ainpc debugdump npc"))
        assertTrue(source.contains("NPC Diagnostics"))
        assertTrue(source.contains("Spawned:"))
        assertTrue(source.contains("Ruleaza Tick Rutina"))
        assertTrue(source.contains("Rutinele NPC Active"))
    }
}
