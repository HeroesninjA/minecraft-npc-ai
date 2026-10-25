package ro.ainpc.world

import java.math.BigInteger
import java.util.Locale

internal enum class WorldBlueprintArchitectureBlockPurpose(val label: String) {
    BUILD("Constructie"), ENTRANCE_CLEARANCE("Intrare"), INTERIOR_CLEARANCE("Interior");
}

internal data class WorldBlueprintPlannedBlock(
    val x: Int,
    val y: Int,
    val z: Int,
    val blockId: String,
    val purpose: WorldBlueprintArchitectureBlockPurpose = WorldBlueprintArchitectureBlockPurpose.BUILD,
)

internal data class WorldBlueprintArchitecturePlan(
    val templateId: String,
    val blocks: List<WorldBlueprintPlannedBlock>,
    val materials: WorldBlueprintArchitectureMaterialRoles,
    val entrance: WorldBlueprintArchitectureEntrance = WorldBlueprintArchitectureEntrance.NONE,
    val clearInterior: Boolean = false,
)

internal object WorldBlueprintArchitectureTemplate {
    const val KEY = "architecture_template"
    const val CUBOID_SHELL = "cuboid_shell"
    const val GABLE_SHELL = "gable_shell"
    const val GABLE_SHELL_X = "gable_shell_x"
    const val MAX_BLOCKS = 4096

    fun isValid(raw: String?): Boolean = raw == null || raw.trim().lowercase(Locale.ROOT) in setOf(CUBOID_SHELL, GABLE_SHELL, GABLE_SHELL_X)

    fun plan(
        bounds: WorldBlueprintRegionBounds,
        palette: List<String>,
        materialRoles: WorldBlueprintArchitectureMaterialRoles? = null,
        entrance: WorldBlueprintArchitectureEntrance = WorldBlueprintArchitectureEntrance.NONE,
        clearInterior: Boolean = false,
        templateId: String = CUBOID_SHELL,
    ): WorldBlueprintArchitecturePlan {
        val selectedTemplate = templateId.trim().lowercase(Locale.ROOT)
        require(selectedTemplate in setOf(CUBOID_SHELL, GABLE_SHELL, GABLE_SHELL_X)) {
            "$KEY: sablon necunoscut '$templateId'; foloseste cuboid_shell, gable_shell sau gable_shell_x."
        }
        if (selectedTemplate == GABLE_SHELL) return planGable(bounds, palette, materialRoles, entrance, clearInterior)
        if (selectedTemplate == GABLE_SHELL_X) return planGableX(bounds, palette, materialRoles, entrance, clearInterior)
        require(bounds.width >= 3 && bounds.height >= 3 && bounds.depth >= 3) {
            "$KEY: cuboid_shell necesita minimum 3 blocuri pe fiecare axa."
        }
        require(entrance == WorldBlueprintArchitectureEntrance.NONE || bounds.height >= 4) {
            "${WorldBlueprintArchitectureEntrance.KEY}: intrarea de 1x2 necesita minimum 4 blocuri pe verticala."
        }
        val materials = materialRoles ?: palette.singleOrNull()?.let { WorldBlueprintArchitectureMaterialRoles(it, it, it) }
        require(materials != null) {
            "$KEY: configureaza architecture_material_roles sau exact un bloc in paleta efectiva."
        }
        val missing = setOf(materials.floor, materials.wall, materials.roof) - palette.toSet()
        require(missing.isEmpty()) {
            "${WorldBlueprintArchitectureMaterials.KEY}: materiale absente din paleta efectiva: ${missing.sorted().joinToString()}."
        }
        val interior = BigInteger.valueOf(bounds.width - 2)
            .multiply(BigInteger.valueOf(bounds.height - 2)).multiply(BigInteger.valueOf(bounds.depth - 2))
        val blockCount = if (clearInterior) bounds.blockPositions else bounds.blockPositions - interior
        require(blockCount <= BigInteger.valueOf(MAX_BLOCKS.toLong())) {
            "$KEY: planul necesita $blockCount pozitii; limita preview-ului este $MAX_BLOCKS."
        }
        val entranceColumn = entrance.column(bounds)
        fun wallBlock(blockX: Int, blockY: Int, blockZ: Int): WorldBlueprintPlannedBlock {
            val opening = entranceColumn != null && blockX == entranceColumn.first &&
                blockZ == entranceColumn.second && blockY in bounds.minY + 1..bounds.minY + 2
            return WorldBlueprintPlannedBlock(blockX, blockY, blockZ, if (opening) "air" else materials.wall,
                if (opening) WorldBlueprintArchitectureBlockPurpose.ENTRANCE_CLEARANCE else WorldBlueprintArchitectureBlockPurpose.BUILD)
        }
        val blocks = buildList {
            for (blockX in bounds.minX..bounds.maxX) {
                for (blockZ in bounds.minZ..bounds.maxZ) {
                    add(WorldBlueprintPlannedBlock(blockX, bounds.minY, blockZ, materials.floor))
                    add(WorldBlueprintPlannedBlock(blockX, bounds.maxY, blockZ, materials.roof))
                }
            }
            for (blockY in bounds.minY + 1..bounds.maxY - 1) {
                for (blockX in bounds.minX..bounds.maxX) {
                    add(wallBlock(blockX, blockY, bounds.minZ))
                    add(wallBlock(blockX, blockY, bounds.maxZ))
                }
                for (blockZ in bounds.minZ + 1..bounds.maxZ - 1) {
                    add(wallBlock(bounds.minX, blockY, blockZ))
                    add(wallBlock(bounds.maxX, blockY, blockZ))
                }
            }
            if (clearInterior) {
                for (blockX in bounds.minX + 1..bounds.maxX - 1) {
                    for (blockY in bounds.minY + 1..bounds.maxY - 1) {
                        for (blockZ in bounds.minZ + 1..bounds.maxZ - 1) {
                            add(WorldBlueprintPlannedBlock(blockX, blockY, blockZ, "air",
                                WorldBlueprintArchitectureBlockPurpose.INTERIOR_CLEARANCE))
                        }
                    }
                }
            }
        }
        return WorldBlueprintArchitecturePlan(CUBOID_SHELL, blocks, materials, entrance, clearInterior)
    }

