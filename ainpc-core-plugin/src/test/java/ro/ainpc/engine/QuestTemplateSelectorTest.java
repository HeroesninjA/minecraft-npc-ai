package ro.ainpc.engine;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestTemplateSelectorTest {

    @Test
    void prefersFirstAvailableUncompletedQuestOverEarlierCompletedQuest() {
        ScenarioEngine.ScenarioTemplate repeatableCompleted = template("Q04");
        ScenarioEngine.ScenarioTemplate nextChainQuest = template("Q07");

        ScenarioEngine.ScenarioTemplate selected = QuestTemplateSelector.selectConfiguredTemplate(
            List.of(repeatableCompleted, nextChainQuest),
            template -> true,
            template -> Set.of("Q04").contains(template.getTemplateId())
        );

        assertSame(nextChainQuest, selected);
    }

    @Test
    void skipsUnavailableEarlierQuestWhenLaterQuestIsAvailable() {
        ScenarioEngine.ScenarioTemplate completedStarterQuest = template("Q03");
        ScenarioEngine.ScenarioTemplate nextGuardQuest = template("Q08");

        ScenarioEngine.ScenarioTemplate selected = QuestTemplateSelector.selectConfiguredTemplate(
            List.of(completedStarterQuest, nextGuardQuest),
            template -> !"Q03".equals(template.getTemplateId()),
            template -> Set.of("Q03").contains(template.getTemplateId())
        );

        assertSame(nextGuardQuest, selected);
    }

    @Test
    void keepsCompletedAvailableQuestWhenNoFreshQuestExists() {
        ScenarioEngine.ScenarioTemplate repeatableCompleted = template("Q04");

        ScenarioEngine.ScenarioTemplate selected = QuestTemplateSelector.selectConfiguredTemplate(
            List.of(repeatableCompleted),
            template -> true,
            template -> Set.of("Q04").contains(template.getTemplateId())
        );

        assertSame(repeatableCompleted, selected);
    }

    @Test
    void returnsFirstUnavailableQuestWhenNothingIsAvailable() {
        ScenarioEngine.ScenarioTemplate firstUnavailable = template("Q01");
        ScenarioEngine.ScenarioTemplate secondUnavailable = template("Q06");

        ScenarioEngine.ScenarioTemplate selected = QuestTemplateSelector.selectConfiguredTemplate(
            List.of(firstUnavailable, secondUnavailable),
            template -> false,
            template -> false
        );

        assertSame(firstUnavailable, selected);
    }

    @Test
    void filtersMixedNpcTemplatesByProgressionKind() {
        ScenarioEngine.ScenarioTemplate contract = progressionTemplate("C02", "contract", "village_contracts", "contract", "contracte");
        ScenarioEngine.ScenarioTemplate duty = progressionTemplate("D01", "duty", "npc_duties", "sarcina", "sarcini");
        ScenarioEngine.ScenarioTemplate bounty = progressionTemplate("B01", "bounty", "local_bounties", "bounty", "bounty-uri");
        ScenarioEngine.ScenarioTemplate event = progressionTemplate("E01", "event", "village_events", "eveniment", "evenimente");
        ScenarioEngine.ScenarioTemplate tutorial = progressionTemplate("T01", "tutorial", "onboarding", "tutorial", "tutoriale");
        ScenarioEngine.ScenarioTemplate ritual = progressionTemplate("R01", "ritual", "village_rituals", "ritual", "ritualuri");

        List<ScenarioEngine.ScenarioTemplate> dutyTemplates = List.of(contract, duty, bounty, event, tutorial, ritual).stream()
            .filter(template -> QuestTemplateSelector.matchesProgressionKind(template, "duty", "Sarcini NPC"))
            .toList();

        ScenarioEngine.ScenarioTemplate selected = QuestTemplateSelector.selectConfiguredTemplate(
            dutyTemplates,
            template -> true,
            template -> false
        );

        assertSame(duty, selected);
        assertTrue(QuestTemplateSelector.matchesProgressionKind(duty, "npc_duties", "Sarcini NPC"));
        assertTrue(QuestTemplateSelector.matchesProgressionKind(bounty, "local_bounties", "Bounty locale"));
        assertTrue(QuestTemplateSelector.matchesProgressionKind(event, "village_events", "Evenimente locale"));
        assertTrue(QuestTemplateSelector.matchesProgressionKind(tutorial, "onboarding", "Tutoriale"));
        assertTrue(QuestTemplateSelector.matchesProgressionKind(ritual, "village_rituals", "Ritualuri locale"));
        assertFalse(QuestTemplateSelector.matchesProgressionKind(contract, "duty", "Contracte locale"));
        assertFalse(QuestTemplateSelector.matchesProgressionKind(bounty, "duty", "Bounty locale"));
        assertFalse(QuestTemplateSelector.matchesProgressionKind(event, "duty", "Evenimente locale"));
        assertFalse(QuestTemplateSelector.matchesProgressionKind(tutorial, "duty", "Tutoriale"));
        assertFalse(QuestTemplateSelector.matchesProgressionKind(ritual, "duty", "Ritualuri locale"));
    }

    private ScenarioEngine.ScenarioTemplate template(String id) {
        ScenarioEngine.ScenarioTemplate template = new ScenarioEngine.ScenarioTemplate(ScenarioEngine.ScenarioType.QUEST);
        template.setTemplateId(id);
        template.setDisplayName(id);
        return template;
    }

    private ScenarioEngine.ScenarioTemplate progressionTemplate(String id,
                                                                String kind,
                                                                String mechanic,
                                                                String singular,
                                                                String plural) {
        ScenarioEngine.ScenarioTemplate template = template(id);
        template.setProgressionKind(kind);
        template.setProgressionMechanicId(mechanic);
        template.setProgressionSingularLabel(singular);
        template.setProgressionPluralLabel(plural);
        return template;
    }
}
