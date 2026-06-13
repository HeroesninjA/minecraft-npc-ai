@file:JvmName("AINPCCommandStory")

package ro.ainpc.commands

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import ro.ainpc.api.WorldAdminApi
import ro.ainpc.npc.AINPC
import ro.ainpc.story.PlaceStoryState
import ro.ainpc.story.RegionStoryState
import ro.ainpc.story.StoryEvent
import ro.ainpc.story.StoryContextSnapshot
import ro.ainpc.world.WorldPlaceInfo
import ro.ainpc.world.WorldRegionInfo
import java.sql.SQLException

lateinit var ainpcCommandStoryPlugin: AINPCPlugin

fun initAinpcCommandStoryPlugin(plugin: AINPCPlugin) {
    ainpcCommandStoryPlugin = plugin
}

private const val STORY_EVENT_DEFAULT_LIMIT = 10
private const val STORY_EVENT_MAX_LIMIT = 50

fun handleStory(sender: CommandSender, args: Array<String>): Boolean {
    if (!sender.hasPermission("ainpc.admin")) {
        ainpcCommandStoryPlugin.messageUtils.sendMessage(sender, "no_permission")
        return true
    }

    if (args.size < 2) {
        sendStoryUsage(sender)
        return true
    }

    val storyMode = args[1].lowercase()
    return when (storyMode) {
        "context" -> handleStoryContext(sender, args)
        "region" -> handleStoryRegion(sender, args)
        "place" -> handleStoryPlace(sender, args)
        "events" -> handleStoryEvents(sender, args)
        else -> {
            sendStoryUsage(sender)
            true
        }
    }
}

private fun getEnabledWorldAdmin(): WorldAdminApi? {
    val worldAdmin = ainpcCommandStoryPlugin.platform?.worldAdmin
    return if (worldAdmin != null && worldAdmin.isEnabled) worldAdmin else null
}

fun handleStoryRegion(sender: CommandSender, args: Array<String>): Boolean {
    if (args.size < 3) {
        ainpcCommandStoryPlugin.messageUtils.send(sender, "&cUtilizare: /ainpc story region <regionId>")
        return true
    }
    if (ainpcCommandStoryPlugin.storyStateService == null) {
        ainpcCommandStoryPlugin.messageUtils.send(sender, "&cStoryStateService nu este initializat.")
        return true
    }

    val worldAdmin = getEnabledWorldAdmin()
    val mappedRegion = resolveSingleStoryRegion(sender, worldAdmin, args[2])
    if (mappedRegion == null && hasAmbiguousRegionMatch(worldAdmin, args[2])) {
        return true
    }

    val regionId = mappedRegion?.id() ?: args[2]
    try {
        val state = ainpcCommandStoryPlugin.storyStateService.getRegionState(regionId).orElse(null)
        val events = ainpcCommandStoryPlugin.storyStateService.listRecentEvents(regionId, "", 5)

        ainpcCommandStoryPlugin.messageUtils.send(sender, "&6=== Story Region ===")
        ainpcCommandStoryPlugin.messageUtils.send(sender, "&eRegion ID: &f$regionId")
        if (mappedRegion != null) {
            ainpcCommandStoryPlugin.messageUtils.send(sender, "&eMapping name: &f${mappedRegion.name()}")
            ainpcCommandStoryPlugin.messageUtils.send(sender, "&eMapping story mode: &f${mappedRegion.storyMode().id}")
            ainpcCommandStoryPlugin.messageUtils.send(sender, "&eMapping story state: &f${mappedRegion.storyStateKey()}")
            ainpcCommandStoryPlugin.messageUtils.send(sender, "&eMapping story pool: &f${formatList(mappedRegion.storyPool())}")
        } else {
            ainpcCommandStoryPlugin.messageUtils.send(sender, "&eMapping: &7nu exista regiune mapata pentru selectorul dat")
        }

        if (state == null) {
            ainpcCommandStoryPlugin.messageUtils.send(sender, "&ePersistent state: &7nu exista rand in region_story_state")
        } else {
            sendRegionStoryState(sender, state)
        }
        ainpcCommandStoryPlugin.messageUtils.send(sender, "&eEvenimente recente: &f${events.size}")
        for (event in events) {
            ainpcCommandStoryPlugin.messageUtils.send(sender, "&7- ${formatStoryEvent(event)}")
        }
    } catch (exception: SQLException) {
        ainpcCommandStoryPlugin.logger.warning("Nu am putut citi story state pentru regiune: ${exception.message}")
        ainpcCommandStoryPlugin.messageUtils.send(sender, "&cNu am putut citi story state: ${exception.message}")
    }
    return true
}

