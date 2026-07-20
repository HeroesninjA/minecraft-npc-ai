package ro.ainpc.platform

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.api.StoryEventSummary
import ro.ainpc.story.StoryAuthoringService

class StoryAuthoringApiAdapterTest {
    @Test
    fun `returns template identifiers in provider order`() {
        val adapter = StoryAuthoringApiAdapter(
            templatesProvider = {
                listOf(
                    template("village_celebration"),
                    template("merchant_arrival"),
                )
            },
            eventsProvider = { _, _, _ -> emptyList() },
            pendingEventsProvider = { _, _ -> false },
        )

        assertEquals(
            listOf("village_celebration", "merchant_arrival"),
            adapter.getAvailableTemplates(),
        )
    }

    @Test
    fun `delegates scoped event lookup and maps public summary`() {
        var requestedScopeType = ""
        var requestedScopeId = ""
        var requestedLimit = 0
        val adapter = StoryAuthoringApiAdapter(
            templatesProvider = { emptyList() },
            eventsProvider = { scopeType, scopeId, limit ->
                requestedScopeType = scopeType
                requestedScopeId = scopeId
                requestedLimit = limit
                listOf(
                    StoryAuthoringService.StoryEventInfo(
                        id = 42L,
                        scopeType = scopeType,
                        scopeId = scopeId,
                        eventKey = "festival_start",
                        eventType = "celebration",
                        title = "Festival",
                        description = "The village celebrates.",
                        createdAt = 1234L,
                    ),
                )
            },
            pendingEventsProvider = { _, _ -> false },
        )

        val events = adapter.getRecentEvents("region", "demo_sat", 7)

        assertEquals("region", requestedScopeType)
        assertEquals("demo_sat", requestedScopeId)
        assertEquals(7, requestedLimit)
        assertEquals(
            listOf(
                StoryEventSummary(
                    eventKey = "festival_start",
                    eventType = "celebration",
                    title = "Festival",
                    description = "The village celebrates.",
                    createdAt = 1234L,
                ),
            ),
            events,
        )
    }

    @Test
    fun `delegates scoped pending lookup to dedicated provider`() {
        var requestedScopeType = ""
        var requestedScopeId = ""
        val adapter = StoryAuthoringApiAdapter(
            templatesProvider = { emptyList() },
            eventsProvider = { _, _, _ -> error("Recent events must not emulate pending events") },
            pendingEventsProvider = { scopeType, scopeId ->
                requestedScopeType = scopeType
                requestedScopeId = scopeId
                true
            },
        )

        assertTrue(adapter.hasPendingEvents("region", "demo_sat"))
        assertEquals("region", requestedScopeType)
        assertEquals("demo_sat", requestedScopeId)
    }

    private fun template(id: String): StoryAuthoringService.StoryTemplate =
        StoryAuthoringService.StoryTemplate(
            id = id,
            name = id,
            description = "",
            eventType = "story_event",
            suggestedTitle = "",
            suggestedDescription = "",
        )
}
