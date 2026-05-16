package ro.ainpc.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ro.ainpc.AINPCPlugin;
import ro.ainpc.gui.GuiKey;
import ro.ainpc.npc.AINPC;
import ro.ainpc.world.PlaceType;
import ro.ainpc.world.RegionType;
import ro.ainpc.world.WorldPlaceInfo;
import ro.ainpc.world.WorldRegionInfo;
import ro.ainpc.world.WorldNodeType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tab completer pentru comenzile NPC
 */
public class AINPCTabCompleter implements TabCompleter {

    private final AINPCPlugin plugin;
    
    private static final List<String> SUBCOMMANDS = Arrays.asList(
        "create", "delete", "delete-id", "duplicates", "repair", "info", "gui", "quest", "progression", "contract", "duty", "bounty", "event", "tutorial", "ritual", "world", "patch", "wand", "map", "story", "migration", "audit", "debugdump", "list", "family", "routine", "mood", "tp", "reload", "test"
    );
    private static final List<String> GUI_MODES = Arrays.asList(
        "main", "quest", "progresii", "progression", "story", "poveste", "world", "stats", "interact", "routine", "shop", "manager", "audit", "debug"
    );
    private static final List<String> AUDIT_MODES = Arrays.asList("all", "npc", "world", "db", "spawn", "quest", "wand");
    private static final List<String> AUDIT_QUEST_OPTIONS = Arrays.asList("strict", "full", "offline");
    private static final List<String> DEBUG_DUMP_SCOPES = Arrays.asList("all", "npc", "world", "quest", "story", "openai");
    private static final List<String> MIGRATION_TARGETS = Arrays.asList("households");
    private static final List<String> MIGRATION_MODES = Arrays.asList("dryrun", "apply");
    private static final List<String> REPAIR_TARGETS = Arrays.asList(
        "duplicates", "households", "npc-bindings", "mapping-metadata", "batch", "spawn-batch"
    );
    private static final List<String> REPAIR_MODES = Arrays.asList("dryrun", "apply");
    private static final List<String> REPAIR_BATCH_MODES = Arrays.asList(
        "dryrun", "apply", "inspect", "steps", "mark-steps", "sync-steps", "mark-failed"
    );
    private static final List<String> REPAIR_BATCH_KEYS = Arrays.asList("list", "recent", "<batchKey>");
    private static final List<String> REPAIR_BATCH_LIST_FILTERS = Arrays.asList(
        "problem", "all", "failed", "running", "rolled_back", "succeeded"
    );
    private static final List<String> ROUTINE_ACTIONS = Arrays.asList("tick", "status");
    private static final List<String> QUEST_MODES = Arrays.asList(
        "gui", "log", "track", "current", "nearest", "accept", "decline", "da", "nu", "ok", "refuz",
        "abandon", "status", "progress", "progres", "reset", "complete", "anchors", "definitions", "defs",
        "stored", "state", "progressions"
    );
    private static final List<String> QUEST_DECISION_MODES = Arrays.asList(
        "accept", "decline", "yes", "y", "da", "ok", "confirm", "deny", "reject", "no", "n", "nu", "refuz"
    );
    private static final List<String> QUEST_LOG_FILTERS = Arrays.asList(
        "active", "current", "tracked", "quest", "contract", "duty", "bounty", "event", "tutorial", "ritual", "main", "side", "repeatable",
        "completed", "failed", "archived", "all"
    );
    private static final List<String> GUI_QUEST_FILTERS = Arrays.asList(
        "all", "active", "quest", "contract", "duty", "bounty", "event", "tutorial", "ritual"
    );
    private static final List<String> GUI_ALIAS_FILTERS = Arrays.asList(
        "all", "active", "current", "tracked", "offered", "completed", "failed", "archived"
    );
    private static final List<String> PROGRESSION_STORED_FILTERS = Arrays.asList(
        "all", "active", "current", "tracked", "offered", "completed", "failed", "archived",
        "quest", "contract", "duty", "bounty", "event", "tutorial", "ritual", "investigation", "main_quests", "side_quests", "village_contracts", "npc_duties", "local_bounties", "village_events", "onboarding", "village_rituals", "unresolved",
        "kind:contract", "scenario:investigation", "base:TRADE_DEAL", "category:side", "mechanic:village_contracts",
        "kind:duty", "scenario:duty", "base:DUTY", "mechanic:npc_duties",
        "kind:bounty", "scenario:hunt", "base:BOUNTY", "mechanic:local_bounties",
        "kind:event", "scenario:event", "base:WORLD_EVENT", "mechanic:village_events",
        "kind:tutorial", "scenario:tutorial", "base:TUTORIAL", "mechanic:onboarding",
        "kind:ritual", "scenario:ritual", "base:RITUAL", "mechanic:village_rituals",
        "status:active", "tracked:true", "resolved:false"
    );
    private static final List<String> PROGRESSION_DEFINITION_FILTERS = Arrays.asList(
        "all", "quest", "contract", "duty", "bounty", "event", "tutorial", "ritual", "investigation", "main_quests", "side_quests", "village_contracts", "npc_duties", "local_bounties", "village_events", "onboarding", "village_rituals",
        "kind:contract", "scenario:investigation", "base:TRADE_DEAL", "category:side", "mechanic:village_contracts",
        "kind:duty", "scenario:duty", "base:DUTY", "mechanic:npc_duties",
        "kind:bounty", "scenario:hunt", "base:BOUNTY", "mechanic:local_bounties",
        "kind:event", "scenario:event", "base:WORLD_EVENT", "mechanic:village_events",
        "kind:tutorial", "scenario:tutorial", "base:TUTORIAL", "mechanic:onboarding",
        "kind:ritual", "scenario:ritual", "base:RITUAL", "mechanic:village_rituals"
    );
    private static final List<String> WORLD_MODES = Arrays.asList("whereami", "places", "region", "place", "node", "scan", "demo", "bind", "bindings", "household", "settlement", "save");
    private static final List<String> PATCH_ACTIONS = Arrays.asList("analyze", "plan", "validate");
    private static final List<String> PATCH_POPULATION_SUGGESTIONS = Arrays.asList("4", "6", "8", "10", "12");
    private static final List<String> PATCH_PROFESSION_SUGGESTIONS = Arrays.asList(
        "blacksmith", "farmer", "merchant", "innkeeper", "guard", "priest",
        "blacksmith,farmer", "blacksmith,farmer,merchant"
    );
    private static final List<String> WAND_ACTIONS = Arrays.asList(
        "mode", "pos1", "pos2", "point", "status", "inspect", "clear", "reset"
    );
    private static final List<String> WAND_RESET_TARGETS = Arrays.asList("pos1", "pos2", "point", "all");
    private static final List<String> WAND_MODES = Arrays.asList("region", "place", "node", "npc_bind", "quest_anchor");
    private static final List<String> MAP_ACTIONS = Arrays.asList("region", "place", "node", "npc_bind", "quest_anchor", "preview", "confirm", "cancel");
    private static final List<String> MAP_QUEST_ANCHOR_SELECTORS = Arrays.asList("tracked", "current");
    private static final List<String> MAP_QUEST_ANCHOR_OBJECTIVE_TYPES = Arrays.asList(
        "visit_place", "inspect_node", "talk_to_npc", "deliver_to_npc", "visit_region", "kill_mob"
    );
    private static final List<String> STORY_MODES = Arrays.asList("context", "region", "place", "events");
    private static final List<String> REGION_ACTIONS = Arrays.asList("info", "create");
    private static final List<String> PLACE_ACTIONS = Arrays.asList("info", "create");
    private static final List<String> NODE_ACTIONS = Arrays.asList("create");
    private static final List<String> SCAN_TARGETS = Arrays.asList("village");
    private static final List<String> DEMO_ACTIONS = Arrays.asList("create");
    private static final List<String> BIND_TARGETS = Arrays.asList("npc");
    private static final List<String> BINDINGS_ACTIONS = Arrays.asList("list", "npc", "place");
    private static final List<String> HOUSEHOLD_ACTIONS = Arrays.asList("plan", "spawn", "status", "place", "resident", "list");
    private static final List<String> HOUSEHOLD_PLAN_ACTIONS = Arrays.asList("plan", "spawn");
    private static final List<String> SETTLEMENT_ACTIONS = Arrays.asList("plan", "spawn");
    private static final List<String> REGION_TYPES = Arrays.stream(RegionType.values())
        .map(RegionType::getId)
        .sorted()
        .toList();
    private static final List<String> PLACE_TYPES = Arrays.stream(PlaceType.values())
        .map(PlaceType::getId)
        .sorted()
        .toList();
    private static final List<String> NODE_TYPES = Arrays.stream(WorldNodeType.values())
        .map(WorldNodeType::getId)
        .sorted()
        .toList();
    
