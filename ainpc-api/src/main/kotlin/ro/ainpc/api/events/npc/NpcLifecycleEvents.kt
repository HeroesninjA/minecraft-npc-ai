package ro.ainpc.api.events.npc

import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import ro.ainpc.api.events.AINPCEventSource
import java.util.Collections
import java.util.LinkedHashMap
import java.util.UUID

/**
 * Ainpcdiscoveredeventpayload.
 */
class AINPCDiscoveredEventPayload(
    val eventId: UUID,
    val createdAtMillis: Long,
    val source: AINPCEventSource,
    val npcId: String,
    val npcUuid: UUID,
    val npcName: String,
    val worldName: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val discoveryReason: String,
    metadata: Map<String, String>?
) {
    val metadata: Map<String, String> = Collections.unmodifiableMap(LinkedHashMap(metadata ?: emptyMap()))
}

/**
 * Ainpcdiscoveredevent.
 */
class AINPCDiscoveredEvent(
    val payload: AINPCDiscoveredEventPayload
) : Event() {
    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}

/**
 * Ainpcspawnedeventpayload.
 */
class AINPCSpawnedEventPayload(
    val eventId: UUID,
    val createdAtMillis: Long,
    val source: AINPCEventSource,
    val npcId: String,
    val npcUuid: UUID,
    val npcName: String,
    val worldName: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val occupation: String?,
    metadata: Map<String, String>?
) {
    val metadata: Map<String, String> = Collections.unmodifiableMap(LinkedHashMap(metadata ?: emptyMap()))
}

/**
 * Ainpcspawnedevent.
 */
class AINPCSpawnedEvent(
    val payload: AINPCSpawnedEventPayload
) : Event(), org.bukkit.event.Cancellable {
    private var cancelled = false
    override fun isCancelled(): Boolean = cancelled
    override fun setCancelled(cancel: Boolean) { cancelled = cancel }
    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}

/**
 * Ainpcprofilerefreshedeventpayload.
 */
class AINPCProfileRefreshedEventPayload(
    val eventId: UUID,
    val createdAtMillis: Long,
    val source: AINPCEventSource,
    val npcId: String,
    val npcUuid: UUID,
    val npcName: String,
    val occupation: String?,
    val profileSource: String?,
    val profileVersion: Int,
    val backstory: String?,
    val refreshReason: String,
    metadata: Map<String, String>?
) {
    val metadata: Map<String, String> = Collections.unmodifiableMap(LinkedHashMap(metadata ?: emptyMap()))
}

/**
 * Ainpcprofilerefreshedevent.
 */
class AINPCProfileRefreshedEvent(
    val payload: AINPCProfileRefreshedEventPayload
) : Event() {
    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}

/**
 * Ainpcemotionchangedeventpayload.
 */
class AINPCEmotionChangedEventPayload(
    val eventId: UUID,
    val createdAtMillis: Long,
    val source: AINPCEventSource,
    val npcId: String,
    val npcUuid: UUID,
    val npcName: String,
    val emotion: String,
    val intensity: Double,
    val previousEmotion: String?,
    val triggerType: String,
    metadata: Map<String, String>?
) {
    val metadata: Map<String, String> = Collections.unmodifiableMap(LinkedHashMap(metadata ?: emptyMap()))
}

/**
 * Ainpcemotionchangedevent.
 */
class AINPCEmotionChangedEvent(
    val payload: AINPCEmotionChangedEventPayload
) : Event() {
    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}

/**
 * Ainpcmemoryrecordedeventpayload.
 */
class AINPCMemoryRecordedEventPayload(
    val eventId: UUID,
    val createdAtMillis: Long,
    val source: AINPCEventSource,
    val npcId: String,
    val npcUuid: UUID?,
    val npcName: String,
    val playerUuid: UUID?,
    val playerName: String?,
    val memoryType: String,
    val emotionalImpact: Double,
    val importance: Int,
    metadata: Map<String, String>?
) {
    val metadata: Map<String, String> = Collections.unmodifiableMap(LinkedHashMap(metadata ?: emptyMap()))
}

/**
 * Ainpcmemoryrecordedevent.
 */
class AINPCMemoryRecordedEvent(
    val payload: AINPCMemoryRecordedEventPayload
) : Event() {
    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}

/**
 * Ainpcroutinechangedeventpayload.
 */
class AINPCRoutineChangedEventPayload(
    val eventId: UUID,
    val createdAtMillis: Long,
    val source: AINPCEventSource,
    val npcId: String,
    val npcUuid: UUID,
    val npcName: String,
    val previousActivity: String?,
    val newActivity: String,
    val worldTime: Long,
    metadata: Map<String, String>?
) {
    val metadata: Map<String, String> = Collections.unmodifiableMap(LinkedHashMap(metadata ?: emptyMap()))
}

/**
 * Ainpcroutinechangedevent.
 */
class AINPCRoutineChangedEvent(
    val payload: AINPCRoutineChangedEventPayload
) : Event() {
    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}

/**
 * Ainpcdeatheventpayload.
 */
class AINPCDeathEventPayload(
    val eventId: UUID,
    val createdAtMillis: Long,
    val source: AINPCEventSource,
    val npcId: String,
    val npcUuid: UUID?,
    val npcName: String,
    val worldName: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val killerType: String?,
    metadata: Map<String, String>?
) {
    val metadata: Map<String, String> = Collections.unmodifiableMap(LinkedHashMap(metadata ?: emptyMap()))
}

/**
 * Ainpcdeathevent.
 */
class AINPCDeathEvent(
    val payload: AINPCDeathEventPayload
) : Event() {
    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}
