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
import ro.ainpc.api.events.AINPCEventSource
import ro.ainpc.api.events.context.PlayerContextChangedEvent
import ro.ainpc.api.events.context.PlayerContextChangedEventPayload
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Listener pentru progresul obiectivelor de quest bazate pe evenimente.
 */
class QuestObjectiveListener(plugin: AINPCPlugin) : AbstractPluginListener(plugin) {
    private val playerRegions: MutableMap<UUID, String> = ConcurrentHashMap()
    private val playerPlaces: MutableMap<UUID, String> = ConcurrentHashMap()

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerMove(event: PlayerMoveEvent) {
        if (!questFeatureEnabled()) {
            return
        }
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
        publishPlayerContextChanged(event.player, to)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityDeath(event: EntityDeathEvent) {
        if (!questFeatureEnabled()) {
            return
        }
        val killer = event.entity.killer ?: return
        plugin.scenarioEngine.recordMobKill(killer, event.entity)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityPickupItem(event: EntityPickupItemEvent) {
        if (!questFeatureEnabled()) {
            return
        }
        val player = event.entity as? Player ?: return
        refreshInventoryProgressNextTick(player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        if (!questFeatureEnabled()) {
            return
        }
        refreshInventoryProgressNextTick(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onInventoryClick(event: InventoryClickEvent) {
        if (!questFeatureEnabled()) {
            return
        }
        val player = event.whoClicked as? Player ?: return
        refreshInventoryProgressNextTick(player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onInventoryDrag(event: InventoryDragEvent) {
        if (!questFeatureEnabled()) {
            return
        }
        val player = event.whoClicked as? Player ?: return
        refreshInventoryProgressNextTick(player)
    }

    private fun refreshInventoryProgressNextTick(player: Player?) {
        if (player == null) {
            return
        }
        runLater({ plugin.scenarioEngine.recordInventoryChange(player) }, 1L)
    }

    private fun questFeatureEnabled(): Boolean = plugin.config.getBoolean("features.quest", true)

    private fun contextEventsEnabled(): Boolean =
        plugin.config.getBoolean("events.context_events_enabled", false) &&
        plugin.config.getBoolean("events.public_api_enabled", true)

    private fun publishPlayerContextChanged(player: Player, to: Location) {
        if (!contextEventsEnabled()) return

        val playerId = player.uniqueId
        val worldAdmin = plugin.platform?.worldAdminService ?: return
        if (!worldAdmin.isEnabled) return

        val currentRegion = worldAdmin.findRegion(to.world.name, to.blockX, to.blockY, to.blockZ)
        val currentPlace = worldAdmin.findPlace(to.world.name, to.blockX, to.blockY, to.blockZ)
        val currentRegionId = currentRegion?.id()
        val currentPlaceId = currentPlace?.id()

        val previousRegionId = playerRegions[playerId]
        val previousPlaceId = playerPlaces[playerId]

        if (previousRegionId == currentRegionId && previousPlaceId == currentPlaceId) return
        if (previousRegionId == null && previousPlaceId == null) {
            playerRegions[playerId] = currentRegionId ?: ""
            playerPlaces[playerId] = currentPlaceId ?: ""
            return
        }

        playerRegions[playerId] = currentRegionId ?: ""
        playerPlaces[playerId] = currentPlaceId ?: ""

        val event = PlayerContextChangedEvent(
            PlayerContextChangedEventPayload(
                UUID.randomUUID(),
                System.currentTimeMillis(),
                AINPCEventSource.PLAYER,
                playerId,
                player.name,
                previousRegionId.takeIf { it?.isNotBlank() == true },
                previousPlaceId.takeIf { it?.isNotBlank() == true },
                currentRegionId.takeIf { it?.isNotBlank() == true },
                currentPlaceId.takeIf { it?.isNotBlank() == true },
                to.world.name,
                to.x, to.y, to.z,
                mapOf("source" to "QuestObjectiveListener")
            )
        )
        plugin.server.pluginManager.callEvent(event)
    }
}
