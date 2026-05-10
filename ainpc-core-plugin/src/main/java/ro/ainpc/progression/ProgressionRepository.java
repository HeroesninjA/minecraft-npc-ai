package ro.ainpc.progression;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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

    public List<ProgressionAnchorBinding> findAnchorBindings(String playerUuid,
                                                             String templateId,
                                                             int limit) throws SQLException {
        return queryAnchorBindings(playerUuid, templateId, "", "", "", limit);
    }

    public List<ProgressionAnchorBinding> findAnchorBindingsForProgression(String playerUuid,
                                                                           String templateId,
                                                                           String questCode,
                                                                           int limit) throws SQLException {
        String safeTemplateId = valueOrEmpty(templateId);
        String safeQuestCode = valueOrEmpty(questCode);
        if (!safeTemplateId.isBlank()) {
            List<ProgressionAnchorBinding> rows = queryAnchorBindings(playerUuid, safeTemplateId, "", "", "", limit);
            if (!rows.isEmpty() || safeQuestCode.isBlank()) {
                return rows;
            }
        }
        return queryAnchorBindings(playerUuid, "", safeQuestCode, "", "", limit);
    }

    public List<ProgressionAnchorBinding> findAnchorBindingsForAnchor(String playerUuid,
                                                                      String anchorType,
                                                                      String anchorId,
                                                                      int limit) throws SQLException {
        return queryAnchorBindings(playerUuid, "", "", anchorType, anchorId, limit);
    }

    public void saveAnchorBinding(ProgressionAnchorBinding binding) throws SQLException {
        if (statementProvider == null) {
            throw new SQLException("StatementProvider indisponibil");
        }
        if (binding == null) {
            throw new IllegalArgumentException("Binding-ul quest anchor este null.");
        }
        if (binding.playerUuid().isBlank()
            || binding.templateId().isBlank()
            || binding.objectiveKey().isBlank()
            || binding.objectiveType().isBlank()
            || binding.anchorType().isBlank()
            || binding.anchorId().isBlank()) {
            throw new IllegalArgumentException("Quest anchor binding incomplet.");
        }

        long now = System.currentTimeMillis();
        long createdAt = binding.createdAt() > 0L ? binding.createdAt() : now;
        long updatedAt = binding.updatedAt() > 0L ? binding.updatedAt() : now;
        String sql = """
            INSERT INTO quest_anchor_bindings (
                player_uuid, template_id, objective_key, quest_code, objective_type, reference,
                anchor_type, anchor_id, anchor_label, created_at, updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(player_uuid, template_id, objective_key) DO UPDATE SET
                quest_code = excluded.quest_code,
                objective_type = excluded.objective_type,
                reference = excluded.reference,
                anchor_type = excluded.anchor_type,
                anchor_id = excluded.anchor_id,
                anchor_label = excluded.anchor_label,
                updated_at = excluded.updated_at
        """;

        try (PreparedStatement statement = statementProvider.prepareStatement(sql)) {
            statement.setString(1, binding.playerUuid());
            statement.setString(2, binding.templateId());
            statement.setString(3, binding.objectiveKey());
            statement.setString(4, binding.questCode());
            statement.setString(5, binding.objectiveType());
            statement.setString(6, binding.reference());
            statement.setString(7, binding.anchorType());
            statement.setString(8, binding.anchorId());
            statement.setString(9, binding.anchorLabel());
            statement.setLong(10, createdAt);
            statement.setLong(11, updatedAt);
            statement.executeUpdate();
        }
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
            definition != null ? definition.category() : "",
            definition != null ? definition.scenarioKind() : "",
            definition != null ? definition.baseType() : "",
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

    private List<ProgressionAnchorBinding> queryAnchorBindings(String playerUuid,
                                                               String templateId,
                                                               String questCode,
                                                               String anchorType,
                                                               String anchorId,
                                                               int limit) throws SQLException {
        if (statementProvider == null) {
            return List.of();
        }

        String safePlayerUuid = valueOrEmpty(playerUuid);
        String safeTemplateId = valueOrEmpty(templateId);
        String safeQuestCode = valueOrEmpty(questCode);
        String safeAnchorType = valueOrEmpty(anchorType);
        String safeAnchorId = valueOrEmpty(anchorId);
        StringBuilder sql = new StringBuilder("""
            SELECT b.player_uuid, b.template_id, b.objective_key, b.quest_code,
                   b.objective_type, b.reference, b.anchor_type, b.anchor_id,
                   b.anchor_label, b.created_at, b.updated_at, p.status
            FROM quest_anchor_bindings b
            LEFT JOIN player_quests p
              ON p.player_uuid = b.player_uuid AND p.template_id = b.template_id
            WHERE 1 = 1
        """);
        List<String> parameters = new ArrayList<>();
        if (!safePlayerUuid.isBlank()) {
            sql.append(" AND b.player_uuid = ?");
            parameters.add(safePlayerUuid);
        }
        if (!safeTemplateId.isBlank()) {
            sql.append(" AND b.template_id = ?");
            parameters.add(safeTemplateId);
        } else if (!safeQuestCode.isBlank()) {
            sql.append(" AND LOWER(b.quest_code) = ?");
            parameters.add(safeQuestCode.toLowerCase(Locale.ROOT));
        }
        if (!safeAnchorType.isBlank()) {
            sql.append(" AND LOWER(b.anchor_type) = ?");
            parameters.add(safeAnchorType.toLowerCase(Locale.ROOT));
        }
        if (!safeAnchorId.isBlank()) {
            sql.append(" AND LOWER(b.anchor_id) = ?");
            parameters.add(safeAnchorId.toLowerCase(Locale.ROOT));
        }
        sql.append(" ORDER BY b.updated_at DESC, b.template_id, b.objective_key LIMIT ?");

        int safeLimit = limit > 0 ? Math.min(limit, 500) : 100;
        try (PreparedStatement statement = statementProvider.prepareStatement(sql.toString())) {
            int index = 1;
            for (String parameter : parameters) {
                statement.setString(index++, parameter);
            }
            statement.setInt(index, safeLimit);

            try (ResultSet resultSet = statement.executeQuery()) {
                List<ProgressionAnchorBinding> rows = new ArrayList<>();
                while (resultSet.next()) {
                    rows.add(toAnchorBinding(resultSet));
                }
                return List.copyOf(rows);
            }
        }
    }

    private ProgressionAnchorBinding toAnchorBinding(ResultSet resultSet) throws SQLException {
        return new ProgressionAnchorBinding(
            resultSet.getString("player_uuid"),
            resultSet.getString("template_id"),
            resultSet.getString("objective_key"),
            resultSet.getString("quest_code"),
            resultSet.getString("objective_type"),
            resultSet.getString("reference"),
            resultSet.getString("anchor_type"),
            resultSet.getString("anchor_id"),
            resultSet.getString("anchor_label"),
            resultSet.getLong("created_at"),
            resultSet.getLong("updated_at"),
            resultSet.getString("status")
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
        return ProgressionFilter.matchesStored(progression, filter);
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

    private String valueOrFallback(String value, String fallback) {
        String safeValue = valueOrEmpty(value);
        return safeValue.isBlank() ? valueOrEmpty(fallback) : safeValue;
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
