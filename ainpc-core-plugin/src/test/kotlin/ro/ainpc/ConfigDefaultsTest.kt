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
        assertTrue(config.isSet("simulation.progress.enabled"))
        assertFalse(config.getBoolean("simulation.progress.enabled"))
        assertTrue(config.isSet("simulation.handoff.enabled"))
        assertFalse(config.getBoolean("simulation.handoff.enabled"))
        assertTrue(config.isSet("simulation.lod.enabled"))
        assertFalse(config.getBoolean("simulation.lod.enabled"))
        assertEquals(48.0, config.getDouble("simulation.lod.full_radius"))
        assertEquals(64.0, config.getDouble("simulation.lod.light_radius"))
        assertTrue(config.isSet("simulation.lod.perception.enabled"))
        assertFalse(config.getBoolean("simulation.lod.perception.enabled"))
        assertEquals(1200, config.getInt("simulation.lod.perception.light_refresh_ticks"))
        assertEquals(2.0, config.getDouble("simulation.lod.perception.max_reuse_distance"))
        assertTrue(config.isSet("simulation.decision.care_planner_shadow_enabled"))
        assertFalse(config.getBoolean("simulation.decision.care_planner_shadow_enabled"))
        assertTrue(config.isSet("simulation.decision.care_planner_execution_enabled"))
        assertFalse(config.getBoolean("simulation.decision.care_planner_execution_enabled"))
        assertEquals(32, config.getInt("simulation.decision.care_planner_max_per_cycle"))
        assertEquals(2.0, config.getDouble("simulation.decision.care_planner_budget_ms"))
        assertTrue(config.getBoolean("simulation.decision.probabilistic_enabled"))
        assertEquals(3, config.getInt("simulation.decision.max_choices"))
        assertEquals(15, config.getInt("simulation.decision.score_window"))
        assertEquals(8.0, config.getDouble("simulation.decision.temperature"))
        assertEquals(1, config.getInt("simulation.decision.repeat_history_size"))
        assertEquals(32, config.getInt("simulation.decision.statistics_window"))
        assertTrue(config.getConfigurationSection("simulation.decision.action_weights") != null)
        assertTrue(config.getConfigurationSection("simulation.decision.action_weights_by_occupation") == null)
        assertTrue(config.getConfigurationSection("simulation.decision.action_weights_by_time") == null)
        assertTrue(config.getConfigurationSection("simulation.decision.action_weights_by_weather") == null)
        assertTrue(config.getConfigurationSection("simulation.decision.action_cooldowns") == null)
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
