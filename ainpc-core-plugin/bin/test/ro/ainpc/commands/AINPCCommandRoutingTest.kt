package ro.ainpc.commands

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AINPCCommandRoutingTest {
    @Test
    fun routesProgressionAliasGuiToKindFilter() {
        val command = AINPCCommand(null)
        val route = AINPCCommand::class.java.getDeclaredMethod("routeProgressionAlias", Array<String>::class.java, String::class.java)
        route.isAccessible = true

        assertArrayEquals(
            arrayOf("quest", "gui", "contract"),
            route.invoke(command, arrayOf("contract", "gui"), "contract") as Array<String>
        )
        assertArrayEquals(
            arrayOf("quest", "gui", "contract_active"),
            route.invoke(command, arrayOf("contract", "gui", "active"), "contract") as Array<String>
        )
        assertArrayEquals(
            arrayOf("quest", "gui", "ritual_tracked"),
            route.invoke(command, arrayOf("ritual", "gui", "tracked"), "ritual") as Array<String>
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
