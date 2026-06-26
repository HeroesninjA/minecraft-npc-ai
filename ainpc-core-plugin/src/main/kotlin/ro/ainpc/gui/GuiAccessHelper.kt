package ro.ainpc.gui

import org.bukkit.entity.Player

object GuiAccessHelper {
    @JvmStatic
    fun roleOf(player: Player): GuiRole = GuiRole.resolve(player)

    @JvmStatic
    fun isAdmin(player: Player): Boolean = player.hasPermission("ainpc.admin")

    @JvmStatic
    fun isCreator(player: Player): Boolean = player.hasPermission("ainpc.creator")

    @JvmStatic
    fun isPlayer(player: Player): Boolean = !isAdmin(player) && !isCreator(player)

    @JvmStatic
    fun canAccess(player: Player, target: GuiKey, service: GuiService): Boolean = service.canOpen(player, target)

    @JvmStatic
    fun adminOrCreator(player: Player): Boolean = isAdmin(player) || isCreator(player)

    @JvmStatic
    fun adminOrPermission(player: Player, permission: String): Boolean =
        isAdmin(player) || player.hasPermission(permission)

    @JvmStatic
    fun accessDeniedLore(): List<String> = listOf("&8Necesita permisiune superioara.")
}
