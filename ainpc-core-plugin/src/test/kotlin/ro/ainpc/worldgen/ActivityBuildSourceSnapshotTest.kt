package ro.ainpc.worldgen

import java.util.logging.Logger
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ro.ainpc.settlement.BuildingAutoPlaceService
import ro.ainpc.settlement.BuildingTemplateRegistry
import ro.ainpc.world.RegionType
import ro.ainpc.world.WorldAdminService

class ActivityBuildSourceSnapshotTest {
    private lateinit var worldAdmin: WorldAdminService

    @BeforeEach
    fun prepare() {
        BuildingTemplateRegistry.clear()
        BuildingTemplateRegistry.loadDefaults()
        worldAdmin = WorldAdminService({ }, Logger.getLogger("ActivityBuildSourceSnapshotTest"))
        worldAdmin.createRegion("test", "Test", "world", RegionType.SETTLEMENT, -50, 60, -50, 50, 85, 50)
        assertTrue(BuildingAutoPlaceService(worldAdmin).autoPlace("shop", "test").success())
    }

    @AfterEach
    fun cleanup() = BuildingTemplateRegistry.clear()

    @Test
    fun unchangedMappingProducesEqualSnapshotsAndMetadataChangesInvalidateThem() {
        val definition = BuildingTemplateRegistry.get("shop")!!
        val snapshot = ActivityBuildSourceSnapshot.capture(worldAdmin, definition, "test:shop_1")
        assertEquals(snapshot, ActivityBuildSourceSnapshot.capture(worldAdmin, definition, "test:shop_1"))
        worldAdmin.getPlaceModels("test").single().putMetadata("max_workers", "2")
        assertNotEquals(snapshot, ActivityBuildSourceSnapshot.capture(worldAdmin, definition, "test:shop_1"))
    }

    @Test
    fun deletedPlacesAndNodesCannotBeConfirmed() {
        val definition = BuildingTemplateRegistry.get("shop")!!
        worldAdmin.removeNode("test:shop_1:counter")
        assertThrows(IllegalArgumentException::class.java) { ActivityBuildSourceSnapshot.capture(worldAdmin, definition, "test:shop_1") }
        worldAdmin.removePlace("test:shop_1")
        assertThrows(IllegalArgumentException::class.java) { ActivityBuildSourceSnapshot.capture(worldAdmin, definition, "test:shop_1") }
    }

    @Test
    fun changedAnchorGeometryAndFootprintAreRejected() {
        val definition = BuildingTemplateRegistry.get("shop")!!
        assertThrows(IllegalArgumentException::class.java) {
            ActivityBuildSourceSnapshot.capture(worldAdmin, definition.copy(footprintWidth = 9), "test:shop_1")
        }
        assertThrows(IllegalArgumentException::class.java) {
            ActivityBuildSourceSnapshot.capture(worldAdmin, definition.copy(anchors = definition.anchors.map { it.copy(radius = it.radius + 1) }), "test:shop_1")
        }
    }

    @Test
    fun snapshotCopiesMutableTemplateMetadata() {
        val metadata = mutableMapOf("max_workers" to "1")
        val definition = BuildingTemplateRegistry.get("shop")!!.copy(metadata = metadata)
        val snapshot = ActivityBuildSourceSnapshot.capture(worldAdmin, definition, "test:shop_1")
        metadata["max_workers"] = "2"
        assertNotEquals(snapshot, ActivityBuildSourceSnapshot.capture(worldAdmin, definition, "test:shop_1"))
    }

    @Test
    fun gardenGeometryPreservesMappedSocialAnchorWithoutWorkplace() {
        assertTrue(BuildingAutoPlaceService(worldAdmin).autoPlace("leisure_garden", "test").success())
        val definition = BuildingTemplateRegistry.get("leisure_garden")!!
        val place = worldAdmin.getPlace("test:leisure_garden_2")!!
        ActivityBuildSourceSnapshot.capture(worldAdmin, definition, place.id())
        val geometry = ActivityBuildingGeometry().generate(definition)
        val mapped = worldAdmin.getNode("${place.id()}:benches")!!
        val physical = geometry.getAnchor("benches", place.minX(), place.minY(), place.minZ())!!
        assertEquals(mapped.x(), physical.x)
        assertEquals(mapped.y(), physical.y)
        assertEquals(mapped.z(), physical.z)
        assertTrue(ro.ainpc.spawn.SpawnSemanticRules.isSocialPlace(place))
        assertTrue(!ro.ainpc.spawn.SpawnSemanticRules.isWorkplace(place))
    }

