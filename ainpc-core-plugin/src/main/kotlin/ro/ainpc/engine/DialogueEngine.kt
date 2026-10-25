@file:Suppress("SENSELESS_COMPARISON")
package ro.ainpc.engine

import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import ro.ainpc.ai.DialogManager
import ro.ainpc.ai.DialogHistory
import ro.ainpc.ai.NPCRelationship
import ro.ainpc.ai.NpcFactResolver
import ro.ainpc.ai.NpcCognitiveResponse
import ro.ainpc.ai.EnvironmentFactResolver
import ro.ainpc.ai.NpcShopFactResolver
import ro.ainpc.ai.NpcShopAssociation
import ro.ainpc.ai.NpcWorkFactResponse
import ro.ainpc.economy.NpcShopDefinition
import ro.ainpc.ai.OpenAIService
import ro.ainpc.ai.OpenAISemanticWorldContextBuilder
import ro.ainpc.ai.PlayerRecognition
import ro.ainpc.ai.PlayerProfileGreeting
import ro.ainpc.ai.PlayerIdentityResponse
import ro.ainpc.ai.PlayerConversationRecall
import ro.ainpc.ai.PlayerConversationContinuation
import ro.ainpc.ai.orchestration.AIOrchestrationRequest
import ro.ainpc.ai.orchestration.AIResultStatus
import ro.ainpc.ai.orchestration.AIUseCase
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCContext
import ro.ainpc.npc.NpcCognitiveState
import java.util.Collections
import java.util.EnumMap
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom

/**
 * Dialogueengine.
 */
