package ro.ainpc.debug

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class DebugDumpQuestTextTest {
    @Test
    fun buildsQuestSummaryTextFromProgressionAndAnchorExports() {
        val source = File("src/main/kotlin/ro/ainpc/debug/DebugDumpQuestText.kt").readText()

        assertTrue(source.contains("DebugDumpProgressionJson.buildPlayerProgressionsJson(plugin)"))
        assertTrue(source.contains("DebugDumpProgressionJson.buildPlayerQuestProgressJson(plugin)"))
        assertTrue(source.contains("DebugDumpProgressionJson.buildQuestAnchorBindingsJson(plugin)"))
        assertTrue(source.contains("Quest audit report file: quest-audit-report.txt"))
        assertTrue(source.contains("Quest anchors by anchor type"))
        assertTrue(source.contains("Quest rows by status"))
    }
}
