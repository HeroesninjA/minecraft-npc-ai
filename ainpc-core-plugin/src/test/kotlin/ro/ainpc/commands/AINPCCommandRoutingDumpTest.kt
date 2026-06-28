package ro.ainpc.commands

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class AINPCCommandRoutingDumpTest {
    @Test
    fun debugDumpRoutingUsesRoutingTextFormatter() {
        val source = File("src/main/kotlin/ro/ainpc/commands/AINPCCommand.kt").readText()

        assertTrue(source.contains("DebugDumpRoutingText.buildRoutingText(plugin)"))
        assertTrue(source.contains("DebugDumpRoutingText.buildSummaryText(plugin)"))
        assertTrue(source.contains("handleDebugDumpRouting(sender: CommandSender, args: Array<String>): Boolean"))
        assertTrue(source.contains("\"routing\" -> handleDebugDumpRouting(sender, args)"))
        assertTrue(source.contains("debugdump routing [summary]"))
        assertTrue(source.contains("Routing Summary"))
    }
}
