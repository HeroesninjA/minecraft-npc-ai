package ro.ainpc.engine;

import static ro.ainpc.engine.QuestTrackingModelsKt.center;
import static ro.ainpc.engine.QuestTrackingModelsKt.formatHorizontalDirection;
import static ro.ainpc.engine.QuestTrackingModelsKt.formatQuestAnchorType;
import static ro.ainpc.engine.QuestTrackingModelsKt.formatQuestPhase;
import static ro.ainpc.engine.QuestTrackingModelsKt.formatQuestTrackingCoordinates;
import static ro.ainpc.engine.QuestTrackingModelsKt.formatVerticalHint;
import static ro.ainpc.engine.QuestTrackingModelsKt.normalizeTrackingAnchorType;
import static ro.ainpc.engine.ScenarioEngineTextKt.capitalizeProgressionLabel;
import static ro.ainpc.engine.ScenarioEngineTextKt.formatDuration;
import static ro.ainpc.engine.ScenarioEngineTextKt.formatObjectiveProgressLabel;
import static ro.ainpc.engine.ScenarioEngineTextKt.formatOptional;
import static ro.ainpc.engine.ScenarioEngineTextKt.formatQuestDebugMap;
import static ro.ainpc.engine.ScenarioEngineTextKt.formatQuestDebugTime;
import static ro.ainpc.engine.ScenarioEngineTextKt.formatQuestLogMechanicCounts;
import static ro.ainpc.engine.ScenarioEngineTextKt.formatQuestEntry;
import static ro.ainpc.engine.ScenarioEngineTextKt.formatStageCompletionMode;
import static ro.ainpc.engine.ScenarioEngineTextKt.formatQuestStatus;
import static ro.ainpc.engine.ScenarioEngineTextKt.resolveQuestTitle;
import static ro.ainpc.engine.ScenarioEngineTextKt.valueOrFallback;
import static ro.ainpc.engine.ScenarioObjectiveProgressKt.buildObjectiveKey;
import static ro.ainpc.engine.ScenarioObjectiveProgressKt.carryLegacyObjectiveProgress;
import static ro.ainpc.engine.ScenarioObjectiveProgressKt.hasInventoryObjective;
import static ro.ainpc.engine.ScenarioObjectiveProgressKt.hasObjectiveType;
import static ro.ainpc.engine.ScenarioObjectiveProgressKt.incrementObjectiveProgress;
import static ro.ainpc.engine.ScenarioObjectiveProgressKt.matchesObjectiveReference;
import static ro.ainpc.engine.ScenarioObjectiveProgressKt.matchesObjectiveType;
import static ro.ainpc.engine.ScenarioObjectiveProgressKt.normalizeObjectiveType;
import static ro.ainpc.engine.ScenarioObjectiveProgressKt.objectiveKeyCandidates;
import static ro.ainpc.engine.ScenarioObjectiveProgressKt.readObjectiveProgress;
import static ro.ainpc.engine.ScenarioObjectiveProgressKt.resolveQuestObjectiveState;
import static ro.ainpc.engine.ScenarioObjectiveProgressKt.shouldConsumeObjectiveItem;
import static ro.ainpc.engine.ScenarioObjectiveProgressKt.shouldShowObjectiveForCurrentStage;
import static ro.ainpc.engine.ScenarioObjectiveProgressKt.usesInventoryProgress;
import static ro.ainpc.engine.ScenarioObjectiveProgressKt.buildCompletedObjectiveProgress;
import static ro.ainpc.engine.ScenarioObjectiveProgressKt.buildObjectiveProgressSnapshot;
import static ro.ainpc.engine.ScenarioObjectiveProgressKt.countMaterial;
import static ro.ainpc.engine.ScenarioObjectiveProgressKt.removeMaterial;
import static ro.ainpc.engine.ScenarioObjectiveProgressKt.cloneStorageContents;
import static ro.ainpc.engine.ScenarioObjectiveProgressKt.grantQuestRewards;
import static ro.ainpc.engine.ScenarioObjectiveProgressKt.hasBoundAnchor;
import static ro.ainpc.engine.ScenarioObjectiveProgressKt.consumeQuestObjectives;
import static ro.ainpc.engine.ScenarioObjectiveProgressKt.matchesStoredQuestNpc;
import static ro.ainpc.engine.ScenarioObjectiveProgressKt.inspectQuestInventory;
import static ro.ainpc.engine.ScenarioObjectiveProgressKt.matchesBoundAnchor;
import static ro.ainpc.engine.ScenarioObjectiveProgressKt.inspectQuestObjectives;
import static ro.ainpc.engine.ScenarioObjectiveProgressKt.inspectQuestRewardDelivery;
import static ro.ainpc.engine.ScenarioObjectiveProgressKt.resolveObjectiveCurrentProgress;
import static ro.ainpc.engine.ScenarioObjectiveProgressKt.simulateQuestObjectiveConsumption;
import static ro.ainpc.engine.ScenarioObjectiveProgressKt.simulateRemoveMaterial;
import static ro.ainpc.engine.ScenarioObjectiveProgressKt.simulateAddMaterial;
import static ro.ainpc.engine.QuestLogFilterKt.parseQuestLogFilter;
import static ro.ainpc.engine.QuestLogFilterKt.questLogActionSelector;
import static ro.ainpc.engine.QuestLogFilterKt.questLogStatusPriority;
import static ro.ainpc.engine.ScenarioQuestReferencesKt.isTrackedQuestSelector;
import static ro.ainpc.engine.ScenarioQuestReferencesKt.matchesQuestReference;
import static ro.ainpc.engine.ScenarioQuestReferencesKt.progressionReference;
import static ro.ainpc.engine.ScenarioQuestPhaseKt.areObjectivesSatisfied;
import static ro.ainpc.engine.ScenarioQuestPhaseKt.areObjectivesSatisfiedForStage;
import static ro.ainpc.engine.ScenarioQuestPhaseKt.canonicalQuestPhase;
import static ro.ainpc.engine.ScenarioQuestPhaseKt.findMatchingObjectiveStage;
import static ro.ainpc.engine.ScenarioQuestPhaseKt.getDefaultActiveQuestPhase;
import static ro.ainpc.engine.ScenarioQuestPhaseKt.getFirstQuestPhase;
import static ro.ainpc.engine.ScenarioQuestPhaseKt.getLastQuestPhase;
import static ro.ainpc.engine.ScenarioQuestPhaseKt.getObjectiveStage;
import static ro.ainpc.engine.ScenarioQuestPhaseKt.getQuestWorkPhase;
import static ro.ainpc.engine.ScenarioQuestPhaseKt.getReadyToTurnInQuestPhase;
import static ro.ainpc.engine.ScenarioQuestPhaseKt.hasStagedObjectives;
import static ro.ainpc.engine.ScenarioQuestPhaseKt.isObjectiveActiveForPhase;
import static ro.ainpc.engine.ScenarioQuestPhaseKt.isObjectiveActiveForProgress;
import static ro.ainpc.engine.ScenarioQuestPhaseKt.resolveQuestPhase;
import static ro.ainpc.engine.ScenarioRoleScoringKt.normalizeScenarioToken;
import static ro.ainpc.engine.ScenarioRoleScoringKt.scoreBoolean;
import static ro.ainpc.engine.ScenarioStageProgressKt.normalizeStageCompletionMode;
import static ro.ainpc.engine.ScenarioStageProgressKt.objectiveListedInAnyStage;
import static ro.ainpc.engine.ScenarioStageProgressKt.phasesMatch;
import static ro.ainpc.engine.ScenarioStageProgressKt.stageReferencesObjective;
import static ro.ainpc.engine.ScenarioStoryTextKt.cleanStoryId;
import static ro.ainpc.engine.ScenarioStoryTextKt.detectStoryTargetScope;
import static ro.ainpc.engine.ScenarioStoryTextKt.firstNonBlank;
import static ro.ainpc.engine.ScenarioStoryTextKt.getQuestEntryMetadata;
import static ro.ainpc.engine.ScenarioStoryTextKt.isQuestStoryAction;
import static ro.ainpc.engine.ScenarioStoryTextKt.normalizeReference;
import static ro.ainpc.engine.ScenarioStoryTextKt.normalizeStoryActionType;
import static ro.ainpc.engine.ScenarioStoryTextKt.normalizeStoryScope;
import static ro.ainpc.engine.ScenarioStoryTextKt.parseStoryList;
import static ro.ainpc.engine.ScenarioStoryTextKt.stripObjectivePrefix;

import static ro.ainpc.engine.ScenarioProgressionKt.resolveProgressionMechanicDefinition;
import static ro.ainpc.engine.ScenarioProgressionKt.resolveProgressionMechanicKey;
import static ro.ainpc.engine.ScenarioProgressionKt.resolveProgressionMechanicDisplay;
import static ro.ainpc.engine.ScenarioProgressionKt.resolveProgressionPluralLabel;
import static ro.ainpc.engine.ScenarioProgressionKt.resolveProgressionSingularLabel;
import static ro.ainpc.engine.ScenarioProgressionKt.progressionKindMatches;
import static ro.ainpc.engine.ScenarioProgressionKt.resolveProgressionMechanicSortKey;
import static ro.ainpc.engine.ScenarioQuestCategoryKt.resolveQuestCategory;
import static ro.ainpc.engine.ScenarioQuestCategoryKt.questLogCategoryPriority;
import static ro.ainpc.engine.ScenarioNpcMatcherKt.matchesQuestGiver;
import static ro.ainpc.engine.ScenarioNpcMatcherKt.matchesNpcObjective;
import static ro.ainpc.engine.ScenarioNpcMatcherKt.matchesProfessionReference;
import static ro.ainpc.engine.ScenarioQuestOfferKt.buildInitialQuestNpcFallbackMessages;
import static ro.ainpc.engine.ScenarioQuestOfferKt.buildQuestOfferMessage;
import static ro.ainpc.engine.ScenarioQuestOfferKt.resolveInitialQuestDialogueContext;
import static ro.ainpc.engine.ScenarioQuestOfferKt.shouldAutoAcceptOnOffer;
import static ro.ainpc.engine.ScenarioObjectiveMatcherKt.matchesMobObjective;
import static ro.ainpc.engine.ScenarioObjectiveMatcherKt.matchesNodeObjective;
import static ro.ainpc.engine.ScenarioObjectiveMatcherKt.matchesPlaceObjective;
import static ro.ainpc.engine.ScenarioObjectiveMatcherKt.matchesRegionObjective;
import static ro.ainpc.engine.ScenarioQuestRulesKt.findObjectiveStageId;
import static ro.ainpc.engine.ScenarioQuestRulesKt.getQuestCategoryLimit;
import static ro.ainpc.engine.ScenarioQuestRulesKt.requiresQuestGiverTurnIn;
import static ro.ainpc.engine.ScenarioRoleAssignmentKt.baseRoleScore;
import static ro.ainpc.engine.ScenarioRoleAssignmentKt.canAssignMandatoryRoles;
import static ro.ainpc.engine.ScenarioRoleAssignmentKt.canTriggerScenario;
import static ro.ainpc.engine.ScenarioRoleAssignmentKt.hasConflictingPersonalities;
import static ro.ainpc.engine.ScenarioRoleAssignmentKt.hasMixedGenders;
import static ro.ainpc.engine.ScenarioRoleAssignmentKt.hasRequiredProfessions;
import static ro.ainpc.engine.ScenarioRoleAssignmentKt.hasRequiredTraits;
import static ro.ainpc.engine.ScenarioRoleAssignmentKt.matchesOccupation;
import static ro.ainpc.engine.ScenarioRoleAssignmentKt.scoreNpcForRole;
import static ro.ainpc.engine.ScenarioRoleAssignmentKt.selectBestNpcForRole;
import static ro.ainpc.engine.ScenarioEngineUtilsKt.buildQuestCompletionKey;
import static ro.ainpc.engine.ScenarioEngineUtilsKt.buildStoryActionData;
import static ro.ainpc.engine.ScenarioEngineUtilsKt.describeGenericQuestTrackingHint;
import static ro.ainpc.engine.ScenarioEngineUtilsKt.isQuestAnchorVariableKey;
import static ro.ainpc.engine.ScenarioEngineUtilsKt.readNullableLong;
import static ro.ainpc.engine.ScenarioEngineUtilsKt.readTextOrEmpty;
import static ro.ainpc.engine.ScenarioEngineUtilsKt.remainingQuestCooldownMillis;
import static ro.ainpc.engine.ScenarioEngineUtilsKt.resolveQuestNpcName;
import static ro.ainpc.engine.ScenarioEngineUtilsKt.storyActorId;
import static ro.ainpc.engine.ScenarioQuestDisplayKt.buildObjectiveProgressLines;
import static ro.ainpc.engine.ScenarioQuestDisplayKt.buildQuestNpcMessages;
import static ro.ainpc.engine.ScenarioQuestDisplayKt.buildQuestProgressDetailLines;
import static ro.ainpc.engine.ScenarioQuestDisplayKt.resolveQuestDialogueMessages;
import static ro.ainpc.engine.ScenarioQuestDisplayKt.resolveStatusDialogueContext;
import static ro.ainpc.engine.ScenarioTrackingDisplayKt.buildQuestTrackingMarker;
import static ro.ainpc.engine.ScenarioTrackingDisplayKt.formatQuestTrackingActionBar;
import static ro.ainpc.engine.ScenarioTrackingDisplayKt.formatQuestTrackingPosition;
import static ro.ainpc.engine.ScenarioTrackingDisplayKt.formatQuestTrackingTarget;
import static ro.ainpc.engine.ScenarioTrackingDisplayKt.toQuestTrackingLocation;
import static ro.ainpc.engine.ScenarioTrackingResolutionKt.buildQuestTrackingLines;
import static ro.ainpc.engine.ScenarioTrackingResolutionKt.describeObjectiveTrackingTarget;
import static ro.ainpc.engine.ScenarioTrackingResolutionKt.describeQuestGiverTrackingTarget;
import static ro.ainpc.engine.ScenarioTrackingResolutionKt.resolveNextQuestTrackingStep;
import static ro.ainpc.engine.ScenarioTrackingResolutionKt.resolveNpcByAnchorId;
import static ro.ainpc.engine.ScenarioTrackingResolutionKt.resolveNpcTrackingTarget;
import static ro.ainpc.engine.ScenarioTrackingResolutionKt.resolveQuestAnchorLocation;
import static ro.ainpc.engine.ScenarioTrackingResolutionKt.resolveQuestAnchorTrackingTarget;
import static ro.ainpc.engine.ScenarioTrackingResolutionKt.resolveQuestGiverNpc;
import static ro.ainpc.engine.ScenarioTrackingResolutionKt.resolveQuestGiverTrackingTarget;
import static ro.ainpc.engine.ScenarioTrackingResolutionKt.targetFromPlace;
import static ro.ainpc.engine.ScenarioTrackingResolutionKt.targetFromRegion;
import static ro.ainpc.engine.ScenarioStageVariablesKt.seedQuestStageVariables;
import static ro.ainpc.engine.ScenarioStageVariablesKt.buildQuestStageTransitionVariables;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import ro.ainpc.AINPCPlugin;
import ro.ainpc.api.WorldAdminApi;
import ro.ainpc.npc.AINPC;
import ro.ainpc.npc.NPCEmotions;
import ro.ainpc.npc.NPCPersonality;
import ro.ainpc.story.StoryStateService;
import ro.ainpc.world.StoryMode;
import ro.ainpc.world.WorldNode;
import ro.ainpc.world.WorldNodeInfo;
import ro.ainpc.world.WorldPlace;
import ro.ainpc.world.WorldPlaceInfo;
import ro.ainpc.world.WorldRegion;
import ro.ainpc.world.WorldRegionInfo;

import java.lang.reflect.Type;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Motor de scenarii emergente.
 * Scenariile built-in pot fi suprascrise de scenarii definite in addon packs.
 */
public class ScenarioEngine {

    private static final Type OBJECTIVE_PROGRESS_TYPE = new TypeToken<LinkedHashMap<String, Integer>>() { }.getType();
    private static final Type QUEST_VARIABLES_TYPE = new TypeToken<LinkedHashMap<String, String>>() { }.getType();

    private final AINPCPlugin plugin;
    private final Gson gson;
    private final Map<UUID, ActiveScenario> activeScenarios;
    private final Map<ScenarioType, ScenarioTemplate> scenarioTemplates;
    private final List<ScenarioTemplate> questTemplates;
    private final Map<UUID, Map<String, PlayerQuestProgress>> activePlayerQuests;
    private final Map<UUID, Map<String, PlayerQuestProgress>> archivedPlayerQuests;
    private final Set<String> questCompletionLocks;
    private final Set<UUID> trackedQuestPlayers;
    private final Map<UUID, String> trackedQuestTemplates;

    public ScenarioEngine(AINPCPlugin plugin) {
        this.plugin = plugin;
        ScenarioTrackingResolutionKt.setEnginePlugin(plugin);
        ScenarioSimpleQuestKt.initSimpleQuestPlugin(plugin);
        this.gson = new Gson();
        this.activeScenarios = new ConcurrentHashMap<>();
        this.scenarioTemplates = new EnumMap<>(ScenarioType.class);
        this.questTemplates = new ArrayList<>();
        this.activePlayerQuests = new ConcurrentHashMap<>();
        this.archivedPlayerQuests = new ConcurrentHashMap<>();
        this.questCompletionLocks = ConcurrentHashMap.newKeySet();
        this.trackedQuestPlayers = ConcurrentHashMap.newKeySet();
        this.trackedQuestTemplates = new ConcurrentHashMap<>();

        loadScenarioTemplates();
        loadPersistedQuestProgress();
    }

    public void reloadTemplates() {
        loadScenarioTemplates();
    }

    public void flushQuestProgress() {
        if (plugin.getDatabaseManager() == null) {
            return;
        }

        Map<UUID, List<PlayerQuestProgress>> snapshot = snapshotQuestProgress();
        if (snapshot.isEmpty()) {
            return;
        }

        try {
            plugin.getDatabaseManager().runAsync(() -> persistQuestProgressSnapshot(snapshot)).join();
            plugin.debug("[QuestEngine] Progres quest salvat la oprire pentru " + snapshot.size() + " jucatori.");
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("Nu am putut salva progresul quest-urilor la oprire: " + ex.getMessage());
        }
    }

    /**
     * Incarca template-urile implicite si apoi aplica override-urile din addon packs.
     */
    private void loadScenarioTemplates() {
        scenarioTemplates.clear();
        questTemplates.clear();

        ScenarioTemplate theft = new ScenarioTemplate(ScenarioType.THEFT);
        theft.addRole("THIEF", "Hotul care fura");
        theft.addRole("VICTIM", "Victima furtului");
        theft.addRole("WITNESS", "Martor la furt", true);
        theft.addRole("RESPONDER", "NPC care intervine", true);
        theft.addPhase("PLANNING", "Hotul planuieste furtul");
        theft.addPhase("EXECUTION", "Furtul are loc");
        theft.addPhase("DISCOVERY", "Victima descopera furtul");
        theft.addPhase("CONFLICT", "Confruntare intre parti");
        theft.addPhase("RESOLUTION", "Rezolvare - cineva intervine sau hotul fuge");
        theft.setTriggerProbability(0.05);
        theft.setMinimumNpcCount(2);
        scenarioTemplates.put(ScenarioType.THEFT, theft);

        ScenarioTemplate conflict = new ScenarioTemplate(ScenarioType.CONFLICT);
        conflict.addRole("AGGRESSOR", "Cel care incepe conflictul");
        conflict.addRole("DEFENDER", "Cel care se apara");
        conflict.addRole("MEDIATOR", "Cel care incearca sa medieze", true);
        conflict.addRole("SPECTATOR", "Spectatori", true);
        conflict.addPhase("TENSION", "Tensiune initiala");
        conflict.addPhase("ARGUMENT", "Cearta verbala");
        conflict.addPhase("ESCALATION", "Escaladare optionala");
        conflict.addPhase("RESOLUTION", "Rezolvare - pace sau lupta");
        conflict.setTriggerProbability(0.08);
        conflict.setMinimumNpcCount(2);
        scenarioTemplates.put(ScenarioType.CONFLICT, conflict);

        ScenarioTemplate celebration = new ScenarioTemplate(ScenarioType.CELEBRATION);
        celebration.addRole("HOST", "Gazda sarbatorii");
        celebration.addRole("GUEST", "Invitati", true);
        celebration.addRole("ENTERTAINER", "Cel care anima atmosfera", true);
        celebration.addPhase("GATHERING", "Lumea se strange");
        celebration.addPhase("CELEBRATION", "Sarbatoarea propriu-zisa");
        celebration.addPhase("PEAK", "Momentul culminant");
        celebration.addPhase("ENDING", "Sfarsitul sarbatorii");
        celebration.setTriggerProbability(0.03);
        celebration.setMinimumNpcCount(2);
        scenarioTemplates.put(ScenarioType.CELEBRATION, celebration);

        ScenarioTemplate emergency = new ScenarioTemplate(ScenarioType.EMERGENCY);
        emergency.addRole("VICTIM", "Cel in pericol");
        emergency.addRole("HELPER", "Cel care ajuta");
        emergency.addRole("COWARD", "Cel care fuge", true);
        emergency.addRole("LEADER", "Cel care organizeaza", true);
        emergency.addPhase("ALERT", "Alerta initiala");
        emergency.addPhase("PANIC", "Panica generala");
        emergency.addPhase("RESPONSE", "Raspunsul comunitatii");
        emergency.addPhase("RESOLUTION", "Rezolvare");
        emergency.setTriggerProbability(0.02);
        emergency.setMinimumNpcCount(2);
        scenarioTemplates.put(ScenarioType.EMERGENCY, emergency);

        ScenarioTemplate romance = new ScenarioTemplate(ScenarioType.ROMANCE);
        romance.addRole("SUITOR", "Curtezanul");
        romance.addRole("BELOVED", "Persoana iubita");
        romance.addRole("RIVAL", "Rival in dragoste", true);
        romance.addRole("CONFIDANT", "Prieten confident", true);
        romance.addPhase("ATTRACTION", "Atractie initiala");
        romance.addPhase("COURTSHIP", "Curte");
        romance.addPhase("COMPLICATION", "Complicatii");
        romance.addPhase("RESOLUTION", "Rezolvare");
        romance.setTriggerProbability(0.04);
        romance.setMinimumNpcCount(2);
        scenarioTemplates.put(ScenarioType.ROMANCE, romance);

        ScenarioTemplate tradeDeal = new ScenarioTemplate(ScenarioType.TRADE_DEAL);
        tradeDeal.addRole("SELLER", "Vanzatorul");
        tradeDeal.addRole("BUYER", "Cumparatorul");
        tradeDeal.addRole("COMPETITOR", "Competitor", true);
        tradeDeal.addPhase("NEGOTIATION", "Negociere");
        tradeDeal.addPhase("BARGAINING", "Tocmeala");
        tradeDeal.addPhase("AGREEMENT", "Acord sau esec");
        tradeDeal.setTriggerProbability(0.10);
        tradeDeal.setMinimumNpcCount(2);
        scenarioTemplates.put(ScenarioType.TRADE_DEAL, tradeDeal);

        ScenarioTemplate quest = new ScenarioTemplate(ScenarioType.QUEST);
        quest.addRole("QUEST_GIVER", "Cel care da misiunea");
        quest.addPlayerRole("HERO", "Eroul (jucatorul)");
        quest.addRole("HELPER", "Ajutor pentru erou", true);
        quest.addRole("ANTAGONIST", "Antagonistul", true);
        quest.addPhase("INTRODUCTION", "Prezentarea problemei");
        quest.addPhase("ACCEPTANCE", "Acceptarea misiunii");
        quest.addPhase("JOURNEY", "Calatoria/actiunea");
        quest.addPhase("COMPLETION", "Finalizare si recompensa");
        quest.setTriggerProbability(0.06);
        quest.setMinimumNpcCount(1);
        quest.setRequiresPlayer(true);
        scenarioTemplates.put(ScenarioType.QUEST, quest);

        ScenarioTemplate gossip = new ScenarioTemplate(ScenarioType.GOSSIP_SPREAD);
        gossip.addRole("ORIGINATOR", "Sursa zvonului");
        gossip.addRole("SPREADER", "Cel care raspandeste");
        gossip.addRole("SUBJECT", "Subiectul zvonului", true);
        gossip.addRole("SKEPTIC", "Cel care nu crede", true);
        gossip.addPhase("ORIGIN", "Nasterea zvonului");
        gossip.addPhase("SPREAD", "Raspandirea");
        gossip.addPhase("DISCOVERY", "Subiectul afla");
        gossip.addPhase("CONFRONTATION", "Confruntare");
        gossip.setTriggerProbability(0.07);
        gossip.setMinimumNpcCount(2);
        scenarioTemplates.put(ScenarioType.GOSSIP_SPREAD, gossip);

        loadAddonScenarioTemplates();
    }

