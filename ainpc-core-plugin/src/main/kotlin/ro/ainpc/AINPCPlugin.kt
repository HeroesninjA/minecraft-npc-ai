package ro.ainpc

import org.bukkit.command.PluginCommand
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.ServicePriority
import org.bukkit.plugin.java.JavaPlugin
import ro.ainpc.ai.DialogManager
import ro.ainpc.ai.OpenAIService
import ro.ainpc.ai.orchestration.AIOrchestrationService
import ro.ainpc.api.AINPCPlatformApi
import ro.ainpc.bootstrap.SchedulerCoordinator
import ro.ainpc.commands.AINPCCommand
import ro.ainpc.commands.AINPCTabCompleter
import ro.ainpc.database.DatabaseManager
import ro.ainpc.engine.DecisionEngine
import ro.ainpc.engine.DialogueEngine
import ro.ainpc.engine.FeaturePackLoader
import ro.ainpc.engine.QuestAuthoringService
import ro.ainpc.engine.ScenarioEngine
import ro.ainpc.engine.ScriptConfigurationLoader
import ro.ainpc.gui.GuiService
import ro.ainpc.listeners.ListenerRegistry
import ro.ainpc.managers.ConversationSessionManager
import ro.ainpc.managers.EmotionManager
import ro.ainpc.managers.FamilyManager
import ro.ainpc.managers.MemoryManager
import ro.ainpc.managers.NPCManager
import ro.ainpc.mcp.McpRuntimeClient
import ro.ainpc.mcp.McpRuntimeClientFactory
import ro.ainpc.platform.AINPCPlatform
import ro.ainpc.progression.ProgressionService
import ro.ainpc.routine.RoutineService
import ro.ainpc.debug.RecentEventsBuffer
import ro.ainpc.spawn.HouseholdPersistenceService
import ro.ainpc.spawn.NpcSpawnOrchestrator
import ro.ainpc.story.StoryContextService
import ro.ainpc.story.StoryStateService
import ro.ainpc.utils.MessageUtils
import ro.ainpc.world.NpcWorldBindingService
import ro.ainpc.economy.EconomyService
import ro.ainpc.economy.ShopService
import ro.ainpc.world.mapping.MappingWandService
import java.io.File
import java.util.logging.Level

class AINPCPlugin : JavaPlugin() {
    lateinit var databaseManager: DatabaseManager
        private set
    lateinit var npcManager: NPCManager
        private set
    lateinit var memoryManager: MemoryManager
        private set
    lateinit var emotionManager: EmotionManager
        private set
    lateinit var familyManager: FamilyManager
        private set
    lateinit var conversationSessionManager: ConversationSessionManager
        private set
    lateinit var dialogManager: DialogManager
        private set
    lateinit var openAIService: OpenAIService
        private set
    lateinit var aiOrchestrationService: AIOrchestrationService
        private set
    lateinit var mcpRuntimeClient: McpRuntimeClient
        private set
    lateinit var routineService: RoutineService
        private set
    lateinit var npcSpawnOrchestrator: NpcSpawnOrchestrator
        private set
    lateinit var householdPersistenceService: HouseholdPersistenceService
        private set
    lateinit var npcWorldBindingService: NpcWorldBindingService
        private set
    lateinit var messageUtils: MessageUtils
        private set
    lateinit var platform: AINPCPlatform
        private set
    lateinit var listenerRegistry: ListenerRegistry
        private set
    lateinit var schedulerCoordinator: SchedulerCoordinator
        private set
    lateinit var questConfigFile: File
        private set
    lateinit var questConfig: FileConfiguration
        private set

    lateinit var decisionEngine: DecisionEngine
        private set
    lateinit var dialogueEngine: DialogueEngine
        private set
    lateinit var scenarioEngine: ScenarioEngine
        private set
    lateinit var featurePackLoader: FeaturePackLoader
        private set
    lateinit var progressionService: ProgressionService
        private set
    lateinit var storyContextService: StoryContextService
        private set
    lateinit var storyStateService: StoryStateService
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
    lateinit var reputationService: ro.ainpc.reputation.ReputationService
    lateinit var socialCoordinator: ro.ainpc.routine.SocialCoordinator
    lateinit var recentEventsBuffer: RecentEventsBuffer

