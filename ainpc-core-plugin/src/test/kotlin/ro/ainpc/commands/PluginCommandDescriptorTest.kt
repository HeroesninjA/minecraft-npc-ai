package ro.ainpc.commands

import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class PluginCommandDescriptorTest {
    @Test
    fun pluginYmlDeclaresProgressionAndContractCommandFacades() {
        val config = YamlConfiguration.loadConfiguration(File("src/main/resources/plugin.yml"))

        val commands = config.getConfigurationSection("commands")
        assertNotNull(commands, "commands section should exist")
        commands!!

        assertTrue(commands.contains("quest"), "quest command should stay registered")
        assertTrue(commands.contains("progression"), "progression command should be registered")
        assertTrue(commands.contains("contract"), "contract command should be registered")
        assertTrue(commands.contains("ritual"), "ritual command should be registered")

        assertEquals(listOf("progress"), commands.getStringList("progression.aliases"))
        assertEquals(listOf("contracts"), commands.getStringList("contract.aliases"))
        assertEquals(listOf("rituals", "ceremony", "ceremonies"), commands.getStringList("ritual.aliases"))
        assertTrue((commands.getString("progression.usage", "") ?: "").contains("status"))
        assertTrue((commands.getString("contract.usage", "") ?: "").contains("track"))
        assertTrue((commands.getString("ritual.usage", "") ?: "").contains("track"))
    }
}
