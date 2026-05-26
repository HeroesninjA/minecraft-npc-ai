package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.spawn.HouseAllocationPlanner
import ro.ainpc.spawn.HouseAllocationValidator
import java.util.logging.Logger

class HouseAllocationPlannerTest {
    @Test
    fun plansValidHouseholdFromDemoMapping() {
        val worldAdmin = WorldAdminService({ }, Logger.getLogger("HouseAllocationPlannerTest"))
        worldAdmin.createDemoSettlement("demo_sat", "world", 0, 64, 0, -64, 320)

        val planning = HouseAllocationPlanner().plan(worldAdmin, "demo_sat:house_1", 0)

        assertTrue(planning.success()) { "errors=${planning.errors()}" }
        val allocation = planning.allocation()!!
        assertEquals("demo_sat:house_1", allocation.placeId())
        assertEquals(1, allocation.residentPlans().size)

        val resident = allocation.residentPlans().first()
        assertEquals("demo_sat:house_1:npc_spawn_1", resident.spawnNodeId())
        assertEquals("demo_sat:house_1:bed_1", resident.homeNodeId())
        assertEquals("demo_sat:fierarie", resident.workPlaceId())
        assertEquals("demo_sat:fierarie:work_1", resident.workNodeId())
        assertEquals("demo_sat:piata", resident.socialPlaceId())
        assertEquals("demo_sat:piata:meeting_point_1", resident.socialNodeId())

        val validation = HouseAllocationValidator().validate(allocation, worldAdmin)
        assertTrue(validation.valid()) { "errors=${validation.errors()} warnings=${validation.warnings()}" }
    }

    @Test
    fun plansSettlementFromAllDemoHouses() {
        val worldAdmin = WorldAdminService({ }, Logger.getLogger("HouseAllocationPlannerSettlementTest"))
        worldAdmin.createDemoSettlement("demo_sat", "world", 0, 64, 0, -64, 320)

        val planning = HouseAllocationPlanner().planSettlement(worldAdmin, "demo_sat", 0)

        assertTrue(planning.success()) { "errors=${planning.errors()}" }
        assertEquals("demo_sat", planning.regionId())
        assertEquals(4, planning.allocations().size)
        assertEquals(4, planning.allocations().sumOf { allocation -> allocation.residentPlans().size })

        for (allocation in planning.allocations()) {
            val validation = HouseAllocationValidator().validate(allocation, worldAdmin)
            assertTrue(validation.valid()) {
                "${allocation.placeId()} errors=${validation.errors()} warnings=${validation.warnings()}"
            }
        }
    }

    @Test
    fun capsRequestedResidentsToDemoHouseNodeCapacity() {
        val worldAdmin = WorldAdminService({ }, Logger.getLogger("HouseAllocationPlannerCapacityTest"))
        worldAdmin.createDemoSettlement("demo_sat", "world", 0, 64, 0, -64, 320)

        val planning = HouseAllocationPlanner().plan(worldAdmin, "demo_sat:house_1", 4)

        assertTrue(planning.success()) { "errors=${planning.errors()}" }
        assertEquals(2, planning.allocation()!!.maxResidents())
        assertEquals(1, planning.allocation()!!.residentPlans().size)
        assertTrue(planning.warnings().any { warning -> warning.contains("maxResidents=2") }) {
            "warnings=${planning.warnings()}"
        }
        assertTrue(planning.warnings().any { warning -> warning.contains("doar 1 perechi spawn/home") }) {
            "warnings=${planning.warnings()}"
        }
    }

    @Test
    fun plansSettlementSubsetWithClearLimitWarning() {
        val worldAdmin = WorldAdminService({ }, Logger.getLogger("HouseAllocationPlannerSubsetTest"))
        worldAdmin.createDemoSettlement("demo_sat", "world", 0, 64, 0, -64, 320)

        val planning = HouseAllocationPlanner().planSettlement(worldAdmin, "demo_sat", 2)

        assertTrue(planning.success()) { "errors=${planning.errors()}" }
        assertEquals("demo_sat", planning.regionId())
        assertEquals(listOf("demo_sat:house_1", "demo_sat:house_2"), planning.allocations().map { it.placeId() })
        assertEquals(2, planning.allocations().sumOf { it.residentPlans().size })
        assertTrue(planning.warnings().any { warning ->
            warning == "Regiunea demo_sat are 4 case; au fost planificate primele 2."
        }) {
            "warnings=${planning.warnings()}"
        }
    }

    @Test
    fun givesEveryDemoSettlementResidentHomeWorkAndSocialAnchors() {
        val worldAdmin = WorldAdminService({ }, Logger.getLogger("HouseAllocationPlannerAnchorsTest"))
        worldAdmin.createDemoSettlement("demo_sat", "world", 0, 64, 0, -64, 320)

        val planning = HouseAllocationPlanner().planSettlement(worldAdmin, "demo_sat", 0)

        assertTrue(planning.success()) { "errors=${planning.errors()}" }
        for (allocation in planning.allocations()) {
            assertTrue(allocation.primaryOwnerNpcKey().isNotBlank()) { allocation.placeId() }
            assertEquals(listOf("resident"), allocation.residentPlans().map { it.relationRole() }.distinct())
            for (resident in allocation.residentPlans()) {
                assertTrue(resident.npcKey().startsWith(allocation.placeId().replace(':', '_'))) { resident.npcKey() }
                assertTrue(resident.spawnNodeId().startsWith("${allocation.placeId()}:npc_spawn_")) { resident.spawnNodeId() }
                assertTrue(resident.homeNodeId().startsWith("${allocation.placeId()}:bed_")) { resident.homeNodeId() }
                assertEquals("demo_sat:fierarie", resident.workPlaceId())
                assertEquals("demo_sat:fierarie:work_1", resident.workNodeId())
                assertEquals("demo_sat:piata", resident.socialPlaceId())
                assertEquals("demo_sat:piata:meeting_point_1", resident.socialNodeId())
                assertTrue(resident.occupation().isNotBlank()) { resident.npcKey() }
                assertTrue(resident.archetype().isNotBlank()) { resident.npcKey() }
            }
        }
    }

    @Test
    fun reportsInvalidSettlementSelectorsWithoutAllocations() {
        val worldAdmin = WorldAdminService({ }, Logger.getLogger("HouseAllocationPlannerInvalidSelectorTest"))
        worldAdmin.createDemoSettlement("demo_sat", "world", 0, 64, 0, -64, 320)

        val missingRegion = HouseAllocationPlanner().planSettlement(worldAdmin, "missing_region", 0)
        val missingHouse = HouseAllocationPlanner().plan(worldAdmin, "demo_sat:missing_house", 0)

        assertFalse(missingRegion.success())
        assertEquals("", missingRegion.regionId())
        assertTrue(missingRegion.allocations().isEmpty())
        assertTrue(missingRegion.errors().any { it == "Regiunea missing_region nu a fost gasita." }) {
            "errors=${missingRegion.errors()}"
        }

        assertFalse(missingHouse.success())
        assertTrue(missingHouse.allocation() == null)
        assertTrue(missingHouse.errors().any { it == "Casa/place-ul demo_sat:missing_house nu a fost gasit." }) {
            "errors=${missingHouse.errors()}"
        }
    }
}
