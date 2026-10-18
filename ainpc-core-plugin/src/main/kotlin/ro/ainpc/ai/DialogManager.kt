package ro.ainpc.ai

import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import ro.ainpc.api.events.AINPCEventSource
import ro.ainpc.api.events.dialog.DialogAIRequestBuiltEvent
import ro.ainpc.api.events.dialog.DialogAIRequestBuiltEventPayload
import ro.ainpc.engine.DialogueEngine
import ro.ainpc.engine.DialogueAiIntentClassification
import ro.ainpc.managers.MemoryManager
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NpcInteractionProfile
import ro.ainpc.world.WorldContextSnapshot
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
    private val playerProfiles = PlayerProfileRepository(plugin.databaseManager)
    private val dialogHistory = DialogHistoryRepository(plugin.databaseManager)
    private val interactionProfileResponses = NpcInteractionProfileResponses()
    private val pendingRequests = DialogueRequestGate()

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
        if (parsePersonalMemoryAction(request) != null) return processAcceptedMessage(request)
        return pendingRequests.submit(request.player().uniqueId, request.npc().uuid, DialogResult::busy) {
            processAcceptedMessage(request)
        }
    }

    fun isProcessingMessage(request: DialogRequest): Boolean =
        pendingRequests.isActive(request.player().uniqueId, request.npc().uuid) &&
            parsePersonalMemoryAction(request) == null

    private fun parsePersonalMemoryAction(request: DialogRequest): PlayerPersonalMemoryCommand.Action? =
        if (request.directAddress() && request.explicitConversation()) {
            PlayerPersonalMemoryCommand.parse(
                request.message(),
                plugin.config.getInt("npc.personal_memory.max_statement_chars", 160).coerceIn(40, 300),
            )
        } else {
            null
        }

    private fun processAcceptedMessage(request: DialogRequest): CompletableFuture<DialogResult> {
        val npc = request.npc()
        val player = request.player()
        val message = request.message()
        val playerUuid = player.uniqueId
        val playerName = player.name
        val regionId = npc.context.worldContextSnapshot.currentRegion()?.id().orEmpty()
        val storyWorldContext = npc.context.worldContextSnapshot

        // Verifica cooldown
        if (isOnCooldown(playerUuid, npc.uuid)) {
            return CompletableFuture.completedFuture(DialogResult.cooldown())
        }

        val personalMemoryEnabled = plugin.config.getBoolean("npc.personal_memory.enabled", true)
        val personalMemoryAction = parsePersonalMemoryAction(request)
        if (personalMemoryAction == PlayerPersonalMemoryCommand.Action.Forget ||
            personalMemoryAction is PlayerPersonalMemoryCommand.Action.ForgetOne ||
            personalMemoryAction == PlayerPersonalMemoryCommand.Action.MissingForgetStatement ||
            personalMemoryAction == PlayerPersonalMemoryCommand.Action.InvalidForgetStatement
        ) {
            setCooldown(playerUuid, npc.uuid)
            return processPersonalMemoryAction(npc, playerUuid, playerName, personalMemoryAction)
        }

        val classificationEnabled = plugin.config.getBoolean("features.ai", false) &&
            plugin.config.getBoolean("ai.orchestration.enabled", false) &&
            plugin.config.getBoolean("ai.orchestration.intent_classification.enabled", false)
        val inspectDomainContextForIntent = DialogueAiIntentClassification.shouldPrefetchDomainContext(
            message,
            npc.context.relationshipStatus,
            !npc.occupation.isNullOrBlank(),
            classificationEnabled,
        )
        val associatedShops = if (npc.interactionProfile == NpcInteractionProfile.SHOP_ONLY ||
            DialogueAiIntentClassification.shouldInspectShops(
                message,
                npc.context.relationshipStatus,
                !npc.occupation.isNullOrBlank(),
                classificationEnabled,
            )
        ) {
            NpcShopAssociation.findShops(plugin.shopService, npc, plugin.platform.worldAdmin)
        } else {
            emptyList()
        }
        val snapshotWorkFacts = DialogueAiIntentClassification.shouldSnapshotWorkFacts(
            message,
            npc.context.relationshipStatus,
            !npc.occupation.isNullOrBlank(),
            classificationEnabled,
        )
        val workFacts = if (snapshotWorkFacts) {
            NpcWorkFactResponse.Facts(
                npc.occupation,
                npc.plannedRoutineActivity,
                OpenAISemanticWorldContextBuilder
                    .resolveWorkPlace(plugin.platform.worldAdmin, npc.workAnchor)?.displayName(),
            )
        } else {
            null
        }
        val accessibleShops = associatedShops.filter { plugin.shopService.canAccess(player, it) }
        val shopAvailable = NpcShopFactResolver.hasListedGoods(accessibleShops)
        val shopAccessDenied = !shopAvailable && NpcShopFactResolver.hasListedGoods(associatedShops)

        // Verifica interaction profile
        val profileBlock = checkInteractionProfile(npc, player, shopAvailable, shopAccessDenied)
        if (profileBlock != null) {
            setCooldown(playerUuid, npc.uuid)
            return CompletableFuture.completedFuture(DialogResult.success(profileBlock))
        }

        // Seteaza cooldown
        setCooldown(playerUuid, npc.uuid)

        if (personalMemoryAction != null && !personalMemoryEnabled) {
            return CompletableFuture.completedFuture(DialogResult.success("Nu pot retine informatii personale acum."))
        }
        if (personalMemoryAction != null) {
            return processPersonalMemoryAction(npc, playerUuid, playerName, personalMemoryAction)
        }

        return plugin.databaseManager.supplyAsync {
            val playerStatementRevision = plugin.memoryManager.playerStatementRevision(npc, playerUuid)
            // Obtine istoricul conversatiei
            val history = DialogHistory.recentStable(getRecentHistory(npc, playerUuid, 5))

            // Obtine amintiri relevante
            val memoryQuery = PlayerConversationContinuation.memoryQuery(message, history)
            val memories = plugin.memoryManager.getRelevantMemories(npc, playerUuid, memoryQuery, 5)
            val playerConfirmedStatements = if (personalMemoryEnabled) {
                plugin.memoryManager.getPlayerConfirmedStatements(npc, playerUuid, 10)
            } else {
                emptyList()
            }
            val totalMemoryCount = plugin.memoryManager.getMemoryCount(npc, playerUuid)
            val weightedMemoryImpact = plugin.memoryManager.getTotalEmotionalImpact(npc, playerUuid)

            // Obtine relatia
            val relationship = getRelationship(npc, playerUuid)
            val regionalReputation = regionId.takeIf { it.isNotBlank() }?.let {
                plugin.reputationService.getReputation(playerUuid.toString(), "region", it)
            }
            val playerProfile = try {
                playerProfiles.find(playerUuid)
            } catch (error: SQLException) {
                plugin.logger.warning("Profilul jucatorului nu a putut fi citit: ${error.message}")
                null
            }

            DialogContext(
                history,
                memories,
                relationship,
                PromptDbContext(
                    totalMemoryCount,
                    weightedMemoryImpact,
                    regionalReputation,
                    playerProfile,
                    playerConfirmedStatements,
                    plugin.config.getInt("npc.personal_memory.max_prompt_statements", 3).coerceIn(0, 10),
                ),
                playerStatementRevision,
                loadStoryFactResponse(message, storyWorldContext),
            )
        }.thenCompose { context ->
            generateResponseOnMainThread(npc, player, message, request, context) {
                val dialogueEngine: DialogueEngine? = plugin.dialogueEngine
                val responseFuture: CompletableFuture<String> = if (dialogueEngine != null) {
                    dialogueEngine.generateResponse(
                        request,
                        context.history,
                        context.memories,
                        context.relationship,
                        context.dbContext,
                        accessibleShops,
                        shopAccessDenied,
                        workFacts,
                        context.storyFactResponse,
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

                responseFuture
            }.thenApply { response ->
                GeneratedDialog(response, context.relationship, context.playerStatementRevision)
            }
        }.thenCompose { generated ->
            if (generated.response().isNullOrBlank()) {
                return@thenCompose CompletableFuture.completedFuture(DialogResult.error())
            }

            val sentiment = plugin.openAIService.analyzeSentimentFast(message)
            val interactionEffect = DialogueInteractionPolicy.forMessage(
                message,
                sentiment,
                request.directAddress(),
                npc.name,
                npc.displayName,
                request.addressClarified,
            )

            val branchDecision = BranchDecision.runtimeSelected(
                "branch_${System.currentTimeMillis()}",
                "raspuns_generat"
            )

            plugin.databaseManager.supplyAsync {
                if (plugin.memoryManager.playerStatementRevision(npc, playerUuid) != generated.playerStatementRevision) {
                    return@supplyAsync null
                }
                saveDialog(npc, playerUuid, message, generated.response())
                recordBranchDecision(npc, playerUuid, branchDecision)
                updateRelationship(npc, playerUuid, playerName, interactionEffect)
                createMemoryIfImportant(npc, playerUuid, playerName, message, interactionEffect.emotionType ?: sentiment, interactionEffect)
                PostProcessResult(generated.response(), interactionEffect, generated.relationship())
            }.exceptionally { ex ->
                val root = unwrapCompletionException(ex)
                plugin.logger.warning(
                    "Eroare la post-procesarea dialogului; trimit raspunsul generat: " +
                        "${root.javaClass.simpleName}: ${root.message}"
                )
                if (plugin.config.getBoolean("debug.enabled", false)) {
                    root.printStackTrace()
                }
                PostProcessResult(generated.response(), DialogueInteractionEffect(), generated.relationship())
            }.thenApply { postProcess ->
                if (postProcess == null) {
                    return@thenApply DialogResult.success("Mi-am actualizat amintirile intre timp. Te rog sa repeti intrebarea.")
                }
                updateEmotions(npc, postProcess.relationship(), postProcess.effect())
                DialogResult.success(postProcess.response())
            }
        }.exceptionally { ex ->
            val root = unwrapCompletionException(ex)
            plugin.logger.warning(
                "Eroare in procesarea dialogului: ${root.javaClass.simpleName}: ${root.message}"
            )
            if (plugin.config.getBoolean("debug.enabled", false)) {
                root.printStackTrace()
            }
            DialogResult.error()
        }
    }

    private fun loadStoryFactResponse(message: String, worldContext: WorldContextSnapshot): String? {
        val locationFacts = StoryFactResolver.Facts(
            regionName = worldContext.currentRegion()?.name().orEmpty(),
            placeName = worldContext.currentPlace()?.displayName().orEmpty(),
        )
        if (!StoryFactResolver.isLookupQuestion(message, locationFacts)) return null
        val regionId = worldContext.currentRegion()?.id().orEmpty()
        val placeId = worldContext.currentPlace()?.id().orEmpty()
        val storyService = plugin.storyStateService
        val wantsState = StoryFactResolver.wantsCurrentState(message, locationFacts)
        val regionState = if (!wantsState || regionId.isBlank()) null else try {
            storyService.getRegionState(regionId)
        } catch (error: SQLException) {
            plugin.logger.warning("Starea story a regiunii nu a putut fi citita: ${error.message}")
            null
        }
        val placeState = if (!wantsState || placeId.isBlank()) null else try {
            storyService.getPlaceState(placeId)
        } catch (error: SQLException) {
            plugin.logger.warning("Starea story a locului nu a putut fi citita: ${error.message}")
            null
        }
        val recentEvents = if (!StoryFactResolver.wantsHistory(message) ||
            (regionId.isBlank() && placeId.isBlank())) emptyList() else try {
            storyService.listRecentEvents(regionId, placeId, 2)
        } catch (error: SQLException) {
            plugin.logger.warning("Evenimentele story recente nu au putut fi citite: ${error.message}")
            emptyList()
        }
        val facts = StoryFactResolver.fromWorldContext(worldContext, regionState, placeState, recentEvents)
        return StoryFactResolver.resolve(message, facts)
    }

    private fun processPersonalMemoryAction(
        npc: AINPC,
        playerUuid: UUID,
        playerName: String,
        action: PlayerPersonalMemoryCommand.Action,
    ): CompletableFuture<DialogResult> = plugin.databaseManager.supplyAsync {
        when (action) {
            is PlayerPersonalMemoryCommand.Action.Remember -> when (
                plugin.memoryManager.rememberPlayerStatement(npc, playerUuid, playerName, action.statement)
            ) {
                MemoryManager.PlayerStatementSaveResult.STORED ->
                    DialogResult.success("Am retinut ce mi-ai spus, $playerName.")
                MemoryManager.PlayerStatementSaveResult.ALREADY_STORED ->
                    DialogResult.success("Tin minte deja ca mi-ai spus asta.")
                MemoryManager.PlayerStatementSaveResult.FAILED -> DialogResult.error()
            }
            is PlayerPersonalMemoryCommand.Action.Correct -> when (
                plugin.memoryManager.correctPlayerStatement(npc, playerUuid, action.previousStatement, action.statement)
            ) {
                MemoryManager.PlayerStatementCorrectionResult.UPDATED ->
                    DialogResult.success("Am corectat declaratia pe care mi-ai cerut sa o retin.")
                MemoryManager.PlayerStatementCorrectionResult.NOT_FOUND ->
                    DialogResult.success("Nu am gasit declaratia veche. Spune-mi exact ce mi-ai cerut sa retin.")
                MemoryManager.PlayerStatementCorrectionResult.UNCHANGED ->
                    DialogResult.success("Declaratia este deja memorata astfel.")
                MemoryManager.PlayerStatementCorrectionResult.FAILED -> DialogResult.error()
            }
            PlayerPersonalMemoryCommand.Action.InvalidCorrection ->
                DialogResult.success("Foloseste: corecteaza ce ti-am cerut sa tii minte: text vechi -> text nou.")
            PlayerPersonalMemoryCommand.Action.Forget -> {
                val forgotten = plugin.memoryManager.forgetPlayerStatements(npc, playerUuid)
                when {
                    forgotten == null -> DialogResult.error()
                    forgotten > 0 -> DialogResult.success("Am uitat ce mi-ai cerut sa tin minte.")
                    else -> DialogResult.success("Nu am astfel de informatii de uitat.")
                }
            }
            is PlayerPersonalMemoryCommand.Action.ForgetOne -> {
                val forgotten = plugin.memoryManager.forgetPlayerStatement(npc, playerUuid, action.statement)
                when {
                    forgotten == null -> DialogResult.error()
                    forgotten > 0 -> DialogResult.success("Am uitat doar aceasta declaratie.")
                    else -> DialogResult.success("Nu am gasit aceasta declaratie in memoria mea.")
                }
            }
            PlayerPersonalMemoryCommand.Action.MissingForgetStatement ->
                DialogResult.success("Spune-mi ce declaratie sa uit dupa doua puncte.")
            PlayerPersonalMemoryCommand.Action.InvalidForgetStatement ->
                DialogResult.success("Scrie declaratia exacta pe un singur rand, in cel mult 300 de caractere.")
            PlayerPersonalMemoryCommand.Action.MissingStatement ->
                DialogResult.success("Spune-mi ce sa retin: tine minte ca ...")
            is PlayerPersonalMemoryCommand.Action.TooLong ->
                DialogResult.success("Spune-mi mai pe scurt, in cel mult ${action.maxChars} caractere.")
            PlayerPersonalMemoryCommand.Action.InvalidStatement ->
                DialogResult.success("Pot retine doar o propozitie simpla, pe un singur rand.")
        }
    }.exceptionally { error ->
        val root = unwrapCompletionException(error)
        plugin.logger.warning("Eroare la memoria declarata de jucator: ${root.message}")
        DialogResult.error()
    }

    private fun unwrapCompletionException(error: Throwable): Throwable {
        var current = error
        while (current.cause != null && (
                current is java.util.concurrent.CompletionException ||
                    current is java.util.concurrent.ExecutionException
                )
        ) {
            current = current.cause!!
        }
        return current
    }

    /**
     * Obtine istoricul recent al conversatiei
     */
    fun getRecentHistory(npc: AINPC, player: Player, limit: Int): List<DialogHistory> {
        return getRecentHistory(npc, player.uniqueId, limit)
    }

    fun getRecentHistory(npc: AINPC, playerUuid: UUID, limit: Int): List<DialogHistory> {
        return try {
            dialogHistory.findRecent(npc.databaseId, playerUuid, limit)
        } catch (e: SQLException) {
            plugin.logger.warning("Eroare la obtinerea istoricului: " + e.message)
            emptyList()
        }
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

    fun recordBranchDecision(npc: AINPC, playerUuid: UUID, decision: BranchDecision) {
        plugin.debug("[DialogManager] Branch ${decision.branchId}: propus de ${decision.proposedBy}, " +
            "selectat de ${decision.selectedBy}, motiv: ${decision.reason}")
    }

    /**
     * Obtine relatia dintre NPC si jucator
     */
    fun getRelationship(npc: AINPC, player: Player): NPCRelationship? {
        return getRelationship(npc, player.uniqueId)
    }

    fun getRelationship(npc: AINPC, playerUuid: UUID): NPCRelationship? {
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
                        val rel = NPCRelationship()
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

    fun getRelationshipAsync(npc: AINPC, player: Player): CompletableFuture<NPCRelationship?> {
        val playerUuid = player.uniqueId
        return plugin.databaseManager.supplyAsync { getRelationship(npc, playerUuid) }
    }

    /**
     * Actualizeaza relatia dupa o interactiune
     */
    private fun updateRelationship(
        npc: AINPC,
        playerUuid: UUID,
        playerName: String,
        effect: DialogueInteractionEffect,
    ) {
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
                stmt.setDouble(4, effect.affectionChange)
                stmt.setDouble(5, effect.trustChange)
                stmt.setDouble(6, effect.respectChange)
                stmt.setDouble(7, effect.affectionChange)
                stmt.setDouble(8, effect.trustChange)
                stmt.setDouble(9, effect.respectChange)
                stmt.setDouble(10, effect.affectionChange)
                stmt.setDouble(11, effect.affectionChange)
                stmt.setDouble(12, effect.affectionChange)
                stmt.setDouble(13, effect.affectionChange)
                stmt.setDouble(14, effect.affectionChange)
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
        sentiment: String,
        effect: DialogueInteractionEffect,
    ) {
        val importance = effect.memoryImportance
        if (importance != null) {
            val content = "Jucatorul $playerName mi-a spus: \"${truncate(playerMessage, 100)}\" (sentiment: $sentiment)"

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
    private fun updateEmotions(npc: AINPC, relationship: NPCRelationship?, effect: DialogueInteractionEffect) {
        val interactionType = effect.emotionType ?: return
        val familiarity = relationship?.familiarity ?: 0.0
        val multiplier = 0.5 + familiarity * 0.5

        plugin.emotionManager.applyInteractionEffect(npc, interactionType, multiplier)
    }

    fun isOnCooldown(player: Player, npc: AINPC): Boolean {
        return isOnCooldown(player.uniqueId, npc.uuid)
    }

    // Cooldown methods

    private fun isOnCooldown(playerUuid: UUID, npcUuid: UUID): Boolean {
        val now = System.currentTimeMillis()
        val cooldownMs = plugin.config.getInt("npc.message_cooldown", 2) * 1000L
        val playerCooldowns = cooldowns[playerUuid] ?: return false

        val lastTime = playerCooldowns[npcUuid] ?: return false
        if (now - lastTime < cooldownMs) return true

        playerCooldowns.remove(npcUuid)
        if (playerCooldowns.isEmpty()) {
            cooldowns.remove(playerUuid)
        }
        return false
    }

    private fun checkInteractionProfile(
        npc: AINPC,
        player: Player,
        shopAvailable: Boolean,
        shopAccessDenied: Boolean,
    ): String? {
        return interactionProfileResponses.resolve(
            profile = npc.interactionProfile,
            npcUuid = npc.uuid,
            playerUuid = player.uniqueId,
            playerName = player.name,
            questAvailable = npc.interactionProfile == NpcInteractionProfile.QUEST_ONLY &&
                plugin.scenarioEngine.hasOfferedQuest(player, npc),
            shopAvailable = shopAvailable,
            shopAccessDenied = shopAccessDenied,
        )
    }

    private fun setCooldown(playerUuid: UUID, npcUuid: UUID) {
        val now = System.currentTimeMillis()
        val cooldownMs = plugin.config.getInt("npc.message_cooldown", 2) * 1000L
        val playerCooldowns = cooldowns.computeIfAbsent(playerUuid) { ConcurrentHashMap() }

        playerCooldowns.entries.removeAll { now - it.value >= cooldownMs * 2 }
        if (playerCooldowns.isEmpty()) {
            cooldowns.remove(playerUuid)
            cooldowns.computeIfAbsent(playerUuid) { ConcurrentHashMap() }
        }
        cooldowns[playerUuid]!!.put(npcUuid, now)
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

    private fun generateResponseOnMainThread(
        npc: AINPC,
        player: Player,
        message: String,
        request: DialogRequest,
        context: DialogContext,
        generate: () -> CompletableFuture<String>,
    ): CompletableFuture<String> {
        val future = CompletableFuture<CompletableFuture<String>>()
        val responseTask = Runnable {
            try {
                if (plugin.config.getBoolean("events.public_api_enabled", true) &&
                    plugin.config.getBoolean("events.dialog_prompt_events_enabled", false)
                ) {
                    val summary = "memories=" + context.memories.size + ",history=" + context.history.size
                    val event = DialogAIRequestBuiltEvent(
                        DialogAIRequestBuiltEventPayload(
                            UUID.randomUUID(),
                            System.currentTimeMillis(),
                            AINPCEventSource.NPC,
                            player.uniqueId.toString() + ":" + npc.uuid,
                            player.uniqueId,
                            player.name,
                            npc.databaseId.toString(),
                            npc.uuid,
                            npc.name,
                            message,
                            summary,
                            request.directAddress(),
                            request.explicitConversation(),
                            request.triggerReason(),
                            mapOf("source" to "DialogManager")
                        )
                    )
                    plugin.server.pluginManager.callEvent(event)
                }
                future.complete(generate())
            } catch (failure: RuntimeException) {
                future.completeExceptionally(failure)
            }
        }

        if (plugin.server.isPrimaryThread) {
            responseTask.run()
        } else {
            try {
                plugin.server.scheduler.runTask(plugin, responseTask)
            } catch (failure: RuntimeException) {
                future.completeExceptionally(failure)
            }
        }
        return future.thenCompose { it }
    }

    // Context holder class
    private class DialogContext(
        history: List<DialogHistory>,
        memories: List<String>,
        relationship: NPCRelationship?,
        dbContext: PromptDbContext,
        val playerStatementRevision: Long,
        val storyFactResponse: String?,
    ) {
        val history: List<DialogHistory> = immutableCopy(history)
        val memories: List<String> = immutableCopy(memories)
        val relationship: NPCRelationship? = relationship?.let {
            NPCRelationship(
                affection = it.affection,
                trust = it.trust,
                respect = it.respect,
                familiarity = it.familiarity,
                interactionCount = it.interactionCount,
                relationshipType = it.relationshipType,
            )
        }
        val dbContext: PromptDbContext = dbContext.copy(
            playerConfirmedStatements = immutableCopy(dbContext.playerConfirmedStatements),
        )

        private fun <Value> immutableCopy(values: Collection<Value>): List<Value> =
            java.util.Collections.unmodifiableList(ArrayList(values))
    }

    private data class GeneratedDialog(
        val response: String,
        val relationship: NPCRelationship?,
        val playerStatementRevision: Long,
    ) {
        fun response(): String = response
        fun relationship(): NPCRelationship? = relationship
    }

    private data class PostProcessResult(
        val response: String,
        val effect: DialogueInteractionEffect,
        val relationship: NPCRelationship?
    ) {
        fun response(): String = response
        fun effect(): DialogueInteractionEffect = effect
        fun relationship(): NPCRelationship? = relationship
    }

    data class PromptDbContext(
        val totalMemoryCount: Int,
        val weightedMemoryImpact: Double,
        val regionalReputation: Int? = null,
        val playerProfile: PlayerProfile? = null,
        val playerConfirmedStatements: List<String> = emptyList(),
        val maxPromptStatements: Int = 3,
    ) {
        fun totalMemoryCount(): Int = totalMemoryCount
        fun weightedMemoryImpact(): Double = weightedMemoryImpact
    }

    class DialogResult private constructor(
        val status: DialogStatus,
        val response: String?,
        val branchDecision: BranchDecision? = null
    ) {
        fun isSuccess(): Boolean = status == DialogStatus.SUCCESS

        companion object {
            @JvmStatic
            fun success(response: String): DialogResult = DialogResult(DialogStatus.SUCCESS, response)

            @JvmStatic
            fun successWithBranch(response: String, decision: BranchDecision): DialogResult =
                DialogResult(DialogStatus.SUCCESS, response, decision)

            @JvmStatic
            fun cooldown(): DialogResult = DialogResult(DialogStatus.COOLDOWN, null)

            @JvmStatic
            fun busy(): DialogResult = DialogResult(DialogStatus.BUSY, null)

            @JvmStatic
            fun error(): DialogResult = DialogResult(DialogStatus.ERROR, null)
        }
    }

    enum class DialogStatus {
        SUCCESS,
        COOLDOWN,
        ERROR,
        BUSY
    }

    data class BranchDecision(
        val branchId: String,
        val proposedBy: String,
        val selectedBy: String,
        val reason: String,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        companion object {
            @JvmStatic
            fun aiProposed(branchId: String, reason: String): BranchDecision =
                BranchDecision(branchId, "ai", "runtime", reason)

            @JvmStatic
            fun runtimeSelected(branchId: String, reason: String): BranchDecision =
                BranchDecision(branchId, "runtime", "runtime", reason)
        }
    }

    data class DialogRequest @JvmOverloads constructor(
        val npc: AINPC,
        val player: Player,
        val message: String,
        val directAddress: Boolean,
        val explicitConversation: Boolean,
        val triggerReason: String,
        val nearbyNpcCount: Int,
        val distanceToNpc: Double,
        val addressClarified: Boolean = false,
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
