package ro.ainpc.engine

import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import ro.ainpc.ai.DialogManager
import ro.ainpc.ai.DialogHistory
import ro.ainpc.ai.NPCRelationship
import ro.ainpc.ai.NpcFactResolver
import ro.ainpc.ai.OpenAIService
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NPCContext
import ro.ainpc.npc.NPCEmotions
import java.util.Collections
import java.util.EnumMap
import java.util.Locale
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom

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
            "Un calator nou? Interesant.",
            "Salut, strainule. Cu ce te pot ajuta?",
            "Nu te cunosc, dar bine ai venit."
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
    }

    fun selectIntent(npc: AINPC, context: NPCContext, playerMessage: String): DialogueIntent {
        val emotions: NPCEmotions = npc.emotions
        val dominantEmotion = emotions.dominantEmotion
        val relationStatus = context.relationshipStatus

        if (isOutOfWorldQuestion(playerMessage)) {
            return if (!npc.occupation.isNullOrEmpty()) DialogueIntent.CONFUSED_PROFESSIONAL else DialogueIntent.CONFUSED
        }

        val msg = playerMessage.lowercase()
        if (containsAny(msg, "salut", "buna", "hei", "noroc", "servus")) return selectGreetIntent(relationStatus)
        if (containsAny(msg, "pa", "la revedere", "adio", "pe curand")) return DialogueIntent.FAREWELL
        if (containsAny(msg, "cumpara", "vinde", "pret", "comert", "marfa", "afaceri")) return DialogueIntent.TRADE
        if (containsAny(msg, "ajutor", "ajuta", "nevoie")) return DialogueIntent.HELP
        if (containsAny(msg, "multumesc", "mersi", "apreciez")) return DialogueIntent.THANK
        if (containsAny(msg, "familie", "copii", "sot", "sotie", "parinti")) return DialogueIntent.FAMILY_TALK
        if (containsAny(msg, "munca", "meserie", "job", "profesie")) return DialogueIntent.WORK_TALK
        if (containsAny(msg, "poveste", "povesteste", "intamplat")) return DialogueIntent.STORY
        if (containsAny(msg, "stii", "auzit", "zvon", "barfa")) return DialogueIntent.GOSSIP
        if (containsAny(msg, "misiune", "quest", "sarcina", "ajutor")) return DialogueIntent.QUEST_OFFER

        return when (dominantEmotion.lowercase()) {
            "anger" -> DialogueIntent.ANGRY
            "sadness" -> DialogueIntent.SAD
            "happiness" -> DialogueIntent.HAPPY
            "fear" -> DialogueIntent.SCARED
            else -> DialogueIntent.GREET
        }
    }

    private fun selectGreetIntent(relationStatus: String): DialogueIntent =
        when (relationStatus) {
            "CLOSE_FRIEND", "FRIEND", "FAMILY", "SPOUSE" -> DialogueIntent.GREET_FRIEND
            "STRANGER" -> DialogueIntent.GREET_STRANGER
            else -> DialogueIntent.GREET
        }

    private fun isOutOfWorldQuestion(message: String): Boolean {
        val msg = message.lowercase()
        return containsAny(
            msg,
            "telefon", "internet", "computer", "masina", "avion", "televizor",
            "electricitate", "bitcoin", "social media", "facebook", "instagram",
            "email", "website", "app", "aplicatie", "program", "software"
        )
    }

    private fun containsAny(text: String, vararg words: String): Boolean = words.any { text.contains(it) }

    fun generateResponse(
        request: DialogManager.DialogRequest,
        recentHistory: List<DialogHistory>,
        relevantMemories: List<String>,
        relationship: NPCRelationship?,
        dbContext: DialogManager.PromptDbContext
    ): CompletableFuture<String> {
        val npc = request.npc()
        val context = npc.context ?: NPCContext(npc)
        if (request.player() != null && context.interactingPlayer == null) {
            context.setInteractingPlayer(request.player())
        }
        context.lastPlayerMessage = request.message()

        val factResponse = resolveFactResponse(npc, context, request.message())
        if (factResponse != null) return CompletableFuture.completedFuture(factResponse)

        val intent = selectIntent(npc, context, request.message())
        val template = selectTemplate(npc, intent)
        val processed = processTemplate(template, npc, context)

        if (!shouldUseContextualAI(intent, context, request, recentHistory, relevantMemories, relationship)) {
            return CompletableFuture.completedFuture(processed)
        }

        return openAIService.generateResponse(request, recentHistory, relevantMemories, relationship, dbContext)
            .thenApply { response -> if (response.isNullOrBlank()) processed else response }
            .exceptionally { ex ->
                plugin.debug("Eroare in pipeline-ul contextual de dialog: ${ex.message}")
                processed
            }
    }

    fun generateResponse(npc: AINPC, context: NPCContext, playerMessage: String): CompletableFuture<String> {
        val factResponse = resolveFactResponse(npc, context, playerMessage)
        if (factResponse != null) return CompletableFuture.completedFuture(factResponse)

        val intent = selectIntent(npc, context, playerMessage)
        val template = selectTemplate(npc, intent)
        val processed = processTemplate(template, npc, context)
        val useAI = shouldUseAI(intent, context)
        if (useAI && openAIService.isAvailable) {
            return reformulateWithAI(npc, context, intent, processed, playerMessage)
        }
        return CompletableFuture.completedFuture(processed)
    }

    private fun resolveFactResponse(npc: AINPC, context: NPCContext, playerMessage: String): String? {
        val facts = NpcFactResolver.NpcFacts(
            npc.name,
            npc.occupation ?: "",
            npc.emotions?.getShortDescription() ?: "",
            npc.currentState?.displayName ?: "",
            NpcFactResolver.describeCurrentActivity(npc.occupation, npc.currentState),
            NpcFactResolver.describeLocation(npc, context)
        )
        return NpcFactResolver.resolve(playerMessage, facts).orElse(null)
    }

    private fun selectTemplate(npc: AINPC, intent: DialogueIntent): String {
        var intentTemplates = templates[intent]
        if (intentTemplates.isNullOrEmpty()) intentTemplates = templates[DialogueIntent.GREET]
        val recent = recentResponses.computeIfAbsent(npc.uuid) { Collections.synchronizedList(mutableListOf()) }
        val selected: String
        synchronized(recent) {
            var available = intentTemplates!!.filter { !recent.contains(it) }
            if (available.isEmpty()) {
                recent.clear()
                available = intentTemplates
            }
            selected = available[ThreadLocalRandom.current().nextInt(available.size)]
            recent.add(selected)
            if (recent.size > 5) recent.removeAt(0)
        }
        return selected
    }

    private fun processTemplate(template: String, npc: AINPC, context: NPCContext): String {
        var result = template
        val interactingPlayer = context.interactingPlayer
        if (interactingPlayer != null) {
            result = result.replace("{player}", interactingPlayer.name)
        }
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
        context: NPCContext,
        request: DialogManager.DialogRequest,
        recentHistory: List<DialogHistory>?,
        relevantMemories: List<String>?,
        relationship: NPCRelationship?
    ): Boolean {
        if (!openAIService.isAvailable) return false
        if (shouldUseAI(intent, context)) return true
        if (!relevantMemories.isNullOrEmpty()) return true
        if (recentHistory != null && recentHistory.size >= 2) return true
        if (relationship != null && relationship.interactionCount >= 3) return true
        val message = request.message()?.trim() ?: ""
        return request.explicitConversation() && message.length > 8
    }

    private fun shouldUseAI(intent: DialogueIntent, context: NPCContext): Boolean =
        when (intent) {
            DialogueIntent.STORY,
            DialogueIntent.GOSSIP,
            DialogueIntent.CONFUSED,
            DialogueIntent.CONFUSED_PROFESSIONAL,
            DialogueIntent.QUEST_OFFER -> true
            else -> {
                val lastMessage = context.lastPlayerMessage
                lastMessage != null && lastMessage.length > 20
            }
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

    enum class DialogueIntent(val description: String) {
        GREET("Salut simplu"),
        GREET_FRIEND("Salut prieten"),
        GREET_STRANGER("Salut strain"),
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
