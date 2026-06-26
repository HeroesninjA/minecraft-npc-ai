package ro.ainpc.engine

import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FeaturePackImportObjectivesTest {

    @Test
    fun importObjectivesFromCopiesObjectives() {
        val config = YamlConfiguration()
        config.loadFromString("""
            scenarios:
              source_quest:
                name: "Quest Sursa"
                base_type: "QUEST"
                quest:
                  code: "source_q"
                  objectives:
                    obj1:
                      type: "collect_item"
                      item: "STONE"
                      amount: 5
                    obj2:
                      type: "talk_to_npc"
                      item: "npc:vanator"
                      amount: 1
              target_quest:
                name: "Quest Tinta"
                base_type: "QUEST"
                quest:
                  code: "target_q"
                  import_objectives_from: "source_quest"
                  objectives:
                    obj3:
                      type: "kill_mob"
                      item: "ZOMBIE"
                      amount: 3
        """.trimIndent())
        val scenarios = linkedMapOf<String, FeaturePackLoader.ScenarioDefinition>()
        val pack = FeaturePackLoader.FeaturePack("test", "Test", "")
        FeaturePackYamlSupport.loadScenarios(
            pack, config.getConfigurationSection("scenarios")!!,
            scenarios, {}, { _, _ -> null }
        )
        val target = scenarios["test:target_quest"]
        assertTrue(target != null)
        assertEquals(3, target!!.objectives.size)
        val importedObj = target.objectives.find { it.type == "collect_item" }
        assertTrue(importedObj != null)
        assertEquals("STONE", importedObj!!.itemId)
        val localObj = target.objectives.find { it.type == "kill_mob" }
        assertTrue(localObj != null)
        assertEquals("ZOMBIE", localObj!!.itemId)
    }

    @Test
    fun importObjectivesFromMissingSourceIsSkipped() {
        val config = YamlConfiguration()
        config.loadFromString("""
            scenarios:
              test_quest:
                name: "Quest Test"
                base_type: "QUEST"
                quest:
                  code: "test_q"
                  import_objectives_from: "nonexistent_quest"
                  objectives:
                    obj1:
                      type: "collect_item"
                      item: "DIRT"
                      amount: 1
        """.trimIndent())
        val scenarios = linkedMapOf<String, FeaturePackLoader.ScenarioDefinition>()
        val pack = FeaturePackLoader.FeaturePack("test", "Test", "")
        FeaturePackYamlSupport.loadScenarios(
            pack, config.getConfigurationSection("scenarios")!!,
            scenarios, {}, { _, _ -> null }
        )
        val quest = scenarios["test:test_quest"]
        assertTrue(quest != null)
        assertEquals(1, quest!!.objectives.size)
    }
}
