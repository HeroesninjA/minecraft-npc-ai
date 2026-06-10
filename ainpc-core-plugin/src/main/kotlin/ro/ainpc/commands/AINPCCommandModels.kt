package ro.ainpc.commands

import org.bukkit.entity.Player
import ro.ainpc.npc.AINPC
import ro.ainpc.spawn.HouseholdPersistenceService

class StoryContextTarget(
    private val playerValue: Player,
    private val npcSelectorValue: String
) {
    fun player(): Player = playerValue
    fun npcSelector(): String = npcSelectorValue
}

class StoryEventTarget(
    private val regionIdValue: String,
    private val placeIdValue: String,
    private val labelValue: String,
    private val mappedValue: Boolean
) {
    fun regionId(): String = regionIdValue
    fun placeId(): String = placeIdValue
    fun label(): String = labelValue
    fun mapped(): Boolean = mappedValue
}

class QuestDecisionTarget(
    private val playerValue: Player,
    private val npcValue: AINPC
) {
    fun player(): Player = playerValue
    fun npc(): AINPC = npcValue
}

class QuestTrackRequest(
    private val playerValue: Player,
    private val questSelectorValue: String,
    private val actionValue: String
) {
    fun player(): Player = playerValue
    fun questSelector(): String = questSelectorValue
    fun action(): String = actionValue
}

class WorldCommandLocation(
    private val worldNameValue: String,
    private val xValue: Int,
    private val yValue: Int,
    private val zValue: Int,
    private val minHeightValue: Int,
    private val maxHeightValue: Int,
    private val consoleFallbackValue: Boolean
) {
    fun worldName(): String = worldNameValue
    fun x(): Int = xValue
    fun y(): Int = yValue
    fun z(): Int = zValue
    fun minHeight(): Int = minHeightValue
    fun maxHeight(): Int = maxHeightValue
    fun consoleFallback(): Boolean = consoleFallbackValue
}

class QuestLogRequest(
    private val playerValue: Player,
    private val filterValue: String
) {
    fun player(): Player = playerValue
    fun filter(): String = filterValue
}

class HouseholdMetadataBackfillInputs(
    private val inputsValue: List<HouseholdPersistenceService.MetadataResidentBackfillInput>,
    private val warningsValue: List<String>
) {
    fun inputs(): List<HouseholdPersistenceService.MetadataResidentBackfillInput> = inputsValue
    fun warnings(): List<String> = warningsValue
}

class ProgressionAliasConfig(
    private val commandValue: String,
    private val kindValue: String,
    private val displayLabelValue: String,
    private val shortSelectorExampleValue: String,
    private val mechanicExampleValue: String,
    private val baseTypeExampleValue: String
) {
    fun command(): String = commandValue
    fun kind(): String = kindValue
    fun displayLabel(): String = displayLabelValue
    fun shortSelectorExample(): String = shortSelectorExampleValue
    fun mechanicExample(): String = mechanicExampleValue
    fun baseTypeExample(): String = baseTypeExampleValue
}
