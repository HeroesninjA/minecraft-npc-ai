@file:Suppress("SENSELESS_COMPARISON")
package ro.ainpc.listeners

import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.entity.Player
import org.bukkit.entity.Villager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerQuitEvent
import ro.ainpc.AINPCPlugin
import ro.ainpc.ai.DialogManager
import ro.ainpc.api.events.AINPCEventSource
import ro.ainpc.api.events.dialog.DialogIntentResolvedEvent
import ro.ainpc.api.events.dialog.DialogIntentResolvedEventPayload
import ro.ainpc.api.events.dialog.DialogMessageReceivedEvent
import ro.ainpc.api.events.dialog.DialogMessageReceivedEventPayload
import ro.ainpc.api.events.dialog.DialogResponseGeneratedEvent
import ro.ainpc.api.events.dialog.DialogResponseGeneratedEventPayload
import ro.ainpc.api.events.dialog.DialogSessionEndedEvent
import ro.ainpc.api.events.dialog.DialogSessionEndedEventPayload
import ro.ainpc.api.events.dialog.DialogSessionStartedEvent
import ro.ainpc.api.events.dialog.DialogSessionStartedEventPayload
import ro.ainpc.engine.QuestDecisionIntentResolver
import ro.ainpc.engine.ScenarioEngine
import ro.ainpc.npc.AINPC
import java.util.Comparator
import java.util.UUID

/**
 * Listener dedicat chat-ului privat dintre jucator si NPC.
 */
class NPCChatListener(plugin: AINPCPlugin) : AbstractPluginListener(plugin) {
    private val dialogManager: DialogManager = plugin.dialogManager

    @EventHandler(priority = EventPriority.LOWEST)
    fun onAsyncChat(event: AsyncChatEvent) {
        val player = event.player
        val message = PLAIN_TEXT.serialize(event.message())

        if (plugin.guiService.hasTextInputRequest(player)) {
            event.isCancelled = true
            runSync { handleGuiTextInput(player, message) }
            return
        }

        val target: ResolvedDialogTarget = try {
            callSync { resolveTarget(player, message) }
        } catch (ex: IllegalStateException) {
            plugin.logger.warning("Nu s-a putut rezolva tinta de dialog: " + ex.message)
            return
        } ?: return

        event.isCancelled = true
        runSync { handleResolvedMessage(player, message, target) }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val partner = conversations().getConversationPartner(event.player)
        if (partner != null) {
            publishDialogSessionEnded(event.player, partner, "quit")
        }
        conversations().clearConversation(event.player)
    }

    private fun resolveTarget(player: Player, message: String): ResolvedDialogTarget? {
        val activeNpc = conversations().getConversationPartner(player)
        if (activeNpc != null) {
            if (conversations().isExpired(player, CONVERSATION_TIMEOUT_MILLIS)) {
                publishDialogSessionEnded(player, activeNpc, "timeout")
                conversations().clearConversation(player)
            } else {
                return buildTarget(player, activeNpc, true, true, "active_session", 1)
            }
        }

        if (!plugin.config.getBoolean("dialog.passive_listen_enabled", false)) {
            return null
        }

        val listenRadius = plugin.config.getDouble("dialog.passive_listen_radius", 8.0)
        val nearby = plugin.npcManager.getActiveNPCsNear(player.location, listenRadius).stream()
            .sorted(Comparator.comparingDouble { npc -> (npc.location ?: player.location).distanceSquared(player.location) })
            .toList()

        if (nearby.isEmpty()) {
            return null
        }

        val directMatches = nearby.stream()
            .filter { npc -> mentionsNpc(message, npc) }
            .toList()

        if (!directMatches.isEmpty()) {
            return buildTarget(player, directMatches[0], true, false, "name_match", nearby.size)
        }

        if (nearby.size == 1) {
            return buildTarget(player, nearby[0], false, false, "single_nearby_npc", 1)
        }

        val nearest = nearby[0]
        val directRadius = plugin.config.getDouble("dialog.auto_engage_radius", 4.0)
        if ((nearest.location ?: player.location).distanceSquared(player.location) <= directRadius * directRadius) {
            return buildTarget(player, nearest, false, false, "nearest_npc", nearby.size)
        }

        return null
    }

