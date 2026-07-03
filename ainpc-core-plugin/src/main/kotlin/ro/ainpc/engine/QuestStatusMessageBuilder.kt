package ro.ainpc.engine

import org.bukkit.entity.Player
import java.util.UUID

class QuestStatusMessageBuilder(
    private val collectFailureReasons: (UUID, String) -> List<String>,
) {
    fun build(template: ScenarioTemplate, progress: PlayerQuestProgress, player: Player): List<String> {
        val title = resolveQuestTitle(template)
        val status = when {
            progress.isCompleted() -> "&a[COMPLETATA]"
            progress.isActive() -> "&e[ACTIVA]"
            progress.isOffered() -> "&6[OFERITA]"
            progress.status() == QuestStatus.FAILED -> "&c[ESUATA]"
            else -> "&7[IN CURS]"
        }
        val lines = mutableListOf<String>()
        lines.add("&6=== $title &6===")
        lines.add("&7Status: $status")
        if (progress.status() == QuestStatus.FAILED) {
            val reasons = collectFailureReasons(player.uniqueId, template.templateId)
            if (reasons.isNotEmpty()) {
                lines.add("&cMotive esec: ${reasons.joinToString(", ")}")
            }
        }
        if (template.description.isNotBlank()) {
            lines.add("&7" + template.description)
        }
        if (template.objectives.isNotEmpty()) {
            lines.add("&6Obiective:")
            val objectiveLines = buildObjectiveProgressLines(template, progress, player)
            if (objectiveLines.isNotEmpty()) {
                lines.addAll(objectiveLines)
            } else {
                for ((index, objective) in template.objectives.withIndex()) {
                    val label = formatObjectiveProgressLabel(objective)
                    lines.add("&7- &f$label &7x&f${objective.amount}")
                }
            }
        }
        if (template.rewards.isNotEmpty()) {
            lines.add("&6Recompense:")
            for (reward in template.rewards) {
                val rewardLabel = formatRewardLabel(reward)
                lines.add("&7- &f$rewardLabel")
            }
        }
        return lines
    }
}
