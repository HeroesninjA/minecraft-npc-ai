package ro.ainpc.gui.screens

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class DebugGuiWorldAuditTest {
    @Test
    fun debugGuiExposesWorldAuditShortcut() {
        val source = File("src/main/kotlin/ro/ainpc/gui/screens/DebugGui.kt").readText()

        assertTrue(source.contains("ainpc audit world"))
        assertTrue(source.contains("World audit"))
    }
}
