package ro.ainpc.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import ro.ainpc.AINPCPlugin;
import ro.ainpc.npc.AINPC;

import java.util.List;

/**
 * Listener pentru evenimente de intrare si recunoastere a jucatorului.
 */
public class PlayerJoinListener extends AbstractPluginListener {

    public PlayerJoinListener(AINPCPlugin plugin) {
        super(plugin);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Verifica daca sunt NPC-uri in apropiere care il recunosc pe jucator
        runLater(() -> checkNearbyNPCsRecognition(player), 40L);
    }

    /**
     * Verifica daca NPC-urile din apropiere recunosc jucatorul
     */
    private void checkNearbyNPCsRecognition(Player player) {
        List<AINPC> nearbyNPCs = plugin.getNpcManager().getActiveNPCsNear(player.getLocation(), 20);
        
        for (AINPC npc : nearbyNPCs) {
            plugin.getMemoryManager().getPlayerMemoryStatsAsync(npc, player)
                .thenAccept(stats -> runSync(() -> applyRecognition(player, npc, stats)))
                .exceptionally(ex -> {
                    plugin.getLogger().warning("Nu am putut evalua recunoasterea pentru NPC-ul " + npc.getName()
                        + ": " + ex.getMessage());
                    return null;
                });
        }
    }

    private void applyRecognition(Player player, AINPC npc, ro.ainpc.managers.MemoryManager.MemoryStats stats) {
        if (!stats.hasMemories()) {
            return;
        }

        if (stats.memoryCount() < 5 && Math.abs(stats.emotionalImpact()) <= 0.3) {
            return;
        }

        npc.lookAt(player);

        if (stats.emotionalImpact() > 0) {
            plugin.getEmotionManager().applyEmotion(npc, "happiness", 0.1);
        } else if (stats.emotionalImpact() < -0.2) {
            plugin.getEmotionManager().applyEmotion(npc, "anger", 0.1);
        }
    }
}
