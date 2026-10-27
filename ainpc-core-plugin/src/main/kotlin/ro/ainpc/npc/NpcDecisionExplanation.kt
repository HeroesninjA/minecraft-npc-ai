package ro.ainpc.npc

data class NpcDecisionExplanation(
    val action: NPCAction,
    val selectedAt: Long,
    val variedChoice: Boolean,
    val primaryFactor: Factor? = null,
    val secondaryFactor: Factor? = null,
    val continuingSelfCare: Boolean = false,
    val fromCarePlan: Boolean = false,
) {
    fun description(now: Long): String {
        if (selectedAt <= 0L || now < selectedAt || now - selectedAt >= MAX_AGE) return ""
        return buildString {
            append("La ultima decizie, am ales ").append(actionIntention()).append(".")
            if (fromCarePlan) {
                append(" Alegerea a venit din planul local de ingrijire; refacerea nu este confirmata de aceasta alegere.")
            }
            if (continuingSelfCare) {
                append(" Am pastrat intoarcerea acasa pentru obiectivul de ingrijire in curs.")
            }
            val factors = listOfNotNull(primaryFactor, secondaryFactor).distinct()
            if (factors.isNotEmpty()) {
                append(" Printre factorii favorabili se aflau ")
                append(factors.joinToString(" si ") { it.description }).append(".")
            }
            if (variedChoice) {
                append(" Erau mai multe optiuni potrivite, iar alegerea putea varia.")
            }
        }
    }

    private fun actionIntention(): String = when (action) {
        NPCAction.EAT -> "sa ma ocup de hrana"
        NPCAction.SLEEP, NPCAction.REST -> "sa ma odihnesc"
        NPCAction.START_WORK, NPCAction.CONTINUE_WORK, NPCAction.CRAFT,
        NPCAction.FARM, NPCAction.MINE, NPCAction.FISH -> "sa ma ocup de munca mea"
        NPCAction.GREET -> "sa salut"
        NPCAction.TALK -> "sa vorbesc cu cineva"
        NPCAction.LISTEN -> "sa ascult"
        NPCAction.SOCIALIZE -> "sa caut companie"
        NPCAction.WALK_RANDOM -> "sa ma plimb"
        NPCAction.WALK_TO_TARGET -> "sa merg spre destinatie"
        NPCAction.RUN_TO_TARGET -> "sa alerg spre destinatie"
        NPCAction.OBSERVE -> "sa observ imprejurimile"
        NPCAction.INVESTIGATE -> "sa cercetez imprejurimile"
        NPCAction.FLEE -> "sa ma indepartez de pericol"
        NPCAction.HIDE -> "sa caut adapost"
        NPCAction.DEFEND -> "sa ma apar"
        NPCAction.CALL_HELP -> "sa cer ajutor pentru siguranta"
        NPCAction.WARN -> "sa avertizez despre pericol"
        NPCAction.DO_NOTHING -> "sa astept"
        else -> "actiunea \"${action.displayName}\""
    }

    enum class Factor(val description: String) {
        NEEDS("nevoile mele de atunci"),
        CONTEXT("situatia din jur de atunci"),
        ROUTINE("programul meu de atunci"),
        TRAITS("felul meu de a fi"),
        EMOTIONS("starea mea emotionala de atunci"),
        MEMORY("contextul relatiei cu interlocutorul de atunci"),
        EXPERIENCE("experientele mele recente de ingrijire"),
        SOCIAL_HELP("ajutorul alimentar confirmat pentru interlocutorul de atunci"),
    }

    companion object {
        private const val MAX_AGE = 5 * 60 * 1_000L
    }
}
