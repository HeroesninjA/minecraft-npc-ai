package ro.ainpc.listeners

import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerMoveEvent
import ro.ainpc.AINPCPlugin

/**
 * Listener pentru progresul obiectivelor de quest bazate pe evenimente.
 */
class QuestObjectiveListener(plugin: AINPCPlugin) : AbstractPluginListener(plugin) {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerMove(event: PlayerMoveEvent) {
        val from: Location = event.from
        val to: Location = event.to
        if (from.world == null || to.world == null) {
            return
        }

        val sameBlock = from.world == to.world &&
            from.blockX == to.blockX &&
            from.blockY == to.blockY &&
            from.blockZ == to.blockZ
        if (sameBlock) {
            return
        }

        plugin.scenarioEngine.recordRegionVisit(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityDeath(event: EntityDeathEvent) {
        val killer = event.entity.killer ?: return
        plugin.scenarioEngine.recordMobKill(killer, event.entity)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityPickupItem(event: EntityPickupItemEvent) {
        val player = event.entity as? Player ?: return
        refreshInventoryProgressNextTick(player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        refreshInventoryProgressNextTick(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        refreshInventoryProgressNextTick(player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onInventoryDrag(event: InventoryDragEvent) {
        val player = event.whoClicked as? Player ?: return
        refreshInventoryProgressNextTick(player)
    }

    private fun refreshInventoryProgressNextTick(player: Player?) {
        if (player == null) {
            return
        }
        runLater({ plugin.scenarioEngine.recordInventoryChange(player) }, 1L)
    }
}
