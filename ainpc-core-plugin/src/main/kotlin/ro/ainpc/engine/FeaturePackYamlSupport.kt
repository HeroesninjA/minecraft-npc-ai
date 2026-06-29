package ro.ainpc.engine

import org.bukkit.configuration.ConfigurationSection
import ro.ainpc.npc.NpcEntityKind
import ro.ainpc.npc.NpcInteractionProfile
import ro.ainpc.npc.NpcLifecycleType
import ro.ainpc.npc.NpcPersistenceMode
import ro.ainpc.npc.NpcSpawnPolicy
import ro.ainpc.npc.NpcScenarioActorDefinition
import ro.ainpc.npc.NpcSimulationMode
import ro.ainpc.topology.TopologyCategory
import java.util.LinkedHashMap
import java.util.function.BiConsumer
import java.util.function.BiFunction
import java.util.function.Consumer

object FeaturePackYamlSupport {
    @JvmStatic
    fun loadTraits(
        pack: FeaturePackLoader.FeaturePack,
        section: ConfigurationSection,
        allTraits: MutableMap<String, FeaturePackLoader.TraitDefinition>,
    ) {
        for (traitId in section.getKeys(false)) {
            val traitSection = section.getConfigurationSection(traitId) ?: continue
            val name = traitSection.getString("name", traitId) ?: traitId
            val description = traitSection.getString("description", "") ?: ""
            val trait = FeaturePackLoader.TraitDefinition(traitId, name, description)

            val modifiersSection = traitSection.getConfigurationSection("modifiers")
            val actionScores = modifiersSection?.getConfigurationSection("action_scores")
            if (actionScores != null) {
                for (actionName in actionScores.getKeys(false)) {
                    trait.addActionModifier(actionName, actionScores.getInt(actionName))
                }
            }

            val emotionModifiers = modifiersSection?.getConfigurationSection("emotions")
            if (emotionModifiers != null) {
                for (emotion in emotionModifiers.getKeys(false)) {
                    trait.addEmotionModifier(emotion, emotionModifiers.getDouble(emotion))
                }
            }

            pack.addTrait(trait)
            allTraits[traitId] = trait
        }
    }

    @JvmStatic
    fun loadProfessions(
        pack: FeaturePackLoader.FeaturePack,
        section: ConfigurationSection,
        allProfessions: MutableMap<String, FeaturePackLoader.ProfessionDefinition>,
    ) {
        for (professionId in section.getKeys(false)) {
            val profSection = section.getConfigurationSection(professionId) ?: continue
            val name = profSection.getString("name", professionId) ?: professionId
            val description = profSection.getString("description", "") ?: ""
            val profession = FeaturePackLoader.ProfessionDefinition(professionId, name, description)

            val scheduleSection = profSection.getConfigurationSection("schedule")
            if (scheduleSection != null) {
                for (timeOfDay in scheduleSection.getKeys(false)) {
                    profession.addScheduleEntry(timeOfDay, scheduleSection.getString(timeOfDay))
                }
            }

            profession.workLocations = profSection.getStringList("work_locations")
            profession.tools = profSection.getStringList("tools")
            profession.aliases = profSection.getStringList("aliases")
            profession.suggestedTraits = profSection.getStringList("suggested_traits")
            profession.dialogues = profSection.getStringList("dialogues")

            pack.addProfession(profession)
            allProfessions[professionId] = profession
        }
    }

    @JvmStatic
    fun loadTopologies(
        pack: FeaturePackLoader.FeaturePack,
        section: ConfigurationSection,
        registerTopology: BiConsumer<FeaturePackLoader.FeaturePack, FeaturePackLoader.TopologyDefinition>,
    ) {
        for (topologyId in section.getKeys(false)) {
            val topologySection = section.getConfigurationSection(topologyId) ?: continue
            val category = TopologyCategory.fromId(topologySection.getString("category", topologyId) ?: topologyId)
            val topology = FeaturePackLoader.TopologyDefinition(
                pack.id,
                topologyId,
                topologySection.getString("name", topologyId) ?: topologyId,
                category,
                topologySection.getString("description", category.description) ?: category.description,
            )
            topology.biomes = topologySection.getStringList("biomes")
            topology.dialogueHints = topologySection.getStringList("dialogue_hints")
            topology.suggestedTraits = topologySection.getStringList("suggested_traits")
            registerTopology.accept(pack, topology)
        }
    }

