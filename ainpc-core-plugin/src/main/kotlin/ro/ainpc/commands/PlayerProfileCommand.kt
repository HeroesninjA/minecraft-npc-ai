package ro.ainpc.commands

import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import ro.ainpc.api.PermissionNode
import ro.ainpc.ai.PlayerProfile
import ro.ainpc.ai.PlayerProfileRepository
import ro.ainpc.ai.PlayerRole
import ro.ainpc.ai.PlayerStyle
import ro.ainpc.ai.PlayerTrait
import java.util.Locale

internal object PlayerProfileCommand {
    fun handle(plugin: AINPCPlugin, sender: CommandSender, args: Array<String>): Boolean {
        val player = sender as? Player ?: run {
            plugin.messageUtils.send(sender, "&cProfilul poate fi editat doar de jucatorul conectat.")
            return true
        }
        if (!player.hasPermission(PermissionNode.PROFILE.node)) {
            plugin.messageUtils.send(player, "&cNu ai permisiunea ainpc.profile.")
            return true
        }
        val action = args.getOrNull(1)?.lowercase(Locale.ROOT) ?: "show"
        val value = args.getOrNull(2)
        if (!validInput(action, value, args.size)) {
            plugin.messageUtils.send(player, "&e/ainpc profile [show|rol <calator|comerciant|mestesugar|explorator>|stil <direct|prietenos|formal>|trasatura <curios|prudent|generos>|reset]")
            return true
        }
        val playerUuid = player.uniqueId
        plugin.databaseManager.supplyAsync {
            val repository = PlayerProfileRepository(plugin.databaseManager)
            when (action) {
                "reset" -> {
                    repository.delete(playerUuid)
                    null
                }
                "rol" -> (repository.find(playerUuid) ?: PlayerProfile()).copy(role = PlayerRole.parse(value!!))
                    .also { repository.save(playerUuid, it) }
                "stil" -> (repository.find(playerUuid) ?: PlayerProfile()).copy(style = PlayerStyle.parse(value!!))
                    .also { repository.save(playerUuid, it) }
                "trasatura" -> (repository.find(playerUuid) ?: PlayerProfile()).copy(trait = PlayerTrait.parse(value!!))
                    .also { repository.save(playerUuid, it) }
                else -> repository.find(playerUuid)
            }
        }.whenComplete { profile, error ->
            Bukkit.getScheduler().runTask(plugin, Runnable {
                if (!player.isOnline) return@Runnable
                if (error != null) {
                    plugin.logger.warning("Eroare profil jucator: ${error.message}")
                    plugin.messageUtils.send(player, "&cProfilul nu a putut fi salvat sau citit.")
                    return@Runnable
                }
                if (action == "reset") plugin.messageUtils.send(player, "&aProfilul declarat a fost resetat.")
                else {
                    val current = profile ?: PlayerProfile()
                    plugin.messageUtils.send(
                        player,
                        "&eProfilul tau: &frol=${current.role?.label ?: "-"}, " +
                            "stil=${current.style?.label ?: "-"}, trasatura=${current.trait?.label ?: "-"}"
                    )
                }
            })
        }
        return true
    }

    internal fun validInput(action: String, value: String?, argumentCount: Int): Boolean = when (action) {
        "show" -> argumentCount == 1 || argumentCount == 2
        "reset" -> argumentCount == 2
        "rol" -> argumentCount == 3 && value != null && PlayerRole.parse(value) != null
        "stil" -> argumentCount == 3 && value != null && PlayerStyle.parse(value) != null
        "trasatura" -> argumentCount == 3 && value != null && PlayerTrait.parse(value) != null
        else -> false
    }
}
