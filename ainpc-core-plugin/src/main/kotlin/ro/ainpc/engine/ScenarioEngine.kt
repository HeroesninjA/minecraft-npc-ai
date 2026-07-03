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
import ro.ainpc.story.PlannedStructureStoryEvent
import ro.ainpc.world.WorldNode
import ro.ainpc.world.WorldPlace
import ro.ainpc.engine.runtime.ScenarioActionRegistry
import ro.ainpc.engine.runtime.ScenarioConditionRegistry
import ro.ainpc.engine.runtime.ScenarioExecutionContext
import ro.ainpc.engine.runtime.ScenarioRuntimeDefinition
import ro.ainpc.engine.runtime.ScenarioTriggerRegistry
import ro.ainpc.engine.runtime.ScenarioVariableProviderRegistry
import ro.ainpc.engine.runtime.QuestVariableProvider
import ro.ainpc.engine.runtime.ObjectiveContext
import ro.ainpc.engine.runtime.ObjectiveHandlerRegistry
import ro.ainpc.engine.runtime.ObjectiveResult
import ro.ainpc.engine.runtime.objectivehandlers.EquipItemObjectiveHandler
import ro.ainpc.engine.runtime.objectivehandlers.UseItemObjectiveHandler
import ro.ainpc.engine.runtime.actions.GiveItemAction
import ro.ainpc.engine.runtime.actions.SetStoryStateAction
import ro.ainpc.engine.runtime.conditions.HasCompletedQuestCondition
import ro.ainpc.engine.runtime.triggers.PlayerEntersRegionTrigger
import ro.ainpc.story.StructureStoryEventPlanner
import ro.ainpc.world.WorldRegion
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