    @JvmStatic
    fun loadDialogues(
        pack: FeaturePackLoader.FeaturePack,
        section: ConfigurationSection,
        allDialogues: MutableMap<String, List<String>>,
    ) {
        for (category in section.getKeys(false)) {
            val lines = section.getStringList(category)
            if (lines.isNotEmpty()) {
                val key = "${pack.id}:$category"
                allDialogues[key] = lines
                pack.addDialogueCategory(category, lines)
            }
        }
    }

    @JvmStatic
    fun loadProgressionMechanics(
        pack: FeaturePackLoader.FeaturePack?,
        section: ConfigurationSection?,
        registerProgressionMechanic: BiConsumer<
            FeaturePackLoader.FeaturePack,
            FeaturePackLoader.ProgressionMechanicDefinition,
        >,
    ) {
        if (pack == null || section == null) {
            return
        }

        for (mechanicId in section.getKeys(false)) {
            val mechanicSection = section.getConfigurationSection(mechanicId) ?: continue
            val mechanic = FeaturePackLoader.ProgressionMechanicDefinition(
                pack.id,
                mechanicId,
                mechanicSection.getString("kind", mechanicId),
                mechanicSection.getString("label", mechanicSection.getString("name", mechanicId)),
                mechanicSection.getString("singular_label", mechanicSection.getString("singular", mechanicId)),
                mechanicSection.getString("plural_label", mechanicSection.getString("plural", mechanicId + "s")),
                mechanicSection.getBoolean("progress", mechanicSection.getBoolean("enabled", true)),
                mechanicSection.getInt("max_active", 0).coerceAtLeast(0),
                loadProgressionMetadata(mechanicSection),
            )
            registerProgressionMechanic.accept(pack, mechanic)
        }
    }

