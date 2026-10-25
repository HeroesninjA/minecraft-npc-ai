package ro.ainpc.world

import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.UUID

class WorldBlueprintArchitectureGableIntegrationTest {
    @TempDir
    lateinit var directory: Path

    private val bounds = WorldBlueprintRegionBounds(0, 64, 0, 4, 69, 6)

    @Test
    fun `structured validation and planner select gable without altering cuboid defaults`() {
        assertTrue(WorldBlueprintCompatibility.structuredRuleSyntaxIssues(
            WorldBlueprintArchitectureTemplate.KEY, " GABLE_SHELL ").isEmpty())
        val gable = preview(draft())
        val plan = requireNotNull(gable.architecturePlan)

        assertNull(gable.architecturePlanIssue)
        assertEquals("gable_shell", plan.templateId)
        assertEquals(118, plan.blocks.size)
        assertEquals("bricks", plan.blocks.single { it.x == 0 && it.y == 67 && it.z == 3 }.blockId)
        assertEquals("bricks", plan.blocks.single { it.x == 2 && it.y == 69 && it.z == 3 }.blockId)
        assertEquals("oak_planks", plan.blocks.single { it.x == 2 && it.y == 68 && it.z == 0 }.blockId)
        val cuboid = requireNotNull(preview(draft(mapOf(WorldBlueprintArchitectureTemplate.KEY to "cuboid_shell")))
            .architecturePlan)
        assertEquals("cuboid_shell", cuboid.templateId)
        assertTrue(cuboid.blocks.any { it.x == 0 && it.y == 69 && it.z == 3 })
        assertFalse(plan.blocks.any { it.x == 0 && it.y == 69 && it.z == 3 })
    }

    @Test
    fun `effective type palette is used and role mismatches still stop gable compilation`() {
        val single = draft(mapOf(WorldBlueprintStructurePalettes.KEY to "castle=oak_planks"))
        val mismatch = preview(single)
        assertNull(mismatch.architecturePlan)
        assertTrue(requireNotNull(mismatch.architecturePlanIssue).contains("paleta efectiva"))

        val selected = preview(single.copy(mapRules = single.mapRules - WorldBlueprintArchitectureMaterials.KEY))
        assertNull(selected.architecturePlanIssue)
        assertEquals(setOf("oak_planks"), requireNotNull(selected.architecturePlan).blocks.map { it.blockId }.toSet())
    }

    @Test
    fun `gable remains opt in and bounds are required for compilation`() {
        val withoutBounds = preview(draft(), null)
        assertNull(withoutBounds.architecturePlan)
        assertNull(withoutBounds.architecturePlanIssue)
        val withoutTemplate = draft().let { it.copy(mapRules = it.mapRules - WorldBlueprintArchitectureTemplate.KEY) }
        assertNull(preview(withoutTemplate).architecturePlan)
    }

    @Test
    fun `insufficient eave height and entrance clearance become preflight conflicts`() {
        val cases = listOf(
            draft() to bounds.copy(maxY = 67),
            draft(mapOf(WorldBlueprintArchitectureEntrance.KEY to "north")) to bounds.copy(maxY = 68),
        )
        cases.forEach { (blueprint, selected) ->
            val result = preview(blueprint, selected)
            assertNull(result.architecturePlan)
            assertNotNull(result.architecturePlanIssue)
            assertTrue(WorldBlueprintStructureConfigurationPreflightInspector.inspect(result, emptyMap())
                .conflicts.contains("sablon arhitectural necompilabil"))
        }
    }

