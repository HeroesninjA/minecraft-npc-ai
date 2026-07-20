package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ro.ainpc.world.patch.PatchBuildMode
import ro.ainpc.world.patch.PatchPlan
import ro.ainpc.world.patch.PatchType
import ro.ainpc.world.patch.PatchValidationStatus
import ro.ainpc.world.patch.VillagePatchApplier
import java.util.logging.Logger

class VillagePatchApplierTest {

    private lateinit var service: WorldAdminService

    @BeforeEach
    fun setUp() {
        service = WorldAdminService({ }, Logger.getLogger("VillagePatchApplierTest"))
        service.createRegion("test_sat", "Test Sat", "world", RegionType.SETTLEMENT,
            -100, 50, -100, 100, 90, 100)
    }

    @Test
    fun applyWithValidationErrorsReturnsFailure() {
        val plan = PatchPlan(
            "test:patch:bad",
            PatchType.ADD_HOUSE,
            PatchBuildMode.NATIVE_PATCH,
            "test_sat", "",
            "", listOf("test_sat:patch_house_01"), emptyList(),
            listOf("native-block-build"), PatchValidationStatus.BLOCKED,
            emptyList(), listOf("Eroare de validare test"),
            "test", 5, 4, 4
        )

        val result = VillagePatchApplier().apply(service, plan)
        assertFalse(result.success())
        assertTrue(result.errors().any { it.contains("erori de validare") })
    }

    @Test
    fun applyWithBlockedStatusProceedsWithWarning() {
        val plan = PatchPlan(
            "test:patch:blocked",
            PatchType.ADD_HOUSE,
            PatchBuildMode.NATIVE_PATCH,
            "test_sat", "",
            "", listOf("test_sat:patch_house_01"), emptyList(),
            emptyList(), PatchValidationStatus.BLOCKED,
            emptyList(), emptyList(),
            "test", 5, 4, 4
        )

        val result = VillagePatchApplier().apply(service, plan)
        assertTrue(result.warnings().any { it.contains("BLOCKED") })
    }

    @Test
    fun applyToNonexistentRegionReturnsError() {
        val plan = PatchPlan(
            "test:patch:ghost",
            PatchType.ADD_HOUSE,
            PatchBuildMode.NATIVE_PATCH,
            "test_sat", "",
            "", listOf("test_sat:patch_house_01"), emptyList(),
            emptyList(), PatchValidationStatus.VALID,
            emptyList(), emptyList(),
            "test", 5, 4, 4
        )

        val result = VillagePatchApplier().apply(service, plan, "nonexistent_region")
        assertFalse(result.success())
        assertTrue(result.errors().any { it.contains("nu exista") })
    }

    @Test
    fun applyNativePatchAddHouseCreatesPlaceAndNodes() {
        val plan = PatchPlan(
            "test:patch:add_house",
            PatchType.ADD_HOUSE,
            PatchBuildMode.NATIVE_PATCH,
            "test_sat", "",
            "", listOf("test_sat:patch_house_01"),
            listOf("test_sat:patch_house_01:bed_1", "test_sat:patch_house_01:npc_spawn_1"),
            emptyList(), PatchValidationStatus.VALID,
            emptyList(), emptyList(),
            "Lipsa casa", 5, 4, 4
        )

        val result = VillagePatchApplier().apply(service, plan)
        assertTrue(result.success(), "Errors: ${result.errors()}")
        assertEquals(1, result.placeCount())
        assertEquals(2, result.nodeCount())

        val regionPlaces = service.getPlaces("test_sat")
        assertTrue(regionPlaces.any { it.id() == "test_sat:patch_house_01" })

        val placeNodes = service.getNodesForPlace("test_sat:patch_house_01")
        assertEquals(2, placeNodes.size)
    }

    @Test
    fun applyNativePatchAddWorkplaceCreatesShopPlace() {
        val plan = PatchPlan(
            "test:patch:add_workplace",
            PatchType.ADD_WORKPLACE,
            PatchBuildMode.NATIVE_PATCH,
            "test_sat", "",
            "", listOf("test_sat:patch_atelier_01"),
            listOf("test_sat:patch_atelier_01:workstation_1"),
            emptyList(), PatchValidationStatus.VALID,
            emptyList(), emptyList(),
            "Lipsa atelier", 5, 5, 5
        )

        val result = VillagePatchApplier().apply(service, plan)
        assertTrue(result.success(), "Errors: ${result.errors()}")
        assertEquals(1, result.placeCount())
        assertEquals(1, result.nodeCount())
    }

    @Test
    fun applySemanticOnlyAddNodeCreatesNodesWithoutPlace() {
        val placeId = "test_sat:piata"
        service.createPlace("test_sat", "piata", "Piata", "world", PlaceType.MARKET,
            -14, 60, -12, 14, 70, 12)

        val plan = PatchPlan(
            "test:patch:add_node",
            PatchType.ADD_NODE,
            PatchBuildMode.SEMANTIC_ONLY,
            "test_sat", placeId,
            "", emptyList(),
            listOf("$placeId:patch_quest_board", "$placeId:patch_meeting_point"),
            emptyList(), PatchValidationStatus.VALID,
            emptyList(), emptyList(),
            "Lipsa noduri", 3, 1, 1
        )

        val result = VillagePatchApplier().apply(service, plan)
        assertTrue(result.success(), "Errors: ${result.errors()}")
        assertEquals(0, result.placeCount())
        assertEquals(2, result.nodeCount())
    }