    @JvmStatic
    fun loadScenarios(
        pack: FeaturePackLoader.FeaturePack,
        section: ConfigurationSection,
        allScenarios: MutableMap<String, FeaturePackLoader.ScenarioDefinition>,
        ensureDefaultQuestMechanic: Consumer<FeaturePackLoader.FeaturePack>,
        findProgressionMechanic: BiFunction<
            String,
            String,
            FeaturePackLoader.ProgressionMechanicDefinition?,
        >,
    ) {
        for (scenarioId in section.getKeys(false)) {
            val scenarioSection = section.getConfigurationSection(scenarioId) ?: continue
            val baseType = ScenarioType.fromId(
                scenarioSection.getString("base_type", scenarioSection.getString("type", "QUEST")) ?: "QUEST",
            )

            val scenario = FeaturePackLoader.ScenarioDefinition(
                pack.id,
                scenarioId,
                scenarioSection.getString("name", scenarioId) ?: scenarioId,
                scenarioSection.getString("description", "") ?: "",
                baseType,
            )
            scenario.triggerProbability = scenarioSection.getDouble("trigger_probability", 0.05)
            scenario.minimumNpcCount = scenarioSection.getInt("min_npcs", 2).coerceAtLeast(1)
            scenario.isRequiresPlayer = scenarioSection.getBoolean(
                "requires_player",
                baseType == ScenarioType.QUEST,
            )
            scenario.isReplaceBaseType = scenarioSection.getBoolean("replace_base_type", false)
            scenario.hint = scenarioSection.getString("hint", "") ?: ""
            scenario.preferredTopologies = scenarioSection.getStringList("preferred_topologies")
            scenario.narrativeHints = scenarioSection.getStringList("narrative_hints")
            scenario.progressionMechanicId = scenarioSection.getString("mechanic", "") ?: ""

            val rolesSection = scenarioSection.getConfigurationSection("roles")
            if (rolesSection != null) {
                for (roleId in rolesSection.getKeys(false)) {
                    val roleSection = rolesSection.getConfigurationSection(roleId) ?: continue
                    val role = FeaturePackLoader.ScenarioRoleDefinition(
                        roleId,
                        roleSection.getString("description", roleId),
                    )
                    role.isPlayerRole = roleSection.getBoolean("player_role", false)
                    role.isOptional = roleSection.getBoolean("optional", false)
                    role.requiredProfessions = roleSection.getStringList("required_professions")
                    role.preferredProfessions = roleSection.getStringList("preferred_professions")
                    role.requiredTraits = roleSection.getStringList("required_traits")
                    role.preferredTraits = roleSection.getStringList("preferred_traits")
                    scenario.addRole(role)
                }
            }

            loadScenarioPhases(scenario, scenarioSection)
            loadScenarioActors(scenario, scenarioSection.getConfigurationSection("actors"))
            loadQuestActorTriggers(scenario, scenarioSection.getConfigurationSection("quest_actor_triggers"))

            val questSection = scenarioSection.getConfigurationSection("quest")

            val importFrom = questSection?.getString("import_objectives_from", "") ?: ""
            if (importFrom.isNotBlank()) {
                val sourceScenario = allScenarios["${pack.id}:$importFrom"]
                if (sourceScenario != null) {
                    for (obj in sourceScenario.objectives) {
                        scenario.addObjective(obj)
                    }
                    for (reward in sourceScenario.rewards) {
                        scenario.addReward(reward)
                    }
                }
            }

            if (questSection != null) {
                scenario.questCode = questSection.getString("code", scenarioId) ?: scenarioId
                scenario.questGiverProfession = questSection.getString("giver_profession", "") ?: ""
                scenario.questScenarioKind = questSection.getString(
                    "scenario_kind",
                    questSection.getString("kind", questSection.getString("scenario_type", "")),
                ) ?: ""
                scenario.questCategory = questSection.getString("category", "")?.let {
                    it.ifBlank { 
                        questSection.getString("scenario_kind", questSection.getString("kind", "")) ?: ""
                    }
                } ?: ""
                scenario.questAcceptanceMode = questSection.getString(
                    "acceptance_mode",
                    questSection.getString("offer_policy", ""),
                )?.ifBlank { "auto" } ?: "auto"
                scenario.questCompletionMode = questSection.getString("completion_mode", "")?.ifBlank { "all_objectives" } ?: "all_objectives"
                scenario.questTrackingMode = questSection.getString("tracking_mode", "")?.ifBlank { "auto" } ?: "auto"
                scenario.questTags = questSection.getStringList("tags")
                scenario.questPrerequisites = questSection.getStringList("prerequisites")
                scenario.isQuestRepeatable = questSection.getBoolean("repeatable", false)
                scenario.questCooldownSeconds = questSection.getLong("cooldown_seconds", 0L).coerceAtLeast(0L)
                scenario.nextQuest = questSection.getString("next_quest", "") ?: ""
                scenario.questDialogues = loadQuestDialogues(questSection.getConfigurationSection("dialogues"))
                loadQuestStages(scenario, questSection.getConfigurationSection("stages"))

                loadQuestEntries(questSection.getConfigurationSection("objectives"), scenario::addObjective)
                loadQuestEntries(questSection.getConfigurationSection("rewards"), scenario::addReward)
            }

            loadRuntimeConditions(scenario, scenarioSection.getConfigurationSection("conditions"))
            loadRuntimeTriggers(scenario, scenarioSection.getConfigurationSection("runtime_triggers"))
            loadRuntimeActions(scenario, scenarioSection.getConfigurationSection("runtime_actions"))
            loadScenarioProgression(scenario, scenarioSection, questSection != null, findProgressionMechanic)
            if (scenario.progressionMechanicId.equals("quest", ignoreCase = true)) {
                ensureDefaultQuestMechanic.accept(pack)
            }

            pack.addScenario(scenario)
            allScenarios[pack.id + ":" + scenarioId] = scenario
        }

        if (pack.scenarios.isNotEmpty()) {
            pack.markHasScenarioDefinitions()
        }
    }

    @JvmStatic
    fun loadProgressionMetadata(section: ConfigurationSection?): Map<String, String> {
        val metadata = LinkedHashMap<String, String>()
        if (section == null) {
            return metadata
        }

        for (key in section.getKeys(false)) {
            val value = section.get(key)
            if (value == null || value is ConfigurationSection) {
                continue
            }
            if (value is List<*>) {
                metadata[key] = section.getStringList(key).joinToString(",")
            } else {
                metadata[key] = questEntryValueToString(value)
            }
        }
        return metadata
    }

