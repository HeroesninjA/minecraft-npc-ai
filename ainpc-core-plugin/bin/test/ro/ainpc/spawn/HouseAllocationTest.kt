package ro.ainpc.spawn

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HouseAllocationTest {
    @Test
    fun toNpcSpawnPlansCarriesHouseFamilyAndResidentNodes() {
        val allocation = HouseAllocation.builder("sat_01:casa_popescu")
            .familyId("family_popescu_001")
            .primaryOwnerNpcKey("npc_ion")
            .maxResidents(3)
            .addResident(
                HouseAllocation.ResidentPlan.builder("npc_ion", "Ion")
                    .relationRole("father")
                    .occupation("fierar")
                    .age(42)
                    .gender("male")
                    .spawnNodeId("sat_01:casa_popescu:spawn_ion")
                    .bedNodeId("sat_01:casa_popescu:bed_ion")
                    .workPlaceId("sat_01:fierarie")
                    .workNodeId("sat_01:fierarie:anvil")
                    .build()
            )
            .build()

        val plans = allocation.toNpcSpawnPlans()

        assertEquals(1, plans.size)
        val plan = plans.first()
        assertEquals("npc_ion", plan.npcKey())
        assertEquals("Ion", plan.name())
        assertEquals("fierar", plan.occupation())
        assertEquals(42, plan.age())
        assertEquals("sat_01:casa_popescu", plan.homePlaceId())
        assertEquals("sat_01:casa_popescu:spawn_ion", plan.spawnNodeId())
        assertEquals("sat_01:casa_popescu:bed_ion", plan.homeNodeId())
        assertEquals("sat_01:fierarie", plan.workPlaceId())
        assertEquals("sat_01:fierarie:anvil", plan.workNodeId())
        assertEquals("family_popescu_001", plan.familyId())
        assertEquals("spawn_plan:family_popescu_001:npc_ion", plan.sourceKey())
    }

    @Test
    fun sourceKeyFallsBackToHomePlaceAndSanitizesResidentKey() {
        val plan = NpcSpawnPlan.builder("NPC Ion Senior", "Ion")
            .homePlaceId("Sat 01/Casa Popescu")
            .spawnNodeId("spawn_ion")
            .build()

        assertEquals("spawn_plan:sat_01_casa_popescu:npc_ion_senior", plan.sourceKey())
    }

    @Test
    fun toPlaceMetadataExportsTemporaryHouseholdMetadata() {
        val allocation = HouseAllocation.builder("sat_01:casa_popescu")
            .familyId("family_popescu_001")
            .maxResidents(3)
            .addResident(resident("npc_ion", "Ion", "spawn_ion", "bed_ion"))
            .addResident(resident("npc_maria", "Maria", "spawn_maria", "bed_maria"))
            .build()

        val metadata = allocation.toPlaceMetadata()

        assertEquals("home", metadata["role"])
        assertEquals("family_popescu_001", metadata["family_id"])
        assertEquals("3", metadata["max_residents"])
        assertEquals("npc_ion,npc_maria", metadata["residents"])
    }

    private fun resident(npcKey: String, name: String, spawnNodeId: String, bedNodeId: String): HouseAllocation.ResidentPlan {
        return HouseAllocation.ResidentPlan.builder(npcKey, name)
            .relationRole("relative")
            .occupation("locuitor")
            .spawnNodeId(spawnNodeId)
            .bedNodeId(bedNodeId)
            .build()
    }
}