    private void loadAddonScenarioTemplates() {
        FeaturePackLoader featurePackLoader = plugin.getFeaturePackLoader();
        if (featurePackLoader == null) {
            return;
        }

        FeaturePackLoader.FeaturePack primaryScenarioPack = featurePackLoader.getPrimaryScenarioPack();

        for (FeaturePackLoader.FeaturePack pack : featurePackLoader.getLoadedPacks()) {
            for (FeaturePackLoader.ScenarioDefinition definition : pack.getScenarios()) {
                ScenarioTemplate template = new ScenarioTemplate(definition.getBaseType());
                template.setTemplateId(pack.getId() + ":" + definition.getId());
                template.setDisplayName(definition.getName());
                template.setDescription(definition.getDescription());
                template.setSourcePackId(pack.getId());
                template.setHint(definition.getHint());
                template.setTriggerProbability(definition.getTriggerProbability());
                template.setMinimumNpcCount(definition.getMinimumNpcCount());
                template.setRequiresPlayer(definition.isRequiresPlayer());
                template.setPreferredTopologies(definition.getPreferredTopologies());
                template.setNarrativeHints(definition.getNarrativeHints());
                template.setProgressionEnabled(definition.isProgressionEnabled());
                template.setProgressionMechanicId(definition.getProgressionMechanicId());
                template.setProgressionKind(definition.getProgressionKind());
                template.setProgressionLabel(definition.getProgressionLabel());
                template.setProgressionSingularLabel(definition.getProgressionSingularLabel());
                template.setProgressionPluralLabel(definition.getProgressionPluralLabel());
                template.setProgressionMaxActive(definition.getProgressionMaxActive());
                template.setQuestCode(definition.getQuestCode());
                template.setQuestGiverProfession(definition.getQuestGiverProfession());
                template.setQuestPrerequisites(definition.getQuestPrerequisites());
                template.setQuestRepeatable(definition.isQuestRepeatable());
                template.setQuestCooldownSeconds(definition.getQuestCooldownSeconds());
                template.setQuestDialogues(definition.getQuestDialogues());
                template.setQuestStages(definition.getQuestStages());
                template.setObjectives(definition.getObjectives());
                template.setRewards(definition.getRewards());
                template.setQuestContract(QuestScenarioContract.fromScenarioDefinition(definition));

                for (FeaturePackLoader.ScenarioRoleDefinition roleDefinition : definition.getRoles().values()) {
                    ScenarioRoleRule role = new ScenarioRoleRule(
                        roleDefinition.getId(),
                        roleDefinition.getDescription(),
                        roleDefinition.isPlayerRole(),
                        roleDefinition.isOptional()
                    );
                    role.setRequiredProfessions(roleDefinition.getRequiredProfessions());
                    role.setPreferredProfessions(roleDefinition.getPreferredProfessions());
                    role.setRequiredTraits(roleDefinition.getRequiredTraits());
                    role.setPreferredTraits(roleDefinition.getPreferredTraits());
                    template.addRole(role);
                }

                for (String phase : definition.getPhases()) {
                    template.addPhase(phase, phase);
                }

                if (isProgressionRuntimeDefinition(definition, template)) {
                    questTemplates.add(template);
                }

                boolean shouldReplace = definition.isReplaceBaseType()
                    || primaryScenarioPack != null && primaryScenarioPack.getId().equalsIgnoreCase(pack.getId())
                    || !scenarioTemplates.containsKey(definition.getBaseType());

                if (shouldReplace) {
                    scenarioTemplates.put(definition.getBaseType(), template);
                    plugin.getLogger().info("Scenariu addon incarcat: " + template.getDisplayName()
                        + " (" + template.getTemplateId() + ")");
                }
            }
        }
    }

    private boolean isProgressionRuntimeDefinition(FeaturePackLoader.ScenarioDefinition definition,
                                                   ScenarioTemplate template) {
        if (definition == null || template == null || !template.hasQuestBriefing()) {
            return false;
        }
        return definition.getBaseType() == ScenarioType.QUEST || definition.isProgressionEnabled();
    }

    public QuestInteractionResult handleQuestInteraction(Player player, AINPC npc) {
        if (player == null || npc == null) {
            plugin.debug("[QuestEngine] handleQuestInteraction oprit: player sau npc este null.");
            return QuestInteractionResult.notHandled();
        }

        plugin.debug("[QuestEngine] handleQuestInteraction player=" + player.getName()
            + " npc=" + npc.getName()
            + " ocupatie=" + npc.getOccupation());
        UUID playerId = player.getUniqueId();
        ScenarioTemplate template = findQuestTemplateForNpc(npc, playerId);
        if (template == null || !template.hasQuestBriefing()) {
            plugin.debug("[QuestEngine] Nu exista template de quest pentru npc=" + npc.getName());
            return QuestInteractionResult.notHandled();
        }

        plugin.debug("[QuestEngine] Template gasit pentru npc=" + npc.getName()
            + " templateId=" + template.getTemplateId()
            + " title=" + resolveQuestTitle(template));

        PlayerQuestProgress currentProgress = getCurrentQuestProgress(playerId, template.getTemplateId());
        if (currentProgress != null) {
            plugin.debug("[QuestEngine] Progres curent pentru player=" + player.getName()
                + " templateId=" + currentProgress.templateId()
                + " status=" + currentProgress.status());
        } else {
            plugin.debug("[QuestEngine] Player=" + player.getName() + " nu are progres de quest inregistrat.");
        }

        if (currentProgress == null || !currentProgress.templateId().equals(template.getTemplateId())) {
            QuestAvailability availability = evaluateQuestAvailability(playerId, template);
            if (!availability.available()) {
                plugin.debug("[QuestEngine] Quest indisponibil pentru player=" + player.getName()
                    + " templateId=" + template.getTemplateId()
                    + " motiv=" + String.join("; ", availability.issues()));
                return buildQuestUnavailableResult(template, availability);
            }

            QuestAnchorResolver.ResolvedQuestAnchors resolvedAnchors = resolveQuestAnchors(template, player, npc);
            if (!resolvedAnchors.valid()) {
                return buildQuestUnavailableResult(template, resolvedAnchors);
            }

            PlayerQuestProgress offeredProgress = setInitialQuestProgress(playerId, player, template);
            offeredProgress = bindQuestProgressToNpc(playerId, template, offeredProgress, npc);
            offeredProgress = bindQuestProgressToAnchors(playerId, offeredProgress, resolvedAnchors);
            plugin.debug("[QuestEngine] Quest oferit pentru player=" + player.getName()
                + " templateId=" + template.getTemplateId());

            List<String> npcMessages = buildQuestNpcMessages(
                template,
                offeredProgress,
                resolveInitialQuestDialogueContext(template),
                buildInitialQuestNpcFallbackMessages(template)
            );
            return QuestInteractionResult.handled(
                true,
                npcMessages,
                buildQuestStatusMessages(template, offeredProgress, player, npc.getName())
            );
        }

        if (currentProgress.isOffered()) {
            return QuestInteractionResult.handled(
                true,
                buildQuestNpcMessages(
                    template,
                    currentProgress,
                    QuestDialogueContext.OFFERED,
                    List.of("Misiunea e a ta daca o vrei. Spune-mi clar daca o accepti.")
                ),
                buildQuestStatusMessages(template, currentProgress, player, npc.getName())
            );
        }

        currentProgress = refreshTrackedQuestProgress(player, template, currentProgress);
        currentProgress = trackNpcObjectiveProgress(player, npc, template, currentProgress);

        QuestObjectiveCheck objectiveCheck = inspectQuestObjectives(player, template, currentProgress, npc, true);
        if (!objectiveCheck.complete()) {
            plugin.debug("[QuestEngine] Quest incomplet pentru player=" + player.getName()
                + " lipsesc=" + String.join(", ", objectiveCheck.missingObjectives()));
            return QuestInteractionResult.handled(
                true,
                buildQuestNpcMessages(
                    template,
                    currentProgress,
                    QuestDialogueContext.ACTIVE,
                    List.of("Inca nu ai terminat tot ce ti-am cerut. Revino cand obiectivele sunt complete.")
                ),
                buildQuestStatusMessages(template, currentProgress, player, npc.getName())
            );
        }

        if (requiresQuestGiverTurnIn(template) && !matchesQuestGiver(plugin.getFeaturePackLoader(), npc, template)) {
            return QuestInteractionResult.handled(
                true,
                buildQuestNpcMessages(
                    template,
                    currentProgress,
                    QuestDialogueContext.READY,
                    List.of("Ai terminat ce era de facut. Intoarce-te la cel care ti-a dat misiunea.")
                ),
                buildQuestStatusMessages(template, currentProgress, player, npc.getName())
            );
        }

        String completionKey = buildQuestCompletionKey(playerId, template.getTemplateId());
        if (!questCompletionLocks.add(completionKey)) {
            return QuestInteractionResult.handled(
                true,
                List.of("Misiunea este deja in curs de finalizare."),
                buildQuestStatusMessages(template, currentProgress, player, npc.getName())
            );
        }

        try {
            QuestRewardCheck rewardCheck = inspectQuestRewardDelivery(
                player.getInventory(),
                template.getObjectives(),
                template.getRewards()
            );
            if (!rewardCheck.canGrant()) {
                List<String> systemMessages = buildQuestStatusMessages(template, currentProgress, player, npc.getName());
                systemMessages.add("&cNu pot finaliza questul pana cand recompensa poate fi acordata:");
                for (String issue : rewardCheck.issues()) {
                    systemMessages.add("&7- &f" + issue);
                }
                return QuestInteractionResult.handled(
                    true,
                    buildQuestNpcMessages(
                        template,
                        currentProgress,
                        QuestDialogueContext.READY,
                        List.of("Ai facut partea grea, dar fa putin loc pentru rasplata si vorbim din nou.")
                    ),
                    systemMessages
                );
            }

            consumeQuestObjectives(player.getInventory(), template.getObjectives());
            List<String> rewardNotes = grantQuestRewards(player, template.getRewards());
            player.updateInventory();
            rewardNotes.addAll(applyQuestStoryActions(player, npc, template, currentProgress, template.getRewards()));

            markQuestCompleted(playerId, template);
            plugin.debug("[QuestEngine] Quest completat pentru player=" + player.getName()
                + " templateId=" + template.getTemplateId());

            List<String> systemMessages = new ArrayList<>();
            systemMessages.add("&aQuest completat: &f" + resolveQuestTitle(template));
            systemMessages.add("&aRecompense primite:");
            for (FeaturePackLoader.QuestEntryDefinition reward : template.getRewards()) {
                systemMessages.add("&7- &f" + formatQuestEntry(reward));
            }
            systemMessages.addAll(rewardNotes);

            return QuestInteractionResult.handled(
                true,
                buildQuestNpcMessages(
                    template,
                    currentProgress,
                    QuestDialogueContext.COMPLETED,
                    List.of("Perfect. Exact materialele de care aveam nevoie.", "Poftim sabia promisa. Sa-ti fie de folos.")
                ),
                systemMessages
            );
        } finally {
            questCompletionLocks.remove(completionKey);
        }
    }

    public QuestInteractionResult acceptQuest(Player player, AINPC npc) {
        return acceptQuest(player, npc, "");
    }

    public QuestInteractionResult acceptQuest(Player player, AINPC npc, String progressionKind) {
        if (player == null || npc == null) {
            plugin.debug("[QuestEngine] acceptQuest oprit: player sau npc este null.");
            return QuestInteractionResult.notHandled();
        }

        UUID playerId = player.getUniqueId();
        ScenarioTemplate template = findQuestTemplateForNpc(npc, playerId, progressionKind);
        if (template == null || !template.hasQuestBriefing()) {
            plugin.debug("[QuestEngine] acceptQuest fara template pentru npc=" + npc.getName());
            return QuestInteractionResult.notHandled();
        }

        PlayerQuestProgress currentProgress = getCurrentQuestProgress(playerId, template.getTemplateId());
        PlayerQuestProgress completedProgress = getCompletedQuestProgress(playerId, template.getTemplateId());
        if (completedProgress != null && !template.isQuestRepeatable()) {
            return QuestInteractionResult.handled(
                true,
                buildQuestNpcMessages(
                    template,
                    completedProgress,
                    QuestDialogueContext.COMPLETED,
                    List.of("Misiunea asta este deja incheiata intre noi.")
                ),
                buildQuestStatusMessages(template, completedProgress, player, npc.getName())
            );
        }
        if (completedProgress != null) {
            QuestAvailability availability = evaluateQuestAvailability(playerId, template);
            if (!availability.available()) {
                return buildQuestUnavailableResult(template, availability);
            }
        }

        if (currentProgress == null || !currentProgress.templateId().equals(template.getTemplateId())) {
            return QuestInteractionResult.handled(
                true,
                List.of("Nu ti-am dat inca misiunea asta. Intreaba-ma mai intai de quest."),
                buildQuestStatusMessages(template, null, player, npc.getName())
            );
        }

        if (currentProgress.isActive()) {
            return QuestInteractionResult.handled(
                true,
                buildQuestNpcMessages(
                    template,
                    currentProgress,
                    QuestDialogueContext.ACTIVE,
                    List.of("Ai acceptat deja misiunea. Ma astept sa te intorci cu ce ti-am cerut.")
                ),
                buildQuestStatusMessages(template, currentProgress, player, npc.getName())
            );
        }

        QuestAnchorResolver.ResolvedQuestAnchors resolvedAnchors = resolveQuestAnchors(template, player, npc);
        if (!resolvedAnchors.valid()) {
            return buildQuestUnavailableResult(template, resolvedAnchors);
        }

        PlayerQuestProgress acceptedProgress = setActiveQuestProgress(playerId, player, template);
        acceptedProgress = bindQuestProgressToNpc(playerId, template, acceptedProgress, npc);
        acceptedProgress = bindQuestProgressToAnchors(playerId, acceptedProgress, resolvedAnchors);
        plugin.debug("[QuestEngine] Quest acceptat pentru player=" + player.getName()
            + " templateId=" + template.getTemplateId());
        return QuestInteractionResult.handled(
            true,
            buildQuestNpcMessages(
                template,
                acceptedProgress,
                QuestDialogueContext.ACCEPTED,
                List.of("Bine. Ma bazez pe tine.", "Intoarce-te cand ai terminat.")
            ),
            buildQuestStatusMessages(template, acceptedProgress, player, npc.getName())
        );
    }

    public QuestInteractionResult declineQuest(Player player, AINPC npc) {
        return declineQuest(player, npc, "");
    }

    public QuestInteractionResult declineQuest(Player player, AINPC npc, String progressionKind) {
        if (player == null || npc == null) {
            plugin.debug("[QuestEngine] declineQuest oprit: player sau npc este null.");
            return QuestInteractionResult.notHandled();
        }

        UUID playerId = player.getUniqueId();
        ScenarioTemplate template = findQuestTemplateForNpc(npc, playerId, progressionKind);
        if (template == null || !template.hasQuestBriefing()) {
            plugin.debug("[QuestEngine] declineQuest fara template pentru npc=" + npc.getName());
            return QuestInteractionResult.notHandled();
        }

        PlayerQuestProgress currentProgress = getCurrentQuestProgress(playerId, template.getTemplateId());
        PlayerQuestProgress completedProgress = getCompletedQuestProgress(playerId, template.getTemplateId());
        if (completedProgress != null) {
            return QuestInteractionResult.handled(
                true,
                List.of("Prea tarziu sa refuzi. Misiunea asta este deja incheiata."),
                buildQuestStatusMessages(template, completedProgress, player, npc.getName())
            );
        }

        if (currentProgress == null || !currentProgress.templateId().equals(template.getTemplateId())) {
            return QuestInteractionResult.handled(
                true,
                List.of("Nu ai o oferta de quest activa de la mine."),
                buildQuestStatusMessages(template, null, player, npc.getName())
            );
        }

        if (currentProgress.isActive()) {
            return QuestInteractionResult.handled(
                true,
                List.of("Ai acceptat deja misiunea. Daca vrei sa renunti, abandoneaz-o."),
                buildQuestStatusMessages(template, currentProgress, player, npc.getName())
            );
        }

        removeActiveQuestProgress(playerId, template.getTemplateId());
        deleteQuestProgressAsync(playerId, template.getTemplateId());
        return QuestInteractionResult.handled(
            true,
            List.of("In regula. Poate alta data."),
            buildQuestStatusMessages(template, null, player, npc.getName())
        );
    }

    public QuestInteractionResult abandonQuest(Player player, AINPC npc) {
        if (player == null || npc == null) {
            plugin.debug("[QuestEngine] abandonQuest oprit: player sau npc este null.");
            return QuestInteractionResult.notHandled();
        }

        UUID playerId = player.getUniqueId();
        ScenarioTemplate template = findQuestTemplateForNpc(npc, playerId);
        if (template == null || !template.hasQuestBriefing()) {
            plugin.debug("[QuestEngine] abandonQuest fara template pentru npc=" + npc.getName());
            return QuestInteractionResult.notHandled();
        }

        PlayerQuestProgress currentProgress = getCurrentQuestProgress(playerId, template.getTemplateId());
        PlayerQuestProgress completedProgress = getCompletedQuestProgress(playerId, template.getTemplateId());
        if (completedProgress != null) {
            return QuestInteractionResult.handled(
                true,
                List.of("Misiunea asta este deja terminata. Nu mai ai la ce renunta."),
                buildQuestStatusMessages(template, completedProgress, player, npc.getName())
            );
        }

        if (currentProgress == null || !currentProgress.templateId().equals(template.getTemplateId())) {
            return QuestInteractionResult.handled(
                true,
                List.of("Nu ai un quest activ de abandonat la mine."),
                buildQuestStatusMessages(template, getFailedQuestProgress(playerId, template.getTemplateId()), player, npc.getName())
            );
        }

        if (currentProgress.isOffered()) {
            return QuestInteractionResult.handled(
                true,
                List.of("Nu ai acceptat inca misiunea. O poti doar refuza."),
                buildQuestStatusMessages(template, currentProgress, player, npc.getName())
            );
        }

        PlayerQuestProgress failedProgress = markQuestFailed(playerId, template);
        return QuestInteractionResult.handled(
            true,
            buildQuestNpcMessages(
                template,
                failedProgress,
                QuestDialogueContext.FAILED,
                List.of("Am inteles. Consider misiunea abandonata.")
            ),
            buildQuestStatusMessages(template, failedProgress, player, npc.getName())
        );
    }

    public QuestInteractionResult abandonQuest(Player player, String questReference) {
        if (player == null || questReference == null || questReference.isBlank()) {
            plugin.debug("[QuestEngine] abandonQuest selector oprit: player sau selector lipsa.");
            return QuestInteractionResult.notHandled();
        }

        UUID playerId = player.getUniqueId();
        PlayerQuestProgress progress = isTrackedQuestSelector(questReference)
            ? getTrackedQuestProgress(playerId, true)
            : findQuestProgressByReference(playerId, questReference, true);
        if (progress == null) {
            return QuestInteractionResult.notHandled();
        }

        ScenarioTemplate template = resolveTemplateForProgress(progress, null);
        List<String> systemMessages = new ArrayList<>();
        systemMessages.add("&6=== Quest Abandon ===");
        systemMessages.add("&eJucator: &f" + player.getName());

        if (template == null) {
            systemMessages.add("&eQuest: &f" + progress.templateId());
            systemMessages.add("&7Status: &f" + formatQuestStatus(progress.status()));
            systemMessages.add("&cTemplate-ul questului nu mai este disponibil; nu pot schimba starea in siguranta.");
            return QuestInteractionResult.handled(false, List.of(), systemMessages);
        }

        if (progress.isCompleted()) {
            systemMessages.add("&7Misiunea asta este deja terminata. Nu mai ai la ce renunta.");
            systemMessages.addAll(buildQuestStatusMessages(template, progress, player, resolveQuestNpcName(progress)));
            return QuestInteractionResult.handled(false, List.of(), systemMessages);
        }

        if (progress.status() == QuestStatus.FAILED) {
            systemMessages.add("&7Questul este deja abandonat sau esuat.");
            systemMessages.addAll(buildQuestStatusMessages(template, progress, player, resolveQuestNpcName(progress)));
            return QuestInteractionResult.handled(false, List.of(), systemMessages);
        }

        if (progress.isOffered()) {
            systemMessages.add("&7Nu ai acceptat inca misiunea. O poti doar refuza.");
            systemMessages.addAll(buildQuestStatusMessages(template, progress, player, resolveQuestNpcName(progress)));
            return QuestInteractionResult.handled(false, List.of(), systemMessages);
        }

        if (!progress.isActive()) {
            systemMessages.add("&7Questul nu este activ si nu poate fi abandonat.");
            systemMessages.addAll(buildQuestStatusMessages(template, progress, player, resolveQuestNpcName(progress)));
            return QuestInteractionResult.handled(false, List.of(), systemMessages);
        }

        PlayerQuestProgress failedProgress = markQuestFailed(playerId, template);
        systemMessages.add("&eQuest abandonat: &f" + resolveQuestTitle(template));
        systemMessages.addAll(buildQuestStatusMessages(template, failedProgress, player, resolveQuestNpcName(failedProgress)));
        return QuestInteractionResult.handled(false, List.of(), systemMessages);
    }

    public QuestInteractionResult startQuestManually(Player player, AINPC npc) {
        return startQuestManually(player, npc, "");
    }

    public QuestInteractionResult startQuestManually(Player player, AINPC npc, String progressionKind) {
        if (player == null || npc == null) {
            plugin.debug("[QuestEngine] startQuestManually oprit: player sau npc este null.");
            return QuestInteractionResult.notHandled();
        }

        UUID playerId = player.getUniqueId();
        ScenarioTemplate template = findQuestTemplateForNpc(npc, playerId, progressionKind);
        if (template == null || !template.hasQuestBriefing()) {
            plugin.debug("[QuestEngine] startQuestManually fara template pentru npc=" + npc.getName());
            return QuestInteractionResult.notHandled();
        }

        PlayerQuestProgress currentProgress = getCurrentQuestProgress(playerId, template.getTemplateId());
        if (currentProgress != null && currentProgress.templateId().equals(template.getTemplateId())) {
            return getQuestStatus(player, npc);
        }

        QuestAvailability availability = evaluateQuestAvailability(playerId, template);
        if (!availability.available()) {
            return buildQuestUnavailableResult(template, availability);
        }

        QuestAnchorResolver.ResolvedQuestAnchors resolvedAnchors = resolveQuestAnchors(template, player, npc);
        if (!resolvedAnchors.valid()) {
            return buildQuestUnavailableResult(template, resolvedAnchors);
        }

        removeArchivedQuestProgress(playerId, template.getTemplateId());
        PlayerQuestProgress offeredProgress = setInitialQuestProgress(playerId, player, template);
        offeredProgress = bindQuestProgressToNpc(playerId, template, offeredProgress, npc);
        offeredProgress = bindQuestProgressToAnchors(playerId, offeredProgress, resolvedAnchors);
        plugin.debug("[QuestEngine] startQuestManually a oferit questul pentru player=" + player.getName()
            + " templateId=" + template.getTemplateId());

        List<String> npcMessages = buildQuestNpcMessages(
            template,
            offeredProgress,
            resolveInitialQuestDialogueContext(template),
            buildInitialQuestNpcFallbackMessages(template)
        );
        return QuestInteractionResult.handled(
            true,
            npcMessages,
            buildQuestStatusMessages(template, offeredProgress, player, npc.getName())
        );
    }

    public boolean resetQuestProgress(Player player, AINPC npc) {
        if (player == null || npc == null) {
            plugin.debug("[QuestEngine] resetQuestProgress oprit: player sau npc este null.");
            return false;
        }

        UUID playerId = player.getUniqueId();
        ScenarioTemplate template = findQuestTemplateForNpc(npc, playerId);
        if (template == null || !template.hasQuestBriefing()) {
            plugin.debug("[QuestEngine] resetQuestProgress fara template pentru npc=" + npc.getName());
            return false;
        }

        boolean removedActive = removeActiveQuestProgress(playerId, template.getTemplateId());
        boolean removedArchived = removeArchivedQuestProgress(playerId, template.getTemplateId());
        if (!removedActive && !removedArchived) {
            plugin.debug("[QuestEngine] resetQuestProgress fara progres potrivit pentru player="
                + player.getName() + " templateId=" + template.getTemplateId());
            return false;
        }

        clearQuestTrackingIfMatches(playerId, template.getTemplateId());
        deleteQuestProgressAsync(playerId, template.getTemplateId());

        plugin.debug("[QuestEngine] resetQuestProgress reusit pentru player=" + player.getName()
            + " templateId=" + template.getTemplateId());
        return true;
    }

    public QuestInteractionResult forceCompleteQuest(Player player, AINPC npc) {
        if (player == null || npc == null) {
            plugin.debug("[QuestEngine] forceCompleteQuest oprit: player sau npc este null.");
            return QuestInteractionResult.notHandled();
        }

        UUID playerId = player.getUniqueId();
        ScenarioTemplate template = findQuestTemplateForNpc(npc, playerId);
        if (template == null || !template.hasQuestBriefing()) {
            plugin.debug("[QuestEngine] forceCompleteQuest fara template pentru npc=" + npc.getName());
            return QuestInteractionResult.notHandled();
        }

        plugin.debug("[QuestEngine] forceCompleteQuest pentru player=" + player.getName()
            + " templateId=" + template.getTemplateId());

        PlayerQuestProgress completedProgress = getCompletedQuestProgress(playerId, template.getTemplateId());
        if (completedProgress != null) {
            return QuestInteractionResult.handled(
                false,
                buildQuestNpcMessages(
                    template,
                    completedProgress,
                    QuestDialogueContext.COMPLETED,
                    List.of("Misiunea asta este deja marcata ca terminata.")
                ),
                buildQuestStatusMessages(template, completedProgress, player, npc.getName())
            );
        }

        String completionKey = buildQuestCompletionKey(playerId, template.getTemplateId());
        if (!questCompletionLocks.add(completionKey)) {
            return QuestInteractionResult.handled(
                false,
                List.of("Misiunea este deja in curs de finalizare."),
                buildQuestStatusMessages(template, getCurrentQuestProgress(playerId, template.getTemplateId()), player, npc.getName())
            );
        }

        try {
            QuestRewardCheck rewardCheck = inspectQuestRewardDelivery(
                player.getInventory(),
                List.of(),
                template.getRewards()
            );
            if (!rewardCheck.canGrant()) {
                List<String> systemMessages = buildQuestStatusMessages(
                    template,
                    getCurrentQuestProgress(playerId, template.getTemplateId()),
                    player,
                    npc.getName()
                );
                systemMessages.add("&cNu pot marca questul ca finalizat pana cand recompensa poate fi acordata:");
                for (String issue : rewardCheck.issues()) {
                    systemMessages.add("&7- &f" + issue);
                }
                return QuestInteractionResult.handled(
                    false,
                    List.of("Fa loc pentru rasplata inainte sa inchid misiunea."),
                    systemMessages
                );
            }

            List<String> rewardNotes = grantQuestRewards(player, template.getRewards());
            player.updateInventory();

            markQuestCompleted(playerId, template);
            plugin.debug("[QuestEngine] forceCompleteQuest a marcat quest complet pentru player="
                + player.getName() + " templateId=" + template.getTemplateId());

            List<String> systemMessages = new ArrayList<>();
            systemMessages.add("&aQuest marcat manual ca finalizat: &f" + resolveQuestTitle(template));
            if (!template.getRewards().isEmpty()) {
                systemMessages.add("&aRecompense acordate:");
                for (FeaturePackLoader.QuestEntryDefinition reward : template.getRewards()) {
                    systemMessages.add("&7- &f" + formatQuestEntry(reward));
                }
            }
            systemMessages.addAll(rewardNotes);

            return QuestInteractionResult.handled(
                false,
                buildQuestNpcMessages(
                    template,
                    null,
                    QuestDialogueContext.COMPLETED,
                    List.of("In regula. Consider misiunea terminata.", "Poftim rasplata promisa.")
                ),
                systemMessages
            );
        } finally {
            questCompletionLocks.remove(completionKey);
        }
    }

