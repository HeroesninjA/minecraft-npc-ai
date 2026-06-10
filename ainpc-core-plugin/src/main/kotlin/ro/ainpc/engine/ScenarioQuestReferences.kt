package ro.ainpc.engine

fun isTrackedQuestSelector(questReference: String?): Boolean =
    when (normalizeReference(questReference)) {
        "tracked", "current", "curent", "urmarit" -> true
        else -> false
    }

fun matchesQuestReference(
    progress: PlayerQuestProgress?,
    questReference: String?,
    template: ScenarioEngine.ScenarioTemplate?,
): Boolean {
    val normalizedReference = normalizeReference(questReference)
    if (progress == null || normalizedReference.isBlank()) {
        return false
    }

    return buildProgressionReferenceCandidates(progress, template)
        .any { candidate -> normalizedReference == normalizeReference(candidate) }
}

fun buildProgressionReferenceCandidates(
    progress: PlayerQuestProgress?,
    template: ScenarioEngine.ScenarioTemplate?,
): List<String> {
    if (progress == null) {
        return emptyList()
    }

    val candidates = LinkedHashSet<String>()
    addProgressionReferenceCandidate(candidates, progress.templateId())
    addProgressionReferenceCandidate(candidates, progress.questCode())

    var definitionId = extractProgressionDefinitionId(progress.templateId())
    addProgressionReferenceCandidate(candidates, definitionId)

    if (template != null) {
        addProgressionReferenceCandidate(candidates, template.templateId)
        addProgressionReferenceCandidate(candidates, template.questCode)
        definitionId = extractProgressionDefinitionId(template.templateId)
        addProgressionReferenceCandidate(candidates, definitionId)

        val code = firstNonBlank(progress.questCode(), template.questCode, definitionId)
        addProgressionReferenceCandidate(candidates, progressionReference(template.progressionMechanicId, code))
        addProgressionReferenceCandidate(candidates, progressionReference(template.progressionMechanicId, definitionId))
        addProgressionReferenceCandidate(candidates, progressionReference(template.progressionKind, code))
        addProgressionReferenceCandidate(candidates, progressionReference(template.progressionKind, definitionId))
        addProgressionReferenceCandidate(
            candidates,
            progressionReference(template.sourcePackId, template.progressionMechanicId, code),
        )
        addProgressionReferenceCandidate(
            candidates,
            progressionReference(template.sourcePackId, template.progressionMechanicId, definitionId),
        )
    }

    return candidates.toList()
}

fun extractProgressionDefinitionId(templateId: String?): String {
    if (templateId.isNullOrBlank()) {
        return ""
    }

    val separator = templateId.indexOf(':')
    return if (separator >= 0 && separator < templateId.length - 1) {
        templateId.substring(separator + 1)
    } else {
        templateId
    }
}

fun progressionReference(vararg parts: String?): String =
    parts
        .filter { !it.isNullOrBlank() }
        .joinToString(":") { it!!.trim() }

private fun addProgressionReferenceCandidate(candidates: MutableSet<String>, value: String?) {
    if (!value.isNullOrBlank()) {
        candidates.add(value)
    }
}
