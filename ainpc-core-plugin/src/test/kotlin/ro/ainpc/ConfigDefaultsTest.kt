package ro.ainpc

import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.InputStreamReader

class ConfigDefaultsTest {
    @Test
    fun coreDefaultsKeepAutomaticWorldImpactOptIn() {
        val stream = javaClass.classLoader.getResourceAsStream("config.yml")
            ?: error("config.yml resource missing")
        val config = stream.use { input ->
            YamlConfiguration.loadConfiguration(InputStreamReader(input))
        }

        assertFalse(config.getBoolean("demo.enabled"))
        assertTrue(config.getBoolean("features.gui"))
        assertTrue(config.getBoolean("features.quest"))
        assertTrue(config.getBoolean("features.progression"))
        assertTrue(config.getBoolean("features.story"))
        assertTrue(config.getBoolean("features.mapping"))
        assertFalse(config.getBoolean("features.routine"), "routine automation must stay opt-in")
        assertFalse(config.getBoolean("features.simulation"), "simulation automation must stay opt-in")
        assertTrue(config.getBoolean("features.ai"), "AI API feature must be enabled for runtime API access")
        assertFalse(config.getBoolean("features.generation"), "world/NPC generation must stay opt-in")
        assertFalse(config.getBoolean("simulation.enabled"))
        assertFalse(config.getBoolean("routine.enabled"))
        assertFalse(config.getBoolean("dialog.passive_listen_enabled"))
        assertFalse(config.getBoolean("family.auto_generate"))
        assertFalse(config.getBoolean("villagers.auto_repopulate.enabled"))
        assertEquals("sqlite", config.getString("database.type"))
        assertEquals("ainpc_data.db", config.getString("database.sqlite.filename"))
        assertEquals("AINPC_MYSQL_PASSWORD", config.getString("database.mysql.password_env"))
    }
}
