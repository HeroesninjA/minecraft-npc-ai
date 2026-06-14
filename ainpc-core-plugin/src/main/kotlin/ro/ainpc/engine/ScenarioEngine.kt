package ro.ainpc.engine

import com.google.gson.Gson
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import ro.ainpc.npc.AINPC
import ro.ainpc.engine.FeaturePackLoader.ProfessionDefinition
import ro.ainpc.engine.FeaturePackLoader.QuestEntryDefinition
import ro.ainpc.engine.FeaturePackLoader.ScenarioDefinition
import ro.ainpc.engine.QuestAnchorResolver.ResolvedQuestAnchors
import ro.ainpc.engine.QuestScenarioContract.Category
import ro.ainpc.story.StoryContextService
import ro.ainpc.world.WorldNode
import ro.ainpc.world.WorldPlace
import ro.ainpc.world.WorldRegion
import java.lang.reflect.Type
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class ScenarioEngine(private val plugin: AINPCPlugin) {
    private val scenarioTemplates = LinkedHashMap<ScenarioType, ScenarioTemplate>()
    private val questTemplates = LinkedHashMap<String, ScenarioTemplate>()
    private val gson = Gson()
    private val activePlayerQuests = ConcurrentHashMap<UUID, MutableMap<String, PlayerQuestProgress>>()
    private val archivedPlayerQuests = ConcurrentHashMap<UUID, MutableMap<String, PlayerQuestProgress>>()
    private val questCompletionLocks = ConcurrentHashMap.newKeySet<String>()
    val questDefinitions = ArrayList<ScenarioDefinition>()
    var storyContextService: StoryContextService? = null
    private val trackedQuestPlayers = HashSet<UUID>()
    private val trackedQuestTemplates = ConcurrentHashMap<UUID, String>()
    private val activeScenarios = HashMap<UUID, ActiveScenario>()

    init { loadScenarioTemplates() }

    fun reloadTemplates() {
        TODO()
    }
    fun flushQuestProgress() {
        TODO()
    }
    private fun loadScenarioTemplates() {
        scenarioTemplates.clear()
        questTemplates.clear()

        val theft = ScenarioTemplate(ScenarioType.THEFT)
        theft.addRole("THIEF", "Hotul care fura")
        theft.addRole("VICTIM", "Victima furtului")
        theft.addRole("WITNESS", "Martor la furt", true)
        theft.addRole("RESPONDER", "NPC care intervine", true)
        theft.addPhase("PLANNING", "Hotul planuieste furtul")
        theft.addPhase("EXECUTION", "Furtul are loc")
        theft.addPhase("DISCOVERY", "Victima descopera furtul")
        theft.addPhase("CONFLICT", "Confruntare intre parti")
        theft.addPhase("RESOLUTION", "Rezolvare - cineva intervine sau hotul fuge")
        theft.triggerProbability = 0.05
        theft.minimumNpcCount = 2
        scenarioTemplates[ScenarioType.THEFT] = theft

        val conflict = ScenarioTemplate(ScenarioType.CONFLICT)
        conflict.addRole("AGGRESSOR", "Cel care incepe conflictul")
        conflict.addRole("DEFENDER", "Cel care se apara")
        conflict.addRole("MEDIATOR", "Cel care incearca sa medieze", true)
        conflict.addRole("SPECTATOR", "Spectatori", true)
        conflict.addPhase("TENSION", "Tensiune initiala")
        conflict.addPhase("ARGUMENT", "Cearta verbala")
        conflict.addPhase("ESCALATION", "Escaladare optionala")
        conflict.addPhase("RESOLUTION", "Rezolvare - pace sau lupta")
        conflict.triggerProbability = 0.08
        conflict.minimumNpcCount = 2
        scenarioTemplates[ScenarioType.CONFLICT] = conflict

        val celebration = ScenarioTemplate(ScenarioType.CELEBRATION)
        celebration.addRole("HOST", "Gazda sarbatorii")
        celebration.addRole("GUEST", "Invitati", true)
        celebration.addRole("ENTERTAINER", "Cel care anima atmosfera", true)
        celebration.addPhase("GATHERING", "Lumea se strange")
        celebration.addPhase("CELEBRATION", "Sarbatoarea propriu-zisa")
        celebration.addPhase("PEAK", "Momentul culminant")
        celebration.addPhase("ENDING", "Sfarsitul sarbatorii")
        celebration.triggerProbability = 0.03
        celebration.minimumNpcCount = 2
        scenarioTemplates[ScenarioType.CELEBRATION] = celebration

        val emergency = ScenarioTemplate(ScenarioType.EMERGENCY)
        emergency.addRole("VICTIM", "Cel in pericol")
        emergency.addRole("HELPER", "Cel care ajuta")
        emergency.addRole("COWARD", "Cel care fuge", true)
        emergency.addRole("LEADER", "Cel care organizeaza", true)
        emergency.addPhase("ALERT", "Alerta initiala")
        emergency.addPhase("PANIC", "Panica generala")
        emergency.addPhase("RESPONSE", "Raspunsul comunitatii")
        emergency.addPhase("RESOLUTION", "Rezolvare")
        emergency.triggerProbability = 0.02
        emergency.minimumNpcCount = 2
        scenarioTemplates[ScenarioType.EMERGENCY] = emergency

        val romance = ScenarioTemplate(ScenarioType.ROMANCE)
        romance.addRole("SUITOR", "Curtezanul")
        romance.addRole("BELOVED", "Persoana iubita")
        romance.addRole("RIVAL", "Rival in dragoste", true)
        romance.addRole("CONFIDANT", "Prieten confident", true)
        romance.addPhase("ATTRACTION", "Atractie initiala")
        romance.addPhase("COURTSHIP", "Curte")
        romance.addPhase("COMPLICATION", "Complicatii")
        romance.addPhase("RESOLUTION", "Rezolvare")
        romance.triggerProbability = 0.04
        romance.minimumNpcCount = 2
        scenarioTemplates[ScenarioType.ROMANCE] = romance

        val tradeDeal = ScenarioTemplate(ScenarioType.TRADE_DEAL)
        tradeDeal.addRole("SELLER", "Vanzatorul")
        tradeDeal.addRole("BUYER", "Cumparatorul")
        tradeDeal.addRole("COMPETITOR", "Competitor", true)
        tradeDeal.addPhase("NEGOTIATION", "Negociere")
        tradeDeal.addPhase("BARGAINING", "Tocmeala")
        tradeDeal.addPhase("AGREEMENT", "Acord sau esec")
        tradeDeal.triggerProbability = 0.10
        tradeDeal.minimumNpcCount = 2
        scenarioTemplates[ScenarioType.TRADE_DEAL] = tradeDeal

        val quest = ScenarioTemplate(ScenarioType.QUEST)
        quest.addRole("QUEST_GIVER", "Cel care da misiunea")
        quest.addPlayerRole("HERO", "Eroul (jucatorul)")
        quest.addRole("HELPER", "Ajutor pentru erou", true)
        quest.addRole("ANTAGONIST", "Antagonistul", true)
        quest.addPhase("INTRODUCTION", "Prezentarea problemei")
        quest.addPhase("ACCEPTANCE", "Acceptarea misiunii")
        quest.addPhase("JOURNEY", "Calatoria/actiunea")
        quest.addPhase("COMPLETION", "Finalizare si recompensa")
        quest.triggerProbability = 0.06
        quest.minimumNpcCount = 1
        quest.requiresPlayer = true
        scenarioTemplates[ScenarioType.QUEST] = quest

        val gossip = ScenarioTemplate(ScenarioType.GOSSIP_SPREAD)
        gossip.addRole("ORIGINATOR", "Sursa zvonului")
        gossip.addRole("SPREADER", "Cel care raspandeste")
        gossip.addRole("SUBJECT", "Subiectul zvonului", true)
        gossip.addRole("SKEPTIC", "Cel care nu crede", true)
        gossip.addPhase("ORIGIN", "Nasterea zvonului")
        gossip.addPhase("SPREAD", "Raspandirea")
        gossip.addPhase("DISCOVERY", "Subiectul afla")
        gossip.addPhase("CONFRONTATION", "Confruntare")
        gossip.triggerProbability = 0.07
        gossip.minimumNpcCount = 2
        scenarioTemplates[ScenarioType.GOSSIP_SPREAD] = gossip

        loadAddonScenarioTemplates()
    }
    private fun loadAddonScenarioTemplates() {
        val featurePackLoader = plugin.featurePackLoader ?: return
        val primaryScenarioPack = featurePackLoader.getPrimaryScenarioPack()

        for (pack in featurePackLoader.getLoadedPacks()) {
            for (definition in pack.scenarios) {
                val template = ScenarioTemplate(definition.baseType)
                template.templateId = pack.id + ":" + definition.id
                template.displayName = definition.name
                template.description = definition.description
                template.sourcePackId = pack.id
                template.hint = definition.hint
                template.triggerProbability = definition.triggerProbability
                template.minimumNpcCount = definition.minimumNpcCount
                template.requiresPlayer = definition.isRequiresPlayer
                template.preferredTopologies = ArrayList(definition.preferredTopologies)
                template.narrativeHints = ArrayList(definition.narrativeHints)
                template.progressionEnabled = definition.isProgressionEnabled
                template.progressionMechanicId = definition.progressionMechanicId
                template.progressionKind = definition.progressionKind
                template.progressionLabel = definition.progressionLabel
                template.progressionSingularLabel = definition.progressionSingularLabel
                template.progressionPluralLabel = definition.progressionPluralLabel
                template.progressionMaxActive = definition.progressionMaxActive
                template.questCode = definition.questCode
                template.questGiverProfession = definition.questGiverProfession
                template.questPrerequisites = ArrayList(definition.questPrerequisites)
                template.questRepeatable = definition.isQuestRepeatable
                template.questCooldownSeconds = definition.questCooldownSeconds
                template.questDialogues = LinkedHashMap(definition.questDialogues)
                template.questStages = definition.questStages
                template.objectives = definition.objectives
                template.rewards = definition.rewards
                template.questContract = QuestScenarioContract.fromScenarioDefinition(definition)

                for (roleDefinition in definition.roles.values) {
                    val role = ScenarioRoleRule(
                        roleDefinition.id,
                        roleDefinition.description ?: "",
                        roleDefinition.isPlayerRole,
                        roleDefinition.isOptional
                    )
                    role.requiredProfessions = roleDefinition.requiredProfessions
                    role.preferredProfessions = roleDefinition.preferredProfessions
                    role.requiredTraits = roleDefinition.requiredTraits
                    role.preferredTraits = roleDefinition.preferredTraits
                    template.addRole(role)
                }

                for (phase in definition.phases) {
                    template.addPhase(phase, phase)
                }

                if (isProgressionRuntimeDefinition(definition, template)) {
                    questTemplates[template.templateId] = template
                }

                val shouldReplace = definition.isReplaceBaseType
                    || (primaryScenarioPack != null && primaryScenarioPack.id.equals(pack.id, ignoreCase = true))
                    || !scenarioTemplates.containsKey(definition.baseType)

                if (shouldReplace) {
                    scenarioTemplates[definition.baseType] = template
                    plugin.logger.info("Scenariu addon incarcat: " + template.displayName
                        + " (" + template.templateId + ")")
                }
            }
        }
    }
    private fun isProgressionRuntimeDefinition(definition: FeaturePackLoader.ScenarioDefinition, template: ScenarioTemplate): Boolean {
        if (definition == null || template == null || !template.hasQuestBriefing()) return false
        return definition.baseType == ScenarioType.QUEST || definition.isProgressionEnabled
    }
    fun handleQuestInteraction(p0: Player, p1: AINPC): QuestInteractionResult {
        if (p0 == null || p1 == null) {
            plugin.debug("[QuestEngine] handleQuestInteraction oprit: player sau npc este null.")
            return QuestInteractionResult.notHandled()
        }
        plugin.debug("[QuestEngine] handleQuestInteraction player=" + p0.name
            + " npc=" + p1.name
            + " ocupatie=" + p1.occupation)
        val playerId = p0.uniqueId
        val template = findQuestTemplateForNpc(p1, playerId)
        if (template == null || !template.hasQuestBriefing()) {
            plugin.debug("[QuestEngine] Nu exista template de quest pentru npc=" + p1.name)
            return QuestInteractionResult.notHandled()
        }
        plugin.debug("[QuestEngine] Template gasit pentru npc=" + p1.name
            + " templateId=" + template.templateId
            + " title=" + resolveQuestTitle(template))
        var currentProgress = getCurrentQuestProgress(playerId, template.templateId)
        if (currentProgress != null) {
            plugin.debug("[QuestEngine] Progres curent pentru player=" + p0.name
                + " templateId=" + currentProgress.templateId()
                + " status=" + currentProgress.status())
        } else {
            plugin.debug("[QuestEngine] Player=" + p0.name + " nu are progres de quest inregistrat.")
        }
        if (currentProgress == null || currentProgress.templateId() != template.templateId) {
            val availability = evaluateQuestAvailability(playerId, template)
            if (!availability.available()) {
                plugin.debug("[QuestEngine] Quest indisponibil pentru player=" + p0.name
                    + " templateId=" + template.templateId
                    + " motiv=" + java.lang.String.join("; ", availability.issues()))
                return buildQuestUnavailableResult(template, availability)
            }
            val resolvedAnchors = resolveQuestAnchors(template, p0, p1)
            if (!resolvedAnchors.valid()) {
                return buildQuestUnavailableResult(template, resolvedAnchors)
            }
            var offeredProgress = setInitialQuestProgress(playerId, p0, template)
            offeredProgress = bindQuestProgressToNpc(playerId, template, offeredProgress, p1)
            offeredProgress = bindQuestProgressToAnchors(playerId, offeredProgress, resolvedAnchors)
            plugin.debug("[QuestEngine] Quest oferit pentru player=" + p0.name
                + " templateId=" + template.templateId)
            val npcMessages = buildQuestNpcMessages(
                template,
                offeredProgress,
                resolveInitialQuestDialogueContext(template),
                buildInitialQuestNpcFallbackMessages(template)
            )
            return QuestInteractionResult.handled(
                true,
                npcMessages,
                buildQuestStatusMessages(template, offeredProgress, p0, p1.name)
            )
        }
        if (currentProgress.isOffered()) {
            return QuestInteractionResult.handled(
                true,
                buildQuestNpcMessages(
                    template,
                    currentProgress,
                    QuestDialogueContext.OFFERED,
                    listOf("Misiunea e a ta daca o vrei. Spune-mi clar daca o accepti.")
                ),
                buildQuestStatusMessages(template, currentProgress, p0, p1.name)
            )
        }
        currentProgress = refreshTrackedQuestProgress(p0, template, currentProgress)
        currentProgress = trackNpcObjectiveProgress(p0, p1, template, currentProgress)
        val objectiveCheck = inspectQuestObjectives(p0, template, currentProgress, p1, true)
        if (!objectiveCheck.complete()) {
            plugin.debug("[QuestEngine] Quest incomplet pentru player=" + p0.name
                + " lipsesc=" + java.lang.String.join(", ", objectiveCheck.missingObjectives()))
            return QuestInteractionResult.handled(
                true,
                buildQuestNpcMessages(
                    template,
                    currentProgress,
                    QuestDialogueContext.ACTIVE,
                    listOf("Inca nu ai terminat tot ce ti-am cerut. Revino cand obiectivele sunt complete.")
                ),
                buildQuestStatusMessages(template, currentProgress, p0, p1.name)
            )
        }
        if (requiresQuestGiverTurnIn(template) && !matchesQuestGiver(plugin.featurePackLoader, p1, template)) {
            return QuestInteractionResult.handled(
                true,
                buildQuestNpcMessages(
                    template,
                    currentProgress,
                    QuestDialogueContext.READY,
                    listOf("Ai terminat ce era de facut. Intoarce-te la cel care ti-a dat misiunea.")
                ),
                buildQuestStatusMessages(template, currentProgress, p0, p1.name)
            )
        }
        val completionKey = buildQuestCompletionKey(playerId, template.templateId)
        if (!questCompletionLocks.add(completionKey)) {
            return QuestInteractionResult.handled(
                true,
                listOf("Misiunea este deja in curs de finalizare."),
                buildQuestStatusMessages(template, currentProgress, p0, p1.name)
            )
        }
        try {
            val rewardCheck = inspectQuestRewardDelivery(
                p0.inventory,
                template.objectives,
                template.rewards
            )
            if (!rewardCheck.canGrant()) {
                val systemMessages = buildQuestStatusMessages(template, currentProgress, p0, p1.name).toMutableList()
                systemMessages.add("&cNu pot finaliza questul pana cand recompensa poate fi acordata:")
                for (issue in rewardCheck.issues()) {
                    systemMessages.add("&7- &f" + issue)
                }
                return QuestInteractionResult.handled(
                    true,
                    buildQuestNpcMessages(
                        template,
                        currentProgress,
                        QuestDialogueContext.READY,
                        listOf("Ai facut partea grea, dar fa putin loc pentru rasplata si vorbim din nou.")
                    ),
                    systemMessages
                )
            }
            consumeQuestObjectives(p0.inventory, template.objectives)
            val rewardNotes = grantQuestRewards(p0, template.rewards).toMutableList()
            p0.updateInventory()
            rewardNotes.addAll(applyQuestStoryActions(p0, p1, template, currentProgress, template.rewards))
            markQuestCompleted(playerId, template)
            plugin.debug("[QuestEngine] Quest completat pentru player=" + p0.name
                + " templateId=" + template.templateId)
            val systemMessages = mutableListOf<String>()
            systemMessages.add("&aQuest completat: &f" + resolveQuestTitle(template))
            systemMessages.add("&aRecompense primite:")
            for (reward in template.rewards) {
                systemMessages.add("&7- &f" + formatQuestEntry(reward))
            }
            systemMessages.addAll(rewardNotes)
            return QuestInteractionResult.handled(
                true,
                buildQuestNpcMessages(
                    template,
                    currentProgress,
                    QuestDialogueContext.COMPLETED,
                    listOf("Perfect. Exact materialele de care aveam nevoie.", "Poftim sabia promisa. Sa-ti fie de folos.")
                ),
                systemMessages
            )
        } finally {
            questCompletionLocks.remove(completionKey)
        }
    }
    fun acceptQuest(p0: Player, p1: AINPC): QuestInteractionResult = acceptQuest(p0, p1, "")
    fun acceptQuest(p0: Player, p1: AINPC, p2: String): QuestInteractionResult {
        if (p0 == null || p1 == null) {
            plugin.debug("[QuestEngine] acceptQuest oprit: player sau npc este null.")
            return QuestInteractionResult.notHandled()
        }
        val playerId = p0.uniqueId
        val template = findQuestTemplateForNpc(p1, playerId, p2)
        if (template == null || !template.hasQuestBriefing()) {
            plugin.debug("[QuestEngine] acceptQuest fara template pentru npc=" + p1.name)
            return QuestInteractionResult.notHandled()
        }
        val currentProgress = getCurrentQuestProgress(playerId, template.templateId)
        val completedProgress = getCompletedQuestProgress(playerId, template.templateId)
        if (completedProgress != null && !template.questRepeatable) {
            return QuestInteractionResult.handled(
                true,
                buildQuestNpcMessages(
                    template,
                    completedProgress,
                    QuestDialogueContext.COMPLETED,
                    listOf("Misiunea asta este deja incheiata intre noi.")
                ),
                buildQuestStatusMessages(template, completedProgress, p0, p1.name)
            )
        }
        if (completedProgress != null) {
            val availability = evaluateQuestAvailability(playerId, template)
            if (!availability.available()) {
                return buildQuestUnavailableResult(template, availability)
            }
        }
        if (currentProgress == null || currentProgress.templateId() != template.templateId) {
            return QuestInteractionResult.handled(
                true,
                listOf("Nu ti-am dat inca misiunea asta. Intreaba-ma mai intai de quest."),
                buildQuestStatusMessages(template, null, p0, p1.name)
            )
        }
        if (currentProgress.isActive()) {
            return QuestInteractionResult.handled(
                true,
                buildQuestNpcMessages(
                    template,
                    currentProgress,
                    QuestDialogueContext.ACTIVE,
                    listOf("Ai acceptat deja misiunea. Ma astept sa te intorci cu ce ti-am cerut.")
                ),
                buildQuestStatusMessages(template, currentProgress, p0, p1.name)
            )
        }
        val resolvedAnchors = resolveQuestAnchors(template, p0, p1)
        if (!resolvedAnchors.valid()) {
            return buildQuestUnavailableResult(template, resolvedAnchors)
        }
        var acceptedProgress = setActiveQuestProgress(playerId, p0, template)
        acceptedProgress = bindQuestProgressToNpc(playerId, template, acceptedProgress, p1)
        acceptedProgress = bindQuestProgressToAnchors(playerId, acceptedProgress, resolvedAnchors)
        plugin.debug("[QuestEngine] Quest acceptat pentru player=" + p0.name
            + " templateId=" + template.templateId)
        return QuestInteractionResult.handled(
            true,
            buildQuestNpcMessages(
                template,
                acceptedProgress,
                QuestDialogueContext.ACCEPTED,
                listOf("Bine. Ma bazez pe tine.", "Intoarce-te cand ai terminat.")
            ),
            buildQuestStatusMessages(template, acceptedProgress, p0, p1.name)
        )
    }
    fun declineQuest(p0: Player, p1: AINPC): QuestInteractionResult = declineQuest(p0, p1, "")
    fun declineQuest(p0: Player, p1: AINPC, p2: String): QuestInteractionResult {
        if (p0 == null || p1 == null) {
            plugin.debug("[QuestEngine] declineQuest oprit: player sau npc este null.")
            return QuestInteractionResult.notHandled()
        }
        val playerId = p0.uniqueId
        val template = findQuestTemplateForNpc(p1, playerId, p2)
        if (template == null || !template.hasQuestBriefing()) {
            plugin.debug("[QuestEngine] declineQuest fara template pentru npc=" + p1.name)
            return QuestInteractionResult.notHandled()
        }
        val currentProgress = getCurrentQuestProgress(playerId, template.templateId)
        val completedProgress = getCompletedQuestProgress(playerId, template.templateId)
        if (completedProgress != null) {
            return QuestInteractionResult.handled(
                true,
                listOf("Prea tarziu sa refuzi. Misiunea asta este deja incheiata."),
                buildQuestStatusMessages(template, completedProgress, p0, p1.name)
            )
        }
        if (currentProgress == null || currentProgress.templateId() != template.templateId) {
            return QuestInteractionResult.handled(
                true,
                listOf("Nu ai o oferta de quest activa de la mine."),
                buildQuestStatusMessages(template, null, p0, p1.name)
            )
        }
        if (currentProgress.isActive()) {
            return QuestInteractionResult.handled(
                true,
                listOf("Ai acceptat deja misiunea. Daca vrei sa renunti, abandoneaz-o."),
                buildQuestStatusMessages(template, currentProgress, p0, p1.name)
            )
        }
        removeActiveQuestProgress(playerId, template.templateId)
        deleteQuestProgressAsync(playerId, template.templateId)
        return QuestInteractionResult.handled(
            true,
            listOf("In regula. Poate alta data."),
            buildQuestStatusMessages(template, null, p0, p1.name)
        )
    }
    fun abandonQuest(p0: Player, p1: AINPC): QuestInteractionResult {
        if (p0 == null || p1 == null) {
            plugin.debug("[QuestEngine] abandonQuest oprit: player sau npc este null.")
            return QuestInteractionResult.notHandled()
        }
        val playerId = p0.uniqueId
        val template = findQuestTemplateForNpc(p1, playerId)
        if (template == null || !template.hasQuestBriefing()) {
            plugin.debug("[QuestEngine] abandonQuest fara template pentru npc=" + p1.name)
            return QuestInteractionResult.notHandled()
        }
        val currentProgress = getCurrentQuestProgress(playerId, template.templateId)
        val completedProgress = getCompletedQuestProgress(playerId, template.templateId)
        if (completedProgress != null) {
            return QuestInteractionResult.handled(
                true,
                listOf("Misiunea asta este deja terminata. Nu mai ai la ce renunta."),
                buildQuestStatusMessages(template, completedProgress, p0, p1.name)
            )
        }
        if (currentProgress == null || currentProgress.templateId() != template.templateId) {
            return QuestInteractionResult.handled(
                true,
                listOf("Nu ai un quest activ de abandonat la mine."),
                buildQuestStatusMessages(template, getFailedQuestProgress(playerId, template.templateId), p0, p1.name)
            )
        }
        if (currentProgress.isOffered()) {
            return QuestInteractionResult.handled(
                true,
                listOf("Nu ai acceptat inca misiunea. O poti doar refuza."),
                buildQuestStatusMessages(template, currentProgress, p0, p1.name)
            )
        }
        val failedProgress = markQuestFailed(playerId, template)
        return QuestInteractionResult.handled(
            true,
            buildQuestNpcMessages(
                template,
                failedProgress,
                QuestDialogueContext.FAILED,
                listOf("Am inteles. Consider misiunea abandonata.")
            ),
            buildQuestStatusMessages(template, failedProgress, p0, p1.name)
        )
    }
    fun abandonQuest(p0: Player, p1: String): QuestInteractionResult {
        if (p0 == null || p1.isBlank()) {
            plugin.debug("[QuestEngine] abandonQuest selector oprit: player sau selector lipsa.")
            return QuestInteractionResult.notHandled()
        }
        val playerId = p0.uniqueId
        val progress = if (isTrackedQuestSelector(p1))
            getTrackedQuestProgress(playerId, true)
        else
            findQuestProgressByReference(playerId, p1, true)
        if (progress == null) {
            return QuestInteractionResult.notHandled()
        }
        val template = resolveTemplateForProgress(progress, null)
        val systemMessages = mutableListOf<String>()
        systemMessages.add("&6=== Quest Abandon ===")
        systemMessages.add("&eJucator: &f" + p0.name)
        if (template == null) {
            systemMessages.add("&eQuest: &f" + progress.templateId())
            systemMessages.add("&7Status: &f" + formatQuestStatus(progress.status()))
            systemMessages.add("&cTemplate-ul questului nu mai este disponibil; nu pot schimba starea in siguranta.")
            return QuestInteractionResult.handled(false, listOf(), systemMessages)
        }
        if (progress.isCompleted()) {
            systemMessages.add("&7Misiunea asta este deja terminata. Nu mai ai la ce renunta.")
            systemMessages.addAll(buildQuestStatusMessages(template, progress, p0, resolveQuestNpcName(progress)))
            return QuestInteractionResult.handled(false, listOf(), systemMessages)
        }
        if (progress.status() == QuestStatus.FAILED) {
            systemMessages.add("&7Questul este deja abandonat sau esuat.")
            systemMessages.addAll(buildQuestStatusMessages(template, progress, p0, resolveQuestNpcName(progress)))
            return QuestInteractionResult.handled(false, listOf(), systemMessages)
        }
        if (progress.isOffered()) {
            systemMessages.add("&7Nu ai acceptat inca misiunea. O poti doar refuza.")
            systemMessages.addAll(buildQuestStatusMessages(template, progress, p0, resolveQuestNpcName(progress)))
            return QuestInteractionResult.handled(false, listOf(), systemMessages)
        }
        if (!progress.isActive()) {
            systemMessages.add("&7Questul nu este activ si nu poate fi abandonat.")
            systemMessages.addAll(buildQuestStatusMessages(template, progress, p0, resolveQuestNpcName(progress)))
            return QuestInteractionResult.handled(false, listOf(), systemMessages)
        }
        val failedProgress = markQuestFailed(playerId, template)
        systemMessages.add("&eQuest abandonat: &f" + resolveQuestTitle(template))
        systemMessages.addAll(buildQuestStatusMessages(template, failedProgress, p0, resolveQuestNpcName(failedProgress)))
        return QuestInteractionResult.handled(false, listOf(), systemMessages)
    }
    fun startQuestManually(p0: Player, p1: AINPC): QuestInteractionResult = startQuestManually(p0, p1, "")
    fun startQuestManually(p0: Player, p1: AINPC, p2: String): QuestInteractionResult {
        if (p0 == null || p1 == null) {
            plugin.debug("[QuestEngine] startQuestManually oprit: player sau npc este null.")
            return QuestInteractionResult.notHandled()
        }
        val playerId = p0.uniqueId
        val template = findQuestTemplateForNpc(p1, playerId, p2)
        if (template == null || !template.hasQuestBriefing()) {
            plugin.debug("[QuestEngine] startQuestManually fara template pentru npc=" + p1.name)
            return QuestInteractionResult.notHandled()
        }
        val currentProgress = getCurrentQuestProgress(playerId, template.templateId)
        if (currentProgress != null && currentProgress.templateId() == template.templateId) {
            return getQuestStatus(p0, p1)
        }
        val availability = evaluateQuestAvailability(playerId, template)
        if (!availability.available()) {
            return buildQuestUnavailableResult(template, availability)
        }
        val resolvedAnchors = resolveQuestAnchors(template, p0, p1)
        if (!resolvedAnchors.valid()) {
            return buildQuestUnavailableResult(template, resolvedAnchors)
        }
        removeArchivedQuestProgress(playerId, template.templateId)
        var offeredProgress = setInitialQuestProgress(playerId, p0, template)
        offeredProgress = bindQuestProgressToNpc(playerId, template, offeredProgress, p1)
        offeredProgress = bindQuestProgressToAnchors(playerId, offeredProgress, resolvedAnchors)
        plugin.debug("[QuestEngine] startQuestManually a oferit questul pentru player=" + p0.name
            + " templateId=" + template.templateId)
        val npcMessages = buildQuestNpcMessages(
            template,
            offeredProgress,
            resolveInitialQuestDialogueContext(template),
            buildInitialQuestNpcFallbackMessages(template)
        )
        return QuestInteractionResult.handled(
            true,
            npcMessages,
            buildQuestStatusMessages(template, offeredProgress, p0, p1.name)
        )
    }
    fun resetQuestProgress(p0: Player, p1: AINPC): Boolean {
        if (p0 == null || p1 == null) {
            plugin.debug("[QuestEngine] resetQuestProgress oprit: player sau npc este null.")
            return false
        }
        val playerId = p0.uniqueId
        val template = findQuestTemplateForNpc(p1, playerId)
        if (template == null || !template.hasQuestBriefing()) {
            plugin.debug("[QuestEngine] resetQuestProgress fara template pentru npc=" + p1.name)
            return false
        }
        val removedActive = removeActiveQuestProgress(playerId, template.templateId)
        val removedArchived = removeArchivedQuestProgress(playerId, template.templateId)
        if (!removedActive && !removedArchived) {
            plugin.debug("[QuestEngine] resetQuestProgress fara progres potrivit pentru player="
                + p0.name + " templateId=" + template.templateId)
            return false
        }
        clearQuestTrackingIfMatches(playerId, template.templateId)
        deleteQuestProgressAsync(playerId, template.templateId)
        plugin.debug("[QuestEngine] resetQuestProgress reusit pentru player=" + p0.name
            + " templateId=" + template.templateId)
        return true
    }
    fun forceCompleteQuest(p0: Player, p1: AINPC): QuestInteractionResult {
        if (p0 == null || p1 == null) {
            plugin.debug("[QuestEngine] forceCompleteQuest oprit: player sau npc este null.")
            return QuestInteractionResult.notHandled()
        }
        val playerId = p0.uniqueId
        val template = findQuestTemplateForNpc(p1, playerId)
        if (template == null || !template.hasQuestBriefing()) {
            plugin.debug("[QuestEngine] forceCompleteQuest fara template pentru npc=" + p1.name)
            return QuestInteractionResult.notHandled()
        }
        plugin.debug("[QuestEngine] forceCompleteQuest pentru player=" + p0.name
            + " templateId=" + template.templateId)
        val completedProgress = getCompletedQuestProgress(playerId, template.templateId)
        if (completedProgress != null) {
            return QuestInteractionResult.handled(
                false,
                buildQuestNpcMessages(
                    template,
                    completedProgress,
                    QuestDialogueContext.COMPLETED,
                    listOf("Misiunea asta este deja marcata ca terminata.")
                ),
                buildQuestStatusMessages(template, completedProgress, p0, p1.name)
            )
        }
        val completionKey = buildQuestCompletionKey(playerId, template.templateId)
        if (!questCompletionLocks.add(completionKey)) {
            return QuestInteractionResult.handled(
                false,
                listOf("Misiunea este deja in curs de finalizare."),
                buildQuestStatusMessages(template, getCurrentQuestProgress(playerId, template.templateId), p0, p1.name)
            )
        }
        try {
            val rewardCheck = inspectQuestRewardDelivery(
                p0.inventory,
                listOf(),
                template.rewards
            )
            if (!rewardCheck.canGrant()) {
                val systemMessages = buildQuestStatusMessages(
                    template,
                    getCurrentQuestProgress(playerId, template.templateId),
                    p0,
                    p1.name
                ).toMutableList()
                systemMessages.add("&cNu pot marca questul ca finalizat pana cand recompensa poate fi acordata:")
                for (issue in rewardCheck.issues()) {
                    systemMessages.add("&7- &f" + issue)
                }
                return QuestInteractionResult.handled(
                    false,
                    listOf("Fa loc pentru rasplata inainte sa inchid misiunea."),
                    systemMessages
                )
            }
            val rewardNotes = grantQuestRewards(p0, template.rewards).toMutableList()
            p0.updateInventory()
            markQuestCompleted(playerId, template)
            plugin.debug("[QuestEngine] forceCompleteQuest a marcat quest complet pentru player="
                + p0.name + " templateId=" + template.templateId)
            val systemMessages = mutableListOf<String>()
            systemMessages.add("&aQuest marcat manual ca finalizat: &f" + resolveQuestTitle(template))
            if (template.rewards.isNotEmpty()) {
                systemMessages.add("&aRecompense acordate:")
                for (reward in template.rewards) {
                    systemMessages.add("&7- &f" + formatQuestEntry(reward))
                }
            }
            systemMessages.addAll(rewardNotes)
            return QuestInteractionResult.handled(
                false,
                buildQuestNpcMessages(
                    template,
                    null,
                    QuestDialogueContext.COMPLETED,
                    listOf("In regula. Consider misiunea terminata.", "Poftim rasplata promisa.")
                ),
                systemMessages
            )
        } finally {
            questCompletionLocks.remove(completionKey)
        }
    }
    fun getQuestTitle(p0: AINPC): String {
        val template = findQuestTemplateForNpc(p0)
        if (template == null || !template.hasQuestBriefing()) return ""
        return resolveQuestTitle(template)
    }
    fun hasOfferedQuest(p0: Player): Boolean {
        if (p0 == null) return false
        return getCurrentQuestProgress(p0.uniqueId).any { it.isOffered() }
    }
    fun resolveActiveQuestNpc(p0: Player): AINPC? {
        for (progress in getCurrentQuestProgress(p0.uniqueId)) {
            if (!progress.isCurrent()) continue
            val questGiver = resolveQuestGiverNpc(progress)
            val template = resolveTemplateForProgress(progress, questGiver)
            if (questGiver != null && matchesProgressionKindFilter(template, "")) {
                return questGiver
            }
        }
        @Suppress("UNCHECKED_CAST")
        return null as AINPC
    }
    fun resolveActiveQuestNpc(p0: Player, p1: String): AINPC? = resolveActiveQuestNpc(p0, p1, null)
    fun resolveActiveQuestNpc(p0: Player, p1: AINPC): AINPC? = resolveActiveQuestNpc(p0, "", p1)
    fun resolveActiveQuestNpc(p0: Player, p1: String, p2: AINPC?): AINPC? {
        if (p0 == null) return null
        for (progress in getCurrentQuestProgress(p0.uniqueId)) {
            if (!progress.isCurrent()) continue
            val questGiver = resolveQuestGiverNpc(progress)
            val template = resolveTemplateForProgress(progress, null)
            if (questGiver != null && matchesProgressionKindFilter(template, p1)) {
                return questGiver
            }
        }
        if (p2 == null) return null
        for (progress in getCurrentQuestProgress(p0.uniqueId)) {
            val fallbackTemplate = resolveTemplateForProgress(progress, null)
            if (fallbackTemplate != null
                && matchesProgressionKindFilter(fallbackTemplate, p1)
                && matchesQuestGiver(plugin.featurePackLoader, p2, fallbackTemplate)) {
                return p2
            }
        }
        return null
    }
    fun hasQuestForNpc(p0: Player, p1: AINPC, p2: String): Boolean {
        if (p0 == null || p1 == null) return false
        val template = findQuestTemplateForNpc(p1, p0.uniqueId, p2)
        return template != null && template.hasQuestBriefing()
    }
    fun getQuestStatus(p0: Player, p1: AINPC): QuestInteractionResult {
        if (p0 == null || p1 == null) return QuestInteractionResult.notHandled()
        val playerId = p0.uniqueId
        val template = findQuestTemplateForNpc(p1, playerId)
        if (template == null || !template.hasQuestBriefing()) return QuestInteractionResult.notHandled()
        val currentProgress = getCurrentQuestProgress(playerId, template.templateId)
        val completedProgress = getCompletedQuestProgress(playerId, template.templateId)
        val failedProgress = getFailedQuestProgress(playerId, template.templateId)
        if (completedProgress != null) {
            return QuestInteractionResult.handled(
                true,
                buildQuestNpcMessages(
                    template,
                    completedProgress,
                    QuestDialogueContext.COMPLETED,
                    listOf("Ti-ai dus la capat datoria pentru misiunea asta.")
                ),
                buildQuestStatusMessages(template, completedProgress, p0, p1.name)
            )
        }
        if (failedProgress != null) {
            return QuestInteractionResult.handled(
                true,
                buildQuestNpcMessages(
                    template,
                    failedProgress,
                    QuestDialogueContext.FAILED,
                    listOf("Misiunea asta a fost abandonata sau esuata.")
                ),
                buildQuestStatusMessages(template, failedProgress, p0, p1.name)
            )
        }
        if (currentProgress != null && currentProgress.templateId() == template.templateId) {
            var refreshed = refreshTrackedQuestProgress(p0, template, currentProgress)
            val context = if (refreshed.isOffered()) QuestDialogueContext.OFFERED
            else resolveStatusDialogueContext(p0, template, refreshed)
            return QuestInteractionResult.handled(
                true,
                buildQuestNpcMessages(
                    template,
                    refreshed,
                    context,
                    listOf(if (refreshed.isOffered()) "Inca astept sa-mi spui daca accepti." else "Asa stai acum cu misiunea.")
                ),
                buildQuestStatusMessages(template, refreshed, p0, p1.name)
            )
        }
        return QuestInteractionResult.handled(
            true,
            buildQuestNpcMessages(
                template,
                null,
                QuestDialogueContext.OFFER,
                listOf("Misiunea este disponibila, dar inca nu ai acceptat-o.")
            ),
            buildQuestStatusMessages(template, null, p0, p1.name)
        )
    }
    fun getQuestLog(p0: Player): QuestInteractionResult = getQuestLog(p0, "")
    fun getQuestLog(p0: Player, p1: String): QuestInteractionResult = getQuestLog(p0, p1, false)
    fun getQuestLog(p0: Player, p1: String, p2: Boolean): QuestInteractionResult {
        if (p0 == null) {
            return QuestInteractionResult.notHandled()
        }
        val playerId = p0.uniqueId
        val logFilter = parseQuestLogFilter(p1)
        val currentProgresses = getCurrentQuestProgress(playerId)
        val archivedProgresses = getArchivedQuestProgress(playerId)
        val systemMessages = mutableListOf<String>()
        systemMessages.add("&6=== Progression Log ===")
        systemMessages.add("&eJucator: &f" + p0.name)
        if (logFilter != QuestLogFilter.SUMMARY) {
            systemMessages.add("&eFiltru: &f" + logFilter.displayName())
        }
        if (currentProgresses.isNotEmpty()) {
            systemMessages.addAll(buildQuestLogSummaryLines(playerId, currentProgresses))
        }
        val matchingCurrent = currentProgresses
            .filter { questLogMatches(playerId, it, logFilter, false) }
            .sortedWith(questLogCurrentComparator(playerId))
        if (matchingCurrent.isNotEmpty()) {
            systemMessages.add("&aProgresii curente: &f" + matchingCurrent.size)
            var currentGroup = ""
            for (currentProgress in matchingCurrent) {
                val template = resolveTemplateForProgress(currentProgress, null)
                val logProgress = if (template != null)
                    refreshTrackedQuestProgress(p0, template, currentProgress)
                else
                    currentProgress
                val groupLabel = questLogCurrentGroupLabel(playerId, template, logProgress)
                if (groupLabel != currentGroup) {
                    systemMessages.add(groupLabel)
                    currentGroup = groupLabel
                }
                if (template != null) {
                    if (isTrackedQuest(playerId, logProgress)) {
                        systemMessages.add("&bQuest urmarit: &f" + resolveQuestTitle(template))
                    }
                    systemMessages.addAll(buildQuestStatusMessages(
                        template,
                        logProgress,
                        p0,
                        resolveQuestNpcName(logProgress)
                    ))
                    systemMessages.addAll(buildQuestLogActionLines(p0, playerId, template, logProgress, p2))
                } else {
                    systemMessages.add("&7Template: &f" + logProgress.templateId())
                    systemMessages.add("&7Status: &f" + formatQuestStatus(logProgress.status()))
                    if (isTrackedQuest(playerId, logProgress)) {
                        systemMessages.add("&bProgresie urmarita: &fda")
                    }
                    if (logProgress.currentPhase().isNotBlank()) {
                        systemMessages.add("&7Faza curenta: &f" + formatQuestPhase(logProgress.currentPhase()))
                    }
                    systemMessages.addAll(buildQuestLogActionLines(p0, playerId, null, logProgress, p2))
                }
            }
        } else if (logFilter.showsCurrent()) {
            systemMessages.add(
                if (logFilter == QuestLogFilter.SUMMARY)
                    "&7Nu ai progresie activa."
                else
                    "&7Nu exista progresii curente pentru filtrul ales."
            )
        }
        val archivedLimit = if (logFilter == QuestLogFilter.SUMMARY) 3 else 20
        val matchingArchived = archivedProgresses
            .filter { questLogMatches(playerId, it, logFilter, true) }
            .take(archivedLimit)
        if (matchingArchived.isNotEmpty()) {
            systemMessages.add(
                if (logFilter == QuestLogFilter.SUMMARY) "&eUltimele progresii:" else "&eProgresii arhivate:"
            )
            for (archivedProgress in matchingArchived) {
                val template = resolveTemplateForProgress(archivedProgress, null)
                val title = if (template != null) resolveQuestTitle(template) else archivedProgress.templateId()
                systemMessages.add(formatQuestLogArchivedLine(playerId, template, archivedProgress, title ?: ""))
            }
            val totalMatchingArchived = archivedProgresses.count { questLogMatches(playerId, it, logFilter, true) }.toLong()
            if (totalMatchingArchived > matchingArchived.size) {
                systemMessages.add("&7... inca &f" + (totalMatchingArchived - matchingArchived.size)
                    + " &7progresii arhivate pentru filtrul ales.")
            }
        } else if (logFilter.showsArchived() && logFilter != QuestLogFilter.SUMMARY) {
            systemMessages.add("&7Nu exista progresii arhivate pentru filtrul ales.")
        }
        return QuestInteractionResult.handled(false, listOf(), systemMessages)
    }
    fun getQuestGuiSnapshot(p0: Player, p1: String, p2: Boolean): QuestGuiSnapshot {
        if (p0 == null) {
            return QuestGuiSnapshot.empty()
        }
        val playerId = p0.uniqueId
        val logFilter = parseQuestLogFilter(p1)
        val currentProgresses = getCurrentQuestProgress(playerId)
        val archivedProgresses = getArchivedQuestProgress(playerId)
        val summaryLines = if (currentProgresses.isEmpty())
            listOf("&7Nu ai progresie activa.")
        else
            buildQuestLogSummaryLines(playerId, currentProgresses)
        val currentEntries = currentProgresses
            .filter { questLogMatches(playerId, it, logFilter, false) }
            .sortedWith(questLogCurrentComparator(playerId))
            .map { buildQuestGuiEntry(p0, playerId, it, false, p2) }
        val archivedLimit = if (logFilter == QuestLogFilter.SUMMARY) 3 else 20
        val archivedEntries = archivedProgresses
            .filter { questLogMatches(playerId, it, logFilter, true) }
            .take(archivedLimit)
            .map { buildQuestGuiEntry(p0, playerId, it, true, p2) }
        val totalMatchingArchived = archivedProgresses.count { questLogMatches(playerId, it, logFilter, true) }.toLong()
        return QuestGuiSnapshot(
            true,
            p0.name,
            logFilter.displayName(),
            summaryLines,
            currentEntries,
            archivedEntries,
            totalMatchingArchived
        )
    }
    private fun buildQuestGuiEntry(player: Player, playerId: UUID, progress: PlayerQuestProgress, archived: Boolean, adminView: Boolean): QuestGuiEntry {
        val template = resolveTemplateForProgress(progress, null)
        val viewProgress: PlayerQuestProgress = if (template != null && progress != null && progress.isCurrent())
            refreshTrackedQuestProgress(player, template, progress)
        else
            progress

        val selector = questLogActionSelector(template, viewProgress)
        val title = if (template != null) resolveQuestTitle(template) else valueOrFallback(viewProgress.templateId(), "Quest necunoscut")
        val category = if (template != null) resolveQuestCategory(template).displayName() else "Necunoscut"
        val mechanic = if (template != null) resolveProgressionMechanicDisplay(plugin.featurePackLoader, template) else "Necunoscuta"
        val statusDisplay = formatQuestStatus(viewProgress.status())
        var currentStageId = ""
        currentStageId = viewProgress.currentPhase()
        if (currentStageId.isBlank() && template != null) {
            currentStageId = resolveQuestPhase(template, viewProgress.status(), viewProgress)
        }

        val statusLines = if (template != null)
            buildQuestStatusMessages(template, viewProgress, player, resolveQuestNpcName(viewProgress))
        else
            buildMissingQuestTemplateLines(viewProgress)

        return QuestGuiEntry(
            selector,
            valueOrFallback(viewProgress.templateId(), ""),
            valueOrFallback(viewProgress.questCode(), ""),
            title,
            statusDisplay,
            category,
            mechanic,
            isTrackedQuest(playerId, viewProgress),
            viewProgress.isCurrent(),
            viewProgress.isActive(),
            viewProgress.isOffered(),
            archived,
            template == null,
            currentStageId,
            formatQuestPhase(currentStageId),
            viewProgress.updatedAt(),
            resolveQuestNpcName(viewProgress),
            statusLines,
            if (template != null) buildQuestGuiObjectives(player, template, viewProgress) else emptyList(),
            if (template != null) buildQuestGuiStages(template, viewProgress, currentStageId) else emptyList(),
            if (template != null) template.rewards.map { formatQuestEntry(it) } else emptyList(),
            buildQuestLogActionLines(player, playerId, template, viewProgress, adminView)
        )
    }
    private fun buildMissingQuestTemplateLines(progress: PlayerQuestProgress?): List<String> {
        if (progress == null) return listOf("&cQuest progress indisponibil.")
        val lines = mutableListOf<String>()
        lines.add("&eTemplate lipsa: &f" + progress.templateId())
        lines.add("&7Status: &f" + formatQuestStatus(progress.status()))
        if (progress.currentPhase().isNotBlank()) {
            lines.add("&7Faza curenta: &f" + formatQuestPhase(progress.currentPhase()))
        }
        return lines
    }
    private fun buildQuestGuiObjectives(player: Player, template: ScenarioTemplate?, progress: PlayerQuestProgress?): List<QuestGuiObjective> {
        if (template == null || template.objectives.isEmpty()) return emptyList()

        val objectives = mutableListOf<QuestGuiObjective>()
        val entries = template.objectives
        for (index in entries.indices) {
            val objective = entries[index]
            val objectiveKey = buildObjectiveKey(objective, index)
            val requiredAmount = maxOf(1, objective.amount)
            val storedProgress = if (progress != null)
                readObjectiveProgress(progress.objectiveProgress(), objective, index)
            else
                0
            val currentProgress = if (progress != null && progress.isActive())
                resolveObjectiveCurrentProgress(player, objective, progress, index)
            else
                minOf(requiredAmount, storedProgress)
            val activeForStage = progress == null || shouldShowObjectiveForCurrentStage(template, progress, objective)
            val stageId = findObjectiveStageId(template, objective)
            val objectiveState = resolveQuestObjectiveState(
                progress,
                currentProgress,
                requiredAmount,
                activeForStage
            )

            objectives.add(QuestGuiObjective(
                objectiveKey,
                normalizeObjectiveType(objective.type),
                formatObjectiveProgressLabel(objective),
                formatQuestEntry(objective),
                stageId,
                formatQuestPhase(stageId),
                objectiveState.id(),
                objectiveState.displayName(),
                minOf(currentProgress, requiredAmount),
                requiredAmount,
                currentProgress >= requiredAmount,
                activeForStage
            ))
        }
        return objectives
    }
    private fun buildQuestGuiStages(template: ScenarioTemplate?, progress: PlayerQuestProgress?, currentStageId: String): List<QuestGuiStage> {
        if (template == null || template.questStages.isEmpty()) return emptyList()

        val stages = mutableListOf<QuestGuiStage>()
        for (stage in template.questStages) {
            if (stage == null || stage.id.isBlank()) continue

            val active = currentStageId.isNotBlank() && phasesMatch(stage.id, currentStageId)
            val complete = progress != null && areObjectivesSatisfiedForStage(template, stage.id, progress.objectiveProgress())
            stages.add(QuestGuiStage(
                stage.id,
                formatQuestPhase(stage.id),
                stage.description,
                formatStageCompletionMode(stage.completionMode),
                stage.getNextStageId(),
                active,
                complete,
                stage.objectiveIds
            ))
        }
        return stages
    }
    fun getQuestStatus(p0: Player, p1: String): QuestInteractionResult {
        if (p0 == null || p1.isBlank()) {
            return QuestInteractionResult.notHandled()
        }
        var progress = findQuestProgressByReference(p0.uniqueId, p1, true)
        if (progress == null) {
            return QuestInteractionResult.notHandled()
        }
        val template = resolveTemplateForProgress(progress, null)
        if (template != null && progress.isCurrent()) {
            progress = refreshTrackedQuestProgress(p0, template, progress)
        }
        val systemMessages = mutableListOf<String>()
        systemMessages.add("&6=== Progression Status ===")
        systemMessages.add("&eJucator: &f" + p0.name)
        if (template != null) {
            systemMessages.addAll(buildQuestStatusMessages(
                template,
                progress,
                p0,
                resolveQuestNpcName(progress)
            ))
        } else {
            systemMessages.add("&eProgresie: &f" + progress.templateId())
            systemMessages.add("&7Status: &f" + formatQuestStatus(progress.status()))
            if (progress.currentPhase().isNotBlank()) {
                systemMessages.add("&7Faza curenta: &f" + formatQuestPhase(progress.currentPhase()))
            }
            systemMessages.add("&cTemplate-ul questului nu mai este disponibil in configuratia curenta.")
        }
        return QuestInteractionResult.handled(false, listOf(), systemMessages)
    }
    fun getQuestDebug(p0: Player, p1: String): QuestInteractionResult {
        if (p0 == null || p1.isBlank()) {
            return QuestInteractionResult.notHandled()
        }
        val playerId = p0.uniqueId
        var progress = if (isTrackedQuestSelector(p1))
            getTrackedQuestProgress(playerId, true)
        else
            findQuestProgressByReference(playerId, p1, true)
        if (progress == null) {
            return QuestInteractionResult.notHandled()
        }
        val template = resolveTemplateForProgress(progress, null)
        if (template != null && progress.isCurrent()) {
            progress = refreshTrackedQuestProgress(p0, template, progress)
        }
        val systemMessages = mutableListOf<String>()
        systemMessages.add("&6=== Progression Debug ===")
        systemMessages.add("&eJucator: &f" + p0.name + " &7(" + playerId + ")")
        systemMessages.add("&eSelector: &f" + p1)
        systemMessages.add("&eTemplate: &f" + progress.templateId())
        systemMessages.add("&eCod: &f" + formatOptional(progress.questCode()))
        systemMessages.add("&7Status: &f" + formatQuestStatus(progress.status()))
        systemMessages.add("&7Tracked: &f" + (if (isTrackedQuest(playerId, progress)) "da" else "nu"))
        systemMessages.add("&7Faza: &f" + formatOptional(progress.currentPhase()))
        systemMessages.add("&7Started/Completed/Updated: &f"
            + formatQuestDebugTime(progress.startedAt()) + " / "
            + formatQuestDebugTime(progress.completedAt()) + " / "
            + formatQuestDebugTime(progress.updatedAt()))
        if (template != null) {
            systemMessages.add("&eTitlu: &f" + resolveQuestTitle(template))
            if (template.progressionMechanicId.isNotBlank()) {
                systemMessages.add("&7Progression: &f" + template.progressionMechanicId
                    + " &7/ kind=&f" + formatOptional(template.progressionKind)
                    + " &7/ label=&f" + formatOptional(template.progressionLabel))
            }
            systemMessages.add("&7Giver profession: &f" + formatOptional(template.questGiverProfession))
            val contract = template.questContract
            if (contract != null) {
                systemMessages.add("&7Contract: &f" + contract.displayName()
                    + " &7/ categorie=&f" + contract.categoryDisplayName()
                    + " &7/ acceptare=&f" + contract.acceptanceMode().name.lowercase(java.util.Locale.ROOT))
            }
            systemMessages.add("&eObiective template:")
            val objectives = template.objectives
            if (objectives.isEmpty()) {
                systemMessages.add("&7- &f<gol>")
            } else {
                for (index in objectives.indices) {
                    val objective = objectives[index]
                    val objectiveKey = buildObjectiveKey(objective, index)
                    val current = if (progress.isCurrent())
                        resolveObjectiveCurrentProgress(p0, objective, progress, index)
                    else
                        readObjectiveProgress(progress.objectiveProgress(), objective, index)
                    val activeForStage = shouldShowObjectiveForCurrentStage(template, progress, objective)
                    val state = resolveQuestObjectiveState(
                        progress,
                        current,
                        objective.amount,
                        activeForStage
                    )
                    systemMessages.add("&7- &f" + objectiveKey
                        + " &7type=&f" + normalizeObjectiveType(objective.type)
                        + " &7stage=&f" + formatOptional(canonicalQuestPhase(template, getObjectiveStage(objective)))
                        + " &7target=&f" + formatOptional(objective.itemId)
                        + " &7state=&f" + state.displayName()
                        + " &7progress=&f" + minOf(current, maxOf(1, objective.amount))
                        + "/" + maxOf(1, objective.amount))
                }
            }
        } else {
            systemMessages.add("&cTemplate-ul questului nu mai este disponibil in configuratia curenta.")
        }
        systemMessages.add("&eObjective progress:")
        systemMessages.addAll(formatQuestDebugMap(progress.objectiveProgress(), 20))
        systemMessages.add("&eQuest variables:")
        systemMessages.addAll(formatQuestDebugMap(progress.questVariables(), 30))
        return QuestInteractionResult.handled(false, listOf(), systemMessages)
    }
    fun getQuestProgress(p0: Player, p1: String): QuestInteractionResult {
        if (p0 == null) {
            return QuestInteractionResult.notHandled()
        }
        val playerId = p0.uniqueId
        var progress = selectQuestProgressForProgress(playerId, p1)
        val systemMessages = mutableListOf<String>()
        systemMessages.add("&6=== Progression Progress ===")
        systemMessages.add("&eJucator: &f" + p0.name)
        if (progress == null) {
            if (!p1.isBlank()) {
                systemMessages.add("&cNu exista progres pentru selectorul: &f" + p1)
            } else {
                systemMessages.add("&7Nu ai quest curent cu progres.")
            }
            return QuestInteractionResult.handled(false, listOf(), systemMessages)
        }
        val template = resolveTemplateForProgress(progress, null)
        if (template != null && progress.isCurrent()) {
            progress = refreshTrackedQuestProgress(p0, template, progress)
        }
        systemMessages.add("&eTemplate: &f" + progress.templateId())
        systemMessages.add("&eCod: &f" + formatOptional(progress.questCode()))
        systemMessages.add("&7Status: &f" + formatQuestStatus(progress.status()))
        systemMessages.add("&7Tracked: &f" + (if (isTrackedQuest(playerId, progress)) "da" else "nu"))
        if (progress.currentPhase().isNotBlank()) {
            systemMessages.add("&7Faza curenta: &f" + formatQuestPhase(progress.currentPhase()))
        }
        if (template == null) {
            systemMessages.add("&cTemplate-ul progresiei nu mai este disponibil in configuratia curenta.")
            systemMessages.add("&eObjective progress brut:")
            systemMessages.addAll(formatQuestDebugMap(progress.objectiveProgress(), 20))
            return QuestInteractionResult.handled(false, listOf(), systemMessages)
        }
        systemMessages.add("&e" + capitalizeProgressionLabel(resolveProgressionSingularLabel(plugin.featurePackLoader, template)) + ": &f" + resolveQuestTitle(template))
        if (template.progressionMechanicId.isNotBlank()) {
            systemMessages.add("&7Mecanica: &f" + template.progressionMechanicId
                + (if (template.progressionLabel.isBlank()) "" else " &7(" + template.progressionLabel + ")"))
        }
        if (template.objectives.isEmpty()) {
            systemMessages.add("&7Nu exista obiective in template.")
        } else {
            systemMessages.add("&eObiective:")
            systemMessages.addAll(buildQuestProgressDetailLines(p0, template, progress))
        }
        return QuestInteractionResult.handled(false, listOf(), systemMessages)
    }
    fun getQuestTrack(p0: Player): QuestInteractionResult = getQuestTrack(p0, "")
    fun getQuestTrack(p0: Player, p1: String): QuestInteractionResult {
        if (p0 == null) {
            return QuestInteractionResult.notHandled()
        }
        val playerId = p0.uniqueId
        val currentProgress = selectQuestProgressForTracking(playerId, p1)
        val systemMessages = mutableListOf<String>()
        systemMessages.add("&6=== Quest Track ===")
        systemMessages.add("&eJucator: &f" + p0.name)
        if (currentProgress == null || !currentProgress.isCurrent()) {
            if (!p1.isBlank()) {
                systemMessages.add("&cNu exista quest curent pentru selectorul: &f" + p1)
            } else {
                systemMessages.add("&7Nu ai quest activ de urmarit.")
            }
            return QuestInteractionResult.handled(false, listOf(), systemMessages)
        }
        val template = resolveTemplateForProgress(currentProgress, null)
        if (template == null) {
            systemMessages.add("&eQuest: &f" + currentProgress.templateId())
            systemMessages.add("&7Status: &f" + formatQuestStatus(currentProgress.status()))
            if (currentProgress.currentPhase().isNotBlank()) {
                systemMessages.add("&7Faza curenta: &f" + formatQuestPhase(currentProgress.currentPhase()))
            }
            systemMessages.add("&cTemplate-ul questului nu mai este disponibil in configuratia curenta.")
            return QuestInteractionResult.handled(false, listOf(), systemMessages)
        }
        val refreshedProgress = refreshTrackedQuestProgress(p0, template, currentProgress)
        systemMessages.add("&eQuest: &f" + resolveQuestTitle(template))
        systemMessages.add("&7Status: &f" + formatQuestStatus(refreshedProgress.status()))
        if (refreshedProgress.currentPhase().isNotBlank()) {
            systemMessages.add("&7Faza curenta: &f" + formatQuestPhase(refreshedProgress.currentPhase()))
        }
        if (refreshedProgress.isOffered()) {
            systemMessages.add("&7Misiunea este oferita, dar trebuie acceptata inainte sa fie urmarita.")
            return QuestInteractionResult.handled(false, listOf(), systemMessages)
        }
        if (!refreshedProgress.isActive()) {
            systemMessages.add("&7Questul nu este activ.")
            return QuestInteractionResult.handled(false, listOf(), systemMessages)
        }
        val objectiveCheck = inspectQuestObjectives(p0, template, refreshedProgress, null, false)
        if (objectiveCheck.complete()) {
            systemMessages.add("&aObiectivele sunt complete. Revino la NPC pentru finalizare.")
            val questGiverHint = describeQuestGiverTrackingTarget(refreshedProgress, p0)
            if (questGiverHint.isNotBlank()) {
                systemMessages.add("&bTinta: &f" + questGiverHint)
            }
            return QuestInteractionResult.handled(false, listOf(), systemMessages)
        }
        val trackingLines = buildQuestTrackingLines(template, refreshedProgress, p0)
        if (trackingLines.isNotEmpty()) {
            systemMessages.add("&eUrmatorul pas:")
            systemMessages.addAll(trackingLines)
        } else {
            systemMessages.add("&eIti mai lipsesc:")
            for (missingObjective in objectiveCheck.missingObjectives()) {
                systemMessages.add("&7- &f" + missingObjective)
            }
            systemMessages.add("&7Nu exista tinte de locatie salvate pentru obiectivele ramase.")
        }
        return QuestInteractionResult.handled(false, listOf(), systemMessages)
    }
    fun getQuestTrackingMarker(p0: Player): QuestTrackingMarker? = getQuestTrackingMarker(p0, "")
    fun getQuestTrackingMarker(p0: Player, p1: String): QuestTrackingMarker? {
        if (p0 == null) {
            return null
        }
        val selectedProgress = selectQuestProgressForTracking(p0.uniqueId, p1)
        if (selectedProgress == null && !p1.isBlank()) {
            return null
        }
        val candidates = if (selectedProgress != null)
            listOf(selectedProgress)
        else
            getCurrentQuestProgress(p0.uniqueId)
        for (currentProgress in candidates) {
            if (!currentProgress.isActive()) {
                continue
            }
            val template = resolveTemplateForProgress(currentProgress, null)
            if (template == null) {
                continue
            }
            val refreshedProgress = refreshTrackedQuestProgress(p0, template, currentProgress)
            val objectiveCheck = inspectQuestObjectives(p0, template, refreshedProgress, null, false)
            if (objectiveCheck.complete()) {
                val questGiverTarget = resolveQuestGiverTrackingTarget(refreshedProgress)
                val marker = buildQuestTrackingMarker("finalizeaza questul", questGiverTarget, p0)
                if (marker != null) {
                    return marker
                }
                continue
            }
            val trackingStep = resolveNextQuestTrackingStep(template, refreshedProgress, p0)
            if (trackingStep == null) {
                continue
            }
            val marker = buildQuestTrackingMarker(trackingStep.objectiveLabel(), trackingStep.target(), p0)
            if (marker != null) {
                return marker
            }
        }
        return null
    }
    fun startQuestTracking(p0: Player): QuestTrackingMarker? = startQuestTracking(p0, "")
    fun startQuestTracking(p0: Player, p1: String): QuestTrackingMarker? {
        val selectedProgress = if (p0 != null)
            selectQuestProgressForTracking(p0.uniqueId, p1)
        else
            null
        val marker = getQuestTrackingMarker(p0, p1)
        if (p0 == null || marker == null || !marker.hasLocation()) {
            return marker
        }
        trackedQuestPlayers.add(p0.uniqueId)
        val tid = selectedProgress?.templateId()
        if (tid != null && tid.isNotBlank()) {
            trackedQuestTemplates.put(p0.uniqueId, tid)
            persistQuestTrackingPreferenceAsync(p0.uniqueId, selectedProgress)
        }
        return marker
    }
    fun stopQuestTracking(p0: Player): Boolean {
        if (p0 == null) {
            return false
        }
        val hadTemplate = trackedQuestTemplates.remove(p0.uniqueId) != null
        val stopped = trackedQuestPlayers.remove(p0.uniqueId)
        if (stopped || hadTemplate) {
            persistQuestTrackingPreferenceAsync(p0.uniqueId, "")
        }
        return stopped || hadTemplate
    }
    fun stopAllQuestTracking() {
        trackedQuestPlayers.clear()
        trackedQuestTemplates.clear()
    }
    fun isQuestTracking(p0: Player): Boolean = p0 != null && trackedQuestPlayers.contains(p0.uniqueId)
    fun tickQuestTrackingMarkers(): Int {
        var updated = 0
        val iterator = trackedQuestPlayers.iterator()
        while (iterator.hasNext()) {
            val playerId = iterator.next()
            val player = Bukkit.getPlayer(playerId)
            if (player == null || !player.isOnline) {
                iterator.remove()
                continue
            }
            val marker = getQuestTrackingMarker(player, trackedQuestTemplates.getOrDefault(playerId, ""))
            if (marker == null || !marker.hasLocation()) {
                iterator.remove()
                trackedQuestTemplates.remove(playerId)
                persistQuestTrackingPreferenceAsync(playerId, "")
                plugin.messageUtils.sendActionBar(player, "&cQuest tracking oprit &8| &7nu mai exista tinta activa")
                continue
            }
            applyQuestTrackingMarker(player, marker)
            updated++
        }
        return updated
    }
    fun applyQuestTrackingMarker(p0: Player, p1: QuestTrackingMarker): Boolean {
        if (p0 == null || p1 == null || !p1.hasLocation()) return false
        val targetLocation = p1.location
        plugin.messageUtils.sendActionBar(p0, p1.actionBarMessage)
        spawnQuestTrackingParticles(p0, p1)
        if (targetLocation?.world == p0.world) {
            p0.compassTarget = targetLocation
            return true
        }
        return false
    }
    private fun spawnQuestTrackingParticles(p0: Player, p1: QuestTrackingMarker) {
        TODO()
    }
    private fun spawnQuestDirectionParticles(p0: Player, p1: Location, p2: Double) {
        TODO()
    }
    private fun spawnQuestWaypointParticles(p0: Player, p1: Location) {
        TODO()
    }
    fun recordNpcConversation(p0: Player, p1: AINPC) {
        if (p0 == null || p1 == null) return
        for (progress in getCurrentQuestProgress(p0.uniqueId)) {
            if (!progress.isCurrent()) continue
            val template = resolveTemplateForProgress(progress, p1)
            if (template == null) continue
            val refreshedProgress = refreshTrackedQuestProgress(p0, template, progress)
            trackNpcObjectiveProgress(p0, p1, template, refreshedProgress)
        }
    }
    fun recordRegionVisit(p0: Player) {
        if (p0 == null) return
        val location = p0.location
        val region = findCurrentRegion(location)
        val place = findCurrentPlace(location)
        val node = findCurrentNode(location)
        if (region == null && place == null && node == null) return
        for (progress in getCurrentQuestProgress(p0.uniqueId)) {
            if (!progress.isCurrent()) continue
            val template = resolveTemplateForProgress(progress, null)
            if (template == null
                || (!hasObjectiveType(template, "visit_region")
                    && !hasObjectiveType(template, "visit_place")
                    && !hasObjectiveType(template, "inspect_node"))) {
                continue
            }
            val updatedProgress = LinkedHashMap(progress.objectiveProgress())
            var changed = false
            val objectives = template.objectives
            for (index in objectives.indices) {
                val objective = objectives[index]
                if (!isObjectiveActiveForProgress(template, progress, objective)) continue
                val objectiveKey = buildObjectiveKey(objective, index)
                changed = changed or carryLegacyObjectiveProgress(updatedProgress, objective, index)
                val matchesLocationObjective =
                    (matchesObjectiveType(objective, "visit_region") && matchesRegionObjective(progress, objective, index, region))
                        || (matchesObjectiveType(objective, "visit_place") && matchesPlaceObjective(progress, objective, index, place))
                        || (matchesObjectiveType(objective, "inspect_node") && matchesNodeObjective(progress, objective, index, node))
                if (!matchesLocationObjective) continue
                changed = changed or incrementObjectiveProgress(updatedProgress, objectiveKey, objective.amount)
            }
            if (changed) {
                updateTrackedQuestProgress(p0.uniqueId, template, progress, updatedProgress)
            }
        }
    }
    fun recordMobKill(p0: Player, p1: Entity) {
        if (p0 == null || p1 == null) return
        for (progress in getCurrentQuestProgress(p0.uniqueId)) {
            if (!progress.isCurrent()) continue
            val template = resolveTemplateForProgress(progress, null)
            if (template == null || !hasObjectiveType(template, "kill_mob")) continue
            val updatedProgress = LinkedHashMap(progress.objectiveProgress())
            var changed = false
            val objectives = template.objectives
            for (index in objectives.indices) {
                val objective = objectives[index]
                if (!isObjectiveActiveForProgress(template, progress, objective)) continue
                if (!matchesObjectiveType(objective, "kill_mob") || !matchesMobObjective(objective, p1)) continue
                val objectiveKey = buildObjectiveKey(objective, index)
                changed = changed or carryLegacyObjectiveProgress(updatedProgress, objective, index)
                changed = changed or incrementObjectiveProgress(updatedProgress, objectiveKey, objective.amount)
            }
            if (changed) {
                updateTrackedQuestProgress(p0.uniqueId, template, progress, updatedProgress)
            }
        }
    }
    fun recordInventoryChange(p0: Player) {
        if (p0 == null) return
        for (progress in getCurrentQuestProgress(p0.uniqueId)) {
            if (!progress.isCurrent()) continue
            val template = resolveTemplateForProgress(progress, null)
            if (template == null || !hasInventoryObjective(template)) continue
            refreshTrackedQuestProgress(p0, template, progress)
        }
    }
    private fun setOfferedQuestProgress(playerId: UUID, player: Player?, template: ScenarioTemplate): PlayerQuestProgress =
        setCurrentQuestProgress(playerId, player, template, QuestStatus.OFFERED)

    private fun setActiveQuestProgress(playerId: UUID, player: Player?, template: ScenarioTemplate): PlayerQuestProgress =
        setCurrentQuestProgress(playerId, player, template, QuestStatus.ACTIVE)

    private fun setInitialQuestProgress(playerId: UUID, player: Player?, template: ScenarioTemplate): PlayerQuestProgress =
        if (shouldAutoAcceptOnOffer(template))
            setActiveQuestProgress(playerId, player, template)
        else
            setOfferedQuestProgress(playerId, player, template)

    private fun setCurrentQuestProgress(playerId: UUID, player: Player?, template: ScenarioTemplate, status: QuestStatus): PlayerQuestProgress {
        val now = System.currentTimeMillis()
        val matchingProgress = getCurrentQuestProgress(playerId, template.templateId)
        val startedAt = matchingProgress?.startedAt() ?: now
        val currentPhase = resolveQuestPhase(template, status, matchingProgress)

        val objectiveSnapshot = buildObjectiveProgressSnapshot(
            player?.inventory,
            template,
            matchingProgress?.objectiveProgress() ?: emptyMap(),
            currentPhase
        )
        val questVariables = seedQuestStageVariables(
            template,
            status,
            currentPhase,
            matchingProgress?.questVariables() ?: emptyMap(),
            now
        )

        val currentProgress = PlayerQuestProgress(
            template.templateId,
            template.questCode,
            status,
            startedAt,
            0L,
            now,
            currentPhase,
            objectiveSnapshot,
            questVariables
        )
        putActiveQuestProgress(playerId, currentProgress)
        removeArchivedQuestProgress(playerId, template.templateId)
        persistQuestProgressAsync(playerId, currentProgress)
        return currentProgress
    }
    private fun markQuestCompleted(p0: UUID, p1: ScenarioTemplate) {
        TODO()
    }
    private fun markQuestFailed(playerId: UUID, template: ScenarioTemplate): PlayerQuestProgress {
        val now = System.currentTimeMillis()
        val activeProgress = getCurrentQuestProgress(playerId, template.templateId)
        val startedAt = activeProgress?.startedAt() ?: now

        clearQuestTrackingIfMatches(playerId, template.templateId)
        removeActiveQuestProgress(playerId, template.templateId)
        val failedProgress = PlayerQuestProgress(
            template.templateId,
            template.questCode,
            QuestStatus.FAILED,
            startedAt,
            now,
            now,
            resolveQuestPhase(template, QuestStatus.FAILED, activeProgress),
            activeProgress?.objectiveProgress() ?: buildObjectiveProgressSnapshot(null, template, emptyMap()),
            activeProgress?.questVariables() ?: emptyMap()
        )
        archiveQuestProgress(playerId, failedProgress)
        persistQuestProgressAsync(playerId, failedProgress)
        return failedProgress
    }
    private fun getCurrentQuestProgress(playerId: UUID): List<PlayerQuestProgress> {
        val currentQuests = activePlayerQuests[playerId] ?: return emptyList()
        return currentQuests.values
            .filter { it != null && it.isCurrent() }
            .sortedWith(compareByDescending<PlayerQuestProgress> { it.updatedAt() }.thenBy { it.templateId() ?: "" })
    }

    private fun getCurrentQuestProgress(playerId: UUID?, templateId: String?): PlayerQuestProgress? {
        if (playerId == null || templateId == null || templateId.isBlank()) return null
        val currentQuests = activePlayerQuests[playerId] ?: return null
        return currentQuests[templateId]
    }
    private fun selectQuestProgressForTracking(playerId: UUID?, questReference: String?): PlayerQuestProgress? {
        if (!questReference.isNullOrBlank()) {
            return findQuestProgressByReference(playerId, questReference, false)
        }

        val trackedTemplateId = trackedQuestTemplates[playerId] ?: ""
        if (trackedTemplateId.isNotBlank()) {
            val trackedProgress = getCurrentQuestProgress(playerId, trackedTemplateId)
            if (trackedProgress != null && trackedProgress.isActive()) return trackedProgress
            trackedQuestTemplates.remove(playerId)
        }

        val currentProgresses = getCurrentQuestProgress(playerId ?: return null)
        for (progress in currentProgresses) {
            if (progress.isActive()) return progress
        }
        return currentProgresses.firstOrNull()
    }

    private fun selectQuestProgressForProgress(playerId: UUID?, questReference: String?): PlayerQuestProgress? {
        if (playerId == null) return null

        if (!questReference.isNullOrBlank() && !isTrackedQuestSelector(questReference)) {
            return findQuestProgressByReference(playerId, questReference, true)
        }

        val trackedProgress = getTrackedQuestProgress(playerId, true)
        if (trackedProgress != null) return trackedProgress

        val currentProgresses = getCurrentQuestProgress(playerId)
        if (currentProgresses.isNotEmpty()) return currentProgresses.first()

        return null
    }
    private fun getTrackedQuestProgress(playerId: UUID?, includeArchived: Boolean): PlayerQuestProgress? {
        if (playerId == null) return null

        val trackedTemplateId = trackedQuestTemplates[playerId] ?: ""
        if (trackedTemplateId.isBlank()) return null

        val currentProgress = getCurrentQuestProgress(playerId, trackedTemplateId)
        if (currentProgress != null) return currentProgress

        return if (includeArchived) getArchivedQuestProgress(playerId, trackedTemplateId) else null
    }
    private fun findQuestProgressByReference(playerId: UUID?, questReference: String?, includeArchived: Boolean): PlayerQuestProgress? {
        if (playerId == null || questReference.isNullOrBlank()) return null

        for (progress in getCurrentQuestProgress(playerId)) {
            if (matchesQuestReference(progress, questReference, resolveTemplateForProgress(progress, null))) return progress
        }

        if (!includeArchived) return null

        val archivedQuests = archivedPlayerQuests[playerId] ?: return null

        return archivedQuests.values
            .filter { matchesQuestReference(it, questReference, resolveTemplateForProgress(it, null)) }
            .maxByOrNull { it.updatedAt() }
    }
    private fun isTrackedQuest(playerId: UUID?, progress: PlayerQuestProgress?): Boolean {
        if (playerId == null || progress == null || progress.templateId().isNullOrBlank()) return false
        val trackedTemplateId = trackedQuestTemplates[playerId] ?: ""
        return trackedTemplateId.isNotBlank() && trackedTemplateId == progress.templateId()
    }
    private fun clearQuestTrackingIfMatches(playerId: UUID?, templateId: String?) {
        if (playerId == null || templateId.isNullOrBlank()) return

        val trackedTemplateId = trackedQuestTemplates[playerId] ?: ""
        if (templateId != trackedTemplateId) return

        trackedQuestTemplates.remove(playerId)
        trackedQuestPlayers.remove(playerId)
        persistQuestTrackingPreferenceAsync(playerId, "")
    }
    private fun putActiveQuestProgress(p0: UUID, p1: PlayerQuestProgress) {
        TODO()
    }
    private fun getArchivedQuestProgress(p0: UUID, p1: String): PlayerQuestProgress = TODO()
    private fun getCompletedQuestProgress(p0: UUID, p1: String): PlayerQuestProgress = TODO()
    private fun getFailedQuestProgress(p0: UUID, p1: String): PlayerQuestProgress = TODO()
    private fun evaluateQuestAvailability(p0: UUID, p1: ScenarioTemplate): QuestAvailability = TODO()
    private fun getProgressionMechanicLimit(p0: ScenarioTemplate): Int = TODO()
    private fun countCurrentProgressionsInMechanic(p0: UUID, p1: ScenarioTemplate, p2: String): Int = TODO()
    private fun countCurrentQuestsInCategory(p0: UUID, p1: QuestScenarioContract.Category, p2: String): Int = TODO()
    private fun hasCompletedQuest(p0: UUID, p1: String): Boolean = TODO()
    private fun questLogMatches(p0: UUID, p1: PlayerQuestProgress, p2: QuestLogFilter, p3: Boolean): Boolean = TODO()
    private fun questLogMatchesProgressionKind(p0: PlayerQuestProgress, p1: String): Boolean = TODO()
    private fun buildQuestLogSummaryLines(p0: UUID, p1: List<PlayerQuestProgress>): List<String> = TODO()
    private fun questLogCurrentComparator(p0: UUID): Comparator<PlayerQuestProgress> = TODO()
    private fun questLogCurrentGroupLabel(p0: UUID, p1: ScenarioTemplate?, p2: PlayerQuestProgress): String = TODO()
    private fun formatQuestLogArchivedLine(p0: UUID, p1: ScenarioTemplate?, p2: PlayerQuestProgress, p3: String): String = TODO()
    private fun buildQuestLogActionLines(p0: Player, p1: UUID, p2: ScenarioTemplate?, p3: PlayerQuestProgress, p4: Boolean): List<String> = TODO()
    private fun getRecentArchivedQuestProgress(p0: UUID, p1: Int): List<PlayerQuestProgress> = TODO()
    private fun getArchivedQuestProgress(p0: UUID): List<PlayerQuestProgress> = TODO()
    private fun archiveQuestProgress(p0: UUID, p1: PlayerQuestProgress) {
        TODO()
    }
    private fun removeActiveQuestProgress(p0: UUID, p1: String): Boolean = TODO()
    private fun removeArchivedQuestProgress(p0: UUID, p1: String): Boolean = TODO()
    private fun loadPersistedQuestProgress() {
        TODO()
    }
    private fun registerLoadedQuestProgress(p0: UUID, p1: PlayerQuestProgress): Boolean = TODO()
    private fun registerLoadedTrackedQuest(p0: UUID, p1: PlayerQuestProgress) {
        TODO()
    }
    private fun persistQuestProgressAsync(p0: UUID, p1: PlayerQuestProgress) {
        TODO()
    }
    private fun snapshotQuestProgress(): Map<UUID, List<PlayerQuestProgress>> = TODO()
    private fun persistQuestProgressSnapshot(p0: Map<UUID, List<PlayerQuestProgress>>) {
        TODO()
    }
    private fun persistQuestProgress(p0: UUID, p1: PlayerQuestProgress) {
        TODO()
    }
    private fun persistQuestTrackingPreferenceAsync(p0: UUID, p1: PlayerQuestProgress) {
        TODO()
    }
    private fun persistQuestTrackingPreferenceAsync(p0: UUID, p1: String) {
        TODO()
    }
    private fun persistQuestTrackingPreference(p0: UUID, p1: String) {
        TODO()
    }
    private fun deleteQuestProgressAsync(p0: UUID, p1: String) {
        TODO()
    }
    private fun deleteQuestProgress(p0: UUID, p1: String) {
        TODO()
    }
    private fun persistQuestAnchorsAsync(p0: UUID, p1: PlayerQuestProgress, p2: QuestAnchorResolver.ResolvedQuestAnchors) {
        TODO()
    }
    private fun persistQuestAnchors(p0: UUID, p1: PlayerQuestProgress, p2: QuestAnchorResolver.ResolvedQuestAnchors) {
        TODO()
    }
    private fun resolveQuestAnchors(p0: ScenarioTemplate, p1: Player, p2: AINPC): QuestAnchorResolver.ResolvedQuestAnchors = TODO()
    private fun mergeStoredQuestAnchorVariables(p0: UUID, p1: String, p2: Map<String, String>): Map<String, String> = TODO()
    private fun loadQuestAnchorVariables(p0: UUID, p1: String): Map<String, String> = TODO()
    private fun parseObjectiveProgress(p0: String): Map<String, Int> = TODO()
    private fun parseQuestVariables(p0: String): Map<String, String> = TODO()
    private fun <T> parseJsonMap(p0: String, p1: Type): Map<String, T> = TODO()
    private fun serializeJson(p0: Map<*, *>): String = TODO()
    private fun refreshTrackedQuestProgress(p0: Player, p1: ScenarioTemplate, p2: PlayerQuestProgress): PlayerQuestProgress = TODO()
    private fun trackNpcObjectiveProgress(p0: Player, p1: AINPC, p2: ScenarioTemplate, p3: PlayerQuestProgress): PlayerQuestProgress = TODO()
    private fun buildQuestUnavailableResult(p0: ScenarioTemplate, p1: QuestAnchorResolver.ResolvedQuestAnchors): QuestInteractionResult = TODO()
    private fun buildQuestUnavailableResult(p0: ScenarioTemplate, p1: QuestAvailability): QuestInteractionResult = TODO()
    private fun bindQuestProgressToNpc(p0: UUID, p1: ScenarioTemplate, p2: PlayerQuestProgress, p3: AINPC): PlayerQuestProgress = TODO()
    private fun bindQuestProgressToAnchors(p0: UUID, p1: PlayerQuestProgress, p2: QuestAnchorResolver.ResolvedQuestAnchors): PlayerQuestProgress = TODO()
    private fun updateTrackedQuestProgress(p0: UUID, p1: ScenarioTemplate, p2: PlayerQuestProgress, p3: Map<String, Int>): PlayerQuestProgress = TODO()
    private fun resolveTemplateForProgress(p0: PlayerQuestProgress, p1: AINPC?): ScenarioTemplate = TODO()
    private fun findCurrentRegion(p0: Location): WorldRegion = TODO()
    private fun findCurrentPlace(p0: Location): WorldPlace = TODO()
    private fun findCurrentNode(p0: Location): WorldNode = TODO()
    private fun findQuestTemplateForNpc(p0: AINPC): ScenarioTemplate = TODO()
    private fun findQuestTemplateForNpc(p0: AINPC, p1: UUID): ScenarioTemplate = TODO()
    private fun findQuestTemplateForNpc(p0: AINPC, p1: UUID, p2: String): ScenarioTemplate = TODO()
    private fun resolveCurrentQuestTemplateForNpc(p0: AINPC, p1: UUID): ScenarioTemplate = TODO()
    private fun resolveCurrentQuestTemplateForNpc(p0: AINPC, p1: UUID, p2: String): ScenarioTemplate = TODO()
    private fun resolveCurrentQuestTemplateForNpc(p0: AINPC, p1: PlayerQuestProgress): ScenarioTemplate = TODO()
    private fun matchesActiveQuestNpcObjective(p0: AINPC, p1: ScenarioTemplate, p2: PlayerQuestProgress): Boolean = TODO()
    private fun matchesProgressionKindFilter(p0: ScenarioTemplate, p1: String): Boolean = TODO()
    private fun shouldUseSimpleQuestForAllNpcs(): Boolean = TODO()
    private fun buildSimpleQuestTemplate(p0: AINPC): ScenarioTemplate = TODO()
    private fun resolveSimpleQuestProfile(p0: AINPC, p1: FeaturePackLoader.ProfessionDefinition): SimpleQuestProfile = TODO()
    private fun applyConfiguredSimpleQuestProfile(p0: AINPC, p1: String, p2: String, p3: SimpleQuestProfile): SimpleQuestProfile = TODO()
    private fun resolveProfessionFallbackSection(p0: String, p1: String): ConfigurationSection = TODO()
    private fun resolveConfiguredSimpleQuestTitle(p0: String): String = TODO()
    private fun resolveConfiguredQuestMaterial(p0: String, p1: Material): Material = TODO()
    private fun resolveConfiguredQuestMaterialValue(p0: String, p1: Material): Material = TODO()
    private fun buildQuestBriefingMessages(p0: ScenarioTemplate): List<String> = TODO()
    private fun buildQuestStatusMessages(p0: ScenarioTemplate, p1: PlayerQuestProgress?, p2: Player, p3: String): List<String> = TODO()
    private fun applyQuestStoryActions(p0: Player, p1: AINPC, p2: ScenarioTemplate, p3: PlayerQuestProgress, p4: List<ro.ainpc.engine.FeaturePackLoader.QuestEntryDefinition>): List<String> = TODO()
    private fun resolveStoryActionTarget(p0: FeaturePackLoader.QuestEntryDefinition, p1: Player, p2: PlayerQuestProgress): StoryActionTarget = TODO()
    private fun resolveStoryScopeId(p0: String, p1: String, p2: Player, p3: PlayerQuestProgress): String = TODO()
    private fun resolveStoryAnchorReference(p0: String, p1: String, p2: PlayerQuestProgress): String = TODO()
    private fun resolveQuestVariableAnchorId(p0: PlayerQuestProgress, p1: String, p2: String): String = TODO()
    private fun findFirstQuestAnchorId(p0: PlayerQuestProgress, p1: String): String = TODO()
    private fun resolveRegionIdForPlace(p0: String, p1: Player): String = TODO()
    private fun findCurrentRegionId(p0: Player): String = TODO()
    private fun findCurrentPlaceId(p0: Player): String = TODO()
    fun evaluateScenarioTriggers(p0: List<AINPC>, p1: List<Player>) {
        if (p0.isEmpty()) return
        val random = Random()
        for (template in scenarioTemplates.values) {
            val hasActiveOfType = activeScenarios.values.any { it.type == template.type }
            if (hasActiveOfType) continue
            if (random.nextDouble() < template.triggerProbability
                && canTriggerScenario(template, p0, p1)) {
                startScenario(template, p0, p1)
            }
        }
    }
    private fun startScenario(p0: ScenarioTemplate, p1: List<AINPC>, p2: List<Player>) {
        TODO()
    }
    private fun assignRoles(p0: ActiveScenario, p1: ScenarioTemplate, p2: List<AINPC>, p3: List<Player>): Boolean = TODO()
    private fun assignFallbackRoles(p0: ActiveScenario, p1: List<ScenarioRoleRule>, p2: List<AINPC>, p3: Random, p4: Boolean): Boolean = TODO()
    private fun getQuestSettings(): ConfigurationSection = TODO()
    private fun notifyParticipants(p0: ActiveScenario) {
        TODO()
    }
    private fun adjustEmotionsForRole(p0: AINPC, p1: String, p2: ScenarioType) {
        TODO()
    }
    private fun sendScenarioHint(p0: Player, p1: ActiveScenario) {
        TODO()
    }
    private fun sendQuestBriefing(p0: Player, p1: ActiveScenario) {
        TODO()
    }
    private fun resolveProfessionName(p0: String): String = TODO()
    fun advanceScenario(p0: UUID) {
        val scenario = activeScenarios[p0] ?: return
        val template = scenarioTemplates[scenario.type] ?: return
        val phases = template.phases
        val currentIndex = phases.indexOf(scenario.currentPhase)
        if (currentIndex < phases.size - 1) {
            scenario.currentPhase = phases[currentIndex + 1]
            plugin.debug("Scenariu " + p0.toString().substring(0, 8)
                + " avansat la faza: " + scenario.currentPhase)
        } else {
            endScenario(p0)
        }
    }
    fun endScenario(p0: UUID) {
        val scenario = activeScenarios.remove(p0) ?: return
        createScenarioMemories(scenario)
        plugin.logger.info("Scenariu terminat: " + scenario.displayName
            + " (ID: " + p0.toString().substring(0, 8) + ")")
    }
    private fun createScenarioMemories(p0: ActiveScenario) {
        TODO()
    }
    fun getActiveScenarios(): Map<UUID, ActiveScenario> = HashMap(activeScenarios)
    fun getNPCScenario(p0: UUID): ActiveScenario? {
        for (scenario in activeScenarios.values) {
            if (scenario.hasNPCRole(p0)) {
                return scenario
            }
        }
        return null
    }
}