package ro.ainpc.debug

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.time.LocalDateTime

class DebugDumpSnapshotTest {
    @Test
    fun artifactSetPreservesOrderAndRejectsDuplicates() {
        val artifacts = DebugDumpArtifactSet()
        artifacts.addText("first.txt", "first")
        artifacts.addJson("second.json", mapOf("ok" to true))

        assertEquals(listOf("first.txt", "second.json"), artifacts.values().map { it.fileName })
        assertThrows(IllegalStateException::class.java) {
            artifacts.addText("first.txt", "duplicate")
        }
        assertThrows(IllegalArgumentException::class.java) {
            DebugDumpArtifactSnapshot.text("nested/unsafe.txt", "unsafe")
        }
    }

    @Test
    fun serverFilesRenderOnlyCapturedValues() {
        val capturedAt = LocalDateTime.of(2026, 7, 18, 10, 15, 30)
        val snapshot = DebugDumpServerSnapshotData(
            "1.2.3",
            7,
            DebugDumpWorldAdminSummary(true, 2, 3, 4),
            "Paper",
            "1.21.11-R0.1",
            "1.21.11",
            "25",
            "JetBrains",
            "Windows",
            "11",
            "amd64",
            5,
            listOf(DebugDumpWorldSummary("world", "NORMAL", 12, 34)),
        )

        val summary = DebugDumpServerSnapshot.buildSummary("all", Path.of("debug-dump-test"), capturedAt, snapshot)
        val server = DebugDumpServerSnapshot.buildServerInfo(snapshot)

        assertTrue(summary.contains("Generated: 2026-07-18T10:15:30"))
        assertTrue(summary.contains("Plugin version: 1.2.3"))
        assertTrue(summary.contains("NPC count: 7"))
        assertTrue(server.contains("Online players: 5"))
        assertTrue(server.contains("world env=NORMAL loadedChunks=12 entities=34"))
    }
}
