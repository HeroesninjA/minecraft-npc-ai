package ro.ainpc.managers

import org.bukkit.entity.Player
import org.bukkit.Bukkit
import ro.ainpc.AINPCPlugin
import ro.ainpc.api.events.AINPCEventSource
import ro.ainpc.api.events.npc.AINPCMemoryRecordedEvent
import ro.ainpc.api.events.npc.AINPCMemoryRecordedEventPayload
import ro.ainpc.npc.AINPC
import java.sql.SQLException
import java.sql.Types
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.CompletableFuture

internal const val ACTIVE_MEMORY_SQL = "(expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP)"

/**
 * Memorymanager.
 */
class MemoryManager(private val plugin: AINPCPlugin) {
    private val playerStatements by lazy { PlayerStatementRepository(plugin.databaseManager) }

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

    fun decayDaysForType(memoryType: String): Int {
        val defaultDecay = plugin.config.getInt("npc.memory_decay_days", 30)
        val typeKey = "npc.memory_decay_types.$memoryType"
        return plugin.config.getInt(typeKey, defaultDecay)
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
        insertMemory(npc, playerUuid, playerName, memoryType, content, emotionalImpact, importance)
    }

    enum class PlayerStatementSaveResult { STORED, ALREADY_STORED, FAILED }
    enum class PlayerStatementCorrectionResult { UPDATED, NOT_FOUND, UNCHANGED, FAILED }

    fun rememberPlayerStatement(
        npc: AINPC,
        playerUuid: UUID,
        playerName: String,
        statement: String,
    ): PlayerStatementSaveResult {
        val existingSql = """
            SELECT 1 FROM npc_memories
            WHERE npc_id = ? AND player_uuid = ? AND memory_type = 'player_confirmed_statement'
                AND LOWER(content) = LOWER(?)
            LIMIT 1
        """
        val alreadyStored = try {
            plugin.databaseManager.prepareStatement(existingSql).use { query ->
                query.setInt(1, npc.databaseId)
                query.setString(2, playerUuid.toString())
                query.setString(3, statement)
                query.executeQuery().use { result -> result.next() }
            }
        } catch (error: SQLException) {
            plugin.logger.warning("Eroare la verificarea declaratiei jucatorului: ${error.message}")
            return PlayerStatementSaveResult.FAILED
        }
        if (alreadyStored) return PlayerStatementSaveResult.ALREADY_STORED
        return if (insertMemory(npc, playerUuid, playerName, "player_confirmed_statement", statement, 0.0, 2)) {
            cleanExcessPlayerStatements(npc, playerUuid)
            PlayerStatementSaveResult.STORED
        } else {
            PlayerStatementSaveResult.FAILED
        }
    }

    fun correctPlayerStatement(
        npc: AINPC,
        playerUuid: UUID,
        previousContent: String,
        replacement: String,
    ): PlayerStatementCorrectionResult = try {
        playerStatements.correct(npc.databaseId, playerUuid, previousContent, replacement)
    } catch (error: SQLException) {
        plugin.logger.warning("Eroare la corectarea declaratiei jucatorului: ${error.message}")
        PlayerStatementCorrectionResult.FAILED
    }

    fun forgetPlayerStatements(npc: AINPC, playerUuid: UUID): Int? =
        forgetStatements(npc, playerUuid, null)

    fun forgetPlayerStatement(npc: AINPC, playerUuid: UUID, content: String): Int? =
        forgetStatements(npc, playerUuid, content)

    fun playerStatementRevision(npc: AINPC, playerUuid: UUID): Long =
        playerStatements.revision(npc.databaseId, playerUuid)

    private fun forgetStatements(npc: AINPC, playerUuid: UUID, content: String?): Int? {
        return try {
            playerStatements.forget(npc.databaseId, playerUuid, content)
        } catch (error: SQLException) {
            plugin.logger.warning("Eroare la uitarea declaratiilor jucatorului: ${error.message}")
            null
        }
    }

