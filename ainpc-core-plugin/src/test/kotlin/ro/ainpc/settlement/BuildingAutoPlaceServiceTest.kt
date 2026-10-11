package ro.ainpc.settlement

import java.util.logging.Logger
import java.nio.file.Path
import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ro.ainpc.api.settlement.BuildingAnchorDefinition
import ro.ainpc.api.settlement.BuildingTemplateDefinition
import ro.ainpc.world.RegionType
import ro.ainpc.world.WorldAdminService
import ro.ainpc.platform.PlatformProfile
import ro.ainpc.spawn.HouseAllocationPlanner
import ro.ainpc.spawn.HouseAllocationValidator
import ro.ainpc.story.StoryStructureSignalResolver
import ro.ainpc.story.StructureStoryEventPlanner

class BuildingAutoPlaceServiceTest {
    @Test
    fun settlementPlanSpreadsWorkstationsAcrossDifferentHouses() {
        val service = BuildingAutoPlaceService(worldAdmin)
        assertTrue(service.autoPlace("house_small", "test_region").success())
        assertTrue(service.autoPlace("house_small", "test_region").success())
        assertTrue(service.autoPlace("workshop_shared", "test_region").success())

        val first = HouseAllocationPlanner().planSettlement(worldAdmin, "test_region", 0)
        val second = HouseAllocationPlanner().planSettlement(worldAdmin, "test_region", 0)
        assertTrue(first.success(), first.errors().toString())
        val workNodes = first.allocations().flatMap { it.residentPlans() }.map { it.workNodeId() }
        assertEquals(setOf("test_region:workshop_shared_3:bench_west", "test_region:workshop_shared_3:bench_east"), workNodes.toSet())
        assertEquals(workNodes, second.allocations().flatMap { it.residentPlans() }.map { it.workNodeId() })
    }

    @Test
    fun sharedWorkshopAssignsTwoFamilyWorkersToDistinctStationsAndCapsBindings() {
        val service = BuildingAutoPlaceService(worldAdmin)
        assertTrue(service.autoPlace("house_family", "test_region").success())
        val placement = service.autoPlace("workshop_shared", "test_region")
        assertTrue(placement.success(), placement.errors.toString())
        val workshop = requireNotNull(worldAdmin.getPlace("test_region:workshop_shared_2"))
        val planning = HouseAllocationPlanner().plan(worldAdmin, "test_region:house_family_1", 2)
        assertTrue(planning.success(), planning.errors().toString())
        val residents = requireNotNull(planning.allocation()).residentPlans()
        assertEquals(2, residents.size)
        assertTrue(residents.all { it.workPlaceId() == workshop.id() && it.occupation() == "tamplar" })
        assertEquals(setOf("${workshop.id()}:bench_west", "${workshop.id()}:bench_east"), residents.map { it.workNodeId() }.toSet())
        assertTrue(StoryStructureSignalResolver().collectPlaceStructureSignals(workshop).contains("structure_category" to "profession"))

        worldAdmin.bindNpcToWorkPlace(workshop.id(), "npc_1", "Primul")
        worldAdmin.bindNpcToWorkPlace(workshop.id(), "npc_2", "Al doilea")
        val workers = requireNotNull(worldAdmin.getPlace(workshop.id())).metadata()["worker_npc_ids"]
        assertThrows(IllegalArgumentException::class.java) {
            worldAdmin.bindNpcToWorkPlace(workshop.id(), "npc_3", "Al treilea")
        }
        assertEquals(workers, requireNotNull(worldAdmin.getPlace(workshop.id())).metadata()["worker_npc_ids"])
    }

    @Test
    fun settlementPlanSpreadsSocialNodesAcrossDifferentHouses() {
        val service = BuildingAutoPlaceService(worldAdmin)
        assertTrue(service.autoPlace("house_small", "test_region").success())
        assertTrue(service.autoPlace("house_small", "test_region").success())
        assertTrue(service.autoPlace("community_hall", "test_region").success())

        val first = HouseAllocationPlanner().planSettlement(worldAdmin, "test_region", 0)
        val second = HouseAllocationPlanner().planSettlement(worldAdmin, "test_region", 0)
        assertTrue(first.success(), first.errors().toString())
        val socialNodes = first.allocations().flatMap { it.residentPlans() }.map { it.socialNodeId() }
        assertEquals(setOf("test_region:community_hall_3:gathering_west", "test_region:community_hall_3:gathering_east"), socialNodes.toSet())
        assertEquals(socialNodes, second.allocations().flatMap { it.residentPlans() }.map { it.socialNodeId() })
    }

