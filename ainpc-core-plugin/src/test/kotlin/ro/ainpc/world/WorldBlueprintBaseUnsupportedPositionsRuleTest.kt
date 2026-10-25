package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigInteger

class WorldBlueprintBaseUnsupportedPositionsRuleTest {
    private val smallBounds = WorldBlueprintRegionBounds(0, 64, 0, 1, 64, 1)

    private fun rule(value: String): WorldBlueprintBaseUnsupportedPositionsRule =
        WorldBlueprintBaseUnsupportedPositionsRule.parse(mapOf(WorldBlueprintBaseUnsupportedPositionsRule.KEY to value))

    @Test
    fun `accepts the full horizontal coordinate range without overflow`() {
        val maximum = BigInteger.ONE.shiftLeft(64)
        val bounds = WorldBlueprintRegionBounds(Int.MIN_VALUE, 64, Int.MIN_VALUE,
            Int.MAX_VALUE, 64, Int.MAX_VALUE)
        val preview = WorldBlueprintFoundationPreview(5, 5, 0, 0, 0)

        assertEquals(maximum, rule(maximum.toString()).maximum)
        assertEquals(false, rule(maximum.toString()).exceeds(bounds, preview))
        assertEquals(false, rule((maximum - BigInteger.ONE).toString()).exceeds(bounds, preview))
        assertNull(rule("0").exceeds(bounds, preview))
    }

    @Test
    fun `rejects malformed and out of range values`() {
        assertFalse(rule("-1").isValid)
        assertFalse(rule("1.5").isValid)
        assertFalse(rule("").isValid)
        assertFalse(rule((BigInteger.ONE.shiftLeft(64) + BigInteger.ONE).toString()).isValid)
        assertTrue(rule(" 000 ").isValid)
        assertEquals(BigInteger.ZERO, rule(" 000 ").maximum)
        assertFalse(WorldBlueprintBaseUnsupportedPositionsRule.parse(emptyMap()).configured)
    }

    @Test
    fun `exact small survey distinguishes violation compliance and missing columns`() {
        val complete = WorldBlueprintFoundationPreview(4, 3, 1, 0, 0)
        val incomplete = WorldBlueprintFoundationPreview(4, 2, 1, 0, 0)

        assertEquals(true, rule("0").exceeds(smallBounds, complete))
        assertEquals(false, rule("1").exceeds(smallBounds, complete))
        assertNull(rule("1").exceeds(smallBounds, incomplete))
        assertEquals(false, rule("2").exceeds(smallBounds, incomplete))
        assertNull(rule("0").exceeds(smallBounds, null))
    }

    @Test
    fun `large sampled base proves only lower or upper bounds`() {
        val bounds = WorldBlueprintRegionBounds(0, 64, 0, 499, 64, 499)
        val preview = WorldBlueprintFoundationPreview(5, 4, 1, 0, 0)

        assertEquals(true, rule("0").exceeds(bounds, preview))
        assertNull(rule("1").exceeds(bounds, preview))
        assertEquals(false, rule("250000").exceeds(bounds, preview))
    }
}
