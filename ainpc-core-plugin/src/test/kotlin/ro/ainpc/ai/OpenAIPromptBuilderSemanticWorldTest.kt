package ro.ainpc.ai

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import ro.ainpc.environment.EnvironmentContext
import java.util.UUID

class OpenAIPromptBuilderSemanticWorldTest {
    @Test
    fun `memory recording date is presented without claiming when the event happened`() {
        val memory = "[source=conversation_quote] [recorded_on=2026-10-15] Jucatorul a spus ca a pierdut sabia ieri."
        val prompt = OpenAIPromptBuilder.buildPrompt(
            semanticSnapshot().copy(playerMessage = "Ce stii despre sabie?"), emptyList(), listOf(memory), null, null,
        )

        assertTrue(prompt.contains("- $memory"))
        assertTrue(prompt.contains("[recorded_on=...] arata ziua in care amintirea a fost salvata"))
        assertTrue(prompt.contains("nu deduce data evenimentului din acest camp"))
    }

    @Test
    fun `repeated memory records are not treated as independent confirmation`() {
        val memory = "[source=conversation_quote] [recorded_on=2026-10-15] [record_count=3] Jucatorul a spus ca a pierdut sabia."
        val prompt = OpenAIPromptBuilder.buildPrompt(
            semanticSnapshot().copy(playerMessage = "Ce stii despre sabie?"), emptyList(), listOf(memory), null, null,
        )

        assertTrue(prompt.contains("- $memory\n"))
        assertTrue(prompt.contains("din setul limitat recuperat"))
        assertTrue(prompt.contains("nu dovedeste N evenimente distincte sau confirmari independente"))
        assertTrue(prompt.contains("Repetarea unei declaratii nu o transforma in fapt verificat"))
    }

    @Test
    fun `AI resolves relative choices from offered options instead of always asking again`() {
        val question = "Vrei detalii despre atelier sau despre program?"
        val history = listOf(DialogHistory("Ce meserie ai?", question, 1L))
        for (message in listOf("A doua", "Ambele", "Niciuna")) {
            val prompt = OpenAIPromptBuilder.buildPrompt(
                semanticSnapshot().copy(playerMessage = message), history, emptyList(), null, null,
            )

            assertTrue(prompt.contains("Intrebarea NPC-ului: $question"), message)
            assertTrue(prompt.contains("Raspunsul scurt al jucatorului: $message"), message)
            assertTrue(prompt.contains("ordinea optiunilor din ultima intrebare"), message)
            assertTrue(prompt.contains("nu inventa optiuni lipsa"), message)
            assertTrue(prompt.contains("nu cere din nou aceeasi alegere"), message)
            assertTrue(prompt.contains("nu autorizeaza tranzactii, questuri sau alte actiuni"), message)
            assertFalse(prompt.contains("Daca intrebarea ofera mai multe optiuni sau raspunsul este ambiguu"), message)
        }
    }

    @Test
    fun `missing or ambiguous options are left to clarification rather than guessed`() {
        val prompt = OpenAIPromptBuilder.buildPrompt(
            semanticSnapshot().copy(playerMessage = "A treia"),
            listOf(DialogHistory("Salut", "Vrei sa discutam?", 1L)), emptyList(), null, null,
        )

        assertTrue(prompt.contains("optiunea ceruta lipseste ori ordinea nu este clara"))
        assertTrue(prompt.contains("Un simplu da la mai multe optiuni nu selecteaza automat prima"))
        val withoutHistory = OpenAIPromptBuilder.buildPrompt(
            semanticSnapshot().copy(playerMessage = "A doua"), emptyList(), emptyList(), null, null,
        )
        assertFalse(withoutHistory.contains("=== RASPUNS LA INTREBAREA NPC-ULUI ==="))
    }

    @Test
    fun `offline choice replies do not claim to have selected or executed an option`() {
        val history = listOf(DialogHistory("Ce meserie ai?", "Vrei detalii despre atelier sau despre program?", 1L))
        for (message in listOf("A doua", "Ambele", "Niciuna")) {
            val response = OpenAITextSupport.generateFallbackResponse(
                semanticSnapshot().copy(playerMessage = message), history,
            )

            assertTrue(PlayerConversationContinuation.templates(message, history).orEmpty().contains(response), response)
        }
    }

    @Test
    fun `AI prompt connects short answers to the last NPC question without authorizing actions`() {
        val question = "Vrei detalii despre atelier?"
        val history = listOf(DialogHistory("Ce meserie ai?", question, 1L))
        for (message in listOf("Da, te rog", "Nu, mulțumesc")) {
            val prompt = OpenAIPromptBuilder.buildPrompt(
                semanticSnapshot().copy(playerMessage = message), history, emptyList(), null, null,
            )

            assertTrue(prompt.contains("=== RASPUNS LA INTREBAREA NPC-ULUI ==="), message)
            assertTrue(prompt.contains("Intrebarea NPC-ului: $question"), message)
            assertTrue(prompt.contains("nu autorizeaza tranzactii, questuri sau alte actiuni"), message)
            assertTrue(prompt.contains("mai multe optiuni"), message)
            assertTrue(prompt.indexOf("=== RASPUNS LA INTREBAREA NPC-ULUI ===") < prompt.indexOf("=== MESAJUL JUCATORULUI ==="))
            assertTrue(prompt.contains("Intrebarea care a deschis subiectul: Ce meserie ai?"), message)
        }
        val unrelatedPrompt = OpenAIPromptBuilder.buildPrompt(
            semanticSnapshot().copy(playerMessage = "Da, unde este piața?"), history, emptyList(), null, null,
        )
        assertFalse(unrelatedPrompt.contains("=== RASPUNS LA INTREBAREA NPC-ULUI ==="))
        val missingQuestionPrompt = OpenAIPromptBuilder.buildPrompt(
            semanticSnapshot().copy(playerMessage = "Da"), emptyList(), emptyList(), null, null,
        )
        assertFalse(missingQuestionPrompt.contains("=== RASPUNS LA INTREBAREA NPC-ULUI ==="))
    }

