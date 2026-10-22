package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.world.exterior.ExteriorStructureType

class WorldBlueprintStructurePalettesTest {
    private val isBuildableBlock: (String) -> Boolean = {
        it in setOf("STONE_BRICKS", "OAK_PLANKS", "DEEPSLATE_BRICKS")
    }

    @Test
    fun `canonical types and materials are normalized without losing type order`() {
        val palettes = WorldBlueprintStructurePalettes.parse(
            " CASTLE = minecraft:stone_bricks, OAK_PLANKS,stone_bricks ; watchtower=deepslate_bricks ",
            isBuildableBlock)

        assertNotNull(palettes)
        assertNull(palettes!!.error)
        assertEquals(listOf(ExteriorStructureType.CASTLE, ExteriorStructureType.WATCHTOWER),
            palettes.byType.keys.toList())
        assertEquals(listOf("stone_bricks", "oak_planks"), palettes.byType[ExteriorStructureType.CASTLE])
        assertEquals(listOf("deepslate_bricks"), palettes.byType[ExteriorStructureType.WATCHTOWER])
        assertNull(WorldBlueprintStructurePalettes.parse(null, isBuildableBlock))
    }

    @Test
    fun `malformed mapping rejects the entire rule`() {
        listOf("", "castle", "castle=", "castle=stone_bricks;", "custom=stone_bricks",
            "unknown=stone_bricks", "castel=stone_bricks", "castle=stone_bricks;CASTLE=oak_planks",
            "castle=stone_bricks;watchtower=stick", "castle=stone_bricks,",
            List(13) { "castle=stone_bricks" }.joinToString(";"),
        ).forEach { raw ->
            val palettes = WorldBlueprintStructurePalettes.parse(raw, isBuildableBlock)
            assertNotNull(palettes!!.error, raw)
            assertTrue(palettes.byType.isEmpty(), raw)
        }
    }

    @Test
    fun `compatibility counts valid mapping and rejects invalid mapping without partial guidance`() {
        val valid = WorldBlueprint("world", mapRules = mapOf(
            WorldBlueprintStructurePalettes.KEY to "castle=stone_bricks;watchtower=oak_planks"))
        val report = WorldBlueprintCompatibility.check(valid, null, null,
            isBuildableBlock = isBuildableBlock)

        assertEquals(1, report.structuredRuleCount)
        assertTrue(report.issues.isEmpty())
        assertEquals(listOf("stone_bricks"), report.structurePalettes[ExteriorStructureType.CASTLE])

        val invalid = WorldBlueprintCompatibility.check(valid.copy(mapRules = mapOf(
            WorldBlueprintStructurePalettes.KEY to "castle=stone_bricks;watchtower=stick")), null, null,
            isBuildableBlock = isBuildableBlock)
        assertEquals(WorldBlueprintCompatibilityCode.INVALID_STRUCTURE_PALETTES_RULE, invalid.issues.single().code)
        assertTrue(invalid.structurePalettes.isEmpty())
    }

    @Test
    fun `structure preview prefers type palette and falls back to global palette`() {
        val draft = WorldBlueprint("world", mapRules = mapOf(
            "required_exterior_types" to "castle,watchtower",
            WorldBlueprintArchitecturePalette.KEY to "oak_planks",
            WorldBlueprintStructurePalettes.KEY to "castle=stone_bricks"))
        val castle = WorldBlueprintStructurePlanner.compile(draft, "castle", "keep",
            null, null, null, isBuildableBlock = isBuildableBlock)
        val watchtower = WorldBlueprintStructurePlanner.compile(draft, "watchtower", "tower",
            null, null, null, isBuildableBlock = isBuildableBlock)

        assertEquals(listOf("stone_bricks"), castle.architecturePalette)
        assertEquals("${WorldBlueprintStructurePalettes.KEY}:castle", castle.architecturePaletteSource)
        assertEquals(listOf("oak_planks"), watchtower.architecturePalette)
        assertEquals(WorldBlueprintArchitecturePalette.KEY, watchtower.architecturePaletteSource)
        assertNull(castle.architecturePaletteIssue)
    }

    @Test
    fun `invalid type mapping reports an issue and never applies a partial override`() {
        val draft = WorldBlueprint("world", mapRules = mapOf(
            "required_exterior_types" to "castle",
            WorldBlueprintArchitecturePalette.KEY to "oak_planks",
            WorldBlueprintStructurePalettes.KEY to "castle=stone_bricks;watchtower=stick"))
        val preview = WorldBlueprintStructurePlanner.compile(draft, "castle", "keep",
            null, null, null, isBuildableBlock = isBuildableBlock)

        assertEquals(listOf("oak_planks"), preview.architecturePalette)
        assertEquals(WorldBlueprintArchitecturePalette.KEY, preview.architecturePaletteSource)
        assertTrue(preview.architecturePaletteIssue?.contains(WorldBlueprintStructurePalettes.KEY) == true)
    }
}
