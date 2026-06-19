package ro.ainpc.engine

import ro.ainpc.progression.ProgressionDefinition
import ro.ainpc.story.StoryContextSnapshot

class QuestAuthoringService(
    private val questDirector: QuestDirector = QuestDirector(),
    private val seedFactory: QuestSeedFactory = QuestSeedFactory()
) {
    fun analyze(
        storyContext: StoryContextSnapshot?,
        definitions: List<ProgressionDefinition>?,
        preferredQuestSelector: String?,
        preferredMechanicId: String?,
        regionId: String?,
        placeId: String?,
        questSeedAllowed: Boolean,
        blockingReasons: List<String>?
    ): QuestAuthoringSnapshot {
        val request = QuestDirectorRequest(
            storyContext,
            definitions,
            preferredMechanicId,
            questSeedAllowed,
            blockingReasons
        )
        val decision = questDirector.decide(request)
        val selectedDefinition = resolveDefinition(
            decision,
            request.definitions(),
            preferredQuestSelector,
            preferredMechanicId
        )
        val seed = seedFactory.create(decision, request.storyContext(), selectedDefinition, regionId, placeId)

        return QuestAuthoringSnapshot(
            handled = true,
            playerName = request.storyContext().playerName(),
            requestedQuestSelector = preferredQuestSelector.orEmpty(),
            requestedMechanicId = preferredMechanicId.orEmpty(),
            decision = decision,
            seed = seed,
            progressionDefinition = selectedDefinition,
            summaryLines = buildSummaryLines(decision, seed, selectedDefinition, preferredQuestSelector, preferredMechanicId),
            warnings = mergeWarnings(request.storyContext(), decision, seed)
        )
    }

    private fun resolveDefinition(
        decision: QuestDirectorDecision,
        definitions: List<ProgressionDefinition>,
        preferredQuestSelector: String?,
        preferredMechanicId: String?
    ): ProgressionDefinition? {
        val questSelector = preferredQuestSelector.orEmpty().trim()
        if (questSelector.isNotBlank()) {
            val explicitDefinition = definitions.firstOrNull { definition ->
                matches(definition.progressionId(), questSelector) ||
                    matches(definition.templateId(), questSelector) ||
                    matches(definition.definitionId(), questSelector) ||
                    matches(definition.code(), questSelector)
            }
            if (explicitDefinition != null) {
                if (preferredMechanicId.isNullOrBlank() || matches(explicitDefinition.mechanicId(), preferredMechanicId)) {
                    return explicitDefinition
                }
                val mechanicMatch = definitions.firstOrNull { definition ->
                    matches(definition.mechanicId(), preferredMechanicId) && (
                        matches(definition.progressionId(), explicitDefinition.progressionId()) ||
                            matches(definition.templateId(), explicitDefinition.templateId()) ||
                            matches(definition.definitionId(), explicitDefinition.definitionId()) ||
                            matches(definition.code(), explicitDefinition.code())
                        )
                }
                if (mechanicMatch != null) {
                    return mechanicMatch
                }
                return explicitDefinition
            }
        }

        val selectedProgressionId = decision.selectedProgressionId()
        val selectedTemplateId = decision.selectedTemplateId()
        val selectedDefinitionId = decision.selectedDefinitionId()
        return definitions.firstOrNull { definition ->
            matches(definition.progressionId(), selectedProgressionId) ||
                matches(definition.templateId(), selectedTemplateId) ||
                matches(definition.definitionId(), selectedDefinitionId) ||
                matches(definition.code(), selectedDefinitionId)
        }
    }

    private fun buildSummaryLines(
        decision: QuestDirectorDecision,
        seed: QuestSeed,
        definition: ProgressionDefinition?,
        requestedQuestSelector: String?,
        requestedMechanicId: String?
    ): List<String> {
        val lines = ArrayList<String>()
        lines.add("decision=${decision.status().id()}:${decision.reason()}")
        lines.add("decision_runtime=${decision.runtimeExecutable()}")
        if (!requestedQuestSelector.isNullOrBlank()) {
            lines.add("requested_selector=$requestedQuestSelector")
        }
        if (!requestedMechanicId.isNullOrBlank()) {
            lines.add("requested_mechanic=$requestedMechanicId")
        }
        if (definition != null) {
            lines.add("selected_selector=" + definition.progressionId())
        }
        if (decision.selectedTemplateId().isNotBlank()) {
            lines.add("selected_template=" + decision.selectedTemplateId())
        }
        if (definition != null) {
            lines.add("selected_mechanic=" + definition.mechanicId())
            lines.add("selected_kind=" + definition.kind())
        }
        if (decision.matchedSignals().isNotEmpty()) {
            lines.add("matched_signals=" + decision.matchedSignals().joinToString(","))
        }
        if (decision.candidateTemplateIds().isNotEmpty()) {
            lines.add("candidate_templates=" + decision.candidateTemplateIds().joinToString(","))
        }
        if (decision.blockedReasons().isNotEmpty()) {
            lines.add("blocked_reasons=" + decision.blockedReasons().joinToString(","))
        }
        lines.add("seed_region=" + seed.regionId())
        lines.add("seed_place=" + seed.placeId())
        lines.add("seed_mechanic=" + seed.mechanicId())
        lines.add("seed_kind=" + seed.kind())
        lines.add("seed_story_mode=" + seed.storyMode())
        lines.add("seed_theme=" + seed.theme())
        return lines
    }

    private fun mergeWarnings(
        storyContext: StoryContextSnapshot,
        decision: QuestDirectorDecision,
        seed: QuestSeed
    ): List<String> {
        val warnings = ArrayList<String>()
        warnings.addAll(storyContext.warnings())
        warnings.addAll(decision.warnings())
        warnings.addAll(decision.blockedReasons())
        warnings.addAll(seed.limits())
        return warnings.distinct()
    }

    private fun matches(left: String?, right: String?): Boolean =
        left != null && right != null && left.isNotBlank() && right.isNotBlank() && left.equals(right, ignoreCase = true)
}
