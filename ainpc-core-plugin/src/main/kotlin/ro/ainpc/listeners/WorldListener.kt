package ro.ainpc.listeners

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.WorldUnloadEvent
import ro.ainpc.AINPCPlugin

class WorldListener(private val plugin: AINPCPlugin) : Listener {

    @EventHandler
    fun onWorldUnload(event: WorldUnloadEvent) {
        plugin.environmentEngine.onWorldUnload(event.world.name)
    }
}
