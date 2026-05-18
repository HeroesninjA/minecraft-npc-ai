package ro.ainpc.gui.listeners

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.PlayerQuitEvent
import ro.ainpc.AINPCPlugin
import ro.ainpc.gui.AINPCGuiHolder

class GuiInventoryListener(
    private val plugin: AINPCPlugin
) : Listener {
    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val topInventory = event.view.topInventory
        val holder = topInventory.holder
        if (holder !is AINPCGuiHolder) {
            return
        }

        event.isCancelled = true
        val player = event.whoClicked
        if (player !is Player) {
            return
        }
        if (event.rawSlot < 0 || event.rawSlot >= topInventory.size) {
            return
        }

        plugin.guiService.handleClick(
            player,
            holder.sessionId(),
            event.rawSlot,
            event.click,
            event.action
        )
    }

    @EventHandler
    fun onInventoryDrag(event: InventoryDragEvent) {
        val topInventory = event.view.topInventory
        if (topInventory.holder is AINPCGuiHolder) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val holder = event.inventory.holder
        if (holder is AINPCGuiHolder) {
            plugin.guiService.sessions().close(holder.sessionId())
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        plugin.guiService.clearPlayerState(event.player.uniqueId)
    }
}
