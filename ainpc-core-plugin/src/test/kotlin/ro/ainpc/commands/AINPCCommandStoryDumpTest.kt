package ro.ainpc.commands

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class AINPCCommandStoryDumpTest {
    @Test
    fun debugDumpStoryCapturesRuntimeBeforeDatabaseWorkAndReturnsOnMain() {
        val source = File("src/main/kotlin/ro/ainpc/commands/AINPCCommand.kt").readText()
        val handler = source.substringAfter("private fun handleDebugDumpStory(sender: CommandSender, args: Array<String>): Boolean")
            .substringBefore("private fun handleDebugDumpAi(")

        assertTrue(handler.contains("DebugDumpStoryText.captureForAsync(plugin)"))
        assertTrue(handler.contains("databaseManager.supplyAsync"))
        assertTrue(handler.contains("DebugDumpStoryText.buildFromDatabaseCapture(plugin, capture, summaryOnly)"))
        assertTrue(handler.contains("plugin.server.scheduler.runTask(plugin, report)"))
        assertTrue(handler.contains("sender.hasPermission(\"ainpc.admin\")"))
        assertTrue(handler.contains("\"summary\", \"summarize\""))
    }
}
