package ro.ainpc.environment

import java.util.concurrent.ConcurrentHashMap

internal class WorldSpecialEventRegistry {
    private val eventsByWorld = ConcurrentHashMap<String, List<String>>()

    fun add(worldName: String?, event: String) {
        if (worldName.isNullOrBlank() || event.isBlank()) return
        eventsByWorld.compute(worldName) { _, existing ->
            val events = existing.orEmpty()
            if (event in events) events else events + event
        }
    }

    fun remove(worldName: String?, event: String) {
        if (worldName.isNullOrBlank()) return
        eventsByWorld.computeIfPresent(worldName) { _, existing ->
            (existing - event).takeIf { it.isNotEmpty() }
        }
    }

    fun clear(worldName: String?) {
        if (!worldName.isNullOrBlank()) eventsByWorld.remove(worldName)
    }

    fun get(worldName: String?): List<String> =
        if (worldName.isNullOrBlank()) emptyList() else eventsByWorld[worldName].orEmpty()
}
