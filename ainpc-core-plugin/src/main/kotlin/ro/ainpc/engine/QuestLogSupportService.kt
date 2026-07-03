package ro.ainpc.engine

import java.util.UUID

class QuestLogSupportService(
    private val getQuestTemplate: (String) -> ScenarioTemplate?,
    private val getCurrentQuestProgress: (UUID, String) -> PlayerQuestProgress?,
    private val getArchivedQuestProgress: (UUID) -> List<PlayerQuestProgress>,
) {
    fun collectFailureReasons(playerId: UUID, templateId: String): List<String> {
        val reasons = mutableListOf<String>()
        val template = getQuestTemplate(templateId) ?: return reasons
        val progress = getCurrentQuestProgress(playerId, templateId) ?: return reasons
        if (template.objectives.isNotEmpty()) {
            val incompleteObjectives = template.objectives.filterIndexed { index, obj ->
                val key = buildObjectiveKey(obj, index)
                (progress.objectiveProgress()[key] ?: 0) < obj.amount
            }
            if (incompleteObjectives.isNotEmpty()) {
                reasons.add("objective_incomplete: ${incompleteObjectives.size}/${template.objectives.size}")
            }
        }
        val elapsed = (System.currentTimeMillis() - progress.startedAt()) / 1000
        if (elapsed > 0) reasons.add("elapsed_seconds=$elapsed")
        return reasons
    }

    fun questLogCurrentComparator(playerId: UUID): Comparator<PlayerQuestProgress> {
        return Comparator { a, b -> (b.updatedAt() - a.updatedAt()).toInt() }
    }

    fun buildQuestLogSummaryLines(playerId: UUID, progresses: List<PlayerQuestProgress>): List<String> {
        return progresses.map { it.templateId() ?: "unknown" }
    }

    fun questLogCurrentGroupLabel(playerId: UUID, template: ScenarioTemplate?, progress: PlayerQuestProgress): String {
        return template?.displayName ?: progress.templateId() ?: "Quest"
    }

    fun questLogMatches(playerId: UUID, progress: PlayerQuestProgress, filter: QuestLogFilter, archivedHint: Boolean): Boolean {
        val archived = getArchivedQuestProgress(playerId).any { it.templateId() == progress.templateId() }
        return when (filter) {
            QuestLogFilter.ALL -> true
            QuestLogFilter.ACTIVE -> !archived && progress.isActive()
            QuestLogFilter.ARCHIVED -> archived
            else -> !archived
        }
    }

    fun formatQuestLogArchivedLine(playerId: UUID, template: ScenarioTemplate?, progress: PlayerQuestProgress, title: String): String {
        return title
    }
}
