@file:Suppress("SENSELESS_COMPARISON")
package ro.ainpc.progression

import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import ro.ainpc.api.PlayerProgressionGrant
import ro.ainpc.api.PlayerProgressionSnapshot
import java.sql.SQLException
import java.util.logging.Level

class PlayerProgressionService(private val plugin: AINPCPlugin) {
    fun getSnapshot(player: Player): PlayerProgressionSnapshot {
        return loadSnapshot(player.uniqueId.toString())
    }

    fun getSnapshot(playerUuid: String): PlayerProgressionSnapshot {
        return loadSnapshot(playerUuid)
    }

    fun getTopPlayers(limit: Int): List<PlayerProgressionSnapshot> {
        val safeLimit = limit.coerceIn(1, 100)
        val databaseManager = plugin.databaseManager ?: return emptyList()
        val sql = "SELECT player_uuid, level, xp, total_xp, skills_json, last_updated " +
            "FROM player_progression ORDER BY level DESC, total_xp DESC LIMIT ?"
        return try {
            databaseManager.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, safeLimit)
                stmt.executeQuery().use { rs ->
                    val results = mutableListOf<PlayerProgressionSnapshot>()
                    while (rs.next()) {
                        val playerUuid = rs.getString("player_uuid") ?: continue
                        val level = rs.getInt("level").coerceAtLeast(1)
                        val totalXp = rs.getLong("total_xp").coerceAtLeast(0L)
                        val xp = rs.getLong("xp").coerceAtLeast(0L)
                        val skills = decodeSkills(rs.getString("skills_json"))
                        val lastUpdated = rs.getLong("last_updated")
                        results.add(
                            PlayerProgressionSnapshot(
                                playerUuid = playerUuid,
                                level = level,
                                xp = xp,
                                xpToNextLevel = xpRequiredForLevel(level),
                                totalXp = totalXp,
                                skills = skills,
                                lastUpdated = lastUpdated,
                            )
                        )
                    }
                    results
                }
            }
        } catch (e: SQLException) {
            plugin.logger.log(Level.WARNING, "Eroare la citirea top progresie jucatori", e)
            emptyList()
        }
    }

    fun grantXp(player: Player, amount: Long): PlayerProgressionGrant {
        return grantXp(player.uniqueId.toString(), amount)
    }

    fun grantXp(playerUuid: String, amount: Long): PlayerProgressionGrant {
        if (amount <= 0L) {
            return PlayerProgressionGrant(playerUuid, 0L, 0, emptyMap(), loadSnapshot(playerUuid))
        }
        return persistGrant(playerUuid, xpToAdd = amount, skillId = null, skillXp = 0)
    }

    fun addSkillXp(player: Player, skillId: String, amount: Int): PlayerProgressionGrant {
        return addSkillXp(player.uniqueId.toString(), skillId, amount)
    }

    fun addSkillXp(playerUuid: String, skillId: String, amount: Int): PlayerProgressionGrant {
        val normalizedSkill = skillId.trim().lowercase()
        if (normalizedSkill.isEmpty() || amount <= 0) {
            return PlayerProgressionGrant(playerUuid, 0L, 0, emptyMap(), loadSnapshot(playerUuid))
        }
        return persistGrant(playerUuid, xpToAdd = 0L, skillId = normalizedSkill, skillXp = amount)
    }

    fun setLevel(player: Player, level: Int): PlayerProgressionSnapshot {
        return setLevel(player.uniqueId.toString(), level)
    }

    fun setLevel(playerUuid: String, level: Int): PlayerProgressionSnapshot {
        val target = level.coerceAtLeast(1)
        val totalXp = cumulativeXpForLevel(target)
        return persistTotalXp(playerUuid, totalXp)
    }

    fun setSkillLevel(player: Player, skillId: String, level: Int): PlayerProgressionSnapshot {
        return setSkillLevel(player.uniqueId.toString(), skillId, level)
    }

    fun setSkillLevel(playerUuid: String, skillId: String, level: Int): PlayerProgressionSnapshot {
        val normalizedSkill = skillId.trim().lowercase()
        if (normalizedSkill.isEmpty()) {
            return loadSnapshot(playerUuid)
        }
        val target = level.coerceAtLeast(1)
        val targetSkillXp = cumulativeXpForLevel(target).toInt()
        val before = loadSnapshot(playerUuid)
        val newSkills = LinkedHashMap(before.skills)
        newSkills[normalizedSkill] = targetSkillXp
        val now = System.currentTimeMillis()
        try {
            val sql = "INSERT INTO player_progression " +
                "(player_uuid, level, xp, total_xp, skills_json, last_updated) " +
                "VALUES (?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT(player_uuid) DO UPDATE SET " +
                "level = excluded.level, " +
                "xp = excluded.xp, " +
                "total_xp = excluded.total_xp, " +
                "skills_json = excluded.skills_json, " +
                "last_updated = excluded.last_updated"
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.setString(1, playerUuid)
                stmt.setInt(2, before.level)
                stmt.setLong(3, before.xp)
                stmt.setLong(4, before.totalXp)
                stmt.setString(5, encodeSkills(newSkills))
                stmt.setLong(6, now)
                stmt.executeUpdate()
            }
        } catch (e: SQLException) {
            plugin.logger.log(Level.WARNING, "Eroare la setarea nivelului de skill pentru $playerUuid/$normalizedSkill", e)
        }
        return PlayerProgressionSnapshot(
            playerUuid = playerUuid,
            level = before.level,
            xp = before.xp,
            xpToNextLevel = before.xpToNextLevel,
            totalXp = before.totalXp,
            skills = newSkills,
            lastUpdated = now,
        )
    }

    fun getSkillLevel(playerUuid: String, skillId: String): Int {
        val normalizedSkill = skillId.trim().lowercase()
        if (normalizedSkill.isEmpty()) return 1
        val snapshot = loadSnapshot(playerUuid)
        val skillXp = snapshot.skills[normalizedSkill] ?: 0
        return levelForTotalXp(skillXp.toLong())
    }

    fun resetPlayer(player: Player): PlayerProgressionSnapshot {
        return resetPlayer(player.uniqueId.toString())
    }

    fun resetPlayer(playerUuid: String): PlayerProgressionSnapshot {
        try {
            val sql = "DELETE FROM player_progression WHERE player_uuid = ?"
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.setString(1, playerUuid)
                stmt.executeUpdate()
            }
        } catch (e: SQLException) {
            plugin.logger.log(Level.WARNING, "Eroare la resetarea progresiei pentru $playerUuid", e)
        }
        return PlayerProgressionSnapshot(playerUuid, 1, 0L, xpRequiredForLevel(1), 0L, emptyMap(), 0L)
    }

    fun xpRequiredForLevel(level: Int): Long {
        val safe = level.coerceAtLeast(1)
        return (100L * safe) + (50L * (safe - 1) * safe)
    }

    fun cumulativeXpForLevel(level: Int): Long {
        val safe = level.coerceAtLeast(1)
        var total = 0L
        for (lvl in 1 until safe) {
            total += xpRequiredForLevel(lvl)
        }
        return total
    }

    fun levelForTotalXp(totalXp: Long): Int {
        var level = 1
        var accumulated = 0L
        while (true) {
            val required = xpRequiredForLevel(level)
            if (accumulated + required > totalXp) return level
            accumulated += required
            level += 1
            if (level > 200) return 200
        }
    }

    private fun persistGrant(
        playerUuid: String,
        xpToAdd: Long,
        skillId: String?,
        skillXp: Int,
    ): PlayerProgressionGrant {
        val before = loadSnapshot(playerUuid)
        val newTotalXp = (before.totalXp + xpToAdd).coerceAtLeast(0L)
        val newLevel = levelForTotalXp(newTotalXp)
        val newSkills = LinkedHashMap(before.skills)
        var skillDelta = 0
        if (skillId != null && skillXp > 0) {
            val currentSkill = newSkills[skillId] ?: 0
            newSkills[skillId] = currentSkill + skillXp
            skillDelta = skillXp
        }
        val now = System.currentTimeMillis()
        try {
            val sql = "INSERT INTO player_progression " +
                "(player_uuid, level, xp, total_xp, skills_json, last_updated) " +
                "VALUES (?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT(player_uuid) DO UPDATE SET " +
                "level = excluded.level, " +
                "xp = excluded.xp, " +
                "total_xp = excluded.total_xp, " +
                "skills_json = excluded.skills_json, " +
                "last_updated = excluded.last_updated"
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.setString(1, playerUuid)
                stmt.setInt(2, newLevel)
                stmt.setLong(3, newTotalXp - cumulativeXpForLevel(newLevel))
                stmt.setLong(4, newTotalXp)
                stmt.setString(5, encodeSkills(newSkills))
                stmt.setLong(6, now)
                stmt.executeUpdate()
            }
        } catch (e: SQLException) {
            plugin.logger.log(Level.WARNING, "Eroare la acordarea progresiei pentru $playerUuid", e)
        }
        val after = PlayerProgressionSnapshot(
            playerUuid = playerUuid,
            level = newLevel,
            xp = newTotalXp - cumulativeXpForLevel(newLevel),
            xpToNextLevel = xpRequiredForLevel(newLevel),
            totalXp = newTotalXp,
            skills = newSkills,
            lastUpdated = now,
        )
        return PlayerProgressionGrant(
            playerUuid = playerUuid,
            xpGranted = xpToAdd,
            levelsGained = (newLevel - before.level).coerceAtLeast(0),
            skillsGained = if (skillDelta > 0 && skillId != null) mapOf(skillId to skillDelta) else emptyMap(),
            snapshot = after,
        )
    }

    private fun persistTotalXp(playerUuid: String, totalXp: Long): PlayerProgressionSnapshot {
        val level = levelForTotalXp(totalXp)
        val now = System.currentTimeMillis()
        try {
            val skills = loadSnapshot(playerUuid).skills
            val sql = "INSERT INTO player_progression " +
                "(player_uuid, level, xp, total_xp, skills_json, last_updated) " +
                "VALUES (?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT(player_uuid) DO UPDATE SET " +
                "level = excluded.level, " +
                "xp = excluded.xp, " +
                "total_xp = excluded.total_xp, " +
                "skills_json = excluded.skills_json, " +
                "last_updated = excluded.last_updated"
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.setString(1, playerUuid)
                stmt.setInt(2, level)
                stmt.setLong(3, totalXp - cumulativeXpForLevel(level))
                stmt.setLong(4, totalXp)
                stmt.setString(5, encodeSkills(skills))
                stmt.setLong(6, now)
                stmt.executeUpdate()
            }
        } catch (e: SQLException) {
            plugin.logger.log(Level.WARNING, "Eroare la setarea nivelului pentru $playerUuid", e)
        }
        return PlayerProgressionSnapshot(
            playerUuid = playerUuid,
            level = level,
            xp = totalXp - cumulativeXpForLevel(level),
            xpToNextLevel = xpRequiredForLevel(level),
            totalXp = totalXp,
            skills = loadSnapshot(playerUuid).skills,
            lastUpdated = now,
        )
    }

    private fun loadSnapshot(playerUuid: String): PlayerProgressionSnapshot {
        return try {
            val sql = "SELECT level, xp, total_xp, skills_json, last_updated " +
                "FROM player_progression WHERE player_uuid = ?"
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.setString(1, playerUuid)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        val level = rs.getInt("level").coerceAtLeast(1)
                        val totalXp = rs.getLong("total_xp").coerceAtLeast(0L)
                        val xp = rs.getLong("xp").coerceAtLeast(0L)
                        val skills = decodeSkills(rs.getString("skills_json"))
                        val lastUpdated = rs.getLong("last_updated")
                        PlayerProgressionSnapshot(
                            playerUuid = playerUuid,
                            level = level,
                            xp = xp,
                            xpToNextLevel = xpRequiredForLevel(level),
                            totalXp = totalXp,
                            skills = skills,
                            lastUpdated = lastUpdated,
                        )
                    } else {
                        PlayerProgressionSnapshot(
                            playerUuid = playerUuid,
                            level = 1,
                            xp = 0L,
                            xpToNextLevel = xpRequiredForLevel(1),
                            totalXp = 0L,
                            skills = emptyMap(),
                            lastUpdated = 0L,
                        )
                    }
                }
            }
        } catch (e: SQLException) {
            plugin.logger.log(Level.WARNING, "Eroare la citirea progresiei pentru $playerUuid", e)
            PlayerProgressionSnapshot(
                playerUuid = playerUuid,
                level = 1,
                xp = 0L,
                xpToNextLevel = xpRequiredForLevel(1),
                totalXp = 0L,
                skills = emptyMap(),
                lastUpdated = 0L,
            )
        }
    }

    private fun encodeSkills(skills: Map<String, Int>): String {
        if (skills.isEmpty()) return "{}"
        val sb = StringBuilder("{")
        var first = true
        for ((key, value) in skills) {
            if (!first) sb.append(",")
            sb.append("\"").append(escapeJson(key)).append("\":").append(value)
            first = false
        }
        sb.append("}")
        return sb.toString()
    }

    private fun decodeSkills(json: String?): Map<String, Int> {
        if (json.isNullOrBlank() || json == "{}") return emptyMap()
        val result = LinkedHashMap<String, Int>()
        val trimmed = json.trim().removePrefix("{").removeSuffix("}")
        if (trimmed.isBlank()) return emptyMap()
        for (entry in trimmed.split(",")) {
            val parts = entry.split(":", limit = 2)
            if (parts.size != 2) continue
            val key = parts[0].trim().removePrefix("\"").removeSuffix("\"")
            val value = parts[1].trim().toIntOrNull() ?: continue
            if (key.isNotEmpty()) result[key] = value
        }
        return result
    }

    private fun escapeJson(value: String): String {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
    }
}
