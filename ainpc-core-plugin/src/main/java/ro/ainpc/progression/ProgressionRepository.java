package ro.ainpc.progression;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

public class ProgressionRepository {

    @FunctionalInterface
    public interface StatementProvider {
        PreparedStatement prepareStatement(String sql) throws SQLException;
    }

    private final StatementProvider statementProvider;
    private final Supplier<List<ProgressionDefinition>> definitionsSupplier;

    public ProgressionRepository(StatementProvider statementProvider,
                                 Supplier<List<ProgressionDefinition>> definitionsSupplier) {
        this.statementProvider = statementProvider;
        this.definitionsSupplier = definitionsSupplier;
    }

    public List<StoredProgression> findAll() throws SQLException {
        if (statementProvider == null) {
            return List.of();
        }

        String sql = """
            SELECT player_uuid, template_id, quest_code, status, started_at, completed_at,
                   current_phase, current_stage_id, objective_progress, quest_variables, updated_at, tracked
            FROM player_quests
            ORDER BY player_uuid, status, updated_at DESC, template_id
        """;

        List<ProgressionDefinition> definitions = definitions();
        try (PreparedStatement statement = statementProvider.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            java.util.ArrayList<StoredProgression> rows = new java.util.ArrayList<>();
            while (resultSet.next()) {
                rows.add(toStoredProgression(resultSet, definitions));
            }
            return List.copyOf(rows);
        }
    }

    public List<StoredProgression> find(String playerUuid, String filter, int limit) throws SQLException {
        String safePlayerUuid = valueOrEmpty(playerUuid);
        String normalizedFilter = valueOrEmpty(filter).toLowerCase(Locale.ROOT);
        return findAll().stream()
            .filter(progression -> safePlayerUuid.isBlank()
                || progression.playerUuid().equalsIgnoreCase(safePlayerUuid))
            .filter(progression -> storedProgressionMatchesFilter(progression, normalizedFilter))
            .limit(limit > 0 ? limit : Long.MAX_VALUE)
            .toList();
    }

    public StoredProgressionSummary summarize(String playerUuid, String filter) throws SQLException {
        return StoredProgressionSummary.from(find(playerUuid, filter, 0));
    }

    private StoredProgression toStoredProgression(ResultSet resultSet,
                                                  List<ProgressionDefinition> definitions) throws SQLException {
        String templateId = valueOrEmpty(resultSet.getString("template_id"));
        String code = valueOrEmpty(resultSet.getString("quest_code"));
        ProgressionDefinition definition = findDefinition(templateId, code, definitions);

        return new StoredProgression(
            resultSet.getString("player_uuid"),
            progressionId(definition, templateId, code),
            definition != null ? definition.packId() : "",
            definition != null ? definition.mechanicId() : "",
            definition != null ? definition.kind() : "",
            definition != null ? definition.definitionId() : extractDefinitionId(templateId),
            templateId,
            code,
            resultSet.getString("status"),
            resultSet.getLong("started_at"),
            resultSet.getLong("completed_at"),
            resultSet.getString("current_phase"),
            resultSet.getString("current_stage_id"),
            resultSet.getString("objective_progress"),
            resultSet.getString("quest_variables"),
            resultSet.getLong("updated_at"),
            resultSet.getInt("tracked") != 0,
            definition != null,
            definition != null ? definition.label() : "",
            definition != null ? definition.singularLabel() : "",
            definition != null ? definition.pluralLabel() : "",
            "player_quests"
        );
    }

    private ProgressionDefinition findDefinition(String templateId,
                                                 String code,
                                                 List<ProgressionDefinition> definitions) {
        if (definitions == null || definitions.isEmpty()) {
            return null;
        }

        String safeTemplateId = valueOrEmpty(templateId);
        String safeCode = valueOrEmpty(code);
        String definitionId = extractDefinitionId(safeTemplateId);
        String packQualifiedReference = packQualifiedReference(safeTemplateId);

        return definitions.stream()
            .filter(Objects::nonNull)
            .filter(definition -> matchesDefinition(definition, safeTemplateId, safeCode, definitionId, packQualifiedReference))
            .findFirst()
            .orElse(null);
    }

