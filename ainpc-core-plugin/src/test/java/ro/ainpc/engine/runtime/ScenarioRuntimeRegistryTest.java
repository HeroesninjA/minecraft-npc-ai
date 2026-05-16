package ro.ainpc.engine.runtime;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenarioRuntimeRegistryTest {

    @Test
    void actionRegistryNormalizesLookupAndRejectsDuplicateHandlers() {
        ScenarioActionRegistry registry = new ScenarioActionRegistry();

        ScenarioValidationReport first = registry.register(actionHandler("Give_Item"));
        ScenarioValidationReport duplicate = registry.register(actionHandler(" give_item "));

        assertTrue(first.isValid());
        assertFalse(duplicate.isValid());
        assertTrue(registry.supports(" GIVE_ITEM "));
        assertEquals(1, registry.handlers().size());
    }

    @Test
    void validateActionReportsMissingHandler() {
        ScenarioActionRegistry registry = new ScenarioActionRegistry();
        ScenarioRuntimeDefinition action = new ScenarioRuntimeDefinition(
            "reward-1",
            "give_item",
            Map.of("material", "EMERALD")
        );

        ScenarioValidationReport report = registry.validateAction(action);

        assertFalse(report.isValid());
        assertTrue(report.errors().getFirst().contains("give_item"));
    }

    @Test
    void validateActionAcceptsRegisteredHandlerAndWarnsForMissingStableId() {
        ScenarioActionRegistry registry = new ScenarioActionRegistry();
        registry.register(actionHandler("give_item"));
        ScenarioRuntimeDefinition action = new ScenarioRuntimeDefinition(
            "",
            "GIVE_ITEM",
            Map.of("material", "EMERALD")
        );

        ScenarioValidationReport report = registry.validateAction(action);

        assertTrue(report.isValid());
        assertTrue(report.hasWarnings());
    }

    @Test
    void executionContextAndDefinitionSanitizeMaps() {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put(" region ", " spawn ");
        variables.put("", "ignored");
        variables.put(null, "ignored");
        variables.put("missing", null);

        ScenarioExecutionContext context = new ScenarioExecutionContext(
            " player-uuid ",
            " Hero ",
            " npc-1 ",
            " Guard ",
            " region-1 ",
            " place-1 ",
            " node-1 ",
            " Q01 ",
            " progress-1 ",
            " standalone ",
            variables
        );

        ScenarioRuntimeDefinition definition = new ScenarioRuntimeDefinition(
            " action-1 ",
            " give_item ",
            variables
        );

        assertEquals("player-uuid", context.playerUuid());
        assertEquals("spawn", context.variable("region"));
        assertEquals("", context.variable("missing"));
        assertEquals("action-1", definition.id());
        assertEquals("give_item", definition.type());
        assertEquals("spawn", definition.parameter("region"));
        assertThrows(UnsupportedOperationException.class, () -> context.variables().put("new", "value"));
        assertThrows(UnsupportedOperationException.class, () -> definition.parameters().put("new", "value"));
    }

    private ScenarioActionHandler actionHandler(String type) {
        return new ScenarioActionHandler() {
            @Override
            public String type() {
                return type;
            }

            @Override
            public void execute(ScenarioExecutionContext context, ScenarioRuntimeDefinition action) {
            }
        };
    }
}
