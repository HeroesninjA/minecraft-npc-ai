package ro.ainpc;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.PluginCommand;
import ro.ainpc.ai.DialogManager;
import ro.ainpc.ai.OpenAIService;
import ro.ainpc.api.AINPCPlatformApi;
import ro.ainpc.bootstrap.SchedulerCoordinator;
import ro.ainpc.commands.AINPCCommand;
import ro.ainpc.commands.AINPCTabCompleter;
import ro.ainpc.database.DatabaseManager;
import ro.ainpc.engine.DecisionEngine;
import ro.ainpc.engine.DialogueEngine;
import ro.ainpc.engine.FeaturePackLoader;
import ro.ainpc.engine.ScenarioEngine;
import ro.ainpc.listeners.ListenerRegistry;
import ro.ainpc.managers.ConversationSessionManager;
import ro.ainpc.managers.EmotionManager;
import ro.ainpc.managers.FamilyManager;
import ro.ainpc.managers.MemoryManager;
import ro.ainpc.managers.NPCManager;
import ro.ainpc.platform.AINPCPlatform;
import ro.ainpc.routine.RoutineService;
import ro.ainpc.spawn.NpcSpawnOrchestrator;
import ro.ainpc.story.StoryContextService;
import ro.ainpc.utils.MessageUtils;

import java.io.File;
import java.util.logging.Level;

public class AINPCPlugin extends JavaPlugin {

    private static AINPCPlugin instance;
    
    private DatabaseManager databaseManager;
    private NPCManager npcManager;
    private MemoryManager memoryManager;
    private EmotionManager emotionManager;
    private FamilyManager familyManager;
    private ConversationSessionManager conversationSessionManager;
    private DialogManager dialogManager;
    private OpenAIService openAIService;
    private RoutineService routineService;
    private NpcSpawnOrchestrator npcSpawnOrchestrator;
    private MessageUtils messageUtils;
    private AINPCPlatform platform;
    private ListenerRegistry listenerRegistry;
    private SchedulerCoordinator schedulerCoordinator;
    private File questConfigFile;
    private FileConfiguration questConfig;
    
    // Motoare AI
    private DecisionEngine decisionEngine;
    private DialogueEngine dialogueEngine;
    private ScenarioEngine scenarioEngine;
    private FeaturePackLoader featurePackLoader;
    private StoryContextService storyContextService;

