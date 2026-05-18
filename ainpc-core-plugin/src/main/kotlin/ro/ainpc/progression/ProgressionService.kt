package ro.ainpc.progression

import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import ro.ainpc.engine.FeaturePackLoader
import ro.ainpc.engine.ScenarioEngine
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

    fun getLog(player: Player, filter: String, adminView: Boolean): ScenarioEngine.QuestInteractionResult {
        return scenarioEngine().getQuestLog(player, filter, adminView)
    }

    fun getGuiSnapshot(player: Player, filter: String, adminView: Boolean): ScenarioEngine.QuestGuiSnapshot {
        return scenarioEngine().getQuestGuiSnapshot(player, filter, adminView)
    }

    fun getProgressionGuiSnapshot(player: Player, filter: String, adminView: Boolean): ProgressionGuiSnapshot {
        return ProgressionGuiSnapshot.fromQuestGuiSnapshot(
            getGuiSnapshot(player, filter, adminView),
            this::findDefinitionForEntry
        )
    }

    fun getDefinitions(): List<ProgressionDefinition> {
        val featurePackLoader = plugin.featurePackLoader ?: return listOf()

        return featurePackLoader.getAllScenarios().stream()
            .filter(ProgressionDefinition::isProgressionCandidate)
            .map(ProgressionDefinition::fromScenarioDefinition)
            .sorted(
                Comparator
                    .comparing(ProgressionDefinition::packId, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(ProgressionDefinition::mechanicId, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(ProgressionDefinition::definitionId, String.CASE_INSENSITIVE_ORDER)
            )
            .toList()
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

    @Throws(SQLException::class)
    fun saveAnchorBinding(binding: ProgressionAnchorBinding?) {
        repository.saveAnchorBinding(binding)
    }

    fun getStatus(player: Player, selector: String): ScenarioEngine.QuestInteractionResult {
        return getStatusSnapshot(player, selector).toQuestInteractionResult()
    }

    fun getDebug(player: Player, selector: String): ScenarioEngine.QuestInteractionResult {
        return scenarioEngine().getQuestDebug(player, commandSelector(selector))
    }

    fun getProgress(player: Player, selector: String): ScenarioEngine.QuestInteractionResult {
        return getProgressSnapshot(player, selector).toQuestInteractionResult()
    }

    fun getStatusSnapshot(player: Player?, selector: String): ProgressionStatusSnapshot {
        val progressionSelector = parseSelector(selector)
        val result = if (player == null) {
            ScenarioEngine.QuestInteractionResult.notHandled()
        } else {
            scenarioEngine().getQuestStatus(player, progressionSelector.commandSelector())
        }
        val entry = findEntry(player, progressionSelector)
        val definition = findDefinitionForEntry(entry)
        return ProgressionStatusSnapshot.fromResult(
            player?.name ?: "",
            progressionSelector,
            result,
            entry,
            definition
        )
    }

    fun getProgressSnapshot(player: Player?, selector: String): ProgressionProgressSnapshot {
        val progressionSelector = parseSelector(selector)
        val result = if (player == null) {
            ScenarioEngine.QuestInteractionResult.notHandled()
        } else {
            scenarioEngine().getQuestProgress(player, progressionSelector.commandSelector())
        }
        val entry = findEntry(player, progressionSelector)
        val definition = findDefinitionForEntry(entry)
        return ProgressionProgressSnapshot.fromResult(
            player?.name ?: "",
            progressionSelector,
            result,
            entry,
            definition
        )
    }

    fun getTrack(player: Player, selector: String): ScenarioEngine.QuestInteractionResult {
        return scenarioEngine().getQuestTrack(player, commandSelector(selector))
    }

    fun startTracking(player: Player, selector: String): ScenarioEngine.QuestTrackingMarker {
        return scenarioEngine().startQuestTracking(player, commandSelector(selector))
    }

    fun getTrackingMarker(player: Player, selector: String): ScenarioEngine.QuestTrackingMarker {
        return scenarioEngine().getQuestTrackingMarker(player, commandSelector(selector))
    }

    fun stopTracking(player: Player): Boolean = scenarioEngine().stopQuestTracking(player)

    fun applyTrackingMarker(player: Player, trackingMarker: ScenarioEngine.QuestTrackingMarker): Boolean {
        return scenarioEngine().applyQuestTrackingMarker(player, trackingMarker)
    }

    fun abandon(player: Player, selector: String): ScenarioEngine.QuestInteractionResult {
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

    private fun commandSelector(selector: String): String = parseSelector(selector).commandSelector()

    private fun scenarioEngine(): ScenarioEngine = plugin.scenarioEngine

    private fun findEntry(player: Player?, selector: ProgressionSelector?): ScenarioEngine.QuestGuiEntry? {
        if (player == null) {
            return null
        }

        val snapshot = scenarioEngine().getQuestGuiSnapshot(player, "all", true)
        if (snapshot == null || !snapshot.handled()) {
            return null
        }

        val entries = snapshot.allEntries()
        if (selector == null || selector.isEmpty()) {
            return entries.stream()
                .filter(ScenarioEngine.QuestGuiEntry::tracked)
                .findFirst()
                .or { entries.stream().filter(ScenarioEngine.QuestGuiEntry::current).findFirst() }
                .or { entries.stream().filter(ScenarioEngine.QuestGuiEntry::active).findFirst() }
                .orElse(null)
        }

        if (selector.isTrackedAlias()) {
            val raw = selector.raw().lowercase(Locale.ROOT)
            if (raw == "current" || raw == "curent") {
                return entries.stream()
                    .filter(ScenarioEngine.QuestGuiEntry::current)
                    .findFirst()
                    .orElse(null)
            }
            return entries.stream()
                .filter(ScenarioEngine.QuestGuiEntry::tracked)
                .findFirst()
                .or { entries.stream().filter(ScenarioEngine.QuestGuiEntry::current).findFirst() }
                .orElse(null)
        }

        return entries.stream()
            .filter { entry -> entryMatchesSelector(entry, selector) }
            .findFirst()
            .orElse(null)
    }

    private fun entryMatchesSelector(entry: ScenarioEngine.QuestGuiEntry?, selector: ProgressionSelector?): Boolean {
        if (entry == null || selector == null || selector.commandSelector().isBlank()) {
            return false
        }

        val normalized = selector.commandSelector().lowercase(Locale.ROOT)
        return entrySelectorCandidates(entry).stream()
            .map { candidate -> candidate.lowercase(Locale.ROOT) }
            .anyMatch { candidate -> normalized == candidate }
    }

    private fun entrySelectorCandidates(entry: ScenarioEngine.QuestGuiEntry): Set<String> {
        val candidates = LinkedHashSet<String>()
        addCandidate(candidates, entry.selector())
        addCandidate(candidates, entry.templateId())
        addCandidate(candidates, entry.questCode())

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

    private fun findDefinitionForEntry(entry: ScenarioEngine.QuestGuiEntry?): ProgressionDefinition? {
        if (entry == null) {
            return null
        }

        val byTemplate = getDefinitions().stream()
            .filter { definition -> equalsIgnoreCase(definition.templateId(), entry.templateId()) }
            .findFirst()
        if (byTemplate.isPresent) {
            return byTemplate.get()
        }

        return getDefinitions().stream()
            .filter { definition -> definition.code().isNotBlank() }
            .filter { definition -> equalsIgnoreCase(definition.code(), entry.questCode()) }
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
