package ro.ainpc.spawn;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpawnBatchPlanHasherTest {

    @Test
    void settlementHashIsStableWhenAllocationsAreReordered() {
        HouseAllocation first = allocation("sat_01:casa_a", "family_a", "npc_ion", "Ion");
        HouseAllocation second = allocation("sat_01:casa_b", "family_b", "npc_maria", "Maria");

        String original = SpawnBatchPlanHasher.settlementPlanHash(List.of(first, second));
        String reordered = SpawnBatchPlanHasher.settlementPlanHash(List.of(second, first));

        assertEquals(original, reordered);
        assertEquals(
            SpawnBatchPlanHasher.settlementBatchKey(List.of(first, second)),
            SpawnBatchPlanHasher.settlementBatchKey(List.of(second, first))
        );
    }

    @Test
    void settlementHashChangesWhenResidentPlanChanges() {
        HouseAllocation original = allocation("sat_01:casa_a", "family_a", "npc_ion", "Ion");
        HouseAllocation changed = allocation("sat_01:casa_a", "family_a", "npc_ion", "Ioan");

        assertNotEquals(
            SpawnBatchPlanHasher.settlementPlanHash(List.of(original)),
            SpawnBatchPlanHasher.settlementPlanHash(List.of(changed))
        );
    }

    @Test
    void settlementBatchKeyUsesCommonRegionAndShortHash() {
        HouseAllocation allocation = allocation("Sat 01/Casa A", "family_a", "npc_ion", "Ion");

        String batchKey = SpawnBatchPlanHasher.settlementBatchKey(List.of(allocation));

        assertTrue(batchKey.startsWith("settlement:sat_01_casa_a:"));
        assertEquals(3, batchKey.split(":").length);
    }

    @Test
    void dryRunBatchKeysAreSeparateFromApplyBatchKeys() {
        HouseAllocation allocation = allocation("sat_01:casa_a", "family_a", "npc_ion", "Ion");

        assertNotEquals(
            SpawnBatchPlanHasher.householdBatchKey(allocation),
            SpawnBatchPlanHasher.dryRunHouseholdBatchKey(allocation)
        );
        assertNotEquals(
            SpawnBatchPlanHasher.settlementBatchKey(List.of(allocation)),
            SpawnBatchPlanHasher.dryRunSettlementBatchKey(List.of(allocation))
        );
        assertTrue(SpawnBatchPlanHasher.dryRunHouseholdBatchKey(allocation).startsWith("dryrun:household:"));
        assertTrue(SpawnBatchPlanHasher.dryRunSettlementBatchKey(List.of(allocation)).startsWith("dryrun:settlement:"));
    }

    private HouseAllocation allocation(String placeId, String familyId, String npcKey, String name) {
        return HouseAllocation.builder(placeId)
            .familyId(familyId)
            .primaryOwnerNpcKey(npcKey)
            .maxResidents(1)
            .addResident(HouseAllocation.ResidentPlan.builder(npcKey, name)
                .relationRole("resident")
                .occupation("locuitor")
                .spawnNodeId(placeId + ":spawn")
                .bedNodeId(placeId + ":bed")
                .build())
            .build();
    }
}
