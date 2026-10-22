package ro.ainpc.world

import org.bukkit.WorldType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorldBlueprintWorldGenerationRulesTest {
    @Test
    fun `defaults to server settings when rules are absent`() {
        val rules = WorldBlueprintWorldGenerationRules.parse(emptyMap())

        assertFalse(rules.typeConfigured)
        assertNull(rules.worldType)
        assertFalse(rules.structuresConfigured)
        assertNull(rules.generateStructures)
        assertTrue(rules.invalidKeys.isEmpty())
    }

    @Test
    fun `parses supported vanilla world types and structure toggle`() {
        val types = mapOf("normal" to WorldType.NORMAL, "flat" to WorldType.FLAT,
            "large_biomes" to WorldType.LARGE_BIOMES, "amplified" to WorldType.AMPLIFIED)
        types.forEach { (raw, expected) ->
            val rules = WorldBlueprintWorldGenerationRules.parse(mapOf(
                WorldBlueprintWorldGenerationRules.TYPE_KEY to " ${raw.uppercase()} ",
                WorldBlueprintWorldGenerationRules.STRUCTURES_KEY to " FaLsE ",
            ))
            assertEquals(expected, rules.worldType)
            assertEquals(false, rules.generateStructures)
            assertTrue(rules.invalidKeys.isEmpty())
        }
        assertEquals(true, WorldBlueprintWorldGenerationRules.parse(mapOf(
            WorldBlueprintWorldGenerationRules.STRUCTURES_KEY to "true")).generateStructures)
    }

    @Test
    fun `rejects unknown world type and ambiguous structure toggle`() {
        val rules = WorldBlueprintWorldGenerationRules.parse(mapOf(
            WorldBlueprintWorldGenerationRules.TYPE_KEY to "custom_generator",
            WorldBlueprintWorldGenerationRules.STRUCTURES_KEY to "yes",
        ))

        assertEquals(listOf(WorldBlueprintWorldGenerationRules.TYPE_KEY,
            WorldBlueprintWorldGenerationRules.STRUCTURES_KEY), rules.invalidKeys)
    }
}
