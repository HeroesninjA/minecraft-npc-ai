package ro.ainpc.engine

import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import ro.ainpc.npc.AINPC
import java.util.UUID

class QuestProgressViewBuilder(
    private val plugin: AINPCPlugin,
    private val findQuestProgressByReference: (UUID, String, Boolean) -> PlayerQuestProgress?,
    private val getTrackedQuestProgress: (UUID, Boolean) -> PlayerQuestProgress?,
    private val resolveTemplateForProgress: (PlayerQuestProgress, AINPC?) -> ScenarioTemplate?,
    private val refreshTrackedQuestProgress: (Player, ScenarioTemplate, PlayerQuestProgress) -> PlayerQuestProgress,
    private val isTrackedQuest: (UUID, PlayerQuestProgress) -> Boolean,
    private val buildQuestStatusMessages: (ScenarioTemplate, PlayerQuestProgress?, Player, String) -> List<String>,
    private val resolveQuestNpcName: (PlayerQuestProgress?) -> String,
    private val formatQuestStatus: (QuestStatus) -> String,
    private val formatQuestPhase: (String) -> String,
    private val isTrackedQuestSelector: (String) -> Boolean,
) {
    fun buildPlayerStatus(player: Player, reference: String): QuestInteractionResult {
        if (reference.isBlank()) {
            return QuestInteractionResult.notHandled()
        }
        var progress = findQuestProgressByReference(player.uniqueId, reference, true)
        if (progress == null) {
            return QuestInteractionResult.notHandled()
        }
        val template = resolveTemplateForProgress(progress, null)
        if (template != null && progress.isCurrent()) {
            progress = refreshTrackedQuestProgress(player, template, progress)
        }
        val systemMessages = mutableListOf<String>()
        systemMessages.add("&6=== Progression Status ===")
        systemMessages.add("&eJucator: &f" + player.name)
        if (template != null) {
            systemMessages.addAll(
                buildQuestStatusMessages(
                    template,
                    progress,
                    player,
                    resolveQuestNpcName(progress)
                )
            )
        } else {
            systemMessages.add("&eProgresie: &f" + progress.templateId())
            systemMessages.add("&7Status: &f" + formatQuestStatus(progress.status()))
            if (progress.currentPhase().isNotBlank()) {
                systemMessages.add("&7Faza curenta: &f" + formatQuestPhase(progress.currentPhase()))
            }
            systemMessages.add("&cTemplate-ul questului nu mai este disponibil in configuratia curenta.")
        }
        return QuestInteractionResult.handled(false, listOf(), systemMessages)
    }

    fun buildDebug(player: Player, reference: String): QuestInteractionResult {
        if (reference.isBlank()) {
            return QuestInteractionResult.notHandled()
        }
        val playerId = player.uniqueId
        var progress = if (isTrackedQuestSelector(reference)) {
            getTrackedQuestProgress(playerId, true)
        } else {
            findQuestProgressByReference(playerId, reference, true)
        }
        if (progress == null) {
            return QuestInteractionResult.notHandled()
        }
        val template = resolveTemplateForProgress(progress, null)
        if (template != null && progress.isCurrent()) {
            progress = refreshTrackedQuestProgress(player, template, progress)
        }
        val systemMessages = mutableListOf<String>()
        systemMessages.add("&6=== Progression Debug ===")
        systemMessages.add("&eJucator: &f" + player.name + " &7(" + playerId + ")")
        systemMessages.add("&eSelector: &f" + reference)
        systemMessages.add("&eTemplate: &f" + progress.templateId())
        systemMessages.add("&eCod: &f" + formatOptional(progress.questCode()))
        systemMessages.add("&7Status: &f" + formatQuestStatus(progress.status()))
        systemMessages.add("&7Tracked: &f" + (if (isTrackedQuest(playerId, progress)) "da" else "nu"))
        systemMessages.add("&7Faza: &f" + formatOptional(progress.currentPhase()))
        systemMessages.add(
            "&7Started/Completed/Updated: &f"
                + formatQuestDebugTime(progress.startedAt()) + " / "
                + formatQuestDebugTime(progress.completedAt()) + " / "
                + formatQuestDebugTime(progress.updatedAt())
        )
        if (template != null) {
            systemMessages.add("&eTitlu: &f" + resolveQuestTitle(template))
            if (template.progressionMechanicId.isNotBlank()) {
                systemMessages.add(
                    "&7Progression: &f" + template.progressionMechanicId
                        + " &7/ kind=&f" + formatOptional(template.progressionKind)
                        + " &7/ label=&f" + formatOptional(template.progressionLabel)
                )
            }
            val storyContext = plugin.storyContextService.buildForPlayer(player)
            val authoringSnapshot = plugin.authoringService.analyze(
                storyContext,
                plugin.progressionService.getDefinitions(),
                reference,
                template.progressionMechanicId,
                storyContext.worldContext().currentRegion()?.id(),
                storyContext.worldContext().currentPlace()?.id(),
                true,
                emptyList()
            )
            systemMessages.add(
                "&eAuthoring: &f" + authoringSnapshot.decisionStatus()
                    + " &7/ reason=&f" + formatOptional(authoringSnapshot.decisionReason())
            )
            if (authoringSnapshot.selectedProgressionId().isNotBlank()) {
                systemMessages.add(
                    "&7Authoring progression: &f" + authoringSnapshot.selectedProgressionId()
                        + " &7/ mechanic=&f" + formatOptional(authoringSnapshot.selectedMechanicId())
                )
            }
            systemMessages.add("&7Giver profession: &f" + formatOptional(template.questGiverProfession))
            val contract = template.questContract
            systemMessages.add(
                "&7Contract: &f" + contract.displayName()
                    + " &7/ categorie=&f" + contract.categoryDisplayName()
                    + " &7/ acceptare=&f" + contract.acceptanceMode().name.lowercase(java.util.Locale.ROOT)
            )
            systemMessages.add("&eObiective template:")
            val objectives = template.objectives
            if (objectives.isEmpty()) {
                systemMessages.add("&7- &f<gol>")
            } else {
                for (index in objectives.indices) {
                    val objective = objectives[index]
                    val objectiveKey = buildObjectiveKey(objective, index)
                    val current = if (progress.isCurrent()) {
                        resolveObjectiveCurrentProgress(player, objective, progress, index)
                    } else {
                        readObjectiveProgress(progress.objectiveProgress(), objective, index)
                    }
                    val activeForStage = shouldShowObjectiveForCurrentStage(template, progress, objective)
                    val state = resolveQuestObjectiveState(progress, current, objective.amount, activeForStage)
                    systemMessages.add(
                        "&7- &f" + objectiveKey
                            + " &7type=&f" + normalizeObjectiveType(objective.type)
                            + " &7stage=&f" + formatOptional(canonicalQuestPhase(template, getObjectiveStage(objective)))
                            + " &7target=&f" + formatOptional(objective.itemId)
                            + " &7state=&f" + state.displayName()
                            + " &7progress=&f" + minOf(current, maxOf(1, objective.amount))
                            + "/" + maxOf(1, objective.amount)
                    )
                }
            }
        } else {
            systemMessages.add("&cTemplate-ul questului nu mai este disponibil in configuratia curenta.")
        }
        systemMessages.add("&eObjective progress:")
        systemMessages.addAll(formatQuestDebugMap(progress.objectiveProgress(), 20))
        systemMessages.add("&eQuest variables:")
        systemMessages.addAll(formatQuestDebugMap(progress.questVariables(), 30))
        return QuestInteractionResult.handled(false, listOf(), systemMessages)
    }

    private fun formatOptional(value: String?): String = ro.ainpc.engine.formatOptional(value)
    private fun formatQuestDebugTime(epochMillis: Long): String = ro.ainpc.engine.formatQuestDebugTime(epochMillis)
    private fun formatQuestDebugMap(values: Map<String, *>?, limit: Int): List<String> = ro.ainpc.engine.formatQuestDebugMap(values, limit)
    private fun resolveQuestTitle(template: ScenarioTemplate): String = ro.ainpc.engine.resolveQuestTitle(template)
    private fun buildObjectiveKey(objective: FeaturePackLoader.QuestEntryDefinition?, index: Int): String = ro.ainpc.engine.buildObjectiveKey(objective, index)
    private fun readObjectiveProgress(values: Map<String, Int>, objective: FeaturePackLoader.QuestEntryDefinition?, index: Int): Int = ro.ainpc.engine.readObjectiveProgress(values, objective, index)
    private fun resolveObjectiveCurrentProgress(player: Player?, objective: FeaturePackLoader.QuestEntryDefinition?, progress: PlayerQuestProgress?, index: Int): Int = ro.ainpc.engine.resolveObjectiveCurrentProgress(player, objective, progress, index)
    private fun shouldShowObjectiveForCurrentStage(template: ScenarioTemplate?, progress: PlayerQuestProgress?, objective: FeaturePackLoader.QuestEntryDefinition?): Boolean = ro.ainpc.engine.shouldShowObjectiveForCurrentStage(template, progress, objective)
    private fun resolveQuestObjectiveState(progress: PlayerQuestProgress?, current: Int, required: Int, activeForStage: Boolean): QuestObjectiveState = ro.ainpc.engine.resolveQuestObjectiveState(progress, current, required, activeForStage)
    private fun normalizeObjectiveType(type: String?): String = ro.ainpc.engine.normalizeObjectiveType(type)
    private fun canonicalQuestPhase(template: ScenarioTemplate?, phase: String?): String = ro.ainpc.engine.canonicalQuestPhase(template, phase)
    private fun getObjectiveStage(objective: FeaturePackLoader.QuestEntryDefinition?): String = ro.ainpc.engine.getObjectiveStage(objective)
    private fun isTrackedQuest(playerId: UUID, progress: PlayerQuestProgress): Boolean = this.isTrackedQuest.invoke(playerId, progress)
}
