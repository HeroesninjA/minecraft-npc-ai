@file:Suppress("SENSELESS_COMPARISON")
@file:JvmName("AINPCCommandQuest")

package ro.ainpc.commands

import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import ro.ainpc.npc.AINPC
import ro.ainpc.AINPCPlugin
import ro.ainpc.engine.*
import ro.ainpc.commands.QuestAnchorBindingRow
import ro.ainpc.gui.GuiKey
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.util.Locale

lateinit var ainpcCommandQuestPlugin: AINPCPlugin

fun initAinpcCommandQuestPlugin(plugin: AINPCPlugin) {
    ainpcCommandQuestPlugin = plugin
}

fun handleQuestGui(
    sender: CommandSender,
    args: Array<String>,
    requirePlayerSender: (CommandSender) -> Player?,
): Boolean {
    val player = requirePlayerSender(sender) ?: return true
    if (args.size > 3) {
        ainpcCommandQuestPlugin.messageUtils.send(sender, "&cUtilizare: /ainpc quest gui [progressionFilter]")
        return true
    }
    if (args.size >= 3) {
        ainpcCommandQuestPlugin.guiService.openQuestLog(player, args[2])
        return true
    }
    ainpcCommandQuestPlugin.guiService.open(player, GuiKey.QUEST)
    return true
}

fun handleQuestLog(
    sender: CommandSender,
    args: Array<String>,
    questDebug: (String) -> Unit,
    resolveQuestTargetPlayer: (CommandSender, Array<String>, Int, String) -> Player?,
): Boolean {
    val request = resolveQuestLogRequest(sender, args, resolveQuestTargetPlayer) ?: run {
        questDebug("Quest log oprit: nu am putut rezolva jucatorul tinta.")
        return true
    }
    val targetPlayer = request.player()

    val questInteraction = ainpcCommandQuestPlugin.progressionService
        .getLog(targetPlayer, request.filter(), sender.hasPermission("ainpc.admin"))
    if (!questInteraction.isHandled) {
        ainpcCommandQuestPlugin.messageUtils.send(sender, "&cNu am putut citi quest log-ul.")
        return true
    }

    val recipient = if (sender == targetPlayer) targetPlayer else sender
    for (systemMessage in questInteraction.systemMessages) {
        ainpcCommandQuestPlugin.messageUtils.send(recipient, systemMessage)
    }

    if (sender != targetPlayer) {
        ainpcCommandQuestPlugin.messageUtils.send(sender,
            "&aAi cerut quest log-ul pentru &f${targetPlayer.name}" +
                (if (request.filter().isBlank()) "&a." else " &afiltru=&f${request.filter()}&a."))
    }
    return true
}

private fun resolveQuestLogRequest(
    sender: CommandSender,
    args: Array<String>,
    resolveQuestTargetPlayer: (CommandSender, Array<String>, Int, String) -> Player?,
): QuestLogRequest? {
    val usage = "&cUtilizare: /ainpc quest log [jucator] [active|current|tracked|quest|contract|duty|bounty|event|main|side|repeatable|completed|failed|archived|all]"
    if (args.size > 4) {
        ainpcCommandQuestPlugin.messageUtils.send(sender, usage)
        return null
    }

    var filter = ""
    var playerArgIndex = -1
    if (args.size == 3) {
        val argument = args[2]
        if (isQuestLogFilter(argument)) {
            filter = normalizeQuestLogFilter(argument)
        } else {
            playerArgIndex = 2
        }
    } else if (args.size == 4) {
        val firstIsFilter = isQuestLogFilter(args[2])
        val secondIsFilter = isQuestLogFilter(args[3])
        if (firstIsFilter && !secondIsFilter) {
            filter = normalizeQuestLogFilter(args[2])
            playerArgIndex = 3
        } else if (!firstIsFilter && secondIsFilter) {
            playerArgIndex = 2
            filter = normalizeQuestLogFilter(args[3])
        } else {
            ainpcCommandQuestPlugin.messageUtils.send(sender, usage)
            return null
        }
    }

    val targetPlayer = resolveQuestTargetPlayer(sender, args, playerArgIndex, usage)
    return if (targetPlayer != null) QuestLogRequest(targetPlayer, filter) else null
}

fun handleQuestProgress(
    sender: CommandSender,
    args: Array<String>,
    questDebug: (String) -> Unit,
    resolveQuestTargetPlayer: (CommandSender, Array<String>, Int, String) -> Player?,
): Boolean {
    val usage = "&cUtilizare: /ainpc quest progress [tracked|questCode|templateId] [jucator]"
    if (args.size > 4) {
        ainpcCommandQuestPlugin.messageUtils.send(sender, usage)
        return true
    }

    var questSelector = ""
    var playerArgIndex = -1
    if (args.size == 3) {
        val explicitPlayer = findOnlinePlayer(args[2])
        if (explicitPlayer != null && !isTrackedQuestSelector(args[2])) {
            playerArgIndex = 2
        } else {
            questSelector = args[2]
        }
    } else if (args.size == 4) {
        questSelector = args[2]
        playerArgIndex = 3
    }

    val targetPlayer = resolveQuestTargetPlayer(sender, args, playerArgIndex, usage) ?: run {
        questDebug("Quest progress oprit: nu am putut rezolva jucatorul tinta.")
        return true
    }

    val questInteraction = ainpcCommandQuestPlugin.progressionService
        .getProgress(targetPlayer, questSelector)
    if (!questInteraction.isHandled) {
        ainpcCommandQuestPlugin.messageUtils.send(sender, "&cNu am putut citi progresul questului.")
        return true
    }

    val recipient = if (sender == targetPlayer) targetPlayer else sender
    for (systemMessage in questInteraction.systemMessages) {
        ainpcCommandQuestPlugin.messageUtils.send(recipient, systemMessage)
    }
    if (sender != targetPlayer) {
        ainpcCommandQuestPlugin.messageUtils.send(sender,
            "&aAi cerut progresul questului pentru &f${targetPlayer.name}" +
                (if (questSelector.isBlank()) "&a." else " &aselector=&f${questSelector}&a."))
    }
    return true
}

