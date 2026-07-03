package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class ScenarioEngineProgressionEventsTest {
    @Test
    fun scenarioEnginePublishesProgressionLifecycleEvents() {
        val scenarioSource = File("src/main/kotlin/ro/ainpc/engine/ScenarioEngine.kt").readText()
        val publisherSource = File("src/main/kotlin/ro/ainpc/engine/QuestProgressionLifecyclePublisher.kt").readText()

        assertTrue(scenarioSource.contains("progressionLifecyclePublisher.publishAccepted("))
        assertTrue(scenarioSource.contains("progressionLifecyclePublisher.publishDeclined("))
        assertTrue(scenarioSource.contains("progressionLifecyclePublisher.publishAbandoned("))
        assertTrue(scenarioSource.contains("progressionLifecyclePublisher.publishCompleted("))
        assertTrue(scenarioSource.contains("emitProgressionObjectiveProgress("))
        assertTrue(publisherSource.contains("ProgressionAcceptedEvent("))
        assertTrue(publisherSource.contains("ProgressionDeclinedEvent("))
        assertTrue(publisherSource.contains("ProgressionAbandonedEvent("))
        assertTrue(publisherSource.contains("ProgressionCompletedEvent("))
        assertTrue(publisherSource.contains("Bukkit.getPluginManager().callEvent(event)"))
        assertTrue(publisherSource.contains("events.public_api_enabled"))
    }
}
