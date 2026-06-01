package ro.ainpc.listeners

import org.bukkit.entity.Player
import org.bukkit.entity.Villager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerInteractEntityEvent
import ro.ainpc.AINPCPlugin
import ro.ainpc.ai.DialogManager
import ro.ainpc.ai.NPCRelationship
import ro.ainpc.engine.ScenarioEngine
import ro.ainpc.npc.AINPC
import java.util.concurrent.CompletableFuture

/**
 * Listener pentru interactiunile cu NPC-urile
 */
class NPCInteractionListener(plugin: AINPCPlugin) : AbstractPluginListener(plugin) {
    private val dialogManager: DialogManager = plugin.dialogManager

    /**
     * Cand un jucator da click dreapta pe un NPC
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        val entity = event.rightClicked

        // Verifica daca e un Villager (baza pentru NPC-uri)
        if (entity !is Villager) return

        var npc = plugin.npcManager.getNPCByEntity(entity)
        if (npc == null) {
            npc = plugin.npcManager.ensureVillagerIsNPC(entity)
        }
        if (npc == null) return

        plugin.npcManager.refreshVillagerProfile(entity)
        npc = plugin.npcManager.getNPCByEntity(entity)
        if (npc == null) return

        // Anuleaza interactiunea default
        event.isCancelled = true

        val player = event.player

        // Verifica distanta
        if (!npc.isInRange(player)) {
            messages().sendMessage(player, "too_far")
            return
        }

        // Face NPC-ul sa se uite la jucator
        npc.lookAt(player)
        npc.updateContext()
        npc.context.setInteractingPlayer(player)
        if (questFeatureEnabled()) {
            plugin.scenarioEngine.recordNpcConversation(player, npc)

            val questInteraction: ScenarioEngine.QuestInteractionResult = plugin.scenarioEngine.handleQuestInteraction(player, npc)
            if (questInteraction.isHandled) {
                if (questInteraction.shouldOpenConversation()) {
                    openOrRefreshConversation(player, npc)
                }

                for (npcMessage in questInteraction.npcMessages) {
                    messages().sendNPCMessage(player, npc.name, npcMessage)
                }
                for (systemMessage in questInteraction.systemMessages) {
                    messages().send(player, systemMessage)
                }
                if (questInteraction.shouldOpenConversation()) {
                    messages().send(player, "&7&o(Scrie in chat pentru a vorbi cu " + npc.name + ". Scrie 'pa' pentru a termina conversatia.)")
                }

                plugin.emotionManager.processEvent(npc, "player_approach", 1.0)
                return
            }
        }

        // Activeaza conversatia
        startConversation(player, npc)
    }

    /**
     * Incepe o conversatie cu un NPC
     */
    private fun startConversation(player: Player, npc: AINPC) {
        beginConversationSession(player, npc)
            .thenCompose { firstMeeting ->
                if (firstMeeting) {
                    return@thenCompose CompletableFuture.completedFuture(getFirstMeetingGreeting(npc))
                }
                dialogManager.getRelationshipAsync(npc, player)
                    .thenApply { relationship -> getReturningGreeting(npc, player, relationship) }
            }
            .thenAccept { greeting ->
                runSync {
                    messages().sendNPCMessage(player, npc.name, greeting)
                    messages().send(player, "&7&o(Scrie in chat pentru a vorbi cu " + npc.name + ". Scrie 'pa' pentru a termina conversatia.)")
                    plugin.emotionManager.processEvent(npc, "player_approach", 1.0)
                }
            }
            .exceptionally { ex ->
                runSync {
                    plugin.logger.warning("Nu am putut initializa conversatia cu " + npc.name + ": " + ex.message)
                    messages().sendMessage(player, "ai_error")
                }
                null
            }
    }

    private fun openOrRefreshConversation(player: Player, npc: AINPC) {
        val currentPartner = conversations().getConversationPartner(player)
        if (currentPartner != null && currentPartner.uuid == npc.uuid) {
            refreshConversationSession(player)
            return
        }

        beginConversationSession(player, npc).exceptionally { ex ->
            plugin.logger.warning("Nu am putut reactualiza sesiunea pentru " + npc.name + ": " + ex.message)
            false
        }
    }

    private fun getFirstMeetingGreeting(npc: AINPC): String {
        val extraversion = npc.personality.extraversion

        return if (extraversion > 0.7) {
            "Buna ziua, straiin! Ce te aduce pe aici? Eu sunt ${npc.name}!"
        } else if (extraversion > 0.4) {
            "Salut. Nu cred ca ne-am mai intalnit. Eu sunt ${npc.name}."
        } else {
            "Hm? Ah... salut. Sunt ${npc.name}."
        }
    }

    private fun getReturningGreeting(npc: AINPC, player: Player, relationship: NPCRelationship?): String {
        var affection = 0.0
        if (relationship != null) {
            affection = relationship.affection
        }

        return if (affection > 0.6) {
            "Ce bucurie sa te vad din nou, ${player.name}! Cum mai esti?"
        } else if (affection > 0.3) {
            "A, ${player.name}! Bine ai revenit."
        } else if (affection > 0) {
            "Te-ai intors, ${player.name}. Ce mai vrei?"
        } else if (affection > -0.3) {
            "Tu iar? Ce vrei?"
        } else {
            "*oftez* Ce vrei de la mine?"
        }
    }

    private fun questFeatureEnabled(): Boolean = plugin.config.getBoolean("features.quest", true)
}