fun handleQuestTrack(
    sender: CommandSender,
    args: Array<String>,
    questDebug: (String) -> Unit,
    resolveQuestTargetPlayer: (CommandSender, Array<String>, Int, String) -> Player?,
): Boolean {
    val trackAction = if (args.size > 2) args[2].lowercase() else ""
    val trackRequest = resolveQuestTrackRequest(sender, args, trackAction, resolveQuestTargetPlayer)
    if (trackRequest == null) {
        questDebug("Quest track oprit: nu am putut rezolva jucatorul tinta.")
        return true
    }
    val targetPlayer = trackRequest.player()
    val questSelector = trackRequest.questSelector()

    if (trackRequest.action() == "stop") {
        val stopped = ainpcCommandQuestPlugin.progressionService.stopTracking(targetPlayer)
        ainpcCommandQuestPlugin.messageUtils.send(sender,
            if (stopped) "&aQuest tracking oprit pentru &f${targetPlayer.name}&a."
            else "&7Quest tracking nu era pornit pentru &f${targetPlayer.name}&7.")
        if (sender != targetPlayer) {
            ainpcCommandQuestPlugin.messageUtils.sendActionBar(targetPlayer, "&cQuest tracking oprit")
        }
        return true
    }

    val questInteraction = ainpcCommandQuestPlugin.progressionService
        .getTrack(targetPlayer, questSelector)
    if (!questInteraction.isHandled) {
        ainpcCommandQuestPlugin.messageUtils.send(sender, "&cNu am putut urmari quest-ul curent.")
        return true
    }

    val recipient = if (sender == targetPlayer) targetPlayer else sender
    for (systemMessage in questInteraction.systemMessages) {
        ainpcCommandQuestPlugin.messageUtils.send(recipient, systemMessage)
    }

    val trackingMarker = if (trackRequest.action() == "start")
        ainpcCommandQuestPlugin.progressionService.startTracking(targetPlayer, questSelector)
    else
        ainpcCommandQuestPlugin.progressionService.getTrackingMarker(targetPlayer, questSelector)
    applyQuestTrackingMarker(sender, targetPlayer, trackingMarker)

    if (trackRequest.action() == "start") {
        if (trackingMarker != null && trackingMarker.hasLocation()) {
            ainpcCommandQuestPlugin.messageUtils.send(sender,
                "&aQuest tracking persistent pornit pentru &f${targetPlayer.name}&a.")
        } else {
            ainpcCommandQuestPlugin.messageUtils.send(sender,
                "&cNu am pornit quest tracking persistent: nu exista o tinta cu locatie pentru questul curent.")
        }
        return true
    }

    if (sender != targetPlayer) {
        ainpcCommandQuestPlugin.messageUtils.send(sender,
            "&aAi cerut quest tracking pentru &f${targetPlayer.name}&a.")
    }
    return true
}

private fun resolveQuestTrackRequest(
    sender: CommandSender,
    args: Array<String>,
    rawAction: String,
    resolveQuestTargetPlayer: (CommandSender, Array<String>, Int, String) -> Player?,
): QuestTrackRequest? {
    val action = if (rawAction == "start" || rawAction == "stop") rawAction else ""
    val firstOptionalIndex = if (action.isBlank()) 2 else 3
    var questSelector = ""
    var playerArgIndex = -1
    val usage = "&cUtilizare: /ainpc quest track [start|stop] [questCode|templateId] [jucator]"

    if (args.size > firstOptionalIndex) {
        val firstOptional = args[firstOptionalIndex]
        val firstAsPlayer = findOnlinePlayer(firstOptional)
        if (firstAsPlayer != null) {
            playerArgIndex = firstOptionalIndex
        } else if (action != "stop") {
            questSelector = firstOptional
            playerArgIndex = if (args.size > firstOptionalIndex + 1) firstOptionalIndex + 1 else -1
        } else {
            ainpcCommandQuestPlugin.messageUtils.send(sender, usage)
            return null
        }
    }

    val maxArgs = firstOptionalIndex +
        if (questSelector.isBlank()) (if (playerArgIndex >= 0) 1 else 0)
        else (if (playerArgIndex >= 0) 2 else 1)
    if (args.size > maxArgs) {
        ainpcCommandQuestPlugin.messageUtils.send(sender, usage)
        return null
    }

    val targetPlayer = resolveQuestTargetPlayer(sender, args, playerArgIndex, usage) ?: return null
    return QuestTrackRequest(targetPlayer, questSelector, action)
}

private fun applyQuestTrackingMarker(
    sender: CommandSender,
    targetPlayer: Player?,
    trackingMarker: QuestTrackingMarker?,
) {
    if (targetPlayer == null || trackingMarker == null || !trackingMarker.hasLocation()) return

    val targetLocation = trackingMarker.location
    val compassSet = ainpcCommandQuestPlugin.progressionService.applyTrackingMarker(targetPlayer, trackingMarker)
    if (compassSet) {
        ainpcCommandQuestPlugin.messageUtils.send(targetPlayer,
            "&aBusola indica acum tinta questului: &f${trackingMarker.targetLabel}")
        if (sender != targetPlayer) {
            ainpcCommandQuestPlugin.messageUtils.send(sender,
                "&aMarkerul vizual a fost trimis catre &f${targetPlayer.name}" +
                    " &apentru &f${trackingMarker.targetLabel}&a.")
        }
        return
    }

    ainpcCommandQuestPlugin.messageUtils.send(targetPlayer,
        "&eTinta questului este in alta lume: &f${targetLocation?.world?.name ?: "necunoscuta"}" +
            " &7- &f${trackingMarker.targetLabel}")
    if (sender != targetPlayer) {
        ainpcCommandQuestPlugin.messageUtils.send(sender,
            "&eTinta questului pentru &f${targetPlayer.name}" +
                " &eeste in alta lume: &f${targetLocation?.world?.name ?: "necunoscuta"}")
    }
}

