package ro.ainpc.commands

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class PostSpawnBindingResultTest {
    @Test
    fun completeStatusRequiresEveryHouseholdAndNpc() {
        val result = PostSpawnBindingResult(
            householdsAttempted = 2,
            householdsCompleted = 2,
            npcsAttempted = 4,
            npcsCompleted = 4,
            mappingWritesApplied = 9,
            persistentBindingsSaved = 4,
            failures = emptyList()
        )

        assertEquals(PostSpawnBindingStatus.COMPLETE, result.status)
        assertTrue(postSpawnBindingSummary(result).contains("4/4 &7NPC-uri"))
        assertFalse(postSpawnBindingCompletionNotice("Spawn reusit", result).contains("Succes partial"))
    }

    @Test
    fun partialStatusReportsAppliedMappingWithoutRollback() {
        val result = PostSpawnBindingResult(
            householdsAttempted = 1,
            householdsCompleted = 0,
            npcsAttempted = 1,
            npcsCompleted = 0,
            mappingWritesApplied = 1,
            persistentBindingsSaved = 0,
            failures = listOf("work place lipsa")
        )

        val notice = postSpawnBindingCompletionNotice("Settlement spawn reusit", result)

        assertEquals(PostSpawnBindingStatus.PARTIAL, result.status)
        assertTrue(notice.contains("Succes partial"))
        assertTrue(notice.contains("NPC-urile raman spawnate"))
        assertTrue(notice.contains("nu s-a executat rollback"))
    }

    @Test
    fun failedAndNotApplicableStatusesRemainDistinct() {
        val failed = PostSpawnBindingResult(
            householdsAttempted = 1,
            householdsCompleted = 0,
            npcsAttempted = 1,
            npcsCompleted = 0,
            mappingWritesApplied = 0,
            persistentBindingsSaved = 0,
            failures = listOf("binding indisponibil")
        )

        assertEquals(PostSpawnBindingStatus.FAILED, failed.status)
        assertEquals(PostSpawnBindingStatus.NOT_APPLICABLE, PostSpawnBindingResult.EMPTY.status)
    }

    @Test
    fun aggregationPreservesProgressAndFailures() {
        val complete = PostSpawnBindingResult(1, 1, 2, 2, 4, 2, emptyList())
        val partial = PostSpawnBindingResult(1, 0, 1, 0, 1, 0, listOf("persistenta esuata"))

        val aggregate = complete + partial

        assertEquals(PostSpawnBindingStatus.PARTIAL, aggregate.status)
        assertEquals(2, aggregate.householdsAttempted)
        assertEquals(3, aggregate.npcsAttempted)
        assertEquals(5, aggregate.mappingWritesApplied)
        assertEquals(listOf("persistenta esuata"), aggregate.failures)
    }

    @Test
    fun householdAndSettlementCommandsUseStructuredBindingOutcome() {
        val source = File("src/main/kotlin/ro/ainpc/commands/AINPCCommand.kt").readText()

        assertTrue(source.contains("val bindingResult = bindSpawnedHouseholdToMapping"))
        assertTrue(source.contains("val bindingResult = bindSpawnedSettlementToMapping"))
        assertTrue(source.contains("sendPostSpawnBindingResult(sender, bindingResult)"))
        assertTrue(source.contains("if (saveNpcWorldBinding(sender, persistentBinding, false))"))
        assertFalse(source.contains("NPC-urile au fost create si legate la mapping."))
        assertFalse(source.contains("Settlement spawn terminat. Ruleaza"))
    }
}
