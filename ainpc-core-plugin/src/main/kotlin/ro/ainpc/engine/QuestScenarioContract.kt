package ro.ainpc.engine

import java.util.Locale

/**
 * Contractul mecanicii comune de quest, independent de tema scenariului.
 */
data class QuestScenarioContract(
    val kind: Kind?,
    val category: Category?,
    val acceptanceMode: AcceptanceMode?,
    val completionMode: CompletionMode?,
    val trackingMode: TrackingMode?,
    val tags: List<String>?
) {
    private val resolvedKind = kind ?: Kind.CUSTOM
    private val resolvedCategory = category ?: Category.SIDE
    private val resolvedAcceptanceMode = acceptanceMode ?: AcceptanceMode.EXPLICIT
    private val resolvedCompletionMode = completionMode ?: CompletionMode.RETURN_TO_GIVER
    private val resolvedTrackingMode = trackingMode ?: TrackingMode.NEXT_OBJECTIVE
    private val resolvedTags = tags?.toList() ?: listOf()

    fun kind(): Kind = resolvedKind
    fun category(): Category = resolvedCategory
    fun acceptanceMode(): AcceptanceMode = resolvedAcceptanceMode
    fun completionMode(): CompletionMode = resolvedCompletionMode
    fun trackingMode(): TrackingMode = resolvedTrackingMode
    fun tags(): List<String> = resolvedTags.toList()

    fun displayName(): String {
        return when (resolvedKind) {
            Kind.FETCH -> "adunare"
            Kind.HUNT -> "vanatoare"
            Kind.DELIVERY -> "livrare"
            Kind.EXPLORATION -> "explorare"
            Kind.INVESTIGATION -> "investigatie"
            Kind.DUTY -> "sarcina"
            Kind.EVENT -> "eveniment"
            Kind.TUTORIAL -> "tutorial"
            Kind.RITUAL -> "ritual"
            Kind.SOCIAL -> "social"
            Kind.CUSTOM -> "personalizat"
        }
    }

    fun categoryDisplayName(): String = resolvedCategory.displayName()

    fun autoAcceptOnOffer(): Boolean = resolvedAcceptanceMode == AcceptanceMode.AUTO_ACCEPT

    companion object {
        @JvmStatic
        fun defaultContract(): QuestScenarioContract {
            return QuestScenarioContract(
                Kind.CUSTOM,
                Category.SIDE,
                AcceptanceMode.EXPLICIT,
                CompletionMode.RETURN_TO_GIVER,
                TrackingMode.NEXT_OBJECTIVE,
                listOf()
            )
        }

        @JvmStatic
        fun fromScenarioDefinition(definition: FeaturePackLoader.ScenarioDefinition?): QuestScenarioContract {
            if (definition == null) {
                return defaultContract()
            }

            return fromQuestEntries(
                definition.questCategory,
                definition.questScenarioKind,
                definition.questAcceptanceMode,
                definition.questCompletionMode,
                definition.questTrackingMode,
                definition.questTags,
                definition.objectives,
                definition.isQuestRepeatable
            )
        }

        @JvmStatic
        fun fromQuestEntries(
            kind: String,
            acceptanceMode: String,
            completionMode: String,
            trackingMode: String,
            tags: List<String>?,
            objectives: List<FeaturePackLoader.QuestEntryDefinition>?
        ): QuestScenarioContract {
            return fromQuestEntries("", kind, acceptanceMode, completionMode, trackingMode, tags, objectives, false)
        }

        @JvmStatic
        fun fromQuestEntries(
            category: String,
            kind: String,
            acceptanceMode: String,
            completionMode: String,
            trackingMode: String,
            tags: List<String>?,
            objectives: List<FeaturePackLoader.QuestEntryDefinition>?,
            repeatable: Boolean
        ): QuestScenarioContract {
            var resolvedKind = Kind.fromId(kind)
            if (resolvedKind == Kind.CUSTOM) {
                resolvedKind = inferKind(objectives)
            }
            val normalizedTags = normalizeTags(tags)

            return QuestScenarioContract(
                resolvedKind,
                Category.fromId(category, normalizedTags, repeatable),
                AcceptanceMode.fromId(acceptanceMode),
                CompletionMode.fromId(completionMode),
                TrackingMode.fromId(trackingMode),
                normalizedTags
            )
        }

        private fun inferKind(objectives: List<FeaturePackLoader.QuestEntryDefinition>?): Kind {
            if (objectives.isNullOrEmpty()) {
                return Kind.CUSTOM
            }

            var hasHunt = false
            var hasDelivery = false
            var hasInvestigation = false
            var hasExploration = false
            var hasSocial = false
            var hasFetch = false

            for (objective in objectives) {
                val type = normalize(objective.type)
                when (type) {
                    "killmob", "kill_mob", "hunt" -> hasHunt = true
                    "deliveritem", "deliver_item", "delivery" -> hasDelivery = true
                    "inspectnode", "inspect_node", "investigate", "investigation" -> hasInvestigation = true
                    "visitregion", "visit_region", "visitplace", "visit_place", "explore" -> hasExploration = true
                    "talktonpc", "talk_to_npc", "dialogue", "social" -> hasSocial = true
                    "item", "collect", "collectitem", "collect_item", "fetch", "gather" -> hasFetch = true
                    else -> {
                        // Tipurile necunoscute raman custom daca nu exista semnale mai precise.
                    }
                }
            }

            if (hasHunt) return Kind.HUNT
            if (hasDelivery) return Kind.DELIVERY
            if (hasInvestigation) return Kind.INVESTIGATION
            if (hasExploration) return Kind.EXPLORATION
            if (hasSocial) return Kind.SOCIAL
            return if (hasFetch) Kind.FETCH else Kind.CUSTOM
        }

        private fun normalizeTags(tags: List<String>?): List<String> {
            if (tags.isNullOrEmpty()) {
                return listOf()
            }

            val normalizedTags = mutableListOf<String>()
            for (tag in tags) {
                val normalized = normalize(tag)
                if (normalized.isNotBlank()) {
                    normalizedTags.add(normalized)
                }
            }
            return normalizedTags
        }

        private fun normalize(value: String?): String {
            return value?.trim()?.lowercase(Locale.ROOT)?.replace('-', '_')?.replace(" ", "_") ?: ""
        }
    }

    enum class Kind {
        FETCH,
        HUNT,
        DELIVERY,
        EXPLORATION,
        INVESTIGATION,
        DUTY,
        EVENT,
        TUTORIAL,
        RITUAL,
        SOCIAL,
        CUSTOM;

        companion object {
            @JvmStatic
            fun fromId(value: String?): Kind {
                return when (normalize(value)) {
                    "fetch", "collect", "collect_item", "gather" -> FETCH
                    "hunt", "kill", "kill_mob", "combat" -> HUNT
                    "delivery", "deliver", "deliver_item" -> DELIVERY
                    "exploration", "explore", "visit", "inspect" -> EXPLORATION
                    "investigation", "investigate", "investigatie", "investigare" -> INVESTIGATION
                    "duty", "duties", "sarcina", "sarcini", "datorie", "npc_duty" -> DUTY
                    "event", "events", "eveniment", "evenimente", "local_event", "world_event", "village_event" -> EVENT
                    "tutorial", "tutorials", "onboarding", "indrumare" -> TUTORIAL
                    "ritual", "rituals", "ceremony", "ceremonies", "ceremonie", "ceremonii", "village_ritual" -> RITUAL
                    "social", "dialogue", "talk" -> SOCIAL
                    else -> CUSTOM
                }
            }
        }
    }

    enum class Category(private val displayName: String) {
        MAIN("principal"),
        SIDE("secundar"),
        REPEATABLE("repetabil");

        fun displayName(): String = displayName

        companion object {
            @JvmStatic
            fun fromId(value: String?, tags: List<String>?, repeatable: Boolean): Category {
                return when (normalize(value)) {
                    "main", "primary", "main_quest", "principal" -> MAIN
                    "repeatable", "daily", "repetabil" -> REPEATABLE
                    "side", "secondary", "side_quest", "secundar" -> SIDE
                    else -> infer(tags, repeatable)
                }
            }

            private fun infer(tags: List<String>?, repeatable: Boolean): Category {
                if (repeatable || (tags != null && tags.contains("repeatable"))) {
                    return REPEATABLE
                }
                if (tags != null && (tags.contains("main") || tags.contains("primary") || tags.contains("main_quest"))) {
                    return MAIN
                }
                return SIDE
            }
        }
    }

    enum class AcceptanceMode {
        EXPLICIT,
        AUTO_ACCEPT;

        companion object {
            @JvmStatic
            fun fromId(value: String?): AcceptanceMode {
                return when (normalize(value)) {
                    "auto", "auto_accept", "direct", "direct_accept" -> AUTO_ACCEPT
                    else -> EXPLICIT
                }
            }
        }
    }

    enum class CompletionMode {
        RETURN_TO_GIVER,
        AUTO,
        MANUAL;

        companion object {
            @JvmStatic
            fun fromId(value: String?): CompletionMode {
                return when (normalize(value)) {
                    "auto", "automatic" -> AUTO
                    "manual", "admin" -> MANUAL
                    else -> RETURN_TO_GIVER
                }
            }
        }
    }

    enum class TrackingMode {
        NEXT_OBJECTIVE,
        QUEST_GIVER,
        DISABLED;

        companion object {
            @JvmStatic
            fun fromId(value: String?): TrackingMode {
                return when (normalize(value)) {
                    "giver", "quest_giver", "return_to_giver" -> QUEST_GIVER
                    "none", "disabled", "off" -> DISABLED
                    else -> NEXT_OBJECTIVE
                }
            }
        }
    }
}