fun handleStoryPlace(sender: CommandSender, args: Array<String>): Boolean {
    if (args.size < 3) {
        ainpcCommandStoryPlugin.messageUtils.send(sender, "&cUtilizare: /ainpc story place <placeId>")
        return true
    }
    if (ainpcCommandStoryPlugin.storyStateService == null) {
        ainpcCommandStoryPlugin.messageUtils.send(sender, "&cStoryStateService nu este initializat.")
        return true
    }

    val worldAdmin = getEnabledWorldAdmin()
    val mappedPlace = resolveSingleStoryPlace(sender, worldAdmin, args[2])
    if (mappedPlace == null && hasAmbiguousPlaceMatch(worldAdmin, args[2])) {
        return true
    }

    val placeId = mappedPlace?.id() ?: args[2]
    val regionId = mappedPlace?.regionId() ?: inferRegionIdFromPlaceId(placeId)
    try {
        val state = ainpcCommandStoryPlugin.storyStateService.getPlaceState(placeId).orElse(null)
        val events = ainpcCommandStoryPlugin.storyStateService.listRecentEvents(regionId, placeId, 5)

        ainpcCommandStoryPlugin.messageUtils.send(sender, "&6=== Story Place ===")
        ainpcCommandStoryPlugin.messageUtils.send(sender, "&ePlace ID: &f$placeId")
        if (mappedPlace != null) {
            ainpcCommandStoryPlugin.messageUtils.send(sender, "&eMapping name: &f${mappedPlace.displayName()}")
            ainpcCommandStoryPlugin.messageUtils.send(sender, "&eRegiune: &f${mappedPlace.regionId()}")
            ainpcCommandStoryPlugin.messageUtils.send(sender, "&eTip: &f${mappedPlace.placeType().id}")
            ainpcCommandStoryPlugin.messageUtils.send(sender, "&eMetadata story: &f${formatStoryMetadata(mappedPlace.metadata())}")
        } else {
            ainpcCommandStoryPlugin.messageUtils.send(sender, "&eMapping: &7nu exista place mapat pentru selectorul dat")
        }

        if (state == null) {
            ainpcCommandStoryPlugin.messageUtils.send(sender, "&ePersistent state: &7nu exista rand in place_story_state")
        } else {
            sendPlaceStoryState(sender, state)
        }
        ainpcCommandStoryPlugin.messageUtils.send(sender, "&eEvenimente recente: &f${events.size}")
        for (event in events) {
            ainpcCommandStoryPlugin.messageUtils.send(sender, "&7- ${formatStoryEvent(event)}")
        }
    } catch (exception: SQLException) {
        ainpcCommandStoryPlugin.logger.warning("Nu am putut citi story state pentru place: ${exception.message}")
        ainpcCommandStoryPlugin.messageUtils.send(sender, "&cNu am putut citi story state: ${exception.message}")
    }
    return true
}

