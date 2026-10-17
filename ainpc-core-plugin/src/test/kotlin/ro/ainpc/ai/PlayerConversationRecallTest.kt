package ro.ainpc.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PlayerConversationRecallTest {
    @Test
    fun `relative choice replies preserve the conversation subject for later continuation`() {
        val subject = DialogHistory("Ce meserie ai?", "Vrei detalii despre atelier sau despre program?", 1L)
        val history = listOf(
            subject,
            DialogHistory("A doua", "Vrei programul de dimineață sau de seară?", 2L),
            DialogHistory("Ambele", "Nu am programul complet confirmat.", 3L),
        )

        assertEquals(subject, PlayerConversationRecall.lastSubjectExchange(history))
        assertEquals(
            "In ultimul schimb disponibil, am vorbit despre o meserie.",
            PlayerConversationRecall.resolve("Ce am discutat?", history),
        )
    }

    @Test
    fun `answers to NPC clarification preserve the earlier substantive subject`() {
        val subject = DialogHistory("Ce meserie ai?", "Sunt fierar. Vrei detalii despre atelier?", 1L)
        val history = listOf(subject, DialogHistory("Da, te rog", "Repar unelte la atelier.", 2L))

        assertEquals(subject, PlayerConversationRecall.lastSubjectExchange(history))
        assertEquals(
            "In ultimul schimb disponibil, am vorbit despre o meserie.",
            PlayerConversationRecall.resolve("Ce am discutat?", history),
        )
        val unrelatedAnswer = history.last().copy(playerMessage = "Da, unde este piața?")
        assertEquals(unrelatedAnswer, PlayerConversationRecall.lastSubjectExchange(listOf(subject, unrelatedAnswer)))
        val withoutQuestion = subject.copy(npcResponse = "Sunt fierar.")
        assertEquals(history.last(), PlayerConversationRecall.lastSubjectExchange(listOf(withoutQuestion, history.last())))
    }

    @Test
    fun `explicit replay preserves the last NPC reply exactly`() {
        val response = "Piața este lângă fierărie. Întreabă-l pe Ion!"
        val history = listOf(DialogHistory("Unde este piața?", response, 1L))

        for (message in listOf(
            "Repetă exact!", "Te rog, repetă ultima replică.", "Repetă exact ultimul răspuns, te rog.",
            "Ce ai spus?", "Ce ai spus ultima dată?", "  REPETĂ   EXACT  CE AI SPUS! ",
        )) {
            assertEquals(response, PlayerConversationRecall.resolve(message, history), message)
            assertEquals(response, PlayerConversationRecall.resolveForFallback(message, history), message)
        }
    }

    @Test
    fun `replay selects the latest stable nonempty exchange from unordered history`() {
        val history = listOf(
            DialogHistory("Unde este piața?", "Lângă fierărie.", 2L, DialogHistory.BranchStatus.SELECTED),
            DialogHistory("Salut", "Bună!", 1L),
            DialogHistory("Mesaj eșuat", "Nu trebuie redat.", 3L, DialogHistory.BranchStatus.FAILED),
            DialogHistory("Mesaj respins", "Nu trebuie redat.", 4L, DialogHistory.BranchStatus.REJECTED),
            DialogHistory("Mesaj propus", "Nu trebuie redat.", 5L, DialogHistory.BranchStatus.PROPOSED),
            DialogHistory("Mesaj fără răspuns", " ", 6L),
            DialogHistory(" ", "Răspuns fără mesaj.", 7L),
        )

        assertEquals("Lângă fierărie.", PlayerConversationRecall.resolve("Repetă exact", history))
    }

    @Test
    fun `replay uses only the supplied conversation and admits missing history`() {
        val firstHistory = listOf(DialogHistory("Unde e piața?", "La nord.", 1L))
        val secondHistory = listOf(DialogHistory("Unde e piața?", "La sud.", 1L))

        assertEquals("La nord.", PlayerConversationRecall.resolve("Ce ai spus?", firstHistory))
        assertEquals("La sud.", PlayerConversationRecall.resolve("Ce ai spus?", secondHistory))
        assertEquals(
            "Nu am o replica anterioara disponibila pe care sa o repet.",
            PlayerConversationRecall.resolve("Ce ai spus?", emptyList()),
        )
    }

    @Test
    fun `replay rejects invalid stored replies without substituting an older answer`() {
        for (response in listOf("{\"reply\":\"La nord.\"}", "{{raspuns}}", "<script>eroare</script>", "A".repeat(501))) {
            val history = listOf(
                DialogHistory("Unde e piața?", "La nord.", 1L),
                DialogHistory("Cum ajung acolo?", response, 2L),
            )

            assertEquals(
                "Nu pot reda exact ultima replica disponibila.",
                PlayerConversationRecall.resolve("Repetă exact", history),
            )
        }
    }

    @Test
    fun `replay returns the last explanation rather than the answer that opened the topic`() {
        val history = listOf(
            DialogHistory("Ce meserie ai?", "Sunt fierar.", 1L),
            DialogHistory("Continuă", "Repar unelte la atelier.", 2L),
            DialogHistory("Repetă exact", "Repar unelte la atelier.", 3L),
        )

        assertEquals("Repar unelte la atelier.", PlayerConversationRecall.resolve("Repetă ultima replică", history))
    }

    @Test
    fun `replay does not intercept explanations complaints or qualified questions`() {
        val history = listOf(DialogHistory("Unde e piața?", "La nord.", 1L))

        for (message in listOf(
            "Explică din nou", "Ai repetat răspunsul", "Nu repeta exact", "Ce ai spus despre fierărie?",
            "Repetă exact, apoi spune-mi unde este piața.", "Ce ai spus? Unde mergi?",
        )) {
            assertNull(PlayerConversationRecall.resolve(message, history), message)
            assertNull(PlayerConversationRecall.resolveForFallback(message, history), message)
        }
    }

    @Test
    fun `replay turns preserve the topic for recap and continuation`() {
        val subject = DialogHistory("Ce meserie ai?", "Sunt fierar.", 1L)
        val history = listOf(
            subject,
            DialogHistory("Repetă exact", "Sunt fierar.", 2L),
            DialogHistory("Ce ai spus?", "Sunt fierar.", 3L),
        )

        assertEquals(subject, PlayerConversationRecall.lastSubjectExchange(history))
        assertEquals(
            "In ultimul schimb disponibil, am vorbit despre o meserie.",
            PlayerConversationRecall.resolve("Ce am discutat?", history),
        )
        val replayOnlyHistory = (2L..6L).map { DialogHistory("Repetă exact", "Sunt fierar.", it) }
        assertNull(PlayerConversationRecall.lastSubjectExchange(listOf(subject) + replayOnlyHistory))
    }

    @Test
    fun `recall uses the latest exchange even when caller history is unordered`() {
        val history = listOf(
            DialogHistory("Ai vazut ploaia?", "Da.", 2L),
            DialogHistory("Ce meserie ai?", "Sunt fierar.", 1L),
        )

        assertEquals(
            "In ultimul schimb disponibil, am vorbit despre vreme.",
            PlayerConversationRecall.resolve("Ce am discutat?", history)
        )
    }

    @Test
    fun `short follow ups preserve the last substantive topic`() {
        val history = listOf(
            DialogHistory("Ce meserie ai?", "Sunt fierar.", 1L),
            DialogHistory("Continuă!", "Ce detaliu te intereseaza?", 2L),
            DialogHistory("Spune-mi mai mult.", "Despre meserie?", 3L),
        )

        assertEquals(
            "In ultimul schimb disponibil, am vorbit despre o meserie.",
            PlayerConversationRecall.resolve("Ce am discutat?", history)
        )
    }

    @Test
    fun `greeting prefix does not become the recalled topic`() {
        assertEquals(
            "In ultimul schimb disponibil, am vorbit despre vreme.",
            PlayerConversationRecall.resolve("Ce am discutat?", listOf(
                DialogHistory("Salut, ai vazut ploaia?", "Da.", 1L)
            ))
        )
    }

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
    fun `subject specific recap reaches contextual dialogue instead of a category answer`() {
        val history = listOf(
            DialogHistory("Ce s-a intamplat la fierarie?", "Am gasit usa incuiata.", 1L),
        )

        assertNull(PlayerConversationRecall.resolve("Ce am discutat despre fierarie?", history))
        assertEquals(
            "Nu pot confirma din schimburile recente ce am discutat despre acel subiect.",
            PlayerConversationRecall.resolveForFallback("Ce am discutat despre fierarie?", history),
        )
        assertEquals(
            "Nu am un schimb anterior disponibil despre care sa-ti povestesc.",
            PlayerConversationRecall.resolve("Ce am discutat despre fierarie?", emptyList()),
        )
        assertEquals(
            "Imi amintesc ca am vorbit, dar nu pot spune sigur care a fost subiectul.",
            PlayerConversationRecall.resolve("Te rog, ce am discutat?", history),
        )
    }

    @Test
    fun `provider fallback only recalls stable local category`() {
        val history = listOf(
            DialogHistory("Ce meserie ai?", "Sunt fierar.", 1L),
            DialogHistory("Ce am discutat?", "Despre meserie.", 2L),
            DialogHistory("Unde este satul?", "La nord.", 3L, DialogHistory.BranchStatus.FAILED),
        )

        assertNull(PlayerConversationRecall.resolve("Ce am discutat despre meserie?", history))
        assertEquals(
            "In schimburile recente disponibile, am vorbit despre o meserie.",
            PlayerConversationRecall.resolveForFallback("Ce am discutat despre meserie?", history),
        )
        assertNull(PlayerConversationRecall.resolve("Ce am discutat despre ce meserie ai?", history))
        assertEquals(
            "In schimburile recente disponibile, am vorbit despre o meserie.",
            PlayerConversationRecall.resolveForFallback("Ce am discutat despre ce meserie ai?", history),
        )
        assertEquals(
            "Nu am un schimb anterior disponibil despre care sa-ti povestesc.",
            PlayerConversationRecall.resolveForFallback("Ce am discutat despre meserie?", emptyList()),
        )
        assertEquals(
            "Nu pot confirma din schimburile recente ce am discutat despre acel subiect.",
            PlayerConversationRecall.resolveForFallback("Ce am discutat despre unde este satul?", history),
        )
        assertNull(PlayerConversationRecall.resolveForFallback("Cat costa sabia?", history))
    }

    @Test
    fun `qualified fallback finds earlier matching category and does not use unrelated latest topic`() {
        val history = listOf(
            DialogHistory("Ce meserie ai?", "Sunt fierar.", 1L),
            DialogHistory("Ai vazut ploaia?", "Da.", 2L),
        )

        assertEquals(
            "In schimburile recente disponibile, am vorbit despre o meserie.",
            PlayerConversationRecall.resolveForFallback("Ce am discutat despre meserie?", history),
        )
        assertEquals(
            "Nu pot confirma din schimburile recente ce am discutat despre acel subiect.",
            PlayerConversationRecall.resolveForFallback("Ce am discutat despre comert?", history),
        )
        assertEquals(
            "In ultimul schimb disponibil, am vorbit despre vreme.",
            PlayerConversationRecall.resolveForFallback("Ce am discutat data trecuta?", history),
        )
    }

    @Test
    fun `subject specific recap does not replace the earlier conversation topic`() {
        val history = listOf(
            DialogHistory("Ce meserie ai?", "Sunt fierar.", 1L),
            DialogHistory("Ce am discutat despre fierarie?", "Am vorbit despre lucru.", 2L),
        )

        assertEquals(
            "In ultimul schimb disponibil, am vorbit despre o meserie.",
            PlayerConversationRecall.resolve("Ce am discutat?", history),
        )
    }

    @Test
    fun `ordinary conversation is not intercepted`() {
        assertNull(PlayerConversationRecall.resolve(
            "Cat costa sabia?", listOf(DialogHistory("Salut", "Buna", 1L))
        ))
    }
}
