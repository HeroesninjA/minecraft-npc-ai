@file:JvmName("AINPCCommandReputation")

package ro.ainpc.commands

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import java.util.Locale
import java.util.UUID

lateinit var ainpcCommandReputationPlugin: AINPCPlugin

fun initAinpcCommandReputationPlugin(plugin: AINPCPlugin) {
    ainpcCommandReputationPlugin = plugin
}

private const val REPUTATION_PLAYER_DEFAULT_LIMIT = 8
private const val REPUTATION_TOP_MAX_LIMIT = 50

fun handleReputationPlayer(
    sender: CommandSender,
    args: Array<String>,
    findOnlinePlayer: (String) -> Player?,
): Boolean {
    if (!sender.hasPermission("ainpc.admin") && !sender.hasPermission("ainpc.quest")) {
        ainpcCommandReputationPlugin.messageUtils.sendMessage(sender, "no_permission")
        return true
    }
    if (args.size < 3 || args.size > 4) {
        ainpcCommandReputationPlugin.messageUtils.send(sender, "&cUtilizare: /ainpc reputation player <jucator> [limit]")
        return true
    }
    val targetInput = args[2]
    val limit = if (args.size == 4) parseReputationLimit(sender, args[3]) else REPUTATION_PLAYER_DEFAULT_LIMIT
    val target = findOnlinePlayer(targetInput)
    val resolvedUuid = if (target != null) {
        target.uniqueId.toString()
    } else {
        tryParseUuidOrNullReputation(targetInput) ?: run {
            ainpcCommandReputationPlugin.messageUtils.send(sender, "&cJucatorul &f$targetInput&c nu este online si nu pare a fi un UUID valid.")
            return true
        }
    }
    val displayName = target?.name ?: targetInput
    ainpcCommandReputationPlugin.messageUtils.send(sender, "&6=== Reputatie jucator: $displayName ===")
    val rows = try {
        ainpcCommandReputationPlugin.reputationService.getTopReputations("player", resolvedUuid, 1)
    } catch (e: Exception) {
        ainpcCommandReputationPlugin.messageUtils.send(sender, "&cEroare la citirea reputatiei: ${e.message}")
        return true
    }
    if (rows.isEmpty()) {
        ainpcCommandReputationPlugin.messageUtils.send(sender, "&7Nicio intrare de reputatie persistata pentru acest jucator.")
        return true
    }
    val allScopes = LinkedHashMap<String, Int>()
    val groupedScopes = loadAllReputationScopesForPlayer(resolvedUuid)
    if (groupedScopes.isEmpty()) {
        ainpcCommandReputationPlugin.messageUtils.send(sender, "&7Nicio intrare de reputatie persistata pentru acest jucator.")
        return true
    }
    for ((scope, value) in groupedScopes) {
        allScopes[scope] = value
    }
    val sortedScopes = allScopes.entries.sortedByDescending { it.value }
    ainpcCommandReputationPlugin.messageUtils.send(sender,
        "&7Total &f${sortedScopes.size}&7 scopuri; se afiseaza maxim &f$limit&7.")
    val displayCount = minOf(limit, sortedScopes.size)
    for (index in 0 until displayCount) {
        val entry = sortedScopes[index]
        val sign = if (entry.value >= 0) "&a" else "&c"
        ainpcCommandReputationPlugin.messageUtils.send(sender,
            "&7- ${entry.key} &7= $sign${entry.value}")
    }
    if (sortedScopes.size > displayCount) {
        ainpcCommandReputationPlugin.messageUtils.send(sender,
            "&7... inca &f${sortedScopes.size - displayCount}&7 scopuri. Mareste limitul pentru mai multe.")
    }
    return true
}