    @Test
    fun communityHallAssociatesTwoFamilyResidentsWithoutCreatingWorkplaces() {
        val service = BuildingAutoPlaceService(worldAdmin)
        assertTrue(service.autoPlace("house_family", "test_region").success())
        val placement = service.autoPlace("community_hall", "test_region")
        assertTrue(placement.success(), placement.errors.toString())
        val hall = requireNotNull(worldAdmin.getPlace("test_region:community_hall_2"))
        val definition = requireNotNull(BuildingTemplateRegistry.get("community_hall"))
        val geometry = ro.ainpc.worldgen.ActivityBuildingGeometry().generate(definition)
        val socialNodes = listOf("gathering_west", "gathering_east").map { anchorId ->
            val mapped = requireNotNull(worldAdmin.getNode("${hall.id()}:$anchorId"))
            val physical = requireNotNull(geometry.getAnchor(anchorId, hall.minX(), hall.minY(), hall.minZ()))
            assertEquals(mapped.x(), physical.x)
            assertEquals(mapped.y(), physical.y)
            assertEquals(mapped.z(), physical.z)
            mapped
        }

        val planning = HouseAllocationPlanner().plan(worldAdmin, "test_region:house_family_1", 2)
        assertTrue(planning.success(), planning.errors().toString())
        val residents = requireNotNull(planning.allocation()).residentPlans()
        assertEquals(2, residents.size)
        assertTrue(residents.all { it.socialPlaceId() == hall.id() })
        assertEquals(socialNodes.map { it.id() }.toSet(), residents.map { it.socialNodeId() }.toSet())
        assertTrue(residents.all { it.workPlaceId().isBlank() })
        val signals = StoryStructureSignalResolver().collectPlaceStructureSignals(hall)
        assertTrue(signals.contains("structure_category" to "public"))
        assertTrue(signals.contains("quest_hook" to "meeting_notice"))
        assertEquals("structure_visit_public", StructureStoryEventPlanner().planPlaceVisit(
            "test_region", worldAdmin.getPlaceModels("test_region").single { it.id == hall.id() }, null)?.eventType)

        worldAdmin.bindNpcToSocialPlace(hall.id(), "npc_1", "Primul")
        worldAdmin.bindNpcToSocialPlace(hall.id(), "npc_2", "Al doilea")
        val associations = requireNotNull(worldAdmin.getPlace(hall.id())).metadata()["social_npc_ids"]
        assertThrows(IllegalArgumentException::class.java) {
            worldAdmin.bindNpcToSocialPlace(hall.id(), "npc_3", "Al treilea")
        }
        assertEquals(associations, requireNotNull(worldAdmin.getPlace(hall.id())).metadata()["social_npc_ids"])
    }

    @Test
    fun generatedFamilyHouseProvidesTwoDistinctPhysicalResidentBindings() {
        val placement = BuildingAutoPlaceService(worldAdmin).autoPlace("house_family", "test_region")
        assertTrue(placement.success(), placement.errors.toString())
        val house = requireNotNull(worldAdmin.getPlace("test_region:house_family_1"))
        val definition = requireNotNull(BuildingTemplateRegistry.get("house_family"))
        val geometry = ro.ainpc.worldgen.ActivityBuildingGeometry().generate(definition)
        for (anchor in definition.anchors) {
            val mapped = requireNotNull(worldAdmin.getNode("${house.id()}:${anchor.anchorId}"))
            val physical = requireNotNull(geometry.getAnchor(anchor.anchorId, house.minX(), house.minY(), house.minZ()))
            assertEquals(mapped.x(), physical.x)
            assertEquals(mapped.y(), physical.y)
            assertEquals(mapped.z(), physical.z)
        }

        val planning = HouseAllocationPlanner().plan(worldAdmin, house.id(), 2)
        assertTrue(planning.success(), planning.errors().toString())
        val allocation = requireNotNull(planning.allocation())
        assertEquals("family_house_family_1", allocation.familyId())
        assertEquals(listOf("${house.id()}:bed_01", "${house.id()}:bed_02"), allocation.residentPlans().map { it.homeNodeId() })
        assertEquals(listOf("${house.id()}:spawn_01", "${house.id()}:spawn_02"), allocation.residentPlans().map { it.spawnNodeId() })
    }

    @Test
    fun generatedHouseConnectsPhysicalSleepingAndSpawnAnchorsToResidentPlan() {
        val placement = BuildingAutoPlaceService(worldAdmin).autoPlace("house_small", "test_region")
        assertTrue(placement.success(), placement.errors.toString())
        val house = requireNotNull(worldAdmin.getPlace("test_region:house_small_1"))
        val definition = requireNotNull(BuildingTemplateRegistry.get("house_small"))
        val geometry = ro.ainpc.worldgen.ActivityBuildingGeometry().generate(definition)
        for (anchorId in listOf("bed", "spawn", "entrance")) {
            val mapped = requireNotNull(worldAdmin.getNode("${house.id()}:$anchorId"))
            val physical = requireNotNull(geometry.getAnchor(anchorId, house.minX(), house.minY(), house.minZ()))
            assertEquals(mapped.x(), physical.x)
            assertEquals(mapped.y(), physical.y)
            assertEquals(mapped.z(), physical.z)
        }

        val allocation = HouseAllocationPlanner().plan(worldAdmin, house.id(), 1)
        assertTrue(allocation.success(), allocation.errors().toString())
        val resident = requireNotNull(allocation.allocation()).residentPlans().single()
        assertEquals(house.id(), allocation.allocation()!!.placeId())
        assertEquals("${house.id()}:bed", resident.homeNodeId())
        assertEquals("${house.id()}:spawn", resident.spawnNodeId())
    }

