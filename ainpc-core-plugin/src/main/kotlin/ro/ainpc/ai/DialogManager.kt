package ro.ainpc.ai

import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import ro.ainpc.engine.DialogueEngine
import ro.ainpc.npc.AINPC
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * Manager pentru dialoguri si conversatii intre jucatori si NPC-uri
 */
class DialogManager(private val plugin: AINPCPlugin) {
    private val cooldowns: MutableMap<UUID, MutableMap<UUID, Long>> = ConcurrentHashMap()

    /**
     * Proceseaza un mesaj de la jucator catre NPC
     */
    fun processMessage(npc: AINPC, player: Player, message: String): CompletableFuture<DialogResult> {
        return processMessage(
            DialogRequest(
                npc,
                player,
                message,
                true,
                true,
                "explicit_interaction",
                1,
                0.0
            )
        )
    }

    fun processMessage(request: DialogRequest): CompletableFuture<DialogResult> {
        val npc = request.npc()
        val player = request.player()
        val message = request.message()
        val playerUuid = player.uniqueId
        val playerName = player.name

        // Verifica cooldown
        if (isOnCooldown(playerUuid, npc.uuid)) {
            return CompletableFuture.completedFuture(DialogResult.cooldown())
        }

        // Seteaza cooldown
        setCooldown(playerUuid, npc.uuid)

        return plugin.databaseManager.supplyAsync {
            // Obtine istoricul conversatiei
            val history = getRecentHistory(npc, playerUuid, 5)

            // Obtine amintiri relevante
            val memories = plugin.memoryManager.getRelevantMemories(npc, playerUuid, message, 5)
            val totalMemoryCount = plugin.memoryManager.getMemoryCount(npc, playerUuid)
            val weightedMemoryImpact = plugin.memoryManager.getTotalEmotionalImpact(npc, playerUuid)

            // Obtine relatia
            val relationship = getRelationship(npc, playerUuid)

            DialogContext(
                history,
                memories,
                relationship,
                PromptDbContext(totalMemoryCount, weightedMemoryImpact)
            )
        }.thenCompose { context ->
            val dialogueEngine: DialogueEngine? = plugin.dialogueEngine
            val responseFuture: CompletableFuture<String> = if (dialogueEngine != null) {
                dialogueEngine.generateResponse(
                    request,
                    context.history,
                    context.memories,
                    context.relationship,
                    context.dbContext
                )
            } else {
                plugin.openAIService.generateResponse(
                    request,
                    context.history,
                    context.memories,
                    context.relationship,
                    context.dbContext
                )
            }

            responseFuture.thenApply { response -> GeneratedDialog(response, context.relationship) }
        }.thenCompose { generated ->
            if (generated.response().isNullOrBlank()) {
                return@thenCompose CompletableFuture.completedFuture(DialogResult.error())
            }

            val sentiment = plugin.openAIService.analyzeSentimentFast(message)

            plugin.databaseManager.supplyAsync {
                saveDialog(npc, playerUuid, message, generated.response())
                updateRelationship(npc, playerUuid, playerName, sentiment)
                createMemoryIfImportant(npc, playerUuid, playerName, message, sentiment)
                PostProcessResult(generated.response(), sentiment, generated.relationship())
            }.thenApply { postProcess ->
                updateEmotions(npc, postProcess.relationship(), postProcess.sentiment())
                DialogResult.success(postProcess.response())
            }
        }.exceptionally { ex ->
            plugin.logger.warning("Eroare in procesarea dialogului: " + ex.message)
            DialogResult.error()
        }
    }

    /**
     * Obtine istoricul recent al conversatiei
     */
    fun getRecentHistory(npc: AINPC, player: Player, limit: Int): List<OpenAIService.DialogHistory> {
        return getRecentHistory(npc, player.uniqueId, limit)
    }

