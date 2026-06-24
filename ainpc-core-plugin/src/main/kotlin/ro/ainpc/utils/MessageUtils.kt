package ro.ainpc.utils

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin

class MessageUtils(private val plugin: AINPCPlugin) {
    private val miniMessage: MiniMessage = MiniMessage.miniMessage()
    private val legacySerializer: LegacyComponentSerializer = LegacyComponentSerializer.legacyAmpersand()

    /**
     * Trimite un mesaj din configuratie catre un jucator
     */
    fun sendMessage(sender: CommandSender, messageKey: String) {
        sendMessage(sender, messageKey, mapOf())
    }

    /**
     * Trimite un mesaj din configuratie cu placeholdere
     */
    fun sendMessage(sender: CommandSender, messageKey: String, placeholders: Map<String, String>) {
        var message = plugin.config.getString("messages.$messageKey", messageKey) ?: messageKey
        message = replacePlaceholders(message, placeholders)
        sender.sendMessage(colorize(message))
    }

    /**
     * Trimite un mesaj custom catre un jucator
     */
    fun send(sender: CommandSender, message: String) {
        sender.sendMessage(colorize(message))
    }

    fun sendActionBar(player: Player?, message: String?) {
        if (player == null || message.isNullOrBlank()) {
            return
        }
        player.sendActionBar(colorize(message))
    }

    /**
     * Trimite un mesaj de la un NPC catre un jucator
     */
    fun sendNPCMessage(player: Player, npcName: String, message: String) {
        val prefix = plugin.config.getString("dialog.prefix", "&6[NPC] &e")
        val showName = plugin.config.getBoolean("dialog.show_name", true)

        val fullMessage = if (showName) {
            prefix + "&f" + npcName + "&7: &f" + message
        } else {
            prefix + message
        }

        player.sendMessage(colorize(fullMessage))
    }

    /**
     * Coloreaza un mesaj folosind coduri & si MiniMessage
     */
    fun colorize(message: String): Component {
        // Converteste codurile & in componente
        return legacySerializer.deserialize(message)
    }

    /**
     * Inlocuieste placeholderele in mesaj
     */
    fun replacePlaceholders(message: String, placeholders: Map<String, String>): String {
        var result = message
        for ((key, value) in placeholders) {
            result = result.replace("%$key%", value)
        }
        return result
    }

}
