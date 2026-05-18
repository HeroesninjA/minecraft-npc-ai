package ro.ainpc

import org.bukkit.command.PluginCommand
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
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
import ro.ainpc.engine.ScenarioEngine
import ro.ainpc.gui.GuiService
import ro.ainpc.listeners.ListenerRegistry
import ro.ainpc.managers.ConversationSessionManager
import ro.ainpc.managers.EmotionManager
import ro.ainpc.managers.FamilyManager
import ro.ainpc.managers.MemoryManager
import ro.ainpc.managers.NPCManager
import ro.ainpc.platform.AINPCPlatform
import ro.ainpc.progression.ProgressionService
import ro.ainpc.routine.RoutineService
import ro.ainpc.spawn.HouseholdPersistenceService
import ro.ainpc.spawn.NpcSpawnOrchestrator
import ro.ainpc.story.StoryContextService
import ro.ainpc.story.StoryStateService
import ro.ainpc.utils.MessageUtils
import ro.ainpc.world.NpcWorldBindingService
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
    lateinit var guiService: GuiService
        private set
    lateinit var mappingWandService: MappingWandService
        private set

    override fun onEnable() {
        instance = this
        saveDefaultConfig()
        config.options().copyDefaults(true)
        saveConfig()
        loadQuestConfig()

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
        guiService = GuiService(this)
        mappingWandService = MappingWandService(this)

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
        openAIService = OpenAIService(this)
        aiOrchestrationService = AIOrchestrationService(this)
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
        if (::npcManager.isInitialized) npcManager.ensureAllNPCsHaveProfiles()
    }

    private fun loadQuestConfig() {
        if (!dataFolder.exists()) dataFolder.mkdirs()
        questConfigFile = File(dataFolder, "quests.yml")
        if (!questConfigFile.exists()) saveResource("quests.yml", false)
        questConfig = YamlConfiguration.loadConfiguration(questConfigFile)
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
        if (config.getBoolean("debug")) {
            logger.log(Level.INFO, "[Debug] $message")
        }
    }

    companion object {
        private lateinit var instance: AINPCPlugin

        @JvmStatic
        fun getInstance(): AINPCPlugin = instance
    }
}
