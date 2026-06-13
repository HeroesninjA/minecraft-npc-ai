package ro.ainpc.engine

import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import ro.ainpc.AINPCPlugin
import ro.ainpc.npc.AINPC
import java.util.Locale

lateinit var simpleQuestPlugin: AINPCPlugin

fun initSimpleQuestPlugin(plugin: AINPCPlugin) {
    simpleQuestPlugin = plugin
}

fun getQuestSettings(): ConfigurationSection {
    val questFile = simpleQuestPlugin.questConfig
    if (questFile != null) {
        val nested = questFile.getConfigurationSection("quests")
        return nested ?: questFile
    }
    val nested = simpleQuestPlugin.config.getConfigurationSection("quests")
    return nested ?: simpleQuestPlugin.config
}

fun shouldUseSimpleQuestForAllNpcs(): Boolean {
    if (!simpleQuestPlugin.config.getBoolean("demo.enabled", true)) {
        return false
    }
    return getQuestSettings().getBoolean("simple_for_all_npcs", true)
}

fun buildSimpleQuestTemplate(npc: AINPC?): ScenarioEngine.ScenarioTemplate? {
    if (npc == null) return null
    val profession = resolveQuestProfession(npc)
    val questProfile = resolveSimpleQuestProfile(npc, profession)
    val npcIdentifier = if (npc.databaseId > 0) npc.databaseId.toString() else npc.uuid.toString()
    val template = ScenarioEngine.ScenarioTemplate(ScenarioEngine.ScenarioType.QUEST)
    template.templateId = "simple_npc_quest:$npcIdentifier"
    template.displayName = questProfile.title()
    template.description = questProfile.objectivePrompt() + " si iti dau " + formatQuestAmount(questProfile.rewardAmount(), questProfile.rewardMaterial()) + "."
    template.hint = questProfile.hint()
    template.questGiverProfession = profession?.id ?: npc.occupation
    template.setRequiresPlayer(true)
    template.minimumNpcCount = 1
    template.objectives = listOf(
        FeaturePackLoader.QuestEntryDefinition(
            "collect_item",
            questProfile.objectiveMaterial().name,
            questProfile.objectiveAmount(),
            questProfile.objectivePrompt() + ".",
        ),
    )
    template.rewards = listOf(
        FeaturePackLoader.QuestEntryDefinition(
            "item",
            questProfile.rewardMaterial().name,
            questProfile.rewardAmount(),
            "Primesti " + formatQuestAmount(questProfile.rewardAmount(), questProfile.rewardMaterial()) + ".",
        ),
    )
    template.questContract = QuestScenarioContract.fromQuestEntries(
        "fetch",
        "explicit",
        "return_to_giver",
        "next_objective",
        listOf("fallback", "simple"),
        template.objectives,
    )
    return template
}

fun resolveQuestProfession(npc: AINPC?): FeaturePackLoader.ProfessionDefinition? {
    if (npc == null || simpleQuestPlugin.featurePackLoader == null) return null
    return simpleQuestPlugin.featurePackLoader.findPrimaryScenarioProfession(npc.occupation)
}

fun resolveSimpleQuestProfile(
    npc: AINPC?,
    profession: FeaturePackLoader.ProfessionDefinition?,
): SimpleQuestProfile {
    val professionId = if (profession != null) normalizeScenarioToken(profession.id) else ""
    var professionName = if (profession != null && !profession.name.isNullOrBlank()) profession.name else npc?.occupation
    if (professionName.isNullOrBlank()) professionName = "localnicul"

    val questSettings = getQuestSettings()
    val objectiveAmount = maxOf(1, questSettings.getInt("simple.objective.amount", 3))
    val objectiveMaterial = resolveConfiguredQuestMaterial("simple.objective.item", Material.OAK_PLANKS)
    val rewardAmount = maxOf(1, questSettings.getInt("simple.reward.amount", 1))
    val rewardMaterial = resolveConfiguredQuestMaterial("simple.reward.item", Material.EMERALD)
    val defaultProfile = SimpleQuestProfile(
        resolveConfiguredSimpleQuestTitle(professionName),
        objectiveMaterial,
        objectiveAmount,
        rewardMaterial,
        rewardAmount,
        "Adu-mi " + formatQuestAmount(objectiveAmount, objectiveMaterial),
        (npc?.name ?: "NPC") + " are nevoie de ajutor cu treburi obisnuite de " + professionName + ".",
    )
    return applyConfiguredSimpleQuestProfile(npc, professionId, professionName, defaultProfile)
}

fun applyConfiguredSimpleQuestProfile(
    npc: AINPC?,
    professionId: String,
    professionName: String,
    fallbackProfile: SimpleQuestProfile,
): SimpleQuestProfile {
    val section = resolveProfessionFallbackSection(professionId, professionName)
    if (section == null) return fallbackProfile

    val objectiveMaterial = resolveConfiguredQuestMaterialValue(
        section.getString("objective.item"),
        fallbackProfile.objectiveMaterial(),
    )
    val objectiveAmount = maxOf(1, section.getInt("objective.amount", fallbackProfile.objectiveAmount()))
    val rewardMaterial = resolveConfiguredQuestMaterialValue(
        section.getString("reward.item"),
        fallbackProfile.rewardMaterial(),
    )
    val rewardAmount = maxOf(1, section.getInt("reward.amount", fallbackProfile.rewardAmount()))

    val objectiveText = formatQuestAmount(objectiveAmount, objectiveMaterial)
    val rewardText = formatQuestAmount(rewardAmount, rewardMaterial)
    val title = applyQuestFallbackPlaceholders(
        section.getString("title", fallbackProfile.title()),
        npc,
        professionName,
        objectiveText,
        rewardText,
    )
    val objectivePrompt = applyQuestFallbackPlaceholders(
        section.getString("objective.prompt", "Adu-mi " + objectiveText),
        npc,
        professionName,
        objectiveText,
        rewardText,
    )
    val hint = applyQuestFallbackPlaceholders(
        section.getString("hint", fallbackProfile.hint()),
        npc,
        professionName,
        objectiveText,
        rewardText,
    )
    return SimpleQuestProfile(title, objectiveMaterial, objectiveAmount, rewardMaterial, rewardAmount, objectivePrompt, hint)
}

fun resolveProfessionFallbackSection(professionId: String, professionName: String): ConfigurationSection? {
    val root = getQuestSettings().getConfigurationSection("profession_fallbacks") ?: return null
    if (professionId.isNotBlank()) {
        val byId = root.getConfigurationSection(sanitizeConfigKey(professionId))
        if (byId != null) return byId
    }
    if (professionName.isNotBlank()) {
        val byName = root.getConfigurationSection(sanitizeConfigKey(professionName))
        if (byName != null) return byName
    }
    return root.getConfigurationSection("default")
}

fun resolveConfiguredSimpleQuestTitle(professionName: String?): String {
    val configuredTitle = getQuestSettings().getString("simple.title", "Ajutor rapid") ?: "Ajutor rapid"
    if (professionName.isNullOrBlank()) return configuredTitle
    return "$configuredTitle - $professionName"
}

fun resolveConfiguredQuestMaterial(path: String, fallback: Material): Material {
    val configuredValue = getQuestSettings().getString(path, fallback.name)
    return resolveConfiguredQuestMaterialValue(configuredValue, fallback)
}

fun resolveConfiguredQuestMaterialValue(configuredValue: String?, fallback: Material): Material {
    if (configuredValue.isNullOrBlank()) return fallback
    val material = Material.matchMaterial(configuredValue.trim().uppercase(Locale.ROOT))
    return material ?: fallback
}
