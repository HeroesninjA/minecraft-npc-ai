package ro.ainpc.world

import org.bukkit.World
import org.bukkit.WorldType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorldBlueprintWorldCreationRuntimeCheckTest {
    @Test
    fun `compares only explicitly configured observable options`() {
        val options = WorldBlueprintWorldCreationOptions(42L, WorldType.FLAT, World.Environment.NORMAL, false)
        val matching = WorldBlueprintWorldCreationRuntimeCheck.compare(options,
            WorldBlueprintWorldCreationRuntimeSnapshot(42L, World.Environment.NORMAL, false))

        assertEquals(listOf("world_seed", "world_environment", "world_generate_structures"), matching.map { it.rule })
        assertTrue(matching.all { it.matches })
        assertEquals("42", matching.first().requested)
        assertEquals("42", matching.first().observed)
        assertFalse(matching.any { it.rule == "world_type" })
    }

    @Test
    fun `reports present mismatches without asserting the historical creator`() {
        val options = WorldBlueprintWorldCreationOptions(42L, environment = World.Environment.NETHER,
            generateStructures = false)
        val observed = WorldBlueprintWorldCreationRuntimeSnapshot(43L, World.Environment.NORMAL, true)

        val checks = WorldBlueprintWorldCreationRuntimeCheck.compare(options, observed)
        assertTrue(checks.none { it.matches })
        assertEquals(listOf("43", "normal", "true"), checks.map { it.observed })
        assertTrue(WorldBlueprintWorldCreationRuntimeCheck.compare(WorldBlueprintWorldCreationOptions(), observed).isEmpty())
    }
}
