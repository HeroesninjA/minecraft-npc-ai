package ro.ainpc.scenario

import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.EntityType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.util.Locale

class MedievalQuestPackTest {
    @Test
    fun medievalQuestPackDefinesPlayableQuestSet() {
        val config = loadMedievalQuestPack()

        val scenarios = config.getConfigurationSection("scenarios")
        assertNotNull(scenarios, "scenarios section should exist")
        assertEquals(
            setOf("Q01", "Q02", "Q03", "Q04", "Q05", "Q06", "Q07", "Q08", "C01", "C02", "D01", "B01", "B02", "E01", "T01", "R01"),
            scenarios!!.getKeys(false)
        )

        val mechanics = config.getConfigurationSection("mechanics")
        assertNotNull(mechanics, "mechanics section should exist")
        assertEquals(setOf("main_quests", "side_quests", "village_contracts", "npc_duties", "local_bounties", "village_events", "onboarding", "village_rituals"), mechanics!!.getKeys(false))
        assertEquals(1, mechanics.getInt("main_quests.max_active"))
        assertEquals(3, mechanics.getInt("side_quests.max_active"))
        assertEquals(3, mechanics.getInt("village_contracts.max_active"))
        assertEquals("contract", mechanics.getString("village_contracts.kind"))
        assertEquals(2, mechanics.getInt("npc_duties.max_active"))
        assertEquals("duty", mechanics.getString("npc_duties.kind"))
        assertEquals(1, mechanics.getInt("local_bounties.max_active"))
        assertEquals("bounty", mechanics.getString("local_bounties.kind"))
        assertEquals(2, mechanics.getInt("village_events.max_active"))
        assertEquals("event", mechanics.getString("village_events.kind"))
        assertEquals(1, mechanics.getInt("onboarding.max_active"))
        assertEquals("tutorial", mechanics.getString("onboarding.kind"))
        assertEquals(1, mechanics.getInt("village_rituals.max_active"))
        assertEquals("ritual", mechanics.getString("village_rituals.kind"))

        val expectedGivers = mapOf(
            "Q01" to "blacksmith", "Q02" to "farmer", "Q03" to "guard", "Q04" to "innkeeper",
            "Q05" to "healer", "Q06" to "blacksmith", "Q07" to "innkeeper", "Q08" to "guard",
            "C01" to "merchant", "C02" to "merchant", "D01" to "guard", "B01" to "guard",
            "B02" to "farmer", "E01" to "guard", "T01" to "priest", "R01" to "priest"
        )
        val expectedBaseTypes = mapOf(
            "Q01" to "QUEST", "Q02" to "QUEST", "Q03" to "QUEST", "Q04" to "QUEST", "Q05" to "QUEST", "Q06" to "QUEST",
            "Q07" to "QUEST", "Q08" to "QUEST", "C01" to "TRADE_DEAL", "C02" to "TRADE_DEAL", "D01" to "DUTY",
            "B01" to "BOUNTY", "B02" to "BOUNTY", "E01" to "WORLD_EVENT", "T01" to "TUTORIAL", "R01" to "RITUAL"
        )
        val expectedMechanics = mapOf(
            "Q01" to "main_quests", "Q02" to "side_quests", "Q03" to "side_quests", "Q04" to "side_quests",
            "Q05" to "side_quests", "Q06" to "side_quests", "Q07" to "side_quests", "Q08" to "side_quests",
            "C01" to "village_contracts", "C02" to "village_contracts", "D01" to "npc_duties", "B01" to "local_bounties",
            "B02" to "local_bounties", "E01" to "village_events", "T01" to "onboarding", "R01" to "village_rituals"
        )
        val expectedKinds = mapOf(
            "Q01" to "fetch", "Q02" to "fetch", "Q03" to "hunt", "Q04" to "fetch", "Q05" to "fetch", "Q06" to "exploration",
            "Q07" to "delivery", "Q08" to "hunt", "C01" to "delivery", "C02" to "investigation", "D01" to "duty",
            "B01" to "hunt", "B02" to "hunt", "E01" to "event", "T01" to "tutorial", "R01" to "ritual"
        )
        val expectedCategories = mapOf(
            "Q01" to "main", "Q02" to "side", "Q03" to "side", "Q04" to "repeatable", "Q05" to "side", "Q06" to "side",
            "Q07" to "side", "Q08" to "side", "C01" to "side", "C02" to "side", "D01" to "repeatable", "B01" to "repeatable",
            "B02" to "repeatable", "E01" to "repeatable", "T01" to "side", "R01" to "repeatable"
        )

        for ((key, giver) in expectedGivers) {
            val scenario = scenarios.getConfigurationSection(key)
            assertNotNull(scenario, "scenario $key should exist")
            assertEquals(expectedBaseTypes[key], scenario!!.getString("base_type"))
            assertEquals(expectedMechanics[key], scenario.getString("mechanic"))
            assertEquals(giver, scenario.getString("quest.giver_profession"))
            assertEquals(expectedKinds[key], scenario.getString("quest.kind"))
            assertEquals(expectedCategories[key], scenario.getString("quest.category"))
            assertEquals("explicit", scenario.getString("quest.acceptance_mode"))
            assertEquals("return_to_giver", scenario.getString("quest.completion_mode"))
            assertEquals("next_objective", scenario.getString("quest.tracking_mode"))
            assertTrue(scenario.getStringList("quest.tags").isNotEmpty(), "$key should define quest tags")
            assertTrue(scenario.getBoolean("requires_player"), "$key should require a player")

            val phases = scenario.getConfigurationSection("phases")
            assertNotNull(phases, "$key should define phases")
            assertTrue(phases!!.contains("INTRODUCTION"))
            assertTrue(phases.contains("ACCEPTANCE"))
            assertTrue(phases.contains("RETURN"))
            assertTrue(phases.contains("COMPLETION"))

            val objectives = scenario.getConfigurationSection("quest.objectives")
            assertNotNull(objectives, "$key should define objectives")
            assertTrue(objectives!!.getKeys(false).size >= 1)
            validateObjectives(objectives)
            validateObjectivePhases(key, phases, objectives)
            validateQuestStages(key, phases, objectives, scenario.getConfigurationSection("quest.stages"))

            val rewards = scenario.getConfigurationSection("quest.rewards")
            assertNotNull(rewards, "$key should define rewards")
            validateRewards(rewards!!)

            val questDialogues = scenario.getConfigurationSection("quest.dialogues")
            assertNotNull(questDialogues, "$key should define quest dialogues")
            assertDialogueLines(questDialogues!!, "offer")
            assertDialogueLines(questDialogues, "offered")
            assertDialogueLines(questDialogues, "accepted")
            assertDialogueLines(questDialogues, "active")
            assertDialogueLines(questDialogues, "ready")
            assertDialogueLines(questDialogues, "completed")
        }

        assertEquals(listOf("Q01"), scenarios.getStringList("Q03.quest.prerequisites"))
        assertTrue(scenarios.getBoolean("Q04.quest.repeatable"))
        assertEquals(1800, scenarios.getInt("Q04.quest.cooldown_seconds"))
        assertTrue(scenarios.getBoolean("C01.quest.repeatable"))
        assertEquals(1200, scenarios.getInt("C01.quest.cooldown_seconds"))
        assertTrue(scenarios.getBoolean("C01.progress.enabled"))
        assertEquals("contract", scenarios.getString("C01.progress.kind"))
        assertEquals("village_contracts", scenarios.getString("C01.progress.mechanic"))
        assertTrue(scenarios.getBoolean("C02.quest.repeatable"))
        assertEquals(1800, scenarios.getInt("C02.quest.cooldown_seconds"))
        assertTrue(scenarios.getBoolean("C02.progress.enabled"))
        assertEquals("contract", scenarios.getString("C02.progress.kind"))
        assertEquals("village_contracts", scenarios.getString("C02.progress.mechanic"))
        assertTrue(scenarios.getBoolean("D01.quest.repeatable"))
        assertEquals(900, scenarios.getInt("D01.quest.cooldown_seconds"))
        assertTrue(scenarios.getBoolean("D01.progress.enabled"))
        assertEquals("duty", scenarios.getString("D01.progress.kind"))
        assertEquals("npc_duties", scenarios.getString("D01.progress.mechanic"))
        assertTrue(scenarios.getBoolean("B01.quest.repeatable"))
        assertEquals(1800, scenarios.getInt("B01.quest.cooldown_seconds"))
        assertTrue(scenarios.getBoolean("B01.progress.enabled"))
        assertEquals("bounty", scenarios.getString("B01.progress.kind"))
        assertEquals("local_bounties", scenarios.getString("B01.progress.mechanic"))
        assertTrue(scenarios.getBoolean("B02.quest.repeatable"))
        assertEquals(2700, scenarios.getInt("B02.quest.cooldown_seconds"))
        assertTrue(scenarios.getBoolean("B02.progress.enabled"))
        assertEquals("bounty", scenarios.getString("B02.progress.kind"))
        assertEquals("local_bounties", scenarios.getString("B02.progress.mechanic"))
        assertTrue(scenarios.getBoolean("E01.quest.repeatable"))
        assertEquals(2400, scenarios.getInt("E01.quest.cooldown_seconds"))
        assertTrue(scenarios.getBoolean("E01.progress.enabled"))
        assertEquals("event", scenarios.getString("E01.progress.kind"))
        assertEquals("village_events", scenarios.getString("E01.progress.mechanic"))
        assertFalse(scenarios.getBoolean("T01.quest.repeatable"))
        assertTrue(scenarios.getBoolean("T01.progress.enabled"))
        assertEquals("tutorial", scenarios.getString("T01.progress.kind"))
        assertEquals("onboarding", scenarios.getString("T01.progress.mechanic"))
        assertTrue(scenarios.getBoolean("R01.quest.repeatable"))
        assertEquals(3600, scenarios.getInt("R01.quest.cooldown_seconds"))
        assertTrue(scenarios.getBoolean("R01.progress.enabled"))
        assertEquals("ritual", scenarios.getString("R01.progress.kind"))
        assertEquals("village_rituals", scenarios.getString("R01.progress.mechanic"))
        assertEquals(listOf("Q02"), scenarios.getStringList("Q05.quest.prerequisites"))
        assertEquals(listOf("Q01"), scenarios.getStringList("Q06.quest.prerequisites"))
        assertEquals(listOf("Q03", "Q04"), scenarios.getStringList("Q07.quest.prerequisites"))
        assertEquals(listOf("Q03"), scenarios.getStringList("Q08.quest.prerequisites"))
    }

