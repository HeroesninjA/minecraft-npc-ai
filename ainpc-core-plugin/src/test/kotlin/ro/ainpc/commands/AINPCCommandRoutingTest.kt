package ro.ainpc.commands

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class AINPCCommandRoutingTest {
    @Test
    fun routesProgressionAliasGuiToKindFilter() {
        assertArrayEquals(
            arrayOf("quest", "gui", "contract"),
            routeProgressionAlias(arrayOf("contract", "gui"), "contract") { s, s2 -> s }
        )
        assertArrayEquals(
            arrayOf("quest", "gui", "contract_active"),
            routeProgressionAlias(arrayOf("contract", "gui", "active"), "contract") { s, s2 -> s }
        )
        assertArrayEquals(
            arrayOf("quest", "gui", "ritual_tracked"),
            routeProgressionAlias(arrayOf("ritual", "gui", "tracked"), "ritual") { s, s2 -> s }
        )
    }

    @Test
    fun formatsFeatureDisabledMessageWithConfigPath() {
        assertEquals(
            listOf(
                "&cFunctia Questurile este dezactivata in configuratie.",
                "&7Activeaza &ffeatures.quest=true &7in config.yml si ruleaza /ainpc reload."
            ),
            featureDisabledMessages("features.quest", "Questurile")
        )
    }

    @Test
    fun scenarioListShowsValidationWarnings() {
        val source = File("src/main/kotlin/ro/ainpc/commands/AINPCCommand.kt").readText()

        assertTrue(source.contains("warnings=&f\${scenario.validationWarnings.size}"))
    }

    @Test
    fun scenarioListSupportsWarningsOnlyFilter() {
        val source = File("src/main/kotlin/ro/ainpc/commands/AINPCCommand.kt").readText()

        assertTrue(source.contains("warnings-only"))
        assertTrue(source.contains("warnings-first"))
        assertTrue(source.contains("\"warnings\" -> {"))
        assertTrue(source.contains("Mod list necunoscut pentru scenariu"))
        assertTrue(source.contains("sunt incompatibile"))
        assertTrue(source.contains("Filtru: doar scenarii cu warning-uri."))
        assertTrue(source.contains("Scenarii cu warning-uri"))
        assertTrue(source.contains("Sortare: warning-uri mai intai."))
        assertTrue(source.contains("scenario.validationWarnings.isEmpty()"))
    }
}
