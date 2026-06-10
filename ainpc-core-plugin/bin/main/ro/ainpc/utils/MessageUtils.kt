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

    /**
     * Formateaza timpul in format citibil
     */
    fun formatTime(seconds: Long): String {
        return if (seconds < 60) {
            "$seconds secunde"
        } else if (seconds < 3600) {
            (seconds / 60).toString() + " minute"
        } else if (seconds < 86400) {
            (seconds / 3600).toString() + " ore"
        } else {
            (seconds / 86400).toString() + " zile"
        }
    }

    /**
     * Formateaza un procent (0.0 - 1.0) in text
     */
    fun formatPercentage(value: Double): String = String.format("%.0f%%", value * 100)

    /**
     * Creeaza o bara de progres vizuala
     */
    fun createProgressBar(
        value: Double,
        length: Int,
        filledChar: String,
        emptyChar: String,
        filledColor: String,
        emptyColor: String
    ): String {
        val filled = Math.round(value * length).toInt()
        val empty = length - filled

        val bar = StringBuilder()
        bar.append(filledColor)
        for (i in 0 until filled) {
            bar.append(filledChar)
        }
        bar.append(emptyColor)
        for (i in 0 until empty) {
            bar.append(emptyChar)
        }

        return bar.toString()
    }

    /**
     * Bara de progres default
     */
    fun createProgressBar(value: Double): String {
        return createProgressBar(value, 10, "â–ˆ", "â–‘", "&a", "&7")
    }

    /**
     * Obtine culoarea emotiei
     */
    fun getEmotionColor(emotion: String): String {
        return when (emotion.lowercase()) {
            "happiness", "bucurie" -> "&a"
            "sadness", "tristete" -> "&9"
            "anger", "furie" -> "&c"
            "fear", "frica" -> "&5"
            "surprise", "surpriza" -> "&e"
            "disgust", "dezgust" -> "&2"
            "trust", "incredere" -> "&b"
            "anticipation", "anticipare" -> "&6"
            else -> "&f"
        }
    }

    /**
     * Obtine numele emotiei in romana
     */
    fun getEmotionName(emotion: String): String {
        return when (emotion.lowercase()) {
            "happiness" -> "Bucurie"
            "sadness" -> "Tristete"
            "anger" -> "Furie"
            "fear" -> "Frica"
            "surprise" -> "Surpriza"
            "disgust" -> "Dezgust"
            "trust" -> "Incredere"
            "anticipation" -> "Anticipare"
            else -> emotion
        }
    }

    /**
     * Obtine numele relatiei in romana
     */
    fun getRelationshipName(relationship: String): String {
        return when (relationship.lowercase()) {
            "stranger" -> "Strain"
            "acquaintance" -> "Cunoscut"
            "friend" -> "Prieten"
            "close_friend" -> "Prieten apropiat"
            "best_friend" -> "Cel mai bun prieten"
            "rival" -> "Rival"
            "enemy" -> "Dusman"
            "lover" -> "Iubit/a"
            "spouse" -> "Sot/Sotie"
            else -> relationship
        }
    }

    /**
     * Obtine numele relatiei de familie in romana
     */
    fun getFamilyRelationName(relation: String): String {
        return when (relation.lowercase()) {
            "father" -> "Tata"
            "mother" -> "Mama"
            "son" -> "Fiu"
            "daughter" -> "Fiica"
            "brother" -> "Frate"
            "sister" -> "Sora"
            "spouse" -> "Sot/Sotie"
            "grandfather" -> "Bunic"
            "grandmother" -> "Bunica"
            "uncle" -> "Unchi"
            "aunt" -> "Matusa"
            "cousin" -> "Var/Verisoara"
            else -> relation
        }
    }
}
