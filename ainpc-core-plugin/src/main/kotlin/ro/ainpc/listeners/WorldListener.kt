package ro.ainpc.listeners

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.WorldLoadEvent
import org.bukkit.event.world.WorldUnloadEvent
import ro.ainpc.AINPCPlugin

class WorldListener(private val plugin: AINPCPlugin) : Listener {

    @EventHandler
    fun onWorldLoad(event: WorldLoadEvent) {
        val worldName = event.world.name
        plugin.platform.worldAdminService.refreshIndexForWorld(worldName)
        plugin.debug("Lume incarcata: $worldName — index mapping reîmprospatat")
    }

    @EventHandler
    fun onWorldUnload(event: WorldUnloadEvent) {
        val worldName = event.world.name
        plugin.environmentEngine.onWorldUnload(worldName)
        plugin.platform.worldAdminService.refreshIndexForWorld(worldName)
        plugin.debug("Lume descarcata: $worldName — index mapping curatat")
    }
}
