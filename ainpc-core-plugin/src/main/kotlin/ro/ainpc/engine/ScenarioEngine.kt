@file:Suppress("SENSELESS_COMPARISON")
package ro.ainpc.engine

import com.google.gson.Gson
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import ro.ainpc.AINPCPlugin
import ro.ainpc.api.events.AINPCEventSource
import ro.ainpc.api.events.quest.ProgressionAbandonedEvent
import ro.ainpc.api.events.quest.ProgressionAcceptedEvent
import ro.ainpc.api.events.quest.ProgressionCompletedEvent
import ro.ainpc.api.events.quest.ProgressionDeclinedEvent
import ro.ainpc.api.events.quest.ProgressionEventPayload
import ro.ainpc.api.events.quest.ProgressionObjectiveProgressEvent
import ro.ainpc.api.events.quest.ProgressionObjectiveProgressEventPayload
import ro.ainpc.api.events.quest.ProgressionAnchorBinding
import ro.ainpc.api.events.quest.ProgressionAnchorBoundEvent
import ro.ainpc.api.events.quest.ProgressionAnchorBoundEventPayload
import ro.ainpc.api.events.quest.ProgressionOfferEvent
import ro.ainpc.api.events.quest.ProgressionOfferEventPayload
import ro.ainpc.api.events.quest.ProgressionStageChangedEvent
import ro.ainpc.api.events.quest.ProgressionStageChangedEventPayload
import ro.ainpc.api.events.quest.ProgressionTrackingChangedEvent
import ro.ainpc.api.events.quest.ProgressionTrackingChangedEventPayload
import ro.ainpc.api.events.quest.ProgressionFailedEvent
import ro.ainpc.api.events.quest.ProgressionFailedEventPayload
import ro.ainpc.api.events.quest.ProgressionLifecycleEvent
import ro.ainpc.api.events.story.StoryActionAppliedEvent
import ro.ainpc.api.events.story.StoryActionAppliedEventPayload
import ro.ainpc.npc.AINPC
import ro.ainpc.npc.NpcLifecycleType
import ro.ainpc.npc.NpcPersistenceMode
import ro.ainpc.npc.NpcScenarioActorDefinition
import ro.ainpc.npc.NpcSpawnPolicy
import ro.ainpc.engine.FeaturePackLoader.ProfessionDefinition
import ro.ainpc.engine.FeaturePackLoader.QuestEntryDefinition
import ro.ainpc.engine.FeaturePackLoader.ScenarioDefinition
import ro.ainpc.engine.QuestAnchorResolver.ResolvedQuestAnchors
import ro.ainpc.engine.QuestScenarioContract.Category
import ro.ainpc.story.StoryContextService
import ro.ainpc.story.StoryContextSnapshot
import ro.ainpc.world.WorldNode
import ro.ainpc.world.WorldPlace
import ro.ainpc.engine.runtime.ScenarioActionRegistry
import ro.ainpc.engine.runtime.ScenarioConditionRegistry
import ro.ainpc.engine.runtime.ScenarioExecutionContext
import ro.ainpc.engine.runtime.ScenarioRuntimeDefinition
import ro.ainpc.engine.runtime.ScenarioTriggerRegistry
import ro.ainpc.engine.runtime.ObjectiveContext
import ro.ainpc.engine.runtime.ObjectiveHandlerRegistry
import ro.ainpc.engine.runtime.ObjectiveResult
import ro.ainpc.engine.runtime.objectivehandlers.EquipItemObjectiveHandler
import ro.ainpc.engine.runtime.objectivehandlers.UseItemObjectiveHandler
import ro.ainpc.engine.runtime.actions.GiveItemAction
import ro.ainpc.engine.runtime.actions.SetStoryStateAction
import ro.ainpc.engine.runtime.conditions.HasCompletedQuestCondition
import ro.ainpc.engine.runtime.triggers.PlayerEntersRegionTrigger
import ro.ainpc.world.WorldRegion
import java.lang.reflect.Type
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

class ScenarioEngine(private val plugin: AINPCPlugin) {
    val actionRegistry: ScenarioActionRegistry = ScenarioActionRegistry()
    val conditionRegistry: ScenarioConditionRegistry = ScenarioConditionRegistry()
    val triggerRegistry: ScenarioTriggerRegistry = ScenarioTriggerRegistry()
    val objectiveHandlerRegistry: ObjectiveHandlerRegistry = ObjectiveHandlerRegistry()
    private val scenarioTemplates = LinkedHashMap<ScenarioType, ScenarioTemplate>()
    private val questTemplates = LinkedHashMap<String, ScenarioTemplate>()
    private val gson = Gson()
    private val activePlayerQuests = ConcurrentHashMap<UUID, MutableMap<String, PlayerQuestProgress>>()
    private val archivedPlayerQuests = ConcurrentHashMap<UUID, MutableMap<String, PlayerQuestProgress>>()
    private val questCompletionLocks = ConcurrentHashMap.newKeySet<String>()
    private val sentCompletionMessages = ConcurrentHashMap.newKeySet<String>()
    val questDefinitions = ArrayList<ScenarioDefinition>()
    var storyContextService: StoryContextService? = null
    private val trackedQuestPlayers = HashSet<UUID>()
    private val trackedQuestTemplates = ConcurrentHashMap<UUID, String>()
    private val activeScenarios = HashMap<UUID, ActiveScenario>()
    private val npcConversationCooldowns = ConcurrentHashMap<UUID, Long>()
    private val trackedBlockLocations = ConcurrentHashMap<UUID, MutableSet<String>>()
    private val trackedVisitedPlaces = ConcurrentHashMap<UUID, MutableSet<String>>()
    private val regionEntryCounts = ConcurrentHashMap<UUID, MutableMap<String, Int>>()
    private val eventDebounceBuffer = ConcurrentHashMap<String, Long>()

    init {
        initSimpleQuestPlugin(plugin)
        initTrackingResolutionPlugin(plugin)
        registerRuntimeHandlers()
        loadScenarioTemplates()
    }

    private fun registerRuntimeHandlers() {
        actionRegistry.register(GiveItemAction())
        actionRegistry.register(SetStoryStateAction())
        conditionRegistry.register(HasCompletedQuestCondition())
        triggerRegistry.register(PlayerEntersRegionTrigger())
        objectiveHandlerRegistry.register(
            ro.ainpc.engine.runtime.objectivehandlers.UseItemObjectiveHandler()
        )
        objectiveHandlerRegistry.register(
            ro.ainpc.engine.runtime.objectivehandlers.CollectItemObjectiveHandler()
        )
        objectiveHandlerRegistry.register(
            ro.ainpc.engine.runtime.objectivehandlers.DeliverToNpcObjectiveHandler()
        )
        objectiveHandlerRegistry.register(
            ro.ainpc.engine.runtime.objectivehandlers.KillMobObjectiveHandler()
        )
        objectiveHandlerRegistry.register(
            ro.ainpc.engine.runtime.objectivehandlers.VisitRegionObjectiveHandler()
        )
        objectiveHandlerRegistry.register(
            ro.ainpc.engine.runtime.objectivehandlers.VisitPlaceObjectiveHandler()
        )
        objectiveHandlerRegistry.register(
            ro.ainpc.engine.runtime.objectivehandlers.TalkToNpcObjectiveHandler()
        )
        objectiveHandlerRegistry.register(
            ro.ainpc.engine.runtime.objectivehandlers.BreakBlockObjectiveHandler()
        )
        objectiveHandlerRegistry.register(
            ro.ainpc.engine.runtime.objectivehandlers.PlaceBlockObjectiveHandler()
        )
        objectiveHandlerRegistry.register(
            ro.ainpc.engine.runtime.objectivehandlers.CraftItemObjectiveHandler()
        )
        objectiveHandlerRegistry.register(
            ro.ainpc.engine.runtime.objectivehandlers.InspectNodeObjectiveHandler()
        )
        objectiveHandlerRegistry.register(
            ro.ainpc.engine.runtime.objectivehandlers.EquipItemObjectiveHandler()
        )
    }

    fun reloadTemplates() {
        loadScenarioTemplates()
    }

    fun reloadSingleQuest(templateId: String): Boolean {
        plugin.featurePackLoader.loadAllPacks()
        val updated = plugin.featurePackLoader.getAllScenarios().find {
            it.questCode.equals(templateId, ignoreCase = true) || it.id.equals(templateId, ignoreCase = true)
        } ?: return false
        val existing = questTemplates.values.find {
            it.questCode.equals(templateId, ignoreCase = true) || it.templateId.equals(templateId, ignoreCase = true)
        }
        if (existing != null) {
            existing.description = updated.description
            existing.displayName = updated.name
            existing.objectives = updated.objectives?.map { obj ->
                FeaturePackLoader.QuestEntryDefinition(
                    obj.type, obj.itemId, obj.amount, obj.description
                )
            } ?: existing.objectives
            existing.rewards = updated.rewards?.map { reward ->
                FeaturePackLoader.QuestEntryDefinition(
                    reward.type, reward.itemId, reward.amount, reward.description
                )
            } ?: existing.rewards
        }
        return true
    }

    fun flushQuestProgress() {
        val snapshot = snapshotQuestProgress()
        val total = snapshot.values.sumOf { it.size }
        persistQuestProgressSnapshot(snapshot)
        plugin.debug("[QuestEngine] Flush progresii: $total progresii in memorie.")
    }

