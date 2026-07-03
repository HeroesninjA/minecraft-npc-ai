package ro.ainpc.engine

import org.bukkit.entity.Player
import ro.ainpc.npc.AINPC
import java.util.UUID

class QuestGuiEntryBuilder(
    private val featurePackLoader: FeaturePackLoader,
    private val resolveTemplateForProgress: (PlayerQuestProgress, AINPC?) -> ScenarioTemplate?,
    private val refreshTrackedQuestProgress: (Player, ScenarioTemplate, PlayerQuestProgress) -> PlayerQuestProgress,
    private val questLogActionSelector: (ScenarioTemplate?, PlayerQuestProgress) -> String,
    private val resolveQuestTitle: (ScenarioTemplate) -> String,
    private val resolveQuestCategory: (ScenarioTemplate) -> QuestScenarioContract.Category,
    private val resolveProgressionMechanicDisplay: (FeaturePackLoader, ScenarioTemplate) -> String,
    private val formatQuestStatus: (QuestStatus) -> String,
    private val resolveQuestPhase: (ScenarioTemplate, QuestStatus, PlayerQuestProgress) -> String,
    private val buildQuestStatusMessages: (ScenarioTemplate, PlayerQuestProgress?, Player, String) -> List<String>,
    private val isTrackedQuest: (UUID, PlayerQuestProgress) -> Boolean,
    private val formatQuestPhase: (String) -> String,
    private val resolveQuestNpcName: (PlayerQuestProgress?) -> String,
    private val buildQuestGuiObjectives: (Player, ScenarioTemplate, PlayerQuestProgress) -> List<QuestGuiObjective>,
    private val buildQuestGuiStages: (ScenarioTemplate, PlayerQuestProgress, String) -> List<QuestGuiStage>,
    private val buildQuestLogActionLines: (Player, UUID, ScenarioTemplate?, PlayerQuestProgress, Boolean) -> List<String>,
    private val buildMissingQuestTemplateLines: (PlayerQuestProgress) -> List<String>,
    private val valueOrFallback: (String, String) -> String,
) {
    fun build(
        player: Player,
        playerId: UUID,
        progress: PlayerQuestProgress,
        archived: Boolean,
        adminView: Boolean,
    ): QuestGuiEntry {
        val template = resolveTemplateForProgress(progress, null)
        val viewProgress: PlayerQuestProgress = if (template != null && progress.isCurrent()) {
            refreshTrackedQuestProgress(player, template, progress)
        } else {
            progress
        }

        val selector = questLogActionSelector(template, viewProgress)
        val title = if (template != null) resolveQuestTitle(template) else valueOrFallback(viewProgress.templateId(), "Quest necunoscut")
        val category = if (template != null) resolveQuestCategory(template).displayName() else "Necunoscut"
        val mechanic = if (template != null) resolveProgressionMechanicDisplay(featurePackLoader, template) else "Necunoscuta"
        val statusDisplay = formatQuestStatus(viewProgress.status())
        var currentStageId = viewProgress.currentPhase()
        if (currentStageId.isBlank() && template != null) {
            currentStageId = resolveQuestPhase(template, viewProgress.status(), viewProgress)
        }

        val statusLines = if (template != null) {
            buildQuestStatusMessages(template, viewProgress, player, resolveQuestNpcName(viewProgress))
        } else {
            buildMissingQuestTemplateLines(viewProgress)
        }

        return QuestGuiEntry(
            selector,
            valueOrFallback(viewProgress.templateId(), ""),
            valueOrFallback(viewProgress.questCode(), ""),
            title,
            statusDisplay,
            category,
            mechanic,
            isTrackedQuest(playerId, viewProgress),
            viewProgress.isCurrent(),
            viewProgress.isActive(),
            viewProgress.isOffered(),
            archived,
            template == null,
            currentStageId,
            formatQuestPhase(currentStageId),
            viewProgress.updatedAt(),
            resolveQuestNpcName(viewProgress),
            statusLines,
            if (template != null) buildQuestGuiObjectives(player, template, viewProgress) else emptyList(),
            if (template != null) buildQuestGuiStages(template, viewProgress, currentStageId) else emptyList(),
            if (template != null) template.rewards.map { formatQuestEntry(it) } else emptyList(),
            buildQuestLogActionLines(player, playerId, template, viewProgress, adminView)
        )
    }
}
