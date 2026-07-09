package ro.ainpc.bootstrap

import org.bukkit.configuration.file.FileConfiguration
import ro.ainpc.AINPCPlugin
import ro.ainpc.ai.DialogManager
import ro.ainpc.ai.OllamaService
import ro.ainpc.ai.OpenAIService
import ro.ainpc.ai.RelationshipService
import ro.ainpc.ai.orchestration.AIOrchestrationService
import ro.ainpc.bootstrap.PackFileWatcher
import ro.ainpc.bootstrap.PerformanceMonitor
import ro.ainpc.bootstrap.SchedulerCoordinator
import ro.ainpc.commands.AINPCCommand
import ro.ainpc.commands.AINPCTabCompleter
import ro.ainpc.database.DatabaseManager
import ro.ainpc.debug.RecentEventsBuffer
import ro.ainpc.economy.BankingService
import ro.ainpc.economy.EconomyService
import ro.ainpc.economy.NpcEconomyService
import ro.ainpc.economy.ShopService
import ro.ainpc.economy.VaultEconomyHook
import ro.ainpc.engine.DecisionEngine
import ro.ainpc.engine.DialogueEngine
import ro.ainpc.engine.FeaturePackLoader
import ro.ainpc.engine.QuestAuthoringService
import ro.ainpc.engine.ScenarioEngine
import ro.ainpc.engine.ScriptConfigurationLoader
import ro.ainpc.environment.EnvironmentEngine
import ro.ainpc.environment.SeasonalBehaviorService
import ro.ainpc.gui.GuiService
import ro.ainpc.listeners.ListenerRegistry
import ro.ainpc.managers.ConversationSessionManager
import ro.ainpc.managers.EmotionManager
import ro.ainpc.managers.FamilyManager
import ro.ainpc.managers.MemoryManager
import ro.ainpc.managers.NPCManager
import ro.ainpc.mcp.McpRuntimeClient
import ro.ainpc.mcp.McpRuntimeClientFactory
import ro.ainpc.mcp.McpRuntimeConfig
import ro.ainpc.mcp.bridge.McpCommandQueue
import ro.ainpc.mcp.bridge.RuntimeSnapshotProducer
import ro.ainpc.platform.AINPCPlatform
import ro.ainpc.progression.PlayerProgressionService
import ro.ainpc.progression.ProgressionService
import ro.ainpc.reputation.ReputationService
import ro.ainpc.routine.RoutineCoordinator
import ro.ainpc.routine.RoutineService
import ro.ainpc.routine.SocialCoordinator
import ro.ainpc.spawn.AutoSettlementGenerator
import ro.ainpc.spawn.HouseholdPersistenceService
import ro.ainpc.spawn.NpcSpawnOrchestrator
import ro.ainpc.story.RandomWorldEventService
import ro.ainpc.story.StoryAuthoringService
import ro.ainpc.story.StoryContextService
import ro.ainpc.story.StoryReactionService
import ro.ainpc.story.StoryStateService
import ro.ainpc.utils.MessageUtils
import ro.ainpc.world.NpcWorldBindingService
import ro.ainpc.world.mapping.MappingWandService
import java.nio.file.Path
import java.util.logging.Level

class ServiceRegistry(private val plugin: AINPCPlugin) {

