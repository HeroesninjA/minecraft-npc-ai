package ro.ainpc.spawn

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SpawnBatchTrackerTest {
    @Test
    fun parsesDebugFriendlyNpcIdsForRollback() {
        val npcIds = SpawnBatchTracker.parseNpcDatabaseIds("Ion#12, Maria#34, 56, invalid, Ion#12")
        assertEquals(listOf(12, 34, 56), npcIds)
    }

    @Test
    fun ignoresBlankOrMalformedNpcIds() {
        assertEquals(listOf<Int>(), SpawnBatchTracker.parseNpcDatabaseIds(""))
        assertEquals(listOf<Int>(), SpawnBatchTracker.parseNpcDatabaseIds("Ana#abc, #0, -1"))
    }

    @Test
    fun normalizesBatchStatusFiltersForOperationalListing() {
        assertEquals("problem", SpawnBatchTracker.normalizeBatchStatusFilter(null))
        assertEquals("problem", SpawnBatchTracker.normalizeBatchStatusFilter("issues"))
        assertEquals("rolled_back", SpawnBatchTracker.normalizeBatchStatusFilter("rollback"))
        assertEquals("rolled_back", SpawnBatchTracker.normalizeBatchStatusFilter("rolled-back"))
        assertEquals("succeeded", SpawnBatchTracker.normalizeBatchStatusFilter("ok"))
        assertTrue(SpawnBatchTracker.isSupportedBatchStatusFilter("failed"))
        assertFalse(SpawnBatchTracker.isSupportedBatchStatusFilter("unknown"))
    }
}