    private fun planGableX(
        bounds: WorldBlueprintRegionBounds,
        palette: List<String>,
        materialRoles: WorldBlueprintArchitectureMaterialRoles?,
        entrance: WorldBlueprintArchitectureEntrance,
        clearInterior: Boolean,
    ): WorldBlueprintArchitecturePlan {
        val transposedBounds = WorldBlueprintRegionBounds(bounds.minZ, bounds.minY, bounds.minX,
            bounds.maxZ, bounds.maxY, bounds.maxX)
        val transposedEntrance = when (entrance) {
            WorldBlueprintArchitectureEntrance.NONE -> WorldBlueprintArchitectureEntrance.NONE
            WorldBlueprintArchitectureEntrance.NORTH -> WorldBlueprintArchitectureEntrance.WEST
            WorldBlueprintArchitectureEntrance.SOUTH -> WorldBlueprintArchitectureEntrance.EAST
            WorldBlueprintArchitectureEntrance.EAST -> WorldBlueprintArchitectureEntrance.SOUTH
            WorldBlueprintArchitectureEntrance.WEST -> WorldBlueprintArchitectureEntrance.NORTH
        }
        val plan = planGable(transposedBounds, palette, materialRoles, transposedEntrance, clearInterior)
        return plan.copy(templateId = GABLE_SHELL_X, blocks = plan.blocks.map { it.copy(x = it.z, z = it.x) },
            entrance = entrance)
    }