    lateinit var databaseManager: DatabaseManager
        private set
    lateinit var messageUtils: MessageUtils
        private set
    lateinit var platform: AINPCPlatform
        private set
    lateinit var npcWorldBindingService: NpcWorldBindingService
        private set
    lateinit var householdPersistenceService: HouseholdPersistenceService
        private set
    lateinit var openAIService: OpenAIService
        private set
    lateinit var ollamaService: OllamaService
        private set
    lateinit var mcpRuntimeClient: McpRuntimeClient
        private set
    var snapshotProducer: RuntimeSnapshotProducer? = null
        private set
    lateinit var mcpCommandQueue: McpCommandQueue
        private set
    lateinit var performanceMonitor: PerformanceMonitor
        private set
    lateinit var aiOrchestrationService: AIOrchestrationService
        private set
    lateinit var featurePackLoader: FeaturePackLoader
        private set
    lateinit var memoryManager: MemoryManager
        private set
    lateinit var emotionManager: EmotionManager
        private set
    lateinit var familyManager: FamilyManager
        private set
    lateinit var npcManager: NPCManager
        private set
    lateinit var routineService: RoutineService
        private set
    lateinit var routineCoordinator: RoutineCoordinator
        private set
    lateinit var autoSettlementGenerator: AutoSettlementGenerator
        private set
    lateinit var npcSpawnOrchestrator: NpcSpawnOrchestrator
        private set
    lateinit var conversationSessionManager: ConversationSessionManager
        private set
    lateinit var dialogManager: DialogManager
        private set
    lateinit var decisionEngine: DecisionEngine
        private set
    lateinit var dialogueEngine: DialogueEngine
        private set
    lateinit var scenarioEngine: ScenarioEngine
        private set
    lateinit var progressionService: ProgressionService
        private set
    lateinit var playerProgressionService: PlayerProgressionService
        private set
    lateinit var storyStateService: StoryStateService
        private set
    lateinit var storyContextService: StoryContextService
        private set
    lateinit var storyReactionService: StoryReactionService
        private set
    lateinit var randomWorldEventService: RandomWorldEventService
        private set
    lateinit var storyAuthoringService: StoryAuthoringService
        private set
    lateinit var environmentEngine: EnvironmentEngine
        private set
    lateinit var seasonalBehaviorService: SeasonalBehaviorService
        private set
    lateinit var authoringService: QuestAuthoringService
        private set
    lateinit var guiService: GuiService
        private set
    lateinit var mappingWandService: MappingWandService
        private set
    lateinit var economyService: EconomyService
        private set
    lateinit var shopService: ShopService
        private set
    lateinit var npcEconomyService: NpcEconomyService
        private set
    lateinit var bankingService: BankingService
        private set
    lateinit var vaultEconomyHook: VaultEconomyHook
        private set
    lateinit var relationshipService: RelationshipService
        private set
    lateinit var reputationService: ReputationService
        private set
    lateinit var socialCoordinator: SocialCoordinator
        private set
    lateinit var recentEventsBuffer: RecentEventsBuffer
        private set
    lateinit var listenerRegistry: ListenerRegistry
        private set
    lateinit var packFileWatcher: PackFileWatcher
        private set
    lateinit var schedulerCoordinator: SchedulerCoordinator
        private set
    lateinit var questConfigFile: java.io.File
        private set
    lateinit var questConfig: FileConfiguration
        private set

    fun initialize(): InitializationResult {
        val log = plugin.logger
        log.info("=== Pornire initializare AINPC ServiceRegistry ===")

        try {
            phase1ConfigAndResources()
            phase2Database()
            phase3CorePlatform()
            phase4AIServices()
            phase5FeaturePacks()
            phase6Managers()
            phase7Engines()
            phase8CommandsAndListeners()
            phase9Starters()
            phase10Report()
            log.info("=== Initializare AINPC ServiceRegistry completa ===")
            return InitializationResult.SUCCESS
        } catch (e: Exception) {
            log.log(Level.SEVERE, "Eroare critica la initializarea ServiceRegistry", e)
            return InitializationResult.failure(e)
        }
    }

    private fun phase1ConfigAndResources() {
        val log = plugin.logger
        databaseManager = DatabaseManager(plugin)
        messageUtils = MessageUtils(plugin)
        platform = AINPCPlatform(plugin)
        platform.initialize()
        npcWorldBindingService = NpcWorldBindingService(plugin)
        householdPersistenceService = HouseholdPersistenceService(plugin)
        log.info("[Faza 1/10] Configuratie si resurse incarcate")
    }

    private fun phase2Database() {
        if (!databaseManager.initialize()) {
            throw IllegalStateException("Nu s-a putut initializa baza de date!")
        }
        plugin.logger.info("[Faza 2/10] Baza de date initializata")
    }

    private fun phase3CorePlatform() {
        platform.reloadFromConfig()
        plugin.logger.info("[Faza 3/10] Platforma AINPC initializata")
    }

    private fun phase4AIServices() {
        val log = plugin.logger
        openAIService = OpenAIService(plugin)
        ollamaService = OllamaService(plugin)
        val snapshotPath = Path.of(McpRuntimeConfig.from(plugin.config).snapshotPath)
        snapshotProducer = RuntimeSnapshotProducer(plugin, snapshotPath).also { it.start() }
        performanceMonitor = PerformanceMonitor(plugin)
        mcpCommandQueue = McpCommandQueue(plugin)
        mcpCommandQueue.start()
        mcpRuntimeClient = McpRuntimeClientFactory.create(plugin)
        aiOrchestrationService = AIOrchestrationService(plugin)
        openAIService.runDiagnosticsAsync("startup")
        log.info("[Faza 4/10] Servicii AI initializate")
    }

