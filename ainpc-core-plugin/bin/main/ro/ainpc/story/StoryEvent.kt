package ro.ainpc.story

import java.util.LinkedHashMap

class StoryEvent(
    private val idValue: Long,
    scopeType: String?,
    scopeId: String?,
    regionId: String?,
    placeId: String?,
    eventType: String?,
    eventKey: String?,
    title: String?,
    description: String?,
    payload: Map<String, String>?,
    actorType: String?,
    actorId: String?,
    playerUuid: String?,
    npcId: String?,
    private val createdAtValue: Long
) {
    private val scopeTypeValue: String = valueOrEmpty(scopeType)
    private val scopeIdValue: String = valueOrEmpty(scopeId)
    private val regionIdValue: String = valueOrEmpty(regionId)
    private val placeIdValue: String = valueOrEmpty(placeId)
    private val eventTypeValue: String = valueOrEmpty(eventType)
    private val eventKeyValue: String = valueOrEmpty(eventKey)
    private val titleValue: String = valueOrEmpty(title)
    private val descriptionValue: String = valueOrEmpty(description)
    private val payloadValue: Map<String, String> = copyMap(payload)
    private val actorTypeValue: String = valueOrEmpty(actorType)
    private val actorIdValue: String = valueOrEmpty(actorId)
    private val playerUuidValue: String = valueOrEmpty(playerUuid)
    private val npcIdValue: String = valueOrEmpty(npcId)

    fun id(): Long = idValue
    fun scopeType(): String = scopeTypeValue
    fun scopeId(): String = scopeIdValue
    fun regionId(): String = regionIdValue
    fun placeId(): String = placeIdValue
    fun eventType(): String = eventTypeValue
    fun eventKey(): String = eventKeyValue
    fun title(): String = titleValue
    fun description(): String = descriptionValue
    fun payload(): Map<String, String> = payloadValue
    fun actorType(): String = actorTypeValue
    fun actorId(): String = actorIdValue
    fun playerUuid(): String = playerUuidValue
    fun npcId(): String = npcIdValue
    fun createdAt(): Long = createdAtValue

    companion object {
        private fun valueOrEmpty(value: String?): String = value ?: ""

        private fun copyMap(values: Map<String, String>?): Map<String, String> {
            if (values.isNullOrEmpty()) {
                return emptyMap()
            }
            val copy = LinkedHashMap<String, String>()
            for ((key, value) in values) {
                if (key.isNotBlank()) {
                    copy[key] = valueOrEmpty(value)
                }
            }
            return copy.toMap()
        }
    }
}
