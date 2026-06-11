package ro.ainpc.spawn

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SpawnBatchPlanHasherTest {
    @Test
    fun settlementHashIsStableWhenAllocationsAreReordered() {
        val first = allocation("sat_01:casa_a", "family_a", "npc_ion", "Ion")
        val second = allocation("sat_01:casa_b", "family_b", "npc_maria", "Maria")

        val original = SpawnBatchPlanHasher.settlementPlanHash(listOf(first, second))
        val reordered = SpawnBatchPlanHasher.settlementPlanHash(listOf(second, first))

        assertEquals(original, reordered)
        assertEquals(
            SpawnBatchPlanHasher.settlementBatchKey(listOf(first, second)),
            SpawnBatchPlanHasher.settlementBatchKey(listOf(second, first))
        )
    }

    @Test
    fun settlementHashChangesWhenResidentPlanChanges() {
        val original = allocation("sat_01:casa_a", "family_a", "npc_ion", "Ion")
        val changed = allocation("sat_01:casa_a", "family_a", "npc_ion", "Ioan")

        assertNotEquals(
            SpawnBatchPlanHasher.settlementPlanHash(listOf(original)),
            SpawnBatchPlanHasher.settlementPlanHash(listOf(changed))
        )
    }

    @Test
    fun settlementBatchKeyUsesCommonRegionAndShortHash() {
        val allocation = allocation("Sat 01/Casa A", "family_a", "npc_ion", "Ion")

        val batchKey = SpawnBatchPlanHasher.settlementBatchKey(listOf(allocation))

        assertTrue(batchKey.startsWith("settlement:sat_01_casa_a:"))
        assertEquals(3, batchKey.split(":").size)
    }

    @Test
    fun dryRunBatchKeysAreSeparateFromApplyBatchKeys() {
        val allocation = allocation("sat_01:casa_a", "family_a", "npc_ion", "Ion")

        assertNotEquals(
            SpawnBatchPlanHasher.householdBatchKey(allocation),
            SpawnBatchPlanHasher.dryRunHouseholdBatchKey(allocation)
        )
        assertNotEquals(
            SpawnBatchPlanHasher.settlementBatchKey(listOf(allocation)),
            SpawnBatchPlanHasher.dryRunSettlementBatchKey(listOf(allocation))
        )
        assertTrue(SpawnBatchPlanHasher.dryRunHouseholdBatchKey(allocation).startsWith("dryrun:household:"))
        assertTrue(SpawnBatchPlanHasher.dryRunSettlementBatchKey(listOf(allocation)).startsWith("dryrun:settlement:"))
    }

    private fun allocation(placeId: String, familyId: String, npcKey: String, name: String): HouseAllocation {
        return HouseAllocation.builder(placeId)
            .familyId(familyId)
            .primaryOwnerNpcKey(npcKey)
            .maxResidents(1)
            .addResident(
                HouseAllocation.ResidentPlan.builder(npcKey, name)
                    .relationRole("resident")
                    .occupation("locuitor")
                    .spawnNodeId("$placeId:spawn")
                    .bedNodeId("$placeId:bed")
                    .build()
            )
            .build()
    }
}
