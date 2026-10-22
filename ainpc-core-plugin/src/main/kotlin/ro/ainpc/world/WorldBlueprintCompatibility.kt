package ro.ainpc.world

import ro.ainpc.spawn.SpawnSemanticRules
import ro.ainpc.story.RegionStoryState
import ro.ainpc.story.StoryStateViewResolver
import ro.ainpc.world.exterior.ExteriorStructureReport
import ro.ainpc.world.exterior.ExteriorStructureType
import java.util.Locale

internal data class WorldBlueprintProfessionProfile(
    val id: String,
    val name: String,
    val aliases: List<String> = emptyList(),
)

internal enum class WorldBlueprintCompatibilityCode {
    UNKNOWN_PROFESSION,
    AMBIGUOUS_PROFESSION,
    DUPLICATE_PROFESSION,
    INVALID_PLACE_TYPE_RULE,
    CONFLICTING_PLACE_TYPE_RULES,
    REQUIRED_PLACE_TYPE_MISSING,
    FORBIDDEN_PLACE_TYPE_PRESENT,
    INVALID_REGION_TYPE_RULE,
    CONFLICTING_REGION_TYPE_RULES,
    REQUIRED_REGION_TYPE_MISSING,
    FORBIDDEN_REGION_TYPE_PRESENT,
    INVALID_STORY_REGION_RULE,
    STORY_REGION_MISSING,
    INVALID_EXTERIOR_TYPE_RULE,
    CONFLICTING_EXTERIOR_TYPE_RULES,
    REQUIRED_EXTERIOR_TYPE_MISSING,
    FORBIDDEN_EXTERIOR_TYPE_PRESENT,
    REQUIRED_EXTERIOR_TYPE_UNREADY,
    INVALID_STRUCTURE_BIOME_RULE,
    CONFLICTING_STRUCTURE_BIOME_RULES,
    INVALID_STRUCTURE_SURFACE_SPAN_RULE,
    INVALID_STRUCTURE_REGION_POSITIONS_RULE,
    INVALID_STRUCTURE_NON_AIR_RULE,
    INVALID_STRUCTURE_FLUID_RULE,
    INVALID_BASE_SUPPORT_RULE,
    INVALID_WORLD_SEED_RULE,
    INVALID_WORLD_TYPE_RULE,
    INVALID_WORLD_STRUCTURES_RULE,
    INVALID_FORBIDDEN_STRUCTURE_BLOCKS_RULE,
    INVALID_ARCHITECTURE_PALETTE_RULE,
    INVALID_STRUCTURE_PALETTES_RULE,
    CONFLICTING_STRUCTURE_PALETTE_RULES,
    INVALID_STRUCTURE_STYLES_RULE,
    CONFLICTING_STRUCTURE_STYLE_RULES,
}

internal data class WorldBlueprintCompatibilityIssue(
    val code: WorldBlueprintCompatibilityCode,
    val subject: String,
    val message: String,
)

internal data class WorldBlueprintStoryRegionLink(
    val id: String,
    val stateKey: String,
    val mode: StoryMode,
    val poolSize: Int,
    val hasStateDescription: Boolean,
    val source: String,
)

internal data class WorldBlueprintCompatibilityReport(
    val professionCatalogAvailable: Boolean,
    val resolvedProfessionIds: Map<String, String>,
    val structuredRuleCount: Int,
    val issues: List<WorldBlueprintCompatibilityIssue>,
    val regionMappingAvailable: Boolean,
    val storyRegionLinks: List<WorldBlueprintStoryRegionLink>,
    val storyStateAvailable: Boolean,
    val exteriorAnalysisAvailable: Boolean,
    val exteriorTemplateTypes: List<ExteriorStructureType>,
    val architecturePalette: List<String>,
    val structurePalettes: Map<ExteriorStructureType, List<String>>,
    val structureStyles: Map<ExteriorStructureType, String>,
)

internal class WorldBlueprintProfessionCatalog(profiles: Collection<WorldBlueprintProfessionProfile>) {
    private val profiles = profiles.sortedBy { it.id }