class ScenarioEngine(private val plugin: AINPCPlugin) {
    private val progressionLifecyclePublisher = QuestProgressionLifecyclePublisher(plugin)
    private val progressionEventPublisher = QuestProgressionEventPublisher(plugin) { progress ->
        resolveTemplateForProgress(progress, null)
    }
    val actionRegistry: ScenarioActionRegistry = ScenarioActionRegistry()
    val conditionRegistry: ScenarioConditionRegistry = ScenarioConditionRegistry()
    val triggerRegistry: ScenarioTriggerRegistry = ScenarioTriggerRegistry()
    val variableProviderRegistry: ScenarioVariableProviderRegistry = ScenarioVariableProviderRegistry()
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
    private val structureStoryEventCooldownMs = 30_000L
    private val questProgressPersistence = QuestProgressPersistenceService(plugin, gson)
    private val questAnchorBindingService = QuestAnchorBindingService(plugin)
    private val questTemplateSelectionService = QuestTemplateSelectionService(questTemplates, scenarioTemplates) { playerId, reference ->
        getCurrentQuestProgress(playerId, reference)
    }
    private val questStoryActionEventPublisher = QuestStoryActionEventPublisher(plugin)
    private val questProgressLookupService = QuestProgressLookupService(
        getActiveQuests = { playerId -> activePlayerQuests[playerId] },
        getArchivedQuests = { playerId -> archivedPlayerQuests[playerId] }
    )
    private val questProgressStoreService = QuestProgressStoreService(
        getActiveQuests = { playerId -> activePlayerQuests.computeIfAbsent(playerId) { ConcurrentHashMap() } },
        getArchivedQuests = { playerId -> archivedPlayerQuests.computeIfAbsent(playerId) { ConcurrentHashMap() } }
    )
    private val questProgressLifecycleService = QuestProgressLifecycleService(
        getCurrentQuestProgress = { playerId, templateId -> questProgressLookupService.getCurrentQuestProgress(playerId, templateId) },
        putActiveQuestProgress = questProgressStoreService::putActiveQuestProgress,
        removeArchivedQuestProgress = questProgressStoreService::removeArchivedQuestProgress,
        persistQuestProgressAsync = questProgressPersistence::persistQuestProgressAsync,
        shouldAutoAcceptOnOffer = ::shouldAutoAcceptOnOffer,
        resolveQuestPhase = ::resolveQuestPhase,
        buildObjectiveProgressSnapshot = ::buildObjectiveProgressSnapshot,
        seedQuestStageVariables = ::seedQuestStageVariables,
        publishProgressionStageChanged = { playerId, player, template, progress, previousPhase, currentPhase ->
            if (player != null) {
                publishProgressionStageChanged(
                    AINPCEventSource.PLAYER,
                    player,
                    null,
                    template,
                    progress,
                    previousPhase,
                    currentPhase,
                    "quest_state_rebuild",
                    emptyMap()
                )
            }
        }
    )
    private val questProgressCleanupService = QuestProgressCleanupService(
        getActivePlayerQuests = { activePlayerQuests },
        getArchivedPlayerQuests = { archivedPlayerQuests },
        getQuestTemplates = { questTemplates },
        deleteQuestProgress = questProgressPersistence::deleteQuestProgress,
        trackedBlockLocations = trackedBlockLocations,
        trackedVisitedPlaces = trackedVisitedPlaces,
        npcConversationCooldowns = npcConversationCooldowns,
        regionEntryCounts = regionEntryCounts,
        eventDebounceBuffer = eventDebounceBuffer,
        sentCompletionMessages = sentCompletionMessages,
        trackedQuestPlayers = trackedQuestPlayers,
        trackedQuestTemplates = trackedQuestTemplates,
        persistQuestTrackingPreferenceAsync = questProgressPersistence::persistQuestTrackingPreferenceAsync,
        onlinePlayerIdsProvider = { plugin.server.onlinePlayers.map { it.uniqueId }.toSet() },
    )
    private val questProgressFinalizationService = QuestProgressFinalizationService(
        getCurrentQuestProgress = { playerId, templateId -> questProgressLookupService.getCurrentQuestProgress(playerId, templateId) },
        removeActiveQuestProgress = questProgressStoreService::removeActiveQuestProgress,
        archiveQuestProgress = questProgressStoreService::archiveQuestProgress,
        persistQuestProgressAsync = questProgressPersistence::persistQuestProgressAsync,
        resolveQuestPhase = ::resolveQuestPhase,
        questProgressCleanupService = questProgressCleanupService,
        publishProgressionFailed = ::publishProgressionFailed,
    )
    private val questLogSupportService = QuestLogSupportService(
        getQuestTemplate = { templateId -> questTemplates[templateId] },
        getCurrentQuestProgress = { playerId, templateId -> questProgressLookupService.getCurrentQuestProgress(playerId, templateId) },
        getArchivedQuestProgress = { playerId -> questProgressLookupService.getArchivedQuestProgress(playerId) }
    )
    private val questStatusMessageBuilder = QuestStatusMessageBuilder(questLogSupportService::collectFailureReasons)
    private val buildQuestStatusMessages: (ScenarioTemplate, PlayerQuestProgress?, Player, String) -> List<String> = { template, progress, player, _ ->
        if (progress == null) {
            listOf("&7Nicio misiune activa.")
        } else {
            questStatusMessageBuilder.build(template, progress, player)
        }
    }
    private val questGuiContentBuilder = QuestGuiContentBuilder(
        buildObjectiveKey = ::buildObjectiveKey,
        readObjectiveProgress = ::readObjectiveProgress,
        resolveObjectiveCurrentProgress = ::resolveObjectiveCurrentProgress,
        shouldShowObjectiveForCurrentStage = ::shouldShowObjectiveForCurrentStage,
        findObjectiveStageId = ::findObjectiveStageId,
        resolveQuestObjectiveState = ::resolveQuestObjectiveState,
        normalizeObjectiveType = ::normalizeObjectiveType,
        formatObjectiveProgressLabel = ::formatObjectiveProgressLabel,
        formatQuestEntry = ::formatQuestEntry,
        formatQuestPhase = ::formatQuestPhase,
        phasesMatch = ::phasesMatch,
        areObjectivesSatisfiedForStage = ::areObjectivesSatisfiedForStage,
        formatStageCompletionMode = ::formatStageCompletionMode
    )
    private val questProgressViewBuilder = QuestProgressViewBuilder(
        plugin = plugin,
        findQuestProgressByReference = ::findQuestProgressByReference,
        getTrackedQuestProgress = ::getTrackedQuestProgress,
        resolveTemplateForProgress = ::resolveTemplateForProgress,
        refreshTrackedQuestProgress = ::refreshTrackedQuestProgress,
        isTrackedQuest = ::isTrackedQuest,
        buildQuestStatusMessages = buildQuestStatusMessages,
        resolveQuestNpcName = ::resolveQuestNpcName,
        formatQuestStatus = ::formatQuestStatus,
        formatQuestPhase = ::formatQuestPhase,
        isTrackedQuestSelector = ::isTrackedQuestSelector
    )
    private val questLogViewBuilder = QuestLogViewBuilder(
        parseQuestLogFilter = ::parseQuestLogFilter,
        getCurrentQuestProgress = { playerId -> getCurrentQuestProgress(playerId) },
        getArchivedQuestProgress = { playerId -> questProgressLookupService.getArchivedQuestProgress(playerId) },
        resolveTemplateForProgress = ::resolveTemplateForProgress,
        refreshTrackedQuestProgress = ::refreshTrackedQuestProgress,
        questLogMatches = questLogSupportService::questLogMatches,
        questLogCurrentComparator = questLogSupportService::questLogCurrentComparator,
        buildQuestLogSummaryLines = questLogSupportService::buildQuestLogSummaryLines,
        questLogCurrentGroupLabel = questLogSupportService::questLogCurrentGroupLabel,
        isTrackedQuest = ::isTrackedQuest,
        resolveQuestTitle = ::resolveQuestTitle,
        resolveQuestNpcName = ::resolveQuestNpcName,
        buildQuestStatusMessages = buildQuestStatusMessages,
        buildQuestLogActionLines = ::buildQuestLogActionLines,
        formatQuestStatus = ::formatQuestStatus,
        formatQuestPhase = ::formatQuestPhase,
        formatQuestLogArchivedLine = questLogSupportService::formatQuestLogArchivedLine
    )
    private val questGuiEntryBuilder = QuestGuiEntryBuilder(
        featurePackLoader = plugin.featurePackLoader,
        resolveTemplateForProgress = ::resolveTemplateForProgress,
        refreshTrackedQuestProgress = ::refreshTrackedQuestProgress,
        questLogActionSelector = ::questLogActionSelector,
        resolveQuestTitle = ::resolveQuestTitle,
        resolveQuestCategory = ::resolveQuestCategory,
        resolveProgressionMechanicDisplay = ::resolveProgressionMechanicDisplay,
        formatQuestStatus = ::formatQuestStatus,
        resolveQuestPhase = ::resolveQuestPhase,
        buildQuestStatusMessages = buildQuestStatusMessages,
        isTrackedQuest = ::isTrackedQuest,
        formatQuestPhase = ::formatQuestPhase,
        resolveQuestNpcName = ::resolveQuestNpcName,
        buildQuestGuiObjectives = questGuiContentBuilder::buildObjectives,
        buildQuestGuiStages = questGuiContentBuilder::buildStages,
        buildQuestLogActionLines = ::buildQuestLogActionLines,
        buildMissingQuestTemplateLines = ::buildMissingQuestTemplateLines,
        valueOrFallback = ::valueOrFallback
    )
    private val questStoryTargetResolver = QuestStoryTargetResolver(
        normalizeStoryScope = ::normalizeStoryScope,
        getQuestEntryMetadata = { entry, keys -> getQuestEntryMetadata(entry, *keys) },
        firstNonBlank = { values -> firstNonBlank(*values) },
        cleanStoryId = ::cleanStoryId,
        detectStoryTargetScope = ::detectStoryTargetScope,
        resolveRegionIdForPlace = { _, player -> findCurrentRegion(player.location)?.id ?: "" },
        findCurrentRegionId = { player -> findCurrentRegion(player.location)?.id ?: "" },
        findCurrentPlaceId = { player -> findCurrentPlace(player.location)?.id ?: "" }
    )
    private val structureStoryEventPlanner = StructureStoryEventPlanner()
    init {
        initSimpleQuestPlugin(plugin)
        initTrackingResolutionPlugin(plugin)
        registerRuntimeHandlers()
        loadScenarioTemplates()
    }

