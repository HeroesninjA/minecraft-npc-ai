package ro.ainpc.ai

import java.util.UUID

data class PromptSnapshot(
    val npcUuid: UUID,
    val npcName: String,
    val npcDescription: String,
    val environmentDescription: String,
    val topologyConsensusBlock: String,
    val familyMembers: List<FamilyMemberSnapshot>,
    val profileCreated: Boolean,
    val profileSource: String,
    val profileVersion: Int,
    val profileSummary: String,
    val profileDataJson: String,
    val traitIds: List<String>,
    val playerName: String,
    val playerMessage: String,
    val occupation: String,
    val emotionShortDescription: String,
    val dominantEmotion: String,
    val currentState: String,
    val currentActivity: String,
    val locationDescription: String,
    val directAddress: Boolean,
    val explicitConversation: Boolean,
    val triggerReason: String,
    val nearbyNpcCount: Int,
    val distanceToNpc: Double
) {
    fun npcUuid(): UUID = npcUuid
    fun npcName(): String = npcName
    fun npcDescription(): String = npcDescription
    fun environmentDescription(): String = environmentDescription
    fun topologyConsensusBlock(): String = topologyConsensusBlock
    fun familyMembers(): List<FamilyMemberSnapshot> = familyMembers
    fun profileCreated(): Boolean = profileCreated
    fun profileSource(): String = profileSource
    fun profileVersion(): Int = profileVersion
    fun profileSummary(): String = profileSummary
    fun profileDataJson(): String = profileDataJson
    fun traitIds(): List<String> = traitIds
    fun playerName(): String = playerName
    fun playerMessage(): String = playerMessage
    fun occupation(): String = occupation
    fun emotionShortDescription(): String = emotionShortDescription
    fun dominantEmotion(): String = dominantEmotion
    fun currentState(): String = currentState
    fun currentActivity(): String = currentActivity
    fun locationDescription(): String = locationDescription
    fun directAddress(): Boolean = directAddress
    fun explicitConversation(): Boolean = explicitConversation
    fun triggerReason(): String = triggerReason
    fun nearbyNpcCount(): Int = nearbyNpcCount
    fun distanceToNpc(): Double = distanceToNpc
}
