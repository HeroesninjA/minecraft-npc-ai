package ro.ainpc.debug

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import ro.ainpc.AINPCPlugin
import ro.ainpc.engine.FeaturePackLoader
import ro.ainpc.engine.QuestScenarioContract
import ro.ainpc.engine.formatRewardLabel
import ro.ainpc.progression.ProgressionDefinition

object DebugDumpQuestDefinitionJson {
    @JvmStatic
    fun buildLoadedQuestDefinitionsJson(plugin: AINPCPlugin, gson: Gson): JsonObject {
        val featurePackLoader = runCatching { plugin.featurePackLoader }.getOrNull()
        val progressionService = runCatching { plugin.progressionService }.getOrNull()
        return buildLoadedQuestDefinitionsJson(
            featurePackLoader?.getAllScenarios(),
            progressionService?.getDefinitions(),
            featurePackLoader?.getAllProgressionMechanics(),
            gson,
        )
    }

    @JvmStatic
    fun buildLoadedQuestDefinitionsJson(
        scenarios: Collection<FeaturePackLoader.ScenarioDefinition>?,
        progressionDefinitions: Collection<ProgressionDefinition>?,
        progressionMechanics: Collection<FeaturePackLoader.ProgressionMechanicDefinition>?,
        gson: Gson,
    ): JsonObject {
        val root = JsonObject()
        root.addProperty("source", "FeaturePackLoader#getAllScenarios")
        if (scenarios == null) {
            root.addProperty("available", false)
            root.addProperty("error", "FeaturePackLoader indisponibil")
            root.addProperty("scenario_count", 0)
            root.addProperty("quest_count", 0)
            root.addProperty("actor_count", 0)
            root.addProperty("validation_warning_count", 0)
            root.add("rows", JsonArray())
            return root
        }

        root.addProperty("available", true)
        val sortedScenarios = scenarios.sortedWith(
            compareBy<FeaturePackLoader.ScenarioDefinition>(
                { scenario -> DebugDumpSupport.valueOrEmpty(scenario.packId) },
                { scenario -> DebugDumpSupport.valueOrEmpty(scenario.id) },
            ),
        )

        val rows = JsonArray()
        val byPack = LinkedHashMap<String, Int>()
        val byCategory = LinkedHashMap<String, Int>()
        val byKind = LinkedHashMap<String, Int>()
        val byMechanic = LinkedHashMap<String, Int>()
        var actorCount = 0
        var validationWarningCount = 0

        for (scenario in sortedScenarios) {
            if (!isLoadedQuestDefinitionCandidate(scenario)) {
                continue
            }

            val contract = QuestScenarioContract.fromScenarioDefinition(scenario)
            rows.add(loadedQuestDefinitionRowJson(scenario, contract, gson))
            actorCount += scenario.actors.size
            validationWarningCount += scenario.validationWarnings.size
            DebugDumpSupport.incrementCount(byPack, scenario.packId)
            DebugDumpSupport.incrementCount(byCategory, DebugDumpSupport.enumJsonId(contract.category()))
            DebugDumpSupport.incrementCount(byKind, DebugDumpSupport.enumJsonId(contract.kind()))
            DebugDumpSupport.incrementCount(
                byMechanic,
                DebugDumpSupport.valueOrFallback(scenario.progressionMechanicId, "quest"),
            )
        }

        root.addProperty("scenario_count", sortedScenarios.size)
        root.addProperty("quest_count", rows.size())
        root.addProperty("actor_count", actorCount)
        root.addProperty("validation_warning_count", validationWarningCount)
        root.addProperty("progression_mechanic_count", progressionMechanics?.size ?: 0)
        root.addProperty("progression_definition_count", progressionDefinitions?.size ?: 0)
        root.add("by_pack", DebugDumpSupport.countMapJson(byPack))
        root.add("by_category", DebugDumpSupport.countMapJson(byCategory))
        root.add("by_kind", DebugDumpSupport.countMapJson(byKind))
        root.add("by_mechanic", DebugDumpSupport.countMapJson(byMechanic))
        root.add(
            "progression_mechanics",
            progressionMechanicsJson(progressionMechanics, gson)
        )
        root.add("progression_definitions", progressionDefinitionsJson(progressionDefinitions))
        root.add("rows", rows)
        return root
    }

