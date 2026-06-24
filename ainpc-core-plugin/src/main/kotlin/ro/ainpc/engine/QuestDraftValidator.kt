package ro.ainpc.engine

import java.util.Locale

class QuestDraftValidator(
    allowedObjectiveTypes: Set<String> = DEFAULT_OBJECTIVE_TYPES,
    allowedRewardTypes: Set<String> = DEFAULT_REWARD_TYPES,
    allowedStoryActionTypes: Set<String> = DEFAULT_STORY_ACTION_TYPES
) {
    private val objectiveTypes = normalizeSet(allowedObjectiveTypes)
    private val rewardTypes = normalizeSet(allowedRewardTypes)
    private val storyActionTypes = normalizeSet(allowedStoryActionTypes)

    fun validate(seed: QuestSeed?, draft: QuestDraft?): QuestDraftValidationReport {
        val errors = ArrayList<String>()
        val warnings = ArrayList<String>()
        val infos = ArrayList<String>()

        if (seed == null) {
            errors.add("QuestSeed lipseste.")
        }
        if (draft == null) {
            errors.add("QuestDraft lipseste.")
            return QuestDraftValidationReport(
                valid = false,
                executable = false,
                errors = errors,
                warnings = warnings,
                infos = infos
            )
        }

        validateSeed(seed, errors, warnings)
        validateDraftIdentity(draft, errors, warnings)
        validateObjectives(seed, draft, errors, warnings)
        validateRewards(seed, draft, errors, warnings)
        validateStory(seed, draft, errors, warnings, infos)
        validateAnchors(draft, errors, warnings)

        if (draft.exportEnabled()) {
            errors.add("QuestDraft trebuie sa ramana read-only: exportEnabled=false.")
        }
        infos.add("QuestDraft validat read-only; nu executa progres si nu scrie YAML live.")

        return QuestDraftValidationReport(errors.isEmpty(), false, errors, warnings, infos)
    }

    private fun validateSeed(seed: QuestSeed?, errors: MutableList<String>, warnings: MutableList<String>) {
        if (seed == null) {
            return
        }
        if (seed.regionId().isBlank()) {
            errors.add("QuestSeed trebuie sa aiba regionId stabil.")
        }
        if (seed.mechanicId().isBlank() && seed.kind().isBlank()) {
            errors.add("QuestSeed trebuie sa declare mechanicId sau kind.")
        }
        if (seed.allowedObjectiveTypes().isEmpty()) {
            warnings.add("QuestSeed nu limiteaza explicit tipurile de obiective.")
        }
    }

    private fun validateDraftIdentity(draft: QuestDraft, errors: MutableList<String>, warnings: MutableList<String>) {
        if (draft.draftId().isBlank()) {
            errors.add("QuestDraft trebuie sa aiba draftId.")
        }
        if (draft.title().isBlank()) {
            errors.add("QuestDraft trebuie sa aiba titlu.")
        }
        if (draft.mechanicId().isBlank() && draft.kind().isBlank()) {
            errors.add("QuestDraft trebuie sa declare mechanicId sau kind.")
        }
        if (draft.description().length > 800) {
            warnings.add("QuestDraft are descriere lunga; verifica manual textul AI.")
        }
    }

    private fun validateObjectives(
        seed: QuestSeed?,
        draft: QuestDraft,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        if (draft.objectives().isEmpty()) {
            errors.add("QuestDraft trebuie sa contina cel putin un obiectiv.")
            return
        }
        val seedObjectiveTypes = normalizeSet(seed?.allowedObjectiveTypes()?.toSet().orEmpty())
        val seenIds = LinkedHashSet<String>()
        for (objective in draft.objectives()) {
            val objectiveId = objective.id()
            val objectiveType = normalize(objective.type())
            if (objectiveId.isBlank()) {
                errors.add("Obiectiv fara id stabil.")
            } else if (!seenIds.add(objectiveId)) {
                errors.add("Obiectiv duplicat: $objectiveId.")
            }
            if (objectiveType.isBlank()) {
                errors.add("Obiectivul $objectiveId nu are type.")
            } else if (!objectiveTypes.contains(objectiveType)) {
                errors.add("Obiectivul $objectiveId are type necunoscut: ${objective.type()}.")
            } else if (seedObjectiveTypes.isNotEmpty() && !seedObjectiveTypes.contains(objectiveType)) {
                errors.add("Obiectivul $objectiveId nu este permis de QuestSeed: ${objective.type()}.")
            }
            validateNoRawCoordinates("obiectiv $objectiveId target", objective.target(), errors)
            validateNoRawCoordinates("obiectiv $objectiveId anchor", objective.anchorReference(), errors)
            if (objective.target().isBlank() && objective.anchorReference().isBlank()) {
                warnings.add("Obiectivul $objectiveId nu are target sau ancora semantica.")
            }
        }
    }

    private fun validateRewards(
        seed: QuestSeed?,
        draft: QuestDraft,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        val seedRewardTypes = normalizeSet(seed?.allowedRewardTypes()?.toSet().orEmpty())
        if (draft.rewards().isEmpty()) {
            warnings.add("QuestDraft nu declara reward-uri.")
            return
        }
        for (reward in draft.rewards()) {
            val rewardType = normalize(reward.type())
            if (rewardType.isBlank()) {
                errors.add("Reward fara type.")
            } else if (!rewardTypes.contains(rewardType)) {
                errors.add("Reward cu type necunoscut: ${reward.type()}.")
            } else if (seedRewardTypes.isNotEmpty() && !seedRewardTypes.contains(rewardType)) {
                errors.add("Reward-ul ${reward.type()} nu este permis de QuestSeed.")
            }
            if (reward.amount() < 0) {
                errors.add("Reward-ul ${reward.type()} are amount negativ.")
            }
        }
    }

    private fun validateStory(
        seed: QuestSeed?,
        draft: QuestDraft,
        errors: MutableList<String>,
        warnings: MutableList<String>,
        infos: MutableList<String>
    ) {
        val storyMode = normalize(seed?.storyMode())
        if (draft.storyActions().isEmpty()) {
            if (storyMode == "writes_story" || storyMode == "story_driven") {
                warnings.add("QuestSeed cere story, dar QuestDraft nu declara storyActions.")
            }
            return
        }
        if (storyMode == "no_story") {
            errors.add("QuestDraft declara storyActions desi QuestSeed este no_story.")
        }
        for (action in draft.storyActions()) {
            val type = normalize(action.type())
            val scope = normalize(action.scope())
            if (!storyActionTypes.contains(type)) {
                errors.add("Story action necunoscuta: ${action.type()}.")
            }
            if (scope != "region" && scope != "place") {
                errors.add("Story action ${action.type()} trebuie sa aiba scope region sau place.")
            }
            if (action.target().isBlank()) {
                errors.add("Story action ${action.type()} trebuie sa aiba target explicit.")
            }
            if (type == "set_story_state" && action.key().isBlank()) {
                errors.add("set_story_state trebuie sa aiba key.")
            }
            validateNoRawCoordinates("story action ${action.type()} target", action.target(), errors)
        }
        infos.add("Story actions sunt doar validate; aplicarea ramane in runtime-ul de progres.")
    }

    private fun validateAnchors(draft: QuestDraft, errors: MutableList<String>, warnings: MutableList<String>) {
        val references = LinkedHashSet<String>()
        references.addAll(draft.anchorReferences())
        for (objective in draft.objectives()) {
            if (objective.anchorReference().isNotBlank()) {
                references.add(objective.anchorReference())
            }
        }
        if (references.isEmpty()) {
            warnings.add("QuestDraft nu are ancore semantice explicite.")
            return
        }
        for (reference in references) {
            validateSemanticReference(reference, errors)
            validateNoRawCoordinates("anchor $reference", reference, errors)
        }
    }

    private fun validateSemanticReference(reference: String, errors: MutableList<String>) {
        val normalizedReference = normalize(reference)
        val valid = SEMANTIC_PREFIXES.any { prefix -> normalizedReference.startsWith(prefix) }
        if (!valid) {
            errors.add("Ancora trebuie sa fie semantica, nu coordonata sau text liber: $reference.")
        }
    }

    private fun validateNoRawCoordinates(label: String, value: String, errors: MutableList<String>) {
        if (value.isBlank()) {
            return
        }
        if (RAW_COORDINATE_REGEX.containsMatchIn(value)) {
            errors.add("$label foloseste coordonate brute; foloseste region/place/node/tag/role.")
        }
    }

    companion object {
        val DEFAULT_OBJECTIVE_TYPES: Set<String> = setOf(
            "visit_region",
            "visit_place",
            "inspect_node",
            "talk_to_npc",
            "deliver_to_npc",
            "collect_item",
            "kill_mob",
            "place_block",
            "break_block",
            "craft_item",
            "use_item",
            "equip_item"
        )
        val DEFAULT_REWARD_TYPES: Set<String> = setOf("item", "experience", "command", "economy:money", "story")
        val DEFAULT_STORY_ACTION_TYPES: Set<String> = setOf("record_story_event", "set_story_state")
        private val SEMANTIC_PREFIXES = setOf("region:", "place:", "node:", "tag:", "role:", "npc:", "objective:")
        private val RAW_COORDINATE_REGEX = Regex("""(^|\W)-?\d+(\.\d+)?\s*,\s*-?\d+(\.\d+)?\s*,\s*-?\d+(\.\d+)?($|\W)""")

        private fun normalizeSet(values: Set<String>): Set<String> =
            values.map { normalize(it) }.filter { it.isNotBlank() }.toSet()

        private fun normalize(value: String?): String = value?.trim()?.lowercase(Locale.ROOT).orEmpty()
    }
}
