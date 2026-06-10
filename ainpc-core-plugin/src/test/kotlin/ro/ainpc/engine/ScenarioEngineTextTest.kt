package ro.ainpc.engine

import org.bukkit.Material
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import ro.ainpc.npc.AINPC

class ScenarioEngineTextTest {
    @Test
    fun questFallbackPlaceholdersUseProvidedValuesAndDefaults() {
        val npc = AINPC(null).apply { name = "Mara" }

        assertEquals("", applyQuestFallbackPlaceholders(null, null, null, null, null))
        assertEquals(" ", applyQuestFallbackPlaceholders(" ", null, null, null, null))
        assertEquals(
            "Mara fierar adu lemn pentru 3x emerald",
            applyQuestFallbackPlaceholders(
                "{npc} {profession} {objective} pentru {reward}",
                npc,
                "fierar",
                "adu lemn",
                "3x emerald",
            ),
        )
        assertEquals(
            "NPC localnic materiale pentru o recompensa",
            applyQuestFallbackPlaceholders(
                "{npc} {profession} {objective} pentru {reward}",
                null,
                " ",
                null,
                null,
            ),
        )
    }

    @Test
    fun configKeysAreLowercaseUnderscoreAndAsciiSafe() {
        assertEquals("", sanitizeConfigKey(null))
        assertEquals("black_smith", sanitizeConfigKey(" Black-Smith "))
        assertEquals("farmer_2", sanitizeConfigKey("Farmer #2"))
        assertEquals("npc_home", sanitizeConfigKey("NPC Home!"))
    }

    @Test
    fun progressionLabelCapitalizationKeepsLegacyFallbacks() {
        assertEquals("Progresie", capitalizeProgressionLabel(null))
        assertEquals("Progresie", capitalizeProgressionLabel(" "))
        assertEquals("Q", capitalizeProgressionLabel(" q "))
        assertEquals("Quest rapid", capitalizeProgressionLabel(" quest rapid"))
    }

    @Test
    fun resolveQuestTitleUsesQuestCodePrefixWhenAvailable() {
        assertEquals("", resolveQuestTitle(null))

        val template = ScenarioEngine.ScenarioTemplate(ScenarioEngine.ScenarioType.QUEST)
        template.displayName = "Ajutor rapid"
        assertEquals("Ajutor rapid", resolveQuestTitle(template))

        template.questCode = "Q-101"
        assertEquals("Q-101 - Ajutor rapid", resolveQuestTitle(template))
    }

    @Test
    fun stageCompletionModesUseLegacyDisplayLabels() {
        assertEquals("Toate obiectivele", formatStageCompletionMode(null))
        assertEquals("Toate obiectivele", formatStageCompletionMode("all"))
        assertEquals("Orice obiectiv", formatStageCompletionMode("any_objectives"))
        assertEquals("Returnare manuala", formatStageCompletionMode("turn-in"))
        assertEquals("Toate obiectivele", formatStageCompletionMode("custom"))
    }

    @Test
    fun fallbackAndOptionalFormattingKeepLegacyBlankHandling() {
        assertEquals("fallback", valueOrFallback(null, "fallback"))
        assertEquals("fallback", valueOrFallback(" ", "fallback"))
        assertEquals(" value ", valueOrFallback(" value ", "fallback"))
        assertEquals("<gol>", formatOptional(null))
        assertEquals("<gol>", formatOptional(" "))
        assertEquals("cod", formatOptional("cod"))
        assertEquals("<gol>", formatQuestDebugTime(0L))
        assertEquals("<gol>", formatQuestDebugTime(-5L))
        assertEquals("12345", formatQuestDebugTime(12345L))
    }

    @Test
    fun durationFormattingRoundsUpToReadableUnits() {
        assertEquals("1s", formatDuration(0L))
        assertEquals("1s", formatDuration(1L))
        assertEquals("1s", formatDuration(1000L))
        assertEquals("2s", formatDuration(1001L))
        assertEquals("1m 1s", formatDuration(61_000L))
        assertEquals("1h 1m", formatDuration(3_660_000L))
    }

