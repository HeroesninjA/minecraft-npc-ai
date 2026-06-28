package ro.ainpc.commands

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class AINPCCommandStoryDumpTest {
    @Test
    fun debugDumpStoryUsesStoryTextFormatter() {
        val source = File("src/main/kotlin/ro/ainpc/commands/AINPCCommand.kt").readText()

        assertTrue(source.contains("DebugDumpStoryText.buildStoryText(plugin)"))
        assertTrue(source.contains("DebugDumpStoryText.buildSummaryText(plugin)"))
        assertTrue(source.contains("handleDebugDumpStory(sender: CommandSender, args: Array<String>): Boolean"))
        assertTrue(source.contains("\"summary\", \"summarize\""))
    }
}
