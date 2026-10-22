package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorldBlueprintStructureBiomeRulesTest {
    @Test
    fun `canonicalizes namespaced and vanilla biome ids before comparing observations`() {
        val rules = WorldBlueprintStructureBiomeRules.parse(mapOf(
            WorldBlueprintStructureBiomeRules.ALLOWED_KEY to " Plains, custom:Moon_Forest, plains ",
            WorldBlueprintStructureBiomeRules.FORBIDDEN_KEY to "minecraft:desert",
        ))

        assertTrue(rules.isValid)
        assertEquals(setOf("minecraft:plains", "custom:moon_forest"), rules.allowed)
        assertEquals(setOf("minecraft:desert"), rules.forbidden)
        assertEquals(setOf("minecraft:desert", "minecraft:swamp") to setOf("minecraft:desert"),
            rules.violations(setOf("minecraft:plains", "minecraft:desert", "minecraft:swamp")))
    }

    @Test
    fun `invalid entries reject the whole individual list`() {
        listOf("", "plains,", "minecraft:plains,bad key", "a:b:c", List(65) { "plains" }.joinToString()).forEach { raw ->
            val rules = WorldBlueprintStructureBiomeRules.parse(mapOf(
                WorldBlueprintStructureBiomeRules.ALLOWED_KEY to raw,
                WorldBlueprintStructureBiomeRules.FORBIDDEN_KEY to "desert",
            ))

            assertFalse(rules.isValid)
            assertEquals(listOf(WorldBlueprintStructureBiomeRules.ALLOWED_KEY), rules.invalidKeys)
            assertEquals(null, rules.allowed)
            assertEquals(setOf("minecraft:desert"), rules.forbidden)
            assertEquals(emptySet<String>() to emptySet<String>(), rules.violations(setOf("minecraft:desert")))
        }
    }

    @Test
    fun `same biome cannot be allowed and forbidden even with different spelling`() {
        val rules = WorldBlueprintStructureBiomeRules.parse(mapOf(
            WorldBlueprintStructureBiomeRules.ALLOWED_KEY to "plains",
            WorldBlueprintStructureBiomeRules.FORBIDDEN_KEY to "MINECRAFT:PLAINS",
        ))

        assertFalse(rules.isValid)
        assertEquals(setOf("minecraft:plains"), rules.conflicts)
    }
}
