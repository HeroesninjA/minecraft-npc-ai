@file:Suppress("SENSELESS_COMPARISON")
package ro.ainpc.listeners

import com.google.gson.JsonParser
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.entity.Player
import org.bukkit.entity.Villager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerQuitEvent
import ro.ainpc.AINPCPlugin
import ro.ainpc.ai.DialogManager
import ro.ainpc.ai.DialogueAddressCandidate
import ro.ainpc.ai.DialogueAddressReply
import ro.ainpc.ai.DialogueAddressResolver
import ro.ainpc.ai.DialogueAddressStatus
import ro.ainpc.ai.DialogueFarewell
import ro.ainpc.ai.PendingDialogueAddress
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
import ro.ainpc.commands.defaultObjectiveTarget
import ro.ainpc.engine.QuestDecisionIntentResolver
import ro.ainpc.engine.ScenarioEngine
import ro.ainpc.managers.isSameNpcRecord
import ro.ainpc.npc.AINPC
import java.util.UUID

/**
 * Listener dedicat chat-ului privat dintre jucator si NPC.
 */
/**
 * Npcchatlistener.
 */
class NPCChatListener(plugin: AINPCPlugin) : AbstractPluginListener(plugin) {
    private val dialogManager: DialogManager = plugin.dialogManager

