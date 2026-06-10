package ro.ainpc.listeners

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerJoinEvent
import ro.ainpc.AINPCPlugin
import ro.ainpc.npc.AINPC

/**
 * Listener pentru evenimente de intrare si recunoastere a jucatorului.
 */
class PlayerJoinListener(plugin: AINPCPlugin) : AbstractPluginListener(plugin) {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        // Verifica daca sunt NPC-uri in apropiere care il recunosc pe jucator
        runLater(Runnable { checkNearbyNPCsRecognition(player) }, 40L)
    }

    /**
     * Verifica daca NPC-urile din apropiere recunosc jucatorul
     */
    private fun checkNearbyNPCsRecognition(player: Player) {
        val nearbyNPCs = plugin.npcManager.getActiveNPCsNear(player.location, 20.0)

        for (npc in nearbyNPCs) {
            plugin.memoryManager.getPlayerMemoryStatsAsync(npc, player)
                .thenAccept { stats -> runSync(Runnable { applyRecognition(player, npc, stats) }) }
                .exceptionally { ex ->
                    plugin.logger.warning(
                        "Nu am putut evalua recunoasterea pentru NPC-ul " + npc.name +
                            ": " + ex.message
                    )
                    null
                }
        }
    }

    private fun applyRecognition(player: Player, npc: AINPC, stats: ro.ainpc.managers.MemoryManager.MemoryStats) {
        if (!stats.hasMemories()) {
            return
        }

        if (stats.memoryCount() < 5 && kotlin.math.abs(stats.emotionalImpact()) <= 0.3) {
            return
        }

        npc.lookAt(player)

        if (stats.emotionalImpact() > 0) {
            plugin.emotionManager.applyEmotion(npc, "happiness", 0.1)
        } else if (stats.emotionalImpact() < -0.2) {
            plugin.emotionManager.applyEmotion(npc, "anger", 0.1)
        }
    }
}