    @Test
    fun communityHallGeometryPreservesMappedSocialAnchorWithoutWorkplace() {
        assertTrue(BuildingAutoPlaceService(worldAdmin).autoPlace("community_hall", "test").success())
        val definition = BuildingTemplateRegistry.get("community_hall")!!
        val place = worldAdmin.getPlace("test:community_hall_2")!!
        ActivityBuildSourceSnapshot.capture(worldAdmin, definition, place.id())
        val geometry = ActivityBuildingGeometry().generate(definition)
        for (anchor in definition.anchors) {
            val mapped = worldAdmin.getNode("${place.id()}:${anchor.anchorId}")!!
            val physical = geometry.getAnchor(anchor.anchorId, place.minX(), place.minY(), place.minZ())!!
            assertEquals(mapped.x(), physical.x)
            assertEquals(mapped.y(), physical.y)
            assertEquals(mapped.z(), physical.z)
        }
        assertTrue(ro.ainpc.spawn.SpawnSemanticRules.isSocialPlace(place))
        assertTrue(!ro.ainpc.spawn.SpawnSemanticRules.isWorkplace(place))
    }

    @Test
    fun farmGeometryMatchesMappedWorkEntranceAndSpawnAnchors() {
        assertTrue(BuildingAutoPlaceService(worldAdmin).autoPlace("farm", "test").success())
        val definition = BuildingTemplateRegistry.get("farm")!!
        val place = worldAdmin.getPlace("test:farm_2")!!
        ActivityBuildSourceSnapshot.capture(worldAdmin, definition, place.id())
        val geometry = ActivityBuildingGeometry().generate(definition)
        for (anchorId in listOf("workstation", "entrance", "spawn")) {
            val mapped = worldAdmin.getNode("${place.id()}:$anchorId")!!
            val physical = geometry.getAnchor(anchorId, place.minX(), place.minY(), place.minZ())!!
            assertEquals(mapped.x(), physical.x)
            assertEquals(mapped.y(), physical.y)
            assertEquals(mapped.z(), physical.z)
        }
    }

    @Test
    fun houseGeometryMatchesMappedSleepingEntranceAndSpawnAnchors() {
        assertTrue(BuildingAutoPlaceService(worldAdmin).autoPlace("house_small", "test").success())
        val definition = BuildingTemplateRegistry.get("house_small")!!
        val place = worldAdmin.getPlace("test:house_small_2")!!
        ActivityBuildSourceSnapshot.capture(worldAdmin, definition, place.id())
        val geometry = ActivityBuildingGeometry().generate(definition)
        for (anchorId in listOf("bed", "entrance", "spawn")) {
            val mapped = worldAdmin.getNode("${place.id()}:$anchorId")!!
            val physical = geometry.getAnchor(anchorId, place.minX(), place.minY(), place.minZ())!!
            assertEquals(mapped.x(), physical.x)
            assertEquals(mapped.y(), physical.y)
            assertEquals(mapped.z(), physical.z)
        }
    }

    @Test
    fun familyHouseGeometryMatchesEveryMappedAnchor() {
        assertTrue(BuildingAutoPlaceService(worldAdmin).autoPlace("house_family", "test").success())
        val definition = BuildingTemplateRegistry.get("house_family")!!
        val place = worldAdmin.getPlace("test:house_family_2")!!
        ActivityBuildSourceSnapshot.capture(worldAdmin, definition, place.id())
        val geometry = ActivityBuildingGeometry().generate(definition)
        for (anchor in definition.anchors) {
            val mapped = worldAdmin.getNode("${place.id()}:${anchor.anchorId}")!!
            val physical = geometry.getAnchor(anchor.anchorId, place.minX(), place.minY(), place.minZ())!!
            assertEquals(mapped.x(), physical.x)
            assertEquals(mapped.y(), physical.y)
            assertEquals(mapped.z(), physical.z)
        }
    }

    @Test
    fun marketGeometryMatchesMappedTradeAndMeetingAnchors() {
        assertTrue(BuildingAutoPlaceService(worldAdmin).autoPlace("market", "test").success())
        val definition = BuildingTemplateRegistry.get("market")!!
        val place = worldAdmin.getPlace("test:market_2")!!
        ActivityBuildSourceSnapshot.capture(worldAdmin, definition, place.id())
        val geometry = ActivityBuildingGeometry().generate(definition)
        for (anchorId in listOf("entrance", "stall", "customer", "square")) {
            val mapped = worldAdmin.getNode("${place.id()}:$anchorId")!!
            val physical = geometry.getAnchor(anchorId, place.minX(), place.minY(), place.minZ())!!
            assertEquals(mapped.x(), physical.x)
            assertEquals(mapped.y(), physical.y)
            assertEquals(mapped.z(), physical.z)
        }
    }

