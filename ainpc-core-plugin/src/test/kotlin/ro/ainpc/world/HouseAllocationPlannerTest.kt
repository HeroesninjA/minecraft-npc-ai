package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
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
}
