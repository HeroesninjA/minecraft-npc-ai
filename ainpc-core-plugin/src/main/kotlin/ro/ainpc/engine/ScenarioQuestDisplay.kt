package ro.ainpc.engine

import org.bukkit.entity.Player

fun buildQuestNpcMessages(
    template: ScenarioEngine.ScenarioTemplate?,
    progress: PlayerQuestProgress?,
    context: QuestDialogueContext?,
    fallback: List<String>,
): List<String> {
    val configuredMessages = resolveQuestDialogueMessages(template, progress, context)
    return if (configuredMessages.isEmpty()) fallback else configuredMessages
}

fun resolveQuestDialogueMessages(
    template: ScenarioEngine.ScenarioTemplate?,
    progress: PlayerQuestProgress?,
    context: QuestDialogueContext?,
): List<String> {
    if (template == null || context == null || template.questDialogues.isEmpty()) return listOf()
    val keys = mutableListOf<String>()
    keys.addAll(context.dialogueKeys())
    if (progress != null && !progress.currentPhase().isNullOrBlank()) {
        keys.add("phase." + progress.currentPhase())
        keys.add(progress.currentPhase())
    }
    for (key in keys) {
        val lines = template.getQuestDialogueLines(key)
        if (lines.isNotEmpty()) return lines
    }
    return listOf()
}

fun resolveStatusDialogueContext(
    player: Player?,
    template: ScenarioEngine.ScenarioTemplate?,
    progress: PlayerQuestProgress?,
): QuestDialogueContext {
    if (progress == null) return QuestDialogueContext.OFFER
    if (progress.isCompleted()) return QuestDialogueContext.COMPLETED
    if (progress.status() == QuestStatus.FAILED) return QuestDialogueContext.FAILED
    if (progress.isActive() && inspectQuestObjectives(player, template, progress, null, false).complete()) {
        return QuestDialogueContext.READY
    }
    return if (progress.isOffered()) QuestDialogueContext.OFFERED else QuestDialogueContext.ACTIVE
}

fun buildObjectiveProgressLines(
    template: ScenarioEngine.ScenarioTemplate?,
    progress: PlayerQuestProgress?,
    player: Player?,
): List<String> {
    if (template == null || progress == null || template.objectives.isEmpty()) return listOf()
    return template.objectives.mapIndexedNotNull { index, objective ->
        val activeForStage = shouldShowObjectiveForCurrentStage(template, progress, objective)
        if (!activeForStage) return@mapIndexedNotNull null
        val currentAmount = resolveObjectiveCurrentProgress(player, objective, progress, index)
        val requiredAmount = kotlin.math.max(1, objective.amount)
        val label = formatObjectiveProgressLabel(objective)
        val state = resolveQuestObjectiveState(progress, currentAmount, requiredAmount, activeForStage)
        "&7- &f$label: &e${kotlin.math.min(currentAmount, requiredAmount)}&7/&f$requiredAmount &8(${state.displayName()})"
    }
}

fun buildQuestProgressDetailLines(
    player: Player?,
    template: ScenarioEngine.ScenarioTemplate?,
    progress: PlayerQuestProgress?,
): List<String> {
    if (template == null || progress == null || template.objectives.isEmpty()) return listOf()
    val lines = mutableListOf<String>()
    for ((index, objective) in template.objectives.withIndex()) {
        val objectiveKey = buildObjectiveKey(objective, index)
        val requiredAmount = kotlin.math.max(1, objective.amount)
        val currentAmount = if (progress.isCurrent())
            resolveObjectiveCurrentProgress(player, objective, progress, index)
        else
            readObjectiveProgress(progress.objectiveProgress(), objective, index)
        val activeForStage = shouldShowObjectiveForCurrentStage(template, progress, objective)
        val state = resolveQuestObjectiveState(progress, currentAmount, requiredAmount, activeForStage)
        val stageId = findObjectiveStageId(template, objective)

        val line = StringBuilder("&7- &f")
            .append(formatObjectiveProgressLabel(objective))
            .append(" &8[$objectiveKey]")
            .append(" &7stare=&f")
            .append(state.displayName())
            .append(" &7progres=&e")
            .append(kotlin.math.min(currentAmount, requiredAmount))
            .append("&7/&f")
            .append(requiredAmount)
        if (stageId.isNotBlank()) {
            line.append(" &7stage=&f").append(formatQuestPhase(stageId))
        }
        if (!activeForStage) {
            line.append(" &8(inactiv)")
        }
        lines.add(line.toString())
    }
    return lines
}
