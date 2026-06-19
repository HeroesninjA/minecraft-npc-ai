package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class ScenarioEngineStoryActionEventsTest {
    @Test
    fun scenarioEnginePublishesStoryActionEvents() {
        val source = File("src/main/kotlin/ro/ainpc/engine/ScenarioEngine.kt").readText()

        assertTrue(source.contains("StoryActionAppliedEvent("))
        assertTrue(source.contains("publishStoryActionApplied("))
        assertTrue(source.contains("applyQuestStoryActions("))
        assertTrue(source.contains("set_story_state"))
        assertTrue(source.contains("record_story_event"))
    }
}