    @Test
    fun questDebugMapFormatsSortedLimitedRows() {
        assertEquals(listOf("&7- &f<gol>"), formatQuestDebugMap(null, 10))
        assertEquals(listOf("&7- &f<gol>"), formatQuestDebugMap(emptyMap<String, Any?>(), 10))
        assertEquals(
            listOf("&7- &fa &7= &f1", "&7- &fb &7= &f2"),
            formatQuestDebugMap(mapOf("b" to 2, "a" to 1), 10),
        )
        assertEquals(
            listOf("&7- &fa &7= &f1", "&7- &f... inca 2 valori"),
            formatQuestDebugMap(mapOf("b" to 2, "a" to 1, "c" to 3), 1),
        )
        assertEquals(
            listOf("&7- &fa &7= &f1", "&7- &f... inca 1 valori"),
            formatQuestDebugMap(mapOf("b" to 2, "a" to 1), 0),
        )
    }

    @Test
    fun questLogMechanicCountsFormatsEmptyAndCaseInsensitiveSortedCounts() {
        assertEquals("<gol>", formatQuestLogMechanicCounts(null))
        assertEquals("<gol>", formatQuestLogMechanicCounts(emptyMap()))
        assertEquals(
            "Bounty=2&7, &fcontract=1&7, &fquest=3",
            formatQuestLogMechanicCounts(mapOf("quest" to 3, "Bounty" to 2, "contract" to 1)),
        )
    }

    @Test
    fun questMaterialResolutionUsesBukkitMaterialWhenAvailable() {
        assertNull(resolveQuestMaterial(null))
        assertNull(resolveQuestMaterial(entry("collect_item", " ")))
        assertNull(resolveQuestMaterial(entry("collect_item", "custom_item")))
        assertEquals(Material.EMERALD, resolveQuestMaterial(entry("collect_item", "EMERALD")))
    }

    @Test
    fun objectiveProgressLabelsKeepLegacyWording() {
        assertEquals("obiectiv", formatObjectiveProgressLabel(null))
        assertEquals("emerald", formatObjectiveProgressLabel(entry("collect_item", "EMERALD", 3)))
        assertEquals("custom item", formatObjectiveProgressLabel(entry("collect_item", "custom_item", 2)))
        assertEquals("vorbeste cu blacksmith npc", formatObjectiveProgressLabel(entry("talk_to_npc", "npc:Blacksmith_NPC")))
        assertEquals("Aduna praf", formatObjectiveProgressLabel(entry("custom_type", "mystic_dust", description = "Aduna praf")))
        assertEquals("mystic dust", formatObjectiveProgressLabel(entry("custom_type", "mystic_dust")))
    }

    @Test
    fun missingObjectiveTextKeepsAmountsAndTargetFallbacks() {
        assertEquals("obiectiv necunoscut", formatMissingObjective(null, 0, 1))
        assertEquals("4x emerald", formatMissingObjective(entry("collect_item", "EMERALD"), currentAmount = 1, requiredAmount = 5))
        assertEquals("emerald", formatMissingObjective(entry("collect_item", "EMERALD"), currentAmount = 5, requiredAmount = 5))
        assertEquals("vorbeste cu npc-ul tintit", formatMissingObjective(entry("talk_to_npc", ""), 0, 1))
        assertEquals("ucide 3x zombie", formatMissingObjective(entry("kill_mob", "mob:Zombie"), currentAmount = 0, requiredAmount = 3))
        assertEquals("mystic dust", formatMissingObjective(entry("custom_type", "mystic_dust"), 0, 1))
    }

