package ro.ainpc.commands

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class AINPCDebugDumpAiVersionTest {
    @Test
    fun debugDumpAiShowsBuildMetadata() {
        val source = File("src/main/kotlin/ro/ainpc/commands/AINPCCommand.kt").readText()

        assertTrue(source.contains("BuildVersionInfo.capture(plugin)"))
        assertTrue(source.contains("&eBuild version:"))
        assertTrue(source.contains("&eBuild hash:"))
        assertTrue(source.contains("&eBuild timestamp:"))
    }
}