    @Test
    fun medievalQuestPackHasStableRuntimeContractForStarterQuests() {
        val config = loadMedievalQuestPack()

        val professions = config.getConfigurationSection("professions")
        val scenarios = config.getConfigurationSection("scenarios")
        assertNotNull(professions, "professions section should exist")
        assertNotNull(scenarios, "scenarios section should exist")

        val knownQuestReferences = HashSet<String>()
        for (scenarioId in scenarios!!.getKeys(false)) {
            knownQuestReferences.add(normalize(scenarioId))
            knownQuestReferences.add(normalize("medieval_quest:$scenarioId"))
            knownQuestReferences.add(normalize(scenarios.getString("$scenarioId.quest.code", "")))
        }

        val questCodes = HashSet<String>()
        for (scenarioId in scenarios.getKeys(false)) {
            val scenario = scenarios.getConfigurationSection(scenarioId)
            assertNotNull(scenario, "scenario $scenarioId should exist")

            val questCode = scenario!!.getString("quest.code", "") ?: ""
            assertTrue(questCode.isNotBlank(), "$scenarioId should define quest.code")
            assertTrue(questCodes.add(normalize(questCode)), "quest.code should be unique: $questCode")

            val giverProfession = scenario.getString("quest.giver_profession", "") ?: ""
            assertTrue(giverProfession.isNotBlank(), "$scenarioId should define giver profession")
            assertTrue(professions!!.contains(giverProfession), "$scenarioId giver profession should exist")

            val giverRole = scenario.getConfigurationSection("roles.QUEST_GIVER")
            assertNotNull(giverRole, "$scenarioId should define QUEST_GIVER role")
            assertTrue(giverRole!!.getStringList("required_professions").contains(giverProfession), "$scenarioId QUEST_GIVER role should require the configured giver profession")

            for (prerequisite in scenario.getStringList("quest.prerequisites")) {
                assertTrue(knownQuestReferences.contains(normalize(prerequisite)), "$scenarioId prerequisite should reference an existing quest: $prerequisite")
            }

            val repeatable = scenario.getBoolean("quest.repeatable", false)
            val cooldownSeconds = scenario.getInt("quest.cooldown_seconds", 0)
            if (repeatable) {
                assertTrue(cooldownSeconds > 0, "$scenarioId repeatable quests should define cooldown_seconds")
            } else {
                assertEquals(0, cooldownSeconds, "$scenarioId non-repeatable quests should not define cooldown_seconds")
            }

            val dialogues = scenario.getConfigurationSection("quest.dialogues")
            assertNotNull(dialogues, "$scenarioId should define quest dialogues")
            if (repeatable || scenario.getStringList("quest.prerequisites").isNotEmpty()) {
                assertDialogueLines(dialogues!!, "unavailable")
            }

            validateRuntimeSupportedObjectives(scenarioId, scenario.getConfigurationSection("quest.objectives"))
            validateRuntimeSupportedRewards(scenarioId, scenario.getConfigurationSection("quest.rewards"))
            validateStableEntryIds(scenarioId, "objective", scenario.getConfigurationSection("quest.objectives"))
            validateStableEntryIds(scenarioId, "reward", scenario.getConfigurationSection("quest.rewards"))
            validateObjectivePhases(scenarioId, scenario.getConfigurationSection("phases"), scenario.getConfigurationSection("quest.objectives"))
            validateQuestStages(scenarioId, scenario.getConfigurationSection("phases"), scenario.getConfigurationSection("quest.objectives"), scenario.getConfigurationSection("quest.stages"))
        }
    }