    @Test
    fun applyWorldEditTemplateReturnsWarning() {
        val plan = PatchPlan(
            "test:patch:we",
            PatchType.ADD_HOUSE,
            PatchBuildMode.WORLDEDIT_TEMPLATE,
            "test_sat", "",
            "small_house", listOf("test_sat:patch_house_we"), emptyList(),
            emptyList(), PatchValidationStatus.VALID,
            emptyList(), emptyList(),
            "test", 5, 4, 4
        )

        val result = VillagePatchApplier().apply(service, plan)
        assertTrue(result.success())
        assertTrue(result.warnings().any { it.contains("WorldEdit") })
        assertEquals(0, result.placeCount())
        assertEquals(0, result.nodeCount())
    }

    @Test
    fun applyDuplicatePlaceIdSkipsWithWarning() {
        service.createPlace("test_sat", "house_01", "Casa 01", "world", PlaceType.HOUSE,
            -30, 60, -30, -18, 70, -18)

        val plan = PatchPlan(
            "test:patch:duplicate",
            PatchType.ADD_HOUSE,
            PatchBuildMode.NATIVE_PATCH,
            "test_sat", "",
            "", listOf("test_sat:house_01"),
            listOf("test_sat:house_01:bed_1"),
            emptyList(), PatchValidationStatus.VALID,
            emptyList(), emptyList(),
            "test", 5, 4, 4
        )

        val result = VillagePatchApplier().apply(service, plan)
        assertTrue(result.success())
        assertTrue(result.warnings().any { it.contains("exista deja") })
        assertEquals(0, result.placeCount())
    }

    @Test
    fun applyRegionIdOverrideUsesOverride() {
        service.createRegion("alt_sat", "Alt Sat", "world", RegionType.SETTLEMENT,
            200, 50, 200, 300, 90, 300)

        val plan = PatchPlan(
            "test:patch:override",
            PatchType.ADD_HOUSE,
            PatchBuildMode.NATIVE_PATCH,
            "test_sat", "",
            "", listOf("alt_sat:patch_house_01"), emptyList(),
            emptyList(), PatchValidationStatus.VALID,
            emptyList(), emptyList(),
            "test", 5, 4, 4
        )

        val result = VillagePatchApplier().apply(service, plan, "alt_sat")
        assertTrue(result.success(), "Errors: ${result.errors()}")
        assertEquals(1, result.placeCount())
        assertTrue(service.getPlace("alt_sat:patch_house_01") != null)
    }

    @Test
    fun applyWithRegionIdOverrideEmptyUsesPlanRegion() {
        val plan = PatchPlan(
            "test:patch:native",
            PatchType.ADD_HOUSE,
            PatchBuildMode.NATIVE_PATCH,
            "test_sat", "",
            "", listOf("test_sat:patch_house_native"), emptyList(),
            emptyList(), PatchValidationStatus.VALID,
            emptyList(), emptyList(),
            "test", 5, 4, 4
        )

        val result = VillagePatchApplier().apply(service, plan, "")
        assertTrue(result.success(), "Errors: ${result.errors()}")
        assertEquals(1, result.placeCount())
    }

    @Test
    fun resultReflectsPartialFailureWhenNodeCreationFails() {
        service.createPlace("test_sat", "existing_place", "Existing", "world", PlaceType.HOUSE,
            -10, 60, -10, 2, 70, 2)

        val plan = PatchPlan(
            "test:patch:partial",
            PatchType.ADD_NODE,
            PatchBuildMode.SEMANTIC_ONLY,
            "test_sat", "test_sat:nonexistent_place",
            "", emptyList(),
            listOf("test_sat:nonexistent_place:bad_node"),
            emptyList(), PatchValidationStatus.VALID,
            emptyList(), emptyList(),
            "test", 3, 1, 1
        )

        val result = VillagePatchApplier().apply(service, plan)
        assertFalse(result.success())
        assertTrue(result.errors().any { it.contains("Nu pot crea node-ul") })
    }

    @Test
    fun rollsBackCreatedNodeWhenLaterNodeCreationFails() {
        val plan = PatchPlan(
            "test:patch:rollback",
            PatchType.ADD_NODE,
            PatchBuildMode.SEMANTIC_ONLY,
            "test_sat", "",
            "", emptyList(),
            listOf("test_sat:duplicate_node", "test_sat:duplicate_node"),
            emptyList(), PatchValidationStatus.VALID,
            emptyList(), emptyList(),
            "test", 3, 1, 1,
        )

        val result = VillagePatchApplier().apply(service, plan)

        assertFalse(result.success())
        assertTrue(result.createdNodeIds().isEmpty())
        assertNull(service.getNode("test_sat:duplicate_node"))
        assertTrue(result.warnings().any { it.startsWith("Compensare mapping reusita") })
    }
}