fun handleStoryEvents(sender: CommandSender, args: Array<String>): Boolean {
    if (args.size < 3) {
        ainpcCommandStoryPlugin.messageUtils.send(sender, "&cUtilizare: /ainpc story events <regionId|placeId> [limit]")
        return true
    }
    if (ainpcCommandStoryPlugin.storyStateService == null) {
        ainpcCommandStoryPlugin.messageUtils.send(sender, "&cStoryStateService nu este initializat.")
        return true
    }

    var limit = STORY_EVENT_DEFAULT_LIMIT
    if (args.size >= 4) {
        val parsedLimit = parseIntegerStrict(args[3])
        if (parsedLimit == null || parsedLimit <= 0) {
            ainpcCommandStoryPlugin.messageUtils.send(sender, "&cLimit trebuie sa fie un numar pozitiv.")
            return true
        }
        limit = minOf(parsedLimit, STORY_EVENT_MAX_LIMIT)
    }

    val target = resolveStoryEventTarget(sender, args[2]) ?: return true

    try {
        val events = ainpcCommandStoryPlugin.storyStateService.listRecentEvents(target.regionId(), target.placeId(), limit)
        ainpcCommandStoryPlugin.messageUtils.send(sender, "&6=== Story Events ===")
        ainpcCommandStoryPlugin.messageUtils.send(sender, "&eTinta: &f${target.label()}")
        if (!target.mapped()) {
            ainpcCommandStoryPlugin.messageUtils.send(sender, "&7Selectorul nu a fost gasit in mapping; se cauta direct in DB.")
        }
        if (events.isEmpty()) {
            ainpcCommandStoryPlugin.messageUtils.send(sender, "&7Nu exista story_events pentru tinta curenta.")
            return true
        }
        for (event in events) {
            ainpcCommandStoryPlugin.messageUtils.send(sender, "&7- ${formatStoryEvent(event)}")
            if (event.description().isNotBlank()) {
                ainpcCommandStoryPlugin.messageUtils.send(sender, "&8  ${event.description()}")
            }
            if (event.payload().isNotEmpty()) {
                ainpcCommandStoryPlugin.messageUtils.send(sender, "&8  payload: ${formatMap(event.payload())}")
            }
        }
    } catch (exception: SQLException) {
        ainpcCommandStoryPlugin.logger.warning("Nu am putut lista story events: ${exception.message}")
        ainpcCommandStoryPlugin.messageUtils.send(sender, "&cNu am putut lista story events: ${exception.message}")
    }
    return true
}

fun handleStoryContext(sender: CommandSender, args: Array<String>): Boolean {
    if (ainpcCommandStoryPlugin.storyContextService == null) {
        ainpcCommandStoryPlugin.messageUtils.send(sender, "&cStoryContextService nu este initializat.")
        return true
    }

    val target = resolveStoryContextTarget(sender, args) ?: return true

    val npc = resolveStoryContextNpc(sender, target.npcSelector(), target.player())
    if (npc == null && !target.npcSelector().isNullOrBlank()) {
        return true
    }

    val snapshot: StoryContextSnapshot = if (npc != null) {
        ainpcCommandStoryPlugin.storyContextService.buildForNpc(npc, target.player())
    } else {
        ainpcCommandStoryPlugin.storyContextService.buildForPlayer(target.player())
    }

    ainpcCommandStoryPlugin.messageUtils.send(sender, "&6=== Story Context ===")
    ainpcCommandStoryPlugin.messageUtils.send(sender, "&eJucator: &f${target.player().name}")
    ainpcCommandStoryPlugin.messageUtils.send(sender, "&eNPC: &f${npc?.name ?: "<fara NPC tinta>"}")

    val promptBlock = snapshot.toPromptBlock()
    if (promptBlock.isBlank()) {
        ainpcCommandStoryPlugin.messageUtils.send(sender, "&7Nu exista context story relevant pentru tinta curenta.")
        return true
    }

    for (line in promptBlock.split("\\R".toRegex())) {
        if (line.isNotBlank()) {
            ainpcCommandStoryPlugin.messageUtils.send(sender, "&7$line")
        }
    }
    return true
}

