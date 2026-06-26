package ro.ainpc.reputation

import ro.ainpc.AINPCPlugin
import ro.ainpc.api.ReputationApi
import ro.ainpc.api.ReputationEntry
import java.sql.SQLException
import java.util.logging.Level

class ReputationService(private val plugin: AINPCPlugin) : ReputationApi {

    override fun getReputation(playerUuid: String, scopeType: String, scopeId: String): Int {
        return try {
            val sql = "SELECT reputation FROM player_reputation WHERE player_uuid = ? AND scope_type = ? AND scope_id = ?"
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.setString(1, playerUuid)
                stmt.setString(2, scopeType)
                stmt.setString(3, scopeId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) rs.getInt("reputation") else 0
                }
            }
        } catch (e: SQLException) {
            plugin.logger.log(Level.WARNING, "Eroare la citirea reputatiei pentru $playerUuid/$scopeType/$scopeId", e)
            0
        }
    }

    override fun addReputation(playerUuid: String, scopeType: String, scopeId: String, amount: Int) {
        try {
            val current = getReputation(playerUuid, scopeType, scopeId)
            setReputation(playerUuid, scopeType, scopeId, current + amount)
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Eroare la adaugarea reputatiei", e)
        }
    }

    override fun setReputation(playerUuid: String, scopeType: String, scopeId: String, value: Int) {
        try {
            val sql = "INSERT OR REPLACE INTO player_reputation " +
                "(player_uuid, scope_type, scope_id, reputation, last_interaction, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, COALESCE((SELECT created_at FROM player_reputation " +
                "WHERE player_uuid = ? AND scope_type = ? AND scope_id = ?), ?), ?)"
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                val now = System.currentTimeMillis()
                stmt.setString(1, playerUuid)
                stmt.setString(2, scopeType)
                stmt.setString(3, scopeId)
                stmt.setInt(4, value)
                stmt.setLong(5, now)
                stmt.setString(6, playerUuid)
                stmt.setString(7, scopeType)
                stmt.setString(8, scopeId)
                stmt.setLong(9, now)
                stmt.setLong(10, now)
                stmt.executeUpdate()
            }
        } catch (e: SQLException) {
            plugin.logger.log(Level.WARNING, "Eroare la setarea reputatiei", e)
        }
    }

    override fun getTopReputations(scopeType: String, scopeId: String, limit: Int): List<ReputationEntry> {
        return try {
            val sql = "SELECT player_uuid, reputation FROM player_reputation " +
                "WHERE scope_type = ? AND scope_id = ? ORDER BY reputation DESC LIMIT ?"
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.setString(1, scopeType)
                stmt.setString(2, scopeId)
                stmt.setInt(3, limit.coerceIn(1, 100))
                stmt.executeQuery().use { rs ->
                    val results = mutableListOf<ReputationEntry>()
                    while (rs.next()) {
                        val playerUuid = rs.getString("player_uuid")
                        val player = plugin.server.getOfflinePlayer(java.util.UUID.fromString(playerUuid))
                        results.add(ReputationEntry(
                            playerUuid = playerUuid,
                            playerName = player.name ?: "Unknown",
                            reputation = rs.getInt("reputation"),
                        ))
                    }
                    results
                }
            }
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Eroare la citirea top reputatii", e)
            emptyList()
        }
    }
}
