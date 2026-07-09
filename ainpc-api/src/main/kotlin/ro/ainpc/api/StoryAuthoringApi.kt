package ro.ainpc.api

interface StoryAuthoringApi {
    fun getAvailableTemplates(): List<String>
    fun getRecentEvents(scopeType: String, scopeId: String, limit: Int): List<StoryEventSummary>
    fun hasPendingEvents(scopeType: String, scopeId: String): Boolean
}

data class StoryEventSummary(
    val eventKey: String,
    val eventType: String,
    val title: String,
    val description: String,
    val createdAt: Long,
)
