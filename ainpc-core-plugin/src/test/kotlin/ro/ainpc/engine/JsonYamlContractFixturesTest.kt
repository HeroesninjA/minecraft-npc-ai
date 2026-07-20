package ro.ainpc.engine

import ro.ainpc.debug.DebugDumpMappingSnapshotJson
import ro.ainpc.debug.DebugDumpNarrativePlanJson
import ro.ainpc.debug.DebugDumpQuestConfigJson
import ro.ainpc.debug.DebugDumpQuestDefinitionJson
import ro.ainpc.debug.DebugDumpQuestMappingContractJson
import ro.ainpc.debug.DebugDumpQuestText
import ro.ainpc.debug.DebugDumpStoryText
import ro.ainpc.debug.DebugDumpWorldAdminJson
import ro.ainpc.debug.ScriptDocumentNormalizer
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ro.ainpc.engine.FeaturePackLoader
import ro.ainpc.engine.ActiveScenario
import ro.ainpc.engine.ScenarioTemplate
import ro.ainpc.engine.ScenarioType
import ro.ainpc.debug.DebugDumpWorldJson
import ro.ainpc.platform.PlatformProfile
import ro.ainpc.platform.RuntimeMode
import java.nio.file.Files
import java.nio.file.Path
import java.util.logging.Logger
import ro.ainpc.world.StoryMode
import ro.ainpc.world.WorldAdminService
import ro.ainpc.world.WorldMode

class JsonYamlContractFixturesTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun questFixturesProduceSameLogicalConfiguration() {
        val yamlConfig = loadFixture("json-yaml-contract/quests.yml", "quests.yml")
        val jsonConfig = loadFixture("json-yaml-contract/quests.json", "quests.json")

