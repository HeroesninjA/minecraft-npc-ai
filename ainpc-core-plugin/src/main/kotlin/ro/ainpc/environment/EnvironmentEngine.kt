package ro.ainpc.environment

import org.bukkit.World
import ro.ainpc.AINPCPlugin
import ro.ainpc.environment.EnvironmentContext.Weather
import ro.ainpc.environment.EnvironmentContext.TimeOfDay
import ro.ainpc.environment.EnvironmentContext.Season
import ro.ainpc.environment.EnvironmentContext.Temperature
import java.util.concurrent.ConcurrentHashMap

/**
 * Environmentengine.
 */
class EnvironmentEngine(private val plugin: AINPCPlugin) {
    private val worldContexts = ConcurrentHashMap<String, EnvironmentContext>()
    private val worldDayCounters = ConcurrentHashMap<String, Int>()
    private val specialEvents = WorldSpecialEventRegistry()

    fun onWorldUnload(worldName: String) {
        worldContexts.remove(worldName)
        worldDayCounters.remove(worldName)
        specialEvents.clear(worldName)
    }

    fun tick() {
        for (world in plugin.server.worlds) {
            updateWorldContext(world)
        }
    }

    fun getContext(world: World?): EnvironmentContext {
        if (world == null) return EnvironmentContext.empty()
        return withSpecialEvents(worldContexts.getOrPut(world.name) { buildContext(world) })
    }

    fun getContext(worldName: String?): EnvironmentContext {
        if (worldName.isNullOrBlank()) return EnvironmentContext.empty()
        val context = worldContexts[worldName] ?: run {
            plugin.server.getWorld(worldName)?.let { updateWorldContext(it) } ?: EnvironmentContext.empty(worldName)
        }
        return withSpecialEvents(context)
    }

    fun getContextForLocation(worldName: String?, biome: String?): EnvironmentContext {
        val base = getContext(worldName)
        if (biome.isNullOrBlank()) return base
        val season = base.season
        val localWeather = if (base.weather == Weather.RAIN) {
            Weather.fromBukkit(true, false, biome.uppercase())
        } else base.weather
        return base.copy(
            weather = localWeather,
            temperature = Temperature.fromBiome(biome, season)
        )
    }

    fun registerSpecialEvent(worldName: String?, event: String) {
        specialEvents.add(worldName, event)
    }

    fun clearSpecialEvent(worldName: String?, event: String) {
        specialEvents.remove(worldName, event)
    }

    fun clearAllSpecialEvents(worldName: String?) {
        specialEvents.clear(worldName)
    }

    private fun withSpecialEvents(context: EnvironmentContext): EnvironmentContext {
        val events = specialEvents.get(context.worldName)
        return if (context.specialEvents == events) context else context.copy(specialEvents = events)
    }

    fun getDayNumber(world: World?): Int {
        if (world == null) return 0
        return worldDayCounters.getOrPut(world.name) { (world.fullTime / 24000L).toInt() }
    }

    fun getSeason(world: World?): Season {
        val day = getDayNumber(world)
        return Season.fromDay(day)
    }

    private fun updateWorldContext(world: World): EnvironmentContext {
        val fullTime = world.fullTime
        val tickOfDay = fullTime % 24000L
        val dayNumber = (fullTime / 24000L).toInt()
        worldDayCounters[world.name] = dayNumber

        val timeOfDay = TimeOfDay.fromTick(tickOfDay)
        val weather = Weather.fromBukkit(world.hasStorm(), world.isThundering, null)
        val season = Season.fromDay(dayNumber)

        val context = EnvironmentContext(
            worldName = world.name,
            timeOfDay = timeOfDay,
            weather = weather,
            season = season,
            lightLevel = (world.getBlockAt(0, 64, 0).lightLevel.toInt()).coerceIn(0, 15),
            temperature = Temperature.MILD,
            specialEvents = specialEvents.get(world.name),
            dayNumber = dayNumber,
            tickOfDay = tickOfDay
        )
        worldContexts[world.name] = context
        return context
    }

    private fun buildContext(world: World): EnvironmentContext {
        val fullTime = world.fullTime
        return EnvironmentContext(
            worldName = world.name,
            timeOfDay = TimeOfDay.fromTick(fullTime % 24000L),
            weather = Weather.fromBukkit(world.hasStorm(), world.isThundering, null),
            season = Season.fromDay((fullTime / 24000L).toInt()),
            lightLevel = 15,
            temperature = Temperature.MILD,
            specialEvents = specialEvents.get(world.name),
            dayNumber = (fullTime / 24000L).toInt(),
            tickOfDay = fullTime % 24000L
        )
    }
}
