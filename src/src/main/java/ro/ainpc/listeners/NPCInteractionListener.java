package ro.ainpc.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import ro.ainpc.AINPCPlugin;
import ro.ainpc.ai.DialogManager;
import ro.ainpc.ai.OpenAIService;
import ro.ainpc.engine.ScenarioEngine;
import ro.ainpc.npc.AINPC;

/**
 * Listener pentru interactiunile cu NPC-urile
 */
public class NPCInteractionListener extends AbstractPluginListener {

    private final DialogManager dialogManager;

    public NPCInteractionListener(AINPCPlugin plugin) {
        super(plugin);
        this.dialogManager = plugin.getDialogManager();
    }

    /**
     * Cand un jucator da click dreapta pe un NPC
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        
        // Verifica daca e un Villager (baza pentru NPC-uri)
        if (!(entity instanceof Villager)) return;
        
        AINPC npc = plugin.getNpcManager().getNPCByEntity(entity);
        if (npc == null) {
            npc = plugin.getNpcManager().ensureVillagerIsNPC((Villager) entity);
        }
        if (npc == null) return;
        
        // Anuleaza interactiunea default
        event.setCancelled(true);
        
        Player player = event.getPlayer();
        
        // Verifica distanta
        if (!npc.isInRange(player)) {
            messages().sendMessage(player, "too_far");
            return;
        }
        
        // Face NPC-ul sa se uite la jucator
        npc.lookAt(player);
        npc.updateContext();
        npc.getContext().setInteractingPlayer(player);

        ScenarioEngine.QuestInteractionResult questInteraction = plugin.getScenarioEngine().handleQuestInteraction(player, npc);
        if (questInteraction.isHandled()) {
            if (questInteraction.shouldOpenConversation()) {
                openOrRefreshConversation(player, npc);
            }

            for (String npcMessage : questInteraction.getNpcMessages()) {
                messages().sendNPCMessage(player, npc.getName(), npcMessage);
            }
            for (String systemMessage : questInteraction.getSystemMessages()) {
                messages().send(player, systemMessage);
            }
            if (questInteraction.shouldOpenConversation()) {
                messages().send(player, "&7&o(Scrie in chat pentru a vorbi cu " + npc.getName() + ". Scrie 'pa' pentru a termina conversatia.)");
            }

            plugin.getEmotionManager().processEvent(npc, "player_approach", 1.0);
            return;
        }
        
        // Activeaza conversatia
        startConversation(player, npc);
    }

    /**
     * Incepe o conversatie cu un NPC
     */
    private void startConversation(Player player, AINPC npc) {
        beginConversationSession(player, npc)
            .thenCompose(firstMeeting -> {
                if (firstMeeting) {
                    return java.util.concurrent.CompletableFuture.completedFuture(getFirstMeetingGreeting(npc));
                }
                return dialogManager.getRelationshipAsync(npc, player)
                    .thenApply(relationship -> getReturningGreeting(npc, player, relationship));
            })
            .thenAccept(greeting -> runSync(() -> {
                messages().sendNPCMessage(player, npc.getName(), greeting);
                messages().send(player, "&7&o(Scrie in chat pentru a vorbi cu " + npc.getName() + ". Scrie 'pa' pentru a termina conversatia.)");
                plugin.getEmotionManager().processEvent(npc, "player_approach", 1.0);
            }))
            .exceptionally(ex -> {
                runSync(() -> {
                    plugin.getLogger().warning("Nu am putut initializa conversatia cu " + npc.getName() + ": " + ex.getMessage());
                    messages().sendMessage(player, "ai_error");
                });
                return null;
            });
    }

    private void openOrRefreshConversation(Player player, AINPC npc) {
        AINPC currentPartner = conversations().getConversationPartner(player);
        if (currentPartner != null && currentPartner.getUuid().equals(npc.getUuid())) {
            refreshConversationSession(player);
            return;
        }

        beginConversationSession(player, npc).exceptionally(ex -> {
            plugin.getLogger().warning("Nu am putut reactualiza sesiunea pentru " + npc.getName()
                + ": " + ex.getMessage());
            return false;
        });
    }

    private String getFirstMeetingGreeting(AINPC npc) {
        double extraversion = npc.getPersonality().getExtraversion();
        
        if (extraversion > 0.7) {
            return "Buna ziua, straiin! Ce te aduce pe aici? Eu sunt " + npc.getName() + "!";
        } else if (extraversion > 0.4) {
            return "Salut. Nu cred ca ne-am mai intalnit. Eu sunt " + npc.getName() + ".";
        } else {
            return "Hm? Ah... salut. Sunt " + npc.getName() + ".";
        }
    }

    private String getReturningGreeting(AINPC npc, Player player, OpenAIService.NPCRelationship relationship) {
        double affection = 0;
        if (relationship != null) {
            affection = relationship.getAffection();
        }

        if (affection > 0.6) {
            return "Ce bucurie sa te vad din nou, " + player.getName() + "! Cum mai esti?";
        } else if (affection > 0.3) {
            return "A, " + player.getName() + "! Bine ai revenit.";
        } else if (affection > 0) {
            return "Te-ai intors, " + player.getName() + ". Ce mai vrei?";
        } else if (affection > -0.3) {
            return "Tu iar? Ce vrei?";
        } else {
            return "*oftez* Ce vrei de la mine?";
        }
    }

}