fun findOnlinePlayer(playerName: String?): Player? {
    if (playerName == null || playerName.isBlank()) return null
    return ainpcCommandQuestPlugin.server.getPlayerExact(playerName)
        ?: ainpcCommandQuestPlugin.server.getPlayer(playerName)
}

fun handleQuestDebug(
    sender: CommandSender,
    args: Array<String>,
    questDebug: (String) -> Unit,
    resolveQuestTargetPlayer: (CommandSender, Array<String>, Int, String) -> Player?,
): Boolean {
    if (!sender.hasPermission("ainpc.admin")) {
        ainpcCommandQuestPlugin.messageUtils.sendMessage(sender, "no_permission")
        return true
    }

    val usage = "&cUtilizare: /ainpc quest debug <tracked|questCode|templateId> [jucator]"
    if (args.size < 3) {
        ainpcCommandQuestPlugin.messageUtils.send(sender, usage)
        return true
    }

    val questSelector = args[2]
    val targetPlayer = resolveQuestTargetPlayer(sender, args, 3, usage) ?: run {
        questDebug("Quest debug oprit: nu am putut rezolva jucatorul tinta.")
        return true
    }

    val questInteraction = ainpcCommandQuestPlugin.progressionService
        .getDebug(targetPlayer, questSelector)
    if (!questInteraction.isHandled) {
        ainpcCommandQuestPlugin.messageUtils.send(sender,
            "&cNu exista quest curent sau arhivat pentru selectorul &f${questSelector}&c.")
        return true
    }

    for (systemMessage in questInteraction.systemMessages) {
        ainpcCommandQuestPlugin.messageUtils.send(sender, systemMessage)
    }
    return true
}

fun handleResetQuest(
    sender: CommandSender,
    args: Array<String>,
    questDebug: (String) -> Unit,
    resolveQuestTargetPlayer: (CommandSender, Array<String>, Int, String) -> Player?,
    refreshQuestNpc: (AINPC) -> AINPC,
): Boolean {
    if (!sender.hasPermission("ainpc.admin")) {
        ainpcCommandQuestPlugin.messageUtils.sendMessage(sender, "no_permission")
        return true
    }

    val usage = "&cUtilizare: /ainpc quest reset <numeNpc> [jucator]"
    if (args.size < 3) {
        ainpcCommandQuestPlugin.messageUtils.send(sender, usage)
        return true
    }

    val targetPlayer = resolveQuestTargetPlayer(sender, args, 3, usage) ?: run {
        questDebug("Quest reset oprit: nu am putut rezolva jucatorul tinta.")
        return true
    }

    var npc = ainpcCommandQuestPlugin.npcManager.getNPCByName(args[2])
    if (npc == null) {
        questDebug("Quest reset: NPC inexistent pentru numele '" + args[2] + "'.")
        ainpcCommandQuestPlugin.messageUtils.sendMessage(sender, "npc_not_found")
        return true
    }

    npc = refreshQuestNpc(npc)
    questDebug("Quest reset pentru npc=${npc.name} player=${targetPlayer.name}")
    val reset = ainpcCommandQuestPlugin.scenarioEngine.resetQuestProgress(targetPlayer, npc)
    if (!reset) {
        questDebug("Quest reset: nu exista progres activ pentru npc=${npc.name} player=${targetPlayer.name}")
        ainpcCommandQuestPlugin.messageUtils.send(sender,
            "&cNu exista progres pentru quest-ul lui &e${npc.name} &cla jucatorul &f${targetPlayer.name}&c.")
        return true
    }

    val questTitle = ainpcCommandQuestPlugin.scenarioEngine.getQuestTitle(npc)
    ainpcCommandQuestPlugin.messageUtils.send(targetPlayer,
        "&eQuest resetat manual: &f${if (questTitle.isBlank()) npc.name else questTitle}")
    if (sender != targetPlayer) {
        ainpcCommandQuestPlugin.messageUtils.send(sender,
            "&aAi resetat quest-ul lui &e${npc.name} &apentru &f${targetPlayer.name}&a.")
    }
    return true
}

fun handleCompleteQuest(
    sender: CommandSender,
    args: Array<String>,
    questDebug: (String) -> Unit,
    resolveQuestTargetPlayer: (CommandSender, Array<String>, Int, String) -> Player?,
    refreshQuestNpc: (AINPC) -> AINPC,
    deliverQuestInteraction: (CommandSender, Player, AINPC, QuestInteractionResult, String) -> Unit,
): Boolean {
    if (!sender.hasPermission("ainpc.admin")) {
        ainpcCommandQuestPlugin.messageUtils.sendMessage(sender, "no_permission")
        return true
    }

    val usage = "&cUtilizare: /ainpc quest complete <numeNpc> [jucator]"
    if (args.size < 3) {
        ainpcCommandQuestPlugin.messageUtils.send(sender, usage)
        return true
    }

    val targetPlayer = resolveQuestTargetPlayer(sender, args, 3, usage) ?: run {
        questDebug("Quest complete oprit: nu am putut rezolva jucatorul tinta.")
        return true
    }

    var npc = ainpcCommandQuestPlugin.npcManager.getNPCByName(args[2])
    if (npc == null) {
        questDebug("Quest complete: NPC inexistent pentru numele '" + args[2] + "'.")
        ainpcCommandQuestPlugin.messageUtils.sendMessage(sender, "npc_not_found")
        return true
    }

    npc = refreshQuestNpc(npc)
    questDebug("Quest complete pentru npc=${npc.name} player=${targetPlayer.name}")
    val questInteraction = ainpcCommandQuestPlugin.scenarioEngine.forceCompleteQuest(targetPlayer, npc)
    if (!questInteraction.isHandled) {
        questDebug("Quest complete: ScenarioEngine a returnat handled=false pentru npc=${npc.name} player=${targetPlayer.name}")
        ainpcCommandQuestPlugin.messageUtils.send(sender,
            "&cNPC-ul &e${npc.name} &cnu are un quest disponibil.")
        return true
    }

    deliverQuestInteraction(sender, targetPlayer, npc, questInteraction,
        "&aAi marcat manual quest-ul lui &e${npc.name} &aca finalizat pentru &f${targetPlayer.name}&a.")
    return true
}

