package ro.ainpc.engine

import org.bukkit.configuration.ConfigurationSection
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
            val baseType = ScenarioEngine.ScenarioType.fromId(
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
                baseType == ScenarioEngine.ScenarioType.QUEST,
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

            val questSection = scenarioSection.getConfigurationSection("quest")
            if (questSection != null) {
                scenario.questCode = questSection.getString("code", scenarioId) ?: scenarioId
                scenario.questGiverProfession = questSection.getString("giver_profession", "") ?: ""
                scenario.questScenarioKind = questSection.getString(
                    "scenario_kind",
                    questSection.getString("kind", questSection.getString("scenario_type", "")),
                ) ?: ""
                scenario.questCategory = questSection.getString("category", "") ?: ""
                scenario.questAcceptanceMode = questSection.getString(
                    "acceptance_mode",
                    questSection.getString("offer_policy", ""),
                ) ?: ""
                scenario.questCompletionMode = questSection.getString("completion_mode", "") ?: ""
                scenario.questTrackingMode = questSection.getString("tracking_mode", "") ?: ""
                scenario.questTags = questSection.getStringList("tags")
                scenario.questPrerequisites = questSection.getStringList("prerequisites")
                scenario.isQuestRepeatable = questSection.getBoolean("repeatable", false)
                scenario.questCooldownSeconds = questSection.getLong("cooldown_seconds", 0L).coerceAtLeast(0L)
                scenario.questDialogues = loadQuestDialogues(questSection.getConfigurationSection("dialogues"))
                loadQuestStages(scenario, questSection.getConfigurationSection("stages"))

                loadQuestEntries(questSection.getConfigurationSection("objectives"), scenario::addObjective)
                loadQuestEntries(questSection.getConfigurationSection("rewards"), scenario::addReward)
            }

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
        val legacyQuestProgress = hasQuestSection || scenario.baseType == ScenarioEngine.ScenarioType.QUEST
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

    private fun questEntryValueToString(value: Any): String {
        if (value is List<*>) {
            return value.filterNotNull().joinToString(",") { it.toString() }
        }
        return value.toString()
    }
}
