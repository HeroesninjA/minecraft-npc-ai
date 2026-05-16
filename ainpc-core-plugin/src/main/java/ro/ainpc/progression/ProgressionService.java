package ro.ainpc.progression;

import org.bukkit.entity.Player;
import ro.ainpc.AINPCPlugin;
import ro.ainpc.engine.FeaturePackLoader;
import ro.ainpc.engine.ScenarioEngine;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public class ProgressionService {

    private final AINPCPlugin plugin;
    private final ProgressionRepository repository;

    public ProgressionService(AINPCPlugin plugin) {
        this.plugin = plugin;
        this.repository = new ProgressionRepository(
            sql -> {
                if (plugin.getDatabaseManager() == null) {
                    throw new SQLException("DatabaseManager indisponibil");
                }
                return plugin.getDatabaseManager().prepareStatement(sql);
            },
            this::getDefinitions
        );
    }

    public ScenarioEngine.QuestInteractionResult getLog(Player player, String filter, boolean adminView) {
        return scenarioEngine().getQuestLog(player, filter, adminView);
    }

    public ScenarioEngine.QuestGuiSnapshot getGuiSnapshot(Player player, String filter, boolean adminView) {
        return scenarioEngine().getQuestGuiSnapshot(player, filter, adminView);
    }

    public ProgressionGuiSnapshot getProgressionGuiSnapshot(Player player, String filter, boolean adminView) {
        return ProgressionGuiSnapshot.fromQuestGuiSnapshot(
            getGuiSnapshot(player, filter, adminView),
            this::findDefinitionForEntry
        );
    }

    public List<ProgressionDefinition> getDefinitions() {
        FeaturePackLoader featurePackLoader = plugin.getFeaturePackLoader();
        if (featurePackLoader == null) {
            return List.of();
        }

        return featurePackLoader.getAllScenarios().stream()
            .filter(ProgressionDefinition::isProgressionCandidate)
            .map(ProgressionDefinition::fromScenarioDefinition)
            .sorted(Comparator
                .comparing(ProgressionDefinition::packId, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(ProgressionDefinition::mechanicId, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(ProgressionDefinition::definitionId, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    public List<ProgressionDefinition> getDefinitions(String filter) {
        if (ProgressionFilter.isAllFilter(filter)) {
            return getDefinitions();
        }

        return getDefinitions().stream()
            .filter(definition -> ProgressionFilter.matchesDefinition(definition, filter))
            .toList();
    }

    public List<String> getObjectiveIdSuggestions(Player player, String selector) {
        FeaturePackLoader.ScenarioDefinition scenario = null;
        ProgressionSelector progressionSelector = parseSelector(selector);
        ScenarioEngine.QuestGuiEntry entry = findEntry(player, progressionSelector);
        ProgressionDefinition entryDefinition = findDefinitionForEntry(entry);
        if (entryDefinition != null) {
            scenario = findScenarioForDefinition(entryDefinition);
        }
        if (scenario == null) {
            scenario = findScenarioForSelector(selector);
        }
        if (scenario == null) {
            return List.of();
        }

        LinkedHashSet<String> suggestions = new LinkedHashSet<>();
        for (FeaturePackLoader.QuestEntryDefinition objective : scenario.getObjectives()) {
            addCandidate(suggestions, displayObjectiveKey(objective));
        }
        return List.copyOf(suggestions);
    }

    public List<StoredProgression> getStoredProgressions() throws SQLException {
        return repository.findAll();
    }

    public List<StoredProgression> getStoredProgressions(String playerUuid, String filter, int limit) throws SQLException {
        return repository.find(playerUuid, filter, limit);
    }

    public StoredProgressionSummary getStoredProgressionSummary(String playerUuid, String filter) throws SQLException {
        return repository.summarize(playerUuid, filter);
    }

    public List<ProgressionAnchorBinding> getAnchorBindings(String playerUuid, String templateId, int limit)
        throws SQLException {
        return repository.findAnchorBindings(playerUuid, templateId, limit);
    }

    public List<ProgressionAnchorBinding> getAnchorBindingsForProgression(String playerUuid,
                                                                          String templateId,
                                                                          String questCode,
                                                                          int limit) throws SQLException {
        return repository.findAnchorBindingsForProgression(playerUuid, templateId, questCode, limit);
    }

    public List<ProgressionAnchorBinding> getAnchorBindingsForAnchor(String playerUuid,
                                                                     String anchorType,
                                                                     String anchorId,
                                                                     int limit) throws SQLException {
        return repository.findAnchorBindingsForAnchor(playerUuid, anchorType, anchorId, limit);
    }

    public void saveAnchorBinding(ProgressionAnchorBinding binding) throws SQLException {
        repository.saveAnchorBinding(binding);
    }

    public ScenarioEngine.QuestInteractionResult getStatus(Player player, String selector) {
        return getStatusSnapshot(player, selector).toQuestInteractionResult();
    }

    public ScenarioEngine.QuestInteractionResult getDebug(Player player, String selector) {
        return scenarioEngine().getQuestDebug(player, commandSelector(selector));
    }

    public ScenarioEngine.QuestInteractionResult getProgress(Player player, String selector) {
        return getProgressSnapshot(player, selector).toQuestInteractionResult();
    }

    public ProgressionStatusSnapshot getStatusSnapshot(Player player, String selector) {
        ProgressionSelector progressionSelector = parseSelector(selector);
        ScenarioEngine.QuestInteractionResult result = player == null
            ? ScenarioEngine.QuestInteractionResult.notHandled()
            : scenarioEngine().getQuestStatus(player, progressionSelector.commandSelector());
        ScenarioEngine.QuestGuiEntry entry = findEntry(player, progressionSelector);
        ProgressionDefinition definition = findDefinitionForEntry(entry);
        return ProgressionStatusSnapshot.fromResult(
            player != null ? player.getName() : "",
            progressionSelector,
            result,
            entry,
            definition
        );
    }

    public ProgressionProgressSnapshot getProgressSnapshot(Player player, String selector) {
        ProgressionSelector progressionSelector = parseSelector(selector);
        ScenarioEngine.QuestInteractionResult result = player == null
            ? ScenarioEngine.QuestInteractionResult.notHandled()
            : scenarioEngine().getQuestProgress(player, progressionSelector.commandSelector());
        ScenarioEngine.QuestGuiEntry entry = findEntry(player, progressionSelector);
        ProgressionDefinition definition = findDefinitionForEntry(entry);
        return ProgressionProgressSnapshot.fromResult(
            player != null ? player.getName() : "",
            progressionSelector,
            result,
            entry,
            definition
        );
    }

    public ScenarioEngine.QuestInteractionResult getTrack(Player player, String selector) {
        return scenarioEngine().getQuestTrack(player, commandSelector(selector));
    }

    public ScenarioEngine.QuestTrackingMarker startTracking(Player player, String selector) {
        return scenarioEngine().startQuestTracking(player, commandSelector(selector));
    }

    public ScenarioEngine.QuestTrackingMarker getTrackingMarker(Player player, String selector) {
        return scenarioEngine().getQuestTrackingMarker(player, commandSelector(selector));
    }

    public boolean stopTracking(Player player) {
        return scenarioEngine().stopQuestTracking(player);
    }

    public boolean applyTrackingMarker(Player player, ScenarioEngine.QuestTrackingMarker trackingMarker) {
        return scenarioEngine().applyQuestTrackingMarker(player, trackingMarker);
    }

    public ScenarioEngine.QuestInteractionResult abandon(Player player, String selector) {
        return scenarioEngine().abandonQuest(player, commandSelector(selector));
    }

    public String contractSelector(String selector) {
        return ProgressionSelector.forContractAlias(selector).commandSelector();
    }

    public String kindSelector(String selector, String progressionKind) {
        return ProgressionSelector.forKindAlias(selector, progressionKind).commandSelector();
    }

    public boolean isTrackedSelector(String selector) {
        return ProgressionSelector.isTrackedAlias(selector);
    }

    public ProgressionSelector parseSelector(String selector) {
        return ProgressionSelector.parse(selector);
    }

    private String commandSelector(String selector) {
        return parseSelector(selector).commandSelector();
    }

    private ScenarioEngine scenarioEngine() {
        return plugin.getScenarioEngine();
    }

    private ScenarioEngine.QuestGuiEntry findEntry(Player player, ProgressionSelector selector) {
        if (player == null || scenarioEngine() == null) {
            return null;
        }

        ScenarioEngine.QuestGuiSnapshot snapshot = scenarioEngine().getQuestGuiSnapshot(player, "all", true);
        if (snapshot == null || !snapshot.handled()) {
            return null;
        }

        List<ScenarioEngine.QuestGuiEntry> entries = snapshot.allEntries();
        if (selector == null || selector.isEmpty()) {
            return entries.stream()
                .filter(ScenarioEngine.QuestGuiEntry::tracked)
                .findFirst()
                .or(() -> entries.stream().filter(ScenarioEngine.QuestGuiEntry::current).findFirst())
                .or(() -> entries.stream().filter(ScenarioEngine.QuestGuiEntry::active).findFirst())
                .orElse(null);
        }

        if (selector.isTrackedAlias()) {
            String raw = selector.raw().toLowerCase(Locale.ROOT);
            if ("current".equals(raw) || "curent".equals(raw)) {
                return entries.stream()
                    .filter(ScenarioEngine.QuestGuiEntry::current)
                    .findFirst()
                    .orElse(null);
            }
            return entries.stream()
                .filter(ScenarioEngine.QuestGuiEntry::tracked)
                .findFirst()
                .or(() -> entries.stream().filter(ScenarioEngine.QuestGuiEntry::current).findFirst())
                .orElse(null);
        }

        return entries.stream()
            .filter(entry -> entryMatchesSelector(entry, selector))
            .findFirst()
            .orElse(null);
    }

    private boolean entryMatchesSelector(ScenarioEngine.QuestGuiEntry entry, ProgressionSelector selector) {
        if (entry == null || selector == null || selector.commandSelector().isBlank()) {
            return false;
        }

        String normalized = selector.commandSelector().toLowerCase(Locale.ROOT);
        return entrySelectorCandidates(entry).stream()
            .map(candidate -> candidate.toLowerCase(Locale.ROOT))
            .anyMatch(normalized::equals);
    }

    private Set<String> entrySelectorCandidates(ScenarioEngine.QuestGuiEntry entry) {
        Set<String> candidates = new LinkedHashSet<>();
        addCandidate(candidates, entry.selector());
        addCandidate(candidates, entry.templateId());
        addCandidate(candidates, entry.questCode());

        ProgressionDefinition definition = findDefinitionForEntry(entry);
        if (definition != null) {
            addCandidate(candidates, definition.progressionId());
            addCandidate(candidates, definition.definitionId());
            addCandidate(candidates, definition.templateId());
            addCandidate(candidates, definition.code());
            addCandidate(candidates, definition.kind() + ":" + definition.definitionId());
            addCandidate(candidates, definition.kind() + ":" + definition.code());
            addCandidate(candidates, definition.mechanicId() + ":" + definition.definitionId());
            addCandidate(candidates, definition.mechanicId() + ":" + definition.code());
            addCandidate(candidates, definition.packId() + ":" + definition.mechanicId() + ":" + definition.definitionId());
            addCandidate(candidates, definition.packId() + ":" + definition.mechanicId() + ":" + definition.code());
        }

        return candidates;
    }

    private ProgressionDefinition findDefinitionForEntry(ScenarioEngine.QuestGuiEntry entry) {
        if (entry == null) {
            return null;
        }

        Optional<ProgressionDefinition> byTemplate = getDefinitions().stream()
            .filter(definition -> equalsIgnoreCase(definition.templateId(), entry.templateId()))
            .findFirst();
        if (byTemplate.isPresent()) {
            return byTemplate.get();
        }

        return getDefinitions().stream()
            .filter(definition -> !definition.code().isBlank())
            .filter(definition -> equalsIgnoreCase(definition.code(), entry.questCode()))
            .findFirst()
            .orElse(null);
    }

    private FeaturePackLoader.ScenarioDefinition findScenarioForDefinition(ProgressionDefinition definition) {
        if (definition == null || plugin.getFeaturePackLoader() == null) {
            return null;
        }

        return plugin.getFeaturePackLoader().getAllScenarios().stream()
            .filter(ProgressionDefinition::isProgressionCandidate)
            .filter(scenario -> definitionMatchesScenario(definition, scenario))
            .findFirst()
            .orElse(null);
    }

    private FeaturePackLoader.ScenarioDefinition findScenarioForSelector(String selector) {
        String normalized = valueOrEmpty(selector).toLowerCase(Locale.ROOT);
        if (normalized.isBlank() || plugin.getFeaturePackLoader() == null) {
            return null;
        }

        return plugin.getFeaturePackLoader().getAllScenarios().stream()
            .filter(ProgressionDefinition::isProgressionCandidate)
            .filter(scenario -> definitionSelectorCandidates(ProgressionDefinition.fromScenarioDefinition(scenario))
                .stream()
                .map(candidate -> candidate.toLowerCase(Locale.ROOT))
                .anyMatch(normalized::equals))
            .findFirst()
            .orElse(null);
    }

    private boolean definitionMatchesScenario(ProgressionDefinition definition,
                                              FeaturePackLoader.ScenarioDefinition scenario) {
        if (definition == null || scenario == null) {
            return false;
        }
        ProgressionDefinition scenarioDefinition = ProgressionDefinition.fromScenarioDefinition(scenario);
        return equalsIgnoreCase(definition.progressionId(), scenarioDefinition.progressionId())
            || equalsIgnoreCase(definition.templateId(), scenarioDefinition.templateId())
            || equalsIgnoreCase(definition.code(), scenarioDefinition.code())
            || (equalsIgnoreCase(definition.packId(), scenarioDefinition.packId())
                && equalsIgnoreCase(definition.definitionId(), scenarioDefinition.definitionId()));
    }

    private Set<String> definitionSelectorCandidates(ProgressionDefinition definition) {
        Set<String> candidates = new LinkedHashSet<>();
        if (definition == null) {
            return candidates;
        }
        addCandidate(candidates, definition.progressionId());
        addCandidate(candidates, definition.definitionId());
        addCandidate(candidates, definition.templateId());
        addCandidate(candidates, definition.code());
        addCandidate(candidates, definition.kind() + ":" + definition.definitionId());
        addCandidate(candidates, definition.kind() + ":" + definition.code());
        addCandidate(candidates, definition.mechanicId() + ":" + definition.definitionId());
        addCandidate(candidates, definition.mechanicId() + ":" + definition.code());
        addCandidate(candidates, definition.packId() + ":" + definition.definitionId());
        addCandidate(candidates, definition.packId() + ":" + definition.mechanicId() + ":" + definition.definitionId());
        addCandidate(candidates, definition.packId() + ":" + definition.mechanicId() + ":" + definition.code());
        return candidates;
    }

    private String displayObjectiveKey(FeaturePackLoader.QuestEntryDefinition objective) {
        if (objective == null) {
            return "";
        }
        String entryId = valueOrEmpty(objective.getEntryId());
        if (!entryId.isBlank()) {
            return entryId;
        }
        String type = valueOrEmpty(objective.getType());
        String itemId = valueOrEmpty(objective.getItemId());
        if (type.isBlank() && itemId.isBlank()) {
            return "";
        }
        return type + ":" + itemId;
    }

    private void addCandidate(Set<String> candidates, String candidate) {
        if (candidate != null && !candidate.isBlank()) {
            candidates.add(candidate);
        }
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

}