    @EventHandler(priority = EventPriority.LOWEST)
    fun onAsyncChat(event: AsyncChatEvent) {
        val player = event.player
        val message = PLAIN_TEXT.serialize(event.message())

        if (plugin.guiService.hasTextInputRequest(player)) {
            event.isCancelled = true
            runSync {
                conversations().clearAddressClarification(player)
                handleGuiTextInput(player, message)
            }
            return
        }

        event.isCancelled = true
        runSync {
            if (!player.isOnline) return@runSync
            try {
                if (handleAddressClarification(player, message)) return@runSync
                val target = resolveTarget(player, message)
                if (target != null) {
                    handleResolvedMessage(player, message, target)
                }
            } catch (ex: Exception) {
                plugin.logger.warning("Eroare la rezolvarea tintei de dialog: ${ex.message}")
            }
        }
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
        if (discardUnavailableSession(player)) return null
        val activeNpc = conversations().getConversationPartner(player)
        if (activeNpc != null) {
            if (conversations().isExpired(player)) {
                publishDialogSessionEnded(player, activeNpc, "timeout")
                conversations().clearConversation(player)
            } else {
                val listenRadius = plugin.config.getDouble("dialog.passive_listen_radius", 8.0)
                val nearby = plugin.npcManager.getActiveNPCsNear(player.location, listenRadius)
                val candidates = (nearby + activeNpc).distinctBy { it.uuid }
                val address = resolveExplicitAddress(message, candidates)
                if (address.status == DialogueAddressStatus.AMBIGUOUS) {
                    requestAddressClarification(player, message, candidates.filter { it.uuid in address.matchedNpcIds })
                    return null
                }
                val addressedNpc = nearby.firstOrNull { it.uuid == address.npcId && it.uuid != activeNpc.uuid }
                if (addressedNpc != null) {
                    return buildTarget(player, addressedNpc, true, true, "named_address_switch", nearby.size)
                }
                return buildTarget(player, activeNpc, true, true, "active_session", 1)
            }
        }

        if (!plugin.config.getBoolean("dialog.passive_listen_enabled", false)) {
            return null
        }

        val listenRadius = plugin.config.getDouble("dialog.passive_listen_radius", 8.0)
        val nearby = plugin.npcManager.getActiveNPCsNear(player.location, listenRadius)
            .sortedBy { (it.location ?: player.location).distanceSquared(player.location) }

        if (nearby.isEmpty()) {
            return null
        }

        val address = resolveExplicitAddress(message, nearby)
        if (address.status == DialogueAddressStatus.AMBIGUOUS) {
            requestAddressClarification(player, message, nearby.filter { it.uuid in address.matchedNpcIds })
            return null
        }
        nearby.firstOrNull { it.uuid == address.npcId }?.let {
            return buildTarget(player, it, true, false, "name_match", nearby.size)
        }

        val directMatches = nearby.filter { mentionsNpc(message, it) }

        if (directMatches.size > 1) {
            requestAddressClarification(player, message, directMatches)
            return null
        }
        if (directMatches.size == 1) {
            return buildTarget(player, directMatches.single(), true, false, "name_match", nearby.size)
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

    private fun resolveExplicitAddress(message: String, candidates: List<AINPC>) =
        DialogueAddressResolver.resolveDetailed(
            message,
            candidates.map { DialogueAddressCandidate(it.uuid, it.name, it.displayName) },
        )

    private fun requestAddressClarification(player: Player, message: String, candidates: List<AINPC>) {
        val pending = conversations().beginAddressClarification(
            player, message, candidates.map { DialogueAddressCandidate(it.uuid, it.name, it.displayName) },
        )
        if (pending == null) {
            messages().sendMessage(player, "dialog_target_ambiguous")
            return
        }
        showAddressChoices(player, pending)
    }

    private fun showAddressChoices(player: Player, pending: PendingDialogueAddress) {
        val radius = plugin.config.getDouble("dialog.passive_listen_radius", 8.0)
        val nearby = plugin.npcManager.getActiveNPCsNear(player.location, radius).associateBy { it.uuid }
        messages().sendMessage(player, "dialog_target_choose")
        pending.candidates.forEachIndexed { index, candidate ->
            val location = nearby[candidate.npcId]?.location
            val position = location?.let { "${it.blockX}, ${it.blockY}, ${it.blockZ}" } ?: "-"
            messages().sendMessage(
                player, "dialog_target_option",
                mapOf(
                    "number" to (index + 1).toString(),
                    "name" to (candidate.displayName?.takeIf(String::isNotBlank) ?: candidate.name),
                    "position" to position,
                ),
            )
        }
    }

    private fun handleAddressClarification(player: Player, input: String): Boolean {
        if (discardUnavailableSession(player)) return true
        return when (val reply = conversations().replyToAddressClarification(player, input)) {
            DialogueAddressReply.None -> false
            DialogueAddressReply.Cancelled -> {
                messages().sendMessage(player, "dialog_target_cancelled")
                true
            }
            DialogueAddressReply.Expired -> {
                messages().sendMessage(player, "dialog_target_expired")
                true
            }
            is DialogueAddressReply.Retry -> {
                showAddressChoices(player, reply.pending)
                true
            }
            is DialogueAddressReply.Selected -> {
                val radius = plugin.config.getDouble("dialog.passive_listen_radius", 8.0)
                val nearby = plugin.npcManager.getActiveNPCsNear(player.location, radius)
                val selectedNpc = nearby.firstOrNull { it.uuid == reply.npcId }
                if (selectedNpc == null) {
                    messages().sendMessage(player, "dialog_target_unavailable")
                } else {
                    val target = buildTarget(player, selectedNpc, true, true, "named_address_clarified", nearby.size)
                        .copy(addressClarified = true)
                    handleResolvedMessage(player, reply.message, target)
                }
                true
            }
        }
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
        if (npc == null) return
        if (!isConversationPartnerAvailable(player, npc)) {
            endUnavailableConversation(player, npc)
            return
        }

        if (DialogueFarewell.isFarewell(message, listOfNotNull(npc.name, npc.displayName))) {
            if (target.explicitConversation() && conversations().getConversationNpcId(player) == npc.uuid) {
                endConversation(player, npc)
            } else if (target.directAddress()) {
                messages().sendNPCMessage(player, npc.name, getGoodbyeMessage(npc))
            }
            return
        }

        val request = DialogManager.DialogRequest(
            npc,
            player,
            message,
            target.directAddress(),
            target.explicitConversation(),
            target.triggerReason(),
            target.nearbyNpcCount(),
            target.distanceToNpc(),
            target.addressClarified,
        )
        if (dialogManager.isProcessingMessage(request)) {
            messages().sendMessage(player, "dialog_busy")
            return
        }

        val initialization = if (conversations().getConversationNpcId(player) != npc.uuid) {
            if (!publishDialogSessionStarted(
                    player, npc,
                    target.directAddress(), target.explicitConversation(),
                    target.triggerReason(), target.nearbyNpcCount(), target.distanceToNpc()
                )
            ) {
                return
            }
            val previousNpc = conversations().getConversationPartner(player)
            if (previousNpc != null && previousNpc.uuid != npc.uuid) {
                publishDialogSessionEnded(player, previousNpc, target.triggerReason())
                conversations().clearConversation(player)
            }
            beginConversationSession(player, npc)
        } else {
            refreshConversationSession(player)
            null
        }
        val sessionId = conversations().getConversationSessionId(player)
        initialization?.whenComplete { _, ex ->
            if (ex != null) {
                runSync {
                    if (!canDeliverConversationResponse(player, npc, sessionId)) return@runSync
                    plugin.logger.warning("Nu am putut salva memoria primei intalniri cu " + npc.name + ": " + ex.message)
                }
            }
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

        dialogManager.processMessage(request).thenAccept { result ->
            runSync {
                if (!canDeliverConversationResponse(player, npc, sessionId)) return@runSync
                if (result == null) {
                    messages().sendMessage(player, "ai_error")
                    return@runSync
                }

                publishDialogResponseGenerated(player, npc, target, result)
                if (!canDeliverConversationResponse(player, npc, sessionId)) return@runSync
                when (result.status) {
                    DialogManager.DialogStatus.SUCCESS -> messages().sendNPCMessage(player, npc.name, result.response ?: "")
                    DialogManager.DialogStatus.COOLDOWN -> messages().sendMessage(player, "cooldown")
                    DialogManager.DialogStatus.BUSY -> messages().sendMessage(player, "dialog_busy")
                    DialogManager.DialogStatus.ERROR -> messages().sendMessage(player, "ai_error")
                }
            }
        }.exceptionally { ex ->
            runSync {
                if (!canDeliverConversationResponse(player, npc, sessionId)) return@runSync
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
        if (normalized.equals("cancel", ignoreCase = true) ||
            normalized.equals("anuleaza", ignoreCase = true)
        ) {
            messages().send(player, "&7Input anulat pentru &f${request.title()}&7.")
            reopenAfterTextInput(player, request)
            return
        }
        if (normalized.equals("clear", ignoreCase = true)) {
            plugin.guiService.setCreatorFormValue(player, request.formKey(), null)
            messages().send(player, "&7Campul &f${request.title()} &7a fost curatat.")
            reopenAfterTextInput(player, request)
            return
        }

        val sanitized = sanitizeGuiTextInput(request.formKey(), normalized)
        if (!isValidGuiTextInput(request.formKey(), sanitized)) {
            messages().send(player, "&cValoare invalida pentru &f${request.title()}&c.")
            requeueTextInput(player, request)
            return
        }

        plugin.guiService.setCreatorFormValue(player, request.formKey(), sanitized)
        messages().send(player, "&aSalvat: &f${request.title()}")
        reopenAfterTextInput(player, request)
    }

    private fun handleSpecialGuiTextInput(
        player: Player,
        request: ro.ainpc.gui.GuiService.TextInputRequest,
        normalized: String
    ): Boolean {
        return when (request.formKey()) {
            "creator_defs_filter" -> {
                val filter = if (normalized.equals("clear", ignoreCase = true)) null else normalized
                plugin.guiService.setCreatorFormValue(player, request.formKey(), filter)
                plugin.guiService.setCreatorFormValue(player, "creator_defs_page", null)
                plugin.guiService.open(player, ro.ainpc.gui.GuiKey.CREATOR_QUEST_DEFS)
                true
            }
            "quest_edit_query" -> {
                val query = if (normalized.equals("clear", ignoreCase = true)) null else normalized
                plugin.guiService.setCreatorFormValue(player, request.formKey(), query)
                plugin.guiService.setCreatorFormValue(player, "quest_edit_page", null)
                plugin.guiService.open(player, ro.ainpc.gui.GuiKey.QUEST_EDIT)
                true
            }
            "quest_obj_type" -> {
                val previousType = plugin.guiService.getCreatorFormValue(player, "quest_obj_type")
                val previousTarget = plugin.guiService.getCreatorFormValue(player, "quest_obj_target")
                plugin.guiService.setCreatorFormValue(player, request.formKey(), normalized)
                if (shouldResetObjectiveTarget(previousType, previousTarget)) {
                    plugin.guiService.setCreatorFormValue(player, "quest_obj_target", defaultObjectiveTarget(normalized))
                }
                plugin.guiService.open(player, ro.ainpc.gui.GuiKey.QUEST_CREATE)
                true
            }
            "quest_reward_type" -> {
                plugin.guiService.setCreatorFormValue(player, request.formKey(), normalized)
                val usesStoryEvent = normalized.equals("story_event", ignoreCase = true) ||
                    normalized.equals("record_story_event", ignoreCase = true)
                if (usesStoryEvent) {
                    if (plugin.guiService.getCreatorFormValue(player, "quest_reward_event_scope").isBlank()) {
                        plugin.guiService.setCreatorFormValue(player, "quest_reward_event_scope", "region")
                    }
                    if (plugin.guiService.getCreatorFormValue(player, "quest_reward_event_target").isBlank()) {
                        plugin.guiService.setCreatorFormValue(player, "quest_reward_event_target", "current_region")
                    }
                    if (plugin.guiService.getCreatorFormValue(player, "quest_reward_event_title").isBlank()) {
                        val questName = plugin.guiService.getCreatorFormValue(player, "quest_name").ifBlank { "Quest" }
                        plugin.guiService.setCreatorFormValue(player, "quest_reward_event_title", "$questName event")
                    }
                } else {
                    plugin.guiService.setCreatorFormValue(player, "quest_reward_event_scope", null)
                    plugin.guiService.setCreatorFormValue(player, "quest_reward_event_target", null)
                    plugin.guiService.setCreatorFormValue(player, "quest_reward_event_key", null)
                    plugin.guiService.setCreatorFormValue(player, "quest_reward_event_title", null)
                    plugin.guiService.setCreatorFormValue(player, "quest_reward_event_payload", null)
                }
                plugin.guiService.open(player, ro.ainpc.gui.GuiKey.QUEST_CREATE)
                true
            }
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

    private fun shouldResetObjectiveTarget(previousType: String, previousTarget: String): Boolean {
        if (previousTarget.isBlank()) return true
        val previousDefaults = objectiveTargetOptions(previousType)
        return previousTarget == defaultObjectiveTarget(previousType) || previousTarget in previousDefaults
    }

    private fun objectiveTargetOptions(objectiveType: String): List<String> {
        return when (objectiveType.lowercase()) {
            "talk_to_npc", "deliver_to_npc" -> listOf("npc:nearest", "npc:giver", "profession:garda", "profession:preot", "uuid:<npc_uuid>")
            "visit_place" -> listOf("place:nearest", "place:castel", "place:curte_castel", "tag:locatie", "tag:poi")
            "visit_region" -> listOf("region:nearest", "region:castel", "tag:regiune", "tag:zone")
            "inspect_node" -> listOf("node:nearest", "node:cufar", "node:altar", "place:castel:curte_castel", "tag:interior")
            "collect_item" -> listOf("item:IRON_INGOT", "item:EMERALD", "item:BOOK", "tag:resource")
            "kill_mob" -> listOf("mob:ZOMBIE", "mob:SKELETON", "mob:SPIDER", "tag:undead")
            "place_block" -> listOf("block:OAK_PLANKS", "block:STONE", "tag:build")
            "break_block" -> listOf("block:COBBLESTONE", "block:DEEPSLATE", "tag:mine")
            "craft_item" -> listOf("item:TORCH", "item:IRON_SWORD", "item:BOOK", "tag:craft")
            else -> listOf("tag:locatie", "place:sample", "node:sample", "npc:nearest")
        }
    }

    private fun isValidGuiTextInput(formKey: String, normalized: String): Boolean {
        return when (formKey) {
            "quest_obj_count", "quest_reward_count" -> normalized.toIntOrNull() != null
            "quest_id" -> normalized.matches(Regex("[A-Za-z0-9_:-]{1,64}"))
            "quest_mechanic",
            "quest_base",
            "quest_obj_type",
            "quest_obj_target",
            "quest_reward_type",
            "quest_stage_id",
            "quest_stage_name",
            "quest_stage_mode",
            "quest_dialog_type",
            "quest_dialog_speaker",
            "quest_reward_value" -> normalized.isNotBlank()
            "quest_reward_event_payload" -> normalized.isBlank() || runCatching {
                JsonParser.parseString(normalized).asJsonObject
            }.isSuccess
            else -> true
        }
    }

    private fun sanitizeGuiTextInput(formKey: String, normalized: String): String {
        return when (formKey) {
            "quest_mechanic",
            "quest_base",
            "quest_obj_type",
            "quest_obj_target",
            "quest_reward_type",
            "quest_stage_mode",
            "quest_dialog_type",
            "quest_dialog_speaker" -> normalized.lowercase()
            else -> normalized
        }
    }

    private fun requeueTextInput(player: Player, request: ro.ainpc.gui.GuiService.TextInputRequest) {
        plugin.guiService.openTextInput(
            player,
            request.title(),
            request.formKey(),
            request.returnKey(),
            request.returnSelector(),
            request.promptLines()
        )
    }

    private fun reopenAfterTextInput(player: Player, request: ro.ainpc.gui.GuiService.TextInputRequest) {
        if (request.returnKey() == ro.ainpc.gui.GuiKey.QUEST_DETAIL && request.returnSelector()?.isNotBlank() == true) {
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

        if (questInteraction.progressionSelector != null) {
            plugin.guiService.openQuestOffer(player, questInteraction.progressionSelector)
        }

        return true
    }

    private fun resolveQuestNpcForIntent(player: Player, npc: AINPC, intent: QuestDecisionIntentResolver.Intent): AINPC {
        if (intent == QuestDecisionIntentResolver.Intent.ACCEPT
            || intent == QuestDecisionIntentResolver.Intent.DECLINE
        ) {
            if (plugin.scenarioEngine.hasOfferedQuest(player, npc)) {
                return npc
            }
            val activeQuestNpc = plugin.scenarioEngine.resolveActiveQuestNpc(player, npc)
            if (activeQuestNpc != null && isSameNpc(activeQuestNpc, npc)) {
                return activeQuestNpc
            }
        }

        return npc
    }

    private fun isQuestDecisionContext(player: Player, npc: AINPC): Boolean {
        if (!questFeatureEnabled()) {
            return false
        }
        return plugin.scenarioEngine.hasOfferedQuest(player, npc)
    }

    private fun isSameNpc(first: AINPC?, second: AINPC?): Boolean {
        if (first == null || second == null) {
            return false
        }
        return isSameNpcRecord(first, second)
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
        return normalizedName.isNotBlank() &&
            Regex("(?<![\\p{L}\\p{N}])${Regex.escape(normalizedName)}(?![\\p{L}\\p{N}])")
                .containsMatchIn(normalizedMessage)
    }

    private fun normalize(value: String): String = QuestDecisionIntentResolver.normalize(value)

    private fun discardUnavailableSession(player: Player): Boolean {
        if (conversations().getConversationNpcId(player) == null) return false
        val npc = conversations().getConversationPartner(player)
        if (npc != null && isConversationPartnerAvailable(player, npc)) return false
        if (npc == null) {
            conversations().clearConversation(player)
        } else {
            endUnavailableConversation(player, npc)
        }
        return true
    }

    private fun endUnavailableConversation(player: Player, npc: AINPC) {
        if (conversations().getConversationNpcId(player) != npc.uuid) return
        publishDialogSessionEnded(player, npc, "npc_unavailable")
        conversations().clearConversation(player)
    }

    private fun endConversation(player: Player, npc: AINPC) {
        if (conversations().getConversationNpcId(player) != npc.uuid) return
        publishDialogSessionEnded(player, npc, "goodbye")
        conversations().clearConversation(player)
        messages().sendNPCMessage(player, npc.name, getGoodbyeMessage(npc))
        messages().send(player, "&7&o(Conversatia cu " + npc.name + " s-a incheiat.)")
        plugin.emotionManager.processEvent(npc, "player_leave", 1.0)
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
        val distanceToNpc: Double,
        val addressClarified: Boolean = false,
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
        private val QUEST_INTENTS = QuestDecisionIntentResolver()
    }
}