    @JvmStatic
    fun loadQuestStageMetadata(stageId: String?, stageSection: ConfigurationSection?): Map<String, String> {
        val metadata = LinkedHashMap<String, String>()
        metadata["stage_id"] = stageId ?: ""
        if (stageSection == null) {
            return metadata
        }

        for (key in stageSection.getKeys(false)) {
            val value = stageSection.get(key)
            if (value == null || value is ConfigurationSection) {
                continue
            }
            if (value is List<*>) {
                metadata[key] = stageSection.getStringList(key).joinToString(",")
            } else {
                metadata[key] = questEntryValueToString(value)
            }
        }
        return metadata
    }

    @JvmStatic
    fun loadQuestEntryMetadata(entryId: String?, entrySection: ConfigurationSection?): Map<String, String> {
        val metadata = LinkedHashMap<String, String>()
        metadata["entry_id"] = entryId ?: ""
        if (entrySection == null) {
            return metadata
        }

        for (key in entrySection.getKeys(false)) {
            val value = entrySection.get(key)
            if (value == null || value is ConfigurationSection) {
                continue
            }
            metadata[key] = questEntryValueToString(value)
        }
        return metadata
    }

    @JvmStatic
    fun loadQuestEntryMap(section: ConfigurationSection?): Map<String, String> {
        val values = LinkedHashMap<String, String>()
        if (section == null) {
            return values
        }

        for (key in section.getKeys(false)) {
            val value = section.get(key)
            if (value == null || value is ConfigurationSection) {
                continue
            }
            values[key] = questEntryValueToString(value)
        }
        return values
    }

    @JvmStatic
    fun loadQuestDialogues(section: ConfigurationSection?): Map<String, List<String>> {
        val dialogues = LinkedHashMap<String, List<String>>()
        if (section == null) {
            return dialogues
        }

        for (key in section.getKeys(false)) {
            val lines = section.getStringList(key)
            if (lines.isNotEmpty()) {
                dialogues[key] = lines
                continue
            }

            val nestedSection = section.getConfigurationSection(key) ?: continue
            for (nestedKey in nestedSection.getKeys(false)) {
                val nestedLines = nestedSection.getStringList(nestedKey)
                if (nestedLines.isNotEmpty()) {
                    dialogues["$key.$nestedKey"] = nestedLines
                }
            }
        }
        return dialogues
    }

    @JvmStatic
    fun loadScenarioPhases(
        scenario: FeaturePackLoader.ScenarioDefinition,
        scenarioSection: ConfigurationSection,
    ) {
        val phasesSection = scenarioSection.getConfigurationSection("phases")
        if (phasesSection != null) {
            for (phaseId in phasesSection.getKeys(false)) {
                val description = phasesSection.getString(phaseId, phaseId) ?: phaseId
                scenario.addPhase(phaseId)
                scenario.addQuestStage(
                    FeaturePackLoader.QuestStageDefinition(
                        phaseId,
                        description,
                        "all_objectives",
                        emptyList(),
                        mapOf("source" to "phases"),
                    ),
                )
            }
            return
        }

        val phases = scenarioSection.getStringList("phases")
        scenario.phases = phases
        for (phaseId in phases) {
            scenario.addQuestStage(
                FeaturePackLoader.QuestStageDefinition(
                    phaseId,
                    phaseId,
                    "all_objectives",
                    emptyList(),
                    mapOf("source" to "phases"),
                ),
            )
        }
    }

    @JvmStatic
    fun loadScenarioActors(
        scenario: FeaturePackLoader.ScenarioDefinition,
        actorsSection: ConfigurationSection?,
    ) {
        if (actorsSection == null) {
            return
        }

        for (actorId in actorsSection.getKeys(false)) {
            val actorSection = actorsSection.getConfigurationSection(actorId) ?: continue
            val actor = NpcScenarioActorDefinition(
                id = actorId,
                name = actorSection.getString("name", actorId) ?: actorId,
                lifecycleType = readLifecycleType(actorSection.getString("lifecycle_type"), NpcLifecycleType.TEMPORARY),
                persistenceMode = readPersistenceMode(actorSection.getString("persistence_mode"), NpcPersistenceMode.LIGHT),
                simulationMode = readSimulationMode(actorSection.getString("simulation_mode"), NpcSimulationMode.LIGHT),
                interactionProfile = readInteractionProfile(actorSection.getString("interaction_profile"), NpcInteractionProfile.MINIMAL),
                entityKind = readEntityKind(actorSection.getString("entity_kind"), NpcEntityKind.VILLAGER),
                entityArchetype = actorSection.getString("entity_archetype", "") ?: "",
                ownerScenarioId = actorSection.getString("owner_scenario_id", "") ?: "",
                ownerQuestId = actorSection.getString("owner_quest_id", "") ?: "",
                spawnSource = actorSection.getString("spawn_source", "") ?: "",
                spawnPhase = actorSection.getString("spawn_phase", "") ?: "",
                spawnPolicy = readSpawnPolicy(actorSection.getString("spawn_policy"), NpcSpawnPolicy.AUTO),
                despawnRule = actorSection.getString("despawn_rule", "") ?: "",
                durationSeconds = if (actorSection.contains("duration_seconds")) actorSection.getLong("duration_seconds") else null,
                temporaryTags = actorSection.getStringList("temporary_tags").toSet(),
            )
            scenario.addActor(actorId, actor)
        }
    }

