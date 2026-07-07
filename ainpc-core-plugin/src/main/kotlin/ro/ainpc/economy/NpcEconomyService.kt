package ro.ainpc.economy

import ro.ainpc.AINPCPlugin
import ro.ainpc.npc.NPCState
import java.sql.SQLException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

class NpcEconomyService(private val plugin: AINPCPlugin) {
    private val npcBalances: MutableMap<String, Int> = ConcurrentHashMap()
    private val npcSalaries: MutableMap<String, Int> = ConcurrentHashMap()

    companion object {
        private const val DEFAULT_SALARY = 5
        private const val MAX_BALANCE = 10000
    }

    init {
        loadAllBalances()
    }

    fun getBalance(npcId: String): Int = npcBalances.getOrDefault(npcId, 0)

    fun setBalance(npcId: String, amount: Int) {
        npcBalances[npcId] = amount.coerceIn(0, MAX_BALANCE)
        saveBalance(npcId, npcBalances[npcId]!!)
    }

    fun deposit(npcId: String, amount: Int): Boolean {
        if (amount <= 0) return false
        val current = npcBalances.getOrDefault(npcId, 0)
        npcBalances[npcId] = (current + amount).coerceAtMost(MAX_BALANCE)
        saveBalance(npcId, npcBalances[npcId]!!)
        return true
    }

    fun withdraw(npcId: String, amount: Int): Boolean {
        if (amount <= 0) return false
        val current = npcBalances.getOrDefault(npcId, 0)
        if (current < amount) return false
        npcBalances[npcId] = current - amount
        saveBalance(npcId, npcBalances[npcId]!!)
        return true
    }

    fun getSalary(occupation: String?): Int {
        if (occupation.isNullOrBlank()) return 0
        return npcSalaries.getOrElse(occupation) { DEFAULT_SALARY }
    }

    fun setSalary(occupation: String, amount: Int) {
        npcSalaries[occupation] = amount.coerceAtLeast(0)
    }

    fun paySalaries() {
        var count = 0
        var total = 0
        for (npc in plugin.npcManager.getAllNPCs()) {
            if (!npc.isSpawned()) continue
            val occupation = npc.occupation ?: continue
            val salary = getSalary(occupation)
            if (salary <= 0) continue
            val npcKey = npcKey(npc.uuid, npc.databaseId)
            deposit(npcKey, salary)
            count++
            total += salary
        }
        plugin.platform.addonRegistry.dispatchSalaryPaid(count, total)
        plugin.debug("[NpcEconomy] Salarii platite pentru NPC-uri active.")
    }

    fun paySalaryForWork(npcUuid: UUID?, npcDbId: Int) {
        if (npcUuid == null) return
        val npc = plugin.npcManager.getNPCByUUID(npcUuid) ?: return
        val occupation = npc.occupation ?: return
        val salary = getSalary(occupation)
        if (salary <= 0) return
        deposit(npcKey(npcUuid, npcDbId), salary)
    }

    fun getBalanceCount(): Int = npcBalances.size

    fun getTotalEconomyValue(): Int = npcBalances.values.sum()

    fun flushAll() {
        for ((key, balance) in npcBalances) {
            saveBalance(key, balance)
        }
    }

    private fun npcKey(uuid: UUID?, dbId: Int): String {
        return if (uuid != null) "npc_${uuid}" else "npc_db_$dbId"
    }

    private fun saveBalance(npcKey: String, balance: Int) {
        try {
            val sql = """
                INSERT OR REPLACE INTO npc_economy
                (npc_key, balance, updated_at)
                VALUES (?, ?, ?)
            """.trimIndent()
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.setString(1, npcKey)
                stmt.setInt(2, balance)
                stmt.setLong(3, System.currentTimeMillis())
                stmt.executeUpdate()
            }
        } catch (e: SQLException) {
            plugin.logger.log(Level.WARNING, "[NpcEconomy] Eroare salvare balanta $npcKey", e)
        }
    }

    private fun loadAllBalances() {
        try {
            val sql = "SELECT npc_key, balance FROM npc_economy"
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        val key = rs.getString("npc_key")
                        val balance = rs.getInt("balance")
                        npcBalances[key] = balance
                    }
                }
            }
            plugin.debug("[NpcEconomy] Balante incarcate: ${npcBalances.size}")
        } catch (e: SQLException) {
            if (e.message?.contains("no such table") == true) {
                plugin.logger.info("[NpcEconomy] Tabela npc_economy nu exista — se va crea la prima scriere.")
            } else {
                plugin.logger.log(Level.WARNING, "[NpcEconomy] Eroare incarcare balante", e)
            }
        }
    }
}