    @Test
    fun workshopGeometryMatchesMappedCraftAndCustomerAnchors() {
        assertTrue(BuildingAutoPlaceService(worldAdmin).autoPlace("workshop", "test").success())
        val definition = BuildingTemplateRegistry.get("workshop")!!
        val place = worldAdmin.getPlace("test:workshop_2")!!
        ActivityBuildSourceSnapshot.capture(worldAdmin, definition, place.id())
        val geometry = ActivityBuildingGeometry().generate(definition)
        for (anchorId in listOf("entrance", "bench", "customer")) {
            val mapped = worldAdmin.getNode("${place.id()}:$anchorId")!!
            val physical = geometry.getAnchor(anchorId, place.minX(), place.minY(), place.minZ())!!
            assertEquals(mapped.x(), physical.x)
            assertEquals(mapped.y(), physical.y)
            assertEquals(mapped.z(), physical.z)
        }
        assertTrue(ro.ainpc.spawn.SpawnSemanticRules.isWorkplace(place))
    }

    @Test
    fun sharedWorkshopGeometryMatchesBothWorkstationsAndCustomer() {
        assertTrue(BuildingAutoPlaceService(worldAdmin).autoPlace("workshop_shared", "test").success())
        val definition = BuildingTemplateRegistry.get("workshop_shared")!!
        val place = worldAdmin.getPlace("test:workshop_shared_2")!!
        ActivityBuildSourceSnapshot.capture(worldAdmin, definition, place.id())
        val geometry = ActivityBuildingGeometry().generate(definition)
        for (anchor in definition.anchors) {
            val mapped = worldAdmin.getNode("${place.id()}:${anchor.anchorId}")!!
            val physical = geometry.getAnchor(anchor.anchorId, place.minX(), place.minY(), place.minZ())!!
            assertEquals(mapped.x(), physical.x)
            assertEquals(mapped.y(), physical.y)
            assertEquals(mapped.z(), physical.z)
        }
    }

    @Test
    fun bakeryGeometryMatchesMappedOvenAndCustomerAnchors() {
        assertTrue(BuildingAutoPlaceService(worldAdmin).autoPlace("bakery", "test").success())
        val definition = BuildingTemplateRegistry.get("bakery")!!
        val place = worldAdmin.getPlace("test:bakery_2")!!
        ActivityBuildSourceSnapshot.capture(worldAdmin, definition, place.id())
        val geometry = ActivityBuildingGeometry().generate(definition)
        for (anchorId in listOf("entrance", "oven", "customer")) {
            val mapped = worldAdmin.getNode("${place.id()}:$anchorId")!!
            val physical = geometry.getAnchor(anchorId, place.minX(), place.minY(), place.minZ())!!
            assertEquals(mapped.x(), physical.x)
            assertEquals(mapped.y(), physical.y)
            assertEquals(mapped.z(), physical.z)
        }
        assertTrue(ro.ainpc.spawn.SpawnSemanticRules.isWorkplace(place))
    }

    @Test
    fun libraryGeometryMatchesMappedWorkAndSocialAnchors() {
        assertTrue(BuildingAutoPlaceService(worldAdmin).autoPlace("library", "test").success())
        val definition = BuildingTemplateRegistry.get("library")!!
        val place = worldAdmin.getPlace("test:library_2")!!
        ActivityBuildSourceSnapshot.capture(worldAdmin, definition, place.id())
        val geometry = ActivityBuildingGeometry().generate(definition)
        for (anchorId in listOf("entrance", "desk", "reading")) {
            val mapped = worldAdmin.getNode("${place.id()}:$anchorId")!!
            val physical = geometry.getAnchor(anchorId, place.minX(), place.minY(), place.minZ())!!
            assertEquals(mapped.x(), physical.x)
            assertEquals(mapped.y(), physical.y)
            assertEquals(mapped.z(), physical.z)
        }
        assertTrue(ro.ainpc.spawn.SpawnSemanticRules.isWorkplace(place))
        assertTrue(ro.ainpc.spawn.SpawnSemanticRules.isSocialPlace(place))
    }

    @Test
    fun tavernGeometryMatchesMappedWorkAndSocialAnchors() {
        val placed = BuildingAutoPlaceService(worldAdmin).autoPlace("tavern", "test")
        assertTrue(placed.success())
        val definition = BuildingTemplateRegistry.get("tavern")!!
        val place = worldAdmin.getPlace("test:tavern_2")!!
        ActivityBuildSourceSnapshot.capture(worldAdmin, definition, place.id())
        val geometry = ActivityBuildingGeometry().generate(definition)
        for (anchorId in listOf("counter", "tables")) {
            val mapped = worldAdmin.getNode("${place.id()}:$anchorId")!!
            val physical = geometry.getAnchor(anchorId, place.minX(), place.minY(), place.minZ())!!
            assertEquals(mapped.x(), physical.x)
            assertEquals(mapped.y(), physical.y)
            assertEquals(mapped.z(), physical.z)
        }
    }
}
