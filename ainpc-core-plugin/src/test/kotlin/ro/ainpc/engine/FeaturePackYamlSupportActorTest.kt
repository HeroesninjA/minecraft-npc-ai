package ro.ainpc.engine

import org.bukkit.configuration.file.YamlConfiguration
import com.google.gson.Gson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.debug.DebugDumpQuestDefinitionJson
import ro.ainpc.npc.NpcEntityKind
import ro.ainpc.npc.NpcInteractionProfile
import ro.ainpc.npc.NpcLifecycleType
import ro.ainpc.npc.NpcPersistenceMode
import ro.ainpc.npc.NpcSpawnPolicy
import ro.ainpc.npc.NpcSimulationMode

class FeaturePackYamlSupportActorTest {
    @Test
    fun loadsScenarioActorsFromYamlSection() {
        val yaml = """
            actors:
              dungeon_spirit:
                name: "Spiritul din Cripta"
                lifecycle_type: "episodic"
                persistence_mode: "runtime_only"
                simulation_mode: "none"
                interaction_profile: "scene_only"
                entity_kind: "spirit"
                entity_archetype: "wraith"
                owner_scenario_id: "medieval_quest"
                owner_quest_id: "Q08"
                spawn_source: "border_patrol_scene"
                spawn_phase: "investigation"
                spawn_policy: "phase"
                despawn_rule: "on_stage_complete"
                duration_seconds: 180
                temporary_tags:
                  - "undead"
                  - "dungeon"
                  - "scene"
        """.trimIndent()
        val config = YamlConfiguration()
        config.loadFromString(yaml)

        val scenario = FeaturePackLoader.ScenarioDefinition(
            "medieval_quest",
            "Q08",
            "Patrula De Hotar",
            "Descriere",
            ScenarioType.QUEST,
        )

        FeaturePackYamlSupport.loadScenarioActors(scenario, config.getConfigurationSection("actors"))

        val actor = scenario.actors["dungeon_spirit"]
        assertTrue(actor != null)
        assertEquals("Spiritul din Cripta", actor?.name)
        assertEquals(NpcLifecycleType.EPISODIC, actor?.lifecycleType)
        assertEquals(NpcPersistenceMode.RUNTIME_ONLY, actor?.persistenceMode)
        assertEquals(NpcSimulationMode.NONE, actor?.simulationMode)
        assertEquals(NpcInteractionProfile.SCENE_ONLY, actor?.interactionProfile)
        assertEquals(NpcEntityKind.SPIRIT, actor?.entityKind)
        assertEquals("wraith", actor?.entityArchetype)
        assertEquals("medieval_quest", actor?.ownerScenarioId)
        assertEquals("Q08", actor?.ownerQuestId)
        assertEquals("border_patrol_scene", actor?.spawnSource)
        assertEquals("investigation", actor?.spawnPhase)
        assertEquals("PHASE", actor?.spawnPolicy?.name)
        assertEquals("on_stage_complete", actor?.despawnRule)
        assertEquals(180L, actor?.durationSeconds)
        assertTrue(actor?.temporaryTags?.containsAll(setOf("undead", "dungeon", "scene")) == true)
    }

