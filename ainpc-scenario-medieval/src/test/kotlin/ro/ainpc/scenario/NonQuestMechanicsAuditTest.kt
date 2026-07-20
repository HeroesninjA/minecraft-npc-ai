package ro.ainpc.scenario

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.yaml.snakeyaml.Yaml
import java.io.File

class NonQuestMechanicsAuditTest {

    data class MechanicSlice(
        val baseType: String,
        val displayName: String,
        val expectedMinScenarios: Int,
    )

    companion object {
        @JvmStatic
        fun nonQuestMechanics(): List<MechanicSlice> = listOf(
            MechanicSlice("TRADE_DEAL", "Contracte/Afaceri", 1),
            MechanicSlice("DUTY", "Sarcini", 1),
            MechanicSlice("BOUNTY", "Bounty-uri", 1),
            MechanicSlice("WORLD_EVENT", "Evenimente", 1),
            MechanicSlice("TUTORIAL", "Tutoriale", 1),
            MechanicSlice("RITUAL", "Ritualuri", 1),
        )
    }

    @ParameterizedTest
    @MethodSource("nonQuestMechanics")
    fun `each non-quest mechanic has scenarios`(slice: MechanicSlice) {
        val packDir = File("src/main/resources/packs")
        val scenarios = collectScenariosOfType(packDir, slice.baseType)

        assertTrue(
            scenarios.size >= slice.expectedMinScenarios,
            "${slice.displayName} ($slice.baseType) trebuie sa aiba minim ${slice.expectedMinScenarios} scenariu. Gasite: ${scenarios.size}"
        )

        for ((file, id, scenario) in scenarios) {
            val questSection = scenario["quest"] as? Map<*, *>
            assertNotNull(questSection, "$id in $file trebuie sa aiba sectiunea quest")
            val objectives = questSection?.get("objectives") as? Map<*, *>
            assertNotNull(objectives, "$id in $file trebuie sa aiba obiective")
            assertTrue(objectives!!.isNotEmpty(), "$id in $file trebuie sa aiba cel putin un obiectiv")

            val rewards = questSection?.get("rewards") as? Map<*, *>
            assertNotNull(rewards, "$id in $file trebuie sa aiba sectiunea rewards")
            assertTrue(rewards!!.isNotEmpty(), "$id in $file trebuie sa aiba cel putin o recompensa")
        }
    }

    @Test
    fun `non-quest filters are complete`() {
        val expectedFilters = mapOf(
            "TRADE_DEAL" to "contract",
            "DUTY" to "duty",
            "BOUNTY" to "bounty",
            "WORLD_EVENT" to "event",
            "TUTORIAL" to "tutorial",
            "RITUAL" to "ritual",
        )

        for ((baseType, expectedFilter) in expectedFilters) {
            val filterText = readQuestLogGuiFilterSource()
            assertTrue(
                filterText.contains("\"$expectedFilter\""),
                "QuestLogGuiFilter trebuie sa contina filtrul '$expectedFilter' pentru $baseType"
            )
        }
    }

    @Test
    fun `each non-quest base type maps to ScenarioType`() {
        val scenarioTypeSource = File("../ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/ScenarioType.kt").readText()
        for (slice in nonQuestMechanics()) {
            assertTrue(
                scenarioTypeSource.contains(slice.baseType),
                "ScenarioType trebuie sa contina ${slice.baseType}"
            )
        }
    }

    @Test
    fun `non-quest mechanics have progression aliases`() {
        val aliasesSource = File("../ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommand.kt").readText()
        for (slice in nonQuestMechanics()) {
            assertTrue(
                aliasesSource.contains("\"${slice.baseType}\""),
                "AINPCCommand trebuie sa aiba alias de progresie pentru ${slice.baseType}"
            )
        }
    }

    @Test
    fun `QuestCreateGui supports all non-quest base types`() {
        val guiSource = File("../ainpc-core-plugin/src/main/kotlin/ro/ainpc/gui/screens/QuestCreateGui.kt").readText()
        for (slice in nonQuestMechanics()) {
            assertTrue(
                guiSource.contains("\"${slice.baseType}\""),
                "QuestCreateGui trebuie sa expuna ${slice.baseType}"
            )
        }
    }

    private fun collectScenariosOfType(
        packDir: File,
        baseType: String
    ): List<Triple<String, String, Map<String, Any>>> {
        val results = mutableListOf<Triple<String, String, Map<String, Any>>>()
        val yaml = Yaml()
        val files = packDir.listFiles()?.filter {
            it.name.endsWith(".yml") || it.name.endsWith(".yaml")
        } ?: return results

        for (file in files.sortedBy { it.name }) {
            val root = yaml.load<Map<String, Any>>(file.readText()) ?: continue
            val scenarios = root["scenarios"] as? Map<String, Any> ?: continue
            for ((id, scenarioData) in scenarios) {
                val scenario = scenarioData as? Map<String, Any> ?: continue
                val bt = scenario["base_type"]?.toString()?.trim() ?: "QUEST"
                if (bt.equals(baseType, ignoreCase = true)) {
                    results.add(Triple(file.name, id, scenario))
                }
            }
        }
        return results
    }

    private fun readQuestLogGuiFilterSource(): String {
        return File("../ainpc-core-plugin/src/main/kotlin/ro/ainpc/gui/QuestLogGuiFilter.kt").readText()
    }
}
