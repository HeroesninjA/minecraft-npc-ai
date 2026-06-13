@file:JvmName("NPCManagerDB")

package ro.ainpc.managers

import org.bukkit.Location
import org.bukkit.entity.Villager
import ro.ainpc.AINPCPlugin
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCEmotions
import ro.ainpc.npc.NPCPersonality
import java.sql.PreparedStatement
import java.sql.SQLException
import java.util.Locale
import java.util.Random

lateinit var npcManagerDbPlugin: AINPCPlugin

fun initNpcManagerDbPlugin(plugin: AINPCPlugin) {
    npcManagerDbPlugin = plugin
}

fun savePersonality(npc: AINPC): Boolean {
    val sql = """
        INSERT OR REPLACE INTO npc_personality
        (npc_id, openness, conscientiousness, extraversion, agreeableness, neuroticism)
        VALUES (?, ?, ?, ?, ?, ?)
    """.trimIndent()

    try {
        npcManagerDbPlugin.databaseManager.prepareStatement(sql).use { stmt ->
            val p = npc.personality
            stmt.setInt(1, npc.databaseId)
            stmt.setDouble(2, p.openness)
            stmt.setDouble(3, p.conscientiousness)
            stmt.setDouble(4, p.extraversion)
            stmt.setDouble(5, p.agreeableness)
            stmt.setDouble(6, p.neuroticism)
            stmt.executeUpdate()
            return true
        }
    } catch (e: SQLException) {
        npcManagerDbPlugin.logger.severe("Eroare la salvarea personalitatii: ${e.message}")
        return false
    }
}

fun saveEmotionsRow(npc: AINPC): Boolean {
    val sql = """
        INSERT OR REPLACE INTO npc_emotions
        (npc_id, happiness, sadness, anger, fear, surprise, disgust, trust, anticipation, last_updated)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
    """.trimIndent()

    try {
        npcManagerDbPlugin.databaseManager.prepareStatement(sql).use { stmt ->
            val e = npc.emotions
            stmt.setInt(1, npc.databaseId)
            stmt.setDouble(2, e.happiness)
            stmt.setDouble(3, e.sadness)
            stmt.setDouble(4, e.anger)
            stmt.setDouble(5, e.fear)
            stmt.setDouble(6, e.surprise)
            stmt.setDouble(7, e.disgust)
            stmt.setDouble(8, e.trust)
            stmt.setDouble(9, e.anticipation)
            stmt.executeUpdate()
            return true
        }
    } catch (e: SQLException) {
        npcManagerDbPlugin.logger.severe("Eroare la salvarea emotiilor: ${e.message}")
        return false
    }
}

fun loadTraits(npc: AINPC) {
    val sql = """
        SELECT trait_id
        FROM npc_traits
        WHERE npc_id = ?
        ORDER BY trait_id ASC
    """.trimIndent()

    val traits = ArrayList<String>()
    npcManagerDbPlugin.databaseManager.prepareStatement(sql).use { stmt ->
        stmt.setInt(1, npc.databaseId)
        stmt.executeQuery().use { rs ->
            while (rs.next()) {
                traits.add(rs.getString("trait_id"))
            }
        }
    }

    npc.traits = traits
}

fun saveTraits(npc: AINPC): Boolean {
    val deleteSql = "DELETE FROM npc_traits WHERE npc_id = ?"

    try {
        npcManagerDbPlugin.databaseManager.prepareStatement(deleteSql).use { deleteStmt ->
            deleteStmt.setInt(1, npc.databaseId)
            deleteStmt.executeUpdate()
        }
    } catch (e: SQLException) {
        npcManagerDbPlugin.logger.severe("Eroare la stergerea traits pentru profilul NPC: ${e.message}")
        return false
    }

    if (npc.traits.isNullOrEmpty()) return true

    val insertSql = """
        INSERT OR IGNORE INTO npc_traits (npc_id, trait_id)
        VALUES (?, ?)
    """.trimIndent()

    try {
        npcManagerDbPlugin.databaseManager.prepareStatement(insertSql).use { insertStmt ->
            for (traitId in npc.traits) {
                if (traitId.isBlank()) continue
                insertStmt.setInt(1, npc.databaseId)
                insertStmt.setString(2, traitId)
                insertStmt.addBatch()
            }
            insertStmt.executeBatch()
            return true
        }
    } catch (e: SQLException) {
        npcManagerDbPlugin.logger.severe("Eroare la salvarea traits pentru profilul NPC: ${e.message}")
        return false
    }
}

fun deleteOtherPersistentSourceKeys(npcId: Int, normalizedSourceKey: String) {
    val sql = """
        DELETE FROM npc_source_keys
        WHERE npc_id = ?
          AND source_key <> ?
    """.trimIndent()

    npcManagerDbPlugin.databaseManager.prepareStatement(sql).use { stmt ->
        stmt.setInt(1, npcId)
        stmt.setString(2, normalizedSourceKey)
        stmt.executeUpdate()
    }
}