    @Test
    fun `gable target inspection includes actual roof and clearance but never space above slopes`() {
        val plan = requireNotNull(preview(draft(mapOf(
            WorldBlueprintArchitectureInteriorClearance.KEY to "true",
            WorldBlueprintArchitectureEntrance.KEY to "north",
        ))).architecturePlan)
        val observedPositions = mutableSetOf<Triple<Int, Int, Int>>()
        val targets = WorldBlueprintArchitectureTargetInspector.inspect(plan, 0, 100) { blockX, blockY, blockZ ->
            assertTrue(observedPositions.add(Triple(blockX, blockY, blockZ)))
            when (Triple(blockX, blockY, blockZ)) {
                Triple(1, 65, 1) -> WorldBlueprintArchitectureObservedBlock(true, "chest", true)
                Triple(2, 65, 0) -> WorldBlueprintArchitectureObservedBlock(true, "stone", false)
                Triple(2, 69, 3) -> null
                else -> WorldBlueprintArchitectureObservedBlock(false, "air", false)
            }
        }

        assertEquals(168, targets.totalCount)
        assertEquals(plan.blocks.map { Triple(it.x, it.y, it.z) }.toSet(), observedPositions)
        assertFalse(Triple(1, 69, 1) in observedPositions)
        assertEquals(WorldBlueprintArchitectureClearancePreview(50, 1, 0),
            targets.clearances[WorldBlueprintArchitectureBlockPurpose.INTERIOR_CLEARANCE])
        assertEquals(WorldBlueprintArchitectureClearancePreview(2, 1, 0),
            targets.clearances[WorldBlueprintArchitectureBlockPurpose.ENTRANCE_CLEARANCE])
        assertEquals(1, targets.unavailableCount)
        assertEquals(1, targets.persistentStateCount)
        val preflight = WorldBlueprintStructurePreflightInspector.inspect(true, 0, emptyList(),
            WorldBlueprintPlacementAssessment(0, 100, true, true), WorldBlueprintChunkCoverage(1, 1, 1),
            architectureTargets = targets)
        assertTrue(preflight.conflicts.containsAll(listOf("interior arhitectural ocupat",
            "intrare arhitecturala blocata", "tinte arhitecturale cu date persistente")))
        assertTrue(preflight.unknowns.contains("tinte arhitecturale neobservabile"))
    }

    @Test
    fun `gable recipe exports and verifies all sloped operations with the existing schema`() {
        val plan = requireNotNull(preview(draft(mapOf(
            WorldBlueprintArchitectureInteriorClearance.KEY to "true",
            WorldBlueprintArchitectureEntrance.KEY to "north",
        ))).architecturePlan)
        val worldId = UUID.fromString("1bfb0dad-350a-4eac-a3b1-52b0fe112cf5")
        val export = WorldBlueprintArchitectureExport.create("world", worldId, "a".repeat(64),
            "castle", "citadel", bounds, plan)
        val document = JsonParser.parseString(export.json).asJsonObject
        assertEquals(1, document.get("schema_version").asInt)
        val recipe = document.getAsJsonObject("plan")
        assertEquals("gable_shell", recipe.get("template_id").asString)
        assertEquals(plan.blocks.size, recipe.getAsJsonArray("operations").size())
        assertEquals(plan.blocks.map { Triple(it.x, it.y, it.z) }, recipe.getAsJsonArray("operations").map { element ->
            val operation = element.asJsonObject
            Triple(operation.get("x").asInt, operation.get("y").asInt, operation.get("z").asInt)
        })
        val saved = export.write(directory, export.fingerprint)
        assertEquals(saved.path, export.verify(directory, export.fingerprint))
        val cuboid = WorldBlueprintArchitectureTemplate.plan(bounds, listOf("stone", "oak_planks", "bricks"),
            plan.materials, plan.entrance, true)
        val other = WorldBlueprintArchitectureExport.create("world", worldId, "a".repeat(64),
            "castle", "citadel", bounds, cuboid)
        assertNotEquals(export.fingerprint, other.fingerprint)
        assertThrows(IllegalArgumentException::class.java) { other.verify(directory, export.fingerprint) }
    }

