package ro.ainpc.engine

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import ro.ainpc.AINPCPlugin
import ro.ainpc.addons.AddonDescriptor
import ro.ainpc.addons.AddonType
import ro.ainpc.npc.NPCAction
import ro.ainpc.platform.RuntimeMode
import ro.ainpc.topology.TopologyCategory
import ro.ainpc.topology.TopologyConsensus
import java.io.File
import java.util.Collections
import java.util.EnumMap
import java.util.EnumSet
import java.util.LinkedHashMap
import java.util.Locale

/**
 * Incarca Feature Packs din fisiere YAML.
 * Feature Packs definesc traits, profesii, dialoguri, scenarii si metadata.
 */
class FeaturePackLoader(private val plugin: AINPCPlugin) {
    private val loadedPacks: MutableMap<String, FeaturePack> = LinkedHashMap()
    private val allTraits: MutableMap<String, TraitDefinition> = HashMap()
    private val allProfessions: MutableMap<String, ProfessionDefinition> = HashMap()
    private val allDialogues: MutableMap<String, List<String>> = HashMap()
    private val allTopologies: MutableMap<String, TopologyDefinition> = HashMap()
    private val topologiesByCategory: MutableMap<TopologyCategory, MutableList<TopologyDefinition>> =
        EnumMap(TopologyCategory::class.java)
    private val allScenarios: MutableMap<String, ScenarioDefinition> = LinkedHashMap()
    private val allProgressionMechanics: MutableMap<String, ProgressionMechanicDefinition> = LinkedHashMap()

    fun loadAllPacks() {
        loadedPacks.clear()
        allTraits.clear()
        allProfessions.clear()
        allDialogues.clear()
        allTopologies.clear()
        topologiesByCategory.clear()
        allScenarios.clear()
        allProgressionMechanics.clear()
        plugin.platform.addonRegistry.removeByOrigin(AddonDescriptor.ORIGIN_FEATURE_PACK)

        val packsFolder = File(plugin.dataFolder, "packs")
        if (!packsFolder.exists()) {
            packsFolder.mkdirs()
        }

        saveDefaultPacks()

        val files = mutableListOf<File>()
        collectPackFiles(
            packsFolder,
            packsFolder,
            files,
            plugin.config.getBoolean("feature_packs.allow_addon_packs", true),
        )
        files.sortWith(Comparator.comparing({ file -> relativizePackPath(packsFolder, file) }, String.CASE_INSENSITIVE_ORDER))
        val candidatePackIds = if (shouldValidatePackMetadata()) {
            collectCandidatePackIds(files)
        } else {
            emptySet()
        }
        for (file in files) {
            loadPack(file, candidatePackIds)
        }

        if (loadedPacks.isEmpty() && plugin.config.getBoolean("feature_packs.allow_builtin_fallbacks", true)) {
            FeaturePackDefaults.loadNeutralFallbackPack(
                loadedPacks,
                allTraits,
                allProfessions,
                this::registerTopology,
                this::registerPackDescriptor,
            )
        }

        if (allTopologies.isEmpty()) {
            FeaturePackDefaults.loadDefaultTopologies(
                loadedPacks,
                this::registerTopology,
                this::registerPackDescriptor,
            )
        }

        plugin.logger.info("Feature Packs incarcate: ${loadedPacks.size}")
        plugin.logger.info("Traits disponibile: ${allTraits.size}")
        plugin.logger.info("Profesii disponibile: ${allProfessions.size}")
        plugin.logger.info("Topologii disponibile: ${allTopologies.size}")
        plugin.logger.info("Mecanici de progres disponibile: ${allProgressionMechanics.size}")
    }

    private fun saveDefaultPacks() {
        plugin.debug("Core-ul nu mai instaleaza pack-uri tematice implicite; continutul este livrat prin addonuri.")
    }

