package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorldBlueprintStructureFluidRuleTest {
    private val bounds = WorldBlueprintRegionBounds(0, 0, 0, 1, 0, 1)

    @Test
    fun `observed fluid positions prove violation with unloaded positions`() {
        val rule = WorldBlueprintStructureFluidRule.parse(mapOf(WorldBlueprintStructureFluidRule.KEY to "1"))
        val occupancy = WorldBlueprintBlockOccupancyPreview(4, 3, 2, true, fluidPositionCount = 2)

        assertEquals(true, rule.exceeds(bounds, occupancy))
    }

    @Test
    fun `only a complete or bounded volume proves compliance`() {
        val limitOne = WorldBlueprintStructureFluidRule.parse(mapOf(WorldBlueprintStructureFluidRule.KEY to "1"))
        val limitZero = WorldBlueprintStructureFluidRule.parse(mapOf(WorldBlueprintStructureFluidRule.KEY to "0"))

        assertEquals(false, limitOne.exceeds(bounds,
            WorldBlueprintBlockOccupancyPreview(4, 3, 2, true, fluidPositionCount = 0)))
        assertNull(limitZero.exceeds(bounds,
            WorldBlueprintBlockOccupancyPreview(4, 3, 2, true, fluidPositionCount = 0)))
        assertEquals(false, limitZero.exceeds(bounds,
            WorldBlueprintBlockOccupancyPreview(4, 4, 2, true, fluidPositionCount = 0)))
        assertNull(limitOne.exceeds(bounds, null))
    }

    @Test
    fun `clean samples do not prove a large site is dry`() {
        val rule = WorldBlueprintStructureFluidRule.parse(mapOf(WorldBlueprintStructureFluidRule.KEY to "0"))
        val largeBounds = WorldBlueprintRegionBounds(0, 0, 0, 99, 0, 0)

        assertNull(rule.exceeds(largeBounds,
            WorldBlueprintBlockOccupancyPreview(3, 3, 0, false, fluidPositionCount = 0)))
    }

    @Test
    fun `parser rejects invalid and out of range limits`() {
        assertFalse(WorldBlueprintStructureFluidRule.parse(mapOf(WorldBlueprintStructureFluidRule.KEY to "-1")).isValid)
        assertFalse(WorldBlueprintStructureFluidRule.parse(mapOf(
            WorldBlueprintStructureFluidRule.KEY to "79228162514264337593543950337")).isValid)
        assertTrue(WorldBlueprintStructureFluidRule.parse(mapOf(
            WorldBlueprintStructureFluidRule.KEY to "79228162514264337593543950336")).isValid)
        assertEquals("0", WorldBlueprintStructureFluidRule.parse(mapOf(
            WorldBlueprintStructureFluidRule.KEY to "0000")).maximum.toString())
    }
}
