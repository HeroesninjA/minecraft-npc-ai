package ro.ainpc.commands

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class AINPCCommandMappingDumpTest {
    @Test
    fun debugDumpMappingUsesMappingTextFormatter() {
        val source = File("src/main/kotlin/ro/ainpc/commands/AINPCCommand.kt").readText()

        assertTrue(source.contains("DebugDumpMappingText.buildMappingText(plugin)"))
        assertTrue(source.contains("DebugDumpMappingText.buildSummaryText(plugin)"))
        assertTrue(source.contains("handleDebugDumpMapping(sender: CommandSender, args: Array<String>): Boolean"))
        assertTrue(source.contains("\"summary\", \"summarize\""))
    }
}
