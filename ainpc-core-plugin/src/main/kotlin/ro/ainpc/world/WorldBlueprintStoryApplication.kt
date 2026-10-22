package ro.ainpc.world

import ro.ainpc.story.RegionStoryState
import ro.ainpc.story.BlueprintStoryApplicationVerification
import ro.ainpc.story.BlueprintStoryApplicationVerificationStatus

internal data class WorldBlueprintStoryApplicationRequest(
    val regionId: String,
    val storyMode: StoryMode,
    val stateKey: String,
    val storyPool: List<String>,
    val variables: Map<String, String>,
    val source: String,
)

internal enum class WorldBlueprintStoryMappingStatus(val label: String) {
    MAPPING_UNAVAILABLE("mapping-ul semantic este indisponibil"),
    REGION_MISSING("regiunea nu mai exista in mapping"),
    WRONG_WORLD("regiunea este mapata in alta lume"),
    AMBIGUOUS("ID-ul regiunii apare de mai multe ori in mapping"),
    STORY_CONFIG_CHANGED("modul, cheia sau pool-ul story difera de starea aplicata"),
    DESCRIPTION_PRESENT("mapping-ul contine acum o descriere story separata"),
    MATCH("configuratia story din mapping coincide cu starea aplicata"),
}

internal object WorldBlueprintStoryProvenance {
    private val fingerprintPattern = Regex("[0-9a-f]{64}")

    fun source(worldName: String, fingerprint: String): String = "world_blueprint:$worldName:$fingerprint"

    fun fingerprint(worldName: String, source: String): String? {
        val prefix = "world_blueprint:$worldName:"
        if (!source.startsWith(prefix)) return null
        return source.removePrefix(prefix).takeIf(fingerprintPattern::matches)
    }
}

internal object WorldBlueprintStoryApplication {
    fun validateUpdate(
        blueprint: WorldBlueprint, regionId: String, expectedApplicationId: Long,
        expectedFingerprint: String, actualFingerprint: String,
        mappedRegions: Collection<WorldRegionInfo>?, verification: BlueprintStoryApplicationVerification,
    ) {
        require(WorldBlueprintPolicy.completionIssues(blueprint).isEmpty()) {
            "Draftul este incomplet sau invalid; ruleaza validate si review."
        }
        require(blueprint.approval?.fingerprint == actualFingerprint && expectedFingerprint == actualFingerprint) {
            "Revizia nu este confirmata sau amprenta difera; ruleaza review si confirm."
        }
        require(regionId in WorldBlueprintCompatibility.storyRegionIds(blueprint)) {
            "Regiunea trebuie declarata exact in story_region_ids."
        }
        val entry = verification.entry
        require(entry != null && entry.id == expectedApplicationId && entry.worldName == blueprint.worldName &&
            entry.regionId == regionId && verification.status == BlueprintStoryApplicationVerificationStatus.MATCH) {
            "Aplicarea asteptata nu este disponibila pentru actualizare; consulta applications verify."
        }
        val state = requireNotNull(verification.state) { "Starea story persistenta lipseste." }
        require(state.regionId() == regionId) { "Starea story nu corespunde regiunii." }
        val mappingStatus = inspectAppliedMapping(blueprint.worldName, regionId, state, mappedRegions)
        require(mappingStatus == WorldBlueprintStoryMappingStatus.MATCH) {
            "Actualizarea este blocata: ${mappingStatus.label}."
        }
        require(entry.blueprintFingerprint != actualFingerprint &&
            state.variables()["story_description_${state.stateKey()}"] != blueprint.story) {
            "Povestea si revizia trebuie sa difere de aplicarea existenta."
        }
    }

    fun inspectAppliedMapping(
        worldName: String,
        regionId: String,
        state: RegionStoryState,
        mappedRegions: Collection<WorldRegionInfo>?,
    ): WorldBlueprintStoryMappingStatus {
        if (mappedRegions == null) return WorldBlueprintStoryMappingStatus.MAPPING_UNAVAILABLE
        val matches = mappedRegions.filter { it.id() == regionId }
        if (matches.isEmpty()) return WorldBlueprintStoryMappingStatus.REGION_MISSING
        if (matches.size != 1) return WorldBlueprintStoryMappingStatus.AMBIGUOUS
        val region = matches.single()
        if (region.worldName() != worldName) return WorldBlueprintStoryMappingStatus.WRONG_WORLD
        if (region.storyMode() != state.storyMode() ||
            region.storyStateKey().ifBlank { "default" } != state.stateKey() ||
            region.storyPool() != state.storyPool()) {
            return WorldBlueprintStoryMappingStatus.STORY_CONFIG_CHANGED
        }
        if (region.storyDescriptions().values.any { description ->
                description.trim().let { it.isNotEmpty() && it != "<none>" }
            }) {
            return WorldBlueprintStoryMappingStatus.DESCRIPTION_PRESENT
        }
        return WorldBlueprintStoryMappingStatus.MATCH
    }

    fun parseRegionSelection(raw: String): List<String> {
        val regionIds = raw.split(',').map { it.trim() }
        require(regionIds.all { it.isNotEmpty() } && regionIds.size <= 64 &&
            regionIds.distinct().size == regionIds.size) {
            "Selecteaza 1..64 ID-uri de regiune distincte, separate prin virgula, fara elemente goale."
        }
        return regionIds
    }

