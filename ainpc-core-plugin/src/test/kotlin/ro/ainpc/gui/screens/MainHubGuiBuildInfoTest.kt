package ro.ainpc.gui.screens

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class MainHubGuiBuildInfoTest {
    @Test
    fun mainHubGuiExposesBuildSnapshotAndVersionShortcut() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/MainHubGui.kt").readText()

        assertTrue(source.contains("BuildVersionInfo.capture(context.plugin())"))
        assertTrue(source.contains("&aVersion snapshot"))
        assertTrue(source.contains("GuiItemFactory.item(Material.NAME_TAG, \"&eVersion\""))
        assertTrue(source.contains("/npc version"))
        assertTrue(!source.contains("runCommand(\"version\")"))
    }
}
