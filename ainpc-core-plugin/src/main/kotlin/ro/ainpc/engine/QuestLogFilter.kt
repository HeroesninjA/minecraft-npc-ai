package ro.ainpc.engine

enum class QuestLogFilter(
    private val displayName: String,
) {
    SUMMARY("sumar"),
    ALL("toate"),
    CURRENT("curente"),
    ACTIVE("active"),
    OFFERED("oferite"),
    TRACKED("urmarit"),
    QUEST_KIND("questuri"),
    CONTRACT_KIND("contracte"),
    DUTY_KIND("sarcini"),
    BOUNTY_KIND("bounty-uri"),
    EVENT_KIND("evenimente"),
    TUTORIAL_KIND("tutoriale"),
    RITUAL_KIND("ritualuri"),
    CONTRACT_CURRENT("contracte curente"),
    CONTRACT_ACTIVE("contracte active"),
    CONTRACT_OFFERED("contracte oferite"),
    CONTRACT_TRACKED("contracte urmarite"),
    CONTRACT_COMPLETED("contracte completate"),
    CONTRACT_FAILED("contracte esuate"),
    CONTRACT_ARCHIVED("contracte arhivate"),
    DUTY_CURRENT("sarcini curente"),
    DUTY_ACTIVE("sarcini active"),
    DUTY_OFFERED("sarcini oferite"),
    DUTY_TRACKED("sarcini urmarite"),
    DUTY_COMPLETED("sarcini completate"),
    DUTY_FAILED("sarcini esuate"),
    DUTY_ARCHIVED("sarcini arhivate"),
    BOUNTY_CURRENT("bounty-uri curente"),
    BOUNTY_ACTIVE("bounty-uri active"),
    BOUNTY_OFFERED("bounty-uri oferite"),
    BOUNTY_TRACKED("bounty-uri urmarite"),
    BOUNTY_COMPLETED("bounty-uri completate"),
    BOUNTY_FAILED("bounty-uri esuate"),
    BOUNTY_ARCHIVED("bounty-uri arhivate"),
    EVENT_CURRENT("evenimente curente"),
    EVENT_ACTIVE("evenimente active"),
    EVENT_OFFERED("evenimente oferite"),
    EVENT_TRACKED("evenimente urmarite"),
    EVENT_COMPLETED("evenimente completate"),
    EVENT_FAILED("evenimente esuate"),
    EVENT_ARCHIVED("evenimente arhivate"),
    TUTORIAL_CURRENT("tutoriale curente"),
    TUTORIAL_ACTIVE("tutoriale active"),
    TUTORIAL_OFFERED("tutoriale oferite"),
    TUTORIAL_TRACKED("tutoriale urmarite"),
    TUTORIAL_COMPLETED("tutoriale completate"),
    TUTORIAL_FAILED("tutoriale esuate"),
    TUTORIAL_ARCHIVED("tutoriale arhivate"),
    RITUAL_CURRENT("ritualuri curente"),
    RITUAL_ACTIVE("ritualuri active"),
    RITUAL_OFFERED("ritualuri oferite"),
    RITUAL_TRACKED("ritualuri urmarite"),
    RITUAL_COMPLETED("ritualuri completate"),
    RITUAL_FAILED("ritualuri esuate"),
    RITUAL_ARCHIVED("ritualuri arhivate"),
    MAIN("principal"),
    SIDE("secundar"),
    REPEATABLE("repetabil"),
    COMPLETED("completate"),
    FAILED("esuate"),
    ARCHIVED("arhivate");

    fun displayName(): String = displayName

    fun showsCurrent(): Boolean = when (this) {
        SUMMARY,
        ALL,
        CURRENT,
        ACTIVE,
        OFFERED,
        TRACKED,
        QUEST_KIND,
        CONTRACT_KIND,
        DUTY_KIND,
        BOUNTY_KIND,
        EVENT_KIND,
        TUTORIAL_KIND,
        RITUAL_KIND,
        CONTRACT_CURRENT,
        CONTRACT_ACTIVE,
        CONTRACT_OFFERED,
        CONTRACT_TRACKED,
        DUTY_CURRENT,
        DUTY_ACTIVE,
        DUTY_OFFERED,
        DUTY_TRACKED,
        BOUNTY_CURRENT,
        BOUNTY_ACTIVE,
        BOUNTY_OFFERED,
        BOUNTY_TRACKED,
        EVENT_CURRENT,
        EVENT_ACTIVE,
        EVENT_OFFERED,
        EVENT_TRACKED,
        TUTORIAL_CURRENT,
        TUTORIAL_ACTIVE,
        TUTORIAL_OFFERED,
        TUTORIAL_TRACKED,
        RITUAL_CURRENT,
        RITUAL_ACTIVE,
        RITUAL_OFFERED,
        RITUAL_TRACKED,
        MAIN,
        SIDE,
        REPEATABLE,
        -> true

        COMPLETED,
        FAILED,
        ARCHIVED,
        CONTRACT_COMPLETED,
        CONTRACT_FAILED,
        CONTRACT_ARCHIVED,
        DUTY_COMPLETED,
        DUTY_FAILED,
        DUTY_ARCHIVED,
        BOUNTY_COMPLETED,
        BOUNTY_FAILED,
        BOUNTY_ARCHIVED,
        EVENT_COMPLETED,
        EVENT_FAILED,
        EVENT_ARCHIVED,
        TUTORIAL_COMPLETED,
        TUTORIAL_FAILED,
        TUTORIAL_ARCHIVED,
        RITUAL_COMPLETED,
        RITUAL_FAILED,
        RITUAL_ARCHIVED,
        -> false
    }

    fun showsArchived(): Boolean = when (this) {
        SUMMARY,
        ALL,
        COMPLETED,
        FAILED,
        ARCHIVED,
        QUEST_KIND,
        CONTRACT_KIND,
        DUTY_KIND,
        BOUNTY_KIND,
        EVENT_KIND,
        TUTORIAL_KIND,
        RITUAL_KIND,
        CONTRACT_COMPLETED,
        CONTRACT_FAILED,
        CONTRACT_ARCHIVED,
        DUTY_COMPLETED,
        DUTY_FAILED,
        DUTY_ARCHIVED,
        BOUNTY_COMPLETED,
        BOUNTY_FAILED,
        BOUNTY_ARCHIVED,
        EVENT_COMPLETED,
        EVENT_FAILED,
        EVENT_ARCHIVED,
        TUTORIAL_COMPLETED,
        TUTORIAL_FAILED,
        TUTORIAL_ARCHIVED,
        RITUAL_COMPLETED,
        RITUAL_FAILED,
        RITUAL_ARCHIVED,
        MAIN,
        SIDE,
        REPEATABLE,
        -> true

        CURRENT,
        ACTIVE,
        OFFERED,
        TRACKED,
        CONTRACT_CURRENT,
        CONTRACT_ACTIVE,
        CONTRACT_OFFERED,
        CONTRACT_TRACKED,
        DUTY_CURRENT,
        DUTY_ACTIVE,
        DUTY_OFFERED,
        DUTY_TRACKED,
        BOUNTY_CURRENT,
        BOUNTY_ACTIVE,
        BOUNTY_OFFERED,
        BOUNTY_TRACKED,
        EVENT_CURRENT,
        EVENT_ACTIVE,
        EVENT_OFFERED,
        EVENT_TRACKED,
        TUTORIAL_CURRENT,
        TUTORIAL_ACTIVE,
        TUTORIAL_OFFERED,
        TUTORIAL_TRACKED,
        RITUAL_CURRENT,
        RITUAL_ACTIVE,
        RITUAL_OFFERED,
        RITUAL_TRACKED,
        -> false
    }
}

