package ro.ainpc.gui

import org.bukkit.entity.Player

enum class GuiRole(val id: String, val displayName: String) {
    PLAYER("player", "Jucator"),
    CREATOR("creator", "Creator"),
    ADMIN("admin", "Administrator");

    companion object {
        @JvmStatic
        fun resolve(player: Player): GuiRole {
            if (player.hasPermission("ainpc.admin")) return ADMIN
            if (player.hasPermission("ainpc.creator")) return CREATOR
            return PLAYER
        }

        @JvmStatic
        fun fromId(raw: String?): GuiRole {
            if (raw.isNullOrBlank()) return PLAYER
            return entries.firstOrNull { it.id.equals(raw, ignoreCase = true) } ?: PLAYER
        }
    }
}
