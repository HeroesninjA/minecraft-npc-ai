package ro.ainpc.commands;

import static ro.ainpc.commands.AINPCCommandText.*;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import ro.ainpc.AINPCPlugin;
import ro.ainpc.ai.OpenAIDebugSnapshot;
import ro.ainpc.api.WorldAdminApi;
import ro.ainpc.debug.DebugDumpService;
import ro.ainpc.debug.WorldMappingSemanticIndex;
import ro.ainpc.engine.FeaturePackLoader;
import ro.ainpc.engine.ScenarioEngine;
import ro.ainpc.gui.GuiKey;
import ro.ainpc.npc.AINPC;
import ro.ainpc.progression.ProgressionAnchorBinding;
import ro.ainpc.progression.ProgressionDefinition;
import ro.ainpc.progression.StoredProgression;
import ro.ainpc.progression.StoredProgressionSummary;
import ro.ainpc.routine.RoutineAssignment;
import ro.ainpc.routine.RoutineTickSummary;
import ro.ainpc.spawn.HouseAllocation;
import ro.ainpc.spawn.HouseAllocationPlanner;
import ro.ainpc.spawn.HouseholdPersistenceService;
import ro.ainpc.spawn.HouseholdSpawnResult;
import ro.ainpc.spawn.NpcSpawnPlan;
import ro.ainpc.spawn.NpcSpawnResult;
import ro.ainpc.spawn.SettlementSpawnResult;
import ro.ainpc.spawn.SpawnBatchTracker;
import ro.ainpc.story.PlaceStoryState;
import ro.ainpc.story.RegionStoryState;
import ro.ainpc.story.StoryContextSnapshot;
import ro.ainpc.story.StoryEvent;
import ro.ainpc.world.PlaceType;
import ro.ainpc.world.RegionType;
import ro.ainpc.world.NpcWorldBinding;
import ro.ainpc.world.WorldAdminService;
import ro.ainpc.world.WorldNodeInfo;
import ro.ainpc.world.WorldNodeType;
import ro.ainpc.world.WorldPlaceInfo;
import ro.ainpc.world.WorldRegionInfo;
import ro.ainpc.world.mapping.MappingDraft;
import ro.ainpc.world.mapping.MappingDraftApplyResult;
import ro.ainpc.world.mapping.MappingDraftKind;
import ro.ainpc.world.mapping.MappingPoint;
import ro.ainpc.world.mapping.MappingWandMode;
import ro.ainpc.world.mapping.MappingWandSelection;
import ro.ainpc.world.mapping.MappingWandService;
import ro.ainpc.world.patch.GapReport;
import ro.ainpc.world.patch.PatchCandidate;
import ro.ainpc.world.patch.PatchPlan;
import ro.ainpc.world.patch.PatchPlannerOptions;
import ro.ainpc.world.patch.PatchPlannerResult;
import ro.ainpc.world.patch.VillageGap;
import ro.ainpc.world.patch.VillageGapAnalyzer;
import ro.ainpc.world.patch.VillagePatchPlanner;
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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Comanda principala pentru gestionarea NPC-urilor AI
 */
public class AINPCCommand implements CommandExecutor {

    private static final int AUDIT_PREVIEW_LIMIT = 12;
    private static final int STORY_EVENT_DEFAULT_LIMIT = 10;
    private static final int STORY_EVENT_MAX_LIMIT = 50;
    private static final int NPC_WORLD_BINDING_DEFAULT_LIMIT = 10;
    private static final int NPC_WORLD_BINDING_MAX_LIMIT = 50;
    private static final int NPC_WORLD_BINDING_LOOKUP_LIMIT = 500;
    private static final int HOUSEHOLD_DEFAULT_LIMIT = 10;
    private static final int HOUSEHOLD_MAX_LIMIT = 50;
    private static final int SPAWN_BATCH_DEFAULT_LIMIT = 10;
    private static final int SPAWN_BATCH_STEP_PREVIEW_LIMIT = 12;
    private static final int QUEST_ANCHOR_AUDIT_DEFAULT_LIMIT = 500;
    private static final int PROGRESSION_STORED_DEFAULT_LIMIT = 12;
    private static final int PROGRESSION_STORED_MAX_LIMIT = 50;
    private static final DateTimeFormatter STORY_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AINPCPlugin plugin;

    public AINPCCommand(AINPCPlugin plugin) {
        this.plugin = plugin;
    }

    private static final ProgressionAliasConfig CONTRACT_ALIAS = new ProgressionAliasConfig(
        "contract",
        "contract",
        "contract",
        "C01",
        "village_contracts",
        "TRADE_DEAL"
    );
    private static final ProgressionAliasConfig DUTY_ALIAS = new ProgressionAliasConfig(
        "duty",
        "duty",
        "sarcina",
        "D01",
        "npc_duties",
        "DUTY"
    );
    private static final ProgressionAliasConfig BOUNTY_ALIAS = new ProgressionAliasConfig(
        "bounty",
        "bounty",
        "bounty",
        "B01",
        "local_bounties",
        "BOUNTY"
    );
    private static final ProgressionAliasConfig EVENT_ALIAS = new ProgressionAliasConfig(
        "event",
        "event",
        "eveniment",
        "E01",
        "village_events",
        "WORLD_EVENT"
    );
    private static final ProgressionAliasConfig TUTORIAL_ALIAS = new ProgressionAliasConfig(
        "tutorial",
        "tutorial",
        "tutorial",
        "T01",
        "onboarding",
        "TUTORIAL"
    );
    private static final ProgressionAliasConfig RITUAL_ALIAS = new ProgressionAliasConfig(
        "ritual",
        "ritual",
        "ritual",
        "R01",
        "village_rituals",
        "RITUAL"
    );

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if ("npcquest".equalsIgnoreCase(command.getName()) || "quest".equalsIgnoreCase(command.getName())) {
            String[] routedArgs = new String[args.length + 1];
            routedArgs[0] = "quest";
            System.arraycopy(args, 0, routedArgs, 1, args.length);
            return handleQuest(sender, routedArgs);
        }
        if ("progression".equalsIgnoreCase(command.getName()) || "progress".equalsIgnoreCase(command.getName())) {
            return handleProgression(sender, routeDirectCommandToQuest(args));
        }
        if ("contract".equalsIgnoreCase(command.getName()) || "contracts".equalsIgnoreCase(command.getName())) {
            return handleContract(sender, routeDirectCommandToQuest(args));
        }
        if ("duty".equalsIgnoreCase(command.getName()) || "duties".equalsIgnoreCase(command.getName())
            || "sarcina".equalsIgnoreCase(command.getName()) || "sarcini".equalsIgnoreCase(command.getName())) {
            return handleDuty(sender, routeDirectCommandToQuest(args));
        }
        if ("bounty".equalsIgnoreCase(command.getName()) || "bounties".equalsIgnoreCase(command.getName())) {
            return handleBounty(sender, routeDirectCommandToQuest(args));
        }
        if ("event".equalsIgnoreCase(command.getName()) || "events".equalsIgnoreCase(command.getName())) {
            return handleEvent(sender, routeDirectCommandToQuest(args));
        }
        if ("tutorial".equalsIgnoreCase(command.getName()) || "tutorials".equalsIgnoreCase(command.getName())
            || "onboarding".equalsIgnoreCase(command.getName())) {
            return handleTutorial(sender, routeDirectCommandToQuest(args));
        }
        if ("ritual".equalsIgnoreCase(command.getName()) || "rituals".equalsIgnoreCase(command.getName())
            || "ceremony".equalsIgnoreCase(command.getName()) || "ceremonies".equalsIgnoreCase(command.getName())) {
            return handleRitual(sender, routeDirectCommandToQuest(args));
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        return switch (subCommand) {
            case "create" -> handleCreate(sender, args);
            case "delete", "remove" -> handleDelete(sender, args);
            case "delete-id" -> handleDeleteId(sender, args);
            case "duplicates" -> handleDuplicates(sender, args);
            case "repair" -> handleRepair(sender, args);
            case "info" -> handleInfo(sender, args);
            case "gui" -> handleGui(sender, args);
            case "quest" -> handleQuest(sender, args);
            case "progression", "progress" -> handleProgression(sender, args);
            case "contract", "contracts" -> handleContract(sender, args);
            case "duty", "duties", "sarcina", "sarcini" -> handleDuty(sender, args);
            case "bounty", "bounties" -> handleBounty(sender, args);
            case "event", "events", "eveniment", "evenimente" -> handleEvent(sender, args);
            case "tutorial", "tutorials", "onboarding" -> handleTutorial(sender, args);
            case "ritual", "rituals", "ceremony", "ceremonies", "ceremonie", "ceremonii" -> handleRitual(sender, args);
            case "demo" -> DemoReadinessCommand.handle(plugin, sender, args);
            case "world" -> handleWorld(sender, args);
            case "patch" -> handlePatch(sender, args);
            case "wand" -> handleWand(sender, args);
            case "map" -> handleMap(sender, args);
            case "story" -> handleStory(sender, args);
            case "migration" -> handleMigration(sender, args);
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

    private String[] routeDirectCommandToQuest(String[] args) {
        String[] routedArgs = new String[args.length + 1];
        routedArgs[0] = "quest";
        System.arraycopy(args, 0, routedArgs, 1, args.length);
        return routedArgs;
    }

    private String[] routeSubcommandToQuest(String[] args) {
        String[] routedArgs = args.clone();
        routedArgs[0] = "quest";
        return routedArgs;
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
     * /ainpc quest track [start|stop] [questCode|templateId] [jucator]
     * /ainpc quest accept <numeNpc>|nearest [jucator]
     * /ainpc quest decline <numeNpc>|nearest [jucator]
     * /ainpc quest abandon <numeNpc>|nearest|tracked|<questCode|templateId> [jucator]
     * /ainpc quest status <numeNpc>|nearest|<questCode|templateId> [jucator]
     * /ainpc quest progress [tracked|questCode|templateId] [jucator]
     * /ainpc quest reset <numeNpc> [jucator]
     * /ainpc quest complete <numeNpc> [jucator]
     */
    private boolean handleProgression(CommandSender sender, String[] args) {
        if (args.length < 2 || isHelpMode(args[1])) {
            sendProgressionUsage(sender);
            return true;
        }
        String mode = args[1].toLowerCase(Locale.ROOT);
        if ("definitions".equals(mode) || "definition".equals(mode) || "defs".equals(mode)) {
            return handleProgressionDefinitions(sender, args);
        }
        if ("stored".equals(mode) || "store".equals(mode) || "state".equals(mode)
            || "states".equals(mode) || "progressions".equals(mode)) {
            return handleProgressionStored(sender, args, "");
        }
        return handleQuest(sender, routeSubcommandToQuest(args));
    }

    private boolean handleProgressionDefinitions(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ainpc.admin") && !sender.hasPermission("ainpc.quest")) {
            plugin.getMessageUtils().sendMessage(sender, "no_permission");
            return true;
        }
        if (args.length > 3) {
            plugin.getMessageUtils().send(sender, "&cUtilizare: /ainpc progression definitions [filter]");
            return true;
        }

        String filter = args.length == 3 ? args[2] : "";
        List<ProgressionDefinition> definitions = plugin.getProgressionService().getDefinitions(filter);
        plugin.getMessageUtils().send(sender, "&6=== Progression Definitions ===");
        plugin.getMessageUtils().send(sender, "&7Total: &f" + definitions.size()
            + (filter.isBlank() ? "" : " &7filtru=&f" + filter));

        int limit = Math.min(12, definitions.size());
        for (int index = 0; index < limit; index++) {
            ProgressionDefinition definition = definitions.get(index);
            plugin.getMessageUtils().send(sender,
                "&e" + definition.progressionId()
                    + " &7code=&f" + formatOptional(definition.code())
                    + " &7kind=&f" + formatOptional(definition.kind())
                    + " &7objectives=&f" + definition.objectiveCount()
                    + " &7stages=&f" + definition.stageCount());
        }
        if (definitions.size() > limit) {
            plugin.getMessageUtils().send(sender, "&7... inca &f" + (definitions.size() - limit)
                + " &7definitii. Foloseste un filtru.");
        }
        return true;
    }

    private boolean handleProgressionStored(CommandSender sender, String[] args, String defaultFilter) {
        if (!sender.hasPermission("ainpc.admin")) {
            plugin.getMessageUtils().sendMessage(sender, "no_permission");
            return true;
        }
        String normalizedDefaultFilter = normalizeProgressionKind(defaultFilter);
        String commandLabel = normalizedDefaultFilter.isBlank() ? "progression" : normalizedDefaultFilter;
        String usage = "&cUtilizare: /ainpc " + commandLabel + " stored [jucator|uuid|all] [filter] [limit]";
        if (args.length > 5) {
            plugin.getMessageUtils().send(sender, usage);
            return true;
        }

        String playerUuid = "";
        String filter = "";
        int limit = PROGRESSION_STORED_DEFAULT_LIMIT;
        int index = 2;

        if (index < args.length) {
            Integer directLimit = parseIntegerStrict(args[index]);
            String resolvedPlayerUuid = resolveProgressionStoredPlayerUuid(args[index]);
            if (directLimit != null) {
                limit = clampProgressionStoredLimit(sender, directLimit);
                index++;
            } else if (resolvedPlayerUuid != null || "all".equalsIgnoreCase(args[index])) {
                playerUuid = resolvedPlayerUuid != null ? resolvedPlayerUuid : "";
                index++;
            }
        }

        if (index < args.length) {
            Integer parsedLimit = parseIntegerStrict(args[index]);
            if (parsedLimit != null) {
                limit = clampProgressionStoredLimit(sender, parsedLimit);
            } else {
                filter = args[index];
            }
            index++;
        }

        if (index < args.length) {
            Integer parsedLimit = parseIntegerStrict(args[index]);
            if (parsedLimit == null) {
                plugin.getMessageUtils().send(sender, usage);
                return true;
            }
            limit = clampProgressionStoredLimit(sender, parsedLimit);
            index++;
        }

        if (index < args.length) {
            plugin.getMessageUtils().send(sender, usage);
            return true;
        }

        if (filter.isBlank() && defaultFilter != null && !defaultFilter.isBlank()) {
            filter = defaultFilter;
        }

        try {
            List<StoredProgression> allMatches = plugin.getProgressionService()
                .getStoredProgressions(playerUuid, filter, 0);
            StoredProgressionSummary summary = StoredProgressionSummary.from(allMatches);
            List<StoredProgression> rows = allMatches.stream()
                .limit(limit)
                .toList();

            plugin.getMessageUtils().send(sender, "&6=== Stored Progressions ===");
            plugin.getMessageUtils().send(sender,
                "&7Player: &f" + (playerUuid.isBlank() ? "all" : playerUuid)
                    + " &7filter=&f" + (filter.isBlank() ? "all" : filter)
                    + " &7total=&f" + allMatches.size()
                    + " &7afisate=&f" + rows.size());
            sendStoredProgressionSummary(sender, summary);

            if (rows.isEmpty()) {
                plugin.getMessageUtils().send(sender, "&7Nu exista progresii persistate pentru filtrul ales.");
                return true;
            }

            for (StoredProgression progression : rows) {
                sendStoredProgressionLine(sender, progression);
            }
            if (allMatches.size() > rows.size()) {
                plugin.getMessageUtils().send(sender, "&7... inca &f" + (allMatches.size() - rows.size())
                    + " &7progresii. Mareste limitul sau foloseste un filtru mai strict.");
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("Nu am putut lista progresiile persistate: " + exception.getMessage());
            plugin.getMessageUtils().send(sender,
                "&cNu am putut lista progresiile persistate: " + exception.getMessage());
        }
        return true;
    }

    private void sendStoredProgressionSummary(CommandSender sender, StoredProgressionSummary summary) {
        plugin.getMessageUtils().send(sender,
            "&7Jucatori=&f" + summary.playerCount()
                + " &7current=&f" + summary.currentCount()
                + " &7archived=&f" + summary.archivedCount()
                + " &7tracked=&f" + summary.trackedCount()
                + " &7unresolved=&f" + summary.unresolvedDefinitionCount());
        plugin.getMessageUtils().send(sender,
            "&7Status: &f" + formatCountMap(summary.byStatus()));
        plugin.getMessageUtils().send(sender,
            "&7Mecanici: &f" + formatCountMap(summary.byMechanic()));
        plugin.getMessageUtils().send(sender,
            "&7Scenarii: &f" + formatCountMap(summary.byScenarioKind())
                + " &7base=&f" + formatCountMap(summary.byBaseType()));
    }

    private void sendStoredProgressionLine(CommandSender sender, StoredProgression progression) {
        plugin.getMessageUtils().send(sender,
            "&e" + progression.progressionId()
                + " &7player=&f" + compactUuid(progression.playerUuid())
                + " &7status=&f" + formatOptional(progression.status())
                + " &7kind=&f" + formatOptional(progression.kind())
                + (progression.scenarioKind().isBlank() ? "" : " &7scenario=&f" + progression.scenarioKind())
                + (progression.tracked() ? " &btracked" : ""));
        plugin.getMessageUtils().send(sender,
            "&8  template=" + formatOptional(progression.templateId())
                + " code=" + formatOptional(progression.code())
                + " mechanic=" + formatOptional(progression.mechanicId())
                + " stage=" + formatOptional(progression.currentStageId())
                + " updated=" + formatStoryTime(progression.updatedAt())
                + (progression.definitionResolved() ? "" : " definition=<missing>"));
    }

    private String resolveProgressionStoredPlayerUuid(String selector) {
        if (selector == null || selector.isBlank() || "all".equalsIgnoreCase(selector)) {
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
        return player != null ? player.getUniqueId().toString() : null;
    }

    private int clampProgressionStoredLimit(CommandSender sender, int limit) {
        if (limit <= 0) {
            plugin.getMessageUtils().send(sender, "&cLimit trebuie sa fie un numar pozitiv.");
            return PROGRESSION_STORED_DEFAULT_LIMIT;
        }
        if (limit > PROGRESSION_STORED_MAX_LIMIT) {
            plugin.getMessageUtils().send(sender,
                "&eLimit maxim pentru afisare: &f" + PROGRESSION_STORED_MAX_LIMIT + "&e.");
        }
        return Math.max(1, Math.min(limit, PROGRESSION_STORED_MAX_LIMIT));
    }

    private String compactUuid(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            return "<nesetat>";
        }
        String safeUuid = uuid.trim();
        return safeUuid.length() > 8 ? safeUuid.substring(0, 8) : safeUuid;
    }

    private boolean handleContract(CommandSender sender, String[] args) {
        return handleProgressionAlias(sender, args, CONTRACT_ALIAS);
    }

    private boolean handleDuty(CommandSender sender, String[] args) {
        return handleProgressionAlias(sender, args, DUTY_ALIAS);
    }

    private boolean handleBounty(CommandSender sender, String[] args) {
        return handleProgressionAlias(sender, args, BOUNTY_ALIAS);
    }

    private boolean handleEvent(CommandSender sender, String[] args) {
        return handleProgressionAlias(sender, args, EVENT_ALIAS);
    }

    private boolean handleTutorial(CommandSender sender, String[] args) {
        return handleProgressionAlias(sender, args, TUTORIAL_ALIAS);
    }

    private boolean handleRitual(CommandSender sender, String[] args) {
        return handleProgressionAlias(sender, args, RITUAL_ALIAS);
    }

    private boolean handleProgressionAlias(CommandSender sender, String[] args, ProgressionAliasConfig alias) {
        if (alias == null) {
            sendProgressionUsage(sender);
            return true;
        }
        if (args.length > 1 && isHelpMode(args[1])) {
            sendProgressionAliasUsage(sender, alias);
            return true;
        }
        if (args.length > 1) {
            String mode = args[1].toLowerCase(Locale.ROOT);
            if ("definitions".equals(mode) || "definition".equals(mode) || "defs".equals(mode)) {
                return handleProgressionDefinitions(sender, args.length == 2
                    ? new String[] {"progression", "definitions", alias.kind()}
                    : args);
            }
            if ("stored".equals(mode) || "store".equals(mode) || "state".equals(mode)
                || "states".equals(mode) || "progressions".equals(mode)) {
                return handleProgressionStored(sender, args, alias.kind());
            }
            if ("nearest".equals(mode)) {
                return handleNearestQuest(sender, args, alias.kind(), alias.displayLabel());
            }
            if (isQuestAcceptMode(mode)) {
                return handleAcceptQuest(sender, args, alias.kind(),
                    "&cUtilizare: /ainpc " + alias.command() + " accept [numeNpc|nearest] [jucator]");
            }
            if (isQuestDeclineMode(mode)) {
                return handleDeclineQuest(sender, args, alias.kind(),
                    "&cUtilizare: /ainpc " + alias.command() + " decline [numeNpc|nearest] [jucator]");
            }
        }
        return handleQuest(sender, routeProgressionAlias(args, alias.kind()));
    }

    private String[] routeProgressionAlias(String[] args, String kind) {
        String[] routedArgs = routeSubcommandToQuest(args);
        String normalizedKind = normalizeProgressionKind(kind);
        if (routedArgs.length == 1) {
            return new String[] {"quest", "log", normalizedKind};
        }

        String mode = routedArgs[1].toLowerCase(Locale.ROOT);
        if ("log".equals(mode)) {
            return routeProgressionAliasLogArgs(routedArgs, normalizedKind);
        }
        if ("gui".equals(mode)) {
            return routeProgressionAliasGuiArgs(routedArgs, normalizedKind);
        }

        return routeProgressionAliasSelectorArgs(routedArgs, mode, normalizedKind);
    }

    private String[] routeProgressionAliasGuiArgs(String[] args, String kind) {
        if (args.length == 2) {
            return new String[] {"quest", "gui", kind};
        }
        if (args.length == 3) {
            return new String[] {"quest", "gui", progressionAliasLogFilter(args[2], kind)};
        }
        return args;
    }

    private String[] routeProgressionAliasLogArgs(String[] args, String kind) {
        if (args.length == 2) {
            return new String[] {"quest", "log", kind};
        }

        if (args.length == 3) {
            return isQuestLogFilter(args[2])
                ? new String[] {"quest", "log", progressionAliasLogFilter(args[2], kind)}
                : new String[] {"quest", "log", args[2], kind};
        }

        if (args.length == 4) {
            boolean firstIsFilter = isQuestLogFilter(args[2]);
            boolean secondIsFilter = isQuestLogFilter(args[3]);
            if (firstIsFilter && !secondIsFilter) {
                return new String[] {"quest", "log", progressionAliasLogFilter(args[2], kind), args[3]};
            }
            if (!firstIsFilter && secondIsFilter) {
                return new String[] {"quest", "log", args[2], progressionAliasLogFilter(args[3], kind)};
            }
        }

        return args;
    }

    private String[] routeProgressionAliasSelectorArgs(String[] args, String mode, String kind) {
        String[] routedArgs = args.clone();
        switch (mode) {
            case "gui", "nearest", "accept", "yes", "y", "da", "ok", "confirm",
                 "decline", "deny", "reject", "no", "n", "nu", "refuz",
                 "reset", "complete", "anchors" -> {
                return routedArgs;
            }
            case "status", "progress", "progres", "debug", "abandon" -> {
                if (routedArgs.length > 2) {
                    routedArgs[2] = progressionAliasSelector(routedArgs[2], kind);
                }
            }
            case "track", "current" -> {
                String rawAction = routedArgs.length > 2 ? routedArgs[2].toLowerCase(Locale.ROOT) : "";
                String action = "start".equals(rawAction) || "stop".equals(rawAction) ? rawAction : "";
                int selectorIndex = action.isBlank() ? 2 : 3;
                if (!"stop".equals(action) && routedArgs.length > selectorIndex) {
                    routedArgs[selectorIndex] = progressionAliasSelector(routedArgs[selectorIndex], kind);
                }
            }
            default -> {
                if (routedArgs.length == 2) {
                    return new String[] {"quest", "status", progressionAliasSelector(routedArgs[1], kind)};
                }
                if (routedArgs.length == 3) {
                    return new String[] {"quest", "status", progressionAliasSelector(routedArgs[1], kind), routedArgs[2]};
                }
            }
        }
        return routedArgs;
    }

    private String progressionAliasLogFilter(String filter, String kind) {
        String normalized = normalizeQuestLogFilter(filter);
        return switch (normalized) {
            case "current" -> kind + "_current";
            case "active" -> kind + "_active";
            case "offered" -> kind + "_offered";
            case "tracked" -> kind + "_tracked";
            case "completed" -> kind + "_completed";
            case "failed" -> kind + "_failed";
            case "archived" -> kind + "_archived";
            default -> kind;
        };
    }

    private String progressionAliasSelector(String selector, String kind) {
        if (selector == null || selector.isBlank()
            || selector.contains(":")
            || "nearest".equalsIgnoreCase(selector)
            || isTrackedQuestSelector(selector)
            || findOnlinePlayer(selector) != null) {
            return selector;
        }

        return plugin.getProgressionService().kindSelector(selector, kind);
    }

    private boolean isHelpMode(String value) {
        if (value == null) {
            return false;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return "help".equals(normalized)
            || "usage".equals(normalized)
            || "ajutor".equals(normalized)
            || "?".equals(normalized);
    }

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
            case "definitions", "definition", "defs" -> handleProgressionDefinitions(sender, args);
            case "gui" -> handleQuestGui(sender, args);
            case "log" -> handleQuestLog(sender, args);
            case "track", "current" -> handleQuestTrack(sender, args);
            case "nearest" -> handleNearestQuest(sender, args);
            case "accept", "yes", "y", "da", "ok", "confirm" -> handleAcceptQuest(sender, args);
            case "decline", "deny", "reject", "no", "n", "nu", "refuz" -> handleDeclineQuest(sender, args);
            case "abandon" -> handleAbandonQuest(sender, args);
            case "status" -> handleStatusQuest(sender, args);
            case "progress", "progres" -> handleQuestProgress(sender, args);
            case "debug" -> handleQuestDebug(sender, args);
            case "reset" -> handleResetQuest(sender, args);
            case "complete" -> handleCompleteQuest(sender, args);
            default -> handleTriggerQuest(sender, args[1],
                resolveQuestTargetPlayer(sender, args, 2, "&cUtilizare: /ainpc quest <numeNpc> [jucator]"));
        };
    }

    private boolean handleQuestGui(CommandSender sender, String[] args) {
        Player player = requirePlayerSender(sender);
        if (player == null) {
            return true;
        }
        if (args.length > 3) {
            plugin.getMessageUtils().send(sender, "&cUtilizare: /ainpc quest gui [progressionFilter]");
            return true;
        }
        if (args.length >= 3) {
            plugin.getGuiService().openQuestLog(player, args[2]);
            return true;
        }
        plugin.getGuiService().open(player, GuiKey.QUEST);
        return true;
    }

    private boolean handleQuestLog(CommandSender sender, String[] args) {
        QuestLogRequest request = resolveQuestLogRequest(sender, args);
        if (request == null) {
            questDebug("Quest log oprit: nu am putut rezolva jucatorul tinta.");
            return true;
        }
        Player targetPlayer = request.player();

        ScenarioEngine.QuestInteractionResult questInteraction =
            plugin.getProgressionService().getLog(targetPlayer, request.filter(), sender.hasPermission("ainpc.admin"));
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
                "&aAi cerut quest log-ul pentru &f" + targetPlayer.getName()
                    + (request.filter().isBlank() ? "&a." : " &afiltru=&f" + request.filter() + "&a."));
        }
        return true;
    }

    private QuestLogRequest resolveQuestLogRequest(CommandSender sender, String[] args) {
        String usage = "&cUtilizare: /ainpc quest log [jucator] [active|current|tracked|quest|contract|duty|bounty|event|main|side|repeatable|completed|failed|archived|all]";
        if (args.length > 4) {
            plugin.getMessageUtils().send(sender, usage);
            return null;
        }

        String filter = "";
        int playerArgIndex = -1;
        if (args.length == 3) {
            String argument = args[2];
            if (isQuestLogFilter(argument)) {
                filter = normalizeQuestLogFilter(argument);
            } else {
                playerArgIndex = 2;
            }
        } else if (args.length == 4) {
            boolean firstIsFilter = isQuestLogFilter(args[2]);
            boolean secondIsFilter = isQuestLogFilter(args[3]);
            if (firstIsFilter && !secondIsFilter) {
                filter = normalizeQuestLogFilter(args[2]);
                playerArgIndex = 3;
            } else if (!firstIsFilter && secondIsFilter) {
                playerArgIndex = 2;
                filter = normalizeQuestLogFilter(args[3]);
            } else {
                plugin.getMessageUtils().send(sender, usage);
                return null;
            }
        }

        Player targetPlayer = resolveQuestTargetPlayer(sender, args, playerArgIndex, usage);
        return targetPlayer != null ? new QuestLogRequest(targetPlayer, filter) : null;
    }

    private boolean isQuestLogFilter(String value) {
        return !normalizeQuestLogFilter(value).isBlank();
    }

    private String normalizeQuestLogFilter(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase();
        return switch (normalized) {
            case "all", "toate" -> "all";
            case "current", "curent", "curente" -> "current";
            case "active", "activ", "activeaza" -> "active";
            case "offered", "oferit", "oferite" -> "offered";
            case "tracked", "urmarit" -> "tracked";
            case "quest", "questuri" -> "quest";
            case "contract", "contracts", "contracte" -> "contract";
            case "duty", "duties", "sarcina", "sarcini" -> "duty";
            case "bounty", "bounties", "recompensa", "recompense" -> "bounty";
            case "event", "events", "eveniment", "evenimente" -> "event";
            case "tutorial", "tutorials", "onboarding", "indrumare" -> "tutorial";
            case "ritual", "rituals", "ceremony", "ceremonies", "ceremonie", "ceremonii" -> "ritual";
            case "contract_current", "contract_curent", "contracte_curente" -> "contract_current";
            case "contract_active", "contract_activ", "contracte_active" -> "contract_active";
            case "contract_offered", "contract_oferit", "contracte_oferite" -> "contract_offered";
            case "contract_tracked", "contract_urmarit", "contracte_urmarite" -> "contract_tracked";
            case "contract_completed", "contract_completat", "contracte_completate" -> "contract_completed";
            case "contract_failed", "contract_esuat", "contracte_esuate" -> "contract_failed";
            case "contract_archived", "contract_arhivat", "contracte_arhivate" -> "contract_archived";
            case "duty_current", "duty_curent", "sarcini_curente" -> "duty_current";
            case "duty_active", "duty_activ", "sarcini_active" -> "duty_active";
            case "duty_offered", "duty_oferit", "sarcini_oferite" -> "duty_offered";
            case "duty_tracked", "duty_urmarit", "sarcini_urmarite" -> "duty_tracked";
            case "duty_completed", "duty_completat", "sarcini_completate" -> "duty_completed";
            case "duty_failed", "duty_esuat", "sarcini_esuate" -> "duty_failed";
            case "duty_archived", "duty_arhivat", "sarcini_arhivate" -> "duty_archived";
            case "bounty_current", "bounty_curent", "recompense_curente" -> "bounty_current";
            case "bounty_active", "bounty_activ", "recompense_active" -> "bounty_active";
            case "bounty_offered", "bounty_oferit", "recompense_oferite" -> "bounty_offered";
            case "bounty_tracked", "bounty_urmarit", "recompense_urmarite" -> "bounty_tracked";
            case "bounty_completed", "bounty_completat", "recompense_completate" -> "bounty_completed";
            case "bounty_failed", "bounty_esuat", "recompense_esuate" -> "bounty_failed";
            case "bounty_archived", "bounty_arhivat", "recompense_arhivate" -> "bounty_archived";
            case "event_current", "event_curent", "evenimente_curente" -> "event_current";
            case "event_active", "event_activ", "evenimente_active" -> "event_active";
            case "event_offered", "event_oferit", "evenimente_oferite" -> "event_offered";
            case "event_tracked", "event_urmarit", "evenimente_urmarite" -> "event_tracked";
            case "event_completed", "event_completat", "evenimente_completate" -> "event_completed";
            case "event_failed", "event_esuat", "evenimente_esuate" -> "event_failed";
            case "event_archived", "event_arhivat", "evenimente_arhivate" -> "event_archived";
            case "tutorial_current", "tutorial_curent", "tutoriale_curente" -> "tutorial_current";
            case "tutorial_active", "tutorial_activ", "tutoriale_active" -> "tutorial_active";
            case "tutorial_offered", "tutorial_oferit", "tutoriale_oferite" -> "tutorial_offered";
            case "tutorial_tracked", "tutorial_urmarit", "tutoriale_urmarite" -> "tutorial_tracked";
            case "tutorial_completed", "tutorial_completat", "tutoriale_completate" -> "tutorial_completed";
            case "tutorial_failed", "tutorial_esuat", "tutoriale_esuate" -> "tutorial_failed";
            case "tutorial_archived", "tutorial_arhivat", "tutoriale_arhivate" -> "tutorial_archived";
            case "ritual_current", "ritual_curent", "ritualuri_curente" -> "ritual_current";
            case "ritual_active", "ritual_activ", "ritualuri_active" -> "ritual_active";
            case "ritual_offered", "ritual_oferit", "ritualuri_oferite" -> "ritual_offered";
            case "ritual_tracked", "ritual_urmarit", "ritualuri_urmarite" -> "ritual_tracked";
            case "ritual_completed", "ritual_completat", "ritualuri_completate" -> "ritual_completed";
            case "ritual_failed", "ritual_esuat", "ritualuri_esuate" -> "ritual_failed";
            case "ritual_archived", "ritual_arhivat", "ritualuri_arhivate" -> "ritual_archived";
            case "main", "principal" -> "main";
            case "side", "secundar", "secundare" -> "side";
            case "repeatable", "repetabil", "repetabile" -> "repeatable";
            case "completed", "complete", "completat", "finalizat", "finalizate" -> "completed";
            case "failed", "esuat", "abandonat", "abandonate" -> "failed";
            case "archived", "archive", "arhivat", "arhivate" -> "archived";
            default -> "";
        };
    }