    private fun loadMedievalQuestPack(): YamlConfiguration {
        return YamlConfiguration.loadConfiguration(File("src/main/resources/packs/medieval_quest.yml"))
    }

    private fun validateObjectives(objectives: ConfigurationSection) {
        for (key in objectives.getKeys(false)) {
            val objective = objectives.getConfigurationSection(key)
            assertNotNull(objective, "objective $key should be a section")

            val type = objective!!.getString("type", "") ?: ""
            val item = objective.getString("item", "") ?: ""
            assertTrue(objective.getInt("amount", 0) > 0, "objective $key should have positive amount")
            val normalizedType = normalizeRuntimeType(type)
            if ("kill_mob" == normalizedType) {
                assertNotNull(EntityType.valueOf(item), "objective $key should target a valid entity type")
            } else if ("collect_item" == normalizedType || "deliver_to_npc" == normalizedType) {
                assertNotNull(Material.matchMaterial(item), "objective $key should use a valid material")
            } else {
                assertTrue(item.isNotBlank(), "semantic objective $key should define a reference")
            }
        }
    }

    private fun validateRuntimeSupportedObjectives(scenarioId: String, objectives: ConfigurationSection?) {
        assertNotNull(objectives, "$scenarioId should define objectives")
        val supportedObjectiveTypes = setOf("collect_item", "deliver_to_npc", "talk_to_npc", "visit_region", "visit_place", "inspect_node", "kill_mob")

        for (key in objectives!!.getKeys(false)) {
            val objective = objectives.getConfigurationSection(key)
            assertNotNull(objective, "$scenarioId objective $key should be a section")
            val type = normalizeRuntimeType(objective!!.getString("type", "") ?: "")
            assertTrue(supportedObjectiveTypes.contains(type), "$scenarioId objective $key should use a supported runtime type: $type")
        }
    }