    private fun phase5FeaturePacks() {
        featurePackLoader = FeaturePackLoader(plugin)
        featurePackLoader.loadAllPacks()
        plugin.logger.info("[Faza 5/10] Feature pack-uri incarcate")
    }

    private fun phase6Managers() {
        memoryManager = MemoryManager(plugin)
        emotionManager = EmotionManager(plugin)
        familyManager = FamilyManager(plugin)
        npcManager = NPCManager(plugin)
        routineService = RoutineService(plugin)
        routineCoordinator = RoutineCoordinator(plugin)
        autoSettlementGenerator = AutoSettlementGenerator(plugin)
        npcSpawnOrchestrator = NpcSpawnOrchestrator(plugin)
        dialogManager = DialogManager(plugin)
        conversationSessionManager = ConversationSessionManager(plugin)

        npcManager.loadAllNPCs()
        npcManager.discoverExistingVillagers()
        npcManager.reconcileDuplicateLiveNPCEntities("startup")
        npcManager.restoreMissingNPCsInLoadedChunks()
        npcManager.enforceControlledEntitySettings("startup")

        val backfilledProfiles = npcManager.ensureAllNPCsHaveProfiles()
        val backfilledWorldBindings = npcManager.backfillWorldBindingsFromAnchors()
        plugin.logger.info("Profiluri NPC verificate. Profiluri create/backfill: $backfilledProfiles")
        plugin.logger.info("Binding-uri NPC-world inferate/backfill: $backfilledWorldBindings")
        plugin.logger.info("[Faza 6/10] Manageri initializati si NPC-uri incarcate")
    }

    private fun phase7Engines() {
        decisionEngine = DecisionEngine(plugin)
        dialogueEngine = DialogueEngine(plugin, openAIService)
        scenarioEngine = ScenarioEngine(plugin)
        progressionService = ProgressionService(plugin)
        playerProgressionService = PlayerProgressionService(plugin)
        storyStateService = StoryStateService(plugin)
        storyContextService = StoryContextService(plugin)
        storyReactionService = StoryReactionService(plugin)
        randomWorldEventService = RandomWorldEventService(plugin)
        storyAuthoringService = StoryAuthoringService(plugin)
        environmentEngine = EnvironmentEngine(plugin)
        seasonalBehaviorService = SeasonalBehaviorService(plugin)
        authoringService = QuestAuthoringService()
        guiService = GuiService(plugin)
        mappingWandService = MappingWandService(plugin)
        economyService = EconomyService(plugin)
        npcEconomyService = NpcEconomyService(plugin)
        shopService = ShopService(economyService, npcEconomyService)
        bankingService = BankingService(plugin)
        vaultEconomyHook = VaultEconomyHook(plugin)
        platform.integrationRegistry.register(vaultEconomyHook)
        relationshipService = RelationshipService(plugin)
        reputationService = ReputationService(plugin)
        socialCoordinator = SocialCoordinator(plugin)
        recentEventsBuffer = RecentEventsBuffer(plugin)
        recentEventsBuffer.configure(plugin.config.getInt("events.debug_recent_event_buffer", 100))
        plugin.logger.info("[Faza 7/10] Engine-uri si servicii de domeniu initializate")
    }

    private fun phase8CommandsAndListeners() {
        val log = plugin.logger
        val command = AINPCCommand(plugin)
        val ainpcCommand = plugin.getCommand("ainpc")
        if (ainpcCommand == null) {
            throw IllegalStateException("Comanda 'ainpc' nu a fost gasita in plugin.yml")
        }
        ainpcCommand.setExecutor(command)
        ainpcCommand.setTabCompleter(AINPCTabCompleter(plugin))
        registerAliasCommand("npc", command)
        registerAliasCommand("npcquest", command)
        registerAliasCommand("quest", command)
        registerAliasCommand("progression", command)
        registerAliasCommand("contract", command)
        registerAliasCommand("duty", command)
        registerAliasCommand("bounty", command)
        registerAliasCommand("event", command)
        registerAliasCommand("tutorial", command)
        registerAliasCommand("ritual", command)

        listenerRegistry = ListenerRegistry(plugin)
        listenerRegistry.registerAll()
        log.info("[Faza 8/10] Comenzi si listenere inregistrate")
    }

    private fun phase9Starters() {
        packFileWatcher = PackFileWatcher(plugin)
        packFileWatcher.start()
        schedulerCoordinator = SchedulerCoordinator(plugin)
        schedulerCoordinator.start()
        plugin.server.servicesManager.register(
            ro.ainpc.api.AINPCPlatformApi::class.java,
            platform, plugin, org.bukkit.plugin.ServicePriority.Normal
        )
        plugin.logger.info("[Faza 9/10] Watcher si scheduler pornite")
    }

