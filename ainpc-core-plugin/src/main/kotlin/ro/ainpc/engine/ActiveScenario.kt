package ro.ainpc.engine

import java.util.UUID

class ActiveScenario(val id: UUID, template: ScenarioTemplate) {
    val type: ScenarioType = template.type
    val templateId: String = template.templateId
    val displayName: String = template.displayName
    val hint: String = template.hint
    val questCode: String = template.questCode
    val questGiverProfession: String = template.questGiverProfession
    val objectives: List<FeaturePackLoader.QuestEntryDefinition> = ArrayList(template.objectives)
    val rewards: List<FeaturePackLoader.QuestEntryDefinition> = ArrayList(template.rewards)
    val npcRoles: MutableMap<UUID, String> = HashMap()
    val playerRoles: MutableMap<UUID, String> = HashMap()
    var currentPhase: String = ""
    val startTime: Long = System.currentTimeMillis()

    fun assignNPCRole(npcId: UUID, role: String) {
        npcRoles[npcId] = role
    }

    fun assignPlayerRole(playerId: UUID, role: String) {
        playerRoles[playerId] = role
    }

    fun hasNPCRole(npcId: UUID): Boolean = npcRoles.containsKey(npcId)

    fun hasQuestBriefing(): Boolean = questCode.isNotBlank() || objectives.isNotEmpty() || rewards.isNotEmpty()
}
