package ro.ainpc.ai

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlayerConversationContinuationTest {
    @Test
    fun `short follow up uses context after the first completed exchange`() {
        val history = listOf(DialogHistory("Ce meserie ai?", "Sunt fierar.", 1L))

        for (message in listOf("Continuă!", "De ce?", "Spune-mi mai mult, te rog.", "Te rog, explică mai clar.")) {
            assertTrue(PlayerConversationContinuation.needsContext(message, history), message)
        }
    }

    @Test
    fun `missing failed or empty replies do not count as usable context`() {
        assertFalse(PlayerConversationContinuation.needsContext("Continua", emptyList()))
        for (exchange in listOf(
            DialogHistory("Ce meserie ai?", "Sunt fierar.", 1L, DialogHistory.BranchStatus.FAILED),
            DialogHistory("Ce meserie ai?", "", 1L),
            DialogHistory("Continua", "La ce te referi?", 1L),
        )) {
            assertFalse(PlayerConversationContinuation.needsContext("Continua", listOf(exchange)))
        }
    }

    @Test
    fun `new explicit questions are not intercepted as follow ups`() {
        for (message in listOf("De ce ploua?", "Continua reparatia podului.", "Ce meserie ai?")) {
            assertNull(PlayerConversationContinuation.templates(message, emptyList()), message)
        }
    }

    @Test
    fun `repeated follow ups and recap requests preserve the original topic`() {
        val history = listOf(
            DialogHistory("Ce meserie ai?", "Sunt fierar.", 1L),
            DialogHistory("Continua", "Ce vrei sa afli?", 2L),
            DialogHistory("Ce am discutat?", "Despre o meserie.", 3L),
        )

        val templates = PlayerConversationContinuation.templates("Mai multe detalii", history)!!

        assertTrue(templates.all { it.contains("o meserie") })
    }

    @Test
    fun `unclear new topic does not revive an older topic`() {
        val history = listOf(
            DialogHistory("Ce meserie ai?", "Sunt fierar.", 1L),
            DialogHistory("Piatra verde", "Te ascult.", 2L),
        )

        val templates = PlayerConversationContinuation.templates("Continua", history)!!

        assertTrue(templates.none { it.contains("meserie") || it.contains("Piatra verde") })
    }

    @Test
    fun `conversation topics stay isolated between supplied player histories`() {
        val workHistory = listOf(DialogHistory("Ce meserie ai?", "Sunt fierar.", 1L))
        val weatherHistory = listOf(DialogHistory("Ai vazut ploaia?", "Da.", 1L))

        assertTrue(PlayerConversationContinuation.templates("Continua", workHistory)!!.all { it.contains("o meserie") })
        assertTrue(PlayerConversationContinuation.templates("Continua", weatherHistory)!!.all { it.contains("vreme") })
        assertTrue(PlayerConversationContinuation.templates("Continua", workHistory)!!.all { it.contains("o meserie") })
    }
}
