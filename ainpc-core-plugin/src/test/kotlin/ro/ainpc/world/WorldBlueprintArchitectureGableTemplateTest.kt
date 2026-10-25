package ro.ainpc.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorldBlueprintArchitectureGableTemplateTest {
    private val materials = WorldBlueprintArchitectureMaterialRoles("stone", "oak_planks", "bricks")
    private val bounds = WorldBlueprintRegionBounds(10, 64, 20, 14, 70, 24)

    @Test
    fun `odd width roof rises to a single ridge along Z`() {
        val plan = gable()

        assertEquals(WorldBlueprintArchitectureTemplate.GABLE_SHELL, plan.templateId)
        assertEquals(materials, plan.materials)
        assertEquals(106, plan.blocks.size)
        assertEquals(plan.blocks.size, positions(plan).size)
        for ((offset, roofY) in listOf(68, 69, 70, 69, 68).withIndex()) {
            val roofs = plan.blocks.filter { it.x == bounds.minX + offset && it.blockId == materials.roof }
            assertEquals((20..24).map { Triple(bounds.minX + offset, roofY, it) }.toSet(), positions(roofs))
        }
        assertEquals(plan, gable())
    }

    @Test
    fun `even width roof has a two block ridge and normalized template id`() {
        val selected = WorldBlueprintRegionBounds(10, 64, 20, 15, 70, 24)
        val plan = WorldBlueprintArchitectureTemplate.plan(selected, listOf("stone"), templateId = " GABLE_SHELL ")

        assertEquals(WorldBlueprintArchitectureTemplate.GABLE_SHELL, plan.templateId)
        assertEquals(126, plan.blocks.size)
        for ((offset, roofY) in listOf(68, 69, 70, 70, 69, 68).withIndex()) {
            assertTrue((20..24).all { blockZ -> plan.blocks.any {
                it.x == selected.minX + offset && it.y == roofY && it.z == blockZ
            } })
            assertFalse(plan.blocks.any { it.x == selected.minX + offset && it.y > roofY })
        }
        assertEquals(plan.blocks.size, positions(plan).size)
        assertTrue(WorldBlueprintArchitectureTemplate.isValid(" GABLE_SHELL "))
    }

    @Test
    fun `floor gable ends and side walls use their assigned roles without corners duplicated`() {
        val plan = gable()
        val floor = plan.blocks.filter { it.blockId == materials.floor }
        assertEquals(25, floor.size)
        assertTrue(floor.all { it.y == bounds.minY })
        assertEquals(25, plan.blocks.count { it.blockId == materials.roof })
        assertEquals(56, plan.blocks.count { it.blockId == materials.wall })
        assertEquals(materials.wall, block(plan, 12, 69, 20).blockId)
        assertEquals(materials.wall, block(plan, 12, 69, 24).blockId)
        assertEquals(materials.wall, block(plan, 10, 67, 22).blockId)
        assertEquals(materials.wall, block(plan, 14, 67, 22).blockId)
        assertFalse(plan.blocks.any { it.x == 12 && it.y == 69 && it.z == 22 })
        assertFalse(plan.blocks.any { it.x == 10 && it.y > 68 })
        assertTrue(plan.blocks.all { it.purpose == WorldBlueprintArchitectureBlockPurpose.BUILD })
    }

    @Test
    fun `all entrances preserve floor roof and remaining walls with deterministic even centers`() {
        val selected = WorldBlueprintRegionBounds(10, 64, 20, 15, 71, 25)
        val columns = mapOf(
            WorldBlueprintArchitectureEntrance.NORTH to (12 to 20),
            WorldBlueprintArchitectureEntrance.SOUTH to (12 to 25),
            WorldBlueprintArchitectureEntrance.EAST to (15 to 22),
            WorldBlueprintArchitectureEntrance.WEST to (10 to 22),
        )
        for ((entrance, column) in columns) {
            val plan = gable(selected, entrance)
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
    fun `interior clearance fills only cells below each local roof and preserves the shell`() {
        val shell = gable()
        val cleared = gable(clearInterior = true)
        val interior = cleared.blocks.filter { it.purpose == WorldBlueprintArchitectureBlockPurpose.INTERIOR_CLEARANCE }
        val expected = buildSet {
            for (blockX in 11..13) {
                val roofY = if (blockX == 12) 70 else 69
                for (blockY in 65 until roofY) {
                    for (blockZ in 21..23) add(Triple(blockX, blockY, blockZ))
                }
            }
        }

        assertEquals(39, interior.size)
        assertEquals(expected, positions(interior))
        assertTrue(interior.all { it.blockId == "air" })
        assertEquals(145, cleared.blocks.size)
        assertEquals(shell.blocks, cleared.blocks.take(shell.blocks.size))
        assertEquals(cleared.blocks.size, positions(cleared).size)
        assertTrue(cleared.clearInterior)
        assertFalse(shell.clearInterior)
        assertFalse(cleared.blocks.any { it.x == 11 && it.y == 70 })
        assertFalse(cleared.blocks.any { it.x == 10 && it.y > 68 })
    }

    @Test
    fun `entrance and interior clearance remain separate and share the same budget`() {
        val plan = gable(entrance = WorldBlueprintArchitectureEntrance.NORTH, clearInterior = true)

        assertEquals(145, plan.blocks.size)
        assertEquals(2, plan.blocks.count { it.purpose == WorldBlueprintArchitectureBlockPurpose.ENTRANCE_CLEARANCE })
        assertEquals(39, plan.blocks.count { it.purpose == WorldBlueprintArchitectureBlockPurpose.INTERIOR_CLEARANCE })
        assertEquals(plan.blocks.size, positions(plan).size)
    }

    @Test
    fun `shell accepts exactly 4096 operations and rejects the next height before allocation`() {
        val allowed = WorldBlueprintRegionBounds(0, 0, 0, 2, 409, 3)
        assertEquals(4096, gable(allowed).blocks.size)

        val error = assertThrows(IllegalArgumentException::class.java) {
            gable(allowed.copy(maxY = 410))
        }
        assertTrue(error.message!!.contains("4106"))
        assertTrue(error.message!!.contains("4096"))
    }

    @Test
    fun `interior covered volume accepts exactly 4096 operations rather than the bounding cuboid`() {
        val allowed = WorldBlueprintRegionBounds(0, 0, 0, 2, 5, 255)
        assertTrue(allowed.blockPositions.toInt() > 4096)
        assertEquals(4096, gable(allowed, clearInterior = true).blocks.size)

        val error = assertThrows(IllegalArgumentException::class.java) {
            gable(allowed.copy(maxZ = 256), clearInterior = true)
        }
        assertTrue(error.message!!.contains("4112"))
    }

    @Test
    fun `extreme coordinate volumes are rejected without overflow or coordinate iteration`() {
        val huge = WorldBlueprintRegionBounds(Int.MIN_VALUE, Int.MIN_VALUE, Int.MIN_VALUE,
            Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE)

        for (clearInterior in listOf(false, true)) {
            val error = assertThrows(IllegalArgumentException::class.java) { gable(huge, clearInterior = clearInterior) }
            assertTrue(error.message!!.contains("4096"))
        }
    }

    @Test
    fun `small gables at extreme coordinates keep roof and interior in bounds`() {
        for (minimum in listOf(Int.MIN_VALUE, Int.MAX_VALUE - 4)) {
            val selected = WorldBlueprintRegionBounds(minimum, minimum, minimum,
                minimum + 4, minimum + 4, minimum + 4)
            val plan = gable(selected, clearInterior = true)

            assertEquals(95, plan.blocks.size)
            assertEquals(plan.blocks.size, positions(plan).size)
            assertTrue(plan.blocks.all { it.x in selected.minX..selected.maxX &&
                it.y in selected.minY..selected.maxY && it.z in selected.minZ..selected.maxZ })
            assertEquals(materials.roof, block(plan, minimum + 2, minimum + 4, minimum + 2).blockId)
            assertFalse(plan.blocks.any { it.x == minimum && it.y > minimum + 2 })
        }
    }

    @Test
    fun `gable dimensions require three block eaves and a conservatively taller entrance`() {
        val validMinimum = WorldBlueprintRegionBounds(0, 0, 0, 2, 3, 2)
        assertEquals(28, gable(validMinimum).blocks.size)
        for (invalid in listOf(validMinimum.copy(maxX = 1), validMinimum.copy(maxZ = 1),
            validMinimum.copy(maxY = 2), WorldBlueprintRegionBounds(0, 0, 0, 6, 4, 2))) {
            assertThrows(IllegalArgumentException::class.java) { gable(invalid) }
        }
        for (entrance in WorldBlueprintArchitectureEntrance.entries.filter { it != WorldBlueprintArchitectureEntrance.NONE }) {
            assertThrows(IllegalArgumentException::class.java) { gable(validMinimum, entrance) }
            assertEquals(2, gable(validMinimum.copy(maxY = 4), entrance).blocks.count {
                it.purpose == WorldBlueprintArchitectureBlockPurpose.ENTRANCE_CLEARANCE
            })
        }
    }

    @Test
    fun `gable material requirements match cuboid and unknown templates fail explicitly`() {
        for (palette in listOf(emptyList(), listOf("stone", "bricks"))) {
            assertThrows(IllegalArgumentException::class.java) {
                WorldBlueprintArchitectureTemplate.plan(bounds, palette, templateId = "gable_shell")
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintArchitectureTemplate.plan(bounds, listOf("stone", "bricks"), materials, templateId = "gable_shell")
        }
        assertThrows(IllegalArgumentException::class.java) {
            WorldBlueprintArchitectureTemplate.plan(bounds, listOf("stone"), templateId = "unknown")
        }
        assertFalse(WorldBlueprintArchitectureTemplate.isValid("unknown"))
        assertFalse(WorldBlueprintArchitectureTemplate.isValid(""))
        assertTrue(WorldBlueprintArchitectureTemplate.isValid(null))
        assertEquals(WorldBlueprintArchitectureTemplate.CUBOID_SHELL,
            WorldBlueprintArchitectureTemplate.plan(bounds, listOf("stone")).templateId)
    }

    private fun gable(
        selected: WorldBlueprintRegionBounds = bounds,
        entrance: WorldBlueprintArchitectureEntrance = WorldBlueprintArchitectureEntrance.NONE,
        clearInterior: Boolean = false,
    ) = WorldBlueprintArchitectureTemplate.plan(selected, listOf(materials.floor, materials.wall, materials.roof),
        materials, entrance, clearInterior, WorldBlueprintArchitectureTemplate.GABLE_SHELL)

    private fun positions(plan: WorldBlueprintArchitecturePlan) = positions(plan.blocks)

    private fun positions(blocks: List<WorldBlueprintPlannedBlock>) = blocks.map { Triple(it.x, it.y, it.z) }.toSet()

    private fun block(plan: WorldBlueprintArchitecturePlan, blockX: Int, blockY: Int, blockZ: Int) =
        plan.blocks.single { it.x == blockX && it.y == blockY && it.z == blockZ }
}
