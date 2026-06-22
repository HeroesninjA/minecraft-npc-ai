package ro.ainpc.economy

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import ro.ainpc.commands.ainpcCommandEconomyPlugin
import ro.ainpc.commands.initAinpcCommandEconomyPlugin

class EconomyTopCommand {
    companion object {
        fun execute(sender: CommandSender, plugin: AINPCPlugin, limit: Int = 10): Boolean {
            val onlinePlayers = plugin.server.onlinePlayers.sortedByDescending {
                plugin.economyService.getBalance(it)
            }.take(limit.coerceIn(1, 20))

            plugin.messageUtils.send(sender, "&6=== Top Economie ===")
            if (onlinePlayers.isEmpty()) {
                plugin.messageUtils.send(sender, "&7Niciun jucator online.")
                return true
            }
            var rank = 1
            for (p in onlinePlayers) {
                val bal = plugin.economyService.getBalance(p)
                plugin.messageUtils.send(sender, "&e#$rank &f${p.name} &7- &e$bal &7monede")
                rank++
            }
            return true
        }
    }
}