    @JvmStatic
    fun loadQuestActorTriggers(
        scenario: FeaturePackLoader.ScenarioDefinition,
        triggersSection: ConfigurationSection?,
    ) {
        if (triggersSection == null) {
            return
        }

        for (triggerId in triggersSection.getKeys(false)) {
            val rawTrigger = triggersSection.get(triggerId)
            val normalizedTriggerId = QuestActorTriggers.normalize(triggerId)
            if (normalizedTriggerId.isNullOrBlank()) {
                continue
            }
            if (!QuestActorTriggers.isSupported(normalizedTriggerId)) {
                scenario.addValidationWarning("quest_actor_triggers.$triggerId necunoscut; ignorat.")
                continue
            }
            val actorIds = when (rawTrigger) {
                is List<*> -> rawTrigger.mapNotNull { it?.toString() }
                is String -> rawTrigger.split(',', ';', '|')
                is ConfigurationSection -> rawTrigger.getStringList("actors")
                else -> emptyList()
            }
            scenario.addQuestActorTrigger(normalizedTriggerId, actorIds)
        }
    }

    private fun readLifecycleType(rawValue: String?, fallback: NpcLifecycleType): NpcLifecycleType =
        enumValue(rawValue, fallback, NpcLifecycleType.values())

    private fun readPersistenceMode(rawValue: String?, fallback: NpcPersistenceMode): NpcPersistenceMode =
        enumValue(rawValue, fallback, NpcPersistenceMode.values())

    private fun readSimulationMode(rawValue: String?, fallback: NpcSimulationMode): NpcSimulationMode =
        enumValue(rawValue, fallback, NpcSimulationMode.values())

    private fun readInteractionProfile(rawValue: String?, fallback: NpcInteractionProfile): NpcInteractionProfile =
        enumValue(rawValue, fallback, NpcInteractionProfile.values())

    private fun readEntityKind(rawValue: String?, fallback: NpcEntityKind): NpcEntityKind =
        enumValue(rawValue, fallback, NpcEntityKind.values())

    private fun readSpawnPolicy(rawValue: String?, fallback: NpcSpawnPolicy): NpcSpawnPolicy =
        enumValue(rawValue, fallback, NpcSpawnPolicy.values())

    private fun <T : Enum<T>> enumValue(rawValue: String?, fallback: T, values: Array<T>): T {
        val normalized = rawValue?.trim().orEmpty()
        if (normalized.isBlank()) {
            return fallback
        }
        return values.firstOrNull { candidate -> candidate.name.equals(normalized, ignoreCase = true) } ?: fallback
    }

    @JvmStatic
    fun loadQuestStages(
        scenario: FeaturePackLoader.ScenarioDefinition?,
        stagesSection: ConfigurationSection?,
    ) {
        if (scenario == null || stagesSection == null) {
            return
        }

        for (stageId in stagesSection.getKeys(false)) {
            val stageSection = stagesSection.getConfigurationSection(stageId) ?: continue
            scenario.addQuestStage(
                FeaturePackLoader.QuestStageDefinition(
                    stageId,
                    stageSection.getString("description", stageSection.getString("name", stageId)) ?: stageId,
                    stageSection.getString("completion_mode", stageSection.getString("complete_when", "all_objectives"))
                        ?: "all_objectives",
                    stageSection.getStringList("objectives"),
                    loadQuestStageMetadata(stageId, stageSection),
                ),
            )
            if (scenario.phases.none { phase -> phase.equals(stageId, ignoreCase = true) }) {
                scenario.addPhase(stageId)
            }
        }
    }