    private fun buildTarget(
        player: Player,
        npc: AINPC,
        directAddress: Boolean,
        explicitConversation: Boolean,
        triggerReason: String,
        nearbyNpcCount: Int
    ): ResolvedDialogTarget {
        val npcLocation = npc.location
        val distance = if (npcLocation != null) npcLocation.distance(player.location) else 0.0
        return ResolvedDialogTarget(npc, directAddress, explicitConversation, triggerReason, nearbyNpcCount, distance)
    }

    private fun handleResolvedMessage(player: Player, message: String, target: ResolvedDialogTarget) {
        val npc = target.npc()
        if (npc == null || !npc.isSpawned()) {
            conversations().clearConversation(player)
            return
        }

        if (isGoodbye(message)) {
            if (target.explicitConversation()) {
                endConversation(player, npc)
            }
            return
        }

        if (!target.explicitConversation()) {
            if (!publishDialogSessionStarted(
                player,
                npc,
                target.directAddress(),
                target.explicitConversation(),
                target.triggerReason(),
                target.nearbyNpcCount(),
                target.distanceToNpc()
            )) {
                return
            }
            beginConversationSession(player, npc).exceptionally { ex ->
                plugin.logger.warning("Nu am putut initializa sesiunea de conversatie pentru " + npc.name + ": " + ex.message)
                false
            }
        } else {
            refreshConversationSession(player)
        }

        npc.lookAt(player)
        npc.updateContext()
        npc.context.setInteractingPlayer(player)
        npc.context.lastPlayerMessage = message
        if (questFeatureEnabled()) {
            plugin.scenarioEngine.recordNpcConversation(player, npc)
        }

        if (handleQuestInteractionFromMessage(player, npc, message, target)) {
            return
        }

        if (dialogManager.isOnCooldown(player, npc)) {
            messages().sendMessage(player, "cooldown")
            return
        }

        if (!publishDialogMessageReceived(player, npc, message, target)) {
            return
        }
        messages().send(player, "&7Tu: &f$message")
        messages().send(player, "&8" + npc.name + " se gandeste...")

        val request = DialogManager.DialogRequest(
            npc,
            player,
            message,
            target.directAddress(),
            target.explicitConversation(),
            target.triggerReason(),
            target.nearbyNpcCount(),
            target.distanceToNpc()
        )

        dialogManager.processMessage(request).thenAccept { result ->
            runSync {
                if (result == null) {
                    messages().sendMessage(player, "ai_error")
                    return@runSync
                }

                publishDialogResponseGenerated(player, npc, target, result)
                when (result.status) {
                    DialogManager.DialogStatus.SUCCESS -> messages().sendNPCMessage(player, npc.name, result.response ?: "")
                    DialogManager.DialogStatus.COOLDOWN -> messages().sendMessage(player, "cooldown")
                    DialogManager.DialogStatus.ERROR -> messages().sendMessage(player, "ai_error")
                }
            }
        }.exceptionally { ex ->
            runSync {
                plugin.logger.warning("Eroare la procesarea mesajului: " + ex.message)
                messages().sendMessage(player, "ai_error")
            }
            null
        }
    }

    private fun handleGuiTextInput(player: Player, message: String) {
        val request = plugin.guiService.consumeTextInputRequest(player) ?: return
        val normalized = message.trim()
        if (handleSpecialGuiTextInput(player, request, normalized)) {
            return
        }
        if (normalized.equals("clear", ignoreCase = true) ||
            normalized.equals("cancel", ignoreCase = true) ||
            normalized.equals("anuleaza", ignoreCase = true)
        ) {
            plugin.guiService.setCreatorFormValue(player, request.formKey(), null)
            messages().send(player, "&7Campul &f${request.title()} &7a fost curatat.")
            reopenAfterTextInput(player, request)
            return
        }

        plugin.guiService.setCreatorFormValue(player, request.formKey(), normalized)
        messages().send(player, "&aSalvat: &f${request.title()}")
        reopenAfterTextInput(player, request)
    }

