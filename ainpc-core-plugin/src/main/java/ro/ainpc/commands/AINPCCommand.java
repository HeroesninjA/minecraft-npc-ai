package ro.ainpc.commands;

import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import ro.ainpc.AINPCPlugin;
import ro.ainpc.api.WorldAdminApi;
import ro.ainpc.debug.DebugDumpService;
import ro.ainpc.engine.ScenarioEngine;
import ro.ainpc.npc.AINPC;
import ro.ainpc.routine.RoutineAssignment;
import ro.ainpc.routine.RoutineTickSummary;
import ro.ainpc.spawn.HouseAllocation;
import ro.ainpc.spawn.HouseAllocationPlanner;
import ro.ainpc.spawn.HouseholdSpawnResult;
import ro.ainpc.spawn.NpcSpawnPlan;
import ro.ainpc.spawn.NpcSpawnResult;
import ro.ainpc.spawn.SettlementSpawnResult;
import ro.ainpc.story.PlaceStoryState;
import ro.ainpc.story.RegionStoryState;
import ro.ainpc.story.StoryContextSnapshot;
import ro.ainpc.story.StoryEvent;
import ro.ainpc.world.PlaceType;
import ro.ainpc.world.RegionType;
import ro.ainpc.world.WorldAdminService;
import ro.ainpc.world.WorldNodeInfo;
import ro.ainpc.world.WorldNodeType;
import ro.ainpc.world.WorldPlaceInfo;
import ro.ainpc.world.WorldRegionInfo;
import ro.ainpc.world.scan.SemanticVillageImportResult;
import ro.ainpc.world.scan.SemanticVillageMapper;
import ro.ainpc.world.scan.VanillaVillageFeatureType;
import ro.ainpc.world.scan.VanillaVillageScanResult;
import ro.ainpc.world.scan.VanillaVillageScanner;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Comanda principala pentru gestionarea NPC-urilor AI
 */
public class AINPCCommand implements CommandExecutor {

    private static final int AUDIT_PREVIEW_LIMIT = 12;
    private static final int STORY_EVENT_DEFAULT_LIMIT = 10;
    private static final int STORY_EVENT_MAX_LIMIT = 50;
    private static final DateTimeFormatter STORY_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AINPCPlugin plugin;

    public AINPCCommand(AINPCPlugin plugin) {
        this.plugin = plugin;
    }

    private record StoryContextTarget(Player player, String npcSelector) {
    }

    private record StoryEventTarget(String regionId, String placeId, String label, boolean mapped) {
    }

    private record QuestDecisionTarget(Player player, AINPC npc) {
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if ("npcquest".equalsIgnoreCase(command.getName()) || "quest".equalsIgnoreCase(command.getName())) {
            String[] routedArgs = new String[args.length + 1];
            routedArgs[0] = "quest";
            System.arraycopy(args, 0, routedArgs, 1, args.length);
            return handleQuest(sender, routedArgs);
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        return switch (subCommand) {
            case "create" -> handleCreate(sender, args);
            case "delete", "remove" -> handleDelete(sender, args);
            case "info" -> handleInfo(sender, args);
            case "quest" -> handleQuest(sender, args);
            case "world" -> handleWorld(sender, args);
            case "story" -> handleStory(sender, args);
            case "audit" -> handleAudit(sender, args);
            case "debugdump" -> handleDebugDump(sender, args);
            case "list" -> handleList(sender, args);
            case "family" -> handleFamily(sender, args);
            case "routine" -> handleRoutine(sender, args);
            case "mood", "emotion" -> handleMood(sender, args);
            case "tp", "teleport" -> handleTeleport(sender, args);
            case "reload" -> handleReload(sender);
            case "test" -> handleTest(sender);
            default -> {
                sendHelp(sender);
                yield true;
            }
        };
    }

    /**
     * /ainpc create <nume> [ocupatie] [varsta] [gen] [arhetip]
     */
    private boolean handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ainpc.admin")) {
            plugin.getMessageUtils().sendMessage(sender, "no_permission");
            return true;
        }

        if (!(sender instanceof Player player)) {
            plugin.getMessageUtils().send(sender, "&cAceasta comanda poate fi folosita doar de jucatori!");
            return true;
        }

        if (args.length < 2) {
            plugin.getMessageUtils().send(sender, "&cUtilizare: /ainpc create <nume> [ocupatie] [varsta] [gen] [arhetip]");
            return true;
        }

        String name = args[1];
        String occupation = args.length > 2 ? args[2] : null;
        int age = args.length > 3 ? parseInt(args[3], 30) : 30;
        String gender = args.length > 4 ? args[4].toLowerCase() : "male";
        String archetype = args.length > 5 ? args[5] : null;

        // Valideaza genul
        if (!gender.equals("male") && !gender.equals("female")) {
            gender = "male";
        }

        Location location = player.getLocation();
        AINPC npc = plugin.getNpcManager().createNPC(name, location, occupation, null, age, gender, archetype);

        if (npc != null) {
            plugin.getMessageUtils().sendMessage(sender, "npc_created", Map.of("name", name));
            plugin.getMessageUtils().send(sender, "&7ID: &f" + npc.getDatabaseId());
            plugin.getMessageUtils().send(sender, "&7Personalitate: &f" + npc.getPersonality().getDominantTraits());
        } else {
            plugin.getMessageUtils().send(sender, "&cEroare la crearea NPC-ului!");
        }

