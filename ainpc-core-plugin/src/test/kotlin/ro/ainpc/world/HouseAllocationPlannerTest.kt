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
    fun socialCapacityReservesDistinctPlacesAcrossSettlementPreview() {
        val worldAdmin = WorldAdminService({ }, Logger.getLogger("SocialCapacitySettlementTest"))
        worldAdmin.createDemoSettlement("demo_sat", "world", 0, 64, 0, -64, 320)
        for (placeId in listOf("piata", "taverna", "altar")) {
            worldAdmin.getPlaceModels("demo_sat").single { it.id == "demo_sat:$placeId" }
                .putMetadata("max_social_associations", "1")
        }
        worldAdmin.bindNpcToSocialPlace("demo_sat:piata", "npc_existing", "Existing")
        val before = worldAdmin.getPlaces("demo_sat").associate { it.id() to it.metadata() }

        val result = HouseAllocationPlanner().planSettlement(worldAdmin, "demo_sat", 0)

        assertTrue(result.success()) { result.errors().toString() }
        val assignments = result.allocations().flatMap { it.residentPlans() }.map { it.socialPlaceId() }
        assertEquals(2, assignments.count { it.isNotBlank() })
        assertEquals(2, assignments.count { it.isBlank() })
        assertEquals(2, assignments.filter { it.isNotBlank() }.toSet().size)
        assertTrue(result.warnings().any { it.contains("Capacitate sociala insuficienta") })
        assertEquals(before, worldAdmin.getPlaces("demo_sat").associate { it.id() to it.metadata() })
    }

    @Test
    fun invalidSocialCapacityStopsHousePlanning() {
        val worldAdmin = WorldAdminService({ }, Logger.getLogger("InvalidSocialCapacityTest"))
        worldAdmin.createDemoSettlement("demo_sat", "world", 0, 64, 0, -64, 320)
        worldAdmin.getPlaceModels("demo_sat").single { it.id == "demo_sat:piata" }
            .putMetadata("max_social_associations", "-1")

        val result = HouseAllocationPlanner().plan(worldAdmin, "demo_sat:house_1", 1)

        assertFalse(result.success())
        assertTrue(result.errors().any { it.contains("max_social_associations") })
    }

    @Test
    fun balancesNewSocialAssignmentsAgainstExistingMappingBindings() {
        val worldAdmin = WorldAdminService({ }, Logger.getLogger("ExistingSocialBindingsTest"))
        worldAdmin.createDemoSettlement("demo_sat", "world", 0, 64, 0, -64, 320)
        worldAdmin.bindNpcToSocialPlace("demo_sat:piata", "npc_existing", "Existing")
        val planner = HouseAllocationPlanner()
        val before = worldAdmin.getPlaces("demo_sat").associate { it.id() to it.metadata() }

        val first = planner.plan(worldAdmin, "demo_sat:house_1", 1)

        assertTrue(first.success()) { first.errors().toString() }
        assertEquals("demo_sat:taverna", first.allocation()!!.residentPlans().single().socialPlaceId())
        assertEquals(before, worldAdmin.getPlaces("demo_sat").associate { it.id() to it.metadata() })

        worldAdmin.bindNpcToSocialPlace("demo_sat:taverna", "npc_other", "Other")
        val second = planner.plan(worldAdmin, "demo_sat:house_1", 1)

        assertTrue(second.success()) { second.errors().toString() }
        assertEquals("demo_sat:altar", second.allocation()!!.residentPlans().single().socialPlaceId())
    }

    @Test
    fun duplicateSocialBindingIdsCountAsOneAssociation() {
        val worldAdmin = WorldAdminService({ }, Logger.getLogger("DuplicateSocialBindingsTest"))
        worldAdmin.createDemoSettlement("demo_sat", "world", 0, 64, 0, -64, 320)
        worldAdmin.bindNpcToSocialPlace("demo_sat:piata", "npc_market", "Market")
        worldAdmin.bindNpcToSocialPlace("demo_sat:taverna", "npc_tavern", "Tavern")
        worldAdmin.bindNpcToSocialPlace("demo_sat:altar", "npc_shrine", "Shrine")
        worldAdmin.getPlaceModels("demo_sat").first { it.id == "demo_sat:piata" }
            .putMetadata("social_npc_ids", "npc_market; NPC_MARKET, ,")

        val planning = HouseAllocationPlanner().plan(worldAdmin, "demo_sat:house_1", 1)

        assertTrue(planning.success()) { planning.errors().toString() }
        assertEquals("demo_sat:piata", planning.allocation()!!.residentPlans().single().socialPlaceId())
    }

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
        assertEquals("demo_sat:fierarie:workstation_1", resident.workNodeId())
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
            warning == "Regiunea demo_sat are 4 case disponibile; au fost planificate primele 2."
        }) {
            "warnings=${planning.warnings()}"
        }
    }

    @Test
    fun givesEveryDemoSettlementResidentDistinctWorkplacesAndSocialAnchors() {
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
                val expectedWorkplaces = mapOf(
                    "demo_sat:house_1" to "demo_sat:fierarie",
                    "demo_sat:house_2" to "demo_sat:ferma",
                    "demo_sat:house_3" to "demo_sat:piata",
                    "demo_sat:house_4" to "demo_sat:taverna",
                )
                assertEquals(expectedWorkplaces[allocation.placeId()], resident.workPlaceId())
                if (resident.workPlaceId() == "demo_sat:piata") {
                    assertTrue(resident.workNodeId().isBlank())
                    assertTrue(planning.warnings().any { it.contains("demo_sat:piata nu are node work/workstation") })
                } else {
                    assertTrue(resident.workNodeId().isNotBlank())
                }
                val expectedSocialPlaces = mapOf(
                    "demo_sat:house_1" to "demo_sat:piata",
                    "demo_sat:house_2" to "demo_sat:taverna",
                    "demo_sat:house_3" to "demo_sat:altar",
                    "demo_sat:house_4" to "demo_sat:piata",
                )
                assertEquals(expectedSocialPlaces[allocation.placeId()], resident.socialPlaceId())
                assertTrue(resident.socialNodeId().startsWith("${resident.socialPlaceId()}:"))
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

    @Test
    fun reservesConfiguredWorkplaceCapacityAcrossHousesWithoutMutatingMapping() {
        val worldAdmin = WorldAdminService({ }, Logger.getLogger("WorkplaceCapacityTest"))
        worldAdmin.createDemoSettlement("demo_sat", "world", 0, 64, 0, -64, 320)
        for (place in worldAdmin.getPlaceModels("demo_sat")) {
            place.putMetadata("max_workers", if (place.id == "demo_sat:fierarie") "2" else "0")
        }
        val before = worldAdmin.getPlaces("demo_sat").associate { it.id() to it.metadata() }
        val planner = HouseAllocationPlanner()

        val first = planner.planSettlement(worldAdmin, "demo_sat", 0)
        val second = planner.planSettlement(worldAdmin, "demo_sat", 0)

        assertTrue(first.success()) { first.errors().toString() }
        val residents = first.allocations().flatMap { it.residentPlans() }
        assertEquals(listOf("demo_sat:fierarie", "demo_sat:fierarie", "", ""), residents.map { it.workPlaceId() })
        assertTrue(residents.drop(2).all { it.occupation() == "locuitor" && it.workNodeId().isBlank() })
        assertTrue(first.warnings().any { it.contains("Capacitate workplace insuficienta") })
        assertEquals(residents.map { it.workPlaceId() }, second.allocations().flatMap { it.residentPlans() }.map { it.workPlaceId() })
        assertEquals(before, worldAdmin.getPlaces("demo_sat").associate { it.id() to it.metadata() })
        assertEquals("demo_sat:fierarie", planner.plan(worldAdmin, "demo_sat:house_4", 0).allocation()!!.residentPlans().single().workPlaceId())
    }

    @Test
    fun rejectsInvalidWorkplaceCapacityInsteadOfIgnoringIt() {
        for (capacity in listOf("-1", "many", "1.5", "2147483648", "")) {
            val worldAdmin = WorldAdminService({ }, Logger.getLogger("InvalidWorkplaceCapacityTest"))
            worldAdmin.createDemoSettlement("demo_sat", "world", 0, 64, 0, -64, 320)
            worldAdmin.getPlaceModels("demo_sat").first { it.id == "demo_sat:fierarie" }.putMetadata("max_workers", capacity)

            val planning = HouseAllocationPlanner().planSettlement(worldAdmin, "demo_sat", 0)

            assertFalse(planning.success()) { "capacity=$capacity" }
            assertTrue(planning.allocations().isEmpty())
            assertTrue(planning.errors().any { it.contains("max_workers invalid") })
        }
    }

    @Test
    fun populatedHouseIsNotReplannedAsNewResidents() {
        val worldAdmin = WorldAdminService({ }, Logger.getLogger("PopulatedHousePlanningTest"))
        worldAdmin.createDemoSettlement("demo_sat", "world", 0, 64, 0, -64, 320)
        worldAdmin.addNpcResidentToHomePlace("demo_sat:house_1", "npc_11", "Existing Resident")
        worldAdmin.bindNpcToWorkPlace("demo_sat:fierarie", "npc_11", "Existing Resident")
        val before = worldAdmin.getPlaces("demo_sat").associate { it.id() to it.metadata() }

        val planning = HouseAllocationPlanner().plan(worldAdmin, "demo_sat:house_1", 0)

        assertFalse(planning.success())
        assertTrue(planning.allocation() == null)
        assertTrue(planning.errors().any { it.contains("are deja rezidenti in mapping") })
        assertEquals(before, worldAdmin.getPlaces("demo_sat").associate { it.id() to it.metadata() })
    }

    @Test
    fun regionalLimitAppliesAfterSkippingPopulatedHouses() {
        val worldAdmin = WorldAdminService({ }, Logger.getLogger("IncrementalSettlementPlanningTest"))
        worldAdmin.createDemoSettlement("demo_sat", "world", 0, 64, 0, -64, 320)
        worldAdmin.addNpcResidentToHomePlace("demo_sat:house_1", "npc_11", "Existing Resident")
        worldAdmin.bindNpcToWorkPlace("demo_sat:fierarie", "npc_11", "Existing Resident")
        val before = worldAdmin.getPlaces("demo_sat").associate { it.id() to it.metadata() }

        val planning = HouseAllocationPlanner().planSettlement(worldAdmin, "demo_sat", 1)

        assertTrue(planning.success()) { planning.errors().toString() }
        assertEquals(listOf("demo_sat:house_2"), planning.allocations().map { it.placeId() })
        assertEquals("demo_sat:ferma", planning.allocations().single().residentPlans().single().workPlaceId())
        assertTrue(planning.warnings().any { it.contains("house_1") && it.contains("omisa") })
        assertEquals(before, worldAdmin.getPlaces("demo_sat").associate { it.id() to it.metadata() })
    }

    @Test
    fun fullyPopulatedRegionReturnsClearFailureWithoutNewAllocations() {
        val worldAdmin = WorldAdminService({ }, Logger.getLogger("FullSettlementPlanningTest"))
        worldAdmin.createDemoSettlement("demo_sat", "world", 0, 64, 0, -64, 320)
        for (index in 1..4) worldAdmin.addNpcResidentToHomePlace("demo_sat:house_$index", "npc_$index", "Resident $index")

        val planning = HouseAllocationPlanner().planSettlement(worldAdmin, "demo_sat", 0)

        assertFalse(planning.success())
        assertTrue(planning.allocations().isEmpty())
        assertTrue(planning.errors().any { it.contains("nu are case fara rezidenti") })
        assertEquals(4, planning.warnings().count { it.contains("omisa") })
    }

    @Test
    fun blankResidentListDoesNotMarkHouseOccupied() {
        val worldAdmin = WorldAdminService({ }, Logger.getLogger("EmptyResidentsPlanningTest"))
        worldAdmin.createDemoSettlement("demo_sat", "world", 0, 64, 0, -64, 320)
        worldAdmin.getPlaceModels("demo_sat").first { it.id == "demo_sat:house_1" }.putMetadata("resident_npc_ids", " , ; ")
        assertTrue(HouseAllocationPlanner().plan(worldAdmin, "demo_sat:house_1", 0).success())
    }

    @Test
    fun limitsHugeCapacityToTheActualPopulation() {
        val worldAdmin = WorldAdminService({ }, Logger.getLogger("LargeWorkplaceCapacityTest"))
        worldAdmin.createDemoSettlement("demo_sat", "world", 0, 64, 0, -64, 320)
        worldAdmin.getPlaceModels("demo_sat").first { it.id == "demo_sat:fierarie" }.putMetadata("max_workers", Int.MAX_VALUE.toString())

        val planning = HouseAllocationPlanner().planSettlement(worldAdmin, "demo_sat", 2)

        assertTrue(planning.success())
        assertEquals(listOf("demo_sat:fierarie", "demo_sat:fierarie"), planning.allocations().flatMap { it.residentPlans() }.map { it.workPlaceId() })
    }

    @Test
    fun existingWorkersAndRegionalReservationsShareTheSameCapacity() {
        val worldAdmin = WorldAdminService({ }, Logger.getLogger("ExistingWorkplaceOccupancyTest"))
        worldAdmin.createDemoSettlement("demo_sat", "world", 0, 64, 0, -64, 320)
        for (place in worldAdmin.getPlaceModels("demo_sat")) {
            place.putMetadata("max_workers", if (place.id == "demo_sat:fierarie") "3" else "0")
        }
        worldAdmin.bindNpcToWorkPlace("demo_sat:fierarie", "npc_11", "Worker One")
        worldAdmin.bindNpcToWorkPlace("demo_sat:fierarie", "npc_12", "Worker Two")
        val before = worldAdmin.getPlaces("demo_sat").associate { it.id() to it.metadata() }
        val planner = HouseAllocationPlanner()

        val first = planner.planSettlement(worldAdmin, "demo_sat", 0)
        val second = planner.planSettlement(worldAdmin, "demo_sat", 0)
        assertTrue(first.success()) { first.errors().toString() }
        val residents = first.allocations().flatMap { it.residentPlans() }
        assertEquals(listOf("demo_sat:fierarie", "", "", ""), residents.map { it.workPlaceId() })
        assertTrue(residents.drop(1).all { it.occupation() == "locuitor" && it.workNodeId().isBlank() })
        assertEquals(residents.map { it.workPlaceId() }, second.allocations().flatMap { it.residentPlans() }.map { it.workPlaceId() })
        assertEquals(before, worldAdmin.getPlaces("demo_sat").associate { it.id() to it.metadata() })
        assertEquals("demo_sat:fierarie", planner.plan(worldAdmin, "demo_sat:house_4", 0).allocation()!!.residentPlans().single().workPlaceId())
    }

    @Test
    fun singleHouseSkipsFullWorkplaceAndChoosesNextAvailableOne() {
        val worldAdmin = WorldAdminService({ }, Logger.getLogger("OccupiedForgePlanningTest"))
        worldAdmin.createDemoSettlement("demo_sat", "world", 0, 64, 0, -64, 320)
        worldAdmin.bindNpcToWorkPlace("demo_sat:fierarie", "npc_11", "Existing Worker")

        val planning = HouseAllocationPlanner().plan(worldAdmin, "demo_sat:house_1", 0)

        assertTrue(planning.success()) { planning.errors().toString() }
        assertEquals("demo_sat:ferma", planning.allocation()!!.residentPlans().single().workPlaceId())
        assertTrue(planning.warnings().any { it.contains("1 angajati existenti") && it.contains("demo_sat:fierarie") })
    }

    @Test
    fun duplicateMappingWorkerIdsDoNotConsumeMultiplePositions() {
        val worldAdmin = WorldAdminService({ }, Logger.getLogger("DuplicateWorkerPlanningTest"))
        worldAdmin.createDemoSettlement("demo_sat", "world", 0, 64, 0, -64, 320)
        for (place in worldAdmin.getPlaceModels("demo_sat")) {
            place.putMetadata("max_workers", if (place.id == "demo_sat:fierarie") "2" else "0")
            if (place.id == "demo_sat:fierarie") place.putMetadata("worker_npc_ids", " NPC_11; npc_11, ; NPC_11 ")
        }

        val planning = HouseAllocationPlanner().planSettlement(worldAdmin, "demo_sat", 0)

        assertTrue(planning.success())
        assertEquals(1, planning.allocations().flatMap { it.residentPlans() }.count { it.workPlaceId() == "demo_sat:fierarie" })
    }

    @Test
    fun capacityReducedBelowExistingOccupancyDoesNotAllocateOrRemoveWorkers() {
        val worldAdmin = WorldAdminService({ }, Logger.getLogger("OverOccupiedWorkplacePlanningTest"))
        worldAdmin.createDemoSettlement("demo_sat", "world", 0, 64, 0, -64, 320)
        for (place in worldAdmin.getPlaceModels("demo_sat")) {
            place.putMetadata("max_workers", "0")
            if (place.id == "demo_sat:fierarie") place.putMetadata("worker_npc_ids", "npc_11,npc_12")
        }
        val before = worldAdmin.getPlaces("demo_sat").associate { it.id() to it.metadata() }

        val planning = HouseAllocationPlanner().planSettlement(worldAdmin, "demo_sat", 0)

        assertTrue(planning.success())
        assertTrue(planning.allocations().flatMap { it.residentPlans() }.all { it.workPlaceId().isBlank() })
        assertTrue(planning.warnings().any { it.contains("Capacitate workplace insuficienta") })
        assertEquals(before, worldAdmin.getPlaces("demo_sat").associate { it.id() to it.metadata() })
    }
}
