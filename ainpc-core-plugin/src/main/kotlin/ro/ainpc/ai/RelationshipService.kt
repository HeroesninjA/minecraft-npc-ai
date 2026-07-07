package ro.ainpc.ai

import ro.ainpc.AINPCPlugin
import java.sql.SQLException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

class RelationshipService(private val plugin: AINPCPlugin) {
    private val relationships: MutableMap<String, NPCRelationship> = ConcurrentHashMap()
    private val lastInteractionTimestamps: MutableMap<String, Long> = ConcurrentHashMap()

    companion object {
        private const val AFFECTION_DECAY_PER_TICK = 0.001
        private const val TRUST_DECAY_PER_TICK = 0.0005
        private const val FAMILIARITY_DECAY_PER_TICK = 0.0003
        private const val MAX_AFFECTION = 100.0
        private const val MAX_TRUST = 100.0
        private const val MAX_RESPECT = 100.0
        private const val MAX_FAMILIARITY = 100.0
        private const val SOCIAL_INTERACTION_AFFECTION_BOOST = 0.5
        private const val SOCIAL_INTERACTION_FAMILIARITY_BOOST = 1.0
        private const val SOCIAL_INTERACTION_TRUST_BOOST = 0.3
    }

    init {
        loadAllRelationships()
    }

    private fun relationshipKey(npcA: UUID, npcB: UUID): String {
        val first = minOf(npcA.toString(), npcB.toString())
        val second = maxOf(npcA.toString(), npcB.toString())
        return "$first:$second"
    }

    fun getRelationship(npcA: UUID, npcB: UUID): NPCRelationship {
        val key = relationshipKey(npcA, npcB)
        return relationships.getOrPut(key) { NPCRelationship() }
    }

    fun setAffection(npcA: UUID, npcB: UUID, value: Double) {
        val rel = getRelationship(npcA, npcB)
        rel.affection = value.coerceIn(0.0, MAX_AFFECTION)
        saveRelationship(npcA, npcB, rel)
    }

    fun setTrust(npcA: UUID, npcB: UUID, value: Double) {
        val rel = getRelationship(npcA, npcB)
        rel.trust = value.coerceIn(0.0, MAX_TRUST)
        saveRelationship(npcA, npcB, rel)
    }

    fun setRespect(npcA: UUID, npcB: UUID, value: Double) {
        val rel = getRelationship(npcA, npcB)
        rel.respect = value.coerceIn(0.0, MAX_RESPECT)
        saveRelationship(npcA, npcB, rel)
    }

    fun setFamiliarity(npcA: UUID, npcB: UUID, value: Double) {
        val rel = getRelationship(npcA, npcB)
        rel.familiarity = value.coerceIn(0.0, MAX_FAMILIARITY)
        saveRelationship(npcA, npcB, rel)
    }

    fun modifyAffection(npcA: UUID, npcB: UUID, delta: Double) {
        val rel = getRelationship(npcA, npcB)
        rel.affection = (rel.affection + delta).coerceIn(0.0, MAX_AFFECTION)
        saveRelationship(npcA, npcB, rel)
    }

    fun modifyTrust(npcA: UUID, npcB: UUID, delta: Double) {
        val rel = getRelationship(npcA, npcB)
        rel.trust = (rel.trust + delta).coerceIn(0.0, MAX_TRUST)
        saveRelationship(npcA, npcB, rel)
    }

    fun modifyRespect(npcA: UUID, npcB: UUID, delta: Double) {
        val rel = getRelationship(npcA, npcB)
        rel.respect = (rel.respect + delta).coerceIn(0.0, MAX_RESPECT)
        saveRelationship(npcA, npcB, rel)
    }

    fun setRelationshipType(npcA: UUID, npcB: UUID, type: String) {
        val rel = getRelationship(npcA, npcB)
        rel.relationshipType = type
        saveRelationship(npcA, npcB, rel)
    }

    fun recordInteraction(npcA: UUID, npcB: UUID) {
        val key = relationshipKey(npcA, npcB)
        val rel = getRelationship(npcA, npcB)
        rel.interactionCount++
        rel.affection = (rel.affection + SOCIAL_INTERACTION_AFFECTION_BOOST).coerceAtMost(MAX_AFFECTION)
        rel.familiarity = (rel.familiarity + SOCIAL_INTERACTION_FAMILIARITY_BOOST).coerceAtMost(MAX_FAMILIARITY)
        rel.trust = (rel.trust + SOCIAL_INTERACTION_TRUST_BOOST).coerceAtMost(MAX_TRUST)
        val newType = resolveRelationshipType(rel)
        rel.relationshipType = newType
        lastInteractionTimestamps[key] = System.currentTimeMillis()
        saveRelationship(npcA, npcB, rel)
        plugin.platform.addonRegistry.dispatchRelationshipChange(
            npcA.toString(), npcB.toString(), newType)
    }