    @Test
    fun `AI template proposals use the same gable validation without changing the source draft`() {
        val base = draft()
        val request = WorldBlueprintSuggestionRequest("world", WorldBlueprintSuggestionField.MAP_RULE,
            WorldBlueprintArchitectureTemplate.KEY, "acoperis in doua ape", "base-hash")
        val provenance = WorldBlueprintSuggestionProvenance("openai", "test-model", 100L,
            WorldBlueprintSuggestionParser.requestFingerprint(request.instruction))
        val prompt = WorldBlueprintSuggestionParser.prompt(request, base)
        assertTrue(prompt.contains("gable_shell"))
        val suggestion = WorldBlueprintSuggestionParser.parse(request, base,
            "{\"text\":\"GABLE_SHELL\"}", provenance)
        assertEquals("GABLE_SHELL", suggestion.candidate.mapRules[WorldBlueprintArchitectureTemplate.KEY])
        assertEquals(" GABLE_SHELL ", base.mapRules[WorldBlueprintArchitectureTemplate.KEY])
        assertEquals("gable_shell", requireNotNull(preview(suggestion.candidate).architecturePlan).templateId)
        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintSuggestionParser.parse(request, base, "{\"text\":\"unknown_roof\"}", provenance)
        }
    }

    @Test
    fun `small compiled gables match the complete geometric boundary or covered volume`() {
        for (width in 3..8) {
            for (depth in 3..6) {
                for (eaveHeight in 3..5) {
                    for (clearInterior in listOf(false, true)) {
                        val maximumRise = (width - 1) / 2
                        val selected = WorldBlueprintRegionBounds(0, 0, 0, width - 1,
                            eaveHeight + maximumRise - 1, depth - 1)
                        val expected = mutableSetOf<Triple<Int, Int, Int>>()
                        for (blockX in 0 until width) {
                            val localRoofY = eaveHeight - 1 + minOf(blockX, width - 1 - blockX)
                            for (blockY in 0..localRoofY) {
                                for (blockZ in 0 until depth) {
                                    if (clearInterior || blockY == 0 || blockY == localRoofY ||
                                        blockX == 0 || blockX == width - 1 || blockZ == 0 || blockZ == depth - 1) {
                                        expected.add(Triple(blockX, blockY, blockZ))
                                    }
                                }
                            }
                        }
                        val plan = requireNotNull(preview(draft(mapOf(
                            WorldBlueprintArchitectureInteriorClearance.KEY to clearInterior.toString())), selected)
                            .architecturePlan)
                        assertEquals(expected.size, plan.blocks.size)
                        assertEquals(expected, plan.blocks.map { Triple(it.x, it.y, it.z) }.toSet())
                    }
                }
            }
        }
    }

    @Test
    fun `explicit cuboid selection keeps the legacy default plan and export bytes`() {
        val palette = listOf("stone", "oak_planks", "bricks")
        val materials = WorldBlueprintArchitectureMaterialRoles("stone", "oak_planks", "bricks")
        val legacy = WorldBlueprintArchitectureTemplate.plan(bounds, palette, materials,
            WorldBlueprintArchitectureEntrance.NORTH, true)
        val explicit = WorldBlueprintArchitectureTemplate.plan(bounds, palette, materials,
            WorldBlueprintArchitectureEntrance.NORTH, true, " CUBOID_SHELL ")
        assertEquals(legacy, explicit)
        val worldId = UUID.fromString("1bfb0dad-350a-4eac-a3b1-52b0fe112cf5")
        val originalExport = WorldBlueprintArchitectureExport.create("world", worldId, "a".repeat(64),
            "castle", "citadel", bounds, legacy)
        val selectedExport = WorldBlueprintArchitectureExport.create("world", worldId, "a".repeat(64),
            "castle", "citadel", bounds, explicit)
        assertEquals(originalExport.fingerprint, selectedExport.fingerprint)
        assertEquals(originalExport.json, selectedExport.json)
    }

    private fun draft(overrides: Map<String, String> = emptyMap()) = WorldBlueprint("world", mapRules = mapOf(
        "required_exterior_types" to "castle",
        WorldBlueprintArchitectureTemplate.KEY to " GABLE_SHELL ",
        WorldBlueprintArchitecturePalette.KEY to "stone,oak_planks,bricks",
        WorldBlueprintArchitectureMaterials.KEY to "floor=stone;wall=oak_planks;roof=bricks",
    ) + overrides)

    private fun preview(blueprint: WorldBlueprint, selected: WorldBlueprintRegionBounds? = bounds) =
        WorldBlueprintStructurePlanner.compile(blueprint, "castle", "citadel", emptyList(), emptyList(), emptyList(),
            selected, isBuildableBlock = { it in setOf("STONE", "OAK_PLANKS", "BRICKS") })
}