    fun executeRuntimeAction(actionDef: ScenarioRuntimeDefinition?, context: ScenarioExecutionContext) {
        if (actionDef == null) return
        val report = actionRegistry.validateDefinition(actionDef, "actiune")
        if (!report.isValid()) {
            plugin.debug("[Runtime] Actiune invalida: ${actionDef.id()} - erori: ${report.errors().joinToString("; ")}")
            return
        }
        actionRegistry.find(actionDef.type()).ifPresent { handler ->
            handler.execute(context, actionDef)
        }
    }
    private fun loadScenarioTemplates() {
        templatesLoadedAt = System.currentTimeMillis()
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
                template.nextQuest = definition.nextQuest
                template.questDialogues = LinkedHashMap(definition.questDialogues)
                template.questActorTriggers = LinkedHashMap(
                    definition.questActorTriggers.mapValues { entry -> LinkedHashSet(entry.value) }
                )
                template.validationWarnings = ArrayList(definition.validationWarnings)
                template.validationWarningDetails = ArrayList(definition.validationWarningDetails)
                template.questStages = definition.questStages
                template.actors = LinkedHashMap(definition.actors)
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
            offeredProgress = bindQuestProgressToAnchors(p0, offeredProgress, resolvedAnchors)
            publishProgressionOffered(p0, p1, template, offeredProgress, availability, "npc_interaction")
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
            val completedProgress = markQuestCompleted(playerId, template)
            publishProgressionCompleted(AINPCEventSource.PLAYER, p0, p1, template, completedProgress)
            triggerQuestActors(template, QuestActorTriggers.ON_COMPLETE, p0, p1)
            triggerQuestActors(template, QuestActorTriggers.ON_RETURN_TO_GIVER, p0, p1)
            advanceToNextChainedQuest(p0, template)
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
        acceptedProgress = bindQuestProgressToAnchors(p0, acceptedProgress, resolvedAnchors)
        publishProgressionAccepted(p0, p1, template, acceptedProgress)
        triggerQuestActors(template, QuestActorTriggers.ON_ACCEPT, p0, p1)
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
        publishProgressionDeclined(p0, p1, template, currentProgress)
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
        publishProgressionAbandoned(AINPCEventSource.PLAYER, p0, p1, template, failedProgress)
        cleanupQuestActors(template, QuestActorTriggers.ON_FAIL)
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
        publishProgressionAbandoned(AINPCEventSource.COMMAND, p0, null, template, failedProgress)
        cleanupQuestActors(template, QuestActorTriggers.ON_FAIL)
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
        offeredProgress = bindQuestProgressToAnchors(p0, offeredProgress, resolvedAnchors)
        publishProgressionOffered(p0, p1, template, offeredProgress, availability, "manual_start")
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
        cleanupQuestActors(template, QuestActorTriggers.ON_RESET)
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
            val completedProgress = markQuestCompleted(playerId, template)
            publishProgressionCompleted(AINPCEventSource.COMMAND, p0, p1, template, completedProgress)
            triggerQuestActors(template, QuestActorTriggers.ON_COMPLETE, p0, p1)
            triggerQuestActors(template, QuestActorTriggers.ON_RETURN_TO_GIVER, p0, p1)
            advanceToNextChainedQuest(p0, template)
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
            if (questGiver != null && template != null && matchesProgressionKindFilter(template, "")) {
                return questGiver
            }
        }
        return null
    }
    fun resolveActiveQuestNpc(p0: Player, p1: String): AINPC? = resolveActiveQuestNpc(p0, p1, null)
    fun resolveActiveQuestNpc(p0: Player, p1: AINPC): AINPC? = resolveActiveQuestNpc(p0, "", p1)
    fun resolveActiveQuestNpc(p0: Player, p1: String, p2: AINPC?): AINPC? {
        if (p0 == null) return null
        for (progress in getCurrentQuestProgress(p0.uniqueId)) {
            if (!progress.isCurrent()) continue
            val questGiver = resolveQuestGiverNpc(progress)
            val template = resolveTemplateForProgress(progress, null)
            if (questGiver != null && template != null && matchesProgressionKindFilter(template, p1)) {
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
            val storyContext = plugin.storyContextService.buildForPlayer(p0)
            val authoringSnapshot = plugin.authoringService.analyze(
                storyContext,
                plugin.progressionService.getDefinitions(),
                p1,
                template.progressionMechanicId,
                storyContext.worldContext().currentRegion()?.id(),
                storyContext.worldContext().currentPlace()?.id(),
                true,
                emptyList()
            )
            systemMessages.add("&eAuthoring: &f" + authoringSnapshot.decisionStatus()
                + " &7/ reason=&f" + formatOptional(authoringSnapshot.decisionReason()))
            if (authoringSnapshot.selectedProgressionId().isNotBlank()) {
                systemMessages.add("&7Authoring progression: &f" + authoringSnapshot.selectedProgressionId()
                    + " &7/ mechanic=&f" + formatOptional(authoringSnapshot.selectedMechanicId()))
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
        publishProgressionTrackingChanged(
            p0,
            selectedProgress,
            marker,
            true,
            "START",
            mapOf(
                "trackingReference" to p1,
                "trackingMode" to "manual"
            )
        )
        return marker
    }
    fun stopQuestTracking(p0: Player): Boolean {
        if (p0 == null) {
            return false
        }
        val trackedTemplateId = trackedQuestTemplates[p0.uniqueId] ?: ""
        val selectedProgress = selectQuestProgressForTracking(p0.uniqueId, trackedTemplateId)
        val marker = getQuestTrackingMarker(p0, trackedTemplateId)
        val hadTemplate = trackedQuestTemplates.remove(p0.uniqueId) != null
        val stopped = trackedQuestPlayers.remove(p0.uniqueId)
        if (stopped || hadTemplate) {
            persistQuestTrackingPreferenceAsync(p0.uniqueId, "")
            publishProgressionTrackingChanged(
                p0,
                selectedProgress,
                marker,
                false,
                "STOP",
                mapOf(
                    "trackingReference" to trackedTemplateId,
                    "trackingMode" to "manual"
                )
            )
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
    }
    private fun spawnQuestDirectionParticles(p0: Player, p1: Location, p2: Double) {
    }
    private fun spawnQuestWaypointParticles(p0: Player, p1: Location) {
    }
    fun recordNpcConversation(p0: Player, p1: AINPC) {
        if (p0 == null || p1 == null) return
        val now = System.currentTimeMillis()
        val lastTalk = npcConversationCooldowns.getOrDefault(p0.uniqueId, 0L)
        if (now - lastTalk < 3000) return
        npcConversationCooldowns[p0.uniqueId] = now
        for (progress in getCurrentQuestProgress(p0.uniqueId)) {
            if (!progress.isCurrent()) continue
            val template = resolveTemplateForProgress(progress, p1)
            if (template == null) continue
            val refreshedProgress = refreshTrackedQuestProgress(p0, template, progress)
            trackNpcObjectiveProgress(p0, p1, template, refreshedProgress)
        }
    }
    fun recordNodeInteraction(p0: Player) {
        if (p0 == null) return
        val node = findCurrentNode(p0.location) ?: return
        for (progress in getCurrentQuestProgress(p0.uniqueId)) {
            if (!progress.isCurrent()) continue
            val template = resolveTemplateForProgress(progress, null)
            if (template == null || !hasObjectiveType(template, "inspect_node")) continue
            val updatedProgress = LinkedHashMap(progress.objectiveProgress())
            var changed = false
            for ((index, objective) in template.objectives.withIndex()) {
                if (!isObjectiveActiveForProgress(template, progress, objective)) continue
                if (!matchesObjectiveType(objective, "inspect_node")) continue
                if (!matchesNodeObjective(progress, objective, index, node)) continue
                val objectiveKey = buildObjectiveKey(objective, index)
                val before = updatedProgress.getOrDefault(objectiveKey, 0)
                changed = changed or carryLegacyObjectiveProgress(updatedProgress, objective, index)
                changed = changed or incrementObjectiveProgress(updatedProgress, objectiveKey, objective.amount)
                if (changed && updatedProgress.getOrDefault(objectiveKey, 0) > before) {
                    emitProgressionObjectiveProgress(
                        player = p0, npc = null, template = template, progress = progress,
                        objective = objective, objectiveKey = objectiveKey,
                        before = before, after = updatedProgress.getOrDefault(objectiveKey, 0),
                        trigger = "inspect_node",
                        metadata = linkedMapOf("nodeId" to (node.id ?: ""))
                    )
                }
            }
            if (changed) updateTrackedQuestProgress(p0.uniqueId, template, progress, updatedProgress)
        }
    }

    fun recordRegionVisit(p0: Player) {
        if (p0 == null || isDebounced(p0.uniqueId, "regionVisit")) return
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
                val before = updatedProgress.getOrDefault(objectiveKey, 0)
                changed = changed or carryLegacyObjectiveProgress(updatedProgress, objective, index)
                val matchesLocationObjective =
                    (matchesObjectiveType(objective, "visit_region") && matchesRegionObjective(progress, objective, index, region))
                        || (matchesObjectiveType(objective, "visit_place") && matchesPlaceObjective(progress, objective, index, place))
                        || (matchesObjectiveType(objective, "inspect_node") && matchesNodeObjective(progress, objective, index, node))
                if (!matchesLocationObjective) continue
                if (matchesObjectiveType(objective, "visit_place")) {
                    val placeKey = "${p0.uniqueId}:${objectiveKey}:${place?.id.orEmpty()}"
                    val visited = trackedVisitedPlaces.getOrPut(p0.uniqueId) { HashSet() }
                    if (!visited.add(placeKey)) continue
                }
                if (matchesObjectiveType(objective, "inspect_node") && objective.metadata["interact_node"] == "true") {
                    continue
                }
                if (matchesObjectiveType(objective, "visit_region")) {
                    val minEntries = objective.metadata["min_entries"]?.toIntOrNull() ?: 1
                    if (minEntries > 1) {
                        val regionId = region?.id.orEmpty()
                        val entryKey = "${objectiveKey}:${regionId}"
                        val entries = regionEntryCounts.getOrPut(p0.uniqueId) { HashMap() }
                        val current = entries.getOrDefault(entryKey, 0) + 1
                        entries[entryKey] = current
                        if (current < minEntries) continue
                    }
                }
                changed = changed or incrementObjectiveProgress(updatedProgress, objectiveKey, objective.amount)
                val after = updatedProgress.getOrDefault(objectiveKey, 0)
                if (after > before) {
                    emitProgressionObjectiveProgress(
                        player = p0,
                        npc = null,
                        template = template,
                        progress = progress,
                        objective = objective,
                        objectiveKey = objectiveKey,
                        before = before,
                        after = after,
                        trigger = objective.type.orEmpty(),
                        metadata = linkedMapOf(
                            "regionId" to (region?.id.orEmpty()),
                            "placeId" to (place?.id.orEmpty()),
                            "nodeId" to (node?.id.orEmpty())
                        )
                    )
                }
                if (after >= objective.amount && before < objective.amount) {
                    triggerQuestActors(template, QuestActorTriggers.ON_OBJECTIVE_COMPLETE, p0, null)
                }
                if (after >= objective.amount && sentCompletionMessages.add(p0.uniqueId.toString() + ":" + objectiveKey)) {
                    val msg = net.kyori.adventure.text.Component.text(
                        "✓ " + objective.description,
                        net.kyori.adventure.text.format.NamedTextColor.GREEN
                    )
                    p0.sendMessage(msg)
                }
            }
            if (changed) {
                updateTrackedQuestProgress(p0.uniqueId, template, progress, updatedProgress)
            }
        }
    }
    fun recordMobKill(p0: Player, p1: Entity) {
        if (p0 == null || p1 == null || isDebounced(p0.uniqueId, "mobKill")) return
        for (progress in getCurrentQuestProgress(p0.uniqueId)) {
            if (!progress.isCurrent()) continue
            val template = resolveTemplateForProgress(progress, null)
            if (template == null || !hasObjectiveType(template, "kill_mob")) continue
            if (processObjectiveViaHandlers(p0.uniqueId, template, progress, "kill_mob")) continue
            val updatedProgress = LinkedHashMap(progress.objectiveProgress())
            var changed = false
            val objectives = template.objectives
            for (index in objectives.indices) {
                val objective = objectives[index]
                if (!isObjectiveActiveForProgress(template, progress, objective)) continue
                if (!matchesObjectiveType(objective, "kill_mob") || !matchesMobObjective(objective, p1)) continue
                val objectiveKey = buildObjectiveKey(objective, index)
                val before = updatedProgress.getOrDefault(objectiveKey, 0)
                changed = changed or carryLegacyObjectiveProgress(updatedProgress, objective, index)
                changed = changed or incrementObjectiveProgress(updatedProgress, objectiveKey, objective.amount)
                val after = updatedProgress.getOrDefault(objectiveKey, 0)
                if (after > before) {
                    emitProgressionObjectiveProgress(
                        player = p0,
                        npc = null,
                        template = template,
                        progress = progress,
                        objective = objective,
                        objectiveKey = objectiveKey,
                        before = before,
                        after = after,
                        trigger = "kill_mob",
                        metadata = linkedMapOf(
                            "entityType" to p1.type.name,
                            "entityName" to p1.name.orEmpty()
                        )
                    )
                }
                if (after >= objective.amount && before < objective.amount) {
                    triggerQuestActors(template, QuestActorTriggers.ON_OBJECTIVE_COMPLETE, p0, null)
                }
            }
            if (changed) {
                updateTrackedQuestProgress(p0.uniqueId, template, progress, updatedProgress)
            }
        }
    }
    fun recordItemCrafted(p0: Player, p1: Material?) {
        if (p0 == null || p1 == null || isDebounced(p0.uniqueId, "craft")) return
        for (progress in getCurrentQuestProgress(p0.uniqueId)) {
            if (!progress.isCurrent()) continue
            val template = resolveTemplateForProgress(progress, null)
            if (template == null || !hasObjectiveType(template, "craft_item")) continue
            if (processObjectiveViaHandlers(p0.uniqueId, template, progress, "craft_item")) continue
            incrementCraftObjective(p0, template, progress, p1.name)
        }
    }
    private fun incrementCraftObjective(p0: Player, template: ScenarioTemplate, progress: PlayerQuestProgress, itemName: String) {
        val updatedProgress = LinkedHashMap(progress.objectiveProgress())
        var changed = false
        for ((index, objective) in template.objectives.withIndex()) {
            if (!matchesObjectiveType(objective, "craft_item")) continue
            if (!isObjectiveActiveForProgress(template, progress, objective)) continue
            if (!matchesObjectiveReference(objective.itemId, itemName)) continue
            val allowedRecipes = objective.metadata["allowed_recipes"]
            if (allowedRecipes != null && allowedRecipes.split(",").none { it.trim().equals(itemName, ignoreCase = true) }) continue
            val objectiveKey = buildObjectiveKey(objective, index)
            val before = updatedProgress.getOrDefault(objectiveKey, 0)
            changed = changed or carryLegacyObjectiveProgress(updatedProgress, objective, index)
            changed = changed or incrementObjectiveProgress(updatedProgress, objectiveKey, objective.amount)
            val after = updatedProgress.getOrDefault(objectiveKey, 0)
            if (after > before) {
                emitProgressionObjectiveProgress(p0, null, template, progress, objective, objectiveKey, before, after, "craft_item", linkedMapOf("item" to itemName))
            }
            if (after >= objective.amount && before < objective.amount) {
                triggerQuestActors(template, QuestActorTriggers.ON_OBJECTIVE_COMPLETE, p0, null)
            }
        }
        if (changed) updateTrackedQuestProgress(p0.uniqueId, template, progress, updatedProgress)
    }

    fun recordBlockPlaced(p0: Player, p1: Material) {
        if (p0 == null) return
        for (progress in getCurrentQuestProgress(p0.uniqueId)) {
            if (!progress.isCurrent()) continue
            val template = resolveTemplateForProgress(progress, null)
            if (template == null || !hasObjectiveType(template, "place_block")) continue
            if (processObjectiveViaHandlers(p0.uniqueId, template, progress, "place_block")) continue
            incrementBlockObjective(p0, template, progress, "place_block", p1.name)
        }
    }
    fun recordBlockPlaced(p0: Player, p1: Material, p2: org.bukkit.Location) {
        if (p0 == null) return
        val locationKey = "${p2.world.name}:${p2.blockX}:${p2.blockY}:${p2.blockZ}"
        val placed = trackedBlockLocations.getOrPut(p0.uniqueId) { HashSet() }
        if (!placed.add(locationKey)) return
        for (progress in getCurrentQuestProgress(p0.uniqueId)) {
            if (!progress.isCurrent()) continue
            val template = resolveTemplateForProgress(progress, null)
            if (template == null || !hasObjectiveType(template, "place_block")) continue
            incrementBlockObjective(p0, template, progress, "place_block", p1.name)
        }
    }
    fun recordBlockBroken(p0: Player, p1: Material) {
        if (p0 == null) return
        for (progress in getCurrentQuestProgress(p0.uniqueId)) {
            if (!progress.isCurrent()) continue
            val template = resolveTemplateForProgress(progress, null)
            if (template == null || !hasObjectiveType(template, "break_block")) continue
            incrementBlockObjective(p0, template, progress, "break_block", p1.name)
        }
    }
    fun recordBlockBroken(p0: Player, p1: Material, p2: org.bukkit.Location) {
        if (p0 == null) return
        val locationKey = "${p2.world.name}:${p2.blockX}:${p2.blockY}:${p2.blockZ}"
        var isConfirmed = false
        for (progress in getCurrentQuestProgress(p0.uniqueId)) {
            if (!progress.isCurrent()) continue
            val template = resolveTemplateForProgress(progress, null)
            if (template == null || !hasObjectiveType(template, "break_block")) continue
            for ((_, objective) in template.objectives.withIndex()) {
                if (!isObjectiveActiveForProgress(template, progress, objective)) continue
                if (!matchesObjectiveType(objective, "break_block")) continue
                if (objective.metadata["confirm_target"] != "true") { isConfirmed = true; break }
                val brokenKey = "broken:${locationKey}"
                val confirmed = trackedBlockLocations.getOrPut(p0.uniqueId) { HashSet() }
                if (confirmed.add(brokenKey)) { isConfirmed = true; break }
            }
        }
        if (isConfirmed) recordBlockBroken(p0, p1)
    }
    private fun incrementBlockObjective(p0: Player, template: ScenarioTemplate, progress: PlayerQuestProgress, objectiveType: String, blockName: String) {
        val updatedProgress = LinkedHashMap(progress.objectiveProgress())
        var changed = false
        for ((index, objective) in template.objectives.withIndex()) {
            if (!matchesObjectiveType(objective, objectiveType)) continue
            if (!isObjectiveActiveForProgress(template, progress, objective)) continue
            if (!matchesObjectiveReference(objective.itemId, blockName)) continue
            val objectiveKey = buildObjectiveKey(objective, index)
            val before = updatedProgress.getOrDefault(objectiveKey, 0)
            changed = changed or carryLegacyObjectiveProgress(updatedProgress, objective, index)
            changed = changed or incrementObjectiveProgress(updatedProgress, objectiveKey, objective.amount)
        }
        if (changed) updateTrackedQuestProgress(p0.uniqueId, template, progress, updatedProgress)
    }
    fun recordItemUsed(p0: Player, p1: Material?) {
        if (p0 == null || p1 == null || isDebounced(p0.uniqueId, "itemUse")) return
        for (progress in getCurrentQuestProgress(p0.uniqueId)) {
            if (!progress.isCurrent()) continue
            val template = resolveTemplateForProgress(progress, null)
            if (template == null || !hasObjectiveType(template, "use_item")) continue
            incrementItemUseObjective(p0, template, progress, p1.name)
        }
    }
    private fun incrementItemUseObjective(p0: Player, template: ScenarioTemplate, progress: PlayerQuestProgress, itemName: String) {
        val updatedProgress = LinkedHashMap(progress.objectiveProgress())
        var changed = false
        for ((index, objective) in template.objectives.withIndex()) {
            if (!matchesObjectiveType(objective, "use_item")) continue
            if (!isObjectiveActiveForProgress(template, progress, objective)) continue
            if (!matchesObjectiveReference(objective.itemId, itemName)) continue
            val objectiveKey = buildObjectiveKey(objective, index)
            val before = updatedProgress.getOrDefault(objectiveKey, 0)
            changed = changed or carryLegacyObjectiveProgress(updatedProgress, objective, index)
            changed = changed or incrementObjectiveProgress(updatedProgress, objectiveKey, objective.amount)
            val after = updatedProgress.getOrDefault(objectiveKey, 0)
            if (after > before) {
                emitProgressionObjectiveProgress(p0, null, template, progress, objective, objectiveKey, before, after, "use_item", linkedMapOf("item" to itemName))
            }
            if (after >= objective.amount && before < objective.amount) {
                triggerQuestActors(template, QuestActorTriggers.ON_OBJECTIVE_COMPLETE, p0, null)
            }
        }
        if (changed) {
            updateTrackedQuestProgress(p0.uniqueId, template, progress, updatedProgress)
        }
    }
    fun recordItemEquipped(p0: Player, p1: Material?) {
        if (p0 == null || p1 == null || isDebounced(p0.uniqueId, "equip")) return
        for (progress in getCurrentQuestProgress(p0.uniqueId)) {
            if (!progress.isCurrent()) continue
            val template = resolveTemplateForProgress(progress, null)
            if (template == null || !hasObjectiveType(template, "equip_item")) continue
            incrementEquipObjective(p0, template, progress, p1.name)
        }
    }
    private fun incrementEquipObjective(p0: Player, template: ScenarioTemplate, progress: PlayerQuestProgress, itemName: String) {
        val updatedProgress = LinkedHashMap(progress.objectiveProgress())
        var changed = false
        for ((index, objective) in template.objectives.withIndex()) {
            if (!matchesObjectiveType(objective, "equip_item")) continue
            if (!isObjectiveActiveForProgress(template, progress, objective)) continue
            if (!matchesObjectiveReference(objective.itemId, itemName)) continue
            val objectiveKey = buildObjectiveKey(objective, index)
            val before = updatedProgress.getOrDefault(objectiveKey, 0)
            changed = changed or carryLegacyObjectiveProgress(updatedProgress, objective, index)
            changed = changed or incrementObjectiveProgress(updatedProgress, objectiveKey, objective.amount)
            val after = updatedProgress.getOrDefault(objectiveKey, 0)
            if (after > before) {
                emitProgressionObjectiveProgress(p0, null, template, progress, objective, objectiveKey, before, after, "equip_item", linkedMapOf("item" to itemName))
            }
            if (after >= objective.amount && before < objective.amount) {
                triggerQuestActors(template, QuestActorTriggers.ON_OBJECTIVE_COMPLETE, p0, null)
            }
        }
        if (changed) {
            updateTrackedQuestProgress(p0.uniqueId, template, progress, updatedProgress)
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
        val previousPhase = matchingProgress?.currentPhase().orEmpty()

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
        if (previousPhase.isNotBlank() && previousPhase != currentPhase && player != null) {
            publishProgressionStageChanged(
                AINPCEventSource.PLAYER,
                player,
                null,
                template,
                currentProgress,
                previousPhase,
                currentPhase,
                "quest_state_rebuild",
                emptyMap()
            )
        }
        return currentProgress
    }
    private fun markQuestCompleted(p0: UUID, p1: ScenarioTemplate): PlayerQuestProgress {
        val now = System.currentTimeMillis()
        val activeProgress = getCurrentQuestProgress(p0, p1.templateId)
        val startedAt = activeProgress?.startedAt() ?: now
        clearQuestTrackingIfMatches(p0, p1.templateId)
        removeActiveQuestProgress(p0, p1.templateId)
        clearQuestTrackingData(p0)
        val completedProgress = PlayerQuestProgress(
            p1.templateId,
            p1.questCode,
            QuestStatus.COMPLETED,
            startedAt,
            now,
            now,
            resolveQuestPhase(p1, QuestStatus.COMPLETED, activeProgress),
            activeProgress?.objectiveProgress() ?: emptyMap(),
            activeProgress?.questVariables() ?: emptyMap()
        )
        archiveQuestProgress(p0, completedProgress)
        persistQuestProgressAsync(p0, completedProgress)
        return completedProgress
    }
    private fun clearQuestTrackingData(playerId: UUID) {
        trackedBlockLocations.remove(playerId)
        trackedVisitedPlaces.remove(playerId)
        npcConversationCooldowns.remove(playerId)
        regionEntryCounts.remove(playerId)
        eventDebounceBuffer.keys.removeIf { it.startsWith("$playerId:") }
    }

    fun cleanupOrphanedObjectives() {
        val onlinePlayerIds = plugin.server.onlinePlayers.map { it.uniqueId }.toSet()
        activePlayerQuests.keys.removeIf { it !in onlinePlayerIds }
        archivedPlayerQuests.keys.removeIf { it !in onlinePlayerIds }
        sentCompletionMessages.removeIf { key -> key.split(":").firstOrNull()?.let { uid ->
            runCatching { UUID.fromString(uid) }.getOrNull()?.let { it !in onlinePlayerIds } ?: false } == true }
        trackedQuestPlayers.removeIf { it !in onlinePlayerIds }
        trackedQuestTemplates.keys.removeIf { it !in onlinePlayerIds }
        eventDebounceBuffer.keys.removeIf { key -> key.split(":").firstOrNull()?.let { uid ->
            runCatching { UUID.fromString(uid) }.getOrNull()?.let { it !in onlinePlayerIds } ?: false } == true }
        cleanupStaleTemplateProgress()
    }

    fun cleanupStaleTemplateProgress() {
        val knownTemplateIds = questTemplates.keys.toSet()
        for ((playerId, quests) in activePlayerQuests) {
            val staleKeys = quests.keys.filter { it !in knownTemplateIds }
            for (key in staleKeys) {
                quests.remove(key)
                deleteQuestProgress(playerId, key)
            }
        }
        for ((playerId, quests) in archivedPlayerQuests) {
            val staleKeys = quests.keys.filter { it !in knownTemplateIds }
            for (key in staleKeys) {
                quests.remove(key)
                deleteQuestProgress(playerId, key)
            }
        }
    }

    private fun clearLocationObjectiveProgress(
        progressByObjective: MutableMap<String, Int>?,
        template: ScenarioTemplate,
    ): MutableMap<String, Int> {
        val cleared = progressByObjective?.toMutableMap() ?: LinkedHashMap()
        if (template.objectives.isEmpty()) return cleared
        val locationTypes = setOf("visit_region", "visit_place", "inspect_node")
        for ((index, objective) in template.objectives.withIndex()) {
            if (locationTypes.any { matchesObjectiveType(objective, it) }) {
                val key = buildObjectiveKey(objective, index)
                cleared.remove(key)
            }
        }
        return cleared
    }

    private fun isDebounced(playerId: UUID, eventKey: String): Boolean {
        val fullKey = "$playerId:$eventKey"
        val now = System.currentTimeMillis()
        val last = eventDebounceBuffer.getOrDefault(fullKey, 0L)
        if (now - last < 500) return true
        eventDebounceBuffer[fullKey] = now
        return false
    }

    private fun markQuestFailed(playerId: UUID, template: ScenarioTemplate): PlayerQuestProgress {
        val now = System.currentTimeMillis()
        val activeProgress = getCurrentQuestProgress(playerId, template.templateId)
        val startedAt = activeProgress?.startedAt() ?: now

        clearQuestTrackingIfMatches(playerId, template.templateId)
        removeActiveQuestProgress(playerId, template.templateId)
        clearQuestTrackingData(playerId)
        val clearedProgress = clearLocationObjectiveProgress(activeProgress?.objectiveProgress()?.toMutableMap(), template)
        val failedProgress = PlayerQuestProgress(
            template.templateId,
            template.questCode,
            QuestStatus.FAILED,
            startedAt,
            now,
            now,
            resolveQuestPhase(template, QuestStatus.FAILED, activeProgress),
            clearedProgress,
            activeProgress?.questVariables() ?: emptyMap()
        )
        archiveQuestProgress(playerId, failedProgress)
        persistQuestProgressAsync(playerId, failedProgress)
        publishProgressionFailed(template, failedProgress, "quest_failed")
        return failedProgress
    }

    private fun publishProgressionAccepted(
        player: Player,
        npc: AINPC?,
        template: ScenarioTemplate,
        progress: PlayerQuestProgress
    ) {
        publishProgressionLifecycleEvent(
            ProgressionAcceptedEvent(
                buildProgressionEventPayload(AINPCEventSource.PLAYER, player, npc, template, progress)
            )
        )
    }

    private fun publishProgressionAbandoned(
        source: AINPCEventSource,
        player: Player,
        npc: AINPC?,
        template: ScenarioTemplate,
        progress: PlayerQuestProgress
    ) {
        publishProgressionLifecycleEvent(
            ProgressionAbandonedEvent(buildProgressionEventPayload(source, player, npc, template, progress))
        )
    }

    private fun publishProgressionDeclined(
        player: Player,
        npc: AINPC?,
        template: ScenarioTemplate,
        progress: PlayerQuestProgress
    ) {
        publishProgressionLifecycleEvent(
            ProgressionDeclinedEvent(
                buildProgressionEventPayload(AINPCEventSource.PLAYER, player, npc, template, progress)
            )
        )
    }

    private fun publishProgressionCompleted(
        source: AINPCEventSource,
        player: Player,
        npc: AINPC?,
        template: ScenarioTemplate,
        progress: PlayerQuestProgress
    ) {
        publishProgressionLifecycleEvent(
            ProgressionCompletedEvent(buildProgressionEventPayload(source, player, npc, template, progress))
        )
    }

    private fun publishProgressionLifecycleEvent(event: ProgressionLifecycleEvent) {
        if (!plugin.config.getBoolean("events.public_api_enabled", true)) {
            return
        }
        Bukkit.getPluginManager().callEvent(event)
    }

    private fun triggerQuestActors(
        template: ScenarioTemplate,
        triggerId: String,
        player: Player,
        npc: AINPC?,
    ) {
        val actorIds = template.questActorTriggers[triggerId.trim()] ?: return
        if (actorIds.isEmpty()) {
            return
        }

        val scenarioEntry = findActiveScenarioByTemplateId(template.templateId) ?: return
        val anchorLocation = npc?.location ?: player.location
        if (anchorLocation.world == null) {
            return
        }

        spawnScenarioActors(scenarioEntry.key, anchorLocation, scenarioEntry.value.currentPhase, actorIds, true, true)
    }

    private fun cleanupQuestActors(
        template: ScenarioTemplate,
        triggerId: String,
    ) {
        val actorIds = template.questActorTriggers[triggerId.trim()] ?: return
        if (actorIds.isEmpty()) {
            return
        }

        val scenarioEntry = findActiveScenarioByTemplateId(template.templateId) ?: return
        val scenarioId = scenarioEntry.key
        for (actorId in actorIds) {
            despawnScenarioActor(scenarioId, actorId)
        }
    }

    private fun publishProgressionOffered(
        player: Player,
        npc: AINPC,
        template: ScenarioTemplate,
        progress: PlayerQuestProgress,
        availability: QuestAvailability,
        offerReason: String
    ) {
        if (!plugin.config.getBoolean("events.public_api_enabled", true)) {
            return
        }

        val metadata = LinkedHashMap<String, String>()
        metadata["availability"] = if (availability.available()) "available" else "unavailable"
        metadata["offerReason"] = offerReason
        Bukkit.getPluginManager().callEvent(
            ProgressionOfferEvent(
                ProgressionOfferEventPayload(
                    UUID.randomUUID(),
                    System.currentTimeMillis(),
                    AINPCEventSource.PLAYER,
                    player.uniqueId,
                    player.name,
                    npc.databaseId.takeIf { it > 0 }?.toString(),
                    npc.uuid,
                    npc.name,
                    progress.templateId().orEmpty().ifBlank { template.templateId },
                    progress.templateId().orEmpty().ifBlank { template.templateId },
                    template.progressionKind.ifBlank { template.type.name.lowercase(Locale.ROOT) },
                    template.progressionMechanicId,
                    progress.questCode().orEmpty().ifBlank { template.questCode },
                    progress.currentPhase(),
                    offerReason,
                    availability.available(),
                    metadata
                )
            )
        )
    }

    private fun publishProgressionTrackingChanged(
        player: Player,
        progress: PlayerQuestProgress?,
        marker: QuestTrackingMarker?,
        trackingActive: Boolean,
        trackingAction: String,
        metadata: Map<String, String>
    ) {
        if (!plugin.config.getBoolean("events.public_api_enabled", true)) {
            return
        }

        val template = progress?.let { resolveTemplateForProgress(it, null) }
        val progressionId = progress?.templateId().orEmpty().ifBlank { template?.templateId.orEmpty() }
        val questCode = progress?.questCode().orEmpty().ifBlank { template?.questCode.orEmpty() }
        val stageId = progress?.currentPhase().orEmpty()
        val markerLocation = marker?.location

        Bukkit.getPluginManager().callEvent(
            ProgressionTrackingChangedEvent(
                ProgressionTrackingChangedEventPayload(
                    UUID.randomUUID(),
                    System.currentTimeMillis(),
                    AINPCEventSource.PLAYER,
                    player.uniqueId,
                    player.name,
                    null,
                    null,
                    null,
                    progressionId,
                    progressionId,
                    template?.progressionKind.orEmpty().ifBlank { template?.type?.name?.lowercase(Locale.ROOT).orEmpty() },
                    template?.progressionMechanicId.orEmpty(),
                    questCode,
                    stageId,
                    trackingActive,
                    trackingAction,
                    marker?.objectiveLabel.orEmpty(),
                    marker?.targetLabel.orEmpty(),
                    marker?.anchorType.orEmpty(),
                    marker != null && marker.hasLocation(),
                    markerLocation?.world?.name.orEmpty(),
                    markerLocation?.x ?: 0.0,
                    markerLocation?.y ?: 0.0,
                    markerLocation?.z ?: 0.0,
                    marker?.actionBarMessage.orEmpty(),
                    metadata
                )
            )
        )
    }

    private fun publishProgressionAnchorsBound(
        player: Player,
        progress: PlayerQuestProgress,
        resolvedAnchors: QuestAnchorResolver.ResolvedQuestAnchors,
        metadata: Map<String, String>
    ) {
        if (!plugin.config.getBoolean("events.public_api_enabled", true)) {
            return
        }

        val anchors = resolvedAnchors.anchors()
        if (anchors.isEmpty()) {
            return
        }

        val template = resolveTemplateForProgress(progress, null)
        Bukkit.getPluginManager().callEvent(
            ProgressionAnchorBoundEvent(
                ProgressionAnchorBoundEventPayload(
                    UUID.randomUUID(),
                    System.currentTimeMillis(),
                    AINPCEventSource.PLAYER,
                    player.uniqueId,
                    player.name,
                    progress.templateId().orEmpty().ifBlank { template?.templateId.orEmpty() },
                    progress.templateId().orEmpty().ifBlank { template?.templateId.orEmpty() },
                    template?.progressionKind.orEmpty().ifBlank { template?.type?.name?.lowercase(Locale.ROOT).orEmpty() },
                    template?.progressionMechanicId.orEmpty(),
                    progress.questCode().orEmpty().ifBlank { template?.questCode.orEmpty() },
                    progress.currentPhase(),
                    anchors.size,
                    anchors.map { anchor ->
                        ProgressionAnchorBinding(
                            anchor.objectiveKey(),
                            anchor.objectiveType(),
                            anchor.anchorType(),
                            anchor.anchorId(),
                            anchor.label(),
                            anchor.reference()
                        )
                    },
                    metadata
                )
            )
        )
    }

    private fun emitProgressionObjectiveProgress(
        player: Player,
        npc: AINPC?,
        template: ScenarioTemplate,
        progress: PlayerQuestProgress,
        objective: FeaturePackLoader.QuestEntryDefinition,
        objectiveKey: String,
        before: Int,
        after: Int,
        trigger: String,
        metadata: Map<String, String>
    ) {
        if (after <= before || !plugin.config.getBoolean("events.public_api_enabled", true)) {
            return
        }

        val event = ProgressionObjectiveProgressEvent(
            ProgressionObjectiveProgressEventPayload(
                UUID.randomUUID(),
                System.currentTimeMillis(),
                AINPCEventSource.PLAYER,
                player.uniqueId,
                player.name,
                npc?.databaseId?.takeIf { it > 0 }?.toString(),
                npc?.uuid,
                npc?.name,
                progress.templateId().orEmpty().ifBlank { template.templateId },
                progress.templateId().orEmpty().ifBlank { template.templateId },
                template.progressionKind.ifBlank { template.type.name.lowercase(Locale.ROOT) },
                template.progressionMechanicId,
                progress.questCode().orEmpty().ifBlank { template.questCode },
                progress.currentPhase(),
                objectiveKey,
                objective.type.orEmpty(),
                objective.itemId.orEmpty().ifBlank { objective.description.orEmpty().ifBlank { objective.type.orEmpty() } },
                after,
                maxOf(1, objective.amount),
                after - before,
                after >= objective.amount,
                trigger,
                metadata.toMap()
            )
        )
        Bukkit.getPluginManager().callEvent(event)
    }

    private fun publishProgressionStageChanged(
        source: AINPCEventSource,
        player: Player,
        npc: AINPC?,
        template: ScenarioTemplate,
        progress: PlayerQuestProgress,
        previousStageId: String,
        newStageId: String,
        reason: String,
        metadata: Map<String, String>
    ) {
        if (previousStageId == newStageId || !plugin.config.getBoolean("events.public_api_enabled", true)) {
            return
        }

        Bukkit.getPluginManager().callEvent(
            ProgressionStageChangedEvent(
                ProgressionStageChangedEventPayload(
                    UUID.randomUUID(),
                    System.currentTimeMillis(),
                    source,
                    player.uniqueId,
                    player.name,
                    npc?.databaseId?.takeIf { it > 0 }?.toString(),
                    npc?.uuid,
                    npc?.name,
                    progress.templateId().orEmpty().ifBlank { template.templateId },
                    progress.templateId().orEmpty().ifBlank { template.templateId },
                    template.progressionKind.ifBlank { template.type.name.lowercase(Locale.ROOT) },
                    template.progressionMechanicId,
                    progress.questCode().orEmpty().ifBlank { template.questCode },
                    previousStageId,
                    newStageId,
                    reason,
                    metadata
                )
            )
        )
    }

    private fun buildProgressionEventPayload(
        source: AINPCEventSource,
        player: Player,
        npc: AINPC?,
        template: ScenarioTemplate,
        progress: PlayerQuestProgress
    ): ProgressionEventPayload {
        val metadata = LinkedHashMap<String, String>()
        metadata["scenarioType"] = template.type.name
        if (template.displayName.isNotBlank()) {
            metadata["displayName"] = template.displayName
        }
        if (template.sourcePackId.isNotBlank()) {
            metadata["sourcePackId"] = template.sourcePackId
        }

        val progressionId = progress.templateId().orEmpty().ifBlank { template.templateId }
        val questCode = progress.questCode().orEmpty().ifBlank { template.questCode }
        return ProgressionEventPayload(
            UUID.randomUUID(),
            System.currentTimeMillis(),
            source,
            player.uniqueId,
            player.name,
            npc?.databaseId?.takeIf { it > 0 }?.toString().orEmpty(),
            npc?.uuid,
            npc?.name.orEmpty(),
            progressionId,
            progressionId,
            template.progressionKind.ifBlank { template.type.name.lowercase(Locale.ROOT) },
            template.progressionMechanicId,
            questCode,
            progress.currentPhase(),
            progress.status()?.name.orEmpty(),
            metadata
        )
    }
    private fun publishProgressionFailed(template: ScenarioTemplate, progress: PlayerQuestProgress, failReason: String) {
        if (!plugin.config.getBoolean("events.public_api_enabled", true)) return
        val event = ProgressionFailedEvent(
            ProgressionFailedEventPayload(
                UUID.randomUUID(),
                System.currentTimeMillis(),
                AINPCEventSource.SYSTEM,
                UUID.randomUUID(),
                "",
                null,
                null,
                null,
                progress.templateId().orEmpty().ifBlank { template.templateId },
                progress.templateId().orEmpty().ifBlank { template.templateId },
                failReason,
                mapOf("source" to "ScenarioEngine")
            )
        )
        Bukkit.getPluginManager().callEvent(event)
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
        val templateId = p1.templateId() ?: return
        activePlayerQuests.computeIfAbsent(p0) { ConcurrentHashMap() }[templateId] = p1
    }
    private fun getArchivedQuestProgress(p0: UUID, p1: String): PlayerQuestProgress? {
        return archivedPlayerQuests[p0]?.get(p1)
    }
    private fun getCompletedQuestProgress(p0: UUID, p1: String): PlayerQuestProgress? {
        return getArchivedQuestProgress(p0, p1)?.takeIf { it.isCompleted() }
    }
    private fun getFailedQuestProgress(p0: UUID, p1: String): PlayerQuestProgress? {
        return getArchivedQuestProgress(p0, p1)?.takeIf { it.status() == QuestStatus.FAILED }
    }
    private fun hasCompletedQuest(p0: UUID, p1: String): Boolean {
        return getCompletedQuestProgress(p0, p1) != null
    }
    private fun evaluateQuestAvailability(p0: UUID, p1: ScenarioTemplate): QuestAvailability {
        if (hasCompletedQuest(p0, p1.templateId) && !p1.questRepeatable) {
            return QuestAvailability.unavailable(listOf("Quest deja completat."))
        }
        if (p1.questRepeatable && p1.questCooldownSeconds > 0) {
            val lastCompleted = getCompletedQuestProgress(p0, p1.templateId)
            if (lastCompleted != null) {
                val elapsed = (System.currentTimeMillis() - lastCompleted.completedAt()) / 1000L
                val remaining = p1.questCooldownSeconds - elapsed
                if (remaining > 0) {
                    return QuestAvailability.unavailable(listOf("Mai asteapta ${remaining}s inainte sa reiei acest quest."))
                }
            }
        }
        if (p1.questPrerequisites.isNotEmpty()) {
            val missing = p1.questPrerequisites.filter { prereq ->
                !hasCompletedQuest(p0, prereq) && !hasCompletedQuestByCode(p0, prereq)
            }
            if (missing.isNotEmpty()) {
                return QuestAvailability.unavailable(listOf("Completeaza mai intai: ${missing.joinToString(", ")}"))
            }
        }
        val limit = getProgressionMechanicLimit(p1)
        if (limit > 0) {
            val current = countCurrentProgressionsInMechanic(p0, p1, p1.progressionMechanicId)
            if (current >= limit) {
                return QuestAvailability.unavailable(listOf("Ai atins limita de ${p1.progressionLabel.lowercase()} active."))
            }
        }
        return QuestAvailability.allowed()
    }
    private fun hasCompletedQuestByCode(p0: UUID, p1: String): Boolean {
        val archived = getArchivedQuestProgress(p0)
        return archived.any { it.templateId().equals(p1, ignoreCase = true) || it.questCode().equals(p1, ignoreCase = true) }
    }
    private fun advanceToNextChainedQuest(p0: Player, p1: ScenarioTemplate) {
        val nextTemplates = mutableListOf<ScenarioTemplate>()
        if (p1.nextQuest.isNotBlank()) {
            val explicitNext = questTemplates.values.find { t ->
                t.templateId.equals(p1.nextQuest, ignoreCase = true) || t.questCode.equals(p1.nextQuest, ignoreCase = true)
            }
            if (explicitNext != null) nextTemplates.add(explicitNext)
        }
        nextTemplates.addAll(questTemplates.values.filter { t ->
            t.questPrerequisites.any { prereq ->
                prereq.equals(p1.templateId, ignoreCase = true) || prereq.equals(p1.questCode, ignoreCase = true)
            }
        })
        for (nextTemplate in nextTemplates) {
            val availability = evaluateQuestAvailability(p0.uniqueId, nextTemplate)
            if (!availability.available()) continue
            val npc = resolveQuestGiverNpc(getCurrentQuestProgress(p0.uniqueId, p1.templateId))
            if (npc == null) {
                plugin.debug("[Chain] NPC pentru questul urmator (${nextTemplate.templateId}) nu a fost gasit.")
                continue
            }
            val resolvedAnchors = resolveQuestAnchors(nextTemplate, p0, npc)
            if (!resolvedAnchors.valid()) {
                plugin.debug("[Chain] Ancore nerezolvate pentru ${nextTemplate.templateId}, sar peste lant.")
                continue
            }
            removeArchivedQuestProgress(p0.uniqueId, nextTemplate.templateId)
            var offered = setInitialQuestProgress(p0.uniqueId, p0, nextTemplate)
            offered = bindQuestProgressToNpc(p0.uniqueId, nextTemplate, offered, npc)
            offered = bindQuestProgressToAnchors(p0, offered, resolvedAnchors)
            publishProgressionOffered(p0, npc, nextTemplate, offered, availability, "chain_advance")
            plugin.debug("[Chain] Avansat automat la ${nextTemplate.templateId} pentru ${p0.name}")
        }
    }
    private fun getProgressionMechanicLimit(p0: ScenarioTemplate): Int {
        val mechanicId = p0.progressionMechanicId
        if (mechanicId.isBlank()) return 0
        val definitions = plugin.progressionService.getDefinitions()
        for (def in definitions) {
            if (def.progressionId() == mechanicId) {
                return def.maxActive()
            }
        }
        return 0
    }
    private fun countCurrentProgressionsInMechanic(p0: UUID, p1: ScenarioTemplate?, p2: String): Int {
        val current = getCurrentQuestProgress(p0)
        return current.count { 
            val mechanic = it.templateId()?.let { id -> 
                questTemplates[id]?.progressionMechanicId 
            }
            mechanic == p2 || (p1 != null && mechanic == p1.progressionMechanicId)
        }
    }
    private fun questLogCurrentComparator(p0: UUID): Comparator<PlayerQuestProgress> {
        return Comparator { a, b -> (b.updatedAt() - a.updatedAt()).toInt() }
    }
    private fun buildQuestLogSummaryLines(p0: UUID, p1: List<PlayerQuestProgress>): List<String> {
        return p1.map { it.templateId() ?: "unknown" }
    }
    private fun questLogMatchesProgressionKind(p0: PlayerQuestProgress, p1: String): Boolean {
        return true
    }
    private fun questLogMatches(p0: UUID, p1: PlayerQuestProgress, p2: QuestLogFilter, p3: Boolean): Boolean {
        if (p1 == null) return false
        val archived = getArchivedQuestProgress(p0).any { it.templateId() == p1.templateId() }
        return when (p2) {
            QuestLogFilter.ALL -> true
            QuestLogFilter.ACTIVE -> !archived && p1.isActive()
            QuestLogFilter.ARCHIVED -> archived
            else -> !archived
        }
    }
    private fun questLogCurrentGroupLabel(p0: UUID, p1: ScenarioTemplate?, p2: PlayerQuestProgress): String {
        return p1?.displayName ?: p2.templateId() ?: "Quest"
    }
    private fun formatQuestLogArchivedLine(p0: UUID, p1: ScenarioTemplate?, p2: PlayerQuestProgress, p3: String): String {
        return p3
    }
    private fun buildQuestLogActionLines(p0: Player, p1: UUID, p2: ScenarioTemplate?, p3: PlayerQuestProgress, p4: Boolean): List<String> {
        return emptyList()
    }
    private fun getRecentArchivedQuestProgress(p0: UUID, p1: Int): List<PlayerQuestProgress> {
        return emptyList()
    }
    private fun getArchivedQuestProgress(p0: UUID): List<PlayerQuestProgress> {
        val archived = archivedPlayerQuests[p0] ?: return emptyList()
        return archived.values.toList()
    }
    private fun archiveQuestProgress(p0: UUID, p1: PlayerQuestProgress) {
        val templateId = p1.templateId() ?: return
        archivedPlayerQuests.computeIfAbsent(p0) { ConcurrentHashMap() }[templateId] = p1
    }
    private fun removeActiveQuestProgress(p0: UUID, p1: String): Boolean {
        return activePlayerQuests[p0]?.remove(p1) != null
    }
    private fun removeArchivedQuestProgress(p0: UUID, p1: String): Boolean {
        return archivedPlayerQuests[p0]?.remove(p1) != null
    }
    private fun loadPersistedQuestProgress() {
    }
    private fun registerLoadedQuestProgress(p0: UUID, p1: PlayerQuestProgress): Boolean {
        if (p1.status() == QuestStatus.COMPLETED || p1.status() == QuestStatus.FAILED) {
            archiveQuestProgress(p0, p1)
        } else {
            putActiveQuestProgress(p0, p1)
        }
        return true
    }
    private fun registerLoadedTrackedQuest(p0: UUID, p1: PlayerQuestProgress) {
        trackedQuestTemplates[p0] = p1.templateId() ?: ""
        trackedQuestPlayers.add(p0)
    }
    private fun persistQuestProgressAsync(playerId: UUID, progress: PlayerQuestProgress) {
        plugin.databaseManager.runAsync { persistQuestProgress(playerId, progress) }
    }
    private fun snapshotQuestProgress(): Map<UUID, List<PlayerQuestProgress>> {
        val snapshot = mutableMapOf<UUID, List<PlayerQuestProgress>>()
        activePlayerQuests.forEach { (playerId, map) -> snapshot[playerId] = map.values.toList() }
        archivedPlayerQuests.forEach { (playerId, map) ->
            snapshot.merge(playerId, map.values.toList()) { a, b -> a + b }
        }
        return snapshot
    }
    fun snapshotQuestProgressPublic(): Map<UUID, List<PlayerQuestProgress>> = snapshotQuestProgress()

    fun findQuestTemplate(templateId: String): ScenarioTemplate? = questTemplates[templateId]

    var templatesLoadedAt: Long = System.currentTimeMillis()

    fun isCacheStale(): Boolean {
        val questFile = java.io.File(plugin.dataFolder, "quests.yml")
        if (!questFile.exists()) return false
        val lastModified = questFile.lastModified()
        return lastModified > templatesLoadedAt
    }

    fun cacheAgeSeconds(): Long = (System.currentTimeMillis() - templatesLoadedAt) / 1000

    fun collectQuestWarnings(): List<Pair<String, String>> {
        val warnings = mutableListOf<Pair<String, String>>()
        for (template in questTemplates.values) {
            val templateId = template.templateId
            for ((index, objective) in template.objectives.withIndex()) {
                val type = objective.type
                val normalizedType = ObjectiveTypeAliasRegistry.normalize(type)
                val deprecated = ObjectiveTypeAliasRegistry.isDeprecated(type)
                if (deprecated) {
                    warnings.add("quests.yml" to "Quest $templateId obj[$index]: tip '$type' este deprecated, foloseste '$normalizedType'")
                }
                if (!ObjectiveTypeAliasRegistry.isSupported(type) && !ObjectiveTypeAliasRegistry.isSupported(normalizedType)) {
                    val suggestion = ObjectiveTypeAliasRegistry.suggestCorrection(type)
                    val hint = if (suggestion != null) " - ai vrut '$suggestion'?" else ""
                    warnings.add("quests.yml" to "Quest $templateId obj[$index]: tip necunoscut '$type'$hint")
                }
                if (objective.itemId.isBlank() && normalizedType != "kill_mob") {
                    warnings.add("quests.yml" to "Quest $templateId obj[$index] ($normalizedType): lipseste 'item'")
                }
            }
        }
        if (isCacheStale()) {
            warnings.add("system" to "Cache-ul de questuri este invechit (${cacheAgeSeconds()}s). Ruleaza /ainpc quest reload.")
        }
        for (pack in plugin.featurePackLoader.getLoadedPacks()) {
            if (pack.schemaVersion != 1) {
                warnings.add("pack:${pack.id}" to "Schema version ${pack.schemaVersion} difera de versiunea curenta 1.")
            }
        }
        return warnings
    }

    private fun processObjectiveViaHandlers(
        playerId: UUID,
        template: ScenarioTemplate,
        progress: PlayerQuestProgress,
        objectiveType: String,
    ): Boolean {
        for ((index, objective) in template.objectives.withIndex()) {
            if (!matchesObjectiveType(objective, objectiveType)) continue
            val handler = objectiveHandlerRegistry.find(objectiveType) ?: continue
            val key = buildObjectiveKey(objective, index)
            val current = progress.objectiveProgress()[key] ?: 0
            val result = handler.handleProgress(ro.ainpc.engine.runtime.ObjectiveContext(
                playerId = playerId,
                objective = objective,
                currentProgress = current,
                requiredAmount = objective.amount,
                metadata = emptyMap(),
            ))
            if (result.progressed > 0 || result.completed) {
                return true
            }
        }
        return false
    }

    fun processObjectiveViaRegistry(
        playerId: UUID,
        template: ScenarioTemplate,
        objectiveIndex: Int,
        currentProgress: Int,
    ): ObjectiveResult? {
        if (objectiveIndex < 0 || objectiveIndex >= template.objectives.size) return null
        val objective = template.objectives[objectiveIndex]
        val handler = objectiveHandlerRegistry.find(objective.type)
        if (handler == null) return null
        val context = ro.ainpc.engine.runtime.ObjectiveContext(
            playerId = playerId,
            objective = objective,
            currentProgress = currentProgress,
            requiredAmount = objective.amount,
            metadata = emptyMap(),
        )
        return handler.handleProgress(context)
    }

    fun collectFailureReasons(playerId: UUID, templateId: String): List<String> {
        val reasons = mutableListOf<String>()
        val template = questTemplates[templateId] ?: return reasons
        val quests = activePlayerQuests[playerId]
        val progress = quests?.get(templateId) ?: return reasons
        if (template.objectives.isNotEmpty()) {
            val incompleteObjectives = template.objectives.filterIndexed { index, obj ->
                val key = buildObjectiveKey(obj, index)
                (progress.objectiveProgress()[key] ?: 0) < obj.amount
            }
            if (incompleteObjectives.isNotEmpty()) {
                reasons.add("objective_incomplete: ${incompleteObjectives.size}/${template.objectives.size}")
            }
        }
        val elapsed = (System.currentTimeMillis() - progress.startedAt()) / 1000
        if (elapsed > 0) reasons.add("elapsed_seconds=$elapsed")
        return reasons
    }

    private fun persistQuestProgressSnapshot(snapshot: Map<UUID, List<PlayerQuestProgress>>) {
        plugin.databaseManager.runAsync {
            try {
                plugin.databaseManager.executeTransaction { conn ->
                    val sql = "INSERT OR REPLACE INTO player_quests " +
                        "(player_uuid, template_id, quest_code, status, started_at, completed_at, " +
                        "current_phase, objective_progress, quest_variables, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    conn.prepareStatement(sql).use { stmt ->
                        for ((playerId, progresses) in snapshot) {
                            for (p in progresses) {
                                bindUpsert(stmt, playerId, p)
                                stmt.addBatch()
                            }
                        }
                        stmt.executeBatch()
                    }
                }
            } catch (e: Exception) {
                plugin.logger.log(Level.WARNING, "Eroare la salvarea snapshot-ului de progres questuri", e)
            }
        }
    }
    private fun bindUpsert(
        stmt: java.sql.PreparedStatement,
        playerId: UUID,
        p: PlayerQuestProgress
    ) {
        stmt.setString(1, playerId.toString())
        stmt.setString(2, p.templateId() ?: "")
        stmt.setString(3, p.questCode() ?: "")
        stmt.setString(4, (p.status() ?: QuestStatus.NOT_STARTED).storageValue())
        stmt.setLong(5, p.startedAt())
        stmt.setLong(6, p.completedAt())
        stmt.setString(7, p.currentPhase())
        stmt.setString(8, gson.toJson(p.objectiveProgress()))
        stmt.setString(9, gson.toJson(p.questVariables()))
        stmt.setLong(10, maxOf(System.currentTimeMillis(), p.updatedAt()))
    }
    private fun persistQuestProgress(playerId: UUID, progress: PlayerQuestProgress) {
        try {
            val sql = "INSERT OR REPLACE INTO player_quests " +
                "(player_uuid, template_id, quest_code, status, started_at, completed_at, " +
                "current_phase, objective_progress, quest_variables, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                bindUpsert(stmt, playerId, progress)
                stmt.executeUpdate()
            }
        } catch (e: SQLException) {
            plugin.logger.log(Level.WARNING, "Eroare la salvarea progresului quest ${progress.templateId()} " +
                "pentru jucatorul $playerId", e)
        }
    }
    private fun persistQuestTrackingPreferenceAsync(playerId: UUID, progress: PlayerQuestProgress) {
        persistQuestProgressAsync(playerId, progress)
    }
    private fun persistQuestTrackingPreferenceAsync(playerId: UUID, trackedTemplateId: String) {
        plugin.databaseManager.runAsync {
            try {
                val sql = "UPDATE player_quests SET tracked = 0 WHERE player_uuid = ?"
                plugin.databaseManager.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, playerId.toString())
                    stmt.executeUpdate()
                }
                if (trackedTemplateId.isNotBlank()) {
                    val updateSql = "UPDATE player_quests SET tracked = 1 " +
                        "WHERE player_uuid = ? AND template_id = ?"
                    plugin.databaseManager.prepareStatement(updateSql).use { stmt ->
                        stmt.setString(1, playerId.toString())
                        stmt.setString(2, trackedTemplateId)
                        stmt.executeUpdate()
                    }
                }
            } catch (e: SQLException) {
                plugin.logger.log(Level.WARNING,
                    "Eroare la salvarea preferintei de tracking pentru $playerId", e)
            }
        }
    }
    private fun persistQuestTrackingPreference(playerId: UUID, trackedTemplateId: String) {
        try {
            val sql = "UPDATE player_quests SET tracked = 0 WHERE player_uuid = ?"
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.setString(1, playerId.toString())
                stmt.executeUpdate()
            }
            if (trackedTemplateId.isNotBlank()) {
                val updateSql = "UPDATE player_quests SET tracked = 1 " +
                    "WHERE player_uuid = ? AND template_id = ?"
                plugin.databaseManager.prepareStatement(updateSql).use { stmt ->
                    stmt.setString(1, playerId.toString())
                    stmt.setString(2, trackedTemplateId)
                    stmt.executeUpdate()
                }
            }
        } catch (e: SQLException) {
            plugin.logger.log(Level.WARNING,
                "Eroare la salvarea preferintei de tracking pentru $playerId", e)
        }
    }
    private fun deleteQuestProgressAsync(playerId: UUID, templateId: String) {
        plugin.databaseManager.runAsync { deleteQuestProgress(playerId, templateId) }
    }
    private fun deleteQuestProgress(playerId: UUID, templateId: String) {
        try {
            val sql = "DELETE FROM player_quests WHERE player_uuid = ? AND template_id = ?"
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.setString(1, playerId.toString())
                stmt.setString(2, templateId)
                stmt.executeUpdate()
            }
        } catch (e: SQLException) {
            plugin.logger.log(Level.WARNING,
                "Eroare la stergerea progresului quest $templateId pentru $playerId", e)
        }
    }
    private fun persistQuestAnchorsAsync(
        playerId: UUID,
        progress: PlayerQuestProgress,
        anchors: QuestAnchorResolver.ResolvedQuestAnchors
    ) {
        plugin.databaseManager.runAsync { persistQuestAnchors(playerId, progress, anchors) }
    }
    private fun persistQuestAnchors(
        playerId: UUID,
        progress: PlayerQuestProgress,
        anchors: QuestAnchorResolver.ResolvedQuestAnchors
    ) {
        try {
            plugin.databaseManager.executeTransaction { conn ->
                val deleteSql = "DELETE FROM quest_anchor_bindings " +
                    "WHERE player_uuid = ? AND template_id = ?"
                conn.prepareStatement(deleteSql).use { stmt ->
                    stmt.setString(1, playerId.toString())
                    stmt.setString(2, progress.templateId() ?: "")
                    stmt.executeUpdate()
                }
                val anchorList = anchors.anchors()
                if (anchorList.isNotEmpty()) {
                    val insertSql = "INSERT OR REPLACE INTO quest_anchor_bindings " +
                        "(player_uuid, template_id, objective_key, quest_code, objective_type, " +
                        "reference, anchor_type, anchor_id, anchor_label, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    conn.prepareStatement(insertSql).use { stmt ->
                        for (anchor in anchorList) {
                            stmt.setString(1, playerId.toString())
                            stmt.setString(2, progress.templateId() ?: "")
                            stmt.setString(3, anchor.objectiveKey())
                            stmt.setString(4, progress.questCode() ?: "")
                            stmt.setString(5, anchor.objectiveType())
                            stmt.setString(6, anchor.reference())
                            stmt.setString(7, anchor.anchorType())
                            stmt.setString(8, anchor.anchorId())
                            stmt.setString(9, anchor.label())
                            stmt.setLong(10, System.currentTimeMillis())
                            stmt.setLong(11, System.currentTimeMillis())
                            stmt.executeUpdate()
                        }
                    }
                }
            }
        } catch (e: SQLException) {
            plugin.logger.log(Level.WARNING,
                "Eroare la salvarea ancorelor pentru quest ${progress.templateId()}", e)
        }
    }

    fun loadPlayerQuests() {
        try {
            val sql = "SELECT player_uuid, template_id, quest_code, status, started_at, " +
                "completed_at, current_phase, objective_progress, quest_variables, updated_at " +
                "FROM player_quests"
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        val playerId = UUID.fromString(rs.getString("player_uuid"))
                        val progress = deserializeProgress(rs)
                        val templateId = progress.templateId() ?: continue
                        when (progress.status()) {
                            QuestStatus.COMPLETED, QuestStatus.FAILED -> {
                                archivedPlayerQuests
                                    .getOrPut(playerId) { ConcurrentHashMap() }[templateId] = progress
                            }
                            else -> {
                                activePlayerQuests
                                    .getOrPut(playerId) { ConcurrentHashMap() }[templateId] = progress
                            }
                        }
                    }
                }
            }
            val totalActive = activePlayerQuests.values.sumOf { it.size }
            val totalArchived = archivedPlayerQuests.values.sumOf { it.size }
            plugin.debug("[QuestEngine] Incarcate $totalActive progresii active, $totalArchived arhivate din DB.")
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Eroare la incarcarea progresului questurilor din DB", e)
        }
    }

    private fun deserializeProgress(rs: ResultSet): PlayerQuestProgress {
        val objectiveProgressType: Type = object : com.google.gson.reflect.TypeToken<Map<String, Int>>() {}.type
        val questVariablesType: Type = object : com.google.gson.reflect.TypeToken<Map<String, String>>() {}.type
        val objectiveProgress: Map<String, Int> = try {
            gson.fromJson(rs.getString("objective_progress"), objectiveProgressType) ?: emptyMap()
        } catch (_: Exception) { emptyMap() }
        val questVariables: Map<String, String> = try {
            gson.fromJson(rs.getString("quest_variables"), questVariablesType) ?: emptyMap()
        } catch (_: Exception) { emptyMap() }
        return PlayerQuestProgress(
            templateId = rs.getString("template_id"),
            questCode = rs.getString("quest_code"),
            status = QuestStatus.fromStorage(rs.getString("status")),
            startedAt = rs.getLong("started_at"),
            completedAt = rs.getLong("completed_at"),
            updatedAt = rs.getLong("updated_at"),
            currentPhase = rs.getString("current_phase"),
            objectiveProgress = objectiveProgress,
            questVariables = questVariables,
        )
    }
    private fun resolveQuestAnchors(p0: ScenarioTemplate, p1: Player, p2: AINPC): QuestAnchorResolver.ResolvedQuestAnchors {
        if (p0 == null || p0.objectives.isEmpty()) return QuestAnchorResolver.ResolvedQuestAnchors.valid(emptyList())

        val bindings = mutableListOf<QuestAnchorResolver.ResolvedQuestAnchor>()
        runCatching {
            val uuid = p1.uniqueId.toString()
            val personal = plugin.progressionService.getAnchorBindings(uuid, p0.templateId, 50)
            bindings.addAll(personal.map { b ->
                QuestAnchorResolver.ResolvedQuestAnchor(
                    b.objectiveKey(), b.objectiveType(),
                    b.reference(), b.anchorType(),
                    b.anchorId(), b.displayLabel()
                )
            })
            val global = plugin.progressionService.getAnchorBindings("", p0.templateId, 50)
            for (b in global) {
                if (bindings.none { it.objectiveKey().equals(b.objectiveKey(), ignoreCase = true) }) {
                    bindings.add(QuestAnchorResolver.ResolvedQuestAnchor(
                        b.objectiveKey(), b.objectiveType(),
                        b.reference(), b.anchorType(),
                        b.anchorId(), b.displayLabel()
                    ))
                }
            }
        }

        val resolver = QuestAnchorResolver(plugin.platform?.worldAdminService ?: return QuestAnchorResolver.ResolvedQuestAnchors.valid(emptyList()), null)
        return resolver.resolve(p0, p1.location, p2, bindings.ifEmpty { null })
    }
    private fun mergeStoredQuestAnchorVariables(p0: UUID, p1: String, p2: Map<String, String>): Map<String, String> {
        return p2
    }
    private fun loadQuestAnchorVariables(p0: UUID, p1: String): Map<String, String> {
        return emptyMap()
    }
    private fun parseObjectiveProgress(p0: String): Map<String, Int> {
        return emptyMap()
    }
    private fun parseQuestVariables(p0: String): Map<String, String> {
        return emptyMap()
    }
    private fun <T> parseJsonMap(p0: String, p1: Type): Map<String, T> {
        return emptyMap()
    }
    private fun serializeJson(p0: Map<*, *>): String {
        return "{}"
    }
    private fun refreshTrackedQuestProgress(p0: Player, p1: ScenarioTemplate, p2: PlayerQuestProgress): PlayerQuestProgress {
        if (p2 == null || p0 == null) return p2
        updateTrackedQuestProgress(p0.uniqueId, p1, p2, p2.objectiveProgress())
        return p2
    }
    private fun trackNpcObjectiveProgress(p0: Player, p1: AINPC, p2: ScenarioTemplate, p3: PlayerQuestProgress): PlayerQuestProgress {
        if (p3 == null || p1 == null) return p3
        val updatedVariables = p3.questVariables().toMutableMap()
        updatedVariables["quest_giver_name"] = p1.name ?: ""
        val updatedProgress = PlayerQuestProgress(p3.templateId(), p3.questCode(), p3.status(),
            p3.startedAt(), p3.completedAt(), System.currentTimeMillis(), p3.currentPhase(),
            p3.objectiveProgress(), updatedVariables)
        updateTrackedQuestProgress(p0.uniqueId, p2, updatedProgress, p3.objectiveProgress())
        val npcProgress = markNpcTalkObjective(p0, p1, p2, p3)
        if (npcProgress != p3) return npcProgress
        return updatedProgress
    }

    private fun markNpcTalkObjective(p0: Player, p1: AINPC, p2: ScenarioTemplate, p3: PlayerQuestProgress): PlayerQuestProgress {
        if (p2 == null || p2.objectives.isEmpty()) return p3
        val updatedObjectives = LinkedHashMap(p3.objectiveProgress())
        var changed = false
        for ((index, objective) in p2.objectives.withIndex()) {
            if (!matchesObjectiveType(objective, "talk_to_npc")) continue
            if (shouldShowObjectiveForCurrentStage(p2, p3, objective)) {
                val objectiveKey = buildObjectiveKey(objective, index)
                val before = updatedObjectives.getOrDefault(objectiveKey, 0)
                if (before < objective.amount) {
                    updatedObjectives[objectiveKey] = objective.amount
                    changed = true
                }
            }
        }
        if (!changed) return p3
        val updatedVars = p3.questVariables().toMutableMap()
        updatedVars["quest_giver_name"] = p1.name ?: ""
        val newProgress = PlayerQuestProgress(p3.templateId(), p3.questCode(), p3.status(),
            p3.startedAt(), p3.completedAt(), System.currentTimeMillis(), p3.currentPhase(),
            updatedObjectives, updatedVars)
        updateTrackedQuestProgress(p0.uniqueId, p2, newProgress, p3.objectiveProgress())
        return newProgress
    }
    private fun buildQuestUnavailableResult(p0: ScenarioTemplate, p1: QuestAnchorResolver.ResolvedQuestAnchors): QuestInteractionResult {
        val issues = p1.formatIssues()
        return QuestInteractionResult.handled(true, listOf("Nu pot oferi aceasta misiune momentan."), issues)
    }
    private fun buildQuestUnavailableResult(p0: ScenarioTemplate, p1: QuestAvailability): QuestInteractionResult {
        return QuestInteractionResult.handled(true, listOf("Misiunea nu este disponibila."), p1.issues())
    }
    private fun bindQuestProgressToNpc(p0: UUID, p1: ScenarioTemplate, p2: PlayerQuestProgress, p3: AINPC): PlayerQuestProgress {
        if (p3 == null || p2 == null) return p2
        val updatedVariables = p2.questVariables().toMutableMap()
        updatedVariables["quest_giver_npc_id"] = p3.databaseId.toString()
        updatedVariables["quest_giver_npc_name"] = p3.name ?: ""
        return PlayerQuestProgress(p2.templateId(), p2.questCode(), p2.status(),
            p2.startedAt(), p2.completedAt(), System.currentTimeMillis(), p2.currentPhase(),
            p2.objectiveProgress(), updatedVariables)
    }
    private fun bindQuestProgressToAnchors(p0: Player, p1: PlayerQuestProgress, p2: QuestAnchorResolver.ResolvedQuestAnchors): PlayerQuestProgress {
        if (p1 == null || p2 == null) return p1
        val anchorVariables = p2.toQuestVariables()
        val updatedVariables = p1.questVariables().toMutableMap()
        updatedVariables.putAll(anchorVariables)
        val updatedProgress = PlayerQuestProgress(p1.templateId(), p1.questCode(), p1.status(),
            p1.startedAt(), p1.completedAt(), System.currentTimeMillis(), p1.currentPhase(),
            p1.objectiveProgress(), updatedVariables)
        publishProgressionAnchorsBound(p0, updatedProgress, p2, updatedVariables)
        return updatedProgress
    }
    private fun buildQuestStatusMessages(p0: ScenarioTemplate, p1: PlayerQuestProgress?, p2: Player, p3: String): List<String> {
        if (p1 == null) return listOf("&7Nicio misiune activa.")
        val title = resolveQuestTitle(p0)
        val status = when {
            p1.isCompleted() -> "&a[COMPLETATA]"
            p1.isActive() -> "&e[ACTIVA]"
            p1.isOffered() -> "&6[OFERITA]"
            p1.status() == QuestStatus.FAILED -> "&c[ESUATA]"
            else -> "&7[IN CURS]"
        }
        val lines = mutableListOf<String>()
        lines.add("&6=== $title &6===")
        lines.add("&7Status: $status")
        if (p1.status() == QuestStatus.FAILED) {
            val reasons = collectFailureReasons(p2.uniqueId, p0.templateId)
            if (reasons.isNotEmpty()) {
                lines.add("&cMotive esec: ${reasons.joinToString(", ")}")
            }
        }
        if (p0.description.isNotBlank()) {
            lines.add("&7" + p0.description)
        }
        if (p0.objectives.isNotEmpty()) {
            lines.add("&6Obiective:")
            val objectiveLines = buildObjectiveProgressLines(p0, p1, p2)
            if (objectiveLines.isNotEmpty()) {
                lines.addAll(objectiveLines)
            } else {
                for ((index, objective) in p0.objectives.withIndex()) {
                    val label = formatObjectiveProgressLabel(objective)
                    lines.add("&7- &f$label &7x&f${objective.amount}")
                }
            }
        }
        if (p0.rewards.isNotEmpty()) {
            lines.add("&6Recompense:")
            for (reward in p0.rewards) {
                val rewardLabel = formatObjectiveProgressLabel(reward)
                lines.add("&7- &f$rewardLabel &7x&f${reward.amount}")
            }
        }
        return lines
    }
    private fun resolveTemplateForProgress(p0: PlayerQuestProgress, p1: AINPC?): ScenarioTemplate? {
        if (p0 == null) return null
        return questTemplates[p0.templateId()]
    }
    private fun findCurrentRegion(p0: Location): WorldRegion? {
        if (p0 == null || p0.world == null || plugin.platform == null) return null
        return plugin.platform.worldAdminService.findRegionAt(p0.world.name, p0.blockX, p0.blockY, p0.blockZ)
    }
    private fun findCurrentPlace(p0: Location): WorldPlace? {
        if (p0 == null || p0.world == null || plugin.platform == null) return null
        return plugin.platform.worldAdminService.findPlaceAt(p0.world.name, p0.blockX, p0.blockY, p0.blockZ)
    }
    private fun findCurrentNode(p0: Location): WorldNode? {
        if (p0 == null || p0.world == null || plugin.platform == null) return null
        return plugin.platform.worldAdminService.findNodeAt(p0.world.name, p0.x, p0.y, p0.z)
    }
    private fun findQuestTemplateForNpc(p0: AINPC): ScenarioTemplate {
        return findQuestTemplateForNpc(p0, UUID(0, 0))
    }
    private fun findQuestTemplateForNpc(p0: AINPC, p1: UUID): ScenarioTemplate {
        if (p0 == null) return questTemplates.values.firstOrNull() ?: scenarioTemplates[ScenarioType.QUEST] ?: error("No QUEST template found in scenarioTemplates.")
        val occupation = p0.occupation ?: ""
        for (template in questTemplates.values) {
            if (occupation.isNotBlank() && template.questGiverProfession.equals(occupation, ignoreCase = true)) {
                return template
            }
        }
        return questTemplates.values.firstOrNull() ?: scenarioTemplates[ScenarioType.QUEST] ?: error("No QUEST template found in scenarioTemplates.")
    }
    private fun findQuestTemplateForNpc(p0: AINPC, p1: UUID, p2: String): ScenarioTemplate {
        if (p2.isBlank()) return findQuestTemplateForNpc(p0, p1)
        for (template in questTemplates.values) {
            if (template.templateId.equals(p2, ignoreCase = true) || template.questCode.equals(p2, ignoreCase = true)) {
                return template
            }
        }
        return findQuestTemplateForNpc(p0, p1)
    }
    private fun resolveCurrentQuestTemplateForNpc(p0: AINPC, p1: UUID): ScenarioTemplate {
        val progress = getCurrentQuestProgress(p1, "")
        if (progress != null) {
            val template = questTemplates[progress.templateId()]
            if (template != null) return template
        }
        return findQuestTemplateForNpc(p0, p1)
    }
    private fun resolveCurrentQuestTemplateForNpc(p0: AINPC, p1: UUID, p2: String): ScenarioTemplate {
        val template = findQuestTemplateForNpc(p0, p1, p2)
        if (template != null) return template
        return resolveCurrentQuestTemplateForNpc(p0, p1)
    }
    private fun resolveCurrentQuestTemplateForNpc(p0: AINPC, p1: PlayerQuestProgress): ScenarioTemplate {
        if (p1 != null) {
            val template = questTemplates[p1.templateId()]
            if (template != null) return template
        }
        return findQuestTemplateForNpc(p0)
    }
    private fun matchesActiveQuestNpcObjective(p0: AINPC, p1: ScenarioTemplate, p2: PlayerQuestProgress): Boolean {
        return p0 != null && p2 != null
    }
    private fun matchesProgressionKindFilter(p0: ScenarioTemplate, p1: String): Boolean {
        return p1.isBlank() || p0.progressionKind.equals(p1, ignoreCase = true)
    }
    private fun shouldUseSimpleQuestForAllNpcs(): Boolean {
        return false
    }
    private fun buildSimpleQuestTemplate(p0: AINPC): ScenarioTemplate {
        return ScenarioTemplate(ScenarioType.QUEST)
    }
    private fun resolveSimpleQuestProfile(p0: AINPC, p1: FeaturePackLoader.ProfessionDefinition): SimpleQuestProfile {
        return SimpleQuestProfile("", Material.STONE, 1, Material.STONE, 1, "", "")
    }
    private fun applyConfiguredSimpleQuestProfile(p0: AINPC, p1: String, p2: String, p3: SimpleQuestProfile): SimpleQuestProfile {
        return p3
    }
    private fun resolveProfessionFallbackSection(p0: String, p1: String): ConfigurationSection {
        return plugin.config.getConfigurationSection("default") ?: plugin.config
    }
    private fun resolveConfiguredSimpleQuestTitle(p0: String): String {
        return p0
    }
    private fun resolveConfiguredQuestMaterial(p0: String, p1: Material): Material {
        return p1
    }
    private fun resolveConfiguredQuestMaterialValue(p0: String, p1: Material): Material {
        return p1
    }
    private fun buildQuestBriefingMessages(p0: ScenarioTemplate): List<String> {
        return p0.description.let { if (it.isNotBlank()) listOf(it) else emptyList() }
    }
    private fun applyQuestStoryActions(p0: Player, p1: AINPC, p2: ScenarioTemplate, p3: PlayerQuestProgress, p4: List<ro.ainpc.engine.FeaturePackLoader.QuestEntryDefinition>): List<String> {
        if (p4.isEmpty()) {
            return emptyList()
        }

        val notes = mutableListOf<String>()
        for (entry in p4) {
            val actionType = normalizeStoryActionType(entry)
            if (actionType.isBlank()) {
                continue
            }

            val target = resolveStoryActionTarget(entry, p0, p3)
            val scopeId = resolveStoryScopeId(actionType, target.scopeId(), p0, p3)
            val anchorReference = resolveStoryAnchorReference(actionType, getQuestEntryMetadata(entry, "anchor", "anchor_ref", "reference", "target"), p3)
            val metadata = LinkedHashMap<String, String>()
            metadata.putAll(entry.metadata)
            metadata.putAll(entry.variables)
            metadata.putAll(entry.payload)
            metadata["scope"] = target.scopeType()
            metadata["scopeId"] = scopeId
            if (anchorReference.isNotBlank()) {
                metadata["anchorReference"] = anchorReference
            }

            publishStoryActionApplied(
                p0,
                p1,
                p2,
                p3,
                entry,
                actionType,
                target,
                scopeId,
                metadata
            )
            notes.add((entry.description.ifBlank { actionType }).trim())
        }
        return notes
    }
    private fun resolveStoryActionTarget(p0: FeaturePackLoader.QuestEntryDefinition, p1: Player, p2: PlayerQuestProgress): StoryActionTarget {
        val scopeType = normalizeStoryScope(getQuestEntryMetadata(p0, "scope", "scope_type"))
        val rawTarget = firstNonBlank(
            getQuestEntryMetadata(p0, "target", "scope_id", "target_id", "id", "place_id", "region_id", "target_place", "target_region", "place", "region"),
            p0.itemId
        )
        val cleanedTarget = cleanStoryId(rawTarget)
        val resolvedScopeId = if (cleanedTarget.isNotBlank()) cleanedTarget else rawTarget
        val regionId = when {
            scopeType == "region" -> resolvedScopeId
            scopeType == "place" -> resolveRegionIdForPlace(resolvedScopeId, p1)
            else -> getQuestEntryMetadata(p0, "region_id", "region", "target_region")
        }
        val placeId = when {
            scopeType == "place" -> resolvedScopeId
            scopeType == "region" -> getQuestEntryMetadata(p0, "place_id", "place", "target_place")
            else -> getQuestEntryMetadata(p0, "place_id", "place", "target_place")
        }
        return StoryActionTarget(
            if (scopeType.isNotBlank()) scopeType else detectStoryTargetScope(rawTarget).ifBlank { "region" },
            if (resolvedScopeId.isNotBlank()) resolvedScopeId else cleanStoryId(getQuestEntryMetadata(p0, "scope_id", "target_id", "id")),
            regionId,
            placeId
        )
    }
    private fun resolveStoryScopeId(p0: String, p1: String, p2: Player, p3: PlayerQuestProgress): String {
        val cleanScopeId = cleanStoryId(p1)
        if (cleanScopeId.isNotBlank()) {
            return cleanScopeId
        }
        return when (p0) {
            "set_story_state" -> firstNonBlank(p3.questVariables()["story_scope_id"], p3.questVariables()["scope_id"], findCurrentRegionId(p2))
            "record_story_event" -> firstNonBlank(p3.questVariables()["story_scope_id"], p3.questVariables()["scope_id"], findCurrentPlaceId(p2), findCurrentRegionId(p2))
            else -> p1
        }
    }
    private fun resolveStoryAnchorReference(p0: String, p1: String, p2: PlayerQuestProgress): String {
        return firstNonBlank(cleanStoryId(p1), p2.questVariables()["anchor_reference"], p2.questVariables()["anchor_ref"])
    }
    private fun resolveQuestVariableAnchorId(p0: PlayerQuestProgress, p1: String, p2: String): String {
        return p2
    }
    private fun findFirstQuestAnchorId(p0: PlayerQuestProgress, p1: String): String {
        return p1
    }
    private fun resolveRegionIdForPlace(p0: String, p1: Player): String {
        return findCurrentRegionId(p1)
    }
    private fun findCurrentRegionId(p0: Player): String {
        val region = findCurrentRegion(p0.location)
        return region?.id ?: ""
    }
    private fun findCurrentPlaceId(p0: Player): String {
        val place = findCurrentPlace(p0.location)
        return place?.id ?: ""
    }
    private fun publishStoryActionApplied(
        player: Player,
        npc: AINPC,
        template: ScenarioTemplate,
        progress: PlayerQuestProgress,
        entry: FeaturePackLoader.QuestEntryDefinition,
        actionType: String,
        target: StoryActionTarget,
        scopeId: String,
        metadata: Map<String, String>
    ) {
        if (!plugin.config.getBoolean("events.public_api_enabled", true)) {
            return
        }

        val eventType = getQuestEntryMetadata(entry, "event_type", "type_id")
        val eventKey = firstNonBlank(getQuestEntryMetadata(entry, "event_key", "key"), progress.questCode(), template.questCode)
        Bukkit.getPluginManager().callEvent(
            StoryActionAppliedEvent(
                StoryActionAppliedEventPayload(
                    UUID.randomUUID(),
                    System.currentTimeMillis(),
                    AINPCEventSource.PLAYER,
                    player.uniqueId,
                    player.name,
                    npc.databaseId.takeIf { it > 0 }?.toString(),
                    npc.uuid,
                    npc.name,
                    progress.templateId().orEmpty().ifBlank { template.templateId },
                    progress.templateId().orEmpty().ifBlank { template.templateId },
                    template.progressionKind.ifBlank { template.type.name.lowercase(Locale.ROOT) },
                    template.progressionMechanicId,
                    progress.questCode().orEmpty().ifBlank { template.questCode },
                    progress.currentPhase(),
                    actionType,
                    entry.entryId,
                    entry.itemId,
                    target.scopeType(),
                    scopeId,
                    target.regionId(),
                    target.placeId(),
                    eventType,
                    eventKey,
                    entry.description,
                    metadata
                )
            )
        )
    }
    private fun updateTrackedQuestProgress(p0: UUID, p1: ScenarioTemplate, p2: PlayerQuestProgress, p3: Map<String, Int>): PlayerQuestProgress {
        val currentPhase = resolveQuestPhase(p1, p2.status(), p2.currentPhase(), p3)
        val updatedProgress = PlayerQuestProgress(p2.templateId(), p2.questCode(), p2.status(),
            p2.startedAt(), p2.completedAt(), System.currentTimeMillis(), currentPhase, p3, p2.questVariables())
        putActiveQuestProgress(p0, updatedProgress)
        persistQuestProgressAsync(p0, updatedProgress)
        val previousPhase = p2.currentPhase()
        if (previousPhase.isNotBlank() && previousPhase != currentPhase) {
            val player = Bukkit.getPlayer(p0)
            if (player != null) {
                publishProgressionStageChanged(
                    AINPCEventSource.PLAYER,
                    player,
                    null,
                    p1,
                    updatedProgress,
                    previousPhase,
                    currentPhase,
                    "objective_progress",
                    emptyMap()
                )
            }
        }
        return updatedProgress
    }
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
        val scenarioId = UUID.randomUUID()
        val scenario = ActiveScenario(scenarioId, p0)
        scenario.currentPhase = p0.phases.firstOrNull().orEmpty()
        scenario.anchorLocation = p2.firstOrNull()?.location?.clone() ?: p1.firstOrNull()?.location?.clone()
        scenario.questActorTriggers.putAll(p0.questActorTriggers.mapValues { entry -> LinkedHashSet(entry.value) })
        activeScenarios[scenarioId] = scenario
        val anchorLocation = scenario.anchorLocation
        if (anchorLocation != null) {
            applyScenarioPhaseActors(scenarioId, scenario.currentPhase, anchorLocation)
        }
        plugin.logger.info(
            "Scenariu pornit: " + scenario.displayName +
                " (ID: " + scenarioId.toString().substring(0, 8) + ", actori: " + scenario.actors.size + ")"
        )
    }
    private fun assignRoles(p0: ActiveScenario, p1: ScenarioTemplate, p2: List<AINPC>, p3: List<Player>): Boolean {
        return true
    }
    private fun assignFallbackRoles(p0: ActiveScenario, p1: List<ScenarioRoleRule>, p2: List<AINPC>, p3: Random, p4: Boolean): Boolean {
        return true
    }
    private fun getQuestSettings(): ConfigurationSection {
        return plugin.config.getConfigurationSection("quests") ?: plugin.config
    }
    private fun notifyParticipants(p0: ActiveScenario) {
    }

    fun spawnScenarioActor(scenarioId: UUID, actorId: String, location: Location): AINPC? {
        val activeScenario = activeScenarios[scenarioId] ?: return null
        val actorDefinition = activeScenario.actors[actorId] ?: return null
        val npc = AINPC(plugin)
        npc.applyScenarioDefinition(actorDefinition)
        if (npc.name.isBlank()) {
            npc.name = actorDefinition.name
            npc.displayName = actorDefinition.name
        }
        npc.sourceKey = if (npc.sourceKey.isBlank()) {
            "${activeScenario.templateId}:$actorId"
        } else {
            npc.sourceKey
        }
        npc.profileSource = "scenario_actor"
        val worldName = location.world?.name ?: return null
        npc.setLocation(worldName, location.x, location.y, location.z, location.yaw, location.pitch)

        if (!publishScenarioActorSpawned(activeScenario, actorDefinition, npc, location)) {
            return null
        }
        if (!npc.spawn()) {
            return null
        }

        if (actorDefinition.persistenceMode != NpcPersistenceMode.RUNTIME_ONLY &&
            !plugin.npcManager.saveNPC(npc, false)
        ) {
            npc.despawn()
            return null
        }

        plugin.npcManager.registerTransientNPC(npc)
        activeScenario.assignActor(actorId, npc.uuid)
        scheduleScenarioActorExpiration(scenarioId, actorId, actorDefinition)
        return npc
    }

    fun spawnScenarioActors(
        scenarioId: UUID,
        anchorLocation: Location,
        phase: String? = null,
        actorIds: Set<String>? = null,
        allowManual: Boolean = false,
        allowStage: Boolean = false,
    ): List<AINPC> {
        val activeScenario = activeScenarios[scenarioId] ?: return emptyList()
        val spawned = mutableListOf<AINPC>()
        var offset = 0.0
        for (actorId in activeScenario.actors.keys) {
            if (!actorIds.isNullOrEmpty() && !actorIds.contains(actorId)) {
                continue
            }
            val actorDefinition = activeScenario.actors[actorId] ?: continue
            if (!allowManual && actorDefinition.spawnPolicy == NpcSpawnPolicy.MANUAL) {
                continue
            }
            if (!allowStage && actorDefinition.spawnPolicy == NpcSpawnPolicy.STAGE) {
                continue
            }
            if (!shouldSpawnScenarioActorInPhase(actorDefinition, phase ?: activeScenario.currentPhase)) {
                continue
            }
            if (activeScenario.spawnedActors.containsKey(actorId)) {
                continue
            }
            val spawnLocation = anchorLocation.clone().add(offset, 0.0, offset)
            val npc = spawnScenarioActor(scenarioId, actorId, spawnLocation)
            if (npc != null) {
                spawned.add(npc)
            }
            offset += 1.5
        }
        return spawned
    }

    fun despawnScenarioActor(scenarioId: UUID, actorId: String) {
        val activeScenario = activeScenarios[scenarioId] ?: return
        val npcUuid = activeScenario.removeActor(actorId) ?: return
        val npc = plugin.npcManager.getNPCByUuid(npcUuid) ?: return
        if (npc.persistenceMode == NpcPersistenceMode.RUNTIME_ONLY ||
            npc.lifecycleType == NpcLifecycleType.EPISODIC ||
            npc.lifecycleType == NpcLifecycleType.SCENE_ONLY
        ) {
            plugin.npcManager.unregisterTransientNPC(npc)
        }
        npc.despawn()
    }

    fun despawnScenarioActors(scenarioId: UUID) {
        val activeScenario = activeScenarios[scenarioId] ?: return
        for (actorId in activeScenario.spawnedActors.keys.toList()) {
            despawnScenarioActor(scenarioId, actorId)
        }
    }

    private fun refreshScenarioActorsForPhase(scenarioId: UUID, phase: String) {
        val activeScenario = activeScenarios[scenarioId] ?: return
        val anchorLocation = activeScenario.anchorLocation ?: activeScenario.spawnedActors.keys
            .firstNotNullOfOrNull { actorId ->
                val npcUuid = activeScenario.spawnedActors[actorId] ?: return@firstNotNullOfOrNull null
                plugin.npcManager.getNPCByUuid(npcUuid)?.location
            }
            ?.clone()

        if (anchorLocation == null) {
            return
        }

        activeScenario.anchorLocation = anchorLocation.clone()
        val phaseRule = getScenarioPhaseActorRule(activeScenario, phase)
        phaseRule.despawnActorIds.forEach { actorId ->
            despawnScenarioActor(scenarioId, actorId)
        }
        for (actorId in activeScenario.spawnedActors.keys.toList()) {
            val actorDefinition = activeScenario.actors[actorId] ?: continue
            if (shouldSpawnScenarioActorInPhase(actorDefinition, phase)) {
                continue
            }
            despawnScenarioActor(scenarioId, actorId)
        }

        if (phaseRule.spawnActorIds.isNotEmpty()) {
            spawnScenarioActors(scenarioId, anchorLocation, phase, phaseRule.spawnActorIds, false, true)
        } else {
            spawnScenarioActors(scenarioId, anchorLocation, phase)
        }
    }

    private fun applyScenarioPhaseActors(scenarioId: UUID, phase: String, anchorLocation: Location) {
        val activeScenario = activeScenarios[scenarioId] ?: return
        activeScenario.anchorLocation = anchorLocation.clone()
        refreshScenarioActorsForPhase(scenarioId, phase)
    }

    private fun publishScenarioActorSpawned(
        activeScenario: ActiveScenario,
        actorDefinition: NpcScenarioActorDefinition,
        npc: AINPC,
        location: Location,
    ): Boolean {
        plugin.debug(
            "Spawn actor scenariu=" + activeScenario.templateId +
                " actor=" + actorDefinition.id +
                " npc=" + npc.name +
                " la " + location
        )
        return true
    }

    private fun scheduleScenarioActorExpiration(
        scenarioId: UUID,
        actorId: String,
        actorDefinition: NpcScenarioActorDefinition,
    ) {
        val durationSeconds = actorDefinition.durationSeconds ?: return
        if (durationSeconds <= 0) return
        val ticks = durationSeconds * 20L
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            despawnScenarioActor(scenarioId, actorId)
        }, ticks)
    }

    private fun shouldSpawnScenarioActorInPhase(
        actorDefinition: NpcScenarioActorDefinition,
        phase: String,
    ): Boolean {
        when (actorDefinition.spawnPolicy) {
            NpcSpawnPolicy.MANUAL -> return false
            NpcSpawnPolicy.STAGE -> return true
            NpcSpawnPolicy.AUTO -> return true
            NpcSpawnPolicy.PHASE -> Unit
        }
        val actorPhase = actorDefinition.spawnPhase.trim()
        return actorPhase.isBlank() || actorPhase.equals(phase, ignoreCase = true)
    }

    private data class ScenarioPhaseActorRule(
        val spawnActorIds: Set<String>,
        val despawnActorIds: Set<String>,
    )

    private fun getScenarioPhaseActorRule(
        scenario: ActiveScenario,
        phase: String,
    ): ScenarioPhaseActorRule {
        val template = scenarioTemplates[scenario.type] ?: return ScenarioPhaseActorRule(emptySet(), emptySet())
        val stage = template.questStages.firstOrNull { questStage ->
            questStage.id.equals(phase, ignoreCase = true)
        } ?: return ScenarioPhaseActorRule(emptySet(), emptySet())
        return ScenarioPhaseActorRule(
            spawnActorIds = readScenarioActorTriggerList(stage.metadata, "spawn_actors", QuestActorTriggers.ON_STAGE_ENTER, "stage_enter_actors"),
            despawnActorIds = readScenarioActorTriggerList(stage.metadata, "despawn_actors", QuestActorTriggers.ON_STAGE_EXIT, "stage_exit_actors"),
        )
    }

    private fun getScenarioStageCompleteActors(
        scenario: ActiveScenario,
        phase: String,
    ): Set<String> {
        val template = scenarioTemplates[scenario.type] ?: return emptySet()
        val stage = template.questStages.firstOrNull { questStage ->
            questStage.id.equals(phase, ignoreCase = true)
        } ?: return emptySet()
        return readScenarioActorTriggerList(stage.metadata, QuestActorTriggers.ON_STAGE_COMPLETE, "stage_complete_actors", "complete_actors")
    }

    private fun readScenarioActorTriggerList(
        metadata: Map<String, String>,
        vararg keys: String,
    ): Set<String> {
        for (key in keys) {
            val actorIds = readScenarioActorIdList(metadata[key])
            if (actorIds.isNotEmpty()) {
                return actorIds
            }
        }
        return emptySet()
    }

    private fun readScenarioActorIdList(rawValue: String?): Set<String> {
        if (rawValue.isNullOrBlank()) {
            return emptySet()
        }
        return rawValue.split(',', ';', '|')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toCollection(LinkedHashSet())
    }
    private fun adjustEmotionsForRole(p0: AINPC, p1: String, p2: ScenarioType) {
    }
    private fun sendScenarioHint(p0: Player, p1: ActiveScenario) {
    }
    private fun sendQuestBriefing(p0: Player, p1: ActiveScenario) {
    }
    private fun resolveProfessionName(p0: String): String {
        return p0
    }
    fun advanceScenario(p0: UUID) {
        val scenario = activeScenarios[p0] ?: return
        val template = scenarioTemplates[scenario.type] ?: return
        val phases = template.phases
        val currentIndex = phases.indexOf(scenario.currentPhase)
        if (currentIndex < phases.size - 1) {
            val previousPhase = scenario.currentPhase
            scenario.currentPhase = phases[currentIndex + 1]
            cleanupScenarioStageActors(p0, previousPhase, getScenarioStageCompleteActors(scenario, previousPhase))
            refreshScenarioActorsForPhase(p0, scenario.currentPhase)
            plugin.debug("Scenariu " + p0.toString().substring(0, 8)
                + " avansat la faza: " + scenario.currentPhase)
        } else {
            endScenario(p0)
        }
    }

    private fun cleanupScenarioStageActors(
        scenarioId: UUID,
        phase: String,
        actorIds: Set<String>,
    ) {
        if (actorIds.isEmpty()) {
            return
        }
        for (actorId in actorIds) {
            despawnScenarioActor(scenarioId, actorId)
        }
        plugin.debug("Scenariu " + scenarioId.toString().substring(0, 8)
            + " a curatat actorii de stage complete pentru faza: " + phase)
    }
    fun endScenario(p0: UUID) {
        val scenario = activeScenarios[p0] ?: return
        despawnScenarioActors(p0)
        activeScenarios.remove(p0)
        createScenarioMemories(scenario)
        plugin.logger.info("Scenariu terminat: " + scenario.displayName
            + " (ID: " + p0.toString().substring(0, 8) + ")")
    }
    private fun createScenarioMemories(p0: ActiveScenario) {
    }
    fun getActiveScenarios(): Map<UUID, ActiveScenario> = HashMap(activeScenarios)

    fun findActiveScenarioByTemplateId(templateId: String): Map.Entry<UUID, ActiveScenario>? {
        val normalized = templateId.trim()
        if (normalized.isBlank()) {
            return null
        }
        return activeScenarios.entries.firstOrNull { entry ->
            entry.value.templateId.equals(normalized, ignoreCase = true) ||
                entry.value.displayName.equals(normalized, ignoreCase = true)
        }
    }

    fun getNPCScenario(p0: UUID): ActiveScenario? {
        for (scenario in activeScenarios.values) {
            if (scenario.hasNPCRole(p0)) {
                return scenario
            }
        }
        return null
    }
}
