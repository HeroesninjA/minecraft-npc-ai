package ro.ainpc.context

import ro.ainpc.AINPCPlugin
import ro.ainpc.npc.AINPC
import ro.ainpc.story.StoryContextSnapshot
import ro.ainpc.world.WorldContextSnapshot
import ro.ainpc.world.WorldPlaceInfo
import ro.ainpc.world.WorldRegionInfo

data class ContextSnapshot(
    val playerName: String,
    val playerLevel: Int,
    val playerHealth: Double,
    val playerFood: Int,
    val npcName: String = "",
    val npcOccupation: String = "",
    val npcState: String = "",
    val npcEmotion: String = "",
    val npcHomeAnchor: String = "",
    val npcWorkAnchor: String = "",
    val npcSocialAnchor: String = "",
    val currentRegion: String = "",
    val currentRegionType: String = "",
    val currentPlace: String = "",
    val currentPlaceType: String = "",
    val worldTime: Long = 0L,
    val worldName: String = "",
    val worldWeather: String = "",
    val worldBiome: String = "",
    val worldTimePeriod: String = "",
    val economyBalance: Int = 0,
    val activeQuestCount: Int = 0,
    val recentEvents: List<String> = emptyList(),
    val localQuestAnchors: List<String> = emptyList(),
    val localEconomySummary: String = "",
    val settlementSafety: String = "",
    val settlementComfort: String = "",
    val warnings: List<String> = emptyList()
) {
    fun isEmpty(): Boolean = playerName.isBlank()

    fun toPromptBlock(): String = buildString {
        appendLine("=== Context Snapshot ===")
        appendLine("Player: $playerName (level $playerLevel, health $playerHealth, food $playerFood)")
        if (npcName.isNotBlank()) {
            appendLine("NPC: $npcName — $npcOccupation [$npcState, $npcEmotion]")
            if (npcHomeAnchor.isNotBlank()) appendLine("  Home: $npcHomeAnchor")
            if (npcWorkAnchor.isNotBlank()) appendLine("  Work: $npcWorkAnchor")
            if (npcSocialAnchor.isNotBlank()) appendLine("  Social: $npcSocialAnchor")
        }
        if (currentRegion.isNotBlank()) appendLine("Region: $currentRegion ($currentRegionType)")
        if (currentPlace.isNotBlank()) appendLine("Place: $currentPlace ($currentPlaceType)")
        appendLine("World: $worldName (time: ${worldTime % 24000}, $worldTimePeriod, $worldWeather, $worldBiome)")
        appendLine("Economy: $economyBalance coins")
        appendLine("Active quests: $activeQuestCount")
        if (recentEvents.isNotEmpty()) {
            appendLine("Recent events:")
            for (event in recentEvents.take(5)) appendLine("  - $event")
        }
        if (localQuestAnchors.isNotEmpty()) {
            appendLine("Local quest anchors:")
            for (anchor in localQuestAnchors.take(5)) appendLine("  - $anchor")
        }
        if (localEconomySummary.isNotBlank()) {
            appendLine("Local economy: $localEconomySummary")
        }
        if (settlementSafety.isNotBlank()) {
            appendLine("Settlement safety: $settlementSafety")
        }
        if (settlementComfort.isNotBlank()) {
            appendLine("Settlement comfort: $settlementComfort")
        }
        if (warnings.isNotEmpty()) {
            appendLine("Warnings:")
            for (w in warnings) appendLine("  - $w")
        }
    }

    companion object {
        fun build(plugin: AINPCPlugin, playerName: String, npc: AINPC? = null): ContextSnapshot {
            val player = plugin.server.getPlayerExact(playerName)
            val worldAdmin = plugin.platform.worldAdmin
            val location = player?.location

            val region: WorldRegionInfo? = if (location != null && worldAdmin.isEnabled) {
                worldAdmin.findRegion(location.world.name, location.blockX, location.blockY, location.blockZ)
            } else null
            val place: WorldPlaceInfo? = if (location != null && worldAdmin.isEnabled) {
                worldAdmin.findPlace(location.world.name, location.blockX, location.blockY, location.blockZ)
            } else null

            val economyBalance = if (player != null) plugin.economyService.getBalance(player) else 0

            val localAnchors = runCatching {
                plugin.progressionService.getAnchorBindings("", "", 5)
                    .map { "${it.anchorType()}:${it.anchorId()} (${it.objectiveKey()})" }
            }.getOrDefault(emptyList())

            val recentStoryEvents = if (region != null) {
                runCatching {
                    plugin.storyStateService.listRecentEvents(region.id(), "", 5)
                        .map { "${it.eventType()}: ${it.title()}" }
                }.getOrDefault(emptyList())
            } else emptyList()

            val activeQuests = if (player != null) {
                runCatching {
                    val snapshot = plugin.progressionService.getProgressionGuiSnapshot(player, "active", false)
                    snapshot.allEntries().count { it.active() }
                }.getOrDefault(0)
            } else 0

            val world = location?.world
            val weather = if (world != null) {
                when {
                    world.isThundering() -> "thunder"
                    world.hasStorm() -> "rain"
                    else -> "clear"
                }
            } else ""
            val biomeType = runCatching { location?.block?.getBiome()?.name()?.lowercase() }.getOrDefault("")
            val biome = if (biomeType.isNullOrBlank()) "" else biomeType
            val timeOfDay = world?.time?.let { time ->
                val normalized = (time % 24000L + 24000L) % 24000L
                when {
                    normalized < 0 || normalized >= 24000 -> "unknown"
                    normalized in 0..6000 -> "day"
                    normalized in 6001..12000 -> "sunset"
                    normalized in 12001..13800 -> "night"
                    else -> "night"
                }
            } ?: ""

            return ContextSnapshot(
                playerName = playerName,
                playerLevel = player?.level ?: 0,
                playerHealth = player?.health ?: 20.0,
                playerFood = player?.foodLevel ?: 20,
                npcName = npc?.name ?: "",
                npcOccupation = npc?.occupation ?: "",
                npcState = npc?.currentState?.displayName ?: "",
                npcEmotion = npc?.emotions?.dominantEmotion ?: "",
                npcHomeAnchor = npc?.homeAnchor?.label() ?: "",
                npcWorkAnchor = npc?.workAnchor?.label() ?: "",
                npcSocialAnchor = npc?.socialAnchor?.label() ?: "",
                currentRegion = region?.name() ?: "",
                currentRegionType = region?.typeId() ?: "",
                currentPlace = place?.displayName() ?: "",
                currentPlaceType = place?.placeType()?.id ?: "",
                worldTime = location?.world?.time ?: 0L,
                worldName = location?.world?.name ?: "",
                worldWeather = weather,
                worldBiome = biome,
                worldTimePeriod = timeOfDay,
                economyBalance = economyBalance,
                activeQuestCount = activeQuests,
                recentEvents = recentStoryEvents,
                localQuestAnchors = localAnchors,
                localEconomySummary = "${plugin.shopService.shopCount()} NPC shops",
                settlementSafety = if (region != null) "region_type=${region.typeId()}" else "",
                settlementComfort = if (place != null) "place_type=${place.placeType().id}" else "",
                warnings = if (!worldAdmin.isEnabled) listOf("World admin dezactivat.") else emptyList()
            )
        }
    }
}