fun handleReputationTop(sender: CommandSender, args: Array<String>): Boolean {
    if (!sender.hasPermission("ainpc.admin")) {
        ainpcCommandReputationPlugin.messageUtils.sendMessage(sender, "no_permission")
        return true
    }
    if (args.size < 4 || args.size > 5) {
        ainpcCommandReputationPlugin.messageUtils.send(sender, "&cUtilizare: /ainpc reputation top <scope_type> <scope_id> [limit]")
        return true
    }
    val scopeType = args[2].trim().lowercase(Locale.ROOT)
    val scopeId = args[3].trim()
    if (scopeType.isBlank() || scopeId.isBlank()) {
        ainpcCommandReputationPlugin.messageUtils.send(sender, "&cscope_type si scope_id sunt obligatorii.")
        return true
    }
    val limit = if (args.size == 5) parseReputationLimit(sender, args[4]) else REPUTATION_PLAYER_DEFAULT_LIMIT
    val entries = try {
        ainpcCommandReputationPlugin.reputationService.getTopReputations(scopeType, scopeId, limit)
    } catch (e: Exception) {
        ainpcCommandReputationPlugin.messageUtils.send(sender, "&cEroare la citirea top reputatii: ${e.message}")
        return true
    }
    ainpcCommandReputationPlugin.messageUtils.send(sender, "&6=== Top reputatie [$scopeType:$scopeId] ===")
    ainpcCommandReputationPlugin.messageUtils.send(sender, "&7Afisate: &f${entries.size} &7(jucatori unici)")
    if (entries.isEmpty()) {
        ainpcCommandReputationPlugin.messageUtils.send(sender, "&7Nicio reputatie persistata pentru acest scop.")
        return true
    }
    for ((index, entry) in entries.withIndex()) {
        val position = index + 1
        val sign = if (entry.reputation >= 0) "&a" else "&c"
        ainpcCommandReputationPlugin.messageUtils.send(sender,
            "&7#$position &f${entry.playerName} &7= $sign${entry.reputation}")
    }
    return true
}

fun handleReputation(
    sender: CommandSender,
    args: Array<String>,
    findOnlinePlayer: (String) -> Player?,
): Boolean {
    if (args.size < 2 || isHelpMode(args[1])) {
        sendReputationUsage(sender)
        return true
    }
    val mode = args[1].lowercase(Locale.ROOT)
    return when (mode) {
        "player", "players" -> handleReputationPlayer(sender, args, findOnlinePlayer)
        "top" -> handleReputationTop(sender, args)
        else -> {
            sendReputationUsage(sender)
            true
        }
    }
}

private fun sendReputationUsage(sender: CommandSender) {
    ainpcCommandReputationPlugin.messageUtils.send(sender, "&6=== Comenzi reputatie ===")
    ainpcCommandReputationPlugin.messageUtils.send(sender, "&e/ainpc reputation player <jucator> [limit] &7- inspectie reputatie jucator")
    ainpcCommandReputationPlugin.messageUtils.send(sender, "&e/ainpc reputation top <scope_type> <scope_id> [limit] &7- top jucatori pentru un scop")
}

private fun parseReputationLimit(sender: CommandSender, raw: String): Int {
    val value = raw.toIntOrNull() ?: return run {
        ainpcCommandReputationPlugin.messageUtils.send(sender, "&cLimit invalid; folosesc default $REPUTATION_PLAYER_DEFAULT_LIMIT.")
        REPUTATION_PLAYER_DEFAULT_LIMIT
    }
    if (value <= 0) {
        ainpcCommandReputationPlugin.messageUtils.send(sender, "&cLimit trebuie sa fie pozitiv; folosesc default $REPUTATION_PLAYER_DEFAULT_LIMIT.")
        return REPUTATION_PLAYER_DEFAULT_LIMIT
    }
    if (value > REPUTATION_TOP_MAX_LIMIT) {
        ainpcCommandReputationPlugin.messageUtils.send(sender, "&eLimit maxim: $REPUTATION_TOP_MAX_LIMIT; trunchiez.")
        return REPUTATION_TOP_MAX_LIMIT
    }
    return value
}

fun tryParseUuidOrNullReputation(value: String): String? {
    return try {
        UUID.fromString(value).toString()
    } catch (_: IllegalArgumentException) {
        null
    }
}

private fun loadAllReputationScopesForPlayer(playerUuid: String): Map<String, Int> {
    val result = LinkedHashMap<String, Int>()
    val databaseManager = ainpcCommandReputationPlugin.databaseManager ?: return result
    val sql = "SELECT scope_type, scope_id, reputation FROM player_reputation WHERE player_uuid = ? ORDER BY scope_type, scope_id"
    return try {
        databaseManager.prepareStatement(sql).use { stmt ->
            stmt.setString(1, playerUuid)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    val scopeType = rs.getString("scope_type") ?: continue
                    val scopeId = rs.getString("scope_id") ?: continue
                    val value = rs.getInt("reputation")
                    result["$scopeType:$scopeId"] = value
                }
            }
        }
        result
    } catch (_: Exception) {
        result
    }
}