    @Test
    fun generatedFarmProvidesPhysicalWorkAnchorForNpcPlan() {
        val service = BuildingAutoPlaceService(worldAdmin)
        assertTrue(service.autoPlace("house_small", "test_region").success())
        val placement = service.autoPlace("farm", "test_region")
        assertTrue(placement.success(), placement.errors.toString())
        val farm = requireNotNull(worldAdmin.getPlace("test_region:farm_2"))
        val definition = requireNotNull(BuildingTemplateRegistry.get("farm"))
        val geometry = ro.ainpc.worldgen.ActivityBuildingGeometry().generate(definition)
        val mappedStation = requireNotNull(worldAdmin.getNode("${farm.id()}:workstation"))
        val physicalStation = requireNotNull(geometry.getAnchor("workstation", farm.minX(), farm.minY(), farm.minZ()))
        assertEquals(mappedStation.x(), physicalStation.x)
        assertEquals(mappedStation.y(), physicalStation.y)
        assertEquals(mappedStation.z(), physicalStation.z)

        val allocation = HouseAllocationPlanner().plan(worldAdmin, "test_region:house_small_1", 1)
        assertTrue(allocation.success(), allocation.errors().toString())
        val resident = requireNotNull(allocation.allocation()).residentPlans().single()
        assertEquals(farm.id(), resident.workPlaceId())
        assertEquals(mappedStation.id(), resident.workNodeId())
    }

    @Test
    fun rejectsProgrammaticInvalidSocialCapacityBeforeWritingMapping() {
        BuildingTemplateRegistry.register(BuildingTemplateDefinition(
            templateId = "invalid_social_capacity",
            placeType = "custom",
            tags = setOf("social"),
            metadata = mapOf("max_social_associations" to "many"),
        ))

        val result = BuildingAutoPlaceService(worldAdmin).autoPlace("invalid_social_capacity", "test_region")

        assertFalse(result.success())
        assertTrue(result.errors.any { it.contains("max_social_associations") })
        assertTrue(worldAdmin.getPlaceModels("test_region").isEmpty())
    }

    @Test
    fun generatedLibraryConnectsWorkAndSocialAnchorsToNpcAndStory() {
        val service = BuildingAutoPlaceService(worldAdmin)
        assertTrue(service.autoPlace("house_small", "test_region").success())
        val placement = service.autoPlace("library", "test_region")
        assertTrue(placement.success(), placement.errors.toString())
        val library = requireNotNull(worldAdmin.getPlace("test_region:library_2"))
        val definition = requireNotNull(BuildingTemplateRegistry.get("library"))
        val geometry = ro.ainpc.worldgen.ActivityBuildingGeometry().generate(definition)
        for (anchorId in listOf("desk", "reading")) {
            val mapped = requireNotNull(worldAdmin.getNode("${library.id()}:$anchorId"))
            val physical = requireNotNull(geometry.getAnchor(anchorId, library.minX(), library.minY(), library.minZ()))
            assertEquals(mapped.x(), physical.x)
            assertEquals(mapped.y(), physical.y)
            assertEquals(mapped.z(), physical.z)
        }

        val allocation = HouseAllocationPlanner().plan(worldAdmin, "test_region:house_small_1", 1)
        assertTrue(allocation.success(), allocation.errors().toString())
        val resident = requireNotNull(allocation.allocation()).residentPlans().single()
        assertEquals("lucrator", resident.occupation())
        assertEquals(library.id(), resident.workPlaceId())
        assertEquals("${library.id()}:desk", resident.workNodeId())
        assertEquals(library.id(), resident.socialPlaceId())
        assertEquals("${library.id()}:reading", resident.socialNodeId())
        val storySignals = StoryStructureSignalResolver().collectPlaceStructureSignals(library)
        assertTrue(storySignals.contains("structure_category" to "profession"))
        assertTrue(storySignals.contains("quest_hook" to "lost_book"))
        val placeModel = worldAdmin.getPlaceModels("test_region").single { it.id == library.id() }
        assertEquals("structure_visit_profession",
            StructureStoryEventPlanner().planPlaceVisit("test_region", placeModel, null)?.eventType)
        val readingNode = worldAdmin.getNodeModelsForPlace(library.id()).single { it.id.endsWith(":reading") }
        assertEquals("structure_node_social",
            StructureStoryEventPlanner().planNodeInteraction("test_region", placeModel, readingNode)?.eventType)
    }

    @Test
    fun generatedBakeryConnectsWorkplaceGeometryProfessionAndStory() {
        val service = BuildingAutoPlaceService(worldAdmin)
        assertTrue(service.autoPlace("house_small", "test_region").success())
        val placement = service.autoPlace("bakery", "test_region")
        assertTrue(placement.success(), placement.errors.toString())
        val bakery = requireNotNull(worldAdmin.getPlace("test_region:bakery_2"))
        val definition = requireNotNull(BuildingTemplateRegistry.get("bakery"))
        val geometry = ro.ainpc.worldgen.ActivityBuildingGeometry().generate(definition)
        val mappedOven = requireNotNull(worldAdmin.getNode("${bakery.id()}:oven"))
        val physicalOven = requireNotNull(geometry.getAnchor("oven", bakery.minX(), bakery.minY(), bakery.minZ()))
        assertEquals(mappedOven.x(), physicalOven.x)
        assertEquals(mappedOven.y(), physicalOven.y)
        assertEquals(mappedOven.z(), physicalOven.z)

        val allocation = HouseAllocationPlanner().plan(worldAdmin, "test_region:house_small_1", 1)
        assertTrue(allocation.success(), allocation.errors().toString())
        val resident = requireNotNull(allocation.allocation()).residentPlans().single()
        assertEquals("brutar", resident.occupation())
        assertEquals(bakery.id(), resident.workPlaceId())
        assertEquals(mappedOven.id(), resident.workNodeId())
        val storySignals = StoryStructureSignalResolver().collectPlaceStructureSignals(bakery)
        assertTrue(storySignals.contains("structure_category" to "profession"))
        assertTrue(storySignals.contains("quest_hook" to "bread_order"))
        assertEquals("structure_visit_profession",
            StructureStoryEventPlanner().planPlaceVisit("test_region", worldAdmin.getPlaceModels("test_region")
                .single { it.id == bakery.id() }, null)?.eventType)
    }

