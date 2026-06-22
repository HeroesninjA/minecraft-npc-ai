package ro.ainpc.context

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ContextServiceTest {

    @Test
    fun shortSummaryFormat() {
        val svc = ContextService(null)
        val summary = svc.buildShortSummary("TestPlayer")
        assertTrue(summary.contains("TestPlayer"))
        assertTrue(summary.contains("Lv"))
        assertTrue(summary.contains("coins"))
    }

    @Test
    fun compactPromptBlockEmpty() {
        val svc = ContextService(null)
        val block = svc.buildCompactPromptBlock("")
        assertTrue(block.contains("Context Snapshot"))
        assertTrue(block.contains("("))
    }

    @Test
    fun compactPromptBlockWithPlayer() {
        val svc = ContextService(null)
        val block = svc.buildCompactPromptBlock("Player1")
        assertTrue(block.contains("Player1"))
        assertTrue(block.contains("Context Snapshot"))
    }
}
