package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class ScenarioEngineQuestCooldownTest {
    @Test
    fun scenarioEngineEnforcesQuestCooldown() {
        val source = File("src/main/kotlin/ro/ainpc/engine/ScenarioEngine.kt").readText()

        assertTrue(source.contains("questCooldownSeconds"))
        assertTrue(source.contains("questRepeatable"))
        assertTrue(source.contains("remaining"))
        assertTrue(source.contains("Mai asteapta"))
    }
}
