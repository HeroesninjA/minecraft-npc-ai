package ro.ainpc.commands

import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class AINPCCommandReachabilityTest {
    @Test
    fun routesEveryRegisteredCommandAndAlias() {
        val config = YamlConfiguration.loadConfiguration(File("src/main/resources/plugin.yml"))
        val commands = config.getConfigurationSection("commands")
        assertNotNull(commands)
        commands!!

        val primaryRoutes = commands.getKeys(false).associateWith { commandName ->
            AINPCCommandCatalog.resolveDirectCommand(commandName)
        }
        assertTrue(primaryRoutes.isNotEmpty())
        assertTrue(
            primaryRoutes.values.none { route -> route == null },
            "Comenzi fara ruta directa: ${primaryRoutes.filterValues { route -> route == null }.keys}",
        )
        assertEquals(AINPCDirectCommandRoute.entries.toSet(), primaryRoutes.values.filterNotNull().toSet())

        for ((commandName, route) in primaryRoutes) {
            for (alias in commands.getStringList("$commandName.aliases")) {
                assertEquals(route, AINPCCommandCatalog.resolveDirectCommand(alias), "Alias fara ruta: /$alias")
            }
        }
    }

    @Test
    fun routesEveryTopLevelCommandReferencedByDocumentation() {
        val docsRoot = File("../docs v2")
        assertTrue(docsRoot.isDirectory)
        val references = docsRoot.walkTopDown()
            .filter { file -> file.isFile && file.extension == "md" }
            .flatMap { file ->
                AINPC_COMMAND.findAll(file.readText()).map { match -> match.groupValues[1] }
            }
            .toSortedSet()
        assertTrue(references.size >= 10, "Inventarul documentatiei este neasteptat de mic: $references")

        val unreachable = references.filter { token -> AINPCCommandCatalog.resolveSubcommand(token) == null }

        assertTrue(unreachable.isEmpty(), "Comenzi /ainpc documentate fara ruta: $unreachable")
    }

    companion object {
        private val AINPC_COMMAND = Regex("/ainpc\\s+([a-z][a-z-]*)")
    }
}
