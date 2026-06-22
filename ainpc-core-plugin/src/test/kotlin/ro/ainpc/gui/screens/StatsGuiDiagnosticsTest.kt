package ro.ainpc.gui.screens

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class StatsGuiDiagnosticsTest {
    @Test
    fun statsGuiHasPlayerSnapshot() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/StatsGui.kt").readText()

        assertTrue(source.contains("Snapshot jucator"))
        assertTrue(source.contains("player.level"))
        assertTrue(source.contains("player.health"))
        assertTrue(source.contains("player.foodLevel"))
    }

    @Test
    fun statsGuiHasEconomyBalance() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/StatsGui.kt").readText()

        assertTrue(source.contains("GOLD_INGOT"))
        assertTrue(source.contains("economyService.getBalance"))
        assertTrue(source.contains("Economie"))
        assertTrue(source.contains("monede"))
    }

    @Test
    fun statsGuiHasNavigation() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/StatsGui.kt").readText()

        assertTrue(source.contains("Progresii"))
        assertTrue(source.contains("World"))
        assertTrue(source.contains("Admin Mapping"))
        assertTrue(source.contains("Admin Quest"))
    }

    @Test
    fun statsGuiListsNearbyNpcs() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/StatsGui.kt").readText()

        assertTrue(source.contains("PLAYER_HEAD"))
        assertTrue(source.contains("npc.name"))
        assertTrue(source.contains("npc.occupation"))
        assertTrue(source.contains("npc.age"))
    }

    @Test
    fun statsGuiUsesStandardControls() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/StatsGui.kt").readText()

        assertTrue(source.contains("GuiNavigation.addStandardControls"))
        assertTrue(source.contains("GuiKey.STATS"))
        assertTrue(source.contains("fillEmpty"))
    }
}