    @Test
    fun questEntryFormattingKeepsObjectiveStoryAndFallbackBranches() {
        assertEquals("", formatQuestEntry(null))
        assertEquals("Descriere manuala", formatQuestEntry(entry("collect_item", "EMERALD", description = "Descriere manuala")))
        assertEquals("3x emerald", formatQuestEntry(entry("collect_item", "EMERALD", 3)))
        assertEquals("2x mystic dust", formatQuestEntry(entry("collect_item", "mystic_dust", 2)))
        assertEquals("Vorbeste cu blacksmith", formatQuestEntry(entry("talk_to_npc", "npc:Blacksmith")))
        assertEquals("Ucide 2x zombie", formatQuestEntry(entry("kill_mob", "mob:Zombie", 2)))
        assertEquals(
            "Actualizeaza story state: village.saved",
            formatQuestEntry(entry("set_story_state", "", metadata = mapOf("state_key" to "village.saved"))),
        )
        assertEquals(
            "Inregistreaza story event: quest_started",
            formatQuestEntry(entry("record_story_event", "", metadata = mapOf("event_type" to "quest_started"))),
        )
        assertEquals("2x custom token", formatQuestEntry(entry("custom_type", "CUSTOM_TOKEN", 2)))
    }

    @Test
    fun questStatusKeepsLegacyDisplayLabels() {
        assertEquals("Necunoscut", formatQuestStatus(null))
        assertEquals("Disponibil", formatQuestStatus(QuestStatus.NOT_STARTED))
        assertEquals("Oferit, asteapta acceptarea", formatQuestStatus(QuestStatus.OFFERED))
        assertEquals("Activ", formatQuestStatus(QuestStatus.ACTIVE))
        assertEquals("Completat", formatQuestStatus(QuestStatus.COMPLETED))
        assertEquals("Esuat", formatQuestStatus(QuestStatus.FAILED))
    }

    @Test
    fun describeQuestProgressPrefersQuestCodeThenTemplateId() {
        assertEquals("necunoscut", describeQuestProgress(null))
        assertEquals(
            "Q-001 (Activ)",
            describeQuestProgress(progress(templateId = "template.collect", questCode = "Q-001", status = QuestStatus.ACTIVE)),
        )
        assertEquals(
            "template.collect (Completat)",
            describeQuestProgress(progress(templateId = "template.collect", questCode = " ", status = QuestStatus.COMPLETED)),
        )
        assertEquals(
            "template.collect (Necunoscut)",
            describeQuestProgress(progress(templateId = "template.collect", questCode = null, status = null)),
        )
    }

    @Test
    fun questAmountFormatsMaterialNamesAndFallbackItem() {
        assertEquals("item", formatQuestAmount(1, null))
        assertEquals("emerald", formatQuestAmount(1, Material.EMERALD))
        assertEquals("3x oak log", formatQuestAmount(3, Material.OAK_LOG))
    }

    @Test
    fun joinNaturallyUsesRomanianConnector() {
        assertEquals("", joinNaturally(null))
        assertEquals("", joinNaturally(emptyList()))
        assertEquals("mere", joinNaturally(listOf("mere")))
        assertEquals("mere si paine", joinNaturally(listOf("mere", "paine")))
        assertEquals("mere, paine si lapte", joinNaturally(listOf("mere", "paine", "lapte")))
    }

    @Test
    fun humanizeItemIdKeepsLegacyLowercaseSpacing() {
        assertEquals("item", humanizeItemId(null))
        assertEquals("item", humanizeItemId(" "))
        assertEquals("oak log", humanizeItemId("OAK_LOG"))
        assertEquals("minecraft:gold ingot", humanizeItemId("MINECRAFT:GOLD_INGOT"))
    }

    private fun progress(
        templateId: String?,
        questCode: String?,
        status: QuestStatus?,
    ): PlayerQuestProgress =
        PlayerQuestProgress(
            templateId,
            questCode,
            status,
            0L,
            0L,
            0L,
            "",
            emptyMap(),
            emptyMap(),
        )

    private fun entry(
        type: String?,
        itemId: String?,
        amount: Int = 1,
        description: String? = "",
        metadata: Map<String, String> = emptyMap(),
    ): FeaturePackLoader.QuestEntryDefinition =
        FeaturePackLoader.QuestEntryDefinition(type, itemId, amount, description, metadata, emptyMap(), emptyMap())
}