    fun applyDecay() {
        val now = System.currentTimeMillis()
        for ((key, rel) in relationships) {
            val lastInteraction = lastInteractionTimestamps[key] ?: 0L
            val hoursSinceInteraction = (now - lastInteraction) / 3600000.0
            if (hoursSinceInteraction < 1.0) continue
            val decayFactor = minOf(hoursSinceInteraction * 0.01, 1.0)
            rel.affection = (rel.affection - AFFECTION_DECAY_PER_TICK * decayFactor).coerceAtLeast(0.0)
            rel.trust = (rel.trust - TRUST_DECAY_PER_TICK * decayFactor).coerceAtLeast(0.0)
            rel.familiarity = (rel.familiarity - FAMILIARITY_DECAY_PER_TICK * decayFactor).coerceAtLeast(0.0)
            val parts = key.split(":")
            if (parts.size == 2) {
                saveRelationship(UUID.fromString(parts[0]), UUID.fromString(parts[1]), rel)
            }
        }
    }

    fun getNPCInteractions(npcUuid: UUID): List<Pair<UUID, NPCRelationship>> {
        val result = mutableListOf<Pair<UUID, NPCRelationship>>()
        val npcStr = npcUuid.toString()
        for ((key, rel) in relationships) {
            val parts = key.split(":")
            if (parts.size == 2) {
                val partner = when {
                    parts[0] == npcStr -> parts[1]
                    parts[1] == npcStr -> parts[0]
                    else -> null
                }
                if (partner != null) {
                    result.add(UUID.fromString(partner) to rel)
                }
            }
        }
        return result.sortedByDescending { it.second.affection }
    }

    fun getRelationshipCount(): Int = relationships.size

    fun flushAll() {
        for ((key, rel) in relationships) {
            val parts = key.split(":")
            if (parts.size == 2) {
                try {
                    saveRelationship(
                        UUID.fromString(parts[0]),
                        UUID.fromString(parts[1]),
                        rel
                    )
                } catch (e: Exception) {
                    plugin.logger.log(Level.WARNING, "Eroare la salvarea relatiei $key", e)
                }
            }
        }
    }

    private fun resolveRelationshipType(rel: NPCRelationship): String {
        val avgAffection = rel.affection
        val avgTrust = rel.trust
        return when {
            avgAffection >= 80.0 && avgTrust >= 70.0 -> "close_friend"
            avgAffection >= 60.0 && avgTrust >= 50.0 -> "friend"
            avgAffection >= 40.0 && avgTrust >= 30.0 -> "acquaintance"
            avgAffection >= 20.0 -> "neutral"
            avgAffection < 20.0 && avgTrust < 20.0 -> "rival"
            else -> "stranger"
        }
    }

    private fun saveRelationship(npcA: UUID, npcB: UUID, rel: NPCRelationship) {
        try {
            val sql = """
                INSERT OR REPLACE INTO npc_npc_relationships
                (npc_a_uuid, npc_b_uuid, affection, trust, respect, familiarity, interaction_count, relationship_type, last_interaction)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.setString(1, npcA.toString())
                stmt.setString(2, npcB.toString())
                stmt.setDouble(3, rel.affection)
                stmt.setDouble(4, rel.trust)
                stmt.setDouble(5, rel.respect)
                stmt.setDouble(6, rel.familiarity)
                stmt.setInt(7, rel.interactionCount)
                stmt.setString(8, rel.relationshipType ?: "stranger")
                stmt.setLong(9, System.currentTimeMillis())
                stmt.executeUpdate()
            }
        } catch (e: SQLException) {
            plugin.logger.log(Level.WARNING, "Eroare la salvarea relatiei NPC-NPC", e)
        }
    }

    private fun loadAllRelationships() {
        try {
            val sql = "SELECT * FROM npc_npc_relationships"
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        val npcA = UUID.fromString(rs.getString("npc_a_uuid"))
                        val npcB = UUID.fromString(rs.getString("npc_b_uuid"))
                        val key = relationshipKey(npcA, npcB)
                        val rel = NPCRelationship(
                            affection = rs.getDouble("affection"),
                            trust = rs.getDouble("trust"),
                            respect = rs.getDouble("respect"),
                            familiarity = rs.getDouble("familiarity"),
                            interactionCount = rs.getInt("interaction_count"),
                            relationshipType = rs.getString("relationship_type")
                        )
                        relationships[key] = rel
                        lastInteractionTimestamps[key] = rs.getLong("last_interaction")
                    }
                }
            }
            plugin.debug("Relatii NPC-NPC incarcate: ${relationships.size}")
        } catch (e: SQLException) {
            if (e.message?.contains("no such table") == true) {
                plugin.logger.info("Tabela npc_npc_relationships nu exista inca — se va crea la prima scriere.")
            } else {
                plugin.logger.log(Level.WARNING, "Eroare la incarcarea relatiilor NPC-NPC", e)
            }
        }
    }
}
