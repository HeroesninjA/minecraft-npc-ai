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
        assertTrue((commands.getString("ainpc.usage", "") ?: "").contains("demo"))
        assertTrue((commands.getString("ainpc.usage", "") ?: "").contains("world"))
    }

    @Test
    fun mainHelpMentionsAllDemoCommandFamilies() {
        val commandSource = File("src/main/java/ro/ainpc/commands/AINPCCommand.java")
        assertTrue(commandSource.isFile, "AINPCCommand.java should exist")

        val helpText = commandSource.readText()
        val demoHelpLine = helpText.lineSequence()
            .firstOrNull { it.contains("/ainpc demo <") }
            .orEmpty()

        assertTrue(demoHelpLine.isNotBlank(), "Main help should include a /ainpc demo usage line")
        EXPECTED_DEMO_HELP_TOKENS.forEach { token ->
            assertTrue(demoHelpLine.contains(token), "Main help should mention demo token '$token'")
        }
    }

    @Test
    fun npcInteractionGuiKeepsNearestRoutineCommandExplicit() {
        val guiSource = File("src/main/kotlin/ro/ainpc/gui/screens/NpcInteractionGui.kt")
        assertTrue(guiSource.isFile, "NpcInteractionGui.kt should exist")

        val source = guiSource.readText()

        assertTrue(source.contains("Rutina nearest"), "GUI should expose a nearest routine action")
        assertTrue(
            source.contains("\"ainpc routine status nearest\""),
            "Nearest routine GUI action should keep the nearest selector"
        )
    }

    @Test
    fun infoCommandSupportsExplicitNearestSelector() {
        val commandSource = File("src/main/java/ro/ainpc/commands/AINPCCommand.java")
        assertTrue(commandSource.isFile, "AINPCCommand.java should exist")

        val source = commandSource.readText()

        assertTrue(source.contains("/ainpc info [nume|nearest]"))
        assertTrue(source.contains("\"nearest\".equalsIgnoreCase(args[1])"))
    }

    @Test
    fun routineCommandsKeepNearestStatusExplicit() {
        val commandSource = File("src/main/java/ro/ainpc/commands/AINPCCommand.java")
        val routineGuiSource = File("src/main/kotlin/ro/ainpc/gui/screens/RoutineGui.kt")
        assertTrue(commandSource.isFile, "AINPCCommand.java should exist")
        assertTrue(routineGuiSource.isFile, "RoutineGui.kt should exist")

        val commandText = commandSource.readText()
        val guiText = routineGuiSource.readText()

        assertTrue(commandText.contains("/ainpc routine status [numeNpc|nearest]"))
        assertTrue(commandText.contains("\"nearest\".equalsIgnoreCase(args[2])"))
        assertTrue(guiText.contains("\"ainpc routine status nearest\""))
    }

    companion object {
        private val EXPECTED_DEMO_HELP_TOKENS = listOf(
            "definition",
            "status",
            "next",
            "script",
            "phases",
            "evidence",
            "runbook",
            "smoke",
            "summary",
            "commands",
            "restart",
            "experimental",
            "experimental5",
            "experimental25",
            "experimental25deep",
            "experimental25ops"
        )
    }
}