    @Test
    fun questDefinitionDumpIncludesScenarioActors() {
        val scenario = FeaturePackLoader.ScenarioDefinition(
            "medieval_quest",
            "Q08",
            "Patrula De Hotar",
            "Descriere",
            ScenarioType.QUEST,
        )
        scenario.questCode = "Q08"
        scenario.addObjective(
            FeaturePackLoader.QuestEntryDefinition("visit_region", "type:settlement", 1, "Patruleaza regiunea")
        )
        scenario.addReward(
            FeaturePackLoader.QuestEntryDefinition("item", "ARROW", 8, "Primesti sageti")
        )
        scenario.addValidationWarning("quest_actor_triggers.on_stage_spawn necunoscut; ignorat.")
        scenario.addActor(
            "dungeon_spirit",
            ro.ainpc.npc.NpcScenarioActorDefinition(
                id = "dungeon_spirit",
                name = "Spiritul din Cripta",
                lifecycleType = NpcLifecycleType.EPISODIC,
                persistenceMode = NpcPersistenceMode.RUNTIME_ONLY,
                simulationMode = NpcSimulationMode.NONE,
                interactionProfile = NpcInteractionProfile.SCENE_ONLY,
                entityKind = NpcEntityKind.SPIRIT,
                spawnPhase = "investigation",
                spawnPolicy = NpcSpawnPolicy.PHASE,
            ),
        )

        val dump = DebugDumpQuestDefinitionJson.buildLoadedQuestDefinitionsJson(
            listOf(scenario),
            emptyList(),
            emptyList(),
            Gson(),
        )

        assertEquals(1, dump.get("actor_count").asInt)
        assertEquals(1, dump.get("validation_warning_count").asInt)
        assertEquals(1, dump.getAsJsonArray("rows")[0].asJsonObject.get("actor_count").asInt)
        assertEquals(1, dump.getAsJsonArray("rows")[0].asJsonObject.get("validation_warning_count").asInt)
        assertTrue(
            dump.getAsJsonArray("rows")[0]
                .asJsonObject
                .getAsJsonObject("actors")
                .has("dungeon_spirit")
        )
        assertTrue(
            dump.getAsJsonArray("rows")[0]
                .asJsonObject
                .getAsJsonArray("validation_warnings")
                .any { it.asString.contains("on_stage_spawn") }
        )
    }

    @Test
    fun loadsQuestStageActorMetadata() {
        val yaml = """
            stages:
              PATROL:
                description: "Patrula"
                completion_mode: "all_objectives"
                on_stage_enter: "dungeon_spirit,forest_guard"
                on_stage_complete: "border_messenger"
                objectives:
                  - "patrol_region"
              RETURN:
                description: "Intoarcere"
                completion_mode: "manual_turn_in"
                on_stage_exit: "dungeon_spirit"
                objectives:
                  - "report_to_guard"
        """.trimIndent()
        val config = YamlConfiguration()
        config.loadFromString(yaml)

        val scenario = FeaturePackLoader.ScenarioDefinition(
            "medieval_quest",
            "Q08",
            "Patrula De Hotar",
            "Descriere",
            ScenarioType.QUEST,
        )

        FeaturePackYamlSupport.loadQuestStages(scenario, config.getConfigurationSection("stages"))

        val patrolStage = scenario.questStages.first { it.id == "PATROL" }
        val returnStage = scenario.questStages.first { it.id == "RETURN" }

        assertEquals("dungeon_spirit,forest_guard", patrolStage.metadata["on_stage_enter"])
        assertEquals("border_messenger", patrolStage.metadata["on_stage_complete"])
        assertEquals("dungeon_spirit", returnStage.metadata["on_stage_exit"])
    }

    @Test
    fun loadsQuestActorTriggersFromYamlSection() {
        val yaml = """
            quest_actor_triggers:
              on_stage_start:
                - "border_messenger"
              on_stage_finish: "dungeon_spirit|forest_guard"
        """.trimIndent()
        val config = YamlConfiguration()
        config.loadFromString(yaml)

        val scenario = FeaturePackLoader.ScenarioDefinition(
            "medieval_quest",
            "Q08",
            "Patrula De Hotar",
            "Descriere",
            ScenarioType.QUEST,
        )

        FeaturePackYamlSupport.loadQuestActorTriggers(
            scenario,
            config.getConfigurationSection("quest_actor_triggers")
        )

        assertTrue(scenario.questActorTriggers["on_stage_enter"]?.contains("border_messenger") == true)
        assertTrue(scenario.questActorTriggers["on_stage_complete"]?.containsAll(setOf("dungeon_spirit", "forest_guard")) == true)
    }

    @Test
    fun ignoresUnknownQuestActorTriggersWithValidationWarning() {
        val yaml = """
            quest_actor_triggers:
              on_stage_spawn:
                - "border_messenger"
        """.trimIndent()
        val config = YamlConfiguration()
        config.loadFromString(yaml)

        val scenario = FeaturePackLoader.ScenarioDefinition(
            "medieval_quest",
            "Q08",
            "Patrula De Hotar",
            "Descriere",
            ScenarioType.QUEST,
        )

        FeaturePackYamlSupport.loadQuestActorTriggers(
            scenario,
            config.getConfigurationSection("quest_actor_triggers")
        )

        assertFalse(scenario.questActorTriggers.containsKey("on_stage_spawn"))
        assertTrue(scenario.validationWarnings.any { it.contains("on_stage_spawn") })
    }
}