    private fun handleSpecialGuiTextInput(
        player: Player,
        request: ro.ainpc.gui.GuiService.TextInputRequest,
        normalized: String
    ): Boolean {
        return when (request.formKey()) {
            "creator_quest_log_filter" -> {
                val filter = if (normalized.equals("clear", ignoreCase = true)) "all" else normalized
                plugin.guiService.openQuestLog(player, filter)
                true
            }
            "creator_quest_map_target" -> {
                if (normalized.equals("clear", ignoreCase = true)) {
                    plugin.guiService.setQuestMapMechanicFilter(player, null)
                    plugin.guiService.setQuestMapTemplateId(player, null)
                    plugin.guiService.setQuestMapObjectiveKey(player, null)
                    plugin.guiService.open(player, ro.ainpc.gui.GuiKey.QUEST_MAP)
                    return true
                }

                val input = normalized.removePrefix("mechanic:").removePrefix("template:").removePrefix("objective:")
                when {
                    normalized.startsWith("mechanic:", ignoreCase = true) -> {
                        plugin.guiService.setQuestMapMechanicFilter(player, input.ifBlank { null })
                        plugin.guiService.setQuestMapTemplateId(player, null)
                        plugin.guiService.setQuestMapObjectiveKey(player, null)
                    }
                    normalized.startsWith("template:", ignoreCase = true) -> {
                        plugin.guiService.setQuestMapMechanicFilter(player, null)
                        plugin.guiService.setQuestMapTemplateId(player, input.ifBlank { null })
                        plugin.guiService.setQuestMapObjectiveKey(player, null)
                    }
                    normalized.startsWith("objective:", ignoreCase = true) -> {
                        plugin.guiService.setQuestMapObjectiveKey(player, input.ifBlank { null })
                    }
                    else -> {
                        plugin.guiService.setQuestMapTemplateId(player, normalized)
                        plugin.guiService.setQuestMapMechanicFilter(player, null)
                        plugin.guiService.setQuestMapObjectiveKey(player, null)
                    }
                }
                plugin.guiService.open(player, ro.ainpc.gui.GuiKey.QUEST_MAP)
                true
            }
            else -> false
        }
    }

    private fun reopenAfterTextInput(player: Player, request: ro.ainpc.gui.GuiService.TextInputRequest) {
        if (request.returnKey() == ro.ainpc.gui.GuiKey.QUEST_DETAIL && request.returnSelector().isNotBlank()) {
            plugin.guiService.openQuestDetail(player, request.returnSelector(), plugin.guiService.getQuestDetailFilter(player))
            return
        }
        plugin.guiService.open(player, request.returnKey())
    }

    private fun handleQuestInteractionFromMessage(player: Player, npc: AINPC, message: String, target: ResolvedDialogTarget): Boolean {
        if (!questFeatureEnabled()) {
            return false
        }
        val intent = QUEST_INTENTS.resolve(
            message,
            isQuestDecisionContext(player, npc)
        )
        if (intent == QuestDecisionIntentResolver.Intent.NONE) {
            return false
        }

        publishDialogIntentResolved(player, npc, message, intent, target)
        val questNpc = refreshQuestNpc(resolveQuestNpcForIntent(player, npc, intent)) ?: return false

        val questInteraction = when (intent) {
            QuestDecisionIntentResolver.Intent.DECLINE -> plugin.scenarioEngine.declineQuest(player, questNpc)
            QuestDecisionIntentResolver.Intent.ABANDON -> plugin.scenarioEngine.abandonQuest(player, questNpc)
            QuestDecisionIntentResolver.Intent.ACCEPT -> plugin.scenarioEngine.acceptQuest(player, questNpc)
            QuestDecisionIntentResolver.Intent.STATUS -> plugin.scenarioEngine.getQuestStatus(player, questNpc)
            else -> plugin.scenarioEngine.handleQuestInteraction(player, questNpc)
        }
        if (!questInteraction.isHandled) {
            return false
        }

        messages().send(player, "&7Tu: &f$message")
        for (npcMessage in questInteraction.npcMessages) {
            messages().sendNPCMessage(player, questNpc.name, npcMessage)
        }
        for (systemMessage in questInteraction.systemMessages) {
            messages().send(player, systemMessage)
        }

        return true
    }

    private fun resolveQuestNpcForIntent(player: Player, npc: AINPC, intent: QuestDecisionIntentResolver.Intent): AINPC {
        if (intent == QuestDecisionIntentResolver.Intent.ACCEPT
            || intent == QuestDecisionIntentResolver.Intent.DECLINE
        ) {
            val activeQuestNpc = plugin.scenarioEngine.resolveActiveQuestNpc(player, npc)
            if (activeQuestNpc != null) {
                return activeQuestNpc
            }
        }

        return npc
    }