    override fun onEnable() {
        instance = this
        saveDefaultConfig()
        config.options().copyDefaults(true)
        saveConfig()
        loadQuestConfig()
        validateConfig()
        for (res in listOf("castel-world-admin.yml", "settlements.yml", "building_templates.yml", "behavior_profiles.yml")) {
            try {
                saveResource(res, false)
            } catch (ignored: java.io.IOException) {
                logger.fine("$res deja exista sau nu este disponibil.")
            }
        }

        messageUtils = MessageUtils(this)
        platform = AINPCPlatform(this)
        platform.initialize()

        logger.info("Initializare baza de date...")
        databaseManager = DatabaseManager(this)
        if (!databaseManager.initialize()) {
            logger.severe("Nu s-a putut initializa baza de date! Pluginul se opreste.")
            server.pluginManager.disablePlugin(this)
            return
        }
        npcWorldBindingService = NpcWorldBindingService(this)
        householdPersistenceService = HouseholdPersistenceService(this)

        logger.info("Initializare serviciu OpenAI...")
        openAIService = OpenAIService(this)
        mcpRuntimeClient = McpRuntimeClientFactory.create(this)
        aiOrchestrationService = AIOrchestrationService(this)
        openAIService.runDiagnosticsAsync("startup")

        logger.info("Incarcare Feature Packs...")
        featurePackLoader = FeaturePackLoader(this)
        featurePackLoader.loadAllPacks()

        logger.info("Initializare manageri...")
        memoryManager = MemoryManager(this)
        emotionManager = EmotionManager(this)
        familyManager = FamilyManager(this)
        npcManager = NPCManager(this)
        routineService = RoutineService(this)
        npcSpawnOrchestrator = NpcSpawnOrchestrator(this)
        dialogManager = DialogManager(this)
        conversationSessionManager = ConversationSessionManager(this)

        npcManager.loadAllNPCs()
        npcManager.discoverExistingVillagers()
        npcManager.reconcileDuplicateLiveNPCEntities("startup")
        npcManager.restoreMissingNPCsInLoadedChunks()
        npcManager.enforceControlledEntitySettings("startup")
        val backfilledProfiles = npcManager.ensureAllNPCsHaveProfiles()
        val backfilledWorldBindings = npcManager.backfillWorldBindingsFromAnchors()
        logger.info("Profiluri NPC verificate. Profiluri create/backfill: $backfilledProfiles")
        logger.info("Binding-uri NPC-world inferate/backfill: $backfilledWorldBindings")

        logger.info("Initializare motoare AI...")
        decisionEngine = DecisionEngine(this)
        dialogueEngine = DialogueEngine(this, openAIService)
        scenarioEngine = ScenarioEngine(this)
        progressionService = ProgressionService(this)
        storyStateService = StoryStateService(this)
        storyContextService = StoryContextService(this)
        authoringService = QuestAuthoringService()
        guiService = GuiService(this)
        mappingWandService = MappingWandService(this)
        economyService = EconomyService(this)
        shopService = ShopService(economyService)
        reputationService = ro.ainpc.reputation.ReputationService(this)
        socialCoordinator = ro.ainpc.routine.SocialCoordinator(this)

        logger.info("Inregistrare comenzi...")
        val command = AINPCCommand(this)
        val ainpcCommand = getCommand("ainpc")
        if (ainpcCommand == null) {
            logger.severe("Comanda 'ainpc' nu a fost gasita in plugin.yml. Pluginul se opreste.")
            server.pluginManager.disablePlugin(this)
            return
        }
        ainpcCommand.setExecutor(command)
        ainpcCommand.setTabCompleter(AINPCTabCompleter(this))
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

        logger.info("Inregistrare listenere...")
        listenerRegistry = ListenerRegistry(this)
        listenerRegistry.registerAll()

        schedulerCoordinator = SchedulerCoordinator(this)
        schedulerCoordinator.start()
        server.servicesManager.register(AINPCPlatformApi::class.java, platform, this, ServicePriority.Normal)

        logger.info("========================================")
        logger.info("AI NPC Plugin v${pluginMeta.version} activat!")
        logger.info("NPC-uri incarcate: ${npcManager.getNPCCount()}")
        logger.info("Addonuri inregistrate: ${platform.addonRegistry.size()}")
        logger.info(
            "World admin: ${platform.worldAdmin.regionCount} regiuni / " +
                "${platform.worldAdmin.placeCount} places / ${platform.worldAdmin.nodeCount} noduri"
        )
        logger.info("========================================")
    }

