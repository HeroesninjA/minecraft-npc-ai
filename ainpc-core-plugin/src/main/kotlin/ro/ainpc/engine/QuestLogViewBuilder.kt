package ro.ainpc.engine

import org.bukkit.entity.Player
import ro.ainpc.npc.AINPC
import java.util.UUID

class QuestLogViewBuilder(
    private val parseQuestLogFilter: (String?) -> QuestLogFilter,
    private val getCurrentQuestProgress: (UUID) -> List<PlayerQuestProgress>,
    private val getArchivedQuestProgress: (UUID) -> List<PlayerQuestProgress>,
    private val resolveTemplateForProgress: (PlayerQuestProgress, AINPC?) -> ScenarioTemplate?,
    private val refreshTrackedQuestProgress: (Player, ScenarioTemplate, PlayerQuestProgress) -> PlayerQuestProgress,
    private val questLogMatches: (UUID, PlayerQuestProgress, QuestLogFilter, Boolean) -> Boolean,
    private val questLogCurrentComparator: (UUID) -> Comparator<PlayerQuestProgress>,
    private val buildQuestLogSummaryLines: (UUID, List<PlayerQuestProgress>) -> List<String>,
    private val questLogCurrentGroupLabel: (UUID, ScenarioTemplate?, PlayerQuestProgress) -> String,
    private val isTrackedQuest: (UUID, PlayerQuestProgress) -> Boolean,
    private val resolveQuestTitle: (ScenarioTemplate) -> String,
    private val resolveQuestNpcName: (PlayerQuestProgress?) -> String,
    private val buildQuestStatusMessages: (ScenarioTemplate, PlayerQuestProgress?, Player, String) -> List<String>,
    private val buildQuestLogActionLines: (Player, UUID, ScenarioTemplate?, PlayerQuestProgress, Boolean) -> List<String>,
    private val formatQuestStatus: (QuestStatus) -> String,
    private val formatQuestPhase: (String) -> String,
    private val formatQuestLogArchivedLine: (UUID, ScenarioTemplate?, PlayerQuestProgress, String) -> String,
) {
    fun build(player: Player, filterText: String, adminView: Boolean): QuestInteractionResult {
        val playerId = player.uniqueId
        val logFilter = parseQuestLogFilter(filterText)
        val currentProgresses = getCurrentQuestProgress(playerId)
        val archivedProgresses = getArchivedQuestProgress(playerId)
        val systemMessages = mutableListOf<String>()
        systemMessages.add("&6=== Progression Log ===")
        systemMessages.add("&eJucator: &f" + player.name)
        if (logFilter != QuestLogFilter.SUMMARY) {
            systemMessages.add("&eFiltru: &f" + logFilter.displayName())
        }
        if (currentProgresses.isNotEmpty()) {
            systemMessages.addAll(buildQuestLogSummaryLines(playerId, currentProgresses))
        }
        val matchingCurrent = currentProgresses
            .filter { questLogMatches(playerId, it, logFilter, false) }
            .sortedWith(questLogCurrentComparator(playerId))
        if (matchingCurrent.isNotEmpty()) {
            systemMessages.add("&aProgresii curente: &f" + matchingCurrent.size)
            var currentGroup = ""
            for (currentProgress in matchingCurrent) {
                val template = resolveTemplateForProgress(currentProgress, null)
                val logProgress = if (template != null) {
                    refreshTrackedQuestProgress(player, template, currentProgress)
                } else {
                    currentProgress
                }
                val groupLabel = questLogCurrentGroupLabel(playerId, template, logProgress)
                if (groupLabel != currentGroup) {
                    systemMessages.add(groupLabel)
                    currentGroup = groupLabel
                }
                if (template != null) {
                    if (isTrackedQuest(playerId, logProgress)) {
                        systemMessages.add("&bQuest urmarit: &f" + resolveQuestTitle(template))
                    }
                    systemMessages.addAll(buildQuestStatusMessages(template, logProgress, player, resolveQuestNpcName(logProgress)))
                    systemMessages.addAll(buildQuestLogActionLines(player, playerId, template, logProgress, adminView))
                } else {
                    systemMessages.add("&7Template: &f" + logProgress.templateId())
                    systemMessages.add("&7Status: &f" + formatQuestStatus(logProgress.status()))
                    if (isTrackedQuest(playerId, logProgress)) {
                        systemMessages.add("&bProgresie urmarita: &fda")
                    }
                    if (logProgress.currentPhase().isNotBlank()) {
                        systemMessages.add("&7Faza curenta: &f" + formatQuestPhase(logProgress.currentPhase()))
                    }
                    systemMessages.addAll(buildQuestLogActionLines(player, playerId, null, logProgress, adminView))
                }
            }
        } else if (logFilter.showsCurrent()) {
            systemMessages.add(
                if (logFilter == QuestLogFilter.SUMMARY) {
                    "&7Nu ai progresie activa."
                } else {
                    "&7Nu exista progresii curente pentru filtrul ales."
                }
            )
        }
        val archivedLimit = if (logFilter == QuestLogFilter.SUMMARY) 3 else 20
        val matchingArchived = archivedProgresses
            .filter { questLogMatches(playerId, it, logFilter, true) }
            .take(archivedLimit)
        if (matchingArchived.isNotEmpty()) {
            systemMessages.add(
                if (logFilter == QuestLogFilter.SUMMARY) "&eUltimele progresii:" else "&eProgresii arhivate:"
            )
            for (archivedProgress in matchingArchived) {
                val template = resolveTemplateForProgress(archivedProgress, null)
                val title = if (template != null) resolveQuestTitle(template) else archivedProgress.templateId()
                systemMessages.add(formatQuestLogArchivedLine(playerId, template, archivedProgress, title ?: ""))
            }
            val totalMatchingArchived = archivedProgresses.count { questLogMatches(playerId, it, logFilter, true) }.toLong()
            if (totalMatchingArchived > matchingArchived.size) {
                systemMessages.add("&7... inca &f" + (totalMatchingArchived - matchingArchived.size)
                    + " &7progresii arhivate pentru filtrul ales.")
            }
        } else if (logFilter.showsArchived() && logFilter != QuestLogFilter.SUMMARY) {
            systemMessages.add("&7Nu exista progresii arhivate pentru filtrul ales.")
        }
        return QuestInteractionResult.handled(false, listOf(), systemMessages)
    }
}
