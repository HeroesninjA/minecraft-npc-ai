package ro.ainpc.listeners

import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import ro.ainpc.AINPCPlugin
import ro.ainpc.world.mapping.MappingPoint
import ro.ainpc.world.mapping.MappingWandMode

class MappingWandListener(plugin: AINPCPlugin) : AbstractPluginListener(plugin) {
    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) {
            return
        }
        val player = event.player
        val service = plugin.mappingWandService
        if (service == null || !service.isWandItem(event.item)) {
            return
        }
        if (!player.hasPermission("ainpc.admin")) {
            messages().sendMessage(player, "no_permission")
            return
        }

        val clickedBlock = event.clickedBlock
        if (clickedBlock == null
            || (event.action != Action.LEFT_CLICK_BLOCK && event.action != Action.RIGHT_CLICK_BLOCK)
        ) {
            return
        }

        event.isCancelled = true
        val point = toPoint(clickedBlock.location)
        val mode = service.mode(player.uniqueId)
        if (mode == MappingWandMode.NODE
            || mode == MappingWandMode.NPC_BIND
            || mode == MappingWandMode.QUEST_ANCHOR
            || player.isSneaking
        ) {
            val session = service.setPoint(player, point)
            messages().send(player, "&aWand point setat: &f" + point.format())
            messages().send(player, "&7Draft: &f/ainpc map " + mode.id() + " <descriere>")
            service.showSelectionPreview(player, session)
            return
        }

        val session = if (event.action == Action.LEFT_CLICK_BLOCK) {
            val value = service.setPos1(player, point)
            messages().send(player, "&aWand pos1 setat: &f" + point.format())
            value
        } else {
            val value = service.setPos2(player, point)
            messages().send(player, "&aWand pos2 setat: &f" + point.format())
            value
        }
        messages().send(player, "&7Draft: &f/ainpc map " + mode.id() + " <descriere>")
        service.showSelectionPreview(player, session)
    }

    private fun toPoint(location: Location): MappingPoint {
        return MappingPoint(
            location.world?.name ?: "",
            location.blockX,
            location.blockY,
            location.blockZ
        )
    }
}
