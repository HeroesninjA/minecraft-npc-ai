package ro.ainpc.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ro.ainpc.npc.NPCState

class SocialActionOutcomeTest {
    @Test
    fun `nearby interlocutor lets npc socialize without a social destination`() {
        assertEquals(NPCState.SOCIALIZING, SocialActionOutcome.state(false, true))
        assertEquals("petreaca timp cu cei din apropiere",
            SocialActionOutcome.goal(false, true, "locul de intalnire"))
    }

    @Test
    fun `social destination is used only when there is no nearby interlocutor`() {
        assertEquals(NPCState.WALKING, SocialActionOutcome.state(false, false))
        assertEquals("ajunga la piata", SocialActionOutcome.goal(false, false, "piata"))
        assertEquals(NPCState.SOCIALIZING, SocialActionOutcome.state(true, false))
        assertEquals("petreaca timp cu ceilalti localnici",
            SocialActionOutcome.goal(true, false, "piata"))
    }
}