    fun getRecentHistory(npc: AINPC, playerUuid: UUID, limit: Int): List<OpenAIService.DialogHistory> {
        val history = mutableListOf<OpenAIService.DialogHistory>()

        val sql = """
            SELECT player_message, npc_response, created_at
            FROM dialog_history
            WHERE npc_id = ? AND player_uuid = ?
            ORDER BY created_at DESC
            LIMIT ?
        """.trimIndent()

        try {
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, npc.databaseId)
                stmt.setString(2, playerUuid.toString())
                stmt.setInt(3, limit)

                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        history.add(
                            OpenAIService.DialogHistory(
                                rs.getString("player_message"),
                                rs.getString("npc_response"),
                                rs.getTimestamp("created_at").time
                            )
                        )
                    }
                }
            }
        } catch (e: SQLException) {
            plugin.logger.warning("Eroare la obtinerea istoricului: " + e.message)
        }

        // Inverseaza pentru ordine cronologica
        history.reverse()
        return history
    }

    /**
     * Salveaza un dialog in baza de date
     */
    fun saveDialog(npc: AINPC, player: Player, playerMessage: String, npcResponse: String) {
        saveDialog(npc, player.uniqueId, playerMessage, npcResponse)
    }

    fun saveDialog(npc: AINPC, playerUuid: UUID, playerMessage: String, npcResponse: String) {
        val sql = """
            INSERT INTO dialog_history (npc_id, player_uuid, player_message, npc_response, emotion_state)
            VALUES (?, ?, ?, ?, ?)
        """.trimIndent()

        try {
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, npc.databaseId)
                stmt.setString(2, playerUuid.toString())
                stmt.setString(3, playerMessage)
                stmt.setString(4, npcResponse)
                stmt.setString(5, npc.emotions.dominantEmotion)
                stmt.executeUpdate()
            }
        } catch (e: SQLException) {
            plugin.logger.warning("Eroare la salvarea dialogului: " + e.message)
        }
    }

    /**
     * Obtine relatia dintre NPC si jucator
     */
    fun getRelationship(npc: AINPC, player: Player): OpenAIService.NPCRelationship? {
        return getRelationship(npc, player.uniqueId)
    }

    fun getRelationship(npc: AINPC, playerUuid: UUID): OpenAIService.NPCRelationship? {
        val sql = """
            SELECT affection, trust, respect, familiarity, interaction_count, relationship_type
            FROM npc_relationships
            WHERE npc_id = ? AND player_uuid = ?
        """.trimIndent()

        try {
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, npc.databaseId)
                stmt.setString(2, playerUuid.toString())

                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        val rel = OpenAIService.NPCRelationship()
                        rel.affection = rs.getDouble("affection")
                        rel.trust = rs.getDouble("trust")
                        rel.respect = rs.getDouble("respect")
                        rel.familiarity = rs.getDouble("familiarity")
                        rel.interactionCount = rs.getInt("interaction_count")
                        rel.relationshipType = rs.getString("relationship_type")
                        return rel
                    }
                }
            }
        } catch (e: SQLException) {
            plugin.logger.warning("Eroare la obtinerea relatiei: " + e.message)
        }

        return null // Prima intalnire
    }

    fun getRelationshipAsync(npc: AINPC, player: Player): CompletableFuture<OpenAIService.NPCRelationship?> {
        val playerUuid = player.uniqueId
        return plugin.databaseManager.supplyAsync { getRelationship(npc, playerUuid) }
    }

    /**
     * Actualizeaza relatia dupa o interactiune
     */
    private fun updateRelationship(npc: AINPC, playerUuid: UUID, playerName: String, sentiment: String) {
        var affectionChange = 0.0
        var trustChange = 0.0
        var respectChange = 0.0

        when (sentiment) {
            "positive", "compliment" -> {
                affectionChange = 0.05
                trustChange = 0.03
            }
            "greeting" -> {
                affectionChange = 0.02
                trustChange = 0.01
            }
            "negative", "insult" -> {
                affectionChange = -0.1
                trustChange = -0.05
                respectChange = -0.05
            }
            "threat" -> {
                affectionChange = -0.15
                trustChange = -0.2
                respectChange = -0.1
            }
        }

        // Aplica modificarile in baza de date
        val sql = """
            INSERT INTO npc_relationships 
            (npc_id, player_uuid, player_name, affection, trust, respect, familiarity, interaction_count, last_interaction, relationship_type)
            VALUES (?, ?, ?, ?, ?, ?, 0.1, 1, CURRENT_TIMESTAMP, 'acquaintance')
            ON CONFLICT(npc_id, player_uuid) DO UPDATE SET
                affection = MIN(1.0, MAX(-1.0, affection + ?)),
                trust = MIN(1.0, MAX(-1.0, trust + ?)),
                respect = MIN(1.0, MAX(-1.0, respect + ?)),
                familiarity = MIN(1.0, familiarity + 0.01),
                interaction_count = interaction_count + 1,
                last_interaction = CURRENT_TIMESTAMP,
                relationship_type = CASE
                    WHEN affection + ? > 0.7 THEN 'close_friend'
                    WHEN affection + ? > 0.4 THEN 'friend'
                    WHEN affection + ? > 0.1 THEN 'acquaintance'
                    WHEN affection + ? < -0.5 THEN 'enemy'
                    WHEN affection + ? < -0.2 THEN 'rival'
                    ELSE 'stranger'
                END
        """.trimIndent()

        try {
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, npc.databaseId)
                stmt.setString(2, playerUuid.toString())
                stmt.setString(3, playerName)
                stmt.setDouble(4, affectionChange)
                stmt.setDouble(5, trustChange)
                stmt.setDouble(6, respectChange)
                stmt.setDouble(7, affectionChange)
                stmt.setDouble(8, trustChange)
                stmt.setDouble(9, respectChange)
                stmt.setDouble(10, affectionChange)
                stmt.setDouble(11, affectionChange)
                stmt.setDouble(12, affectionChange)
                stmt.setDouble(13, affectionChange)
                stmt.setDouble(14, affectionChange)
                stmt.executeUpdate()
            }
        } catch (e: SQLException) {
            plugin.logger.warning("Eroare la actualizarea relatiei: " + e.message)
        }
    }

    /**
     * Creeaza o amintire daca interactiunea este importanta
     */
    private fun createMemoryIfImportant(
        npc: AINPC,
        playerUuid: UUID,
        playerName: String,
        playerMessage: String,
        sentiment: String
    ) {
        val importance = when (sentiment) {
            "insult", "threat" -> 4
            "compliment" -> 3
            "positive", "negative" -> 2
            else -> 1
        }

        // Creeaza amintire doar pentru interactiuni importante
        if (importance >= 2) {
            val content = "Jucatorul $playerName mi-a spus: \"" +
                truncate(playerMessage, 100) + "\" (sentiment: " + sentiment + ")"

            plugin.memoryManager.createMemory(
                npc, playerUuid, playerName,
                "dialog",
                content,
                sentimentToEmotionalImpact(sentiment),
                importance
            )
        }
    }

    /**
     * Actualizeaza emotiile NPC-ului bazat pe mesaj
     */
    private fun updateEmotions(npc: AINPC, relationship: OpenAIService.NPCRelationship?, sentiment: String) {
        val familiarity = relationship?.familiarity ?: 0.0
        val multiplier = 0.5 + familiarity * 0.5

        // Aplica efectul emotional
        val interactionType = when (sentiment) {
            "positive" -> "compliment"
            "negative" -> "insult"
            "greeting" -> "greeting"
            "threat" -> "threat"
            "compliment" -> "compliment"
            "insult" -> "insult"
            else -> "greeting"
        }

        plugin.emotionManager.applyInteractionEffect(npc, interactionType, multiplier)
    }

    fun isOnCooldown(player: Player, npc: AINPC): Boolean {
        return isOnCooldown(player.uniqueId, npc.uuid)
    }

    // Cooldown methods

    private fun isOnCooldown(playerUuid: UUID, npcUuid: UUID): Boolean {
        val playerCooldowns = cooldowns[playerUuid] ?: return false

        val lastTime = playerCooldowns[npcUuid] ?: return false

        val cooldownMs = plugin.config.getInt("npc.message_cooldown", 2) * 1000L
        return System.currentTimeMillis() - lastTime < cooldownMs
    }

    private fun setCooldown(playerUuid: UUID, npcUuid: UUID) {
        cooldowns.computeIfAbsent(playerUuid) { ConcurrentHashMap() }
            .put(npcUuid, System.currentTimeMillis())
    }

    // Helper methods

    private fun truncate(text: String, maxLength: Int): String {
        if (text.length <= maxLength) return text
        return text.substring(0, maxLength - 3) + "..."
    }

    private fun sentimentToEmotionalImpact(sentiment: String): Double {
        return when (sentiment) {
            "positive", "compliment" -> 0.5
            "greeting" -> 0.1
            "negative" -> -0.3
            "insult" -> -0.6
            "threat" -> -0.8
            else -> 0.0
        }
    }

    // Context holder class
    private class DialogContext(
        val history: List<OpenAIService.DialogHistory>,
        val memories: List<String>,
        val relationship: OpenAIService.NPCRelationship?,
        val dbContext: PromptDbContext
    )

    private data class GeneratedDialog(
        val response: String,
        val relationship: OpenAIService.NPCRelationship?
    ) {
        fun response(): String = response
        fun relationship(): OpenAIService.NPCRelationship? = relationship
    }

    private data class PostProcessResult(
        val response: String,
        val sentiment: String,
        val relationship: OpenAIService.NPCRelationship?
    ) {
        fun response(): String = response
        fun sentiment(): String = sentiment
        fun relationship(): OpenAIService.NPCRelationship? = relationship
    }

    data class PromptDbContext(
        val totalMemoryCount: Int,
        val weightedMemoryImpact: Double
    ) {
        fun totalMemoryCount(): Int = totalMemoryCount
        fun weightedMemoryImpact(): Double = weightedMemoryImpact
    }

    class DialogResult private constructor(
        val status: DialogStatus,
        val response: String?
    ) {
        fun isSuccess(): Boolean = status == DialogStatus.SUCCESS

        companion object {
            @JvmStatic
            fun success(response: String): DialogResult = DialogResult(DialogStatus.SUCCESS, response)

            @JvmStatic
            fun cooldown(): DialogResult = DialogResult(DialogStatus.COOLDOWN, null)

            @JvmStatic
            fun error(): DialogResult = DialogResult(DialogStatus.ERROR, null)
        }
    }

    enum class DialogStatus {
        SUCCESS,
        COOLDOWN,
        ERROR
    }

    data class DialogRequest(
        val npc: AINPC,
        val player: Player,
        val message: String,
        val directAddress: Boolean,
        val explicitConversation: Boolean,
        val triggerReason: String,
        val nearbyNpcCount: Int,
        val distanceToNpc: Double
    ) {
        fun npc(): AINPC = npc
        fun player(): Player = player
        fun message(): String = message
        fun directAddress(): Boolean = directAddress
        fun explicitConversation(): Boolean = explicitConversation
        fun triggerReason(): String = triggerReason
        fun nearbyNpcCount(): Int = nearbyNpcCount
        fun distanceToNpc(): Double = distanceToNpc
    }
}
