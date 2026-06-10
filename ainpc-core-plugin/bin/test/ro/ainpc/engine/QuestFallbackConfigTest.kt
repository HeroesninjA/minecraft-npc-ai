package ro.ainpc.engine

import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.io.InputStreamReader

class QuestFallbackConfigTest {
    @Test
    fun coreQuestFallbackConfigDoesNotShipThematicProfessionProfiles() {
        val stream = javaClass.classLoader.getResourceAsStream("quests.yml")
        assertNotNull(stream)

        val config = stream.use { input ->
            YamlConfiguration.loadConfiguration(InputStreamReader(input))
        }
        val fallbacks = config.getConfigurationSection("profession_fallbacks")
        assertNotNull(fallbacks)

        assertFalse(fallbacks!!.contains("blacksmith"))
        assertFalse(fallbacks.contains("farmer"))
        assertFalse(fallbacks.contains("guard"))
    }
}
