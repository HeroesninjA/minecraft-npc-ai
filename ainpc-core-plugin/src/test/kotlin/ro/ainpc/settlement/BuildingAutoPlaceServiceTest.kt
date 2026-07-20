package ro.ainpc.settlement

import java.util.logging.Logger
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ro.ainpc.api.settlement.BuildingAnchorDefinition
import ro.ainpc.api.settlement.BuildingTemplateDefinition
import ro.ainpc.world.RegionType
import ro.ainpc.world.WorldAdminService

class BuildingAutoPlaceServiceTest {
    private lateinit var worldAdmin: WorldAdminService

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
}
