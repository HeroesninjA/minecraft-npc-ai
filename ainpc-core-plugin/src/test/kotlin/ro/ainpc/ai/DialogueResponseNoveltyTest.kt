package ro.ainpc.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DialogueResponseNoveltyTest {
    private val prompt = "=== CONVERSATIA RECENTA ===\nIon: Am o veste.\n\nIon: "
    private val history = listOf(DialogHistory("Ce se intampla?", "Am o veste.", 1L))

    @Test
    fun `relative option selection retries an AI reply that repeats the same choice question`() {
        val questionHistory = listOf(DialogHistory("Ce meserie ai?", "Vrei detalii despre atelier sau despre program?", 1L))
        for (message in listOf("A doua", "Ambele", "Niciuna")) {
            var calls = 0
            val result = DialogueResponseNovelty.generate(
                prompt, "Ion", message, questionHistory, null, true,
            ) {
                calls++
                if (calls == 1) questionHistory.single().npcResponse else "Nu am programul complet confirmat."
            }

            assertEquals(2, calls, message)
            assertEquals("Nu am programul complet confirmat.", result.response, message)
            assertFalse(result.retryExhausted, message)
        }
    }

    @Test
    fun `short answer to NPC question retries a repeated clarification instead of looping`() {
        val questionHistory = listOf(DialogHistory("Ce meserie ai?", "Vrei detalii despre atelier?", 1L))
        for (message in listOf("Da", "Nu, mulțumesc")) {
            var calls = 0
            val result = DialogueResponseNovelty.generate(
                prompt, "Ion", message, questionHistory, null, true, retryRepeatedQuestions = false,
            ) {
                calls++
                if (calls == 1) questionHistory.single().npcResponse else "Ce altceva ai vrea sa lamurim?"
            }

            assertEquals(2, calls, message)
            assertEquals("Ce altceva ai vrea sa lamurim?", result.response, message)
            assertTrue(result.prompt.contains("=== CONTINUARE FARA REPETITIE ==="), message)
        }
        assertFalse(DialogueResponseNovelty.isRepeatedFollowUp("Da", history, "Am o veste."))
    }

    @Test
    fun `requested exact repetition is not rejected as a repeated new question`() {
        var calls = 0
        val result = DialogueResponseNovelty.generate(
            prompt, "Ion", "Ce ai spus?", history, null, true,
            retryInvalidReplies = true, retryRepeatedQuestions = true,
        ) {
            calls++
            "Am o veste."
        }

        assertEquals(1, calls)
        assertEquals("Am o veste.", result.response)
        assertFalse(result.retryExhausted)
        assertNull(result.rejectionReason)
    }

    @Test
    fun `explanations and qualified replay questions still reject repeated replies`() {
        assertTrue(DialogueResponseNovelty.isRepeatedFollowUp("Explică din nou", history, "Am o veste."))
        assertTrue(DialogueResponseNovelty.isRepeatedQuestion("Ce ai spus despre vreme?", history, "Am o veste."))
        assertTrue(DialogueResponseNovelty.isRepeatedQuestion("Nu repeta exact! Ce se aude?", history, "Am o veste."))
    }

    @Test
    fun `replay exemption does not bypass invalid reply validation`() {
        var calls = 0
        val result = DialogueResponseNovelty.generate(
            prompt, "Ion", "Ce ai spus?", history, null, true,
            retryInvalidReplies = true, retryRepeatedQuestions = true,
        ) {
            calls++
            if (calls == 1) "{\"reply\":\"Am o veste.\"}" else "Am o veste."
        }

        assertEquals(2, calls)
        assertEquals("Am o veste.", result.response)
        assertTrue(result.prompt.contains("=== CORECTARE FORMAT REPLICA ==="))
        assertFalse(result.retryExhausted)
        assertNull(result.rejectionReason)
    }

    @Test
    fun `repeated follow up gets one AI retry with an explicit continuation instruction`() {
        val prompts = mutableListOf<String>()
        val result = DialogueResponseNovelty.generate(
            prompt, "Ion", "Continua", history, null, true, retryInvalidReplies = true,
        ) { attemptPrompt ->
            prompts.add(attemptPrompt)
            if (prompts.size == 1) "  Am o veste! " else "Vestea priveste targul de maine."
        }

        assertEquals(2, prompts.size)
        assertTrue(prompts[1].contains("=== CONTINUARE FARA REPETITIE ==="))
        assertTrue(prompts[1].endsWith("\nIon: "))
        assertEquals("Vestea priveste targul de maine.", result.response)
        assertFalse(result.retryExhausted)
        assertEquals(prompts[1], result.prompt)
    }

    @Test
    fun `second repeated reply is rejected for local fallback`() {
        var calls = 0
        val result = DialogueResponseNovelty.generate(
            prompt, "Ion", "Spune-mi mai mult", history, null, true,
        ) {
            calls++
            "Am o veste."
        }

        assertEquals(2, calls)
        assertNull(result.response)
        assertTrue(result.retryExhausted)
    }

    @Test
    fun `explicit repetition complaint retries even without a question word`() {
        for (message in listOf("Ai repetat raspunsul", "Ai repetat raspunsul, explica mai clar.")) {
            var calls = 0
            val result = DialogueResponseNovelty.generate(
                prompt, "Ion", message, history, null, true,
                retryRepeatedQuestions = false,
            ) {
                calls++
                if (calls == 1) "Am o veste." else "Nu am detalii confirmate despre veste."
            }

            assertEquals(2, calls)
            assertEquals("Nu am detalii confirmate despre veste.", result.response)
            assertFalse(result.retryExhausted)
        }
    }

    @Test
    fun `unavailable provider is not retried as a repeated reply`() {
        var calls = 0
        val result = DialogueResponseNovelty.generate(
            prompt, "Ion", "Continua", history, null, true, retryInvalidReplies = true,
        ) {
            calls++
            null
        }

        assertEquals(1, calls)
        assertNull(result.response)
        assertFalse(result.retryExhausted)
    }

    @Test
    fun `verified facts and unrelated messages are not retried`() {
        var calls = 0
        val generator: (String) -> String? = {
            calls++
            "Am o veste."
        }

        assertEquals("Am o veste.", DialogueResponseNovelty.generate(
            prompt, "Ion", "Continua", history, "Am o veste.", true, generator = generator,
        ).response)
        assertEquals("Am o veste.", DialogueResponseNovelty.generate(
            prompt, "Ion", "Ce se intampla?", history, null, true, generator = generator,
        ).response)
        assertEquals("Am o veste.", DialogueResponseNovelty.generate(
            prompt, "Ion", "Continua", history, null, false, generator = generator,
        ).response)
        assertEquals(3, calls)
    }

    @Test
    fun `failed history cannot trigger a retry`() {
        val failedHistory = listOf(history.single().copy(branchStatus = DialogHistory.BranchStatus.FAILED))
        assertFalse(DialogueResponseNovelty.isRepeatedFollowUp("Continua", failedHistory, "Am o veste."))
    }

    @Test
    fun `invalid AI dialogue reply is rejected before reaching the player`() {
        var calls = 0
        val result = DialogueResponseNovelty.generate(
            prompt, "Ion", "Ce se intampla?", history, null, true,
        ) {
            calls++
            """{"reply":"Am o veste."}"""
        }

        assertEquals(1, calls)
        assertNull(result.response)
        assertEquals("invalid_ai_reply:format_invalid", result.rejectionReason)
        assertFalse(result.retryExhausted)
    }

    @Test
    fun `invalid reply gets one corrective AI retry without echoing rejected content`() {
        val prompts = mutableListOf<String>()
        val result = DialogueResponseNovelty.generate(
            prompt, "Ion", "Ce se intampla?", history, null, true, retryInvalidReplies = true,
        ) { attemptPrompt ->
            prompts.add(attemptPrompt)
            if (prompts.size == 1) """{"reply":"Am o veste."}""" else "Vestea priveste targul de maine."
        }

        assertEquals(2, prompts.size)
        assertTrue(prompts[1].contains("=== CORECTARE FORMAT REPLICA ==="))
        assertFalse(prompts[1].contains("""{"reply":"Am o veste."}"""))
        assertTrue(prompts[1].endsWith("\nIon: "))
        assertEquals("Vestea priveste targul de maine.", result.response)
        assertNull(result.rejectionReason)
        assertFalse(result.retryExhausted)
    }

    @Test
    fun `second invalid reply falls back after exactly one retry`() {
        var calls = 0
        val result = DialogueResponseNovelty.generate(
            prompt, "Ion", "Ce se intampla?", history, null, true, retryInvalidReplies = true,
        ) {
            calls++
            "Raspuns {{npc_name}}"
        }

        assertEquals(2, calls)
        assertNull(result.response)
        assertEquals("invalid_ai_reply:pattern_interzis", result.rejectionReason)
        assertTrue(result.retryExhausted)
    }

    @Test
    fun `invalid grounded reply is retried but never more than once`() {
        var calls = 0
        val groundedHistory = listOf(DialogHistory("Ce stii despre sat?", "Fieraria este inchisa.", 1L))
        val result = DialogueResponseNovelty.generate(
            prompt, "Ion", "Continua", groundedHistory, "Fieraria este inchisa.", true,
            retryInvalidReplies = true,
        ) {
            calls++
            if (calls == 1) """{"reply":"Fieraria este inchisa."}""" else "Fieraria este inchisa."
        }

        assertEquals(2, calls)
        assertEquals("Fieraria este inchisa.", result.response)
        assertFalse(result.retryExhausted)
    }

    @Test
    fun `invalid reply followed by a repeated answer does not trigger a third request`() {
        var calls = 0
        val result = DialogueResponseNovelty.generate(
            prompt, "Ion", "Continua", history, null, true, retryInvalidReplies = true,
        ) {
            calls++
            if (calls == 1) """{"reply":"Am o veste."}""" else "Am o veste."
        }

        assertEquals(2, calls)
        assertNull(result.response)
        assertEquals("repeated_followup", result.rejectionReason)
        assertTrue(result.retryExhausted)
    }

    @Test
    fun `reformulated question gets one retry focused on the current question`() {
        val prompts = mutableListOf<String>()
        val result = DialogueResponseNovelty.generate(
            prompt, "Ion", "Despre ce este vestea?", history, null, true,
            retryRepeatedQuestions = true,
        ) { attemptPrompt ->
            prompts.add(attemptPrompt)
            if (prompts.size == 1) "Am o veste!" else "Nu am inca detalii confirmate."
        }

        assertEquals(2, prompts.size)
        assertTrue(prompts.last().contains("=== RASPUNS LA INTREBAREA CURENTA ==="))
        assertTrue(prompts.last().contains("Nu inventa informatii pentru a evita repetitia"))
        assertTrue(prompts.last().endsWith("\nIon: "))
        assertEquals("Nu am inca detalii confirmate.", result.response)
        assertFalse(result.retryExhausted)
        assertNull(result.rejectionReason)
    }

    @Test
    fun `repeated question exhausts a single retry and records its own fallback reason`() {
        for (invalidFirstReply in listOf(false, true)) {
            var calls = 0
            val result = DialogueResponseNovelty.generate(
                prompt, "Ion", "Despre ce este vestea?", history, null, true,
                retryInvalidReplies = true, retryRepeatedQuestions = true,
            ) {
                calls++
                if (invalidFirstReply && calls == 1) """{"reply":"Am o veste."}""" else "Am o veste."
            }

            assertEquals(2, calls)
            assertNull(result.response)
            assertTrue(result.retryExhausted)
            assertEquals("repeated_question", result.rejectionReason)
        }
    }

    @Test
    fun `question repetition toggle prevents an additional request`() {
        var calls = 0
        val result = DialogueResponseNovelty.generate(
            prompt, "Ion", "Despre ce este vestea?", history, null, true,
            retryRepeatedQuestions = false,
        ) {
            calls++
            "Am o veste."
        }

        assertEquals(1, calls)
        assertEquals("Am o veste.", result.response)
        assertFalse(result.retryExhausted)
    }

    @Test
    fun `question retry leaves identical questions grounded replies and greetings alone`() {
        var calls = 0
        val generator: (String) -> String? = {
            calls++
            "Am o veste."
        }
        for ((message, facts) in listOf(
            "CE SE INTAMPLA!" to null,
            "Despre ce este vestea?" to "Am o veste.",
            "Salut" to null,
        )) {
            val result = DialogueResponseNovelty.generate(
                prompt, "Ion", message, history, facts, true,
                retryRepeatedQuestions = true, generator = generator,
            )
            assertEquals("Am o veste.", result.response)
            assertFalse(result.retryExhausted)
        }
        assertEquals(3, calls)
    }

    @Test
    fun `disabled repetition checks stay disabled after format correction`() {
        for (message in listOf("De ce?", "Despre ce este vestea?")) {
            var calls = 0
            val result = DialogueResponseNovelty.generate(
                prompt, "Ion", message, history, null, false,
                retryInvalidReplies = true, retryRepeatedQuestions = message == "De ce?",
            ) {
                calls++
                if (calls == 1) """{"reply":"Am o veste."}""" else "Am o veste."
            }
            assertEquals(2, calls)
            assertEquals("Am o veste.", result.response)
            assertNull(result.rejectionReason)
            assertFalse(result.retryExhausted)
        }
    }

    @Test
    fun `follow up detects alternating replies across recent stable history`() {
        val alternatingHistory = history + DialogHistory("Continua", "Ce anume vrei sa afli?", 2L)

        assertTrue(DialogueResponseNovelty.isRepeatedFollowUp("Continua", alternatingHistory, "Am o veste!"))
        assertFalse(DialogueResponseNovelty.isRepeatedFollowUp("Continua", alternatingHistory, "Vestea nu este confirmata."))
    }

    @Test
    fun `repetition checks ignore failed exchanges and history beyond five turns`() {
        val failedHistory = listOf(history.single().copy(branchStatus = DialogHistory.BranchStatus.FAILED))
        val oldHistory = history + (2L..6L).map { timestamp ->
            DialogHistory("Intrebare $timestamp", "Raspuns $timestamp", timestamp)
        }
        assertFalse(DialogueResponseNovelty.isRepeatedQuestion("Despre ce este vestea?", failedHistory, "Am o veste."))
        assertFalse(DialogueResponseNovelty.isRepeatedQuestion("Despre ce este vestea?", oldHistory, "Am o veste."))
        assertFalse(DialogueResponseNovelty.isRepeatedFollowUp("Continua", oldHistory, "Am o veste."))
    }

    @Test
    fun `dialogue accepts natural ellipsis but rejects template markers`() {
        val natural = DialogueResponseNovelty.generate(
            prompt, "Ion", "Ce se intampla?", history, null, true,
        ) { "Mai caut indicii..." }
        val template = DialogueResponseNovelty.generate(
            prompt, "Ion", "Ce se intampla?", history, null, true,
        ) { "Raspuns {{npc_name}}" }

        assertEquals("Mai caut indicii...", natural.response)
        assertNull(natural.rejectionReason)
        assertNull(template.response)
        assertEquals("invalid_ai_reply:pattern_interzis", template.rejectionReason)
    }

    @Test
    fun `duplicate detection ignores punctuation and diacritics`() {
        val previous = listOf(DialogHistory("Ce stii?", "Știu câteva lucruri!", 1L))
        assertTrue(DialogueResponseNovelty.isRepeatedFollowUp("Continuă", previous, "Stiu cateva lucruri."))
    }
}
