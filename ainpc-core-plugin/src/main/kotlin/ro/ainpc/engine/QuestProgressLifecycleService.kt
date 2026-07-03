package ro.ainpc.engine

import org.bukkit.entity.Player
import org.bukkit.inventory.PlayerInventory
import java.util.UUID

class QuestProgressLifecycleService(
    private val getCurrentQuestProgress: (UUID, String) -> PlayerQuestProgress?,
    private val putActiveQuestProgress: (UUID, PlayerQuestProgress) -> Unit,
    private val removeArchivedQuestProgress: (UUID, String) -> Boolean,
    private val persistQuestProgressAsync: (UUID, PlayerQuestProgress) -> Unit,
    private val shouldAutoAcceptOnOffer: (ScenarioTemplate?) -> Boolean,
    private val resolveQuestPhase: (ScenarioTemplate, QuestStatus, PlayerQuestProgress?) -> String,
    private val buildObjectiveProgressSnapshot: (PlayerInventory?, ScenarioTemplate?, Map<String, Int>?, String) -> Map<String, Int>,
    private val seedQuestStageVariables: (ScenarioTemplate, QuestStatus?, String, Map<String, String>?, Long) -> Map<String, String>,
    private val publishProgressionStageChanged: (UUID, Player?, ScenarioTemplate, PlayerQuestProgress, String, String) -> Unit,
) {
    fun setInitialQuestProgress(playerId: UUID, player: Player?, template: ScenarioTemplate): PlayerQuestProgress {
        return if (shouldAutoAcceptOnOffer(template)) {
            setActiveQuestProgress(playerId, player, template)
        } else {
            setOfferedQuestProgress(playerId, player, template)
        }
    }

    fun setOfferedQuestProgress(playerId: UUID, player: Player?, template: ScenarioTemplate): PlayerQuestProgress {
        return setCurrentQuestProgress(playerId, player, template, QuestStatus.OFFERED)
    }

    fun setActiveQuestProgress(playerId: UUID, player: Player?, template: ScenarioTemplate): PlayerQuestProgress {
        return setCurrentQuestProgress(playerId, player, template, QuestStatus.ACTIVE)
    }

    fun setCurrentQuestProgress(playerId: UUID, player: Player?, template: ScenarioTemplate, status: QuestStatus): PlayerQuestProgress {
        val now = System.currentTimeMillis()
        val matchingProgress = getCurrentQuestProgress(playerId, template.templateId)
        val startedAt = matchingProgress?.startedAt() ?: now
        val currentPhase = resolveQuestPhase(template, status, matchingProgress)
        val previousPhase = matchingProgress?.currentPhase().orEmpty()

        val objectiveSnapshot = buildObjectiveProgressSnapshot(
            player?.inventory,
            template,
            matchingProgress?.objectiveProgress(),
            currentPhase
        )
        val questVariables = seedQuestStageVariables(
            template,
            status,
            currentPhase,
            matchingProgress?.questVariables(),
            now
        )

        val currentProgress = PlayerQuestProgress(
            template.templateId,
            template.questCode,
            status,
            startedAt,
            0L,
            now,
            currentPhase,
            objectiveSnapshot,
            questVariables
        )
        putActiveQuestProgress(playerId, currentProgress)
        removeArchivedQuestProgress(playerId, template.templateId)
        persistQuestProgressAsync(playerId, currentProgress)
        if (previousPhase.isNotBlank() && previousPhase != currentPhase && player != null) {
            publishProgressionStageChanged(
                playerId,
                player,
                template,
                currentProgress,
                previousPhase,
                currentPhase
            )
        }
        return currentProgress
    }
}