    private fun isQuestDecisionContext(player: Player, npc: AINPC): Boolean {
        if (!questFeatureEnabled()) {
            return false
        }
        if (!plugin.scenarioEngine.hasOfferedQuest(player)) {
            return false
        }

        val activeQuestNpc = plugin.scenarioEngine.resolveActiveQuestNpc(player, npc)
        return isSameNpc(activeQuestNpc, npc)
    }

    private fun isSameNpc(first: AINPC?, second: AINPC?): Boolean {
        if (first == null || second == null) {
            return false
        }
        if (first === second) {
            return true
        }
        if (first.uuid != null && second.uuid != null && first.uuid == second.uuid) {
            return true
        }
        if (first.databaseId > 0 && first.databaseId == second.databaseId) {
            return true
        }
        return first.name != null && first.name.equals(second.name, ignoreCase = true)
    }

    private fun questFeatureEnabled(): Boolean = plugin.config.getBoolean("features.quest", true)

    private fun refreshQuestNpc(npc: AINPC?): AINPC? {
        if (npc == null) {
            return null
        }

        if (npc.bukkitEntity is Villager) {
            val villager = npc.bukkitEntity as Villager
            plugin.npcManager.refreshVillagerProfile(villager)
            val refreshedNpc = plugin.npcManager.getNPCByEntity(villager)
            if (refreshedNpc != null) {
                return refreshedNpc
            }
        }

        return npc
    }

    private fun mentionsNpc(message: String, npc: AINPC): Boolean {
        val normalizedMessage = normalize(message)
        return containsNpcName(normalizedMessage, npc.name) || containsNpcName(normalizedMessage, npc.displayName)
    }

    private fun containsNpcName(normalizedMessage: String, npcName: String?): Boolean {
        if (npcName.isNullOrBlank()) {
            return false
        }

        val normalizedName = normalize(npcName)
        return normalizedName.isNotBlank() && normalizedMessage.contains(normalizedName)
    }

    private fun normalize(value: String): String = QuestDecisionIntentResolver.normalize(value)

    private fun endConversation(player: Player, npc: AINPC) {
        publishDialogSessionEnded(player, npc, "goodbye")
        conversations().clearConversation(player)
        messages().sendNPCMessage(player, npc.name, getGoodbyeMessage(npc))
        messages().send(player, "&7&o(Conversatia cu " + npc.name + " s-a incheiat.)")
        plugin.emotionManager.processEvent(npc, "player_leave", 1.0)
    }

    private fun isGoodbye(message: String): Boolean {
        val lower = message.lowercase().trim()
        return lower == "pa" || lower == "la revedere" || lower == "bye" ||
            lower == "adio" || lower == "exit" || lower == "quit" ||
            lower.startsWith("pa ")
    }

    private fun getGoodbyeMessage(npc: AINPC): String {
        val extraversion = npc.personality.extraversion
        val emotion = npc.emotions.dominantEmotion

        if (emotion == "happiness") {
            return "La revedere! A fost placut sa vorbim!"
        } else if (emotion == "sadness") {
            return "Pa... ai grija de tine."
        } else if (emotion == "anger") {
            return "In sfarsit pleci. La revedere."
        }

        if (extraversion > 0.6) {
            return "Pa pa! Sa ne vedem curand!"
        }
        return "La revedere."
    }

    private data class ResolvedDialogTarget(
        val npc: AINPC?,
        val directAddress: Boolean,
        val explicitConversation: Boolean,
        val triggerReason: String,
        val nearbyNpcCount: Int,
        val distanceToNpc: Double
    ) {
        fun npc(): AINPC? = npc
        fun directAddress(): Boolean = directAddress
        fun explicitConversation(): Boolean = explicitConversation
        fun triggerReason(): String = triggerReason
        fun nearbyNpcCount(): Int = nearbyNpcCount
        fun distanceToNpc(): Double = distanceToNpc
    }

