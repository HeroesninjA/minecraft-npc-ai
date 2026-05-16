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

    @Test
    void prefixesShortKindAlias() {
        ProgressionSelector selector = ProgressionSelector.forKindAlias("D01", "duty");

        assertEquals("duty:D01", selector.commandSelector());
        assertEquals("duty", selector.mechanicOrKind());
        assertEquals("D01", selector.definitionId());
    }

    @Test
    void prefixesShortEventAlias() {
        ProgressionSelector selector = ProgressionSelector.forKindAlias("E01", "event");

        assertEquals("event:E01", selector.commandSelector());
        assertEquals("event", selector.mechanicOrKind());
        assertEquals("E01", selector.definitionId());
    }

    @Test
    void prefixesShortTutorialAlias() {
        ProgressionSelector selector = ProgressionSelector.forKindAlias("T01", "tutorial");

        assertEquals("tutorial:T01", selector.commandSelector());
        assertEquals("tutorial", selector.mechanicOrKind());
        assertEquals("T01", selector.definitionId());
    }

    @Test
    void prefixesShortRitualAlias() {
        ProgressionSelector selector = ProgressionSelector.forKindAlias("R01", "ritual");

        assertEquals("ritual:R01", selector.commandSelector());
        assertEquals("ritual", selector.mechanicOrKind());
        assertEquals("R01", selector.definitionId());
    }
}
