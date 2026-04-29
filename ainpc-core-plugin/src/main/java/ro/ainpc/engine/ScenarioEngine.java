package ro.ainpc.engine;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import ro.ainpc.AINPCPlugin;
import ro.ainpc.npc.AINPC;
import ro.ainpc.npc.NPCEmotions;
import ro.ainpc.npc.NPCPersonality;
import ro.ainpc.world.WorldRegion;

import java.lang.reflect.Type;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
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
    private final Map<UUID, PlayerQuestProgress> activePlayerQuests;
    private final Map<UUID, Map<String, PlayerQuestProgress>> archivedPlayerQuests;

    public ScenarioEngine(AINPCPlugin plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
        this.activeScenarios = new ConcurrentHashMap<>();
        this.scenarioTemplates = new EnumMap<>(ScenarioType.class);
        this.questTemplates = new ArrayList<>();
        this.activePlayerQuests = new ConcurrentHashMap<>();
        this.archivedPlayerQuests = new ConcurrentHashMap<>();

        loadScenarioTemplates();
        loadPersistedQuestProgress();
    }

    public void reloadTemplates() {
        loadScenarioTemplates();
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
        theft.addRole("GUARD", "Garda care intervine", true);
        theft.addPhase("PLANNING", "Hotul planuieste furtul");
        theft.addPhase("EXECUTION", "Furtul are loc");
        theft.addPhase("DISCOVERY", "Victima descopera furtul");
        theft.addPhase("CONFLICT", "Confruntare intre parti");
        theft.addPhase("RESOLUTION", "Rezolvare - garda intervine sau hotul fuge");
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
                template.setQuestCode(definition.getQuestCode());
                template.setQuestGiverProfession(definition.getQuestGiverProfession());
                template.setObjectives(definition.getObjectives());
                template.setRewards(definition.getRewards());

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

                if (definition.getBaseType() == ScenarioType.QUEST && template.hasQuestBriefing()) {
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

    public QuestInteractionResult handleQuestInteraction(Player player, AINPC npc) {
        if (player == null || npc == null) {
            plugin.debug("[QuestEngine] handleQuestInteraction oprit: player sau npc este null.");
            return QuestInteractionResult.notHandled();
        }

        plugin.debug("[QuestEngine] handleQuestInteraction player=" + player.getName()
            + " npc=" + npc.getName()
            + " ocupatie=" + npc.getOccupation());
        ScenarioTemplate template = findQuestTemplateForNpc(npc);
        if (template == null || !template.hasQuestBriefing()) {
            plugin.debug("[QuestEngine] Nu exista template de quest pentru npc=" + npc.getName());
            return QuestInteractionResult.notHandled();
        }

        plugin.debug("[QuestEngine] Template gasit pentru npc=" + npc.getName()
            + " templateId=" + template.getTemplateId()
            + " title=" + resolveQuestTitle(template));

        UUID playerId = player.getUniqueId();
        PlayerQuestProgress currentProgress = activePlayerQuests.get(playerId);
        PlayerQuestProgress completedProgress = getCompletedQuestProgress(playerId, template.getTemplateId());
        if (currentProgress != null) {
            plugin.debug("[QuestEngine] Progres curent pentru player=" + player.getName()
                + " templateId=" + currentProgress.templateId()
                + " status=" + currentProgress.status());
        } else {
            plugin.debug("[QuestEngine] Player=" + player.getName() + " nu are progres de quest inregistrat.");
        }
        if (completedProgress != null) {
            plugin.debug("[QuestEngine] Quest deja completat pentru player=" + player.getName()
                + " templateId=" + template.getTemplateId());
            return QuestInteractionResult.handled(
                true,
                List.of("Ti-am dat deja recompensa pentru " + resolveQuestTitle(template) + ". Foloseste sabia cu cap."),
                List.of("&7Quest deja completat: &f" + resolveQuestTitle(template))
            );
        }

        if (currentProgress != null && !currentProgress.templateId().equals(template.getTemplateId())) {
            plugin.debug("[QuestEngine] Player=" + player.getName()
                + " are deja alt quest activ: " + currentProgress.templateId());
            return QuestInteractionResult.handled(
                true,
                List.of(currentProgress.isOffered()
                    ? "Ti-am oferit deja o alta misiune. Hotaraste-te intai la aceea."
                    : "Ai deja o alta misiune in desfasurare. Termina intai ce ai inceput."),
                List.of("&cAi deja alta misiune in curs: &f" + describeQuestProgress(currentProgress))
            );
        }

        if (currentProgress == null || !currentProgress.templateId().equals(template.getTemplateId())) {
            PlayerQuestProgress offeredProgress = setOfferedQuestProgress(playerId, player, template);
            offeredProgress = bindQuestProgressToNpc(playerId, template, offeredProgress, npc);
            plugin.debug("[QuestEngine] Quest oferit pentru player=" + player.getName()
                + " templateId=" + template.getTemplateId());

            List<String> npcMessages = List.of(
                "Am o treaba pentru tine.",
                buildQuestOfferMessage(template)
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
                List.of("Misiunea e a ta daca o vrei. Spune-mi clar daca o accepti."),
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
                List.of("Inca nu ai terminat tot ce ti-am cerut. Revino cand obiectivele sunt complete."),
                buildQuestStatusMessages(template, currentProgress, player, npc.getName())
            );
        }

        consumeQuestObjectives(player.getInventory(), template.getObjectives());
        List<String> rewardNotes = grantQuestRewards(player, template.getRewards());
        player.updateInventory();

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
            List.of("Perfect. Exact materialele de care aveam nevoie.", "Poftim sabia promisa. Sa-ti fie de folos."),
            systemMessages
        );
    }

    public QuestInteractionResult acceptQuest(Player player, AINPC npc) {
        if (player == null || npc == null) {
            plugin.debug("[QuestEngine] acceptQuest oprit: player sau npc este null.");
            return QuestInteractionResult.notHandled();
        }

        ScenarioTemplate template = findQuestTemplateForNpc(npc);
        if (template == null || !template.hasQuestBriefing()) {
            plugin.debug("[QuestEngine] acceptQuest fara template pentru npc=" + npc.getName());
            return QuestInteractionResult.notHandled();
        }

        UUID playerId = player.getUniqueId();
        PlayerQuestProgress currentProgress = activePlayerQuests.get(playerId);
        PlayerQuestProgress completedProgress = getCompletedQuestProgress(playerId, template.getTemplateId());
        if (completedProgress != null) {
            return QuestInteractionResult.handled(
                true,
                List.of("Misiunea asta este deja incheiata intre noi."),
                buildQuestStatusMessages(template, completedProgress, player, npc.getName())
            );
        }

        if (currentProgress != null && !currentProgress.templateId().equals(template.getTemplateId())) {
            return QuestInteractionResult.handled(
                true,
                List.of(currentProgress.isOffered()
                    ? "Intai raspunde la cealalta misiune pe care ti-am oferit-o."
                    : "Ai deja alta misiune activa. Revino dupa ce o termini."),
                List.of("&cAi deja alta misiune in curs: &f" + describeQuestProgress(currentProgress))
            );
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
                List.of("Ai acceptat deja misiunea. Ma astept sa te intorci cu ce ti-am cerut."),
                buildQuestStatusMessages(template, currentProgress, player, npc.getName())
            );
        }

        PlayerQuestProgress acceptedProgress = setActiveQuestProgress(playerId, player, template);
        acceptedProgress = bindQuestProgressToNpc(playerId, template, acceptedProgress, npc);
        plugin.debug("[QuestEngine] Quest acceptat pentru player=" + player.getName()
            + " templateId=" + template.getTemplateId());
        return QuestInteractionResult.handled(
            true,
            List.of("Bine. Ma bazez pe tine.", "Intoarce-te cand ai terminat."),
            buildQuestStatusMessages(template, acceptedProgress, player, npc.getName())
        );
    }

    public QuestInteractionResult declineQuest(Player player, AINPC npc) {
        if (player == null || npc == null) {
            plugin.debug("[QuestEngine] declineQuest oprit: player sau npc este null.");
            return QuestInteractionResult.notHandled();
        }

        ScenarioTemplate template = findQuestTemplateForNpc(npc);
        if (template == null || !template.hasQuestBriefing()) {
            plugin.debug("[QuestEngine] declineQuest fara template pentru npc=" + npc.getName());
            return QuestInteractionResult.notHandled();
        }

        UUID playerId = player.getUniqueId();
        PlayerQuestProgress currentProgress = activePlayerQuests.get(playerId);
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

        ScenarioTemplate template = findQuestTemplateForNpc(npc);
        if (template == null || !template.hasQuestBriefing()) {
            plugin.debug("[QuestEngine] abandonQuest fara template pentru npc=" + npc.getName());
            return QuestInteractionResult.notHandled();
        }

        UUID playerId = player.getUniqueId();
        PlayerQuestProgress currentProgress = activePlayerQuests.get(playerId);
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
            List.of("Am inteles. Consider misiunea abandonata."),
            buildQuestStatusMessages(template, failedProgress, player, npc.getName())
        );
    }

    public QuestInteractionResult startQuestManually(Player player, AINPC npc) {
        if (player == null || npc == null) {
            plugin.debug("[QuestEngine] startQuestManually oprit: player sau npc este null.");
            return QuestInteractionResult.notHandled();
        }

        ScenarioTemplate template = findQuestTemplateForNpc(npc);
        if (template == null || !template.hasQuestBriefing()) {
            plugin.debug("[QuestEngine] startQuestManually fara template pentru npc=" + npc.getName());
            return QuestInteractionResult.notHandled();
        }

        UUID playerId = player.getUniqueId();
        removeArchivedQuestProgress(playerId, template.getTemplateId());
        PlayerQuestProgress offeredProgress = setOfferedQuestProgress(playerId, player, template);
        offeredProgress = bindQuestProgressToNpc(playerId, template, offeredProgress, npc);
        plugin.debug("[QuestEngine] startQuestManually a oferit questul pentru player=" + player.getName()
            + " templateId=" + template.getTemplateId());

        List<String> npcMessages = List.of(
            "Am o treaba pentru tine.",
            buildQuestOfferMessage(template)
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

        ScenarioTemplate template = findQuestTemplateForNpc(npc);
        if (template == null || !template.hasQuestBriefing()) {
            plugin.debug("[QuestEngine] resetQuestProgress fara template pentru npc=" + npc.getName());
            return false;
        }

        UUID playerId = player.getUniqueId();
        boolean removedActive = removeActiveQuestProgress(playerId, template.getTemplateId());
        boolean removedArchived = removeArchivedQuestProgress(playerId, template.getTemplateId());
        if (!removedActive && !removedArchived) {
            plugin.debug("[QuestEngine] resetQuestProgress fara progres potrivit pentru player="
                + player.getName() + " templateId=" + template.getTemplateId());
            return false;
        }

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

        ScenarioTemplate template = findQuestTemplateForNpc(npc);
        if (template == null || !template.hasQuestBriefing()) {
            plugin.debug("[QuestEngine] forceCompleteQuest fara template pentru npc=" + npc.getName());
            return QuestInteractionResult.notHandled();
        }

        plugin.debug("[QuestEngine] forceCompleteQuest pentru player=" + player.getName()
            + " templateId=" + template.getTemplateId());

        List<String> rewardNotes = grantQuestRewards(player, template.getRewards());
        player.updateInventory();

        markQuestCompleted(player.getUniqueId(), template);
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
            List.of("In regula. Consider misiunea terminata.", "Poftim rasplata promisa."),
            systemMessages
        );
    }

    public String getQuestTitle(AINPC npc) {
        ScenarioTemplate template = findQuestTemplateForNpc(npc);
        if (template == null || !template.hasQuestBriefing()) {
            return "";
        }
        return resolveQuestTitle(template);
    }

    public QuestInteractionResult getQuestStatus(Player player, AINPC npc) {
        if (player == null || npc == null) {
            return QuestInteractionResult.notHandled();
        }

        ScenarioTemplate template = findQuestTemplateForNpc(npc);
        if (template == null || !template.hasQuestBriefing()) {
            return QuestInteractionResult.notHandled();
        }

        UUID playerId = player.getUniqueId();
        PlayerQuestProgress currentProgress = activePlayerQuests.get(playerId);
        PlayerQuestProgress completedProgress = getCompletedQuestProgress(playerId, template.getTemplateId());
        PlayerQuestProgress failedProgress = getFailedQuestProgress(playerId, template.getTemplateId());

        if (completedProgress != null) {
            return QuestInteractionResult.handled(
                true,
                List.of("Ti-ai dus la capat datoria pentru misiunea asta."),
                buildQuestStatusMessages(template, completedProgress, player, npc.getName())
            );
        }

        if (failedProgress != null) {
            return QuestInteractionResult.handled(
                true,
                List.of("Misiunea asta a fost abandonata sau esuata."),
                buildQuestStatusMessages(template, failedProgress, player, npc.getName())
            );
        }

        if (currentProgress != null && currentProgress.templateId().equals(template.getTemplateId())) {
            return QuestInteractionResult.handled(
                true,
                List.of(currentProgress.isOffered()
                    ? "Inca astept sa-mi spui daca accepti."
                    : "Asa stai acum cu misiunea."),
                buildQuestStatusMessages(template, currentProgress, player, npc.getName())
            );
        }

        if (currentProgress != null) {
            return QuestInteractionResult.handled(
                true,
                List.of("Nu asta este misiunea la care lucrezi acum."),
                List.of(
                    "&6[Quest] &f" + resolveQuestTitle(template),
                    "&7Status pentru acest NPC: &fDisponibil",
                    "&cAi deja alta misiune in curs: &f" + describeQuestProgress(currentProgress)
                )
            );
        }

        return QuestInteractionResult.handled(
            true,
            List.of("Misiunea este disponibila, dar inca nu ai acceptat-o."),
            buildQuestStatusMessages(template, null, player, npc.getName())
        );
    }

    public void recordNpcConversation(Player player, AINPC npc) {
        if (player == null || npc == null) {
            return;
        }

        PlayerQuestProgress progress = activePlayerQuests.get(player.getUniqueId());
        if (progress == null || !progress.isCurrent()) {
            return;
        }

        ScenarioTemplate template = resolveTemplateForProgress(progress, npc);
        if (template == null) {
            return;
        }

        PlayerQuestProgress refreshedProgress = refreshTrackedQuestProgress(player, template, progress);
        trackNpcObjectiveProgress(player, npc, template, refreshedProgress);
    }

    public void recordRegionVisit(Player player) {
        if (player == null) {
            return;
        }

        PlayerQuestProgress progress = activePlayerQuests.get(player.getUniqueId());
        if (progress == null || !progress.isCurrent()) {
            return;
        }

        ScenarioTemplate template = resolveTemplateForProgress(progress, null);
        if (template == null || !hasObjectiveType(template, "visit_region")) {
            return;
        }

        WorldRegion region = findCurrentRegion(player.getLocation());
        if (region == null) {
            return;
        }

        Map<String, Integer> updatedProgress = new LinkedHashMap<>(progress.objectiveProgress());
        boolean changed = false;
        List<FeaturePackLoader.QuestEntryDefinition> objectives = template.getObjectives();
        for (int index = 0; index < objectives.size(); index++) {
            FeaturePackLoader.QuestEntryDefinition objective = objectives.get(index);
            if (!matchesObjectiveType(objective, "visit_region") || !matchesRegionObjective(objective, region)) {
                continue;
            }

            String objectiveKey = buildObjectiveKey(objective, index);
            changed |= incrementObjectiveProgress(updatedProgress, objectiveKey, objective.getAmount());
        }

        if (changed) {
            updateTrackedQuestProgress(player.getUniqueId(), template, progress, updatedProgress);
        }
    }

    public void recordMobKill(Player player, Entity entity) {
        if (player == null || entity == null) {
            return;
        }

        PlayerQuestProgress progress = activePlayerQuests.get(player.getUniqueId());
        if (progress == null || !progress.isCurrent()) {
            return;
        }

        ScenarioTemplate template = resolveTemplateForProgress(progress, null);
        if (template == null || !hasObjectiveType(template, "kill_mob")) {
            return;
        }

        Map<String, Integer> updatedProgress = new LinkedHashMap<>(progress.objectiveProgress());
        boolean changed = false;
        List<FeaturePackLoader.QuestEntryDefinition> objectives = template.getObjectives();
        for (int index = 0; index < objectives.size(); index++) {
            FeaturePackLoader.QuestEntryDefinition objective = objectives.get(index);
            if (!matchesObjectiveType(objective, "kill_mob") || !matchesMobObjective(objective, entity)) {
                continue;
            }

            String objectiveKey = buildObjectiveKey(objective, index);
            changed |= incrementObjectiveProgress(updatedProgress, objectiveKey, objective.getAmount());
        }

        if (changed) {
            updateTrackedQuestProgress(player.getUniqueId(), template, progress, updatedProgress);
        }
    }

    private PlayerQuestProgress setOfferedQuestProgress(UUID playerId, Player player, ScenarioTemplate template) {
        return setCurrentQuestProgress(playerId, player, template, QuestStatus.OFFERED);
    }

    private PlayerQuestProgress setActiveQuestProgress(UUID playerId, Player player, ScenarioTemplate template) {
        return setCurrentQuestProgress(playerId, player, template, QuestStatus.ACTIVE);
    }

    private PlayerQuestProgress setCurrentQuestProgress(UUID playerId,
                                                        Player player,
                                                        ScenarioTemplate template,
                                                        QuestStatus status) {
        long now = System.currentTimeMillis();
        PlayerQuestProgress existingProgress = activePlayerQuests.get(playerId);
        PlayerQuestProgress matchingProgress = existingProgress != null && existingProgress.templateId().equals(template.getTemplateId())
            ? existingProgress
            : null;
        long startedAt = existingProgress != null && existingProgress.templateId().equals(template.getTemplateId())
            ? existingProgress.startedAt()
            : now;

        PlayerQuestProgress currentProgress = new PlayerQuestProgress(
            template.getTemplateId(),
            template.getQuestCode(),
            status,
            startedAt,
            0L,
            now,
            resolveQuestPhase(template, status, matchingProgress),
            buildObjectiveProgressSnapshot(
                player != null ? player.getInventory() : null,
                template,
                matchingProgress != null ? matchingProgress.objectiveProgress() : Map.of()
            ),
            matchingProgress != null ? matchingProgress.questVariables() : Map.of()
        );
        activePlayerQuests.put(playerId, currentProgress);
        removeArchivedQuestProgress(playerId, template.getTemplateId());
        persistQuestProgressAsync(playerId, currentProgress);
        return currentProgress;
    }

    private void markQuestCompleted(UUID playerId, ScenarioTemplate template) {
        long now = System.currentTimeMillis();
        PlayerQuestProgress activeProgress = activePlayerQuests.get(playerId);
        long startedAt = activeProgress != null && activeProgress.templateId().equals(template.getTemplateId())
            ? activeProgress.startedAt()
            : now;

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
        PlayerQuestProgress activeProgress = activePlayerQuests.get(playerId);
        long startedAt = activeProgress != null && activeProgress.templateId().equals(template.getTemplateId())
            ? activeProgress.startedAt()
            : now;

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
        PlayerQuestProgress currentProgress = activePlayerQuests.get(playerId);
        if (currentProgress == null || !currentProgress.templateId().equals(templateId)) {
            return false;
        }

        activePlayerQuests.remove(playerId);
        return true;
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

        String sql = """
            SELECT player_uuid, template_id, quest_code, status, started_at, completed_at,
                   current_phase, objective_progress, quest_variables, updated_at
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

                PlayerQuestProgress progress = new PlayerQuestProgress(
                    rs.getString("template_id"),
                    rs.getString("quest_code"),
                    status,
                    readNullableLong(rs, "started_at"),
                    readNullableLong(rs, "completed_at"),
                    rs.getLong("updated_at"),
                    readTextOrEmpty(rs, "current_phase"),
                    parseObjectiveProgress(readTextOrEmpty(rs, "objective_progress")),
                    parseQuestVariables(readTextOrEmpty(rs, "quest_variables"))
                );
                if (registerLoadedQuestProgress(playerId, progress)) {
                    if (progress.isCurrent()) {
                        activeCount++;
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
            PlayerQuestProgress existingProgress = activePlayerQuests.get(playerId);
            if (existingProgress == null || progress.updatedAt() >= existingProgress.updatedAt()) {
                activePlayerQuests.put(playerId, progress);
                if (existingProgress != null && !existingProgress.templateId().equals(progress.templateId())) {
                    plugin.getLogger().warning("Jucatorul " + playerId
                        + " avea mai multe quest-uri curente persistate. Il pastrez pe cel mai recent: "
                        + progress.templateId());
                }
                return true;
            }

            plugin.getLogger().warning("Ignor quest curent mai vechi pentru jucatorul " + playerId
                + ": " + progress.templateId());
            return false;
        }

        archiveQuestProgress(playerId, progress);
        return true;
    }

    private void persistQuestProgressAsync(UUID playerId, PlayerQuestProgress progress) {
        if (playerId == null || progress == null) {
            return;
        }

        plugin.getDatabaseManager().runAsync(() -> persistQuestProgress(playerId, progress));
    }

    private void persistQuestProgress(UUID playerId, PlayerQuestProgress progress) {
        String sql = """
            INSERT INTO player_quests (
                player_uuid, template_id, quest_code, status, started_at, completed_at,
                current_phase, objective_progress, quest_variables, updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(player_uuid, template_id) DO UPDATE SET
                quest_code = excluded.quest_code,
                status = excluded.status,
                started_at = excluded.started_at,
                completed_at = excluded.completed_at,
                current_phase = excluded.current_phase,
                objective_progress = excluded.objective_progress,
                quest_variables = excluded.quest_variables,
                updated_at = excluded.updated_at
        """;

        try {
            if (progress.isCurrent()) {
                deleteOtherCurrentQuestProgress(playerId, progress.templateId());
            }

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
                stmt.setString(8, serializeJson(progress.objectiveProgress()));
                stmt.setString(9, serializeJson(progress.questVariables()));
                stmt.setLong(10, progress.updatedAt());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Nu am putut salva progresul quest-ului " + progress.templateId()
                + " pentru player " + playerId + ": " + e.getMessage());
        }
    }

    private void deleteOtherCurrentQuestProgress(UUID playerId, String currentTemplateId) throws SQLException {
        String sql = """
            DELETE FROM player_quests
            WHERE player_uuid = ? AND status IN (?, ?) AND template_id <> ?
        """;

        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement(sql)) {
            stmt.setString(1, playerId.toString());
            stmt.setString(2, QuestStatus.OFFERED.storageValue());
            stmt.setString(3, QuestStatus.ACTIVE.storageValue());
            stmt.setString(4, currentTemplateId);
            stmt.executeUpdate();
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

        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement(sql)) {
            stmt.setString(1, playerId.toString());
            stmt.setString(2, templateId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Nu am putut sterge progresul quest-ului " + templateId
                + " pentru player " + playerId + ": " + e.getMessage());
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

        String refreshedPhase = progress.currentPhase().isBlank()
            ? resolveQuestPhase(template, progress.status(), progress)
            : progress.currentPhase();
        Map<String, Integer> refreshedObjectiveProgress = buildObjectiveProgressSnapshot(
            player.getInventory(),
            template,
            progress.objectiveProgress()
        );

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
        activePlayerQuests.put(player.getUniqueId(), refreshedProgress);
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
            if (!matchesObjectiveType(objective, "talk_to_npc")) {
                continue;
            }
            if (!matchesNpcObjective(objective, npc, template, progress)) {
                continue;
            }

            String objectiveKey = buildObjectiveKey(objective, index);
            changed |= incrementObjectiveProgress(updatedProgress, objectiveKey, objective.getAmount());
        }

        if (!changed) {
            return progress;
        }

        return updateTrackedQuestProgress(player.getUniqueId(), template, progress, updatedProgress);
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
        activePlayerQuests.put(playerId, updatedProgress);
        persistQuestProgressAsync(playerId, updatedProgress);
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
        if (normalizedProgress.equals(progress.objectiveProgress()) && updatedPhase.equals(progress.currentPhase())) {
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
            progress.questVariables()
        );
        activePlayerQuests.put(playerId, updatedProgress);
        persistQuestProgressAsync(playerId, updatedProgress);
        return updatedProgress;
    }

    private String resolveQuestPhase(ScenarioTemplate template,
                                     QuestStatus status,
                                     PlayerQuestProgress existingProgress) {
        Map<String, Integer> objectiveProgress = existingProgress != null ? existingProgress.objectiveProgress() : Map.of();
        return resolveQuestPhase(template, status, existingProgress, objectiveProgress);
    }

    private String resolveQuestPhase(ScenarioTemplate template,
                                     QuestStatus status,
                                     PlayerQuestProgress existingProgress,
                                     Map<String, Integer> objectiveProgress) {
        if (template == null || status == null) {
            return "";
        }

        String existingPhase = existingProgress != null ? existingProgress.currentPhase() : "";
        return switch (status) {
            case NOT_STARTED -> "";
            case OFFERED -> !existingPhase.isBlank() ? existingPhase : getFirstQuestPhase(template);
            case ACTIVE -> resolveActiveQuestPhase(template, existingPhase, existingProgress, objectiveProgress);
            case COMPLETED -> {
                String lastPhase = getLastQuestPhase(template);
                yield !lastPhase.isBlank() ? lastPhase : existingPhase;
            }
            case FAILED -> !existingPhase.isBlank() ? existingPhase : getDefaultActiveQuestPhase(template);
        };
    }

    private String resolveActiveQuestPhase(ScenarioTemplate template,
                                           String existingPhase,
                                           PlayerQuestProgress existingProgress,
                                           Map<String, Integer> objectiveProgress) {
        if (areObjectivesSatisfied(template, objectiveProgress)) {
            String lastPhase = getLastQuestPhase(template);
            if (!lastPhase.isBlank()) {
                return lastPhase;
            }
        }

        if (existingProgress != null && existingProgress.isOffered()) {
            String advancedPhase = advanceQuestPhase(template, existingPhase);
            if (!advancedPhase.isBlank()) {
                return advancedPhase;
            }
        }

        if (!existingPhase.isBlank()) {
            return existingPhase;
        }

        return getDefaultActiveQuestPhase(template);
    }

    private String getFirstQuestPhase(ScenarioTemplate template) {
        return template != null && !template.getPhases().isEmpty() ? template.getPhases().get(0) : "";
    }

    private String getDefaultActiveQuestPhase(ScenarioTemplate template) {
        if (template == null || template.getPhases().isEmpty()) {
            return "";
        }

        return template.getPhases().size() > 1 ? template.getPhases().get(1) : template.getPhases().get(0);
    }

    private String getLastQuestPhase(ScenarioTemplate template) {
        return template != null && !template.getPhases().isEmpty()
            ? template.getPhases().get(template.getPhases().size() - 1)
            : "";
    }

    private String advanceQuestPhase(ScenarioTemplate template, String currentPhase) {
        if (template == null || template.getPhases().isEmpty()) {
            return "";
        }

        if (currentPhase == null || currentPhase.isBlank()) {
            return getDefaultActiveQuestPhase(template);
        }

        int currentIndex = template.getPhases().indexOf(currentPhase);
        if (currentIndex >= 0 && currentIndex < template.getPhases().size() - 1) {
            return template.getPhases().get(currentIndex + 1);
        }

        return currentPhase;
    }

    private boolean areObjectivesSatisfied(ScenarioTemplate template, Map<String, Integer> objectiveProgress) {
        if (template == null || template.getObjectives().isEmpty()) {
            return true;
        }

        Map<String, Integer> safeProgress = objectiveProgress != null ? objectiveProgress : Map.of();
        List<FeaturePackLoader.QuestEntryDefinition> objectives = template.getObjectives();
        for (int index = 0; index < objectives.size(); index++) {
            FeaturePackLoader.QuestEntryDefinition objective = objectives.get(index);
            String objectiveKey = buildObjectiveKey(objective, index);
            if (safeProgress.getOrDefault(objectiveKey, 0) < objective.getAmount()) {
                return false;
            }
        }

        return true;
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

    private boolean hasObjectiveType(ScenarioTemplate template, String type) {
        if (template == null || template.getObjectives().isEmpty()) {
            return false;
        }

        for (FeaturePackLoader.QuestEntryDefinition objective : template.getObjectives()) {
            if (matchesObjectiveType(objective, type)) {
                return true;
            }
        }

        return false;
    }

    private boolean matchesObjectiveType(FeaturePackLoader.QuestEntryDefinition objective, String expectedType) {
        if (objective == null) {
            return false;
        }

        return normalizeObjectiveType(objective.getType()).equals(normalizeObjectiveType(expectedType));
    }

    private String normalizeObjectiveType(String type) {
        String normalized = normalizeReference(type);
        return switch (normalized) {
            case "", "item", "collect", "collectitem", "collect_item", "fetch", "gather" -> "collect_item";
            case "deliver", "deliveritem", "deliver_item", "deliver_to_npc", "turnin", "turn_in" -> "deliver_to_npc";
            case "talk", "speak", "conversation", "talk_to_npc", "speak_to_npc" -> "talk_to_npc";
            case "visit", "travel", "go_to", "visit_region", "enter_region" -> "visit_region";
            case "kill", "slay", "defeat", "kill_mob" -> "kill_mob";
            default -> normalized;
        };
    }

    private boolean usesInventoryProgress(FeaturePackLoader.QuestEntryDefinition objective) {
        String objectiveType = normalizeObjectiveType(objective != null ? objective.getType() : "");
        return "collect_item".equals(objectiveType) || "deliver_to_npc".equals(objectiveType);
    }

    private boolean shouldConsumeObjectiveItem(FeaturePackLoader.QuestEntryDefinition objective) {
        return usesInventoryProgress(objective);
    }

    private Map<String, Integer> buildObjectiveProgressSnapshot(PlayerInventory inventory,
                                                                ScenarioTemplate template,
                                                                Map<String, Integer> existingProgress) {
        LinkedHashMap<String, Integer> snapshot = new LinkedHashMap<>();
        if (template == null || template.getObjectives().isEmpty()) {
            return snapshot;
        }

        Map<String, Integer> existingValues = existingProgress != null ? existingProgress : Map.of();
        List<FeaturePackLoader.QuestEntryDefinition> objectives = template.getObjectives();
        for (int index = 0; index < objectives.size(); index++) {
            FeaturePackLoader.QuestEntryDefinition objective = objectives.get(index);
            String objectiveKey = buildObjectiveKey(objective, index);
            int progressValue = Math.max(0, existingValues.getOrDefault(objectiveKey, 0));

            Material material = resolveQuestMaterial(objective);
            if (inventory != null && material != null && usesInventoryProgress(objective)) {
                progressValue = Math.min(objective.getAmount(), countMaterial(inventory, material));
            } else {
                progressValue = Math.min(objective.getAmount(), progressValue);
            }

            snapshot.put(objectiveKey, progressValue);
        }

        return Collections.unmodifiableMap(snapshot);
    }

    private Map<String, Integer> buildCompletedObjectiveProgress(ScenarioTemplate template,
                                                                 Map<String, Integer> existingProgress) {
        LinkedHashMap<String, Integer> completedProgress = new LinkedHashMap<>(
            buildObjectiveProgressSnapshot(null, template, existingProgress)
        );
        if (template == null) {
            return Collections.unmodifiableMap(completedProgress);
        }

        List<FeaturePackLoader.QuestEntryDefinition> objectives = template.getObjectives();
        for (int index = 0; index < objectives.size(); index++) {
            FeaturePackLoader.QuestEntryDefinition objective = objectives.get(index);
            completedProgress.put(buildObjectiveKey(objective, index), Math.max(0, objective.getAmount()));
        }
        return Collections.unmodifiableMap(completedProgress);
    }

    private String buildObjectiveKey(FeaturePackLoader.QuestEntryDefinition objective, int index) {
        String type = objective != null && objective.getType() != null && !objective.getType().isBlank()
            ? normalize(objective.getType())
            : "objective";
        String itemId = objective != null && objective.getItemId() != null && !objective.getItemId().isBlank()
            ? normalize(objective.getItemId())
            : "entry";
        return type + ":" + itemId + ":" + index;
    }

    private boolean incrementObjectiveProgress(Map<String, Integer> progressByObjective,
                                               String objectiveKey,
                                               int objectiveAmount) {
        int currentValue = Math.max(0, progressByObjective.getOrDefault(objectiveKey, 0));
        int updatedValue = Math.min(Math.max(1, objectiveAmount), currentValue + 1);
        if (updatedValue == currentValue) {
            return false;
        }

        progressByObjective.put(objectiveKey, updatedValue);
        return true;
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

    private boolean matchesObjectiveReference(String reference, String... candidates) {
        String normalizedReference = normalizeReference(stripObjectivePrefix(reference));
        if (normalizedReference.isBlank() || candidates == null || candidates.length == 0) {
            return false;
        }

        for (String candidate : candidates) {
            String normalizedCandidate = normalizeReference(candidate);
            if (!normalizedCandidate.isBlank() && normalizedCandidate.equals(normalizedReference)) {
                return true;
            }
        }

        return false;
    }

    private String stripObjectivePrefix(String reference) {
        if (reference == null || reference.isBlank()) {
            return "";
        }

        String trimmed = reference.trim();
        int prefixSeparator = trimmed.indexOf(':');
        if (prefixSeparator <= 0) {
            return trimmed;
        }

        String prefix = normalizeReference(trimmed.substring(0, prefixSeparator));
        return switch (prefix) {
            case "npc", "name", "profession", "region", "tag", "type", "mob", "entity" ->
                trimmed.substring(prefixSeparator + 1);
            default -> trimmed;
        };
    }

    private String normalizeReference(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return value.toLowerCase(Locale.ROOT)
            .replace("minecraft:", "")
            .replaceAll("[^\\p{L}\\p{Nd}]+", "_")
            .replaceAll("^_+|_+$", "")
            .replaceAll("_+", "_");
    }

    private ScenarioTemplate findQuestTemplateForNpc(AINPC npc) {
        ScenarioTemplate configuredTemplate = questTemplates.stream()
            .filter(ScenarioTemplate::hasQuestBriefing)
            .filter(template -> matchesQuestGiver(npc, template))
            .findFirst()
            .orElse(null);
        if (configuredTemplate != null) {
            return configuredTemplate;
        }

        return shouldUseSimpleQuestForAllNpcs() ? buildSimpleQuestTemplate(npc) : null;
    }

    private boolean shouldUseSimpleQuestForAllNpcs() {
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
        String professionId = profession != null ? normalize(profession.getId()) : "";
        String professionName = profession != null && profession.getName() != null && !profession.getName().isBlank()
            ? profession.getName()
            : npc.getOccupation();
        if (professionName == null || professionName.isBlank()) {
            professionName = "localnicul";
        }

        SimpleQuestProfile defaultProfile = switch (professionId) {
            case "blacksmith" -> new SimpleQuestProfile(
                "Provizii pentru fierarie",
                Material.IRON_INGOT,
                3,
                Material.IRON_SWORD,
                1,
                "Adu-mi " + formatQuestAmount(3, Material.IRON_INGOT),
                npc.getName() + " are nevoie de fier pentru o arma noua."
            );
            case "farmer" -> new SimpleQuestProfile(
                "Recolta de dimineata",
                Material.WHEAT,
                10,
                Material.BREAD,
                4,
                "Adu-mi " + formatQuestAmount(10, Material.WHEAT),
                npc.getName() + " are nevoie de provizii pentru hambar."
            );
            case "guard" -> new SimpleQuestProfile(
                "Provizii pentru garda",
                Material.ARROW,
                8,
                Material.SHIELD,
                1,
                "Adu-mi " + formatQuestAmount(8, Material.ARROW),
                npc.getName() + " isi pregateste echipamentul pentru patrula."
            );
            case "merchant" -> new SimpleQuestProfile(
                "Marfa pentru taraba",
                Material.PAPER,
                6,
                Material.EMERALD,
                2,
                "Adu-mi " + formatQuestAmount(6, Material.PAPER),
                npc.getName() + " vrea sa-si completeze registrele si ofertele."
            );
            case "innkeeper" -> new SimpleQuestProfile(
                "Provizii pentru han",
                Material.WHEAT,
                6,
                Material.COOKED_BEEF,
                3,
                "Adu-mi " + formatQuestAmount(6, Material.WHEAT),
                npc.getName() + " pregateste mesele pentru calatorii din han."
            );
            case "priest" -> new SimpleQuestProfile(
                "Pregatiri pentru altar",
                Material.CANDLE,
                4,
                Material.EXPERIENCE_BOTTLE,
                2,
                "Adu-mi " + formatQuestAmount(4, Material.CANDLE),
                npc.getName() + " are nevoie de lumina pentru altar."
            );
            case "healer" -> new SimpleQuestProfile(
                "Ierburi pentru leacuri",
                Material.DANDELION,
                6,
                Material.HONEY_BOTTLE,
                2,
                "Adu-mi " + formatQuestAmount(6, Material.DANDELION),
                npc.getName() + " pregateste leacuri si ii lipsesc plantele."
            );
            default -> new SimpleQuestProfile(
                resolveConfiguredSimpleQuestTitle(professionName),
                resolveConfiguredQuestMaterial("simple.objective.item", Material.OAK_PLANKS),
                Math.max(1, getQuestSettings().getInt("simple.objective.amount", 3)),
                resolveConfiguredQuestMaterial("simple.reward.item", Material.EMERALD),
                Math.max(1, getQuestSettings().getInt("simple.reward.amount", 1)),
                "Adu-mi " + formatQuestAmount(
                    Math.max(1, getQuestSettings().getInt("simple.objective.amount", 3)),
                    resolveConfiguredQuestMaterial("simple.objective.item", Material.OAK_PLANKS)
                ),
                npc.getName() + " are nevoie de ajutor cu treburi obisnuite de " + professionName + "."
            );
        };
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

    private String applyQuestFallbackPlaceholders(String value,
                                                  AINPC npc,
                                                  String professionName,
                                                  String objectiveText,
                                                  String rewardText) {
        if (value == null || value.isBlank()) {
            return value == null ? "" : value;
        }

        return value
            .replace("{npc}", npc != null ? npc.getName() : "NPC")
            .replace("{profession}", professionName != null && !professionName.isBlank() ? professionName : "localnic")
            .replace("{objective}", objectiveText != null ? objectiveText : "materiale")
            .replace("{reward}", rewardText != null ? rewardText : "o recompensa");
    }

    private String sanitizeConfigKey(String value) {
        String normalized = normalize(value).replace('-', '_').replace(' ', '_');
        return normalized.replaceAll("[^a-z0-9_]", "");
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

            if (normalize(occupation).equals(normalize(reference))) {
                return true;
            }
        }

        return false;
    }

    private List<String> buildQuestBriefingMessages(ScenarioTemplate template) {
        List<String> lines = new ArrayList<>();
        lines.add("&6[Quest] &f" + resolveQuestTitle(template));

        String questGiver = resolveProfessionName(template.getQuestGiverProfession());
        if (!questGiver.isBlank()) {
            lines.add("&7Misiune de la: &f" + questGiver);
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
            lines.add("&7Misiunea este disponibila, dar nu ai acceptat-o inca.");
            if (npcName != null && !npcName.isBlank()) {
                lines.add("&eAcceptare: &fScrie &aaccept &fsau foloseste &a/npcquest accept " + npcName);
            }
            return lines;
        }

        if (progress.isOffered()) {
            if (npcName != null && !npcName.isBlank()) {
                lines.add("&eAcceptare: &fScrie &aaccept &fsau foloseste &a/npcquest accept " + npcName);
                lines.add("&cRefuz: &fScrie &arefuz &fsau foloseste &a/npcquest decline " + npcName);
            }
            return lines;
        }

        if (progress.isActive()) {
            List<String> progressLines = buildObjectiveProgressLines(template, progress);
            if (!progressLines.isEmpty()) {
                lines.add("&eProgres:");
                lines.addAll(progressLines);
            }

            QuestObjectiveCheck objectiveCheck = inspectQuestObjectives(player, template, progress, null, false);
            if (player == null) {
                lines.add("&7Quest activ.");
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
            lines.add("&aQuest finalizat.");
        } else if (progress.status() == QuestStatus.FAILED) {
            lines.add("&cQuest abandonat sau esuat.");
            lines.add("&7Poti cere din nou misiunea daca vrei sa reincepi.");
        }

        return lines;
    }

    private String formatQuestStatus(QuestStatus status) {
        if (status == null) {
            return "Necunoscut";
        }

        return switch (status) {
            case NOT_STARTED -> "Disponibil";
            case OFFERED -> "Oferit, asteapta acceptarea";
            case ACTIVE -> "Activ";
            case COMPLETED -> "Completat";
            case FAILED -> "Esuat";
        };
    }

    private List<String> buildObjectiveProgressLines(ScenarioTemplate template, PlayerQuestProgress progress) {
        if (template == null || progress == null || template.getObjectives().isEmpty()) {
            return List.of();
        }

        List<String> lines = new ArrayList<>();
        List<FeaturePackLoader.QuestEntryDefinition> objectives = template.getObjectives();
        for (int index = 0; index < objectives.size(); index++) {
            FeaturePackLoader.QuestEntryDefinition objective = objectives.get(index);
            String objectiveKey = buildObjectiveKey(objective, index);
            int currentAmount = Math.max(0, progress.objectiveProgress().getOrDefault(objectiveKey, 0));
            int requiredAmount = Math.max(1, objective.getAmount());
            String label = formatObjectiveProgressLabel(objective);
            lines.add("&7- &f" + label + ": &e" + Math.min(currentAmount, requiredAmount) + "&7/&f" + requiredAmount);
        }

        return lines;
    }

    private String formatQuestPhase(String phaseId) {
        if (phaseId == null || phaseId.isBlank()) {
            return "";
        }

        String[] parts = phaseId.toLowerCase(Locale.ROOT).replace('-', '_').split("_+");
        List<String> words = new ArrayList<>();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            words.add(Character.toUpperCase(part.charAt(0)) + part.substring(1));
        }

        return words.isEmpty() ? phaseId : String.join(" ", words);
    }

    private String describeQuestProgress(PlayerQuestProgress progress) {
        if (progress == null) {
            return "necunoscut";
        }

        if (progress.questCode() != null && !progress.questCode().isBlank()) {
            return progress.questCode() + " (" + formatQuestStatus(progress.status()) + ")";
        }

        return progress.templateId() + " (" + formatQuestStatus(progress.status()) + ")";
    }

    private String buildQuestOfferMessage(ScenarioTemplate template) {
        List<String> objectives = template.getObjectives().stream()
            .map(this::formatQuestEntry)
            .toList();
        if (objectives.isEmpty()) {
            return template.getDescription().isBlank()
                ? "Am nevoie de ajutorul tau."
                : template.getDescription();
        }

        boolean deliveryQuest = template.getObjectives().stream().allMatch(this::usesInventoryProgress);
        if (deliveryQuest) {
            return "Adu-mi " + joinNaturally(objectives) + " si te rasplatesc cum se cuvine.";
        }

        if (!template.getDescription().isBlank()) {
            return template.getDescription();
        }

        return "Ai de facut urmatoarele: " + joinNaturally(objectives) + ".";
    }

    private QuestInventoryCheck inspectQuestInventory(PlayerInventory inventory,
                                                      List<FeaturePackLoader.QuestEntryDefinition> objectives) {
        List<String> missingItems = new ArrayList<>();

        for (FeaturePackLoader.QuestEntryDefinition objective : objectives) {
            Material material = resolveQuestMaterial(objective);
            if (material == null) {
                missingItems.add(formatQuestEntry(objective));
                continue;
            }

            int currentAmount = countMaterial(inventory, material);
            if (currentAmount < objective.getAmount()) {
                int missingAmount = objective.getAmount() - currentAmount;
                missingItems.add(formatQuestAmount(missingAmount, material));
            }
        }

        return new QuestInventoryCheck(missingItems.isEmpty(), missingItems);
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
            int requiredAmount = Math.max(1, objective.getAmount());
            int currentAmount = resolveObjectiveCurrentProgress(player, objective, progress, buildObjectiveKey(objective, index));
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

    private void consumeQuestObjectives(PlayerInventory inventory,
                                        List<FeaturePackLoader.QuestEntryDefinition> objectives) {
        for (FeaturePackLoader.QuestEntryDefinition objective : objectives) {
            if (!shouldConsumeObjectiveItem(objective)) {
                continue;
            }
            Material material = resolveQuestMaterial(objective);
            if (material == null) {
                continue;
            }
            removeMaterial(inventory, material, objective.getAmount());
        }
    }

    private int resolveObjectiveCurrentProgress(Player player,
                                                FeaturePackLoader.QuestEntryDefinition objective,
                                                PlayerQuestProgress progress,
                                                String objectiveKey) {
        if (objective == null || objectiveKey == null || objectiveKey.isBlank()) {
            return 0;
        }

        int requiredAmount = Math.max(1, objective.getAmount());
        if (player != null && usesInventoryProgress(objective)) {
            Material material = resolveQuestMaterial(objective);
            if (material != null) {
                return Math.min(requiredAmount, countMaterial(player.getInventory(), material));
            }
        }

        if (progress == null) {
            return 0;
        }

        return Math.min(requiredAmount, Math.max(0, progress.objectiveProgress().getOrDefault(objectiveKey, 0)));
    }

    private String formatObjectiveProgressLabel(FeaturePackLoader.QuestEntryDefinition objective) {
        if (objective == null) {
            return "obiectiv";
        }

        String objectiveType = normalizeObjectiveType(objective.getType());
        Material material = resolveQuestMaterial(objective);
        return switch (objectiveType) {
            case "collect_item", "deliver_to_npc" -> material != null
                ? humanizeItemId(material.name())
                : humanizeItemId(objective.getItemId());
            case "talk_to_npc" -> "vorbeste cu " + formatObjectiveTargetLabel(objective, "npc-ul");
            case "visit_region" -> "viziteaza " + formatObjectiveTargetLabel(objective, "regiunea");
            case "kill_mob" -> "ucide " + formatObjectiveTargetLabel(objective, "inamicul");
            default -> !objective.getDescription().isBlank() ? objective.getDescription() : humanizeItemId(objective.getItemId());
        };
    }

    private String formatMissingObjective(FeaturePackLoader.QuestEntryDefinition objective,
                                          int currentAmount,
                                          int requiredAmount) {
        if (objective == null) {
            return "obiectiv necunoscut";
        }

        int missingAmount = Math.max(0, requiredAmount - currentAmount);
        String objectiveType = normalizeObjectiveType(objective.getType());
        Material material = resolveQuestMaterial(objective);
        return switch (objectiveType) {
            case "collect_item", "deliver_to_npc" -> material != null
                ? formatQuestAmount(Math.max(1, missingAmount), material)
                : formatQuestEntry(objective);
            case "talk_to_npc" -> "vorbeste cu " + formatObjectiveTargetLabel(objective, "npc-ul tintit");
            case "visit_region" -> "viziteaza " + formatObjectiveTargetLabel(objective, "regiunea tintita");
            case "kill_mob" -> "ucide " + (missingAmount > 1
                ? missingAmount + "x " + formatObjectiveTargetLabel(objective, "inamicul tintit")
                : formatObjectiveTargetLabel(objective, "inamicul tintit"));
            default -> formatQuestEntry(objective);
        };
    }

    private String formatObjectiveTargetLabel(FeaturePackLoader.QuestEntryDefinition objective, String fallback) {
        if (objective == null || objective.getItemId() == null || objective.getItemId().isBlank()) {
            return fallback;
        }

        return humanizeItemId(stripObjectivePrefix(objective.getItemId()));
    }

    private List<String> grantQuestRewards(Player player, List<FeaturePackLoader.QuestEntryDefinition> rewards) {
        List<String> notes = new ArrayList<>();
        for (FeaturePackLoader.QuestEntryDefinition reward : rewards) {
            Material material = resolveQuestMaterial(reward);
            if (material == null) {
                notes.add("&cRecompensa invalida in configuratie: &f" + reward.getItemId());
                continue;
            }

            ItemStack rewardStack = new ItemStack(material, reward.getAmount());
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(rewardStack);
            if (!leftovers.isEmpty()) {
                leftovers.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
                notes.add("&eInventarul era plin. Recompensa a fost lasata pe jos langa tine.");
            }
        }

        return notes;
    }

    private Material resolveQuestMaterial(FeaturePackLoader.QuestEntryDefinition entry) {
        if (entry == null || entry.getItemId() == null || entry.getItemId().isBlank()) {
            return null;
        }

        return Material.matchMaterial(entry.getItemId());
    }

    private int countMaterial(PlayerInventory inventory, Material material) {
        if (inventory == null || material == null) {
            return 0;
        }

        int total = 0;
        for (ItemStack stack : inventory.getStorageContents()) {
            if (stack != null && stack.getType() == material) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    private void removeMaterial(PlayerInventory inventory, Material material, int amount) {
        if (inventory == null || material == null || amount <= 0) {
            return;
        }

        ItemStack[] contents = inventory.getStorageContents();
        int remaining = amount;
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType() != material) {
                continue;
            }

            if (stack.getAmount() <= remaining) {
                remaining -= stack.getAmount();
                contents[i] = null;
            } else {
                stack.setAmount(stack.getAmount() - remaining);
                remaining = 0;
            }
        }

        inventory.setStorageContents(contents);
    }

    private String formatQuestAmount(int amount, Material material) {
        String itemName = material == null ? "item" : humanizeItemId(material.name());
        return amount > 1 ? amount + "x " + itemName : itemName;
    }

    private String resolveQuestTitle(ScenarioTemplate template) {
        if (template == null) {
            return "";
        }

        return template.getQuestCode().isBlank()
            ? template.getDisplayName()
            : template.getQuestCode() + " - " + template.getDisplayName();
    }

    private String joinNaturally(List<String> parts) {
        if (parts == null || parts.isEmpty()) {
            return "";
        }

        if (parts.size() == 1) {
            return parts.get(0);
        }

        if (parts.size() == 2) {
            return parts.get(0) + " si " + parts.get(1);
        }

        String last = parts.get(parts.size() - 1);
        return String.join(", ", parts.subList(0, parts.size() - 1)) + " si " + last;
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

            if (normalize(occupation).equals(normalize(requiredProfession))) {
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
            case "GUARD" -> scoreBoolean(matchesOccupation(npc, "guard", "soldier", "garda"), 45);
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
            case "SELLER" -> scoreBoolean(matchesOccupation(npc, "merchant", "negustor"), 35);
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

            if (normalize(occupation).equals(normalize(reference))) {
                return true;
            }
        }

        return false;
    }

    private int scoreBoolean(boolean condition, int positiveScore) {
        return condition ? positiveScore : 0;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
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

    private String formatQuestEntry(FeaturePackLoader.QuestEntryDefinition entry) {
        if (entry == null) {
            return "";
        }

        if (entry.getDescription() != null && !entry.getDescription().isBlank()) {
            return entry.getDescription();
        }

        String objectiveType = normalizeObjectiveType(entry.getType());
        Material material = resolveQuestMaterial(entry);
        return switch (objectiveType) {
            case "collect_item", "deliver_to_npc" -> material != null
                ? formatQuestAmount(entry.getAmount(), material)
                : (entry.getAmount() > 1 ? entry.getAmount() + "x " + humanizeItemId(entry.getItemId()) : humanizeItemId(entry.getItemId()));
            case "talk_to_npc" -> "Vorbeste cu " + formatObjectiveTargetLabel(entry, "NPC-ul tintit");
            case "visit_region" -> "Viziteaza " + formatObjectiveTargetLabel(entry, "regiunea tintita");
            case "kill_mob" -> "Ucide " + (entry.getAmount() > 1
                ? entry.getAmount() + "x " + formatObjectiveTargetLabel(entry, "inamicul tintit")
                : formatObjectiveTargetLabel(entry, "inamicul tintit"));
            default -> {
                String itemName = humanizeItemId(entry.getItemId());
                yield entry.getAmount() > 1 ? entry.getAmount() + "x " + itemName : itemName;
            }
        };
    }

    private String humanizeItemId(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return "item";
        }

        return itemId.toLowerCase(Locale.ROOT).replace('_', ' ');
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
        private String questCode;
        private String questGiverProfession;
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
            this.questCode = "";
            this.questGiverProfession = "";
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
        public String getQuestCode() { return questCode; }
        public void setQuestCode(String questCode) { this.questCode = questCode == null ? "" : questCode; }
        public String getQuestGiverProfession() { return questGiverProfession; }
        public void setQuestGiverProfession(String questGiverProfession) {
            this.questGiverProfession = questGiverProfession == null ? "" : questGiverProfession;
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

    private record PlayerQuestProgress(
        String templateId,
        String questCode,
        QuestStatus status,
        long startedAt,
        long completedAt,
        long updatedAt,
        String currentPhase,
        Map<String, Integer> objectiveProgress,
        Map<String, String> questVariables
    ) {
        private PlayerQuestProgress {
            currentPhase = currentPhase == null ? "" : currentPhase;
            objectiveProgress = Collections.unmodifiableMap(new LinkedHashMap<>(
                objectiveProgress != null ? objectiveProgress : Map.of()
            ));
            questVariables = Collections.unmodifiableMap(new LinkedHashMap<>(
                questVariables != null ? questVariables : Map.of()
            ));
        }

        private boolean isCurrent() {
            return status == QuestStatus.OFFERED || status == QuestStatus.ACTIVE;
        }

        private boolean isOffered() {
            return status == QuestStatus.OFFERED;
        }

        private boolean isActive() {
            return status == QuestStatus.ACTIVE;
        }

        private boolean isCompleted() {
            return status == QuestStatus.COMPLETED;
        }
    }

    private record SimpleQuestProfile(
        String title,
        Material objectiveMaterial,
        int objectiveAmount,
        Material rewardMaterial,
        int rewardAmount,
        String objectivePrompt,
        String hint
    ) {
    }

    private enum QuestStatus {
        NOT_STARTED,
        OFFERED,
        ACTIVE,
        COMPLETED,
        FAILED;

        private boolean isArchived() {
            return this == COMPLETED || this == FAILED;
        }

        private String storageValue() {
            return name().toLowerCase(Locale.ROOT);
        }

        private static QuestStatus fromStorage(String value) {
            if (value == null || value.isBlank()) {
                return NOT_STARTED;
            }

            try {
                return QuestStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                return NOT_STARTED;
            }
        }
    }

    private record QuestInventoryCheck(
        boolean complete,
        List<String> missingItems
    ) {
    }

    private record QuestObjectiveCheck(
        boolean complete,
        List<String> missingObjectives
    ) {
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