    fun parseRegionApplicationSelection(raw: String): Map<String, Long> {
        val entries = raw.split(',')
        require(entries.size in 1..64) { "Selecteaza intre 1 si 64 de regiuni story." }
        val selected = entries.map { entry ->
            val separator = entry.lastIndexOf(':')
            val regionId = if (separator > 0) entry.substring(0, separator).trim() else ""
            val applicationId = if (separator > 0) entry.substring(separator + 1).trim().toLongOrNull() else null
            require(separator > 0 && regionId.isNotEmpty() && applicationId != null && applicationId > 0) {
                "Foloseste perechi regionId:applicationId cu ID-uri de aplicare pozitive."
            }
            regionId to applicationId
        }
        require(selected.map { it.first }.distinct().size == selected.size) {
            "ID-urile regiunilor story trebuie sa fie distincte."
        }
        return selected.toMap()
    }

    fun parseRegionRestorationSelection(raw: String): Map<String, Pair<Long, Long>> {
        val entries = raw.split(',')
        require(entries.size in 1..64) { "Selecteaza intre 1 si 64 de regiuni story." }
        val selected = entries.map { entry ->
            val lastSeparator = entry.lastIndexOf(':')
            val firstSeparator = if (lastSeparator > 0) entry.lastIndexOf(':', lastSeparator - 1) else -1
            val regionId = if (firstSeparator > 0) entry.substring(0, firstSeparator).trim() else ""
            val currentId = if (firstSeparator > 0) entry.substring(firstSeparator + 1, lastSeparator)
                .trim().toLongOrNull() else null
            val previousId = if (lastSeparator > 0) entry.substring(lastSeparator + 1)
                .trim().toLongOrNull() else null
            require(regionId.isNotEmpty() && currentId != null && previousId != null &&
                currentId > 0 && previousId > 0 && currentId != previousId) {
                "Foloseste perechi regionId:applicationIdCurent:applicationIdAnterior cu ID-uri pozitive, diferite."
            }
            regionId to (currentId to previousId)
        }
        require(selected.map { it.first }.distinct().size == selected.size) {
            "ID-urile regiunilor story trebuie sa fie distincte."
        }
        return selected.toMap()
    }

    fun prepareAll(
        blueprint: WorldBlueprint,
        expectedFingerprint: String,
        actualFingerprint: String,
        mappedRegions: Collection<WorldRegionInfo>?,
        persistentStates: Map<String, RegionStoryState>?,
    ): List<WorldBlueprintStoryApplicationRequest> {
        val regionIds = WorldBlueprintCompatibility.storyRegionIds(blueprint)
        return prepareSelected(blueprint, regionIds, expectedFingerprint, actualFingerprint,
            mappedRegions, persistentStates)
    }

    fun prepareSelected(
        blueprint: WorldBlueprint,
        regionIds: List<String>,
        expectedFingerprint: String,
        actualFingerprint: String,
        mappedRegions: Collection<WorldRegionInfo>?,
        persistentStates: Map<String, RegionStoryState>?,
    ): List<WorldBlueprintStoryApplicationRequest> {
        require(regionIds.isNotEmpty() && regionIds.size <= 64 &&
            regionIds.all { it.isNotBlank() } && regionIds.distinct().size == regionIds.size) {
            "Selecteaza 1..64 ID-uri de regiune distincte."
        }
        return regionIds.map { regionId ->
            prepare(blueprint, regionId, expectedFingerprint, actualFingerprint,
                mappedRegions, persistentStates)
        }
    }

    fun prepare(
        blueprint: WorldBlueprint,
        regionId: String,
        expectedFingerprint: String,
        actualFingerprint: String,
        mappedRegions: Collection<WorldRegionInfo>?,
        persistentStates: Map<String, RegionStoryState>?,
    ): WorldBlueprintStoryApplicationRequest {
        require(WorldBlueprintPolicy.completionIssues(blueprint).isEmpty()) {
            "Draftul este incomplet sau invalid; ruleaza validate si review."
        }
        require(blueprint.approval?.fingerprint == actualFingerprint && expectedFingerprint == actualFingerprint) {
            "Revizia nu este confirmata sau amprenta difera; ruleaza review si confirm."
        }
        require(regionId in WorldBlueprintCompatibility.storyRegionIds(blueprint)) {
            "Regiunea trebuie declarata exact in story_region_ids."
        }
        val readiness = WorldBlueprintStoryReadiness.inspect(blueprint.worldName, listOf(regionId),
            mappedRegions, persistentStates).single()
        require(readiness.status == WorldBlueprintStoryReadinessStatus.NO_OBSERVED_CONFLICT) {
            "$regionId: ${readiness.status.label}; aplicarea nu suprascrie story existent."
        }
        val region = requireNotNull(mappedRegions).single {
            it.id() == regionId && it.worldName() == blueprint.worldName
        }
        val stateKey = region.storyStateKey().ifBlank { "default" }
        return WorldBlueprintStoryApplicationRequest(regionId, region.storyMode(), stateKey,
            region.storyPool(), mapOf("story_description_$stateKey" to blueprint.story),
            WorldBlueprintStoryProvenance.source(blueprint.worldName, actualFingerprint))
    }
}
