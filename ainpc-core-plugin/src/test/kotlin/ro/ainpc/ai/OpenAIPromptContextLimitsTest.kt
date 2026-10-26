package ro.ainpc.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ro.ainpc.story.StoryContextSnapshot
import java.util.UUID

class OpenAIPromptContextLimitsTest {
    @Test
    fun `large optional sources are bounded independently without losing current dialogue or facts`() {
        val snapshot = snapshot().copy(
            environmentDescription = oversizedContext("environment"),
            topologyConsensusBlock = oversizedContext("topology"),
            semanticWorldContext = oversizedContext("semantic"),
            playerMessage = "CURRENT_MESSAGE_" + "intrebare ".repeat(200),
        )
        val prompt = OpenAIPromptBuilder.buildPrompt(
            snapshot,
            listOf(DialogHistory("RECENT_PLAYER", "RECENT_NPC", 1L)),
            listOf("[source=game_event] MEMORY_MARKER"),
            null,
            null,
            storyContext = storyContext(listOf(oversizedContext("story"))),
            mcpContext = oversizedContext("mcp"),
            groundedFacts = "GROUNDED_FACT_MARKER",
        )
        val limitedBlocks = mapOf(
            "environment" to prompt.substringAfter("=== MEDIUL TAU IMEDIAT ===\n")
                .substringBefore("=== CONSENS TOPOLOGIC ==="),
            "topology" to prompt.substringAfter("=== CONSENS TOPOLOGIC ===\n")
                .substringBefore("=== CONTEXT SEMANTIC AL LUMII ==="),
            "semantic" to ("semantic_0: " + prompt.substringAfter("semantic_0: "))
                .substringBefore("=== CONTEXTUL CONVERSATIEI ==="),
            "story" to prompt.substringAfter("=== CONTEXT STORY LOCAL ===\n").substringBefore("mcp_0: "),
            "mcp" to ("mcp_0: " + prompt.substringAfter("mcp_0: "))
                .substringBefore("=== FAPTE VERIFICATE PENTRU ACEST RASPUNS ==="),
        )

        for ((source, limit) in mapOf(
            "environment" to 2000, "topology" to 2000, "semantic" to 8000, "story" to 4000, "mcp" to 2000,
        )) {
            val block = limitedBlocks.getValue(source).trimEnd('\n')
            assertTrue(block.contains("${source}_0: "), source)
            assertTrue(block.length <= limit, "$source: ${block.length} > $limit")
            assertTrue(block.endsWith(omissionNotice), source)
            assertFalse(prompt.contains("${source}_TAIL"), source)
        }
        assertEquals(5, prompt.lineSequence().count { it == omissionNotice })
        assertTrue(prompt.length < 33000, "Prompt length: ${prompt.length}")
        for (required in listOf(
            "npc_name: Ion", "npc_profession: paznic", "RECENT_PLAYER", "RECENT_NPC", "MEMORY_MARKER",
            "GROUNDED_FACT_MARKER", "NPC_DESCRIPTION_MARKER", "Steve: \"${snapshot.playerMessage}\"\n\n",
        )) {
            assertTrue(prompt.contains(required), required.take(100))
        }
        assertTrue(prompt.startsWith(OpenAIPromptBuilder.dialogueInstructions))
        assertTrue(prompt.endsWith("Ion: "))
        val body = OpenAIService.buildResponsesRequestBody(
            "gpt-5.4-nano", prompt, 0.7, 150, false, instructions = OpenAIPromptBuilder.dialogueInstructions,
        )
        assertEquals(prompt, body.get("instructions").asString + body.get("input").asString)
    }

