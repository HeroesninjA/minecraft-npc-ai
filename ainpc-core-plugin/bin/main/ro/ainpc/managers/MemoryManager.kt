package ro.ainpc.managers

import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import ro.ainpc.npc.AINPC
import java.sql.SQLException
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.CompletableFuture

class MemoryManager(private val plugin: AINPCPlugin) {

    fun createMemory(
        npc: AINPC,
        player: Player,
        memoryType: String,
        content: String,
        emotionalImpact: Double,
        importance: Int
    ) {
        createMemory(npc, player.uniqueId, player.name, memoryType, content, emotionalImpact, importance)
    }

    fun createMemory(
        npc: AINPC,
        playerUuid: UUID,
        playerName: String,
        memoryType: String,
        content: String,
        emotionalImpact: Double,
        importance: Int
    ) {
        val decayDays = plugin.config.getInt("npc.memory_decay_days", 30)
        val expirationDays = decayDays * importance

        val sql = """
            INSERT INTO npc_memories 
            (npc_id, player_uuid, player_name, memory_type, content, emotional_impact, importance, expires_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, datetime('now', '+' || ? || ' days'))
        """

        try {
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, npc.databaseId)
                stmt.setString(2, playerUuid.toString())
                stmt.setString(3, playerName)
                stmt.setString(4, memoryType)
                stmt.setString(5, content)
                stmt.setDouble(6, emotionalImpact)
                stmt.setInt(7, importance)
                stmt.setInt(8, expirationDays)
                stmt.executeUpdate()
            }