fun handleAbandonQuest(
    sender: CommandSender,
    args: Array<String>,
    questDebug: (String) -> Unit,
    resolveQuestTargetPlayer: (CommandSender, Array<String>, Int, String) -> Player?,
    resolveQuestNpcSelector: (CommandSender, String, Player, String) -> AINPC?,
    refreshQuestNpc: (AINPC) -> AINPC,
    ensureQuestNpcCommandRange: (CommandSender, Player, AINPC) -> Boolean,
    deliverQuestInteraction: (CommandSender, Player, AINPC, QuestInteractionResult, String) -> Unit,
): Boolean {
    val usage = "&cUtilizare: /ainpc quest abandon <numeNpc>|nearest|tracked|<questCode|templateId> [jucator]"
    if (args.size < 3) {
        ainpcCommandQuestPlugin.messageUtils.send(sender, usage)
        return true
    }

    val npcSelector = args[2]
    val targetPlayer = resolveQuestTargetPlayer(sender, args, 3, usage) ?: run {
        questDebug("Quest abandon oprit: nu am putut rezolva jucatorul tinta.")
        return true
    }

    if (shouldHandleAbandonAsQuestSelector(npcSelector) { ainpcCommandQuestPlugin.npcManager.getNPCByName(it) }) {
        val questInteraction = ainpcCommandQuestPlugin.progressionService.abandon(targetPlayer, npcSelector)
        if (!questInteraction.isHandled) {
            ainpcCommandQuestPlugin.messageUtils.send(sender,
                "&cNu exista quest curent sau arhivat pentru selectorul &f${npcSelector}&c.")
            return true
        }

        val recipient = if (sender == targetPlayer) targetPlayer else sender
        for (systemMessage in questInteraction.systemMessages) {
            ainpcCommandQuestPlugin.messageUtils.send(recipient, systemMessage)
        }
        if (sender != targetPlayer) {
            ainpcCommandQuestPlugin.messageUtils.send(sender,
                "&eJucatorul &f${targetPlayer.name} &ea folosit abandon pentru quest selector &6${npcSelector}&e.")
        }
        return true
    }

    var npc = resolveQuestNpcSelector(sender, npcSelector, targetPlayer, "abandon") ?: return true
    npc = refreshQuestNpc(npc)
    if (!ensureQuestNpcCommandRange(sender, targetPlayer, npc)) return true

    val questInteraction = ainpcCommandQuestPlugin.scenarioEngine.abandonQuest(targetPlayer, npc)
    if (!questInteraction.isHandled) {
        ainpcCommandQuestPlugin.messageUtils.send(sender,
            "&cNPC-ul &e${npc.name} &cnu are un quest disponibil.")
        return true
    }

    deliverQuestInteraction(sender, targetPlayer, npc, questInteraction,
        "&eJucatorul &f${targetPlayer.name} &ea abandonat quest-ul lui &6${npc.name}&e.")
    return true
}

private fun questDebug(message: String) {
    ainpcCommandQuestPlugin.debug("[QuestCmd] $message")
}

fun resolveQuestTargetPlayer(
    sender: CommandSender,
    args: Array<String>,
    playerArgIndex: Int,
    usage: String,
): Player? {
    if (playerArgIndex >= 0 && args.size > playerArgIndex) {
        questDebug("Rezolvare player tinta din argumentul $playerArgIndex: '${args[playerArgIndex]}'")
        var targetPlayer = ainpcCommandQuestPlugin.server.getPlayerExact(args[playerArgIndex])
        if (targetPlayer == null) {
            questDebug("getPlayerExact a esuat pentru '${args[playerArgIndex]}', incerc getPlayer.")
            targetPlayer = ainpcCommandQuestPlugin.server.getPlayer(args[playerArgIndex])
        }
        if (targetPlayer == null) {
            questDebug("Rezolvare player tinta a esuat pentru '${args[playerArgIndex]}'.")
            ainpcCommandQuestPlugin.messageUtils.send(sender,
                "&cJucatorul &e${args[playerArgIndex]} &cnu este online.")
            return null
        }
        if (!sender.hasPermission("ainpc.admin")
            && (sender !is Player || sender.uniqueId != targetPlayer.uniqueId)) {
            ainpcCommandQuestPlugin.messageUtils.send(sender,
                "&cNu poti folosi comenzi de quest pentru alt jucator.")
            return null
        }
        questDebug("Player tinta rezolvat: ${targetPlayer.name}")
        return targetPlayer
    }

    if (sender is Player) {
        questDebug("Player tinta implicit din sender: ${sender.name}")
        return sender
    }

    questDebug("Rezolvare player tinta a esuat: sender non-player si nu a fost dat argument de jucator.")
    ainpcCommandQuestPlugin.messageUtils.send(sender, "&cDin consola trebuie sa specifici si jucatorul.")
    ainpcCommandQuestPlugin.messageUtils.send(sender, usage.replace("[jucator]", "<jucator>"))
    return null
}

fun ensureQuestNpcCommandRange(sender: CommandSender, targetPlayer: Player?, npc: AINPC?): Boolean {
    if (sender.hasPermission("ainpc.admin")) return true
    if (targetPlayer == null || npc == null) return false
    if (npc.isInRange(targetPlayer)) return true
    ainpcCommandQuestPlugin.messageUtils.send(sender, "&cEsti prea departe de NPC-ul &e${npc.name}&c.")
    return false
}

