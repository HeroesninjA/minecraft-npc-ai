package ro.ainpc.engine;

import static ro.ainpc.engine.QuestTrackingModelsKt.center;
import static ro.ainpc.engine.QuestTrackingModelsKt.formatHorizontalDirection;
import static ro.ainpc.engine.QuestTrackingModelsKt.formatQuestAnchorType;
import static ro.ainpc.engine.QuestTrackingModelsKt.formatQuestPhase;
import static ro.ainpc.engine.QuestTrackingModelsKt.formatQuestTrackingCoordinates;
import static ro.ainpc.engine.QuestTrackingModelsKt.formatVerticalHint;
import static ro.ainpc.engine.QuestTrackingModelsKt.normalizeTrackingAnchorType;
import static ro.ainpc.engine.ScenarioEngineTextKt.applyQuestFallbackPlaceholders;
import static ro.ainpc.engine.ScenarioEngineTextKt.capitalizeProgressionLabel;
import static ro.ainpc.engine.ScenarioEngineTextKt.describeQuestProgress;
import static ro.ainpc.engine.ScenarioEngineTextKt.formatDuration;
import static ro.ainpc.engine.ScenarioEngineTextKt.formatMissingObjective;
import static ro.ainpc.engine.ScenarioEngineTextKt.formatObjectiveProgressLabel;
import static ro.ainpc.engine.ScenarioEngineTextKt.formatObjectiveTargetLabel;
import static ro.ainpc.engine.ScenarioEngineTextKt.formatOptional;
import static ro.ainpc.engine.ScenarioEngineTextKt.formatQuestDebugMap;
import static ro.ainpc.engine.ScenarioEngineTextKt.formatQuestDebugTime;
import static ro.ainpc.engine.ScenarioEngineTextKt.formatQuestLogMechanicCounts;
import static ro.ainpc.engine.ScenarioEngineTextKt.formatQuestAmount;
import static ro.ainpc.engine.ScenarioEngineTextKt.formatQuestEntry;
import static ro.ainpc.engine.ScenarioEngineTextKt.formatStageCompletionMode;
import static ro.ainpc.engine.ScenarioEngineTextKt.formatQuestStatus;
import static ro.ainpc.engine.ScenarioEngineTextKt.humanizeItemId;
import static ro.ainpc.engine.ScenarioEngineTextKt.joinNaturally;
import static ro.ainpc.engine.ScenarioEngineTextKt.resolveQuestMaterial;
import static ro.ainpc.engine.ScenarioEngineTextKt.resolveQuestTitle;
import static ro.ainpc.engine.ScenarioEngineTextKt.sanitizeConfigKey;
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
import static ro.ainpc.engine.ScenarioObjectiveProgressKt.consumeQuestObjectives;
import static ro.ainpc.engine.ScenarioObjectiveProgressKt.inspectQuestInventory;
import static ro.ainpc.engine.ScenarioObjectiveProgressKt.inspectQuestRewardDelivery;
import static ro.ainpc.engine.ScenarioObjectiveProgressKt.resolveObjectiveCurrentProgress;
import static ro.ainpc.engine.ScenarioObjectiveProgressKt.simulateQuestObjectiveConsumption;
import static ro.ainpc.engine.ScenarioObjectiveProgressKt.simulateRemoveMaterial;
import static ro.ainpc.engine.ScenarioObjectiveProgressKt.simulateAddMaterial;
import static ro.ainpc.engine.QuestLogFilterKt.parseQuestLogFilter;
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

        if (requiresQuestGiverTurnIn(template) && !matchesQuestGiver(npc, template)) {
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
                && matchesQuestGiver(fallbackNpc, fallbackTemplate)) {
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

    private boolean requiresQuestGiverTurnIn(ScenarioTemplate template) {
        QuestScenarioContract contract = template != null ? template.getQuestContract() : null;
        return contract == null || contract.completionMode() == QuestScenarioContract.CompletionMode.RETURN_TO_GIVER;
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
        String mechanic = template != null ? resolveProgressionMechanicDisplay(template) : "Necunoscuta";
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

    private String findObjectiveStageId(ScenarioTemplate template, FeaturePackLoader.QuestEntryDefinition objective) {
        String explicitStage = canonicalQuestPhase(template, getObjectiveStage(objective));
        if (!explicitStage.isBlank()) {
            return explicitStage;
        }
        if (template == null || objective == null || template.getQuestStages().isEmpty()) {
            return "";
        }
        for (FeaturePackLoader.QuestStageDefinition stage : template.getQuestStages()) {
            if (stageReferencesObjective(stage, objective)) {
                return stage.getId();
            }
        }
        return "";
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

        systemMessages.add("&e" + capitalizeProgressionLabel(resolveProgressionSingularLabel(template)) + ": &f" + resolveQuestTitle(template));
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

    private QuestDialogueContext resolveInitialQuestDialogueContext(ScenarioTemplate template) {
        return shouldAutoAcceptOnOffer(template) ? QuestDialogueContext.ACCEPTED : QuestDialogueContext.OFFER;
    }

    private List<String> buildInitialQuestNpcFallbackMessages(ScenarioTemplate template) {
        return shouldAutoAcceptOnOffer(template)
            ? List.of("Bine. Ma bazez pe tine.", "Intoarce-te cand ai terminat.")
            : List.of("Am o treaba pentru tine.", buildQuestOfferMessage(template));
    }

    private boolean shouldAutoAcceptOnOffer(ScenarioTemplate template) {
        return template != null
            && template.getQuestContract() != null
            && template.getQuestContract().autoAcceptOnOffer();
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
        int categoryLimit = getQuestCategoryLimit(category);
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
                    + resolveProgressionPluralLabel(template) + " curente in "
                    + resolveProgressionMechanicDisplay(template) + " (limita " + mechanicLimit + ").");
            }
        }

        return issues.isEmpty()
            ? QuestAvailability.allowed()
            : QuestAvailability.unavailable(issues);
    }

    private QuestScenarioContract.Category resolveQuestCategory(ScenarioTemplate template) {
        QuestScenarioContract contract = template != null ? template.getQuestContract() : null;
        return contract != null ? contract.category() : QuestScenarioContract.Category.SIDE;
    }

    private int getQuestCategoryLimit(QuestScenarioContract.Category category) {
        String key = switch (category != null ? category : QuestScenarioContract.Category.SIDE) {
            case MAIN -> "main";
            case SIDE -> "side";
            case REPEATABLE -> "repeatable";
        };
        int defaultLimit = switch (category != null ? category : QuestScenarioContract.Category.SIDE) {
            case MAIN -> 1;
            case SIDE -> 3;
            case REPEATABLE -> 2;
        };
        return Math.max(0, plugin.getConfig().getInt("quest.max_active." + key, defaultLimit));
    }

    private int getProgressionMechanicLimit(ScenarioTemplate template) {
        if (template == null || !template.isProgressionEnabled()
            || template.getProgressionMechanicId().isBlank()) {
            return 0;
        }

        if (template.getProgressionMaxActive() > 0) {
            return template.getProgressionMaxActive();
        }

        FeaturePackLoader.ProgressionMechanicDefinition mechanic = resolveProgressionMechanicDefinition(template);
        if (mechanic == null || !mechanic.isProgressEnabled()) {
            return 0;
        }
        return mechanic.getMaxActive();
    }

    private int countCurrentProgressionsInMechanic(UUID playerId,
                                                   ScenarioTemplate template,
                                                   String excludedTemplateId) {
        String mechanicKey = resolveProgressionMechanicKey(template);
        if (mechanicKey.isBlank()) {
            return 0;
        }

        int count = 0;
        for (PlayerQuestProgress progress : getCurrentQuestProgress(playerId)) {
            if (progress.templateId() != null && progress.templateId().equals(excludedTemplateId)) {
                continue;
            }

            ScenarioTemplate activeTemplate = resolveTemplateForProgress(progress, null);
            if (mechanicKey.equals(resolveProgressionMechanicKey(activeTemplate))) {
                count++;
            }
        }
        return count;
    }

    private String resolveProgressionMechanicKey(ScenarioTemplate template) {
        if (template == null || template.getProgressionMechanicId().isBlank()) {
            return "";
        }

        FeaturePackLoader.ProgressionMechanicDefinition mechanic = resolveProgressionMechanicDefinition(template);
        if (mechanic != null) {
            return normalizeReference(mechanic.getPackId()) + ":" + normalizeReference(mechanic.getId());
        }

        String packId = template.getSourcePackId();
        return normalizeReference(packId) + ":" + normalizeReference(template.getProgressionMechanicId());
    }

    private FeaturePackLoader.ProgressionMechanicDefinition resolveProgressionMechanicDefinition(ScenarioTemplate template) {
        if (template == null || template.getProgressionMechanicId().isBlank()
            || plugin.getFeaturePackLoader() == null) {
            return null;
        }

        return plugin.getFeaturePackLoader().findProgressionMechanicDefinition(
            template.getSourcePackId(),
            template.getProgressionMechanicId()
        );
    }

    private String resolveProgressionMechanicDisplay(ScenarioTemplate template) {
        if (template == null) {
            return "mecanica de progres";
        }

        if (!template.getProgressionLabel().isBlank()) {
            return template.getProgressionLabel();
        }

        FeaturePackLoader.ProgressionMechanicDefinition mechanic = resolveProgressionMechanicDefinition(template);
        if (mechanic != null && !mechanic.getLabel().isBlank()) {
            return mechanic.getLabel();
        }

        return template.getProgressionMechanicId().isBlank()
            ? "mecanica de progres"
            : template.getProgressionMechanicId();
    }

    private String resolveProgressionPluralLabel(ScenarioTemplate template) {
        if (template == null) {
            return "progresii";
        }

        if (!template.getProgressionPluralLabel().isBlank()) {
            return template.getProgressionPluralLabel();
        }

        FeaturePackLoader.ProgressionMechanicDefinition mechanic = resolveProgressionMechanicDefinition(template);
        if (mechanic != null && !mechanic.getPluralLabel().isBlank()) {
            return mechanic.getPluralLabel();
        }

        return "progresii";
    }

    private String resolveProgressionSingularLabel(ScenarioTemplate template) {
        if (template == null) {
            return "progresie";
        }

        if (!template.getProgressionSingularLabel().isBlank()) {
            return template.getProgressionSingularLabel();
        }

        FeaturePackLoader.ProgressionMechanicDefinition mechanic = resolveProgressionMechanicDefinition(template);
        if (mechanic != null && !mechanic.getSingularLabel().isBlank()) {
            return mechanic.getSingularLabel();
        }

        return "progresie";
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

    private long remainingQuestCooldownMillis(ScenarioTemplate template, PlayerQuestProgress completedProgress) {
        if (template == null || completedProgress == null || template.getQuestCooldownSeconds() <= 0) {
            return 0L;
        }

        long completedAt = completedProgress.completedAt() > 0
            ? completedProgress.completedAt()
            : completedProgress.updatedAt();
        long elapsedMillis = Math.max(0L, System.currentTimeMillis() - completedAt);
        return Math.max(0L, template.getQuestCooldownSeconds() * 1000L - elapsedMillis);
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
                yield template != null && progressionKindMatches(template, "quest");
            }
            case CONTRACT_KIND -> {
                ScenarioTemplate template = resolveTemplateForProgress(progress, null);
                yield template != null && progressionKindMatches(template, "contract");
            }
            case DUTY_KIND -> {
                ScenarioTemplate template = resolveTemplateForProgress(progress, null);
                yield template != null && progressionKindMatches(template, "duty");
            }
            case BOUNTY_KIND -> {
                ScenarioTemplate template = resolveTemplateForProgress(progress, null);
                yield template != null && progressionKindMatches(template, "bounty");
            }
            case EVENT_KIND -> {
                ScenarioTemplate template = resolveTemplateForProgress(progress, null);
                yield template != null && progressionKindMatches(template, "event");
            }
            case TUTORIAL_KIND -> {
                ScenarioTemplate template = resolveTemplateForProgress(progress, null);
                yield template != null && progressionKindMatches(template, "tutorial");
            }
            case RITUAL_KIND -> {
                ScenarioTemplate template = resolveTemplateForProgress(progress, null);
                yield template != null && progressionKindMatches(template, "ritual");
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
        return template != null && progressionKindMatches(template, expectedKind);
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
            mechanicCounts.merge(resolveProgressionMechanicDisplay(template), 1, Integer::sum);
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

    private boolean progressionKindMatches(ScenarioTemplate template, String expectedKind) {
        return QuestTemplateSelector.matchesProgressionKind(
            template,
            expectedKind,
            resolveProgressionMechanicDisplay(template)
        );
    }

    private Comparator<PlayerQuestProgress> questLogCurrentComparator(UUID playerId) {
        return Comparator
            .comparingInt((PlayerQuestProgress progress) -> isTrackedQuest(playerId, progress) ? 0 : 1)
            .thenComparingInt(progress -> questLogCategoryPriority(resolveTemplateForProgress(progress, null)))
            .thenComparing(progress -> resolveProgressionMechanicSortKey(resolveTemplateForProgress(progress, null)))
            .thenComparingInt(QuestLogFilterKt::questLogStatusPriority)
            .thenComparing(Comparator.comparingLong(PlayerQuestProgress::updatedAt).reversed())
            .thenComparing(progress -> progress.templateId() != null ? progress.templateId() : "");
    }

    private int questLogCategoryPriority(ScenarioTemplate template) {
        if (template == null) {
            return 3;
        }
        return switch (resolveQuestCategory(template)) {
            case MAIN -> 0;
            case SIDE -> 1;
            case REPEATABLE -> 2;
        };
    }

    private String resolveProgressionMechanicSortKey(ScenarioTemplate template) {
        String mechanicKey = resolveProgressionMechanicKey(template);
        if (!mechanicKey.isBlank()) {
            return mechanicKey;
        }
        return template != null ? normalizeReference(resolveProgressionMechanicDisplay(template)) : "";
    }

    private String questLogCurrentGroupLabel(UUID playerId, ScenarioTemplate template, PlayerQuestProgress progress) {
        if (isTrackedQuest(playerId, progress)) {
            if (template == null) {
                return "&b--- Progresie urmarita ---";
            }
            return "&b--- " + capitalizeProgressionLabel(resolveProgressionSingularLabel(template)) + " urmarit ---";
        }
        if (template == null) {
            return "&e--- Template lipsa ---";
        }
        return "&e--- " + resolveProgressionMechanicDisplay(template) + " ---";
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
                .append(resolveProgressionMechanicDisplay(template))
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

    private String questLogActionSelector(ScenarioTemplate template, PlayerQuestProgress progress) {
        if (template != null) {
            String code = progress != null && progress.questCode() != null && !progress.questCode().isBlank()
                ? progress.questCode()
                : template.getQuestCode();
            String selector = progressionReference(template.getProgressionMechanicId(), code);
            if (!selector.isBlank()) {
                return selector;
            }
        }
        if (progress != null && progress.questCode() != null && !progress.questCode().isBlank()) {
            return progress.questCode();
        }
        if (template != null && template.getQuestCode() != null && !template.getQuestCode().isBlank()) {
            return template.getQuestCode();
        }
        if (progress != null && progress.templateId() != null && !progress.templateId().isBlank()) {
            return progress.templateId();
        }
        return template != null && template.getTemplateId() != null ? template.getTemplateId() : "";
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

    private String buildQuestCompletionKey(UUID playerId, String templateId) {
        return (playerId != null ? playerId.toString() : "unknown")
            + "::"
            + (templateId != null ? templateId : "");
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

    private long readNullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? 0L : value;
    }

    private String readTextOrEmpty(ResultSet rs, String column) throws SQLException {
        String value = rs.getString(column);
        return value != null ? value : "";
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
            if (!matchesNpcObjective(objective, npc, template, progress)) {
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

    private boolean isQuestAnchorVariableKey(String key) {
        return key != null && (key.startsWith("anchor.") || "quest_anchor_count".equals(key));
    }

    private String resolveQuestNpcName(PlayerQuestProgress progress) {
        if (progress == null || progress.questVariables().isEmpty()) {
            return "";
        }

        String displayName = progress.questVariables().getOrDefault("quest_giver_display_name", "");
        if (!displayName.isBlank()) {
            return displayName;
        }

        return progress.questVariables().getOrDefault("quest_giver_name", "");
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

    private Map<String, String> seedQuestStageVariables(ScenarioTemplate template,
                                                        QuestStatus status,
                                                        String currentPhase,
                                                        Map<String, String> questVariables,
                                                        long timestamp) {
        Map<String, String> safeVariables = questVariables != null ? questVariables : Map.of();
        if (status != QuestStatus.ACTIVE || !hasStagedObjectives(template)) {
            return safeVariables;
        }

        String currentStage = findMatchingObjectiveStage(template, currentPhase);
        if (currentStage.isBlank()) {
            return safeVariables;
        }

        Map<String, String> updatedVariables = new LinkedHashMap<>(safeVariables);
        updatedVariables.put("stage.current", currentStage);
        String normalizedStage = normalizeReference(currentStage);
        if (!normalizedStage.isBlank()) {
            updatedVariables.putIfAbsent("stage.started_at." + normalizedStage, String.valueOf(timestamp));
        }
        return updatedVariables;
    }

    private Map<String, String> buildQuestStageTransitionVariables(ScenarioTemplate template,
                                                                   PlayerQuestProgress progress,
                                                                   String updatedPhase,
                                                                   Map<String, Integer> objectiveProgress) {
        if (progress == null) {
            return Map.of();
        }

        Map<String, String> updatedVariables = seedQuestStageVariables(
            template,
            progress.status(),
            updatedPhase,
            progress.questVariables(),
            System.currentTimeMillis()
        );
        if (!hasStagedObjectives(template) || progress.status() != QuestStatus.ACTIVE) {
            return updatedVariables;
        }

        String previousStage = findMatchingObjectiveStage(template, progress.currentPhase());
        String currentStage = findMatchingObjectiveStage(template, updatedPhase);
        if (previousStage.isBlank() || currentStage.isBlank() || phasesMatch(previousStage, currentStage)) {
            return updatedVariables;
        }

        Map<String, String> transitionVariables = new LinkedHashMap<>(updatedVariables);
        long now = System.currentTimeMillis();
        transitionVariables.put("stage.previous", previousStage);
        transitionVariables.put("stage.changed_at", String.valueOf(now));

        if (areObjectivesSatisfiedForStage(template, previousStage, objectiveProgress)) {
            String normalizedPreviousStage = normalizeReference(previousStage);
            if (!normalizedPreviousStage.isBlank()) {
                transitionVariables.put("stage.completed." + normalizedPreviousStage, "true");
                transitionVariables.putIfAbsent("stage.completed_at." + normalizedPreviousStage, String.valueOf(now));
            }
            transitionVariables.put("stage.last_completed", previousStage);
        }

        return transitionVariables;
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

    private boolean matchesNpcObjective(FeaturePackLoader.QuestEntryDefinition objective,
                                        AINPC npc,
                                        ScenarioTemplate template,
                                        PlayerQuestProgress progress) {
        if (objective == null || npc == null) {
            return false;
        }

        String reference = objective.getItemId();
        if (reference == null || reference.isBlank()) {
            return matchesStoredQuestNpc(progress, npc) || matchesQuestGiver(npc, template);
        }

        if (matchesObjectiveReference(reference, npc.getName(), npc.getDisplayName(), npc.getOccupation())) {
            return true;
        }
        if (npc.getUuid() != null && matchesObjectiveReference(reference, npc.getUuid().toString())) {
            return true;
        }
        if (npc.getDatabaseId() > 0 && matchesObjectiveReference(reference, String.valueOf(npc.getDatabaseId()))) {
            return true;
        }

        return false;
    }

    private boolean matchesStoredQuestNpc(PlayerQuestProgress progress, AINPC npc) {
        if (progress == null || npc == null) {
            return false;
        }

        Map<String, String> questVariables = progress.questVariables();
        if (questVariables.isEmpty()) {
            return false;
        }

        String storedUuid = questVariables.get("quest_giver_uuid");
        if (storedUuid != null && npc.getUuid() != null && storedUuid.equalsIgnoreCase(npc.getUuid().toString())) {
            return true;
        }

        String storedDatabaseId = questVariables.get("quest_giver_db_id");
        if (storedDatabaseId != null && npc.getDatabaseId() > 0 && storedDatabaseId.equals(String.valueOf(npc.getDatabaseId()))) {
            return true;
        }

        return matchesObjectiveReference(
            questVariables.get("quest_giver_name"),
            npc.getName(),
            npc.getDisplayName()
        ) || matchesObjectiveReference(
            questVariables.get("quest_giver_display_name"),
            npc.getName(),
            npc.getDisplayName()
        );
    }

    private boolean matchesRegionObjective(FeaturePackLoader.QuestEntryDefinition objective, WorldRegion region) {
        if (objective == null || region == null) {
            return false;
        }

        String reference = objective.getItemId();
        if (reference == null || reference.isBlank()) {
            return true;
        }

        List<String> candidates = new ArrayList<>();
        candidates.add(region.getId());
        candidates.add(region.getName());
        if (region.getType() != null) {
            candidates.add(region.getType().getId());
            candidates.add(region.getType().name());
        }
        candidates.addAll(region.getTags());
        return matchesObjectiveReference(reference, candidates.toArray(String[]::new));
    }

    private boolean matchesRegionObjective(PlayerQuestProgress progress,
                                           FeaturePackLoader.QuestEntryDefinition objective,
                                           int index,
                                           WorldRegion region) {
        for (String objectiveKey : objectiveKeyCandidates(objective, index)) {
            if (hasBoundAnchor(progress, objectiveKey)) {
                return matchesBoundAnchor(progress, objectiveKey, "region", region != null ? region.getId() : "");
            }
        }
        return matchesRegionObjective(objective, region);
    }

    private boolean matchesPlaceObjective(PlayerQuestProgress progress,
                                          FeaturePackLoader.QuestEntryDefinition objective,
                                          int index,
                                          WorldPlace place) {
        for (String objectiveKey : objectiveKeyCandidates(objective, index)) {
            if (hasBoundAnchor(progress, objectiveKey)) {
                return matchesBoundAnchor(progress, objectiveKey, "place", place != null ? place.getId() : "");
            }
        }
        return matchesPlaceObjective(objective, place);
    }

    private boolean matchesNodeObjective(PlayerQuestProgress progress,
                                         FeaturePackLoader.QuestEntryDefinition objective,
                                         int index,
                                         WorldNode node) {
        for (String objectiveKey : objectiveKeyCandidates(objective, index)) {
            if (hasBoundAnchor(progress, objectiveKey)) {
                return matchesBoundAnchor(progress, objectiveKey, "node", node != null ? node.getId() : "");
            }
        }
        return matchesNodeObjective(objective, node);
    }

    private boolean matchesPlaceObjective(FeaturePackLoader.QuestEntryDefinition objective, WorldPlace place) {
        if (objective == null || place == null) {
            return false;
        }

        String reference = objective.getItemId();
        if (reference == null || reference.isBlank()) {
            return true;
        }

        List<String> candidates = new ArrayList<>();
        candidates.add(place.getId());
        candidates.add(place.getDisplayName());
        candidates.add(place.getRegionId());
        if (place.getPlaceType() != null) {
            candidates.add(place.getPlaceType().getId());
            candidates.add(place.getPlaceType().name());
        }
        candidates.addAll(place.getTags());
        candidates.addAll(place.getMetadata().keySet());
        candidates.addAll(place.getMetadata().values());
        return matchesObjectiveReference(reference, candidates.toArray(String[]::new));
    }

    private boolean matchesNodeObjective(FeaturePackLoader.QuestEntryDefinition objective, WorldNode node) {
        if (objective == null || node == null) {
            return false;
        }

        String reference = objective.getItemId();
        if (reference == null || reference.isBlank()) {
            return true;
        }

        List<String> candidates = new ArrayList<>();
        candidates.add(node.getId());
        candidates.add(node.getRegionId());
        candidates.add(node.getPlaceId());
        if (node.getType() != null) {
            candidates.add(node.getType().getId());
            candidates.add(node.getType().name());
        }
        candidates.addAll(node.getMetadata().keySet());
        candidates.addAll(node.getMetadata().values());
        return matchesObjectiveReference(reference, candidates.toArray(String[]::new));
    }

    private boolean hasBoundAnchor(PlayerQuestProgress progress, String objectiveKey) {
        if (progress == null || objectiveKey == null || objectiveKey.isBlank()) {
            return false;
        }
        return !progress.questVariables().getOrDefault("anchor." + objectiveKey + ".id", "").isBlank();
    }

    private boolean matchesBoundAnchor(PlayerQuestProgress progress,
                                       String objectiveKey,
                                       String expectedAnchorType,
                                       String candidateId) {
        if (progress == null || objectiveKey == null || candidateId == null || candidateId.isBlank()) {
            return false;
        }

        String prefix = "anchor." + objectiveKey;
        String anchorType = progress.questVariables().getOrDefault(prefix + ".type", "");
        String anchorId = progress.questVariables().getOrDefault(prefix + ".id", "");
        return matchesObjectiveReference(anchorType, expectedAnchorType)
            && matchesObjectiveReference(anchorId, candidateId);
    }

    private boolean matchesMobObjective(FeaturePackLoader.QuestEntryDefinition objective, Entity entity) {
        if (objective == null || entity == null) {
            return false;
        }

        String reference = objective.getItemId();
        if (reference == null || reference.isBlank()) {
            return true;
        }

        return matchesObjectiveReference(
            reference,
            entity.getType().name(),
            humanizeItemId(entity.getType().name())
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
            .filter(template -> matchesQuestGiver(npc, template))
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
                && matchesQuestGiver(npc, template)) {
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

        if (matchesQuestGiver(npc, template)) {
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
                && matchesNpcObjective(objective, npc, template, progress)) {
                return true;
            }
        }

        return false;
    }

    private boolean matchesProgressionKindFilter(ScenarioTemplate template, String progressionKind) {
        String expected = normalizeReference(progressionKind);
        return expected.isBlank() || progressionKindMatches(template, expected);
    }

    private boolean shouldUseSimpleQuestForAllNpcs() {
        if (!plugin.getConfig().getBoolean("demo.enabled", true)) {
            return false;
        }
        return getQuestSettings().getBoolean("simple_for_all_npcs", true);
    }

    private ScenarioTemplate buildSimpleQuestTemplate(AINPC npc) {
        if (npc == null) {
            return null;
        }

        FeaturePackLoader.ProfessionDefinition profession = resolveQuestProfession(npc);
        SimpleQuestProfile questProfile = resolveSimpleQuestProfile(npc, profession);
        String npcIdentifier = npc.getDatabaseId() > 0
            ? String.valueOf(npc.getDatabaseId())
            : npc.getUuid().toString();

        ScenarioTemplate template = new ScenarioTemplate(ScenarioType.QUEST);
        template.setTemplateId("simple_npc_quest:" + npcIdentifier);
        template.setDisplayName(questProfile.title());
        template.setDescription(questProfile.objectivePrompt() + " si iti dau "
            + formatQuestAmount(questProfile.rewardAmount(), questProfile.rewardMaterial()) + ".");
        template.setHint(questProfile.hint());
        template.setQuestGiverProfession(profession != null ? profession.getId() : npc.getOccupation());
        template.setRequiresPlayer(true);
        template.setMinimumNpcCount(1);
        template.setObjectives(List.of(new FeaturePackLoader.QuestEntryDefinition(
            "collect_item",
            questProfile.objectiveMaterial().name(),
            questProfile.objectiveAmount(),
            questProfile.objectivePrompt() + "."
        )));
        template.setRewards(List.of(new FeaturePackLoader.QuestEntryDefinition(
            "item",
            questProfile.rewardMaterial().name(),
            questProfile.rewardAmount(),
            "Primesti " + formatQuestAmount(questProfile.rewardAmount(), questProfile.rewardMaterial()) + "."
        )));
        template.setQuestContract(QuestScenarioContract.fromQuestEntries(
            "fetch",
            "explicit",
            "return_to_giver",
            "next_objective",
            List.of("fallback", "simple"),
            template.getObjectives()
        ));
        return template;
    }

    private FeaturePackLoader.ProfessionDefinition resolveQuestProfession(AINPC npc) {
        if (npc == null || plugin.getFeaturePackLoader() == null) {
            return null;
        }

        return plugin.getFeaturePackLoader().findPrimaryScenarioProfession(npc.getOccupation());
    }

    private SimpleQuestProfile resolveSimpleQuestProfile(AINPC npc,
                                                         FeaturePackLoader.ProfessionDefinition profession) {
        String professionId = profession != null ? normalizeScenarioToken(profession.getId()) : "";
        String professionName = profession != null && profession.getName() != null && !profession.getName().isBlank()
            ? profession.getName()
            : npc.getOccupation();
        if (professionName == null || professionName.isBlank()) {
            professionName = "localnicul";
        }

        int objectiveAmount = Math.max(1, getQuestSettings().getInt("simple.objective.amount", 3));
        Material objectiveMaterial = resolveConfiguredQuestMaterial("simple.objective.item", Material.OAK_PLANKS);
        int rewardAmount = Math.max(1, getQuestSettings().getInt("simple.reward.amount", 1));
        Material rewardMaterial = resolveConfiguredQuestMaterial("simple.reward.item", Material.EMERALD);
        SimpleQuestProfile defaultProfile = new SimpleQuestProfile(
            resolveConfiguredSimpleQuestTitle(professionName),
            objectiveMaterial,
            objectiveAmount,
            rewardMaterial,
            rewardAmount,
            "Adu-mi " + formatQuestAmount(objectiveAmount, objectiveMaterial),
            npc.getName() + " are nevoie de ajutor cu treburi obisnuite de " + professionName + "."
        );
        return applyConfiguredSimpleQuestProfile(npc, professionId, professionName, defaultProfile);
    }

    private SimpleQuestProfile applyConfiguredSimpleQuestProfile(AINPC npc,
                                                                 String professionId,
                                                                 String professionName,
                                                                 SimpleQuestProfile fallbackProfile) {
        ConfigurationSection section = resolveProfessionFallbackSection(professionId, professionName);
        if (section == null) {
            return fallbackProfile;
        }

        Material objectiveMaterial = resolveConfiguredQuestMaterialValue(
            section.getString("objective.item"),
            fallbackProfile.objectiveMaterial()
        );
        int objectiveAmount = Math.max(1, section.getInt("objective.amount", fallbackProfile.objectiveAmount()));

        Material rewardMaterial = resolveConfiguredQuestMaterialValue(
            section.getString("reward.item"),
            fallbackProfile.rewardMaterial()
        );
        int rewardAmount = Math.max(1, section.getInt("reward.amount", fallbackProfile.rewardAmount()));

        String objectiveText = formatQuestAmount(objectiveAmount, objectiveMaterial);
        String rewardText = formatQuestAmount(rewardAmount, rewardMaterial);
        String title = applyQuestFallbackPlaceholders(
            section.getString("title", fallbackProfile.title()),
            npc,
            professionName,
            objectiveText,
            rewardText
        );
        String objectivePrompt = applyQuestFallbackPlaceholders(
            section.getString("objective.prompt", "Adu-mi " + objectiveText),
            npc,
            professionName,
            objectiveText,
            rewardText
        );
        String hint = applyQuestFallbackPlaceholders(
            section.getString("hint", fallbackProfile.hint()),
            npc,
            professionName,
            objectiveText,
            rewardText
        );

        return new SimpleQuestProfile(
            title,
            objectiveMaterial,
            objectiveAmount,
            rewardMaterial,
            rewardAmount,
            objectivePrompt,
            hint
        );
    }

    private ConfigurationSection resolveProfessionFallbackSection(String professionId, String professionName) {
        ConfigurationSection root = getQuestSettings().getConfigurationSection("profession_fallbacks");
        if (root == null) {
            return null;
        }

        if (professionId != null && !professionId.isBlank()) {
            ConfigurationSection byId = root.getConfigurationSection(sanitizeConfigKey(professionId));
            if (byId != null) {
                return byId;
            }
        }

        if (professionName != null && !professionName.isBlank()) {
            ConfigurationSection byName = root.getConfigurationSection(sanitizeConfigKey(professionName));
            if (byName != null) {
                return byName;
            }
        }

        return root.getConfigurationSection("default");
    }

    private String resolveConfiguredSimpleQuestTitle(String professionName) {
        String configuredTitle = getQuestSettings().getString("simple.title", "Ajutor rapid");
        if (professionName == null || professionName.isBlank()) {
            return configuredTitle;
        }

        return configuredTitle + " - " + professionName;
    }

    private Material resolveConfiguredQuestMaterial(String path, Material fallback) {
        String configuredValue = getQuestSettings().getString(path, fallback.name());
        return resolveConfiguredQuestMaterialValue(configuredValue, fallback);
    }

    private Material resolveConfiguredQuestMaterialValue(String configuredValue, Material fallback) {
        if (configuredValue == null || configuredValue.isBlank()) {
            return fallback;
        }

        Material material = Material.matchMaterial(configuredValue.trim().toUpperCase(Locale.ROOT));
        return material != null ? material : fallback;
    }

    private boolean matchesQuestGiver(AINPC npc, ScenarioTemplate template) {
        if (npc == null || template == null) {
            return false;
        }

        if (!template.getQuestGiverProfession().isBlank()
            && !matchesProfessionReference(npc, List.of(template.getQuestGiverProfession()))) {
            return false;
        }

        ScenarioRoleRule questGiverRole = template.getRoles().get("QUEST_GIVER");
        if (questGiverRole == null) {
            return true;
        }

        if (!questGiverRole.getRequiredProfessions().isEmpty()
            && !matchesProfessionReference(npc, questGiverRole.getRequiredProfessions())) {
            return false;
        }

        return questGiverRole.getPreferredProfessions().isEmpty()
            || matchesProfessionReference(npc, questGiverRole.getPreferredProfessions());
    }

    private boolean matchesProfessionReference(AINPC npc, List<String> references) {
        if (npc == null || references == null || references.isEmpty()) {
            return false;
        }

        String occupation = npc.getOccupation();
        if (occupation == null || occupation.isBlank()) {
            return false;
        }

        FeaturePackLoader loader = plugin.getFeaturePackLoader();
        for (String reference : references) {
            if (reference == null || reference.isBlank()) {
                continue;
            }

            if (loader != null && loader.matchesProfession(occupation, reference)) {
                return true;
            }

            if (normalizeScenarioToken(occupation).equals(normalizeScenarioToken(reference))) {
                return true;
            }
        }

        return false;
    }

    private List<String> buildQuestBriefingMessages(ScenarioTemplate template) {
        List<String> lines = new ArrayList<>();
        String progressionLabel = capitalizeProgressionLabel(resolveProgressionSingularLabel(template));
        lines.add("&6[" + progressionLabel + "] &f" + resolveQuestTitle(template));

        String questGiver = resolveProfessionName(template.getQuestGiverProfession());
        if (!questGiver.isBlank()) {
            lines.add("&7Dat de: &f" + questGiver);
        }
        if (!resolveProgressionMechanicDisplay(template).isBlank()) {
            lines.add("&7Mecanica: &f" + resolveProgressionMechanicDisplay(template));
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
            lines.add("&7Nu ai acceptat inca acest " + resolveProgressionSingularLabel(template) + ".");
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
                lines.add("&7" + capitalizeProgressionLabel(resolveProgressionSingularLabel(template)) + " activ.");
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
            lines.add("&a" + capitalizeProgressionLabel(resolveProgressionSingularLabel(template)) + " finalizat.");
        } else if (progress.status() == QuestStatus.FAILED) {
            lines.add("&c" + capitalizeProgressionLabel(resolveProgressionSingularLabel(template)) + " abandonat sau esuat.");
            lines.add("&7Poti cere din nou progresia daca vrei sa reincepi.");
        }

        return lines;
    }

    private List<String> buildObjectiveProgressLines(ScenarioTemplate template, PlayerQuestProgress progress, Player player) {
        if (template == null || progress == null || template.getObjectives().isEmpty()) {
            return List.of();
        }

        List<String> lines = new ArrayList<>();
        List<FeaturePackLoader.QuestEntryDefinition> objectives = template.getObjectives();
        for (int index = 0; index < objectives.size(); index++) {
            FeaturePackLoader.QuestEntryDefinition objective = objectives.get(index);
            boolean activeForStage = shouldShowObjectiveForCurrentStage(template, progress, objective);
            if (!activeForStage) {
                continue;
            }
            int currentAmount = resolveObjectiveCurrentProgress(player, objective, progress, index);
            int requiredAmount = Math.max(1, objective.getAmount());
            String label = formatObjectiveProgressLabel(objective);
            QuestObjectiveState state = resolveQuestObjectiveState(progress, currentAmount, requiredAmount, activeForStage);
            lines.add("&7- &f" + label + ": &e" + Math.min(currentAmount, requiredAmount)
                + "&7/&f" + requiredAmount + " &8(" + state.displayName() + ")");
        }

        return lines;
    }

    private List<String> buildQuestProgressDetailLines(Player player,
                                                       ScenarioTemplate template,
                                                       PlayerQuestProgress progress) {
        if (template == null || progress == null || template.getObjectives().isEmpty()) {
            return List.of();
        }

        List<String> lines = new ArrayList<>();
        List<FeaturePackLoader.QuestEntryDefinition> objectives = template.getObjectives();
        for (int index = 0; index < objectives.size(); index++) {
            FeaturePackLoader.QuestEntryDefinition objective = objectives.get(index);
            String objectiveKey = buildObjectiveKey(objective, index);
            int requiredAmount = Math.max(1, objective.getAmount());
            int currentAmount = progress.isCurrent()
                ? resolveObjectiveCurrentProgress(player, objective, progress, index)
                : readObjectiveProgress(progress.objectiveProgress(), objective, index);
            boolean activeForStage = shouldShowObjectiveForCurrentStage(template, progress, objective);
            QuestObjectiveState state = resolveQuestObjectiveState(progress, currentAmount, requiredAmount, activeForStage);
            String stageId = findObjectiveStageId(template, objective);

            StringBuilder line = new StringBuilder("&7- &f")
                .append(formatObjectiveProgressLabel(objective))
                .append(" &8[")
                .append(objectiveKey)
                .append("]")
                .append(" &7stare=&f")
                .append(state.displayName())
                .append(" &7progres=&e")
                .append(Math.min(currentAmount, requiredAmount))
                .append("&7/&f")
                .append(requiredAmount);
            if (!stageId.isBlank()) {
                line.append(" &7stage=&f").append(formatQuestPhase(stageId));
            }
            if (!activeForStage) {
                line.append(" &8(inactiv)");
            }
            lines.add(line.toString());
        }

        return lines;
    }

    private List<String> buildQuestTrackingLines(ScenarioTemplate template,
                                                 PlayerQuestProgress progress,
                                                 Player player) {
        if (template == null || progress == null || template.getObjectives().isEmpty()) {
            return List.of();
        }

        List<String> lines = new ArrayList<>();
        List<FeaturePackLoader.QuestEntryDefinition> objectives = template.getObjectives();
        for (int index = 0; index < objectives.size(); index++) {
            FeaturePackLoader.QuestEntryDefinition objective = objectives.get(index);
            if (!shouldShowObjectiveForCurrentStage(template, progress, objective)) {
                continue;
            }
            String objectiveKey = buildObjectiveKey(objective, index);
            int requiredAmount = Math.max(1, objective != null ? objective.getAmount() : 1);
            int currentAmount = resolveObjectiveCurrentProgress(player, objective, progress, index);
            if (currentAmount >= requiredAmount) {
                continue;
            }

            String objectiveLabel = formatMissingObjective(objective, currentAmount, requiredAmount);
            String targetHint = describeObjectiveTrackingTarget(progress, objectiveKey, objective, index, player);
            if (targetHint.isBlank()) {
                targetHint = describeGenericQuestTrackingHint(objective);
            }

            if (targetHint.isBlank()) {
                lines.add("&7- &f" + objectiveLabel);
            } else {
                lines.add("&7- &f" + objectiveLabel + " &8-> " + targetHint);
            }
        }

        return lines;
    }

    private QuestTrackingStep resolveNextQuestTrackingStep(ScenarioTemplate template,
                                                           PlayerQuestProgress progress,
                                                           Player player) {
        if (template == null || progress == null || template.getObjectives().isEmpty()) {
            return null;
        }

        List<FeaturePackLoader.QuestEntryDefinition> objectives = template.getObjectives();
        for (int index = 0; index < objectives.size(); index++) {
            FeaturePackLoader.QuestEntryDefinition objective = objectives.get(index);
            if (!shouldShowObjectiveForCurrentStage(template, progress, objective)) {
                continue;
            }
            int requiredAmount = Math.max(1, objective != null ? objective.getAmount() : 1);
            int currentAmount = resolveObjectiveCurrentProgress(player, objective, progress, index);
            if (currentAmount >= requiredAmount) {
                continue;
            }

            QuestTrackingTarget target = resolveQuestAnchorTrackingTarget(progress, objective, index);
            String objectiveType = normalizeObjectiveType(objective != null ? objective.getType() : "");
            if (target == null && "deliver_to_npc".equals(objectiveType)) {
                target = resolveQuestGiverTrackingTarget(progress);
            }
            if (target != null && target.hasLocation()) {
                return new QuestTrackingStep(formatMissingObjective(objective, currentAmount, requiredAmount), target);
            }
        }

        return null;
    }

    private String describeObjectiveTrackingTarget(PlayerQuestProgress progress,
                                                   String objectiveKey,
                                                   FeaturePackLoader.QuestEntryDefinition objective,
                                                   int index,
                                                   Player player) {
        QuestTrackingTarget target = resolveQuestAnchorTrackingTarget(progress, objective, index);
        if (target != null) {
            return formatQuestTrackingTarget(target, player);
        }

        String objectiveType = normalizeObjectiveType(objective != null ? objective.getType() : "");
        if ("deliver_to_npc".equals(objectiveType)) {
            return describeQuestGiverTrackingTarget(progress, player);
        }

        return "";
    }

    private String describeGenericQuestTrackingHint(FeaturePackLoader.QuestEntryDefinition objective) {
        String objectiveType = normalizeObjectiveType(objective != null ? objective.getType() : "");
        return switch (objectiveType) {
            case "collect_item" -> "&7aduna obiectele cerute";
            case "deliver_to_npc" -> "&7intoarce-te la NPC-ul questului";
            case "talk_to_npc" -> "&7cauta NPC-ul tintit";
            case "visit_region", "visit_place", "inspect_node" -> "&cancora lipsa in mapping";
            case "kill_mob" -> "&7cauta inamicul tintit";
            default -> "&7continua obiectivul";
        };
    }

    private QuestTrackingTarget resolveQuestAnchorTrackingTarget(PlayerQuestProgress progress,
                                                                 FeaturePackLoader.QuestEntryDefinition objective,
                                                                 int index) {
        for (String objectiveKey : objectiveKeyCandidates(objective, index)) {
            QuestTrackingTarget target = resolveQuestAnchorTrackingTarget(progress, objectiveKey);
            if (target != null) {
                return target;
            }
        }
        return null;
    }

    private QuestTrackingTarget resolveQuestAnchorTrackingTarget(PlayerQuestProgress progress, String objectiveKey) {
        if (!hasBoundAnchor(progress, objectiveKey)) {
            return null;
        }

        String prefix = "anchor." + objectiveKey;
        String anchorType = progress.questVariables().getOrDefault(prefix + ".type", "");
        String anchorId = progress.questVariables().getOrDefault(prefix + ".id", "");
        String label = progress.questVariables().getOrDefault(prefix + ".label", "");
        QuestTrackingTarget locatedTarget = resolveQuestAnchorLocation(anchorType, anchorId, label);
        if (locatedTarget != null) {
            return locatedTarget;
        }

        return new QuestTrackingTarget(anchorType, anchorId, label, "", 0.0, 0.0, 0.0, false);
    }

    private QuestTrackingTarget resolveQuestAnchorLocation(String anchorType, String anchorId, String label) {
        String normalizedType = normalizeTrackingAnchorType(anchorType);
        if (normalizedType.isBlank() || anchorId == null || anchorId.isBlank()) {
            return null;
        }

        if ("npc".equals(normalizedType)) {
            return resolveNpcTrackingTarget(anchorId, label);
        }

        WorldAdminApi worldAdminApi = plugin.getPlatform() != null ? plugin.getPlatform().getWorldAdmin() : null;
        if (worldAdminApi == null) {
            return null;
        }

        return switch (normalizedType) {
            case "region" -> {
                WorldRegionInfo region = worldAdminApi.getRegion(anchorId);
                yield region != null
                    ? targetFromRegion(region, label)
                    : null;
            }
            case "place" -> {
                WorldPlaceInfo place = worldAdminApi.getPlace(anchorId);
                yield place != null
                    ? targetFromPlace(place, label)
                    : null;
            }
            case "node" -> {
                WorldNodeInfo node = worldAdminApi.getNode(anchorId);
                yield node != null
                    ? new QuestTrackingTarget(
                        "node",
                        node.id(),
                        label == null || label.isBlank() ? node.typeId() : label,
                        node.worldName(),
                        node.x(),
                        node.y(),
                        node.z(),
                        true
                    )
                    : null;
            }
            default -> null;
        };
    }

    private QuestTrackingTarget targetFromRegion(WorldRegionInfo region, String label) {
        return new QuestTrackingTarget(
            "region",
            region.id(),
            label == null || label.isBlank() ? region.name() : label,
            region.worldName(),
            center(region.minX(), region.maxX()),
            center(region.minY(), region.maxY()),
            center(region.minZ(), region.maxZ()),
            true
        );
    }

    private QuestTrackingTarget targetFromPlace(WorldPlaceInfo place, String label) {
        return new QuestTrackingTarget(
            "place",
            place.id(),
            label == null || label.isBlank() ? place.displayName() : label,
            place.worldName(),
            center(place.minX(), place.maxX()),
            center(place.minY(), place.maxY()),
            center(place.minZ(), place.maxZ()),
            true
        );
    }

    private QuestTrackingTarget resolveNpcTrackingTarget(String anchorId, String label) {
        AINPC npc = resolveNpcByAnchorId(anchorId);
        if (npc == null && label != null && !label.isBlank()) {
            npc = plugin.getNpcManager() != null ? plugin.getNpcManager().getNPCByName(label) : null;
        }
        if (npc == null) {
            return null;
        }

        Location location = npc.getLocation();
        if (location == null || location.getWorld() == null) {
            return null;
        }

        return new QuestTrackingTarget(
            "npc",
            anchorId,
            label == null || label.isBlank() ? npc.getName() : label,
            location.getWorld().getName(),
            location.getX(),
            location.getY(),
            location.getZ(),
            true
        );
    }

    private String describeQuestGiverTrackingTarget(PlayerQuestProgress progress, Player player) {
        QuestTrackingTarget target = resolveQuestGiverTrackingTarget(progress);
        if (target != null) {
            return formatQuestTrackingTarget(target, player);
        }

        String label = resolveQuestNpcName(progress);
        return label.isBlank() ? "" : "&b" + label + " &7(NPC quest)";
    }

    private QuestTrackingTarget resolveQuestGiverTrackingTarget(PlayerQuestProgress progress) {
        AINPC questGiver = resolveQuestGiverNpc(progress);
        if (questGiver == null) {
            return null;
        }

        Location location = questGiver.getLocation();
        if (location == null || location.getWorld() == null) {
            return null;
        }

        String label = resolveQuestNpcName(progress);
        String anchorId = questGiver.getUuid() != null
            ? questGiver.getUuid().toString()
            : String.valueOf(questGiver.getDatabaseId());
        return new QuestTrackingTarget(
            "npc",
            anchorId,
            label.isBlank() ? questGiver.getName() : label,
            location.getWorld().getName(),
            location.getX(),
            location.getY(),
            location.getZ(),
            true
        );
    }

    private AINPC resolveQuestGiverNpc(PlayerQuestProgress progress) {
        if (progress == null || progress.questVariables().isEmpty() || plugin.getNpcManager() == null) {
            return null;
        }

        String uuid = progress.questVariables().getOrDefault("quest_giver_uuid", "");
        AINPC npc = resolveNpcByAnchorId(uuid);
        if (npc != null) {
            return npc;
        }

        String databaseId = progress.questVariables().getOrDefault("quest_giver_db_id", "");
        npc = resolveNpcByAnchorId(databaseId);
        if (npc != null) {
            return npc;
        }

        String name = progress.questVariables().getOrDefault("quest_giver_name", "");
        if (!name.isBlank()) {
            npc = plugin.getNpcManager().getNPCByName(name);
            if (npc != null) {
                return npc;
            }
        }

        String displayName = progress.questVariables().getOrDefault("quest_giver_display_name", "");
        return displayName.isBlank() ? null : plugin.getNpcManager().getNPCByName(displayName);
    }

    private AINPC resolveNpcByAnchorId(String anchorId) {
        if (anchorId == null || anchorId.isBlank() || plugin.getNpcManager() == null) {
            return null;
        }

        try {
            AINPC npc = plugin.getNpcManager().getNPCByUuid(UUID.fromString(anchorId));
            if (npc != null) {
                return npc;
            }
        } catch (IllegalArgumentException ignored) {
            // Nu este UUID; incercam ID numeric sau nume.
        }

        try {
            int databaseId = Integer.parseInt(anchorId);
            AINPC npc = plugin.getNpcManager().getNPCById(databaseId);
            if (npc != null) {
                return npc;
            }
        } catch (NumberFormatException ignored) {
            // Nu este ID numeric.
        }

        return plugin.getNpcManager().getNPCByName(anchorId);
    }

    private String formatQuestTrackingTarget(QuestTrackingTarget target, Player player) {
        if (target == null) {
            return "";
        }

        String label = target.label() != null && !target.label().isBlank()
            ? target.label()
            : (target.anchorId() != null && !target.anchorId().isBlank()
                ? target.anchorId()
                : formatQuestAnchorType(target.anchorType()));
        String type = formatQuestAnchorType(target.anchorType());
        if (!target.hasLocation()) {
            return "&b" + label + " &7(" + type + ", locatie necunoscuta)";
        }

        return "&b" + label + " &7(" + type + ") " + formatQuestTrackingPosition(target, player);
    }

    private QuestTrackingMarker buildQuestTrackingMarker(String objectiveLabel,
                                                         QuestTrackingTarget target,
                                                         Player player) {
        Location location = toQuestTrackingLocation(target);
        if (location == null) {
            return null;
        }

        String label = target.label() != null && !target.label().isBlank()
            ? target.label()
            : formatQuestAnchorType(target.anchorType());
        return new QuestTrackingMarker(
            objectiveLabel,
            label,
            formatQuestAnchorType(target.anchorType()),
            location,
            formatQuestTrackingActionBar(label, target, player)
        );
    }

    private Location toQuestTrackingLocation(QuestTrackingTarget target) {
        if (target == null || !target.hasLocation() || target.worldName().isBlank()) {
            return null;
        }

        org.bukkit.World world = Bukkit.getWorld(target.worldName());
        if (world == null) {
            return null;
        }

        return new Location(world, target.x(), target.y(), target.z());
    }

    private String formatQuestTrackingActionBar(String label, QuestTrackingTarget target, Player player) {
        Location playerLocation = player != null ? player.getLocation() : null;
        String targetLabel = label == null || label.isBlank() ? "tinta questului" : label;
        if (playerLocation == null || playerLocation.getWorld() == null || target.worldName().isBlank()) {
            return "&6Quest &8| &f" + targetLabel;
        }

        String playerWorldName = playerLocation.getWorld().getName();
        if (!target.worldName().equalsIgnoreCase(playerWorldName)) {
            return "&6Quest &8| &f" + targetLabel + " &7in lumea &e" + target.worldName();
        }

        double dx = target.x() - playerLocation.getX();
        double dy = target.y() - playerLocation.getY();
        double dz = target.z() - playerLocation.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance < 2.0) {
            return "&aQuest &8| &f" + targetLabel + " &aeste aici";
        }

        return "&6Quest &8| &f" + targetLabel + " &7- &e" + Math.round(distance)
            + " blocuri &7spre &b" + formatHorizontalDirection(dx, dz)
            + formatVerticalHint(dy);
    }

    private String formatQuestTrackingPosition(QuestTrackingTarget target, Player player) {
        String coordinates = formatQuestTrackingCoordinates(target);
        Location playerLocation = player != null ? player.getLocation() : null;
        if (playerLocation == null || playerLocation.getWorld() == null || target.worldName().isBlank()) {
            return "&8(" + coordinates + ")";
        }

        String playerWorldName = playerLocation.getWorld().getName();
        if (!target.worldName().equalsIgnoreCase(playerWorldName)) {
            return "&7in lumea &f" + target.worldName() + " &8(" + coordinates + ")";
        }

        double dx = target.x() - playerLocation.getX();
        double dy = target.y() - playerLocation.getY();
        double dz = target.z() - playerLocation.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance < 2.0) {
            return "&ala pozitia ta &8(" + coordinates + ")";
        }

        String direction = formatHorizontalDirection(dx, dz);
        String verticalHint = formatVerticalHint(dy);
        return "&7la &e" + Math.round(distance) + " blocuri &7spre &f" + direction
            + verticalHint + " &8(" + coordinates + ")";
    }

    private String buildQuestOfferMessage(ScenarioTemplate template) {
        List<String> objectives = template.getObjectives().stream()
            .map(ScenarioEngineTextKt::formatQuestEntry)
            .toList();
        if (objectives.isEmpty()) {
            return template.getDescription().isBlank()
                ? "Am nevoie de ajutorul tau."
                : template.getDescription();
        }

        boolean deliveryQuest = template.getObjectives().stream().allMatch(ScenarioObjectiveProgressKt::usesInventoryProgress);
        if (deliveryQuest) {
            return "Adu-mi " + joinNaturally(objectives) + " si te rasplatesc cum se cuvine.";
        }

        if (!template.getDescription().isBlank()) {
            return template.getDescription();
        }

        return "Ai de facut urmatoarele: " + joinNaturally(objectives) + ".";
    }

    private List<String> buildQuestNpcMessages(ScenarioTemplate template,
                                               PlayerQuestProgress progress,
                                               QuestDialogueContext context,
                                               List<String> fallback) {
        List<String> configuredMessages = resolveQuestDialogueMessages(template, progress, context);
        return configuredMessages.isEmpty() ? fallback : configuredMessages;
    }

    private List<String> resolveQuestDialogueMessages(ScenarioTemplate template,
                                                      PlayerQuestProgress progress,
                                                      QuestDialogueContext context) {
        if (template == null || context == null || template.getQuestDialogues().isEmpty()) {
            return List.of();
        }

        List<String> keys = new ArrayList<>();
        keys.addAll(context.dialogueKeys());
        if (progress != null && progress.currentPhase() != null && !progress.currentPhase().isBlank()) {
            keys.add("phase." + progress.currentPhase());
            keys.add(progress.currentPhase());
        }

        for (String key : keys) {
            List<String> lines = template.getQuestDialogueLines(key);
            if (!lines.isEmpty()) {
                return lines;
            }
        }

        return List.of();
    }

    private QuestDialogueContext resolveStatusDialogueContext(Player player,
                                                              ScenarioTemplate template,
                                                              PlayerQuestProgress progress) {
        if (progress == null) {
            return QuestDialogueContext.OFFER;
        }
        if (progress.isCompleted()) {
            return QuestDialogueContext.COMPLETED;
        }
        if (progress.status() == QuestStatus.FAILED) {
            return QuestDialogueContext.FAILED;
        }
        if (progress.isActive()
            && inspectQuestObjectives(player, template, progress, null, false).complete()) {
            return QuestDialogueContext.READY;
        }
        return progress.isOffered() ? QuestDialogueContext.OFFERED : QuestDialogueContext.ACTIVE;
    }

    private QuestObjectiveCheck inspectQuestObjectives(Player player,
                                                       ScenarioTemplate template,
                                                       PlayerQuestProgress progress,
                                                       AINPC npc,
                                                       boolean requireTurnInInteraction) {
        if (template == null || template.getObjectives().isEmpty()) {
            return new QuestObjectiveCheck(true, List.of());
        }

        List<String> missingObjectives = new ArrayList<>();
        List<FeaturePackLoader.QuestEntryDefinition> objectives = template.getObjectives();
        for (int index = 0; index < objectives.size(); index++) {
            FeaturePackLoader.QuestEntryDefinition objective = objectives.get(index);
            if (!shouldShowObjectiveForCurrentStage(template, progress, objective)) {
                continue;
            }
            int requiredAmount = Math.max(1, objective.getAmount());
            int currentAmount = resolveObjectiveCurrentProgress(player, objective, progress, index);
            if (requireTurnInInteraction
                && matchesObjectiveType(objective, "deliver_to_npc")
                && npc == null
                && currentAmount >= requiredAmount) {
                currentAmount = 0;
            }
            if (currentAmount < requiredAmount) {
                missingObjectives.add(formatMissingObjective(objective, currentAmount, requiredAmount));
            }
        }

        return new QuestObjectiveCheck(missingObjectives.isEmpty(), List.copyOf(missingObjectives));
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

    private Map<String, String> buildStoryActionData(Map<String, String> configured,
                                                     ScenarioTemplate template,
                                                     Player player,
                                                     AINPC npc) {
        Map<String, String> data = new LinkedHashMap<>();
        if (configured != null) {
            data.putAll(configured);
        }
        if (template != null) {
            data.putIfAbsent("quest_template", template.getTemplateId());
            data.putIfAbsent("quest_code", template.getQuestCode());
        }
        if (player != null) {
            data.putIfAbsent("player_uuid", player.getUniqueId().toString());
            data.putIfAbsent("player_name", player.getName());
        }
        if (npc != null) {
            data.putIfAbsent("npc_name", npc.getName());
            if (npc.getUuid() != null) {
                data.putIfAbsent("npc_uuid", npc.getUuid().toString());
            }
        }
        return data;
    }

    private String storyActorId(Player player, AINPC npc, ScenarioTemplate template) {
        if (player != null) {
            return player.getUniqueId().toString();
        }
        if (npc != null && npc.getUuid() != null) {
            return npc.getUuid().toString();
        }
        return template != null ? template.getTemplateId() : "quest";
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
     * Verifica daca un scenariu poate fi declansat.
     */
    private boolean canTriggerScenario(ScenarioTemplate template, List<AINPC> npcs, List<Player> players) {
        if (template.requiresPlayer() && players.isEmpty()) {
            return false;
        }

        if (npcs.size() < template.getMinimumNpcCount()) {
            return false;
        }

        if (!canAssignMandatoryRoles(template, npcs, players)) {
            return false;
        }

        return switch (template.getType()) {
            case ROMANCE -> hasMixedGenders(npcs);
            case CONFLICT -> hasConflictingPersonalities(npcs);
            default -> true;
        };
    }

    private boolean canAssignMandatoryRoles(ScenarioTemplate template, List<AINPC> npcs, List<Player> players) {
        long requiredPlayers = template.getPlayerRoles().stream()
            .filter(role -> !role.isOptional())
            .count();
        if (players.size() < requiredPlayers) {
            return false;
        }

        List<AINPC> availableNpcs = new ArrayList<>(npcs);
        for (ScenarioRoleRule role : template.getNpcRoles()) {
            if (role.isOptional()) {
                continue;
            }

            AINPC selected = selectBestNpcForRole(availableNpcs, role);
            if (selected != null) {
                availableNpcs.remove(selected);
                continue;
            }

            if (availableNpcs.isEmpty() || role.hasHardRequirements()) {
                return false;
            }

            availableNpcs.remove(0);
        }

        return true;
    }

    private boolean hasMixedGenders(List<AINPC> npcs) {
        boolean hasMale = npcs.stream().anyMatch(npc -> "male".equalsIgnoreCase(npc.getGender()));
        boolean hasFemale = npcs.stream().anyMatch(npc -> "female".equalsIgnoreCase(npc.getGender()));
        return hasMale && hasFemale;
    }

    /**
     * Verifica daca exista personalitati conflictuale.
     */
    private boolean hasConflictingPersonalities(List<AINPC> npcs) {
        for (AINPC npc1 : npcs) {
            for (AINPC npc2 : npcs) {
                if (npc1 == npc2) {
                    continue;
                }

                NPCPersonality p1 = npc1.getPersonality();
                NPCPersonality p2 = npc2.getPersonality();

                if (Math.abs(p1.getAgreeableness() - p2.getAgreeableness()) > 0.5) {
                    return true;
                }

                if (p1.getNeuroticism() > 0.7 && p2.getNeuroticism() > 0.7) {
                    return true;
                }
            }
        }
        return false;
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

    private AINPC selectBestNpcForRole(List<AINPC> candidates, ScenarioRoleRule role) {
        if (candidates.isEmpty()) {
            return null;
        }

        AINPC bestNpc = null;
        int bestScore = Integer.MIN_VALUE;

        for (AINPC npc : candidates) {
            int score = scoreNpcForRole(npc, role);
            if (score > bestScore) {
                bestNpc = npc;
                bestScore = score;
            }
        }

        return bestScore == Integer.MIN_VALUE ? null : bestNpc;
    }

    private int scoreNpcForRole(AINPC npc, ScenarioRoleRule role) {
        if (!hasRequiredProfessions(npc, role.getRequiredProfessions())) {
            return Integer.MIN_VALUE;
        }

        if (!hasRequiredTraits(npc, role.getRequiredTraits())) {
            return Integer.MIN_VALUE;
        }

        int score = baseRoleScore(npc, role.getId());

        if (!role.getPreferredProfessions().isEmpty()) {
            boolean professionMatch = role.getPreferredProfessions().stream()
                .anyMatch(reference -> plugin.getFeaturePackLoader() != null
                    && plugin.getFeaturePackLoader().matchesProfession(npc.getOccupation(), reference));
            score += professionMatch ? 90 : -15;
        }

        for (String preferredTrait : role.getPreferredTraits()) {
            if (npc.hasTrait(preferredTrait)) {
                score += 25;
            }
        }

        return score;
    }

    private boolean hasRequiredProfessions(AINPC npc, List<String> requiredProfessions) {
        if (requiredProfessions == null || requiredProfessions.isEmpty()) {
            return true;
        }

        String occupation = npc.getOccupation();
        if (occupation == null || occupation.isBlank()) {
            return false;
        }

        FeaturePackLoader loader = plugin.getFeaturePackLoader();
        for (String requiredProfession : requiredProfessions) {
            if (loader != null && loader.matchesProfession(occupation, requiredProfession)) {
                return true;
            }

            if (normalizeScenarioToken(occupation).equals(normalizeScenarioToken(requiredProfession))) {
                return true;
            }
        }

        return false;
    }

    private boolean hasRequiredTraits(AINPC npc, List<String> requiredTraits) {
        if (requiredTraits == null || requiredTraits.isEmpty()) {
            return true;
        }

        for (String requiredTrait : requiredTraits) {
            if (!npc.hasTrait(requiredTrait)) {
                return false;
            }
        }

        return true;
    }

    private int baseRoleScore(AINPC npc, String roleId) {
        NPCPersonality personality = npc.getPersonality();
        NPCEmotions emotions = npc.getEmotions();

        return switch (roleId) {
            case "THIEF" -> scoreBoolean(personality.getConscientiousness() < 0.4
                && personality.getAgreeableness() < 0.5, 40);
            case "RESPONDER" -> scoreBoolean(personality.getConscientiousness() > 0.5
                || emotions.getTrust() > 0.5, 30);
            case "AGGRESSOR" -> scoreBoolean(personality.getAgreeableness() < 0.4
                || emotions.getAnger() > 0.5, 35);
            case "MEDIATOR" -> scoreBoolean(personality.getAgreeableness() > 0.6
                && personality.getExtraversion() > 0.5, 35);
            case "COWARD" -> scoreBoolean(personality.getNeuroticism() > 0.6
                || emotions.getFear() > 0.5, 35);
            case "LEADER" -> scoreBoolean(personality.getExtraversion() > 0.6
                && personality.getConscientiousness() > 0.5, 35);
            case "HOST" -> scoreBoolean(personality.getExtraversion() > 0.5
                && personality.getAgreeableness() > 0.5, 30);
            case "SUITOR" -> scoreBoolean(personality.getExtraversion() > 0.5, 25);
            case "ORIGINATOR" -> scoreBoolean(personality.getOpenness() > 0.6, 25);
            case "QUEST_GIVER" -> scoreBoolean(personality.getAgreeableness() > 0.45
                || personality.getExtraversion() > 0.45, 30);
            case "HELPER" -> scoreBoolean(personality.getAgreeableness() > 0.5
                || emotions.getTrust() > 0.55, 28);
            case "ANTAGONIST" -> scoreBoolean(personality.getAgreeableness() < 0.45
                || emotions.getAnger() > 0.45, 28);
            case "WITNESS" -> scoreBoolean(personality.getOpenness() > 0.45
                || personality.getExtraversion() > 0.45, 20);
            case "SELLER" -> scoreBoolean(personality.getExtraversion() > 0.5
                && personality.getConscientiousness() > 0.45, 25);
            case "BUYER" -> 10;
            default -> 0;
        };
    }

    private boolean matchesOccupation(AINPC npc, String... references) {
        if (references == null || references.length == 0) {
            return false;
        }

        String occupation = npc.getOccupation();
        if (occupation == null || occupation.isBlank()) {
            return false;
        }

        FeaturePackLoader loader = plugin.getFeaturePackLoader();
        for (String reference : references) {
            if (loader != null && loader.matchesProfession(occupation, reference)) {
                return true;
            }

            if (normalizeScenarioToken(occupation).equals(normalizeScenarioToken(reference))) {
                return true;
            }
        }

        return false;
    }

    private ConfigurationSection getQuestSettings() {
        ConfigurationSection questFile = plugin.getQuestConfig();
        if (questFile != null) {
            ConfigurationSection nested = questFile.getConfigurationSection("quests");
            return nested != null ? nested : questFile;
        }

        ConfigurationSection nested = plugin.getConfig().getConfigurationSection("quests");
        return nested != null ? nested : plugin.getConfig();
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