    private fun phase10Report() {
        val log = plugin.logger
        log.info("========================================")
        log.info("AI NPC Plugin v${plugin.pluginMeta.version} activat!")
        log.info("NPC-uri incarcate: ${npcManager.getNPCCount()}")
        log.info("Addonuri inregistrate: ${platform.addonRegistry.size()}")
        log.info(
            "World admin: ${platform.worldAdmin.regionCount} regiuni / " +
                "${platform.worldAdmin.placeCount} places / ${platform.worldAdmin.nodeCount} noduri"
        )
        log.info("========================================")
    }

    fun shutdown() {
        val log = plugin.logger
        if (::packFileWatcher.isInitialized) packFileWatcher.stop()
        if (::schedulerCoordinator.isInitialized) schedulerCoordinator.stop()
        if (::scenarioEngine.isInitialized) {
            scenarioEngine.stopAllQuestTracking()
            log.info("Salvare progres quest-uri...")
            scenarioEngine.flushQuestProgress()
        }
        if (::npcManager.isInitialized) {
            log.info("Salvare date NPC-uri...")
            npcManager.saveAllNPCs()
        }
        if (::economyService.isInitialized) {
            log.info("Salvare balante jucatori...")
            economyService.flush()
        }
        if (::npcEconomyService.isInitialized) {
            log.info("Salvare economie NPC...")
            npcEconomyService.flushAll()
        }
        if (::relationshipService.isInitialized) {
            log.info("Salvare relatii NPC-NPC...")
            relationshipService.flushAll()
        }
        if (::decisionEngine.isInitialized) decisionEngine.clearCache()
        if (::dialogueEngine.isInitialized) dialogueEngine.clearRecentResponses()
        snapshotProducer?.stop()
        if (::databaseManager.isInitialized) {
            log.info("Inchidere conexiune baza de date...")
            databaseManager.close()
        }
        if (::platform.isInitialized) platform.shutdown()
        if (::guiService.isInitialized) guiService.sessions().closeAll()
        plugin.server.servicesManager.unregisterAll(plugin)
        log.info("AI NPC Plugin dezactivat!")
    }

    fun reload() {
        plugin.reloadConfig()
        plugin.config.options().copyDefaults(true)
        plugin.saveConfig()
        loadQuestConfig()
        messageUtils = MessageUtils(plugin)
        if (::platform.isInitialized) platform.reloadFromConfig()
        openAIService.reloadFromConfig()
        if (::aiOrchestrationService.isInitialized) {
            aiOrchestrationService.reloadFromConfig()
        } else {
            aiOrchestrationService = AIOrchestrationService(plugin)
        }
        mcpRuntimeClient = McpRuntimeClientFactory.create(plugin)
        openAIService = OpenAIService(plugin)
        openAIService.runDiagnosticsAsync("reload")
        if (::memoryManager.isInitialized) {
            dialogueEngine = DialogueEngine(plugin, openAIService)
        }
        reloadContent()
        plugin.logger.info("Configuratie reincarcata!")
    }

    fun reloadContent() {
        if (::featurePackLoader.isInitialized) featurePackLoader.loadAllPacks()
        if (::scenarioEngine.isInitialized) scenarioEngine.reloadTemplates()
        storyStateService = StoryStateService(plugin)
        storyContextService = StoryContextService(plugin)
        if (::progressionService.isInitialized) progressionService.invalidateDefinitionCache()
        if (::npcManager.isInitialized) npcManager.ensureAllNPCsHaveProfiles()
    }

    fun loadQuestConfig() {
        val loaded = ScriptConfigurationLoader.loadQuestConfiguration(plugin)
        questConfigFile = loaded.first
        questConfig = loaded.second
        plugin.logger.info("Quest config incarcat din ${questConfigFile.name}")
    }

    private fun registerAliasCommand(name: String, command: AINPCCommand) {
        val aliasCommand = plugin.getCommand(name)
        if (aliasCommand != null) {
            aliasCommand.setExecutor(command)
            aliasCommand.setTabCompleter(AINPCTabCompleter(plugin))
        } else {
            plugin.logger.warning("Comanda '$name' nu a fost gasita in plugin.yml.")
        }
    }

    class InitializationResult private constructor(
        val success: Boolean,
        val error: Exception?
    ) {
        companion object {
            val SUCCESS = InitializationResult(true, null)
            fun failure(error: Exception) = InitializationResult(false, error)
        }
    }
}