    @JvmStatic
    fun isLoadedQuestDefinitionCandidate(scenario: FeaturePackLoader.ScenarioDefinition?): Boolean =
        ProgressionDefinition.isProgressionCandidate(scenario)

    private fun loadedQuestDefinitionRowJson(
        scenario: FeaturePackLoader.ScenarioDefinition,
        contract: QuestScenarioContract,
        gson: Gson,
    ): JsonObject {
        val json = JsonObject()
        json.addProperty("pack_id", DebugDumpSupport.valueOrEmpty(scenario.packId))
        json.addProperty("id", DebugDumpSupport.valueOrEmpty(scenario.id))
        json.addProperty("template_id", DebugDumpSupport.questTemplateId(scenario))
        json.addProperty("name", DebugDumpSupport.valueOrEmpty(scenario.name))
        json.addProperty("description", DebugDumpSupport.valueOrEmpty(scenario.description))
        json.addProperty("base_type", scenario.baseType.name)
        json.addProperty("quest_code", DebugDumpSupport.valueOrEmpty(scenario.questCode))
        json.addProperty("giver_profession", DebugDumpSupport.valueOrEmpty(scenario.questGiverProfession))
        json.addProperty("category", DebugDumpSupport.valueOrEmpty(scenario.questCategory))
        json.addProperty("kind", DebugDumpSupport.valueOrEmpty(scenario.questScenarioKind))
        json.addProperty("acceptance_mode", DebugDumpSupport.valueOrEmpty(scenario.questAcceptanceMode))
        json.addProperty("completion_mode", DebugDumpSupport.valueOrEmpty(scenario.questCompletionMode))
        json.addProperty("tracking_mode", DebugDumpSupport.valueOrEmpty(scenario.questTrackingMode))
        json.addProperty("progression_enabled", scenario.isProgressionEnabled)
        json.addProperty("progression_mechanic", DebugDumpSupport.valueOrEmpty(scenario.progressionMechanicId))
        json.addProperty("progression_kind", DebugDumpSupport.valueOrEmpty(scenario.progressionKind))
        json.addProperty("progression_label", DebugDumpSupport.valueOrEmpty(scenario.progressionLabel))
        json.addProperty("progression_singular_label", DebugDumpSupport.valueOrEmpty(scenario.progressionSingularLabel))
        json.addProperty("progression_plural_label", DebugDumpSupport.valueOrEmpty(scenario.progressionPluralLabel))
        json.addProperty("progression_max_active", scenario.progressionMaxActive)
        json.addProperty("repeatable", scenario.isQuestRepeatable)
        json.addProperty("cooldown_seconds", scenario.questCooldownSeconds)
        json.addProperty("requires_player", scenario.isRequiresPlayer)
        json.addProperty("replace_base_type", scenario.isReplaceBaseType)
        json.addProperty("trigger_probability", scenario.triggerProbability)
        json.addProperty("minimum_npc_count", scenario.minimumNpcCount)
        json.addProperty("hint", DebugDumpSupport.valueOrEmpty(scenario.hint))
        json.add("effective_contract", questContractJson(contract, gson))
        json.add("tags", gson.toJsonTree(scenario.questTags))
        json.add("prerequisites", gson.toJsonTree(scenario.questPrerequisites))
        json.add("phases", gson.toJsonTree(scenario.phases))
        json.add("stages", DebugDumpSupport.questStagesJson(scenario.questStages, gson))
        json.add("objective_tree", buildObjectiveTree(scenario, gson))
        json.add("preferred_topologies", gson.toJsonTree(scenario.preferredTopologies))
        json.add("narrative_hints", gson.toJsonTree(scenario.narrativeHints))
        json.add("roles", scenarioRolesJson(scenario.roles, gson))
        json.addProperty("actor_count", scenario.actors.size)
        json.add("actors", scenarioActorsJson(scenario.actors, gson))
        json.addProperty("quest_actor_trigger_count", scenario.questActorTriggers.size)
        json.add("quest_actor_triggers", scenarioActorTriggersJson(scenario.questActorTriggers, gson))
        json.addProperty("validation_warning_count", scenario.validationWarnings.size)
        json.add("validation_warnings", gson.toJsonTree(scenario.validationWarnings))
        json.add("objectives", questEntriesJson(scenario.objectives, true, gson))
        json.add("rewards", questEntriesJson(scenario.rewards, false, gson))
        json.add("dialogues", gson.toJsonTree(scenario.questDialogues))
        return json
    }

