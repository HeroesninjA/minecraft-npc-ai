package ro.ainpc.spawn;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HouseAllocationTest {

    @Test
    void toNpcSpawnPlansCarriesHouseFamilyAndResidentNodes() {
        HouseAllocation allocation = HouseAllocation.builder("sat_01:casa_popescu")
            .familyId("family_popescu_001")
            .primaryOwnerNpcKey("npc_ion")
            .maxResidents(3)
            .addResident(HouseAllocation.ResidentPlan.builder("npc_ion", "Ion")
                .relationRole("father")
                .occupation("fierar")
                .age(42)
                .gender("male")
                .spawnNodeId("sat_01:casa_popescu:spawn_ion")
                .bedNodeId("sat_01:casa_popescu:bed_ion")
                .workPlaceId("sat_01:fierarie")
                .workNodeId("sat_01:fierarie:anvil")
                .build())
            .build();

        List<NpcSpawnPlan> plans = allocation.toNpcSpawnPlans();

        assertEquals(1, plans.size());
        NpcSpawnPlan plan = plans.getFirst();
        assertEquals("npc_ion", plan.npcKey());
        assertEquals("Ion", plan.name());
        assertEquals("fierar", plan.occupation());
        assertEquals(42, plan.age());
        assertEquals("sat_01:casa_popescu", plan.homePlaceId());
        assertEquals("sat_01:casa_popescu:spawn_ion", plan.spawnNodeId());
        assertEquals("sat_01:casa_popescu:bed_ion", plan.homeNodeId());
        assertEquals("sat_01:fierarie", plan.workPlaceId());
        assertEquals("sat_01:fierarie:anvil", plan.workNodeId());
        assertEquals("family_popescu_001", plan.familyId());
    }

    @Test
    void toPlaceMetadataExportsTemporaryHouseholdMetadata() {
        HouseAllocation allocation = HouseAllocation.builder("sat_01:casa_popescu")
            .familyId("family_popescu_001")
            .maxResidents(3)
            .addResident(resident("npc_ion", "Ion", "spawn_ion", "bed_ion"))
            .addResident(resident("npc_maria", "Maria", "spawn_maria", "bed_maria"))
            .build();

        Map<String, String> metadata = allocation.toPlaceMetadata();

        assertEquals("home", metadata.get("role"));
        assertEquals("family_popescu_001", metadata.get("family_id"));
        assertEquals("3", metadata.get("max_residents"));
        assertEquals("npc_ion,npc_maria", metadata.get("residents"));
    }

    private HouseAllocation.ResidentPlan resident(String npcKey, String name, String spawnNodeId, String bedNodeId) {
        return HouseAllocation.ResidentPlan.builder(npcKey, name)
            .relationRole("relative")
            .occupation("locuitor")
            .spawnNodeId(spawnNodeId)
            .bedNodeId(bedNodeId)
            .build();
    }
}
