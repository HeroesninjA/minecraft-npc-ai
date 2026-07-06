package ro.ainpc.economy

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class EconomyService(private val plugin: AINPCPlugin) {
    private val balances: MutableMap<UUID, Int> = ConcurrentHashMap()
    private val balanceFile: File = File(plugin.dataFolder, "economy-balances.json")
    private val gson: Gson = Gson()
    private var dirty: Boolean = false

    init {
        loadBalances()
    }

    fun getBalance(player: Player): Int {
        return balances.getOrDefault(player.uniqueId, 0)
    }

    fun setBalance(player: Player, amount: Int) {
        balances[player.uniqueId] = amount.coerceAtLeast(0)
        dirty = true
    }

    fun deposit(player: Player, amount: Int): Boolean {
        if (amount <= 0) return false
        balances.merge(player.uniqueId, amount) { old, new -> old + new }
        dirty = true
        return true
    }

    fun withdraw(player: Player, amount: Int): Boolean {
        if (amount <= 0) return false
        val current = balances.getOrDefault(player.uniqueId, 0)
        if (current < amount) return false
        balances[player.uniqueId] = current - amount
        dirty = true
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
        try {
            val json = gson.toJson(balances)
            balanceFile.writeText(json)
            dirty = false
        } catch (e: Exception) {
            plugin.logger.warning("Nu am putut salva balantele: ${e.message}")
        }
    }

    private fun loadBalances() {
        try {
            if (balanceFile.exists()) {
                val json = balanceFile.readText()
                val type = object : TypeToken<Map<UUID, Int>>() {}.type
                val loaded: Map<UUID, Int> = gson.fromJson(json, type) ?: emptyMap()
                balances.putAll(loaded)
            }
        } catch (e: Exception) {
            plugin.logger.warning("Nu am putut incarca balantele: ${e.message}")
        }
    }
}