    private fun registerRuntimeHandlers() {
        actionRegistry.register(GiveItemAction())
        actionRegistry.register(SetStoryStateAction())
        actionRegistry.register(ro.ainpc.engine.runtime.actions.RecordStoryEventAction())
        actionRegistry.register(ro.ainpc.engine.runtime.actions.SendMessageAction())
        actionRegistry.register(ro.ainpc.engine.runtime.actions.ExecuteCommandAction())
        actionRegistry.register(ro.ainpc.engine.runtime.actions.PlaySoundAction())
        actionRegistry.register(ro.ainpc.engine.runtime.actions.TeleportPlayerAction())
        conditionRegistry.register(HasCompletedQuestCondition())
        conditionRegistry.register(ro.ainpc.engine.runtime.conditions.QuestCooldownCondition())
        conditionRegistry.register(ro.ainpc.engine.runtime.conditions.QuestPrerequisiteCondition())
        conditionRegistry.register(ro.ainpc.engine.runtime.conditions.MechanicLimitCondition())
        triggerRegistry.register(PlayerEntersRegionTrigger())
        triggerRegistry.register(ro.ainpc.engine.runtime.triggers.PlayerEntersPlaceTrigger())
        triggerRegistry.register(ro.ainpc.engine.runtime.triggers.PlayerEntersNodeTrigger())
        triggerRegistry.register(ro.ainpc.engine.runtime.triggers.PlayerUsesItemTrigger())
        triggerRegistry.register(ro.ainpc.engine.runtime.triggers.PlayerTalksToNpcTrigger())
        variableProviderRegistry.register(QuestVariableProvider(this))
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

    

    fun flushQuestProgress() {
        val snapshot = questProgressPersistence.snapshotQuestProgress(activePlayerQuests, archivedPlayerQuests)
        val total = snapshot.values.sumOf { it.size }
        questProgressPersistence.persistQuestProgressSnapshot(snapshot)
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

    fun executeRuntimeTrigger(triggerDef: ScenarioRuntimeDefinition?, context: ScenarioExecutionContext) {
        if (triggerDef == null) return
        val report = triggerRegistry.validateDefinition(triggerDef, "trigger")
        if (!report.isValid()) {
            plugin.debug("[Runtime] Trigger invalid: ${triggerDef.id()} - erori: ${report.errors().joinToString("; ")}")
            return
        }
        triggerRegistry.find(triggerDef.type()).ifPresent { handler ->
            handler.bind(context, triggerDef)
        }
        val actionRefs = triggerDef.parameter("action_refs")
        if (actionRefs != null && actionRefs.isNotBlank()) {
            val refIds = actionRefs.split(",").map { it.trim() }.filter { it.isNotBlank() }
            if (refIds.isNotEmpty()) {
                val template = questTemplates[context.templateId()]
                    ?: scenarioTemplates.values.firstOrNull { it.templateId == context.templateId() }
                if (template != null) {
                    for (refId in refIds) {
                        val actionDef = template.runtimeActionDefs.firstOrNull { it.id() == refId }
                        if (actionDef != null) {
                            executeRuntimeAction(actionDef, context)
                        } else {
                            plugin.debug("[Runtime] Actiunea '${refId}' nu a fost gasita in template-ul '${context.templateId()}'")
                        }
                    }
                }
            }
        }
    }

    fun evaluateRuntimeCondition(conditionDef: ScenarioRuntimeDefinition?, context: ScenarioExecutionContext): Boolean {
        if (conditionDef == null) return true
        val report = conditionRegistry.validateDefinition(conditionDef, "conditie")
        if (!report.isValid()) {
            plugin.debug("[Runtime] Conditie invalida: ${conditionDef.id()} - erori: ${report.errors().joinToString("; ")}")
            return true
        }
        return conditionRegistry.find(conditionDef.type())
            .map { handler -> handler.evaluate(context, conditionDef) }
            .orElse(true)
    }
    fun enrichWithProviderVariables(ctx: ScenarioExecutionContext): ScenarioExecutionContext {
        val providerVars = variableProviderRegistry.allVariables(ctx)
        return ScenarioExecutionContext(
            ctx.playerUuid(), ctx.playerName(), ctx.npcId(), ctx.npcName(),
            ctx.regionId(), ctx.placeId(), ctx.nodeId(),
            ctx.templateId(), ctx.progressionId(), ctx.runtimeMode(),
            providerVars + ctx.variables()
        )
    }

    fun fireLifecycleTriggers(
        template: ScenarioTemplate,
        lifecycleType: String,
        player: Player,
        contextVars: Map<String, String>,
    ) {
        if (template.runtimeTriggerDefs.isEmpty()) return
        val triggerCtx = enrichWithProviderVariables(ScenarioExecutionContext(
            player.uniqueId.toString(), player.name, "", "", "", "", "",
            template.templateId, template.templateId, "lifecycle", contextVars
        ))
        for (triggerDef in template.runtimeTriggerDefs) {
            if (triggerDef.type().equals(lifecycleType, ignoreCase = true)) {
                executeRuntimeTrigger(triggerDef, triggerCtx)
            }
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
        val featurePackLoader = plugin.featurePackLoader
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

                template.runtimeConditionDefs = definition.conditions.map { cdef ->
                    val params = LinkedHashMap(cdef)
                    params.remove("id")
                    params.remove("type")
                    ScenarioRuntimeDefinition(cdef["id"], cdef["type"], params.toMap())
                }
                template.runtimeTriggerDefs = definition.runtimeTriggers.map { tdef ->
                    val params = LinkedHashMap(tdef)
                    params.remove("id")
                    params.remove("type")
                    ScenarioRuntimeDefinition(tdef["id"], tdef["type"], params.toMap())
                }
                template.runtimeActionDefs = definition.runtimeActions.map { adef ->
                    val params = LinkedHashMap(adef)
                    params.remove("id")
                    params.remove("type")
                    ScenarioRuntimeDefinition(adef["id"], adef["type"], params.toMap())
                }

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
        val template = questTemplateSelectionService.findQuestTemplateForNpc(p1, playerId)
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
            val resolvedAnchors = questAnchorBindingService.resolve(template, p0, p1)
            if (!resolvedAnchors.valid()) {
                return buildQuestUnavailableResult(template, resolvedAnchors)
            }
            var offeredProgress = questProgressLifecycleService.setInitialQuestProgress(playerId, p0, template)
            offeredProgress = bindQuestProgressToNpc(playerId, template, offeredProgress, p1)
            offeredProgress = bindQuestProgressToAnchors(p0, offeredProgress, resolvedAnchors)
            publishProgressionOffered(p0, p1, template, offeredProgress, availability, "npc_interaction")
            recordQuestStoryEvent(
                "quest_offered",
                p0,
                p1,
                template,
                offeredProgress,
                "Quest oferit",
                "NPC-ul a oferit questul '${resolveQuestTitle(template)}'.",
                "npc_interaction"
            )
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
                buildQuestStatusMessages(template, offeredProgress, p0, p1.name),
                template.templateId
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
            val completedProgress = questProgressFinalizationService.markQuestCompleted(playerId, template)
            publishProgressionCompleted(AINPCEventSource.PLAYER, p0, p1, template, completedProgress)
            recordQuestStoryEvent(
                "quest_completed",
                p0,
                p1,
                template,
                completedProgress,
                "Quest completat",
                "Playerul a finalizat questul '${resolveQuestTitle(template)}'.",
                "player_completion"
            )
            fireLifecycleTriggers(template, "quest_completed", p0, mapOf("npc_id" to p1.uuid.toString()))
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
        val template = questTemplateSelectionService.findQuestTemplateForNpc(p1, playerId, p2)
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
        val resolvedAnchors = questAnchorBindingService.resolve(template, p0, p1)
        if (!resolvedAnchors.valid()) {
            return buildQuestUnavailableResult(template, resolvedAnchors)
        }
        var acceptedProgress = questProgressLifecycleService.setActiveQuestProgress(playerId, p0, template)
        acceptedProgress = bindQuestProgressToNpc(playerId, template, acceptedProgress, p1)
        acceptedProgress = bindQuestProgressToAnchors(p0, acceptedProgress, resolvedAnchors)
        publishProgressionAccepted(p0, p1, template, acceptedProgress)
        recordQuestStoryEvent(
            "quest_accepted",
            p0,
            p1,
            template,
            acceptedProgress,
            "Quest acceptat",
            "Playerul a acceptat questul '${resolveQuestTitle(template)}'.",
            "player_accept"
        )
        fireLifecycleTriggers(template, "quest_accepted", p0, mapOf("npc_id" to p1.uuid.toString()))
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
        val template = questTemplateSelectionService.findQuestTemplateForNpc(p1, playerId, p2)
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
        questProgressStoreService.removeActiveQuestProgress(playerId, template.templateId)
        questProgressPersistence.deleteQuestProgress(playerId, template.templateId)
        publishProgressionDeclined(p0, p1, template, currentProgress)
        recordQuestStoryEvent(
            "quest_declined",
            p0,
            p1,
            template,
            currentProgress,
            "Quest refuzat",
            "Playerul a refuzat questul '${resolveQuestTitle(template)}'.",
            "player_decline"
        )
        fireLifecycleTriggers(template, "quest_declined", p0, mapOf("npc_id" to p1.uuid.toString()))
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
        val template = questTemplateSelectionService.findQuestTemplateForNpc(p1, playerId)
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
        val failedProgress = questProgressFinalizationService.markQuestFailed(playerId, template)
        publishProgressionAbandoned(AINPCEventSource.PLAYER, p0, p1, template, failedProgress)
        recordQuestStoryEvent(
            "quest_abandoned",
            p0,
            p1,
            template,
            failedProgress,
            "Quest abandonat",
            "Playerul a abandonat questul '${resolveQuestTitle(template)}'.",
            "player_abandon"
        )
        fireLifecycleTriggers(template, "quest_abandoned", p0, mapOf("npc_id" to p1.uuid.toString()))
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
        val failedProgress = questProgressFinalizationService.markQuestFailed(playerId, template)
        publishProgressionAbandoned(AINPCEventSource.COMMAND, p0, null, template, failedProgress)
        fireLifecycleTriggers(template, "quest_abandoned", p0, emptyMap())
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
        val template = questTemplateSelectionService.findQuestTemplateForNpc(p1, playerId, p2)
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
        val resolvedAnchors = questAnchorBindingService.resolve(template, p0, p1)
        if (!resolvedAnchors.valid()) {
            return buildQuestUnavailableResult(template, resolvedAnchors)
        }
        questProgressStoreService.removeArchivedQuestProgress(playerId, template.templateId)
        var offeredProgress = questProgressLifecycleService.setInitialQuestProgress(playerId, p0, template)
        offeredProgress = bindQuestProgressToNpc(playerId, template, offeredProgress, p1)
        offeredProgress = bindQuestProgressToAnchors(p0, offeredProgress, resolvedAnchors)
        publishProgressionOffered(p0, p1, template, offeredProgress, availability, "manual_start")
        recordQuestStoryEvent(
            "quest_offered",
            p0,
            p1,
            template,
            offeredProgress,
            "Quest pornit manual",
            "Questul '${resolveQuestTitle(template)}' a fost pornit manual.",
            "manual_start"
        )
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
            buildQuestStatusMessages(template, offeredProgress, p0, p1.name),
            template.templateId
        )
    }
    fun resetQuestProgress(p0: Player, p1: AINPC): Boolean {
        if (p0 == null || p1 == null) {
            plugin.debug("[QuestEngine] resetQuestProgress oprit: player sau npc este null.")
            return false
        }
        val playerId = p0.uniqueId
        val template = questTemplateSelectionService.findQuestTemplateForNpc(p1, playerId)
        if (template == null || !template.hasQuestBriefing()) {
            plugin.debug("[QuestEngine] resetQuestProgress fara template pentru npc=" + p1.name)
            return false
        }
        val removedActive = questProgressStoreService.removeActiveQuestProgress(playerId, template.templateId)
        val removedArchived = questProgressStoreService.removeArchivedQuestProgress(playerId, template.templateId)
        if (!removedActive && !removedArchived) {
            plugin.debug("[QuestEngine] resetQuestProgress fara progres potrivit pentru player="
                + p0.name + " templateId=" + template.templateId)
            return false
        }
        questProgressCleanupService.clearQuestTrackingIfMatches(playerId, template.templateId)
        questProgressPersistence.deleteQuestProgress(playerId, template.templateId)
        fireLifecycleTriggers(template, "quest_reset", p0, mapOf("npc_id" to p1.uuid.toString()))
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
        val template = questTemplateSelectionService.findQuestTemplateForNpc(p1, playerId)
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
            val completedProgress = questProgressFinalizationService.markQuestCompleted(playerId, template)
            publishProgressionCompleted(AINPCEventSource.COMMAND, p0, p1, template, completedProgress)
            recordQuestStoryEvent(
                "quest_completed",
                p0,
                p1,
                template,
                completedProgress,
                "Quest completat",
                "Questul '${resolveQuestTitle(template)}' a fost completat din comanda manuala.",
                "command_completion"
            )
            fireLifecycleTriggers(template, "quest_completed", p0, mapOf("npc_id" to p1.uuid.toString()))
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
        val template = questTemplateSelectionService.findQuestTemplateForNpc(p0)
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
        return resolveActiveQuestNpc(p0, p2, p1) != null
    }
    
    fun getQuestStatus(p0: Player, p1: AINPC): QuestInteractionResult {
        if (p0 == null || p1 == null) return QuestInteractionResult.notHandled()
        val playerId = p0.uniqueId
        val template = questTemplateSelectionService.findQuestTemplateForNpc(p1, playerId)
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
    
    fun getQuestLog(p0: Player, p1: String): QuestInteractionResult = getQuestLog(p0, p1, false)
    fun getQuestLog(p0: Player, p1: String, p2: Boolean): QuestInteractionResult {
        if (!p2) {
            return questLogViewBuilder.build(p0, p1, false)
        }
        return questLogViewBuilder.build(p0, p1, true)
    }
    fun getQuestGuiSnapshot(p0: Player, p1: String, p2: Boolean): QuestGuiSnapshot {
        if (p0 == null) {
            return QuestGuiSnapshot.empty()
        }
        val playerId = p0.uniqueId
        val logFilter = parseQuestLogFilter(p1)
        val currentProgresses = getCurrentQuestProgress(playerId)
        val archivedProgresses = questProgressLookupService.getArchivedQuestProgress(playerId)
        val summaryLines = if (currentProgresses.isEmpty())
            listOf("&7Nu ai progresie activa.")
        else
            questLogSupportService.buildQuestLogSummaryLines(playerId, currentProgresses)
        val currentEntries = currentProgresses
            .filter { questLogSupportService.questLogMatches(playerId, it, logFilter, false) }
            .sortedWith(questLogSupportService.questLogCurrentComparator(playerId))
            .map { questGuiEntryBuilder.build(p0, playerId, it, false, p2) }
        val archivedLimit = if (logFilter == QuestLogFilter.SUMMARY) 3 else 20
        val archivedEntries = archivedProgresses
            .filter { questLogSupportService.questLogMatches(playerId, it, logFilter, true) }
            .take(archivedLimit)
            .map { questGuiEntryBuilder.build(p0, playerId, it, true, p2) }
        val totalMatchingArchived = archivedProgresses.count {
            questLogSupportService.questLogMatches(playerId, it, logFilter, true)
        }.toLong()
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
    fun getQuestStatus(p0: Player, p1: String): QuestInteractionResult {
        return questProgressViewBuilder.buildPlayerStatus(p0, p1)
    }
    fun getQuestDebug(p0: Player, p1: String): QuestInteractionResult {
        return questProgressViewBuilder.buildDebug(p0, p1)
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
            questProgressPersistence.persistQuestProgressAsync(p0.uniqueId, selectedProgress)
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
            questProgressPersistence.persistQuestTrackingPreferenceAsync(p0.uniqueId, "")
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
                questProgressPersistence.persistQuestTrackingPreferenceAsync(playerId, "")
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
        val loc = p1.location ?: return
        if (loc.world != p0.world) return
        spawnQuestWaypointParticles(p0, loc)
        val distance = loc.distance(p0.location)
        if (distance > 3.0 && distance < 64.0) {
            spawnQuestDirectionParticles(p0, loc, distance)
        }
    }
    private fun spawnQuestDirectionParticles(p0: Player, p1: Location, p2: Double) {
        val from = p0.location.clone().add(0.0, 1.0, 0.0)
        val to = p1.clone().add(0.0, 1.0, 0.0)
        val direction = to.toVector().subtract(from.toVector()).normalize()
        p0.spawnParticle(
            org.bukkit.Particle.END_ROD,
            from.add(direction.multiply(2.0)),
            0, direction.x, direction.y, direction.z, 0.5
        )
    }
    private fun spawnQuestWaypointParticles(p0: Player, p1: Location) {
        val distance = if (p1.world == p0.world) p1.distance(p0.location) else Double.MAX_VALUE
        if (distance < 48.0) {
            val beamLoc = p1.clone().add(0.0, 1.0, 0.0)
            p0.spawnParticle(org.bukkit.Particle.COMPOSTER, beamLoc, 3, 0.5, 1.0, 0.5, 0.0)
        }
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
        val region = findCurrentRegion(p0.location)
        val place = findCurrentPlace(p0.location)
        recordStructureStoryEvent(p0, region, place, node, "node_interaction")
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
                metadata = linkedMapOf("nodeId" to node.id)
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

        if (region != null) {
            val triggerVars = mapOf("region_id" to region.id)
            val triggerCtx = enrichWithProviderVariables(ScenarioExecutionContext(
                p0.uniqueId.toString(), p0.name, "", "", region.id,
                place?.id.orEmpty(), node?.id.orEmpty(),
                "", "", "trigger", triggerVars
            ))
            executeRuntimeTrigger(
                ScenarioRuntimeDefinition("region_enter_${region.id}", "player_enters_region", triggerVars),
                triggerCtx
            )
        }
        if (place != null) {
            val triggerVars = mapOf("place_id" to place.id)
            val triggerCtx = enrichWithProviderVariables(ScenarioExecutionContext(
                p0.uniqueId.toString(), p0.name, "", "", region?.id.orEmpty(), place.id,
                node?.id.orEmpty(), "", "", "trigger", triggerVars
            ))
            executeRuntimeTrigger(
                ScenarioRuntimeDefinition("place_enter_${place.id}", "player_enters_place", triggerVars),
                triggerCtx
            )
        }
        if (node != null) {
            val triggerVars = mapOf("node_id" to node.id)
            val triggerCtx = enrichWithProviderVariables(ScenarioExecutionContext(
                p0.uniqueId.toString(), p0.name, "", "", region?.id.orEmpty(),
                place?.id.orEmpty(), node.id, "", "", "trigger", triggerVars
            ))
            executeRuntimeTrigger(
                ScenarioRuntimeDefinition("node_enter_${node.id}", "player_enters_node", triggerVars),
                triggerCtx
            )
        }

        recordStructureStoryEvent(p0, region, place, node, "place_visit")

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
    fun cleanupOrphanedObjectives() = questProgressCleanupService.cleanupOrphanedObjectives()

    fun cleanupStaleTemplateProgress() = questProgressCleanupService.cleanupStaleTemplateProgress()

    private fun isDebounced(playerId: UUID, eventKey: String, cooldownMs: Long = 500L): Boolean {
        val fullKey = "$playerId:$eventKey"
        val now = System.currentTimeMillis()
        val last = eventDebounceBuffer.getOrDefault(fullKey, 0L)
        if (now - last < cooldownMs) return true
        eventDebounceBuffer[fullKey] = now
        return false
    }

    private fun publishProgressionAccepted(
        player: Player,
        npc: AINPC?,
        template: ScenarioTemplate,
        progress: PlayerQuestProgress
    ) {
        progressionLifecyclePublisher.publishAccepted(player, npc, template, progress)
    }

    private fun publishProgressionAbandoned(
        source: AINPCEventSource,
        player: Player,
        npc: AINPC?,
        template: ScenarioTemplate,
        progress: PlayerQuestProgress
    ) {
        progressionLifecyclePublisher.publishAbandoned(source, player, npc, template, progress)
    }

    private fun publishProgressionDeclined(
        player: Player,
        npc: AINPC?,
        template: ScenarioTemplate,
        progress: PlayerQuestProgress
    ) {
        progressionLifecyclePublisher.publishDeclined(player, npc, template, progress)
    }

    private fun publishProgressionCompleted(
        source: AINPCEventSource,
        player: Player,
        npc: AINPC?,
        template: ScenarioTemplate,
        progress: PlayerQuestProgress
    ) {
        progressionLifecyclePublisher.publishCompleted(source, player, npc, template, progress)
    }

    private fun recordQuestStoryEvent(
        eventType: String,
        player: Player,
        npc: AINPC?,
        template: ScenarioTemplate,
        progress: PlayerQuestProgress?,
        title: String,
        description: String,
        interaction: String,
        extraPayload: Map<String, String> = emptyMap()
    ) {
        val anchorLocation = npc?.location ?: player.location
        val region = if (anchorLocation != null) findCurrentRegion(anchorLocation) else null
        val place = if (anchorLocation != null) findCurrentPlace(anchorLocation) else null
        val node = if (anchorLocation != null) findCurrentNode(anchorLocation) else null
        val payload = LinkedHashMap<String, String>()
        payload["quest_template_id"] = template.templateId
        payload["quest_code"] = template.questCode
        payload["quest_title"] = resolveQuestTitle(template)
        payload["quest_status"] = progress?.status()?.name.orEmpty()
        payload["quest_phase"] = progress?.currentPhase().orEmpty()
        payload["interaction"] = interaction
        payload["player_name"] = player.name
        payload["player_uuid"] = player.uniqueId.toString()
        payload["npc_name"] = npc?.name.orEmpty()
        payload["npc_uuid"] = npc?.uuid?.toString().orEmpty()
        payload["npc_database_id"] = npc?.databaseId?.takeIf { it > 0 }?.toString().orEmpty()
        payload["node_id"] = node?.id.orEmpty()
        payload["place_id"] = place?.id.orEmpty()
        payload["region_id"] = region?.id.orEmpty()
        payload.putAll(extraPayload)
        try {
            plugin.storyStateService.recordEvent(
                "quest",
                template.templateId,
                region?.id,
                place?.id,
                eventType,
                template.questCode.ifBlank { template.templateId },
                title,
                description,
                payload,
                "player",
                player.uniqueId.toString(),
                player.uniqueId.toString(),
                npc?.uuid?.toString()
            )
        } catch (exception: SQLException) {
            plugin.logger.log(Level.WARNING, "Nu s-a putut inregistra evenimentul de quest in story_events.", exception)
        }
    }

    private fun recordStructureStoryEvent(
        player: Player,
        region: WorldRegion?,
        place: WorldPlace?,
        node: WorldNode?,
        interaction: String
    ) {
        val plan = when (interaction) {
            "node_interaction" -> structureStoryEventPlanner.planNodeInteraction(region?.id, place, node)
            else -> structureStoryEventPlanner.planPlaceVisit(region?.id, place, node)
        } ?: return
        val debounceKey = buildStructureStoryEventDebounceKey(plan)
        if (isDebounced(player.uniqueId, debounceKey, structureStoryEventCooldownMs)) {
            return
        }

        try {
            plugin.storyStateService.recordEvent(
                plan.scopeType,
                plan.scopeId,
                plan.regionId.ifBlank { region?.id.orEmpty() },
                plan.placeId.ifBlank { place?.id.orEmpty() },
                plan.eventType,
                plan.eventKey,
                plan.title,
                plan.description,
                plan.payload,
                "player",
                player.uniqueId.toString(),
                player.uniqueId.toString(),
                null
            )
        } catch (exception: SQLException) {
            plugin.logger.log(Level.WARNING, "Nu s-a putut inregistra story event-ul structural in story_events.", exception)
        }
    }

    private fun buildStructureStoryEventDebounceKey(plan: PlannedStructureStoryEvent): String {
        val payloadNodeId = plan.payload["node_id"].orEmpty()
        val payloadPlaceId = plan.payload["place_id"].orEmpty()
        return listOf(
            plan.scopeType,
            plan.scopeId,
            plan.eventType,
            plan.eventKey,
            payloadPlaceId,
            payloadNodeId
        ).joinToString("|")
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
        progressionEventPublisher.publishOffered(player, npc, template, progress, availability, offerReason)
    }

    private fun publishProgressionTrackingChanged(
        player: Player,
        progress: PlayerQuestProgress?,
        marker: QuestTrackingMarker?,
        trackingActive: Boolean,
        trackingAction: String,
        metadata: Map<String, String>
    ) {
        progressionEventPublisher.publishTrackingChanged(player, progress, marker, trackingActive, trackingAction, metadata)
    }

    private fun publishProgressionAnchorsBound(
        player: Player,
        progress: PlayerQuestProgress,
        resolvedAnchors: QuestAnchorResolver.ResolvedQuestAnchors,
        metadata: Map<String, String>
    ) {
        progressionEventPublisher.publishAnchorsBound(player, progress, resolvedAnchors, metadata)
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
        progressionEventPublisher.publishStageChanged(source, player, npc, template, progress, previousStageId, newStageId, reason, metadata)
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

    fun getCurrentQuestProgress(playerId: UUID): List<PlayerQuestProgress> {
        return questProgressLookupService.getCurrentQuestProgress(playerId)
    }

    fun getCurrentQuestProgress(playerId: UUID?, templateId: String?): PlayerQuestProgress? {
        return questProgressLookupService.getCurrentQuestProgress(playerId, templateId)
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
    private fun putActiveQuestProgress(p0: UUID, p1: PlayerQuestProgress) {
        questProgressStoreService.putActiveQuestProgress(p0, p1)
    }
    private fun getArchivedQuestProgress(p0: UUID, p1: String): PlayerQuestProgress? {
        return questProgressLookupService.getArchivedQuestProgress(p0, p1)
    }
    fun getCompletedQuestProgress(p0: UUID, p1: String): PlayerQuestProgress? {
        return getArchivedQuestProgress(p0, p1)?.takeIf { it.isCompleted() }
    }
    private fun getFailedQuestProgress(p0: UUID, p1: String): PlayerQuestProgress? {
        return getArchivedQuestProgress(p0, p1)?.takeIf { it.status() == QuestStatus.FAILED }
    }
    fun hasCompletedQuest(p0: UUID, p1: String): Boolean {
        return getCompletedQuestProgress(p0, p1) != null
    }
    private fun evaluateQuestAvailability(p0: UUID, p1: ScenarioTemplate): QuestAvailability {
        val issues = mutableListOf<String>()

        val contextVars = mutableMapOf<String, String>()
        contextVars["player_uuid"] = p0.toString()
        contextVars["mechanic_id"] = p1.progressionMechanicId

        if (hasCompletedQuest(p0, p1.templateId)) {
            contextVars["quest_completed_${p1.templateId}"] = "true"
            val completedProgress = getCompletedQuestProgress(p0, p1.templateId)
            if (completedProgress != null) {
                contextVars["quest_completed_${p1.templateId}_at"] = completedProgress.completedAt().toString()
            }
        }

        val mechanicCount = countCurrentProgressionsInMechanic(p0, p1, p1.progressionMechanicId)
        contextVars["mechanic_active_count_${p1.progressionMechanicId}"] = mechanicCount.toString()

        val execContext = enrichWithProviderVariables(ScenarioExecutionContext(
            p0.toString(), "", "", "", "", "", "",
            p1.templateId, p1.templateId, "quest_availability", contextVars
        ))

        if (!p1.questRepeatable) {
            val completedDef = ScenarioRuntimeDefinition(
                "completed_check", "has_completed_quest",
                mapOf("template_id" to p1.templateId)
            )
            val completedReport = conditionRegistry.validateDefinition(completedDef, "has_completed_quest")
            if (completedReport.isValid()) {
                val completedHandler = conditionRegistry.find("has_completed_quest")
                if (completedHandler.isPresent && completedHandler.get().evaluate(execContext, completedDef)) {
                    issues.add("Quest deja completat.")
                }
            }
        }

        if (p1.questRepeatable && p1.questCooldownSeconds > 0) {
            val cooldownDef = ScenarioRuntimeDefinition(
                "cooldown_check", "quest_cooldown",
                mapOf("template_id" to p1.templateId, "seconds" to p1.questCooldownSeconds.toString())
            )
            val cooldownReport = conditionRegistry.validateDefinition(cooldownDef, "quest_cooldown")
            if (cooldownReport.isValid()) {
                val cooldownHandler = conditionRegistry.find("quest_cooldown")
                if (cooldownHandler.isPresent && !cooldownHandler.get().evaluate(execContext, cooldownDef)) {
                    issues.add("Mai asteapta inainte sa reiei acest quest.")
                }
            }
        }

        if (p1.questPrerequisites.isNotEmpty()) {
            for (prereq in p1.questPrerequisites) {
                val prereqDef = ScenarioRuntimeDefinition(
                    "prerequisite_check", "quest_prerequisite",
                    mapOf("template_id" to prereq)
                )
                val prereqReport = conditionRegistry.validateDefinition(prereqDef, "quest_prerequisite")
                if (prereqReport.isValid()) {
                    val prereqHandler = conditionRegistry.find("quest_prerequisite")
                    if (prereqHandler.isPresent && !prereqHandler.get().evaluate(execContext, prereqDef)) {
                        issues.add("Completeaza mai intai: $prereq")
                    }
                }
            }
        }

        val limit = getProgressionMechanicLimit(p1)
        if (limit > 0) {
            val limitDef = ScenarioRuntimeDefinition(
                "mechanic_limit_check", "mechanic_limit",
                mapOf("mechanic_id" to p1.progressionMechanicId, "max_active" to limit.toString())
            )
            val limitReport = conditionRegistry.validateDefinition(limitDef, "mechanic_limit")
            if (limitReport.isValid()) {
                val limitHandler = conditionRegistry.find("mechanic_limit")
                if (limitHandler.isPresent && !limitHandler.get().evaluate(execContext, limitDef)) {
                    issues.add("Ai atins limita de ${p1.progressionLabel.lowercase()} active.")
                }
            }
        }

        for (condDef in p1.runtimeConditionDefs) {
            if (!evaluateRuntimeCondition(condDef, execContext)) {
                issues.add("Conditie runtime neindeplinita: ${condDef.id()}")
            }
        }

        return if (issues.isEmpty()) QuestAvailability.allowed()
        else QuestAvailability.unavailable(issues)
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
            val resolvedAnchors = questAnchorBindingService.resolve(nextTemplate, p0, npc)
            if (!resolvedAnchors.valid()) {
                plugin.debug("[Chain] Ancore nerezolvate pentru ${nextTemplate.templateId}, sar peste lant.")
                continue
            }
            questProgressStoreService.removeArchivedQuestProgress(p0.uniqueId, nextTemplate.templateId)
            var offered = questProgressLifecycleService.setInitialQuestProgress(p0.uniqueId, p0, nextTemplate)
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
    fun countCurrentProgressionsInMechanic(p0: UUID, p1: ScenarioTemplate?, p2: String): Int {
        val current = getCurrentQuestProgress(p0)
        return current.count {
            val mechanic = it.templateId()?.let { id ->
                questTemplates[id]?.progressionMechanicId
            }
            mechanic == p2 || (p1 != null && mechanic == p1.progressionMechanicId)
        }
    }
    private fun buildQuestLogActionLines(p0: Player, p1: UUID, p2: ScenarioTemplate?, p3: PlayerQuestProgress, p4: Boolean): List<String> {
        val lines = mutableListOf<String>()
        if (p2 == null) {
            lines.add("&8Actiuni: &fstatus in chat")
            return lines
        }

        val selector = questLogActionSelector(p2, p3)
        if (selector.isNotBlank()) {
            lines.add("&8Selector: &f$selector")
        }
        when {
            p3.isOffered() -> lines.add("&8Actiune: &faccepta progresia din dialog")
            p3.isActive() -> lines.add("&8Actiune: &fcontinua progresia si urmareste obiectivele")
            p3.status()?.isArchived() == true -> lines.add("&8Actiune: &frevizualizeaza progresia din jurnal")
            else -> lines.add("&8Actiune: &fdeschide detaliile progresiei")
        }
        if (p3.currentPhase().isNotBlank()) {
            lines.add("&8Faza curenta: &f${formatQuestPhase(p3.currentPhase())}")
        }
        if (p4) {
            lines.add("&8Admin: &ffiltreaza / quest log pentru analiza extinsa")
        }
        return lines
    }
    
    

    

    fun snapshotQuestProgressPublic(): Map<UUID, List<PlayerQuestProgress>> =
        questProgressPersistence.snapshotQuestProgress(activePlayerQuests, archivedPlayerQuests)

    fun findQuestTemplate(templateId: String): ScenarioTemplate? = questTemplates[templateId]

    var templatesLoadedAt: Long = System.currentTimeMillis()

    

    

    

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

    

    

    fun loadPlayerQuests() {
        questProgressPersistence.loadPlayerQuests(activePlayerQuests, archivedPlayerQuests) { message -> plugin.debug(message) }
    }
    private fun refreshTrackedQuestProgress(p0: Player, p1: ScenarioTemplate, p2: PlayerQuestProgress): PlayerQuestProgress {
        updateTrackedQuestProgress(p0.uniqueId, p1, p2, p2.objectiveProgress())
        return p2
    }
    private fun trackNpcObjectiveProgress(p0: Player, p1: AINPC, p2: ScenarioTemplate, p3: PlayerQuestProgress): PlayerQuestProgress {
        val updatedVariables = p3.questVariables().toMutableMap()
        updatedVariables["quest_giver_name"] = p1.name
        val updatedProgress = PlayerQuestProgress(p3.templateId(), p3.questCode(), p3.status(),
            p3.startedAt(), p3.completedAt(), System.currentTimeMillis(), p3.currentPhase(),
            p3.objectiveProgress(), updatedVariables)
        updateTrackedQuestProgress(p0.uniqueId, p2, updatedProgress, p3.objectiveProgress())
        val npcProgress = markNpcTalkObjective(p0, p1, p2, p3)
        if (npcProgress != p3) return npcProgress
        return updatedProgress
    }

    private fun markNpcTalkObjective(p0: Player, p1: AINPC, p2: ScenarioTemplate, p3: PlayerQuestProgress): PlayerQuestProgress {
        if (p2.objectives.isEmpty()) return p3
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
        updatedVars["quest_giver_name"] = p1.name
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
        val updatedVariables = p2.questVariables().toMutableMap()
        updatedVariables["quest_giver_npc_id"] = p3.databaseId.toString()
        updatedVariables["quest_giver_npc_name"] = p3.name
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
    private fun matchesProgressionKindFilter(p0: ScenarioTemplate, p1: String): Boolean {
        return p1.isBlank() || p0.progressionKind.equals(p1, ignoreCase = true)
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

            val target = questStoryTargetResolver.resolveStoryActionTarget(entry, p0, p3)
            val scopeId = questStoryTargetResolver.resolveStoryScopeId(actionType, target.scopeId(), p0, p3)
            val anchorReference = questStoryTargetResolver.resolveStoryAnchorReference(getQuestEntryMetadata(entry, "anchor", "anchor_ref", "reference", "target"), p3)
            val metadata = LinkedHashMap<String, String>()
            metadata.putAll(entry.metadata)
            metadata.putAll(entry.variables)
            metadata.putAll(entry.payload)
            metadata["scope"] = target.scopeType()
            metadata["scopeId"] = scopeId
            if (anchorReference.isNotBlank()) {
                metadata["anchorReference"] = anchorReference
            }

            questStoryActionEventPublisher.publish(
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
    private fun updateTrackedQuestProgress(p0: UUID, p1: ScenarioTemplate, p2: PlayerQuestProgress, p3: Map<String, Int>): PlayerQuestProgress {
        val currentPhase = resolveQuestPhase(p1, p2.status(), p2.currentPhase(), p3)
        val updatedProgress = PlayerQuestProgress(p2.templateId(), p2.questCode(), p2.status(),
            p2.startedAt(), p2.completedAt(), System.currentTimeMillis(), currentPhase, p3, p2.questVariables())
        putActiveQuestProgress(p0, updatedProgress)
        questProgressPersistence.persistQuestProgressAsync(p0, updatedProgress)
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

        plugin.debug(
            "Spawn actor scenariu=" + activeScenario.templateId +
                " actor=" + actorDefinition.id +
                " npc=" + npc.name +
                " la " + location
        )
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
        plugin.logger.info("Scenariu terminat: " + scenario.displayName
            + " (ID: " + p0.toString().substring(0, 8) + ")")
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

    
}