    @Test
    fun generatedWorkshopConnectsWorkplaceGeometryProfessionAndStory() {
        val service = BuildingAutoPlaceService(worldAdmin)
        assertTrue(service.autoPlace("house_small", "test_region").success())
        val placement = service.autoPlace("workshop", "test_region")
        assertTrue(placement.success(), placement.errors.toString())
        val workshop = requireNotNull(worldAdmin.getPlace("test_region:workshop_2"))
        val definition = requireNotNull(BuildingTemplateRegistry.get("workshop"))
        val geometry = ro.ainpc.worldgen.ActivityBuildingGeometry().generate(definition)
        val mappedBench = requireNotNull(worldAdmin.getNode("${workshop.id()}:bench"))
        val physicalBench = requireNotNull(geometry.getAnchor("bench", workshop.minX(), workshop.minY(), workshop.minZ()))
        assertEquals(mappedBench.x(), physicalBench.x)
        assertEquals(mappedBench.y(), physicalBench.y)
        assertEquals(mappedBench.z(), physicalBench.z)

        val allocation = HouseAllocationPlanner().plan(worldAdmin, "test_region:house_small_1", 1)
        assertTrue(allocation.success(), allocation.errors().toString())
        val resident = requireNotNull(allocation.allocation()).residentPlans().single()
        assertEquals("tamplar", resident.occupation())
        assertEquals(workshop.id(), resident.workPlaceId())
        assertEquals(mappedBench.id(), resident.workNodeId())
        val storySignals = StoryStructureSignalResolver().collectPlaceStructureSignals(workshop)
        assertTrue(storySignals.contains("structure_category" to "profession"))
        assertTrue(storySignals.contains("quest_hook" to "craft_order"))
        assertEquals("structure_visit_profession",
            StructureStoryEventPlanner().planPlaceVisit("test_region", worldAdmin.getPlaceModels("test_region")
                .single { it.id == workshop.id() }, null)?.eventType)
    }

    @Test
    fun programmaticCustomWorkplaceKeepsConfiguredStoryCategory() {
        BuildingTemplateRegistry.register(BuildingTemplateDefinition(
            templateId = "custom_workshop",
            placeType = "custom",
            tags = setOf("work"),
            metadata = mapOf("max_workers" to "1", "story_category" to "profession",
                "quest_hooks" to "repair_tool, metal_supply"),
            anchors = listOf(BuildingAnchorDefinition("bench", "workstation", 1, 1, 1)),
        ))

        val placement = BuildingAutoPlaceService(worldAdmin).autoPlace("custom_workshop", "test_region")

        assertTrue(placement.success(), placement.errors.toString())
        val place = worldAdmin.getPlaceModels("test_region").single()
        assertEquals("profession", StoryStructureSignalResolver().collectPlaceStructureSignals(worldAdmin.getPlace(place.id))
            .single { it.first == "structure_category" }.second)
        assertEquals("structure_visit_profession",
            StructureStoryEventPlanner().planPlaceVisit("test_region", place, null)?.eventType)
        assertEquals(listOf("repair_tool", "metal_supply"),
            StoryStructureSignalResolver().collectPlaceStructureSignals(worldAdmin.getPlace(place.id))
                .filter { it.first == "quest_hook" }.map { it.second })
    }

    @Test
    fun rejectsProgrammaticInvalidStoryCategoryBeforeWritingMapping() {
        BuildingTemplateRegistry.register(BuildingTemplateDefinition(
            templateId = "invalid_story_category",
            placeType = "custom",
            metadata = mapOf("story_category" to "unknown"),
        ))

        val result = BuildingAutoPlaceService(worldAdmin).autoPlace("invalid_story_category", "test_region")

        assertFalse(result.success())
        assertTrue(result.errors.any { it.contains("story_category") })
        assertTrue(worldAdmin.getPlaceModels("test_region").isEmpty())
    }

    @Test
    fun rejectsProgrammaticInvalidQuestHooksBeforeWritingMapping() {
        BuildingTemplateRegistry.register(BuildingTemplateDefinition(
            templateId = "invalid_quest_hooks",
            placeType = "custom",
            metadata = mapOf("quest_hooks" to "repair_tool,"),
        ))

        val result = BuildingAutoPlaceService(worldAdmin).autoPlace("invalid_quest_hooks", "test_region")

        assertFalse(result.success())
        assertTrue(result.errors.any { it.contains("quest_hooks") })
        assertTrue(worldAdmin.getPlaceModels("test_region").isEmpty())
    }

