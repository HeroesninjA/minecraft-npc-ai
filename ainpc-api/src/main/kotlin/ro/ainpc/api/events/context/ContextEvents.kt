package ro.ainpc.api.events.context

import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import ro.ainpc.api.events.AINPCEventSource
import java.util.Collections
import java.util.LinkedHashMap
import java.util.UUID

class WorldContextBuiltEventPayload(
    val eventId: UUID,
    val createdAtMillis: Long,
    val source: AINPCEventSource,
    val worldName: String,
    val regionId: String?,
    val placeId: String?,
    val nearbyPlaceCount: Int,
    val nearbyNodeCount: Int,
    val npcId: String?,
    val npcName: String?,
    metadata: Map<String, String>?
) {
    val metadata: Map<String, String> = Collections.unmodifiableMap(LinkedHashMap(metadata ?: emptyMap()))
}

class WorldContextBuiltEvent(
    val payload: WorldContextBuiltEventPayload
) : Event() {
    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}

class NPCContextUpdatedEventPayload(
    val eventId: UUID,
    val createdAtMillis: Long,
    val source: AINPCEventSource,
    val npcId: String,
    val npcUuid: UUID,
    val npcName: String,
    val timeOfDay: String?,
    val weather: String?,
    val isIndoors: Boolean,
    val isAtHome: Boolean,
    val isAtWork: Boolean,
    val isAtSocialSpot: Boolean,
    val nearbyPlayerCount: Int,
    val nearbyNpcCount: Int,
    val hasInteractingPlayer: Boolean,
    val updateReason: String,
    metadata: Map<String, String>?
) {
    val metadata: Map<String, String> = Collections.unmodifiableMap(LinkedHashMap(metadata ?: emptyMap()))
}

class NPCContextUpdatedEvent(
    val payload: NPCContextUpdatedEventPayload
) : Event() {
    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}

class ContextSignalCollectedEventPayload(
    val eventId: UUID,
    val createdAtMillis: Long,
    val source: AINPCEventSource,
    val signalType: String,
    val signalKey: String,
    val signalValue: String,
    val npcId: String?,
    val npcName: String?,
    val playerUuid: UUID?,
    val playerName: String?,
    metadata: Map<String, String>?
) {
    val metadata: Map<String, String> = Collections.unmodifiableMap(LinkedHashMap(metadata ?: emptyMap()))
}

class ContextSignalCollectedEvent(
    val payload: ContextSignalCollectedEventPayload
) : Event() {
    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}

class PlayerContextChangedEventPayload(
    val eventId: UUID,
    val createdAtMillis: Long,
    val source: AINPCEventSource,
    val playerUuid: UUID,
    val playerName: String,
    val previousRegionId: String?,
    val previousPlaceId: String?,
    val currentRegionId: String?,
    val currentPlaceId: String?,
    val worldName: String,
    val x: Double,
    val y: Double,
    val z: Double,
    metadata: Map<String, String>?
) {
    val metadata: Map<String, String> = Collections.unmodifiableMap(LinkedHashMap(metadata ?: emptyMap()))
}

class PlayerContextChangedEvent(
    val payload: PlayerContextChangedEventPayload
) : Event() {
    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}
