package ro.ainpc.engine.runtime

import ro.ainpc.engine.ScenarioEngine
import ro.ainpc.engine.ScenarioTemplate
import java.util.UUID

class QuestVariableProvider(private val engine: ScenarioEngine) : ScenarioVariableProvider {
    override fun namespace(): String = "quest"

    override fun variables(context: ScenarioExecutionContext): Map<String, String> {
        val vars = mutableMapOf<String, String>()
        val playerUuid = context.playerUuid()
        if (playerUuid.isBlank()) return vars

        val uuid = runCatching { UUID.fromString(playerUuid) }.getOrNull() ?: return vars
        val templateId = context.templateId()

        vars["player_uuid"] = playerUuid
        vars["mechanic_id"] = context.progressionId()

        if (templateId.isNotBlank()) {
            val completed = engine.hasCompletedQuest(uuid, templateId)
            vars["quest_completed_$templateId"] = completed.toString()

            val completedProgress = engine.getCompletedQuestProgress(uuid, templateId)
            if (completedProgress != null) {
                vars["quest_completed_${templateId}_at"] = completedProgress.completedAt().toString()
            }

            val currentProgress = engine.getCurrentQuestProgress(uuid, templateId)
            if (currentProgress != null) {
                vars["quest_active_$templateId"] = "true"
                vars["quest_status_$templateId"] = currentProgress.status()?.name?.lowercase() ?: "unknown"
                vars["quest_phase_$templateId"] = currentProgress.currentPhase()
            }
        }

        val mechanicId = context.variable("mechanic_id").ifBlank { context.progressionId() }
        if (mechanicId.isNotBlank()) {
            val count = engine.countCurrentProgressionsInMechanic(uuid, null, mechanicId)
            vars["mechanic_active_count_$mechanicId"] = count.toString()
        }

        return vars
    }
}