    @Test
    fun `short answer prompt prioritizes the most recent NPC question`() {
        val history = listOf(
            DialogHistory("Ce meserie ai?", "Vrei detalii despre atelier?", 1L),
            DialogHistory("Da", "Preferi să discutăm despre program?", 2L),
        )
        val prompt = OpenAIPromptBuilder.buildPrompt(
            semanticSnapshot().copy(playerMessage = "Nu"), history.reversed(), emptyList(), null, null,
        )
        val answerContext = prompt.substringAfter("=== RASPUNS LA INTREBAREA NPC-ULUI ===")

        assertTrue(answerContext.contains("Intrebarea NPC-ului: Preferi să discutăm despre program?"))
        assertFalse(answerContext.contains("Intrebarea NPC-ului: Vrei detalii despre atelier?"))
        assertTrue(answerContext.contains("nu izolat sau ca raspuns la una mai veche"))
        assertTrue(answerContext.contains("Raspunsul scurt al jucatorului: Nu"))
    }

    @Test
    fun `continuation after a clarification answer retains the initial subject`() {
        val history = listOf(
            DialogHistory("Ce meserie ai?", "Vrei detalii despre atelier?", 1L),
            DialogHistory("Da", "Repar unelte la atelier.", 2L),
        )
        val prompt = OpenAIPromptBuilder.buildPrompt(
            semanticSnapshot().copy(playerMessage = "Continuă"), history, emptyList(), null, null,
        )

        assertTrue(prompt.contains("Intrebarea care a deschis subiectul: Ce meserie ai?"))
        assertTrue(prompt.contains("Ultima replica deja rostita: Repar unelte la atelier."))
        assertFalse(prompt.contains("Intrebarea care a deschis subiectul: Da"))
    }

    @Test
    fun `offline clarification replies do not become gratitude or indirect acknowledgements`() {
        val history = listOf(DialogHistory("Ce meserie ai?", "Vrei detalii despre atelier?", 1L))
        for (message in listOf("Da", "Nu, mulțumesc")) {
            val snapshot = semanticSnapshot().copy(playerMessage = message, directAddress = false)
            val response = OpenAITextSupport.generateFallbackResponse(snapshot, history)

            assertTrue(PlayerConversationContinuation.templates(message, history).orEmpty().contains(response), response)
            assertFalse(response.contains("Cu placere"))
            assertFalse(response.contains("Te aud"))
        }
    }

    @Test
    fun `provider fallback replays stored text while explanations remain contextual`() {
        val snapshot = semanticSnapshot().copy(playerMessage = "Ce ai spus?")
        val response = "Sunt paznic. Păzesc intrarea în sat."
        val history = listOf(DialogHistory("Ce meserie ai?", response, 1L))

        assertEquals(response, OpenAITextSupport.generateFallbackResponse(snapshot, history))
        assertEquals(
            "Nu am o replica anterioara disponibila pe care sa o repet.",
            OpenAITextSupport.generateFallbackResponse(snapshot),
        )
        val explanation = OpenAITextSupport.generateFallbackResponse(
            snapshot.copy(playerMessage = "Explică din nou"), history,
        )
        assertNotEquals(response, explanation)
        assertTrue(explanation.contains("o meserie"))
    }

    @Test
    fun `AI continuation keeps the original question after exact replay turns`() {
        val history = listOf(
            DialogHistory("Ce s-a intamplat la fierarie?", "Usa era incuiata.", 1L),
            DialogHistory("Repetă exact", "Usa era incuiata.", 2L),
            DialogHistory("Ce ai spus?", "Usa era incuiata.", 3L),
        )
        val prompt = OpenAIPromptBuilder.buildPrompt(
            semanticSnapshot().copy(playerMessage = "Explică din nou"), history, emptyList(), null, null,
        )

        assertTrue(prompt.contains("=== REPERE PENTRU CONTINUARE ==="))
        assertTrue(prompt.contains("Intrebarea care a deschis subiectul: Ce s-a intamplat la fierarie?"))
        assertTrue(prompt.contains("Ultima replica deja rostita: Usa era incuiata."))
        assertFalse(prompt.contains("Intrebarea care a deschis subiectul: Ce ai spus?"))
    }

    @Test
    fun `verified dialogue facts enter the AI prompt as data for a natural reply`() {
        val snapshot = semanticSnapshot().copy(playerMessage = "Ce se întâmplă în sat?")
        val verifiedFacts = "Lipsesc proviziile.\n Fierăria este închisă."
        val prompt = OpenAIPromptBuilder.buildPrompt(
            snapshot, emptyList(), emptyList(), null, null, groundedFacts = verifiedFacts,
        )

        assertTrue(prompt.contains("=== FAPTE VERIFICATE PENTRU ACEST RASPUNS ==="))
        assertTrue(prompt.contains("Lipsesc proviziile. Fierăria este închisă."))
        assertTrue(prompt.contains("Raspunde natural cu propriile cuvinte, pastrand faptele"))
        assertTrue(prompt.indexOf("=== FAPTE VERIFICATE PENTRU ACEST RASPUNS ===") <
            prompt.indexOf("=== MESAJUL JUCATORULUI ==="))
        assertFalse(OpenAIPromptBuilder.buildPrompt(snapshot, emptyList(), emptyList(), null, null)
            .contains("=== FAPTE VERIFICATE PENTRU ACEST RASPUNS ==="))
    }