private fun resolveStoryContextTarget(sender: CommandSender, args: Array<String>): StoryContextTarget? {
    if (args.size > 2) {
        val explicitPlayer = findOnlinePlayer(args[2])
        if (explicitPlayer != null) {
            val npcSelector = if (args.size > 3) args[3] else ""
            return StoryContextTarget(explicitPlayer, npcSelector)
        }

        if (sender is Player) {
            return StoryContextTarget(sender, args[2])
        }

        ainpcCommandStoryPlugin.messageUtils.send(sender, "&cJucatorul &e${args[2]} &cnu este online.")
        ainpcCommandStoryPlugin.messageUtils.send(sender, "&cUtilizare consola: /ainpc story context <jucator> [numeNpc|nearest]")
        return null
    }

    if (sender is Player) {
        return StoryContextTarget(sender, "")
    }

    ainpcCommandStoryPlugin.messageUtils.send(sender, "&cDin consola trebuie sa specifici si jucatorul.")
    ainpcCommandStoryPlugin.messageUtils.send(sender, "&cUtilizare consola: /ainpc story context <jucator> [numeNpc|nearest]")
    return null
}

private fun findOnlinePlayer(playerName: String): Player? {
    if (playerName.isNullOrBlank()) return null

    val targetPlayer = ainpcCommandStoryPlugin.server.getPlayerExact(playerName)
        ?: ainpcCommandStoryPlugin.server.getPlayer(playerName)
    return targetPlayer
}

private fun resolveStoryContextNpc(sender: CommandSender, npcSelector: String?, targetPlayer: Player): AINPC? {
    if (npcSelector.isNullOrBlank()) return null

    if ("nearest".equals(npcSelector, ignoreCase = true)) {
        val nearestNpc = findNearestQuestNpc(targetPlayer)
        if (nearestNpc == null) {
            ainpcCommandStoryPlugin.messageUtils.send(sender, "&cNu exista NPC-uri active in apropierea jucatorului.")
        }
        return nearestNpc
    }

    val npc = ainpcCommandStoryPlugin.npcManager.getNPCByName(npcSelector)
    if (npc == null) {
        ainpcCommandStoryPlugin.messageUtils.sendMessage(sender, "npc_not_found")
    }
    return npc
}

private fun findNearestQuestNpc(targetPlayer: Player): AINPC? {
    return findNearestQuestNpc(targetPlayer, "")
}

private fun findNearestQuestNpc(targetPlayer: Player, progressionKind: String): AINPC? {
    if (targetPlayer == null) return null

    return ainpcCommandStoryPlugin.npcManager.getActiveNPCsNear(targetPlayer.location, 16.0)
        .filter { npc ->
            normalizeProgressionKind(progressionKind).isBlank()
                || ainpcCommandStoryPlugin.scenarioEngine.hasQuestForNpc(targetPlayer, npc, progressionKind)
        }
        .sortedBy { npc -> npc.location?.distanceSquared(targetPlayer.location) ?: Double.MAX_VALUE }
        .firstOrNull()
}

private fun resolveSingleStoryRegion(sender: CommandSender, worldAdmin: WorldAdminApi?, selector: String): WorldRegionInfo? {
    if (worldAdmin == null) return null

    val matches = findRegionMatches(worldAdmin, selector)
    if (matches.size > 1) {
        ainpcCommandStoryPlugin.messageUtils.send(sender, "&cSelector ambiguu pentru regiune. Foloseste ID-ul complet.")
        ainpcCommandStoryPlugin.messageUtils.send(sender, "&7Potriviri: &f${formatList(matches.map { it.id() })}")
        return null
    }
    return if (matches.isEmpty()) null else matches[0]
}

