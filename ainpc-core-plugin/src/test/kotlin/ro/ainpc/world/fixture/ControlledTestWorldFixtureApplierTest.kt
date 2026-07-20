package ro.ainpc.world.fixture

import java.util.logging.Logger
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.world.WorldAdminService

class ControlledTestWorldFixtureApplierTest {
    @Test
    fun rollsBackFixtureWhenARequiredNodeFails() {
        val service = WorldAdminService({ }, Logger.getLogger("ControlledTestWorldFixtureApplierTest"))
        val region = ControlledFixtureRegionPlan(
            "fixture_region",
            "settlement",
            "Fixture Region",
            "village",
            0,
            0,
            emptyList(),
            emptyList(),
            listOf("meeting", "meeting"),
        )
        val plan = ControlledTestWorldFixturePlan(
            "fixture_rollback",
            "fixture_",
            region,
            emptyList(),
            emptyList(),
        )

        val result = ControlledTestWorldFixtureApplier().apply(service, plan, "world", 0, 0)

        assertFalse(result.success())
        assertTrue(result.regionIds().isEmpty())
        assertTrue(result.nodeIds().isEmpty())
        assertNull(service.getRegion("fixture_region"))
        assertNull(service.getNode("fixture_region:meeting"))
        assertTrue(result.warnings().any { it.startsWith("Compensare mapping reusita") })
    }
}