fun deletePersistentSourceKey(npcId: Int): Boolean {
    if (npcId <= 0) return true

    val sql = "DELETE FROM npc_source_keys WHERE npc_id = ?"
    try {
        npcManagerDbPlugin.databaseManager.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, npcId)
            stmt.executeUpdate()
            return true
        }
    } catch (e: SQLException) {
        npcManagerDbPlugin.logger.severe("Eroare la stergerea source_key persistent pentru NPC #$npcId: ${e.message}")
        return false
    }
}

fun insertSourceKeyOwner(normalizedSourceKey: String, npcId: Int, source: String?) {
    val sql = """
        INSERT INTO npc_source_keys (source_key, npc_id, source, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?)
    """.trimIndent()

    val now = System.currentTimeMillis()
    npcManagerDbPlugin.databaseManager.prepareStatement(sql).use { stmt ->
        stmt.setString(1, normalizedSourceKey)
        stmt.setInt(2, npcId)
        stmt.setString(3, source ?: "")
        stmt.setLong(4, now)
        stmt.setLong(5, now)
        stmt.executeUpdate()
    }
}

fun updateSourceKeyOwner(normalizedSourceKey: String, npcId: Int, source: String?) {
    val sql = """
        UPDATE npc_source_keys
        SET npc_id = ?, source = ?, updated_at = ?
        WHERE source_key = ?
    """.trimIndent()

    npcManagerDbPlugin.databaseManager.prepareStatement(sql).use { stmt ->
        stmt.setInt(1, npcId)
        stmt.setString(2, source ?: "")
        stmt.setLong(3, System.currentTimeMillis())
        stmt.setString(4, normalizedSourceKey)
        stmt.executeUpdate()
    }
}

fun findPersistedSourceKeyOwnerId(sourceKey: String?): Int? {
    val normalizedSourceKey = normalizeSourceKey(sourceKey)
    if (normalizedSourceKey.isBlank()) return null

    val sql = "SELECT npc_id FROM npc_source_keys WHERE source_key = ?"
    try {
        npcManagerDbPlugin.databaseManager.prepareStatement(sql).use { stmt ->
            stmt.setString(1, normalizedSourceKey)
            stmt.executeQuery().use { rs ->
                if (rs.next()) return rs.getInt("npc_id")
            }
        }
    } catch (_: SQLException) {
    }
    return null
}

fun findPersistedSourceKeyOwnerIdQuietly(sourceKey: String?): Int? {
    return try {
        findPersistedSourceKeyOwnerId(sourceKey)
    } catch (e: SQLException) {
        npcManagerDbPlugin.debug("Nu pot citi indexul source_key persistent: ${e.message}")
        null
    }
}

fun clearPersistentSourceKeys(): Int {
    val sql = "DELETE FROM npc_source_keys"
    return npcManagerDbPlugin.databaseManager.prepareStatement(sql).use { stmt ->
        stmt.executeUpdate()
    }
}

fun spawnNaturalVillageVillager(location: Location?): Villager? {
    if (location == null || location.world == null) return null
    return try {
        location.world.spawn(location, Villager::class.java) { villager ->
            villager.setAdult()
            villager.profession = Villager.Profession.NONE
            villager.villagerType = Villager.Type.PLAINS
            villager.isPersistent = true
            villager.setRemoveWhenFarAway(false)
        }
    } catch (exception: Exception) {
        npcManagerDbPlugin.logger.warning("Nu am putut genera un villager nou pentru sat la ${formatLocation(location)}: ${exception.message}")
        null
    }
}

fun applyThemeDefaults(npc: AINPC) {
    val profession = npcManagerDbPlugin.featurePackLoader.findPrimaryScenarioProfession(npc.occupation)
    if (profession == null || profession.suggestedTraits.isEmpty()) return

    if (!npc.traits.isNullOrEmpty()) return

    val candidates = ArrayList(profession.suggestedTraits)
    val random = Random(
        npc.uuid.mostSignificantBits
            xor npc.uuid.leastSignificantBits
            xor profession.id.hashCode().toLong()
    )
    candidates.shuffle(random)
    val traitsToAssign = minOf(2, candidates.size)
    for (i in 0 until traitsToAssign) {
        npc.addTrait(candidates[i])
    }
}

fun inferOccupationFromPrimaryScenario(random: Random): String? {
    val pack = npcManagerDbPlugin.featurePackLoader.getPrimaryScenarioPack() ?: return null
    if (pack.professions.isEmpty()) return null

    val shuffled = pack.professions.toMutableList()
    shuffled.shuffle(random)
    return shuffled[0].name.lowercase(Locale.ROOT)
}

fun resolveOccupationForVillager(villager: Villager, random: Random): String? {

    val mappedOccupation = mapProfessionToOccupation(villager.profession)
    val inferredOccupation = inferOccupationFromEnvironment(villager)
    val themedOccupation = if (isGenericOccupation(mappedOccupation) && (inferredOccupation == null || inferredOccupation.isBlank())) {
        inferOccupationFromPrimaryScenario(random)
    } else {
        null
    }
    return resolveOccupationChoice(villager.profession, mappedOccupation, inferredOccupation, themedOccupation)
}
