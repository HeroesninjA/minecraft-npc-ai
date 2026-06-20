package ro.ainpc.api.events.npc

import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import ro.ainpc.api.events.AINPCEventSource
import java.util.Collections
import java.util.LinkedHashMap
import java.util.UUID

class AINPCInteractedEventPayload(
    val eventId: UUID,
    val createdAtMillis: Long,
    val source: AINPCEventSource,
    val interactionId: String,
    val playerUuid: UUID,
    val playerName: String,
    val npcId: String,
    val npcUuid: UUID,
    val npcName: String,
    val directAddress: Boolean,
    val triggerReason: String,
    val nearbyNpcCount: Int,
    val distanceToNpc: Double,
    metadata: Map<String, String>?
) {
    val metadata: Map<String, String> = Collections.unmodifiableMap(LinkedHashMap(metadata ?: emptyMap()))
}

class AINPCInteractedEvent(
    val payload: AINPCInteractedEventPayload
) : Event() {
    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}
