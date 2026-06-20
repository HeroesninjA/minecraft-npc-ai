package ro.ainpc.api.events.story

import ro.ainpc.api.events.AINPCEventSource
import java.util.Collections
import java.util.LinkedHashMap
import java.util.UUID

class StoryActionAppliedEventPayload(
    val eventId: UUID,
    val createdAtMillis: Long,
    val source: AINPCEventSource,
    val playerUuid: UUID,
    val playerName: String,
    val npcId: String?,
    val npcUuid: UUID?,
    val npcName: String?,
    val progressionId: String,
    val progressionSelector: String,
    val progressionKind: String,
    val mechanicId: String,
    val questCode: String,
    val stageId: String,
    val actionType: String,
    val entryId: String,
    val itemId: String,
    val scopeType: String,
    val scopeId: String,
    val regionId: String,
    val placeId: String,
    val eventType: String,
    val eventKey: String,
    val description: String,
    metadata: Map<String, String>?
) {
    val metadata: Map<String, String> = Collections.unmodifiableMap(LinkedHashMap(metadata ?: emptyMap()))
}

class StoryActionAppliedEvent(
    val payload: StoryActionAppliedEventPayload
) : org.bukkit.event.Event() {
    override fun getHandlers(): org.bukkit.event.HandlerList = HANDLERS

    companion object {
        private val HANDLERS = org.bukkit.event.HandlerList()

        @JvmStatic
        fun getHandlerList(): org.bukkit.event.HandlerList = HANDLERS
    }
}
