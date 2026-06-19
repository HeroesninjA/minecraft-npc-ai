package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class ScenarioEngineProgressionEventsTest {
    @Test
    fun scenarioEnginePublishesProgressionLifecycleEvents() {
        val source = File("src/main/kotlin/ro/ainpc/engine/ScenarioEngine.kt").readText()

        assertTrue(source.contains("ProgressionAcceptedEvent("))
        assertTrue(source.contains("ProgressionDeclinedEvent("))
        assertTrue(source.contains("ProgressionAbandonedEvent("))
        assertTrue(source.contains("ProgressionCompletedEvent("))
        assertTrue(source.contains("ProgressionObjectiveProgressEvent("))
        assertTrue(source.contains("emitProgressionObjectiveProgress("))
        assertTrue(source.contains("ProgressionOfferEvent("))
        assertTrue(source.contains("publishProgressionOffered("))
        assertTrue(source.contains("ProgressionStageChangedEvent("))
        assertTrue(source.contains("publishProgressionStageChanged("))
        assertTrue(source.contains("ProgressionTrackingChangedEvent("))
        assertTrue(source.contains("publishProgressionTrackingChanged("))
        assertTrue(source.contains("ProgressionAnchorBoundEvent("))
        assertTrue(source.contains("publishProgressionAnchorsBound("))
        assertTrue(source.contains("Bukkit.getPluginManager().callEvent(event)"))
        assertTrue(source.contains("events.public_api_enabled"))
    }
}
