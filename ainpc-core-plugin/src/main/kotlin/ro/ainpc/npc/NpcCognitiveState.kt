package ro.ainpc.npc

data class NpcCognitiveState(
    val hungerLevel: Int = 100,
    val energyLevel: Int = 100,
    val socialNeedLevel: Int = 100,
    val comfortLevel: Int = 100,
    val safetyLevel: Int = 100,
    val isInDanger: Boolean = false,
    val currentState: NPCState = NPCState.IDLE,
    val hasInteractingPlayer: Boolean = false,
    val currentGoal: String = "",
    val plannedRoutineActivity: String = "",
    val experienceDescription: String = "",
    val decisionDescription: String = "",
    val selfCareProgress: NpcSelfCareGoal.Snapshot? = null,
) {
    val focus: Focus
        get() {
            if (needsImmediateSafety) return Focus.SAFETY
            if (hasInteractingPlayer && currentState in setOf(NPCState.TALKING, NPCState.LISTENING)) {
                return Focus.CONVERSATION
            }

            val hungerThreshold = careThreshold(currentState == NPCState.EATING)
            val energyThreshold = careThreshold(currentState == NPCState.SLEEPING)
            val hungerDeficit = relativeDeficit(hungerLevel, hungerThreshold)
            val energyDeficit = relativeDeficit(energyLevel, energyThreshold)
            if (hungerDeficit > 0.0 || energyDeficit > 0.0) {
                return if (hungerDeficit >= energyDeficit) Focus.HUNGER else Focus.ENERGY
            }

            val secondaryNeed = listOf(
                Focus.SAFETY to relativeDeficit(safetyLevel, 40),
                Focus.COMFORT to relativeDeficit(comfortLevel, 40),
                Focus.SOCIAL to relativeDeficit(socialNeedLevel, 40),
            ).maxBy { it.second }
            if (secondaryNeed.second > 0.0) return secondaryNeed.first
            if (currentGoal.isNotBlank()) return Focus.GOAL
            if (plannedRoutineActivity.isNotBlank()) return Focus.ROUTINE
            return Focus.IDLE
        }

    private val needsImmediateSafety: Boolean
        get() = isInDanger || currentState in setOf(NPCState.COMBAT, NPCState.FLEEING, NPCState.PANICKING)

    fun thoughtDescription(): String = when (focus) {
        Focus.SAFETY -> "Ma gandesc sa raman in siguranta si sa evit pericolul."
        Focus.CONVERSATION -> "Sunt atent la discutia curenta si la ce mi se spune."
        Focus.HUNGER -> "Mi-e foame si ma gandesc sa gasesc ceva de mancare."
        Focus.ENERGY -> "Ma gandesc la odihna; am nevoie sa-mi refac energia."
        Focus.COMFORT -> "Ma gandesc sa gasesc un loc mai confortabil."
        Focus.SOCIAL -> "Ma gandesc sa gasesc pe cineva cu care sa vorbesc."
        Focus.GOAL -> "Ma concentrez pe obiectivul meu imediat: ${sentencePart(currentGoal)}."
        Focus.ROUTINE -> "Ma gandesc la ce am in program: ${sentencePart(plannedRoutineActivity)}."
        Focus.IDLE -> "Sunt atent la ce se intampla in jurul meu."
    }

    fun needsDescription(): String = buildList {
        if (needsImmediateSafety || safetyLevel < 40) add("Am nevoie de siguranta.")
        if (NpcSelfCareThresholds.needsCare(hungerLevel, currentState == NPCState.EATING)) {
            add("Mi-e foame si am nevoie de mancare.")
        }
        if (NpcSelfCareThresholds.needsCare(energyLevel, currentState == NPCState.SLEEPING)) {
            add("Am nevoie de odihna.")
        }
        if (comfortLevel < 40) add("Am nevoie de confort.")
        if (socialNeedLevel < 40) add("Am nevoie de companie.")
    }.joinToString(" ").ifBlank { "Nevoile mele de baza sunt in echilibru acum." }

    fun intentionDescription(): String {
        val currentFocus = focus
        if (currentFocus == Focus.SAFETY) return "Vreau sa gasesc un loc sigur si sa evit pericolul."
        if (currentGoal.isNotBlank()) return "Obiectivul meu imediat este: ${sentencePart(currentGoal)}."
        return when (currentFocus) {
            Focus.CONVERSATION -> "Vreau sa urmaresc discutia curenta."
            Focus.HUNGER -> "Vreau sa gasesc ceva de mancare."
            Focus.ENERGY -> "Vreau sa gasesc un loc de odihna."
            Focus.COMFORT -> "Vreau sa gasesc un loc mai confortabil."
            Focus.SOCIAL -> "Vreau sa gasesc pe cineva cu care sa vorbesc."
            Focus.ROUTINE -> "Vreau sa-mi urmez programul: ${sentencePart(plannedRoutineActivity)}."
            Focus.SAFETY, Focus.GOAL, Focus.IDLE -> "Vreau sa observ ce se intampla in jurul meu."
        }
    }

    fun needModifier(action: NPCAction): Int {
        var modifier = 0
        if (hungerLevel < NpcSelfCareThresholds.START) {
            if (action == NPCAction.EAT) modifier += 45
            if (action.getCategory() == NPCAction.ActionCategory.WORK) modifier -= 12
        }
        if (energyLevel < NpcSelfCareThresholds.START) {
            if (action == NPCAction.SLEEP) modifier += 50
            if (action == NPCAction.REST) modifier += 35
            if (action.getCategory() == NPCAction.ActionCategory.WORK) modifier -= 18
            if (action == NPCAction.INVESTIGATE) modifier -= 10
        }
        if (socialNeedLevel < 40) {
            if (action == NPCAction.SOCIALIZE || action == NPCAction.GREET || action == NPCAction.TALK) modifier += 28
        }
        if (comfortLevel < 40) {
            if (action == NPCAction.REST || action == NPCAction.SLEEP) modifier += 22
            if (action == NPCAction.HIDE) modifier += 14
        }
        if (safetyLevel < 40) {
            if (action == NPCAction.FLEE || action == NPCAction.HIDE || action == NPCAction.CALL_HELP) modifier += 30
            if (action.isAggressive()) modifier -= 10
        }
        return modifier
    }

    private fun careThreshold(alreadyActive: Boolean): Int =
        if (alreadyActive) NpcSelfCareThresholds.RECOVERED else NpcSelfCareThresholds.START

    private fun relativeDeficit(level: Int, threshold: Int): Double =
        (threshold - level.coerceIn(0, 100)).coerceAtLeast(0).toDouble() / threshold

    private fun sentencePart(value: String): String = value.trim().trimEnd('.', '!', '?')

    enum class Focus {
        SAFETY, CONVERSATION, HUNGER, ENERGY, COMFORT, SOCIAL, GOAL, ROUTINE, IDLE,
    }

    companion object {
        fun capture(npc: AINPC, context: NPCContext = npc.context): NpcCognitiveState = NpcCognitiveState(
            hungerLevel = npc.hungerLevel,
            energyLevel = npc.energyLevel,
            socialNeedLevel = npc.socialNeedLevel,
            comfortLevel = npc.comfortLevel,
            safetyLevel = npc.safetyLevel,
            isInDanger = context.isInDanger,
            currentState = npc.currentState,
            hasInteractingPlayer = context.interactingPlayer != null || context.interactingNPC != null,
            currentGoal = npc.currentGoal,
            plannedRoutineActivity = npc.plannedRoutineActivity,
            experienceDescription = npc.actionExperience.latestDescription(System.currentTimeMillis()),
            decisionDescription = npc.lastDecision?.description(System.currentTimeMillis()).orEmpty(),
            selfCareProgress = npc.selfCareGoal.snapshot(System.currentTimeMillis()),
        )
    }
}