    private fun buildObjectiveTree(
        scenario: FeaturePackLoader.ScenarioDefinition,
        gson: Gson,
    ): JsonObject {
        val tree = JsonObject()
        val objectivesByStage = LinkedHashMap<String, JsonArray>()
        objectivesByStage["(no stage)"] = JsonArray()

        for ((index, objective) in scenario.objectives.withIndex()) {
            val stage = DebugDumpSupport.questEntryStage(objective)
            val stageKey = if (stage.isNotBlank() && scenario.questStages.any { DebugDumpSupport.normalizeKey(it.id) == DebugDumpSupport.normalizeKey(stage) }) {
                stage
            } else {
                "(no stage)"
            }
            val arr = objectivesByStage.getOrPut(stageKey) { JsonArray() }
            val entry = JsonObject()
            entry.addProperty("index", index)
            entry.addProperty("id", DebugDumpSupport.valueOrEmpty(objective.entryId))
            entry.addProperty("type", DebugDumpSupport.normalizeQuestObjectiveType(objective.type))
            entry.addProperty("target", DebugDumpSupport.valueOrEmpty(objective.itemId))
            entry.addProperty("amount", objective.amount)
            arr.add(entry)
        }

        for ((stageKey, objectives) in objectivesByStage) {
            val stageNode = JsonObject()
            stageNode.addProperty("stage_id", stageKey)
            stageNode.add("objectives", objectives)
            val stageDef = scenario.questStages.find { DebugDumpSupport.normalizeKey(it.id) == DebugDumpSupport.normalizeKey(stageKey) }
            if (stageDef != null) {
                stageNode.addProperty("completion_mode", stageDef.completionMode)
                val nextStage = stageDef.getNextStageId()
                if (nextStage.isNotBlank()) stageNode.addProperty("next_stage", nextStage)
            }
            tree.add(stageKey, stageNode)
        }
        return tree
    }

    private fun progressionMechanicsJson(
        mechanics: Collection<FeaturePackLoader.ProgressionMechanicDefinition>?,
        gson: Gson,
    ): JsonArray {
        val json = JsonArray()
        if (mechanics.isNullOrEmpty()) {
            return json
        }

        mechanics.sortedWith(
            compareBy<FeaturePackLoader.ProgressionMechanicDefinition>(
                { mechanic -> DebugDumpSupport.valueOrEmpty(mechanic.packId) },
                { mechanic -> DebugDumpSupport.valueOrEmpty(mechanic.id) },
            ),
        ).forEach { mechanic -> json.add(progressionMechanicJson(mechanic, gson)) }
        return json
    }

    private fun progressionDefinitionsJson(definitions: Collection<ProgressionDefinition>?): JsonArray {
        val json = JsonArray()
        if (definitions.isNullOrEmpty()) {
            return json
        }

        for (definition in definitions) {
            json.add(progressionDefinitionJson(definition))
        }
        return json
    }

    private fun progressionDefinitionJson(definition: ProgressionDefinition?): JsonObject {
        val json = JsonObject()
        if (definition == null) {
            return json
        }

        json.addProperty("progression_id", definition.progressionId())
        json.addProperty("pack_id", definition.packId())
        json.addProperty("mechanic_id", definition.mechanicId())
        json.addProperty("kind", definition.kind())
        json.addProperty("definition_id", definition.definitionId())
        json.addProperty("template_id", definition.templateId())
        json.addProperty("code", definition.code())
        json.addProperty("display_name", definition.displayName())
        json.addProperty("description", definition.description())
        json.addProperty("category", definition.category())
        json.addProperty("scenario_kind", definition.scenarioKind())
        json.addProperty("base_type", definition.baseType())
        json.addProperty("label", definition.label())
        json.addProperty("singular_label", definition.singularLabel())
        json.addProperty("plural_label", definition.pluralLabel())
        json.addProperty("max_active", definition.maxActive())
        json.addProperty("objective_count", definition.objectiveCount())
        json.addProperty("stage_count", definition.stageCount())
        json.addProperty("reward_count", definition.rewardCount())
        json.addProperty("repeatable", definition.repeatable())
        json.addProperty("enabled", definition.enabled())
        return json
    }