    private static final List<String> OCCUPATIONS = Arrays.asList(
        "fermier", "fierar", "pescar", "negustor", "miner", "tamplar",
        "soldat", "paznic", "brutar", "croitor", "alchimist", "bibliotecar",
        "preot", "cartograf", "macelar"
    );
    
    private static final List<String> GENDERS = Arrays.asList("male", "female");
    
    private static final List<String> ARCHETYPES = Arrays.asList(
        "hero", "villain", "sage", "jester", "caregiver", "explorer",
        "rebel", "lover", "creator", "ruler", "magician", "innocent",
        "orphan", "warrior", "merchant"
    );
    
    private static final List<String> EMOTIONS = Arrays.asList(
        "happiness", "sadness", "anger", "fear", "surprise", "disgust", "trust", "anticipation"
    );

    public AINPCTabCompleter(AINPCPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if ("npcquest".equalsIgnoreCase(command.getName())
            || "quest".equalsIgnoreCase(command.getName())
            || "progression".equalsIgnoreCase(command.getName())
            || "progress".equalsIgnoreCase(command.getName())
            || "contract".equalsIgnoreCase(command.getName())
            || "contracts".equalsIgnoreCase(command.getName())
            || "duty".equalsIgnoreCase(command.getName())
            || "duties".equalsIgnoreCase(command.getName())
            || "sarcina".equalsIgnoreCase(command.getName())
            || "sarcini".equalsIgnoreCase(command.getName())
            || "bounty".equalsIgnoreCase(command.getName())
            || "bounties".equalsIgnoreCase(command.getName())
            || "event".equalsIgnoreCase(command.getName())
            || "events".equalsIgnoreCase(command.getName())
            || "tutorial".equalsIgnoreCase(command.getName())
            || "tutorials".equalsIgnoreCase(command.getName())
            || "onboarding".equalsIgnoreCase(command.getName())
            || "ritual".equalsIgnoreCase(command.getName())
            || "rituals".equalsIgnoreCase(command.getName())
            || "ceremony".equalsIgnoreCase(command.getName())
            || "ceremonies".equalsIgnoreCase(command.getName())) {
            return isProgressionAliasCommand(command.getName())
                ? completeProgressionAliasArgs(args)
                : completeQuestArgs(args);
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Primul argument - subcomanda
            completions.addAll(filterStartsWith(SUBCOMMANDS, args[0]));
        } else if (args.length >= 2) {
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "create" -> {
                    switch (args.length) {
                        case 2 -> completions.add("<nume>");
                        case 3 -> completions.addAll(filterStartsWith(OCCUPATIONS, args[2]));
                        case 4 -> completions.addAll(Arrays.asList("20", "25", "30", "40", "50", "60"));
                        case 5 -> completions.addAll(filterStartsWith(GENDERS, args[4]));
                        case 6 -> completions.addAll(filterStartsWith(ARCHETYPES, args[5]));
                    }
                }
                case "delete", "info", "family", "tp" -> {
                    if (args.length == 2) {
                        completions.addAll(getNPCNames(args[1]));
                    }
                }
                case "delete-id" -> {
                    if (args.length == 2) {
                        completions.addAll(getNPCIds(args[1]));
                    } else if (args.length == 3) {
                        completions.addAll(filterStartsWith(List.of("confirm"), args[2]));
                    }
                }
                case "repair" -> {
                    if (args.length == 2) {
                        completions.addAll(filterStartsWith(REPAIR_TARGETS, args[1]));
                    } else if (args.length == 3
                        && ("duplicates".equalsIgnoreCase(args[1])
                        || "households".equalsIgnoreCase(args[1])
                        || "npc-bindings".equalsIgnoreCase(args[1])
                        || "npc_bindings".equalsIgnoreCase(args[1])
                        || "mapping-metadata".equalsIgnoreCase(args[1])
                        || "mapping_metadata".equalsIgnoreCase(args[1]))) {
                        completions.addAll(filterStartsWith(REPAIR_MODES, args[2]));
                    } else if (args.length == 3
                        && isRepairBatchTarget(args[1])) {
                        completions.addAll(filterStartsWith(REPAIR_BATCH_KEYS, args[2]));
                    } else if (args.length == 4
                        && isRepairBatchTarget(args[1])) {
                        if ("list".equalsIgnoreCase(args[2]) || "recent".equalsIgnoreCase(args[2])) {
                            completions.addAll(filterStartsWith(REPAIR_BATCH_LIST_FILTERS, args[3]));
                        } else {
                            completions.addAll(filterStartsWith(REPAIR_BATCH_MODES, args[3]));
                        }
                    }
                }
                case "quest", "progression", "progress", "contract", "contracts", "duty", "duties", "sarcina", "sarcini", "bounty", "bounties", "event", "events", "eveniment", "evenimente", "tutorial", "tutorials", "onboarding", "ritual", "rituals", "ceremony", "ceremonies", "ceremonie", "ceremonii" -> {
                    String[] questArgs = sliceQuestArgs(args);
                    completions.addAll(isProgressionAliasCommand(subCommand)
                        ? completeProgressionAliasArgs(questArgs)
                        : completeQuestArgs(questArgs));
                }
                case "gui" -> {
                    if (args.length == 2) {
                        completions.addAll(filterStartsWith(GUI_MODES, args[1]));
                    } else if (args.length == 3 && isProgressionGuiMode(args[1])) {
                        completions.addAll(filterStartsWith(GUI_QUEST_FILTERS, args[2]));
                    }
                }
                case "world" -> {
                    completions.addAll(completeWorldArgs(sender, args));
                }
                case "patch" -> {
                    completions.addAll(completePatchArgs(args));
                }
                case "wand" -> {
                    if (args.length == 2) {
                        completions.addAll(filterStartsWith(WAND_ACTIONS, args[1]));
                    } else if (args.length == 3 && "mode".equalsIgnoreCase(args[1])) {
                        completions.addAll(filterStartsWith(WAND_MODES, args[2]));
                    } else if (args.length == 3
                        && ("clear".equalsIgnoreCase(args[1]) || "reset".equalsIgnoreCase(args[1]))) {
                        completions.addAll(filterStartsWith(WAND_RESET_TARGETS, args[2]));
                    }
                }
                case "map" -> {
                    if (args.length == 2) {
                        completions.addAll(filterStartsWith(MAP_ACTIONS, args[1]));
                    } else if (args.length >= 3 && "quest_anchor".equalsIgnoreCase(args[1])) {
                        completions.addAll(completeMapQuestAnchorArgs(sender, args));
                    }
                }
                case "story" -> {
                    completions.addAll(completeStoryArgs(args));
                }
                case "migration" -> {
                    if (args.length == 2) {
                        completions.addAll(filterStartsWith(MIGRATION_TARGETS, args[1]));
                    } else if (args.length == 3 && "households".equalsIgnoreCase(args[1])) {
                        completions.addAll(filterStartsWith(MIGRATION_MODES, args[2]));
                    } else if (args.length == 4 && "households".equalsIgnoreCase(args[1])) {
                        completions.addAll(filterStartsWith(Arrays.asList("100", "500", "1000"), args[3]));
                    }
                }
                case "audit" -> {
                    if (args.length == 2) {
                        completions.addAll(filterStartsWith(AUDIT_MODES, args[1]));
                    } else if (args.length == 3
                        && ("quest".equalsIgnoreCase(args[1]) || "all".equalsIgnoreCase(args[1]))) {
                        completions.addAll(filterStartsWith(AUDIT_QUEST_OPTIONS, args[2]));
                    }
                }
                case "debugdump" -> {
                    if (args.length == 2) {
                        completions.addAll(filterStartsWith(DEBUG_DUMP_SCOPES, args[1]));
                    }
                }
                case "routine" -> {
                    if (args.length == 2) {
                        completions.addAll(filterStartsWith(ROUTINE_ACTIONS, args[1]));
                    } else if (args.length == 3 && "status".equalsIgnoreCase(args[1])) {
                        completions.addAll(getNPCNames(args[2]));
                    }
                }
                case "mood", "emotion" -> {
                    switch (args.length) {
                        case 2 -> completions.addAll(getNPCNames(args[1]));
                        case 3 -> completions.addAll(filterStartsWith(EMOTIONS, args[2]));
                        case 4 -> completions.addAll(Arrays.asList("0.3", "0.5", "0.7", "1.0"));
                    }
                }
            }
        }

