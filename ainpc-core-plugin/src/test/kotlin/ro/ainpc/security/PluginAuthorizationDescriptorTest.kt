package ro.ainpc.security

import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class PluginAuthorizationDescriptorTest {
    @Test
    fun optionalVaultBridgeHasDeterministicLoadOrder() {
        val config = loadDescriptor()

        assertTrue("Vault" in config.getStringList("softdepend"))
    }

    @Test
    fun everyGuiPermissionUsedAtRuntimeIsDeclared() {
        val config = loadDescriptor()
        val permissions = config.getConfigurationSection("permissions")
        assertNotNull(permissions)

        val source = File("src/main/kotlin/ro/ainpc/gui/GuiService.kt").readText()
        val accessPolicy = source.substringAfter("fun canOpen(").substringBefore("private fun hasAny")
        val runtimeNodes = Regex("\"(ainpc\\.(?:admin|creator|info|quest|talk|gui(?:\\.[a-z_]+)?))\"")
            .findAll(accessPolicy)
            .map { it.groupValues[1] }
            .toSet()

        runtimeNodes.forEach { node ->
            assertTrue(permissions!!.contains(node), "GUI permission '$node' must be declared in plugin.yml")
        }
    }

    @Test
    fun specializedGuiNodesDefaultToOperators() {
        val config = loadDescriptor()

        assertEquals("op", config.getString("permissions.ainpc.gui.relationship.default"))
        assertEquals("op", config.getString("permissions.ainpc.gui.npc.default"))
        assertEquals("op", config.getString("permissions.ainpc.gui.mcp.default"))
    }

    private fun loadDescriptor(): YamlConfiguration =
        YamlConfiguration.loadConfiguration(File("src/main/resources/plugin.yml"))
}