    @Test
    fun `grounded AI failure returns the exact local fact instead of a generic fallback`() {
        val snapshot = semanticSnapshot().copy(playerMessage = "Ce se întâmplă în sat?")
        val verifiedFact = "Starea curenta a regiunii: Lipsesc proviziile."

        assertEquals(verifiedFact, OpenAITextSupport.generateGroundedFallbackResponse(snapshot, emptyList(), verifiedFact))
        assertEquals("Sunt Ion.", OpenAITextSupport.generateGroundedFallbackResponse(
            snapshot.copy(playerMessage = "Cine esti?"), emptyList(), null,
        ))
    }

    @Test
    fun `personality fact stays available in prompt and fallback without an AI provider`() {
        val description = "De obicei, prefer traditiile si imi pastrez calmul."
        val snapshot = semanticSnapshot().copy(
            playerMessage = "Ce fel de persoana esti?",
            personalityDescription = description,
        )

        assertEquals(description, OpenAITextSupport.generateFallbackResponse(snapshot))
        val prompt = OpenAIPromptBuilder.buildPrompt(snapshot, emptyList(), emptyList(), null, null)
        assertTrue(prompt.contains("npc_personality: $description"))
        assertTrue(prompt.contains("fact_personality_answer: $description"))
    }

    @Test
    fun `unusable history neither enters the prompt nor establishes personal recognition`() {
        val history = listOf(
            DialogHistory("mesaj-esuat", "raspuns-esuat", 1L, DialogHistory.BranchStatus.FAILED),
            DialogHistory("mesaj-respins", "raspuns-respins", 2L, DialogHistory.BranchStatus.REJECTED),
            DialogHistory("mesaj-propus", "raspuns-propus", 3L, DialogHistory.BranchStatus.PROPOSED),
            DialogHistory("mesaj-fara-raspuns", "   ", 4L),
        )

        val prompt = OpenAIPromptBuilder.buildPrompt(semanticSnapshot(), history, emptyList(), null, null)

        assertFalse(prompt.contains("=== CONVERSATIA RECENTA ==="))
        assertTrue(prompt.contains("Numar replici recente extrase din istoric: 0"))
        assertTrue(prompt.contains("Recunoastere personala: fara intalniri anterioare cunoscute"))
        history.forEach { assertFalse(prompt.contains(it.playerMessage)) }
    }

    @Test
    fun `prompt keeps exchange pairs in chronological order`() {
        val history = listOf(
            DialogHistory("mesaj-recent", "replica-recenta", 30L),
            DialogHistory("mesaj-vechi", "replica-veche", 10L),
            DialogHistory("mesaj-intermediar", "replica-intermediara", 20L),
        )

        val prompt = OpenAIPromptBuilder.buildPrompt(semanticSnapshot(), history, emptyList(), null, null)

        assertTrue(prompt.indexOf("replica-veche") < prompt.indexOf("mesaj-intermediar"))
        assertTrue(prompt.indexOf("replica-intermediara") < prompt.indexOf("mesaj-recent"))
    }

    @Test
    fun `provider fallback continues the topic supplied by this conversation history`() {
        val snapshot = semanticSnapshot().copy(playerMessage = "Continuă!")
        val workHistory = listOf(DialogHistory("Ce meserie ai?", "Sunt paznic.", 1L))
        val weatherHistory = listOf(DialogHistory("Ai vazut ploaia?", "Da.", 2L))

        assertTrue(OpenAITextSupport.generateFallbackResponse(snapshot, workHistory).contains("o meserie"))
        assertTrue(OpenAITextSupport.generateFallbackResponse(snapshot, weatherHistory).contains("vreme"))
        assertFalse(OpenAITextSupport.generateFallbackResponse(snapshot).contains("o meserie"))
    }

    @Test
    fun `provider failure recalls a safe category instead of a generic question reply`() {
        val snapshot = semanticSnapshot().copy(playerMessage = "Ce am discutat despre meserie?")
        val history = listOf(DialogHistory("Ce meserie ai?", "Sunt paznic.", 1L))

        assertEquals(
            "In schimburile recente disponibile, am vorbit despre o meserie.",
            OpenAITextSupport.generateFallbackResponse(snapshot, history),
        )
        assertEquals(
            "In schimburile recente disponibile, am vorbit despre o meserie.",
            OpenAITextSupport.generateFallbackResponse(
                snapshot.copy(playerMessage = "Ce am discutat despre ce meserie ai?"), history,
            ),
        )
        assertEquals(
            "Nu pot confirma din schimburile recente ce am discutat despre acel subiect.",
            OpenAITextSupport.generateFallbackResponse(snapshot.copy(playerMessage = "Ce am discutat despre comert?"), history),
        )
        assertEquals(
            "Nu am un schimb anterior disponibil despre care sa-ti povestesc.",
            OpenAITextSupport.generateFallbackResponse(snapshot),
        )
    }

    @Test
    fun `prompt instructs short follow ups to use recent context without repeating the greeting`() {
        val prompt = OpenAIPromptBuilder.buildPrompt(
            semanticSnapshot().copy(playerMessage = "De ce?"),
            listOf(DialogHistory("Ai vazut ploaia?", "Da.", 1L)), emptyList(), null, null
        )

        assertTrue(prompt.contains("foloseste ultimul schimb relevant din conversatia recenta"))
        assertTrue(prompt.contains("fara sa reiei salutul ori sa repeti aceeasi replica"))
        assertTrue(prompt.contains("Steve: Ai vazut ploaia?"))
    }

