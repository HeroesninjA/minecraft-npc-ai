package ro.ainpc.engine

import ro.ainpc.engine.FeaturePackLoader.QuestEntryDefinition
import ro.ainpc.engine.ScenarioEngine.ScenarioTemplate
import ro.ainpc.npc.AINPC

fun matchesProfessionReference(
    loader: FeaturePackLoader?,
    npc: AINPC?,
    references: List<String>?,
): Boolean {
    if (npc == null || references.isNullOrEmpty()) return false
    val occupation = npc.occupation
    if (occupation.isNullOrBlank()) return false
    for (reference in references) {
        if (reference.isNullOrBlank()) continue
        if (loader != null && loader.matchesProfession(occupation, reference)) return true
        if (normalizeScenarioToken(occupation) == normalizeScenarioToken(reference)) return true
    }
    return false
}

fun matchesQuestGiver(
    loader: FeaturePackLoader?,
    npc: AINPC?,
    template: ScenarioTemplate?,
): Boolean {
    if (npc == null || template == null) return false
    if (template.questGiverProfession.isNotBlank()
        && !matchesProfessionReference(loader, npc, listOf(template.questGiverProfession))
    ) return false
    val questGiverRole = template.roles["QUEST_GIVER"] ?: return true
    if (questGiverRole.requiredProfessions.isNotEmpty()
        && !matchesProfessionReference(loader, npc, questGiverRole.requiredProfessions)
    ) return false
    return questGiverRole.preferredProfessions.isEmpty()
        || matchesProfessionReference(loader, npc, questGiverRole.preferredProfessions)
}

fun matchesNpcObjective(
    loader: FeaturePackLoader?,
    objective: QuestEntryDefinition?,
    npc: AINPC?,
    template: ScenarioTemplate?,
    progress: PlayerQuestProgress?,
): Boolean {
    if (objective == null || npc == null) return false
    val reference = objective.itemId
    if (reference.isNullOrBlank()) {
        return matchesStoredQuestNpc(progress, npc) || matchesQuestGiver(loader, npc, template)
    }
    if (matchesObjectiveReference(reference, npc.name.orEmpty(), npc.displayName.orEmpty(), npc.occupation.orEmpty())) return true
    if (npc.uuid != null && matchesObjectiveReference(reference, npc.uuid.toString())) return true
    if (npc.databaseId > 0 && matchesObjectiveReference(reference, npc.databaseId.toString())) return true
    return false
}