    @Test
    fun generatedGardenProducesLeisureStoryContextAndVisitEvents() {
        val placement = BuildingAutoPlaceService(worldAdmin).autoPlace("leisure_garden", "test_region")
        assertTrue(placement.success(), placement.errors.toString())
        val garden = worldAdmin.getPlaceModels("test_region").single()
        val signals = StoryStructureSignalResolver().collectPlaceStructureSignals(worldAdmin.getPlace(garden.id))
        val planner = StructureStoryEventPlanner()
        val visit = requireNotNull(planner.planPlaceVisit("test_region", garden, null))
        val bench = worldAdmin.getNodeModelsForPlace(garden.id).single { it.id.endsWith(":benches") }
        val interaction = requireNotNull(planner.planNodeInteraction("test_region", garden, bench))

        assertTrue(signals.contains("structure_type" to "custom"))
        assertTrue(signals.contains("structure_category" to "leisure"))
        assertTrue(signals.contains("quest_hook" to "community_event"))
        assertEquals("structure_visit_leisure", visit.eventType)
        assertEquals("leisure", visit.payload["structure_category"])
        assertEquals("structure_node_social", interaction.eventType)
        assertEquals("leisure", interaction.payload["structure_category"])
    }

    @Test
    fun rejectsProgrammaticHouseCapacityWithoutDistinctNodes() {
        BuildingTemplateRegistry.register(
            BuildingTemplateDefinition(
                templateId = "overcrowded_house",
                placeType = "house",
                metadata = mapOf("max_residents" to "2"),
                anchors = listOf(
                    BuildingAnchorDefinition("spawn", "npc_spawn", 1, 1, 1),
                    BuildingAnchorDefinition("bed", "bed", 2, 1, 1),
                ),
            ),
        )

        val result = BuildingAutoPlaceService(worldAdmin).autoPlace("overcrowded_house", "test_region")

        assertFalse(result.success())
        assertTrue(result.errors.any { it.contains("max_residents=2") && it.contains("distincte") })
        assertTrue(worldAdmin.getPlaceModels("test_region").isEmpty())
    }

    @Test
    fun rejectsProgrammaticHouseWithInvalidResidentCapacity() {
        BuildingTemplateRegistry.register(
            BuildingTemplateDefinition(
                templateId = "invalid_capacity_house",
                placeType = "house",
                metadata = mapOf("max_residents" to "0"),
            ),
        )

        val result = BuildingAutoPlaceService(worldAdmin).autoPlace("invalid_capacity_house", "test_region")

        assertFalse(result.success())
        assertTrue(result.errors.any { it.contains("max_residents invalid") })
        assertTrue(worldAdmin.getPlaceModels("test_region").isEmpty())
    }

    @Test
    fun rejectsProgrammaticWorkTemplateWithoutConfiguredCapacity() {
        BuildingTemplateRegistry.register(
            BuildingTemplateDefinition(
                templateId = "unconfigured_forge",
                placeType = "forge",
                tags = setOf("work"),
                anchors = listOf(BuildingAnchorDefinition("anvil", "workstation", 1, 1, 1)),
            ),
        )

        val result = BuildingAutoPlaceService(worldAdmin).autoPlace("unconfigured_forge", "test_region")

        assertFalse(result.success())
        assertTrue(result.errors.any { it.contains("max_workers") })
        assertTrue(worldAdmin.getPlaceModels("test_region").isEmpty())
    }

    @Test
    fun rejectsProgrammaticWorkTemplateWithoutWorkAnchor() {
        BuildingTemplateRegistry.register(
            BuildingTemplateDefinition(
                templateId = "anchorless_forge",
                placeType = "forge",
                tags = setOf("work"),
                metadata = mapOf("max_workers" to "1"),
                anchors = listOf(BuildingAnchorDefinition("bench", "social", 1, 1, 1)),
            ),
        )

        val result = BuildingAutoPlaceService(worldAdmin).autoPlace("anchorless_forge", "test_region")

        assertFalse(result.success())
        assertTrue(result.errors.any { it.contains("ancora de lucru") })
        assertTrue(worldAdmin.getPlaceModels("test_region").isEmpty())
    }

    @Test
    fun rejectsProgrammaticTemplateWithAnchorOutsideFootprintWithoutWritingMapping() {
        BuildingTemplateRegistry.register(
            BuildingTemplateDefinition(
                templateId = "invalid_shop",
                footprintWidth = 5,
                anchors = listOf(BuildingAnchorDefinition("counter", "workstation", 6, 1, 1)),
            ),
        )

        val result = BuildingAutoPlaceService(worldAdmin).autoPlace("invalid_shop", "test_region")

        assertFalse(result.success())
        assertTrue(result.errors.any { it.contains("counter") && it.contains("footprint") })
        assertTrue(worldAdmin.getPlaceModels("test_region").isEmpty())
    }

