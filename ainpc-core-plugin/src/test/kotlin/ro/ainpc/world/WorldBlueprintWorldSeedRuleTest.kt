package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorldBlueprintWorldSeedRuleTest {
    @Test
    fun `accepts signed 64 bit decimal seeds and leaves absent rule unset`() {
        val absent = WorldBlueprintWorldSeedRule.parse(emptyMap())
        assertFalse(absent.configured)
        assertTrue(absent.isValid)
        assertNull(absent.seed)

        for (seed in listOf("0", "-1", Long.MIN_VALUE.toString(), Long.MAX_VALUE.toString())) {
            val parsed = WorldBlueprintWorldSeedRule.parse(mapOf(WorldBlueprintWorldSeedRule.KEY to " $seed "))
            assertTrue(parsed.isValid, seed)
            assertEquals(seed.toLong(), parsed.seed)
        }
    }

    @Test
    fun `rejects non decimal and overflowing seeds`() {
        for (seed in listOf("", "seed", "+1", "01", "1.5", "9223372036854775808", "-9223372036854775809")) {
            val parsed = WorldBlueprintWorldSeedRule.parse(mapOf(WorldBlueprintWorldSeedRule.KEY to seed))
            assertTrue(parsed.configured)
            assertFalse(parsed.isValid, seed)
            assertNull(parsed.seed)
        }
    }
}
