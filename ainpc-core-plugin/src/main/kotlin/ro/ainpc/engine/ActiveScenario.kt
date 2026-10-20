package ro.ainpc.engine

import ro.ainpc.npc.NpcScenarioActorDefinition
import org.bukkit.Location
import java.util.UUID

/**
 * Activescenario.
 */
class ActiveScenario(val id: UUID, template: ScenarioTemplate, val startTime: Long = System.currentTimeMillis()) {
    val recoveryFingerprint: String = ScenarioTemplateRecoveryFingerprint.compute(template)
    val type: ScenarioType = template.type
    val templateId: String = template.templateId
    val displayName: String = template.displayName
    val phases: List<String> = template.phases.toList()
    val questStages: List<FeaturePackLoader.QuestStageDefinition> = template.questStages.toList()
    val hint: String = template.hint
    val questCode: String = template.questCode
    val questGiverProfession: String = template.questGiverProfession
    val actors: MutableMap<String, NpcScenarioActorDefinition> = LinkedHashMap(template.actors)
    val questActorTriggers: MutableMap<String, MutableSet<String>> = LinkedHashMap(
        template.questActorTriggers.mapValues { (_, actorIds) -> LinkedHashSet(actorIds) }
    )
    val validationWarnings: MutableList<String> = ArrayList(template.validationWarnings)
    val validationWarningDetails: List<FeaturePackLoader.ValidationWarning> = ArrayList(template.validationWarningDetails)
    val objectives: List<FeaturePackLoader.QuestEntryDefinition> = ArrayList(template.objectives)
    val rewards: List<FeaturePackLoader.QuestEntryDefinition> = ArrayList(template.rewards)
    val npcRoles: MutableMap<UUID, String> = HashMap()
    val playerRoles: MutableMap<UUID, String> = HashMap()
    val spawnedActors: MutableMap<String, UUID> = LinkedHashMap()
    val actorExpirationTimes: MutableMap<String, Long> = LinkedHashMap()
    val retiredActorIds: MutableSet<String> = LinkedHashSet()
    var anchorLocation: Location? = null
    var currentPhase: String = ""

    fun assignNPCRole(npcId: UUID, role: String) {
        npcRoles[npcId] = role
    }

    fun assignPlayerRole(playerId: UUID, role: String) {
        playerRoles[playerId] = role
    }

    fun hasNPCRole(npcId: UUID): Boolean = npcRoles.containsKey(npcId)

    fun hasQuestBriefing(): Boolean = questCode.isNotBlank() || objectives.isNotEmpty() || rewards.isNotEmpty()

    fun assignActor(actorId: String, npcId: UUID, expiresAt: Long? = null) {
        if (actorId.isNotBlank()) {
            retiredActorIds.remove(actorId)
            spawnedActors[actorId] = npcId
            if (expiresAt == null) actorExpirationTimes.remove(actorId)
            else actorExpirationTimes[actorId] = expiresAt
        }
    }

    fun removeActor(actorId: String): UUID? {
        actorExpirationTimes.remove(actorId)
        val npcId = spawnedActors.remove(actorId)
        if (npcId != null) retiredActorIds.add(actorId)
        return npcId
    }

    fun isCurrentActor(actorId: String, npcUuid: UUID): Boolean = spawnedActors[actorId] == npcUuid
}
