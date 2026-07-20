package ro.ainpc.world

import java.io.File
import java.util.logging.Logger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class WorldMappingCompensatorTest {
    private lateinit var service: WorldAdminService

    @BeforeEach
    fun setUp() {
        service = WorldAdminService({ }, Logger.getLogger("WorldMappingCompensatorTest"))
    }

    @Test
    fun rollsBackOnlyIdsRecordedByCurrentMutation() {
        service.createRegion(
            "existing_region", "Existing", "world", RegionType.SETTLEMENT,
            -100, 50, -100, -40, 90, -40,
        )
        val existingPlace = service.createPlace(
            "existing_region", "existing_place", "Existing Place", "world", PlaceType.HOUSE,
            -90, 60, -90, -70, 75, -70,
        )
        val existingNode = service.createNode(
            "existing_region", existingPlace.id, "existing_node", WorldNodeType.HOME,
            "world", -80.0, 64.0, -80.0, 2.0,
        )

        val createdRegion = service.createRegion(
            "created_region", "Created", "world", RegionType.SETTLEMENT,
            40, 50, 40, 100, 90, 100,
        )
        val createdPlace = service.createPlace(
            createdRegion.id, "created_place", "Created Place", "world", PlaceType.HOUSE,
            50, 60, 50, 70, 75, 70,
        )
        val createdNode = service.createNode(
            createdRegion.id, createdPlace.id, "created_node", WorldNodeType.HOME,
            "world", 60.0, 64.0, 60.0, 2.0,
        )
        val regionIds = mutableListOf(createdRegion.id)
        val placeIds = mutableListOf(createdPlace.id)
        val nodeIds = mutableListOf(createdNode.id)

        val result = WorldMappingCompensator.rollback(service, regionIds, placeIds, nodeIds)

        assertTrue(result.success())
        assertEquals(listOf(createdRegion.id), result.removedRegionIds)
        assertEquals(listOf(createdPlace.id), result.removedPlaceIds)
        assertEquals(listOf(createdNode.id), result.removedNodeIds)
        assertTrue(regionIds.isEmpty())
        assertTrue(placeIds.isEmpty())
        assertTrue(nodeIds.isEmpty())
        assertNull(service.getRegion(createdRegion.id))
        assertNull(service.getPlace(createdPlace.id))
        assertNull(service.getNode(createdNode.id))
        assertNotNull(service.getRegion("existing_region"))
        assertNotNull(service.getPlace(existingPlace.id))
        assertNotNull(service.getNode(existingNode.id))
    }

    @Test
    fun everyBulkMappingMutatorUsesCompensation() {
        val files = listOf(
            "src/main/kotlin/ro/ainpc/world/scan/SemanticVillageMapper.kt",
            "src/main/kotlin/ro/ainpc/world/patch/VillagePatchApplier.kt",
            "src/main/kotlin/ro/ainpc/world/fixture/ControlledTestWorldFixtureApplier.kt",
            "src/main/kotlin/ro/ainpc/settlement/BuildingAutoPlaceService.kt",
        )

        for (path in files) {
            val source = File(path).readText()
            assertTrue(
                source.contains("WorldMappingCompensator.rollback("),
                "Mutatorul bulk nu foloseste compensarea: $path",
            )
        }
    }
}
