package ro.ainpc.engine

class QuestSeed(
    regionId: String?,
    placeId: String?,
    mechanicId: String?,
    kind: String?,
    theme: String?,
    allowedObjectiveTypes: List<String>?,
    allowedRewardTypes: List<String>?,
    storyMode: String?,
    storySignals: List<String>?,
    limits: List<String>?
) {
    private val regionIdValue: String = clean(regionId)
    private val placeIdValue: String = clean(placeId)
    private val mechanicIdValue: String = clean(mechanicId)
    private val kindValue: String = clean(kind)
    private val themeValue: String = clean(theme)
    private val allowedObjectiveTypesValue: List<String> = cleanList(allowedObjectiveTypes)
    private val allowedRewardTypesValue: List<String> = cleanList(allowedRewardTypes)
    private val storyModeValue: String = clean(storyMode)
    private val storySignalsValue: List<String> = cleanList(storySignals)
    private val limitsValue: List<String> = cleanList(limits)

    fun regionId(): String = regionIdValue
    fun placeId(): String = placeIdValue
    fun mechanicId(): String = mechanicIdValue
    fun kind(): String = kindValue
    fun theme(): String = themeValue
    fun allowedObjectiveTypes(): List<String> = allowedObjectiveTypesValue
    fun allowedRewardTypes(): List<String> = allowedRewardTypesValue
    fun storyMode(): String = storyModeValue
    fun storySignals(): List<String> = storySignalsValue
    fun limits(): List<String> = limitsValue

    companion object {
        internal fun clean(value: String?): String = value?.trim().orEmpty()

        internal fun cleanList(values: List<String>?): List<String> {
            if (values.isNullOrEmpty()) {
                return emptyList()
            }
            return values
                .map { clean(it) }
                .filter { it.isNotBlank() }
                .distinct()
        }
    }
}
