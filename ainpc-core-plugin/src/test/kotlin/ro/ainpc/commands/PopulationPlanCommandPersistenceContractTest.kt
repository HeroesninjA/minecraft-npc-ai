package ro.ainpc.commands

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class PopulationPlanCommandPersistenceContractTest {
    @Test
    fun populationCommandsUsePersistentPlanIdsWithoutSpawnIntegration() {
        val commandSource = File("src/main/kotlin/ro/ainpc/commands/AINPCCommand.kt").readText()
        val tabSource = File("src/main/kotlin/ro/ainpc/commands/AINPCTabCompleter.kt").readText()

        assertFalse(commandSource.contains("lastPopulationPlan"))
        assertTrue(commandSource.contains("populationPlanRepository.save(plan)"))
        assertTrue(commandSource.contains("populationPlanRepository.find(args[2])"))
        assertTrue(commandSource.contains("populationPlanRepository.select(args[2])"))
        assertTrue(commandSource.contains("\"list\" -> handlePopulationList(sender, args)"))
        assertTrue(commandSource.contains("spawn-ul curent regenereaza alocari separate si nu executa acest plan"))
        assertTrue(tabSource.contains("listOf(\"plan\", \"list\", \"inspect\", \"select\", \"stats\")"))
    }

    @Test
    fun generatedPlanIdsUseTheFullUnsignedSeedHash() {
        val generatorSource = File("src/main/kotlin/ro/ainpc/spawn/NarrativeGenerator.kt").readText()

        assertTrue(generatorSource.contains("Integer.toUnsignedString(effectiveSeed.hashCode(), 16).padStart(8, '0')"))
        assertFalse(generatorSource.contains("abs(effectiveSeed.hashCode()) % 1000"))
    }
}
