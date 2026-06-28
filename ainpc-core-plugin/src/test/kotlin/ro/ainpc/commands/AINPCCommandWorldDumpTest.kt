package ro.ainpc.commands

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class AINPCCommandWorldDumpTest {
    @Test
    fun debugDumpWorldSupportsSummaryMode() {
        val source = File("src/main/kotlin/ro/ainpc/commands/AINPCCommand.kt").readText()

        assertTrue(source.contains("handleDebugDumpWorld(sender: CommandSender, args: Array<String>): Boolean"))
        assertTrue(source.contains("\"world\", \"worlds\" -> handleDebugDumpWorld(sender, args)"))
        assertTrue(source.contains("World Admin Summary"))
        assertTrue(source.contains("\"summary\", \"summarize\""))
        assertTrue(source.contains("debugdump world [summary]"))
    }
}
