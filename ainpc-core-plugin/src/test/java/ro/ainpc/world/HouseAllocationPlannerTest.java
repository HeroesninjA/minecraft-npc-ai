package ro.ainpc.world;

import org.junit.jupiter.api.Test;
import ro.ainpc.spawn.HouseAllocation;
import ro.ainpc.spawn.HouseAllocationPlanner;
import ro.ainpc.spawn.HouseAllocationValidationResult;
import ro.ainpc.spawn.HouseAllocationValidator;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HouseAllocationPlannerTest {

    @Test
    void plansValidHouseholdFromDemoMapping() {
        WorldAdminService worldAdmin = new WorldAdminService(message -> { }, Logger.getLogger("HouseAllocationPlannerTest"));
        worldAdmin.createDemoSettlement("demo_sat", "world", 0, 64, 0, -64, 320);

        HouseAllocationPlanner.PlanningResult planning =
            new HouseAllocationPlanner().plan(worldAdmin, "demo_sat:house_1", 0);

        assertTrue(planning.success(), () -> "errors=" + planning.errors());
        HouseAllocation allocation = planning.allocation();
        assertEquals("demo_sat:house_1", allocation.placeId());
        assertEquals(1, allocation.residentPlans().size());

        HouseAllocation.ResidentPlan resident = allocation.residentPlans().getFirst();
        assertEquals("demo_sat:house_1:npc_spawn_1", resident.spawnNodeId());
        assertEquals("demo_sat:house_1:bed_1", resident.homeNodeId());
        assertEquals("demo_sat:fierarie", resident.workPlaceId());
        assertEquals("demo_sat:fierarie:work_1", resident.workNodeId());
        assertEquals("demo_sat:piata", resident.socialPlaceId());
        assertEquals("demo_sat:piata:meeting_point_1", resident.socialNodeId());

        HouseAllocationValidationResult validation =
            new HouseAllocationValidator().validate(allocation, worldAdmin);
        assertTrue(validation.valid(), () -> "errors=" + validation.errors() + " warnings=" + validation.warnings());
    }

    @Test
    void plansSettlementFromAllDemoHouses() {
        WorldAdminService worldAdmin = new WorldAdminService(message -> { }, Logger.getLogger("HouseAllocationPlannerSettlementTest"));
        worldAdmin.createDemoSettlement("demo_sat", "world", 0, 64, 0, -64, 320);

        HouseAllocationPlanner.SettlementPlanningResult planning =
            new HouseAllocationPlanner().planSettlement(worldAdmin, "demo_sat", 0);

        assertTrue(planning.success(), () -> "errors=" + planning.errors());
        assertEquals("demo_sat", planning.regionId());
        assertEquals(4, planning.allocations().size());
        assertEquals(4, planning.allocations().stream().mapToInt(allocation -> allocation.residentPlans().size()).sum());

        for (HouseAllocation allocation : planning.allocations()) {
            HouseAllocationValidationResult validation =
                new HouseAllocationValidator().validate(allocation, worldAdmin);
            assertTrue(validation.valid(), () -> allocation.placeId()
                + " errors=" + validation.errors()
                + " warnings=" + validation.warnings());
        }
    }
}
