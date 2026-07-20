package ro.ainpc.spawn

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PopulationPlanConversionTest {
    @Test
    fun conversionPreservesNarrativeFieldsThroughSpawnPlan() {
        val resident = ResidentNarrativePlan(
            npcKey = "npc_mara",
            displayName = "Mara Popescu",
            familyName = "Popescu",
            relationRole = "owner",
            socialRole = "artisan",
            profession = "fierar",
            ageGroup = "adult",
            age = 34,
            gender = "female",
            homePlaceId = "sat_01:casa_popescu",
            bedNodeId = "sat_01:casa_popescu:bed_mara",
            spawnNodeId = "sat_01:casa_popescu:spawn_mara",
            workPlaceId = "sat_01:fierarie",
            workNodeId = "sat_01:fierarie:anvil",
            socialPlaceId = "sat_01:piata",
            socialNodeId = "sat_01:piata:gather",
            routineProfile = "day_worker",
            questRole = "giver",
            personalitySeed = "steady_artisan",
            backstorySeed = "mara_forge_01"
        )
        val populationPlan = PopulationPlan(
            planId = "sat_01_population_01",
            regionId = "sat_01",
            themeId = "medieval",
            seed = "sat_01:narrative:01",
            targetPopulation = 1,
            households = listOf(
                HouseholdPlan(
                    householdKey = "household_popescu",
                    familyId = "family_popescu",
                    homePlaceId = resident.homePlaceId,
                    capacity = 2,
                    primaryOwnerNpcKey = resident.npcKey,
                    residents = listOf(resident),
                    familyType = "single"
                )
            ),
            unassignedWorkplaces = emptyList(),
            warnings = emptyList()
        )

        val allocationResident = populationPlan.toHouseAllocations().single().residentPlans().single()

        assertEquals("artisan", allocationResident.socialRole())
        assertEquals("adult", allocationResident.ageGroup())
        assertEquals("day_worker", allocationResident.routineProfile())
        assertEquals("giver", allocationResident.questRole())
        assertEquals("mara_forge_01", allocationResident.backstorySeed())

        val spawnPlan = allocationResident.toNpcSpawnPlan(resident.homePlaceId, "family_popescu")
        assertEquals("owner", spawnPlan.relationRole())
        assertEquals("artisan", spawnPlan.socialRole())
        assertEquals("adult", spawnPlan.ageGroup())
        assertEquals("day_worker", spawnPlan.routineProfile())
        assertEquals("giver", spawnPlan.questRole())
        assertEquals("mara_forge_01", spawnPlan.backstorySeed())
    }
}
