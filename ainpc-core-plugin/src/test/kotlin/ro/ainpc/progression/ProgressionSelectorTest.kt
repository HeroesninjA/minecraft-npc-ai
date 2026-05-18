package ro.ainpc.progression

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProgressionSelectorTest {
    @Test
    fun parsesSimpleDefinitionSelector() {
        val selector = ProgressionSelector.parse("Q01")

        assertEquals("Q01", selector.commandSelector())
        assertEquals("Q01", selector.definitionId())
        assertFalse(selector.hasNamespace())
    }

    @Test
    fun parsesMechanicQualifiedSelector() {
        val selector = ProgressionSelector.parse("village_contracts:C01")

        assertEquals("village_contracts:C01", selector.commandSelector())
        assertEquals("village_contracts", selector.mechanicOrKind())
        assertEquals("C01", selector.definitionId())
        assertTrue(selector.hasNamespace())
    }

    @Test
    fun parsesPackMechanicQualifiedSelector() {
        val selector = ProgressionSelector.parse("medieval:village_contracts:C01")

        assertEquals("medieval:village_contracts:C01", selector.commandSelector())
        assertEquals("medieval", selector.packId())
        assertEquals("village_contracts", selector.mechanicOrKind())
        assertEquals("C01", selector.definitionId())
    }

    @Test
    fun normalizesTrackedAliases() {
        val selector = ProgressionSelector.parse("curent")

        assertEquals("tracked", selector.commandSelector())
        assertTrue(selector.isTrackedAlias())
    }

    @Test
    fun prefixesShortContractAlias() {
        val selector = ProgressionSelector.forContractAlias("C01")

        assertEquals("contract:C01", selector.commandSelector())
        assertEquals("contract", selector.mechanicOrKind())
        assertEquals("C01", selector.definitionId())
    }

    @Test
    fun preservesQualifiedContractAlias() {
        val selector = ProgressionSelector.forContractAlias("village_contracts:C01")

        assertEquals("village_contracts:C01", selector.commandSelector())
    }

    @Test
    fun prefixesShortKindAlias() {
        val selector = ProgressionSelector.forKindAlias("D01", "duty")

        assertEquals("duty:D01", selector.commandSelector())
        assertEquals("duty", selector.mechanicOrKind())
        assertEquals("D01", selector.definitionId())
    }

    @Test
    fun prefixesShortEventAlias() {
        val selector = ProgressionSelector.forKindAlias("E01", "event")

        assertEquals("event:E01", selector.commandSelector())
        assertEquals("event", selector.mechanicOrKind())
        assertEquals("E01", selector.definitionId())
    }

    @Test
    fun prefixesShortTutorialAlias() {
        val selector = ProgressionSelector.forKindAlias("T01", "tutorial")

        assertEquals("tutorial:T01", selector.commandSelector())
        assertEquals("tutorial", selector.mechanicOrKind())
        assertEquals("T01", selector.definitionId())
    }

    @Test
    fun prefixesShortRitualAlias() {
        val selector = ProgressionSelector.forKindAlias("R01", "ritual")

        assertEquals("ritual:R01", selector.commandSelector())
        assertEquals("ritual", selector.mechanicOrKind())
        assertEquals("R01", selector.definitionId())
    }
}