class DialogueEngine(
    private val plugin: AINPCPlugin,
    private val openAIService: OpenAIService
) {
    private val templates: MutableMap<DialogueIntent, List<String>> = EnumMap(DialogueIntent::class.java)
    private val recentResponses: MutableMap<UUID, MutableList<String>> = ConcurrentHashMap()

    init {
        loadTemplates()
    }

    private fun loadTemplates() {
        templates[DialogueIntent.GREET] = listOf(
            "Buna ziua, {player}!",
            "Salut! Ce te aduce pe aici?",
            "A, {player}! Ma bucur sa te vad.",
            "Bine ai venit, calatorule!",
            "Hei! Cum mai esti?",
            "Noroc, {player}! E o zi frumoasa, nu-i asa?"
        )
        templates[DialogueIntent.GREET_FRIEND] = listOf(
            "Ah, {player}! Ce bine ca te vad din nou!",
            "Prietenul meu! Cum ai fost?",
            "{player}! Tocmai ma gandeam la tine!",
            "Hei, vechiul meu prieten! Intra, intra!",
            "Ce surpriza placuta! {player} in persoana!"
        )
        templates[DialogueIntent.GREET_STRANGER] = listOf(
            "Hmm? Cine esti tu?",
            "Nu cred ca ne-am mai intalnit...",
            "Ai ajuns de curand pe aici?",
            "Salut! Cu ce te pot ajuta?",
            "Nu cred ca am mai vorbit. Bine ai venit."
        )
        templates[DialogueIntent.GREET_GUARDED] = listOf(
            "Buna, {player}. Cu ce te pot ajuta?",
            "Te ascult, {player}. Spune-mi ce doresti.",
            "Putem vorbi, dar as prefera sa fim directi."
        )
        templates[DialogueIntent.FAREWELL] = listOf(
            "Pa, {player}! Drum bun!",
            "Ne vedem curand!",
            "Ai grija de tine!",
            "La revedere! Sa mai treci pe aici!",
            "Ramas bun, calatorule!"
        )
        templates[DialogueIntent.TRADE] = listOf(
            "Ce doresti sa cumperi?",
            "Am marfa buna azi!",
            "Hai sa vedem ce am pe aici...",
            "Preturi bune pentru prieteni!",
            "Ai venit sa faci afaceri? Perfect!"
        )
        templates[DialogueIntent.WARN] = listOf(
            "Ai grija! Am auzit ca e periculos pe aici.",
            "Fii atent, {player}. Nu e totul asa cum pare.",
            "Ti-as recomanda sa nu mergi singur noaptea...",
            "Am vazut creaturi ciudate prin padure. Fii prudent!",
            "Daca ma intrebi pe mine, as sta departe de acolo."
        )
        templates[DialogueIntent.HELP] = listOf(
            "Cu ce te pot ajuta?",
            "Ai nevoie de ceva?",
            "Spune-mi cum te pot ajuta.",
            "Sunt aici daca ai nevoie de mine.",
            "Ce pot face pentru tine?"
        )
        templates[DialogueIntent.THANK] = listOf(
            "Multumesc frumos, {player}!",
            "Apreciez foarte mult!",
            "Esti prea bun cu mine!",
            "Nu stiu cum sa-ti multumesc!",
            "Chiar mi-ai facut ziua mai buna!"
        )
        templates[DialogueIntent.ANGRY] = listOf(
            "Ce vrei?! Nu am timp de prostii!",
            "Lasa-ma in pace!",
            "Nu sunt dispus sa vorbesc acum.",
            "Hmph! De ce ar trebui sa-ti raspund?",
            "Pleaca de aici inainte sa ma enervezi mai tare!"
        )
        templates[DialogueIntent.SAD] = listOf(
            "*oftez* Nu e cea mai buna zi pentru mine...",
            "Ma simt cam prost azi...",
            "Scuza-ma, nu prea am chef de vorba...",
            "Viata e grea uneori, stii?",
            "Am trecut prin momente mai bune..."
        )
        templates[DialogueIntent.HAPPY] = listOf(
            "Ce zi minunata! Cum esti, {player}?",
            "Ma simt fantastic azi!",
            "Totul merge perfect! Si pentru tine?",
            "Viata e frumoasa, nu-i asa?",
            "Sunt atat de fericit/fericita ca te vad!"
        )
        templates[DialogueIntent.SCARED] = listOf(
            "C-cine e acolo?! A, tu esti, {player}...",
            "M-ai speriat! Ce vrei?",
            "Nu e sigur aici... ar trebui sa plecam...",
            "Ai auzit zgomotul ala? Ce a fost?",
            "Nu-mi place deloc atmosfera asta..."
        )
        templates[DialogueIntent.WORK_TALK] = listOf(
            "Munca merge bine azi.",
            "Sunt ocupat cu treburile, dar pot vorbi putin.",
            "Ai nevoie de serviciile mele?",
            "Meseria mea e {occupation}. Cu ce te pot ajuta?",
            "Am mult de lucru, dar pentru tine fac timp."
        )
        templates[DialogueIntent.FAMILY_TALK] = listOf(
            "Familia mea e totul pentru mine.",
            "Ai intrebat de {family_member}? E bine, multumesc!",
            "Copiii cresc repede... timpul zboara.",
            "Sotul/Sotia mea e cea mai buna persoana pe care o cunosc.",
            "Familia e cel mai important lucru in viata."
        )
        templates[DialogueIntent.GOSSIP] = listOf(
            "Ai auzit ce s-a intamplat cu {npc_name}?",
            "Nu spune nimanui, dar am auzit ca...",
            "Intre noi fie vorba...",
            "Stii ce se zice prin sat?",
            "Am aflat ceva interesant zilele trecute..."
        )
        templates[DialogueIntent.STORY] = listOf(
            "Lasa-ma sa-ti povestesc ceva...",
            "Mi-am amintit de o intamplare...",
            "Odata, cand eram tanar/tanara...",
            "Stii legenda despre acest loc?",
            "Hai sa-ti spun o poveste interesanta..."
        )
        templates[DialogueIntent.CONFUSED] = listOf(
            "Ce e aia? Nu inteleg...",
            "Hmm, nu am auzit niciodata de asa ceva.",
            "Vorbesti in ghicitori? Explica-mi.",
            "E un fel de... magie? Nu pricep.",
            "Suna ciudat. Ce vrei sa zici?"
        )
        templates[DialogueIntent.CONFUSED_PROFESSIONAL] = listOf(
            "Ce e aia? Un fel de {professional_item}?",
            "Nu stiu ce inseamna, dar suna ca {professional_guess}.",
            "In meseria mea nu folosim asa ceva...",
            "Daca e ca un {professional_tool}, poate pot ajuta.",
            "Nu sunt sigur, dar pot incerca sa-ti fac ceva similar."
        )
        templates[DialogueIntent.QUEST_OFFER] = listOf(
            "Ai putea sa ma ajuti cu ceva?",
            "Am o problema si cred ca tu esti persoana potrivita.",
            "Daca ai timp, am nevoie de ajutorul tau.",
            "Te-ai oferi sa faci ceva pentru mine?",
            "Am o sarcina care s-ar potrivi aventurierului ca tine."
        )
        templates[DialogueIntent.REFUSE] = listOf(
            "Nu, multumesc.",
            "Nu pot face asta.",
            "Imi pare rau, dar nu.",
            "Nu e posibil acum.",
            "Refuz sa fac asta."
        )
        templates[DialogueIntent.AGREE] = listOf(
            "Da, sigur!",
            "Bineinteles!",
            "Cu placere!",
            "Consider ca e o idee buna.",
            "Sunt de acord!"
        )
        templates[DialogueIntent.QUESTION] = listOf(
            "Nu stiu sigur raspunsul. Ce detalii mai ai?",
            "Nu vreau sa te induc in eroare. Poti sa-mi spui mai exact ce cauti?",
            "Nu am destule informatii despre asta. Ce anume te intereseaza?"
        )
        templates[DialogueIntent.CONVERSATION] = listOf(
            "Te ascult. Spune-mi mai mult.",
            "Sunt aici, te ascult.",
            "Inteleg ce spui. Ce s-a intamplat mai departe?",
            "Pare important pentru tine. Cum pot sa te ajut?"
        )
    }

    @JvmOverloads
    fun selectIntent(
        npc: AINPC,
        context: NPCContext,
        playerMessage: String,
        recognition: PlayerRecognition? = null
    ): DialogueIntent = DialogueIntentSelector.select(
        playerMessage, context.relationshipStatus, !npc.occupation.isNullOrBlank(), recognition,
        listOfNotNull(npc.name, npc.displayName),
    )

    @JvmOverloads
    fun generateResponse(
        request: DialogManager.DialogRequest,
        recentHistory: List<DialogHistory>,
        relevantMemories: List<String>,
        relationship: NPCRelationship?,
        dbContext: DialogManager.PromptDbContext,
        accessibleShops: List<NpcShopDefinition>,
        shopAccessDenied: Boolean,
        workFacts: NpcWorkFactResponse.Facts? = null,
        storyFactResponse: String? = null,
    ): CompletableFuture<String> {
        val conversationHistory = DialogHistory.recentStable(recentHistory)
        val npc = request.npc()
        val context = npc.context ?: NPCContext(npc)
        val recallResponse = PlayerConversationRecall.resolve(request.message(), conversationHistory)
        if (recallResponse != null) return CompletableFuture.completedFuture(recallResponse)
        val contextualRecallFallback = PlayerConversationRecall.resolveForFallback(request.message(), conversationHistory)
        if (contextualRecallFallback != null) {
            return generateContextualReply(
                request, conversationHistory, relevantMemories, relationship, dbContext, contextualRecallFallback,
            )
        }

        val factResponse = resolveFactResponse(npc, context, request.message(), request.player().uniqueId)
        val semanticFactResponse = if (factResponse == null) {
            OpenAISemanticWorldContextBuilder.resolveProfessionFact(plugin, npc, request.message())
        } else {
            null
        }
        val shopFactResponse = resolveShopFactResponse(npc, request.message(), accessibleShops, shopAccessDenied)

        val recognition = PlayerRecognition.from(
            relationship,
            conversationHistory.size,
            dbContext.totalMemoryCount,
            dbContext.regionalReputation,
            confirmedFoodHelp = npc.socialExperience.latestFor(request.player().uniqueId, System.currentTimeMillis()) != null,
        )
        val identityResponse = PlayerIdentityResponse.resolve(
            request.message(), request.player().name, recognition, dbContext.playerProfile,
            dbContext.playerConfirmedStatements,
        )
        val factualResponses = listOfNotNull(factResponse ?: semanticFactResponse, storyFactResponse, shopFactResponse, identityResponse)
        if (factualResponses.isNotEmpty()) {
            return generateGroundedResponse(
                request, conversationHistory, relevantMemories, relationship, dbContext,
                factualResponses.joinToString(" "),
                shopFactResponse != null || identityResponse != null,
            )
        }

        val deterministicIntent = selectIntent(npc, context, request.message(), recognition)
        val contextualAiNeeded = shouldUseContextualAI(
            deterministicIntent, request, conversationHistory, relevantMemories, relationship
        )
        return resolveAmbiguousIntent(
            request.message(), deterministicIntent, contextualAiNeeded, accessibleShops, workFacts
        )
            .thenCompose { intent ->
                val groundedReply = when (intent) {
                    DialogueIntent.TRADE -> NpcShopFactResolver.tradeSummary(accessibleShops, shopAccessDenied)
                    DialogueIntent.WORK_TALK -> workFacts?.let { facts ->
                        NpcWorkFactResponse.resolve(facts, conversationHistory.map { it.npcResponse })
                    }
                    else -> null
                }
                if (groundedReply != null) {
                    return@thenCompose generateGroundedResponse(
                        request, conversationHistory, relevantMemories, relationship, dbContext,
                        groundedReply, intent == DialogueIntent.TRADE,
                    )
                }
                val resolvedIntent = if (intent != deterministicIntent) deterministicIntent else intent
                val greetingTemplates = if (resolvedIntent in setOf(
                        DialogueIntent.GREET,
                        DialogueIntent.GREET_FRIEND,
                        DialogueIntent.GREET_STRANGER,
                        DialogueIntent.GREET_GUARDED,
                    )
                ) {
                    PlayerProfileGreeting.templates(dbContext.playerProfile, recognition)
                } else {
                    null
                }
                val continuationTemplates = PlayerConversationContinuation.templates(request.message(), conversationHistory)
                val template = selectTemplate(npc, resolvedIntent, continuationTemplates ?: greetingTemplates)
                val processed = processTemplate(template, npc, context, request.player().name)
                val localFallback = PlayerConversationRecall.resolveForFallback(request.message(), conversationHistory)
                    ?: processed

                if (!contextualAiNeeded) {
                    return@thenCompose CompletableFuture.completedFuture(localFallback)
                }

                generateContextualReply(
                    request, conversationHistory, relevantMemories, relationship, dbContext, localFallback,
                )
            }
    }

    private fun generateContextualReply(
        request: DialogManager.DialogRequest,
        recentHistory: List<DialogHistory>,
        relevantMemories: List<String>,
        relationship: NPCRelationship?,
        dbContext: DialogManager.PromptDbContext,
        localFallback: String,
    ): CompletableFuture<String> {
        if (!openAIService.canGenerateContextualResponse) return CompletableFuture.completedFuture(localFallback)
        return openAIService.generateResponse(request, recentHistory, relevantMemories, relationship, dbContext)
            .thenApply { response -> if (response.isNullOrBlank()) localFallback else response }
            .exceptionally { error ->
                plugin.debug("Eroare in pipeline-ul contextual de dialog: ${error.message}")
                localFallback
            }
    }

    private fun generateGroundedResponse(
        request: DialogManager.DialogRequest,
        recentHistory: List<DialogHistory>,
        relevantMemories: List<String>,
        relationship: NPCRelationship?,
        dbContext: DialogManager.PromptDbContext,
        groundedReply: String,
        exactRequired: Boolean,
    ): CompletableFuture<String> {
        if (exactRequired ||
            !plugin.config.getBoolean("ai.dialogue.ground_facts_with_ai", true) ||
            !openAIService.canGenerateContextualResponse
        ) return CompletableFuture.completedFuture(groundedReply)

        return openAIService.generateResponse(
            request, recentHistory, relevantMemories, relationship, dbContext, groundedReply,
        ).handle { response, error ->
            if (error != null || response.isNullOrBlank()) groundedReply else response
        }
    }

    private fun resolveAmbiguousIntent(
        playerMessage: String,
        deterministicIntent: DialogueIntent,
        contextualAiNeeded: Boolean,
        accessibleShops: List<NpcShopDefinition>,
        workFacts: NpcWorkFactResponse.Facts?,
    ): CompletableFuture<DialogueIntent> {
        if (!plugin.config.getBoolean("ai.orchestration.intent_classification.enabled", false) ||
            !DialogueAiIntentClassification.shouldClassify(deterministicIntent, playerMessage, contextualAiNeeded)
        ) {
            return CompletableFuture.completedFuture(deterministicIntent)
        }
        val orchestrator = plugin.aiOrchestrationService
        if (!orchestrator.enabled()) return CompletableFuture.completedFuture(deterministicIntent)
        val hasAccessibleShop = NpcShopFactResolver.hasListedGoods(accessibleShops)
        val hasOccupation = !workFacts?.occupation.isNullOrBlank()
        val request = AIOrchestrationRequest(
            AIUseCase.INTENT_CLASSIFICATION, "", "", "dialogue",
            mapOf(
                "player_message" to playerMessage,
                "has_accessible_shop" to hasAccessibleShop.toString(),
                "has_occupation" to hasOccupation.toString(),
            )
        )
        val classification = runCatching { orchestrator.orchestrateAsync(request) }
            .getOrElse { return CompletableFuture.completedFuture(deterministicIntent) }
        return classification.handle { result, error ->
            if (error != null || result?.status() != AIResultStatus.SUCCESS) {
                deterministicIntent
            } else {
                DialogueAiIntentClassification.parse(result.message(), hasAccessibleShop, hasOccupation)
                    ?: deterministicIntent
            }
        }
    }

    fun generateResponse(npc: AINPC, context: NPCContext, playerMessage: String): CompletableFuture<String> {
        val factResponse = resolveFactResponse(npc, context, playerMessage)
        if (factResponse != null) return CompletableFuture.completedFuture(factResponse)
        val shopFactResponse = resolveShopFactResponse(npc, playerMessage)
        if (shopFactResponse != null) return CompletableFuture.completedFuture(shopFactResponse)

        val intent = selectIntent(npc, context, playerMessage)
        val template = selectTemplate(npc, intent)
        val processed = processTemplate(template, npc, context)
        val useAI = shouldUseAI(intent, playerMessage)
        if (useAI && openAIService.canGenerateContextualResponse) {
            return reformulateWithAI(npc, context, intent, processed, playerMessage)
        }
        return CompletableFuture.completedFuture(processed)
    }

    private fun resolveShopFactResponse(
        npc: AINPC,
        playerMessage: String,
        accessibleShops: List<NpcShopDefinition>? = null,
        shopAccessDenied: Boolean = false,
    ): String? {
        if (!NpcShopFactResolver.isLookupQuestion(playerMessage)) return null
        val shops = accessibleShops ?: NpcShopAssociation.findShops(plugin.shopService, npc, plugin.platform.worldAdmin)
        val workPlaceName = if (NpcShopFactResolver.requiresWorkPlace(playerMessage)) {
            OpenAISemanticWorldContextBuilder
                .resolveWorkPlace(plugin.platform.worldAdmin, npc.workAnchor)?.displayName()
        } else {
            null
        }
        return NpcShopFactResolver.resolve(playerMessage, shops, workPlaceName, shopAccessDenied)
    }

    private fun resolveFactResponse(
        npc: AINPC, context: NPCContext, playerMessage: String, playerUuid: UUID? = null,
    ): String? {
        val facts = NpcFactResolver.NpcFacts(
            npc.name,
            npc.occupation ?: "",
            npc.emotions?.getShortDescription() ?: "",
            npc.currentState?.displayName ?: "",
            NpcFactResolver.describeCurrentActivity(npc.occupation, npc.currentState),
            NpcFactResolver.describeLocation(npc, context),
            npc.plannedRoutineActivity,
            NpcFactResolver.describePersonality(npc.personality),
        )
        val npcFact = NpcFactResolver.resolve(playerMessage, facts).orElse(null)
        val helperExperience = playerUuid?.let {
            npc.socialExperience.descriptionFor(it, System.currentTimeMillis())
        }.orEmpty()
        val cognitiveFact = NpcCognitiveResponse.resolve(
            playerMessage, NpcCognitiveState.capture(npc, context), helperExperience,
            allowFoodRequest = playerUuid?.let {
                !npc.foodRequestPreferences.isDeferred(it, System.currentTimeMillis())
            } ?: false,
        )
        val environmentFact = EnvironmentFactResolver.resolve(playerMessage, context.environmentContext)
        return listOfNotNull(npcFact, cognitiveFact, environmentFact).joinToString(" ").takeIf { it.isNotBlank() }
    }

    fun clearRecentResponses() {
        recentResponses.clear()
    }

    fun cleanRecentResponses(activeNpcUuids: Set<UUID>) {
        recentResponses.keys.removeAll { it !in activeNpcUuids }
    }

    fun removeNpcResponses(uuid: UUID) {
        recentResponses.remove(uuid)
    }

    private fun selectTemplate(npc: AINPC, intent: DialogueIntent, alternatives: List<String>? = null): String {
        var intentTemplates = alternatives ?: templates[intent]
        if (intentTemplates.isNullOrEmpty()) intentTemplates = templates[DialogueIntent.GREET]
        if (alternatives == null) {
            intentTemplates = DialoguePersonalityPolicy.variants(intent, npc.personality, intentTemplates!!)
        }
        val recent = recentResponses.computeIfAbsent(npc.uuid) { Collections.synchronizedList(mutableListOf()) }
        return DialogueTemplateRotation.choose(intentTemplates!!, recent)
    }

    private fun processTemplate(template: String, npc: AINPC, context: NPCContext, playerName: String? = null): String {
        var result = template
        val currentPlayerName = playerName ?: context.interactingPlayer?.name ?: "calatorule"
        result = result.replace("{player}", currentPlayerName)
        result = result.replace("{occupation}", npc.occupation ?: "locuitor")
        result = result.replace("{name}", npc.name)

        result = result.replace("{professional_item}", "obiect util")
        result = result.replace("{professional_guess}", "ceva interesant")
        result = result.replace("{professional_tool}", "unealta")
        result = result.replace("{family_member}", getRandomFamilyMember())
        result = result.replace("{npc_name}", "vecinul")
        return result
    }

    private fun getRandomFamilyMember(): String {
        val members = arrayOf("sotul meu", "sotia mea", "copiii", "mama", "tata", "fratele meu", "sora mea")
        return members[ThreadLocalRandom.current().nextInt(members.size)]
    }

    private fun shouldUseContextualAI(
        intent: DialogueIntent,
        request: DialogManager.DialogRequest,
        recentHistory: List<DialogHistory>?,
        relevantMemories: List<String>?,
        relationship: NPCRelationship?
    ): Boolean {
        if (!openAIService.canGenerateContextualResponse) return false
        if (PlayerConversationContinuation.needsContext(request.message(), recentHistory.orEmpty())) return true
        if (shouldUseAI(intent, request.message())) return true
        if (!relevantMemories.isNullOrEmpty()) return true
        if (recentHistory != null && recentHistory.size >= 2) return true
        if (relationship != null && relationship.interactionCount >= 3) return true
        return request.explicitConversation()
    }

    private fun shouldUseAI(intent: DialogueIntent, playerMessage: String): Boolean =
        when (intent) {
            DialogueIntent.STORY,
            DialogueIntent.GOSSIP,
            DialogueIntent.CONFUSED,
            DialogueIntent.CONFUSED_PROFESSIONAL,
            DialogueIntent.QUEST_OFFER -> true
            else -> playerMessage.length > 20
        }

    private fun reformulateWithAI(
        npc: AINPC,
        context: NPCContext,
        intent: DialogueIntent,
        template: String,
        playerMessage: String
    ): CompletableFuture<String> {
        val prompt = StringBuilder()
        prompt.append("Esti un NPC intr-un server Minecraft configurabil prin addonuri.\n")
        prompt.append(npc.generateContextDescription()).append("\n")
        prompt.append(context.generateContextDescription()).append("\n")
        prompt.append("\nIntentia dialogului: ").append(intent.description).append("\n")
        prompt.append("Template de baza: ").append(template).append("\n")
        prompt.append("\nJucatorul a spus: ").append(playerMessage).append("\n")
        prompt.append("\nReformuleaza template-ul pastrand sensul dar facandu-l mai natural si specific personajului.")
        prompt.append(" Raspunde DOAR cu replica NPC-ului, maxim 2 propozitii, in romana.")

        return openAIService.generateAsync(prompt.toString())
            .thenApply { response -> if (response.isNullOrEmpty()) template else response }
            .exceptionally { e ->
                plugin.debug("Eroare AI reformulare: ${e.message}")
                template
            }
    }

    fun generateQuickResponse(npc: AINPC, context: NPCContext, intent: DialogueIntent): String {
        val template = selectTemplate(npc, intent)
        return processTemplate(template, npc, context)
    }

/**
 * Dialogueintent.
 */
    enum class DialogueIntent(val description: String) {
        GREET("Salut simplu"),
        GREET_FRIEND("Salut prieten"),
        GREET_STRANGER("Salut strain"),
        GREET_GUARDED("Salut prudent"),
        QUESTION("Intrebare fara raspuns confirmat"),
        CONVERSATION("Conversatie deschisa"),
        FAREWELL("La revedere"),
        TRADE("Comert"),
        WARN("Avertizare"),
        HELP("Oferire ajutor"),
        THANK("Multumire"),
        ANGRY("Raspuns furios"),
        SAD("Raspuns trist"),
        HAPPY("Raspuns fericit"),
        SCARED("Raspuns speriat"),
        WORK_TALK("Despre munca"),
        FAMILY_TALK("Despre familie"),
        GOSSIP("Barfa"),
        STORY("Povestire"),
        CONFUSED("Confuz - nu intelege"),
        CONFUSED_PROFESSIONAL("Confuz - interpreteaza prin profesie"),
        QUEST_OFFER("Ofera misiune"),
        REFUSE("Refuz"),
        AGREE("Acord");
    }
}