    fun getPlayerConfirmedStatements(npc: AINPC, playerUuid: UUID, limit: Int): List<String> {
        if (limit <= 0) return emptyList()
        val sql = """
            SELECT content FROM npc_memories
            WHERE npc_id = ? AND player_uuid = ? AND memory_type = 'player_confirmed_statement'
            ORDER BY created_at DESC, id DESC
            LIMIT ?
        """
        return try {
            plugin.databaseManager.prepareStatement(sql).use { statement ->
                statement.setInt(1, npc.databaseId)
                statement.setString(2, playerUuid.toString())
                statement.setInt(3, limit.coerceAtMost(10))
                statement.executeQuery().use { result ->
                    buildList {
                        while (result.next()) add(result.getString("content"))
                    }
                }
            }
        } catch (error: SQLException) {
            plugin.logger.warning("Eroare la citirea declaratiilor jucatorului: ${error.message}")
            emptyList()
        }
    }

    private fun insertMemory(
        npc: AINPC,
        playerUuid: UUID,
        playerName: String,
        memoryType: String,
        content: String,
        emotionalImpact: Double,
        importance: Int,
    ): Boolean {
        val baseDecay = decayDaysForType(memoryType)
        val expirationDays = maxOf(0, baseDecay * importance)

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
                if (memoryType == "player_confirmed_statement") {
                    stmt.setNull(8, Types.INTEGER)
                } else {
                    stmt.setInt(8, expirationDays)
                }
                stmt.executeUpdate()
            }

