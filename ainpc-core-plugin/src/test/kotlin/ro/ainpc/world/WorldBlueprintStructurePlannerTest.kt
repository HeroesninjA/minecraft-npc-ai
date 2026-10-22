package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigInteger

class WorldBlueprintStructurePlannerTest {
    private val draft = WorldBlueprint("world", mapRules = mapOf("required_exterior_types" to "castle"))

    @Test
    fun `catalog preview normalizes the base id and lists scoped places and nodes`() {
        val preview = WorldBlueprintStructurePlanner.compile(draft, "CASTLE", "Cetate-Nord",
            emptyList(), emptyList(), emptyList())

        assertEquals("world", preview.worldName)
        assertEquals("cetate_nord", preview.structure.regionId())
        assertTrue(preview.structure.plannedPlaces().contains("cetate_nord:main_gate"))
        assertTrue(preview.structure.plannedNodes().contains("cetate_nord:main_gate [entrance]"))
        assertTrue(preview.structure.warnings().any { "normalizat" in it })
        assertTrue(preview.mappingAvailable)
        assertTrue(preview.collisions.isEmpty())
        assertEquals(null, preview.overlappingRegionIds)
        assertTrue(preview.architecturePalette.isEmpty())
        assertEquals(null, preview.architecturePaletteIssue)
        assertFalse(preview.storyRegionReferenced)
        assertEquals(null, preview.storyRegionRuleIssue)
    }

    @Test
    fun `planned story reference matches the normalized regional id without requiring mapping`() {
        val withStory = draft.copy(story = "Cetatea pazeste drumul", mapRules = draft.mapRules +
            ("story_region_ids" to "village,cetate_nord,cetate_nord"))
        val preview = WorldBlueprintStructurePlanner.compile(withStory, "castle", "Cetate-Nord",
            null, null, null)

        assertFalse(preview.mappingAvailable)
        assertTrue(preview.storyRegionReferenced)
        assertEquals(null, preview.storyRegionRuleIssue)
    }

    @Test
    fun `story reference remains exact and an invalid rule cannot create a planned link`() {
        val wrongCase = draft.copy(mapRules = draft.mapRules +
            ("story_region_ids" to "Cetate_Nord"))
        val invalid = draft.copy(mapRules = draft.mapRules +
            ("story_region_ids" to "cetate_nord,"))

        val unmatched = WorldBlueprintStructurePlanner.compile(wrongCase, "castle", "Cetate-Nord",
            emptyList(), emptyList(), emptyList())
        val rejected = WorldBlueprintStructurePlanner.compile(invalid, "castle", "Cetate-Nord",
            emptyList(), emptyList(), emptyList())

        assertFalse(unmatched.storyRegionReferenced)
        assertEquals(null, unmatched.storyRegionRuleIssue)
        assertFalse(rejected.storyRegionReferenced)
        assertTrue(rejected.storyRegionRuleIssue?.contains("story_region_ids") == true)
    }

    @Test
    fun `invalid architecture palette is not presented as build guidance`() {
        val invalid = draft.copy(mapRules = draft.mapRules +
            (WorldBlueprintArchitecturePalette.KEY to List(17) { "stone_bricks" }.joinToString()))
        val preview = WorldBlueprintStructurePlanner.compile(invalid, "castle", "citadel",
            emptyList(), emptyList(), emptyList())

        assertTrue(preview.architecturePalette.isEmpty())
        assertTrue(preview.architecturePaletteIssue?.contains(WorldBlueprintArchitecturePalette.KEY) == true)
    }

    @Test
    fun `base id checks cover other worlds and existing children without matching similar prefixes`() {
        val preview = WorldBlueprintStructurePlanner.compile(draft, "castle", "Cetate-Nord",
            listOf(region("cetate_nord", "nether")),
            listOf(place("cetate_nord:old_room", "legacy"), place("cetate_nord_2:room", "cetate_nord_2")),
            listOf(node("legacy_node", "cetate_nord"), node("cetate_nord_2:guard", "cetate_nord_2")))

        assertEquals(listOf(WorldBlueprintMappingKind.REGION, WorldBlueprintMappingKind.PLACE,
            WorldBlueprintMappingKind.NODE), preview.collisions.map { it.kind })
        assertEquals("nether", preview.collisions.first().worldName)
        assertEquals(listOf("cetate_nord", "cetate_nord:old_room", "legacy_node"),
            preview.collisions.map { it.id })
    }

    @Test
    fun `incomplete mapping keeps known collisions without claiming a free base id`() {
        val unavailable = WorldBlueprintStructurePlanner.compile(draft, "castle", "citadel", null, null, null)
        val partial = WorldBlueprintStructurePlanner.compile(draft, "castle", "citadel",
            listOf(region("citadel")), emptyList(), null)

        assertFalse(unavailable.mappingAvailable)
        assertTrue(unavailable.collisions.isEmpty())
        assertFalse(partial.mappingAvailable)
        assertEquals("citadel", partial.collisions.single().id)
    }

    @Test
    fun `region placement uses inclusive bounds and only regions in the draft world`() {
        val bounds = WorldBlueprintRegionBounds(10, 64, 10, 20, 80, 20)
        val preview = WorldBlueprintStructurePlanner.compile(draft, "castle", "citadel",
            listOf(
                regionBox("touches", "world", 20, 70, 12, 25, 90, 18),
                regionBox("above", "world", 12, 81, 12, 18, 90, 18),
                regionBox("beside", "world", 21, 70, 12, 25, 90, 18),
                regionBox("foreign", "nether", 10, 64, 10, 20, 80, 20),
            ), emptyList(), emptyList(), bounds)

        assertTrue(preview.mappingAvailable)
        assertEquals(listOf("touches"), preview.overlappingRegionIds)
        assertTrue(preview.collisions.isEmpty())
    }