        return true;
    }

    /**
     * /ainpc quest <numeNpc> [jucator]
     * /ainpc quest nearest [jucator]
     * /ainpc quest track [jucator]
     * /ainpc quest accept <numeNpc>|nearest [jucator]
     * /ainpc quest decline <numeNpc>|nearest [jucator]
     * /ainpc quest abandon <numeNpc>|nearest [jucator]
     * /ainpc quest status <numeNpc>|nearest [jucator]
     * /ainpc quest reset <numeNpc> [jucator]
     * /ainpc quest complete <numeNpc> [jucator]
     */
    private boolean handleQuest(CommandSender sender, String[] args) {
        questDebug("Comanda quest primita de la " + sender.getName() + ": " + String.join(" ", args));
        if (!sender.hasPermission("ainpc.admin") && !sender.hasPermission("ainpc.quest")) {
            questDebug("Respins: " + sender.getName() + " nu are permisiune pentru /ainpc quest.");
            plugin.getMessageUtils().sendMessage(sender, "no_permission");
            return true;
        }

        if (args.length < 2) {
            questDebug("Respins: lipseste modul sau numele NPC-ului pentru comanda quest.");
            sendQuestUsage(sender);
            return true;
        }

        String mode = args[1].toLowerCase();
        questDebug("Parsare quest mode='" + mode + "' sender=" + sender.getName());
        return switch (mode) {
            case "anchors" -> handleQuestAnchors(sender, args);
            case "log" -> handleQuestLog(sender, args);
            case "track", "current" -> handleQuestTrack(sender, args);
            case "nearest" -> handleNearestQuest(sender, args);
            case "accept", "yes", "y", "da", "ok", "confirm" -> handleAcceptQuest(sender, args);
            case "decline", "deny", "reject", "no", "n", "nu", "refuz" -> handleDeclineQuest(sender, args);
            case "abandon" -> handleAbandonQuest(sender, args);
            case "status" -> handleStatusQuest(sender, args);
            case "reset" -> handleResetQuest(sender, args);
            case "complete" -> handleCompleteQuest(sender, args);
            default -> handleTriggerQuest(sender, args[1],
                resolveQuestTargetPlayer(sender, args, 2, "&cUtilizare: /ainpc quest <numeNpc> [jucator]"));
        };
    }

    private boolean handleQuestLog(CommandSender sender, String[] args) {
        Player targetPlayer = resolveQuestTargetPlayer(
            sender,
            args,
            2,
            "&cUtilizare: /ainpc quest log [jucator]"
        );
        if (targetPlayer == null) {
            questDebug("Quest log oprit: nu am putut rezolva jucatorul tinta.");
            return true;
        }

        ScenarioEngine.QuestInteractionResult questInteraction = plugin.getScenarioEngine().getQuestLog(targetPlayer);
        if (!questInteraction.isHandled()) {
            plugin.getMessageUtils().send(sender, "&cNu am putut citi quest log-ul.");
            return true;
        }

        CommandSender recipient = sender.equals(targetPlayer) ? targetPlayer : sender;
        for (String systemMessage : questInteraction.getSystemMessages()) {
            plugin.getMessageUtils().send(recipient, systemMessage);
        }

        if (!sender.equals(targetPlayer)) {
            plugin.getMessageUtils().send(sender,
                "&aAi cerut quest log-ul pentru &f" + targetPlayer.getName() + "&a.");
        }
        return true;
    }

    private boolean handleQuestTrack(CommandSender sender, String[] args) {
        String trackAction = args.length > 2 ? args[2].toLowerCase() : "";
        boolean persistentAction = "start".equals(trackAction) || "stop".equals(trackAction);
        Player targetPlayer = resolveQuestTargetPlayer(
            sender,
            args,
            persistentAction ? 3 : 2,
            "&cUtilizare: /ainpc quest track [start|stop] [jucator]"
        );
        if (targetPlayer == null) {
            questDebug("Quest track oprit: nu am putut rezolva jucatorul tinta.");
            return true;
        }

        if ("stop".equals(trackAction)) {
            boolean stopped = plugin.getScenarioEngine().stopQuestTracking(targetPlayer);
            plugin.getMessageUtils().send(sender, stopped
                ? "&aQuest tracking oprit pentru &f" + targetPlayer.getName() + "&a."
                : "&7Quest tracking nu era pornit pentru &f" + targetPlayer.getName() + "&7.");
            if (!sender.equals(targetPlayer)) {
                plugin.getMessageUtils().sendActionBar(targetPlayer, "&cQuest tracking oprit");
            }
            return true;
        }

        ScenarioEngine.QuestInteractionResult questInteraction = plugin.getScenarioEngine().getQuestTrack(targetPlayer);
        if (!questInteraction.isHandled()) {
            plugin.getMessageUtils().send(sender, "&cNu am putut urmari quest-ul curent.");
            return true;
        }

        CommandSender recipient = sender.equals(targetPlayer) ? targetPlayer : sender;
        for (String systemMessage : questInteraction.getSystemMessages()) {
            plugin.getMessageUtils().send(recipient, systemMessage);
        }

        ScenarioEngine.QuestTrackingMarker trackingMarker = "start".equals(trackAction)
            ? plugin.getScenarioEngine().startQuestTracking(targetPlayer)
            : plugin.getScenarioEngine().getQuestTrackingMarker(targetPlayer);
        applyQuestTrackingMarker(sender, targetPlayer, trackingMarker);

        if ("start".equals(trackAction)) {
            if (trackingMarker != null && trackingMarker.hasLocation()) {
                plugin.getMessageUtils().send(sender,
                    "&aQuest tracking persistent pornit pentru &f" + targetPlayer.getName() + "&a.");
            } else {
                plugin.getMessageUtils().send(sender,
                    "&cNu am pornit quest tracking persistent: nu exista o tinta cu locatie pentru questul curent.");
            }
            return true;
        }

        if (!sender.equals(targetPlayer)) {
            plugin.getMessageUtils().send(sender,
                "&aAi cerut quest tracking pentru &f" + targetPlayer.getName() + "&a.");
        }
        return true;
    }

    private void applyQuestTrackingMarker(CommandSender sender,
                                          Player targetPlayer,
                                          ScenarioEngine.QuestTrackingMarker trackingMarker) {
        if (targetPlayer == null || trackingMarker == null || !trackingMarker.hasLocation()) {
            return;
        }

        Location targetLocation = trackingMarker.location();
        boolean compassSet = plugin.getScenarioEngine().applyQuestTrackingMarker(targetPlayer, trackingMarker);
        if (compassSet) {
            plugin.getMessageUtils().send(targetPlayer,
                "&aBusola indica acum tinta questului: &f" + trackingMarker.targetLabel());
            if (!sender.equals(targetPlayer)) {
                plugin.getMessageUtils().send(sender,
                    "&aMarkerul vizual a fost trimis catre &f" + targetPlayer.getName()
                        + " &apentru &f" + trackingMarker.targetLabel() + "&a.");
            }
            return;
        }

        plugin.getMessageUtils().send(targetPlayer,
            "&eTinta questului este in alta lume: &f" + targetLocation.getWorld().getName()
                + " &7- &f" + trackingMarker.targetLabel());
        if (!sender.equals(targetPlayer)) {
            plugin.getMessageUtils().send(sender,
                "&eTinta questului pentru &f" + targetPlayer.getName()
                    + " &eeste in alta lume: &f" + targetLocation.getWorld().getName());
        }
    }

    private boolean handleNearestQuest(CommandSender sender, String[] args) {
        Player targetPlayer = resolveQuestTargetPlayer(
            sender,
            args,
            2,
            "&cUtilizare: /ainpc quest nearest [jucator]"
        );
        if (targetPlayer == null) {
            questDebug("Quest nearest oprit: nu am putut rezolva jucatorul tinta.");
            return true;
        }

        questDebug("Quest nearest pentru player=" + targetPlayer.getName()
            + " locatie=" + formatLocation(targetPlayer.getLocation()));
        AINPC nearestNpc = findNearestQuestNpc(targetPlayer);
        if (nearestNpc == null) {
            questDebug("Quest nearest: nu exista NPC activ in raza 16 pentru " + targetPlayer.getName());
            plugin.getMessageUtils().send(sender, "&cNu exista NPC-uri active in apropierea jucatorului.");
            return true;
        }

        questDebug("Quest nearest a ales NPC-ul " + nearestNpc.getName()
            + " (id=" + nearestNpc.getDatabaseId() + ")");
        return handleTriggerQuest(sender, nearestNpc.getName(), targetPlayer);
    }

    private boolean handleAcceptQuest(CommandSender sender, String[] args) {
        QuestDecisionTarget target = resolveQuestDecisionTarget(
            sender,
            args,
            "accept",
            "&cUtilizare: /ainpc quest accept [numeNpc|nearest] [jucator]"
        );
        if (target == null) {
            return true;
        }

        ScenarioEngine.QuestInteractionResult questInteraction =
            plugin.getScenarioEngine().acceptQuest(target.player(), target.npc());
        if (!questInteraction.isHandled()) {
            plugin.getMessageUtils().send(sender, "&cNPC-ul &e" + target.npc().getName() + " &cnu are un quest disponibil.");
            return true;
        }

        deliverQuestInteraction(
            sender,
            target.player(),
            target.npc(),
            questInteraction,
            "&aJucatorul &f" + target.player().getName() + " &aa acceptat quest-ul lui &e" + target.npc().getName() + "&a."
        );
        return true;
    }

    private boolean handleDeclineQuest(CommandSender sender, String[] args) {
        QuestDecisionTarget target = resolveQuestDecisionTarget(
            sender,
            args,
            "decline",
            "&cUtilizare: /ainpc quest decline [numeNpc|nearest] [jucator]"
        );
        if (target == null) {
            return true;
        }

        ScenarioEngine.QuestInteractionResult questInteraction =
            plugin.getScenarioEngine().declineQuest(target.player(), target.npc());
        if (!questInteraction.isHandled()) {
            plugin.getMessageUtils().send(sender, "&cNPC-ul &e" + target.npc().getName() + " &cnu are un quest disponibil.");
            return true;
        }

        deliverQuestInteraction(
            sender,
            target.player(),
            target.npc(),
            questInteraction,
            "&eJucatorul &f" + target.player().getName() + " &ea refuzat quest-ul lui &6" + target.npc().getName() + "&e."
        );
        return true;
    }

    private QuestDecisionTarget resolveQuestDecisionTarget(CommandSender sender,
                                                           String[] args,
                                                           String action,
                                                           String usage) {
        String npcSelector = args.length > 2 ? args[2] : "";
        int playerArgIndex = args.length > 2 ? 3 : -1;
        if (args.length == 3 && shouldTreatQuestDecisionArgumentAsPlayer(args[2])) {
            npcSelector = "";
            playerArgIndex = 2;
        }

        Player targetPlayer = resolveQuestTargetPlayer(sender, args, playerArgIndex, usage);
        if (targetPlayer == null) {
            questDebug("Quest " + action + " oprit: nu am putut rezolva jucatorul tinta.");
            return null;
        }

        AINPC npc = resolveFlexibleQuestDecisionNpc(sender, npcSelector, targetPlayer, action);
        if (npc == null) {
            return null;
        }

        npc = refreshQuestNpc(npc);
        if (!ensureQuestNpcCommandRange(sender, targetPlayer, npc)) {
            return null;
        }

        return new QuestDecisionTarget(targetPlayer, npc);
    }

    private boolean shouldTreatQuestDecisionArgumentAsPlayer(String argument) {
        if (argument == null || argument.isBlank() || "nearest".equalsIgnoreCase(argument)) {
            return false;
        }
        if (plugin.getNpcManager().getNPCByName(argument) != null) {
            return false;
        }

        return findOnlinePlayer(argument) != null;
    }

    private AINPC resolveFlexibleQuestDecisionNpc(CommandSender sender,
                                                  String npcSelector,
                                                  Player targetPlayer,
                                                  String action) {
        if (npcSelector != null && !npcSelector.isBlank()) {
            return resolveQuestNpcSelector(sender, npcSelector, targetPlayer, action);
        }

        AINPC npc = plugin.getScenarioEngine().resolveActiveQuestNpc(targetPlayer);
        if (npc != null) {
            questDebug("Quest " + action + " a folosit NPC-ul questului curent: " + npc.getName());
            return npc;
        }

        npc = findNearestQuestNpc(targetPlayer);
        if (npc != null) {
            questDebug("Quest " + action + " fara selector a ales cel mai apropiat NPC: " + npc.getName());
            return npc;
        }

        plugin.getMessageUtils().send(sender,
            "&cNu pot determina NPC-ul questului. Foloseste &e/ainpc quest " + action
                + " <numeNpc>|nearest&c.");
        return null;
    }

    private boolean handleAbandonQuest(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.getMessageUtils().send(sender, "&cUtilizare: /ainpc quest abandon <numeNpc>|nearest [jucator]");
            return true;
        }

        String npcSelector = args[2];
        Player targetPlayer = resolveQuestTargetPlayer(
            sender,
            args,
            3,
            "&cUtilizare: /ainpc quest abandon <numeNpc>|nearest [jucator]"
        );
        if (targetPlayer == null) {
            questDebug("Quest abandon oprit: nu am putut rezolva jucatorul tinta.");
            return true;
        }

        AINPC npc = resolveQuestNpcSelector(sender, npcSelector, targetPlayer, "abandon");
        if (npc == null) {
            return true;
        }

        npc = refreshQuestNpc(npc);
        if (!ensureQuestNpcCommandRange(sender, targetPlayer, npc)) {
            return true;
        }

        ScenarioEngine.QuestInteractionResult questInteraction = plugin.getScenarioEngine().abandonQuest(targetPlayer, npc);
        if (!questInteraction.isHandled()) {
            plugin.getMessageUtils().send(sender, "&cNPC-ul &e" + npc.getName() + " &cnu are un quest disponibil.");
            return true;
        }

        deliverQuestInteraction(
            sender,
            targetPlayer,
            npc,
            questInteraction,
            "&eJucatorul &f" + targetPlayer.getName() + " &ea abandonat quest-ul lui &6" + npc.getName() + "&e."
        );
        return true;
    }

    private boolean handleStatusQuest(CommandSender sender, String[] args) {
        if (args.length < 3) {
            return handleQuestLog(sender, args);
        }

        String npcSelector = args[2];
        Player targetPlayer = resolveQuestTargetPlayer(
            sender,
            args,
            3,
            "&cUtilizare: /ainpc quest status <numeNpc>|nearest [jucator]"
        );
        if (targetPlayer == null) {
            questDebug("Quest status oprit: nu am putut rezolva jucatorul tinta.");
            return true;
        }

        AINPC npc = resolveQuestNpcSelector(sender, npcSelector, targetPlayer, "status");
        if (npc == null) {
            return true;
        }

        npc = refreshQuestNpc(npc);
        if (!ensureQuestNpcCommandRange(sender, targetPlayer, npc)) {
            return true;
        }

        ScenarioEngine.QuestInteractionResult questInteraction = plugin.getScenarioEngine().getQuestStatus(targetPlayer, npc);
        if (!questInteraction.isHandled()) {
            plugin.getMessageUtils().send(sender, "&cNPC-ul &e" + npc.getName() + " &cnu are un quest disponibil.");
            return true;
        }

        deliverQuestInteraction(
            sender,
            targetPlayer,
            npc,
            questInteraction,
            "&aAi cerut statusul quest-ului lui &e" + npc.getName() + " &apentru &f" + targetPlayer.getName() + "&a."
        );
        return true;
    }

    private boolean handleResetQuest(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ainpc.admin")) {
            plugin.getMessageUtils().sendMessage(sender, "no_permission");
            return true;
        }

        if (args.length < 3) {
            plugin.getMessageUtils().send(sender, "&cUtilizare: /ainpc quest reset <numeNpc> [jucator]");
            return true;
        }

        Player targetPlayer = resolveQuestTargetPlayer(
            sender,
            args,
            3,
            "&cUtilizare: /ainpc quest reset <numeNpc> [jucator]"
        );
        if (targetPlayer == null) {
            questDebug("Quest reset oprit: nu am putut rezolva jucatorul tinta.");
            return true;
        }

        AINPC npc = plugin.getNpcManager().getNPCByName(args[2]);
        if (npc == null) {
            questDebug("Quest reset: NPC inexistent pentru numele '" + args[2] + "'.");
            plugin.getMessageUtils().sendMessage(sender, "npc_not_found");
            return true;
        }

        npc = refreshQuestNpc(npc);
        questDebug("Quest reset pentru npc=" + npc.getName() + " player=" + targetPlayer.getName());
        boolean reset = plugin.getScenarioEngine().resetQuestProgress(targetPlayer, npc);
        if (!reset) {
            questDebug("Quest reset: nu exista progres activ pentru npc=" + npc.getName()
                + " player=" + targetPlayer.getName());
            plugin.getMessageUtils().send(sender,
                "&cNu exista progres pentru quest-ul lui &e" + npc.getName() + " &cla jucatorul &f"
                    + targetPlayer.getName() + "&c.");
            return true;
        }

        String questTitle = plugin.getScenarioEngine().getQuestTitle(npc);
        plugin.getMessageUtils().send(targetPlayer,
            "&eQuest resetat manual: &f" + (questTitle.isBlank() ? npc.getName() : questTitle));
        if (!sender.equals(targetPlayer)) {
            plugin.getMessageUtils().send(sender,
                "&aAi resetat quest-ul lui &e" + npc.getName() + " &apentru &f" + targetPlayer.getName() + "&a.");
        }
        return true;
    }

    private boolean handleCompleteQuest(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ainpc.admin")) {
            plugin.getMessageUtils().sendMessage(sender, "no_permission");
            return true;
        }

        if (args.length < 3) {
            plugin.getMessageUtils().send(sender, "&cUtilizare: /ainpc quest complete <numeNpc> [jucator]");
            return true;
        }

        Player targetPlayer = resolveQuestTargetPlayer(
            sender,
            args,
            3,
            "&cUtilizare: /ainpc quest complete <numeNpc> [jucator]"
        );
        if (targetPlayer == null) {
            questDebug("Quest complete oprit: nu am putut rezolva jucatorul tinta.");
            return true;
        }

        AINPC npc = plugin.getNpcManager().getNPCByName(args[2]);
        if (npc == null) {
            questDebug("Quest complete: NPC inexistent pentru numele '" + args[2] + "'.");
            plugin.getMessageUtils().sendMessage(sender, "npc_not_found");
            return true;
        }

        npc = refreshQuestNpc(npc);
        questDebug("Quest complete pentru npc=" + npc.getName() + " player=" + targetPlayer.getName());
        ScenarioEngine.QuestInteractionResult questInteraction =
            plugin.getScenarioEngine().forceCompleteQuest(targetPlayer, npc);
        if (!questInteraction.isHandled()) {
            questDebug("Quest complete: ScenarioEngine a returnat handled=false pentru npc="
                + npc.getName() + " player=" + targetPlayer.getName());
            plugin.getMessageUtils().send(sender, "&cNPC-ul &e" + npc.getName() + " &cnu are un quest disponibil.");
            return true;
        }

        deliverQuestInteraction(
            sender,
            targetPlayer,
            npc,
            questInteraction,
            "&aAi marcat manual quest-ul lui &e" + npc.getName() + " &aca finalizat pentru &f"
                + targetPlayer.getName() + "&a."
        );
        return true;
    }

    private boolean handleQuestAnchors(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ainpc.admin")) {
            plugin.getMessageUtils().sendMessage(sender, "no_permission");
            return true;
        }
        if (plugin.getDatabaseManager() == null) {
            plugin.getMessageUtils().send(sender, "&cDatabaseManager nu este initializat.");
            return true;
        }

        String playerUuid = "";
        String templateId = "";
        if (args.length > 2 && !"all".equalsIgnoreCase(args[2])) {
            playerUuid = resolveQuestAnchorPlayerUuid(sender, args[2]);
            if (playerUuid == null) {
                return true;
            }
        }
        if (args.length > 3) {
            templateId = args[3];
        }
        if (args.length > 4) {
            plugin.getMessageUtils().send(sender, "&cUtilizare: /ainpc quest anchors [jucator|uuid|all] [templateId]");
            return true;
        }

        try {
            List<QuestAnchorBindingRow> rows = queryQuestAnchorBindings(playerUuid, templateId, 20);
            plugin.getMessageUtils().send(sender, "&6=== Quest Anchor Bindings ===");
            plugin.getMessageUtils().send(sender, "&eFiltru player: &f" + (playerUuid.isBlank() ? "all" : playerUuid));
            plugin.getMessageUtils().send(sender, "&eFiltru template: &f" + (templateId.isBlank() ? "all" : templateId));
            if (rows.isEmpty()) {
                plugin.getMessageUtils().send(sender, "&7Nu exista binding-uri pentru filtrul curent.");
                return true;
            }

            plugin.getMessageUtils().send(sender, "&eAfisate: &f" + rows.size() + " &7(max 20, cele mai recente)");
            for (QuestAnchorBindingRow row : rows) {
                plugin.getMessageUtils().send(sender, "&7- &f" + formatQuestAnchorBinding(row));
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("Nu am putut lista quest_anchor_bindings: " + exception.getMessage());
            plugin.getMessageUtils().send(sender, "&cNu am putut lista quest anchor bindings: " + exception.getMessage());
        }
        return true;
    }

    private String resolveQuestAnchorPlayerUuid(CommandSender sender, String selector) {
        if (selector == null || selector.isBlank()) {
            return "";
        }

        try {
            return UUID.fromString(selector).toString();
        } catch (IllegalArgumentException ignored) {
            // Nu este UUID; incercam jucator online.
        }

        Player player = plugin.getServer().getPlayerExact(selector);
        if (player == null) {
            player = plugin.getServer().getPlayer(selector);
        }
        if (player == null) {
            plugin.getMessageUtils().send(sender,
                "&cJucatorul trebuie sa fie online sau trebuie sa folosesti UUID-ul lui.");
            plugin.getMessageUtils().send(sender, "&cUtilizare: /ainpc quest anchors [jucator|uuid|all] [templateId]");
            return null;
        }
        return player.getUniqueId().toString();
    }

    private List<QuestAnchorBindingRow> queryQuestAnchorBindings(String playerUuid,
                                                                 String templateId,
                                                                 int limit) throws SQLException {
        StringBuilder sql = new StringBuilder("""
            SELECT b.player_uuid, b.template_id, b.objective_key, b.quest_code,
                   b.objective_type, b.reference, b.anchor_type, b.anchor_id,
                   b.anchor_label, b.created_at, b.updated_at, p.status
            FROM quest_anchor_bindings b
            LEFT JOIN player_quests p
              ON p.player_uuid = b.player_uuid AND p.template_id = b.template_id
            WHERE 1 = 1
        """);
        List<String> parameters = new ArrayList<>();
        if (playerUuid != null && !playerUuid.isBlank()) {
            sql.append(" AND b.player_uuid = ?");
            parameters.add(playerUuid);
        }
        if (templateId != null && !templateId.isBlank()) {
            sql.append(" AND b.template_id = ?");
            parameters.add(templateId);
        }
        sql.append(" ORDER BY b.updated_at DESC LIMIT ?");

        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement(sql.toString())) {
            int index = 1;
            for (String parameter : parameters) {
                stmt.setString(index++, parameter);
            }
            stmt.setInt(index, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                List<QuestAnchorBindingRow> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(readQuestAnchorBindingRow(rs));
                }
                return rows;
            }
        }
    }

    private QuestAnchorBindingRow readQuestAnchorBindingRow(ResultSet rs) throws SQLException {
        return new QuestAnchorBindingRow(
            rs.getString("player_uuid"),
            rs.getString("template_id"),
            rs.getString("objective_key"),
            rs.getString("quest_code"),
            rs.getString("objective_type"),
            rs.getString("reference"),
            rs.getString("anchor_type"),
            rs.getString("anchor_id"),
            rs.getString("anchor_label"),
            rs.getLong("created_at"),
            rs.getLong("updated_at"),
            rs.getString("status")
        );
    }

    private String formatQuestAnchorBinding(QuestAnchorBindingRow row) {
        return row.playerUuid() + " | " + row.templateId()
            + " | " + row.objectiveKey()
            + " | " + row.objectiveType()
            + " -> " + row.anchorType() + ":" + row.anchorId()
            + " (" + formatOptional(row.anchorLabel()) + ")"
            + " | status=" + formatOptional(row.status());
    }

    private AINPC resolveQuestNpcSelector(CommandSender sender, String npcSelector, Player targetPlayer, String action) {
        if (npcSelector == null || npcSelector.isBlank()) {
            plugin.getMessageUtils().send(sender, "&cSpecifica NPC-ul pentru quest.");
            return null;
        }

        if ("nearest".equalsIgnoreCase(npcSelector)) {
            AINPC nearestNpc = findNearestQuestNpc(targetPlayer);
            if (nearestNpc == null) {
                questDebug("Quest " + action + ": nu exista NPC activ in raza 16 pentru " + targetPlayer.getName());
                plugin.getMessageUtils().send(sender, "&cNu exista NPC-uri active in apropierea jucatorului.");
                return null;
            }
            return nearestNpc;
        }

        AINPC npc = plugin.getNpcManager().getNPCByName(npcSelector);
        if (npc == null) {
            questDebug("Quest " + action + ": NPC inexistent pentru numele '" + npcSelector + "'.");
            plugin.getMessageUtils().sendMessage(sender, "npc_not_found");
        }
        return npc;
    }

    private AINPC findNearestQuestNpc(Player targetPlayer) {
        if (targetPlayer == null) {
            return null;
        }

        return plugin.getNpcManager().getActiveNPCsNear(targetPlayer.getLocation(), 16).stream()
            .sorted(Comparator.comparingDouble(npc -> npc.getLocation().distanceSquared(targetPlayer.getLocation())))
            .findFirst()
            .orElse(null);
    }

    private boolean handleTriggerQuest(CommandSender sender, String npcName, Player targetPlayer) {
        if (targetPlayer == null) {
            questDebug("Quest trigger oprit: player tinta este null pentru npcName='" + npcName + "'.");
            return true;
        }

        questDebug("Quest trigger cerut pentru npcName='" + npcName + "' player=" + targetPlayer.getName());
        AINPC npc = plugin.getNpcManager().getNPCByName(npcName);
        if (npc == null) {
            questDebug("Quest trigger: NPC inexistent pentru numele '" + npcName + "'.");
            plugin.getMessageUtils().sendMessage(sender, "npc_not_found");
            return true;
        }

        npc = refreshQuestNpc(npc);
        if (!ensureQuestNpcCommandRange(sender, targetPlayer, npc)) {
            return true;
        }

        questDebug("Quest trigger foloseste npc=" + npc.getName() + " id=" + npc.getDatabaseId()
            + " ocupatie=" + npc.getOccupation());

        ScenarioEngine.QuestInteractionResult questInteraction =
            plugin.getScenarioEngine().startQuestManually(targetPlayer, npc);
        if (!questInteraction.isHandled()) {
            questDebug("Quest trigger: handled=false pentru npc=" + npc.getName()
                + " player=" + targetPlayer.getName());
            plugin.getMessageUtils().send(sender, "&cNPC-ul &e" + npc.getName() + " &cnu are un quest disponibil.");
            return true;
        }

        questDebug("Quest trigger reusit pentru npc=" + npc.getName()
            + " player=" + targetPlayer.getName()
            + " openConversation=" + questInteraction.shouldOpenConversation()
            + " npcMessages=" + questInteraction.getNpcMessages().size()
            + " systemMessages=" + questInteraction.getSystemMessages().size());

        deliverQuestInteraction(
            sender,
            targetPlayer,
            npc,
            questInteraction,
            "&aQuest-ul lui &e" + npc.getName() + " &aa fost declansat pentru &f" + targetPlayer.getName() + "&a."
        );
        return true;
    }

    private Player resolveQuestTargetPlayer(CommandSender sender, String[] args, int playerArgIndex, String usage) {
        if (playerArgIndex >= 0 && args.length > playerArgIndex) {
            questDebug("Rezolvare player tinta din argumentul " + playerArgIndex + ": '" + args[playerArgIndex] + "'");
            Player targetPlayer = plugin.getServer().getPlayerExact(args[playerArgIndex]);
            if (targetPlayer == null) {
                questDebug("getPlayerExact a esuat pentru '" + args[playerArgIndex] + "', incerc getPlayer.");
                targetPlayer = plugin.getServer().getPlayer(args[playerArgIndex]);
            }
            if (targetPlayer == null) {
                questDebug("Rezolvare player tinta a esuat pentru '" + args[playerArgIndex] + "'.");
                plugin.getMessageUtils().send(sender, "&cJucatorul &e" + args[playerArgIndex] + " &cnu este online.");
                return null;
            }
            if (!sender.hasPermission("ainpc.admin")
                && (!(sender instanceof Player player) || !player.getUniqueId().equals(targetPlayer.getUniqueId()))) {
                plugin.getMessageUtils().send(sender, "&cNu poti folosi comenzi de quest pentru alt jucator.");
                return null;
            }
            questDebug("Player tinta rezolvat: " + targetPlayer.getName());
            return targetPlayer;
        }

        if (sender instanceof Player player) {
            questDebug("Player tinta implicit din sender: " + player.getName());
            return player;
        }

        questDebug("Rezolvare player tinta a esuat: sender non-player si nu a fost dat argument de jucator.");
        plugin.getMessageUtils().send(sender, "&cDin consola trebuie sa specifici si jucatorul.");
        plugin.getMessageUtils().send(sender, usage.replace("[jucator]", "<jucator>"));
        return null;
    }

    private boolean ensureQuestNpcCommandRange(CommandSender sender, Player targetPlayer, AINPC npc) {
        if (sender.hasPermission("ainpc.admin")) {
            return true;
        }
        if (targetPlayer == null || npc == null) {
            return false;
        }
        if (npc.isInRange(targetPlayer)) {
            return true;
        }

        plugin.getMessageUtils().send(sender, "&cEsti prea departe de NPC-ul &e" + npc.getName() + "&c.");
        return false;
    }

    private void deliverQuestInteraction(CommandSender sender,
                                         Player targetPlayer,
                                         AINPC npc,
                                         ScenarioEngine.QuestInteractionResult questInteraction,
                                         String adminConfirmation) {
        questDebug("Livrare quest interaction: npc=" + npc.getName()
            + " player=" + targetPlayer.getName()
            + " openConversation=" + questInteraction.shouldOpenConversation()
            + " npcMessages=" + questInteraction.getNpcMessages().size()
            + " systemMessages=" + questInteraction.getSystemMessages().size());
        if (questInteraction.shouldOpenConversation()) {
            plugin.getConversationSessionManager().startConversation(targetPlayer, npc);
            plugin.getMemoryManager().ensureFirstMeetingMemoryAsync(npc, targetPlayer);
        }

        for (String npcMessage : questInteraction.getNpcMessages()) {
            plugin.getMessageUtils().sendNPCMessage(targetPlayer, npc.getName(), npcMessage);
        }
        for (String systemMessage : questInteraction.getSystemMessages()) {
            plugin.getMessageUtils().send(targetPlayer, systemMessage);
        }
        if (questInteraction.shouldOpenConversation()) {
            plugin.getMessageUtils().send(targetPlayer,
                "&7&o(Scrie in chat pentru a vorbi cu " + npc.getName() + ". Scrie 'pa' pentru a termina conversatia.)");
        }

        plugin.getEmotionManager().processEvent(npc, "player_approach", 1.0);

        if (!sender.equals(targetPlayer)) {
            plugin.getMessageUtils().send(sender, adminConfirmation);
        }
    }

    private void sendQuestUsage(CommandSender sender) {
        plugin.getMessageUtils().send(sender, "&cUtilizare:");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest log [jucator]");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest track [start|stop] [jucator]");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest status");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest <numeNpc> [jucator]");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest nearest [jucator]");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest accept|da [numeNpc|nearest] [jucator]");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest decline|nu [numeNpc|nearest] [jucator]");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest abandon <numeNpc>|nearest [jucator]");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest status <numeNpc>|nearest [jucator]");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest reset <numeNpc> [jucator]");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest complete <numeNpc> [jucator]");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest anchors [jucator|uuid|all] [templateId]");
    }

    private boolean handleStory(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ainpc.admin")) {
            plugin.getMessageUtils().sendMessage(sender, "no_permission");
            return true;
        }

        if (args.length < 2) {
            sendStoryUsage(sender);
            return true;
        }

        String storyMode = args[1].toLowerCase();
        return switch (storyMode) {
            case "context" -> handleStoryContext(sender, args);
            case "region" -> handleStoryRegion(sender, args);
            case "place" -> handleStoryPlace(sender, args);
            case "events" -> handleStoryEvents(sender, args);
            default -> {
                sendStoryUsage(sender);
                yield true;
            }
        };
    }

    private boolean handleStoryRegion(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.getMessageUtils().send(sender, "&cUtilizare: /ainpc story region <regionId>");
            return true;
        }
        if (plugin.getStoryStateService() == null) {
            plugin.getMessageUtils().send(sender, "&cStoryStateService nu este initializat.");
            return true;
        }

        WorldAdminApi worldAdmin = getEnabledWorldAdmin();
        WorldRegionInfo mappedRegion = resolveSingleStoryRegion(sender, worldAdmin, args[2]);
        if (mappedRegion == null && hasAmbiguousRegionMatch(worldAdmin, args[2])) {
            return true;
        }

        String regionId = mappedRegion != null ? mappedRegion.id() : args[2];
        try {
            RegionStoryState state = plugin.getStoryStateService().getRegionState(regionId).orElse(null);
            List<StoryEvent> events = plugin.getStoryStateService()
                .listRecentEvents(regionId, "", 5);

            plugin.getMessageUtils().send(sender, "&6=== Story Region ===");
            plugin.getMessageUtils().send(sender, "&eRegion ID: &f" + regionId);
            if (mappedRegion != null) {
                plugin.getMessageUtils().send(sender, "&eMapping name: &f" + mappedRegion.name());
                plugin.getMessageUtils().send(sender, "&eMapping story mode: &f" + mappedRegion.storyMode().getId());
                plugin.getMessageUtils().send(sender, "&eMapping story state: &f" + mappedRegion.storyStateKey());
                plugin.getMessageUtils().send(sender, "&eMapping story pool: &f" + formatList(mappedRegion.storyPool()));
            } else {
                plugin.getMessageUtils().send(sender, "&eMapping: &7nu exista regiune mapata pentru selectorul dat");
            }

            if (state == null) {
                plugin.getMessageUtils().send(sender, "&ePersistent state: &7nu exista rand in region_story_state");
            } else {
                sendRegionStoryState(sender, state);
            }
            plugin.getMessageUtils().send(sender, "&eEvenimente recente: &f" + events.size());
            for (StoryEvent event : events) {
                plugin.getMessageUtils().send(sender, "&7- " + formatStoryEvent(event));
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("Nu am putut citi story state pentru regiune: " + exception.getMessage());
            plugin.getMessageUtils().send(sender, "&cNu am putut citi story state: " + exception.getMessage());
        }
        return true;
    }

    private boolean handleStoryPlace(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.getMessageUtils().send(sender, "&cUtilizare: /ainpc story place <placeId>");
            return true;
        }
        if (plugin.getStoryStateService() == null) {
            plugin.getMessageUtils().send(sender, "&cStoryStateService nu este initializat.");
            return true;
        }

        WorldAdminApi worldAdmin = getEnabledWorldAdmin();
        WorldPlaceInfo mappedPlace = resolveSingleStoryPlace(sender, worldAdmin, args[2]);
        if (mappedPlace == null && hasAmbiguousPlaceMatch(worldAdmin, args[2])) {
            return true;
        }

        String placeId = mappedPlace != null ? mappedPlace.id() : args[2];
        String regionId = mappedPlace != null ? mappedPlace.regionId() : inferRegionIdFromPlaceId(placeId);
        try {
            PlaceStoryState state = plugin.getStoryStateService().getPlaceState(placeId).orElse(null);
            List<StoryEvent> events = plugin.getStoryStateService()
                .listRecentEvents(regionId, placeId, 5);

            plugin.getMessageUtils().send(sender, "&6=== Story Place ===");
            plugin.getMessageUtils().send(sender, "&ePlace ID: &f" + placeId);
            if (mappedPlace != null) {
                plugin.getMessageUtils().send(sender, "&eMapping name: &f" + mappedPlace.displayName());
                plugin.getMessageUtils().send(sender, "&eRegiune: &f" + mappedPlace.regionId());
                plugin.getMessageUtils().send(sender, "&eTip: &f" + mappedPlace.placeType().getId());
                plugin.getMessageUtils().send(sender, "&eMetadata story: &f" + formatStoryMetadata(mappedPlace.metadata()));
            } else {
                plugin.getMessageUtils().send(sender, "&eMapping: &7nu exista place mapat pentru selectorul dat");
            }

            if (state == null) {
                plugin.getMessageUtils().send(sender, "&ePersistent state: &7nu exista rand in place_story_state");
            } else {
                sendPlaceStoryState(sender, state);
            }
            plugin.getMessageUtils().send(sender, "&eEvenimente recente: &f" + events.size());
            for (StoryEvent event : events) {
                plugin.getMessageUtils().send(sender, "&7- " + formatStoryEvent(event));
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("Nu am putut citi story state pentru place: " + exception.getMessage());
            plugin.getMessageUtils().send(sender, "&cNu am putut citi story state: " + exception.getMessage());
        }
        return true;
    }

    private boolean handleStoryEvents(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.getMessageUtils().send(sender, "&cUtilizare: /ainpc story events <regionId|placeId> [limit]");
            return true;
        }
        if (plugin.getStoryStateService() == null) {
            plugin.getMessageUtils().send(sender, "&cStoryStateService nu este initializat.");
            return true;
        }

        int limit = STORY_EVENT_DEFAULT_LIMIT;
        if (args.length >= 4) {
            Integer parsedLimit = parseIntegerStrict(args[3]);
            if (parsedLimit == null || parsedLimit <= 0) {
                plugin.getMessageUtils().send(sender, "&cLimit trebuie sa fie un numar pozitiv.");
                return true;
            }
            limit = Math.min(parsedLimit, STORY_EVENT_MAX_LIMIT);
        }

        StoryEventTarget target = resolveStoryEventTarget(sender, args[2]);
        if (target == null) {
            return true;
        }

        try {
            List<StoryEvent> events = plugin.getStoryStateService()
                .listRecentEvents(target.regionId(), target.placeId(), limit);
            plugin.getMessageUtils().send(sender, "&6=== Story Events ===");
            plugin.getMessageUtils().send(sender, "&eTinta: &f" + target.label());
            if (!target.mapped()) {
                plugin.getMessageUtils().send(sender, "&7Selectorul nu a fost gasit in mapping; se cauta direct in DB.");
            }
            if (events.isEmpty()) {
                plugin.getMessageUtils().send(sender, "&7Nu exista story_events pentru tinta curenta.");
                return true;
            }
            for (StoryEvent event : events) {
                plugin.getMessageUtils().send(sender, "&7- " + formatStoryEvent(event));
                if (!event.description().isBlank()) {
                    plugin.getMessageUtils().send(sender, "&8  " + event.description());
                }
                if (!event.payload().isEmpty()) {
                    plugin.getMessageUtils().send(sender, "&8  payload: " + formatMap(event.payload()));
                }
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("Nu am putut lista story events: " + exception.getMessage());
            plugin.getMessageUtils().send(sender, "&cNu am putut lista story events: " + exception.getMessage());
        }
        return true;
    }

    private boolean handleStoryContext(CommandSender sender, String[] args) {
        if (plugin.getStoryContextService() == null) {
            plugin.getMessageUtils().send(sender, "&cStoryContextService nu este initializat.");
            return true;
        }

        StoryContextTarget target = resolveStoryContextTarget(sender, args);
        if (target == null) {
            return true;
        }

        AINPC npc = resolveStoryContextNpc(sender, target.npcSelector(), target.player());
        if (npc == null && target.npcSelector() != null && !target.npcSelector().isBlank()) {
            return true;
        }

        StoryContextSnapshot snapshot = npc != null
            ? plugin.getStoryContextService().buildForNpc(npc, target.player())
            : plugin.getStoryContextService().buildForPlayer(target.player());

        plugin.getMessageUtils().send(sender, "&6=== Story Context ===");
        plugin.getMessageUtils().send(sender, "&eJucator: &f" + target.player().getName());
        plugin.getMessageUtils().send(sender, "&eNPC: &f" + (npc != null ? npc.getName() : "<fara NPC tinta>"));

        String promptBlock = snapshot.toPromptBlock();
        if (promptBlock.isBlank()) {
            plugin.getMessageUtils().send(sender, "&7Nu exista context story relevant pentru tinta curenta.");
            return true;
        }

        for (String line : promptBlock.split("\\R")) {
            if (!line.isBlank()) {
                plugin.getMessageUtils().send(sender, "&7" + line);
            }
        }
        return true;
    }

    private StoryContextTarget resolveStoryContextTarget(CommandSender sender, String[] args) {
        if (args.length > 2) {
            Player explicitPlayer = findOnlinePlayer(args[2]);
            if (explicitPlayer != null) {
                String npcSelector = args.length > 3 ? args[3] : "";
                return new StoryContextTarget(explicitPlayer, npcSelector);
            }

            if (sender instanceof Player player) {
                return new StoryContextTarget(player, args[2]);
            }

            plugin.getMessageUtils().send(sender, "&cJucatorul &e" + args[2] + " &cnu este online.");
            plugin.getMessageUtils().send(sender, "&cUtilizare consola: /ainpc story context <jucator> [numeNpc|nearest]");
            return null;
        }

        if (sender instanceof Player player) {
            return new StoryContextTarget(player, "");
        }

        plugin.getMessageUtils().send(sender, "&cDin consola trebuie sa specifici si jucatorul.");
        plugin.getMessageUtils().send(sender, "&cUtilizare consola: /ainpc story context <jucator> [numeNpc|nearest]");
        return null;
    }

    private Player findOnlinePlayer(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return null;
        }

        Player targetPlayer = plugin.getServer().getPlayerExact(playerName);
        if (targetPlayer == null) {
            targetPlayer = plugin.getServer().getPlayer(playerName);
        }
        return targetPlayer;
    }

    private AINPC resolveStoryContextNpc(CommandSender sender, String npcSelector, Player targetPlayer) {
        if (npcSelector == null || npcSelector.isBlank()) {
            return null;
        }

        if ("nearest".equalsIgnoreCase(npcSelector)) {
            AINPC nearestNpc = findNearestQuestNpc(targetPlayer);
            if (nearestNpc == null) {
                plugin.getMessageUtils().send(sender, "&cNu exista NPC-uri active in apropierea jucatorului.");
            }
            return nearestNpc;
        }

        AINPC npc = plugin.getNpcManager().getNPCByName(npcSelector);
        if (npc == null) {
            plugin.getMessageUtils().sendMessage(sender, "npc_not_found");
        }
        return npc;
    }

    private WorldAdminApi getEnabledWorldAdmin() {
        WorldAdminApi worldAdmin = plugin.getPlatform() != null ? plugin.getPlatform().getWorldAdmin() : null;
        return worldAdmin != null && worldAdmin.isEnabled() ? worldAdmin : null;
    }

    private WorldRegionInfo resolveSingleStoryRegion(CommandSender sender, WorldAdminApi worldAdmin, String selector) {
        if (worldAdmin == null) {
            return null;
        }

        List<WorldRegionInfo> matches = findRegionMatches(worldAdmin, selector);
        if (matches.size() > 1) {
            plugin.getMessageUtils().send(sender, "&cSelector ambiguu pentru regiune. Foloseste ID-ul complet.");
            plugin.getMessageUtils().send(sender, "&7Potriviri: &f" + formatList(matches.stream().map(WorldRegionInfo::id).toList()));
            return null;
        }
        return matches.isEmpty() ? null : matches.get(0);
    }

    private WorldPlaceInfo resolveSingleStoryPlace(CommandSender sender, WorldAdminApi worldAdmin, String selector) {
        if (worldAdmin == null) {
            return null;
        }

        List<WorldPlaceInfo> matches = findPlaceMatches(worldAdmin, selector);
        if (matches.size() > 1) {
            plugin.getMessageUtils().send(sender, "&cSelector ambiguu pentru place. Foloseste ID-ul complet.");
            plugin.getMessageUtils().send(sender, "&7Potriviri: &f" + formatList(matches.stream().map(WorldPlaceInfo::id).toList()));
            return null;
        }
        return matches.isEmpty() ? null : matches.get(0);
    }

    private boolean hasAmbiguousRegionMatch(WorldAdminApi worldAdmin, String selector) {
        return worldAdmin != null && findRegionMatches(worldAdmin, selector).size() > 1;
    }

    private boolean hasAmbiguousPlaceMatch(WorldAdminApi worldAdmin, String selector) {
        return worldAdmin != null && findPlaceMatches(worldAdmin, selector).size() > 1;
    }

    private StoryEventTarget resolveStoryEventTarget(CommandSender sender, String selector) {
        WorldAdminApi worldAdmin = getEnabledWorldAdmin();
        if (worldAdmin != null) {
            List<WorldPlaceInfo> placeMatches = findPlaceMatches(worldAdmin, selector);
            if (placeMatches.size() > 1) {
                plugin.getMessageUtils().send(sender, "&cSelector ambiguu pentru place. Foloseste ID-ul complet.");
                plugin.getMessageUtils().send(sender, "&7Potriviri: &f" + formatList(placeMatches.stream().map(WorldPlaceInfo::id).toList()));
                return null;
            }
            if (placeMatches.size() == 1) {
                WorldPlaceInfo place = placeMatches.get(0);
                return new StoryEventTarget(
                    place.regionId(),
                    place.id(),
                    "place " + place.id(),
                    true
                );
            }

            List<WorldRegionInfo> regionMatches = findRegionMatches(worldAdmin, selector);
            if (regionMatches.size() > 1) {
                plugin.getMessageUtils().send(sender, "&cSelector ambiguu pentru regiune. Foloseste ID-ul complet.");
                plugin.getMessageUtils().send(sender, "&7Potriviri: &f" + formatList(regionMatches.stream().map(WorldRegionInfo::id).toList()));
                return null;
            }
            if (regionMatches.size() == 1) {
                WorldRegionInfo region = regionMatches.get(0);
                return new StoryEventTarget(
                    region.id(),
                    "",
                    "region " + region.id(),
                    true
                );
            }
        }

        String rawSelector = selector != null ? selector.trim() : "";
        if (rawSelector.isBlank()) {
            plugin.getMessageUtils().send(sender, "&cSelectorul story events nu poate fi gol.");
            return null;
        }
        if (rawSelector.contains(":")) {
            return new StoryEventTarget(
                inferRegionIdFromPlaceId(rawSelector),
                rawSelector,
                "place " + rawSelector,
                false
            );
        }
        return new StoryEventTarget(rawSelector, "", "region " + rawSelector, false);
    }

    private void sendRegionStoryState(CommandSender sender, RegionStoryState state) {
        plugin.getMessageUtils().send(sender, "&ePersistent mode: &f" + state.storyMode().getId());
        plugin.getMessageUtils().send(sender, "&ePersistent state: &f" + state.stateKey());
        plugin.getMessageUtils().send(sender, "&ePersistent pool: &f" + formatList(state.storyPool()));
        plugin.getMessageUtils().send(sender, "&eVariables: &f" + formatMap(state.variables()));
        plugin.getMessageUtils().send(sender, "&eUpdated: &f" + formatStoryTime(state.updatedAt())
            + " &7by &f" + formatOptional(state.updatedBy())
            + " &7source=&f" + formatOptional(state.source()));
    }

    private void sendPlaceStoryState(CommandSender sender, PlaceStoryState state) {
        plugin.getMessageUtils().send(sender, "&ePersistent state: &f" + state.stateKey());
        plugin.getMessageUtils().send(sender, "&eRegion: &f" + formatOptional(state.regionId()));
        plugin.getMessageUtils().send(sender, "&eVariables: &f" + formatMap(state.variables()));
        plugin.getMessageUtils().send(sender, "&eUpdated: &f" + formatStoryTime(state.updatedAt())
            + " &7by &f" + formatOptional(state.updatedBy())
            + " &7source=&f" + formatOptional(state.source()));
    }

    private String formatStoryEvent(StoryEvent event) {
        String eventKey = event.eventKey().isBlank() ? "<no-key>" : event.eventKey();
        String title = event.title().isBlank() ? "" : " - " + event.title();
        String actor = event.actorType().isBlank() && event.actorId().isBlank()
            ? ""
            : " actor=" + formatOptional(event.actorType()) + ":" + formatOptional(event.actorId());
        return "#" + event.id()
            + " " + formatStoryTime(event.createdAt())
            + " " + event.scopeType() + ":" + event.scopeId()
            + " " + event.eventType() + "/" + eventKey
            + title
            + actor;
    }

    private String formatStoryMetadata(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "<gol>";
        }

        Map<String, String> storyMetadata = new HashMap<>();
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            String key = entry.getKey() != null ? entry.getKey().toLowerCase() : "";
            if (key.contains("story") || key.contains("event") || key.contains("state")
                || key.contains("tension") || key.contains("danger") || key.contains("conflict")) {
                storyMetadata.put(entry.getKey(), entry.getValue());
            }
        }
        return formatMap(storyMetadata);
    }

    private String inferRegionIdFromPlaceId(String placeId) {
        if (placeId == null || placeId.isBlank() || !placeId.contains(":")) {
            return "";
        }
        return placeId.substring(0, placeId.indexOf(':'));
    }

    private String formatStoryTime(long epochMillis) {
        if (epochMillis <= 0L) {
            return "<necunoscut>";
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())
            .format(STORY_TIME_FORMAT);
    }

    private void sendStoryUsage(CommandSender sender) {
        plugin.getMessageUtils().send(sender, "&cUtilizare:");
        plugin.getMessageUtils().send(sender, "&e/ainpc story context [jucator] [numeNpc|nearest]");
        plugin.getMessageUtils().send(sender, "&e/ainpc story region <regionId>");
        plugin.getMessageUtils().send(sender, "&e/ainpc story place <placeId>");
        plugin.getMessageUtils().send(sender, "&e/ainpc story events <regionId|placeId> [limit]");
        plugin.getMessageUtils().send(sender, "&7Fara NPC tinta, contextul este construit pentru locatia jucatorului.");
    }

    private boolean handleWorld(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ainpc.admin")) {
            plugin.getMessageUtils().sendMessage(sender, "no_permission");
            return true;
        }

        if (args.length < 2) {
            sendWorldUsage(sender);
            return true;
        }

        String worldMode = args[1].toLowerCase();
        return switch (worldMode) {
            case "whereami" -> handleWorldWhereAmI(sender, args);
            case "places" -> handleWorldPlaces(sender, args);
            case "region" -> handleWorldRegion(sender, args);
            case "place" -> handleWorldPlace(sender, args);
            case "node" -> handleWorldNode(sender, args);
            case "scan" -> handleWorldScan(sender, args);
            case "demo" -> handleWorldDemo(sender, args);
            case "bind" -> handleWorldBind(sender, args);
            case "household" -> handleWorldHousehold(sender, args);
            case "settlement" -> handleWorldSettlement(sender, args);
            case "save" -> handleWorldSave(sender);
            default -> {
                sendWorldUsage(sender);
                yield true;
            }
        };
    }

    private boolean handleWorldWhereAmI(CommandSender sender, String[] args) {
        Player targetPlayer = resolveQuestTargetPlayer(
            sender,
            args,
            2,
            "&cUtilizare: /ainpc world whereami [jucator]"
        );
        if (targetPlayer == null) {
            return true;
        }

        WorldAdminApi worldAdmin = plugin.getPlatform().getWorldAdmin();
        if (!worldAdmin.isEnabled()) {
            plugin.getMessageUtils().send(sender, "&cWorld admin este dezactivat.");
            return true;
        }

        Location location = targetPlayer.getLocation();
        WorldRegionInfo region = worldAdmin.findRegion(
            location.getWorld().getName(),
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ()
        );
        WorldPlaceInfo place = worldAdmin.findPlace(
            location.getWorld().getName(),
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ()
        );
        List<WorldNodeInfo> nearbyNodes = findNodesAtLocation(worldAdmin, location);

        plugin.getMessageUtils().send(sender, "&6=== World Mapping: whereami ===");
        plugin.getMessageUtils().send(sender, "&eJucator: &f" + targetPlayer.getName());
        plugin.getMessageUtils().send(sender, "&eLocatie: &f" + formatLocation(location));

        if (region != null) {
            plugin.getMessageUtils().send(sender, "&eRegiune: &f" + region.id() + " &7(" + region.name() + ")");
            plugin.getMessageUtils().send(sender, "&eTip regiune: &f" + region.typeId());
        } else {
            plugin.getMessageUtils().send(sender, "&eRegiune: &cNiciuna");
        }

        if (place != null) {
            plugin.getMessageUtils().send(sender, "&ePlace: &f" + place.id() + " &7(" + place.displayName() + ")");
            plugin.getMessageUtils().send(sender, "&eTip place: &f" + place.placeType().getId());
            plugin.getMessageUtils().send(sender, "&eTag-uri place: &f" + formatList(place.tags()));
        } else {
            plugin.getMessageUtils().send(sender, "&ePlace: &cNiciunul");
        }

        if (nearbyNodes.isEmpty()) {
            plugin.getMessageUtils().send(sender, "&eNodes active aici: &7niciunul");
        } else {
            plugin.getMessageUtils().send(sender, "&eNodes active aici: &f" + formatList(
                nearbyNodes.stream().map(WorldNodeInfo::id).toList()
            ));
        }

        return true;
    }

    private boolean handleWorldPlaces(CommandSender sender, String[] args) {
        WorldAdminApi worldAdmin = plugin.getPlatform().getWorldAdmin();
        if (!worldAdmin.isEnabled()) {
            plugin.getMessageUtils().send(sender, "&cWorld admin este dezactivat.");
            return true;
        }

        String regionFilter = args.length > 2 ? args[2] : null;
        List<WorldPlaceInfo> places = (regionFilter == null
            ? worldAdmin.getPlaces()
            : worldAdmin.getPlaces(regionFilter))
            .stream()
            .sorted(Comparator.comparing(WorldPlaceInfo::id))
            .toList();

        if (places.isEmpty()) {
            plugin.getMessageUtils().send(sender,
                regionFilter == null
                    ? "&7Nu exista places configurate."
                    : "&7Nu exista places configurate pentru regiunea &f" + regionFilter + "&7.");
            return true;
        }

        plugin.getMessageUtils().send(sender, "&6=== Places (" + places.size() + ") ===");
        if (regionFilter != null) {
            plugin.getMessageUtils().send(sender, "&7Filtru regiune: &f" + regionFilter);
        }

        for (WorldPlaceInfo place : places) {
            plugin.getMessageUtils().send(sender,
                "&e" + place.id() + " &7- &f" + place.displayName()
                    + " &8[" + place.placeType().getId() + "]"
                    + " &7regiune=&f" + place.regionId());
        }

        return true;
    }

    private boolean handleWorldScan(CommandSender sender, String[] args) {
        if (args.length < 3 || !"village".equalsIgnoreCase(args[2])) {
            plugin.getMessageUtils().send(sender,
                "&cUtilizare: /ainpc world scan village [radius] [import] [regionId]");
            return true;
        }

        Player player = requirePlayerSender(sender);
        if (player == null) {
            return true;
        }

        WorldAdminService worldAdmin = plugin.getPlatform().getWorldAdminService();
        if (!worldAdmin.isEnabled()) {
            plugin.getMessageUtils().send(sender, "&cWorld admin este dezactivat.");
            return true;
        }

        int radius = VanillaVillageScanner.DEFAULT_HORIZONTAL_RADIUS;
        if (args.length >= 4) {
            Integer parsedRadius = parseIntegerStrict(args[3]);
            if (parsedRadius == null || parsedRadius <= 0) {
                plugin.getMessageUtils().send(sender, "&cRadius trebuie sa fie un numar pozitiv.");
                return true;
            }
            radius = parsedRadius;
        }

        boolean shouldImport = args.length >= 5 && "import".equalsIgnoreCase(args[4]);
        if (args.length >= 5 && !shouldImport) {
            plugin.getMessageUtils().send(sender,
                "&cUtilizare: /ainpc world scan village [radius] [import] [regionId]");
            return true;
        }
        String regionId = args.length >= 6 ? args[5] : null;

        VanillaVillageScanResult scan = new VanillaVillageScanner().scan(
            player.getLocation(),
            radius,
            VanillaVillageScanner.DEFAULT_VERTICAL_RADIUS
        );
        sendVillageScanSummary(sender, scan);

        if (!shouldImport) {
            plugin.getMessageUtils().send(sender,
                "&7Dry-run. Pentru import ruleaza &f/ainpc world scan village "
                    + scan.horizontalRadius() + " import [regionId]&7.");
            return true;
        }

        SemanticVillageImportResult result = new SemanticVillageMapper().importScan(worldAdmin, scan, regionId);
        if (!result.errors().isEmpty()) {
            plugin.getMessageUtils().send(sender, "&cImportul mapping-ului vanilla a fost oprit:");
            for (String error : result.errors()) {
                plugin.getMessageUtils().send(sender, "&7- &f" + error);
            }
            return true;
        }

        plugin.getMessageUtils().send(sender, "&aMapping vanilla importat in regiunea &f" + result.regionId() + "&a.");
        plugin.getMessageUtils().send(sender, "&7Places create: &f" + result.createdPlaceIds().size()
            + " &7| Nodes create: &f" + result.createdNodeIds().size());
        if (!result.warnings().isEmpty()) {
            plugin.getMessageUtils().send(sender, "&eWarning-uri:");
            for (String warning : result.warnings()) {
                plugin.getMessageUtils().send(sender, "&7- &f" + warning);
            }
        }
        plugin.getMessageUtils().send(sender, "&7Ruleaza &f/ainpc world save &7ca sa persisti mapping-ul.");
        return true;
    }

    private boolean handleWorldDemo(CommandSender sender, String[] args) {
        if (args.length < 3 || args.length > 4 || !"create".equalsIgnoreCase(args[2])) {
            plugin.getMessageUtils().send(sender, "&cUtilizare: /ainpc world demo create [regionId]");
            plugin.getMessageUtils().send(sender, "&7Creeaza un mapping demo minim in jurul pozitiei tale curente.");
            return true;
        }

        Player player = requirePlayerSender(sender);
        if (player == null) {
            return true;
        }

        WorldAdminService worldAdmin = plugin.getPlatform().getWorldAdminService();
        if (!worldAdmin.isEnabled()) {
            plugin.getMessageUtils().send(sender, "&cWorld admin este dezactivat.");
            return true;
        }

        String regionId = args.length == 4 ? args[3] : null;
        Location location = player.getLocation();
        try {
            WorldAdminService.DemoMappingResult result = worldAdmin.createDemoSettlement(
                regionId,
                player.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                player.getWorld().getMinHeight(),
                player.getWorld().getMaxHeight()
            );

            plugin.getMessageUtils().send(sender, "&aMapping demo creat in regiunea &f" + result.regionId() + "&a.");
            plugin.getMessageUtils().send(sender, "&7Centru: &f" + location.getBlockX() + ", "
                + location.getBlockY() + ", " + location.getBlockZ());
            plugin.getMessageUtils().send(sender, "&7Places create: &f" + result.createdPlaceIds().size()
                + " &7| Nodes create: &f" + result.createdNodeIds().size());
            plugin.getMessageUtils().send(sender, "&7Places: &f" + formatList(result.createdPlaceIds()));
            for (String warning : result.warnings()) {
                plugin.getMessageUtils().send(sender, "&eWarning: &f" + warning);
            }
            plugin.getMessageUtils().send(sender, "&7Urmatorul pas: &f/ainpc audit world");
            plugin.getMessageUtils().send(sender, "&7Daca auditul arata bine, ruleaza &f/ainpc world save&7.");
        } catch (IllegalArgumentException exception) {
            plugin.getMessageUtils().send(sender, "&c" + exception.getMessage());
        }
        return true;
    }

    private boolean handleWorldBind(CommandSender sender, String[] args) {
        if (args.length < 5 || args.length > 7 || !"npc".equalsIgnoreCase(args[2])) {
            plugin.getMessageUtils().send(sender,
                "&cUtilizare: /ainpc world bind npc <numeNpc|nearest> <homePlaceId> [workPlaceId|-] [socialPlaceId|-]");
            plugin.getMessageUtils().send(sender,
                "&7Leaga un NPC incarcat la home/work/social anchors din world mapping.");
            return true;
        }

        WorldAdminService worldAdmin = plugin.getPlatform().getWorldAdminService();
        if (!worldAdmin.isEnabled()) {
            plugin.getMessageUtils().send(sender, "&cWorld admin este dezactivat.");
            return true;
        }

        AINPC npc = resolveWorldBindNpc(sender, args[3]);
        if (npc == null) {
            return true;
        }

        WorldPlaceInfo homePlace = resolveSingleWorldPlace(sender, worldAdmin, args[4], "home");
        if (homePlace == null) {
            return true;
        }

        WorldPlaceInfo workPlace = null;
        if (args.length >= 6 && !isNoneSelector(args[5])) {
            workPlace = resolveSingleWorldPlace(sender, worldAdmin, args[5], "work");
            if (workPlace == null) {
                return true;
            }
        }

        WorldPlaceInfo socialPlace = null;
        if (args.length >= 7 && !isNoneSelector(args[6])) {
            socialPlace = resolveSingleWorldPlace(sender, worldAdmin, args[6], "social");
            if (socialPlace == null) {
                return true;
            }
        }

        if (!isHousePlace(homePlace)) {
            plugin.getMessageUtils().send(sender,
                "&eWarning: homePlace-ul &f" + homePlace.id() + " &enu este marcat ca house/home.");
        }
        if (workPlace != null && !isWorkplace(workPlace)) {
            plugin.getMessageUtils().send(sender,
                "&eWarning: workPlace-ul &f" + workPlace.id() + " &enu este marcat clar ca workplace.");
        }
        if (socialPlace != null && !isSocialPlace(socialPlace)) {
            plugin.getMessageUtils().send(sender,
                "&eWarning: socialPlace-ul &f" + socialPlace.id() + " &enu este marcat clar ca loc social.");
        }

        AINPC.OwnedLocation previousHome = npc.getHomeAnchor();
        AINPC.OwnedLocation previousWork = npc.getWorkAnchor();
        AINPC.OwnedLocation previousSocial = npc.getSocialAnchor();
        AINPC.OwnedLocation homeAnchor = createOwnedLocationFromPlace(worldAdmin, homePlace, "home");
        AINPC.OwnedLocation workAnchor = workPlace != null
            ? createOwnedLocationFromPlace(worldAdmin, workPlace, "work")
            : previousWork;
        AINPC.OwnedLocation socialAnchor = socialPlace != null
            ? createOwnedLocationFromPlace(worldAdmin, socialPlace, "social")
            : previousSocial;

        npc.setHomeAnchor(homeAnchor);
        npc.setWorkAnchor(workAnchor);
        npc.setSocialAnchor(socialAnchor);

        if (!plugin.getNpcManager().saveNPC(npc, false)) {
            npc.setHomeAnchor(previousHome);
            npc.setWorkAnchor(previousWork);
            npc.setSocialAnchor(previousSocial);
            plugin.getMessageUtils().send(sender, "&cNu am putut salva profilul NPC-ului.");
            return true;
        }

        String npcBindingId = npcBindingId(npc);
        try {
            worldAdmin.bindNpcToHomePlace(homePlace.id(), npcBindingId, npc.getName());
            if (workPlace != null) {
                worldAdmin.bindNpcToWorkPlace(workPlace.id(), npcBindingId, npc.getName());
            }
            if (socialPlace != null) {
                worldAdmin.bindNpcToSocialPlace(socialPlace.id(), npcBindingId, npc.getName());
            }
        } catch (IllegalArgumentException exception) {
            plugin.getMessageUtils().send(sender, "&c" + exception.getMessage());
            return true;
        }

        plugin.getMessageUtils().send(sender, "&aNPC-ul &f" + npc.getName() + " &aa fost legat la mapping.");
        plugin.getMessageUtils().send(sender, "&eHome: &f" + formatOwnedLocation(homeAnchor));
        if (workPlace != null) {
            plugin.getMessageUtils().send(sender, "&eWork: &f" + formatOwnedLocation(workAnchor));
        } else {
            plugin.getMessageUtils().send(sender, "&eWork: &7pastrat neschimbat");
        }
        if (socialPlace != null) {
            plugin.getMessageUtils().send(sender, "&eSocial: &f" + formatOwnedLocation(socialAnchor));
        } else {
            plugin.getMessageUtils().send(sender, "&eSocial: &7pastrat neschimbat");
        }
        plugin.getMessageUtils().send(sender,
            "&7Profilul NPC a fost salvat. Ruleaza &f/ainpc world save &7pentru metadata mapping, apoi &f/ainpc audit spawn&7.");
        return true;
    }

    private boolean handleWorldHousehold(CommandSender sender, String[] args) {
        if (args.length < 4 || args.length > 5
            || (!"plan".equalsIgnoreCase(args[2]) && !"spawn".equalsIgnoreCase(args[2]))) {
            plugin.getMessageUtils().send(sender,
                "&cUtilizare: /ainpc world household <plan|spawn> <homePlaceId> [count]");
            plugin.getMessageUtils().send(sender,
                "&7Genereaza un HouseAllocation din mapping. plan este dry-run, spawn creeaza NPC-uri.");
            return true;
        }

        WorldAdminService worldAdmin = plugin.getPlatform().getWorldAdminService();
        if (!worldAdmin.isEnabled()) {
            plugin.getMessageUtils().send(sender, "&cWorld admin este dezactivat.");
            return true;
        }

        int requestedCount = 0;
        if (args.length == 5) {
            Integer parsedCount = parseIntegerStrict(args[4]);
            if (parsedCount == null || parsedCount <= 0) {
                plugin.getMessageUtils().send(sender, "&cCount trebuie sa fie un numar pozitiv.");
                return true;
            }
            requestedCount = parsedCount;
        }

        HouseAllocationPlanner.PlanningResult planning =
            new HouseAllocationPlanner().plan(worldAdmin, args[3], requestedCount);
        if (!planning.success()) {
            plugin.getMessageUtils().send(sender, "&cNu am putut genera HouseAllocation.");
            sendAuditMessages(sender, "&cErori", planning.errors());
            sendAuditMessages(sender, "&eWarning-uri", planning.warnings());
            return true;
        }

        HouseAllocation allocation = planning.allocation();
        sendHouseholdAllocationSummary(sender, allocation);
        sendAuditMessages(sender, "&eWarning-uri planner", planning.warnings());

        boolean shouldSpawn = "spawn".equalsIgnoreCase(args[2]);
        HouseholdSpawnResult result = shouldSpawn
            ? plugin.getNpcSpawnOrchestrator().spawnHousehold(allocation)
            : plugin.getNpcSpawnOrchestrator().dryRunHouseAllocation(allocation);

        sendHouseholdSpawnResult(sender, result);
        if (!result.success()) {
            return true;
        }

        if (shouldSpawn) {
            bindSpawnedHouseholdToMapping(sender, worldAdmin, result);
            plugin.getMessageUtils().send(sender,
                "&7NPC-urile au fost create si legate la mapping. Ruleaza &f/ainpc world save &7si &f/ainpc audit spawn&7.");
        } else {
            plugin.getMessageUtils().send(sender,
                "&7Dry-run reusit. Pentru executie: &f/ainpc world household spawn "
                    + allocation.placeId() + " " + allocation.residentPlans().size());
        }
        return true;
    }

    private boolean handleWorldSettlement(CommandSender sender, String[] args) {
        if (args.length < 4 || args.length > 5
            || (!"plan".equalsIgnoreCase(args[2]) && !"spawn".equalsIgnoreCase(args[2]))) {
            plugin.getMessageUtils().send(sender,
                "&cUtilizare: /ainpc world settlement <plan|spawn> <regionId> [maxHouses]");
            plugin.getMessageUtils().send(sender,
                "&7Genereaza household plan-uri pentru toate casele dintr-o regiune.");
            return true;
        }

        WorldAdminService worldAdmin = plugin.getPlatform().getWorldAdminService();
        if (!worldAdmin.isEnabled()) {
            plugin.getMessageUtils().send(sender, "&cWorld admin este dezactivat.");
            return true;
        }

        int maxHouses = 0;
        if (args.length == 5) {
            Integer parsedMaxHouses = parseIntegerStrict(args[4]);
            if (parsedMaxHouses == null || parsedMaxHouses <= 0) {
                plugin.getMessageUtils().send(sender, "&cmaxHouses trebuie sa fie un numar pozitiv.");
                return true;
            }
            maxHouses = parsedMaxHouses;
        }

        HouseAllocationPlanner.SettlementPlanningResult planning =
            new HouseAllocationPlanner().planSettlement(worldAdmin, args[3], maxHouses);
        if (!planning.success()) {
            plugin.getMessageUtils().send(sender, "&cNu am putut genera planul pentru regiune.");
            sendSettlementPlanningSummary(sender, planning);
            sendAuditMessages(sender, "&cErori", planning.errors());
            sendAuditMessages(sender, "&eWarning-uri", planning.warnings());
            return true;
        }

        sendSettlementPlanningSummary(sender, planning);
        sendAuditMessages(sender, "&eWarning-uri planner", planning.warnings());

        boolean shouldSpawn = "spawn".equalsIgnoreCase(args[2]);
        SettlementSpawnResult result = shouldSpawn
            ? plugin.getNpcSpawnOrchestrator().spawnSettlement(planning.allocations())
            : plugin.getNpcSpawnOrchestrator().dryRunSettlement(planning.allocations());

        sendSettlementSpawnResult(sender, result);
        if (result.success()) {
            if (shouldSpawn) {
                bindSpawnedSettlementToMapping(sender, worldAdmin, result);
                plugin.getMessageUtils().send(sender,
                    "&7Settlement spawn terminat. Ruleaza &f/ainpc world save &7si &f/ainpc audit spawn&7.");
            } else {
                plugin.getMessageUtils().send(sender,
                    "&7Dry-run reusit. Pentru executie: &f/ainpc world settlement spawn "
                        + planning.regionId() + (maxHouses > 0 ? " " + maxHouses : ""));
            }
        }
        return true;
    }

    private void sendHouseholdAllocationSummary(CommandSender sender, HouseAllocation allocation) {
        plugin.getMessageUtils().send(sender, "&6=== Household Plan ===");
        plugin.getMessageUtils().send(sender, "&eCasa: &f" + allocation.placeId());
        plugin.getMessageUtils().send(sender, "&eFamily: &f" + formatOptional(allocation.familyId()));
        plugin.getMessageUtils().send(sender, "&eRezidenti: &f" + allocation.residentPlans().size()
            + " &7/ max &f" + allocation.maxResidents());
        for (HouseAllocation.ResidentPlan resident : allocation.residentPlans()) {
            plugin.getMessageUtils().send(sender,
                "&7- &f" + resident.name()
                    + " &7key=&f" + resident.npcKey()
                    + " &7ocupatie=&f" + formatOptional(resident.occupation())
                    + " &7spawn=&f" + resident.spawnNodeId()
                    + " &7home=&f" + resident.effectiveHomeNodeId()
                    + " &7work=&f" + formatOptional(resident.workPlaceId()));
        }
    }

    private void sendSettlementPlanningSummary(CommandSender sender,
                                               HouseAllocationPlanner.SettlementPlanningResult planning) {
        plugin.getMessageUtils().send(sender, "&6=== Settlement Plan ===");
        plugin.getMessageUtils().send(sender, "&eRegiune: &f" + formatOptional(planning.regionId()));
        plugin.getMessageUtils().send(sender, "&eHousehold-uri: &f" + planning.allocations().size());
        int totalResidents = planning.allocations().stream()
            .mapToInt(allocation -> allocation.residentPlans().size())
            .sum();
        plugin.getMessageUtils().send(sender, "&eRezidenti planificati: &f" + totalResidents);
        for (HouseAllocation allocation : planning.allocations().stream().limit(8).toList()) {
            plugin.getMessageUtils().send(sender,
                "&7- &f" + allocation.placeId()
                    + " &7rezidenti=&f" + allocation.residentPlans().size()
                    + " &7family=&f" + formatOptional(allocation.familyId()));
        }
        if (planning.allocations().size() > 8) {
            plugin.getMessageUtils().send(sender,
                "&7... inca " + (planning.allocations().size() - 8) + " household-uri.");
        }
    }

    private void sendHouseholdSpawnResult(CommandSender sender, HouseholdSpawnResult result) {
        String mode = result.dryRun() ? "dry-run" : "spawn";
        if (result.success()) {
            plugin.getMessageUtils().send(sender, "&aHousehold " + mode + " reusit.");
        } else {
            plugin.getMessageUtils().send(sender, "&cHousehold " + mode + " esuat.");
        }
        plugin.getMessageUtils().send(sender, "&eSpawn plans: &f" + result.spawnPlans().size()
            + " &7| Rezultate spawn: &f" + result.spawnResults().size());
        if (result.rolledBack()) {
            plugin.getMessageUtils().send(sender, "&eRollback: &fexecutat pentru NPC-urile create partial.");
        }
        sendAuditMessages(sender, "&cErori household", result.errors());
        sendAuditMessages(sender, "&eWarning-uri household", result.warnings());
        if (!result.spawnResults().isEmpty()) {
            List<String> spawned = result.spawnResults().stream()
                .filter(NpcSpawnResult::success)
                .map(spawnResult -> spawnResult.npc().getName() + "#" + spawnResult.npc().getDatabaseId())
                .toList();
            if (!spawned.isEmpty()) {
                plugin.getMessageUtils().send(sender, "&eNPC-uri create: &f" + formatList(spawned));
            }
        }
    }

    private void sendSettlementSpawnResult(CommandSender sender, SettlementSpawnResult result) {
        String mode = result.dryRun() ? "dry-run" : "spawn";
        plugin.getMessageUtils().send(sender,
            (result.success() ? "&a" : "&c") + "Settlement " + mode
                + ": &f" + result.successfulHouseholds() + "/" + result.allocations().size()
                + " &7household-uri reusite, &f" + result.totalSpawnPlans() + " &7NPC planificati.");
        if (result.rolledBack()) {
            plugin.getMessageUtils().send(sender,
                "&eRollback global: &fNPC-urile create in household-uri anterioare au fost sterse.");
        }
        sendAuditMessages(sender, "&cErori settlement", result.errors());
        sendAuditMessages(sender, "&eWarning-uri settlement", result.warnings());
    }

    private void bindSpawnedSettlementToMapping(CommandSender sender,
                                                WorldAdminService worldAdmin,
                                                SettlementSpawnResult result) {
        int householdCount = 0;
        for (HouseholdSpawnResult householdResult : result.householdResults()) {
            if (!householdResult.success()) {
                continue;
            }
            bindSpawnedHouseholdToMapping(sender, worldAdmin, householdResult);
            householdCount++;
        }
        plugin.getMessageUtils().send(sender, "&eHousehold-uri legate la mapping: &f" + householdCount);
    }

    private void bindSpawnedHouseholdToMapping(CommandSender sender,
                                               WorldAdminService worldAdmin,
                                               HouseholdSpawnResult result) {
        List<NpcSpawnPlan> plans = result.spawnPlans();
        List<NpcSpawnResult> spawnResults = result.spawnResults();
        int boundCount = 0;
        for (int index = 0; index < Math.min(plans.size(), spawnResults.size()); index++) {
            NpcSpawnPlan plan = plans.get(index);
            NpcSpawnResult spawnResult = spawnResults.get(index);
            if (!spawnResult.success() || spawnResult.npc() == null) {
                continue;
            }

            AINPC npc = spawnResult.npc();
            String bindingId = npcBindingId(npc);
            try {
                if (!plan.homePlaceId().isBlank()) {
                    worldAdmin.bindNpcToHomePlace(plan.homePlaceId(), bindingId, npc.getName());
                }
                if (!plan.workPlaceId().isBlank()) {
                    worldAdmin.bindNpcToWorkPlace(plan.workPlaceId(), bindingId, npc.getName());
                }
                if (!plan.socialPlaceId().isBlank()) {
                    worldAdmin.bindNpcToSocialPlace(plan.socialPlaceId(), bindingId, npc.getName());
                }
                boundCount++;
            } catch (IllegalArgumentException exception) {
                plugin.getMessageUtils().send(sender, "&eWarning: &f" + exception.getMessage());
            }
        }
        plugin.getMessageUtils().send(sender, "&eBind-uri mapping actualizate: &f" + boundCount);
    }

    private void sendVillageScanSummary(CommandSender sender, VanillaVillageScanResult scan) {
        plugin.getMessageUtils().send(sender, "&6=== Vanilla Village Scan ===");
        plugin.getMessageUtils().send(sender, "&eLume: &f" + scan.worldName());
        plugin.getMessageUtils().send(sender, "&eCentru: &f"
            + scan.centerX() + ", " + scan.centerY() + ", " + scan.centerZ());
        plugin.getMessageUtils().send(sender, "&eRaza: &f" + scan.horizontalRadius()
            + " &7orizontal / &f" + scan.verticalRadius() + " &7vertical");
        plugin.getMessageUtils().send(sender, "&eSemnale: &f"
            + "clopote=" + scan.count(VanillaVillageFeatureType.BELL)
            + ", paturi=" + scan.count(VanillaVillageFeatureType.BED)
            + ", workstation-uri=" + scan.count(VanillaVillageFeatureType.WORKSTATION)
            + ", usi=" + scan.count(VanillaVillageFeatureType.DOOR)
            + ", farmland=" + scan.count(VanillaVillageFeatureType.FARMLAND));
        for (String warning : scan.warnings()) {
            plugin.getMessageUtils().send(sender, "&eWarning: &f" + warning);
        }
    }

    private boolean handleWorldRegion(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.getMessageUtils().send(sender,
                "&cUtilizare: /ainpc world region <info|create> ...");
            return true;
        }

        WorldAdminApi worldAdmin = plugin.getPlatform().getWorldAdmin();
        if (!worldAdmin.isEnabled()) {
            plugin.getMessageUtils().send(sender, "&cWorld admin este dezactivat.");
            return true;
        }

        String action = args[2].toLowerCase();
        if ("create".equals(action)) {
            return handleWorldRegionCreate(sender, args);
        }
        if (!"info".equals(action) || args.length < 4) {
            plugin.getMessageUtils().send(sender, "&cUtilizare: /ainpc world region info <regionId>");
            plugin.getMessageUtils().send(sender, "&cUtilizare: /ainpc world region create <id> <type> <x1> <y1> <z1> <x2> <y2> <z2>");
            return true;
        }

        List<WorldRegionInfo> matches = findRegionMatches(worldAdmin, args[3]);
        if (matches.isEmpty()) {
            plugin.getMessageUtils().send(sender, "&cRegiunea &e" + args[3] + " &cnu a fost gasita.");
            return true;
        }
        if (matches.size() > 1) {
            plugin.getMessageUtils().send(sender, "&cSelector ambiguu pentru regiune. Foloseste ID-ul complet.");
            plugin.getMessageUtils().send(sender, "&7Potriviri: &f" + formatList(matches.stream().map(WorldRegionInfo::id).toList()));
            return true;
        }

        WorldRegionInfo region = matches.get(0);
        List<WorldPlaceInfo> places = worldAdmin.getPlaces(region.id()).stream()
            .sorted(Comparator.comparing(WorldPlaceInfo::id))
            .toList();
        List<WorldNodeInfo> nodes = worldAdmin.getNodes(region.id()).stream()
            .sorted(Comparator.comparing(WorldNodeInfo::id))
            .toList();

        plugin.getMessageUtils().send(sender, "&6=== World Region Info ===");
        plugin.getMessageUtils().send(sender, "&eID: &f" + region.id());
        plugin.getMessageUtils().send(sender, "&eNume: &f" + region.name());
        plugin.getMessageUtils().send(sender, "&eLume: &f" + region.worldName());
        plugin.getMessageUtils().send(sender, "&eTip: &f" + region.typeId());
        plugin.getMessageUtils().send(sender, "&eBounds: &f" + formatBounds(region.minX(), region.minY(), region.minZ(),
            region.maxX(), region.maxY(), region.maxZ()));
        plugin.getMessageUtils().send(sender, "&eTag-uri: &f" + formatList(region.tags()));
        plugin.getMessageUtils().send(sender, "&eStory mode: &f" + region.storyMode().getId());
        plugin.getMessageUtils().send(sender, "&eStory state: &f" + region.storyStateKey());
        plugin.getMessageUtils().send(sender, "&eStory pool: &f" + formatList(region.storyPool()));
        plugin.getMessageUtils().send(sender, "&ePlaces: &f" + places.size());
        plugin.getMessageUtils().send(sender, "&eNodes: &f" + nodes.size());
        if (!places.isEmpty()) {
            plugin.getMessageUtils().send(sender, "&ePlace IDs: &f" + formatList(places.stream().map(WorldPlaceInfo::id).toList()));
        }

        return true;
    }

    private boolean handleWorldPlace(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.getMessageUtils().send(sender,
                "&cUtilizare: /ainpc world place <info|create> ...");
            return true;
        }

        WorldAdminApi worldAdmin = plugin.getPlatform().getWorldAdmin();
        if (!worldAdmin.isEnabled()) {
            plugin.getMessageUtils().send(sender, "&cWorld admin este dezactivat.");
            return true;
        }

        String action = args[2].toLowerCase();
        if ("create".equals(action)) {
            return handleWorldPlaceCreate(sender, args);
        }
        if (!"info".equals(action) || args.length < 4) {
            plugin.getMessageUtils().send(sender, "&cUtilizare: /ainpc world place info <placeId>");
            plugin.getMessageUtils().send(sender, "&cUtilizare: /ainpc world place create <regionId> <id> <type> <x1> <y1> <z1> <x2> <y2> <z2>");
            return true;
        }

        List<WorldPlaceInfo> matches = findPlaceMatches(worldAdmin, args[3]);
        if (matches.isEmpty()) {
            plugin.getMessageUtils().send(sender, "&cPlace-ul &e" + args[3] + " &cnu a fost gasit.");
            return true;
        }
        if (matches.size() > 1) {
            plugin.getMessageUtils().send(sender, "&cSelector ambiguu pentru place. Foloseste ID-ul complet.");
            plugin.getMessageUtils().send(sender, "&7Potriviri: &f" + formatList(matches.stream().map(WorldPlaceInfo::id).toList()));
            return true;
        }

        WorldPlaceInfo place = matches.get(0);
        List<WorldNodeInfo> nodes = worldAdmin.getNodesForPlace(place.id()).stream()
            .sorted(Comparator.comparing(WorldNodeInfo::id))
            .toList();

        plugin.getMessageUtils().send(sender, "&6=== World Place Info ===");
        plugin.getMessageUtils().send(sender, "&eID: &f" + place.id());
        plugin.getMessageUtils().send(sender, "&eNume: &f" + place.displayName());
        plugin.getMessageUtils().send(sender, "&eRegiune: &f" + place.regionId());
        plugin.getMessageUtils().send(sender, "&eLume: &f" + place.worldName());
        plugin.getMessageUtils().send(sender, "&eTip: &f" + place.placeType().getId());
        plugin.getMessageUtils().send(sender, "&eBounds: &f" + formatBounds(place.minX(), place.minY(), place.minZ(),
            place.maxX(), place.maxY(), place.maxZ()));
        plugin.getMessageUtils().send(sender, "&eTag-uri: &f" + formatList(place.tags()));
        plugin.getMessageUtils().send(sender, "&eOwner NPC: &f" + formatOptional(place.ownerNpcId()));
        plugin.getMessageUtils().send(sender, "&ePublic access: &f" + (place.publicAccess() ? "da" : "nu"));
        plugin.getMessageUtils().send(sender, "&eMetadata: &f" + formatMap(place.metadata()));
        plugin.getMessageUtils().send(sender, "&eNodes: &f" + nodes.size());
        if (!nodes.isEmpty()) {
            plugin.getMessageUtils().send(sender, "&eNode IDs: &f" + formatList(nodes.stream().map(WorldNodeInfo::id).toList()));
        }

        return true;
    }

    private boolean handleWorldNode(CommandSender sender, String[] args) {
        if (args.length < 3 || !"create".equalsIgnoreCase(args[2])) {
            plugin.getMessageUtils().send(sender,
                "&cUtilizare: /ainpc world node create <regionId> <placeId|-> <id> <type> <x> <y> <z> [radius]");
            return true;
        }

        WorldAdminApi worldAdmin = plugin.getPlatform().getWorldAdmin();
        if (!worldAdmin.isEnabled()) {
            plugin.getMessageUtils().send(sender, "&cWorld admin este dezactivat.");
            return true;
        }

        return handleWorldNodeCreate(sender, args);
    }

    private boolean handleWorldSave(CommandSender sender) {
        WorldAdminService worldAdmin = plugin.getPlatform().getWorldAdminService();
        if (!worldAdmin.isEnabled()) {
            plugin.getMessageUtils().send(sender, "&cWorld admin este dezactivat.");
            return true;
        }

        if (!worldAdmin.hasUnsavedChanges()) {
            plugin.getMessageUtils().send(sender, "&7Nu exista modificari runtime de salvat.");
            return true;
        }

        worldAdmin.saveToConfig(plugin.getConfig());
        plugin.saveConfig();
        plugin.getMessageUtils().send(sender,
            "&aWorld admin salvat in config.yml: &f"
                + worldAdmin.getRegionCount() + " regiuni, "
                + worldAdmin.getPlaceCount() + " places, "
                + worldAdmin.getNodeCount() + " noduri&a.");
        return true;
    }

    private boolean handleWorldRegionCreate(CommandSender sender, String[] args) {
        if (args.length != 11) {
            plugin.getMessageUtils().send(sender,
                "&cUtilizare: /ainpc world region create <id> <type> <x1> <y1> <z1> <x2> <y2> <z2>");
            return true;
        }

        Player player = requirePlayerSender(sender);
        if (player == null) {
            return true;
        }

        RegionType regionType = parseRegionTypeStrict(args[4]);
        if (regionType == null) {
            plugin.getMessageUtils().send(sender, "&cTip de regiune invalid: &e" + args[4] + "&c.");
            return true;
        }

        Integer minX = parseIntegerStrict(args[5]);
        Integer minY = parseIntegerStrict(args[6]);
        Integer minZ = parseIntegerStrict(args[7]);
        Integer maxX = parseIntegerStrict(args[8]);
        Integer maxY = parseIntegerStrict(args[9]);
        Integer maxZ = parseIntegerStrict(args[10]);
        if (minX == null || minY == null || minZ == null || maxX == null || maxY == null || maxZ == null) {
            plugin.getMessageUtils().send(sender, "&cCoordonatele regiunii trebuie sa fie numere intregi.");
            return true;
        }

        try {
            WorldRegionInfo regionInfo = toRegionInfo(plugin.getPlatform().getWorldAdminService().createRegion(
                args[3],
                null,
                player.getWorld().getName(),
                regionType,
                minX,
                minY,
                minZ,
                maxX,
                maxY,
                maxZ
            ));
            plugin.getMessageUtils().send(sender,
                "&aRegiune creata: &f" + regionInfo.id() + " &7(" + regionInfo.name() + ")");
            plugin.getMessageUtils().send(sender,
                "&7Lume: &f" + regionInfo.worldName() + " &7| Bounds: &f"
                    + formatBounds(regionInfo.minX(), regionInfo.minY(), regionInfo.minZ(),
                    regionInfo.maxX(), regionInfo.maxY(), regionInfo.maxZ()));
            plugin.getMessageUtils().send(sender, "&7Ruleaza &f/ainpc world save &7ca sa persisti modificarile.");
        } catch (IllegalArgumentException exception) {
            plugin.getMessageUtils().send(sender, "&c" + exception.getMessage());
        }
        return true;
    }

    private boolean handleWorldPlaceCreate(CommandSender sender, String[] args) {
        if (args.length != 12) {
            plugin.getMessageUtils().send(sender,
                "&cUtilizare: /ainpc world place create <regionId> <id> <type> <x1> <y1> <z1> <x2> <y2> <z2>");
            return true;
        }

        WorldAdminApi worldAdmin = plugin.getPlatform().getWorldAdmin();
        List<WorldRegionInfo> regionMatches = findRegionMatches(worldAdmin, args[3]);
        if (regionMatches.isEmpty()) {
            plugin.getMessageUtils().send(sender, "&cRegiunea &e" + args[3] + " &cnu a fost gasita.");
            return true;
        }
        if (regionMatches.size() > 1) {
            plugin.getMessageUtils().send(sender, "&cSelector ambiguu pentru regiune. Foloseste ID-ul complet.");
            plugin.getMessageUtils().send(sender, "&7Potriviri: &f" + formatList(regionMatches.stream().map(WorldRegionInfo::id).toList()));
            return true;
        }

        PlaceType placeType = parsePlaceTypeStrict(args[5]);
        if (placeType == null) {
            plugin.getMessageUtils().send(sender, "&cTip de place invalid: &e" + args[5] + "&c.");
            return true;
        }

        Integer minX = parseIntegerStrict(args[6]);
        Integer minY = parseIntegerStrict(args[7]);
        Integer minZ = parseIntegerStrict(args[8]);
        Integer maxX = parseIntegerStrict(args[9]);
        Integer maxY = parseIntegerStrict(args[10]);
        Integer maxZ = parseIntegerStrict(args[11]);
        if (minX == null || minY == null || minZ == null || maxX == null || maxY == null || maxZ == null) {
            plugin.getMessageUtils().send(sender, "&cCoordonatele place-ului trebuie sa fie numere intregi.");
            return true;
        }

        WorldRegionInfo region = regionMatches.get(0);
        try {
            WorldPlaceInfo placeInfo = toPlaceInfo(plugin.getPlatform().getWorldAdminService().createPlace(
                region.id(),
                args[4],
                null,
                region.worldName(),
                placeType,
                minX,
                minY,
                minZ,
                maxX,
                maxY,
                maxZ
            ));
            plugin.getMessageUtils().send(sender,
                "&aPlace creat: &f" + placeInfo.id() + " &7(" + placeInfo.displayName() + ")");
            plugin.getMessageUtils().send(sender,
                "&7Regiune: &f" + placeInfo.regionId() + " &7| Bounds: &f"
                    + formatBounds(placeInfo.minX(), placeInfo.minY(), placeInfo.minZ(),
                    placeInfo.maxX(), placeInfo.maxY(), placeInfo.maxZ()));
            plugin.getMessageUtils().send(sender, "&7Ruleaza &f/ainpc world save &7ca sa persisti modificarile.");
        } catch (IllegalArgumentException exception) {
            plugin.getMessageUtils().send(sender, "&c" + exception.getMessage());
        }
        return true;
    }

    private boolean handleWorldNodeCreate(CommandSender sender, String[] args) {
        if (args.length < 10 || args.length > 11) {
            plugin.getMessageUtils().send(sender,
                "&cUtilizare: /ainpc world node create <regionId> <placeId|-> <id> <type> <x> <y> <z> [radius]");
            return true;
        }

        WorldAdminApi worldAdmin = plugin.getPlatform().getWorldAdmin();
        List<WorldRegionInfo> regionMatches = findRegionMatches(worldAdmin, args[3]);
        if (regionMatches.isEmpty()) {
            plugin.getMessageUtils().send(sender, "&cRegiunea &e" + args[3] + " &cnu a fost gasita.");
            return true;
        }
        if (regionMatches.size() > 1) {
            plugin.getMessageUtils().send(sender, "&cSelector ambiguu pentru regiune. Foloseste ID-ul complet.");
            plugin.getMessageUtils().send(sender, "&7Potriviri: &f" + formatList(regionMatches.stream().map(WorldRegionInfo::id).toList()));
            return true;
        }

        WorldNodeType nodeType = parseNodeTypeStrict(args[6]);
        if (nodeType == null) {
            plugin.getMessageUtils().send(sender, "&cTip de node invalid: &e" + args[6] + "&c.");
            return true;
        }

        Double x = parseDoubleStrict(args[7]);
        Double y = parseDoubleStrict(args[8]);
        Double z = parseDoubleStrict(args[9]);
        Double radius = args.length == 11 ? parseDoubleStrict(args[10]) : 2.5D;
        if (x == null || y == null || z == null || radius == null) {
            plugin.getMessageUtils().send(sender, "&cCoordonatele node-ului si raza trebuie sa fie numere.");
            return true;
        }

        WorldRegionInfo region = regionMatches.get(0);
        String placeSelector = args[4];
        String resolvedPlaceId = null;
        WorldPlaceInfo place = null;
        if (!isNoneSelector(placeSelector)) {
            List<WorldPlaceInfo> placeMatches = findPlaceMatches(worldAdmin, region.id(), placeSelector);
            if (placeMatches.isEmpty()) {
                plugin.getMessageUtils().send(sender, "&cPlace-ul &e" + placeSelector + " &cnu a fost gasit in regiunea &f" + region.id() + "&c.");
                return true;
            }
            if (placeMatches.size() > 1) {
                plugin.getMessageUtils().send(sender, "&cSelector ambiguu pentru place. Foloseste ID-ul complet.");
                plugin.getMessageUtils().send(sender, "&7Potriviri: &f" + formatList(placeMatches.stream().map(WorldPlaceInfo::id).toList()));
                return true;
            }
            place = placeMatches.get(0);
            resolvedPlaceId = place.id();
        }

        try {
            WorldNodeInfo nodeInfo = toNodeInfo(plugin.getPlatform().getWorldAdminService().createNode(
                region.id(),
                resolvedPlaceId,
                args[5],
                nodeType,
                place != null ? place.worldName() : region.worldName(),
                x,
                y,
                z,
                radius
            ));
            plugin.getMessageUtils().send(sender,
                "&aNode creat: &f" + nodeInfo.id() + " &7[" + nodeInfo.typeId() + "]");
            plugin.getMessageUtils().send(sender,
                "&7Regiune: &f" + nodeInfo.regionId()
                    + " &7| Place: &f" + formatOptional(nodeInfo.placeId())
                    + " &7| Pozitie: &f"
                    + String.format("%.1f, %.1f, %.1f", nodeInfo.x(), nodeInfo.y(), nodeInfo.z()));
            plugin.getMessageUtils().send(sender, "&7Ruleaza &f/ainpc world save &7ca sa persisti modificarile.");
        } catch (IllegalArgumentException exception) {
            plugin.getMessageUtils().send(sender, "&c" + exception.getMessage());
        }
        return true;
    }

    private void sendWorldUsage(CommandSender sender) {
        plugin.getMessageUtils().send(sender, "&cUtilizare:");
        plugin.getMessageUtils().send(sender, "&e/ainpc world whereami [jucator]");
        plugin.getMessageUtils().send(sender, "&e/ainpc world places [regionId]");
        plugin.getMessageUtils().send(sender, "&e/ainpc world region info <regionId>");
        plugin.getMessageUtils().send(sender, "&e/ainpc world region create <id> <type> <x1> <y1> <z1> <x2> <y2> <z2>");
        plugin.getMessageUtils().send(sender, "&e/ainpc world place info <placeId>");
        plugin.getMessageUtils().send(sender, "&e/ainpc world place create <regionId> <id> <type> <x1> <y1> <z1> <x2> <y2> <z2>");
        plugin.getMessageUtils().send(sender, "&e/ainpc world node create <regionId> <placeId|-> <id> <type> <x> <y> <z> [radius]");
        plugin.getMessageUtils().send(sender, "&e/ainpc world scan village [radius] [import] [regionId]");
        plugin.getMessageUtils().send(sender, "&e/ainpc world demo create [regionId]");
        plugin.getMessageUtils().send(sender, "&e/ainpc world bind npc <numeNpc|nearest> <homePlaceId> [workPlaceId|-] [socialPlaceId|-]");
        plugin.getMessageUtils().send(sender, "&e/ainpc world household <plan|spawn> <homePlaceId> [count]");
        plugin.getMessageUtils().send(sender, "&e/ainpc world settlement <plan|spawn> <regionId> [maxHouses]");
        plugin.getMessageUtils().send(sender, "&e/ainpc world save");
    }

    private boolean handleAudit(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ainpc.admin")) {
            plugin.getMessageUtils().sendMessage(sender, "no_permission");
            return true;
        }

        String mode = args.length > 1 ? args[1].toLowerCase() : "all";
        if (!Set.of("all", "npc", "world", "db", "spawn", "quest").contains(mode)) {
            sendAuditUsage(sender);
            return true;
        }

        AuditReport report = new AuditReport();
        if ("all".equals(mode) || "npc".equals(mode)) {
            auditNpcs(report);
        }
        if ("all".equals(mode) || "world".equals(mode)) {
            auditWorld(report);
        }
        if ("all".equals(mode) || "db".equals(mode)) {
            auditDatabase(report);
        }
        if ("all".equals(mode) || "spawn".equals(mode)) {
            auditSpawnOrder(report);
        }
        if ("all".equals(mode) || "quest".equals(mode)) {
            auditQuestAnchors(report);
        }

        sendAuditReport(sender, mode, report);
        return true;
    }

    private void sendAuditUsage(CommandSender sender) {
        plugin.getMessageUtils().send(sender, "&cUtilizare:");
        plugin.getMessageUtils().send(sender, "&e/ainpc audit &7- ruleaza toate verificarile");
        plugin.getMessageUtils().send(sender, "&e/ainpc audit npc &7- verifica profilurile si ancorele NPC");
        plugin.getMessageUtils().send(sender, "&e/ainpc audit world &7- verifica world mapping");
        plugin.getMessageUtils().send(sender, "&e/ainpc audit db &7- verifica tabelele si profile_data");
        plugin.getMessageUtils().send(sender, "&e/ainpc audit spawn &7- verifica ordinea casa/node/NPC/familie");
        plugin.getMessageUtils().send(sender, "&e/ainpc audit quest &7- verifica quest_anchor_bindings");
    }

    private boolean handleDebugDump(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ainpc.admin")) {
            plugin.getMessageUtils().sendMessage(sender, "no_permission");
            return true;
        }

        String scope = args.length > 1 ? args[1].toLowerCase() : "all";
        if (!Set.of("all", "npc", "world", "quest", "openai").contains(scope)) {
            plugin.getMessageUtils().send(sender, "&cUtilizare: /ainpc debugdump [all|npc|world|quest|openai]");
            return true;
        }

        try {
            DebugDumpService.DebugDumpResult result = new DebugDumpService(plugin).createDump(scope);
            plugin.getMessageUtils().send(sender, "&aDebug dump generat.");
            plugin.getMessageUtils().send(sender, "&eScope: &f" + result.scope());
            plugin.getMessageUtils().send(sender, "&eFolder: &f" + result.directory().toAbsolutePath());
        } catch (IOException exception) {
            plugin.getLogger().warning("Nu am putut genera debug dump: " + exception.getMessage());
            plugin.getMessageUtils().send(sender, "&cNu am putut genera debug dump: " + exception.getMessage());
        }

        return true;
    }

    private void auditNpcs(AuditReport report) {
        Collection<AINPC> npcs = plugin.getNpcManager().getAllNPCs();
        report.info("NPC-uri incarcate: " + npcs.size());
        if (npcs.isEmpty()) {
            report.warn("Nu exista NPC-uri incarcate in manager.");
            return;
        }

        Map<String, List<AINPC>> npcsByName = new HashMap<>();
        Map<Integer, List<AINPC>> npcsByDatabaseId = new HashMap<>();

        for (AINPC npc : npcs) {
            String label = auditNpcLabel(npc);
            if (npc.getDatabaseId() <= 0) {
                report.error(label + " nu are ID valid in baza de date.");
            } else {
                npcsByDatabaseId.computeIfAbsent(npc.getDatabaseId(), ignored -> new ArrayList<>()).add(npc);
            }

            if (npc.getUuid() == null) {
                report.error(label + " nu are UUID.");
            }

            if (npc.getName() == null || npc.getName().isBlank()) {
                report.error(label + " nu are nume.");
            } else {
                npcsByName.computeIfAbsent(normalizeAuditKey(npc.getName()), ignored -> new ArrayList<>()).add(npc);
            }

            if (npc.getWorldName() == null || npc.getWorldName().isBlank()) {
                report.error(label + " nu are lume setata.");
            } else if (plugin.getServer().getWorld(npc.getWorldName()) == null) {
                report.warn(label + " refera o lume neincarcata: " + npc.getWorldName() + ".");
            }

            if (npc.getOccupation() == null || npc.getOccupation().isBlank()) {
                report.warn(label + " nu are ocupatie.");
            }

            if (!npc.isProfileCreated()) {
                report.warn(label + " nu are profil persistent creat.");
            }

            if (npc.getProfileSource() == null || npc.getProfileSource().isBlank()) {
                report.warn(label + " nu are profile_source.");
            }

            validateProfileJson(report, label, npc.getProfileDataJson());

            if (npc.getHomeAnchor() == null) {
                report.warn(label + " nu are casa/homeAnchor.");
            } else {
                validateOwnedLocation(report, label + " homeAnchor", npc.getHomeAnchor());
            }

            if (npc.getWorkAnchor() == null) {
                report.warn(label + " nu are loc de munca/workAnchor.");
            } else {
                validateOwnedLocation(report, label + " workAnchor", npc.getWorkAnchor());
            }
        }

        for (Map.Entry<String, List<AINPC>> entry : npcsByName.entrySet()) {
            if (entry.getValue().size() > 1) {
                String duplicateIds = entry.getValue().stream()
                    .map(npc -> npc.getName() + "#" + npc.getDatabaseId())
                    .toList()
                    .toString();
                report.warn("Exista mai multi NPC cu acelasi nume normalizat: "
                    + entry.getKey() + " (" + entry.getValue().size() + "): " + duplicateIds
                    + ". Comenzile dupa nume pot selecta NPC-ul gresit.");
            }
        }

        for (Map.Entry<Integer, List<AINPC>> entry : npcsByDatabaseId.entrySet()) {
            if (entry.getValue().size() > 1) {
                report.error("Exista mai multi NPC cu acelasi database_id: " + entry.getKey() + ".");
            }
        }
    }

    private void auditWorld(AuditReport report) {
        WorldAdminApi worldAdmin = plugin.getPlatform().getWorldAdmin();
        if (worldAdmin == null || !worldAdmin.isEnabled()) {
            report.warn("World admin este dezactivat sau indisponibil.");
            return;
        }

        List<WorldRegionInfo> regions = new ArrayList<>(worldAdmin.getRegions());
        List<WorldPlaceInfo> places = new ArrayList<>(worldAdmin.getPlaces());
        List<WorldNodeInfo> nodes = new ArrayList<>(worldAdmin.getNodes());
        report.info("World mapping: " + regions.size() + " regiuni, " + places.size()
            + " places, " + nodes.size() + " nodes.");
        if (regions.isEmpty()) {
            report.warn("World admin este activ, dar mapping-ul nu are nicio regiune. NPC-urile si questurile folosesc doar fallback-uri de coordonate.");
        } else if (places.isEmpty()) {
            report.warn("World admin are regiuni, dar nu are places. Casele, locurile de munca si contextul semantic pentru NPC-uri sunt incomplete.");
        }

        Map<String, WorldRegionInfo> regionsById = new HashMap<>();
        for (WorldRegionInfo region : regions) {
            regionsById.put(region.id(), region);
            if (plugin.getServer().getWorld(region.worldName()) == null) {
                report.warn("Regiunea " + region.id() + " refera o lume neincarcata: " + region.worldName() + ".");
            }
            if (!validBounds(region.minX(), region.minY(), region.minZ(), region.maxX(), region.maxY(), region.maxZ())) {
                report.error("Regiunea " + region.id() + " are bounds invalide.");
            }
        }

        Map<String, WorldPlaceInfo> placesById = new HashMap<>();
        Set<String> ownerWarnings = new HashSet<>();
        for (WorldPlaceInfo place : places) {
            placesById.put(place.id(), place);
            WorldRegionInfo region = regionsById.get(place.regionId());
            if (region == null) {
                report.error("Place-ul " + place.id() + " refera regiunea inexistenta " + place.regionId() + ".");
            } else if (!placeInsideRegion(place, region)) {
                report.error("Place-ul " + place.id() + " nu este complet in regiunea " + region.id() + ".");
            }

            if (plugin.getServer().getWorld(place.worldName()) == null) {
                report.warn("Place-ul " + place.id() + " refera o lume neincarcata: " + place.worldName() + ".");
            }
            if (!validBounds(place.minX(), place.minY(), place.minZ(), place.maxX(), place.maxY(), place.maxZ())) {
                report.error("Place-ul " + place.id() + " are bounds invalide.");
            }
            if (place.placeType() == PlaceType.HOUSE && place.ownerNpcId().isBlank() && !hasPendingOwner(place)) {
                report.warn("Casa " + place.id() + " nu are owner_npc_id.");
            }
            if (!place.publicAccess() && place.ownerNpcId().isBlank() && !hasPendingOwner(place)) {
                report.warn("Place-ul privat " + place.id() + " nu are owner_npc_id.");
            }
            if (!place.ownerNpcId().isBlank() && !ownerMatchesLoadedNpc(place.ownerNpcId())) {
                String key = place.ownerNpcId() + "@" + place.id();
                if (ownerWarnings.add(key)) {
                    report.warn("Place-ul " + place.id() + " are owner_npc_id fara NPC incarcat potrivit: "
                        + place.ownerNpcId() + ".");
                }
            }
            if (isWorkplace(place) && worldAdmin.getNodesForPlace(place.id()).isEmpty()) {
                report.warn("Locul de munca " + place.id() + " nu are nodes de interactiune.");
            }
        }

        auditWorldReadiness(report, worldAdmin, places, nodes);

        for (int i = 0; i < places.size(); i++) {
            for (int j = i + 1; j < places.size(); j++) {
                WorldPlaceInfo left = places.get(i);
                WorldPlaceInfo right = places.get(j);
                if (left.regionId().equalsIgnoreCase(right.regionId()) && placesIntersect(left, right)) {
                    report.warn("Place-uri suprapuse in " + left.regionId() + ": "
                        + left.id() + " si " + right.id() + ".");
                }
            }
        }

        for (WorldNodeInfo node : nodes) {
            WorldRegionInfo region = regionsById.get(node.regionId());
            if (region == null) {
                report.error("Node-ul " + node.id() + " refera regiunea inexistenta " + node.regionId() + ".");
            }

            WorldPlaceInfo place = null;
            if (node.placeId() != null && !node.placeId().isBlank()) {
                place = placesById.get(node.placeId());
                if (place == null) {
                    report.error("Node-ul " + node.id() + " refera place-ul inexistent " + node.placeId() + ".");
                } else if (!place.regionId().equalsIgnoreCase(node.regionId())) {
                    report.error("Node-ul " + node.id() + " refera un place din alta regiune: " + node.placeId() + ".");
                }
            }

            if (node.radius() <= 0) {
                report.error("Node-ul " + node.id() + " are raza invalida: " + node.radius() + ".");
            }
            if (plugin.getServer().getWorld(node.worldName()) == null) {
                report.warn("Node-ul " + node.id() + " refera o lume neincarcata: " + node.worldName() + ".");
            }

            if (place != null && !pointInsidePlace(node, place)) {
                report.error("Node-ul " + node.id() + " nu este in interiorul place-ului " + place.id() + ".");
            } else if (place == null && region != null && !pointInsideRegion(node, region)) {
                report.error("Node-ul " + node.id() + " nu este in interiorul regiunii " + region.id() + ".");
            }
        }
    }

    private void auditWorldReadiness(AuditReport report,
                                     WorldAdminApi worldAdmin,
                                     List<WorldPlaceInfo> places,
                                     List<WorldNodeInfo> nodes) {
        if (places.isEmpty()) {
            return;
        }

        long houseCount = places.stream().filter(this::isHousePlace).count();
        long workplaceCount = places.stream().filter(this::isWorkplace).count();
        long socialPlaceCount = places.stream().filter(this::isSocialPlace).count();
        long questNodeCount = nodes.stream().filter(node -> nodeMatchesAny(node,
            "quest_trigger", "quest_board", "inspect_node", "interaction")).count();
        long bedNodeCount = nodes.stream().filter(node -> nodeMatchesAny(node, "bed")).count();
        long workNodeCount = nodes.stream().filter(node -> nodeMatchesAny(node,
            "work", "workstation", "work_anchor")).count();

        report.info("Mapping readiness: " + houseCount + " case, " + workplaceCount
            + " locuri de munca, " + socialPlaceCount + " locuri sociale, "
            + questNodeCount + " quest/interaction nodes.");

        if (houseCount == 0) {
            report.warn("Mapping-ul nu are nicio casa. Spawn-ul household si rutina home vor cadea pe fallback.");
        }
        if (bedNodeCount == 0) {
            report.warn("Mapping-ul nu are node-uri de tip bed. Casele nu pot fi validate bine pentru rezidenti.");
        }
        if (workplaceCount == 0 || workNodeCount == 0) {
            report.warn("Mapping-ul nu are locuri de munca cu node-uri work/workstation.");
        }
        if (socialPlaceCount == 0) {
            report.warn("Mapping-ul nu are loc social public pentru rutina sociala.");
        }
        if (questNodeCount == 0) {
            report.warn("Mapping-ul nu are quest/interaction nodes pentru obiective inspect_node.");
        }

        for (WorldPlaceInfo house : places.stream().filter(this::isHousePlace).toList()) {
            Collection<WorldNodeInfo> houseNodes = worldAdmin.getNodesForPlace(house.id());
            if (!hasAnySemanticNode(houseNodes, "bed", "home", "npc_spawn", "spawn")) {
                report.warn("Casa " + house.id() + " nu are node bed/home/npc_spawn.");
            }
        }
    }

    private void auditSpawnOrder(AuditReport report) {
        WorldAdminApi worldAdmin = plugin.getPlatform().getWorldAdmin();
        if (worldAdmin == null || !worldAdmin.isEnabled()) {
            report.warn("Audit spawn-order: World admin este dezactivat sau indisponibil.");
            return;
        }

        List<WorldPlaceInfo> places = new ArrayList<>(worldAdmin.getPlaces());
        List<WorldNodeInfo> nodes = new ArrayList<>(worldAdmin.getNodes());
        Collection<AINPC> npcs = plugin.getNpcManager().getAllNPCs();
        report.info("Spawn-order audit: " + npcs.size() + " NPC-uri, "
            + places.size() + " places, " + nodes.size() + " nodes.");

        for (WorldPlaceInfo place : places) {
            if (isHousePlace(place)) {
                auditHouseSpawnOrder(report, worldAdmin, place);
            }
        }

        for (AINPC npc : npcs) {
            auditNpcSpawnBindings(report, places, npc);
        }

        auditFamilyReciprocity(report);
    }

    private void auditHouseSpawnOrder(AuditReport report, WorldAdminApi worldAdmin, WorldPlaceInfo house) {
        List<String> residents = parseResidents(house);
        Integer maxResidents = parsePositiveIntMetadata(house, "max_residents", "maxResidents", "capacity");

        if (maxResidents == null) {
            report.warn("Casa " + house.id() + " nu are metadata.max_residents/capacity.");
        } else if (!residents.isEmpty() && residents.size() > maxResidents) {
            report.error("Casa " + house.id() + " are " + residents.size()
                + " rezidenti, peste max_residents=" + maxResidents + ".");
        }

        if (!residents.isEmpty() && !hasAnySemanticNode(worldAdmin.getNodesForPlace(house.id()),
            "bed", "home", "npc_spawn", "spawn")) {
            report.error("Casa " + house.id() + " are rezidenti, dar nu are node bed/home/npc_spawn.");
        }

        Set<Integer> seenResidentIds = new HashSet<>();
        for (String residentSelector : residents) {
            AINPC resident = findLoadedNpcBySelector(residentSelector);
            if (resident == null) {
                report.error("Casa " + house.id() + " contine resident necunoscut in metadata.residents: "
                    + residentSelector + ".");
                continue;
            }

            if (!seenResidentIds.add(resident.getDatabaseId())) {
                report.warn("Casa " + house.id() + " contine resident duplicat: "
                    + resident.getName() + "#" + resident.getDatabaseId() + ".");
            }

            if (resident.getHomeAnchor() == null) {
                report.error(auditNpcLabel(resident) + " este listed resident in " + house.id()
                    + ", dar nu are homeAnchor.");
            } else if (!ownedLocationInsidePlace(resident.getHomeAnchor(), house)) {
                report.error(auditNpcLabel(resident) + " este listed resident in " + house.id()
                    + ", dar homeAnchor nu este in interiorul casei.");
            }
        }
    }

    private void auditNpcSpawnBindings(AuditReport report, List<WorldPlaceInfo> places, AINPC npc) {
        String label = auditNpcLabel(npc);
        if (requiresWorkAnchor(npc.getOccupation()) && npc.getWorkAnchor() == null) {
            report.warn(label + " are ocupatie '" + npc.getOccupation() + "', dar nu are workAnchor.");
        }

        if (npc.getHomeAnchor() != null) {
            WorldPlaceInfo homePlace = findPlaceContainingOwnedLocation(places, npc.getHomeAnchor());
            if (homePlace != null && !isHousePlace(homePlace)) {
                report.warn(label + " are homeAnchor intr-un place care nu este casa: " + homePlace.id() + ".");
            }
        }

        if (npc.getWorkAnchor() != null) {
            WorldPlaceInfo workPlace = findPlaceContainingOwnedLocation(places, npc.getWorkAnchor());
            if (workPlace == null) {
                report.warn(label + " are workAnchor in afara oricarui mapped place.");
            } else if (!isWorkplace(workPlace)) {
                report.error(label + " are workAnchor in " + workPlace.id()
                    + ", dar place-ul nu este workplace compatibil.");
            }
        }
    }

    private void auditFamilyReciprocity(AuditReport report) {
        if (plugin.getDatabaseManager() == null) {
            report.warn("Audit familie: DatabaseManager indisponibil.");
            return;
        }

        try {
            auditQueryRows(report, """
                SELECT npc_id, related_npc_id, relation_type, COUNT(*) AS duplicate_count
                FROM npc_family
                WHERE related_npc_id IS NOT NULL
                GROUP BY npc_id, related_npc_id, relation_type
                HAVING COUNT(*) > 1
                """, "Relatie familie duplicata");

            Set<String> relationDirections = new HashSet<>();
            Map<String, String> relationLabels = new HashMap<>();
            try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement("""
                     SELECT npc_id, related_npc_id, related_name, relation_type
                     FROM npc_family
                     WHERE related_npc_id IS NOT NULL
                     """);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int npcId = rs.getInt("npc_id");
                    int relatedNpcId = rs.getInt("related_npc_id");
                    String key = npcId + "->" + relatedNpcId;
                    relationDirections.add(key);
                    relationLabels.put(key, "npc_id=" + npcId
                        + " -> related_npc_id=" + relatedNpcId
                        + " (" + rs.getString("relation_type")
                        + " " + rs.getString("related_name") + ")");
                }
            }

            for (String direction : relationDirections) {
                String[] parts = direction.split("->", 2);
                if (parts.length != 2) {
                    continue;
                }
                String reverse = parts[1] + "->" + parts[0];
                if (!relationDirections.contains(reverse)) {
                    report.error("Relatie familie fara reciproca: " + relationLabels.get(direction) + ".");
                }
            }
        } catch (SQLException exception) {
            report.error("Audit familie/spawn-order esuat: " + exception.getMessage());
        }
    }

    private void auditQuestAnchors(AuditReport report) {
        if (plugin.getDatabaseManager() == null) {
            report.error("DatabaseManager nu este initializat.");
            return;
        }

        try {
            int questRows = queryCount("SELECT COUNT(*) FROM player_quests");
            int anchorRows = queryCount("SELECT COUNT(*) FROM quest_anchor_bindings");
            report.info("Quest anchors: " + anchorRows + " binding-uri, " + questRows + " progres quest in DB.");

            auditQueryRows(report, """
                SELECT b.player_uuid, b.template_id, b.objective_key
                FROM quest_anchor_bindings b
                LEFT JOIN player_quests p
                  ON p.player_uuid = b.player_uuid AND p.template_id = b.template_id
                WHERE p.player_uuid IS NULL
                """, "Quest anchor fara progres parinte");

            List<QuestAnchorBindingRow> rows = queryQuestAnchorBindings("", "", 500);
            if (anchorRows > rows.size()) {
                report.warn("Audit quest anchors a verificat primele " + rows.size()
                    + " randuri din " + anchorRows + ". Ruleaza inspectie DB pentru audit complet.");
            }
            validateQuestAnchorRows(report, rows);
        } catch (SQLException exception) {
            report.error("Audit quest anchors esuat: " + exception.getMessage());
        }
    }

    private void validateQuestAnchorRows(AuditReport report, List<QuestAnchorBindingRow> rows) {
        WorldAdminApi worldAdmin = plugin.getPlatform() != null ? plugin.getPlatform().getWorldAdmin() : null;
        Map<String, WorldRegionInfo> regionsById = new HashMap<>();
        Map<String, WorldPlaceInfo> placesById = new HashMap<>();
        Map<String, WorldNodeInfo> nodesById = new HashMap<>();
        if (worldAdmin != null && worldAdmin.isEnabled()) {
            for (WorldRegionInfo region : worldAdmin.getRegions()) {
                regionsById.put(region.id(), region);
            }
            for (WorldPlaceInfo place : worldAdmin.getPlaces()) {
                placesById.put(place.id(), place);
            }
            for (WorldNodeInfo node : worldAdmin.getNodes()) {
                nodesById.put(node.id(), node);
            }
        }

        Map<String, Integer> countsByAnchorType = new HashMap<>();
        for (QuestAnchorBindingRow row : rows) {
            String label = "Quest anchor " + row.templateId() + "/" + row.objectiveKey()
                + " pentru player " + row.playerUuid();
            String anchorType = normalizeAuditKey(row.anchorType());
            countsByAnchorType.merge(anchorType.isBlank() ? "<gol>" : anchorType, 1, Integer::sum);

            if (row.playerUuid() == null || row.playerUuid().isBlank()) {
                report.error(label + " nu are player_uuid.");
            } else {
                try {
                    UUID.fromString(row.playerUuid());
                } catch (IllegalArgumentException exception) {
                    report.error(label + " are player_uuid invalid: " + row.playerUuid() + ".");
                }
            }
            if (row.templateId() == null || row.templateId().isBlank()) {
                report.error(label + " nu are template_id.");
            }
            if (row.objectiveKey() == null || row.objectiveKey().isBlank()) {
                report.error(label + " nu are objective_key.");
            }
            if (row.objectiveType() == null || row.objectiveType().isBlank()) {
                report.error(label + " nu are objective_type.");
            }
            if (row.anchorType() == null || row.anchorType().isBlank()) {
                report.error(label + " nu are anchor_type.");
            }
            if (row.anchorId() == null || row.anchorId().isBlank()) {
                report.error(label + " nu are anchor_id.");
            }
            if (!isQuestAnchorTypeCompatible(row.objectiveType(), row.anchorType())) {
                report.error(label + " are tip incompatibil: objective_type=" + row.objectiveType()
                    + ", anchor_type=" + row.anchorType() + ".");
            }
            validateQuestAnchorTarget(report, label, row, worldAdmin, regionsById, placesById, nodesById);
        }

        if (!countsByAnchorType.isEmpty()) {
            report.info("Quest anchors pe tip: " + formatCountMap(countsByAnchorType));
        }
    }

    private void validateQuestAnchorTarget(AuditReport report,
                                           String label,
                                           QuestAnchorBindingRow row,
                                           WorldAdminApi worldAdmin,
                                           Map<String, WorldRegionInfo> regionsById,
                                           Map<String, WorldPlaceInfo> placesById,
                                           Map<String, WorldNodeInfo> nodesById) {
        String anchorType = normalizeAuditKey(row.anchorType());
        String anchorId = row.anchorId() == null ? "" : row.anchorId();
        if (anchorId.isBlank()) {
            return;
        }

        switch (anchorType) {
            case "region" -> {
                if (worldAdmin == null || !worldAdmin.isEnabled()) {
                    report.warn(label + " nu poate valida region anchor: World admin este dezactivat.");
                } else if (!regionsById.containsKey(anchorId)) {
                    report.error(label + " refera regiunea inexistenta " + anchorId + ".");
                }
            }
            case "place" -> {
                if (worldAdmin == null || !worldAdmin.isEnabled()) {
                    report.warn(label + " nu poate valida place anchor: World admin este dezactivat.");
                } else if (!placesById.containsKey(anchorId)) {
                    report.error(label + " refera place-ul inexistent " + anchorId + ".");
                }
            }
            case "node" -> {
                if (worldAdmin == null || !worldAdmin.isEnabled()) {
                    report.warn(label + " nu poate valida node anchor: World admin este dezactivat.");
                } else if (!nodesById.containsKey(anchorId)) {
                    report.error(label + " refera node-ul inexistent " + anchorId + ".");
                }
            }
            case "npc" -> {
                if (findLoadedNpcBySelector(anchorId) == null) {
                    report.warn(label + " refera NPC care nu este incarcat acum: " + anchorId + ".");
                }
            }
            default -> report.error(label + " are anchor_type necunoscut: " + row.anchorType() + ".");
        }
    }

    private boolean isQuestAnchorTypeCompatible(String objectiveType, String anchorType) {
        String objective = normalizeAuditKey(objectiveType);
        String anchor = normalizeAuditKey(anchorType);
        if (objective.isBlank() || anchor.isBlank()) {
            return true;
        }

        return switch (objective) {
            case "visit_region" -> "region".equals(anchor);
            case "visit_place" -> "place".equals(anchor);
            case "inspect_node" -> "node".equals(anchor);
            case "talk_to_npc" -> "npc".equals(anchor);
            default -> true;
        };
    }

    private void auditDatabase(AuditReport report) {
        if (plugin.getDatabaseManager() == null) {
            report.error("DatabaseManager nu este initializat.");
            return;
        }

        try {
            int npcRows = queryCount("SELECT COUNT(*) FROM npcs");
            int profileRows = queryCount("SELECT COUNT(*) FROM npc_profiles");
            report.info("DB: " + npcRows + " randuri in npcs, " + profileRows + " randuri in npc_profiles.");

            auditQueryRows(report, """
                SELECT n.id, n.name
                FROM npcs n
                LEFT JOIN npc_profiles p ON n.id = p.npc_id
                WHERE p.npc_id IS NULL
                """, "NPC fara profil persistent");

            auditQueryRows(report, """
                SELECT p.npc_id, p.profile_source
                FROM npc_profiles p
                LEFT JOIN npcs n ON n.id = p.npc_id
                WHERE n.id IS NULL
                """, "Profil orfan fara NPC");

            auditQueryRows(report, """
                SELECT id, name
                FROM npcs
                WHERE uuid IS NULL OR uuid = '' OR name IS NULL OR name = '' OR world IS NULL OR world = ''
                """, "NPC cu campuri DB obligatorii lipsa");

            auditQueryRows(report, """
                SELECT uuid, COUNT(*) AS duplicate_count
                FROM npcs
                GROUP BY uuid
                HAVING COUNT(*) > 1
                """, "UUID duplicat in DB");

            try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement(
                     "SELECT npc_id, profile_source, profile_data FROM npc_profiles");
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int npcId = rs.getInt("npc_id");
                    String source = rs.getString("profile_source");
                    String profileData = rs.getString("profile_data");
                    if (source == null || source.isBlank()) {
                        report.warn("Profilul DB pentru npc_id=" + npcId + " nu are profile_source.");
                    }
                    validateProfileJson(report, "Profil DB npc_id=" + npcId, profileData);
                }
            }
        } catch (SQLException exception) {
            report.error("Audit DB esuat: " + exception.getMessage());
        }
    }

    private void sendAuditReport(CommandSender sender, String mode, AuditReport report) {
        plugin.getMessageUtils().send(sender, "&6=== AINPC Audit: " + mode + " ===");
        plugin.getMessageUtils().send(sender, "&eRezultat: &c" + report.errors.size() + " erori"
            + " &7| &e" + report.warnings.size() + " warning-uri");

        for (String info : report.infos) {
            plugin.getMessageUtils().send(sender, "&7- " + info);
        }

        sendAuditMessages(sender, "&cErori", report.errors);
        sendAuditMessages(sender, "&eWarning-uri", report.warnings);

        if (report.errors.isEmpty() && report.warnings.isEmpty()) {
            plugin.getMessageUtils().send(sender, "&aAudit curat. Nu au fost gasite probleme evidente.");
        } else {
            plugin.getMessageUtils().send(sender, "&7Auditul este read-only. Nu a modificat date sau config.");
        }
    }

    private void sendAuditMessages(CommandSender sender, String title, List<String> messages) {
        if (messages.isEmpty()) {
            return;
        }

        plugin.getMessageUtils().send(sender, title + " &7(" + messages.size() + "):");
        int limit = Math.min(AUDIT_PREVIEW_LIMIT, messages.size());
        for (int i = 0; i < limit; i++) {
            plugin.getMessageUtils().send(sender, "&7- &f" + messages.get(i));
        }
        if (messages.size() > limit) {
            plugin.getMessageUtils().send(sender, "&7... inca " + (messages.size() - limit)
                + " rezultate. Ruleaza audituri mai specifice: npc/world/db/spawn.");
        }
    }

    private void validateProfileJson(AuditReport report, String label, String profileData) {
        if (profileData == null || profileData.isBlank()) {
            report.warn(label + " are profile_data gol.");
            return;
        }

        try {
            JsonParser.parseString(profileData);
        } catch (JsonSyntaxException exception) {
            report.error(label + " are profile_data JSON invalid: " + exception.getMessage());
        }
    }

    private void validateOwnedLocation(AuditReport report, String label, AINPC.OwnedLocation location) {
        if (location.worldName() == null || location.worldName().isBlank()) {
            report.error(label + " nu are lume.");
            return;
        }
        if (plugin.getServer().getWorld(location.worldName()) == null) {
            report.warn(label + " refera o lume neincarcata: " + location.worldName() + ".");
        }
        if (location.label() == null || location.label().isBlank()) {
            report.warn(label + " nu are label.");
        }
    }

    private int queryCount(String sql) throws SQLException {
        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private void auditQueryRows(AuditReport report, String sql, String label) throws SQLException {
        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                report.warn(label + ": " + describeCurrentRow(rs));
            }
        }
    }

    private String describeCurrentRow(ResultSet rs) throws SQLException {
        int columnCount = rs.getMetaData().getColumnCount();
        List<String> parts = new ArrayList<>();
        for (int i = 1; i <= columnCount; i++) {
            parts.add(rs.getMetaData().getColumnLabel(i) + "=" + rs.getString(i));
        }
        return String.join(", ", parts);
    }

    private boolean ownerMatchesLoadedNpc(String ownerNpcId) {
        if (ownerNpcId == null || ownerNpcId.isBlank()) {
            return false;
        }

        String owner = normalizeAuditKey(ownerNpcId);
        for (AINPC npc : plugin.getNpcManager().getAllNPCs()) {
            if (npc.getUuid() != null && owner.equalsIgnoreCase(npc.getUuid().toString())) {
                return true;
            }
            if (npc.getDatabaseId() > 0) {
                String id = String.valueOf(npc.getDatabaseId());
                if (owner.equals(id) || owner.equals("npc_" + id)) {
                    return true;
                }
            }
            String name = normalizeAuditKey(npc.getName());
            if (!name.isBlank() && (owner.equals(name) || owner.equals("npc_" + name))) {
                return true;
            }
        }
        return false;
    }

    private boolean isWorkplace(WorldPlaceInfo place) {
        return place.hasTag("work")
            || place.hasTag("workplace")
            || "work".equalsIgnoreCase(place.metadata().get("role"))
            || "work".equalsIgnoreCase(place.metadata().get("purpose"))
            || place.placeType() == PlaceType.FORGE
            || place.placeType() == PlaceType.SHOP
            || place.placeType() == PlaceType.FARM
            || place.placeType() == PlaceType.MARKET
            || place.placeType() == PlaceType.TAVERN;
    }

    private boolean isSocialPlace(WorldPlaceInfo place) {
        return place.hasTag("social")
            || place.hasTag("public")
            || "social".equalsIgnoreCase(place.metadata().get("role"))
            || "social".equalsIgnoreCase(place.metadata().get("purpose"))
            || place.placeType() == PlaceType.MARKET
            || place.placeType() == PlaceType.TAVERN
            || place.placeType() == PlaceType.CAMP;
    }

    private boolean hasPendingOwner(WorldPlaceInfo place) {
        String ownerStatus = place.metadata().getOrDefault("owner_status", "");
        String ownerPending = place.metadata().getOrDefault("owner_pending", "");
        return "pending".equalsIgnoreCase(ownerStatus)
            || "true".equalsIgnoreCase(ownerPending)
            || ("demo_mapping".equalsIgnoreCase(place.metadata().getOrDefault("source", ""))
                && place.hasTag("demo"));
    }

    private boolean placeInsideRegion(WorldPlaceInfo place, WorldRegionInfo region) {
        return place.worldName().equalsIgnoreCase(region.worldName())
            && place.minX() >= region.minX()
            && place.maxX() <= region.maxX()
            && place.minY() >= region.minY()
            && place.maxY() <= region.maxY()
            && place.minZ() >= region.minZ()
            && place.maxZ() <= region.maxZ();
    }

    private boolean pointInsidePlace(WorldNodeInfo node, WorldPlaceInfo place) {
        return node.worldName().equalsIgnoreCase(place.worldName())
            && node.x() >= place.minX()
            && node.x() <= place.maxX()
            && node.y() >= place.minY()
            && node.y() <= place.maxY()
            && node.z() >= place.minZ()
            && node.z() <= place.maxZ();
    }

    private boolean pointInsideRegion(WorldNodeInfo node, WorldRegionInfo region) {
        return node.worldName().equalsIgnoreCase(region.worldName())
            && node.x() >= region.minX()
            && node.x() <= region.maxX()
            && node.y() >= region.minY()
            && node.y() <= region.maxY()
            && node.z() >= region.minZ()
            && node.z() <= region.maxZ();
    }

    private boolean placesIntersect(WorldPlaceInfo left, WorldPlaceInfo right) {
        return left.worldName().equalsIgnoreCase(right.worldName())
            && overlaps(left.minX(), left.maxX(), right.minX(), right.maxX())
            && overlaps(left.minY(), left.maxY(), right.minY(), right.maxY())
            && overlaps(left.minZ(), left.maxZ(), right.minZ(), right.maxZ());
    }

    private boolean overlaps(int leftMin, int leftMax, int rightMin, int rightMax) {
        return leftMax >= rightMin && rightMax >= leftMin;
    }

    private boolean validBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return minX <= maxX && minY <= maxY && minZ <= maxZ;
    }

    private boolean isHousePlace(WorldPlaceInfo place) {
        return place.placeType() == PlaceType.HOUSE
            || place.hasTag("home")
            || place.hasTag("house")
            || "home".equalsIgnoreCase(place.metadata().get("role"))
            || "home".equalsIgnoreCase(place.metadata().get("purpose"));
    }

    private List<String> parseResidents(WorldPlaceInfo place) {
        String rawResidents = firstNonBlank(
            place.metadata().get("residents"),
            place.metadata().get("resident_npc_ids"),
            place.metadata().get("resident_ids")
        );
        if (rawResidents.isBlank()) {
            return List.of();
        }

        List<String> residents = new ArrayList<>();
        for (String part : rawResidents.split("[,;]")) {
            String resident = part.trim();
            if (!resident.isBlank()) {
                residents.add(resident);
            }
        }
        return residents;
    }

    private Integer parsePositiveIntMetadata(WorldPlaceInfo place, String... keys) {
        String rawValue = firstNonBlankFromMap(place.metadata(), keys);
        if (rawValue.isBlank()) {
            return null;
        }

        try {
            int value = Integer.parseInt(rawValue.trim());
            return value > 0 ? value : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String firstNonBlankFromMap(Map<String, String> values, String... keys) {
        for (String key : keys) {
            String value = values.get(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private boolean hasAnySemanticNode(Collection<WorldNodeInfo> nodes, String... expectedTokens) {
        for (WorldNodeInfo node : nodes) {
            if (nodeMatchesAny(node, expectedTokens)) {
                return true;
            }
        }
        return false;
    }

    private boolean nodeMatchesAny(WorldNodeInfo node, String... expectedTokens) {
        if (matchesAnyToken(node.typeId(), expectedTokens)) {
            return true;
        }
        for (Map.Entry<String, String> entry : node.metadata().entrySet()) {
            if (matchesAnyToken(entry.getKey(), expectedTokens) || matchesAnyToken(entry.getValue(), expectedTokens)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesAnyToken(String rawValue, String... expectedTokens) {
        String value = normalizeAuditKey(rawValue).replace('-', '_');
        if (value.isBlank()) {
            return false;
        }
        for (String expectedToken : expectedTokens) {
            if (value.equals(normalizeAuditKey(expectedToken).replace('-', '_'))) {
                return true;
            }
        }
        return false;
    }

    private AINPC findLoadedNpcBySelector(String selector) {
        String normalizedSelector = normalizeAuditKey(selector);
        if (normalizedSelector.isBlank()) {
            return null;
        }

        for (AINPC npc : plugin.getNpcManager().getAllNPCs()) {
            if (npc.getUuid() != null && normalizedSelector.equalsIgnoreCase(npc.getUuid().toString())) {
                return npc;
            }
            if (npc.getDatabaseId() > 0) {
                String id = String.valueOf(npc.getDatabaseId());
                if (normalizedSelector.equals(id) || normalizedSelector.equals("npc_" + id)) {
                    return npc;
                }
            }
            String npcName = normalizeAuditKey(npc.getName());
            if (!npcName.isBlank() && (normalizedSelector.equals(npcName) || normalizedSelector.equals("npc_" + npcName))) {
                return npc;
            }
        }

        return null;
    }

    private boolean ownedLocationInsidePlace(AINPC.OwnedLocation location, WorldPlaceInfo place) {
        return location != null
            && place.worldName().equalsIgnoreCase(location.worldName())
            && location.x() >= place.minX()
            && location.x() <= place.maxX()
            && location.y() >= place.minY()
            && location.y() <= place.maxY()
            && location.z() >= place.minZ()
            && location.z() <= place.maxZ();
    }

    private WorldPlaceInfo findPlaceContainingOwnedLocation(List<WorldPlaceInfo> places, AINPC.OwnedLocation location) {
        if (location == null) {
            return null;
        }

        return places.stream()
            .filter(place -> ownedLocationInsidePlace(location, place))
            .findFirst()
            .orElse(null);
    }

    private boolean requiresWorkAnchor(String occupation) {
        String normalized = normalizeAuditKey(occupation);
        return !normalized.isBlank()
            && !normalized.equals("locuitor")
            && !normalized.equals("localnic")
            && !normalized.equals("villager")
            && !normalized.equals("resident");
    }

    private String auditNpcLabel(AINPC npc) {
        String name = npc.getName() == null || npc.getName().isBlank() ? "<fara nume>" : npc.getName();
        return "NPC " + name + " (id=" + npc.getDatabaseId() + ")";
    }

    private String normalizeAuditKey(String value) {
        return value == null ? "" : value.trim().toLowerCase().replace(' ', '_');
    }

    private static final class AuditReport {
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private final List<String> infos = new ArrayList<>();

        private void error(String message) {
            errors.add(message);
        }

        private void warn(String message) {
            warnings.add(message);
        }

        private void info(String message) {
            infos.add(message);
        }
    }

    private record QuestAnchorBindingRow(
        String playerUuid,
        String templateId,
        String objectiveKey,
        String questCode,
        String objectiveType,
        String reference,
        String anchorType,
        String anchorId,
        String anchorLabel,
        long createdAt,
        long updatedAt,
        String status
    ) {
    }

    /**
     * /ainpc delete <nume>
     */
    private boolean handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ainpc.admin")) {
            plugin.getMessageUtils().sendMessage(sender, "no_permission");
            return true;
        }

        if (args.length < 2) {
            plugin.getMessageUtils().send(sender, "&cUtilizare: /ainpc delete <nume>");
            return true;
        }

        String name = args[1];
        AINPC npc = plugin.getNpcManager().getNPCByName(name);

        if (npc == null) {
            plugin.getMessageUtils().sendMessage(sender, "npc_not_found");
            return true;
        }

        if (plugin.getNpcManager().deleteNPC(npc)) {
            plugin.getMessageUtils().sendMessage(sender, "npc_deleted", Map.of("name", name));
        } else {
            plugin.getMessageUtils().send(sender, "&cEroare la stergerea NPC-ului!");
        }

        return true;
    }

    /**
     * /ainpc info [nume]
     */
    private boolean handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ainpc.info")) {
            plugin.getMessageUtils().sendMessage(sender, "no_permission");
            return true;
        }

        AINPC npc;
        
        if (args.length < 2) {
            // Gaseste NPC-ul cel mai apropiat
            if (!(sender instanceof Player player)) {
                plugin.getMessageUtils().send(sender, "&cSpecifica numele NPC-ului!");
                return true;
            }
            
            List<AINPC> nearby = plugin.getNpcManager().getNPCsNear(player.getLocation(), 10);
            if (nearby.isEmpty()) {
                plugin.getMessageUtils().send(sender, "&cNu exista NPC-uri in apropiere!");
                return true;
            }
            npc = nearby.get(0);
        } else {
            npc = plugin.getNpcManager().getNPCByName(args[1]);
        }

        if (npc == null) {
            plugin.getMessageUtils().sendMessage(sender, "npc_not_found");
            return true;
        }

        npc.updateContext();

        // Afiseaza informatii
        plugin.getMessageUtils().send(sender, "&6=== Informatii NPC ===");
        plugin.getMessageUtils().send(sender, "&eNume: &f" + npc.getName());
        plugin.getMessageUtils().send(sender, "&eID: &f" + npc.getDatabaseId());
        plugin.getMessageUtils().send(sender, "&eVarsta: &f" + npc.getAge() + " ani");
        plugin.getMessageUtils().send(sender, "&eGen: &f" + (npc.getGender().equals("male") ? "Barbat" : "Femeie"));
        
        if (npc.getOccupation() != null) {
            plugin.getMessageUtils().send(sender, "&eOcupatie: &f" + npc.getOccupation());
        }
        
        plugin.getMessageUtils().send(sender, "&eLocatie: &f" + formatLocation(npc.getLocation()));
        if (npc.getContext() != null && npc.getContext().getTopologyCategory() != null) {
            plugin.getMessageUtils().send(sender,
                "&eTopologie: &f" + npc.getContext().getTopologyCategory().getDisplayName());
        }
        plugin.getMessageUtils().send(sender, "");
        plugin.getMessageUtils().send(sender, "&ePersonalitate: &f" + npc.getPersonality().getDominantTraits());
        plugin.getMessageUtils().send(sender, "&eEmotie: &f" + npc.getEmotions().getShortDescription());
        plugin.getMessageUtils().send(sender, "&eProfil creat: &f" + (npc.isProfileCreated() ? "da" : "nu"));
        plugin.getMessageUtils().send(sender, "&eSursa profil: &f" + npc.getProfileSource());
        if (npc.getProfileSummary() != null && !npc.getProfileSummary().isBlank()) {
            plugin.getMessageUtils().send(sender, "&eRezumat profil: &f" + npc.getProfileSummary());
        }
        
        if (npc.getBackstory() != null) {
            plugin.getMessageUtils().send(sender, "");
            plugin.getMessageUtils().send(sender, "&ePoveste: &f" + npc.getBackstory());
        }

        return true;
    }

    /**
     * /ainpc list
     */
    private boolean handleList(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ainpc.admin")) {
            plugin.getMessageUtils().sendMessage(sender, "no_permission");
            return true;
        }

        var npcs = plugin.getNpcManager().getAllNPCs();
        
        if (npcs.isEmpty()) {
            plugin.getMessageUtils().send(sender, "&7Nu exista NPC-uri create.");
            return true;
        }

        plugin.getMessageUtils().send(sender, "&6=== Lista NPC-uri (" + npcs.size() + ") ===");
        
        for (AINPC npc : npcs) {
            String emotionColor = npc.getEmotions().getDominantEmotionColor();
            String status = npc.isSpawned() ? "&a[ACTIV]" : "&c[INACTIV]";
            
            plugin.getMessageUtils().send(sender, 
                status + " " + emotionColor + npc.getName() + 
                " &7- " + (npc.getOccupation() != null ? npc.getOccupation() : "fara ocupatie") +
                " &8(ID: " + npc.getDatabaseId() + ")"
            );
        }

        return true;
    }

    /**
     * /ainpc family <nume>
     */
    private boolean handleFamily(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ainpc.info")) {
            plugin.getMessageUtils().sendMessage(sender, "no_permission");
            return true;
        }

        if (args.length < 2) {
            plugin.getMessageUtils().send(sender, "&cUtilizare: /ainpc family <nume>");
            return true;
        }

        AINPC npc = plugin.getNpcManager().getNPCByName(args[1]);
        if (npc == null) {
            plugin.getMessageUtils().sendMessage(sender, "npc_not_found");
            return true;
        }

        String report = plugin.getFamilyManager().getFamilyReport(npc);
        plugin.getMessageUtils().send(sender, report);

        return true;
    }

    private boolean handleRoutine(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ainpc.admin")) {
            plugin.getMessageUtils().sendMessage(sender, "no_permission");
            return true;
        }

        if (args.length < 2) {
            sendRoutineUsage(sender);
            return true;
        }

        String action = args[1].toLowerCase();
        return switch (action) {
            case "tick" -> handleRoutineTick(sender);
            case "status" -> handleRoutineStatus(sender, args);
            default -> {
                sendRoutineUsage(sender);
                yield true;
            }
        };
    }

    private boolean handleRoutineTick(CommandSender sender) {
        RoutineTickSummary summary = plugin.getRoutineService().runRoutineTick();
        if (!summary.enabled()) {
            plugin.getMessageUtils().send(sender, "&eRoutine service este dezactivat in config.");
            return true;
        }

        plugin.getMessageUtils().send(sender, "&6=== Routine Tick ===");
        plugin.getMessageUtils().send(sender, "&eNPC total: &f" + summary.totalNpcs());
        plugin.getMessageUtils().send(sender, "&eEvaluati: &f" + summary.evaluatedNpcs());
        plugin.getMessageUtils().send(sender, "&eMutati: &f" + summary.movedNpcs());
        plugin.getMessageUtils().send(sender, "&eSkip busy: &f" + summary.skippedBusy());
        plugin.getMessageUtils().send(sender, "&eSkip fara tinta: &f" + summary.skippedMissingTarget());
        plugin.getMessageUtils().send(sender, "&eSkip tinta invalida: &f" + summary.skippedInvalidTarget());
        return true;
    }

    private boolean handleRoutineStatus(CommandSender sender, String[] args) {
        AINPC npc;
        if (args.length >= 3) {
            npc = plugin.getNpcManager().getNPCByName(args[2]);
        } else if (sender instanceof Player player) {
            List<AINPC> nearby = plugin.getNpcManager().getNPCsNear(player.getLocation(), 10);
            npc = nearby.isEmpty() ? null : nearby.get(0);
        } else {
            plugin.getMessageUtils().send(sender, "&cUtilizare: /ainpc routine status <numeNpc>");
            return true;
        }

        if (npc == null) {
            plugin.getMessageUtils().sendMessage(sender, "npc_not_found");
            return true;
        }

        RoutineAssignment assignment = plugin.getRoutineService().preview(npc);
        plugin.getMessageUtils().send(sender, "&6=== Routine Status ===");
        plugin.getMessageUtils().send(sender, "&eNPC: &f" + npc.getName() + " &7(ID: " + npc.getDatabaseId() + ")");
        plugin.getMessageUtils().send(sender, "&eSlot: &f" + assignment.slot());
        plugin.getMessageUtils().send(sender, "&eActivitate: &f" + assignment.activity());
        plugin.getMessageUtils().send(sender, "&eGoal: &f" + assignment.goal());
        plugin.getMessageUtils().send(sender, "&eStare tinta: &f" + assignment.targetState().name());
        plugin.getMessageUtils().send(sender, "&eTinta: &f" + formatOwnedLocation(assignment.targetAnchor()));
        plugin.getMessageUtils().send(sender, "&eRutina curenta salvata: &f" + formatOptional(npc.getPlannedRoutineActivity()));
        plugin.getMessageUtils().send(sender, "&eObiectiv curent: &f" + formatOptional(npc.getCurrentGoal()));
        return true;
    }

    private void sendRoutineUsage(CommandSender sender) {
        plugin.getMessageUtils().send(sender, "&cUtilizare:");
        plugin.getMessageUtils().send(sender, "&e/ainpc routine tick &7- ruleaza manual rutina pentru NPC-urile active");
        plugin.getMessageUtils().send(sender, "&e/ainpc routine status [numeNpc] &7- previzualizeaza rutina unui NPC");
    }

    /**
     * /ainpc mood <nume> <emotie> [intensitate]
     */
    private boolean handleMood(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ainpc.admin")) {
            plugin.getMessageUtils().sendMessage(sender, "no_permission");
            return true;
        }

        if (args.length < 3) {
            plugin.getMessageUtils().send(sender, "&cUtilizare: /ainpc mood <nume> <emotie> [intensitate]");
            plugin.getMessageUtils().send(sender, "&7Emotii: happiness, sadness, anger, fear, surprise, disgust, trust, anticipation");
            return true;
        }

        AINPC npc = plugin.getNpcManager().getNPCByName(args[1]);
        if (npc == null) {
            plugin.getMessageUtils().sendMessage(sender, "npc_not_found");
            return true;
        }

        String emotion = args[2].toLowerCase();
        double intensity = args.length > 3 ? parseDouble(args[3], 0.7) : 0.7;
        intensity = Math.max(0.0, Math.min(1.0, intensity));

        plugin.getEmotionManager().setMood(npc, emotion, intensity);
        
        plugin.getMessageUtils().send(sender, "&aEmotia lui &e" + npc.getName() + 
            " &aa fost setata la &f" + emotion + " &7(" + String.format("%.0f%%", intensity * 100) + ")");

        return true;
    }

    /**
     * /ainpc tp <nume>
     */
    private boolean handleTeleport(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ainpc.admin")) {
            plugin.getMessageUtils().sendMessage(sender, "no_permission");
            return true;
        }

        if (!(sender instanceof Player player)) {
            plugin.getMessageUtils().send(sender, "&cAceasta comanda poate fi folosita doar de jucatori!");
            return true;
        }

        if (args.length < 2) {
            plugin.getMessageUtils().send(sender, "&cUtilizare: /ainpc tp <nume>");
            return true;
        }

        AINPC npc = plugin.getNpcManager().getNPCByName(args[1]);
        if (npc == null) {
            plugin.getMessageUtils().sendMessage(sender, "npc_not_found");
            return true;
        }

        Location loc = npc.getLocation();
        if (loc != null) {
            player.teleport(loc);
            plugin.getMessageUtils().send(sender, "&aTeleportat la &e" + npc.getName());
        } else {
            plugin.getMessageUtils().send(sender, "&cNu s-a putut obtine locatia NPC-ului!");
        }

        return true;
    }

    /**
     * /ainpc reload
     */
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("ainpc.admin")) {
            plugin.getMessageUtils().sendMessage(sender, "no_permission");
            return true;
        }

        plugin.reload();
        plugin.getMessageUtils().send(sender, "&aConfiguratia a fost reincarcata!");

        return true;
    }

    /**
     * /ainpc test - testeaza conexiunea OpenAI
     */
    private boolean handleTest(CommandSender sender) {
        if (!sender.hasPermission("ainpc.admin")) {
            plugin.getMessageUtils().sendMessage(sender, "no_permission");
            return true;
        }

        plugin.getMessageUtils().send(sender, "&7Testare conexiune OpenAI...");

        plugin.getOpenAIService().diagnoseConnection(true).thenAccept(status -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (status.isReachable() && status.isModelAvailable()) {
                    plugin.getMessageUtils().send(sender, "&aOpenAI este conectat si functional pe &f"
                        + status.getRespondingUrl());
                } else {
                    plugin.getMessageUtils().send(sender, "&c" + status.getSummary());
                }

                if (status.isReachable() && !status.isModelAvailable()) {
                    plugin.getMessageUtils().send(sender, "&eModele raportate: &f"
                        + (status.getAvailableModels().isEmpty()
                            ? "<niciun model>"
                            : String.join(", ", status.getAvailableModels())));
                }

                if (!status.getErrors().isEmpty()) {
                    plugin.getMessageUtils().send(sender, "&7Probe: &f"
                        + String.join(" &7| &f", status.getErrors()));
                }
            });
        });

        return true;
    }

    /**
     * Afiseaza mesajul de ajutor
     */
    private void sendHelp(CommandSender sender) {
        plugin.getMessageUtils().send(sender, "&6=== AI NPC Plugin - Comenzi ===");
        plugin.getMessageUtils().send(sender, "&e/ainpc create <nume> [ocupatie] [varsta] [gen] [arhetip]");
        plugin.getMessageUtils().send(sender, "&7  Creeaza un NPC nou la locatia ta");
        plugin.getMessageUtils().send(sender, "&e/ainpc delete <nume>");
        plugin.getMessageUtils().send(sender, "&7  Sterge un NPC");
        plugin.getMessageUtils().send(sender, "&e/ainpc info [nume]");
        plugin.getMessageUtils().send(sender, "&7  Afiseaza informatii despre un NPC");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest <numeNpc> [jucator]");
        plugin.getMessageUtils().send(sender, "&7  Declanseaza manual quest-ul unui NPC");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest track [start|stop] [jucator]");
        plugin.getMessageUtils().send(sender, "&7  Arata sau mentine busola/actionbar/particule catre tinta questului");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest nearest [jucator]");
        plugin.getMessageUtils().send(sender, "&7  Declanseaza quest-ul celui mai apropiat NPC");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest reset <numeNpc> [jucator]");
        plugin.getMessageUtils().send(sender, "&7  Reseteaza progresul quest-ului pentru un jucator");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest complete <numeNpc> [jucator]");
        plugin.getMessageUtils().send(sender, "&7  Marcheaza manual quest-ul ca finalizat si da recompensa");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest anchors [jucator|uuid|all] [templateId]");
        plugin.getMessageUtils().send(sender, "&7  Listeaza ancorele semantice persistate pentru questuri");
        plugin.getMessageUtils().send(sender, "&e/ainpc list");
        plugin.getMessageUtils().send(sender, "&7  Lista toate NPC-urile");
        plugin.getMessageUtils().send(sender, "&e/ainpc world whereami [jucator]");
        plugin.getMessageUtils().send(sender, "&7  Arata regiunea, place-ul si node-urile active pentru o locatie");
        plugin.getMessageUtils().send(sender, "&e/ainpc world places [regionId]");
        plugin.getMessageUtils().send(sender, "&7  Listeaza place-urile mapate");
        plugin.getMessageUtils().send(sender, "&e/ainpc world region info <regionId>");
        plugin.getMessageUtils().send(sender, "&7  Arata detalii despre o regiune mapata");
        plugin.getMessageUtils().send(sender, "&e/ainpc world region create <id> <type> <x1> <y1> <z1> <x2> <y2> <z2>");
        plugin.getMessageUtils().send(sender, "&7  Creeaza o regiune noua in lumea jucatorului");
        plugin.getMessageUtils().send(sender, "&e/ainpc world place info <placeId>");
        plugin.getMessageUtils().send(sender, "&7  Arata detalii despre un place mapat");
        plugin.getMessageUtils().send(sender, "&e/ainpc world place create <regionId> <id> <type> <x1> <y1> <z1> <x2> <y2> <z2>");
        plugin.getMessageUtils().send(sender, "&7  Creeaza un place nou in interiorul unei regiuni");
        plugin.getMessageUtils().send(sender, "&e/ainpc world node create <regionId> <placeId|-> <id> <type> <x> <y> <z> [radius]");
        plugin.getMessageUtils().send(sender, "&7  Creeaza un node de regiune sau de place");
        plugin.getMessageUtils().send(sender, "&e/ainpc world scan village [radius] [import] [regionId]");
        plugin.getMessageUtils().send(sender, "&7  Scaneaza sat vanilla si poate importa mapping semantic AINPC");
        plugin.getMessageUtils().send(sender, "&e/ainpc world demo create [regionId]");
        plugin.getMessageUtils().send(sender, "&7  Creeaza un mapping demo minim in jurul pozitiei tale");
        plugin.getMessageUtils().send(sender, "&e/ainpc world bind npc <numeNpc|nearest> <homePlaceId> [workPlaceId|-] [socialPlaceId|-]");
        plugin.getMessageUtils().send(sender, "&7  Leaga un NPC la home/work/social places din mapping");
        plugin.getMessageUtils().send(sender, "&e/ainpc world household <plan|spawn> <homePlaceId> [count]");
        plugin.getMessageUtils().send(sender, "&7  Genereaza sau executa un household spawn plan din mapping");
        plugin.getMessageUtils().send(sender, "&e/ainpc world settlement <plan|spawn> <regionId> [maxHouses]");
        plugin.getMessageUtils().send(sender, "&7  Genereaza sau executa household-uri pentru casele din regiune");
        plugin.getMessageUtils().send(sender, "&e/ainpc world save");
        plugin.getMessageUtils().send(sender, "&7  Salveaza modificarile runtime in config.yml");
        plugin.getMessageUtils().send(sender, "&e/ainpc story context [jucator] [numeNpc|nearest]");
        plugin.getMessageUtils().send(sender, "&7  Afiseaza contextul narativ curent din mapping si quest anchors");
        plugin.getMessageUtils().send(sender, "&e/ainpc story region <regionId>");
        plugin.getMessageUtils().send(sender, "&7  Afiseaza story state-ul persistent pentru o regiune");
        plugin.getMessageUtils().send(sender, "&e/ainpc story place <placeId>");
        plugin.getMessageUtils().send(sender, "&7  Afiseaza story state-ul persistent pentru un place");
        plugin.getMessageUtils().send(sender, "&e/ainpc story events <regionId|placeId> [limit]");
        plugin.getMessageUtils().send(sender, "&7  Listeaza evenimente story persistente");
        plugin.getMessageUtils().send(sender, "&e/ainpc audit [all|npc|world|db|spawn|quest]");
        plugin.getMessageUtils().send(sender, "&7  Verifica probleme ascunse in NPC-uri, mapping si baza de date");
        plugin.getMessageUtils().send(sender, "&e/ainpc debugdump [all|npc|world|quest|openai]");
        plugin.getMessageUtils().send(sender, "&7  Genereaza un jurnal avansat read-only pentru debugging");
        plugin.getMessageUtils().send(sender, "&e/ainpc family <nume>");
        plugin.getMessageUtils().send(sender, "&7  Afiseaza familia unui NPC");
        plugin.getMessageUtils().send(sender, "&e/ainpc routine <tick|status>");
        plugin.getMessageUtils().send(sender, "&7  Verifica sau ruleaza rutina zilnica a NPC-urilor");
        plugin.getMessageUtils().send(sender, "&e/ainpc mood <nume> <emotie> [intensitate]");
        plugin.getMessageUtils().send(sender, "&7  Seteaza emotia unui NPC");
        plugin.getMessageUtils().send(sender, "&e/ainpc tp <nume>");
        plugin.getMessageUtils().send(sender, "&7  Teleporteaza-te la un NPC");
        plugin.getMessageUtils().send(sender, "&e/ainpc test");
        plugin.getMessageUtils().send(sender, "&7  Testeaza conexiunea OpenAI");
        plugin.getMessageUtils().send(sender, "&e/ainpc reload");
        plugin.getMessageUtils().send(sender, "&7  Reincarca configuratia");
    }

    // Metode helper

    private String formatLocation(Location loc) {
        if (loc == null) return "necunoscuta";
        return String.format("%s (%.1f, %.1f, %.1f)", 
            loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
    }

    private int parseInt(String s, int defaultValue) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private double parseDouble(String s, double defaultValue) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Integer parseIntegerStrict(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Double parseDoubleStrict(String value) {
        try {
            return Double.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Player requirePlayerSender(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }

        plugin.getMessageUtils().send(sender, "&cAceasta comanda poate fi folosita doar de jucatori.");
        return null;
    }

    private List<WorldNodeInfo> findNodesAtLocation(WorldAdminApi worldAdmin, Location location) {
        return worldAdmin.getNodes().stream()
            .filter(node -> node.worldName().equalsIgnoreCase(location.getWorld().getName()))
            .filter(node -> {
                double dx = node.x() - location.getX();
                double dy = node.y() - location.getY();
                double dz = node.z() - location.getZ();
                double radius = Math.max(0.0, node.radius());
                return dx * dx + dy * dy + dz * dz <= radius * radius;
            })
            .sorted(Comparator.comparing(WorldNodeInfo::id))
            .toList();
    }

    private AINPC resolveWorldBindNpc(CommandSender sender, String selector) {
        if (selector == null || selector.isBlank()) {
            plugin.getMessageUtils().send(sender, "&cSpecifica NPC-ul pentru bind.");
            return null;
        }

        if ("nearest".equalsIgnoreCase(selector)) {
            Player player = requirePlayerSender(sender);
            if (player == null) {
                return null;
            }
            AINPC nearestNpc = findNearestQuestNpc(player);
            if (nearestNpc == null) {
                plugin.getMessageUtils().send(sender, "&cNu exista NPC-uri active in apropierea jucatorului.");
            }
            return nearestNpc;
        }

        AINPC npc = findLoadedNpcBySelector(selector);
        if (npc == null) {
            plugin.getMessageUtils().send(sender, "&cNPC-ul &e" + selector + " &cnu a fost gasit sau nu este incarcat.");
        }
        return npc;
    }

    private WorldPlaceInfo resolveSingleWorldPlace(CommandSender sender,
                                                   WorldAdminApi worldAdmin,
                                                   String selector,
                                                   String role) {
        List<WorldPlaceInfo> matches = findPlaceMatches(worldAdmin, selector);
        if (matches.isEmpty()) {
            plugin.getMessageUtils().send(sender,
                "&cPlace-ul &e" + selector + " &cpentru &f" + role + " &cnu a fost gasit.");
            return null;
        }
        if (matches.size() > 1) {
            plugin.getMessageUtils().send(sender,
                "&cSelector ambiguu pentru place-ul &f" + role + "&c. Foloseste ID-ul complet.");
            plugin.getMessageUtils().send(sender,
                "&7Potriviri: &f" + formatList(matches.stream().map(WorldPlaceInfo::id).toList()));
            return null;
        }
        return matches.get(0);
    }

    private AINPC.OwnedLocation createOwnedLocationFromPlace(WorldAdminApi worldAdmin,
                                                             WorldPlaceInfo place,
                                                             String anchorRole) {
        WorldNodeInfo node = findBestAnchorNodeForPlace(worldAdmin, place, anchorRole);
        if (node != null) {
            return new AINPC.OwnedLocation(
                anchorRole,
                nodeLabel(node, place.displayName()),
                node.worldName(),
                node.x(),
                node.y(),
                node.z()
            );
        }

        return new AINPC.OwnedLocation(
            anchorRole,
            place.displayName(),
            place.worldName(),
            placeCenterX(place),
            placeAnchorY(place),
            placeCenterZ(place)
        );
    }

    private WorldNodeInfo findBestAnchorNodeForPlace(WorldAdminApi worldAdmin,
                                                     WorldPlaceInfo place,
                                                     String anchorRole) {
        WorldNodeInfo bestNode = null;
        double bestScore = Double.MAX_VALUE;
        for (WorldNodeInfo node : worldAdmin.getNodesForPlace(place.id())) {
            int priority = nodePriorityForAnchor(node, anchorRole);
            if (priority < 0) {
                continue;
            }

            double score = priority * 100_000D + distanceSquaredToPlaceCenter(place, node);
            if (score < bestScore) {
                bestScore = score;
                bestNode = node;
            }
        }
        return bestNode;
    }

    private int nodePriorityForAnchor(WorldNodeInfo node, String anchorRole) {
        return switch (normalizeAuditKey(anchorRole)) {
            case "home" -> {
                if (nodeMatchesAny(node, "home", "house", "bed", "sleep", "pat")) {
                    yield 0;
                }
                if (nodeMatchesAny(node, "npc_spawn", "spawn")) {
                    yield 1;
                }
                if (nodeMatchesAny(node, "entrance", "door", "inside", "intrare", "usa")) {
                    yield 2;
                }
                yield nodeMatchesAny(node, "interaction") ? 3 : -1;
            }
            case "work" -> {
                if (nodeMatchesAny(node, "work", "workplace", "workstation", "job", "munca", "lucru")) {
                    yield 0;
                }
                if (nodeMatchesAny(node, "npc_spawn", "spawn")) {
                    yield 1;
                }
                if (nodeMatchesAny(node, "interaction", "counter", "desk")) {
                    yield 2;
                }
                yield -1;
            }
            case "social" -> {
                if (nodeMatchesAny(node, "social", "meeting_point", "meeting", "market", "well", "tavern", "piata", "fantana")) {
                    yield 0;
                }
                if (nodeMatchesAny(node, "interaction")) {
                    yield 1;
                }
                if (nodeMatchesAny(node, "npc_spawn", "spawn")) {
                    yield 2;
                }
                yield -1;
            }
            default -> -1;
        };
    }

    private String nodeLabel(WorldNodeInfo node, String fallbackLabel) {
        String explicitLabel = firstNonBlank(
            node.metadata().get("label"),
            node.metadata().get("name"),
            node.metadata().get("display_name")
        );
        if (!explicitLabel.isBlank()) {
            return explicitLabel;
        }
        return fallbackLabel == null || fallbackLabel.isBlank() ? node.id() : fallbackLabel;
    }

    private String npcBindingId(AINPC npc) {
        if (npc.getDatabaseId() > 0) {
            return "npc_" + npc.getDatabaseId();
        }
        if (npc.getUuid() != null) {
            return npc.getUuid().toString();
        }
        return npc.getName();
    }

    private double distanceSquaredToPlaceCenter(WorldPlaceInfo place, WorldNodeInfo node) {
        return distanceSquared(placeCenterX(place), placeAnchorY(place), placeCenterZ(place), node.x(), node.y(), node.z());
    }

    private double distanceSquared(double leftX, double leftY, double leftZ,
                                   double rightX, double rightY, double rightZ) {
        double dx = leftX - rightX;
        double dy = leftY - rightY;
        double dz = leftZ - rightZ;
        return dx * dx + dy * dy + dz * dz;
    }

    private double placeCenterX(WorldPlaceInfo place) {
        return (place.minX() + place.maxX()) / 2.0D;
    }

    private double placeAnchorY(WorldPlaceInfo place) {
        return Math.min(place.maxY(), place.minY() + 1.0D);
    }

    private double placeCenterZ(WorldPlaceInfo place) {
        return (place.minZ() + place.maxZ()) / 2.0D;
    }

    private List<WorldRegionInfo> findRegionMatches(WorldAdminApi worldAdmin, String selector) {
        if (selector == null || selector.isBlank()) {
            return List.of();
        }

        String normalizedSelector = selector.trim();
        return worldAdmin.getRegions().stream()
            .filter(region -> region.id().equalsIgnoreCase(normalizedSelector)
                || region.name().equalsIgnoreCase(normalizedSelector))
            .sorted(Comparator.comparing(WorldRegionInfo::id))
            .toList();
    }

    private List<WorldPlaceInfo> findPlaceMatches(WorldAdminApi worldAdmin, String selector) {
        if (selector == null || selector.isBlank()) {
            return List.of();
        }

        String normalizedSelector = selector.trim();
        String idSuffix = ":" + normalizedSelector;
        return worldAdmin.getPlaces().stream()
            .filter(place -> place.id().equalsIgnoreCase(normalizedSelector)
                || place.displayName().equalsIgnoreCase(normalizedSelector)
                || place.id().toLowerCase().endsWith(idSuffix.toLowerCase()))
            .sorted(Comparator.comparing(WorldPlaceInfo::id))
            .toList();
    }

    private List<WorldPlaceInfo> findPlaceMatches(WorldAdminApi worldAdmin, String regionId, String selector) {
        return findPlaceMatches(worldAdmin, selector).stream()
            .filter(place -> place.regionId().equalsIgnoreCase(regionId))
            .sorted(Comparator.comparing(WorldPlaceInfo::id))
            .toList();
    }

    private RegionType parseRegionTypeStrict(String value) {
        RegionType type = RegionType.fromId(value);
        if (type == RegionType.CUSTOM && !"custom".equalsIgnoreCase(value)) {
            return null;
        }
        return type;
    }

    private PlaceType parsePlaceTypeStrict(String value) {
        PlaceType type = PlaceType.fromId(value);
        if (type == PlaceType.CUSTOM && !"custom".equalsIgnoreCase(value)) {
            return null;
        }
        return type;
    }

    private WorldNodeType parseNodeTypeStrict(String value) {
        WorldNodeType type = WorldNodeType.fromId(value);
        if (type == WorldNodeType.CUSTOM && !"custom".equalsIgnoreCase(value)) {
            return null;
        }
        return type;
    }

    private boolean isNoneSelector(String value) {
        return value == null || value.isBlank()
            || "-".equals(value)
            || "none".equalsIgnoreCase(value)
            || "null".equalsIgnoreCase(value);
    }

    private WorldRegionInfo toRegionInfo(ro.ainpc.world.WorldRegion region) {
        return new WorldRegionInfo(
            region.getId(),
            region.getName(),
            region.getWorldName(),
            region.getType() != null ? region.getType().getId() : "custom",
            region.getMinX(),
            region.getMinY(),
            region.getMinZ(),
            region.getMaxX(),
            region.getMaxY(),
            region.getMaxZ(),
            region.getTags(),
            region.getStoryState() != null ? region.getStoryState().getMode() : ro.ainpc.world.StoryMode.EVOLUTIVE,
            region.getStoryState() != null ? region.getStoryState().getStateKey() : "default",
            region.getStoryState() != null ? region.getStoryState().getStoryPool() : List.of()
        );
    }

    private WorldPlaceInfo toPlaceInfo(ro.ainpc.world.WorldPlace place) {
        return new WorldPlaceInfo(
            place.getId(),
            place.getRegionId(),
            place.getDisplayName(),
            place.getWorldName(),
            place.getPlaceType(),
            place.getMinX(),
            place.getMinY(),
            place.getMinZ(),
            place.getMaxX(),
            place.getMaxY(),
            place.getMaxZ(),
            place.getTags(),
            place.getOwnerNpcId(),
            place.isPublicAccess(),
            place.getMetadata()
        );
    }

    private WorldNodeInfo toNodeInfo(ro.ainpc.world.WorldNode node) {
        return new WorldNodeInfo(
            node.getId(),
            node.getRegionId(),
            node.getPlaceId(),
            node.getType() != null ? node.getType().getId() : "custom",
            node.getWorldName(),
            node.getX(),
            node.getY(),
            node.getZ(),
            node.getRadius(),
            node.getMetadata()
        );
    }

    private String formatBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return minX + "," + minY + "," + minZ + " -> " + maxX + "," + maxY + "," + maxZ;
    }

    private String formatList(Collection<String> values) {
        return values == null || values.isEmpty() ? "<gol>" : String.join(", ", values);
    }

    private String formatMap(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return "<gol>";
        }

        return values.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .reduce((left, right) -> left + ", " + right)
            .orElse("<gol>");
    }

    private String formatCountMap(Map<String, Integer> values) {
        if (values == null || values.isEmpty()) {
            return "<gol>";
        }

        return values.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .reduce((left, right) -> left + ", " + right)
            .orElse("<gol>");
    }

    private String formatOptional(String value) {
        return value == null || value.isBlank() ? "<nesetat>" : value;
    }

    private String formatOwnedLocation(AINPC.OwnedLocation location) {
        if (location == null) {
            return "<nesetat>";
        }
        return location.label() + " [" + location.type() + "] "
            + location.worldName() + " "
            + String.format("%.1f, %.1f, %.1f", location.x(), location.y(), location.z());
    }

    private AINPC refreshQuestNpc(AINPC npc) {
        if (npc == null) {
            questDebug("refreshQuestNpc primit cu npc=null.");
            return null;
        }

        if (npc.getBukkitEntity() instanceof Villager villager) {
            questDebug("refreshQuestNpc pentru " + npc.getName() + " prin villager uuid=" + villager.getUniqueId());
            plugin.getNpcManager().refreshVillagerProfile(villager);
            AINPC refreshedNpc = plugin.getNpcManager().getNPCByEntity(villager);
            if (refreshedNpc != null) {
                questDebug("refreshQuestNpc a rezolvat npc=" + refreshedNpc.getName()
                    + " ocupatie=" + refreshedNpc.getOccupation());
                return refreshedNpc;
            }
            questDebug("refreshQuestNpc nu a gasit NPC dupa entity pentru " + npc.getName() + ".");
        }

        return npc;
    }

    private void questDebug(String message) {
        plugin.debug("[QuestCmd] " + message);
    }
}