    public String getQuestTitle(AINPC npc) {
        ScenarioTemplate template = findQuestTemplateForNpc(npc);
        if (template == null || !template.hasQuestBriefing()) {
            return "";
        }
        return resolveQuestTitle(template);
    }

    public boolean hasOfferedQuest(Player player) {
        if (player == null) {
            return false;
        }

        return getCurrentQuestProgress(player.getUniqueId()).stream().anyMatch(PlayerQuestProgress::isOffered);
    }

    public AINPC resolveActiveQuestNpc(Player player) {
        return resolveActiveQuestNpc(player, (AINPC) null);
    }

    public AINPC resolveActiveQuestNpc(Player player, String progressionKind) {
        return resolveActiveQuestNpc(player, progressionKind, null);
    }

    public AINPC resolveActiveQuestNpc(Player player, AINPC fallbackNpc) {
        return resolveActiveQuestNpc(player, "", fallbackNpc);
    }

    public AINPC resolveActiveQuestNpc(Player player, String progressionKind, AINPC fallbackNpc) {
        if (player == null) {
            return null;
        }

        for (PlayerQuestProgress progress : getCurrentQuestProgress(player.getUniqueId())) {
            if (!progress.isCurrent()) {
                continue;
            }

            AINPC questGiver = resolveQuestGiverNpc(progress);
            ScenarioTemplate template = resolveTemplateForProgress(progress, null);
            if (questGiver != null && matchesProgressionKindFilter(template, progressionKind)) {
                return questGiver;
            }
        }

        if (fallbackNpc == null) {
            return null;
        }

        for (PlayerQuestProgress progress : getCurrentQuestProgress(player.getUniqueId())) {
            ScenarioTemplate fallbackTemplate = resolveTemplateForProgress(progress, null);
            if (fallbackTemplate != null
                && matchesProgressionKindFilter(fallbackTemplate, progressionKind)
                && matchesQuestGiver(plugin.getFeaturePackLoader(), fallbackNpc, fallbackTemplate)) {
                return fallbackNpc;
            }
        }

        return null;
    }

    public boolean hasQuestForNpc(Player player, AINPC npc, String progressionKind) {
        if (player == null || npc == null) {
            return false;
        }

        ScenarioTemplate template = findQuestTemplateForNpc(npc, player.getUniqueId(), progressionKind);
        return template != null && template.hasQuestBriefing();
    }


    public QuestInteractionResult getQuestStatus(Player player, AINPC npc) {
        if (player == null || npc == null) {
            return QuestInteractionResult.notHandled();
        }

        UUID playerId = player.getUniqueId();
        ScenarioTemplate template = findQuestTemplateForNpc(npc, playerId);
        if (template == null || !template.hasQuestBriefing()) {
            return QuestInteractionResult.notHandled();
        }

        PlayerQuestProgress currentProgress = getCurrentQuestProgress(playerId, template.getTemplateId());
        PlayerQuestProgress completedProgress = getCompletedQuestProgress(playerId, template.getTemplateId());
        PlayerQuestProgress failedProgress = getFailedQuestProgress(playerId, template.getTemplateId());

        if (completedProgress != null) {
            return QuestInteractionResult.handled(
                true,
                buildQuestNpcMessages(
                    template,
                    completedProgress,
                    QuestDialogueContext.COMPLETED,
                    List.of("Ti-ai dus la capat datoria pentru misiunea asta.")
                ),
                buildQuestStatusMessages(template, completedProgress, player, npc.getName())
            );
        }

        if (failedProgress != null) {
            return QuestInteractionResult.handled(
                true,
                buildQuestNpcMessages(
                    template,
                    failedProgress,
                    QuestDialogueContext.FAILED,
                    List.of("Misiunea asta a fost abandonata sau esuata.")
                ),
                buildQuestStatusMessages(template, failedProgress, player, npc.getName())
            );
        }

        if (currentProgress != null && currentProgress.templateId().equals(template.getTemplateId())) {
            currentProgress = refreshTrackedQuestProgress(player, template, currentProgress);
            QuestDialogueContext context = currentProgress.isOffered()
                ? QuestDialogueContext.OFFERED
                : resolveStatusDialogueContext(player, template, currentProgress);
            return QuestInteractionResult.handled(
                true,
                buildQuestNpcMessages(
                    template,
                    currentProgress,
                    context,
                    List.of(currentProgress.isOffered()
                        ? "Inca astept sa-mi spui daca accepti."
                        : "Asa stai acum cu misiunea.")
                ),
                buildQuestStatusMessages(template, currentProgress, player, npc.getName())
            );
        }

        return QuestInteractionResult.handled(
            true,
            buildQuestNpcMessages(
                template,
                null,
                QuestDialogueContext.OFFER,
                List.of("Misiunea este disponibila, dar inca nu ai acceptat-o.")
            ),
            buildQuestStatusMessages(template, null, player, npc.getName())
        );
    }

    public QuestInteractionResult getQuestLog(Player player) {
        return getQuestLog(player, "");
    }

    public QuestInteractionResult getQuestLog(Player player, String filter) {
        return getQuestLog(player, filter, false);
    }

    public QuestInteractionResult getQuestLog(Player player, String filter, boolean adminView) {
        if (player == null) {
            return QuestInteractionResult.notHandled();
        }

        UUID playerId = player.getUniqueId();
        QuestLogFilter logFilter = parseQuestLogFilter(filter);
        List<PlayerQuestProgress> currentProgresses = getCurrentQuestProgress(playerId);
        List<PlayerQuestProgress> archivedProgresses = getArchivedQuestProgress(playerId);
        List<String> systemMessages = new ArrayList<>();
        systemMessages.add("&6=== Progression Log ===");
        systemMessages.add("&eJucator: &f" + player.getName());
        if (logFilter != QuestLogFilter.SUMMARY) {
            systemMessages.add("&eFiltru: &f" + logFilter.displayName());
        }
        if (!currentProgresses.isEmpty()) {
            systemMessages.addAll(buildQuestLogSummaryLines(playerId, currentProgresses));
        }

        List<PlayerQuestProgress> matchingCurrent = currentProgresses.stream()
            .filter(progress -> questLogMatches(playerId, progress, logFilter, false))
            .sorted(questLogCurrentComparator(playerId))
            .toList();
        if (!matchingCurrent.isEmpty()) {
            systemMessages.add("&aProgresii curente: &f" + matchingCurrent.size());
            String currentGroup = "";
            for (PlayerQuestProgress currentProgress : matchingCurrent) {
                ScenarioTemplate template = resolveTemplateForProgress(currentProgress, null);
                PlayerQuestProgress logProgress = template != null
                    ? refreshTrackedQuestProgress(player, template, currentProgress)
                    : currentProgress;
                String groupLabel = questLogCurrentGroupLabel(playerId, template, logProgress);
                if (!groupLabel.equals(currentGroup)) {
                    systemMessages.add(groupLabel);
                    currentGroup = groupLabel;
                }

                if (template != null) {
                    if (isTrackedQuest(playerId, logProgress)) {
                        systemMessages.add("&bQuest urmarit: &f" + resolveQuestTitle(template));
                    }
                    systemMessages.addAll(buildQuestStatusMessages(
                        template,
                        logProgress,
                        player,
                        resolveQuestNpcName(logProgress)
                    ));
                    systemMessages.addAll(buildQuestLogActionLines(player, playerId, template, logProgress, adminView));
                } else {
                    systemMessages.add("&7Template: &f" + logProgress.templateId());
                    systemMessages.add("&7Status: &f" + formatQuestStatus(logProgress.status()));
                    if (isTrackedQuest(playerId, logProgress)) {
                        systemMessages.add("&bProgresie urmarita: &fda");
                    }
                    if (!logProgress.currentPhase().isBlank()) {
                        systemMessages.add("&7Faza curenta: &f" + formatQuestPhase(logProgress.currentPhase()));
                    }
                    systemMessages.addAll(buildQuestLogActionLines(player, playerId, null, logProgress, adminView));
                }
            }
        } else if (logFilter.showsCurrent()) {
            systemMessages.add(logFilter == QuestLogFilter.SUMMARY
                ? "&7Nu ai progresie activa."
                : "&7Nu exista progresii curente pentru filtrul ales.");
        }

        int archivedLimit = logFilter == QuestLogFilter.SUMMARY ? 3 : 20;
        List<PlayerQuestProgress> matchingArchived = archivedProgresses.stream()
            .filter(progress -> questLogMatches(playerId, progress, logFilter, true))
            .limit(archivedLimit)
            .toList();
        if (!matchingArchived.isEmpty()) {
            systemMessages.add(logFilter == QuestLogFilter.SUMMARY ? "&eUltimele progresii:" : "&eProgresii arhivate:");
            for (PlayerQuestProgress archivedProgress : matchingArchived) {
                ScenarioTemplate template = resolveTemplateForProgress(archivedProgress, null);
                String title = template != null ? resolveQuestTitle(template) : archivedProgress.templateId();
                systemMessages.add(formatQuestLogArchivedLine(playerId, template, archivedProgress, title));
            }
            long totalMatchingArchived = archivedProgresses.stream()
                .filter(progress -> questLogMatches(playerId, progress, logFilter, true))
                .count();
            if (totalMatchingArchived > matchingArchived.size()) {
                systemMessages.add("&7... inca &f" + (totalMatchingArchived - matchingArchived.size())
                    + " &7progresii arhivate pentru filtrul ales.");
            }
        } else if (logFilter.showsArchived() && logFilter != QuestLogFilter.SUMMARY) {
            systemMessages.add("&7Nu exista progresii arhivate pentru filtrul ales.");
        }

        return QuestInteractionResult.handled(false, List.of(), systemMessages);
    }

    public QuestGuiSnapshot getQuestGuiSnapshot(Player player, String filter, boolean adminView) {
        if (player == null) {
            return QuestGuiSnapshot.empty();
        }

        UUID playerId = player.getUniqueId();
        QuestLogFilter logFilter = parseQuestLogFilter(filter);
        List<PlayerQuestProgress> currentProgresses = getCurrentQuestProgress(playerId);
        List<PlayerQuestProgress> archivedProgresses = getArchivedQuestProgress(playerId);
        List<String> summaryLines = currentProgresses.isEmpty()
            ? List.of("&7Nu ai progresie activa.")
            : buildQuestLogSummaryLines(playerId, currentProgresses);

        List<QuestGuiEntry> currentEntries = currentProgresses.stream()
            .filter(progress -> questLogMatches(playerId, progress, logFilter, false))
            .sorted(questLogCurrentComparator(playerId))
            .map(progress -> buildQuestGuiEntry(player, playerId, progress, false, adminView))
            .toList();

        int archivedLimit = logFilter == QuestLogFilter.SUMMARY ? 3 : 20;
        List<QuestGuiEntry> archivedEntries = archivedProgresses.stream()
            .filter(progress -> questLogMatches(playerId, progress, logFilter, true))
            .limit(archivedLimit)
            .map(progress -> buildQuestGuiEntry(player, playerId, progress, true, adminView))
            .toList();

        long totalMatchingArchived = archivedProgresses.stream()
            .filter(progress -> questLogMatches(playerId, progress, logFilter, true))
            .count();

        return new QuestGuiSnapshot(
            true,
            player.getName(),
            logFilter.displayName(),
            summaryLines,
            currentEntries,
            archivedEntries,
            totalMatchingArchived
        );
    }

    private QuestGuiEntry buildQuestGuiEntry(Player player,
                                             UUID playerId,
                                             PlayerQuestProgress progress,
                                             boolean archived,
                                             boolean adminView) {
        ScenarioTemplate template = resolveTemplateForProgress(progress, null);
        PlayerQuestProgress viewProgress = template != null && progress != null && progress.isCurrent()
            ? refreshTrackedQuestProgress(player, template, progress)
            : progress;

        String selector = questLogActionSelector(template, viewProgress);
        String title = template != null ? resolveQuestTitle(template) : valueOrFallback(viewProgress.templateId(), "Quest necunoscut");
        String category = template != null ? resolveQuestCategory(template).displayName() : "Necunoscut";
        String mechanic = template != null ? resolveProgressionMechanicDisplay(plugin.getFeaturePackLoader(), template) : "Necunoscuta";
        String statusDisplay = formatQuestStatus(viewProgress != null ? viewProgress.status() : QuestStatus.NOT_STARTED);
        String currentStageId = "";
        if (viewProgress != null) {
            currentStageId = viewProgress.currentPhase();
            if (currentStageId.isBlank() && template != null) {
                currentStageId = resolveQuestPhase(template, viewProgress.status(), viewProgress);
            }
        }

        List<String> statusLines = template != null
            ? buildQuestStatusMessages(template, viewProgress, player, viewProgress != null ? resolveQuestNpcName(viewProgress) : "")
            : buildMissingQuestTemplateLines(viewProgress);

        return new QuestGuiEntry(
            selector,
            viewProgress != null ? valueOrFallback(viewProgress.templateId(), "") : "",
            viewProgress != null ? valueOrFallback(viewProgress.questCode(), "") : "",
            title,
            statusDisplay,
            category,
            mechanic,
            viewProgress != null && isTrackedQuest(playerId, viewProgress),
            viewProgress != null && viewProgress.isCurrent(),
            viewProgress != null && viewProgress.isActive(),
            viewProgress != null && viewProgress.isOffered(),
            archived,
            template == null,
            currentStageId,
            formatQuestPhase(currentStageId),
            viewProgress != null ? viewProgress.updatedAt() : 0L,
            viewProgress != null ? resolveQuestNpcName(viewProgress) : "",
            statusLines,
            template != null ? buildQuestGuiObjectives(player, template, viewProgress) : List.of(),
            template != null ? buildQuestGuiStages(template, viewProgress, currentStageId) : List.of(),
            template != null ? template.getRewards().stream().map(ScenarioEngineTextKt::formatQuestEntry).toList() : List.of(),
            buildQuestLogActionLines(player, playerId, template, viewProgress, adminView)
        );
    }

    private List<String> buildMissingQuestTemplateLines(PlayerQuestProgress progress) {
        if (progress == null) {
            return List.of("&cQuest progress indisponibil.");
        }

        List<String> lines = new ArrayList<>();
        lines.add("&eTemplate lipsa: &f" + progress.templateId());
        lines.add("&7Status: &f" + formatQuestStatus(progress.status()));
        if (!progress.currentPhase().isBlank()) {
            lines.add("&7Faza curenta: &f" + formatQuestPhase(progress.currentPhase()));
        }
        return lines;
    }

    private List<QuestGuiObjective> buildQuestGuiObjectives(Player player,
                                                            ScenarioTemplate template,
                                                            PlayerQuestProgress progress) {
        if (template == null || template.getObjectives().isEmpty()) {
            return List.of();
        }

        List<QuestGuiObjective> objectives = new ArrayList<>();
        List<FeaturePackLoader.QuestEntryDefinition> entries = template.getObjectives();
        for (int index = 0; index < entries.size(); index++) {
            FeaturePackLoader.QuestEntryDefinition objective = entries.get(index);
            String objectiveKey = buildObjectiveKey(objective, index);
            int requiredAmount = Math.max(1, objective.getAmount());
            int storedProgress = progress != null
                ? readObjectiveProgress(progress.objectiveProgress(), objective, index)
                : 0;
            int currentProgress = progress != null && progress.isActive()
                ? resolveObjectiveCurrentProgress(player, objective, progress, index)
                : Math.min(requiredAmount, storedProgress);
            boolean activeForStage = progress == null || shouldShowObjectiveForCurrentStage(template, progress, objective);
            String stageId = findObjectiveStageId(template, objective);
            QuestObjectiveState objectiveState = resolveQuestObjectiveState(
                progress,
                currentProgress,
                requiredAmount,
                activeForStage
            );

            objectives.add(new QuestGuiObjective(
                objectiveKey,
                normalizeObjectiveType(objective.getType()),
                formatObjectiveProgressLabel(objective),
                formatQuestEntry(objective),
                stageId,
                formatQuestPhase(stageId),
                objectiveState.id(),
                objectiveState.displayName(),
                Math.min(currentProgress, requiredAmount),
                requiredAmount,
                currentProgress >= requiredAmount,
                activeForStage
            ));
        }
        return objectives;
    }

    private List<QuestGuiStage> buildQuestGuiStages(ScenarioTemplate template,
                                                    PlayerQuestProgress progress,
                                                    String currentStageId) {
        if (template == null || template.getQuestStages().isEmpty()) {
            return List.of();
        }

        List<QuestGuiStage> stages = new ArrayList<>();
        for (FeaturePackLoader.QuestStageDefinition stage : template.getQuestStages()) {
            if (stage == null || stage.getId().isBlank()) {
                continue;
            }

            boolean active = !currentStageId.isBlank() && phasesMatch(stage.getId(), currentStageId);
            boolean complete = progress != null && areObjectivesSatisfiedForStage(template, stage.getId(), progress.objectiveProgress());
            stages.add(new QuestGuiStage(
                stage.getId(),
                formatQuestPhase(stage.getId()),
                stage.getDescription(),
                formatStageCompletionMode(stage.getCompletionMode()),
                stage.getNextStageId(),
                active,
                complete,
                stage.getObjectiveIds()
            ));
        }
        return stages;
    }


    public QuestInteractionResult getQuestStatus(Player player, String questReference) {
        if (player == null || questReference == null || questReference.isBlank()) {
            return QuestInteractionResult.notHandled();
        }

        PlayerQuestProgress progress = findQuestProgressByReference(player.getUniqueId(), questReference, true);
        if (progress == null) {
            return QuestInteractionResult.notHandled();
        }

        ScenarioTemplate template = resolveTemplateForProgress(progress, null);
        if (template != null && progress.isCurrent()) {
            progress = refreshTrackedQuestProgress(player, template, progress);
        }

        List<String> systemMessages = new ArrayList<>();
        systemMessages.add("&6=== Progression Status ===");
        systemMessages.add("&eJucator: &f" + player.getName());
        if (template != null) {
            systemMessages.addAll(buildQuestStatusMessages(
                template,
                progress,
                player,
                resolveQuestNpcName(progress)
            ));
        } else {
            systemMessages.add("&eProgresie: &f" + progress.templateId());
            systemMessages.add("&7Status: &f" + formatQuestStatus(progress.status()));
            if (!progress.currentPhase().isBlank()) {
                systemMessages.add("&7Faza curenta: &f" + formatQuestPhase(progress.currentPhase()));
            }
            systemMessages.add("&cTemplate-ul questului nu mai este disponibil in configuratia curenta.");
        }

        return QuestInteractionResult.handled(false, List.of(), systemMessages);
    }

    public QuestInteractionResult getQuestDebug(Player player, String questReference) {
        if (player == null || questReference == null || questReference.isBlank()) {
            return QuestInteractionResult.notHandled();
        }

        UUID playerId = player.getUniqueId();
        PlayerQuestProgress progress = isTrackedQuestSelector(questReference)
            ? getTrackedQuestProgress(playerId, true)
            : findQuestProgressByReference(playerId, questReference, true);
        if (progress == null) {
            return QuestInteractionResult.notHandled();
        }

        ScenarioTemplate template = resolveTemplateForProgress(progress, null);
        if (template != null && progress.isCurrent()) {
            progress = refreshTrackedQuestProgress(player, template, progress);
        }

        List<String> systemMessages = new ArrayList<>();
        systemMessages.add("&6=== Progression Debug ===");
        systemMessages.add("&eJucator: &f" + player.getName() + " &7(" + playerId + ")");
        systemMessages.add("&eSelector: &f" + questReference);
        systemMessages.add("&eTemplate: &f" + progress.templateId());
        systemMessages.add("&eCod: &f" + formatOptional(progress.questCode()));
        systemMessages.add("&7Status: &f" + formatQuestStatus(progress.status()));
        systemMessages.add("&7Tracked: &f" + (isTrackedQuest(playerId, progress) ? "da" : "nu"));
        systemMessages.add("&7Faza: &f" + formatOptional(progress.currentPhase()));
        systemMessages.add("&7Started/Completed/Updated: &f"
            + formatQuestDebugTime(progress.startedAt()) + " / "
            + formatQuestDebugTime(progress.completedAt()) + " / "
            + formatQuestDebugTime(progress.updatedAt()));

        if (template != null) {
            systemMessages.add("&eTitlu: &f" + resolveQuestTitle(template));
            if (!template.getProgressionMechanicId().isBlank()) {
                systemMessages.add("&7Progression: &f" + template.getProgressionMechanicId()
                    + " &7/ kind=&f" + formatOptional(template.getProgressionKind())
                    + " &7/ label=&f" + formatOptional(template.getProgressionLabel()));
            }
            systemMessages.add("&7Giver profession: &f" + formatOptional(template.getQuestGiverProfession()));
            QuestScenarioContract contract = template.getQuestContract();
            if (contract != null) {
                systemMessages.add("&7Contract: &f" + contract.displayName()
                    + " &7/ categorie=&f" + contract.categoryDisplayName()
                    + " &7/ acceptare=&f" + contract.acceptanceMode().name().toLowerCase(Locale.ROOT));
            }
            systemMessages.add("&eObiective template:");
            List<FeaturePackLoader.QuestEntryDefinition> objectives = template.getObjectives();
            if (objectives.isEmpty()) {
                systemMessages.add("&7- &f<gol>");
            } else {
                for (int index = 0; index < objectives.size(); index++) {
                    FeaturePackLoader.QuestEntryDefinition objective = objectives.get(index);
                    String objectiveKey = buildObjectiveKey(objective, index);
                    int current = progress.isCurrent()
                        ? resolveObjectiveCurrentProgress(player, objective, progress, index)
                        : readObjectiveProgress(progress.objectiveProgress(), objective, index);
                    boolean activeForStage = shouldShowObjectiveForCurrentStage(template, progress, objective);
                    QuestObjectiveState state = resolveQuestObjectiveState(
                        progress,
                        current,
                        objective.getAmount(),
                        activeForStage
                    );
                    systemMessages.add("&7- &f" + objectiveKey
                        + " &7type=&f" + normalizeObjectiveType(objective.getType())
                        + " &7stage=&f" + formatOptional(canonicalQuestPhase(template, getObjectiveStage(objective)))
                        + " &7target=&f" + formatOptional(objective.getItemId())
                        + " &7state=&f" + state.displayName()
                        + " &7progress=&f" + Math.min(current, Math.max(1, objective.getAmount()))
                        + "/" + Math.max(1, objective.getAmount()));
                }
            }
        } else {
            systemMessages.add("&cTemplate-ul questului nu mai este disponibil in configuratia curenta.");
        }

        systemMessages.add("&eObjective progress:");
        systemMessages.addAll(formatQuestDebugMap(progress.objectiveProgress(), 20));
        systemMessages.add("&eQuest variables:");
        systemMessages.addAll(formatQuestDebugMap(progress.questVariables(), 30));

        return QuestInteractionResult.handled(false, List.of(), systemMessages);
    }

    public QuestInteractionResult getQuestProgress(Player player, String questReference) {
        if (player == null) {
            return QuestInteractionResult.notHandled();
        }

        UUID playerId = player.getUniqueId();
        PlayerQuestProgress progress = selectQuestProgressForProgress(playerId, questReference);
        List<String> systemMessages = new ArrayList<>();
        systemMessages.add("&6=== Progression Progress ===");
        systemMessages.add("&eJucator: &f" + player.getName());

        if (progress == null) {
            if (questReference != null && !questReference.isBlank()) {
                systemMessages.add("&cNu exista progres pentru selectorul: &f" + questReference);
            } else {
                systemMessages.add("&7Nu ai quest curent cu progres.");
            }
            return QuestInteractionResult.handled(false, List.of(), systemMessages);
        }

        ScenarioTemplate template = resolveTemplateForProgress(progress, null);
        if (template != null && progress.isCurrent()) {
            progress = refreshTrackedQuestProgress(player, template, progress);
        }

        systemMessages.add("&eTemplate: &f" + progress.templateId());
        systemMessages.add("&eCod: &f" + formatOptional(progress.questCode()));
        systemMessages.add("&7Status: &f" + formatQuestStatus(progress.status()));
        systemMessages.add("&7Tracked: &f" + (isTrackedQuest(playerId, progress) ? "da" : "nu"));
        if (!progress.currentPhase().isBlank()) {
            systemMessages.add("&7Faza curenta: &f" + formatQuestPhase(progress.currentPhase()));
        }

        if (template == null) {
            systemMessages.add("&cTemplate-ul progresiei nu mai este disponibil in configuratia curenta.");
            systemMessages.add("&eObjective progress brut:");
            systemMessages.addAll(formatQuestDebugMap(progress.objectiveProgress(), 20));
            return QuestInteractionResult.handled(false, List.of(), systemMessages);
        }

        systemMessages.add("&e" + capitalizeProgressionLabel(resolveProgressionSingularLabel(plugin.getFeaturePackLoader(), template)) + ": &f" + resolveQuestTitle(template));
        if (!template.getProgressionMechanicId().isBlank()) {
            systemMessages.add("&7Mecanica: &f" + template.getProgressionMechanicId()
                + (template.getProgressionLabel().isBlank() ? "" : " &7(" + template.getProgressionLabel() + ")"));
        }
        if (template.getObjectives().isEmpty()) {
            systemMessages.add("&7Nu exista obiective in template.");
        } else {
            systemMessages.add("&eObiective:");
            systemMessages.addAll(buildQuestProgressDetailLines(player, template, progress));
        }

        return QuestInteractionResult.handled(false, List.of(), systemMessages);
    }

