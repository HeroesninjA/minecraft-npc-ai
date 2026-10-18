package ro.ainpc.debug

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class DebugDumpPlayerProgressionJsonTest {
    @Test
    fun playerProgressionExportUsesLiteralSqlFilterAndDeclaresItsPresence() {
        val source = File("src/main/kotlin/ro/ainpc/debug/DebugDumpPlayerProgressionJson.kt").readText()

        assertTrue(source.contains("DebugDumpPlayerUuidFilter.addMetadata(root, playerFilter)"))
        assertTrue(source.contains("DebugDumpPlayerUuidFilter.sqlCondition(\"player_uuid\")"))
        assertTrue(source.contains("stmt.setString(1, normalizedFilter)"))
        assertFalse(source.contains("LIKE ?"))
    }
}
