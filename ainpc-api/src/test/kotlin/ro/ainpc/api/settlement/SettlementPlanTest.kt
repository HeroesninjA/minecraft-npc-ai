package ro.ainpc.api.settlement

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SettlementPlanTest {

    @Test
    fun settlementPlanDefaults() {
        val plan = SettlementPlan(planId = "test_plan")
        assertEquals("test_plan", plan.planId)
        assertEquals(1, plan.version)
        assertEquals(SettlementPlanSource.MANUAL, plan.source)
        assertEquals(SettlementPlanStatus.DRAFT, plan.status)
        assertEquals(0, plan.buildingCount())
        assertEquals(0, plan.nodeCount())
        assertEquals(0, plan.householdCount())
    }

    @Test
    fun settlementPlanStatusTransitions() {
        val draft = SettlementPlan(planId = "test", status = SettlementPlanStatus.DRAFT)
        assertTrue(draft.isDraft())
        assertFalse(draft.isValid())
        assertFalse(draft.isCommittable())

        val validated = draft.copy(status = SettlementPlanStatus.VALIDATED)
        assertTrue(validated.isValid())
        assertTrue(validated.isCommittable())

        val committed = draft.copy(status = SettlementPlanStatus.COMMITTED)
        assertTrue(committed.isValid())
        assertFalse(committed.isCommittable())

        val discarded = draft.copy(status = SettlementPlanStatus.DISCARDED)
        assertFalse(discarded.isValid())
        assertFalse(discarded.isCommittable())
    }

    @Test
    fun regionPlanRequiredFields() {
        val region = RegionPlan(
            regionId = "test_region",
            displayName = "Test Region",
            worldName = "world",
            regionType = "village",
            minX = 0, minY = 0, minZ = 0,
            maxX = 100, maxY = 50, maxZ = 100,
            centerX = 50.0, centerY = 25.0, centerZ = 50.0
        )
        assertEquals("test_region", region.regionId)
        assertEquals("village", region.regionType)
    }

    @Test
    fun buildingPlanDefaults() {
        val building = BuildingPlan(
            buildingKey = "house_01",
            placeId = "test_place",
            displayName = "House 1",
            placeType = "house",
            minX = 0, minY = 0, minZ = 0,
            maxX = 10, maxY = 5, maxZ = 10
        )
        assertEquals(BuildMode.SEMANTIC_ONLY, building.buildMode)
        assertEquals(1, building.capacity)
    }

    @Test
    fun nodePlanDefaults() {
        val node = NodePlan(
            nodeId = "bed_01",
            placeId = "house_01",
            nodeType = "bed",
            x = 5.0, y = 1.0, z = 5.0
        )
        assertEquals(2.0, node.radius)
        assertTrue(node.role.isEmpty())
    }

    @Test
    fun householdPlanDefaults() {
        val household = HouseholdPlan(
            householdKey = "fam_01",
            homePlaceId = "house_01"
        )
        assertEquals("proposed", household.status)
        assertTrue(household.residentNpcKeys.isEmpty())
    }

    @Test
    fun settlementPlanWithFullData() {
        val region = RegionPlan(
            regionId = "satul_central", displayName = "Satul Central", worldName = "world",
            regionType = "village",
            minX = 0, minY = 0, minZ = 0, maxX = 200, maxY = 64, maxZ = 200,
            centerX = 100.0, centerY = 32.0, centerZ = 100.0,
            tags = setOf("medieval", "player_friendly")
        )
        val building = BuildingPlan(
            buildingKey = "house_01", placeId = "satul_central:house_01",
            displayName = "Casa lui Ion", placeType = "house",
            minX = 50, minY = 0, minZ = 50, maxX = 60, maxY = 5, maxZ = 60,
            capacity = 3, buildMode = BuildMode.SEMANTIC_ONLY
        )
        val node = NodePlan(
            nodeId = "satul_central:house_01:bed_01", placeId = "satul_central:house_01",
            nodeType = "bed", x = 55.0, y = 1.0, z = 55.0
        )
        val household = HouseholdPlan(
            householdKey = "fam_ionescu", homePlaceId = "satul_central:house_01",
            familyId = "ionescu", primaryOwnerNpcKey = "npc_ion",
            residentNpcKeys = listOf("npc_ion", "npc_maria"), capacity = 3,
            bedNodeIds = listOf("satul_central:house_01:bed_01")
        )
        val plan = SettlementPlan(
            planId = "plan_satul_central", version = 1,
            source = SettlementPlanSource.DEMO, status = SettlementPlanStatus.DRAFT,
            seed = 42L, themeId = "medieval",
            regionPlan = region,
            buildingPlans = listOf(building),
            nodePlans = listOf(node),
            householdPlans = listOf(household)
        )
        assertEquals(1, plan.buildingCount())
        assertEquals(1, plan.nodeCount())
        assertEquals(1, plan.householdCount())
        assertTrue(plan.isDraft())
        assertEquals(SettlementPlanSource.DEMO, plan.source)
    }

    @Test
    fun settlementPlanStatusFromId() {
        assertEquals(SettlementPlanStatus.DRAFT, SettlementPlanStatus.fromId("draft"))
        assertEquals(SettlementPlanStatus.VALIDATED, SettlementPlanStatus.fromId("validated"))
        assertEquals(SettlementPlanStatus.COMMITTED, SettlementPlanStatus.fromId("committed"))
        assertEquals(SettlementPlanStatus.DISCARDED, SettlementPlanStatus.fromId("discarded"))
        assertEquals(SettlementPlanStatus.FAILED, SettlementPlanStatus.fromId("failed"))
        assertEquals(SettlementPlanStatus.DRAFT, SettlementPlanStatus.fromId("unknown"))
        assertEquals(SettlementPlanStatus.DRAFT, SettlementPlanStatus.fromId(null))
    }

    @Test
    fun settlementPlanSourceFromId() {
        assertEquals(SettlementPlanSource.MANUAL, SettlementPlanSource.fromId("manual"))
        assertEquals(SettlementPlanSource.VANILLA_SCAN, SettlementPlanSource.fromId("vanilla_scan"))
        assertEquals(SettlementPlanSource.AI_DRAFT, SettlementPlanSource.fromId("ai_draft"))
        assertEquals(SettlementPlanSource.DEMO, SettlementPlanSource.fromId("demo"))
        assertEquals(SettlementPlanSource.MIGRATION, SettlementPlanSource.fromId("migration"))
        assertEquals(SettlementPlanSource.MANUAL, SettlementPlanSource.fromId("unknown"))
    }

    @Test
    fun buildModeFromId() {
        assertEquals(BuildMode.EXISTING, BuildMode.fromId("existing"))
        assertEquals(BuildMode.SEMANTIC_ONLY, BuildMode.fromId("semantic_only"))
        assertEquals(BuildMode.NATIVE_PATCH, BuildMode.fromId("native_patch"))
        assertEquals(BuildMode.WORLDEDIT_TEMPLATE, BuildMode.fromId("worldedit_template"))
        assertEquals(BuildMode.EXTERNAL, BuildMode.fromId("external"))
        assertEquals(BuildMode.EXISTING, BuildMode.fromId("unknown"))
    }

    @Test
    fun validationReportDefaults() {
        val report = SettlementValidationReport()
        assertTrue(report.valid)
        assertTrue(report.errors.isEmpty())
        assertTrue(report.warnings.isEmpty())
    }

    @Test
    fun validationReportWithIssues() {
        val report = SettlementValidationReport(
            valid = false,
            errors = listOf("Region bounds invalid"),
            warnings = listOf("No population plan")
        )
        assertFalse(report.valid)
        assertEquals(1, report.errors.size)
        assertEquals(1, report.warnings.size)
    }
}
