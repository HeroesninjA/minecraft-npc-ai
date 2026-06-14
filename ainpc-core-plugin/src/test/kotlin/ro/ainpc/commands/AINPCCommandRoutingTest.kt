package ro.ainpc.commands

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

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
}
