package ro.ainpc.gui.screens

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class MainHubGuiAuthoringTest {
    @Test
    fun mainHubGuiExposesAuthoringEntry() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/MainHubGui.kt").readText()

        assertTrue(source.contains("GuiKey.AUTHORING"))
        assertTrue(source.contains("Material.ENCHANTED_BOOK"))
        assertTrue(source.contains("Snapshot story, mapping si progresie"))
    }
}
