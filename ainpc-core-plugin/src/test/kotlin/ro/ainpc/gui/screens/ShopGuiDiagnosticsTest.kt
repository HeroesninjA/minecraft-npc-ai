package ro.ainpc.gui.screens

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class ShopGuiDiagnosticsTest {
    @Test
    fun shopGuiHasNpcList() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/ShopGui.kt").readText()

        assertTrue(source.contains("Shop & NPCs"))
        assertTrue(source.contains("getNPCsNear"))
        assertTrue(source.contains("getNPCCount"))
    }

    @Test
    fun shopGuiHasBalanceDisplay() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/ShopGui.kt").readText()

        assertTrue(source.contains("GOLD_INGOT"))
        assertTrue(source.contains("economyService.getBalance"))
        assertTrue(source.contains("Balanța"))
    }

    @Test
    fun shopGuiHasOfferView() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/ShopGui.kt").readText()

        assertTrue(source.contains("renderOffers"))
        assertTrue(source.contains("shopService.findShopsForRole"))
        assertTrue(source.contains("Inapoi la lista NPC"))
    }

    @Test
    fun shopGuiHasNavigationAndFiller() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/ShopGui.kt").readText()

        assertTrue(source.contains("GuiNavigation.addStandardControls"))
        assertTrue(source.contains("GuiKey.SHOP"))
        assertTrue(source.contains("fillEmpty"))
    }
}
