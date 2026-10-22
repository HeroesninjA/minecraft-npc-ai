package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class WorldBlueprintArchitecturePaletteTest {
    private val isBuildableBlock: (String) -> Boolean = { it in setOf("STONE_BRICKS", "OAK_PLANKS") }

    @Test
    fun `valid block palette normalizes names and removes duplicates`() {
        val palette = WorldBlueprintArchitecturePalette.parse(" Stone_Bricks, minecraft:oak_planks, STONE_BRICKS ", isBuildableBlock)

        assertNotNull(palette)
        assertNull(palette!!.error)
        assertEquals(listOf("stone_bricks", "oak_planks"), palette.blockIds)
    }

    @Test
    fun `items air custom namespaces empty entries and excessive lists are rejected`() {
        listOf("", "stone_bricks,", "stick", "air", "unknown_block", "custom:stone_bricks",
            List(17) { "stone_bricks" }.joinToString()).forEach { raw ->
            val palette = WorldBlueprintArchitecturePalette.parse(raw, isBuildableBlock)
            assertNotNull(palette)
            assertNotNull(palette!!.error, raw)
            assertEquals(emptyList<String>(), palette.blockIds)
        }
        assertNull(WorldBlueprintArchitecturePalette.parse(null))
    }
}
