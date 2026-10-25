package ro.ainpc.world

import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

class WorldBlueprintArchitectureGableAxisIntegrationTest {
    @TempDir
    lateinit var directory: Path

    private val bounds = WorldBlueprintRegionBounds(100, 64, 100, 106, 70, 104)

    @Test
    fun `structured validation and planner select the explicit X ridge with effective roles`() {
        assertTrue(WorldBlueprintCompatibility.structuredRuleSyntaxIssues(
            WorldBlueprintArchitectureTemplate.KEY, " GABLE_SHELL_X ").isEmpty())
        val result = preview(draft())
        val plan = requireNotNull(result.architecturePlan)

        assertNull(result.architecturePlanIssue)
        assertEquals("gable_shell_x", plan.templateId)
        assertEquals(138, plan.blocks.size)
        assertEquals("bricks", plan.blocks.single { it.x == 103 && it.y == 68 && it.z == 100 }.blockId)
        assertEquals("bricks", plan.blocks.single { it.x == 103 && it.y == 70 && it.z == 102 }.blockId)
        assertEquals("oak_planks", plan.blocks.single { it.x == 100 && it.y == 69 && it.z == 102 }.blockId)
        assertFalse(plan.blocks.any { it.x == 103 && it.y == 70 && it.z == 100 })

        val single = draft(mapOf(WorldBlueprintStructurePalettes.KEY to "castle=oak_planks"))
        val mismatch = preview(single)
        assertNull(mismatch.architecturePlan)
        assertTrue(requireNotNull(mismatch.architecturePlanIssue).contains("paleta efectiva"))
        val matched = preview(single.copy(mapRules = single.mapRules - WorldBlueprintArchitectureMaterials.KEY))
        assertEquals(setOf("oak_planks"), requireNotNull(matched.architecturePlan).blocks.map { it.blockId }.toSet())
    }

    @Test
    fun `the roof span follows depth rather than width and failures remain preflight conflicts`() {
        val narrowDepth = WorldBlueprintRegionBounds(0, 0, 0, 6, 4, 2)
        assertNull(preview(draft(), narrowDepth).architecturePlanIssue)
        val previousAxis = preview(draft(mapOf(WorldBlueprintArchitectureTemplate.KEY to "gable_shell")), narrowDepth)
        assertNull(previousAxis.architecturePlan)

        val deepSpan = WorldBlueprintRegionBounds(0, 0, 0, 2, 4, 6)
        val rejected = preview(draft(), deepSpan)
        assertNull(rejected.architecturePlan)
        assertTrue(WorldBlueprintStructureConfigurationPreflightInspector.inspect(rejected, emptyMap())
            .conflicts.contains("sablon arhitectural necompilabil"))
        assertNull(preview(draft(mapOf(WorldBlueprintArchitectureTemplate.KEY to "gable_shell")), deepSpan)
            .architecturePlanIssue)
    }

    @Test
    fun `the new axis remains opt in and cannot compile without bounds`() {
        val withoutBounds = preview(draft(), null)
        assertNull(withoutBounds.architecturePlan)
        assertNull(withoutBounds.architecturePlanIssue)
        val withoutTemplate = draft().let { it.copy(mapRules = it.mapRules - WorldBlueprintArchitectureTemplate.KEY) }
        assertNull(preview(withoutTemplate).architecturePlan)
    }

    @Test
    fun `target scans preserve original cardinal clearance labels and never inspect above the Z slope`() {
        val plan = requireNotNull(preview(draft(mapOf(
            WorldBlueprintArchitectureInteriorClearance.KEY to "true",
            WorldBlueprintArchitectureEntrance.KEY to "north",
        ))).architecturePlan)
        val observedPositions = mutableSetOf<Triple<Int, Int, Int>>()
        val targets = WorldBlueprintArchitectureTargetInspector.inspect(plan, 0, 100) { blockX, blockY, blockZ ->
            assertTrue(observedPositions.add(Triple(blockX, blockY, blockZ)))
            when (Triple(blockX, blockY, blockZ)) {
                Triple(101, 65, 101) -> WorldBlueprintArchitectureObservedBlock(true, "chest", true)
                Triple(103, 65, 100) -> WorldBlueprintArchitectureObservedBlock(true, "stone", false)
                Triple(103, 70, 102) -> null
                else -> WorldBlueprintArchitectureObservedBlock(false, "air", false)
            }
        }

        assertEquals(WorldBlueprintArchitectureEntrance.NORTH, plan.entrance)
        assertEquals(203, targets.totalCount)
        assertEquals(plan.blocks.map { Triple(it.x, it.y, it.z) }.toSet(), observedPositions)
        assertFalse(Triple(103, 70, 100) in observedPositions)
        assertEquals(WorldBlueprintArchitectureClearancePreview(65, 1, 0),
            targets.clearances[WorldBlueprintArchitectureBlockPurpose.INTERIOR_CLEARANCE])
        assertEquals(WorldBlueprintArchitectureClearancePreview(2, 1, 0),
            targets.clearances[WorldBlueprintArchitectureBlockPurpose.ENTRANCE_CLEARANCE])
        val preflight = WorldBlueprintStructurePreflightInspector.inspect(true, 0, emptyList(),
            WorldBlueprintPlacementAssessment(0, 100, true, true), WorldBlueprintChunkCoverage(1, 1, 1),
            architectureTargets = targets)
        assertTrue(preflight.conflicts.containsAll(listOf("interior arhitectural ocupat",
            "intrare arhitecturala blocata", "tinte arhitecturale cu date persistente")))
        assertTrue(preflight.unknowns.contains("tinte arhitecturale neobservabile"))
    }

