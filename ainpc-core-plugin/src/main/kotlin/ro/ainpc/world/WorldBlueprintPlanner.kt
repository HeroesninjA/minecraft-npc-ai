package ro.ainpc.world

import ro.ainpc.spawn.SpawnSemanticRules
import ro.ainpc.story.RegionStoryState
import ro.ainpc.world.exterior.ExteriorStructureBlueprint
import ro.ainpc.world.exterior.ExteriorStructureBlueprintCatalog
import ro.ainpc.world.exterior.ExteriorStructureReport
import ro.ainpc.world.exterior.ExteriorStructureType

internal data class WorldBlueprintProfessionPlan(
    val id: String,
    val maxNpcs: Int,
    val workplaceCount: Int,
    val mappedSlots: Long,
    val occupiedSlots: Long,
    val assignedWorkerCount: Int,
    val runtimeProfessionId: String? = null,
) {
    val capacityGap: Long get() = (maxNpcs.toLong() - mappedSlots).coerceAtLeast(0L)
}

internal data class WorldBlueprintPlan(
    val worldName: String,
    val completionIssues: List<String>,
    val mappingAvailable: Boolean,
    val professionPlans: List<WorldBlueprintProfessionPlan>,
    val mappingWarnings: List<String>,
    val declarativeRuleCount: Int,
    val compatibility: WorldBlueprintCompatibilityReport,
    val exteriorPlans: List<WorldBlueprintExteriorPlan>,
    val storyReadiness: List<WorldBlueprintStoryTargetReadiness>,
)

internal data class WorldBlueprintExteriorPlan(
    val type: ExteriorStructureType,
    val template: ExteriorStructureBlueprint?,
)

internal object WorldBlueprintPlanner {
    fun compile(
        blueprint: WorldBlueprint,
        mappedPlaces: Collection<WorldPlaceInfo>?,
        loadedProfessions: Collection<WorldBlueprintProfessionProfile>? = null,
        mappedRegions: Collection<WorldRegionInfo>? = null,
        persistentStoryStates: Map<String, RegionStoryState>? = null,
        exteriorReports: Collection<ExteriorStructureReport>? = null,
        currentFingerprint: String? = null,
    ): WorldBlueprintPlan {
        val catalog = loadedProfessions?.let(::WorldBlueprintProfessionCatalog)
        val compatibility = WorldBlueprintCompatibility.check(blueprint, mappedPlaces, catalog,
            mappedRegions, persistentStoryStates, exteriorReports)
        val warnings = mutableListOf<String>()
        val capacities = blueprint.professions.keys.associateWith { mutableListOf<WorldPlaceInfo>() }
        if (mappedPlaces != null) {
            mappedPlaces.asSequence()
                .filter { it.worldName() == blueprint.worldName && SpawnSemanticRules.isWorkplace(it) }
                .sortedBy { it.id() }
                .forEach { place ->
                    val references = listOfNotNull(place.metadata()["profession"], place.metadata()["occupation"]) + place.tags()
                    if (catalog != null) {
                        val resolutions = references.map { reference -> catalog.resolve(reference) }
                        if (resolutions.any { it.size > 1 } || resolutions.flatten().map { it.id }.distinct().size > 1) {
                            warnings.add("${place.id()}: referintele de meserie sunt ambigue in catalog; capacitatea nu este numarata")
                            return@forEach
                        }
                    }
                    val matches = blueprint.professions.keys.filter { profession ->
                        references.any { reference ->
                            catalog?.matches(profession, reference)
                                ?: SpawnSemanticRules.matchesAnyToken(reference, profession)
                        }
                    }
                    when (matches.size) {
                        0 -> Unit
                        1 -> capacities.getValue(matches.single()).add(place)
                        else -> warnings.add("${place.id()}: meserie ambigua (${matches.sorted().joinToString()})")
                    }
                }
        }
        val professionsByAssignedWorker = mutableMapOf<String, MutableSet<String>>()
        val professions = blueprint.professions.toSortedMap().map { (id, profession) ->
            val places = capacities.getValue(id)
            var slots = 0L
            var occupied = 0L
            val assignedWorkers = mutableSetOf<String>()
            places.forEach { place ->
                val workers = WorkplaceOccupancy.workerIds(place.metadata())
                val duplicates = workers.intersect(assignedWorkers)
                if (duplicates.isNotEmpty()) {
                    warnings.add("${place.id()}: ${duplicates.size} NPC atribuiti si altui loc al meseriei $id")
                }
                assignedWorkers.addAll(workers)
                workers.forEach { workerId ->
                    professionsByAssignedWorker.getOrPut(workerId) { mutableSetOf() }.add(id)
                }
                val rawCapacity = place.metadata()["max_workers"]
                val capacity = if (rawCapacity == null) 1 else rawCapacity.trim().toIntOrNull()
                if (capacity == null || capacity < 0) {
                    warnings.add("${place.id()}: max_workers invalid; capacitatea nu este numarata")
                } else {
                    slots += capacity.toLong()
                    occupied += workers.size.toLong()
                    if (workers.size > capacity) {
                        warnings.add("${place.id()}: ${workers.size} NPC legati depasesc max_workers=$capacity")
                    }
                }
            }
            if (assignedWorkers.size > profession.maxNpcs) {
                warnings.add("$id: ${assignedWorkers.size} NPC distincti atribuiti locurilor depasesc limita draftului ${profession.maxNpcs}")
            }
            WorldBlueprintProfessionPlan(id, profession.maxNpcs, places.size, slots, occupied,
                assignedWorkers.size,
                compatibility.resolvedProfessionIds[id])
        }
        val crossProfessionAssignments = professionsByAssignedWorker.values.count { it.size > 1 }
        if (crossProfessionAssignments > 0) {
            val subject = if (crossProfessionAssignments == 1) "ID NPC apare" else "ID-uri NPC apar"
            warnings.add("$crossProfessionAssignments $subject la locuri ale unor meserii diferite; verifica worker_npc_ids")
        }
        return WorldBlueprintPlan(
            blueprint.worldName,
            WorldBlueprintPolicy.completionIssues(blueprint),
            mappedPlaces != null,
            professions,
            warnings,
            blueprint.mapRules.size - compatibility.structuredRuleCount,
            compatibility,
            compatibility.exteriorTemplateTypes.map { type ->
                WorldBlueprintExteriorPlan(type, ExteriorStructureBlueprintCatalog.get(type))
            },
            WorldBlueprintStoryReadiness.inspect(blueprint.worldName,
                WorldBlueprintCompatibility.storyRegionIds(blueprint), mappedRegions, persistentStoryStates,
                currentFingerprint, blueprint.story),
        )
    }
}