    @Override
    public void onEnable() {
        instance = this;
        
        // Salveaza configuratia default
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        loadQuestConfig();
        
        // Initializeaza utilitarele
        messageUtils = new MessageUtils(this);

        // Initializeaza platforma core
        platform = new AINPCPlatform(this);
        platform.initialize();
        
        // Initializeaza baza de date
        getLogger().info("Initializare baza de date...");
        databaseManager = new DatabaseManager(this);
        if (!databaseManager.initialize()) {
            getLogger().severe("Nu s-a putut initializa baza de date! Pluginul se opreste.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Initializeaza serviciul OpenAI
        getLogger().info("Initializare serviciu OpenAI...");
        openAIService = new OpenAIService(this);
        openAIService.runDiagnosticsAsync("startup");
        
        // Incarca Feature Packs
        getLogger().info("Incarcare Feature Packs...");
        featurePackLoader = new FeaturePackLoader(this);
        featurePackLoader.loadAllPacks();
        
        // Initializeaza managerii
        getLogger().info("Initializare manageri...");
        memoryManager = new MemoryManager(this);
        emotionManager = new EmotionManager(this);
        familyManager = new FamilyManager(this);
        npcManager = new NPCManager(this);
        routineService = new RoutineService(this);
        npcSpawnOrchestrator = new NpcSpawnOrchestrator(this);
        dialogManager = new DialogManager(this);
        conversationSessionManager = new ConversationSessionManager(this);
        
        // Incarca NPC-urile din baza de date
        npcManager.loadAllNPCs();
        npcManager.discoverExistingVillagers();
        int backfilledProfiles = npcManager.ensureAllNPCsHaveProfiles();
        getLogger().info("Profiluri NPC verificate. Profiluri create/backfill: " + backfilledProfiles);
        
        // Initializeaza motoarele AI
        getLogger().info("Initializare motoare AI...");
        decisionEngine = new DecisionEngine(this);
        dialogueEngine = new DialogueEngine(this, openAIService);
        scenarioEngine = new ScenarioEngine(this);
        storyContextService = new StoryContextService(this);
        
        // Inregistreaza comenzile
        getLogger().info("Inregistrare comenzi...");
        AINPCCommand command = new AINPCCommand(this);
        PluginCommand ainpcCommand = getCommand("ainpc");
        if (ainpcCommand == null) {
            getLogger().severe("Comanda 'ainpc' nu a fost gasita in plugin.yml. Pluginul se opreste.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        ainpcCommand.setExecutor(command);
        ainpcCommand.setTabCompleter(new AINPCTabCompleter(this));

        PluginCommand npcQuestCommand = getCommand("npcquest");
        if (npcQuestCommand != null) {
            npcQuestCommand.setExecutor(command);
            npcQuestCommand.setTabCompleter(new AINPCTabCompleter(this));
        } else {
            getLogger().warning("Comanda 'npcquest' nu a fost gasita in plugin.yml.");
        }

        PluginCommand questCommand = getCommand("quest");
        if (questCommand != null) {
            questCommand.setExecutor(command);
            questCommand.setTabCompleter(new AINPCTabCompleter(this));
        } else {
            getLogger().warning("Comanda 'quest' nu a fost gasita in plugin.yml.");
        }
        
        // Inregistreaza listenerele
        getLogger().info("Inregistrare listenere...");
        listenerRegistry = new ListenerRegistry(this);
        listenerRegistry.registerAll();

        schedulerCoordinator = new SchedulerCoordinator(this);
        schedulerCoordinator.start();
        getServer().getServicesManager().register(AINPCPlatformApi.class, platform, this, ServicePriority.Normal);
        
        getLogger().info("========================================");
        getLogger().info("AI NPC Plugin v" + getPluginMeta().getVersion() + " activat!");
        getLogger().info("NPC-uri incarcate: " + npcManager.getNPCCount());
        getLogger().info("Addonuri inregistrate: " + platform.getAddonRegistry().size());
        getLogger().info("World admin: " + platform.getWorldAdmin().getRegionCount() + " regiuni / "
            + platform.getWorldAdmin().getPlaceCount() + " places / "
            + platform.getWorldAdmin().getNodeCount() + " noduri");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        if (scenarioEngine != null) {
            scenarioEngine.stopAllQuestTracking();
            getLogger().info("Salvare progres quest-uri...");
            scenarioEngine.flushQuestProgress();
        }

        // Salveaza toate datele NPC-urilor
        if (npcManager != null) {
            getLogger().info("Salvare date NPC-uri...");
            npcManager.saveAllNPCs();
        }
        
        // Inchide conexiunea la baza de date
        if (databaseManager != null) {
            getLogger().info("Inchidere conexiune baza de date...");
            databaseManager.close();
        }

        if (platform != null) {
            platform.shutdown();
        }
        getServer().getServicesManager().unregisterAll(this);
        
        getLogger().info("AI NPC Plugin dezactivat!");
    }
    
    public void reload() {
        reloadConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        loadQuestConfig();
        messageUtils = new MessageUtils(this);
        if (platform != null) {
            platform.reloadFromConfig();
        }
        openAIService = new OpenAIService(this);
        openAIService.runDiagnosticsAsync("reload");
        if (memoryManager != null) {
            dialogueEngine = new DialogueEngine(this, openAIService);
        }
        reloadContent();
        getLogger().info("Configuratie reincarcata!");
    }

    public void reloadContent() {
        if (featurePackLoader != null) {
            featurePackLoader.loadAllPacks();
        }
        if (scenarioEngine != null) {
            scenarioEngine.reloadTemplates();
        }
        storyContextService = new StoryContextService(this);
        if (npcManager != null) {
            npcManager.ensureAllNPCsHaveProfiles();
        }
    }

    private void loadQuestConfig() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        questConfigFile = new File(getDataFolder(), "quests.yml");
        if (!questConfigFile.exists()) {
            saveResource("quests.yml", false);
        }

        questConfig = YamlConfiguration.loadConfiguration(questConfigFile);
    }

    // Getters
    public static AINPCPlugin getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public NPCManager getNpcManager() {
        return npcManager;
    }

    public MemoryManager getMemoryManager() {
        return memoryManager;
    }

    public EmotionManager getEmotionManager() {
        return emotionManager;
    }

    public FamilyManager getFamilyManager() {
        return familyManager;
    }

    public ConversationSessionManager getConversationSessionManager() {
        return conversationSessionManager;
    }

    public AINPCPlatform getPlatform() {
        return platform;
    }

    public NpcSpawnOrchestrator getNpcSpawnOrchestrator() {
        return npcSpawnOrchestrator;
    }

    public RoutineService getRoutineService() {
        return routineService;
    }

    public DialogManager getDialogManager() {
        return dialogManager;
    }

    public OpenAIService getOpenAIService() {
        return openAIService;
    }

    public MessageUtils getMessageUtils() {
        return messageUtils;
    }

    public DecisionEngine getDecisionEngine() {
        return decisionEngine;
    }

    public DialogueEngine getDialogueEngine() {
        return dialogueEngine;
    }

    public ScenarioEngine getScenarioEngine() {
        return scenarioEngine;
    }

    public StoryContextService getStoryContextService() {
        return storyContextService;
    }

    public FeaturePackLoader getFeaturePackLoader() {
        return featurePackLoader;
    }

    public FileConfiguration getQuestConfig() {
        return questConfig;
    }
    
    public void debug(String message) {
        if (getConfig().getBoolean("debug")) {
            getLogger().log(Level.INFO, "[Debug] " + message);
        }
    }
}
