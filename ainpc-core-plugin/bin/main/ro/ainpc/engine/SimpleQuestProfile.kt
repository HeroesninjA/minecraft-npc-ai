package ro.ainpc.engine

import org.bukkit.Material

class SimpleQuestProfile(
    private val title: String,
    private val objectiveMaterial: Material,
    private val objectiveAmount: Int,
    private val rewardMaterial: Material,
    private val rewardAmount: Int,
    private val objectivePrompt: String,
    private val hint: String,
) {
    fun title(): String = title

    fun objectiveMaterial(): Material = objectiveMaterial

    fun objectiveAmount(): Int = objectiveAmount

    fun rewardMaterial(): Material = rewardMaterial

    fun rewardAmount(): Int = rewardAmount

    fun objectivePrompt(): String = objectivePrompt

    fun hint(): String = hint
}
