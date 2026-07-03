package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class ScenarioEngineStoryActionEventsTest {
    @Test
    fun scenarioEnginePublishesStoryActionEvents() {
        val scenarioSource = File("src/main/kotlin/ro/ainpc/engine/ScenarioEngine.kt").readText()
        val publisherSource = File("src/main/kotlin/ro/ainpc/engine/QuestStoryActionEventPublisher.kt").readText()

        assertTrue(scenarioSource.contains("questStoryActionEventPublisher.publish("))
        assertTrue(scenarioSource.contains("applyQuestStoryActions("))
        assertTrue(publisherSource.contains("StoryActionAppliedEvent("))
        assertTrue(publisherSource.contains("Bukkit.getPluginManager().callEvent("))
        assertTrue(publisherSource.contains("events.public_api_enabled"))
    }
}
