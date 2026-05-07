package ro.ainpc.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ro.ainpc.AINPCPlugin;
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
        "create", "delete", "info", "gui", "quest", "progression", "contract", "world", "story", "audit", "debugdump", "list", "family", "routine", "mood", "tp", "reload", "test"
    );
    private static final List<String> GUI_MODES = Arrays.asList(
        "main", "quest", "world", "stats", "interact", "shop", "manager", "audit", "debug"
    );
    private static final List<String> AUDIT_MODES = Arrays.asList("all", "npc", "world", "db", "spawn", "quest");
    private static final List<String> DEBUG_DUMP_SCOPES = Arrays.asList("all", "npc", "world", "quest", "openai");
    private static final List<String> ROUTINE_ACTIONS = Arrays.asList("tick", "status");
    private static final List<String> QUEST_MODES = Arrays.asList(
        "gui", "log", "track", "current", "nearest", "accept", "decline", "da", "nu", "ok", "refuz",
        "abandon", "status", "progress", "progres", "reset", "complete", "anchors"
    );
    private static final List<String> QUEST_DECISION_MODES = Arrays.asList(
        "accept", "decline", "yes", "y", "da", "ok", "confirm", "deny", "reject", "no", "n", "nu", "refuz"
    );
    private static final List<String> QUEST_LOG_FILTERS = Arrays.asList(
        "active", "current", "tracked", "quest", "contract", "main", "side", "repeatable",
        "completed", "failed", "archived", "all"
    );
    private static final List<String> WORLD_MODES = Arrays.asList("whereami", "places", "region", "place", "node", "scan", "demo", "bind", "household", "settlement", "save");
    private static final List<String> STORY_MODES = Arrays.asList("context", "region", "place", "events");
    private static final List<String> REGION_ACTIONS = Arrays.asList("info", "create");
    private static final List<String> PLACE_ACTIONS = Arrays.asList("info", "create");
    private static final List<String> NODE_ACTIONS = Arrays.asList("create");
    private static final List<String> SCAN_TARGETS = Arrays.asList("village");
    private static final List<String> DEMO_ACTIONS = Arrays.asList("create");
    private static final List<String> BIND_TARGETS = Arrays.asList("npc");
    private static final List<String> HOUSEHOLD_ACTIONS = Arrays.asList("plan", "spawn");
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
            || "contracts".equalsIgnoreCase(command.getName())) {
            return completeQuestArgs(args);
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
                case "quest", "progression", "progress", "contract", "contracts" -> {
                    completions.addAll(completeQuestArgs(sliceQuestArgs(args)));
                }
                case "gui" -> {
                    if (args.length == 2) {
                        completions.addAll(filterStartsWith(GUI_MODES, args[1]));
                    }
                }
                case "world" -> {
                    completions.addAll(completeWorldArgs(sender, args));
                }
                case "story" -> {
                    completions.addAll(completeStoryArgs(args));
                }
                case "audit" -> {
                    if (args.length == 2) {
                        completions.addAll(filterStartsWith(AUDIT_MODES, args[1]));
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
            case "household" -> {
                if (args.length == 3) {
                    completions.addAll(filterStartsWith(HOUSEHOLD_ACTIONS, args[2]));
                } else if (args.length == 4 && HOUSEHOLD_ACTIONS.stream().anyMatch(args[2]::equalsIgnoreCase)) {
                    completions.addAll(getPlaceIds(args[3]));
                } else if (args.length == 5 && HOUSEHOLD_ACTIONS.stream().anyMatch(args[2]::equalsIgnoreCase)) {
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
                    completions.add("<templateId>");
                } else if (questMode.equals("log")) {
                    completions.addAll(filterStartsWith(QUEST_LOG_FILTERS, questArgs[2]));
                    completions.addAll(getOnlinePlayerNames(questArgs[2]));
                } else if ((questMode.equals("track") || questMode.equals("current"))
                    && (questArgs[1].equalsIgnoreCase("start") || questArgs[1].equalsIgnoreCase("stop"))) {
                    completions.addAll(getOnlinePlayerNames(questArgs[2]));
                }
            }
        }

        return completions;
    }

    private String[] sliceQuestArgs(String[] args) {
        if (args.length <= 1) {
            return new String[0];
        }

        String[] sliced = new String[args.length - 1];
        System.arraycopy(args, 1, sliced, 0, sliced.length);
        return sliced;
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
