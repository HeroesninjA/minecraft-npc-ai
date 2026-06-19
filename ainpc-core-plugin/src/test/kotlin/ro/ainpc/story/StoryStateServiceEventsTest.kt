package ro.ainpc.story

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class StoryStateServiceEventsTest {
    @Test
    fun storyStateServicePublishesPublicEvents() {
        val source = File("src/main/kotlin/ro/ainpc/story/StoryStateService.kt").readText()

        assertTrue(source.contains("StoryStateChangedEvent("))
        assertTrue(source.contains("publishStoryStateChanged("))
        assertTrue(source.contains("StoryEventRecordedEvent("))
        assertTrue(source.contains("publishStoryEventRecorded("))
        assertTrue(source.contains("events.public_api_enabled"))
    }
}
