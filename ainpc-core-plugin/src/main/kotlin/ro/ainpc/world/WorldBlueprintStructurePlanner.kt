package ro.ainpc.world

import ro.ainpc.world.exterior.ExteriorStructureBlueprintCatalog
import ro.ainpc.world.exterior.ExteriorStructurePlan
import ro.ainpc.world.exterior.ExteriorStructurePlanner
import java.math.BigInteger
import java.util.Locale

internal enum class WorldBlueprintMappingKind(val label: String) {
    REGION("regiune"), PLACE("loc"), NODE("nod"),
}

internal data class WorldBlueprintMappingCollision(
    val kind: WorldBlueprintMappingKind,
    val id: String,
    val worldName: String,
)

internal data class WorldBlueprintRegionBounds(
    val minX: Int,
    val minY: Int,
    val minZ: Int,
    val maxX: Int,
    val maxY: Int,
    val maxZ: Int,
) {
    init {
        require(minX <= maxX && minY <= maxY && minZ <= maxZ) {
            "Coordonatele minime trebuie sa fie cel mult egale cu cele maxime pe fiecare axa."
        }
    }

    val width: Long get() = maxX.toLong() - minX + 1
    val height: Long get() = maxY.toLong() - minY + 1
    val depth: Long get() = maxZ.toLong() - minZ + 1
    val blockPositions: BigInteger
        get() = BigInteger.valueOf(width).multiply(BigInteger.valueOf(height)).multiply(BigInteger.valueOf(depth))

    companion object {
        fun parse(values: List<String>): WorldBlueprintRegionBounds {
            require(values.size == 6) { "Sunt necesare sase coordonate: minX minY minZ maxX maxY maxZ." }
            val coordinates = values.map { value ->
                value.toIntOrNull() ?: throw IllegalArgumentException("Coordonata '$value' nu este un intreg valid.")
            }
            return WorldBlueprintRegionBounds(coordinates[0], coordinates[1], coordinates[2],
                coordinates[3], coordinates[4], coordinates[5])
        }
    }
}

internal data class WorldBlueprintStructurePreview(
    val worldName: String,
    val structure: ExteriorStructurePlan,
    val mappingAvailable: Boolean,
    val collisions: List<WorldBlueprintMappingCollision>,
    val bounds: WorldBlueprintRegionBounds?,
    val overlappingRegionIds: List<String>?,
    val architecturePalette: List<String>,
    val architecturePaletteIssue: String?,
    val architecturePaletteSource: String?,
    val architectureStyle: String,
    val architectureStyleSource: String,
    val architectureStyleIssue: String?,
    val storyRegionReferenced: Boolean,
    val storyRegionRuleIssue: String?,
)

internal data class WorldBlueprintPlacementAssessment(
    val minBuildY: Int,
    val maxBuildYExclusive: Int,
    val withinBuildHeight: Boolean,
    val withinWorldBorder: Boolean,
)

internal object WorldBlueprintPlacementInspector {
    fun inspect(
        bounds: WorldBlueprintRegionBounds,
        minBuildY: Int,
        maxBuildYExclusive: Int,
        worldBorderContains: (Int, Int) -> Boolean,
    ): WorldBlueprintPlacementAssessment {
        require(minBuildY < maxBuildYExclusive) { "Limitele verticale ale lumii sunt invalide." }
        val withinBuildHeight = bounds.minY >= minBuildY && bounds.maxY < maxBuildYExclusive
        val corners = listOf(
            bounds.minX to bounds.minZ,
            bounds.minX to bounds.maxZ,
            bounds.maxX to bounds.minZ,
            bounds.maxX to bounds.maxZ,
        )
        return WorldBlueprintPlacementAssessment(minBuildY, maxBuildYExclusive, withinBuildHeight,
            corners.all { (blockX, blockZ) -> worldBorderContains(blockX, blockZ) })
    }
}

