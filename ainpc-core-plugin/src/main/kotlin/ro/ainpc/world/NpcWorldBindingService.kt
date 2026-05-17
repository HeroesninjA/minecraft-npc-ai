package ro.ainpc.world

import ro.ainpc.AINPCPlugin
import ro.ainpc.database.DatabaseManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.util.Optional
import java.util.logging.Logger

class NpcWorldBindingService {
    fun interface StatementProvider {
        @Throws(SQLException::class)
        fun prepareStatement(sql: String): PreparedStatement
    }

    private val statements: StatementProvider?
    private val logger: Logger

    constructor(plugin: AINPCPlugin?) : this(
        if (plugin != null) plugin.databaseManager else null,
        if (plugin != null) plugin.logger else null
    )

    constructor(databaseManager: DatabaseManager?, logger: Logger?) : this(
        if (databaseManager != null) StatementProvider { sql -> databaseManager.prepareStatement(sql) } else null,
        logger
    )

    internal constructor(statements: StatementProvider?, logger: Logger?) {
        this.statements = statements
        this.logger = logger ?: Logger.getLogger(NpcWorldBindingService::class.java.name)
    }

    @Throws(SQLException::class)
    fun saveBinding(binding: NpcWorldBinding?): NpcWorldBinding {
        val normalized = requireValid(binding)
        val now = System.currentTimeMillis()
        val createdAt = if (normalized.createdAt() > 0L) normalized.createdAt() else now
        val sql = """
            INSERT INTO npc_world_bindings (
                npc_id, npc_uuid, npc_name,
                home_place_id, work_place_id, social_place_id,
                home_node_id, work_node_id, social_node_id,
                family_id, source, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(npc_id) DO UPDATE SET
                npc_uuid = excluded.npc_uuid,
                npc_name = excluded.npc_name,
                home_place_id = excluded.home_place_id,
                work_place_id = excluded.work_place_id,
                social_place_id = excluded.social_place_id,
                home_node_id = excluded.home_node_id,
                work_node_id = excluded.work_node_id,
                social_node_id = excluded.social_node_id,
                family_id = excluded.family_id,
                source = excluded.source,
                updated_at = excluded.updated_at
        """.trimIndent()

        requireStatements().prepareStatement(sql).use { statement ->
            statement.setInt(1, normalized.npcId())
            statement.setString(2, normalized.npcUuid())
            statement.setString(3, normalized.npcName())
            statement.setString(4, normalized.homePlaceId())
            statement.setString(5, normalized.workPlaceId())
            statement.setString(6, normalized.socialPlaceId())
            statement.setString(7, normalized.homeNodeId())
            statement.setString(8, normalized.workNodeId())
            statement.setString(9, normalized.socialNodeId())
            statement.setString(10, normalized.familyId())
            statement.setString(11, normalized.source())
            statement.setLong(12, createdAt)
            statement.setLong(13, now)
            statement.executeUpdate()
        }

        return NpcWorldBinding(
            normalized.npcId(),
            normalized.npcUuid(),
            normalized.npcName(),
            normalized.homePlaceId(),
            normalized.workPlaceId(),
            normalized.socialPlaceId(),
            normalized.homeNodeId(),
            normalized.workNodeId(),
            normalized.socialNodeId(),
            normalized.familyId(),
            normalized.source(),
            createdAt,
            now
        )
    }

    @Throws(SQLException::class)
    fun getBinding(npcId: Int): Optional<NpcWorldBinding> {
        if (npcId <= 0) {
            return Optional.empty()
        }

        val sql = """
            SELECT npc_id, npc_uuid, npc_name,
                   home_place_id, work_place_id, social_place_id,
                   home_node_id, work_node_id, social_node_id,
                   family_id, source, created_at, updated_at
            FROM npc_world_bindings
            WHERE npc_id = ?
        """.trimIndent()

        requireStatements().prepareStatement(sql).use { statement ->
            statement.setInt(1, npcId)
            statement.executeQuery().use { resultSet ->
                return if (resultSet.next()) Optional.of(readBinding(resultSet)) else Optional.empty()
            }
        }
    }

    @Throws(SQLException::class)
    fun listBindings(limit: Int): List<NpcWorldBinding> {
        val safeLimit = maxOf(1, limit)
        val sql = """
            SELECT npc_id, npc_uuid, npc_name,
                   home_place_id, work_place_id, social_place_id,
                   home_node_id, work_node_id, social_node_id,
                   family_id, source, created_at, updated_at
            FROM npc_world_bindings
            ORDER BY updated_at DESC, npc_id ASC
            LIMIT ?
        """.trimIndent()

        val bindings = mutableListOf<NpcWorldBinding>()
        requireStatements().prepareStatement(sql).use { statement ->
            statement.setInt(1, safeLimit)
            statement.executeQuery().use { resultSet ->
                while (resultSet.next()) {
                    bindings.add(readBinding(resultSet))
                }
            }
        }
        return bindings
    }

    @Throws(SQLException::class)
    fun countBindings(): Int {
        requireStatements().prepareStatement("SELECT COUNT(*) FROM npc_world_bindings").use { statement ->
            statement.executeQuery().use { resultSet ->
                return if (resultSet.next()) resultSet.getInt(1) else 0
            }
        }
    }

    @Throws(SQLException::class)
    fun deleteBinding(npcId: Int): Boolean {
        if (npcId <= 0) {
            return false
        }

        requireStatements().prepareStatement("DELETE FROM npc_world_bindings WHERE npc_id = ?").use { statement ->
            statement.setInt(1, npcId)
            return statement.executeUpdate() > 0
        }
    }

    private fun requireValid(binding: NpcWorldBinding?): NpcWorldBinding {
        if (binding == null) {
            throw IllegalArgumentException("Binding-ul NPC-world nu poate fi null.")
        }
        if (binding.npcId() <= 0) {
            throw IllegalArgumentException("Binding-ul NPC-world are npc_id invalid: " + binding.npcId())
        }
        if (!binding.hasAnyPlaceBinding()) {
            logger.fine("Salvez binding NPC-world fara place IDs pentru npc_id=" + binding.npcId())
        }
        return binding
    }

    @Throws(SQLException::class)
    private fun readBinding(resultSet: ResultSet): NpcWorldBinding {
        return NpcWorldBinding(
            resultSet.getInt("npc_id"),
            text(resultSet, "npc_uuid"),
            text(resultSet, "npc_name"),
            text(resultSet, "home_place_id"),
            text(resultSet, "work_place_id"),
            text(resultSet, "social_place_id"),
            text(resultSet, "home_node_id"),
            text(resultSet, "work_node_id"),
            text(resultSet, "social_node_id"),
            text(resultSet, "family_id"),
            text(resultSet, "source"),
            resultSet.getLong("created_at"),
            resultSet.getLong("updated_at")
        )
    }

    @Throws(SQLException::class)
    private fun requireStatements(): StatementProvider {
        if (statements == null) {
            throw SQLException("NpcWorldBindingService nu are acces la baza de date.")
        }
        return statements
    }

    companion object {
        @Throws(SQLException::class)
        private fun text(resultSet: ResultSet, column: String): String {
            val value = resultSet.getString(column)
            return value ?: ""
        }
    }
}
