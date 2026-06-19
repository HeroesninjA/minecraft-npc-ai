package ro.ainpc.gui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ro.ainpc.progression.ProgressionDefinition

class AuthoringSelectionSupportTest {
    @Test
    fun buildsSortedUniqueAuthoringOptions() {
        val definitions = listOf(
            definition("pack", "mechanic_b", "quest", "def_b", "pack:def_b", "code_b"),
            definition("pack", "mechanic_a", "quest", "def_a", "pack:def_a", "code_a"),
            definition("pack", "mechanic_a", "quest", "def_a", "pack:def_a", "code_a")
        )

        assertEquals(
            listOf("code_a", "code_b", "def_a", "def_b", "pack", "pack:def_a", "pack:def_b"),
            AuthoringSelectionSupport.questSelectorOptions(definitions)
        )
        assertEquals(
            listOf("mechanic_a", "mechanic_b"),
            AuthoringSelectionSupport.mechanicOptions(definitions)
        )
    }

    @Test
    fun cyclesAuthoringValuesWithWrapAndFallback() {
        val values = listOf("a", "b", "c")

        assertEquals("b", AuthoringSelectionSupport.cycle(values, "a", 1))
        assertEquals("a", AuthoringSelectionSupport.cycle(values, "c", 1))
        assertEquals("c", AuthoringSelectionSupport.cycle(values, "a", -1))
        assertEquals("a", AuthoringSelectionSupport.cycle(values, "missing", 1))
        assertEquals("current", AuthoringSelectionSupport.cycle(emptyList(), "current", 1))
    }

    private fun definition(
        progressionId: String,
        mechanicId: String,
        kind: String,
        definitionId: String,
        templateId: String,
        code: String
    ): ProgressionDefinition {
        return ProgressionDefinition(
            progressionId,
            "pack",
            mechanicId,
            kind,
            definitionId,
            templateId,
            code,
            "title",
            "description",
            "category",
            "scenarioKind",
            "baseType",
            "label",
            "singular",
            "plural",
            1,
            1,
            1,
            1,
            repeatableValue = false,
            enabledValue = true
        )
    }
}
