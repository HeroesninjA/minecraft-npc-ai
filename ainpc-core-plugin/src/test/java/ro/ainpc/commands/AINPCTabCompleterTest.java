package ro.ainpc.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AINPCTabCompleterTest {

    @Test
    void completesProgressionGuiAliasesWithRelativeFilters() {
        AINPCTabCompleter completer = new AINPCTabCompleter(null);

        List<String> subcommandCompletions = completer.onTabComplete(
            null,
            new TestCommand("ainpc"),
            "ainpc",
            new String[] {"contract", "gui", ""}
        );
        assertTrue(subcommandCompletions.contains("active"));
        assertTrue(subcommandCompletions.contains("tracked"));
        assertTrue(subcommandCompletions.contains("archived"));
        assertFalse(subcommandCompletions.contains("quest"));

        List<String> directCompletions = completer.onTabComplete(
            null,
            new TestCommand("contract"),
            "contract",
            new String[] {"gui", ""}
        );
        assertTrue(directCompletions.contains("current"));
        assertTrue(directCompletions.contains("failed"));
        assertFalse(directCompletions.contains("ritual"));
    }

    @Test
    void completesGlobalProgressionGuiWithKindFilters() {
        AINPCTabCompleter completer = new AINPCTabCompleter(null);

        List<String> completions = completer.onTabComplete(
            null,
            new TestCommand("ainpc"),
            "ainpc",
            new String[] {"gui", "progresii", ""}
        );
        assertTrue(completions.contains("contract"));
        assertTrue(completions.contains("ritual"));
        assertTrue(completions.contains("active"));

        List<String> directProgressionCompletions = completer.onTabComplete(
            null,
            new TestCommand("progression"),
            "progression",
            new String[] {"gui", ""}
        );
        assertTrue(directProgressionCompletions.contains("contract"));
        assertTrue(directProgressionCompletions.contains("ritual"));
        assertTrue(directProgressionCompletions.contains("active"));
    }

    @Test
    void completesQuestAnchorsWithTemplateOrCodeHint() {
        AINPCTabCompleter completer = new AINPCTabCompleter(null);

        List<String> completions = completer.onTabComplete(
            null,
            new TestCommand("ainpc"),
            "ainpc",
            new String[] {"quest", "anchors", "all", ""}
        );

        assertTrue(completions.contains("<templateId|questCode>"));
    }

    @Test
    void completesMappingWandAndDraftCommands() {
        AINPCTabCompleter completer = new AINPCTabCompleter(null);

        List<String> modes = completer.onTabComplete(
            null,
            new TestCommand("ainpc"),
            "ainpc",
            new String[] {"wand", "mode", ""}
        );
        assertTrue(modes.contains("region"));
        assertTrue(modes.contains("node"));
        assertTrue(modes.contains("quest_anchor"));

        List<String> mapActions = completer.onTabComplete(
            null,
            new TestCommand("ainpc"),
            "ainpc",
            new String[] {"map", ""}
        );
        assertTrue(mapActions.contains("region"));
        assertTrue(mapActions.contains("place"));
        assertTrue(mapActions.contains("npc_bind"));
        assertTrue(mapActions.contains("quest_anchor"));
        assertTrue(mapActions.contains("preview"));
        assertTrue(mapActions.contains("confirm"));
    }

    @Test
    void completesHouseholdInspectionActions() {
        AINPCTabCompleter completer = new AINPCTabCompleter(null);

        List<String> completions = completer.onTabComplete(
            null,
            new TestCommand("ainpc"),
            "ainpc",
            new String[] {"world", "household", ""}
        );

        assertTrue(completions.contains("status"));
        assertTrue(completions.contains("place"));
        assertTrue(completions.contains("resident"));
        assertTrue(completions.contains("list"));
    }

    @Test
    void completesMigrationHouseholdActions() {
        AINPCTabCompleter completer = new AINPCTabCompleter(null);

        List<String> targets = completer.onTabComplete(
            null,
            new TestCommand("ainpc"),
            "ainpc",
            new String[] {"migration", ""}
        );
        assertTrue(targets.contains("households"));

        List<String> modes = completer.onTabComplete(
            null,
            new TestCommand("ainpc"),
            "ainpc",
            new String[] {"migration", "households", ""}
        );
        assertTrue(modes.contains("dryrun"));
        assertTrue(modes.contains("apply"));
    }

    @Test
    void completesHouseholdRepairActions() {
        AINPCTabCompleter completer = new AINPCTabCompleter(null);

        List<String> targets = completer.onTabComplete(
            null,
            new TestCommand("ainpc"),
            "ainpc",
            new String[] {"repair", ""}
        );
        assertTrue(targets.contains("households"));
        assertTrue(targets.contains("batch"));

        List<String> modes = completer.onTabComplete(
            null,
            new TestCommand("ainpc"),
            "ainpc",
            new String[] {"repair", "households", ""}
        );
        assertTrue(modes.contains("dryrun"));
        assertTrue(modes.contains("apply"));

        List<String> batchModes = completer.onTabComplete(
            null,
            new TestCommand("ainpc"),
            "ainpc",
            new String[] {"repair", "batch", "settlement:sat:abc", ""}
        );
        assertTrue(batchModes.contains("dryrun"));
        assertTrue(batchModes.contains("apply"));
        assertTrue(batchModes.contains("inspect"));
        assertTrue(batchModes.contains("mark-steps"));
        assertTrue(batchModes.contains("mark-failed"));

        List<String> batchKeys = completer.onTabComplete(
            null,
            new TestCommand("ainpc"),
            "ainpc",
            new String[] {"repair", "batch", ""}
        );
        assertTrue(batchKeys.contains("list"));

        List<String> batchListFilters = completer.onTabComplete(
            null,
            new TestCommand("ainpc"),
            "ainpc",
            new String[] {"repair", "batch", "list", ""}
        );
        assertTrue(batchListFilters.contains("problem"));
        assertTrue(batchListFilters.contains("failed"));
        assertTrue(batchListFilters.contains("rolled_back"));
    }

    private static final class TestCommand extends Command {
        private TestCommand(String name) {
            super(name);
        }

        @Override
        public boolean execute(CommandSender sender, String commandLabel, String[] args) {
            return true;
        }
    }
}
