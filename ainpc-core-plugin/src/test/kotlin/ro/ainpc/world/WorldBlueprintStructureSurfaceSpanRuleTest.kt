package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorldBlueprintStructureSurfaceSpanRuleTest {
    @Test
    fun `parses a nonnegative block threshold and compares sampled span`() {
        val rule = WorldBlueprintStructureSurfaceSpanRule.parse(mapOf(WorldBlueprintStructureSurfaceSpanRule.KEY to " 20 "))
        val within = WorldBlueprintTerrainPreview(5, 3, 60, 80)
        val exceeded = WorldBlueprintTerrainPreview(5, 3, 60, 81)

        assertTrue(rule.isValid)
        assertEquals(20, rule.maximum)
        assertEquals(false, rule.exceeds(within))
        assertEquals(true, rule.exceeds(exceeded))
    }

    @Test
    fun `invalid and missing thresholds do not evaluate terrain`() {
        listOf("", "-1", "1.5", "many", "2147483648").forEach { raw ->
            val rule = WorldBlueprintStructureSurfaceSpanRule.parse(mapOf(WorldBlueprintStructureSurfaceSpanRule.KEY to raw))
            assertTrue(rule.configured)
            assertFalse(rule.isValid)
            assertEquals(null, rule.exceeds(WorldBlueprintTerrainPreview(5, 2, 60, 80)))
        }
        val absent = WorldBlueprintStructureSurfaceSpanRule.parse(emptyMap())
        assertFalse(absent.configured)
        assertTrue(absent.isValid)
    }

    @Test
    fun `requires two observations and avoids integer overflow`() {
        val rule = WorldBlueprintStructureSurfaceSpanRule.parse(mapOf(WorldBlueprintStructureSurfaceSpanRule.KEY to "2147483647"))

        assertEquals(null, rule.exceeds(WorldBlueprintTerrainPreview(5, 1, 64, 64)))
        assertEquals(4294967295L, WorldBlueprintTerrainPreview(5, 2, Int.MIN_VALUE, Int.MAX_VALUE).observedSpan)
        assertEquals(true, rule.exceeds(WorldBlueprintTerrainPreview(5, 2, Int.MIN_VALUE, Int.MAX_VALUE)))
    }
}