fun deliverQuestInteraction(
    sender: CommandSender,
    targetPlayer: Player,
    npc: AINPC,
    questInteraction: QuestInteractionResult,
    adminConfirmation: String,
) {
    questDebug("Livrare quest interaction: npc=${npc.name}" +
        " player=${targetPlayer.name}" +
        " openConversation=${questInteraction.shouldOpenConversation()}" +
        " npcMessages=${questInteraction.npcMessages.size}" +
        " systemMessages=${questInteraction.systemMessages.size}")
    if (questInteraction.shouldOpenConversation()) {
        ainpcCommandQuestPlugin.conversationSessionManager.startConversation(targetPlayer, npc)
        ainpcCommandQuestPlugin.memoryManager.ensureFirstMeetingMemoryAsync(npc, targetPlayer)
    }

    for (npcMessage in questInteraction.npcMessages) {
        ainpcCommandQuestPlugin.messageUtils.sendNPCMessage(targetPlayer, npc.name, npcMessage)
    }
    for (systemMessage in questInteraction.systemMessages) {
        ainpcCommandQuestPlugin.messageUtils.send(targetPlayer, systemMessage)
    }
    if (questInteraction.shouldOpenConversation()) {
        ainpcCommandQuestPlugin.messageUtils.send(targetPlayer,
            "&7&o(Scrie in chat pentru a vorbi cu ${npc.name}. Scrie 'pa' pentru a termina conversatia.)")
    }

    ainpcCommandQuestPlugin.emotionManager.processEvent(npc, "player_approach", 1.0)

    if (sender != targetPlayer) {
        ainpcCommandQuestPlugin.messageUtils.send(sender, adminConfirmation)
    }
}

fun refreshQuestNpc(npc: AINPC?): AINPC? {
    if (npc == null) {
        questDebug("refreshQuestNpc primit cu npc=null.")
        return null
    }

    val villager = npc.bukkitEntity
    if (villager is org.bukkit.entity.Villager) {
        questDebug("refreshQuestNpc pentru ${npc.name} prin villager uuid=${villager.uniqueId}")
        ainpcCommandQuestPlugin.npcManager.refreshVillagerProfile(villager)
        val refreshedNpc = ainpcCommandQuestPlugin.npcManager.getNPCByEntity(villager)
        if (refreshedNpc != null) {
            questDebug("refreshQuestNpc a rezolvat npc=${refreshedNpc.name}" +
                " ocupatie=${refreshedNpc.occupation}")
            return refreshedNpc
        }
        questDebug("refreshQuestNpc nu a gasit NPC dupa entity pentru ${npc.name}.")
    }

    return npc
}

fun resolveQuestNpcSelector(
    sender: CommandSender,
    npcSelector: String,
    targetPlayer: Player,
    action: String,
): AINPC? = resolveQuestNpcSelector(sender, npcSelector, targetPlayer, action, "")

fun resolveQuestNpcSelector(
    sender: CommandSender,
    npcSelector: String?,
    targetPlayer: Player,
    action: String,
    progressionKind: String,
): AINPC? {
    if (npcSelector == null || npcSelector.isBlank()) {
        ainpcCommandQuestPlugin.messageUtils.send(sender, "&cSpecifica NPC-ul pentru quest.")
        return null
    }

    if (npcSelector.equals("nearest", ignoreCase = true)) {
        val nearestNpc = findNearestQuestNpc(targetPlayer, progressionKind)
        if (nearestNpc == null) {
            questDebug("Quest $action: nu exista NPC activ in raza 16 pentru ${targetPlayer.name}")
            ainpcCommandQuestPlugin.messageUtils.send(sender,
                "&cNu exista NPC-uri active in apropierea jucatorului.")
            return null
        }
        return nearestNpc
    }

    val npc = ainpcCommandQuestPlugin.npcManager.getNPCByName(npcSelector)
    if (npc == null) {
        questDebug("Quest $action: NPC inexistent pentru numele '$npcSelector'.")
        ainpcCommandQuestPlugin.messageUtils.sendMessage(sender, "npc_not_found")
    }
    return npc
}

fun handleStatusQuest(
    sender: CommandSender,
    args: Array<String>,
): Boolean {
    val usage = "&cUtilizare: /ainpc quest status <numeNpc>|nearest|<questCode|templateId> [jucator]"
    if (args.size < 3) {
        return handleQuestLog(sender, args, ::questDebug, ::resolveQuestTargetPlayer)
    }

    val npcSelector = args[2]
    val targetPlayer = resolveQuestTargetPlayer(sender, args, 3, usage) ?: run {
        questDebug("Quest status oprit: nu am putut rezolva jucatorul tinta.")
        return true
    }

    if (!npcSelector.equals("nearest", ignoreCase = true)
        && ainpcCommandQuestPlugin.npcManager.getNPCByName(npcSelector) == null) {
        val questInteraction = ainpcCommandQuestPlugin.progressionService.getStatus(targetPlayer, npcSelector)
        if (questInteraction.isHandled) {
            val recipient = if (sender == targetPlayer) targetPlayer else sender
            for (systemMessage in questInteraction.systemMessages) {
                ainpcCommandQuestPlugin.messageUtils.send(recipient, systemMessage)
            }
            if (sender != targetPlayer) {
                ainpcCommandQuestPlugin.messageUtils.send(sender,
                    "&aAi cerut statusul questului &f${npcSelector}" +
                        " &apentru &f${targetPlayer.name}&a.")
            }
            return true
        }
    }

    return handleTriggerQuest(sender, npcSelector, targetPlayer, "")
}

fun handleTriggerQuest(sender: CommandSender, npcName: String, targetPlayer: Player): Boolean =
    handleTriggerQuest(sender, npcName, targetPlayer, "")