    @Test
    fun `subject specific recap prompt limits claims to recent exchanges`() {
        val prompt = OpenAIPromptBuilder.buildPrompt(
            semanticSnapshot().copy(playerMessage = "Ce am discutat despre fierarie?"),
            listOf(DialogHistory("Ce s-a intamplat la fierarie?", "Am gasit usa incuiata.", 1L)),
            emptyList(), null, null,
        )

        assertTrue(prompt.contains("foloseste numai schimburile din CONVERSATIA RECENTA"))
        assertTrue(prompt.contains("Rezuma cu propriile cuvinte"))
        assertTrue(prompt.contains("nu pretinde acces la discutii mai vechi"))
        assertTrue(prompt.contains("Steve: Ce s-a intamplat la fierarie?"))
    }

    @Test
    fun `short follow up anchors the first question and latest reply in an AI prompt`() {
        val history = listOf(
            DialogHistory("Ce s-a intamplat la fierarie?", "Fieraria s-a inchis aseara.", 1L),
            DialogHistory("Continua", "Dimineata am gasit usa incuiata.", 2L),
        )
        val prompt = OpenAIPromptBuilder.buildPrompt(
            semanticSnapshot().copy(playerMessage = "De ce?"), history, emptyList(), null, null,
        )

        assertTrue(prompt.contains("=== REPERE PENTRU CONTINUARE ==="))
        assertTrue(prompt.contains("Intrebarea care a deschis subiectul: Ce s-a intamplat la fierarie?"))
        assertTrue(prompt.contains("Ultima replica deja rostita: Dimineata am gasit usa incuiata."))
        assertTrue(prompt.indexOf("=== REPERE PENTRU CONTINUARE ===") < prompt.indexOf("=== MESAJUL JUCATORULUI ==="))
    }

    @Test
    fun `reply complaints direct AI back to the original question across repeated repairs`() {
        val history = listOf(
            DialogHistory("Ce s-a intamplat la fierarie?", "Usa era incuiata.", 1L),
            DialogHistory("Nu mi-ai raspuns", "Am gasit usa incuiata.", 2L),
            DialogHistory("Te repeti", "Nu am alte detalii confirmate.", 3L),
        )
        val prompt = OpenAIPromptBuilder.buildPrompt(
            semanticSnapshot().copy(playerMessage = "Nu asta am intrebat"), history, emptyList(), null, null,
        )

        assertTrue(prompt.contains("=== CORECTAREA RASPUNSULUI ANTERIOR ==="))
        assertTrue(prompt.contains("Raspunde direct la ultima intrebare relevanta"))
        assertTrue(prompt.contains("Intrebarea care a deschis subiectul: Ce s-a intamplat la fierarie?"))
        assertTrue(prompt.contains("Ultima replica deja rostita: Nu am alte detalii confirmate."))
        assertFalse(prompt.contains("Intrebarea care a deschis subiectul: Te repeti"))
        assertFalse(prompt.contains("Continua subiectul de mai jos pornind de la ultima replica"))
    }

    @Test
    fun `reply complaint without valid history asks for clarification without inventing an anchor`() {
        val prompt = OpenAIPromptBuilder.buildPrompt(
            semanticSnapshot().copy(playerMessage = "Ai repetat raspunsul"),
            listOf(DialogHistory("mesaj-esuat", "replica-esuata", 1L, DialogHistory.BranchStatus.FAILED)),
            emptyList(), null, null,
        )

        assertTrue(prompt.contains("daca intrebarea lipseste ori este ambigua, cere o precizare"))
        assertFalse(prompt.contains("=== REPERE PENTRU CONTINUARE ==="))
        assertFalse(prompt.contains("mesaj-esuat"))
    }

    @Test
    fun `compound complaints keep the original topic in prompt and provider fallback`() {
        val history = listOf(
            DialogHistory("Ce meserie ai?", "Sunt paznic.", 1L),
            DialogHistory("Nu mi-ai raspuns, de ce?", "Pazesc poarta.", 2L),
        )
        val snapshot = semanticSnapshot().copy(playerMessage = "Ai repetat raspunsul, explica mai clar.")
        val prompt = OpenAIPromptBuilder.buildPrompt(snapshot, history, emptyList(), null, null)

        assertTrue(prompt.contains("=== CORECTAREA RASPUNSULUI ANTERIOR ==="))
        assertTrue(prompt.contains("Intrebarea care a deschis subiectul: Ce meserie ai?"))
        assertTrue(prompt.contains("Ultima replica deja rostita: Pazesc poarta."))
        assertFalse(prompt.contains("Intrebarea curenta de lamurit:"))
        assertTrue(OpenAITextSupport.generateFallbackResponse(snapshot, history).contains("o meserie"))
        assertEquals("In ultimul schimb disponibil, am vorbit despre o meserie.",
            PlayerConversationRecall.resolve("Ce am discutat?", history))
    }

    @Test
    fun `mixed repair prompt prioritizes the explicit new question with or without history`() {
        val oldHistory = listOf(DialogHistory("Ce s-a intamplat la fierarie?", "Usa era incuiata.", 1L))
        for (history in listOf(emptyList(), oldHistory)) {
            val prompt = OpenAIPromptBuilder.buildPrompt(
                semanticSnapshot().copy(playerMessage = "Nu mi-ai răspuns, unde este piața?"),
                history, emptyList(), null, null,
            )

            assertTrue(prompt.contains("=== CORECTAREA RASPUNSULUI ANTERIOR ==="))
            assertTrue(prompt.contains("Intrebarea curenta de lamurit: unde este piața?"))
            assertTrue(prompt.contains("aceasta are prioritate fata de subiectul anterior"))
            assertTrue(prompt.contains("Steve: Nu mi-ai răspuns, unde este piața?"))
            assertFalse(prompt.contains("=== REPERE PENTRU CONTINUARE ==="))
            assertFalse(prompt.contains("Raspunde direct la ultima intrebare relevanta din conversatia recenta"))
        }
    }

