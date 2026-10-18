package ro.ainpc.debug

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class DebugDumpQuestAuditCaptureTest {
    @Test
    fun capturedAnchorIdsKeepCaseSensitiveWorldLookup() {
        val capture = DebugDumpQuestAudit.RuntimeCapture(
            emptyList(),
            emptyList(),
            emptyMap(),
            DebugDumpStoryFeatureCapture.from(emptyList()),
            mapOf("region" to setOf("village")),
        )

        assertTrue(capture.hasAnchor("REGION", "village"))
        assertFalse(capture.hasAnchor("region", "Village"))
        assertFalse(capture.hasAnchor("region", "missing"))
        assertTrue(capture.hasAnchor("custom", "anything"))
    }

    @Test
    fun exportUsesCapturedAuditStateOnDatabaseWorker() {
        val service = File("src/main/kotlin/ro/ainpc/debug/DebugDumpService.kt").readText()
        val audit = File("src/main/kotlin/ro/ainpc/debug/DebugDumpQuestAudit.kt").readText()

        assertTrue(service.contains("DebugDumpQuestAudit.captureForAsync(plugin, requireNotNull(storyReadCapture).featureScenarios)"))
        assertTrue(service.contains("buildQuestAuditReportText(plugin, requireNotNull(capture.questAuditCapture))"))
        assertTrue(audit.contains("auditStoryProgressionConsistency(capture.storyScenarios, databaseManager, warnings)"))
        assertFalse(audit.contains("worldAdmin.getRegion(anchorId)"))
    }
}
