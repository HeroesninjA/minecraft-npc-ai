package ro.ainpc.spawn;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpawnBatchTrackerTest {

    @Test
    void parsesDebugFriendlyNpcIdsForRollback() {
        List<Integer> npcIds = SpawnBatchTracker.parseNpcDatabaseIds("Ion#12, Maria#34, 56, invalid, Ion#12");

        assertEquals(List.of(12, 34, 56), npcIds);
    }

    @Test
    void ignoresBlankOrMalformedNpcIds() {
        assertEquals(List.of(), SpawnBatchTracker.parseNpcDatabaseIds(""));
        assertEquals(List.of(), SpawnBatchTracker.parseNpcDatabaseIds("Ana#abc, #0, -1"));
    }

    @Test
    void normalizesBatchStatusFiltersForOperationalListing() {
        assertEquals("problem", SpawnBatchTracker.normalizeBatchStatusFilter(null));
        assertEquals("problem", SpawnBatchTracker.normalizeBatchStatusFilter("issues"));
        assertEquals("rolled_back", SpawnBatchTracker.normalizeBatchStatusFilter("rollback"));
        assertEquals("rolled_back", SpawnBatchTracker.normalizeBatchStatusFilter("rolled-back"));
        assertEquals("succeeded", SpawnBatchTracker.normalizeBatchStatusFilter("ok"));

        assertTrue(SpawnBatchTracker.isSupportedBatchStatusFilter("failed"));
        assertFalse(SpawnBatchTracker.isSupportedBatchStatusFilter("unknown"));
    }
}
