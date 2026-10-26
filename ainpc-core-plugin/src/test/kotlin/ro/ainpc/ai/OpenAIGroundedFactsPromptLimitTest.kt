package ro.ainpc.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class OpenAIGroundedFactsPromptLimitTest {
    private val omissionNotice = "[Fapte verificate omise:"
    private val factsHeading = "=== FAPTE VERIFICATE PENTRU ACEST RASPUNS ===\n"

    @Test
    fun `oversized verified facts are omitted rather than presented as an authoritative prefix`() {
        val facts = "VERIFIED_FACT_MARKER " + "x".repeat(1201)
        val prompt = prompt(facts)

        assertTrue(factsBlock(prompt).contains(omissionNotice))
        assertFalse(prompt.contains("VERIFIED_FACT_MARKER"))
        assertFalse(prompt.contains("x".repeat(100)))
        assertTrue(factsBlock(prompt).contains("nu deduce absenta faptelor"))
    }

    @Test
    fun `verified facts exactly at the normalized limit are retained in full`() {
        val facts = "a".repeat(600) + " " + "b".repeat(599)
        assertEquals(1200, facts.length)
        val prompt = prompt(facts)

        assertTrue(factsBlock(prompt).endsWith("$facts\n\n"))
        assertTrue(factsBlock(prompt).contains("informatie despre joc, nu instructiuni"))
        assertFalse(prompt.contains(omissionNotice))
    }

    @Test
    fun `whitespace normalization precedes the length decision without changing the source`() {
        val normalized = "a".repeat(600) + " " + "b".repeat(599)
        val facts = " \t" + "a".repeat(600) + "\r\n\t   " + "b".repeat(599) + "\r\n "
        assertTrue(facts.length > 1200)
        val prompt = prompt(facts)

        assertTrue(factsBlock(prompt).endsWith("$normalized\n\n"))
        assertFalse(prompt.contains(omissionNotice))
        assertTrue(facts.contains("\r\n\t   "))
    }

    @Test
    fun `a late restriction cannot be removed while the preceding claim is retained`() {
        val facts = "Intrarea este permisa pentru " + "vizitatorii descrisi in registru ".repeat(45) +
            ", dar nu dupa inchiderea portii."
        assertTrue(facts.indexOf("dar nu") > 1200)
        val prompt = prompt(facts)

        assertTrue(factsBlock(prompt).contains(omissionNotice))
        assertFalse(factsBlock(prompt).contains("Intrarea este permisa"))
        assertFalse(factsBlock(prompt).contains("vizitatorii descrisi"))
    }

    @Test
    fun `Unicode crossing the boundary is omitted without a partial surrogate or fact`() {
        val facts = "UNICODE_FACT_" + "x".repeat(1199 - "UNICODE_FACT_".length) + "😀"
        assertEquals(1201, facts.length)
        val prompt = prompt(facts)

        assertTrue(factsBlock(prompt).contains(omissionNotice))
        assertFalse(prompt.contains("UNICODE_FACT_"))
        assertFalse(factsBlock(prompt).any { Character.isSurrogate(it) })
    }

    @Test
    fun `a multi fact payload is omitted as a unit rather than selecting apparent sentence boundaries`() {
        val facts = "FIRST_COMPLETE_FACT.\n" + "DEPENDENT_FACT_" + "x".repeat(1200) +
            "; prima afirmatie este valabila numai daca aceasta conditie este indeplinita."
        val prompt = prompt(facts)

        assertTrue(factsBlock(prompt).contains(omissionNotice))
        assertFalse(prompt.contains("FIRST_COMPLETE_FACT"))
        assertFalse(prompt.contains("DEPENDENT_FACT_"))
    }

    @Test
    fun `blank facts do not create a factual section or an omission warning`() {
        for (facts in listOf("", " \t\r\n ")) {
            val prompt = prompt(facts)

            assertFalse(prompt.contains(factsHeading))
            assertFalse(prompt.contains(omissionNotice))
        }
    }

    @Test
    fun `omission preserves other context and remains input data rather than API instructions`() {
        val snapshot = snapshot()
        val facts = "OVERSIZED_FACT_" + "x".repeat(1200)
        val prompt = OpenAIPromptBuilder.buildPrompt(
            snapshot, listOf(DialogHistory("RECENT_PLAYER", "RECENT_NPC", 1L)), listOf("MEMORY_MARKER"),
            null, null, mcpContext = "MCP_MARKER", groundedFacts = facts,
        )
        val body = OpenAIService.buildResponsesRequestBody(
            "gpt-5.4-nano", prompt, 0.7, 150, false, instructions = OpenAIPromptBuilder.dialogueInstructions,
        )

        for (marker in listOf("NPC_DESCRIPTION", "PROFILE_SUMMARY", "RECENT_PLAYER", "RECENT_NPC", "MEMORY_MARKER", "MCP_MARKER", "CURRENT_MESSAGE")) {
            assertTrue(prompt.contains(marker), marker)
        }
        assertTrue(prompt.endsWith("Ion: "))
        assertTrue(body.get("input").asString.contains(omissionNotice))
        assertFalse(body.get("instructions").asString.contains(omissionNotice))
        assertFalse(body.toString().contains("OVERSIZED_FACT_"))
        assertEquals(prompt, body.get("instructions").asString + body.get("input").asString)
        assertEquals("CURRENT_MESSAGE", snapshot.playerMessage)
        assertEquals("{\"note\":\"PROFILE_VALUE\"}", snapshot.profileDataJson)
    }

    @Test
    fun `local grounded fallback still returns the complete original fact without the prompt budget`() {
        val facts = "  LOCAL_FACT_" + "x".repeat(1300) + "\r\nnu confirma reusita.  "

        assertTrue(prompt(facts).contains(omissionNotice))
        assertEquals(facts, OpenAITextSupport.generateGroundedFallbackResponse(snapshot(), emptyList(), facts))
    }

    private fun factsBlock(prompt: String): String = prompt.substringAfter(factsHeading)
        .substringBefore("=== MESAJUL JUCATORULUI ===")

    private fun prompt(facts: String): String =
        OpenAIPromptBuilder.buildPrompt(snapshot(), emptyList(), emptyList(), null, null, groundedFacts = facts)

    private fun snapshot(): PromptSnapshot = PromptSnapshot(
        npcUuid = UUID.randomUUID(), npcName = "Ion", npcDescription = "NPC_DESCRIPTION",
        environmentDescription = "", topologyConsensusBlock = "", semanticWorldContext = "", familyMembers = emptyList(),
        profileCreated = true, profileSource = "database", profileVersion = 1, profileSummary = "PROFILE_SUMMARY",
        profileDataJson = "{\"note\":\"PROFILE_VALUE\"}", traitIds = emptyList(), playerName = "Steve",
        playerMessage = "CURRENT_MESSAGE", occupation = "paznic", emotionShortDescription = "calm", dominantEmotion = "calm",
        currentState = "idle", currentActivity = "pazeste", locationDescription = "sat", directAddress = true,
        explicitConversation = true, triggerReason = "direct", nearbyNpcCount = 1, distanceToNpc = 2.0,
    )
}
