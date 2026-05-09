package ro.ainpc.commands;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginCommandDescriptorTest {

    @Test
    void pluginYmlDeclaresProgressionAndContractCommandFacades() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(
            new File("src/main/resources/plugin.yml")
        );

        ConfigurationSection commands = config.getConfigurationSection("commands");
        assertNotNull(commands, "commands section should exist");

        assertTrue(commands.contains("quest"), "quest command should stay registered");
        assertTrue(commands.contains("progression"), "progression command should be registered");
        assertTrue(commands.contains("contract"), "contract command should be registered");
        assertTrue(commands.contains("ritual"), "ritual command should be registered");

        assertEquals(List.of("progress"), commands.getStringList("progression.aliases"));
        assertEquals(List.of("contracts"), commands.getStringList("contract.aliases"));
        assertEquals(List.of("rituals", "ceremony", "ceremonies"), commands.getStringList("ritual.aliases"));
        assertTrue(commands.getString("progression.usage", "").contains("status"));
        assertTrue(commands.getString("contract.usage", "").contains("track"));
        assertTrue(commands.getString("ritual.usage", "").contains("track"));
    }
}
