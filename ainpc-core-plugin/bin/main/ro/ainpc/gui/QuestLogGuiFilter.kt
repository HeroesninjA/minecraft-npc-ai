package ro.ainpc.gui

import java.util.Locale
import java.util.Optional

enum class QuestLogGuiFilter(
    private val filterValue: String,
    private val buttonLabelValue: String,
    private val displayLabelValue: String
) {
    ALL("all", "Toate", "toate"),
    ACTIVE("active", "Active", "active"),
    QUEST("quest", "Questuri", "questuri"),
    CONTRACT("contract", "Contracte", "contracte"),
    DUTY("duty", "Sarcini", "sarcini"),
    BOUNTY("bounty", "Bounty", "bounty-uri"),
    EVENT("event", "Events", "evenimente"),
    TUTORIAL("tutorial", "Tutoriale", "tutoriale"),
    RITUAL("ritual", "Ritualuri", "ritualuri");

    fun filter(): String = filterValue

    fun buttonLabel(): String = buttonLabelValue

    fun displayLabel(): String = displayLabelValue

    fun matches(rawFilter: String?): Boolean = normalize(filterValue) == normalize(rawFilter)

    companion object {
        @JvmStatic
        fun primaryFilters(): List<QuestLogGuiFilter> = values().asList()

        @JvmStatic
        fun normalizeFilter(rawFilter: String?): String {
            val resolved = fromId(rawFilter)
            if (resolved.isPresent) {
                return resolved.get().filter()
            }
            return if (rawFilter.isNullOrBlank()) ALL.filterValue else rawFilter.trim()
        }

        @JvmStatic
        fun fromId(rawValue: String?): Optional<QuestLogGuiFilter> {
            val normalized = normalize(rawValue)
            if (normalized.isBlank()) {
                return Optional.of(ALL)
            }

            val canonical = when (normalized) {
                "toate", "tot" -> "all"
                "activ", "curente", "curent" -> "active"
                "quests", "questuri" -> "quest"
                "contracts", "contracte" -> "contract"
                "duties", "sarcina", "sarcini" -> "duty"
                "bounties", "recompensa", "recompense" -> "bounty"
                "events", "eveniment", "evenimente" -> "event"
                "tutorials", "onboarding", "indrumare" -> "tutorial"
                "rituals", "ceremony", "ceremonies", "ceremonie", "ceremonii" -> "ritual"
                else -> normalized
            }

            for (filter in values()) {
                if (filter.filterValue == canonical || normalize(filter.buttonLabelValue) == canonical) {
                    return Optional.of(filter)
                }
            }
            return Optional.empty()
        }

        private fun normalize(value: String?): String {
            if (value == null) {
                return ""
            }
            return value.trim()
                .lowercase(Locale.ROOT)
                .replace('-', '_')
                .replace(Regex("[^\\p{L}\\p{Nd}]+"), "_")
                .replace(Regex("^_+|_+$"), "")
        }
    }
}