    fun resolve(reference: String): List<WorldBlueprintProfessionProfile> {
        val normalized = reference.trim().lowercase(Locale.ROOT)
        if (normalized.isBlank()) return emptyList()
        val direct = profiles.filter { it.id.trim().lowercase(Locale.ROOT) == normalized }
        if (direct.isNotEmpty()) return direct
        return profiles.filter { profile ->
            (profile.aliases + profile.name).any { it.trim().lowercase(Locale.ROOT) == normalized }
        }
    }

    fun matches(draftId: String, reference: String?): Boolean {
        if (reference.isNullOrBlank()) return false
        val draftMatches = resolve(draftId)
        if (draftMatches.isEmpty()) return SpawnSemanticRules.matchesAnyToken(reference, draftId)
        val profile = draftMatches.singleOrNull() ?: return false
        return resolve(reference).singleOrNull()?.id == profile.id
    }
}

internal object WorldBlueprintCompatibility {
    private val ruleKeys = setOf("required_place_types", "forbidden_place_types",
        "required_region_types", "forbidden_region_types", "story_region_ids",
        "required_exterior_types", "forbidden_exterior_types",
        WorldBlueprintStructureBiomeRules.ALLOWED_KEY, WorldBlueprintStructureBiomeRules.FORBIDDEN_KEY,
        WorldBlueprintStructureSurfaceSpanRule.KEY, WorldBlueprintStructureRegionPositionsRule.KEY,
        WorldBlueprintStructureNonAirRule.KEY,
        WorldBlueprintStructureFluidRule.KEY,
        WorldBlueprintBaseSupportRule.KEY,
        WorldBlueprintWorldSeedRule.KEY,
        WorldBlueprintWorldGenerationRules.TYPE_KEY, WorldBlueprintWorldGenerationRules.STRUCTURES_KEY,
        WorldBlueprintForbiddenStructureBlocks.KEY,
        WorldBlueprintArchitecturePalette.KEY, WorldBlueprintStructurePalettes.KEY,
        WorldBlueprintStructureStyles.KEY)
    private val placeTypes = PlaceType.entries.associateBy { it.id }
    private val regionTypes = RegionType.entries.associateBy { it.id }
    private val exteriorTypes = ExteriorStructureType.entries.associateBy { it.id() }
    private val conflictingRuleKeys = setOf("required_place_types", "forbidden_place_types",
        "required_region_types", "forbidden_region_types", "required_exterior_types", "forbidden_exterior_types",
        WorldBlueprintStructureBiomeRules.ALLOWED_KEY, WorldBlueprintStructureBiomeRules.FORBIDDEN_KEY,
        WorldBlueprintStructurePalettes.KEY, WorldBlueprintStructureStyles.KEY)
    private val conflictingRuleCodes = setOf(WorldBlueprintCompatibilityCode.CONFLICTING_PLACE_TYPE_RULES,
        WorldBlueprintCompatibilityCode.CONFLICTING_REGION_TYPE_RULES,
        WorldBlueprintCompatibilityCode.CONFLICTING_EXTERIOR_TYPE_RULES,
        WorldBlueprintCompatibilityCode.CONFLICTING_STRUCTURE_BIOME_RULES,
        WorldBlueprintCompatibilityCode.CONFLICTING_STRUCTURE_PALETTE_RULES,
        WorldBlueprintCompatibilityCode.CONFLICTING_STRUCTURE_STYLE_RULES)

    fun structuredRuleKeys(): List<String> = ruleKeys.sorted()

    fun structuredRuleSyntaxIssues(key: String, value: String): List<WorldBlueprintCompatibilityIssue> {
        if (key !in ruleKeys) return emptyList()
        return check(WorldBlueprint("validation", mapRules = mapOf(key to value)), null, null).issues
    }