    private fun validateObjectivePhases(scenarioId: String, phases: ConfigurationSection?, objectives: ConfigurationSection?) {
        assertNotNull(phases, "$scenarioId should define phases")
        assertNotNull(objectives, "$scenarioId should define objectives")

        val knownPhases = HashSet<String>()
        for (phase in phases!!.getKeys(false)) knownPhases.add(normalize(phase))

        for (key in objectives!!.getKeys(false)) {
            val objective = objectives.getConfigurationSection(key)
            assertNotNull(objective, "$scenarioId objective $key should be a section")
            val phase = objective!!.getString("phase", "") ?: ""
            if (phase.isNotBlank()) {
                assertTrue(knownPhases.contains(normalize(phase)), "$scenarioId objective $key should reference an existing phase: $phase")
            }
        }
    }

    private fun validateQuestStages(scenarioId: String, phases: ConfigurationSection?, objectives: ConfigurationSection?, stages: ConfigurationSection?) {
        if (stages == null) return
        assertNotNull(phases, "$scenarioId should define phases before quest stages")
        assertNotNull(objectives, "$scenarioId should define objectives before quest stages")

        val knownPhases = HashSet<String>()
        for (phase in phases!!.getKeys(false)) knownPhases.add(normalize(phase))

        val knownObjectives = HashSet<String>()
        for (objective in objectives!!.getKeys(false)) knownObjectives.add(normalize(objective))

        val supportedCompletionModes = setOf("all_objectives", "any_objective", "manual_turn_in")
        for (stageId in stages.getKeys(false)) {
            val stage = stages.getConfigurationSection(stageId)
            assertNotNull(stage, "$scenarioId stage $stageId should be a section")
            assertTrue(knownPhases.contains(normalize(stageId)), "$scenarioId stage $stageId should reference an existing phase")
            val completionMode = normalizeStageCompletionMode(stage!!.getString("completion_mode", "all_objectives") ?: "all_objectives")
            assertTrue(supportedCompletionModes.contains(completionMode), "$scenarioId stage $stageId should use a supported completion mode: $completionMode")
            val nextStage = stage.getString("next_stage", "") ?: ""
            if (nextStage.isNotBlank()) {
                assertTrue(knownPhases.contains(normalize(nextStage)), "$scenarioId stage $stageId should use an existing next_stage: $nextStage")
                assertTrue(normalize(stageId) != normalize(nextStage), "$scenarioId stage $stageId should not point next_stage to itself")
            }
            val stageObjectives = stage.getStringList("objectives")
            assertTrue(stageObjectives.isNotEmpty(), "$scenarioId stage $stageId should list objectives")
            for (objective in stageObjectives) {
                assertTrue(knownObjectives.contains(normalize(objective)), "$scenarioId stage $stageId should reference existing objective: $objective")
            }
        }
    }

