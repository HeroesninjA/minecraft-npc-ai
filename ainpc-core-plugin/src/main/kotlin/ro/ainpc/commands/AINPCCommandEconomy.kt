package ro.ainpc.commands

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import ro.ainpc.economy.EconomyTopCommand

lateinit var ainpcCommandEconomyPlugin: AINPCPlugin

fun initAinpcCommandEconomyPlugin(plugin: AINPCPlugin) {
    ainpcCommandEconomyPlugin = plugin
}

fun handleEconomyBalance(sender: CommandSender, args: Array<String>): Boolean {
    val target = if (args.size > 2) {
        ainpcCommandEconomyPlugin.server.getPlayerExact(args[2])
            ?: ainpcCommandEconomyPlugin.server.getPlayer(args[2])
    } else {
        sender as? Player
    }
    if (target == null) {
        ainpcCommandEconomyPlugin.messageUtils.send(sender, "&cJucatorul nu este online sau nu ai specificat un nume.")
        return true
    }
    val balance = ainpcCommandEconomyPlugin.economyService.getBalance(target)
    ainpcCommandEconomyPlugin.messageUtils.send(sender, "&6Balanța &f${target.name}&6: &e$balance &7monede")
    return true
}

fun handleEconomyPay(sender: CommandSender, args: Array<String>): Boolean {
    if (isRuntimeReadOnly(ainpcCommandEconomyPlugin)) {
        ainpcCommandEconomyPlugin.messageUtils.send(sender,
            "&cMCP read_only este activ; economy pay este blocat pana la iesirea din modul read-only.")
        return true
    }

    val player = sender as? Player ?: run {
        ainpcCommandEconomyPlugin.messageUtils.send(sender, "&cAceasta comanda poate fi folosita doar de jucatori.")
        return true
    }
    if (args.size < 4) {
        ainpcCommandEconomyPlugin.messageUtils.send(sender, "&cUtilizare: /ainpc economy pay <jucator> <suma>")
        return true
    }
    val target = ainpcCommandEconomyPlugin.server.getPlayerExact(args[2])
        ?: ainpcCommandEconomyPlugin.server.getPlayer(args[2])
    if (target == null) {
        ainpcCommandEconomyPlugin.messageUtils.send(sender, "&cJucatorul &f${args[2]} &cnu este online.")
        return true
    }
    val amount = args[3].toIntOrNull()
    if (amount == null || amount <= 0) {
        ainpcCommandEconomyPlugin.messageUtils.send(sender, "&cSuma invalida: &f${args[3]}")
        return true
    }
    if (ainpcCommandEconomyPlugin.economyService.transfer(player, target, amount)) {
        ainpcCommandEconomyPlugin.messageUtils.send(sender, "&aAi trimis &e$amount &amonede catre &f${target.name}&a.")
        ainpcCommandEconomyPlugin.messageUtils.send(target, "&aAi primit &e$amount &amonede de la &f${player.name}&a.")
    } else {
        ainpcCommandEconomyPlugin.messageUtils.send(sender, "&cFonduri insuficiente. Ai doar &e${ainpcCommandEconomyPlugin.economyService.getBalance(player)} &7monede.")
    }
    return true
}

fun handleEconomyTop(sender: CommandSender, args: Array<String>): Boolean {
    if (!sender.hasPermission("ainpc.info")) {
        ainpcCommandEconomyPlugin.messageUtils.sendMessage(sender, "no_permission")
        return true
    }
    val limit = if (args.size > 2) (args[2].toIntOrNull() ?: 10).coerceIn(1, 50) else 10
    return EconomyTopCommand.execute(sender, ainpcCommandEconomyPlugin, limit)
}

fun handleEconomySet(sender: CommandSender, args: Array<String>): Boolean {
    if (!sender.hasPermission("ainpc.admin")) {
        ainpcCommandEconomyPlugin.messageUtils.sendMessage(sender, "no_permission")
        return true
    }
    if (isRuntimeReadOnly(ainpcCommandEconomyPlugin)) {
        ainpcCommandEconomyPlugin.messageUtils.send(sender,
            "&cMCP read_only este activ; economy set este blocat pana la iesirea din modul read-only.")
        return true
    }
    if (args.size < 4) {
        ainpcCommandEconomyPlugin.messageUtils.send(sender, "&cUtilizare: /ainpc economy set <jucator> <suma>")
        return true
    }
    val target = ainpcCommandEconomyPlugin.server.getPlayerExact(args[2])
        ?: ainpcCommandEconomyPlugin.server.getPlayer(args[2])
    if (target == null) {
        ainpcCommandEconomyPlugin.messageUtils.send(sender, "&cJucatorul &f${args[2]} &cnu este online.")
        return true
    }
    val amount = args[3].toIntOrNull()
    if (amount == null || amount < 0) {
        ainpcCommandEconomyPlugin.messageUtils.send(sender, "&cSuma invalida: &f${args[3]}")
        return true
    }
    ainpcCommandEconomyPlugin.economyService.setBalance(target, amount)
    ainpcCommandEconomyPlugin.messageUtils.send(sender, "&aBalanța lui &f${target.name} &aeste acum &e$amount &amonede.")
    return true
}
