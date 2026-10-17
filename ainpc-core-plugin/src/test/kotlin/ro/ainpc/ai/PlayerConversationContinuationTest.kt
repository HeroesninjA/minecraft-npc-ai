package ro.ainpc.ai

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlayerConversationContinuationTest {
    @Test
    fun `relative option answers use the last NPC question without choosing locally`() {
        val history = listOf(DialogHistory("Ce meserie ai?", "Vrei detalii despre atelier sau despre program?", 1L))

        for (message in listOf(
            "Prima", "Primul", "Prima opțiune", "A doua, te rog!", "A doua variantă", "Cea de-a doua",
            "A treia", "Ultima", "Ultimul", "Ambele", "Amândouă", "Niciuna", "Niciunul",
        )) {
            assertTrue(PlayerConversationContinuation.isShortAnswer(message), message)
            assertEquals(history.single(), PlayerConversationContinuation.answeredQuestion(message, history), message)
            assertTrue(PlayerConversationContinuation.needsContext(message, history), message)
            assertFalse(PlayerConversationContinuation.needsContext(message, emptyList()), message)
        }
    }

    @Test
    fun `relative choices do not turn complete questions or quantity replies into option selections`() {
        val history = listOf(DialogHistory("Ce meserie ai?", "Vrei detalii despre atelier sau despre program?", 1L))

        for (message in listOf(
            "A doua?", "Prima opțiune, dar unde este piața?", "Care este a doua opțiune?",
            "Prima dată când am venit", "Ambele magazine sunt închise.", "Două", "2", "Nu prima",
        )) {
            assertFalse(PlayerConversationContinuation.isShortAnswer(message), message)
            assertNull(PlayerConversationContinuation.answeredQuestion(message, history), message)
        }
        val otherHistory = listOf(DialogHistory("Salut", "Bună.", 1L))
        assertNull(PlayerConversationContinuation.answeredQuestion("A doua", otherHistory))
        assertEquals(history.single(), PlayerConversationContinuation.answeredQuestion("A doua", history))
        assertNull(PlayerConversationContinuation.answeredQuestion("A doua", listOf(
            history.single().copy(branchStatus = DialogHistory.BranchStatus.FAILED),
        )))
    }

    @Test
    fun `short answers use the NPC question even after a single exchange`() {
        val history = listOf(DialogHistory("Ce meserie ai?", "Sunt fierar. Vrei detalii despre atelier?", 1L))

        for (message in listOf("Da", "Da, te rog!", "Nu", "Nu, mulțumesc.", "Desigur", "De acord", "Mai bine nu")) {
            assertTrue(PlayerConversationContinuation.needsContext(message, history), message)
            assertFalse(PlayerConversationContinuation.templates(message, history).isNullOrEmpty(), message)
        }
        assertTrue(PlayerConversationContinuation.needsContext(
            "Da", listOf(DialogHistory("Continuă", "Vrei să lămurim ceva anume?", 1L)),
        ))
    }

    @Test
    fun `short answers require the latest stable NPC reply to contain a question`() {
        val question = DialogHistory("Ce meserie ai?", "Vrei detalii despre atelier?", 1L)
        val completed = DialogHistory("Continuă", "Sunt fierar.", 2L)

        assertFalse(PlayerConversationContinuation.needsContext("Da", emptyList()))
        assertFalse(PlayerConversationContinuation.needsContext("Da", listOf(question, completed)))
        assertFalse(PlayerConversationContinuation.needsContext("Nu", listOf(completed, question)))
        for (status in listOf(DialogHistory.BranchStatus.FAILED, DialogHistory.BranchStatus.REJECTED, DialogHistory.BranchStatus.PROPOSED)) {
            assertFalse(PlayerConversationContinuation.needsContext("Da", listOf(question.copy(branchStatus = status))))
        }
        assertFalse(PlayerConversationContinuation.needsContext("Da", listOf(question.copy(npcResponse = " "))))
        assertTrue(PlayerConversationContinuation.needsContext(
            "Da", listOf(question, completed.copy(branchStatus = DialogHistory.BranchStatus.FAILED)),
        ))
    }

    @Test
    fun `confirmation prefixes do not hide a new question or an uncertain response`() {
        val history = listOf(DialogHistory("Ce meserie ai?", "Vrei detalii?", 1L))

        for (message in listOf("Da?", "Nu?", "Da, unde este piața?", "Nu, ce meserie ai?", "Nu știu", "Darius")) {
            assertFalse(PlayerConversationContinuation.needsContext(message, history), message)
            assertNull(PlayerConversationContinuation.templates(message, history), message)
        }
    }

    @Test
    fun `short follow up uses context after the first completed exchange`() {
        val history = listOf(DialogHistory("Ce meserie ai?", "Sunt fierar.", 1L))

        for (message in listOf("Continuă!", "De ce?", "Spune-mi mai mult, te rog.", "Te rog, explică mai clar.")) {
            assertTrue(PlayerConversationContinuation.needsContext(message, history), message)
        }
    }

    @Test
    fun `explicit reply complaints preserve the conversation context`() {
        val history = listOf(DialogHistory("Ce meserie ai?", "Sunt fierar.", 1L))

        for (message in listOf("Ai repetat răspunsul!", "Te rog, nu mi-ai răspuns.", "Nu asta am întrebat", "Te repeți")) {
            assertTrue(PlayerConversationContinuation.isRepairRequest(message), message)
            assertTrue(PlayerConversationContinuation.needsContext(message, history), message)
            assertTrue(PlayerConversationContinuation.templates(message, history)!!.all { it.contains("o meserie") }, message)
        }
        assertFalse(PlayerConversationContinuation.needsContext("Nu mi-ai raspuns", emptyList()))
    }

    @Test
    fun `complaints followed by dependent clarification preserve the existing topic`() {
        val history = listOf(DialogHistory("Ce meserie ai?", "Sunt fierar.", 1L))
        for (message in listOf(
            "Nu mi-ai răspuns, de ce?", "Te rog, ai repetat răspunsul; explică mai clar.",
            "Nu asta am întrebat! Nu mi-ai răspuns, spune-mi mai mult.",
        )) {
            assertTrue(PlayerConversationContinuation.isFollowUp(message), message)
            assertTrue(PlayerConversationContinuation.isRepairRequest(message), message)
            assertTrue(PlayerConversationContinuation.needsContext(message, history), message)
            assertNull(PlayerConversationContinuation.explicitRepairQuestion(message), message)
            assertTrue(PlayerConversationContinuation.templates(message, history)!!.all { it.contains("o meserie") }, message)
            assertFalse(PlayerConversationContinuation.needsContext(message, emptyList()), message)
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
        for (message in listOf(
            "De ce ploua?", "Continua reparatia podului.", "Ce meserie ai?",
            "Nu mi-ai raspuns, unde este piata?", "Ai repetat raspunsul lui?",
        )) {
            assertNull(PlayerConversationContinuation.templates(message, emptyList()), message)
            assertFalse(PlayerConversationContinuation.isRepairRequest(message), message)
        }
    }

    @Test
    fun `mixed repair extracts the current question with its original details`() {
        for ((message, question) in listOf(
            "Nu mi-ai răspuns, unde este piața?" to "unde este piața?",
            "Te rog, nu asta am întrebat: Cum ajung la Podul Vechi?" to "Cum ajung la Podul Vechi?",
            "Ai repetat răspunsul! Ce meserie ai și unde lucrezi?" to "Ce meserie ai și unde lucrezi?",
            "Te repeți; când se deschide piața" to "când se deschide piața",
            "Nu mi-ai răspuns. Te repeți, unde este piața?" to "unde este piața?",
        )) {
            assertEquals(question, PlayerConversationContinuation.explicitRepairQuestion(message), message)
            assertFalse(PlayerConversationContinuation.isFollowUp(message), message)
        }
    }

    @Test
    fun `repair extraction requires a standalone complaint prefix and a new question`() {
        for (message in listOf(
            "Nu mi-ai raspuns", "Nu mi-ai raspuns!", "Nu mi-ai raspuns, sunt obosit.",
            "Ai repetat raspunsul lui? Unde este piata?", "El spune ca te repeti, unde este piata?",
            "Nu mi-ai raspuns, de ce?", "Unde este piata?", "Nu mi-ai raspuns unde este piata?",
            "Nu mi-ai raspuns, ???",
        )) {
            assertNull(PlayerConversationContinuation.explicitRepairQuestion(message), message)
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
