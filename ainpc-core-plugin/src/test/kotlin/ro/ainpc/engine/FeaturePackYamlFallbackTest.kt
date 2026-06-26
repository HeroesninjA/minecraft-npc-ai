package ro.ainpc.engine

import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FeaturePackYamlFallbackTest {

    @Test
    fun questCategoryDefaultsToScenarioKind() {
        val config = YamlConfiguration()
        config.loadFromString("""
            scenarios:
              test_q:
                name: "Test"
                base_type: "QUEST"
                quest:
                  code: "t1"
                  scenario_kind: "hunting"
                  objectives:
                    obj1:
                      type: "kill_mob"
                      item: "ZOMBIE"
                      amount: 1
        """.trimIndent())
        val quest = parseSingle(config)
        assertEquals("hunting", quest.questCategory)
    }

    @Test
    fun completionModeDefaultsToAllObjectives() {
        val config = YamlConfiguration()
        config.loadFromString("""
            scenarios:
              test_q:
                name: "Test"
                base_type: "QUEST"
                quest:
                  code: "t1"
                  objectives:
                    obj1:
                      type: "collect_item"
                      item: "STONE"
                      amount: 1
        """.trimIndent())
        val quest = parseSingle(config)
        assertEquals("all_objectives", quest.questCompletionMode)
    }

    @Test
    fun trackingModeDefaultsToAuto() {
        val config = YamlConfiguration()
        config.loadFromString("""
            scenarios:
              test_q:
                name: "Test"
                base_type: "QUEST"
                quest:
                  code: "t1"
                  objectives:
                    obj1:
                      type: "collect_item"
                      item: "DIRT"
                      amount: 1
        """.trimIndent())
        val quest = parseSingle(config)
        assertEquals("auto", quest.questTrackingMode)
    }

    @Test
    fun acceptanceModeDefaultsToAuto() {
        val config = YamlConfiguration()
        config.loadFromString("""
            scenarios:
              test_q:
                name: "Test"
                base_type: "QUEST"
                quest:
                  code: "t1"
                  objectives:
                    obj1:
                      type: "collect_item"
                      item: "STICK"
                      amount: 1
        """.trimIndent())
        val quest = parseSingle(config)
        assertEquals("auto", quest.questAcceptanceMode)
    }

    @Test
    fun nextQuestDefaultsToEmpty() {
        val config = YamlConfiguration()
        config.loadFromString("""
            scenarios:
              test_q:
                name: "Test"
                base_type: "QUEST"
                quest:
                  code: "t1"
                  objectives:
                    obj1:
                      type: "collect_item"
                      item: "STONE"
                      amount: 1
        """.trimIndent())
        val quest = parseSingle(config)
        assertEquals("", quest.nextQuest)
    }

    @Test
    fun schemaVersionDefaultsToOne() {
        val pack = FeaturePackLoader.FeaturePack("test", "Test", "Test pack")
        assertEquals(1, pack.schemaVersion)
    }

    @Test
    fun questRepeatableDefaultsToFalse() {
        val config = YamlConfiguration()
        config.loadFromString("""
            scenarios:
              test_q:
                name: "Test"
                base_type: "QUEST"
                quest:
                  code: "t1"
                  objectives:
                    obj1:
                      type: "collect_item"
                      item: "STONE"
                      amount: 1
        """.trimIndent())
        val quest = parseSingle(config)
        assertEquals(false, quest.isQuestRepeatable)
    }

    private fun parseSingle(config: YamlConfiguration): FeaturePackLoader.ScenarioDefinition {
        val scenarios = linkedMapOf<String, FeaturePackLoader.ScenarioDefinition>()
        val pack = FeaturePackLoader.FeaturePack("pack", "Pack", "Test")
        FeaturePackYamlSupport.loadScenarios(
            pack, config.getConfigurationSection("scenarios")!!,
            scenarios, {}, { _, _ -> null }
        )
        return scenarios["pack:test_q"]!!
    }
}
