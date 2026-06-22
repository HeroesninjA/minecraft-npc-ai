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

        assertTrue(source.contains("Progresie nearest"))
        assertTrue(source.contains("Accepta nearest"))
        assertTrue(source.contains("Status nearest"))
    }

    @Test
    fun npcInteractionGuiHasContextActions() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/NpcInteractionGui.kt").readText()

        assertTrue(source.contains("Story nearest"))
        assertTrue(source.contains("Rutina nearest"))
        assertTrue(source.contains("Shop nearest"))
    }

    @Test
    fun npcInteractionGuiUsesStandardControls() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/NpcInteractionGui.kt").readText()

        assertTrue(source.contains("GuiNavigation.addStandardControls"))
        assertTrue(source.contains("GuiKey.INTERACT"))
        assertTrue(source.contains("fillEmpty"))
    }
}