fun handleTriggerQuest(
    sender: CommandSender,
    npcName: String,
    targetPlayer: Player?,
    progressionKind: String,
): Boolean {
    if (targetPlayer == null) {
        questDebug("Quest trigger oprit: player tinta este null pentru npcName='$npcName'.")
        return true
    }

    questDebug("Quest trigger cerut pentru npcName='$npcName' player=${targetPlayer.name}")
    var npc = ainpcCommandQuestPlugin.npcManager.getNPCByName(npcName)
    if (npc == null) {
        questDebug("Quest trigger: NPC inexistent pentru numele '$npcName'.")
        ainpcCommandQuestPlugin.messageUtils.sendMessage(sender, "npc_not_found")
        return true
    }

    npc = refreshQuestNpc(npc)!!
    if (!ensureQuestNpcCommandRange(sender, targetPlayer, npc)) return true

    questDebug("Quest trigger foloseste npc=${npc.name} id=${npc.databaseId}" +
        " ocupatie=${npc.occupation}")

    val questInteraction = ainpcCommandQuestPlugin.scenarioEngine
        .startQuestManually(targetPlayer, npc, progressionKind)
    if (!questInteraction.isHandled) {
        questDebug("Quest trigger: handled=false pentru npc=${npc.name}" +
            " player=${targetPlayer.name}")
        ainpcCommandQuestPlugin.messageUtils.send(sender,
            "&cNPC-ul &e${npc.name} &cnu are un quest disponibil.")
        return true
    }

    questDebug("Quest trigger reusit pentru npc=${npc.name}" +
        " player=${targetPlayer.name}" +
        " openConversation=${questInteraction.shouldOpenConversation()}" +
        " npcMessages=${questInteraction.npcMessages.size}" +
        " systemMessages=${questInteraction.systemMessages.size}")

    deliverQuestInteraction(sender, targetPlayer, npc, questInteraction,
        "&aQuest-ul lui &e${npc.name} &aa fost declansat pentru &f${targetPlayer.name}&a.")
    return true
}

fun handleNearestQuest(
    sender: CommandSender,
    args: Array<String>,
): Boolean = handleNearestQuest(sender, args, "", "quest")

fun handleNearestQuest(
    sender: CommandSender,
    args: Array<String>,
    progressionKind: String,
    label: String,
): Boolean {
    val targetPlayer = resolveQuestTargetPlayer(sender, args, 2,
        "&cUtilizare: /ainpc ${commandLabelForKind(progressionKind)} nearest [jucator]") ?: run {
        questDebug("Quest nearest oprit: nu am putut rezolva jucatorul tinta.")
        return true
    }

    questDebug("Quest nearest pentru player=${targetPlayer.name}" +
        " kind=${formatOptional(progressionKind)}" +
        " locatie=${formatLocation(targetPlayer.location)}")
    val nearestNpc = findNearestQuestNpc(targetPlayer, progressionKind)
    if (nearestNpc == null) {
        questDebug("Quest nearest: nu exista NPC activ in raza 16 pentru ${targetPlayer.name}")
        ainpcCommandQuestPlugin.messageUtils.send(sender,
            "&cNu exista NPC-uri active in apropierea jucatorului" +
                (if (normalizeProgressionKind(progressionKind).isBlank()) "." else " cu $label disponibila."))
        return true
    }

    questDebug("Quest nearest a ales NPC-ul ${nearestNpc.name}" +
        " (id=${nearestNpc.databaseId})")
    return handleTriggerQuest(sender, nearestNpc.name, targetPlayer, progressionKind)
}

fun findNearestQuestNpc(targetPlayer: Player, progressionKind: String): AINPC? {
    val npcs = ainpcCommandQuestPlugin.npcManager.getActiveNPCsNear(targetPlayer.location, 16.0)
    return npcs
        .filter { normalizeProgressionKind(progressionKind).isBlank()
            || ainpcCommandQuestPlugin.scenarioEngine.hasQuestForNpc(targetPlayer, it, progressionKind) }
        .filter { it.location != null }
        .minByOrNull { it.location!!.distanceSquared(targetPlayer.location) }
}

fun handleAcceptQuest(
    sender: CommandSender,
    args: Array<String>,
): Boolean = handleAcceptQuest(sender, args, "",
    "&cUtilizare: /ainpc quest accept [numeNpc|nearest] [jucator]")

fun handleAcceptQuest(
    sender: CommandSender,
    args: Array<String>,
    progressionKind: String,
    usage: String,
): Boolean {
    val target = resolveQuestDecisionTarget(sender, args, "accept", usage, progressionKind) ?: return true

    val questInteraction = ainpcCommandQuestPlugin.scenarioEngine
        .acceptQuest(target.player(), target.npc(), progressionKind)
    if (!questInteraction.isHandled) {
        ainpcCommandQuestPlugin.messageUtils.send(sender,
            "&cNPC-ul &e${target.npc().name} &cnu are un quest disponibil.")
        return true
    }

    deliverQuestInteraction(sender, target.player(), target.npc(), questInteraction,
        "&aJucatorul &f${target.player().name} &aa acceptat quest-ul lui &e${target.npc().name}&a.")
    return true
}

fun handleDeclineQuest(
    sender: CommandSender,
    args: Array<String>,
): Boolean = handleDeclineQuest(sender, args, "",
    "&cUtilizare: /ainpc quest decline [numeNpc|nearest] [jucator]")

fun handleDeclineQuest(
    sender: CommandSender,
    args: Array<String>,
    progressionKind: String,
    usage: String,
): Boolean {
    val target = resolveQuestDecisionTarget(sender, args, "decline", usage, progressionKind) ?: return true

    val questInteraction = ainpcCommandQuestPlugin.scenarioEngine
        .declineQuest(target.player(), target.npc(), progressionKind)
    if (!questInteraction.isHandled) {
        ainpcCommandQuestPlugin.messageUtils.send(sender,
            "&cNPC-ul &e${target.npc().name} &cnu are un quest disponibil.")
        return true
    }

    deliverQuestInteraction(sender, target.player(), target.npc(), questInteraction,
        "&eJucatorul &f${target.player().name} &ea refuzat quest-ul lui &6${target.npc().name}&e.")
    return true
}

