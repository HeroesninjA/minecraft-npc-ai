package ro.ainpc.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import ro.ainpc.AINPCPlugin;
import ro.ainpc.api.WorldAdminApi;
import ro.ainpc.engine.ScenarioEngine;
import ro.ainpc.npc.AINPC;
import ro.ainpc.world.PlaceType;
import ro.ainpc.world.RegionType;
import ro.ainpc.world.WorldAdminService;
import ro.ainpc.world.WorldNodeInfo;
import ro.ainpc.world.WorldNodeType;
import ro.ainpc.world.WorldPlaceInfo;
import ro.ainpc.world.WorldRegionInfo;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Comanda principala pentru gestionarea NPC-urilor AI
 */
public class AINPCCommand implements CommandExecutor {

    private final AINPCPlugin plugin;

    public AINPCCommand(AINPCPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if ("npcquest".equalsIgnoreCase(command.getName())) {
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
            case "list" -> handleList(sender, args);
            case "family" -> handleFamily(sender, args);
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
            case "nearest" -> handleNearestQuest(sender, args);
            case "accept" -> handleAcceptQuest(sender, args);
            case "decline" -> handleDeclineQuest(sender, args);
            case "abandon" -> handleAbandonQuest(sender, args);
            case "status" -> handleStatusQuest(sender, args);
            case "reset" -> handleResetQuest(sender, args);
            case "complete" -> handleCompleteQuest(sender, args);
            default -> handleTriggerQuest(sender, args[1],
                resolveQuestTargetPlayer(sender, args, 2, "&cUtilizare: /ainpc quest <numeNpc> [jucator]"));
        };
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
        if (args.length < 3) {
            plugin.getMessageUtils().send(sender, "&cUtilizare: /ainpc quest accept <numeNpc>|nearest [jucator]");
            return true;
        }

        String npcSelector = args[2];
        Player targetPlayer = resolveQuestTargetPlayer(
            sender,
            args,
            3,
            "&cUtilizare: /ainpc quest accept <numeNpc>|nearest [jucator]"
        );
        if (targetPlayer == null) {
            questDebug("Quest accept oprit: nu am putut rezolva jucatorul tinta.");
            return true;
        }

        AINPC npc = resolveQuestNpcSelector(sender, npcSelector, targetPlayer, "accept");
        if (npc == null) {
            return true;
        }

        npc = refreshQuestNpc(npc);
        ScenarioEngine.QuestInteractionResult questInteraction = plugin.getScenarioEngine().acceptQuest(targetPlayer, npc);
        if (!questInteraction.isHandled()) {
            plugin.getMessageUtils().send(sender, "&cNPC-ul &e" + npc.getName() + " &cnu are un quest disponibil.");
            return true;
        }

        deliverQuestInteraction(
            sender,
            targetPlayer,
            npc,
            questInteraction,
            "&aJucatorul &f" + targetPlayer.getName() + " &aa acceptat quest-ul lui &e" + npc.getName() + "&a."
        );
        return true;
    }

    private boolean handleDeclineQuest(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.getMessageUtils().send(sender, "&cUtilizare: /ainpc quest decline <numeNpc>|nearest [jucator]");
            return true;
        }

        String npcSelector = args[2];
        Player targetPlayer = resolveQuestTargetPlayer(
            sender,
            args,
            3,
            "&cUtilizare: /ainpc quest decline <numeNpc>|nearest [jucator]"
        );
        if (targetPlayer == null) {
            questDebug("Quest decline oprit: nu am putut rezolva jucatorul tinta.");
            return true;
        }

        AINPC npc = resolveQuestNpcSelector(sender, npcSelector, targetPlayer, "decline");
        if (npc == null) {
            return true;
        }

        npc = refreshQuestNpc(npc);
        ScenarioEngine.QuestInteractionResult questInteraction = plugin.getScenarioEngine().declineQuest(targetPlayer, npc);
        if (!questInteraction.isHandled()) {
            plugin.getMessageUtils().send(sender, "&cNPC-ul &e" + npc.getName() + " &cnu are un quest disponibil.");
            return true;
        }

        deliverQuestInteraction(
            sender,
            targetPlayer,
            npc,
            questInteraction,
            "&eJucatorul &f" + targetPlayer.getName() + " &ea refuzat quest-ul lui &6" + npc.getName() + "&e."
        );
        return true;
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
            plugin.getMessageUtils().send(sender, "&cUtilizare: /ainpc quest status <numeNpc>|nearest [jucator]");
            return true;
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
        plugin.getMessageUtils().send(sender, "&e/ainpc quest <numeNpc> [jucator]");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest nearest [jucator]");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest accept <numeNpc>|nearest [jucator]");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest decline <numeNpc>|nearest [jucator]");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest abandon <numeNpc>|nearest [jucator]");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest status <numeNpc>|nearest [jucator]");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest reset <numeNpc> [jucator]");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest complete <numeNpc> [jucator]");
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
        plugin.getMessageUtils().send(sender, "&e/ainpc world save");
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
        plugin.getMessageUtils().send(sender, "&e/ainpc quest nearest [jucator]");
        plugin.getMessageUtils().send(sender, "&7  Declanseaza quest-ul celui mai apropiat NPC");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest reset <numeNpc> [jucator]");
        plugin.getMessageUtils().send(sender, "&7  Reseteaza progresul quest-ului pentru un jucator");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest complete <numeNpc> [jucator]");
        plugin.getMessageUtils().send(sender, "&7  Marcheaza manual quest-ul ca finalizat si da recompensa");
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
        plugin.getMessageUtils().send(sender, "&e/ainpc world save");
        plugin.getMessageUtils().send(sender, "&7  Salveaza modificarile runtime in config.yml");
        plugin.getMessageUtils().send(sender, "&e/ainpc family <nume>");
        plugin.getMessageUtils().send(sender, "&7  Afiseaza familia unui NPC");
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

    private String formatOptional(String value) {
        return value == null || value.isBlank() ? "<nesetat>" : value;
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