            plugin.debug("Amintire creata pentru ${npc.name} despre $playerName")
            cleanExcessMemories(npc, playerUuid)
        } catch (e: SQLException) {
            plugin.logger.warning("Eroare la crearea amintirii: ${e.message}")
        }
    }

    fun getRelevantMemories(npc: AINPC, player: Player, context: String, limit: Int): List<String> =
        getRelevantMemories(npc, player.uniqueId, context, limit)

    fun getRelevantMemories(npc: AINPC, playerUuid: UUID, context: String, limit: Int): List<String> {
        val memories = mutableListOf<String>()
        val sql = """
            SELECT content, memory_type, emotional_impact, importance, created_at
            FROM npc_memories
            WHERE npc_id = ? AND player_uuid = ?
            ORDER BY importance DESC, created_at DESC
            LIMIT ?
        """

        try {
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, npc.databaseId)
                stmt.setString(2, playerUuid.toString())
                stmt.setInt(3, limit)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        memories.add(rs.getString("content"))
                    }
                }
            }
        } catch (e: SQLException) {
            plugin.logger.warning("Eroare la obtinerea amintirilor: ${e.message}")
        }

        return memories
    }

    fun getAllMemories(npc: AINPC, player: Player): List<Memory> = getAllMemories(npc, player.uniqueId)

    fun getAllMemories(npc: AINPC, playerUuid: UUID): List<Memory> {
        val memories = mutableListOf<Memory>()
        val sql = """
            SELECT id, memory_type, content, emotional_impact, importance, created_at
            FROM npc_memories
            WHERE npc_id = ? AND player_uuid = ?
            ORDER BY created_at DESC
        """

        try {
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, npc.databaseId)
                stmt.setString(2, playerUuid.toString())
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        val memory = Memory()
                        memory.setId(rs.getInt("id"))
                        memory.setMemoryType(rs.getString("memory_type"))
                        memory.setContent(rs.getString("content"))
                        memory.setEmotionalImpact(rs.getDouble("emotional_impact"))
                        memory.setImportance(rs.getInt("importance"))
                        memory.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime())
                        memories.add(memory)
                    }
                }
            }
        } catch (e: SQLException) {
            plugin.logger.warning("Eroare la obtinerea amintirilor: ${e.message}")
        }

        return memories
    }

    fun cleanOldMemories() {
        val deleteExpired = """
            DELETE FROM npc_memories
            WHERE expires_at IS NOT NULL AND expires_at < datetime('now')
        """

        try {
            plugin.databaseManager.prepareStatement(deleteExpired).use { stmt ->
                val deleted = stmt.executeUpdate()
                if (deleted > 0) {
                    plugin.debug("Sterse $deleted amintiri expirate.")
                }
            }
        } catch (e: SQLException) {
            plugin.logger.warning("Eroare la curatarea amintirilor: ${e.message}")
        }
    }

    private fun cleanExcessMemories(npc: AINPC, player: Player) {
        cleanExcessMemories(npc, player.uniqueId)
    }

    private fun cleanExcessMemories(npc: AINPC, playerUuid: UUID) {
        val maxMemories = plugin.config.getInt("npc.max_memories_per_player", 50)
        val sql = """
            DELETE FROM npc_memories
            WHERE id IN (
                SELECT id FROM npc_memories
                WHERE npc_id = ? AND player_uuid = ?
                ORDER BY importance ASC, created_at ASC
                LIMIT MAX(0, (
                    SELECT COUNT(*) FROM npc_memories
                    WHERE npc_id = ? AND player_uuid = ?
                ) - ?)
            )
        """

        try {
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, npc.databaseId)
                stmt.setString(2, playerUuid.toString())
                stmt.setInt(3, npc.databaseId)
                stmt.setString(4, playerUuid.toString())
                stmt.setInt(5, maxMemories)
                stmt.executeUpdate()
            }
        } catch (e: SQLException) {
            plugin.logger.warning("Eroare la curatarea amintirilor in exces: ${e.message}")
        }
    }

    fun createFirstMeetingMemory(npc: AINPC, player: Player) {
        createFirstMeetingMemory(npc, player.uniqueId, player.name)
    }

    fun createFirstMeetingMemory(npc: AINPC, playerUuid: UUID, playerName: String) {
        val content = "L-am intalnit pe $playerName pentru prima data."
        createMemory(npc, playerUuid, playerName, "first_meeting", content, 0.3, 3)
    }

    fun createGiftMemory(npc: AINPC, player: Player, itemName: String) {
        val content = "${player.name} mi-a oferit un cadou: $itemName"
        createMemory(npc, player, "gift", content, 0.5, 4)
    }

    fun createBetrayalMemory(npc: AINPC, player: Player, reason: String) {
        val content = "${player.name} m-a tradat: $reason"
        createMemory(npc, player, "betrayal", content, -0.8, 5)
    }

    fun createHelpMemory(npc: AINPC, player: Player, helpType: String) {
        val content = "${player.name} m-a ajutat: $helpType"
        createMemory(npc, player, "help", content, 0.4, 3)
    }

    fun addScenarioMemory(npcUuid: UUID, scenarioType: String, role: String) {
        val npc = plugin.npcManager.getNPCByUuid(npcUuid) ?: return
        val sql = """
            INSERT INTO npc_memories
            (npc_id, player_uuid, player_name, memory_type, content, emotional_impact, importance, expires_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, datetime('now', '+90 days'))
        """

        try {
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, npc.databaseId)
                stmt.setString(2, "scenario:${scenarioType.lowercase()}")
                stmt.setString(3, "Scenario")
                stmt.setString(4, "scenario")
                stmt.setString(5, "Am participat la scenariul $scenarioType cu rolul $role.")
                stmt.setDouble(6, 0.2)
                stmt.setInt(7, 3)
                stmt.executeUpdate()
            }
        } catch (e: SQLException) {
            plugin.logger.warning("Eroare la salvarea amintirii de scenariu: ${e.message}")
        }
    }

    fun hasMemoriesOf(npc: AINPC, player: Player): Boolean = hasMemoriesOf(npc, player.uniqueId)

    fun hasMemoriesOf(npc: AINPC, playerUuid: UUID): Boolean {
        val sql = "SELECT COUNT(*) FROM npc_memories WHERE npc_id = ? AND player_uuid = ?"
        return try {
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, npc.databaseId)
                stmt.setString(2, playerUuid.toString())
                stmt.executeQuery().use { rs ->
                    rs.next() && rs.getInt(1) > 0
                }
            }
        } catch (e: SQLException) {
            plugin.logger.warning("Eroare la verificarea amintirilor: ${e.message}")
            false
        }
    }

    fun getMemoryCount(npc: AINPC, player: Player): Int = getMemoryCount(npc, player.uniqueId)

    fun getMemoryCount(npc: AINPC, playerUuid: UUID): Int {
        val sql = "SELECT COUNT(*) FROM npc_memories WHERE npc_id = ? AND player_uuid = ?"
        return try {
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, npc.databaseId)
                stmt.setString(2, playerUuid.toString())
                stmt.executeQuery().use { rs ->
                    if (rs.next()) rs.getInt(1) else 0
                }
            }
        } catch (e: SQLException) {
            plugin.logger.warning("Eroare la numararea amintirilor: ${e.message}")
            0
        }
    }

    fun getTotalEmotionalImpact(npc: AINPC, player: Player): Double = getTotalEmotionalImpact(npc, player.uniqueId)

    fun getTotalEmotionalImpact(npc: AINPC, playerUuid: UUID): Double {
        val sql = """
            SELECT SUM(emotional_impact * importance) / SUM(importance) as weighted_avg
            FROM npc_memories
            WHERE npc_id = ? AND player_uuid = ?
        """
        return try {
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, npc.databaseId)
                stmt.setString(2, playerUuid.toString())
                stmt.executeQuery().use { rs ->
                    if (rs.next()) rs.getDouble("weighted_avg") else 0.0
                }
            }
        } catch (e: SQLException) {
            plugin.logger.warning("Eroare la calcularea impactului emotional: ${e.message}")
            0.0
        }
    }

    fun ensureFirstMeetingMemoryAsync(npc: AINPC, player: Player): CompletableFuture<Boolean> {
        val playerUuid = player.uniqueId
        val playerName = player.name
        return plugin.databaseManager.supplyAsync {
            val hasMemories = hasMemoriesOf(npc, playerUuid)
            if (!hasMemories) {
                createFirstMeetingMemory(npc, playerUuid, playerName)
            }
            !hasMemories
        }
    }

    fun getPlayerMemoryStatsAsync(npc: AINPC, player: Player): CompletableFuture<MemoryStats> {
        val playerUuid = player.uniqueId
        return plugin.databaseManager.supplyAsync {
            MemoryStats(
                hasMemoriesOf(npc, playerUuid),
                getMemoryCount(npc, playerUuid),
                getTotalEmotionalImpact(npc, playerUuid)
            )
        }
    }

    class MemoryStats(
        private val hasMemories: Boolean,
        private val memoryCount: Int,
        private val emotionalImpact: Double
    ) {
        fun hasMemories(): Boolean = hasMemories
        fun memoryCount(): Int = memoryCount
        fun emotionalImpact(): Double = emotionalImpact
    }

    class Memory {
        private var id: Int = 0
        private var memoryType: String = ""
        private var content: String = ""
        private var emotionalImpact: Double = 0.0
        private var importance: Int = 0
        private var createdAt: LocalDateTime? = null

        fun getId(): Int = id
        fun setId(id: Int) { this.id = id }

        fun getMemoryType(): String = memoryType
        fun setMemoryType(memoryType: String) { this.memoryType = memoryType }

        fun getContent(): String = content
        fun setContent(content: String) { this.content = content }

        fun getEmotionalImpact(): Double = emotionalImpact
        fun setEmotionalImpact(emotionalImpact: Double) { this.emotionalImpact = emotionalImpact }

        fun getImportance(): Int = importance
        fun setImportance(importance: Int) { this.importance = importance }

        fun getCreatedAt(): LocalDateTime? = createdAt
        fun setCreatedAt(createdAt: LocalDateTime?) { this.createdAt = createdAt }

        fun getTypeEmoji(): String =
            when (memoryType) {
                "first_meeting" -> "👋"
                "dialog" -> "💬"
                "gift" -> "🎁"
                "help" -> "🤝"
                "betrayal" -> "💔"
                else -> "📝"
            }

        fun getImportanceStars(): String = "★".repeat(minOf(5, importance)) + "☆".repeat(maxOf(0, 5 - importance))
    }
}
