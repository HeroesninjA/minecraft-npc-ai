package ro.ainpc.world

import java.util.UUID
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
    val managedNpcCount: Int? = null,
) {
    val capacityGap: Long get() = (maxNpcs.toLong() - mappedSlots).coerceAtLeast(0L)
    val managedNpcDelta: Int? get() = managedNpcCount?.let { maxNpcs - it }
}

internal data class WorldBlueprintNpcSnapshot(
    val worldName: String?,
    val occupation: String?,
    val uuid: UUID? = null,
    val databaseId: Int? = null,
)

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
        managedNpcs: Collection<WorldBlueprintNpcSnapshot>? = null,
        isKnownStructureBlock: (String) -> Boolean = WorldBlueprintForbiddenStructureBlocks::isValidBlock,
    ): WorldBlueprintPlan {
        val catalog = loadedProfessions?.let(::WorldBlueprintProfessionCatalog)
        val compatibility = WorldBlueprintCompatibility.check(blueprint, mappedPlaces, catalog,
            mappedRegions, persistentStoryStates, exteriorReports,
            isKnownStructureBlock = isKnownStructureBlock)
        val warnings = mutableListOf<String>()
        val managedNpcCounts = managedNpcs?.let { snapshots ->
            val counts = blueprint.professions.keys.associateWith { 0 }.toMutableMap()
            var ambiguous = 0
            var unmatched = 0
            var unknownOccupation = 0
            snapshots.filter { it.worldName == blueprint.worldName }.forEach { npc ->
                val occupation = npc.occupation?.takeIf { it.isNotBlank() }
                if (occupation == null) {
                    unknownOccupation++
                    return@forEach
                }
                val matches = blueprint.professions.keys.filter { profession ->
                    catalog?.matches(profession, occupation)
                        ?: SpawnSemanticRules.matchesAnyToken(occupation, profession)
                }
                val match = matches.singleOrNull()
                when {
                    match != null -> counts[match] = counts.getValue(match) + 1
                    matches.isEmpty() -> unmatched++
                    else -> ambiguous++
                }
            }
            if (ambiguous > 0) {
                warnings.add("NPC gestionati cu ocupatii ambigue intre meseriile draftului: $ambiguous; exclusi din calculul limitelor")
            }
            if (unmatched > 0) {
                warnings.add("NPC gestionati cu ocupatii fara corespondent in draft: $unmatched; exclusi din calculul limitelor")
            }
            if (unknownOccupation > 0) {
                warnings.add("NPC gestionati fara ocupatie in lumea draftului: $unknownOccupation; exclusi din calculul limitelor")
            }
            counts
        }
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
            val managedCount = managedNpcCounts?.getValue(id)
            if (managedCount != null && managedCount > profession.maxNpcs) {
                warnings.add("$id: numarul de NPC gestionati ($managedCount) depaseste limita draftului ${profession.maxNpcs}")
            }
            WorldBlueprintProfessionPlan(id, profession.maxNpcs, places.size, slots, occupied,
                assignedWorkers.size,
                compatibility.resolvedProfessionIds[id], managedCount)
        }
        val crossProfessionAssignments = professionsByAssignedWorker.values.count { it.size > 1 }
        if (crossProfessionAssignments > 0) {
            val subject = if (crossProfessionAssignments == 1) "ID NPC apare" else "ID-uri NPC apar"
            warnings.add("$crossProfessionAssignments $subject la locuri ale unor meserii diferite; verifica worker_npc_ids")
        }
        if (mappedPlaces != null && managedNpcs != null) {
            warnings.addAll(reconcileAssignedWorkers(blueprint, professionsByAssignedWorker, managedNpcs, catalog))
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

    private fun reconcileAssignedWorkers(
        blueprint: WorldBlueprint,
        assignments: Map<String, Set<String>>,
        managedNpcs: Collection<WorldBlueprintNpcSnapshot>,
        catalog: WorldBlueprintProfessionCatalog?,
    ): List<String> {
        val byCanonicalId = mutableMapOf<String, MutableList<WorldBlueprintNpcSnapshot>>()
        managedNpcs.forEach { npc ->
            npc.uuid?.let { byCanonicalId.getOrPut(it.toString()) { mutableListOf() }.add(npc) }
            npc.databaseId?.takeIf { it > 0 }?.let {
                byCanonicalId.getOrPut("npc_$it") { mutableListOf() }.add(npc)
            }
        }
        var unchecked = 0
        var absentFromCache = 0
        var ambiguousIdentity = 0
        var otherWorld = 0
        var differentProfession = 0
        var unknownOccupation = 0
        assignments.forEach { (workerId, professions) ->
            val canonicalId = canonicalWorkerId(workerId)
            if (canonicalId == null) {
                unchecked++
                return@forEach
            }
            val matches = byCanonicalId[canonicalId].orEmpty()
            when {
                matches.isEmpty() -> absentFromCache++
                matches.size > 1 -> ambiguousIdentity++
                matches.single().worldName != blueprint.worldName -> otherWorld++
                professions.size == 1 -> {
                    val occupation = matches.single().occupation?.takeIf { it.isNotBlank() }
                    if (occupation == null) {
                        unknownOccupation++
                    } else {
                        val profession = professions.single()
                        val compatible = catalog?.matches(profession, occupation)
                            ?: SpawnSemanticRules.matchesAnyToken(occupation, profession)
                        if (!compatible) differentProfession++
                    }
                }
            }
        }
        return buildList {
            if (unchecked > 0) add("ID-uri work in format liber, necorelate sigur cu NPC gestionati: $unchecked")
            if (absentFromCache > 0) add("ID-uri work canonice absente din cache-ul NPCManager: $absentFromCache; existenta lor nu este verificata in DB")
            if (ambiguousIdentity > 0) add("ID-uri work canonice cu identitate ambigua in cache: $ambiguousIdentity")
            if (otherWorld > 0) add("ID-uri work canonice cu NPC gestionat in alta lume: $otherWorld")
            if (differentProfession > 0) add("ID-uri work canonice cu ocupatie diferita de meseria locului: $differentProfession")
            if (unknownOccupation > 0) add("ID-uri work canonice cu ocupatie NPC necunoscuta: $unknownOccupation")
        }
    }

    private fun canonicalWorkerId(workerId: String): String? {
        val numericId = workerId.removePrefix("npc_").toIntOrNull()
        if (numericId != null && numericId > 0 &&
            (workerId == numericId.toString() || workerId == "npc_$numericId")) {
            return "npc_$numericId"
        }
        if (workerId.length != 36) return null
        val uuid = runCatching { UUID.fromString(workerId) }.getOrNull() ?: return null
        return uuid.toString().takeIf { it.equals(workerId, ignoreCase = true) }
    }
}
