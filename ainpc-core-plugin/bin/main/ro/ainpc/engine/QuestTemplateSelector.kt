package ro.ainpc.engine

import java.util.Locale
import java.util.function.Predicate

class QuestTemplateSelector private constructor() {
    companion object {
        @JvmStatic
        fun selectConfiguredTemplate(
            templates: List<ScenarioEngine.ScenarioTemplate?>?,
            available: Predicate<ScenarioEngine.ScenarioTemplate>?,
            completed: Predicate<ScenarioEngine.ScenarioTemplate>?
        ): ScenarioEngine.ScenarioTemplate? {
            if (templates.isNullOrEmpty()) {
                return null
            }

            val safeAvailable = available ?: Predicate { true }
            val safeCompleted = completed ?: Predicate { false }
            var firstAvailableCompletedTemplate: ScenarioEngine.ScenarioTemplate? = null
            var firstUnavailableTemplate: ScenarioEngine.ScenarioTemplate? = null

            for (template in templates) {
                if (template == null) {
                    continue
                }

                if (!safeAvailable.test(template)) {
                    if (firstUnavailableTemplate == null) {
                        firstUnavailableTemplate = template
                    }
                    continue
                }

                if (!safeCompleted.test(template)) {
                    return template
                }

                if (firstAvailableCompletedTemplate == null) {
                    firstAvailableCompletedTemplate = template
                }
            }

            return firstAvailableCompletedTemplate ?: firstUnavailableTemplate
        }

        @JvmStatic
        fun matchesProgressionKind(
            template: ScenarioEngine.ScenarioTemplate?,
            expectedKind: String?,
            mechanicDisplay: String?
        ): Boolean {
            if (template == null || expectedKind.isNullOrBlank()) {
                return false
            }

            val expected = normalize(expectedKind)
            val candidates = listOf(
                template.getProgressionKind(),
                template.getProgressionMechanicId(),
                template.getProgressionSingularLabel(),
                template.getProgressionPluralLabel(),
                mechanicDisplay
            )
            for (candidate in candidates) {
                if (expected == normalize(candidate)) {
                    return true
                }
            }
            return false
        }

        private fun normalize(value: String?): String {
            if (value.isNullOrBlank()) {
                return ""
            }

            return value.lowercase(Locale.ROOT)
                .replace("minecraft:", "")
                .replace(Regex("[^\\p{L}\\p{Nd}]+"), "_")
                .replace(Regex("^_+|_+$"), "")
                .replace(Regex("_+"), "_")
        }
    }
}
