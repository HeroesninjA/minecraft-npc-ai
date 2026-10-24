package ro.ainpc.world

import org.bukkit.World
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
        assertFalse(rules.environmentConfigured)
        assertNull(rules.environment)
        assertFalse(rules.structuresConfigured)
        assertNull(rules.generateStructures)
        assertTrue(rules.invalidKeys.isEmpty())
    }

    @Test
    fun `parses supported vanilla environments without custom dimensions`() {
        val environments = mapOf("normal" to World.Environment.NORMAL, "nether" to World.Environment.NETHER,
            "the_end" to World.Environment.THE_END)
        environments.forEach { (raw, expected) ->
            val rules = WorldBlueprintWorldGenerationRules.parse(mapOf(
                WorldBlueprintWorldGenerationRules.ENVIRONMENT_KEY to " ${raw.uppercase()} ",
            ))
            assertTrue(rules.environmentConfigured)
            assertEquals(expected, rules.environment)
            assertTrue(rules.invalidKeys.isEmpty())
        }
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
            WorldBlueprintWorldGenerationRules.ENVIRONMENT_KEY to "custom",
            WorldBlueprintWorldGenerationRules.STRUCTURES_KEY to "yes",
        ))

        assertEquals(listOf(WorldBlueprintWorldGenerationRules.TYPE_KEY,
            WorldBlueprintWorldGenerationRules.ENVIRONMENT_KEY,
            WorldBlueprintWorldGenerationRules.STRUCTURES_KEY), rules.invalidKeys)
    }

    @Test
    fun `rejects overworld type when creating a nether or end world`() {
        for (environment in listOf("nether", "the_end")) {
            val rules = WorldBlueprintWorldGenerationRules.parse(mapOf(
                WorldBlueprintWorldGenerationRules.TYPE_KEY to "flat",
                WorldBlueprintWorldGenerationRules.ENVIRONMENT_KEY to environment,
            ))
            assertTrue(rules.incompatibleType)
            assertEquals(listOf(WorldBlueprintWorldGenerationRules.TYPE_KEY), rules.invalidKeys)
        }
    }
}
