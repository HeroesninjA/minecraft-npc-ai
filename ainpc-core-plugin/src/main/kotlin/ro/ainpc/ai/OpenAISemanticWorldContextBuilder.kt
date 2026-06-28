package ro.ainpc.ai

import ro.ainpc.AINPCPlugin
import ro.ainpc.npc.AINPC
import ro.ainpc.world.PlaceType
import ro.ainpc.world.WorldRegionInfo
import ro.ainpc.world.WorldPlaceInfo
import ro.ainpc.story.StoryContextSnapshot
import java.util.Locale
import kotlin.math.roundToInt

object OpenAISemanticWorldContextBuilder {
    private const val MAX_PLACES = 8
    private const val MAX_NPCS = 12
    private const val MAX_HISTORY_EVENTS = 5

    fun build(plugin: AINPCPlugin, npc: AINPC): String {
        return try {
            buildUnsafe(plugin, npc)
        } catch (e: RuntimeException) {
            plugin.debug("[SemanticWorldContext] Context semantic indisponibil: ${e.javaClass.simpleName}: ${e.message}")
            ""
        }
    }

    fun resolveProfessionFact(plugin: AINPCPlugin, npc: AINPC, playerMessage: String): String? {
        val normalized = playerMessage.lowercase(Locale.ROOT)
        if (!normalized.contains("cine ") && !normalized.contains("care ")) {
            return null
        }
        val context = build(plugin, npc)
        val marker = "Raspuns factual pentru intrebari despre meserii:"
        return context.lineSequence()
            .map { line -> line.trim() }
            .firstOrNull { line -> line.startsWith(marker) }
            ?.removePrefix(marker)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    private fun buildUnsafe(plugin: AINPCPlugin, npc: AINPC): String {
        val worldAdmin = plugin.platform.worldAdmin
        if (!worldAdmin.isEnabled) {
            return ""
        }
        val worldName = npc.worldName ?: return ""
        val x = npc.x.roundToInt()
        val y = npc.y.roundToInt()
        val z = npc.z.roundToInt()
        val region = worldAdmin.findRegion(worldName, x, y, z)
        val currentPlace = worldAdmin.findPlace(worldName, x, y, z)
        val regionId = region?.id()
        val storySnapshot = plugin.storyContextService.buildForNpc(npc, null)

        val places = if (regionId.isNullOrBlank()) {
            worldAdmin.findPlacesByWorld(worldName)
        } else {
            worldAdmin.getPlaces(regionId)
        }.sortedWith(compareBy<WorldPlaceInfo> { placePriority(it) }.thenBy { it.displayName().lowercase(Locale.ROOT) })
            .take(MAX_PLACES)

        val knownNpcs = plugin.npcManager.getAllNPCs()
            .asSequence()
            .filter { known -> known.name.isNotBlank() }
            .filter { known -> sameSemanticRegion(plugin, known, regionId, worldName) }
            .sortedWith(compareBy<AINPC> { npcPriority(it) }.thenBy { it.name.lowercase(Locale.ROOT) })
            .take(MAX_NPCS)
            .toList()

        if (region == null && currentPlace == null && places.isEmpty() && knownNpcs.isEmpty()) {
            return ""
        }

        return buildString {
            append("Regiune curenta: ")
            append(region?.name() ?: "necunoscuta")
            if (region != null) {
                append(" (id=").append(region.id()).append(", type=").append(region.typeId()).append(")")
            }
            append('\n')

            append("Loc curent: ")
            append(currentPlace?.displayName() ?: "necunoscut")
            if (currentPlace != null) {
                append(" (type=").append(currentPlace.placeType().id).append(")")
            }
            append('\n')

            if (places.isNotEmpty()) {
                append("Locuri cunoscute in zona:\n")
                for (place in places) {
                    append("- ").append(place.displayName())
                        .append(" [").append(place.placeType().id).append("]")
                    if (place.ownerNpcId().isNotBlank()) {
                        append(", owner_npc_id=").append(place.ownerNpcId())
                    }
                    if (place.tags().isNotEmpty()) {
                        append(", tags=").append(place.tags().take(4).joinToString(","))
                    }
                    append('\n')
                }
            }

            append(buildWorldLoreBlock(region, currentPlace, places))
            append(buildWorldHistoryBlock(region, currentPlace, storySnapshot))
            append(buildNpcLoreBlock(knownNpcs))
            append(buildStorySignalsBlock(storySnapshot))

            val tradeNpcs = knownNpcs.filter { isTradeNpc(it) }
            if (tradeNpcs.isNotEmpty()) {
                append("Raspuns factual pentru intrebari despre meserii: ")
                append(tradeNpcs.joinToString("; ") { known ->
                    val occupation = known.occupation.orEmpty().ifBlank { "meserie cunoscuta" }
                    val workPlace = known.workAnchor?.label()?.let { worldAdmin.getPlace(it) }
                    if (workPlace != null) {
                        "${known.name} are meseria $occupation si lucreaza la ${workPlace.displayName()}."
                    } else {
                        "${known.name} are meseria $occupation."
                    }
                })
                append('\n')
            }
        }.trim()
    }

    private fun buildWorldLoreBlock(region: WorldRegionInfo?, currentPlace: WorldPlaceInfo?, places: List<WorldPlaceInfo>): String {
        if (region == null && currentPlace == null && places.isEmpty()) {
            return ""
        }
        return buildString {
            append("WORLD_LORE:\n")
            if (region != null) {
                append("- region=").append(region.id())
                    .append(", name=").append(region.name())
                    .append(", type=").append(region.typeId())
                    .append(", tags=").append(region.tags().take(8).joinToString(","))
                    .append('\n')
            }
            if (currentPlace != null) {
                append("- current_place=").append(currentPlace.id())
                    .append(", name=").append(currentPlace.displayName())
                    .append(", type=").append(currentPlace.placeType().id)
                    .append(", tags=").append(currentPlace.tags().take(8).joinToString(","))
                    .append(", access=").append(if (currentPlace.publicAccess()) "public" else "restricted")
                    .append('\n')
            }
            if (places.isNotEmpty()) {
                append("- nearby_places:\n")
                for (place in places) {
                    append("  - ").append(place.id())
                        .append(", type=").append(place.placeType().id)
                        .append(", owner=").append(place.ownerNpcId().ifBlank { "<none>" })
                        .append(", tags=").append(place.tags().take(6).joinToString(","))
                        .append('\n')
                }
            }
        }
    }

    private fun buildWorldHistoryBlock(region: WorldRegionInfo?, currentPlace: WorldPlaceInfo?, storySnapshot: StoryContextSnapshot): String {
        val recentEvents = storySnapshot.recentStoryEvents().take(MAX_HISTORY_EVENTS)
        val regionHasHistory = region != null
        val placeHasHistory = currentPlace != null && (
            currentPlace.metadata()["lore"].orEmpty().isNotBlank() ||
                currentPlace.metadata()["history"].orEmpty().isNotBlank()
            )
        if (!regionHasHistory && !placeHasHistory && recentEvents.isEmpty()) {
            return ""
        }
        return buildString {
            append("WORLD_HISTORY:\n")
            if (region != null) {
                append("- region_story_mode=").append(region.storyMode().id)
                    .append(", state=").append(region.storyStateKey())
                    .append(", pool=").append(region.storyPool().take(6).joinToString(","))
                    .append('\n')
            }
            if (currentPlace != null) {
                val placeLore = currentPlace.metadata()["lore"].orEmpty()
                val placeHistory = currentPlace.metadata()["history"].orEmpty()
                if (placeLore.isNotBlank() || placeHistory.isNotBlank()) {
                    append("- place_history=").append(currentPlace.id())
                        .append(", lore=").append(placeLore.ifBlank { "<none>" })
                        .append(", history=").append(placeHistory.ifBlank { "<none>" })
                        .append('\n')
                }
            }
            if (recentEvents.isNotEmpty()) {
                append("- recent_events:\n")
                for (event in recentEvents) {
                    append("  - ").append(event.scopeType()).append(":").append(event.scopeId())
                        .append(' ').append(event.eventType())
                        .append('/').append(event.eventKey().ifBlank { "<fara_cheie>" })
                    if (event.title().isNotBlank()) {
                        append(" - ").append(event.title())
                    }
                    append('\n')
                }
            }
        }
    }

    private fun buildNpcLoreBlock(knownNpcs: List<AINPC>): String {
        if (knownNpcs.isEmpty()) {
            return ""
        }
        return buildString {
            append("NPC_LORE:\n")
            for (known in knownNpcs) {
                append("- ").append(known.name)
                    .append(", occupation=").append(known.occupation.orEmpty().ifBlank { "<none>" })
                    .append(", work=").append(known.workAnchor?.label().orEmpty().ifBlank { "<none>" })
                    .append(", home=").append(known.homeAnchor?.label().orEmpty().ifBlank { "<none>" })
                    .append(", lore=").append(known.backstory.orEmpty().ifBlank { "<none>" })
                    .append('\n')
            }
        }
    }

    private fun buildStorySignalsBlock(storySnapshot: StoryContextSnapshot): String {
        val signals = storySnapshot.storySignals().take(12)
        return if (signals.isEmpty()) {
            ""
        } else {
            buildString {
                append("STORY_SIGNALS: ").append(signals.joinToString(", ")).append('\n')
            }
        }
    }

    private fun sameSemanticRegion(plugin: AINPCPlugin, npc: AINPC, regionId: String?, fallbackWorldName: String): Boolean {
        if (regionId.isNullOrBlank()) {
            return npc.worldName.equals(fallbackWorldName, ignoreCase = true)
        }
        val npcRegion = plugin.platform.worldAdmin.findRegion(
            npc.worldName,
            npc.x.roundToInt(),
            npc.y.roundToInt(),
            npc.z.roundToInt()
        )
        return npcRegion?.id().equals(regionId, ignoreCase = true)
    }

    private fun placePriority(place: WorldPlaceInfo): Int = when (place.placeType()) {
        PlaceType.FORGE -> 0
        PlaceType.MARKET -> 1
        PlaceType.TAVERN -> 2
        PlaceType.SHOP -> 3
        PlaceType.FARM -> 4
        PlaceType.HOUSE -> 5
        else -> 9
    }

    private fun npcPriority(npc: AINPC): Int = when {
        isTradeNpc(npc) -> 0
        npc.occupation.orEmpty().isNotBlank() -> 1
        else -> 5
    }

    private fun isTradeNpc(npc: AINPC): Boolean {
        if (npc.occupation.orEmpty().isBlank()) {
            return false
        }
        val workLabel = npc.workAnchor?.label().orEmpty()
        if (workLabel.isBlank()) {
            return true
        }
        val placeType = npc.plugin?.platform?.worldAdmin?.getPlace(workLabel)?.placeType()
        return placeType == null || placeType == PlaceType.FORGE || placeType == PlaceType.SHOP ||
            placeType == PlaceType.FARM || placeType == PlaceType.MARKET || placeType == PlaceType.TAVERN ||
            placeType == PlaceType.CUSTOM
    }
}