    private fun progressionMechanicJson(
        mechanic: FeaturePackLoader.ProgressionMechanicDefinition?,
        gson: Gson,
    ): JsonObject {
        val json = JsonObject()
        if (mechanic == null) {
            return json
        }

        json.addProperty("pack_id", DebugDumpSupport.valueOrEmpty(mechanic.packId))
        json.addProperty("id", DebugDumpSupport.valueOrEmpty(mechanic.id))
        json.addProperty("kind", DebugDumpSupport.valueOrEmpty(mechanic.kind))
        json.addProperty("label", DebugDumpSupport.valueOrEmpty(mechanic.label))
        json.addProperty("singular_label", DebugDumpSupport.valueOrEmpty(mechanic.singularLabel))
        json.addProperty("plural_label", DebugDumpSupport.valueOrEmpty(mechanic.pluralLabel))
        json.addProperty("progress_enabled", mechanic.isProgressEnabled)
        json.addProperty("max_active", mechanic.maxActive)
        json.add("metadata", gson.toJsonTree(mechanic.metadata))
        return json
    }

    private fun questContractJson(contract: QuestScenarioContract, gson: Gson): JsonObject {
        val json = JsonObject()
        json.addProperty("kind", DebugDumpSupport.enumJsonId(contract.kind()))
        json.addProperty("category", DebugDumpSupport.enumJsonId(contract.category()))
        json.addProperty("category_display_name", contract.categoryDisplayName())
        json.addProperty("acceptance_mode", DebugDumpSupport.enumJsonId(contract.acceptanceMode()))
        json.addProperty("completion_mode", DebugDumpSupport.enumJsonId(contract.completionMode()))
        json.addProperty("tracking_mode", DebugDumpSupport.enumJsonId(contract.trackingMode()))
        json.addProperty("auto_accept_on_offer", contract.autoAcceptOnOffer())
        json.add("tags", gson.toJsonTree(contract.tags()))
        return json
    }

    private fun scenarioRolesJson(
        roles: Map<String, FeaturePackLoader.ScenarioRoleDefinition>?,
        gson: Gson,
    ): JsonObject {
        val json = JsonObject()
        if (roles.isNullOrEmpty()) {
            return json
        }

        roles.entries
            .sortedBy { entry -> DebugDumpSupport.valueOrEmpty(entry.key) }
            .forEach { entry ->
                json.add(
                    DebugDumpSupport.valueOrEmpty(entry.key),
                    scenarioRoleJson(entry.value, gson)
                )
            }
        return json
    }

    private fun scenarioRoleJson(role: FeaturePackLoader.ScenarioRoleDefinition?, gson: Gson): JsonObject {
        val json = JsonObject()
        if (role == null) {
            return json
        }

        json.addProperty("id", DebugDumpSupport.valueOrEmpty(role.id))
        json.addProperty("description", DebugDumpSupport.valueOrEmpty(role.description))
        json.addProperty("player_role", role.isPlayerRole)
        json.addProperty("optional", role.isOptional)
        json.add("required_professions", gson.toJsonTree(role.requiredProfessions))
        json.add("preferred_professions", gson.toJsonTree(role.preferredProfessions))
        json.add("required_traits", gson.toJsonTree(role.requiredTraits))
        json.add("preferred_traits", gson.toJsonTree(role.preferredTraits))
        return json
    }

    private fun scenarioActorsJson(
        actors: Map<String, ro.ainpc.npc.NpcScenarioActorDefinition>?,
        gson: Gson,
    ): JsonObject {
        val json = JsonObject()
        if (actors.isNullOrEmpty()) {
            return json
        }

        actors.entries
            .sortedBy { entry -> DebugDumpSupport.valueOrEmpty(entry.key) }
            .forEach { entry ->
                json.add(DebugDumpSupport.valueOrEmpty(entry.key), scenarioActorJson(entry.value, gson))
            }
        return json
    }

