package ro.ainpc.story

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class StoryContextServiceEventsTest {
    @Test
    fun storyContextServicePublishesPublicEvents() {
        val source = File("src/main/kotlin/ro/ainpc/story/StoryContextService.kt").readText()

        assertTrue(source.contains("StoryContextBuiltEvent("))
        assertTrue(source.contains("publishStoryContextBuilt("))
        assertTrue(source.contains("StorySignalCollectedEvent("))
        assertTrue(source.contains("publishStorySignalsCollected("))
        assertTrue(source.contains("events.public_api_enabled"))
    }
}