    @Test
    fun `follow up after mixed repair uses the new question as its topic`() {
        val history = listOf(
            DialogHistory("Ce s-a intamplat la fierarie?", "Usa era incuiata.", 1L),
            DialogHistory("Nu mi-ai raspuns, unde este piata?", "Piata este la nord.", 2L),
        )
        val prompt = OpenAIPromptBuilder.buildPrompt(
            semanticSnapshot().copy(playerMessage = "Continua"), history, emptyList(), null, null,
        )

        assertTrue(prompt.contains("Intrebarea care a deschis subiectul: unde este piata?"))
        assertTrue(prompt.contains("Ultima replica deja rostita: Piata este la nord."))
        assertFalse(prompt.contains("Intrebarea care a deschis subiectul: Ce s-a intamplat la fierarie?"))
        assertFalse(prompt.contains("Intrebarea care a deschis subiectul: Nu mi-ai raspuns"))
        assertTrue(prompt.contains("Steve: Nu mi-ai raspuns, unde este piata?"))
    }

    @Test
    fun `mixed repair provider fallback answers the current factual question`() {
        val response = OpenAITextSupport.generateFallbackResponse(
            semanticSnapshot().copy(playerMessage = "Nu mi-ai raspuns, ce meserie ai?"),
            listOf(DialogHistory("Ai vazut ploaia?", "Da.", 1L)),
        )

        assertEquals("Sunt paznic.", response)
    }

    @Test
    fun `provider fallback preserves topic through reply complaints`() {
        val history = listOf(
            DialogHistory("Ce meserie ai?", "Sunt paznic.", 1L),
            DialogHistory("Nu mi-ai raspuns", "Ce detaliu te intereseaza?", 2L),
        )
        val response = OpenAITextSupport.generateFallbackResponse(
            semanticSnapshot().copy(playerMessage = "Ai repetat raspunsul"), history,
        )

        assertTrue(response.contains("o meserie"))
        assertEquals("In ultimul schimb disponibil, am vorbit despre o meserie.",
            PlayerConversationRecall.resolve("Ce am discutat?", history))
    }

    @Test
    fun `new questions and interrupted topics do not reuse continuation anchors`() {
        val history = listOf(
            DialogHistory("Ce s-a intamplat la fierarie?", "Fieraria s-a inchis aseara.", 1L),
            DialogHistory("Despre ce am vorbit?", "Despre fierarie.", 2L),
        )

        val newQuestion = OpenAIPromptBuilder.buildPrompt(
            semanticSnapshot().copy(playerMessage = "Unde este piata?"), history, emptyList(), null, null,
        )
        val interrupted = OpenAIPromptBuilder.buildPrompt(
            semanticSnapshot().copy(playerMessage = "Continua"), history, emptyList(), null, null,
        )

        assertFalse(newQuestion.contains("=== REPERE PENTRU CONTINUARE ==="))
        assertFalse(newQuestion.contains("=== CORECTAREA RASPUNSULUI ANTERIOR ==="))
        assertFalse(interrupted.contains("=== REPERE PENTRU CONTINUARE ==="))
    }

    @Test
    fun `greeting alone does not become an AI continuation topic`() {
        val prompt = OpenAIPromptBuilder.buildPrompt(
            semanticSnapshot().copy(playerMessage = "Continua"),
            listOf(DialogHistory("Salut!", "Buna ziua!", 1L)), emptyList(), null, null,
        )

        assertFalse(prompt.contains("=== REPERE PENTRU CONTINUARE ==="))
    }

    @Test
    fun `fallback greeting varies when repeated without an AI provider`() {
        val snapshot = semanticSnapshot().copy(playerMessage = "Salut")

        val first = OpenAITextSupport.generateFallbackResponse(snapshot)
        val second = OpenAITextSupport.generateFallbackResponse(snapshot)

        assertNotEquals(first, second)
    }

    @Test
    fun `prompt treats editable player profile as unverified declaration`() {
        val prompt = OpenAIPromptBuilder.buildPrompt(
            semanticSnapshot(), emptyList(), emptyList(), null,
            DialogManager.PromptDbContext(
                0, 0.0, null,
                PlayerProfile(PlayerRole.COMERCIANT, PlayerStyle.FORMAL, PlayerTrait.PRUDENT)
            )
        )

        assertTrue(prompt.contains("Rol declarat: comerciant"))
        assertTrue(prompt.contains("Stil de comunicare preferat: formal"))
        assertTrue(prompt.contains("Trasatura declarata: prudent"))
        assertTrue(prompt.contains("nu ca dovada a meseriei"))
    }

    @Test
    fun `prompt statement limit does not require discarding loaded personal memories`() {
        val statements = listOf("imi place fieraria", "locuiesc in sat")
        val hiddenPrompt = OpenAIPromptBuilder.buildPrompt(
            semanticSnapshot(), emptyList(), emptyList(), null,
            DialogManager.PromptDbContext(0, 0.0, playerConfirmedStatements = statements, maxPromptStatements = 0),
        )
        val limitedPrompt = OpenAIPromptBuilder.buildPrompt(
            semanticSnapshot(), emptyList(), emptyList(), null,
            DialogManager.PromptDbContext(0, 0.0, playerConfirmedStatements = statements, maxPromptStatements = 1),
        )

        assertFalse(hiddenPrompt.contains("imi place fieraria"))
        assertTrue(limitedPrompt.contains("imi place fieraria"))
        assertFalse(limitedPrompt.contains("locuiesc in sat"))
    }

