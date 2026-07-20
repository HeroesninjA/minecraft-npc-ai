package ro.ainpc.debug

import com.google.gson.Gson
import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.time.Duration

class DebugDumpManifestTest {
    @Test
    fun declaresConfidentialityWithoutEmbeddingPlayerFilterValueOrAbsolutePaths() {
        val cleanup = DebugDumpCleanupResult(
            listOf(DebugDumpDeletedExport("debug-dump-20260701-120000", 128L, DebugDumpCleanupReason.AGE)),
            emptyList(),
            1,
            256L,
            true,
            true,
            true,
        )
        val manifest = DebugDumpManifest.build(
            "all",
            "2026-07-17T12:00:00",
            DebugDumpPrivacyMode.PRIVACY_SAFE,
            true,
            listOf(
                DebugDumpWriteResult(
                    Path.of("C:/server/plugins/AINPC/debug-dumps/debug-dump-20260717-120000/summary.txt"),
                    DebugDumpArtifactFormat.TEXT,
                    512,
                    256,
                    true,
                )
            ),
            DebugDumpRetentionPolicy(Duration.ofDays(14), 20, 512L * 1024L * 1024L),
            cleanup,
        )
        val json = Gson().toJson(manifest)
        val root = JsonParser.parseString(json).asJsonObject
        val confidentiality = root.getAsJsonObject("confidentiality")
        val artifact = root.getAsJsonArray("artifacts")[0].asJsonObject

        assertEquals(1, root.get("schema_version").asInt)
        assertEquals("privacy-safe-review-required", confidentiality.get("classification").asString)
        assertTrue(confidentiality.get("manual_review_required").asBoolean)
        assertTrue(confidentiality.get("player_filter_present").asBoolean)
        assertFalse(confidentiality.get("player_filter_value_included").asBoolean)
        assertEquals("summary.txt", artifact.get("file").asString)
        assertTrue(artifact.get("truncated").asBoolean)
        assertFalse(json.contains("C:/server"))
        assertFalse(json.contains("Alice"))
    }

    @Test
    fun capturedManifestDeclaresThreadingAndDatabaseConsistencyModel() {
        val cleanup = DebugDumpCleanupResult(emptyList(), emptyList(), 0, 0L, true, true, true)
        val manifest = DebugDumpManifest.buildCaptured(
            "npc",
            "2026-07-18T10:00:00",
            "2026-07-18T10:00:01",
            DebugDumpPrivacyMode.STANDARD,
            false,
            emptyList(),
            DebugDumpRetentionPolicy(Duration.ofDays(14), 20, 512L * 1024L * 1024L),
            cleanup,
            true,
        )
        val snapshot = JsonParser.parseString(Gson().toJson(manifest))
            .asJsonObject
            .getAsJsonObject("snapshot")

        assertEquals("server-main", snapshot.get("runtime_capture_thread").asString)
        assertEquals("paper-async-worker", snapshot.get("database_and_file_io_thread").asString)
        assertTrue(snapshot.get("database_transaction_used").asBoolean)
        assertTrue(snapshot.get("artifact_set_frozen_before_write").asBoolean)
    }
}
