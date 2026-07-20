package ro.ainpc.commands

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class AINPCVersionCommandTest {
    @Test
    fun versionCommandIsRoutedThroughNpcAliasOnly() {
        val pluginSource = File("src/main/kotlin/ro/ainpc/AINPCPlugin.kt").readText()
        val helpSource = File("src/main/kotlin/ro/ainpc/commands/AINPCCommandDisplay.kt").readText()
        val pluginYaml = File("src/main/resources/plugin.yml").readText()

        assertTrue(!pluginSource.contains("""registerAliasCommand("version", command)"""))
        assertEquals(AINPCSubcommandRoute.VERSION, AINPCCommandCatalog.resolveSubcommand("version"))
        assertTrue(helpSource.contains("""msg.send(sender, "&e/npc version")"""))
        assertTrue(AINPCCommandCatalog.suggestedSubcommands.contains("version"))
        assertTrue(pluginYaml.contains("npc:"))
        assertTrue(pluginYaml.contains("aliases: [ai]"))
        assertTrue(!pluginYaml.contains("""
  version:
"""))
        assertTrue(!pluginYaml.contains("""usage: /version"""))
    }
}