fun resolveQuestDecisionTarget(
    sender: CommandSender,
    args: Array<String>,
    action: String,
    usage: String,
): QuestDecisionTarget? = resolveQuestDecisionTarget(sender, args, action, usage, "")

fun resolveQuestDecisionTarget(
    sender: CommandSender,
    args: Array<String>,
    action: String,
    usage: String,
    progressionKind: String,
): QuestDecisionTarget? {
    var npcSelector = if (args.size > 2) args[2] else ""
    var playerArgIndex = if (args.size > 2) 3 else -1
    if (args.size == 3 && shouldTreatQuestDecisionArgumentAsPlayer(
            args[2],
            java.util.function.Function { ainpcCommandQuestPlugin.npcManager.getNPCByName(it) },
            java.util.function.Function { ainpcCommandQuestPlugin.server.getPlayerExact(it) ?: ainpcCommandQuestPlugin.server.getPlayer(it) },
        )) {
        npcSelector = ""
        playerArgIndex = 2
    }

    val targetPlayer = resolveQuestTargetPlayer(sender, args, playerArgIndex, usage) ?: run {
        questDebug("Quest $action oprit: nu am putut rezolva jucatorul tinta.")
        return null
    }

    val npc = resolveFlexibleQuestDecisionNpc(sender, npcSelector, targetPlayer, action, progressionKind) ?: return null
    val refreshedNpc = refreshQuestNpc(npc) ?: return null
    if (!ensureQuestNpcCommandRange(sender, targetPlayer, refreshedNpc)) return null
    return QuestDecisionTarget(targetPlayer, refreshedNpc)
}

fun resolveFlexibleQuestDecisionNpc(
    sender: CommandSender,
    npcSelector: String?,
    targetPlayer: Player,
    action: String,
): AINPC? = resolveFlexibleQuestDecisionNpc(sender, npcSelector, targetPlayer, action, "")

fun resolveFlexibleQuestDecisionNpc(
    sender: CommandSender,
    npcSelector: String?,
    targetPlayer: Player,
    action: String,
    progressionKind: String,
): AINPC? {
    if (!npcSelector.isNullOrBlank()) {
        return resolveQuestNpcSelector(sender, npcSelector, targetPlayer, action, progressionKind)
    }

    val activeNpc = ainpcCommandQuestPlugin.scenarioEngine.resolveActiveQuestNpc(targetPlayer, progressionKind)
    if (activeNpc != null) {
        questDebug("Quest $action a folosit NPC-ul questului curent: ${activeNpc.name}")
        return activeNpc
    }

    val nearestNpc = findNearestQuestNpc(targetPlayer, progressionKind)
    if (nearestNpc != null) {
        questDebug("Quest $action fara selector a ales cel mai apropiat NPC: ${nearestNpc.name}")
        return nearestNpc
    }

    ainpcCommandQuestPlugin.messageUtils.send(sender,
        "&cNu pot determina NPC-ul questului. Foloseste &e/ainpc quest $action <numeNpc>|nearest&c.")
    return null
}

fun handleQuestObjectives(sender: CommandSender, args: Array<String>): Boolean {
    if (!sender.hasPermission("ainpc.admin") && !sender.hasPermission("ainpc.quest")) {
        ainpcCommandQuestPlugin.messageUtils.sendMessage(sender, "no_permission")
        return true
    }
    if (args.size < 3) {
        ainpcCommandQuestPlugin.messageUtils.send(sender,
            "&cUtilizare: /ainpc quest objectives <selector> [jucator]")
        return true
    }

    val selector = args[2]
    val targetPlayer = resolveQuestTargetPlayer(sender, args, 3,
        "&cUtilizare: /ainpc quest objectives <selector> [jucator]") ?: return true

    val suggestions = ainpcCommandQuestPlugin.progressionService
        .getObjectiveIdSuggestions(targetPlayer, selector)
    if (suggestions.isEmpty()) {
        ainpcCommandQuestPlugin.messageUtils.send(sender,
            "&7Nu exista obiective mapabile pentru selectorul &f$selector&7.")
        return true
    }

    ainpcCommandQuestPlugin.messageUtils.send(sender, "&6=== Obiective mapabile ===")
    ainpcCommandQuestPlugin.messageUtils.send(sender,
        "&eSelector: &f$selector &7(&f${suggestions.size}&7 obiective)")
    for (suggestion in suggestions) {
        ainpcCommandQuestPlugin.messageUtils.send(sender, "&7- &f$suggestion")
    }
    return true
}

fun handleQuestAnchors(
    sender: CommandSender,
    args: Array<String>,
): Boolean {
    if (!sender.hasPermission("ainpc.admin")) {
        ainpcCommandQuestPlugin.messageUtils.sendMessage(sender, "no_permission")
        return true
    }
    if (ainpcCommandQuestPlugin.databaseManager == null) {
        ainpcCommandQuestPlugin.messageUtils.send(sender, "&cDatabaseManager nu este initializat.")
        return true
    }

    if (args.size > 2 && args[2].equals("remove", ignoreCase = true)) {
        return handleAnchorRemove(sender, args)
    }

    var playerUuid = ""
    var templateId = ""
    if (args.size > 2 && !args[2].equals("all", ignoreCase = true)) {
        playerUuid = resolveQuestAnchorPlayerUuid(sender, args[2]) ?: return true
    }
    if (args.size > 3) {
        templateId = args[3]
    }
    if (args.size > 4) {
        ainpcCommandQuestPlugin.messageUtils.send(sender,
            "&cUtilizare: /ainpc quest anchors [jucator|uuid|all] [templateId|questCode]")
        return true
    }

    try {
        val rows = queryQuestAnchorBindings(playerUuid, templateId, 20)
        ainpcCommandQuestPlugin.messageUtils.send(sender, "&6=== Quest Anchor Bindings ===")
        ainpcCommandQuestPlugin.messageUtils.send(sender,
            "&eFiltru player: &f${if (playerUuid.isBlank()) "all" else playerUuid}")
        ainpcCommandQuestPlugin.messageUtils.send(sender,
            "&eFiltru template/cod: &f${if (templateId.isBlank()) "all" else templateId}")
        if (rows.isEmpty()) {
            ainpcCommandQuestPlugin.messageUtils.send(sender, "&7Nu exista binding-uri pentru filtrul curent.")
            return true
        }

        ainpcCommandQuestPlugin.messageUtils.send(sender,
            "&eAfisate: &f${rows.size} &7(max 20, cele mai recente)")
        for (row in rows) {
            ainpcCommandQuestPlugin.messageUtils.send(sender, "&7- &f${formatQuestAnchorBinding(row)}")
        }
    } catch (exception: SQLException) {
        ainpcCommandQuestPlugin.logger.warning("Nu am putut lista quest_anchor_bindings: ${exception.message}")
        ainpcCommandQuestPlugin.messageUtils.send(sender,
            "&cNu am putut lista quest anchor bindings: ${exception.message}")
    }
    return true
}

