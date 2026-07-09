package ro.ainpc.api

import java.util.UUID

interface RelationshipApi {
    fun getAffection(npcA: UUID, npcB: UUID): Double
    fun getTrust(npcA: UUID, npcB: UUID): Double
    fun getRelationshipType(npcA: UUID, npcB: UUID): String
    fun getInteractionCount(npcA: UUID, npcB: UUID): Int
    fun getTopRelationships(npcUuid: UUID, limit: Int): List<RelationshipEntry>
}

data class RelationshipEntry(
    val partnerUuid: UUID,
    val partnerName: String,
    val affection: Double,
    val relationshipType: String,
)
