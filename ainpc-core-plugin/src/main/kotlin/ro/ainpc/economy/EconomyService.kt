package ro.ainpc.economy

import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin

class EconomyService(private val plugin: AINPCPlugin) {

    fun getBalance(player: Player): Int {
        return plugin.config.getInt("economy.balances.${player.uniqueId}", 0)
    }

    fun setBalance(player: Player, amount: Int) {
        plugin.config.set("economy.balances.${player.uniqueId}", amount.coerceAtLeast(0))
        plugin.saveConfig()
    }

    fun deposit(player: Player, amount: Int): Boolean {
        if (amount <= 0) return false
        val current = getBalance(player)
        setBalance(player, current + amount)
        return true
    }

    fun withdraw(player: Player, amount: Int): Boolean {
        if (amount <= 0) return false
        val current = getBalance(player)
        if (current < amount) return false
        setBalance(player, current - amount)
        return true
    }

    fun transfer(from: Player, to: Player, amount: Int): Boolean {
        if (withdraw(from, amount)) {
            deposit(to, amount)
            return true
        }
        return false
    }
}