    @Test
    fun `the X ridge exports original coordinates and entrance with a distinct schema one fingerprint`() {
        val configured = mapOf(WorldBlueprintArchitectureInteriorClearance.KEY to "true",
            WorldBlueprintArchitectureEntrance.KEY to "north")
        val plan = requireNotNull(preview(draft(configured)).architecturePlan)
        val previous = requireNotNull(preview(draft(configured +
            (WorldBlueprintArchitectureTemplate.KEY to "gable_shell"))).architecturePlan)
        val worldId = UUID.fromString("1bfb0dad-350a-4eac-a3b1-52b0fe112cf5")
        val export = WorldBlueprintArchitectureExport.create("world", worldId, "a".repeat(64),
            "castle", "citadel", bounds, plan)
        val previousExport = WorldBlueprintArchitectureExport.create("world", worldId, "a".repeat(64),
            "castle", "citadel", bounds, previous)
        assertNotEquals(previousExport.fingerprint, export.fingerprint)
        val document = JsonParser.parseString(export.json).asJsonObject
        assertEquals(1, document.get("schema_version").asInt)
        val recipe = document.getAsJsonObject("plan")
        assertEquals("gable_shell_x", recipe.get("template_id").asString)
        assertEquals("north", recipe.get("entrance").asString)
        val exportedBlocks = recipe.getAsJsonArray("operations").map { element ->
            val operation = element.asJsonObject
            Triple(operation.get("x").asInt, operation.get("y").asInt, operation.get("z").asInt)
        }
        assertEquals(plan.blocks.map { Triple(it.x, it.y, it.z) }, exportedBlocks)
        val previousFile = previousExport.write(directory, previousExport.fingerprint).path
        val previousBytes = Files.readAllBytes(previousFile)
        assertThrows(IllegalArgumentException::class.java) { export.verify(directory, previousExport.fingerprint) }
        assertFalse(Files.exists(directory.resolve("${export.fingerprint}.json")))
        val saved = export.write(directory, export.fingerprint)
        assertEquals(saved.path, export.verify(directory, export.fingerprint))
        assertTrue(previousBytes.contentEquals(Files.readAllBytes(previousFile)))
    }

    @Test
    fun `AI proposals explicitly distinguish both ridge axes without changing the source draft`() {
        val base = draft(mapOf(WorldBlueprintArchitectureTemplate.KEY to "gable_shell"))
        val request = WorldBlueprintSuggestionRequest("world", WorldBlueprintSuggestionField.MAP_RULE,
            WorldBlueprintArchitectureTemplate.KEY, "coama pe axa X", "base-hash")
        val provenance = WorldBlueprintSuggestionProvenance("openai", "test-model", 100L,
            WorldBlueprintSuggestionParser.requestFingerprint(request.instruction))
        val prompt = WorldBlueprintSuggestionParser.prompt(request, base)
        assertTrue(prompt.contains("gable_shell_x (coama paralela cu X)"))
        assertTrue(prompt.contains("gable_shell (coama paralela cu Z)"))
        val entrancePrompt = WorldBlueprintSuggestionParser.prompt(
            request.copy(key = WorldBlueprintArchitectureEntrance.KEY), base)
        assertTrue(entrancePrompt.contains("pana la streasina pentru sabloanele inclinate"))
        val suggestion = WorldBlueprintSuggestionParser.parse(request, base,
            "{\"text\":\"GABLE_SHELL_X\"}", provenance)
        assertEquals("GABLE_SHELL_X", suggestion.candidate.mapRules[WorldBlueprintArchitectureTemplate.KEY])
        assertEquals("gable_shell", base.mapRules[WorldBlueprintArchitectureTemplate.KEY])
        assertEquals("gable_shell_x", requireNotNull(preview(suggestion.candidate).architecturePlan).templateId)
        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintSuggestionParser.parse(request, base, "{\"text\":\"gable_shell_y\"}", provenance)
        }
    }

    private fun draft(overrides: Map<String, String> = emptyMap()) = WorldBlueprint("world", mapRules = mapOf(
        "required_exterior_types" to "castle",
        WorldBlueprintArchitectureTemplate.KEY to " GABLE_SHELL_X ",
        WorldBlueprintArchitecturePalette.KEY to "stone,oak_planks,bricks",
        WorldBlueprintArchitectureMaterials.KEY to "floor=stone;wall=oak_planks;roof=bricks",
    ) + overrides)

    private fun preview(blueprint: WorldBlueprint, selected: WorldBlueprintRegionBounds? = bounds) =
        WorldBlueprintStructurePlanner.compile(blueprint, "castle", "citadel", emptyList(), emptyList(), emptyList(),
            selected, isBuildableBlock = { it in setOf("STONE", "OAK_PLANKS", "BRICKS") })
}
