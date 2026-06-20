package ro.ainpc.gui.screens

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class DebugGuiBuildInfoTest {
    @Test
    fun debugGuiExposesBuildMetadataAndVersionShortcut() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/DebugGui.kt").readText()

        assertTrue(source.contains("BuildVersionInfo.capture(context.plugin())"))
        assertTrue(source.contains("&aVersion"))
        assertTrue(source.contains("/npc version"))
        assertTrue(!source.contains("runCommand(\"version\")"))
    }
}