    @Test
    fun `prompt recognizes past player interactions without a relationship row`() {
        val prompt = OpenAIPromptBuilder.buildPrompt(
            semanticSnapshot().copy(playerHeldItem = "minecraft:iron_sword"),
            listOf(DialogHistory("Salut", "Buna", 1L)),
            emptyList(),
            null,
            DialogManager.PromptDbContext(1, 0.0, 12)
        )

        assertTrue(prompt.contains("Recunoastere personala: cunoscut din interactiuni"))
        assertTrue(prompt.contains("Obiect vizibil in mana: minecraft:iron_sword"))
        assertTrue(prompt.contains("Reputatie in regiune: 12"))
        assertFalse(prompt.contains("Aceasta este prima intalnire"))
    }

    @Test
    fun `prompt distinguishes first personal meeting from regional reputation`() {
        val prompt = OpenAIPromptBuilder.buildPrompt(
            semanticSnapshot(), emptyList(), emptyList(), null,
            DialogManager.PromptDbContext(0, 0.0, 20)
        )

        assertTrue(prompt.contains("Recunoastere personala: fara intalniri anterioare cunoscute"))
        assertTrue(prompt.contains("Reputatie in regiune: 20"))
        assertTrue(prompt.contains("fara sa pretinzi ca il cunosti"))
    }

    @Test
    fun `neutral or absent regional reputation is not presented as a known trait`() {
        val prompt = OpenAIPromptBuilder.buildPrompt(
            semanticSnapshot(), emptyList(), emptyList(), null,
            DialogManager.PromptDbContext(0, 0.0, 0)
        )

        assertFalse(prompt.contains("Reputatie in regiune:"))
    }

    @Test
    fun `prompt separates current activity from planned routine and goal`() {
        val snapshot = semanticSnapshot().copy(
            currentActivity = "vorbeste cu jucatorul",
            plannedRoutineActivity = "merge la fierarie",
            immediateGoal = "termina reparatia"
        )

        val prompt = OpenAIPromptBuilder.buildPrompt(snapshot, emptyList(), emptyList(), null, null)

        assertTrue(prompt.contains("npc_current_activity: vorbeste cu jucatorul"))
        assertTrue(prompt.contains("npc_planned_routine: merge la fierarie"))
        assertTrue(prompt.contains("npc_immediate_goal: termina reparatia"))
        assertTrue(prompt.contains("fact_activity_answer: Vorbesc cu tine acum; in program am: merge la fierarie."))
        assertTrue(prompt.contains("fact_routine_answer: Acum am in program: merge la fierarie."))
        assertTrue(prompt.contains("nu pretinde ca un plan este deja realizat"))
    }

    @Test
    fun `provider fallback answers routine questions from the planned activity`() {
        val snapshot = semanticSnapshot().copy(
            playerMessage = "Ce urmeaza sa faci?",
            currentActivity = "vorbesc cu jucatorul",
            plannedRoutineActivity = "merge la fierarie",
        )

        assertTrue(OpenAITextSupport.generateFallbackResponse(snapshot).contains("Acum am in program: merge la fierarie."))
        assertTrue(
            OpenAITextSupport.generateFallbackResponse(snapshot.copy(playerMessage = "Ce faci?"))
                .contains("Vorbesc cu tine acum; in program am: merge la fierarie.")
        )
    }

    @Test
    fun `prompt does not expose internal routine diagnostics as npc plans`() {
        val prompt = OpenAIPromptBuilder.buildPrompt(
            semanticSnapshot().copy(plannedRoutineActivity = "ancora WORK lipseste sau este invalida"),
            emptyList(), emptyList(), null, null,
        )

        assertTrue(prompt.contains("npc_planned_routine: necunoscut"))
        assertTrue(prompt.contains("fact_routine_answer: Nu am un program stabilit acum."))
        assertFalse(prompt.contains("ancora WORK lipseste"))
    }

    @Test
    fun `MCP context precedes player message and NPC response prefix`() {
        val prompt = OpenAIPromptBuilder.buildPrompt(
            semanticSnapshot(), emptyList(), emptyList(), null, null,
            mcpContext = "=== CONTEXT LOCAL DIN MCP ===\n6 NPC-uri inregistrate."
        )

        assertTrue(prompt.indexOf("=== CONTEXT LOCAL DIN MCP ===") < prompt.indexOf("=== MESAJUL JUCATORULUI ==="))
        assertTrue(prompt.trimEnd().endsWith("Ion:"))
    }

    @Test
    fun `prompt includes semantic world context for profession questions`() {
        val snapshot = semanticSnapshot()

        val prompt = OpenAIPromptBuilder.buildPrompt(snapshot, emptyList(), emptyList(), null, null)

        assertTrue(prompt.contains("=== CONTEXT SEMANTIC AL LUMII ==="))
        assertTrue(prompt.contains("WORLD_LORE este pentru locuri"))
        assertTrue(prompt.contains("WORLD_HISTORY contine istoria locului"))
        assertTrue(prompt.contains("nu evenimente trecute"))
        assertTrue(prompt.contains("Pentru starea story curenta foloseste valoarea efectiva"))
        assertTrue(prompt.contains("NPC_LORE este pentru povestea"))
        assertTrue(prompt.contains("WORLD_LORE:"))
        assertTrue(prompt.contains("WORLD_HISTORY:"))
        assertTrue(prompt.contains("NPC_LORE:"))
        assertTrue(prompt.contains("STORY_SIGNALS:"))
        assertTrue(prompt.contains("lore=Fierar vechi din sat"))
        assertTrue(prompt.contains("Gheorghe Fierarul este fierarul"))
        assertTrue(prompt.contains("Daca jucatorul intreaba cine are o meserie"))
    }

    @Test
    fun `fallback can answer who questions from semantic facts`() {
        val response = OpenAITextSupport.generateFallbackResponse(semanticSnapshot())

        assertTrue(response.contains("Gheorghe Fierarul este fierarul"))
    }