            plugin.debug("Amintire creata pentru ${npc.name} despre $playerName")
            cleanExcessMemories(npc, playerUuid)
            publishMemoryRecorded(npc, playerUuid, playerName, memoryType, emotionalImpact, importance)
            return true
        } catch (e: SQLException) {
            plugin.logger.warning("Eroare la crearea amintirii: ${e.message}")
            return false
        }
    }

    fun getRelevantMemories(npc: AINPC, player: Player, context: String, limit: Int): List<String> =
        getRelevantMemories(npc, player.uniqueId, context, limit)

    fun getRelevantMemories(npc: AINPC, playerUuid: UUID, context: String, limit: Int): List<String> {
        if (limit <= 0) return emptyList()
        val maxPromptChars = plugin.config.getInt("npc.memory_prompt_max_chars", MemoryRecallSelector.DEFAULT_MAX_PROMPT_CHARS)
        if (maxPromptChars <= 0) return emptyList()

        val candidates = mutableListOf<MemoryRecallCandidate>()
        val candidateLimit = maxOf(limit, minOf(200, plugin.config.getInt("npc.max_memories_per_player", 50)))
        val sql = """
            SELECT content, memory_type, emotional_impact, importance, created_at
            FROM npc_memories
            WHERE npc_id = ? AND player_uuid = ? AND memory_type <> 'player_confirmed_statement'
                AND $ACTIVE_MEMORY_SQL
            ORDER BY importance DESC, created_at DESC
            LIMIT ?
        """

        try {
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, npc.databaseId)
                stmt.setString(2, playerUuid.toString())
                stmt.setInt(3, candidateLimit)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        val source = when (rs.getString("memory_type")) {
                            "dialog" -> "conversation_quote"
                            "first_meeting", "gift", "help", "betrayal" -> "game_event"
                            else -> "unclassified"
                        }
                        candidates.add(MemoryRecallCandidate(
                            rs.getString("content"),
                            source,
                            MemoryRecallCandidate.parseRecordedOn(rs.getString("created_at")),
                        ))
                    }
                }
            }
        } catch (e: SQLException) {
            plugin.logger.warning("Eroare la obtinerea amintirilor: ${e.message}")
        }

        return MemoryRecallSelector.select(context, candidates, limit, maxPromptChars)
            .map(MemoryRecallCandidate::promptText)
    }

    fun getAllMemories(npc: AINPC, player: Player): List<Memory> = getAllMemories(npc, player.uniqueId)

    fun getAllMemories(npc: AINPC, playerUuid: UUID): List<Memory> {
        val memories = mutableListOf<Memory>()
        val sql = """
            SELECT id, memory_type, content, emotional_impact, importance, created_at
            FROM npc_memories
            WHERE npc_id = ? AND player_uuid = ? AND $ACTIVE_MEMORY_SQL
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
                        val timestamp = rs.getTimestamp("created_at")
                        memory.setCreatedAt(if (timestamp != null) timestamp.toLocalDateTime() else LocalDateTime.now())
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
            WHERE expires_at IS NOT NULL AND expires_at <= CURRENT_TIMESTAMP
                AND memory_type <> 'player_confirmed_statement'
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
                WHERE npc_id = ? AND player_uuid = ? AND memory_type <> 'player_confirmed_statement'
                    AND $ACTIVE_MEMORY_SQL
                ORDER BY importance ASC, created_at ASC
                LIMIT MAX(0, (
                    SELECT COUNT(*) FROM npc_memories
                    WHERE npc_id = ? AND player_uuid = ? AND memory_type <> 'player_confirmed_statement'
                        AND $ACTIVE_MEMORY_SQL
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

    private fun cleanExcessPlayerStatements(npc: AINPC, playerUuid: UUID) {
        val maxStatements = plugin.config.getInt("npc.personal_memory.max_stored_statements", 10).coerceIn(1, 50)
        val sql = """
            DELETE FROM npc_memories
            WHERE npc_id = ? AND player_uuid = ? AND memory_type = 'player_confirmed_statement'
                AND id NOT IN (
                    SELECT id FROM npc_memories
                    WHERE npc_id = ? AND player_uuid = ? AND memory_type = 'player_confirmed_statement'
                    ORDER BY created_at DESC, id DESC
                    LIMIT ?
                )
        """
        try {
            plugin.databaseManager.prepareStatement(sql).use { statement ->
                statement.setInt(1, npc.databaseId)
                statement.setString(2, playerUuid.toString())
                statement.setInt(3, npc.databaseId)
                statement.setString(4, playerUuid.toString())
                statement.setInt(5, maxStatements)
                statement.executeUpdate()
            }
        } catch (error: SQLException) {
            plugin.logger.warning("Eroare la limitarea declaratiilor jucatorului: ${error.message}")
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
        val sql = "SELECT COUNT(*) FROM npc_memories WHERE npc_id = ? AND player_uuid = ? AND $ACTIVE_MEMORY_SQL"
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
        val sql = "SELECT COUNT(*) FROM npc_memories WHERE npc_id = ? AND player_uuid = ? AND $ACTIVE_MEMORY_SQL"
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
            WHERE npc_id = ? AND player_uuid = ? AND memory_type <> 'player_confirmed_statement'
                AND $ACTIVE_MEMORY_SQL
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

/**
 * Memorystats.
 */
    class MemoryStats(
        private val hasMemories: Boolean,
        private val memoryCount: Int,
        private val emotionalImpact: Double
    ) {
        fun hasMemories(): Boolean = hasMemories
        fun memoryCount(): Int = memoryCount
        fun emotionalImpact(): Double = emotionalImpact
    }

/**
 * Memory.
 */
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
                "player_confirmed_statement" -> "🧠"
                "gift" -> "🎁"
                "help" -> "🤝"
                "betrayal" -> "💔"
                else -> "📝"
            }

        fun getImportanceStars(): String = "★".repeat(minOf(5, importance)) + "☆".repeat(maxOf(0, 5 - importance))
    }

    private fun publishMemoryRecorded(npc: AINPC, playerUuid: UUID, playerName: String?, memoryType: String, emotionalImpact: Double, importance: Int) {
        if (!plugin.config.getBoolean("events.public_api_enabled", true)) return
        val event = AINPCMemoryRecordedEvent(
            AINPCMemoryRecordedEventPayload(
                UUID.randomUUID(),
                System.currentTimeMillis(),
                AINPCEventSource.SYSTEM,
                npc.databaseId.toString(),
                npc.uuid,
                npc.name,
                playerUuid,
                playerName,
                memoryType,
                emotionalImpact,
                importance,
                mapOf("source" to "MemoryManager")
            )
        )
        if (Bukkit.isPrimaryThread()) {
            plugin.server.pluginManager.callEvent(event)
        } else {
            plugin.server.scheduler.runTask(plugin, Runnable {
                plugin.server.pluginManager.callEvent(event)
            })
        }
    }
}