    public QuestInteractionResult getQuestTrack(Player player) {
        return getQuestTrack(player, "");
    }

    public QuestInteractionResult getQuestTrack(Player player, String questReference) {
        if (player == null) {
            return QuestInteractionResult.notHandled();
        }

        UUID playerId = player.getUniqueId();
        PlayerQuestProgress currentProgress = selectQuestProgressForTracking(playerId, questReference);
        List<String> systemMessages = new ArrayList<>();
        systemMessages.add("&6=== Quest Track ===");
        systemMessages.add("&eJucator: &f" + player.getName());

        if (currentProgress == null || !currentProgress.isCurrent()) {
            if (questReference != null && !questReference.isBlank()) {
                systemMessages.add("&cNu exista quest curent pentru selectorul: &f" + questReference);
            } else {
                systemMessages.add("&7Nu ai quest activ de urmarit.");
            }
            return QuestInteractionResult.handled(false, List.of(), systemMessages);
        }

        ScenarioTemplate template = resolveTemplateForProgress(currentProgress, null);
        if (template == null) {
            systemMessages.add("&eQuest: &f" + currentProgress.templateId());
            systemMessages.add("&7Status: &f" + formatQuestStatus(currentProgress.status()));
            if (!currentProgress.currentPhase().isBlank()) {
                systemMessages.add("&7Faza curenta: &f" + formatQuestPhase(currentProgress.currentPhase()));
            }
            systemMessages.add("&cTemplate-ul questului nu mai este disponibil in configuratia curenta.");
            return QuestInteractionResult.handled(false, List.of(), systemMessages);
        }

        PlayerQuestProgress refreshedProgress = refreshTrackedQuestProgress(player, template, currentProgress);
        systemMessages.add("&eQuest: &f" + resolveQuestTitle(template));
        systemMessages.add("&7Status: &f" + formatQuestStatus(refreshedProgress.status()));
        if (!refreshedProgress.currentPhase().isBlank()) {
            systemMessages.add("&7Faza curenta: &f" + formatQuestPhase(refreshedProgress.currentPhase()));
        }

        if (refreshedProgress.isOffered()) {
            systemMessages.add("&7Misiunea este oferita, dar trebuie acceptata inainte sa fie urmarita.");
            return QuestInteractionResult.handled(false, List.of(), systemMessages);
        }

        if (!refreshedProgress.isActive()) {
            systemMessages.add("&7Questul nu este activ.");
            return QuestInteractionResult.handled(false, List.of(), systemMessages);
        }

        QuestObjectiveCheck objectiveCheck = inspectQuestObjectives(player, template, refreshedProgress, null, false);
        if (objectiveCheck.complete()) {
            systemMessages.add("&aObiectivele sunt complete. Revino la NPC pentru finalizare.");
            String questGiverHint = describeQuestGiverTrackingTarget(refreshedProgress, player);
            if (!questGiverHint.isBlank()) {
                systemMessages.add("&bTinta: &f" + questGiverHint);
            }
            return QuestInteractionResult.handled(false, List.of(), systemMessages);
        }

        List<String> trackingLines = buildQuestTrackingLines(template, refreshedProgress, player);
        if (!trackingLines.isEmpty()) {
            systemMessages.add("&eUrmatorul pas:");
            systemMessages.addAll(trackingLines);
        } else {
            systemMessages.add("&eIti mai lipsesc:");
            for (String missingObjective : objectiveCheck.missingObjectives()) {
                systemMessages.add("&7- &f" + missingObjective);
            }
            systemMessages.add("&7Nu exista tinte de locatie salvate pentru obiectivele ramase.");
        }

        return QuestInteractionResult.handled(false, List.of(), systemMessages);
    }

    public QuestTrackingMarker getQuestTrackingMarker(Player player) {
        return getQuestTrackingMarker(player, "");
    }

    public QuestTrackingMarker getQuestTrackingMarker(Player player, String questReference) {
        if (player == null) {
            return null;
        }

        PlayerQuestProgress selectedProgress = selectQuestProgressForTracking(player.getUniqueId(), questReference);
        if (selectedProgress == null && questReference != null && !questReference.isBlank()) {
            return null;
        }

        List<PlayerQuestProgress> candidates = selectedProgress != null
            ? List.of(selectedProgress)
            : getCurrentQuestProgress(player.getUniqueId());
        for (PlayerQuestProgress currentProgress : candidates) {
            if (!currentProgress.isActive()) {
                continue;
            }

            ScenarioTemplate template = resolveTemplateForProgress(currentProgress, null);
            if (template == null) {
                continue;
            }

            PlayerQuestProgress refreshedProgress = refreshTrackedQuestProgress(player, template, currentProgress);
            QuestObjectiveCheck objectiveCheck = inspectQuestObjectives(player, template, refreshedProgress, null, false);
            if (objectiveCheck.complete()) {
                QuestTrackingTarget questGiverTarget = resolveQuestGiverTrackingTarget(refreshedProgress);
                QuestTrackingMarker marker = buildQuestTrackingMarker("finalizeaza questul", questGiverTarget, player);
                if (marker != null) {
                    return marker;
                }
                continue;
            }

            QuestTrackingStep trackingStep = resolveNextQuestTrackingStep(template, refreshedProgress, player);
            if (trackingStep == null) {
                continue;
            }

            QuestTrackingMarker marker = buildQuestTrackingMarker(trackingStep.objectiveLabel(), trackingStep.target(), player);
            if (marker != null) {
                return marker;
            }
        }

        return null;
    }

    public QuestTrackingMarker startQuestTracking(Player player) {
        return startQuestTracking(player, "");
    }

    public QuestTrackingMarker startQuestTracking(Player player, String questReference) {
        PlayerQuestProgress selectedProgress = player != null
            ? selectQuestProgressForTracking(player.getUniqueId(), questReference)
            : null;
        QuestTrackingMarker marker = getQuestTrackingMarker(player, questReference);
        if (player == null || marker == null || !marker.hasLocation()) {
            return marker;
        }

        trackedQuestPlayers.add(player.getUniqueId());
        if (selectedProgress != null && selectedProgress.templateId() != null && !selectedProgress.templateId().isBlank()) {
            trackedQuestTemplates.put(player.getUniqueId(), selectedProgress.templateId());
            persistQuestTrackingPreferenceAsync(player.getUniqueId(), selectedProgress);
        }
        return marker;
    }

    public boolean stopQuestTracking(Player player) {
        if (player == null) {
            return false;
        }

        boolean hadTemplate = trackedQuestTemplates.remove(player.getUniqueId()) != null;
        boolean stopped = trackedQuestPlayers.remove(player.getUniqueId());
        if (stopped || hadTemplate) {
            persistQuestTrackingPreferenceAsync(player.getUniqueId(), "");
        }
        return stopped || hadTemplate;
    }

    public void stopAllQuestTracking() {
        trackedQuestPlayers.clear();
        trackedQuestTemplates.clear();
    }

    public boolean isQuestTracking(Player player) {
        return player != null && trackedQuestPlayers.contains(player.getUniqueId());
    }

