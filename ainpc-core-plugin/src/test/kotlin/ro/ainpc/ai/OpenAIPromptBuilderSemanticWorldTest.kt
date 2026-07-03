package ro.ainpc.ai

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class OpenAIPromptBuilderSemanticWorldTest {
    @Test
    fun `prompt includes semantic world context for profession questions`() {
        val snapshot = semanticSnapshot()

        val prompt = OpenAIPromptBuilder.buildPrompt(snapshot, emptyList(), emptyList(), null, null)

        assertTrue(prompt.contains("=== CONTEXT SEMANTIC AL LUMII ==="))
        assertTrue(prompt.contains("WORLD_LORE este pentru locuri"))
        assertTrue(prompt.contains("WORLD_HISTORY este pentru trecut"))
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
    fun `fallback can surface history facts from semantic context`() {
        val response = OpenAITextSupport.generateFallbackResponse(
            semanticSnapshot().copy(playerMessage = "Ce istoric are satul?")
        )

        assertTrue(response.contains("Starea istorica a regiunii este market_day"))
        assertTrue(response.contains("Targul s-a deschis"))
    }

    @Test
    fun `fallback can surface npc lore facts from semantic context`() {
        val response = OpenAITextSupport.generateFallbackResponse(
            semanticSnapshot().copy(playerMessage = "Spune-mi povestea fierarului.")
        )

        assertTrue(response.contains("Fierar vechi din sat"))
        assertTrue(response.contains("lucreaza la sat_central:fierarie"))
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
