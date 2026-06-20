package ro.ainpc.debug

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class DebugDumpMappingTextTest {
    @Test
    fun buildsMappingSummaryTextFromWorldAndNpcBindingExports() {
        val source = File("src/main/kotlin/ro/ainpc/debug/DebugDumpMappingText.kt").readText()

        assertTrue(source.contains("DebugDumpWorldJson.buildWorldMappingJson(plugin)"))
        assertTrue(source.contains("DebugDumpNpcWorldBindingJson.buildNpcWorldBindingsJson(plugin)"))
        assertTrue(source.contains("World mapping available:"))
        assertTrue(source.contains("NPC bindings available:"))
        assertTrue(source.contains("Semantic index buckets:"))
        assertTrue(source.contains("Bindings by source"))
    }
}
