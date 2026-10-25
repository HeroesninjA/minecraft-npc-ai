package ro.ainpc.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import ro.ainpc.npc.NpcFoodRequestPreferences

class NpcFoodRequestReplyTest {
    @Test
    fun `direct food replies support Romanian spelling and an exact leading NPC address`() {
        for (message in listOf("Nu pot acum.", "Bob, nu am mancare!", "Negustorul, nu am paine.")) {
            assertEquals(NpcFoodRequestPreferences.Reply.DECLINE, NpcFoodRequestReply.parse(message, listOf("Bob", "Negustorul")))
        }
        for (message in listOf("Îți aduc mâncare!", "BOB, iti aduc un mar.", "Vin cu mancare")) {
            assertEquals(NpcFoodRequestPreferences.Reply.PROMISE, NpcFoodRequestReply.parse(message, listOf("Bob")))
        }
    }

    @Test
    fun `quotes reports questions conflicting promises and another addressee are not food replies`() {
        for (message in listOf(
            "Da", "Nu", "Maria, nu pot acum", "Bob si Maria, nu pot acum", "Ti-am dat mancare",
            "Nu iti aduc mancare", "Poate iti aduc mancare", "Iti aduc mancare?", "Nu am mancare?",
            "Maria a zis nu pot acum", "Am auzit ca iti aduc mancare", "\"Nu pot acum\"",
            "Nu pot acum, dar iti aduc mancare", "Ce inseamna nu pot acum?", "nu pot acum".repeat(30),
        )) {
            assertNull(NpcFoodRequestReply.parse(message, listOf("Bob")), message)
        }
    }
}