    @Test
    fun `small optional context is preserved verbatim without omission notice`() {
        val snapshot = snapshot().copy(
            environmentDescription = "Seara.\nPloua.",
            topologyConsensusBlock = "Piata este la nord.\nFieraria este la est.",
            semanticWorldContext = "NPC_LORE:\n- Gheorghe, occupation=Fierar",
        )
        val story = storyContext(listOf("market_open"))
        val mcp = "=== CONTEXT LOCAL DIN MCP ===\nExemple: Ana, Maria.\n"
        val prompt = OpenAIPromptBuilder.buildPrompt(
            snapshot, emptyList(), emptyList(), null, null, storyContext = story, mcpContext = mcp,
        )

        for (context in listOf(
            snapshot.environmentDescription, snapshot.topologyConsensusBlock, snapshot.semanticWorldContext,
            story.toPromptBlock(), mcp,
        )) {
            assertTrue(prompt.contains(context), context)
        }
        assertFalse(prompt.contains(omissionNotice))
    }

    @Test
    fun `oversized factual line is omitted rather than cut into an incomplete claim`() {
        val prefix = "WORLD_HISTORY:\r\n"
        val oversizedFact = "- place_history=" + "lore ".repeat(2000) + ", history=nu a avut loc niciun atac."
        val prompt = OpenAIPromptBuilder.buildPrompt(
            snapshot().copy(semanticWorldContext = prefix + oversizedFact + "\r\n"),
            emptyList(), emptyList(), null, null,
        )

        assertTrue(prompt.contains("WORLD_HISTORY:\n$omissionNotice"))
        assertFalse(prompt.contains("- place_history="))
        assertFalse(prompt.contains("lore lore"))
        assertTrue(prompt.contains("Un context marcat partial nu este o lista completa"))
    }

    @Test
    fun `oversized first line is omitted and later sections remain intact`() {
        val prompt = OpenAIPromptBuilder.buildPrompt(
            snapshot().copy(environmentDescription = "😀".repeat(3000)),
            listOf(DialogHistory("Unde este piata?", "Piata este la nord.", 1L)),
            emptyList(), null, null, groundedFacts = "Piata este la nord.",
        )

        assertTrue(prompt.contains("=== MEDIUL TAU IMEDIAT ===\n$omissionNotice\n"))
        assertFalse(prompt.contains("😀"))
        assertTrue(prompt.contains("Steve: \"Unde este piata?\"\nIon: \"Piata este la nord.\""))
        assertTrue(prompt.contains("=== FAPTE VERIFICATE PENTRU ACEST RASPUNS ==="))
        assertTrue(prompt.contains("=== MESAJUL JUCATORULUI ===\nSteve: \"Salut!\"\n\n"))
    }

    @Test
    fun `optional block exactly at its limit is not changed`() {
        val context = "x".repeat(2000)
        val prompt = OpenAIPromptBuilder.buildPrompt(
            snapshot().copy(environmentDescription = context), emptyList(), emptyList(), null, null,
            mcpContext = context,
        )

        assertEquals(2, prompt.lineSequence().count { it == context })
        assertFalse(prompt.contains(omissionNotice))
    }

    private fun oversizedContext(source: String): String = buildString {
        repeat(500) { index -> append("${source}_$index: ").append("detaliu ".repeat(8)).append('\n') }
        append("${source}_TAIL")
    }

    private fun storyContext(signals: List<String>): StoryContextSnapshot = StoryContextSnapshot(
        "Ion", "paznic", "Steve", null, null, null, emptyList(), emptyList(), signals, emptyList(),
    )

    private fun snapshot(): PromptSnapshot = PromptSnapshot(
        npcUuid = UUID.randomUUID(),
        npcName = "Ion",
        npcDescription = "NPC_DESCRIPTION_MARKER",
        environmentDescription = "",
        topologyConsensusBlock = "",
        semanticWorldContext = "",
        familyMembers = emptyList(),
        profileCreated = false,
        profileSource = "",
        profileVersion = 1,
        profileSummary = "",
        profileDataJson = "{}",
        traitIds = emptyList(),
        playerName = "Steve",
        playerMessage = "Salut!",
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
        distanceToNpc = 2.0,
    )

    private val omissionNotice = "[Context partial: detalii omise din cauza limitei de lungime.]"
}