private fun handleAnchorRemove(sender: CommandSender, args: Array<String>): Boolean {
    if (args.size < 5) {
        ainpcCommandQuestPlugin.messageUtils.send(sender,
            "&cUtilizare: /ainpc quest anchors remove <jucator|uuid> <templateId> <objectiveKey>")
        return true
    }
    val playerUuid = resolveQuestAnchorPlayerUuid(sender, args[3]) ?: return true
    val templateId = args[4].trim()
    val objectiveKey = if (args.size > 5) args[5].trim() else ""

    if (templateId.isBlank() || objectiveKey.isBlank()) {
        ainpcCommandQuestPlugin.messageUtils.send(sender,
            "&cUtilizare: /ainpc quest anchors remove <jucator|uuid> <templateId> <objectiveKey>")
        return true
    }

    try {
        ainpcCommandQuestPlugin.progressionService.deleteAnchorBinding(playerUuid, templateId, objectiveKey)
        ainpcCommandQuestPlugin.messageUtils.send(sender,
            "&aAnchor binding sters: &f$playerUuid &7/ &f$templateId &7/ &f$objectiveKey")
    } catch (exception: SQLException) {
        ainpcCommandQuestPlugin.logger.warning("Nu am putut sterge anchor binding: ${exception.message}")
        ainpcCommandQuestPlugin.messageUtils.send(sender,
            "&cNu am putut sterge anchor binding: ${exception.message}")
    }
    return true
}

private fun resolveQuestAnchorPlayerUuid(sender: CommandSender, selector: String?): String? {
    if (selector.isNullOrBlank()) return ""

    try {
        return java.util.UUID.fromString(selector).toString()
    } catch (_: IllegalArgumentException) {
    }

    var player = ainpcCommandQuestPlugin.server.getPlayerExact(selector)
    if (player == null) {
        player = ainpcCommandQuestPlugin.server.getPlayer(selector)
    }
    if (player == null) {
        ainpcCommandQuestPlugin.messageUtils.send(sender,
            "&cJucatorul trebuie sa fie online sau trebuie sa folosesti UUID-ul lui.")
        ainpcCommandQuestPlugin.messageUtils.send(sender,
            "&cUtilizare: /ainpc quest anchors [jucator|uuid|all] [templateId|questCode]")
        return null
    }
    return player.uniqueId.toString()
}

fun queryQuestAnchorBindings(
    playerUuid: String,
    templateIdOrQuestCode: String?,
    limit: Int,
): List<QuestAnchorBindingRow> {
    val reference = templateIdOrQuestCode?.trim() ?: ""
    if (reference.isBlank()) {
        return queryQuestAnchorBindings(playerUuid, "", "", limit)
    }

    val rows = queryQuestAnchorBindings(playerUuid, reference, "", limit)
    if (rows.isNotEmpty()) return rows
    return queryQuestAnchorBindings(playerUuid, "", reference, limit)
}

fun queryQuestAnchorBindings(
    playerUuid: String,
    templateId: String,
    questCode: String,
    limit: Int,
): List<QuestAnchorBindingRow> {
    val sql = StringBuilder("""
        SELECT b.player_uuid, b.template_id, b.objective_key, b.quest_code,
               b.objective_type, b.reference, b.anchor_type, b.anchor_id,
               b.anchor_label, b.created_at, b.updated_at, p.status
        FROM quest_anchor_bindings b
        LEFT JOIN player_quests p
          ON p.player_uuid = b.player_uuid AND p.template_id = b.template_id
        WHERE 1 = 1
    """.trimIndent())
    val parameters = mutableListOf<String>()
    if (playerUuid.isNotBlank()) {
        sql.append(" AND b.player_uuid = ?")
        parameters.add(playerUuid)
    }
    if (templateId.isNotBlank()) {
        sql.append(" AND b.template_id = ?")
        parameters.add(templateId)
    } else if (questCode.isNotBlank()) {
        sql.append(" AND LOWER(b.quest_code) = ?")
        parameters.add(questCode.lowercase(Locale.ROOT))
    }
    sql.append(" ORDER BY b.updated_at DESC")
    if (limit > 0) {
        sql.append(" LIMIT ?")
    }

    val statement = sql.toString()
    val stmt = ainpcCommandQuestPlugin.databaseManager.prepareStatement(statement)
    try {
        var index = 1
        for (parameter in parameters) {
            stmt.setString(index++, parameter)
        }
        if (limit > 0) {
            stmt.setInt(index, limit)
        }

        val rs = stmt.executeQuery()
        try {
            val rows = mutableListOf<QuestAnchorBindingRow>()
            while (rs.next()) {
                rows.add(readQuestAnchorBindingRow(rs))
            }
            return rows
        } finally {
            rs.close()
        }
    } finally {
        stmt.close()
    }
}