    @JvmStatic
    fun loadQuestEntries(
        section: ConfigurationSection?,
        consumer: Consumer<FeaturePackLoader.QuestEntryDefinition>?,
    ) {
        if (section == null || consumer == null) {
            return
        }

        for (entryId in section.getKeys(false)) {
            val entrySection = section.getConfigurationSection(entryId) ?: continue
            consumer.accept(
                FeaturePackLoader.QuestEntryDefinition(
                    entrySection.getString("type", "item"),
                    entrySection.getString("item", entryId),
                    entrySection.getInt("amount", 1).coerceAtLeast(1),
                    entrySection.getString("description", ""),
                    loadQuestEntryMetadata(entryId, entrySection),
                    loadQuestEntryMap(entrySection.getConfigurationSection("variables")),
                    loadQuestEntryMap(entrySection.getConfigurationSection("payload")),
                ),
            )
        }
    }

    @JvmStatic
    fun loadScenarioProgression(
        scenario: FeaturePackLoader.ScenarioDefinition?,
        scenarioSection: ConfigurationSection?,
        hasQuestSection: Boolean,
        findProgressionMechanic: BiFunction<
            String,
            String,
            FeaturePackLoader.ProgressionMechanicDefinition?,
        >,
    ) {
        if (scenario == null || scenarioSection == null) {
            return
        }

        var progressSection = scenarioSection.getConfigurationSection("progress")
        if (progressSection == null) {
            progressSection = scenarioSection.getConfigurationSection("progression")
        }

        val hasProgressSection = progressSection != null
        val legacyQuestProgress = hasQuestSection || scenario.baseType == ScenarioType.QUEST
        scenario.isProgressionEnabled = if (hasProgressSection) {
            progressSection.getBoolean("enabled", progressSection.getBoolean("progress", true))
        } else {
            legacyQuestProgress
        }

        if (hasProgressSection) {
            scenario.progressionMechanicId = FeaturePackSupport.firstNonBlank(
                progressSection.getString("mechanic", ""),
                progressSection.getString("mechanic_id", ""),
                scenario.progressionMechanicId,
            )
            scenario.progressionKind = FeaturePackSupport.firstNonBlank(
                progressSection.getString("kind", ""),
                progressSection.getString("type", ""),
                scenario.progressionKind,
            )
            scenario.progressionLabel = FeaturePackSupport.firstNonBlank(
                progressSection.getString("label", ""),
                progressSection.getString("display_name", ""),
            )
            scenario.progressionSingularLabel = FeaturePackSupport.firstNonBlank(
                progressSection.getString("singular_label", ""),
                progressSection.getString("singular", ""),
            )
            scenario.progressionPluralLabel = FeaturePackSupport.firstNonBlank(
                progressSection.getString("plural_label", ""),
                progressSection.getString("plural", ""),
            )
            scenario.progressionMaxActive = progressSection.getInt("max_active", 0).coerceAtLeast(0)
        }

        if (scenario.progressionMechanicId.isBlank() && legacyQuestProgress) {
            scenario.progressionMechanicId = "quest"
        }
        applyProgressionMechanicDefaults(scenario, findProgressionMechanic)
        if (scenario.progressionKind.isBlank() && legacyQuestProgress) {
            scenario.progressionKind = FeaturePackSupport.firstNonBlank(scenario.questScenarioKind, "quest")
        }
        if (scenario.progressionLabel.isBlank() && legacyQuestProgress) {
            scenario.progressionLabel = "Quest"
        }
    }

    private fun applyProgressionMechanicDefaults(
        scenario: FeaturePackLoader.ScenarioDefinition,
        findProgressionMechanic: BiFunction<
            String,
            String,
            FeaturePackLoader.ProgressionMechanicDefinition?,
        >,
    ) {
        if (scenario.progressionMechanicId.isBlank()) {
            return
        }

        val mechanic = findProgressionMechanic.apply(scenario.packId, scenario.progressionMechanicId) ?: return
        if (scenario.progressionKind.isBlank()) {
            scenario.progressionKind = mechanic.kind
        }
        if (scenario.progressionLabel.isBlank()) {
            scenario.progressionLabel = mechanic.label
        }
        if (scenario.progressionSingularLabel.isBlank()) {
            scenario.progressionSingularLabel = mechanic.singularLabel
        }
        if (scenario.progressionPluralLabel.isBlank()) {
            scenario.progressionPluralLabel = mechanic.pluralLabel
        }
        if (scenario.progressionMaxActive == 0) {
            scenario.progressionMaxActive = mechanic.maxActive
        }
    }