    private fun publishDialogSessionStarted(
        player: Player,
        npc: AINPC,
        directAddress: Boolean,
        explicitConversation: Boolean,
        triggerReason: String,
        nearbyNpcCount: Int,
        distanceToNpc: Double
    ): Boolean {
        if (!plugin.config.getBoolean("events.public_api_enabled", true)) return true
        val event = DialogSessionStartedEvent(
            DialogSessionStartedEventPayload(
                UUID.randomUUID(),
                System.currentTimeMillis(),
                AINPCEventSource.PLAYER,
                player.uniqueId.toString() + ":" + npc.uuid,
                player.uniqueId,
                player.name,
                npc.databaseId.toString(),
                npc.uuid,
                npc.name,
                directAddress,
                explicitConversation,
                triggerReason,
                nearbyNpcCount,
                distanceToNpc,
                mapOf("source" to "NPCChatListener")
            )
        )
        plugin.server.pluginManager.callEvent(event)
        return !event.isCancelled
    }

    private fun publishDialogMessageReceived(player: Player, npc: AINPC, message: String, target: ResolvedDialogTarget): Boolean {
        if (!plugin.config.getBoolean("events.public_api_enabled", true)) return true
        val event = DialogMessageReceivedEvent(
            DialogMessageReceivedEventPayload(
                UUID.randomUUID(),
                System.currentTimeMillis(),
                AINPCEventSource.PLAYER,
                player.uniqueId.toString() + ":" + npc.uuid,
                player.uniqueId,
                player.name,
                npc.databaseId.toString(),
                npc.uuid,
                npc.name,
                message,
                target.directAddress(),
                target.explicitConversation(),
                target.triggerReason(),
                target.nearbyNpcCount(),
                target.distanceToNpc(),
                mapOf("source" to "NPCChatListener")
            )
        )
        plugin.server.pluginManager.callEvent(event)
        return !event.isCancelled
    }

    private fun publishDialogIntentResolved(
        player: Player,
        npc: AINPC,
        message: String,
        intent: QuestDecisionIntentResolver.Intent,
        target: ResolvedDialogTarget
    ) {
        if (!plugin.config.getBoolean("events.public_api_enabled", true)) return
        val event = DialogIntentResolvedEvent(
            DialogIntentResolvedEventPayload(
                UUID.randomUUID(),
                System.currentTimeMillis(),
                AINPCEventSource.PLAYER,
                player.uniqueId.toString() + ":" + npc.uuid,
                player.uniqueId,
                player.name,
                npc.databaseId.toString(),
                npc.uuid,
                npc.name,
                message,
                normalize(message),
                intent.name,
                target.directAddress(),
                target.explicitConversation(),
                target.triggerReason(),
                target.nearbyNpcCount(),
                target.distanceToNpc(),
                mapOf("source" to "NPCChatListener")
            )
        )
        plugin.server.pluginManager.callEvent(event)
    }

    private fun publishDialogResponseGenerated(player: Player, npc: AINPC, target: ResolvedDialogTarget, result: DialogManager.DialogResult) {
        if (!plugin.config.getBoolean("events.public_api_enabled", true)) return
        val preview = result.response?.take(140).orEmpty()
        val event = DialogResponseGeneratedEvent(
            DialogResponseGeneratedEventPayload(
                UUID.randomUUID(),
                System.currentTimeMillis(),
                AINPCEventSource.NPC,
                player.uniqueId.toString() + ":" + npc.uuid,
                player.uniqueId,
                player.name,
                npc.databaseId.toString(),
                npc.uuid,
                npc.name,
                result.status.name,
                preview,
                target.directAddress(),
                target.explicitConversation(),
                target.triggerReason(),
                mapOf("source" to "DialogManager")
            )
        )
        plugin.server.pluginManager.callEvent(event)
    }

    private fun publishDialogSessionEnded(player: Player, npc: AINPC, endReason: String) {
        if (!plugin.config.getBoolean("events.public_api_enabled", true)) return
        val event = DialogSessionEndedEvent(
            DialogSessionEndedEventPayload(
                UUID.randomUUID(),
                System.currentTimeMillis(),
                AINPCEventSource.PLAYER,
                player.uniqueId.toString() + ":" + npc.uuid,
                player.uniqueId,
                player.name,
                npc.databaseId.toString(),
                npc.uuid,
                npc.name,
                endReason,
                mapOf("source" to "NPCChatListener")
            )
        )
        plugin.server.pluginManager.callEvent(event)
    }

    companion object {
        private val PLAIN_TEXT: PlainTextComponentSerializer = PlainTextComponentSerializer.plainText()
        private const val CONVERSATION_TIMEOUT_MILLIS = 300_000L
        private val QUEST_INTENTS = QuestDecisionIntentResolver()
    }
}