    fun introducedStructuredRuleConflicts(
        base: WorldBlueprint, candidate: WorldBlueprint,
        isBuildableBlock: (String) -> Boolean = WorldBlueprintArchitecturePalette::isBuildableBlock,
    ): List<WorldBlueprintCompatibilityIssue> {
        val existing = structuredRuleConflicts(base.mapRules, isBuildableBlock).map { it.code to it.subject }.toSet()
        return structuredRuleConflicts(candidate.mapRules, isBuildableBlock)
            .filter { (it.code to it.subject) !in existing }
    }

    private fun structuredRuleConflicts(
        mapRules: Map<String, String>, isBuildableBlock: (String) -> Boolean,
    ): List<WorldBlueprintCompatibilityIssue> =
        check(WorldBlueprint("validation", mapRules = mapRules.filterKeys { it in conflictingRuleKeys }), null, null,
            isBuildableBlock = isBuildableBlock)
            .issues.filter { it.code in conflictingRuleCodes }

    fun check(
        blueprint: WorldBlueprint,
        mappedPlaces: Collection<WorldPlaceInfo>?,
        catalog: WorldBlueprintProfessionCatalog?,
        mappedRegions: Collection<WorldRegionInfo>? = null,
        persistentStoryStates: Map<String, RegionStoryState>? = null,
        exteriorReports: Collection<ExteriorStructureReport>? = null,
        isBuildableBlock: (String) -> Boolean = WorldBlueprintArchitecturePalette::isBuildableBlock,
        isKnownStructureBlock: (String) -> Boolean = WorldBlueprintForbiddenStructureBlocks::isValidBlock,
    ): WorldBlueprintCompatibilityReport {
        val issues = mutableListOf<WorldBlueprintCompatibilityIssue>()
        val resolved = linkedMapOf<String, String>()
        val storyRegionLinks = mutableListOf<WorldBlueprintStoryRegionLink>()
        if (catalog != null) {
            blueprint.professions.keys.sorted().forEach { id ->
                val matches = catalog.resolve(id)
                when (matches.size) {
                    0 -> issues.add(WorldBlueprintCompatibilityIssue(WorldBlueprintCompatibilityCode.UNKNOWN_PROFESSION,
                        id, "$id: nu exista un profil de meserie incarcat; definitia din draft ramane declarativa"))
                    1 -> resolved[id] = matches.single().id
                    else -> issues.add(WorldBlueprintCompatibilityIssue(WorldBlueprintCompatibilityCode.AMBIGUOUS_PROFESSION,
                        id, "$id: referinta ambigua in catalog (${matches.map { it.id }.joinToString()})"))
                }
            }
            resolved.entries.groupBy { it.value }.toSortedMap().forEach { (profileId, entries) ->
                if (entries.size > 1) {
                    issues.add(WorldBlueprintCompatibilityIssue(WorldBlueprintCompatibilityCode.DUPLICATE_PROFESSION,
                        profileId, "${entries.map { it.key }.sorted().joinToString()}: aceeasi meserie $profileId apare de mai multe ori in draft"))
                }
            }
        }
        val required = parseTypes(blueprint.mapRules["required_place_types"], "required_place_types",
            placeTypes, WorldBlueprintCompatibilityCode.INVALID_PLACE_TYPE_RULE, issues)
        val forbidden = parseTypes(blueprint.mapRules["forbidden_place_types"], "forbidden_place_types",
            placeTypes, WorldBlueprintCompatibilityCode.INVALID_PLACE_TYPE_RULE, issues)
        required.intersect(forbidden).sortedBy { it.id }.forEach { type ->
            issues.add(WorldBlueprintCompatibilityIssue(WorldBlueprintCompatibilityCode.CONFLICTING_PLACE_TYPE_RULES,
                type.id, "${type.id}: tipul este simultan obligatoriu si interzis"))
        }
        if (mappedPlaces != null) {
            val placesByType = mappedPlaces.filter { it.worldName() == blueprint.worldName }.groupBy { it.placeType() }
            required.sortedBy { it.id }.forEach { type ->
                if (placesByType[type].isNullOrEmpty()) {
                    issues.add(WorldBlueprintCompatibilityIssue(WorldBlueprintCompatibilityCode.REQUIRED_PLACE_TYPE_MISSING,
                        type.id, "${type.id}: niciun loc de acest tip obligatoriu nu este mapat in ${blueprint.worldName}"))
                }
            }
            forbidden.sortedBy { it.id }.forEach { type ->
                val places = placesByType[type].orEmpty()
                if (places.isNotEmpty()) {
                    val examples = places.map { it.id() }.sorted().take(5).joinToString()
                    issues.add(WorldBlueprintCompatibilityIssue(WorldBlueprintCompatibilityCode.FORBIDDEN_PLACE_TYPE_PRESENT,
                        type.id, "${type.id}: ${places.size} locuri mapate au un tip interzis de draft (exemple: $examples)"))
                }
            }
        }
        val requiredRegions = parseTypes(blueprint.mapRules["required_region_types"], "required_region_types",
            regionTypes, WorldBlueprintCompatibilityCode.INVALID_REGION_TYPE_RULE, issues)
        val forbiddenRegions = parseTypes(blueprint.mapRules["forbidden_region_types"], "forbidden_region_types",
            regionTypes, WorldBlueprintCompatibilityCode.INVALID_REGION_TYPE_RULE, issues)
        requiredRegions.intersect(forbiddenRegions).sortedBy { it.id }.forEach { type ->
            issues.add(WorldBlueprintCompatibilityIssue(WorldBlueprintCompatibilityCode.CONFLICTING_REGION_TYPE_RULES,
                type.id, "${type.id}: tipul de regiune este simultan obligatoriu si interzis"))
        }
        if (mappedRegions != null) {
            val regionsByType = mappedRegions.filter { it.worldName() == blueprint.worldName }
                .groupBy { it.typeId().trim().lowercase(Locale.ROOT) }
            requiredRegions.sortedBy { it.id }.forEach { type ->
                if (regionsByType[type.id].isNullOrEmpty()) {
                    issues.add(WorldBlueprintCompatibilityIssue(WorldBlueprintCompatibilityCode.REQUIRED_REGION_TYPE_MISSING,
                        type.id, "${type.id}: nicio regiune de acest tip obligatoriu nu este mapata in ${blueprint.worldName}"))
                }
            }
            forbiddenRegions.sortedBy { it.id }.forEach { type ->
                val regions = regionsByType[type.id].orEmpty()
                if (regions.isNotEmpty()) {
                    val examples = regions.map { it.id() }.sorted().take(5).joinToString()
                    issues.add(WorldBlueprintCompatibilityIssue(WorldBlueprintCompatibilityCode.FORBIDDEN_REGION_TYPE_PRESENT,
                        type.id, "${type.id}: ${regions.size} regiuni mapate au un tip interzis de draft (exemple: $examples)"))
                }
            }
        }
        val requiredExterior = parseTypes(blueprint.mapRules["required_exterior_types"], "required_exterior_types",
            exteriorTypes, WorldBlueprintCompatibilityCode.INVALID_EXTERIOR_TYPE_RULE, issues)
        val forbiddenExterior = parseTypes(blueprint.mapRules["forbidden_exterior_types"], "forbidden_exterior_types",
            exteriorTypes, WorldBlueprintCompatibilityCode.INVALID_EXTERIOR_TYPE_RULE, issues)
        requiredExterior.intersect(forbiddenExterior).sortedBy { it.id() }.forEach { type ->
            issues.add(WorldBlueprintCompatibilityIssue(WorldBlueprintCompatibilityCode.CONFLICTING_EXTERIOR_TYPE_RULES,
                type.id(), "${type.id()}: tipul exterior este simultan obligatoriu si interzis"))
        }
        if (mappedRegions != null && exteriorReports != null) {
            val regionIds = mappedRegions.filter { it.worldName() == blueprint.worldName }.map { it.id() }.toSet()
            val reportsByType = exteriorReports.filter { it.regionId() in regionIds }.groupBy { it.type() }
            requiredExterior.sortedBy { it.id() }.forEach { type ->
                val reports = reportsByType[type].orEmpty()
                if (reports.isEmpty()) {
                    issues.add(WorldBlueprintCompatibilityIssue(WorldBlueprintCompatibilityCode.REQUIRED_EXTERIOR_TYPE_MISSING,
                        type.id(), "${type.id()}: nicio regiune mapata nu este clasificata ca acest tip exterior in ${blueprint.worldName}"))
                } else {
                    val unready = reports.filterNot { it.success() }
                    if (unready.isNotEmpty()) {
                        issues.add(WorldBlueprintCompatibilityIssue(WorldBlueprintCompatibilityCode.REQUIRED_EXTERIOR_TYPE_UNREADY,
                            type.id(), "${type.id()}: ${unready.size} regiuni clasificate au erori in analiza structurii (exemple: ${unready.map { it.regionId() }.sorted().take(5).joinToString()})"))
                    }
                }
            }
            forbiddenExterior.sortedBy { it.id() }.forEach { type ->
                val reports = reportsByType[type].orEmpty()
                if (reports.isNotEmpty()) {
                    issues.add(WorldBlueprintCompatibilityIssue(WorldBlueprintCompatibilityCode.FORBIDDEN_EXTERIOR_TYPE_PRESENT,
                        type.id(), "${type.id()}: ${reports.size} regiuni mapate sunt clasificate ca tip exterior interzis (exemple: ${reports.map { it.regionId() }.sorted().take(5).joinToString()})"))
                }
            }
        }
        val storyRegionIds = parseStoryRegionIds(blueprint.mapRules["story_region_ids"], issues)
        val biomeRules = WorldBlueprintStructureBiomeRules.parse(blueprint.mapRules)
        biomeRules.invalidKeys.forEach { key ->
            issues.add(WorldBlueprintCompatibilityIssue(WorldBlueprintCompatibilityCode.INVALID_STRUCTURE_BIOME_RULE,
                key, "$key: foloseste cel mult 64 de ID-uri de biom valide, separate prin virgula (de exemplu minecraft:plains)"))
        }
        biomeRules.conflicts.sorted().forEach { biome ->
            issues.add(WorldBlueprintCompatibilityIssue(WorldBlueprintCompatibilityCode.CONFLICTING_STRUCTURE_BIOME_RULES,
                biome, "$biome: biomul este simultan permis si interzis pentru structuri"))
        }
        if (!WorldBlueprintStructureSurfaceSpanRule.parse(blueprint.mapRules).isValid) {
            issues.add(WorldBlueprintCompatibilityIssue(WorldBlueprintCompatibilityCode.INVALID_STRUCTURE_SURFACE_SPAN_RULE,
                WorldBlueprintStructureSurfaceSpanRule.KEY,
                "${WorldBlueprintStructureSurfaceSpanRule.KEY}: foloseste un numar intreg nenegativ de blocuri"))
        }
        if (!WorldBlueprintStructureRegionPositionsRule.parse(blueprint.mapRules).isValid) {
            issues.add(WorldBlueprintCompatibilityIssue(WorldBlueprintCompatibilityCode.INVALID_STRUCTURE_REGION_POSITIONS_RULE,
                WorldBlueprintStructureRegionPositionsRule.KEY,
                "${WorldBlueprintStructureRegionPositionsRule.KEY}: foloseste un numar intreg pozitiv, cel mult 2^96 pozitii"))
        }
        if (!WorldBlueprintStructureNonAirRule.parse(blueprint.mapRules).isValid) {
            issues.add(WorldBlueprintCompatibilityIssue(WorldBlueprintCompatibilityCode.INVALID_STRUCTURE_NON_AIR_RULE,
                WorldBlueprintStructureNonAirRule.KEY,
                "${WorldBlueprintStructureNonAirRule.KEY}: foloseste un numar intreg nenegativ, cel mult 2^96 pozitii"))
        }
        if (!WorldBlueprintStructureFluidRule.parse(blueprint.mapRules).isValid) {
            issues.add(WorldBlueprintCompatibilityIssue(WorldBlueprintCompatibilityCode.INVALID_STRUCTURE_FLUID_RULE,
                WorldBlueprintStructureFluidRule.KEY,
                "${WorldBlueprintStructureFluidRule.KEY}: foloseste un numar intreg nenegativ, cel mult 2^96 pozitii"))
        }
        if (!WorldBlueprintBaseSupportRule.parse(blueprint.mapRules).isValid) {
            issues.add(WorldBlueprintCompatibilityIssue(WorldBlueprintCompatibilityCode.INVALID_BASE_SUPPORT_RULE,
                WorldBlueprintBaseSupportRule.KEY,
                "${WorldBlueprintBaseSupportRule.KEY}: foloseste un numar intreg de la 0 la 5 puncte"))
        }
        if (!WorldBlueprintWorldSeedRule.parse(blueprint.mapRules).isValid) {
            issues.add(WorldBlueprintCompatibilityIssue(WorldBlueprintCompatibilityCode.INVALID_WORLD_SEED_RULE,
                WorldBlueprintWorldSeedRule.KEY,
                "${WorldBlueprintWorldSeedRule.KEY}: foloseste un intreg zecimal pe 64 de biti, optional negativ"))
        }
        val worldGenerationRules = WorldBlueprintWorldGenerationRules.parse(blueprint.mapRules)
        if (WorldBlueprintWorldGenerationRules.TYPE_KEY in worldGenerationRules.invalidKeys) {
            issues.add(WorldBlueprintCompatibilityIssue(WorldBlueprintCompatibilityCode.INVALID_WORLD_TYPE_RULE,
                WorldBlueprintWorldGenerationRules.TYPE_KEY,
                "${WorldBlueprintWorldGenerationRules.TYPE_KEY}: foloseste normal, flat, large_biomes sau amplified"))
        }
        if (WorldBlueprintWorldGenerationRules.STRUCTURES_KEY in worldGenerationRules.invalidKeys) {
            issues.add(WorldBlueprintCompatibilityIssue(WorldBlueprintCompatibilityCode.INVALID_WORLD_STRUCTURES_RULE,
                WorldBlueprintWorldGenerationRules.STRUCTURES_KEY,
                "${WorldBlueprintWorldGenerationRules.STRUCTURES_KEY}: foloseste true sau false"))
        }
        WorldBlueprintForbiddenStructureBlocks.parse(blueprint.mapRules, isKnownStructureBlock).error?.let { error ->
            issues.add(WorldBlueprintCompatibilityIssue(WorldBlueprintCompatibilityCode.INVALID_FORBIDDEN_STRUCTURE_BLOCKS_RULE,
                WorldBlueprintForbiddenStructureBlocks.KEY, error))
        }
        val architecturePalette = WorldBlueprintArchitecturePalette.parse(
            blueprint.mapRules[WorldBlueprintArchitecturePalette.KEY], isBuildableBlock)
        architecturePalette?.error?.let { error ->
            issues.add(WorldBlueprintCompatibilityIssue(WorldBlueprintCompatibilityCode.INVALID_ARCHITECTURE_PALETTE_RULE,
                WorldBlueprintArchitecturePalette.KEY, error))
        }
        val structurePalettes = WorldBlueprintStructurePalettes.parse(
            blueprint.mapRules[WorldBlueprintStructurePalettes.KEY], isBuildableBlock)
        structurePalettes?.error?.let { error ->
            issues.add(WorldBlueprintCompatibilityIssue(WorldBlueprintCompatibilityCode.INVALID_STRUCTURE_PALETTES_RULE,
                WorldBlueprintStructurePalettes.KEY, error))
        }
        structurePalettes?.byType?.keys?.intersect(forbiddenExterior)?.sortedBy { it.id() }?.forEach { type ->
            issues.add(WorldBlueprintCompatibilityIssue(WorldBlueprintCompatibilityCode.CONFLICTING_STRUCTURE_PALETTE_RULES,
                type.id(), "${type.id()}: paleta arhitecturala este definita pentru un tip exterior interzis; elimina paleta sau interdictia"))
        }
        val structureStyles = WorldBlueprintStructureStyles.parse(blueprint.mapRules[WorldBlueprintStructureStyles.KEY])
        structureStyles?.error?.let { error ->
            issues.add(WorldBlueprintCompatibilityIssue(WorldBlueprintCompatibilityCode.INVALID_STRUCTURE_STYLES_RULE,
                WorldBlueprintStructureStyles.KEY, error))
        }
        structureStyles?.byType?.keys?.intersect(forbiddenExterior)?.sortedBy { it.id() }?.forEach { type ->
            issues.add(WorldBlueprintCompatibilityIssue(WorldBlueprintCompatibilityCode.CONFLICTING_STRUCTURE_STYLE_RULES,
                type.id(), "${type.id()}: stilul arhitectural este definit pentru un tip exterior interzis; elimina stilul sau interdictia"))
        }
        if (mappedRegions != null) {
            val regionsById = mappedRegions.filter { it.worldName() == blueprint.worldName }.associateBy { it.id() }
            storyRegionIds.forEach { id ->
                val region = regionsById[id]
                if (region == null) {
                    issues.add(WorldBlueprintCompatibilityIssue(WorldBlueprintCompatibilityCode.STORY_REGION_MISSING,
                        id, "$id: regiunea pentru poveste nu este mapata in ${blueprint.worldName}"))
                } else {
                    val state = requireNotNull(StoryStateViewResolver.region(region, persistentStoryStates?.get(id)))
                    storyRegionLinks.add(WorldBlueprintStoryRegionLink(id, state.stateKey, state.mode,
                        state.pool.size, state.description.isNotBlank(), state.source))
                }
            }
        }
        return WorldBlueprintCompatibilityReport(catalog != null, resolved,
            blueprint.mapRules.keys.count { it in ruleKeys }, issues, mappedRegions != null,
            storyRegionLinks, persistentStoryStates != null, mappedRegions != null && exteriorReports != null,
            (requiredExterior - forbiddenExterior).sortedBy { it.id() }, architecturePalette?.blockIds.orEmpty(),
            structurePalettes?.byType.orEmpty(), structureStyles?.byType.orEmpty())
    }

