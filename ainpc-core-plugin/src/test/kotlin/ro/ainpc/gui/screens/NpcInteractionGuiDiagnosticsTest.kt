package ro.ainpc.gui.screens

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class NpcInteractionGuiDiagnosticsTest {
    @Test
    fun npcInteractionGuiHasNpcCards() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/NpcInteractionGui.kt").readText()

        assertTrue(source.contains("PLAYER_HEAD"))
        assertTrue(source.contains("npc.name"))
        assertTrue(source.contains("occupation"))
        assertTrue(source.contains("currentState"))
    }

    @Test
    fun npcInteractionGuiHasShopIntegration() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/NpcInteractionGui.kt").readText()

        assertTrue(source.contains("shopService.findShopsForRole"))
        assertTrue(source.contains("hasShop"))
        assertTrue(source.contains("EMERALD"))
        assertTrue(source.contains("setShopSelectedNpcId"))
        assertTrue(source.contains("GuiKey.SHOP"))
    }

    @Test
    fun npcInteractionGuiHasProgressionActions() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/NpcInteractionGui.kt").readText()

        assertTrue(source.contains("Quest Nearest"))
        assertTrue(source.contains("Progresie Nearest"))
        assertTrue(source.contains("Accepta Nearest"))
        assertTrue(source.contains("Status Nearest"))
    }

    @Test
    fun npcInteractionGuiHasContextActions() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/NpcInteractionGui.kt").readText()

        assertTrue(source.contains("Story Nearest"))
        assertTrue(source.contains("Rutina Nearest"))
        assertTrue(source.contains("Routine Nearest"))
        assertTrue(source.contains("Shop Nearest"))
        assertTrue(source.contains("Pentru Dialog Direct: Click Dreapta pe NPC In Lume."))
        assertTrue(source.contains("Mai Apropiat NPC"))
    }

    @Test
    fun npcInteractionGuiUsesStandardControls() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/NpcInteractionGui.kt").readText()

        assertTrue(source.contains("GuiNavigation.addStandardControls"))
        assertTrue(source.contains("GuiKey.INTERACT"))
        assertTrue(source.contains("fillEmpty"))
    }
}