    private boolean handleQuestTrack(CommandSender sender, String[] args) {
        String trackAction = args.length > 2 ? args[2].toLowerCase() : "";
        QuestTrackRequest trackRequest = resolveQuestTrackRequest(sender, args, trackAction);
        if (trackRequest == null) {
            questDebug("Quest track oprit: nu am putut rezolva jucatorul tinta.");
            return true;
        }
        Player targetPlayer = trackRequest.player();
        String questSelector = trackRequest.questSelector();

        if ("stop".equals(trackRequest.action())) {
            boolean stopped = plugin.getProgressionService().stopTracking(targetPlayer);
            plugin.getMessageUtils().send(sender, stopped
                ? "&aQuest tracking oprit pentru &f" + targetPlayer.getName() + "&a."
                : "&7Quest tracking nu era pornit pentru &f" + targetPlayer.getName() + "&7.");
            if (!sender.equals(targetPlayer)) {
                plugin.getMessageUtils().sendActionBar(targetPlayer, "&cQuest tracking oprit");
            }
            return true;
        }

        ScenarioEngine.QuestInteractionResult questInteraction = plugin.getProgressionService().getTrack(targetPlayer, questSelector);
        if (!questInteraction.isHandled()) {
            plugin.getMessageUtils().send(sender, "&cNu am putut urmari quest-ul curent.");
            return true;
        }

        CommandSender recipient = sender.equals(targetPlayer) ? targetPlayer : sender;
        for (String systemMessage : questInteraction.getSystemMessages()) {
            plugin.getMessageUtils().send(recipient, systemMessage);
        }

        ScenarioEngine.QuestTrackingMarker trackingMarker = "start".equals(trackRequest.action())
            ? plugin.getProgressionService().startTracking(targetPlayer, questSelector)
            : plugin.getProgressionService().getTrackingMarker(targetPlayer, questSelector);
        applyQuestTrackingMarker(sender, targetPlayer, trackingMarker);

        if ("start".equals(trackRequest.action())) {
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

    private QuestTrackRequest resolveQuestTrackRequest(CommandSender sender, String[] args, String rawAction) {
        String action = "start".equals(rawAction) || "stop".equals(rawAction) ? rawAction : "";
        int firstOptionalIndex = action.isBlank() ? 2 : 3;
        String questSelector = "";
        int playerArgIndex = -1;
        String usage = "&cUtilizare: /ainpc quest track [start|stop] [questCode|templateId] [jucator]";

        if (args.length > firstOptionalIndex) {
            String firstOptional = args[firstOptionalIndex];
            Player firstAsPlayer = findOnlinePlayer(firstOptional);
            if (firstAsPlayer != null) {
                playerArgIndex = firstOptionalIndex;
            } else if (!"stop".equals(action)) {
                questSelector = firstOptional;
                playerArgIndex = args.length > firstOptionalIndex + 1 ? firstOptionalIndex + 1 : -1;
            } else {
                plugin.getMessageUtils().send(sender, usage);
                return null;
            }
        }

        int maxArgs = firstOptionalIndex
            + (questSelector.isBlank() ? (playerArgIndex >= 0 ? 1 : 0) : (playerArgIndex >= 0 ? 2 : 1));
        if (args.length > maxArgs) {
            plugin.getMessageUtils().send(sender, usage);
            return null;
        }

        Player targetPlayer = resolveQuestTargetPlayer(sender, args, playerArgIndex, usage);
        if (targetPlayer == null) {
            return null;
        }

        return new QuestTrackRequest(targetPlayer, questSelector, action);
    }

    private void applyQuestTrackingMarker(CommandSender sender,
                                          Player targetPlayer,
                                          ScenarioEngine.QuestTrackingMarker trackingMarker) {
        if (targetPlayer == null || trackingMarker == null || !trackingMarker.hasLocation()) {
            return;
        }

        Location targetLocation = trackingMarker.location();
        boolean compassSet = plugin.getProgressionService().applyTrackingMarker(targetPlayer, trackingMarker);
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
        return handleNearestQuest(sender, args, "", "quest");
    }

    private boolean handleNearestQuest(CommandSender sender, String[] args, String progressionKind, String label) {
        Player targetPlayer = resolveQuestTargetPlayer(
            sender,
            args,
            2,
            "&cUtilizare: /ainpc " + commandLabelForKind(progressionKind) + " nearest [jucator]"
        );
        if (targetPlayer == null) {
            questDebug("Quest nearest oprit: nu am putut rezolva jucatorul tinta.");
            return true;
        }

        questDebug("Quest nearest pentru player=" + targetPlayer.getName()
            + " kind=" + formatOptional(progressionKind)
            + " locatie=" + formatLocation(targetPlayer.getLocation()));
        AINPC nearestNpc = findNearestQuestNpc(targetPlayer, progressionKind);
        if (nearestNpc == null) {
            questDebug("Quest nearest: nu exista NPC activ in raza 16 pentru " + targetPlayer.getName());
            plugin.getMessageUtils().send(sender, "&cNu exista NPC-uri active in apropierea jucatorului"
                + (normalizeProgressionKind(progressionKind).isBlank() ? "." : " cu " + label + " disponibila."));
            return true;
        }

        questDebug("Quest nearest a ales NPC-ul " + nearestNpc.getName()
            + " (id=" + nearestNpc.getDatabaseId() + ")");
        return handleTriggerQuest(sender, nearestNpc.getName(), targetPlayer, progressionKind);
    }

    private boolean handleAcceptQuest(CommandSender sender, String[] args) {
        return handleAcceptQuest(sender, args, "",
            "&cUtilizare: /ainpc quest accept [numeNpc|nearest] [jucator]");
    }

    private boolean handleAcceptQuest(CommandSender sender, String[] args, String progressionKind, String usage) {
        QuestDecisionTarget target = resolveQuestDecisionTarget(
            sender,
            args,
            "accept",
            usage,
            progressionKind
        );
        if (target == null) {
            return true;
        }

        ScenarioEngine.QuestInteractionResult questInteraction =
            plugin.getScenarioEngine().acceptQuest(target.player(), target.npc(), progressionKind);
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
        return handleDeclineQuest(sender, args, "",
            "&cUtilizare: /ainpc quest decline [numeNpc|nearest] [jucator]");
    }

    private boolean handleDeclineQuest(CommandSender sender, String[] args, String progressionKind, String usage) {
        QuestDecisionTarget target = resolveQuestDecisionTarget(
            sender,
            args,
            "decline",
            usage,
            progressionKind
        );
        if (target == null) {
            return true;
        }

        ScenarioEngine.QuestInteractionResult questInteraction =
            plugin.getScenarioEngine().declineQuest(target.player(), target.npc(), progressionKind);
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
        return resolveQuestDecisionTarget(sender, args, action, usage, "");
    }

    private QuestDecisionTarget resolveQuestDecisionTarget(CommandSender sender,
                                                           String[] args,
                                                           String action,
                                                           String usage,
                                                           String progressionKind) {
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

        AINPC npc = resolveFlexibleQuestDecisionNpc(sender, npcSelector, targetPlayer, action, progressionKind);
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
        return resolveFlexibleQuestDecisionNpc(sender, npcSelector, targetPlayer, action, "");
    }

    private AINPC resolveFlexibleQuestDecisionNpc(CommandSender sender,
                                                  String npcSelector,
                                                  Player targetPlayer,
                                                  String action,
                                                  String progressionKind) {
        if (npcSelector != null && !npcSelector.isBlank()) {
            return resolveQuestNpcSelector(sender, npcSelector, targetPlayer, action, progressionKind);
        }

        AINPC npc = plugin.getScenarioEngine().resolveActiveQuestNpc(targetPlayer, progressionKind);
        if (npc != null) {
            questDebug("Quest " + action + " a folosit NPC-ul questului curent: " + npc.getName());
            return npc;
        }

        npc = findNearestQuestNpc(targetPlayer, progressionKind);
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
        String usage = "&cUtilizare: /ainpc quest abandon <numeNpc>|nearest|tracked|<questCode|templateId> [jucator]";
        if (args.length < 3) {
            plugin.getMessageUtils().send(sender, usage);
            return true;
        }

        String npcSelector = args[2];
        Player targetPlayer = resolveQuestTargetPlayer(
            sender,
            args,
            3,
            usage
        );
        if (targetPlayer == null) {
            questDebug("Quest abandon oprit: nu am putut rezolva jucatorul tinta.");
            return true;
        }

        if (shouldHandleAbandonAsQuestSelector(npcSelector)) {
            ScenarioEngine.QuestInteractionResult questInteraction =
                plugin.getProgressionService().abandon(targetPlayer, npcSelector);
            if (!questInteraction.isHandled()) {
                plugin.getMessageUtils().send(sender,
                    "&cNu exista quest curent sau arhivat pentru selectorul &f" + npcSelector + "&c.");
                return true;
            }

            CommandSender recipient = sender.equals(targetPlayer) ? targetPlayer : sender;
            for (String systemMessage : questInteraction.getSystemMessages()) {
                plugin.getMessageUtils().send(recipient, systemMessage);
            }
            if (!sender.equals(targetPlayer)) {
                plugin.getMessageUtils().send(sender,
                    "&eJucatorul &f" + targetPlayer.getName()
                        + " &ea folosit abandon pentru quest selector &6" + npcSelector + "&e.");
            }
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

    private boolean shouldHandleAbandonAsQuestSelector(String selector) {
        if (selector == null || selector.isBlank() || "nearest".equalsIgnoreCase(selector)) {
            return false;
        }
        if (isTrackedQuestSelector(selector)) {
            return true;
        }
        return plugin.getNpcManager().getNPCByName(selector) == null;
    }

    private boolean isTrackedQuestSelector(String selector) {
        String normalized = selector == null ? "" : selector.trim().toLowerCase();
        return "tracked".equals(normalized)
            || "current".equals(normalized)
            || "curent".equals(normalized)
            || "urmarit".equals(normalized);
    }

    private boolean handleQuestDebug(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ainpc.admin")) {
            plugin.getMessageUtils().sendMessage(sender, "no_permission");
            return true;
        }

        String usage = "&cUtilizare: /ainpc quest debug <tracked|questCode|templateId> [jucator]";
        if (args.length < 3) {
            plugin.getMessageUtils().send(sender, usage);
            return true;
        }

        String questSelector = args[2];
        Player targetPlayer = resolveQuestTargetPlayer(sender, args, 3, usage);
        if (targetPlayer == null) {
            questDebug("Quest debug oprit: nu am putut rezolva jucatorul tinta.");
            return true;
        }

        ScenarioEngine.QuestInteractionResult questInteraction =
            plugin.getProgressionService().getDebug(targetPlayer, questSelector);
        if (!questInteraction.isHandled()) {
            plugin.getMessageUtils().send(sender,
                "&cNu exista quest curent sau arhivat pentru selectorul &f" + questSelector + "&c.");
            return true;
        }

        for (String systemMessage : questInteraction.getSystemMessages()) {
            plugin.getMessageUtils().send(sender, systemMessage);
        }
        return true;
    }

    private boolean handleQuestProgress(CommandSender sender, String[] args) {
        String usage = "&cUtilizare: /ainpc quest progress [tracked|questCode|templateId] [jucator]";
        if (args.length > 4) {
            plugin.getMessageUtils().send(sender, usage);
            return true;
        }

        String questSelector = "";
        int playerArgIndex = -1;
        if (args.length == 3) {
            Player explicitPlayer = findOnlinePlayer(args[2]);
            if (explicitPlayer != null && !isTrackedQuestSelector(args[2])) {
                playerArgIndex = 2;
            } else {
                questSelector = args[2];
            }
        } else if (args.length == 4) {
            questSelector = args[2];
            playerArgIndex = 3;
        }

        Player targetPlayer = resolveQuestTargetPlayer(sender, args, playerArgIndex, usage);
        if (targetPlayer == null) {
            questDebug("Quest progress oprit: nu am putut rezolva jucatorul tinta.");
            return true;
        }

        ScenarioEngine.QuestInteractionResult questInteraction =
            plugin.getProgressionService().getProgress(targetPlayer, questSelector);
        if (!questInteraction.isHandled()) {
            plugin.getMessageUtils().send(sender, "&cNu am putut citi progresul questului.");
            return true;
        }

        CommandSender recipient = sender.equals(targetPlayer) ? targetPlayer : sender;
        for (String systemMessage : questInteraction.getSystemMessages()) {
            plugin.getMessageUtils().send(recipient, systemMessage);
        }
        if (!sender.equals(targetPlayer)) {
            plugin.getMessageUtils().send(sender,
                "&aAi cerut progresul questului pentru &f" + targetPlayer.getName()
                    + (questSelector.isBlank() ? "&a." : " &aselector=&f" + questSelector + "&a."));
        }
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
            "&cUtilizare: /ainpc quest status <numeNpc>|nearest|<questCode|templateId> [jucator]"
        );
        if (targetPlayer == null) {
            questDebug("Quest status oprit: nu am putut rezolva jucatorul tinta.");
            return true;
        }

        if (!"nearest".equalsIgnoreCase(npcSelector)
            && plugin.getNpcManager().getNPCByName(npcSelector) == null) {
            ScenarioEngine.QuestInteractionResult questInteraction =
                plugin.getProgressionService().getStatus(targetPlayer, npcSelector);
            if (questInteraction.isHandled()) {
                CommandSender recipient = sender.equals(targetPlayer) ? targetPlayer : sender;
                for (String systemMessage : questInteraction.getSystemMessages()) {
                    plugin.getMessageUtils().send(recipient, systemMessage);
                }
                if (!sender.equals(targetPlayer)) {
                    plugin.getMessageUtils().send(sender,
                        "&aAi cerut statusul questului &f" + npcSelector
                            + " &apentru &f" + targetPlayer.getName() + "&a.");
                }
                return true;
            }
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
            plugin.getMessageUtils().send(sender, "&cUtilizare: /ainpc quest anchors [jucator|uuid|all] [templateId|questCode]");
            return true;
        }

        try {
            List<QuestAnchorBindingRow> rows = queryQuestAnchorBindings(playerUuid, templateId, 20);
            plugin.getMessageUtils().send(sender, "&6=== Quest Anchor Bindings ===");
            plugin.getMessageUtils().send(sender, "&eFiltru player: &f" + (playerUuid.isBlank() ? "all" : playerUuid));
            plugin.getMessageUtils().send(sender, "&eFiltru template/cod: &f" + (templateId.isBlank() ? "all" : templateId));
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
            plugin.getMessageUtils().send(sender, "&cUtilizare: /ainpc quest anchors [jucator|uuid|all] [templateId|questCode]");
            return null;
        }
        return player.getUniqueId().toString();
    }

    private List<QuestAnchorBindingRow> queryQuestAnchorBindings(String playerUuid,
                                                                 String templateIdOrQuestCode,
                                                                 int limit) throws SQLException {
        String reference = templateIdOrQuestCode == null ? "" : templateIdOrQuestCode.trim();
        if (reference.isBlank()) {
            return queryQuestAnchorBindings(playerUuid, "", "", limit);
        }

        List<QuestAnchorBindingRow> rows = queryQuestAnchorBindings(playerUuid, reference, "", limit);
        if (!rows.isEmpty()) {
            return rows;
        }
        return queryQuestAnchorBindings(playerUuid, "", reference, limit);
    }

    private List<QuestAnchorBindingRow> queryQuestAnchorBindings(String playerUuid,
                                                                 String templateId,
                                                                 String questCode,
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
        } else if (questCode != null && !questCode.isBlank()) {
            sql.append(" AND LOWER(b.quest_code) = ?");
            parameters.add(questCode.toLowerCase(Locale.ROOT));
        }
        sql.append(" ORDER BY b.updated_at DESC");
        if (limit > 0) {
            sql.append(" LIMIT ?");
        }

        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement(sql.toString())) {
            int index = 1;
            for (String parameter : parameters) {
                stmt.setString(index++, parameter);
            }
            if (limit > 0) {
                stmt.setInt(index, limit);
            }

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
        return resolveQuestNpcSelector(sender, npcSelector, targetPlayer, action, "");
    }

    private AINPC resolveQuestNpcSelector(CommandSender sender,
                                          String npcSelector,
                                          Player targetPlayer,
                                          String action,
                                          String progressionKind) {
        if (npcSelector == null || npcSelector.isBlank()) {
            plugin.getMessageUtils().send(sender, "&cSpecifica NPC-ul pentru quest.");
            return null;
        }

        if ("nearest".equalsIgnoreCase(npcSelector)) {
            AINPC nearestNpc = findNearestQuestNpc(targetPlayer, progressionKind);
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
        return findNearestQuestNpc(targetPlayer, "");
    }

    private AINPC findNearestQuestNpc(Player targetPlayer, String progressionKind) {
        if (targetPlayer == null) {
            return null;
        }

        return plugin.getNpcManager().getActiveNPCsNear(targetPlayer.getLocation(), 16).stream()
            .filter(npc -> normalizeProgressionKind(progressionKind).isBlank()
                || plugin.getScenarioEngine().hasQuestForNpc(targetPlayer, npc, progressionKind))
            .sorted(Comparator.comparingDouble(npc -> npc.getLocation().distanceSquared(targetPlayer.getLocation())))
            .findFirst()
            .orElse(null);
    }

    private boolean handleTriggerQuest(CommandSender sender, String npcName, Player targetPlayer) {
        return handleTriggerQuest(sender, npcName, targetPlayer, "");
    }

    private boolean handleTriggerQuest(CommandSender sender, String npcName, Player targetPlayer, String progressionKind) {
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
            plugin.getScenarioEngine().startQuestManually(targetPlayer, npc, progressionKind);
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

    private boolean isQuestAcceptMode(String mode) {
        String normalized = mode == null ? "" : mode.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "accept", "yes", "y", "da", "ok", "confirm" -> true;
            default -> false;
        };
    }

    private boolean isQuestDeclineMode(String mode) {
        String normalized = mode == null ? "" : mode.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "decline", "deny", "reject", "no", "n", "nu", "refuz" -> true;
            default -> false;
        };
    }

    private String commandLabelForKind(String progressionKind) {
        String normalized = normalizeProgressionKind(progressionKind);
        return switch (normalized) {
            case "contract" -> "contract";
            case "duty" -> "duty";
            case "bounty" -> "bounty";
            case "event" -> "event";
            case "tutorial" -> "tutorial";
            case "ritual" -> "ritual";
            default -> "quest";
        };
    }

    private String normalizeProgressionKind(String progressionKind) {
        return progressionKind == null ? "" : progressionKind.trim().toLowerCase(Locale.ROOT);
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
        plugin.getMessageUtils().send(sender, "&e/ainpc quest log [jucator] [active|current|tracked|quest|contract|duty|bounty|event|main|side|repeatable|completed|failed|archived|all]");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest track [start|stop] [questCode|templateId] [jucator]");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest status");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest <numeNpc> [jucator]");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest nearest [jucator]");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest accept|da [numeNpc|nearest] [jucator]");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest decline|nu [numeNpc|nearest] [jucator]");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest abandon <numeNpc>|nearest|tracked|<questCode|templateId> [jucator]");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest status <numeNpc>|nearest|<questCode|templateId> [jucator]");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest progress [tracked|questCode|templateId] [jucator]");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest debug <tracked|questCode|templateId> [jucator]");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest reset <numeNpc> [jucator]");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest complete <numeNpc> [jucator]");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest anchors [jucator|uuid|all] [templateId|questCode]");
    }

    private void sendProgressionUsage(CommandSender sender) {
        plugin.getMessageUtils().send(sender, "&cUtilizare:");
        plugin.getMessageUtils().send(sender, "&e/ainpc progression gui [quest|contract|duty|bounty|event|tutorial|ritual|active|all]");
        plugin.getMessageUtils().send(sender, "&e/ainpc progression log [jucator] [quest|contract|duty|bounty|event|tutorial|ritual|active|completed|all]");
        plugin.getMessageUtils().send(sender, "&e/ainpc progression definitions [filter]");
        plugin.getMessageUtils().send(sender, "&e/ainpc progression stored [jucator|uuid|all] [filter] [limit]");
        plugin.getMessageUtils().send(sender, "&e/ainpc progression status <tracked|selector> [jucator]");
        plugin.getMessageUtils().send(sender, "&e/ainpc progression progress [tracked|selector] [jucator]");
        plugin.getMessageUtils().send(sender, "&e/ainpc progression track [start|stop] [selector] [jucator]");
        plugin.getMessageUtils().send(sender, "&e/ainpc progression abandon <tracked|selector> [jucator]");
        plugin.getMessageUtils().send(sender, "&7Selector exemple: &fQ01&7, &fside_quests:Q07&7, &fvillage_contracts:C01&7, &fnpc_duties:D01&7, &flocal_bounties:B01&7, &fvillage_events:E01&7, &fonboarding:T01&7, &fvillage_rituals:R01&7.");
        plugin.getMessageUtils().send(sender, "&7Filtre exemple: &fkind:contract&7, &fkind:duty&7, &fkind:bounty&7, &fkind:event&7, &fkind:tutorial&7, &fkind:ritual&7, &fscenario:investigation&7, &fbase:TRADE_DEAL&7.");
    }

    private void sendProgressionAliasUsage(CommandSender sender, ProgressionAliasConfig alias) {
        String command = alias.command();
        plugin.getMessageUtils().send(sender, "&cUtilizare:");
        plugin.getMessageUtils().send(sender, "&e/ainpc " + command + " gui [active|current|tracked|completed|failed|archived|all]");
        plugin.getMessageUtils().send(sender, "&e/ainpc " + command + " log [jucator] [active|current|tracked|completed|failed|archived|all]");
        plugin.getMessageUtils().send(sender, "&e/ainpc " + command + " definitions [filter]");
        plugin.getMessageUtils().send(sender, "&e/ainpc " + command + " stored [jucator|uuid|all] [filter] [limit]");
        plugin.getMessageUtils().send(sender, "&e/ainpc " + command + " status <selector> [jucator]");
        plugin.getMessageUtils().send(sender, "&e/ainpc " + command + " progress <selector> [jucator]");
        plugin.getMessageUtils().send(sender, "&e/ainpc " + command + " track [start|stop] [selector] [jucator]");
        plugin.getMessageUtils().send(sender, "&e/ainpc " + command + " abandon <selector> [jucator]");
        plugin.getMessageUtils().send(sender, "&7Selector scurt: &f" + alias.shortSelectorExample()
            + " &7devine &f" + alias.kind() + ":" + alias.shortSelectorExample() + "&7.");
        plugin.getMessageUtils().send(sender, "&7Filtre exemple: &fkind:" + alias.kind()
            + "&7, &fmechanic:" + alias.mechanicExample()
            + "&7, &fbase:" + alias.baseTypeExample() + "&7.");
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

    private boolean handleWand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ainpc.admin")) {
            plugin.getMessageUtils().sendMessage(sender, "no_permission");
            return true;
        }

        Player player = requirePlayerSender(sender);
        if (player == null) {
            return true;
        }

        MappingWandService service = plugin.getMappingWandService();
        if (service == null) {
            plugin.getMessageUtils().send(sender, "&cMappingWandService este indisponibil.");
            return true;
        }

        if (args.length == 1) {
            MappingWandService.MappingWandSession session = service.start(player, MappingWandMode.PLACE);
            plugin.getMessageUtils().send(sender, "&aMapping wand activat in modul &f" + session.mode().id() + "&a.");
            sendWandStatus(sender, session);
            return true;
        }

        String action = args[1].toLowerCase(Locale.ROOT);
        return switch (action) {
            case "mode" -> {
                if (args.length != 3) {
                    sendWandUsage(sender);
                    yield true;
                }
                MappingWandMode mode = MappingWandMode.fromId(args[2]).orElse(null);
                if (mode == null) {
                    plugin.getMessageUtils().send(sender,
                        "&cMod wand invalid. Optiuni: &fregion, place, node, npc_bind, quest_anchor");
                    yield true;
                }
                MappingWandService.MappingWandSession session = service.setMode(player, mode);
                plugin.getMessageUtils().send(sender, "&aMapping wand setat pe modul &f" + session.mode().id() + "&a.");
                sendWandStatus(sender, session);
                yield true;
            }
            case "pos1" -> {
                MappingWandService.MappingWandSession session = service.setPos1(player, pointFromPlayer(player));
                plugin.getMessageUtils().send(sender, "&aWand pos1 setat la pozitia ta.");
                sendWandStatus(sender, session);
                service.showSelectionPreview(player, session);
                yield true;
            }
            case "pos2" -> {
                MappingWandService.MappingWandSession session = service.setPos2(player, pointFromPlayer(player));
                plugin.getMessageUtils().send(sender, "&aWand pos2 setat la pozitia ta.");
                sendWandStatus(sender, session);
                service.showSelectionPreview(player, session);
                yield true;
            }
            case "point", "punct" -> {
                MappingWandService.MappingWandSession session = service.setPoint(player, pointFromPlayer(player));
                plugin.getMessageUtils().send(sender, "&aWand point setat la pozitia ta.");
                sendWandStatus(sender, session);
                service.showSelectionPreview(player, session);
                yield true;
            }
            case "status", "inspect" -> {
                sendWandStatus(sender, service.ensureSession(player));
                yield true;
            }
            case "clear", "reset" -> {
                if (args.length == 2 || (args.length == 3 && "all".equalsIgnoreCase(args[2]))) {
                    service.clear(player.getUniqueId());
                    plugin.getMessageUtils().send(sender, "&aSelectia wand a fost curatata.");
                    yield true;
                }
                if (args.length == 3) {
                    MappingWandService.MappingWandSession session = resetWandSelectionPart(service, player, args[2]);
                    if (session == null) {
                        plugin.getMessageUtils().send(sender,
                            "&cParte wand invalida. Optiuni: &fpos1, pos2, point, all");
                        yield true;
                    }
                    plugin.getMessageUtils().send(sender,
                        "&aWand " + formatWandSelectionPart(args[2]) + " a fost resetat.");
                    sendWandStatus(sender, session);
                    yield true;
                }
                sendWandUsage(sender);
                yield true;
            }
            default -> {
                sendWandUsage(sender);
                yield true;
            }
        };
    }

    private boolean handleMap(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ainpc.admin")) {
            plugin.getMessageUtils().sendMessage(sender, "no_permission");
            return true;
        }

        Player player = requirePlayerSender(sender);
        if (player == null) {
            return true;
        }

        MappingWandService service = plugin.getMappingWandService();
        if (service == null) {
            plugin.getMessageUtils().send(sender, "&cMappingWandService este indisponibil.");
            return true;
        }

        if (args.length == 1) {
            sendMapUsage(sender);
            return true;
        }

        String action = args[1].toLowerCase(Locale.ROOT);
        if ("preview".equals(action)) {
            MappingDraft draft = service.session(player.getUniqueId())
                .map(MappingWandService.MappingWandSession::draft)
                .orElse(null);
            if (draft == null) {
                plugin.getMessageUtils().send(sender, "&7Nu exista draft mapping. Ruleaza &f/ainpc map <descriere>&7.");
                return true;
            }
            sendMappingDraft(sender, draft);
            service.showDraftPreview(player, draft);
            return true;
        }
        if ("cancel".equals(action) || "anuleaza".equals(action)) {
            service.cancelDraft(player.getUniqueId());
            plugin.getMessageUtils().send(sender, "&aDraft-ul mapping a fost anulat.");
            return true;
        }
        if ("confirm".equals(action) || "confirma".equals(action)) {
            MappingDraft draft = service.session(player.getUniqueId())
                .map(MappingWandService.MappingWandSession::draft)
                .orElse(null);
            if (draft != null && draft.isNpcBind()) {
                if (applyNpcBindDraft(sender, draft)) {
                    service.cancelDraft(player.getUniqueId());
                }
                return true;
            }
            if (draft != null && draft.isQuestAnchor()) {
                if (applyQuestAnchorDraft(sender, player, draft)) {
                    service.cancelDraft(player.getUniqueId());
                }
                return true;
            }
            try {
                MappingDraftApplyResult result = service.confirmDraft(player, plugin.getPlatform().getWorldAdminService());
                plugin.getMessageUtils().send(sender,
                    "&a" + result.message() + ": &f" + result.createdId() + "&a.");
                plugin.getMessageUtils().send(sender, "&7Ruleaza &f/ainpc audit world &7si apoi &f/ainpc world save&7.");
            } catch (IllegalArgumentException exception) {
                plugin.getMessageUtils().send(sender, "&c" + exception.getMessage());
            }
            return true;
        }

        MappingDraftKind explicitKind = MappingDraftKind.fromId(action).orElse(null);
        int descriptionStart = explicitKind != null ? 2 : 1;
        if (descriptionStart >= args.length) {
            sendMapUsage(sender);
            return true;
        }

        String description = joinArgs(args, descriptionStart);
        try {
            MappingDraft draft = service.createDraft(
                player,
                explicitKind,
                description,
                plugin.getPlatform().getWorldAdminService()
            );
            plugin.getMessageUtils().send(sender, "&aDraft mapping creat. Verifica preview-ul inainte de confirmare.");
            sendMappingDraft(sender, draft);
            service.showDraftPreview(player, draft);
        } catch (IllegalArgumentException exception) {
            plugin.getMessageUtils().send(sender, "&c" + exception.getMessage());
        }
        return true;
    }

    private void sendWandUsage(CommandSender sender) {
        plugin.getMessageUtils().send(sender, "&cUtilizare:");
        plugin.getMessageUtils().send(sender, "&e/ainpc wand");
        plugin.getMessageUtils().send(sender, "&e/ainpc wand mode <region|place|node|npc_bind|quest_anchor>");
        plugin.getMessageUtils().send(sender, "&e/ainpc wand <pos1|pos2|point|status|inspect>");
        plugin.getMessageUtils().send(sender, "&e/ainpc wand <clear|reset> [pos1|pos2|point|all]");
        plugin.getMessageUtils().send(sender, "&7Click stanga/dreapta cu wand-ul seteaza pos1/pos2; in modurile node/npc_bind/quest_anchor seteaza punctul.");
    }

    private MappingWandService.MappingWandSession resetWandSelectionPart(MappingWandService service,
                                                                         Player player,
                                                                         String rawPart) {
        String part = rawPart.toLowerCase(Locale.ROOT);
        return switch (part) {
            case "pos1" -> service.resetPos1(player);
            case "pos2" -> service.resetPos2(player);
            case "point", "punct" -> service.resetPoint(player);
            default -> null;
        };
    }

    private String formatWandSelectionPart(String rawPart) {
        String part = rawPart.toLowerCase(Locale.ROOT);
        return switch (part) {
            case "pos1" -> "pos1";
            case "pos2" -> "pos2";
            case "punct" -> "point";
            default -> "point";
        };
    }

    private void sendMapUsage(CommandSender sender) {
        plugin.getMessageUtils().send(sender, "&cUtilizare:");
        plugin.getMessageUtils().send(sender, "&e/ainpc map <region|place|node|npc_bind|quest_anchor> <descriere>");
        plugin.getMessageUtils().send(sender, "&e/ainpc map quest_anchor [player:<jucator|uuid>] <tracked|current|templateId|questCode> <objective_id> [objective_type] [reference]");
        plugin.getMessageUtils().send(sender, "&e/ainpc map <descriere> &7(foloseste modul wand curent)");
        plugin.getMessageUtils().send(sender, "&e/ainpc map preview");
        plugin.getMessageUtils().send(sender, "&e/ainpc map confirm");
        plugin.getMessageUtils().send(sender, "&e/ainpc map cancel");
    }

    private void sendWandStatus(CommandSender sender, MappingWandService.MappingWandSession session) {
        MappingWandSelection selection = session.selection();
        plugin.getMessageUtils().send(sender, "&6=== Mapping Wand ===");
        plugin.getMessageUtils().send(sender, "&eMod: &f" + session.mode().id());
        plugin.getMessageUtils().send(sender, "&ePos1: &f" + formatMappingPoint(selection.pos1()));
        plugin.getMessageUtils().send(sender, "&ePos2: &f" + formatMappingPoint(selection.pos2()));
        plugin.getMessageUtils().send(sender, "&ePoint: &f" + formatMappingPoint(selection.point()));
        selection.bounds().ifPresent(bounds ->
            plugin.getMessageUtils().send(sender, "&eBounds: &f" + bounds.format()));
        plugin.getMessageUtils().send(sender, "&eDraft: &f"
            + (session.draft() != null ? session.draft().qualifiedId() : "<nesetat>"));
    }

    private void sendMappingDraft(CommandSender sender, MappingDraft draft) {
        plugin.getMessageUtils().send(sender, "&6=== Mapping Draft Preview ===");
        plugin.getMessageUtils().send(sender, "&eTip draft: &f" + draft.kind().id());
        plugin.getMessageUtils().send(sender, "&eID propus: &f" + draft.qualifiedId());
        plugin.getMessageUtils().send(sender, "&eNume: &f" + draft.displayName());
        plugin.getMessageUtils().send(sender, "&eTip semantic: &f" + draft.typeId());
        if (draft.isBox()) {
            plugin.getMessageUtils().send(sender, "&eLume: &f" + draft.worldName());
            plugin.getMessageUtils().send(sender, "&eBounds: &f"
                + formatBounds(draft.minX(), draft.minY(), draft.minZ(), draft.maxX(), draft.maxY(), draft.maxZ()));
        } else if (draft.isNode()) {
            plugin.getMessageUtils().send(sender, "&eRegiune: &f" + draft.regionId());
            plugin.getMessageUtils().send(sender, "&ePlace: &f" + formatOptional(draft.placeId()));
            plugin.getMessageUtils().send(sender, "&ePozitie: &f"
                + String.format(Locale.ROOT, "%.1f, %.1f, %.1f", draft.x(), draft.y(), draft.z()));
            plugin.getMessageUtils().send(sender, "&eRaza: &f" + String.format(Locale.ROOT, "%.1f", draft.radius()));
        } else if (draft.isNpcBind()) {
            plugin.getMessageUtils().send(sender, "&eNPC selector: &f" + draft.metadata().getOrDefault("npc_selector", "<nesetat>"));
            plugin.getMessageUtils().send(sender, "&eRol bind: &f" + draft.metadata().getOrDefault("bind_role", "<nesetat>"));
            plugin.getMessageUtils().send(sender, "&eRegiune: &f" + draft.regionId());
            plugin.getMessageUtils().send(sender, "&ePlace: &f" + formatOptional(draft.placeId()));
        } else if (draft.isQuestAnchor()) {
            plugin.getMessageUtils().send(sender, "&ePlayer selector: &f" + draft.metadata().getOrDefault("player_selector", "self"));
            plugin.getMessageUtils().send(sender, "&eProgresie: &f" + draft.metadata().getOrDefault("progression_selector", "<nesetat>"));
            plugin.getMessageUtils().send(sender, "&eObjective ID: &f" + draft.metadata().getOrDefault("objective_key", "<nesetat>"));
            plugin.getMessageUtils().send(sender, "&eObjective type: &f" + draft.metadata().getOrDefault("objective_type", "<nesetat>"));
            plugin.getMessageUtils().send(sender, "&eAncora: &f"
                + draft.metadata().getOrDefault("anchor_type", "?") + ":"
                + draft.metadata().getOrDefault("anchor_id", "?"));
        }
        plugin.getMessageUtils().send(sender, "&eTag-uri: &f" + formatList(draft.tags()));
        plugin.getMessageUtils().send(sender, "&eMetadata: &f" + formatMap(draft.metadata()));
        if (!draft.warnings().isEmpty()) {
            for (String warning : draft.warnings()) {
                plugin.getMessageUtils().send(sender, "&eWarning: &f" + warning);
            }
        }
        plugin.getMessageUtils().send(sender, "&7Comanda de baza: &f" + draft.confirmationCommand());
        plugin.getMessageUtils().send(sender, "&7Confirma cu &f/ainpc map confirm &7sau anuleaza cu &f/ainpc map cancel&7.");
    }

    private boolean applyNpcBindDraft(CommandSender sender, MappingDraft draft) {
        WorldAdminService worldAdmin = plugin.getPlatform().getWorldAdminService();
        if (!worldAdmin.isEnabled()) {
            plugin.getMessageUtils().send(sender, "&cWorld admin este dezactivat.");
            return false;
        }

        String npcSelector = draft.metadata().getOrDefault("npc_selector", "nearest");
        String role = draft.metadata().getOrDefault("bind_role", "home").toLowerCase(Locale.ROOT);
        AINPC npc = resolveWorldBindNpc(sender, npcSelector);
        if (npc == null) {
            return false;
        }

        WorldPlaceInfo place = worldAdmin.getPlace(draft.placeId());
        if (place == null) {
            plugin.getMessageUtils().send(sender, "&cPlace-ul din draft nu mai exista: &e" + draft.placeId() + "&c.");
            return false;
        }

        if ("home".equals(role) && !isHousePlace(place)) {
            plugin.getMessageUtils().send(sender, "&eWarning: place-ul &f" + place.id() + " &enu este marcat ca house/home.");
        } else if ("work".equals(role) && !isWorkplace(place)) {
            plugin.getMessageUtils().send(sender, "&eWarning: place-ul &f" + place.id() + " &enu este marcat clar ca workplace.");
        } else if ("social".equals(role) && !isSocialPlace(place)) {
            plugin.getMessageUtils().send(sender, "&eWarning: place-ul &f" + place.id() + " &enu este marcat clar ca loc social.");
        } else if (!List.of("home", "work", "social").contains(role)) {
            plugin.getMessageUtils().send(sender, "&cRol bind invalid in draft: &e" + role + "&c.");
            return false;
        }

        AINPC.OwnedLocation previousHome = npc.getHomeAnchor();
        AINPC.OwnedLocation previousWork = npc.getWorkAnchor();
        AINPC.OwnedLocation previousSocial = npc.getSocialAnchor();
        AINPC.OwnedLocation anchor = createOwnedLocationFromPlace(worldAdmin, place, role);
        switch (role) {
            case "home" -> npc.setHomeAnchor(anchor);
            case "work" -> npc.setWorkAnchor(anchor);
            case "social" -> npc.setSocialAnchor(anchor);
            default -> {
                return false;
            }
        }

        if (!plugin.getNpcManager().saveNPC(npc, false)) {
            npc.setHomeAnchor(previousHome);
            npc.setWorkAnchor(previousWork);
            npc.setSocialAnchor(previousSocial);
            plugin.getMessageUtils().send(sender, "&cNu am putut salva profilul NPC-ului.");
            return false;
        }

        String bindingId = npcBindingId(npc);
        try {
            switch (role) {
                case "home" -> worldAdmin.bindNpcToHomePlace(place.id(), bindingId, npc.getName());
                case "work" -> worldAdmin.bindNpcToWorkPlace(place.id(), bindingId, npc.getName());
                case "social" -> worldAdmin.bindNpcToSocialPlace(place.id(), bindingId, npc.getName());
                default -> throw new IllegalArgumentException("Rol bind invalid: " + role);
            }
        } catch (IllegalArgumentException exception) {
            npc.setHomeAnchor(previousHome);
            npc.setWorkAnchor(previousWork);
            npc.setSocialAnchor(previousSocial);
            if (!plugin.getNpcManager().saveNPC(npc, false)) {
                plugin.getMessageUtils().send(sender,
                    "&eWarning: &fNu am putut restaura ancorele NPC dupa esecul bind-ului.");
            }
            plugin.getMessageUtils().send(sender, "&c" + exception.getMessage());
            return false;
        }

        WorldNodeInfo node = findBestAnchorNodeForPlace(worldAdmin, place, role);
        saveNpcWorldBinding(sender, new NpcWorldBinding(
            npc.getDatabaseId(),
            npc.getUuid() != null ? npc.getUuid().toString() : "",
            npc.getName(),
            "home".equals(role) ? place.id() : "",
            "work".equals(role) ? place.id() : "",
            "social".equals(role) ? place.id() : "",
            "home".equals(role) && node != null ? node.id() : "",
            "work".equals(role) && node != null ? node.id() : "",
            "social".equals(role) && node != null ? node.id() : "",
            "",
            "wand_bind",
            0L,
            0L
        ), true);

        plugin.getMessageUtils().send(sender,
            "&aNPC-ul &f" + npc.getName() + " &aa fost legat la &f" + role + " &ain &f" + place.id() + "&a.");
        plugin.getMessageUtils().send(sender, "&eAncora: &f" + formatOwnedLocation(anchor));
        plugin.getMessageUtils().send(sender,
            "&7Ruleaza &f/ainpc world save &7pentru metadata mapping, apoi &f/ainpc audit spawn&7.");
        recordConfirmedMappingDraft(sender, draft, bindingId + ":" + role + ":" + place.id(), "NPC bind confirmat");
        return true;
    }

    private boolean applyQuestAnchorDraft(CommandSender sender, Player commandPlayer, MappingDraft draft) {
        if (plugin.getDatabaseManager() == null) {
            plugin.getMessageUtils().send(sender, "&cDatabaseManager nu este initializat.");
            return false;
        }
        if (plugin.getProgressionService() == null) {
            plugin.getMessageUtils().send(sender, "&cProgressionService este indisponibil.");
            return false;
        }

        Map<String, String> metadata = draft.metadata();
        String playerSelector = metadata.getOrDefault("player_selector", "self");
        String targetPlayerUuid = resolveQuestAnchorDraftPlayerUuid(sender, commandPlayer, playerSelector);
        if (targetPlayerUuid == null) {
            return false;
        }

        String progressionSelector = metadata.getOrDefault("progression_selector", "");
        StoredProgression progression;
        try {
            progression = resolveQuestAnchorProgression(sender, targetPlayerUuid, progressionSelector);
        } catch (SQLException exception) {
            plugin.getMessageUtils().send(sender,
                "&cNu am putut citi progresiile persistate: " + exception.getMessage());
            return false;
        }
        if (progression == null) {
            return false;
        }

        String objectiveKey = metadata.getOrDefault("objective_key", "");
        String objectiveType = metadata.getOrDefault("objective_type", "");
        String reference = metadata.getOrDefault("reference", "");
        String anchorType = metadata.getOrDefault("anchor_type", "");
        String anchorId = metadata.getOrDefault("anchor_id", "");
        String anchorLabel = metadata.getOrDefault("anchor_label", "");
        if (objectiveKey.isBlank() || objectiveType.isBlank() || anchorType.isBlank() || anchorId.isBlank()) {
            plugin.getMessageUtils().send(sender, "&cDraft quest_anchor incomplet. Refaceti draft-ul.");
            return false;
        }
        if (!isQuestAnchorTypeCompatible(objectiveType, anchorType)) {
            plugin.getMessageUtils().send(sender,
                "&cTip incompatibil: objective_type=" + objectiveType + ", anchor_type=" + anchorType + ".");
            return false;
        }
        if (!validateQuestAnchorObjectiveAgainstDefinition(sender, progression, objectiveKey, objectiveType)) {
            return false;
        }
        if (!questAnchorTargetExists(anchorType, anchorId)) {
            plugin.getMessageUtils().send(sender,
                "&cAncora din draft nu mai exista in mapping: &e" + anchorType + ":" + anchorId + "&c.");
            return false;
        }

        long now = System.currentTimeMillis();
        try {
            plugin.getProgressionService().saveAnchorBinding(new ProgressionAnchorBinding(
                targetPlayerUuid,
                progression.templateId(),
                objectiveKey,
                progression.code(),
                objectiveType,
                reference,
                anchorType,
                anchorId,
                anchorLabel,
                now,
                now,
                progression.status()
            ));
        } catch (SQLException | IllegalArgumentException exception) {
            plugin.getMessageUtils().send(sender,
                "&cNu am putut salva quest_anchor_bindings: " + exception.getMessage());
            return false;
        }

        plugin.getMessageUtils().send(sender,
            "&aQuest anchor salvat pentru &f" + progression.templateId()
                + " &a/ &f" + objectiveKey + "&a -> &f" + anchorType + ":" + anchorId + "&a.");
        plugin.getMessageUtils().send(sender,
            "&7Verifica prin &f/ainpc quest anchors " + targetPlayerUuid + " " + progression.templateId() + "&7.");
        recordConfirmedMappingDraft(sender, draft,
            progression.templateId() + ":" + objectiveKey,
            "Quest anchor confirmat");
        return true;
    }

    private boolean validateQuestAnchorObjectiveAgainstDefinition(CommandSender sender,
                                                                  StoredProgression progression,
                                                                  String objectiveKey,
                                                                  String objectiveType) {
        FeaturePackLoader.ScenarioDefinition scenario = findScenarioForProgression(progression);
        if (scenario == null) {
            plugin.getMessageUtils().send(sender,
                "&eWarning: &fNu am gasit definitia progresiei pentru validarea stricta a objective_id.");
            return true;
        }

        Map<String, FeaturePackLoader.QuestEntryDefinition> objectives = collectObjectiveKeyLookup(scenario);
        FeaturePackLoader.QuestEntryDefinition objective = objectives.get(normalizeQuestObjectiveLookupKey(objectiveKey));
        if (objective == null) {
            List<String> candidates = objectives.values().stream()
                .distinct()
                .map(this::displayQuestObjectiveKey)
                .filter(value -> !value.isBlank())
                .distinct()
                .limit(8)
                .toList();
            plugin.getMessageUtils().send(sender,
                "&cObjective_id invalid pentru progresia &e" + progression.templateId() + "&c: &e" + objectiveKey + "&c.");
            if (!candidates.isEmpty()) {
                plugin.getMessageUtils().send(sender, "&7Obiective valide: &f" + String.join(", ", candidates));
            }
            return false;
        }

        String expectedType = normalizeQuestObjectiveType(objective.getType());
        String requestedType = normalizeQuestObjectiveType(objectiveType);
        if (!expectedType.isBlank() && !requestedType.isBlank() && !expectedType.equals(requestedType)) {
            plugin.getMessageUtils().send(sender,
                "&cObjective_type nu corespunde definitiei: draft=&e" + objectiveType
                    + " &c, definitie=&e" + expectedType + "&c.");
            return false;
        }
        return true;
    }

    private FeaturePackLoader.ScenarioDefinition findScenarioForProgression(StoredProgression progression) {
        FeaturePackLoader loader = plugin.getFeaturePackLoader();
        if (loader == null || progression == null) {
            return null;
        }
        for (FeaturePackLoader.ScenarioDefinition scenario : loader.getAllScenarios()) {
            if (!ProgressionDefinition.isProgressionCandidate(scenario)) {
                continue;
            }
            ProgressionDefinition definition = ProgressionDefinition.fromScenarioDefinition(scenario);
            if (storedProgressionMatchesDefinition(progression, definition)) {
                return scenario;
            }
        }
        return null;
    }

    private boolean storedProgressionMatchesDefinition(StoredProgression progression,
                                                       ProgressionDefinition definition) {
        return sameNonBlankIgnoreCase(progression.templateId(), definition.templateId())
            || sameNonBlankIgnoreCase(progression.code(), definition.code())
            || sameNonBlankIgnoreCase(progression.progressionId(), definition.progressionId())
            || (sameNonBlankIgnoreCase(progression.packId(), definition.packId())
                && sameNonBlankIgnoreCase(progression.definitionId(), definition.definitionId()))
            || sameNonBlankIgnoreCase(progression.definitionId(), definition.definitionId());
    }

    private Map<String, FeaturePackLoader.QuestEntryDefinition> collectObjectiveKeyLookup(
        FeaturePackLoader.ScenarioDefinition scenario) {
        Map<String, FeaturePackLoader.QuestEntryDefinition> lookup = new HashMap<>();
        List<FeaturePackLoader.QuestEntryDefinition> objectives = scenario.getObjectives();
        for (int index = 0; index < objectives.size(); index++) {
            FeaturePackLoader.QuestEntryDefinition objective = objectives.get(index);
            addObjectiveLookupKey(lookup, objective.getEntryId(), objective);
            addObjectiveLookupKey(lookup, questEntryId(objective), objective);
            addObjectiveLookupKey(lookup, objective.getItemId(), objective);
            addObjectiveLookupKey(lookup, generatedQuestObjectiveKey(objective, index), objective);
        }
        return lookup;
    }

    private void addObjectiveLookupKey(Map<String, FeaturePackLoader.QuestEntryDefinition> lookup,
                                       String key,
                                       FeaturePackLoader.QuestEntryDefinition objective) {
        String normalized = normalizeQuestObjectiveLookupKey(key);
        if (!normalized.isBlank()) {
            lookup.putIfAbsent(normalized, objective);
        }
    }

    private String generatedQuestObjectiveKey(FeaturePackLoader.QuestEntryDefinition objective, int index) {
        String entryId = objective.getEntryId();
        if (entryId != null && !entryId.isBlank()) {
            return entryId;
        }
        String type = normalizeQuestStageReference(firstNonBlank(objective.getType(), "objective"));
        String itemId = normalizeQuestStageReference(firstNonBlank(objective.getItemId(), "entry"));
        return type + ":" + itemId + ":" + index;
    }

    private String displayQuestObjectiveKey(FeaturePackLoader.QuestEntryDefinition objective) {
        String entryId = objective.getEntryId();
        if (entryId != null && !entryId.isBlank()) {
            return entryId;
        }
        return questEntryId(objective);
    }

    private String normalizeQuestObjectiveLookupKey(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim()
            .toLowerCase(Locale.ROOT)
            .replace("minecraft:", "")
            .replaceAll("[^\\p{L}\\p{Nd}:]+", "_")
            .replaceAll("^_+|_+$", "")
            .replaceAll("_+", "_");
    }

    private void recordConfirmedMappingDraft(CommandSender sender,
                                             MappingDraft draft,
                                             String resultId,
                                             String resultMessage) {
        if (!(sender instanceof Player player)) {
            return;
        }
        MappingWandService service = plugin.getMappingWandService();
        if (service != null) {
            service.recordConfirmedDraft(player, draft, resultId, resultMessage);
        }
    }

    private String resolveQuestAnchorDraftPlayerUuid(CommandSender sender, Player commandPlayer, String selector) {
        String safeSelector = selector == null ? "" : selector.trim();
        if (safeSelector.isBlank()
            || "self".equalsIgnoreCase(safeSelector)
            || "me".equalsIgnoreCase(safeSelector)
            || "@s".equalsIgnoreCase(safeSelector)) {
            return commandPlayer.getUniqueId().toString();
        }

        try {
            return UUID.fromString(safeSelector).toString();
        } catch (IllegalArgumentException ignored) {
            // Nu este UUID; incercam jucator online.
        }

        Player target = plugin.getServer().getPlayerExact(safeSelector);
        if (target == null) {
            target = plugin.getServer().getPlayer(safeSelector);
        }
        if (target == null) {
            plugin.getMessageUtils().send(sender,
                "&cJucatorul pentru quest_anchor trebuie sa fie online sau selectorul trebuie sa fie UUID.");
            return null;
        }
        return target.getUniqueId().toString();
    }

    private StoredProgression resolveQuestAnchorProgression(CommandSender sender,
                                                            String playerUuid,
                                                            String selector) throws SQLException {
        String safeSelector = selector == null ? "" : selector.trim();
        if (safeSelector.isBlank()) {
            plugin.getMessageUtils().send(sender,
                "&cSpecifica progresia: tracked, current, templateId sau questCode.");
            return null;
        }

        List<StoredProgression> rows = plugin.getProgressionService()
            .getStoredProgressions(playerUuid, "all", 0);
        if (rows.isEmpty()) {
            plugin.getMessageUtils().send(sender, "&cJucatorul nu are progresii persistate.");
            return null;
        }

        String normalized = safeSelector.toLowerCase(Locale.ROOT);
        List<StoredProgression> matches;
        if ("tracked".equals(normalized) || "urmarit".equals(normalized)) {
            matches = rows.stream().filter(StoredProgression::tracked).toList();
        } else if ("current".equals(normalized) || "active".equals(normalized) || "curent".equals(normalized)) {
            matches = rows.stream().filter(StoredProgression::current).toList();
        } else {
            matches = rows.stream()
                .filter(row -> storedProgressionMatchesSelector(row, safeSelector))
                .toList();
        }

        if (matches.isEmpty()) {
            plugin.getMessageUtils().send(sender,
                "&cNu am gasit progresia &e" + safeSelector + " &cpentru playerul selectat.");
            return null;
        }
        if (matches.size() > 1) {
            plugin.getMessageUtils().send(sender,
                "&cSelectorul &e" + safeSelector + " &care " + matches.size()
                    + " potriviri. Foloseste templateId sau questCode exact.");
            matches.stream()
                .limit(5)
                .forEach(row -> plugin.getMessageUtils().send(sender,
                    "&7- &f" + row.templateId() + " &7cod=&f" + formatOptional(row.code())
                        + " &7status=&f" + row.status()));
            return null;
        }
        return matches.get(0);
    }

    private boolean storedProgressionMatchesSelector(StoredProgression row, String selector) {
        String normalized = selector == null ? "" : selector.trim();
        return equalsIgnoreCase(row.templateId(), normalized)
            || equalsIgnoreCase(row.code(), normalized)
            || equalsIgnoreCase(row.progressionId(), normalized)
            || equalsIgnoreCase(row.definitionId(), normalized)
            || equalsIgnoreCase(row.mechanicId() + ":" + row.definitionId(), normalized)
            || equalsIgnoreCase(row.packId() + ":" + row.definitionId(), normalized);
    }

    private boolean questAnchorTargetExists(String anchorType, String anchorId) {
        WorldAdminApi worldAdmin = plugin.getPlatform() != null ? plugin.getPlatform().getWorldAdmin() : null;
        if (worldAdmin == null || !worldAdmin.isEnabled()) {
            return false;
        }
        String type = normalizeAuditKey(anchorType);
        return switch (type) {
            case "region" -> worldAdmin.getRegion(anchorId) != null;
            case "place" -> worldAdmin.getPlace(anchorId) != null;
            case "node" -> worldAdmin.getNode(anchorId) != null;
            case "npc" -> findLoadedNpcBySelector(anchorId) != null;
            default -> false;
        };
    }

    private MappingPoint pointFromPlayer(Player player) {
        Location location = player.getLocation();
        return new MappingPoint(
            player.getWorld().getName(),
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ()
        );
    }

    private String formatMappingPoint(MappingPoint point) {
        return point == null ? "<nesetat>" : point.format();
    }

    private String joinArgs(String[] args, int startIndex) {
        StringBuilder builder = new StringBuilder();
        for (int index = Math.max(0, startIndex); index < args.length; index++) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(args[index]);
        }
        return builder.toString();
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
            case "binding", "bindings" -> handleWorldBindings(sender, args);
            case "household" -> handleWorldHousehold(sender, args);
            case "settlement" -> handleWorldSettlement(sender, args);
            case "save" -> handleWorldSave(sender);
            default -> {
                sendWorldUsage(sender);
                yield true;
            }
        };
    }

    private boolean handlePatch(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ainpc.admin")) {
            plugin.getMessageUtils().sendMessage(sender, "no_permission");
            return true;
        }
        if (args.length < 3 || args.length > 5) {
            sendPatchUsage(sender);
            return true;
        }

        String mode = args[1].toLowerCase(Locale.ROOT);
        if (!Set.of("analyze", "analyse", "plan", "validate").contains(mode)) {
            sendPatchUsage(sender);
            return true;
        }

        int targetPopulation = 0;
        if (args.length >= 4) {
            Integer parsedPopulation = parseIntegerStrict(args[3]);
            if (parsedPopulation == null || parsedPopulation < 0) {
                plugin.getMessageUtils().send(sender, "&cTarget population trebuie sa fie 0 sau un numar pozitiv.");
                return true;
            }
            targetPopulation = parsedPopulation;
        }

        PatchPlannerOptions options = PatchPlannerOptions.forTargetPopulation(
            targetPopulation,
            args.length >= 5 ? parsePatchProfessionList(args[4]) : List.of()
        );
        WorldAdminApi worldAdmin = plugin.getPlatform().getWorldAdmin();
        GapReport report = new VillageGapAnalyzer().analyze(worldAdmin, args[2], options);
        sendPatchGapReport(sender, report);
        if (!report.success() || "analyze".equals(mode) || "analyse".equals(mode)) {
            return true;
        }

        PatchPlannerResult result = new VillagePatchPlanner().plan(report, options);
        sendPatchPlannerResult(sender, result, "validate".equals(mode));
        return true;
    }

    private void sendPatchUsage(CommandSender sender) {
        plugin.getMessageUtils().send(sender, "&cUtilizare:");
        plugin.getMessageUtils().send(sender, "&e/ainpc patch analyze <regionId> [targetPopulation] [profesiiCSV]");
        plugin.getMessageUtils().send(sender, "&e/ainpc patch plan <regionId> [targetPopulation] [profesiiCSV]");
        plugin.getMessageUtils().send(sender, "&e/ainpc patch validate <regionId> [targetPopulation] [profesiiCSV]");
        plugin.getMessageUtils().send(sender,
            "&7Read-only: produce GapReport si PatchPlan, fara constructie si fara scrieri in mapping.");
    }

    private List<String> parsePatchProfessionList(String rawValue) {
        if (rawValue == null || rawValue.isBlank() || "-".equals(rawValue)) {
            return List.of();
        }
        List<String> professions = new ArrayList<>();
        for (String part : rawValue.split(",")) {
            String profession = part.trim();
            if (!profession.isBlank()) {
                professions.add(profession);
            }
        }
        return List.copyOf(professions);
    }

    private void sendPatchGapReport(CommandSender sender, GapReport report) {
        plugin.getMessageUtils().send(sender, "&6=== Patch Gap Report ===");
        plugin.getMessageUtils().send(sender, "&eRegiune: &f" + formatOptional(report.regionId()));
        plugin.getMessageUtils().send(sender,
            "&eCapacitate: &f" + report.currentCapacity()
                + " &7/ tinta &f" + report.requiredCapacity()
                + " &7| case &f" + report.houseCount()
                + " &7| case lipsa &f" + report.missingHomes());
        plugin.getMessageUtils().send(sender,
            "&eWorkplace lipsa: &f" + formatListOrNone(report.missingWorkplaces())
                + " &7| social lipsa &f" + report.missingSocialPlaces()
                + " &7| node-uri lipsa &f" + formatListOrNone(report.missingNodes()));
        sendAuditMessages(sender, "&cErori patch", report.errors());
        sendAuditMessages(sender, "&eWarning-uri patch", report.warnings());
        sendAuditMessages(sender, "&eGap-uri", report.gaps().stream()
            .map(this::formatVillageGap)
            .toList());
        if (report.success() && !report.hasGaps()) {
            plugin.getMessageUtils().send(sender, "&aNu sunt gap-uri evidente pentru optiunile curente.");
        }
    }

    private String formatVillageGap(VillageGap gap) {
        return gap.type()
            + " x" + gap.amount()
            + (gap.targetPlaceId().isBlank() ? "" : " place=" + gap.targetPlaceId())
            + (gap.reference().isBlank() ? "" : " ref=" + gap.reference())
            + " severity=" + gap.severity()
            + " - " + gap.reason();
    }

    private void sendPatchPlannerResult(CommandSender sender, PatchPlannerResult result, boolean validationView) {
        plugin.getMessageUtils().send(sender, validationView ? "&6=== Patch Validation ===" : "&6=== Patch Plan ===");
        plugin.getMessageUtils().send(sender,
            "&eCandidati: &f" + result.candidates().size()
                + " &7| Patch-uri: &f" + result.patchPlans().size()
                + " &7| Blocate: &f" + result.patchPlans().stream().filter(plan -> !plan.valid()).count());
        sendAuditMessages(sender, "&cErori planner", result.errors());
        sendAuditMessages(sender, "&eWarning-uri planner", result.warnings());
        sendAuditMessages(sender, "&eCandidati patch", result.candidates().stream()
            .map(this::formatPatchCandidate)
            .toList());
        sendAuditMessages(sender, validationView ? "&eValidare patch-uri" : "&ePatch-uri planificate",
            result.patchPlans().stream()
                .map(this::formatPatchPlan)
                .toList());
        if (validationView) {
            if (result.patchPlans().stream().allMatch(PatchPlan::valid)) {
                plugin.getMessageUtils().send(sender, "&aToate patch-urile planificate sunt valide pentru mod read-only.");
            } else {
                plugin.getMessageUtils().send(sender, "&eUnele patch-uri sunt blocate de capabilitati lipsa.");
            }
        }
    }

    private String formatPatchCandidate(PatchCandidate candidate) {
        return candidate.candidateId()
            + " " + candidate.patchType().id()
            + " priority=" + candidate.priority()
            + " cost=" + candidate.cost()
            + " risk=" + candidate.risk();
    }

    private String formatPatchPlan(PatchPlan plan) {
        return plan.patchId()
            + " " + plan.type().id()
            + " mode=" + plan.buildMode().id()
            + " status=" + plan.validationStatus()
            + " places=" + formatListOrNone(plan.plannedPlaces())
            + " nodes=" + formatListOrNone(plan.plannedNodes())
            + (plan.errors().isEmpty() ? "" : " errors=" + String.join("; ", plan.errors()));
    }

    private String formatListOrNone(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "-";
        }
        return String.join(", ", values);
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
            plugin.getMessageUtils().send(sender,
                "&7Creeaza un mapping demo minim la pozitia ta; consola/RCON foloseste spawn-ul lumii.");
            return true;
        }

        WorldCommandLocation origin = resolveWorldDemoLocation(sender);
        if (origin == null) {
            return true;
        }

        WorldAdminService worldAdmin = plugin.getPlatform().getWorldAdminService();
        if (!worldAdmin.isEnabled()) {
            plugin.getMessageUtils().send(sender, "&cWorld admin este dezactivat.");
            return true;
        }

        String regionId = args.length == 4 ? args[3] : null;
        try {
            WorldAdminService.DemoMappingResult result = worldAdmin.createDemoSettlement(
                regionId,
                origin.worldName(),
                origin.x(),
                origin.y(),
                origin.z(),
                origin.minHeight(),
                origin.maxHeight()
            );

            plugin.getMessageUtils().send(sender, "&aMapping demo creat in regiunea &f" + result.regionId() + "&a.");
            if (origin.consoleFallback()) {
                plugin.getMessageUtils().send(sender, "&7Consola/RCON: am folosit spawn-ul lumii &f"
                    + origin.worldName() + "&7 ca centru demo.");
            }
            plugin.getMessageUtils().send(sender, "&7Centru: &f" + origin.x() + ", "
                + origin.y() + ", " + origin.z());
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

    private WorldCommandLocation resolveWorldDemoLocation(CommandSender sender) {
        if (sender instanceof Player player) {
            Location location = player.getLocation();
            World world = player.getWorld();
            return new WorldCommandLocation(
                world.getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                world.getMinHeight(),
                world.getMaxHeight(),
                false
            );
        }

        List<World> worlds = Bukkit.getWorlds();
        if (worlds.isEmpty()) {
            plugin.getMessageUtils().send(sender, "&cNu exista lumi incarcate pentru mapping demo.");
            return null;
        }

        World world = worlds.get(0);
        Location spawn = world.getSpawnLocation();
        return new WorldCommandLocation(
            world.getName(),
            spawn.getBlockX(),
            spawn.getBlockY(),
            spawn.getBlockZ(),
            world.getMinHeight(),
            world.getMaxHeight(),
            true
        );
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
        WorldNodeInfo homeNode = findBestAnchorNodeForPlace(worldAdmin, homePlace, "home");
        WorldNodeInfo workNode = workPlace != null
            ? findBestAnchorNodeForPlace(worldAdmin, workPlace, "work")
            : null;
        WorldNodeInfo socialNode = socialPlace != null
            ? findBestAnchorNodeForPlace(worldAdmin, socialPlace, "social")
            : null;
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

        NpcWorldBinding binding = new NpcWorldBinding(
            npc.getDatabaseId(),
            npc.getUuid() != null ? npc.getUuid().toString() : "",
            npc.getName(),
            homePlace.id(),
            workPlace != null ? workPlace.id() : "",
            socialPlace != null ? socialPlace.id() : "",
            homeNode != null ? homeNode.id() : "",
            workNode != null ? workNode.id() : "",
            socialNode != null ? socialNode.id() : "",
            "",
            "manual_bind",
            0L,
            0L
        );
        saveNpcWorldBinding(sender, binding, true);

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

    private boolean handleWorldBindings(CommandSender sender, String[] args) {
        if (plugin.getNpcWorldBindingService() == null) {
            plugin.getMessageUtils().send(sender, "&cNpcWorldBindingService este indisponibil.");
            return true;
        }

        if (args.length == 2) {
            return sendNpcWorldBindingsList(sender, NPC_WORLD_BINDING_DEFAULT_LIMIT);
        }

        if (args.length == 3) {
            Integer directLimit = parseIntegerStrict(args[2]);
            if (directLimit != null) {
                return sendNpcWorldBindingsList(sender, clampNpcWorldBindingLimit(sender, directLimit));
            }
        }

        String mode = args[2].toLowerCase(Locale.ROOT);
        return switch (mode) {
            case "list", "all" -> {
                if (args.length > 4) {
                    sendWorldBindingsUsage(sender);
                    yield true;
                }
                int limit = args.length == 4
                    ? clampNpcWorldBindingLimit(sender, parseNpcWorldBindingLimit(sender, args[3]))
                    : NPC_WORLD_BINDING_DEFAULT_LIMIT;
                yield sendNpcWorldBindingsList(sender, limit);
            }
            case "npc" -> {
                if (args.length != 4) {
                    sendWorldBindingsUsage(sender);
                    yield true;
                }
                yield sendNpcWorldBindingForNpc(sender, args[3]);
            }
            case "place" -> {
                if (args.length < 4 || args.length > 5) {
                    sendWorldBindingsUsage(sender);
                    yield true;
                }
                int limit = args.length == 5
                    ? clampNpcWorldBindingLimit(sender, parseNpcWorldBindingLimit(sender, args[4]))
                    : NPC_WORLD_BINDING_DEFAULT_LIMIT;
                yield sendNpcWorldBindingsForPlace(sender, args[3], limit);
            }
            default -> {
                sendWorldBindingsUsage(sender);
                yield true;
            }
        };
    }

    private boolean sendNpcWorldBindingsList(CommandSender sender, int limit) {
        try {
            int total = plugin.getNpcWorldBindingService().countBindings();
            List<NpcWorldBinding> bindings = plugin.getNpcWorldBindingService().listBindings(limit);
            plugin.getMessageUtils().send(sender, "&6=== NPC World Bindings ===");
            plugin.getMessageUtils().send(sender, "&eRanduri: &f" + bindings.size() + "/" + total);
            if (bindings.isEmpty()) {
                plugin.getMessageUtils().send(sender, "&7Nu exista binding-uri NPC-world persistate.");
                plugin.getMessageUtils().send(sender,
                    "&7Leaga un NPC cu &f/ainpc world bind npc <numeNpc|nearest> <homePlaceId>&7.");
                return true;
            }

            for (NpcWorldBinding binding : bindings) {
                sendNpcWorldBindingSummary(sender, binding);
            }
            if (total > bindings.size()) {
                plugin.getMessageUtils().send(sender,
                    "&7Mai exista randuri. Foloseste &f/ainpc world bindings <limit> &7sau &f/ainpc world bindings npc <npc>&7.");
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("Nu am putut lista npc_world_bindings: " + exception.getMessage());
            plugin.getMessageUtils().send(sender, "&cNu am putut lista npc_world_bindings: " + exception.getMessage());
        }
        return true;
    }

    private boolean sendNpcWorldBindingForNpc(CommandSender sender, String selector) {
        try {
            NpcWorldBinding binding = resolveNpcWorldBinding(sender, selector);
            if (binding == null) {
                plugin.getMessageUtils().send(sender,
                    "&cNu exista binding NPC-world pentru selectorul &e" + selector + "&c.");
                return true;
            }

            sendNpcWorldBindingDetails(sender, binding);
        } catch (SQLException exception) {
            plugin.getLogger().warning("Nu am putut citi npc_world_bindings: " + exception.getMessage());
            plugin.getMessageUtils().send(sender, "&cNu am putut citi npc_world_bindings: " + exception.getMessage());
        }
        return true;
    }

    private boolean sendNpcWorldBindingsForPlace(CommandSender sender, String placeSelector, int limit) {
        try {
            Set<String> resolvedPlaceIds = resolveNpcWorldBindingPlaceIds(placeSelector);
            Set<String> placeIds = resolvedPlaceIds.isEmpty()
                ? Set.of(placeSelector)
                : resolvedPlaceIds;

            int totalRows = plugin.getNpcWorldBindingService().countBindings();
            List<NpcWorldBinding> matches = plugin.getNpcWorldBindingService()
                .listBindings(NPC_WORLD_BINDING_LOOKUP_LIMIT)
                .stream()
                .filter(binding -> bindingReferencesAnyPlace(binding, placeIds))
                .sorted(Comparator
                    .comparing((NpcWorldBinding binding) -> binding.npcName().isBlank() ? "~" : binding.npcName())
                    .thenComparingInt(NpcWorldBinding::npcId))
                .toList();

            plugin.getMessageUtils().send(sender, "&6=== NPC World Bindings: Place ===");
            plugin.getMessageUtils().send(sender, "&ePlace selector: &f" + placeSelector);
            plugin.getMessageUtils().send(sender, "&ePlace IDs: &f" + formatList(placeIds));
            plugin.getMessageUtils().send(sender, "&ePotriviri: &f" + Math.min(matches.size(), limit) + "/" + matches.size());
            if (matches.isEmpty()) {
                plugin.getMessageUtils().send(sender, "&7Nu exista NPC binding-uri pentru acest place.");
                return true;
            }

            for (NpcWorldBinding binding : matches.stream().limit(limit).toList()) {
                sendNpcWorldBindingSummary(sender, binding);
            }
            if (matches.size() > limit) {
                plugin.getMessageUtils().send(sender, "&7Mai exista potriviri. Mareste limita pentru mai multe randuri.");
            }
            if (totalRows > NPC_WORLD_BINDING_LOOKUP_LIMIT) {
                plugin.getMessageUtils().send(sender,
                    "&eWarning: &ffiltrarea place a verificat primele " + NPC_WORLD_BINDING_LOOKUP_LIMIT
                        + " randuri din " + totalRows + ".");
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("Nu am putut filtra npc_world_bindings: " + exception.getMessage());
            plugin.getMessageUtils().send(sender, "&cNu am putut filtra npc_world_bindings: " + exception.getMessage());
        }
        return true;
    }

    private void sendWorldBindingsUsage(CommandSender sender) {
        plugin.getMessageUtils().send(sender, "&cUtilizare:");
        plugin.getMessageUtils().send(sender, "&e/ainpc world bindings [limit]");
        plugin.getMessageUtils().send(sender, "&e/ainpc world bindings list [limit]");
        plugin.getMessageUtils().send(sender, "&e/ainpc world bindings npc <numeNpc|nearest|npcId|uuid>");
        plugin.getMessageUtils().send(sender, "&e/ainpc world bindings place <placeId> [limit]");
        plugin.getMessageUtils().send(sender, "&7Comanda este read-only si inspecteaza tabela persistenta npc_world_bindings.");
    }

    private void sendNpcWorldBindingSummary(CommandSender sender, NpcWorldBinding binding) {
        plugin.getMessageUtils().send(sender,
            "&e#" + binding.npcId() + " &f" + formatOptional(binding.npcName())
                + " &7source=&f" + formatOptional(binding.source())
                + " &7updated=&f" + formatStoryTime(binding.updatedAt()));
        plugin.getMessageUtils().send(sender,
            "&7  home=&f" + formatOptional(binding.homePlaceId())
                + " &7work=&f" + formatOptional(binding.workPlaceId())
                + " &7social=&f" + formatOptional(binding.socialPlaceId()));
    }

    private void sendNpcWorldBindingDetails(CommandSender sender, NpcWorldBinding binding) {
        WorldAdminApi worldAdmin = plugin.getPlatform() != null ? plugin.getPlatform().getWorldAdmin() : null;
        AINPC loadedNpc = findLoadedNpcBySelector("npc_" + binding.npcId());

        plugin.getMessageUtils().send(sender, "&6=== NPC World Binding ===");
        plugin.getMessageUtils().send(sender,
            "&eNPC: &f#" + binding.npcId() + " " + formatOptional(binding.npcName())
                + " &7uuid=&f" + formatOptional(binding.npcUuid()));
        plugin.getMessageUtils().send(sender,
            "&eSursa: &f" + formatOptional(binding.source())
                + " &7family=&f" + formatOptional(binding.familyId()));
        plugin.getMessageUtils().send(sender,
            "&eCreat: &f" + formatStoryTime(binding.createdAt())
                + " &7| Updated: &f" + formatStoryTime(binding.updatedAt()));
        plugin.getMessageUtils().send(sender,
            "&eNPC incarcat: &f" + (loadedNpc != null ? "da" : "nu"));

        sendNpcWorldBindingRole(sender, worldAdmin, "home", binding.homePlaceId(), binding.homeNodeId());
        sendNpcWorldBindingRole(sender, worldAdmin, "work", binding.workPlaceId(), binding.workNodeId());
        sendNpcWorldBindingRole(sender, worldAdmin, "social", binding.socialPlaceId(), binding.socialNodeId());

        if (loadedNpc != null) {
            plugin.getMessageUtils().send(sender, "&eAncore profil runtime:");
            plugin.getMessageUtils().send(sender, "&7  home=&f" + formatOwnedLocation(loadedNpc.getHomeAnchor()));
            plugin.getMessageUtils().send(sender, "&7  work=&f" + formatOwnedLocation(loadedNpc.getWorkAnchor()));
            plugin.getMessageUtils().send(sender, "&7  social=&f" + formatOwnedLocation(loadedNpc.getSocialAnchor()));
        }
    }

    private void sendNpcWorldBindingRole(CommandSender sender,
                                         WorldAdminApi worldAdmin,
                                         String role,
                                         String placeId,
                                         String nodeId) {
        plugin.getMessageUtils().send(sender,
            "&e" + role + ": &fplace=" + formatOptional(placeId)
                + " &7node=&f" + formatOptional(nodeId));
        if (worldAdmin == null || !worldAdmin.isEnabled()) {
            return;
        }

        WorldPlaceInfo place = findPlaceById(worldAdmin, placeId);
        WorldNodeInfo node = findNodeById(worldAdmin, nodeId);
        if (place != null) {
            plugin.getMessageUtils().send(sender,
                "&7  place info: &f" + place.displayName()
                    + " &8[" + place.placeType().getId() + "]"
                    + " &7regiune=&f" + place.regionId());
        } else if (placeId != null && !placeId.isBlank()) {
            plugin.getMessageUtils().send(sender, "&e  Warning: &fplace-ul nu exista in mapping-ul incarcat.");
        }

        if (node != null) {
            plugin.getMessageUtils().send(sender,
                "&7  node info: &f" + node.id()
                    + " &8[" + node.typeId() + "]"
                    + " &7place=&f" + formatOptional(node.placeId())
                    + " &7loc=&f" + node.worldName() + " "
                    + String.format("%.1f, %.1f, %.1f", node.x(), node.y(), node.z()));
            if (place != null && !node.placeId().isBlank() && !node.placeId().equalsIgnoreCase(place.id())) {
                plugin.getMessageUtils().send(sender,
                    "&e  Warning: &fnode-ul este in alt place decat " + place.id() + ".");
            }
        } else if (nodeId != null && !nodeId.isBlank()) {
            plugin.getMessageUtils().send(sender, "&e  Warning: &fnode-ul nu exista in mapping-ul incarcat.");
        }
    }

    private NpcWorldBinding resolveNpcWorldBinding(CommandSender sender, String selector) throws SQLException {
        AINPC loadedNpc = null;
        if ("nearest".equalsIgnoreCase(selector)) {
            loadedNpc = resolveWorldBindNpc(sender, selector);
            if (loadedNpc == null) {
                return null;
            }
        } else {
            loadedNpc = findLoadedNpcBySelector(selector);
        }

        if (loadedNpc != null && loadedNpc.getDatabaseId() > 0) {
            return plugin.getNpcWorldBindingService().getBinding(loadedNpc.getDatabaseId()).orElse(null);
        }

        Integer npcId = parseNpcIdSelector(selector);
        if (npcId != null && npcId > 0) {
            return plugin.getNpcWorldBindingService().getBinding(npcId).orElse(null);
        }

        String normalizedSelector = normalizeAuditKey(selector);
        if (normalizedSelector.isBlank()) {
            return null;
        }
        return plugin.getNpcWorldBindingService()
            .listBindings(NPC_WORLD_BINDING_LOOKUP_LIMIT)
            .stream()
            .filter(binding -> normalizedSelector.equals(normalizeAuditKey(binding.npcName()))
                || normalizedSelector.equals(normalizeAuditKey(binding.npcUuid()))
                || normalizedSelector.equals("npc_" + binding.npcId()))
            .findFirst()
            .orElse(null);
    }

    private Integer parseNpcIdSelector(String selector) {
        if (selector == null || selector.isBlank()) {
            return null;
        }
        String normalized = normalizeAuditKey(selector);
        if (normalized.startsWith("npc_")) {
            normalized = normalized.substring("npc_".length());
        }
        return parseIntegerStrict(normalized);
    }

    private Set<String> resolveNpcWorldBindingPlaceIds(String selector) {
        WorldAdminApi worldAdmin = plugin.getPlatform() != null ? plugin.getPlatform().getWorldAdmin() : null;
        if (worldAdmin == null || !worldAdmin.isEnabled()) {
            return Set.of();
        }
        return findPlaceMatches(worldAdmin, selector).stream()
            .map(WorldPlaceInfo::id)
            .collect(Collectors.toCollection(HashSet::new));
    }

    private boolean bindingReferencesAnyPlace(NpcWorldBinding binding, Set<String> placeIds) {
        if (binding == null || placeIds == null || placeIds.isEmpty()) {
            return false;
        }
        for (String placeId : placeIds) {
            if (placeId == null || placeId.isBlank()) {
                continue;
            }
            if (placeId.equalsIgnoreCase(binding.homePlaceId())
                || placeId.equalsIgnoreCase(binding.workPlaceId())
                || placeId.equalsIgnoreCase(binding.socialPlaceId())) {
                return true;
            }
        }
        return false;
    }

    private WorldPlaceInfo findPlaceById(WorldAdminApi worldAdmin, String placeId) {
        if (worldAdmin == null || placeId == null || placeId.isBlank()) {
            return null;
        }
        return worldAdmin.getPlaces().stream()
            .filter(place -> place.id().equalsIgnoreCase(placeId))
            .findFirst()
            .orElse(null);
    }

    private WorldNodeInfo findNodeById(WorldAdminApi worldAdmin, String nodeId) {
        if (worldAdmin == null || nodeId == null || nodeId.isBlank()) {
            return null;
        }
        return worldAdmin.getNodes().stream()
            .filter(node -> node.id().equalsIgnoreCase(nodeId))
            .findFirst()
            .orElse(null);
    }

    private int parseNpcWorldBindingLimit(CommandSender sender, String rawLimit) {
        Integer parsedLimit = parseIntegerStrict(rawLimit);
        if (parsedLimit == null || parsedLimit <= 0) {
            plugin.getMessageUtils().send(sender, "&cLimit trebuie sa fie un numar pozitiv.");
            return NPC_WORLD_BINDING_DEFAULT_LIMIT;
        }
        return parsedLimit;
    }

    private int clampNpcWorldBindingLimit(CommandSender sender, int limit) {
        if (limit > NPC_WORLD_BINDING_MAX_LIMIT) {
            plugin.getMessageUtils().send(sender,
                "&eLimit maxim pentru afisare: &f" + NPC_WORLD_BINDING_MAX_LIMIT + "&e.");
        }
        return Math.max(1, Math.min(limit, NPC_WORLD_BINDING_MAX_LIMIT));
    }

    private boolean handleWorldHousehold(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendWorldHouseholdUsage(sender);
            return true;
        }

        return switch (args[2].toLowerCase(Locale.ROOT)) {
            case "plan", "spawn" -> handleWorldHouseholdPlanOrSpawn(sender, args);
            case "status" -> handleWorldHouseholdStatus(sender, args);
            case "place" -> handleWorldHouseholdPlace(sender, args);
            case "resident" -> handleWorldHouseholdResident(sender, args);
            case "list" -> handleWorldHouseholdList(sender, args);
            default -> {
                sendWorldHouseholdUsage(sender);
                yield true;
            }
        };
    }

    private boolean handleWorldHouseholdPlanOrSpawn(CommandSender sender, String[] args) {
        if (args.length < 4 || args.length > 5) {
            sendWorldHouseholdUsage(sender);
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

    private boolean handleWorldHouseholdStatus(CommandSender sender, String[] args) {
        if (args.length != 4) {
            sendWorldHouseholdUsage(sender);
            return true;
        }

        HouseholdPersistenceService service = requireHouseholdPersistence(sender);
        if (service == null) {
            return true;
        }

        String selector = args[3];
        try {
            Optional<HouseholdPersistenceService.HouseholdRecord> household = service.getHousehold(selector);
            if (household.isEmpty()) {
                household = service.findHouseholdByHomePlace(selector);
            }
            if (household.isEmpty()) {
                plugin.getMessageUtils().send(sender,
                    "&cNu exista household persistent pentru &e" + selector + "&c.");
                plugin.getMessageUtils().send(sender,
                    "&7Foloseste household_id sau home_place_id exact. Pentru sumar: &f/ainpc world household list");
                return true;
            }

            sendPersistentHouseholdSummary(sender, household.get(), service.listResidents(household.get().householdId()));
        } catch (SQLException exception) {
            plugin.getMessageUtils().send(sender,
                "&cNu am putut citi household-ul persistent: &e" + exception.getMessage());
        }
        return true;
    }

    private boolean handleWorldHouseholdPlace(CommandSender sender, String[] args) {
        if (args.length != 4) {
            sendWorldHouseholdUsage(sender);
            return true;
        }

        HouseholdPersistenceService service = requireHouseholdPersistence(sender);
        if (service == null) {
            return true;
        }

        String homePlaceId = args[3];
        try {
            Optional<HouseholdPersistenceService.HouseholdRecord> household =
                service.findHouseholdByHomePlace(homePlaceId);
            if (household.isEmpty()) {
                plugin.getMessageUtils().send(sender,
                    "&cNu exista household persistent pentru casa &e" + homePlaceId + "&c.");
                return true;
            }
            sendPersistentHouseholdSummary(sender, household.get(), service.listResidents(household.get().householdId()));
        } catch (SQLException exception) {
            plugin.getMessageUtils().send(sender,
                "&cNu am putut lista household-ul pentru place: &e" + exception.getMessage());
        }
        return true;
    }

    private boolean handleWorldHouseholdResident(CommandSender sender, String[] args) {
        if (args.length != 4) {
            sendWorldHouseholdUsage(sender);
            return true;
        }

        HouseholdPersistenceService service = requireHouseholdPersistence(sender);
        if (service == null) {
            return true;
        }

        Integer npcId = parseIntegerStrict(args[3]);
        if (npcId == null) {
            AINPC npc = resolveWorldBindNpc(sender, args[3]);
            if (npc == null) {
                return true;
            }
            npcId = npc.getDatabaseId();
        }
        if (npcId == null || npcId <= 0) {
            plugin.getMessageUtils().send(sender, "&cNPC-ul selectat nu are ID DB valid.");
            return true;
        }

        try {
            Optional<HouseholdPersistenceService.HouseholdResidentRecord> resident =
                service.findResidentByNpcId(npcId);
            if (resident.isEmpty()) {
                plugin.getMessageUtils().send(sender,
                    "&cNu exista resident household persistent pentru NPC id &e" + npcId + "&c.");
                return true;
            }

            plugin.getMessageUtils().send(sender, "&6=== Household Resident ===");
            sendPersistentHouseholdResident(sender, resident.get());
            service.getHousehold(resident.get().householdId())
                .ifPresent(household -> sendPersistentHouseholdCompact(sender, household));
        } catch (SQLException exception) {
            plugin.getMessageUtils().send(sender,
                "&cNu am putut citi residentul household: &e" + exception.getMessage());
        }
        return true;
    }

    private boolean handleWorldHouseholdList(CommandSender sender, String[] args) {
        if (args.length > 4) {
            sendWorldHouseholdUsage(sender);
            return true;
        }

        HouseholdPersistenceService service = requireHouseholdPersistence(sender);
        if (service == null) {
            return true;
        }

        int limit = HOUSEHOLD_DEFAULT_LIMIT;
        if (args.length == 4) {
            Integer parsedLimit = parseIntegerStrict(args[3]);
            if (parsedLimit == null || parsedLimit <= 0) {
                plugin.getMessageUtils().send(sender, "&cLimit trebuie sa fie un numar pozitiv.");
                return true;
            }
            limit = Math.min(parsedLimit, HOUSEHOLD_MAX_LIMIT);
            if (parsedLimit > HOUSEHOLD_MAX_LIMIT) {
                plugin.getMessageUtils().send(sender,
                    "&eLimit maxim pentru afisare: &f" + HOUSEHOLD_MAX_LIMIT + "&e.");
            }
        }

        try {
            List<HouseholdPersistenceService.HouseholdRecord> households = service.listHouseholds(limit);
            plugin.getMessageUtils().send(sender, "&6=== Household-uri Persistente ===");
            plugin.getMessageUtils().send(sender,
                "&eTotal DB: &f" + service.countHouseholds()
                    + " &7| Rezidenti: &f" + service.countResidents()
                    + " &7| Afisate: &f" + households.size());
            if (households.isEmpty()) {
                plugin.getMessageUtils().send(sender,
                    "&7Nu exista inca household-uri persistente. Ruleaza un household/settlement spawn controlat.");
                return true;
            }
            for (HouseholdPersistenceService.HouseholdRecord household : households) {
                sendPersistentHouseholdCompact(sender, household);
            }
        } catch (SQLException exception) {
            plugin.getMessageUtils().send(sender,
                "&cNu am putut lista household-urile persistente: &e" + exception.getMessage());
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

    private HouseholdPersistenceService requireHouseholdPersistence(CommandSender sender) {
        HouseholdPersistenceService service = plugin.getHouseholdPersistenceService();
        if (service == null) {
            plugin.getMessageUtils().send(sender, "&cHousehold persistence nu este initializata.");
            return null;
        }
        return service;
    }

    private void sendWorldHouseholdUsage(CommandSender sender) {
        plugin.getMessageUtils().send(sender, "&cUtilizare:");
        plugin.getMessageUtils().send(sender, "&e/ainpc world household <plan|spawn> <homePlaceId> [count]");
        plugin.getMessageUtils().send(sender, "&e/ainpc world household status <householdId|homePlaceId>");
        plugin.getMessageUtils().send(sender, "&e/ainpc world household place <homePlaceId>");
        plugin.getMessageUtils().send(sender, "&e/ainpc world household resident <npcId|numeNpc|nearest>");
        plugin.getMessageUtils().send(sender, "&e/ainpc world household list [limit]");
        plugin.getMessageUtils().send(sender,
            "&7Comenzile status/place/resident/list sunt read-only si inspecteaza tabelele households.");
    }

    private void sendPersistentHouseholdSummary(CommandSender sender,
                                                HouseholdPersistenceService.HouseholdRecord household,
                                                List<HouseholdPersistenceService.HouseholdResidentRecord> residents) {
        plugin.getMessageUtils().send(sender, "&6=== Household Persistent ===");
        plugin.getMessageUtils().send(sender, "&eID: &f" + household.householdId());
        plugin.getMessageUtils().send(sender, "&eCasa: &f" + formatOptional(household.homePlaceId()));
        plugin.getMessageUtils().send(sender, "&eFamily: &f" + formatOptional(household.familyId()));
        plugin.getMessageUtils().send(sender, "&eOwner key: &f" + formatOptional(household.primaryOwnerKey()));
        plugin.getMessageUtils().send(sender,
            "&eRezidenti: &f" + residents.size()
                + " &7/ DB &f" + household.residentCount()
                + " &7/ max &f" + household.maxResidents());
        plugin.getMessageUtils().send(sender,
            "&eSource: &f" + formatOptional(household.source())
                + " &7| Update: &f" + formatStoryTime(household.updatedAt()));
        plugin.getMessageUtils().send(sender, "&ePlan hash: &f" + formatOptional(household.planHash()));

        if (residents.isEmpty()) {
            plugin.getMessageUtils().send(sender, "&eWarning: &fhousehold-ul nu are rezidenti persistenti.");
            return;
        }

        for (HouseholdPersistenceService.HouseholdResidentRecord resident : residents.stream().limit(8).toList()) {
            sendPersistentHouseholdResident(sender, resident);
        }
        if (residents.size() > 8) {
            plugin.getMessageUtils().send(sender,
                "&7... inca " + (residents.size() - 8) + " rezidenti.");
        }
    }

    private void sendPersistentHouseholdCompact(CommandSender sender,
                                                HouseholdPersistenceService.HouseholdRecord household) {
        plugin.getMessageUtils().send(sender,
            "&7- &f" + household.householdId()
                + " &7casa=&f" + formatOptional(household.homePlaceId())
                + " &7family=&f" + formatOptional(household.familyId())
                + " &7rezidenti=&f" + household.residentCount()
                + "/" + household.maxResidents()
                + " &7update=&f" + formatStoryTime(household.updatedAt()));
    }

    private void sendPersistentHouseholdResident(CommandSender sender,
                                                 HouseholdPersistenceService.HouseholdResidentRecord resident) {
        plugin.getMessageUtils().send(sender,
            "&7- &f" + resident.residentKey()
                + " &7npc=&f" + resident.npcName() + "#" + resident.npcId()
                + " &7rol=&f" + formatOptional(resident.relationRole())
                + " &7household=&f" + resident.householdId());
        plugin.getMessageUtils().send(sender,
            "  &7home=&f" + formatOptional(resident.homePlaceId())
                + " &7bed=&f" + formatOptional(resident.homeNodeId())
                + " &7work=&f" + formatOptional(resident.workPlaceId())
                + " &7source=&f" + formatOptional(resident.sourceKey()));
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
            List<String> created = result.spawnResults().stream()
                .filter(NpcSpawnResult::success)
                .filter(NpcSpawnResult::created)
                .map(spawnResult -> spawnResult.npc().getName() + "#" + spawnResult.npc().getDatabaseId())
                .toList();
            if (!created.isEmpty()) {
                plugin.getMessageUtils().send(sender, "&eNPC-uri create: &f" + formatList(created));
            }

            List<String> reused = result.spawnResults().stream()
                .filter(NpcSpawnResult::success)
                .filter(spawnResult -> !spawnResult.created())
                .map(spawnResult -> spawnResult.npc().getName() + "#" + spawnResult.npc().getDatabaseId())
                .toList();
            if (!reused.isEmpty()) {
                plugin.getMessageUtils().send(sender, "&eNPC-uri reutilizate: &f" + formatList(reused));
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
                saveNpcWorldBinding(sender, NpcWorldBinding.fromSpawnPlan(npc, plan, "spawn_plan"), false);
                boundCount++;
            } catch (IllegalArgumentException exception) {
                plugin.getMessageUtils().send(sender, "&eWarning: &f" + exception.getMessage());
            }
        }
        plugin.getMessageUtils().send(sender, "&eBind-uri mapping actualizate: &f" + boundCount);
    }

    private boolean saveNpcWorldBinding(CommandSender sender, NpcWorldBinding binding, boolean mergeExisting) {
        if (plugin.getNpcWorldBindingService() == null) {
            plugin.getMessageUtils().send(sender,
                "&eWarning: &fnpc_world_bindings nu este disponibil; ramane fallback-ul profile_data/metadata.");
            return false;
        }

        try {
            NpcWorldBinding toSave = binding;
            if (mergeExisting) {
                toSave = plugin.getNpcWorldBindingService()
                    .getBinding(binding.npcId())
                    .map(binding::mergeMissingFrom)
                    .orElse(binding);
            }
            plugin.getNpcWorldBindingService().saveBinding(toSave);
            return true;
        } catch (SQLException | IllegalArgumentException exception) {
            plugin.getMessageUtils().send(sender,
                "&eWarning: &fNu am putut salva npc_world_bindings pentru npc_id="
                    + binding.npcId() + ": " + exception.getMessage());
            return false;
        }
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
        plugin.getMessageUtils().send(sender, "&7  Din consola/RCON foloseste spawn-ul lumii incarcate");
        plugin.getMessageUtils().send(sender, "&e/ainpc world bind npc <numeNpc|nearest> <homePlaceId> [workPlaceId|-] [socialPlaceId|-]");
        plugin.getMessageUtils().send(sender, "&e/ainpc world bindings [limit]");
        plugin.getMessageUtils().send(sender, "&e/ainpc world bindings npc <numeNpc|nearest|npcId|uuid>");
        plugin.getMessageUtils().send(sender, "&e/ainpc world bindings place <placeId> [limit]");
        plugin.getMessageUtils().send(sender, "&e/ainpc world household <plan|spawn> <homePlaceId> [count]");
        plugin.getMessageUtils().send(sender, "&e/ainpc world household <status|place|resident|list> ...");
        plugin.getMessageUtils().send(sender, "&e/ainpc world settlement <plan|spawn> <regionId> [maxHouses]");
        plugin.getMessageUtils().send(sender, "&e/ainpc world save");
    }

    private boolean handleMigration(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ainpc.admin")) {
            plugin.getMessageUtils().sendMessage(sender, "no_permission");
            return true;
        }

        if (args.length < 3
            || !"households".equalsIgnoreCase(args[1])
            || (!"dryrun".equalsIgnoreCase(args[2]) && !"apply".equalsIgnoreCase(args[2]))
            || args.length > 4) {
            sendMigrationUsage(sender);
            return true;
        }

        int limit = NPC_WORLD_BINDING_LOOKUP_LIMIT;
        if (args.length == 4) {
            Integer parsedLimit = parseIntegerStrict(args[3]);
            if (parsedLimit == null || parsedLimit <= 0) {
                plugin.getMessageUtils().send(sender, "&cLimit trebuie sa fie un numar pozitiv.");
                return true;
            }
            limit = Math.min(parsedLimit, 1000);
            if (parsedLimit > 1000) {
                plugin.getMessageUtils().send(sender, "&eLimit maxim pentru migration households: &f1000&e.");
            }
        }

        HouseholdPersistenceService service = requireHouseholdPersistence(sender);
        if (service == null) {
            return true;
        }

        boolean apply = "apply".equalsIgnoreCase(args[2]);
        try {
            HouseholdPersistenceService.HouseholdBackfillReport bindingReport =
                service.backfillFromNpcWorldBindings(apply, limit);
            sendHouseholdBackfillReport(sender, "npc_world_bindings", bindingReport);

            HouseholdMetadataBackfillInputs metadataInputs = collectHouseholdMetadataBackfillInputs(limit);
            sendAuditMessages(sender, "&eWarning-uri metadata migration", metadataInputs.warnings());
            HouseholdPersistenceService.HouseholdBackfillReport metadataReport =
                service.backfillFromMetadataResidents(apply, limit, metadataInputs.inputs());
            sendHouseholdBackfillReport(sender, "metadata resident_npc_ids", metadataReport);
        } catch (SQLException exception) {
            plugin.getMessageUtils().send(sender,
                "&cMigration households a esuat: &e" + exception.getMessage());
        }
        return true;
    }

    private void sendMigrationUsage(CommandSender sender) {
        plugin.getMessageUtils().send(sender, "&cUtilizare:");
        plugin.getMessageUtils().send(sender, "&e/ainpc migration households dryrun [limit]");
        plugin.getMessageUtils().send(sender, "&e/ainpc migration households apply [limit]");
        plugin.getMessageUtils().send(sender,
            "&7Backfill controlat din npc_world_bindings si metadata resident_npc_ids.");
    }

    private HouseholdMetadataBackfillInputs collectHouseholdMetadataBackfillInputs(int limit) {
        WorldAdminApi worldAdmin = plugin.getPlatform() != null ? plugin.getPlatform().getWorldAdmin() : null;
        if (worldAdmin == null || !worldAdmin.isEnabled()) {
            return new HouseholdMetadataBackfillInputs(List.of(),
                List.of("World admin este dezactivat; sar peste metadata resident_npc_ids."));
        }

        List<HouseholdPersistenceService.MetadataResidentBackfillInput> inputs = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int safeLimit = Math.max(1, Math.min(1000, limit));

        for (WorldPlaceInfo place : worldAdmin.getPlaces()) {
            if (inputs.size() >= safeLimit) {
                break;
            }
            if (!isHousePlace(place)) {
                continue;
            }

            String familyId = firstNonBlank(
                place.metadata().get("family_id"),
                place.metadata().get("household_id"),
                place.metadata().get("household")
            );
            for (String residentSelector : parseResidents(place)) {
                if (inputs.size() >= safeLimit) {
                    break;
                }
                Integer npcId = parseNpcIdSelector(residentSelector);
                if (npcId == null || npcId <= 0) {
                    AINPC npc = findLoadedNpcBySelector(residentSelector);
                    if (npc != null && npc.getDatabaseId() > 0) {
                        npcId = npc.getDatabaseId();
                    }
                }
                if (npcId == null || npcId <= 0) {
                    if (warnings.size() < AUDIT_PREVIEW_LIMIT) {
                        warnings.add("Nu pot rezolva resident_npc_ids=" + residentSelector
                            + " pentru " + place.id() + ".");
                    }
                    continue;
                }

                String key = place.id() + ":" + npcId;
                if (seen.add(key)) {
                    inputs.add(new HouseholdPersistenceService.MetadataResidentBackfillInput(
                        place.id(),
                        familyId,
                        npcId
                    ));
                }
            }
        }

        return new HouseholdMetadataBackfillInputs(List.copyOf(inputs), List.copyOf(warnings));
    }

    private void sendHouseholdBackfillReport(CommandSender sender,
                                             String sourceLabel,
                                             HouseholdPersistenceService.HouseholdBackfillReport report) {
        String mode = report.apply() ? "apply" : "dry-run";
        plugin.getMessageUtils().send(sender, "&6=== Migration Households " + mode
            + " [" + sourceLabel + "] ===");
        plugin.getMessageUtils().send(sender,
            "&eBindings scanate: &f" + report.scannedBindings()
                + " &7| Household candidati: &f" + report.candidateHouseholds());
        plugin.getMessageUtils().send(sender,
            "&eHousehold create/update: &f" + report.householdsCreated()
                + "/" + report.householdsUpdated()
                + " &7| Rezidenti noi: &f" + report.residentsCreated()
                + " &7| deja existenti: &f" + report.residentsAlreadyPresent()
                + " &7| sariti: &f" + report.skippedResidents());
        sendAuditMessages(sender, report.apply() ? "&aActiuni migration" : "&eActiuni dry-run", report.actions());
        sendAuditMessages(sender, "&eWarning-uri migration", report.warnings());
        sendAuditMessages(sender, "&cErori migration", report.errors());
        if (!report.apply()) {
            plugin.getMessageUtils().send(sender,
                "&7Dry-run read-only. Pentru scriere: &f/ainpc migration households apply");
        } else {
            plugin.getMessageUtils().send(sender,
                "&7Migration apply terminat. Ruleaza &f/ainpc audit db &7si &f/ainpc world household list&7.");
        }
    }

    private boolean handleAudit(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ainpc.admin")) {
            plugin.getMessageUtils().sendMessage(sender, "no_permission");
            return true;
        }

        String mode = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "all";
        if (!Set.of("all", "npc", "world", "db", "spawn", "quest", "wand").contains(mode)) {
            sendAuditUsage(sender);
            return true;
        }
        String option = args.length > 2 ? args[2].toLowerCase(Locale.ROOT) : "";
        if (args.length > 3 || !isAuditOptionSupported(mode, option)) {
            sendAuditUsage(sender);
            return true;
        }
        boolean strictQuestAnchorAudit = isStrictQuestAuditOption(option);

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
            auditQuestAnchors(report, strictQuestAnchorAudit);
        }
        if ("all".equals(mode) || "world".equals(mode) || "wand".equals(mode)) {
            auditMappingWandDrafts(report);
        }

        sendAuditReport(sender, auditModeLabel(mode, option), report);
        return true;
    }

    private boolean isAuditOptionSupported(String mode, String option) {
        if (option == null || option.isBlank()) {
            return true;
        }
        return ("quest".equals(mode) || "all".equals(mode)) && isStrictQuestAuditOption(option);
    }

    private boolean isStrictQuestAuditOption(String option) {
        return "strict".equals(option) || "full".equals(option) || "offline".equals(option);
    }

    private String auditModeLabel(String mode, String option) {
        return option == null || option.isBlank() ? mode : mode + " " + option;
    }

    private void sendAuditUsage(CommandSender sender) {
        plugin.getMessageUtils().send(sender, "&cUtilizare:");
        plugin.getMessageUtils().send(sender, "&e/ainpc audit &7- ruleaza toate verificarile");
        plugin.getMessageUtils().send(sender, "&e/ainpc audit npc &7- verifica profilurile si ancorele NPC");
        plugin.getMessageUtils().send(sender, "&e/ainpc audit world &7- verifica world mapping");
        plugin.getMessageUtils().send(sender, "&e/ainpc audit db &7- verifica tabelele si profile_data");
        plugin.getMessageUtils().send(sender, "&e/ainpc audit spawn &7- verifica ordinea casa/node/NPC/familie");
        plugin.getMessageUtils().send(sender, "&e/ainpc audit quest &7- verifica quest templates si quest_anchor_bindings");
        plugin.getMessageUtils().send(sender,
            "&e/ainpc audit quest <strict|full|offline> &7- verifica toate randurile quest_anchor_bindings");
        plugin.getMessageUtils().send(sender, "&e/ainpc audit wand &7- verifica draft-urile wand confirmate recent");
    }

    private boolean handleDebugDump(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ainpc.admin")) {
            plugin.getMessageUtils().sendMessage(sender, "no_permission");
            return true;
        }

        String scope = args.length > 1 ? args[1].toLowerCase() : "all";
        if (!Set.of("all", "npc", "world", "quest", "story", "openai").contains(scope)) {
            plugin.getMessageUtils().send(sender, "&cUtilizare: /ainpc debugdump [all|npc|world|quest|story|openai]");
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
        Map<String, List<AINPC>> npcsBySourceKey = new HashMap<>();

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
            if (npc.getSourceKey() != null && !npc.getSourceKey().isBlank()) {
                npcsBySourceKey.computeIfAbsent(normalizeAuditKey(npc.getSourceKey()), ignored -> new ArrayList<>()).add(npc);
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

        for (Map.Entry<String, List<AINPC>> entry : npcsBySourceKey.entrySet()) {
            if (entry.getValue().size() > 1) {
                String duplicateIds = entry.getValue().stream()
                    .map(npc -> npc.getName() + "#" + npc.getDatabaseId())
                    .toList()
                    .toString();
                report.error("Exista mai multi NPC cu acelasi source_key: "
                    + entry.getKey() + " (" + entry.getValue().size() + "): " + duplicateIds + ".");
            }
        }

        for (var issue : plugin.getNpcManager().auditManagedVillagerEntities()) {
            if (issue.error()) {
                report.error(issue.message());
            } else {
                report.warn(issue.message());
            }
        }

        for (var issue : plugin.getNpcManager().auditPersistentSourceKeyIndex()) {
            if (issue.error()) {
                report.error(issue.message());
            } else {
                report.warn(issue.message());
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

    private void auditMappingWandDrafts(AuditReport report) {
        MappingWandService service = plugin.getMappingWandService();
        if (service == null) {
            report.warn("MappingWandService este indisponibil; nu pot audita draft-urile wand recente.");
            return;
        }

        List<MappingWandService.MappingWandAuditEntry> entries = service.recentConfirmedDrafts();
        report.info("Wand draft-uri confirmate recent: " + entries.size() + ".");
        if (entries.isEmpty()) {
            return;
        }

        WorldAdminApi worldAdmin = plugin.getPlatform() != null ? plugin.getPlatform().getWorldAdmin() : null;
        boolean canValidateWorld = worldAdmin != null && worldAdmin.isEnabled();
        int limit = Math.min(AUDIT_PREVIEW_LIMIT, entries.size());
        for (int i = 0; i < limit; i++) {
            MappingWandService.MappingWandAuditEntry entry = entries.get(i);
            report.info("Wand " + formatStoryTime(entry.confirmedAt())
                + " player=" + entry.playerName()
                + " kind=" + formatMappingDraftKind(entry.kind())
                + " draft=" + formatOptional(entry.qualifiedId())
                + " result=" + formatOptional(entry.resultId())
                + " (" + formatOptional(entry.resultMessage()) + ").");
            if (canValidateWorld) {
                validateMappingWandAuditEntry(report, entry, worldAdmin);
            }
        }

        if (entries.size() > limit) {
            report.info("Wand audit afiseaza ultimele " + limit + " din " + entries.size()
                + " confirmari pastrate in memorie.");
        }
        if (!canValidateWorld) {
            report.warn("World admin este dezactivat sau indisponibil; auditul wand nu poate valida tintele din mapping.");
        }
    }

    private void validateMappingWandAuditEntry(AuditReport report,
                                               MappingWandService.MappingWandAuditEntry entry,
                                               WorldAdminApi worldAdmin) {
        String label = "Wand draft recent " + formatOptional(entry.qualifiedId());
        if (entry.kind() == null) {
            report.warn(label + " nu are kind setat.");
            return;
        }

        switch (entry.kind()) {
            case REGION -> {
                if (entry.resultId().isBlank() || worldAdmin.getRegion(entry.resultId()) == null) {
                    report.error(label + " a confirmat o regiune care nu mai exista: "
                        + formatOptional(entry.resultId()) + ".");
                }
            }
            case PLACE -> {
                if (entry.resultId().isBlank() || worldAdmin.getPlace(entry.resultId()) == null) {
                    report.error(label + " a confirmat un place care nu mai exista: "
                        + formatOptional(entry.resultId()) + ".");
                }
            }
            case NODE -> {
                if (entry.resultId().isBlank() || worldAdmin.getNode(entry.resultId()) == null) {
                    report.error(label + " a confirmat un node care nu mai exista: "
                        + formatOptional(entry.resultId()) + ".");
                }
            }
            case NPC_BIND -> {
                if (entry.placeId().isBlank() || worldAdmin.getPlace(entry.placeId()) == null) {
                    report.error(label + " a confirmat un npc_bind catre place inexistent: "
                        + formatOptional(entry.placeId()) + ".");
                }
            }
            case QUEST_ANCHOR -> {
                String anchorType = entry.metadata().getOrDefault("anchor_type", "");
                String anchorId = entry.metadata().getOrDefault("anchor_id", "");
                if (anchorType.isBlank() || anchorId.isBlank()) {
                    report.warn(label + " nu are anchor_type/anchor_id in metadata.");
                } else if (!questAnchorTargetExists(anchorType, anchorId)) {
                    report.error(label + " a confirmat quest_anchor catre tinta inexistenta: "
                        + anchorType + ":" + anchorId + ".");
                }
            }
        }
    }

    private String formatMappingDraftKind(MappingDraftKind kind) {
        return kind != null ? kind.id() : "<nesetat>";
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

    private void auditQuestAnchors(AuditReport report, boolean strict) {
        auditQuestTemplates(report);

        if (plugin.getDatabaseManager() == null) {
            report.error("DatabaseManager nu este initializat.");
            return;
        }

        try {
            int questRows = queryCount("SELECT COUNT(*) FROM player_quests");
            int anchorRows = queryCount("SELECT COUNT(*) FROM quest_anchor_bindings");
            report.info("Quest anchors: " + anchorRows + " binding-uri, " + questRows + " progres quest in DB.");
            int trackedRows = queryCount("SELECT COUNT(*) FROM player_quests WHERE tracked <> 0");
            report.info("Quest tracking: " + trackedRows + " questuri urmarite persistent.");
            auditStoredProgressions(report);

            auditQueryErrorRows(report, """
                SELECT player_uuid, COUNT(*) AS tracked_count
                FROM player_quests
                WHERE tracked <> 0
                GROUP BY player_uuid
                HAVING COUNT(*) > 1
                """, "Jucator cu mai multe questuri tracked");

            auditQueryErrorRows(report, """
                SELECT player_uuid, template_id, quest_code, status
                FROM player_quests
                WHERE tracked <> 0
                  AND LOWER(COALESCE(status, '')) <> 'active'
                """, "Quest tracked care nu este activ");
            auditLegacyQuestProgressKeys(report);

            auditQueryRows(report, """
                SELECT b.player_uuid, b.template_id, b.objective_key
                FROM quest_anchor_bindings b
                LEFT JOIN player_quests p
                  ON p.player_uuid = b.player_uuid AND p.template_id = b.template_id
                WHERE p.player_uuid IS NULL
                """, "Quest anchor fara progres parinte");
            auditQueryRows(report, """
                SELECT player_uuid, template_id, objective_key
                FROM quest_anchor_bindings
                WHERE objective_key LIKE '%:%:%'
                LIMIT 20
                """, "Quest anchor cu objective_key legacy");
            auditStoryPersistence(report);

            int auditLimit = strict ? 0 : QUEST_ANCHOR_AUDIT_DEFAULT_LIMIT;
            List<QuestAnchorBindingRow> rows = queryQuestAnchorBindings("", "", auditLimit);
            report.info("Quest anchor audit: " + rows.size() + "/" + anchorRows
                + " binding-uri verificate" + (strict ? " in mod strict." : "."));
            if (!strict && anchorRows > rows.size()) {
                report.warn("Audit quest anchors a verificat primele " + rows.size()
                    + " randuri din " + anchorRows + ". Ruleaza `/ainpc audit quest strict` pentru audit complet.");
            }
            validateQuestAnchorRows(report, rows);
        } catch (SQLException exception) {
            report.error("Audit quest anchors esuat: " + exception.getMessage());
        }
    }

    private void auditStoredProgressions(AuditReport report) throws SQLException {
        if (plugin.getProgressionService() == null) {
            report.warn("ProgressionService indisponibil; nu pot agrega progresiile persistate.");
            return;
        }

        StoredProgressionSummary summary = plugin.getProgressionService()
            .getStoredProgressionSummary("", "");
        report.info("Progression persistence: " + summary.rowCount()
            + " randuri, " + summary.playerCount() + " jucatori, "
            + summary.currentCount() + " curente, " + summary.archivedCount()
            + " arhivate, " + summary.trackedCount() + " tracked.");
        report.info("Progression status: " + formatCountMap(summary.byStatus()));
        report.info("Progression mechanics: " + formatCountMap(summary.byMechanic()));
        report.info("Progression scenario kinds: " + formatCountMap(summary.byScenarioKind()));
        report.info("Progression base types: " + formatCountMap(summary.byBaseType()));

        if (summary.unresolvedDefinitionCount() > 0) {
            report.warn("Exista " + summary.unresolvedDefinitionCount()
                + " progresii persistate fara definitie incarcata in feature packs.");
        }
        if (summary.byStatus().containsKey("unknown")) {
            report.warn("Exista progresii persistate fara status valid.");
        }
        if (summary.byMechanic().containsKey("unknown") && summary.rowCount() > 0) {
            report.warn("Exista progresii persistate fara mecanica rezolvata.");
        }
    }

    private void auditStoryPersistence(AuditReport report) throws SQLException {
        int regionRows = queryCount("SELECT COUNT(*) FROM region_story_state");
        int placeRows = queryCount("SELECT COUNT(*) FROM place_story_state");
        int eventRows = queryCount("SELECT COUNT(*) FROM story_events");
        report.info("Story persistence: " + regionRows + " stari regionale, "
            + placeRows + " stari place, " + eventRows + " evenimente.");

        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement("""
                 SELECT region_id, story_pool, variables
                 FROM region_story_state
                 ORDER BY updated_at DESC, region_id
                 LIMIT 500
             """);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String label = "region_id=" + rs.getString("region_id");
                auditStoryJsonColumn(report, "region_story_state.story_pool", label, rs.getString("story_pool"), true);
                auditStoryJsonColumn(report, "region_story_state.variables", label, rs.getString("variables"), false);
            }
        }

        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement("""
                 SELECT place_id, variables
                 FROM place_story_state
                 ORDER BY updated_at DESC, place_id
                 LIMIT 500
             """);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                auditStoryJsonColumn(report,
                    "place_story_state.variables",
                    "place_id=" + rs.getString("place_id"),
                    rs.getString("variables"),
                    false);
            }
        }

        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement("""
                 SELECT id, payload
                 FROM story_events
                 ORDER BY created_at DESC, id DESC
                 LIMIT 500
             """);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                auditStoryJsonColumn(report,
                    "story_events.payload",
                    "id=" + rs.getLong("id"),
                    rs.getString("payload"),
                    false);
            }
        }

        auditStoryProgressionConsistency(report);
    }

    private void auditStoryJsonColumn(AuditReport report,
                                      String column,
                                      String label,
                                      String rawValue,
                                      boolean arrayExpected) {
        String safeRawValue = rawValue == null || rawValue.isBlank()
            ? (arrayExpected ? "[]" : "{}")
            : rawValue;
        try {
            JsonElement parsed = JsonParser.parseString(safeRawValue);
            if (arrayExpected && !parsed.isJsonArray()) {
                report.warn(column + " nu este array JSON pentru " + label + ".");
            } else if (!arrayExpected && !parsed.isJsonObject()) {
                report.warn(column + " nu este obiect JSON pentru " + label + ".");
            }
        } catch (JsonSyntaxException exception) {
            report.warn(column + " are JSON invalid pentru " + label + ": " + exception.getMessage());
        }
    }

    private void auditStoryProgressionConsistency(AuditReport report) throws SQLException {
        Map<String, FeaturePackLoader.ScenarioDefinition> scenariosBySelector = buildProgressionScenarioLookup();
        if (scenariosBySelector.isEmpty()) {
            return;
        }

        Set<String> storyEventProgressionKeys = queryStoryEventProgressionKeys(report);
        String completedProgressionsSql = """
            SELECT player_uuid, template_id, quest_code, status
            FROM player_quests
            WHERE LOWER(COALESCE(status, '')) IN ('completed', 'complete', 'done')
            ORDER BY completed_at DESC, updated_at DESC
            LIMIT 500
        """;

        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement(completedProgressionsSql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String playerUuid = rs.getString("player_uuid");
                String templateId = rs.getString("template_id");
                String questCode = rs.getString("quest_code");
                FeaturePackLoader.ScenarioDefinition scenario =
                    findScenarioForProgressionRow(templateId, questCode, scenariosBySelector);
                if (scenario == null || !hasRecordStoryEventAction(scenario)) {
                    continue;
                }
                if (hasStoryEventProgressionKey(storyEventProgressionKeys, playerUuid, templateId, questCode)) {
                    continue;
                }
                report.warn("Progresie completata cu record_story_event fara story_event asociat detectabil: player_uuid="
                    + playerUuid + ", template_id=" + templateId + ", quest_code=" + questCode
                    + ". Verifica payload.quest_template/quest_code in story_events.");
            }
        }
    }

    private Set<String> queryStoryEventProgressionKeys(AuditReport report) throws SQLException {
        Set<String> keys = new HashSet<>();
        String storyEventSql = """
            SELECT id, player_uuid, event_key, actor_type, payload
            FROM story_events
            ORDER BY created_at DESC, id DESC
            LIMIT 1000
        """;

        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement(storyEventSql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                long id = rs.getLong("id");
                String playerUuid = safeAuditValue(rs.getString("player_uuid"));
                String actorType = safeAuditValue(rs.getString("actor_type"));
                String eventKey = safeAuditValue(rs.getString("event_key"));
                JsonElement payload = parseStoredJsonObject(rs.getString("payload"));
                String questTemplate = jsonString(payload, "quest_template");
                String questCodeFromPayload = jsonString(payload, "quest_code");
                String questCode = firstNonBlank(questCodeFromPayload, eventKey);
                String payloadPlayerUuid = jsonString(payload, "player_uuid");
                String effectivePlayerUuid = firstNonBlank(playerUuid, payloadPlayerUuid);

                addStoryEventProgressionKey(keys, effectivePlayerUuid, questTemplate);
                addStoryEventProgressionKey(keys, effectivePlayerUuid, questCode);

                boolean questLikeEvent = "quest".equalsIgnoreCase(actorType)
                    || !questTemplate.isBlank()
                    || !questCodeFromPayload.isBlank();
                if (questLikeEvent && questTemplate.isBlank() && questCode.isBlank()) {
                    report.warn("story_events id=" + id
                        + " pare legat de quest, dar nu are payload.quest_template sau payload.quest_code.");
                }
            }
        }
        return keys;
    }

    private boolean hasStoryEventProgressionKey(Set<String> keys,
                                                String playerUuid,
                                                String templateId,
                                                String questCode) {
        return keys.contains(storyEventProgressionKey(playerUuid, templateId))
            || keys.contains(storyEventProgressionKey(playerUuid, questCode))
            || keys.contains(storyEventProgressionKey("", templateId))
            || keys.contains(storyEventProgressionKey("", questCode));
    }

    private void addStoryEventProgressionKey(Set<String> keys, String playerUuid, String selector) {
        String key = storyEventProgressionKey(playerUuid, selector);
        if (!key.isBlank()) {
            keys.add(key);
        }
    }

    private String storyEventProgressionKey(String playerUuid, String selector) {
        String normalizedSelector = normalizeQuestObjectiveLookupKey(selector);
        if (normalizedSelector.isBlank()) {
            return "";
        }
        return safeAuditValue(playerUuid) + "|" + normalizedSelector;
    }

    private FeaturePackLoader.ScenarioDefinition findScenarioForProgressionRow(
        String templateId,
        String questCode,
        Map<String, FeaturePackLoader.ScenarioDefinition> scenariosBySelector
    ) {
        if (scenariosBySelector == null || scenariosBySelector.isEmpty()) {
            return null;
        }
        FeaturePackLoader.ScenarioDefinition scenario =
            scenariosBySelector.get(normalizeQuestObjectiveLookupKey(templateId));
        if (scenario != null) {
            return scenario;
        }
        scenario = scenariosBySelector.get(normalizeQuestObjectiveLookupKey(questCode));
        if (scenario != null) {
            return scenario;
        }
        return scenariosBySelector.get(normalizeQuestObjectiveLookupKey(lastSelectorSegment(templateId)));
    }

    private boolean hasRecordStoryEventAction(FeaturePackLoader.ScenarioDefinition scenario) {
        if (scenario == null) {
            return false;
        }
        for (FeaturePackLoader.QuestEntryDefinition reward : scenario.getRewards()) {
            if ("record_story_event".equals(normalizeQuestRewardType(reward.getType()))) {
                return true;
            }
        }
        return false;
    }

    private JsonElement parseStoredJsonObject(String rawValue) {
        String safeRawValue = rawValue == null || rawValue.isBlank() ? "{}" : rawValue;
        try {
            JsonElement parsed = JsonParser.parseString(safeRawValue);
            return parsed != null && parsed.isJsonObject() ? parsed : null;
        } catch (JsonSyntaxException ignored) {
            return null;
        }
    }

    private void auditQuestTemplates(AuditReport report) {
        FeaturePackLoader featurePackLoader = plugin.getFeaturePackLoader();
        if (featurePackLoader == null) {
            report.warn("FeaturePackLoader indisponibil; nu pot valida quest templates.");
            return;
        }

        List<FeaturePackLoader.ScenarioDefinition> quests = featurePackLoader.getAllScenarios().stream()
            .filter(this::isQuestAuditCandidate)
            .toList();

        report.info("Progression mechanics: "
            + featurePackLoader.getAllProgressionMechanics().size()
            + " mecanici definite in feature packs.");
        validateProgressionMechanicDefinitions(report, featurePackLoader.getAllProgressionMechanics());

        report.info("Quest/progression templates: " + quests.size() + " definitii jucabile in feature packs.");
        if (quests.isEmpty()) {
            report.warn("Nu exista quest/progression templates incarcate din feature packs.");
            return;
        }

        Set<String> knownQuestReferences = collectKnownQuestReferences(quests);
        WorldMappingSemanticIndex worldSemanticIndex = buildWorldMappingSemanticIndexForAudit();
        Map<String, String> questCodes = new HashMap<>();
        for (FeaturePackLoader.ScenarioDefinition quest : quests) {
            String label = "Quest template " + quest.getPackId() + ":" + quest.getId()
                + " (" + formatOptional(quest.getName()) + ")";
            validateQuestTemplate(report, featurePackLoader, label, quest, knownQuestReferences, questCodes, worldSemanticIndex);
        }
    }

    private WorldMappingSemanticIndex buildWorldMappingSemanticIndexForAudit() {
        WorldAdminApi worldAdmin = plugin.getPlatform() != null ? plugin.getPlatform().getWorldAdmin() : null;
        if (worldAdmin == null || !worldAdmin.isEnabled()) {
            return null;
        }

        WorldMappingSemanticIndex index = WorldMappingSemanticIndex.from(
            worldAdmin.getRegions(),
            worldAdmin.getPlaces(),
            worldAdmin.getNodes()
        );
        return index.hasAnyCandidates() ? index : null;
    }

    private boolean isQuestAuditCandidate(FeaturePackLoader.ScenarioDefinition scenario) {
        if (scenario == null) {
            return false;
        }
        return scenario.getBaseType() == ScenarioEngine.ScenarioType.QUEST
            || (scenario.isProgressionEnabled()
                && (!scenario.getQuestCode().isBlank()
                    || !scenario.getObjectives().isEmpty()
                    || !scenario.getRewards().isEmpty()));
    }

    private Set<String> collectKnownQuestReferences(List<FeaturePackLoader.ScenarioDefinition> quests) {
        Set<String> references = new HashSet<>();
        for (FeaturePackLoader.ScenarioDefinition quest : quests) {
            addQuestReference(references, quest.getId());
            addQuestReference(references, quest.getPackId() + ":" + quest.getId());
            addQuestReference(references, quest.getQuestCode());
        }
        return references;
    }

    private void addQuestReference(Set<String> references, String value) {
        String normalized = normalizeAuditKey(value);
        if (!normalized.isBlank()) {
            references.add(normalized);
        }
    }

    private void validateQuestTemplate(AuditReport report,
                                       FeaturePackLoader featurePackLoader,
                                       String label,
                                       FeaturePackLoader.ScenarioDefinition quest,
                                       Set<String> knownQuestReferences,
                                       Map<String, String> questCodes,
                                       WorldMappingSemanticIndex worldSemanticIndex) {
        if (quest.getQuestCode().isBlank()) {
            report.warn(label + " nu are quest.code; runtime-ul foloseste template_id ca fallback.");
        } else {
            String normalizedCode = normalizeAuditKey(quest.getQuestCode());
            String previous = questCodes.putIfAbsent(normalizedCode, label);
            if (previous != null) {
                report.error(label + " are quest.code duplicat cu " + previous + ": " + quest.getQuestCode() + ".");
            }
        }

        if (!quest.isRequiresPlayer()) {
            report.warn(label + " nu are requires_player=true; questurile jucabile ar trebui sa ceara player.");
        }
        if (quest.getQuestGiverProfession().isBlank()) {
            report.warn(label + " nu are quest.giver_profession; poate cadea pe fallback de NPC.");
        } else if (featurePackLoader.findProfessionDefinition(quest.getQuestGiverProfession()) == null) {
            report.warn(label + " foloseste giver_profession necunoscuta: "
                + quest.getQuestGiverProfession() + ".");
        }

        validateQuestPrerequisites(report, label, quest, knownQuestReferences);
        validateQuestRepeatability(report, label, quest);
        validateQuestPhases(report, label, quest);
        validateQuestDialogues(report, label, quest);
        validateQuestGiverRole(report, label, quest);
        validateQuestProgressionMetadata(report, featurePackLoader, label, quest);
        validateQuestEntries(report, label, "obiectiv", quest.getObjectives(), true, worldSemanticIndex);
        validateQuestEntries(report, label, "recompensa", quest.getRewards(), false);
        validateQuestObjectiveStages(report, label, quest);
    }

    private void validateProgressionMechanicDefinitions(
        AuditReport report,
        Collection<FeaturePackLoader.ProgressionMechanicDefinition> mechanics
    ) {
        if (mechanics == null || mechanics.isEmpty()) {
            report.warn("Nu exista mechanics/progression definite explicit; questurile legacy folosesc fallback intern.");
            return;
        }

        Set<String> keys = new HashSet<>();
        for (FeaturePackLoader.ProgressionMechanicDefinition mechanic : mechanics) {
            if (mechanic == null) {
                continue;
            }

            String label = "Progression mechanic " + mechanic.getPackId() + ":" + mechanic.getId();
            String key = normalizeAuditKey(mechanic.getPackId() + ":" + mechanic.getId());
            if (!keys.add(key)) {
                report.error(label + " este duplicat.");
            }
            if (mechanic.getId().isBlank()) {
                report.error(label + " nu are ID.");
            }
            if (mechanic.getKind().isBlank()) {
                report.warn(label + " nu are kind; UI-ul va folosi ID-ul ca fallback.");
            }
            if (mechanic.getLabel().isBlank()) {
                report.warn(label + " nu are label vizibil.");
            }
            if (mechanic.isProgressEnabled() && mechanic.getMaxActive() == 0) {
                report.info(label + " nu are max_active; se aplica limitele globale/config existente.");
            }
        }
    }

    private void validateQuestProgressionMetadata(AuditReport report,
                                                  FeaturePackLoader featurePackLoader,
                                                  String label,
                                                  FeaturePackLoader.ScenarioDefinition quest) {
        if (!quest.isProgressionEnabled()) {
            report.warn(label + " are progression/progress disabled; nu va fi candidat bun pentru runtime generic.");
            return;
        }

        String mechanicId = quest.getProgressionMechanicId();
        if (mechanicId.isBlank()) {
            report.warn(label + " nu are mechanic/progression.mechanic; quest runtime foloseste fallback.");
            return;
        }

        FeaturePackLoader.ProgressionMechanicDefinition mechanic =
            featurePackLoader.findProgressionMechanicDefinition(quest.getPackId(), mechanicId);
        if (mechanic == null) {
            report.warn(label + " refera progression mechanic necunoscuta: " + mechanicId + ".");
        } else if (!mechanic.isProgressEnabled()) {
            report.warn(label + " foloseste mechanic cu progress=false: "
                + mechanic.getPackId() + ":" + mechanic.getId() + ".");
        }
    }

    private void validateQuestPrerequisites(AuditReport report,
                                            String label,
                                            FeaturePackLoader.ScenarioDefinition quest,
                                            Set<String> knownQuestReferences) {
        for (String prerequisite : quest.getQuestPrerequisites()) {
            String normalizedPrerequisite = normalizeAuditKey(prerequisite);
            if (normalizedPrerequisite.isBlank()) {
                report.warn(label + " are prerequisite gol.");
            } else if (!knownQuestReferences.contains(normalizedPrerequisite)) {
                report.error(label + " cere prerequisite necunoscut: " + prerequisite + ".");
            }
        }
    }

    private void validateQuestRepeatability(AuditReport report,
                                            String label,
                                            FeaturePackLoader.ScenarioDefinition quest) {
        if (quest.isQuestRepeatable() && quest.getQuestCooldownSeconds() <= 0) {
            report.warn(label + " este repeatable, dar nu are cooldown_seconds.");
        }
        if (!quest.isQuestRepeatable() && quest.getQuestCooldownSeconds() > 0) {
            report.warn(label + " are cooldown_seconds, dar repeatable=false.");
        }
    }

    private void validateQuestPhases(AuditReport report,
                                     String label,
                                     FeaturePackLoader.ScenarioDefinition quest) {
        if (quest.getPhases().isEmpty()) {
            report.warn(label + " nu are phases; statusul va fi mai greu de urmarit.");
            return;
        }

        Set<String> phases = new HashSet<>();
        for (String phase : quest.getPhases()) {
            phases.add(normalizeAuditKey(phase));
        }
        for (String requiredPhase : List.of("introduction", "acceptance", "return", "completion")) {
            if (!phases.contains(requiredPhase)) {
                report.warn(label + " nu are faza " + requiredPhase + ".");
            }
        }
    }

    private void validateQuestObjectiveStages(AuditReport report,
                                              String label,
                                              FeaturePackLoader.ScenarioDefinition quest) {
        if (quest.getObjectives().isEmpty()) {
            return;
        }

        Set<String> knownPhases = quest.getPhases().stream()
            .map(phase -> normalizeAuditKey(phase))
            .filter(phase -> !phase.isBlank())
            .collect(Collectors.toSet());
        boolean hasStagedObjective = false;
        boolean hasUnstagedObjective = false;

        for (FeaturePackLoader.QuestEntryDefinition objective : quest.getObjectives()) {
            String stage = questEntryStage(objective);
            if (stage.isBlank()) {
                if (questStageReferencesObjective(quest, objective)) {
                    hasStagedObjective = true;
                } else {
                    hasUnstagedObjective = true;
                }
                continue;
            }

            hasStagedObjective = true;
            String normalizedStage = normalizeAuditKey(stage);
            if (!knownPhases.contains(normalizedStage)) {
                report.error(label + " are obiectiv " + questEntryId(objective)
                    + " cu phase/stage necunoscut: " + stage + ".");
            }
        }

        if (hasStagedObjective && hasUnstagedObjective) {
            report.warn(label + " combina obiective cu phase/stage si obiective fara etapa explicita.");
        }

        validateQuestStageDefinitions(report, label, quest, knownPhases);
    }

    private String questEntryStage(FeaturePackLoader.QuestEntryDefinition entry) {
        if (entry == null) {
            return "";
        }

        return firstNonBlank(
            entry.getMetadata().get("stage_id"),
            entry.getMetadata().get("stage"),
            entry.getMetadata().get("phase"),
            entry.getMetadata().get("current_stage_id"),
            entry.getMetadata().get("current_phase"),
            entry.getVariables().get("stage_id"),
            entry.getVariables().get("stage"),
            entry.getVariables().get("phase")
        );
    }

    private void validateQuestStageDefinitions(AuditReport report,
                                               String label,
                                               FeaturePackLoader.ScenarioDefinition quest,
                                               Set<String> knownPhases) {
        if (quest.getQuestStages().isEmpty()) {
            return;
        }

        Set<String> objectiveReferences = collectQuestObjectiveReferences(quest.getObjectives());
        for (FeaturePackLoader.QuestStageDefinition stage : quest.getQuestStages()) {
            if (stage == null || stage.getId().isBlank()) {
                report.error(label + " are quest stage fara ID.");
                continue;
            }

            String normalizedStageId = normalizeAuditKey(stage.getId());
            if (!knownPhases.contains(normalizedStageId)) {
                report.error(label + " are quest stage care nu exista in phases: " + stage.getId() + ".");
            }

            String completionMode = normalizeQuestStageCompletionMode(stage.getCompletionMode());
            if (!isSupportedQuestStageCompletionMode(completionMode)) {
                report.error(label + " stage " + stage.getId()
                    + " are completion_mode necunoscut: " + stage.getCompletionMode() + ".");
            }

            validateQuestStageNextStage(report, label, quest, stage, knownPhases, normalizedStageId);

            boolean stageHasObjectiveMetadata = quest.getObjectives().stream()
                .map(this::questEntryStage)
                .anyMatch(objectiveStage -> normalizeAuditKey(objectiveStage).equals(normalizedStageId));
            if (stage.getObjectiveIds().isEmpty()) {
                if (!"phases".equalsIgnoreCase(stage.getMetadata().getOrDefault("source", ""))
                    && !stageHasObjectiveMetadata) {
                    report.warn(label + " stage " + stage.getId()
                        + " nu listeaza objectives si nu are obiective cu phase/stage aferent.");
                }
                continue;
            }

            Set<String> seenStageObjectives = new HashSet<>();
            for (String objectiveId : stage.getObjectiveIds()) {
                String normalizedObjective = normalizeQuestStageReference(objectiveId);
                if (normalizedObjective.isBlank()) {
                    report.warn(label + " stage " + stage.getId() + " are objective ID gol.");
                    continue;
                }
                if (!seenStageObjectives.add(normalizedObjective)) {
                    report.warn(label + " stage " + stage.getId()
                        + " listeaza objective duplicat: " + objectiveId + ".");
                }
                if (!objectiveReferences.contains(normalizedObjective)) {
                    report.error(label + " stage " + stage.getId()
                        + " refera objective necunoscut: " + objectiveId + ".");
                }
            }
        }
    }

    private void validateQuestStageNextStage(AuditReport report,
                                             String label,
                                             FeaturePackLoader.ScenarioDefinition quest,
                                             FeaturePackLoader.QuestStageDefinition stage,
                                             Set<String> knownPhases,
                                             String normalizedStageId) {
        String nextStage = stage.getNextStageId();
        if (nextStage == null || nextStage.isBlank()) {
            return;
        }

        String normalizedNextStage = normalizeAuditKey(nextStage);
        if (normalizedNextStage.isBlank()) {
            report.warn(label + " stage " + stage.getId() + " are next_stage gol.");
            return;
        }
        if (normalizedNextStage.equals(normalizedStageId)) {
            report.error(label + " stage " + stage.getId() + " are next_stage catre sine.");
        }
        if (!knownPhases.contains(normalizedNextStage)) {
            report.error(label + " stage " + stage.getId()
                + " are next_stage necunoscut: " + nextStage + ".");
        } else if (!isQuestRuntimeStage(quest, normalizedNextStage)) {
            report.warn(label + " stage " + stage.getId()
                + " are next_stage catre o faza fara obiective runtime: " + nextStage + ".");
        }
    }

    private boolean isQuestRuntimeStage(FeaturePackLoader.ScenarioDefinition quest, String normalizedStageId) {
        if (quest == null || normalizedStageId == null || normalizedStageId.isBlank()) {
            return false;
        }

        for (FeaturePackLoader.QuestStageDefinition stage : quest.getQuestStages()) {
            if (stage == null || !normalizeAuditKey(stage.getId()).equals(normalizedStageId)) {
                continue;
            }
            if (!stage.getObjectiveIds().isEmpty()) {
                return true;
            }
            return quest.getObjectives().stream()
                .map(this::questEntryStage)
                .anyMatch(objectiveStage -> normalizeAuditKey(objectiveStage).equals(normalizedStageId));
        }
        return false;
    }

    private Set<String> collectQuestObjectiveReferences(List<FeaturePackLoader.QuestEntryDefinition> objectives) {
        Set<String> references = new HashSet<>();
        for (FeaturePackLoader.QuestEntryDefinition objective : objectives) {
            if (objective == null) {
                continue;
            }
            references.add(normalizeQuestStageReference(objective.getEntryId()));
            references.add(normalizeQuestStageReference(objective.getItemId()));
        }
        references.remove("");
        return references;
    }

    private boolean questStageReferencesObjective(FeaturePackLoader.ScenarioDefinition quest,
                                                  FeaturePackLoader.QuestEntryDefinition objective) {
        if (quest == null || objective == null || quest.getQuestStages().isEmpty()) {
            return false;
        }

        for (FeaturePackLoader.QuestStageDefinition stage : quest.getQuestStages()) {
            if (stageReferencesObjective(stage, objective)) {
                return true;
            }
        }
        return false;
    }

    private boolean stageReferencesObjective(FeaturePackLoader.QuestStageDefinition stage,
                                             FeaturePackLoader.QuestEntryDefinition objective) {
        if (stage == null || objective == null || stage.getObjectiveIds().isEmpty()) {
            return false;
        }

        String entryId = normalizeQuestStageReference(objective.getEntryId());
        String itemId = normalizeQuestStageReference(objective.getItemId());
        for (String objectiveId : stage.getObjectiveIds()) {
            String normalizedObjective = normalizeQuestStageReference(objectiveId);
            if (!normalizedObjective.isBlank()
                && (normalizedObjective.equals(entryId) || normalizedObjective.equals(itemId))) {
                return true;
            }
        }
        return false;
    }

    private String normalizeQuestStageCompletionMode(String completionMode) {
        String normalized = normalizeQuestStageReference(completionMode);
        return switch (normalized) {
            case "", "all", "all_objective", "all_objectives", "allobjective", "allobjectives" -> "all_objectives";
            case "any", "any_objective", "any_objectives", "anyobjective", "anyobjectives" -> "any_objective";
            case "manual", "manual_turn_in", "manualturnin", "turn_in", "turnin" -> "manual_turn_in";
            default -> normalized;
        };
    }

    private boolean isSupportedQuestStageCompletionMode(String completionMode) {
        return Set.of("all_objectives", "any_objective", "manual_turn_in").contains(completionMode);
    }

    private String normalizeQuestStageReference(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return value.trim()
            .toLowerCase(Locale.ROOT)
            .replace("minecraft:", "")
            .replaceAll("[^\\p{L}\\p{Nd}]+", "_")
            .replaceAll("^_+|_+$", "")
            .replaceAll("_+", "_");
    }

    private void validateQuestDialogues(AuditReport report,
                                        String label,
                                        FeaturePackLoader.ScenarioDefinition quest) {
        Map<String, List<String>> dialogues = quest.getQuestDialogues();
        for (String requiredDialogue : List.of("offer", "offered", "accepted", "active", "ready", "completed")) {
            List<String> lines = dialogues.get(requiredDialogue);
            if (lines == null || lines.isEmpty()) {
                report.warn(label + " nu are quest.dialogues." + requiredDialogue + ".");
            }
        }
        boolean hasAvailabilityGate = !quest.getQuestPrerequisites().isEmpty() || quest.isQuestRepeatable();
        List<String> unavailableLines = dialogues.get("unavailable");
        if (hasAvailabilityGate && (unavailableLines == null || unavailableLines.isEmpty())) {
            report.warn(label + " are prerequisite/repeatable, dar nu are quest.dialogues.unavailable.");
        }
    }

    private void validateQuestGiverRole(AuditReport report,
                                        String label,
                                        FeaturePackLoader.ScenarioDefinition quest) {
        FeaturePackLoader.ScenarioRoleDefinition role = quest.getRoles().get("QUEST_GIVER");
        if (role == null) {
            report.warn(label + " nu defineste rolul QUEST_GIVER.");
            return;
        }

        String giverProfession = normalizeAuditKey(quest.getQuestGiverProfession());
        if (giverProfession.isBlank() || role.getRequiredProfessions().isEmpty()) {
            return;
        }

        boolean roleMatchesGiver = role.getRequiredProfessions().stream()
            .map(profession -> normalizeAuditKey(profession))
            .anyMatch(giverProfession::equals);
        if (!roleMatchesGiver) {
            report.warn(label + " are QUEST_GIVER.required_professions diferit de quest.giver_profession.");
        }
    }

    private void validateQuestEntries(AuditReport report,
                                      String label,
                                      String entryKind,
                                      List<FeaturePackLoader.QuestEntryDefinition> entries,
                                      boolean objectives) {
        validateQuestEntries(report, label, entryKind, entries, objectives, null);
    }

    private void validateQuestEntries(AuditReport report,
                                      String label,
                                      String entryKind,
                                      List<FeaturePackLoader.QuestEntryDefinition> entries,
                                      boolean objectives,
                                      WorldMappingSemanticIndex worldSemanticIndex) {
        if (entries.isEmpty()) {
            if (objectives) {
                report.error(label + " nu are obiective.");
            } else {
                report.warn(label + " nu are recompense.");
            }
            return;
        }

        Set<String> entryIds = new HashSet<>();
        for (FeaturePackLoader.QuestEntryDefinition entry : entries) {
            String entryLabel = label + " " + entryKind + " " + questEntryId(entry);
            String rawEntryId = entry.getMetadata().getOrDefault("entry_id", "");
            String entryId = normalizeAuditKey(rawEntryId);
            validateQuestEntryId(report, label, entryKind, rawEntryId);
            if (!entryId.isBlank() && !entryIds.add(entryId)) {
                report.error(label + " are " + entryKind + " duplicat: " + rawEntryId + ".");
            }
            if (entry.getAmount() <= 0) {
                report.error(entryLabel + " are amount invalid: " + entry.getAmount() + ".");
            }

            if (objectives) {
                validateQuestObjectiveEntry(report, entryLabel, entry, worldSemanticIndex);
            } else {
                validateQuestRewardEntry(report, entryLabel, entry);
            }
        }
    }

    private void validateQuestEntryId(AuditReport report, String label, String entryKind, String entryId) {
        if (entryId == null || entryId.isBlank()) {
            report.error(label + " are " + entryKind + " fara ID stabil.");
            return;
        }

        if (!entryId.matches("[A-Za-z0-9][A-Za-z0-9_.-]*")) {
            report.error(label + " are " + entryKind + " cu ID fragil: " + entryId
                + ". Foloseste doar litere ASCII, cifre, '_', '-' sau '.'.");
        }
    }

    private void validateQuestObjectiveEntry(AuditReport report,
                                             String entryLabel,
                                             FeaturePackLoader.QuestEntryDefinition entry) {
        validateQuestObjectiveEntry(report, entryLabel, entry, null);
    }

    private void validateQuestObjectiveEntry(AuditReport report,
                                             String entryLabel,
                                             FeaturePackLoader.QuestEntryDefinition entry,
                                             WorldMappingSemanticIndex worldSemanticIndex) {
        String type = normalizeQuestObjectiveType(entry.getType());
        switch (type) {
            case "collect_item", "deliver_to_npc" -> validateMaterialReference(report, entryLabel, entry.getItemId());
            case "kill_mob" -> validateEntityReference(report, entryLabel, entry.getItemId());
            case "talk_to_npc", "visit_region", "visit_place", "inspect_node" -> {
                if (entry.getItemId().isBlank()) {
                    report.warn(entryLabel + " nu are item/reference; resolverul va folosi contextul curent daca poate.");
                } else {
                    validateQuestSemanticReference(report, entryLabel, type, entry.getItemId());
                    validateQuestSemanticReferenceExists(report, entryLabel, type, entry.getItemId(), worldSemanticIndex);
                }
            }
            default -> report.error(entryLabel + " are tip de obiectiv nesuportat de runtime: " + entry.getType() + ".");
        }
    }

    private void validateQuestSemanticReferenceExists(AuditReport report,
                                                      String label,
                                                      String objectiveType,
                                                      String reference,
                                                      WorldMappingSemanticIndex worldSemanticIndex) {
        if (worldSemanticIndex == null) {
            return;
        }

        String anchorType = semanticAnchorTypeForObjective(objectiveType);
        if (anchorType.isBlank() || "npc".equals(anchorType)) {
            return;
        }

        if (!worldSemanticIndex.hasReference(anchorType, reference)) {
            report.warn(label + " refera " + objectiveType + " `" + reference
                + "`, dar tokenul nu apare in world mapping semantic_index pentru ancora " + anchorType
                + ". Verifica /ainpc debugdump world sau mapping-ul demo.");
        }
    }

    private String semanticAnchorTypeForObjective(String objectiveType) {
        return switch (normalizeQuestObjectiveType(objectiveType)) {
            case "visit_region" -> "region";
            case "visit_place" -> "place";
            case "inspect_node" -> "node";
            case "talk_to_npc" -> "npc";
            default -> "";
        };
    }

    private void validateQuestRewardEntry(AuditReport report,
                                          String entryLabel,
                                          FeaturePackLoader.QuestEntryDefinition entry) {
        String type = normalizeQuestRewardType(entry.getType());
        switch (type) {
            case "item" -> validateMaterialReference(report, entryLabel, entry.getItemId());
            case "set_story_state", "record_story_event" -> validateQuestStoryAction(report, entryLabel, type, entry);
            default -> report.error(entryLabel + " are tip de recompensa nesuportat de runtime: " + entry.getType() + ".");
        }
    }

    private void validateMaterialReference(AuditReport report, String label, String materialId) {
        if (materialId == null || materialId.isBlank()) {
            report.error(label + " nu are item/material.");
            return;
        }
        if (Material.matchMaterial(materialId) == null) {
            report.error(label + " refera material Minecraft invalid: " + materialId + ".");
        }
    }

    private void validateEntityReference(AuditReport report, String label, String entityId) {
        if (entityId == null || entityId.isBlank()) {
            report.error(label + " nu are mob/entity.");
            return;
        }
        try {
            EntityType.valueOf(entityId.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            report.error(label + " refera entity Minecraft invalid: " + entityId + ".");
        }
    }

    private void validateQuestSemanticReference(AuditReport report,
                                                String label,
                                                String objectiveType,
                                                String reference) {
        String prefix = questReferencePrefix(reference);
        if (prefix.isBlank()) {
            return;
        }

        Set<String> allowedPrefixes = switch (objectiveType) {
            case "talk_to_npc" -> Set.of("npc", "name", "profession");
            case "visit_region" -> Set.of("region", "tag", "type");
            case "visit_place" -> Set.of("place", "region", "tag", "type");
            case "inspect_node" -> Set.of("node", "place", "tag", "type");
            default -> Set.of();
        };

        if (!allowedPrefixes.isEmpty() && !allowedPrefixes.contains(prefix)) {
            report.warn(label + " foloseste prefix de referinta neobisnuit pentru " + objectiveType
                + ": " + reference + ".");
        }
    }

    private void validateQuestStoryAction(AuditReport report,
                                          String label,
                                          String type,
                                          FeaturePackLoader.QuestEntryDefinition entry) {
        Map<String, String> metadata = entry.getMetadata();
        String scope = normalizeStoryActionScope(metadata.getOrDefault("scope", ""));
        if (scope.isBlank()) {
            report.error(label + " nu are metadata.scope pentru story action.");
        } else if (!Set.of("region", "place").contains(scope)) {
            report.error(label + " are metadata.scope invalid pentru story action: "
                + metadata.getOrDefault("scope", "") + ". Valori acceptate: region, place.");
        }
        if (!hasAnyMetadata(metadata,
            "target", "scope_id", "target_id", "id",
            "place_id", "region_id", "target_place", "target_region", "place", "region")) {
            report.error(label + " nu are metadata.target pentru story action.");
        }
        if ("set_story_state".equals(type)
            && !hasAnyMetadata(metadata, "state_key", "state", "flag", "value", "item")) {
            report.error(label + " nu are metadata.state pentru set_story_state.");
        }
        if ("set_story_state".equals(type) && entry.getVariables().isEmpty()) {
            report.warn(label + " nu are variables pentru set_story_state; va scrie doar state_key.");
        }
        if ("record_story_event".equals(type)) {
            if (!hasAnyMetadata(metadata, "event_type", "type_id")) {
                report.error(label + " nu are metadata.event_type pentru record_story_event.");
            }
            if (!hasAnyMetadata(metadata, "event_key", "key")) {
                report.error(label + " nu are metadata.event_key pentru record_story_event.");
            }
            if (entry.getPayload().isEmpty()) {
                report.error(label + " nu are payload minim pentru record_story_event.");
            } else if (!hasAnyMapEntry(entry.getPayload(),
                "quest", "outcome", "result", "reason", "state", "mechanic", "tag", "quest_template", "quest_code")) {
                report.warn(label + " are payload record_story_event, dar fara cheie semantica uzuala "
                    + "(quest/outcome/result/reason/state/mechanic/tag).");
            }
        }
    }

    private String questEntryId(FeaturePackLoader.QuestEntryDefinition entry) {
        if (entry == null) {
            return "<null>";
        }
        String entryId = entry.getMetadata().getOrDefault("entry_id", "");
        if (!entryId.isBlank()) {
            return entryId;
        }
        return entry.getType() + ":" + entry.getItemId();
    }

    private void auditLegacyQuestProgressKeys(AuditReport report) throws SQLException {
        String sql = """
            SELECT player_uuid, template_id, objective_progress
            FROM player_quests
            WHERE objective_progress IS NOT NULL
              AND TRIM(objective_progress) <> ''
              AND TRIM(objective_progress) <> '{}'
            LIMIT 500
        """;

        int legacyWarnings = 0;
        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String rawProgress = rs.getString("objective_progress");
                if (rawProgress == null || rawProgress.isBlank()) {
                    continue;
                }

                JsonElement parsed;
                try {
                    parsed = JsonParser.parseString(rawProgress);
                } catch (JsonSyntaxException exception) {
                    report.warn("player_quests.objective_progress JSON invalid: player_uuid="
                        + rs.getString("player_uuid") + ", template_id=" + rs.getString("template_id")
                        + ", eroare=" + exception.getMessage());
                    continue;
                }

                if (!parsed.isJsonObject()) {
                    report.warn("player_quests.objective_progress nu este obiect JSON: player_uuid="
                        + rs.getString("player_uuid") + ", template_id=" + rs.getString("template_id") + ".");
                    continue;
                }

                for (String key : parsed.getAsJsonObject().keySet()) {
                    if (!isLegacyObjectiveProgressKey(key)) {
                        continue;
                    }
                    report.warn("player_quests contine objective_progress legacy: player_uuid="
                        + rs.getString("player_uuid") + ", template_id=" + rs.getString("template_id")
                        + ", objective_key=" + key + ".");
                    legacyWarnings++;
                    break;
                }

                if (legacyWarnings >= AUDIT_PREVIEW_LIMIT) {
                    report.warn("Audit quest a oprit preview-ul pentru objective_progress legacy la "
                        + AUDIT_PREVIEW_LIMIT + " randuri.");
                    return;
                }
            }
        }
    }

    private boolean isLegacyObjectiveProgressKey(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }

        String[] parts = key.split(":");
        if (parts.length < 3) {
            return false;
        }

        String type = normalizeQuestObjectiveType(parts[0]);
        if (!isSupportedQuestObjectiveType(type)) {
            return false;
        }

        try {
            return Integer.parseInt(parts[parts.length - 1]) >= 0;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private boolean isSupportedQuestObjectiveType(String type) {
        return switch (normalizeQuestObjectiveType(type)) {
            case "collect_item", "deliver_to_npc", "talk_to_npc", "visit_region", "visit_place",
                 "inspect_node", "kill_mob" -> true;
            default -> false;
        };
    }

    private String normalizeQuestObjectiveType(String type) {
        String normalized = normalizeAuditKey(type);
        return switch (normalized) {
            case "", "item", "collect", "collectitem", "collect_item", "fetch", "gather" -> "collect_item";
            case "deliver", "deliveritem", "deliver_item", "deliver_to_npc", "turnin", "turn_in" -> "deliver_to_npc";
            case "talk", "speak", "conversation", "talk_to_npc", "speak_to_npc" -> "talk_to_npc";
            case "visit", "travel", "go_to", "visit_region", "enter_region" -> "visit_region";
            case "visitplace", "visit_place", "enterplace", "enter_place", "go_to_place", "place" -> "visit_place";
            case "inspect", "inspectnode", "inspect_node", "interact_node", "node" -> "inspect_node";
            case "kill", "slay", "defeat", "kill_mob" -> "kill_mob";
            default -> normalized;
        };
    }

    private String normalizeQuestRewardType(String type) {
        String normalized = normalizeAuditKey(type);
        return switch (normalized) {
            case "", "item", "reward_item" -> "item";
            case "set_story_state", "record_story_event" -> normalized;
            default -> normalized;
        };
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

        Map<String, FeaturePackLoader.ScenarioDefinition> scenariosBySelector = buildProgressionScenarioLookup();
        if (scenariosBySelector.isEmpty() && !rows.isEmpty()) {
            report.warn("Nu pot valida objective_key pentru quest anchors; nu exista definitii de progresie incarcate.");
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
            if (!scenariosBySelector.isEmpty()) {
                validateQuestAnchorObjectiveDefinition(report, label, row, scenariosBySelector);
            }
            validateQuestAnchorTarget(report, label, row, worldAdmin, regionsById, placesById, nodesById);
        }

        if (!countsByAnchorType.isEmpty()) {
            report.info("Quest anchors pe tip: " + formatCountMap(countsByAnchorType));
        }
    }

    private Map<String, FeaturePackLoader.ScenarioDefinition> buildProgressionScenarioLookup() {
        FeaturePackLoader loader = plugin.getFeaturePackLoader();
        if (loader == null) {
            return Map.of();
        }

        Map<String, FeaturePackLoader.ScenarioDefinition> lookup = new HashMap<>();
        for (FeaturePackLoader.ScenarioDefinition scenario : loader.getAllScenarios()) {
            if (!ProgressionDefinition.isProgressionCandidate(scenario)) {
                continue;
            }
            ProgressionDefinition definition = ProgressionDefinition.fromScenarioDefinition(scenario);
            addScenarioLookupKey(lookup, definition.templateId(), scenario);
            addScenarioLookupKey(lookup, definition.progressionId(), scenario);
            addScenarioLookupKey(lookup, definition.definitionId(), scenario);
            addScenarioLookupKey(lookup, definition.code(), scenario);
            addScenarioLookupKey(lookup, definition.packId() + ":" + definition.definitionId(), scenario);
            addScenarioLookupKey(lookup,
                definition.packId() + ":" + definition.mechanicId() + ":" + definition.definitionId(),
                scenario);
        }
        return lookup;
    }

    private void addScenarioLookupKey(Map<String, FeaturePackLoader.ScenarioDefinition> lookup,
                                      String key,
                                      FeaturePackLoader.ScenarioDefinition scenario) {
        String normalized = normalizeQuestObjectiveLookupKey(key);
        if (!normalized.isBlank()) {
            lookup.putIfAbsent(normalized, scenario);
        }
    }

    private void validateQuestAnchorObjectiveDefinition(AuditReport report,
                                                       String label,
                                                       QuestAnchorBindingRow row,
                                                       Map<String, FeaturePackLoader.ScenarioDefinition> scenariosBySelector) {
        if (row.objectiveKey() == null || row.objectiveKey().isBlank()) {
            return;
        }

        FeaturePackLoader.ScenarioDefinition scenario = findScenarioForQuestAnchorRow(row, scenariosBySelector);
        if (scenario == null) {
            report.warn(label + " nu poate valida objective_key; definitia progresiei nu este incarcata.");
            return;
        }

        Map<String, FeaturePackLoader.QuestEntryDefinition> objectives = collectObjectiveKeyLookup(scenario);
        FeaturePackLoader.QuestEntryDefinition objective =
            objectives.get(normalizeQuestObjectiveLookupKey(row.objectiveKey()));
        if (objective == null) {
            List<String> candidates = objectives.values().stream()
                .distinct()
                .map(this::displayQuestObjectiveKey)
                .filter(value -> !value.isBlank())
                .distinct()
                .limit(8)
                .toList();
            report.error(label + " are objective_key inexistent in definitie: " + row.objectiveKey()
                + formatObjectiveCandidates(candidates));
            return;
        }

        String expectedType = normalizeQuestObjectiveType(objective.getType());
        String actualType = normalizeQuestObjectiveType(row.objectiveType());
        if (!expectedType.isBlank() && !actualType.isBlank() && !expectedType.equals(actualType)) {
            report.error(label + " are objective_type diferit de definitie: binding="
                + row.objectiveType() + ", definitie=" + expectedType + ".");
        }
    }

    private FeaturePackLoader.ScenarioDefinition findScenarioForQuestAnchorRow(
        QuestAnchorBindingRow row,
        Map<String, FeaturePackLoader.ScenarioDefinition> scenariosBySelector) {
        if (scenariosBySelector == null || scenariosBySelector.isEmpty()) {
            return null;
        }

        FeaturePackLoader.ScenarioDefinition scenario =
            scenariosBySelector.get(normalizeQuestObjectiveLookupKey(row.templateId()));
        if (scenario != null) {
            return scenario;
        }
        scenario = scenariosBySelector.get(normalizeQuestObjectiveLookupKey(row.questCode()));
        if (scenario != null) {
            return scenario;
        }
        return scenariosBySelector.get(normalizeQuestObjectiveLookupKey(lastSelectorSegment(row.templateId())));
    }

    private String formatObjectiveCandidates(List<String> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return ".";
        }
        return ". Obiective valide: " + String.join(", ", candidates) + ".";
    }

    private String lastSelectorSegment(String selector) {
        String safeSelector = selector == null ? "" : selector.trim();
        int separator = safeSelector.lastIndexOf(':');
        if (separator < 0 || separator >= safeSelector.length() - 1) {
            return safeSelector;
        }
        return safeSelector.substring(separator + 1);
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
            int sourceKeyRows = queryCount("SELECT COUNT(*) FROM npc_source_keys");
            int worldBindingRows = queryCount("SELECT COUNT(*) FROM npc_world_bindings");
            int householdRows = queryCount("SELECT COUNT(*) FROM households");
            int householdResidentRows = queryCount("SELECT COUNT(*) FROM household_residents");
            int spawnBatchRows = queryCount("SELECT COUNT(*) FROM spawn_batches");
            int activeSpawnBatchRows = queryCount("""
                SELECT COUNT(*)
                FROM spawn_batches
                WHERE status IN ('RUNNING', 'FAILED', 'ROLLED_BACK')
                """);
            report.info("DB: " + npcRows + " randuri in npcs, " + profileRows + " randuri in npc_profiles.");
            report.info("NPC source keys: " + sourceKeyRows + " randuri in npc_source_keys.");
            report.info("NPC world bindings: " + worldBindingRows + " randuri in npc_world_bindings.");
            report.info("Households: " + householdRows + " randuri, " + householdResidentRows + " rezidenti persistenti.");
            report.info("Spawn batches: " + spawnBatchRows + " randuri, " + activeSpawnBatchRows
                + " nefinalizate/esuate.");

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
            auditNpcDuplicateDatabaseRows(report);

            auditQueryRows(report, """
                SELECT s.source_key, s.npc_id
                FROM npc_source_keys s
                LEFT JOIN npcs n ON n.id = s.npc_id
                WHERE n.id IS NULL
                """, "source_key persistent orfan fara NPC");

            auditQueryRows(report, """
                SELECT b.npc_id, b.npc_name
                FROM npc_world_bindings b
                LEFT JOIN npcs n ON n.id = b.npc_id
                WHERE n.id IS NULL
                """, "NPC world binding orfan fara NPC");

            auditQueryRows(report, """
                SELECT npc_id, npc_name
                FROM npc_world_bindings
                WHERE home_place_id = '' AND work_place_id = '' AND social_place_id = ''
                """, "NPC world binding fara niciun place");

            auditQueryRows(report, """
                SELECT household_id, family_id, resident_count
                FROM households
                WHERE TRIM(COALESCE(home_place_id, '')) = ''
                """, "Household fara home_place_id");

            auditQueryRows(report, """
                SELECT r.household_id, r.resident_key, r.npc_id
                FROM household_residents r
                LEFT JOIN households h ON h.household_id = r.household_id
                WHERE h.household_id IS NULL
                """, "Household resident orfan fara household");

            auditQueryRows(report, """
                SELECT r.household_id, r.resident_key, r.npc_id
                FROM household_residents r
                LEFT JOIN npcs n ON n.id = r.npc_id
                WHERE n.id IS NULL
                """, "Household resident refera NPC inexistent");

            auditQueryRows(report, """
                SELECT r.household_id, r.resident_key, r.npc_id, r.source_key
                FROM household_residents r
                LEFT JOIN npc_source_keys s ON s.source_key = r.source_key
                WHERE r.source_key <> ''
                  AND (s.source_key IS NULL OR s.npc_id <> r.npc_id)
                """, "Household resident are source_key nealiniat cu npc_source_keys");

            auditQueryRows(report, """
                SELECT h.household_id, h.resident_count, COUNT(r.resident_key) AS actual_residents
                FROM households h
                LEFT JOIN household_residents r
                  ON r.household_id = h.household_id AND r.status = 'active'
                GROUP BY h.household_id, h.resident_count
                HAVING h.resident_count <> COUNT(r.resident_key)
                """, "Household are resident_count diferit de rezidentii activi");

            auditQueryRows(report, """
                SELECT r.npc_id, COUNT(*) AS household_count
                FROM household_residents r
                GROUP BY r.npc_id
                HAVING COUNT(*) > 1
                """, "NPC asignat in mai multe household-uri");
            auditHouseholdMappingReferences(report, householdRows, householdResidentRows);

            auditQueryRows(report, """
                SELECT batch_key, scope_type, scope_id, status, allocation_count, npc_plan_count
                FROM spawn_batches
                WHERE status IN ('RUNNING', 'FAILED', 'ROLLED_BACK')
                ORDER BY updated_at DESC
                LIMIT 10
                """, "Spawn batch nefinalizat sau esuat");

            auditQueryRows(report, """
                SELECT b.batch_key, b.scope_type, b.scope_id, COUNT(s.step_index) AS creator_steps
                FROM spawn_batches b
                JOIN spawn_batch_steps s ON s.batch_key = b.batch_key
                WHERE b.status = 'RUNNING'
                  AND TRIM(COALESCE(s.created_npc_ids, '')) <> ''
                GROUP BY b.batch_key, b.scope_type, b.scope_id
                ORDER BY b.updated_at DESC
                LIMIT 10
                """, "Spawn batch RUNNING are NPC-uri create si necesita repair inainte de retry");

            auditQueryRows(report, """
                SELECT s.batch_key, s.step_index, s.step_key
                FROM spawn_batch_steps s
                LEFT JOIN spawn_batches b ON b.batch_key = s.batch_key
                WHERE b.batch_key IS NULL
                """, "Spawn batch step orfan fara batch");

            auditQueryRows(report, """
                SELECT s.batch_key, s.step_index, s.step_key
                FROM spawn_batch_steps s
                WHERE TRIM(COALESCE(s.step_key, '')) <> ''
                  AND TRIM(COALESCE(s.household_id, '')) = ''
                """, "Spawn batch step fara household_id explicit");

            auditQueryRows(report, """
                SELECT s.batch_key, s.step_index, s.step_key, s.status
                FROM spawn_batch_steps s
                JOIN spawn_batches b ON b.batch_key = s.batch_key
                WHERE b.status = 'ROLLED_BACK'
                  AND TRIM(COALESCE(s.created_npc_ids, '')) <> ''
                  AND s.status <> 'ROLLED_BACK'
                """, "Spawn batch ROLLED_BACK are pasi creatori nemarcati ROLLED_BACK");

            auditQueryRows(report, """
                SELECT s.batch_key, s.step_index, s.step_key, s.status
                FROM spawn_batch_steps s
                JOIN spawn_batches b ON b.batch_key = s.batch_key
                WHERE b.status = 'SUCCEEDED'
                  AND s.status IN ('FAILED', 'ROLLED_BACK')
                """, "Spawn batch SUCCEEDED are pasi esuati sau rollback");

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
            auditNpcWorldBindings(report, worldBindingRows);
        } catch (SQLException exception) {
            report.error("Audit DB esuat: " + exception.getMessage());
        }
    }

    private void auditNpcDuplicateDatabaseRows(AuditReport report) throws SQLException {
        auditQueryRows(report, """
            SELECT LOWER(TRIM(name)) AS name_key,
                   world,
                   CAST(ROUND(x * 2.0) AS INTEGER) AS x_half_block,
                   CAST(ROUND(y * 2.0) AS INTEGER) AS y_half_block,
                   CAST(ROUND(z * 2.0) AS INTEGER) AS z_half_block,
                   COUNT(*) AS duplicate_count,
                   GROUP_CONCAT(id || ':' || name, ', ') AS npc_rows
            FROM npcs
            WHERE TRIM(COALESCE(name, '')) <> ''
              AND TRIM(COALESCE(world, '')) <> ''
            GROUP BY name_key, world, x_half_block, y_half_block, z_half_block
            HAVING COUNT(*) > 1
            """, "NPC DB duplicat dupa nume si locatie apropiata");

        Map<String, List<String>> rowsBySourceKey = new HashMap<>();
        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement("""
                 SELECT n.id, n.name, p.profile_data
                 FROM npcs n
                 JOIN npc_profiles p ON p.npc_id = n.id
                 WHERE TRIM(COALESCE(p.profile_data, '')) <> ''
                 """);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String sourceKey = extractSourceKeyFromProfileData(rs.getString("profile_data"));
                if (sourceKey.isBlank()) {
                    continue;
                }
                rowsBySourceKey
                    .computeIfAbsent(normalizeAuditKey(sourceKey), ignored -> new ArrayList<>())
                    .add(rs.getInt("id") + ":" + formatOptional(rs.getString("name")));
            }
        }

        for (Map.Entry<String, List<String>> entry : rowsBySourceKey.entrySet()) {
            if (entry.getValue().size() > 1) {
                report.error("NPC DB duplicat dupa profile_data.source_key=" + entry.getKey()
                    + " (" + entry.getValue().size() + "): " + entry.getValue() + ".");
            }
        }
    }

    private String extractSourceKeyFromProfileData(String profileData) {
        if (profileData == null || profileData.isBlank()) {
            return "";
        }

        try {
            JsonElement parsed = JsonParser.parseString(profileData);
            if (parsed == null || !parsed.isJsonObject()) {
                return "";
            }
            JsonElement sourceKey = parsed.getAsJsonObject().get("source_key");
            return sourceKey != null && sourceKey.isJsonPrimitive()
                ? sourceKey.getAsString().trim()
                : "";
        } catch (JsonSyntaxException exception) {
            return "";
        }
    }

    private void auditNpcWorldBindings(AuditReport report, int totalRows) throws SQLException {
        if (plugin.getNpcWorldBindingService() == null) {
            report.warn("NpcWorldBindingService indisponibil; nu pot valida npc_world_bindings.");
            return;
        }

        List<NpcWorldBinding> rows = plugin.getNpcWorldBindingService().listBindings(500);
        if (totalRows > rows.size()) {
            report.warn("Audit npc_world_bindings a verificat primele " + rows.size()
                + " randuri din " + totalRows + ".");
        }

        WorldAdminApi worldAdmin = plugin.getPlatform() != null ? plugin.getPlatform().getWorldAdmin() : null;
        Map<String, WorldPlaceInfo> placesById = new HashMap<>();
        Map<String, WorldNodeInfo> nodesById = new HashMap<>();
        if (worldAdmin != null && worldAdmin.isEnabled()) {
            for (WorldPlaceInfo place : worldAdmin.getPlaces()) {
                placesById.put(place.id(), place);
            }
            for (WorldNodeInfo node : worldAdmin.getNodes()) {
                nodesById.put(node.id(), node);
            }
        }

        Map<Integer, NpcWorldBinding> bindingsByNpcId = new HashMap<>();
        Map<Integer, AINPC> loadedNpcsById = new HashMap<>();
        for (AINPC npc : plugin.getNpcManager().getAllNPCs()) {
            if (npc != null && npc.getDatabaseId() > 0) {
                loadedNpcsById.put(npc.getDatabaseId(), npc);
            }
        }

        for (NpcWorldBinding binding : rows) {
            bindingsByNpcId.put(binding.npcId(), binding);
            String label = "npc_world_bindings npc_id=" + binding.npcId()
                + " (" + formatOptional(binding.npcName()) + ")";
            AINPC loadedNpc = loadedNpcsById.get(binding.npcId());
            if (loadedNpc == null) {
                report.warn(label + " nu are NPC incarcat acum.");
            }
            validateNpcWorldPlaceBinding(report, label, "home", binding.homePlaceId(), placesById);
            validateNpcWorldPlaceBinding(report, label, "work", binding.workPlaceId(), placesById);
            validateNpcWorldPlaceBinding(report, label, "social", binding.socialPlaceId(), placesById);
            validateNpcWorldNodeBinding(report, label, "home", binding.homeNodeId(), binding.homePlaceId(), nodesById);
            validateNpcWorldNodeBinding(report, label, "work", binding.workNodeId(), binding.workPlaceId(), nodesById);
            validateNpcWorldNodeBinding(report, label, "social", binding.socialNodeId(), binding.socialPlaceId(), nodesById);
            auditNpcProfileBindingDivergence(report, label, loadedNpc, binding, worldAdmin);
        }

        if (worldAdmin != null && worldAdmin.isEnabled()) {
            for (AINPC npc : loadedNpcsById.values()) {
                if (bindingsByNpcId.containsKey(npc.getDatabaseId())) {
                    continue;
                }
                NpcWorldBinding inferred = inferNpcWorldBindingFromProfile(npc, worldAdmin, "audit_profile");
                if (inferred != null && inferred.hasAnyPlaceBinding()) {
                    report.warn("NPC " + npc.getName() + "#" + npc.getDatabaseId()
                        + " are ancore in profil dar nu are rand in npc_world_bindings. "
                        + "Ruleaza /ainpc repair npc-bindings dryrun.");
                }
            }
        }
    }

    private void auditNpcProfileBindingDivergence(AuditReport report,
                                                  String label,
                                                  AINPC npc,
                                                  NpcWorldBinding binding,
                                                  WorldAdminApi worldAdmin) {
        if (npc == null || worldAdmin == null || !worldAdmin.isEnabled()) {
            return;
        }

        NpcWorldBinding inferred = inferNpcWorldBindingFromProfile(npc, worldAdmin, "audit_profile");
        if (inferred == null || !inferred.hasAnyPlaceBinding()) {
            if (binding.hasAnyPlaceBinding()) {
                report.warn(label + " are rand persistent, dar profilul NPC nu mai indica ancore mapabile.");
            }
            return;
        }

        warnNpcBindingFieldDivergence(report, label, "home_place_id",
            binding.homePlaceId(), inferred.homePlaceId());
        warnNpcBindingFieldDivergence(report, label, "work_place_id",
            binding.workPlaceId(), inferred.workPlaceId());
        warnNpcBindingFieldDivergence(report, label, "social_place_id",
            binding.socialPlaceId(), inferred.socialPlaceId());
        warnNpcBindingFieldDivergence(report, label, "home_node_id",
            binding.homeNodeId(), inferred.homeNodeId());
        warnNpcBindingFieldDivergence(report, label, "work_node_id",
            binding.workNodeId(), inferred.workNodeId());
        warnNpcBindingFieldDivergence(report, label, "social_node_id",
            binding.socialNodeId(), inferred.socialNodeId());
    }

    private void warnNpcBindingFieldDivergence(AuditReport report,
                                               String label,
                                               String field,
                                               String storedValue,
                                               String profileValue) {
        if (!sameOptionalId(storedValue, profileValue)) {
            report.warn(label + " difera de profil pentru " + field
                + ": binding=" + formatOptional(storedValue)
                + ", profil=" + formatOptional(profileValue) + ".");
        }
    }

    private void auditHouseholdMappingReferences(AuditReport report,
                                                 int totalHouseholdRows,
                                                 int totalResidentRows) throws SQLException {
        WorldAdminApi worldAdmin = plugin.getPlatform() != null ? plugin.getPlatform().getWorldAdmin() : null;
        if (worldAdmin == null || !worldAdmin.isEnabled()) {
            if (totalHouseholdRows > 0 || totalResidentRows > 0) {
                report.warn("World admin este dezactivat; nu pot valida referintele mapping din households.");
            }
            return;
        }

        Map<String, WorldPlaceInfo> placesById = new HashMap<>();
        Map<String, WorldNodeInfo> nodesById = new HashMap<>();
        for (WorldPlaceInfo place : worldAdmin.getPlaces()) {
            placesById.put(normalizeAuditKey(place.id()), place);
        }
        for (WorldNodeInfo node : worldAdmin.getNodes()) {
            nodesById.put(normalizeAuditKey(node.id()), node);
        }

        int checkedHouseholds = 0;
        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement("""
                 SELECT household_id, home_place_id
                 FROM households
                 WHERE TRIM(COALESCE(home_place_id, '')) <> ''
                 ORDER BY household_id ASC
                 LIMIT ?
                 """)) {
            stmt.setInt(1, NPC_WORLD_BINDING_LOOKUP_LIMIT);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    checkedHouseholds++;
                    String label = "Household " + rs.getString("household_id");
                    validateHouseholdPlaceReference(report, label, "home", rs.getString("home_place_id"), placesById);
                }
            }
        }
        if (totalHouseholdRows > checkedHouseholds) {
            report.warn("Audit households a verificat primele " + checkedHouseholds
                + " household-uri din " + totalHouseholdRows + ".");
        }

        int checkedResidents = 0;
        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement("""
                 SELECT household_id, resident_key, npc_id,
                        home_place_id, home_node_id, work_place_id, work_node_id
                 FROM household_residents
                 ORDER BY household_id ASC, resident_key ASC
                 LIMIT ?
                 """)) {
            stmt.setInt(1, NPC_WORLD_BINDING_LOOKUP_LIMIT);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    checkedResidents++;
                    String label = "Household resident " + rs.getString("household_id")
                        + "/" + rs.getString("resident_key") + " npc_id=" + rs.getInt("npc_id");
                    String homePlaceId = rs.getString("home_place_id");
                    String workPlaceId = rs.getString("work_place_id");
                    validateHouseholdPlaceReference(report, label, "home", homePlaceId, placesById);
                    validateHouseholdPlaceReference(report, label, "work", workPlaceId, placesById);
                    validateHouseholdNodeReference(report, label, "home", rs.getString("home_node_id"), homePlaceId, nodesById);
                    validateHouseholdNodeReference(report, label, "work", rs.getString("work_node_id"), workPlaceId, nodesById);
                }
            }
        }
        if (totalResidentRows > checkedResidents) {
            report.warn("Audit household_residents a verificat primii " + checkedResidents
                + " rezidenti din " + totalResidentRows + ".");
        }
    }

    private void validateHouseholdPlaceReference(AuditReport report,
                                                 String label,
                                                 String role,
                                                 String placeId,
                                                 Map<String, WorldPlaceInfo> placesById) {
        if (placeId == null || placeId.isBlank()) {
            return;
        }

        WorldPlaceInfo place = placesById.get(normalizeAuditKey(placeId));
        if (place == null) {
            report.error(label + " refera " + role + "_place_id inexistent in mapping: " + placeId + ".");
        }
    }

    private void validateHouseholdNodeReference(AuditReport report,
                                                String label,
                                                String role,
                                                String nodeId,
                                                String expectedPlaceId,
                                                Map<String, WorldNodeInfo> nodesById) {
        if (nodeId == null || nodeId.isBlank()) {
            return;
        }

        WorldNodeInfo node = nodesById.get(normalizeAuditKey(nodeId));
        if (node == null) {
            report.error(label + " refera " + role + "_node_id inexistent in mapping: " + nodeId + ".");
            return;
        }
        if (expectedPlaceId != null && !expectedPlaceId.isBlank()
            && !node.placeId().isBlank()
            && !node.placeId().equalsIgnoreCase(expectedPlaceId)) {
            report.error(label + " are " + role + "_node_id=" + nodeId
                + " in alt place decat " + expectedPlaceId + ".");
        }
    }

    private void validateNpcWorldPlaceBinding(AuditReport report,
                                              String label,
                                              String role,
                                              String placeId,
                                              Map<String, WorldPlaceInfo> placesById) {
        if (placeId == null || placeId.isBlank()) {
            return;
        }

        WorldPlaceInfo place = placesById.get(placeId);
        if (place == null) {
            report.error(label + " refera " + role + "_place_id inexistent: " + placeId + ".");
            return;
        }

        if ("home".equals(role) && !isHousePlace(place)) {
            report.warn(label + " are home_place_id care nu este casa/home: " + placeId + ".");
        } else if ("work".equals(role) && !isWorkplace(place)) {
            report.error(label + " are work_place_id care nu este workplace: " + placeId + ".");
        } else if ("social".equals(role) && !isSocialPlace(place)) {
            report.warn(label + " are social_place_id care nu este loc social clar: " + placeId + ".");
        }
    }

    private void validateNpcWorldNodeBinding(AuditReport report,
                                             String label,
                                             String role,
                                             String nodeId,
                                             String expectedPlaceId,
                                             Map<String, WorldNodeInfo> nodesById) {
        if (nodeId == null || nodeId.isBlank()) {
            return;
        }

        WorldNodeInfo node = nodesById.get(nodeId);
        if (node == null) {
            report.error(label + " refera " + role + "_node_id inexistent: " + nodeId + ".");
            return;
        }
        if (expectedPlaceId != null && !expectedPlaceId.isBlank()
            && !node.placeId().isBlank()
            && !node.placeId().equalsIgnoreCase(expectedPlaceId)) {
            report.error(label + " are " + role + "_node_id=" + nodeId
                + " in alt place decat " + expectedPlaceId + ".");
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
                + " rezultate. Ruleaza audituri mai specifice: npc/world/db/spawn/quest/wand.");
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

    private void auditQueryErrorRows(AuditReport report, String sql, String label) throws SQLException {
        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                report.error(label + ": " + describeCurrentRow(rs));
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

    private boolean handleDeleteId(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ainpc.admin")) {
            plugin.getMessageUtils().sendMessage(sender, "no_permission");
            return true;
        }

        if (args.length < 2) {
            plugin.getMessageUtils().send(sender, "&cUtilizare: /ainpc delete-id <id> confirm");
            return true;
        }

        Integer npcId = parseIntegerStrict(args[1]);
        if (npcId == null || npcId <= 0) {
            plugin.getMessageUtils().send(sender, "&cID NPC invalid: &f" + args[1]);
            return true;
        }

        AINPC npc = plugin.getNpcManager().getNPCById(npcId);
        if (npc == null) {
            plugin.getMessageUtils().send(sender, "&cNu exista NPC incarcat cu ID-ul: &f" + npcId);
            return true;
        }

        if (args.length < 3 || !"confirm".equalsIgnoreCase(args[2])) {
            plugin.getMessageUtils().send(sender, "&eNPC selectat: &f" + npc.getName()
                + " &7(id=&f" + npc.getDatabaseId() + "&7, source=&f" + formatOptional(npc.getSourceKey()) + "&7)");
            plugin.getMessageUtils().send(sender, "&eLocatie: &f" + formatLocation(npc.getLocation()));
            plugin.getMessageUtils().send(sender, "&cPentru stergere definitiva ruleaza: &f/ainpc delete-id " + npcId + " confirm");
            return true;
        }

        if (plugin.getNpcManager().deleteNPC(npc)) {
            plugin.getMessageUtils().send(sender, "&aNPC sters dupa ID: &f" + npc.getName() + "#" + npcId);
        } else {
            plugin.getMessageUtils().send(sender, "&cEroare la stergerea NPC-ului cu ID: &f" + npcId);
        }

        return true;
    }

    private boolean handleDuplicates(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ainpc.admin")) {
            plugin.getMessageUtils().sendMessage(sender, "no_permission");
            return true;
        }

        List<AINPC> npcs = new ArrayList<>(plugin.getNpcManager().getAllNPCs());
        plugin.getMessageUtils().send(sender, "&6=== Duplicate NPC - raport ===");
        plugin.getMessageUtils().send(sender, "&eNPC-uri incarcate: &f" + npcs.size());

        List<String> findings = new ArrayList<>();
        collectSourceKeyDuplicateFindings(npcs, findings);
        collectNearbyNameDuplicateFindings(npcs, findings);
        for (var issue : plugin.getNpcManager().auditManagedVillagerEntities()) {
            findings.add((issue.error() ? "&c" : "&e") + issue.message());
        }
        for (var issue : plugin.getNpcManager().auditPersistentSourceKeyIndex()) {
            findings.add((issue.error() ? "&c" : "&e") + issue.message());
        }

        if (findings.isEmpty()) {
            plugin.getMessageUtils().send(sender, "&aNu am gasit duplicate evidente in NPCManager, entitati live sau indexul source_key.");
            return true;
        }

        int limit = Math.min(12, findings.size());
        for (int index = 0; index < limit; index++) {
            plugin.getMessageUtils().send(sender, findings.get(index));
        }
        if (findings.size() > limit) {
            plugin.getMessageUtils().send(sender, "&7... inca &f" + (findings.size() - limit)
                + " &7probleme. Ruleaza &f/ainpc audit npc &7si &f/ainpc debugdump npc&7.");
        }
        plugin.getMessageUtils().send(sender, "&7Cleanup sigur: &f/ainpc delete-id <id> confirm");
        return true;
    }

    private boolean handleRepair(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ainpc.admin")) {
            plugin.getMessageUtils().sendMessage(sender, "no_permission");
            return true;
        }

        if (args.length < 2 || (!"duplicates".equalsIgnoreCase(args[1])
            && !"households".equalsIgnoreCase(args[1])
            && !isNpcBindingRepairTarget(args[1])
            && !isMappingMetadataRepairTarget(args[1])
            && !isRepairBatchTarget(args[1]))) {
            plugin.getMessageUtils().send(sender, "&cUtilizare: /ainpc repair duplicates [dryrun|apply]");
            plugin.getMessageUtils().send(sender, "&cUtilizare: /ainpc repair households [dryrun|apply]");
            plugin.getMessageUtils().send(sender, "&cUtilizare: /ainpc repair npc-bindings [dryrun|apply]");
            plugin.getMessageUtils().send(sender, "&cUtilizare: /ainpc repair mapping-metadata [dryrun|apply]");
            plugin.getMessageUtils().send(sender, "&cUtilizare: /ainpc repair batch <batchKey> [dryrun|apply|inspect|mark-steps|mark-failed]");
            plugin.getMessageUtils().send(sender, "&cUtilizare: /ainpc repair batch list [problem|all|failed|running|rolled_back|succeeded]");
            return true;
        }

        if (isRepairBatchTarget(args[1])) {
            if (args.length < 3 || args.length > 4) {
                plugin.getMessageUtils().send(sender, "&cUtilizare: /ainpc repair batch <batchKey> [dryrun|apply|inspect|mark-steps|mark-failed]");
                plugin.getMessageUtils().send(sender, "&cUtilizare: /ainpc repair batch list [problem|all|failed|running|rolled_back|succeeded]");
                return true;
            }
            if (isRepairBatchListAction(args[2])) {
                String filter = args.length >= 4 ? args[3] : "problem";
                return handleRepairSpawnBatchList(sender, filter);
            }
            String mode = args.length >= 4 ? args[3].toLowerCase(Locale.ROOT) : "dryrun";
            if (!"dryrun".equals(mode) && !"apply".equals(mode)
                && !"mark-steps".equals(mode) && !"sync-steps".equals(mode)
                && !"inspect".equals(mode) && !"steps".equals(mode)
                && !"mark-failed".equals(mode) && !"mark_failed".equals(mode)
                && !"fail".equals(mode) && !"abandon".equals(mode)) {
                plugin.getMessageUtils().send(sender,
                    "&cMod invalid. Foloseste: &fdryrun&c, &fapply&c, &finspect&c, &fmark-steps &csau &fmark-failed");
                return true;
            }
            return handleRepairSpawnBatch(sender, args[2], mode);
        }

        String mode = args.length >= 3 ? args[2].toLowerCase(Locale.ROOT) : "dryrun";
        if (!"dryrun".equals(mode) && !"apply".equals(mode)) {
            plugin.getMessageUtils().send(sender, "&cMod invalid. Foloseste: &fdryrun &csau &fapply");
            return true;
        }

        boolean apply = "apply".equals(mode);
        if ("households".equalsIgnoreCase(args[1])) {
            return handleRepairHouseholds(sender, apply);
        }
        if (isNpcBindingRepairTarget(args[1])) {
            return handleRepairNpcBindings(sender, apply);
        }
        if (isMappingMetadataRepairTarget(args[1])) {
            return handleRepairMappingMetadata(sender, apply);
        }

        var result = plugin.getNpcManager().repairDuplicateNPCs(apply);
        plugin.getMessageUtils().send(sender, apply
            ? "&6=== Repair duplicate NPC - APPLY ==="
            : "&6=== Repair duplicate NPC - DRYRUN ===");
        plugin.getMessageUtils().send(sender, "&eRanduri DB duplicate gasite: &f" + result.duplicateDbRows()
            + " &7| sterse=&f" + result.deletedDbRows());
        plugin.getMessageUtils().send(sender, "&eEntitati duplicate gasite: &f" + result.duplicateEntities()
            + " &7| eliminate=&f" + result.removedEntities()
            + " &7| reasociate=&f" + result.reassociatedEntities());
        plugin.getMessageUtils().send(sender, "&eProbleme index source_key: &f" + result.sourceKeyIndexIssues()
            + " &7| reindexate=&f" + result.reindexedSourceKeys());

        sendRepairMessages(sender, result.actions(), apply ? "&a" : "&e", 12);
        sendRepairMessages(sender, result.warnings(), "&e", 8);
        sendRepairMessages(sender, result.errors(), "&c", 8);

        if (!apply && (!result.actions().isEmpty()
            || result.duplicateDbRows() > 0
            || result.duplicateEntities() > 0
            || result.sourceKeyIndexIssues() > 0)) {
            plugin.getMessageUtils().send(sender, "&7Pentru aplicare: &f/ainpc repair duplicates apply");
        }
        if (result.actions().isEmpty() && result.warnings().isEmpty() && result.errors().isEmpty()) {
            plugin.getMessageUtils().send(sender, "&aNu sunt actiuni de reparatie necesare.");
        }
        return true;
    }

    private boolean isNpcBindingRepairTarget(String value) {
        return "npc-bindings".equalsIgnoreCase(value)
            || "npc_bindings".equalsIgnoreCase(value)
            || "world-bindings".equalsIgnoreCase(value)
            || "world_bindings".equalsIgnoreCase(value);
    }

    private boolean isMappingMetadataRepairTarget(String value) {
        return "mapping-metadata".equalsIgnoreCase(value)
            || "mapping_metadata".equalsIgnoreCase(value)
            || "metadata-mapping".equalsIgnoreCase(value)
            || "metadata_mapping".equalsIgnoreCase(value);
    }

    private boolean isRepairBatchTarget(String value) {
        return "batch".equalsIgnoreCase(value)
            || "spawn-batch".equalsIgnoreCase(value)
            || "spawn_batch".equalsIgnoreCase(value);
    }

    private boolean isRepairBatchListAction(String value) {
        return "list".equalsIgnoreCase(value)
            || "recent".equalsIgnoreCase(value)
            || "history".equalsIgnoreCase(value);
    }

    private boolean handleRepairHouseholds(CommandSender sender, boolean apply) {
        HouseholdPersistenceService service = requireHouseholdPersistence(sender);
        if (service == null) {
            return true;
        }

        try {
            HouseholdPersistenceService.HouseholdResidentRepairReport result =
                service.repairDuplicateResidents(apply, NPC_WORLD_BINDING_LOOKUP_LIMIT);
            plugin.getMessageUtils().send(sender, apply
                ? "&6=== Repair household residents - APPLY ==="
                : "&6=== Repair household residents - DRYRUN ===");
            plugin.getMessageUtils().send(sender,
                "&eGrupuri duplicate NPC/source_key: &f" + result.duplicateNpcGroups()
                    + "/" + result.duplicateSourceKeyGroups()
                    + " &7| randuri duplicate=&f" + result.duplicateResidentRows()
                    + " &7| sterse=&f" + result.deletedResidentRows()
                    + " &7| household-uri recalculate=&f" + result.updatedHouseholds());
            sendRepairMessages(sender, result.actions(), apply ? "&a" : "&e", 12);
            sendRepairMessages(sender, result.warnings(), "&e", 8);
            sendRepairMessages(sender, result.errors(), "&c", 8);

            if (!apply && result.duplicateResidentRows() > 0) {
                plugin.getMessageUtils().send(sender, "&7Pentru aplicare: &f/ainpc repair households apply");
            }
            if (result.actions().isEmpty() && result.warnings().isEmpty() && result.errors().isEmpty()) {
                plugin.getMessageUtils().send(sender, "&aNu sunt rezidenti household duplicati de reparat.");
            }
        } catch (SQLException exception) {
            plugin.getMessageUtils().send(sender,
                "&cRepair household residents a esuat: &e" + exception.getMessage());
        }
        return true;
    }

    private boolean handleRepairNpcBindings(CommandSender sender, boolean apply) {
        if (plugin.getNpcWorldBindingService() == null) {
            plugin.getMessageUtils().send(sender, "&cNpcWorldBindingService este indisponibil.");
            return true;
        }
        WorldAdminApi worldAdmin = plugin.getPlatform() != null ? plugin.getPlatform().getWorldAdmin() : null;
        if (worldAdmin == null || !worldAdmin.isEnabled()) {
            plugin.getMessageUtils().send(sender, "&cWorld admin este dezactivat sau indisponibil.");
            return true;
        }

        List<String> actions = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int scannedNpcs = 0;
        int candidates = 0;
        int missingBindings = 0;
        int divergentBindings = 0;
        int savedBindings = 0;

        try {
            Map<Integer, NpcWorldBinding> existingBindings = loadNpcWorldBindingsById();
            for (AINPC npc : plugin.getNpcManager().getAllNPCs()) {
                if (npc == null || npc.getDatabaseId() <= 0) {
                    continue;
                }
                scannedNpcs++;
                NpcWorldBinding inferred = inferNpcWorldBindingFromProfile(npc, worldAdmin, "profile_repair");
                if (inferred == null || !inferred.hasAnyPlaceBinding()) {
                    continue;
                }

                NpcWorldBinding existing = existingBindings.get(npc.getDatabaseId());
                NpcWorldBinding proposed = preserveBindingMetadata(inferred, existing, "profile_repair");
                if (existing == null) {
                    missingBindings++;
                } else if (!sameMappingBinding(existing, proposed)) {
                    divergentBindings++;
                } else {
                    continue;
                }

                candidates++;
                actions.add((apply ? "Salvez" : "Ar salva")
                    + " npc_world_bindings pentru " + npc.getName() + "#" + npc.getDatabaseId()
                    + ": " + formatNpcWorldBindingPlaces(proposed) + ".");
                if (apply) {
                    plugin.getNpcWorldBindingService().saveBinding(proposed);
                    savedBindings++;
                }
            }
        } catch (SQLException | IllegalArgumentException exception) {
            errors.add("Repair npc-bindings a esuat: " + exception.getMessage());
        }

        plugin.getMessageUtils().send(sender, apply
            ? "&6=== Repair npc_world_bindings din profil - APPLY ==="
            : "&6=== Repair npc_world_bindings din profil - DRYRUN ===");
        plugin.getMessageUtils().send(sender,
            "&eNPC-uri scanate: &f" + scannedNpcs
                + " &7| candidati=&f" + candidates
                + " &7| lipsa=&f" + missingBindings
                + " &7| divergente=&f" + divergentBindings
                + " &7| salvate=&f" + savedBindings);
        sendRepairMessages(sender, actions, apply ? "&a" : "&e", 16);
        sendRepairMessages(sender, warnings, "&e", 8);
        sendRepairMessages(sender, errors, "&c", 8);
        if (!apply && candidates > 0) {
            plugin.getMessageUtils().send(sender, "&7Pentru aplicare: &f/ainpc repair npc-bindings apply");
        }
        if (actions.isEmpty() && warnings.isEmpty() && errors.isEmpty()) {
            plugin.getMessageUtils().send(sender, "&aNu sunt divergente profil -> npc_world_bindings de reparat.");
        }
        return true;
    }

    private boolean handleRepairMappingMetadata(CommandSender sender, boolean apply) {
        if (plugin.getNpcWorldBindingService() == null) {
            plugin.getMessageUtils().send(sender, "&cNpcWorldBindingService este indisponibil.");
            return true;
        }
        WorldAdminApi worldAdmin = plugin.getPlatform() != null ? plugin.getPlatform().getWorldAdmin() : null;
        WorldAdminService worldAdminService = plugin.getPlatform() != null
            ? plugin.getPlatform().getWorldAdminService()
            : null;
        if (worldAdmin == null || !worldAdmin.isEnabled() || worldAdminService == null || !worldAdminService.isEnabled()) {
            plugin.getMessageUtils().send(sender, "&cWorld admin este dezactivat sau indisponibil.");
            return true;
        }

        List<String> actions = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int scannedBindings = 0;
        int candidates = 0;
        int appliedUpdates = 0;

        try {
            int totalBindings = plugin.getNpcWorldBindingService().countBindings();
            List<NpcWorldBinding> bindings = plugin.getNpcWorldBindingService()
                .listBindings(Math.max(NPC_WORLD_BINDING_LOOKUP_LIMIT, totalBindings));
            if (totalBindings > bindings.size()) {
                warnings.add("Repair mapping-metadata a scanat primele " + bindings.size()
                    + " randuri din " + totalBindings + ".");
            }

            for (NpcWorldBinding binding : bindings) {
                scannedBindings++;
                String npcSelector = "npc_" + binding.npcId();
                String npcName = firstNonBlank(binding.npcName(), npcSelector);
                candidates += collectMappingMetadataRepairActions(
                    worldAdmin,
                    actions,
                    warnings,
                    binding,
                    "home",
                    binding.homePlaceId(),
                    npcSelector
                );
                candidates += collectMappingMetadataRepairActions(
                    worldAdmin,
                    actions,
                    warnings,
                    binding,
                    "work",
                    binding.workPlaceId(),
                    npcSelector
                );
                candidates += collectMappingMetadataRepairActions(
                    worldAdmin,
                    actions,
                    warnings,
                    binding,
                    "social",
                    binding.socialPlaceId(),
                    npcSelector
                );

                if (!apply) {
                    continue;
                }
                try {
                    if (!binding.homePlaceId().isBlank()) {
                        worldAdminService.bindNpcToHomePlace(binding.homePlaceId(), npcSelector, npcName);
                        appliedUpdates++;
                    }
                    if (!binding.workPlaceId().isBlank()) {
                        worldAdminService.bindNpcToWorkPlace(binding.workPlaceId(), npcSelector, npcName);
                        appliedUpdates++;
                    }
                    if (!binding.socialPlaceId().isBlank()) {
                        worldAdminService.bindNpcToSocialPlace(binding.socialPlaceId(), npcSelector, npcName);
                        appliedUpdates++;
                    }
                } catch (IllegalArgumentException exception) {
                    errors.add("Nu am putut aplica metadata pentru npc_id=" + binding.npcId()
                        + ": " + exception.getMessage());
                }
            }
        } catch (SQLException exception) {
            errors.add("Repair mapping-metadata a esuat: " + exception.getMessage());
        }

        plugin.getMessageUtils().send(sender, apply
            ? "&6=== Repair metadata mapping din npc_world_bindings - APPLY ==="
            : "&6=== Repair metadata mapping din npc_world_bindings - DRYRUN ===");
        plugin.getMessageUtils().send(sender,
            "&eBinding-uri scanate: &f" + scannedBindings
                + " &7| actiuni candidate=&f" + candidates
                + " &7| update-uri aplicate=&f" + appliedUpdates);
        sendRepairMessages(sender, actions, apply ? "&a" : "&e", 16);
        sendRepairMessages(sender, warnings, "&e", 8);
        sendRepairMessages(sender, errors, "&c", 8);
        if (!apply && candidates > 0) {
            plugin.getMessageUtils().send(sender, "&7Pentru aplicare: &f/ainpc repair mapping-metadata apply");
        }
        if (apply && appliedUpdates > 0) {
            plugin.getMessageUtils().send(sender, "&7Ruleaza &f/ainpc world save &7ca sa persisti metadata mapping.");
        }
        if (actions.isEmpty() && warnings.isEmpty() && errors.isEmpty()) {
            plugin.getMessageUtils().send(sender, "&aNu sunt divergente npc_world_bindings -> metadata mapping de reparat.");
        }
        return true;
    }

    private Map<Integer, NpcWorldBinding> loadNpcWorldBindingsById() throws SQLException {
        int totalBindings = plugin.getNpcWorldBindingService().countBindings();
        List<NpcWorldBinding> bindings = plugin.getNpcWorldBindingService()
            .listBindings(Math.max(NPC_WORLD_BINDING_LOOKUP_LIMIT, totalBindings));
        Map<Integer, NpcWorldBinding> byNpcId = new HashMap<>();
        for (NpcWorldBinding binding : bindings) {
            byNpcId.put(binding.npcId(), binding);
        }
        return byNpcId;
    }

    private NpcWorldBinding inferNpcWorldBindingFromProfile(AINPC npc,
                                                            WorldAdminApi worldAdmin,
                                                            String source) {
        if (npc == null || worldAdmin == null || !worldAdmin.isEnabled()) {
            return null;
        }
        WorldPlaceInfo homePlace = inferProfileAnchorPlace(worldAdmin, npc.getHomeAnchor());
        WorldPlaceInfo workPlace = inferProfileAnchorPlace(worldAdmin, npc.getWorkAnchor());
        WorldPlaceInfo socialPlace = inferProfileAnchorPlace(worldAdmin, npc.getSocialAnchor());
        if (homePlace == null && workPlace == null && socialPlace == null) {
            return null;
        }

        WorldNodeInfo homeNode = inferProfileAnchorNode(worldAdmin, npc.getHomeAnchor(), homePlace);
        WorldNodeInfo workNode = inferProfileAnchorNode(worldAdmin, npc.getWorkAnchor(), workPlace);
        WorldNodeInfo socialNode = inferProfileAnchorNode(worldAdmin, npc.getSocialAnchor(), socialPlace);
        return new NpcWorldBinding(
            npc.getDatabaseId(),
            npc.getUuid() != null ? npc.getUuid().toString() : "",
            npc.getName(),
            homePlace != null ? homePlace.id() : "",
            workPlace != null ? workPlace.id() : "",
            socialPlace != null ? socialPlace.id() : "",
            homeNode != null ? homeNode.id() : "",
            workNode != null ? workNode.id() : "",
            socialNode != null ? socialNode.id() : "",
            "",
            source,
            0L,
            0L
        );
    }

    private WorldPlaceInfo inferProfileAnchorPlace(WorldAdminApi worldAdmin, AINPC.OwnedLocation anchor) {
        if (worldAdmin == null || anchor == null || anchor.worldName() == null || anchor.worldName().isBlank()) {
            return null;
        }
        return worldAdmin.findPlace(
            anchor.worldName(),
            (int) Math.floor(anchor.x()),
            (int) Math.floor(anchor.y()),
            (int) Math.floor(anchor.z())
        );
    }

    private WorldNodeInfo inferProfileAnchorNode(WorldAdminApi worldAdmin,
                                                 AINPC.OwnedLocation anchor,
                                                 WorldPlaceInfo place) {
        if (worldAdmin == null || anchor == null || anchor.worldName() == null || anchor.worldName().isBlank()) {
            return null;
        }
        return worldAdmin.findNodesNear(anchor.worldName(), anchor.x(), anchor.y(), anchor.z(), 2.5D, 5)
            .stream()
            .filter(node -> place == null || node.placeId().isBlank() || node.placeId().equalsIgnoreCase(place.id()))
            .findFirst()
            .orElse(null);
    }

    private NpcWorldBinding preserveBindingMetadata(NpcWorldBinding proposed,
                                                    NpcWorldBinding existing,
                                                    String source) {
        if (existing == null) {
            return proposed;
        }
        return new NpcWorldBinding(
            proposed.npcId(),
            firstNonBlank(proposed.npcUuid(), existing.npcUuid()),
            firstNonBlank(proposed.npcName(), existing.npcName()),
            proposed.homePlaceId(),
            proposed.workPlaceId(),
            proposed.socialPlaceId(),
            proposed.homeNodeId(),
            proposed.workNodeId(),
            proposed.socialNodeId(),
            existing.familyId(),
            source,
            existing.createdAt(),
            0L
        );
    }

    private boolean sameMappingBinding(NpcWorldBinding left, NpcWorldBinding right) {
        return sameOptionalId(left.homePlaceId(), right.homePlaceId())
            && sameOptionalId(left.workPlaceId(), right.workPlaceId())
            && sameOptionalId(left.socialPlaceId(), right.socialPlaceId())
            && sameOptionalId(left.homeNodeId(), right.homeNodeId())
            && sameOptionalId(left.workNodeId(), right.workNodeId())
            && sameOptionalId(left.socialNodeId(), right.socialNodeId());
    }

    private String formatNpcWorldBindingPlaces(NpcWorldBinding binding) {
        return "home=" + formatOptional(binding.homePlaceId())
            + " work=" + formatOptional(binding.workPlaceId())
            + " social=" + formatOptional(binding.socialPlaceId())
            + " nodes=" + formatOptional(binding.homeNodeId())
            + "/" + formatOptional(binding.workNodeId())
            + "/" + formatOptional(binding.socialNodeId());
    }

    private int collectMappingMetadataRepairActions(WorldAdminApi worldAdmin,
                                                    List<String> actions,
                                                    List<String> warnings,
                                                    NpcWorldBinding binding,
                                                    String role,
                                                    String placeId,
                                                    String npcSelector) {
        if (placeId == null || placeId.isBlank()) {
            return 0;
        }
        WorldPlaceInfo place = worldAdmin.getPlace(placeId);
        if (place == null) {
            warnings.add("npc_world_bindings npc_id=" + binding.npcId()
                + " refera " + role + "_place_id inexistent: " + placeId + ".");
            return 0;
        }

        int candidates = 0;
        switch (role) {
            case "home" -> {
                if (!sameOptionalId(place.ownerNpcId(), npcSelector)) {
                    actions.add("Ar seta owner_npc_id pentru " + place.id()
                        + " la " + npcSelector + " din binding npc_id=" + binding.npcId() + ".");
                    candidates++;
                }
                if (!metadataListContains(place, "resident_npc_ids", npcSelector)) {
                    actions.add("Ar adauga " + npcSelector + " in resident_npc_ids pentru " + place.id() + ".");
                    candidates++;
                }
            }
            case "work" -> {
                if (!metadataListContains(place, "worker_npc_ids", npcSelector)) {
                    actions.add("Ar adauga " + npcSelector + " in worker_npc_ids pentru " + place.id() + ".");
                    candidates++;
                }
            }
            case "social" -> {
                if (!metadataListContains(place, "social_npc_ids", npcSelector)) {
                    actions.add("Ar adauga " + npcSelector + " in social_npc_ids pentru " + place.id() + ".");
                    candidates++;
                }
            }
            default -> {
                // Rolurile validate mai sus sunt cele persistate in npc_world_bindings.
            }
        }
        return candidates;
    }

    private boolean metadataListContains(WorldPlaceInfo place, String key, String expected) {
        String normalizedExpected = normalizeAuditKey(expected);
        if (normalizedExpected.isBlank()) {
            return false;
        }
        String raw = place.metadata().getOrDefault(key, "");
        if (raw.isBlank()) {
            return false;
        }
        for (String part : raw.split("[,;]")) {
            if (normalizeAuditKey(part).equals(normalizedExpected)) {
                return true;
            }
        }
        return false;
    }

    private boolean handleRepairSpawnBatch(CommandSender sender, String batchKey, String mode) {
        if (plugin.getDatabaseManager() == null) {
            plugin.getMessageUtils().send(sender, "&cDatabaseManager este indisponibil.");
            return true;
        }

        String safeBatchKey = batchKey == null ? "" : batchKey.trim();
        if (safeBatchKey.isBlank()) {
            plugin.getMessageUtils().send(sender, "&cUtilizare: /ainpc repair batch <batchKey> [dryrun|apply|inspect|mark-steps|mark-failed]");
            return true;
        }

        SpawnBatchTracker tracker = new SpawnBatchTracker(plugin.getDatabaseManager(), plugin.getLogger());
        Optional<SpawnBatchTracker.BatchRecord> batchRecord = tracker.findBatch(safeBatchKey);
        if (batchRecord.isEmpty()) {
            plugin.getMessageUtils().send(sender, "&cNu exista spawn batch cu cheia: &e" + safeBatchKey);
            return true;
        }

        String safeMode = mode == null || mode.isBlank() ? "dryrun" : mode.toLowerCase(Locale.ROOT);
        boolean apply = "apply".equals(safeMode);
        SpawnBatchTracker.BatchRecord batch = batchRecord.get();
        if ("mark-steps".equals(safeMode) || "sync-steps".equals(safeMode)) {
            return handleRepairSpawnBatchSteps(sender, tracker, batch, safeBatchKey);
        }
        if ("inspect".equals(safeMode) || "steps".equals(safeMode)) {
            return handleRepairSpawnBatchInspect(sender, tracker, batch, safeBatchKey);
        }
        if ("mark-failed".equals(safeMode) || "mark_failed".equals(safeMode)
            || "fail".equals(safeMode) || "abandon".equals(safeMode)) {
            return handleRepairSpawnBatchMarkFailed(sender, tracker, batch, safeBatchKey);
        }

        List<Integer> createdNpcIds = tracker.findCreatedNpcIdsForBatch(safeBatchKey);
        List<String> affectedHouseholdIds = tracker.findHouseholdIdsForBatch(safeBatchKey);
        List<String> actions = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int existingNpcCount = 0;
        int deletedNpcCount = 0;

        plugin.getMessageUtils().send(sender, apply
            ? "&6=== Repair spawn batch rollback - APPLY ==="
            : "&6=== Repair spawn batch rollback - DRYRUN ===");
        plugin.getMessageUtils().send(sender,
            "&eBatch: &f" + safeBatchKey
                + " &7status=&f" + batch.status()
                + " &7scope=&f" + batch.scopeType() + "/" + batch.scopeId()
                + " &7dry_run=&f" + batch.dryRun());
        if (!affectedHouseholdIds.isEmpty()) {
            plugin.getMessageUtils().send(sender,
                "&eHousehold-uri afectate: &f" + String.join(", ", affectedHouseholdIds));
        }

        if (batch.dryRun()) {
            plugin.getMessageUtils().send(sender, "&eBatch-ul este dry-run; nu exista NPC-uri reale de sters.");
            return true;
        }
        if (apply && SpawnBatchTracker.STATUS_SUCCEEDED.equals(batch.status())) {
            plugin.getMessageUtils().send(sender,
                "&cRefuz apply pe batch SUCCEEDED. Foloseste cleanup manual dupa audit daca vrei sa stergi un spawn valid.");
            return true;
        }
        if (createdNpcIds.isEmpty()) {
            plugin.getMessageUtils().send(sender,
                "&eBatch-ul nu are created_npc_ids in spawn_batch_steps; nu am ce sterge automat.");
            return true;
        }

        for (int npcId : createdNpcIds) {
            AINPC npc = plugin.getNpcManager().getNPCById(npcId);
            if (npc == null) {
                warnings.add("NPC id=" + npcId + " este deja absent din cache/DB.");
                continue;
            }

            existingNpcCount++;
            if (!apply) {
                actions.add("Ar sterge NPC " + npc.getName() + "#" + npc.getDatabaseId()
                    + " creat de batch.");
                continue;
            }

            if (plugin.getNpcManager().deleteNPC(npc)) {
                deletedNpcCount++;
                actions.add("Sters NPC " + npc.getName() + "#" + npc.getDatabaseId()
                    + " creat de batch.");
            } else {
                errors.add("Nu am putut sterge NPC " + npc.getName() + "#" + npc.getDatabaseId() + ".");
            }
        }

        if (!affectedHouseholdIds.isEmpty()) {
            if (!apply) {
                actions.add("Ar recalcula resident_count pentru household-uri: "
                    + String.join(", ", affectedHouseholdIds) + ".");
            } else if (plugin.getHouseholdPersistenceService() != null) {
                try {
                    int updatedHouseholds = plugin.getHouseholdPersistenceService()
                        .recalculateResidentCounts(affectedHouseholdIds);
                    actions.add("Am recalculat resident_count pentru " + updatedHouseholds + " household-uri.");
                } catch (SQLException exception) {
                    errors.add("Nu am putut recalcula resident_count pentru household-uri: " + exception.getMessage());
                }
            } else {
                warnings.add("HouseholdPersistenceService indisponibil; resident_count nu a fost recalculat.");
            }
        }
        boolean rollbackComplete = !apply || errors.isEmpty();
        if (apply && rollbackComplete) {
            int rolledBackSteps = tracker.markCreatedStepsRolledBack(safeBatchKey);
            actions.add("Am marcat " + rolledBackSteps + " pasi batch ca ROLLED_BACK.");
        }

        plugin.getMessageUtils().send(sender,
            "&eNPC-uri create in pasi: &f" + createdNpcIds.size()
                + " &7| existente=&f" + existingNpcCount
                + " &7| sterse=&f" + deletedNpcCount);
        sendRepairMessages(sender, actions, apply ? "&a" : "&e", 16);
        sendRepairMessages(sender, warnings, "&e", 8);
        sendRepairMessages(sender, errors, "&c", 8);

        if (apply) {
            tracker.finishBatch(
                safeBatchKey,
                false,
                rollbackComplete,
                createdNpcIds.size(),
                batch.reusedNpcCount(),
                warnings,
                errors
            );
            plugin.getMessageUtils().send(sender, rollbackComplete
                ? "&aBatch marcat ROLLED_BACK."
                : "&cBatch ramas FAILED deoarece unele stergeri au esuat.");
        } else if (existingNpcCount > 0) {
            plugin.getMessageUtils().send(sender,
                "&7Pentru aplicare: &f/ainpc repair batch " + safeBatchKey + " apply");
        }

        if (actions.isEmpty() && warnings.isEmpty() && errors.isEmpty()) {
            plugin.getMessageUtils().send(sender, "&aNu sunt actiuni de rollback necesare pentru acest batch.");
        }
        return true;
    }

    private boolean handleRepairSpawnBatchList(CommandSender sender, String filter) {
        if (plugin.getDatabaseManager() == null) {
            plugin.getMessageUtils().send(sender, "&cDatabaseManager este indisponibil.");
            return true;
        }

        if (!SpawnBatchTracker.isSupportedBatchStatusFilter(filter)) {
            plugin.getMessageUtils().send(sender,
                "&cFiltru invalid. Foloseste: &fproblem&c, &fall&c, &ffailed&c, &frunning&c, &frolled_back &csau &fsucceeded");
            return true;
        }

        String normalizedFilter = SpawnBatchTracker.normalizeBatchStatusFilter(filter);
        SpawnBatchTracker tracker = new SpawnBatchTracker(plugin.getDatabaseManager(), plugin.getLogger());
        List<SpawnBatchTracker.BatchRecord> batches =
            tracker.findRecentBatches(normalizedFilter, SPAWN_BATCH_DEFAULT_LIMIT);

        plugin.getMessageUtils().send(sender, "&6=== Spawn batch history ===");
        plugin.getMessageUtils().send(sender,
            "&eFiltru: &f" + normalizedFilter + " &7| limita=&f" + SPAWN_BATCH_DEFAULT_LIMIT);

        if (batches.isEmpty()) {
            plugin.getMessageUtils().send(sender, "&aNu exista spawn batches pentru filtrul ales.");
            return true;
        }

        for (SpawnBatchTracker.BatchRecord batch : batches) {
            int pendingCreatorSteps = SpawnBatchTracker.STATUS_RUNNING.equals(batch.status())
                ? tracker.countCreatorStepsForBatch(batch.batchKey())
                : 0;
            String pendingRollback = pendingCreatorSteps <= 0
                ? ""
                : " &crollback_pending_steps=&f" + pendingCreatorSteps;
            plugin.getMessageUtils().send(sender,
                "&e" + batch.batchKey()
                    + " &7status=&f" + batch.status()
                    + " &7scope=&f" + batch.scopeType() + "/" + batch.scopeId()
                    + " &7dry_run=&f" + batch.dryRun()
                    + " &7created/reused=&f" + batch.createdNpcCount() + "/" + batch.reusedNpcCount()
                    + " &7updated=&f" + formatStoryTime(batch.updatedAt())
                    + pendingRollback);
        }

        plugin.getMessageUtils().send(sender,
            "&7Rollback controlat: &f/ainpc repair batch <batchKey> dryrun");
        plugin.getMessageUtils().send(sender,
            "&7Pasi rollback deja stersi: &f/ainpc repair batch <batchKey> mark-steps");
        return true;
    }

    private boolean handleRepairSpawnBatchInspect(CommandSender sender,
                                                  SpawnBatchTracker tracker,
                                                  SpawnBatchTracker.BatchRecord batch,
                                                  String batchKey) {
        List<SpawnBatchTracker.BatchStepRecord> steps = tracker.findBatchSteps(batchKey);

        plugin.getMessageUtils().send(sender, "&6=== Spawn batch inspect ===");
        plugin.getMessageUtils().send(sender,
            "&eBatch: &f" + batchKey
                + " &7status=&f" + batch.status()
                + " &7scope=&f" + batch.scopeType() + "/" + batch.scopeId()
                + " &7dry_run=&f" + batch.dryRun()
                + " &7plan=&f" + shortenBatchValue(batch.planHash(), 16));

        int creatorSteps = SpawnBatchTracker.STATUS_RUNNING.equals(batch.status())
            ? tracker.countCreatorStepsForBatch(batchKey)
            : 0;
        if (creatorSteps > 0) {
            List<Integer> createdNpcIds = tracker.findCreatedNpcIdsForBatch(batchKey);
            plugin.getMessageUtils().send(sender,
                "&cRetry blocat: batch-ul este RUNNING si are &f" + creatorSteps
                    + " &cpasi cu NPC-uri create jurnalizate"
                    + (createdNpcIds.isEmpty() ? "" : " &7(" + createdNpcIds.size() + " ID-uri parsabile)")
                    + "&c. Ruleaza dryrun/apply inainte de rerulare.");
        }

        if (steps.isEmpty()) {
            plugin.getMessageUtils().send(sender, "&eBatch-ul nu are pasi persistati in spawn_batch_steps.");
            return true;
        }

        int displayed = Math.min(SPAWN_BATCH_STEP_PREVIEW_LIMIT, steps.size());
        for (int index = 0; index < displayed; index++) {
            SpawnBatchTracker.BatchStepRecord step = steps.get(index);
            plugin.getMessageUtils().send(sender,
                "&e#" + step.stepIndex()
                    + " &7status=&f" + step.status()
                    + " &7place=&f" + valueOrDash(step.stepKey())
                    + " &7household=&f" + valueOrDash(step.householdId())
                    + " &7created=&f" + formatBatchNpcIdList(step.createdNpcIds())
                    + " &7reused=&f" + formatBatchNpcIdList(step.reusedNpcIds())
                    + " &7updated=&f" + formatStoryTime(step.updatedAt()));
            if (step.warningSummary() != null && !step.warningSummary().isBlank()) {
                plugin.getMessageUtils().send(sender,
                    "&e  warning: &f" + shortenBatchValue(step.warningSummary(), 110));
            }
            if (step.errorSummary() != null && !step.errorSummary().isBlank()) {
                plugin.getMessageUtils().send(sender,
                    "&c  error: &f" + shortenBatchValue(step.errorSummary(), 110));
            }
        }
        if (steps.size() > displayed) {
            plugin.getMessageUtils().send(sender,
                "&7... inca &f" + (steps.size() - displayed) + " &7pasi in batch.");
        }

        plugin.getMessageUtils().send(sender,
            "&7Rollback preview: &f/ainpc repair batch " + batchKey + " dryrun");
        return true;
    }

    private boolean handleRepairSpawnBatchMarkFailed(CommandSender sender,
                                                     SpawnBatchTracker tracker,
                                                     SpawnBatchTracker.BatchRecord batch,
                                                     String batchKey) {
        plugin.getMessageUtils().send(sender, "&6=== Repair spawn batch mark-failed ===");
        plugin.getMessageUtils().send(sender,
            "&eBatch: &f" + batchKey
                + " &7status=&f" + batch.status()
                + " &7scope=&f" + batch.scopeType() + "/" + batch.scopeId()
                + " &7dry_run=&f" + batch.dryRun());

        if (SpawnBatchTracker.STATUS_SUCCEEDED.equals(batch.status())) {
            plugin.getMessageUtils().send(sender,
                "&cRefuz mark-failed pe batch SUCCEEDED. Foloseste cleanup manual dupa audit daca este nevoie.");
            return true;
        }
        if (SpawnBatchTracker.STATUS_ROLLED_BACK.equals(batch.status())) {
            plugin.getMessageUtils().send(sender,
                "&eBatch-ul este deja ROLLED_BACK. Pentru pasi nemarcati foloseste mark-steps.");
            return true;
        }
        if (SpawnBatchTracker.STATUS_FAILED.equals(batch.status())) {
            plugin.getMessageUtils().send(sender, "&aBatch-ul este deja FAILED.");
            return true;
        }

        int creatorSteps = tracker.countCreatorStepsForBatch(batchKey);
        List<Integer> parsedNpcIds = tracker.findCreatedNpcIdsForBatch(batchKey);
        if (!parsedNpcIds.isEmpty()) {
            plugin.getMessageUtils().send(sender,
                "&cRefuz mark-failed: batch-ul are &f" + parsedNpcIds.size()
                    + " &cID-uri parsabile pentru rollback. Ruleaza mai intai dryrun/apply.");
            return true;
        }

        List<String> warnings = new ArrayList<>();
        warnings.add("Batch marcat FAILED manual prin /ainpc repair batch mark-failed; nu s-au sters NPC-uri.");
        if (creatorSteps > 0) {
            warnings.add("Exista " + creatorSteps
                + " pasi creatori cu created_npc_ids neparsabile sau fara ID-uri valide.");
        }
        tracker.finishBatch(
            batchKey,
            false,
            false,
            batch.createdNpcCount(),
            batch.reusedNpcCount(),
            warnings,
            List.of()
        );

        plugin.getMessageUtils().send(sender,
            "&aBatch marcat FAILED manual. Nu am sters NPC-uri si nu am modificat pasii.");
        if (creatorSteps > 0) {
            plugin.getMessageUtils().send(sender,
                "&eAtentie: exista &f" + creatorSteps
                    + " &epasi creatori neparsabili. Verifica inspect/debugdump inainte de retry.");
        }
        plugin.getMessageUtils().send(sender,
            "&7Urmatorul pas: &f/ainpc audit db &7apoi reruleaza spawn-ul doar daca datele sunt corecte.");
        return true;
    }

    private boolean handleRepairSpawnBatchSteps(CommandSender sender,
                                                SpawnBatchTracker tracker,
                                                SpawnBatchTracker.BatchRecord batch,
                                                String batchKey) {
        plugin.getMessageUtils().send(sender, "&6=== Repair spawn batch steps ===");
        plugin.getMessageUtils().send(sender,
            "&eBatch: &f" + batchKey
                + " &7status=&f" + batch.status()
                + " &7scope=&f" + batch.scopeType() + "/" + batch.scopeId()
                + " &7dry_run=&f" + batch.dryRun());

        if (!SpawnBatchTracker.STATUS_ROLLED_BACK.equals(batch.status())) {
            plugin.getMessageUtils().send(sender,
                "&cRefuz marcarea pasilor: batch-ul nu este ROLLED_BACK.");
            return true;
        }

        int rolledBackSteps = tracker.markCreatedStepsRolledBack(batchKey);
        plugin.getMessageUtils().send(sender,
            rolledBackSteps > 0
                ? "&aAm marcat &f" + rolledBackSteps + " &apasi batch ca ROLLED_BACK."
                : "&eNu am gasit pasi cu created_npc_ids care trebuie marcati.");
        return true;
    }

    private String formatBatchNpcIdList(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return "-";
        }
        String[] tokens = rawValue.split(",");
        List<String> values = new ArrayList<>();
        for (String token : tokens) {
            if (token == null || token.isBlank()) {
                continue;
            }
            values.add(token.trim());
            if (values.size() >= 3) {
                break;
            }
        }
        if (values.isEmpty()) {
            return "-";
        }
        String formatted = String.join(",", values);
        int total = 0;
        for (String token : tokens) {
            if (token != null && !token.isBlank()) {
                total++;
            }
        }
        if (total > values.size()) {
            formatted += ",+" + (total - values.size());
        }
        return shortenBatchValue(formatted, 72);
    }

    private void sendRepairMessages(CommandSender sender, List<String> messages, String color, int limit) {
        int safeLimit = Math.max(0, limit);
        int displayed = Math.min(safeLimit, messages.size());
        for (int index = 0; index < displayed; index++) {
            plugin.getMessageUtils().send(sender, color + messages.get(index));
        }
        if (messages.size() > displayed) {
            plugin.getMessageUtils().send(sender, "&7... inca &f" + (messages.size() - displayed) + " &7mesaje.");
        }
    }

    private void collectSourceKeyDuplicateFindings(List<AINPC> npcs, List<String> findings) {
        Map<String, List<AINPC>> bySourceKey = new HashMap<>();
        for (AINPC npc : npcs) {
            if (npc.getSourceKey() == null || npc.getSourceKey().isBlank()) {
                continue;
            }
            bySourceKey.computeIfAbsent(normalizeAuditKey(npc.getSourceKey()), ignored -> new ArrayList<>()).add(npc);
        }

        for (Map.Entry<String, List<AINPC>> entry : bySourceKey.entrySet()) {
            if (entry.getValue().size() <= 1) {
                continue;
            }
            List<AINPC> sorted = entry.getValue().stream()
                .sorted(Comparator.comparingInt(AINPC::getDatabaseId))
                .toList();
            findings.add("&csource_key duplicat &f" + entry.getKey()
                + " &7canonic=&f" + formatNpcIdentity(sorted.get(0))
                + " &7duplicate=&f" + formatNpcIdentities(sorted.subList(1, sorted.size())));
        }
    }

    private void collectNearbyNameDuplicateFindings(List<AINPC> npcs, List<String> findings) {
        Set<String> seenPairs = new HashSet<>();
        for (int leftIndex = 0; leftIndex < npcs.size(); leftIndex++) {
            AINPC left = npcs.get(leftIndex);
            for (int rightIndex = leftIndex + 1; rightIndex < npcs.size(); rightIndex++) {
                AINPC right = npcs.get(rightIndex);
                if (!normalizeAuditKey(left.getName()).equals(normalizeAuditKey(right.getName()))) {
                    continue;
                }
                if (!sameNearbyLocation(left.getLocation(), right.getLocation(), 2.25D)) {
                    continue;
                }

                String pairKey = Math.min(left.getDatabaseId(), right.getDatabaseId())
                    + ":" + Math.max(left.getDatabaseId(), right.getDatabaseId());
                if (seenPairs.add(pairKey)) {
                    findings.add("&eNume si locatie aproape identice: &f"
                        + formatNpcIdentity(left) + " &7<-> &f" + formatNpcIdentity(right)
                        + " &7la &f" + formatLocation(left.getLocation()));
                }
            }
        }
    }

    private boolean sameNearbyLocation(Location left, Location right, double maxDistanceSquared) {
        if (left == null || right == null || left.getWorld() == null || right.getWorld() == null) {
            return false;
        }
        return left.getWorld().equals(right.getWorld()) && left.distanceSquared(right) <= maxDistanceSquared;
    }

    private String formatNpcIdentities(List<AINPC> npcs) {
        return npcs.stream()
            .map(this::formatNpcIdentity)
            .toList()
            .toString();
    }

    private String formatNpcIdentity(AINPC npc) {
        return npc.getName() + "#" + npc.getDatabaseId();
    }

    /**
     * /ainpc info [nume|nearest]
     */
    private boolean handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ainpc.info")) {
            plugin.getMessageUtils().sendMessage(sender, "no_permission");
            return true;
        }

        AINPC npc;
        
        if (args.length < 2 || "nearest".equalsIgnoreCase(args[1])) {
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
        if (args.length >= 3 && !"nearest".equalsIgnoreCase(args[2])) {
            npc = plugin.getNpcManager().getNPCByName(args[2]);
        } else if (sender instanceof Player player) {
            List<AINPC> nearby = plugin.getNpcManager().getNPCsNear(player.getLocation(), 10);
            npc = nearby.isEmpty() ? null : nearby.get(0);
        } else {
            plugin.getMessageUtils().send(sender, "&cUtilizare: /ainpc routine status <numeNpc|nearest>");
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
        sendRoutineMovementStatus(sender, npc, assignment);
        plugin.getMessageUtils().send(sender, "&eRutina curenta salvata: &f" + formatOptional(npc.getPlannedRoutineActivity()));
        plugin.getMessageUtils().send(sender, "&eObiectiv curent: &f" + formatOptional(npc.getCurrentGoal()));
        return true;
    }

    private void sendRoutineMovementStatus(CommandSender sender, AINPC npc, RoutineAssignment assignment) {
        Location current = npc.getLocation();
        Location target = assignment.targetAnchor() == null ? null : assignment.targetAnchor().toLocation();
        plugin.getMessageUtils().send(sender, "&eLocatie curenta: &f" + formatLocation(current));
        plugin.getMessageUtils().send(sender, "&eDistanta pana la tinta: &f" + formatDistance(current, target));

        Entity entity = npc.getBukkitEntity();
        if (entity == null || !entity.isValid()) {
            plugin.getMessageUtils().send(sender, "&eEntitate: &cneatasata sau invalida");
            return;
        }

        String ai = entity instanceof Mob mob ? formatOnOff(mob.hasAI()) : "n/a";
        String gravity = formatOnOff(entity.hasGravity());
        String collidable = entity instanceof LivingEntity livingEntity ? formatOnOff(livingEntity.isCollidable()) : "n/a";
        String silent = formatOnOff(entity.isSilent());
        String path = entity instanceof Mob mob ? formatOnOff(mob.getPathfinder().hasPath()) : "n/a";
        plugin.getMessageUtils().send(sender,
            "&eMiscare live: &fAI=" + ai
                + " &7gravity=&f" + gravity
                + " &7coliziune=&f" + collidable
                + " &7silent=&f" + silent
                + " &7path=&f" + path);
        plugin.getMessageUtils().send(sender,
            "&eConfig miscare: &fnatural=" + formatOnOff(plugin.getConfig().getBoolean("npc.natural_movement", true))
                + " &7gravity=&f" + formatOnOff(plugin.getConfig().getBoolean("npc.gravity", true))
                + " &7routineNatural=&f" + formatOnOff(plugin.getConfig().getBoolean("routine.natural_movement.enabled", true))
                + " &7teleportFallback=&f" + formatOnOff(plugin.getConfig().getBoolean("routine.teleport_enabled", true)));
    }

    private void sendRoutineUsage(CommandSender sender) {
        plugin.getMessageUtils().send(sender, "&cUtilizare:");
        plugin.getMessageUtils().send(sender, "&e/ainpc routine tick &7- ruleaza manual rutina pentru NPC-urile active");
        plugin.getMessageUtils().send(sender, "&e/ainpc routine status [numeNpc|nearest] &7- previzualizeaza rutina unui NPC");
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
     * /ainpc gui [main|quest|progresii|story|world|stats|interact|routine|shop|manager|audit|debug] [questFilter]
     */
    private boolean handleGui(CommandSender sender, String[] args) {
        Player player = requirePlayerSender(sender);
        if (player == null) {
            return true;
        }

        if (args.length > 3) {
            plugin.getMessageUtils().send(sender,
                "&cUtilizare: /ainpc gui [main|quest|progresii|story|world|stats|interact|routine|shop|manager|audit|debug] [questFilter]");
            return true;
        }

        String rawKey = args.length >= 2 ? args[1] : "main";
        Optional<GuiKey> resolvedKey = GuiKey.fromId(rawKey);
        if (resolvedKey.isEmpty()) {
            plugin.getMessageUtils().send(sender,
                "&cGUI necunoscut. Optiuni: &fmain, quest/progresii, story, world, stats, interact, routine, shop, manager, audit, debug");
            return true;
        }

        if (args.length == 3 && resolvedKey.get() != GuiKey.QUEST) {
            plugin.getMessageUtils().send(sender,
                "&cFiltrul este disponibil doar pentru /ainpc gui quest|progresii <filter>.");
            return true;
        }

        if (resolvedKey.get() == GuiKey.QUEST && args.length == 3) {
            plugin.getGuiService().openQuestLog(player, args[2]);
            return true;
        }

        plugin.getGuiService().open(player, resolvedKey.get());
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

        OpenAIDebugSnapshot snapshot = plugin.getOpenAIService().captureDebugSnapshot();
        plugin.getMessageUtils().send(sender, "&eModel runtime: &f" + snapshot.getModel());
        plugin.getMessageUtils().send(sender, "&eEndpoint runtime: &f" + snapshot.getBaseUrl());
        plugin.getMessageUtils().send(sender, "&eBackoff: &f" + (snapshot.getBackoffActive()
            ? "activ (" + snapshot.getBackoffRemainingSeconds() + "s)"
            : "inactiv"));
        if (snapshot.getLastPromptChars() > 0) {
            plugin.getMessageUtils().send(sender, "&eUltimul prompt: &f" + snapshot.getLastPromptChars()
                + " chars &7la &f" + formatStoryTime(snapshot.getLastRequestAtMillis()));
        }
        if (snapshot.getLastResponseChars() > 0) {
            plugin.getMessageUtils().send(sender, "&eUltimul raspuns model: &f" + snapshot.getLastResponseChars()
                + " chars &7la &f" + formatStoryTime(snapshot.getLastResponseAtMillis()));
        }
        if (snapshot.getLastFailureAtMillis() > 0) {
            plugin.getMessageUtils().send(sender, "&eUltima eroare OpenAI: &f"
                + sanitizeForChat(snapshot.getLastFailureMessage())
                + " &7la &f" + formatStoryTime(snapshot.getLastFailureAtMillis()));
        }
        if (snapshot.getLastFallbackAtMillis() > 0) {
            plugin.getMessageUtils().send(sender, "&eUltimul fallback: &f"
                + sanitizeForChat(snapshot.getLastFallbackReason())
                + " &7la &f" + formatStoryTime(snapshot.getLastFallbackAtMillis()));
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

    private String sanitizeForChat(String message) {
        if (message == null || message.isBlank()) {
            return "<gol>";
        }
        return message
            .replace("&", "")
            .replace('\n', ' ')
            .replace('\r', ' ')
            .trim();
    }

    /**
     * Afiseaza mesajul de ajutor
     */
    private void sendHelp(CommandSender sender) {
        plugin.getMessageUtils().send(sender, "&6=== AI NPC Plugin - Comenzi ===");
        plugin.getMessageUtils().send(sender, "&e/ainpc create <nume> [ocupatie] [varsta] [gen] [arhetip]");
        plugin.getMessageUtils().send(sender, "&7  Creeaza un NPC nou la locatia ta");
        plugin.getMessageUtils().send(sender, "&e/ainpc delete <nume>");
        plugin.getMessageUtils().send(sender, "&7  Sterge un NPC dupa nume; evita daca numele este duplicat");
        plugin.getMessageUtils().send(sender, "&e/ainpc delete-id <id> confirm");
        plugin.getMessageUtils().send(sender, "&7  Sterge sigur un NPC dupa ID numeric");
        plugin.getMessageUtils().send(sender, "&e/ainpc duplicates");
        plugin.getMessageUtils().send(sender, "&7  Raporteaza duplicate dupa source_key, nume+locatie si entitati live");
        plugin.getMessageUtils().send(sender, "&e/ainpc repair duplicates [dryrun|apply]");
        plugin.getMessageUtils().send(sender, "&7  Curata controlat randuri/entitati NPC duplicate; ruleaza dryrun inainte de apply");
        plugin.getMessageUtils().send(sender, "&e/ainpc repair households [dryrun|apply]");
        plugin.getMessageUtils().send(sender, "&7  Curata rezidenti household duplicati dupa NPC/source_key; ruleaza dryrun inainte de apply");
        plugin.getMessageUtils().send(sender, "&e/ainpc repair npc-bindings [dryrun|apply]");
        plugin.getMessageUtils().send(sender, "&7  Sincronizeaza profilul NPC catre npc_world_bindings; ruleaza dryrun inainte de apply");
        plugin.getMessageUtils().send(sender, "&e/ainpc repair mapping-metadata [dryrun|apply]");
        plugin.getMessageUtils().send(sender, "&7  Sincronizeaza npc_world_bindings catre metadata WorldAdmin; ruleaza dryrun inainte de apply");
        plugin.getMessageUtils().send(sender, "&e/ainpc repair batch <batchKey> [dryrun|apply|inspect|mark-steps|mark-failed]");
        plugin.getMessageUtils().send(sender, "&7  Inspecteaza sau ruleaza rollback controlat pentru un spawn batch esuat");
        plugin.getMessageUtils().send(sender, "&e/ainpc info [nume]");
        plugin.getMessageUtils().send(sender, "&7  Afiseaza informatii despre un NPC");
        plugin.getMessageUtils().send(sender, "&e/ainpc gui [quest|story|world|stats|interact|routine|shop|manager|audit|debug] [questFilter]");
        plugin.getMessageUtils().send(sender, "&7  Deschide hub-ul GUI sau un ecran specific; questFilter poate fi quest/contract/duty/bounty/event/tutorial/ritual");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest <numeNpc> [jucator]");
        plugin.getMessageUtils().send(sender, "&7  Declanseaza manual quest-ul unui NPC");
        plugin.getMessageUtils().send(sender, "&e/ainpc progression log [jucator] [quest|contract|duty|bounty|event|active|all]");
        plugin.getMessageUtils().send(sender, "&7  Listeaza progresii generice peste questuri, contracte, sarcini, bounty-uri si evenimente");
        plugin.getMessageUtils().send(sender, "&e/ainpc contract log [jucator]");
        plugin.getMessageUtils().send(sender, "&7  Listeaza contractele locale prin runtime-ul comun");
        plugin.getMessageUtils().send(sender, "&e/ainpc duty log [jucator]");
        plugin.getMessageUtils().send(sender, "&7  Listeaza sarcinile NPC prin runtime-ul comun");
        plugin.getMessageUtils().send(sender, "&e/ainpc bounty log [jucator]");
        plugin.getMessageUtils().send(sender, "&7  Listeaza bounty-urile locale prin runtime-ul comun");
        plugin.getMessageUtils().send(sender, "&e/ainpc event log [jucator]");
        plugin.getMessageUtils().send(sender, "&7  Listeaza evenimentele locale prin runtime-ul comun");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest track [start|stop] [questCode|templateId] [jucator]");
        plugin.getMessageUtils().send(sender, "&7  Arata sau mentine busola/actionbar/particule catre tinta questului");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest nearest [jucator]");
        plugin.getMessageUtils().send(sender, "&7  Declanseaza quest-ul celui mai apropiat NPC");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest reset <numeNpc> [jucator]");
        plugin.getMessageUtils().send(sender, "&7  Reseteaza progresul quest-ului pentru un jucator");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest complete <numeNpc> [jucator]");
        plugin.getMessageUtils().send(sender, "&7  Marcheaza manual quest-ul ca finalizat si da recompensa");
        plugin.getMessageUtils().send(sender, "&e/ainpc quest anchors [jucator|uuid|all] [templateId|questCode]");
        plugin.getMessageUtils().send(sender, "&7  Listeaza ancorele semantice persistate pentru questuri");
        plugin.getMessageUtils().send(sender, "&e/ainpc demo <definition|status|next|script|phases|evidence|runbook|smoke|summary|commands|restart|experimental|experimental5|experimental25|experimental25deep|experimental25ops> [regionId] [player]");
        plugin.getMessageUtils().send(sender, "&7  Explica, verifica si ghideaza primul demo intern jucabil; modurile experimental sunt instabile");
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
        plugin.getMessageUtils().send(sender, "&7  Creeaza mapping demo la pozitia ta; consola/RCON foloseste spawn-ul lumii");
        plugin.getMessageUtils().send(sender, "&e/ainpc world bind npc <numeNpc|nearest> <homePlaceId> [workPlaceId|-] [socialPlaceId|-]");
        plugin.getMessageUtils().send(sender, "&7  Leaga un NPC la home/work/social places din mapping");
        plugin.getMessageUtils().send(sender, "&e/ainpc world bindings [list|npc|place] ...");
        plugin.getMessageUtils().send(sender, "&7  Inspecteaza read-only npc_world_bindings");
        plugin.getMessageUtils().send(sender, "&e/ainpc world household <plan|spawn> <homePlaceId> [count]");
        plugin.getMessageUtils().send(sender, "&7  Genereaza sau executa un household spawn plan din mapping");
        plugin.getMessageUtils().send(sender, "&e/ainpc world household <status|place|resident|list> ...");
        plugin.getMessageUtils().send(sender, "&7  Inspecteaza read-only household-uri persistente si rezidenti");
        plugin.getMessageUtils().send(sender, "&e/ainpc world settlement <plan|spawn> <regionId> [maxHouses]");
        plugin.getMessageUtils().send(sender, "&7  Genereaza sau executa household-uri pentru casele din regiune");
        plugin.getMessageUtils().send(sender, "&e/ainpc world save");
        plugin.getMessageUtils().send(sender, "&7  Salveaza modificarile runtime in config.yml");
        plugin.getMessageUtils().send(sender, "&e/ainpc patch <analyze|plan|validate> <regionId> [targetPopulation] [profesiiCSV]");
        plugin.getMessageUtils().send(sender, "&7  Produce gap report si patch plan read-only pentru completarea satului");
        plugin.getMessageUtils().send(sender, "&e/ainpc wand [mode|pos1|pos2|point|status|inspect|clear|reset]");
        plugin.getMessageUtils().send(sender, "&7  Selecteaza geometrie sau puncte pentru mapping manual asistat");
        plugin.getMessageUtils().send(sender, "&e/ainpc map <region|place|node> <descriere>");
        plugin.getMessageUtils().send(sender, "&7  Creeaza draft mapping cu preview si confirmare inainte de scriere");
        plugin.getMessageUtils().send(sender, "&e/ainpc story context [jucator] [numeNpc|nearest]");
        plugin.getMessageUtils().send(sender, "&7  Afiseaza contextul narativ curent din mapping si quest anchors");
        plugin.getMessageUtils().send(sender, "&e/ainpc story region <regionId>");
        plugin.getMessageUtils().send(sender, "&7  Afiseaza story state-ul persistent pentru o regiune");
        plugin.getMessageUtils().send(sender, "&e/ainpc story place <placeId>");
        plugin.getMessageUtils().send(sender, "&7  Afiseaza story state-ul persistent pentru un place");
        plugin.getMessageUtils().send(sender, "&e/ainpc story events <regionId|placeId> [limit]");
        plugin.getMessageUtils().send(sender, "&7  Listeaza evenimente story persistente");
        plugin.getMessageUtils().send(sender, "&e/ainpc migration households <dryrun|apply> [limit]");
        plugin.getMessageUtils().send(sender, "&7  Backfill controlat din npc_world_bindings catre household-uri persistente");
        plugin.getMessageUtils().send(sender, "&e/ainpc audit [all|npc|world|db|spawn|quest]");
        plugin.getMessageUtils().send(sender, "&7  Verifica probleme ascunse in NPC-uri, mapping si baza de date");
        plugin.getMessageUtils().send(sender, "&e/ainpc debugdump [all|npc|world|quest|story|openai]");
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

    private String formatDistance(Location current, Location target) {
        if (current == null || target == null || current.getWorld() == null || target.getWorld() == null) {
            return "necunoscuta";
        }
        if (!current.getWorld().equals(target.getWorld())) {
            return "alta lume";
        }
        return String.format("%.1f blocuri", Math.sqrt(current.distanceSquared(target)));
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
