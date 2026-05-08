package ro.ainpc.progression;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressionSelectorTest {

    @Test
    void parsesSimpleDefinitionSelector() {
        ProgressionSelector selector = ProgressionSelector.parse("Q01");

        assertEquals("Q01", selector.commandSelector());
        assertEquals("Q01", selector.definitionId());
        assertFalse(selector.hasNamespace());
    }

    @Test
    void parsesMechanicQualifiedSelector() {
        ProgressionSelector selector = ProgressionSelector.parse("village_contracts:C01");

        assertEquals("village_contracts:C01", selector.commandSelector());
        assertEquals("village_contracts", selector.mechanicOrKind());
        assertEquals("C01", selector.definitionId());
        assertTrue(selector.hasNamespace());
    }

    @Test
    void parsesPackMechanicQualifiedSelector() {
        ProgressionSelector selector = ProgressionSelector.parse("medieval:village_contracts:C01");

        assertEquals("medieval:village_contracts:C01", selector.commandSelector());
        assertEquals("medieval", selector.packId());
        assertEquals("village_contracts", selector.mechanicOrKind());
        assertEquals("C01", selector.definitionId());
    }

    @Test
    void normalizesTrackedAliases() {
        ProgressionSelector selector = ProgressionSelector.parse("curent");

        assertEquals("tracked", selector.commandSelector());
        assertTrue(selector.isTrackedAlias());
    }

    @Test
    void prefixesShortContractAlias() {
        ProgressionSelector selector = ProgressionSelector.forContractAlias("C01");

        assertEquals("contract:C01", selector.commandSelector());
        assertEquals("contract", selector.mechanicOrKind());
        assertEquals("C01", selector.definitionId());
    }

    @Test
    void preservesQualifiedContractAlias() {
        ProgressionSelector selector = ProgressionSelector.forContractAlias("village_contracts:C01");

        assertEquals("village_contracts:C01", selector.commandSelector());
    }
}
