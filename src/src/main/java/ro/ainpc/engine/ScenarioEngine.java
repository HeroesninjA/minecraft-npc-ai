package ro.ainpc.engine;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import ro.ainpc.AINPCPlugin;
import ro.ainpc.npc.AINPC;
import ro.ainpc.npc.NPCEmotions;
import ro.ainpc.npc.NPCPersonality;

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

    private final AINPCPlugin plugin;
    private final Map<UUID, ActiveScenario> activeScenarios;
    private final Map<ScenarioType, ScenarioTemplate> scenarioTemplates;
    private final Map<UUID, PlayerQuestProgress> playerQuestProgress;

    public ScenarioEngine(AINPCPlugin plugin) {
        this.plugin = plugin;
        this.activeScenarios = new ConcurrentHashMap<>();
        this.scenarioTemplates = new EnumMap<>(ScenarioType.class);
        this.playerQuestProgress = new ConcurrentHashMap<>();

        loadScenarioTemplates();
    }

    public void reloadTemplates() {
        loadScenarioTemplates();
    }

    /**
     * Incarca template-urile implicite si apoi aplica override-urile din addon packs.
     */
    private void loadScenarioTemplates() {
        scenarioTemplates.clear();

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

        PlayerQuestProgress currentProgress = playerQuestProgress.get(player.getUniqueId());
        if (currentProgress != null) {
            plugin.debug("[QuestEngine] Progres curent pentru player=" + player.getName()
                + " templateId=" + currentProgress.templateId()
                + " completed=" + currentProgress.completed());
        } else {
            plugin.debug("[QuestEngine] Player=" + player.getName() + " nu are progres de quest inregistrat.");
        }
        if (currentProgress != null
            && !currentProgress.templateId().equals(template.getTemplateId())
            && !currentProgress.completed()) {
            plugin.debug("[QuestEngine] Player=" + player.getName()
                + " are deja alt quest activ: " + currentProgress.templateId());
            return QuestInteractionResult.handled(
                true,
                List.of("Ai deja o alta misiune in desfasurare. Termina intai ce ai inceput."),
                List.of("&cAi deja o alta misiune activa.")
            );
        }

        if (currentProgress != null
            && currentProgress.templateId().equals(template.getTemplateId())
            && currentProgress.completed()) {
            plugin.debug("[QuestEngine] Quest deja completat pentru player=" + player.getName()
                + " templateId=" + template.getTemplateId());
            return QuestInteractionResult.handled(
                true,
                List.of("Ti-am dat deja recompensa pentru " + resolveQuestTitle(template) + ". Foloseste sabia cu cap."),
                List.of("&7Quest deja completat: &f" + resolveQuestTitle(template))
            );
        }

        if (currentProgress == null || !currentProgress.templateId().equals(template.getTemplateId())) {
            playerQuestProgress.put(
                player.getUniqueId(),
                new PlayerQuestProgress(template.getTemplateId(), template.getQuestCode(), false, System.currentTimeMillis())
            );
            plugin.debug("[QuestEngine] Quest pornit pentru player=" + player.getName()
                + " templateId=" + template.getTemplateId());

            List<String> npcMessages = List.of(
                "Am o treaba pentru tine.",
                buildQuestOfferMessage(template)
            );
            return QuestInteractionResult.handled(true, npcMessages, buildQuestBriefingMessages(template));
        }

        QuestInventoryCheck inventoryCheck = inspectQuestInventory(player.getInventory(), template.getObjectives());
        if (!inventoryCheck.complete()) {
            plugin.debug("[QuestEngine] Quest incomplet pentru player=" + player.getName()
                + " lipsesc=" + String.join(", ", inventoryCheck.missingItems()));
            List<String> systemMessages = new ArrayList<>();
            systemMessages.add("&6[Quest] &f" + resolveQuestTitle(template));
            systemMessages.add("&eIti mai lipsesc:");
            for (String missingItem : inventoryCheck.missingItems()) {
                systemMessages.add("&7- &f" + missingItem);
            }

            return QuestInteractionResult.handled(
                true,
                List.of("Inca nu ai adus tot ce ti-am cerut. Intoarce-te cand ai toate materialele."),
                systemMessages
            );
        }

        consumeQuestObjectives(player.getInventory(), template.getObjectives());
        List<String> rewardNotes = grantQuestRewards(player, template.getRewards());
        player.updateInventory();

        playerQuestProgress.put(
            player.getUniqueId(),
            new PlayerQuestProgress(template.getTemplateId(), template.getQuestCode(), true, System.currentTimeMillis())
        );
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

        playerQuestProgress.put(
            player.getUniqueId(),
            new PlayerQuestProgress(template.getTemplateId(), template.getQuestCode(), false, System.currentTimeMillis())
        );
        plugin.debug("[QuestEngine] startQuestManually a setat questul pentru player=" + player.getName()
            + " templateId=" + template.getTemplateId());

        List<String> npcMessages = List.of(
            "Am o treaba pentru tine.",
            buildQuestOfferMessage(template)
        );
        return QuestInteractionResult.handled(true, npcMessages, buildQuestBriefingMessages(template));
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

        PlayerQuestProgress currentProgress = playerQuestProgress.get(player.getUniqueId());
        if (currentProgress == null || !currentProgress.templateId().equals(template.getTemplateId())) {
            plugin.debug("[QuestEngine] resetQuestProgress fara progres potrivit pentru player="
                + player.getName() + " templateId=" + template.getTemplateId());
            return false;
        }

        playerQuestProgress.remove(player.getUniqueId());
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

        playerQuestProgress.put(
            player.getUniqueId(),
            new PlayerQuestProgress(template.getTemplateId(), template.getQuestCode(), true, System.currentTimeMillis())
        );
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

    private ScenarioTemplate findQuestTemplateForNpc(AINPC npc) {
        if (shouldUseSimpleQuestForAllNpcs()) {
            return buildSimpleQuestTemplate(npc);
        }

        return scenarioTemplates.values().stream()
            .filter(template -> template.getType() == ScenarioType.QUEST)
            .filter(ScenarioTemplate::hasQuestBriefing)
            .filter(template -> matchesQuestGiver(npc, template))
            .findFirst()
            .orElse(null);
    }

    private boolean shouldUseSimpleQuestForAllNpcs() {
        return plugin.getConfig().getBoolean("quests.simple_for_all_npcs", true);
    }

    private ScenarioTemplate buildSimpleQuestTemplate(AINPC npc) {
        if (npc == null) {
            return null;
        }

        Material objectiveMaterial = resolveConfiguredQuestMaterial(
            "quests.simple.objective.item",
            Material.OAK_PLANKS
        );
        int objectiveAmount = Math.max(1, plugin.getConfig().getInt("quests.simple.objective.amount", 3));

        Material rewardMaterial = resolveConfiguredQuestMaterial(
            "quests.simple.reward.item",
            Material.EMERALD
        );
        int rewardAmount = Math.max(1, plugin.getConfig().getInt("quests.simple.reward.amount", 1));

        String title = plugin.getConfig().getString("quests.simple.title", "Ajutor rapid");
        String npcIdentifier = npc.getDatabaseId() > 0
            ? String.valueOf(npc.getDatabaseId())
            : npc.getUuid().toString();

        ScenarioTemplate template = new ScenarioTemplate(ScenarioType.QUEST);
        template.setTemplateId("simple_npc_quest:" + npcIdentifier);
        template.setDisplayName(title + " - " + npc.getName());
        template.setDescription("Adu-mi " + formatQuestAmount(objectiveAmount, objectiveMaterial)
            + " si iti dau " + formatQuestAmount(rewardAmount, rewardMaterial) + ".");
        template.setHint(npc.getName() + " pare sa aiba nevoie de cateva materiale.");
        template.setQuestGiverProfession(npc.getOccupation());
        template.setRequiresPlayer(true);
        template.setMinimumNpcCount(1);
        template.setObjectives(List.of(new FeaturePackLoader.QuestEntryDefinition(
            "collect_item",
            objectiveMaterial.name(),
            objectiveAmount,
            "Adu " + formatQuestAmount(objectiveAmount, objectiveMaterial) + "."
        )));
        template.setRewards(List.of(new FeaturePackLoader.QuestEntryDefinition(
            "item",
            rewardMaterial.name(),
            rewardAmount,
            "Primesti " + formatQuestAmount(rewardAmount, rewardMaterial) + "."
        )));
        return template;
    }

    private Material resolveConfiguredQuestMaterial(String path, Material fallback) {
        String configuredValue = plugin.getConfig().getString(path, fallback.name());
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

    private String buildQuestOfferMessage(ScenarioTemplate template) {
        List<String> objectives = template.getObjectives().stream()
            .map(objective -> {
                Material material = resolveQuestMaterial(objective);
                return material != null
                    ? formatQuestAmount(objective.getAmount(), material)
                    : formatQuestEntry(objective);
            })
            .toList();
        if (objectives.isEmpty()) {
            return template.getDescription().isBlank()
                ? "Am nevoie de ajutorul tau."
                : template.getDescription();
        }

        return "Adu-mi " + joinNaturally(objectives) + " si te rasplatesc cum se cuvine.";
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

    private void consumeQuestObjectives(PlayerInventory inventory,
                                        List<FeaturePackLoader.QuestEntryDefinition> objectives) {
        for (FeaturePackLoader.QuestEntryDefinition objective : objectives) {
            Material material = resolveQuestMaterial(objective);
            if (material == null) {
                continue;
            }
            removeMaterial(inventory, material, objective.getAmount());
        }
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

        String itemName = humanizeItemId(entry.getItemId());
        return entry.getAmount() > 1 ? entry.getAmount() + "x " + itemName : itemName;
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
        boolean completed,
        long updatedAt
    ) {
    }

    private record QuestInventoryCheck(
        boolean complete,
        List<String> missingItems
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
