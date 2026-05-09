package ro.ainpc.scenario;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MedievalQuestPackTest {

    @Test
    void medievalQuestPackDefinesPlayableQuestSet() {
        YamlConfiguration config = loadMedievalQuestPack();

        ConfigurationSection scenarios = config.getConfigurationSection("scenarios");
        assertNotNull(scenarios, "scenarios section should exist");
        assertEquals(Set.of("Q01", "Q02", "Q03", "Q04", "Q05", "Q06", "Q07", "Q08", "C01", "C02", "D01", "B01", "B02", "E01", "T01", "R01"), scenarios.getKeys(false));

        ConfigurationSection mechanics = config.getConfigurationSection("mechanics");
        assertNotNull(mechanics, "mechanics section should exist");
        assertEquals(Set.of("main_quests", "side_quests", "village_contracts", "npc_duties", "local_bounties", "village_events", "onboarding", "village_rituals"), mechanics.getKeys(false));
        assertEquals(1, mechanics.getInt("main_quests.max_active"));
        assertEquals(3, mechanics.getInt("side_quests.max_active"));
        assertEquals(3, mechanics.getInt("village_contracts.max_active"));
        assertEquals("contract", mechanics.getString("village_contracts.kind"));
        assertEquals(2, mechanics.getInt("npc_duties.max_active"));
        assertEquals("duty", mechanics.getString("npc_duties.kind"));
        assertEquals(1, mechanics.getInt("local_bounties.max_active"));
        assertEquals("bounty", mechanics.getString("local_bounties.kind"));
        assertEquals(2, mechanics.getInt("village_events.max_active"));
        assertEquals("event", mechanics.getString("village_events.kind"));
        assertEquals(1, mechanics.getInt("onboarding.max_active"));
        assertEquals("tutorial", mechanics.getString("onboarding.kind"));
        assertEquals(1, mechanics.getInt("village_rituals.max_active"));
        assertEquals("ritual", mechanics.getString("village_rituals.kind"));

        Map<String, String> expectedGivers = Map.ofEntries(
            Map.entry("Q01", "blacksmith"),
            Map.entry("Q02", "farmer"),
            Map.entry("Q03", "guard"),
            Map.entry("Q04", "innkeeper"),
            Map.entry("Q05", "healer"),
            Map.entry("Q06", "blacksmith"),
            Map.entry("Q07", "innkeeper"),
            Map.entry("Q08", "guard"),
            Map.entry("C01", "merchant"),
            Map.entry("C02", "merchant"),
            Map.entry("D01", "guard"),
            Map.entry("B01", "guard"),
            Map.entry("B02", "farmer"),
            Map.entry("E01", "guard"),
            Map.entry("T01", "priest"),
            Map.entry("R01", "priest")
        );
        Map<String, String> expectedBaseTypes = Map.ofEntries(
            Map.entry("Q01", "QUEST"),
            Map.entry("Q02", "QUEST"),
            Map.entry("Q03", "QUEST"),
            Map.entry("Q04", "QUEST"),
            Map.entry("Q05", "QUEST"),
            Map.entry("Q06", "QUEST"),
            Map.entry("Q07", "QUEST"),
            Map.entry("Q08", "QUEST"),
            Map.entry("C01", "TRADE_DEAL"),
            Map.entry("C02", "TRADE_DEAL"),
            Map.entry("D01", "DUTY"),
            Map.entry("B01", "BOUNTY"),
            Map.entry("B02", "BOUNTY"),
            Map.entry("E01", "WORLD_EVENT"),
            Map.entry("T01", "TUTORIAL"),
            Map.entry("R01", "RITUAL")
        );
        Map<String, String> expectedMechanics = Map.ofEntries(
            Map.entry("Q01", "main_quests"),
            Map.entry("Q02", "side_quests"),
            Map.entry("Q03", "side_quests"),
            Map.entry("Q04", "side_quests"),
            Map.entry("Q05", "side_quests"),
            Map.entry("Q06", "side_quests"),
            Map.entry("Q07", "side_quests"),
            Map.entry("Q08", "side_quests"),
            Map.entry("C01", "village_contracts"),
            Map.entry("C02", "village_contracts"),
            Map.entry("D01", "npc_duties"),
            Map.entry("B01", "local_bounties"),
            Map.entry("B02", "local_bounties"),
            Map.entry("E01", "village_events"),
            Map.entry("T01", "onboarding"),
            Map.entry("R01", "village_rituals")
        );
        Map<String, String> expectedKinds = Map.ofEntries(
            Map.entry("Q01", "fetch"),
            Map.entry("Q02", "fetch"),
            Map.entry("Q03", "hunt"),
            Map.entry("Q04", "fetch"),
            Map.entry("Q05", "fetch"),
            Map.entry("Q06", "exploration"),
            Map.entry("Q07", "delivery"),
            Map.entry("Q08", "hunt"),
            Map.entry("C01", "delivery"),
            Map.entry("C02", "investigation"),
            Map.entry("D01", "duty"),
            Map.entry("B01", "hunt"),
            Map.entry("B02", "hunt"),
            Map.entry("E01", "event"),
            Map.entry("T01", "tutorial"),
            Map.entry("R01", "ritual")
        );
        Map<String, String> expectedCategories = Map.ofEntries(
            Map.entry("Q01", "main"),
            Map.entry("Q02", "side"),
            Map.entry("Q03", "side"),
            Map.entry("Q04", "repeatable"),
            Map.entry("Q05", "side"),
            Map.entry("Q06", "side"),
            Map.entry("Q07", "side"),
            Map.entry("Q08", "side"),
            Map.entry("C01", "side"),
            Map.entry("C02", "side"),
            Map.entry("D01", "repeatable"),
            Map.entry("B01", "repeatable"),
            Map.entry("B02", "repeatable"),
            Map.entry("E01", "repeatable"),
            Map.entry("T01", "side"),
            Map.entry("R01", "repeatable")
        );
        for (Map.Entry<String, String> entry : expectedGivers.entrySet()) {
            ConfigurationSection scenario = scenarios.getConfigurationSection(entry.getKey());
            assertNotNull(scenario, "scenario " + entry.getKey() + " should exist");
            assertEquals(expectedBaseTypes.get(entry.getKey()), scenario.getString("base_type"));
            assertEquals(expectedMechanics.get(entry.getKey()), scenario.getString("mechanic"));
            assertEquals(entry.getValue(), scenario.getString("quest.giver_profession"));
            assertEquals(expectedKinds.get(entry.getKey()), scenario.getString("quest.kind"));
            assertEquals(expectedCategories.get(entry.getKey()), scenario.getString("quest.category"));
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
            validateObjectivePhases(entry.getKey(), phases, objectives);
            validateQuestStages(entry.getKey(), phases, objectives, scenario.getConfigurationSection("quest.stages"));

            ConfigurationSection rewards = scenario.getConfigurationSection("quest.rewards");
            assertNotNull(rewards, entry.getKey() + " should define rewards");
            validateRewards(rewards);

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
        assertTrue(scenarios.getBoolean("C01.quest.repeatable"));
        assertEquals(1200, scenarios.getInt("C01.quest.cooldown_seconds"));
        assertTrue(scenarios.getBoolean("C01.progress.enabled"));
        assertEquals("contract", scenarios.getString("C01.progress.kind"));
        assertEquals("village_contracts", scenarios.getString("C01.progress.mechanic"));
        assertTrue(scenarios.getBoolean("C02.quest.repeatable"));
        assertEquals(1800, scenarios.getInt("C02.quest.cooldown_seconds"));
        assertTrue(scenarios.getBoolean("C02.progress.enabled"));
        assertEquals("contract", scenarios.getString("C02.progress.kind"));
        assertEquals("village_contracts", scenarios.getString("C02.progress.mechanic"));
        assertTrue(scenarios.getBoolean("D01.quest.repeatable"));
        assertEquals(900, scenarios.getInt("D01.quest.cooldown_seconds"));
        assertTrue(scenarios.getBoolean("D01.progress.enabled"));
        assertEquals("duty", scenarios.getString("D01.progress.kind"));
        assertEquals("npc_duties", scenarios.getString("D01.progress.mechanic"));
        assertTrue(scenarios.getBoolean("B01.quest.repeatable"));
        assertEquals(1800, scenarios.getInt("B01.quest.cooldown_seconds"));
        assertTrue(scenarios.getBoolean("B01.progress.enabled"));
        assertEquals("bounty", scenarios.getString("B01.progress.kind"));
        assertEquals("local_bounties", scenarios.getString("B01.progress.mechanic"));
        assertTrue(scenarios.getBoolean("B02.quest.repeatable"));
        assertEquals(2700, scenarios.getInt("B02.quest.cooldown_seconds"));
        assertTrue(scenarios.getBoolean("B02.progress.enabled"));
        assertEquals("bounty", scenarios.getString("B02.progress.kind"));
        assertEquals("local_bounties", scenarios.getString("B02.progress.mechanic"));
        assertTrue(scenarios.getBoolean("E01.quest.repeatable"));
        assertEquals(2400, scenarios.getInt("E01.quest.cooldown_seconds"));
        assertTrue(scenarios.getBoolean("E01.progress.enabled"));
        assertEquals("event", scenarios.getString("E01.progress.kind"));
        assertEquals("village_events", scenarios.getString("E01.progress.mechanic"));
        assertFalse(scenarios.getBoolean("T01.quest.repeatable"));
        assertTrue(scenarios.getBoolean("T01.progress.enabled"));
        assertEquals("tutorial", scenarios.getString("T01.progress.kind"));
        assertEquals("onboarding", scenarios.getString("T01.progress.mechanic"));
        assertTrue(scenarios.getBoolean("R01.quest.repeatable"));
        assertEquals(3600, scenarios.getInt("R01.quest.cooldown_seconds"));
        assertTrue(scenarios.getBoolean("R01.progress.enabled"));
        assertEquals("ritual", scenarios.getString("R01.progress.kind"));
        assertEquals("village_rituals", scenarios.getString("R01.progress.mechanic"));
        assertEquals(List.of("Q02"), scenarios.getStringList("Q05.quest.prerequisites"));
        assertEquals(List.of("Q01"), scenarios.getStringList("Q06.quest.prerequisites"));
        assertEquals(List.of("Q03", "Q04"), scenarios.getStringList("Q07.quest.prerequisites"));
        assertEquals(List.of("Q03"), scenarios.getStringList("Q08.quest.prerequisites"));
        assertObjectivePhases(scenarios, "Q06", Map.of(
            "visit_forge", "INVESTIGATION",
            "inspect_forge_marks", "INVESTIGATION",
            "report_to_blacksmith", "RETURN"
        ));
        assertObjectivePhases(scenarios, "Q07", Map.of(
            "prepare_notice", "CONTACT",
            "speak_with_guard", "CONTACT",
            "deliver_rations", "RETURN"
        ));
        assertObjectivePhases(scenarios, "Q08", Map.of(
            "patrol_region", "PATROL",
            "clear_zombies", "PATROL",
            "report_to_guard", "RETURN"
        ));
        assertObjectivePhases(scenarios, "C02", Map.of(
            "visit_market", "MARKET_CHECK",
            "check_market_board", "MARKET_CHECK",
            "deliver_market_note", "RETURN"
        ));
        assertObjectivePhases(scenarios, "D01", Map.of(
            "check_settlement", "PATROL",
            "check_guard_board", "PATROL",
            "report_guard_duty", "RETURN"
        ));
        assertObjectivePhases(scenarios, "B01", Map.of(
            "patrol_old_road", "HUNT",
            "clear_skeletons", "HUNT",
            "report_bounty", "RETURN"
        ));
        assertObjectivePhases(scenarios, "B02", Map.of(
            "visit_farm_edge", "HUNT",
            "clear_farm_spiders", "HUNT",
            "report_farm_bounty", "RETURN"
        ));
        assertObjectivePhases(scenarios, "E01", Map.of(
            "visit_event_market", "CHECK",
            "inspect_event_board", "CHECK",
            "deliver_repair_stone", "RETURN"
        ));
        assertObjectivePhases(scenarios, "T01", Map.of(
            "visit_tutorial_market", "LEARN",
            "inspect_tutorial_board", "LEARN",
            "report_tutorial", "RETURN"
        ));
        assertObjectivePhases(scenarios, "R01", Map.of(
            "collect_ritual_candles", "PREPARE",
            "visit_ritual_altar", "CEREMONY",
            "inspect_ritual_circle", "CEREMONY",
            "deliver_ritual_candles", "RETURN"
        ));
        assertQuestStages(scenarios, "Q06", Map.of(
            "INVESTIGATION", List.of("visit_forge", "inspect_forge_marks"),
            "RETURN", List.of("report_to_blacksmith")
        ), Map.of(
            "INVESTIGATION", "all_objectives",
            "RETURN", "manual_turn_in"
        ), Map.of(
            "INVESTIGATION", "RETURN"
        ));
        assertQuestStages(scenarios, "Q07", Map.of(
            "CONTACT", List.of("prepare_notice", "speak_with_guard"),
            "RETURN", List.of("deliver_rations")
        ), Map.of(
            "CONTACT", "all_objectives",
            "RETURN", "manual_turn_in"
        ), Map.of(
            "CONTACT", "RETURN"
        ));
        assertQuestStages(scenarios, "Q08", Map.of(
            "PATROL", List.of("patrol_region", "clear_zombies"),
            "RETURN", List.of("report_to_guard")
        ), Map.of(
            "PATROL", "all_objectives",
            "RETURN", "manual_turn_in"
        ), Map.of(
            "PATROL", "RETURN"
        ));
        assertQuestStages(scenarios, "C02", Map.of(
            "MARKET_CHECK", List.of("visit_market", "check_market_board"),
            "RETURN", List.of("deliver_market_note")
        ), Map.of(
            "MARKET_CHECK", "all_objectives",
            "RETURN", "manual_turn_in"
        ), Map.of(
            "MARKET_CHECK", "RETURN"
        ));
        assertQuestStages(scenarios, "D01", Map.of(
            "PATROL", List.of("check_settlement", "check_guard_board"),
            "RETURN", List.of("report_guard_duty")
        ), Map.of(
            "PATROL", "all_objectives",
            "RETURN", "manual_turn_in"
        ), Map.of(
            "PATROL", "RETURN"
        ));
        assertQuestStages(scenarios, "B01", Map.of(
            "HUNT", List.of("patrol_old_road", "clear_skeletons"),
            "RETURN", List.of("report_bounty")
        ), Map.of(
            "HUNT", "all_objectives",
            "RETURN", "manual_turn_in"
        ), Map.of(
            "HUNT", "RETURN"
        ));
        assertQuestStages(scenarios, "B02", Map.of(
            "HUNT", List.of("visit_farm_edge", "clear_farm_spiders"),
            "RETURN", List.of("report_farm_bounty")
        ), Map.of(
            "HUNT", "all_objectives",
            "RETURN", "manual_turn_in"
        ), Map.of(
            "HUNT", "RETURN"
        ));
        assertQuestStages(scenarios, "E01", Map.of(
            "CHECK", List.of("visit_event_market", "inspect_event_board"),
            "RETURN", List.of("deliver_repair_stone")
        ), Map.of(
            "CHECK", "all_objectives",
            "RETURN", "manual_turn_in"
        ), Map.of(
            "CHECK", "RETURN"
        ));
        assertQuestStages(scenarios, "T01", Map.of(
            "LEARN", List.of("visit_tutorial_market", "inspect_tutorial_board"),
            "RETURN", List.of("report_tutorial")
        ), Map.of(
            "LEARN", "all_objectives",
            "RETURN", "manual_turn_in"
        ), Map.of(
            "LEARN", "RETURN"
        ));
        assertQuestStages(scenarios, "R01", Map.of(
            "PREPARE", List.of("collect_ritual_candles"),
            "CEREMONY", List.of("visit_ritual_altar", "inspect_ritual_circle"),
            "RETURN", List.of("deliver_ritual_candles")
        ), Map.of(
            "PREPARE", "all_objectives",
            "CEREMONY", "all_objectives",
            "RETURN", "manual_turn_in"
        ), Map.of(
            "PREPARE", "CEREMONY",
            "CEREMONY", "RETURN"
        ));
    }

    @Test
    void medievalQuestPackHasStableRuntimeContractForStarterQuests() {
        YamlConfiguration config = loadMedievalQuestPack();

        ConfigurationSection professions = config.getConfigurationSection("professions");
        ConfigurationSection scenarios = config.getConfigurationSection("scenarios");
        assertNotNull(professions, "professions section should exist");
        assertNotNull(scenarios, "scenarios section should exist");

        Set<String> knownQuestReferences = new HashSet<>();
        for (String scenarioId : scenarios.getKeys(false)) {
            knownQuestReferences.add(normalize(scenarioId));
            knownQuestReferences.add(normalize("medieval_quest:" + scenarioId));
            knownQuestReferences.add(normalize(scenarios.getString(scenarioId + ".quest.code", "")));
        }

        Set<String> questCodes = new HashSet<>();
        for (String scenarioId : scenarios.getKeys(false)) {
            ConfigurationSection scenario = scenarios.getConfigurationSection(scenarioId);
            assertNotNull(scenario, "scenario " + scenarioId + " should exist");

            String questCode = scenario.getString("quest.code", "");
            assertTrue(!questCode.isBlank(), scenarioId + " should define quest.code");
            assertTrue(questCodes.add(normalize(questCode)), "quest.code should be unique: " + questCode);

            String giverProfession = scenario.getString("quest.giver_profession", "");
            assertTrue(!giverProfession.isBlank(), scenarioId + " should define giver profession");
            assertTrue(professions.contains(giverProfession), scenarioId + " giver profession should exist");

            ConfigurationSection giverRole = scenario.getConfigurationSection("roles.QUEST_GIVER");
            assertNotNull(giverRole, scenarioId + " should define QUEST_GIVER role");
            assertTrue(
                giverRole.getStringList("required_professions").contains(giverProfession),
                scenarioId + " QUEST_GIVER role should require the configured giver profession"
            );

            for (String prerequisite : scenario.getStringList("quest.prerequisites")) {
                assertTrue(
                    knownQuestReferences.contains(normalize(prerequisite)),
                    scenarioId + " prerequisite should reference an existing quest: " + prerequisite
                );
            }

            boolean repeatable = scenario.getBoolean("quest.repeatable", false);
            int cooldownSeconds = scenario.getInt("quest.cooldown_seconds", 0);
            if (repeatable) {
                assertTrue(cooldownSeconds > 0, scenarioId + " repeatable quests should define cooldown_seconds");
            } else {
                assertEquals(0, cooldownSeconds, scenarioId + " non-repeatable quests should not define cooldown_seconds");
            }

            ConfigurationSection dialogues = scenario.getConfigurationSection("quest.dialogues");
            assertNotNull(dialogues, scenarioId + " should define quest dialogues");
            if (repeatable || !scenario.getStringList("quest.prerequisites").isEmpty()) {
                assertDialogueLines(dialogues, "unavailable");
            }

            validateRuntimeSupportedObjectives(scenarioId, scenario.getConfigurationSection("quest.objectives"));
            validateRuntimeSupportedRewards(scenarioId, scenario.getConfigurationSection("quest.rewards"));
            validateStableEntryIds(scenarioId, "objective", scenario.getConfigurationSection("quest.objectives"));
            validateStableEntryIds(scenarioId, "reward", scenario.getConfigurationSection("quest.rewards"));
            validateObjectivePhases(
                scenarioId,
                scenario.getConfigurationSection("phases"),
                scenario.getConfigurationSection("quest.objectives")
            );
            validateQuestStages(
                scenarioId,
                scenario.getConfigurationSection("phases"),
                scenario.getConfigurationSection("quest.objectives"),
                scenario.getConfigurationSection("quest.stages")
            );
        }
    }

    private YamlConfiguration loadMedievalQuestPack() {
        return YamlConfiguration.loadConfiguration(
            new File("src/main/resources/packs/medieval_quest.yml")
        );
    }

    private void validateObjectives(ConfigurationSection objectives) {
        for (String key : objectives.getKeys(false)) {
            ConfigurationSection objective = objectives.getConfigurationSection(key);
            assertNotNull(objective, "objective " + key + " should be a section");

            String type = objective.getString("type", "");
            String item = objective.getString("item", "");
            assertTrue(objective.getInt("amount", 0) > 0, "objective " + key + " should have positive amount");
            String normalizedType = normalizeRuntimeType(type);
            if ("kill_mob".equals(normalizedType)) {
                assertNotNull(EntityType.valueOf(item), "objective " + key + " should target a valid entity type");
            } else if ("collect_item".equals(normalizedType) || "deliver_to_npc".equals(normalizedType)) {
                assertNotNull(Material.matchMaterial(item), "objective " + key + " should use a valid material");
            } else {
                assertTrue(!item.isBlank(), "semantic objective " + key + " should define a reference");
            }
        }
    }

    private void validateRuntimeSupportedObjectives(String scenarioId, ConfigurationSection objectives) {
        assertNotNull(objectives, scenarioId + " should define objectives");
        Set<String> supportedObjectiveTypes = Set.of(
            "collect_item",
            "deliver_to_npc",
            "talk_to_npc",
            "visit_region",
            "visit_place",
            "inspect_node",
            "kill_mob"
        );

        for (String key : objectives.getKeys(false)) {
            ConfigurationSection objective = objectives.getConfigurationSection(key);
            assertNotNull(objective, scenarioId + " objective " + key + " should be a section");
            String type = normalizeRuntimeType(objective.getString("type", ""));
            assertTrue(
                supportedObjectiveTypes.contains(type),
                scenarioId + " objective " + key + " should use a supported runtime type: " + type
            );
        }
    }

    private void validateObjectivePhases(String scenarioId, ConfigurationSection phases, ConfigurationSection objectives) {
        assertNotNull(phases, scenarioId + " should define phases");
        assertNotNull(objectives, scenarioId + " should define objectives");

        Set<String> knownPhases = new HashSet<>();
        for (String phase : phases.getKeys(false)) {
            knownPhases.add(normalize(phase));
        }

        for (String key : objectives.getKeys(false)) {
            ConfigurationSection objective = objectives.getConfigurationSection(key);
            assertNotNull(objective, scenarioId + " objective " + key + " should be a section");
            String phase = objective.getString("phase", "");
            if (!phase.isBlank()) {
                assertTrue(
                    knownPhases.contains(normalize(phase)),
                    scenarioId + " objective " + key + " should reference an existing phase: " + phase
                );
            }
        }
    }

    private void validateQuestStages(String scenarioId,
                                     ConfigurationSection phases,
                                     ConfigurationSection objectives,
                                     ConfigurationSection stages) {
        if (stages == null) {
            return;
        }
        assertNotNull(phases, scenarioId + " should define phases before quest stages");
        assertNotNull(objectives, scenarioId + " should define objectives before quest stages");

        Set<String> knownPhases = new HashSet<>();
        for (String phase : phases.getKeys(false)) {
            knownPhases.add(normalize(phase));
        }

        Set<String> knownObjectives = new HashSet<>();
        for (String objective : objectives.getKeys(false)) {
            knownObjectives.add(normalize(objective));
        }

        Set<String> supportedCompletionModes = Set.of("all_objectives", "any_objective", "manual_turn_in");
        for (String stageId : stages.getKeys(false)) {
            ConfigurationSection stage = stages.getConfigurationSection(stageId);
            assertNotNull(stage, scenarioId + " stage " + stageId + " should be a section");
            assertTrue(
                knownPhases.contains(normalize(stageId)),
                scenarioId + " stage " + stageId + " should reference an existing phase"
            );
            String completionMode = normalizeStageCompletionMode(stage.getString("completion_mode", "all_objectives"));
            assertTrue(
                supportedCompletionModes.contains(completionMode),
                scenarioId + " stage " + stageId + " should use a supported completion mode: " + completionMode
            );
            String nextStage = stage.getString("next_stage", "");
            if (!nextStage.isBlank()) {
                assertTrue(
                    knownPhases.contains(normalize(nextStage)),
                    scenarioId + " stage " + stageId + " should use an existing next_stage: " + nextStage
                );
                assertTrue(
                    !normalize(stageId).equals(normalize(nextStage)),
                    scenarioId + " stage " + stageId + " should not point next_stage to itself"
                );
            }
            List<String> stageObjectives = stage.getStringList("objectives");
            assertTrue(!stageObjectives.isEmpty(), scenarioId + " stage " + stageId + " should list objectives");
            for (String objective : stageObjectives) {
                assertTrue(
                    knownObjectives.contains(normalize(objective)),
                    scenarioId + " stage " + stageId + " should reference existing objective: " + objective
                );
            }
        }
    }

    private void assertObjectivePhases(ConfigurationSection scenarios, String scenarioId, Map<String, String> expectedPhases) {
        for (Map.Entry<String, String> entry : expectedPhases.entrySet()) {
            assertEquals(
                entry.getValue(),
                scenarios.getString(scenarioId + ".quest.objectives." + entry.getKey() + ".phase"),
                scenarioId + " objective " + entry.getKey() + " should declare runtime phase"
            );
        }
    }

    private void assertQuestStages(ConfigurationSection scenarios,
                                   String scenarioId,
                                   Map<String, List<String>> expectedObjectives,
                                   Map<String, String> expectedCompletionModes,
                                   Map<String, String> expectedNextStages) {
        ConfigurationSection stages = scenarios.getConfigurationSection(scenarioId + ".quest.stages");
        assertNotNull(stages, scenarioId + " should define explicit quest stages");
        for (Map.Entry<String, List<String>> entry : expectedObjectives.entrySet()) {
            String stageId = entry.getKey();
            assertEquals(
                entry.getValue(),
                stages.getStringList(stageId + ".objectives"),
                scenarioId + " stage " + stageId + " should list runtime objectives"
            );
            assertEquals(
                expectedCompletionModes.get(stageId),
                stages.getString(stageId + ".completion_mode"),
                scenarioId + " stage " + stageId + " should declare completion mode"
            );
            assertEquals(
                expectedNextStages.getOrDefault(stageId, ""),
                stages.getString(stageId + ".next_stage", ""),
                scenarioId + " stage " + stageId + " should declare expected next_stage"
            );
        }
    }

    private void validateRuntimeSupportedRewards(String scenarioId, ConfigurationSection rewards) {
        assertNotNull(rewards, scenarioId + " should define rewards");
        Set<String> supportedRewardTypes = Set.of("item", "set_story_state", "record_story_event");

        for (String key : rewards.getKeys(false)) {
            ConfigurationSection reward = rewards.getConfigurationSection(key);
            assertNotNull(reward, scenarioId + " reward " + key + " should be a section");
            String type = normalizeRuntimeType(reward.getString("type", "item"));
            assertTrue(
                supportedRewardTypes.contains(type),
                scenarioId + " reward " + key + " should use a supported runtime type: " + type
            );
        }
    }

    private void validateStableEntryIds(String scenarioId, String entryKind, ConfigurationSection entries) {
        assertNotNull(entries, scenarioId + " should define " + entryKind + " entries");
        Set<String> entryIds = new HashSet<>();
        for (String key : entries.getKeys(false)) {
            assertTrue(!key.isBlank(), scenarioId + " " + entryKind + " key should not be blank");
            assertTrue(entryIds.add(normalize(key)), scenarioId + " duplicate " + entryKind + " key: " + key);
        }
    }

    private void validateRewards(ConfigurationSection entries) {
        for (String key : entries.getKeys(false)) {
            ConfigurationSection entry = entries.getConfigurationSection(key);
            assertNotNull(entry, "reward " + key + " should be a section");
            assertTrue(entry.getInt("amount", 0) > 0, "reward " + key + " should have positive amount");
            String type = normalizeRuntimeType(entry.getString("type", "item"));
            if ("item".equals(type)) {
                assertNotNull(Material.matchMaterial(entry.getString("item", "")), "reward " + key + " should use a valid material");
            } else if ("record_story_event".equals(type)) {
                assertTrue(!entry.getString("scope", "").isBlank(), "story reward " + key + " should define scope");
                assertTrue(!entry.getString("target", "").isBlank(), "story reward " + key + " should define target");
                assertTrue(!entry.getString("event_type", "").isBlank(), "story reward " + key + " should define event_type");
            } else if ("set_story_state".equals(type)) {
                assertTrue(!entry.getString("scope", "").isBlank(), "story reward " + key + " should define scope");
                assertTrue(!entry.getString("target", "").isBlank(), "story reward " + key + " should define target");
                assertTrue(!entry.getString("state", "").isBlank(), "story reward " + key + " should define state");
            }
        }
    }

    private void assertDialogueLines(ConfigurationSection dialogues, String key) {
        List<String> lines = dialogues.getStringList(key);
        assertTrue(!lines.isEmpty(), "dialogue key " + key + " should define at least one line");
    }

    private String normalizeRuntimeType(String type) {
        String normalized = normalize(type);
        return switch (normalized) {
            case "", "item", "reward_item" -> "item";
            case "collect", "collectitem", "collect_item", "fetch", "gather" -> "collect_item";
            case "deliver", "deliveritem", "deliver_item", "deliver_to_npc", "turnin", "turn_in" -> "deliver_to_npc";
            case "talk", "speak", "conversation", "talk_to_npc", "speak_to_npc" -> "talk_to_npc";
            case "visit", "travel", "go_to", "visit_region", "enter_region" -> "visit_region";
            case "visitplace", "visit_place", "enterplace", "enter_place", "go_to_place", "place" -> "visit_place";
            case "inspect", "inspectnode", "inspect_node", "interact_node", "node" -> "inspect_node";
            case "kill", "slay", "defeat", "kill_mob" -> "kill_mob";
            case "set_story_state", "record_story_event" -> normalized;
            default -> normalized;
        };
    }

    private String normalizeStageCompletionMode(String completionMode) {
        String normalized = normalize(completionMode);
        return switch (normalized) {
            case "", "all", "all_objective", "all_objectives", "allobjective", "allobjectives" -> "all_objectives";
            case "any", "any_objective", "any_objectives", "anyobjective", "anyobjectives" -> "any_objective";
            case "manual", "manual_turn_in", "manualturnin", "turn_in", "turnin" -> "manual_turn_in";
            default -> normalized;
        };
    }

    private String normalize(String value) {
        return value == null
            ? ""
            : value.trim()
                .toLowerCase(Locale.ROOT)
                .replace("minecraft:", "")
                .replaceAll("[^\\p{L}\\p{Nd}]+", "_")
                .replaceAll("^_+|_+$", "")
                .replaceAll("_+", "_");
    }
}
