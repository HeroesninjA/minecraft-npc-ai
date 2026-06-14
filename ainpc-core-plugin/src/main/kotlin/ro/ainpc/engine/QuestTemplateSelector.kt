package ro.ainpc.engine

import java.util.Locale
import java.util.function.Predicate

class QuestTemplateSelector private constructor() {
    companion object {
        @JvmStatic
        fun selectConfiguredTemplate(
            templates: List<ScenarioTemplate?>?,
            available: Predicate<ScenarioTemplate>?,
            completed: Predicate<ScenarioTemplate>?
        ): ScenarioTemplate? {
            if (templates.isNullOrEmpty()) {
                return null
            }

            val safeAvailable = available ?: Predicate { true }
            val safeCompleted = completed ?: Predicate { false }
            var firstAvailableCompletedTemplate: ScenarioTemplate? = null
            var firstUnavailableTemplate: ScenarioTemplate? = null

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
            template: ScenarioTemplate?,
            expectedKind: String?,
            mechanicDisplay: String?
        ): Boolean {
            if (template == null || expectedKind.isNullOrBlank()) {
                return false
            }

            val expected = normalize(expectedKind)
            val candidates = listOf(
                template.progressionKind,
                template.progressionMechanicId,
                template.progressionSingularLabel,
                template.progressionPluralLabel,
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
