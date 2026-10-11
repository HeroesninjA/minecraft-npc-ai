package ro.ainpc.economy

import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import ro.ainpc.database.DatabaseManager
import java.sql.SQLException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Economyservice.
 */
class EconomyService internal constructor(
    private val databaseManager: DatabaseManager,
    private val logger: Logger,
    private val debug: (String) -> Unit = {},
) {
    constructor(plugin: AINPCPlugin) : this(plugin.databaseManager, plugin.logger, plugin::debug)

    private val balances: MutableMap<UUID, Int> = ConcurrentHashMap()
    private var dirty: Boolean = false

    private var balancesLoaded = loadAllBalances()

    fun getBalance(player: Player): Int {
        return balances.getOrDefault(player.uniqueId, 0)
    }

    fun getBalanceByUuid(uuid: UUID): Int {
        return balances.getOrDefault(uuid, 0)
    }

    @Synchronized
    fun setBalance(player: Player, amount: Int): Boolean {
        if (!ensureBalancesLoaded()) return false
        return persistBalances(mapOf(player.uniqueId to amount.coerceAtLeast(0)))
    }

    @Synchronized
    fun deposit(player: Player, amount: Int): Boolean {
        return depositWithPersistence(player.uniqueId, amount) {}
    }

    @Synchronized
    internal fun depositWithPersistence(uuid: UUID, amount: Int, persistRelated: () -> Unit): Boolean {
        if (amount <= 0) return false
        if (!ensureBalancesLoaded()) return false
        val current = balances.getOrDefault(uuid, 0)
        if (current > Int.MAX_VALUE - amount) return false
        return persistBalances(mapOf(uuid to current + amount), persistRelated)
    }

    @Synchronized
    fun withdraw(player: Player, amount: Int): Boolean {
        return withdrawWithPersistence(player.uniqueId, amount) {}
    }

    @Synchronized
    internal fun withdrawWithPersistence(uuid: UUID, amount: Int, persistRelated: () -> Unit): Boolean {
        if (amount <= 0) return false
        if (!ensureBalancesLoaded()) return false
        val current = balances.getOrDefault(uuid, 0)
        if (current < amount) return false
        return persistBalances(mapOf(uuid to current - amount), persistRelated)
    }

    @Synchronized
    fun transfer(from: Player, to: Player, amount: Int): Boolean {
        if (amount <= 0) return false
        if (!ensureBalancesLoaded()) return false
        val fromUuid = from.uniqueId
        val toUuid = to.uniqueId
        val fromBalance = balances.getOrDefault(fromUuid, 0)
        if (fromBalance < amount) return false
        if (fromUuid == toUuid) return true
        val toBalance = balances.getOrDefault(toUuid, 0)
        if (toBalance > Int.MAX_VALUE - amount) return false
        return persistBalances(mapOf(fromUuid to fromBalance - amount, toUuid to toBalance + amount))
    }

    @Synchronized
    fun flush() {
        if (!dirty) return
        if (persistBalances(balances.toMap())) {
            dirty = false
        }
    }

    private fun persistBalances(updates: Map<UUID, Int>, persistRelated: () -> Unit = {}): Boolean {
        return try {
            databaseManager.executeTransaction {
                for ((uuid, balance) in updates) {
                    saveBalance(uuid, balance)
                }
                persistRelated()
            }
            balances.putAll(updates)
            dirty = true
            true
        } catch (exception: SQLException) {
            logger.log(Level.WARNING, "Nu am putut salva tranzactia economica: ${exception.message}", exception)
            false
        }
    }

    private fun saveBalance(uuid: UUID, balance: Int) {
        val sql = """
            INSERT OR REPLACE INTO economy_balances
            (player_uuid, balance, created_at, updated_at)
            VALUES (?, ?,
                COALESCE((SELECT created_at FROM economy_balances WHERE player_uuid = ?), ?),
                ?)
        """.trimIndent()
        val now = System.currentTimeMillis()
        databaseManager.prepareStatement(sql).use { stmt ->
            stmt.setString(1, uuid.toString())
            stmt.setInt(2, balance)
            stmt.setString(3, uuid.toString())
            stmt.setLong(4, now)
            stmt.setLong(5, now)
            stmt.executeUpdate()
        }
    }

    private fun ensureBalancesLoaded(): Boolean {
        if (!balancesLoaded) {
            balancesLoaded = loadAllBalances()
        }
        return balancesLoaded
    }

    private fun loadAllBalances(): Boolean {
        return try {
            val loadedBalances = mutableMapOf<UUID, Int>()
            val sql = "SELECT player_uuid, balance FROM economy_balances"
            databaseManager.prepareStatement(sql).use { stmt ->
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        val uuid = UUID.fromString(rs.getString("player_uuid"))
                        val balance = rs.getInt("balance")
                        loadedBalances[uuid] = balance
                    }
                }
            }
            balances.putAll(loadedBalances)
            debug("Balante incarcate din DB: ${balances.size}")
            true
        } catch (e: SQLException) {
            logger.log(Level.WARNING, "Nu am putut incarca balantele: ${e.message}", e)
            false
        }
    }
}