    private fun planGable(
        bounds: WorldBlueprintRegionBounds,
        palette: List<String>,
        materialRoles: WorldBlueprintArchitectureMaterialRoles?,
        entrance: WorldBlueprintArchitectureEntrance,
        clearInterior: Boolean,
    ): WorldBlueprintArchitecturePlan {
        val maximumRise = (bounds.width - 1) / 2
        val eaveHeight = bounds.height - maximumRise
        require(bounds.width >= 3 && bounds.depth >= 3 && eaveHeight >= 3) {
            "$KEY: sablonul inclinat necesita minimum 3 blocuri pe X/Z si 3 blocuri pana la streasina."
        }
        require(entrance == WorldBlueprintArchitectureEntrance.NONE || eaveHeight >= 4) {
            "${WorldBlueprintArchitectureEntrance.KEY}: intrarea de 1x2 necesita minimum 4 blocuri pana la streasina."
        }
        val materials = materialRoles ?: palette.singleOrNull()?.let { WorldBlueprintArchitectureMaterialRoles(it, it, it) }
        require(materials != null) {
            "$KEY: configureaza architecture_material_roles sau exact un bloc in paleta efectiva."
        }
        val missing = setOf(materials.floor, materials.wall, materials.roof) - palette.toSet()
        require(missing.isEmpty()) {
            "${WorldBlueprintArchitectureMaterials.KEY}: materiale absente din paleta efectiva: ${missing.sorted().joinToString()}."
        }
        val width = BigInteger.valueOf(bounds.width)
        val depth = BigInteger.valueOf(bounds.depth)
        val wallHeight = BigInteger.valueOf(eaveHeight - 2)
        val halfWidth = BigInteger.valueOf(bounds.width / 2)
        val sumRise = halfWidth.multiply(if (bounds.width % 2 == 1L) halfWidth else halfWidth - BigInteger.ONE)
        val blockCount = if (clearInterior) {
            depth.multiply(width.multiply(BigInteger.valueOf(eaveHeight)) + sumRise)
        } else {
            BigInteger.TWO.multiply(width).multiply(depth) +
                BigInteger.TWO.multiply(width.multiply(wallHeight) + sumRise) +
                BigInteger.TWO.multiply(depth - BigInteger.TWO).multiply(wallHeight)
        }
        require(blockCount <= BigInteger.valueOf(MAX_BLOCKS.toLong())) {
            "$KEY: planul necesita $blockCount pozitii; limita preview-ului este $MAX_BLOCKS."
        }
        val entranceColumn = entrance.column(bounds)
        val eaveY = (bounds.maxY.toLong() - maximumRise).toInt()
        fun roofY(blockX: Int): Int {
            val rise = minOf(blockX.toLong() - bounds.minX, bounds.maxX.toLong() - blockX)
            return (eaveY.toLong() + rise).toInt()
        }
        fun wallBlock(blockX: Int, blockY: Int, blockZ: Int): WorldBlueprintPlannedBlock {
            val opening = entranceColumn != null && blockX == entranceColumn.first &&
                blockZ == entranceColumn.second && blockY in bounds.minY + 1..bounds.minY + 2
            return WorldBlueprintPlannedBlock(blockX, blockY, blockZ, if (opening) "air" else materials.wall,
                if (opening) WorldBlueprintArchitectureBlockPurpose.ENTRANCE_CLEARANCE else WorldBlueprintArchitectureBlockPurpose.BUILD)
        }
        val blocks = buildList {
            for (blockX in bounds.minX..bounds.maxX) {
                val roofY = roofY(blockX)
                for (blockZ in bounds.minZ..bounds.maxZ) {
                    add(WorldBlueprintPlannedBlock(blockX, bounds.minY, blockZ, materials.floor))
                    add(WorldBlueprintPlannedBlock(blockX, roofY, blockZ, materials.roof))
                }
            }
            for (blockX in bounds.minX..bounds.maxX) {
                for (blockY in bounds.minY + 1 until roofY(blockX)) {
                    add(wallBlock(blockX, blockY, bounds.minZ))
                    add(wallBlock(blockX, blockY, bounds.maxZ))
                }
            }
            for (blockY in bounds.minY + 1 until eaveY) {
                for (blockZ in bounds.minZ + 1 until bounds.maxZ) {
                    add(wallBlock(bounds.minX, blockY, blockZ))
                    add(wallBlock(bounds.maxX, blockY, blockZ))
                }
            }
            if (clearInterior) {
                for (blockX in bounds.minX + 1 until bounds.maxX) {
                    for (blockY in bounds.minY + 1 until roofY(blockX)) {
                        for (blockZ in bounds.minZ + 1 until bounds.maxZ) {
                            add(WorldBlueprintPlannedBlock(blockX, blockY, blockZ, "air",
                                WorldBlueprintArchitectureBlockPurpose.INTERIOR_CLEARANCE))
                        }
                    }
                }
            }
        }
        return WorldBlueprintArchitecturePlan(GABLE_SHELL, blocks, materials, entrance, clearInterior)
    }
}