fun parseQuestLogFilter(filter: String?): QuestLogFilter {
    val normalized = normalizeReference(filter)
    return when (normalized) {
        "all", "toate" -> QuestLogFilter.ALL
        "current", "curent", "curente" -> QuestLogFilter.CURRENT
        "active", "activ" -> QuestLogFilter.ACTIVE
        "offered", "oferit", "oferite" -> QuestLogFilter.OFFERED
        "tracked", "urmarit" -> QuestLogFilter.TRACKED
        "quest", "questuri" -> QuestLogFilter.QUEST_KIND
        "contract", "contracts", "contracte" -> QuestLogFilter.CONTRACT_KIND
        "duty", "duties", "sarcina", "sarcini" -> QuestLogFilter.DUTY_KIND
        "bounty", "bounties", "recompensa", "recompense" -> QuestLogFilter.BOUNTY_KIND
        "event", "events", "eveniment", "evenimente" -> QuestLogFilter.EVENT_KIND
        "tutorial", "tutorials", "onboarding", "indrumare" -> QuestLogFilter.TUTORIAL_KIND
        "ritual", "rituals", "ceremony", "ceremonies", "ceremonie", "ceremonii" -> QuestLogFilter.RITUAL_KIND
        "contract_current", "contract_curent", "contracte_curente" -> QuestLogFilter.CONTRACT_CURRENT
        "contract_active", "contract_activ", "contracte_active" -> QuestLogFilter.CONTRACT_ACTIVE
        "contract_offered", "contract_oferit", "contracte_oferite" -> QuestLogFilter.CONTRACT_OFFERED
        "contract_tracked", "contract_urmarit", "contracte_urmarite" -> QuestLogFilter.CONTRACT_TRACKED
        "contract_completed", "contract_completat", "contracte_completate" -> QuestLogFilter.CONTRACT_COMPLETED
        "contract_failed", "contract_esuat", "contracte_esuate" -> QuestLogFilter.CONTRACT_FAILED
        "contract_archived", "contract_arhivat", "contracte_arhivate" -> QuestLogFilter.CONTRACT_ARCHIVED
        "duty_current", "duty_curent", "sarcini_curente" -> QuestLogFilter.DUTY_CURRENT
        "duty_active", "duty_activ", "sarcini_active" -> QuestLogFilter.DUTY_ACTIVE
        "duty_offered", "duty_oferit", "sarcini_oferite" -> QuestLogFilter.DUTY_OFFERED
        "duty_tracked", "duty_urmarit", "sarcini_urmarite" -> QuestLogFilter.DUTY_TRACKED
        "duty_completed", "duty_completat", "sarcini_completate" -> QuestLogFilter.DUTY_COMPLETED
        "duty_failed", "duty_esuat", "sarcini_esuate" -> QuestLogFilter.DUTY_FAILED
        "duty_archived", "duty_arhivat", "sarcini_arhivate" -> QuestLogFilter.DUTY_ARCHIVED
        "bounty_current", "bounty_curent", "recompense_curente" -> QuestLogFilter.BOUNTY_CURRENT
        "bounty_active", "bounty_activ", "recompense_active" -> QuestLogFilter.BOUNTY_ACTIVE
        "bounty_offered", "bounty_oferit", "recompense_oferite" -> QuestLogFilter.BOUNTY_OFFERED
        "bounty_tracked", "bounty_urmarit", "recompense_urmarite" -> QuestLogFilter.BOUNTY_TRACKED
        "bounty_completed", "bounty_completat", "recompense_completate" -> QuestLogFilter.BOUNTY_COMPLETED
        "bounty_failed", "bounty_esuat", "recompense_esuate" -> QuestLogFilter.BOUNTY_FAILED
        "bounty_archived", "bounty_arhivat", "recompense_arhivate" -> QuestLogFilter.BOUNTY_ARCHIVED
        "event_current", "event_curent", "evenimente_curente" -> QuestLogFilter.EVENT_CURRENT
        "event_active", "event_activ", "evenimente_active" -> QuestLogFilter.EVENT_ACTIVE
        "event_offered", "event_oferit", "evenimente_oferite" -> QuestLogFilter.EVENT_OFFERED
        "event_tracked", "event_urmarit", "evenimente_urmarite" -> QuestLogFilter.EVENT_TRACKED
        "event_completed", "event_completat", "evenimente_completate" -> QuestLogFilter.EVENT_COMPLETED
        "event_failed", "event_esuat", "evenimente_esuate" -> QuestLogFilter.EVENT_FAILED
        "event_archived", "event_arhivat", "evenimente_arhivate" -> QuestLogFilter.EVENT_ARCHIVED
        "tutorial_current", "tutorial_curent", "tutoriale_curente" -> QuestLogFilter.TUTORIAL_CURRENT
        "tutorial_active", "tutorial_activ", "tutoriale_active" -> QuestLogFilter.TUTORIAL_ACTIVE
        "tutorial_offered", "tutorial_oferit", "tutoriale_oferite" -> QuestLogFilter.TUTORIAL_OFFERED
        "tutorial_tracked", "tutorial_urmarit", "tutoriale_urmarite" -> QuestLogFilter.TUTORIAL_TRACKED
        "tutorial_completed", "tutorial_completat", "tutoriale_completate" -> QuestLogFilter.TUTORIAL_COMPLETED
        "tutorial_failed", "tutorial_esuat", "tutoriale_esuate" -> QuestLogFilter.TUTORIAL_FAILED
        "tutorial_archived", "tutorial_arhivat", "tutoriale_arhivate" -> QuestLogFilter.TUTORIAL_ARCHIVED
        "ritual_current", "ritual_curent", "ritualuri_curente" -> QuestLogFilter.RITUAL_CURRENT
        "ritual_active", "ritual_activ", "ritualuri_active" -> QuestLogFilter.RITUAL_ACTIVE
        "ritual_offered", "ritual_oferit", "ritualuri_oferite" -> QuestLogFilter.RITUAL_OFFERED
        "ritual_tracked", "ritual_urmarit", "ritualuri_urmarite" -> QuestLogFilter.RITUAL_TRACKED
        "ritual_completed", "ritual_completat", "ritualuri_completate" -> QuestLogFilter.RITUAL_COMPLETED
        "ritual_failed", "ritual_esuat", "ritualuri_esuate" -> QuestLogFilter.RITUAL_FAILED
        "ritual_archived", "ritual_arhivat", "ritualuri_arhivate" -> QuestLogFilter.RITUAL_ARCHIVED
        "main", "principal" -> QuestLogFilter.MAIN
        "side", "secundar", "secundare" -> QuestLogFilter.SIDE
        "repeatable", "repetabil", "repetabile" -> QuestLogFilter.REPEATABLE
        "completed", "complete", "completat", "finalizat", "finalizate" -> QuestLogFilter.COMPLETED
        "failed", "esuat", "abandonat", "abandonate" -> QuestLogFilter.FAILED
        "archived", "archive", "arhivat", "arhivate" -> QuestLogFilter.ARCHIVED
        else -> QuestLogFilter.SUMMARY
    }
}