    public int tickQuestTrackingMarkers() {
        int updated = 0;
        Iterator<UUID> iterator = trackedQuestPlayers.iterator();
        while (iterator.hasNext()) {
            UUID playerId = iterator.next();
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                iterator.remove();
                continue;
            }

            QuestTrackingMarker marker = getQuestTrackingMarker(player, trackedQuestTemplates.getOrDefault(playerId, ""));
            if (marker == null || !marker.hasLocation()) {
                iterator.remove();
                trackedQuestTemplates.remove(playerId);
                persistQuestTrackingPreferenceAsync(playerId, "");
                plugin.getMessageUtils().sendActionBar(player, "&cQuest tracking oprit &8| &7nu mai exista tinta activa");
                continue;
            }

            applyQuestTrackingMarker(player, marker);
            updated++;
        }
        return updated;
    }

    public boolean applyQuestTrackingMarker(Player player, QuestTrackingMarker marker) {
        if (player == null || marker == null || !marker.hasLocation()) {
            return false;
        }

        Location targetLocation = marker.location();
        plugin.getMessageUtils().sendActionBar(player, marker.actionBarMessage());
        spawnQuestTrackingParticles(player, marker);
        if (targetLocation.getWorld().equals(player.getWorld())) {
            player.setCompassTarget(targetLocation);
            return true;
        }
        return false;
    }

    private void spawnQuestTrackingParticles(Player player, QuestTrackingMarker marker) {
        if (!plugin.getConfig().getBoolean("quest.tracking_particles", true)) {
            return;
        }
        if (player == null || marker == null || !marker.hasLocation()) {
            return;
        }

        Location targetLocation = marker.location();
        if (!targetLocation.getWorld().equals(player.getWorld())) {
            return;
        }

        double distance = targetLocation.distance(player.getLocation());
        spawnQuestDirectionParticles(player, targetLocation, distance);

        double waypointRange = Math.max(12.0, plugin.getConfig().getDouble("quest.tracking_particle_range", 96.0));
        if (distance <= waypointRange) {
            spawnQuestWaypointParticles(player, targetLocation);
        }
    }

    private void spawnQuestDirectionParticles(Player player, Location targetLocation, double distance) {
        if (distance < 3.0) {
            return;
        }

        Location playerLocation = player.getLocation();
        double dx = targetLocation.getX() - playerLocation.getX();
        double dz = targetLocation.getZ() - playerLocation.getZ();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        if (horizontalDistance < 0.5) {
            return;
        }

        double unitX = dx / horizontalDistance;
        double unitZ = dz / horizontalDistance;
        Location base = playerLocation.clone().add(0.0, 1.15, 0.0);
        int particleCount = Math.min(7, Math.max(3, (int) Math.round(distance / 18.0)));
        for (int index = 1; index <= particleCount; index++) {
            double offset = 0.65 * index;
            Location particleLocation = base.clone().add(unitX * offset, 0.0, unitZ * offset);
            player.spawnParticle(Particle.END_ROD, particleLocation, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    private void spawnQuestWaypointParticles(Player player, Location targetLocation) {
        Location base = targetLocation.clone().add(0.0, 0.2, 0.0);
        for (int index = 0; index < 12; index++) {
            double angle = (Math.PI * 2.0 * index) / 12.0;
            Location ringLocation = base.clone().add(Math.cos(angle) * 0.85, 0.0, Math.sin(angle) * 0.85);
            player.spawnParticle(Particle.END_ROD, ringLocation, 1, 0.0, 0.02, 0.0, 0.0);
        }

        player.spawnParticle(
            Particle.ENCHANT,
            targetLocation.clone().add(0.0, 1.15, 0.0),
            18,
            0.45,
            0.75,
            0.45,
            0.08
        );
    }

    public void recordNpcConversation(Player player, AINPC npc) {
        if (player == null || npc == null) {
            return;
        }

        for (PlayerQuestProgress progress : getCurrentQuestProgress(player.getUniqueId())) {
            if (!progress.isCurrent()) {
                continue;
            }

            ScenarioTemplate template = resolveTemplateForProgress(progress, npc);
            if (template == null) {
                continue;
            }

            PlayerQuestProgress refreshedProgress = refreshTrackedQuestProgress(player, template, progress);
            trackNpcObjectiveProgress(player, npc, template, refreshedProgress);
        }
    }

    public void recordRegionVisit(Player player) {
        if (player == null) {
            return;
        }

        Location location = player.getLocation();
        WorldRegion region = findCurrentRegion(location);
        WorldPlace place = findCurrentPlace(location);
        WorldNode node = findCurrentNode(location);
        if (region == null && place == null && node == null) {
            return;
        }

        for (PlayerQuestProgress progress : getCurrentQuestProgress(player.getUniqueId())) {
            if (!progress.isCurrent()) {
                continue;
            }

            ScenarioTemplate template = resolveTemplateForProgress(progress, null);
            if (template == null
                || (!hasObjectiveType(template, "visit_region")
                    && !hasObjectiveType(template, "visit_place")
                    && !hasObjectiveType(template, "inspect_node"))) {
                continue;
            }

            Map<String, Integer> updatedProgress = new LinkedHashMap<>(progress.objectiveProgress());
            boolean changed = false;
            List<FeaturePackLoader.QuestEntryDefinition> objectives = template.getObjectives();
            for (int index = 0; index < objectives.size(); index++) {
                FeaturePackLoader.QuestEntryDefinition objective = objectives.get(index);
                if (!isObjectiveActiveForProgress(template, progress, objective)) {
                    continue;
                }
                String objectiveKey = buildObjectiveKey(objective, index);
                changed |= carryLegacyObjectiveProgress(updatedProgress, objective, index);
                boolean matchesLocationObjective =
                    (matchesObjectiveType(objective, "visit_region") && matchesRegionObjective(progress, objective, index, region))
                        || (matchesObjectiveType(objective, "visit_place") && matchesPlaceObjective(progress, objective, index, place))
                        || (matchesObjectiveType(objective, "inspect_node") && matchesNodeObjective(progress, objective, index, node));
                if (!matchesLocationObjective) {
                    continue;
                }

                changed |= incrementObjectiveProgress(updatedProgress, objectiveKey, objective.getAmount());
            }

            if (changed) {
                updateTrackedQuestProgress(player.getUniqueId(), template, progress, updatedProgress);
            }
        }
    }

    public void recordMobKill(Player player, Entity entity) {
        if (player == null || entity == null) {
            return;
        }

        for (PlayerQuestProgress progress : getCurrentQuestProgress(player.getUniqueId())) {
            if (!progress.isCurrent()) {
                continue;
            }

            ScenarioTemplate template = resolveTemplateForProgress(progress, null);
            if (template == null || !hasObjectiveType(template, "kill_mob")) {
                continue;
            }

            Map<String, Integer> updatedProgress = new LinkedHashMap<>(progress.objectiveProgress());
            boolean changed = false;
            List<FeaturePackLoader.QuestEntryDefinition> objectives = template.getObjectives();
            for (int index = 0; index < objectives.size(); index++) {
                FeaturePackLoader.QuestEntryDefinition objective = objectives.get(index);
                if (!isObjectiveActiveForProgress(template, progress, objective)) {
                    continue;
                }
                if (!matchesObjectiveType(objective, "kill_mob") || !matchesMobObjective(objective, entity)) {
                    continue;
                }

                String objectiveKey = buildObjectiveKey(objective, index);
                changed |= carryLegacyObjectiveProgress(updatedProgress, objective, index);
                changed |= incrementObjectiveProgress(updatedProgress, objectiveKey, objective.getAmount());
            }

            if (changed) {
                updateTrackedQuestProgress(player.getUniqueId(), template, progress, updatedProgress);
            }
        }
    }

    public void recordInventoryChange(Player player) {
        if (player == null) {
            return;
        }

        for (PlayerQuestProgress progress : getCurrentQuestProgress(player.getUniqueId())) {
            if (!progress.isCurrent()) {
                continue;
            }

            ScenarioTemplate template = resolveTemplateForProgress(progress, null);
            if (template == null || !hasInventoryObjective(template)) {
                continue;
            }

            refreshTrackedQuestProgress(player, template, progress);
        }
    }

    private PlayerQuestProgress setOfferedQuestProgress(UUID playerId, Player player, ScenarioTemplate template) {
        return setCurrentQuestProgress(playerId, player, template, QuestStatus.OFFERED);
    }

    private PlayerQuestProgress setActiveQuestProgress(UUID playerId, Player player, ScenarioTemplate template) {
        return setCurrentQuestProgress(playerId, player, template, QuestStatus.ACTIVE);
    }

    private PlayerQuestProgress setInitialQuestProgress(UUID playerId, Player player, ScenarioTemplate template) {
        return shouldAutoAcceptOnOffer(template)
            ? setActiveQuestProgress(playerId, player, template)
            : setOfferedQuestProgress(playerId, player, template);
    }

    private PlayerQuestProgress setCurrentQuestProgress(UUID playerId,
                                                        Player player,
                                                        ScenarioTemplate template,
                                                        QuestStatus status) {
        long now = System.currentTimeMillis();
        PlayerQuestProgress matchingProgress = getCurrentQuestProgress(playerId, template.getTemplateId());
        long startedAt = matchingProgress != null ? matchingProgress.startedAt() : now;
        String currentPhase = resolveQuestPhase(template, status, matchingProgress);

        Map<String, Integer> objectiveSnapshot = buildObjectiveProgressSnapshot(
            player != null ? player.getInventory() : null,
            template,
            matchingProgress != null ? matchingProgress.objectiveProgress() : Map.of(),
            currentPhase
        );
        Map<String, String> questVariables = seedQuestStageVariables(
            template,
            status,
            currentPhase,
            matchingProgress != null ? matchingProgress.questVariables() : Map.of(),
            now
        );

        PlayerQuestProgress currentProgress = new PlayerQuestProgress(
            template.getTemplateId(),
            template.getQuestCode(),
            status,
            startedAt,
            0L,
            now,
            currentPhase,
            objectiveSnapshot,
            questVariables
        );
        putActiveQuestProgress(playerId, currentProgress);
        removeArchivedQuestProgress(playerId, template.getTemplateId());
        persistQuestProgressAsync(playerId, currentProgress);
        return currentProgress;
    }

    private void markQuestCompleted(UUID playerId, ScenarioTemplate template) {
        long now = System.currentTimeMillis();
        PlayerQuestProgress activeProgress = getCurrentQuestProgress(playerId, template.getTemplateId());
        long startedAt = activeProgress != null ? activeProgress.startedAt() : now;

        clearQuestTrackingIfMatches(playerId, template.getTemplateId());
        removeActiveQuestProgress(playerId, template.getTemplateId());
        PlayerQuestProgress completedProgress = new PlayerQuestProgress(
            template.getTemplateId(),
            template.getQuestCode(),
            QuestStatus.COMPLETED,
            startedAt,
            now,
            now,
            resolveQuestPhase(template, QuestStatus.COMPLETED, activeProgress),
            buildCompletedObjectiveProgress(template, activeProgress != null ? activeProgress.objectiveProgress() : Map.of()),
            activeProgress != null ? activeProgress.questVariables() : Map.of()
        );
        archiveQuestProgress(playerId, completedProgress);
        persistQuestProgressAsync(playerId, completedProgress);
    }

    private PlayerQuestProgress markQuestFailed(UUID playerId, ScenarioTemplate template) {
        long now = System.currentTimeMillis();
        PlayerQuestProgress activeProgress = getCurrentQuestProgress(playerId, template.getTemplateId());
        long startedAt = activeProgress != null ? activeProgress.startedAt() : now;

        clearQuestTrackingIfMatches(playerId, template.getTemplateId());
        removeActiveQuestProgress(playerId, template.getTemplateId());
        PlayerQuestProgress failedProgress = new PlayerQuestProgress(
            template.getTemplateId(),
            template.getQuestCode(),
            QuestStatus.FAILED,
            startedAt,
            now,
            now,
            resolveQuestPhase(template, QuestStatus.FAILED, activeProgress),
            activeProgress != null ? activeProgress.objectiveProgress() : buildObjectiveProgressSnapshot(null, template, Map.of()),
            activeProgress != null ? activeProgress.questVariables() : Map.of()
        );
        archiveQuestProgress(playerId, failedProgress);
        persistQuestProgressAsync(playerId, failedProgress);
        return failedProgress;
    }

    private PlayerQuestProgress getCurrentQuestProgress(UUID playerId, String templateId) {
        if (playerId == null || templateId == null || templateId.isBlank()) {
            return null;
        }

        Map<String, PlayerQuestProgress> currentQuests = activePlayerQuests.get(playerId);
        if (currentQuests == null || currentQuests.isEmpty()) {
            return null;
        }
        return currentQuests.get(templateId);
    }

    private List<PlayerQuestProgress> getCurrentQuestProgress(UUID playerId) {
        Map<String, PlayerQuestProgress> currentQuests = playerId != null ? activePlayerQuests.get(playerId) : null;
        if (currentQuests == null || currentQuests.isEmpty()) {
            return List.of();
        }

        return currentQuests.values().stream()
            .filter(progress -> progress != null && progress.isCurrent())
            .sorted(Comparator
                .comparingLong(PlayerQuestProgress::updatedAt)
                .reversed()
                .thenComparing(progress -> progress.templateId() != null ? progress.templateId() : ""))
            .toList();
    }

    private PlayerQuestProgress selectQuestProgressForTracking(UUID playerId, String questReference) {
        if (questReference != null && !questReference.isBlank()) {
            return findQuestProgressByReference(playerId, questReference, false);
        }

        String trackedTemplateId = playerId != null ? trackedQuestTemplates.getOrDefault(playerId, "") : "";
        if (!trackedTemplateId.isBlank()) {
            PlayerQuestProgress trackedProgress = getCurrentQuestProgress(playerId, trackedTemplateId);
            if (trackedProgress != null && trackedProgress.isActive()) {
                return trackedProgress;
            }
            trackedQuestTemplates.remove(playerId);
        }

        List<PlayerQuestProgress> currentProgresses = getCurrentQuestProgress(playerId);
        for (PlayerQuestProgress progress : currentProgresses) {
            if (progress.isActive()) {
                return progress;
            }
        }
        return currentProgresses.isEmpty() ? null : currentProgresses.get(0);
    }

    private PlayerQuestProgress selectQuestProgressForProgress(UUID playerId, String questReference) {
        if (playerId == null) {
            return null;
        }

        if (questReference != null && !questReference.isBlank() && !isTrackedQuestSelector(questReference)) {
            return findQuestProgressByReference(playerId, questReference, true);
        }

        PlayerQuestProgress trackedProgress = getTrackedQuestProgress(playerId, true);
        if (trackedProgress != null) {
            return trackedProgress;
        }

        List<PlayerQuestProgress> currentProgresses = getCurrentQuestProgress(playerId);
        if (!currentProgresses.isEmpty()) {
            return currentProgresses.get(0);
        }

        return null;
    }

    private PlayerQuestProgress getTrackedQuestProgress(UUID playerId, boolean includeArchived) {
        if (playerId == null) {
            return null;
        }

        String trackedTemplateId = trackedQuestTemplates.getOrDefault(playerId, "");
        if (trackedTemplateId.isBlank()) {
            return null;
        }

        PlayerQuestProgress currentProgress = getCurrentQuestProgress(playerId, trackedTemplateId);
        if (currentProgress != null) {
            return currentProgress;
        }

        return includeArchived ? getArchivedQuestProgress(playerId, trackedTemplateId) : null;
    }

    private PlayerQuestProgress findQuestProgressByReference(UUID playerId,
                                                             String questReference,
                                                             boolean includeArchived) {
        if (playerId == null || questReference == null || questReference.isBlank()) {
            return null;
        }

        for (PlayerQuestProgress progress : getCurrentQuestProgress(playerId)) {
            if (matchesQuestReference(progress, questReference, resolveTemplateForProgress(progress, null))) {
                return progress;
            }
        }

        if (!includeArchived) {
            return null;
        }

        Map<String, PlayerQuestProgress> archivedQuests = archivedPlayerQuests.get(playerId);
        if (archivedQuests == null || archivedQuests.isEmpty()) {
            return null;
        }

        return archivedQuests.values().stream()
            .filter(progress -> matchesQuestReference(progress, questReference, resolveTemplateForProgress(progress, null)))
            .sorted(Comparator.comparingLong(PlayerQuestProgress::updatedAt).reversed())
            .findFirst()
            .orElse(null);
    }

    private boolean isTrackedQuest(UUID playerId, PlayerQuestProgress progress) {
        if (playerId == null || progress == null || progress.templateId() == null || progress.templateId().isBlank()) {
            return false;
        }

        String trackedTemplateId = trackedQuestTemplates.getOrDefault(playerId, "");
        return !trackedTemplateId.isBlank() && trackedTemplateId.equals(progress.templateId());
    }

    private void clearQuestTrackingIfMatches(UUID playerId, String templateId) {
        if (playerId == null || templateId == null || templateId.isBlank()) {
            return;
        }

        String trackedTemplateId = trackedQuestTemplates.getOrDefault(playerId, "");
        if (!templateId.equals(trackedTemplateId)) {
            return;
        }

        trackedQuestTemplates.remove(playerId);
        trackedQuestPlayers.remove(playerId);
        persistQuestTrackingPreferenceAsync(playerId, "");
    }

    private void putActiveQuestProgress(UUID playerId, PlayerQuestProgress progress) {
        if (playerId == null || progress == null || progress.templateId() == null || progress.templateId().isBlank()) {
            return;
        }

        activePlayerQuests
            .computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>())
            .put(progress.templateId(), progress);
    }

    private PlayerQuestProgress getArchivedQuestProgress(UUID playerId, String templateId) {
        Map<String, PlayerQuestProgress> archivedQuests = archivedPlayerQuests.get(playerId);
        if (archivedQuests == null || templateId == null || templateId.isBlank()) {
            return null;
        }
        return archivedQuests.get(templateId);
    }

    private PlayerQuestProgress getCompletedQuestProgress(UUID playerId, String templateId) {
        PlayerQuestProgress archivedProgress = getArchivedQuestProgress(playerId, templateId);
        if (archivedProgress == null || !archivedProgress.isCompleted()) {
            return null;
        }
        return archivedProgress;
    }

    private PlayerQuestProgress getFailedQuestProgress(UUID playerId, String templateId) {
        PlayerQuestProgress archivedProgress = getArchivedQuestProgress(playerId, templateId);
        if (archivedProgress == null || archivedProgress.status() != QuestStatus.FAILED) {
            return null;
        }
        return archivedProgress;
    }

    private QuestAvailability evaluateQuestAvailability(UUID playerId, ScenarioTemplate template) {
        if (playerId == null || template == null) {
            return QuestAvailability.unavailable(List.of("Quest invalid."));
        }

        List<String> issues = new ArrayList<>();
        PlayerQuestProgress completedProgress = getCompletedQuestProgress(playerId, template.getTemplateId());
        if (completedProgress != null) {
            if (!template.isQuestRepeatable()) {
                issues.add("Quest deja completat.");
            } else {
                long remainingCooldownMillis = remainingQuestCooldownMillis(template, completedProgress);
                if (remainingCooldownMillis > 0) {
                    issues.add("Quest repetabil disponibil peste " + formatDuration(remainingCooldownMillis) + ".");
                }
            }
        }

        for (String prerequisite : template.getQuestPrerequisites()) {
            if (prerequisite == null || prerequisite.isBlank()) {
                continue;
            }
            if (!hasCompletedQuest(playerId, prerequisite)) {
                issues.add("Trebuie completat mai intai: " + prerequisite + ".");
            }
        }

        QuestScenarioContract.Category category = resolveQuestCategory(template);
        int categoryLimit = getQuestCategoryLimit(category, plugin);
        int activeInCategory = countCurrentQuestsInCategory(playerId, category, template.getTemplateId());
        if (activeInCategory >= categoryLimit) {
            issues.add("Ai deja " + activeInCategory + " questuri curente din categoria "
                + category.displayName() + " (limita " + categoryLimit + ").");
        }

        int mechanicLimit = getProgressionMechanicLimit(template);
        if (mechanicLimit > 0) {
            int activeInMechanic = countCurrentProgressionsInMechanic(playerId, template, template.getTemplateId());
            if (activeInMechanic >= mechanicLimit) {
                issues.add("Ai deja " + activeInMechanic + " "
                    + resolveProgressionPluralLabel(plugin.getFeaturePackLoader(), template) + " curente in "
                    + resolveProgressionMechanicDisplay(plugin.getFeaturePackLoader(), template) + " (limita " + mechanicLimit + ").");
            }
        }

        return issues.isEmpty()
            ? QuestAvailability.allowed()
            : QuestAvailability.unavailable(issues);
    }


    private int getProgressionMechanicLimit(ScenarioTemplate template) {
        if (template == null || !template.isProgressionEnabled()
            || template.getProgressionMechanicId().isBlank()) {
            return 0;
        }

        if (template.getProgressionMaxActive() > 0) {
            return template.getProgressionMaxActive();
        }

        FeaturePackLoader.ProgressionMechanicDefinition mechanic = resolveProgressionMechanicDefinition(plugin.getFeaturePackLoader(), template);
        if (mechanic == null || !mechanic.isProgressEnabled()) {
            return 0;
        }
        return mechanic.getMaxActive();
    }

    private int countCurrentProgressionsInMechanic(UUID playerId,
                                                   ScenarioTemplate template,
                                                   String excludedTemplateId) {
        String mechanicKey = resolveProgressionMechanicKey(plugin.getFeaturePackLoader(), template);
        if (mechanicKey.isBlank()) {
            return 0;
        }

        int count = 0;
        for (PlayerQuestProgress progress : getCurrentQuestProgress(playerId)) {
            if (progress.templateId() != null && progress.templateId().equals(excludedTemplateId)) {
                continue;
            }

            ScenarioTemplate t = resolveTemplateForProgress(progress, null);
            if (mechanicKey.equals(resolveProgressionMechanicKey(plugin.getFeaturePackLoader(), t))) {
                count++;
            }
        }
        return count;
    }

    

    private int countCurrentQuestsInCategory(UUID playerId,
                                             QuestScenarioContract.Category category,
                                             String excludedTemplateId) {
        int count = 0;
        for (PlayerQuestProgress progress : getCurrentQuestProgress(playerId)) {
            if (progress.templateId() != null && progress.templateId().equals(excludedTemplateId)) {
                continue;
            }

            ScenarioTemplate activeTemplate = resolveTemplateForProgress(progress, null);
            if (activeTemplate != null && resolveQuestCategory(activeTemplate) == category) {
                count++;
            }
        }
        return count;
    }

    private boolean hasCompletedQuest(UUID playerId, String questReference) {
        Map<String, PlayerQuestProgress> archivedQuests = archivedPlayerQuests.get(playerId);
        if (archivedQuests == null || archivedQuests.isEmpty()) {
            return false;
        }

        return archivedQuests.values().stream()
            .filter(PlayerQuestProgress::isCompleted)
            .anyMatch(progress -> matchesQuestReference(progress, questReference, resolveTemplateForProgress(progress, null)));
    }

    private boolean questLogMatches(UUID playerId,
                                    PlayerQuestProgress progress,
                                    QuestLogFilter filter,
                                    boolean archived) {
        if (progress == null) {
            return false;
        }

        QuestLogFilter effectiveFilter = filter != null ? filter : QuestLogFilter.SUMMARY;
        return switch (effectiveFilter) {
            case SUMMARY, ALL -> true;
            case CURRENT -> !archived && progress.isCurrent();
            case ACTIVE -> !archived && progress.isActive();
            case OFFERED -> !archived && progress.isOffered();
            case TRACKED -> !archived && isTrackedQuest(playerId, progress);
            case COMPLETED -> progress.isCompleted();
            case FAILED -> progress.status() == QuestStatus.FAILED;
            case ARCHIVED -> archived || progress.status().isArchived();
            case QUEST_KIND -> {
                ScenarioTemplate template = resolveTemplateForProgress(progress, null);
                yield template != null && progressionKindMatches(plugin.getFeaturePackLoader(), template, "quest");
            }
            case CONTRACT_KIND -> {
                ScenarioTemplate template = resolveTemplateForProgress(progress, null);
                yield template != null && progressionKindMatches(plugin.getFeaturePackLoader(), template, "contract");
            }
            case DUTY_KIND -> {
                ScenarioTemplate template = resolveTemplateForProgress(progress, null);
                yield template != null && progressionKindMatches(plugin.getFeaturePackLoader(), template, "duty");
            }
            case BOUNTY_KIND -> {
                ScenarioTemplate template = resolveTemplateForProgress(progress, null);
                yield template != null && progressionKindMatches(plugin.getFeaturePackLoader(), template, "bounty");
            }
            case EVENT_KIND -> {
                ScenarioTemplate template = resolveTemplateForProgress(progress, null);
                yield template != null && progressionKindMatches(plugin.getFeaturePackLoader(), template, "event");
            }
            case TUTORIAL_KIND -> {
                ScenarioTemplate template = resolveTemplateForProgress(progress, null);
                yield template != null && progressionKindMatches(plugin.getFeaturePackLoader(), template, "tutorial");
            }
            case RITUAL_KIND -> {
                ScenarioTemplate template = resolveTemplateForProgress(progress, null);
                yield template != null && progressionKindMatches(plugin.getFeaturePackLoader(), template, "ritual");
            }
            case CONTRACT_CURRENT -> !archived && progress.isCurrent() && questLogMatchesProgressionKind(progress, "contract");
            case CONTRACT_ACTIVE -> !archived && progress.isActive() && questLogMatchesProgressionKind(progress, "contract");
            case CONTRACT_OFFERED -> !archived && progress.isOffered() && questLogMatchesProgressionKind(progress, "contract");
            case CONTRACT_TRACKED -> !archived && isTrackedQuest(playerId, progress) && questLogMatchesProgressionKind(progress, "contract");
            case CONTRACT_COMPLETED -> progress.isCompleted() && questLogMatchesProgressionKind(progress, "contract");
            case CONTRACT_FAILED -> progress.status() == QuestStatus.FAILED && questLogMatchesProgressionKind(progress, "contract");
            case CONTRACT_ARCHIVED -> (archived || progress.status().isArchived()) && questLogMatchesProgressionKind(progress, "contract");
            case DUTY_CURRENT -> !archived && progress.isCurrent() && questLogMatchesProgressionKind(progress, "duty");
            case DUTY_ACTIVE -> !archived && progress.isActive() && questLogMatchesProgressionKind(progress, "duty");
            case DUTY_OFFERED -> !archived && progress.isOffered() && questLogMatchesProgressionKind(progress, "duty");
            case DUTY_TRACKED -> !archived && isTrackedQuest(playerId, progress) && questLogMatchesProgressionKind(progress, "duty");
            case DUTY_COMPLETED -> progress.isCompleted() && questLogMatchesProgressionKind(progress, "duty");
            case DUTY_FAILED -> progress.status() == QuestStatus.FAILED && questLogMatchesProgressionKind(progress, "duty");
            case DUTY_ARCHIVED -> (archived || progress.status().isArchived()) && questLogMatchesProgressionKind(progress, "duty");
            case BOUNTY_CURRENT -> !archived && progress.isCurrent() && questLogMatchesProgressionKind(progress, "bounty");
            case BOUNTY_ACTIVE -> !archived && progress.isActive() && questLogMatchesProgressionKind(progress, "bounty");
            case BOUNTY_OFFERED -> !archived && progress.isOffered() && questLogMatchesProgressionKind(progress, "bounty");
            case BOUNTY_TRACKED -> !archived && isTrackedQuest(playerId, progress) && questLogMatchesProgressionKind(progress, "bounty");
            case BOUNTY_COMPLETED -> progress.isCompleted() && questLogMatchesProgressionKind(progress, "bounty");
            case BOUNTY_FAILED -> progress.status() == QuestStatus.FAILED && questLogMatchesProgressionKind(progress, "bounty");
            case BOUNTY_ARCHIVED -> (archived || progress.status().isArchived()) && questLogMatchesProgressionKind(progress, "bounty");
            case EVENT_CURRENT -> !archived && progress.isCurrent() && questLogMatchesProgressionKind(progress, "event");
            case EVENT_ACTIVE -> !archived && progress.isActive() && questLogMatchesProgressionKind(progress, "event");
            case EVENT_OFFERED -> !archived && progress.isOffered() && questLogMatchesProgressionKind(progress, "event");
            case EVENT_TRACKED -> !archived && isTrackedQuest(playerId, progress) && questLogMatchesProgressionKind(progress, "event");
            case EVENT_COMPLETED -> progress.isCompleted() && questLogMatchesProgressionKind(progress, "event");
            case EVENT_FAILED -> progress.status() == QuestStatus.FAILED && questLogMatchesProgressionKind(progress, "event");
            case EVENT_ARCHIVED -> (archived || progress.status().isArchived()) && questLogMatchesProgressionKind(progress, "event");
            case TUTORIAL_CURRENT -> !archived && progress.isCurrent() && questLogMatchesProgressionKind(progress, "tutorial");
            case TUTORIAL_ACTIVE -> !archived && progress.isActive() && questLogMatchesProgressionKind(progress, "tutorial");
            case TUTORIAL_OFFERED -> !archived && progress.isOffered() && questLogMatchesProgressionKind(progress, "tutorial");
            case TUTORIAL_TRACKED -> !archived && isTrackedQuest(playerId, progress) && questLogMatchesProgressionKind(progress, "tutorial");
            case TUTORIAL_COMPLETED -> progress.isCompleted() && questLogMatchesProgressionKind(progress, "tutorial");
            case TUTORIAL_FAILED -> progress.status() == QuestStatus.FAILED && questLogMatchesProgressionKind(progress, "tutorial");
            case TUTORIAL_ARCHIVED -> (archived || progress.status().isArchived()) && questLogMatchesProgressionKind(progress, "tutorial");
            case RITUAL_CURRENT -> !archived && progress.isCurrent() && questLogMatchesProgressionKind(progress, "ritual");
            case RITUAL_ACTIVE -> !archived && progress.isActive() && questLogMatchesProgressionKind(progress, "ritual");
            case RITUAL_OFFERED -> !archived && progress.isOffered() && questLogMatchesProgressionKind(progress, "ritual");
            case RITUAL_TRACKED -> !archived && isTrackedQuest(playerId, progress) && questLogMatchesProgressionKind(progress, "ritual");
            case RITUAL_COMPLETED -> progress.isCompleted() && questLogMatchesProgressionKind(progress, "ritual");
            case RITUAL_FAILED -> progress.status() == QuestStatus.FAILED && questLogMatchesProgressionKind(progress, "ritual");
            case RITUAL_ARCHIVED -> (archived || progress.status().isArchived()) && questLogMatchesProgressionKind(progress, "ritual");
            case MAIN, SIDE, REPEATABLE -> {
                ScenarioTemplate template = resolveTemplateForProgress(progress, null);
                if (template == null) {
                    yield false;
                }
                QuestScenarioContract.Category category = resolveQuestCategory(template);
                yield switch (effectiveFilter) {
                    case MAIN -> category == QuestScenarioContract.Category.MAIN;
                    case SIDE -> category == QuestScenarioContract.Category.SIDE;
                    case REPEATABLE -> category == QuestScenarioContract.Category.REPEATABLE;
                    default -> false;
                };
            }
        };
    }

    private boolean questLogMatchesProgressionKind(PlayerQuestProgress progress, String expectedKind) {
        ScenarioTemplate template = resolveTemplateForProgress(progress, null);
        return template != null && progressionKindMatches(plugin.getFeaturePackLoader(), template, expectedKind);
    }

    private List<String> buildQuestLogSummaryLines(UUID playerId, List<PlayerQuestProgress> currentProgresses) {
        EnumMap<QuestScenarioContract.Category, Integer> categoryCounts =
            new EnumMap<>(QuestScenarioContract.Category.class);
        Map<String, Integer> mechanicCounts = new LinkedHashMap<>();
        int activeCount = 0;
        int offeredCount = 0;
        int trackedCount = 0;
        int missingTemplateCount = 0;

        for (PlayerQuestProgress progress : currentProgresses) {
            if (progress == null) {
                continue;
            }
            if (progress.isActive()) {
                activeCount++;
            } else if (progress.isOffered()) {
                offeredCount++;
            }
            if (isTrackedQuest(playerId, progress)) {
                trackedCount++;
            }

            ScenarioTemplate template = resolveTemplateForProgress(progress, null);
            if (template == null) {
                missingTemplateCount++;
                continue;
            }
            QuestScenarioContract.Category category = resolveQuestCategory(template);
            categoryCounts.merge(category, 1, Integer::sum);
            mechanicCounts.merge(resolveProgressionMechanicDisplay(plugin.getFeaturePackLoader(), template), 1, Integer::sum);
        }

        List<String> lines = new ArrayList<>();
        lines.add("&7Status curent: &factive=" + activeCount
            + "&7, oferite=&f" + offeredCount
            + "&7, tracked=&f" + trackedCount);
        lines.add("&7Categorii: &fprincipal="
            + categoryCounts.getOrDefault(QuestScenarioContract.Category.MAIN, 0)
            + "&7, secundar=&f"
            + categoryCounts.getOrDefault(QuestScenarioContract.Category.SIDE, 0)
            + "&7, repetabil=&f"
            + categoryCounts.getOrDefault(QuestScenarioContract.Category.REPEATABLE, 0));
        if (!mechanicCounts.isEmpty()) {
            lines.add("&7Mecanici: &f" + formatQuestLogMechanicCounts(mechanicCounts));
        }
        if (missingTemplateCount > 0) {
            lines.add("&eTemplates lipsa in log: &f" + missingTemplateCount);
        }
        return lines;
    }

    private Comparator<PlayerQuestProgress> questLogCurrentComparator(UUID playerId) {
        return Comparator
            .comparingInt((PlayerQuestProgress progress) -> isTrackedQuest(playerId, progress) ? 0 : 1)
            .thenComparingInt(progress -> questLogCategoryPriority(resolveTemplateForProgress(progress, null)))
            .thenComparing(progress -> resolveProgressionMechanicSortKey(plugin.getFeaturePackLoader(), resolveTemplateForProgress(progress, null)))
            .thenComparingInt(QuestLogFilterKt::questLogStatusPriority)
            .thenComparing(Comparator.comparingLong(PlayerQuestProgress::updatedAt).reversed())
            .thenComparing(progress -> progress.templateId() != null ? progress.templateId() : "");
    }

    private String questLogCurrentGroupLabel(UUID playerId, ScenarioTemplate template, PlayerQuestProgress progress) {
        if (isTrackedQuest(playerId, progress)) {
            if (template == null) {
                return "&b--- Progresie urmarita ---";
            }
            return "&b--- " + capitalizeProgressionLabel(resolveProgressionSingularLabel(plugin.getFeaturePackLoader(), template)) + " urmarit ---";
        }
        if (template == null) {
            return "&e--- Template lipsa ---";
        }
        return "&e--- " + resolveProgressionMechanicDisplay(plugin.getFeaturePackLoader(), template) + " ---";
    }

    private String formatQuestLogArchivedLine(UUID playerId,
                                              ScenarioTemplate template,
                                              PlayerQuestProgress progress,
                                              String title) {
        StringBuilder line = new StringBuilder("&7- &f")
            .append(title)
            .append(" &7(")
            .append(formatQuestStatus(progress.status()))
            .append(")");
        if (template != null) {
            line.append(" &8[")
                .append(resolveProgressionMechanicDisplay(plugin.getFeaturePackLoader(), template))
                .append(" / ")
                .append(resolveQuestCategory(template).displayName())
                .append("]");
        }
        if (isTrackedQuest(playerId, progress)) {
            line.append(" &btracked");
        }
        return line.toString();
    }

    private List<String> buildQuestLogActionLines(Player player,
                                                  UUID playerId,
                                                  ScenarioTemplate template,
                                                  PlayerQuestProgress progress,
                                                  boolean adminView) {
        String selector = questLogActionSelector(template, progress);
        if (selector.isBlank()) {
            return List.of();
        }

        String targetSuffix = adminView && player != null ? " " + player.getName() : "";
        List<String> commands = new ArrayList<>();
        commands.add("/ainpc quest status " + selector + targetSuffix);
        commands.add("/ainpc quest progress " + selector + targetSuffix);
        if (progress != null && progress.isActive()) {
            if (isTrackedQuest(playerId, progress)) {
                commands.add("/ainpc quest track stop" + targetSuffix);
            } else {
                commands.add("/ainpc quest track start " + selector + targetSuffix);
            }
            commands.add("/ainpc quest abandon " + selector + targetSuffix);
        }
        if (adminView) {
            commands.add("/ainpc quest debug " + selector + targetSuffix);
        }

        return List.of("&8Actiuni: &7" + String.join(" &8| &7", commands));
    }

    private List<PlayerQuestProgress> getRecentArchivedQuestProgress(UUID playerId, int limit) {
        Map<String, PlayerQuestProgress> archivedQuests = archivedPlayerQuests.get(playerId);
        if (archivedQuests == null || archivedQuests.isEmpty() || limit <= 0) {
            return List.of();
        }

        return archivedQuests.values().stream()
            .sorted(Comparator.comparingLong(PlayerQuestProgress::updatedAt).reversed())
            .limit(limit)
            .toList();
    }

    private List<PlayerQuestProgress> getArchivedQuestProgress(UUID playerId) {
        Map<String, PlayerQuestProgress> archivedQuests = archivedPlayerQuests.get(playerId);
        if (archivedQuests == null || archivedQuests.isEmpty()) {
            return List.of();
        }

        return archivedQuests.values().stream()
            .sorted(Comparator.comparingLong(PlayerQuestProgress::updatedAt).reversed())
            .toList();
    }

    private void archiveQuestProgress(UUID playerId, PlayerQuestProgress progress) {
        if (playerId == null || progress == null || !progress.status().isArchived()) {
            return;
        }

        archivedPlayerQuests.computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>()).put(
            progress.templateId(),
            progress
        );
    }

    private boolean removeActiveQuestProgress(UUID playerId, String templateId) {
        Map<String, PlayerQuestProgress> currentQuests = activePlayerQuests.get(playerId);
        if (currentQuests == null || templateId == null || templateId.isBlank()) {
            return false;
        }

        boolean removed = currentQuests.remove(templateId) != null;
        if (removed && currentQuests.isEmpty()) {
            activePlayerQuests.remove(playerId);
        }
        return removed;
    }

    private boolean removeArchivedQuestProgress(UUID playerId, String templateId) {
        Map<String, PlayerQuestProgress> archivedQuests = archivedPlayerQuests.get(playerId);
        if (archivedQuests == null || templateId == null || templateId.isBlank()) {
            return false;
        }

        boolean removed = archivedQuests.remove(templateId) != null;
        if (removed && archivedQuests.isEmpty()) {
            archivedPlayerQuests.remove(playerId);
        }
        return removed;
    }

    private void loadPersistedQuestProgress() {
        activePlayerQuests.clear();
        archivedPlayerQuests.clear();
        trackedQuestPlayers.clear();
        trackedQuestTemplates.clear();

        String sql = """
            SELECT player_uuid, template_id, quest_code, status, started_at, completed_at,
                   current_phase, current_stage_id, objective_progress, quest_variables, updated_at, tracked
            FROM player_quests
        """;

        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            int activeCount = 0;
            int archivedCount = 0;

            while (rs.next()) {
                UUID playerId;
                try {
                    playerId = UUID.fromString(rs.getString("player_uuid"));
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("Ignor progres quest cu UUID invalid: " + rs.getString("player_uuid"));
                    continue;
                }

                QuestStatus status = QuestStatus.fromStorage(rs.getString("status"));
                if (status == QuestStatus.NOT_STARTED) {
                    continue;
                }

                String templateId = rs.getString("template_id");
                Map<String, String> questVariables = mergeStoredQuestAnchorVariables(
                    playerId,
                    templateId,
                    parseQuestVariables(readTextOrEmpty(rs, "quest_variables"))
                );

                PlayerQuestProgress progress = new PlayerQuestProgress(
                    templateId,
                    rs.getString("quest_code"),
                    status,
                    readNullableLong(rs, "started_at"),
                    readNullableLong(rs, "completed_at"),
                    rs.getLong("updated_at"),
                    firstNonBlank(readTextOrEmpty(rs, "current_stage_id"), readTextOrEmpty(rs, "current_phase")),
                    parseObjectiveProgress(readTextOrEmpty(rs, "objective_progress")),
                    questVariables
                );
                if (registerLoadedQuestProgress(playerId, progress)) {
                    if (progress.isCurrent()) {
                        activeCount++;
                        if (rs.getInt("tracked") != 0) {
                            registerLoadedTrackedQuest(playerId, progress);
                        }
                    } else {
                        archivedCount++;
                    }
                }
            }

            plugin.debug("[QuestEngine] Progres quest incarcat din DB: active=" + activeCount
                + " arhivate=" + archivedCount);
        } catch (SQLException e) {
            plugin.getLogger().warning("Nu am putut incarca progresul quest-urilor: " + e.getMessage());
        }
    }

    private boolean registerLoadedQuestProgress(UUID playerId, PlayerQuestProgress progress) {
        if (playerId == null || progress == null || progress.templateId() == null || progress.templateId().isBlank()) {
            return false;
        }

        if (progress.isCurrent()) {
            Map<String, PlayerQuestProgress> currentQuests =
                activePlayerQuests.computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>());
            PlayerQuestProgress existingProgress = currentQuests.get(progress.templateId());
            if (existingProgress == null || progress.updatedAt() >= existingProgress.updatedAt()) {
                currentQuests.put(progress.templateId(), progress);
                return true;
            }

            plugin.getLogger().warning("Ignor progres quest curent mai vechi pentru jucatorul " + playerId
                + " si template-ul " + progress.templateId());
            return false;
        }

        archiveQuestProgress(playerId, progress);
        return true;
    }

    private void registerLoadedTrackedQuest(UUID playerId, PlayerQuestProgress progress) {
        if (playerId == null || progress == null || !progress.isActive()) {
            return;
        }

        String existingTemplateId = trackedQuestTemplates.getOrDefault(playerId, "");
        PlayerQuestProgress existingTrackedProgress = getCurrentQuestProgress(playerId, existingTemplateId);
        if (existingTrackedProgress != null && existingTrackedProgress.updatedAt() > progress.updatedAt()) {
            return;
        }

        trackedQuestPlayers.add(playerId);
        trackedQuestTemplates.put(playerId, progress.templateId());
    }

    private void persistQuestProgressAsync(UUID playerId, PlayerQuestProgress progress) {
        if (playerId == null || progress == null) {
            return;
        }

        plugin.getDatabaseManager().runAsync(() -> persistQuestProgress(playerId, progress));
    }

    private Map<UUID, List<PlayerQuestProgress>> snapshotQuestProgress() {
        Map<UUID, List<PlayerQuestProgress>> snapshot = new LinkedHashMap<>();
        for (Map.Entry<UUID, Map<String, PlayerQuestProgress>> playerEntry : activePlayerQuests.entrySet()) {
            for (PlayerQuestProgress progress : playerEntry.getValue().values()) {
                snapshot.computeIfAbsent(playerEntry.getKey(), ignored -> new ArrayList<>()).add(progress);
            }
        }
        for (Map.Entry<UUID, Map<String, PlayerQuestProgress>> playerEntry : archivedPlayerQuests.entrySet()) {
            for (PlayerQuestProgress progress : playerEntry.getValue().values()) {
                snapshot.computeIfAbsent(playerEntry.getKey(), ignored -> new ArrayList<>()).add(progress);
            }
        }
        return snapshot;
    }

    private void persistQuestProgressSnapshot(Map<UUID, List<PlayerQuestProgress>> snapshot) {
        for (Map.Entry<UUID, List<PlayerQuestProgress>> entry : snapshot.entrySet()) {
            UUID playerId = entry.getKey();
            for (PlayerQuestProgress progress : entry.getValue()) {
                persistQuestProgress(playerId, progress);
            }
        }
    }

    private void persistQuestProgress(UUID playerId, PlayerQuestProgress progress) {
        String sql = """
            INSERT INTO player_quests (
                player_uuid, template_id, quest_code, status, started_at, completed_at,
                current_phase, current_stage_id, objective_progress, quest_variables, updated_at, tracked
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(player_uuid, template_id) DO UPDATE SET
                quest_code = excluded.quest_code,
                status = excluded.status,
                started_at = excluded.started_at,
                completed_at = excluded.completed_at,
                current_phase = excluded.current_phase,
                current_stage_id = excluded.current_stage_id,
                objective_progress = excluded.objective_progress,
                quest_variables = excluded.quest_variables,
                updated_at = excluded.updated_at,
                tracked = excluded.tracked
        """;

        try {
            try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement(sql)) {
                stmt.setString(1, playerId.toString());
                stmt.setString(2, progress.templateId());
                stmt.setString(3, progress.questCode());
                stmt.setString(4, progress.status().storageValue());
                if (progress.startedAt() > 0) {
                    stmt.setLong(5, progress.startedAt());
                } else {
                    stmt.setNull(5, java.sql.Types.BIGINT);
                }
                if (progress.completedAt() > 0) {
                    stmt.setLong(6, progress.completedAt());
                } else {
                    stmt.setNull(6, java.sql.Types.BIGINT);
                }
                stmt.setString(7, progress.currentPhase());
                stmt.setString(8, progress.currentPhase());
                stmt.setString(9, serializeJson(progress.objectiveProgress()));
                stmt.setString(10, serializeJson(progress.questVariables()));
                stmt.setLong(11, progress.updatedAt());
                stmt.setInt(12, isTrackedQuest(playerId, progress) && progress.isActive() ? 1 : 0);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Nu am putut salva progresul quest-ului " + progress.templateId()
                + " pentru player " + playerId + ": " + e.getMessage());
        }
    }

    private void persistQuestTrackingPreferenceAsync(UUID playerId, PlayerQuestProgress trackedProgress) {
        if (playerId == null || trackedProgress == null || trackedProgress.templateId().isBlank()) {
            return;
        }

        plugin.getDatabaseManager().runAsync(() -> {
            persistQuestProgress(playerId, trackedProgress);
            persistQuestTrackingPreference(playerId, trackedProgress.templateId());
        });
    }

    private void persistQuestTrackingPreferenceAsync(UUID playerId, String templateId) {
        if (playerId == null) {
            return;
        }

        plugin.getDatabaseManager().runAsync(() -> persistQuestTrackingPreference(playerId, templateId));
    }

    private void persistQuestTrackingPreference(UUID playerId, String templateId) {
        String trackedTemplateId = templateId != null ? templateId : "";
        String sql = """
            UPDATE player_quests
            SET tracked = CASE
                WHEN template_id = ? AND status = ? THEN 1
                ELSE 0
            END
            WHERE player_uuid = ?
        """;

        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement(sql)) {
            stmt.setString(1, trackedTemplateId);
            stmt.setString(2, QuestStatus.ACTIVE.storageValue());
            stmt.setString(3, playerId.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Nu am putut salva quest tracking pentru player "
                + playerId + ": " + e.getMessage());
        }
    }

    private void deleteQuestProgressAsync(UUID playerId, String templateId) {
        if (playerId == null || templateId == null || templateId.isBlank()) {
            return;
        }

        plugin.getDatabaseManager().runAsync(() -> deleteQuestProgress(playerId, templateId));
    }

    private void deleteQuestProgress(UUID playerId, String templateId) {
        String sql = """
            DELETE FROM player_quests
            WHERE player_uuid = ? AND template_id = ?
        """;

        try {
            deleteQuestAnchorBindings(playerId, templateId);
        } catch (SQLException e) {
            plugin.getLogger().warning("Nu am putut sterge binding-urile quest-ului " + templateId
                + " pentru player " + playerId + ": " + e.getMessage());
        }

        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement(sql)) {
            stmt.setString(1, playerId.toString());
            stmt.setString(2, templateId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Nu am putut sterge progresul quest-ului " + templateId
                + " pentru player " + playerId + ": " + e.getMessage());
        }
    }

    private void persistQuestAnchorsAsync(UUID playerId,
                                          PlayerQuestProgress progress,
                                          QuestAnchorResolver.ResolvedQuestAnchors resolvedAnchors) {
        if (playerId == null || progress == null || resolvedAnchors == null || !resolvedAnchors.valid()) {
            return;
        }

        plugin.getDatabaseManager().runAsync(() -> persistQuestAnchors(playerId, progress, resolvedAnchors));
    }

    private void persistQuestAnchors(UUID playerId,
                                     PlayerQuestProgress progress,
                                     QuestAnchorResolver.ResolvedQuestAnchors resolvedAnchors) {
        if (playerId == null || progress == null || resolvedAnchors == null || progress.templateId().isBlank()) {
            return;
        }

        String sql = """
            INSERT INTO quest_anchor_bindings (
                player_uuid, template_id, objective_key, quest_code, objective_type, reference,
                anchor_type, anchor_id, anchor_label, created_at, updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try {
            deleteQuestAnchorBindings(playerId, progress.templateId());
            if (resolvedAnchors.anchors().isEmpty()) {
                return;
            }

            long now = System.currentTimeMillis();
            try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement(sql)) {
                for (QuestAnchorResolver.ResolvedQuestAnchor anchor : resolvedAnchors.anchors()) {
                    if (anchor.anchorId() == null || anchor.anchorId().isBlank()) {
                        continue;
                    }

                    stmt.setString(1, playerId.toString());
                    stmt.setString(2, progress.templateId());
                    stmt.setString(3, anchor.objectiveKey());
                    stmt.setString(4, progress.questCode());
                    stmt.setString(5, anchor.objectiveType());
                    stmt.setString(6, anchor.reference());
                    stmt.setString(7, anchor.anchorType());
                    stmt.setString(8, anchor.anchorId());
                    stmt.setString(9, anchor.label());
                    stmt.setLong(10, now);
                    stmt.setLong(11, now);
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Nu am putut salva binding-urile quest-ului " + progress.templateId()
                + " pentru player " + playerId + ": " + e.getMessage());
        }
    }

    private void deleteQuestAnchorBindings(UUID playerId, String templateId) throws SQLException {
        String sql = """
            DELETE FROM quest_anchor_bindings
            WHERE player_uuid = ? AND template_id = ?
        """;

        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement(sql)) {
            stmt.setString(1, playerId.toString());
            stmt.setString(2, templateId);
            stmt.executeUpdate();
        }
    }

    private Map<String, String> mergeStoredQuestAnchorVariables(UUID playerId,
                                                                String templateId,
                                                                Map<String, String> questVariables) {
        Map<String, String> storedAnchorVariables = loadQuestAnchorVariables(playerId, templateId);
        if (storedAnchorVariables.isEmpty()) {
            return questVariables;
        }

        Map<String, String> mergedVariables = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : questVariables.entrySet()) {
            if (!isQuestAnchorVariableKey(entry.getKey())) {
                mergedVariables.put(entry.getKey(), entry.getValue());
            }
        }
        mergedVariables.putAll(storedAnchorVariables);
        return Collections.unmodifiableMap(mergedVariables);
    }

    private Map<String, String> loadQuestAnchorVariables(UUID playerId, String templateId) {
        if (playerId == null || templateId == null || templateId.isBlank()) {
            return Map.of();
        }

        String sql = """
            SELECT objective_key, objective_type, reference, anchor_type, anchor_id, anchor_label
            FROM quest_anchor_bindings
            WHERE player_uuid = ? AND template_id = ?
            ORDER BY objective_key
        """;

        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement(sql)) {
            stmt.setString(1, playerId.toString());
            stmt.setString(2, templateId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<QuestAnchorResolver.ResolvedQuestAnchor> anchors = new ArrayList<>();
                while (rs.next()) {
                    anchors.add(new QuestAnchorResolver.ResolvedQuestAnchor(
                        readTextOrEmpty(rs, "objective_key"),
                        readTextOrEmpty(rs, "objective_type"),
                        readTextOrEmpty(rs, "reference"),
                        readTextOrEmpty(rs, "anchor_type"),
                        readTextOrEmpty(rs, "anchor_id"),
                        readTextOrEmpty(rs, "anchor_label")
                    ));
                }
                if (anchors.isEmpty()) {
                    return Map.of();
                }
                return new QuestAnchorResolver.ResolvedQuestAnchors(anchors, List.of()).toQuestVariables();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Nu am putut citi binding-urile quest-ului " + templateId
                + " pentru player " + playerId + ": " + e.getMessage());
            return Map.of();
        }
    }

    private Map<String, Integer> parseObjectiveProgress(String json) {
        return parseJsonMap(json, OBJECTIVE_PROGRESS_TYPE);
    }

    private Map<String, String> parseQuestVariables(String json) {
        return parseJsonMap(json, QUEST_VARIABLES_TYPE);
    }

    private <T> Map<String, T> parseJsonMap(String json, Type type) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }

        try {
            Map<String, T> parsed = gson.fromJson(json, type);
            if (parsed == null || parsed.isEmpty()) {
                return Map.of();
            }
            return Collections.unmodifiableMap(new LinkedHashMap<>(parsed));
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("Nu am putut citi progresul JSON al quest-ului: " + ex.getMessage());
            return Map.of();
        }
    }

    private String serializeJson(Map<?, ?> values) {
        if (values == null || values.isEmpty()) {
            return "{}";
        }
        return gson.toJson(values);
    }

    private PlayerQuestProgress refreshTrackedQuestProgress(Player player,
                                                            ScenarioTemplate template,
                                                            PlayerQuestProgress progress) {
        if (player == null || template == null || progress == null || !progress.isCurrent()) {
            return progress;
        }

        String initialPhase = progress.currentPhase().isBlank()
            ? resolveQuestPhase(template, progress.status(), progress)
            : progress.currentPhase();
        Map<String, Integer> refreshedObjectiveProgress = buildObjectiveProgressSnapshot(
            player.getInventory(),
            template,
            progress.objectiveProgress(),
            initialPhase
        );
        String refreshedPhase = resolveQuestPhase(template, progress.status(), initialPhase, refreshedObjectiveProgress);

        if (refreshedPhase.equals(progress.currentPhase())
            && refreshedObjectiveProgress.equals(progress.objectiveProgress())) {
            return progress;
        }

        PlayerQuestProgress refreshedProgress = new PlayerQuestProgress(
            progress.templateId(),
            progress.questCode(),
            progress.status(),
            progress.startedAt(),
            progress.completedAt(),
            System.currentTimeMillis(),
            refreshedPhase,
            refreshedObjectiveProgress,
            progress.questVariables()
        );
        putActiveQuestProgress(player.getUniqueId(), refreshedProgress);
        persistQuestProgressAsync(player.getUniqueId(), refreshedProgress);
        return refreshedProgress;
    }

    private PlayerQuestProgress trackNpcObjectiveProgress(Player player,
                                                          AINPC npc,
                                                          ScenarioTemplate template,
                                                          PlayerQuestProgress progress) {
        if (player == null || npc == null || template == null || progress == null || !progress.isCurrent()) {
            return progress;
        }

        Map<String, Integer> updatedProgress = new LinkedHashMap<>(progress.objectiveProgress());
        boolean changed = false;
        List<FeaturePackLoader.QuestEntryDefinition> objectives = template.getObjectives();
        for (int index = 0; index < objectives.size(); index++) {
            FeaturePackLoader.QuestEntryDefinition objective = objectives.get(index);
            if (!isObjectiveActiveForProgress(template, progress, objective)) {
                continue;
            }
            if (!matchesObjectiveType(objective, "talk_to_npc")) {
                continue;
            }
            if (!matchesNpcObjective(plugin.getFeaturePackLoader(), objective, npc, template, progress)) {
                continue;
            }

            String objectiveKey = buildObjectiveKey(objective, index);
            changed |= carryLegacyObjectiveProgress(updatedProgress, objective, index);
            changed |= incrementObjectiveProgress(updatedProgress, objectiveKey, objective.getAmount());
        }

        if (!changed) {
            return progress;
        }

        return updateTrackedQuestProgress(player.getUniqueId(), template, progress, updatedProgress);
    }

    private QuestAnchorResolver.ResolvedQuestAnchors resolveQuestAnchors(ScenarioTemplate template,
                                                                         Player player,
                                                                         AINPC npc) {
        Collection<AINPC> allNpcs = plugin.getNpcManager() != null
            ? plugin.getNpcManager().getAllNPCs()
            : List.of();
        return new QuestAnchorResolver(
            plugin.getPlatform() != null ? plugin.getPlatform().getWorldAdminService() : null,
            allNpcs
        ).resolve(template, player != null ? player.getLocation() : null, npc);
    }

    private QuestInteractionResult buildQuestUnavailableResult(ScenarioTemplate template,
                                                               QuestAnchorResolver.ResolvedQuestAnchors resolvedAnchors) {
        List<String> systemMessages = new ArrayList<>();
        systemMessages.add("&cQuest indisponibil: &f" + resolveQuestTitle(template));
        systemMessages.add("&7Lipsesc ancore semantice in mapping:");
        for (String issue : resolvedAnchors.formatIssues()) {
            systemMessages.add("&7- &f" + issue);
        }
        systemMessages.add("&8Foloseste /ainpc world si /ainpc audit world pentru verificare.");

        return QuestInteractionResult.handled(
            true,
            buildQuestNpcMessages(
                template,
                null,
                QuestDialogueContext.UNAVAILABLE,
                List.of("Nu pot porni misiunea asta acum. Locurile sau punctele necesare nu sunt pregatite.")
            ),
            systemMessages
        );
    }

    private QuestInteractionResult buildQuestUnavailableResult(ScenarioTemplate template,
                                                               QuestAvailability availability) {
        List<String> systemMessages = new ArrayList<>();
        systemMessages.add("&cQuest indisponibil: &f" + resolveQuestTitle(template));
        for (String issue : availability.issues()) {
            systemMessages.add("&7- &f" + issue);
        }

        return QuestInteractionResult.handled(
            true,
            buildQuestNpcMessages(
                template,
                null,
                QuestDialogueContext.UNAVAILABLE,
                List.of("Nu pot porni misiunea asta acum.")
            ),
            systemMessages
        );
    }

    private PlayerQuestProgress bindQuestProgressToNpc(UUID playerId,
                                                       ScenarioTemplate template,
                                                       PlayerQuestProgress progress,
                                                       AINPC npc) {
        if (playerId == null || template == null || progress == null || npc == null) {
            return progress;
        }

        Map<String, String> updatedVariables = new LinkedHashMap<>(progress.questVariables());
        if (npc.getUuid() != null) {
            updatedVariables.put("quest_giver_uuid", npc.getUuid().toString());
        }
        if (npc.getDatabaseId() > 0) {
            updatedVariables.put("quest_giver_db_id", String.valueOf(npc.getDatabaseId()));
        }
        if (npc.getName() != null && !npc.getName().isBlank()) {
            updatedVariables.put("quest_giver_name", npc.getName());
        }
        if (npc.getDisplayName() != null && !npc.getDisplayName().isBlank()) {
            updatedVariables.put("quest_giver_display_name", npc.getDisplayName());
        }
        if (npc.getOccupation() != null && !npc.getOccupation().isBlank()) {
            updatedVariables.put("quest_giver_occupation", npc.getOccupation());
        }

        if (updatedVariables.equals(progress.questVariables())) {
            return progress;
        }

        PlayerQuestProgress updatedProgress = new PlayerQuestProgress(
            progress.templateId(),
            progress.questCode(),
            progress.status(),
            progress.startedAt(),
            progress.completedAt(),
            System.currentTimeMillis(),
            progress.currentPhase(),
            progress.objectiveProgress(),
            updatedVariables
        );
        putActiveQuestProgress(playerId, updatedProgress);
        persistQuestProgressAsync(playerId, updatedProgress);
        return updatedProgress;
    }

    private PlayerQuestProgress bindQuestProgressToAnchors(UUID playerId,
                                                           PlayerQuestProgress progress,
                                                           QuestAnchorResolver.ResolvedQuestAnchors resolvedAnchors) {
        if (playerId == null || progress == null || resolvedAnchors == null || !resolvedAnchors.valid()) {
            return progress;
        }

        Map<String, String> updatedVariables = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : progress.questVariables().entrySet()) {
            String key = entry.getKey();
            if (!isQuestAnchorVariableKey(key)) {
                updatedVariables.put(key, entry.getValue());
            }
        }
        updatedVariables.putAll(resolvedAnchors.toQuestVariables());

        if (updatedVariables.equals(progress.questVariables())) {
            persistQuestAnchorsAsync(playerId, progress, resolvedAnchors);
            return progress;
        }

        PlayerQuestProgress updatedProgress = new PlayerQuestProgress(
            progress.templateId(),
            progress.questCode(),
            progress.status(),
            progress.startedAt(),
            progress.completedAt(),
            System.currentTimeMillis(),
            progress.currentPhase(),
            progress.objectiveProgress(),
            updatedVariables
        );
        putActiveQuestProgress(playerId, updatedProgress);
        persistQuestProgressAsync(playerId, updatedProgress);
        persistQuestAnchorsAsync(playerId, updatedProgress, resolvedAnchors);
        return updatedProgress;
    }

    private PlayerQuestProgress updateTrackedQuestProgress(UUID playerId,
                                                           ScenarioTemplate template,
                                                           PlayerQuestProgress progress,
                                                           Map<String, Integer> objectiveProgress) {
        if (playerId == null || template == null || progress == null) {
            return progress;
        }

        Map<String, Integer> normalizedProgress = Collections.unmodifiableMap(new LinkedHashMap<>(objectiveProgress));
        String updatedPhase = resolveQuestPhase(template, progress.status(), progress, normalizedProgress);
        Map<String, String> updatedVariables = buildQuestStageTransitionVariables(
            template,
            progress,
            updatedPhase,
            normalizedProgress
        );
        if (normalizedProgress.equals(progress.objectiveProgress())
            && updatedPhase.equals(progress.currentPhase())
            && updatedVariables.equals(progress.questVariables())) {
            return progress;
        }

        PlayerQuestProgress updatedProgress = new PlayerQuestProgress(
            progress.templateId(),
            progress.questCode(),
            progress.status(),
            progress.startedAt(),
            progress.completedAt(),
            System.currentTimeMillis(),
            updatedPhase,
            normalizedProgress,
            updatedVariables
        );
        putActiveQuestProgress(playerId, updatedProgress);
        persistQuestProgressAsync(playerId, updatedProgress);
        return updatedProgress;
    }

    private ScenarioTemplate resolveTemplateForProgress(PlayerQuestProgress progress, AINPC npcContext) {
        if (progress == null || progress.templateId() == null || progress.templateId().isBlank()) {
            return null;
        }

        for (ScenarioTemplate template : questTemplates) {
            if (progress.templateId().equals(template.getTemplateId())) {
                return template;
            }
        }

        if (npcContext == null) {
            return null;
        }

        ScenarioTemplate npcTemplate = findQuestTemplateForNpc(npcContext);
        if (npcTemplate != null && progress.templateId().equals(npcTemplate.getTemplateId())) {
            return npcTemplate;
        }

        return null;
    }

    



    private WorldRegion findCurrentRegion(Location location) {
        if (location == null || location.getWorld() == null || plugin.getPlatform() == null) {
            return null;
        }

        return plugin.getPlatform().getWorldAdminService().findRegionAt(
            location.getWorld().getName(),
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ()
        );
    }

    private WorldPlace findCurrentPlace(Location location) {
        if (location == null || location.getWorld() == null || plugin.getPlatform() == null) {
            return null;
        }

        return plugin.getPlatform().getWorldAdminService().findPlaceAt(
            location.getWorld().getName(),
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ()
        );
    }

    private WorldNode findCurrentNode(Location location) {
        if (location == null || location.getWorld() == null || plugin.getPlatform() == null) {
            return null;
        }

        return plugin.getPlatform().getWorldAdminService().findNodeAt(
            location.getWorld().getName(),
            location.getX(),
            location.getY(),
            location.getZ()
        );
    }

    private ScenarioTemplate findQuestTemplateForNpc(AINPC npc) {
        return findQuestTemplateForNpc(npc, null);
    }

    private ScenarioTemplate findQuestTemplateForNpc(AINPC npc, UUID playerId) {
        return findQuestTemplateForNpc(npc, playerId, "");
    }

    private ScenarioTemplate findQuestTemplateForNpc(AINPC npc, UUID playerId, String progressionKind) {
        ScenarioTemplate currentTemplate = resolveCurrentQuestTemplateForNpc(npc, playerId, progressionKind);
        if (currentTemplate != null) {
            return currentTemplate;
        }

        List<ScenarioTemplate> configuredTemplates = questTemplates.stream()
            .filter(ScenarioTemplate::hasQuestBriefing)
            .filter(template -> matchesQuestGiver(plugin.getFeaturePackLoader(), npc, template))
            .filter(template -> matchesProgressionKindFilter(template, progressionKind))
            .toList();
        if (!configuredTemplates.isEmpty()) {
            if (playerId == null) {
                return configuredTemplates.get(0);
            }

            ScenarioTemplate configuredTemplate = QuestTemplateSelector.selectConfiguredTemplate(
                configuredTemplates,
                template -> evaluateQuestAvailability(playerId, template).available(),
                template -> getCompletedQuestProgress(playerId, template.getTemplateId()) != null
            );
            if (configuredTemplate != null) {
                return configuredTemplate;
            }
        }

        return normalizeReference(progressionKind).isBlank() && shouldUseSimpleQuestForAllNpcs()
            ? buildSimpleQuestTemplate(npc)
            : null;
    }

    private ScenarioTemplate resolveCurrentQuestTemplateForNpc(AINPC npc, UUID playerId) {
        return resolveCurrentQuestTemplateForNpc(npc, playerId, "");
    }

    private ScenarioTemplate resolveCurrentQuestTemplateForNpc(AINPC npc, UUID playerId, String progressionKind) {
        if (npc == null || playerId == null) {
            return null;
        }

        List<PlayerQuestProgress> currentProgresses = getCurrentQuestProgress(playerId);
        for (PlayerQuestProgress progress : currentProgresses) {
            ScenarioTemplate template = resolveTemplateForProgress(progress, null);
            if (template != null
                && template.hasQuestBriefing()
                && matchesProgressionKindFilter(template, progressionKind)
                && matchesQuestGiver(plugin.getFeaturePackLoader(), npc, template)) {
                return template;
            }
        }

        for (PlayerQuestProgress progress : currentProgresses) {
            ScenarioTemplate template = resolveCurrentQuestTemplateForNpc(npc, progress);
            if (template != null && matchesProgressionKindFilter(template, progressionKind)) {
                return template;
            }
        }

        return null;
    }

    private ScenarioTemplate resolveCurrentQuestTemplateForNpc(AINPC npc, PlayerQuestProgress progress) {
        if (npc == null || progress == null || !progress.isCurrent()) {
            return null;
        }

        ScenarioTemplate template = resolveTemplateForProgress(progress, null);
        if (template == null || !template.hasQuestBriefing()) {
            return null;
        }

        if (matchesQuestGiver(plugin.getFeaturePackLoader(), npc, template)) {
            return template;
        }

        return progress.isActive() && matchesActiveQuestNpcObjective(npc, template, progress) ? template : null;
    }

    private boolean matchesActiveQuestNpcObjective(AINPC npc, ScenarioTemplate template, PlayerQuestProgress progress) {
        if (npc == null || template == null || progress == null || template.getObjectives().isEmpty()) {
            return false;
        }

        List<FeaturePackLoader.QuestEntryDefinition> objectives = template.getObjectives();
        for (int index = 0; index < objectives.size(); index++) {
            FeaturePackLoader.QuestEntryDefinition objective = objectives.get(index);
            if (matchesObjectiveType(objective, "talk_to_npc")
                && matchesNpcObjective(plugin.getFeaturePackLoader(), objective, npc, template, progress)) {
                return true;
            }
        }

        return false;
    }

    private boolean matchesProgressionKindFilter(ScenarioTemplate template, String progressionKind) {
        String expected = normalizeReference(progressionKind);
        return expected.isBlank() || progressionKindMatches(plugin.getFeaturePackLoader(), template, expected);
    }

    private boolean shouldUseSimpleQuestForAllNpcs() {
        return ScenarioSimpleQuestKt.shouldUseSimpleQuestForAllNpcs();
    }

    private ScenarioTemplate buildSimpleQuestTemplate(AINPC npc) {
        return ScenarioSimpleQuestKt.buildSimpleQuestTemplate(npc);
    }

    private FeaturePackLoader.ProfessionDefinition resolveQuestProfession(AINPC npc) {
        return ScenarioSimpleQuestKt.resolveQuestProfession(npc);
    }

    private SimpleQuestProfile resolveSimpleQuestProfile(AINPC npc,
                                                         FeaturePackLoader.ProfessionDefinition profession) {
        return ScenarioSimpleQuestKt.resolveSimpleQuestProfile(npc, profession);
    }

    private SimpleQuestProfile applyConfiguredSimpleQuestProfile(AINPC npc,
                                                                 String professionId,
                                                                 String professionName,
                                                                 SimpleQuestProfile fallbackProfile) {
        return ScenarioSimpleQuestKt.applyConfiguredSimpleQuestProfile(npc, professionId, professionName, fallbackProfile);
    }

    private ConfigurationSection resolveProfessionFallbackSection(String professionId, String professionName) {
        return ScenarioSimpleQuestKt.resolveProfessionFallbackSection(professionId, professionName);
    }

    private String resolveConfiguredSimpleQuestTitle(String professionName) {
        return ScenarioSimpleQuestKt.resolveConfiguredSimpleQuestTitle(professionName);
    }

    private Material resolveConfiguredQuestMaterial(String path, Material fallback) {
        return ScenarioSimpleQuestKt.resolveConfiguredQuestMaterial(path, fallback);
    }

    private Material resolveConfiguredQuestMaterialValue(String configuredValue, Material fallback) {
        return ScenarioSimpleQuestKt.resolveConfiguredQuestMaterialValue(configuredValue, fallback);
    }

    private List<String> buildQuestBriefingMessages(ScenarioTemplate template) {
        List<String> lines = new ArrayList<>();
        String progressionLabel = capitalizeProgressionLabel(resolveProgressionSingularLabel(plugin.getFeaturePackLoader(), template));
        lines.add("&6[" + progressionLabel + "] &f" + resolveQuestTitle(template));

        String questGiver = resolveProfessionName(template.getQuestGiverProfession());
        if (!questGiver.isBlank()) {
            lines.add("&7Dat de: &f" + questGiver);
        }
        if (!resolveProgressionMechanicDisplay(plugin.getFeaturePackLoader(), template).isBlank()) {
            lines.add("&7Mecanica: &f" + resolveProgressionMechanicDisplay(plugin.getFeaturePackLoader(), template));
        }
        QuestScenarioContract contract = template.getQuestContract();
        if (contract != null) {
            lines.add("&7Tip scenariu: &f" + contract.displayName());
            lines.add("&7Categorie: &f" + contract.categoryDisplayName());
        }

        if (!template.getObjectives().isEmpty()) {
            lines.add("&eObiective:");
            for (FeaturePackLoader.QuestEntryDefinition objective : template.getObjectives()) {
                lines.add("&7- &f" + formatQuestEntry(objective));
            }
        }

        if (!template.getRewards().isEmpty()) {
            lines.add("&aRecompensa:");
            for (FeaturePackLoader.QuestEntryDefinition reward : template.getRewards()) {
                lines.add("&7- &f" + formatQuestEntry(reward));
            }
        }

        return lines;
    }

    private List<String> buildQuestStatusMessages(ScenarioTemplate template,
                                                  PlayerQuestProgress progress,
                                                  Player player,
                                                  String npcName) {
        if (progress != null && progress.isCurrent()) {
            progress = refreshTrackedQuestProgress(player, template, progress);
        }

        List<String> lines = buildQuestBriefingMessages(template);
        lines.add(1, "&7Status: &f" + formatQuestStatus(progress != null ? progress.status() : QuestStatus.NOT_STARTED));
        if (progress != null && !progress.currentPhase().isBlank()) {
            lines.add(2, "&7Faza curenta: &f" + formatQuestPhase(progress.currentPhase()));
        }

        if (progress == null || progress.status() == QuestStatus.NOT_STARTED) {
            lines.add("&7Nu ai acceptat inca acest " + resolveProgressionSingularLabel(plugin.getFeaturePackLoader(), template) + ".");
            if (npcName != null && !npcName.isBlank()) {
                lines.add("&eAcceptare: &fScrie &ada&f/&aaccept &fsau foloseste &a/npcquest accept " + npcName);
            }
            return lines;
        }

        if (progress.isOffered()) {
            if (npcName != null && !npcName.isBlank()) {
                lines.add("&eAcceptare: &fScrie &ada&f/&aaccept &fsau foloseste &a/npcquest accept");
                lines.add("&cRefuz: &fScrie &anu&f/&arefuz &fsau foloseste &a/npcquest decline");
            }
            return lines;
        }

        if (progress.isActive()) {
            List<String> progressLines = buildObjectiveProgressLines(template, progress, player);
            if (!progressLines.isEmpty()) {
                lines.add("&eProgres:");
                lines.addAll(progressLines);
            }

            QuestObjectiveCheck objectiveCheck = inspectQuestObjectives(player, template, progress, null, false);
            if (player == null) {
                lines.add("&7" + capitalizeProgressionLabel(resolveProgressionSingularLabel(plugin.getFeaturePackLoader(), template)) + " activ.");
            } else if (objectiveCheck.complete()) {
                lines.add("&aAi indeplinit toate obiectivele. Revino la NPC pentru finalizare.");
            } else {
                lines.add("&eIti mai lipsesc:");
                for (String missingObjective : objectiveCheck.missingObjectives()) {
                    lines.add("&7- &f" + missingObjective);
                }
            }
            if (npcName != null && !npcName.isBlank()) {
                lines.add("&cAbandon: &fScrie &arenunt &fsau foloseste &a/npcquest abandon " + npcName);
            }
            return lines;
        }

        if (progress.isCompleted()) {
            lines.add("&a" + capitalizeProgressionLabel(resolveProgressionSingularLabel(plugin.getFeaturePackLoader(), template)) + " finalizat.");
        } else if (progress.status() == QuestStatus.FAILED) {
            lines.add("&c" + capitalizeProgressionLabel(resolveProgressionSingularLabel(plugin.getFeaturePackLoader(), template)) + " abandonat sau esuat.");
            lines.add("&7Poti cere din nou progresia daca vrei sa reincepi.");
        }

        return lines;
    }

    private List<String> applyQuestStoryActions(Player player,
                                                AINPC npc,
                                                ScenarioTemplate template,
                                                PlayerQuestProgress progress,
                                                List<FeaturePackLoader.QuestEntryDefinition> rewards) {
        if (rewards == null || rewards.isEmpty()) {
            return List.of();
        }

        boolean hasStoryAction = rewards.stream().anyMatch(ScenarioStoryTextKt::isQuestStoryAction);
        if (!hasStoryAction) {
            return List.of();
        }

        StoryStateService storyStateService = plugin.getStoryStateService();
        if (storyStateService == null) {
            return List.of("&cStoryStateService indisponibil; actiunile story nu au fost aplicate.");
        }

        List<String> notes = new ArrayList<>();
        for (FeaturePackLoader.QuestEntryDefinition reward : rewards) {
            String actionType = normalizeStoryActionType(reward);
            if (actionType.isBlank()) {
                continue;
            }

            try {
                switch (actionType) {
                    case "set_story_state" -> applySetStoryStateAction(storyStateService, player, npc, template, progress, reward, notes);
                    case "record_story_event" -> applyRecordStoryEventAction(storyStateService, player, npc, template, progress, reward, notes);
                    default -> {
                    }
                }
            } catch (SQLException | IllegalArgumentException ex) {
                plugin.debug("[QuestEngine] Actiune story esuata pentru "
                    + (template != null ? template.getTemplateId() : "quest necunoscut")
                    + ": " + ex.getMessage());
                notes.add("&cActiune story esuata: &f" + formatQuestEntry(reward));
            }
        }

        return notes;
    }

    private void applySetStoryStateAction(StoryStateService storyStateService,
                                          Player player,
                                          AINPC npc,
                                          ScenarioTemplate template,
                                          PlayerQuestProgress progress,
                                          FeaturePackLoader.QuestEntryDefinition action,
                                          List<String> notes) throws SQLException {
        StoryActionTarget target = resolveStoryActionTarget(action, player, progress);
        if (target == null || target.scopeId().isBlank()) {
            notes.add("&eActiune story ignorata: tinta lipsa pentru &f" + formatQuestEntry(action));
            return;
        }

        String stateKey = firstNonBlank(
            getQuestEntryMetadata(action, "state_key", "state", "flag", "value"),
            action != null ? action.getItemId() : "",
            "default"
        );
        Map<String, String> variables = buildStoryActionData(
            action != null ? action.getVariables() : Map.of(),
            template,
            player,
            npc
        );
        String source = firstNonBlank(getQuestEntryMetadata(action, "source"), "quest_completion");
        String updatedBy = player != null ? player.getName() : "quest";

        if ("place".equals(target.scopeType())) {
            storyStateService.savePlaceState(
                target.placeId(),
                target.regionId(),
                stateKey,
                variables,
                updatedBy,
                source
            );
        } else {
            storyStateService.saveRegionState(
                target.regionId(),
                StoryMode.fromId(getQuestEntryMetadata(action, "mode", "story_mode")),
                stateKey,
                parseStoryList(getQuestEntryMetadata(action, "story_pool", "pool")),
                variables,
                updatedBy,
                source
            );
        }

        notes.add("&7Story state actualizat: &f" + target.scopeType() + ":" + target.scopeId() + " &7-> &f" + stateKey);
    }

    private void applyRecordStoryEventAction(StoryStateService storyStateService,
                                             Player player,
                                             AINPC npc,
                                             ScenarioTemplate template,
                                             PlayerQuestProgress progress,
                                             FeaturePackLoader.QuestEntryDefinition action,
                                             List<String> notes) throws SQLException {
        StoryActionTarget target = resolveStoryActionTarget(action, player, progress);
        if (target == null || target.scopeId().isBlank()) {
            notes.add("&eActiune story ignorata: tinta lipsa pentru &f" + formatQuestEntry(action));
            return;
        }

        String eventType = firstNonBlank(getQuestEntryMetadata(action, "event_type", "type_id"), "quest_completed");
        String eventKey = firstNonBlank(
            getQuestEntryMetadata(action, "event_key", "key"),
            template != null ? normalizeReference(template.getQuestCode()) : ""
        );
        String title = firstNonBlank(
            getQuestEntryMetadata(action, "title", "name"),
            template != null ? resolveQuestTitle(template) : "Quest completat"
        );
        String description = firstNonBlank(
            getQuestEntryMetadata(action, "event_description", "message", "details"),
            action != null ? action.getDescription() : ""
        );
        Map<String, String> payload = buildStoryActionData(
            action != null ? action.getPayload() : Map.of(),
            template,
            player,
            npc
        );

        storyStateService.recordEvent(
            target.scopeType(),
            target.scopeId(),
            target.regionId(),
            target.placeId(),
            eventType,
            eventKey,
            title,
            description,
            payload,
            firstNonBlank(getQuestEntryMetadata(action, "actor_type"), player != null ? "player" : "quest"),
            firstNonBlank(getQuestEntryMetadata(action, "actor_id"), storyActorId(player, npc, template)),
            player != null ? player.getUniqueId().toString() : "",
            npc != null && npc.getUuid() != null ? npc.getUuid().toString() : ""
        );

        notes.add("&7Story event inregistrat: &f" + target.scopeType() + ":" + target.scopeId() + " &7-> &f" + eventType);
    }

    private StoryActionTarget resolveStoryActionTarget(FeaturePackLoader.QuestEntryDefinition action,
                                                       Player player,
                                                       PlayerQuestProgress progress) {
        String targetValue = getQuestEntryMetadata(action, "target", "scope_id", "target_id", "id");
        String scope = normalizeStoryScope(getQuestEntryMetadata(action, "scope", "scope_type"));
        String targetScope = detectStoryTargetScope(targetValue);
        String placeId = firstNonBlank(
            cleanStoryId(getQuestEntryMetadata(action, "place_id", "placeId", "target_place", "place")),
            "place".equals(targetScope) ? cleanStoryId(targetValue) : ""
        );
        String regionId = firstNonBlank(
            cleanStoryId(getQuestEntryMetadata(action, "region_id", "regionId", "target_region", "region")),
            "region".equals(targetScope) ? cleanStoryId(targetValue) : ""
        );

        if (scope.isBlank()) {
            scope = !placeId.isBlank() || "place".equals(targetScope) ? "place" : "region";
        }

        if ("place".equals(scope)) {
            if (placeId.isBlank()) {
                placeId = resolveStoryScopeId(targetValue, "place", player, progress);
            }
            if (regionId.isBlank()) {
                regionId = resolveRegionIdForPlace(placeId, player);
            }
            if (placeId.isBlank()) {
                return null;
            }
            return new StoryActionTarget("place", placeId, regionId, placeId);
        }

        if (regionId.isBlank() && !placeId.isBlank()) {
            regionId = resolveRegionIdForPlace(placeId, player);
        }
        if (regionId.isBlank()) {
            regionId = resolveStoryScopeId(targetValue, "region", player, progress);
        }
        if (regionId.isBlank()) {
            return null;
        }
        return new StoryActionTarget("region", regionId, regionId, placeId);
    }

    private String resolveStoryScopeId(String targetValue,
                                       String expectedScope,
                                       Player player,
                                       PlayerQuestProgress progress) {
        String normalizedTarget = normalizeReference(targetValue);
        if (("current_" + expectedScope).equals(normalizedTarget) || expectedScope.equals(normalizedTarget)) {
            return "place".equals(expectedScope) ? findCurrentPlaceId(player) : findCurrentRegionId(player);
        }

        String directScope = detectStoryTargetScope(targetValue);
        if (expectedScope.equals(directScope)) {
            return cleanStoryId(targetValue);
        }
        if (!targetValue.isBlank()
            && directScope.isBlank()
            && !normalizedTarget.startsWith("anchor_")
            && !normalizedTarget.startsWith("current_")) {
            return cleanStoryId(targetValue);
        }

        String anchorId = resolveStoryAnchorReference(targetValue, expectedScope, progress);
        if (!anchorId.isBlank()) {
            return anchorId;
        }

        anchorId = findFirstQuestAnchorId(progress, expectedScope);
        if (!anchorId.isBlank()) {
            return anchorId;
        }

        if ("region".equals(expectedScope)) {
            String placeAnchorId = findFirstQuestAnchorId(progress, "place");
            if (!placeAnchorId.isBlank()) {
                String regionId = resolveRegionIdForPlace(placeAnchorId, player);
                if (!regionId.isBlank()) {
                    return regionId;
                }
            }
            return firstNonBlank(findCurrentRegionId(player), resolveRegionIdForPlace(findCurrentPlaceId(player), player));
        }

        return findCurrentPlaceId(player);
    }

    private String resolveStoryAnchorReference(String targetValue, String expectedScope, PlayerQuestProgress progress) {
        if (targetValue == null || targetValue.isBlank() || progress == null) {
            return "";
        }

        String trimmed = targetValue.trim();
        String normalized = normalizeReference(trimmed);
        if (!normalized.startsWith("anchor_")) {
            return "";
        }

        String requestedAnchor = trimmed.substring(trimmed.indexOf(':') + 1).trim();
        String normalizedRequestedAnchor = normalizeReference(requestedAnchor);
        if (normalizedRequestedAnchor.equals(expectedScope)) {
            return findFirstQuestAnchorId(progress, expectedScope);
        }

        String directAnchorId = resolveQuestVariableAnchorId(progress, requestedAnchor, expectedScope);
        if (!directAnchorId.isBlank()) {
            return directAnchorId;
        }

        if ("region".equals(expectedScope)) {
            String placeId = resolveQuestVariableAnchorId(progress, requestedAnchor, "place");
            if (!placeId.isBlank()) {
                return resolveRegionIdForPlace(placeId, null);
            }
        }

        return "";
    }

    private String resolveQuestVariableAnchorId(PlayerQuestProgress progress, String objectiveKey, String expectedScope) {
        if (progress == null || objectiveKey == null || objectiveKey.isBlank()) {
            return "";
        }

        String prefix = "anchor." + objectiveKey;
        String anchorType = normalizeTrackingAnchorType(progress.questVariables().getOrDefault(prefix + ".type", ""));
        String anchorId = progress.questVariables().getOrDefault(prefix + ".id", "");
        if (expectedScope.equals(anchorType) && anchorId != null && !anchorId.isBlank()) {
            return anchorId;
        }

        String normalizedObjectiveKey = normalizeReference(objectiveKey);
        for (String key : progress.questVariables().keySet()) {
            if (!key.startsWith("anchor.") || !key.endsWith(".type")) {
                continue;
            }
            String candidatePrefix = key.substring(0, key.length() - ".type".length());
            String candidateKey = candidatePrefix.substring("anchor.".length());
            if (!normalizeReference(candidateKey).equals(normalizedObjectiveKey)) {
                continue;
            }
            String candidateType = normalizeTrackingAnchorType(progress.questVariables().getOrDefault(key, ""));
            String candidateId = progress.questVariables().getOrDefault(candidatePrefix + ".id", "");
            if (expectedScope.equals(candidateType) && !candidateId.isBlank()) {
                return candidateId;
            }
        }

        return "";
    }

    private String findFirstQuestAnchorId(PlayerQuestProgress progress, String expectedScope) {
        if (progress == null || progress.questVariables().isEmpty()) {
            return "";
        }

        for (String key : progress.questVariables().keySet()) {
            if (!key.startsWith("anchor.") || !key.endsWith(".type")) {
                continue;
            }
            String anchorType = normalizeTrackingAnchorType(progress.questVariables().getOrDefault(key, ""));
            if (!expectedScope.equals(anchorType)) {
                continue;
            }
            String prefix = key.substring(0, key.length() - ".type".length());
            String anchorId = progress.questVariables().getOrDefault(prefix + ".id", "");
            if (anchorId != null && !anchorId.isBlank()) {
                return anchorId;
            }
        }

        return "";
    }

    private String resolveRegionIdForPlace(String placeId, Player player) {
        if (placeId == null || placeId.isBlank()) {
            return "";
        }

        WorldAdminApi worldAdminApi = plugin.getPlatform() != null ? plugin.getPlatform().getWorldAdmin() : null;
        if (worldAdminApi != null) {
            WorldPlaceInfo place = worldAdminApi.getPlace(placeId);
            if (place != null && place.regionId() != null && !place.regionId().isBlank()) {
                return place.regionId();
            }
        }

        WorldPlace currentPlace = findCurrentPlace(player != null ? player.getLocation() : null);
        if (currentPlace != null && placeId.equalsIgnoreCase(currentPlace.getId())) {
            return currentPlace.getRegionId();
        }

        int separator = placeId.indexOf(':');
        return separator > 0 ? placeId.substring(0, separator) : "";
    }

    private String findCurrentRegionId(Player player) {
        WorldRegion region = findCurrentRegion(player != null ? player.getLocation() : null);
        return region != null ? region.getId() : "";
    }

    private String findCurrentPlaceId(Player player) {
        WorldPlace place = findCurrentPlace(player != null ? player.getLocation() : null);
        return place != null ? place.getId() : "";
    }

    /**
     * Evalueaza daca ar trebui sa inceapa un scenariu nou.
     */
    public void evaluateScenarioTriggers(List<AINPC> npcs, List<Player> nearbyPlayers) {
        if (npcs.isEmpty()) {
            return;
        }

        Random random = new Random();

        for (ScenarioTemplate template : scenarioTemplates.values()) {
            boolean hasActiveOfType = activeScenarios.values().stream()
                .anyMatch(scenario -> scenario.getType() == template.getType());
            if (hasActiveOfType) {
                continue;
            }

            if (random.nextDouble() < template.getTriggerProbability()
                && canTriggerScenario(template, npcs, nearbyPlayers)) {
                startScenario(template, npcs, nearbyPlayers);
            }
        }
    }

    /**
     * Porneste un scenariu nou.
     */
    private void startScenario(ScenarioTemplate template, List<AINPC> npcs, List<Player> players) {
        UUID scenarioId = UUID.randomUUID();
        ActiveScenario scenario = new ActiveScenario(scenarioId, template);

        if (!assignRoles(scenario, template, npcs, players)) {
            plugin.debug("Scenariul " + template.getDisplayName() + " nu a putut asigna toate rolurile obligatorii.");
            return;
        }

        if (!template.getPhases().isEmpty()) {
            scenario.setCurrentPhase(template.getPhases().get(0));
        }

        activeScenarios.put(scenarioId, scenario);

        plugin.getLogger().info("Scenariu nou pornit: " + template.getDisplayName()
            + " (ID: " + scenarioId.toString().substring(0, 8) + ")");

        notifyParticipants(scenario);
    }

    /**
     * Asigneaza roluri NPC-urilor si jucatorilor.
     */
    private boolean assignRoles(ActiveScenario scenario,
                                ScenarioTemplate template,
                                List<AINPC> npcs,
                                List<Player> players) {
        List<AINPC> availableNpcs = new ArrayList<>(npcs);
        List<Player> availablePlayers = new ArrayList<>(players);
        Random random = new Random();

        for (ScenarioRoleRule role : template.getPlayerRoles()) {
            if (availablePlayers.isEmpty()) {
                if (!role.isOptional()) {
                    plugin.debug("Scenariul " + template.getDisplayName() + " nu are jucator pentru rolul " + role.getId());
                    return false;
                }
                continue;
            }

            Player player = availablePlayers.remove(0);
            scenario.assignPlayerRole(player.getUniqueId(), role.getId());
        }

        List<ScenarioRoleRule> mandatoryFallback = new ArrayList<>();
        List<ScenarioRoleRule> optionalFallback = new ArrayList<>();

        for (ScenarioRoleRule role : template.getNpcRoles()) {
            AINPC selected = selectBestNpcForRole(availableNpcs, role);
            if (selected != null) {
                scenario.assignNPCRole(selected.getUuid(), role.getId());
                availableNpcs.remove(selected);
                continue;
            }

            if (role.isOptional()) {
                optionalFallback.add(role);
            } else {
                mandatoryFallback.add(role);
            }
        }

        if (!assignFallbackRoles(scenario, mandatoryFallback, availableNpcs, random, true)) {
            return false;
        }
        assignFallbackRoles(scenario, optionalFallback, availableNpcs, random, false);
        return true;
    }

    private boolean assignFallbackRoles(ActiveScenario scenario,
                                        List<ScenarioRoleRule> roles,
                                        List<AINPC> availableNpcs,
                                        Random random,
                                        boolean mandatory) {
        for (ScenarioRoleRule role : roles) {
            if (availableNpcs.isEmpty()) {
                return !mandatory;
            }

            if (role.hasHardRequirements()) {
                AINPC selected = selectBestNpcForRole(availableNpcs, role);
                if (selected == null) {
                    if (mandatory) {
                        return false;
                    }
                    continue;
                }

                scenario.assignNPCRole(selected.getUuid(), role.getId());
                availableNpcs.remove(selected);
                continue;
            }

            AINPC randomNpc = availableNpcs.remove(random.nextInt(availableNpcs.size()));
            scenario.assignNPCRole(randomNpc.getUuid(), role.getId());
        }

        return true;
    }

    private ConfigurationSection getQuestSettings() {
        return ScenarioSimpleQuestKt.getQuestSettings();
    }

    /**
     * Notifica participantii la scenariu.
     */
    private void notifyParticipants(ActiveScenario scenario) {
        for (Map.Entry<UUID, String> entry : scenario.getNpcRoles().entrySet()) {
            AINPC npc = plugin.getNpcManager().getNPCByUUID(entry.getKey());
            if (npc != null) {
                adjustEmotionsForRole(npc, entry.getValue(), scenario.getType());
            }
        }

        for (Map.Entry<UUID, String> entry : scenario.getPlayerRoles().entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                sendScenarioHint(player, scenario);
                if (scenario.hasQuestBriefing()) {
                    sendQuestBriefing(player, scenario);
                }
            }
        }
    }

    /**
     * Ajusteaza emotiile NPC-ului bazat pe rol.
     */
    private void adjustEmotionsForRole(AINPC npc, String role, ScenarioType type) {
        NPCEmotions emotions = npc.getEmotions();

        switch (role) {
            case "THIEF" -> emotions.adjustFear(0.3);
            case "VICTIM" -> {
                emotions.adjustAnger(0.4);
                emotions.adjustSadness(0.3);
            }
            case "AGGRESSOR", "ANTAGONIST" -> emotions.adjustAnger(0.5);
            case "HOST", "QUEST_GIVER" -> emotions.adjustHappiness(0.3);
            case "HELPER" -> {
                emotions.applyEmotion("trust", 0.7);
                emotions.applyEmotion("anticipation", 0.6);
            }
            case "SUITOR" -> {
                emotions.adjustHappiness(0.3);
                emotions.adjustFear(0.2);
            }
            default -> {
            }
        }
    }

    /**
     * Trimite un indiciu jucatorului despre scenariu.
     */
    private void sendScenarioHint(Player player, ActiveScenario scenario) {
        String hint = scenario.getHint();
        if (hint == null || hint.isBlank()) {
            hint = switch (scenario.getType()) {
                case QUEST -> "Simti ca cineva are nevoie de ajutorul tau...";
                case CONFLICT -> "Tensiunea din aer e palpabila...";
                case CELEBRATION -> "Se aude muzica si rasete in apropiere!";
                case EMERGENCY -> "Ceva nu e in regula...";
                case ROMANCE -> "Aerul e plin de emotie...";
                default -> "";
            };
        }

        if (!hint.isEmpty()) {
            player.sendMessage("§7§o" + hint);
        }
    }

    /**
     * Avanseaza un scenariu la urmatoarea faza.
     */
    private void sendQuestBriefing(Player player, ActiveScenario scenario) {
        String questTitle = scenario.getQuestCode().isBlank()
            ? scenario.getDisplayName()
            : scenario.getQuestCode() + " - " + scenario.getDisplayName();
        player.sendMessage("\u00A76[Quest] \u00A7f" + questTitle);

        String questGiver = resolveProfessionName(scenario.getQuestGiverProfession());
        if (!questGiver.isBlank()) {
            player.sendMessage("\u00A77Misiune de la: \u00A7f" + questGiver);
        }

        if (!scenario.getObjectives().isEmpty()) {
            player.sendMessage("\u00A7eObiective:");
            for (FeaturePackLoader.QuestEntryDefinition objective : scenario.getObjectives()) {
                player.sendMessage("\u00A77- \u00A7f" + formatQuestEntry(objective));
            }
        }

        if (!scenario.getRewards().isEmpty()) {
            player.sendMessage("\u00A7aRecompensa:");
            for (FeaturePackLoader.QuestEntryDefinition reward : scenario.getRewards()) {
                player.sendMessage("\u00A77- \u00A7f" + formatQuestEntry(reward));
            }
        }
    }

    private String resolveProfessionName(String professionReference) {
        if (professionReference == null || professionReference.isBlank()) {
            return "";
        }

        FeaturePackLoader loader = plugin.getFeaturePackLoader();
        if (loader != null) {
            FeaturePackLoader.ProfessionDefinition definition = loader.findProfessionDefinition(professionReference);
            if (definition != null && definition.getName() != null && !definition.getName().isBlank()) {
                return definition.getName();
            }
        }

        return professionReference;
    }

    public void advanceScenario(UUID scenarioId) {
        ActiveScenario scenario = activeScenarios.get(scenarioId);
        if (scenario == null) {
            return;
        }

        ScenarioTemplate template = scenarioTemplates.get(scenario.getType());
        if (template == null) {
            return;
        }

        List<String> phases = template.getPhases();
        int currentIndex = phases.indexOf(scenario.getCurrentPhase());

        if (currentIndex < phases.size() - 1) {
            scenario.setCurrentPhase(phases.get(currentIndex + 1));
            plugin.debug("Scenariu " + scenarioId.toString().substring(0, 8)
                + " avansat la faza: " + scenario.getCurrentPhase());
        } else {
            endScenario(scenarioId);
        }
    }

    /**
     * Termina un scenariu.
     */
    public void endScenario(UUID scenarioId) {
        ActiveScenario scenario = activeScenarios.remove(scenarioId);
        if (scenario == null) {
            return;
        }

        createScenarioMemories(scenario);

        plugin.getLogger().info("Scenariu terminat: " + scenario.getDisplayName()
            + " (ID: " + scenarioId.toString().substring(0, 8) + ")");
    }

    /**
     * Creeaza amintiri despre scenariu pentru participanti.
     */
    private void createScenarioMemories(ActiveScenario scenario) {
        for (UUID npcId : scenario.getNpcRoles().keySet()) {
            plugin.getMemoryManager().addScenarioMemory(
                npcId,
                scenario.getType().name(),
                scenario.getNpcRoles().get(npcId)
            );
        }
    }

    public Map<UUID, ActiveScenario> getActiveScenarios() {
        return new HashMap<>(activeScenarios);
    }

    public ActiveScenario getNPCScenario(UUID npcId) {
        for (ActiveScenario scenario : activeScenarios.values()) {
            if (scenario.hasNPCRole(npcId)) {
                return scenario;
            }
        }
        return null;
    }

    public enum ScenarioType {
        THEFT("Furt"),
        CONFLICT("Conflict"),
        CELEBRATION("Sarbatoare"),
        EMERGENCY("Urgenta"),
        ROMANCE("Romantism"),
        TRADE_DEAL("Afacere"),
        DUTY("Sarcina"),
        BOUNTY("Bounty local"),
        WORLD_EVENT("Eveniment local"),
        TUTORIAL("Tutorial"),
        RITUAL("Ritual"),
        QUEST("Misiune"),
        GOSSIP_SPREAD("Raspandirea zvonurilor");

        private final String displayName;

        ScenarioType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static ScenarioType fromId(String value) {
            if (value == null || value.isBlank()) {
                return QUEST;
            }

            for (ScenarioType type : values()) {
                if (type.name().equalsIgnoreCase(value) || type.displayName.equalsIgnoreCase(value)) {
                    return type;
                }
            }

            String normalized = value.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(" ", "_");
            if ("datorie".equals(normalized) || "sarcina".equals(normalized) || "npc_duty".equals(normalized)) {
                return DUTY;
            }
            if ("bounty".equals(normalized) || "bounties".equals(normalized)
                || "local_bounty".equals(normalized) || "local_bounties".equals(normalized)
                || "recompensa".equals(normalized) || "recompense".equals(normalized)) {
                return BOUNTY;
            }
            if ("event".equals(normalized) || "events".equals(normalized)
                || "eveniment".equals(normalized) || "evenimente".equals(normalized)
                || "world_event".equals(normalized) || "local_event".equals(normalized)
                || "village_event".equals(normalized) || "village_events".equals(normalized)) {
                return WORLD_EVENT;
            }
            if ("tutorial".equals(normalized) || "tutorials".equals(normalized)
                || "onboarding".equals(normalized) || "indrumare".equals(normalized)) {
                return TUTORIAL;
            }
            if ("ritual".equals(normalized) || "rituals".equals(normalized)
                || "ceremony".equals(normalized) || "ceremonies".equals(normalized)
                || "ceremonie".equals(normalized) || "ceremonii".equals(normalized)
                || "village_ritual".equals(normalized) || "village_rituals".equals(normalized)) {
                return RITUAL;
            }

            return QUEST;
        }
    }

    public static class ScenarioTemplate {
        private final ScenarioType type;
        private final Map<String, ScenarioRoleRule> roles;
        private final List<String> phases;
        private String templateId;
        private String displayName;
        private String description;
        private String sourcePackId;
        private String hint;
        private List<String> preferredTopologies;
        private List<String> narrativeHints;
        private boolean progressionEnabled;
        private String progressionMechanicId;
        private String progressionKind;
        private String progressionLabel;
        private String progressionSingularLabel;
        private String progressionPluralLabel;
        private int progressionMaxActive;
        private String questCode;
        private String questGiverProfession;
        private List<String> questPrerequisites;
        private boolean questRepeatable;
        private long questCooldownSeconds;
        private Map<String, List<String>> questDialogues;
        private List<FeaturePackLoader.QuestStageDefinition> questStages;
        private QuestScenarioContract questContract;
        private List<FeaturePackLoader.QuestEntryDefinition> objectives;
        private List<FeaturePackLoader.QuestEntryDefinition> rewards;
        private double triggerProbability;
        private int minimumNpcCount;
        private boolean requiresPlayer;

        public ScenarioTemplate(ScenarioType type) {
            this.type = type;
            this.roles = new LinkedHashMap<>();
            this.phases = new ArrayList<>();
            this.templateId = type.name().toLowerCase(Locale.ROOT);
            this.displayName = type.getDisplayName();
            this.description = "";
            this.sourcePackId = "core";
            this.hint = "";
            this.preferredTopologies = new ArrayList<>();
            this.narrativeHints = new ArrayList<>();
            this.progressionEnabled = type == ScenarioType.QUEST;
            this.progressionMechanicId = type == ScenarioType.QUEST ? "quest" : "";
            this.progressionKind = type == ScenarioType.QUEST ? "quest" : "";
            this.progressionLabel = type == ScenarioType.QUEST ? "Quest" : "";
            this.progressionSingularLabel = "quest";
            this.progressionPluralLabel = "questuri";
            this.progressionMaxActive = 0;
            this.questCode = "";
            this.questGiverProfession = "";
            this.questPrerequisites = new ArrayList<>();
            this.questRepeatable = false;
            this.questCooldownSeconds = 0L;
            this.questDialogues = new LinkedHashMap<>();
            this.questStages = new ArrayList<>();
            this.questContract = QuestScenarioContract.defaultContract();
            this.objectives = new ArrayList<>();
            this.rewards = new ArrayList<>();
            this.triggerProbability = 0.05;
            this.minimumNpcCount = 2;
            this.requiresPlayer = false;
        }

        public void addRole(String roleId, String description) {
            addRole(roleId, description, false);
        }

        public void addRole(String roleId, String description, boolean optional) {
            addRole(new ScenarioRoleRule(roleId, description, false, optional));
        }

        public void addPlayerRole(String roleId, String description) {
            addRole(new ScenarioRoleRule(roleId, description, true, false));
        }

        public void addRole(ScenarioRoleRule role) {
            roles.put(role.getId(), role);
        }

        public void addPhase(String phaseId, String description) {
            phases.add(phaseId);
        }

        public ScenarioType getType() { return type; }
        public Map<String, ScenarioRoleRule> getRoles() { return roles; }
        public List<String> getPhases() { return phases; }
        public double getTriggerProbability() { return triggerProbability; }
        public void setTriggerProbability(double triggerProbability) { this.triggerProbability = triggerProbability; }
        public int getMinimumNpcCount() { return minimumNpcCount; }
        public void setMinimumNpcCount(int minimumNpcCount) { this.minimumNpcCount = minimumNpcCount; }
        public boolean requiresPlayer() { return requiresPlayer; }
        public void setRequiresPlayer(boolean requiresPlayer) { this.requiresPlayer = requiresPlayer; }
        public String getTemplateId() { return templateId; }
        public void setTemplateId(String templateId) { this.templateId = templateId; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getSourcePackId() { return sourcePackId; }
        public void setSourcePackId(String sourcePackId) { this.sourcePackId = sourcePackId; }
        public String getHint() { return hint; }
        public void setHint(String hint) { this.hint = hint == null ? "" : hint; }
        public List<String> getPreferredTopologies() { return preferredTopologies; }
        public void setPreferredTopologies(List<String> preferredTopologies) {
            this.preferredTopologies = preferredTopologies != null ? preferredTopologies : new ArrayList<>();
        }
        public List<String> getNarrativeHints() { return narrativeHints; }
        public void setNarrativeHints(List<String> narrativeHints) {
            this.narrativeHints = narrativeHints != null ? narrativeHints : new ArrayList<>();
        }
        public boolean isProgressionEnabled() { return progressionEnabled; }
        public void setProgressionEnabled(boolean progressionEnabled) { this.progressionEnabled = progressionEnabled; }
        public String getProgressionMechanicId() { return progressionMechanicId; }
        public void setProgressionMechanicId(String progressionMechanicId) {
            this.progressionMechanicId = progressionMechanicId == null ? "" : progressionMechanicId;
        }
        public String getProgressionKind() { return progressionKind; }
        public void setProgressionKind(String progressionKind) {
            this.progressionKind = progressionKind == null ? "" : progressionKind;
        }
        public String getProgressionLabel() { return progressionLabel; }
        public void setProgressionLabel(String progressionLabel) {
            this.progressionLabel = progressionLabel == null ? "" : progressionLabel;
        }
        public String getProgressionSingularLabel() { return progressionSingularLabel; }
        public void setProgressionSingularLabel(String progressionSingularLabel) {
            this.progressionSingularLabel = progressionSingularLabel == null ? "" : progressionSingularLabel;
        }
        public String getProgressionPluralLabel() { return progressionPluralLabel; }
        public void setProgressionPluralLabel(String progressionPluralLabel) {
            this.progressionPluralLabel = progressionPluralLabel == null ? "" : progressionPluralLabel;
        }
        public int getProgressionMaxActive() { return progressionMaxActive; }
        public void setProgressionMaxActive(int progressionMaxActive) {
            this.progressionMaxActive = Math.max(0, progressionMaxActive);
        }
        public String getQuestCode() { return questCode; }
        public void setQuestCode(String questCode) { this.questCode = questCode == null ? "" : questCode; }
        public String getQuestGiverProfession() { return questGiverProfession; }
        public void setQuestGiverProfession(String questGiverProfession) {
            this.questGiverProfession = questGiverProfession == null ? "" : questGiverProfession;
        }
        public List<String> getQuestPrerequisites() { return questPrerequisites; }
        public void setQuestPrerequisites(List<String> questPrerequisites) {
            this.questPrerequisites = questPrerequisites != null ? new ArrayList<>(questPrerequisites) : new ArrayList<>();
        }
        public boolean isQuestRepeatable() { return questRepeatable; }
        public void setQuestRepeatable(boolean questRepeatable) { this.questRepeatable = questRepeatable; }
        public long getQuestCooldownSeconds() { return questCooldownSeconds; }
        public void setQuestCooldownSeconds(long questCooldownSeconds) {
            this.questCooldownSeconds = Math.max(0L, questCooldownSeconds);
        }
        public Map<String, List<String>> getQuestDialogues() { return questDialogues; }
        public void setQuestDialogues(Map<String, List<String>> questDialogues) {
            this.questDialogues = new LinkedHashMap<>();
            if (questDialogues == null) {
                return;
            }

            for (Map.Entry<String, List<String>> entry : questDialogues.entrySet()) {
                String key = normalizeQuestDialogueKey(entry.getKey());
                List<String> lines = entry.getValue();
                if (!key.isBlank() && lines != null && !lines.isEmpty()) {
                    this.questDialogues.put(key, List.copyOf(lines));
                }
            }
        }
        public List<String> getQuestDialogueLines(String key) {
            return questDialogues.getOrDefault(normalizeQuestDialogueKey(key), List.of());
        }
        public List<FeaturePackLoader.QuestStageDefinition> getQuestStages() { return questStages; }
        public void setQuestStages(List<FeaturePackLoader.QuestStageDefinition> questStages) {
            this.questStages = questStages != null ? new ArrayList<>(questStages) : new ArrayList<>();
        }
        public QuestScenarioContract getQuestContract() { return questContract; }
        public void setQuestContract(QuestScenarioContract questContract) {
            this.questContract = questContract != null ? questContract : QuestScenarioContract.defaultContract();
        }
        public List<FeaturePackLoader.QuestEntryDefinition> getObjectives() { return objectives; }
        public void setObjectives(List<FeaturePackLoader.QuestEntryDefinition> objectives) {
            this.objectives = objectives != null ? new ArrayList<>(objectives) : new ArrayList<>();
        }
        public List<FeaturePackLoader.QuestEntryDefinition> getRewards() { return rewards; }
        public void setRewards(List<FeaturePackLoader.QuestEntryDefinition> rewards) {
            this.rewards = rewards != null ? new ArrayList<>(rewards) : new ArrayList<>();
        }
        public boolean hasQuestBriefing() {
            return !questCode.isBlank() || !objectives.isEmpty() || !rewards.isEmpty();
        }

        private static String normalizeQuestDialogueKey(String key) {
            return key == null ? "" : key.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        }

        public List<ScenarioRoleRule> getNpcRoles() {
            return roles.values().stream()
                .filter(role -> !role.isPlayerRole())
                .sorted(Comparator.comparing(ScenarioRoleRule::isOptional)
                    .thenComparing(role -> !role.hasHardRequirements())
                    .thenComparing(ScenarioRoleRule::getId))
                .toList();
        }

        public List<ScenarioRoleRule> getPlayerRoles() {
            return roles.values().stream()
                .filter(ScenarioRoleRule::isPlayerRole)
                .toList();
        }
    }

    public static class ScenarioRoleRule {
        private final String id;
        private final String description;
        private final boolean playerRole;
        private final boolean optional;
        private List<String> requiredProfessions;
        private List<String> preferredProfessions;
        private List<String> requiredTraits;
        private List<String> preferredTraits;

        public ScenarioRoleRule(String id, String description, boolean playerRole, boolean optional) {
            this.id = id;
            this.description = description;
            this.playerRole = playerRole;
            this.optional = optional;
            this.requiredProfessions = new ArrayList<>();
            this.preferredProfessions = new ArrayList<>();
            this.requiredTraits = new ArrayList<>();
            this.preferredTraits = new ArrayList<>();
        }

        public String getId() { return id; }
        public String getDescription() { return description; }
        public boolean isPlayerRole() { return playerRole; }
        public boolean isOptional() { return optional; }
        public List<String> getRequiredProfessions() { return requiredProfessions; }
        public void setRequiredProfessions(List<String> requiredProfessions) {
            this.requiredProfessions = requiredProfessions != null ? requiredProfessions : Collections.emptyList();
        }
        public List<String> getPreferredProfessions() { return preferredProfessions; }
        public void setPreferredProfessions(List<String> preferredProfessions) {
            this.preferredProfessions = preferredProfessions != null ? preferredProfessions : Collections.emptyList();
        }
        public List<String> getRequiredTraits() { return requiredTraits; }
        public void setRequiredTraits(List<String> requiredTraits) {
            this.requiredTraits = requiredTraits != null ? requiredTraits : Collections.emptyList();
        }
        public List<String> getPreferredTraits() { return preferredTraits; }
        public void setPreferredTraits(List<String> preferredTraits) {
            this.preferredTraits = preferredTraits != null ? preferredTraits : Collections.emptyList();
        }
        public boolean hasHardRequirements() {
            return !requiredProfessions.isEmpty() || !requiredTraits.isEmpty();
        }
    }

    public static class ActiveScenario {
        private final UUID id;
        private final ScenarioType type;
        private final String templateId;
        private final String displayName;
        private final String hint;
        private final String questCode;
        private final String questGiverProfession;
        private final List<FeaturePackLoader.QuestEntryDefinition> objectives;
        private final List<FeaturePackLoader.QuestEntryDefinition> rewards;
        private final Map<UUID, String> npcRoles;
        private final Map<UUID, String> playerRoles;
        private String currentPhase;
        private final long startTime;

        public ActiveScenario(UUID id, ScenarioTemplate template) {
            this.id = id;
            this.type = template.getType();
            this.templateId = template.getTemplateId();
            this.displayName = template.getDisplayName();
            this.hint = template.getHint();
            this.questCode = template.getQuestCode();
            this.questGiverProfession = template.getQuestGiverProfession();
            this.objectives = new ArrayList<>(template.getObjectives());
            this.rewards = new ArrayList<>(template.getRewards());
            this.npcRoles = new HashMap<>();
            this.playerRoles = new HashMap<>();
            this.startTime = System.currentTimeMillis();
        }

        public void assignNPCRole(UUID npcId, String role) {
            npcRoles.put(npcId, role);
        }

        public void assignPlayerRole(UUID playerId, String role) {
            playerRoles.put(playerId, role);
        }

        public boolean hasNPCRole(UUID npcId) {
            return npcRoles.containsKey(npcId);
        }

        public UUID getId() { return id; }
        public ScenarioType getType() { return type; }
        public String getTemplateId() { return templateId; }
        public String getDisplayName() { return displayName; }
        public String getHint() { return hint; }
        public String getQuestCode() { return questCode; }
        public String getQuestGiverProfession() { return questGiverProfession; }
        public List<FeaturePackLoader.QuestEntryDefinition> getObjectives() { return objectives; }
        public List<FeaturePackLoader.QuestEntryDefinition> getRewards() { return rewards; }
        public boolean hasQuestBriefing() {
            return !questCode.isBlank() || !objectives.isEmpty() || !rewards.isEmpty();
        }
        public Map<UUID, String> getNpcRoles() { return npcRoles; }
        public Map<UUID, String> getPlayerRoles() { return playerRoles; }
        public String getCurrentPhase() { return currentPhase; }
        public void setCurrentPhase(String currentPhase) { this.currentPhase = currentPhase; }
        public long getStartTime() { return startTime; }
    }

    public record QuestGuiSnapshot(
        boolean handled,
        String playerName,
        String filterLabel,
        List<String> summaryLines,
        List<QuestGuiEntry> currentEntries,
        List<QuestGuiEntry> archivedEntries,
        long totalMatchingArchived
    ) {
        public QuestGuiSnapshot {
            playerName = playerName == null ? "" : playerName;
            filterLabel = filterLabel == null ? "" : filterLabel;
            summaryLines = List.copyOf(summaryLines != null ? summaryLines : List.of());
            currentEntries = List.copyOf(currentEntries != null ? currentEntries : List.of());
            archivedEntries = List.copyOf(archivedEntries != null ? archivedEntries : List.of());
            totalMatchingArchived = Math.max(0L, totalMatchingArchived);
        }

        public static QuestGuiSnapshot empty() {
            return new QuestGuiSnapshot(false, "", "", List.of(), List.of(), List.of(), 0L);
        }

        public List<QuestGuiEntry> allEntries() {
            List<QuestGuiEntry> entries = new ArrayList<>(currentEntries);
            entries.addAll(archivedEntries);
            return List.copyOf(entries);
        }
    }

    public record QuestGuiEntry(
        String selector,
        String templateId,
        String questCode,
        String title,
        String statusDisplay,
        String categoryDisplay,
        String mechanicDisplay,
        boolean tracked,
        boolean current,
        boolean active,
        boolean offered,
        boolean archived,
        boolean missingTemplate,
        String currentStageId,
        String currentStageLabel,
        long updatedAt,
        String questGiverName,
        List<String> statusLines,
        List<QuestGuiObjective> objectives,
        List<QuestGuiStage> stages,
        List<String> rewardLines,
        List<String> actionLines
    ) {
        public QuestGuiEntry {
            selector = selector == null ? "" : selector;
            templateId = templateId == null ? "" : templateId;
            questCode = questCode == null ? "" : questCode;
            title = title == null ? "" : title;
            statusDisplay = statusDisplay == null ? "" : statusDisplay;
            categoryDisplay = categoryDisplay == null ? "" : categoryDisplay;
            mechanicDisplay = mechanicDisplay == null ? "" : mechanicDisplay;
            currentStageId = currentStageId == null ? "" : currentStageId;
            currentStageLabel = currentStageLabel == null ? "" : currentStageLabel;
            questGiverName = questGiverName == null ? "" : questGiverName;
            statusLines = List.copyOf(statusLines != null ? statusLines : List.of());
            objectives = List.copyOf(objectives != null ? objectives : List.of());
            stages = List.copyOf(stages != null ? stages : List.of());
            rewardLines = List.copyOf(rewardLines != null ? rewardLines : List.of());
            actionLines = List.copyOf(actionLines != null ? actionLines : List.of());
        }
    }

    public record QuestGuiObjective(
        String key,
        String type,
        String label,
        String description,
        String stageId,
        String stageLabel,
        String stateId,
        String stateDisplay,
        int currentAmount,
        int requiredAmount,
        boolean complete,
        boolean active
    ) {
        public QuestGuiObjective(String key,
                                 String type,
                                 String label,
                                 String description,
                                 String stageId,
                                 String stageLabel,
                                 int currentAmount,
                                 int requiredAmount,
                                 boolean complete,
                                 boolean active) {
            this(
                key,
                type,
                label,
                description,
                stageId,
                stageLabel,
                legacyObjectiveState(currentAmount, requiredAmount, complete, active).id(),
                legacyObjectiveState(currentAmount, requiredAmount, complete, active).displayName(),
                currentAmount,
                requiredAmount,
                complete,
                active
            );
        }

        public QuestGuiObjective {
            key = key == null ? "" : key;
            type = type == null ? "" : type;
            label = label == null ? "" : label;
            description = description == null ? "" : description;
            stageId = stageId == null ? "" : stageId;
            stageLabel = stageLabel == null ? "" : stageLabel;
            stateId = stateId == null ? "" : stateId;
            stateDisplay = stateDisplay == null ? "" : stateDisplay;
            currentAmount = Math.max(0, currentAmount);
            requiredAmount = Math.max(1, requiredAmount);
        }

        private static QuestObjectiveState legacyObjectiveState(int currentAmount,
                                                                int requiredAmount,
                                                                boolean complete,
                                                                boolean active) {
            if (complete || Math.max(0, currentAmount) >= Math.max(1, requiredAmount)) {
                return QuestObjectiveState.COMPLETED;
            }
            if (!active) {
                return QuestObjectiveState.PENDING;
            }
            if (currentAmount > 0) {
                return QuestObjectiveState.IN_PROGRESS;
            }
            return QuestObjectiveState.STARTED;
        }
    }

    public record QuestGuiStage(
        String id,
        String label,
        String description,
        String completionMode,
        String nextStageId,
        boolean active,
        boolean complete,
        List<String> objectiveIds
    ) {
        public QuestGuiStage {
            id = id == null ? "" : id;
            label = label == null ? "" : label;
            description = description == null ? "" : description;
            completionMode = completionMode == null ? "" : completionMode;
            nextStageId = nextStageId == null ? "" : nextStageId;
            objectiveIds = List.copyOf(objectiveIds != null ? objectiveIds : List.of());
        }
    }

    public record QuestTrackingMarker(
        String objectiveLabel,
        String targetLabel,
        String anchorType,
        Location location,
        String actionBarMessage
    ) {
        public QuestTrackingMarker {
            objectiveLabel = objectiveLabel == null ? "" : objectiveLabel;
            targetLabel = targetLabel == null ? "" : targetLabel;
            anchorType = anchorType == null ? "" : anchorType;
            location = location != null ? location.clone() : null;
            actionBarMessage = actionBarMessage == null ? "" : actionBarMessage;
        }

        public boolean hasLocation() {
            return location != null && location.getWorld() != null;
        }
    }

    public static class QuestInteractionResult {
        private final boolean handled;
        private final boolean openConversation;
        private final List<String> npcMessages;
        private final List<String> systemMessages;

        private QuestInteractionResult(boolean handled,
                                       boolean openConversation,
                                       List<String> npcMessages,
                                       List<String> systemMessages) {
            this.handled = handled;
            this.openConversation = openConversation;
            this.npcMessages = npcMessages != null ? List.copyOf(npcMessages) : List.of();
            this.systemMessages = systemMessages != null ? List.copyOf(systemMessages) : List.of();
        }

        public static QuestInteractionResult notHandled() {
            return new QuestInteractionResult(false, false, List.of(), List.of());
        }

        public static QuestInteractionResult handled(boolean openConversation,
                                                     List<String> npcMessages,
                                                     List<String> systemMessages) {
            return new QuestInteractionResult(true, openConversation, npcMessages, systemMessages);
        }

        public boolean isHandled() { return handled; }
        public boolean shouldOpenConversation() { return openConversation; }
        public List<String> getNpcMessages() { return npcMessages; }
        public List<String> getSystemMessages() { return systemMessages; }
    }
}