    private boolean matchesDefinition(ProgressionDefinition definition,
                                      String templateId,
                                      String code,
                                      String definitionId,
                                      String packQualifiedReference) {
        return equalsIgnoreCase(definition.progressionId(), templateId)
            || equalsIgnoreCase(definition.templateId(), templateId)
            || equalsIgnoreCase(definition.definitionId(), templateId)
            || equalsIgnoreCase(definition.definitionId(), definitionId)
            || equalsIgnoreCase(definition.templateId(), packQualifiedReference)
            || (!code.isBlank() && equalsIgnoreCase(definition.code(), code));
    }

    private boolean storedProgressionMatchesFilter(StoredProgression progression, String filter) {
        if (progression == null) {
            return false;
        }
        if (filter == null || filter.isBlank() || "all".equals(filter) || "toate".equals(filter)) {
            return true;
        }

        return switch (filter) {
            case "current", "curent" -> progression.current();
            case "active", "activ" -> "active".equalsIgnoreCase(progression.status());
            case "offered", "oferta", "oferit" -> "offered".equalsIgnoreCase(progression.status());
            case "completed", "complete", "completat" -> "completed".equalsIgnoreCase(progression.status());
            case "failed", "abandoned", "abandonat" -> "failed".equalsIgnoreCase(progression.status());
            case "archived", "arhivat" -> progression.archived();
            case "tracked", "urmarit" -> progression.tracked();
            case "unresolved", "missing_definition" -> !progression.definitionResolved();
            default -> contains(progression.progressionId(), filter)
                || contains(progression.packId(), filter)
                || contains(progression.mechanicId(), filter)
                || contains(progression.kind(), filter)
                || contains(progression.definitionId(), filter)
                || contains(progression.templateId(), filter)
                || contains(progression.code(), filter)
                || contains(progression.status(), filter)
                || contains(progression.mechanicLabel(), filter)
                || contains(progression.singularLabel(), filter)
                || contains(progression.pluralLabel(), filter);
        };
    }

    private String progressionId(ProgressionDefinition definition, String templateId, String code) {
        if (definition != null && !definition.progressionId().isBlank()) {
            return definition.progressionId();
        }
        return valueOrFallback(templateId, code);
    }

    private List<ProgressionDefinition> definitions() {
        if (definitionsSupplier == null) {
            return List.of();
        }
        List<ProgressionDefinition> definitions = definitionsSupplier.get();
        return definitions == null ? List.of() : List.copyOf(definitions);
    }

    private String extractDefinitionId(String templateId) {
        String safeTemplateId = valueOrEmpty(templateId);
        int separator = safeTemplateId.lastIndexOf(':');
        return separator >= 0 && separator < safeTemplateId.length() - 1
            ? safeTemplateId.substring(separator + 1)
            : safeTemplateId;
    }

    private String packQualifiedReference(String templateId) {
        String safeTemplateId = valueOrEmpty(templateId);
        int firstSeparator = safeTemplateId.indexOf(':');
        int lastSeparator = safeTemplateId.lastIndexOf(':');
        if (firstSeparator < 0 || lastSeparator <= firstSeparator || lastSeparator >= safeTemplateId.length() - 1) {
            return "";
        }
        return safeTemplateId.substring(0, firstSeparator) + ":" + safeTemplateId.substring(lastSeparator + 1);
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && !right.isBlank()
            && left.toLowerCase(Locale.ROOT).equals(right.toLowerCase(Locale.ROOT));
    }

    private boolean contains(String value, String filter) {
        return value != null && filter != null && value.toLowerCase(Locale.ROOT).contains(filter);
    }

    private String valueOrFallback(String value, String fallback) {
        String safeValue = valueOrEmpty(value);
        return safeValue.isBlank() ? valueOrEmpty(fallback) : safeValue;
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
