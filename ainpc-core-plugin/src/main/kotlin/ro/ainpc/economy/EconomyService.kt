package ro.ainpc.economy

import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import java.sql.SQLException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

class EconomyService(private val plugin: AINPCPlugin) {
    private val balances: MutableMap<UUID, Int> = ConcurrentHashMap()
    private var dirty: Boolean = false

    init {
        loadAllBalances()
    }

    fun getBalance(player: Player): Int {
        return balances.getOrDefault(player.uniqueId, 0)
    }

    fun getBalanceByUuid(uuid: UUID): Int {
        return balances.getOrDefault(uuid, 0)
    }

    fun setBalance(player: Player, amount: Int) {
        balances[player.uniqueId] = amount.coerceAtLeast(0)
        dirty = true
        saveBalance(player.uniqueId, amount.coerceAtLeast(0))
    }

    fun deposit(player: Player, amount: Int): Boolean {
        if (amount <= 0) return false
        val newBalance = balances.merge(player.uniqueId, amount) { old, new -> old + new } ?: amount
        dirty = true
        saveBalance(player.uniqueId, newBalance)
        return true
    }

    fun withdraw(player: Player, amount: Int): Boolean {
        if (amount <= 0) return false
        val current = balances.getOrDefault(player.uniqueId, 0)
        if (current < amount) return false
        val newBalance = current - amount
        balances[player.uniqueId] = newBalance
        dirty = true
        saveBalance(player.uniqueId, newBalance)
        return true
    }

    fun transfer(from: Player, to: Player, amount: Int): Boolean {
        if (withdraw(from, amount)) {
            deposit(to, amount)
            return true
        }
        return false
    }

    fun flush() {
        if (!dirty) return
        for ((uuid, balance) in balances) {
            saveBalance(uuid, balance)
        }
        dirty = false
    }

    private fun saveBalance(uuid: UUID, balance: Int) {
        try {
            val sql = """
                INSERT OR REPLACE INTO economy_balances 
                (player_uuid, balance, created_at, updated_at)
                VALUES (?, ?, 
                    COALESCE((SELECT created_at FROM economy_balances WHERE player_uuid = ?), ?),
                    ?)
            """.trimIndent()
            val now = System.currentTimeMillis()
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.setString(1, uuid.toString())
                stmt.setInt(2, balance)
                stmt.setString(3, uuid.toString())
                stmt.setLong(4, now)
                stmt.setLong(5, now)
                stmt.executeUpdate()
            }
        } catch (e: SQLException) {
            plugin.logger.log(Level.WARNING, "Nu am putut salva balanta pentru $uuid: ${e.message}", e)
        }
    }

    private fun loadAllBalances() {
        try {
            val sql = "SELECT player_uuid, balance FROM economy_balances"
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        val uuid = UUID.fromString(rs.getString("player_uuid"))
                        val balance = rs.getInt("balance")
                        balances[uuid] = balance
                    }
                }
            }
            plugin.debug("Balante incarcate din DB: ${balances.size}")
        } catch (e: SQLException) {
            if (e.message?.contains("no such table") == true) {
                plugin.logger.info("Tabela economy_balances nu exista inca — se va crea la prima scriere.")
            } else {
                plugin.logger.log(Level.WARNING, "Nu am putut incarca balantele: ${e.message}", e)
            }
        }
    }
}
