package ro.ainpc.environment

import ro.ainpc.AINPCPlugin
import ro.ainpc.environment.EnvironmentContext.Season
import ro.ainpc.npc.NPCState

class SeasonalBehaviorService(private val plugin: AINPCPlugin) {
    private val springActivities = listOf(
        "plantează în grădină" to NPCState.FARMING,
        "arată câmpul" to NPCState.WORKING,
        "repara gardurile" to NPCState.WORKING,
        "curăță canalele" to NPCState.WORKING,
        "sădește pomi" to NPCState.FARMING
    )

    private val summerActivities = listOf(
        "coace fânul" to NPCState.FARMING,
        "merge la pescuit" to NPCState.FISHING,
        "adună apa de la fântână" to NPCState.WORKING,
        "recoltează grâul" to NPCState.FARMING,
        "face plajă la soare" to NPCState.RESTING
    )

    private val autumnActivities = listOf(
        "culege recoltele" to NPCState.FARMING,
        "adună lemne pentru iarnă" to NPCState.WORKING,
        "prepara conserve" to NPCState.CRAFTING,
        "strânge provizii" to NPCState.WORKING,
        "taie lemne" to NPCState.WORKING
    )

    private val winterActivities = listOf(
        "stă la cămin" to NPCState.RESTING,
        "toarce lână" to NPCState.CRAFTING,
        "povestește la gura sobei" to NPCState.SOCIALIZING,
        "repara unelte" to NPCState.CRAFTING,
        "prepara mâncare" to NPCState.CRAFTING
    )

    private val seasonCache = mutableMapOf<String, Season?>()
    private val previousSeason = mutableMapOf<String, String>()
    private var lastSeasonCheck = 0L
    private val checkInterval = 120000L

    fun getSeasonalActivity(worldName: String?): Pair<String, NPCState>? {
        if (worldName.isNullOrBlank()) return null
        val season = getSeason(worldName) ?: return null
        val activities = when (season) {
            Season.SPRING -> springActivities
            Season.SUMMER -> summerActivities
            Season.AUTUMN -> autumnActivities
            Season.WINTER -> winterActivities
        }
        val index = (worldName.hashCode() + season.ordinal * 31).let { Math.floorMod(it, activities.size) }
        return activities[index]
    }

    fun getSeasonDisplayName(worldName: String?): String {
        return getSeason(worldName)?.displayName ?: "Necunoscut"
    }

    fun getSeason(worldName: String?): Season? {
        if (worldName.isNullOrBlank()) return null
        val now = System.currentTimeMillis()
        if (now - lastSeasonCheck > checkInterval) {
            seasonCache.clear()
            lastSeasonCheck = now
        }
        return seasonCache.getOrPut(worldName) {
            try {
                val world = plugin.server.getWorld(worldName)
                val newSeason = if (world != null) plugin.environmentEngine.getSeason(world) else null
                if (newSeason != null) {
                    val old = previousSeason[worldName]
                    val newName = newSeason.displayName
                    if (old != null && old != newName) {
                        plugin.platform.addonRegistry.dispatchSeasonChange(worldName, old, newName)
                    }
                    previousSeason[worldName] = newName
                }
                newSeason
            } catch (e: Exception) { null }
        }
    }

    fun clearCache() {
        seasonCache.clear()
    }
}