    override fun onDisable() {
        if (::scenarioEngine.isInitialized) {
            scenarioEngine.stopAllQuestTracking()
            logger.info("Salvare progres quest-uri...")
            scenarioEngine.flushQuestProgress()
        }
        if (::npcManager.isInitialized) {
            logger.info("Salvare date NPC-uri...")
            npcManager.saveAllNPCs()
        }
        if (::databaseManager.isInitialized) {
            logger.info("Inchidere conexiune baza de date...")
            databaseManager.close()
        }
        if (::platform.isInitialized) platform.shutdown()
        if (::guiService.isInitialized) guiService.sessions().closeAll()
        server.servicesManager.unregisterAll(this)
        logger.info("AI NPC Plugin dezactivat!")
    }

    fun reload() {
        reloadConfig()
        config.options().copyDefaults(true)
        saveConfig()
        loadQuestConfig()
        messageUtils = MessageUtils(this)
        if (::platform.isInitialized) {
            platform.reloadFromConfig()
        }
        if (::openAIService.isInitialized) {
            openAIService.reloadFromConfig()
        } else {
            openAIService = OpenAIService(this)
        }
        if (::aiOrchestrationService.isInitialized) {
            aiOrchestrationService.reloadFromConfig()
        } else {
            aiOrchestrationService = AIOrchestrationService(this)
        }
        mcpRuntimeClient = McpRuntimeClientFactory.create(this)
        openAIService.runDiagnosticsAsync("reload")
        if (::memoryManager.isInitialized) {
            dialogueEngine = DialogueEngine(this, openAIService)
        }
        reloadContent()
        logger.info("Configuratie reincarcata!")
    }

    fun reloadContent() {
        if (::featurePackLoader.isInitialized) featurePackLoader.loadAllPacks()
        if (::scenarioEngine.isInitialized) scenarioEngine.reloadTemplates()
        storyStateService = StoryStateService(this)
        storyContextService = StoryContextService(this)
        if (::progressionService.isInitialized) progressionService.invalidateDefinitionCache()
        if (::npcManager.isInitialized) npcManager.ensureAllNPCsHaveProfiles()
    }

    private fun loadQuestConfig() {
        val loaded = ScriptConfigurationLoader.loadQuestConfiguration(this)
        questConfigFile = loaded.first
        questConfig = loaded.second
        logger.info("Quest config incarcat din ${questConfigFile.name}")
    }

    private fun registerAliasCommand(name: String, command: AINPCCommand) {
        val aliasCommand: PluginCommand? = getCommand(name)
        if (aliasCommand != null) {
            aliasCommand.setExecutor(command)
            aliasCommand.setTabCompleter(AINPCTabCompleter(this))
        } else {
            logger.warning("Comanda '$name' nu a fost gasita in plugin.yml.")
        }
    }

    fun debug(message: String) {
        if (config.getBoolean("debug.enabled", false)) {
            logger.log(Level.INFO, "[Debug] $message")
        }
    }

    private fun validateConfig() {
        val warnings = mutableListOf<String>()

        if (!config.contains("features.ai")) {
            warnings.add("config.yml: 'features.ai' nu este definit; se va folosi implicit false.")
        }
        if (!config.contains("openai.api_key") && config.getBoolean("features.ai", false)) {
            warnings.add("config.yml: 'openai.api_key' nu este definit, dar features.ai=true. AI-ul nu va functiona.")
            warnings.add("  >> Actiune: seteaza OPENAI_API_KEY in variabilele de mediu sau openai.api_key in config.yml.")
        }
        if (!config.contains("database.dialect")) {
            warnings.add("config.yml: 'database.dialect' nu este definit; se va folosi 'sqlite'.")
        }
        val aiEnabled = config.getBoolean("features.ai", false)
        val routineEnabled = config.getBoolean("routine.enabled", false)
        if (routineEnabled && !aiEnabled) {
            warnings.add("config.yml: routine.enabled=true dar features.ai=false. NPC-urile nu vor avea dialog AI.")
            warnings.add("  >> Actiune: seteaza features.ai=true sau dezactiveaza routine.enabled.")
        }

        if (warnings.isNotEmpty()) {
            logger.warning("=== Validare config.yml ===")
            for (w in warnings) {
                logger.warning("  ! $w")
            }
        }
    }

    companion object {
        private lateinit var instance: AINPCPlugin

        @JvmStatic
        fun getInstance(): AINPCPlugin = instance
    }
}
