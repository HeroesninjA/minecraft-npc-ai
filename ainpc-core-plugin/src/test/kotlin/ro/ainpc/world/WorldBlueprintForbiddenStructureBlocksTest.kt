package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorldBlueprintForbiddenStructureBlocksTest {
    private val knownBlocks = setOf("STONE", "CHEST")

    @Test
    fun `normalizes known block IDs and finds only observed forbidden materials`() {
        val rule = WorldBlueprintForbiddenStructureBlocks.parse(mapOf(
            WorldBlueprintForbiddenStructureBlocks.KEY to "STONE, minecraft:chest,stone")) { it in knownBlocks }

        assertTrue(rule.configured)
        assertTrue(rule.isValid)
        assertEquals(setOf("minecraft:stone", "minecraft:chest"), rule.blockIds)
        assertEquals(mapOf("minecraft:chest" to 2, "minecraft:stone" to 1),
            rule.observedViolations(mapOf(
                "minecraft:chest" to 2,
                "minecraft:stone" to 1,
                "minecraft:dirt" to 3,
            )))
    }

    @Test
    fun `invalid lists never yield partial protection`() {
        val invalidValues = listOf(
            "stone,", "minecraft:not_a_block", "custom:stone", "diamond_sword", "air",
            List(65) { "stone" }.joinToString(),
        )

        invalidValues.forEach { value ->
            val rule = WorldBlueprintForbiddenStructureBlocks.parse(mapOf(
                WorldBlueprintForbiddenStructureBlocks.KEY to value)) { it in knownBlocks }
            assertNotNull(rule.error, value)
            assertTrue(rule.blockIds.isEmpty(), value)
            assertTrue(rule.observedViolations(mapOf("minecraft:stone" to 1)).isEmpty(), value)
        }
        assertFalse(WorldBlueprintForbiddenStructureBlocks.parse(emptyMap()).configured)
    }
}
