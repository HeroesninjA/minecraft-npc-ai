package ro.ainpc

import org.bukkit.command.PluginCommand
import org.bukkit.plugin.java.JavaPlugin
import ro.ainpc.ai.DialogManager
import ro.ainpc.ai.OllamaService
import ro.ainpc.util.PermissionAudit
import ro.ainpc.ai.OpenAIService
import ro.ainpc.ai.RelationshipService
import ro.ainpc.ai.orchestration.AIOrchestrationService
import ro.ainpc.api.AINPCPlatformApi
import ro.ainpc.bootstrap.PackFileWatcher
import ro.ainpc.bootstrap.PerformanceMonitor
import ro.ainpc.bootstrap.SchedulerCoordinator
import ro.ainpc.bootstrap.ServiceRegistry
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
import ro.ainpc.world.scan.VanillaVillageScanService
import java.io.File
import java.util.logging.Level

class AINPCPlugin : JavaPlugin() {
    val services = ServiceRegistry(this)

    val databaseManager: DatabaseManager get() = services.databaseManager
    val npcManager: NPCManager get() = services.npcManager
    val memoryManager: MemoryManager get() = services.memoryManager
    val emotionManager: EmotionManager get() = services.emotionManager
    val familyManager: FamilyManager get() = services.familyManager
    val conversationSessionManager: ConversationSessionManager get() = services.conversationSessionManager
    val dialogManager: DialogManager get() = services.dialogManager
    val openAIService: OpenAIService get() = services.openAIService
    val ollamaService: OllamaService get() = services.ollamaService
    val aiOrchestrationService: AIOrchestrationService get() = services.aiOrchestrationService
    val mcpRuntimeClient: McpRuntimeClient get() = services.mcpRuntimeClient
    val snapshotProducer: RuntimeSnapshotProducer? get() = services.snapshotProducer
    val mcpCommandQueue: McpCommandQueue get() = services.mcpCommandQueue
    val performanceMonitor: PerformanceMonitor get() = services.performanceMonitor
    val routineService: RoutineService get() = services.routineService
    val routineCoordinator: RoutineCoordinator get() = services.routineCoordinator
    val autoSettlementGenerator: AutoSettlementGenerator get() = services.autoSettlementGenerator
    val vanillaVillageScanService: VanillaVillageScanService get() = services.vanillaVillageScanService
    val npcSpawnOrchestrator: NpcSpawnOrchestrator get() = services.npcSpawnOrchestrator
    val householdPersistenceService: HouseholdPersistenceService get() = services.householdPersistenceService
    val npcWorldBindingService: NpcWorldBindingService get() = services.npcWorldBindingService
    val messageUtils: MessageUtils get() = services.messageUtils
    val platform: AINPCPlatform get() = services.platform
    val listenerRegistry: ListenerRegistry get() = services.listenerRegistry
    val packFileWatcher: PackFileWatcher get() = services.packFileWatcher
    val schedulerCoordinator: SchedulerCoordinator get() = services.schedulerCoordinator
    val questConfigFile: File get() = services.questConfigFile
    val questConfig: org.bukkit.configuration.file.FileConfiguration get() = services.questConfig
    val decisionEngine: DecisionEngine get() = services.decisionEngine
    val dialogueEngine: DialogueEngine get() = services.dialogueEngine
    val scenarioEngine: ScenarioEngine get() = services.scenarioEngine
    val featurePackLoader: FeaturePackLoader get() = services.featurePackLoader
    val progressionService: ProgressionService get() = services.progressionService
    val playerProgressionService: PlayerProgressionService get() = services.playerProgressionService
    val storyContextService: StoryContextService get() = services.storyContextService
    val storyAuthoringService: StoryAuthoringService get() = services.storyAuthoringService
    val storyReactionService: StoryReactionService get() = services.storyReactionService
    val randomWorldEventService: RandomWorldEventService get() = services.randomWorldEventService
    val storyStateService: StoryStateService get() = services.storyStateService
    val environmentEngine: EnvironmentEngine get() = services.environmentEngine
    val seasonalBehaviorService: SeasonalBehaviorService get() = services.seasonalBehaviorService
    val authoringService: QuestAuthoringService get() = services.authoringService
    val guiService: GuiService get() = services.guiService
    val mappingWandService: MappingWandService get() = services.mappingWandService
    val economyService: EconomyService get() = services.economyService
    val shopService: ShopService get() = services.shopService
    val npcEconomyService: NpcEconomyService get() = services.npcEconomyService
    val bankingService: BankingService get() = services.bankingService
    val vaultEconomyHook: VaultEconomyHook get() = services.vaultEconomyHook
    val relationshipService: RelationshipService get() = services.relationshipService
    val reputationService: ReputationService get() = services.reputationService
    val socialCoordinator: SocialCoordinator get() = services.socialCoordinator
    val recentEventsBuffer: RecentEventsBuffer get() = services.recentEventsBuffer

    override fun onEnable() {
        instance = this
        saveDefaultConfig()
        config.options().copyDefaults(true)
        saveConfig()
        services.loadQuestConfig()
        validateConfig()
        saveResourceIfAbsent("castel-world-admin.yml")
        saveResourceIfAbsent("settlements.yml")
        saveResourceIfAbsent("building_templates.yml")
        saveResourceIfAbsent("behavior_profiles.yml")
        val packsDir = File(dataFolder, "packs")
        if (!packsDir.exists()) {
            packsDir.mkdirs()
        }
        saveResourceIfAbsent("packs/compat-26-1-2.yml")

        val result = services.initialize()
        if (!result.success) {
            val error = result.error
            logger.severe("Initializarea pluginului a esuat: ${error?.message}")
            server.pluginManager.disablePlugin(this)
        }
    }

    override fun onDisable() {
        services.shutdown()
    }

    fun reload() {
        reloadConfig()
        config.options().copyDefaults(true)
        saveConfig()
        services.reload()
    }

    fun reloadContent() {
        services.reloadContent()
    }

    fun debug(message: String) {
        if (config.getBoolean("debug.enabled", false)) {
            logger.log(Level.INFO, "[Debug] $message")
        }
    }

    private fun saveResourceIfAbsent(path: String) {
        try {
            saveResource(path, false)
        } catch (ignored: Exception) {
            logger.fine("$path deja exista sau nu este disponibil.")
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
        PermissionAudit(this).runStartupAudit()
        PermissionAudit.auditRegisteredCommands(this)
    }

    companion object {
        private lateinit var instance: AINPCPlugin

        @JvmStatic
        fun getInstance(): AINPCPlugin = instance
    }
}
