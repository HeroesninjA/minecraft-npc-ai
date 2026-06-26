@file:Suppress("SENSELESS_COMPARISON")
package ro.ainpc.progression

import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import ro.ainpc.engine.*
import java.sql.SQLException
import java.util.Comparator
import java.util.LinkedHashSet
import java.util.Locale

class ProgressionService(private val plugin: AINPCPlugin) {
    private val repository = ProgressionRepository(
        ProgressionRepository.StatementProvider { sql ->
            val databaseManager = plugin.databaseManager ?: throw SQLException("DatabaseManager indisponibil")
            databaseManager.prepareStatement(sql)
        },
        this::getDefinitions
    )

    private var cachedDefinitions: List<ProgressionDefinition>? = null
    private var cachedDefinitionsTime: Long = 0L
    private val cacheTtlMs: Long = 30_000L

    fun invalidateDefinitionCache() {
        cachedDefinitions = null
        cachedDefinitionsTime = 0L
    }

    private fun loadDefinitions(): List<ProgressionDefinition> {
        val now = System.currentTimeMillis()
        if (cachedDefinitions != null && now - cachedDefinitionsTime < cacheTtlMs) {
            return cachedDefinitions!!
        }
        val featurePackLoader = plugin.featurePackLoader ?: return listOf()
        val definitions = featurePackLoader.getAllScenarios().stream()
            .filter(ProgressionDefinition::isProgressionCandidate)
            .map(ProgressionDefinition::fromScenarioDefinition)
            .sorted(
                Comparator
                    .comparing(ProgressionDefinition::packId, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(ProgressionDefinition::mechanicId, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(ProgressionDefinition::definitionId, String.CASE_INSENSITIVE_ORDER)
            )
            .toList()
        cachedDefinitions = definitions
        cachedDefinitionsTime = now
        return definitions
    }

    fun getLog(player: Player, filter: String, adminView: Boolean): QuestInteractionResult {
        return scenarioEngine().getQuestLog(player, filter, adminView)
    }

    fun getGuiSnapshot(player: Player, filter: String, adminView: Boolean): QuestGuiSnapshot {
        return scenarioEngine().getQuestGuiSnapshot(player, filter, adminView)
    }

    fun getProgressionGuiSnapshot(player: Player, filter: String, adminView: Boolean): ProgressionGuiSnapshot {
        return ProgressionGuiSnapshot.fromQuestGuiSnapshot(
            getGuiSnapshot(player, filter, adminView),
            this::findDefinitionForEntry
        )
    }

    fun findProgressionGuiEntry(player: Player, filter: String, adminView: Boolean, selector: String?): ProgressionGuiEntry? {
        return getProgressionGuiSnapshot(player, filter, adminView).findEntry(selector)
    }

    fun getDefinitions(): List<ProgressionDefinition> {
        return loadDefinitions()
    }

    fun getDefinitions(filter: String): List<ProgressionDefinition> {
        if (ProgressionFilter.isAllFilter(filter)) {
            return getDefinitions()
        }

        return getDefinitions().stream()
            .filter { definition -> ProgressionFilter.matchesDefinition(definition, filter) }
            .toList()
    }

    fun getObjectiveIdSuggestions(player: Player, selector: String): List<String> {
        var scenario: FeaturePackLoader.ScenarioDefinition? = null
        val progressionSelector = parseSelector(selector)
        val entry = findEntry(player, progressionSelector)
        val entryDefinition = findDefinitionForEntry(entry)
        if (entryDefinition != null) {
            scenario = findScenarioForDefinition(entryDefinition)
        }
        if (scenario == null) {
            scenario = findScenarioForSelector(selector)
        }
        if (scenario == null) {
            return listOf()
        }

        val suggestions = LinkedHashSet<String>()
        for (objective in scenario.objectives) {
            addCandidate(suggestions, displayObjectiveKey(objective))
        }
        return suggestions.toList()
    }

    @Throws(SQLException::class)
    fun getStoredProgressions(): List<StoredProgression> = repository.findAll()

    @Throws(SQLException::class)
    fun getStoredProgressions(playerUuid: String?, filter: String?, limit: Int): List<StoredProgression> {
        return repository.find(playerUuid, filter, limit)
    }

    @Throws(SQLException::class)
    fun getStoredProgressionSummary(playerUuid: String?, filter: String?): StoredProgressionSummary {
        return repository.summarize(playerUuid, filter)
    }

    fun getStoredProgressionObjectives(player: Player?, selector: String?): List<QuestGuiObjective> {
        if (player == null || selector.isNullOrBlank()) return listOf()
        val progressionSelector = parseSelector(selector)
        val entry = findEntry(player, progressionSelector)
        return entry?.objectives?.toList() ?: emptyList()
    }

    @Throws(SQLException::class)
    fun getAnchorBindings(playerUuid: String?, templateId: String?, limit: Int): List<ProgressionAnchorBinding> {
        return repository.findAnchorBindings(playerUuid, templateId, limit)
    }

    @Throws(SQLException::class)
    fun getAnchorBindingsForProgression(
        playerUuid: String?,
        templateId: String?,
        questCode: String?,
        limit: Int
    ): List<ProgressionAnchorBinding> {
        return repository.findAnchorBindingsForProgression(playerUuid, templateId, questCode, limit)
    }

    @Throws(SQLException::class)
    fun getAnchorBindingsForAnchor(
        playerUuid: String?,
        anchorType: String?,
        anchorId: String?,
        limit: Int
    ): List<ProgressionAnchorBinding> {
        return repository.findAnchorBindingsForAnchor(playerUuid, anchorType, anchorId, limit)
    }

    fun findDuplicateDefinitions(): Map<String, List<ProgressionDefinition>> {
        val definitions = getDefinitions()
        val grouped = LinkedHashMap<String, MutableList<ProgressionDefinition>>()
        for (definition in definitions) {
            val key = "${definition.mechanicId()}:${definition.definitionId()}".lowercase(Locale.ROOT)
            grouped.getOrPut(key) { mutableListOf() }.add(definition)
        }
        return grouped.filter { it.value.size > 1 }
    }

    fun findDuplicateCodes(): Map<String, List<ProgressionDefinition>> {
        val definitions = getDefinitions()
        val grouped = LinkedHashMap<String, MutableList<ProgressionDefinition>>()
        for (definition in definitions) {
            val code = definition.code().lowercase(Locale.ROOT)
            if (code.isNotBlank()) {
                grouped.getOrPut(code) { mutableListOf() }.add(definition)
            }
        }
        return grouped.filter { it.value.size > 1 }
    }

    @Throws(SQLException::class)
    fun findUnresolvedProgressions(playerUuid: String?, limit: Int): List<StoredProgression> {
        val all = repository.find(playerUuid, "", 0)
        val unresolved = all.filter { !it.definitionResolved() }
        return if (limit > 0) unresolved.take(limit) else unresolved
    }

    fun buildUnresolvedReport(playerUuid: String?): String {
        val unresolved = runCatching { findUnresolvedProgressions(playerUuid, 50) }.getOrNull()
            ?: return "Nu am putut citi progresiile persistate."
        if (unresolved.isEmpty()) {
            return "Toate progresiile au definitie incarcata."
        }
        val sb = StringBuilder()
        sb.appendLine("Progresii fara definitie incarcata: ${unresolved.size}")
        for (progression in unresolved) {
            val playerLabel = ProgressionFormatUtil.compactUuid(progression.playerUuid())
            sb.appendLine("  $playerLabel | ${progression.templateId()} | ${progression.code()} | ${progression.status()} | ${ProgressionFormatUtil.formatStoryTime(progression.updatedAt())}")
        }
        return sb.toString()
    }

    @Throws(SQLException::class)
    fun getAnchorBindingsForObjective(
        playerUuid: String?,
        templateId: String?,
        objectiveKey: String?,
        limit: Int
    ): List<ProgressionAnchorBinding> {
        return repository.findAnchorBindingsForObjective(playerUuid, templateId, objectiveKey, limit)
    }

    @Throws(SQLException::class)
    fun getProgressionsByAnchor(anchorType: String?, anchorId: String?, limit: Int): List<StoredProgression> {
        return repository.findProgressionsByAnchor(anchorType, anchorId, limit)
    }

    @Throws(SQLException::class)
    fun getProgressionSummaryForAnchor(
        anchorType: String?,
        anchorId: String?
    ): StoredProgressionSummary {
        val progressions = repository.findProgressionsByAnchor(anchorType, anchorId, 0)
        return StoredProgressionSummary.from(progressions)
    }

    @Throws(SQLException::class)
    fun getProgressionsGroupedByAnchor(playerUuid: String?, limitPerAnchor: Int): Map<String, List<StoredProgression>> {
        return repository.findProgressionsByAnchorGrouped(playerUuid, limitPerAnchor)
    }

    @Throws(SQLException::class)
    fun getProgressionsByAnchorQuery(
        query: String?,
        anchorTypes: List<String>?,
        limit: Int
    ): List<StoredProgression> {
        return repository.searchProgressionsByAnchor(query, anchorTypes, limit)
    }

    @Throws(SQLException::class)
    fun deleteAnchorBinding(playerUuid: String?, templateId: String?, objectiveKey: String?) {
        repository.deleteAnchorBinding(playerUuid, templateId, objectiveKey)
    }

    @Throws(SQLException::class)
    fun saveAnchorBinding(binding: ProgressionAnchorBinding?) {
        if (binding != null) {
            val existing = repository.findAnchorBindingsForAnchor(
                binding.playerUuid(), binding.anchorType(), binding.anchorId(), 10
            )
            val duplicate = existing.firstOrNull { other ->
                other.templateId() != binding.templateId() &&
                    other.anchorType().equals(binding.anchorType(), ignoreCase = true) &&
                    other.anchorId().equals(binding.anchorId(), ignoreCase = true)
            }
            if (duplicate != null) {
                plugin.logger.warning(
                    "Ancora duplicata detectata: ${binding.anchorType()}:${binding.anchorId()} " +
                        "este deja folosita de template-ul ${duplicate.templateId()}."
                )
            }
        }
        repository.saveAnchorBinding(binding)
    }

    fun getStatus(player: Player, selector: String): QuestInteractionResult {
        return getStatusSnapshot(player, selector).toQuestInteractionResult()
    }

    fun getDebug(player: Player, selector: String): QuestInteractionResult {
        return scenarioEngine().getQuestDebug(player, commandSelector(selector))
    }

    fun getProgress(player: Player, selector: String): QuestInteractionResult {
        return getProgressSnapshot(player, selector).toQuestInteractionResult()
    }

    fun getStatusSnapshot(player: Player?, selector: String): ProgressionStatusSnapshot {
        val progressionSelector = parseSelector(selector)
        val result = if (player == null) {
            QuestInteractionResult.notHandled()
        } else {
            scenarioEngine().getQuestStatus(player, progressionSelector.commandSelector())
        }
        val entryContext = resolveEntryContext(player, progressionSelector)
        return ProgressionStatusSnapshot.fromResult(
            player?.name ?: "",
            progressionSelector,
            result,
            entryContext.first,
            entryContext.second
        )
    }

    fun getProgressSnapshot(player: Player?, selector: String): ProgressionProgressSnapshot {
        val progressionSelector = parseSelector(selector)
        val result = if (player == null) {
            QuestInteractionResult.notHandled()
        } else {
            scenarioEngine().getQuestProgress(player, progressionSelector.commandSelector())
        }
        val entryContext = resolveEntryContext(player, progressionSelector)
        return ProgressionProgressSnapshot.fromResult(
            player?.name ?: "",
            progressionSelector,
            result,
            entryContext.first,
            entryContext.second
        )
    }

    fun getTrack(player: Player, selector: String): QuestInteractionResult {
        return scenarioEngine().getQuestTrack(player, commandSelector(selector))
    }

    fun startTracking(player: Player, selector: String): QuestTrackingMarker? {
        return scenarioEngine().startQuestTracking(player, commandSelector(selector))
    }

    fun getTrackingMarker(player: Player, selector: String): QuestTrackingMarker? {
        return scenarioEngine().getQuestTrackingMarker(player, commandSelector(selector))
    }

    fun stopTracking(player: Player): Boolean = scenarioEngine().stopQuestTracking(player)

    fun applyTrackingMarker(player: Player, trackingMarker: QuestTrackingMarker): Boolean {
        return scenarioEngine().applyQuestTrackingMarker(player, trackingMarker)
    }

    fun abandon(player: Player, selector: String): QuestInteractionResult {
        return scenarioEngine().abandonQuest(player, commandSelector(selector))
    }

    fun contractSelector(selector: String): String {
        return ProgressionSelector.forContractAlias(selector).commandSelector()
    }

    fun kindSelector(selector: String, progressionKind: String): String {
        return ProgressionSelector.forKindAlias(selector, progressionKind).commandSelector()
    }

    fun isTrackedSelector(selector: String): Boolean = ProgressionSelector.isTrackedAlias(selector)

    fun parseSelector(selector: String): ProgressionSelector = ProgressionSelector.parse(selector)

    fun findEntry(player: Player, selector: String): ProgressionGuiEntry? {
        val entry = findEntry(player, parseSelector(selector))
        return entry?.let { ProgressionGuiEntry.fromQuestGuiEntry(it, findDefinitionForEntry(it)) }
    }

    fun findEntry(player: Player, selector: String, adminView: Boolean): ProgressionGuiEntry? {
        val progressionSelector = parseSelector(selector)
        val snapshot = scenarioEngine().getQuestGuiSnapshot(player, "all", adminView)
        if (snapshot == null || !snapshot.handled) return null
        val rawEntry = resolveEntry(snapshot.allEntries(), progressionSelector)
        return rawEntry?.let { ProgressionGuiEntry.fromQuestGuiEntry(it, findDefinitionForEntry(it)) }
    }

    @Throws(SQLException::class)
    fun findStoredEntry(playerUuid: String?, selector: String?): StoredProgression? {
        val progressionSelector = parseSelector(selector.orEmpty())
        if (progressionSelector.isEmpty()) return null

        val stored = repository.find(playerUuid, "", 0)
        val normalized = progressionSelector.commandSelector().lowercase(Locale.ROOT)

        return stored.firstOrNull { progression ->
            storedMatchesSelector(progression, normalized)
        }
    }

    private fun storedMatchesSelector(progression: StoredProgression, normalized: String): Boolean {
        for (candidate in storedSelectorCandidates(progression)) {
            if (candidate.lowercase(Locale.ROOT) == normalized) return true
        }
        return false
    }

    private fun storedSelectorCandidates(progression: StoredProgression): Set<String> {
        val candidates = LinkedHashSet<String>()
        addCandidate(candidates, progression.progressionId())
        addCandidate(candidates, progression.templateId())
        addCandidate(candidates, progression.code())
        addCandidate(candidates, progression.definitionId())
        addCandidate(candidates, progression.kind() + ":" + progression.definitionId())
        addCandidate(candidates, progression.kind() + ":" + progression.code())
        addCandidate(candidates, progression.mechanicId() + ":" + progression.definitionId())
        addCandidate(candidates, progression.mechanicId() + ":" + progression.code())
        addCandidate(candidates, progression.packId() + ":" + progression.mechanicId() + ":" + progression.definitionId())
        addCandidate(candidates, progression.packId() + ":" + progression.mechanicId() + ":" + progression.code())
        return candidates
    }

    private fun commandSelector(selector: String): String = parseSelector(selector).commandSelector()

    private fun scenarioEngine(): ScenarioEngine = plugin.scenarioEngine

    private fun resolveEntryContext(
        player: Player?,
        selector: ProgressionSelector?
    ): Pair<QuestGuiEntry?, ProgressionDefinition?> {
        val entry = findEntry(player, selector)
        return entry to findDefinitionForEntry(entry)
    }

    private fun findEntry(player: Player?, selector: ProgressionSelector?): QuestGuiEntry? {
        if (player == null) return null

        val snapshot = scenarioEngine().getQuestGuiSnapshot(player, "all", true)
        if (snapshot == null || !snapshot.handled) return null

        return resolveEntry(snapshot.allEntries(), selector)
    }

    private fun resolveEntry(entries: List<QuestGuiEntry>, selector: ProgressionSelector?): QuestGuiEntry? {
        if (selector == null || selector.isEmpty()) {
            return entries.stream()
                .filter(QuestGuiEntry::tracked)
                .findFirst()
                .or { entries.stream().filter(QuestGuiEntry::current).findFirst() }
                .or { entries.stream().filter(QuestGuiEntry::active).findFirst() }
                .orElse(null)
        }

        if (selector.isActiveAlias()) {
            return entries.stream()
                .filter(QuestGuiEntry::active)
                .findFirst()
                .or { entries.stream().filter(QuestGuiEntry::current).findFirst() }
                .orElse(null)
        }

        if (selector.isCompletedAlias()) {
            return entries.stream()
                .filter(QuestGuiEntry::archived)
                .findFirst()
                .orElse(null)
        }

        if (selector.isTrackedAlias()) {
            val raw = selector.raw().lowercase(Locale.ROOT)
            if (raw == "current" || raw == "curent") {
                return entries.stream()
                    .filter(QuestGuiEntry::current)
                    .findFirst()
                    .orElse(null)
            }
            return entries.stream()
                .filter(QuestGuiEntry::tracked)
                .findFirst()
                .or { entries.stream().filter(QuestGuiEntry::current).findFirst() }
                .orElse(null)
        }

        return entries.stream()
            .filter { entry -> entryMatchesSelector(entry, selector) }
            .findFirst()
            .orElse(null)
    }

    private fun entryMatchesSelector(entry: QuestGuiEntry?, selector: ProgressionSelector?): Boolean {
        if (entry == null || selector == null || selector.commandSelector().isBlank()) {
            return false
        }

        val normalized = selector.commandSelector().lowercase(Locale.ROOT)
        return entrySelectorCandidates(entry).stream()
            .map { candidate -> candidate.lowercase(Locale.ROOT) }
            .anyMatch { candidate -> normalized == candidate }
    }

    private fun entrySelectorCandidates(entry: QuestGuiEntry): Set<String> {
        val candidates = LinkedHashSet<String>()
        addCandidate(candidates, entry.selector)
        addCandidate(candidates, entry.templateId)
        addCandidate(candidates, entry.questCode)

        val definition = findDefinitionForEntry(entry)
        if (definition != null) {
            addCandidate(candidates, definition.progressionId())
            addCandidate(candidates, definition.definitionId())
            addCandidate(candidates, definition.templateId())
            addCandidate(candidates, definition.code())
            addCandidate(candidates, definition.kind() + ":" + definition.definitionId())
            addCandidate(candidates, definition.kind() + ":" + definition.code())
            addCandidate(candidates, definition.mechanicId() + ":" + definition.definitionId())
            addCandidate(candidates, definition.mechanicId() + ":" + definition.code())
            addCandidate(candidates, definition.packId() + ":" + definition.mechanicId() + ":" + definition.definitionId())
            addCandidate(candidates, definition.packId() + ":" + definition.mechanicId() + ":" + definition.code())
        }

        return candidates
    }

    private fun findDefinitionForEntry(entry: QuestGuiEntry?): ProgressionDefinition? {
        if (entry == null) {
            return null
        }

        val byTemplate = getDefinitions().stream()
            .filter { definition -> equalsIgnoreCase(definition.templateId(), entry.templateId) }
            .findFirst()
        if (byTemplate.isPresent) {
            return byTemplate.get()
        }

        return getDefinitions().stream()
            .filter { definition -> definition.code().isNotBlank() }
            .filter { definition -> equalsIgnoreCase(definition.code(), entry.questCode) }
            .findFirst()
            .orElse(null)
    }

    private fun findScenarioForDefinition(definition: ProgressionDefinition?): FeaturePackLoader.ScenarioDefinition? {
        if (definition == null || plugin.featurePackLoader == null) {
            return null
        }

        return plugin.featurePackLoader.getAllScenarios().stream()
            .filter(ProgressionDefinition::isProgressionCandidate)
            .filter { scenario -> definitionMatchesScenario(definition, scenario) }
            .findFirst()
            .orElse(null)
    }

    private fun findScenarioForSelector(selector: String?): FeaturePackLoader.ScenarioDefinition? {
        val normalized = valueOrEmpty(selector).lowercase(Locale.ROOT)
        if (normalized.isBlank() || plugin.featurePackLoader == null) {
            return null
        }

        return plugin.featurePackLoader.getAllScenarios().stream()
            .filter(ProgressionDefinition::isProgressionCandidate)
            .filter { scenario ->
                definitionSelectorCandidates(ProgressionDefinition.fromScenarioDefinition(scenario))
                    .stream()
                    .map { candidate -> candidate.lowercase(Locale.ROOT) }
                    .anyMatch { candidate -> normalized == candidate }
            }
            .findFirst()
            .orElse(null)
    }

    private fun definitionMatchesScenario(
        definition: ProgressionDefinition?,
        scenario: FeaturePackLoader.ScenarioDefinition?
    ): Boolean {
        if (definition == null || scenario == null) {
            return false
        }
        val scenarioDefinition = ProgressionDefinition.fromScenarioDefinition(scenario)
        return equalsIgnoreCase(definition.progressionId(), scenarioDefinition.progressionId())
            || equalsIgnoreCase(definition.templateId(), scenarioDefinition.templateId())
            || equalsIgnoreCase(definition.code(), scenarioDefinition.code())
            || (equalsIgnoreCase(definition.packId(), scenarioDefinition.packId())
                && equalsIgnoreCase(definition.definitionId(), scenarioDefinition.definitionId()))
    }

    private fun definitionSelectorCandidates(definition: ProgressionDefinition?): Set<String> {
        val candidates = LinkedHashSet<String>()
        if (definition == null) {
            return candidates
        }
        addCandidate(candidates, definition.progressionId())
        addCandidate(candidates, definition.definitionId())
        addCandidate(candidates, definition.templateId())
        addCandidate(candidates, definition.code())
        addCandidate(candidates, definition.kind() + ":" + definition.definitionId())
        addCandidate(candidates, definition.kind() + ":" + definition.code())
        addCandidate(candidates, definition.mechanicId() + ":" + definition.definitionId())
        addCandidate(candidates, definition.mechanicId() + ":" + definition.code())
        addCandidate(candidates, definition.packId() + ":" + definition.definitionId())
        addCandidate(candidates, definition.packId() + ":" + definition.mechanicId() + ":" + definition.definitionId())
        addCandidate(candidates, definition.packId() + ":" + definition.mechanicId() + ":" + definition.code())
        return candidates
    }

    private fun displayObjectiveKey(objective: FeaturePackLoader.QuestEntryDefinition?): String {
        if (objective == null) {
            return ""
        }
        val entryId = valueOrEmpty(objective.entryId)
        if (entryId.isNotBlank()) {
            return entryId
        }
        val type = valueOrEmpty(objective.type)
        val itemId = valueOrEmpty(objective.itemId)
        if (type.isBlank() && itemId.isBlank()) {
            return ""
        }
        return "$type:$itemId"
    }

    private fun addCandidate(candidates: MutableSet<String>, candidate: String?) {
        if (!candidate.isNullOrBlank()) {
            candidates.add(candidate)
        }
    }

    private fun equalsIgnoreCase(left: String?, right: String?): Boolean {
        return left != null && right != null && left.equals(right, ignoreCase = true)
    }

    private fun valueOrEmpty(value: String?): String = value?.trim() ?: ""
}
