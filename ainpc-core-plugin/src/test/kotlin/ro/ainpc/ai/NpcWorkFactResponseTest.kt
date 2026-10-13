package ro.ainpc.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NpcWorkFactResponseTest {
    @Test
    fun combinesOnlyVerifiedOccupationWorkplaceAndPlannedRoutine() {
        val facts = NpcWorkFactResponse.Facts("fierar", "work", "Fieraria satului")

        assertEquals(
            "Meseria mea este fierar. Lucrez la Fieraria satului. In program am: sa lucrez.",
            NpcWorkFactResponse.resolve(facts),
        )
    }

    @Test
    fun repeatedWorkAnswerAlternatesWhileKeepingTheSameFacts() {
        val facts = NpcWorkFactResponse.Facts("fierar", "work", "Fieraria satului")
        val first = NpcWorkFactResponse.resolve(facts)
        val second = NpcWorkFactResponse.resolve(facts, listOf(first))

        assertEquals(
            "Sunt fierar. Locul meu de munca este Fieraria satului. Mi-am planificat sa lucrez.",
            second,
        )
        assertEquals(first, NpcWorkFactResponse.resolve(facts, listOf(first, second)))
    }

    @Test
    fun missingWorkplaceIsNotInventedAndRoutineErrorsAreHidden() {
        val facts = NpcWorkFactResponse.Facts("brutar", "ancora work lipseste", null)
        val first = NpcWorkFactResponse.resolve(facts)

        assertEquals(
            "Meseria mea este brutar. Nu am un program stabilit acum.",
            first,
        )
        assertEquals(
            "Sunt brutar. Nu am un program stabilit acum.",
            NpcWorkFactResponse.resolve(facts, listOf(first)),
        )
    }

    @Test
    fun missingOccupationDoesNotProduceAWorkClaim() {
        val facts = NpcWorkFactResponse.Facts(null, "work", "Atelier")
        val first = NpcWorkFactResponse.resolve(facts)

        assertEquals("Nu am o meserie stabilita acum.", first)
        assertEquals("Deocamdata nu am o meserie stabilita.", NpcWorkFactResponse.resolve(facts, listOf(first)))
    }
}
