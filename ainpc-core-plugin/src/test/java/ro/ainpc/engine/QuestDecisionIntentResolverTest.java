package ro.ainpc.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ro.ainpc.engine.QuestDecisionIntentResolver.Intent.ACCEPT;
import static ro.ainpc.engine.QuestDecisionIntentResolver.Intent.DECLINE;
import static ro.ainpc.engine.QuestDecisionIntentResolver.Intent.NONE;
import static ro.ainpc.engine.QuestDecisionIntentResolver.Intent.OFFER;

class QuestDecisionIntentResolverTest {

    private final QuestDecisionIntentResolver resolver = new QuestDecisionIntentResolver();

    @Test
    void resolvesShortAcceptOnlyWhenQuestOfferIsActive() {
        assertEquals(ACCEPT, resolver.resolve("da", true));
        assertEquals(ACCEPT, resolver.resolve("ok", true));
        assertEquals(NONE, resolver.resolve("da", false));
    }

    @Test
    void resolvesNaturalAcceptPhrasesForActiveOffer() {
        assertEquals(ACCEPT, resolver.resolve("ma bag", true));
        assertEquals(ACCEPT, resolver.resolve("suna bine, o fac", true));
        assertEquals(ACCEPT, resolver.resolve("accept misiunea", false));
    }

    @Test
    void resolvesDeclinePhrasesWithoutMistakingQuestQuestionsForDeclines() {
        assertEquals(DECLINE, resolver.resolve("nu", true));
        assertEquals(DECLINE, resolver.resolve("poate mai tarziu", true));
        assertEquals(DECLINE, resolver.resolve("nu accept questul", false));
        assertEquals(OFFER, resolver.resolve("nu ai quest?", false));
    }

    @Test
    void normalizesRomanianDiacritics() {
        assertEquals(ACCEPT, resolver.resolve("mă bag", true));
        assertEquals(DECLINE, resolver.resolve("poate mai târziu", true));
    }
}