    @Test
    fun rejectsProgrammaticTemplateWithNonFiniteAnchorRadius() {
        BuildingTemplateRegistry.register(
            BuildingTemplateDefinition(
                templateId = "invalid_radius",
                anchors = listOf(BuildingAnchorDefinition("counter", "workstation", 1, 1, 1, Double.NaN)),
            ),
        )

        val result = BuildingAutoPlaceService(worldAdmin).autoPlace("invalid_radius", "test_region")

        assertFalse(result.success())
        assertTrue(result.errors.any { it.contains("counter") && it.contains("raza invalida") })
        assertTrue(worldAdmin.getPlaceModels("test_region").isEmpty())
    }

    @Test
    fun choosesUnusedBuildingIdAfterEarlierBuildingIsRemoved() {
        val service = BuildingAutoPlaceService(worldAdmin)
        val first = service.autoPlace("shop", "test_region")
        val second = service.autoPlace("shop", "test_region")
        assertTrue(first.success(), first.errors.toString())
        assertTrue(second.success(), second.errors.toString())
        assertTrue(worldAdmin.removePlace("test_region:shop_1"))

        val third = service.autoPlace("shop", "test_region")

        assertTrue(third.success(), third.errors.toString())
        assertTrue(third.placed.contains("test_region:shop_3"))
        assertTrue(worldAdmin.getPlace("test_region:shop_2") != null)
        assertTrue(worldAdmin.getPlace("test_region:shop_3") != null)
    }

    private lateinit var worldAdmin: WorldAdminService
    @TempDir
    lateinit var directory: Path

    @BeforeEach
    fun setUp() {
        BuildingTemplateRegistry.clear()
        worldAdmin = WorldAdminService({ }, Logger.getLogger("BuildingAutoPlaceServiceTest"))
        worldAdmin.createRegion(
            "test_region", "Test Region", "world", RegionType.SETTLEMENT,
            -100, 50, -100, 100, 90, 100,
        )
    }

    @AfterEach
    fun tearDown() {
        BuildingTemplateRegistry.clear()
    }

    @Test
    fun rollsBackPlaceAndNodesWhenAnAnchorFails() {
        val duplicateAnchor = BuildingAnchorDefinition("duplicate", "home", 1, 1, 1)
        BuildingTemplateRegistry.register(
            BuildingTemplateDefinition(
                templateId = "rollback_template",
                displayName = "Rollback Template",
                anchors = listOf(duplicateAnchor, duplicateAnchor),
            ),
        )

        val result = BuildingAutoPlaceService(worldAdmin).autoPlace("rollback_template", "test_region")

        assertFalse(result.success())
        assertTrue(result.compensated)
        assertTrue(result.placed.isEmpty())
        assertTrue(result.errors.any { it.contains("duplicate") })
        assertTrue(result.warnings.any { it.startsWith("Compensare mapping reusita") })
        assertNull(worldAdmin.getPlace("test_region:rollback_template_1"))
        assertNull(worldAdmin.getNode("test_region:rollback_template_1:duplicate"))
    }

    @Test
    fun configuredTemplatesCarryPopulationOccupationAndStoryIntoMappingAndPlanner() {
        val file = directory.resolve("building_templates.yml").toFile()
        file.writeText("""
            profiles:
              house_small:
                display_name: Casa configurata
                place_type: house
                footprint: {width: 9, depth: 8, height: 4}
                tags: [custom_home]
                metadata: {max_residents: 1, story_state: peaceful}
                anchors:
                  spawn:
                    node_type: npc_spawn
                    offset: {x: 2, y: 1, z: 2}
                    radius: 0.5
                    role: resident
                    tags: [custom_spawn]
                    metadata: {semantic: spawn_anchor, quest_hook: household_welcome}
                  bed:
                    node_type: bed
                    offset: {x: 3, y: 1, z: 3}
              forge:
                place_type: forge
                footprint: {width: 8, depth: 8, height: 4}
                metadata: {profession: blacksmith, story_state: shortage, quest_hook: supply_delivery}
                anchors:
                  workstation:
                    node_type: workstation
                    offset: {x: 2, y: 1, z: 2}
              square:
                place_type: market
                footprint: {width: 8, depth: 8, height: 4}
                anchors:
                  social:
                    node_type: social
                    offset: {x: 2, y: 1, z: 2}
        """.trimIndent())
        BuildingTemplateRegistry.reload(file)
        val service = BuildingAutoPlaceService(worldAdmin)

        for (templateId in listOf("house_small", "forge", "square")) {
            val result = service.autoPlace(templateId, "test_region")
            assertTrue(result.success(), result.errors.toString())
            assertTrue(result.placed.isNotEmpty())
        }

        val house = requireNotNull(worldAdmin.getPlace("test_region:house_small_1"))
        assertEquals(3, worldAdmin.getPlaceModels("test_region").size)
        assertEquals(2, worldAdmin.getNodesForPlace(house.id()).size)
        assertEquals(9, house.maxX() - house.minX())
        assertTrue(house.tags().contains("custom_home"))
        assertEquals("1", house.metadata()["max_residents"])
        val spawn = requireNotNull(worldAdmin.getNode("test_region:house_small_1:spawn"))
        assertEquals(0.5, spawn.radius())
        assertEquals("spawn_anchor", spawn.metadata()["semantic"])
        assertEquals("custom_spawn", spawn.metadata()["tags"])
        assertEquals("resident", spawn.metadata()["anchor_role"])

        val planning = HouseAllocationPlanner().plan(worldAdmin, house.id(), 1)
        assertTrue(planning.success(), planning.errors().toString())
        val resident = requireNotNull(planning.allocation()).residentPlans().single()
        assertEquals("blacksmith", resident.occupation())
        assertEquals("test_region:forge_2", resident.workPlaceId())
        assertEquals("test_region:square_3", resident.socialPlaceId())
        val forge = requireNotNull(worldAdmin.getPlace(resident.workPlaceId()))
        assertTrue(StoryStructureSignalResolver().collectPlaceMetadataSignals(forge.metadata())
            .contains("place_story_state" to "shortage"))

        val saved = YamlConfiguration()
        worldAdmin.saveToConfig(saved)
        val restored = WorldAdminService({ }, Logger.getLogger("BuildingAutoPlaceServiceTest"))
        restored.reloadFromConfig(saved, PlatformProfile.fromConfig(saved))
        assertEquals(forge.metadata(), restored.getPlace(forge.id())?.metadata())
        assertEquals(spawn.metadata(), restored.getNode(spawn.id())?.metadata())
        val restoredSpawn = requireNotNull(restored.getNode(spawn.id()))
        assertTrue(StoryStructureSignalResolver().isStoryRelevantNode(restoredSpawn))
        val houseModel = restored.getPlaceModels("test_region").single { it.id == house.id() }
        val spawnModel = restored.getNodeModelsForPlace(house.id()).single { it.id == spawn.id() }
        val storyEvent = requireNotNull(StructureStoryEventPlanner().planNodeInteraction("test_region", houseModel, spawnModel))
        assertEquals("household_welcome", storyEvent.eventKey)
        assertEquals("household_welcome", storyEvent.payload["node_quest_hook"])
    }