    @JvmStatic
    fun loadRuntimeConditions(
        scenario: FeaturePackLoader.ScenarioDefinition?,
        conditionsSection: ConfigurationSection?,
    ) {
        if (scenario == null || conditionsSection == null) return
        for (condId in conditionsSection.getKeys(false)) {
            val condSection = conditionsSection.getConfigurationSection(condId) ?: continue
            val type = condSection.getString("type", "") ?: ""
            if (type.isBlank()) {
                scenario.addValidationWarning("condition.$condId nu are 'type'; ignorat.")
                continue
            }
            val params = LinkedHashMap<String, String>()
            for (key in condSection.getKeys(false)) {
                if (key == "type") continue
                val value = condSection.get(key)
                if (value != null && value !is ConfigurationSection) {
                    params[key] = questEntryValueToString(value)
                }
            }
            scenario.addCondition(condId, type, params)
        }
    }

    @JvmStatic
    fun loadRuntimeTriggers(
        scenario: FeaturePackLoader.ScenarioDefinition?,
        triggersSection: ConfigurationSection?,
    ) {
        if (scenario == null || triggersSection == null) return
        for (triggerId in triggersSection.getKeys(false)) {
            val triggerSection = triggersSection.getConfigurationSection(triggerId) ?: continue
            val type = triggerSection.getString("type", "") ?: ""
            if (type.isBlank()) {
                scenario.addValidationWarning("runtime_trigger.$triggerId nu are 'type'; ignorat.")
                continue
            }
            val params = LinkedHashMap<String, String>()
            val actionRefs = mutableListOf<String>()
            for (key in triggerSection.getKeys(false)) {
                if (key == "type") continue
                val value = triggerSection.get(key)
                if (key == "actions" && value is List<*>) {
                    @Suppress("UNCHECKED_CAST")
                    for (entry in value as List<Map<String, Any>>) {
                        val ref = entry["id"]?.toString()
                        if (!ref.isNullOrBlank()) actionRefs.add(ref)
                        val entryType = entry["type"]?.toString() ?: continue
                        if (entryType.isBlank()) continue
                        val entryParams = LinkedHashMap<String, String>()
                        for ((ek, ev) in entry) {
                            if (ek == "id" || ek == "type") continue
                            entryParams[ek] = ev.toString()
                        }
                        scenario.addRuntimeAction(triggerId + "_" + ref.orEmpty(), entryType, entryParams)
                    }
                    continue
                }
                if (value != null && value !is ConfigurationSection) {
                    params[key] = questEntryValueToString(value)
                }
            }
            if (actionRefs.isNotEmpty()) {
                params["action_refs"] = actionRefs.joinToString(",")
            }
            scenario.addRuntimeTrigger(triggerId, type, params)
        }
    }

    @JvmStatic
    fun loadRuntimeActions(
        scenario: FeaturePackLoader.ScenarioDefinition?,
        actionsSection: ConfigurationSection?,
    ) {
        if (scenario == null || actionsSection == null) return
        for (actionId in actionsSection.getKeys(false)) {
            val actionSection = actionsSection.getConfigurationSection(actionId) ?: continue
            val type = actionSection.getString("type", "") ?: ""
            if (type.isBlank()) {
                scenario.addValidationWarning("runtime_action.$actionId nu are 'type'; ignorat.")
                continue
            }
            val params = LinkedHashMap<String, String>()
            for (key in actionSection.getKeys(false)) {
                if (key == "type") continue
                val value = actionSection.get(key)
                if (value != null && value !is ConfigurationSection) {
                    params[key] = questEntryValueToString(value)
                }
            }
            scenario.addRuntimeAction(actionId, type, params)
        }
    }

    private fun questEntryValueToString(value: Any): String {
        if (value is List<*>) {
            return value.filterNotNull().joinToString(",") { it.toString() }
        }
        return value.toString()
    }
}
