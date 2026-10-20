package ro.ainpc.engine

import java.sql.Connection
import java.util.UUID

internal data class StoredScenarioSession(
    val scenarioId: String,
    val templateId: String,
    val version: Int,
    val updatedAt: Long,
    val snapshot: ScenarioSessionSnapshot?,
    val issue: String?,
)

internal class ScenarioSessionSnapshotStore {
    fun save(connection: Connection, snapshot: ScenarioSessionSnapshot) {
        val encoded = ScenarioSessionSnapshotCodec.encode(snapshot)
        val updatedAt = System.currentTimeMillis()
        val updated = connection.prepareStatement(
            "UPDATE scenario_sessions SET template_id = ?, snapshot_version = ?, snapshot_data = ?, updated_at = ? WHERE scenario_id = ?"
        ).use { statement ->
            statement.setString(1, snapshot.templateId)
            statement.setInt(2, snapshot.version)
            statement.setString(3, encoded)
            statement.setLong(4, updatedAt)
            statement.setString(5, snapshot.scenarioId.toString())
            statement.executeUpdate()
        }
        if (updated == 0) {
            connection.prepareStatement(
                "INSERT INTO scenario_sessions (scenario_id, template_id, snapshot_version, snapshot_data, updated_at) VALUES (?, ?, ?, ?, ?)"
            ).use { statement ->
                statement.setString(1, snapshot.scenarioId.toString())
                statement.setString(2, snapshot.templateId)
                statement.setInt(3, snapshot.version)
                statement.setString(4, encoded)
                statement.setLong(5, updatedAt)
                statement.executeUpdate()
            }
        }
    }

    fun find(connection: Connection, scenarioId: UUID): ScenarioSessionSnapshot? = connection.prepareStatement(
        "SELECT template_id, snapshot_version, snapshot_data FROM scenario_sessions WHERE scenario_id = ?"
    ).use { statement ->
        statement.setString(1, scenarioId.toString())
        statement.executeQuery().use { rows ->
            if (!rows.next()) return null
            val snapshot = ScenarioSessionSnapshotCodec.decode(rows.getString("snapshot_data")) ?: return null
            snapshot.takeIf {
                it.scenarioId == scenarioId &&
                    it.templateId == rows.getString("template_id") &&
                    it.version == rows.getInt("snapshot_version")
            }
        }
    }

    fun delete(connection: Connection, scenarioId: UUID) {
        connection.prepareStatement("DELETE FROM scenario_sessions WHERE scenario_id = ?").use { statement ->
            statement.setString(1, scenarioId.toString())
            statement.executeUpdate()
        }
    }

    fun count(connection: Connection): Long = connection.createStatement().use { statement ->
        statement.executeQuery("SELECT COUNT(*) FROM scenario_sessions").use { rows ->
            rows.next()
            rows.getLong(1)
        }
    }

    fun countByTemplate(connection: Connection, templateId: String): Long = connection.prepareStatement(
        "SELECT COUNT(*) FROM scenario_sessions WHERE LOWER(template_id) = LOWER(?)"
    ).use { statement ->
        statement.setString(1, templateId)
        statement.executeQuery().use { rows ->
            rows.next()
            rows.getLong(1)
        }
    }

    fun loadPage(connection: Connection, limit: Int, offset: Int): List<StoredScenarioSession> {
        require(limit in 1..100 && offset >= 0)
        return connection.prepareStatement(
            "SELECT scenario_id, template_id, snapshot_version, snapshot_data, updated_at " +
                "FROM scenario_sessions ORDER BY updated_at DESC, scenario_id ASC LIMIT ? OFFSET ?"
        ).use { statement ->
            statement.setInt(1, limit)
            statement.setInt(2, offset)
            statement.executeQuery().use { rows ->
                buildList {
                    while (rows.next()) {
                        val scenarioId = rows.getString("scenario_id")
                        val templateId = rows.getString("template_id")
                        val version = rows.getInt("snapshot_version")
                        val snapshot = ScenarioSessionSnapshotCodec.decode(rows.getString("snapshot_data"))
                        val issue = when {
                            snapshot == null -> "payload invalid sau versiune nesuportata"
                            snapshot.scenarioId.toString() != scenarioId -> "ID-ul randului difera de snapshot"
                            snapshot.templateId != templateId -> "template-ul randului difera de snapshot"
                            snapshot.version != version -> "versiunea randului difera de snapshot"
                            else -> null
                        }
                        add(StoredScenarioSession(scenarioId, templateId, version, rows.getLong("updated_at"),
                            snapshot.takeIf { issue == null }, issue))
                    }
                }
            }
        }
    }
}
