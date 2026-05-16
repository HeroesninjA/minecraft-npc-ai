package ro.ainpc.engine;

import org.junit.jupiter.api.Test;
import ro.ainpc.progression.ProgressionDefinition;
import ro.ainpc.story.StoryContextSnapshot;
import ro.ainpc.world.WorldContextSnapshot;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestDirectorTest {

    private final QuestDirector director = new QuestDirector();

    @Test
    void emptyStoryContextDoesNotStartQuestSelection() {
        QuestDirectorDecision decision = director.decide(new QuestDirectorRequest(
            StoryContextSnapshot.empty(),
            List.of(definition("market_investigation", "main_quests", "Market Investigation", true)),
            "",
            true,
            List.of()
        ));

        assertEquals(QuestDirectorDecision.Status.NO_ACTION, decision.status());
        assertEquals("story_context_does_not_request_quest", decision.reason());
        assertFalse(decision.runtimeExecutable());
        assertTrue(decision.selectedTemplateId().isBlank());
    }

    @Test
    void blockingReasonsWinBeforeTemplateSelection() {
        QuestDirectorDecision decision = director.decide(new QuestDirectorRequest(
            storyContext(List.of("persistent_place_state=market_unrest"), List.of()),
            List.of(definition("market_investigation", "main_quests", "Market Investigation", true)),
            "",
            true,
            List.of("missing quest_board anchor")
        ));

        assertEquals(QuestDirectorDecision.Status.BLOCKED, decision.status());
        assertEquals(List.of("missing quest_board anchor"), decision.blockedReasons());
        assertTrue(decision.selectedTemplateId().isBlank());
        assertFalse(decision.runtimeExecutable());
    }

    @Test
    void storyConditionBlockExplainsMissingCondition() {
        QuestDirectorDecision decision = director.decide(new QuestDirectorRequest(
            storyContext(List.of("quest_hook=festival_preparation"), List.of()),
            List.of(definition("festival_preparation", "village_events", "Festival Preparation", true)),
            "village_events",
            true,
            List.of("story_condition_missing: region_state=festival_ready")
        ));

        assertEquals(QuestDirectorDecision.Status.BLOCKED, decision.status());
        assertEquals("request_blocked", decision.reason());
        assertEquals(List.of("story_condition_missing: region_state=festival_ready"), decision.blockedReasons());
        assertTrue(decision.selectedTemplateId().isBlank());
        assertFalse(decision.runtimeExecutable());
    }

    @Test
    void storyDemandSelectsMatchingProgressionDefinitionReadOnly() {
        ProgressionDefinition market = definition(
            "market_investigation",
            "main_quests",
            "Market Investigation",
            true
        );
        ProgressionDefinition farm = definition(
            "farm_delivery",
            "village_contracts",
            "Farm Delivery",
            true
        );

        QuestDirectorDecision decision = director.decide(new QuestDirectorRequest(
            storyContext(List.of("persistent_place_state=market_unrest", "recent_story_event_type=vandalism"), List.of()),
            List.of(farm, market),
            "",
            false,
            List.of()
        ));

        assertEquals(QuestDirectorDecision.Status.CANDIDATE_FOUND, decision.status());
        assertEquals(market.templateId(), decision.selectedTemplateId());
        assertEquals(market.progressionId(), decision.selectedProgressionId());
        assertEquals("main_quests", decision.selectedMechanicId());
        assertTrue(decision.matchedSignals().contains("persistent_place_state=market_unrest"));
        assertTrue(decision.candidateTemplateIds().contains(market.templateId()));
        assertFalse(decision.runtimeExecutable());
    }

    @Test
    void storyDemandWithPreferredMechanicSelectsReadOnlyCandidate() {
        ProgressionDefinition contract = definition(
            "market_delivery",
            "village_contracts",
            "Market Delivery",
            true
        );
        ProgressionDefinition event = definition(
            "market_alarm_response",
            "village_events",
            "Market Alarm Response",
            true
        );

        QuestDirectorDecision decision = director.decide(new QuestDirectorRequest(
            storyContext(List.of("recent_story_event_type=market_alarm"), List.of()),
            List.of(contract, event),
            "village_events",
            true,
            List.of()
        ));

        assertEquals(QuestDirectorDecision.Status.CANDIDATE_FOUND, decision.status());
        assertEquals(event.templateId(), decision.selectedTemplateId());
        assertEquals("village_events", decision.selectedMechanicId());
        assertTrue(decision.matchedSignals().contains("preferred_mechanic=village_events"));
        assertTrue(decision.matchedSignals().contains("recent_story_event_type=market_alarm"));
        assertFalse(decision.runtimeExecutable());
    }

    @Test
    void storyDemandSuggestsSeedWhenNoTemplateMatchesAndSeedIsAllowed() {
        QuestDirectorDecision decision = director.decide(new QuestDirectorRequest(
            storyContext(List.of("place_danger=bandits"), List.of("mapping warning")),
            List.of(definition("market_investigation", "main_quests", "Market Investigation", true)),
            "",
            true,
            List.of()
        ));

        assertEquals(QuestDirectorDecision.Status.SEED_SUGGESTED, decision.status());
        assertEquals("no_matching_template_but_seed_allowed", decision.reason());
        assertEquals(List.of("mapping warning"), decision.warnings());
        assertTrue(decision.matchedSignals().contains("place_danger=bandits"));
        assertFalse(decision.runtimeExecutable());
    }

    @Test
    void storyDemandBlocksWhenNoTemplateMatchesAndSeedIsNotAllowed() {
        QuestDirectorDecision decision = director.decide(new QuestDirectorRequest(
            storyContext(List.of("place_conflict=stolen_relic"), List.of()),
            List.of(definition("farm_delivery", "village_contracts", "Farm Delivery", true)),
            "",
            false,
            List.of()
        ));

        assertEquals(QuestDirectorDecision.Status.BLOCKED, decision.status());
        assertEquals("no_matching_progression_definition", decision.reason());
        assertEquals(List.of("Nu exista template/progresie potrivita pentru story context."), decision.blockedReasons());
        assertFalse(decision.runtimeExecutable());
    }

    @Test
    void decisionIsImmutableAndNeverRuntimeExecutable() {
        QuestDirectorDecision decision = new QuestDirectorDecision(
            QuestDirectorDecision.Status.CANDIDATE_FOUND,
            " test ",
            " progression ",
            " template ",
            " mechanic ",
            " definition ",
            List.of(" signal "),
            List.of(" candidate "),
            List.of(" blocked "),
            List.of(" warning "),
            true
        );

        assertFalse(decision.runtimeExecutable());
        assertEquals("test", decision.reason());
        assertEquals(List.of("signal"), decision.matchedSignals());
        assertThrows(UnsupportedOperationException.class, () -> decision.matchedSignals().add("new"));
    }

    private StoryContextSnapshot storyContext(List<String> signals, List<String> warnings) {
        return new StoryContextSnapshot(
            "Mara",
            "merchant",
            "Hero",
            WorldContextSnapshot.empty(),
            null,
            null,
            List.of(),
            List.of(),
            signals,
            warnings
        );
    }

    private ProgressionDefinition definition(String id,
                                             String mechanicId,
                                             String displayName,
                                             boolean enabled) {
        return new ProgressionDefinition(
            "medieval:" + mechanicId + ":" + id,
            "medieval",
            mechanicId,
            "quest",
            id,
            "medieval:" + id,
            id.toUpperCase(),
            displayName,
            displayName + " description",
            "side",
            "investigation",
            "QUEST",
            mechanicId,
            "quest",
            "questuri",
            3,
            2,
            1,
            1,
            false,
            enabled
        );
    }
}
