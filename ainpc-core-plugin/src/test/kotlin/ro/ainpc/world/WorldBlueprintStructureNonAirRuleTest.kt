package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorldBlueprintStructureNonAirRuleTest {
    private val bounds = WorldBlueprintRegionBounds(0, 0, 0, 1, 0, 1)

    @Test
    fun `observed blocks prove violation even when some positions are unknown`() {
        val rule = WorldBlueprintStructureNonAirRule.parse(mapOf(
            WorldBlueprintStructureNonAirRule.KEY to "1"))
        val occupancy = WorldBlueprintBlockOccupancyPreview(4, 3, 2, true)

        assertTrue(rule.isValid)
        assertEquals(true, rule.exceeds(bounds, occupancy))
    }

    @Test
    fun `upper bound proves compliance but incomplete sample does not`() {
        val limitOne = WorldBlueprintStructureNonAirRule.parse(mapOf(
            WorldBlueprintStructureNonAirRule.KEY to "1"))
        val limitZero = WorldBlueprintStructureNonAirRule.parse(mapOf(
            WorldBlueprintStructureNonAirRule.KEY to "0"))

        assertEquals(false, limitOne.exceeds(bounds, WorldBlueprintBlockOccupancyPreview(4, 3, 0, true)))
        assertNull(limitZero.exceeds(bounds, WorldBlueprintBlockOccupancyPreview(4, 3, 0, true)))
        assertEquals(false, limitZero.exceeds(bounds, WorldBlueprintBlockOccupancyPreview(4, 4, 0, true)))
        assertNull(limitOne.exceeds(bounds, null))
    }

    @Test
    fun `large sampled volumes are not declared compliant from a clean sample`() {
        val largeBounds = WorldBlueprintRegionBounds(0, 0, 0, 99, 0, 0)
        val rule = WorldBlueprintStructureNonAirRule.parse(mapOf(
            WorldBlueprintStructureNonAirRule.KEY to "0"))

        assertNull(rule.exceeds(largeBounds, WorldBlueprintBlockOccupancyPreview(3, 3, 0, false)))
    }

    @Test
    fun `parser rejects malformed and out of range limits`() {
        assertFalse(WorldBlueprintStructureNonAirRule.parse(mapOf(
            WorldBlueprintStructureNonAirRule.KEY to "-1")).isValid)
        assertFalse(WorldBlueprintStructureNonAirRule.parse(mapOf(
            WorldBlueprintStructureNonAirRule.KEY to "79228162514264337593543950337")).isValid)
        assertTrue(WorldBlueprintStructureNonAirRule.parse(mapOf(
            WorldBlueprintStructureNonAirRule.KEY to "79228162514264337593543950336")).isValid)
        assertEquals("0", WorldBlueprintStructureNonAirRule.parse(mapOf(
            WorldBlueprintStructureNonAirRule.KEY to "0000")).maximum.toString())
    }
}
