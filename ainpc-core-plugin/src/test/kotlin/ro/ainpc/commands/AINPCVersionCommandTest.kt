package ro.ainpc.commands

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class AINPCVersionCommandTest {
    @Test
    fun versionCommandIsRoutedThroughNpcAliasOnly() {
        val pluginSource = File("src/main/kotlin/ro/ainpc/AINPCPlugin.kt").readText()
        val commandSource = File("src/main/kotlin/ro/ainpc/commands/AINPCCommand.kt").readText()
        val helpSource = File("src/main/kotlin/ro/ainpc/commands/AINPCCommandDisplay.kt").readText()
        val tabSource = File("src/main/kotlin/ro/ainpc/commands/AINPCTabCompleter.kt").readText()
        val pluginYaml = File("src/main/resources/plugin.yml").readText()

        assertTrue(!pluginSource.contains("""registerAliasCommand("version", command)"""))
        assertTrue(commandSource.contains(""""version" -> handleVersion(sender)"""))
        assertTrue(helpSource.contains("""msg.send(sender, "&e/npc version")"""))
        assertTrue(!tabSource.contains(""""version","""))
        assertTrue(pluginYaml.contains("npc:"))
        assertTrue(pluginYaml.contains("aliases: [ai]"))
        assertTrue(!pluginYaml.contains("""
  version:
"""))
        assertTrue(!pluginYaml.contains("""usage: /version"""))
    }
}