        return completions;
    }

    private boolean isRepairBatchTarget(String value) {
        return "batch".equalsIgnoreCase(value)
            || "spawn-batch".equalsIgnoreCase(value)
            || "spawn_batch".equalsIgnoreCase(value);
    }

    private List<String> completeStoryArgs(String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 2) {
            completions.addAll(filterStartsWith(STORY_MODES, args[1]));
        } else if (args.length == 3 && "context".equalsIgnoreCase(args[1])) {
            completions.addAll(getOnlinePlayerNames(args[2]));
            completions.addAll(filterStartsWith(List.of("nearest"), args[2]));
            completions.addAll(getNPCNames(args[2]));
        } else if (args.length == 4 && "context".equalsIgnoreCase(args[1])) {
            completions.addAll(filterStartsWith(List.of("nearest"), args[3]));
            completions.addAll(getNPCNames(args[3]));
        } else if (args.length == 3 && "region".equalsIgnoreCase(args[1])) {
            completions.addAll(getRegionIds(args[2]));
        } else if (args.length == 3 && "place".equalsIgnoreCase(args[1])) {
            completions.addAll(getPlaceIds(args[2]));
        } else if (args.length == 3 && "events".equalsIgnoreCase(args[1])) {
            completions.addAll(getRegionIds(args[2]));
            completions.addAll(getPlaceIds(args[2]));
        } else if (args.length == 4 && "events".equalsIgnoreCase(args[1])) {
            completions.addAll(filterStartsWith(List.of("5", "10", "20", "50"), args[3]));
        }
        return completions;
    }

    private List<String> completeWorldArgs(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 2) {
            completions.addAll(filterStartsWith(WORLD_MODES, args[1]));
            return completions;
        }

        String worldMode = args[1].toLowerCase();
        switch (worldMode) {
            case "whereami" -> {
                if (args.length == 3) {
                    completions.addAll(getOnlinePlayerNames(args[2]));
                }
            }
            case "places" -> {
                if (args.length == 3) {
                    completions.addAll(getRegionIds(args[2]));
                }
            }
            case "region" -> {
                if (args.length == 3) {
                    completions.addAll(filterStartsWith(REGION_ACTIONS, args[2]));
                } else if (args.length == 4) {
                    if ("info".equalsIgnoreCase(args[2])) {
                        completions.addAll(getRegionIds(args[3]));
                    } else if ("create".equalsIgnoreCase(args[2])) {
                        completions.add("<id>");
                    }
                } else if (args.length == 5 && "create".equalsIgnoreCase(args[2])) {
                    completions.addAll(filterStartsWith(REGION_TYPES, args[4]));
                } else if (args.length >= 6 && args.length <= 11 && "create".equalsIgnoreCase(args[2])) {
                    completions.addAll(getCoordinateSuggestions(sender, args[args.length - 1]));
                }
            }
            case "place" -> {
                if (args.length == 3) {
                    completions.addAll(filterStartsWith(PLACE_ACTIONS, args[2]));
                } else if (args.length == 4) {
                    if ("info".equalsIgnoreCase(args[2])) {
                        completions.addAll(getPlaceIds(args[3]));
                    } else if ("create".equalsIgnoreCase(args[2])) {
                        completions.addAll(getRegionIds(args[3]));
                    }
                } else if (args.length == 5 && "create".equalsIgnoreCase(args[2])) {
                    completions.add("<id>");
                } else if (args.length == 6 && "create".equalsIgnoreCase(args[2])) {
                    completions.addAll(filterStartsWith(PLACE_TYPES, args[5]));
                } else if (args.length >= 7 && args.length <= 12 && "create".equalsIgnoreCase(args[2])) {
                    completions.addAll(getCoordinateSuggestions(sender, args[args.length - 1]));
                }
            }
            case "node" -> {
                if (args.length == 3) {
                    completions.addAll(filterStartsWith(NODE_ACTIONS, args[2]));
                } else if (args.length == 4 && "create".equalsIgnoreCase(args[2])) {
                    completions.addAll(getRegionIds(args[3]));
                } else if (args.length == 5 && "create".equalsIgnoreCase(args[2])) {
                    completions.add("-");
                    completions.addAll(getPlaceIdsForRegion(args[3], args[4]));
                } else if (args.length == 6 && "create".equalsIgnoreCase(args[2])) {
                    completions.add("<id>");
                } else if (args.length == 7 && "create".equalsIgnoreCase(args[2])) {
                    completions.addAll(filterStartsWith(NODE_TYPES, args[6]));
                } else if (args.length >= 8 && args.length <= 10 && "create".equalsIgnoreCase(args[2])) {
                    completions.addAll(getCoordinateSuggestions(sender, args[args.length - 1]));
                } else if (args.length == 11 && "create".equalsIgnoreCase(args[2])) {
                    completions.addAll(filterStartsWith(Arrays.asList("1.5", "2.5", "4.0", "6.0"), args[10]));
                }
            }
            case "scan" -> {
                if (args.length == 3) {
                    completions.addAll(filterStartsWith(SCAN_TARGETS, args[2]));
                } else if (args.length == 4 && "village".equalsIgnoreCase(args[2])) {
                    completions.addAll(filterStartsWith(Arrays.asList("32", "48", "64", "80"), args[3]));
                } else if (args.length == 5 && "village".equalsIgnoreCase(args[2])) {
                    completions.addAll(filterStartsWith(Arrays.asList("import"), args[4]));
                } else if (args.length == 6 && "village".equalsIgnoreCase(args[2])
                    && "import".equalsIgnoreCase(args[4])) {
                    completions.add("<regionId>");
                }
            }
            case "demo" -> {
                if (args.length == 3) {
                    completions.addAll(filterStartsWith(DEMO_ACTIONS, args[2]));
                } else if (args.length == 4 && "create".equalsIgnoreCase(args[2])) {
                    completions.add("<regionId>");
                }
            }
            case "bind" -> {
                if (args.length == 3) {
                    completions.addAll(filterStartsWith(BIND_TARGETS, args[2]));
                } else if (args.length == 4 && "npc".equalsIgnoreCase(args[2])) {
                    completions.addAll(filterStartsWith(List.of("nearest"), args[3]));
                    completions.addAll(getNPCNames(args[3]));
                } else if (args.length == 5 && "npc".equalsIgnoreCase(args[2])) {
                    completions.addAll(getPlaceIds(args[4]));
                } else if ((args.length == 6 || args.length == 7) && "npc".equalsIgnoreCase(args[2])) {
                    completions.addAll(filterStartsWith(List.of("-"), args[args.length - 1]));
                    completions.addAll(getPlaceIds(args[args.length - 1]));
                }
            }
            case "binding", "bindings" -> {
                if (args.length == 3) {
                    completions.addAll(filterStartsWith(BINDINGS_ACTIONS, args[2]));
                    completions.addAll(filterStartsWith(Arrays.asList("10", "20", "50"), args[2]));
                } else if (args.length == 4 && "npc".equalsIgnoreCase(args[2])) {
                    completions.addAll(filterStartsWith(List.of("nearest"), args[3]));
                    completions.addAll(getNPCNames(args[3]));
                } else if (args.length == 4 && "place".equalsIgnoreCase(args[2])) {
                    completions.addAll(getPlaceIds(args[3]));
                } else if ((args.length == 4 && "list".equalsIgnoreCase(args[2]))
                    || (args.length == 5 && "place".equalsIgnoreCase(args[2]))) {
                    completions.addAll(filterStartsWith(Arrays.asList("10", "20", "50"), args[args.length - 1]));
                }
            }
            case "household" -> {
                if (args.length == 3) {
                    completions.addAll(filterStartsWith(HOUSEHOLD_ACTIONS, args[2]));
                } else if (args.length == 4 && HOUSEHOLD_PLAN_ACTIONS.stream().anyMatch(args[2]::equalsIgnoreCase)) {
                    completions.addAll(getPlaceIds(args[3]));
                } else if (args.length == 4 && ("status".equalsIgnoreCase(args[2]) || "place".equalsIgnoreCase(args[2]))) {
                    completions.addAll(getPlaceIds(args[3]));
                } else if (args.length == 4 && "resident".equalsIgnoreCase(args[2])) {
                    completions.addAll(filterStartsWith(List.of("nearest"), args[3]));
                    completions.addAll(getNPCNames(args[3]));
                } else if (args.length == 4 && "list".equalsIgnoreCase(args[2])) {
                    completions.addAll(filterStartsWith(Arrays.asList("10", "20", "50"), args[3]));
                } else if (args.length == 5 && HOUSEHOLD_PLAN_ACTIONS.stream().anyMatch(args[2]::equalsIgnoreCase)) {
                    completions.addAll(filterStartsWith(Arrays.asList("1", "2", "3", "4"), args[4]));
                }
            }
            case "settlement" -> {
                if (args.length == 3) {
                    completions.addAll(filterStartsWith(SETTLEMENT_ACTIONS, args[2]));
                } else if (args.length == 4 && SETTLEMENT_ACTIONS.stream().anyMatch(args[2]::equalsIgnoreCase)) {
                    completions.addAll(getRegionIds(args[3]));
                } else if (args.length == 5 && SETTLEMENT_ACTIONS.stream().anyMatch(args[2]::equalsIgnoreCase)) {
                    completions.addAll(filterStartsWith(Arrays.asList("1", "2", "4", "8"), args[4]));
                }
            }
        }

        return completions;
    }

    private List<String> completePatchArgs(String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 2) {
            completions.addAll(filterStartsWith(PATCH_ACTIONS, args[1]));
        } else if (args.length == 3 && PATCH_ACTIONS.stream().anyMatch(args[1]::equalsIgnoreCase)) {
            completions.addAll(getRegionIdsSafe(args[2]));
        } else if (args.length == 4 && PATCH_ACTIONS.stream().anyMatch(args[1]::equalsIgnoreCase)) {
            completions.addAll(filterStartsWith(PATCH_POPULATION_SUGGESTIONS, args[3]));
        } else if (args.length == 5 && PATCH_ACTIONS.stream().anyMatch(args[1]::equalsIgnoreCase)) {
            completions.addAll(filterStartsWith(PATCH_PROFESSION_SUGGESTIONS, args[4]));
        }
        return completions;
    }

    private List<String> completeQuestArgs(String[] questArgs) {
        List<String> completions = new ArrayList<>();
        if (questArgs.length == 0) {
            return completions;
        }

        switch (questArgs.length) {
            case 1 -> {
                completions.addAll(filterStartsWith(QUEST_MODES, questArgs[0]));
                completions.addAll(getNPCNames(questArgs[0]));
            }
            case 2 -> {
                String questMode = questArgs[0].toLowerCase();
                if (questMode.equals("reset") || questMode.equals("complete")
                    || QUEST_DECISION_MODES.contains(questMode)
                    || questMode.equals("abandon") || questMode.equals("status")) {
                    completions.addAll(filterStartsWith(List.of("nearest"), questArgs[1]));
                    completions.addAll(getNPCNames(questArgs[1]));
                    if (QUEST_DECISION_MODES.contains(questMode)) {
                        completions.addAll(getOnlinePlayerNames(questArgs[1]));
                    }
                } else if (questMode.equals("progress") || questMode.equals("progres")) {
                    completions.addAll(filterStartsWith(List.of("tracked", "current"), questArgs[1]));
                    completions.addAll(getOnlinePlayerNames(questArgs[1]));
                } else if (questMode.equals("anchors")) {
                    completions.addAll(filterStartsWith(List.of("all"), questArgs[1]));
                    completions.addAll(getOnlinePlayerNames(questArgs[1]));
                } else if (questMode.equals("log")) {
                    completions.addAll(filterStartsWith(QUEST_LOG_FILTERS, questArgs[1]));
                    completions.addAll(getOnlinePlayerNames(questArgs[1]));
                } else if (questMode.equals("gui")) {
                    completions.addAll(filterStartsWith(GUI_QUEST_FILTERS, questArgs[1]));
                } else if (questMode.equals("definitions") || questMode.equals("defs")) {
                    completions.addAll(filterStartsWith(PROGRESSION_DEFINITION_FILTERS, questArgs[1]));
                } else if (questMode.equals("stored") || questMode.equals("state") || questMode.equals("progressions")) {
                    completions.addAll(filterStartsWith(List.of("all"), questArgs[1]));
                    completions.addAll(getOnlinePlayerNames(questArgs[1]));
                    completions.addAll(filterStartsWith(PROGRESSION_STORED_FILTERS, questArgs[1]));
                } else if (questMode.equals("track") || questMode.equals("current")) {
                    completions.addAll(filterStartsWith(List.of("start", "stop"), questArgs[1]));
                    completions.addAll(getOnlinePlayerNames(questArgs[1]));
                } else {
                    completions.addAll(getOnlinePlayerNames(questArgs[1]));
                }
            }
            case 3 -> {
                String questMode = questArgs[0].toLowerCase();
                if (questMode.equals("reset") || questMode.equals("complete")
                    || QUEST_DECISION_MODES.contains(questMode)
                    || questMode.equals("abandon") || questMode.equals("status")) {
                    completions.addAll(getOnlinePlayerNames(questArgs[2]));
                } else if (questMode.equals("progress") || questMode.equals("progres")) {
                    completions.addAll(getOnlinePlayerNames(questArgs[2]));
                } else if (questMode.equals("anchors")) {
                    completions.add("<templateId|questCode>");
                } else if (questMode.equals("log")) {
                    completions.addAll(filterStartsWith(QUEST_LOG_FILTERS, questArgs[2]));
                    completions.addAll(getOnlinePlayerNames(questArgs[2]));
                } else if (questMode.equals("stored") || questMode.equals("state") || questMode.equals("progressions")) {
                    completions.addAll(filterStartsWith(PROGRESSION_STORED_FILTERS, questArgs[2]));
                    completions.addAll(filterStartsWith(Arrays.asList("10", "20", "50"), questArgs[2]));
                } else if ((questMode.equals("track") || questMode.equals("current"))
                    && (questArgs[1].equalsIgnoreCase("start") || questArgs[1].equalsIgnoreCase("stop"))) {
                    completions.addAll(getOnlinePlayerNames(questArgs[2]));
                }
            }
        }

        return completions;
    }

    private List<String> completeProgressionAliasArgs(String[] questArgs) {
        if (questArgs.length == 2
            && ("gui".equalsIgnoreCase(questArgs[0]) || "log".equalsIgnoreCase(questArgs[0]))) {
            return filterStartsWith(GUI_ALIAS_FILTERS, questArgs[1]);
        }
        return completeQuestArgs(questArgs);
    }

    private String[] sliceQuestArgs(String[] args) {
        if (args.length <= 1) {
            return new String[0];
        }

        String[] sliced = new String[args.length - 1];
        System.arraycopy(args, 1, sliced, 0, sliced.length);
        return sliced;
    }

    private boolean isProgressionGuiMode(String rawValue) {
        return GuiKey.fromId(rawValue)
            .map(key -> key == GuiKey.QUEST)
            .orElse(false);
    }

    private boolean isProgressionAliasCommand(String rawValue) {
        return switch (rawValue == null ? "" : rawValue.toLowerCase()) {
            case "contract", "contracts",
                 "duty", "duties", "sarcina", "sarcini",
                 "bounty", "bounties",
                 "event", "events", "eveniment", "evenimente",
                 "tutorial", "tutorials", "onboarding",
                 "ritual", "rituals", "ceremony", "ceremonies", "ceremonie", "ceremonii" -> true;
            default -> false;
        };
    }

    private List<String> completeMapQuestAnchorArgs(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 3) {
            completions.addAll(filterStartsWith(MAP_QUEST_ANCHOR_SELECTORS, args[2]));
            completions.addAll(getQuestAnchorPlayerSelectors(args[2]));
            completions.addAll(getProgressionSelectors(args[2]));
            return completions;
        }

        boolean hasPlayerSelector = isQuestAnchorPlayerSelector(args[2]);
        if (hasPlayerSelector) {
            if (args.length == 4) {
                completions.addAll(filterStartsWith(MAP_QUEST_ANCHOR_SELECTORS, args[3]));
                completions.addAll(getProgressionSelectors(args[3]));
            } else if (args.length == 5) {
                completions.addAll(getQuestAnchorObjectiveIds(sender, args[2], args[3], args[4]));
            } else if (args.length == 6) {
                completions.addAll(filterStartsWith(MAP_QUEST_ANCHOR_OBJECTIVE_TYPES, args[5]));
            }
            return completions;
        }

        if (args.length == 4) {
            completions.addAll(getQuestAnchorObjectiveIds(sender, "", args[2], args[3]));
        } else if (args.length == 5) {
            completions.addAll(filterStartsWith(MAP_QUEST_ANCHOR_OBJECTIVE_TYPES, args[4]));
        }
        return completions;
    }

    private List<String> getQuestAnchorPlayerSelectors(String prefix) {
        List<String> selectors = new ArrayList<>(List.of("player:self"));
        if (plugin != null) {
            selectors.addAll(plugin.getServer().getOnlinePlayers().stream()
                .map(player -> "player:" + player.getName())
                .toList());
        }
        return filterStartsWith(selectors, prefix);
    }

    private boolean isQuestAnchorPlayerSelector(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase();
        return normalized.startsWith("player:") || normalized.startsWith("jucator:");
    }

    private List<String> getQuestAnchorObjectiveIds(CommandSender sender,
                                                    String playerSelector,
                                                    String progressionSelector,
                                                    String prefix) {
        if (plugin == null || plugin.getProgressionService() == null) {
            return filterStartsWith(List.of("<objective_id>"), prefix);
        }

        Player targetPlayer = resolveQuestAnchorPlayer(sender, playerSelector);
        if (targetPlayer == null) {
            return filterStartsWith(List.of("<objective_id>"), prefix);
        }

        List<String> suggestions = plugin.getProgressionService()
            .getObjectiveIdSuggestions(targetPlayer, progressionSelector);
        if (suggestions.isEmpty()) {
            return filterStartsWith(List.of("<objective_id>"), prefix);
        }
        return filterStartsWith(suggestions, prefix);
    }

    private Player resolveQuestAnchorPlayer(CommandSender sender, String playerSelector) {
        String safeSelector = playerSelector == null ? "" : playerSelector.trim();
        if (safeSelector.isBlank()
            || "player:self".equalsIgnoreCase(safeSelector)
            || "jucator:self".equalsIgnoreCase(safeSelector)
            || "self".equalsIgnoreCase(safeSelector)) {
            return sender instanceof Player player ? player : null;
        }
        if (plugin == null) {
            return null;
        }

        String playerName = safeSelector;
        int separator = playerName.indexOf(':');
        if (separator >= 0 && separator < playerName.length() - 1) {
            playerName = playerName.substring(separator + 1);
        }
        Player exact = plugin.getServer().getPlayerExact(playerName);
        return exact != null ? exact : plugin.getServer().getPlayer(playerName);
    }

    private List<String> getProgressionSelectors(String prefix) {
        if (plugin == null || plugin.getProgressionService() == null) {
            return List.of();
        }

        List<String> selectors = plugin.getProgressionService().getDefinitions().stream()
            .flatMap(definition -> Arrays.asList(
                definition.templateId(),
                definition.code(),
                definition.progressionId(),
                definition.mechanicId() + ":" + definition.definitionId()
            ).stream())
            .filter(value -> value != null && !value.isBlank())
            .distinct()
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
        return filterStartsWith(selectors, prefix);
    }

    /**
     * Obtine numele tuturor NPC-urilor care incep cu prefixul dat
     */
    private List<String> getNPCNames(String prefix) {
        return plugin.getNpcManager().getAllNPCs().stream()
            .map(AINPC::getName)
            .filter(name -> name.toLowerCase().startsWith(prefix.toLowerCase()))
            .collect(Collectors.toList());
    }

    private List<String> getNPCIds(String prefix) {
        return plugin.getNpcManager().getAllNPCs().stream()
            .map(npc -> String.valueOf(npc.getDatabaseId()))
            .filter(id -> id.startsWith(prefix))
            .sorted()
            .collect(Collectors.toList());
    }

    private List<String> getOnlinePlayerNames(String prefix) {
        return plugin.getServer().getOnlinePlayers().stream()
            .map(sender -> sender.getName())
            .filter(name -> name.toLowerCase().startsWith(prefix.toLowerCase()))
            .collect(Collectors.toList());
    }

    private List<String> getRegionIds(String prefix) {
        return plugin.getPlatform().getWorldAdmin().getRegions().stream()
            .map(WorldRegionInfo::id)
            .filter(id -> id.toLowerCase().startsWith(prefix.toLowerCase()))
            .sorted()
            .collect(Collectors.toList());
    }

    private List<String> getRegionIdsSafe(String prefix) {
        if (plugin == null || plugin.getPlatform() == null || plugin.getPlatform().getWorldAdmin() == null) {
            return List.of("<regionId>");
        }
        return getRegionIds(prefix);
    }

    private List<String> getPlaceIds(String prefix) {
        return plugin.getPlatform().getWorldAdmin().getPlaces().stream()
            .map(WorldPlaceInfo::id)
            .filter(id -> id.toLowerCase().startsWith(prefix.toLowerCase()))
            .sorted()
            .collect(Collectors.toList());
    }

    private List<String> getPlaceIdsForRegion(String regionSelector, String prefix) {
        return plugin.getPlatform().getWorldAdmin().getPlaces().stream()
            .filter(place -> place.regionId().equalsIgnoreCase(regionSelector)
                || place.id().toLowerCase().startsWith(regionSelector.toLowerCase() + ":"))
            .map(WorldPlaceInfo::id)
            .filter(id -> id.toLowerCase().startsWith(prefix.toLowerCase()))
            .sorted()
            .collect(Collectors.toList());
    }

    private List<String> getCoordinateSuggestions(CommandSender sender, String prefix) {
        if (!(sender instanceof Player player)) {
            return List.of();
        }

        List<String> values = List.of(
            String.valueOf(player.getLocation().getBlockX()),
            String.valueOf(player.getLocation().getBlockY()),
            String.valueOf(player.getLocation().getBlockZ())
        );
        return filterStartsWith(values, prefix);
    }

    /**
     * Filtreaza lista pentru a returna doar elementele care incep cu prefixul dat
     */
    private List<String> filterStartsWith(List<String> list, String prefix) {
        return list.stream()
            .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
            .collect(Collectors.toList());
    }
}
