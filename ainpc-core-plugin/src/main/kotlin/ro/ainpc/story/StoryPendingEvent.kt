package ro.ainpc.story

data class StoryPendingEvent(
    val id: Long,
    val scopeType: String,
    val scopeId: String,
    val regionId: String,
    val placeId: String,
    val eventType: String,
    val eventKey: String,
    val title: String,
    val description: String,
    val payload: Map<String, String>,
    val actorType: String,
    val actorId: String,
    val playerUuid: String,
    val npcId: String,
    val queuedAt: Long,
)