    private fun validateRuntimeSupportedRewards(scenarioId: String, rewards: ConfigurationSection?) {
        assertNotNull(rewards, "$scenarioId should define rewards")
        val supportedRewardTypes = setOf("item", "set_story_state", "record_story_event")

        for (key in rewards!!.getKeys(false)) {
            val reward = rewards.getConfigurationSection(key)
            assertNotNull(reward, "$scenarioId reward $key should be a section")
            val type = normalizeRuntimeType(reward!!.getString("type", "item") ?: "item")
            assertTrue(supportedRewardTypes.contains(type), "$scenarioId reward $key should use a supported runtime type: $type")
        }
    }

    private fun validateStableEntryIds(scenarioId: String, entryKind: String, entries: ConfigurationSection?) {
        assertNotNull(entries, "$scenarioId should define $entryKind entries")
        val entryIds = HashSet<String>()
        for (key in entries!!.getKeys(false)) {
            assertTrue(key.isNotBlank(), "$scenarioId $entryKind key should not be blank")
            assertTrue(entryIds.add(normalize(key)), "$scenarioId duplicate $entryKind key: $key")
        }
    }

    private fun validateRewards(entries: ConfigurationSection) {
        for (key in entries.getKeys(false)) {
            val entry = entries.getConfigurationSection(key)
            assertNotNull(entry, "reward $key should be a section")
            assertTrue(entry!!.getInt("amount", 0) > 0, "reward $key should have positive amount")
            val type = normalizeRuntimeType(entry.getString("type", "item") ?: "item")
            when (type) {
                "item" -> assertNotNull(Material.matchMaterial(entry.getString("item", "") ?: ""), "reward $key should use a valid material")
                "record_story_event" -> {
                    assertTrue((entry.getString("scope", "") ?: "").isNotBlank(), "story reward $key should define scope")
                    assertTrue((entry.getString("target", "") ?: "").isNotBlank(), "story reward $key should define target")
                    assertTrue((entry.getString("event_type", "") ?: "").isNotBlank(), "story reward $key should define event_type")
                }
                "set_story_state" -> {
                    assertTrue((entry.getString("scope", "") ?: "").isNotBlank(), "story reward $key should define scope")
                    assertTrue((entry.getString("target", "") ?: "").isNotBlank(), "story reward $key should define target")
                    assertTrue((entry.getString("state", "") ?: "").isNotBlank(), "story reward $key should define state")
                }
            }
        }
    }

