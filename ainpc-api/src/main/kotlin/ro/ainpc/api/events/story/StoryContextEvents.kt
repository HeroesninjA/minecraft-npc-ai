package ro.ainpc.api.events.story

import ro.ainpc.api.events.AINPCEventSource
import java.util.Collections
import java.util.LinkedHashMap
import java.util.UUID

class StoryContextBuiltEventPayload(
    val eventId: UUID,
    val createdAtMillis: Long,
    val source: AINPCEventSource,
    val buildMode: String,
    val subjectNpcName: String,
    val subjectNpcOccupation: String,
    val playerName: String,
    val hasWorldContext: Boolean,
    val regionId: String,
    val placeId: String,
    val signalCount: Int,
    signalEntries: List<String>?,
    warnings: List<String>?,
    metadata: Map<String, String>?
) {
    private val safeSignalEntries = signalEntries?.toList() ?: emptyList<String>()
    private val safeWarnings = warnings?.toList() ?: emptyList<String>()
    val signals: List<String> = Collections.unmodifiableList(safeSignalEntries)
    val warnings: List<String> = Collections.unmodifiableList(safeWarnings)
    val metadata: Map<String, String> = Collections.unmodifiableMap(LinkedHashMap(metadata ?: emptyMap()))
}

class StoryContextBuiltEvent(
    val payload: StoryContextBuiltEventPayload
) : org.bukkit.event.Event() {
    override fun getHandlers(): org.bukkit.event.HandlerList = HANDLERS

    companion object {
        private val HANDLERS = org.bukkit.event.HandlerList()

        @JvmStatic
        fun getHandlerList(): org.bukkit.event.HandlerList = HANDLERS
    }
}

class StorySignalCollectedEventPayload(
    val eventId: UUID,
    val createdAtMillis: Long,
    val source: AINPCEventSource,
    val buildMode: String,
    val worldRegionId: String,
    val worldPlaceId: String,
    val signalCount: Int,
    signalEntries: List<String>?,
    metadata: Map<String, String>?
) {
    private val safeSignalEntries = signalEntries?.toList() ?: emptyList<String>()
    val signals: List<String> = Collections.unmodifiableList(safeSignalEntries)
    val metadata: Map<String, String> = Collections.unmodifiableMap(LinkedHashMap(metadata ?: emptyMap()))
}

class StorySignalCollectedEvent(
    val payload: StorySignalCollectedEventPayload
) : org.bukkit.event.Event() {
    override fun getHandlers(): org.bukkit.event.HandlerList = HANDLERS

    companion object {
        private val HANDLERS = org.bukkit.event.HandlerList()

        @JvmStatic
        fun getHandlerList(): org.bukkit.event.HandlerList = HANDLERS
    }
}