    @Test
    fun configuredWorkAnchorOverridesCloserSpawnAndSurvivesMappingReload() {
        val templateFile = directory.resolve("work_anchor_templates.yml").toFile()
        templateFile.writeText("""
            profiles:
              house_small:
                place_type: house
                footprint: {width: 8, depth: 8, height: 4}
                metadata: {max_residents: 1}
                anchors:
                  spawn:
                    node_type: npc_spawn
                    offset: {x: 2, y: 1, z: 2}
                  bed:
                    node_type: bed
                    offset: {x: 3, y: 1, z: 3}
              forge:
                place_type: forge
                footprint: {width: 8, depth: 8, height: 4}
                tags: [work]
                metadata: {max_workers: 1, profession: blacksmith}
                anchors:
                  arrival:
                    node_type: npc_spawn
                    offset: {x: 4, y: 1, z: 4}
                  assigned_station:
                    node_type: entrance
                    offset: {x: 7, y: 1, z: 7}
                    metadata: {semantic: work_anchor}
        """.trimIndent())
        BuildingTemplateRegistry.reload(templateFile)
        val service = BuildingAutoPlaceService(worldAdmin)
        for (templateId in listOf("house_small", "forge")) {
            val placement = service.autoPlace(templateId, "test_region")
            assertTrue(placement.success(), placement.errors.toString())
        }

        val forge = requireNotNull(worldAdmin.getPlace("test_region:forge_2"))
        val arrival = requireNotNull(worldAdmin.getNode("${forge.id()}:arrival"))
        val workAnchor = requireNotNull(worldAdmin.getNode("${forge.id()}:assigned_station"))
        assertEquals((forge.minX() + forge.maxX()) / 2.0, arrival.x())
        assertEquals((forge.minZ() + forge.maxZ()) / 2.0, arrival.z())
        assertEquals(forge.minX() + 7.0, workAnchor.x())
        assertEquals(forge.minZ() + 7.0, workAnchor.z())
        assertEquals("entrance", workAnchor.typeId())
        assertEquals("work_anchor", workAnchor.metadata()["semantic"])

        val saved = YamlConfiguration()
        worldAdmin.saveToConfig(saved)
        val mappingFile = directory.resolve("work_anchor_mapping.yml").toFile()
        saved.save(mappingFile)
        val restoredConfig = YamlConfiguration.loadConfiguration(mappingFile)
        val restored = WorldAdminService({ }, Logger.getLogger("WorkAnchorReloadTest"))
        restored.reloadFromConfig(restoredConfig, PlatformProfile.fromConfig(restoredConfig))

        for (mapping in listOf(worldAdmin, restored)) {
            val planning = HouseAllocationPlanner().plan(mapping, "test_region:house_small_1", 1)
            assertTrue(planning.success(), planning.errors().toString())
            val allocation = requireNotNull(planning.allocation())
            val resident = allocation.residentPlans().single()
            assertEquals("blacksmith", resident.occupation())
            assertEquals(forge.id(), resident.workPlaceId())
            assertEquals(workAnchor.id(), resident.workNodeId())
            val mappedAnchor = requireNotNull(mapping.getNode(resident.workNodeId()))
            assertEquals(workAnchor.x(), mappedAnchor.x())
            assertEquals(workAnchor.y(), mappedAnchor.y())
            assertEquals(workAnchor.z(), mappedAnchor.z())
            assertEquals(workAnchor.metadata(), mappedAnchor.metadata())
            val validation = HouseAllocationValidator().validate(allocation, mapping)
            assertTrue(validation.valid(), validation.errors().toString())
        }
    }

