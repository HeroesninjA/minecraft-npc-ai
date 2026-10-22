package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorldBlueprintBaseSupportRuleTest {
    @Test
    fun `observed unsupported samples prove a threshold violation`() {
        val rule = WorldBlueprintBaseSupportRule.parse(mapOf(WorldBlueprintBaseSupportRule.KEY to "1"))
        val preview = WorldBlueprintFoundationPreview(5, 2, 1, 1, 0)

        assertEquals(2, preview.unsupportedCount)
        assertEquals(1, preview.unknownCount)
        assertEquals(true, rule.exceeds(preview))
    }

    @Test
    fun `unknown samples prevent false compliance but upper bound can prove it`() {
        val preview = WorldBlueprintFoundationPreview(5, 2, 1, 1, 0)
        val strict = WorldBlueprintBaseSupportRule.parse(mapOf(WorldBlueprintBaseSupportRule.KEY to "2"))
        val relaxed = WorldBlueprintBaseSupportRule.parse(mapOf(WorldBlueprintBaseSupportRule.KEY to "3"))
        val complete = WorldBlueprintFoundationPreview(5, 3, 1, 1, 0)

        assertNull(strict.exceeds(preview))
        assertEquals(false, relaxed.exceeds(preview))
        assertEquals(false, strict.exceeds(complete))
        assertNull(strict.exceeds(null))
    }

    @Test
    fun `parser accepts only zero through five and valid draft key`() {
        assertTrue(WorldBlueprintPolicy.validKey(WorldBlueprintBaseSupportRule.KEY))
        assertFalse(WorldBlueprintBaseSupportRule.parse(emptyMap()).configured)
        assertEquals(0, WorldBlueprintBaseSupportRule.parse(mapOf(
            WorldBlueprintBaseSupportRule.KEY to " 0 ")).maximum)
        assertEquals(5, WorldBlueprintBaseSupportRule.parse(mapOf(
            WorldBlueprintBaseSupportRule.KEY to "5")).maximum)
        listOf("", "-1", "6", "1.5", "unknown").forEach { raw ->
            assertFalse(WorldBlueprintBaseSupportRule.parse(mapOf(
                WorldBlueprintBaseSupportRule.KEY to raw)).isValid, raw)
        }
    }
}
