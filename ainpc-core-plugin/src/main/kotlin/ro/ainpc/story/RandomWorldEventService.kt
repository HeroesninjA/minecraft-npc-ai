package ro.ainpc.story

import ro.ainpc.AINPCPlugin
import ro.ainpc.utils.ConfigKeys
import ro.ainpc.world.WorldRegionInfo
import java.util.Random
import java.util.logging.Level

class RandomWorldEventService(private val plugin: AINPCPlugin) {
    private val random = Random()
    private var lastEventTime = mutableMapOf<String, Long>()
    private val dayLengthTicks = 24000L

    data class WorldEventConfig(
        val templateId: String,
        val minIntervalHours: Int,
        val maxIntervalHours: Int,
        val probability: Double,
        val requiredSeason: String? = null,
        val requiredTimeOfDay: String? = null,
        val minNpcs: Int = 0
    )

    private val eventConfigs = listOf(
        WorldEventConfig("village_celebration", 24, 72, 0.3, requiredTimeOfDay = "day"),
        WorldEventConfig("merchant_arrival", 48, 120, 0.4, requiredTimeOfDay = "day"),
        WorldEventConfig("raider_attack", 72, 168, 0.2, requiredTimeOfDay = "night"),
        WorldEventConfig("natural_disaster", 168, 720, 0.05),
        WorldEventConfig("seasonal_festival", 168, 336, 0.6),
        WorldEventConfig("discovery", 48, 144, 0.3, requiredTimeOfDay = "day"),
        WorldEventConfig("dark_omen", 72, 240, 0.15, requiredTimeOfDay = "night")
    )

    fun tick() {
        if (!plugin.config.getBoolean(ConfigKeys.STORY_RANDOM_EVENTS, false)) return
        val worldAdmin = plugin.platform.worldAdmin
        if (!worldAdmin.isEnabled) return

        for (region in worldAdmin.regions) {
            val regionId = region.id()
            if (!canFireEvent(regionId)) continue
            val npcCount = plugin.npcManager.getAllNPCs().count {
                it.isSpawned() && it.location?.world?.name?.equals(region.worldName(), ignoreCase = true) == true
            }
            val eligibleConfigs = eventConfigs.filter { config ->
                npcCount >= config.minNpcs && passesConditions(config, region)
            }
            if (eligibleConfigs.isEmpty()) continue
            val config = eligibleConfigs[random.nextInt(eligibleConfigs.size)]
            if (random.nextDouble() > config.probability) continue

            fireEvent(config, region)
        }
    }

    private fun canFireEvent(regionId: String): Boolean {
        val now = System.currentTimeMillis()
        val lastTime = lastEventTime.getOrDefault(regionId, 0L)
        if (now - lastTime < 3600000L) return false
        val minInterval = eventConfigs.minOf { it.minIntervalHours } * 3600000L
        return now - lastTime >= minInterval
    }

    private fun passesConditions(config: WorldEventConfig, region: WorldRegionInfo): Boolean {
        val world = plugin.server.getWorld(region.worldName()) ?: return false
        val timeOfDay = (world.time % dayLengthTicks).let { t ->
            when {
                t in 0..6000 -> "day"
                t in 6001..12000 -> "sunset"
                else -> "night"
            }
        }
        if (config.requiredTimeOfDay != null && config.requiredTimeOfDay != timeOfDay) return false
        if (config.requiredSeason != null) {
            val season = plugin.environmentEngine.getSeason(world)
            if (!season.id.equals(config.requiredSeason, ignoreCase = true)) return false
        }
        return true
    }

    private fun fireEvent(config: WorldEventConfig, region: WorldRegionInfo): Boolean {
        return try {
            val regionId = region.id()
            val authoring = plugin.storyAuthoringService
            val reviewRequired = plugin.config.getBoolean(ConfigKeys.STORY_RANDOM_REVIEW_REQUIRED, false)
            val success = if (reviewRequired) {
                if (authoring.hasPendingEvents("region", regionId)) {
                    plugin.logger.fine("[RandomEvent] Draft pending existent in $regionId; generarea este amanata")
                    return false
                }
                authoring.queueTemplate(
                    config.templateId,
                    regionId,
                    actorType = "scheduler",
                    actorId = "random-world-events",
                ) != null
            } else {
                authoring.applyTemplate(config.templateId, regionId)
            }
            if (success) {
                lastEventTime[regionId] = System.currentTimeMillis()
                val action = if (reviewRequired) "adaugat in coada pending" else "declansat"
                plugin.logger.info("[RandomEvent] ${config.templateId} $action in $regionId")
            }
            success
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "[RandomEvent] Eroare la declansarea ${config.templateId}", e)
            false
        }
    }

    fun forceEvent(templateId: String, regionId: String): Boolean {
        val config = eventConfigs.find { it.templateId == templateId } ?: return false
        val region = plugin.platform.worldAdmin.getRegion(regionId) ?: return false
        return fireEvent(config, region)
    }

    fun getCooldownStatus(regionId: String): Long {
        val lastTime = lastEventTime.getOrDefault(regionId, 0L)
        return if (lastTime == 0L) 0L else (System.currentTimeMillis() - lastTime) / 60000
    }
}