internal object WorldBlueprintStructurePlanner {
    fun compile(
        blueprint: WorldBlueprint,
        typeId: String,
        baseId: String,
        mappedRegions: Collection<WorldRegionInfo>?,
        mappedPlaces: Collection<WorldPlaceInfo>?,
        mappedNodes: Collection<WorldNodeInfo>?,
        bounds: WorldBlueprintRegionBounds? = null,
        isBuildableBlock: (String) -> Boolean = WorldBlueprintArchitecturePalette::isBuildableBlock,
        isKnownStructureBlock: (String) -> Boolean = WorldBlueprintForbiddenStructureBlocks::isValidBlock,
    ): WorldBlueprintStructurePreview {
        val compatibility = WorldBlueprintCompatibility.check(blueprint, null, null,
            isBuildableBlock = isBuildableBlock, isKnownStructureBlock = isKnownStructureBlock)
        require(compatibility.issues.none { it.code == WorldBlueprintCompatibilityCode.INVALID_EXTERIOR_TYPE_RULE }) {
            "Corecteaza listele required_exterior_types/forbidden_exterior_types inainte de planificare."
        }
        val normalizedType = typeId.trim().lowercase(Locale.ROOT)
        val type = compatibility.exteriorTemplateTypes.singleOrNull { it.id() == normalizedType }
        require(type != null) { "Tipul trebuie sa fie cerut prin required_exterior_types si sa nu fie interzis." }
        require(ExteriorStructureBlueprintCatalog.get(type) != null) {
            "Tipul ${type.id()} nu are model in catalog; necesita definire manuala."
        }
        val structure = ExteriorStructurePlanner().plan(type.id(), baseId)
        val regionId = structure.regionId()
        val prefix = "$regionId:"
        val collisions = buildList {
            mappedRegions.orEmpty().filter { it.id() == regionId }.forEach { region ->
                add(WorldBlueprintMappingCollision(WorldBlueprintMappingKind.REGION, region.id(), region.worldName()))
            }
            mappedPlaces.orEmpty().filter { it.regionId() == regionId || it.id().startsWith(prefix) }.forEach { place ->
                add(WorldBlueprintMappingCollision(WorldBlueprintMappingKind.PLACE, place.id(), place.worldName()))
            }
            mappedNodes.orEmpty().filter { it.regionId() == regionId || it.id().startsWith(prefix) }.forEach { node ->
                add(WorldBlueprintMappingCollision(WorldBlueprintMappingKind.NODE, node.id(), node.worldName()))
            }
        }.distinct().sortedWith(compareBy({ it.kind }, { it.id }, { it.worldName }))
        val overlappingRegionIds = if (bounds != null && mappedRegions != null) {
            mappedRegions.asSequence()
                .filter { it.worldName().equals(blueprint.worldName, ignoreCase = true) }
                .filter { region ->
                    overlaps(bounds.minX, bounds.maxX, region.minX(), region.maxX()) &&
                        overlaps(bounds.minY, bounds.maxY, region.minY(), region.maxY()) &&
                        overlaps(bounds.minZ, bounds.maxZ, region.minZ(), region.maxZ())
                }
                .map { it.id() }.distinct().sorted().toList()
        } else null
        val typePalette = compatibility.structurePalettes[type]
        val effectivePalette = typePalette ?: compatibility.architecturePalette
        val typeStyle = compatibility.structureStyles[type]
        val paletteIssues = compatibility.issues.filter { it.code == WorldBlueprintCompatibilityCode.INVALID_ARCHITECTURE_PALETTE_RULE ||
            it.code == WorldBlueprintCompatibilityCode.INVALID_STRUCTURE_PALETTES_RULE }
        val storyRegionRuleIssue = compatibility.issues.firstOrNull {
            it.code == WorldBlueprintCompatibilityCode.INVALID_STORY_REGION_RULE
        }?.message
        val styleIssue = compatibility.issues.firstOrNull {
            it.code == WorldBlueprintCompatibilityCode.INVALID_STRUCTURE_STYLES_RULE
        }?.message
        return WorldBlueprintStructurePreview(blueprint.worldName, structure,
            mappedRegions != null && mappedPlaces != null && mappedNodes != null, collisions,
            bounds, overlappingRegionIds, effectivePalette,
            paletteIssues.takeIf { it.isNotEmpty() }?.joinToString("; ") { it.message },
            when {
                typePalette != null -> "${WorldBlueprintStructurePalettes.KEY}:${type.id()}"
                effectivePalette.isNotEmpty() -> WorldBlueprintArchitecturePalette.KEY
                else -> null
            }, typeStyle ?: blueprint.architecture,
            if (typeStyle != null) "${WorldBlueprintStructureStyles.KEY}:${type.id()}" else "architecture",
            styleIssue, storyRegionRuleIssue == null && regionId in WorldBlueprintCompatibility.storyRegionIds(blueprint),
            storyRegionRuleIssue)
    }

    private fun overlaps(minimum: Int, maximum: Int, otherMinimum: Int, otherMaximum: Int): Boolean =
        maximum >= otherMinimum && otherMaximum >= minimum
}
