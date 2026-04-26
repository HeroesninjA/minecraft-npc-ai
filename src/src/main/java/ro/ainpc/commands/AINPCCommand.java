package ro.ainpc.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import ro.ainpc.AINPCPlugin;
import ro.ainpc.engine.ScenarioEngine;
import ro.ainpc.npc.AINPC;

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
        AINPC nearestNpc = plugin.getNpcManager().getActiveNPCsNear(targetPlayer.getLocation(), 16).stream()
            .sorted(Comparator.comparingDouble(npc -> npc.getLocation().distanceSquared(targetPlayer.getLocation())))
            .findFirst()
            .orElse(null);
        if (nearestNpc == null) {
            questDebug("Quest nearest: nu exista NPC activ in raza 16 pentru " + targetPlayer.getName());
            plugin.getMessageUtils().send(sender, "&cNu exista NPC-uri active in apropierea jucatorului.");
            return true;
        }

        questDebug("Quest nearest a ales NPC-ul " + nearestNpc.getName()
            + " (id=" + nearestNpc.getDatabaseId() + ")");
        return handleTriggerQuest(sender, nearestNpc.getName(), targetPlayer);
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
        plugin.getMessageUtils().send(sender, "&e/ainpc quest reset <numeNpc> [jucator]");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest complete <numeNpc> [jucator]");
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