        assertEquals(yamlConfig.getString("type"), jsonConfig.getString("type"))
        assertEquals(yamlConfig.getInt("version"), jsonConfig.getInt("version"))
        assertEquals(yamlConfig.getString("meta.id"), jsonConfig.getString("meta.id"))
        assertEquals(yamlConfig.getString("meta.title"), jsonConfig.getString("meta.title"))
        assertEquals(yamlConfig.getString("spec.objectives.0.target.ref"), jsonConfig.getString("spec.objectives.0.target.ref"))
        assertEquals(yamlConfig.getInt("spec.rewards.0.amount"), jsonConfig.getInt("spec.rewards.0.amount"))
    }

    @Test
    fun jsonFeaturePackFlowsThroughLoaderPathIntoRuntimeModels() {
        val resource = requireNotNull(javaClass.classLoader.getResource("json-yaml-contract/quests.json")) {
            "Missing JSON feature-pack fixture"
        }
        val jsonFile = tempDir.resolve("feature-pack.json").toFile()
        jsonFile.writeText(Files.readString(Path.of(resource.toURI())))

        assertTrue(FeaturePackLoader.isSupportedPackFile(jsonFile))
        assertTrue(FeaturePackLoader.isSupportedPackFile(tempDir.resolve("feature-pack.YAML").toFile()))
        assertFalse(FeaturePackLoader.isSupportedPackFile(tempDir.resolve("feature-pack.txt").toFile()))

        val config = FeaturePackLoader.loadPackConfiguration(jsonFile)
        val validation = FeaturePackMetadataValidator.validate(config, jsonFile, RuntimeMode.STANDALONE)
        assertTrue(validation.valid(), "JSON feature-pack metadata errors: ${validation.errors()}")

        val pack = FeaturePackLoader.FeaturePack(
            requireNotNull(config.getString("id")),
            requireNotNull(config.getString("name")),
            config.getString("description", "") ?: "",
        )
        pack.schemaVersion = config.getInt("version", pack.schemaVersion)

        val traits = linkedMapOf<String, FeaturePackLoader.TraitDefinition>()
        val dialogues = linkedMapOf<String, List<String>>()
        val scenarios = linkedMapOf<String, FeaturePackLoader.ScenarioDefinition>()
        FeaturePackYamlSupport.loadTraits(pack, requireNotNull(config.getConfigurationSection("traits")), traits)
        FeaturePackYamlSupport.loadDialogues(
            pack,
            requireNotNull(config.getConfigurationSection("dialogues")),
            dialogues,
        )
        FeaturePackYamlSupport.loadScenarios(
            pack,
            requireNotNull(config.getConfigurationSection("scenarios")),
            scenarios,
            {},
            { _, _ -> null },
        )

        assertEquals(1, pack.schemaVersion)
        assertEquals("Ajutator", traits.getValue("helpful").name)
        assertEquals(30, traits.getValue("helpful").getActionModifier("HELP"))
        assertEquals(listOf("Bine ai venit!", "Ce mai faci?"), dialogues["contract_example:greeting"])
        val scenario = scenarios.getValue("contract_example:quest_intro_market")
        assertEquals("QM01", scenario.questCode)
        assertEquals(2, scenario.objectives.size)
        assertEquals(2, scenario.rewards.size)
        assertEquals(listOf("peaceful", "trade", "festival"), config.getStringList("story_defaults.settlement.pool"))
        assertTrue(pack.hasScenarioDefinitions())
    }

    @Test
    fun worldAdminFixturesProduceSameLogicalConfiguration() {
        val yamlConfig = loadFixture("json-yaml-contract/world-admin.yml", "world-admin.yml")
        val jsonConfig = loadFixture("json-yaml-contract/world-admin.json", "world-admin.json")

        assertTrue(yamlConfig.getBoolean("world_admin.enabled"))
        assertTrue(jsonConfig.getBoolean("world_admin.enabled"))
        assertEquals(
            yamlConfig.getString("world_admin.regions.satul_central.name"),
            jsonConfig.getString("world_admin.regions.satul_central.name")
        )
        assertEquals(
            yamlConfig.getString("world_admin.regions.satul_central.places.piata.nodes.quest_board.type"),
            jsonConfig.getString("world_admin.regions.satul_central.places.piata.nodes.quest_board.type")
        )
        assertNotNull(jsonConfig.getConfigurationSection("world_admin.regions.satul_central"))
    }

    @Test
    fun questFixtureFeedsQuestSnapshotHelper() {
        val resource = requireNotNull(javaClass.classLoader.getResource("json-yaml-contract/quests.yml")) {
            "Missing quest fixture"
        }
        val questDocument = ScriptDocumentNormalizer.normalizeDocument(
            resource.openStream().bufferedReader().use { it.readText() },
            "quests.yml",
        )

        val snapshot = DebugDumpQuestConfigJson.buildQuestConfigSnapshotJson(
            questDocument,
            "quests.yml",
            true,
        )

        assertTrue(snapshot.getAsJsonObject("summary").get("has_type").asBoolean)
        assertTrue(snapshot.getAsJsonObject("summary").get("has_version").asBoolean)
        assertTrue(
            snapshot.getAsJsonArray("sections").any { element -> element.asString == "spec" }
        )
        assertEquals("quests.yml", snapshot.get("quest_file_name").asString)
    }

    @Test
    fun worldAdminFixtureFeedsMappingSnapshotHelper() {
        val config = loadFixture("json-yaml-contract/world-admin.yml", "world-admin.yml")
        val worldAdminService = WorldAdminService({ }, Logger.getLogger("JsonYamlContractFixturesTest"))
        worldAdminService.reloadFromConfig(config, platformProfile())

        val worldMapping = DebugDumpWorldJson.buildWorldMappingJson(worldAdminService)
        val snapshot = DebugDumpMappingSnapshotJson.buildMappingSnapshotJson(
            worldMapping,
            worldAdminService,
            listOf("world-admin.yml"),
        )

        assertTrue(snapshot.get("world_admin_enabled").asBoolean)
        assertTrue(snapshot.get("auto_index_enabled").asBoolean)
        assertEquals(1, snapshot.get("region_count").asInt)
        assertEquals(1, snapshot.get("place_count").asInt)
        assertEquals(1, snapshot.get("node_count").asInt)
        assertTrue(
            snapshot.getAsJsonArray("overlay_sources").any { element -> element.asString == "world-admin.yml" }
        )
    }

    @Test
    fun worldAdminFixtureFeedsWorldAdminSnapshotHelper() {
        val config = loadFixture("json-yaml-contract/world-admin.yml", "world-admin.yml")
        val worldAdminService = WorldAdminService({ }, Logger.getLogger("JsonYamlContractFixturesTest"))
        worldAdminService.reloadFromConfig(config, platformProfile())

        val snapshot = DebugDumpWorldAdminJson.buildWorldAdminSnapshotJson(
            worldAdminService,
            listOf("world-admin.yml"),
        )

        assertTrue(snapshot.get("available").asBoolean)
        assertTrue(snapshot.get("enabled").asBoolean)
        assertTrue(snapshot.get("auto_index_enabled").asBoolean)
        assertEquals("finite_dynamic", snapshot.get("world_mode").asString)
        assertEquals(1, snapshot.get("region_count").asInt)
        assertEquals(1, snapshot.get("place_count").asInt)
        assertEquals(1, snapshot.get("node_count").asInt)
        assertTrue(
            snapshot.getAsJsonArray("source_files").any { element -> element.asString == "world-admin.yml" }
        )
        assertTrue(snapshot.getAsJsonObject("world_mapping").get("enabled").asBoolean)
    }

    @Test
    fun fixtureDataFeedsQuestMappingContractHelper() {
        val questResource = requireNotNull(javaClass.classLoader.getResource("json-yaml-contract/quests.yml")) {
            "Missing quest fixture"
        }
        val questDocument = ScriptDocumentNormalizer.normalizeDocument(
            questResource.openStream().bufferedReader().use { it.readText() },
            "quests.yml",
        )

        val worldAdminConfig = loadFixture("json-yaml-contract/world-admin.yml", "world-admin.yml")
        val worldAdminService = WorldAdminService({ }, Logger.getLogger("JsonYamlContractFixturesTest"))
        worldAdminService.reloadFromConfig(worldAdminConfig, platformProfile())
        val worldMapping = DebugDumpWorldJson.buildWorldMappingJson(worldAdminService)

        val snapshot = DebugDumpQuestMappingContractJson.buildQuestMappingContractJson(
            questDocument,
            "quests.yml",
            worldMapping,
        )

        assertTrue(snapshot.get("available").asBoolean)
        assertEquals("quests.yml", snapshot.get("quest_file_name").asString)
        assertTrue(snapshot.getAsJsonArray("supported_formats").any { element -> element.asString == "json" })
        assertTrue(snapshot.getAsJsonObject("quest_summary").get("has_type").asBoolean)
        assertTrue(snapshot.getAsJsonObject("quest_summary").get("has_spec").asBoolean)
        assertTrue(snapshot.getAsJsonObject("mapping_summary").get("enabled").asBoolean)
        assertTrue(snapshot.getAsJsonObject("mapping_summary").getAsJsonArray("semantic_index_keys").size() > 0)
    }

    @Test
    fun jsonFixtureDataFeedsQuestMappingContractHelper() {
        val questResource = requireNotNull(javaClass.classLoader.getResource("json-yaml-contract/quests.json")) {
            "Missing quest fixture"
        }
        val questDocument = ScriptDocumentNormalizer.normalizeDocument(
            questResource.openStream().bufferedReader().use { it.readText() },
            "quests.json",
        )

        val worldAdminConfig = loadFixture("json-yaml-contract/world-admin.json", "world-admin.json")
        val worldAdminService = WorldAdminService({ }, Logger.getLogger("JsonYamlContractFixturesTest"))
        worldAdminService.reloadFromConfig(worldAdminConfig, platformProfile())
        val worldMapping = DebugDumpWorldJson.buildWorldMappingJson(worldAdminService)

        val snapshot = DebugDumpQuestMappingContractJson.buildQuestMappingContractJson(
            questDocument,
            "quests.json",
            worldMapping,
        )

        assertTrue(snapshot.get("available").asBoolean)
        assertEquals("quests.json", snapshot.get("quest_file_name").asString)
        assertTrue(snapshot.getAsJsonArray("supported_formats").any { element -> element.asString == "json" })
        assertTrue(snapshot.getAsJsonObject("quest_summary").get("has_type").asBoolean)
        assertTrue(snapshot.getAsJsonObject("mapping_summary").get("enabled").asBoolean)
        assertTrue(snapshot.getAsJsonObject("mapping_summary").getAsJsonArray("semantic_index_keys").size() > 0)
    }

    @Test
    fun worldAdminFixtureFeedsNarrativePlanHelper() {
        val worldAdminConfig = loadFixture("json-yaml-contract/world-admin.yml", "world-admin.yml")
        val worldAdminService = WorldAdminService({ }, Logger.getLogger("JsonYamlContractFixturesTest"))
        worldAdminService.reloadFromConfig(worldAdminConfig, platformProfile())

        val snapshot = DebugDumpNarrativePlanJson.buildNarrativePlanJson(worldAdminService)

        assertTrue(snapshot.get("available").asBoolean)
        assertEquals("narrative_generator", snapshot.get("source_type").asString)
        assertTrue(snapshot.get("region_count").asInt >= 0)
    }

    @Test
    fun scenarioFixtureFeedsLoadedQuestDefinitionsHelper() {
        val scenario = FeaturePackLoader.ScenarioDefinition(
            "pack_demo",
            "quest_demo",
            "Quest demo",
            "Descriere demo",
            ScenarioType.QUEST,
        )
        scenario.questCode = "quest_demo"
        scenario.questTags = listOf("demo", "quest")
        scenario.addObjective(
            FeaturePackLoader.QuestEntryDefinition("collect", "wood", 3, "Colecteaza lemn")
        )
        scenario.addReward(
            FeaturePackLoader.QuestEntryDefinition("xp", "xp", 10, "Experienta")
        )

        val snapshot = DebugDumpQuestDefinitionJson.buildLoadedQuestDefinitionsJson(
            listOf(scenario),
            emptyList(),
            emptyList(),
            com.google.gson.Gson(),
        )

        assertTrue(snapshot.get("available").asBoolean)
        assertEquals(1, snapshot.get("scenario_count").asInt)
        assertEquals(1, snapshot.get("quest_count").asInt)
        assertTrue(snapshot.getAsJsonArray("rows").size() == 1)
        assertTrue(!snapshot.getAsJsonArray("rows")[0].asJsonObject.get("progression_enabled").asBoolean)
    }

    @Test
    fun questTextHelperUsesProvidedSnapshots() {
        val progression = JsonObject().apply {
            addProperty("available", true)
            addProperty("row_count", 2)
            addProperty("player_count", 1)
            addProperty("current_count", 1)
            addProperty("archived_count", 0)
            addProperty("tracked_count", 1)
            addProperty("resolved_definition_count", 1)
            addProperty("unresolved_definition_count", 0)
            add("by_status", JsonObject().apply { addProperty("active", 2) })
            add("by_mechanic", JsonObject().apply { addProperty("quest", 2) })
            add("by_kind", JsonObject().apply { addProperty("quest", 2) })
            add("by_scenario_kind", JsonObject().apply { addProperty("main", 2) })
            add("by_base_type", JsonObject().apply { addProperty("QUEST", 2) })
        }
        val questProgress = JsonObject().apply {
            addProperty("available", true)
            addProperty("row_count", 1)
            addProperty("current_count", 1)
            addProperty("archived_count", 0)
            addProperty("tracked_count", 1)
            add("by_status", JsonObject().apply { addProperty("active", 1) })
            add("by_template", JsonObject().apply { addProperty("pack_demo:quest_demo", 1) })
        }
        val anchors = JsonObject().apply {
            addProperty("available", true)
            addProperty("row_count", 1)
            add("by_template", JsonObject().apply { addProperty("pack_demo:quest_demo", 1) })
            add("by_anchor_type", JsonObject().apply { addProperty("quest_giver", 1) })
        }

        val text = DebugDumpQuestText.buildQuestText(
            progression,
            questProgress,
            anchors,
            "[WARN] demo warning\n[ERROR] demo error",
        )

        assertTrue(text.contains("AINPC Quest Dump"))
        assertTrue(text.contains("Quest audit errors: 1"))
        assertTrue(text.contains("Quest audit warnings: 1"))
        assertTrue(text.contains("Progressions available: true"))
    }

    @Test
    fun storyTextHelperUsesProvidedSnapshots() {
        val template = ScenarioTemplate(
            ScenarioType.QUEST,
        ).apply {
            templateId = "scenario_demo"
            displayName = "Scenario demo"
            hint = "Hint demo"
        }
        val scenario = ActiveScenario(java.util.UUID.randomUUID(), template).also {
            it.currentPhase = "phase_one"
        }
        val states = JsonObject().apply {
            addProperty("available", true)
            addProperty("region_state_count", 1)
            addProperty("place_state_count", 1)
            addProperty("invalid_json_count", 0)
            add("regions_by_mode", JsonObject().apply { addProperty("evolutive", 1) })
            add("regions_by_state", JsonObject().apply { addProperty("ready", 1) })
            add("regions_by_source", JsonObject().apply { addProperty("world_admin", 1) })
            add("places_by_region", JsonObject().apply { addProperty("satul_central", 1) })
            add("places_by_state", JsonObject().apply { addProperty("ready", 1) })
            add("places_by_source", JsonObject().apply { addProperty("world_admin", 1) })
        }
        val events = JsonObject().apply {
            addProperty("available", true)
            addProperty("row_count", 1)
            addProperty("progression_cross_link_available", true)
            addProperty("progression_cross_link_source_rows", 1)
            add("by_event_type", JsonObject().apply { addProperty("quest_started", 1) })
            add("by_scope_type", JsonObject().apply { addProperty("player", 1) })
            add("by_quest_template", JsonObject().apply { addProperty("pack_demo:scenario_demo", 1) })
            add("by_quest_code", JsonObject().apply { addProperty("scenario_demo", 1) })
            add("by_progression_link", JsonObject().apply { addProperty("player:scenario_demo", 1) })
        }
        val gaps = JsonObject().apply {
            addProperty("gap_count", 0)
            addProperty("story_event_row_count", 1)
            addProperty("linked_count", 1)
            add("by_status", JsonObject())
            add("by_template", JsonObject())
            add("by_mechanic", JsonObject())
        }

        val text = DebugDumpStoryText.buildStoryText(listOf(scenario), states, events, gaps)

        assertTrue(text.contains("AINPC Story Dump"))
        assertTrue(text.contains("Active scenarios: 1"))
        assertTrue(text.contains("Story states available: true"))
        assertTrue(text.contains("Story events rows: 1"))
    }

    @Test
    fun storySummaryTextHelperUsesProvidedSnapshots() {
        val scenario = ActiveScenario(java.util.UUID.randomUUID(), ScenarioTemplate(ScenarioType.QUEST))
        val states = JsonObject().apply {
            addProperty("region_state_count", 1)
            addProperty("place_state_count", 1)
        }
        val events = JsonObject().apply {
            addProperty("row_count", 1)
            addProperty("progression_cross_link_available", true)
            addProperty("progression_cross_link_source_rows", 1)
        }
        val gaps = JsonObject().apply {
            addProperty("gap_count", 0)
        }

        val text = DebugDumpStoryText.buildSummaryText(listOf(scenario), states, events, gaps)

        assertTrue(text.contains("AINPC Story Summary"))
        assertTrue(text.contains("Active scenarios: 1"))
        assertTrue(text.contains("Region story states: 1"))
        assertTrue(text.contains("Story progression gaps: 0"))
    }

    private fun loadFixture(resourcePath: String, fileName: String) = run {
        val resourceUrl = requireNotNull(javaClass.classLoader.getResource(resourcePath)) {
            "Missing test fixture $resourcePath"
        }
        val file = tempDir.resolve(fileName).toFile()
        file.writeText(Files.readString(Path.of(resourceUrl.toURI())))
        ScriptConfigurationLoader.loadConfiguration(file)
    }

    private fun platformProfile(): PlatformProfile {
        return PlatformProfile(RuntimeMode.STANDALONE, WorldMode.FINITE_DYNAMIC, StoryMode.EVOLUTIVE)
    }
}
