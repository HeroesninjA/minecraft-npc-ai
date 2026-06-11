package ro.ainpc.engine

import ro.ainpc.engine.ScenarioEngine.ScenarioTemplate

fun shouldAutoAcceptOnOffer(template: ScenarioTemplate?): Boolean =
    template?.questContract?.autoAcceptOnOffer() == true

fun resolveInitialQuestDialogueContext(template: ScenarioTemplate?): QuestDialogueContext =
    if (shouldAutoAcceptOnOffer(template)) QuestDialogueContext.ACCEPTED else QuestDialogueContext.OFFER

fun buildInitialQuestNpcFallbackMessages(template: ScenarioTemplate?): List<String> =
    if (shouldAutoAcceptOnOffer(template)) {
        listOf("Bine. Ma bazez pe tine.", "Intoarce-te cand ai terminat.")
    } else {
        listOf("Am o treaba pentru tine.", buildQuestOfferMessage(template))
    }

fun buildQuestOfferMessage(template: ScenarioTemplate?): String {
    val objectives = template?.objectives?.map { formatQuestEntry(it) } ?: emptyList()
    if (objectives.isEmpty()) {
        return if (template?.description.isNullOrBlank()) "Am nevoie de ajutorul tau."
        else template?.description ?: "Am nevoie de ajutorul tau."
    }
    val deliveryQuest = template?.objectives?.all { usesInventoryProgress(it) } == true
    if (deliveryQuest) return "Adu-mi " + joinNaturally(objectives) + " si te rasplatesc cum se cuvine."
    if (!template?.description.isNullOrBlank()) return template?.description ?: ""
    return "Ai de facut urmatoarele: " + joinNaturally(objectives) + "."
}