    @Test
    fun rejectsUnsupportedRotationBeforeCreatingMapping() {
        BuildingTemplateRegistry.register(BuildingTemplateDefinition("rotated", supportedRotations = listOf(90)))

        val result = BuildingAutoPlaceService(worldAdmin).autoPlace("rotated", "test_region")

        assertFalse(result.success())
        assertTrue(result.errors.any { it.contains("rotatia 0") })
        assertTrue(worldAdmin.getPlaceModels("test_region").isEmpty())
    }

    @Test
    fun settlementDistributesSocialAssignmentsAcrossMarketTavernAndGarden() {
        val service = BuildingAutoPlaceService(worldAdmin)
        repeat(6) { assertTrue(service.autoPlace("house_small", "test_region").success()) }
        for (templateId in listOf("market", "tavern", "leisure_garden")) {
            assertTrue(service.autoPlace(templateId, "test_region").success())
        }
        val planner = HouseAllocationPlanner()
        val first = planner.planSettlement(worldAdmin, "test_region", 0)
        val second = planner.planSettlement(worldAdmin, "test_region", 0)
        assertTrue(first.success(), first.errors().toString())
        val residents = first.allocations().flatMap { it.residentPlans() }
        val socialPlaces = residents.map { it.socialPlaceId() }
        assertEquals(listOf("test_region:market_7", "test_region:tavern_8", "test_region:leisure_garden_9",
            "test_region:market_7", "test_region:tavern_8", "test_region:leisure_garden_9"), socialPlaces)
        assertEquals(socialPlaces, second.allocations().flatMap { it.residentPlans() }.map { it.socialPlaceId() })
        assertEquals(setOf(2), socialPlaces.groupingBy { it }.eachCount().values.toSet())
        assertTrue(residents.all { it.socialNodeId().startsWith("${it.socialPlaceId()}:") })
        assertTrue(residents.none { it.workPlaceId() == "test_region:leisure_garden_9" })
        assertTrue(worldAdmin.getPlaces("test_region").all { it.metadata()["social_npc_ids"] == null })
        val subset = planner.planSettlement(worldAdmin, "test_region", 2)
        assertEquals(socialPlaces.take(2), subset.allocations().flatMap { it.residentPlans() }.map { it.socialPlaceId() })
    }

    @Test
    fun bundledActivityBuildingsProvideWorkTradeAndSocialAnchors() {
        val service = BuildingAutoPlaceService(worldAdmin)
        for (templateId in listOf("house_small", "forge", "farm", "shop", "tavern", "market", "leisure_garden")) {
            val result = service.autoPlace(templateId, "test_region")
            assertTrue(result.success(), "$templateId: ${result.errors}")
        }
        for (workplaceId in listOf("forge_2", "farm_3", "shop_4", "tavern_5", "market_6")) {
            assertTrue(worldAdmin.getNodesForPlace("test_region:$workplaceId").any { it.typeId() == "workstation" })
        }
        for (commercialId in listOf("shop_4", "market_6")) {
            assertTrue(worldAdmin.getNodesForPlace("test_region:$commercialId").any {
                it.typeId() == "interaction" && it.metadata()["anchor_role"] == "trade"
            })
        }
        val planning = HouseAllocationPlanner().plan(worldAdmin, "test_region:house_small_1", 1)
        assertTrue(planning.success(), planning.errors().toString())
        val resident = planning.allocation()!!.residentPlans().single()
        assertEquals("test_region:forge_2", resident.workPlaceId())
        assertEquals("test_region:market_6", resident.socialPlaceId())
        assertEquals("test_region:market_6:square", resident.socialNodeId())
        val garden = worldAdmin.getPlace("test_region:leisure_garden_7")!!
        assertTrue(ro.ainpc.spawn.SpawnSemanticRules.isSocialPlace(garden))
        assertFalse(ro.ainpc.spawn.SpawnSemanticRules.isWorkplace(garden))
    }

    @Test
    fun shopAndTavernCanBeSelectedAsTheOnlyWorkplace() {
        for (templateId in listOf("shop", "tavern", "leisure_garden")) {
            val isolated = WorldAdminService({ }, Logger.getLogger("ActivityTemplateTest"))
            isolated.createRegion("activities", "Activities", "world", RegionType.SETTLEMENT, -100, 50, -100, 100, 90, 100)
            val service = BuildingAutoPlaceService(isolated)
            assertTrue(service.autoPlace("house_small", "activities").success())
            assertTrue(service.autoPlace(templateId, "activities").success())
            val planning = HouseAllocationPlanner().plan(isolated, "activities:house_small_1", 1)
            assertTrue(planning.success(), planning.errors().toString())
            val resident = planning.allocation()!!.residentPlans().single()
            if (templateId == "leisure_garden") {
                assertEquals("", resident.workPlaceId())
            } else {
                assertEquals("activities:${templateId}_2", resident.workPlaceId())
                assertEquals("activities:${templateId}_2:counter", resident.workNodeId())
            }
            assertEquals("activities:${templateId}_2", resident.socialPlaceId())
        }
    }
}
