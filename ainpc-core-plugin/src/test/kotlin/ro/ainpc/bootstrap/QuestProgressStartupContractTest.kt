package ro.ainpc.bootstrap

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class QuestProgressStartupContractTest {
    @Test
    fun questRestoreRunsBeforeCommandRegistrationAndScheduledWork() {
        val registry = source("bootstrap/ServiceRegistry.kt")
        val initialization = registry.substringAfter("fun initialize()").substringBefore("private fun phase1ConfigAndResources")
        val enginePhase = registry.substringAfter("private fun phase7Engines()").substringBefore("private fun phase8CommandsAndListeners")

        assertTrue(enginePhase.contains("scenarioEngine.loadPlayerQuests()"))
        assertTrue(enginePhase.contains("scenarioEngine = ScenarioEngine(plugin)"))
        listOf("phase7Engines()", "phase8CommandsAndListeners()", "phase9Starters()").forEach { phase ->
            assertTrue(initialization.contains(phase))
        }
        assertTrue(enginePhase.indexOf("scenarioEngine = ScenarioEngine(plugin)") < enginePhase.indexOf("scenarioEngine.loadPlayerQuests()"))
        assertTrue(initialization.indexOf("phase7Engines()") < initialization.indexOf("phase8CommandsAndListeners()"))
        assertTrue(initialization.indexOf("phase8CommandsAndListeners()") < initialization.indexOf("phase9Starters()"))
        assertFalse(source("bootstrap/SchedulerCoordinator.kt").contains("loadPlayerQuests()"))
    }

    @Test
    fun enginePropagatesRestorationFailureIntoStartupFailure() {
        val loadMethod = source("engine/ScenarioEngine.kt")
            .substringAfter("fun loadPlayerQuests()")
            .substringBefore("private fun refreshTrackedQuestProgress")

        assertTrue(loadMethod.contains("check(questProgressPersistence.loadPlayerQuests("))
    }

    private fun source(relativePath: String): String =
        Files.readString(Path.of("src/main/kotlin/ro/ainpc/$relativePath"))
}
