package ro.ainpc.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import ro.ainpc.AINPCPlugin;
import ro.ainpc.npc.AINPC;

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
        "create", "delete", "info", "quest", "list", "family", "mood", "tp", "reload", "test"
    );
    private static final List<String> QUEST_MODES = Arrays.asList("nearest", "reset", "complete");
    
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
        if ("npcquest".equalsIgnoreCase(command.getName())) {
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
                case "quest" -> {
                    completions.addAll(completeQuestArgs(sliceQuestArgs(args)));
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
                if (questMode.equals("reset") || questMode.equals("complete")) {
                    completions.addAll(getNPCNames(questArgs[1]));
                } else {
                    completions.addAll(getOnlinePlayerNames(questArgs[1]));
                }
            }
            case 3 -> {
                String questMode = questArgs[0].toLowerCase();
                if (questMode.equals("reset") || questMode.equals("complete")) {
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

    /**
     * Filtreaza lista pentru a returna doar elementele care incep cu prefixul dat
     */
    private List<String> filterStartsWith(List<String> list, String prefix) {
        return list.stream()
            .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
            .collect(Collectors.toList());
    }
}
