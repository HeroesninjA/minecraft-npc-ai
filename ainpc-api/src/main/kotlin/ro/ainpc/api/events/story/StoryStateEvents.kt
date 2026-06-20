package ro.ainpc.api.events.story

import ro.ainpc.api.events.AINPCEventSource
import java.util.Collections
import java.util.LinkedHashMap
import java.util.UUID

class StoryStateChangedEventPayload(
    val eventId: UUID,
    val createdAtMillis: Long,
    val source: AINPCEventSource,
    val scopeType: String,
    val scopeId: String,
    val regionId: String,
    val placeId: String,
    val previousStateKey: String,
    val newStateKey: String,
    val storyMode: String,
    val updatedBy: String,
    val origin: String,
    storyPoolEntries: List<String>?,
    metadata: Map<String, String>?
) {
    private val safeStoryPool = storyPoolEntries?.toList() ?: emptyList<String>()
    val storyPool: List<String> = Collections.unmodifiableList(safeStoryPool)
    val metadata: Map<String, String> = Collections.unmodifiableMap(LinkedHashMap(metadata ?: emptyMap()))
}

class StoryStateChangedEvent(
    val payload: StoryStateChangedEventPayload
) : org.bukkit.event.Event() {
    override fun getHandlers(): org.bukkit.event.HandlerList = HANDLERS

    companion object {
        private val HANDLERS = org.bukkit.event.HandlerList()

        @JvmStatic
        fun getHandlerList(): org.bukkit.event.HandlerList = HANDLERS
    }
}

class StoryEventRecordedEventPayload(
    val eventId: UUID,
    val createdAtMillis: Long,
    val source: AINPCEventSource,
    val storyEventId: Long,
    val scopeType: String,
    val scopeId: String,
    val regionId: String,
    val placeId: String,
    val eventType: String,
    val eventKey: String,
    val title: String,
    val description: String,
    val actorType: String,
    val actorId: String,
    val playerUuid: String,
    val npcId: String,
    metadata: Map<String, String>?
) {
    val metadata: Map<String, String> = Collections.unmodifiableMap(LinkedHashMap(metadata ?: emptyMap()))
}

class StoryEventRecordedEvent(
    val payload: StoryEventRecordedEventPayload
) : org.bukkit.event.Event() {
    override fun getHandlers(): org.bukkit.event.HandlerList = HANDLERS

    companion object {
        private val HANDLERS = org.bukkit.event.HandlerList()

        @JvmStatic
        fun getHandlerList(): org.bukkit.event.HandlerList = HANDLERS
    }
}
