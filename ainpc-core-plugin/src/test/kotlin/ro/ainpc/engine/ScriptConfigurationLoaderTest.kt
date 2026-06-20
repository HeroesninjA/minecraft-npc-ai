package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class ScriptConfigurationLoaderTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun loadsYamlQuestConfiguration() {
        val file = tempDir.resolve("quests.yml").toFile()
        file.writeText(
            """
            type: quest
            version: 1
            spec:
              trigger:
                - quest.started
              mapping:
                place: tag:market
            """.trimIndent()
        )

        val config = ScriptConfigurationLoader.loadConfiguration(file)

        assertEquals("quest", config.getString("type"))
        assertEquals(1, config.getInt("version"))
        assertTrue(config.getStringList("spec.trigger").contains("quest.started"))
        assertEquals("tag:market", config.getString("spec.mapping.place"))
    }

    @Test
    fun loadsJsonQuestConfiguration() {
        val file = tempDir.resolve("quests.json").toFile()
        file.writeText(
            """
            {
              "type": "quest",
              "version": 1,
              "spec": {
                "trigger": ["quest.started"],
                "mapping": {
                  "place": "tag:market"
                }
              }
            }
            """.trimIndent()
        )

        val config = ScriptConfigurationLoader.loadConfiguration(file)

        assertEquals("quest", config.getString("type"))
        assertEquals(1, config.getInt("version"))
        assertTrue(config.getStringList("spec.trigger").contains("quest.started"))
        assertEquals("tag:market", config.getString("spec.mapping.place"))
    }

    @Test
    fun mergesOverlaySectionIntoYamlConfiguration() {
        val target = YamlConfiguration()
        target.loadFromString(
            """
            world_admin:
              enabled: false
              regions:
                old_region:
                  name: "Old"
            """.trimIndent()
        )

        val overlay = YamlConfiguration()
        overlay.loadFromString(
            """
            world_admin:
              enabled: true
              auto_index:
                enabled: true
              regions:
                satul_central:
                  name: "Satul Central"
                  type: "settlement"
            """.trimIndent()
        )

        ScriptConfigurationLoader.mergeSection(
            target,
            "world_admin",
            overlay.getConfigurationSection("world_admin")
        )

        assertTrue(target.getBoolean("world_admin.enabled"))
        assertTrue(target.getBoolean("world_admin.auto_index.enabled"))
        assertEquals("Satul Central", target.getString("world_admin.regions.satul_central.name"))
        assertEquals("settlement", target.getString("world_admin.regions.satul_central.type"))
        val worldAdminSection = target.getConfigurationSection("world_admin")
        assertNotNull(worldAdminSection)
        assertTrue(worldAdminSection!!.getKeys(false).contains("regions"))
    }
}
