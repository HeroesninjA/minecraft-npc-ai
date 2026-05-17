package ro.ainpc.progression

import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.util.Locale
import java.util.Objects
import java.util.function.Supplier

class ProgressionRepository(
    private val statementProvider: StatementProvider?,
    private val definitionsSupplier: Supplier<List<ProgressionDefinition>>?
) {
    fun interface StatementProvider {
        @Throws(SQLException::class)
        fun prepareStatement(sql: String): PreparedStatement
    }

    @Throws(SQLException::class)
    fun findAll(): List<StoredProgression> {
        if (statementProvider == null) {
            return listOf()
        }

        val sql = """
            SELECT player_uuid, template_id, quest_code, status, started_at, completed_at,
                   current_phase, current_stage_id, objective_progress, quest_variables, updated_at, tracked
            FROM player_quests
            ORDER BY player_uuid, status, updated_at DESC, template_id
        """.trimIndent()

        val definitions = definitions()
        statementProvider.prepareStatement(sql).use { statement ->
            statement.executeQuery().use { resultSet ->
                val rows = arrayListOf<StoredProgression>()
                while (resultSet.next()) {
                    rows.add(toStoredProgression(resultSet, definitions))
                }
                return rows.toList()
            }
        }
    }

    @Throws(SQLException::class)
    fun find(playerUuid: String?, filter: String?, limit: Int): List<StoredProgression> {
        val safePlayerUuid = valueOrEmpty(playerUuid)
        val normalizedFilter = valueOrEmpty(filter).lowercase(Locale.ROOT)
        return findAll().asSequence()
            .filter { progression ->
                safePlayerUuid.isBlank() || progression.playerUuid().equals(safePlayerUuid, ignoreCase = true)
            }
            .filter { progression -> storedProgressionMatchesFilter(progression, normalizedFilter) }
            .let { sequence ->
                if (limit > 0) sequence.take(limit) else sequence
            }
            .toList()
    }

    @Throws(SQLException::class)
    fun summarize(playerUuid: String?, filter: String?): StoredProgressionSummary {
        return StoredProgressionSummary.from(find(playerUuid, filter, 0))
    }

    @Throws(SQLException::class)
    fun findAnchorBindings(playerUuid: String?, templateId: String?, limit: Int): List<ProgressionAnchorBinding> {
        return queryAnchorBindings(playerUuid, templateId, "", "", "", limit)
    }

    @Throws(SQLException::class)
    fun findAnchorBindingsForProgression(
        playerUuid: String?,
        templateId: String?,
        questCode: String?,
        limit: Int
    ): List<ProgressionAnchorBinding> {
        val safeTemplateId = valueOrEmpty(templateId)
        val safeQuestCode = valueOrEmpty(questCode)
        if (safeTemplateId.isNotBlank()) {
            val rows = queryAnchorBindings(playerUuid, safeTemplateId, "", "", "", limit)
            if (rows.isNotEmpty() || safeQuestCode.isBlank()) {
                return rows
            }
        }
        return queryAnchorBindings(playerUuid, "", safeQuestCode, "", "", limit)
    }

    @Throws(SQLException::class)
    fun findAnchorBindingsForAnchor(
        playerUuid: String?,
        anchorType: String?,
        anchorId: String?,
        limit: Int
    ): List<ProgressionAnchorBinding> {
        return queryAnchorBindings(playerUuid, "", "", anchorType, anchorId, limit)
    }

    @Throws(SQLException::class)
    fun saveAnchorBinding(binding: ProgressionAnchorBinding?) {
        if (statementProvider == null) {
            throw SQLException("StatementProvider indisponibil")
        }
        if (binding == null) {
            throw IllegalArgumentException("Binding-ul quest anchor este null.")
        }
        if (binding.playerUuid().isBlank()
            || binding.templateId().isBlank()
            || binding.objectiveKey().isBlank()
            || binding.objectiveType().isBlank()
            || binding.anchorType().isBlank()
            || binding.anchorId().isBlank()
        ) {
            throw IllegalArgumentException("Quest anchor binding incomplet.")
        }

        val now = System.currentTimeMillis()
        val createdAt = if (binding.createdAt() > 0L) binding.createdAt() else now
        val updatedAt = if (binding.updatedAt() > 0L) binding.updatedAt() else now
        val sql = """
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
        """.trimIndent()

        statementProvider.prepareStatement(sql).use { statement ->
            statement.setString(1, binding.playerUuid())
            statement.setString(2, binding.templateId())
            statement.setString(3, binding.objectiveKey())
            statement.setString(4, binding.questCode())
            statement.setString(5, binding.objectiveType())
            statement.setString(6, binding.reference())
            statement.setString(7, binding.anchorType())
            statement.setString(8, binding.anchorId())
            statement.setString(9, binding.anchorLabel())
            statement.setLong(10, createdAt)
            statement.setLong(11, updatedAt)
            statement.executeUpdate()
        }
    }

    @Throws(SQLException::class)
    private fun toStoredProgression(resultSet: ResultSet, definitions: List<ProgressionDefinition>): StoredProgression {
        val templateId = valueOrEmpty(resultSet.getString("template_id"))
        val code = valueOrEmpty(resultSet.getString("quest_code"))
        val definition = findDefinition(templateId, code, definitions)

        return StoredProgression(
            resultSet.getString("player_uuid"),
            progressionId(definition, templateId, code),
            definition?.packId() ?: "",
            definition?.mechanicId() ?: "",
            definition?.kind() ?: "",
            definition?.category() ?: "",
            definition?.scenarioKind() ?: "",
            definition?.baseType() ?: "",
            definition?.definitionId() ?: extractDefinitionId(templateId),
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
            definition?.label() ?: "",
            definition?.singularLabel() ?: "",
            definition?.pluralLabel() ?: "",
            "player_quests"
        )
    }

    @Throws(SQLException::class)
    private fun queryAnchorBindings(
        playerUuid: String?,
        templateId: String?,
        questCode: String?,
        anchorType: String?,
        anchorId: String?,
        limit: Int
    ): List<ProgressionAnchorBinding> {
        if (statementProvider == null) {
            return listOf()
        }

        val safePlayerUuid = valueOrEmpty(playerUuid)
        val safeTemplateId = valueOrEmpty(templateId)
        val safeQuestCode = valueOrEmpty(questCode)
        val safeAnchorType = valueOrEmpty(anchorType)
        val safeAnchorId = valueOrEmpty(anchorId)
        val sql = StringBuilder(
            """
            SELECT b.player_uuid, b.template_id, b.objective_key, b.quest_code,
                   b.objective_type, b.reference, b.anchor_type, b.anchor_id,
                   b.anchor_label, b.created_at, b.updated_at, p.status
            FROM quest_anchor_bindings b
            LEFT JOIN player_quests p
              ON p.player_uuid = b.player_uuid AND p.template_id = b.template_id
            WHERE 1 = 1
            """.trimIndent()
        )
        val parameters = mutableListOf<String>()
        if (safePlayerUuid.isNotBlank()) {
            sql.append(" AND b.player_uuid = ?")
            parameters.add(safePlayerUuid)
        }
        if (safeTemplateId.isNotBlank()) {
            sql.append(" AND b.template_id = ?")
            parameters.add(safeTemplateId)
        } else if (safeQuestCode.isNotBlank()) {
            sql.append(" AND LOWER(b.quest_code) = ?")
            parameters.add(safeQuestCode.lowercase(Locale.ROOT))
        }
        if (safeAnchorType.isNotBlank()) {
            sql.append(" AND LOWER(b.anchor_type) = ?")
            parameters.add(safeAnchorType.lowercase(Locale.ROOT))
        }
        if (safeAnchorId.isNotBlank()) {
            sql.append(" AND LOWER(b.anchor_id) = ?")
            parameters.add(safeAnchorId.lowercase(Locale.ROOT))
        }
        sql.append(" ORDER BY b.updated_at DESC, b.template_id, b.objective_key LIMIT ?")

        val safeLimit = if (limit > 0) minOf(limit, 500) else 100
        statementProvider.prepareStatement(sql.toString()).use { statement ->
            var index = 1
            for (parameter in parameters) {
                statement.setString(index++, parameter)
            }
            statement.setInt(index, safeLimit)

            statement.executeQuery().use { resultSet ->
                val rows = mutableListOf<ProgressionAnchorBinding>()
                while (resultSet.next()) {
                    rows.add(toAnchorBinding(resultSet))
                }
                return rows.toList()
            }
        }
    }

    @Throws(SQLException::class)
    private fun toAnchorBinding(resultSet: ResultSet): ProgressionAnchorBinding {
        return ProgressionAnchorBinding(
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
        )
    }

    private fun findDefinition(
        templateId: String,
        code: String,
        definitions: List<ProgressionDefinition>?
    ): ProgressionDefinition? {
        if (definitions.isNullOrEmpty()) {
            return null
        }

        val safeTemplateId = valueOrEmpty(templateId)
        val safeCode = valueOrEmpty(code)
        val definitionId = extractDefinitionId(safeTemplateId)
        val packQualifiedReference = packQualifiedReference(safeTemplateId)

        return definitions.asSequence()
            .filter(Objects::nonNull)
            .firstOrNull { definition ->
                matchesDefinition(definition, safeTemplateId, safeCode, definitionId, packQualifiedReference)
            }
    }

    private fun matchesDefinition(
        definition: ProgressionDefinition,
        templateId: String,
        code: String,
        definitionId: String,
        packQualifiedReference: String
    ): Boolean {
        return equalsIgnoreCase(definition.progressionId(), templateId)
            || equalsIgnoreCase(definition.templateId(), templateId)
            || equalsIgnoreCase(definition.definitionId(), templateId)
            || equalsIgnoreCase(definition.definitionId(), definitionId)
            || equalsIgnoreCase(definition.templateId(), packQualifiedReference)
            || (code.isNotBlank() && equalsIgnoreCase(definition.code(), code))
    }

    private fun storedProgressionMatchesFilter(progression: StoredProgression, filter: String): Boolean {
        return ProgressionFilter.matchesStored(progression, filter)
    }

    private fun progressionId(definition: ProgressionDefinition?, templateId: String, code: String): String {
        if (definition != null && definition.progressionId().isNotBlank()) {
            return definition.progressionId()
        }
        return valueOrFallback(templateId, code)
    }

    private fun definitions(): List<ProgressionDefinition> {
        if (definitionsSupplier == null) {
            return listOf()
        }
        val definitions = definitionsSupplier.get()
        return definitions.toList()
    }

    private fun extractDefinitionId(templateId: String?): String {
        val safeTemplateId = valueOrEmpty(templateId)
        val separator = safeTemplateId.lastIndexOf(':')
        return if (separator >= 0 && separator < safeTemplateId.length - 1) {
            safeTemplateId.substring(separator + 1)
        } else {
            safeTemplateId
        }
    }

    private fun packQualifiedReference(templateId: String?): String {
        val safeTemplateId = valueOrEmpty(templateId)
        val firstSeparator = safeTemplateId.indexOf(':')
        val lastSeparator = safeTemplateId.lastIndexOf(':')
        if (firstSeparator < 0 || lastSeparator <= firstSeparator || lastSeparator >= safeTemplateId.length - 1) {
            return ""
        }
        return safeTemplateId.substring(0, firstSeparator) + ":" + safeTemplateId.substring(lastSeparator + 1)
    }

    private fun equalsIgnoreCase(left: String?, right: String?): Boolean {
        return left != null && right != null && right.isNotBlank()
            && left.lowercase(Locale.ROOT) == right.lowercase(Locale.ROOT)
    }

    private fun valueOrFallback(value: String?, fallback: String?): String {
        val safeValue = valueOrEmpty(value)
        return if (safeValue.isBlank()) valueOrEmpty(fallback) else safeValue
    }

    private fun valueOrEmpty(value: String?): String = value?.trim() ?: ""
}
