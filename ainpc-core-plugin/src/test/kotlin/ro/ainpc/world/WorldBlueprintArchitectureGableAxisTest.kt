package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorldBlueprintArchitectureGableAxisTest {
    private val materials = WorldBlueprintArchitectureMaterialRoles("stone", "oak_planks", "bricks")
    private val palette = listOf(materials.floor, materials.wall, materials.roof)
    private val bounds = WorldBlueprintRegionBounds(10, 64, 20, 16, 70, 24)

    @Test
    fun `odd depth roof rises to a single ridge along X`() {
        val plan = gableX()

        assertEquals(WorldBlueprintArchitectureTemplate.GABLE_SHELL_X, plan.templateId)
        assertEquals(materials, plan.materials)
        for ((offset, roofY) in listOf(68, 69, 70, 69, 68).withIndex()) {
            val roofs = plan.blocks.filter { it.z == bounds.minZ + offset && it.blockId == materials.roof }
            assertEquals((10..16).map { Triple(it, roofY, bounds.minZ + offset) }.toSet(), positions(roofs))
        }
        assertEquals(plan.blocks.size, positions(plan).size)
        assertEquals(plan, gableX())
    }

    @Test
    fun `even depth roof has a two block ridge and normalized template id`() {
        val selected = WorldBlueprintRegionBounds(10, 64, 20, 13, 70, 25)
        val plan = WorldBlueprintArchitectureTemplate.plan(selected, palette, materials, templateId = " GABLE_SHELL_X ")

        assertEquals(WorldBlueprintArchitectureTemplate.GABLE_SHELL_X, plan.templateId)
        for ((offset, roofY) in listOf(68, 69, 70, 70, 69, 68).withIndex()) {
            val roofs = plan.blocks.filter { it.z == selected.minZ + offset && it.blockId == materials.roof }
            assertEquals((10..13).map { Triple(it, roofY, selected.minZ + offset) }.toSet(), positions(roofs))
            assertFalse(plan.blocks.any { it.z == selected.minZ + offset && it.y > roofY })
        }
        assertTrue(WorldBlueprintArchitectureTemplate.isValid(" GABLE_SHELL_X "))
        assertEquals(plan.blocks.size, positions(plan).size)
    }

    @Test
    fun `all cardinal entrances keep original directions and lower centers`() {
        val selected = WorldBlueprintRegionBounds(10, 64, 20, 15, 71, 25)
        val columns = mapOf(
            WorldBlueprintArchitectureEntrance.NORTH to (12 to 20),
            WorldBlueprintArchitectureEntrance.SOUTH to (12 to 25),
            WorldBlueprintArchitectureEntrance.EAST to (15 to 22),
            WorldBlueprintArchitectureEntrance.WEST to (10 to 22),
        )
        for ((entrance, column) in columns) {
            val plan = gableX(selected, entrance)
            val opening = plan.blocks.filter { it.purpose == WorldBlueprintArchitectureBlockPurpose.ENTRANCE_CLEARANCE }

            assertEquals(setOf(Triple(column.first, 65, column.second), Triple(column.first, 66, column.second)),
                positions(opening), entrance.id)
            assertTrue(opening.all { it.blockId == "air" })
            assertEquals(2, plan.blocks.count { it.blockId == "air" })
            assertEquals(materials.floor, block(plan, column.first, 64, column.second).blockId)
            assertEquals(materials.wall, block(plan, column.first, 67, column.second).blockId)
            assertEquals(entrance, plan.entrance)
            assertEquals(plan.blocks.size, positions(plan).size)
        }
    }

    @Test
    fun `axis transposition preserves all roles purposes and deterministic operation order`() {
        val selected = WorldBlueprintRegionBounds(-6, 40, 10, 2, 48, 15)
        val canonicalBounds = WorldBlueprintRegionBounds(10, 40, -6, 15, 48, 2)
        val entrances = mapOf(
            WorldBlueprintArchitectureEntrance.NONE to WorldBlueprintArchitectureEntrance.NONE,
            WorldBlueprintArchitectureEntrance.NORTH to WorldBlueprintArchitectureEntrance.WEST,
            WorldBlueprintArchitectureEntrance.SOUTH to WorldBlueprintArchitectureEntrance.EAST,
            WorldBlueprintArchitectureEntrance.EAST to WorldBlueprintArchitectureEntrance.SOUTH,
            WorldBlueprintArchitectureEntrance.WEST to WorldBlueprintArchitectureEntrance.NORTH,
        )
        for ((entrance, canonicalEntrance) in entrances) {
            for (clearInterior in listOf(false, true)) {
                val canonical = WorldBlueprintArchitectureTemplate.plan(canonicalBounds, palette, materials,
                    canonicalEntrance, clearInterior, WorldBlueprintArchitectureTemplate.GABLE_SHELL)
                val plan = gableX(selected, entrance, clearInterior)

                assertEquals(canonical.blocks.map { it.copy(x = it.z, z = it.x) }, plan.blocks)
                assertEquals(canonical.materials, plan.materials)
                assertEquals(entrance, plan.entrance)
                assertEquals(clearInterior, plan.clearInterior)
                assertEquals(WorldBlueprintArchitectureTemplate.GABLE_SHELL_X, plan.templateId)
            }
        }
    }

    @Test
    fun `interior clearance stays strictly inside walls and below the local Z slope`() {
        val shell = gableX()
        val cleared = gableX(clearInterior = true)
        val interior = cleared.blocks.filter { it.purpose == WorldBlueprintArchitectureBlockPurpose.INTERIOR_CLEARANCE }
        val expected = buildSet {
            for (blockZ in 21..23) {
                val roofY = if (blockZ == 22) 70 else 69
                for (blockY in 65 until roofY) {
                    for (blockX in 11..15) add(Triple(blockX, blockY, blockZ))
                }
            }
        }

        assertEquals(expected, positions(interior))
        assertTrue(interior.all { it.blockId == "air" })
        assertEquals(shell.blocks, cleared.blocks.take(shell.blocks.size))
        assertEquals(cleared.blocks.size, positions(cleared).size)
        assertFalse(cleared.blocks.any { it.z == 21 && it.y == 70 })
        assertFalse(cleared.blocks.any { it.z == 20 && it.y > 68 })
    }

    @Test
    fun `entrance and interior operations stay separate in the transposed plan`() {
        val plan = gableX(entrance = WorldBlueprintArchitectureEntrance.WEST, clearInterior = true)

        assertEquals(2, plan.blocks.count { it.purpose == WorldBlueprintArchitectureBlockPurpose.ENTRANCE_CLEARANCE })
        assertEquals(65, plan.blocks.count { it.purpose == WorldBlueprintArchitectureBlockPurpose.INTERIOR_CLEARANCE })
        assertEquals(plan.blocks.size, positions(plan).size)
        assertTrue(plan.clearInterior)
    }

    @Test
    fun `shell accepts exactly 4096 operations with dimensions transposed`() {
        val allowed = WorldBlueprintRegionBounds(0, 0, 0, 3, 409, 2)
        assertEquals(4096, gableX(allowed).blocks.size)

        val error = assertThrows(IllegalArgumentException::class.java) { gableX(allowed.copy(maxY = 410)) }
        assertTrue(error.message!!.contains("4106"))
        assertTrue(error.message!!.contains("4096"))
    }

    @Test
    fun `interior covered volume accepts exactly 4096 operations with dimensions transposed`() {
        val allowed = WorldBlueprintRegionBounds(0, 0, 0, 255, 5, 2)
        assertTrue(allowed.blockPositions.toInt() > 4096)
        assertEquals(4096, gableX(allowed, clearInterior = true).blocks.size)

        val error = assertThrows(IllegalArgumentException::class.java) {
            gableX(allowed.copy(maxX = 256), clearInterior = true)
        }
        assertTrue(error.message!!.contains("4112"))
        assertTrue(error.message!!.contains("4096"))
    }

    @Test
    fun `huge extents fail the budget guard before transposed coordinate iteration`() {
        val hugeBounds = listOf(
            WorldBlueprintRegionBounds(Int.MIN_VALUE, Int.MIN_VALUE, Int.MIN_VALUE,
                Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE),
            WorldBlueprintRegionBounds(Int.MIN_VALUE, 0, 0, Int.MAX_VALUE, 3, 2),
            WorldBlueprintRegionBounds(0, Int.MIN_VALUE, Int.MIN_VALUE, 2, Int.MAX_VALUE, Int.MAX_VALUE),
        )
        for (selected in hugeBounds) {
            for (clearInterior in listOf(false, true)) {
                val error = assertThrows(IllegalArgumentException::class.java) { gableX(selected, clearInterior = clearInterior) }
                assertTrue(error.message!!.contains("4096"))
            }
        }
    }

    @Test
    fun `small extreme coordinate plans retain original X and Z and avoid overflow`() {
        for ((minimumX, minimumZ) in listOf(Int.MIN_VALUE to Int.MAX_VALUE - 4, Int.MAX_VALUE - 4 to Int.MIN_VALUE)) {
            for (minimumY in listOf(Int.MIN_VALUE, Int.MAX_VALUE - 4)) {
                val selected = WorldBlueprintRegionBounds(minimumX, minimumY, minimumZ,
                    minimumX + 4, minimumY + 4, minimumZ + 4)
                val plan = gableX(selected, clearInterior = true)

                assertEquals(95, plan.blocks.size)
                assertEquals(plan.blocks.size, positions(plan).size)
                assertTrue(plan.blocks.all { it.x in selected.minX..selected.maxX &&
                    it.y in selected.minY..selected.maxY && it.z in selected.minZ..selected.maxZ })
                assertEquals(materials.roof, block(plan, minimumX + 2, minimumY + 4, minimumZ + 2).blockId)
                assertFalse(plan.blocks.any { it.z == minimumZ && it.y > minimumY + 2 })
            }
        }
    }

    @Test
    fun `X ridge requires eave height computed from depth not width`() {
        val validMinimum = WorldBlueprintRegionBounds(0, 0, 0, 6, 3, 2)
        assertEquals(WorldBlueprintArchitectureTemplate.GABLE_SHELL_X, gableX(validMinimum).templateId)
        for (invalid in listOf(validMinimum.copy(maxX = 1), validMinimum.copy(maxZ = 1),
            validMinimum.copy(maxY = 2), WorldBlueprintRegionBounds(0, 0, 0, 2, 4, 6))) {
            assertThrows(IllegalArgumentException::class.java) { gableX(invalid) }
        }
        for (entrance in WorldBlueprintArchitectureEntrance.entries.filter { it != WorldBlueprintArchitectureEntrance.NONE }) {
            assertThrows(IllegalArgumentException::class.java) { gableX(validMinimum, entrance) }
            assertEquals(2, gableX(validMinimum.copy(maxY = 4), entrance).blocks.count {
                it.purpose == WorldBlueprintArchitectureBlockPurpose.ENTRANCE_CLEARANCE
            })
        }
    }

    @Test
    fun `new axis retains effective palette and material validation`() {
        for (invalidPalette in listOf(emptyList(), listOf("stone", "bricks"))) {
            assertThrows(IllegalArgumentException::class.java) {
                WorldBlueprintArchitectureTemplate.plan(bounds, invalidPalette,
                    templateId = WorldBlueprintArchitectureTemplate.GABLE_SHELL_X)
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintArchitectureTemplate.plan(bounds, listOf("stone", "bricks"), materials,
                templateId = WorldBlueprintArchitectureTemplate.GABLE_SHELL_X)
        }
        val fallback = WorldBlueprintArchitectureTemplate.plan(bounds, listOf("stone"),
            templateId = WorldBlueprintArchitectureTemplate.GABLE_SHELL_X)
        assertEquals(WorldBlueprintArchitectureMaterialRoles("stone", "stone", "stone"), fallback.materials)
    }

    @Test
    fun `default remains cuboid and explicit legacy gable retains its Z ridge and operation order`() {
        val selected = WorldBlueprintRegionBounds(10, 64, 20, 12, 67, 23)
        val defaultPlan = WorldBlueprintArchitectureTemplate.plan(selected, palette, materials)
        val explicitCuboid = WorldBlueprintArchitectureTemplate.plan(selected, palette, materials,
            templateId = WorldBlueprintArchitectureTemplate.CUBOID_SHELL)
        val legacy = WorldBlueprintArchitectureTemplate.plan(selected, palette, materials,
            templateId = WorldBlueprintArchitectureTemplate.GABLE_SHELL)
        val normalizedLegacy = WorldBlueprintArchitectureTemplate.plan(selected, palette, materials,
            templateId = " GABLE_SHELL ")
        val expectedStart = buildList {
            for (blockX in 10..12) {
                val roofY = if (blockX == 11) 67 else 66
                for (blockZ in 20..23) {
                    add(WorldBlueprintPlannedBlock(blockX, 64, blockZ, materials.floor))
                    add(WorldBlueprintPlannedBlock(blockX, roofY, blockZ, materials.roof))
                }
            }
        }

        assertEquals(WorldBlueprintArchitectureTemplate.CUBOID_SHELL, defaultPlan.templateId)
        assertEquals(defaultPlan, explicitCuboid)
        assertEquals(legacy, normalizedLegacy)
        assertEquals(WorldBlueprintArchitectureTemplate.GABLE_SHELL, legacy.templateId)
        assertEquals(expectedStart, legacy.blocks.take(expectedStart.size))
        assertFalse(legacy.blocks.any { it.x == 10 && it.y > 66 })
        assertTrue(WorldBlueprintArchitectureTemplate.isValid(null))
        assertFalse(WorldBlueprintArchitectureTemplate.isValid("unknown"))
    }

    private fun gableX(
        selected: WorldBlueprintRegionBounds = bounds,
        entrance: WorldBlueprintArchitectureEntrance = WorldBlueprintArchitectureEntrance.NONE,
        clearInterior: Boolean = false,
    ) = WorldBlueprintArchitectureTemplate.plan(selected, palette, materials, entrance, clearInterior,
        WorldBlueprintArchitectureTemplate.GABLE_SHELL_X)

    private fun positions(plan: WorldBlueprintArchitecturePlan) = positions(plan.blocks)

    private fun positions(blocks: List<WorldBlueprintPlannedBlock>) = blocks.map { Triple(it.x, it.y, it.z) }.toSet()

    private fun block(plan: WorldBlueprintArchitecturePlan, blockX: Int, blockY: Int, blockZ: Int) =
        plan.blocks.single { it.x == blockX && it.y == blockY && it.z == blockZ }
}
