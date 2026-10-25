package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorldBlueprintArchitectureMaterialsTest {
    private val knownBlocks = setOf("STONE", "OAK_PLANKS", "BRICKS")
    private val bounds = WorldBlueprintRegionBounds(0, 64, 0, 2, 66, 2)
    private val roles = WorldBlueprintArchitectureMaterialRoles("stone", "oak_planks", "bricks")
    private val rule = "floor=stone;wall=oak_planks;roof=bricks"

    @Test
    fun `normalizes role names and material ids while allowing one material for several roles`() {
        val parsed = parse(" Roof = MINECRAFT:STONE ; Floor=stone; WALL=OAK_PLANKS ")!!

        assertNull(parsed.error)
        assertEquals(WorldBlueprintArchitectureMaterialRoles("stone", "oak_planks", "stone"), parsed.roles)
        assertNull(parse(null))
    }

    @Test
    fun `rejects incomplete duplicate unknown and invalid assignments without partial roles`() {
        listOf("", "floor=stone", "floor=stone;wall=oak_planks;floor=bricks",
            "floor=stone;wall=oak_planks;WALL=bricks", "floor=stone;wall=oak_planks;ceiling=bricks",
            "floor=stone;wall=;roof=bricks", "floor=stone,bricks;wall=oak_planks;roof=bricks",
            "floor=diamond_sword;wall=oak_planks;roof=bricks", "floor=air;wall=oak_planks;roof=bricks",
            "floor=custom:stone;wall=oak_planks;roof=bricks", "$rule;", "$rule;other=stone").forEach { raw ->
            val parsed = parse(raw)!!
            assertNull(parsed.roles, raw)
            assertNotNull(parsed.error, raw)
        }
    }

    @Test
    fun `floor and roof own their complete layers while walls own only the middle boundary`() {
        val plan = WorldBlueprintArchitectureTemplate.plan(bounds, listOf("stone", "oak_planks", "bricks"), roles)

        assertEquals(roles, plan.materials)
        assertEquals(26, plan.blocks.size)
        assertEquals(26, plan.blocks.map { Triple(it.x, it.y, it.z) }.toSet().size)
        assertEquals(mapOf("stone" to 9, "oak_planks" to 8, "bricks" to 9),
            plan.blocks.groupingBy { it.blockId }.eachCount())
        plan.blocks.forEach { block ->
            val expected = when (block.y) {
                64 -> "stone"
                66 -> "bricks"
                else -> "oak_planks"
            }
            assertEquals(expected, block.blockId)
        }
        val byPosition = plan.blocks.associateBy { Triple(it.x, it.y, it.z) }
        val observed = WorldBlueprintArchitectureTargetInspector.inspect(plan, -64, 320) { blockX, blockY, blockZ ->
            WorldBlueprintArchitectureObservedBlock(true, byPosition.getValue(Triple(blockX, blockY, blockZ)).blockId, false)
        }
        assertEquals(26, observed.matchingMaterialCount)
        assertEquals(0, observed.differentMaterialCount)
    }

    @Test
    fun `type palette takes precedence when validating all role materials`() {
        val draft = draft().copy(mapRules = draft().mapRules +
            (WorldBlueprintStructurePalettes.KEY to "castle=stone,oak_planks"))
        val invalid = preview(draft)
        assertNull(invalid.architecturePlan)
        assertTrue(invalid.architecturePlanIssue!!.contains("bricks"))
        assertTrue(WorldBlueprintStructureConfigurationPreflightInspector.inspect(invalid, emptyMap())
            .conflicts.contains("sablon arhitectural necompilabil"))
        val valid = preview(draft.copy(mapRules = draft.mapRules +
            (WorldBlueprintStructurePalettes.KEY to "castle=bricks,stone,oak_planks")))
        assertEquals(roles, valid.architecturePlan!!.materials)
        assertNull(valid.architecturePlanIssue)
    }

    @Test
    fun `invalid template and role rule both remain visible without compiling a fallback`() {
        val issues = WorldBlueprintCompatibility.structuredRuleSyntaxIssues(WorldBlueprintArchitectureMaterials.KEY, "floor=stone")
        assertEquals(WorldBlueprintCompatibilityCode.INVALID_ARCHITECTURE_MATERIAL_ROLES_RULE, issues.single().code)
        val invalid = preview(draft().copy(mapRules = draft().mapRules + mapOf(
            WorldBlueprintArchitectureMaterials.KEY to "floor=stone",
            WorldBlueprintArchitectureTemplate.KEY to "unknown",
        )))
        assertNull(invalid.architecturePlan)
        assertTrue(invalid.architecturePlanIssue!!.contains(WorldBlueprintArchitectureTemplate.KEY))
        assertTrue(invalid.architecturePlanIssue.contains(WorldBlueprintArchitectureMaterials.KEY))
    }

    @Test
    fun `roles alone do not activate a template and absence preserves the single material recipe`() {
        assertNull(preview(draft().copy(mapRules = draft().mapRules - WorldBlueprintArchitectureTemplate.KEY)).architecturePlan)
        val single = preview(draft().copy(mapRules = (draft().mapRules - WorldBlueprintArchitectureMaterials.KEY) +
            (WorldBlueprintArchitecturePalette.KEY to "stone")))
        assertEquals(WorldBlueprintArchitectureMaterialRoles("stone", "stone", "stone"), single.architecturePlan!!.materials)
        assertEquals(setOf("stone"), single.architecturePlan.blocks.map { it.blockId }.toSet())
    }

    private fun parse(raw: String?) = WorldBlueprintArchitectureMaterials.parse(raw) { it in knownBlocks }

    private fun draft() = WorldBlueprint("world", mapRules = mapOf(
        "required_exterior_types" to "castle",
        WorldBlueprintArchitectureTemplate.KEY to "cuboid_shell",
        WorldBlueprintArchitecturePalette.KEY to "stone,oak_planks,bricks",
        WorldBlueprintArchitectureMaterials.KEY to rule,
    ))

    private fun preview(draft: WorldBlueprint) = WorldBlueprintStructurePlanner.compile(
        draft, "castle", "citadel", emptyList(), emptyList(), emptyList(), bounds,
        isBuildableBlock = { it in knownBlocks })
}
