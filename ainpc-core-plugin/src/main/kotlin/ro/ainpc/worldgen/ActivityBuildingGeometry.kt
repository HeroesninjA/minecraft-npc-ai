package ro.ainpc.worldgen

import org.bukkit.Material
import org.bukkit.util.Vector
import ro.ainpc.api.settlement.BuildingTemplateDefinition

class ActivityBuildingGeometry {
    fun generate(definition: BuildingTemplateDefinition): BuildingTemplate {
        require(supports(definition)) { "Unsupported physical building geometry" }
        val geometryKind = if (definition.placeType in setOf("custom", "house")) definition.metadata["geometry"] else definition.placeType
        val garden = geometryKind == "leisure_garden"
        val socialOnly = geometryKind in setOf("leisure_garden", "community_hall")
        val home = geometryKind == "house_small"
        val customWorkplace = geometryKind in setOf("workshop", "bakery", "library")
        require(0 in definition.supportedRotations) { "Physical geometry requires rotation 0" }
        val width = definition.footprintWidth
        val depth = definition.footprintDepth
        val height = definition.footprintHeight
        require(width in 4..100 && depth in 4..100 && height in 3..20) { "Unsupported building dimensions" }
        require((width + 1L) * (depth + 1L) * (height + 1L) <= 10000) { "Building exceeds 10000 cells" }
        require(definition.anchors.map { it.anchorId }.distinct().size == definition.anchors.size) { "Duplicate anchor IDs" }
        val entrances = definition.anchors.filter { it.nodeType == "entrance" }
        val workplaces = definition.anchors.filter { it.nodeType in setOf("work", "workstation") }
        val socialAnchors = definition.anchors.filter { it.nodeType in setOf("social", "meeting_point") }
        val sleepingAnchors = definition.anchors.filter { it.nodeType in setOf("bed", "home") }
        val spawnAnchors = definition.anchors.filter { it.nodeType == "npc_spawn" }
        require(entrances.isNotEmpty() && (socialOnly || home || workplaces.isNotEmpty())) { "Entrance and activity anchors are required" }
        if (home) {
            val capacity = definition.metadata["max_residents"]?.toIntOrNull() ?: 0
            require("home" in definition.tags && capacity > 0 && sleepingAnchors.size >= capacity && spawnAnchors.size >= capacity && workplaces.isEmpty()) {
                "Physical home requires home tag, resident capacity, sleeping and spawn anchors without workplaces"
            }
            require(definition.anchors.map { it.offsetX to it.offsetZ }.distinct().size == definition.anchors.size) {
                "Physical home anchors require distinct standing positions"
            }
        }
        if (socialOnly) {
            require(socialAnchors.isNotEmpty() && workplaces.isEmpty()) { "Social-only geometry requires social anchors and no workplaces" }
            require(definition.metadata["max_workers"]?.toIntOrNull() == 0 && "social" in definition.tags &&
                definition.tags.none { tag -> setOf("work", "workplace", "job").any { it.equals(tag, ignoreCase = true) } } &&
                !definition.metadata["role"].equals("work", ignoreCase = true) && !definition.metadata["purpose"].equals("work", ignoreCase = true)) {
                "Social-only geometry requires social tag and max_workers=0"
            }
        }
        if (geometryKind == "community_hall") {
            require((definition.metadata["max_social_associations"]?.toIntOrNull() ?: 0) > 0) {
                "Community hall requires positive max_social_associations"
            }
        }
        if (customWorkplace) {
            require("work" in definition.tags && (definition.metadata["max_workers"]?.toIntOrNull() ?: 0) > 0) {
                "Custom workplace requires work tag and positive max_workers"
            }
        }
        require(geometryKind !in setOf("tavern", "market", "library") || socialAnchors.isNotEmpty()) {
            "Tavern, market and library require a social anchor"
        }
        require(geometryKind != "library" || "social" in definition.tags) { "Library requires social tag" }
        for (anchor in definition.anchors) {
            require(anchor.offsetY == 1) { "Anchor ${anchor.anchorId} must stand at floor + 1" }
            val onWall = (anchor.offsetX in setOf(0, width) && anchor.offsetZ in 1 until depth) ||
                (anchor.offsetZ in setOf(0, depth) && anchor.offsetX in 1 until width)
            val inside = anchor.offsetX in 1 until width && anchor.offsetZ in 1 until depth
            require(if (anchor.nodeType == "entrance") onWall else inside) { "Anchor ${anchor.anchorId} has an invalid position" }
        }
        val floorMaterial = when (geometryKind) {
            "forge" -> Material.STONE_BRICKS
            "bakery" -> Material.BRICKS
            "tavern", "workshop", "farm", "community_hall" -> Material.SPRUCE_PLANKS
            "market" -> Material.COBBLESTONE
            "leisure_garden" -> Material.GRASS_BLOCK
            else -> Material.OAK_PLANKS
        }
        val cells = linkedMapOf<Cell, Material>()
        for (offsetX in 0..width) for (offsetZ in 0..depth) for (offsetY in 0..height) {
            cells[Cell(offsetX, offsetY, offsetZ)] = when {
                geometryKind == "market" || garden -> if (offsetY == 0) floorMaterial else Material.AIR
                offsetY == 0 || offsetY == height -> floorMaterial
                offsetX in setOf(0, width) || offsetZ in setOf(0, depth) -> floorMaterial
                else -> Material.AIR
            }
        }
        for (entrance in entrances) {
            cells[Cell(entrance.offsetX, 1, entrance.offsetZ)] = Material.AIR
            cells[Cell(entrance.offsetX, 2, entrance.offsetZ)] = Material.AIR
        }
        val standingPositions = definition.anchors.map { Cell(it.offsetX, 1, it.offsetZ) }.toSet()
        if (home) {
            for (sleepingAnchor in sleepingAnchors) {
                val marker = neighbors(Cell(sleepingAnchor.offsetX, 1, sleepingAnchor.offsetZ)).firstOrNull {
                    it.x in 1 until width && it.z in 1 until depth && it !in standingPositions && cells[it] == Material.AIR
                }
                require(marker != null) { "No sleeping marker space near ${sleepingAnchor.anchorId}" }
                cells[marker] = Material.WHITE_WOOL
            }
        }
        for (workplace in workplaces) {
            val furniture = neighbors(Cell(workplace.offsetX, 1, workplace.offsetZ)).firstOrNull {
                it.x in 1 until width && it.z in 1 until depth && it !in standingPositions && cells[it] == Material.AIR &&
                    (geometryKind != "market" || listOf(it.copy(z = it.z - 1), it.copy(z = it.z + 1)).all { post ->
                        post.z in 1 until depth && post !in standingPositions && (1..height).all { level -> cells[post.copy(y = level)] == Material.AIR }
                    })
            }
            require(furniture != null) { "No furniture space near ${workplace.anchorId}" }
            cells[furniture] = when (geometryKind) {
                "forge" -> Material.SMITHING_TABLE
                "workshop" -> Material.CRAFTING_TABLE
                "bakery" -> Material.SMOKER
                "library" -> Material.LECTERN
                "farm" -> Material.COMPOSTER
                else -> Material.BARREL
            }
            if (geometryKind == "market") {
                for (postZ in listOf(furniture.z - 1, furniture.z + 1)) {
                    for (level in 1 until height) cells[Cell(furniture.x, level, postZ)] = Material.OAK_FENCE
                }
                for (canopyZ in furniture.z - 1..furniture.z + 1) cells[Cell(furniture.x, height, canopyZ)] = Material.WHITE_WOOL
            }
        }
        if (geometryKind == "farm") {
            val hayBale = Cell(width - 2, 1, depth - 2)
            if (hayBale !in standingPositions && cells[hayBale] == Material.AIR) {
                cells[hayBale] = Material.HAY_BLOCK
            }
        }
        if (geometryKind in setOf("tavern", "library", "community_hall") || garden) {
            for (anchor in socialAnchors) {
                val table = neighbors(Cell(anchor.offsetX, 1, anchor.offsetZ)).firstOrNull {
                    it.x in 1 until width && it.z in 1 until depth && it !in standingPositions &&
                        cells[it] == Material.AIR && cells[it.copy(y = 2)] == Material.AIR
                }
                require(table != null) { "No social furniture space near ${anchor.anchorId}" }
                when (geometryKind) {
                    "leisure_garden" -> cells[table] = Material.OAK_SLAB
                    "library" -> cells[table] = Material.BOOKSHELF
                    else -> {
                        cells[table] = Material.SPRUCE_FENCE
                        cells[table.copy(y = 2)] = Material.SPRUCE_PRESSURE_PLATE
                    }
                }
            }
        }
        val reachable = mutableSetOf<Cell>()
        val pending = ArrayDeque<Cell>()
        pending.add(Cell(entrances.first().offsetX, 1, entrances.first().offsetZ))
        while (pending.isNotEmpty()) {
            val current = pending.removeFirst()
            if (current in reachable || cells[current] != Material.AIR || cells[current.copy(y = 2)] != Material.AIR) continue
            reachable.add(current)
            neighbors(current).filter { it.x in 0..width && it.z in 0..depth }.forEach { pending.add(it) }
        }
        require(reachable.containsAll(standingPositions)) { "Furniture blocks access to an anchor" }
        return BuildingTemplate(
            id = definition.templateId,
            name = definition.displayName,
            description = "Building generated from semantic footprint and anchors",
            author = "AINPC",
            version = 1,
            blocks = cells.map { (cell, material) -> TemplateBlock(cell.x, cell.y, cell.z, material) },
            anchors = definition.anchors.associate { it.anchorId to Vector(it.offsetX, it.offsetY, it.offsetZ) },
            metadata = definition.metadata,
        )
    }

    private fun neighbors(cell: Cell) = listOf(
        cell.copy(x = cell.x + 1), cell.copy(x = cell.x - 1),
        cell.copy(z = cell.z + 1), cell.copy(z = cell.z - 1),
    )

    private data class Cell(val x: Int, val y: Int, val z: Int)

    companion object {
        fun supports(placeType: String?): Boolean = placeType in setOf("shop", "forge", "farm", "tavern", "market")
        fun supports(definition: BuildingTemplateDefinition?): Boolean = definition != null &&
            (supports(definition.placeType) ||
                definition.placeType == "house" && definition.metadata["geometry"] == "house_small" ||
                definition.placeType == "custom" &&
                definition.metadata["geometry"] in setOf("leisure_garden", "community_hall", "workshop", "bakery", "library"))
    }
}
