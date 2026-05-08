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
        String normalizedFilter = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
        if (normalizedFilter.isBlank() || "all".equals(normalizedFilter) || "toate".equals(normalizedFilter)) {
            return getDefinitions();
        }

        return getDefinitions().stream()
            .filter(definition -> definitionMatchesFilter(definition, normalizedFilter))
            .toList();
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
        if (player == null) {
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

    private void addCandidate(Set<String> candidates, String candidate) {
        if (candidate != null && !candidate.isBlank()) {
            candidates.add(candidate);
        }
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    private boolean definitionMatchesFilter(ProgressionDefinition definition, String filter) {
        if (definition == null || filter == null || filter.isBlank()) {
            return false;
        }

        return contains(definition.progressionId(), filter)
            || contains(definition.packId(), filter)
            || contains(definition.mechanicId(), filter)
            || contains(definition.kind(), filter)
            || contains(definition.definitionId(), filter)
            || contains(definition.templateId(), filter)
            || contains(definition.code(), filter)
            || contains(definition.displayName(), filter);
    }

    private boolean contains(String value, String filter) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(filter);
    }
}
