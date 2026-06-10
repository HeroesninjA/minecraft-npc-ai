package ro.ainpc.engine.runtime

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.LinkedHashMap

class ScenarioRuntimeRegistryTest {
    @Test
    fun actionRegistryNormalizesLookupAndRejectsDuplicateHandlers() {
        val registry = ScenarioActionRegistry()

        val first = registry.register(actionHandler("Give_Item"))
        val duplicate = registry.register(actionHandler(" give_item "))

        assertTrue(first.isValid())
        assertFalse(duplicate.isValid())
        assertTrue(registry.supports(" GIVE_ITEM "))
        assertEquals(1, registry.handlers().size)
    }

    @Test
    fun validateActionReportsMissingHandler() {
        val registry = ScenarioActionRegistry()
        val action = ScenarioRuntimeDefinition("reward-1", "give_item", mapOf("material" to "EMERALD"))

        val report = registry.validateAction(action)

        assertFalse(report.isValid())
        assertTrue(report.errors().first().contains("give_item"))
    }

    @Test
    fun validateActionAcceptsRegisteredHandlerAndWarnsForMissingStableId() {
        val registry = ScenarioActionRegistry()
        registry.register(actionHandler("give_item"))
        val action = ScenarioRuntimeDefinition("", "GIVE_ITEM", mapOf("material" to "EMERALD"))

        val report = registry.validateAction(action)

        assertTrue(report.isValid())
        assertTrue(report.hasWarnings())
    }

    @Test
    fun executionContextAndDefinitionSanitizeMaps() {
        val variables = LinkedHashMap<String, String>()
        variables[" region "] = " spawn "
        variables[""] = "ignored"
        variables["missing"] = ""

        val context = ScenarioExecutionContext(
            " player-uuid ", " Hero ", " npc-1 ", " Guard ", " region-1 ",
            " place-1 ", " node-1 ", " Q01 ", " progress-1 ", " standalone ", variables
        )

        val definition = ScenarioRuntimeDefinition(" action-1 ", " give_item ", variables)

        assertEquals("player-uuid", context.playerUuid())
        assertEquals("spawn", context.variable("region"))
        assertEquals("", context.variable("missing"))
        assertEquals("action-1", definition.id())
        assertEquals("give_item", definition.type())
        assertEquals("spawn", definition.parameter("region"))
        assertThrows(UnsupportedOperationException::class.java) { (context.variables() as MutableMap<String, String>)["new"] = "value" }
        assertThrows(UnsupportedOperationException::class.java) { (definition.parameters() as MutableMap<String, String>)["new"] = "value" }
    }

    private fun actionHandler(type: String): ScenarioActionHandler {
        return object : ScenarioActionHandler {
            override fun type(): String = type
            override fun execute(context: ScenarioExecutionContext, action: ScenarioRuntimeDefinition) {}
        }
    }
}
