package ro.ainpc.engine

class QuestDraftObjective(
    id: String?,
    type: String?,
    target: String?,
    description: String?,
    anchorReference: String?
) {
    private val idValue: String = QuestSeed.clean(id)
    private val typeValue: String = QuestSeed.clean(type)
    private val targetValue: String = QuestSeed.clean(target)
    private val descriptionValue: String = QuestSeed.clean(description)
    private val anchorReferenceValue: String = QuestSeed.clean(anchorReference)

    fun id(): String = idValue
    fun type(): String = typeValue
    fun target(): String = targetValue
    fun description(): String = descriptionValue
    fun anchorReference(): String = anchorReferenceValue
}

class QuestDraftReward(
    type: String?,
    value: String?,
    amount: Int,
    description: String?
) {
    private val typeValue: String = QuestSeed.clean(type)
    private val valueValue: String = QuestSeed.clean(value)
    private val amountValue: Int = amount
    private val descriptionValue: String = QuestSeed.clean(description)

    fun type(): String = typeValue
    fun value(): String = valueValue
    fun amount(): Int = amountValue
    fun description(): String = descriptionValue
}

class QuestDraftStoryAction(
    type: String?,
    scope: String?,
    target: String?,
    key: String?,
    value: String?
) {
    private val typeValue: String = QuestSeed.clean(type)
    private val scopeValue: String = QuestSeed.clean(scope)
    private val targetValue: String = QuestSeed.clean(target)
    private val keyValue: String = QuestSeed.clean(key)
    private val valueValue: String = QuestSeed.clean(value)

    fun type(): String = typeValue
    fun scope(): String = scopeValue
    fun target(): String = targetValue
    fun key(): String = keyValue
    fun value(): String = valueValue
}

class QuestDraft(
    draftId: String?,
    title: String?,
    description: String?,
    mechanicId: String?,
    kind: String?,
    objectives: List<QuestDraftObjective>?,
    rewards: List<QuestDraftReward>?,
    storyActions: List<QuestDraftStoryAction>?,
    anchorReferences: List<String>?,
    exportEnabled: Boolean
) {
    private val draftIdValue: String = QuestSeed.clean(draftId)
    private val titleValue: String = QuestSeed.clean(title)
    private val descriptionValue: String = QuestSeed.clean(description)
    private val mechanicIdValue: String = QuestSeed.clean(mechanicId)
    private val kindValue: String = QuestSeed.clean(kind)
    private val objectivesValue: List<QuestDraftObjective> = objectives?.filterNotNull().orEmpty()
    private val rewardsValue: List<QuestDraftReward> = rewards?.filterNotNull().orEmpty()
    private val storyActionsValue: List<QuestDraftStoryAction> = storyActions?.filterNotNull().orEmpty()
    private val anchorReferencesValue: List<String> = QuestSeed.cleanList(anchorReferences)
    private val exportEnabledValue: Boolean = exportEnabled

    fun draftId(): String = draftIdValue
    fun title(): String = titleValue
    fun description(): String = descriptionValue
    fun mechanicId(): String = mechanicIdValue
    fun kind(): String = kindValue
    fun objectives(): List<QuestDraftObjective> = objectivesValue
    fun rewards(): List<QuestDraftReward> = rewardsValue
    fun storyActions(): List<QuestDraftStoryAction> = storyActionsValue
    fun anchorReferences(): List<String> = anchorReferencesValue
    fun exportEnabled(): Boolean = exportEnabledValue
}
