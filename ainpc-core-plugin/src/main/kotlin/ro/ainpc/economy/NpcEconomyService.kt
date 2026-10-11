package ro.ainpc.economy

import org.bukkit.configuration.ConfigurationSection
import ro.ainpc.AINPCPlugin
import ro.ainpc.api.NpcEconomyApi
import ro.ainpc.database.DatabaseManager
import ro.ainpc.npc.AINPC
import ro.ainpc.utils.ConfigKeys
import java.sql.SQLException
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Npceconomyservice.
 */
class NpcEconomyService internal constructor(
    private val databaseManager: DatabaseManager,
    private val config: ConfigurationSection,
    private val allNpcs: () -> Collection<AINPC>,
    private val findNpc: (UUID) -> AINPC?,
    private val salaryPaid: (Int, Int) -> Unit,
    private val logger: Logger,
    private val debug: (String) -> Unit = {},
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) : NpcEconomyApi {
    constructor(plugin: AINPCPlugin) : this(
        plugin.databaseManager,
        plugin.config,
        { plugin.npcManager.getAllNPCs() },
        { plugin.npcManager.getNPCByUuid(it) },
        { count, total -> plugin.platform.addonRegistry.dispatchSalaryPaid(count, total) },
        plugin.logger,
        plugin::debug,
    )

    private data class NpcAccount(val balance: Int, val lastSalaryAt: Long = 0L)

    private val npcAccounts = mutableMapOf<String, NpcAccount>()
    private val npcSalaries = mutableMapOf<String, Int>()

    companion object {
        private const val DEFAULT_SALARY = 5
        private const val MAX_BALANCE = 10000
    }

    private var accountsLoaded = loadAllBalances()

    @Synchronized
    override fun getBalance(npcKey: String): Int {
        if (!ensureAccountsLoaded()) return 0
        return npcAccounts[npcKey]?.balance ?: 0
    }

    @Synchronized
    fun setBalance(npcId: String, amount: Int): Boolean {
        if (!ensureAccountsLoaded()) return false
        val account = npcAccounts[npcId] ?: NpcAccount(0)
        return persistAccount(npcId, account.copy(balance = amount.coerceIn(0, MAX_BALANCE)))
    }

    @Synchronized
    override fun deposit(npcKey: String, amount: Int): Boolean {
        if (amount <= 0) return false
        if (!ensureAccountsLoaded()) return false
        val account = npcAccounts[npcKey] ?: NpcAccount(0)
        val credited = amount.coerceAtMost(MAX_BALANCE - account.balance)
        if (credited <= 0) return false
        return persistAccount(npcKey, account.copy(balance = account.balance + credited))
    }

    @Synchronized
    override fun withdraw(npcKey: String, amount: Int): Boolean {
        if (amount <= 0) return false
        if (!ensureAccountsLoaded()) return false
        val account = npcAccounts[npcKey] ?: return false
        if (account.balance < amount) return false
        return persistAccount(npcKey, account.copy(balance = account.balance - amount))
    }

    @Synchronized
    override fun getSalary(occupation: String?): Int {
        if (occupation.isNullOrBlank()) return 0
        return npcSalaries.getOrElse(occupation) {
            config.getInt("economy.npc_salary_overrides.$occupation", DEFAULT_SALARY).coerceAtLeast(0)
        }
    }

    @Synchronized
    fun setSalary(occupation: String, amount: Int) {
        npcSalaries[occupation] = amount.coerceAtLeast(0)
    }

    @Synchronized
    fun paySalaries() {
        var count = 0
        var total = 0L
        for (npc in allNpcs()) {
            if (!npc.isSpawned()) continue
            val credited = paySalary(npc)
            if (credited <= 0) continue
            count++
            total += credited
        }
        salaryPaid(count, total.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
        debug("[NpcEconomy] Salarii platite: $count NPC-uri, $total monede.")
    }

    @Synchronized
    override fun paySalaryForWork(npcUuid: UUID, npcDbId: Int): Boolean {
        val npc = findNpc(npcUuid) ?: return false
        if (!npc.isSpawned() || !npc.currentState.isWorkState()) return false
        return paySalary(npc) > 0
    }

    private fun paySalary(npc: AINPC): Int {
        val occupation = npc.occupation ?: return 0
        val salary = getSalary(occupation)
        if (salary <= 0 || !ensureAccountsLoaded()) return 0
        val key = npcKey(npc.uuid, npc.databaseId)
        val account = npcAccounts[key] ?: NpcAccount(0)
        val now = currentTimeMillis()
        val intervalMillis = config.getInt(ConfigKeys.ECONOMY_SALARY_INTERVAL, 600).coerceAtLeast(120) * 1000L
        if (account.lastSalaryAt > 0 && now - account.lastSalaryAt < intervalMillis) return 0
        val credited = salary.coerceAtMost(MAX_BALANCE - account.balance)
        if (credited <= 0) return 0
        return if (persistAccount(key, NpcAccount(account.balance + credited, now))) credited else 0
    }

    @Synchronized
    override fun getBalanceCount(): Int {
        if (!ensureAccountsLoaded()) return 0
        return npcAccounts.size
    }

    @Synchronized
    override fun getTotalEconomyValue(): Int {
        if (!ensureAccountsLoaded()) return 0
        return npcAccounts.values.sumOf { it.balance.toLong() }.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    @Synchronized
    fun flushAll() {
        if (!ensureAccountsLoaded() || npcAccounts.isEmpty()) return
        try {
            databaseManager.executeTransaction {
                for ((key, account) in npcAccounts) {
                    saveAccount(key, account)
                }
            }
        } catch (exception: SQLException) {
            logger.log(Level.WARNING, "[NpcEconomy] Eroare salvare conturi", exception)
        }
    }

    private fun npcKey(uuid: UUID?, dbId: Int): String {
        return if (uuid != null) "npc_${uuid}" else "npc_db_$dbId"
    }

    private fun persistAccount(key: String, account: NpcAccount): Boolean {
        return try {
            databaseManager.executeTransaction {
                saveAccount(key, account)
            }
            npcAccounts[key] = account
            true
        } catch (exception: SQLException) {
            logger.log(Level.WARNING, "[NpcEconomy] Eroare salvare cont $key", exception)
            false
        }
    }

    private fun saveAccount(key: String, account: NpcAccount) {
        val sql = """
            INSERT OR REPLACE INTO npc_economy
            (npc_key, balance, created_at, updated_at, last_salary_at)
            VALUES (?, ?, COALESCE((SELECT created_at FROM npc_economy WHERE npc_key = ?), ?), ?, ?)
        """.trimIndent()
        val now = currentTimeMillis()
        databaseManager.prepareStatement(sql).use { stmt ->
            stmt.setString(1, key)
            stmt.setInt(2, account.balance)
            stmt.setString(3, key)
            stmt.setLong(4, now)
            stmt.setLong(5, now)
            stmt.setLong(6, account.lastSalaryAt)
            if (stmt.executeUpdate() <= 0) throw SQLException("Contul NPC nu a fost salvat")
        }
    }

    private fun ensureAccountsLoaded(): Boolean {
        if (!accountsLoaded) accountsLoaded = loadAllBalances()
        return accountsLoaded
    }

    private fun loadAllBalances(): Boolean {
        return try {
            val loadedAccounts = mutableMapOf<String, NpcAccount>()
            val sql = "SELECT npc_key, balance, last_salary_at FROM npc_economy"
            databaseManager.prepareStatement(sql).use { stmt ->
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        val key = rs.getString("npc_key")
                        val balance = rs.getLong("balance")
                        val lastSalaryAt = rs.getLong("last_salary_at")
                        require(balance in 0L..MAX_BALANCE.toLong() && lastSalaryAt >= 0) { "Cont NPC invalid: $key" }
                        loadedAccounts[key] = NpcAccount(balance.toInt(), lastSalaryAt)
                    }
                }
            }
            npcAccounts.clear()
            npcAccounts.putAll(loadedAccounts)
            debug("[NpcEconomy] Balante incarcate: ${npcAccounts.size}")
            true
        } catch (e: SQLException) {
            logger.log(Level.WARNING, "[NpcEconomy] Eroare incarcare balante", e)
            false
        } catch (e: IllegalArgumentException) {
            logger.log(Level.WARNING, "[NpcEconomy] Date invalide in conturi", e)
            false
        }
    }
}
