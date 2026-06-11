package ro.ainpc.listeners

import org.bukkit.event.Listener
import ro.ainpc.AINPCPlugin
import ro.ainpc.gui.listeners.GuiInventoryListener

/**
 * Punct unic pentru inregistrarea listener-elor pluginului.
 */
class ListenerRegistry(private val plugin: AINPCPlugin) {
    fun registerAll() {
        register(NPCInteractionListener(plugin))
        register(NPCChatListener(plugin))
        register(QuestObjectiveListener(plugin))
        register(PlayerJoinListener(plugin))
        register(VillagerLifecycleListener(plugin))
        register(MappingWandListener(plugin))
        register(GuiInventoryListener(plugin))
    }

    private fun register(listener: Listener) {
        plugin.server.pluginManager.registerEvents(listener, plugin)
    }
}
