package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class QuestTemplateSelectorTest {
    @Test
    fun prefersFirstAvailableUncompletedQuestOverEarlierCompletedQuest() {
        val repeatableCompleted = template("Q04")
        val nextChainQuest = template("Q07")

        val selected = QuestTemplateSelector.selectConfiguredTemplate(
            listOf(repeatableCompleted, nextChainQuest),
            { true },
            { setOf("Q04").contains(it.templateId) }
        )

        assertSame(nextChainQuest, selected)
    }

    @Test
    fun skipsUnavailableEarlierQuestWhenLaterQuestIsAvailable() {
        val completedStarterQuest = template("Q03")
        val nextGuardQuest = template("Q08")

        val selected = QuestTemplateSelector.selectConfiguredTemplate(
            listOf(completedStarterQuest, nextGuardQuest),
            { !"Q03".equals(it.templateId) },
            { setOf("Q03").contains(it.templateId) }
        )

        assertSame(nextGuardQuest, selected)
    }

    @Test
    fun keepsCompletedAvailableQuestWhenNoFreshQuestExists() {
        val repeatableCompleted = template("Q04")

        val selected = QuestTemplateSelector.selectConfiguredTemplate(
            listOf(repeatableCompleted),
            { true },
            { setOf("Q04").contains(it.templateId) }
        )

        assertSame(repeatableCompleted, selected)
    }

    @Test
    fun returnsFirstUnavailableQuestWhenNothingIsAvailable() {
        val firstUnavailable = template("Q01")
        val secondUnavailable = template("Q06")

        val selected = QuestTemplateSelector.selectConfiguredTemplate(
            listOf(firstUnavailable, secondUnavailable),
            { false },
            { false }
        )

        assertSame(firstUnavailable, selected)
    }

    @Test
    fun filtersMixedNpcTemplatesByProgressionKind() {
        val contract = progressionTemplate("C02", "contract", "village_contracts", "contract", "contracte")
        val duty = progressionTemplate("D01", "duty", "npc_duties", "sarcina", "sarcini")
        val bounty = progressionTemplate("B01", "bounty", "local_bounties", "bounty", "bounty-uri")
        val event = progressionTemplate("E01", "event", "village_events", "eveniment", "evenimente")
        val tutorial = progressionTemplate("T01", "tutorial", "onboarding", "tutorial", "tutoriale")
        val ritual = progressionTemplate("R01", "ritual", "village_rituals", "ritual", "ritualuri")

        val dutyTemplates = listOf(contract, duty, bounty, event, tutorial, ritual)
            .filter { QuestTemplateSelector.matchesProgressionKind(it, "duty", "Sarcini NPC") }

        val selected = QuestTemplateSelector.selectConfiguredTemplate(dutyTemplates, { true }, { false })

        assertSame(duty, selected)
        assertTrue(QuestTemplateSelector.matchesProgressionKind(duty, "npc_duties", "Sarcini NPC"))
        assertTrue(QuestTemplateSelector.matchesProgressionKind(bounty, "local_bounties", "Bounty locale"))
        assertTrue(QuestTemplateSelector.matchesProgressionKind(event, "village_events", "Evenimente locale"))
        assertTrue(QuestTemplateSelector.matchesProgressionKind(tutorial, "onboarding", "Tutoriale"))
        assertTrue(QuestTemplateSelector.matchesProgressionKind(ritual, "village_rituals", "Ritualuri locale"))
        assertFalse(QuestTemplateSelector.matchesProgressionKind(contract, "duty", "Contracte locale"))
        assertFalse(QuestTemplateSelector.matchesProgressionKind(bounty, "duty", "Bounty locale"))
        assertFalse(QuestTemplateSelector.matchesProgressionKind(event, "duty", "Evenimente locale"))
        assertFalse(QuestTemplateSelector.matchesProgressionKind(tutorial, "duty", "Tutoriale"))
        assertFalse(QuestTemplateSelector.matchesProgressionKind(ritual, "duty", "Ritualuri locale"))
    }

    private fun template(id: String): ScenarioEngine.ScenarioTemplate {
        val template = ScenarioEngine.ScenarioTemplate(ScenarioEngine.ScenarioType.QUEST)
        template.templateId = id
        template.displayName = id
        return template
    }

    private fun progressionTemplate(id: String, kind: String, mechanic: String, singular: String, plural: String): ScenarioEngine.ScenarioTemplate {
        val template = template(id)
        template.progressionKind = kind
        template.progressionMechanicId = mechanic
        template.progressionSingularLabel = singular
        template.progressionPluralLabel = plural
        return template
    }
}
