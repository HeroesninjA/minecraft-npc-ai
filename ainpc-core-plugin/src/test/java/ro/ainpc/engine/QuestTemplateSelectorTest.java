package ro.ainpc.engine;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertSame;

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

    private ScenarioEngine.ScenarioTemplate template(String id) {
        ScenarioEngine.ScenarioTemplate template = new ScenarioEngine.ScenarioTemplate(ScenarioEngine.ScenarioType.QUEST);
        template.setTemplateId(id);
        template.setDisplayName(id);
        return template;
    }
}