    @Test
    fun `unknown region mapping leaves geometry unresolved`() {
        val bounds = WorldBlueprintRegionBounds(10, 64, 10, 20, 80, 20)
        val preview = WorldBlueprintStructurePlanner.compile(draft, "castle", "citadel",
            null, emptyList(), emptyList(), bounds)

        assertFalse(preview.mappingAvailable)
        assertEquals(bounds, preview.bounds)
        assertEquals(null, preview.overlappingRegionIds)
    }

    @Test
    fun `coordinate input requires six integers in ascending bounds`() {
        assertEquals(WorldBlueprintRegionBounds(-20, 0, 1, 20, 40, 30),
            WorldBlueprintRegionBounds.parse(listOf("-20", "0", "1", "20", "40", "30")))
        listOf(
            listOf("0", "0", "0", "10", "10"),
            listOf("0", "0", "0", "10", "bad", "10"),
            listOf("10", "0", "0", "0", "10", "10"),
            listOf("0", "10", "0", "10", "0", "10"),
        ).forEach { values ->
            assertThrows(IllegalArgumentException::class.java) { WorldBlueprintRegionBounds.parse(values) }
        }
    }

    @Test
    fun `region size counts inclusive block positions without overflow`() {
        val small = WorldBlueprintRegionBounds(0, 64, 0, 9, 67, 19)
        val point = WorldBlueprintRegionBounds(5, 70, -3, 5, 70, -3)
        val extreme = WorldBlueprintRegionBounds(Int.MIN_VALUE, Int.MIN_VALUE, Int.MIN_VALUE,
            Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE)

        assertEquals(10L, small.width)
        assertEquals(4L, small.height)
        assertEquals(20L, small.depth)
        assertEquals(BigInteger.valueOf(800), small.blockPositions)
        assertEquals(BigInteger.ONE, point.blockPositions)
        assertEquals(4_294_967_296L, extreme.width)
        assertEquals(BigInteger.ONE.shiftLeft(96), extreme.blockPositions)
    }

    @Test
    fun `placement inspection treats the maximum build height as exclusive`() {
        val bounds = WorldBlueprintRegionBounds(10, -64, 10, 20, 319, 20)
        val valid = WorldBlueprintPlacementInspector.inspect(bounds, -64, 320) { _, _ -> true }
        val tooHigh = WorldBlueprintPlacementInspector.inspect(bounds.copy(maxY = 320), -64, 320) { _, _ -> true }
        val tooLow = WorldBlueprintPlacementInspector.inspect(bounds.copy(minY = -65), -64, 320) { _, _ -> true }

        assertTrue(valid.withinBuildHeight)
        assertFalse(tooHigh.withinBuildHeight)
        assertFalse(tooLow.withinBuildHeight)
        assertEquals(320, valid.maxBuildYExclusive)
    }

    @Test
    fun `placement inspection checks all horizontal corners including boundaries`() {
        val bounds = WorldBlueprintRegionBounds(10, 64, 10, 20, 80, 20)
        val checked = mutableSetOf<Pair<Int, Int>>()
        val valid = WorldBlueprintPlacementInspector.inspect(bounds, -64, 320) { blockX, blockZ ->
            checked += blockX to blockZ
            true
        }
        val outside = WorldBlueprintPlacementInspector.inspect(bounds, -64, 320) { blockX, blockZ ->
            blockX != 20 || blockZ != 20
        }

        assertTrue(valid.withinWorldBorder)
        assertEquals(setOf(10 to 10, 10 to 20, 20 to 10, 20 to 20), checked)
        assertFalse(outside.withinWorldBorder)
    }

    @Test
    fun `only declared unambiguous catalog requirements can produce a structure preview`() {
        val rejected = listOf(
            draft to "watchtower",
            draft to "castel",
            draft.copy(mapRules = draft.mapRules + ("forbidden_exterior_types" to "castle")) to "castle",
            draft.copy(mapRules = mapOf("required_exterior_types" to "castle,unknown")) to "castle",
            draft.copy(mapRules = draft.mapRules + ("forbidden_exterior_types" to "camp,unknown")) to "castle",
            draft.copy(mapRules = mapOf("required_exterior_types" to "custom")) to "custom",
        )
        rejected.forEach { (blueprint, type) ->
            assertThrows(IllegalArgumentException::class.java) {
                WorldBlueprintStructurePlanner.compile(blueprint, type, "citadel", null, null, null)
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintStructurePlanner.compile(draft, "castle", "!!!", null, null, null)
        }
    }

    private fun region(id: String, world: String = "world") =
        WorldRegionInfo(id, id, world, "castle", 0, 0, 0, 1, 1, 1, emptyList(), null, null, emptyList())

    private fun regionBox(
        id: String, world: String, minX: Int, minY: Int, minZ: Int, maxX: Int, maxY: Int, maxZ: Int,
    ) = WorldRegionInfo(id, id, world, "castle", minX, minY, minZ, maxX, maxY, maxZ,
        emptyList(), null, null, emptyList())

    private fun place(id: String, regionId: String) =
        WorldPlaceInfo(id, regionId, id, "world", PlaceType.CUSTOM, 0, 0, 0, 1, 1, 1,
            emptyList(), null, true, emptyMap())

    private fun node(id: String, regionId: String) =
        WorldNodeInfo(id, regionId, null, "interaction", "world", 0.0, 0.0, 0.0, 1.0, emptyMap())
}
