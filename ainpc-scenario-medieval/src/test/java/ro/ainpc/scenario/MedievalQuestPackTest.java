package ro.ainpc.scenario;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MedievalQuestPackTest {

    @Test
    void medievalQuestPackDefinesPlayableQuestSet() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(
            new File("src/main/resources/packs/medieval_quest.yml")
        );

        ConfigurationSection scenarios = config.getConfigurationSection("scenarios");
        assertNotNull(scenarios, "scenarios section should exist");
        assertEquals(Set.of("Q01", "Q02", "Q03", "Q04", "Q05"), scenarios.getKeys(false));

        Map<String, String> expectedGivers = Map.of(
            "Q01", "blacksmith",
            "Q02", "farmer",
            "Q03", "guard",
            "Q04", "innkeeper",
            "Q05", "healer"
        );
        Map<String, String> expectedKinds = Map.of(
            "Q01", "fetch",
            "Q02", "fetch",
            "Q03", "hunt",
            "Q04", "fetch",
            "Q05", "fetch"
        );
        for (Map.Entry<String, String> entry : expectedGivers.entrySet()) {
            ConfigurationSection scenario = scenarios.getConfigurationSection(entry.getKey());
            assertNotNull(scenario, "scenario " + entry.getKey() + " should exist");
            assertEquals("QUEST", scenario.getString("base_type"));
            assertEquals(entry.getValue(), scenario.getString("quest.giver_profession"));
            assertEquals(expectedKinds.get(entry.getKey()), scenario.getString("quest.kind"));
            assertEquals("explicit", scenario.getString("quest.acceptance_mode"));
            assertEquals("return_to_giver", scenario.getString("quest.completion_mode"));
            assertEquals("next_objective", scenario.getString("quest.tracking_mode"));
            assertTrue(!scenario.getStringList("quest.tags").isEmpty(), entry.getKey() + " should define quest tags");
            assertTrue(scenario.getBoolean("requires_player"), entry.getKey() + " should require a player");

            ConfigurationSection phases = scenario.getConfigurationSection("phases");
            assertNotNull(phases, entry.getKey() + " should define phases");
            assertTrue(phases.contains("INTRODUCTION"));
            assertTrue(phases.contains("ACCEPTANCE"));
            assertTrue(phases.contains("RETURN"));
            assertTrue(phases.contains("COMPLETION"));

            ConfigurationSection objectives = scenario.getConfigurationSection("quest.objectives");
            assertNotNull(objectives, entry.getKey() + " should define objectives");
            assertTrue(objectives.getKeys(false).size() >= 1);
            validateObjectives(objectives);

            ConfigurationSection rewards = scenario.getConfigurationSection("quest.rewards");
            assertNotNull(rewards, entry.getKey() + " should define rewards");
            validateItemEntries(rewards);

            ConfigurationSection questDialogues = scenario.getConfigurationSection("quest.dialogues");
            assertNotNull(questDialogues, entry.getKey() + " should define quest dialogues");
            assertDialogueLines(questDialogues, "offer");
            assertDialogueLines(questDialogues, "offered");
            assertDialogueLines(questDialogues, "accepted");
            assertDialogueLines(questDialogues, "active");
            assertDialogueLines(questDialogues, "ready");
            assertDialogueLines(questDialogues, "completed");
        }

        assertEquals(List.of("Q01"), scenarios.getStringList("Q03.quest.prerequisites"));
        assertTrue(scenarios.getBoolean("Q04.quest.repeatable"));
        assertEquals(1800, scenarios.getInt("Q04.quest.cooldown_seconds"));
        assertEquals(List.of("Q02"), scenarios.getStringList("Q05.quest.prerequisites"));
    }

    private void validateObjectives(ConfigurationSection objectives) {
        for (String key : objectives.getKeys(false)) {
            ConfigurationSection objective = objectives.getConfigurationSection(key);
            assertNotNull(objective, "objective " + key + " should be a section");

            String type = objective.getString("type", "");
            String item = objective.getString("item", "");
            assertTrue(objective.getInt("amount", 0) > 0, "objective " + key + " should have positive amount");
            if ("kill_mob".equalsIgnoreCase(type)) {
                assertNotNull(EntityType.valueOf(item), "objective " + key + " should target a valid entity type");
            } else {
                assertNotNull(Material.matchMaterial(item), "objective " + key + " should use a valid material");
            }
        }
    }

    private void validateItemEntries(ConfigurationSection entries) {
        for (String key : entries.getKeys(false)) {
            ConfigurationSection entry = entries.getConfigurationSection(key);
            assertNotNull(entry, "reward " + key + " should be a section");
            assertTrue(entry.getInt("amount", 0) > 0, "reward " + key + " should have positive amount");
            assertNotNull(Material.matchMaterial(entry.getString("item", "")), "reward " + key + " should use a valid material");
        }
    }

    private void assertDialogueLines(ConfigurationSection dialogues, String key) {
        List<String> lines = dialogues.getStringList(key);
        assertTrue(!lines.isEmpty(), "dialogue key " + key + " should define at least one line");
    }
}