    private fun collectPackFiles(root: File, folder: File, files: MutableList<File>, allowAddonPacks: Boolean) {
        val entries = folder.listFiles() ?: return
        entries.sortWith(Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER))
        for (entry in entries) {
            val relativePath = relativizePackPath(root, entry)
            if (!allowAddonPacks &&
                (relativePath.equals("addons", ignoreCase = true) ||
                    relativePath.lowercase(Locale.ROOT).startsWith("addons/"))
            ) {
                continue
            }
            if (entry.isDirectory) {
                collectPackFiles(root, entry, files, allowAddonPacks)
                continue
            }

            val fileName = entry.name.lowercase(Locale.ROOT)
            if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) {
                files.add(entry)
            }
        }
    }

    private fun relativizePackPath(root: File, file: File): String =
        root.toPath().relativize(file.toPath()).toString().replace(File.separatorChar, '/')

    fun loadPack(file: File) {
        loadPack(file, null)
    }

    private fun loadPack(file: File, candidatePackIds: Set<String>?) {
        try {
            val config = YamlConfiguration.loadConfiguration(file)
            if (shouldValidatePackMetadata()) {
                val validation = FeaturePackMetadataValidator.validate(config, file, currentRuntimeMode())
                logPackMetadataWarnings(file, validation)
                if (!validation.valid()) {
                    rejectInvalidPackMetadata(file, validation)
                    return
                }
            }

            val id = config.getString("id", file.name.replace(".yml", "")) ?: file.name.replace(".yml", "")
            val name = config.getString("name", id) ?: id
            val description = config.getString("description", "") ?: ""
            if (isDisabledCoreDemoPack(id, file)) {
                plugin.logger.info("Feature pack demo din core ignorat deoarece demo.enabled=false: $id")
                return
            }
            if (isFeaturePackDisabled(id)) {
                plugin.logger.info("Feature pack dezactivat prin addons.disabled: $id")
                return
            }
            if (!validateFeaturePackDependencies(file, config, id, candidatePackIds)) {
                return
            }

            val pack = FeaturePack(id, name, description)

            config.getConfigurationSection("traits")?.let { section ->
                FeaturePackYamlSupport.loadTraits(pack, section, allTraits)
            }
            config.getConfigurationSection("professions")?.let { section ->
                FeaturePackYamlSupport.loadProfessions(pack, section, allProfessions)
            }
            config.getConfigurationSection("topologies")?.let { section ->
                FeaturePackYamlSupport.loadTopologies(pack, section, this::registerTopology)
            }
            config.getConfigurationSection("dialogues")?.let { section ->
                FeaturePackYamlSupport.loadDialogues(pack, section, allDialogues)
            }
            config.getConfigurationSection("mechanics")?.let { section ->
                FeaturePackYamlSupport.loadProgressionMechanics(pack, section, this::registerProgressionMechanic)
            }
            config.getConfigurationSection("scenarios")?.let { section ->
                FeaturePackYamlSupport.loadScenarios(
                    pack,
                    section,
                    allScenarios,
                    this::ensureDefaultQuestMechanic,
                    this::findProgressionMechanicDefinition,
                )
            }

            registerPackDescriptor(pack, config.getConfigurationSection("addon"))
            loadedPacks[id] = pack
            plugin.debug("Feature Pack incarcat: $name ($id)")
        } catch (exception: Exception) {
            plugin.logger.warning("Eroare la incarcarea feature pack: ${file.name}")
            exception.printStackTrace()
            if (plugin.config.getBoolean("feature_packs.fail_invalid_pack", false)) {
                throw IllegalStateException("Feature pack invalid: ${file.name}", exception)
            }
        }
    }

    private fun collectCandidatePackIds(files: List<File>): Set<String> {
        val dependenciesByPackId = LinkedHashMap<String, List<String>>()
        for (file in files) {
            try {
                val config = YamlConfiguration.loadConfiguration(file)
                val validation = FeaturePackMetadataValidator.validate(config, file, currentRuntimeMode())
                val packId = validation.packId()
                if (validation.valid() && !isFeaturePackDisabled(packId)) {
                    dependenciesByPackId[packId] = config.getStringList("addon.dependencies")
                }
            } catch (exception: Exception) {
                plugin.debug("Nu s-a putut citi metadata pentru candidate pack: ${file.name}")
            }
        }
        return FeaturePackDependencyValidator.resolveAvailablePackIds(dependenciesByPackId)
    }

    private fun validateFeaturePackDependencies(
        file: File,
        config: YamlConfiguration,
        packId: String,
        candidatePackIds: Set<String>?,
    ): Boolean {
        if (!shouldValidatePackMetadata() || candidatePackIds == null) {
            return true
        }

        val dependencies = config.getStringList("addon.dependencies")
        val missingDependencies = FeaturePackDependencyValidator.missingDependencies(candidatePackIds, dependencies)
        if (missingDependencies.isEmpty()) {
            return true
        }

        plugin.logger.warning("Feature pack respins prin dependinte lipsa: $packId (${file.name})")
        plugin.logger.warning("- dependinte lipsa: ${missingDependencies.joinToString(", ")}")
        if (plugin.config.getBoolean("feature_packs.fail_invalid_pack", false)) {
            throw IllegalStateException("Dependinte feature pack lipsa: $packId")
        }
        return false
    }

    private fun shouldValidatePackMetadata(): Boolean =
        plugin.config.getBoolean("feature_packs.validate_on_startup", true) &&
            plugin.config.getBoolean("feature_packs.validate_addon_metadata", true)

    private fun currentRuntimeMode(): RuntimeMode =
        RuntimeMode.fromId(plugin.config.getString("platform.runtime_mode", "standalone"))

    private fun logPackMetadataWarnings(file: File, validation: FeaturePackMetadataValidator.ValidationResult) {
        for (warning in validation.warnings()) {
            plugin.logger.warning("Metadata feature pack ${file.name}: $warning")
        }
    }

    private fun rejectInvalidPackMetadata(file: File, validation: FeaturePackMetadataValidator.ValidationResult) {
        plugin.logger.warning("Feature pack respins prin metadata invalida: ${file.name}")
        for (error in validation.errors()) {
            plugin.logger.warning("- $error")
        }
        if (plugin.config.getBoolean("feature_packs.fail_invalid_pack", false)) {
            throw IllegalStateException("Metadata feature pack invalida: ${file.name}")
        }
    }

    private fun isFeaturePackDisabled(id: String?): Boolean {
        val normalized = id?.trim()?.lowercase(Locale.ROOT).orEmpty()
        if (normalized.isBlank()) {
            return false
        }
        return plugin.config.getStringList("addons.disabled")
            .filter { value -> !value.isNullOrBlank() }
            .map { value -> value.trim().lowercase(Locale.ROOT) }
            .any { value -> value == normalized }
    }

    private fun isDisabledCoreDemoPack(id: String?, file: File): Boolean {
        if (plugin.config.getBoolean("demo.enabled", true)) {
            return false
        }
        val normalized = id?.trim()?.lowercase(Locale.ROOT).orEmpty()
        if (normalized !in CORE_DEMO_PACK_IDS) {
            return false
        }
        val parentName = file.parentFile?.name ?: return false
        return parentName.equals("packs", ignoreCase = true)
    }

    private fun registerProgressionMechanic(pack: FeaturePack?, mechanic: ProgressionMechanicDefinition?) {
        if (pack == null || mechanic == null || mechanic.id.isBlank()) {
            return
        }

        pack.addProgressionMechanic(mechanic)
        allProgressionMechanics[pack.id + ":" + mechanic.id] = mechanic
    }

    private fun ensureDefaultQuestMechanic(pack: FeaturePack?) {
        if (pack == null || pack.hasProgressionMechanic("quest")) {
            return
        }

        registerProgressionMechanic(
            pack,
            ProgressionMechanicDefinition(
                pack.id,
                "quest",
                "quest",
                "Questuri",
                "quest",
                "questuri",
                true,
                0,
                mapOf("source" to "legacy_quest_default"),
            ),
        )
    }

    private fun registerTopology(pack: FeaturePack, topology: TopologyDefinition) {
        val key = pack.id + ":" + topology.id
        allTopologies[key] = topology
        topologiesByCategory.computeIfAbsent(topology.category) { ArrayList() }.add(topology)
        pack.addTopology(topology)
    }

    fun getTrait(id: String): TraitDefinition? = allTraits[id]

    fun getProfession(id: String): ProfessionDefinition? = allProfessions[id]

    fun getTopology(id: String?): TopologyDefinition? {
        if (id.isNullOrBlank()) {
            return null
        }

        val direct = allTopologies[id]
        if (direct != null) {
            return direct
        }

        return allTopologies.values.firstOrNull { topology -> topology.id.equals(id, ignoreCase = true) }
    }

    fun getTopologies(category: TopologyCategory?): List<TopologyDefinition> {
        if (category == null) {
            return emptyList()
        }
        return Collections.unmodifiableList(topologiesByCategory.getOrDefault(category, emptyList()))
    }

    fun buildTopologyConsensus(category: TopologyCategory): TopologyConsensus? {
        val definitions = topologiesByCategory[category]
        if (definitions.isNullOrEmpty()) {
            return null
        }

        val descriptions = LinkedHashSet<String>()
        val biomes = LinkedHashSet<String>()
        val dialogueHints = LinkedHashSet<String>()
        val suggestedTraits = LinkedHashSet<String>()
        val sourcePacks = LinkedHashSet<String>()

        for (definition in definitions) {
            if (!definition.description.isNullOrBlank()) {
                descriptions.add(definition.description)
            }
            biomes.addAll(definition.biomes)
            dialogueHints.addAll(definition.dialogueHints)
            suggestedTraits.addAll(definition.suggestedTraits)
            sourcePacks.add(definition.packId)
        }

        return TopologyConsensus(
            category,
            ArrayList(descriptions),
            ArrayList(biomes),
            ArrayList(dialogueHints),
            ArrayList(suggestedTraits),
            ArrayList(sourcePacks),
        )
    }

    fun getDialogues(packId: String, category: String): List<String> =
        allDialogues.getOrDefault("$packId:$category", emptyList())

    fun getAllTraits(): Collection<TraitDefinition> = allTraits.values

    fun getAllProfessions(): Collection<ProfessionDefinition> = allProfessions.values

    fun getAllTopologies(): Collection<TopologyDefinition> = allTopologies.values

    fun getAllProgressionMechanics(): Collection<ProgressionMechanicDefinition> =
        Collections.unmodifiableCollection(allProgressionMechanics.values)

    fun getAllScenarios(): Collection<ScenarioDefinition> = Collections.unmodifiableCollection(allScenarios.values)

    fun getLoadedPacks(): Collection<FeaturePack> = Collections.unmodifiableCollection(loadedPacks.values)

    fun getPrimaryScenarioPack(): FeaturePack? =
        loadedPacks.values
            .asSequence()
            .filter { pack -> pack.addonDescriptor != null }
            .filter { pack -> pack.addonDescriptor?.type == AddonType.SCENARIO }
            .filter { pack -> pack.addonDescriptor?.isPrimaryScenario == true }
            .filter { pack -> pack.hasScenarioDefinitions() }
            .firstOrNull()
            ?: loadedPacks.values.firstOrNull { pack -> pack.hasScenarioDefinitions() }

    fun findProfessionDefinition(reference: String?): ProfessionDefinition? {
        if (reference.isNullOrBlank()) {
            return null
        }

        val direct = allProfessions[reference]
        if (direct != null) {
            return direct
        }

        return allProfessions.values.firstOrNull { profession -> profession.matches(reference) }
    }

    fun findPrimaryScenarioProfession(occupation: String?): ProfessionDefinition? {
        val primaryScenario = getPrimaryScenarioPack()
        for (profession in primaryScenario?.professions.orEmpty()) {
            if (profession.matches(occupation)) {
                return profession
            }
        }

        return findProfessionDefinition(occupation)
    }

    fun matchesProfession(occupation: String?, professionReference: String?): Boolean {
        if (occupation.isNullOrBlank() || professionReference.isNullOrBlank()) {
            return false
        }

        val target = findProfessionDefinition(professionReference)
        if (target == null) {
            return FeaturePackSupport.normalizeText(occupation) == FeaturePackSupport.normalizeText(professionReference)
        }

        return target.matches(occupation)
    }

    fun findProgressionMechanicDefinition(packId: String?, reference: String?): ProgressionMechanicDefinition? {
        if (reference.isNullOrBlank()) {
            return null
        }

        val normalizedReference = reference.trim()
        if (normalizedReference.contains(":")) {
            val direct = allProgressionMechanics[normalizedReference]
            if (direct != null) {
                return direct
            }
        }

        if (!packId.isNullOrBlank()) {
            val local = allProgressionMechanics["$packId:$normalizedReference"]
            if (local != null) {
                return local
            }
        }

        return allProgressionMechanics.values.firstOrNull { mechanic ->
            mechanic.id.equals(normalizedReference, ignoreCase = true) ||
                mechanic.kind.equals(normalizedReference, ignoreCase = true)
        }
    }

    fun applyTraitModifiers(traitId: String, action: NPCAction, baseScore: Int): Int {
        val trait = allTraits[traitId] ?: return baseScore
        val modifier = trait.getActionModifier(action.name)
        return if (modifier != null) baseScore + modifier else baseScore
    }

    fun applyTraitEmotionModifiers(traitId: String, emotion: String, baseValue: Double): Double {
        val trait = allTraits[traitId] ?: return baseValue
        val modifier = trait.getEmotionModifier(emotion)
        return if (modifier != null) baseValue + modifier else baseValue
    }

    private fun registerPackDescriptor(pack: FeaturePack, addonSection: ConfigurationSection?) {
        var addonType = if (addonSection != null) {
            AddonType.fromId(addonSection.getString("type"))
        } else {
            FeaturePackSupport.inferAddonType(pack)
        }

        if (addonType == AddonType.SCENARIO && !pack.hasScenarioDefinitions()) {
            addonType = AddonType.FEATURE
        }

        var primaryScenario = if (addonSection != null) {
            addonSection.getBoolean("primary_scenario", false)
        } else {
            false
        }
        primaryScenario = primaryScenario && addonType == AddonType.SCENARIO && pack.hasScenarioDefinitions()

        var capabilities = if (addonSection != null && addonSection.getStringList("capabilities").isNotEmpty()) {
            addonSection.getStringList("capabilities")
        } else {
            FeaturePackSupport.detectCapabilities(pack)
        }
        capabilities = FeaturePackSupport.sanitizeCapabilities(capabilities, pack)
        val dependencies = addonSection?.getStringList("dependencies") ?: emptyList()
        val runtimeModes = addonSection?.let {
            RuntimeMode.fromIds(it.getStringList("runtime_modes"))
        } ?: EnumSet.allOf(RuntimeMode::class.java)

        val descriptor = AddonDescriptor(
            AddonDescriptor.ORIGIN_FEATURE_PACK,
            pack.id,
            pack.name,
            addonSection?.getString("version", "1.0.0") ?: "1.0.0",
            pack.description,
            addonType,
            primaryScenario,
            runtimeModes,
            capabilities,
            dependencies,
        )

        pack.addonDescriptor = descriptor
        plugin.platform.addonRegistry.registerDescriptor(descriptor)
    }

    companion object {
        private val CORE_DEMO_PACK_IDS = setOf("medieval", "modern", "social")
    }

    class FeaturePack(
        val id: String,
        val name: String,
        val description: String,
    ) {
        val traits: MutableList<TraitDefinition> = ArrayList()
        val professions: MutableList<ProfessionDefinition> = ArrayList()
        val topologies: MutableList<TopologyDefinition> = ArrayList()
        val scenarios: MutableList<ScenarioDefinition> = ArrayList()
        val progressionMechanics: MutableList<ProgressionMechanicDefinition> = ArrayList()
        val dialogues: MutableMap<String, List<String>> = HashMap()
        private var scenarioDefinitions = false
        var addonDescriptor: AddonDescriptor? = null

        fun addTrait(trait: TraitDefinition) {
            traits.add(trait)
        }

        fun addProfession(profession: ProfessionDefinition) {
            professions.add(profession)
        }

        fun addTopology(topology: TopologyDefinition) {
            topologies.add(topology)
        }

        fun addScenario(scenario: ScenarioDefinition) {
            scenarios.add(scenario)
        }

        fun addProgressionMechanic(progressionMechanic: ProgressionMechanicDefinition?) {
            if (progressionMechanic != null && !hasProgressionMechanic(progressionMechanic.id)) {
                progressionMechanics.add(progressionMechanic)
            }
        }

        fun addDialogueCategory(category: String, lines: List<String>) {
            dialogues[category] = lines
        }

        fun markHasScenarioDefinitions() {
            scenarioDefinitions = true
        }

        fun hasScenarioDefinitions(): Boolean = scenarioDefinitions

        fun hasProgressionMechanic(mechanicId: String?): Boolean {
            if (mechanicId.isNullOrBlank()) {
                return false
            }
            return progressionMechanics.any { mechanic -> mechanic.id.equals(mechanicId, ignoreCase = true) }
        }
    }

    class ProgressionMechanicDefinition(
        packId: String?,
        id: String?,
        kind: String?,
        label: String?,
        singularLabel: String?,
        pluralLabel: String?,
        val isProgressEnabled: Boolean,
        maxActive: Int,
        metadata: Map<String, String>?,
    ) {
        val packId: String = packId ?: ""
        val id: String = id ?: ""
        val kind: String = if (kind.isNullOrBlank()) this.id else kind
        val label: String = if (label.isNullOrBlank()) this.id else label
        val singularLabel: String = if (singularLabel.isNullOrBlank()) this.id else singularLabel
        val pluralLabel: String = if (pluralLabel.isNullOrBlank()) this.label else pluralLabel
        val maxActive: Int = maxActive.coerceAtLeast(0)
        val metadata: Map<String, String> = Collections.unmodifiableMap(LinkedHashMap(metadata ?: emptyMap()))
    }

    class TraitDefinition(
        val id: String,
        val name: String,
        val description: String,
    ) {
        val actionModifiers: MutableMap<String, Int> = HashMap()
        val emotionModifiers: MutableMap<String, Double> = HashMap()

        fun addActionModifier(action: String, modifier: Int) {
            actionModifiers[action] = modifier
        }

        fun addEmotionModifier(emotion: String, modifier: Double) {
            emotionModifiers[emotion] = modifier
        }

        fun getActionModifier(action: String): Int? = actionModifiers[action]

        fun getEmotionModifier(emotion: String): Double? = emotionModifiers[emotion]
    }

    class ProfessionDefinition(
        val id: String,
        val name: String,
        val description: String,
    ) {
        val schedule: MutableMap<String, String> = LinkedHashMap()
        var workLocations: List<String> = ArrayList()
        var tools: List<String> = ArrayList()
        var dialogues: List<String> = ArrayList()
        var aliases: List<String> = ArrayList()
        var suggestedTraits: List<String> = ArrayList()

        fun addScheduleEntry(timeOfDay: String, activity: String?) {
            schedule[timeOfDay] = activity ?: ""
        }

        fun matches(value: String?): Boolean {
            val normalized = FeaturePackSupport.normalizeText(value)
            if (normalized.isBlank()) {
                return false
            }

            if (FeaturePackSupport.normalizeText(id) == normalized ||
                FeaturePackSupport.normalizeText(name) == normalized
            ) {
                return true
            }

            return aliases.any { alias -> FeaturePackSupport.normalizeText(alias) == normalized }
        }
    }

    class TopologyDefinition(
        val packId: String,
        val id: String,
        val name: String,
        val category: TopologyCategory,
        val description: String,
    ) {
        var biomes: List<String> = ArrayList()
        var dialogueHints: List<String> = ArrayList()
        var suggestedTraits: List<String> = ArrayList()
    }

    class ScenarioDefinition(
        val packId: String,
        val id: String,
        val name: String,
        val description: String,
        val baseType: ScenarioEngine.ScenarioType,
    ) {
        val roles: MutableMap<String, ScenarioRoleDefinition> = LinkedHashMap()
        val objectives: MutableList<QuestEntryDefinition> = ArrayList()
        val rewards: MutableList<QuestEntryDefinition> = ArrayList()
        private val questStagesById: MutableMap<String, QuestStageDefinition> = LinkedHashMap()
        val questStages: List<QuestStageDefinition>
            get() = ArrayList(questStagesById.values)
        var phases: List<String> = ArrayList()
        var preferredTopologies: List<String> = ArrayList()
        var narrativeHints: List<String> = ArrayList()
        var triggerProbability = 0.05
        var minimumNpcCount = 2
        var isRequiresPlayer = false
        var isReplaceBaseType = false
        var hint = ""
        var questCode = ""
        var questGiverProfession = ""
        var questCategory = ""
        var questScenarioKind = ""
        var questAcceptanceMode = ""
        var questCompletionMode = ""
        var questTrackingMode = ""
        var questTags: List<String> = ArrayList()
        var questPrerequisites: List<String> = ArrayList()
        var isQuestRepeatable = false
        var questCooldownSeconds = 0L
            set(value) {
                field = value.coerceAtLeast(0L)
            }
        var questDialogues: Map<String, List<String>> = LinkedHashMap()
        var isProgressionEnabled = false
        var progressionMechanicId = ""
        var progressionKind = ""
        var progressionLabel = ""
        var progressionSingularLabel = ""
        var progressionPluralLabel = ""
        var progressionMaxActive = 0
            set(value) {
                field = value.coerceAtLeast(0)
            }

        fun addRole(role: ScenarioRoleDefinition) {
            roles[role.id] = role
        }

        fun addPhase(phaseId: String?) {
            if (!phaseId.isNullOrBlank()) {
                phases = phases + phaseId
            }
        }

        fun addQuestStage(stage: QuestStageDefinition?) {
            if (stage != null && stage.id.isNotBlank()) {
                questStagesById[stage.id] = stage
            }
        }

        fun addObjective(objective: QuestEntryDefinition?) {
            if (objective != null) {
                objectives.add(objective)
            }
        }

        fun addReward(reward: QuestEntryDefinition?) {
            if (reward != null) {
                rewards.add(reward)
            }
        }

    }

    class ScenarioRoleDefinition(
        val id: String,
        val description: String?,
    ) {
        var isPlayerRole = false
        var isOptional = false
        var requiredProfessions: List<String> = ArrayList()
        var preferredProfessions: List<String> = ArrayList()
        var requiredTraits: List<String> = ArrayList()
        var preferredTraits: List<String> = ArrayList()
    }

    class QuestStageDefinition(
        id: String?,
        description: String?,
        completionMode: String?,
        objectiveIds: List<String>?,
        metadata: Map<String, String>?,
    ) {
        val id: String = id ?: ""
        val description: String = description ?: ""
        val completionMode: String = if (completionMode.isNullOrBlank()) "all_objectives" else completionMode
        val objectiveIds: List<String> = Collections.unmodifiableList(ArrayList(objectiveIds ?: emptyList()))
        val metadata: Map<String, String> = Collections.unmodifiableMap(LinkedHashMap(metadata ?: emptyMap()))

        fun getNextStageId(): String = metadata.getOrDefault("next_stage", metadata.getOrDefault("next", ""))
    }

    class QuestEntryDefinition(
        type: String?,
        itemId: String?,
        amount: Int,
        description: String?,
        metadata: Map<String, String>?,
        variables: Map<String, String>?,
        payload: Map<String, String>?,
    ) {
        constructor(type: String?, itemId: String?, amount: Int, description: String?) :
            this(type, itemId, amount, description, emptyMap(), emptyMap(), emptyMap())

        val type: String = type ?: "item"
        val itemId: String = itemId ?: ""
        val amount: Int = amount.coerceAtLeast(1)
        val description: String = description ?: ""
        val metadata: Map<String, String> = Collections.unmodifiableMap(LinkedHashMap(metadata ?: emptyMap()))
        val variables: Map<String, String> = Collections.unmodifiableMap(LinkedHashMap(variables ?: emptyMap()))
        val payload: Map<String, String> = Collections.unmodifiableMap(LinkedHashMap(payload ?: emptyMap()))
        val entryId: String
            get() = metadata.getOrDefault("entry_id", "")
    }
}
