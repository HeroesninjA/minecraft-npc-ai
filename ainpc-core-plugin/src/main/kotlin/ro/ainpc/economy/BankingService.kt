package ro.ainpc.economy

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import ro.ainpc.database.DatabaseManager
import java.math.BigDecimal
import java.sql.PreparedStatement
import java.sql.SQLException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Bankingservice.
 */
class BankingService internal constructor(
    private val databaseManager: DatabaseManager,
    private val economyService: EconomyService,
    private val config: ConfigurationSection,
    private val onlinePlayers: () -> Collection<Player>,
    private val logger: Logger,
    private val debug: (String) -> Unit = {},
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) {
    constructor(plugin: AINPCPlugin) : this(
        plugin.databaseManager,
        plugin.economyService,
        plugin.config,
        { plugin.server.onlinePlayers },
        plugin.logger,
        plugin::debug,
    )

    private val investments: MutableMap<UUID, MutableList<Investment>> = ConcurrentHashMap()
    private val interestCache: MutableMap<UUID, Long> = ConcurrentHashMap()
    private val interestPaymentStore = BankInterestPaymentStore(databaseManager::prepareStatement)
    private var interestPaymentsAvailable: Boolean
    private var investmentsAvailable: Boolean

/**
 * Investment.
 */
    data class Investment(
        val id: String,
        val amount: Int,
        val interestRate: Double,
        val createdAt: Long,
        val durationHours: Int,
        val description: String
    ) {
        val matured: Boolean get() = isMatured(System.currentTimeMillis())
        val expectedReturn: Long = BigDecimal.valueOf(amount.toLong())
            .multiply(BigDecimal.ONE.add(BigDecimal.valueOf(interestRate))).toLong()

        fun isMatured(now: Long): Boolean = now - createdAt >= durationHours * 3600000L
    }

    init {
        interestPaymentsAvailable = loadInterestPayments()
        investmentsAvailable = loadInvestments()
    }

    @Synchronized
    fun applyInterest(): Int {
        if (!interestPaymentsAvailable) {
            interestPaymentsAvailable = loadInterestPayments()
            if (!interestPaymentsAvailable) return 0
        }
        var totalInterest = 0
        val interestRate = config.getDouble("economy.interest_rate", 0.02)
        val intervalHours = config.getInt("economy.interest_interval_hours", 24)
        if (!interestRate.isFinite() || interestRate <= 0.0 || intervalHours <= 0) return 0

        for (player in onlinePlayers()) {
            val balance = economyService.getBalance(player)
            if (balance <= 0) continue
            val uuid = player.uniqueId
            val lastApplied = interestCache.getOrDefault(uuid, 0L)
            val now = currentTimeMillis()
            if (now - lastApplied < intervalHours * 3600000L) continue

            val interest = (balance * interestRate).toInt().coerceAtLeast(1)
            val paid = economyService.depositWithPersistence(uuid, interest) {
                interestPaymentStore.save(uuid, now)
            }
            if (!paid) continue
            totalInterest += interest
            interestCache[uuid] = now
        }
        debug("[Banking] Dobanda platita: $totalInterest monede")
        return totalInterest
    }

    @Synchronized
    fun invest(player: Player, amount: Int, durationHours: Int, description: String): Boolean {
        if (amount < 100) return false
        if (durationHours <= 0) return false
        if (!ensureInvestmentsLoaded()) return false

        val rate = when {
            durationHours >= 720 -> 0.15
            durationHours >= 168 -> 0.10
            durationHours >= 48 -> 0.05
            else -> 0.02
        }

        val inv = Investment(
            id = UUID.randomUUID().toString(),
            amount = amount,
            interestRate = rate,
            createdAt = currentTimeMillis(),
            durationHours = durationHours,
            description = description
        )
        if (inv.expectedReturn > Int.MAX_VALUE) return false
        val invested = economyService.withdrawWithPersistence(player.uniqueId, amount) {
            saveInvestment(player.uniqueId, inv)
        }
        if (!invested) return false
        investments.getOrPut(player.uniqueId) { mutableListOf() }.add(inv)
        return true
    }

    @Synchronized
    fun collectMatured(player: Player): Int {
        if (!ensureInvestmentsLoaded()) return 0
        val list = investments[player.uniqueId] ?: return 0
        val now = currentTimeMillis()
        val matured = list.filter { it.isMatured(now) }
        if (matured.isEmpty()) return 0

        val totalReturn = matured.sumOf { it.expectedReturn }
        if (totalReturn <= 0 || totalReturn > Int.MAX_VALUE) return 0
        val collected = economyService.depositWithPersistence(player.uniqueId, totalReturn.toInt()) {
            for (inv in matured) {
                removeInvestment(player.uniqueId, inv.id)
            }
        }
        if (!collected) return 0
        list.removeAll(matured.toSet())
        if (list.isEmpty()) investments.remove(player.uniqueId)
        return totalReturn.toInt()
    }

    @Synchronized
    fun getActiveInvestments(player: UUID): List<Investment> {
        if (!ensureInvestmentsLoaded()) return emptyList()
        return investments[player]?.toList() ?: emptyList()
    }

    @Synchronized
    fun getTotalInvested(player: UUID): Long {
        if (!ensureInvestmentsLoaded()) return 0L
        return investments[player].orEmpty().sumOf { it.amount.toLong() }
    }

    @Synchronized
    fun getPendingReturns(player: UUID): Long {
        if (!ensureInvestmentsLoaded()) return 0L
        val now = currentTimeMillis()
        return investments[player].orEmpty().sumOf { if (it.isMatured(now)) it.expectedReturn else 0L }
    }

    @Synchronized
    fun getInvestmentCount(): Int {
        if (!ensureInvestmentsLoaded()) return 0
        return investments.values.sumOf { it.size }
    }

    private fun loadInterestPayments(): Boolean {
        return try {
            interestCache.putAll(interestPaymentStore.load())
            true
        } catch (e: SQLException) {
            logger.log(Level.WARNING, "[Banking] Eroare incarcare istoric dobanzi", e)
            false
        } catch (e: IllegalArgumentException) {
            logger.log(Level.WARNING, "[Banking] UUID invalid in istoricul dobanzilor", e)
            false
        }
    }

    private fun saveInvestment(playerUuid: UUID, inv: Investment) {
        val sql = """
            INSERT INTO player_investments
            (investment_id, player_uuid, amount, interest_rate, created_at, duration_hours, description)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()
        databaseManager.prepareStatement(sql).use { stmt ->
            stmt.setString(1, inv.id)
            stmt.setString(2, playerUuid.toString())
            stmt.setInt(3, inv.amount)
            stmt.setDouble(4, inv.interestRate)
            stmt.setLong(5, inv.createdAt)
            stmt.setInt(6, inv.durationHours)
            stmt.setString(7, inv.description)
            if (stmt.executeUpdate() != 1) throw SQLException("Investitia nu a fost salvata")
        }
    }

    private fun removeInvestment(playerUuid: UUID, investmentId: String) {
        val sql = "DELETE FROM player_investments WHERE investment_id = ? AND player_uuid = ?"
        databaseManager.prepareStatement(sql).use { stmt ->
            stmt.setString(1, investmentId)
            stmt.setString(2, playerUuid.toString())
            if (stmt.executeUpdate() != 1) throw SQLException("Investitia nu mai este disponibila pentru colectare")
        }
    }

    private fun ensureInvestmentsLoaded(): Boolean {
        if (!investmentsAvailable) {
            investmentsAvailable = loadInvestments()
        }
        return investmentsAvailable
    }

    private fun loadInvestments(): Boolean {
        return try {
            val loadedInvestments = mutableMapOf<UUID, MutableList<Investment>>()
            val sql = "SELECT * FROM player_investments"
            databaseManager.prepareStatement(sql).use { stmt ->
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
                        require(inv.amount > 0 && inv.interestRate.isFinite() && inv.interestRate in 0.0..1.0 && inv.durationHours > 0) {
                            "Investitie invalida: ${inv.id}"
                        }
                        loadedInvestments.getOrPut(uuid) { mutableListOf() }.add(inv)
                    }
                }
            }
            investments.clear()
            investments.putAll(loadedInvestments)
            debug("[Banking] Investitii incarcate: ${loadedInvestments.values.sumOf { it.size }}")
            true
        } catch (e: SQLException) {
            logger.log(Level.WARNING, "[Banking] Eroare incarcare investitii", e)
            false
        } catch (e: IllegalArgumentException) {
            logger.log(Level.WARNING, "[Banking] Date invalide in investitii", e)
            false
        }
    }
}

internal class BankInterestPaymentStore(private val prepareStatement: (String) -> PreparedStatement) {
    fun load(): Map<UUID, Long> {
        val payments = mutableMapOf<UUID, Long>()
        prepareStatement("SELECT player_uuid, last_applied FROM bank_interest_payments").use { stmt ->
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    payments[UUID.fromString(rs.getString("player_uuid"))] = rs.getLong("last_applied")
                }
            }
        }
        return payments
    }

    fun save(playerUuid: UUID, timestamp: Long) {
        prepareStatement(
            "INSERT OR REPLACE INTO bank_interest_payments (player_uuid, last_applied) VALUES (?, ?)"
        ).use { stmt ->
            stmt.setString(1, playerUuid.toString())
            stmt.setLong(2, timestamp)
            stmt.executeUpdate()
        }
    }
}
