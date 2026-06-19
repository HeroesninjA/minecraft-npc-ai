package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class ScenarioEngineProgressionFailedEventTest {
    @Test
    fun scenarioEnginePublishesProgressionFailedEvent() {
        val source = File("src/main/kotlin/ro/ainpc/engine/ScenarioEngine.kt").readText()

        assertTrue(source.contains("ProgressionFailedEvent("))
        assertTrue(source.contains("publishProgressionFailed("))
        assertTrue(source.contains("events.public_api_enabled"))
    }
}
