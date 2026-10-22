package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigInteger

class WorldBlueprintStructureRegionPositionsRuleTest {
    @Test
    fun `compares exact inclusive cuboid positions to a configured maximum`() {
        val bounds = WorldBlueprintRegionBounds(0, 64, 0, 9, 67, 19)
        val equal = WorldBlueprintStructureRegionPositionsRule.parse(mapOf(
            WorldBlueprintStructureRegionPositionsRule.KEY to " 000800 ",
        ))
        val exceeded = WorldBlueprintStructureRegionPositionsRule.parse(mapOf(
            WorldBlueprintStructureRegionPositionsRule.KEY to "799",
        ))

        assertTrue(equal.isValid)
        assertEquals(BigInteger.valueOf(800), equal.maximum)
        assertEquals(false, equal.exceeds(bounds))
        assertEquals(true, exceeded.exceeds(bounds))
    }

    @Test
    fun `rejects invalid or impossible thresholds without affecting absent rules`() {
        listOf("", "0", "-1", "1.5", "1e6", "invalid", "79228162514264337593543950337").forEach { raw ->
            val rule = WorldBlueprintStructureRegionPositionsRule.parse(mapOf(
                WorldBlueprintStructureRegionPositionsRule.KEY to raw,
            ))
            assertTrue(rule.configured)
            assertFalse(rule.isValid)
            assertEquals(null, rule.exceeds(WorldBlueprintRegionBounds(0, 0, 0, 0, 0, 0)))
        }
        val absent = WorldBlueprintStructureRegionPositionsRule.parse(emptyMap())
        assertFalse(absent.configured)
        assertTrue(absent.isValid)
    }

    @Test
    fun `full integer coordinate cube is a valid exact limit`() {
        val maximum = BigInteger.ONE.shiftLeft(96)
        val rule = WorldBlueprintStructureRegionPositionsRule.parse(mapOf(
            WorldBlueprintStructureRegionPositionsRule.KEY to maximum.toString(),
        ))
        val bounds = WorldBlueprintRegionBounds(Int.MIN_VALUE, Int.MIN_VALUE, Int.MIN_VALUE,
            Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE)

        assertEquals(maximum, rule.maximum)
        assertEquals(false, rule.exceeds(bounds))
    }
}
