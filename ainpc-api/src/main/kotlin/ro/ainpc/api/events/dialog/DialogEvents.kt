package ro.ainpc.api.events.dialog

import ro.ainpc.api.events.AINPCEventSource
import java.util.Collections
import java.util.LinkedHashMap
import java.util.UUID

class DialogSessionStartedEventPayload(
    val eventId: UUID,
    val createdAtMillis: Long,
    val source: AINPCEventSource,
    val conversationId: String,
    val playerUuid: UUID,
    val playerName: String,
    val npcId: String,
    val npcUuid: UUID?,
    val npcName: String,
    val directAddress: Boolean,
    val explicitConversation: Boolean,
    val triggerReason: String,
    val nearbyNpcCount: Int,
    val distanceToNpc: Double,
    metadata: Map<String, String>?
) {
    val metadata: Map<String, String> = Collections.unmodifiableMap(LinkedHashMap(metadata ?: emptyMap()))
}

class DialogSessionStartedEvent(
    val payload: DialogSessionStartedEventPayload
) : org.bukkit.event.Event(), org.bukkit.event.Cancellable {
    private var cancelled = false
    override fun isCancelled(): Boolean = cancelled
    override fun setCancelled(cancel: Boolean) { cancelled = cancel }
    override fun getHandlers(): org.bukkit.event.HandlerList = HANDLERS

    companion object {
        private val HANDLERS = org.bukkit.event.HandlerList()
        @JvmStatic fun getHandlerList(): org.bukkit.event.HandlerList = HANDLERS
    }
}

class DialogMessageReceivedEventPayload(
    val eventId: UUID,
    val createdAtMillis: Long,
    val source: AINPCEventSource,
    val conversationId: String,
    val playerUuid: UUID,
    val playerName: String,
    val npcId: String,
    val npcUuid: UUID?,
    val npcName: String,
    val message: String,
    val directAddress: Boolean,
    val explicitConversation: Boolean,
    val triggerReason: String,
    val nearbyNpcCount: Int,
    val distanceToNpc: Double,
    metadata: Map<String, String>?
) {
    val metadata: Map<String, String> = Collections.unmodifiableMap(LinkedHashMap(metadata ?: emptyMap()))
}

class DialogMessageReceivedEvent(
    val payload: DialogMessageReceivedEventPayload
) : org.bukkit.event.Event(), org.bukkit.event.Cancellable {
    private var cancelled = false
    override fun isCancelled(): Boolean = cancelled
    override fun setCancelled(cancel: Boolean) { cancelled = cancel }
    override fun getHandlers(): org.bukkit.event.HandlerList = HANDLERS

    companion object {
        private val HANDLERS = org.bukkit.event.HandlerList()
        @JvmStatic fun getHandlerList(): org.bukkit.event.HandlerList = HANDLERS
    }
}

class DialogIntentResolvedEventPayload(
    val eventId: UUID,
    val createdAtMillis: Long,
    val source: AINPCEventSource,
    val conversationId: String,
    val playerUuid: UUID,
    val playerName: String,
    val npcId: String,
    val npcUuid: UUID?,
    val npcName: String,
    val message: String,
    val normalizedMessage: String,
    val intentKey: String,
    val directAddress: Boolean,
    val explicitConversation: Boolean,
    val triggerReason: String,
    val nearbyNpcCount: Int,
    val distanceToNpc: Double,
    metadata: Map<String, String>?
) {
    val metadata: Map<String, String> = Collections.unmodifiableMap(LinkedHashMap(metadata ?: emptyMap()))
}

class DialogIntentResolvedEvent(
    val payload: DialogIntentResolvedEventPayload
) : org.bukkit.event.Event() {
    override fun getHandlers(): org.bukkit.event.HandlerList = HANDLERS
    companion object {
        private val HANDLERS = org.bukkit.event.HandlerList()
        @JvmStatic fun getHandlerList(): org.bukkit.event.HandlerList = HANDLERS
    }
}

class DialogResponseGeneratedEventPayload(
    val eventId: UUID,
    val createdAtMillis: Long,
    val source: AINPCEventSource,
    val conversationId: String,
    val playerUuid: UUID,
    val playerName: String,
    val npcId: String,
    val npcUuid: UUID?,
    val npcName: String,
    val responseStatus: String,
    val responsePreview: String,
    val directAddress: Boolean,
    val explicitConversation: Boolean,
    val triggerReason: String,
    metadata: Map<String, String>?
) {
    val metadata: Map<String, String> = Collections.unmodifiableMap(LinkedHashMap(metadata ?: emptyMap()))
}

class DialogResponseGeneratedEvent(
    val payload: DialogResponseGeneratedEventPayload
) : org.bukkit.event.Event() {
    override fun getHandlers(): org.bukkit.event.HandlerList = HANDLERS
    companion object {
        private val HANDLERS = org.bukkit.event.HandlerList()
        @JvmStatic fun getHandlerList(): org.bukkit.event.HandlerList = HANDLERS
    }
}

class DialogSessionEndedEventPayload(
    val eventId: UUID,
    val createdAtMillis: Long,
    val source: AINPCEventSource,
    val conversationId: String,
    val playerUuid: UUID,
    val playerName: String,
    val npcId: String,
    val npcUuid: UUID?,
    val npcName: String,
    val endReason: String,
    metadata: Map<String, String>?
) {
    val metadata: Map<String, String> = Collections.unmodifiableMap(LinkedHashMap(metadata ?: emptyMap()))
}

class DialogSessionEndedEvent(
    val payload: DialogSessionEndedEventPayload
) : org.bukkit.event.Event() {
    override fun getHandlers(): org.bukkit.event.HandlerList = HANDLERS
    companion object {
        private val HANDLERS = org.bukkit.event.HandlerList()
        @JvmStatic fun getHandlerList(): org.bukkit.event.HandlerList = HANDLERS
    }
}
