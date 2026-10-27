package ro.ainpc.ai

import com.google.gson.JsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.managers.MemoryRecallCandidate
import ro.ainpc.managers.MemoryRecallSelector
import java.time.LocalDate
import java.util.UUID

class OpenAIMemoryPromptLimitTest {
    private val omissionNotice = "[Amintiri omise:"

    @Test
    fun `oversized memory is omitted without dropping a late restriction from its text`() {
        val memory = "[source=game_event] OVERSIZED_MEMORY_MARKER " + "detaliu ".repeat(80) + "nu confirma consumul obiectului."
        val valid = "[source=game_event] Sabia nu a fost gasita."
        val prompt = prompt(listOf(memory, valid))

        assertFalse(prompt.contains("OVERSIZED_MEMORY_MARKER"))
        assertTrue(prompt.contains("- ${JsonPrimitive(valid)}\n"))
        assertTrue(prompt.contains(omissionNotice))
        assertTrue(prompt.contains("omisiunea nu confirma absenta"))
        assertTrue(memory.endsWith("nu confirma consumul obiectului."))
    }

    @Test
    fun `an exact five hundred character entry retains every metadata and content character`() {
        val prefix = "[source=conversation_quote] [recorded_on=2026-10-15] [record_count=2] "
        val entry = prefix + "x".repeat(500 - prefix.length)
        val prompt = prompt(listOf(entry))

        assertTrue(prompt.contains("- ${JsonPrimitive(entry)}\n"))
        assertFalse(prompt.contains(omissionNotice))
    }

    @Test
    fun `existing newline normalization is applied without clipping a fitting memory`() {
        val entry = "  [source=game_event] Sabia\r\nera veche.\n " + " \r\n".repeat(300)
        val prompt = prompt(listOf(entry))

        assertTrue(prompt.contains("- \"[source=game_event] Sabia  era veche.\"\n"))
        assertFalse(prompt.contains(omissionNotice))
        assertTrue(entry.length > 500)
    }

    @Test
    fun `oversized Unicode memory cannot leave a partial surrogate or a factual prefix`() {
        val prefix = "[source=game_event] UNICODE_MEMORY_MARKER "
        val entry = prefix + "x".repeat(496 - prefix.length) + "😀end"
        assertEquals(501, entry.length)
        val prompt = prompt(listOf(entry))
        val memoryBlock = prompt.substringAfter("=== AMINTIRI DESPRE ACEST JUCATOR ===\n")
            .substringBefore("=== MESAJUL JUCATORULUI ===")

        assertTrue(memoryBlock.contains(omissionNotice))
        assertFalse(memoryBlock.contains("UNICODE_MEMORY_MARKER"))
        assertFalse(memoryBlock.any { Character.isSurrogate(it) })
    }

    @Test
    fun `retrieval selects complete replacement memories that the builder can render unchanged`() {
        val oversized = MemoryRecallCandidate("Sabia OVERSIZED_MEMORY_MARKER " + "x".repeat(500), "conversation_quote")
        val replacements = List(6) { index ->
            MemoryRecallCandidate("Sabia MEMORY_$index " + "x".repeat(300), "game_event", LocalDate.of(2026, 10, 15))
        }
        val selected = MemoryRecallSelector.select("sabie", listOf(oversized) + replacements, 5, 2400)
        val prompt = prompt(selected.map(MemoryRecallCandidate::promptText))

        assertEquals(replacements.take(5), selected)
        assertFalse(prompt.contains("OVERSIZED_MEMORY_MARKER"))
        for (memory in selected) assertTrue(prompt.contains("- ${JsonPrimitive(memory.promptText())}\n"))
        assertFalse(prompt.contains(omissionNotice))
    }

    @Test
    fun `omission warning stays input data and preserves current dialogue and other context`() {
        val snapshot = snapshot()
        val entry = "[source=game_event] OVERSIZED_MEMORY_MARKER " + "x".repeat(500)
        val prompt = OpenAIPromptBuilder.buildPrompt(
            snapshot, listOf(DialogHistory("RECENT_PLAYER", "RECENT_NPC", 1L)), listOf(entry), null, null,
            mcpContext = "MCP_MARKER", groundedFacts = "VERIFIED_FACT_MARKER",
        )
        val body = OpenAIService.buildResponsesRequestBody(
            "gpt-5.4-nano", prompt, 0.7, 150, false, instructions = OpenAIPromptBuilder.dialogueInstructions,
        )

        for (marker in listOf("NPC_DESCRIPTION", "PROFILE_SUMMARY", "RECENT_PLAYER", "RECENT_NPC", "MCP_MARKER", "VERIFIED_FACT_MARKER", "CURRENT_MESSAGE")) {
            assertTrue(prompt.contains(marker), marker)
        }
        assertTrue(body.get("input").asString.contains(omissionNotice))
        assertFalse(body.get("instructions").asString.contains(omissionNotice))
        assertFalse(body.toString().contains("OVERSIZED_MEMORY_MARKER"))
        assertEquals(prompt, body.get("instructions").asString + body.get("input").asString)
        assertEquals("CURRENT_MESSAGE", snapshot.playerMessage)
        assertTrue(prompt.endsWith("Ion: "))
    }

    @Test
    fun `ordinary provenance metadata remains readable without an omission warning`() {
        val entries = listOf(
            "[source=conversation_quote] [recorded_on=2026-10-15] [record_count=3] Jucatorul a spus ca a pierdut sabia ieri.",
            "[source=game_event] Jucatorul a oferit paine.",
            "[source=unclassified] O inregistrare cu provenienta incerta.",
        )
        val prompt = prompt(entries)

        for (entry in entries) assertTrue(prompt.contains("- ${JsonPrimitive(entry)}\n"))
        assertTrue(prompt.contains("nu dovedeste N evenimente distincte"))
        assertFalse(prompt.contains(omissionNotice))
    }

    private fun prompt(memories: List<String>): String =
        OpenAIPromptBuilder.buildPrompt(snapshot(), emptyList(), memories, null, null)

    private fun snapshot(): PromptSnapshot = PromptSnapshot(
        npcUuid = UUID.randomUUID(), npcName = "Ion", npcDescription = "NPC_DESCRIPTION",
        environmentDescription = "", topologyConsensusBlock = "", semanticWorldContext = "", familyMembers = emptyList(),
        profileCreated = false, profileSource = "", profileVersion = 1, profileSummary = "PROFILE_SUMMARY", profileDataJson = "{}",
        traitIds = emptyList(), playerName = "Steve", playerMessage = "CURRENT_MESSAGE", occupation = "paznic",
        emotionShortDescription = "calm", dominantEmotion = "calm", currentState = "idle", currentActivity = "pazeste",
        locationDescription = "sat", directAddress = true, explicitConversation = true, triggerReason = "direct",
        nearbyNpcCount = 1, distanceToNpc = 2.0,
    )
}