    private fun assertDialogueLines(dialogues: ConfigurationSection, key: String) {
        val lines = dialogues.getStringList(key)
        assertTrue(lines.isNotEmpty(), "dialogue key $key should define at least one line")
    }

    private fun normalizeRuntimeType(type: String): String {
        val normalized = normalize(type)
        return when (normalized) {
            "", "item", "reward_item" -> "item"
            "collect", "collectitem", "collect_item", "fetch", "gather" -> "collect_item"
            "deliver", "deliveritem", "deliver_item", "deliver_to_npc", "turnin", "turn_in" -> "deliver_to_npc"
            "talk", "speak", "conversation", "talk_to_npc", "speak_to_npc" -> "talk_to_npc"
            "visit", "travel", "go_to", "visit_region", "enter_region" -> "visit_region"
            "visitplace", "visit_place", "enterplace", "enter_place", "go_to_place", "place" -> "visit_place"
            "inspect", "inspectnode", "inspect_node", "interact_node", "node" -> "inspect_node"
            "kill", "slay", "defeat", "kill_mob" -> "kill_mob"
            "set_story_state", "record_story_event" -> normalized
            else -> normalized
        }
    }

    private fun normalizeStageCompletionMode(completionMode: String): String {
        val normalized = normalize(completionMode)
        return when (normalized) {
            "", "all", "all_objective", "all_objectives", "allobjective", "allobjectives" -> "all_objectives"
            "any", "any_objective", "any_objectives", "anyobjective", "anyobjectives" -> "any_objective"
            "manual", "manual_turn_in", "manualturnin", "turn_in", "turnin" -> "manual_turn_in"
            else -> normalized
        }
    }

    private fun normalize(value: String?): String {
        return value?.trim()
            ?.lowercase(Locale.ROOT)
            ?.replace("minecraft:", "")
            ?.replace(Regex("[^\\p{L}\\p{Nd}]+"), "_")
            ?.replace(Regex("^_+|_+$"), "")
            ?.replace(Regex("_+"), "_")
            ?: ""
    }
}