    @Test
    fun `unrelated questions do not repeat an arbitrary profession fact`() {
        for (message in listOf("Care este pretul?", "Cine a ascuns cheia?", "Cine este brutarul?", "Unde sunt dragonii?")) {
            val response = OpenAITextSupport.generateFallbackResponse(semanticSnapshot().copy(playerMessage = message))

            assertFalse(response.contains("Gheorghe"), "$message -> $response")
            assertFalse(response.contains("Sunt in Fierarie"), "$message -> $response")
        }
    }

    @Test
    fun `fallback greeting prefix preserves the actual question`() {
        val response = OpenAITextSupport.generateFallbackResponse(
            semanticSnapshot().copy(playerMessage = "Salut, ai vazut ploaia?")
        )

        assertFalse(response.startsWith("Salut") || response.startsWith("Buna"), response)
    }

    @Test
    fun `indirect question with diacritics is not reduced to an acknowledgement`() {
        val snapshot = semanticSnapshot().copy(playerMessage = "Mai avem adăpost?", directAddress = false)
        val acknowledgements = setOf("Te aud.", "Daca imi vorbesti mie, spune mai clar.", "Sunt aici, daca voiai un raspuns.")

        repeat(3) {
            val response = OpenAITextSupport.generateFallbackResponse(snapshot)
            assertFalse(response in acknowledgements, response)
        }
    }

    @Test
    fun `word fragments do not produce an insult response`() {
        val snapshot = semanticSnapshot().copy(playerMessage = "Am curatat uneltele.")
        val insults = setOf("Vorbeste cu respect daca vrei raspunsuri.", "Nu-mi place tonul tau.", "Daca vii cu insulte, n-avem ce discuta.")

        repeat(3) {
            val response = OpenAITextSupport.generateFallbackResponse(snapshot)
            assertFalse(response in insults, response)
        }
    }

    @Test
    fun `unknown lore subject does not borrow another npc biography`() {
        val response = OpenAITextSupport.generateFallbackResponse(
            semanticSnapshot().copy(playerMessage = "Spune-mi povestea brutarului.")
        )

        assertFalse(response.contains("Gheorghe") || response.contains("Fierar vechi"), response)
    }

    @Test
    fun `fallback can surface history facts from semantic context`() {
        val response = OpenAITextSupport.generateFallbackResponse(
            semanticSnapshot().copy(playerMessage = "Ce istoric are satul?")
        )

        assertTrue(response.contains("Targul s-a deschis"))
        assertFalse(response.contains("market_day"))
        assertFalse(response.contains("Teme de istorie"))
    }

    @Test
    fun `fallback separates current story state place history and recent event titles`() {
        val baseSnapshot = semanticSnapshot()
        val snapshot = baseSnapshot.copy(semanticWorldContext = baseSnapshot.semanticWorldContext.replace(
            "- recent_events:",
            "- place_story_state=forge_open, source=persistent\n- place_history=sat_central:fierarie, lore=<none>, history=Atelierul a fost reconstruit\n- recent_events:",
        ))

        assertEquals(
            "Starea curenta a regiunii este market_day. Starea curenta a locului este forge_open.",
            OpenAITextSupport.generateFallbackResponse(snapshot.copy(playerMessage = "Care este starea povestii aici?")),
        )
        assertEquals(
            "Starea curenta a locului este forge_open.",
            OpenAITextSupport.generateFallbackResponse(snapshot.copy(playerMessage = "Care este starea cladirii?")),
        )
        assertEquals(
            "Istoria locului: Atelierul a fost reconstruit. Eveniment recent: Targul s-a deschis.",
            OpenAITextSupport.generateFallbackResponse(snapshot.copy(playerMessage = "Ce s-a intamplat aici?")),
        )
    }

    @Test
    fun `fallback does not present past events as active when live environment is unavailable`() {
        val response = OpenAITextSupport.generateFallbackResponse(
            semanticSnapshot().copy(playerMessage = "Ce evenimente sunt acum?")
        )

        assertFalse(response.contains("Targul s-a deschis"))
        assertFalse(response.contains("market_day"))
    }

    @Test
    fun `prompt and provider fallback use narrative descriptions instead of technical state keys`() {
        val baseSnapshot = semanticSnapshot()
        val snapshot = baseSnapshot.copy(
            playerMessage = "Care este starea poveștii aici?",
            semanticWorldContext = baseSnapshot.semanticWorldContext.replace(
                "- recent_events:",
                "- region_story_description=Este zi de târg, piața este deschisă.\n" +
                    "- place_story_state=closed, source=persistent\n" +
                    "- place_story_description=Atelierul este închis, temporar.\n- recent_events:",
            ),
        )

        assertEquals(
            "Starea curenta a regiunii: Este zi de târg, piața este deschisă. " +
                "Starea curenta a locului: Atelierul este închis, temporar.",
            OpenAITextSupport.generateFallbackResponse(snapshot),
        )
        val history = OpenAITextSupport.generateFallbackResponse(snapshot.copy(playerMessage = "Ce evenimente au fost?"))
        assertTrue(history.contains("Targul s-a deschis"))
        assertFalse(history.contains("Atelierul este închis"))
        assertFalse(history.contains("Este zi de târg"))
        val prompt = OpenAIPromptBuilder.buildPrompt(snapshot, emptyList(), emptyList(), null, null)
        assertTrue(prompt.contains("explica starea folosind acea descriere in locul cheii tehnice"))
        assertTrue(prompt.contains("- place_story_description=Atelierul este închis, temporar."))
    }

