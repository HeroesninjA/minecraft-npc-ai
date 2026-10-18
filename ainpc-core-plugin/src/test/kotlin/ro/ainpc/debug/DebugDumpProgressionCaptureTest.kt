package ro.ainpc.debug

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class DebugDumpProgressionCaptureTest {
    @Test
    fun exportUsesCapturedDefinitionsForStoredProgressions() {
        val service = File("src/main/kotlin/ro/ainpc/debug/DebugDumpService.kt").readText()
        val progression = File("src/main/kotlin/ro/ainpc/debug/DebugDumpProgressionJson.kt").readText()

        assertTrue(service.contains("DebugDumpProgressionJson.captureForAsync(plugin)"))
        assertTrue(service.contains("DebugDumpProgressionJson.buildPlayerProgressionsJsonFromCapture("))
        assertTrue(progression.contains("progressionService.getStoredProgressions(capture.definitions, playerFilter)"))
    }

    @Test
    fun playerOwnedQuestExportsApplyTheUuidFilter() {
        val service = File("src/main/kotlin/ro/ainpc/debug/DebugDumpService.kt").readText()
        val progression = File("src/main/kotlin/ro/ainpc/debug/DebugDumpProgressionJson.kt").readText()
        val playerProgression = File("src/main/kotlin/ro/ainpc/debug/DebugDumpPlayerProgressionJson.kt").readText()

        assertTrue(progression.contains("progressionService.getStoredProgressions(capture.definitions, playerFilter)"))
        assertTrue(progression.contains("DebugDumpPlayerUuidFilter.sqlCondition(\"player_uuid\")"))
        assertTrue(progression.contains("DebugDumpPlayerUuidFilter.sqlCondition(\"b.player_uuid\")"))
        assertTrue(service.contains("buildQuestAnchorBindingsJson(plugin, capture.playerFilter)"))
        assertTrue(playerProgression.contains("DebugDumpPlayerUuidFilter.sqlCondition(\"player_uuid\")"))
        assertEquals(3, Regex("DebugDumpPlayerUuidFilter\\.addMetadata\\(root, playerFilter\\)").findAll(progression).count())
        assertTrue(playerProgression.contains("DebugDumpPlayerUuidFilter.addMetadata(root, playerFilter)"))
    }
}
