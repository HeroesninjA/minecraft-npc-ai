package ro.ainpc.engine

fun resolveQuestCategory(template: ScenarioEngine.ScenarioTemplate?): QuestScenarioContract.Category {
    val contract = template?.questContract
    return contract?.category ?: QuestScenarioContract.Category.SIDE
}

fun questLogCategoryPriority(template: ScenarioEngine.ScenarioTemplate?): Int {
    if (template == null) return 3
    return when (resolveQuestCategory(template)) {
        QuestScenarioContract.Category.MAIN -> 0
        QuestScenarioContract.Category.SIDE -> 1
        QuestScenarioContract.Category.REPEATABLE -> 2
    }
}
