package ro.ainpc.engine

import java.util.UUID

class QuestProgressFinalizationService(
    private val getCurrentQuestProgress: (UUID, String) -> PlayerQuestProgress?,
    private val removeActiveQuestProgress: (UUID, String) -> Boolean,
    private val archiveQuestProgress: (UUID, PlayerQuestProgress) -> Unit,
    private val persistQuestProgressAsync: (UUID, PlayerQuestProgress) -> Unit,
    private val resolveQuestPhase: (ScenarioTemplate, QuestStatus, PlayerQuestProgress?) -> String,
    private val questProgressCleanupService: QuestProgressCleanupService,
    private val publishProgressionFailed: (ScenarioTemplate, PlayerQuestProgress, String) -> Unit,
) {
    fun markQuestCompleted(playerId: UUID, template: ScenarioTemplate): PlayerQuestProgress {
        val now = System.currentTimeMillis()
        val activeProgress = getCurrentQuestProgress(playerId, template.templateId)
        val startedAt = activeProgress?.startedAt() ?: now
        questProgressCleanupService.clearQuestTrackingIfMatches(playerId, template.templateId)
        removeActiveQuestProgress(playerId, template.templateId)
        questProgressCleanupService.clearQuestTrackingData(playerId)
        val completedProgress = PlayerQuestProgress(
            template.templateId,
            template.questCode,
            QuestStatus.COMPLETED,
            startedAt,
            now,
            now,
            resolveQuestPhase(template, QuestStatus.COMPLETED, activeProgress),
            activeProgress?.objectiveProgress() ?: emptyMap(),
            activeProgress?.questVariables() ?: emptyMap()
        )
        archiveQuestProgress(playerId, completedProgress)
        persistQuestProgressAsync(playerId, completedProgress)
        return completedProgress
    }

    fun markQuestFailed(playerId: UUID, template: ScenarioTemplate): PlayerQuestProgress {
        val now = System.currentTimeMillis()
        val activeProgress = getCurrentQuestProgress(playerId, template.templateId)
        val startedAt = activeProgress?.startedAt() ?: now

        questProgressCleanupService.clearQuestTrackingIfMatches(playerId, template.templateId)
        removeActiveQuestProgress(playerId, template.templateId)
        questProgressCleanupService.clearQuestTrackingData(playerId)
        val clearedProgress = clearLocationObjectiveProgress(activeProgress?.objectiveProgress()?.toMutableMap(), template)
        val failedProgress = PlayerQuestProgress(
            template.templateId,
            template.questCode,
            QuestStatus.FAILED,
            startedAt,
            now,
            now,
            resolveQuestPhase(template, QuestStatus.FAILED, activeProgress),
            clearedProgress,
            activeProgress?.questVariables() ?: emptyMap()
        )
        archiveQuestProgress(playerId, failedProgress)
        persistQuestProgressAsync(playerId, failedProgress)
        publishProgressionFailed(template, failedProgress, "quest_failed")
        return failedProgress
    }

    private fun clearLocationObjectiveProgress(
        progressByObjective: MutableMap<String, Int>?,
        template: ScenarioTemplate,
    ): MutableMap<String, Int> {
        val cleared = progressByObjective?.toMutableMap() ?: LinkedHashMap()
        if (template.objectives.isEmpty()) return cleared
        val locationTypes = setOf("visit_region", "visit_place", "inspect_node")
        for ((index, objective) in template.objectives.withIndex()) {
            if (locationTypes.any { matchesObjectiveType(objective, it) }) {
                val key = buildObjectiveKey(objective, index)
                cleared.remove(key)
            }
        }
        return cleared
    }
}
