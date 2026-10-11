package ro.ainpc.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PlayerConversationRecallTest {
    @Test
    fun `recalls last stable topic without repeating message content`() {
        val history = listOf(
            DialogHistory("Unde este satul?", "La nord.", 1L),
            DialogHistory("Ce meserie ai?", "Sunt fierar.", 2L),
        )

        assertEquals(
            "In ultimul schimb disponibil, am vorbit despre o meserie.",
            PlayerConversationRecall.resolve("Despre ce am vorbit data trecută?", history)
        )
    }

    @Test
    fun `skips repeated recap requests and unstable history`() {
        val history = listOf(
            DialogHistory("Unde este satul?", "La nord.", 1L),
            DialogHistory("Ce am discutat?", "Despre sat.", 2L),
            DialogHistory("Ce pret are cheia?", "Nu stiu.", 3L, DialogHistory.BranchStatus.FAILED),
        )

        assertEquals(
            "In ultimul schimb disponibil, am vorbit despre un loc.",
            PlayerConversationRecall.resolve("Ce am discutat?", history)
        )
    }

    @Test
    fun `does not invent topic when history is missing or unclear`() {
        assertEquals(
            "Nu am un schimb anterior disponibil despre care sa-ti povestesc.",
            PlayerConversationRecall.resolve("Ce am discutat?", emptyList())
        )
        assertEquals(
            "Imi amintesc ca am vorbit, dar nu pot spune sigur care a fost subiectul.",
            PlayerConversationRecall.resolve(
                "Ce am discutat?", listOf(DialogHistory("Piatra verde", "Da.", 1L))
            )
        )
    }

    @Test
    fun `ordinary conversation is not intercepted`() {
        assertNull(PlayerConversationRecall.resolve(
            "Cat costa sabia?", listOf(DialogHistory("Salut", "Buna", 1L))
        ))
    }
}