    @Test
    fun `provider fallback answers natural local situation questions from current story descriptions`() {
        val baseSnapshot = semanticSnapshot()
        val snapshot = baseSnapshot.copy(semanticWorldContext = baseSnapshot.semanticWorldContext.replace(
            "- recent_events:",
            "- region_story_description=Piața este deschisă.\n" +
                "- place_story_state=closed, source=persistent\n" +
                "- place_story_description=Atelierul este închis.\n- recent_events:",
        ))

        assertEquals(
            "Starea curenta a regiunii: Piața este deschisă.",
            OpenAITextSupport.generateFallbackResponse(snapshot.copy(playerMessage = "Ce se întâmplă în sat?")),
        )
        assertEquals(
            "Starea curenta a locului: Atelierul este închis.",
            OpenAITextSupport.generateFallbackResponse(snapshot.copy(playerMessage = "Cum stau lucrurile în clădire?")),
        )
        assertEquals(
            "Starea curenta a regiunii: Piața este deschisă.",
            OpenAITextSupport.generateFallbackResponse(snapshot.copy(playerMessage = "Cum e situația în Sat Central?")),
        )
        assertEquals(
            "Starea curenta a locului: Atelierul este închis.",
            OpenAITextSupport.generateFallbackResponse(snapshot.copy(playerMessage = "Ce se întâmplă la Fierarie?")),
        )
    }

    @Test
    fun `fallback can surface npc lore facts from semantic context`() {
        val response = OpenAITextSupport.generateFallbackResponse(
            semanticSnapshot().copy(playerMessage = "Spune-mi povestea fierarului.")
        )

        assertTrue(response.contains("Fierar vechi din sat"))
        assertTrue(response.contains("lucreaza la sat_central:fierarie"))

        val historyResponse = OpenAITextSupport.generateFallbackResponse(
            semanticSnapshot().copy(playerMessage = "Care este istoria fierarului?")
        )
        assertTrue(historyResponse.contains("Fierar vechi din sat"))
        assertFalse(historyResponse.contains("Targul s-a deschis"))
    }

    @Test
    fun `fallback can surface world lore facts from semantic context`() {
        val response = OpenAITextSupport.generateFallbackResponse(
            semanticSnapshot().copy(playerMessage = "Unde sunt acum?")
        )

        assertTrue(response.contains("Sunt in Fierarie din Sat Central"))
        assertTrue(response.contains("Locuri din zona"))
    }

    @Test
    fun `fallback can surface story signals from semantic context`() {
        val response = OpenAITextSupport.generateFallbackResponse(
            semanticSnapshot().copy(playerMessage = "Ce semnale are satul?")
        )

        assertTrue(response.contains("Semnalele povestii sunt"))
        assertTrue(response.contains("market_open"))
        assertTrue(response.contains("forge_trouble"))
    }

    @Test
    fun `fallback prefers current special events over historical events`() {
        val environment = EnvironmentContext.empty("world").copy(
            specialEvents = listOf("Festivalul recoltei"),
        )
        val response = OpenAITextSupport.generateFallbackResponse(
            semanticSnapshot().copy(
                playerMessage = "Ce evenimente speciale sunt acum?",
                environmentContext = environment,
            )
        )

        assertEquals("Evenimente active acum: Festivalul recoltei.", response)

        val combined = OpenAITextSupport.generateFallbackResponse(
            semanticSnapshot().copy(
                playerMessage = "Cine esti si ce evenimente speciale sunt acum?",
                environmentContext = environment,
            )
        )
        assertEquals("Sunt Ion. Evenimente active acum: Festivalul recoltei.", combined)

        val withoutMapping = OpenAITextSupport.generateFallbackResponse(
            semanticSnapshot().copy(
                playerMessage = "Cum e vremea?",
                semanticWorldContext = "",
                environmentContext = environment.copy(weather = EnvironmentContext.Weather.RAIN),
            )
        )
        assertEquals("Acum ploua.", withoutMapping)
    }

    private fun semanticSnapshot(): PromptSnapshot =
        PromptSnapshot(
            npcUuid = UUID.randomUUID(),
            npcName = "Ion",
            npcDescription = "Un satean atent la ce se intampla in sat.",
            environmentDescription = "",
            topologyConsensusBlock = "",
            semanticWorldContext = """
                WORLD_LORE:
                - region=sat_central, name=Sat Central, type=village, tags=trade,craft
                - current_place=sat_central:fierarie, name=Fierarie, type=forge, tags=work,craft_worker, access=public
                - nearby_place=sat_central:piata, name=Piata, type=market, tags=trade,public
                WORLD_HISTORY:
                - region_story_mode=evolutive, state=market_day, pool=market_day,forge_trouble
                - recent_events:
                  - region:sat_central market_open/market_day - Targul s-a deschis
                NPC_LORE:
                - Gheorghe Fierarul, occupation=Fierar, work=sat_central:fierarie, home=<none>, lore=Fierar vechi din sat, a reparat podul dupa furtuna.
                STORY_SIGNALS: market_open, forge_trouble
                Raspuns factual pentru intrebari despre meserii: Gheorghe Fierarul este fierarul si lucreaza la Fierarie.
            """.trimIndent(),
            familyMembers = emptyList(),
            profileCreated = false,
            profileSource = "",
            profileVersion = 1,
            profileSummary = "",
            profileDataJson = "{}",
            traitIds = emptyList(),
            playerName = "Steve",
            playerMessage = "Cine e fierarul in sat?",
            occupation = "paznic",
            emotionShortDescription = "calm",
            dominantEmotion = "calm",
            currentState = "idle",
            currentActivity = "sta de vorba",
            locationDescription = "sat",
            directAddress = true,
            explicitConversation = true,
            triggerReason = "direct",
            nearbyNpcCount = 1,
            distanceToNpc = 2.0
        )
}
