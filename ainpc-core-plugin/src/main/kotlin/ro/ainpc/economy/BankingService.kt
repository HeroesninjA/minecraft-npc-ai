package ro.ainpc.economy

import ro.ainpc.AINPCPlugin
import java.sql.SQLException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

class BankingService(private val plugin: AINPCPlugin) {
    private val investments: MutableMap<UUID, MutableList<Investment>> = ConcurrentHashMap()
    private val interestCache: MutableMap<UUID, Double> = ConcurrentHashMap()

    data class Investment(
        val id: String,
        val amount: Int,
        val interestRate: Double,
        val createdAt: Long,
        val durationHours: Int,
        val description: String
    ) {
        val matured: Boolean get() = System.currentTimeMillis() - createdAt >= durationHours * 3600000L
        val expectedReturn: Int get() = (amount * (1.0 + interestRate)).toInt()
    }

    init {
        loadInvestments()
    }

    fun applyInterest(): Int {
        var totalInterest = 0
        val interestRate = plugin.config.getDouble("economy.interest_rate", 0.02)
        val intervalHours = plugin.config.getInt("economy.interest_interval_hours", 24)

        for (player in plugin.server.getOnlinePlayers()) {
            val balance = plugin.economyService.getBalance(player)
            if (balance <= 0) continue
            val uuid = player.uniqueId
            val lastApplied = interestCache.getOrDefault(uuid, 0.0)
            val now = System.currentTimeMillis().toDouble()
            if (now - lastApplied < intervalHours * 3600000.0) continue

            val interest = (balance * interestRate).toInt().coerceAtLeast(1)
            plugin.economyService.deposit(player, interest)
            totalInterest += interest
            interestCache[uuid] = now
        }
        plugin.debug("[Banking] Dobanda platita: $totalInterest monede")
        return totalInterest
    }

    fun invest(player: org.bukkit.entity.Player, amount: Int, durationHours: Int, description: String): Boolean {
        if (amount < 100) return false
        if (plugin.economyService.getBalance(player) < amount) return false
        plugin.economyService.withdraw(player, amount)

        val rate = when {
            durationHours >= 720 -> 0.15
            durationHours >= 168 -> 0.10
            durationHours >= 48 -> 0.05
            else -> 0.02
        }

        val inv = Investment(
            id = "${player.uniqueId}_${System.currentTimeMillis()}",
            amount = amount,
            interestRate = rate,
            createdAt = System.currentTimeMillis(),
            durationHours = durationHours,
            description = description
        )
        investments.getOrPut(player.uniqueId) { mutableListOf() }.add(inv)
        saveInvestment(player.uniqueId, inv)
        return true
    }

    fun collectMatured(player: org.bukkit.entity.Player): Int {
        val list = investments[player.uniqueId] ?: return 0
        val matured = list.filter { it.matured }
        if (matured.isEmpty()) return 0

        val totalReturn = matured.sumOf { it.expectedReturn }
        plugin.economyService.deposit(player, totalReturn)
        list.removeAll(matured)
        for (inv in matured) {
            removeInvestment(player.uniqueId, inv.id)
        }
        return totalReturn
    }

    fun getActiveInvestments(player: UUID): List<Investment> =
        investments.getOrDefault(player, emptyList())

    fun getTotalInvested(player: UUID): Int =
        investments.getOrDefault(player, emptyList()).sumOf { it.amount }

    fun getPendingReturns(player: UUID): Int =
        investments.getOrDefault(player, emptyList())
            .filter { it.matured }.sumOf { it.expectedReturn }

    fun getInvestmentCount(): Int = investments.values.sumOf { it.size }

    private fun saveInvestment(playerUuid: UUID, inv: Investment) {
        try {
            val sql = """
                INSERT OR REPLACE INTO player_investments
                (investment_id, player_uuid, amount, interest_rate, created_at, duration_hours, description)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.setString(1, inv.id)
                stmt.setString(2, playerUuid.toString())
                stmt.setInt(3, inv.amount)
                stmt.setDouble(4, inv.interestRate)
                stmt.setLong(5, inv.createdAt)
                stmt.setInt(6, inv.durationHours)
                stmt.setString(7, inv.description)
                stmt.executeUpdate()
            }
        } catch (e: SQLException) {
            plugin.logger.log(Level.WARNING, "[Banking] Eroare salvare investitie", e)
        }
    }

    private fun removeInvestment(playerUuid: UUID, investmentId: String) {
        try {
            val sql = "DELETE FROM player_investments WHERE investment_id = ?"
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.setString(1, investmentId)
                stmt.executeUpdate()
            }
        } catch (e: SQLException) {
            plugin.logger.log(Level.WARNING, "[Banking] Eroare stergere investitie", e)
        }
    }

    private fun loadInvestments() {
        try {
            val sql = "SELECT * FROM player_investments"
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        val uuid = UUID.fromString(rs.getString("player_uuid"))
                        val inv = Investment(
                            id = rs.getString("investment_id"),
                            amount = rs.getInt("amount"),
                            interestRate = rs.getDouble("interest_rate"),
                            createdAt = rs.getLong("created_at"),
                            durationHours = rs.getInt("duration_hours"),
                            description = rs.getString("description") ?: ""
                        )
                        investments.getOrPut(uuid) { mutableListOf() }.add(inv)
                    }
                }
            }
            plugin.debug("[Banking] Investitii incarcate: ${getInvestmentCount()}")
        } catch (e: SQLException) {
            if (e.message?.contains("no such table") == true) {
                plugin.logger.info("[Banking] Tabela player_investments nu exista — se va crea la prima scriere.")
            } else {
                plugin.logger.log(Level.WARNING, "[Banking] Eroare incarcare investitii", e)
            }
        }
    }
}
