package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class QuestAuthoringSnapshotTest {
    @Test
    fun exposesQuestDirectorDecisionDiagnostics() {
        val decision = QuestDirectorDecision.blocked(
            "request_blocked",
            listOf("mapping blocked", "story blocked"),
            listOf("story warning")
        )
        val snapshot = QuestAuthoringSnapshot(
            handled = true,
            decision = decision
        )

        assertEquals(emptyList<String>(), snapshot.decisionMatchedSignals())
        assertEquals(emptyList<String>(), snapshot.decisionCandidateTemplateIds())
        assertEquals(listOf("mapping blocked", "story blocked"), snapshot.decisionBlockedReasons())
        assertEquals(listOf("story warning"), snapshot.decisionWarnings())
        assertFalse(snapshot.decisionRuntimeExecutable())
        assertTrue(snapshot.decisionStatus().isNotBlank())
        assertEquals("request_blocked", snapshot.decisionReason())
    }
}
