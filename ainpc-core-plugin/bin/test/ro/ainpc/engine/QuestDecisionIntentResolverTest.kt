package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ro.ainpc.engine.QuestDecisionIntentResolver.Intent.ACCEPT
import ro.ainpc.engine.QuestDecisionIntentResolver.Intent.DECLINE
import ro.ainpc.engine.QuestDecisionIntentResolver.Intent.NONE
import ro.ainpc.engine.QuestDecisionIntentResolver.Intent.OFFER

class QuestDecisionIntentResolverTest {
    private val resolver = QuestDecisionIntentResolver()

    @Test
    fun resolvesShortAcceptOnlyWhenQuestOfferIsActive() {
        assertEquals(ACCEPT, resolver.resolve("da", true))
        assertEquals(ACCEPT, resolver.resolve("ok", true))
        assertEquals(NONE, resolver.resolve("da", false))
    }

    @Test
    fun resolvesNaturalAcceptPhrasesForActiveOffer() {
        assertEquals(ACCEPT, resolver.resolve("ma bag", true))
        assertEquals(ACCEPT, resolver.resolve("suna bine, o fac", true))
        assertEquals(ACCEPT, resolver.resolve("accept misiunea", false))
    }

    @Test
    fun resolvesDeclinePhrasesWithoutMistakingQuestQuestionsForDeclines() {
        assertEquals(DECLINE, resolver.resolve("nu", true))
        assertEquals(DECLINE, resolver.resolve("poate mai tarziu", true))
        assertEquals(DECLINE, resolver.resolve("nu accept questul", false))
        assertEquals(OFFER, resolver.resolve("nu ai quest?", false))
    }

    @Test
    fun normalizesRomanianDiacritics() {
        assertEquals(ACCEPT, resolver.resolve("mă bag", true))
        assertEquals(DECLINE, resolver.resolve("poate mai târziu", true))
    }
}