    fun storyRegionIds(blueprint: WorldBlueprint): List<String> =
        parseStoryRegionIds(blueprint.mapRules["story_region_ids"], mutableListOf())

    private fun parseStoryRegionIds(
        raw: String?, issues: MutableList<WorldBlueprintCompatibilityIssue>,
    ): List<String> {
        if (raw == null) return emptyList()
        val ids = raw.split(',').map { it.trim() }
        if (ids.any { it.isEmpty() }) {
            issues.add(WorldBlueprintCompatibilityIssue(WorldBlueprintCompatibilityCode.INVALID_STORY_REGION_RULE,
                "story_region_ids", "story_region_ids: foloseste ID-uri de regiuni nevide separate prin virgula"))
            return emptyList()
        }
        val distinct = ids.distinct()
        if (distinct.size > 64) {
            issues.add(WorldBlueprintCompatibilityIssue(WorldBlueprintCompatibilityCode.INVALID_STORY_REGION_RULE,
                "story_region_ids", "story_region_ids: sunt permise cel mult 64 de ID-uri de regiuni"))
            return emptyList()
        }
        return distinct
    }

    private fun <T> parseTypes(
        raw: String?, key: String, allowedTypes: Map<String, T>,
        invalidCode: WorldBlueprintCompatibilityCode, issues: MutableList<WorldBlueprintCompatibilityIssue>,
    ): Set<T> {
        if (raw == null) return emptySet()
        val tokens = raw.split(',').map { it.trim().lowercase(Locale.ROOT) }
        val invalid = tokens.filter { it !in allowedTypes }
        if (invalid.isNotEmpty()) {
            val values = invalid.distinct().joinToString { it.ifBlank { "<gol>" } }
            issues.add(WorldBlueprintCompatibilityIssue(invalidCode,
                key, "$key: ID-uri de tip invalide ($values); regula necesita ID-uri separate prin virgula: ${allowedTypes.keys.joinToString()}"))
            return emptySet()
        }
        return tokens.map { allowedTypes.getValue(it) }.toSet()
    }
}
