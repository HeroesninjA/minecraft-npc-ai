package ro.ainpc.engine

fun resolveQuestCategory(template: ScenarioTemplate?): QuestScenarioContract.Category {
    val contract = template?.questContract
    return contract?.category ?: QuestScenarioContract.Category.SIDE
}

fun questLogCategoryPriority(template: ScenarioTemplate?): Int {
    if (template == null) return 3
    return when (resolveQuestCategory(template)) {
        QuestScenarioContract.Category.MAIN -> 0
        QuestScenarioContract.Category.SIDE -> 1
        QuestScenarioContract.Category.REPEATABLE -> 2
    }
}
