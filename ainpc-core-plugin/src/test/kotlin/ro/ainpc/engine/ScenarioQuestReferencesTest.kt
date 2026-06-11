package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ScenarioQuestReferencesTest {
    @Test
    fun trackedQuestSelectorsAcceptLegacyAliases() {
        assertTrue(isTrackedQuestSelector("tracked"))
        assertTrue(isTrackedQuestSelector("current"))
        assertTrue(isTrackedQuestSelector("curent"))
        assertTrue(isTrackedQuestSelector("urmarit"))
        assertTrue(isTrackedQuestSelector(" Urmarit "))
        assertFalse(isTrackedQuestSelector(null))
        assertFalse(isTrackedQuestSelector("quest:Q-101"))
    }

    @Test
    fun progressionDefinitionIdsKeepLegacySeparatorRules() {
        assertEquals("", extractProgressionDefinitionId(null))
        assertEquals("", extractProgressionDefinitionId(" "))
        assertEquals("quest_001", extractProgressionDefinitionId("pack:quest_001"))
        assertEquals("pack:", extractProgressionDefinitionId("pack:"))
        assertEquals("quest_001", extractProgressionDefinitionId("quest_001"))
    }

    @Test
    fun progressionReferencesTrimAndSkipBlankParts() {
        assertEquals("", progressionReference())
        assertEquals("", progressionReference(null, " ", ""))
        assertEquals("quest:Q-101", progressionReference(" quest ", null, " Q-101 "))
        assertEquals("medieval:contract:Q-101", progressionReference("medieval", "contract", "Q-101"))
    }

    @Test
    fun progressionReferenceCandidatesIncludeProgressAndTemplateAliasesInStableOrder() {
        val progress = progress(templateId = "medieval:help_blacksmith", questCode = "Q-101")
        val template = template(
            templateId = "medieval:help_blacksmith",
            questCode = "Q-101",
            sourcePackId = "medieval",
            mechanicId = "guild_contract",
            progressionKind = "contract",
        )

        assertEquals(
            listOf(
                "medieval:help_blacksmith",
                "Q-101",
                "help_blacksmith",
                "guild_contract:Q-101",
                "guild_contract:help_blacksmith",
                "contract:Q-101",
                "contract:help_blacksmith",
                "medieval:guild_contract:Q-101",
                "medieval:guild_contract:help_blacksmith",
            ),
            buildProgressionReferenceCandidates(progress, template),
        )
        assertEquals(
            listOf("pack:quest_one", "quest_one"),
            buildProgressionReferenceCandidates(progress(templateId = "pack:quest_one", questCode = " "), null),
        )
        assertEquals(emptyList<String>(), buildProgressionReferenceCandidates(null, template))
    }

    @Test
    fun questReferenceMatchingUsesNormalizedCandidateAliases() {
        val progress = progress(templateId = "medieval:help_blacksmith", questCode = "Q-101")
        val template = template(
            templateId = "medieval:help_blacksmith",
            questCode = "Q-101",
            sourcePackId = "medieval",
            mechanicId = "guild_contract",
            progressionKind = "contract",
        )

        assertTrue(matchesQuestReference(progress, "Q-101", template))
        assertTrue(matchesQuestReference(progress, "help blacksmith", template))
        assertTrue(matchesQuestReference(progress, "guild contract q 101", template))
        assertTrue(matchesQuestReference(progress, "medieval:guild_contract:help_blacksmith", template))
        assertFalse(matchesQuestReference(progress, "other quest", template))
        assertFalse(matchesQuestReference(null, "Q-101", template))
        assertFalse(matchesQuestReference(progress, " ", template))
    }

    private fun progress(templateId: String?, questCode: String?): PlayerQuestProgress =
        PlayerQuestProgress(
            templateId,
            questCode,
            QuestStatus.ACTIVE,
            0L,
            0L,
            0L,
            "",
            emptyMap(),
            emptyMap(),
        )

    private fun template(
        templateId: String,
        questCode: String,
        sourcePackId: String,
        mechanicId: String,
        progressionKind: String,
    ): ScenarioEngine.ScenarioTemplate =
        ScenarioEngine.ScenarioTemplate(ScenarioEngine.ScenarioType.QUEST).apply {
            this.templateId = templateId
            this.questCode = questCode
            this.sourcePackId = sourcePackId
            this.progressionMechanicId = mechanicId
            this.progressionKind = progressionKind
        }
}
