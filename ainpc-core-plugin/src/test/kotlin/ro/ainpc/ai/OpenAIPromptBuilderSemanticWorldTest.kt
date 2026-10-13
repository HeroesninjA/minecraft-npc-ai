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
