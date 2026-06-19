package ro.ainpc.gui

import ro.ainpc.progression.ProgressionDefinition

object AuthoringSelectionSupport {
    @JvmStatic
    fun questSelectorOptions(definitions: List<ProgressionDefinition>): List<String> {
        return definitions
            .flatMap { definition ->
                listOf(
                    definition.progressionId(),
                    definition.templateId(),
                    definition.definitionId(),
                    definition.code()
                )
            }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
    }

    @JvmStatic
    fun mechanicOptions(definitions: List<ProgressionDefinition>): List<String> {
        return definitions
            .map { definition -> definition.mechanicId() }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
    }

    @JvmStatic
    fun cycle(values: List<String>, currentValue: String, step: Int): String {
        if (values.isEmpty()) {
            return currentValue
        }
        val currentIndex = values.indexOfFirst { value -> value.equals(currentValue, ignoreCase = true) }
        if (currentIndex < 0) {
            return values.first()
        }
        val nextIndex = ((currentIndex + step) % values.size + values.size) % values.size
        return values[nextIndex]
    }
}