    private fun scenarioActorJson(actor: ro.ainpc.npc.NpcScenarioActorDefinition?, gson: Gson): JsonObject {
        val json = JsonObject()
        if (actor == null) {
            return json
        }

        json.addProperty("id", DebugDumpSupport.valueOrEmpty(actor.id))
        json.addProperty("name", DebugDumpSupport.valueOrEmpty(actor.name))
        json.addProperty("lifecycle_type", actor.lifecycleType.name)
        json.addProperty("persistence_mode", actor.persistenceMode.name)
        json.addProperty("simulation_mode", actor.simulationMode.name)
        json.addProperty("interaction_profile", actor.interactionProfile.name)
        json.addProperty("entity_kind", actor.entityKind.name)
        json.addProperty("entity_archetype", DebugDumpSupport.valueOrEmpty(actor.entityArchetype))
        json.addProperty("owner_scenario_id", DebugDumpSupport.valueOrEmpty(actor.ownerScenarioId))
        json.addProperty("owner_quest_id", DebugDumpSupport.valueOrEmpty(actor.ownerQuestId))
        json.addProperty("spawn_source", DebugDumpSupport.valueOrEmpty(actor.spawnSource))
        json.addProperty("spawn_phase", DebugDumpSupport.valueOrEmpty(actor.spawnPhase))
        json.addProperty("spawn_policy", actor.spawnPolicy.name)
        json.addProperty("despawn_rule", DebugDumpSupport.valueOrEmpty(actor.despawnRule))
        json.addProperty("duration_seconds", actor.durationSeconds ?: 0L)
        json.add("temporary_tags", gson.toJsonTree(actor.temporaryTags.toList()))
        return json
    }

    private fun questEntriesJson(
        entries: List<FeaturePackLoader.QuestEntryDefinition>?,
        objectiveEntries: Boolean,
        gson: Gson,
    ): JsonArray {
        val json = JsonArray()
        if (entries.isNullOrEmpty()) {
            return json
        }

        for (index in entries.indices) {
            json.add(questEntryJson(entries[index], index, objectiveEntries, gson))
        }
        return json
    }

    private fun scenarioActorTriggersJson(
        triggers: Map<String, MutableSet<String>>?,
        gson: Gson,
    ): JsonObject {
        val json = JsonObject()
        if (triggers.isNullOrEmpty()) {
            return json
        }

        triggers.entries
            .sortedBy { entry -> DebugDumpSupport.valueOrEmpty(entry.key) }
            .forEach { entry ->
                json.add(entry.key, gson.toJsonTree(entry.value.toList()))
            }
        return json
    }

    private fun questEntryJson(
        entry: FeaturePackLoader.QuestEntryDefinition?,
        index: Int,
        objectiveEntry: Boolean,
        gson: Gson,
    ): JsonObject {
        val json = JsonObject()
        json.addProperty("index", index)
        if (entry == null) {
            return json
        }

        val type = DebugDumpSupport.valueOrEmpty(entry.type)
        val itemId = DebugDumpSupport.valueOrEmpty(entry.itemId)
        val normalizedType = if (objectiveEntry) {
            DebugDumpSupport.normalizeQuestObjectiveType(type)
        } else {
            DebugDumpSupport.normalizeQuestRewardType(type)
        }
        val semanticAnchorType = if (objectiveEntry) {
            DebugDumpSupport.semanticAnchorTypeForObjective(normalizedType)
        } else {
            ""
        }

        json.addProperty("entry_id", DebugDumpSupport.valueOrEmpty(entry.entryId))
        json.addProperty("type", type)
        json.addProperty("normalized_type", normalizedType)
        json.addProperty("item", itemId)
        json.addProperty("semantic_anchor_type", semanticAnchorType)
        json.addProperty("semantic_reference", if (semanticAnchorType.isBlank()) "" else itemId)
        json.addProperty(
            "semantic_reference_prefix",
            if (semanticAnchorType.isBlank()) "" else DebugDumpSupport.semanticReferencePrefix(itemId),
        )
        json.addProperty(
            "semantic_reference_value",
            if (semanticAnchorType.isBlank()) "" else DebugDumpSupport.semanticReferenceValue(itemId),
        )
        json.addProperty("amount", entry.amount)
        json.addProperty("description", DebugDumpSupport.valueOrEmpty(entry.description))
        if (!objectiveEntry) {
            json.addProperty("formatted_label", formatRewardLabel(entry))
        }
        json.add("metadata", gson.toJsonTree(entry.metadata))
        json.add("variables", gson.toJsonTree(entry.variables))
        json.add("payload", gson.toJsonTree(entry.payload))
        return json
    }
}
