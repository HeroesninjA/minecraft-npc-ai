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

        assertTrue(config.getBoolean("demo.enabled"))
        assertTrue(config.getBoolean("features.gui"))
        assertTrue(config.getBoolean("features.quest"))
        assertTrue(config.getBoolean("features.progression"))
        assertTrue(config.getBoolean("features.story"))
        assertTrue(config.getBoolean("features.mapping"))
        assertTrue(config.getBoolean("features.routine"), "routine automation enabled for demo")
        assertTrue(config.getBoolean("features.simulation"), "simulation automation enabled for demo")
        assertTrue(config.getBoolean("features.ai"), "AI API feature must be enabled for runtime API access")
        assertTrue(config.getBoolean("features.generation"), "world/NPC generation must be enabled for demo")
        assertFalse(config.getBoolean("simulation.enabled"))
        assertFalse(config.getBoolean("routine.enabled"))
        assertFalse(config.getBoolean("dialog.passive_listen_enabled"))
        assertFalse(config.getBoolean("family.auto_generate"))
        assertFalse(config.getBoolean("villagers.auto_repopulate.enabled"))
        assertEquals("sqlite", config.getString("database.type"))
        assertEquals("ainpc_data.db", config.getString("database.sqlite.filename"))
        assertEquals("AINPC_MYSQL_PASSWORD", config.getString("database.mysql.password_env"))
        assertEquals(4096, config.getInt("world_admin.scan.blocks_per_tick"))
        assertTrue(config.getBoolean("observability.tracing.enabled"))
        assertEquals(64, config.getInt("observability.tracing.max_spans"))
    }
}