private fun resolveSingleStoryPlace(sender: CommandSender, worldAdmin: WorldAdminApi?, selector: String): WorldPlaceInfo? {
    if (worldAdmin == null) return null

    val matches = findPlaceMatches(worldAdmin, selector)
    if (matches.size > 1) {
        ainpcCommandStoryPlugin.messageUtils.send(sender, "&cSelector ambiguu pentru place. Foloseste ID-ul complet.")
        ainpcCommandStoryPlugin.messageUtils.send(sender, "&7Potriviri: &f${formatList(matches.map { it.id() })}")
        return null
    }
    return if (matches.isEmpty()) null else matches[0]
}

private fun resolveStoryEventTarget(sender: CommandSender, selector: String): StoryEventTarget? {
    val worldAdmin = getEnabledWorldAdmin()
    if (worldAdmin != null) {
        val placeMatches = findPlaceMatches(worldAdmin, selector)
        if (placeMatches.size > 1) {
            ainpcCommandStoryPlugin.messageUtils.send(sender, "&cSelector ambiguu pentru place. Foloseste ID-ul complet.")
            ainpcCommandStoryPlugin.messageUtils.send(sender, "&7Potriviri: &f${formatList(placeMatches.map { it.id() })}")
            return null
        }
        if (placeMatches.size == 1) {
            val place = placeMatches[0]
            return StoryEventTarget(place.regionId(), place.id(), "place ${place.id()}", true)
        }

        val regionMatches = findRegionMatches(worldAdmin, selector)
        if (regionMatches.size > 1) {
            ainpcCommandStoryPlugin.messageUtils.send(sender, "&cSelector ambiguu pentru regiune. Foloseste ID-ul complet.")
            ainpcCommandStoryPlugin.messageUtils.send(sender, "&7Potriviri: &f${formatList(regionMatches.map { it.id() })}")
            return null
        }
        if (regionMatches.size == 1) {
            val region = regionMatches[0]
            return StoryEventTarget(region.id(), "", "region ${region.id()}", true)
        }
    }

    val rawSelector = selector?.trim() ?: ""
    if (rawSelector.isBlank()) {
        ainpcCommandStoryPlugin.messageUtils.send(sender, "&cSelectorul story events nu poate fi gol.")
        return null
    }
    return if (rawSelector.contains(":")) {
        StoryEventTarget(inferRegionIdFromPlaceId(rawSelector), rawSelector, "place $rawSelector", false)
    } else {
        StoryEventTarget(rawSelector, "", "region $rawSelector", false)
    }
}

private fun sendRegionStoryState(sender: CommandSender, state: RegionStoryState) {
    ainpcCommandStoryPlugin.messageUtils.send(sender, "&ePersistent mode: &f${state.storyMode().id}")
    ainpcCommandStoryPlugin.messageUtils.send(sender, "&ePersistent state: &f${state.stateKey()}")
    ainpcCommandStoryPlugin.messageUtils.send(sender, "&ePersistent pool: &f${formatList(state.storyPool())}")
    ainpcCommandStoryPlugin.messageUtils.send(sender, "&eVariables: &f${formatMap(state.variables())}")
    ainpcCommandStoryPlugin.messageUtils.send(sender, "&eUpdated: &f${formatStoryTime(state.updatedAt())} &7by &f${formatOptional(state.updatedBy())} &7source=&f${formatOptional(state.source())}")
}

private fun sendPlaceStoryState(sender: CommandSender, state: PlaceStoryState) {
    ainpcCommandStoryPlugin.messageUtils.send(sender, "&ePersistent state: &f${state.stateKey()}")
    ainpcCommandStoryPlugin.messageUtils.send(sender, "&eRegion: &f${formatOptional(state.regionId())}")
    ainpcCommandStoryPlugin.messageUtils.send(sender, "&eVariables: &f${formatMap(state.variables())}")
    ainpcCommandStoryPlugin.messageUtils.send(sender, "&eUpdated: &f${formatStoryTime(state.updatedAt())} &7by &f${formatOptional(state.updatedBy())} &7source=&f${formatOptional(state.source())}")
}
