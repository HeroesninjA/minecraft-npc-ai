package ro.ainpc.spawn

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.sql.SQLException

class SettlementSpawnFailureHandlerTest {
    private val allocations = listOf("house_1", "house_2", "house_3").map { houseId ->
        HouseAllocation.builder("sat_a:$houseId").familyId("family_$houseId").build()
    }
    private val previousSuccess = HouseholdSpawnResult.success(emptyList(), emptyList(), null, emptyList())
    private val spawnFailure = HouseholdSpawnResult.failed(false, true, emptyList(), emptyList(), null, listOf("spawn failed"), emptyList())

    @Test
    fun persistenceFailurePreservesEarlierHouseholdsAndNeverInvokesDestructiveCallbacks() {
        val persistenceFailure = HouseholdSpawnResult.failedPersistence(emptyList(), emptyList(), null, listOf("save failed"), emptyList())
        val results = listOf(previousSuccess, persistenceFailure)

        val result = SettlementSpawnFailureHandler.finish(
            false, allocations, results, listOf("existing error"), listOf("existing warning"),
            rollbackCreatedNpcs = { error("NPC rollback must not run") },
            removeHousehold = { error("Household removal must not run") }
        )

        assertFalse(result.success())
        assertFalse(result.rolledBack())
        assertFalse(result.dryRun())
        assertEquals(1, result.successfulHouseholds())
        assertEquals(results, result.householdResults())
        assertEquals(allocations, result.allocations())
        assertEquals(listOf("existing error", "sat_a:house_2: save failed"), result.errors())
        assertTrue(result.warnings().contains("existing warning"))
        assertTrue(result.warnings().any { it.contains("fara rollback global") })
        assertFalse(result.warnings().any { it.contains("Rollback global settlement incomplet") })
    }

    @Test
    fun ordinarySpawnFailureKeepsRollbackAndCleanupBehavior() {
        var rollbackCalls = 0
        val removedHouseholds = mutableListOf<HouseAllocation>()
        val result = SettlementSpawnFailureHandler.finish(
            false, allocations, listOf(previousSuccess, spawnFailure), emptyList(), emptyList(),
            rollbackCreatedNpcs = { warnings ->
                rollbackCalls++
                warnings.add("rollback diagnostic")
                true
            },
            removeHousehold = { removedHouseholds.add(it) }
        )

        assertFalse(result.success())
        assertTrue(result.rolledBack())
        assertEquals(1, rollbackCalls)
        assertEquals(allocations.take(2), removedHouseholds)
        assertTrue(result.warnings().contains("rollback diagnostic"))
        assertTrue(result.warnings().any { it.contains("rollback local executat") })
        assertTrue(result.warnings().any { it.contains("Curatare DB pentru 2") })
    }

    @Test
    fun incompleteRollbackDoesNotRemoveHouseholdsAndKeepsDiagnostics() {
        val result = SettlementSpawnFailureHandler.finish(
            false, allocations, listOf(previousSuccess, spawnFailure), emptyList(), emptyList(),
            rollbackCreatedNpcs = { warnings ->
                warnings.add("NPC deletion failed")
                false
            },
            removeHousehold = { error("Household removal must not run after incomplete rollback") }
        )

        assertFalse(result.rolledBack())
        assertTrue(result.warnings().contains("NPC deletion failed"))
        assertTrue(result.warnings().any { it.contains("Rollback global settlement incomplet") })
    }

    @Test
    fun dryRunDoesNotInvokeDestructiveCallbacks() {
        val validationFailure = HouseholdSpawnResult.failed(true, false, emptyList(), emptyList(), null, listOf("invalid plan"), emptyList())
        val result = SettlementSpawnFailureHandler.finish(
            true, allocations, listOf(validationFailure), emptyList(), emptyList(),
            rollbackCreatedNpcs = { error("Dry run must not delete NPCs") },
            removeHousehold = { error("Dry run must not delete households") }
        )

        assertTrue(result.dryRun())
        assertFalse(result.success())
        assertFalse(result.rolledBack())
        assertTrue(result.warnings().any { it.contains("nu s-au creat NPC-uri") })
    }

    @Test
    fun cleanupFailureRemainsVisible() {
        val result = SettlementSpawnFailureHandler.finish(
            false, allocations, listOf(spawnFailure), emptyList(), emptyList(),
            rollbackCreatedNpcs = { true },
            removeHousehold = { throw SQLException("cleanup failed") }
        )

        assertFalse(result.success())
        assertTrue(result.rolledBack())
        assertTrue(result.warnings().any { it.contains("sat_a:house_1") && it.contains("cleanup failed") })
    }
}
