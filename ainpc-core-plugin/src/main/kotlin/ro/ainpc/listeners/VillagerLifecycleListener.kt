package ro.ainpc.listeners

import org.bukkit.entity.Villager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.VillagerCareerChangeEvent
import org.bukkit.event.world.ChunkLoadEvent
import ro.ainpc.AINPCPlugin

/**
 * Sincronizeaza villagerii noi sau incarcati din chunk cu sistemul de NPC-uri.
 */
class VillagerLifecycleListener(plugin: AINPCPlugin) : AbstractPluginListener(plugin) {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onVillagerSpawn(event: CreatureSpawnEvent) {
        val villager = event.entity as? Villager ?: return

        plugin.npcManager.ensureVillagerIsNPC(villager)
        runLater({ plugin.npcManager.refreshVillagerProfile(villager) }, 60L)

        if (event.spawnReason != CreatureSpawnEvent.SpawnReason.CUSTOM) {
            runLater({ plugin.npcManager.rebalanceVillagePopulation(villager.location.chunk) }, 80L)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onChunkLoad(event: ChunkLoadEvent) {
        runLater({
            for (entity in event.chunk.entities) {
                val villager = entity as? Villager ?: continue
                plugin.npcManager.ensureVillagerIsNPC(villager)
                plugin.npcManager.refreshVillagerProfile(villager)
            }

            plugin.npcManager.restoreNPCsForChunk(event.chunk)
            plugin.npcManager.rebalanceVillagePopulation(event.chunk)
        }, 1L)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onVillagerCareerChange(event: VillagerCareerChangeEvent) {
        val villager = event.entity
        runLater({ plugin.npcManager.refreshVillagerProfile(villager) }, 1L)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onVillagerDeath(event: EntityDeathEvent) {
        val villager = event.entity as? Villager ?: return
        plugin.npcManager.handleEntityDeath(villager)
    }
}
